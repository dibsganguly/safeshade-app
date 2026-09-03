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
 *  - Added remote LED pattern control (item #13): a 7-chip grid mirroring the
 *    firmware's physical-button patterns, writes to LED_CHAR_UUID via
 *    bleManager.sendLedPattern(). LedControlCard now shows a real AckBadge
 *    (ACK_CHAR_UUID round trip) instead of the optimistic-only state it used
 *    to - the "selected" chip itself is still local UI state, but whether it
 *    actually applied is now device-confirmed, not assumed.
 */

package com.safeshade.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.safeshade.data.LedPattern
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

        LedControlCard(
            bleManager = bleManager,
            enabled = isConnected,
            onPatternSelected = { bleManager.sendLedPattern(it) }
        )
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
            Text("Device Information", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
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
            Text("Connection Stats", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
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
                Text("Live Sensors", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)

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
 * Remote LED pattern control - a 7-chip grid mirroring the firmware's
 * RGBPattern enum. Selection is optimistic local UI state only; there's no
 * device readback to confirm the pattern actually took, so we show a brief
 * "sent" pulse on tap rather than claiming confirmed sync.
 */
@Composable
private fun LedControlCard(
    bleManager: BleManager,
    enabled: Boolean,
    onPatternSelected: (LedPattern) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    var selected by remember { mutableStateOf<LedPattern?>(null) }
    var justSent by remember { mutableStateOf<LedPattern?>(null) }
    var ackSeq by remember { mutableStateOf(0) }

    LaunchedEffect(justSent) {
        if (justSent != null) {
            delay(900)
            justSent = null
        }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("LED Pattern", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = colors.accentWarning,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xs))
            // Real on-device confirmation now (was previously optimistic-only
            // with no readback at all - see the class doc comment history).
            // Tag is value-qualified ("LED:TORCH" not just "LED") - with the
            // generic tag, rapidly tapping two different patterns could let
            // the first (slower) send's stale ack satisfy the second
            // pattern's badge, showing "SYNCED" for the wrong value (found
            // in review).
            com.safeshade.ui.components.AckBadge(
                bleManager = bleManager,
                tag = selected?.let { "LED:${it.name}" } ?: "",
                trigger = ackSeq.takeIf { it > 0 }
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.heightIn(max = 260.dp)
            ) {
                items(LedPattern.entries) { pattern ->
                    LedPatternChip(
                        pattern = pattern,
                        isSelected = selected == pattern,
                        justSent = justSent == pattern,
                        enabled = enabled,
                        onClick = {
                            selected = pattern
                            justSent = pattern
                            ackSeq++
                            onPatternSelected(pattern)
                        }
                    )
                }
            }

            if (!enabled) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    "Connect to the device to control the LED ring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
            }
        }
    }
}

@Composable
private fun LedPatternChip(
    pattern: LedPattern,
    isSelected: Boolean,
    justSent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "ledChipBg"
    )
    val background = colors.accentPrimary.copy(alpha = 0.12f + 0.10f * bgAlpha)
    val borderColor = if (isSelected) colors.accentPrimary else colors.borderGlass

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(background)
            .border(1.dp, borderColor, RoundedCornerShape(Radius.sm))
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = Spacing.md, horizontal = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            pattern.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) colors.accentPrimary else colors.onSurface,
            fontWeight = FontWeight.Bold
        )
        AnimatedVisibility(visible = justSent) {
            Text(
                "sent",
                style = MaterialTheme.typography.labelSmall,
                color = colors.accentSuccess
            )
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
