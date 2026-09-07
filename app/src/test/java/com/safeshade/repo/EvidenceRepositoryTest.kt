package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.EvidenceClip
import com.safeshade.data.EvidenceSettings
import com.safeshade.data.EvidenceStore
import com.safeshade.data.EvidenceUploadState
import com.safeshade.data.MAX_EVIDENCE_CLIPS
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
 * The two rules that make this repository worth having: the cap never leaves a
 * file behind, and nothing is queued for the cloud unless the user said it may
 * be.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EvidenceRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** In-memory [EvidenceStore]. No DataStore, no Context, no Android. */
    private class FakeStore : EvidenceStore {
        private val _clips = MutableStateFlow<List<EvidenceClip>>(emptyList())
        private val _settings = MutableStateFlow(EvidenceSettings())

        override val clips: Flow<List<EvidenceClip>> = _clips
        override suspend fun setClips(clips: List<EvidenceClip>) {
            _clips.value = clips
        }

        override val settings: Flow<EvidenceSettings> = _settings
        override suspend fun setSettings(settings: EvidenceSettings) {
            _settings.value = settings
        }

        val current: List<EvidenceClip> get() = _clips.value
        val currentSettings: EvidenceSettings get() = _settings.value
    }

    /** Counts what would have been queued for the server, and for which table. */
    private class CountingHooks : SyncHooks {
        val upserts = mutableListOf<Pair<String, String>>()
        val deletes = mutableListOf<Pair<String, String>>()
        override suspend fun onUpsert(table: String, recordId: String) {
            upserts += table to recordId
        }

        override suspend fun onDelete(table: String, recordId: String) {
            deletes += table to recordId
        }
    }

    private fun writeAudio(dir: File, name: String): File =
        File(dir, name).apply { writeBytes(ByteArray(64) { 7 }) }

    // ============================================
    // The opt-in gate
    // ============================================

    @Test
    fun `a clip stays on the phone when the user has not opted in to upload`() = runTest {
        val store = FakeStore()
        val hooks = CountingHooks()
        val repo = EvidenceRepository(
            store = store,
            scope = backgroundScope,
            evidenceDir = folder.root,
            hooks = hooks,
            uploadOptIn = { false }
        )

        val clip = repo.add(file = "one.m4a", durationMs = 30_000, byteSize = 1024L)

        assertEquals(EvidenceUploadState.LOCAL_ONLY, clip.upload)
        assertEquals(EvidenceUploadState.LOCAL_ONLY, store.current.single().upload)
        assertTrue("nothing may be queued without an opt-in", hooks.upserts.isEmpty())
    }

    @Test
    fun `a clip is queued once, on the evidence table, when the user has opted in`() = runTest {
        val store = FakeStore()
        val hooks = CountingHooks()
        val repo = EvidenceRepository(
            store = store,
            scope = backgroundScope,
            evidenceDir = folder.root,
            hooks = hooks,
            uploadOptIn = { true }
        )

        val clip = repo.add(file = "one.m4a", durationMs = 30_000, byteSize = 1024L)

        assertEquals(listOf(CloudTables.EVIDENCE to clip.id), hooks.upserts)
        assertEquals(EvidenceUploadState.QUEUED, clip.upload)
        assertEquals(EvidenceUploadState.QUEUED, store.current.single().upload)
    }

    @Test
    fun `the opt-in is read at the moment of each clip, not cached`() = runTest {
        val store = FakeStore()
        val hooks = CountingHooks()
        var optedIn = true
        val repo = EvidenceRepository(
            store = store,
            scope = backgroundScope,
            evidenceDir = folder.root,
            hooks = hooks,
            uploadOptIn = { optedIn }
        )

        repo.add(file = "one.m4a", durationMs = 1_000, byteSize = 10L)
        optedIn = false
        repo.add(file = "two.m4a", durationMs = 1_000, byteSize = 10L)

        assertEquals(1, hooks.upserts.size)
        assertEquals(EvidenceUploadState.QUEUED, store.current[0].upload)
        assertEquals(EvidenceUploadState.LOCAL_ONLY, store.current[1].upload)
    }

    // ============================================
    // Files, the cap, and deletion
    // ============================================

    @Test
    fun `a path is reduced to a file name`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)
        repo.add(file = File(folder.root, "deep.m4a").absolutePath, durationMs = 1, byteSize = 1L)
        assertEquals("deep.m4a", store.current.single().file)
    }

    @Test
    fun `the cap keeps the newest fifty and deletes the audio of the rest`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)

        val files = (1..MAX_EVIDENCE_CLIPS + 3).map { writeAudio(folder.root, "clip-$it.m4a") }
        files.forEach { repo.add(file = it.name, durationMs = 1_000, byteSize = it.length()) }

        assertEquals(MAX_EVIDENCE_CLIPS, store.current.size)
        assertEquals("clip-4.m4a", store.current.first().file)
        assertEquals("clip-${MAX_EVIDENCE_CLIPS + 3}.m4a", store.current.last().file)

        // The whole point of applying the cap here rather than on write: an
        // orphaned recording of somebody's living room would otherwise sit in
        // filesDir forever with nothing left pointing at it.
        assertFalse(files[0].exists())
        assertFalse(files[1].exists())
        assertFalse(files[2].exists())
        assertTrue(files[3].exists())
    }

    @Test
    fun `deleting a clip deletes its audio`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)
        val audio = writeAudio(folder.root, "gone.m4a")

        val clip = repo.add(file = audio.name, durationMs = 1_000, byteSize = audio.length())
        repo.delete(clip.id)

        assertTrue(store.current.isEmpty())
        assertFalse(audio.exists())
    }

    @Test
    fun `deleting a local-only clip queues no tombstone`() = runTest {
        val store = FakeStore()
        val hooks = CountingHooks()
        val repo = EvidenceRepository(store, backgroundScope, folder.root, hooks, { false })

        val clip = repo.add(file = "local.m4a", durationMs = 1_000, byteSize = 10L)
        repo.delete(clip.id)

        // The server has never seen this id; a delete for it would fail forever.
        assertTrue(hooks.deletes.isEmpty())
    }

    @Test
    fun `deleting an uploaded clip does queue a tombstone even after opting out`() = runTest {
        val store = FakeStore()
        val hooks = CountingHooks()
        var optedIn = true
        val repo = EvidenceRepository(store, backgroundScope, folder.root, hooks, { optedIn })

        val clip = repo.add(file = "up.m4a", durationMs = 1_000, byteSize = 10L)
        repo.setUploadState(clip.id, EvidenceUploadState.UPLOADED, null)
        optedIn = false
        repo.delete(clip.id)

        assertEquals(listOf(CloudTables.EVIDENCE to clip.id), hooks.deletes)
    }

    @Test
    fun `an upload failure keeps the reason it was given`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)
        val clip = repo.add(file = "f.m4a", durationMs = 1_000, byteSize = 10L)

        repo.setUploadState(clip.id, EvidenceUploadState.FAILED, "The upload was refused: no session")

        val stored = store.current.single()
        assertEquals(EvidenceUploadState.FAILED, stored.upload)
        assertEquals("The upload was refused: no session", stored.uploadReason)

        repo.setUploadState(clip.id, EvidenceUploadState.UPLOADED, null)
        assertNull(store.current.single().uploadReason)
    }

    @Test
    fun `clips are found by the trip they belong to`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)
        repo.add(file = "a.m4a", durationMs = 1, byteSize = 1L, alertId = "trip-1")
        repo.add(file = "b.m4a", durationMs = 1, byteSize = 1L, alertId = "trip-2")
        repo.add(file = "c.m4a", durationMs = 1, byteSize = 1L, alertId = "trip-1")

        // clips is a StateFlow started Eagerly; wait for the store to answer.
        store.clips.first { it.size == 3 }
        repo.clips.first { it?.size == 3 }

        assertEquals(listOf("a.m4a", "c.m4a"), repo.forAlert("trip-1").map { it.file })
        assertTrue(repo.forAlert("trip-none").isEmpty())
    }

    @Test
    fun `settings clamp the duration on the way in`() = runTest {
        val store = FakeStore()
        val repo = EvidenceRepository(store, backgroundScope, folder.root)
        repo.setSettings(EvidenceSettings(recordOnFall = true, durationSeconds = 500))
        assertEquals(120, store.currentSettings.durationSeconds)
        assertTrue(store.currentSettings.recordOnFall)
    }
}
