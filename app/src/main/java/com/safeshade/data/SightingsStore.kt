package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

/**
 * Who this phone has walked past, and which of its own devices are lost.
 *
 * ### What a sighting is
 *
 * Every SafeShade wearable advertises the same `SERVICE_UUID`, so a phone
 * scanning for its own device also hears everybody else's. That overhearing is
 * the community last-seen feature: a wearable that has gone missing - or a
 * collar on an animal that has - is found because some stranger's SafeShade
 * phone passed within radio range and said where it was. A [Sighting] is one
 * such pass: an address, how strong it was, when, and where this phone was
 * standing at the time.
 *
 * ### Why its own DataStore file
 *
 * `data/Preferences.kt` declares a private delegate over `safeshade_prefs`, and
 * a second delegate for the same file name does not share the store - it builds
 * a second `DataStore` over one file and throws `IllegalStateException: There
 * are multiple DataStores active for the same file` at runtime, inside a
 * coroutine. So this uses `safeshade_sightings`, exactly as `EvidenceStore`
 * uses `safeshade_evidence`.
 *
 * The shape argues for it too. A sweep appends rows at radio speed and DataStore
 * rewrites its whole file on every `edit`; sharing a file with the medical ID
 * and the zone list would make all of those churn behind every twenty-second
 * sweep.
 *
 * ### Gson, and its one hard rule
 *
 * Same reasoning as the outbox and the evidence store: this blob never leaves
 * the phone, so there is no schema to negotiate. It carries that pattern's rule
 * - **every DTO field below is nullable with a default** - because Gson does not
 * call Kotlin constructors. It allocates a zeroed object through `Unsafe` and
 * assigns only the fields present in the JSON, so a Kotlin default never runs
 * and a non-null property can arrive holding null.
 *
 * ### Why the store is an interface
 *
 * [SightingsStore] has no Android in it, so `SightingsRepository` - which owns
 * the dedupe window, the ring cap and the own-device split, i.e. every rule
 * worth testing - runs in a plain JVM unit test against
 * [InMemorySightingsStore]. [DataStoreSightingsStore] is the only implementation
 * that touches disk.
 */

/**
 * One SafeShade heard by this phone.
 *
 * [lat], [lon] and [accuracyM] are nullable together and for one reason: a
 * sighting recorded while the phone had no fix has no position, and a sighting
 * at 0,0 would place a missing wearable in the Atlantic. Absent is null, and a
 * reader shows no location rather than the wrong one.
 *
 * [reported] is what this phone knows about the *cloud*, not about the device:
 * true only once `SightingsCloud.report` has accepted the row. It starts false
 * so a sighting recorded with no signal is still waiting to be sent when there
 * is some.
 */
data class Sighting(
    val address: String,
    /** The advertised name, or null when the frame carried none. */
    val name: String?,
    val rssi: Int,
    val at: Long,
    /** Where this phone was, or null if it did not know. Never a placeholder. */
    val lat: Double? = null,
    val lon: Double? = null,
    /** Fix accuracy in metres, or null. A report is worth little without it. */
    val accuracyM: Double? = null,
    val reported: Boolean = false
)

/**
 * One of the user's own devices, marked lost.
 *
 * This is a flag the user raised, not a state the app inferred. A wearable out
 * of range is the ordinary case a hundred times a day - somebody leaving their
 * phone on the kitchen table - and treating that as lost would make the word
 * mean nothing.
 *
 * [label] is what to call it in a notification when it turns up ("Dad's band",
 * "Rufus's collar"); [since] is when the user said so, which is what makes a
 * later sighting meaningful rather than just recent.
 */
data class LostMode(
    val address: String,
    val since: Long,
    val label: String
)

/** The sighting ring and the lost list, however they happen to be stored. */
interface SightingsStore {
    val sightings: Flow<List<Sighting>>
    suspend fun setSightings(sightings: List<Sighting>)
    val lost: Flow<List<LostMode>>
    suspend fun setLost(lost: List<LostMode>)
}

/**
 * How many sightings are kept, newest last.
 *
 * A busy street fills this in minutes, and that is fine: the oldest rows are
 * the least useful ones to a search, and every row that mattered has already
 * been reported to the cloud by the time it is evicted.
 */
const val MAX_SIGHTINGS: Int = 200

// ============================================
// The on-disk shapes. Every field nullable; see the file KDoc.
// ============================================

private data class SightingDto(
    val address: String? = null,
    val name: String? = null,
    val rssi: Int? = null,
    val at: Long? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val accuracyM: Double? = null,
    val reported: Boolean? = null
)

private data class LostModeDto(
    val address: String? = null,
    val since: Long? = null,
    val label: String? = null
)

/**
 * The JSON codec, as a pure object.
 *
 * Pure so the round trip can be unit-tested without a `Context`. The cases that
 * matter are a corrupt blob - which must yield an empty list rather than throw
 * inside a DataStore `map`, since a throw there kills every collector of the
 * flow - and a row from an older build missing fields this one reads.
 */
object SightingsJson {

    private val gson = Gson()
    private val sightingListType: Type = object : TypeToken<List<SightingDto>>() {}.type
    private val lostListType: Type = object : TypeToken<List<LostModeDto>>() {}.type

    fun encodeSightings(sightings: List<Sighting>): String = gson.toJson(
        sightings.map {
            SightingDto(
                address = it.address,
                name = it.name,
                rssi = it.rssi,
                at = it.at,
                lat = it.lat,
                lon = it.lon,
                accuracyM = it.accuracyM,
                reported = it.reported
            )
        }
    )

    fun decodeSightings(json: String?): List<Sighting> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<SightingDto?>>(json, sightingListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
        }.getOrDefault(emptyList())
    }

    fun encodeLost(lost: List<LostMode>): String = gson.toJson(
        lost.map { LostModeDto(address = it.address, since = it.since, label = it.label) }
    )

    fun decodeLost(json: String?): List<LostMode> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<LostModeDto?>>(json, lostListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
        }.getOrDefault(emptyList())
    }

    /**
     * A row with no address names nothing that can be searched for, matched
     * against the paired list, or reported, so it is dropped rather than
     * resurrected with an empty string.
     *
     * A position is kept only when it is *complete*. Half a fix - a latitude
     * with no longitude - is not a place, and carrying it forward would let a
     * caller draw a pin from one coordinate and a zero.
     */
    private fun SightingDto.toDomainOrNull(): Sighting? {
        val realAddress = address?.takeIf { it.isNotBlank() } ?: return null
        val hasFix = lat != null && lon != null
        return Sighting(
            address = realAddress,
            name = name?.takeIf { it.isNotBlank() },
            rssi = rssi ?: RSSI_UNKNOWN,
            at = at ?: 0L,
            lat = if (hasFix) lat else null,
            lon = if (hasFix) lon else null,
            accuracyM = if (hasFix) accuracyM else null,
            reported = reported ?: false
        )
    }

    /** No address, or no moment it was raised, and the row cannot be acted on. */
    private fun LostModeDto.toDomainOrNull(): LostMode? {
        val realAddress = address?.takeIf { it.isNotBlank() } ?: return null
        return LostMode(
            address = realAddress,
            since = since ?: 0L,
            label = label?.takeIf { it.isNotBlank() }.orEmpty()
        )
    }

    /** Android's own "no reading" sentinel. Zero would read as full signal. */
    private const val RSSI_UNKNOWN = -127
}

private val Context.sightingsDataStore by preferencesDataStore(name = "safeshade_sightings")

private object SightingsKeys {
    /** Versioned in the name, so a breaking shape change is a new key. */
    val SIGHTINGS = stringPreferencesKey("sightings_json_v1")
    val LOST = stringPreferencesKey("lost_mode_json_v1")
}

/** The real store, over `safeshade_sightings`. */
class DataStoreSightingsStore(context: Context) : SightingsStore {

    private val appContext = context.applicationContext

    override val sightings: Flow<List<Sighting>> = appContext.sightingsDataStore.data
        .map { SightingsJson.decodeSightings(it[SightingsKeys.SIGHTINGS]) }

    override suspend fun setSightings(sightings: List<Sighting>) {
        appContext.sightingsDataStore.edit {
            it[SightingsKeys.SIGHTINGS] = SightingsJson.encodeSightings(sightings)
        }
    }

    override val lost: Flow<List<LostMode>> = appContext.sightingsDataStore.data
        .map { SightingsJson.decodeLost(it[SightingsKeys.LOST]) }

    override suspend fun setLost(lost: List<LostMode>) {
        appContext.sightingsDataStore.edit {
            it[SightingsKeys.LOST] = SightingsJson.encodeLost(lost)
        }
    }
}

/** The same store in memory, for unit tests. */
class InMemorySightingsStore(
    initialSightings: List<Sighting> = emptyList(),
    initialLost: List<LostMode> = emptyList()
) : SightingsStore {

    private val _sightings = MutableStateFlow(initialSightings)
    private val _lost = MutableStateFlow(initialLost)

    override val sightings: Flow<List<Sighting>> = _sightings.asStateFlow()
    override val lost: Flow<List<LostMode>> = _lost.asStateFlow()

    override suspend fun setSightings(sightings: List<Sighting>) {
        _sightings.value = sightings
    }

    override suspend fun setLost(lost: List<LostMode>) {
        _lost.value = lost
    }
}
