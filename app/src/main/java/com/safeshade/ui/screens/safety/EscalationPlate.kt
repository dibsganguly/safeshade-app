package com.safeshade.ui.screens.safety

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safeshade.service.EscalationRun
import com.safeshade.service.EscalationStep
import com.safeshade.service.EscalationTarget
import com.safeshade.service.StepOutcome
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Timeline
import com.safeshade.ui.board.TimelineStop
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One alert's ladder, rung by rung, as the phone actually climbed it.
 *
 * A record of a run is a sequence with times, so it draws as a timeline
 * (2.23) rather than a bank of ways: Dialled, Skipped, Not needed, Waiting.
 * There is no "Reached" and no tick, because nothing on a phone can see that
 * a person picked up - the dialer returns the instant it opens. What is
 * known is the time and what the dial reported, and that is the whole of
 * each stop's line.
 *
 * Shared by the trip's own page, where it answers "what did the phone do
 * while I was not looking", and by the ladder's settings page, where it
 * shows the run in progress.
 */
@Composable
fun EscalationPlate(run: EscalationRun, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis()) {
    Timeline(
        run.steps.map { step ->
            val (lamp, word, line) = rungWay(step, now)
            TimelineStop(time = clock(rungTime(step, now)), what = "${rungName(step.target)}: $word · $line", state = lamp)
        },
        modifier = modifier.fillMaxWidth()
    )
}

/** The instant a rung's line should be read at: when it dialled, or when it is due. */
private fun rungTime(step: EscalationStep, now: Long): Long = when (val o = step.outcome) {
    is StepOutcome.Dialled -> o.at
    else -> step.dueAt
}

/** "Meera" for a contact; "Emergency number 112" for the end of the ladder. */
internal fun rungName(target: EscalationTarget): String = when (target) {
    is EscalationTarget.Contact -> target.label
    is EscalationTarget.Emergency -> "Emergency number ${target.number}"
}

/** The lamp, the word and the line for one rung. The time is shown separately by the [Timeline]. */
internal fun rungWay(step: EscalationStep, now: Long): Triple<LampState, String, String> =
    when (val o = step.outcome) {
        is StepOutcome.Dialled ->
            Triple(LampState.LIVE, "Dialled", o.result)

        is StepOutcome.Skipped ->
            Triple(LampState.OFF, "Skipped", o.reason)

        StepOutcome.Cancelled ->
            Triple(LampState.OFF, "Not needed", "The trip was closed before this rung came due")

        StepOutcome.Waiting ->
            if (step.dueAt <= now) {
                Triple(LampState.ATTENTION, "Due", "Dialling now")
            } else {
                Triple(LampState.ATTENTION, "Waiting", "Unless the trip is closed first")
            }
    }

internal fun clock(at: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
