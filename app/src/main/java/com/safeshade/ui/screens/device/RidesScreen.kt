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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** One ride as the list prints it. */
data class RideRow(
    val id: String,
    val whenLabel: String,
    val distanceKm: Double,
    val movingMinutes: Int,
    val maxKmh: Double,
    val samples: Int,
    val live: Boolean
)

/** Everything the ride log draws. */
data class RidesUiState(
    val wearerName: String = "",
    val rides: List<RideRow> = emptyList(),
    val totalKm: Double = 0.0,
    val totalMinutes: Int = 0,
    val bikeMode: Boolean = false
)

/**
 * Ride log.
 *
 * Every journey the phone tracked, as distance, time moving and top speed
 * off the phone's own fixes. The figures are what the phone measured and
 * nothing more: a fix worse than fifty metres is dropped, and a ride with
 * three fixes says so. The Bike profile's brake light lives on the wearable
 * and is described where the lights are, not here.
 */
@Composable
fun RidesScreen(
    state: RidesUiState,
    onClear: () -> Unit,
    onStartJourney: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            )
    ) {
        ScreenHeader(title = "Ride log", subtitle = "What the phone measured on each journey", onBack = onBack)

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                Readout(label = "RIDES", value = if (state.rides.isEmpty()) "—" else state.rides.size.toString(), large = true, modifier = Modifier.weight(1f))
                Readout(label = "KM", value = if (state.rides.isEmpty()) "—" else "%.1f".format(state.totalKm), large = true, modifier = Modifier.weight(1f))
                Readout(label = "MOVING", value = if (state.rides.isEmpty()) "—" else minutesLabel(state.totalMinutes), large = true, modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Rides")
        Spacer(Modifier.height(Spacing.sm))
        if (state.rides.isEmpty()) {
            Note("Nothing tracked yet. A ride is recorded while a journey runs.")
            Spacer(Modifier.height(Spacing.md))
            BoardButton(label = "Start a Journey", icon = SafeShadeIcons.Bicycle01, onClick = onStartJourney, weight = ButtonWeight.SECONDARY, modifier = Modifier.fillMaxWidth())
        } else {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.rides.forEachIndexed { i, r ->
                    if (i > 0) Hairline()
                    Way(
                        name = r.whenLabel,
                        state = if (r.live) LampState.LIVE else LampState.OFF,
                        stateLabel = if (r.live) "Riding" else "%.1f km".format(r.distanceKm),
                        detail = if (r.samples < 3) {
                            "Too few fixes to measure (${r.samples})."
                        } else {
                            "${minutesLabel(r.movingMinutes)} moving · top %.0f km/h · ${r.samples} fixes".format(r.maxKmh)
                        },
                        icon = SafeShadeIcons.Bicycle01
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            BoardButton(label = "Clear the Log", onClick = onClear, weight = ButtonWeight.QUIET, icon = SafeShadeIcons.DeleteBin, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(Spacing.sm))
        Note(
            if (state.bikeMode) "Bike profile: the wearable's brake light strobes red on a hard stop by itself; nothing on the phone changes that."
            else "Distances come from this phone's own position fixes, once a minute while a journey runs."
        )
    }
}

internal fun minutesLabel(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60}"
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.board.inkMuted, modifier = Modifier.fillMaxWidth())
}

@Preview(name = "Rides", showBackground = true, heightDp = 900)
@Composable
private fun RidesPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            RidesScreen(
                state = RidesUiState(rides = listOf(RideRow("1", "Today 07:40", 4.2, 18, 24.0, 19, false)), totalKm = 4.2, totalMinutes = 18, bikeMode = true),
                onClear = {}, onStartJourney = {}, onBack = {}
            )
        }
    }
}
