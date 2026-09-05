package com.safeshade.debug

/**
 * A scripted state of the world for [FakeDeviceLink].
 *
 * Every BLE-dependent screen in this app is unreachable on an emulator: there
 * is no radio, so `connectionState` never leaves Disconnected and the connected,
 * tripped, ringing and live-telemetry layouts can only be reviewed by carrying
 * a wearable to the desk. That makes exactly the screens that matter most the
 * ones nobody looks at.
 *
 * These scenarios are the substitute. Each drives the fake link through a fixed
 * timeline, so a screenshot of any of them is reproducible rather than
 * dependent on what a real device happened to be doing.
 *
 * They exist only in the debug source set. Leak-proofing here is by *source
 * set*, not by a `BuildConfig.DEBUG` branch — a runtime check still compiles
 * the fake, its scripted alerts and its fabricated telemetry into the release
 * APK, where one mis-evaluated condition is a fall alert the user never had.
 */
enum class Scenario(val label: String, val blurb: String) {

    DISCONNECTED("Disconnected", "No device. The empty state every screen must handle."),

    SCANNING("Scanning", "Scan running, nothing found yet. Exercises the busy affordances."),

    CONNECTED_IDLE(
        "Connected",
        "Link up and Ready, no telemetry yet. The gap between Connected and Ready is scripted."
    ),

    LIVE_TELEMETRY(
        "Live telemetry",
        "~1 Hz accelerometer walk and a slowly draining battery, so sparklines have real shape."
    ),

    FALL_ALERT("Fall detected", "A fall two seconds after Ready, then a second one, to prove alerts are events."),

    SOS_ACTIVE("SOS", "An unrecognised ALERT_CHAR payload, which is how an SOS press arrives."),

    INCOMING_MESSAGE("Incoming reply", "A companion reply, then the same text again to exercise the dedupe."),

    LOW_BATTERY("Low battery", "Telemetry draining from 12% so the low-battery treatment is visible."),

    GEOFENCE_EXIT("Left a zone", "Ready link, for driving a zone exit from the geofence bus."),

    RINGING("Ringing", "ringDevice() called. Nothing is ever acknowledged, matching the firmware."),

    ACK_ALL("Acknowledge everything", "Ready, and every write is acked immediately. The happy path for pushAll().")
}
