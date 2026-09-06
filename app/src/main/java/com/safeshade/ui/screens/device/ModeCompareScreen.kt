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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallSensitivity
import com.safeshade.data.PersonaMode
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Seal
import com.safeshade.ui.board.icon
import com.safeshade.ui.board.plateClickable
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import java.util.Locale

/** Everything the comparison draws. */
data class ModeCompareUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val activeMode: PersonaMode = PersonaMode.AUTO,
    val fallSensitivity: FallSensitivity = FallSensitivity.MEDIUM
)

/**
 * Every profile's figures, one under another, so the differences are visible
 * without paging. Four readouts per profile — what it trips at, whether it
 * needs a rotation, how many screens it cycles, how often it asks for a
 * position — is the set that actually differs between them; the rest is on
 * each profile's own page.
 */
@Composable
fun ModeCompareScreen(
    state: ModeCompareUiState,
    onOpenMode: (PersonaMode) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val gutter = Modifier.padding(horizontal = Spacing.gutter)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item("title") {
            ScreenHeader(
                title = "Compare profiles",
                subtitle = "Thresholds read for the device's current sensitivity, " +
                    state.fallSensitivity.label.lowercase(Locale.getDefault()) + ".",
                onBack = onBack,
                modifier = gutter
            )
        }
        items(ModeFacts.all.size, key = { ModeFacts.all[it].mode.name }) { index ->
            val facts = ModeFacts.all[index]
            val mode = facts.mode
            val accent = modeAccent(mode, colors)
            val selected = mode == state.activeMode
            val lamp = modeLamp(selected, inFlight = false, linkUsable = state.connection.isUsable, ack = AckState.IDLE)
            BoardPlate(
                modifier = gutter
                    .fillMaxWidth()
                    .plateClickable(role = Role.Button, onClick = { onOpenMode(mode) })
            ) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    BusTick(state = lamp, modifier = Modifier.fillMaxHeight())
                    Column(modifier = Modifier.weight(1f).padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = mode.icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(Spacing.sm))
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.ink,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (mode.isGuardianLocked) {
                                Spacer(Modifier.width(Spacing.sm))
                                Seal()
                            }
                            if (selected) {
                                Spacer(Modifier.width(Spacing.sm))
                                Text(
                                    text = if (state.connection.isUsable) "ACTIVE" else "LAST SET",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = stateInk(lamp, colors)
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        Hairline()
                        Spacer(Modifier.height(Spacing.md))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            val g = facts.impactG(state.fallSensitivity)
                            Readout(
                                label = "Trips at",
                                value = if (!facts.fallDetection) "Off" else g?.let { String.format(Locale.US, "%.1f g", it) } ?: "—",
                                compact = !facts.fallDetection,
                                modifier = Modifier.weight(1f)
                            )
                            Readout(
                                label = "Rotation",
                                value = facts.rotationDps?.let { String.format(Locale.US, "%.0f°/s", it) } ?: "—",
                                modifier = Modifier.weight(1f)
                            )
                            Readout(
                                label = "Screens",
                                value = facts.screens.size.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            Readout(
                                label = "Position",
                                value = "${facts.gatewayPollMs / 1000} s",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Compare · light", showBackground = true)
@Composable
private fun ModeComparePreview() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            ModeCompareScreen(state = ModeCompareUiState(activeMode = PersonaMode.KIDS), onOpenMode = {})
        }
    }
}
