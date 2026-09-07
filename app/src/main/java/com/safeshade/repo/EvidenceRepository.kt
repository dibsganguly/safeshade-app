package com.safeshade.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.data.EvidenceClip
import com.safeshade.data.EvidenceSettings
import com.safeshade.data.EvidenceStore
import com.safeshade.data.EvidenceUploadState
import com.safeshade.data.MAX_EVIDENCE_CLIPS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/**
 * The evidence clips: the rows, and the `.m4a` files under them.
 *
 * ### Why the rows and the files are managed together
 *
 * Same rule as [VoiceNoteRepository], and for the same reason. A clip is two
 * things - a row in `safeshade_evidence` and a file in `filesDir/evidence` -
 * and they can only go out of step in one direction that matters. A row with
 * no file is visible and reports itself; a file with no row is invisible,
 * unreachable and permanent. So every path that drops a row here deletes its
 * file first, inside the same lock, and the cap is applied in this class
 * rather than left to the store: capping on write alone would orphan one
 * recording of somebody's living room per clip past the limit, forever.
 *
 * ### The upload gate
 *
 * This is the part worth reading twice. Evidence is a recording of a room the
 * user was in during the worst minute of their week. It leaves this phone only
 * if they said it may, so [uploadOptIn] is consulted **at the moment a clip is
 * added**, not read once at construction: a user who turns the setting off
 * between two falls must not find the second clip queued because the
 * repository cached the answer to a question asked last Tuesday.
 *
 * When it answers false the clip stays [EvidenceUploadState.LOCAL_ONLY] and
 * [hooks] is not called at all - no queued id, no row on the server, nothing
 * for a later sign-in to drain.
 *
 * ### Why the directory is injected
 *
 * `repo/` holds no `Context`. The directory arrives as a plain [File] so this
 * class can be built in a unit test against a temporary folder, and so that
 * the "file name, never a path" rule on [EvidenceClip.file] has exactly one
 * place that joins the two. A null directory means "there is nowhere to delete
 * from", which is correct in a test that never wrote a file.
 */
class EvidenceRepository(
    private val store: EvidenceStore,
    private val scope: CoroutineScope,
    /** `filesDir/evidence`, or null when there is no file system to speak of. */
    private val evidenceDir: File? = null,
    /** See [SyncHooks]. Nothing is queued for the cloud without one. */
    private val hooks: SyncHooks = SyncHooks.None,
    /**
     * Whether the user has agreed that evidence may be uploaded. Asked afresh
     * on every [add]; see the class doc.
     */
    private val uploadOptIn: () -> Boolean = { false },
    /** Overridable so tests can control `capturedAt`. */
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /**
     * Null until the first store read completes.
     *
     * The distinction matters: null is "we have not looked yet" and an empty
     * list is "there are none", and a screen that drew "No recordings" for the
     * first would be stating a fact it does not have.
     */
    val clips: StateFlow<List<EvidenceClip>?> =
        store.clips.stateIn(scope, SharingStarted.Eagerly, null)

    /** The recording settings, straight through. */
    val settings: Flow<EvidenceSettings> = store.settings

    /** Serialises read-modify-write on the list. See [VoiceNoteRepository]. */
    private val lock = Mutex()

    /**
     * Records a finished recording.
     *
     * The clip is [EvidenceUploadState.LOCAL_ONLY] on return even when the
     * user has opted in, because on return that is the truth: the audio exists
     * on this phone and nowhere else. It becomes
     * [EvidenceUploadState.QUEUED] only once the id has actually been handed
     * to the outbox, and [EvidenceUploadState.UPLOADED] only when the sync
     * track says the server took it - nothing here draws a tick before an
     * upload has happened.
     *
     * @param file the file name under `filesDir/evidence`, as
     *   `EvidenceRecorder` created it. A path is accepted and reduced to its
     *   name rather than rejected, because the recorder hands its caller a
     *   `File` and the temptation to pass `absolutePath` is obvious.
     */
    suspend fun add(
        file: String,
        durationMs: Int,
        byteSize: Long,
        alertId: String? = null
    ): EvidenceClip {
        val clip = EvidenceClip(
            file = File(file).name,
            capturedAt = now(),
            durationMs = durationMs.coerceAtLeast(0),
            byteSize = byteSize.coerceAtLeast(0L),
            alertId = alertId?.takeIf { it.isNotBlank() },
            upload = EvidenceUploadState.LOCAL_ONLY
        )

        lock.withLock {
            val current = store.clips.first()
            store.setClips(trimmed(current + clip))
        }

        if (!uploadOptIn()) return clip

        hooks.onUpsert(CloudTables.EVIDENCE, clip.id)
        // Only now, and only if the row survived the cap it was just written
        // under. Marking it queued before the hook returned would claim a
        // queue position the outbox had not given it.
        setUploadState(clip.id, EvidenceUploadState.QUEUED, null)
        return clip.copy(upload = EvidenceUploadState.QUEUED)
    }

    /**
     * Records what the upload is doing now.
     *
     * Takes the state verbatim rather than a boolean pair, so a failure keeps
     * the reason whatever refused it gave - which is the only thing the row
     * can honestly show in place of a tick.
     */
    suspend fun setUploadState(
        id: String,
        state: EvidenceUploadState,
        reason: String? = null
    ) {
        lock.withLock {
            val current = store.clips.first()
            val updated = current.map {
                if (it.id == id) {
                    it.copy(upload = state, uploadReason = reason?.takeIf { r -> r.isNotBlank() })
                } else {
                    it
                }
            }
            if (updated != current) store.setClips(updated)
        }
    }

    /** Deletes a clip and its audio. */
    suspend fun delete(id: String) {
        val removed = lock.withLock {
            val current = store.clips.first()
            val gone = current.firstOrNull { it.id == id } ?: return@withLock null
            deleteFileOf(gone)
            store.setClips(current.filterNot { it.id == id })
            gone
        } ?: return

        // Only a clip that reached the server has a row there to soft-delete.
        // Sending a delete for a LOCAL_ONLY clip would queue a tombstone for
        // an id the server has never seen, which fails forever and holds an
        // outbox slot. A clip that was uploaded before the user turned the
        // setting off still gets its delete: opting out of new uploads must
        // not strand what is already up there.
        if (removed.upload != EvidenceUploadState.LOCAL_ONLY) {
            hooks.onDelete(CloudTables.EVIDENCE, removed.id)
        }
    }

    /** Everything recorded around one trip, oldest first. */
    fun forAlert(alertId: String): List<EvidenceClip> =
        clips.value.orEmpty().filter { it.alertId == alertId }

    /** Writes the recording settings through to the store. */
    suspend fun setSettings(settings: EvidenceSettings) {
        store.setSettings(
            settings.copy(
                durationSeconds = settings.durationSeconds
                    .coerceIn(com.safeshade.data.MIN_EVIDENCE_SECONDS, com.safeshade.data.MAX_EVIDENCE_SECONDS)
            )
        )
    }

    /**
     * Applies the cap, deleting the audio of everything it drops.
     *
     * Oldest first, matching the list's own ordering, so what falls off the
     * end is the recording least likely to still be wanted.
     */
    private fun trimmed(clips: List<EvidenceClip>): List<EvidenceClip> {
        if (clips.size <= MAX_EVIDENCE_CLIPS) return clips
        val kept = clips.takeLast(MAX_EVIDENCE_CLIPS)
        val keptIds = kept.mapTo(mutableSetOf()) { it.id }
        clips.filterNot { it.id in keptIds }.forEach { deleteFileOf(it) }
        return kept
    }

    private fun deleteFileOf(clip: EvidenceClip) {
        val dir = evidenceDir ?: return
        val name = clip.file
        if (name.isBlank()) return
        // Nothing here throws: a file already gone, a read-only directory and
        // a name that tries to escape the folder all end the same way - the
        // row is dropped and the storage is at worst unchanged.
        runCatching {
            val target = File(dir, File(name).name)
            if (target.exists()) target.delete()
        }
    }
}
