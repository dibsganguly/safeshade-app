package com.safeshade.ui.screens.circle

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.MessageChannel
import com.safeshade.data.UserRole
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import com.safeshade.ui.board.Avatar
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.rowClickable
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

/**
 * One person a Guardian looks after, as the family dashboard draws them.
 *
 * Every field is a fact the phone has or a dash. Battery is the wearable's
 * own report and exists only for the connected one; a place is the last fix
 * that reached this phone; the last alert is the newest trip logged for this
 * person, or for the household when no trip has been stamped with anyone.
 */
data class WearerCard(
    val id: String,
    val name: String,
    val avatarId: String,
    val modeLabel: String,
    val linkState: LampState,
    val linkLabel: String,
    /** "82 %" or null for a dash. */
    val batteryLabel: String? = null,
    /** "Home · 4 min ago" or null for a dash. */
    val placeLabel: String? = null,
    /** "Fall · yesterday, dismissed" or null for a dash. */
    val lastAlertLabel: String? = null,
    /** False when no SIM number is stored for their wearable; the Call action then says so. */
    val canCall: Boolean = false
)

/** Everything the Circle hub draws. */
data class CircleUiState(
    /** A Guardian's people, one plate each. Empty for a Companion. */
    val wearers: List<WearerCard> = emptyList(),
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
    onSendQuickMessage: suspend (String) -> Boolean,
    onRefreshLocation: () -> Unit,
    onOpenZones: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenCheckIn: () -> Unit,
    onOpenSim: () -> Unit,
    onOpenPeople: () -> Unit = {},
    onOpenPerson: (id: String) -> Unit = {},
    onLocate: (id: String) -> Unit = {},
    onCallWearable: (id: String) -> Unit = {},
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /**
     * Hoisted above the NavHost by SafeShadeApp so scroll position
     * survives a tab switch. Defaulted so the previews still compile
     * without one.
     */
    listState: LazyListState = rememberLazyListState()
) {
    val colors = MaterialTheme.board
    // Two names, not one. `other` is grammatical mid-sentence ("messages from
    // your guardian"); `headlineName` is what belongs in headline type, where a
    // lowercase sentence fragment reads as somebody's actual name. Deriving one
    // from the other is what produced "Messages from No guardian set yet".
    val other = counterpartName(state.role, state.wearerName, state.guardianName)
    val headlineName = counterpartHeadline(state.role, state.wearerName, state.guardianName)

    // No single spacedBy gap for the whole list any more. A flat 16dp between
    // every item put a section heading as far from the card it labels as that
    // card was from the next heading, which is what made the screen read as
    // one undifferentiated stack rather than a set of grouped sections. Each
    // item below carries its own top gap instead: tight (sm) under a heading
    // it labels, looser (md) between blocks still inside the same section, and
    // loosest (xl) where one section ends and the next begins.
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        )
    ) {
        item("title") {
            // A tab root, so it takes the ROOT tier — the same weight the
            // Board and Device roots carry. This screen was `titleLarge`, two
            // steps below its peers, which was visible the moment you moved
            // between tabs.
            ScreenHeader(
                title = when (state.role) {
                    UserRole.GUARDIAN -> "Circle"
                    UserRole.COMPANION -> "Your guardian"
                },
                // No subtitle. It listed the three section headings that
                // begin immediately below it, which is a table of contents for
                // a screen you can already see all of.
                tier = ScreenTier.ROOT
            )
        }

        if (state.role == UserRole.GUARDIAN && state.wearers.isNotEmpty()) {
            // The family dashboard: one plate per person, the link lamp on
            // each, the three readouts a guardian actually checks, and the
            // three things they do next. The single-person plate below is
            // what a Companion sees of their guardian.
            item("people-heading") {
                SectionPlate(
                    title = "People I look after",
                    modifier = Modifier.padding(top = Spacing.lg),
                    trailing = {
                        TextButton(onClick = onOpenPeople) {
                            Text("Manage", style = MaterialTheme.typography.bodyMedium, color = colors.inkAttention)
                        }
                    }
                )
            }
            items(state.wearers, key = { "wearer-" + it.id }) { card ->
                WearerPlate(
                    card = card,
                    onOpen = { onOpenPerson(card.id) },
                    onMessage = onOpenThread,
                    onLocate = { onLocate(card.id) },
                    onCall = { onCallWearable(card.id) },
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            }
        } else {
            item("person") {
                PersonPlate(state = state, name = headlineName, modifier = Modifier.padding(top = Spacing.lg))
            }
        }

        // ---- Messages
        item("messages-heading") {
            SectionPlate(
                title = if (state.unreadCount > 0) "Messages · ${state.unreadCount} new" else "Messages",
                modifier = Modifier.padding(top = Spacing.xl)
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
                    onAction = onOpenThread,
                    modifier = Modifier.padding(top = Spacing.sm)
                )
            } else {
                BoardPlate(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    // Still the Messages section, so a medium gap rather than
                    // the tight one a heading gets or the loose one between
                    // sections.
                    modifier = Modifier.padding(top = Spacing.md)
                ) {
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
                                onSend = { onSendQuickMessage(message) }
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
                // Same section as the plate above it, not a new one.
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.md)
            )
        }

        // ---- Where
        item("place-heading") {
            SectionPlate(
                title = "Where",
                modifier = Modifier.padding(top = Spacing.xl),
                trailing = { RefreshLocationButton(state.isRefreshingLocation, onRefreshLocation) }
            )
        }

        item("place") {
            PlacePlate(state = state, name = other, modifier = Modifier.padding(top = Spacing.sm))
        }

        // ---- The standing arrangements
        item("arrangements-heading") {
            SectionPlate(title = "Arrangements", modifier = Modifier.padding(top = Spacing.xl))
        }

        item("arrangements") {
            BoardPlate(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                Way(
                    name = "Safe zones",
                    state = state.zones.state,
                    stateLabel = state.zones.stateLabel,
                    detail = state.zones.detail,
                    icon = SafeShadeIcons.SafeZone,
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
                    icon = SafeShadeIcons.HelpCircle,
                    onClick = onOpenCheckIn
                )
                Hairline()
                Way(
                    name = "SIM and SMS",
                    state = state.sms.state,
                    stateLabel = state.sms.stateLabel,
                    detail = state.sms.detail,
                    icon = SafeShadeIcons.SimAndSms,
                    onClick = onOpenSim
                )
            }
        }
    }
}

/**
 * Who this screen is about, and whether they can be reached.
 *
 * The hero of the screen, built to the same anatomy as `MainsPlate` — the
 * strongest card in the app — because the question this card answers
 * ("who, and can I reach them") is the Circle tab's equivalent of "is the
 * device on". Headline, one subline, a rule, then a readout strip for the
 * live facts: link state, last seen, and how many messages are waiting.
 *
 * The headline is a person's name, not a device name. On the Board the subject
 * is the wearable; here it is deliberately the human being at the other end,
 * because that is the difference between the two tabs.
 *
 * The lamp used to lead the row, level with and competing against the name for
 * the same line. It now sits with the state word in its own column at the top
 * right, the way a panel's pilot light sits apart from the circuit label it
 * lights for. That column is laid out in the row rather than overlaid on the
 * card corner — a state word is real text, not a fixed glyph,
 * and it has to be free to grow at a raised font scale without drifting over
 * the name beside it.
 */
@Composable
private fun PersonPlate(state: CircleUiState, name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = state.subline.ifBlank { defaultSubline(state) },
                    style = MaterialTheme.boardType.rowDetail,
                    color = colors.inkMuted
                )
                LampWord(state.linkLabel)
            }
            Spacer(Modifier.width(Spacing.md))
            // The lamp alone in the corner. Its word has moved to the foot of
            // the block on the left - see [LampWord].
            PilotLamp(state = state.linkState, size = 22.dp, description = state.linkLabel)
        }

        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            // Two readouts, not three, and there is no "Link" one.
            //
            // The link state is already the lamp and the word in the corner
            // above, so a third readout repeating it said the same thing twice
            // on one card - the same redundancy that got the sync time deleted
            // from the Board. Removing it also fixed a real layout fault:
            // three columns of monospace readout across a phone left the last
            // one about 300dp wide, and "None waiting" wrapped onto a second
            // line and collided with the column beside it. Measured on a
            // device, not in a preview.
            Readout(
                label = "Last seen",
                value = state.lastSeenLabel ?: "Not yet",
                // The only readout in the app whose value is a sentence.
                compact = true,
                modifier = Modifier.weight(1f)
            )
            Readout(
                label = "Messages",
                // "None", not "None waiting". Two words of monospace in half a
                // card is what wrapped; the label above it already supplies the
                // noun.
                value = if (state.unreadCount > 0) "${state.unreadCount} new" else "None",
                state = if (state.unreadCount > 0) LampState.ATTENTION else null,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * One person on the family dashboard.
 *
 * The same anatomy as the person plate: identity at the top left, the lamp
 * at the top right, a rule, a readout strip, a rule, then actions. The face
 * is what tells two plates apart at a glance; the lamp is what tells a
 * guardian which one to look at first.
 */
@Composable
private fun WearerPlate(
    card: WearerCard,
    onOpen: () -> Unit,
    onMessage: () -> Unit,
    onLocate: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .plateClickable(role = Role.Button, onClick = onOpen)
                .padding(Spacing.lg)
        ) {
            Avatar(avatarId = card.avatarId, name = card.name, size = 52.dp)
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name.ifBlank { "Unnamed" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(text = card.modeLabel, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted)
                LampWord(card.linkLabel)
            }
            Spacer(Modifier.width(Spacing.md))
            PilotLamp(state = card.linkState, size = 22.dp, description = card.linkLabel)
        }
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            Readout(label = "Battery", value = card.batteryLabel ?: "\u2014", modifier = Modifier.weight(1f))
            Readout(label = "Last seen", value = card.placeLabel ?: "\u2014", compact = true, modifier = Modifier.weight(1.4f))
            Readout(
                label = "Last alert",
                value = card.lastAlertLabel ?: "\u2014",
                compact = true,
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1.2f)
            )
        }
        Hairline()
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            BoardButton(label = "Message", onClick = onMessage, weight = ButtonWeight.QUIET, modifier = Modifier.weight(1f))
            BoardButton(label = "Where", onClick = onLocate, weight = ButtonWeight.QUIET, modifier = Modifier.weight(1f))
            BoardButton(
                label = "Call",
                onClick = onCall,
                weight = ButtonWeight.QUIET,
                enabled = card.canCall,
                modifier = Modifier.weight(1f)
            )
        }
        if (!card.canCall) {
            Text(
                text = "Call needs the wearable's SIM number, stored under SIM and SMS.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.md)
            )
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
private fun PlacePlate(state: CircleUiState, name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    // "Last seen" is about the phone's own fix, not a satellite lock on the
    // wearable, and both the state word and the fallback copy below must not
    // imply otherwise.
    val freshnessWord = when (state.locationState) {
        LampState.LIVE -> "Recent"
        LampState.ATTENTION -> "Getting old"
        LampState.TRIP -> "Stale"
        else -> "No fix yet"
    }

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.placeLabel ?: "No place known yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.ink
                )
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = state.lastSeenLabel ?: when (state.role) {
                        UserRole.GUARDIAN -> "No location has reached this phone yet."
                        UserRole.COMPANION -> "This phone has not recorded a location yet."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
                LampWord(freshnessWord)
            }
            Spacer(Modifier.width(Spacing.md))
            PilotLamp(state = state.locationState, description = freshnessWord)
        }

        if (state.lat != null && state.lon != null) {
            Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Readout(label = "Latitude", value = "%.5f".format(state.lat), modifier = Modifier.weight(1f))
                Readout(
                    label = "Longitude",
                    value = "%.5f".format(state.lon),
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (state.role == UserRole.GUARDIAN) {
            // The header block above always closes with its own bottom
            // Spacing.lg, lat/lon row or not, so this needs no top gap of its
            // own to avoid stacking two margins into one oversized one.
            Text(
                // The name goes in the middle, not at the front. It is not
                // always a name: with no wearer set it is the fallback noun
                // "the wearer", and a sentence opening with a lowercase word
                // reads as a typo. The old wording had the same fault and it
                // was on screen for two releases.
                text = "Checking this does not notify $name.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg)
            )
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
            .rowClickable(role = Role.Button, onClick = onClick)
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

/**
 * The word under a card's pilot lamp, moved out from under it.
 *
 * It used to sit stacked beneath the lamp in the top-right corner, which put a
 * second, competing text column on a card that already had one and left the
 * corner top-heavy. The word is still here - a colour with no word beside it is
 * not something this system does - but it has moved to the foot of the block it
 * describes and dropped to the faintest ink, so the corner is just the lamp and
 * the card reads down one column instead of two.
 */
@Composable
private fun LampWord(word: String) {
    Spacer(Modifier.height(Spacing.sm))
    Text(
        text = word,
        style = MaterialTheme.boardType.nameplateSmall,
        // Faint, not muted. This is a caption on something that already says
        // the same thing in colour a few millimetres away.
        color = MaterialTheme.board.inkFaint
    )
}

/**
 * Ask for a fresh location.
 *
 * The glyph nods when pressed. Nothing else acknowledges the tap - a fix can
 * take several seconds and may never arrive at all - so without it the control
 * read as dead on the one screen where being told nothing is the complaint.
 *
 * A nod rather than a spin: a spinner promises progress the app is not
 * tracking. This says "asked", not "working".
 */
@Composable
private fun RefreshLocationButton(refreshing: Boolean, onRefresh: () -> Unit) {
    val colors = MaterialTheme.board
    var nods by remember { mutableIntStateOf(0) }
    val tilt = remember { Animatable(0f) }

    LaunchedEffect(nods) {
        if (nods == 0) return@LaunchedEffect
        tilt.animateTo(16f, tween(110, easing = Motion.Standard))
        tilt.animateTo(-8f, tween(150, easing = Motion.Standard))
        tilt.animateTo(0f, tween(220, easing = Motion.ThrowEasing))
    }

    IconButton(
        onClick = {
            nods++
            onRefresh()
        },
        enabled = !refreshing
    ) {
        Icon(
            SafeShadeIcons.Gps,
            contentDescription = "Ask for a fresh location",
            // Asked for in red. `inkTrip` rather than the trip lamp glass, for
            // the same reason the back chevron takes `inkAttention`: the raw
            // glass is a fill colour and does not carry a 20dp glyph on the
            // bone panel. Worth knowing that this is the one place in the app
            // where a state hue sits on something that is not reporting a
            // state.
            tint = if (refreshing) colors.inkFaint else colors.inkTrip,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = tilt.value }
        )
    }
}

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
            onSendQuickMessage = { true },
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
            onSendQuickMessage = { true },
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
 * but carries an outbound-message glyph instead of a lamp.
 */
@Composable
private fun QuickMessageRow(
    text: String,
    onSend: suspend () -> Boolean
) {
    val colors = MaterialTheme.board
    val scope = rememberCoroutineScope()

    // In-flight and acknowledged, both held by the row itself.
    //
    // The screen carried one `isSendingQuickMessage` flag for the whole list,
    // which could only ever dim all four rows and never say which was pressed
    // - and nothing in the app ever set it, so it did not even do that.
    //
    // The tick was worse. It was set on the tap, before anything had been
    // written anywhere, so it appeared just as readily for a send that failed:
    // with no wearable in range and no device SIM number stored, every quick
    // message failed and every one of them drew a tick. It waits for the real
    // result now.
    var sending by remember { mutableStateOf(false) }
    // A timestamp rather than a flag, so a second send inside the window
    // restarts the mark instead of inheriting the first one's expiry.
    var sentAt by remember { mutableLongStateOf(0L) }
    val justSent = sentAt > 0L
    LaunchedEffect(sentAt) {
        if (sentAt > 0L) {
            delay(SENT_MARK_MS)
            sentAt = 0L
        }
    }
    val enabled = !sending

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .rowClickable(
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    sending = true
                    scope.launch {
                        val sent = onSend()
                        sending = false
                        if (sent) sentAt = System.currentTimeMillis()
                    }
                }
            )
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        // Set as a nameplate, like every other control in the system. These
        // are buttons, not quoted message text, and body type here made the
        // plate read as a component borrowed from another app.
        Nameplate(
            text = text,
            muted = !enabled,
            modifier = Modifier.weight(1f)
        )
        // A send arrow that becomes a tick and goes back.
        //
        // A crossfade rather than a swap. The row dims while the send is in
        // flight and the tick follows it, so the sequence reads arrow ->
        // dimmed arrow -> tick: pressed, working, gone. A failure stops at the
        // dimmed arrow and puts its reason in a snackbar, which is the whole
        // reason the tick is worth anything.
        //
        // It still means the transport took the message, not that anybody read
        // it - an SMS is `Sent` when the send call returns. Nothing in this
        // system carries a delivery receipt, so that is the honest ceiling.
        Crossfade(
            targetState = justSent,
            animationSpec = tween(Motion.normal),
            label = "quick-sent"
        ) { sent ->
            Icon(
                imageVector = if (sent) SafeShadeIcons.Tick02 else SafeShadeIcons.SendMessageDiagonal,
                contentDescription = null,
                tint = when {
                    sent -> colors.inkLive
                    enabled -> colors.inkMuted
                    else -> colors.inkFaint
                },
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** How long the tick stands in for the send arrow after a quick message. */
private const val SENT_MARK_MS = 1400L
