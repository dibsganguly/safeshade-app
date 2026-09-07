package com.safeshade.repo

import com.safeshade.data.Ride
import com.safeshade.data.RideLogStore
import com.safeshade.data.MAX_RIDE_LOG_ENTRIES
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distance and moving-time accumulation from a stream of GPS fixes.
 *
 * Pure and free of any repository, coroutine or Android dependency, so the
 * arithmetic that actually matters here — does the distance add up, does a
 * bad fix get ignored, does "moving" only count while actually moving — can
 * be unit-tested without going anywhere near `RideLogRepository`.
 */
data class RideAccumulator(
    val distanceM: Double = 0.0,
    val maxSpeedMps: Float = 0f,
    val movingSeconds: Int = 0,
    val samples: Int = 0,
    /** Not for callers to read meaningfully — carried only so the next
     * [RideMath.accumulate] call has something to measure from. */
    val lastLat: Double? = null,
    val lastLon: Double? = null,
    val lastAt: Long? = null
)

object RideMath {

    private const val EARTH_RADIUS_M = 6_371_000.0

    /** How poor a fix's reported accuracy may be before it is dropped, in metres. */
    const val MAX_FIX_ACCURACY_M = 50f

    /** The speed above which a sample counts as "moving", in metres/second. */
    const val MOVING_SPEED_MPS = 0.8f

    /** Great-circle distance between two points, in metres. */
    fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_M * c
    }

    /**
     * Folds one GPS fix into [acc], returning the updated accumulator.
     *
     * A fix whose reported accuracy is worse than [MAX_FIX_ACCURACY_M] is
     * dropped untouched — it counts as neither a sample, nor distance, nor
     * moving time, because a wandering low-accuracy fix would otherwise add
     * fictitious distance to every ride.
     *
     * Moving time is accumulated as the wall-clock gap since the previous
     * *accepted* fix, credited only when [speedMps] is above
     * [MOVING_SPEED_MPS] — a single fast fix does not retroactively make the
     * gap before it "moving", but it does make the gap after it count, once a
     * further fix arrives.
     */
    fun accumulate(
        acc: RideAccumulator,
        lat: Double,
        lon: Double,
        speedMps: Float?,
        accuracyM: Float?,
        at: Long
    ): RideAccumulator {
        if (accuracyM != null && accuracyM > MAX_FIX_ACCURACY_M) return acc

        val addedDistance = if (acc.lastLat != null && acc.lastLon != null) {
            haversineMeters(acc.lastLat, acc.lastLon, lat, lon)
        } else {
            0.0
        }

        val movingDelta = if (speedMps != null && speedMps > MOVING_SPEED_MPS && acc.lastAt != null) {
            ((at - acc.lastAt) / 1000L).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return acc.copy(
            distanceM = acc.distanceM + addedDistance,
            maxSpeedMps = maxOf(acc.maxSpeedMps, speedMps ?: 0f),
            movingSeconds = acc.movingSeconds + movingDelta,
            samples = acc.samples + 1,
            lastLat = lat,
            lastLon = lon,
            lastAt = at
        )
    }
}

/**
 * The ride log: begins and ends a ride around a journey, folding location
 * fixes into it as they arrive.
 *
 * Accumulation is entirely in memory — [addFix] and [begin] are not
 * `suspend` and touch no store — and only [end] writes a finished [Ride]
 * to [RideLogStore]. A per-fix DataStore write (once a minute, for the
 * length of a walk) would rewrite the whole preferences file that often for
 * no reader that needs it before the ride is over; the summary is all any
 * screen shows.
 */
class RideLogRepository(
    private val store: RideLogStore,
    private val scope: CoroutineScope,
    /** Overridable so tests can control `startedAt`/`endedAt`. */
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /** Null until the first store read completes. See `EvidenceRepository`. */
    val rides: StateFlow<List<Ride>?> =
        store.rides.stateIn(scope, SharingStarted.Eagerly, null)

    private val lock = Mutex()

    private data class ActiveRide(
        val id: String,
        val journeyId: String?,
        val startedAt: Long,
        val acc: RideAccumulator = RideAccumulator()
    )

    @Volatile
    private var active: ActiveRide? = null

    /**
     * Starts a new ride, replacing any ride already in progress without
     * logging it — a ride abandoned mid-journey (the previous one never
     * called [end]) is the app's bookkeeping catching up, not a ride worth
     * keeping a truncated record of.
     */
    fun begin(journeyId: String?) {
        active = ActiveRide(id = UUID.randomUUID().toString(), journeyId = journeyId, startedAt = now())
    }

    /** Folds one fix into the ride in progress. A no-op when no ride is active. */
    fun addFix(lat: Double, lon: Double, speedMps: Float?, accuracyM: Float?, at: Long) {
        val current = active ?: return
        active = current.copy(acc = RideMath.accumulate(current.acc, lat, lon, speedMps, accuracyM, at))
    }

    /** Ends the ride in progress and appends its summary to the log. */
    suspend fun end() {
        val finished = active ?: return
        active = null
        val ride = Ride(
            id = finished.id,
            journeyId = finished.journeyId,
            startedAt = finished.startedAt,
            endedAt = now(),
            distanceM = finished.acc.distanceM,
            maxSpeedMps = finished.acc.maxSpeedMps,
            movingSeconds = finished.acc.movingSeconds,
            samples = finished.acc.samples
        )
        lock.withLock {
            val current = store.rides.first()
            store.setRides((current + ride).takeLast(MAX_RIDE_LOG_ENTRIES))
        }
    }

    /** Discards the whole log. Does not touch a ride currently in progress. */
    suspend fun clear() {
        lock.withLock { store.setRides(emptyList()) }
    }
}
