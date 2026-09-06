package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.PRIMARY_WEARER_ID
import com.safeshade.data.PersonaMode
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.data.Wearer
import com.safeshade.repo.SyncKeys
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * What actually goes on the wire.
 *
 * These are the assertions that catch the failures nobody sees for weeks: a
 * `wearer_id` left null so a medical record belongs to nobody, a `created_at`
 * sent as an explicit null onto a `not null default now()` column, two rows of
 * one table with different key sets so PostgREST rejects the whole batch, and a
 * `photo:` avatar id uploaded to a server that cannot fetch the file.
 */
class PayloadResolverTest {

    private val circle = "11111111-1111-4111-8111-111111111111"

    private val wearer = Wearer(
        id = PRIMARY_WEARER_ID,
        name = "Amma",
        avatarId = "preset-7",
        medicalId = MedicalId(bloodType = "B+", allergies = "Penicillin", age = 71),
        activeMode = PersonaMode.ELDERLY,
        deviceAddresses = listOf("AA:BB:CC:DD:EE:FF"),
        contacts = listOf(EmergencyContact(name = "Ravi", phone = "98765 43210"))
    )

    private val snapshot = SyncSnapshot(
        circleId = circle,
        userId = "22222222-2222-4222-8222-222222222222",
        userEmail = "owner@example.com",
        ownerName = "Dibs",
        wearers = listOf(wearer),
        globalContacts = listOf(EmergencyContact(name = "Sister", phone = "91234 56789", isPrimary = true)),
        alerts = listOf(
            FallAlertEvent(
                id = "33333333-3333-4333-8333-333333333333",
                timestamp = 1_700_000_000_000L,
                kind = TripKind.FALL,
                outcome = TripOutcome.DISMISSED,
                wearerId = PRIMARY_WEARER_ID
            )
        ),
        zones = listOf(
            GeofenceZone(
                id = "44444444-4444-4444-8444-444444444444",
                name = "Home",
                lat = 12.9,
                lon = 77.6,
                radiusMeters = 150f
            )
        )
    )

    private fun JsonObject.str(key: String): String? =
        this[key]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content

    // ============================================
    // THREE TABLES
    // ============================================

    @Test
    fun `wearer row carries the circle and a derived uuid id`() {
        val row = PayloadResolver.resolve(
            CloudTables.WEARERS, wearer.id, OutboxOp.UPSERT, snapshot
        )
        assertNotNull(row)
        requireNotNull(row)
        assertEquals(circle, row.str("circle_id"))
        assertEquals("Amma", row.str("name"))
        assertEquals("ELDERLY", row.str("persona_mode"))
        // "wearer-primary" is not a uuid and would be rejected outright.
        assertEquals(CloudIds.cloudId(PRIMARY_WEARER_ID, circle), row.str("id"))
        assertTrue(row.str("id")!!.matches(UUID_SHAPE))
    }

    @Test
    fun `medical row belongs to its wearer`() {
        val row = PayloadResolver.resolve(
            CloudTables.MEDICAL_IDS,
            SyncKeys.medical(wearer.id),
            OutboxOp.UPSERT,
            snapshot
        )
        requireNotNull(row)
        assertEquals(CloudIds.cloudId(wearer.id, circle), row.str("wearer_id"))
        assertEquals("B+", row.str("blood_type"))
        assertEquals("Penicillin", row.str("allergies"))
        assertEquals(circle, row.str("circle_id"))
    }

    @Test
    fun `alert row keeps its outcome and is stamped with the circle`() {
        val row = PayloadResolver.resolve(
            CloudTables.ALERTS,
            "33333333-3333-4333-8333-333333333333",
            OutboxOp.UPSERT,
            snapshot
        )
        requireNotNull(row)
        assertEquals("FALL", row.str("kind"))
        assertEquals("DISMISSED", row.str("outcome"))
        assertEquals(circle, row.str("circle_id"))
        assertEquals("2023-11-14T22:13:20Z", row.str("occurred_at"))
    }

    @Test
    fun `a contact in the global list has no wearer`() {
        val key = SyncKeys.contact(wearerId = null, phone = "91234 56789")
        val row = PayloadResolver.resolve(
            CloudTables.EMERGENCY_CONTACTS, key, OutboxOp.UPSERT, snapshot
        )
        requireNotNull(row)
        assertNull(row.str("wearer_id"))
        assertEquals("Sister", row.str("name"))
    }

    @Test
    fun `a contact belonging to a wearer names them`() {
        val key = SyncKeys.contact(wearer.id, "98765 43210")
        val row = PayloadResolver.resolve(
            CloudTables.EMERGENCY_CONTACTS, key, OutboxOp.UPSERT, snapshot
        )
        requireNotNull(row)
        assertEquals(CloudIds.cloudId(wearer.id, circle), row.str("wearer_id"))
        assertEquals("Ravi", row.str("name"))
    }

    // ============================================
    // SKIPPING
    // ============================================

    @Test
    fun `an unknown record id resolves to null rather than a half-built row`() {
        assertNull(
            PayloadResolver.resolve(
                CloudTables.ZONES, "no-such-zone", OutboxOp.UPSERT, snapshot
            )
        )
        assertNull(
            PayloadResolver.resolve(
                CloudTables.WEARERS, "no-such-wearer", OutboxOp.UPSERT, snapshot
            )
        )
        assertNull(
            PayloadResolver.resolve(
                CloudTables.MEDICAL_IDS,
                SyncKeys.medical("no-such-wearer"),
                OutboxOp.UPSERT,
                snapshot
            )
        )
    }

    @Test
    fun `an unknown table resolves to null`() {
        assertNull(
            PayloadResolver.resolve("vitals_samples", wearer.id, OutboxOp.UPSERT, snapshot)
        )
    }

    @Test
    fun `nothing resolves without a circle`() {
        assertNull(
            PayloadResolver.resolve(
                CloudTables.WEARERS,
                wearer.id,
                OutboxOp.UPSERT,
                snapshot.copy(circleId = "")
            )
        )
    }

    // ============================================
    // THE SHAPE OF A BATCH
    // ============================================

    @Test
    fun `a live row and a tombstone of the same table have identical keys`() {
        // PostgREST rejects a bulk array whose objects differ in key set, with
        // one 400 for the whole batch - and a drain sends a table as one batch.
        val live = PayloadResolver.resolve(
            CloudTables.ZONES,
            "44444444-4444-4444-8444-444444444444",
            OutboxOp.UPSERT,
            snapshot
        )
        val dead = PayloadResolver.resolve(
            CloudTables.ZONES,
            "55555555-5555-4555-8555-555555555555",
            OutboxOp.DELETE,
            snapshot
        )
        requireNotNull(live)
        requireNotNull(dead)
        assertEquals(live.keys, dead.keys)
    }

    @Test
    fun `no row carries the columns the server owns`() {
        val tables = listOf(
            CloudTables.WEARERS to wearer.id,
            CloudTables.ALERTS to "33333333-3333-4333-8333-333333333333",
            CloudTables.ZONES to "44444444-4444-4444-8444-444444444444"
        )
        for ((table, id) in tables) {
            val row = PayloadResolver.resolve(table, id, OutboxOp.UPSERT, snapshot)
            requireNotNull(row)
            // Explicit nulls on `not null default now()` columns are a
            // constraint violation, not a defaulted value.
            assertTrue("$table sent created_at", "created_at" !in row.keys)
            assertTrue("$table sent updated_at", "updated_at" !in row.keys)
        }
        val alert = PayloadResolver.resolve(
            CloudTables.ALERTS, "33333333-3333-4333-8333-333333333333", OutboxOp.UPSERT, snapshot
        )
        requireNotNull(alert)
        // Another guardian's acknowledgement is not this phone's to erase.
        assertTrue("acknowledged_by" !in alert.keys)
        assertTrue("acknowledged_at" !in alert.keys)
    }

    @Test
    fun `a photo avatar is sent as nothing rather than as a local file path`() {
        val withPhoto = snapshot.copy(
            wearers = listOf(wearer.copy(avatarId = "photo:/data/user/0/com.safeshade/x.jpg"))
        )
        val row = PayloadResolver.resolve(
            CloudTables.WEARERS, wearer.id, OutboxOp.UPSERT, withPhoto
        )
        requireNotNull(row)
        assertNull(row.str("avatar_id"))
    }

    @Test
    fun `the profile role is the lower case the CHECK constraint allows`() {
        val row = PayloadResolver.resolve(
            CloudTables.PROFILES,
            requireNotNull(snapshot.userId),
            OutboxOp.UPSERT,
            snapshot
        )
        requireNotNull(row)
        assertEquals("guardian", row.str("role"))
    }

    private companion object {
        val UUID_SHAPE =
            Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
    }
}
