package com.safeshade.data

import java.util.UUID

/**
 * Every piece of app state, as plain immutable data.
 *
 * Deliberately free of Compose types. The previous version carried
 * `ImageVector` and `Color` on `PersonaMode` and `DeviceIconType`, which put a
 * UI dependency in the persistence path and — more damagingly — gave each mode
 * its own accent hue. The Distribution Board system reserves colour for
 * circuit state, so icon and any visual treatment now live in
 * `ui/board/ModeVisuals.kt`, keyed off these enums.
 */

// ============================================
// WEATHER & LOCATION
// ============================================

data class WeatherUiState(
    val rainChance: Int = 0,
    val condition: String = "",
    val uvIndex: Float = 0f,
    val humidity: Float = 0f,
    val temp: Float = 0f,
    val isLoaded: Boolean = false,
    val lastSyncTime: String = "--:--"
)

data class LocationState(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val locationName: String = "",
    val locality: String = "",
    val altitude: Int = 0,
    val isValid: Boolean = false,
    /** When this fix was taken. Used to age "last seen" copy honestly. */
    val capturedAt: Long = 0L
)

// ============================================
// ROLE
// ============================================

/**
 * Which end of the link this phone is.
 *
 * This is the single most load-bearing setting in the app, because it decides
 * whether every string reads "you are wearing this" or "someone you are
 * responsible for is wearing this". Chosen in onboarding, changeable later.
 */
enum class UserRole {
    /** A caregiver phone. The wearer never opens this app. */
    GUARDIAN,

    /** The wearer own phone. Wearer and app user are the same person. */
    COMPANION
}

// ============================================
// MEDICAL ID - 11 fields, matching HEALTH_CHAR exactly
// ============================================

/**
 * Field order here is the wire order the firmware parses positionally
 * (`HealthCallbacks::onWrite`). Fields 6-11 were added firmware-side and had
 * no app representation at all until now, so a guardian could not fill in the
 * on-device Medical ID screen lower half.
 *
 * `notes` is the firmware field 11. The old model `medicalNotes` mapped to
 * nothing — it was never transmitted — and its default value was a list of
 * conditions, so it migrates into [conditions], not into [notes].
 */
data class MedicalId(
    val bloodType: String = "",
    val emergencyContact: String = "",
    val contactName: String = "",
    val allergies: String = "",
    val age: Int = 0,
    val conditions: String = "",
    val medications: String = "",
    val secondaryContactName: String = "",
    val secondaryContact: String = "",
    val organDonor: Boolean = false,
    val notes: String = ""
) {
    /** True when there is enough here to be worth showing a responder. */
    val isUsable: Boolean
        get() = bloodType.isNotBlank() || emergencyContact.isNotBlank() || allergies.isNotBlank()

    /** How complete the card is, for the setup progress affordance. */
    val filledFieldCount: Int
        get() = listOf(
            bloodType, emergencyContact, contactName, allergies,
            if (age > 0) "y" else "", conditions, medications,
            secondaryContactName, secondaryContact, notes
        ).count { it.isNotBlank() } + if (organDonor) 1 else 0
}

// ============================================
// DEVICE IDENTITY
// ============================================

data class DeviceSettings(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "SafeShade S1",
    val iconType: DeviceIconType = DeviceIconType.BACKPACK,
    /** Who wears it. Used throughout Guardian copy, e.g. "Baba's cane". */
    val wearerName: String = "",
    /** The wearer's face: an avatar id as `ui/board/Avatar.kt` defines them, or blank. */
    val wearerAvatarId: String = "",
    val isPrimary: Boolean = true
)

enum class DeviceIconType(val label: String) {
    UMBRELLA("Umbrella"),
    WATCH("Wristband"),
    BACKPACK("Backpack"),
    BIKE("Bicycle"),
    PENDANT("Locket"),
    HAT("Cap"),
    CANE("Cane"),
    COLLAR("Pet collar")
}

data class PairedDevice(
    val address: String,
    val name: String,
    val lastConnected: Long = System.currentTimeMillis()
)

// ============================================
// WEARERS - the people this phone looks after
// ============================================

/**
 * One person who is looked after, with everything that belongs to *them*
 * rather than to the phone.
 *
 * A GUARDIAN phone may hold any number of these; a COMPANION phone holds
 * exactly one, marked [isSelf], because the person holding the phone and the
 * person wearing the device are the same. That asymmetry is the whole reason
 * this type exists: until now the single wearer lived in
 * `DeviceSettings.wearerName` inside the profile blob, which cannot express
 * "Baba and Ma and the dog" at all.
 *
 * ### Binding is by BLE address, never by device id
 *
 * [deviceAddresses] holds the MAC-style addresses of the wearables this person
 * wears. `DeviceSettings.id` is a UUID this app minted for itself and is the
 * same value on every install of the same profile, so it identifies nothing
 * about the hardware on the other end of the link. The address is what the
 * link actually reports, and it is therefore the only key that can answer
 * "whose device just connected" — which is a different question from "whose
 * page is on screen".
 *
 * ### What is deliberately absent
 *
 * No PIN, no SMS allowlist, no device SIM number. `parentalPin` guards this
 * phone, the allowlist and the SIM number belong to the hardware; none of the
 * three is a fact about a person, and putting any of them here would put a
 * secret into a per-person record that is meant to be shareable.
 *
 * [contacts] supplements the global [SafetySettings.emergencyContacts] rather
 * than replacing it — see [contactsFor]. The global list stays the SOS source
 * so that adding a second wearer can never quietly shorten the list of people
 * an emergency reaches.
 */
data class Wearer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    /** An avatar id as `ui/board/Avatar.kt` defines them, or blank. */
    val avatarId: String = "",
    val medicalId: MedicalId = MedicalId(),
    val activeMode: PersonaMode = PersonaMode.BACKPACK,
    val iconType: DeviceIconType = DeviceIconType.BACKPACK,
    /** BLE addresses of the wearables this person wears. */
    val deviceAddresses: List<String> = emptyList(),
    /** Extra contacts for this person only. Never the whole SOS list. */
    val contacts: List<EmergencyContact> = emptyList(),
    /** True when this wearer is the person holding the phone. */
    val isSelf: Boolean = false
)

// ============================================
// ADAPTIVE MODES - 8, matching the firmware
// ============================================

/**
 * The firmware `PersonaMode`. [wireName] is what crosses the link via
 * `EXT_CHAR` `MODE:<name>` and what comes back as `ACK:MODE:<name>`; it must
 * match `modeFromName()` exactly.
 *
 * [AUTO] is the firmware fresh-boot default and was entirely absent from the
 * app, so a factory-fresh device sat in a mode the app could neither display
 * nor select.
 *
 * [isGuardianLocked] mirrors `isModeGuardianLocked()`: in these four modes the
 * wearable hides mode switching and its whole Safety menu, so the wearer
 * cannot quietly weaken their own protection. Those settings remain reachable
 * here, which is precisely why the app must show that it now owns them.
 */
enum class PersonaMode(
    val wireName: String,
    val label: String,
    val blurb: String,
    val defaultFallSensitivity: FallSensitivity,
    val simplifiedUi: Boolean,
    val isGuardianLocked: Boolean,
    val matchingDeviceIcon: DeviceIconType
) {
    AUTO(
        "AUTO", "Adaptive",
        "The device picks the profile that fits how it is being carried.",
        FallSensitivity.MEDIUM, simplifiedUi = false, isGuardianLocked = false,
        matchingDeviceIcon = DeviceIconType.BACKPACK
    ),
    ELDERLY(
        "ELDERLY", "Elderly",
        "Most sensitive fall detection, larger on-device text, medication reminders.",
        FallSensitivity.HIGH, simplifiedUi = true, isGuardianLocked = true,
        matchingDeviceIcon = DeviceIconType.CANE
    ),
    KIDS(
        "KIDS", "Kids",
        "Location first, safe-zone alerts, quiet hours during school.",
        FallSensitivity.MEDIUM, simplifiedUi = true, isGuardianLocked = true,
        matchingDeviceIcon = DeviceIconType.BACKPACK
    ),
    BIKE(
        "BIKE", "Bike",
        "Crash detection tuned for cycling, ride stats, brake light.",
        FallSensitivity.HIGH, simplifiedUi = false, isGuardianLocked = false,
        matchingDeviceIcon = DeviceIconType.BIKE
    ),
    PET(
        "PET", "Pet",
        "Activity tracking with fall detection off, and a virtual leash.",
        FallSensitivity.LOW, simplifiedUi = false, isGuardianLocked = true,
        matchingDeviceIcon = DeviceIconType.COLLAR
    ),
    HELMET(
        "HELMET", "Helmet",
        "High-impact threshold, worker check-ins, minimal on-device distraction.",
        FallSensitivity.LOW, simplifiedUi = false, isGuardianLocked = true,
        matchingDeviceIcon = DeviceIconType.HAT
    ),
    WRIST(
        "WRIST", "Wrist",
        "Everyday wristband profile with an activity tally.",
        FallSensitivity.MEDIUM, simplifiedUi = false, isGuardianLocked = false,
        matchingDeviceIcon = DeviceIconType.WATCH
    ),
    BACKPACK(
        "BACKPACK", "Backpack",
        "Balanced commuter profile. Everything on, nothing emphasised.",
        FallSensitivity.MEDIUM, simplifiedUi = false, isGuardianLocked = false,
        matchingDeviceIcon = DeviceIconType.BACKPACK
    );

    companion object {
        /**
         * Mirrors the firmware `modeFromName()`, including its fallback.
         *
         * The firmware deliberately falls back to BACKPACK rather than AUTO for
         * an unrecognised name, so that an old app build sending garbage never
         * lands the device on the adaptive placeholder. Matching that here keeps
         * the two ends agreeing about what an unknown string means.
         */
        fun fromWire(name: String): PersonaMode =
            entries.firstOrNull { it.wireName.equals(name.trim(), ignoreCase = true) } ?: BACKPACK
    }
}

// ============================================
// SAFETY
// ============================================

/** Ordinal is the wire value the firmware clamps to 0..2. Do not reorder. */
enum class FallSensitivity(val label: String, val blurb: String) {
    LOW("Low", "Only severe impacts"),
    MEDIUM("Medium", "Recommended"),
    HIGH("High", "Most sensitive")
}

data class EmergencyContact(
    val name: String,
    val phone: String,
    val isPrimary: Boolean = false,
    /**
     * How this person is related to the wearer, in the wearer's own words.
     *
     * Free text rather than an enum, and empty rather than null. A responder
     * reading a phone's lock screen needs "Daughter" or "Upstairs neighbour",
     * not a value from a list the app happened to think of; the chip row that
     * fills this field offers the common answers and then gets out of the way.
     * Empty is a real state and means the contact predates this field or the
     * user chose not to say - never render a placeholder in its place.
     */
    val relationship: String = ""
)

data class SafetySettings(
    val parentalControlsEnabled: Boolean = false,
    val parentalPin: String = "",
    val autoCallEmergency: Boolean = true,
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM,
    val emergencyContacts: List<EmergencyContact> = emptyList(),
    val sosVolumeLevel: Float = 0.8f,
    val smsFallbackEnabled: Boolean = false,
    /** Seconds before an unattended fall alert places a call. */
    val fallCountdownSeconds: Int = 30,

    /** The contact-by-contact ladder that runs when an alert goes unanswered. */
    val escalation: EscalationSettings = EscalationSettings(),

    /**
     * How long a bound wearable may be out of reach before the guardian is
     * told. Zero switches the notice off entirely.
     *
     * Two hours by default because a wearable left on a charger in another
     * room is the common case and a shorter window would train the guardian to
     * swipe the notice away — which is the failure mode that matters, since it
     * is the same notice that reports a device left behind on a bus.
     */
    val offlineAlertMinutes: Int = 120,

    /**
     * The battery percentage at or below which the guardian is told once.
     * Zero switches the notice off.
     */
    val lowBatteryPercent: Int = 15
) {
    val primaryContact: EmergencyContact?
        get() = emergencyContacts.firstOrNull { it.isPrimary } ?: emergencyContacts.firstOrNull()
}

/**
 * The unanswered-alert ladder: contact one, then contact two, then the
 * emergency number.
 *
 * The delays are seconds rather than minutes because the whole ladder has to
 * finish inside the window in which a fall is still an emergency, and because
 * the first rung is deliberately close behind the fall countdown - somebody who
 * did not answer their phone in thirty seconds is not more likely to answer it
 * in five minutes.
 *
 * [emergencyNumber] is a string, not an Int: 112 keeps its leading digits, and
 * a country whose service number is not three digits is a settings change
 * rather than a code change.
 */
data class EscalationSettings(
    /**
     * Off until a person turns it on. The ladder dials real numbers on a
     * timer, and a default that did so on a phone whose owner had never seen
     * the page would be the app deciding, on their behalf, to ring their
     * contacts. The page that switches it on says what it will do first.
     */
    val enabled: Boolean = false,
    /** Seconds after the alert is logged before contact one is dialled. */
    val firstDelaySec: Int = 30,
    /** Seconds after that before contact two is dialled. */
    val secondDelaySec: Int = 60,
    /** Whether the ladder ends at the emergency number. */
    val thenEmergency: Boolean = true,
    val emergencyNumber: String = "112"
)

/** A recorded trip on the board: a fall, an SOS, a missed check-in. */
data class FallAlertEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val kind: TripKind = TripKind.FALL,
    val outcome: TripOutcome = TripOutcome.PENDING,
    val wasEmergencyContacted: Boolean = false,
    /** The guardian phone location when logged — not a device GPS fix. */
    val location: String? = null,
    /** A real telemetry snapshot at trip time, when the link was live. */
    val note: String? = null,
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
)

enum class TripKind(val label: String) {
    FALL("Fall detected"),

    /** The physical button on the wearable. */
    SOS("SOS pressed"),

    /**
     * The SOS control in this app's bottom bar.
     *
     * Distinct from [SOS] rather than folded into it because the trip log is
     * read after the fact, sometimes months later, and "the SOS button was held
     * on the device" is simply false when someone raised it from their phone.
     * Whether the wearable was even involved is exactly the sort of detail that
     * matters when a guardian is reconstructing what happened.
     *
     * Safe to have been added mid-life: these are persisted by *name* (see
     * Dtos.kt, which decodes with `enumOrDefault`), so an older build reading a
     * newer store degrades this to FALL — still an alert in the log — rather
     * than shifting every subsequent ordinal.
     */
    PHONE_SOS("SOS sent from the phone"),
    MISSED_CHECKIN("Check-in missed"),
    ZONE_EXIT("Left a safe zone"),
    JOURNEY_OVERDUE("Journey overdue")
}

enum class TripOutcome(val label: String) {
    PENDING("Awaiting response"),
    DISMISSED("Dismissed as OK"),
    CONTACTED("Emergency contacted"),
    AUTO_RESOLVED("Resolved automatically")
}

// ============================================
// MESSAGING
// ============================================

data class QuickMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromGuardian: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val replied: Boolean = false,
    val replyText: String? = null,
    /** How it travelled. Shown so a guardian knows an SMS may cost money. */
    val channel: MessageChannel = MessageChannel.BLE,
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
)

enum class MessageChannel { BLE, SMS }

/**
 * Where a voice note's audio currently lives.
 *
 * Modelled as a state rather than a `Boolean uploaded` because the interesting
 * value is the middle one: a note that is uploading has been recorded, is
 * playable on this phone and is not yet anywhere else, and a plate that drew a
 * tick for it would be claiming the other person could hear it. [Failed]
 * carries the reason so the row can say why rather than showing a bare cross.
 */
sealed interface VoiceUpload {

    /** Recorded on this phone and never sent. The state every note starts in. */
    data object LocalOnly : VoiceUpload

    data object Uploading : VoiceUpload

    /** @param location the storage path or URL the sync track wrote it to. */
    data class Uploaded(val location: String) : VoiceUpload

    /** @param reason user-facing, verbatim from whatever refused the upload. */
    data class Failed(val reason: String) : VoiceUpload
}

/**
 * One push-to-talk note on the Circle thread.
 *
 * [file] is a **file name**, not a path: the app's `filesDir` moves between
 * installs and between users on the same phone, so an absolute path stored in
 * DataStore is a path that resolves to nothing after a restore. The directory
 * is `filesDir/voice`, and only the layer holding a `Context` may join the two.
 *
 * [waveform] is captured while recording rather than derived afterwards - the
 * app keeps only the encoded `.m4a` and never the raw PCM - so a note carries
 * its own picture from the instant it exists, before any upload.
 */
data class VoiceNote(
    val id: String = UUID.randomUUID().toString(),
    /** Whose thread this belongs to. Null means the primary wearer. */
    val wearerId: String? = null,
    /** True when a guardian recorded it, mirroring [QuickMessage.fromGuardian]. */
    val fromGuardian: Boolean = true,
    val authorName: String = "",
    /** File name under `filesDir/voice`. See the class note. */
    val file: String = "",
    val durationMs: Int = 0,
    /** Normalised 0..1 amplitudes, one per bar. */
    val waveform: List<Float> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val uploadState: VoiceUpload = VoiceUpload.LocalOnly,
    val listened: Boolean = false
)

// ============================================
// TELEMETRY
// ============================================

data class LiveSensorData(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val temperature: Float = 0f,
    val lightLevel: Int = 0,
    val batteryLevel: Int = 0,
    /** True once a real TELEMETRY payload has arrived this session. */
    val isRealData: Boolean = false
) {
    val magnitudeG: Float
        get() = kotlin.math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ)
}

/** One sample for the battery / link-quality sparklines. */
data class TelemetryPoint(
    val at: Long,
    val batteryPercent: Int,
    val rssiDbm: Int
)

// ============================================
// GEOFENCING
// ============================================

data class GeofenceZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lat: Double,
    val lon: Double,
    val radiusMeters: Float = 200f,
    val alertOnExit: Boolean = true,
    val alertOnEnter: Boolean = false,
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
)

// ============================================
// LED
// ============================================

/** Wire index must match the firmware `RGBPattern` enum order exactly. */
enum class LedPattern(val label: String, val wireIndex: Int) {
    TORCH("Torch", 0),
    RAINBOW("Rainbow", 1),
    CYBER("Cyber", 2),
    POLICE("Police", 3),
    FIRE("Fire", 4),
    OCEAN("Ocean", 5),
    PULSE("Pulse", 6)
}

// ============================================
// REMINDERS, CHECK-INS, JOURNEYS
// ============================================

enum class ReminderKind { MEDICATION, CHECK_IN, FAKE_CALL }

data class Reminder(
    val id: String = UUID.randomUUID().toString(),
    val kind: ReminderKind,
    val hour: Int = 9,
    val minute: Int = 0,
    /** For CHECK_IN, the interval instead of a wall-clock time. */
    val intervalMinutes: Int = 0,
    val enabled: Boolean = true,
    val label: String = "",
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
) {
    val requestCode: Int get() = id.hashCode()
}

/** A guardian asking "are you OK?" with a deadline behind it. */
data class CheckInRequest(
    val id: String = UUID.randomUUID().toString(),
    val sentAt: Long = System.currentTimeMillis(),
    val deadlineAt: Long,
    val answeredAt: Long? = null,
    val escalated: Boolean = false,
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
) {
    val isOpen: Boolean get() = answeredAt == null && !escalated
}

/** "Walk with me" — a timed journey that escalates if it is not ended. */
data class Journey(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val startedAt: Long = System.currentTimeMillis(),
    val etaAt: Long,
    val graceSeconds: Int = 60,
    val destinationLat: Double? = null,
    val destinationLon: Double? = null,
    val state: JourneyState = JourneyState.ACTIVE,
    /**
     * Which [Wearer] this belongs to, or null.
     *
     * Null is the honest value for every record written before the app knew
     * about more than one person, and for any record whose owner cannot be
     * resolved. A reader must treat null as "the primary wearer" rather than
     * as "nobody"; it is nullable rather than defaulted to a real id precisely
     * so that a guess never masquerades as a fact.
     */
    val wearerId: String? = null
)

enum class JourneyState { ACTIVE, ARRIVED, ESCALATED, CANCELLED }

// ============================================
// EMERGENCY SERVICES - offline, no network, no permission
// ============================================

data class EmergencyService(
    val number: String,
    val label: String,
    val blurb: String
)

/**
 * India public emergency numbers, held on-device.
 *
 * The deck promises the wearable "caches closest emergency services contacts";
 * the app half of that promise is this list, which needs no network and no
 * account. Every entry dials through ACTION_DIAL rather than ACTION_CALL —
 * pre-filling the dialer is the correct affordance for a real emergency line,
 * and it makes a mis-tap recoverable.
 */
val IndiaEmergencyServices = listOf(
    EmergencyService("112", "All emergencies", "ERSS - police, fire and ambulance on one number"),
    EmergencyService("108", "Ambulance", "Free emergency medical transport"),
    EmergencyService("100", "Police", "Direct police control room"),
    EmergencyService("101", "Fire", "Fire and rescue services"),
    EmergencyService("1091", "Women's helpline", "24-hour national helpline"),
    EmergencyService("1098", "Childline", "Help for children in distress"),
    EmergencyService("14567", "Senior citizens", "Elder helpline for abuse, health and rescue"),
    EmergencyService("1930", "Cyber crime", "Financial fraud and online crime reporting")
)
