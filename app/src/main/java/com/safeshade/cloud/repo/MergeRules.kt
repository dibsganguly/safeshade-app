package com.safeshade.cloud.repo

import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.MedicalIdRow
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.dto.SmartHomeHookRow
import com.safeshade.cloud.dto.WearerRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.dto.isoToEpochMillis
import com.safeshade.cloud.dto.toDomain
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.MessageChannel
import com.safeshade.data.PersonaMode
import com.safeshade.data.QuickMessage
import com.safeshade.data.SmartHomeHook
import com.safeshade.data.TripOutcome
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import com.safeshade.data.Wearer
import com.safeshade.platform.PhoneNumbers

/**
 * What happens when a row arrives from another phone and this phone already has
 * an opinion about it.
 *
 * Pure functions over immutable lists, for the same reason `OutboxPolicy` is:
 * this is the part of sync that can be wrong for weeks without anybody
 * noticing, and it is the part a unit test can reach.
 *
 * ### The base rule, stated honestly
 *
 * The brief says last-write-wins on `updated_at`. The local models do not carry
 * an `updated_at` - `GeofenceZone`, `QuickMessage` and the rest are the shapes
 * the app has always had - so a literal field comparison is not available, and
 * inventing one would mean writing a timestamp the phone controls and then
 * trusting it, which is the trap `0001_init.sql` rule 3 exists to avoid.
 *
 * What is available is better. The pull is cursored per table on the server's
 * own `updated_at`, so **every row that reaches these functions is by
 * construction newer than the last thing this phone pulled**. The only thing
 * that can be newer still is an edit made here that has not been pushed yet -
 * and the outbox knows exactly which records those are. So:
 *
 * > Remote wins, unless the record has a pending outbox entry, in which case
 * > the local copy wins and the queued push will carry it to the server.
 *
 * That is last-write-wins with the clock the server keeps, and it needs no
 * clock the phone keeps.
 *
 * ### Two exceptions, and why each one is not negotiable
 *
 *  * **[alerts]: an outcome never regresses to `PENDING`.** A guardian
 *    dismisses a fall alert on their phone; the wearer's phone, which still has
 *    the row as pending, pushes it a moment later. Plain LWW would reopen the
 *    alert - the dialog comes back, the countdown restarts, and on a bad day
 *    the escalation runs for an incident somebody already looked at and closed.
 *    A resolution is a fact about the world; "not yet resolved" is only the
 *    absence of one, and the absence of a fact must never overwrite the fact.
 *  * **[contacts]: union by phone number.** The emergency contact list is the
 *    thing the SOS path dials. If two guardians each add one contact while
 *    offline, LWW keeps one list and silently discards the other person - and
 *    the discarded one is discovered when nobody answers. Union is the only
 *    merge where the failure mode is "one extra number was tried".
 *
 * ### Deletions
 *
 * A row carrying `deleted_at` removes the local record. That is the whole
 * reason the schema soft-deletes (rule 4): a row that simply stopped existing
 * produces no change to pull, so a safe zone deleted on one phone would live
 * forever on every other one.
 */
internal object MergeRules {

    /** True when the server has tombstoned this row. */
    private fun deleted(at: String?): Boolean = !at.isNullOrBlank()

    /**
     * The app's one definition of a phone number's identity.
     *
     * `PhoneNumbers.digitsOf` drops a +91, a bare 91 and a trunk 0, so
     * "9876543210" and "+91 98765 43210" are the same person. A plain digit
     * filter here would make them two, and the union rule would quietly grow a
     * duplicate of every contact whose two phones spell it differently.
     */
    private fun digits(raw: String): String = PhoneNumbers.digitsOf(raw)

    /**
     * A row's `wearer_id` translated back into the id this phone knows.
     *
     * Server ids are uuids derived from local ids (see [CloudIds]); the local
     * id of `"wearer-primary"` is not a uuid at all. Without the translation a
     * pulled zone would point at a person nobody on this phone can look up, and
     * every per-wearer filter in the UI would quietly show nothing.
     *
     * A server id with no local counterpart is kept as-is: it names a wearer
     * this phone has not pulled yet, and the next `wearers` pull creates them
     * under exactly that id.
     */
    private fun localWearer(serverId: String?, wearerIds: Map<String, String>): String? =
        serverId?.let { wearerIds[it] ?: it }

    // ============================================
    // ALERTS
    // ============================================

    /**
     * @param pending local record ids with an unsent outbox entry. Those keep
     *   their local shape entirely; see the class KDoc.
     */
    fun alerts(
        local: List<FallAlertEvent>,
        remote: List<AlertRow>,
        circleId: String,
        pending: Set<String> = emptySet(),
        wearerIds: Map<String, String> = emptyMap()
    ): List<FallAlertEvent> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            if (existing != null && existing.id in pending) continue

            val incoming = row.toDomain()
            if (existing == null) {
                result += incoming.copy(wearerId = localWearer(row.wearerId, wearerIds))
                continue
            }

            result[result.indexOfFirst { it.id == existing.id }] = existing.copy(
                timestamp = incoming.timestamp.takeIf { it > 0L } ?: existing.timestamp,
                kind = incoming.kind,
                // The exception. A remote PENDING is the absence of an outcome
                // and never overwrites one this phone already has.
                outcome = if (incoming.outcome == TripOutcome.PENDING) {
                    existing.outcome
                } else {
                    incoming.outcome
                },
                // Same shape of fact: somebody was contacted, or nothing is
                // known. "Nothing is known" does not un-contact them.
                wasEmergencyContacted =
                    existing.wasEmergencyContacted || incoming.wasEmergencyContacted,
                location = incoming.location ?: existing.location,
                note = incoming.note ?: existing.note
            )
        }

        return result.filterNot { it.id in removed }
    }

    // ============================================
    // EMERGENCY CONTACTS
    // ============================================

    /**
     * Union by phone number, with the remote name winning on a match.
     *
     * The number is the identity - it is what gets dialled, and a person whose
     * name is spelled differently on two phones is one person. A contact
     * tombstoned on the server is removed here, so "remove" still works; it is
     * a deliberate act, unlike "my list did not happen to include them".
     *
     * @param remote rows for one list only. The caller separates the global
     *   `SafetySettings.emergencyContacts` (rows with a null `wearer_id`) from
     *   each wearer's own; mixing them here would move a wearer's private
     *   contact into the circle-wide SOS list.
     */
    fun contacts(
        local: List<EmergencyContact>,
        remote: List<EmergencyContactRow>
    ): List<EmergencyContact> {
        val result = local.toMutableList()

        for (row in remote) {
            val phone = row.phone.orEmpty()
            val key = digits(phone)
            if (key.isBlank()) continue
            val index = result.indexOfFirst { digits(it.phone) == key }

            if (deleted(row.deletedAt)) {
                if (index >= 0) result.removeAt(index)
                continue
            }

            val incoming = EmergencyContact(
                name = row.name.orEmpty(),
                phone = phone,
                // priority 0 is the one the SOS path dials first; see
                // PayloadResolver.contact.
                isPrimary = (row.priority ?: 1) <= 0,
                relationship = row.relationship.orEmpty(),
                // The face lives on this phone only; the row has no column
                // for it, so a pull must not wipe it.
                avatarId = if (index >= 0) result[index].avatarId else ""
            )

            if (index < 0) {
                result += incoming
            } else {
                val existing = result[index]
                result[index] = existing.copy(
                    name = incoming.name.ifBlank { existing.name },
                    relationship = incoming.relationship.ifBlank { existing.relationship },
                    isPrimary = incoming.isPrimary
                )
            }
        }

        // At most one primary. Two phones can each have marked a different
        // person, and the SOS path reads `primaryContact` as a single answer.
        val firstPrimary = result.indexOfFirst { it.isPrimary }
        return result.mapIndexed { i, c ->
            if (c.isPrimary && i != firstPrimary) c.copy(isPrimary = false) else c
        }
    }

    // ============================================
    // ZONES
    // ============================================

    fun zones(
        local: List<GeofenceZone>,
        remote: List<ZoneRow>,
        circleId: String,
        pending: Set<String> = emptySet(),
        wearerIds: Map<String, String> = emptyMap()
    ): List<GeofenceZone> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            if (existing != null && existing.id in pending) continue

            val incoming = row.toDomain()
            val owner = localWearer(row.wearerId, wearerIds)
            if (existing == null) {
                result += incoming.copy(wearerId = owner)
            } else {
                result[result.indexOfFirst { it.id == existing.id }] =
                    incoming.copy(id = existing.id, wearerId = existing.wearerId ?: owner)
            }
        }

        return result.filterNot { it.id in removed }
    }

    // ============================================
    // SMART HOME HOOKS
    // ============================================

    /**
     * Hooks, merged the way zones are, with one extra rule.
     *
     * The extra rule is about the last-fired stamp: `lastStatusCode` has no
     * column, so an arriving row would blank it. It is kept from the local copy
     * instead. It is this phone's note about its own last attempt, and losing
     * it on every pull would make a hook's row flicker between "403" and
     * nothing depending on when the last sync happened.
     *
     * A hook with no endpoint is dropped rather than kept: it names nowhere to
     * post, so it can never fire, and a row in the list that cannot work is
     * worse than no row - the user believes the light will come on.
     */
    fun smartHomeHooks(
        local: List<SmartHomeHook>,
        remote: List<SmartHomeHookRow>,
        circleId: String,
        pending: Set<String> = emptySet()
    ): List<SmartHomeHook> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            // A local edit that has not been pushed is newer than anything the
            // server could be holding. See the class KDoc.
            if (existing != null && existing.id in pending) continue

            val endpoint = row.endpointUrl?.takeIf { it.isNotBlank() }
            if (endpoint == null && existing == null) continue

            val incoming = SmartHomeHook(
                id = existing?.id ?: serverId,
                name = row.name.orEmpty().ifBlank { existing?.name ?: "" },
                trigger = row.trigger.orEmpty().ifBlank { existing?.trigger ?: "" },
                provider = row.provider.orEmpty().ifBlank { existing?.provider ?: "" },
                endpointUrl = endpoint ?: existing?.endpointUrl.orEmpty(),
                secret = row.secret?.takeIf { it.isNotBlank() } ?: existing?.secret,
                enabled = row.enabled ?: existing?.enabled ?: true,
                lastFiredAt = com.safeshade.cloud.parseServerInstant(row.lastFiredAt)?.toEpochMilli()
                    ?: existing?.lastFiredAt,
                lastError = row.lastError?.takeIf { it.isNotBlank() },
                // No column. This phone's own note about its own last attempt.
                lastStatusCode = existing?.lastStatusCode
            )

            if (existing == null) {
                result += incoming
            } else {
                result[result.indexOfFirst { it.id == existing.id }] = incoming
            }
        }

        return result.filterNot { it.id in removed }
    }

    // ============================================
    // MESSAGES
    // ============================================

    /**
     * Messages are append-mostly, so this is close to a union.
     *
     * `replied`/`replyText` are local bookkeeping about a thread on this phone
     * and have no column, so an incoming row never clears them.
     */
    fun messages(
        local: List<QuickMessage>,
        remote: List<MessageRow>,
        circleId: String,
        pending: Set<String> = emptySet(),
        wearerIds: Map<String, String> = emptyMap()
    ): List<QuickMessage> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            if (existing != null && existing.id in pending) continue

            val text = row.text.orEmpty()
            if (text.isBlank()) continue
            val incoming = QuickMessage(
                id = existing?.id ?: serverId,
                text = text,
                fromGuardian = row.fromGuardian ?: true,
                timestamp = row.sentAt.isoToEpochMillis(
                    fallback = existing?.timestamp ?: System.currentTimeMillis()
                ),
                replied = existing?.replied ?: false,
                replyText = existing?.replyText,
                channel = channelOf(row.channel, existing?.channel),
                wearerId = localWearer(row.wearerId, wearerIds) ?: existing?.wearerId
            )

            if (existing == null) {
                result += incoming
            } else {
                result[result.indexOfFirst { it.id == existing.id }] = incoming
            }
        }

        return result.filterNot { it.id in removed }.sortedBy { it.timestamp }
    }

    /**
     * `messages.channel` allows `CLOUD`, which [MessageChannel] does not - the
     * app has two transports and the schema anticipates a third. An unknown
     * value keeps whatever the local row said, or falls back to BLE, rather
     * than throwing inside a pull.
     */
    private fun channelOf(raw: String?, fallback: MessageChannel?): MessageChannel =
        runCatching { MessageChannel.valueOf(raw.orEmpty()) }
            .getOrNull() ?: fallback ?: MessageChannel.BLE

    // ============================================
    // WEARERS
    // ============================================

    /**
     * Names, faces and modes come from the server; everything device-local
     * stays.
     *
     * `deviceAddresses` is the binding between a person and a wearable that is
     * paired to *this* phone, `medicalId` arrives on its own table, and
     * `contacts` merges by [contacts] - so none of the three is taken from a
     * `wearers` row, which carries none of them anyway.
     *
     * `isSelf` is not taken either: it is a statement about whose phone this
     * is, and `applyRoleFork` owns it.
     */
    fun wearers(
        local: List<Wearer>,
        remote: List<WearerRow>,
        circleId: String,
        pending: Set<String> = emptySet()
    ): List<Wearer> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            if (existing != null && existing.id in pending) continue

            val name = row.name.orEmpty()
            val mode = PersonaMode.fromWire(row.personaMode.orEmpty())

            if (existing == null) {
                result += Wearer(
                    id = serverId,
                    name = name,
                    avatarId = row.avatarId.orEmpty(),
                    activeMode = mode,
                    iconType = mode.matchingDeviceIcon
                )
            } else {
                result[result.indexOfFirst { it.id == existing.id }] = existing.copy(
                    name = name.ifBlank { existing.name },
                    avatarId = row.avatarId.orEmpty().ifBlank { existing.avatarId },
                    activeMode = mode
                )
            }
        }

        // The list never empties from a pull: an empty "People I look after" is
        // an app with nothing to show and no way back, and wearer #1 is
        // synthesised from the profile when the key is absent anyway.
        val kept = result.filterNot { it.id in removed }
        return kept.ifEmpty { local }
    }

    /**
     * Voice notes, from `messages` rows with `kind = 'voice'`.
     *
     * ### What is taken from the row, and what never is
     *
     * The row is the authority on the things a recording *is* - who spoke,
     * which way, when, how long, and what the waveform looks like. It is never
     * the authority on two things that are facts about **this** phone:
     *
     *  * [VoiceNote.file] and [VoiceNote.uploadState] on a note this phone
     *    recorded. A row echoing back this phone's own push must not reset a
     *    note to "not downloaded"; the `.m4a` is right here.
     *  * [VoiceNote.listened]. Whether somebody has played it is a fact about
     *    one reader, and syncing it would mark a note heard for the whole
     *    Circle because one guardian opened the thread.
     *
     * A note that is genuinely new here arrives with an empty [VoiceNote.file]
     * and [VoiceUpload.Uploaded] pointing at the storage path - which is the
     * honest pair: the audio is on the server and not yet on this phone.
     * `VoiceCloud.downloadVoice` is what fills the file in, when somebody asks
     * to hear it.
     *
     * [VoiceNote.authorName] is blank on an incoming note when the row does not
     * say. `messages` carries `author_id`, a uuid, and one Circle member cannot
     * read another's profile - so a name that this phone has not been told is
     * left blank rather than guessed at. A screen resolves it from
     * `CloudState.members` or shows nothing.
     *
     * @param pending local ids with an unsent write queued. They win, for the
     *   reason given in this class's KDoc.
     */
    fun voiceNotes(
        local: List<VoiceNote>,
        remote: List<MessageRow>,
        circleId: String,
        pending: Set<String> = emptySet(),
        wearerIds: Map<String, String> = emptyMap()
    ): List<VoiceNote> {
        val byServerId = local.associateBy { CloudIds.cloudId(it.id, circleId) }
        val result = local.toMutableList()
        val removed = mutableSetOf<String>()

        for (row in remote) {
            val serverId = row.id ?: continue
            val existing = byServerId[serverId]

            if (deleted(row.deletedAt)) {
                if (existing != null) removed += existing.id
                continue
            }
            if (existing != null && existing.id in pending) continue

            // A voice row with no object behind it is not playable and not
            // fetchable; there is nothing this phone could do with it.
            val path = row.audioPath?.takeIf { it.isNotBlank() } ?: continue

            val incoming = VoiceNote(
                id = existing?.id ?: serverId,
                wearerId = localWearer(row.wearerId, wearerIds) ?: existing?.wearerId,
                fromGuardian = row.fromGuardian ?: existing?.fromGuardian ?: true,
                authorName = existing?.authorName.orEmpty(),
                // Empty until downloaded, and kept as it stands for a note this
                // phone already holds the audio for.
                file = existing?.file.orEmpty(),
                durationMs = row.durationMs ?: existing?.durationMs ?: 0,
                waveform = row.waveform ?: existing?.waveform.orEmpty(),
                createdAt = row.sentAt.isoToEpochMillis(
                    fallback = existing?.createdAt ?: System.currentTimeMillis()
                ),
                // The audio is in the bucket - the row exists, and this app
                // never writes the row before the object. For a note this phone
                // recorded, whatever state it already had is kept, so a local
                // Failed is not quietly promoted to Uploaded by its own echo.
                uploadState = existing?.uploadState ?: VoiceUpload.Uploaded(path),
                listened = existing?.listened ?: false
            )

            if (existing == null) {
                result += incoming
            } else {
                result[result.indexOfFirst { it.id == existing.id }] = incoming
            }
        }

        return result.filterNot { it.id in removed }.sortedBy { it.createdAt }
    }

    /**
     * A `medical_ids` row onto the wearer it belongs to.
     *
     * Blank incoming fields do not erase filled local ones. A phone that has
     * never opened the medical ID screen pushes an empty record; without this,
     * one sync from it would wipe the allergies list of somebody it is trying
     * to protect.
     */
    fun medicalId(local: MedicalId, row: MedicalIdRow): MedicalId {
        if (deleted(row.deletedAt)) return local
        return MedicalId(
            bloodType = row.bloodType.orEmpty().ifBlank { local.bloodType },
            emergencyContact = row.emergencyContact.orEmpty().ifBlank { local.emergencyContact },
            contactName = row.contactName.orEmpty().ifBlank { local.contactName },
            allergies = row.allergies.orEmpty().ifBlank { local.allergies },
            age = row.age ?: local.age,
            conditions = row.conditions.orEmpty().ifBlank { local.conditions },
            medications = row.medications.orEmpty().ifBlank { local.medications },
            secondaryContactName = row.secondaryContactName.orEmpty()
                .ifBlank { local.secondaryContactName },
            secondaryContact = row.secondaryContact.orEmpty().ifBlank { local.secondaryContact },
            organDonor = row.organDonor ?: local.organDonor,
            notes = row.notes.orEmpty().ifBlank { local.notes }
        )
    }
}
