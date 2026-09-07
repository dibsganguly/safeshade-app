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
import java.util.UUID

/**
 * The vitals ring and the thresholds it is judged against, on disk.
 *
 * ### Why its own DataStore file
 *
 * `data/Preferences.kt` owns the delegate for `safeshade_prefs` and
 * `cloud/sync/CloudDataStore.kt` owns the one for `safeshade_cloud`. A second
 * delegate for a file that already has one does not share the store — it builds
 * a second `DataStore` over one file and the first read throws
 * `IllegalStateException: There are multiple DataStores active for the same
 * file`, at runtime, inside a coroutine. So this declares a third file,
 * `safeshade_vitals`, and the delegate lives here where its only writer is.
 *
 * It is also the right shape independently. DataStore rewrites its whole file
 * on every `edit`, and vitals are written far more often than a medical ID is
 * read; sharing `safeshade_prefs` would make every profile read churn behind a
 * polling vitals screen.
 *
 * ### Gson, not kotlinx.serialization
 *
 * `data/` is Gson and `cloud/dto/` is `@Serializable`; a class carrying both
 * annotations is the thing this codebase has decided not to have. [VitalsSample]
 * is therefore a plain domain class and [VitalsSampleDto] is its on-disk shape,
 * with **every field nullable and defaulted** — Gson does not call Kotlin
 * constructors, so a default argument never runs and a non-null property can
 * arrive holding null. `data/local/Dtos.kt` explains this at length.
 *
 * ### The interface
 *
 * [VitalsStore] exists so `VitalsRepository` can be unit-tested against an
 * in-memory implementation with no Android, no DataStore and no Robolectric.
 * [DataStoreVitalsStore] is the real one and holds every Android call in this
 * file.
 */

/** One vitals reading, from the wearable or from the phone's health store. */
data class VitalsSample(
    val id: String = UUID.randomUUID().toString(),
    val at: Long = System.currentTimeMillis(),
    /**
     * Every measurement is independently nullable: one sample carries whichever
     * sensors were readable at that moment. Null is absent; zero is a reading.
     */
    val heartRateBpm: Int? = null,
    val spo2Percent: Int? = null,
    val tempC: Float? = null,
    /** Ambient sound level, for the loud-environment warning. */
    val ambientDb: Double? = null,
    /**
     * How it was obtained: [SOURCE_DEVICE] or [SOURCE_PHONE], and nothing else.
     *
     * The server's `vitals_samples.source` carries a check constraint, so a
     * third value here is a row that is silently rejected at drain time with
     * nothing on this phone to show for it. There is deliberately no value
     * meaning "made up" — a sample that was not measured is not written.
     */
    val source: String = SOURCE_PHONE,
    val wearerId: String? = null
) {
    /** True when nothing was actually measured. Such a sample is not recorded. */
    val isEmpty: Boolean
        get() = heartRateBpm == null && spo2Percent == null && tempC == null && ambientDb == null

    /**
     * True when this carries a body reading rather than only an ambient one.
     *
     * The distinction matters to `VitalsRepository.recordFromPhone`: an ambient
     * sound level is stamped with the wall clock and a Health Connect reading
     * with the moment it was measured, so the two must not share a timeline.
     */
    val hasVitals: Boolean
        get() = heartRateBpm != null || spo2Percent != null || tempC != null

    companion object {
        /** Read off the wearable's telemetry. */
        const val SOURCE_DEVICE = "device"

        /** Read out of the phone's Health Connect store. */
        const val SOURCE_PHONE = "phone"

        /** The two values the server's check constraint accepts. */
        val SOURCES: Set<String> = setOf(SOURCE_DEVICE, SOURCE_PHONE)
    }
}

/**
 * The lines a reading has to stay inside.
 *
 * Editable because "high" is a property of a person, not of the species: a
 * resting 45 is normal for one wearer and alarming for another. The defaults
 * are the widely used adult resting ranges, and they are the starting point,
 * not a diagnosis — nothing in this app tells anyone they are ill.
 */
data class VitalsThresholds(
    val hrLow: Int = 40,
    val hrHigh: Int = 130,
    val spo2Low: Int = 90,
    val tempHigh: Float = 38.0f
) {
    /**
     * Which lines [sample] crosses. Pure, total, and never throws.
     *
     * **Boundaries are inclusive of the named value being fine.** A heart rate
     * of exactly `hrLow` is not low and exactly `hrHigh` is not high; a
     * temperature of exactly `tempHigh` is not a fever. The alternative would
     * make the default 38.0 flag a reading of 38.0 that a thermometer would
     * call borderline, and a threshold a user typed should mean "past this",
     * not "at this". Null measurements produce no flag at all — an absent
     * reading is never evidence of anything.
     */
    fun assess(sample: VitalsSample): List<VitalsFlag> = buildList {
        sample.heartRateBpm?.let {
            if (it < hrLow) add(VitalsFlag.HR_LOW)
            if (it > hrHigh) add(VitalsFlag.HR_HIGH)
        }
        sample.spo2Percent?.let { if (it < spo2Low) add(VitalsFlag.SPO2_LOW) }
        sample.tempC?.let { if (it > tempHigh) add(VitalsFlag.TEMP_HIGH) }
    }
}

/** A threshold a reading crossed. */
enum class VitalsFlag { HR_LOW, HR_HIGH, SPO2_LOW, TEMP_HIGH }

/**
 * Where the ring and the thresholds are kept.
 *
 * Deliberately dumb: read, write, no policy. The cap, the dedupe and the sync
 * hook all live in `VitalsRepository`, which is where they can be tested.
 */
interface VitalsStore {
    val samples: Flow<List<VitalsSample>>
    val thresholds: Flow<VitalsThresholds>
    suspend fun setSamples(list: List<VitalsSample>)
    suspend fun setThresholds(thresholds: VitalsThresholds)
}

// ============================================
// Serialisation — pure, and tested as such
// ============================================

/** The on-disk shape of [VitalsSample]. Every field nullable. See the file KDoc. */
internal data class VitalsSampleDto(
    val id: String? = null,
    val at: Long? = null,
    val heartRateBpm: Int? = null,
    val spo2Percent: Int? = null,
    val tempC: Float? = null,
    val ambientDb: Double? = null,
    val source: String? = null,
    val wearerId: String? = null
)

/** The on-disk shape of [VitalsThresholds]. Every field nullable. */
internal data class VitalsThresholdsDto(
    val hrLow: Int? = null,
    val hrHigh: Int? = null,
    val spo2Low: Int? = null,
    val tempHigh: Float? = null
)

/**
 * JSON in, JSON out, with no Android and no DataStore anywhere near it.
 *
 * Split out as a plain object precisely so the round trip can be exercised in a
 * JVM unit test — the same reason `parseOverpass` is a free function rather
 * than a method on `OverpassClient`.
 *
 * Every decode is total. A corrupt or half-written blob yields an empty list or
 * the default thresholds, never an exception: the one place this is called from
 * is inside a Flow operator, where a throw tears down every collector at once.
 */
object VitalsJson {

    private val gson = Gson()
    private val sampleListType: Type = object : TypeToken<List<VitalsSampleDto>>() {}.type

    fun encodeSamples(list: List<VitalsSample>): String = gson.toJson(
        list.map {
            VitalsSampleDto(
                id = it.id,
                at = it.at,
                heartRateBpm = it.heartRateBpm,
                spo2Percent = it.spo2Percent,
                tempC = it.tempC,
                ambientDb = it.ambientDb,
                source = it.source,
                wearerId = it.wearerId
            )
        }
    )

    /**
     * A row with no id, or with a `source` the server would reject, is dropped
     * rather than repaired. Guessing a source turns an unreadable row into a
     * confident claim about where a heart rate came from, which is the one
     * thing this table must never do.
     */
    fun decodeSamples(json: String?): List<VitalsSample> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<VitalsSampleDto?>>(json, sampleListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { dto ->
                    val id = dto.id?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val source = dto.source?.takeIf { it in VitalsSample.SOURCES }
                        ?: return@mapNotNull null
                    VitalsSample(
                        id = id,
                        at = dto.at ?: 0L,
                        heartRateBpm = dto.heartRateBpm,
                        spo2Percent = dto.spo2Percent,
                        tempC = dto.tempC,
                        ambientDb = dto.ambientDb,
                        source = source,
                        wearerId = dto.wearerId?.takeIf { it.isNotBlank() }
                    )
                }
        }.getOrDefault(emptyList())
    }

    fun encodeThresholds(t: VitalsThresholds): String = gson.toJson(
        VitalsThresholdsDto(t.hrLow, t.hrHigh, t.spo2Low, t.tempHigh)
    )

    /** Field by field, so a blob written before a field existed keeps the rest. */
    fun decodeThresholds(json: String?): VitalsThresholds {
        if (json.isNullOrBlank()) return VitalsThresholds()
        val dto = runCatching { gson.fromJson(json, VitalsThresholdsDto::class.java) }.getOrNull()
            ?: return VitalsThresholds()
        val default = VitalsThresholds()
        return VitalsThresholds(
            hrLow = dto.hrLow ?: default.hrLow,
            hrHigh = dto.hrHigh ?: default.hrHigh,
            spo2Low = dto.spo2Low ?: default.spo2Low,
            tempHigh = dto.tempHigh ?: default.tempHigh
        )
    }
}

// ============================================
// The real store
// ============================================

/** The one delegate for the file `safeshade_vitals`. See the file KDoc. */
private val Context.vitalsDataStore by preferencesDataStore(name = "safeshade_vitals")

private object VitalsKeys {
    /** Versioned in the name, so a breaking shape change is a new key. */
    val SAMPLES = stringPreferencesKey("vitals_samples_json_v1")
    val THRESHOLDS = stringPreferencesKey("vitals_thresholds_json_v1")
}

/** [VitalsStore] over DataStore. Holds every Android call in this file. */
class DataStoreVitalsStore(context: Context) : VitalsStore {

    private val appContext = context.applicationContext

    override val samples: Flow<List<VitalsSample>> =
        appContext.vitalsDataStore.data.map { VitalsJson.decodeSamples(it[VitalsKeys.SAMPLES]) }

    override val thresholds: Flow<VitalsThresholds> =
        appContext.vitalsDataStore.data.map { VitalsJson.decodeThresholds(it[VitalsKeys.THRESHOLDS]) }

    override suspend fun setSamples(list: List<VitalsSample>) {
        appContext.vitalsDataStore.edit { it[VitalsKeys.SAMPLES] = VitalsJson.encodeSamples(list) }
    }

    override suspend fun setThresholds(thresholds: VitalsThresholds) {
        appContext.vitalsDataStore.edit {
            it[VitalsKeys.THRESHOLDS] = VitalsJson.encodeThresholds(thresholds)
        }
    }
}

/**
 * An in-memory [VitalsStore], for unit tests and for any surface that must run
 * without a file system. Nothing is persisted and nothing pretends to be.
 */
class InMemoryVitalsStore(
    initialSamples: List<VitalsSample> = emptyList(),
    initialThresholds: VitalsThresholds = VitalsThresholds()
) : VitalsStore {

    private val _samples = MutableStateFlow(initialSamples)
    private val _thresholds = MutableStateFlow(initialThresholds)

    override val samples: Flow<List<VitalsSample>> = _samples.asStateFlow()
    override val thresholds: Flow<VitalsThresholds> = _thresholds.asStateFlow()

    override suspend fun setSamples(list: List<VitalsSample>) {
        _samples.value = list
    }

    override suspend fun setThresholds(thresholds: VitalsThresholds) {
        _thresholds.value = thresholds
    }
}
