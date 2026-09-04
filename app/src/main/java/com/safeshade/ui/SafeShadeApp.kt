/**
 * SafeShade - Universal Safety Companion
 *
 * SafeShadeApp.kt
 *
 * Main application composable that sets up navigation, state management,
 * and handles global events like fall alerts and device replies.
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FEATURES (this pass):
 *  - Real auto-call-emergency: FallAlertDialog now runs an actual visible
 *    countdown and places a real call via EmergencyActions.placeEmergencyCall
 *    when it expires (or immediately on "Call Emergency") - previously the
 *    dialog's "Call Emergency" button was a no-op TODO with no countdown at
 *    all despite implying one existed via SafetySettings.autoCallEmergency.
 *  - SMS fallback alert fires alongside the call attempt (item #9).
 *  - liveSensorData now comes straight from bleManager.liveSensorData
 *    (real MPU6050/LDR/battery telemetry) instead of a local mutable state
 *    that DeviceScreen used to fill with Math.random().
 *  - Onboarding gating, dark-mode preference, geofence zone CRUD + sync,
 *    and adaptive persona-mode wiring threaded down from MainActivity.
 *
 * FIXES (earlier pass):
 *  - Added permissionsGranted/onRequestPermissions parameters so the Home
 *    screen's connect toggle can no longer call bleManager.startScanning()
 *    before Bluetooth/location permissions are actually granted.
 *  - onReply (Companion mode quick-reply chips) sends the reply to the
 *    device via bleManager.sendDeviceReply() instead of only updating local
 *    UI state.
 */

package com.safeshade.ui

import android.Manifest
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalPhone
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.safeshade.BleManager
import com.safeshade.GeofenceEventBus
import com.safeshade.GeofenceManager
import com.safeshade.SmsMessageEventBus
import com.safeshade.data.*
import com.safeshade.placeEmergencyCall
import com.safeshade.sendEmergencySms
import com.safeshade.sendSmsText
import com.safeshade.ui.navigation.SafeShadeBottomBar
import com.safeshade.ui.screens.*
import com.safeshade.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext

/** Countdown before an unattended fall alert triggers a real emergency call, in seconds. */
private const val FALL_AUTO_CALL_SECONDS = 30

@Composable
fun SafeShadeApp(
    bleManager: BleManager,
    weather: WeatherUiState,
    location: LocationState,
    permissionsGranted: Boolean = true,
    onRequestPermissions: () -> Unit = {},
    onSyncWeather: () -> Unit,
    preferences: SafeShadePreferences,
    darkModePreference: DarkModePreference = DarkModePreference.SYSTEM,
    onDarkModeChange: (DarkModePreference) -> Unit = {},
    onboardingSeen: Boolean? = true,
    onOnboardingComplete: () -> Unit = {},
    geofenceManager: GeofenceManager,
    onRequestSensitivePermissions: (Array<String>) -> Unit = {},
    syncInProgress: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val fallAlert by bleManager.fallAlert.collectAsState()
    val liveSensorData by bleManager.liveSensorData.collectAsState()

    // ============================================
    // APP STATE
    // ============================================
    var deviceSettings by remember { mutableStateOf(DeviceSettings()) }
    var medicalId by remember { mutableStateOf(MedicalId()) }
    var safetySettings by remember { mutableStateOf(SafetySettings()) }
    var fallHistory by remember { mutableStateOf(listOf<FallAlertEvent>()) }
    var messageHistory by remember { mutableStateOf(listOf<QuickMessage>()) }
    var isGuardianMode by remember { mutableStateOf(true) }
    var geofenceZones by remember { mutableStateOf(listOf<GeofenceZone>()) }
    var activeMode by remember { mutableStateOf(PersonaMode.BACKPACK) }
    var devicePhoneNumber by remember { mutableStateOf("") }
    var smsAllowlist by remember { mutableStateOf(listOf<String>()) }

    val pairedDevices by preferences.pairedDevices.collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        preferences.activeModeName.collect { name ->
            activeMode = runCatching { PersonaMode.valueOf(name) }.getOrDefault(PersonaMode.BACKPACK)
        }
    }

    LaunchedEffect(Unit) {
        preferences.devicePhoneNumber.collect { devicePhoneNumber = it }
    }

    LaunchedEffect(Unit) {
        preferences.smsAllowlist.collect { smsAllowlist = it }
    }

    // Appends a real device->Guardian message, deduping against a
    // near-identical one that just arrived seconds ago via the other
    // channel - the firmware relays every reply over BOTH BLE notify and
    // gateway SMS (see triggerGatewayReply() in SafeShadev21.ino), so a
    // BLE-connected session can otherwise see the same reply twice: once
    // from deviceReply below, once from the SMS observer.
    fun appendIncomingDeviceMessage(text: String) {
        val now = System.currentTimeMillis()
        val isDuplicate = messageHistory.firstOrNull()?.let {
            !it.fromGuardian && it.text == text && (now - it.timestamp) < 8000
        } ?: false
        if (isDuplicate) return
        messageHistory = listOf(QuickMessage(text = text, fromGuardian = false, timestamp = now)) + messageHistory
    }

    // Guardian->device message send, made device-independent: BLE while
    // connected (fast, free), automatic SMS fallback to the wearable's SIM
    // otherwise - the EC200U gateway relays it back out over cellular even
    // when the Guardian's phone has no BLE link to the device at all.
    val onSendGuardianMessage: (String) -> Unit = { text ->
        if (bleManager.isConnected()) {
            bleManager.sendGuardianMessage(text)
        } else if (devicePhoneNumber.isNotBlank()) {
            sendSmsText(context, devicePhoneNumber, text, logTag = "GUARDIAN_MSG_SMS")
        }
    }

    // ============================================
    // DEVICE REPLY OBSERVER
    // ============================================
    val deviceReply by bleManager.deviceReply.collectAsState()

    LaunchedEffect(deviceReply) {
        deviceReply?.let { reply ->
            appendIncomingDeviceMessage(reply)
            bleManager.clearReply()
        }
    }

    // ============================================
    // SMS MESSAGE OBSERVER - real device-independent receive side. Matches
    // incoming SMS senders against the configured device phone number (last
    // 10 digits, so +91/0/no-prefix variants all match) since SmsReceiver
    // has no reference to this state (see SmsMessageEventBus's doc comment).
    // ============================================
    LaunchedEffect(Unit) {
        SmsMessageEventBus.events.collect { (sender, body) ->
            val deviceDigits = devicePhoneNumber.filter { it.isDigit() }.takeLast(10)
            val senderDigits = sender.filter { it.isDigit() }.takeLast(10)
            if (deviceDigits.length == 10 && deviceDigits == senderDigits) {
                appendIncomingDeviceMessage(body)
            }
        }
    }

    // ============================================
    // GEOFENCE EVENT OBSERVER - real safe-zone enter/exit, forwarded to the
    // device via EXT_CHAR (see GeofenceEventBus's doc comment for why this
    // needs a bus rather than a direct callback).
    // ============================================
    LaunchedEffect(Unit) {
        GeofenceEventBus.events.collect { (zoneId, isInside) ->
            val zoneName = geofenceZones.firstOrNull { it.id == zoneId }?.name ?: "Zone"
            if (bleManager.isConnected()) {
                bleManager.sendExtCommand("GEOFENCE", "$zoneName:${if (isInside) "IN" else "OUT"}")
            }
        }
    }

    // ============================================
    // CONNECTION STATE OBSERVER
    // ============================================
    val connectionState by bleManager.connectionState.collectAsState()

    LaunchedEffect(connectionState) {
        if (connectionState == "Connected") {
            bleManager.sendHealthData(medicalId)
            bleManager.sendSettings(safetySettings)
            // Allowlist lives phone-side (DataStore) but must be re-pushed to
            // the device on every reconnect - the firmware only holds it in RAM.
            if (smsAllowlist.isNotEmpty()) {
                bleManager.sendSmsAllowlist(smsAllowlist)
            }
        }
    }

    // ============================================
    // FALL ALERT LOGGING
    // ============================================
    LaunchedEffect(fallAlert) {
        if (fallAlert) {
            val locationSnapshot = if (location.isValid) {
                location.locationName.ifBlank { "%.4f, %.4f".format(location.lat, location.lon) }
            } else null
            val sensorSnapshot = if (liveSensorData.isRealData) {
                "Battery ${liveSensorData.batteryLevel}% • accel %.2fg".format(
                    kotlin.math.sqrt(
                        liveSensorData.accelX * liveSensorData.accelX +
                            liveSensorData.accelY * liveSensorData.accelY +
                            liveSensorData.accelZ * liveSensorData.accelZ
                    )
                )
            } else null
            val newEvent = FallAlertEvent(
                eventType = "Fall detected",
                action = "Awaiting dismissal",
                wasEmergencyContacted = safetySettings.autoCallEmergency,
                location = locationSnapshot,
                note = sensorSnapshot
            )
            fallHistory = listOf(newEvent) + fallHistory
        }
    }

    // ============================================
    // FALL ALERT DIALOG - real countdown + real call/SMS
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
            },
            onCallNow = {
                val primary = safetySettings.emergencyContacts.firstOrNull { it.isPrimary }
                    ?: safetySettings.emergencyContacts.firstOrNull()
                if (primary != null) {
                    placeEmergencyCall(context, primary)
                    if (safetySettings.smsFallbackEnabled) {
                        sendEmergencySms(context, primary, "SafeShade: possible fall detected. This is an automated alert.")
                    }
                }
                bleManager.clearAlert()
                if (fallHistory.isNotEmpty()) {
                    val updated = fallHistory.first().copy(action = "Emergency contacted", wasEmergencyContacted = true)
                    fallHistory = listOf(updated) + fallHistory.drop(1)
                }
            }
        )
    }

    // ============================================
    // MAIN UI SCAFFOLD
    // ============================================
    Scaffold(
        containerColor = MaterialTheme.safeShadeColors.background,
        bottomBar = {
            // Gated on onboardingSeen - was rendering (and tappable)
            // underneath the onboarding flow, letting a user navigate away
            // from it before finishing (found in review).
            if (onboardingSeen != false) {
                SafeShadeBottomBar(
                    navController = navController,
                    parentalControlsEnabled = safetySettings.parentalControlsEnabled
                )
            }
        }
    ) { innerPadding ->
        if (onboardingSeen == false) {
            OnboardingScreen(
                modifier = Modifier.padding(innerPadding),
                onFinish = onOnboardingComplete
            )
            return@Scaffold
        }

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    bleManager = bleManager,
                    weather = weather,
                    deviceSettings = deviceSettings,
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = onRequestPermissions,
                    onSyncWeather = onSyncWeather,
                    activeMode = activeMode,
                    liveSensorData = liveSensorData,
                    isSyncing = syncInProgress
                )
            }

            composable("guardian") {
                GuardianUserScreen(
                    bleManager = bleManager,
                    safetySettings = safetySettings,
                    isGuardianMode = isGuardianMode,
                    onModeChange = { isGuardianMode = it },
                    messageHistory = messageHistory,
                    onSendMessage = onSendGuardianMessage,
                    onMessageSent = { msg -> messageHistory = listOf(msg) + messageHistory },
                    onReply = { msgId, reply ->
                        bleManager.sendDeviceReply(reply)
                        messageHistory = messageHistory.map {
                            if (it.id == msgId) it.copy(replied = true, replyText = reply) else it
                        }
                    },
                    geofenceZones = geofenceZones,
                    onGeofenceZonesChange = { zones ->
                        geofenceZones = zones
                        geofenceManager.syncZones(zones)
                    },
                    location = location,
                    onRequestSensitivePermissions = onRequestSensitivePermissions,
                    devicePhoneNumber = devicePhoneNumber,
                    onDevicePhoneNumberChange = { number ->
                        devicePhoneNumber = number
                        coroutineScope.launch { preferences.setDevicePhoneNumber(number) }
                        if (number.isNotBlank()) {
                            onRequestSensitivePermissions(
                                arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS)
                            )
                        }
                    },
                    smsAllowlist = smsAllowlist,
                    onAllowlistChange = { numbers ->
                        smsAllowlist = numbers
                        coroutineScope.launch { preferences.setSmsAllowlist(numbers) }
                        if (bleManager.isConnected()) {
                            bleManager.sendSmsAllowlist(numbers)
                        }
                    }
                )
            }

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
                    fallHistory = fallHistory,
                    onRequestSensitivePermissions = onRequestSensitivePermissions
                )
            }

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
                    onDeviceSettingsChange = { newDeviceSettings ->
                        val nameChanged = newDeviceSettings.name != deviceSettings.name
                        deviceSettings = newDeviceSettings
                        // Device name previously never reached the firmware
                        // at all - now synced via EXT_CHAR "DEVNAME:" and
                        // shown on the Home screen ticker.
                        if (nameChanged && bleManager.isConnected()) {
                            bleManager.sendExtCommand("DEVNAME", newDeviceSettings.name)
                        }
                    },
                    bleManager = bleManager,
                    pairedDevices = pairedDevices,
                    onPairDevice = { device ->
                        coroutineScope.launch { preferences.upsertPairedDevice(device) }
                    },
                    onRemoveDevice = { address ->
                        coroutineScope.launch { preferences.removePairedDevice(address) }
                    },
                    darkModePreference = darkModePreference,
                    onDarkModeChange = onDarkModeChange,
                    activeMode = activeMode,
                    onActiveModeChange = { mode ->
                        activeMode = mode
                        coroutineScope.launch { preferences.setActiveMode(mode) }
                        safetySettings = safetySettings.copy(fallSensitivity = mode.defaultFallSensitivity)
                        if (bleManager.isConnected()) {
                            bleManager.sendSettings(safetySettings)
                            // Real mode sync (EXT_CHAR "MODE:<name>") - the
                            // device actually changes its algorithm/UI/LED
                            // behavior per mode now (applyMode() in the
                            // firmware), and acknowledges the write.
                            bleManager.sendExtCommand("MODE", mode.name)
                        }
                    },
                    geofenceZoneCount = geofenceZones.size,
                    parentalControlsEnabled = safetySettings.parentalControlsEnabled
                )
            }

            composable("device") {
                DeviceScreen(
                    bleManager = bleManager,
                    liveSensorData = liveSensorData
                )
            }
        }
    }
}

/**
 * Fall alert dialog shown when device detects a potential fall. Runs a
 * real, visible countdown and places a real call (+ SMS fallback) via
 * [onCallNow] when it expires - autoCallEmergency being on is no longer
 * a false promise with zero call-placing code behind it.
 */
@Composable
private fun FallAlertDialog(
    safetySettings: SafetySettings,
    onDismiss: () -> Unit,
    onCallNow: () -> Unit
) {
    var secondsLeft by remember { mutableStateOf(FALL_AUTO_CALL_SECONDS) }

    LaunchedEffect(Unit) {
        if (!safetySettings.autoCallEmergency) return@LaunchedEffect
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        onCallNow()
    }

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
        title = { Text("Fall Detected!", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Your SafeShade device has detected a possible fall.")
                if (safetySettings.autoCallEmergency) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Calling emergency contact in $secondsLeft s unless dismissed.",
                        color = AccentRed,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { secondsLeft / FALL_AUTO_CALL_SECONDS.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = AccentRed
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Auto-call is off - no call will be placed automatically.",
                        color = MaterialTheme.safeShadeColors.onSurfaceMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
            ) {
                Text("I'm OK - Dismiss")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCallNow) {
                Icon(Icons.Rounded.LocalPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Call Emergency Now")
            }
        }
    )
}
