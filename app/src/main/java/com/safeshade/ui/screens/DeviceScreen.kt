/**
 * SafeShade - Universal Safety Companion
 *
 * DeviceScreen.kt
 *
 * Device information and live sensor data display screen.
 * Shows connection stats, device info, and real-time sensor readings.
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FIXES (this pass):
 *  - Removed the old Math.random()-driven fake sensor simulation entirely.
 *    liveSensorData now comes straight from BleManager's real TELEMETRY_CHAR_UUID
 *    notify stream (parsed real MPU6050 accel/temp/light/battery).
 *  - The "LIVE" badge and battery reading are now gated on
 *    liveSensorData.isRealData so we never present simulated/stale numbers as
 *    if they were fresh from the device.
 *  - LedControlCard (remote LED pattern control) moved to ProfileScreen.kt -
 *    remote customization now lives together there (Adaptive Mode + LED),
 *    leaving this screen as the read-only device/telemetry view. AppInfoCard
 *    moved the other direction, from ProfileScreen.kt, and now lives here
 *    alongside the rest of the device-facing information.
 */

package com.safeshade.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safeshade.BleManager
import com.safeshade.BuildConfig
import com.safeshade.data.LiveSensorData
import com.safeshade.ui.components.GlassCard
import com.safeshade.ui.components.LiveDot
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.safeShadeColors
import kotlinx.coroutines.delay

/**
 * Device screen - Shows device info and live sensor data.
 *
 * @param bleManager BLE manager for device data and control
 * @param liveSensorData Real telemetry pushed in from BleManager.liveSensorData
 */
@Composable
fun DeviceScreen(
    bleManager: BleManager,
    liveSensorData: LiveSensorData
) {
    val colors = MaterialTheme.safeShadeColors
    val connectionState by bleManager.connectionState.collectAsState()
    val deviceName by bleManager.deviceName.collectAsState()
    val deviceAddress by bleManager.deviceAddress.collectAsState()
    val rssi by bleManager.rssi.collectAsState()
    val isConnected = connectionState == "Connected"

    var uptime by remember { mutableStateOf("00:00") }

    // Periodically refresh uptime/RSSI while connected. No more fake sensor
    // generation here - liveSensorData now flows in from real BLE telemetry.
    LaunchedEffect(isConnected) {
        while (isConnected) {
            uptime = bleManager.getUptimeString()
            bleManager.readRssi()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Device", style = MaterialTheme.typography.displayMedium, color = colors.onSurface)
        Spacer(modifier = Modifier.height(Spacing.xl))

        DeviceInfoCard(deviceName = deviceName, deviceAddress = deviceAddress)

        Spacer(modifier = Modifier.height(Spacing.lg))

        ConnectionStatsCard(
            isConnected = isConnected,
            rssi = rssi,
            uptime = uptime
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        LiveSensorCard(sensorData = liveSensorData)

        Spacer(modifier = Modifier.height(Spacing.lg))

        AppInfoCard()

        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

/**
 * Device information card showing static device details.
 *
 * NOTE: "Firmware" and "Hardware" are static nominal values, not read from
 * the device - the firmware doesn't expose a version-string characteristic.
 * They describe the reference hardware/firmware SafeShade ships, not a live
 * readback, so they never change even across sessions/devices.
 */
@Composable
private fun DeviceInfoCard(
    deviceName: String,
    deviceAddress: String
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Device Information", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            DeviceInfoRow("Name", deviceName)
            DeviceInfoRow("MAC Address", deviceAddress)
            // Static nominal values - not read from the device each session.
            DeviceInfoRow("Firmware (nominal)", "v6.0.0")
            DeviceInfoRow("Hardware (nominal)", "ESP32-C3 Rev 4")
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
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.NetworkCheck,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Connection Stats", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            DeviceInfoRow("Status", if (isConnected) "Connected" else "Disconnected")
            DeviceInfoRow("Signal Strength", "$rssi dBm")
            DeviceInfoRow("Session Uptime", uptime)
            // Static nominal value - the firmware doesn't expose a negotiated
            // PHY/version characteristic to read this from live.
            DeviceInfoRow("Protocol (nominal)", "BLE 5.0")
        }
    }
}

/**
 * Live sensor data card with animated values. Only shows the pulsing
 * LIVE badge once liveSensorData.isRealData is true - i.e. at least one
 * genuine BLE telemetry notification has arrived this session.
 */
@Composable
private fun LiveSensorCard(sensorData: LiveSensorData) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl).animateContentSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Sensors,
                        contentDescription = null,
                        tint = colors.accentPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("Live Sensors", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                }

                if (sensorData.isRealData) {
                    LiveIndicator()
                } else {
                    WaitingForDataBadge()
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            if (!sensorData.isRealData) {
                Text(
                    "Waiting for live telemetry from the device...",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
                Spacer(modifier = Modifier.height(Spacing.md))
            }

            val placeholder = "--"
            AnimatedSensorRow(
                label = "Accelerometer X",
                value = if (sensorData.isRealData) String.format("%.2f G", sensorData.accelX) else placeholder,
                color = colors.accentInfo
            )
            AnimatedSensorRow(
                label = "Accelerometer Y",
                value = if (sensorData.isRealData) String.format("%.2f G", sensorData.accelY) else placeholder,
                color = colors.accentInfo
            )
            AnimatedSensorRow(
                label = "Accelerometer Z",
                value = if (sensorData.isRealData) String.format("%.2f G", sensorData.accelZ) else placeholder,
                color = colors.accentInfo
            )
            AnimatedSensorRow(
                label = "Temperature",
                value = if (sensorData.isRealData) String.format("%.1f°C", sensorData.temperature) else "$placeholder°C",
                color = colors.accentWarning
            )
            AnimatedSensorRow(
                label = "Light (raw)",
                value = if (sensorData.isRealData) "${sensorData.lightLevel}" else placeholder,
                color = colors.accentSecondary
            )
            AnimatedSensorRow(
                label = "Battery",
                value = if (sensorData.isRealData) "${sensorData.batteryLevel}%" else "$placeholder%",
                color = colors.accentSuccess
            )
        }
    }
}

/**
 * App information card - relocated here from ProfileScreen.kt so the
 * app-identity summary sits with the rest of the device/telemetry
 * information rather than the customization-heavy Profile screen. Expanded
 * beyond plain Version/Build with the app name and its tagline (the same
 * "Universal Safety Companion" phrase already used as this file's header
 * comment) - Version/Build still read live from BuildConfig, never
 * hardcoded.
 */
@Composable
private fun AppInfoCard() {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("App Information", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            Text("SafeShade", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text("Universal Safety Companion", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceMuted)

            Spacer(modifier = Modifier.height(Spacing.lg))

            DeviceInfoRow("Version", BuildConfig.VERSION_NAME)
            DeviceInfoRow("Build", BuildConfig.BUILD_TYPE)
        }
    }
}

/**
 * Animated "LIVE" indicator with pulsing effect - only ever shown once real
 * telemetry has actually arrived (see LiveSensorCard).
 */
@Composable
private fun LiveIndicator() {
    val colors = MaterialTheme.safeShadeColors
    Box(
        modifier = Modifier
            .background(colors.accentSuccess.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveDot(color = colors.accentSuccess, size = 6.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accentSuccess
            )
        }
    }
}

/** Neutral, non-pulsing placeholder shown while no real telemetry has arrived yet. */
@Composable
private fun WaitingForDataBadge() {
    val colors = MaterialTheme.safeShadeColors
    Box(
        modifier = Modifier
            .background(colors.onSurfaceFaint.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(colors.onSurfaceFaint)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "WAITING",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceMuted
            )
        }
    }
}

/**
 * Single device info row.
 */
@Composable
fun DeviceInfoRow(label: String, value: String) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs + 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface, fontWeight = FontWeight.Medium)
    }
}

/**
 * Animated sensor value row - cross-fades when the value string changes.
 */
@Composable
fun AnimatedSensorRow(
    label: String,
    value: String,
    color: Color
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs + 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceMuted)
        AnimatedContent(
            targetState = value,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "sensorValue"
        ) { animatedValue ->
            Text(
                animatedValue,
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
