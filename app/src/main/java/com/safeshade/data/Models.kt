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
    val fallCountdownSeconds: Int = 30
) {
    val primaryContact: EmergencyContact?
        get() = emergencyContacts.firstOrNull { it.isPrimary } ?: emergencyContacts.firstOrNull()
}

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
    val note: String? = null
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
    val channel: MessageChannel = MessageChannel.BLE
)

enum class MessageChannel { BLE, SMS }

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
    val alertOnEnter: Boolean = false
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
    val label: String = ""
) {
    val requestCode: Int get() = id.hashCode()
}

/** A guardian asking "are you OK?" with a deadline behind it. */
data class CheckInRequest(
    val id: String = UUID.randomUUID().toString(),
    val sentAt: Long = System.currentTimeMillis(),
    val deadlineAt: Long,
    val answeredAt: Long? = null,
    val escalated: Boolean = false
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
    val state: JourneyState = JourneyState.ACTIVE
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
