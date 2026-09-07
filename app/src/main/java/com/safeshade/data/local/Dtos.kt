package com.safeshade.data.local

import com.safeshade.data.CheckInRequest
import com.safeshade.data.DeviceIconType
import com.safeshade.data.DeviceSettings
import com.safeshade.data.EmergencyContact
import com.safeshade.data.EscalationSettings
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.FallSensitivity
import com.safeshade.data.GeofenceZone
import com.safeshade.data.Journey
import com.safeshade.data.JourneyState
import com.safeshade.data.LocationState
import com.safeshade.data.MedicalId
import com.safeshade.data.MessageChannel
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.data.QuickMessage
import com.safeshade.data.Reminder
import com.safeshade.data.ReminderKind
import com.safeshade.data.SafetySettings
import com.safeshade.data.TelemetryPoint
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.data.UserRole
import com.safeshade.data.VoiceNote
import com.safeshade.data.VoiceUpload
import com.safeshade.data.Wearer
import java.util.UUID

/**
 * The on-disk shape of everything in `data/Models.kt`.
 *
 * These exist for one reason, and it is not layering purity.
 *
 * **Gson does not call Kotlin constructors.** For a class with no no-arg
 * constructor it falls through to `Unsafe.allocateInstance`, which produces a
 * zeroed object and then reflectively assigns only the fields that were
 * actually present in the JSON. Kotlin default arguments never run, and
 * nullability is a compile-time fiction to the reflection API. So a field
 * added to a domain model after a build's JSON was written lands as a `null`
 * sitting inside a non-null `String` property, and the NPE surfaces at some
 * unrelated `.isNotBlank()` three screens away with a stack trace pointing
 * nowhere near the decode.
 *
 * Every field below is therefore genuinely nullable with a `= null` default,
 * and `toDomain()` is the single place where a missing value becomes a real
 * default. That turns "old JSON, new field" into a boring total function
 * instead of a delayed crash.
 *
 * Enums are stored as their `name` string rather than their ordinal, because
 * an ordinal silently re-points at a different constant the moment anyone
 * inserts an entry — which [PersonaMode] just did by gaining `AUTO` at index
 * 0. Decoding goes through [enumOrDefault], so a name removed in a later build
 * (or corrupted on disk) degrades to a documented default rather than throwing
 * `IllegalArgumentException` out of a DataStore `map` operator.
 */

/**
 * Safe enum decode. Never throws.
 *
 * `enumValueOf` throws on an unknown name, and the one place that call happens
 * is inside a Flow operator, where the exception would tear down every
 * collector of the preferences stream at once.
 */
private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T {
    val raw = name ?: return default
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
}

// ============================================
// MEDICAL ID
// ============================================

data class MedicalIdDto(
    val bloodType: String? = null,
    val emergencyContact: String? = null,
    val contactName: String? = null,
    val allergies: String? = null,
    val age: Int? = null,
    val conditions: String? = null,
    val medications: String? = null,
    val secondaryContactName: String? = null,
    val secondaryContact: String? = null,
    val organDonor: Boolean? = null,
    val notes: String? = null
) {
    fun toDomain(): MedicalId = MedicalId(
        bloodType = bloodType.orEmpty(),
        emergencyContact = emergencyContact.orEmpty(),
        contactName = contactName.orEmpty(),
        allergies = allergies.orEmpty(),
        age = age ?: 0,
        conditions = conditions.orEmpty(),
        medications = medications.orEmpty(),
        secondaryContactName = secondaryContactName.orEmpty(),
        secondaryContact = secondaryContact.orEmpty(),
        organDonor = organDonor ?: false,
        notes = notes.orEmpty()
    )
}

fun MedicalId.toDto(): MedicalIdDto = MedicalIdDto(
    bloodType, emergencyContact, contactName, allergies, age, conditions,
    medications, secondaryContactName, secondaryContact, organDonor, notes
)

// ============================================
// DEVICE IDENTITY
// ============================================

data class DeviceSettingsDto(
    val id: String? = null,
    val name: String? = null,
    val iconType: String? = null,
    val wearerName: String? = null,
    val wearerAvatarId: String? = null,
    val isPrimary: Boolean? = null
) {
    fun toDomain(): DeviceSettings = DeviceSettings(
        id = id ?: UUID.randomUUID().toString(),
        name = name ?: "SafeShade S1",
        iconType = enumOrDefault(iconType, DeviceIconType.BACKPACK),
        wearerName = wearerName.orEmpty(),
        wearerAvatarId = wearerAvatarId.orEmpty(),
        isPrimary = isPrimary ?: true
    )
}

fun DeviceSettings.toDto(): DeviceSettingsDto =
    DeviceSettingsDto(id, name, iconType.name, wearerName, wearerAvatarId, isPrimary)

/**
 * The paired-device list.
 *
 * Two migration hazards live in this one small type:
 *
 *  - The timestamp field was `lastConnectedAt` in the v1 preferences model and
 *    is `lastConnected` on the domain type. Reading only the new name would
 *    silently reset every remembered device to the epoch, so both are read and
 *    coalesced.
 *  - `iconOrdinal` has no slot on the domain [PairedDevice], so it survives a
 *    read but is dropped the next time the list is written back. That is a
 *    deliberate lossy simplification, not an oversight: icon choice now belongs
 *    to [DeviceSettings.iconType], not to the pairing record.
 */
data class PairedDeviceDto(
    val address: String? = null,
    val name: String? = null,
    val iconOrdinal: Int? = null,
    val lastConnectedAt: Long? = null,
    val lastConnected: Long? = null
) {
    fun toDomain(): PairedDevice = PairedDevice(
        address = address.orEmpty(),
        name = name ?: "SafeShade",
        lastConnected = lastConnected ?: lastConnectedAt ?: 0L
    )
}

fun PairedDevice.toDto(): PairedDeviceDto = PairedDeviceDto(
    address = address,
    name = name,
    iconOrdinal = null,
    // Written under both names so a downgrade to a v1 build still finds a
    // timestamp where it expects one.
    lastConnectedAt = lastConnected,
    lastConnected = lastConnected
)

// ============================================
// SAFETY
// ============================================

data class EmergencyContactDto(
    val name: String? = null,
    val phone: String? = null,
    val isPrimary: Boolean? = null,
    /**
     * Added after contacts were already being written to disk, which is why it
     * is nullable with no default reached at read time: every contact saved
     * before this field existed decodes with `relationship == null` and
     * `orEmpty()` turns that into the empty string the domain type expects.
     * No migration, no reset, nothing to write back.
     */
    val relationship: String? = null
) {
    fun toDomain(): EmergencyContact = EmergencyContact(
        name = name.orEmpty(),
        phone = phone.orEmpty(),
        isPrimary = isPrimary ?: false,
        relationship = relationship.orEmpty()
    )
}

fun EmergencyContact.toDto(): EmergencyContactDto =
    EmergencyContactDto(name, phone, isPrimary, relationship)

data class SafetySettingsDto(
    val parentalControlsEnabled: Boolean? = null,
    val parentalPin: String? = null,
    val autoCallEmergency: Boolean? = null,
    val fallSensitivity: String? = null,
    val emergencyContacts: List<EmergencyContactDto?>? = null,
    val sosVolumeLevel: Float? = null,
    val smsFallbackEnabled: Boolean? = null,
    val fallCountdownSeconds: Int? = null,
    /**
     * Null on every store written before the ladder existed, and decoded as
     * the documented defaults rather than as "off" - a guardian who upgrades
     * gets the feature, not a silently disabled one.
     */
    val escalation: EscalationSettingsDto? = null,
    val offlineAlertMinutes: Int? = null,
    val lowBatteryPercent: Int? = null
) {
    fun toDomain(): SafetySettings = SafetySettings(
        parentalControlsEnabled = parentalControlsEnabled ?: false,
        parentalPin = parentalPin.orEmpty(),
        autoCallEmergency = autoCallEmergency ?: true,
        fallSensitivity = enumOrDefault(fallSensitivity, FallSensitivity.MEDIUM),
        // The element type is nullable too: a literal `null` inside the JSON
        // array is something Gson will happily hand back, and `map` over a
        // List<T?> declared as List<T> is exactly the delayed NPE this file
        // exists to prevent.
        emergencyContacts = emergencyContacts.orEmpty().filterNotNull().map { it.toDomain() },
        sosVolumeLevel = sosVolumeLevel ?: 0.8f,
        smsFallbackEnabled = smsFallbackEnabled ?: false,
        fallCountdownSeconds = fallCountdownSeconds ?: 30,
        escalation = (escalation ?: EscalationSettingsDto()).toDomain(),
        // Negative values are clamped rather than rejected: a stored -1 would
        // otherwise read as "alert immediately, forever".
        offlineAlertMinutes = (offlineAlertMinutes ?: 120).coerceAtLeast(0),
        lowBatteryPercent = (lowBatteryPercent ?: 15).coerceIn(0, 100)
    )
}

/**
 * The ladder's settings on disk.
 *
 * The delays are clamped on decode, not on write. A zero first delay would mean
 * an alert dialling a contact in the same instant it is logged, before the
 * person has had any chance to touch "I'm OK", and a truncated JSON write is
 * enough to produce one.
 */
data class EscalationSettingsDto(
    val enabled: Boolean? = null,
    val firstDelaySec: Int? = null,
    val secondDelaySec: Int? = null,
    val thenEmergency: Boolean? = null,
    val emergencyNumber: String? = null
) {
    fun toDomain(): EscalationSettings = EscalationSettings(
        // Off when absent: an install upgraded from before the ladder existed
        // must not start ringing people on a timer. See EscalationSettings.
        enabled = enabled ?: false,
        firstDelaySec = (firstDelaySec ?: 30).coerceIn(MIN_DELAY_SEC, MAX_DELAY_SEC),
        secondDelaySec = (secondDelaySec ?: 60).coerceIn(MIN_DELAY_SEC, MAX_DELAY_SEC),
        thenEmergency = thenEmergency ?: true,
        emergencyNumber = emergencyNumber?.takeIf { it.isNotBlank() } ?: "112"
    )

    private companion object {
        const val MIN_DELAY_SEC = 5
        const val MAX_DELAY_SEC = 3600
    }
}

fun EscalationSettings.toDto(): EscalationSettingsDto = EscalationSettingsDto(
    enabled = enabled,
    firstDelaySec = firstDelaySec,
    secondDelaySec = secondDelaySec,
    thenEmergency = thenEmergency,
    emergencyNumber = emergencyNumber
)

fun SafetySettings.toDto(): SafetySettingsDto = SafetySettingsDto(
    parentalControlsEnabled = parentalControlsEnabled,
    parentalPin = parentalPin,
    autoCallEmergency = autoCallEmergency,
    fallSensitivity = fallSensitivity.name,
    emergencyContacts = emergencyContacts.map { it.toDto() },
    sosVolumeLevel = sosVolumeLevel,
    smsFallbackEnabled = smsFallbackEnabled,
    fallCountdownSeconds = fallCountdownSeconds,
    escalation = escalation.toDto(),
    offlineAlertMinutes = offlineAlertMinutes,
    lowBatteryPercent = lowBatteryPercent
)

data class FallAlertEventDto(
    val id: String? = null,
    val timestamp: Long? = null,
    val kind: String? = null,
    val outcome: String? = null,
    val wasEmergencyContacted: Boolean? = null,
    val location: String? = null,
    val note: String? = null,
    /**
     * Which wearer this trip belongs to. Null on every record written before
     * the app knew about more than one person, and read as "the primary
     * wearer" — see `resolveWearerForDevice`. Never defaulted to a real id
     * here: guessing an owner for a fall alert is exactly the kind of
     * plausible fiction a guardian would later act on.
     */
    val wearerId: String? = null
) {
    fun toDomain(): FallAlertEvent = FallAlertEvent(
        id = id ?: UUID.randomUUID().toString(),
        timestamp = timestamp ?: 0L,
        kind = enumOrDefault(kind, TripKind.FALL),
        outcome = enumOrDefault(outcome, TripOutcome.PENDING),
        wasEmergencyContacted = wasEmergencyContacted ?: false,
        location = location,
        note = note,
        wearerId = wearerId
    )
}

fun FallAlertEvent.toDto(): FallAlertEventDto = FallAlertEventDto(
    id, timestamp, kind.name, outcome.name, wasEmergencyContacted, location, note, wearerId
)

data class CheckInRequestDto(
    val id: String? = null,
    val sentAt: Long? = null,
    val deadlineAt: Long? = null,
    val answeredAt: Long? = null,
    val escalated: Boolean? = null,
    /** Which wearer was asked. Null means the primary one. */
    val wearerId: String? = null
) {
    fun toDomain(): CheckInRequest {
        val sent = sentAt ?: 0L
        return CheckInRequest(
            id = id ?: UUID.randomUUID().toString(),
            sentAt = sent,
            // A stored request with no deadline must not read as "due now" —
            // that would escalate on the next cold start, calling an emergency
            // contact because of a truncated JSON write. It decodes as an
            // already-escalated (therefore closed) request instead.
            deadlineAt = deadlineAt ?: sent,
            escalated = escalated ?: (deadlineAt == null),
            answeredAt = answeredAt,
            wearerId = wearerId
        )
    }
}

fun CheckInRequest.toDto(): CheckInRequestDto =
    CheckInRequestDto(id, sentAt, deadlineAt, answeredAt, escalated, wearerId)

// ============================================
// GEOFENCING
// ============================================

data class GeofenceZoneDto(
    val id: String? = null,
    val name: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val radiusMeters: Float? = null,
    val alertOnExit: Boolean? = null,
    val alertOnEnter: Boolean? = null,
    /** Whose safe zone this is. Null means it applies to the primary wearer. */
    val wearerId: String? = null
) {
    fun toDomain(): GeofenceZone = GeofenceZone(
        id = id ?: UUID.randomUUID().toString(),
        name = name ?: "Safe zone",
        lat = lat ?: 0.0,
        lon = lon ?: 0.0,
        // The Geofencing API rejects a non-positive radius outright, and it
        // does so asynchronously, so a zero here would fail the whole
        // registration batch rather than just this zone.
        radiusMeters = radiusMeters?.takeIf { it > 0f } ?: 200f,
        alertOnExit = alertOnExit ?: true,
        alertOnEnter = alertOnEnter ?: false,
        wearerId = wearerId
    )
}

fun GeofenceZone.toDto(): GeofenceZoneDto =
    GeofenceZoneDto(id, name, lat, lon, radiusMeters, alertOnExit, alertOnEnter, wearerId)

// ============================================
// MESSAGING
// ============================================

data class QuickMessageDto(
    val id: String? = null,
    val text: String? = null,
    val fromGuardian: Boolean? = null,
    val timestamp: Long? = null,
    val replied: Boolean? = null,
    val replyText: String? = null,
    val channel: String? = null,
    /** Which wearer the thread belongs to. Null means the primary one. */
    val wearerId: String? = null
) {
    fun toDomain(): QuickMessage = QuickMessage(
        id = id ?: UUID.randomUUID().toString(),
        text = text.orEmpty(),
        fromGuardian = fromGuardian ?: true,
        timestamp = timestamp ?: 0L,
        replied = replied ?: false,
        replyText = replyText,
        channel = enumOrDefault(channel, MessageChannel.BLE),
        wearerId = wearerId
    )
}

fun QuickMessage.toDto(): QuickMessageDto =
    QuickMessageDto(id, text, fromGuardian, timestamp, replied, replyText, channel.name, wearerId)

// ============================================
// VOICE NOTES
// ============================================

/**
 * A voice note on disk.
 *
 * The upload state is stored as a **tag plus one payload string** rather than
 * as a nested object per case. Gson has no idea what a Kotlin sealed interface
 * is: asked to decode one it picks the declared type and produces either a
 * crash or an instance of the wrong case. A tag and a string is the shape that
 * survives both a new case being added later and an older build reading a
 * newer store, and [toDomain] is the one place the pair becomes a type again.
 *
 * An unknown tag decodes to [VoiceUpload.LocalOnly], which is the honest
 * reading: this build does not know that the audio is anywhere else, and the
 * file on this phone is the copy it can actually play.
 */
data class VoiceNoteDto(
    val id: String? = null,
    val wearerId: String? = null,
    val fromGuardian: Boolean? = null,
    val authorName: String? = null,
    val file: String? = null,
    val durationMs: Int? = null,
    val waveform: List<Float?>? = null,
    val createdAt: Long? = null,
    /** One of `local`, `uploading`, `uploaded`, `failed`. */
    val uploadState: String? = null,
    /** The location for `uploaded`, the reason for `failed`, otherwise null. */
    val uploadDetail: String? = null,
    val listened: Boolean? = null
) {
    fun toDomain(): VoiceNote = VoiceNote(
        id = id ?: UUID.randomUUID().toString(),
        wearerId = wearerId,
        fromGuardian = fromGuardian ?: true,
        authorName = authorName.orEmpty(),
        file = file.orEmpty(),
        durationMs = (durationMs ?: 0).coerceAtLeast(0),
        // A null inside the array is something Gson will hand back happily,
        // and mapping over a List<Float?> declared as List<Float> is exactly
        // the delayed NPE this file exists to prevent.
        waveform = waveform.orEmpty().filterNotNull().map { it.coerceIn(0f, 1f) },
        createdAt = createdAt ?: 0L,
        uploadState = when (uploadState) {
            STATE_UPLOADING -> VoiceUpload.Uploading
            STATE_UPLOADED -> uploadDetail?.let { VoiceUpload.Uploaded(it) } ?: VoiceUpload.LocalOnly
            STATE_FAILED -> VoiceUpload.Failed(uploadDetail.orEmpty().ifBlank { "Upload failed" })
            else -> VoiceUpload.LocalOnly
        },
        listened = listened ?: false
    )

    companion object {
        const val STATE_LOCAL = "local"
        const val STATE_UPLOADING = "uploading"
        const val STATE_UPLOADED = "uploaded"
        const val STATE_FAILED = "failed"
    }
}

fun VoiceNote.toDto(): VoiceNoteDto = VoiceNoteDto(
    id = id,
    wearerId = wearerId,
    fromGuardian = fromGuardian,
    authorName = authorName,
    file = file,
    durationMs = durationMs,
    waveform = waveform,
    createdAt = createdAt,
    uploadState = when (uploadState) {
        VoiceUpload.LocalOnly -> VoiceNoteDto.STATE_LOCAL
        VoiceUpload.Uploading -> VoiceNoteDto.STATE_UPLOADING
        is VoiceUpload.Uploaded -> VoiceNoteDto.STATE_UPLOADED
        is VoiceUpload.Failed -> VoiceNoteDto.STATE_FAILED
    },
    uploadDetail = when (val state = uploadState) {
        is VoiceUpload.Uploaded -> state.location
        is VoiceUpload.Failed -> state.reason
        else -> null
    },
    listened = listened
)

// ============================================
// REMINDERS
// ============================================

data class ReminderDto(
    val id: String? = null,
    val kind: String? = null,
    val hour: Int? = null,
    val minute: Int? = null,
    val intervalMinutes: Int? = null,
    val enabled: Boolean? = null,
    val label: String? = null,
    /**
     * Whose reminder this is. Null means the primary wearer.
     *
     * Reminders are per-person rather than per-phone because a medication time
     * is a fact about a body: two people in one Circle take different pills at
     * different hours, and a single global list would fire both alarms for
     * whoever happened to be selected.
     */
    val wearerId: String? = null
) {
    fun toDomain(): Reminder = Reminder(
        id = id ?: UUID.randomUUID().toString(),
        kind = enumOrDefault(kind, ReminderKind.MEDICATION),
        // Clamped here rather than at the alarm site: an out-of-range hour
        // reaches AlarmManager as a wall-clock time that never arrives, which
        // presents as a reminder that simply never fires.
        hour = (hour ?: 9).coerceIn(0, 23),
        minute = (minute ?: 0).coerceIn(0, 59),
        intervalMinutes = (intervalMinutes ?: 0).coerceAtLeast(0),
        enabled = enabled ?: true,
        label = label.orEmpty(),
        wearerId = wearerId
    )
}

fun Reminder.toDto(): ReminderDto =
    ReminderDto(id, kind.name, hour, minute, intervalMinutes, enabled, label, wearerId)

// ============================================
// JOURNEY
// ============================================

data class JourneyDto(
    val id: String? = null,
    val label: String? = null,
    val startedAt: Long? = null,
    val etaAt: Long? = null,
    val graceSeconds: Int? = null,
    val destinationLat: Double? = null,
    val destinationLon: Double? = null,
    val state: String? = null,
    /** Who is walking. Null means the primary wearer. */
    val wearerId: String? = null
) {
    fun toDomain(): Journey {
        val started = startedAt ?: 0L
        return Journey(
            id = id ?: UUID.randomUUID().toString(),
            label = label.orEmpty(),
            startedAt = started,
            etaAt = etaAt ?: started,
            graceSeconds = (graceSeconds ?: 60).coerceAtLeast(0),
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            // A journey with no stored ETA cannot be escalated against
            // meaningfully, so it decodes as CANCELLED rather than as an
            // ACTIVE journey whose deadline is already in the past — which
            // would escalate the instant the state machine resumed.
            state = enumOrDefault(
                state,
                if (etaAt == null) JourneyState.CANCELLED else JourneyState.ACTIVE
            ),
            wearerId = wearerId
        )
    }
}

fun Journey.toDto(): JourneyDto = JourneyDto(
    id, label, startedAt, etaAt, graceSeconds, destinationLat, destinationLon, state.name,
    wearerId
)

// ============================================
// TELEMETRY & LOCATION
// ============================================

data class TelemetryPointDto(
    val at: Long? = null,
    val batteryPercent: Int? = null,
    val rssiDbm: Int? = null
) {
    fun toDomain(): TelemetryPoint = TelemetryPoint(
        at = at ?: 0L,
        batteryPercent = (batteryPercent ?: 0).coerceIn(0, 100),
        // -127 dBm is the "no reading" sentinel Android itself uses. Zero
        // would draw as a perfect signal on the link sparkline.
        rssiDbm = rssiDbm ?: -127
    )
}

fun TelemetryPoint.toDto(): TelemetryPointDto = TelemetryPointDto(at, batteryPercent, rssiDbm)

data class LocationStateDto(
    val lat: Double? = null,
    val lon: Double? = null,
    val locationName: String? = null,
    val locality: String? = null,
    val altitude: Int? = null,
    val isValid: Boolean? = null,
    val capturedAt: Long? = null
) {
    fun toDomain(): LocationState = LocationState(
        lat = lat ?: 0.0,
        lon = lon ?: 0.0,
        locationName = locationName.orEmpty(),
        locality = locality.orEmpty(),
        altitude = altitude ?: 0,
        // Never trust a stored `isValid` over the coordinates themselves. A
        // half-written record claiming validity at 0,0 renders as a confident
        // "last seen" pin in the Gulf of Guinea.
        isValid = (isValid ?: false) && (lat != null && lon != null) && (lat != 0.0 || lon != 0.0),
        capturedAt = capturedAt ?: 0L
    )
}

fun LocationState.toDto(): LocationStateDto =
    LocationStateDto(lat, lon, locationName, locality, altitude, isValid, capturedAt)

// ============================================
// PROFILE AGGREGATE
// ============================================

/**
 * The identity blob: who this phone is, what it is paired to, and which
 * adaptive mode the wearable should be in.
 *
 * Stored as one aggregate rather than four keys because these four are always
 * read together to build the first frame, and four separate DataStore reads
 * are four separate chances to render a half-migrated screen.
 *
 * [activeMode] holds the [PersonaMode.wireName], not the Kotlin enum `name`.
 * They are identical today, but the wire name is the actual contract with the
 * firmware, and decoding through [PersonaMode.fromWire] means a stored name a
 * later build removes falls back exactly the way the firmware's
 * `modeFromName()` does — to BACKPACK, not to AUTO.
 */
data class ProfileDto(
    val medicalId: MedicalIdDto? = null,
    val deviceSettings: DeviceSettingsDto? = null,
    val role: String? = null,
    val activeMode: String? = null,
    val ownerName: String? = null,
    val ownerAvatarId: String? = null
) {
    fun toDomain(): ProfileSnapshot = ProfileSnapshot(
        medicalId = (medicalId ?: MedicalIdDto()).toDomain(),
        deviceSettings = (deviceSettings ?: DeviceSettingsDto()).toDomain(),
        role = enumOrDefault(role, UserRole.GUARDIAN),
        activeMode = PersonaMode.fromWire(activeMode.orEmpty()),
        ownerName = ownerName.orEmpty(),
        ownerAvatarId = ownerAvatarId.orEmpty()
    )
}

/** The decoded form of [ProfileDto]. Plain data — no Compose, no Android. */
data class ProfileSnapshot(
    val medicalId: MedicalId = MedicalId(),
    val deviceSettings: DeviceSettings = DeviceSettings(),
    val role: UserRole = UserRole.GUARDIAN,
    val activeMode: PersonaMode = PersonaMode.BACKPACK,
    /**
     * The person holding the phone — "Me" on the Profile page. For a
     * Companion this is also the wearer; for a Guardian it is the guardian.
     * Phase 2's account and wearer model builds on these two fields rather
     * than replacing them.
     */
    val ownerName: String = "",
    val ownerAvatarId: String = ""
)

fun ProfileSnapshot.toDto(): ProfileDto = ProfileDto(
    medicalId = medicalId.toDto(),
    deviceSettings = deviceSettings.toDto(),
    role = role.name,
    activeMode = activeMode.wireName,
    ownerName = ownerName,
    ownerAvatarId = ownerAvatarId
)

// ============================================
// WEARERS
// ============================================

/**
 * The on-disk shape of a [com.safeshade.data.Wearer].
 *
 * Note what is *not* here, and that the absence is enforced by this file
 * having no field for it: no PIN, no SMS allowlist, no device SIM number.
 * `parentalPin` is the secret that stops a child from turning off their own
 * protection, the allowlist and the SIM number are facts about hardware, and a
 * wearer record is the thing this app is most likely to hand to somebody else
 * — a second guardian, a server row, an exported card. A secret that rides
 * along inside a record designed to be shared has already leaked.
 *
 * [deviceAddresses] is stored as written; matching is case-insensitive at the
 * point of use rather than by normalising here, because normalising on write
 * would silently rewrite an address a user could otherwise recognise.
 */
data class WearerDto(
    val id: String? = null,
    val name: String? = null,
    val avatarId: String? = null,
    val medicalId: MedicalIdDto? = null,
    val activeMode: String? = null,
    val iconType: String? = null,
    val deviceAddresses: List<String?>? = null,
    val contacts: List<EmergencyContactDto?>? = null,
    val isSelf: Boolean? = null
) {
    fun toDomain(): Wearer = Wearer(
        // A wearer with no stored id is unaddressable: nothing could select
        // it, bind a device to it or remove it. A fresh id is the only
        // recoverable answer, and it is stable from the next write onwards.
        id = id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
        name = name.orEmpty(),
        avatarId = avatarId.orEmpty(),
        medicalId = (medicalId ?: MedicalIdDto()).toDomain(),
        // Through fromWire, not enumOrDefault: a stored mode name that a later
        // build removed must fall back the way the firmware's own
        // modeFromName() does.
        activeMode = PersonaMode.fromWire(activeMode.orEmpty()),
        iconType = enumOrDefault(iconType, DeviceIconType.BACKPACK),
        deviceAddresses = deviceAddresses.orEmpty().filterNotNull().filter { it.isNotBlank() },
        contacts = contacts.orEmpty().filterNotNull().map { it.toDomain() },
        isSelf = isSelf ?: false
    )
}

fun Wearer.toDto(): WearerDto = WearerDto(
    id = id,
    name = name,
    avatarId = avatarId,
    medicalId = medicalId.toDto(),
    activeMode = activeMode.wireName,
    iconType = iconType.name,
    deviceAddresses = deviceAddresses,
    contacts = contacts.map { it.toDto() },
    isSelf = isSelf
)
