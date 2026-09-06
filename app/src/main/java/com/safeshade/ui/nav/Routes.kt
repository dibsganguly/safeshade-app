package com.safeshade.ui.nav

/**
 * Every destination in the app, in one place.
 *
 * Plain string routes with string/int arguments rather than type-safe
 * `@Serializable` routes: the latter needs the kotlinx.serialization Gradle
 * *plugin*, and adding a plugin is the one category of change this toolchain
 * (AGP 9.1.1 with several compatibility escape hatches already set in
 * gradle.properties) can punish without a workaround. Adding libraries is
 * cheap; adding plugins is not.
 *
 * This file is a merge hotspot. Screens are built by separate agents in
 * parallel and none of them may edit it — a route added in two lanes at once
 * is a conflict nobody notices until the graph silently loses a destination.
 */
object Routes {

    // ============================================
    // Onboarding — a separate graph, not a screen
    // ============================================
    const val ONBOARDING_GRAPH = "onboarding"
    const val ONBOARDING_WELCOME = "onboarding/welcome"

    /**
     * The fork the whole app hangs off: who is this device for?
     *
     * Answering it sets [com.safeshade.data.UserRole], which decides whether
     * every subsequent string reads "you are wearing this" or "someone you are
     * responsible for is wearing this". Getting this wrong makes the app feel
     * addressed to the wrong person on every screen, so it is asked first and
     * asked plainly.
     */
    const val ONBOARDING_ROLE = "onboarding/role"
    const val ONBOARDING_WEARER = "onboarding/wearer"
    const val ONBOARDING_PERMISSIONS = "onboarding/permissions"
    const val ONBOARDING_RELIABILITY = "onboarding/reliability"
    const val ONBOARDING_PAIR = "onboarding/pair"
    const val ONBOARDING_MEDICAL = "onboarding/medical"

    // ============================================
    // Main graph
    // ============================================
    const val MAIN_GRAPH = "main"

    /** Bottom destination 1 — the panel. */
    const val BOARD = "board"
    const val BOARD_LINK = "board/link"

    /** Bottom destination 2 — the people on either end of the link. */
    const val CIRCLE = "circle"
    const val CIRCLE_THREAD = "circle/thread"
    const val CIRCLE_ZONES = "circle/zones"
    const val CIRCLE_ZONE_EDIT = "circle/zones/edit"
    const val CIRCLE_ZONE_PICK = "circle/zones/pick"
    const val CIRCLE_CHECKIN = "circle/checkin"
    const val CIRCLE_JOURNEY = "circle/journey"
    const val CIRCLE_SIM = "circle/sim"

    /** Bottom destination 3 — everything that fires in an emergency. */
    const val SAFETY = "safety"
    const val SAFETY_FALL = "safety/fall"
    const val SAFETY_CONTACTS = "safety/contacts"
    const val SAFETY_MEDICAL = "safety/medical"
    const val SAFETY_MEDICAL_CARD = "safety/medical/card"
    const val SAFETY_SERVICES = "safety/services"
    const val SAFETY_TRIPS = "safety/trips"
    const val SAFETY_TRIP_DETAIL = "safety/trips/detail"
    const val SAFETY_SILENT = "safety/silent"

    /** Bottom destination 4 — the wearable itself. */
    const val DEVICE = "device"
    const val DEVICE_MODE = "device/mode"
    const val DEVICE_MODE_DETAIL = "device/mode/detail"
    const val DEVICE_MODE_COMPARE = "device/mode/compare"
    const val DEVICE_SETTINGS = "device/settings"
    const val DEVICE_LOCATE = "device/locate"
    const val DEVICE_TELEMETRY = "device/telemetry"
    const val DEVICE_LIGHTS = "device/lights"
    const val DEVICE_PAIRED = "device/paired"
    const val DEVICE_REMINDERS = "device/reminders"

    /** Reached from the Board's top bar, not from the bottom bar. */
    const val SETTINGS = "settings"
    const val SETTINGS_ROLE = "settings/role"
    const val SETTINGS_RELIABILITY = "settings/reliability"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_DEVELOPER = "settings/developer"

    /**
     * The component kit, reachable only from the developer screen.
     *
     * It had no route at all, and the handler that was supposed to open it was
     * an empty lambda — so the one screen the project's own notes call "the
     * reference to check before inventing a seventh kind of card" could not be
     * opened. A debug-only destination is still a destination.
     */
    const val SETTINGS_KIT = "settings/kit"

    // ============================================
    // Arguments
    // ============================================
    object Args {
        const val ZONE_ID = "zoneId"
        const val TRIP_ID = "tripId"
        const val CONTACT_INDEX = "contactIndex"
        const val MODE = "mode"
        const val LAT = "lat"
        const val LON = "lon"
        const val RADIUS = "radius"
    }

    fun zoneEdit(zoneId: String?): String =
        if (zoneId == null) "$CIRCLE_ZONE_EDIT?${Args.ZONE_ID}=" else "$CIRCLE_ZONE_EDIT?${Args.ZONE_ID}=$zoneId"

    // `zonePick(lat, lon, radius)` used to live here and was never called: the
    // picker is registered as an argument-less destination and navigated to by
    // the bare constant. A builder nobody uses is a trap for whoever tries it
    // next, because its arguments would have been silently dropped.

    fun tripDetail(tripId: String): String = "$SAFETY_TRIP_DETAIL/$tripId"

    /** One profile's page. The argument is the enum name, which is also the wire name. */
    fun modeDetail(mode: com.safeshade.data.PersonaMode): String = "$DEVICE_MODE_DETAIL/${mode.name}"

    fun contactEdit(index: Int): String = "$SAFETY_CONTACTS/edit?${Args.CONTACT_INDEX}=$index"
}

/**
 * The four bottom destinations.
 *
 * `DEVICE` stays a top-level destination rather than folding under the Board.
 * For a guardian the wearable is a thing they manage in its own right — its
 * mode, its settings, where it physically is — and burying that a level down
 * makes the most common maintenance task a two-tap journey.
 */
enum class BottomDestination(
    val route: String,
    val guardianLabel: String,
    val companionLabel: String
) {
    BOARD(Routes.BOARD, "Board", "Board"),

    /**
     * Labelled by role. A guardian is looking at the person they watch over;
     * a wearer is looking at the guardian who watches over them. Same routes,
     * same content, opposite point of view.
     */
    CIRCLE(Routes.CIRCLE, "Circle", "Guardian"),

    SAFETY(Routes.SAFETY, "Safety", "Safety"),
    DEVICE(Routes.DEVICE, "Device", "Device")
}
