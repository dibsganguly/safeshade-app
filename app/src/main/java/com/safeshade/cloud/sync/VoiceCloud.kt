package com.safeshade.cloud.sync

import android.util.Log
import com.safeshade.cloud.CloudClient
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import java.io.File

/**
 * The bytes half of a voice note: the `.m4a` between `filesDir/voice` and the
 * private `voice` bucket.
 *
 * ### Why this is separate from the row
 *
 * A voice note is two facts - a `messages` row with `kind = 'voice'`, and an
 * object in storage that the row's `audio_path` points at. The row is cheap,
 * ordered, and merged by the ordinary sync rules; the object is a file upload
 * that can be slow and can fail on its own. Keeping them in one class would put
 * a network upload inside [PayloadResolver], which is pure by design and tested
 * as such.
 *
 * So the split is: **this** gets a note to [VoiceUpload.Uploaded], and only then
 * can the resolver produce a row for it. The invariant that falls out of the
 * ordering is the one that matters - **no row exists for audio that is not in
 * the bucket** - which means no phone in the Circle can ever be shown a voice
 * note it cannot play.
 *
 * ### The note's own state is the honest one
 *
 * Every outcome is written back through [VoiceNoteStore.setUploadState],
 * verbatim. [VoiceUpload.Failed] carries whatever refused it - "You are not
 * connected to the internet", a storage rejection, or "That recording is no
 * longer on this phone" when the file has been trimmed away underneath the row.
 * A note never shows a tick before the upload has actually returned OK, and it
 * never shows "sending" about something that stopped.
 *
 * ### `Uploading` is treated as stale, not as in-flight
 *
 * [ensureUploaded] is only ever called from the push, which runs under
 * `SyncEngine`'s single-flight drain lock - so if a note is found in
 * [VoiceUpload.Uploading] here, nothing is actually uploading it. The only way
 * to reach that state is a process that died mid-upload. Retrying it is
 * correct; waiting for it would leave the note "sending" until the app was
 * reinstalled.
 *
 * @param voiceDir `filesDir/voice`, or null where there is no file system -
 *   the same convention as `VoiceNoteRepository`, and half the reason this
 *   class is constructible in a unit test. The other half is [notes].
 */
class VoiceCloud(
    private val client: CloudClient,
    private val notes: VoiceNoteStore?,
    private val voiceDir: File?
) {

    /**
     * Puts a note's audio in the bucket if it is not there already.
     *
     * @return null when the note is now [VoiceUpload.Uploaded] and its row may
     *   be built, or the resolution the push should report instead.
     */
    suspend fun ensureUploaded(note: VoiceNote, circleId: String): PayloadResolution? {
        if (note.uploadState is VoiceUpload.Uploaded) return null
        val repo = notes ?: return PayloadResolution.Skip

        val path = PayloadResolver.voicePath(note.id, circleId)
        val file = fileOf(note)
        if (file == null || !file.isFile) {
            // Nothing to upload and nothing that waiting will fix. Reported as
            // a failure on the note rather than left as a skip, because "the
            // recording is gone" is a thing the person should be told rather
            // than an entry quietly counting to three.
            val reason = "That recording is no longer on this phone."
            repo.setUploadState(note.id, VoiceUpload.Failed(reason))
            return PayloadResolution.Failed(reason, retryable = false)
        }

        val bytes = runCatching { file.readBytes() }.getOrNull()
            ?: run {
                val reason = "That recording could not be read from this phone."
                repo.setUploadState(note.id, VoiceUpload.Failed(reason))
                return PayloadResolution.Failed(reason, retryable = false)
            }

        repo.setUploadState(note.id, VoiceUpload.Uploading)

        return when (val result = client.uploadPrivate(CloudTables.Buckets.VOICE, path, bytes, MIME)) {
            is CloudResult.Ok -> {
                // Uploaded, and only now. The location is the storage path, so
                // any phone holding the row can name the object exactly.
                repo.setUploadState(note.id, VoiceUpload.Uploaded(path))
                null
            }

            is CloudResult.Failed -> {
                repo.setUploadState(note.id, VoiceUpload.Failed(result.reason))
                PayloadResolution.Failed(result.reason, result.retryable)
            }

            // No cloud on this build. The note goes back to being local-only,
            // which is exactly what it is, and the entry is skipped rather than
            // penalised - the push holds everything in this case anyway.
            CloudResult.Disabled -> {
                repo.setUploadState(note.id, VoiceUpload.LocalOnly)
                PayloadResolution.Skip
            }
        }
    }

    /**
     * Fetches a note that arrived from another phone, into `filesDir/voice`.
     *
     * A pulled note has an empty [VoiceNote.file] until this runs: the row says
     * a recording exists and where it is, and nothing is downloaded until
     * somebody asks to hear it. Twenty seconds of AAC is small, but a guardian
     * with a chatty family should not be paying for a thread they have not
     * opened.
     *
     * The bytes land in a temporary file and are renamed into place only after
     * the whole download has succeeded, so a failure halfway cannot leave a
     * truncated `.m4a` that the player would open and report as corrupt.
     *
     * The row's `file` is set through [VoiceNoteStore.applyRemote] rather
     * than a hook-firing write, for the reason that method exists: this is a
     * fact about a remote record, and queuing a push for it would echo it back.
     */
    suspend fun downloadVoice(note: VoiceNote): CloudResult<File> {
        val repo = notes
            ?: return CloudResult.Failed("There is nowhere to save that recording.", false)
        val dir = voiceDir
            ?: return CloudResult.Failed("There is nowhere to save that recording.", false)

        val existing = fileOf(note)
        if (existing != null && existing.isFile && existing.length() > 0L) {
            return CloudResult.Ok(existing)
        }

        val path = (note.uploadState as? VoiceUpload.Uploaded)?.location
            ?: return CloudResult.Failed(
                "That recording has not reached SafeShade Cloud yet.",
                retryable = false
            )

        val bytes = when (val result = client.downloadPrivate(CloudTables.Buckets.VOICE, path)) {
            is CloudResult.Ok -> result.value
            is CloudResult.Failed -> return result
            CloudResult.Disabled -> return CloudResult.Disabled
        }

        val name = "${note.id}.m4a"
        val written = runCatching {
            if (!dir.exists()) dir.mkdirs()
            val temp = File(dir, "$name.part")
            temp.writeBytes(bytes)
            val target = File(dir, name)
            if (target.exists()) target.delete()
            if (!temp.renameTo(target)) {
                // A rename across the same directory does not normally fail;
                // when it does, copying is still correct and the half-written
                // temp file is cleaned up either way.
                target.writeBytes(bytes)
                temp.delete()
            }
            target
        }.getOrElse { error ->
            Log.w(TAG, "voice download write failed: " + error.javaClass.simpleName)
            return CloudResult.Failed("That recording could not be saved on this phone.", true)
        }

        repo.applyRemote { current ->
            current.map { if (it.id == note.id) it.copy(file = name) else it }
        }
        return CloudResult.Ok(written)
    }

    /** The `.m4a` for [note], or null when there is no directory or no name. */
    private fun fileOf(note: VoiceNote): File? {
        val dir = voiceDir ?: return null
        val name = note.file.takeIf { it.isNotBlank() } ?: return null
        // File(name).name for the same reason VoiceNoteRepository does it: the
        // field is a name, and a path that escaped the folder must not.
        return File(dir, File(name).name)
    }

    private companion object {
        const val TAG = "SafeShadeSync"

        /** What `MediaRecorder`'s AAC output actually is. */
        const val MIME = "audio/mp4"
    }
}

/**
 * The two things [VoiceCloud] needs to do to the note list.
 *
 * `VoiceNoteRepository` implements it and is the only production
 * implementation. It exists so that the upload and download paths - the two
 * places in this feature where the app could tell somebody their recording had
 * been sent when it had not - are reachable from a JVM unit test, which a
 * DataStore-backed repository is not.
 *
 * Deliberately two methods and not the repository's whole surface. Nothing in
 * `cloud/` has any business adding, removing or marking a note listened; those
 * are things a person does, and they belong to the screen and the repository.
 */
interface VoiceNoteStore {

    /** See `VoiceNoteRepository.setUploadState`. Verbatim, never interpreted. */
    suspend fun setUploadState(id: String, state: VoiceUpload)

    /** See `VoiceNoteRepository.applyRemote`. Fires no sync hooks. */
    suspend fun applyRemote(transform: (List<VoiceNote>) -> List<VoiceNote>)
}
