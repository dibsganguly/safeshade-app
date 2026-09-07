package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.KIND_TEXT
import com.safeshade.cloud.dto.KIND_VOICE
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.MergeRules
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.PayloadResolution
import com.safeshade.cloud.sync.VoiceCloud
import com.safeshade.cloud.sync.VoiceNoteStore
import com.safeshade.data.MessageChannel
import com.safeshade.data.QuickMessage
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
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
 * Voice notes, end to end through the sync layer.
 *
 * Three things are being defended here, and each of them was a real failure
 * before this pass:
 *
 *  1. **A voice note's id resolves to a row at all.** It queues against
 *     `messages`, the resolver only knew about typed messages, so the id
 *     resolved to null - a skip - and three drains later the outbox reported
 *     "There was nothing left on this phone to send for this" about a recording
 *     that was sitting on the phone.
 *  2. **No row exists before the audio does.** A row is a pointer into the
 *     `voice` bucket; a row pushed ahead of its object is a note the rest of the
 *     Circle can see, tap, and fail to play.
 *  3. **A failed upload says what actually went wrong**, on the note and on the
 *     outbox entry, and does not spend the entry's three skips getting there.
 */
class VoiceSyncTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val circle = "11111111-1111-4111-8111-111111111111"
    private val user = "22222222-2222-4222-8222-222222222222"
    private val noteId = "44444444-4444-4444-8444-444444444444"

    private val note = VoiceNote(
        id = noteId,
        wearerId = null,
        fromGuardian = false,
        authorName = "Amma",
        file = "note.m4a",
        durationMs = 4200,
        waveform = listOf(0.1f, 0.9f, 0.4f),
        createdAt = 1_700_000_000_000L,
        uploadState = VoiceUpload.LocalOnly
    )

    private fun snapshot(vararg notes: VoiceNote) = SyncSnapshot(
        circleId = circle,
        userId = user,
        voiceNotes = notes.toList()
    )

    private fun resolve(s: SyncSnapshot, id: String = noteId): JsonObject? =
        PayloadResolver.resolve(CloudTables.MESSAGES, id, OutboxOp.UPSERT, s)

    // ============================================
    // PAYLOAD
    // ============================================

    @Test
    fun `a local-only note resolves to nothing, because its audio is nowhere`() {
        assertNull(resolve(snapshot(note)))
    }

    @Test
    fun `a note whose upload failed still resolves to nothing`() {
        val failed = note.copy(uploadState = VoiceUpload.Failed("No internet."))
        assertNull(resolve(snapshot(failed)))
    }

    @Test
    fun `an uploaded note resolves to a voice row carrying its audio and its picture`() {
        val path = PayloadResolver.voicePath(noteId, circle)
        val row = resolve(snapshot(note.copy(uploadState = VoiceUpload.Uploaded(path))))

        assertNotNull("a voice note id must resolve to a row", row)
        requireNotNull(row)
        assertEquals(KIND_VOICE, row["kind"]?.jsonPrimitive?.content)
        assertEquals("$circle/$noteId.m4a", row["audio_path"]?.jsonPrimitive?.content)
        assertEquals(4200, row["duration_ms"]?.jsonPrimitive?.content?.toInt())
        assertEquals(circle, row["circle_id"]?.jsonPrimitive?.content)
        assertEquals(CloudIds.cloudId(noteId, circle), row["id"]?.jsonPrimitive?.content)
        assertEquals(user, row["author_id"]?.jsonPrimitive?.content)
        assertEquals(false, row["from_guardian"]?.jsonPrimitive?.content?.toBoolean())
        // A spoken note has no words, and none are invented for it.
        assertEquals(JsonNull, row["text"])
        assertNotNull(row["sent_at"]?.jsonPrimitive?.content)

        val waveform = row["waveform"]?.jsonArray
        assertNotNull(waveform)
        assertEquals(3, requireNotNull(waveform).size)
    }

    @Test
    fun `a typed message says so, so the discriminator is never guessed`() {
        val message = QuickMessage(
            id = "55555555-5555-4555-8555-555555555555",
            text = "On my way",
            fromGuardian = true,
            timestamp = 1_700_000_100_000L,
            channel = MessageChannel.BLE
        )
        val row = PayloadResolver.resolve(
            CloudTables.MESSAGES,
            message.id,
            OutboxOp.UPSERT,
            SyncSnapshot(circleId = circle, userId = user, messages = listOf(message))
        )
        assertEquals(KIND_TEXT, requireNotNull(row)["kind"]?.jsonPrimitive?.content)
    }

    /**
     * `kind` is `not null` and the rows are encoded with explicit nulls, so a
     * tombstone that left it out - or sent it as null - would be rejected by the
     * constraint, and rejected for the whole batch it travelled in.
     */
    @Test
    fun `every messages row, live or tombstone, carries a real kind and the same keys`() {
        val path = PayloadResolver.voicePath(noteId, circle)
        val voice = requireNotNull(resolve(snapshot(note.copy(uploadState = VoiceUpload.Uploaded(path)))))
        val message = QuickMessage(id = "66666666-6666-4666-8666-666666666666", text = "Hello")
        val text = requireNotNull(
            PayloadResolver.resolve(
                CloudTables.MESSAGES,
                message.id,
                OutboxOp.UPSERT,
                SyncSnapshot(circleId = circle, userId = user, messages = listOf(message))
            )
        )
        val tombstone = requireNotNull(
            PayloadResolver.resolve(CloudTables.MESSAGES, noteId, OutboxOp.DELETE, snapshot(note))
        )

        assertEquals(text.keys, voice.keys)
        assertEquals(text.keys, tombstone.keys)
        for (row in listOf(voice, text, tombstone)) {
            val kind = row["kind"]
            assertFalse("kind must never go on the wire as null", kind == JsonNull)
            assertTrue(
                "kind must be one the CHECK allows",
                kind?.jsonPrimitive?.content in setOf(KIND_TEXT, KIND_VOICE)
            )
        }
    }

    // ============================================
    // UPLOAD
    // ============================================

    @Test
    fun `a successful upload marks the note Uploaded and lets the row through`() = runBlocking {
        val dir = temp.newFolder("voice")
        File(dir, "note.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val store = FakeStore()
        val client = FakeCloudClient()
        val cloud = VoiceCloud(client, store, dir)

        assertNull("nothing should be held back", cloud.ensureUploaded(note, circle))
        assertEquals(
            VoiceUpload.Uploaded("$circle/$noteId.m4a"),
            store.states.last().second
        )
        // And the state before it was honest about being in flight.
        assertEquals(VoiceUpload.Uploading, store.states.first().second)
    }

    @Test
    fun `a failed upload puts the real reason on the note and on the outbox entry`() = runBlocking {
        val dir = temp.newFolder("voice")
        File(dir, "note.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val store = FakeStore()
        val client = FakeCloudClient().apply { failNext = "You are not connected to the internet." }
        val cloud = VoiceCloud(client, store, dir)

        val held = cloud.ensureUploaded(note, circle)

        // Not a skip. A skip counts towards MAX_SKIPS and would abandon the
        // entry with "there was nothing left on this phone to send for this",
        // about a recording that is right here.
        assertTrue(held is PayloadResolution.Failed)
        assertEquals(
            "You are not connected to the internet.",
            (held as PayloadResolution.Failed).reason
        )
        assertEquals(
            VoiceUpload.Failed("You are not connected to the internet."),
            store.states.last().second
        )
    }

    @Test
    fun `a note whose audio has gone says so rather than counting skips forever`() = runBlocking {
        val dir = temp.newFolder("voice")
        val store = FakeStore()
        val cloud = VoiceCloud(FakeCloudClient(), store, dir)

        val held = cloud.ensureUploaded(note, circle)

        assertTrue(held is PayloadResolution.Failed)
        assertFalse((held as PayloadResolution.Failed).retryable)
        val state = store.states.last().second
        assertTrue(state is VoiceUpload.Failed)
        assertEquals("That recording is no longer on this phone.", (state as VoiceUpload.Failed).reason)
    }

    @Test
    fun `an already uploaded note is not uploaded twice`() = runBlocking {
        val dir = temp.newFolder("voice")
        File(dir, "note.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val store = FakeStore()
        val cloud = VoiceCloud(FakeCloudClient(), store, dir)
        val done = note.copy(uploadState = VoiceUpload.Uploaded("$circle/$noteId.m4a"))

        assertNull(cloud.ensureUploaded(done, circle))
        assertTrue("nothing should have been written", store.states.isEmpty())
    }

    /**
     * A process killed mid-upload leaves the note in [VoiceUpload.Uploading].
     * The push is single-flight, so nothing is actually uploading it - waiting
     * would leave the note "sending" until the app was reinstalled.
     */
    @Test
    fun `a note stranded mid-upload is retried rather than waited on`() = runBlocking {
        val dir = temp.newFolder("voice")
        File(dir, "note.m4a").writeBytes(byteArrayOf(1, 2, 3))
        val store = FakeStore()
        val cloud = VoiceCloud(FakeCloudClient(), store, dir)

        assertNull(cloud.ensureUploaded(note.copy(uploadState = VoiceUpload.Uploading), circle))
        assertEquals(
            VoiceUpload.Uploaded("$circle/$noteId.m4a"),
            store.states.last().second
        )
    }

    // ============================================
    // DOWNLOAD
    // ============================================

    @Test
    fun `downloading a pulled note writes the file and points the row at it`() = runBlocking {
        val dir = temp.newFolder("voice")
        val store = FakeStore()
        val client = FakeCloudClient()
        val path = "$circle/$noteId.m4a"
        client.uploadPrivate(CloudTables.Buckets.VOICE, path, byteArrayOf(9, 9), "audio/mp4")

        val remote = note.copy(file = "", uploadState = VoiceUpload.Uploaded(path))
        val result = VoiceCloud(client, store, dir).downloadVoice(remote)

        assertTrue(result is CloudResult.Ok)
        assertTrue(File(dir, "$noteId.m4a").isFile)
        assertFalse("no half-written temp is left behind", File(dir, "$noteId.m4a.part").exists())

        // The row now names the file, through applyRemote so nothing is queued.
        val applied = store.applied.single()(listOf(remote))
        assertEquals("$noteId.m4a", applied.single().file)
    }

    @Test
    fun `a note that has not reached the cloud cannot be downloaded, and says why`() = runBlocking {
        val dir = temp.newFolder("voice")
        val result = VoiceCloud(FakeCloudClient(), FakeStore(), dir).downloadVoice(note)
        assertTrue(result is CloudResult.Failed)
        assertEquals(
            "That recording has not reached SafeShade Cloud yet.",
            (result as CloudResult.Failed).reason
        )
    }

    // ============================================
    // PULL ROUTING
    // ============================================

    /**
     * The reason routing by `kind` is not cosmetic: `MergeRules.messages` drops
     * a row with blank text, so a voice row handed to it is decoded, examined
     * and silently discarded. Before the split, that is exactly what happened
     * to every voice note pulled from another phone.
     */
    @Test
    fun `the message merge cannot carry a voice row, which is why they are split`() {
        val row = voiceRow()
        assertTrue(MergeRules.messages(emptyList(), listOf(row), circle).isEmpty())
    }

    @Test
    fun `a row with no kind is text, so rows written before 0006 still land`() {
        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; explicitNulls = false }
            .decodeFromString(
                MessageRow.serializer(),
                """{"id":"x","circle_id":"$circle","text":"hi","from_guardian":true}"""
            )
        assertEquals(KIND_TEXT, decoded.kind)
    }

    @Test
    fun `a pulled voice row becomes a note with no file yet and a path to fetch`() {
        val merged = MergeRules.voiceNotes(emptyList(), listOf(voiceRow()), circle)
        val incoming = merged.single()

        assertEquals("", incoming.file)
        assertEquals(VoiceUpload.Uploaded("$circle/$noteId.m4a"), incoming.uploadState)
        assertEquals(4200, incoming.durationMs)
        assertEquals(listOf(0.1f, 0.9f, 0.4f), incoming.waveform)
        assertFalse(incoming.fromGuardian)
        assertFalse(incoming.listened)
    }

    @Test
    fun `a row echoing this phone's own note does not forget the file or the state`() {
        val local = note.copy(
            uploadState = VoiceUpload.Uploaded("$circle/$noteId.m4a"),
            listened = true
        )
        val merged = MergeRules.voiceNotes(listOf(local), listOf(voiceRow()), circle)
        val kept = merged.single()

        assertEquals("note.m4a", kept.file)
        assertEquals(VoiceUpload.Uploaded("$circle/$noteId.m4a"), kept.uploadState)
        assertTrue("listened is this phone's own fact", kept.listened)
    }

    @Test
    fun `a queued local edit wins over an arriving row`() {
        val local = note.copy(authorName = "Mine")
        val merged = MergeRules.voiceNotes(
            listOf(local),
            listOf(voiceRow().copy(durationMs = 99)),
            circle,
            pending = setOf(noteId)
        )
        assertEquals(4200, merged.single().durationMs)
    }

    @Test
    fun `a tombstone removes the note`() {
        val local = note.copy(uploadState = VoiceUpload.Uploaded("$circle/$noteId.m4a"))
        val merged = MergeRules.voiceNotes(
            listOf(local),
            listOf(voiceRow().copy(deletedAt = "2026-09-07T10:00:00Z")),
            circle
        )
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `a voice row with no audio path is not a note anything could play`() {
        val merged = MergeRules.voiceNotes(emptyList(), listOf(voiceRow().copy(audioPath = null)), circle)
        assertTrue(merged.isEmpty())
    }

    private fun voiceRow() = MessageRow(
        id = CloudIds.cloudId(noteId, circle),
        circleId = circle,
        authorId = user,
        text = null,
        fromGuardian = false,
        channel = "CLOUD",
        sentAt = "2026-09-07T09:00:00Z",
        kind = KIND_VOICE,
        audioPath = "$circle/$noteId.m4a",
        durationMs = 4200,
        waveform = listOf(0.1f, 0.9f, 0.4f)
    )

    /** Records what was written, so a test can assert the order as well as the end. */
    private class FakeStore : VoiceNoteStore {
        val states = mutableListOf<Pair<String, VoiceUpload>>()
        val applied = mutableListOf<(List<VoiceNote>) -> List<VoiceNote>>()

        override suspend fun setUploadState(id: String, state: VoiceUpload) {
            states += id to state
        }

        override suspend fun applyRemote(transform: (List<VoiceNote>) -> List<VoiceNote>) {
            applied += transform
        }
    }
}
