package com.safeshade.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A fixed-length recording of the wearer's surroundings, made as evidence.
 *
 * ### The one trap this class exists to contain
 *
 * `MediaRecorder.setMaxDuration` does not merely notify at the cap - the
 * recorder **stops itself**, then delivers
 * `MEDIA_RECORDER_INFO_MAX_DURATION_REACHED` through `OnInfoListener`. A
 * `stop()` issued afterwards throws `IllegalStateException`, and since this
 * recorder is fixed-length that race is not an edge case: it is what happens
 * every single time a recording runs to its full length while any caller-side
 * timer is also counting down. `VoiceRecorder` in `VoiceNotes.kt` leans on the
 * caller's timer arriving at the same instant; this class does not, because
 * here there is no finger on a button - a fall alert starts it and nothing
 * watches it finish.
 *
 * So finishing happens on exactly one path, guarded by one [AtomicBoolean]:
 * whichever of the cap callback and [stop] gets there first wins, the other
 * returns the same [EvidenceRecordingResult].
 *
 * That path always calls `stop()`, inside a `runCatching`, and the reason is
 * worth stating because the obvious optimisation is wrong. `setMaxDuration`'s
 * own documentation says the stop it performs is *asynchronous* and gives no
 * guarantee that it has finished by the time the listener is notified. So
 * skipping `stop()` on the cap path to dodge the throw can hand `release()` a
 * recorder that is still writing, and that leaves an `.m4a` with no `moov`
 * atom - non-zero bytes, past the size check below, and silent in every
 * player. Catching a possible `IllegalStateException` is the cheaper mistake.
 *
 * ### Why a zero-byte file is a failure
 *
 * A `.m4a` of zero bytes is what a mic seized by another app, or an encoder
 * that never started, leaves behind. Keeping it would put a row in the
 * evidence list that plays silence and cannot be told apart from a genuinely
 * quiet room - so it is deleted and reported as
 * [EvidenceRecordingResult.Failed] with a reason a person can read.
 *
 * This class does not request `RECORD_AUDIO`. That belongs at the call site,
 * where there is a screen to explain why the microphone is needed; here the
 * grant is only checked, and its absence reported plainly.
 */
class EvidenceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var tickerJob: Job? = null
    private var startedAt = 0L
    private var totalMs = 0
    private val finished = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private val _elapsedMs = MutableStateFlow(0)

    /** How far into the recording we are, in milliseconds. Zero when idle. */
    val elapsedMs: StateFlow<Int> = _elapsedMs.asStateFlow()

    private val _result = MutableStateFlow<EvidenceRecordingResult?>(null)

    /**
     * The finished recording, however it finished.
     *
     * This is the only place a caller learns that the cap fired, because the
     * cap fires with nobody watching.
     */
    val result: StateFlow<EvidenceRecordingResult?> = _result.asStateFlow()

    /** Whether a recording is running right now. */
    val isRecording: Boolean get() = recorder != null

    /**
     * Starts recording to `filesDir/evidence/<id>.m4a`.
     *
     * @param durationSeconds clamped to 10..120. Shorter than ten seconds is
     *   not evidence of anything; longer than two minutes is a file nobody
     *   will listen to and a microphone left open past the incident.
     * @return null when recording did not start - the microphone permission is
     *   missing, or `MediaRecorder` refused the configuration. [result] is set
     *   to the matching [EvidenceRecordingResult.Failed] so a caller watching
     *   the flow sees the reason too.
     */
    fun start(durationSeconds: Int = DEFAULT_DURATION_SECONDS): File? {
        if (isRecording) {
            fail("A recording is already running")
            return null
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            fail("Microphone permission has not been granted")
            return null
        }

        val seconds = durationSeconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)
        totalMs = seconds * 1000

        val dir = File(context.filesDir, DIR_NAME).apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")
        val mr = newMediaRecorder()

        try {
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE_HZ)
                setAudioChannels(1)
                setAudioEncodingBitRate(BIT_RATE_BPS)
                setOutputFile(file.absolutePath)
                // Before prepare(), or it is not applied.
                setMaxDuration(totalMs)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED ||
                        what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED
                    ) {
                        // The recorder has already begun stopping itself.
                        // See the class doc for why finish() still calls
                        // stop() here rather than going straight to release().
                        finish()
                    }
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            runCatching { mr.release() }
            runCatching { if (file.exists()) file.delete() }
            Log.e(TAG, "Could not start the evidence recording", e)
            fail(e.message?.takeIf { it.isNotBlank() } ?: "The microphone would not start")
            return null
        }

        recorder = mr
        currentFile = file
        finished.set(false)
        _result.value = null
        startedAt = System.currentTimeMillis()
        _elapsedMs.value = 0
        startTicker()
        return file
    }

    /**
     * Stops early, keeping what has been recorded so far.
     *
     * A recording cut short by the user is a shorter recording, not a failed
     * one - the seconds already captured are exactly as much evidence as they
     * were a moment ago. Calling this after the cap has already fired returns
     * the result the cap produced rather than throwing.
     */
    fun stop(): EvidenceRecordingResult = finish()

    /**
     * Stops and deletes. For a recording the user has asked to discard, and
     * for a service being torn down before anything usable existed.
     */
    fun cancel() {
        val outcome = finish()
        if (outcome is EvidenceRecordingResult.Recorded) {
            runCatching { if (outcome.file.exists()) outcome.file.delete() }
        }
        _result.value = null
    }

    // ============================================
    // The single finishing path
    // ============================================

    private fun finish(): EvidenceRecordingResult {
        // Whichever caller loses this race gets the result the winner produced,
        // and never touches the MediaRecorder.
        if (!finished.compareAndSet(false, true)) {
            return _result.value ?: EvidenceRecordingResult.Failed("The recording had already ended")
        }

        val mr = recorder
        val file = currentFile
        tickerJob?.cancel()
        tickerJob = null
        recorder = null
        currentFile = null

        if (mr == null) {
            val outcome = EvidenceRecordingResult.Failed("No recording was running")
            _result.value = outcome
            return outcome
        }

        val durationMs = (System.currentTimeMillis() - startedAt).toInt().coerceAtLeast(0)

        // Wrapped, not guarded by a state check: MediaRecorder exposes no way
        // to ask whether it is still recording, so the only honest handling of
        // "it may have stopped itself a millisecond ago" is to catch it.
        runCatching { mr.stop() }
            .onFailure { Log.w(TAG, "stop() on an already-stopped recorder", it) }
        runCatching { mr.reset() }
        runCatching { mr.release() }

        _elapsedMs.value = 0

        val outcome = when {
            file == null -> EvidenceRecordingResult.Failed("The recording file was lost")
            !file.exists() || file.length() == 0L -> {
                runCatching { if (file.exists()) file.delete() }
                EvidenceRecordingResult.Failed("Nothing was recorded")
            }
            else -> EvidenceRecordingResult.Recorded(
                file = file,
                // The cap path may land a few milliseconds either side of the
                // requested length; report what was measured, capped at what
                // was asked for, never a round number nobody timed.
                durationMs = durationMs.coerceAtMost(totalMs),
                bytes = file.length()
            )
        }
        _result.value = outcome
        return outcome
    }

    private fun fail(reason: String) {
        Log.w(TAG, "Evidence recording refused: $reason")
        _result.value = EvidenceRecordingResult.Failed(reason)
    }

    private fun startTicker() {
        tickerJob = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                val elapsed = (System.currentTimeMillis() - startedAt).toInt()
                _elapsedMs.value = elapsed.coerceIn(0, totalMs)
                // A belt-and-braces stop. The cap callback is the mechanism;
                // this catches the OEM that never delivers it, which would
                // otherwise leave the microphone open indefinitely.
                if (elapsed >= totalMs + GRACE_MS) {
                    finish()
                    break
                }
            }
        }
    }

    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    /** Cancels the ticker scope. The recorder cannot be reused afterwards. */
    fun release() {
        if (isRecording) cancel()
        scope.cancel()
    }

    companion object {
        private const val TAG = "EvidenceRecorder"

        /** The folder under `filesDir`. `EvidenceRepository` joins the same one. */
        const val DIR_NAME = "evidence"

        const val DEFAULT_DURATION_SECONDS = 30
        const val MIN_DURATION_SECONDS = 10
        const val MAX_DURATION_SECONDS = 120

        private const val SAMPLE_RATE_HZ = 16_000
        private const val BIT_RATE_BPS = 32_000
        private const val TICK_MS = 200L

        /** How long past the cap the fallback stop waits for the callback. */
        private const val GRACE_MS = 1_500
    }
}

/** The outcome of a recording. Nothing throws to the caller. */
sealed interface EvidenceRecordingResult {

    /** Audio was captured. [bytes] is the file's real size, measured after the fact. */
    data class Recorded(val file: File, val durationMs: Int, val bytes: Long) :
        EvidenceRecordingResult

    /** [reason] is shown to the user as written, so it says what happened in plain words. */
    data class Failed(val reason: String) : EvidenceRecordingResult
}
