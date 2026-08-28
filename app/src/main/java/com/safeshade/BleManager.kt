/**
 * SafeShade - Universal Safety Companion
 *
 * BleManager.kt
 *
 * Handles all Bluetooth Low Energy communication with the SafeShade device.
 * Manages scanning, connection, data transmission, and notifications.
 *
 * @author SafeShade Team
 * @version 2.1.0
 */

package com.safeshade

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.safeshade.data.FallSensitivity
import com.safeshade.data.MedicalId
import com.safeshade.data.SafetySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

// Standard descriptor UUID for enabling notifications
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

/**
 * BLE Manager class for SafeShade device communication.
 *
 * Handles:
 * - Device scanning and connection
 * - Sending weather, health, and settings data
 * - Receiving fall alerts and device replies
 * - Connection state management
 */
@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private var bluetoothGatt: BluetoothGatt? = null

    // ============================================
    // ALL Characteristic References
    // ============================================
    private var weatherCharacteristic: BluetoothGattCharacteristic? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var healthCharacteristic: BluetoothGattCharacteristic? = null
    private var settingsCharacteristic: BluetoothGattCharacteristic? = null
    private var replyCharacteristic: BluetoothGattCharacteristic? = null

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

    private var connectionTime: Long = 0

    // ============================================
    // Scanning
    // ============================================

    /**
     * Start scanning for SafeShade devices.
     *
     * @param onFound Callback when device is found
     */
    @SuppressLint("MissingPermission")
    fun startScanning(onFound: () -> Unit = {}) {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
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
                    _connectionState.value = "Found ${deviceNameResult}!"
                    _deviceName.value = deviceNameResult
                    _deviceAddress.value = device.address
                    _rssi.value = result?.rssi ?: -100
                    scanner.stopScan(this)
                    connectToDevice(device)
                    onFound()
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _connectionState.value = "Scan Failed: $errorCode"
                Log.e("BLE_SCAN", "Scan Failed: $errorCode")
            }
        }

        scanner.startScan(listOf(filter), settings, scanCallback)
    }

    // ============================================
    // Connection
    // ============================================

    private fun connectToDevice(device: BluetoothDevice) {
        _connectionState.value = "Connecting..."
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
                    gatt?.discoverServices()
                    gatt?.readRemoteRssi()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = "Disconnected"
                    connectionTime = 0
                    clearCharacteristics()
                    startScanning()
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssi
            }
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

            Log.d("BLE", "Characteristics discovered:")
            Log.d("BLE", "  Weather: ${weatherCharacteristic != null}")
            Log.d("BLE", "  Message: ${messageCharacteristic != null}")
            Log.d("BLE", "  Health: ${healthCharacteristic != null}")
            Log.d("BLE", "  Settings: ${settingsCharacteristic != null}")
            Log.d("BLE", "  Reply: ${replyCharacteristic != null}")

            // Enable notifications for ALERT characteristic
            val alertChar = service.getCharacteristic(ALERT_CHAR_UUID)
            if (alertChar != null) {
                enableNotification(gatt, alertChar)
            }

            // Enable notifications for REPLY characteristic
            // Use delayed execution to avoid BLE queue issues
            if (replyCharacteristic != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    enableNotification(gatt, replyCharacteristic!!)
                    Log.d("BLE", "Reply notifications enabled")
                }, 500)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = characteristic.value?.let { String(it) } ?: return

            when (characteristic.uuid) {
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
        }
    }

    // ============================================
    // Helper Functions
    // ============================================

    /**
     * Enable notifications for a characteristic.
     */
    private fun enableNotification(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
        if (gatt == null) return

        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
        }
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

        val payload = "$rainChance,$condition,$uvIndex,$humidity,$lat,$lon,$locationName,$locality,$altitude,$hour,$minute"
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
     * Send guardian message to device.
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
     * Send health/medical ID data to device.
     * Format: "bloodType,emergencyContact,contactName,allergies,age"
     */
    fun sendHealthData(medicalId: MedicalId) {
        if (healthCharacteristic == null || bluetoothGatt == null) {
            Log.e("BLE", "Cannot send health data - not connected or characteristic not found")
            return
        }

        val payload = "${medicalId.bloodType},${medicalId.emergencyContact},${medicalId.contactName},${medicalId.allergies},${medicalId.age}"
        writeCharacteristic(healthCharacteristic!!, payload)
        Log.d("BLE", "Sent health data: $payload")
    }

    /**
     * Send settings to device.
     * Format: "sensitivity,volume,autoCall"
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
        val payload = "$sensitivity,${(settings.sosVolumeLevel * 100).toInt()},${if (settings.autoCallEmergency) 1 else 0}"
        writeCharacteristic(settingsCharacteristic!!, payload)
        Log.d("BLE", "Sent settings: $payload")
    }

    /**
     * Helper to write to a characteristic.
     */
    private fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: String) {
        characteristic.value = value.toByteArray(Charsets.UTF_8)
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        bluetoothGatt?.writeCharacteristic(characteristic)
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
     * Request RSSI reading from device.
     */
    fun readRssi() {
        bluetoothGatt?.readRemoteRssi()
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
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        clearCharacteristics()
    }
}
