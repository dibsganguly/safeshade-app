/**
 * SafeShade - Universal Safety Companion
 *
 * ProfileScreen.kt
 *
 * User profile management screen with Medical ID editor,
 * device settings, and app information.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.data.DeviceSettings
import com.safeshade.data.MedicalId
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*
import com.safeshade.BleManager
import com.safeshade.data.*
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*


/**
 * Profile screen - User profile and device management.
 *
 * Features:
 * - Medical ID display and editor
 * - Device settings and icon customization
 * - Pair new device placeholder
 * - App information
 *
 * @param medicalId Current medical ID data
 * @param onMedicalIdChange Callback when medical ID is updated
 * @param deviceSettings Current device settings
 * @param onDeviceSettingsChange Callback when device settings change
 */
@Composable
fun ProfileScreen(
    medicalId: MedicalId,
    onMedicalIdChange: (MedicalId) -> Unit,
    deviceSettings: DeviceSettings,
    onDeviceSettingsChange: (DeviceSettings) -> Unit
) {
    // Dialog states
    var showMedicalIdEditor by remember { mutableStateOf(false) }
    var showDeviceSettings by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }
    var showAddDevice by remember { mutableStateOf(false) }

    // ============================================
    // DIALOGS
    // ============================================
    if (showMedicalIdEditor) {
        MedicalIdEditorDialog(
            medicalId = medicalId,
            onDismiss = { showMedicalIdEditor = false },
            onSave = {
                onMedicalIdChange(it)
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
            onDismiss = { showAddDevice = false }
        )
    }

    // ============================================
    // MAIN CONTENT
    // ============================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(24.dp))

        // Medical ID Card
        MedicalIdCard(
            medicalId = medicalId,
            onEditClick = { showMedicalIdEditor = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // My Devices Card
        MyDevicesCard(
            deviceSettings = deviceSettings,
            onDeviceClick = { showDeviceSettings = true },
            onAddDeviceClick = { showAddDevice = true }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App Info Card
        AppInfoCard()
    }
}

/**
 * Medical ID display card with edit button.
 */
@Composable
private fun MedicalIdCard(
    medicalId: MedicalId,
    onEditClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
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
                        tint = AccentRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Medical ID", fontWeight = FontWeight.Bold, color = TextDark)
                }
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = "Edit Medical ID",
                        tint = AccentPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
 * My Devices card showing paired devices and add button.
 */
@Composable
private fun MyDevicesCard(
    deviceSettings: DeviceSettings,
    onDeviceClick: () -> Unit,
    onAddDeviceClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("My Devices", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            // Current device row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDeviceClick() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Device icon
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentPurple.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        deviceSettings.iconType.icon,
                        contentDescription = null,
                        tint = AccentPurple
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Device info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        deviceSettings.name,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        "${deviceSettings.primaryUserName}'s ${deviceSettings.iconType.label}",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                // Primary badge
                if (deviceSettings.isPrimary) {
                    Box(
                        modifier = Modifier
                            .background(AccentGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "PRIMARY",
                            fontSize = 9.sp,
                            color = AccentGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = "Device Settings",
                    tint = TextGray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Add device button
            OutlinedButton(
                onClick = onAddDeviceClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentPurple)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Pair New Device")
            }
        }
    }
}

/**
 * App information card.
 */
@Composable
private fun AppInfoCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("App Information", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileField("Version", "2.0.0")
            ProfileField("Build", "Demo Day Edition")
        }
    }
}

/**
 * Single profile field with label and value.
 */
@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = TextGray)
        Text(value, fontSize = 16.sp, color = TextDark, fontWeight = FontWeight.Medium)
    }
}
