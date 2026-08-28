/**
 * SafeShade - Universal Safety Companion
 *
 * SafetyScreen.kt
 *
 * Safety management screen with Find Device, emergency contacts,
 * fall alert history, and safety settings.
 *
 * @author SafeShade Team
 * @version 2.1.0
 */

package com.safeshade.ui.screens

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
import androidx.compose.ui.unit.sp
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
 * - Safety settings (sensitivity, auto-call, parental controls)
 *
 * @param bleManager BLE manager for device commands
 * @param safetySettings Current safety settings
 * @param onSettingsChange Callback when settings change
 * @param fallHistory List of fall alert events
 */
@Composable
fun SafetyScreen(
    bleManager: BleManager,
    safetySettings: SafetySettings,
    onSettingsChange: (SafetySettings) -> Unit,
    fallHistory: List<FallAlertEvent>
) {
    val connectionState by bleManager.connectionState.collectAsState()
    val isConnected = connectionState == "Connected"

    // Dialog states
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showSensitivityDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }

    // Dialogs
    if (showAddContactDialog) {
        AddEmergencyContactDialog(
            onDismiss = { showAddContactDialog = false },
            onAdd = { contact ->
                onSettingsChange(safetySettings.copy(
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
                onSettingsChange(safetySettings.copy(fallSensitivity = sensitivity))
                showSensitivityDialog = false
            }
        )
    }

    if (showPinSetupDialog) {
        PinSetupDialog(
            onDismiss = { showPinSetupDialog = false },
            onSave = { pin ->
                onSettingsChange(safetySettings.copy(parentalPin = pin))
                showPinSetupDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Safety", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(24.dp))

        // Find Device Button
        FindDeviceButton(
            isConnected = isConnected,
            onClick = { bleManager.sendCommand("CMD_FIND") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Emergency Contacts Card
        EmergencyContactsCard(
            contacts = safetySettings.emergencyContacts,
            onAddClick = { showAddContactDialog = true },
            onDeleteContact = { contact ->
                onSettingsChange(safetySettings.copy(
                    emergencyContacts = safetySettings.emergencyContacts - contact
                ))
            },
            onSetPrimary = { contact ->
                val updated = safetySettings.emergencyContacts.map {
                    it.copy(isPrimary = it == contact)
                }
                onSettingsChange(safetySettings.copy(emergencyContacts = updated))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Fall Alert History
        FallHistoryCard(fallHistory = fallHistory)

        Spacer(modifier = Modifier.height(16.dp))

        // Alert Settings - FIXED FORMATTING
        AlertSettingsCard(
            safetySettings = safetySettings,
            onSettingsChange = onSettingsChange,
            onSensitivityClick = { showSensitivityDialog = true },
            onPinSetupClick = { showPinSetupDialog = true }
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Trigger alarm on SafeShade",
                fontSize = 11.sp,
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Emergency Contacts", fontWeight = FontWeight.Bold, color = TextDark)
                IconButton(onClick = onAddClick) {
                    Icon(Icons.Rounded.PersonAdd, contentDescription = "Add Contact", tint = AccentPurple)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            contacts.forEach { contact ->
                EmergencyContactRow(
                    contact = contact,
                    onDelete = { onDeleteContact(contact) },
                    onSetPrimary = { onSetPrimary(contact) }
                )
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (contact.isPrimary) AccentPurple else BgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Person,
                contentDescription = null,
                tint = if (contact.isPrimary) Color.White else TextGray,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    contact.name,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (contact.isPrimary) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("PRIMARY", fontSize = 9.sp, color = AccentGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(contact.phone, fontSize = 12.sp, color = TextGray)
        }

        // Action buttons
        if (!contact.isPrimary) {
            IconButton(onClick = onSetPrimary) {
                Icon(Icons.Rounded.Star, contentDescription = "Set Primary", tint = TextGray, modifier = Modifier.size(20.dp))
            }
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Fall alert history card.
 */
@Composable
private fun FallHistoryCard(fallHistory: List<FallAlertEvent>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Fall Alert History", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            if (fallHistory.isEmpty()) {
                Text(
                    "No fall alerts recorded",
                    color = TextGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                fallHistory.take(10).forEach { event ->
                    AlertLogItem(event)
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
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
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
                        event.wasEmergencyContacted -> AccentRed
                        event.action.contains("dismissed", ignoreCase = true) -> AccentOrange
                        else -> AccentGreen
                    }
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(event.eventType, fontWeight = FontWeight.Medium, color = TextDark, fontSize = 14.sp)
            Text(
                "${dateFormat.format(Date(event.timestamp))} • ${event.action}",
                fontSize = 12.sp,
                color = TextGray
            )
            if (event.wasEmergencyContacted) {
                Text(
                    "Emergency contact notified",
                    fontSize = 11.sp,
                    color = AccentRed
                )
            }
        }
    }
}

/**
 * Alert settings card - FIXED FORMATTING (Issue #2.2)
 */
@Composable
private fun AlertSettingsCard(
    safetySettings: SafetySettings,
    onSettingsChange: (SafetySettings) -> Unit,
    onSensitivityClick: () -> Unit,
    onPinSetupClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Alert Settings",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Auto-call toggle - FIXED FORMATTING
            SettingsToggleRow(
                title = "Auto-call emergency contact",
                subtitle = "Call primary contact after 30s of fall detection",
                checked = safetySettings.autoCallEmergency,
                onCheckedChange = {
                    onSettingsChange(safetySettings.copy(autoCallEmergency = it))
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BgColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Fall sensitivity - FIXED FORMATTING
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSensitivityClick() }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Fall detection sensitivity",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        safetySettings.fallSensitivity.description,
                        fontSize = 12.sp,
                        color = TextGray,
                        lineHeight = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        safetySettings.fallSensitivity.label,
                        fontSize = 14.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BgColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // SOS Volume - FIXED FORMATTING
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SOS Alarm Volume",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                    Text(
                        "${(safetySettings.sosVolumeLevel * 100).toInt()}%",
                        fontSize = 14.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = safetySettings.sosVolumeLevel,
                    onValueChange = { onSettingsChange(safetySettings.copy(sosVolumeLevel = it)) },
                    colors = SliderDefaults.colors(
                        thumbColor = AccentPurple,
                        activeTrackColor = AccentPurple,
                        inactiveTrackColor = AccentPurple.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BgColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Parental controls - FIXED FORMATTING
            SettingsToggleRow(
                title = "Enable Parental Controls",
                subtitle = "Password protect Guardian/Companion mode",
                checked = safetySettings.parentalControlsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) onPinSetupClick()
                    onSettingsChange(safetySettings.copy(parentalControlsEnabled = enabled))
                }
            )

            if (safetySettings.parentalControlsEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onPinSetupClick,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Change PIN", color = AccentPurple, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Reusable settings toggle row - FIXED FORMATTING
 */
@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = TextGray,
                lineHeight = 16.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AccentPurple,
                checkedThumbColor = Color.White
            )
        )
    }
}
