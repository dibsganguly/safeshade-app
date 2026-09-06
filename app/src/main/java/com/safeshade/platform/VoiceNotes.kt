package com.safeshade.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.max

/**
 * Push-to-talk voice notes for the Circle Talk thread.
 *
 * Two small pieces of state make this trickier than "wrap MediaRecorder":
 *
 *  - **The waveform has to exist by the time recording stops**, because the
 *    message list shows it immediately, before any upload — sampling has to
 *    run *during* recording, not be derived from the file afterward (this
 *    app never keeps the raw PCM, only the encoded `.m4a`).
 *  - **Hitting the 20 s cap is not an error.** `MediaRecorder` delivers it as
 *    an `OnInfoListener` callback, not an exception or a stopped state the
 *    caller can poll — `stop()` has to treat "the cap fired" the same as "the
 *    user let go of the button".
 *
 * Neither class requests `RECORD_AUDIO` itself. That belongs at the call site,
 * where there is a screen to explain why the mic is needed; this file only
 * checks the grant and reports plainly when it is missing.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var samplingJob: Job? = null
    private val amplitudeSamples = mutableListOf<Int>()
    private var startedAt = 0L
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Starts recording to a new file under `filesDir/voice/`.
     *
     * Returns null — recording never starts — when `RECORD_AUDIO` is not
     * granted, or when `MediaRecorder` itself rejects the configuration (a
     * busy mic, an OEM quirk); either way the caller sees "no file", not an
     * exception.
     */
    fun start(): File? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.m4a")
        val mr = newMediaRecorder()

        return try {
            mr.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE_HZ)
                setAudioEncodingBitRate(BIT_RATE_BPS)
                setOutputFile(file.absolutePath)
                setMaxDuration(MAX_DURATION_MS)
                setOnInfoListener { _, what, _ ->
                    // The cap firing is a normal stop, not a caller-visible
                    // error — see the class doc. Nothing to do here beyond
                    // letting MediaRecorder's own internal stop proceed;
                    // `stop()` below is what the UI actually calls next.
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        // No-op: MediaRecorder stops itself internally on the
                        // cap. The caller's own timer (driving the button UI)
                        // is expected to call stop() at the same 20 s mark.
                    }
                }
                prepare()
                start()
            }
            recorder = mr
            currentFile = file
            amplitudeSamples.clear()
            startedAt = System.currentTimeMillis()
            startSampling()
            file
        } catch (e: Exception) {
            mr.release()
            null
        }
    }

    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    private fun startSampling() {
        samplingJob = scope.launch {
            while (isActive) {
                delay(SAMPLE_INTERVAL_MS)
                val amp = try {
                    recorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }
                synchronized(amplitudeSamples) { amplitudeSamples.add(amp) }
            }
        }
    }

    /** Stops recording (if any) and reduces the sampled amplitudes to a waveform. */
    suspend fun stop(): VoiceNoteResult {
        val mr = recorder ?: return VoiceNoteResult.Failed("Microphone permission not granted")
        val file = currentFile
        samplingJob?.cancel()
        samplingJob = null

        return try {
            mr.stop()
            mr.release()
            recorder = null
            val durationMs = (System.currentTimeMillis() - startedAt).toInt()
            val samplesCopy = synchronized(amplitudeSamples) { amplitudeSamples.toList() }
            val waveform = bucketWaveform(samplesCopy)
            if (file != null) {
                VoiceNoteResult.Ok(file, durationMs, waveform)
            } else {
                VoiceNoteResult.Failed("Recording file was lost")
            }
        } catch (e: Exception) {
            recorder = null
            VoiceNoteResult.Failed(e.message ?: "Could not finish the recording")
        }
    }

    companion object {
        private const val SAMPLE_RATE_HZ = 16_000
        private const val BIT_RATE_BPS = 24_000
        private const val MAX_DURATION_MS = 20_000
        private const val SAMPLE_INTERVAL_MS = 50L
    }
}

/** Result of [VoiceRecorder.stop]. Nothing throws to the caller. */
sealed interface VoiceNoteResult {
    data class Ok(val file: File, val durationMs: Int, val waveform: List<Float>) : VoiceNoteResult
    data class Failed(val reason: String) : VoiceNoteResult
}

/** Inline playback for a recorded voice note, with progress and completion callbacks. */
class VoicePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /** Plays [file] from the start, stopping any note already playing on this instance. */
    fun play(file: File, onProgress: (Float) -> Unit, onDone: () -> Unit) {
        stop()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                progressJob?.cancel()
                progressJob = null
                _isPlaying.value = false
                onDone()
                releasePlayer()
            }
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            _isPlaying.value = true

            val duration = mp.duration.coerceAtLeast(1)
            progressJob = scope.launch {
                while (isActive) {
                    val pos = try {
                        mp.currentPosition
                    } catch (e: Exception) {
                        0
                    }
                    onProgress((pos.toFloat() / duration).coerceIn(0f, 1f))
                    delay(PROGRESS_INTERVAL_MS)
                }
            }
        } catch (e: Exception) {
            _isPlaying.value = false
            mp.release()
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        releasePlayer()
        _isPlaying.value = false
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (e: Exception) {
                // Already stopped/released — nothing to clean up.
            }
            it.release()
        }
        mediaPlayer = null
    }

    companion object {
        private const val PROGRESS_INTERVAL_MS = 50L
    }
}

/**
 * Reduces raw `maxAmplitude` samples (0..32767, roughly every [samples]
 * entry = one [VoiceRecorder] tick) to [buckets] normalised 0..1 values for a
 * fixed-width waveform view.
 *
 * A pure function so it can be unit-tested without `MediaRecorder` — the
 * interesting cases are short recordings (fewer samples than buckets, so the
 * tail is padded with silence) and a silent recording (max is zero, so
 * dividing by it would be a NaN wall rather than a flat line).
 */
internal fun bucketWaveform(samples: List<Int>, buckets: Int = 40): List<Float> {
    if (samples.isEmpty()) return List(buckets) { 0f }

    val chunkSize = max(1, samples.size / buckets)
    val chunked = samples.chunked(chunkSize).map { chunk -> chunk.average().toFloat() }
    val trimmed = if (chunked.size > buckets) chunked.take(buckets) else chunked
    val padded = if (trimmed.size < buckets) trimmed + List(buckets - trimmed.size) { 0f } else trimmed

    val maxVal = padded.maxOrNull()?.takeIf { it > 0f } ?: return List(buckets) { 0f }
    return padded.map { (it / maxVal).coerceIn(0f, 1f) }
}
