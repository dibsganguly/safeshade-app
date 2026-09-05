package com.safeshade.device

import com.safeshade.BleManager
import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The only class in the app that knows how [BleManager] talks.
 *
 * `BleManager` reports its state as free-text strings ("Connected",
 * "Scanning...", "Found SafeShade S1!"). Five screens used to compare those
 * strings inline, which meant a wording change anywhere was a silent bug
 * everywhere. That mapping now happens exactly once, here.
 *
 * The GATT operation queue stays entirely inside `BleManager`. This class
 * calls its public send functions and nothing else — it has no reference to a
 * `BluetoothGatt`, a characteristic, or the queue.
 */
class RealDeviceLink(
    private val ble: BleManager,
    private val scope: CoroutineScope
) : DeviceLink {

    /**
     * Maps the raw string state plus the readiness flag onto [ConnectionState].
     *
     * The `Connected` vs `Ready` split is the point of this combine: a write
     * issued while the link is merely `Connected` hits a null characteristic
     * and is dropped without an error.
     */
    override val connectionState = combine(
        ble.connectionState,
        ble.servicesReady
    ) { raw, ready ->
        when {
            raw == "Connected" && ready -> ConnectionState.Ready
            raw == "Connected" -> ConnectionState.Connected
            raw == "Connecting..." -> ConnectionState.Connecting
            raw == "Scanning..." -> ConnectionState.Scanning
            raw == "BT Unavailable" -> ConnectionState.BluetoothUnavailable
            raw.startsWith("Found ") ->
                ConnectionState.Found(raw.removePrefix("Found ").removeSuffix("!"))
            raw.startsWith("Scan Failed") ->
                ConnectionState.ScanFailed(raw.substringAfterLast(": ").trim().toIntOrNull() ?: -1)
            else -> ConnectionState.Disconnected
        }
    }.distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    override val deviceName = ble.deviceName
    override val deviceAddress = ble.deviceAddress
    override val rssi = ble.rssi
    override val telemetry = ble.liveSensorData

    private val _alerts = MutableSharedFlow<DeviceAlert>(extraBufferCapacity = 8)
    override val alerts = _alerts.asSharedFlow()

    private val _replies = MutableSharedFlow<String>(extraBufferCapacity = 16)
    override val replies = _replies.asSharedFlow()

    override val acks = ble.ackEvents

    /** The MTU actually negotiated, for payload budgeting in DeviceProtocol. */
    val mtu get() = ble.negotiatedMtu.value

    init {
        // Alerts arrive as raw strings on the event flow; classify once here so
        // nothing downstream string-matches "FALL_DETECTED" again.
        ble.alertEvents
            .onEach { raw ->
                _alerts.tryEmit(
                    if (raw == "FALL_DETECTED") DeviceAlert.Fall() else DeviceAlert.Unknown(raw)
                )
            }
            .launchIn(scope)

        // deviceReply is a StateFlow<String?>, so an identical reply sent twice
        // in a row would be conflated away. Republishing it as a SharedFlow and
        // clearing immediately turns it back into a stream of events; the
        // repository still de-duplicates against the SMS relay, which delivers
        // the same reply a second time over cellular.
        scope.launch {
            ble.deviceReply.collect { reply ->
                if (reply != null) {
                    _replies.tryEmit(reply)
                    ble.clearReply()
                }
            }
        }
    }

    override fun startScan(preferredAddress: String?) = ble.startScanning(preferredAddress = preferredAddress)
    override fun stopScan() = ble.stopScanning()
    override fun disconnect() = ble.disconnect()
    override fun readRssi() = ble.readRssi()

    override fun writeWeather(payload: String) = ble.sendWeatherPayload(payload)
    override fun writeHealth(payload: String) = ble.sendHealthPayload(payload)
    override fun writeSettings(payload: String) = ble.sendSettingsPayload(payload)
    override fun writeLed(pattern: LedPattern) = ble.sendLedPattern(pattern)
    override fun writeExt(tag: String, payload: String) = ble.sendExtCommand(tag, payload)

    override fun writeGuardianMessage(text: String) = ble.sendGuardianMessage(text)
    override fun writeCompanionReply(text: String) = ble.sendDeviceReply(text)

    override fun ringDevice() = ble.sendCommand(DeviceProtocol.CMD_FIND)

    override suspend fun awaitAck(tag: String, timeoutMs: Long): Boolean =
        ble.awaitAck(tag, timeoutMs)
}

/** Convenience for the LiveSensorData default, so the fake and the real agree. */
internal val NoTelemetry = LiveSensorData()
