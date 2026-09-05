package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.icon
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/** Everything the mode picker draws. */
data class ModePickerUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val activeMode: PersonaMode = PersonaMode.AUTO,
    /**
     * The mode written to `EXT MODE` and still waiting for `ACK:MODE:<name>`.
     *
     * Kept separate from [activeMode] so the list can show the old mode as
     * still current while the new one shows as in flight. Collapsing them would
     * mean the row lights up before the wearable has agreed to anything.
     */
    val inFlightMode: PersonaMode? = null,
    val ack: AckState = AckState.IDLE,
    /**
     * A guardian-locked mode the user has tapped but not yet confirmed.
     *
     * Hoisted rather than held in a `remember` inside the composable: the
     * confirmation gates a change with real consequences on the wearable, and
     * the caller is entitled to be able to cancel it, restore it, or test it.
     */
    val confirmingMode: PersonaMode? = null
)

/**
 * Which profile the wearable is running.
 *
 * Two things shape this screen. The first is that AUTO is not one of eight
 * peers — it is the firmware's fresh-boot default and the answer for anyone who
 * does not want to think about this at all, so it gets the hero and the seven
 * explicit modes get a list below a rule.
 *
 * The second is that four of those seven take something away from the person
 * wearing the device. In ELDERLY, KIDS, PET and HELMET the firmware hides its
 * own Mode and Safety menus, so the wearer cannot quietly turn their own fall
 * detection down. That is a defensible thing to do to a person and an
 * indefensible thing to do to them silently, which is why those modes carry a
 * seal in the list and a plainly worded confirmation on selection.
 */
@Composable
fun ModePickerScreen(
    state: ModePickerUiState,
    onSelectMode: (PersonaMode) -> Unit,
    onConfirmMode: (PersonaMode) -> Unit,
    onCancelConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

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
            Column {
                Text(
                    text = "Adaptive mode",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.ink
                )
                Text(
                    text = "The profile decides how hard the device listens for a " +
                        "fall and what it shows on its own screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
            }
        }

        item("offline") {
            OfflineNotice(
                connection = state.connection,
                // The mode is persisted and re-pushed when the link reaches
                // Ready, so this promise is one the caller actually keeps.
                queuedNote = "A mode chosen now is stored and sent as soon as the " +
                    "device connects."
            )
        }

        // AUTO is separated from the seven by a rule and a whole different
        // shape, because choosing it is a different kind of decision: it is
        // declining to choose, and that deserves to be the easy path rather
        // than the eighth row of a list.
        item("auto") {
            AutoHero(
                selected = state.activeMode == PersonaMode.AUTO,
                inFlight = state.inFlightMode == PersonaMode.AUTO,
                linkUsable = state.connection.isUsable,
                ack = state.ack,
                onSelect = { onSelectMode(PersonaMode.AUTO) }
            )
        }

        item("explicit-heading") { SectionPlate(title = "Choose a profile") }

        item("explicit") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                PersonaMode.entries
                    .filter { it != PersonaMode.AUTO }
                    .forEachIndexed { index, mode ->
                        if (index > 0) Hairline()
                        val selected = state.activeMode == mode
                        val inFlight = state.inFlightMode == mode
                        val linkUsable = state.connection.isUsable
                        Way(
                            name = mode.label,
                            state = when {
                                inFlight -> ackLamp(state.ack, LampState.LIVE)
                                // A teal lamp on a dead link would claim we know
                                // what profile the wearable is running. We know
                                // what we last stored, which is not the same
                                // thing and is exactly the Connected-versus-Ready
                                // lie this codebase has already paid for once.
                                selected && linkUsable -> LampState.LIVE
                                selected -> LampState.UNKNOWN
                                else -> LampState.OFF
                            },
                            stateLabel = when {
                                inFlight -> "Sending"
                                selected && linkUsable -> "Active"
                                selected -> "Last set"
                                else -> "Off"
                            },
                            detail = modeDetail(mode, selected, inFlight, state.ack),
                            icon = mode.icon,
                            // The seal marks the four modes that hide the
                            // wearable's own Mode and Safety menus. It appears
                            // on the row whether or not the mode is active, so
                            // it reads as a property of the choice rather than
                            // as a consequence discovered afterwards.
                            sealed = mode.isGuardianLocked,
                            onClick = { onSelectMode(mode) }
                        )
                    }
            }
        }

        item("footnote") {
            Text(
                text = "Fall sensitivity shown here is the profile's starting " +
                    "point. You can change it afterwards in device settings.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }

    if (state.confirmingMode != null) {
        GuardianLockDialog(
            mode = state.confirmingMode,
            onConfirm = { onConfirmMode(state.confirmingMode) },
            onCancel = onCancelConfirm
        )
    }
}

/**
 * The adaptive hero.
 *
 * Built from a plate, a nameplate and a lamp rather than a new card type: it is
 * wider and taller than a way, but it is the same material and obeys the same
 * rule that only the lamp carries colour.
 */
@Composable
private fun AutoHero(
    selected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    ack: AckState,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val lamp = when {
        inFlight -> ackLamp(ack, LampState.LIVE)
        selected && linkUsable -> LampState.LIVE
        selected -> LampState.UNKNOWN
        else -> LampState.OFF
    }
    val stateWord = when {
        inFlight -> "Sending"
        selected && linkUsable -> "Active"
        selected -> "Last set"
        else -> "Not selected"
    }

    BoardPlate(
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.RadioButton, onClick = onSelect)
            .clearAndSetSemantics {
                this.selected = selected
                contentDescription =
                    "Adaptive, $stateWord. ${PersonaMode.AUTO.blurb} " +
                        "Starting fall sensitivity " +
                        PersonaMode.AUTO.defaultFallSensitivity.label + "."
            }
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = PersonaMode.AUTO.icon,
                    contentDescription = null,
                    tint = colors.inkMuted,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = PersonaMode.AUTO.label,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.ink
                    )
                    Text(
                        text = "The device's own default",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
                Text(
                    text = stateWord.uppercase(),
                    style = MaterialTheme.boardType.stateLabel,
                    color = when (lamp) {
                        LampState.LIVE -> colors.inkLive
                        LampState.ATTENTION -> colors.inkAttention
                        LampState.TRIP -> colors.inkTrip
                        LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
                    }
                )
                Spacer(Modifier.width(Spacing.sm))
                PilotLamp(state = lamp, size = 18.dp)
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                // Straight from the enum. The firmware, the app and this
                // sentence should never be able to drift apart.
                text = PersonaMode.AUTO.blurb,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )

            Spacer(Modifier.height(Spacing.md))
            Hairline()
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate("Starting fall sensitivity", small = true, muted = true)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = PersonaMode.AUTO.defaultFallSensitivity.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
            }
            val ackLine = if (inFlight) ackWord(ack) else null
            if (ackLine != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = ackLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

/**
 * The consequence of a guardian-locked mode, said once and plainly.
 *
 * Deliberately not a "are you sure?" — that asks the user to guess what they
 * are agreeing to. It names the two things the wearer loses and the one way
 * back, and it uses the wearable's own menu names so the sentence can be
 * checked against the device in front of them.
 */
@Composable
private fun GuardianLockDialog(
    mode: PersonaMode,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.board
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.plate,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        icon = {
            Icon(
                imageVector = mode.icon,
                contentDescription = null,
                tint = colors.inkMuted
            )
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Seal the device to ${mode.label}",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink
                )
                Spacer(Modifier.width(Spacing.sm))
                Seal()
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "While ${mode.label} is active the wearable hides mode " +
                        "switching and its whole Safety menu. The person wearing " +
                        "it cannot change the profile or turn fall detection " +
                        "down from the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "Only this app can change it back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink
                )
                Text(
                    text = "Starting fall sensitivity: " +
                        mode.defaultFallSensitivity.label +
                        " — " + mode.defaultFallSensitivity.blurb.lowercase() + ".",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        },
        confirmButton = {
            BoardButton(
                label = "Seal to ${mode.label}",
                onClick = onConfirm,
                weight = ButtonWeight.PRIMARY
            )
        },
        dismissButton = {
            BoardButton(
                label = "Cancel",
                onClick = onCancel,
                weight = ButtonWeight.QUIET
            )
        }
    )
}

/**
 * The supporting line under a mode's name.
 *
 * The acknowledgement replaces the blurb rather than joining it while a write
 * is in flight: at that moment what the user needs is whether the device
 * agreed, and the description of the mode they just chose is not news.
 */
private fun modeDetail(
    mode: PersonaMode,
    selected: Boolean,
    inFlight: Boolean,
    ack: AckState
): String {
    if (inFlight) return ackWord(ack) ?: mode.blurb
    if (selected && ack == AckState.CONFIRMED) return "Confirmed by the device"
    return "Fall sensitivity ${mode.defaultFallSensitivity.label.lowercase()} · ${mode.blurb}"
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
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.AUTO
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · dark, sealed active", showBackground = true)
@Composable
private fun ModePickerPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.ELDERLY,
                    ack = AckState.CONFIRMED
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · confirming a seal", showBackground = true)
@Composable
private fun ModePickerPreviewConfirming() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.BACKPACK,
                    confirmingMode = PersonaMode.KIDS
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}

@Preview(name = "Modes · large text", showBackground = true, fontScale = 1.3f)
@Composable
private fun ModePickerPreviewLargeText() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModePickerScreen(
                state = ModePickerUiState(
                    connection = ConnectionState.Ready,
                    activeMode = PersonaMode.BACKPACK,
                    inFlightMode = PersonaMode.BIKE,
                    ack = AckState.PENDING
                ),
                onSelectMode = {},
                onConfirmMode = {},
                onCancelConfirm = {}
            )
        }
    }
}
