package com.safeshade.ui.screens.safety

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.safeshade.service.EscalationRun
import com.safeshade.service.EscalationStep
import com.safeshade.service.EscalationTarget
import com.safeshade.service.StepOutcome
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * One alert's ladder, rung by rung, as the phone actually climbed it.
 *
 * Every row is a way with a state word the run can stand behind: Dialled,
 * Skipped, Not needed, Waiting. There is no "Reached" and no tick, because
 * nothing on a phone can see that a person picked up - the dialer returns
 * the instant it opens. What is known is the time and what the dial
 * reported, and that is the whole of the detail line.
 *
 * Shared by the trip's own page, where it answers "what did the phone do
 * while I was not looking", and by the ladder's settings page, where it
 * shows the run in progress.
 */
@Composable
fun EscalationPlate(run: EscalationRun, modifier: Modifier = Modifier, now: Long = System.currentTimeMillis()) {
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        run.steps.forEachIndexed { index, step ->
            if (index > 0) Hairline()
            val (lamp, word, line) = rungWay(step, now)
            Way(
                name = rungName(step.target),
                state = lamp,
                stateLabel = word,
                detail = line,
                icon = SafeShadeIcons.CallAfterAFall
            )
        }
    }
}

/** "Meera" for a contact; "Emergency number 112" for the end of the ladder. */
internal fun rungName(target: EscalationTarget): String = when (target) {
    is EscalationTarget.Contact -> target.label
    is EscalationTarget.Emergency -> "Emergency number ${target.number}"
}

/** The lamp, the word and the line for one rung. */
internal fun rungWay(step: EscalationStep, now: Long): Triple<LampState, String, String> =
    when (val o = step.outcome) {
        is StepOutcome.Dialled ->
            Triple(LampState.LIVE, "Dialled", "At ${clock(o.at)} · ${o.result}")

        is StepOutcome.Skipped ->
            Triple(LampState.OFF, "Skipped", o.reason)

        StepOutcome.Cancelled ->
            Triple(LampState.OFF, "Not needed", "The trip was closed before this rung came due")

        StepOutcome.Waiting ->
            if (step.dueAt <= now) {
                Triple(LampState.ATTENTION, "Due", "Due at ${clock(step.dueAt)} · dialling now")
            } else {
                Triple(LampState.ATTENTION, "Waiting", "At ${clock(step.dueAt)} unless the trip is closed first")
            }
    }

internal fun clock(at: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(at))
