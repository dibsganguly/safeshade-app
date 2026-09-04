/**
 * SafeShade - Universal Safety Companion
 *
 * BleManager.kt
 *
 * Handles all Bluetooth Low Energy communication with the SafeShade device.
 * Manages scanning, connection, data transmission, and notifications.
 *
 * @author SafeShade Team
 * @version 2.2.0
 *
 * FEATURES (this pass):
 *  - Added telemetryCharacteristic (TELEMETRY_CHAR_UUID) parsing into a new
 *    liveSensorData StateFlow, and ledCharacteristic (LED_CHAR_UUID) via
 *    sendLedPattern() - both require the re-flashed firmware from this pass
 *    (docs/SafeShadev21.ino) to actually be present on the GATT service.
 *
 * FIXES (earlier pass):
 *  - Added a strict, single-threaded GATT operation queue. Android's
 *    BluetoothGatt only allows ONE outstanding operation (write/read/
 *    descriptor-write/MTU request) at a time - firing a second one before
 *    the previous operation's callback returns causes it to be silently
 *    dropped (no error, no callback). This previously caused health data
 *    / settings / weather syncs to randomly "not stick" on the device.
 *  - Negotiate a larger MTU on connect so weather/health payloads (which
 *    routinely exceed the default 20-byte ATT payload limit) are not
 *    silently truncated by the Android BLE stack before they even reach
 *    the ESP32.
 *  - Added sendDeviceReply() which actually writes to REPLY_CHAR_UUID.
 *    Previously "quick replies" from Companion mode were being sent via
 *    sendGuardianMessage() to MESSAGE_CHAR_UUID, which the firmware
 *    interprets as an incoming Guardian message (it buzzes the device and
 *    opens SCREEN_MESSAGE) rather than as a reply. That direction of
 *    communication was completely non-functional before this fix.
 *  - Guarded startScanning() against being called while already scanning
 *    or connected, and against a null/off adapter, to avoid Android's
 *    "scanning too frequently" throttling and duplicate scanner leaks.
 *  - enableNotification() now goes through the operation queue too.
 */

package com.safeshade

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.safeshade.data.FallSensitivity
import com.safeshade.data.LedPattern
import com.safeshade.data.LiveSensorData
import com.safeshade.data.MedicalId
import com.safeshade.data.SafetySettings
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.ArrayDeque
import java.util.UUID

// ============================================
// BLE UUIDs - Must match ESP32 firmware exactly
// ============================================
val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
val WEATHER_CHAR_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
val ALERT_CHAR_UUID = UUID.fromString("8c5314e3-89ee-4752-9214-4d834311827e")
val MESSAGE_CHAR_UUID = UUID.fromString("1c95d5e3-d8f7-413a-bf3d-7a2e5d7be87e")
val HEALTH_CHAR_UUID = UUID.fromString("2a4d6e8f-1234-5678-abcd-ef0123456789")
val SETTINGS_CHAR_UUID = UUID.fromString("3b5e7f90-2345-6789-bcde-f01234567890")
val REPLY_CHAR_UUID = UUID.fromString("4c6f8a01-3456-789a-cdef-012345678901")
val TELEMETRY_CHAR_UUID = UUID.fromString("5d7f9a02-4567-89ab-def0-123456789012")
val LED_CHAR_UUID = UUID.fromString("6e8a0b13-5678-9abc-ef01-234567890123")
// Generic tagged-command channel (App->Device) and unified acknowledgement
// stream (Device->App) - see sendExtCommand()/awaitAck() below.
val EXT_CHAR_UUID = UUID.fromString("7f9b1c24-6789-abcd-f012-3456789abcde")
val ACK_CHAR_UUID = UUID.fromString("80ac2d35-789a-bcde-0123-456789abcdef")

// Standard descriptor UUID for enabling notifications
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// Requested ATT MTU. 247 gives ~244 usable payload bytes, comfortably above
// anything this app sends (the longest is the weather+location payload).
private const val DESIRED_MTU = 247

/**
 * A single queued GATT operation. Only one of these runs at a time; the
 * next one is dequeued only after the corresponding callback fires.
 */
private sealed class GattOp {
    data class Write(val characteristic: BluetoothGattCharacteristic, val value: ByteArray) : GattOp()
    data class WriteDescriptor(val descriptor: BluetoothGattDescriptor) : GattOp()
    data class ReadRssi(val unused: Unit = Unit) : GattOp()
    data class RequestMtu(val mtu: Int) : GattOp()
}

/**
 * BLE Manager class for SafeShade device communication.
 *
 * Handles:
 * - Device scanning and connection
 * - Sending weather, health, and settings data
 * - Receiving fall alerts and device replies
 * - Connection state management
 *
 * All GATT operations are funneled through a simple FIFO queue
 * (see [enqueue]/[drainQueue]) because Android's BluetoothGatt can only
 * process one outstanding operation at a time; anything issued while
 * another is in flight is silently dropped by the OS.
 */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var bluetoothGatt: BluetoothGatt? = null

    // ============================================
    // ALL Characteristic References
    // ============================================
    private var weatherCharacteristic: BluetoothGattCharacteristic? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var healthCharacteristic: BluetoothGattCharacteristic? = null
    private var settingsCharacteristic: BluetoothGattCharacteristic? = null
    private var replyCharacteristic: BluetoothGattCharacteristic? = null
    private var telemetryCharacteristic: BluetoothGattCharacteristic? = null
    private var ledCharacteristic: BluetoothGattCharacteristic? = null
    private var extCharacteristic: BluetoothGattCharacteristic? = null
    private var ackCharacteristic: BluetoothGattCharacteristic? = null

    // ============================================
    // GATT operation queue state
    // ============================================
    private val opQueue = ArrayDeque<GattOp>()
    private var opInFlight = false

    // ============================================
    // Scan state
    // ============================================
    private var isScanning = false
    private var activeScanCallback: ScanCallback? = null

    // ============================================
    // State Flows for UI observation
    // ============================================
    private val _connectionState = MutableStateFlow("Disconnected")
    val connectionState = _connectionState.asStateFlow()

    private val _fallAlert = MutableStateFlow(false)
    val fallAlert = _fallAlert.asStateFlow()

    private val _deviceName = MutableStateFlow("SafeShade S1")
    val deviceName = _deviceName.asStateFlow()

    private val _deviceAddress = MutableStateFlow("--:--:--:--:--:--")
    val deviceAddress = _deviceAddress.asStateFlow()

    private val _rssi = MutableStateFlow(-100)
    val rssi = _rssi.asStateFlow()

    // State flow for device replies - observed by UI for real-time updates
    private val _deviceReply = MutableStateFlow<String?>(null)
    val deviceReply = _deviceReply.asStateFlow()

    // Real telemetry from the device's MPU6050/LDR/battery, replacing the
    // Math.random()-driven placeholder DeviceScreen used to show. isRealData
    // stays false until the first notify actually arrives.
    private val _liveSensorData = MutableStateFlow(LiveSensorData())
    val liveSensorData = _liveSensorData.asStateFlow()

    // Unified acknowledgement stream from ACK_CHAR_UUID (device confirms a
    // settings/health/LED/ext write actually took effect). Each emission is
    // the tag alone, e.g. "SETTINGS" or "LED:FIRE" - the "ACK:" wire prefix
    // is stripped in handleIncomingValue(). Prefer awaitAck() below over
    // collecting this directly for a one-shot "did my write land" check.
    private val _ackEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val ackEvents = _ackEvents.asSharedFlow()

    private var connectionTime: Long = 0

    // Set right before a user-initiated disconnect() so the
    // STATE_DISCONNECTED branch below knows not to auto-reconnect.
    private var userInitiatedDisconnect = false

    // ============================================
    // Scanning
    // ============================================

    /**
     * Start scanning for SafeShade devices.
     *
     * Safe to call repeatedly - it is a no-op if a scan is already running
     * or a device is already connected/connecting, which avoids Android's
     * "scanning too frequently" throttling (undocumented ~5 scans / 30s
     * limit) and duplicate ScanCallback leaks.
     *
     * @param onFound Callback when device is found
     */
    @SuppressLint("MissingPermission")
    fun startScanning(onFound: () -> Unit = {}) {
        if (isScanning) {
            Log.d("BLE_SCAN", "Scan already in progress, ignoring request")
            return
        }
        if (bluetoothGatt != null) {
            Log.d("BLE_SCAN", "Already connected/connecting, ignoring scan request")
            return
        }
        // A prior disconnect() call synchronously closes the GATT client
        // (see its comment) - Android frequently never delivers the async
        // STATE_DISCONNECTED callback once close() has already been called,
        // which is the only place this flag would otherwise get reset. Left
        // stuck true, it would silently skip the auto-retry the very next
        // failed connection attempt needs (Android's first connectGatt()
        // commonly fails once and needs exactly that retry). Any explicit
        // new scan means we're no longer in a "stay disconnected" state, so
        // clear it here defensively.
        userInitiatedDisconnect = false

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _connectionState.value = "BT Unavailable"
            return
        }

        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _connectionState.value = "BT Unavailable"
            return
        }

        _connectionState.value = "Scanning..."

        val filter = android.bluetooth.le.ScanFilter.Builder()
            .setServiceUuid(android.os.ParcelUuid(SERVICE_UUID))
            .build()

        val settings = android.bluetooth.le.ScanSettings.Builder()
            .setScanMode(android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                val deviceNameResult = result?.device?.name ?: "SafeShade"

                Log.d("BLE_SCAN", "Found: $deviceNameResult - ${device?.address}")

                if (device != null) {
                    _connectionState.value = "Found $deviceNameResult!"
                    _deviceName.value = deviceNameResult
                    _deviceAddress.value = device.address
                    _rssi.value = result.rssi
                    stopScanning()
                    connectToDevice(device)
                    onFound()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                isScanning = false
                activeScanCallback = null
                _connectionState.value = "Scan Failed: $errorCode"
                Log.e("BLE_SCAN", "Scan Failed: $errorCode")
            }
        }

        activeScanCallback = scanCallback
        isScanning = true
        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    /**
     * Stop any in-progress scan. Safe to call even if nothing is scanning.
     */
    @SuppressLint("MissingPermission")
    fun stopScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
        val callback = activeScanCallback
        if (scanner != null && callback != null) {
            scanner.stopScan(callback)
        }
        isScanning = false
        activeScanCallback = null
    }

    // ============================================
    // Connection
    // ============================================

    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = "Connecting..."
        opQueue.clear()
        opInFlight = false
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    // ============================================
    // GATT Callbacks
    // ============================================

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionState.value = "Connected"
                    connectionTime = System.currentTimeMillis()
                    userInitiatedDisconnect = false
                    // Request a larger MTU first so subsequent writes (weather,
                    // health) aren't truncated to 20 bytes by the stack.
                    // discoverServices() is kicked off once the MTU callback
                    // returns (see onMtuChanged below) to keep operations
                    // strictly sequential.
                    gatt?.requestMtu(DESIRED_MTU)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = "Disconnected"
                    connectionTime = 0
                    clearCharacteristics()
                    opQueue.clear()
                    opInFlight = false
                    gatt?.close()
                    bluetoothGatt = null
                    if (userInitiatedDisconnect) {
                        userInitiatedDisconnect = false
                    } else {
                        startScanning()
                    }
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            Log.d("BLE", "MTU changed to $mtu (status=$status)")
            gatt?.discoverServices()
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssi
            }
            opInFlight = false
            drainQueue()
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE", "Service discovery failed: $status")
                return
            }

            val service = gatt?.getService(SERVICE_UUID)
            if (service == null) {
                Log.e("BLE", "SafeShade service not found!")
                return
            }

            // Discover ALL characteristics
            weatherCharacteristic = service.getCharacteristic(WEATHER_CHAR_UUID)
            messageCharacteristic = service.getCharacteristic(MESSAGE_CHAR_UUID)
            healthCharacteristic = service.getCharacteristic(HEALTH_CHAR_UUID)
            settingsCharacteristic = service.getCharacteristic(SETTINGS_CHAR_UUID)
            replyCharacteristic = service.getCharacteristic(REPLY_CHAR_UUID)
            telemetryCharacteristic = service.getCharacteristic(TELEMETRY_CHAR_UUID)
            ledCharacteristic = service.getCharacteristic(LED_CHAR_UUID)
            extCharacteristic = service.getCharacteristic(EXT_CHAR_UUID)
            ackCharacteristic = service.getCharacteristic(ACK_CHAR_UUID)

            Log.d("BLE", "Characteristics discovered:")
            Log.d("BLE", "  Weather: ${weatherCharacteristic != null}")
            Log.d("BLE", "  Message: ${messageCharacteristic != null}")
            Log.d("BLE", "  Health: ${healthCharacteristic != null}")
            Log.d("BLE", "  Settings: ${settingsCharacteristic != null}")
            Log.d("BLE", "  Reply: ${replyCharacteristic != null}")
            Log.d("BLE", "  Telemetry: ${telemetryCharacteristic != null}")
            Log.d("BLE", "  LED: ${ledCharacteristic != null}")
            Log.d("BLE", "  Ext: ${extCharacteristic != null}")
            Log.d("BLE", "  Ack: ${ackCharacteristic != null}")

            // Enable notifications for ALERT/REPLY/TELEMETRY characteristics.
            // All go through the operation queue now, so there's no need
            // for a fragile postDelayed() hack to avoid clobbering the
            // first descriptor write. Telemetry is optional - older firmware
            // without this characteristic simply won't populate liveSensorData.
            val alertChar = service.getCharacteristic(ALERT_CHAR_UUID)
            if (alertChar != null) {
                enableNotification(alertChar)
            }
            replyCharacteristic?.let { enableNotification(it) }
            telemetryCharacteristic?.let { enableNotification(it) }
            ackCharacteristic?.let { enableNotification(it) }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            @Suppress("DEPRECATION")
            val value = characteristic.value?.let { String(it, Charsets.UTF_8) } ?: return
            handleIncomingValue(characteristic.uuid, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            handleIncomingValue(characteristic.uuid, String(value, Charsets.UTF_8))
        }

        private fun handleIncomingValue(uuid: UUID, value: String) {
            when (uuid) {
                ALERT_CHAR_UUID -> {
                    Log.d("BLE", "Alert received: $value")
                    if (value == "FALL_DETECTED") {
                        _fallAlert.value = true
                    }
                }
                REPLY_CHAR_UUID -> {
                    // Handle device replies - this triggers real-time UI update
                    Log.d("BLE", "Reply received from device: $value")
                    _deviceReply.value = value
                }
                ACK_CHAR_UUID -> {
                    // Wire format "ACK:<tag>" - see sendAck() in the firmware.
                    val tag = value.removePrefix("ACK:")
                    Log.d("BLE", "Ack received: $tag")
                    _ackEvents.tryEmit(tag)
                }
                TELEMETRY_CHAR_UUID -> {
                    // "accelXg,accelYg,accelZg,tempC,lightRaw,batteryPct"
                    val parts = value.split(",")
                    if (parts.size >= 6) {
                        _liveSensorData.value = LiveSensorData(
                            accelX = parts[0].toFloatOrNull() ?: 0f,
                            accelY = parts[1].toFloatOrNull() ?: 0f,
                            accelZ = parts[2].toFloatOrNull() ?: 0f,
                            temperature = parts[3].toFloatOrNull() ?: 0f,
                            lightLevel = parts[4].toIntOrNull() ?: 0,
                            batteryLevel = parts[5].toIntOrNull() ?: 0,
                            isRealData = true
                        )
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("BLE", "Notification enabled for: ${descriptor?.characteristic?.uuid}")
            } else {
                Log.e("BLE", "Failed to enable notification: $status")
            }
            opInFlight = false
            drainQueue()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e("BLE", "Write failed for ${characteristic?.uuid}: status=$status")
            }
            opInFlight = false
            drainQueue()
        }
    }

    // ============================================
    // GATT operation queue
    //
    // Android's BluetoothGatt allows only ONE outstanding request/response
    // operation (write, descriptor write, RSSI read, MTU request) at a
    // time. Firing a second one before the callback for the first has
    // returned causes it to be silently ignored - no exception, no
    // callback, no error. Every operation in this class goes through
    // enqueue()/drainQueue() to guarantee strict one-at-a-time execution.
    // ============================================

    private fun enqueue(op: GattOp) {
        opQueue.add(op)
        drainQueue()
    }

    @SuppressLint("MissingPermission")
    private fun drainQueue() {
        if (opInFlight) return
        val gatt = bluetoothGatt ?: return
        val op = opQueue.poll() ?: return

        opInFlight = true
        when (op) {
            is GattOp.Write -> {
                val characteristic = op.characteristic
                val started = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(
                        characteristic,
                        op.value,
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    characteristic.value = op.value
                    @Suppress("DEPRECATION")
                    characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(characteristic)
                }
                if (!started) {
                    Log.e("BLE", "writeCharacteristic() failed to start for ${characteristic.uuid}")
                    opInFlight = false
                    drainQueue()
                }
            }
            is GattOp.WriteDescriptor -> {
                val descriptor = op.descriptor
                val started = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeDescriptor(
                        descriptor,
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    ) == BluetoothStatusCodes.SUCCESS
                } else {
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    @Suppress("DEPRECATION")
                    gatt.writeDescriptor(descriptor)
                }
                if (!started) {
                    Log.e("BLE", "writeDescriptor() failed to start for ${descriptor.characteristic?.uuid}")
                    opInFlight = false
                    drainQueue()
                }
            }
            is GattOp.ReadRssi -> {
                if (!gatt.readRemoteRssi()) {
                    opInFlight = false
                    drainQueue()
                }
            }
            is GattOp.RequestMtu -> {
                if (!gatt.requestMtu(op.mtu)) {
                    opInFlight = false
                    drainQueue()
                }
            }
        }
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Enable notifications for a characteristic. Queued so it can't race
     * with other in-flight GATT operations.
     */
    private fun enableNotification(characteristic: BluetoothGattCharacteristic) {
        val gatt = bluetoothGatt ?: return
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID) ?: run {
            Log.e("BLE", "CCCD descriptor missing for ${characteristic.uuid}")
            return
        }
        enqueue(GattOp.WriteDescriptor(descriptor))
    }

    /**
     * Clear all characteristic references on disconnect.
     */
    private fun clearCharacteristics() {
        weatherCharacteristic = null
        messageCharacteristic = null
        healthCharacteristic = null
        settingsCharacteristic = null
        replyCharacteristic = null
        telemetryCharacteristic = null
        ledCharacteristic = null
        extCharacteristic = null
        ackCharacteristic = null
        _liveSensorData.value = LiveSensorData()
    }

    // ============================================
    // Send Functions
    // ============================================

    /**
     * Send weather and location data to device.
     * Format: "rain,condition,uv,humidity,lat,lon,locationName,locality,altitude,hour,minute"
     */
    fun sendWeatherData(
        rainChance: Int,
        condition: String,
        uvIndex: Float,
        humidity: Float,
        lat: Double,
        lon: Double,
        locationName: String,
        locality: String,
        altitude: Int,
        hour: Int,
        minute: Int
    ) {
        if (weatherCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE", "Cannot send weather - not connected")
            return
        }

        // Guardian against commas in free-text fields corrupting the
        // firmware's comma-delimited parser (WeatherCallbacks::onWrite
        // splits on ',' with no escaping).
        val safeLocationName = locationName.replace(",", " ")
        val safeLocality = locality.replace(",", " ")
        val safeCondition = condition.replace(",", " ")

        val payload = "$rainChance,$safeCondition,$uvIndex,$humidity,$lat,$lon,$safeLocationName,$safeLocality,$altitude,$hour,$minute"
        writeCharacteristic(weatherCharacteristic!!, payload)
        Log.d("BLE", "Sent weather: $payload")
    }

    /**
     * Send a command (like CMD_FIND).
     */
    fun sendCommand(command: String) {
        if (weatherCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE", "Cannot send command - not connected")
            return
        }
        writeCharacteristic(weatherCharacteristic!!, command)
        Log.d("BLE", "Sent command: $command")
    }

    /**
     * Send a guardian message to the device (Guardian -> Companion
     * direction). Writes to MESSAGE_CHAR_UUID, which the firmware
     * displays on SCREEN_MESSAGE and buzzes for.
     */
    fun sendGuardianMessage(message: String) {
        if (messageCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE_MSG", "Cannot send message - not connected")
            return
        }
        writeCharacteristic(messageCharacteristic!!, message)
        Log.d("BLE_MSG", "Sent message: $message")
    }

    /**
     * Send a quick reply from the device user back to the Guardian
     * (Companion -> Guardian direction). This writes to REPLY_CHAR_UUID,
     * matching how the firmware's own hardware quick-reply flow notifies
     * pReplyChar. Previously the app had no path that wrote to this
     * characteristic at all - "replies" sent from the Companion UI were
     * being written to MESSAGE_CHAR_UUID instead, which the firmware
     * treats as a brand new incoming Guardian message rather than a reply.
     */
    fun sendDeviceReply(reply: String) {
        val characteristic = replyCharacteristic
        if (characteristic == null || bluetoothGatt == null) {
            Log.e("BLE_REPLY", "Cannot send reply - not connected")
            return
        }
        writeCharacteristic(characteristic, reply)
        Log.d("BLE_REPLY", "Sent reply: $reply")
    }

    /**
     * Send health/medical ID data to device.
     * Format: "bloodType,emergencyContact,contactName,allergies,age"
     */
    fun sendHealthData(medicalId: MedicalId) {
        if (healthCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE", "Cannot send health data - not connected or characteristic not found")
            return
        }

        // Guard against commas in free-text fields corrupting the firmware's
        // fixed-count comma-delimited parser, same as sendWeatherData() does.
        val safeBloodType = medicalId.bloodType.replace(",", " ")
        val safeEmergencyContact = medicalId.emergencyContact.replace(",", " ")
        val safeContactName = medicalId.contactName.replace(",", " ")
        val safeAllergies = medicalId.allergies.replace(",", " ")

        val payload = "$safeBloodType,$safeEmergencyContact,$safeContactName,$safeAllergies,${medicalId.age}"
        writeCharacteristic(healthCharacteristic!!, payload)
        Log.d("BLE", "Sent health data: $payload")
    }

    /**
     * Send settings to device.
     * Format: "sensitivity,volume,autoCall,parentalControls,smsFallback" -
     * the last two fields were added so SafetySettings' full field set
     * actually reaches the device (previously only the first 3 did).
     */
    fun sendSettings(settings: SafetySettings) {
        if (settingsCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE", "Cannot send settings - not connected or characteristic not found")
            return
        }

        val sensitivity = when (settings.fallSensitivity) {
            FallSensitivity.LOW -> 0
            FallSensitivity.MEDIUM -> 1
            FallSensitivity.HIGH -> 2
        }
        val payload = "$sensitivity,${(settings.sosVolumeLevel * 100).toInt()},${if (settings.autoCallEmergency) 1 else 0}," +
            "${if (settings.parentalControlsEnabled) 1 else 0},${if (settings.smsFallbackEnabled) 1 else 0}"
        writeCharacteristic(settingsCharacteristic!!, payload)
        Log.d("BLE", "Sent settings: $payload")
    }

    /**
     * Send a generic tagged command to the device's EXT_CHAR_UUID - the
     * shared channel for anything that doesn't warrant its own
     * characteristic (adaptive mode switches, geofence events, reminder
     * scheduling, device name/icon). Wire format "TAG:payload", or just
     * "TAG" if payload is blank.
     */
    fun sendExtCommand(tag: String, payload: String = "") {
        val characteristic = extCharacteristic
        if (characteristic == null || bluetoothGatt == null) {
            Log.e("BLE_EXT", "Cannot send ext command - not connected")
            return
        }
        val value = if (payload.isEmpty()) tag else "$tag:$payload"
        writeCharacteristic(characteristic, value)
        Log.d("BLE_EXT", "Sent ext command: $value")
    }

    /**
     * Send the SMS fallback allowlist (numbers eligible to receive the
     * device's SMS fallback alerts) as a comma-separated list via the
     * generic ext-command channel.
     */
    fun sendSmsAllowlist(numbers: List<String>) {
        sendExtCommand("SMSALLOW", numbers.joinToString(","))
    }

    /**
     * Suspends until the device acknowledges [tag] (or a "tag:detail" ack
     * sharing that tag) via ACK_CHAR_UUID, or [timeoutMs] elapses. Use this
     * right after a settings/health/LED/ext write to show a real "applied
     * on device" confirmation instead of an optimistic one - see
     * ui/components/SharedComponents.kt's AckBadge for the UI half of this.
     */
    suspend fun awaitAck(tag: String, timeoutMs: Long = 4000): Boolean {
        return withTimeoutOrNull(timeoutMs) {
            ackEvents.first { it == tag || it.startsWith("$tag:") }
        } != null
    }

    /**
     * Remotely set the device's WS2812B LED ring pattern - the same
     * patterns already reachable via the physical button, now also
     * reachable from the app (item #13, requires the re-flashed firmware).
     */
    fun sendLedPattern(pattern: LedPattern) {
        val characteristic = ledCharacteristic
        if (characteristic == null || bluetoothGatt == null) {
            Log.e("BLE_LED", "Cannot send LED pattern - not connected")
            return
        }
        writeCharacteristic(characteristic, pattern.wireIndex.toString())
        Log.d("BLE_LED", "Sent LED pattern: ${pattern.label}")
    }

    /**
     * Helper to queue a write to a characteristic. Never writes directly -
     * always goes through the GATT operation queue.
     */
    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: String) {
        enqueue(GattOp.Write(characteristic, value.toByteArray(Charsets.UTF_8)))
    }

    // ============================================
    // Utility Functions
    // ============================================

    /**
     * Get formatted uptime string (MM:SS).
     */
    fun getUptimeString(): String {
        if (connectionTime == 0L) return "00:00"
        val elapsed = (System.currentTimeMillis() - connectionTime) / 1000
        val minutes = elapsed / 60
        val seconds = elapsed % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Request RSSI reading from device. Queued like every other GATT op.
     */
    fun readRssi() {
        if (bluetoothGatt == null) return
        enqueue(GattOp.ReadRssi())
    }

    /**
     * Clear fall alert state.
     */
    fun clearAlert() {
        _fallAlert.value = false
    }

    /**
     * Clear device reply state after processing.
     */
    fun clearReply() {
        _deviceReply.value = null
    }

    /**
     * Check if currently connected.
     */
    fun isConnected(): Boolean {
        return _connectionState.value == "Connected"
    }

    /**
     * Disconnect from device and clean up.
     */
    fun disconnect() {
        stopScanning()
        userInitiatedDisconnect = true
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        clearCharacteristics()
        opQueue.clear()
        opInFlight = false
    }
}
