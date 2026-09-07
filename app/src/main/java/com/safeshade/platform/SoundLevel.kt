package com.safeshade.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A sound-level meter built on the phone's own microphone.
 *
 * ### Read this before showing a number to anybody
 *
 * The dBFS figure is exact: it is arithmetic on the samples the microphone
 * delivered, and two readings from the same phone are comparable with each
 * other. The **SPL figure is not**. A phone is not a sound-level meter. Its
 * microphone has no calibration certificate, its response is shaped by the
 * case, the OEM's automatic gain control moves the floor under the reading
 * while it is being taken, and the offset applied here ([SPL_OFFSET_DB]) is a
 * single constant standing in for a curve that differs per handset.
 *
 * So the SPL number is an approximation and is worth about ±10 dB. That is not
 * a caveat to bury: it is the difference between "this workshop is loud" and
 * "this workshop is over the legal exposure limit", and only the first of
 * those is a claim this class can support. [CALIBRATION_NOTE] carries the
 * sentence for the UI to print verbatim - print it beside the number, not
 * behind an info button.
 */
class SoundLevelMeter(private val context: Context) {

    private var record: AudioRecord? = null
    private var readerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _reading = MutableStateFlow<SoundReading?>(null)

    /** The latest window's reading, or null before the first one lands. */
    val reading: StateFlow<SoundReading?> = _reading.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /**
     * Opens the microphone and starts metering.
     *
     * @return false, with the reason logged, when `RECORD_AUDIO` is not
     *   granted or when `AudioRecord` will not initialise (another app holds
     *   the microphone, or the OEM rejects this configuration). The caller
     *   gets a plain false, never an exception, and must say something honest
     *   rather than draw a meter reading zero.
     */
    fun start(): Boolean {
        if (_running.value) return true

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Sound level: RECORD_AUDIO is not granted, the meter cannot start")
            return false
        }

        val minBuffer = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            Log.w(TAG, "Sound level: this device rejected 16 kHz mono PCM16 (min buffer $minBuffer)")
            return false
        }

        // One window of samples, or the platform minimum if that is larger.
        val windowSamples = SAMPLE_RATE_HZ * WINDOW_MS / 1000
        val bufferBytes = maxOf(minBuffer, windowSamples * 2 * 2)

        val recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE_HZ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferBytes
            )
        } catch (e: Exception) {
            // SecurityException on a revoked grant, IllegalArgumentException on
            // a configuration the device will not take.
            Log.e(TAG, "Sound level: could not open the microphone", e)
            return false
        }

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "Sound level: the microphone is busy or unavailable")
            recorder.release()
            return false
        }

        return try {
            recorder.startRecording()
            record = recorder
            _running.value = true
            readerJob = scope.launch { readLoop(recorder, windowSamples) }
            true
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Sound level: the microphone would not start", e)
            recorder.release()
            false
        }
    }

    private suspend fun readLoop(recorder: AudioRecord, windowSamples: Int) {
        val buffer = ShortArray(windowSamples)
        while (scope.isActive && _running.value) {
            val read = try {
                recorder.read(buffer, 0, buffer.size)
            } catch (e: Exception) {
                Log.w(TAG, "Sound level: read failed, stopping the meter", e)
                break
            }
            if (read <= 0) {
                // ERROR_INVALID_OPERATION / ERROR_DEAD_OBJECT: the record was
                // stopped under us. Nothing to recover, and looping on it
                // would spin a core.
                break
            }
            val window = if (read == buffer.size) buffer else buffer.copyOf(read)
            val rms = SoundMath.rmsOf(window)
            val dbfs = SoundMath.dbfsOf(rms)
            _reading.value = SoundReading(
                dbfsRounded = if (dbfs.isFinite()) dbfs.roundToInt() else SILENT_DBFS,
                approxDbSpl = SoundMath.approxSplOf(dbfs),
                at = System.currentTimeMillis()
            )
        }
    }

    /** Closes the microphone. Safe to call when nothing is running. */
    fun stop() {
        _running.value = false
        readerJob?.cancel()
        readerJob = null
        record?.let { r ->
            runCatching { if (r.recordingState == AudioRecord.RECORDSTATE_RECORDING) r.stop() }
            runCatching { r.release() }
        }
        record = null
    }

    /** Releases the coroutine scope as well. The meter cannot be restarted after this. */
    fun release() {
        stop()
        scope.cancel()
    }

    companion object {
        private const val TAG = "SoundLevelMeter"
        private const val SAMPLE_RATE_HZ = 16_000
        private const val WINDOW_MS = 250
        private const val SILENT_DBFS = -96

        /**
         * The sentence the UI prints beside any SPL number, verbatim.
         *
         * Verbatim and as a constant so that it cannot drift into a softer
         * wording on one screen and a stronger one on another, and so that
         * nothing renders the number without something in reach to print
         * next to it.
         */
        const val CALIBRATION_NOTE: String =
            "Uncalibrated: a phone microphone is not a sound-level meter, so treat this as " +
                "roughly right to within about 10 dB, not as a measurement."
    }
}

/** One 250 ms window of the microphone, measured. */
data class SoundReading(
    /** Exact, and comparable between readings from the same phone. */
    val dbfsRounded: Int,
    /** Approximate. See [SoundLevelMeter.CALIBRATION_NOTE]. */
    val approxDbSpl: Int,
    val at: Long
)

/**
 * The arithmetic, with no Android in it.
 *
 * Split out so the conversion can be tested against known signals - silence,
 * full scale, and a sine at a known level - rather than against whatever the
 * room happened to sound like.
 */
object SoundMath {

    /** Full scale for signed 16-bit PCM. */
    const val FULL_SCALE: Double = 32768.0

    /** The floor and ceiling of the approximate SPL scale. */
    const val MIN_SPL: Int = 30

    /** See [MIN_SPL]. */
    const val MAX_SPL: Int = 120

    /**
     * The single constant standing in for a per-handset calibration curve.
     * See [SoundLevelMeter]'s class doc for why this is an approximation.
     */
    const val SPL_OFFSET_DB: Int = 90

    /** Root mean square of one window of samples. Empty window is silence. */
    fun rmsOf(shorts: ShortArray): Double {
        if (shorts.isEmpty()) return 0.0
        var sum = 0.0
        for (s in shorts) {
            val v = s.toDouble()
            sum += v * v
        }
        return sqrt(sum / shorts.size)
    }

    /**
     * dBFS for an RMS in sample units.
     *
     * Digital silence has no logarithm, so it returns
     * [Double.NEGATIVE_INFINITY] rather than a made-up floor - the flooring is
     * [approxSplOf]'s job, and doing it here would make silence
     * indistinguishable from a very quiet room at the boundary.
     */
    fun dbfsOf(rms: Double): Double {
        if (rms <= 0.0) return Double.NEGATIVE_INFINITY
        return 20.0 * log10(rms / FULL_SCALE)
    }

    /**
     * The approximate SPL for a dBFS figure: the offset applied, then clamped
     * to a range a phone microphone can plausibly report. A non-finite input
     * (silence) reads as the floor.
     */
    fun approxSplOf(dbfs: Double): Int {
        if (!dbfs.isFinite()) return MIN_SPL
        return (dbfs + SPL_OFFSET_DB).roundToInt().coerceIn(MIN_SPL, MAX_SPL)
    }
}

/** What [LoudEnvironment.assess] concluded about the last minute. */
sealed interface LoudVerdict {
    /** Measured, and not loud enough for long enough. */
    data object Quiet : LoudVerdict

    /** Loud, and has been since [sinceMs]. [peakDb] is the loudest reading in the window. */
    data class Loud(val sinceMs: Long, val peakDb: Int) : LoudVerdict

    /**
     * Nothing measured, or not measured for long enough to say. Distinct from
     * [Quiet] on purpose: "we did not listen" and "it is not loud" are
     * different answers, and a UI that drew the second for the first would be
     * reassuring somebody on the strength of no data at all.
     */
    data object NoData : LoudVerdict
}

/**
 * Whether the wearer has been somewhere loud long enough to be worth saying so.
 *
 * ### Why sustain, and why the median
 *
 * A door slamming is 100 dB for a quarter of a second and means nothing. What
 * matters for hearing is a level held for a while, so a verdict is only
 * returned once the readings actually span [SUSTAIN_MS] - a single loud window
 * inside a five-second history is not a loud environment, it is a bang, and
 * saying otherwise would train the wearer to ignore the notice.
 *
 * The median rather than the mean for the same reason: one 110 dB spike drags
 * a mean over the line on its own, while a median needs half the window to
 * actually be loud.
 *
 * [WARN_DB] is 85 because that is the level at which hearing-conservation
 * guidance the world over starts talking about exposure time. It is compared
 * against the **approximate** SPL, so it inherits that figure's ±10 dB - which
 * is why the outcome is a notice to the wearer and never a compliance claim.
 */
object LoudEnvironment {

    const val WARN_DB: Int = 85
    const val SUSTAIN_MS: Long = 60_000L

    /** One 250 ms window, so that a history one window short of the sustain still counts. */
    const val WINDOW_MS: Long = 250L

    fun assess(readings: List<SoundReading>, nowMs: Long): LoudVerdict {
        if (readings.isEmpty()) return LoudVerdict.NoData

        val window = readings
            .filter { it.at > nowMs - SUSTAIN_MS && it.at <= nowMs }
            .sortedBy { it.at }
        if (window.isEmpty()) return LoudVerdict.NoData

        // The history has to actually cover the sustain period. Anything less
        // and the honest answer is that we have not been listening long enough
        // to tell a loud room from a slammed door.
        val oldest = window.first().at
        if (oldest > nowMs - SUSTAIN_MS + WINDOW_MS) return LoudVerdict.NoData

        val levels = window.map { it.approxDbSpl }.sorted()
        val median = if (levels.size % 2 == 1) {
            levels[levels.size / 2]
        } else {
            (levels[levels.size / 2 - 1] + levels[levels.size / 2]) / 2
        }

        return if (median >= WARN_DB) {
            LoudVerdict.Loud(sinceMs = oldest, peakDb = levels.last())
        } else {
            LoudVerdict.Quiet
        }
    }
}
