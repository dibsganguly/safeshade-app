package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.InMemoryVitalsStore
import com.safeshade.data.VitalsFlag
import com.safeshade.data.VitalsSample
import com.safeshade.data.VitalsThresholds
import com.safeshade.platform.VitalsResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The ring, the dedupe and the sync hook.
 *
 * Runs against [InMemoryVitalsStore] rather than DataStore, which is the whole
 * reason `VitalsStore` is an interface — no Robolectric, no Android, and the
 * assertions are about policy rather than about a file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VitalsRepositoryTest {

    /** Records what was queued for the cloud, in order. */
    private class RecordingHooks : SyncHooks {
        val upserts = mutableListOf<Pair<String, String>>()
        val deletes = mutableListOf<Pair<String, String>>()
        override suspend fun onUpsert(table: String, recordId: String) {
            upserts += table to recordId
        }
        override suspend fun onDelete(table: String, recordId: String) {
            deletes += table to recordId
        }
    }

    private fun repo(
        store: InMemoryVitalsStore = InMemoryVitalsStore(),
        hooks: SyncHooks = SyncHooks.None
    ): Pair<VitalsRepository, InMemoryVitalsStore> {
        val scope = TestScope(UnconfinedTestDispatcher())
        return VitalsRepository(store, scope, hooks) to store
    }

    private fun sample(id: String, at: Long, hr: Int? = 70, source: String = VitalsSample.SOURCE_DEVICE) =
        VitalsSample(id = id, at = at, heartRateBpm = hr, source = source)

    // ============================================
    // record()
    // ============================================

    @Test
    fun `a measured sample is stored and queued`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)

        r.record(sample("a", 10L))

        assertEquals(listOf("a"), store.samples.first().map { it.id })
        assertEquals(listOf(CloudTables.VITALS_SAMPLES to "a"), hooks.upserts)
    }

    @Test
    fun `a sample with nothing measured is not stored`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)

        r.record(VitalsSample(id = "empty", at = 1L))

        assertTrue(store.samples.first().isEmpty())
        assertTrue(hooks.upserts.isEmpty())
    }

    @Test
    fun `a source the server would reject is refused here`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)

        r.record(sample("x", 1L, source = "simulated"))

        assertTrue(store.samples.first().isEmpty())
        assertTrue(hooks.upserts.isEmpty())
    }

    @Test
    fun `the ring is capped at CAP, oldest dropped first`() = runTest {
        val (r, store) = repo()

        repeat(VitalsRepository.CAP + 25) { i ->
            r.record(sample("s$i", i.toLong()))
        }

        val stored = store.samples.first()
        assertEquals(VitalsRepository.CAP, stored.size)
        assertEquals("s25", stored.first().id)
        assertEquals("s${VitalsRepository.CAP + 24}", stored.last().id)
    }

    // ============================================
    // recordFromPhone()
    // ============================================

    @Test
    fun `phone readings are stored with the phone source and the measured time`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)

        val written = r.recordFromPhone(
            VitalsResult.Readings(
                heartRateBpm = 88,
                heartRateAt = 5_000L,
                spo2Percent = 97,
                spo2At = 7_000L,
                bodyTempC = 36.9f,
                bodyTempAt = 6_000L
            ),
            wearerId = "w1"
        )

        assertNotNull(written)
        val stored = store.samples.first().single()
        assertEquals(VitalsSample.SOURCE_PHONE, stored.source)
        assertEquals(7_000L, stored.at)
        assertEquals(88, stored.heartRateBpm)
        assertEquals("w1", stored.wearerId)
        assertEquals(listOf(CloudTables.VITALS_SAMPLES to stored.id), hooks.upserts)
    }

    @Test
    fun `readings with nothing in them are skipped`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)

        val written = r.recordFromPhone(VitalsResult.Readings(), wearerId = null)

        assertNull(written)
        assertTrue(store.samples.first().isEmpty())
        assertTrue(hooks.upserts.isEmpty())
    }

    @Test
    fun `the same poll answered twice writes one sample`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)
        val readings = VitalsResult.Readings(heartRateBpm = 71, heartRateAt = 9_000L)

        assertNotNull(r.recordFromPhone(readings, null))
        assertNull(r.recordFromPhone(readings, null))
        assertNull(r.recordFromPhone(readings, null))

        assertEquals(1, store.samples.first().size)
        assertEquals(1, hooks.upserts.size)
    }

    @Test
    fun `a genuinely newer reading is written`() = runTest {
        val (r, store) = repo()

        r.recordFromPhone(VitalsResult.Readings(heartRateBpm = 71, heartRateAt = 9_000L), null)
        r.recordFromPhone(VitalsResult.Readings(heartRateBpm = 73, heartRateAt = 9_001L), null)

        assertEquals(listOf(71, 73), store.samples.first().map { it.heartRateBpm })
    }

    @Test
    fun `a device sample does not suppress the next phone sample`() = runTest {
        val (r, store) = repo()

        // The dedupe is scoped to phone samples: a device reading with a later
        // clock must not make the health store look stale.
        r.record(sample("dev", 100_000L))
        val written = r.recordFromPhone(
            VitalsResult.Readings(heartRateBpm = 66, heartRateAt = 50_000L), null
        )

        assertNotNull(written)
        assertEquals(2, store.samples.first().size)
    }

    // ============================================
    // ambient, clear, thresholds
    // ============================================

    @Test
    fun `an ambient level is a phone sample with no vitals on it`() = runTest {
        val (r, store) = repo()

        r.recordAmbientDb(88.5, "w2")

        val stored = store.samples.first().single()
        assertEquals(88.5, stored.ambientDb!!, 0.0001)
        assertNull(stored.heartRateBpm)
        assertEquals(VitalsSample.SOURCE_PHONE, stored.source)
        assertEquals("w2", stored.wearerId)
    }

    @Test
    fun `an ambient sample does not suppress a later health reading`() = runTest {
        val (r, store) = repo()

        // Ambient rows are stamped with the wall clock; a Health Connect
        // reading carries the moment it was measured, which is earlier. If the
        // ambient row set the dedupe baseline, every real reading would be
        // dropped until one arrived newer than the last microphone check.
        val now = System.currentTimeMillis()
        r.recordAmbientDb(72.0, null)
        val written = r.recordFromPhone(
            VitalsResult.Readings(heartRateBpm = 69, heartRateAt = now - 60_000L), null
        )

        assertNotNull(written)
        assertEquals(2, store.samples.first().size)
    }

    @Test
    fun `latest is the newest sample, whatever order they arrived in`() = runTest {
        val (r, _) = repo()
        assertNull(r.latest())

        r.record(sample("late", 900L, hr = 80))
        r.record(sample("early", 100L, hr = 60))

        assertEquals("late", r.latest()?.id)
    }

    @Test
    fun `clear empties the ring and tombstones every row`() = runTest {
        val hooks = RecordingHooks()
        val (r, store) = repo(hooks = hooks)
        r.record(sample("a", 1L))
        r.record(sample("b", 2L))

        r.clear()

        assertTrue(store.samples.first().isEmpty())
        assertEquals(
            listOf(CloudTables.VITALS_SAMPLES to "a", CloudTables.VITALS_SAMPLES to "b"),
            hooks.deletes
        )
    }

    @Test
    fun `thresholds are persisted and are what latestFlags applies`() = runTest {
        val (r, store) = repo()
        r.record(sample("a", 1L, hr = 105))

        assertTrue(r.latestFlags.first().isEmpty())

        r.setThresholds(VitalsThresholds(hrHigh = 100))

        assertEquals(VitalsThresholds(hrHigh = 100), store.thresholds.first())
        assertEquals(listOf(VitalsFlag.HR_HIGH), r.latestFlags.first())
    }

    @Test
    fun `latestFlags judges the newest sample, whatever order they arrived in`() = runTest {
        val (r, _) = repo()

        r.record(sample("late", 500L, hr = 150))
        r.record(sample("early", 100L, hr = 70))

        assertEquals(listOf(VitalsFlag.HR_HIGH), r.latestFlags.first())
    }

    @Test
    fun `latestFlags is empty when nothing has been measured`() = runTest {
        val (r, _) = repo()

        assertTrue(r.latestFlags.first().isEmpty())
    }
}
