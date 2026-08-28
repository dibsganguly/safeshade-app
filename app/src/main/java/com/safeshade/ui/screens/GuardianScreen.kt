/**
 * SafeShade - Universal Safety Companion
 *
 * GuardianScreen.kt
 *
 * Screen for Guardian/Companion messaging functionality.
 * Supports two modes:
 * - Guardian: Send messages to device user
 * - Companion: Receive messages and send quick replies
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.BleManager
import com.safeshade.data.QuickMessage
import com.safeshade.data.SafetySettings
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.safeshade.data.*
import com.safeshade.ui.components.*
import com.safeshade.ui.theme.*


/**
 * Guardian/User screen for messaging between Guardian and Companion.
 *
 * @param bleManager BLE manager for sending messages
 * @param safetySettings Safety settings (for parental controls)
 * @param isGuardianMode Current mode (true = Guardian, false = Companion)
 * @param onModeChange Callback when mode changes
 * @param messageHistory List of messages exchanged
 * @param onMessageSent Callback when a message is sent
 * @param onReply Callback when a reply is sent
 */
@Composable
fun GuardianUserScreen(
    bleManager: BleManager,
    safetySettings: SafetySettings,
    isGuardianMode: Boolean,
    onModeChange: (Boolean) -> Unit,
    messageHistory: List<QuickMessage>,
    onMessageSent: (QuickMessage) -> Unit,
    onReply: (String, String) -> Unit
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
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with mode switch button
        GuardianScreenHeader(
            isGuardianMode = isGuardianMode,
            parentalControlsEnabled = safetySettings.parentalControlsEnabled,
            onModeSwitchClick = { showModeSelector = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Content based on mode
        if (isGuardianMode) {
            GuardianModeContent(bleManager, onMessageSent)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = if (isGuardianMode) "Guardian" else "Companion",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = if (isGuardianMode) "Send messages to your loved one"
                else "Stay connected with your guardian",
                fontSize = 14.sp,
                color = TextGray
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
 * Guardian mode content - for sending messages to device user.
 */
@Composable
fun GuardianModeContent(
    bleManager: BleManager,
    onMessageSent: (QuickMessage) -> Unit
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

    Spacer(modifier = Modifier.height(16.dp))

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

    Spacer(modifier = Modifier.height(16.dp))

    // Geofencing preview
    GeofencingPreviewCard()
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Quick Messages", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(16.dp))

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
                QuickMessageButton("On My Way", Color(0xFF00CEC9), isConnected, Modifier.weight(1f)) {
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Custom Message", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customMessage,
                onValueChange = onMessageChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Type your message...", color = TextGray) },
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            Text(
                "${customMessage.length}/60 characters",
                fontSize = 11.sp,
                color = TextGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

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

    Spacer(modifier = Modifier.height(16.dp))

    // Recent messages card
    RecentMessagesCard(messageHistory = messageHistory, onReply = onReply)

    Spacer(modifier = Modifier.height(16.dp))

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
                Text("Quick Replies", fontWeight = FontWeight.Bold, color = TextDark)
                Text("Send to Guardian", fontSize = 12.sp, color = TextGray)
            }
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("I'm OK", AccentGreen, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("REPLY:I'm OK!")
                }
                QuickMessageButton("Coming!", AccentOrange, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("REPLY:On my way!")
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickMessageButton("Need Help", AccentRed, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("REPLY:I need help!")
                }
                QuickMessageButton("Call You Soon", AccentBlue, isConnected, Modifier.weight(1f)) {
                    bleManager.sendGuardianMessage("REPLY:Will call soon")
                }
            }
        }
    }
}

/**
 * Recent messages from Guardian card.
 */
@Composable
private fun RecentMessagesCard(
    messageHistory: List<QuickMessage>,
    onReply: (String, String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Recent Messages from Guardian", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            val guardianMessages = messageHistory.filter { it.fromGuardian }.take(5)

            if (guardianMessages.isEmpty()) {
                Text(
                    "No messages yet",
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 20.dp),
                    textAlign = TextAlign.Center
                )
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
 * Single message item with reply functionality.
 */
@Composable
fun MessageItem(
    message: QuickMessage,
    onReply: (String) -> Unit
) {
    var showReplyOptions by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(message.text, color = TextDark, fontWeight = FontWeight.Medium)
                Text(
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(message.timestamp)),
                    fontSize = 11.sp,
                    color = TextGray
                )
                if (message.replied && message.replyText != null) {
                    Text(
                        "↪ ${message.replyText}",
                        fontSize = 12.sp,
                        color = AccentGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (!message.replied) {
                IconButton(onClick = { showReplyOptions = !showReplyOptions }) {
                    Icon(Icons.Rounded.Reply, contentDescription = "Reply", tint = AccentPurple)
                }
            }
        }

        // Reply options chips
        if (showReplyOptions) {
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
                        label = { Text(reply, fontSize = 12.sp) }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("My Status", fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) AccentGreen else AccentRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isConnected) "Device Connected" else "Device Offline",
                        color = TextDark
                    )
                }
                Text(
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    color = TextGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Geofencing preview card (coming soon feature).
 */
@Composable
fun GeofencingPreviewCard() {
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
                Text("Geofencing", fontWeight = FontWeight.Bold, color = TextDark)
                Box(
                    modifier = Modifier
                        .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "COMING SOON",
                        fontSize = 10.sp,
                        color = AccentOrange,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Set up safe zones and get alerts when the device leaves designated areas.",
                fontSize = 13.sp,
                color = TextGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Map,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Map Preview", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}
