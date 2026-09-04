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
 *  - Added a real `isSyncing` param driving the sync button's loading state.
 *    NOTE for the wiring side (not done in this file): MainActivity.fetchAndSendWeather()
 *    needs to set a real `syncInProgress` state to true right when the weather-fetch
 *    coroutine starts, and back to false in BOTH the success path and the
 *    failure/catch path - never leave it stuck true on error. That state then needs
 *    to be threaded through SafeShadeApp.kt and passed into this composable's new
 *    `isSyncing` parameter. This file only consumes `isSyncing`; it does not create it.
 *  - Replaced the small ActiveModeChip pill with a full-width ActiveModeBanner card,
 *    moved below the Sync button, using PersonaMode.accentColor for a distinct
 *    per-mode look (icon chip + label + description).
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
 * @param isSyncing Whether a weather/GPS sync is currently in flight (drives the Sync
 *   button's loading spinner + disabled state). Must be backed by a real upstream state -
 *   see the FIXES doc comment at the top of this file.
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
    liveSensorData: LiveSensorData,
    isSyncing: Boolean = false
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
            enabled = isConnected && !isSyncing,
            color = colors.accentWarning,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (simplifiedUi) 68.dp else 60.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSyncing) "Syncing..." else "Sync Weather & GPS",
                    style = if (simplifiedUi) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // ============================================
        // ACTIVE MODE BANNER
        // ============================================
        ActiveModeBanner(activeMode = activeMode)
    }
}

/**
 * Prominent card surfacing the currently active adaptive persona mode -
 * icon chip + mode name + short description, accented in the mode's own
 * distinct PersonaMode.accentColor so each mode reads as visually distinct
 * (Elderly blue, Kids orange, Bike green, etc.) rather than sharing one flat
 * accent like the old ActiveModeChip pill did. The full mode-picker UI still
 * lives on ProfileScreen - this is just a visible indicator, sized to carry
 * roughly the same visual weight as DeviceStatusCard.
 */
@Composable
private fun ActiveModeBanner(activeMode: PersonaMode) {
    val colors = MaterialTheme.safeShadeColors
    val accent = activeMode.accentColor

    GlassCard(shape = RoundedCornerShape(Radius.xl), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top-edge accent bar in the mode's own color.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(accent)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = activeMode.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${activeMode.label} Mode",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activeMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }
            }
        }
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
