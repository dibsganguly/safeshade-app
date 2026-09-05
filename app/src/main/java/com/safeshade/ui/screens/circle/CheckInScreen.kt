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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/** How a past check-in ended. */
enum class CheckInOutcome { ANSWERED, MISSED, CANCELLED }

/** One past check-in. */
data class CheckInHistoryRow(
    val id: String,
    /** When it was sent, in words: "Today 18:02". */
    val label: String,
    val outcome: CheckInOutcome,
    /** "Answered in 40 seconds", "No answer after 5 minutes". */
    val detail: String? = null
)

/** Everything the check-in screen draws. */
data class CheckInUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val guardianName: String = "",

    // ---- Sending one
    val deadlineOptions: List<Int> = listOf(2, 5, 10, 15),
    val deadlineMinutes: Int = 5,

    // ---- The one currently open
    val hasOpenRequest: Boolean = false,
    val sentAtLabel: String? = null,
    val dueAtLabel: String? = null,
    /**
     * Computed by the caller, not ticked here.
     *
     * The screen must render the same thing twice for the same state — that is
     * what makes it previewable and testable — so the clock lives outside it.
     */
    val secondsRemaining: Int? = null,
    val totalSeconds: Int? = null,
    /** The deadline passed with no answer and the escalation has run. */
    val escalated: Boolean = false,
    /** Set once answered, so a guardian sees the outcome without leaving. */
    val answeredLabel: String? = null,

    val history: List<CheckInHistoryRow> = emptyList(),
    /** Who is contacted on a miss. Named, so nobody is surprised afterwards. */
    val contactsSummary: String = "your emergency contacts",
    val isBusy: Boolean = false,
    val errorText: String? = null
) {
    val canSend: Boolean get() = !hasOpenRequest && !isBusy
}

/**
 * "Are you OK?" with a deadline behind it.
 *
 * This is the one feature in the app that can start an emergency from nothing
 * — no fall, no button press, just silence — so the deadline and what follows
 * it are stated on the same screen as the send button, not buried in settings.
 * A guardian who does not know that five minutes of silence will message the
 * whole contact list will send one at a bad moment and lose trust in the app.
 *
 * The wearer's side of the same screen is the mirror: one open question and
 * one very large way to answer it.
 */
@Composable
fun CheckInScreen(
    state: CheckInUiState,
    onDeadlineSelected: (Int) -> Unit,
    onSendCheckIn: () -> Unit,
    onCancelRequest: () -> Unit,
    onRespondOk: () -> Unit,
    onOpenContacts: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val other = counterpartName(state.role, state.wearerName, state.guardianName)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
    ) {
        ScreenHeader(
            title = "Check in",
            subtitle = when (state.role) {
                UserRole.GUARDIAN -> "Ask $other whether they are all right"
                UserRole.COMPANION -> "$other can ask whether you are all right"
            },
            onBack = onBack,
            backDescription = "Go back to the circle",
            tier = ScreenTier.PUSHED,
            modifier = Modifier.padding(horizontal = Spacing.gutter)
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
            if (state.hasOpenRequest) {
                item("open") { OpenRequestPlate(state) }

                // The wearer's answer is the primary action on their phone and
                // the only one that matters; the guardian's is a withdrawal.
                if (state.role == UserRole.COMPANION) {
                    item("respond") {
                        BoardButton(
                            label = "I am OK",
                            supporting = "Answers ${counterpartName(state.role, state.wearerName, state.guardianName)} and stops the timer",
                            icon = SafeShadeIcons.CheckIn,
                            onClick = onRespondOk,
                            enabled = !state.isBusy,
                            weight = ButtonWeight.PRIMARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    item("cancel") {
                        BoardButton(
                            label = "Withdraw the question",
                            supporting = "Stops the timer. Nothing is recorded and nobody is contacted.",
                            onClick = onCancelRequest,
                            enabled = !state.isBusy,
                            weight = ButtonWeight.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (state.role == UserRole.GUARDIAN) {
                item("deadline-heading") {
                    SectionPlate(title = "How long to wait for an answer")
                }
                item("deadline") {
                    ChoiceGrid(items = state.deadlineOptions) { minutes, cell ->
                        ChoicePlate(
                            label = "$minutes min",
                            selected = state.deadlineMinutes == minutes,
                            onClick = { onDeadlineSelected(minutes) },
                            modifier = cell
                        )
                    }
                }
                item("send") {
                    BoardButton(
                        label = if (state.isBusy) "Sending" else "Ask \"Are you OK?\"",
                        supporting = "The device buzzes and shows the question until it is answered",
                        icon = Icons.Outlined.HelpOutline,
                        onClick = onSendCheckIn,
                        enabled = state.canSend,
                        weight = ButtonWeight.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                item("idle") {
                    EmptyBay(
                        message = "Nothing to answer right now. When ${counterpartName(state.role, state.wearerName, state.guardianName)} asks whether you are all right, the question appears here and on the device.",
                        // Nobody has asked anything yet — the questioning
                        // face fits better than a searching one here.
                        shadyMood = ShadyMood.CURIOUS
                    )
                }
            }

            item("escalation-heading") {
                SectionPlate(title = "If there is no answer")
            }

            item("escalation") {
                EscalationPlate(state = state, onOpenContacts = onOpenContacts)
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

            if (state.history.isNotEmpty()) {
                item("history-heading") {
                    SectionPlate(title = "Recent check-ins")
                }
                item("history") {
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        state.history.forEachIndexed { index, row ->
                            if (index > 0) Hairline()
                            Way(
                                name = row.label,
                                state = row.outcome.toLampState(),
                                stateLabel = row.outcome.label,
                                detail = row.detail
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The open question and its countdown.
 *
 * `TripBanner` would be the obvious component and is the wrong one: it fills
 * the whole screen, so it cannot sit in a list beside the escalation notes and
 * the history a guardian is reading while they wait. A check-in with time left
 * on it is also not an emergency yet, and dressing it as one teaches people to
 * discount the real banner when it does appear.
 */
@Composable
private fun OpenRequestPlate(state: CheckInUiState) {
    val colors = MaterialTheme.board

    val lamp = if (state.escalated) LampState.TRIP else LampState.ATTENTION
    val word = when {
        state.escalated -> "No answer"
        state.answeredLabel != null -> "Answered"
        else -> "Waiting"
    }
    val remaining = state.secondsRemaining

    // Coarse and Polite. A per-second assertive announcement of a countdown
    // interrupts everything else a screen-reader user is doing, every second.
    val spoken = when {
        state.escalated -> "No answer. ${state.contactsSummary} have been told."
        remaining == null -> "Waiting for an answer."
        remaining <= 60 -> "Less than a minute left to answer."
        else -> "About ${remaining / 60} minutes left to answer."
    }

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PilotLamp(state = lamp, description = word)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = word,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (state.escalated) colors.inkTrip else colors.inkAttention
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = when (state.role) {
                    UserRole.GUARDIAN -> "\"Are you OK?\" is showing on the device."
                    UserRole.COMPANION -> "\"Are you OK?\" — tap the button below, or press the button on the device."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = colors.ink
            )

            Spacer(Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.Bottom) {
                Readout(
                    label = "Time to answer",
                    value = remaining?.let { formatClock(it) } ?: "--:--",
                    large = true,
                    state = if (state.escalated) LampState.TRIP else null
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    if (state.sentAtLabel != null) {
                        Readout(label = "Asked", value = state.sentAtLabel)
                    }
                    if (state.dueAtLabel != null) {
                        Spacer(Modifier.height(Spacing.xs))
                        Readout(label = "Due", value = state.dueAtLabel)
                    }
                }
            }

            val total = state.totalSeconds
            if (remaining != null && total != null && total > 0) {
                Spacer(Modifier.height(Spacing.sm))
                LinearProgressIndicator(
                    progress = { (remaining.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
                    color = if (state.escalated) colors.lampTrip else colors.lampAttention,
                    trackColor = colors.recess,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Stroke.heavy)
                )
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = spoken,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
            )

            if (state.answeredLabel != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = state.answeredLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkLive
                )
            }
        }
    }
}

/** What a missed check-in sets off, listed rather than summarised. */
@Composable
private fun EscalationPlate(state: CheckInUiState, onOpenContacts: () -> Unit) {
    val colors = MaterialTheme.board
    val subject = wearerSubject(state.role, state.wearerName)

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            listOf(
                "The device buzzes again and keeps the question on screen.",
                "A missed check-in is recorded on the board as a trip, with the time and the last known place.",
                "${state.contactsSummary.replaceFirstChar { it.uppercase() }} are sent a message saying $subject did not answer, with that last known place."
            ).forEachIndexed { index, line ->
                if (index > 0) Spacer(Modifier.height(Spacing.sm))
                Row {
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkFaint
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            BoardButton(
                label = "See who would be contacted",
                onClick = onOpenContacts,
                weight = ButtonWeight.QUIET,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** mm:ss. Monospaced in the readout, so the digits do not jitter. */
private fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}

private fun CheckInOutcome.toLampState(): LampState = when (this) {
    CheckInOutcome.ANSWERED -> LampState.LIVE
    CheckInOutcome.MISSED -> LampState.TRIP
    CheckInOutcome.CANCELLED -> LampState.OFF
}

private val CheckInOutcome.label: String
    get() = when (this) {
        CheckInOutcome.ANSWERED -> "Answered"
        CheckInOutcome.MISSED -> "No answer"
        CheckInOutcome.CANCELLED -> "Withdrawn"
    }

private val sampleHistory = listOf(
    CheckInHistoryRow("1", "Today 14:10", CheckInOutcome.ANSWERED, "Answered in 40 seconds"),
    CheckInHistoryRow("2", "Yesterday 21:30", CheckInOutcome.MISSED, "No answer after 5 minutes"),
    CheckInHistoryRow("3", "Yesterday 09:05", CheckInOutcome.CANCELLED, "Withdrawn after 20 seconds")
)

@Preview(name = "Check in — guardian sending, light", showBackground = true, heightDp = 1100)
@Composable
private fun CheckInGuardianLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        CheckInScreen(
            state = CheckInUiState(
                role = UserRole.GUARDIAN,
                wearerName = "Baba",
                deadlineMinutes = 5,
                history = sampleHistory,
                contactsSummary = "Priya and Anil"
            ),
            onDeadlineSelected = {},
            onSendCheckIn = {},
            onCancelRequest = {},
            onRespondOk = {},
            onOpenContacts = {},
            onBack = {}
        )
    }
}

@Preview(name = "Check in — wearer answering, dark", showBackground = true, heightDp = 1100)
@Composable
private fun CheckInCompanionDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        CheckInScreen(
            state = CheckInUiState(
                role = UserRole.COMPANION,
                guardianName = "Priya",
                hasOpenRequest = true,
                sentAtLabel = "18:02",
                dueAtLabel = "18:07",
                secondsRemaining = 214,
                totalSeconds = 300,
                history = sampleHistory,
                contactsSummary = "Priya and Anil"
            ),
            onDeadlineSelected = {},
            onSendCheckIn = {},
            onCancelRequest = {},
            onRespondOk = {},
            onOpenContacts = {},
            onBack = {}
        )
    }
}
