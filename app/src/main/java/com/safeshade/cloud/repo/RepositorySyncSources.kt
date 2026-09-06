package com.safeshade.cloud.repo

import com.safeshade.cloud.CloudSession
import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.MedicalIdRow
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.dto.ProfileRow
import com.safeshade.cloud.dto.WearerRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.PayloadSource
import com.safeshade.cloud.sync.PullSource
import com.safeshade.data.SafetySettings
import com.safeshade.data.UserRole
import com.safeshade.data.Wearer
import com.safeshade.repo.MessagingRepository
import com.safeshade.repo.ProfileRepository
import com.safeshade.repo.SafetyRepository
import com.safeshade.repo.SyncKeys
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
    private val circleId: suspend () -> String?
) : PayloadSource {

    override suspend fun payloadFor(
        table: String,
        recordId: String,
        op: OutboxOp
    ): JsonElement? {
        val snapshot = snapshot() ?: return null
        return PayloadResolver.resolve(table, recordId, op, snapshot)
    }

    /** Exposed for the tests; there is nothing here a test cannot construct. */
    suspend fun snapshot(): SyncSnapshot? {
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
            zones = zones.zones.value.orEmpty()
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
        CloudTables.CIRCLE_MEMBERS,
        CloudTables.INVITES
    )

    /** See [PullSource.userTables]. Neither of these has a `circle_id`. */
    override val userTables: List<String> = listOf(
        CloudTables.PROFILES,
        CloudTables.SUBSCRIPTIONS
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

    private suspend fun applyMessages(rows: List<JsonObject>, circleId: String) {
        val remote = decode(rows, MessageRow.serializer())
        val queued = pending(CloudTables.MESSAGES)
        val ids = wearerIds(circleId)
        messaging.applyRemoteMessages { current ->
            MergeRules.messages(current, remote, circleId, queued, ids)
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
