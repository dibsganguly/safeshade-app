package com.safeshade.repo

import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.data.LocationState
import com.safeshade.data.PersonaMode
import com.safeshade.data.Reminder
import com.safeshade.data.ReminderKind
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.TelemetryPoint
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import com.safeshade.device.RealDeviceLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/** Where a resync got to, for the sync badge on the device screen. */
sealed interface SyncStatus {
    data object Idle : SyncStatus
    data class Syncing(val stage: String) : SyncStatus
    data class Synced(val at: Long) : SyncStatus

    /** [stage] is the ack tag of the write that did not come back. */
    data class Failed(val stage: String, val at: Long) : SyncStatus
}

/**
 * Owns the link to the wearable.
 *
 * Three behaviours in here are load-bearing and are easy to get wrong in a way
 * that produces no error at all:
 *
 *  1. **The Ready gate.** `BleManager` reports "Connected" the moment the GATT
 *     link comes up, but service discovery has not run yet and every
 *     characteristic is still null. Every send function null-guards and returns
 *     silently, so a write in that window vanishes with one log line. The
 *     previous app re-pushed medical ID, safety settings and the SMS allowlist
 *     keyed on "Connected", and all three had been failing on every single
 *     reconnect since the code was written. [pushAll] therefore triggers on
 *     [ConnectionState.Ready] and nothing else.
 *
 *  2. **The re-arm.** The trigger is written as `map { it is Ready }
 *     .distinctUntilChanged().filter { it }` rather than the more natural
 *     `filter { it is Ready }.distinctUntilChanged()`. The latter looks
 *     equivalent and is not: `Ready` is a `data object`, so after `filter`
 *     strips every other state the stream is `Ready, Ready, Ready…` and
 *     `distinctUntilChanged` collapses all of them into one. That resyncs once
 *     per process and never again — reintroducing the exact bug above by a
 *     different route. Deduping on the *boolean* keeps the false-to-true edge
 *     that a reconnect produces.
 *
 *  3. **The ring/weather collision.** `CMD_FIND` is written to WEATHER_CHAR as
 *     a bare literal, and the firmware special-cases it *before* the weather
 *     parser and returns early. So a weather payload written while the wearable
 *     sits on the alarm screen goes into a handler in an undefined state, is
 *     never acknowledged, and cannot be observed to have failed. Automatic
 *     weather sync is suppressed for the whole time [isRinging] is true, and
 *     the guard lives in [syncWeather] rather than in the caller, because a
 *     flag callers must remember to check is the same class of failure as the
 *     Ready gate.
 */
class DeviceRepository(
    private val link: DeviceLink,
    private val prefs: SafeShadePreferences,
    private val scope: CoroutineScope,
    /**
     * The negotiated ATT MTU.
     *
     * Injected rather than read off [link] because MTU is a property of a real
     * GATT connection and has no meaning on the interface — putting it there
     * would force every implementation, the fake included, to invent a number.
     * The default handles the common case; `LinkFactory.mtuProvider` supplies a
     * variant-aware one that can also see through the debug build's switchable
     * wrapper. Budgeting needs the real value because an over-long Medical ID
     * write is truncated by the ATT layer silently rather than rejected — see
     * `DeviceProtocol.health`.
     */
    private val mtuProvider: () -> Int = { (link as? RealDeviceLink)?.mtu ?: DEFAULT_MTU }
) {

    // ============================================
    // Live link state
    // ============================================

    val connection: StateFlow<ConnectionState> = link.connectionState
    val deviceName: StateFlow<String> = link.deviceName
    val deviceAddress: StateFlow<String> = link.deviceAddress
    val telemetry: StateFlow<LiveSensorData> = link.telemetry

    private val _rssiSmoothed = MutableStateFlow(RSSI_UNKNOWN)

    /**
     * RSSI with an exponential moving average, alpha 0.3.
     *
     * Raw BLE RSSI swings 15 dBm between consecutive reads on a stationary
     * device, which makes an unsmoothed signal bar flicker constantly and
     * makes the link sparkline unreadable. 0.3 keeps roughly a five-sample
     * memory: responsive enough to show someone walking out of range, damped
     * enough not to twitch.
     */
    val rssiSmoothed: StateFlow<Int> = _rssiSmoothed.asStateFlow()

    private val _isRinging = MutableStateFlow(false)

    /**
     * True from the moment [ringDevice] is called until
     * [acknowledgeRingStopped].
     *
     * There is no way to learn this from the device: `CMD_FIND` is never
     * acknowledged, and the alarm clears only when somebody physically taps the
     * button on the wearable. So this flag is the app's *belief*, held until a
     * human says otherwise, and it is deliberately sticky — the failure mode of
     * suppressing weather sync for too long is a stale temperature reading,
     * while the failure mode of clearing too early is an unobservable write
     * into a firmware handler in an undefined state.
     */
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /** Null until the wearable has ever reported a position. */
    val lastKnownDeviceLocation: StateFlow<LocationState?> =
        prefs.lastDeviceLocation.stateIn(scope, SharingStarted.Eagerly, null)

    // ============================================
    // Telemetry history
    // ============================================

    /**
     * The live ring buffer.
     *
     * 600 samples at roughly 1 Hz is ten minutes of detail, which is what the
     * device screen's sparklines actually draw. It is deliberately in memory
     * only: writing every sample to DataStore would rewrite the entire
     * preferences file once a second, and DataStore has no partial write.
     */
    private val ring = ArrayDeque<TelemetryPoint>(RING_CAPACITY)
    private val ringLock = Mutex()

    private val _history = MutableStateFlow<List<TelemetryPoint>>(emptyList())

    /** Full samples, oldest first. */
    val history: StateFlow<List<TelemetryPoint>> = _history.asStateFlow()

    val batteryHistory: StateFlow<List<Int>> = _history
        .map { points -> points.map { it.batteryPercent } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val linkHistory: StateFlow<List<Int>> = _history
        .map { points -> points.map { it.rssiDbm } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ============================================
    // Acks
    // ============================================

    private data class AckEvent(val tag: String, val at: Long)

    /**
     * A replaying mirror of `link.acks`, subscribed eagerly in [init].
     *
     * [DeviceLink.awaitAck] subscribes at call time, which leaves a real race:
     * the write goes out, the device answers, and the ack lands before the
     * awaiting coroutine has actually attached to the `SharedFlow` — which has
     * no replay, so the ack is simply gone and the caller times out on a write
     * that succeeded. Mirroring into a replaying buffer that is already
     * subscribed, then filtering by arrival time, removes the window.
     */
    private val ackLog = MutableSharedFlow<AckEvent>(replay = ACK_REPLAY, extraBufferCapacity = 16)

    init {
        link.acks
            .onEach { ackLog.tryEmit(AckEvent(it, System.currentTimeMillis())) }
            .launchIn(scope)

        // Smooth every RSSI reading as it arrives. Reset to "unknown" on
        // disconnect so a reconnect does not inherit the last strong reading
        // from the previous session and draw a signal bar for a dead link.
        link.rssi
            .onEach { raw -> if (raw != 0) applyRssiSample(raw) }
            .launchIn(scope)

        link.connectionState
            .onEach { if (it !is ConnectionState.Ready) _rssiSmoothed.value = RSSI_UNKNOWN }
            .launchIn(scope)

        scope.launch {
            // Seed the sparklines from the persisted downsample so they are not
            // blank for the first minute after a cold start.
            val seed = prefs.telemetryHistory.first()
            if (seed.isNotEmpty()) {
                ringLock.withLock {
                    seed.sortedBy { it.at }.forEach { push(it) }
                    _history.value = ring.toList()
                }
            }

            // Started here, sequentially after the seed, rather than as its own
            // launch. Racing them lets a live sample land first and the older
            // persisted trail append behind it, leaving the ring out of time
            // order — which `downsample` and every sparkline assume it is not.
            link.telemetry
                .filter { it.isRealData }
                .collect { sensors ->
                    ringLock.withLock {
                        push(
                            TelemetryPoint(
                                at = System.currentTimeMillis(),
                                batteryPercent = sensors.batteryLevel.coerceIn(0, 100),
                                rssiDbm = _rssiSmoothed.value
                            )
                        )
                        _history.value = ring.toList()
                    }
                }
        }

        // Poll RSSI while the link is usable. BleManager routes the read
        // through its GATT queue, so this cannot collide with a write.
        scope.launch {
            while (isActive) {
                if (connection.value is ConnectionState.Ready) link.readRssi()
                delay(RSSI_POLL_MS)
            }
        }

        // Persist a downsample on a slow cadence. Once a minute, not once a
        // sample: this is a whole-file rewrite each time.
        scope.launch {
            while (isActive) {
                delay(PERSIST_INTERVAL_MS)
                val snapshot = _history.value
                if (snapshot.isNotEmpty()) prefs.setTelemetryHistory(downsample(snapshot))
            }
        }

        // The resync trigger. See point 2 in the class doc for why the boolean
        // is deduped rather than the state.
        connection
            .map { it is ConnectionState.Ready }
            .distinctUntilChanged()
            .filter { it }
            .onEach { pushAll() }
            .launchIn(scope)
    }

    private fun push(point: TelemetryPoint) {
        ring.addLast(point)
        while (ring.size > RING_CAPACITY) ring.removeFirst()
    }

    private fun applyRssiSample(raw: Int) {
        val previous = _rssiSmoothed.value
        _rssiSmoothed.value = if (previous == RSSI_UNKNOWN) {
            raw
        } else {
            (RSSI_ALPHA * raw + (1 - RSSI_ALPHA) * previous).toInt()
        }
    }

    /**
     * Evenly spaced picks across the whole window, not the last N samples.
     *
     * Keeping the tail would make the restored sparkline claim ten minutes of
     * history while actually showing the last forty-eight seconds — a chart
     * that lies about its own time axis.
     */
    private fun downsample(points: List<TelemetryPoint>): List<TelemetryPoint> {
        val cap = com.safeshade.data.PrefsLimits.TELEMETRY_HISTORY
        if (points.size <= cap) return points
        val step = points.size.toDouble() / cap
        return (0 until cap).map { points[(it * step).toInt().coerceAtMost(points.lastIndex)] }
    }

    // ============================================
    // Scanning / connection
    // ============================================

    fun startScan() = link.startScan()
    fun stopScan() = link.stopScan()
    fun disconnect() = link.disconnect()

    // ============================================
    // The ready-gated resync
    // ============================================

    private val pushLock = Mutex()

    /**
     * Re-pushes everything the wearable needs after a reconnect.
     *
     * Sequential, with an ack awaited between each step, for a specific
     * reason: `BleManager`'s GATT queue is FIFO and Android allows exactly one
     * outstanding operation, so eight writes issued back to back all *enqueue*
     * successfully and there is no per-write result to inspect. Waiting for
     * each ack is the only way to learn which one the device did not take, and
     * [SyncStatus.Failed] carries that tag so the UI can name it.
     *
     * Returns true only if every stage was acknowledged. A false is a real
     * report that the wearable is out of sync, not a transient to swallow.
     */
    suspend fun pushAll(): Boolean = pushLock.withLock {
        if (connection.value !is ConnectionState.Ready) {
            // Not an error: a resync can be requested from the UI at any time,
            // and the honest answer while disconnected is "no".
            _syncStatus.value = SyncStatus.Idle
            return@withLock false
        }

        val profile = prefs.profile.first()
        val safety = prefs.safetySettings.first()
        val allowlist = prefs.smsAllowlist.first()
        val reminders = prefs.reminders.first()

        /*
         * "No stored value" means SKIP the stage, never send the clearing
         * payload.
         *
         * This distinction is the difference between a resync and a reset. The
         * firmware persists the medication time, the check-in interval and the
         * quiet-hours window to NVS, and an empty EXT payload does not mean
         * "unknown" to it — MED with an empty payload sets medReminderHour to
         * -1, CHECKIN with 0 disables the check-in outright, QUIET with an
         * empty payload turns off Do Not Disturb. So a resync that blindly
         * pushed a default for anything the app had no record of would delete
         * settings the wearable was correctly holding, on every reconnect,
         * reporting success the whole way. That is the same shape as the bug
         * this whole class is built around.
         *
         * Quiet hours is the sharpest case: it has no domain model and no
         * preferences key, so it survives only in memory here. After a process
         * death the app genuinely does not know the window — and the right
         * answer to not knowing is to leave the device's own copy alone.
         */
        val medication = reminders.firstOrNull { it.kind == ReminderKind.MEDICATION }
        val checkIn = reminders.firstOrNull { it.kind == ReminderKind.CHECK_IN }

        val stages: List<Pair<String, (() -> Unit)?>> = listOf(
            TAG_HEALTH to { link.writeHealth(DeviceProtocol.health(profile.medicalId, currentMtu())) },
            TAG_SETTINGS to { link.writeSettings(DeviceProtocol.settings(safety)) },
            TAG_SMS_ALLOW to { link.writeExt(TAG_SMS_ALLOW, DeviceProtocol.smsAllowlist(allowlist)) },
            TAG_MODE to { link.writeExt(TAG_MODE, DeviceProtocol.mode(profile.activeMode)) },
            TAG_DEVNAME to {
                link.writeExt(TAG_DEVNAME, DeviceProtocol.deviceName(profile.deviceSettings.name))
            },
            TAG_QUIET to quietStartHour?.let { start ->
                quietEndHour?.let { end ->
                    { link.writeExt(TAG_QUIET, DeviceProtocol.quietHours(start, end)) }
                }
            },
            TAG_CHECKIN to checkIn?.let { reminder ->
                {
                    // A stored-but-disabled reminder is a real instruction to
                    // turn it off, and does get pushed. Only the absence of any
                    // record is treated as "no opinion".
                    val seconds = if (reminder.enabled) reminder.intervalMinutes * 60 else 0
                    link.writeExt(TAG_CHECKIN, DeviceProtocol.checkInInterval(seconds))
                }
            },
            TAG_MED to medication?.let { reminder ->
                {
                    val hour = reminder.hour.takeIf { reminder.enabled }
                    val minute = reminder.minute.takeIf { reminder.enabled }
                    link.writeExt(TAG_MED, DeviceProtocol.medicationTime(hour, minute))
                }
            }
        )

        for ((tag, write) in stages) {
            if (write == null) continue
            _syncStatus.value = SyncStatus.Syncing(tag)
            // Named argument: the second positional parameter is the timeout.
            if (!writeAndAwait(tag, write = write)) {
                _syncStatus.value = SyncStatus.Failed(tag, System.currentTimeMillis())
                return@withLock false
            }
            // The link can drop mid-sequence; continuing would enqueue writes
            // that are dropped by the null guard, exactly as before.
            if (connection.value !is ConnectionState.Ready) {
                _syncStatus.value = SyncStatus.Failed(tag, System.currentTimeMillis())
                return@withLock false
            }
        }

        _syncStatus.value = SyncStatus.Synced(System.currentTimeMillis())
        true
    }

    /**
     * Issues one write and waits for its ack.
     *
     * The `since` timestamp is taken *before* the write so an ack that arrives
     * during the gap is still matched from the replay buffer, while an ack
     * left over from an earlier write of the same tag is not.
     *
     * Matching is prefix-aware because two firmware acks carry a suffix:
     * `ACK:MODE:<name>` and `ACK:LED:<patternName>`. Comparing for equality
     * would time out on both, and on a mode change that is the difference
     * between a confirmed switch and a UI that reverts a second later.
     */
    private suspend fun writeAndAwait(
        tag: String,
        timeoutMs: Long = ACK_TIMEOUT_MS,
        write: () -> Unit
    ): Boolean {
        if (tag in UNACKNOWLEDGED) {
            // MESSAGE, REPLY and CMD_FIND are never acknowledged by the
            // firmware. Awaiting one always times out, so report the write as
            // issued rather than as failed.
            write()
            return true
        }
        val since = System.currentTimeMillis()
        write()
        return withTimeoutOrNull(timeoutMs) {
            ackLog.first { it.at >= since && (it.tag == tag || it.tag.startsWith("$tag:")) }
        } != null
    }

    private fun currentMtu(): Int = mtuProvider().coerceAtLeast(MIN_MTU)

    // ============================================
    // Weather
    // ============================================

    /**
     * Pushes a weather payload, unless the wearable is ringing.
     *
     * The suppression is here rather than at the call site on purpose. See
     * point 3 in the class doc: weather and `CMD_FIND` share WEATHER_CHAR, and
     * the collision is unobservable, so it must be impossible to cause rather
     * than merely documented.
     *
     * Returns false when the write was suppressed or not acknowledged.
     */
    suspend fun syncWeather(payload: String): Boolean {
        if (_isRinging.value) return false
        if (connection.value !is ConnectionState.Ready) return false
        return writeAndAwait(TAG_WEATHER) { link.writeWeather(payload) }
    }

    // ============================================
    // Ring
    // ============================================

    /**
     * Puts the wearable on its full emergency-siren screen so it can be found.
     *
     * Loud, unacknowledged, and not stoppable from the phone — the firmware
     * clears it only on a physical button press. Callers should present it as
     * such.
     */
    fun ringDevice() {
        _isRinging.value = true
        link.ringDevice()
    }

    /** Called when the user confirms the alarm was silenced on the device. */
    fun acknowledgeRingStopped() {
        _isRinging.value = false
    }

    // ============================================
    // Setters. Each returns whether the device acknowledged.
    // ============================================

    suspend fun setLed(pattern: LedPattern): Boolean =
        writeAndAwait(TAG_LED) { link.writeLed(pattern) }

    suspend fun setMode(mode: PersonaMode): Boolean =
        writeAndAwait(TAG_MODE) { link.writeExt(TAG_MODE, DeviceProtocol.mode(mode)) }

    suspend fun setDeviceName(name: String): Boolean =
        writeAndAwait(TAG_DEVNAME) { link.writeExt(TAG_DEVNAME, DeviceProtocol.deviceName(name)) }

    /**
     * Quiet hours. Pass nulls to disable.
     *
     * Held in memory as well as sent, because [pushAll] re-sends it on every
     * reconnect and there is no domain model or preferences key that owns a
     * quiet-hours window.
     *
     * That memory does not survive a process death, which is exactly why
     * [pushAll] *skips* the QUIET stage when it is unset rather than pushing
     * the empty payload. The firmware persists the window to NVS itself, so
     * leaving it alone keeps the wearable correct; pushing a default would
     * delete the user's Do Not Disturb hours on the first reconnect after
     * Android reclaimed the app.
     */
    suspend fun setQuietHours(startHour: Int?, endHour: Int?): Boolean {
        quietStartHour = startHour
        quietEndHour = endHour
        return writeAndAwait(TAG_QUIET) {
            link.writeExt(TAG_QUIET, DeviceProtocol.quietHours(startHour, endHour))
        }
    }

    private var quietStartHour: Int? = null
    private var quietEndHour: Int? = null

    /**
     * The medication reminder. Pass nulls to clear it.
     *
     * Persisted as well as sent. Without the local record, [pushAll] would find
     * no reminder on the next reconnect, skip the MED stage, and the app would
     * quietly stop being able to re-assert a setting the user configured
     * through it — and any earlier version that pushed a default instead would
     * have deleted it outright.
     *
     * The store is written before the wire, so a failed ack leaves the app and
     * the next resync agreeing about what the user asked for.
     */
    suspend fun setMedicationTime(hour: Int?, minute: Int?): Boolean {
        val enabled = hour != null && minute != null
        upsertReminder(ReminderKind.MEDICATION) { existing ->
            (existing ?: Reminder(kind = ReminderKind.MEDICATION)).copy(
                hour = hour ?: existing?.hour ?: 9,
                minute = minute ?: existing?.minute ?: 0,
                enabled = enabled
            )
        }
        return writeAndAwait(TAG_MED) {
            link.writeExt(TAG_MED, DeviceProtocol.medicationTime(hour, minute))
        }
    }

    /** The check-in interval in seconds. Zero disables it. Persisted, as above. */
    suspend fun setCheckInInterval(seconds: Int): Boolean {
        upsertReminder(ReminderKind.CHECK_IN) { existing ->
            (existing ?: Reminder(kind = ReminderKind.CHECK_IN)).copy(
                intervalMinutes = (seconds / 60).coerceAtLeast(0),
                enabled = seconds > 0
            )
        }
        return writeAndAwait(TAG_CHECKIN) {
            link.writeExt(TAG_CHECKIN, DeviceProtocol.checkInInterval(seconds))
        }
    }

    /** Every stored reminder. Null until the first DataStore read completes. */
    val reminders: StateFlow<List<Reminder>?> =
        prefs.reminders.stateIn(scope, SharingStarted.Eagerly, null)

    private val remindersLock = Mutex()

    /**
     * Replaces the single reminder of [kind], or creates it.
     *
     * One per kind, because the firmware holds exactly one medication time and
     * one check-in interval. Storing two would make which one gets pushed
     * depend on list order.
     */
    private suspend fun upsertReminder(
        kind: ReminderKind,
        transform: (Reminder?) -> Reminder
    ) {
        remindersLock.withLock {
            val current = prefs.reminders.first()
            val existing = current.firstOrNull { it.kind == kind }
            val updated = transform(existing)
            prefs.setReminders(current.filterNot { it.kind == kind } + updated)
        }
    }

    /** Pass nulls to stop navigation on the wearable. */
    suspend fun setNavTarget(lat: Double?, lon: Double?, label: String = ""): Boolean =
        writeAndAwait(TAG_NAV) {
            link.writeExt(TAG_NAV, DeviceProtocol.navigation(lat, lon, label))
        }

    /** Records a position reported for the wearable itself, not for this phone. */
    suspend fun recordDeviceLocation(location: LocationState) {
        prefs.setLastDeviceLocation(location)
    }

    private companion object {
        const val RING_CAPACITY = 600
        const val ACK_REPLAY = 16
        const val ACK_TIMEOUT_MS = 4_000L
        const val RSSI_POLL_MS = 5_000L
        const val PERSIST_INTERVAL_MS = 60_000L
        const val RSSI_ALPHA = 0.3f

        /** Android's own "no reading" sentinel. Zero would draw as full signal. */
        const val RSSI_UNKNOWN = -127
        const val DEFAULT_MTU = 247

        /** The BLE minimum. A provider returning 0 before negotiation must not
         * budget the Medical ID payload down to nothing. */
        const val MIN_MTU = 23

        const val TAG_HEALTH = "HEALTH"
        const val TAG_SETTINGS = "SETTINGS"
        const val TAG_WEATHER = "WEATHER"
        const val TAG_LED = "LED"
        const val TAG_MODE = "MODE"
        const val TAG_DEVNAME = "DEVNAME"
        const val TAG_QUIET = "QUIET"
        const val TAG_CHECKIN = "CHECKIN"
        const val TAG_MED = "MED"
        const val TAG_NAV = "NAV"
        const val TAG_SMS_ALLOW = "SMSALLOW"

        val UNACKNOWLEDGED = setOf("MESSAGE", "REPLY", DeviceProtocol.CMD_FIND)
    }
}
