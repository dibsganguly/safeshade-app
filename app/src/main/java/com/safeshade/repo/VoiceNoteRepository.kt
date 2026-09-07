package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.PrefsLimits
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The Talk thread's voice notes: the rows, and the files under them.
 *
 * ### Why the rows and the files are managed together
 *
 * A voice note is two things - a record in DataStore and an `.m4a` in
 * `filesDir/voice` - and they can only go out of step in one direction that
 * matters. A row with no file is visible and reports itself; a file with no row
 * is invisible, unreachable and permanent. So every path that drops a row here
 * deletes its file first, inside the same lock, and the cap is applied in this
 * class rather than being left to `setVoiceNotes` alone: capping only on write
 * would silently orphan one file per note past the limit, forever, with nothing
 * left pointing at them.
 *
 * The exception is a note that is mid-upload. Its file is being read by
 * whatever is uploading it, so the row is kept along with the file until that
 * finishes or fails - one stale row is a far cheaper failure than a file
 * deleted out from under a reader.
 *
 * ### Why the directory is injected
 *
 * `repo/` holds no `Context`. The directory arrives as a plain [File] so this
 * class can be constructed in a unit test against a temporary folder, and so
 * that the "file name, never a path" rule on [VoiceNote.file] has exactly one
 * place that joins the two. A null directory means "there is nowhere to delete
 * from", which is the correct behaviour in a test that never wrote a file.
 */
class VoiceNoteRepository(
    private val prefs: SafeShadePreferences,
    private val scope: CoroutineScope,
    /** `filesDir/voice`, or null when there is no file system to speak of. */
    private val voiceDir: File? = null,
    /** See [SyncHooks]. Nothing is queued for the cloud without one. */
    private val hooks: SyncHooks = SyncHooks.None
) {

    /** Null until the first DataStore read completes. See [ProfileRepository]. */
    val notes: StateFlow<List<VoiceNote>?> =
        prefs.voiceNotes.stateIn(scope, SharingStarted.Eagerly, null)

    /** Serialises read-modify-write on the list. See [SafetyRepository]. */
    private val lock = Mutex()

    /**
     * Records a finished recording and queues it for the circle.
     *
     * The note is [VoiceUpload.LocalOnly] on return, which is the honest state:
     * the audio exists on this phone and nowhere else. It becomes anything else
     * only when the sync track says so, through [setUploadState] - nothing here
     * draws a tick before an upload has actually completed.
     *
     * @param file the file name under `filesDir/voice`, as
     *   `VoiceRecorder.start` created it. A path is accepted and reduced to its
     *   name rather than being rejected, because the recorder hands its caller
     *   a `File` and the temptation to pass `absolutePath` is obvious.
     */
    suspend fun add(
        file: String,
        durationMs: Int,
        waveform: List<Float>,
        wearerId: String?,
        fromGuardian: Boolean,
        authorName: String
    ): VoiceNote {
        val note = VoiceNote(
            wearerId = wearerId,
            fromGuardian = fromGuardian,
            authorName = authorName.trim(),
            file = File(file).name,
            durationMs = durationMs.coerceAtLeast(0),
            waveform = waveform.map { it.coerceIn(0f, 1f) },
            uploadState = VoiceUpload.LocalOnly
        )

        lock.withLock {
            val current = prefs.voiceNotes.first()
            prefs.setVoiceNotes(trimmed(current + note))
        }

        // The row shape on the server is the sync track's problem; this queues
        // the id and nothing more. `messages` rather than a table of its own:
        // a voice note is a message on the same thread, and a second table
        // would mean two orderings to reconcile for one conversation.
        hooks.onUpsert(CloudTables.MESSAGES, note.id)
        return note
    }

    /**
     * Marks a note as heard.
     *
     * Not queued for the cloud. "I have listened to this" is a fact about this
     * phone's reader, and syncing it would make one guardian opening the thread
     * mark the note read for everybody else in the circle.
     */
    suspend fun markListened(id: String) = mutate { notes ->
        notes.map { if (it.id == id) it.copy(listened = true) else it }
    }

    /** Deletes a note and its audio. */
    suspend fun remove(id: String) {
        val removed = lock.withLock {
            val current = prefs.voiceNotes.first()
            val gone = current.firstOrNull { it.id == id } ?: return@withLock null
            deleteFileOf(gone)
            prefs.setVoiceNotes(current.filterNot { it.id == id })
            gone
        } ?: return
        hooks.onDelete(CloudTables.MESSAGES, removed.id)
    }

    /**
     * Records what the upload is doing now.
     *
     * Takes the state verbatim rather than a boolean pair, so a failure keeps
     * the reason whatever refused it gave - which is the only thing the row can
     * honestly show instead of a tick.
     */
    suspend fun setUploadState(id: String, state: VoiceUpload) = mutate { notes ->
        notes.map { if (it.id == id) it.copy(uploadState = state) else it }
    }

    /**
     * Notes arriving from another phone in the circle.
     *
     * Deliberately without calling [hooks], for the same reason as
     * `SafetyRepository.applyRemoteHistory`: a pulled row that queued its own
     * push would be echoed back, re-pulled, and never settle.
     */
    suspend fun applyRemote(transform: (List<VoiceNote>) -> List<VoiceNote>) {
        lock.withLock {
            val current = prefs.voiceNotes.first()
            val updated = transform(current)
            if (updated != current) prefs.setVoiceNotes(updated)
        }
    }

    private suspend fun mutate(transform: (List<VoiceNote>) -> List<VoiceNote>) {
        lock.withLock {
            val current = prefs.voiceNotes.first()
            val updated = transform(current)
            if (updated != current) prefs.setVoiceNotes(updated)
        }
    }

    /**
     * Applies the cap, deleting the audio of everything it drops.
     *
     * Oldest first, matching the list's own ordering, so the notes that fall
     * off the end are the ones nobody has played in a long time.
     */
    private fun trimmed(notes: List<VoiceNote>): List<VoiceNote> {
        if (notes.size <= PrefsLimits.VOICE_NOTES) return notes
        val kept = notes.takeLast(PrefsLimits.VOICE_NOTES)
        val keptIds = kept.mapTo(mutableSetOf()) { it.id }
        notes.filterNot { it.id in keptIds }.forEach { deleteFileOf(it) }
        return kept
    }

    /**
     * Removes a note's audio, unless something is reading it.
     *
     * An uploaded note's file goes too: the copy that matters is the one on the
     * server, the row that pointed here is gone, and keeping the local `.m4a`
     * would be keeping a file nothing can name.
     */
    private fun deleteFileOf(note: VoiceNote) {
        if (note.uploadState == VoiceUpload.Uploading) return
        val dir = voiceDir ?: return
        val name = note.file
        if (name.isBlank()) return
        // Nothing here throws: a file already gone, a read-only directory and a
        // name that escapes the folder all end the same way - the row is
        // dropped and the storage is at worst unchanged.
        runCatching {
            val target = File(dir, File(name).name)
            if (target.exists()) target.delete()
        }
    }
}
