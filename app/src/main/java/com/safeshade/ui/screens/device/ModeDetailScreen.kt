package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallSensitivity
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.icon
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import java.util.Locale

/** Everything one profile's page draws. */
data class ModeDetailUiState(
    val mode: PersonaMode = PersonaMode.ELDERLY,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val activeMode: PersonaMode = PersonaMode.AUTO,
    /** The mode written to `EXT MODE` and still waiting for `ACK:MODE:<name>`. */
    val inFlightMode: PersonaMode? = null,
    val ack: AckState = AckState.IDLE,
    /** A guardian-locked selection waiting on its confirmation. */
    val confirming: Boolean = false,
    /** The device's current fall sensitivity, so the thresholds read in real g. */
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM
)

/**
 * Everything the app knows about one profile, and the one button that applies it.
 *
 * The page is a bank of facts and a decision at the foot. The facts come from
 * the firmware, not the deck — what the wearable trips at in g for the
 * sensitivity the device is actually on, which screens the rotary cycles
 * through, what the lights do unasked, how often it asks for a position, what
 * a locked profile takes off the wearer's own menu — and the decision is one
 * commit-weight button. A locked profile's button opens the confirmation
 * that names what the wearer loses, in the wearable's own menu names.
 */
@Composable
fun ModeDetailScreen(
    state: ModeDetailUiState,
    onUse: () -> Unit,
    onConfirm: () -> Unit,
    onCancelConfirm: () -> Unit,
    onOpenFeature: (String) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val mode = state.mode
    val facts = remember(mode) { ModeFacts.of(mode) }
    val accent = modeAccent(mode, colors)
    val selected = state.activeMode == mode
    val inFlight = state.inFlightMode == mode
    val linkUsable = state.connection.isUsable
    val lamp = modeLamp(selected, inFlight, linkUsable, state.ack)
    val stateWord = modeStateWord(selected, inFlight, linkUsable, unselectedWord = "Not running")
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
                title = mode.label,
                subtitle = facts.forWhom,
                onBack = onBack,
                backDescription = "Back to the profiles",
                modifier = gutter,
                trailing = if (mode.isGuardianLocked) ({ Seal() }) else null
            )
        }

        // The scene band runs edge to edge under the gutter: identity above
        // the rule, state on the plate below it.
        item("scene") {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(196.dp)
                        .background(accent.copy(alpha = if (colors.isDark) 0.16f else 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    ModeScene(mode = mode, accent = accent, size = 168.dp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.brass)
                        .background(accent)
                )
            }
        }

        item("status") {
            BoardPlate(modifier = gutter.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stateWord.uppercase(),
                            style = MaterialTheme.boardType.stateLabel,
                            color = stateInk(lamp, colors)
                        )
                        val line = when {
                            inFlight -> ackWord(state.ack)
                            selected && linkUsable -> "The wearable is running this profile."
                            selected -> "Stored on this phone. It is sent the moment the device connects."
                            else -> "Starting fall sensitivity ${mode.defaultFallSensitivity.label.lowercase(Locale.getDefault())}."
                        }
                        if (line != null) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted
                            )
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    PilotLamp(state = lamp, size = 18.dp)
                }
            }
        }

        item("offline") {
            OfflineNotice(
                connection = state.connection,
                queuedNote = "A profile chosen now is stored and sent as soon as the device connects.",
                modifier = gutter
            )
        }

        item("fall") {
            Column(modifier = gutter) {
                SectionPlate(title = "Fall detection")
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        if (facts.fallDetection) {
                            val g = facts.impactG(state.fallSensitivity)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Readout(
                                    label = "Trips at",
                                    value = g?.let { String.format(Locale.US, "%.1f g", it) } ?: "—",
                                    large = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Readout(
                                    label = "Sensitivity",
                                    value = state.fallSensitivity.label,
                                    compact = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Readout(
                                    label = "Rotation",
                                    value = facts.rotationDps?.let { String.format(Locale.US, "%.0f°/s", it) } ?: "Not checked",
                                    compact = facts.rotationDps == null,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(Spacing.md))
                            Hairline()
                            Spacer(Modifier.height(Spacing.md))
                            Text(
                                text = fallExplanation(facts, state.fallSensitivity),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                        } else {
                            Text(
                                text = "Off in this profile.",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = "A tumble is play. The wearable tracks activity and position " +
                                    "instead, and being lost is the emergency it watches for.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            }
        }

        item("device") {
            Column(modifier = gutter) {
                SectionPlate(title = "On the wearable")
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    FactRow("Screens", facts.screens.joinToString(" · "))
                    Hairline()
                    FactRow("Home screen", facts.homeScreen)
                    Hairline()
                    FactRow("Lights", facts.lights)
                    Hairline()
                    FactRow("Position updates", "Every ${facts.gatewayPollMs / 1000} seconds from the gateway.")
                    Hairline()
                    FactRow(
                        "Cancelling a fall",
                        if (mode == PersonaMode.HELMET) "Two presses within ten seconds: “Are you OK?”, then “Press again”."
                        else "A double press within five seconds of the impact."
                    )
                }
            }
        }

        item("features") {
            Column(modifier = gutter) {
                SectionPlate(title = "Priority features")
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    facts.features.forEachIndexed { index, feature ->
                        if (index > 0) Hairline()
                        if (feature.route != null) {
                            Way(
                                name = feature.name,
                                state = LampState.OFF,
                                stateLabel = "",
                                detail = "In the app",
                                accent = accent,
                                onClick = { onOpenFeature(feature.route) }
                            )
                        } else {
                            Way(
                                name = feature.name,
                                state = LampState.OFF,
                                stateLabel = "",
                                detail = "On the wearable",
                                accent = accent
                            )
                        }
                    }
                }
            }
        }

        if (mode.isGuardianLocked) {
            item("sealed") {
                Column(modifier = gutter) {
                    SectionPlate(title = "Sealed on the wearable")
                    Spacer(Modifier.height(Spacing.md))
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "While this profile runs, the wearable hides these from its own menu:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                            Spacer(Modifier.height(Spacing.sm))
                            facts.sealedAway.forEach {
                                Text(
                                    text = "•  $it",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink
                                )
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            Text(
                                text = "The person wearing it cannot change the profile or turn fall " +
                                    "detection down from the device. Only this app can.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            }
        }

        item("deck") {
            Column(modifier = gutter) {
                SectionPlate(title = "In short")
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    FactRow("Algorithm", facts.algorithm)
                    Hairline()
                    FactRow("Screen", facts.uiChanges)
                }
            }
        }

        item("use") {
            Column(modifier = gutter) {
                val label = when {
                    selected && !inFlight -> if (linkUsable) "Running Now" else "Stored for the Device"
                    inFlight -> "Sending to the Device"
                    mode.isGuardianLocked -> "Seal to ${mode.label}"
                    else -> "Use ${mode.label}"
                }
                BoardButton(
                    label = label,
                    onClick = onUse,
                    weight = ButtonWeight.COMMIT,
                    enabled = !selected && !inFlight,
                    icon = if (mode.isGuardianLocked && !selected) SafeShadeIcons.ParentalControl else null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (state.confirming) {
        GuardianLockDialog(mode = mode, onConfirm = onConfirm, onCancel = onCancelConfirm)
    }
}

/** A label above a sentence. For facts, which are neither circuits nor settings. */
@Composable
private fun FactRow(label: String, value: String) {
    val colors = MaterialTheme.board
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Nameplate(label, small = true, muted = true)
        Spacer(Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
    }
}

/**
 * The threshold, in a sentence a person can check against the device.
 *
 * Every figure is derived from the firmware constants in [ModeFacts]; the
 * three sensitivities are spelled out so the reader sees what changing the
 * setting would do without leaving the page.
 */
private fun fallExplanation(facts: ModeFacts, sensitivity: FallSensitivity): String = buildString {
    val low = facts.impactG(FallSensitivity.LOW)
    val medium = facts.impactG(FallSensitivity.MEDIUM)
    val high = facts.impactG(FallSensitivity.HIGH)
    fun g(v: Float?) = v?.let { String.format(Locale.US, "%.1f g", it) } ?: "—"
    append("The wearable adds up its three accelerometer axes and trips when the sum passes ")
    append(g(facts.impactG(sensitivity)))
    append(" at ")
    append(sensitivity.label.lowercase(Locale.getDefault()))
    append(" sensitivity")
    if (low == medium && medium == high) {
        append(". This profile holds that floor whatever the sensitivity setting.")
    } else {
        append(" — ${g(low)} at low, ${g(medium)} at medium, ${g(high)} at high.")
    }
    if (facts.rotationDps != null) {
        append(" An impact only counts if the device was also turning faster than ")
        append(String.format(Locale.US, "%.0f°/s", facts.rotationDps))
        append(", which is what keeps road vibration from reading as a fall.")
    }
    if (facts.stillnessCheck) {
        append(" After an impact it watches for movement, and escalates if the wearer stays still.")
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
            Icon(imageVector = mode.icon, contentDescription = null, tint = colors.inkMuted)
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
                    text = "While ${mode.label} is active the wearable hides mode switching and its " +
                        "whole Safety menu. The person wearing it cannot change the profile or turn " +
                        "fall detection down from the device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "Only this app can change it back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink
                )
            }
        },
        confirmButton = {
            BoardButton(label = "Seal to ${mode.label}", onClick = onConfirm, weight = ButtonWeight.PRIMARY)
        },
        dismissButton = {
            BoardButton(label = "Cancel", onClick = onCancel, weight = ButtonWeight.QUIET)
        }
    )
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Mode detail · Elderly, light", showBackground = true)
@Composable
private fun ModeDetailPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModeDetailScreen(
                state = ModeDetailUiState(mode = PersonaMode.ELDERLY, connection = ConnectionState.Ready, activeMode = PersonaMode.AUTO),
                onUse = {}, onConfirm = {}, onCancelConfirm = {}, onOpenFeature = {}
            )
        }
    }
}

@Preview(name = "Mode detail · Bike, dark, running", showBackground = true)
@Composable
private fun ModeDetailPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModeDetailScreen(
                state = ModeDetailUiState(mode = PersonaMode.BIKE, connection = ConnectionState.Ready, activeMode = PersonaMode.BIKE),
                onUse = {}, onConfirm = {}, onCancelConfirm = {}, onOpenFeature = {}
            )
        }
    }
}
