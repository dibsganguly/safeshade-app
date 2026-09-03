/**
 * SafeShade - Universal Safety Companion
 *
 * ProfileScreen.kt
 *
 * User profile management screen with Medical ID editor, multi-device
 * pairing/management, appearance (dark mode), adaptive persona mode
 * selection, and app information.
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FIXES (this pass):
 *  - "My Devices" previously always rendered a single hardcoded
 *    DeviceSettings-derived row. It now renders the real `pairedDevices`
 *    list (persisted via SafeShadePreferences/DataStore), each with a real
 *    remove action and a "Connected" ConnectionStatusChip when its address
 *    matches bleManager.deviceAddress.
 *  - AddDeviceDialog is no longer a fake infinite spinner - see Dialogs.kt.
 *  - "Version 2.0.0" / "Build Demo Day Edition" were hardcoded strings;
 *    replaced with BuildConfig.VERSION_NAME / BuildConfig.BUILD_TYPE.
 *  - Added an Appearance section (System/Light/Dark) and an Adaptive Mode
 *    picker (7 PersonaMode values) - both previously had no UI entry point.
 */

package com.safeshade.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.BleManager
import com.safeshade.BuildConfig
import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceIconType
import com.safeshade.data.DeviceSettings
import com.safeshade.data.MedicalId
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*

/**
 * Profile screen - User profile and device management.
 *
 * Features:
 * - Medical ID display and editor
 * - Multi-device pairing/management (real BLE pairing + DataStore persistence)
 * - Appearance (dark mode) preference
 * - Adaptive Mode picker (the 7 PersonaMode profiles)
 * - App information (real version/build)
 */
@Composable
fun ProfileScreen(
    medicalId: MedicalId,
    onMedicalIdChange: (MedicalId) -> Unit,
    deviceSettings: DeviceSettings,
    onDeviceSettingsChange: (DeviceSettings) -> Unit,
    bleManager: BleManager,
    pairedDevices: List<PairedDevice>,
    onPairDevice: (PairedDevice) -> Unit,
    onRemoveDevice: (address: String) -> Unit,
    darkModePreference: DarkModePreference,
    onDarkModeChange: (DarkModePreference) -> Unit,
    activeMode: PersonaMode,
    onActiveModeChange: (PersonaMode) -> Unit,
    geofenceZoneCount: Int = 0,
    parentalControlsEnabled: Boolean = false
) {
    val colors = MaterialTheme.safeShadeColors

    // Dialog states
    var showMedicalIdEditor by remember { mutableStateOf(false) }
    var showDeviceSettings by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showAddDevice by remember { mutableStateOf(false) }

    val connectedAddress by bleManager.deviceAddress.collectAsState()

    // ============================================
    // DIALOGS
    // ============================================
    // Trigger counters for AckBadge - bumped whenever this screen sends a
    // change to the device, so the badge can show a real on-device
    // confirmation instead of just trusting local state.
    var medicalIdAckSeq by remember { mutableStateOf(0) }
    var modeAckSeq by remember { mutableStateOf(0) }

    if (showMedicalIdEditor) {
        MedicalIdEditorDialog(
            medicalId = medicalId,
            onDismiss = { showMedicalIdEditor = false },
            onSave = {
                onMedicalIdChange(it)
                medicalIdAckSeq++
                showMedicalIdEditor = false
            }
        )
    }

    if (showDeviceSettings) {
        DeviceSettingsDialog(
            deviceSettings = deviceSettings,
            onDismiss = { showDeviceSettings = false },
            onSave = {
                onDeviceSettingsChange(it)
                showDeviceSettings = false
            },
            onIconPickerRequest = {
                showDeviceSettings = false
                showIconPicker = true
            }
        )
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentIcon = deviceSettings.iconType,
            onDismiss = { showIconPicker = false },
            onSelect = { icon ->
                onDeviceSettingsChange(deviceSettings.copy(iconType = icon))
                showIconPicker = false
            }
        )
    }

    if (showAddDevice) {
        AddDeviceDialog(
            bleManager = bleManager,
            onPaired = { device -> onPairDevice(device) },
            onDismiss = { showAddDevice = false }
        )
    }

    // ============================================
    // MAIN CONTENT
    // ============================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Profile", style = MaterialTheme.typography.displayMedium, color = colors.onSurface)
        Spacer(modifier = Modifier.height(Spacing.xl))

        // Adaptive Mode picker — headline feature, so it goes first.
        AdaptiveModeCard(
            bleManager = bleManager,
            activeMode = activeMode,
            ackTrigger = modeAckSeq.takeIf { it > 0 },
            onSelect = { mode ->
                onActiveModeChange(mode)
                modeAckSeq++
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Mode Controls - content changes with activeMode, so switching
        // modes genuinely changes what the app itself offers, not just the
        // device's algorithm/UI.
        ModeControlsCard(
            bleManager = bleManager,
            activeMode = activeMode,
            geofenceZoneCount = geofenceZoneCount,
            parentalControlsEnabled = parentalControlsEnabled
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Medical ID Card
        MedicalIdCard(
            bleManager = bleManager,
            medicalId = medicalId,
            ackTrigger = medicalIdAckSeq.takeIf { it > 0 },
            onEditClick = { showMedicalIdEditor = true }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // My Devices Card
        MyDevicesCard(
            pairedDevices = pairedDevices,
            connectedAddress = connectedAddress,
            onDeviceClick = { showDeviceSettings = true },
            onRemoveDevice = onRemoveDevice,
            onAddDeviceClick = { showAddDevice = true }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Appearance Card
        AppearanceCard(
            darkModePreference = darkModePreference,
            onDarkModeChange = onDarkModeChange
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // App Info Card
        AppInfoCard()
    }
}

/**
 * Adaptive Mode picker - the primary entry point for the "7 Adaptive Modes"
 * headline feature. A horizontally scrollable row of selectable chips, each
 * a PersonaMode with icon/label, plus a description of the selected mode.
 */
@Composable
private fun AdaptiveModeCard(
    bleManager: BleManager,
    activeMode: PersonaMode,
    ackTrigger: Any?,
    onSelect: (PersonaMode) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    "Adaptive Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                // Value-qualified tag ("MODE:KIDS" not just "MODE") - same
                // fix as the LED picker's AckBadge, same reason: rapidly
                // tapping two different modes could otherwise let a stale
                // ack for the first satisfy the second's badge.
                com.safeshade.ui.components.AckBadge(
                    bleManager = bleManager,
                    tag = "MODE:${activeMode.name}",
                    trigger = ackTrigger
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                PersonaMode.entries.forEach { mode ->
                    PersonaModeChip(
                        mode = mode,
                        selected = mode == activeMode,
                        onClick = { onSelect(mode) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))

            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(Motion.normal)),
                exit = fadeOut(tween(Motion.normal))
            ) {
                Text(
                    activeMode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceMuted
                )
            }
        }
    }
}

@Composable
private fun PersonaModeChip(
    mode: PersonaMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accentPrimary else colors.accentPrimary.copy(alpha = 0.08f),
        animationSpec = tween(Motion.fast),
        label = "personaChipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.surface else colors.accentPrimary,
        animationSpec = tween(Motion.fast),
        label = "personaChipFg"
    )

    Column(
        modifier = Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(Radius.md))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(mode.icon, contentDescription = mode.label, tint = fg, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            mode.label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1
        )
    }
}

/**
 * Mode Controls - content switches on [activeMode], so the app itself
 * offers different real controls per mode, not just different device
 * behavior. Elderly/Helmet get real scheduling controls (EXT "MED:"/
 * "CHECKIN:", both actually handled on-device - see the firmware's
 * medication-reminder and check-in trigger logic); the rest get an
 * accurate at-a-glance summary of what's actually active for that mode,
 * rather than fabricated telemetry mirroring the device doesn't expose
 * over BLE.
 */
@Composable
private fun ModeControlsCard(
    bleManager: BleManager,
    activeMode: PersonaMode,
    geofenceZoneCount: Int,
    parentalControlsEnabled: Boolean
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Mode Controls", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            when (activeMode) {
                PersonaMode.ELDERLY -> MedicationReminderControl(bleManager)
                PersonaMode.HELMET -> CheckInIntervalControl(bleManager)
                PersonaMode.KIDS -> KidsModeInfo(geofenceZoneCount, parentalControlsEnabled)
                PersonaMode.BIKE -> ModeInfoText(
                    "Crash detection combines impact strength with rotational " +
                        "jerk (gyro-aware) - fewer false alarms from potholes/bumps " +
                        "than impact alone. Ride distance/time show on the device's " +
                        "Ride Stats screen."
                )
                PersonaMode.PET -> ModeInfoText(
                    "Fall detection is off in Pet mode. Activity tracking and a " +
                        "\"virtual leash\" are active instead - an unexpected " +
                        "disconnect triggers a lost-pet alert (SMS + LED blink) " +
                        "automatically."
                )
                PersonaMode.WRIST -> ModeInfoText(
                    "Live heart-rate/SpO2 (simulated - no HR sensor on this board " +
                        "yet) and real sleep-time tracking show on the device's " +
                        "watch-face Home screen and Vitals screen."
                )
                PersonaMode.BACKPACK -> ModeInfoText(
                    "Balanced default mode - all standard features active, no " +
                        "mode-specific overrides. Every other mode is a deliberate " +
                        "deviation from this baseline."
                )
            }
        }
    }
}

@Composable
private fun ModeInfoText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.safeShadeColors.onSurfaceMuted
    )
}

@Composable
private fun KidsModeInfo(geofenceZoneCount: Int, parentalControlsEnabled: Boolean) {
    val colors = MaterialTheme.safeShadeColors
    Text(
        if (geofenceZoneCount > 0)
            "$geofenceZoneCount safe zone(s) configured - status shows on the device's Safe Zone screen."
        else
            "No safe zones yet - add one from the Guardian tab to see live status on the device.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    Text(
        if (parentalControlsEnabled) "Parental lock: ON" else "Parental lock: OFF",
        style = MaterialTheme.typography.bodySmall,
        color = if (parentalControlsEnabled) colors.accentSuccess else colors.onSurfaceMuted,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun MedicationReminderControl(bleManager: BleManager) {
    val colors = MaterialTheme.safeShadeColors
    // rememberSaveable (not plain remember) - the device doesn't expose
    // what's currently scheduled (no readback), so this at least survives
    // navigating away and back instead of silently resetting to 8:00 every
    // time (found in review).
    var hour by rememberSaveable { mutableStateOf(8) }
    var minute by rememberSaveable { mutableStateOf(0) }
    var sendSeq by remember { mutableStateOf(0) }

    Text(
        "Medication Reminder",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberStepper(value = hour, range = 0..23, onChange = { hour = it })
        Text(":", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        NumberStepper(value = minute, range = 0..59, step = 5, onChange = { minute = it })
        Spacer(modifier = Modifier.width(Spacing.sm))
        BouncyButton(
            onClick = {
                bleManager.sendExtCommand("MED", String.format("%02d:%02d", hour, minute))
                sendSeq++
            },
            color = colors.accentPrimary
        ) {
            Text("Set", color = Color.White, fontSize = 13.sp)
        }
    }
    Spacer(modifier = Modifier.height(Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        AckBadge(bleManager = bleManager, tag = "MED", trigger = sendSeq.takeIf { it > 0 })
        Spacer(modifier = Modifier.width(Spacing.sm))
        TextButton(onClick = {
            bleManager.sendExtCommand("MED", "")
            sendSeq++
        }) {
            Text("Clear", fontSize = 12.sp, color = colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun CheckInIntervalControl(bleManager: BleManager) {
    val colors = MaterialTheme.safeShadeColors
    // rememberSaveable for the same reason as MedicationReminderControl above.
    var selectedMinutes by rememberSaveable { mutableStateOf(60) }
    var sendSeq by remember { mutableStateOf(0) }

    Text(
        "Scheduled Check-In",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurface,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        listOf(30, 60, 120).forEach { mins ->
            val selected = selectedMinutes == mins
            OutlinedButton(
                onClick = { selectedMinutes = mins },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (selected) colors.accentPrimary.copy(alpha = 0.15f) else Color.Transparent,
                    contentColor = if (selected) colors.accentPrimary else colors.onSurfaceMuted
                ),
                contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.xs)
            ) {
                Text("${mins}m", fontSize = 12.sp)
            }
        }
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BouncyButton(
            onClick = {
                bleManager.sendExtCommand("CHECKIN", (selectedMinutes * 60).toString())
                sendSeq++
            },
            color = colors.accentPrimary
        ) {
            Text("Set Check-In", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        AckBadge(bleManager = bleManager, tag = "CHECKIN", trigger = sendSeq.takeIf { it > 0 })
    }
}

/** Minimal +/- stepper - avoids depending on Material3's TimePicker API
 * (version-sensitive) for what's just two small bounded integers. */
@Composable
private fun NumberStepper(value: Int, range: IntRange, step: Int = 1, onChange: (Int) -> Unit) {
    val colors = MaterialTheme.safeShadeColors
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(
            onClick = { onChange((value - step).coerceIn(range.first, range.last)) },
            contentPadding = PaddingValues(horizontal = Spacing.xs)
        ) {
            Text("-", color = colors.accentPrimary, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            String.format("%02d", value),
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = colors.onSurface,
            fontWeight = FontWeight.Bold
        )
        TextButton(
            onClick = { onChange((value + step).coerceIn(range.first, range.last)) },
            contentPadding = PaddingValues(horizontal = Spacing.xs)
        ) {
            Text("+", color = colors.accentPrimary, style = MaterialTheme.typography.titleMedium)
        }
    }
}

/**
 * Medical ID display card with edit button.
 */
@Composable
private fun MedicalIdCard(
    bleManager: BleManager,
    medicalId: MedicalId,
    ackTrigger: Any?,
    onEditClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            // Header with edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = colors.accentDanger,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Medical ID", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    com.safeshade.ui.components.AckBadge(
                        bleManager = bleManager,
                        tag = "HEALTH",
                        trigger = ackTrigger
                    )
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit Medical ID",
                        tint = colors.accentPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Medical ID fields
            ProfileField("Blood Type", medicalId.bloodType)
            ProfileField("Emergency Contact", medicalId.emergencyContact)
            ProfileField("Contact Name", medicalId.contactName)
            ProfileField("Allergies", medicalId.allergies)
            ProfileField("Medical Notes", medicalId.medicalNotes)
            ProfileField("Age", medicalId.age.toString())
        }
    }
}

/**
 * My Devices card showing every paired device (real, DataStore-backed list)
 * and an add-device button. The device whose address matches the live BLE
 * session is marked "Connected".
 */
@Composable
private fun MyDevicesCard(
    pairedDevices: List<PairedDevice>,
    connectedAddress: String,
    onDeviceClick: () -> Unit,
    onRemoveDevice: (address: String) -> Unit,
    onAddDeviceClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text("My Devices", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Spacer(modifier = Modifier.height(Spacing.md))

            if (pairedDevices.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.BluetoothDisabled,
                    message = "No devices paired yet"
                )
            } else {
                pairedDevices.forEach { device ->
                    PairedDeviceRow(
                        device = device,
                        isConnected = device.address == connectedAddress,
                        onClick = onDeviceClick,
                        onRemove = { onRemoveDevice(device.address) }
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Add device button
            OutlinedButton(
                onClick = onAddDeviceClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(Radius.sm),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentPrimary)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Pair New Device")
            }
        }
    }
}

@Composable
private fun PairedDeviceRow(
    device: PairedDevice,
    isConnected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    val icon = DeviceIconType.entries.getOrElse(device.iconOrdinal) { DeviceIconType.UMBRELLA }.icon

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .clickable { onClick() }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Device icon
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(colors.accentPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.accentPrimary)
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Device info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
            Text(
                device.address,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
        }

        ConnectionStatusChip(connected = isConnected)

        Spacer(modifier = Modifier.width(Spacing.sm))

        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "Remove device",
                tint = colors.accentDanger
            )
        }
    }
}

/**
 * Appearance card - dark mode preference (System/Light/Dark).
 */
@Composable
private fun AppearanceCard(
    darkModePreference: DarkModePreference,
    onDarkModeChange: (DarkModePreference) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text("Appearance", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Spacer(modifier = Modifier.height(Spacing.sm))

            val options = listOf(
                Triple(DarkModePreference.SYSTEM, "System", Icons.Rounded.BrightnessAuto),
                Triple(DarkModePreference.LIGHT, "Light", Icons.Rounded.LightMode),
                Triple(DarkModePreference.DARK, "Dark", Icons.Rounded.DarkMode)
            )

            options.forEach { (pref, label, icon) ->
                SettingsRow(
                    icon = icon,
                    title = label,
                    iconTint = colors.accentPrimary,
                    onClick = { onDarkModeChange(pref) },
                    trailingContent = {
                        RadioButton(
                            selected = darkModePreference == pref,
                            onClick = { onDarkModeChange(pref) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.accentPrimary)
                        )
                    }
                )
            }
        }
    }
}

/**
 * App information card - real version/build info from BuildConfig.
 */
@Composable
private fun AppInfoCard() {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text("App Information", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Spacer(modifier = Modifier.height(Spacing.md))
            ProfileField("Version", BuildConfig.VERSION_NAME)
            ProfileField("Build", BuildConfig.BUILD_TYPE)
        }
    }
}

/**
 * Single profile field with label and value.
 */
@Composable
fun ProfileField(label: String, value: String) {
    val colors = MaterialTheme.safeShadeColors
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = colors.onSurface
        )
    }
}
