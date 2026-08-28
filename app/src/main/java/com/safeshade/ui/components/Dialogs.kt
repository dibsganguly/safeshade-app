/**
 * SafeShade - Universal Safety Companion
 *
 * Dialogs.kt
 *
 * All dialog composables used throughout the application.
 * Includes editors, pickers, and confirmation dialogs.
 *
 * @author SafeShade Team
 * @version 2.1.0
 */

package com.safeshade.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.safeshade.data.*
import com.safeshade.ui.theme.*

// ============================================
// MODE SELECTION DIALOG
// ============================================

/**
 * Dialog for selecting between Guardian and Companion modes.
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onSelectGuardian Callback when Guardian mode selected
 * @param onSelectCompanion Callback when Companion mode selected
 */
@Composable
fun ModeSelectionDialog(
    onDismiss: () -> Unit,
    onSelectGuardian: () -> Unit,
    onSelectCompanion: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select Mode",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Guardian option
                    ModeOptionCard(
                        icon = Icons.Rounded.Shield,
                        title = "Guardian",
                        subtitle = "Send messages",
                        color = AccentPurple,
                        onClick = onSelectGuardian,
                        modifier = Modifier.weight(1f)
                    )

                    // Companion option
                    ModeOptionCard(
                        icon = Icons.Rounded.Person,
                        title = "Companion",
                        subtitle = "Reply & status",
                        color = AccentOrange,
                        onClick = onSelectCompanion,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextGray)
                }
            }
        }
    }
}

/**
 * Single mode option card used in ModeSelectionDialog.
 */
@Composable
private fun ModeOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, fontSize = 11.sp, color = TextGray)
        }
    }
}

// ============================================
// PIN DIALOGS
// ============================================

/**
 * PIN entry dialog for parental controls verification.
 *
 * @param correctPin The correct PIN to match
 * @param onSuccess Callback when correct PIN entered
 * @param onDismiss Callback when dialog dismissed
 */
@Composable
fun PinEntryDialog(
    correctPin: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Enter PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4) {
                            pin = it
                            error = false
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    supportingText = if (error) {
                        { Text("Incorrect PIN") }
                    } else null,
                    modifier = Modifier.width(150.dp),
                    textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (pin == correctPin) {
                                onSuccess()
                            } else {
                                error = true
                                pin = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

/**
 * PIN setup dialog for creating/changing parental PIN.
 *
 * @param onDismiss Callback when dialog dismissed
 * @param onSave Callback with new PIN when saved
 */
@Composable
fun PinSetupDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Set Parental PIN",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text("Confirm PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            when {
                                pin.length != 4 -> error = "PIN must be 4 digits"
                                pin != confirmPin -> error = "PINs don't match"
                                else -> onSave(pin)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ============================================
// EMERGENCY CONTACT DIALOG
// ============================================

/**
 * Dialog for adding a new emergency contact.
 *
 * @param onDismiss Callback when dialog dismissed
 * @param onAdd Callback with new contact when added
 */
@Composable
fun AddEmergencyContactDialog(
    onDismiss: () -> Unit,
    onAdd: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add Emergency Contact",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                onAdd(EmergencyContact(name, phone))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

// ============================================
// SENSITIVITY DIALOG
// ============================================

/**
 * Dialog for selecting fall detection sensitivity level.
 *
 * @param currentSensitivity Currently selected sensitivity
 * @param onDismiss Callback when dialog dismissed
 * @param onSelect Callback with selected sensitivity
 */
@Composable
fun SensitivityDialog(
    currentSensitivity: FallSensitivity,
    onDismiss: () -> Unit,
    onSelect: (FallSensitivity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Fall Detection Sensitivity",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                FallSensitivity.entries.forEach { sensitivity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(sensitivity) }
                            .background(
                                if (sensitivity == currentSensitivity)
                                    AccentPurple.copy(alpha = 0.1f)
                                else Color.Transparent
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sensitivity == currentSensitivity,
                            onClick = { onSelect(sensitivity) },
                            colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                sensitivity.label,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                            Text(
                                sensitivity.description,
                                fontSize = 12.sp,
                                color = TextGray
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================
// MEDICAL ID EDITOR DIALOG
// ============================================

/**
 * Full-screen dialog for editing medical ID information.
 *
 * @param medicalId Current medical ID data
 * @param onDismiss Callback when dialog dismissed
 * @param onSave Callback with updated medical ID
 */
@Composable
fun MedicalIdEditorDialog(
    medicalId: MedicalId,
    onDismiss: () -> Unit,
    onSave: (MedicalId) -> Unit
) {
    var bloodType by remember { mutableStateOf(medicalId.bloodType) }
    var emergencyContact by remember { mutableStateOf(medicalId.emergencyContact) }
    var contactName by remember { mutableStateOf(medicalId.contactName) }
    var allergies by remember { mutableStateOf(medicalId.allergies) }
    var medicalNotes by remember { mutableStateOf(medicalId.medicalNotes) }
    var age by remember { mutableStateOf(medicalId.age.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Medical ID",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, null, tint = TextGray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Form fields
                OutlinedTextField(
                    value = bloodType,
                    onValueChange = { bloodType = it },
                    label = { Text("Blood Type") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emergencyContact,
                    onValueChange = { emergencyContact = it },
                    label = { Text("Emergency Contact Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = allergies,
                    onValueChange = { allergies = it },
                    label = { Text("Allergies") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    label = { Text("Medical Notes") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save button
                Button(
                    onClick = {
                        onSave(
                            MedicalId(
                                bloodType = bloodType,
                                emergencyContact = emergencyContact,
                                contactName = contactName,
                                allergies = allergies,
                                medicalNotes = medicalNotes,
                                age = age.toIntOrNull() ?: 0
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Changes")
                }
            }
        }
    }
}

// ============================================
// DEVICE SETTINGS DIALOG
// ============================================

/**
 * Dialog for editing device settings (name, user, icon).
 *
 * @param deviceSettings Current device settings
 * @param onDismiss Callback when dialog dismissed
 * @param onSave Callback with updated settings
 * @param onIconPickerRequest Callback to open icon picker
 */
@Composable
fun DeviceSettingsDialog(
    deviceSettings: DeviceSettings,
    onDismiss: () -> Unit,
    onSave: (DeviceSettings) -> Unit,
    onIconPickerRequest: () -> Unit
) {
    var name by remember { mutableStateOf(deviceSettings.name) }
    var primaryUserName by remember { mutableStateOf(deviceSettings.primaryUserName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Device Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Icon selection row
                Text("Device Icon", fontSize = 14.sp, color = TextGray)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, TextGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable { onIconPickerRequest() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AccentPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(deviceSettings.iconType.icon, null, tint = AccentPurple)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(deviceSettings.iconType.label, color = TextDark)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.ChevronRight, null, tint = TextGray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = primaryUserName,
                    onValueChange = { primaryUserName = it },
                    label = { Text("Primary User Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            onSave(deviceSettings.copy(
                                name = name,
                                primaryUserName = primaryUserName
                            ))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

// ============================================
// ICON PICKER DIALOG - FIXED TEXT CUTOFF (Issue #3.2.1)
// ============================================

/**
 * Grid dialog for selecting device icon.
 * FIXED: Increased box size and improved text display to prevent cutoff
 *
 * @param currentIcon Currently selected icon
 * @param onDismiss Callback when dialog dismissed
 * @param onSelect Callback with selected icon
 */
@Composable
fun IconPickerDialog(
    currentIcon: DeviceIconType,
    onDismiss: () -> Unit,
    onSelect: (DeviceIconType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "Choose Device Icon",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Grid of icons (4 per row) - FIXED sizing
                val icons = DeviceIconType.entries.chunked(4)
                icons.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { iconType ->
                            IconOption(
                                iconType = iconType,
                                isSelected = iconType == currentIcon,
                                onClick = { onSelect(iconType) }
                            )
                        }
                        // Fill empty slots to maintain grid alignment
                        repeat(4 - row.size) {
                            Spacer(modifier = Modifier.size(70.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

/**
 * Single icon option in the picker grid.
 * FIXED: Increased size to 70dp and improved text display
 */
@Composable
private fun IconOption(
    iconType: DeviceIconType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(70.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) AccentPurple
                else AccentPurple.copy(alpha = 0.1f)
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            iconType.icon,
            contentDescription = iconType.label,
            tint = if (isSelected) Color.White else AccentPurple,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            iconType.label,
            fontSize = 10.sp,
            color = if (isSelected) Color.White else TextGray,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================
// ADD DEVICE DIALOG
// ============================================

/**
 * Dialog showing device pairing UI (placeholder).
 *
 * @param onDismiss Callback when dialog dismissed
 */
@Composable
fun AddDeviceDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.BluetoothSearching,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Pair New Device",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Make sure your SafeShade device is powered on and in pairing mode.",
                    textAlign = TextAlign.Center,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Simulated scanning indicator
                CircularProgressIndicator(color = AccentPurple)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Searching for devices...", color = TextGray, fontSize = 14.sp)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    }
}
