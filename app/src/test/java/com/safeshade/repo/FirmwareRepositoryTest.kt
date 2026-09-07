package com.safeshade.repo

import com.safeshade.data.DeviceModel
import com.safeshade.data.FirmwareCache
import com.safeshade.data.FirmwareRelease
import com.safeshade.data.InMemoryFirmwareStore
import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.device.ConnectionState
import com.safeshade.device.BleSighting
import com.safeshade.device.DeviceAlert
import com.safeshade.device.DeviceLink
import com.safeshade.device.OtaProtocol
import com.safeshade.device.OtaProtocol.OtaStep
import com.safeshade.device.OtaProtocol.VersionReply
import com.safeshade.platform.DownloadResult
import com.safeshade.platform.FirmwareDownloader
import com.safeshade.platform.firmwareFile
import com.safeshade.platform.httpReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * The rules that decide whether somebody is told their wearable was updated.
 *
 * The central one is the firmware fact: today's device acknowledges every EXT
 * tag it receives, so a run in which every chunk is acked and `VER` comes back
 * bare must end in a failure that says so. "a bare version reply is not an
 * install" is that run, and it is the outcome on the hardware that exists.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FirmwareRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    // ============================================
    // Fakes
    // ============================================

    /**
     * A [DeviceLink] that answers writes the way the firmware does.
     *
     * [ackFor] decides what — if anything — comes back for a given EXT tag, so
     * one fake covers the bare-ack device, a device that reports a version, and
     * a device that goes quiet mid-transfer.
     */
    private class FakeLink(
        val ackFor: (tag: String, payload: String, index: Int) -> String?
    ) : DeviceLink {

        override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Ready)
        override val deviceName = MutableStateFlow("SafeShade S1")
        override val deviceAddress = MutableStateFlow("AA:BB:CC:DD:EE:FF")
        override val rssi = MutableStateFlow(-60)
        override val telemetry = MutableStateFlow(LiveSensorData())

        private val _alerts = MutableSharedFlow<DeviceAlert>(extraBufferCapacity = 8)
        override val alerts: SharedFlow<DeviceAlert> = _alerts.asSharedFlow()

        private val _replies = MutableSharedFlow<String>(extraBufferCapacity = 8)
        override val replies: SharedFlow<String> = _replies.asSharedFlow()

        private val _acks = MutableSharedFlow<String>(extraBufferCapacity = 64)
        override val acks: SharedFlow<String> = _acks.asSharedFlow()

        private val _sightings = MutableSharedFlow<BleSighting>(extraBufferCapacity = 8)
        override val sightings: SharedFlow<BleSighting> = _sightings.asSharedFlow()

        /** Every EXT write, in order, as tag to payload. */
        val extWrites = mutableListOf<Pair<String, String>>()

        override fun startScan(preferredAddress: String?) = Unit
        override fun stopScan() = Unit
        override fun startSightingScan(durationMs: Long) = Unit
        override fun stopSightingScan() = Unit
        override fun disconnect() = Unit
        override fun readRssi() = Unit
        override fun writeWeather(payload: String) = Unit
        override fun writeHealth(payload: String) = Unit
        override fun writeSettings(payload: String) = Unit
        override fun writeLed(pattern: LedPattern) = Unit

        override fun writeExt(tag: String, payload: String) {
            val index = extWrites.count { it.first == tag }
            extWrites += tag to payload
            ackFor(tag, payload, index)?.let { _acks.tryEmit(it) }
        }

        override fun writeGuardianMessage(text: String) = Unit
        override fun writeCompanionReply(text: String) = Unit
        override fun ringDevice() = Unit

        override suspend fun awaitAck(tag: String, timeoutMs: Long): Boolean = false
    }

    /** Writes the bytes it was handed. No network, no OkHttp. */
    private class FakeDownloader(
        private val bytes: ByteArray?,
        private val failure: String? = null
    ) : FirmwareDownloader {
        val progress = mutableListOf<Pair<Long, Long>>()

        override suspend fun download(
            url: String,
            target: File,
            expectedBytes: Long,
            onProgress: (Long, Long) -> Unit
        ): DownloadResult {
            if (failure != null) return DownloadResult.Failed(failure)
            val payload = bytes ?: ByteArray(0)
            target.parentFile?.mkdirs()
            onProgress(0L, expectedBytes).also { progress += 0L to expectedBytes }
            target.writeBytes(payload)
            onProgress(payload.size.toLong(), expectedBytes)
            progress += payload.size.toLong() to expectedBytes
            return DownloadResult.Done(target)
        }
    }

    private class FakeSource(
        private val published: List<FirmwareRelease> = emptyList(),
        private val error: String? = null,
        private val url: String? = "https://example.invalid/fw.bin"
    ) : FirmwareSource {
        var calls = 0
        override suspend fun releases(model: String): List<FirmwareRelease> {
            calls++
            error?.let { throw IllegalStateException(it) }
            return published.filter { it.model == model }
        }

        override suspend fun downloadUrl(storagePath: String): String? = url
    }

    // ============================================
    // Fixtures
    // ============================================

    private val image = ByteArray(900) { (it % 251).toByte() }

    private fun release(
        id: String = "rel-1",
        model: String = "s1",
        version: String = "1.2.3",
        versionCode: Int = 3,
        sha: String = OtaProtocol.sha256Hex(image),
        size: Long = image.size.toLong()
    ) = FirmwareRelease(
        id = id,
        model = model,
        version = version,
        versionCode = versionCode,
        storagePath = "firmware/$id.bin",
        sha256 = sha,
        byteSize = size,
        releaseNotes = "Fixes the thing",
        mandatory = false,
        publishedAt = 1_700_000_000_000L
    )

    private fun repo(
        scope: CoroutineScope,
        link: DeviceLink,
        source: FirmwareSource = FakeSource(),
        downloader: FirmwareDownloader = FakeDownloader(image),
        store: InMemoryFirmwareStore = InMemoryFirmwareStore()
    ) = FirmwareRepository(
        source = source,
        store = store,
        downloader = downloader,
        link = link,
        scope = scope,
        filesDir = folder.root
    )

    /** Acks every EXT tag bare, exactly as the shipped firmware does. */
    private val bareAckDevice: (String, String, Int) -> String? = { tag, _, _ -> tag }

    // ============================================
    // The check
    // ============================================

    @Test
    fun `a successful check caches the releases and records when it happened`() = runTest {
        val store = InMemoryFirmwareStore()
        val source = FakeSource(listOf(release()))
        val repo = repo(backgroundScope, FakeLink(bareAckDevice), source = source, store = store)

        val result = repo.check(DeviceModel.S1)

        assertTrue(result is CloudCheck.Found)
        assertEquals(listOf("rel-1"), (result as CloudCheck.Found).releases.map { it.id })
        assertEquals(listOf("rel-1"), store.current.releasesByModel["s1"]?.map { it.id })
        assertTrue("the check must be timestamped", (store.current.lastCheckedAt ?: 0L) > 0L)
    }

    @Test
    fun `a failed check keeps the cached list and says why`() = runTest {
        val store = InMemoryFirmwareStore(
            FirmwareCache(releasesByModel = mapOf("s1" to listOf(release())))
        )
        val source = FakeSource(error = "No route to host")
        val repo = repo(backgroundScope, FakeLink(bareAckDevice), source = source, store = store)

        val result = repo.check(DeviceModel.S1)

        assertEquals(CloudCheck.Failed("No route to host"), result)
        assertEquals(
            "the cached release must survive a failed check",
            listOf("rel-1"),
            store.current.releasesByModel["s1"]?.map { it.id }
        )
    }

    // ============================================
    // The version query
    // ============================================

    @Test
    fun `a bare ack is not a version`() = runTest {
        val repo = repo(backgroundScope, FakeLink(bareAckDevice))

        assertEquals(VersionReply.AcknowledgedNoVersion, repo.queryVersion())
    }

    @Test
    fun `a real version reply is stored against the connected address`() = runTest {
        val store = InMemoryFirmwareStore()
        val link = FakeLink { tag, _, _ -> if (tag == "VER") "VER:1.2.3" else tag }
        val repo = repo(backgroundScope, link, store = store)

        assertEquals(VersionReply.Version("1.2.3"), repo.queryVersion())
        assertEquals("1.2.3", store.current.installedVersionByAddress["AA:BB:CC:DD:EE:FF"])
        assertEquals("1.2.3", repo.installedVersion.value)
    }

    @Test
    fun `a silent device times out rather than inventing a version`() = runTest {
        val repo = repo(backgroundScope, FakeLink { _, _, _ -> null })

        assertEquals(VersionReply.Timeout, repo.queryVersion())
    }

    // ============================================
    // Download and verify
    // ============================================

    @Test
    fun `a verified download is kept`() = runTest {
        val rel = release()
        val repo = repo(backgroundScope, FakeLink(bareAckDevice))

        val step = repo.downloadAndVerify(rel)

        assertEquals(OtaStep.Verifying, step)
        assertTrue(firmwareFile(folder.root, rel.id).exists())
    }

    @Test
    fun `a file that fails its checksum is deleted and nothing is sent`() = runTest {
        val rel = release(sha = "00".repeat(32))
        val link = FakeLink(bareAckDevice)
        val repo = repo(backgroundScope, link)

        val step = repo.downloadAndVerify(rel)

        assertTrue("must be a verify failure, not a generic one", step is OtaStep.VerifyFailed)
        assertTrue((step as OtaStep.VerifyFailed).reason.contains("checksum"))
        assertFalse(firmwareFile(folder.root, rel.id).exists())
        assertTrue("nothing may go to the device", link.extWrites.isEmpty())
    }

    @Test
    fun `a download failure is reported in the downloader's own words`() = runTest {
        val downloader = FakeDownloader(null, failure = "This update is no longer available from the server.")
        val repo = repo(backgroundScope, FakeLink(bareAckDevice), downloader = downloader)

        val step = repo.downloadAndVerify(release())

        assertEquals(
            OtaStep.Failed("This update is no longer available from the server."),
            step
        )
    }

    // ============================================
    // The install, against the firmware that exists
    // ============================================

    @Test
    fun `a bare version reply is not an install`() = runTest {
        val rel = release()
        val link = FakeLink(bareAckDevice)
        val repo = repo(backgroundScope, link)
        repo.downloadAndVerify(rel)

        val step = repo.install(rel, mtu = 247)

        assertEquals(
            OtaStep.Failed("The device acknowledged the update but reported no version"),
            step
        )
        assertEquals(step, repo.state.value)
        val chunks = link.extWrites.count { it.first == "OTA" }
        assertTrue("every chunk is written and acked on this firmware", chunks > 0)
        assertEquals(
            "the whole image is sent",
            image.size,
            link.extWrites.filter { it.first == "OTA" }
                .mapNotNull { OtaProtocol.decodeOtaChunk(it.second) }
                .sumOf { it.bytes.size }
        )
    }

    @Test
    fun `a device reporting the sent version is an install`() = runTest {
        val rel = release()
        val link = FakeLink { tag, _, _ -> if (tag == "VER") "VER:1.2.3" else tag }
        val repo = repo(backgroundScope, link)
        repo.downloadAndVerify(rel)

        val step = repo.install(rel, mtu = 247)

        assertEquals(OtaStep.Installed("1.2.3"), step)
    }

    @Test
    fun `a device reporting a different version is not an install`() = runTest {
        val rel = release()
        val link = FakeLink { tag, _, _ -> if (tag == "VER") "VER:1.0.0" else tag }
        val repo = repo(backgroundScope, link)
        repo.downloadAndVerify(rel)

        val step = repo.install(rel, mtu = 247)

        assertEquals(OtaStep.Failed("The device is running 1.0.0, not 1.2.3"), step)
    }

    @Test
    fun `a device that stops answering names the chunk it stopped at`() = runTest {
        val rel = release()
        // Acks the first two OTA chunks and then goes silent.
        val link = FakeLink { tag, _, index ->
            when {
                tag != "OTA" -> tag
                index < 2 -> tag
                else -> null
            }
        }
        val repo = repo(backgroundScope, link)
        repo.downloadAndVerify(rel)

        val step = repo.install(rel, mtu = 247)

        assertTrue(step is OtaStep.Failed)
        val reason = (step as OtaStep.Failed).reason
        assertTrue("must name the chunk: $reason", reason.startsWith("The device stopped answering at chunk 3 of "))
    }

    @Test
    fun `an install without a downloaded file sends nothing`() = runTest {
        val link = FakeLink(bareAckDevice)
        val repo = repo(backgroundScope, link)

        val step = repo.install(release(), mtu = 247)

        assertEquals(OtaStep.Failed("This update has not been downloaded yet."), step)
        assertTrue(link.extWrites.isEmpty())
    }

    @Test
    fun `an install refuses to start unless the link is ready`() = runTest {
        val rel = release()
        val link = FakeLink(bareAckDevice)
        val repo = repo(backgroundScope, link)
        repo.downloadAndVerify(rel)
        link.connectionState.value = ConnectionState.Connected

        val step = repo.install(rel, mtu = 247)

        assertEquals(OtaStep.Failed("The device is not connected, so nothing was sent."), step)
        assertTrue(link.extWrites.isEmpty())
    }

    // ============================================
    // Which release, and which is newer
    // ============================================

    @Test
    fun `newestFor offers only a release for the right model`() {
        val releases = listOf(
            release(id = "s1-new", model = "s1", version = "2.0.0", versionCode = 5),
            release(id = "spark", model = "spark", version = "9.0.0", versionCode = 90)
        )

        val newest = FirmwareRepository.newestFor(releases, "s1", installedVersion = "1.0.0")

        assertEquals("s1-new", newest?.id)
    }

    @Test
    fun `newestFor offers nothing when the installed version is the newest`() {
        val releases = listOf(release(version = "1.2.3", versionCode = 3))

        assertNull(FirmwareRepository.newestFor(releases, "s1", installedVersion = "1.2.3"))
        assertNull(FirmwareRepository.newestFor(releases, "s1", installedVersion = "1.3.0"))
    }

    @Test
    fun `newestFor offers the newest when the device has never reported a version`() {
        val releases = listOf(
            release(id = "old", version = "1.0.0", versionCode = 1),
            release(id = "new", version = "1.10.0", versionCode = 2)
        )

        assertEquals("new", FirmwareRepository.newestFor(releases, "s1", null)?.id)
    }

    @Test
    fun `newestFor returns null for a model with no releases`() {
        assertNull(FirmwareRepository.newestFor(emptyList(), "s1", null))
        assertNull(FirmwareRepository.newestFor(listOf(release()), "5g", null))
    }

    @Test
    fun `compareVersions orders numerically, not as text`() {
        assertTrue(FirmwareRepository.compareVersions("1.10.0", "1.9.9") > 0)
        assertTrue(FirmwareRepository.compareVersions("2.0.0", "10.0.0") < 0)
        assertEquals(0, FirmwareRepository.compareVersions("1.2", "1.2.0"))
        assertEquals(0, FirmwareRepository.compareVersions("v1.2.3", "1.2.3"))
    }

    @Test
    fun `compareVersions ranks a pre-release below the release itself`() {
        assertTrue(FirmwareRepository.compareVersions("1.2.0-beta", "1.2.0") < 0)
        assertTrue(FirmwareRepository.compareVersions("1.2.0", "1.2.0-beta") > 0)
        assertTrue(FirmwareRepository.compareVersions("1.2.0-beta", "1.2.0-alpha") > 0)
    }

    @Test
    fun `compareVersions does not throw on a version it cannot parse`() {
        assertEquals(0, FirmwareRepository.compareVersions("", ""))
        assertTrue(FirmwareRepository.compareVersions("1.0.0", "nonsense") > 0)
    }

    // ============================================
    // Download reasons
    // ============================================

    @Test
    fun `an expired link is explained as one`() {
        assertTrue(httpReason(403).contains("expired"))
        assertTrue(httpReason(404).contains("no longer available"))
        assertTrue(httpReason(500).contains("500"))
        assertTrue(httpReason(418).contains("418"))
    }
}
