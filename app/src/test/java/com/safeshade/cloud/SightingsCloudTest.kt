package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.DeviceSightingRow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The mesh relay: a phone reporting a wearable it walked past.
 *
 * Two things are worth a test here and they pull in opposite directions. A
 * sighting has to be *cheap* - a phone walking beside a wearable sees it several
 * times a minute, and a row per scan window is a battery cost and a table full
 * of one walk. And it has to be *honest* - a wearable somebody is searching for
 * must not be silenced by a limiter that counted an insert the server refused.
 */
class SightingsCloudTest {

    private val address = "AA:BB:CC:DD:EE:FF"
    private val deviceId = "55555555-5555-4555-8555-555555555555"

    private var clock = 1_700_000_000_000L

    private fun cloud(
        client: CloudClient,
        deviceIdFor: (String) -> String? = { null }
    ) = SightingsCloud(client, deviceIdFor, now = { clock })

    private suspend fun SightingsCloud.reportOnce() =
        report(address, rssi = -71, lat = 12.9, lon = 77.6, accuracyM = 8.0, seenAt = clock)

    // ============================================
    // REPORTING
    // ============================================

    @Test
    fun `a sighting is inserted with an id, an address and no reporter`() = runBlocking {
        val client = FakeCloudClient()
        assertEquals(CloudResult.Ok(true), cloud(client).reportOnce())

        val row = client.tables[CloudTables.DEVICE_SIGHTINGS]?.values?.single()
        assertNotNull("the sighting must be inserted", row)
        requireNotNull(row)
        // `device_sightings.id` is a uuid primary key with no default, so the
        // client mints it or the insert fails outright.
        assertNotNull(row["id"]?.jsonPrimitive?.content)
        assertEquals(address, row["ble_address"]?.jsonPrimitive?.content)
        assertEquals("-71", row["rssi"]?.jsonPrimitive?.content)
        // The passer-by is never identified. Identifying them turns a helpful
        // feature into a tracking network.
        assertNull(row["reporter_id"]?.jsonPrimitive?.contentOrNullSafe())
    }

    @Test
    fun `a known address carries the device it belongs to`() = runBlocking {
        val client = FakeCloudClient()
        cloud(client, deviceIdFor = { if (it == address) deviceId else null }).reportOnce()

        val row = requireNotNull(client.tables[CloudTables.DEVICE_SIGHTINGS]?.values?.single())
        assertEquals(deviceId, row["device_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `the same address is not reported twice inside ten minutes`() = runBlocking {
        val client = FakeCloudClient()
        val sightings = cloud(client)

        assertEquals(CloudResult.Ok(true), sightings.reportOnce())
        clock += 5 * 60 * 1000L
        // Not a failure. It means the server already knows, and drawing it as
        // an error would be reporting a success as a problem.
        assertEquals(CloudResult.Ok(false), sightings.reportOnce())
        assertEquals(1, client.tables[CloudTables.DEVICE_SIGHTINGS]?.size)

        clock += 6 * 60 * 1000L
        assertEquals(CloudResult.Ok(true), sightings.reportOnce())
        assertEquals(2, client.tables[CloudTables.DEVICE_SIGHTINGS]?.size)
    }

    /**
     * A limiter stamped before the insert would let one refused call silence
     * the address for ten minutes - and on a wearable somebody is out looking
     * for, that is exactly the wrong ten minutes.
     */
    @Test
    fun `a refused insert does not start the limiter`() = runBlocking {
        val client = FakeCloudClient().apply { failNext = "You are not connected to the internet." }
        val sightings = cloud(client)

        assertTrue(sightings.reportOnce() is CloudResult.Failed)
        assertTrue("the address must still be due", sightings.isDue(address, clock))

        assertEquals(CloudResult.Ok(true), sightings.reportOnce())
    }

    @Test
    fun `the limiter is not case sensitive, because a BLE address is not`() = runBlocking {
        val client = FakeCloudClient()
        val sightings = cloud(client)
        sightings.reportOnce()
        assertFalse(sightings.isDue(address.lowercase(), clock))
    }

    /**
     * A clock that jumped backwards - a manual change, a time sync - must not
     * silence the phone for as long as the jump lasts.
     */
    @Test
    fun `a clock that moved backwards does not hold a report`() = runBlocking {
        val client = FakeCloudClient()
        val sightings = cloud(client)
        sightings.reportOnce()
        assertTrue(sightings.isDue(address, clock - 60 * 60 * 1000L))
    }

    @Test
    fun `an empty address is refused rather than inserted as nothing`() = runBlocking {
        val client = FakeCloudClient()
        val result = cloud(client).report("  ", -60, null, null, null, clock)
        assertTrue(result is CloudResult.Failed)
        assertTrue(client.tables.isEmpty())
    }

    // ============================================
    // READING BACK
    // ============================================

    @Test
    fun `no addresses asked for is no rows and no request`() = runBlocking {
        val client = FakeCloudClient().apply { failNext = "should not be consumed" }
        assertEquals(CloudResult.Ok(emptyList<DeviceSightingRow>()), cloud(client).lastSeen(emptyList()))
        // The armed failure is still armed, which is how we know no call was made.
        assertEquals("should not be consumed", client.failNext)
    }

    @Test
    fun `only the newest sighting of each address comes back, newest first`() {
        val rows = listOf(
            sighting("AA:BB", "2026-09-01T10:00:00Z"),
            sighting("AA:BB", "2026-09-05T18:30:00Z"),
            sighting("CC:DD", "2026-09-06T09:00:00Z"),
            sighting("CC:DD", "2026-09-02T09:00:00Z")
        )
        val newest = SightingsCloud.newestPerAddress(rows)

        assertEquals(2, newest.size)
        assertEquals("CC:DD", newest[0].bleAddress)
        assertEquals("2026-09-06T09:00:00Z", newest[0].seenAt)
        assertEquals("2026-09-05T18:30:00Z", newest[1].seenAt)
    }

    @Test
    fun `a sighting with no address, or a withdrawn one, is not an answer`() {
        val kept = SightingsCloud.newestPerAddress(
            listOf(
                sighting(null, "2026-09-06T09:00:00Z"),
                // Newer, and withdrawn. Reporting it would place a wearable at
                // a corner it has since left.
                sighting("AA:BB", "2026-09-06T12:00:00Z")
                    .copy(deletedAt = "2026-09-06T13:00:00Z"),
                sighting("AA:BB", "2026-09-01T10:00:00Z")
            )
        )
        assertEquals(listOf("AA:BB"), kept.map { it.bleAddress })
        assertEquals("2026-09-01T10:00:00Z", kept.single().seenAt)
    }

    @Test
    fun `a read of two addresses returns the newest of each, through the client`() = runBlocking {
        val client = FakeCloudClient()
        client.upsert(
            CloudTables.DEVICE_SIGHTINGS,
            listOf(
                sighting("AA:BB", "2026-09-01T10:00:00Z").copy(id = "1"),
                sighting("AA:BB", "2026-09-05T18:30:00Z").copy(id = "2"),
                sighting("ZZ:ZZ", "2026-09-07T10:00:00Z").copy(id = "3")
            ),
            DeviceSightingRow.serializer()
        )

        val result = cloud(client).lastSeen(listOf("AA:BB", "CC:DD"))
        assertTrue(result is CloudResult.Ok)
        val rows = (result as CloudResult.Ok).value
        // ZZ:ZZ was not asked for; CC:DD has never been seen.
        assertEquals(listOf("AA:BB"), rows.map { it.bleAddress })
        assertEquals("2026-09-05T18:30:00Z", rows.single().seenAt)
    }

    private fun sighting(address: String?, seenAt: String) = DeviceSightingRow(
        id = seenAt + address,
        deviceId = deviceId,
        bleAddress = address,
        seenAt = seenAt,
        rssi = -70
    )

    /** Null for JSON null as well as for an absent key. */
    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        if (this == kotlinx.serialization.json.JsonNull) null else content
}
