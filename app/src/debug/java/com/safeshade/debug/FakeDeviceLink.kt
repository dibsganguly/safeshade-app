package com.safeshade.debug

import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.device.BleSighting
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceAlert
import com.safeshade.device.DeviceLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * A [DeviceLink] with no radio behind it, driven by a [Scenario].
 *
 * The point is not to simulate BLE faithfully — it is to make every
 * BLE-dependent screen reachable on an emulator, so the connected, tripped,
 * ringing and low-battery layouts can be reviewed and screenshotted without a
 * wearable on the desk.
 *
 * Where it *is* faithful, it is faithful on purpose:
 *
 *  - It passes through [ConnectionState.Connected] before [ConnectionState.Ready],
 *    with a real delay between them. A repository that writes on Connected will
 *    fail here exactly as it fails on hardware — silently — which is the whole
 *    reason that window is scripted rather than skipped.
 *  - It never acknowledges a message, a reply, or `ringDevice()`, matching the
 *    firmware. Code that waits for one of those acks hangs here too.
 *  - Alerts go out on a `SharedFlow`, and [Scenario.FALL_ALERT] fires two of
 *    them. A consumer that collapses alerts back into a Boolean shows one.
 *
 * Debug source set only. See [Scenario] for why that is a source-set boundary
 * rather than a `BuildConfig.DEBUG` branch.
 */
class FakeDeviceLink(
    private val scope: CoroutineScope,
    initialScenario: Scenario = Scenario.LIVE_TELEMETRY
) : DeviceLink {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState = _connectionState.asStateFlow()

    private val _deviceName = MutableStateFlow("SafeShade S1 (fake)")
    override val deviceName = _deviceName.asStateFlow()

    private val _deviceAddress = MutableStateFlow("DE:AD:BE:EF:00:01")
    override val deviceAddress = _deviceAddress.asStateFlow()

    private val _rssi = MutableStateFlow(0)
    override val rssi = _rssi.asStateFlow()

    private val _telemetry = MutableStateFlow(LiveSensorData())
    override val telemetry = _telemetry.asStateFlow()

    private val _alerts = MutableSharedFlow<DeviceAlert>(extraBufferCapacity = 8)
    override val alerts = _alerts.asSharedFlow()

    private val _replies = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val replies = _replies.asSharedFlow()

    private val _acks = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val acks = _acks.asSharedFlow()

    private val _sightings = MutableSharedFlow<BleSighting>(extraBufferCapacity = 64)
    override val sightings = _sightings.asSharedFlow()

    private val _scenario = MutableStateFlow(initialScenario)
    val scenario = _scenario.asStateFlow()

    /** True while [Scenario.ACK_ALL] (or any Ready scenario) should answer writes. */
    private var autoAck = false

    private var timeline: Job? = null

    /** Everything written, newest last. Handy for asserting in a debug overlay. */
    private val _writeLog = MutableStateFlow<List<String>>(emptyList())
    val writeLog = _writeLog.asStateFlow()

    init {
        play(initialScenario)
    }

    /** Switches scenario, cancelling whatever timeline was running. */
    fun play(scenario: Scenario) {
        _scenario.value = scenario
        timeline?.cancel()
        timeline = scope.launch { run(scenario) }
    }

    private suspend fun run(scenario: Scenario) {
        autoAck = false
        _telemetry.value = LiveSensorData()
        _rssi.value = 0

        when (scenario) {
            Scenario.DISCONNECTED -> {
                _connectionState.value = ConnectionState.Disconnected
            }

            Scenario.SCANNING -> {
                _connectionState.value = ConnectionState.Scanning
                delay(2_000)
                _connectionState.value = ConnectionState.Found("SafeShade S1")
            }

            Scenario.CONNECTED_IDLE -> becomeReady()

            Scenario.ACK_ALL -> {
                becomeReady()
                autoAck = true
            }

            Scenario.GEOFENCE_EXIT -> {
                becomeReady()
                autoAck = true
            }

            Scenario.LIVE_TELEMETRY -> {
                becomeReady()
                autoAck = true
                driveTelemetry(startBattery = 87)
            }

            Scenario.LOW_BATTERY -> {
                becomeReady()
                autoAck = true
                driveTelemetry(startBattery = 12, drainPerTick = 0.05f)
            }

            Scenario.FALL_ALERT -> {
                becomeReady()
                autoAck = true
                scope.launch { driveTelemetry(startBattery = 64) }
                delay(2_000)
                _alerts.emit(DeviceAlert.Fall())
                // A second fall while the first is still unresolved. A
                // consumer that stored alerts in a conflating StateFlow<Boolean>
                // shows exactly one of these.
                delay(6_000)
                _alerts.emit(DeviceAlert.Fall())
            }

            Scenario.SOS_ACTIVE -> {
                becomeReady()
                autoAck = true
                delay(1_500)
                _alerts.emit(DeviceAlert.Unknown("SOS_PRESSED"))
            }

            Scenario.INCOMING_MESSAGE -> {
                becomeReady()
                autoAck = true
                delay(1_500)
                _replies.emit("On my way")
                // The gateway relays every reply over BLE *and* SMS. Emitting
                // the same text twice inside the dedupe window is how a
                // repository that forgot to dedupe shows a doubled thread.
                delay(2_000)
                _replies.emit("On my way")
            }

            Scenario.RINGING -> {
                becomeReady()
                // Deliberately no autoAck: CMD_FIND returns before the
                // firmware's ack path, so nothing ever comes back.
            }
        }
    }

    /**
     * Connects, sits in the pre-discovery window, then becomes Ready.
     *
     * The 900 ms in Connected is not padding. It is the window in which every
     * characteristic is still null on real hardware and every write is dropped
     * without an error, and it is scripted so that a repository which gates on
     * Connected instead of Ready fails here too.
     */
    private suspend fun becomeReady() {
        _connectionState.value = ConnectionState.Scanning
        delay(600)
        _connectionState.value = ConnectionState.Found("SafeShade S1")
        delay(400)
        _connectionState.value = ConnectionState.Connecting
        delay(700)
        _connectionState.value = ConnectionState.Connected
        delay(900)
        _connectionState.value = ConnectionState.Ready
        _rssi.value = -58
    }

    /**
     * Roughly 1 Hz telemetry with a plausible shape.
     *
     * A constant value would make every sparkline a flat line, which hides
     * precisely the rendering problems these scenarios exist to expose. The
     * accelerometer is a slow sine per axis with noise on top — a walking gait,
     * hovering near 1 g total — and the battery drains slowly enough that the
     * percentage actually changes while somebody is looking at the screen.
     */
    private suspend fun driveTelemetry(startBattery: Int, drainPerTick: Float = 0.02f) {
        var battery = startBattery.toFloat()
        var tick = 0
        while (scope.isActive) {
            val t = tick / 8.0
            _telemetry.value = LiveSensorData(
                accelX = (sin(t) * 0.28 + Random.nextDouble(-0.05, 0.05)).toFloat(),
                accelY = (sin(t * 1.7 + 1.2) * 0.22 + Random.nextDouble(-0.05, 0.05)).toFloat(),
                accelZ = (0.96 + sin(t * 0.6) * 0.08 + Random.nextDouble(-0.03, 0.03)).toFloat(),
                temperature = 29.5f + (sin(t / 12) * 1.4f).toFloat(),
                lightLevel = (420 + sin(t / 5) * 180).toInt().coerceAtLeast(0),
                batteryLevel = battery.toInt().coerceIn(0, 100),
                isRealData = true
            )
            // RSSI wanders the way a pocketed device's does, so the EMA
            // downstream has something to actually smooth.
            _rssi.value = (-58 - abs(sin(t / 3)) * 22 + Random.nextDouble(-4.0, 4.0)).toInt()

            battery -= drainPerTick
            if (battery < 1f) battery = 1f
            tick++
            delay(1_000)
        }
    }

    // ============================================
    // DeviceLink
    // ============================================

    override fun startScan(preferredAddress: String?) {
        if (_connectionState.value is ConnectionState.Disconnected) play(_scenario.value)
    }

    override fun stopScan() {
        if (_connectionState.value is ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    /**
     * A sweep with no radio: it reports nothing on its own.
     *
     * Deliberately not scripted with invented neighbours. A fabricated stranger
     * would be written into the sightings store and then reported to the cloud
     * as a real device someone's phone had passed, which is a claim about the
     * physical world that this fake is in no position to make. Use
     * [emitSighting] to drive the screens instead - an explicit call from a
     * debug drawer is a person choosing to, rather than the app inventing it.
     *
     * The connection state is untouched, exactly as on hardware.
     */
    override fun startSightingScan(durationMs: Long) = Unit

    override fun stopSightingScan() = Unit

    override fun disconnect() {
        timeline?.cancel()
        _connectionState.value = ConnectionState.Disconnected
        _telemetry.value = LiveSensorData()
        _rssi.value = 0
        autoAck = false
    }

    override fun readRssi() {
        // No-op: the timeline already publishes RSSI.
    }

    override fun writeWeather(payload: String) = record("WEATHER", payload, "WEATHER")
    override fun writeHealth(payload: String) = record("HEALTH", payload, "HEALTH")
    override fun writeSettings(payload: String) = record("SETTINGS", payload, "SETTINGS")

    override fun writeLed(pattern: LedPattern) =
        record("LED", pattern.label, "LED:${pattern.label}")

    override fun writeExt(tag: String, payload: String) {
        // MODE is acknowledged as "MODE:<name>", matching the firmware. Code
        // that compares an ack for equality rather than by prefix times out
        // here exactly as it does on hardware.
        val ackTag = if (tag == "MODE" && payload.isNotEmpty()) "MODE:$payload" else tag
        record(tag, payload, ackTag)
    }

    /** Unacknowledged on real firmware, and unacknowledged here. */
    override fun writeGuardianMessage(text: String) = record("MESSAGE", text, null)

    /** Unacknowledged on real firmware, and unacknowledged here. */
    override fun writeCompanionReply(text: String) = record("REPLY", text, null)

    /** Unacknowledged, and never self-clearing — same as `CMD_FIND`. */
    override fun ringDevice() = record("CMD_FIND", "", null)

    private fun record(tag: String, payload: String, ackTag: String?) {
        // Writes are dropped when the link is not Ready, exactly as
        // BleManager's null guards drop them. Logging them would make the fake
        // more forgiving than the hardware, which defeats the purpose.
        if (_connectionState.value !is ConnectionState.Ready) return
        _writeLog.value = (_writeLog.value + "$tag=$payload").takeLast(64)
        if (autoAck && ackTag != null) {
            scope.launch {
                delay(120)
                _acks.emit(ackTag)
            }
        }
    }

    override suspend fun awaitAck(tag: String, timeoutMs: Long): Boolean =
        withTimeoutOrNull(timeoutMs) {
            acks.first { it == tag || it.startsWith("$tag:") }
        } != null

    // ============================================
    // Debug drivers
    // ============================================

    /** Pushes an alert as if the wearable had sent one. */
    fun emitAlert(alert: DeviceAlert) {
        scope.launch { _alerts.emit(alert) }
    }

    /** Pushes an advertisement as if a SafeShade had walked past. */
    fun emitSighting(sighting: BleSighting) {
        scope.launch { _sightings.emit(sighting) }
    }

    /** Pushes a companion reply as if it had arrived over BLE. */
    fun emitReply(text: String) {
        scope.launch { _replies.emit(text) }
    }
}
