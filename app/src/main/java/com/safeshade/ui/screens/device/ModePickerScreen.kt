package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.icon
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.board.rowClickable
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** Everything the mode picker draws. */
data class ModePickerUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val activeMode: PersonaMode = PersonaMode.AUTO
)

/**
 * Which profile the wearable is running, and the seven it could.
 *
 * A profile is a choice about a life, and the previous page asked the reader
 * to make it from a carousel of cards below the fold, each carrying four
 * facts. This one separates *looking* from *choosing*. The list shows every
 * profile at once — the one running first, at the size it deserves, then the
 * other seven as rows a thumb can scan without swiping — and tapping any of
 * them opens a page that says everything the app knows about it: what the
 * wearable trips at in g, which screens it shows, what its lights do, what it
 * seals away from the wearer, and what the profile is for. Choosing happens
 * there, on a single deliberate button, so nothing on this list can
 * reconfigure somebody's fall detection by accident.
 */
@Composable
fun ModePickerScreen(
    state: ModePickerUiState,
    onOpenMode: (PersonaMode) -> Unit,
    onCompare: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val others = remember(state.activeMode) { PersonaMode.entries.filter { it != state.activeMode } }
    val gutter = Modifier.padding(horizontal = Spacing.gutter)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = "Adaptive mode",
                subtitle = "The profile decides how hard the wearable listens for a fall, " +
                    "what it shows on its own screen and what its lights do.",
                onBack = onBack,
                modifier = gutter
            )
        }

        item("offline") {
            OfflineNotice(
                connection = state.connection,
                queuedNote = "A profile chosen now is stored and sent as soon as the device connects.",
                modifier = gutter
            )
        }

        item("running") {
            Column(modifier = gutter) {
                SectionPlate(title = "Current profile")
                Spacer(Modifier.height(Spacing.md))
                RunningPlate(
                    mode = state.activeMode,
                    linkUsable = state.connection.isUsable,
                    onOpen = { onOpenMode(state.activeMode) }
                )
            }
        }

        item("profiles") {
            Column(modifier = gutter) {
                SectionPlate(title = "Other profiles")
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    others.forEachIndexed { index, mode ->
                        if (index > 0) Hairline()
                        ModeRow(mode = mode, onOpen = { onOpenMode(mode) })
                    }
                }
            }
        }

        item("compare") {
            BoardPlate(modifier = gutter.fillMaxWidth()) {
                Way(
                    name = "Compare the profiles",
                    detail = "Every profile's thresholds, screens and lights side by side.",
                    state = LampState.OFF,
                    stateLabel = "",
                    icon = SafeShadeIcons.AdaptiveMode,
                    accent = colors.inkMuted,
                    onClick = onCompare
                )
            }
        }

        item("footnote") {
            Footnote(
                "Fall sensitivity is the profile's starting point. It can be changed afterwards in device settings without leaving the profile.",
                modifier = gutter
            )
        }
    }
}

// ============================================================================
// Identity and state, shared with the detail page
// ============================================================================

/**
 * The decorative accent a mode is known by.
 *
 * Decorative, never state: this colour says which profile a plate is, and
 * says nothing whatever about whether the wearable is running it. That job
 * belongs to the pilot lamp and the state word beside it.
 */
internal fun modeAccent(mode: PersonaMode, colors: BoardColors): Color = when (mode) {
    PersonaMode.AUTO -> colors.accentSage
    PersonaMode.ELDERLY -> colors.accentSky
    PersonaMode.KIDS -> colors.accentSand
    PersonaMode.BIKE -> colors.accentMoss
    PersonaMode.PET -> colors.accentClay
    PersonaMode.HELMET -> colors.accentOchre
    PersonaMode.WRIST -> colors.accentLilac
    PersonaMode.BACKPACK -> colors.accentCove
}

/**
 * The lamp for a mode.
 *
 * A teal lamp on a dead link would claim we know what profile the wearable
 * is running. We know what we last stored, which is not the same thing — so a
 * selected mode with no usable link is UNKNOWN, not LIVE.
 */
internal fun modeLamp(selected: Boolean, inFlight: Boolean, linkUsable: Boolean, ack: AckState): LampState = when {
    inFlight -> ackLamp(ack, LampState.LIVE)
    selected && linkUsable -> LampState.LIVE
    selected -> LampState.UNKNOWN
    else -> LampState.OFF
}

/** The same distinction in words, because colour alone never carries it. */
internal fun modeStateWord(selected: Boolean, inFlight: Boolean, linkUsable: Boolean, unselectedWord: String = "Off"): String = when {
    inFlight -> "Sending"
    selected && linkUsable -> "Active"
    selected -> "Last set"
    else -> unselectedWord
}

internal fun stateInk(lamp: LampState, colors: BoardColors): Color = when (lamp) {
    LampState.LIVE -> colors.inkLive
    LampState.ATTENTION -> colors.inkAttention
    LampState.TRIP -> colors.inkTrip
    LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
}

// ============================================================================
// The running plate
// ============================================================================

/**
 * The profile the wearable is on, at the size the answer deserves.
 *
 * The scene band carries the identity — a wash of the profile's accent behind
 * the animation, and the accent at full strength as the rule beneath it —
 * while the plate, the ink and the lamp are the same materials as every
 * other surface. Identity in the band, state below the rule.
 */
@Composable
private fun RunningPlate(
    mode: PersonaMode,
    linkUsable: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val accent = modeAccent(mode, colors)
    val lamp = modeLamp(selected = true, inFlight = false, linkUsable = linkUsable, ack = AckState.IDLE)
    val stateWord = modeStateWord(selected = true, inFlight = false, linkUsable = linkUsable)
    val facts = ModeFacts.of(mode)

    BoardPlate(
        modifier = modifier
            .fillMaxWidth()
            .plateClickable(role = Role.Button, onClick = onOpen)
            .clearAndSetSemantics {
                selected = true
                contentDescription = "${mode.label}, $stateWord. ${facts.forWhom} Opens the profile."
            }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            BusTick(state = lamp, modifier = Modifier.fillMaxHeight())
            Column(modifier = Modifier.weight(1f).padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (mode.isGuardianLocked) {
                        Spacer(Modifier.width(Spacing.sm))
                        Seal()
                    }
                }
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PilotLamp(state = lamp, size = 12.dp)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = stateWord.uppercase(),
                        style = MaterialTheme.boardType.stateLabel,
                        color = stateInk(lamp, colors)
                    )
                }
                Spacer(Modifier.height(Spacing.md))
                Text(
                    text = facts.forWhom,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Everything about this profile",
                        style = MaterialTheme.boardType.nameplateSmall,
                        color = colors.inkMuted,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = SafeShadeIcons.ArrowRight01,
                        contentDescription = null,
                        tint = colors.inkFaint,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .fillMaxHeight()
                    .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                ModeScene(mode = mode, accent = accent, size = 108.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .width(Stroke.brass)
                        .fillMaxHeight()
                        .background(accent)
                )
            }
        }
    }
}

// ============================================================================
// A profile row
// ============================================================================

/**
 * One profile in the list: a scene thumbnail, the name, one line on who it is
 * for, and the seal where the wearer would lose the device's own menus.
 *
 * Built by hand rather than from `Way` because the leading slot holds a live
 * scene rather than a glyph, and a `Way` cannot carry one. The anatomy is
 * otherwise the row's: bus tick, nameplate, detail, trailing chevron.
 */
@Composable
private fun ModeRow(
    mode: PersonaMode,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val accent = modeAccent(mode, colors)
    val facts = ModeFacts.of(mode)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .rowClickable(role = Role.Button, onClick = onOpen)
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.sm, bottom = Spacing.sm)
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics {
                contentDescription = buildString {
                    append(mode.label); append(". ")
                    if (mode.isGuardianLocked) append("Guardian-sealed. ")
                    append(facts.forWhom)
                }
            }
    ) {
        BusTick(state = LampState.OFF, modifier = Modifier.fillMaxHeight())
        Spacer(Modifier.width(Spacing.md))
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            ModeScene(mode = mode, accent = accent, size = 58.dp, playing = false)
        }
        Spacer(Modifier.width(Spacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = mode.icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (mode.isGuardianLocked) {
                    Spacer(Modifier.width(Spacing.sm))
                    Seal()
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = facts.forWhom,
                style = MaterialTheme.boardType.rowDetail,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Icon(
            imageVector = SafeShadeIcons.ArrowRight01,
            contentDescription = null,
            tint = colors.inkFaint,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Modes · light", showBackground = true)
@Composable
private fun ModePickerPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(connection = ConnectionState.Ready, activeMode = PersonaMode.ELDERLY),
                onOpenMode = {},
                onCompare = {}
            )
        }
    }
}

@Preview(name = "Modes · dark, offline", showBackground = true)
@Composable
private fun ModePickerPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(connection = ConnectionState.Disconnected, activeMode = PersonaMode.AUTO),
                onOpenMode = {},
                onCompare = {}
            )
        }
    }
}
