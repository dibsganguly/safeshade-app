package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.LedPattern
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the lights screen draws. */
data class LightsUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val pattern: LedPattern = LedPattern.TORCH,
    /** Written to `LED_CHAR` and still waiting for its ack. */
    val inFlightPattern: LedPattern? = null,
    val ack: AckState = AckState.IDLE
)

/**
 * The wearable's LED ring.
 *
 * One deliberate omission: no colour swatches. Rainbow, Police, Fire and Ocean
 * all beg for a preview chip, and four of them on one screen would put more
 * saturated colour in front of the user than the entire rest of the app —
 * colour that means "this pattern is orange", not "this circuit is live". The
 * whole system rests on those never being confusable, so the patterns are
 * distinguished by name and description, and the only lit thing on the screen
 * is the lamp on the selected row.
 */
@Composable
fun LightsScreen(
    state: LightsUiState,
    onSelectPattern: (LedPattern) -> Unit,
    onBack: (() -> Unit)? = null,
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
            ScreenHeader(
                title = "Lights",
                subtitle = "The pattern the LED ring runs when it is on.",
                onBack = onBack
            )
        }

        // No queued-write promise here, unlike mode and settings. An LED
        // pattern is a direct characteristic write with nothing behind it that
        // replays on reconnect, so telling the user it would be sent later
        // would be inventing a mechanism that does not exist.
        item("offline") { OfflineNotice(connection = state.connection) }

        item("patterns-heading") { SectionPlate(title = "Pattern") }

        item("patterns") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                LedPattern.entries.forEachIndexed { index, pattern ->
                    if (index > 0) Hairline()
                    val selected = state.pattern == pattern
                    val inFlight = state.inFlightPattern == pattern
                    val linkUsable = state.connection.isUsable
                    Way(
                        name = pattern.label,
                        state = when {
                            inFlight -> ackLamp(state.ack, LampState.LIVE)
                            // Only claim a pattern is running while the link can
                            // actually tell us. Offline, all we know is what was
                            // last set from this phone.
                            selected && linkUsable -> LampState.LIVE
                            selected -> LampState.UNKNOWN
                            else -> LampState.OFF
                        },
                        stateLabel = when {
                            inFlight -> "Sending"
                            selected && linkUsable -> "Running"
                            selected -> "Last set"
                            else -> "Off"
                        },
                        detail = when {
                            inFlight -> ackWord(state.ack) ?: describe(pattern)
                            selected && state.ack == AckState.CONFIRMED ->
                                "Confirmed by the device"
                            else -> describe(pattern)
                        },
                        // Each pattern gets its own glyph, matched to what the
                        // name and description actually evoke — Police is the
                        // one pattern with no obvious match in the set, so it
                        // falls back to the plain lights glyph.
                        icon = iconFor(pattern),
                        onClick = { onSelectPattern(pattern) }
                    )
                }
            }
        }

        // The master on/off is not here because it cannot be here: the firmware
        // has no characteristic for it. Saying that plainly is better than a
        // switch at the top of this screen that does nothing.
        item("master-note") {
            BoardPlate(modifier = Modifier.fillMaxWidth(), recessed = true) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Nameplate("Turning the lights on and off", small = true, muted = true)
                    Text(
                        text = "The master switch for the LED ring is set on the " +
                            "wearable itself, under Settings › Lights. This app can " +
                            "choose the pattern but cannot switch the ring on or off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                    Text(
                        text = "If a pattern is confirmed here and nothing happens on " +
                            "the device, that switch is the first thing to check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }

    }
}

/**
 * What each pattern looks like, in words.
 *
 * Words rather than a swatch or an animation: the ring is on a device in
 * somebody's pocket, and a phone screen cannot preview it honestly anyway.
 */
private fun describe(pattern: LedPattern): String = when (pattern) {
    LedPattern.TORCH -> "A steady white beam, bright enough to see a path"
    LedPattern.RAINBOW -> "A slow cycle through the whole spectrum"
    LedPattern.CYBER -> "A fast sweep between two cool tones"
    LedPattern.POLICE -> "Hard alternating flashes, meant to be noticed"
    LedPattern.FIRE -> "An irregular warm flicker"
    LedPattern.OCEAN -> "A slow wash between blue and green"
    LedPattern.PULSE -> "One colour breathing in and out"
}

/**
 * The glyph for each pattern, matched to its own name and meaning rather than
 * a shared placeholder.
 *
 * Police has no counterpart in the custom set — inventing one would be a
 * mismatch of its own — so it keeps the plain lights glyph this whole screen
 * used to share.
 */
private fun iconFor(pattern: LedPattern) = when (pattern) {
    LedPattern.TORCH -> SafeShadeIcons.Torch
    LedPattern.RAINBOW -> SafeShadeIcons.Rainbow
    LedPattern.CYBER -> SafeShadeIcons.Cyber
    LedPattern.POLICE -> SafeShadeIcons.Police
    LedPattern.FIRE -> SafeShadeIcons.Fire
    LedPattern.OCEAN -> SafeShadeIcons.Ocean
    LedPattern.PULSE -> SafeShadeIcons.Pulse
}

// ============================================================================
// Previews
// ============================================================================

@Composable
private fun LightsPreviewHost(state: LightsUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        LightsScreen(state = state, onSelectPattern = {})
    }
}

@Preview(name = "Lights · light", showBackground = true)
@Composable
private fun LightsPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Ready,
                pattern = LedPattern.PULSE,
                ack = AckState.CONFIRMED
            )
        )
    }
}

@Preview(name = "Lights · dark", showBackground = true)
@Composable
private fun LightsPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Ready,
                pattern = LedPattern.TORCH,
                inFlightPattern = LedPattern.FIRE,
                ack = AckState.PENDING
            )
        )
    }
}

@Preview(name = "Lights · no reply", showBackground = true)
@Composable
private fun LightsPreviewNoReply() {
    SafeShadeTheme(darkTheme = false) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Disconnected,
                pattern = LedPattern.OCEAN,
                inFlightPattern = LedPattern.POLICE,
                ack = AckState.NO_RESPONSE
            )
        )
    }
}
