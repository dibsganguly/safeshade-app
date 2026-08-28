/**
 * SafeShade - Universal Safety Companion
 *
 * DeviceScreen.kt
 *
 * Device information and live sensor data display screen.
 * Shows connection stats, device info, and real-time sensor readings.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.BleManager
import com.safeshade.data.LiveSensorData
import com.safeshade.ui.theme.*
import kotlinx.coroutines.delay
import com.safeshade.data.*
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*

/**
 * Device screen - Shows device info and live sensor data.
 *
 * Features:
 * - Device information (name, MAC, firmware)
 * - Connection statistics (status, RSSI, uptime)
 * - Live sensor readings with animations
 *
 * @param bleManager BLE manager for device data
 * @param liveSensorData Current sensor readings
 * @param onSensorUpdate Callback when sensor data updates
 */
@Composable
fun DeviceScreen(
    bleManager: BleManager,
    liveSensorData: LiveSensorData,
    onSensorUpdate: (LiveSensorData) -> Unit
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val deviceName by bleManager.deviceName.collectAsState()
    val deviceAddress by bleManager.deviceAddress.collectAsState()
    val rssi by bleManager.rssi.collectAsState()
    val isConnected = connectionState == "Connected"

    var uptime by remember { mutableStateOf("00:00") }

    // ============================================
    // LIVE DATA UPDATE LOOP
    // Updates sensor data every 500ms when connected
    // ============================================
    LaunchedEffect(isConnected) {
        while (isConnected) {
            uptime = bleManager.getUptimeString()
            bleManager.readRssi()

            // Simulate sensor updates (replace with actual BLE reads in production)
            onSensorUpdate(
                liveSensorData.copy(
                    accelX = (-0.5f + Math.random().toFloat()).coerceIn(-1f, 1f),
                    accelY = (-0.2f + Math.random().toFloat() * 0.4f).coerceIn(-1f, 1f),
                    accelZ = (0.9f + Math.random().toFloat() * 0.2f).coerceIn(-1f, 1f),
                    temperature = 24f + Math.random().toFloat() * 2f,
                    lightLevel = (70 + (Math.random() * 20).toInt()).coerceIn(0, 100),
                    batteryLevel = maxOf(
                        liveSensorData.batteryLevel - if (Math.random() > 0.95) 1 else 0,
                        0
                    )
                )
            )

            delay(500)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Device", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(20.dp))

        // Device Info Card
        DeviceInfoCard(deviceName = deviceName, deviceAddress = deviceAddress)

        Spacer(modifier = Modifier.height(16.dp))

        // Connection Stats Card
        ConnectionStatsCard(
            isConnected = isConnected,
            rssi = rssi,
            uptime = uptime
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Sensor Data Card
        LiveSensorCard(
            isConnected = isConnected,
            sensorData = liveSensorData
        )
    }
}

/**
 * Device information card showing static device details.
 */
@Composable
private fun DeviceInfoCard(
    deviceName: String,
    deviceAddress: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Device Information", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(16.dp))

            DeviceInfoRow("Name", deviceName)
            DeviceInfoRow("MAC Address", deviceAddress)
            DeviceInfoRow("Firmware", "v6.0.0")
            DeviceInfoRow("Hardware", "ESP32-C3 Rev 4")
        }
    }
}

/**
 * Connection statistics card.
 */
@Composable
private fun ConnectionStatsCard(
    isConnected: Boolean,
    rssi: Int,
    uptime: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Connection Stats", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(16.dp))

            DeviceInfoRow("Status", if (isConnected) "Connected" else "Disconnected")
            DeviceInfoRow("Signal Strength", "$rssi dBm")
            DeviceInfoRow("Session Uptime", uptime)
            DeviceInfoRow("Protocol", "BLE 5.0")
        }
    }
}

/**
 * Live sensor data card with animated values.
 */
@Composable
private fun LiveSensorCard(
    isConnected: Boolean,
    sensorData: LiveSensorData
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header with live indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Sensors", fontWeight = FontWeight.Bold, color = TextDark)

                if (isConnected) {
                    LiveIndicator()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sensor readings
            AnimatedSensorRow(
                label = "Accelerometer X",
                value = String.format("%.2f G", sensorData.accelX),
                color = AccentBlue
            )
            AnimatedSensorRow(
                label = "Accelerometer Y",
                value = String.format("%.2f G", sensorData.accelY),
                color = AccentBlue
            )
            AnimatedSensorRow(
                label = "Accelerometer Z",
                value = String.format("%.2f G", sensorData.accelZ),
                color = AccentBlue
            )
            AnimatedSensorRow(
                label = "Temperature",
                value = String.format("%.1f°C", sensorData.temperature),
                color = AccentOrange
            )
            AnimatedSensorRow(
                label = "Light Level",
                value = "${sensorData.lightLevel}%",
                color = AccentPurple
            )
            AnimatedSensorRow(
                label = "Battery",
                value = "${sensorData.batteryLevel}%",
                color = AccentGreen
            )
        }
    }
}

/**
 * Animated "LIVE" indicator with pulsing effect.
 */
@Composable
private fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .background(AccentGreen.copy(alpha = alpha * 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(AccentGreen.copy(alpha = alpha))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "LIVE",
                fontSize = 10.sp,
                color = AccentGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Single device info row.
 */
@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(value, color = TextDark, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Animated sensor value row.
 */
@Composable
fun AnimatedSensorRow(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextGray, fontSize = 14.sp)
        Text(
            value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
