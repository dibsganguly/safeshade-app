package com.safeshade.debug

import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.device.DeviceAlert
import com.safeshade.device.DeviceLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

/**
 * A [DeviceLink] that can be swapped between the real radio and [FakeDeviceLink]
 * at runtime, without restarting the app.
 *
 * Only reachable in a debug build. The repositories above it hold a single
 * `DeviceLink` reference for the lifetime of the process, so switching cannot
 * mean handing them a different object — every flow below is therefore a
 * `flatMapLatest` over which delegate is currently active, and every call is
 * forwarded to whichever one that is.
 *
 * @param real the production link. Kept alive across a switch so toggling back
 *   does not tear down a live GATT connection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SwitchableDeviceLink(
    /** Exposed so `LinkFactory.mtuProvider` can see through this wrapper. */
    val real: DeviceLink,
    val fake: FakeDeviceLink,
    // Must be a property, not a plain constructor parameter: the switching
    // helpers below are member functions, and a bare parameter is only in
    // scope inside initialisers.
    private val scope: CoroutineScope,
    useFake: Boolean = false
) : DeviceLink {

    private val _useFake = MutableStateFlow(useFake)
    val usingFake: StateFlow<Boolean> = _useFake.asStateFlow()

    private val active: DeviceLink get() = if (_useFake.value) fake else real

    fun useFake(enabled: Boolean) {
        if (_useFake.value == enabled) return
        // Disconnect whichever link is being switched away from, so a real GATT
        // session is not left open and invisible behind the fake.
        active.disconnect()
        _useFake.value = enabled
    }

    private fun <T> switching(select: (DeviceLink) -> StateFlow<T>): StateFlow<T> =
        _useFake.flatMapLatest { select(active) }
            .stateIn(scope, SharingStarted.Eagerly, select(active).value)

    private fun <T> switchingShared(select: (DeviceLink) -> SharedFlow<T>): SharedFlow<T> =
        _useFake.flatMapLatest { select(active) }
            .shareIn(scope, SharingStarted.Eagerly, replay = 0)

    override val connectionState = switching { it.connectionState }
    override val deviceName = switching { it.deviceName }
    override val deviceAddress = switching { it.deviceAddress }
    override val rssi = switching { it.rssi }
    override val telemetry: StateFlow<LiveSensorData> = switching { it.telemetry }

    override val alerts: SharedFlow<DeviceAlert> = switchingShared { it.alerts }
    override val replies: SharedFlow<String> = switchingShared { it.replies }
    override val acks: SharedFlow<String> = switchingShared { it.acks }

    override fun startScan() = active.startScan()
    override fun stopScan() = active.stopScan()
    override fun disconnect() = active.disconnect()
    override fun readRssi() = active.readRssi()

    override fun writeWeather(payload: String) = active.writeWeather(payload)
    override fun writeHealth(payload: String) = active.writeHealth(payload)
    override fun writeSettings(payload: String) = active.writeSettings(payload)
    override fun writeLed(pattern: LedPattern) = active.writeLed(pattern)
    override fun writeExt(tag: String, payload: String) = active.writeExt(tag, payload)

    /** Guardian to Companion — MESSAGE_CHAR. Not interchangeable with the reply. */
    override fun writeGuardianMessage(text: String) = active.writeGuardianMessage(text)

    /** Companion to Guardian — REPLY_CHAR. Not interchangeable with the message. */
    override fun writeCompanionReply(text: String) = active.writeCompanionReply(text)

    override fun ringDevice() = active.ringDevice()

    override suspend fun awaitAck(tag: String, timeoutMs: Long): Boolean =
        active.awaitAck(tag, timeoutMs)

    /** Convenience for a debug drawer: switch to the fake and play a scenario. */
    fun playScenario(scenario: Scenario) {
        useFake(true)
        fake.play(scenario)
    }

    /** Exposed so a debug overlay can inject an alert without a wearable. */
    fun emitFakeAlert(alert: DeviceAlert) = fake.emitAlert(alert)
}
