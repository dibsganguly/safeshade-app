package com.safeshade.ui.screens.device

import com.safeshade.data.FallSensitivity
import com.safeshade.data.PersonaMode

import com.safeshade.ui.nav.Routes

/**
 * What each adaptive mode actually does, on the wearable and in the app.
 *
 * Sourced from the firmware rather than from the pitch deck wherever the two
 * disagree. Every number here cites the line in `SafeShadev21.ino` it was read
 * from, because the mode page is the one place the app explains the wearable's
 * behaviour in figures, and a figure that drifts from the firmware is worse
 * than no figure.
 *
 * Fall detection: per loop the firmware sums the three
 * accelerometer axes, `|ax| + |ay| + |az|`, in raw LSB at 16384 LSB per g, and
 * trips when the sum exceeds a threshold picked by the sensitivity setting —
 * 70000 / 50000 / 35000 for Low / Medium / High (5841–5845) — raised to the
 * mode's own floor where the mode has one (5850). Bike and Helmet additionally
 * require the summed gyroscope reading to exceed a rotation threshold
 * (5855–5863). Pet turns fall detection off; Elderly adds a stillness check
 * after an impact. The pitch deck's "3-layer freefall + impact + stillness" is
 * a promise: `FREEFALL_THRESH` is defined and never read (311).
 */
data class ModeFacts(
    val mode: PersonaMode,
    /** One line on who the profile is for. */
    val forWhom: String,
    /** The deck's own words for what the algorithm does differently. */
    val algorithm: String,
    /** The deck's own words for how the wearable's screen changes. */
    val uiChanges: String,
    /** Whether the wearable listens for falls at all in this mode. */
    val fallDetection: Boolean,
    /** The mode's floor on the impact sum, in raw LSB, or null where the sensitivity setting is used as is. */
    val impactFloorLsb: Int?,
    /** The gyroscope sum that must also be exceeded, in raw LSB, or null where rotation is not checked. */
    val rotationLsb: Int?,
    /** Elderly only: an impact arms a stillness monitor that escalates if the wearer does not move. */
    val stillnessCheck: Boolean,
    /** The wearable's screen cycle, in order, as the rotary shows them. */
    val screens: List<String>,
    /** How the home screen differs in this mode. */
    val homeScreen: String,
    /** What the LEDs do on their own in this mode. */
    val lights: String,
    /** How often the wearable asks the gateway for a position, in milliseconds. */
    val gatewayPollMs: Int,
    /** The deck's priority features for this mode, each linked to its screen where one exists. */
    val features: List<ModeFeature>
) {
    /**
     * The impact the wearable trips at for a given sensitivity, in g.
     *
     * The sensitivity picks a threshold; the mode may raise it. Pet returns
     * null because there is no threshold to state.
     */
    fun impactG(sensitivity: FallSensitivity): Float? {
        if (!fallDetection) return null
        val fromSensitivity = when (sensitivity) {
            FallSensitivity.LOW -> 70000
            FallSensitivity.MEDIUM -> 50000
            FallSensitivity.HIGH -> 35000
        }
        val lsb = maxOf(fromSensitivity, impactFloorLsb ?: 0)
        return lsb / 16384f
    }

    /** The rotation the wearable also requires, in degrees per second, or null. */
    val rotationDps: Float?
        get() = rotationLsb?.let { it / 131f }

    /** What the wearer loses on the device while this mode runs. */
    val sealedAway: List<String>
        get() = if (mode.isGuardianLocked) listOf(
            "Mode select",
            "The whole Safety menu",
            "Clear allowlist",
            "Check-in interval"
        ) else emptyList()

    companion object {
        fun of(mode: PersonaMode): ModeFacts = all.first { it.mode == mode }

        private const val POLL_DEFAULT_MS = 4000
        private val STANDARD_SCREENS = listOf("Home", "Health", "Message", "GPS", "Weather")

        /** Every mode, in the enum's order. */
        val all: List<ModeFacts> = listOf(
            ModeFacts(
                mode = PersonaMode.AUTO,
                forWhom = "For anyone who would rather not choose. Runs the Backpack profile.",
                algorithm = "Balanced thresholds, every feature on",
                uiChanges = "Standard screens with the ticker",
                fallDetection = true,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = false,
                screens = STANDARD_SCREENS,
                homeScreen = "Clock, weather and the ticker, with Shady.",
                lights = "The chosen light pattern, nothing automatic.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Fall detection", Routes.SAFETY_FALL),
                    ModeFeature("Guardian messages", Routes.CIRCLE_THREAD),
                    ModeFeature("Weather and location on the device", Routes.BOARD)
                )
            ),
            ModeFacts(
                mode = PersonaMode.ELDERLY,
                forWhom = "A parent or grandparent living alone. A fall at night is the fear.",
                algorithm = "Most sensitive fall thresholds, with a stillness check after an impact",
                uiChanges = "Large fonts, simplified screens",
                fallDetection = true,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = true,
                screens = listOf("Home", "Health", "GPS", "Medication"),
                homeScreen = "A big centred clock and larger Medical ID text.",
                lights = "A warm, wide path light comes on by itself in the dark.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Fall detection", Routes.SAFETY_FALL),
                    ModeFeature("Medication reminders", Routes.DEVICE_REMINDERS),
                    ModeFeature("Night bathroom lighting", Routes.DEVICE_LIGHTS)
                )
            ),
            ModeFacts(
                mode = PersonaMode.KIDS,
                forWhom = "A child on the way to school. Where they are matters most.",
                algorithm = "Location first: the wearable asks for a position twice as often",
                uiChanges = "Safe-zone screen, parental lock",
                fallDetection = true,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = false,
                screens = listOf("Home", "Health", "Message", "GPS", "Safe zone", "Weather"),
                homeScreen = "Standard, with a Safe zone screen in the cycle.",
                lights = "A soft rainbow shimmer while idle.",
                gatewayPollMs = 2000,
                features = listOf(
                    ModeFeature("Geofencing", Routes.CIRCLE_ZONES),
                    ModeFeature("Safe zone alerts", Routes.CIRCLE_ZONES),
                    ModeFeature("Stranger danger SOS", Routes.CIRCLE_THREAD)
                )
            ),
            ModeFacts(
                mode = PersonaMode.BIKE,
                forWhom = "A cyclist. Road vibration is not a fall; a crash is.",
                algorithm = "Vibration filtering: an impact only counts with a rotation as well",
                uiChanges = "Navigation display, ride stats",
                fallDetection = true,
                impactFloorLsb = 55000,
                rotationLsb = 20000,
                stillnessCheck = false,
                screens = listOf("Home", "Health", "GPS", "Message", "Ride stats"),
                homeScreen = "Standard, with distance and bearing to a destination on the GPS screen.",
                lights = "The rear strip strobes as a brake light.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Turn-by-turn", null),
                    ModeFeature("Brake light", Routes.DEVICE_LIGHTS),
                    ModeFeature("Ride tracking", Routes.CIRCLE_JOURNEY)
                )
            ),
            ModeFacts(
                mode = PersonaMode.PET,
                forWhom = "A dog or cat. A tumble is play; being lost is the emergency.",
                algorithm = "Activity patterns; fall detection off",
                uiChanges = "Owner info always displayed",
                fallDetection = false,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = false,
                screens = listOf("Home", "Health", "GPS", "Activity"),
                homeScreen = "The owner's name and emergency number instead of the clock.",
                lights = "The chosen light pattern, nothing automatic.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Virtual leash", Routes.DEVICE_LOCATE),
                    ModeFeature("Activity tracking", Routes.DEVICE_TELEMETRY),
                    ModeFeature("Lost pet mode", Routes.DEVICE_LOCATE)
                )
            ),
            ModeFacts(
                mode = PersonaMode.HELMET,
                forWhom = "A worker or rider in a helmet. Only a hard, fast impact counts.",
                algorithm = "High-speed impact only, with a rotation check and a two-step confirmation",
                uiChanges = "Minimal distraction: two screens",
                fallDetection = true,
                impactFloorLsb = 70000,
                rotationLsb = 25000,
                stillnessCheck = false,
                screens = listOf("Home", "Health", "Impact log"),
                homeScreen = "No mascot on the home screen; the Lights menu is hidden.",
                lights = "Lights are off the menu in this mode.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Concussion alerts", Routes.SAFETY_TRIPS),
                    ModeFeature("Worker check-ins", Routes.CIRCLE_CHECKIN)
                )
            ),
            ModeFacts(
                mode = PersonaMode.WRIST,
                forWhom = "Everyday wear on the wrist, health screens up front.",
                algorithm = "Continuous health monitoring",
                uiChanges = "Watch-face style home screen",
                fallDetection = true,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = false,
                screens = listOf("Home", "Health", "GPS", "Message", "Vitals"),
                homeScreen = "A digital watch face with the pulse beside the time.",
                lights = "The chosen light pattern, nothing automatic.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Heart rate and SpO₂", null),
                    ModeFeature("Sleep tracking", null),
                    ModeFeature("Fitness", Routes.DEVICE_TELEMETRY)
                )
            ),
            ModeFacts(
                mode = PersonaMode.BACKPACK,
                forWhom = "A commuter. Everything on, nothing emphasised.",
                algorithm = "Balanced thresholds, every feature on",
                uiChanges = "Full navigation, the standard screens",
                fallDetection = true,
                impactFloorLsb = null,
                rotationLsb = null,
                stillnessCheck = false,
                screens = STANDARD_SCREENS,
                homeScreen = "Clock, weather and the ticker, with Shady.",
                lights = "The chosen light pattern, nothing automatic.",
                gatewayPollMs = POLL_DEFAULT_MS,
                features = listOf(
                    ModeFeature("Commuter safety", Routes.SAFETY),
                    ModeFeature("Full navigation", null),
                    ModeFeature("Guardian messages", Routes.CIRCLE_THREAD)
                )
            )
        )
    }
}

/** One of the deck's priority features for a mode, and the screen that carries it, if any yet. */
data class ModeFeature(val name: String, val route: String?)
