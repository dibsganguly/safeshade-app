package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/** Everything the journey screen draws. */
data class JourneyUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val guardianName: String = "",

    // ---- Setting one up
    val destinationLabel: String = "",
    val etaOptions: List<Int> = listOf(15, 25, 40, 60),
    val etaMinutes: Int = 25,
    val isCustomEta: Boolean = false,
    val customEtaText: String = "",

    // ---- While one is running
    val isActive: Boolean = false,
    /**
     * Held as a computed number rather than as a deadline timestamp.
     *
     * The screen stays a pure function of its state that way: no ticker, no
     * clock read during composition, and previews that render the same thing
     * every time. Counting down is the caller's job.
     */
    val minutesRemaining: Int? = null,
    /** 1.0 at the start, 0.0 at the ETA. Null when nothing is running. */
    val progress: Float? = null,
    val etaClockLabel: String? = null,
    val startedAtLabel: String? = null,
    /** Past the ETA but still inside the grace period. */
    val isOverdue: Boolean = false,
    /** Grace has run out and contacts have been messaged. */
    val escalated: Boolean = false,
    /** Who gets told. Named plainly so nobody is surprised by who was called. */
    val contactsSummary: String = "your emergency contacts",
    val isBusy: Boolean = false,
    val errorText: String? = null
) {
    /** The ETA actually in force, custom entry included. */
    val effectiveEtaMinutes: Int?
        get() = if (isCustomEta) customEtaText.toIntOrNull()?.takeIf { it in 1..600 } else etaMinutes

    val canStart: Boolean
        get() = destinationLabel.isNotBlank() && effectiveEtaMinutes != null && !isBusy
}

/**
 * Walk with me.
 *
 * A journey is a promise with a deadline attached: I am going here, I will be
 * there by then, and if I do not say I arrived, somebody is told where I was
 * last seen. The screen's whole job is to make that bargain unmissable *before*
 * it is accepted — a person who is surprised by the message their daughter
 * received has been failed by this screen, not by the feature.
 */
@Composable
fun JourneyScreen(
    state: JourneyUiState,
    onDestinationChange: (String) -> Unit,
    onEtaSelected: (Int) -> Unit,
    onCustomEtaSelected: () -> Unit,
    onCustomEtaChange: (String) -> Unit,
    onStart: () -> Unit,
    onArrived: () -> Unit,
    onCancelJourney: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            // The theme draws edge to edge, so the window does not resize
            // when the keyboard opens: without this inset a focused field would sit behind it.
            .imePadding()
    ) {
        ScreenHeader(
            title = "Walk with me",
            subtitle = if (state.isActive) state.destinationLabel.ifBlank { "Journey in progress" } else null,
            onBack = onBack,
            backDescription = "Go back to the circle",
            tier = ScreenTier.PUSHED,
            // The `top` is not decoration and is not this screen's choice. Every
            // sub-page outside this bank puts its header inside a LazyColumn
            // whose top contentPadding is `inset + Spacing.sm`; this bank puts
            // the header above the list, so without the same 8dp its content
            // started 8dp higher than every other bank's. That was the whole of
            // "inconsistent header-bottom paddings leading to different sub-page
            // content starting points" - the gap below the header was already
            // uniform; the gap above it was not.
            modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm)
        )

        // The list's own top contentPadding supplies the other half of
        // ScreenHeader's documented 28dp gap, same as every pattern-A screen
        // — see ScreenHeader's KDoc.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.lg,
                bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            if (state.isActive) {
                activeJourney(state, onArrived, onCancelJourney)
            } else {
                journeySetup(state, onDestinationChange, onEtaSelected, onCustomEtaSelected, onCustomEtaChange, onStart)
            }

            if (state.errorText != null) {
                item("error") {
                    Text(
                        text = state.errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkAttention
                    )
                }
            }
        }
    }
}

/** Choosing where and by when. */
private fun LazyListScope.journeySetup(
    state: JourneyUiState,
    onDestinationChange: (String) -> Unit,
    onEtaSelected: (Int) -> Unit,
    onCustomEtaSelected: () -> Unit,
    onCustomEtaChange: (String) -> Unit,
    onStart: () -> Unit
) {
    item("destination") {
        BoardField(
            value = state.destinationLabel,
            onValueChange = onDestinationChange,
            label = "Where are you going",
            placeholder = "The chemist on Park Street",
            supporting = "This exact wording is what appears in the message if the journey is not ended."
        )
    }

    item("eta-heading") {
        SectionPlate(title = "How long it should take")
    }

    item("eta") {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ChoiceGrid(items = state.etaOptions) { minutes, cell ->
                ChoicePlate(
                    label = "$minutes min",
                    selected = !state.isCustomEta && state.etaMinutes == minutes,
                    onClick = { onEtaSelected(minutes) },
                    modifier = cell
                )
            }
            ChoicePlate(
                label = "Some other time",
                selected = state.isCustomEta,
                onClick = onCustomEtaSelected,
                modifier = Modifier.fillMaxWidth()
            )
            if (state.isCustomEta) {
                BoardField(
                    value = state.customEtaText,
                    onValueChange = onCustomEtaChange,
                    label = "Minutes",
                    placeholder = "35",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
    }

    item("bargain") {
        BargainPlate(state)
    }

    item("start") {
        BoardButton(
            label = if (state.isBusy) "Starting" else "Start the journey",
            icon = SafeShadeIcons.RouteNavigation,
            supporting = if (state.canStart || state.isBusy) null else "A destination and a time are needed",
            onClick = onStart,
            enabled = state.canStart,
            weight = ButtonWeight.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The plain-language contract, stated before the journey starts.
 *
 * Deliberately not a footnote in faint grey under the button. This is the one
 * paragraph on the screen the user genuinely has to read, so it sits on a
 * plate at full contrast and names the destination and the people who would
 * be contacted rather than saying "your contacts".
 */
@Composable
private fun BargainPlate(state: JourneyUiState) {
    val colors = MaterialTheme.board
    val minutes = state.effectiveEtaMinutes

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = "What happens if you do not end it",
                style = MaterialTheme.typography.titleSmall,
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = buildString {
                    append("If you have not tapped ")
                    append("\"I have arrived\"")
                    if (minutes != null) {
                        append(" within ")
                        append(minutes)
                        append(" minutes")
                    }
                    append(", ")
                    append(state.contactsSummary)
                    append(" are sent a message saying where you were going and the last place this phone knew you were.")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = when (state.role) {
                    // A guardian phone starts journeys for the guardian's own
                    // walk. Saying "we will tell Baba you are late" would be
                    // exactly backwards.
                    UserRole.GUARDIAN ->
                        "This is for your own walk. Nothing here is sent to the device or to the person wearing it."
                    UserRole.COMPANION ->
                        "Ending the journey yourself is always enough. Nobody is contacted if you tap arrived."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }
}

/** A journey already under way. */
private fun LazyListScope.activeJourney(
    state: JourneyUiState,
    onArrived: () -> Unit,
    onCancelJourney: () -> Unit
) {
    item("clock") {
        JourneyClock(state)
    }

    item("arrived") {
        BoardButton(
            label = "I have arrived",
            icon = SafeShadeIcons.Tick02,
            onClick = onArrived,
            enabled = !state.isBusy,
            weight = ButtonWeight.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )
    }

    item("detail-heading") {
        SectionPlate(title = "This journey")
    }

    item("detail") {
        JourneyDetail(state)
    }

    item("cancel") {
        BoardButton(
            label = "Cancel this journey",
            onClick = onCancelJourney,
            enabled = !state.isBusy,
            weight = ButtonWeight.QUIET,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** The countdown, its lamp, and the one sentence that explains both. */
@Composable
private fun JourneyClock(state: JourneyUiState) {
    val colors = MaterialTheme.board

    val lamp = when {
        state.escalated -> LampState.TRIP
        state.isOverdue -> LampState.ATTENTION
        else -> LampState.LIVE
    }
    val word = when {
        state.escalated -> "Overdue – contacts told"
        state.isOverdue -> "Past the time"
        else -> "Running"
    }
    // Coarse on purpose. A screen reader announcing a number every second is
    // hostile; announcing "about twelve minutes left" as it changes is useful.
    val spoken = when {
        state.escalated -> "Journey overdue. ${state.contactsSummary} have been told."
        state.isOverdue -> "Past the expected arrival time."
        state.minutesRemaining != null -> "About ${state.minutesRemaining} minutes left."
        else -> "Journey running."
    }

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(state = lamp, description = word)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleSmall,
                    color = when (lamp) {
                        LampState.TRIP -> colors.inkTrip
                        LampState.ATTENTION -> colors.inkAttention
                        else -> colors.inkLive
                    }
                )
            }
            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                Readout(
                    label = if (state.isOverdue) "Time past" else "Time left",
                    value = state.minutesRemaining?.toString() ?: "--",
                    large = true
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "minutes",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            val progress = state.progress
            if (progress != null) {
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = when (lamp) {
                        LampState.TRIP -> colors.lampTrip
                        LampState.ATTENTION -> colors.lampAttention
                        else -> colors.ink
                    },
                    trackColor = colors.recess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.heavy)
                )
            }
            // Announced to screen readers only. Sighted users already have the
            // readout above; this used to also render as a visible line that
            // just restated the same number in words.
            Spacer(
                modifier = Modifier
                    .size(0.dp)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = spoken
                    }
            )
        }
    }
}

/** Where, when it started, when it is due, and who would be told. */
@Composable
private fun JourneyDetail(state: JourneyUiState) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = state.destinationLabel.ifBlank { "No destination was written down" },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                if (state.startedAtLabel != null) {
                    Readout(label = "Started", value = state.startedAtLabel)
                }
                if (state.etaClockLabel != null) {
                    Readout(label = "Due", value = state.etaClockLabel)
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = if (state.escalated) {
                    "${state.contactsSummary.replaceFirstChar { it.uppercase() }} have been sent the destination and the last known location."
                } else {
                    "If this is not ended, ${state.contactsSummary} are sent the destination and the last known location."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )
        }
    }
}

@Preview(name = "Journey — setup, light", showBackground = true, heightDp = 1000)
@Composable
private fun JourneySetupLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        JourneyScreen(
            state = JourneyUiState(
                role = UserRole.COMPANION,
                guardianName = "Priya",
                destinationLabel = "The chemist on Park Street",
                etaMinutes = 25,
                contactsSummary = "Priya and Anil"
            ),
            onDestinationChange = {},
            onEtaSelected = {},
            onCustomEtaSelected = {},
            onCustomEtaChange = {},
            onStart = {},
            onArrived = {},
            onCancelJourney = {},
            onBack = {}
        )
    }
}

@Preview(name = "Journey — running, dark", showBackground = true, heightDp = 1000)
@Composable
private fun JourneyActiveDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        JourneyScreen(
            state = JourneyUiState(
                role = UserRole.COMPANION,
                guardianName = "Priya",
                isActive = true,
                destinationLabel = "The chemist on Park Street",
                minutesRemaining = 12,
                progress = 0.48f,
                startedAtLabel = "18:40",
                etaClockLabel = "19:05",
                contactsSummary = "Priya and Anil"
            ),
            onDestinationChange = {},
            onEtaSelected = {},
            onCustomEtaSelected = {},
            onCustomEtaChange = {},
            onStart = {},
            onArrived = {},
            onCancelJourney = {},
            onBack = {}
        )
    }
}
