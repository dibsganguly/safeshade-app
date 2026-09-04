/**
 * SafeShade - Universal Safety Companion
 *
 * Models.kt
 *
 * Contains all data classes and enums used throughout the application.
 * These models represent the state and configuration of various features.
 *
 * @author SafeShade Team
 * @version 2.1.0
 */

package com.safeshade.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.*

// ============================================
// WEATHER & LOCATION
// ============================================

/**
 * Represents the current weather UI state.
 *
 * @property rainChance Probability of rain (0-100%)
 * @property uvIndex Current UV index
 * @property humidity Current humidity percentage
 * @property temp Current temperature in Celsius
 * @property isLoaded Whether weather data has been fetched
 * @property lastSyncTime Time of last successful sync (HH:mm format)
 */
data class WeatherUiState(
    val rainChance: Int = 0,
    val uvIndex: Float = 0f,
    val humidity: Float = 0f,
    val temp: Float = 0f,
    val isLoaded: Boolean = false,
    val lastSyncTime: String = "--:--"
)

/**
 * Represents the current location state.
 *
 * @property lat Latitude coordinate
 * @property lon Longitude coordinate
 * @property locationName Human-readable location name
 * @property locality City or locality name
 * @property altitude Altitude in meters
 * @property isValid Whether location data is valid
 */
data class LocationState(
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val locationName: String = "",
    val locality: String = "",
    val altitude: Int = 0,
    val isValid: Boolean = false
)

// ============================================
// MEDICAL & HEALTH
// ============================================

/**
 * Medical ID information displayed on device during emergencies.
 *
 * UPDATED: New default values as per requirements
 *
 * @property bloodType User's blood type (e.g., "O+", "A-")
 * @property emergencyContact Primary emergency phone number
 * @property contactName Name of emergency contact
 * @property allergies Known allergies (comma-separated)
 * @property medicalNotes Additional medical information
 * @property age User's age
 */
data class MedicalId(
    val bloodType: String = "AB+",
    val emergencyContact: String = "+91 89173 60065",
    val contactName: String = "Moumita Ganguly (Mother)",
    val allergies: String = "Penicillin, Peanuts",
    val medicalNotes: String = "Diabetic - Type 2, Anxiety",
    val age: Int = 21
)

// ============================================
// DEVICE CONFIGURATION
// ============================================

/**
 * SafeShade device settings and identification.
 *
 * @property id Unique device identifier
 * @property name User-defined device name
 * @property iconType Visual icon representing device usage
 * @property primaryUserName Name of the primary user
 * @property isPrimary Whether this is the primary paired device
 */
data class DeviceSettings(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "SafeShade S1",
    val iconType: DeviceIconType = DeviceIconType.UMBRELLA,
    val primaryUserName: String = "User",
    val isPrimary: Boolean = true
)

/**
 * Available device icon types for visual customization.
 * Each type represents a common use case or mounting location.
 *
 * UPDATED: Changed icons as per requirements:
 * - Watch -> FrontHand (Fist)
 * - Pendant -> Diamond (Locket)
 * - Hat -> School (Cap)
 * - Cane -> Elderly
 *
 * @property icon Material icon vector
 * @property label Human-readable label
 */
enum class DeviceIconType(val icon: ImageVector, val label: String) {
    UMBRELLA(Icons.Rounded.Umbrella, "Umbrella"),
    WATCH(Icons.Rounded.FrontHand, "Wristband"),
    BACKPACK(Icons.Rounded.Backpack, "Backpack"),
    BIKE(Icons.Rounded.DirectionsBike, "Bicycle"),
    PENDANT(Icons.Rounded.Diamond, "Locket"),
    HAT(Icons.Rounded.School, "Cap"),
    CANE(Icons.Rounded.Elderly, "Cane"),
    COLLAR(Icons.Rounded.Pets, "Pet Collar")
}

// ============================================
// ADAPTIVE MODES (context-aware persona profiles)
// ============================================

/**
 * The "7 Adaptive Modes" from the pitch deck, approximated as an
 * app-buildable behavioral profile: each mode auto-adjusts fall-detection
 * sensitivity and, for modes aimed at less tech-comfortable wearers,
 * switches the UI to a simplified large-font layout.
 *
 * @property defaultFallSensitivity Sensitivity preset applied on mode switch
 * @property simplifiedUi Whether screens should render in large-font, reduced-density layout
 * @property matchingDeviceIcon The DeviceIconType this mode is visually paired with
 * @property accentColor Distinct per-mode brand color, used for its chip/badge/banner everywhere
 *   the mode is shown (mode picker, Home screen banner) so each mode reads as visually distinct
 *   rather than sharing one flat accent.
 */
enum class PersonaMode(
    val label: String,
    val icon: ImageVector,
    val description: String,
    val defaultFallSensitivity: FallSensitivity,
    val simplifiedUi: Boolean,
    val matchingDeviceIcon: DeviceIconType,
    val accentColor: Color
) {
    ELDERLY(
        "Elderly", Icons.Rounded.Elderly,
        "Large-font UI, sensitive fall detection",
        FallSensitivity.HIGH, simplifiedUi = true, matchingDeviceIcon = DeviceIconType.CANE,
        accentColor = Color(0xFF0984E3)
    ),
    KIDS(
        "Kids", Icons.Rounded.ChildCare,
        "Safe-zone alerts, colourful simplified UI",
        FallSensitivity.MEDIUM, simplifiedUi = true, matchingDeviceIcon = DeviceIconType.BACKPACK,
        accentColor = Color(0xFFFF9F1C)
    ),
    BIKE(
        "Bike", Icons.Rounded.DirectionsBike,
        "Crash-tuned detection for cycling",
        FallSensitivity.HIGH, simplifiedUi = false, matchingDeviceIcon = DeviceIconType.BIKE,
        accentColor = Color(0xFF00B894)
    ),
    PET(
        "Pet", Icons.Rounded.Pets,
        "Activity tracking, fall detection off",
        FallSensitivity.LOW, simplifiedUi = false, matchingDeviceIcon = DeviceIconType.COLLAR,
        accentColor = Color(0xFFE84393)
    ),
    BACKPACK(
        "Backpack", Icons.Rounded.Backpack,
        "Balanced default profile",
        FallSensitivity.MEDIUM, simplifiedUi = false, matchingDeviceIcon = DeviceIconType.BACKPACK,
        accentColor = Color(0xFF6C5CE7)
    ),
    HELMET(
        "Helmet", Icons.Rounded.School,
        "Higher-impact threshold for headwear",
        FallSensitivity.LOW, simplifiedUi = false, matchingDeviceIcon = DeviceIconType.HAT,
        accentColor = Color(0xFFFF7675)
    ),
    WRIST(
        "Wrist", Icons.Rounded.FrontHand,
        "Everyday wristband profile",
        FallSensitivity.MEDIUM, simplifiedUi = false, matchingDeviceIcon = DeviceIconType.WATCH,
        accentColor = Color(0xFF00CEC9)
    )
}

// ============================================
// SAFETY & ALERTS
// ============================================

/**
 * Represents a recorded fall alert event.
 *
 * @property id Unique event identifier
 * @property timestamp Unix timestamp when event occurred
 * @property eventType Type of event (e.g., "Fall detected", "Impact detected")
 * @property action Action taken (e.g., "Dismissed by user", "Auto-dismissed")
 * @property wasEmergencyContacted Whether emergency contact was notified
 * @property location Guardian phone's last-known location at the moment the event was logged
 *   (from the app's [LocationState], not a live device GPS fix - null if no fix was available yet)
 * @property note A short real-data snapshot captured at event time (e.g. from [LiveSensorData]) -
 *   null for events logged before this field existed
 */
data class FallAlertEvent(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String = "Fall detected",
    val action: String = "Auto-dismissed",
    val wasEmergencyContacted: Boolean = false,
    val location: String? = null,
    val note: String? = null
)

/**
 * Safety-related settings for the app and device.
 *
 * UPDATED: New default emergency contacts as per requirements
 *
 * @property parentalControlsEnabled Whether parental mode switching requires PIN
 * @property parentalPin 4-digit PIN for parental controls
 * @property autoCallEmergency Auto-call emergency contact after fall detection
 * @property fallSensitivity Sensitivity level for fall detection
 * @property emergencyContacts List of emergency contacts
 * @property sosVolumeLevel SOS alarm volume (0.0-1.0)
 * @property smsFallbackEnabled Whether a fallback SMS alert is sent to the primary emergency
 *   contact alongside/instead of the call when a fall is confirmed (requires SEND_SMS)
 */
data class SafetySettings(
    val parentalControlsEnabled: Boolean = false,
    val parentalPin: String = "1234",
    val autoCallEmergency: Boolean = true,
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM,
    val emergencyContacts: List<EmergencyContact> = listOf(
        EmergencyContact("Maa", "+91 89173 60065", true),
        EmergencyContact("Dr. Raj Sarkar", "+91 82498 23741", false)
    ),
    val sosVolumeLevel: Float = 0.8f,
    val smsFallbackEnabled: Boolean = false
)

/**
 * Fall detection sensitivity levels.
 *
 * @property label Short display label
 * @property description Detailed description for users
 */
enum class FallSensitivity(val label: String, val description: String) {
    LOW("Low", "Only severe falls"),
    MEDIUM("Medium", "Recommended"),
    HIGH("High", "Most sensitive")
}

/**
 * Emergency contact information.
 *
 * @property name Contact's name
 * @property phone Contact's phone number
 * @property isPrimary Whether this is the primary contact (called first)
 */
data class EmergencyContact(
    val name: String,
    val phone: String,
    val isPrimary: Boolean = false
)

// ============================================
// MESSAGING
// ============================================

/**
 * Represents a quick message between Guardian and Companion.
 *
 * @property id Unique message identifier
 * @property text Message content
 * @property fromGuardian True if sent by Guardian, false if from Companion/device
 * @property timestamp Unix timestamp when message was sent
 * @property replied Whether the message has been replied to
 * @property replyText The reply text (if replied)
 */
data class QuickMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val fromGuardian: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val replied: Boolean = false,
    val replyText: String? = null
)

// ============================================
// SENSOR DATA
// ============================================

/**
 * Live sensor data from the device (simulated in current version).
 *
 * @property accelX Accelerometer X-axis reading in G
 * @property accelY Accelerometer Y-axis reading in G
 * @property accelZ Accelerometer Z-axis reading in G
 * @property temperature Device temperature in Celsius
 * @property lightLevel Ambient light level (0-100%)
 * @property batteryLevel Battery percentage (0-100%)
 */
data class LiveSensorData(
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val temperature: Float = 24.5f,
    val lightLevel: Int = 78,
    val batteryLevel: Int = 85,
    /** True once at least one real BLE telemetry payload has been received for this session. */
    val isRealData: Boolean = false
)

// ============================================
// GEOFENCING
// ============================================

/**
 * A Guardian-defined safe zone. Alerts fire on enter/exit via
 * GeofenceManager (Android GeofencingClient), independent of the BLE link.
 *
 * @property radiusMeters Zone radius in meters
 */
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
// REMOTE LED CONTROL
// ============================================

/**
 * Remote-controllable LED patterns for the device's WS2812B ring, sent over
 * LED_CHAR_UUID as the pattern's numeric index. Mirrors firmware's own
 * RGBPattern enum (SafeShadev21.ino) exactly — these are the same 7
 * patterns already reachable via the device's physical button, just now
 * also reachable remotely from the app.
 */
enum class LedPattern(val label: String, val wireIndex: Int) {
    TORCH("Torch", 0),
    RAINBOW("Rainbow", 1),
    CYBER("Cyber", 2),
    POLICE("Police", 3),
    FIRE("Fire", 4),
    OCEAN("Ocean", 5),
    PULSE("Pulse", 6)
}
