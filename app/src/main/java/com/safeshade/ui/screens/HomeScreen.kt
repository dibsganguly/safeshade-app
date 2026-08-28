/**
 * SafeShade - Universal Safety Companion
 *
 * HomeScreen.kt
 *
 * Main dashboard screen showing device status, weather information,
 * and quick sync functionality.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.ui.screens

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.BleManager
import com.safeshade.data.DeviceSettings
import com.safeshade.data.WeatherUiState
import com.safeshade.ui.components.BouncyButton
import com.safeshade.ui.components.InfoCard
import com.safeshade.ui.theme.*
import com.safeshade.data.*
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*


/**
 * Home screen - Main dashboard of the app.
 *
 * Displays:
 * - Device connection status with dynamic icon
 * - Weather information cards (rain, UV, temp, battery)
 * - Sync button to fetch and send weather data
 *
 * @param bleManager BLE manager for connection state
 * @param weather Current weather data
 * @param deviceSettings Device configuration (icon, name)
 * @param onSyncWeather Callback to trigger weather sync
 */
@Composable
fun HomeScreen(
    bleManager: BleManager,
    weather: WeatherUiState,
    deviceSettings: DeviceSettings,
    onSyncWeather: () -> Unit
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
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
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Switch(
                checked = isConnected,
                onCheckedChange = { if (it) bleManager.startScanning() },
                colors = SwitchDefaults.colors(checkedTrackColor = AccentPurple)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ============================================
        // DEVICE CARD
        // Shows device info with dynamic icon based on settings
        // ============================================
        DeviceStatusCard(
            deviceSettings = deviceSettings,
            connectionState = connectionState,
            isConnected = isConnected
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ============================================
        // WEATHER INFO GRID
        // ============================================
        WeatherInfoGrid(weather = weather)

        Spacer(modifier = Modifier.height(8.dp))

        // Last sync timestamp
        if (weather.isLoaded) {
            Text(
                text = "Last synced: ${weather.lastSyncTime}",
                fontSize = 12.sp,
                color = TextGray,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ============================================
        // SYNC BUTTON
        // ============================================
        BouncyButton(
            onClick = onSyncWeather,
            enabled = isConnected,
            color = AccentOrange,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Sync, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Sync Weather & GPS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
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
    isConnected: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic device icon
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(AccentPurple),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceSettings.iconType.icon,
                    contentDescription = deviceSettings.iconType.label,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceSettings.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "${deviceSettings.primaryUserName}'s ${deviceSettings.iconType.label}",
                    fontSize = 12.sp,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Connection status indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AccentGreen else AccentOrange)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isConnected) "Connected" else connectionState,
                        fontSize = 13.sp,
                        color = if (isConnected) AccentGreen else AccentOrange,
                        fontWeight = FontWeight.SemiBold
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
private fun WeatherInfoGrid(weather: WeatherUiState) {
    // Row 1: Rain & UV
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WaterDrop,
            iconColor = AccentBlue,
            label = "Rain Chance",
            value = if (weather.isLoaded) "${weather.rainChance}%" else "--%"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.WbSunny,
            iconColor = AccentOrange,
            label = "UV Index",
            value = if (weather.isLoaded) String.format("%.1f", weather.uvIndex) else "--"
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Row 2: Temperature & Battery
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Thermostat,
            iconColor = AccentRed,
            label = "Temperature",
            value = if (weather.isLoaded) "${weather.temp.toInt()}°C" else "--°C"
        )
        InfoCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.BatteryChargingFull,
            iconColor = AccentGreen,
            label = "Battery",
            value = "85%"  // TODO: Get from device
        )
    }
}
