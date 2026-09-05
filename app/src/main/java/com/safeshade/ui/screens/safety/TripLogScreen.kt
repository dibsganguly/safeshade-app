package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fence
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonalInjury
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import java.time.LocalDate

/** Everything the trip log draws. */
data class TripLogUiState(
    val trips: List<FallAlertEvent> = emptyList(),
    val wearerName: String = "",
    /**
     * Injected rather than read from the clock inside the composable so that
     * "Today" is stable across a recomposition and deterministic in a preview.
     */
    val today: LocalDate = LocalDate.now()
)

/**
 * Everything that has ever tripped, newest first.
 *
 * "Trip" is the board's word and the right one: a fall, an SOS press, a missed
 * check-in and a safe-zone exit are all the same shape of event — something
 * broke the circuit, and a person either responded or did not.
 *
 * Grouped by day because that is how the question is asked. Nobody wonders
 * what happened at 14:32; they wonder whether anything happened today.
 */
@Composable
fun TripLogScreen(
    state: TripLogUiState,
    onBack: () -> Unit,
    onOpenTrip: (String) -> Unit,
    onOpenFallSettings: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val subject = state.wearerName.ifBlank { "the wearer" }

    // Grouping is done once per list change, outside the lazy content — doing
    // it inside would redo the whole sort on every scroll frame.
    val days = remember(state.trips) {
        state.trips
            .sortedByDescending { it.timestamp }
            .groupBy { localDateOf(it.timestamp) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("header") {
            PanelHeader(
                title = "Trip log",
                subtitle = "Everything the device has raised about $subject.",
                onBack = onBack
            )
        }

        if (state.trips.isEmpty()) {
            item("empty") {
                EmptyBay(
                    message = "Nothing has tripped. Falls, SOS presses, missed check-ins and " +
                        "safe-zone exits all appear here with the time and place.",
                    actionLabel = "Check fall settings",
                    onAction = onOpenFallSettings
                )
            }
        }

        days.forEach { (day, trips) ->
            item("head-$day") {
                SectionPlate(title = formatDayHeading(day, state.today))
            }
            item("day-$day") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    trips.forEachIndexed { index, trip ->
                        if (index > 0) Hairline()
                        Way(
                            name = trip.kind.label,
                            state = trip.outcome.lamp,
                            stateLabel = trip.outcome.shortLabel,
                            detail = tripDetail(trip),
                            icon = trip.kind.icon,
                            onClick = { onOpenTrip(trip.id) }
                        )
                    }
                }
            }
        }

        if (state.trips.isNotEmpty()) {
            item("footer") {
                Note(
                    text = "A trip stays open until somebody answers it. Open one to see where " +
                        "it happened and what the sensors read at the time."
                )
            }
        }
    }
}

/**
 * The one line under a trip's name.
 *
 * Time first, because that is what is being scanned for; then place, then the
 * sensor note. All three are optional in the model, and an absent one is left
 * out rather than padded with a dash.
 */
private fun tripDetail(trip: FallAlertEvent): String = buildString {
    append(formatTimeOfDay(trip.timestamp))
    if (!trip.location.isNullOrBlank()) {
        append(" · ")
        append(trip.location)
    }
    if (!trip.note.isNullOrBlank()) {
        append(" · ")
        append(trip.note)
    }
    if (trip.wasEmergencyContacted) append(" · Contact called")
}

/**
 * An outcome as a lamp.
 *
 * PENDING is the only one that trips. A trip nobody answered is the entire
 * reason this log exists, and it must not read the same as one that was
 * resolved an hour later.
 */
internal val TripOutcome.lamp: LampState
    get() = when (this) {
        TripOutcome.PENDING -> LampState.TRIP
        TripOutcome.CONTACTED -> LampState.ATTENTION
        TripOutcome.DISMISSED -> LampState.OFF
        TripOutcome.AUTO_RESOLVED -> LampState.OFF
    }

/** The state word beside the lamp. Short enough for the column, plain enough to trust. */
internal val TripOutcome.shortLabel: String
    get() = when (this) {
        TripOutcome.PENDING -> "Open"
        TripOutcome.CONTACTED -> "Called"
        TripOutcome.DISMISSED -> "OK"
        TripOutcome.AUTO_RESOLVED -> "Closed"
    }

internal val TripKind.icon: ImageVector
    get() = when (this) {
        TripKind.FALL -> Icons.Outlined.PersonalInjury
        TripKind.SOS -> Icons.Outlined.NotificationsActive
        TripKind.PHONE_SOS -> Icons.Outlined.NotificationsActive
        TripKind.MISSED_CHECKIN -> Icons.Outlined.Schedule
        TripKind.ZONE_EXIT -> Icons.Outlined.Fence
        TripKind.JOURNEY_OVERDUE -> Icons.Outlined.Timer
    }

// ============================================
// PREVIEWS
// ============================================

/**
 * Fixed timestamps rather than `System.currentTimeMillis()` offsets, so a
 * preview screenshot is the same image every time it is rendered.
 */
private const val PREVIEW_TODAY_MS = 1_772_100_000_000L
private const val PREVIEW_DAY_MS = 86_400_000L

private val previewTrips = listOf(
    FallAlertEvent(
        id = "t1",
        timestamp = PREVIEW_TODAY_MS,
        kind = TripKind.FALL,
        outcome = TripOutcome.PENDING,
        location = "Near Salt Lake Sector V",
        note = "Impact 3.4 g"
    ),
    FallAlertEvent(
        id = "t2",
        timestamp = PREVIEW_TODAY_MS - 7_200_000L,
        kind = TripKind.ZONE_EXIT,
        outcome = TripOutcome.AUTO_RESOLVED,
        location = "Left Home"
    ),
    FallAlertEvent(
        id = "t3",
        timestamp = PREVIEW_TODAY_MS - PREVIEW_DAY_MS,
        kind = TripKind.SOS,
        outcome = TripOutcome.CONTACTED,
        wasEmergencyContacted = true,
        location = "Gariahat crossing",
        note = "Button held 3 s"
    ),
    FallAlertEvent(
        id = "t4",
        timestamp = PREVIEW_TODAY_MS - PREVIEW_DAY_MS - 20_000_000L,
        kind = TripKind.MISSED_CHECKIN,
        outcome = TripOutcome.DISMISSED
    )
)

private val previewToday: LocalDate = localDateOf(PREVIEW_TODAY_MS)

@Preview(name = "Trip log — light", showBackground = true, heightDp = 900)
@Composable
private fun TripLogLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        TripLogScreen(
            state = TripLogUiState(trips = previewTrips, wearerName = "Baba", today = previewToday),
            onBack = {}, onOpenTrip = {}, onOpenFallSettings = {}
        )
    }
}

@Preview(
    name = "Trip log — dark, empty",
    showBackground = true,
    heightDp = 600,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TripLogEmptyDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        TripLogScreen(
            state = TripLogUiState(today = previewToday),
            onBack = {}, onOpenTrip = {}, onOpenFallSettings = {}
        )
    }
}
