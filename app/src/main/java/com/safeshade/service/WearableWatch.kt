package com.safeshade.service

import android.content.Context
import com.safeshade.alerts.AlertNotifier
import com.safeshade.data.LiveSensorData
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.WearableWatchMarks
import com.safeshade.data.resolveWearerForDevice
import com.safeshade.device.ConnectionState
import com.safeshade.repo.DeviceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the guardian is told about the wearable itself, rather than about a
 * fall: that it has been out of reach for too long, and that its battery is
 * low.
 *
 * ### Both of these are notifications and nothing else
 *
 * No SMS, no call, no siren, no full-screen intent. A wearable on a charger in
 * another room produces exactly the same signal as one left on a bus, and an
 * app that texted an emergency contact about a flat battery would teach every
 * contact in the circle to ignore its messages - which costs a life the day a
 * real one arrives. These go to the [Channels.WATCH][com.safeshade.alerts.Channels.WATCH]
 * channel with the zone crossings and the missed check-ins, where they belong.
 *
 * ### Why there is a ticker
 *
 * "Disconnected for two hours" is not an event any flow emits. The link goes
 * quiet at the start of the outage and stays quiet; the threshold is crossed by
 * the clock, with nothing happening. So this polls, on the application scope,
 * at the same order of granularity as the journey and check-in escalations -
 * minutes of lateness on a two-hour window is not a meaningful error.
 *
 * The known limit of that: it holds only while the process does. `LinkService`
 * keeps the process alive while it is watching a device, so in the normal case
 * the ticker is running; a process reclaimed during an outage raises the notice
 * when the app next starts rather than at the exact minute. The marks are on
 * disk, so it is raised once either way.
 *
 * ### On the battery figure
 *
 * The wearable's firmware reports a synthetic battery level. That is recorded
 * in the handoff notes; nothing here and nothing in the UI annotates it, and
 * the threshold is applied to whatever the device says exactly as it would be
 * to a measured one.
 */
class WearableWatch(
    private val appContext: Context,
    private val prefs: SafeShadePreferences,
    private val device: DeviceRepository,
    private val scope: CoroutineScope,
    /** Injected so the decision can be tested without a notification manager. */
    private val notifyOffline: (wearerName: String, minutes: Int) -> Unit = { name, minutes ->
        AlertNotifier.showWearableOffline(appContext, name, minutes)
    },
    private val notifyLowBattery: (wearerName: String, percent: Int) -> Unit = { name, percent ->
        AlertNotifier.showWearableLowBattery(appContext, name, percent)
    }
) {

    private val _state = MutableStateFlow(WatchState())

    /** What the watch currently believes, for the plate that reports it. */
    val state: StateFlow<WatchState> = _state.asStateFlow()

    /** Serialises the read-modify-write of the marks. */
    private val lock = Mutex()

    /** Starts the ticker. Idempotent in practice: called once, from `AppContainer`. */
    fun start() {
        scope.launch {
            while (isActive) {
                runCatching { tick(System.currentTimeMillis()) }
                delay(TICK_MS)
            }
        }
    }

    /**
     * One pass: read the world, decide, act, remember.
     *
     * `internal` and taking [now] so a test can drive it, though the decision
     * itself lives in [decideWatchAlerts], which is pure and is where the rules
     * are actually tested.
     */
    internal suspend fun tick(now: Long) {
        val settings = prefs.safetySettings.first()
        val connected = device.connection.value.isUsable
        val address = device.deviceAddress.value
        val telemetry: LiveSensorData = device.telemetry.value

        val decision = lock.withLock {
            val marks = prefs.wearableWatchMarks.first()

            val decision = decideWatchAlerts(
                now = now,
                connected = connected,
                marks = marks,
                offlineAlertMinutes = settings.offlineAlertMinutes,
                lowBatteryPercent = settings.lowBatteryPercent,
                batteryLevel = telemetry.batteryLevel,
                isRealData = telemetry.isRealData
            )

            val nextMarks = marks.copy(
                lastConnectedAt = if (connected) now else marks.lastConnectedAt,
                lastAddress = if (connected && address.isNotBlank()) address else marks.lastAddress,
                offlineAlerted = when {
                    connected -> false
                    decision.raiseOffline -> true
                    else -> marks.offlineAlerted
                },
                lowBatteryAlerted = when {
                    decision.clearLowBattery -> false
                    decision.raiseLowBattery -> true
                    else -> marks.lowBatteryAlerted
                }
            )
            if (nextMarks != marks) prefs.setWearableWatchMarks(nextMarks)

            _state.value = WatchState(
                offlineSince = if (connected) null else nextMarks.lastConnectedAt,
                offlineAlerted = nextMarks.offlineAlerted,
                lowBatteryAlerted = nextMarks.lowBatteryAlerted
            )
            decision to nextMarks
        }

        val (result, marks) = decision
        if (!result.raiseOffline && !result.raiseLowBattery) return

        // Resolved by address, not by selection: the notice has to name the
        // person whose wearable this is, and the person on screen is a
        // different question - handoff7 section 6 item 2.
        val name = wearerNameFor(if (connected) address else marks.lastAddress)

        if (result.raiseOffline) {
            notifyOffline(name, settings.offlineAlertMinutes)
        }
        if (result.raiseLowBattery) {
            notifyLowBattery(name, telemetry.batteryLevel)
        }
    }

    private suspend fun wearerNameFor(address: String): String {
        val wearer = resolveWearerForDevice(
            wearers = prefs.wearers.first(),
            address = address,
            selectedWearerId = prefs.selectedWearerId.first()
        )
        // "The wearable" rather than a blank or a placeholder name: a sentence
        // reading "'s wearable has been out of reach" is worse than one that
        // simply does not name anybody.
        return wearer?.name?.takeIf { it.isNotBlank() } ?: ""
    }

    private companion object {
        /**
         * A minute. The windows this watches are measured in tens of minutes
         * and in percent, so a minute of lateness is invisible, and a minute of
         * sleep costs nothing on a process that is already resident for the
         * link.
         */
        const val TICK_MS = 60_000L
    }
}

/** What the watch believes right now, for the UI. */
data class WatchState(
    /** When the wearable was last on the link, while it is off it. Null when connected. */
    val offlineSince: Long? = null,
    val offlineAlerted: Boolean = false,
    val lowBatteryAlerted: Boolean = false
)

/** What one pass of the watch decided to do. */
data class WatchDecision(
    val raiseOffline: Boolean = false,
    val raiseLowBattery: Boolean = false,
    /** The battery came back up past the threshold; the next dip may alert again. */
    val clearLowBattery: Boolean = false
)

/**
 * The whole rule set, as a pure function.
 *
 * Every condition that can suppress a notification is here rather than spread
 * across the caller, because the failures that matter in this feature are all
 * of the form "it fired when it should not have":
 *
 *  - **Zero is off**, for both thresholds, and off means silent.
 *  - **A phone that has never connected to a wearable has no outage.** A null
 *    `lastConnectedAt` is not the epoch; treating it as one would mean every
 *    fresh install announced a fifty-year absence on first launch.
 *  - **Once per outage, once per discharge.** The marks are the memory, and
 *    they are persisted, so a process restart mid-outage does not re-announce.
 *  - **The battery rule needs real telemetry.** `isRealData` is false until an
 *    actual payload has arrived, and the default `batteryLevel` is 0 - which
 *    is below every threshold. Without this check, connecting to anything at
 *    all would report a flat battery.
 *  - **The low-battery latch clears at threshold + [RECOVERY_MARGIN]**, not at
 *    the threshold itself, so a level hovering on the boundary does not
 *    announce itself once a minute.
 */
internal fun decideWatchAlerts(
    now: Long,
    connected: Boolean,
    marks: WearableWatchMarks,
    offlineAlertMinutes: Int,
    lowBatteryPercent: Int,
    batteryLevel: Int,
    isRealData: Boolean
): WatchDecision {
    val lastConnectedAt = marks.lastConnectedAt

    val raiseOffline = !connected &&
        offlineAlertMinutes > 0 &&
        !marks.offlineAlerted &&
        lastConnectedAt != null &&
        now - lastConnectedAt >= offlineAlertMinutes * 60_000L

    val batteryUsable = connected && isRealData && lowBatteryPercent > 0
    val raiseLowBattery = batteryUsable &&
        !marks.lowBatteryAlerted &&
        batteryLevel <= lowBatteryPercent

    val clearLowBattery = batteryUsable &&
        marks.lowBatteryAlerted &&
        batteryLevel > lowBatteryPercent + RECOVERY_MARGIN

    return WatchDecision(
        raiseOffline = raiseOffline,
        raiseLowBattery = raiseLowBattery,
        clearLowBattery = clearLowBattery
    )
}

/** How far the battery must recover before the low-battery notice can fire again. */
internal const val RECOVERY_MARGIN = 10
