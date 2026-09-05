package com.safeshade.ui.screens.circle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.MessageChannel
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * A summary row on the hub, in the shape a `Way` needs.
 *
 * The four banks below — zones, journey, check-in, SMS — are all the same
 * question with different subjects, so they share one presentation type rather
 * than growing four near-identical triples of fields on the state.
 */
data class CircleWay(
    val state: LampState = LampState.UNKNOWN,
    val stateLabel: String = "Not known",
    val detail: String? = null
)

/** The last few messages, as the hub previews them. */
data class CircleMessagePreview(
    val id: String,
    val text: String,
    val fromGuardian: Boolean,
    val timeLabel: String,
    val channel: MessageChannel = MessageChannel.BLE
)

/** Everything the Circle hub draws. */
data class CircleUiState(
    val role: UserRole = UserRole.GUARDIAN,
    /** Who wears the device. Used in guardian copy: "Baba". */
    val wearerName: String = "",
    /** Who watches over the wearer. Used in companion copy: "Priya". */
    val guardianName: String = "",

    /** Whether the other end can be reached at all right now. */
    val linkState: LampState = LampState.UNKNOWN,
    val linkLabel: String = "Not connected",
    /** One line under the name: what is known, and how recently. */
    val subline: String = "",

    val recentMessages: List<CircleMessagePreview> = emptyList(),
    val unreadCount: Int = 0,
    val quickMessages: List<String> = emptyList(),
    val isSendingQuickMessage: Boolean = false,
    /** How a quick message would travel if sent right now. */
    val outboundChannel: MessageChannel = MessageChannel.BLE,

    val placeLabel: String? = null,
    val lastSeenLabel: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val locationState: LampState = LampState.UNKNOWN,
    val isRefreshingLocation: Boolean = false,

    val zones: CircleWay = CircleWay(),
    val journey: CircleWay = CircleWay(),
    val checkIn: CircleWay = CircleWay(),
    val sms: CircleWay = CircleWay()
)

/**
 * The circle: the people on either end of the link.
 *
 * The same routes and the same structure serve both roles from opposite ends.
 * A guardian opens this screen to find out about the person they are
 * responsible for; a wearer opens it to find the person who is responsible for
 * them. Nothing here may say "you are wearing this" — a daughter reading about
 * her father should never be addressed as though she were him, and that single
 * slip is what makes an app of this kind feel written for somebody else.
 *
 * Order is by urgency of the question, not by feature size: who is this and
 * are they reachable, then what have they said, then where are they, then the
 * arrangements standing in the background.
 */
@Composable
fun CircleScreen(
    state: CircleUiState,
    onOpenThread: () -> Unit,
    onSendQuickMessage: (String) -> Unit,
    onRefreshLocation: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenSim: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    // Two names, not one. `other` is grammatical mid-sentence ("messages from
    // your guardian"); `headlineName` is what belongs in headline type, where a
    // lowercase sentence fragment reads as somebody's actual name. Deriving one
    // from the other is what produced "Messages from No guardian set yet".
    val other = counterpartName(state.role, state.wearerName, state.guardianName)
    val headlineName = counterpartHeadline(state.role, state.wearerName, state.guardianName)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            Text(
                text = when (state.role) {
                    UserRole.GUARDIAN -> "Circle"
                    UserRole.COMPANION -> "Your guardian"
                },
                style = MaterialTheme.typography.titleLarge,
                color = colors.ink,
                modifier = Modifier
                    .padding(vertical = Spacing.sm)
                    .semantics { heading() }
            )
        }

        item("person") {
            PersonPlate(state = state, name = headlineName)
        }

        // ---- Messages
        item("messages-heading") {
            SectionPlate(
                title = if (state.unreadCount > 0) "Messages · ${state.unreadCount} new" else "Messages"
            )
        }

        item("messages") {
            if (state.recentMessages.isEmpty()) {
                EmptyBay(
                    message = when (state.role) {
                        UserRole.GUARDIAN ->
                            "Nothing sent yet. A short message appears on the device screen and buzzes once."
                        UserRole.COMPANION ->
                            "Nothing yet. Messages from $other appear here and on the device."
                    },
                    actionLabel = "Open messages",
                    onAction = onOpenThread
                )
            } else {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    state.recentMessages.forEachIndexed { index, message ->
                        if (index > 0) Hairline()
                        MessagePreviewRow(
                            message = message,
                            role = state.role,
                            wearerName = state.wearerName,
                            guardianName = state.guardianName,
                            onClick = onOpenThread
                        )
                    }
                }
            }
        }

        if (state.quickMessages.isNotEmpty()) {
            item("quick") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    // One plate of rows rather than a 2x2 grid of identical
                    // outlined buttons. Four same-sized boxes give every option
                    // equal weight and none of them any shape, so the eye has
                    // nothing to land on; a stacked list is read top to bottom
                    // the way a bank of ways already is elsewhere in the app.
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        state.quickMessages.forEachIndexed { index, message ->
                            if (index > 0) Hairline()
                            QuickMessageRow(
                                text = message,
                                enabled = !state.isSendingQuickMessage,
                                onClick = { onSendQuickMessage(message) }
                            )
                        }
                    }
                    Text(
                        text = if (state.outboundChannel == MessageChannel.SMS) {
                            "The device is out of Bluetooth range, so these go by SMS. Your carrier may charge for them."
                        } else {
                            "These go over Bluetooth and appear on the device straight away."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }

        item("open-thread") {
            BoardButton(
                label = "Write a message",
                onClick = onOpenThread,
                weight = ButtonWeight.QUIET,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---- Where
        item("place-heading") {
            SectionPlate(
                title = "Where",
                trailing = {
                    IconButton(onClick = onRefreshLocation, enabled = !state.isRefreshingLocation) {
                        Icon(
                            Icons.Outlined.MyLocation,
                            contentDescription = "Ask for a fresh location",
                            tint = if (state.isRefreshingLocation) colors.inkFaint else colors.inkMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        item("place") {
            PlacePlate(state = state, name = other)
        }

        // ---- The standing arrangements
        item("arrangements-heading") {
            SectionPlate(title = "Arrangements")
        }

        item("arrangements") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Safe zones",
                    state = state.zones.state,
                    stateLabel = state.zones.stateLabel,
                    detail = state.zones.detail,
                    icon = Icons.Outlined.Place,
                    onClick = onOpenZones
                )
                Hairline()
                Way(
                    name = "Walk with me",
                    state = state.journey.state,
                    stateLabel = state.journey.stateLabel,
                    detail = state.journey.detail,
                    icon = Icons.Outlined.DirectionsWalk,
                    onClick = onOpenJourney
                )
                Hairline()
                Way(
                    name = "Check in",
                    state = state.checkIn.state,
                    stateLabel = state.checkIn.stateLabel,
                    detail = state.checkIn.detail,
                    icon = Icons.Outlined.HelpOutline,
                    onClick = onOpenCheckIn
                )
                Hairline()
                Way(
                    name = "SIM and SMS",
                    state = state.sms.state,
                    stateLabel = state.sms.stateLabel,
                    detail = state.sms.detail,
                    icon = Icons.Outlined.SimCard,
                    onClick = onOpenSim
                )
            }
        }
    }
}

/**
 * Who this screen is about, and whether they can be reached.
 *
 * The headline is a person's name, not a device name. On the Board the subject
 * is the wearable; here it is deliberately the human being at the other end,
 * because that is the difference between the two tabs.
 */
@Composable
private fun PersonPlate(state: CircleUiState, name: String) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.lg)
        ) {
            PilotLamp(state = state.linkState, size = 22.dp, description = state.linkLabel)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink
                )
                Text(
                    text = state.subline.ifBlank { defaultSubline(state) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            // The state word beside the lamp, never the lamp on its own.
            Nameplate(state.linkLabel, small = true, muted = true)
        }
    }
}

/** The relationship, said plainly, when nothing more specific is known. */
private fun defaultSubline(state: CircleUiState): String = when (state.role) {
    UserRole.GUARDIAN -> "The person you are watching over"
    UserRole.COMPANION -> "The person watching over you"
}

/** Last known place, how old it is, and the coordinates behind it. */
@Composable
private fun PlacePlate(state: CircleUiState, name: String) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(
                    state = state.locationState,
                    description = when (state.locationState) {
                        LampState.LIVE -> "Recent"
                        LampState.ATTENTION -> "Getting old"
                        LampState.TRIP -> "Stale"
                        else -> "No location yet"
                    }
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = state.placeLabel ?: "No place known yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            Text(
                // "Last seen" is about the phone's own fix, not a satellite
                // lock on the wearable, and the copy must not imply otherwise.
                text = state.lastSeenLabel ?: when (state.role) {
                    UserRole.GUARDIAN -> "No location has reached this phone yet."
                    UserRole.COMPANION -> "This phone has not recorded a location yet."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )

            if (state.lat != null && state.lon != null) {
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                    Readout(label = "Latitude", value = "%.5f".format(state.lat))
                    Readout(label = "Longitude", value = "%.5f".format(state.lon))
                }
            }

            if (state.role == UserRole.GUARDIAN) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = "$name is told nothing when you look at this.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

/**
 * One message, small.
 *
 * Not a `Way`: a way's trailing slot is a state word and a lamp, and a message
 * has neither. It borrows two things from `Way` all the same — a 48dp floor
 * set with `defaultMinSize` rather than a fixed height, so a long message can
 * grow at a raised font scale, and one merged spoken node, because "You,
 * 18:44, Bluetooth, coming to get you in ten minutes" read as four fragments
 * is four times the work for the same line.
 *
 * Tapping any row opens the thread rather than the message. There is nothing
 * useful to do with a single line of a conversation, and a row that looks
 * individually actionable but is not is worse than one that plainly is not.
 */
@Composable
private fun MessagePreviewRow(
    message: CircleMessagePreview,
    role: UserRole,
    wearerName: String,
    guardianName: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.board
    val author = when {
        message.fromGuardian && role == UserRole.GUARDIAN -> "You"
        message.fromGuardian -> guardianName.ifBlank { "Your guardian" }
        role == UserRole.COMPANION -> "You"
        else -> wearerName.ifBlank { "The wearer" }
    }
    val spoken = buildString {
        append(author)
        append(", ")
        append(message.text)
        append(", ")
        append(message.timeLabel)
        if (message.channel == MessageChannel.SMS) append(", by SMS")
        append(". Opens the whole conversation.")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate(author, small = true, muted = true)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = message.timeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.weight(1f)
                )
                ChannelBadge(message.channel)
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink
            )
        }
    }
}

private val sampleMessages = listOf(
    CircleMessagePreview("1", "Coming to get you in ten minutes", true, "18:44", MessageChannel.SMS),
    CircleMessagePreview("2", "I am at the park bench", false, "18:20"),
    CircleMessagePreview("3", "Time for your evening tablets", true, "18:02")
)

@Preview(name = "Circle — guardian, light", showBackground = true, heightDp = 1400)
@Composable
private fun CircleGuardianLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        CircleScreen(
            state = CircleUiState(
                role = UserRole.GUARDIAN,
                wearerName = "Baba",
                guardianName = "Priya",
                linkState = LampState.LIVE,
                linkLabel = "Reachable",
                subline = "Wearing the cane sensor · last heard from 2 minutes ago",
                recentMessages = sampleMessages,
                unreadCount = 1,
                quickMessages = listOf("Where are you?", "Call me", "Coming to get you", "Are you OK?"),
                placeLabel = "Near Salt Lake Sector V",
                lastSeenLabel = "Location taken 4 minutes ago by this phone",
                lat = 22.5726,
                lon = 88.3639,
                locationState = LampState.LIVE,
                zones = CircleWay(LampState.LIVE, "Inside Home", "3 zones · told when leaving"),
                journey = CircleWay(LampState.OFF, "None", "No journey running"),
                checkIn = CircleWay(LampState.OFF, "None open", "Last answered today at 14:10"),
                sms = CircleWay(LampState.LIVE, "Ready", "2 numbers allowed")
            ),
            onOpenThread = {},
            onSendQuickMessage = {},
            onRefreshLocation = {},
            onOpenZones = {},
            onOpenJourney = {},
            onOpenCheckIn = {},
            onOpenSim = {}
        )
    }
}

@Preview(name = "Circle — wearer, dark", showBackground = true, heightDp = 1400)
@Composable
private fun CircleCompanionDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        CircleScreen(
            state = CircleUiState(
                role = UserRole.COMPANION,
                wearerName = "Baba",
                guardianName = "Priya",
                linkState = LampState.ATTENTION,
                linkLabel = "By SMS only",
                subline = "Bluetooth is out of range, so messages go by SMS",
                recentMessages = sampleMessages.take(2),
                quickMessages = listOf("I am OK", "On my way", "Call me", "Reached safely"),
                outboundChannel = MessageChannel.SMS,
                placeLabel = "Park Street",
                lastSeenLabel = "Location taken 26 minutes ago",
                locationState = LampState.ATTENTION,
                zones = CircleWay(LampState.ATTENTION, "Outside Home", "3 zones · told when leaving"),
                journey = CircleWay(LampState.LIVE, "Running", "To the chemist · 12 minutes left"),
                checkIn = CircleWay(LampState.ATTENTION, "Waiting", "Priya asked at 18:02"),
                sms = CircleWay(LampState.ATTENTION, "Anyone", "No allowlist, every sender accepted")
            ),
            onOpenThread = {},
            onSendQuickMessage = {},
            onRefreshLocation = {},
            onOpenZones = {},
            onOpenJourney = {},
            onOpenCheckIn = {},
            onOpenSim = {}
        )
    }
}

/**
 * One tappable quick message.
 *
 * Deliberately not a `Way`: a way describes a circuit's state, and this is an
 * action. It borrows the row geometry so the screen still reads as one system,
 * but carries a send glyph instead of a lamp.
 */
@Composable
private fun QuickMessageRow(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) colors.ink else colors.inkFaint,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Outlined.Send,
            contentDescription = null,
            tint = if (enabled) colors.inkMuted else colors.inkFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}
