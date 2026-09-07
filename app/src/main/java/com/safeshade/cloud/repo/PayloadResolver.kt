package com.safeshade.cloud.repo

import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.DeviceRow
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.KIND_TEXT
import com.safeshade.cloud.dto.KIND_VOICE
import com.safeshade.cloud.dto.MedicalIdRow
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.dto.ProfileRow
import com.safeshade.cloud.dto.WearerRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.dto.toIsoOrNull
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.data.EmergencyContact
import com.safeshade.data.UserRole
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import com.safeshade.platform.PhoneNumbers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/**
 * `(table, recordId)` to the JSON row that goes on the wire.
 *
 * Pure, and that is the point - see [SyncSnapshot].
 *
 * ### Every row of one table carries exactly the same keys
 *
 * PostgREST rejects a bulk insert whose objects have different key sets, with
 * one 400 for the whole array ("All object keys must match"). The outbox drains
 * a table as one request, so a batch of three zones where one happens to have a
 * null `wearer_id` and two do not would fail *as a batch* - and every entry in
 * it would be marked failed, five times, and then reported to the user as
 * having not reached SafeShade Cloud.
 *
 * So [encode] serialises with `explicitNulls = true`: an absent value is
 * present as JSON `null` rather than omitted. That is the opposite of
 * `SupabaseCloudClient`'s own serializer, which omits nulls so that a partial
 * row cannot erase a column it knows nothing about - and it is safe here for
 * the reason that rule was written: these rows are not partial. The phone is
 * the author of every column it sends.
 *
 * ### Some keys are dropped from every row
 *
 * `created_at` and `updated_at` are the server's. `updated_at` in particular is
 * the entire pull protocol (rule 3 of `0001_init.sql`: a client-set timestamp
 * is a client-set cursor, and a phone with a fast clock would skip rows
 * forever), and both columns are `not null default now()` - an explicit null on
 * a defaulted column is a constraint violation, because a default applies to a
 * key that is *absent*, not to a key that is present and null.
 *
 * `alerts.acknowledged_by` and `acknowledged_at` are dropped for a different
 * reason: they are written by whichever guardian acknowledged the alert, from
 * their phone. This app has no local field for them, so sending null would
 * erase another person's acknowledgement every time this phone touched the row.
 */
internal object PayloadResolver {

    private val json = Json {
        // See the class KDoc. This is deliberately not the client's serializer.
        explicitNulls = true
        encodeDefaults = true
    }

    /** Columns no client row may carry, whatever the table. */
    private val ALWAYS_DROPPED = setOf("created_at", "updated_at")

    private val DROPPED_BY_TABLE: Map<String, Set<String>> = mapOf(
        CloudTables.ALERTS to setOf("acknowledged_by", "acknowledged_at")
    )

    /**
     * The row for one outbox entry, or null to skip it.
     *
     * **Null is not a failure.** `SyncEngine` skips a null without counting an
     * attempt, which is what stops a record the app no longer holds - a zone
     * the user deleted, a wearer removed on another phone - from marching to
     * five attempts and painting the UI with a failure that never happened. See
     * `PayloadSource.payloadFor`.
     *
     * A [OutboxOp.DELETE] resolves to a *tombstone*: the same key set as a live
     * row, with only `id` and `circle_id` filled in. `SyncEngine` stamps
     * `deleted_at` onto it. The rest of the columns go to null, which is
     * correct for a row nobody should be reading any more, and it is also the
     * only thing available - the record is gone from the phone, which is why it
     * is being deleted.
     */
    fun resolve(
        table: String,
        recordId: String,
        op: OutboxOp,
        snapshot: SyncSnapshot
    ): JsonObject? {
        if (snapshot.circleId.isBlank()) return null
        if (op == OutboxOp.DELETE) return tombstone(table, recordId, snapshot)
        return when (table) {
            CloudTables.PROFILES -> profile(recordId, snapshot)
            CloudTables.WEARERS -> wearer(recordId, snapshot)
            CloudTables.MEDICAL_IDS -> medicalId(recordId, snapshot)
            CloudTables.EMERGENCY_CONTACTS -> contact(recordId, snapshot)
            CloudTables.DEVICES -> device(recordId, snapshot)
            CloudTables.ALERTS -> alert(recordId, snapshot)
            CloudTables.MESSAGES -> message(recordId, snapshot)
            CloudTables.ZONES -> zone(recordId, snapshot)
            else -> null
        }
    }

    // ============================================
    // ROWS
    // ============================================

    /**
     * The account holder's own `profiles` row.
     *
     * `role` is lower-cased: the column's CHECK constraint is
     * `('guardian', 'companion')` and `UserRole.GUARDIAN.name` is not that. A
     * mismatch here is a non-retryable rejection of every profile push, so it
     * is worth the one `when`.
     */
    private fun profile(recordId: String, s: SyncSnapshot): JsonObject? {
        val id = s.userId?.takeIf { it.isNotBlank() && it == recordId } ?: return null
        return encode(
            ProfileRow.serializer(),
            ProfileRow(
                id = id,
                email = s.userEmail,
                displayName = s.ownerName.ifBlank { null },
                avatarId = shareableAvatar(s.ownerAvatarId),
                role = when (s.role) {
                    UserRole.GUARDIAN -> "guardian"
                    UserRole.COMPANION -> "companion"
                }
            ),
            CloudTables.PROFILES
        )
    }

    private fun wearer(recordId: String, s: SyncSnapshot): JsonObject? {
        val wearer = s.wearers.firstOrNull { it.id == recordId } ?: return null
        return encode(
            WearerRow.serializer(),
            WearerRow(
                id = CloudIds.cloudId(wearer.id, s.circleId),
                circleId = s.circleId,
                name = wearer.name.ifBlank { null },
                // `user_id` links a wearer row to an account. Only the person
                // holding this phone has one, and only when they are the wearer.
                userId = if (wearer.isSelf) s.userId else null,
                personaMode = wearer.activeMode.wireName,
                avatarId = shareableAvatar(wearer.avatarId)
            ),
            CloudTables.WEARERS
        )
    }

    /** Record id is [CloudIds.localKey] "medical:" plus the wearer local id. */
    private fun medicalId(recordId: String, s: SyncSnapshot): JsonObject? {
        val wearerLocalId = CloudIds.ownerOf(recordId) ?: return null
        val wearer = s.wearers.firstOrNull { it.id == wearerLocalId } ?: return null
        val m = wearer.medicalId
        return encode(
            MedicalIdRow.serializer(),
            MedicalIdRow(
                id = CloudIds.medicalId(wearer.id, s.circleId),
                circleId = s.circleId,
                wearerId = CloudIds.cloudId(wearer.id, s.circleId),
                bloodType = m.bloodType.ifBlank { null },
                emergencyContact = m.emergencyContact.ifBlank { null },
                contactName = m.contactName.ifBlank { null },
                allergies = m.allergies.ifBlank { null },
                age = m.age.takeIf { it > 0 },
                conditions = m.conditions.ifBlank { null },
                medications = m.medications.ifBlank { null },
                secondaryContactName = m.secondaryContactName.ifBlank { null },
                secondaryContact = m.secondaryContact.ifBlank { null },
                organDonor = m.organDonor,
                notes = m.notes.ifBlank { null }
            ),
            CloudTables.MEDICAL_IDS
        )
    }

    /**
     * Record id is [CloudIds.localKey] "contact:" plus an owner plus the phone digits,
     * where `owner` is a wearer's local id or [CloudIds.GLOBAL].
     */
    private fun contact(recordId: String, s: SyncSnapshot): JsonObject? {
        val ownerLocalId = CloudIds.ownerOf(recordId)
        val digits = recordId.substringAfterLast(':')
        val found: EmergencyContact? = if (ownerLocalId == null) {
            s.globalContacts.firstOrNull { digitsOf(it.phone) == digits }
        } else {
            s.wearers.firstOrNull { it.id == ownerLocalId }
                ?.contacts?.firstOrNull { digitsOf(it.phone) == digits }
        }
        val c = found ?: return null
        return encode(
            EmergencyContactRow.serializer(),
            EmergencyContactRow(
                id = CloudIds.contactId(ownerLocalId, c.phone, s.circleId),
                circleId = s.circleId,
                wearerId = ownerLocalId?.let { CloudIds.cloudId(it, s.circleId) },
                name = c.name.ifBlank { null },
                phone = c.phone.ifBlank { null },
                relationship = c.relationship.ifBlank { null },
                // The contact the SOS path dials first sorts first, and a
                // priority is the only field the schema has to say so with.
                priority = if (c.isPrimary) 0 else 1,
                notifyBySms = true,
                notifyByEmail = false
            ),
            CloudTables.EMERGENCY_CONTACTS
        )
    }

    /** Record id is [CloudIds.localKey] "device:" plus the BLE address. */
    private fun device(recordId: String, s: SyncSnapshot): JsonObject? {
        val address = recordId.substringAfter(':', "").takeIf { it.isNotBlank() } ?: return null
        val paired = s.pairedDevices.firstOrNull { it.address.equals(address, ignoreCase = true) }
            ?: return null
        return encode(
            DeviceRow.serializer(),
            DeviceRow(
                id = CloudIds.deviceId(paired.address, s.circleId),
                circleId = s.circleId,
                // Binding is by BLE address and never by DeviceSettings.id -
                // handoff7 section 6 item 2. The device knows its address;
                // nothing else about it survives a re-pair.
                wearerId = s.wearerForAddress(paired.address)?.let { CloudIds.cloudId(it.id, s.circleId) },
                name = paired.name.ifBlank { null },
                bleAddress = paired.address,
                lastSeenAt = paired.lastConnected.toIsoOrNull()
            ),
            CloudTables.DEVICES
        )
    }

    /**
     * A trip-log entry, with or without the place it happened.
     *
     * ### The place is withheld, not the row
     *
     * [SyncSnapshot.shareAlertPlaces] is the account holder's switch. With it
     * off, `lat`, `lon` and `location_label` go as **null** - not as absent
     * keys, which would break the identical-key-set rule this class exists to
     * enforce - and everything else about the alert still syncs. That is the
     * whole design: a guardian in the Circle must still see that a fall
     * happened, when it happened and how it ended, because a trip log that
     * silently loses entries is worse than one that says "somewhere".
     *
     * ### Two honest limits, stated here so nobody rediscovers them
     *
     *  1. **This is a client-side rule.** It stops this phone sending the
     *     place; it removes nothing already sent. Rows pushed while the switch
     *     was on keep their place, in the row and in the heat-map view built
     *     over it. The switch is forward-looking, and any copy describing it
     *     has to say so.
     *  2. **`lat` and `lon` are null on every alert this app pushes anyway.**
     *     `FallAlertEvent` in `data/Models.kt` carries a human-readable
     *     location string and no coordinates at all, so today the switch has an
     *     effect on `location_label` alone - which also means the community
     *     heat map is fed nothing by this app yet. Both coordinates are
     *     stripped regardless, so the rule is already correct on the day the
     *     model gains them rather than being a bug waiting for that commit.
     */
    private fun alert(recordId: String, s: SyncSnapshot): JsonObject? {
        val event = s.alerts.firstOrNull { it.id == recordId } ?: return null
        val place = s.shareAlertPlaces
        return encode(
            AlertRow.serializer(),
            AlertRow(
                id = CloudIds.cloudId(event.id, s.circleId),
                circleId = s.circleId,
                wearerId = event.wearerId?.let { CloudIds.cloudId(it, s.circleId) },
                kind = event.kind.name,
                outcome = event.outcome.name,
                occurredAt = event.timestamp.toIsoOrNull(),
                lat = null,
                lon = null,
                locationLabel = if (place) event.location else null,
                note = event.note,
                wasEmergencyContacted = event.wasEmergencyContacted
            ),
            CloudTables.ALERTS
        )
    }

    /**
     * A row on the Circle thread - typed, or spoken.
     *
     * One record id, two possible sources: `MessagingRepository`'s quick
     * messages and `VoiceNoteRepository`'s push-to-talk notes both queue
     * against `messages`, because they are one conversation. Text is looked for
     * first only because it is the commoner case; both id spaces are UUIDs and
     * do not collide.
     *
     * Before this looked in both, a voice note's id resolved to null here - and
     * a null is a *skip*, so three drains later `OutboxPolicy.isAbandoned`
     * reported the note as "There was nothing left on this phone to send for
     * this" about a recording that had been sitting on the phone the whole
     * time.
     */
    private fun message(recordId: String, s: SyncSnapshot): JsonObject? {
        val m = s.messages.firstOrNull { it.id == recordId }
            ?: return voiceNote(recordId, s)
        return encode(
            MessageRow.serializer(),
            MessageRow(
                id = CloudIds.cloudId(m.id, s.circleId),
                circleId = s.circleId,
                wearerId = m.wearerId?.let { CloudIds.cloudId(it, s.circleId) },
                authorId = s.userId,
                text = m.text,
                fromGuardian = m.fromGuardian,
                channel = m.channel.name,
                sentAt = m.timestamp.toIsoOrNull(),
                kind = KIND_TEXT
            ),
            CloudTables.MESSAGES
        )
    }

    /**
     * A voice note, **only once its audio is in the bucket**.
     *
     * This is the invariant that keeps the outcome rule true for the Talk
     * thread. The row is a pointer to a storage object, so a row that exists
     * before the object does is a note every other phone in the Circle can see,
     * tap, and fail to play. Anything short of [VoiceUpload.Uploaded] therefore
     * resolves to null and no row is sent.
     *
     * Getting a note *to* Uploaded is the I/O layer's job and not this one's -
     * see `VoiceCloud` and `RepositoryPayloadSource.payloadFor`. This function
     * stays pure, and by construction cannot emit a row for audio that is not
     * there.
     *
     * `text` is null rather than a caption. A spoken note has no words on this
     * phone, and writing "Voice note" into the column would put a string on the
     * server that a real transcription would later have to be distinguished
     * from.
     */
    private fun voiceNote(recordId: String, s: SyncSnapshot): JsonObject? {
        val note: VoiceNote = s.voiceNotes.firstOrNull { it.id == recordId } ?: return null
        val uploaded = note.uploadState as? VoiceUpload.Uploaded ?: return null
        return encode(
            MessageRow.serializer(),
            MessageRow(
                id = CloudIds.cloudId(note.id, s.circleId),
                circleId = s.circleId,
                wearerId = note.wearerId?.let { CloudIds.cloudId(it, s.circleId) },
                authorId = s.userId,
                text = null,
                fromGuardian = note.fromGuardian,
                // CLOUD, not BLE. `channel` records how the thing actually
                // travelled, and this one genuinely went through the server.
                // The column's CHECK allows it; `MessageChannel` does not,
                // which is why voice rows are routed away from
                // `MergeRules.messages` on the way back rather than parsed by
                // it.
                channel = VOICE_CHANNEL,
                sentAt = note.createdAt.toIsoOrNull(),
                kind = KIND_VOICE,
                // What the upload actually returned, or what the row this note
                // was pulled from said - never a fresh derivation. They agree
                // today, and if they ever stop, the object's real name is the
                // one that can be fetched.
                audioPath = uploaded.location.ifBlank { voicePath(note.id, s.circleId) },
                durationMs = note.durationMs,
                waveform = note.waveform
            ),
            CloudTables.MESSAGES
        )
    }

    private fun zone(recordId: String, s: SyncSnapshot): JsonObject? {
        val z = s.zones.firstOrNull { it.id == recordId } ?: return null
        return encode(
            ZoneRow.serializer(),
            ZoneRow(
                id = CloudIds.cloudId(z.id, s.circleId),
                circleId = s.circleId,
                wearerId = z.wearerId?.let { CloudIds.cloudId(it, s.circleId) },
                name = z.name.ifBlank { null },
                lat = z.lat,
                lon = z.lon,
                radiusMeters = z.radiusMeters.toDouble(),
                alertOnExit = z.alertOnExit,
                alertOnEnter = z.alertOnEnter
            ),
            CloudTables.ZONES
        )
    }

    // ============================================
    // TOMBSTONES
    // ============================================

    /**
     * The soft-delete body: the row's id, its circle, and nothing else.
     *
     * `profiles` is absent on purpose. Deleting an account goes through
     * `delete_account()`, which removes the `auth.users` row and cascades; a
     * tombstoned profile would be an account that still exists and cannot be
     * seen, which is worse than either outcome.
     */
    private fun tombstone(table: String, recordId: String, s: SyncSnapshot): JsonObject? {
        val id = serverIdFor(table, recordId, s.circleId) ?: return null
        return when (table) {
            CloudTables.WEARERS ->
                encode(WearerRow.serializer(), WearerRow(id, s.circleId), table)

            CloudTables.MEDICAL_IDS ->
                encode(MedicalIdRow.serializer(), MedicalIdRow(id, s.circleId), table)

            CloudTables.EMERGENCY_CONTACTS ->
                encode(
                    EmergencyContactRow.serializer(),
                    EmergencyContactRow(id, s.circleId),
                    table
                )

            CloudTables.DEVICES ->
                encode(DeviceRow.serializer(), DeviceRow(id, s.circleId), table)

            CloudTables.ALERTS ->
                encode(AlertRow.serializer(), AlertRow(id, s.circleId), table)

            CloudTables.MESSAGES ->
                encode(MessageRow.serializer(), MessageRow(id, s.circleId), table)

            CloudTables.ZONES ->
                encode(ZoneRow.serializer(), ZoneRow(id, s.circleId), table)

            else -> null
        }
    }

    /**
     * Where a voice note's audio lives in the `voice` bucket.
     *
     * `<circle_id>/<note_id>.m4a`, and the first segment is not decoration: the
     * storage policies in `0001_init.sql` and `0006_voice_messages.sql` read
     * the circle id straight out of it with `path_circle_id(name)`, so an
     * object filed anywhere else is unreadable by the Circle it belongs to and
     * unwritable by everyone.
     *
     * The note id rather than a random name so the path is derivable on any
     * phone that holds the row - a pull can find the object without the row
     * having to be trusted about where it put it.
     */
    fun voicePath(noteId: String, circleId: String): String = "$circleId/$noteId.m4a"

    /**
     * The uuid the server stores for a local record id.
     *
     * Public because a tombstone has to name a row the phone no longer holds,
     * so it cannot be looked up - it can only be derived. That is exactly what
     * [CloudIds] is for.
     */
    fun serverIdFor(table: String, recordId: String, circleId: String): String? = when (table) {
        CloudTables.PROFILES -> recordId.takeIf { it.isNotBlank() }

        CloudTables.MEDICAL_IDS ->
            CloudIds.ownerOf(recordId)?.let { CloudIds.medicalId(it, circleId) }

        CloudTables.EMERGENCY_CONTACTS -> CloudIds.contactId(
            CloudIds.ownerOf(recordId),
            recordId.substringAfterLast(':'),
            circleId
        )

        CloudTables.DEVICES ->
            recordId.substringAfter(':', "").takeIf { it.isNotBlank() }
                ?.let { CloudIds.deviceId(it, circleId) }

        else -> recordId.takeIf { it.isNotBlank() }?.let { CloudIds.cloudId(it, circleId) }
    }

    // ============================================
    // ENCODING
    // ============================================

    private fun <T> encode(serializer: KSerializer<T>, row: T, table: String): JsonObject {
        val dropped = ALWAYS_DROPPED + DROPPED_BY_TABLE[table].orEmpty()
        val encoded = json.encodeToJsonElement(serializer, row) as JsonObject
        return JsonObject(encoded.filterKeys { it !in dropped })
    }

    /**
     * An avatar id the rest of the circle can actually render, or null.
     *
     * The avatar kit's preset ids are shared vocabulary and travel fine. An id
     * beginning `photo:` is a handle to a file in this app's own storage - a
     * path that means nothing on anybody else's phone, and whose bytes are not
     * uploaded in this pass. Sending it would make every other member draw a
     * broken face rather than the initial-and-colour fallback a blank gives
     * them, so it is blanked here rather than at eleven call sites.
     */
    private fun shareableAvatar(avatarId: String): String? =
        avatarId.takeIf { it.isNotBlank() && !it.startsWith(PHOTO_PREFIX) }

    private const val PHOTO_PREFIX = "photo:"

    /**
     * `messages.channel` for a voice note.
     *
     * The schema allows `BLE | SMS | CLOUD` and `MessageChannel` has only
     * the first two - the app has two transports and the schema anticipated
     * a third. A voice note is the third.
     */
    private const val VOICE_CHANNEL = "CLOUD"

    private fun digitsOf(raw: String): String = PhoneNumbers.digitsOf(raw)
}
