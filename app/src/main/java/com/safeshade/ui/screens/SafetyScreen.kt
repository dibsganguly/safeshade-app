/**
 * SafeShade - Universal Safety Companion
 *
 * SafetyScreen.kt
 *
 * Safety management screen with Find Device, emergency contacts,
 * fall alert history, and safety settings.
 *
 * @author SafeShade Team
 * @version 3.0.0
 *
 * FIXES (this pass):
 *  - Auto-call emergency toggle now requests CALL_PHONE right when turned on
 *    (via onRequestSensitivePermissions) instead of silently deferring, and
 *    its subtitle explains the real flow: a countdown dialog appears first,
 *    the user can dismiss or call immediately, nothing dials in the
 *    background. The real call-placing lives in EmergencyActions.kt / the
 *    FallAlertDialog in SafeShadeApp.kt — this screen only surfaces the
 *    setting and permission request.
 *  - Added the SMS fallback alert toggle (new SafetySettings.smsFallbackEnabled
 *    field), requesting SEND_SMS when turned on.
 *  - Migrated off raw fontSize=/Color(0x...) literals onto
 *    MaterialTheme.typography.* / MaterialTheme.safeShadeColors.*, and onto
 *    GlassCard instead of plain Card.
 */

package com.safeshade.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safeshade.BleManager
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.SafetySettings
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Safety screen - Manages device safety features.
 *
 * Features:
 * - Find My Device button
 * - Emergency contacts management
 * - Fall alert history
 * - Safety settings (sensitivity, auto-call, SMS fallback, parental controls)
 *
 * @param bleManager BLE manager for device commands
 * @param safetySettings Current safety settings
 * @param onSettingsChange Callback when settings change
 * @param fallHistory List of fall alert events
 * @param onRequestSensitivePermissions Callback to request contextual runtime permissions
 *   (CALL_PHONE / SEND_SMS) exactly when the user turns on the toggle that needs them
 */
@Composable
fun SafetyScreen(
    bleManager: BleManager,
    safetySettings: SafetySettings,
    onSettingsChange: (SafetySettings) -> Unit,
    fallHistory: List<FallAlertEvent>,
    onRequestSensitivePermissions: (Array<String>) -> Unit
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"

    // Dialog states
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    // Tracks every settings write made from this screen so an AckBadge can
    // show a real "applied on device" confirmation (SETTINGS_CHAR_UUID's
    // ACK) instead of just trusting the local state update.
    var settingsAckSeq by remember { mutableStateOf(0) }
    val trackedOnSettingsChange: (SafetySettings) -> Unit = { newSettings ->
        onSettingsChange(newSettings)
        settingsAckSeq++
    }

    // Dialogs
    if (showAddContactDialog) {
        AddEmergencyContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAdd = { contact ->
                trackedOnSettingsChange(safetySettings.copy(
                    emergencyContacts = safetySettings.emergencyContacts + contact
                ))
                showAddContactDialog = false
            }
        )
    }

    if (showSensitivityDialog) {
        SensitivityDialog(
            currentSensitivity = safetySettings.fallSensitivity,
            onDismiss = { showSensitivityDialog = false },
            onSelect = { sensitivity ->
                trackedOnSettingsChange(safetySettings.copy(fallSensitivity = sensitivity))
                showSensitivityDialog = false
            }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onSave = { pin ->
                trackedOnSettingsChange(safetySettings.copy(parentalPin = pin))
                showPinSetupDialog = false
            }
        )
    }

    val colors = MaterialTheme.safeShadeColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.xl)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Safety", style = MaterialTheme.typography.displayMedium, color = colors.onSurface)
        Spacer(modifier = Modifier.height(Spacing.xl))

        // Find Device Button — kept first and motion-free so it's never slowed down.
        FindDeviceButton(
            isConnected = isConnected,
            onClick = { bleManager.sendCommand("CMD_FIND") }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Emergency Contacts Card
        EmergencyContactsCard(
            contacts = safetySettings.emergencyContacts,
            onAddClick = { showAddContactDialog = true },
            onDeleteContact = { contact ->
                trackedOnSettingsChange(safetySettings.copy(
                    emergencyContacts = safetySettings.emergencyContacts - contact
                ))
            },
            onSetPrimary = { contact ->
                val updated = safetySettings.emergencyContacts.map {
                    it.copy(isPrimary = it == contact)
                }
                trackedOnSettingsChange(safetySettings.copy(emergencyContacts = updated))
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Fall Alert History
        FallHistoryCard(fallHistory = fallHistory)

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Alert Settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Alert Settings",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            com.safeshade.ui.components.AckBadge(
                bleManager = bleManager,
                tag = "SETTINGS",
                trigger = settingsAckSeq.takeIf { it > 0 }
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        AlertSettingsCard(
            safetySettings = safetySettings,
            onSettingsChange = trackedOnSettingsChange,
            onSensitivityClick = { showSensitivityDialog = true },
            onPinSetupClick = { showPinSetupDialog = true },
            onRequestSensitivePermissions = onRequestSensitivePermissions
        )
    }
}

/**
 * Large Find Device button that triggers SOS alarm on device.
 */
@Composable
private fun FindDeviceButton(
    isConnected: Boolean,
    onClick: () -> Unit
) {
    BouncyButton(
        onClick = onClick,
        enabled = isConnected,
        color = AccentRed,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.NotificationsActive,
                contentDescription = "Find Device",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "FIND MY DEVICE",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
            Text(
                "Trigger alarm on SafeShade",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

/**
 * Emergency contacts management card.
 */
@Composable
private fun EmergencyContactsCard(
    contacts: List<EmergencyContact>,
    onAddClick: () -> Unit,
    onDeleteContact: (EmergencyContact) -> Unit,
    onSetPrimary: (EmergencyContact) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Emergency Contacts",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface
                )
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Contact", tint = colors.accentPrimary)
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))

            if (contacts.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.ContactPhone,
                    message = "No emergency contacts yet"
                )
            } else {
                contacts.forEach { contact ->
                    key(contact.name, contact.phone) {
                        EmergencyContactRow(
                            contact = contact,
                            onDelete = { onDeleteContact(contact) },
                            onSetPrimary = { onSetPrimary(contact) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single emergency contact row.
 */
@Composable
fun EmergencyContactRow(
    contact: EmergencyContact,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(Motion.normal))
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (contact.isPrimary) colors.accentPrimary else colors.background),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = if (contact.isPrimary) Color.White else colors.onSurfaceMuted,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    StatusBadge(text = "PRIMARY", color = colors.accentSuccess)
                }
            }
            Text(contact.phone, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceMuted)
        }

        // Action buttons
        if (!contact.isPrimary) {
            IconButton(onClick = onSetPrimary) {
                Icon(Icons.Rounded.Star, contentDescription = "Set Primary", tint = colors.onSurfaceMuted, modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = colors.accentDanger, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Fall alert history card.
 */
@Composable
private fun FallHistoryCard(fallHistory: List<FallAlertEvent>) {
    val colors = MaterialTheme.safeShadeColors
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                "Fall Alert History",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )
            Spacer(modifier = Modifier.height(Spacing.md))

            if (fallHistory.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.History,
                    message = "No fall alerts recorded"
                )
            } else {
                fallHistory.take(10).forEach { event ->
                    key(event.id) {
                        AlertLogItem(event)
                    }
                }
            }
        }
    }
}

/**
 * Single fall alert log item.
 */
@Composable
fun AlertLogItem(event: FallAlertEvent) {
    val colors = MaterialTheme.safeShadeColors
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.Top
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .offset(y = 5.dp)
                .clip(CircleShape)
                .background(
                    when {
                        event.wasEmergencyContacted -> colors.accentDanger
                        event.action.contains("dismissed", ignoreCase = true) -> colors.accentWarning
                        else -> colors.accentSuccess
                    }
                )
        )

        Spacer(modifier = Modifier.width(Spacing.md))

        Column {
            Text(event.eventType, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = colors.onSurface)
            Text(
                "${dateFormat.format(Date(event.timestamp))} • ${event.action}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
            if (event.wasEmergencyContacted) {
                Text(
                    "Emergency contact notified",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.accentDanger
                )
            }
        }
    }
}

/**
 * Alert settings card.
 */
@Composable
private fun AlertSettingsCard(
    safetySettings: SafetySettings,
    onSettingsChange: (SafetySettings) -> Unit,
    onSensitivityClick: () -> Unit,
    onPinSetupClick: () -> Unit,
    onRequestSensitivePermissions: (Array<String>) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                "Alert Settings",
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Auto-call toggle
            SettingsToggleRow(
                title = "Auto-call emergency contact",
                subtitle = "On a fall, an alert dialog with a visible countdown appears first — " +
                    "you can dismiss it or call immediately. Nothing dials silently in the " +
                    "background. Needs Phone permission to place a real call; otherwise the " +
                    "dialer opens pre-filled and you tap to call.",
                checked = safetySettings.autoCallEmergency,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onRequestSensitivePermissions(arrayOf(Manifest.permission.CALL_PHONE))
                    }
                    onSettingsChange(safetySettings.copy(autoCallEmergency = enabled))
                }
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = colors.borderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(Spacing.sm))

            // SMS fallback alert toggle
            SettingsToggleRow(
                title = "SMS fallback alert",
                subtitle = "Also send a text to your primary emergency contact when a fall is " +
                    "confirmed — useful if the call can't connect. Needs SMS permission.",
                checked = safetySettings.smsFallbackEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        onRequestSensitivePermissions(arrayOf(Manifest.permission.SEND_SMS))
                    }
                    onSettingsChange(safetySettings.copy(smsFallbackEnabled = enabled))
                }
            )

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = colors.borderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Fall sensitivity
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Radius.sm))
                    .clickable { onSensitivityClick() }
                    .padding(vertical = Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Fall detection sensitivity",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        safetySettings.fallSensitivity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceMuted
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        safetySettings.fallSensitivity.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = colors.onSurfaceMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = colors.borderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(Spacing.md))

            // SOS Volume — real, forwarded to firmware via bleManager.sendSettings() at the call site.
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SOS Alarm Volume",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = colors.onSurface
                    )
                    Text(
                        "${(safetySettings.sosVolumeLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accentPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
                Slider(
                    value = safetySettings.sosVolumeLevel,
                    onValueChange = { onSettingsChange(safetySettings.copy(sosVolumeLevel = it)) },
                    colors = SliderDefaults.colors(
                        thumbColor = colors.accentPrimary,
                        activeTrackColor = colors.accentPrimary,
                        inactiveTrackColor = colors.accentPrimary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))
            HorizontalDivider(color = colors.borderGlass, thickness = 1.dp)
            Spacer(modifier = Modifier.height(Spacing.md))

            // Parental controls
            SettingsToggleRow(
                title = "Enable Parental Controls",
                subtitle = "Password protect Guardian/Companion mode",
                checked = safetySettings.parentalControlsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) onPinSetupClick()
                    onSettingsChange(safetySettings.copy(parentalControlsEnabled = enabled))
                }
            )

            AnimatedVisibility(
                visible = safetySettings.parentalControlsEnabled,
                enter = fadeIn(tween(Motion.normal)) + expandVertically(tween(Motion.normal)),
                exit = fadeOut(tween(Motion.fast)) + shrinkVertically(tween(Motion.fast))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    TextButton(
                        onClick = onPinSetupClick,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Change PIN", color = colors.accentPrimary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

/**
 * Reusable settings toggle row.
 */
@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceMuted
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = colors.accentPrimary,
                checkedThumbColor = Color.White
            )
        )
    }
}
