package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.safeshade.data.MessageChannel
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** One message in the thread, as the screen needs to render it. */
data class ThreadMessage(
    val id: String,
    val text: String,
    /** Who wrote it, not who is reading it. Alignment is derived from both. */
    val fromGuardian: Boolean,
    val timeLabel: String,
    val channel: MessageChannel = MessageChannel.BLE,
    /** A reply the wearer sent back to this specific message, if any. */
    val replyText: String? = null,
    /** False while a send is still in flight or after it failed outright. */
    val delivered: Boolean = true
)

/** Everything the thread draws. */
data class MessagesUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val guardianName: String = "",
    /** Oldest first. The list is drawn in this order and scrolls to the end. */
    val messages: List<ThreadMessage> = emptyList(),
    val quickReplies: List<String> = emptyList(),
    val draft: String = "",
    /**
     * The character budget for one message.
     *
     * 60 rather than the transport's own limit: this is what fits on the
     * wearable's small display in one go, and a message the device has to
     * truncate is a message the wearer half-reads.
     */
    val maxChars: Int = 60,
    /** How the next message will travel, given the link right now. */
    val outboundChannel: MessageChannel = MessageChannel.BLE,
    /** False when the wearable is out of Bluetooth range. */
    val deviceInRange: Boolean = true,
    /** True once the wearable's SIM number is known, so SMS is possible. */
    val smsConfigured: Boolean = false,
    val isSending: Boolean = false,
    val errorText: String? = null
)

/**
 * The thread.
 *
 * The one thing this screen must never do is let a message leave by SMS
 * without saying so. Bluetooth is free and silent; an SMS costs the guardian
 * money on every send and arrives on a wearable that may be in a different
 * city. So the channel is stated twice — once ahead of the send, under the
 * composer, and once after it, on the message itself — because those answer
 * two different questions ("what will this cost me" and "why did that one
 * cost me").
 */
@Composable
fun MessagesScreen(
    state: MessagesUiState,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendQuick: (String) -> Unit,
    onOpenSim: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val other = counterpartName(state.role, state.wearerName, state.guardianName)
    val overBudget = state.draft.length > state.maxChars
    val canSend = state.draft.isNotBlank() && !overBudget && !state.isSending

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            // The theme draws edge to edge, so the window does not resize
            // when the keyboard opens: without this inset the composer goes
            // behind it, hiding the very field being typed into.
            .imePadding()
    ) {
        CircleTopRow(
            title = "Messages",
            subtitle = "With $other",
            onBack = onBack,
            backDescription = "Go back to the circle"
        )

        // A thread that opens at its oldest message is not a thread. The list
        // position is UI-ephemeral in exactly the way the delete confirmation
        // on the zone list is — nothing outside this screen wants it — so it
        // stays here rather than in the state. scrollToItem, not the animated
        // form: arriving on the screen should not look like a fly-past.
        val listState = rememberLazyListState()
        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.sm,
                bottom = Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (state.messages.isEmpty()) {
                item("empty") {
                    EmptyBay(
                        message = "No messages yet. A short message appears on the device screen and buzzes once.",
                        modifier = Modifier.padding(top = Spacing.xl)
                    )
                }
            }

            items(state.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    role = state.role,
                    wearerName = state.wearerName,
                    guardianName = state.guardianName
                )
            }
        }

        Composer(
            state = state,
            canSend = canSend,
            overBudget = overBudget,
            onDraftChange = onDraftChange,
            onSend = onSend,
            onSendQuick = onSendQuick,
            onOpenSim = onOpenSim
        )
    }
}

/**
 * One message.
 *
 * Two things separate the two sides, and neither of them is colour: the bubble
 * is aligned to one edge or the other, and it is either a raised plate or a
 * recessed one. That tonal step is the same one the board uses everywhere for
 * "on the surface" versus "set into it", so it needs no explaining, and it
 * survives greyscale.
 */
@Composable
private fun MessageBubble(
    message: ThreadMessage,
    role: UserRole,
    wearerName: String,
    guardianName: String
) {
    val colors = MaterialTheme.board

    // "Mine" is a question about this phone, not about the message. The same
    // message is on the right for the guardian who sent it and on the left for
    // the wearer who received it.
    val isMine = (role == UserRole.GUARDIAN) == message.fromGuardian
    val author = when {
        message.fromGuardian && role == UserRole.GUARDIAN -> "You"
        message.fromGuardian -> guardianName.ifBlank { "Your guardian" }
        role == UserRole.COMPANION -> "You"
        else -> wearerName.ifBlank { "The wearer" }
    }

    val smsNote = when {
        message.channel != MessageChannel.SMS -> null
        isMine -> "Sent by SMS because the device was out of Bluetooth range. Your carrier may charge for this."
        else -> "Arrived by SMS. The device was out of Bluetooth range."
    }

    // The whole bubble is announced as one utterance. Read as separate nodes
    // it becomes "You. Time for your tablets. 9:04. SMS." — four fragments for
    // what a sighted reader takes in as one line.
    val spoken = buildString {
        append(author)
        append(", ")
        append(message.text)
        append(", ")
        append(message.timeLabel)
        if (message.channel == MessageChannel.SMS) append(", sent by SMS")
        if (!message.delivered) append(", not delivered yet")
        if (message.replyText != null) {
            append(". Replied: ")
            append(message.replyText)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        BoardPlate(
            recessed = !isMine,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Nameplate(author, small = true, muted = true)
                    Spacer(Modifier.weight(1f))
                    ChannelBadge(message.channel)
                }
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (message.delivered) message.timeLabel else "${message.timeLabel} · not delivered yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
                if (smsNote != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = smsNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
                if (message.replyText != null) {
                    Spacer(Modifier.height(Spacing.md))
                    Hairline()
                    Spacer(Modifier.height(Spacing.md))
                    Nameplate("Replied", small = true, muted = true)
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = message.replyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.ink
                    )
                }
            }
        }
    }
}

/** Quick replies, the free-text field, and the one honest sentence about cost. */
@Composable
private fun Composer(
    state: MessagesUiState,
    canSend: Boolean,
    overBudget: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
    onSendQuick: (String) -> Unit,
    onOpenSim: () -> Unit
) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            if (state.quickReplies.isNotEmpty()) {
                SectionPlate(title = "Quick messages")
                ChoiceGrid(items = state.quickReplies) { reply, cell ->
                    BoardButton(
                        label = reply,
                        onClick = { onSendQuick(reply) },
                        enabled = !state.isSending,
                        weight = ButtonWeight.SECONDARY,
                        modifier = cell
                    )
                }
            }

            BoardField(
                value = state.draft,
                onValueChange = onDraftChange,
                label = "Your own message",
                placeholder = "Type something short",
                supporting = if (overBudget) {
                    "The device shows ${state.maxChars} characters. Shorten this and it will arrive whole."
                } else {
                    channelSentence(state)
                },
                enabled = !state.isSending,
                trailing = {
                    Text(
                        text = "${state.draft.length}/${state.maxChars}",
                        style = MaterialTheme.boardType.readout,
                        color = colors.inkFaint
                    )
                }
            )

            if (state.errorText != null) {
                Text(
                    text = state.errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkAttention
                )
            }

            BoardButton(
                label = if (state.isSending) "Sending" else "Send",
                onClick = onSend,
                enabled = canSend,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )

            // Offered only when it is the actual blocker. A "set up SMS" link
            // sitting under every message would read as an upsell.
            if (!state.deviceInRange && !state.smsConfigured) {
                BoardButton(
                    label = "Set up SMS",
                    supporting = "The device is out of range and has no number saved, so nothing can be sent",
                    onClick = onOpenSim,
                    weight = ButtonWeight.QUIET,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** What will happen when Send is pressed, said before it is pressed. */
private fun channelSentence(state: MessagesUiState): String = when {
    state.outboundChannel == MessageChannel.BLE ->
        "Will go over Bluetooth. Free, and it appears on the device straight away."
    state.smsConfigured ->
        "The device is out of Bluetooth range, so this will go by SMS. Your carrier may charge for it."
    else ->
        "The device is out of Bluetooth range and has no SIM number saved, so this cannot be sent yet."
}

private val sampleThread = listOf(
    ThreadMessage(
        id = "1",
        text = "Time for your evening tablets",
        fromGuardian = true,
        timeLabel = "18:02",
        channel = MessageChannel.BLE,
        replyText = "Taken"
    ),
    ThreadMessage(
        id = "2",
        text = "I am at the park bench",
        fromGuardian = false,
        timeLabel = "18:20",
        channel = MessageChannel.BLE
    ),
    ThreadMessage(
        id = "3",
        text = "Coming to get you in ten minutes",
        fromGuardian = true,
        timeLabel = "18:44",
        channel = MessageChannel.SMS
    )
)

@Preview(name = "Messages — guardian, light", showBackground = true, heightDp = 900)
@Composable
private fun MessagesGuardianLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        MessagesScreen(
            state = MessagesUiState(
                role = UserRole.GUARDIAN,
                wearerName = "Baba",
                guardianName = "Priya",
                messages = sampleThread,
                quickReplies = listOf("Where are you?", "Call me", "Coming to get you", "Are you OK?"),
                draft = "On my way now",
                outboundChannel = MessageChannel.SMS,
                deviceInRange = false,
                smsConfigured = true
            ),
            onDraftChange = {},
            onSend = {},
            onSendQuick = {},
            onOpenSim = {},
            onBack = {}
        )
    }
}

@Preview(name = "Messages — companion, dark", showBackground = true, heightDp = 900)
@Composable
private fun MessagesCompanionDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        MessagesScreen(
            state = MessagesUiState(
                role = UserRole.COMPANION,
                wearerName = "Baba",
                guardianName = "Priya",
                messages = sampleThread,
                quickReplies = listOf("I am OK", "On my way", "Call me", "Reached safely"),
                draft = "",
                outboundChannel = MessageChannel.BLE,
                deviceInRange = true
            ),
            onDraftChange = {},
            onSend = {},
            onSendQuick = {},
            onOpenSim = {},
            onBack = {}
        )
    }
}
