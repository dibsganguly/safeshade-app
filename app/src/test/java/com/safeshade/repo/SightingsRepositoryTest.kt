package com.safeshade.repo

import com.safeshade.data.InMemorySightingsStore
import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.data.MAX_SIGHTINGS
import com.safeshade.data.Sighting
import com.safeshade.data.SightingsJson
import com.safeshade.device.BleSighting
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceAlert
import com.safeshade.device.DeviceLink
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dedupe window, the ring cap, the own-device split and the codec.
 *
 * Runs against [InMemorySightingsStore] and a stub link, which is the whole
 * reason both are interfaces — no Robolectric, no radio, and every assertion is
 * about a rule rather than about a file. Sighting times come from the
 * [BleSighting] rather than a clock, so nothing here depends on how long the
 * test took to run.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SightingsRepositoryTest {

    /**
     * A [DeviceLink] that is nothing but a sightings flow.
     *
     * Written here rather than reusing `FakeDeviceLink`, which lives in the
     * debug source set and is therefore invisible to the release unit-test
     * compilation.
     */
    private class StubLink : DeviceLink {
        val emitted = MutableSharedFlow<BleSighting>(extraBufferCapacity = 64)
        var sweepsStarted = 0
        var lastSweepMs: Long? = null
        var sweepsStopped = 0

        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
        override val deviceName = MutableStateFlow("")
        override val deviceAddress = MutableStateFlow("")
        override val rssi = MutableStateFlow(0)
        override val telemetry = MutableStateFlow(LiveSensorData())
        override val alerts = MutableSharedFlow<DeviceAlert>()
        override val replies = MutableSharedFlow<String>()
        override val acks = MutableSharedFlow<String>()
        override val sightings = emitted

        override fun startScan(preferredAddress: String?) = Unit
        override fun stopScan() = Unit
        override fun startSightingScan(durationMs: Long) {
            sweepsStarted++
            lastSweepMs = durationMs
        }
        override fun stopSightingScan() {
            sweepsStopped++
        }
        override fun disconnect() = Unit
        override fun readRssi() = Unit
        override fun writeWeather(payload: String) = Unit
        override fun writeHealth(payload: String) = Unit
        override fun writeSettings(payload: String) = Unit
        override fun writeLed(pattern: LedPattern) = Unit
        override fun writeExt(tag: String, payload: String) = Unit
        override fun writeGuardianMessage(text: String) = Unit
        override fun writeCompanionReply(text: String) = Unit
        override fun ringDevice() = Unit
        override suspend fun awaitAck(tag: String, timeoutMs: Long) = false
    }

    private fun repo(
        store: InMemorySightingsStore = InMemorySightingsStore(),
        own: Set<String> = emptySet(),
        fix: Triple<Double, Double, Double>? = null,
        now: () -> Long = { 1_000L }
    ): Triple<SightingsRepository, StubLink, InMemorySightingsStore> {
        val link = StubLink()
        val scope = TestScope(UnconfinedTestDispatcher())
        val r = SightingsRepository(
            link = link,
            store = store,
            scope = scope,
            ownAddresses = { own },
            location = { fix },
            now = now
        )
        return Triple(r, link, store)
    }

    private fun seen(address: String, at: Long, rssi: Int = -70, name: String? = "SafeShade") =
        BleSighting(address = address, name = name, rssi = rssi, at = at)

    // ============================================
    // SightingPolicy — the pure rules
    // ============================================

    @Test
    fun `an address never seen before is always recorded`() {
        assertTrue(SightingPolicy.shouldRecord(emptyList(), "AA:BB", 0L))
    }

    @Test
    fun `a second sighting inside the window is not recorded`() {
        val existing = listOf(Sighting(address = "AA:BB", name = null, rssi = -60, at = 1_000L))
        assertFalse(SightingPolicy.shouldRecord(existing, "AA:BB", 1_000L + 60_000L))
    }

    @Test
    fun `a sighting once the window has passed is recorded again`() {
        val existing = listOf(Sighting(address = "AA:BB", name = null, rssi = -60, at = 1_000L))
        val at = 1_000L + SightingPolicy.DEDUPE_WINDOW_MS
        assertTrue(SightingPolicy.shouldRecord(existing, "AA:BB", at))
    }

    @Test
    fun `the window is measured from the newest row, not the first`() {
        val existing = listOf(
            Sighting(address = "AA:BB", name = null, rssi = -60, at = 0L),
            Sighting(address = "AA:BB", name = null, rssi = -60, at = 600_000L)
        )
        assertFalse(SightingPolicy.shouldRecord(existing, "AA:BB", 600_001L))
    }

    @Test
    fun `a different address is unaffected by another's window`() {
        val existing = listOf(Sighting(address = "AA:BB", name = null, rssi = -60, at = 1_000L))
        assertTrue(SightingPolicy.shouldRecord(existing, "CC:DD", 1_100L))
    }

    @Test
    fun `addresses match regardless of case`() {
        val existing = listOf(Sighting(address = "AA:BB", name = null, rssi = -60, at = 1_000L))
        assertFalse(SightingPolicy.shouldRecord(existing, "aa:bb", 1_100L))
    }

    @Test
    fun `the ring drops the oldest, not the newest`() {
        var rows = emptyList<Sighting>()
        repeat(MAX_SIGHTINGS + 20) { i ->
            rows = SightingPolicy.append(
                rows,
                Sighting(address = "AA:$i", name = null, rssi = -60, at = i.toLong())
            )
        }
        assertEquals(MAX_SIGHTINGS, rows.size)
        assertEquals(20L, rows.first().at)
        assertEquals((MAX_SIGHTINGS + 19).toLong(), rows.last().at)
    }

    // ============================================
    // Collecting from the link
    // ============================================

    @Test
    fun `a stranger's advertisement is stored`() = runTest {
        val (_, link, store) = repo(fix = Triple(12.9, 77.6, 8.0))

        link.emitted.emit(seen("AA:BB:CC:DD:EE:FF", at = 500L, rssi = -55))

        val rows = store.sightings.first()
        assertEquals(1, rows.size)
        assertEquals("AA:BB:CC:DD:EE:FF", rows[0].address)
        assertEquals(-55, rows[0].rssi)
        assertEquals(500L, rows[0].at)
        assertEquals(12.9, rows[0].lat!!, 0.0001)
        assertEquals(8.0, rows[0].accuracyM!!, 0.0001)
        assertFalse(rows[0].reported)
    }

    @Test
    fun `no fix stores no position rather than zero`() = runTest {
        val (_, link, store) = repo(fix = null)

        link.emitted.emit(seen("AA:BB", at = 1L))

        val row = store.sightings.first().single()
        assertNull(row.lat)
        assertNull(row.lon)
        assertNull(row.accuracyM)
    }

    @Test
    fun `a repeated advertisement inside the window is stored once`() = runTest {
        val (_, link, store) = repo()

        link.emitted.emit(seen("AA:BB", at = 0L))
        link.emitted.emit(seen("AA:BB", at = 900L))
        link.emitted.emit(seen("AA:BB", at = 60_000L))

        assertEquals(1, store.sightings.first().size)
    }

    @Test
    fun `the same device seen again later is stored again`() = runTest {
        val (_, link, store) = repo()

        link.emitted.emit(seen("AA:BB", at = 0L))
        link.emitted.emit(seen("AA:BB", at = SightingPolicy.DEDUPE_WINDOW_MS))

        assertEquals(2, store.sightings.first().size)
    }

    @Test
    fun `own devices are kept out of the community store`() = runTest {
        val (r, link, store) = repo(own = setOf("AA:BB:CC:DD:EE:FF"))

        link.emitted.emit(seen("AA:BB:CC:DD:EE:FF", at = 700L))

        assertTrue(store.sightings.first().isEmpty())
        assertEquals(mapOf("AA:BB:CC:DD:EE:FF" to 700L), r.ownLastSeen.value)
    }

    @Test
    fun `an own device matches the paired list regardless of case`() = runTest {
        val (r, link, store) = repo(own = setOf("aa:bb:cc:dd:ee:ff"))

        link.emitted.emit(seen("AA:BB:CC:DD:EE:FF", at = 700L))

        assertTrue(store.sightings.first().isEmpty())
        assertEquals(700L, r.ownLastSeen.value["AA:BB:CC:DD:EE:FF"])
    }

    @Test
    fun `own last seen keeps the freshest moment`() = runTest {
        val (r, link, _) = repo(own = setOf("AA:BB"))

        link.emitted.emit(seen("AA:BB", at = 100L))
        link.emitted.emit(seen("AA:BB", at = 900L))

        assertEquals(mapOf("AA:BB" to 900L), r.ownLastSeen.value)
    }

    @Test
    fun `the device being connected to is own, even before the paired list loads`() = runTest {
        // The cold-start race: DataStore has not answered yet, so ownAddresses
        // is empty, and the auto-reconnect scan reports the user's own wearable.
        val (r, link, store) = repo(own = emptySet())
        link.deviceAddress.value = "AA:BB:CC:DD:EE:FF"

        link.emitted.emit(seen("AA:BB:CC:DD:EE:FF", at = 300L))

        assertTrue(store.sightings.first().isEmpty())
        assertEquals(300L, r.ownLastSeen.value["AA:BB:CC:DD:EE:FF"])
    }

    @Test
    fun `pairing a device after construction stops it being reported`() = runTest {
        val own = mutableSetOf<String>()
        val link = StubLink()
        val store = InMemorySightingsStore()
        SightingsRepository(
            link = link,
            store = store,
            scope = TestScope(UnconfinedTestDispatcher()),
            ownAddresses = { own },
            location = { null }
        )

        link.emitted.emit(seen("AA:BB", at = 0L))
        own += "AA:BB"
        link.emitted.emit(seen("AA:BB", at = SightingPolicy.DEDUPE_WINDOW_MS))

        assertEquals(1, store.sightings.first().size)
    }

    @Test
    fun `the store never grows past the cap`() = runTest {
        val (_, link, store) = repo()

        repeat(MAX_SIGHTINGS + 30) { i -> link.emitted.emit(seen("AA:$i", at = i.toLong())) }

        assertEquals(MAX_SIGHTINGS, store.sightings.first().size)
    }

    // ============================================
    // Lost mode
    // ============================================

    @Test
    fun `marking lost records the moment and the label`() = runTest {
        val (r, _, store) = repo(now = { 4_242L })

        r.markLost("aa:bb", "Rufus's collar")

        val row = store.lost.first().single()
        assertEquals("AA:BB", row.address)
        assertEquals(4_242L, row.since)
        assertEquals("Rufus's collar", row.label)
    }

    @Test
    fun `re-marking keeps how long it has been missing`() = runTest {
        var clock = 100L
        val (r, _, store) = repo(now = { clock })

        r.markLost("AA:BB", "Band")
        clock = 900L
        r.markLost("AA:BB", "Dad's band")

        val row = store.lost.first().single()
        assertEquals(100L, row.since)
        assertEquals("Dad's band", row.label)
    }

    @Test
    fun `marking found clears only that device`() = runTest {
        val (r, _, store) = repo()

        r.markLost("AA:BB", "One")
        r.markLost("CC:DD", "Two")
        r.markFound("aa:bb")

        assertEquals(listOf("CC:DD"), store.lost.first().map { it.address })
    }

    // ============================================
    // The cloud handoff
    // ============================================

    @Test
    fun `unreported returns rows the cloud has not taken`() = runTest {
        val (r, link, _) = repo()

        link.emitted.emit(seen("AA:BB", at = 1L))
        link.emitted.emit(seen("CC:DD", at = 2L))

        assertEquals(listOf("AA:BB", "CC:DD"), r.unreported().map { it.address })
    }

    @Test
    fun `marking reported flags the named row and leaves its siblings`() = runTest {
        val (r, link, _) = repo()

        link.emitted.emit(seen("AA:BB", at = 0L))
        link.emitted.emit(seen("AA:BB", at = SightingPolicy.DEDUPE_WINDOW_MS))

        r.markReported(listOf("AA:BB" to 0L))

        assertEquals(listOf(SightingPolicy.DEDUPE_WINDOW_MS), r.unreported().map { it.at })
    }

    @Test
    fun `marking reported ignores a row that is not stored`() = runTest {
        val (r, link, _) = repo()

        link.emitted.emit(seen("AA:BB", at = 1L))
        r.markReported(listOf("ZZ:ZZ" to 99L))

        assertEquals(1, r.unreported().size)
    }

    // ============================================
    // The sweep
    // ============================================

    @Test
    fun `a sweep is listen-only and twenty seconds long`() {
        val (r, link, _) = repo()

        r.startSweep()

        assertEquals(1, link.sweepsStarted)
        assertEquals(20_000L, link.lastSweepMs)
    }

    @Test
    fun `stopping a sweep reaches the link`() {
        val (r, link, _) = repo()

        r.stopSweep()

        assertEquals(1, link.sweepsStopped)
    }

    // ============================================
    // The codec
    // ============================================

    @Test
    fun `sightings survive a round trip`() {
        val rows = listOf(
            Sighting("AA:BB", "SafeShade", -61, 10L, 12.9, 77.6, 8.0, reported = true),
            Sighting("CC:DD", null, -80, 20L)
        )
        assertEquals(rows, SightingsJson.decodeSightings(SightingsJson.encodeSightings(rows)))
    }

    @Test
    fun `a corrupt blob decodes to nothing rather than throwing`() {
        assertTrue(SightingsJson.decodeSightings("{not json").isEmpty())
        assertTrue(SightingsJson.decodeLost("{not json").isEmpty())
        assertTrue(SightingsJson.decodeSightings(null).isEmpty())
    }

    @Test
    fun `a row with no address is dropped`() {
        val json = """[{"rssi":-60,"at":1},{"address":"AA:BB","rssi":-60,"at":2}]"""
        assertEquals(listOf("AA:BB"), SightingsJson.decodeSightings(json).map { it.address })
    }

    @Test
    fun `half a fix is no fix`() {
        val json = """[{"address":"AA:BB","lat":12.9,"at":1}]"""
        val row = SightingsJson.decodeSightings(json).single()
        assertNull(row.lat)
        assertNull(row.lon)
    }

    @Test
    fun `a row missing rssi reads as no signal rather than full`() {
        val json = """[{"address":"AA:BB","at":1}]"""
        assertEquals(-127, SightingsJson.decodeSightings(json).single().rssi)
    }

    @Test
    fun `lost rows survive a round trip and drop the address-less`() {
        val rows = listOf(com.safeshade.data.LostMode("AA:BB", 5L, "Band"))
        assertEquals(rows, SightingsJson.decodeLost(SightingsJson.encodeLost(rows)))
        assertTrue(SightingsJson.decodeLost("""[{"since":1,"label":"x"}]""").isEmpty())
    }
}
