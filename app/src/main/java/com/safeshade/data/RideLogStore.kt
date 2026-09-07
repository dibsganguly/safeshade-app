package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import java.util.UUID

/**
 * The ride log: one row per journey, built from the location fixes sampled
 * while it ran.
 *
 * Its own DataStore file, `safeshade_rides`, for the same reason as
 * `EvidenceStore`: `data/Preferences.kt`'s `Context.dataStore` delegate is
 * private to that file, a second delegate for the same file name throws
 * `IllegalStateException` at first use rather than sharing the store, and a
 * ride is written far more often than the medical ID or the zone list —
 * sharing a file would make all of those churn behind a walk.
 *
 * [RideLogStore] is an interface, and [DataStoreRideLogStore] its only
 * Android-backed implementation, so `RideLogRepository` can be built in a
 * plain JVM test against an in-memory fake — see the pattern in
 * `EvidenceStore.kt`.
 */

/** One GPS sample taken while a ride was running. */
data class RideSample(
    val at: Long,
    val lat: Double,
    val lon: Double,
    val speedMps: Float? = null,
    val accuracyM: Float? = null
)

/**
 * One completed (or in-progress) ride: the summary built from the samples
 * folded into it, not the samples themselves — see `RideLogRepository`, which
 * accumulates in memory and never persists a per-sample trail.
 */
data class Ride(
    val id: String = UUID.randomUUID().toString(),
    /** Which journey this ride was walked for, or null. */
    val journeyId: String? = null,
    val startedAt: Long,
    /** Null while the ride is still running. */
    val endedAt: Long? = null,
    val distanceM: Double = 0.0,
    val maxSpeedMps: Float = 0f,
    val movingSeconds: Int = 0,
    val samples: Int = 0
)

/** The ride list, however it happens to be stored. */
interface RideLogStore {
    val rides: Flow<List<Ride>>
    suspend fun setRides(rides: List<Ride>)
}

// ============================================
// The on-disk shape. Every field nullable; see the file KDoc on EvidenceStore
// for why - Gson does not call Kotlin constructors.
// ============================================

private data class RideDto(
    val id: String? = null,
    val journeyId: String? = null,
    val startedAt: Long? = null,
    val endedAt: Long? = null,
    val distanceM: Double? = null,
    val maxSpeedMps: Float? = null,
    val movingSeconds: Int? = null,
    val samples: Int? = null
)

/**
 * The JSON codec, as a pure object — unit-testable without a `Context`, and
 * safe against a corrupt blob or an older row missing a field, matching
 * `EvidenceJson`'s pattern.
 */
object RideLogJson {

    private val gson = Gson()
    private val rideListType: Type = object : TypeToken<List<RideDto>>() {}.type

    fun encode(rides: List<Ride>): String = gson.toJson(rides.map { it.toDto() })

    fun decode(json: String?): List<Ride> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<RideDto?>>(json, rideListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
        }.getOrDefault(emptyList())
    }

    private fun Ride.toDto(): RideDto = RideDto(
        id = id,
        journeyId = journeyId,
        startedAt = startedAt,
        endedAt = endedAt,
        distanceM = distanceM,
        maxSpeedMps = maxSpeedMps,
        movingSeconds = movingSeconds,
        samples = samples
    )

    /** A row with no id points at nothing a later write could ever match, so
     * it is dropped rather than resurrected with a fresh one. */
    private fun RideDto.toDomainOrNull(): Ride? {
        val realId = id?.takeIf { it.isNotBlank() } ?: return null
        return Ride(
            id = realId,
            journeyId = journeyId,
            startedAt = startedAt ?: 0L,
            endedAt = endedAt,
            distanceM = distanceM ?: 0.0,
            maxSpeedMps = maxSpeedMps ?: 0f,
            movingSeconds = movingSeconds ?: 0,
            samples = samples ?: 0
        )
    }
}

/** How many rides are kept before the oldest is dropped. */
const val MAX_RIDE_LOG_ENTRIES: Int = 50

private val Context.rideLogDataStore by preferencesDataStore(name = "safeshade_rides")

private object RideLogKeys {
    val RIDES = stringPreferencesKey("rides_json_v1")
}

/** The real store, over `safeshade_rides`. */
class DataStoreRideLogStore(context: Context) : RideLogStore {

    private val appContext = context.applicationContext

    override val rides: Flow<List<Ride>> =
        appContext.rideLogDataStore.data.map { RideLogJson.decode(it[RideLogKeys.RIDES]) }

    override suspend fun setRides(rides: List<Ride>) {
        appContext.rideLogDataStore.edit {
            it[RideLogKeys.RIDES] = RideLogJson.encode(rides.takeLast(MAX_RIDE_LOG_ENTRIES))
        }
    }
}
