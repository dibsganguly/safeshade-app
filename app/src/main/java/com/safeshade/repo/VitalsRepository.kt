package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.VitalsFlag
import com.safeshade.data.VitalsSample
import com.safeshade.data.VitalsStore
import com.safeshade.data.VitalsThresholds
import com.safeshade.platform.VitalsResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The vitals history: what was measured, when, by what, and whether it was
 * inside the lines.
 *
 * ### What this class refuses to do
 *
 * It never writes a sample that was not measured. [recordFromPhone] returns
 * without writing when Health Connect had nothing to give, and there is no
 * source value meaning "made up" — [VitalsSample.SOURCES] is exactly `device`
 * and `phone`, matching the check constraint on `vitals_samples.source`. A
 * screen with no samples shows a dash, which is true, rather than a plausible
 * number, which is not.
 *
 * ### Why [recordFromPhone] dedupes
 *
 * Health Connect is *polled*: the vitals screen asks for the newest readings
 * whenever it opens or the user taps Read now. Nothing on the phone's side
 * changes between two such calls, so the store hands back the same newest heart
 * rate every time. Writing that blindly would fill the 500-sample ring with one
 * reading repeated and queue 500 identical upserts against the outbox within a
 * minute. So a phone sample is skipped unless something newer than the last
 * phone sample has actually been written to the health store.
 *
 * Device samples are not deduped this way: they arrive pushed, one per
 * telemetry frame, and each one is a genuinely new measurement.
 *
 * ### The ring
 *
 * Oldest first, capped at [CAP]. Vitals are the highest-frequency record in the
 * app and an uncapped list in a DataStore blob would be rewritten in full on
 * every single write.
 */
class VitalsRepository(
    private val store: VitalsStore,
    private val scope: CoroutineScope,
    /** See [SyncHooks]. Nothing is queued for the cloud without one. */
    private val hooks: SyncHooks = SyncHooks.None
) {

    /**
     * Null until the first read completes. See `ProfileRepository`: a screen
     * cannot tell "no vitals recorded" from "not loaded yet" if both are an
     * empty list, and the first draws an empty state while the second must not.
     */
    val samples: StateFlow<List<VitalsSample>?> =
        store.samples.stateIn(scope, SharingStarted.Eagerly, null)

    /** The lines readings are judged against. Defaults until the first read. */
    val thresholds: StateFlow<VitalsThresholds> =
        store.thresholds.stateIn(scope, SharingStarted.Eagerly, VitalsThresholds())

    /**
     * What the newest sample crosses, under the thresholds in force now.
     *
     * A flow rather than a value because both halves move independently: a new
     * sample arrives, or the user edits a threshold, and either has to change
     * what the screen says. Empty when there is no sample yet — an unknown
     * reading is never evidence of anything.
     */
    val latestFlags: Flow<List<VitalsFlag>> =
        combine(store.samples, store.thresholds) { list, t ->
            val newest = list.maxByOrNull { it.at } ?: return@combine emptyList()
            t.assess(newest)
        }

    /** Serialises read-modify-write on the ring. See `SafetyRepository`. */
    private val lock = Mutex()

    /**
     * Appends one measured sample.
     *
     * An empty sample - every measurement null - is dropped rather than stored,
     * because a row of four dashes is a record of nothing that would still take
     * a slot in the ring and a slot in the outbox.
     */
    suspend fun record(sample: VitalsSample) {
        if (sample.isEmpty) return
        // A source outside the two the server accepts would be queued, pushed,
        // and rejected by the check constraint with nothing on this phone to
        // show for it. Better to refuse it here, where the caller is.
        if (sample.source !in VitalsSample.SOURCES) return

        lock.withLock {
            val current = store.samples.first()
            store.setSamples(capped(current + sample))
        }
        hooks.onUpsert(CloudTables.VITALS_SAMPLES, sample.id)
    }

    /**
     * Records what Health Connect just returned, if it is worth recording.
     *
     * @return the sample written, or null when nothing was — no measurements at
     *   all, or nothing newer than what is already stored. Null is a normal
     *   outcome and not a failure; the caller has nothing to report.
     */
    suspend fun recordFromPhone(
        readings: VitalsResult.Readings,
        wearerId: String?
    ): VitalsSample? {
        if (readings.isEmpty) return null

        // The moment the newest of these was actually measured. Falling back to
        // "now" only when the provider gave no timestamps at all, which would
        // otherwise stamp a reading from this morning as current.
        val measuredAt = readings.newestAt ?: System.currentTimeMillis()

        val sample = VitalsSample(
            at = measuredAt,
            heartRateBpm = readings.heartRateBpm,
            spo2Percent = readings.spo2Percent,
            tempC = readings.bodyTempC,
            source = VitalsSample.SOURCE_PHONE,
            wearerId = wearerId
        )

        val written = lock.withLock {
            val current = store.samples.first()
            // Only phone samples that carry a *vitals* reading count as the
            // baseline. An ambient-sound sample is also source=phone but is
            // stamped with the wall clock, while a Health Connect reading
            // carries the moment it was measured, which is in the past. Letting
            // an ambient row set the baseline would silently drop every real
            // reading until the health store produced one newer than the last
            // microphone check - which is not a dedupe, it is data loss.
            val lastPhoneAt = current
                .filter { it.source == VitalsSample.SOURCE_PHONE && it.hasVitals }
                .maxOfOrNull { it.at }
            // See the class KDoc: the same poll returns the same readings.
            if (lastPhoneAt != null && measuredAt <= lastPhoneAt) return@withLock false
            store.setSamples(capped(current + sample))
            true
        }
        if (!written) return null

        hooks.onUpsert(CloudTables.VITALS_SAMPLES, sample.id)
        return sample
    }

    /**
     * Records an ambient sound level.
     *
     * Its own entry point rather than a field on [record] because it comes from
     * a different sensor on a different schedule - the phone's microphone,
     * during the loud-environment check - and stamping it onto a heart-rate
     * sample would claim the two were measured together.
     */
    suspend fun recordAmbientDb(db: Double, wearerId: String?) {
        record(
            VitalsSample(
                ambientDb = db,
                source = VitalsSample.SOURCE_PHONE,
                wearerId = wearerId
            )
        )
    }

    /**
     * The newest sample, or null when there is none.
     *
     * Reads the loaded value rather than suspending: this is called from
     * composition and from the alert path, and neither can wait on a disk read.
     * Null before the first load, which is the same answer as "nothing yet" and
     * is drawn the same way - a dash.
     */
    fun latest(): VitalsSample? = samples.value?.maxByOrNull { it.at }

    /** Replaces the thresholds. Not queued for the cloud: they are per-phone. */
    suspend fun setThresholds(t: VitalsThresholds) = store.setThresholds(t)

    /**
     * Drops the whole history.
     *
     * One tombstone per sample, not a single "it is empty now" message: there
     * is no such message in the protocol, and a phone that has been offline
     * would otherwise never learn these rows are gone. Same rule as
     * `ZoneRepository.clearZones`.
     */
    suspend fun clear() {
        val removed = lock.withLock {
            val current = store.samples.first()
            store.setSamples(emptyList())
            current
        }
        removed.forEach { hooks.onDelete(CloudTables.VITALS_SAMPLES, it.id) }
    }

    /** Oldest first, newest kept. */
    private fun capped(list: List<VitalsSample>): List<VitalsSample> =
        if (list.size <= CAP) list else list.takeLast(CAP)

    companion object {
        /**
         * How many samples are kept.
         *
         * At the polling rate the vitals screen uses this is days of history,
         * and the blob stays a few tens of kilobytes - which matters because
         * DataStore rewrites the whole file on every append.
         */
        const val CAP = 500
    }
}
