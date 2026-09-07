package com.safeshade.cloud.sync

import com.safeshade.cloud.CloudClient
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.data.EvidenceClip
import com.safeshade.data.EvidenceUploadState
import com.safeshade.repo.EvidenceRepository
import java.io.File

/**
 * The bytes half of an evidence clip: the `.m4a` between `filesDir/evidence`
 * and the private `evidence` bucket.
 *
 * ### The same split as [VoiceCloud], for the same reason
 *
 * A clip is two facts - a row in `evidence` and an object in storage that the
 * row's `storage_path` points at. Keeping them in one class would put a network
 * upload inside [PayloadResolver], which is pure by design and tested as such.
 * So **this** gets a clip to [EvidenceUploadState.UPLOADED], and only then can
 * the resolver produce a row for it. The invariant that falls out of the
 * ordering is the one that matters: no row exists for audio that is not in the
 * bucket, so no guardian is ever shown a recording they cannot play.
 *
 * ### What is different from a voice note, and it is the important part
 *
 * A voice note is something a person chose to send. An evidence clip is a
 * recording of the room somebody was in during the worst minute of their week,
 * and it leaves the phone **only** because they said it may. That difference
 * shows up here as two rules this class does not break:
 *
 *  1. **[EvidenceUploadState.LOCAL_ONLY] is never written from here.**
 *     `VoiceCloud` demotes a note to `LocalOnly` when the build has no cloud;
 *     doing the same to a clip would forge the user's answer to the upload
 *     question, and `EvidenceRepository.delete` reads that same state to decide
 *     whether a tombstone is owed on a row the server may well be holding.
 *
 *     Reading a clip that *is* LOCAL_ONLY is not the same thing and is not
 *     refused: reaching this class at all means the outbox holds an entry for
 *     the clip, and `EvidenceRepository.add` only queues one when the opt-in
 *     answered yes. The state simply has not caught up - see below.
 *  2. **A missing file is a failure with a sentence, never a skip.** Three
 *     skips end as "there was nothing left on this phone to send for this",
 *     which about a recording is both wrong and alarming. The clip goes to
 *     [EvidenceUploadState.FAILED] carrying a reason a person can read.
 *
 * ### `QUEUED` is stamped here as well as in the repository
 *
 * `EvidenceRepository.add` marks a clip QUEUED after handing its id to the
 * hooks - and the hooks kick a drain, so the drain can reach this class before
 * that write lands. Rather than race it, the state is set here too, on the way
 * into the upload. It is idempotent and it is what makes the first thing the
 * user sees on a fresh clip honest.
 *
 * @param evidenceDir `filesDir/evidence`, or null where there is no file
 *   system - the same convention `EvidenceRepository` uses.
 */
class EvidenceCloud(
    private val client: CloudClient,
    private val clips: EvidenceRepository?,
    private val evidenceDir: File?
) {

    /**
     * Puts a clip's audio in the bucket if it is not there already.
     *
     * @return null when the clip is now [EvidenceUploadState.UPLOADED] and its
     *   row may be built, or the resolution the push should report instead.
     */
    suspend fun ensureUploaded(clip: EvidenceClip, circleId: String): PayloadResolution? {
        if (clip.upload == EvidenceUploadState.UPLOADED) return null
        val repo = clips ?: return PayloadResolution.Skip

        val path = PayloadResolver.evidencePath(clip.id, circleId)
        val file = fileOf(clip)
        if (file == null || !file.isFile) {
            // Nothing to upload and nothing waiting will fix. Reported as a
            // failure rather than left as a skip, because "the recording is
            // gone" is a thing the person should be told rather than an entry
            // quietly counting to three.
            val reason = MISSING
            repo.setUploadState(clip.id, EvidenceUploadState.FAILED, reason)
            return PayloadResolution.Failed(reason, retryable = false)
        }

        val bytes = runCatching { file.readBytes() }.getOrNull()
            ?: run {
                val reason = "That recording could not be read from this phone."
                repo.setUploadState(clip.id, EvidenceUploadState.FAILED, reason)
                return PayloadResolution.Failed(reason, retryable = false)
            }

        // See the class KDoc: idempotent, and it beats the race with `add`.
        repo.setUploadState(clip.id, EvidenceUploadState.QUEUED, null)

        return when (
            val result = client.uploadPrivate(
                CloudTables.Buckets.EVIDENCE,
                path,
                bytes,
                PayloadResolver.EVIDENCE_MIME
            )
        ) {
            // Uploaded, and only now. Nothing draws a tick before this line.
            is CloudResult.Ok -> {
                repo.setUploadState(clip.id, EvidenceUploadState.UPLOADED, null)
                null
            }

            is CloudResult.Failed -> {
                repo.setUploadState(clip.id, EvidenceUploadState.FAILED, result.reason)
                PayloadResolution.Failed(result.reason, result.retryable)
            }

            // No cloud on this build. The clip is left in whatever state it
            // holds - notably *not* demoted to LOCAL_ONLY, which would be this
            // class answering the upload question on the user's behalf - and
            // the entry is skipped rather than penalised. The push holds
            // everything in this case anyway.
            CloudResult.Disabled -> PayloadResolution.Skip
        }
    }

    /** The `.m4a` for [clip], or null when there is no directory or no name. */
    private fun fileOf(clip: EvidenceClip): File? {
        val dir = evidenceDir ?: return null
        val name = clip.file.takeIf { it.isNotBlank() } ?: return null
        // File(name).name for the same reason EvidenceRepository does it: the
        // field is a name, and a path that escaped the folder must not.
        return File(dir, File(name).name)
    }

    companion object {
        /**
         * What a clip whose audio has been trimmed away reports.
         *
         * Written for the person whose recording it was, and kept here so the
         * test and the row agree on one sentence.
         */
        const val MISSING = "The recording is no longer on this phone."
    }
}
