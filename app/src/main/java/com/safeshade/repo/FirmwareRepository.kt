package com.safeshade.repo

import com.safeshade.data.DeviceModel
import com.safeshade.data.FirmwareRelease
import com.safeshade.data.FirmwareStore
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import com.safeshade.device.OtaProtocol
import com.safeshade.device.OtaProtocol.OtaStep
import com.safeshade.device.OtaProtocol.VersionReply
import com.safeshade.platform.DownloadResult
import com.safeshade.platform.FirmwareDownloader
import com.safeshade.platform.firmwareFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Where the release list comes from.
 *
 * Declared here rather than taken from `cloud/` so this repository — and every
 * rule in it worth testing — can be constructed in a plain JVM unit test with
 * no Supabase client, no network and no Android. The cloud layer supplies an
 * adapter; nothing in this file knows what a bucket is.
 *
 * Both functions may throw: a failure to reach the server is reported as
 * [CloudCheck.Failed] with the thrown message, never swallowed into an empty
 * list, because an empty list and "we could not ask" look identical on screen
 * and mean opposite things.
 */
interface FirmwareSource {
    /** Published releases for [model] (`DeviceModel.wireName`), newest first or not — order is not relied on. */
    suspend fun releases(model: String): List<FirmwareRelease>

    /** A URL the phone can GET for [storagePath], or null when none can be minted. */
    suspend fun downloadUrl(storagePath: String): String?
}

/** The outcome of asking the cloud what it has. */
sealed interface CloudCheck {
    data class Found(val releases: List<FirmwareRelease>) : CloudCheck

    /** [reason] is shown to a person. The previously cached list is untouched. */
    data class Failed(val reason: String) : CloudCheck
}

/**
 * Updating the wearable's firmware, and refusing to claim it worked.
 *
 * ### The firmware fact this whole class is shaped around
 *
 * The shipped firmware acknowledges **every** `EXT` tag it receives, including
 * tags it has never heard of, with a bare `ACK:<tag>` — see
 * `DeviceCapabilities.awaitingFirmware`. Two consequences follow, and both are
 * load-bearing:
 *
 *  1. Every OTA chunk this class sends **will** be acknowledged by today's
 *     device, whether or not a single byte was stored. [install] therefore
 *     treats a chunk ack as proof of nothing but delivery, and a progress bar
 *     driven by it would run smoothly to 100% against firmware with no OTA
 *     support at all.
 *  2. The post-install `EXT VER` query **will** come back as a bare `ACK:VER`
 *     with no version attached, which `OtaProtocol.versionReply` maps to
 *     [VersionReply.AcknowledgedNoVersion].
 *
 * Put together: on the firmware that exists today, [install] can only ever end
 * at `Failed("The device acknowledged the update but reported no version")`.
 * That is not a bug in this code and it is not a placeholder — it is the honest
 * outcome, and it is the outcome the user is shown. There is no OTA partition
 * on the device yet either. The alternative, declaring success on the strength
 * of acks, would tell somebody their fall-detection wearable had been updated
 * when nothing whatsoever had changed on it.
 *
 * ### Everything else
 *
 * Nothing is sent until the downloaded image matches the release's SHA-256
 * ([downloadAndVerify]), and an image that does not match is deleted rather
 * than kept. Chunks go out **one at a time**, each awaiting its own ack, which
 * is both the GATT queue's requirement and the only way a stall can be
 * attributed to a chunk number.
 */
class FirmwareRepository(
    private val source: FirmwareSource,
    private val store: FirmwareStore,
    private val downloader: FirmwareDownloader,
    private val link: DeviceLink,
    private val scope: CoroutineScope,
    private val filesDir: File
) {

    private val _state = MutableStateFlow<OtaStep>(OtaStep.Idle)

    /** Where an update has got to. Never advances on an assumption. */
    val state: StateFlow<OtaStep> = _state.asStateFlow()

    private val _releases = MutableStateFlow<List<FirmwareRelease>?>(null)

    /**
     * The releases for the model last asked about.
     *
     * Null means *not known yet* — never asked, or asked and the answer has not
     * arrived. An empty list means the cloud answered and has nothing for this
     * model. A screen that collapsed the two would show "up to date" to a phone
     * that has never reached the server.
     */
    val releases: StateFlow<List<FirmwareRelease>?> = _releases.asStateFlow()

    private val _installedVersion = MutableStateFlow<String?>(null)

    /**
     * The version the *currently connected* device reported over the link, or
     * null if it has never reported one.
     *
     * Only a well-formed `ACK:VER:<semver>` ever sets this; see
     * `DeviceProtocol.parseVersionAck`.
     */
    val installedVersion: StateFlow<String?> = _installedVersion.asStateFlow()

    /** The model whose releases [releases] currently reflects. */
    private val requestedModel = MutableStateFlow<String?>(null)

    /** One update at a time. Two interleaved chunk streams would corrupt both. */
    private val otaLock = Mutex()

    init {
        scope.launch {
            combine(store.cache, requestedModel) { cache, model ->
                model?.let { cache.releasesByModel[it] }
            }.collect { _releases.value = it }
        }
        scope.launch {
            combine(store.cache, link.deviceAddress) { cache, address ->
                cache.installedVersionByAddress[address]
            }.collect { _installedVersion.value = it }
        }
    }

    // ============================================
    // The cloud half
    // ============================================

    /**
     * Refreshes the release list for [model].
     *
     * On failure the cached list is left exactly as it was: a phone in a lift
     * should keep showing the update it already knows about rather than
     * forgetting it. `lastCheckedAt` is recorded either way, because "we tried
     * at 14:02 and could not reach the server" is a fact worth keeping.
     */
    suspend fun check(model: DeviceModel): CloudCheck {
        val wire = model.wireName
        requestedModel.value = wire
        val now = System.currentTimeMillis()

        val fetched = try {
            source.releases(wire)
        } catch (e: Exception) {
            val current = store.cache.first()
            store.setCache(current.copy(lastCheckedAt = now))
            return CloudCheck.Failed(
                e.message?.takeIf { it.isNotBlank() }
                    ?: "The update list could not be fetched."
            )
        }

        val current = store.cache.first()
        store.setCache(
            current.copy(
                releasesByModel = current.releasesByModel + (wire to fetched),
                lastCheckedAt = now
            )
        )
        _releases.value = fetched
        return CloudCheck.Found(fetched)
    }

    // ============================================
    // Asking the device what it is running
    // ============================================

    /**
     * Writes `EXT VER` and reads the answer.
     *
     * `DeviceLink.awaitAck("VER")` cannot be used here: it returns a Boolean,
     * and the version is in the text *after* the colon. So the ack flow is
     * collected directly, and the write is issued from `onSubscription` so a
     * fast device cannot answer into a flow nobody is listening to yet — the
     * ack flow has no replay buffer, and an ack emitted before the collector
     * attaches is gone.
     *
     * A bare `VER` with no version is [VersionReply.AcknowledgedNoVersion], not
     * success and not silence. That is what today's firmware returns.
     */
    suspend fun queryVersion(): VersionReply {
        val tag = withTimeoutOrNull(VERSION_TIMEOUT_MS) {
            link.acks
                .onSubscription { link.writeExt(TAG_VER, DeviceProtocol.versionQuery()) }
                .first { it == TAG_VER || it.startsWith("$TAG_VER:") }
        }

        val reply = OtaProtocol.versionReply(tag)
        if (reply is VersionReply.Version) {
            recordInstalledVersion(reply.semver)
        }
        return reply
    }

    private suspend fun recordInstalledVersion(version: String) {
        val address = link.deviceAddress.value
        if (address.isBlank()) return
        val current = store.cache.first()
        store.setCache(
            current.copy(
                installedVersionByAddress = current.installedVersionByAddress + (address to version)
            )
        )
        _installedVersion.value = version
    }

    // ============================================
    // Download and verify
    // ============================================

    /**
     * Fetches [release] and checks it against the release's SHA-256.
     *
     * Returns [OtaStep.Verifying] when the file on disk is the published image
     * — the last step reached before anything may be sent. Any other return is
     * a failure, and in every one of them the file is deleted: a firmware image
     * that failed its digest is exactly the file that must not be lying around
     * for [install] to pick up later.
     */
    suspend fun downloadAndVerify(release: FirmwareRelease): OtaStep {
        _state.value = OtaStep.Downloading(0f)

        val url = try {
            source.downloadUrl(release.storagePath)
        } catch (e: Exception) {
            return fail(
                "The download link could not be prepared: " +
                    (e.message?.takeIf { it.isNotBlank() } ?: "the server did not answer") + "."
            )
        } ?: return fail("The server has no download link for this update.")

        val target = firmwareFile(filesDir, release.id)

        val result = downloader.download(url, target, release.byteSize) { sent, total ->
            _state.value = OtaStep.Downloading(progressOf(sent, total))
        }

        val file = when (result) {
            is DownloadResult.Failed -> return fail(result.reason)
            is DownloadResult.Done -> result.file
        }

        _state.value = OtaStep.Verifying

        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            file.delete()
            return failVerify("The downloaded update could not be read back from storage.")
        }

        if (!OtaProtocol.verifySha256(bytes, release.sha256)) {
            file.delete()
            return failVerify(
                "The downloaded file is not the published update — its checksum does not match. " +
                    "It has been deleted and nothing was sent to the device."
            )
        }

        return OtaStep.Verifying
    }

    // ============================================
    // Sending it
    // ============================================

    /**
     * Sends the verified image for [release] to the wearable, one chunk at a
     * time, and then asks the device what it is running.
     *
     * On today's firmware this ends at
     * `Failed("The device acknowledged the update but reported no version")` —
     * see the class KDoc for why that is the honest outcome rather than a gap
     * in this code. Every chunk will be acknowledged on the way there, because
     * the firmware acknowledges everything; that is precisely why the acks are
     * not allowed to decide anything.
     *
     * [mtu] is the negotiated ATT MTU. It is passed in rather than read off the
     * link because MTU belongs to a real GATT connection and has no meaning on
     * the [DeviceLink] interface — `DeviceRepository` makes the same call.
     */
    suspend fun install(release: FirmwareRelease, mtu: Int): OtaStep = otaLock.withLock {
        if (link.connectionState.value !is ConnectionState.Ready) {
            return@withLock fail("The device is not connected, so nothing was sent.")
        }

        val file = firmwareFile(filesDir, release.id)
        if (!file.exists()) {
            return@withLock fail("This update has not been downloaded yet.")
        }

        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return@withLock fail("The downloaded update could not be read back from storage.")
        }

        // Re-checked here and not only after the download: the file has been
        // sitting on disk in between, and half a firmware image is the one
        // thing that can brick the device.
        if (!OtaProtocol.verifySha256(bytes, release.sha256)) {
            file.delete()
            return@withLock failVerify(
                "The stored update no longer matches the published checksum. " +
                    "It has been deleted and nothing was sent to the device."
            )
        }

        val chunks = OtaProtocol.chunkFirmware(bytes, mtu)
        if (chunks.isEmpty()) {
            return@withLock fail(
                if (bytes.isEmpty()) {
                    "The downloaded update is empty."
                } else {
                    "The connection to the device is too narrow to carry this update " +
                        "(MTU $mtu). Reconnect and try again."
                }
            )
        }

        val total = chunks.size
        _state.value = OtaStep.Sending(0, total)

        for (chunk in chunks) {
            if (link.connectionState.value !is ConnectionState.Ready) {
                return@withLock fail(
                    "The device disconnected at chunk ${chunk.seq + 1} of $total."
                )
            }

            val payload = OtaProtocol.encodeOtaChunk(chunk)
            val ack = withTimeoutOrNull(CHUNK_TIMEOUT_MS) {
                link.acks
                    .onSubscription { link.writeExt(TAG_OTA, payload) }
                    .first { it == TAG_OTA || it.startsWith("$TAG_OTA:") }
            }

            if (ack == null) {
                return@withLock fail(
                    "The device stopped answering at chunk ${chunk.seq + 1} of $total"
                )
            }

            _state.value = OtaStep.Sending(chunk.seq + 1, total)
        }

        _state.value = OtaStep.AwaitingDeviceVersion

        val step = OtaProtocol.afterVersionReply(release.version, queryVersion())
        _state.value = step
        return@withLock step
    }

    // ============================================
    // Which release, if any
    // ============================================

    /**
     * The newest cached release for [model] that is newer than
     * [installedVersion], or null when there is nothing to offer.
     */
    fun newestFor(model: DeviceModel, installedVersion: String?): FirmwareRelease? =
        newestFor(_releases.value.orEmpty(), model.wireName, installedVersion)

    private fun fail(reason: String): OtaStep =
        OtaStep.Failed(reason).also { _state.value = it }

    private fun failVerify(reason: String): OtaStep =
        OtaStep.VerifyFailed(reason).also { _state.value = it }

    companion object {
        /** The ack tag for `EXT VER`. */
        const val TAG_VER = "VER"

        /** The ack tag for an OTA chunk. */
        const val TAG_OTA = "OTA"

        /** Same 4 s the rest of the app allows an ack; the device is idle here. */
        const val VERSION_TIMEOUT_MS = 4_000L

        /**
         * Longer than an ordinary ack: the device may be erasing a flash sector
         * between chunks, which is slow and is not a stall.
         */
        const val CHUNK_TIMEOUT_MS = 10_000L

        private fun progressOf(sent: Long, total: Long): Float =
            if (total <= 0L) 0f else (sent.toFloat() / total.toFloat()).coerceIn(0f, 1f)

        /**
         * Pure: the newest release in [releases] for [model] that beats
         * [installedVersion].
         *
         * `versionCode` decides first, because the server maintains it as a
         * monotonic ordering and a number cannot be misparsed. [compareVersions]
         * only breaks a tie, which happens when two rows were published with the
         * same code.
         *
         * A null [installedVersion] means the device has never told us what it
         * runs, so the newest release is offered — offering nothing would hide
         * the update behind a question only the device can answer, and today's
         * firmware never answers it.
         */
        fun newestFor(
            releases: List<FirmwareRelease>,
            model: String,
            installedVersion: String?
        ): FirmwareRelease? {
            val candidates = releases.filter { it.model.equals(model, ignoreCase = true) }
            val newest = candidates.maxWithOrNull(
                compareBy<FirmwareRelease> { it.versionCode }
                    .thenComparator { a, b -> compareVersions(a.version, b.version) }
            ) ?: return null

            if (installedVersion.isNullOrBlank()) return newest
            return if (compareVersions(newest.version, installedVersion) > 0) newest else null
        }

        /**
         * Pure, semver-ish: -1, 0 or 1 for [a] against [b].
         *
         * Dotted numeric segments compare numerically and a missing segment
         * reads as zero, so `1.2` and `1.2.0` are equal and `1.10.0` beats
         * `1.9.9` — which plain string comparison gets backwards, and getting it
         * backwards would offer a downgrade as an update.
         *
         * A suffix after `-` is a pre-release and ranks *below* the same version
         * without one, matching semver: `1.2.0-beta` is older than `1.2.0`. Two
         * pre-releases of the same version are ordered by their text, which is
         * the best that can be done without inventing meaning.
         *
         * A leading `v` is tolerated. Anything non-numeric inside a segment
         * reads as zero rather than throwing; a version string this code cannot
         * parse must not crash an update check.
         */
        fun compareVersions(a: String, b: String): Int {
            val (coreA, preA) = splitVersion(a)
            val (coreB, preB) = splitVersion(b)

            val size = maxOf(coreA.size, coreB.size)
            for (i in 0 until size) {
                val left = coreA.getOrElse(i) { 0 }
                val right = coreB.getOrElse(i) { 0 }
                if (left != right) return if (left < right) -1 else 1
            }

            return when {
                preA == null && preB == null -> 0
                preA == null -> 1
                preB == null -> -1
                else -> preA.compareTo(preB).coerceIn(-1, 1)
            }
        }

        /** `1.2.3-beta.1` to `[1, 2, 3]` and `"beta.1"`. */
        private fun splitVersion(version: String): Pair<List<Int>, String?> {
            val trimmed = version.trim().removePrefix("v").removePrefix("V")
            val dash = trimmed.indexOf('-')
            val core = if (dash >= 0) trimmed.substring(0, dash) else trimmed
            val pre = if (dash >= 0) trimmed.substring(dash + 1).takeIf { it.isNotEmpty() } else null
            val numbers = core.split('.').map { segment ->
                segment.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
            }
            return numbers to pre
        }
    }
}
