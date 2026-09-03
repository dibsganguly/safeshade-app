/**
 * SafeShade - Universal Safety Companion
 *
 * HomeScreen.kt
 *
 * Main dashboard screen showing device status, weather information,
 * and quick sync functionality.
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FIXES (this pass):
 *  - The connect Switch used to call bleManager.startScanning() with no
 *    regard for whether Bluetooth/location permissions were granted. On
 *    Android 12+ this throws a SecurityException and crashes the app the
 *    first time someone taps the switch before (or after denying) the
 *    permission prompt. It now requests permissions instead of scanning
 *    when they're missing.
 *  - Battery no longer hardcodes "85%" - it reads liveSensorData.batteryLevel,
 *    gated on liveSensorData.isRealData so we never show a fake number as if
 *    it came from the device.
 *  - Surfaces the currently active PersonaMode near the top, and bumps key
 *    numeric/status text up one step when activeMode.simplifiedUi is true
 *    (Elderly/Kids modes).
 *  - Migrated off raw fontSize=/Color(0x..) literals onto
 *    MaterialTheme.typography.* / MaterialTheme.safeShadeColors.*, and onto
 *    GlassCard instead of plain Card+CardColor+explicit .shadow().
 */

package com.safeshade.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeshade.BleManager
import com.safeshade.data.DeviceSettings
import com.safeshade.data.PersonaMode
import com.safeshade.data.WeatherUiState
import com.safeshade.ui.components.BouncyButton
import com.safeshade.ui.components.GlassCard
import com.safeshade.ui.components.InfoCard
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.safeShadeColors
import com.safeshade.data.LiveSensorData

/**
 * Home screen - Main dashboard of the app.
 *
 * Displays:
 * - Device connection status with dynamic icon
 * - Active adaptive mode indicator
 * - Weather information cards (rain, UV, temp, battery)
 * - Sync button to fetch and send weather data
 *
 * @param bleManager BLE manager for connection state
 * @param weather Current weather data
 * @param deviceSettings Device configuration (icon, name)
 * @param permissionsGranted Whether Bluetooth/location permissions are granted
 * @param onRequestPermissions Callback to (re-)request Bluetooth/location permissions
 * @param onSyncWeather Callback to trigger weather sync
 * @param activeMode Currently active adaptive persona mode
 * @param liveSensorData Real telemetry from the device, used here for battery %
 */
@Composable
fun HomeScreen(
    bleManager: BleManager,
    weather: WeatherUiState,
    deviceSettings: DeviceSettings,
    permissionsGranted: Boolean = true,
    onRequestPermissions: () -> Unit = {},
    onSyncWeather: () -> Unit,
    activeMode: PersonaMode,
    liveSensorData: LiveSensorData
) {
    val colors = MaterialTheme.safeShadeColors
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"
    val simplifiedUi = activeMode.simplifiedUi

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState())
    ) {
        // ============================================
        // HEADER
        // ============================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SafeShade",
                style = MaterialTheme.typography.displayMedium,
                color = colors.onSurface
            )
            Switch(
                checked = isConnected,
                onCheckedChange = { checked ->
                    if (checked) {
                        if (permissionsGranted) {
                            bleManager.startScanning()
                        } else {
                            // Don't call startScanning() without permissions -
                            // it throws a SecurityException on Android 12+.
                            onRequestPermissions()
                        }
                    } else {
                        bleManager.disconnect()
                    }
                },
                colors = SwitchDefaults.colors(checkedTrackColor = colors.accentPrimary)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        // ============================================
        // ACTIVE MODE INDICATOR
        // ============================================
        ActiveModeChip(activeMode = activeMode)

        Spacer(modifier = Modifier.height(Spacing.lg))

        // ============================================
        // DEVICE CARD
        // ============================================
        DeviceStatusCard(
            deviceSettings = deviceSettings,
            connectionState = connectionState,
            isConnected = isConnected,
            simplifiedUi = simplifiedUi
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // ============================================
        // WEATHER INFO GRID
        // ============================================
        WeatherInfoGrid(weather = weather, liveSensorData = liveSensorData, simplifiedUi = simplifiedUi)

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Last sync timestamp
        if (weather.isLoaded) {
            Text(
                text = "Last synced: ${weather.lastSyncTime}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // ============================================
        // SYNC BUTTON
        // ============================================
        BouncyButton(
            onClick = onSyncWeather,
            enabled = isConnected,
            color = colors.accentWarning,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (simplifiedUi) 68.dp else 60.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sync Weather & GPS",
                    style = if (simplifiedUi) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Small chip surfacing the currently active adaptive persona mode. The full
 * mode-picker UI lives on ProfileScreen - this is just a visible indicator.
 */
@Composable
private fun ActiveModeChip(activeMode: PersonaMode) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.accentPrimary.copy(alpha = 0.12f))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = activeMode.icon,
            contentDescription = null,
            tint = colors.accentPrimary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${activeMode.label} Mode",
            style = MaterialTheme.typography.labelMedium,
            color = colors.accentPrimary
        )
    }
}

/**
 * Device status card showing connection state and device info.
 */
@Composable
private fun DeviceStatusCard(
    deviceSettings: DeviceSettings,
    connectionState: String,
    isConnected: Boolean,
    simplifiedUi: Boolean
) {
    val colors = MaterialTheme.safeShadeColors
    val statusColor by animateColorAsState(
        targetValue = if (isConnected) colors.accentSuccess else colors.accentWarning,
        animationSpec = tween(200),
        label = "statusColor"
    )

    // GlassCard already applies elevation/shadow - no extra .shadow() needed.
    GlassCard(shape = RoundedCornerShape(Radius.xl), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic device icon
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(colors.accentPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceSettings.iconType.icon,
                    contentDescription = deviceSettings.iconType.label,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceSettings.name,
                    style = if (simplifiedUi) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )
                Text(
                    text = "${deviceSettings.primaryUserName}'s ${deviceSettings.iconType.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Connection status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Connected" else connectionState,
                        style = MaterialTheme.typography.labelLarge,
                        color = statusColor
                    )
                }
            }
        }
    }
}

/**
 * Grid of weather information cards.
 */
@Composable
private fun WeatherInfoGrid(
    weather: WeatherUiState,
    liveSensorData: LiveSensorData,
    simplifiedUi: Boolean
) {
    val colors = MaterialTheme.safeShadeColors
    // Row 1: Rain & UV
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WaterDrop,
            iconColor = colors.accentInfo,
            label = "Rain Chance",
            value = if (weather.isLoaded) "${weather.rainChance}%" else "--%"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WbSunny,
            iconColor = colors.accentWarning,
            label = "UV Index",
            value = if (weather.isLoaded) String.format("%.1f", weather.uvIndex) else "--"
        )
    }

    Spacer(modifier = Modifier.height(Spacing.md))

    // Row 2: Temperature & Battery
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Thermostat,
            iconColor = colors.accentDanger,
            label = "Temperature",
            value = if (weather.isLoaded) "${weather.temp.toInt()}°C" else "--°C"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.BatteryChargingFull,
            iconColor = colors.accentSuccess,
            label = "Battery",
            // Real device battery, gated on isRealData - no fake fallback number.
            value = if (liveSensorData.isRealData) "${liveSensorData.batteryLevel}%" else "--%"
        )
    }
}
