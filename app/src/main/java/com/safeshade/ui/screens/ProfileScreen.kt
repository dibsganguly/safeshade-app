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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.BleManager
import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceIconType
import com.safeshade.data.DeviceSettings
import com.safeshade.data.LedPattern
import com.safeshade.data.MedicalId
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*
import kotlinx.coroutines.delay

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
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"

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
            parentalControlsEnabled = parentalControlsEnabled,
            medicalId = medicalId,
            onMedicalIdChange = onMedicalIdChange
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // LED Pattern Card - relocated from DeviceScreen so remote LED
        // control lives alongside the rest of the device/appearance
        // customization on Profile, rather than the read-only telemetry
        // screen.
        LedControlCard(
            bleManager = bleManager,
            enabled = isConnected,
            onPatternSelected = { bleManager.sendLedPattern(it) }
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

        Spacer(modifier = Modifier.height(Spacing.xl))
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
    // Each mode carries its own accentColor (PersonaMode.accentColor) so the
    // 7 chips read as visually distinct at a glance, not just differently
    // labeled instances of the same flat brand color.
    val bg by animateColorAsState(
        targetValue = if (selected) mode.accentColor else mode.accentColor.copy(alpha = 0.10f),
        animationSpec = tween(Motion.fast),
        label = "personaChipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.surface else mode.accentColor,
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
    parentalControlsEnabled: Boolean,
    medicalId: MedicalId,
    onMedicalIdChange: (MedicalId) -> Unit
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
                PersonaMode.ELDERLY -> ElderlyModeControls(bleManager, medicalId)
                PersonaMode.HELMET -> HelmetModeControls(bleManager)
                PersonaMode.KIDS -> KidsModeInfo(bleManager, geofenceZoneCount, parentalControlsEnabled)
                PersonaMode.BIKE -> BikeModeInfo(bleManager)
                PersonaMode.PET -> PetModeInfo(medicalId, onMedicalIdChange)
                PersonaMode.WRIST -> WristModeInfo()
                PersonaMode.BACKPACK -> BackpackModeInfo()
            }
        }
    }
}

/** Small label used above each mode's structured content, matching the
 * weight/size the old ad hoc "Medication Reminder"/"Scheduled Check-In"
 * labels already used. */
@Composable
private fun ModeSectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.safeShadeColors.onSurface,
        fontWeight = FontWeight.Bold
    )
}

/** Icon-led bullet row for structured "what's different in this mode" copy -
 * replaces the old wall-of-text ModeInfoText paragraphs. */
@Composable
private fun ModeBulletRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    val colors = MaterialTheme.safeShadeColors
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Icon + big value + label stat tile, for at-a-glance numbers that are
 * already available client-side (geofence zone count, lock state) rather
 * than fabricated device telemetry. */
@Composable
private fun ModeStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radius.sm))
            .background(tint.copy(alpha = 0.08f))
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colors.onSurface)
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceMuted)
        }
    }
}

/** Tinted note box for a short structured explanation (concussion-confirm
 * flow, simulated-vitals honesty disclaimer, etc). */
@Composable
private fun InfoNoteBox(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(tint.copy(alpha = 0.08f))
            .padding(Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ElderlyModeControls(bleManager: BleManager, medicalId: MedicalId) {
    val colors = MaterialTheme.safeShadeColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
        MedicationReminderControl(bleManager)
        val hasContact = medicalId.contactName.isNotBlank() || medicalId.emergencyContact.isNotBlank()
        if (hasContact) {
            SettingsRow(
                icon = Icons.Rounded.ContactEmergency,
                title = medicalId.contactName.ifBlank { "Emergency Contact" },
                subtitle = medicalId.emergencyContact.ifBlank { null },
                iconTint = colors.accentDanger,
                modifier = Modifier
                    .clip(RoundedCornerShape(Radius.sm))
                    .background(colors.accentDanger.copy(alpha = 0.06f))
                    .padding(horizontal = Spacing.sm)
            )
        }
    }
}

@Composable
private fun HelmetModeControls(bleManager: BleManager) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        CheckInIntervalControl(bleManager)
        InfoNoteBox(
            icon = Icons.Rounded.HealthAndSafety,
            text = "Two-step concussion confirm: a detected impact first buzzes the " +
                "device and waits for an on-device confirm. Only an unconfirmed " +
                "impact escalates to a Guardian alert - fewer false alarms from a " +
                "helmet just being bumped or set down.",
            tint = MaterialTheme.safeShadeColors.accentDanger
        )
        NavigationControl(bleManager)
    }
}

@Composable
private fun KidsModeInfo(bleManager: BleManager, geofenceZoneCount: Int, parentalControlsEnabled: Boolean) {
    val colors = MaterialTheme.safeShadeColors
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ModeStatRow(
            icon = Icons.Rounded.Place,
            value = geofenceZoneCount.toString(),
            label = if (geofenceZoneCount == 1) "Safe Zone" else "Safe Zones",
            tint = colors.accentPrimary,
            modifier = Modifier.weight(1f)
        )
        ModeStatRow(
            icon = if (parentalControlsEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
            value = if (parentalControlsEnabled) "ON" else "OFF",
            label = "Parental Lock",
            tint = if (parentalControlsEnabled) colors.accentSuccess else colors.onSurfaceMuted,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        if (geofenceZoneCount > 0)
            "Live zone status shows on the device's Safe Zone screen."
        else
            "Add a safe zone from the Guardian tab to see live status on the device.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted
    )
    Spacer(modifier = Modifier.height(Spacing.lg))
    QuietHoursControl(bleManager)
}

@Composable
private fun QuietHoursControl(bleManager: BleManager) {
    val colors = MaterialTheme.safeShadeColors
    // rememberSaveable for the same reason as MedicationReminderControl -
    // the device doesn't expose the currently-set window (no readback).
    var startHour by rememberSaveable { mutableStateOf(22) }
    var endHour by rememberSaveable { mutableStateOf(7) }
    var sendSeq by remember { mutableStateOf(0) }

    ModeSectionLabel("Quiet Hours")
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Mutes the non-critical message chime during this window - fall/SOS alerts are never silenced.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        NumberStepper(value = startHour, range = 0..23, onChange = { startHour = it })
        Text(":00 to", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceMuted)
        NumberStepper(value = endHour, range = 0..23, onChange = { endHour = it })
        Text(":00", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceMuted)
    }
    Spacer(modifier = Modifier.height(Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BouncyButton(
            onClick = {
                bleManager.sendExtCommand("QUIET", "$startHour:$endHour")
                sendSeq++
            },
            color = colors.accentPrimary
        ) {
            Text("Set", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        AckBadge(bleManager = bleManager, tag = "QUIET", trigger = sendSeq.takeIf { it > 0 })
        Spacer(modifier = Modifier.width(Spacing.sm))
        TextButton(onClick = {
            bleManager.sendExtCommand("QUIET", "")
            sendSeq++
        }) {
            Text("Disable", fontSize = 12.sp, color = colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun BikeModeInfo(bleManager: BleManager) {
    val colors = MaterialTheme.safeShadeColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ModeSectionLabel("What's Different")
        ModeBulletRow(
            Icons.Rounded.Vibration,
            "Crash-signature detection combines impact strength with rotational jerk (gyro-aware), not accelerometer force alone.",
            colors.accentPrimary
        )
        ModeBulletRow(
            Icons.AutoMirrored.Rounded.TrendingUp,
            "Sensitivity is tuned higher for cycling speeds and impact profiles.",
            colors.accentPrimary
        )
        ModeBulletRow(
            Icons.Rounded.CheckCircle,
            "Fewer false alarms from potholes and bumps than impact-only detection. Ride distance/time show on the device's Ride Stats screen.",
            colors.accentSuccess
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        NavigationControl(bleManager)
    }
}

/**
 * Real (not fabricated) navigation control - EXT "NAV:<lat>:<lon>:<label>"
 * makes the firmware compute a live distance + compass bearing from the
 * current GPS fix to this destination and show it on the device's Location
 * screen. This is a distance/bearing readout, not turn-by-turn routing.
 */
@Composable
private fun NavigationControl(bleManager: BleManager) {
    val colors = MaterialTheme.safeShadeColors
    var label by rememberSaveable { mutableStateOf("") }
    var latText by rememberSaveable { mutableStateOf("") }
    var lonText by rememberSaveable { mutableStateOf("") }
    var sendSeq by remember { mutableStateOf(0) }

    ModeSectionLabel("Navigation")
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        "Shows a live distance and compass bearing to this destination on the device's Location screen - not full turn-by-turn routing.",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceMuted
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    OutlinedTextField(
        value = label,
        onValueChange = { if (it.length <= 20) label = it },
        label = { Text("Destination label") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedTextField(
            value = latText,
            onValueChange = { latText = it },
            label = { Text("Latitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = lonText,
            onValueChange = { lonText = it },
            label = { Text("Longitude") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(modifier = Modifier.height(Spacing.xs))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BouncyButton(
            onClick = {
                val lat = latText.toDoubleOrNull()
                val lon = lonText.toDoubleOrNull()
                if (lat != null && lon != null) {
                    bleManager.sendExtCommand("NAV", "$lat:$lon:$label")
                    sendSeq++
                }
            },
            color = colors.accentPrimary
        ) {
            Text("Start Nav", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        AckBadge(bleManager = bleManager, tag = "NAV", trigger = sendSeq.takeIf { it > 0 })
        Spacer(modifier = Modifier.width(Spacing.sm))
        TextButton(onClick = {
            bleManager.sendExtCommand("NAV", "")
            sendSeq++
        }) {
            Text("Stop Nav", fontSize = 12.sp, color = colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun PetModeInfo(medicalId: MedicalId, onMedicalIdChange: (MedicalId) -> Unit) {
    val colors = MaterialTheme.safeShadeColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ModeSectionLabel("What's Different")
        ModeBulletRow(Icons.Rounded.Block, "Fall detection is off in Pet mode.", colors.onSurfaceMuted)
        ModeBulletRow(
            Icons.Rounded.LinkOff,
            "Virtual leash: an unexpected disconnect triggers a distinct lost-pet alert (SMS + LED blink) automatically.",
            colors.accentWarning
        )
        ModeBulletRow(Icons.AutoMirrored.Rounded.DirectionsRun, "Activity tracking stays active.", colors.accentPrimary)
        Spacer(modifier = Modifier.height(Spacing.md))
        OwnerDetailsControl(medicalId, onMedicalIdChange)
    }
}

/**
 * Editable owner-details UI for Pet mode. No new BLE tag - contactName/
 * emergencyContact already sync to the device via the existing
 * HEALTH_CHAR_UUID/sendHealthData() path (wired outside this file) and are
 * already shown on the device's Pet-mode home screen as "Owner: {name} /
 * {contact}". These are the same underlying fields shown as "Emergency
 * Contact" in Elderly mode - genuinely dual-purpose.
 */
@Composable
private fun OwnerDetailsControl(medicalId: MedicalId, onMedicalIdChange: (MedicalId) -> Unit) {
    val colors = MaterialTheme.safeShadeColors
    var ownerName by rememberSaveable(medicalId.contactName) { mutableStateOf(medicalId.contactName) }
    var ownerContact by rememberSaveable(medicalId.emergencyContact) { mutableStateOf(medicalId.emergencyContact) }
    var justSaved by remember { mutableStateOf(false) }

    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(2000)
            justSaved = false
        }
    }

    ModeSectionLabel("Owner Details")
    Spacer(modifier = Modifier.height(Spacing.sm))
    OutlinedTextField(
        value = ownerName,
        onValueChange = { ownerName = it },
        label = { Text("Owner Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Spacing.xs))
    OutlinedTextField(
        value = ownerContact,
        onValueChange = { ownerContact = it },
        label = { Text("Owner Contact") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(Spacing.sm))
    Row(verticalAlignment = Alignment.CenterVertically) {
        BouncyButton(
            onClick = {
                onMedicalIdChange(medicalId.copy(contactName = ownerName, emergencyContact = ownerContact))
                justSaved = true
            },
            color = colors.accentPrimary
        ) {
            Text("Save", color = Color.White, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        AnimatedVisibility(visible = justSaved) {
            Text("Saved!", style = MaterialTheme.typography.labelMedium, color = colors.accentSuccess)
        }
    }
}

@Composable
private fun WristModeInfo() {
    val colors = MaterialTheme.safeShadeColors
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        ModeSectionLabel("Watch Face")
        ModeBulletRow(Icons.Rounded.Watch, "Home screen shows time, weather and battery at a glance.", colors.accentPrimary)
        ModeBulletRow(Icons.Rounded.Bedtime, "Sleep-time tracking is real, motion-based.", colors.accentPrimary)
        Spacer(modifier = Modifier.height(Spacing.xs))
        InfoNoteBox(
            icon = Icons.Rounded.Info,
            text = "Heart-rate/SpO2 on the device's Vitals screen are simulated - " +
                "approximated from motion patterns, not a real biometric sensor.",
            tint = colors.accentWarning
        )
    }
}

@Composable
private fun BackpackModeInfo() {
    val colors = MaterialTheme.safeShadeColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.sm))
            .background(colors.accentSuccess.copy(alpha = 0.06f))
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Verified, contentDescription = null, tint = colors.accentSuccess, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Balanced Default", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = colors.onSurface)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            "All standard features are active - no mode-specific overrides. Every other mode is a deliberate deviation from this baseline.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
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
 * Remote LED pattern control - relocated here from DeviceScreen so remote
 * customization (LED, mode) lives together on Profile, leaving DeviceScreen
 * as the read-only device/telemetry screen. An 8-chip grid: the 7 real
 * firmware LedPattern values (LED_CHAR_UUID write + real AckBadge round
 * trip) plus an "Auto" chip that's presentation-only - it updates local
 * selection but performs no BLE write at all (product decision: no wire
 * value exists for "auto" yet), so it shows a static "Default" subtitle
 * instead of a "sent"/AckBadge indicator.
 */
@Composable
private fun LedControlCard(
    bleManager: BleManager,
    enabled: Boolean,
    onPatternSelected: (LedPattern) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    var selected by remember { mutableStateOf<LedPattern?>(null) }
    var autoSelected by remember { mutableStateOf(false) }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = colors.accentWarning,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text("LED Pattern", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
                }
                // Value-qualified tag ("LED:TORCH" not just "LED") - with a
                // generic tag, rapidly tapping two different patterns could
                // let the first (slower) send's stale ack satisfy the second
                // pattern's badge, showing "SYNCED" for the wrong value.
                AckBadge(
                    bleManager = bleManager,
                    tag = selected?.let { "LED:${it.name}" } ?: "",
                    trigger = ackSeq.takeIf { it > 0 && !autoSelected }
                )
            }
            Spacer(modifier = Modifier.height(Spacing.lg))

            val ledOptions: List<LedPattern?> = remember { LedPattern.entries + listOf(null) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.heightIn(max = 300.dp)
            ) {
                gridItems(ledOptions) { pattern ->
                    if (pattern == null) {
                        AutoLedChip(
                            isSelected = autoSelected,
                            enabled = enabled,
                            onClick = {
                                autoSelected = true
                                selected = null
                            }
                        )
                    } else {
                        LedPatternChip(
                            pattern = pattern,
                            isSelected = !autoSelected && selected == pattern,
                            justSent = justSent == pattern,
                            enabled = enabled,
                            onClick = {
                                autoSelected = false
                                selected = pattern
                                justSent = pattern
                                ackSeq++
                                onPatternSelected(pattern)
                            }
                        )
                    }
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

/** The 8th, presentation-only chip: selectable local UI state, but never
 * writes to LED_CHAR_UUID - there's no "auto" wire value in the firmware's
 * RGBPattern enum yet, so this is an honest placeholder, not a fake sync. */
@Composable
private fun AutoLedChip(
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "autoChipBg"
    )
    val background = colors.accentSecondary.copy(alpha = 0.12f + 0.10f * bgAlpha)
    val borderColor = if (isSelected) colors.accentSecondary else colors.borderGlass

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
            "Auto",
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) colors.accentSecondary else colors.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Default",
            style = MaterialTheme.typography.labelSmall,
            color = colors.onSurfaceMuted
        )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = colors.accentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("Appearance", style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            }
            Spacer(modifier = Modifier.height(Spacing.md))

            // iOS-style segmented control - three equal-width pill segments
            // in one row, replacing the old vertical list of 3 SettingsRows.
            // Custom Row rather than Material3's SegmentedButton since
            // nothing else in this codebase opts into
            // @ExperimentalMaterial3Api, and this reuses the exact
            // animateColorAsState pattern PersonaModeChip already uses.
            val options = listOf(
                Triple(DarkModePreference.SYSTEM, "System", Icons.Rounded.BrightnessAuto),
                Triple(DarkModePreference.LIGHT, "Light", Icons.Rounded.LightMode),
                Triple(DarkModePreference.DARK, "Dark", Icons.Rounded.DarkMode)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.md))
                    .background(colors.accentPrimary.copy(alpha = 0.06f))
                    .padding(4.dp)
            ) {
                options.forEach { (pref, label, icon) ->
                    val selected = darkModePreference == pref
                    val segmentBg by animateColorAsState(
                        targetValue = if (selected) colors.accentPrimary else Color.Transparent,
                        animationSpec = tween(Motion.fast),
                        label = "segmentBg"
                    )
                    val segmentFg by animateColorAsState(
                        targetValue = if (selected) colors.surface else colors.onSurfaceMuted,
                        animationSpec = tween(Motion.fast),
                        label = "segmentFg"
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(Radius.sm))
                            .background(segmentBg)
                            .clickable { onDarkModeChange(pref) }
                            .padding(vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = label, tint = segmentFg, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            color = segmentFg,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
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
