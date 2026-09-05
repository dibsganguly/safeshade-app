package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ActionResult
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.LiveSensorData
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.openInMaps
import com.safeshade.shareText
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.MainsPlate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import java.time.LocalDate

/** Everything the trip detail screen draws. */
data class TripDetailUiState(
    val trip: FallAlertEvent = FallAlertEvent(),
    val wearerName: String = "",
    /**
     * The coordinates behind `trip.location`, when there were any. The model
     * stores only a human label, so the map action needs these separately and
     * is hidden without them rather than opening an empty map.
     */
    val lat: Double? = null,
    val lon: Double? = null,
    /** The telemetry snapshot taken at trip time, if the link was live. */
    val sensor: LiveSensorData? = null,
    /** Who was called, when anybody was. */
    val contactedName: String? = null,
    val today: LocalDate = LocalDate.now()
)

/**
 * One trip, in full.
 *
 * A guardian arrives here from a notification hours later asking three
 * questions in order: what happened, where, and did anybody deal with it. The
 * screen answers them in that order and then offers the two things worth doing
 * afterwards — look at the place on a map, and send the account to somebody
 * else, usually a doctor or a sibling.
 *
 * Resolving is offered only while the trip is still open. A resolved trip is a
 * record, and a record with a live "mark as OK" button invites rewriting
 * history that a hospital might later ask about.
 */
@Composable
fun TripDetailScreen(
    state: TripDetailUiState,
    onBack: () -> Unit,
    onResolve: (TripOutcome) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    val trip = state.trip
    val subject = state.wearerName.ifBlank { "the wearer" }

    // Failures from the two intents this screen fires. Transient, tied to the
    // last press, and shown rather than logged.
    var failure by remember { mutableStateOf<String?>(null) }

    val account = remember(state, subject) { tripAccount(state, subject) }

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
        PanelHeader(
            title = trip.kind.label,
            subtitle = formatFullTimestamp(trip.timestamp, state.today),
            onBack = onBack
        )

        Spacer(Modifier.height(Spacing.lg))

        MainsPlate(
            state = trip.outcome.lamp,
            headline = trip.outcome.label,
            subline = outcomeExplanation(trip, subject, state.contactedName)
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "What was recorded")
        Spacer(Modifier.height(Spacing.sm))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                DetailLine("Event", trip.kind.label)
                DetailLine("Time", formatFullTimestamp(trip.timestamp, state.today))
                DetailLine("Outcome", trip.outcome.label)
                DetailLine(
                    label = "Place",
                    value = trip.location?.takeIf { it.isNotBlank() }
                        ?: "Not recorded. The phone had no location fix at the time."
                )
                if (!trip.note.isNullOrBlank()) {
                    DetailLine("Device said", trip.note.orEmpty())
                }
                DetailLine(
                    label = "Contact",
                    value = when {
                        trip.wasEmergencyContacted && state.contactedName != null ->
                            "${state.contactedName} was called"
                        trip.wasEmergencyContacted -> "A contact was called"
                        else -> "Nobody was called"
                    }
                )
            }
        }

        val sensor = state.sensor
        if (sensor != null) {
            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "Sensors at that moment")
            Spacer(Modifier.height(Spacing.sm))
            SensorPlate(sensor = sensor)
        }

        Spacer(Modifier.height(Spacing.xl))

        // Pulled into locals so the closure below captures two plain values
        // rather than relying on a smart cast surviving into a lambda.
        val lat = state.lat
        val lon = state.lon
        if (lat != null && lon != null) {
            BoardButton(
                label = "Open in maps",
                supporting = trip.location?.takeIf { it.isNotBlank() },
                icon = Icons.Outlined.Map,
                onClick = {
                    failure = when (val result = openInMaps(context, lat, lon, trip.kind.label)) {
                        is ActionResult.Failed -> result.reason
                        else -> null
                    }
                },
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))
        }

        BoardButton(
            label = "Share as text",
            supporting = "Send this account to a doctor or another family member.",
            icon = Icons.Outlined.Share,
            onClick = {
                failure = when (val result = shareText(context, account, "Share this trip")) {
                    is ActionResult.Failed -> result.reason
                    else -> null
                }
            },
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (failure != null) {
            Spacer(Modifier.height(Spacing.sm))
            FailureNote(text = failure.orEmpty())
        }

        if (trip.outcome == TripOutcome.PENDING) {
            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "Close this off")
            Spacer(Modifier.height(Spacing.sm))
            Note(
                text = "This trip is still open. Say what happened so the log stays honest and " +
                    "the board stops showing it as unanswered."
            )
            Spacer(Modifier.height(Spacing.md))
            BoardButton(
                label = "$subject is fine",
                onClick = { onResolve(TripOutcome.DISMISSED) },
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.md))
            BoardButton(
                label = "Somebody was called",
                onClick = { onResolve(TripOutcome.CONTACTED) },
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = "Times come from this phone's clock and places from its last location fix, " +
                "not from the device. A recorded place is where the phone was, which is not " +
                "always where $subject was.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint
        )
    }
}

/**
 * The telemetry snapshot.
 *
 * `isRealData` is honoured rather than ignored: an all-zero `LiveSensorData`
 * that has never received a payload is presented as "not received", because a
 * row of zeroes reads as a measurement, and a measurement of zero g on a
 * person who has just fallen is a lie.
 */
@Composable
private fun SensorPlate(sensor: LiveSensorData) {
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        if (!sensor.isRealData) {
            Way(
                name = "No telemetry",
                state = LampState.UNKNOWN,
                stateLabel = "None",
                detail = "The device was not connected when this happened, so nothing was captured."
            )
        } else {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                DetailLine("Impact", "%.2f g".format(sensor.magnitudeG))
                DetailLine("Axes", "X %.2f  Y %.2f  Z %.2f".format(sensor.accelX, sensor.accelY, sensor.accelZ))
                DetailLine("Temperature", "%.1f °C".format(sensor.temperature))
                DetailLine("Light", "${sensor.lightLevel}")
                DetailLine("Battery", "${sensor.batteryLevel}%")
            }
        }
        Hairline()
        Way(
            name = "Read from the device",
            state = LampState.OFF,
            stateLabel = "Snapshot",
            detail = "Taken at the moment of the trip, not now."
        )
    }
}

private fun outcomeExplanation(
    trip: FallAlertEvent,
    subject: String,
    contactedName: String?
): String = when (trip.outcome) {
    TripOutcome.PENDING -> "Nobody has said what happened yet."
    TripOutcome.DISMISSED -> "Marked as a false alarm."
    TripOutcome.CONTACTED -> contactedName?.let { "$it was called about this." }
        ?: "An emergency contact was called."
    TripOutcome.AUTO_RESOLVED -> "$subject answered on the device, so it closed itself."
}

/**
 * The shareable account of a trip.
 *
 * Written as sentences rather than as a field dump, because the recipient is a
 * person reading it in a messaging app — often a doctor with no context about
 * this product at all.
 */
private fun tripAccount(state: TripDetailUiState, subject: String): String = buildString {
    appendLine("SafeShade — ${state.trip.kind.label}")
    appendLine("Who: $subject")
    appendLine("When: ${formatFullTimestamp(state.trip.timestamp, state.today)}")
    if (!state.trip.location.isNullOrBlank()) appendLine("Where: ${state.trip.location}")
    if (state.lat != null && state.lon != null) {
        appendLine("Map: https://maps.google.com/?q=%.5f,%.5f".format(state.lat, state.lon))
    }
    appendLine("Outcome: ${state.trip.outcome.label}")
    if (state.trip.wasEmergencyContacted) {
        appendLine("Contacted: ${state.contactedName ?: "an emergency contact"}")
    }
    if (!state.trip.note.isNullOrBlank()) appendLine("Device reading: ${state.trip.note}")
    val sensor = state.sensor
    if (sensor != null && sensor.isRealData) {
        appendLine("Impact: %.2f g".format(sensor.magnitudeG))
    }
}.trim()

// ============================================
// PREVIEWS
// ============================================

private const val PREVIEW_TRIP_MS = 1_772_100_000_000L

private val previewTrip = FallAlertEvent(
    id = "t1",
    timestamp = PREVIEW_TRIP_MS,
    kind = TripKind.FALL,
    outcome = TripOutcome.PENDING,
    location = "Near Salt Lake Sector V",
    note = "Impact 3.4 g, no movement for 3 s"
)

@Preview(name = "Trip detail — light, open", showBackground = true, heightDp = 1400)
@Composable
private fun TripDetailLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        TripDetailScreen(
            state = TripDetailUiState(
                trip = previewTrip,
                wearerName = "Baba",
                lat = 22.5726,
                lon = 88.3639,
                sensor = LiveSensorData(
                    accelX = 1.8f, accelY = 2.4f, accelZ = 1.6f,
                    temperature = 31.4f, lightLevel = 220, batteryLevel = 68,
                    isRealData = true
                ),
                today = localDateOf(PREVIEW_TRIP_MS)
            ),
            onBack = {}, onResolve = {}
        )
    }
}

@Preview(
    name = "Trip detail — dark, resolved",
    showBackground = true,
    heightDp = 1200,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TripDetailDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        TripDetailScreen(
            state = TripDetailUiState(
                trip = previewTrip.copy(
                    outcome = TripOutcome.CONTACTED,
                    wasEmergencyContacted = true
                ),
                wearerName = "Baba",
                contactedName = "Priya",
                sensor = LiveSensorData(),
                today = localDateOf(PREVIEW_TRIP_MS)
            ),
            onBack = {}, onResolve = {}
        )
    }
}
