package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.EvidenceCloud
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.PayloadResolution
import com.safeshade.data.EvidenceClip
import com.safeshade.data.EvidenceSettings
import com.safeshade.data.EvidenceStore
import com.safeshade.data.EvidenceUploadState
import com.safeshade.data.FallAlertEvent
import com.safeshade.repo.EvidenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Evidence clips, through the sync layer.
 *
 * An evidence clip is a recording of the room somebody was in during the worst
 * minute of their week. Three things are defended here, and the first two are
 * the same invariants voice notes have:
 *
 *  1. **No row exists before the audio does.** The row is a pointer into the
 *     private `evidence` bucket; a row pushed ahead of its object is a
 *     recording every guardian in the Circle can see, tap, and fail to play.
 *  2. **A missing file says so, in a sentence, rather than counting skips.**
 *     Three skips end as "there was nothing left on this phone to send for
 *     this", which about somebody's evidence is both wrong and alarming.
 *  3. **Nothing here writes LOCAL_ONLY.** That state is the user's answer to
 *     the upload question, and `EvidenceRepository.delete` reads it to decide
 *     whether the server is owed a tombstone. Forging it either uploads a
 *     recording that was never allowed to travel, or strands one that already
 *     has.
 */
class EvidenceSyncTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val circle = "11111111-1111-4111-8111-111111111111"
    private val clipId = "44444444-4444-4444-8444-444444444444"
    private val alertId = "33333333-3333-4333-8333-333333333333"

    private val clip = EvidenceClip(
        id = clipId,
        file = "clip.m4a",
        capturedAt = 1_700_000_000_000L,
        durationMs = 29_600,
        byteSize = 184_320L,
        alertId = alertId,
        upload = EvidenceUploadState.QUEUED
    )

    private fun snapshot(vararg clips: EvidenceClip) = SyncSnapshot(
        circleId = circle,
        userId = "22222222-2222-4222-8222-222222222222",
        alerts = listOf(FallAlertEvent(id = alertId, timestamp = 1_700_000_000_000L)),
        evidenceClips = clips.toList()
    )

    private fun resolve(s: SyncSnapshot, id: String = clipId, op: OutboxOp = OutboxOp.UPSERT) =
        PayloadResolver.resolve(CloudTables.EVIDENCE, id, op, s)

    private fun JsonObject.str(key: String): String? =
        this[key]?.takeIf { it != JsonNull }?.jsonPrimitive?.content

    // ============================================
    // PAYLOAD
    // ============================================

    @Test
    fun `a clip whose audio is not in the bucket resolves to nothing`() {
        assertNull(resolve(snapshot(clip)))
        assertNull(resolve(snapshot(clip.copy(upload = EvidenceUploadState.LOCAL_ONLY))))
        assertNull(
            resolve(
                snapshot(
                    clip.copy(upload = EvidenceUploadState.FAILED, uploadReason = "No internet.")
                )
            )
        )
    }

    @Test
    fun `an uploaded clip resolves to a row pointing at its object`() {
        val row = resolve(snapshot(clip.copy(upload = EvidenceUploadState.UPLOADED)))
        assertNotNull("an uploaded clip must resolve to a row", row)
        requireNotNull(row)

        assertEquals(circle, row.str("circle_id"))
        assertEquals(CloudIds.cloudId(clipId, circle), row.str("id"))
        assertEquals("audio", row.str("kind"))
        assertEquals(CloudTables.Buckets.EVIDENCE, row.str("bucket"))
        // The first path segment is the circle id, because the storage policy
        // reads it back out with path_circle_id(name). An object filed anywhere
        // else is unwritable by its recorder and unreadable by its circle.
        assertEquals("$circle/$clipId.m4a", row.str("storage_path"))
        assertEquals("audio/mp4", row.str("content_type"))
        assertEquals("184320", row.str("byte_size"))
        assertEquals(CloudIds.cloudId(alertId, circle), row.str("alert_id"))
        assertEquals("2023-11-14T22:13:20Z", row.str("captured_at"))
    }

    @Test
    fun `the duration is rounded rather than truncated`() {
        val row = requireNotNull(resolve(snapshot(clip.copy(upload = EvidenceUploadState.UPLOADED))))
        // 29.6 seconds is a thirty-second recording, not a twenty-nine.
        assertEquals("30", row.str("duration_seconds"))
    }

    /**
     * `evidence.alert_id` is a real foreign key. A clip recorded around a trip
     * the user has since removed from their log would carry a key to a row that
     * does not exist, and PostgREST rejects the whole batch it travelled in -
     * not just the one row.
     */
    @Test
    fun `an alert this phone no longer holds is not named as a foreign key`() {
        val orphan = clip.copy(upload = EvidenceUploadState.UPLOADED, alertId = "gone")
        val row = requireNotNull(resolve(snapshot(orphan)))
        assertEquals(JsonNull, row["alert_id"])
    }

    @Test
    fun `deleting a clip that reached the server produces a tombstone`() {
        val row = resolve(snapshot(), op = OutboxOp.DELETE)
        assertNotNull("a delete must resolve with the clip already gone", row)
        requireNotNull(row)
        assertEquals(CloudIds.cloudId(clipId, circle), row.str("id"))
        assertEquals(circle, row.str("circle_id"))
    }

    @Test
    fun `every evidence row, live or tombstone, carries the same keys`() {
        val live = requireNotNull(resolve(snapshot(clip.copy(upload = EvidenceUploadState.UPLOADED))))
        val tombstone = requireNotNull(resolve(snapshot(), op = OutboxOp.DELETE))
        assertEquals(live.keys, tombstone.keys)
        assertTrue("created_at" !in live.keys)
        assertTrue("updated_at" !in live.keys)
    }

    // ============================================
    // UPLOAD
    // ============================================

    @Test
    fun `a successful upload puts the bytes in the bucket and marks the clip uploaded`() =
        runBlocking {
            val dir = temp.newFolder("evidence")
            File(dir, "clip.m4a").writeBytes(byteArrayOf(1, 2, 3))
            val repo = repository(dir, clip)
            val client = FakeCloudClient()

            val held = EvidenceCloud(client, repo, dir).ensureUploaded(clip, circle)

            assertNull("nothing should be held back", held)
            assertEquals(
                EvidenceUploadState.UPLOADED,
                repo.clips.value.orEmpty().single().upload
            )
            assertNotNull(
                "the object must be in the private evidence bucket",
                client.storage[CloudTables.Buckets.EVIDENCE]?.get("$circle/$clipId.m4a")
            )
        }

    /**
     * `EvidenceRepository.add` marks a clip QUEUED *after* handing its id to the
     * hooks, and the hooks kick a drain - so the drain can arrive here first.
     * Rather than race that write, the state is stamped on the way in.
     */
    @Test
    fun `a clip is marked queued on its way into the upload`() = runBlocking {
        val dir = temp.newFolder("evidence")
        File(dir, "clip.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val fresh = clip.copy(upload = EvidenceUploadState.LOCAL_ONLY)
        val repo = repository(dir, fresh)

        val client = FakeCloudClient().apply { failNext = "You are not connected to the internet." }
        EvidenceCloud(client, repo, dir).ensureUploaded(fresh, circle)

        // The upload failed, so the end state is FAILED - but QUEUED was
        // written first, which is what the sequence proves.
        assertEquals(
            listOf(EvidenceUploadState.QUEUED, EvidenceUploadState.FAILED),
            statesSeen
        )
    }

    @Test
    fun `a failed upload puts the real reason on the clip and on the outbox entry`() =
        runBlocking {
            val dir = temp.newFolder("evidence")
            File(dir, "clip.m4a").writeBytes(byteArrayOf(1, 2, 3))
            val repo = repository(dir, clip)
            val client =
                FakeCloudClient().apply { failNext = "You are not connected to the internet." }

            val held = EvidenceCloud(client, repo, dir).ensureUploaded(clip, circle)

            // Not a skip. A skip counts towards MAX_SKIPS and would abandon the
            // entry with "there was nothing left on this phone to send for
            // this", about a recording that is right here.
            assertTrue(held is PayloadResolution.Failed)
            assertEquals(
                "You are not connected to the internet.",
                (held as PayloadResolution.Failed).reason
            )
            val stored = repo.clips.value.orEmpty().single()
            assertEquals(EvidenceUploadState.FAILED, stored.upload)
            assertEquals("You are not connected to the internet.", stored.uploadReason)
        }

    @Test
    fun `a clip whose audio has gone says so rather than counting skips forever`() = runBlocking {
        val dir = temp.newFolder("evidence")
        val repo = repository(dir, clip)

        val held = EvidenceCloud(FakeCloudClient(), repo, dir).ensureUploaded(clip, circle)

        assertTrue(held is PayloadResolution.Failed)
        assertFalse((held as PayloadResolution.Failed).retryable)
        assertEquals(EvidenceCloud.MISSING, held.reason)
        val stored = repo.clips.value.orEmpty().single()
        assertEquals(EvidenceUploadState.FAILED, stored.upload)
        assertEquals(EvidenceCloud.MISSING, stored.uploadReason)
    }

    /**
     * The rule `VoiceCloud` does not have. LOCAL_ONLY is the person's answer to
     * the upload question, and `EvidenceRepository.delete` reads it to decide
     * whether a tombstone is owed for a row the server may be holding. A build
     * with no project must not answer that question on their behalf.
     */
    @Test
    fun `a build with no cloud leaves the clip's state exactly as it was`() = runBlocking {
        val dir = temp.newFolder("evidence")
        File(dir, "clip.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val repo = repository(dir, clip)

        val held = EvidenceCloud(FakeCloudClient(disabled = true), repo, dir)
            .ensureUploaded(clip, circle)

        assertEquals(PayloadResolution.Skip, held)
        assertEquals(EvidenceUploadState.QUEUED, repo.clips.value.orEmpty().single().upload)
        assertTrue(
            "nothing in cloud/ may write LOCAL_ONLY",
            EvidenceUploadState.LOCAL_ONLY !in statesSeen
        )
    }

    @Test
    fun `an already uploaded clip is not uploaded twice`() = runBlocking {
        val dir = temp.newFolder("evidence")
        File(dir, "clip.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val done = clip.copy(upload = EvidenceUploadState.UPLOADED)
        val repo = repository(dir, done)
        val client = FakeCloudClient()

        assertNull(EvidenceCloud(client, repo, dir).ensureUploaded(done, circle))
        assertTrue("nothing should have been re-uploaded", client.storage.isEmpty())
        assertTrue("nothing should have been written", statesSeen.isEmpty())
    }

    // ============================================
    // FIXTURE
    // ============================================

    /** Every upload state written, in order, so a test can assert the sequence. */
    private val statesSeen = mutableListOf<EvidenceUploadState>()

    /**
     * A real [EvidenceRepository] over an in-memory store.
     *
     * The repository is the production one on purpose - `setUploadState` is the
     * method under test as much as `EvidenceCloud` is - and it is constructible
     * here only because `EvidenceStore` is an interface with no Android in it.
     */
    private fun repository(dir: File, vararg clips: EvidenceClip): EvidenceRepository =
        EvidenceRepository(
            store = FakeStore(clips.toList()),
            scope = CoroutineScope(Dispatchers.Unconfined),
            evidenceDir = dir,
            uploadOptIn = { true }
        )

    private inner class FakeStore(initial: List<EvidenceClip>) : EvidenceStore {
        private val state = MutableStateFlow(initial)
        override val clips: Flow<List<EvidenceClip>> = state
        override val settings: Flow<EvidenceSettings> =
            MutableStateFlow(EvidenceSettings(uploadToCloud = true))

        override suspend fun setClips(clips: List<EvidenceClip>) {
            clips.singleOrNull()?.let { statesSeen += it.upload }
            state.value = clips
        }

        override suspend fun setSettings(settings: EvidenceSettings) = Unit
    }
}
