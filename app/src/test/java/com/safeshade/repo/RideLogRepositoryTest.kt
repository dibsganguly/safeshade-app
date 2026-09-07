package com.safeshade.repo

import com.safeshade.data.Ride
import com.safeshade.data.RideLogStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RideLogRepositoryTest {

    /** In-memory [RideLogStore]. No DataStore, no Context, no Android. */
    private class FakeStore : RideLogStore {
        private val _rides = MutableStateFlow<List<Ride>>(emptyList())
        override val rides: Flow<List<Ride>> = _rides
        override suspend fun setRides(rides: List<Ride>) {
            _rides.value = rides
        }

        val current: List<Ride> get() = _rides.value
    }

    @Test
    fun `a ride that is begun and ended appears in the log`() = runTest {
        val store = FakeStore()
        var clock = 1_000L
        val repo = RideLogRepository(store, backgroundScope, now = { clock })

        repo.begin(journeyId = "j1")
        repo.addFix(12.9716, 77.5946, speedMps = 1.5f, accuracyM = 10f, at = 1_000L)
        clock = 61_000L
        repo.addFix(12.9720, 77.5950, speedMps = 1.5f, accuracyM = 10f, at = 61_000L)
        repo.end()

        assertEquals(1, store.current.size)
        val ride = store.current.single()
        assertEquals("j1", ride.journeyId)
        assertEquals(2, ride.samples)
        assertTrue(ride.distanceM > 0.0)
        assertEquals(61_000L, ride.endedAt)
    }

    @Test
    fun `addFix before begin is a no-op`() = runTest {
        val store = FakeStore()
        val repo = RideLogRepository(store, backgroundScope)

        repo.addFix(12.9716, 77.5946, speedMps = 1f, accuracyM = 5f, at = 1_000L)
        repo.end()

        assertTrue(store.current.isEmpty())
    }

    @Test
    fun `end with no active ride writes nothing`() = runTest {
        val store = FakeStore()
        val repo = RideLogRepository(store, backgroundScope)

        repo.end()

        assertTrue(store.current.isEmpty())
    }

    @Test
    fun `begin again without ending drops the unfinished ride`() = runTest {
        val store = FakeStore()
        val repo = RideLogRepository(store, backgroundScope)

        repo.begin(journeyId = "first")
        repo.addFix(0.0, 0.0, speedMps = null, accuracyM = null, at = 1_000L)
        repo.begin(journeyId = "second")
        repo.end()

        assertEquals(1, store.current.size)
        assertEquals("second", store.current.single().journeyId)
    }

    @Test
    fun `clear empties the log without touching a ride in progress`() = runTest {
        val store = FakeStore()
        val repo = RideLogRepository(store, backgroundScope)

        repo.begin(journeyId = "j1")
        repo.addFix(0.0, 0.0, speedMps = null, accuracyM = null, at = 1_000L)
        repo.end()
        assertEquals(1, store.current.size)

        repo.clear()
        assertTrue(store.current.isEmpty())

        repo.begin(journeyId = "j2")
        repo.addFix(0.0, 0.0, speedMps = null, accuracyM = null, at = 2_000L)
        repo.end()
        assertEquals(1, store.current.size)
        assertEquals("j2", store.current.single().journeyId)
    }
}
