package com.safeshade.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * Fetching a firmware image onto the phone, and saying plainly when it failed.
 *
 * The download is deliberately dumb: one GET, streamed to a file, no resume and
 * no retry. A firmware image is a few hundred kilobytes over a link the user is
 * standing next to; a half-finished file is deleted and the whole thing is
 * fetched again rather than stitched, because the only thing that could stitch
 * it correctly is a range request the storage backend need not support.
 *
 * Nothing here decides whether the bytes are the *right* bytes. The size check
 * below catches a truncated body — a proxy error page saved under a `.bin` name
 * — but the digest is checked by `FirmwareRepository` against the release's
 * SHA-256 before a single chunk is sent, and that is the check that matters.
 */

/** Where a download ended up. There is no third state and no silent partial. */
sealed interface DownloadResult {
    data class Done(val file: File) : DownloadResult

    /** [reason] is shown to a person, so it is written for one. */
    data class Failed(val reason: String) : DownloadResult
}

/**
 * The download step, as an interface so the repository can be unit-tested with
 * no network, no OkHttp and no Android.
 *
 * [onProgress] is called with bytes-so-far and the total the caller expects,
 * which is the release's recorded size rather than the server's
 * `Content-Length`: the release row is the number the phone has a reason to
 * believe, and a missing or lying header must not make the bar jump.
 */
interface FirmwareDownloader {
    suspend fun download(
        url: String,
        target: File,
        expectedBytes: Long,
        onProgress: (Long, Long) -> Unit
    ): DownloadResult
}

/** `filesDir/firmware/<releaseId>.bin` — the one place this path is built. */
fun firmwareFile(filesDir: File, releaseId: String): File =
    File(File(filesDir, FIRMWARE_DIR), "$releaseId.bin")

/** The subdirectory of `filesDir` that holds downloaded images. */
const val FIRMWARE_DIR: String = "firmware"

/** How long a stalled connection or a stalled body is tolerated. */
private const val TIMEOUT_SECONDS = 30L

/** Progress is reported at most this often, to keep off the UI thread's back. */
private const val PROGRESS_STEP_BYTES = 16 * 1024

class OkHttpFirmwareDownloader(
    client: OkHttpClient = OkHttpClient()
) : FirmwareDownloader {

    /**
     * Built from the caller's client so connection pooling is shared, but with
     * this download's own timeouts: the app's default read timeout is tuned for
     * small JSON calls and would abandon a firmware body mid-stream.
     */
    private val http: OkHttpClient = client.newBuilder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun download(
        url: String,
        target: File,
        expectedBytes: Long,
        onProgress: (Long, Long) -> Unit
    ): DownloadResult = withContext(Dispatchers.IO) {
        target.parentFile?.mkdirs()
        // A leftover from an abandoned attempt would otherwise be appended to
        // or, worse, left in place and mistaken for this download's result.
        if (target.exists()) target.delete()

        val request = Request.Builder().url(url).get().build()

        try {
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DownloadResult.Failed(httpReason(response.code))
                }
                val body = response.body
                    ?: return@withContext DownloadResult.Failed(
                        "The server answered with no file attached."
                    )

                var written = 0L
                var lastReported = 0L
                onProgress(0L, expectedBytes)

                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            written += read
                            if (written - lastReported >= PROGRESS_STEP_BYTES) {
                                lastReported = written
                                onProgress(written, expectedBytes)
                            }
                        }
                    }
                }
                onProgress(written, expectedBytes)

                if (expectedBytes > 0 && written != expectedBytes) {
                    target.delete()
                    return@withContext DownloadResult.Failed(
                        "The download stopped early — $written bytes arrived of the " +
                            "$expectedBytes this update should be."
                    )
                }

                DownloadResult.Done(target)
            }
        } catch (e: IOException) {
            target.delete()
            DownloadResult.Failed(
                "The download could not finish: ${e.message ?: "the connection was lost"}."
            )
        } catch (e: IllegalArgumentException) {
            // OkHttp rejects a malformed URL here rather than at call time.
            target.delete()
            DownloadResult.Failed("The download link is not a usable address.")
        }
    }
}

/**
 * Plain English for the statuses this actually meets, and an honest fallback
 * for the rest.
 *
 * A signed storage URL is the normal case, so 403 means an expired link far
 * more often than it means a permissions problem, and saying so points the user
 * at the fix — check again.
 */
fun httpReason(code: Int): String = when (code) {
    401, 403 -> "The download link has expired. Check for updates again to get a fresh one."
    404 -> "This update is no longer available from the server."
    408, 504 -> "The server took too long to send the update."
    429 -> "The server is asking us to slow down. Try again in a minute."
    in 500..599 -> "The server could not send the update right now (error $code)."
    else -> "The server refused the download (HTTP $code)."
}
