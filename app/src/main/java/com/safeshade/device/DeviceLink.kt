package com.safeshade.device

import com.safeshade.data.LedPattern
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The boundary between the app and the wearable.
 *
 * Everything above this interface — repositories, view models, screens — talks
 * in these types and never touches `BluetoothGatt`, a characteristic, or a raw
 * connection string. `BleManager` keeps sole ownership of the GATT operation
 * queue; [RealDeviceLink] only calls its public functions.
 *
 * The interface also exists so an emulator can render every BLE-dependent
 * screen. `FakeDeviceLink` in the debug source set implements exactly this,
 * which is what makes the connected / tripped / live-telemetry states
 * screenshot-verifiable without a wearable on the desk.
 */
interface DeviceLink {

    val connectionState: StateFlow<ConnectionState>
    val deviceName: StateFlow<String>
    val deviceAddress: StateFlow<String>
    val rssi: StateFlow<Int>
    val telemetry: StateFlow<com.safeshade.data.LiveSensorData>

    /**
     * Alerts as *events*, not as a flag.
     *
     * The previous design exposed a `StateFlow<Boolean>` that was set to
     * `true` on a fall. `MutableStateFlow` conflates equal values, so a second
     * fall arriving while the flag was still `true` emitted nothing at all and
     * was silently lost. On a fall-detection product that is not an acceptable
     * failure mode, so alerts are a `SharedFlow` and every one is delivered.
     */
    val alerts: SharedFlow<DeviceAlert>

    /** Companion quick-replies arriving from the wearable. */
    val replies: SharedFlow<String>

    /** Acknowledgement tags, already stripped of the `ACK:` prefix. */
    val acks: SharedFlow<String>

    /**
     * Starts looking for a device.
     *
     * [preferredAddress], when given, restricts the scan to that one device.
     * The paired-devices screen offers a Connect button per saved device, and
     * without this every one of them started the same address-blind scan and
     * joined whichever device answered first - so in the two-device household
     * the screen's own documentation describes, the buttons were
     * indistinguishable.
     */
    fun startScan(preferredAddress: String? = null)
    fun stopScan()
    fun disconnect()
    fun readRssi()

    // Writes. All of these are no-ops unless connectionState is Ready.
    fun writeWeather(payload: String)
    fun writeHealth(payload: String)
    fun writeSettings(payload: String)
    fun writeLed(pattern: LedPattern)
    fun writeExt(tag: String, payload: String)

    /**
     * Guardian to Companion. Writes MESSAGE_CHAR: the wearable buzzes and shows
     * the text as a new incoming message.
     *
     * Never route a reply through here. Doing so makes the firmware treat the
     * reply as a fresh Guardian message, which is a bug this codebase has
     * already shipped once.
     */
    fun writeGuardianMessage(text: String)

    /**
     * Companion to Guardian. Writes REPLY_CHAR.
     *
     * See the warning on [writeGuardianMessage]; these two are not
     * interchangeable and the compiler cannot tell them apart.
     */
    fun writeCompanionReply(text: String)

    /**
     * Makes the wearable announce itself so it can be found.
     *
     * Named honestly rather than as "find my device", because of what the
     * firmware actually does with it: `CMD_FIND` puts the wearable on its full
     * EMERGENCY SOS screen with the siren and red LEDs running, acknowledges
     * nothing, dispatches no alert to anyone, and clears only when somebody
     * physically taps the button on the device.
     *
     * Callers must therefore treat this as loud and unstoppable-from-here, and
     * must suppress automatic weather sync while it is active — `CMD_FIND` and
     * weather share WEATHER_CHAR, and a weather payload written while the
     * device sits on the alarm screen has undefined behaviour that no ACK can
     * reveal.
     */
    fun ringDevice()

    /**
     * Suspends until an `ACK:<tag>` arrives or [timeoutMs] elapses.
     *
     * Returns false on timeout. Note that MESSAGE_CHAR, REPLY_CHAR and
     * `CMD_FIND` are never acknowledged by the firmware, so awaiting an ack for
     * those will always time out.
     */
    suspend fun awaitAck(tag: String, timeoutMs: Long = 4_000L): Boolean
}

/**
 * Link state.
 *
 * The [Connected] / [Ready] distinction is the whole reason this type exists.
 * `BleManager` reports "Connected" the instant the GATT link comes up, but the
 * characteristics are still null at that point — service discovery only starts
 * after the MTU negotiation callback returns. Any write issued in that window
 * hits a null guard and is dropped with nothing but a log line.
 *
 * That window is not theoretical: the previous app re-pushed medical ID, safety
 * settings and the SMS allowlist keyed on "Connected", and every one of those
 * three writes had been silently failing on every reconnect since the code was
 * written. Repositories must gate writes on [Ready].
 */
sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class Found(val name: String) : ConnectionState
    data object Connecting : ConnectionState

    /** GATT link up, characteristics not yet resolved. Writes will be dropped. */
    data object Connected : ConnectionState

    /** Service discovery finished. The only state in which writes reach the device. */
    data object Ready : ConnectionState

    data object BluetoothUnavailable : ConnectionState
    data class ScanFailed(val code: Int) : ConnectionState

    val isUsable: Boolean get() = this is Ready
    val isBusy: Boolean get() = this is Scanning || this is Connecting || this is Found
}

sealed interface DeviceAlert {
    val at: Long

    data class Fall(override val at: Long = System.currentTimeMillis()) : DeviceAlert

    /** Anything else arriving on ALERT_CHAR, kept rather than dropped. */
    data class Unknown(val raw: String, override val at: Long = System.currentTimeMillis()) : DeviceAlert
}
