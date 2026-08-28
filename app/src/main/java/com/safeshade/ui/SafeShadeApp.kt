/**
 * SafeShade - Universal Safety Companion
 *
 * SafeShadeApp.kt
 *
 * Main application composable that sets up navigation, state management,
 * and handles global events like fall alerts and device replies.
 *
 * @author SafeShade Team
 * @version 2.1.0
 */

package com.safeshade.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safeshade.BleManager
import com.safeshade.data.*
import com.safeshade.ui.navigation.SafeShadeBottomBar
import com.safeshade.ui.screens.*
import com.safeshade.ui.theme.*

/**
 * Main application composable.
 *
 * Handles:
 * - Navigation between screens
 * - Global state management
 * - Fall alert dialog
 * - Device reply processing (real-time)
 * - BLE data synchronization
 *
 * @param bleManager BLE manager for device communication
 * @param weather Current weather state
 * @param location Current location state
 * @param onSyncWeather Callback to trigger weather sync
 */
@Composable
fun SafeShadeApp(
    bleManager: BleManager,
    weather: WeatherUiState,
    location: LocationState,
    onSyncWeather: () -> Unit
) {
    val navController = rememberNavController()
    val fallAlert by bleManager.fallAlert.collectAsState()

    // ============================================
    // APP STATE
    // ============================================
    var deviceSettings by remember { mutableStateOf(DeviceSettings()) }
    var medicalId by remember { mutableStateOf(MedicalId()) }
    var safetySettings by remember { mutableStateOf(SafetySettings()) }
    var fallHistory by remember { mutableStateOf(listOf<FallAlertEvent>()) }
    var messageHistory by remember { mutableStateOf(listOf<QuickMessage>()) }
    var isGuardianMode by remember { mutableStateOf(true) }
    var liveSensorData by remember { mutableStateOf(LiveSensorData()) }

    // ============================================
    // DEVICE REPLY OBSERVER
    // Processes quick replies received from device in REAL-TIME
    // ============================================
    val deviceReply by bleManager.deviceReply.collectAsState()

    LaunchedEffect(deviceReply) {
        deviceReply?.let { reply ->
            // Add reply from device to message history
            // fromGuardian = false indicates this is FROM the device user
            val newMessage = QuickMessage(
                text = reply,
                fromGuardian = false,  // Device user sent this
                timestamp = System.currentTimeMillis()
            )
            messageHistory = listOf(newMessage) + messageHistory

            // Clear the reply state after processing
            bleManager.clearReply()
        }
    }

    // ============================================
    // CONNECTION STATE OBSERVER
    // Syncs data when device connects
    // ============================================
    val connectionState by bleManager.connectionState.collectAsState()

    LaunchedEffect(connectionState) {
        if (connectionState == "Connected") {
            bleManager.sendHealthData(medicalId)
            bleManager.sendSettings(safetySettings)
        }
    }

    // ============================================
    // FALL ALERT LOGGING
    // Records fall events to history
    // ============================================
    LaunchedEffect(fallAlert) {
        if (fallAlert) {
            val newEvent = FallAlertEvent(
                eventType = "Fall detected",
                action = "Awaiting dismissal",
                wasEmergencyContacted = safetySettings.autoCallEmergency
            )
            fallHistory = listOf(newEvent) + fallHistory
        }
    }

    // ============================================
    // FALL ALERT DIALOG
    // Shows when device detects a fall
    // ============================================
    if (fallAlert) {
        FallAlertDialog(
            safetySettings = safetySettings,
            onDismiss = {
                bleManager.clearAlert()
                if (fallHistory.isNotEmpty()) {
                    val updated = fallHistory.first().copy(action = "Dismissed by user")
                    fallHistory = listOf(updated) + fallHistory.drop(1)
                }
            }
        )
    }

    // ============================================
    // MAIN UI SCAFFOLD
    // ============================================
    Scaffold(
        containerColor = BgColor,
        bottomBar = {
            SafeShadeBottomBar(
                navController = navController,
                parentalControlsEnabled = safetySettings.parentalControlsEnabled
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Home Screen
            composable("home") {
                HomeScreen(
                    bleManager = bleManager,
                    weather = weather,
                    deviceSettings = deviceSettings,
                    onSyncWeather = onSyncWeather
                )
            }

            // Guardian/Companion Screen
            composable("guardian") {
                GuardianUserScreen(
                    bleManager = bleManager,
                    safetySettings = safetySettings,
                    isGuardianMode = isGuardianMode,
                    onModeChange = { isGuardianMode = it },
                    messageHistory = messageHistory,
                    onMessageSent = { msg ->
                        messageHistory = listOf(msg) + messageHistory
                    },
                    onReply = { msgId, reply ->
                        messageHistory = messageHistory.map {
                            if (it.id == msgId) it.copy(replied = true, replyText = reply)
                            else it
                        }
                    }
                )
            }

            // Safety Screen
            composable("safety") {
                SafetyScreen(
                    bleManager = bleManager,
                    safetySettings = safetySettings,
                    onSettingsChange = { newSettings ->
                        safetySettings = newSettings
                        if (bleManager.isConnected()) {
                            bleManager.sendSettings(newSettings)
                        }
                    },
                    fallHistory = fallHistory
                )
            }

            // Profile Screen
            composable("profile") {
                ProfileScreen(
                    medicalId = medicalId,
                    onMedicalIdChange = { newMedicalId ->
                        medicalId = newMedicalId
                        if (bleManager.isConnected()) {
                            bleManager.sendHealthData(newMedicalId)
                        }
                    },
                    deviceSettings = deviceSettings,
                    onDeviceSettingsChange = { deviceSettings = it }
                )
            }

            // Device Screen
            composable("device") {
                DeviceScreen(
                    bleManager = bleManager,
                    liveSensorData = liveSensorData,
                    onSensorUpdate = { liveSensorData = it }
                )
            }
        }
    }
}

/**
 * Fall alert dialog shown when device detects a potential fall.
 *
 * @param safetySettings Current safety settings
 * @param onDismiss Callback when user dismisses the alert
 */
@Composable
private fun FallAlertDialog(
    safetySettings: SafetySettings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Warning,
                contentDescription = "Warning",
                tint = AccentRed,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text("Fall Detected!", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Your SafeShade device has detected a possible fall.")
                if (safetySettings.autoCallEmergency) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Emergency contact will be notified in 30 seconds.",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Text("I'm OK - Dismiss")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { /* TODO: Implement emergency call */ }) {
                Text("Call Emergency")
            }
        }
    )
}
