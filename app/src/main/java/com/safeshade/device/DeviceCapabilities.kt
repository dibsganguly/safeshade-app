package com.safeshade.device

/**
 * What the app can and cannot change on the wearable.
 *
 * The firmware grew an on-device Settings menu with NVS persistence, and only
 * *some* of those settings also have a BLE write path. Eight of them have none
 * at all. An app that renders a switch for one of those eight looks like it is
 * working and does nothing whatsoever — the write has no characteristic to go
 * to, so there is not even a failed write to report.
 *
 * Rather than ship eight dead controls, the settings screen renders
 * [SyncCapability.DEVICE_ONLY] rows as read-only, with the exact menu path to
 * change them on the wearable. That is less satisfying and considerably more
 * honest.
 *
 * If firmware later adds EXT tags for these, moving a row to
 * [SyncCapability.SYNCED] is a one-line change here.
 */
enum class SyncCapability {
    /** The app can read and write this over BLE. */
    SYNCED,

    /** Exists only in the wearable's own menu. No BLE path in this firmware. */
    DEVICE_ONLY,

    /**
     * The wire format is designed and unit-tested on this side; the firmware
     * does not answer it yet.
     *
     * A third bucket rather than a boolean on [DeviceSetting], because the
     * difference matters at the call site: a [DEVICE_ONLY] row can be changed
     * by walking to the wearable and pressing buttons, and an
     * [AWAITING_FIRMWARE] row cannot be changed by anybody at all. Screens
     * render no control for these — the app must never draw an affordance whose
     * write has nowhere to go.
     *
     * The reason this ledger has to be static is itself a firmware fact: every
     * `EXT` write is acknowledged, *including unknown tags*, and the firmware
     * version is not exposed over BLE. So there is no probe that could
     * distinguish a tag the device honours from one it silently drops, and an
     * app that tried to discover this at runtime would conclude that every
     * feature works.
     */
    AWAITING_FIRMWARE
}

data class DeviceSetting(
    val key: String,
    val label: String,
    val capability: SyncCapability,
    /** Where to find it on the wearable, for the DEVICE_ONLY rows. */
    val deviceMenuPath: String? = null,
    /** How it crosses the link, for the SYNCED rows. Documentation, not dispatch. */
    val wire: String? = null
)

object DeviceCapabilities {

    /**
     * Settings the app genuinely controls.
     *
     * Note that SOS volume is synced but the general chime volume is not —
     * they are different firmware fields and only the first rides in the
     * SETTINGS payload. Conflating them would be an easy and invisible mistake.
     */
    val synced = listOf(
        DeviceSetting("fallSensitivity", "Fall sensitivity", SyncCapability.SYNCED, wire = "SETTINGS field 1"),
        DeviceSetting("sosVolume", "SOS siren volume", SyncCapability.SYNCED, wire = "SETTINGS field 2"),
        DeviceSetting("autoCall", "Auto-call on a fall", SyncCapability.SYNCED, wire = "SETTINGS field 3"),
        DeviceSetting("parentalControls", "Parental controls", SyncCapability.SYNCED, wire = "SETTINGS field 4"),
        DeviceSetting("smsFallback", "SMS fallback alert", SyncCapability.SYNCED, wire = "SETTINGS field 5"),
        DeviceSetting("deviceName", "Device name", SyncCapability.SYNCED, wire = "EXT DEVNAME"),
        DeviceSetting("mode", "Adaptive mode", SyncCapability.SYNCED, wire = "EXT MODE"),
        DeviceSetting("ledPattern", "Light pattern", SyncCapability.SYNCED, wire = "LED_CHAR index"),
        DeviceSetting("medicationTime", "Medication reminder", SyncCapability.SYNCED, wire = "EXT MED"),
        DeviceSetting("checkInInterval", "Check-in interval", SyncCapability.SYNCED, wire = "EXT CHECKIN"),
        DeviceSetting("quietHours", "Quiet hours window", SyncCapability.SYNCED, wire = "EXT QUIET"),
        DeviceSetting("smsAllowlist", "Message allowlist", SyncCapability.SYNCED, wire = "EXT SMSALLOW"),
        DeviceSetting("navigation", "Navigation target", SyncCapability.SYNCED, wire = "EXT NAV")
    )

    /**
     * The eight with no BLE write path in this firmware revision.
     *
     * Verified against `SafeShadev21.ino`: each of these is reachable from the
     * wearable's own Settings menu and persisted to NVS, but no characteristic
     * or EXT tag sets it.
     */
    val deviceOnly = listOf(
        DeviceSetting("ledsEnabled", "Lights on/off", SyncCapability.DEVICE_ONLY, "Settings › Lights"),
        DeviceSetting("displayContrast", "Screen contrast", SyncCapability.DEVICE_ONLY, "Settings › Display"),
        DeviceSetting("flipDisplay", "Flip screen", SyncCapability.DEVICE_ONLY, "Settings › Display"),
        DeviceSetting("homeTicker", "Home screen tips", SyncCapability.DEVICE_ONLY, "Settings › Display"),
        DeviceSetting("masterVolume", "Chime volume", SyncCapability.DEVICE_ONLY, "Settings › Sound"),
        DeviceSetting("bootChime", "Startup chime", SyncCapability.DEVICE_ONLY, "Settings › Sound"),
        DeviceSetting("notifChime", "Message chime", SyncCapability.DEVICE_ONLY, "Settings › Notifications"),
        DeviceSetting(
            "dndEnabled", "Do Not Disturb", SyncCapability.DEVICE_ONLY, "Settings › Notifications",
            wire = "the on/off toggle is device-only; only its hours ride on EXT QUIET"
        )
    )

    /**
     * What the app has designed and the wearable does not yet answer.
     *
     * Each entry names the wire form this side will speak the moment the other
     * side can hear it, so the format is settled and testable now rather than
     * invented under pressure during a firmware pass. [DeviceSetting.wire] is
     * documentation, not dispatch: nothing in the app sends any of these.
     *
     * Not part of [all], and that omission is deliberate — any screen that
     * iterates `all` would otherwise grow a control for a tag the firmware
     * ignores, which is the exact failure the whole file exists to prevent.
     */
    val awaitingFirmware = listOf(
        DeviceSetting(
            "sosRelay", "Relay an SOS raised on the phone",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "EXT SOSRELAY:<lat>,<lon>,<text> with ACK:SOSRELAY"
        ),
        DeviceSetting(
            "emergencyServices", "Nearest emergency services, cached on the device",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "EXT ERS: up to 4 entries kind:name:phoneDigits:distanceMetres joined by ';'"
        ),
        DeviceSetting(
            "firmwareVersion", "Firmware version over the link",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "EXT VER, payload empty; answered ACK:VER:<semver>. A bare ACK:VER is this " +
                "firmware acknowledging an unknown tag and reports no version"
        ),
        DeviceSetting(
            "voiceChunks", "Push-to-talk to and from the wearable",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "VOICE chunk path: binary [0x56][seq][total][length] header + 8 kHz ADPCM payload, per-chunk ack"
        ),
        DeviceSetting(
            "otaChunks", "Firmware update over the link",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "OTA:<seq>/<total>,<crc32hex>,<base64> chunks, CRC32 over the raw bytes; success " +
                "is a later EXT VER matching the release, never a chunk ack. No OTA partition exists yet"
        ),
        DeviceSetting(
            "vitals", "Heart rate, blood oxygen and skin temperature",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "TELEMETRY fields 7-9 hr,spo2,skinTempC; empty or '-' means no reading. No sensor is fitted"
        ),
        DeviceSetting(
            "mesh", "Finding another SafeShade nearby",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "MESH advertisement payload: <deviceId>,<role>,<batteryPercent>,<lastFixAge>, " +
                "role one of wearer|relay, empty numeric fields mean not reported"
        ),
        DeviceSetting(
            "batteryReading", "A measured battery level",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "TELEMETRY field 6; the firmware reports a counted-down number, not a reading"
        ),
        DeviceSetting(
            "lowBatteryAlert", "Telling a guardian the battery is low",
            SyncCapability.AWAITING_FIRMWARE,
            wire = "ALERT_CHAR notify LOW_BATTERY:<percent>; the firmware never notifies it"
        )
    )

    val all: List<DeviceSetting> get() = synced + deviceOnly

    /**
     * Tags the firmware never acknowledges.
     *
     * Awaiting an ack for any of these will always time out, so a caller that
     * shows a confirmation badge must not wait on one. Message and reply writes
     * are deliberately unacknowledged; `CMD_FIND` returns before the weather
     * handler's ack is reached.
     */
    val unacknowledged = setOf("MESSAGE", "REPLY", "CMD_FIND")
}
