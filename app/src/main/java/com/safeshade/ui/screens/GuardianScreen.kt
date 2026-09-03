/**
 * SafeShade - Universal Safety Companion
 *
 * GuardianScreen.kt
 *
 * Screen for Guardian/Companion messaging functionality.
 * Supports two modes:
 * - Guardian: Send messages to device user, manage safe zones (geofencing)
 * - Companion: Receive messages and send quick replies
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FIXES (this pass):
 *  - QuickRepliesCard (Companion mode) previously called
 *    bleManager.sendGuardianMessage("REPLY:...") which writes to
 *    MESSAGE_CHAR_UUID. The firmware's MessageCallbacks::onWrite treats
 *    ANY write to that characteristic as a brand-new incoming Guardian
 *    message - it buzzes the device and switches it to SCREEN_MESSAGE.
 *    Now calls bleManager.sendDeviceReply(...), which writes to
 *    REPLY_CHAR_UUID - the same characteristic the device's own hardware
 *    quick-reply flow notifies on.
 *  - Geofencing was a static "COMING SOON" card with no real functionality.
 *    Replaced with a real add/edit/remove flow backed by GeofenceManager
 *    (wired by the caller in SafeShadeApp.kt via onGeofenceZonesChange).
 *  - Message history rendering used raw unstyled AssistChips; now uses the
 *    shared MessageBubble component.
 *  - Added honest copy: geofencing/location cards no longer imply
 *    always-on live tracking (this app has no server/relay, only the
 *    last-synced phone GPS fix), and the Companion quick-reply card no
 *    longer implies instant push delivery (delivery only happens while
 *    the Guardian's phone is connected to the same BLE session; otherwise
 *    the firmware stores the reply and delivers it on next connect).
 */

package com.safeshade.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.window.Dialog
import com.safeshade.BleManager
import com.safeshade.data.GeofenceZone
import com.safeshade.data.LocationState
import com.safeshade.data.QuickMessage
import com.safeshade.data.SafetySettings
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Guardian/User screen for messaging between Guardian and Companion, plus
 * (Guardian mode) safe-zone / geofencing management.
 *
 * @param bleManager BLE manager for sending messages
 * @param safetySettings Safety settings (for parental controls)
 * @param isGuardianMode Current mode (true = Guardian, false = Companion)
 * @param onModeChange Callback when mode changes
 * @param messageHistory List of messages exchanged
 * @param onMessageSent Callback when a message is sent
 * @param onReply Callback (messageId, replyText) when a reply is sent
 * @param geofenceZones Current list of Guardian-defined safe zones
 * @param onGeofenceZonesChange Callback with the FULL updated zone list on any add/edit/remove
 * @param location Guardian phone's last known location (from last weather sync)
 * @param onRequestSensitivePermissions Callback to request extra runtime permissions contextually
 */
@Composable
fun GuardianUserScreen(
    bleManager: BleManager,
    safetySettings: SafetySettings,
    isGuardianMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    messageHistory: List<QuickMessage>,
    onMessageSent: (QuickMessage) -> Unit,
    onReply: (msgId: String, reply: String) -> Unit,
    geofenceZones: List<GeofenceZone>,
    onGeofenceZonesChange: (List<GeofenceZone>) -> Unit,
    location: LocationState,
    onRequestSensitivePermissions: (Array<String>) -> Unit
) {
    var showModeSelector by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pendingMode by remember { mutableStateOf(true) }

    // Mode selection dialog (when parental controls enabled)
    if (showModeSelector && safetySettings.parentalControlsEnabled) {
        ModeSelectionDialog(
            onDismiss = { showModeSelector = false },
            onSelectGuardian = {
                pendingMode = true
                showPinDialog = true
                showModeSelector = false
            },
            onSelectCompanion = {
                pendingMode = false
                showPinDialog = true
                showModeSelector = false
            }
        )
    }

    // PIN entry dialog
    if (showPinDialog) {
        PinEntryDialog(
            correctPin = safetySettings.parentalPin,
            onSuccess = {
                onModeChange(pendingMode)
                showPinDialog = false
            },
            onDismiss = { showPinDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with mode switch button
        GuardianScreenHeader(
            isGuardianMode = isGuardianMode,
            parentalControlsEnabled = safetySettings.parentalControlsEnabled,
            onModeSwitchClick = { showModeSelector = true }
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        // Content based on mode
        if (isGuardianMode) {
            GuardianModeContent(
                bleManager = bleManager,
                onMessageSent = onMessageSent,
                geofenceZones = geofenceZones,
                onGeofenceZonesChange = onGeofenceZonesChange,
                location = location,
                onRequestSensitivePermissions = onRequestSensitivePermissions
            )
        } else {
            CompanionModeContent(bleManager, messageHistory, onReply)
        }
    }
}

/**
 * Screen header showing current mode and switch button.
 */
@Composable
private fun GuardianScreenHeader(
    isGuardianMode: Boolean,
    parentalControlsEnabled: Boolean,
    onModeSwitchClick: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (isGuardianMode) "Guardian" else "Companion",
                style = MaterialTheme.typography.displayMedium,
                color = colors.onSurface
            )
            Text(
                text = if (isGuardianMode) "Send messages to your loved one"
                else "Stay connected with your guardian",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceMuted
            )
        }

        // Mode switch button (only visible when parental controls enabled)
        if (parentalControlsEnabled) {
            IconButton(
                onClick = onModeSwitchClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = 0.1f))
            ) {
                Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = "Switch Mode",
                    tint = AccentPurple
                )
            }
        }
    }
}

/**
 * Guardian mode content - for sending messages to device user and managing
 * safe zones.
 */
@Composable
fun GuardianModeContent(
    bleManager: BleManager,
    onMessageSent: (QuickMessage) -> Unit,
    geofenceZones: List<GeofenceZone>,
    onGeofenceZonesChange: (List<GeofenceZone>) -> Unit,
    location: LocationState,
    onRequestSensitivePermissions: (Array<String>) -> Unit
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"
    var customMessage by remember { mutableStateOf("") }
    var messageSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Quick messages card
    QuickMessagesCard(
        isConnected = isConnected,
        bleManager = bleManager,
        onMessageSent = onMessageSent
    )

    Spacer(modifier = Modifier.height(Spacing.lg))

    // Custom message card
    CustomMessageCard(
        customMessage = customMessage,
        onMessageChange = { if (it.length <= 60) customMessage = it },
        isConnected = isConnected,
        messageSent = messageSent,
        onSend = {
            if (customMessage.isNotBlank()) {
                bleManager.sendGuardianMessage(customMessage)
                onMessageSent(QuickMessage(text = customMessage, fromGuardian = true))
                messageSent = true
                scope.launch {
                    delay(2000)
                    messageSent = false
                }
                customMessage = ""
            }
        }
    )

    Spacer(modifier = Modifier.height(Spacing.lg))

    // Real geofencing / safe zones management
    GeofenceZonesCard(
        zones = geofenceZones,
        location = location,
        onZonesChange = onGeofenceZonesChange,
        onRequestSensitivePermissions = onRequestSensitivePermissions
    )

    Spacer(modifier = Modifier.height(Spacing.lg))

    // Honest note about what "location sharing" actually means here
    LocationShareInfoCard(location = location)
}

/**
 * Quick messages card with preset message buttons.
 */
@Composable
private fun QuickMessagesCard(
    isConnected: Boolean,
    bleManager: BleManager,
    onMessageSent: (QuickMessage) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            SectionHeader("Quick Messages")
            Spacer(modifier = Modifier.height(Spacing.lg))

            // Row 1
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("Come Home", AccentOrange, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("Come home now!")
                    onMessageSent(QuickMessage(text = "Come home now!", fromGuardian = true))
                }
                QuickMessageButton("Call Me", AccentBlue, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("Please call me!")
                    onMessageSent(QuickMessage(text = "Please call me!", fromGuardian = true))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("Stay Safe", AccentGreen, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("Stay safe! Love you")
                    onMessageSent(QuickMessage(text = "Stay safe! Love you", fromGuardian = true))
                }
                QuickMessageButton("Dinner!", AccentPurple, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("Dinner is ready!")
                    onMessageSent(QuickMessage(text = "Dinner is ready!", fromGuardian = true))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("I'm Here", AccentPink, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("I'm outside!")
                    onMessageSent(QuickMessage(text = "I'm outside!", fromGuardian = true))
                }
                QuickMessageButton("On My Way", AccentTeal, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("On my way to you!")
                    onMessageSent(QuickMessage(text = "On my way to you!", fromGuardian = true))
                }
            }
        }
    }
}

/**
 * Custom message input card.
 */
@Composable
private fun CustomMessageCard(
    customMessage: String,
    onMessageChange: (String) -> Unit,
    isConnected: Boolean,
    messageSent: Boolean,
    onSend: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            SectionHeader("Custom Message")
            Spacer(modifier = Modifier.height(Spacing.md))

            OutlinedTextField(
                value = customMessage,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type your message...", color = colors.onSurfaceMuted) },
                maxLines = 2,
                shape = RoundedCornerShape(Radius.sm)
            )

            Text(
                "${customMessage.length}/60 characters",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(Spacing.md))

            BouncyButton(
                onClick = onSend,
                enabled = isConnected && customMessage.isNotBlank(),
                color = AccentPurple,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (messageSent) Icons.Rounded.Check else Icons.Rounded.Send,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (messageSent) "Sent!" else "Send Message",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ============================================
// GEOFENCING / SAFE ZONES (Guardian mode)
// ============================================

/**
 * Real safe-zone management card: lists current [GeofenceZone]s and lets the
 * Guardian add/edit/remove them. Actual geofence registration with Android's
 * GeofencingClient happens in the caller (SafeShadeApp.kt -> GeofenceManager)
 * whenever [onZonesChange] fires with the full updated list - this composable
 * only owns the add/edit dialog and local list rendering.
 */
@Composable
private fun GeofenceZonesCard(
    zones: List<GeofenceZone>,
    location: LocationState,
    onZonesChange: (List<GeofenceZone>) -> Unit,
    onRequestSensitivePermissions: (Array<String>) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    var showEditor by remember { mutableStateOf(false) }
    var editingZone by remember { mutableStateOf<GeofenceZone?>(null) }

    if (showEditor) {
        GeofenceZoneDialog(
            initial = editingZone,
            location = location,
            onDismiss = { showEditor = false; editingZone = null },
            onSave = { zone ->
                val wasEmpty = zones.isEmpty()
                val updated = if (zones.any { it.id == zone.id }) {
                    zones.map { if (it.id == zone.id) zone else it }
                } else {
                    zones + zone
                }
                onZonesChange(updated)
                if (wasEmpty) {
                    onRequestSensitivePermissions(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                }
                showEditor = false
                editingZone = null
            }
        )
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl).animateContentSize()) {
            SectionHeader(
                title = "Safe Zones",
                trailingContent = {
                    IconButton(
                        onClick = { editingZone = null; showEditor = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Safe Zone", tint = AccentPurple)
                    }
                }
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                "Get alerted when the device enters or leaves a zone.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            if (zones.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.AddLocationAlt,
                    message = "No safe zones yet - tap + to add one"
                )
            } else {
                zones.forEachIndexed { index, zone ->
                    GeofenceZoneRow(
                        zone = zone,
                        onEdit = { editingZone = zone; showEditor = true },
                        onDelete = { onZonesChange(zones.filterNot { it.id == zone.id }) }
                    )
                    if (index != zones.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GeofenceZoneRow(
    zone: GeofenceZone,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(Radius.sm))
                .background(AccentGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(zone.name, style = MaterialTheme.typography.titleSmall, color = colors.onSurface)
            Text(
                "${zone.radiusMeters.toInt()} m radius",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                if (zone.alertOnExit) StatusBadge("ALERT ON EXIT", AccentRed)
                if (zone.alertOnEnter) StatusBadge("ALERT ON ENTER", AccentBlue)
            }
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit zone", tint = colors.onSurfaceMuted)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete zone", tint = AccentRed)
        }
    }
}

/**
 * Add/edit form for a [GeofenceZone]. There is no map library in this
 * project, so the center point is set via an explicit "Use my current
 * location" button off the Guardian phone's last-synced [LocationState]
 * rather than a map picker.
 */
@Composable
private fun GeofenceZoneDialog(
    initial: GeofenceZone?,
    location: LocationState,
    onDismiss: () -> Unit,
    onSave: (GeofenceZone) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var radius by remember { mutableStateOf(initial?.radiusMeters ?: 200f) }
    var alertOnExit by remember { mutableStateOf(initial?.alertOnExit ?: true) }
    var alertOnEnter by remember { mutableStateOf(initial?.alertOnEnter ?: false) }
    var centerLat by remember { mutableStateOf(initial?.lat) }
    var centerLon by remember { mutableStateOf(initial?.lon) }
    val centerSet = centerLat != null && centerLon != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(Radius.xl),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(Spacing.xl)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (initial == null) "Add Safe Zone" else "Edit Safe Zone",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                Spacer(modifier = Modifier.height(Spacing.lg))

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 40) name = it },
                    label = { Text("Zone Name") },
                    placeholder = { Text("Home, School, ...") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                Text(
                    "Radius: ${radius.toInt()} m",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 50f..2000f,
                    steps = 38,
                    colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                OutlinedButton(
                    onClick = {
                        if (location.isValid) {
                            centerLat = location.lat
                            centerLon = location.lon
                        }
                    },
                    enabled = location.isValid,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = AccentPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (centerSet) "Update Center to My Location" else "Use My Current Location")
                }

                Spacer(modifier = Modifier.height(Spacing.xs))

                Text(
                    if (centerSet)
                        "Center set: %.4f, %.4f".format(centerLat, centerLon)
                    else if (!location.isValid)
                        "No location fix yet - sync weather/location first."
                    else
                        "Center not set yet.",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (centerSet) AccentGreen else colors.onSurfaceMuted
                )

                Spacer(modifier = Modifier.height(Spacing.lg))

                SettingsRow(
                    icon = Icons.Rounded.Logout,
                    title = "Alert on Exit",
                    subtitle = "Notify when device leaves this zone",
                    iconTint = AccentRed,
                    trailingContent = {
                        Switch(
                            checked = alertOnExit,
                            onCheckedChange = { alertOnExit = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentRed)
                        )
                    }
                )
                SettingsRow(
                    icon = Icons.Rounded.Login,
                    title = "Alert on Enter",
                    subtitle = "Notify when device enters this zone",
                    iconTint = AccentBlue,
                    trailingContent = {
                        Switch(
                            checked = alertOnEnter,
                            onCheckedChange = { alertOnEnter = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = AccentBlue)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.xl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val lat = centerLat
                            val lon = centerLon
                            if (name.isNotBlank() && lat != null && lon != null) {
                                onSave(
                                    GeofenceZone(
                                        id = initial?.id ?: java.util.UUID.randomUUID().toString(),
                                        name = name.trim(),
                                        lat = lat,
                                        lon = lon,
                                        radiusMeters = radius,
                                        alertOnExit = alertOnExit,
                                        alertOnEnter = alertOnEnter
                                    )
                                )
                            }
                        },
                        enabled = name.isNotBlank() && centerSet,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(if (initial == null) "Add Zone" else "Save Changes")
                    }
                }
            }
        }
    }
}

/**
 * Honest, lightweight status card explaining what "location sharing" means
 * in this app: there is no server/relay, so this is the phone's last
 * weather-sync GPS fix, not a continuous live tracking stream.
 */
@Composable
private fun LocationShareInfoCard(location: LocationState) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Info,
                contentDescription = null,
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column {
                val label = when {
                    !location.isValid -> "No location synced yet"
                    location.locationName.isNotBlank() -> location.locationName
                    else -> "%.4f, %.4f".format(location.lat, location.lon)
                }
                Text(
                    "Last known location: $label",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurface
                )
                Text(
                    "This is the last GPS fix synced from this phone, not a continuous live tracking stream - SafeShade has no cloud/relay backend.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceMuted
                )
            }
        }
    }
}

/**
 * Companion mode content - for receiving messages and sending replies.
 */
@Composable
fun CompanionModeContent(
    bleManager: BleManager,
    messageHistory: List<QuickMessage>,
    onReply: (String, String) -> Unit
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"

    // Quick replies card
    QuickRepliesCard(isConnected = isConnected, bleManager = bleManager)

    Spacer(modifier = Modifier.height(Spacing.lg))

    // Recent messages card
    RecentMessagesCard(messageHistory = messageHistory, onReply = onReply)

    Spacer(modifier = Modifier.height(Spacing.lg))

    // Status card
    StatusCard(isConnected = isConnected)
}

/**
 * Quick replies card for Companion mode.
 */
@Composable
private fun QuickRepliesCard(
    isConnected: Boolean,
    bleManager: BleManager
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            SectionHeader(
                title = "Quick Replies",
                trailingContent = {
                    Text("Send to Guardian", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceMuted)
                }
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("I'm OK", AccentGreen, isConnected, Modifier.weight(1f)) {
                    bleManager.sendDeviceReply("I'm OK!")
                }
                QuickMessageButton("Coming!", AccentOrange, isConnected, Modifier.weight(1f)) {
                    bleManager.sendDeviceReply("On my way!")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("Need Help", AccentRed, isConnected, Modifier.weight(1f)) {
                    bleManager.sendDeviceReply("I need help!")
                }
                QuickMessageButton("Call You Soon", AccentBlue, isConnected, Modifier.weight(1f)) {
                    bleManager.sendDeviceReply("Will call soon")
                }
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = colors.onSurfaceMuted,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Delivered live only while the Guardian's phone is connected to this device. " +
                        "Otherwise the reply is stored on-device and delivered the next time it connects.",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceMuted
                )
            }
        }
    }
}

/**
 * Recent messages from Guardian card.
 *
 * @param onReply Callback (messageId, replyText) invoked when the user
 * taps a quick-reply chip. This is expected to both update local message
 * history AND actually transmit the reply to the device - see
 * SafeShadeApp.kt's onReply wiring, which now calls
 * bleManager.sendDeviceReply(reply) before updating history.
 */
@Composable
private fun RecentMessagesCard(
    messageHistory: List<QuickMessage>,
    onReply: (String, String) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            SectionHeader("Recent Messages from Guardian")
            Spacer(modifier = Modifier.height(Spacing.md))

            val guardianMessages = messageHistory.filter { it.fromGuardian }.take(5)

            if (guardianMessages.isEmpty()) {
                EmptyState(icon = Icons.Rounded.MarkChatUnread, message = "No messages yet")
            } else {
                guardianMessages.forEach { msg ->
                    MessageItem(
                        message = msg,
                        onReply = { reply -> onReply(msg.id, reply) }
                    )
                    if (msg != guardianMessages.last()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Single message item with reply functionality. Message text/timestamp now
 * renders via the shared MessageBubble component instead of raw Text/AssistChip.
 */
@Composable
fun MessageItem(
    message: QuickMessage,
    onReply: (String) -> Unit
) {
    var showReplyOptions by remember { mutableStateOf(false) }
    val colors = MaterialTheme.safeShadeColors
    val timestampLabel = remember(message.timestamp) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
    }

    Column(modifier = Modifier.animateContentSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            MessageBubble(
                text = message.text,
                fromGuardian = true,
                timestampLabel = timestampLabel,
                modifier = Modifier.weight(1f)
            )

            if (!message.replied) {
                IconButton(onClick = { showReplyOptions = !showReplyOptions }) {
                    Icon(Icons.Rounded.Reply, contentDescription = "Reply", tint = AccentPurple)
                }
            }
        }

        if (message.replied && message.replyText != null) {
            Text(
                "↪ ${message.replyText}",
                style = MaterialTheme.typography.bodySmall,
                color = AccentGreen,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Reply options chips
        AnimatedVisibility(
            visible = showReplyOptions,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                listOf("OK!", "Coming!", "5 min").forEach { reply ->
                    AssistChip(
                        onClick = {
                            onReply(reply)
                            showReplyOptions = false
                        },
                        label = { Text(reply, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        }
    }
}

/**
 * Status card showing device connection status.
 */
@Composable
private fun StatusCard(isConnected: Boolean) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            SectionHeader("My Status")
            Spacer(modifier = Modifier.height(Spacing.md))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionStatusChip(
                    connected = isConnected,
                    connectedLabel = "Device Connected",
                    disconnectedLabel = "Device Offline"
                )
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    color = colors.onSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
