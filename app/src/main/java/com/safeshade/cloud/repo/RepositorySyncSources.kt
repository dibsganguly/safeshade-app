package com.safeshade.cloud.repo

import com.safeshade.cloud.CloudSession
import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.KIND_VOICE
import com.safeshade.cloud.dto.MedicalIdRow
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.dto.ProfileRow
import com.safeshade.cloud.dto.SmartHomeHookRow
import com.safeshade.cloud.dto.WearerRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.PayloadResolution
import com.safeshade.cloud.sync.PayloadSource
import com.safeshade.cloud.sync.PullSource
import com.safeshade.cloud.sync.EvidenceCloud
import com.safeshade.cloud.sync.VoiceCloud
import com.safeshade.data.EvidenceUploadState
import com.safeshade.data.SafetySettings
import com.safeshade.data.UserRole
import com.safeshade.data.VoiceUpload
import com.safeshade.data.Wearer
import com.safeshade.repo.EvidenceRepository
import com.safeshade.repo.MessagingRepository
import com.safeshade.repo.ProfileRepository
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.SmartHomeRepository
import com.safeshade.repo.SyncKeys
import com.safeshade.repo.VitalsRepository
import com.safeshade.repo.VoiceNoteRepository
import com.safeshade.repo.ZoneRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * The Json used to read rows back off the wire.
 *
 * `ignoreUnknownKeys` because the server is allowed to grow a column before the
 * app knows about it - and the alternative is that adding one column to
 * `alerts` makes every pull on every unupdated phone throw, which would take
 * fall alerts down for everyone still on the old build.
 */
private val pullJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

private fun <T> decode(rows: List<JsonObject>, serializer: KSerializer<T>): List<T> =
    rows.mapNotNull { runCatching { pullJson.decodeFromJsonElement(serializer, it) }.getOrNull() }

/**
 * Everything the repositories hold, collapsed into a [SyncSnapshot].
 *
 * ### Why the snapshot is taken per entry and not per drain
 *
 * A drain resolves each queued record one at a time, and this is called once
 * per record. That looks wasteful and is not: every one of these reads is a
 * `StateFlow.value` on a flow the repository already keeps hot with
 * `SharingStarted.Eagerly`. No DataStore read happens here. What it buys is
 * that a record edited *during* a drain is sent as it stands now rather than as
 * it stood when the drain began, which is the same promise the outbox makes by
 * storing intents rather than payloads.
 *
 * ### It resolves nothing without a circle
 *
 * No circle means no `circle_id`, and every circle-scoped row has that column
 * as `not null`. Returning null here leaves the entry queued and un-penalised,
 * so everything accumulated before sign-in goes up on the first drain after it.
 */
class RepositoryPayloadSource(
    private val profiles: ProfileRepository,
    private val safety: SafetyRepository,
    private val messaging: MessagingRepository,
    private val zones: ZoneRepository,
    private val session: StateFlow<CloudSession>,
    private val circleId: suspend () -> String?,
    /**
     * The Talk thread's voice notes, or null on a graph built without them.
     *
     * Null is not a hypothetical: `CloudContainer.Repositories` carries this as
     * a defaulted parameter, so a build that has not wired it hands over null
     * and every voice-note id resolves to nothing here - which is the bug this
     * whole seam exists to fix, and is why the wiring is worth checking rather
     * than assuming.
     */
    private val voice: VoiceNoteRepository? = null,
    /** Gets a note's audio into the bucket before its row is built. */
    private val voiceCloud: VoiceCloud? = null,
    /**
     * The account holder's "share where alerts happened" switch.
     *
     * A lambda for the same reason [circleId] is one: it lives in
     * `CloudState`, which is built by `CircleManager`, which is constructed
     * before this. Defaulted to true so a graph without it behaves as the app
     * did before the switch existed.
     */
    private val shareAlertPlaces: suspend () -> Boolean = { true },
    /**
     * The vitals ring, or null on a graph built without it.
     *
     * Null is the same trap [voice] documents: with it absent every queued
     * sample id resolves to nothing, counts three skips, and reports itself as
     * a record this phone no longer holds - about readings sitting in the ring.
     */
    private val vitals: VitalsRepository? = null,
    /** The evidence clips, or null on a graph built without them. */
    private val evidence: EvidenceRepository? = null,
    /** Gets a clip's audio into the bucket before its row is built. */
    private val evidenceCloud: EvidenceCloud? = null,
    /** The smart-home hooks, or null on a graph built without them. */
    private val smartHome: SmartHomeRepository? = null
) : PayloadSource {

    override suspend fun payloadFor(
        table: String,
        recordId: String,
        op: OutboxOp
    ): JsonElement? {
        val snapshot = snapshot() ?: return null
        return PayloadResolver.resolve(table, recordId, op, snapshot)
    }

    /**
     * The row, a skip, or a reason - and the one place a voice note's audio is
     * uploaded.
     *
     * ### Why the upload happens here and not on a track of its own
     *
     * A background uploader would be a second schedule with its own retries,
     * its own backoff and its own idea of when the phone has a network,
     * fighting the one `SyncEngine` already owns. This codebase has that note
     * written down twice already (see `CloudClient.changes`). Instead the
     * upload happens at exactly the moment the row is wanted, inside the push,
     * under the drain lock - so it inherits the engine's triggers, its
     * single-flight rule and its backoff, and there is one schedule.
     *
     * ### What a person sees when it fails
     *
     * Both places agree, and both say the same thing. The note itself goes to
     * [com.safeshade.data.VoiceUpload.Failed] carrying the real reason -
     * "You are not connected to the internet", or whatever storage said - and
     * that is what the Talk thread draws against the note. The outbox entry is
     * marked failed with the *same* string, so the sync dot and the Account
     * page's last error say it too.
     *
     * This is the reason `PayloadResolution.Failed` exists. Returning null
     * would have made the outbox count a skip, and three skips report
     * "there was nothing left on this phone to send for this" about a recording
     * that is sitting on the phone and simply has not got out yet.
     *
     * A DELETE never uploads: a tombstone needs no audio, and re-uploading the
     * file of a note somebody has just deleted would be the opposite of what
     * they asked for.
     */
    override suspend fun resolve(
        table: String,
        recordId: String,
        op: OutboxOp
    ): PayloadResolution {
        val snapshot = snapshot() ?: return PayloadResolution.Skip

        if (table == CloudTables.MESSAGES && op == OutboxOp.UPSERT) {
            val note = snapshot.voiceNotes.firstOrNull { it.id == recordId }
            if (note != null) {
                // No uploader means no way to get the audio up, so no row may
                // be built. A skip, not a failure: nothing was attempted.
                val cloud = voiceCloud ?: return PayloadResolution.Skip
                val held = cloud.ensureUploaded(note, snapshot.circleId)
                if (held != null) return held

                // The upload returned OK. The note's persisted state has been
                // written for the Talk thread to draw, but `notes` is a
                // DataStore-backed flow and may not have emitted yet - so the
                // snapshot is patched here rather than re-read. The path is
                // derived, not remembered, so the two cannot disagree; re-
                // reading would risk resolving against a stale LocalOnly and
                // counting a skip against an upload that had just succeeded.
                val ready = snapshot.copy(
                    voiceNotes = snapshot.voiceNotes.map { current ->
                        // Only a note that was *not* already uploaded is
                        // patched. A note pulled from another phone is queued
                        // by the backfill too, and it already carries the path
                        // its own row named - re-deriving one over the top
                        // would rewrite somebody else's `audio_path` from this
                        // phone's arithmetic.
                        if (current.id != note.id ||
                            current.uploadState is VoiceUpload.Uploaded
                        ) {
                            current
                        } else {
                            current.copy(
                                uploadState = VoiceUpload.Uploaded(
                                    PayloadResolver.voicePath(current.id, snapshot.circleId)
                                )
                            )
                        }
                    }
                )
                return PayloadResolver.resolve(table, recordId, op, ready)
                    ?.let { PayloadResolution.Row(it) }
                    ?: PayloadResolution.Skip
            }
        }

        if (table == CloudTables.EVIDENCE && op == OutboxOp.UPSERT) {
            val clip = snapshot.evidenceClips.firstOrNull { it.id == recordId }
            if (clip != null) {
                // No uploader means no way to get the audio up, so no row may
                // be built. A skip, not a failure: nothing was attempted.
                val cloud = evidenceCloud ?: return PayloadResolution.Skip
                val held = cloud.ensureUploaded(clip, snapshot.circleId)
                if (held != null) return held

                // The upload returned OK. The clip's persisted state has been
                // written for the Safety page to draw, but `clips` is a
                // DataStore-backed flow and may not have emitted yet - so the
                // snapshot is patched here rather than re-read, exactly as the
                // voice path does. Re-reading would risk resolving against a
                // stale QUEUED and counting a skip against an upload that had
                // just succeeded.
                val ready = snapshot.copy(
                    evidenceClips = snapshot.evidenceClips.map { current ->
                        if (current.id == clip.id) {
                            current.copy(upload = EvidenceUploadState.UPLOADED, uploadReason = null)
                        } else {
                            current
                        }
                    }
                )
                return PayloadResolver.resolve(table, recordId, op, ready)
                    ?.let { PayloadResolution.Row(it) }
                    ?: PayloadResolution.Skip
            }
        }

        return PayloadResolver.resolve(table, recordId, op, snapshot)
            ?.let { PayloadResolution.Row(it) }
            ?: PayloadResolution.Skip
    }

    /**
     * True once there is a circle to stamp rows with.
     *
     * The difference between "this record cannot be sent" and "nothing can be
     * sent yet" - see [PayloadSource.isReady]. Signed out, or signed in before
     * the circle has resolved, the whole push is held rather than every queued
     * record being skipped towards a failure it did not earn.
     */
    override suspend fun isReady(): Boolean = loaded() && !circleId().isNullOrBlank()

    /**
     * True once every repository has read itself off disk.
     *
     * Each of these flows is `stateIn(scope, Eagerly, null)`, and null means
     * *not read yet* rather than *empty* - the same trap `AppState.Loading`
     * exists to avoid. Without this check, a drain that runs in the first few
     * hundred milliseconds of a cold start sees empty lists everywhere, finds no
     * row body for anything, and counts a skip against every queued record; three
     * of those and the whole backfill would report itself Failed to a user whose
     * data was simply still loading.
     */
    private fun loaded(): Boolean =
        profiles.profile.value != null &&
            profiles.wearers.value != null &&
            safety.settings.value != null &&
            safety.history.value != null &&
            messaging.messages.value != null &&
            zones.zones.value != null &&
            // Only when there is one. A graph without a voice repository is not
            // waiting for it, and blocking every push on a flow that will never
            // arrive would hold the whole queue forever.
            (voice == null || voice.notes.value != null) &&
            (vitals == null || vitals.samples.value != null) &&
            (evidence == null || evidence.clips.value != null) &&
            (smartHome == null || smartHome.hooks.value != null)

    /** Exposed for the tests; there is nothing here a test cannot construct. */
    suspend fun snapshot(): SyncSnapshot? {
        if (!loaded()) return null
        val circle = circleId()?.takeIf { it.isNotBlank() } ?: return null
        val signedIn = session.value as? CloudSession.SignedIn
        val profile = profiles.profile.value
        return SyncSnapshot(
            circleId = circle,
            userId = signedIn?.userId,
            userEmail = signedIn?.email,
            ownerName = profile?.ownerName.orEmpty(),
            ownerAvatarId = profile?.ownerAvatarId.orEmpty(),
            role = profile?.role ?: UserRole.GUARDIAN,
            wearers = profiles.wearers.value.orEmpty(),
            globalContacts = safety.settings.value?.emergencyContacts.orEmpty(),
            pairedDevices = profiles.pairedDevices.value.orEmpty(),
            alerts = safety.history.value.orEmpty(),
            messages = messaging.messages.value.orEmpty(),
            voiceNotes = voice?.notes?.value.orEmpty(),
            zones = zones.zones.value.orEmpty(),
            vitalsSamples = vitals?.samples?.value.orEmpty(),
            evidenceClips = evidence?.clips?.value.orEmpty(),
            smartHomeHooks = smartHome?.hooks?.value.orEmpty(),
            shareAlertPlaces = shareAlertPlaces()
        )
    }
}

/**
 * The receiving end: rows from the server, back into the repositories.
 *
 * Every write goes through an `applyRemote*` method, which takes the same lock
 * as a local write and does **not** fire [SyncHooks]. See
 * `ZoneRepository.applyRemoteZones` for why a pulled row that queued its own
 * push is a loop with no exit.
 *
 * The merge rules themselves are in [MergeRules], where a test can reach them.
 * This class is the plumbing: decode, split by owner, hand over, and route the
 * three tables that belong to the Circle surface rather than to a repository.
 *
 * @param sideTables called with `circle_members`, `invites` and `subscriptions`
 *   rows. Those have no repository - nothing in the offline app has an opinion
 *   about who else is in the circle - so they go to the Circle state instead.
 */
class RepositoryPullSource(
    private val profiles: ProfileRepository,
    private val safety: SafetyRepository,
    private val messaging: MessagingRepository,
    private val zones: ZoneRepository,
    /** Null on a graph built without the Talk thread. See [applyMessages]. */
    private val voice: VoiceNoteRepository?,
    /** Null on a graph built without the smart-home page. */
    private val smartHome: SmartHomeRepository? = null,
    private val outbox: Outbox,
    private val session: StateFlow<CloudSession>,
    private val circleIdProvider: suspend () -> String?,
    private val sideTables: suspend (String, List<JsonObject>) -> Unit
) : PullSource {

    /**
     * Pull order matters exactly once: `wearers` first.
     *
     * Every other circle-scoped row carries a `wearer_id`, and translating one
     * into a local id needs the wearer to exist here already. Pulling zones
     * before the person they belong to would file them under a stranger.
     */
    override val tables: List<String> = listOf(
        CloudTables.WEARERS,
        CloudTables.MEDICAL_IDS,
        CloudTables.EMERGENCY_CONTACTS,
        CloudTables.ALERTS,
        CloudTables.MESSAGES,
        CloudTables.ZONES,
        CloudTables.SMART_HOME_HOOKS,
        CloudTables.CIRCLE_MEMBERS,
        CloudTables.INVITES
    )

    /** See [PullSource.userTables]. Neither of these has a `circle_id`. */
    override val userTables: List<String> = listOf(
        CloudTables.PROFILES,
        CloudTables.SUBSCRIPTIONS
    )

    /**
     * The four small lists the app has to be able to *render*, not merely keep
     * up to date.
     *
     * A cursored pull returns what changed, which on a fresh process against an
     * unchanged Circle is nothing - so the Guardians page sat on "Reading the
     * Circle..." indefinitely while the server held three members the whole
     * time. These four have no repository and no local copy to fall back on
     * (the persisted [com.safeshade.cloud.CloudState] is a cache, not a source),
     * they are a handful of rows each, and they change rarely. Fetching them
     * whole on every pull costs almost nothing and is the only way the first
     * pull after a cold start can answer the question the screen is asking.
     *
     * `profiles` is here for the same reason: it is one row, and it is the
     * account holder's own name and face.
     */
    override val fullPullTables: Set<String> = setOf(
        CloudTables.CIRCLE_MEMBERS,
        CloudTables.INVITES,
        CloudTables.SUBSCRIPTIONS,
        CloudTables.PROFILES
    )

    override suspend fun circleId(): String? = circleIdProvider()

    override suspend fun userId(): String? = (session.value as? CloudSession.SignedIn)?.userId

    override suspend fun onRowsPulled(table: String, rows: List<JsonObject>) {
        if (rows.isEmpty()) return
        if (table == CloudTables.PROFILES) return applyProfile(rows)
        // Every derived server id is namespaced by circle - see CloudIds - so
        // matching a pulled row to a local record needs the circle it came
        // from. Without one there is nothing to match against and the rows are
        // left for the next pull rather than filed under the wrong person.
        val circle = circleIdProvider() ?: return sideTables(table, rows)
        when (table) {
            CloudTables.WEARERS -> applyWearers(rows, circle)
            CloudTables.MEDICAL_IDS -> applyMedicalIds(rows, circle)
            CloudTables.EMERGENCY_CONTACTS -> applyContacts(rows, circle)
            CloudTables.ALERTS -> applyAlerts(rows, circle)
            CloudTables.MESSAGES -> applyMessages(rows, circle)
            CloudTables.ZONES -> applyZones(rows, circle)
            CloudTables.SMART_HOME_HOOKS -> applySmartHomeHooks(rows, circle)
            else -> sideTables(table, rows)
        }
    }

    // ============================================
    // PENDING
    // ============================================

    /**
     * The record ids this phone has queued but not yet sent.
     *
     * These win over anything arriving, because a local edit that has not been
     * pushed is by definition newer than anything the server could have sent.
     * See the [MergeRules] class KDoc for why this is the app's honest version
     * of last-write-wins.
     */
    private fun pending(table: String): Set<String> =
        outbox.entries.value.filter { it.table == table }.map { it.recordId }.toSet()

    /** Server wearer uuid to the local id this phone files things under. */
    private fun wearerIds(circleId: String): Map<String, String> =
        profiles.wearers.value.orEmpty().associate { CloudIds.cloudId(it.id, circleId) to it.id }

    // ============================================
    // TABLES
    // ============================================

    private suspend fun applyWearers(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, WearerRow.serializer())
        val queued = pending(CloudTables.WEARERS)
        profiles.applyRemoteWearers { current ->
            MergeRules.wearers(current, remote, circleId, queued)
        }
    }

    private suspend fun applyMedicalIds(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, MedicalIdRow.serializer())
        if (remote.isEmpty()) return
        val queued = pending(CloudTables.MEDICAL_IDS)
        profiles.applyRemoteWearers { current ->
            current.map { wearer ->
                if (medicalKey(wearer) in queued) return@map wearer
                val row = remote.firstOrNull {
                    it.wearerId == CloudIds.cloudId(wearer.id, circleId)
                }
                    ?: return@map wearer
                wearer.copy(medicalId = MergeRules.medicalId(wearer.medicalId, row))
            }
        }
    }

    /**
     * Contacts, split by who they belong to before they are merged.
     *
     * A row with a null `wearer_id` is the circle-wide SOS list in
     * `SafetySettings`; anything else is that wearer's own supplementary list.
     * Merging them together would move a private contact into the list the SOS
     * button dials, which is a change nobody asked for made in the one place
     * where a surprise is least welcome.
     */
    private suspend fun applyContacts(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, EmergencyContactRow.serializer())
        val global = remote.filter { it.wearerId.isNullOrBlank() }
        val perWearer = remote.filterNot { it.wearerId.isNullOrBlank() }

        if (global.isNotEmpty()) {
            safety.applyRemoteSettings { current: SafetySettings ->
                current.copy(emergencyContacts = MergeRules.contacts(current.emergencyContacts, global))
            }
        }

        if (perWearer.isNotEmpty()) {
            profiles.applyRemoteWearers { current ->
                current.map { wearer ->
                    val mine = perWearer.filter {
                        it.wearerId == CloudIds.cloudId(wearer.id, circleId)
                    }
                    if (mine.isEmpty()) {
                        wearer
                    } else {
                        wearer.copy(contacts = MergeRules.contacts(wearer.contacts, mine))
                    }
                }
            }
        }
    }

    private suspend fun applyAlerts(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, AlertRow.serializer())
        val queued = pending(CloudTables.ALERTS)
        val ids = wearerIds(circleId)
        safety.applyRemoteHistory { current ->
            MergeRules.alerts(current, remote, circleId, queued, ids)
        }
    }

    /**
     * One table, two threads.
     *
     * `messages` carries both the typed quick messages and the push-to-talk
     * voice notes, because they are one conversation - so the rows are split by
     * `kind` here and handed to the repository that owns each. A row with no
     * `kind` is text: every row written before migration 0006 was, and the
     * column defaults to `text` for exactly that reason.
     *
     * Routing matters more than it looks. `MergeRules.messages` drops a row
     * with blank text, so before this split a voice note pulled from another
     * phone was decoded, examined, and silently discarded - the recording
     * existed on the server and simply never appeared on the second phone,
     * with nothing anywhere saying why.
     */
    private suspend fun applyMessages(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, MessageRow.serializer())
        val queued = pending(CloudTables.MESSAGES)
        val ids = wearerIds(circleId)

        val (spoken, typed) = remote.partition { it.kind == KIND_VOICE }

        if (typed.isNotEmpty()) {
            messaging.applyRemoteMessages { current ->
                MergeRules.messages(current, typed, circleId, queued, ids)
            }
        }

        if (spoken.isNotEmpty()) {
            voice?.applyRemote { current ->
                MergeRules.voiceNotes(current, spoken, circleId, queued, ids)
            }
        }
    }

    private suspend fun applyZones(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, ZoneRow.serializer())
        val queued = pending(CloudTables.ZONES)
        val ids = wearerIds(circleId)
        zones.applyRemoteZones { current ->
            MergeRules.zones(current, remote, circleId, queued, ids)
        }
    }

    /**
     * The smart-home hooks, from whichever guardian set them up.
     *
     * Dropped entirely when there is no repository to write into, rather than
     * routed to [sideTables]: a hook carries a webhook URL and a signing secret,
     * and the side-table path exists for the Circle surface's own rows. There is
     * nothing sensible for it to do with these and no reason to hand them over.
     */
    private suspend fun applySmartHomeHooks(rows: List<JsonObject>, circleId: String) {
        val repo = smartHome ?: return
        val remote = decode(rows, SmartHomeHookRow.serializer())
        if (remote.isEmpty()) return
        val queued = pending(CloudTables.SMART_HOME_HOOKS)
        repo.applyRemoteHooks { current ->
            MergeRules.smartHomeHooks(current, remote, circleId, queued)
        }
    }

    /**
     * The account holder's own profile row.
     *
     * Only fills gaps. The name and face on this phone were typed by the person
     * holding it; a row written by the same account on a tablet last month must
     * not rename them mid-session. What it does do is give a fresh install its
     * owner back after a sign-in, which is the case that matters.
     */
    private suspend fun applyProfile(rows: List<JsonObject>) {
        val row = decode(rows, ProfileRow.serializer()).firstOrNull() ?: return
        if (!row.deletedAt.isNullOrBlank()) return
        profiles.applyRemoteProfile { current ->
            current.copy(
                ownerName = current.ownerName.ifBlank { row.displayName.orEmpty() },
                ownerAvatarId = current.ownerAvatarId.ifBlank { row.avatarId.orEmpty() }
            )
        }
    }

    /** The outbox key a medical record would be queued under. See `SyncKeys`. */
    private fun medicalKey(wearer: Wearer): String = SyncKeys.medical(wearer.id)
}
