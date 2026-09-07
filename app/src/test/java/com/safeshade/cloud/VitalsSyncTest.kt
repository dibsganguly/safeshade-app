package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.data.PRIMARY_WEARER_ID
import com.safeshade.data.VitalsSample
import com.safeshade.data.Wearer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What a vitals reading looks like on the wire.
 *
 * The failures being defended against here are the quiet ones. A sample stamped
 * with the moment it was *sent* rather than the moment it was *measured* puts a
 * heart rate on a guardian's chart at a minute nobody's heart was read. A
 * `wearer_id` left null files a reading under nobody. And a row whose key set
 * differs from its neighbour's gets the whole batch rejected by PostgREST with
 * one 400 - so every sample in that drain is marked failed, five times over,
 * and reported to the user as not having reached SafeShade Cloud.
 */
class VitalsSyncTest {

    private val circle = "11111111-1111-4111-8111-111111111111"

    private val wearer = Wearer(id = PRIMARY_WEARER_ID, name = "Amma")

    private val sample = VitalsSample(
        id = "77777777-7777-4777-8777-777777777777",
        at = 1_700_000_000_000L,
        heartRateBpm = 72,
        spo2Percent = 97,
        tempC = 36.7f,
        source = VitalsSample.SOURCE_DEVICE,
        wearerId = PRIMARY_WEARER_ID
    )

    private fun snapshot(vararg samples: VitalsSample) = SyncSnapshot(
        circleId = circle,
        userId = "22222222-2222-4222-8222-222222222222",
        wearers = listOf(wearer),
        vitalsSamples = samples.toList()
    )

    private fun resolve(s: SyncSnapshot, id: String = sample.id, op: OutboxOp = OutboxOp.UPSERT) =
        PayloadResolver.resolve(CloudTables.VITALS_SAMPLES, id, op, s)

    private fun JsonObject.str(key: String): String? =
        this[key]?.takeIf { it != JsonNull }?.jsonPrimitive?.content

    @Test
    fun `a device reading carries its measurements, its circle and its wearer`() {
        val row = resolve(snapshot(sample))
        assertNotNull("a queued sample id must resolve to a row", row)
        requireNotNull(row)

        assertEquals(circle, row.str("circle_id"))
        assertEquals(CloudIds.cloudId(sample.id, circle), row.str("id"))
        assertEquals(CloudIds.cloudId(PRIMARY_WEARER_ID, circle), row.str("wearer_id"))
        assertEquals("72", row.str("heart_rate_bpm"))
        assertEquals("97", row.str("spo2_percent"))
        assertEquals("device", row.str("source"))
    }

    /**
     * The one that would be invisible for weeks: a sample that sat in an
     * offline queue overnight is still a reading from last night.
     */
    @Test
    fun `measured_at is when it was read, not when it was sent`() {
        val row = requireNotNull(resolve(snapshot(sample)))
        assertEquals("2023-11-14T22:13:20Z", row.str("measured_at"))
    }

    @Test
    fun `an ambient sound sample travels as one too`() {
        val ambient = VitalsSample(
            id = "88888888-8888-4888-8888-888888888888",
            ambientDb = 84.5,
            source = VitalsSample.SOURCE_PHONE,
            wearerId = null
        )
        val row = requireNotNull(resolve(snapshot(ambient), ambient.id))
        assertEquals("84.5", row.str("ambient_db"))
        assertEquals("phone", row.str("source"))
        // No wearer named is a null column, not an absent key. See the key-set
        // test below for why that distinction is the expensive one.
        assertEquals(JsonNull, row["wearer_id"])
    }

    @Test
    fun `a body temperature crosses as a number the column can hold`() {
        val row = requireNotNull(resolve(snapshot(sample)))
        // Float on the phone, double precision in Postgres. Asserted as a
        // number rather than a string so a change of type is caught here.
        assertEquals(36.7, requireNotNull(row.str("body_temp_c")).toDouble(), 0.001)
    }

    /**
     * `vitals_samples.source` carries a check constraint. A row that reached
     * the wire with a third value would be rejected as a *batch*, taking every
     * other reading in the same drain with it - so the resolver refuses it even
     * though `VitalsRepository.record` refuses it first.
     */
    @Test
    fun `a source the server would refuse never reaches the wire`() {
        val invented = sample.copy(source = "simulated")
        assertNull(resolve(snapshot(invented)))
    }

    @Test
    fun `a sample this phone no longer holds resolves to nothing`() {
        assertNull(resolve(snapshot()))
    }

    /**
     * `VitalsRepository.clear` tombstones every sample it drops. Before the
     * DELETE branch existed each of those resolved to null - a skip - and three
     * drains later reported "there was nothing left on this phone to send for
     * this" about a deletion that was the entire point of the tap.
     */
    @Test
    fun `clearing the history produces a tombstone rather than a skip`() {
        val row = resolve(snapshot(), op = OutboxOp.DELETE)
        assertNotNull("a delete must resolve even with the sample gone", row)
        requireNotNull(row)
        assertEquals(CloudIds.cloudId(sample.id, circle), row.str("id"))
        assertEquals(circle, row.str("circle_id"))
    }

    /**
     * PostgREST rejects a bulk insert whose objects have different key sets,
     * with one 400 for the whole array. The outbox drains a table as one
     * request, so this is the difference between one odd sample and every
     * sample in the batch being marked failed.
     */
    @Test
    fun `every vitals row, full, sparse or tombstone, carries the same keys`() {
        val full = requireNotNull(resolve(snapshot(sample)))
        val sparse = VitalsSample(
            id = "99999999-9999-4999-8999-999999999999",
            heartRateBpm = 60,
            source = VitalsSample.SOURCE_PHONE
        )
        val thin = requireNotNull(resolve(snapshot(sparse), sparse.id))
        val tombstone = requireNotNull(resolve(snapshot(), op = OutboxOp.DELETE))

        assertEquals(full.keys, thin.keys)
        assertEquals(full.keys, tombstone.keys)
        // The server's own columns are never sent: an explicit null on a
        // `not null default now()` column is a constraint violation, because a
        // default applies to an absent key rather than to a present null one.
        assertTrue("created_at" !in full.keys)
        assertTrue("updated_at" !in full.keys)
    }
}
