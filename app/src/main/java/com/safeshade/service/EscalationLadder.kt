package com.safeshade.service

import com.safeshade.data.EmergencyContact
import com.safeshade.data.EscalationSettings
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.TripOutcome

/**
 * Who an escalation step calls.
 *
 * Two cases rather than one number, because the two are answered differently:
 * a contact is a person the guardian chose and may be dialled through the
 * dialer or placed directly, while the emergency number is the end of the
 * ladder and is never reached at all unless somebody was reachable to skip
 * first. Keeping them apart is what lets the plate say "we called Meera, then
 * Arun, then 112" instead of listing three anonymous numbers.
 */
sealed interface EscalationTarget {
    data class Contact(val name: String, val phone: String) : EscalationTarget
    data class Emergency(val number: String) : EscalationTarget

    /**
     * The number actually dialled, for either case.
     *
     * Named `dialTo` rather than `number` because [Emergency] already declares
     * a `number` of its own, and a member here by that name would collide with
     * it - the sort of accidental override that reads as a typo for years.
     */
    val dialTo: String
        get() = when (this) {
            is Contact -> phone
            is Emergency -> number
        }

    /** What the plate calls this rung. */
    val label: String
        get() = when (this) {
            is Contact -> name.ifBlank { phone }
            is Emergency -> number
        }
}

/**
 * What happened at one rung.
 *
 * There is deliberately no "Reached" case. Nothing on this phone can observe
 * that somebody picked up - `ACTION_CALL` returns as soon as the dialer starts,
 * and `ACTION_DIAL` returns before a digit is sent - so a state claiming a
 * person was reached would be the app inventing the one fact a guardian most
 * wants to be true. [Dialled] carries the dial result verbatim instead, which
 * is exactly as much as is known.
 */
sealed interface StepOutcome {

    /** Not yet due, or due and not yet acted on. */
    data object Waiting : StepOutcome

    /**
     * @param result whatever `dialNumber` reported, verbatim. "Started" means
     *   a dialer opened, not that a call connected.
     */
    data class Dialled(val at: Long, val result: String) : StepOutcome

    /** @param reason user-facing, e.g. "No emergency contact stored". */
    data class Skipped(val reason: String) : StepOutcome

    /** The alert was answered before this rung came due. */
    data object Cancelled : StepOutcome

    val isSettled: Boolean get() = this !is Waiting
}

/** One rung: who, when, and what came of it. */
data class EscalationStep(
    val target: EscalationTarget,
    val dueAt: Long,
    val outcome: StepOutcome = StepOutcome.Waiting
)

/**
 * One alert's ladder.
 *
 * The run is the record of what the app did about an unanswered alert, and it
 * is deliberately **not** the alert's outcome. The trip stays `PENDING` while
 * the ladder climbs: the gate that lets rung two fire is the trip still being
 * unanswered, so a ladder that resolved the trip on its own first dial would
 * be a ladder that could never reach rung two.
 */
data class EscalationRun(
    val alertId: String,
    val wearerId: String?,
    val startedAt: Long,
    val steps: List<EscalationStep>
) {
    val isFinished: Boolean get() = steps.all { it.outcome.isSettled }

    /** The rungs that have actually been dialled, in order, for the plate. */
    val dialled: List<EscalationStep> get() = steps.filter { it.outcome is StepOutcome.Dialled }
}

/**
 * The unanswered-alert ladder, as a pure state machine.
 *
 * Every function here is a total function of its arguments and the clock value
 * it is handed. That is what makes the whole feature testable without a device,
 * and it is also what makes it survivable: the runner rebuilds a run from the
 * alert's own timestamp after the process has been killed mid-ladder, and
 * because [plan] is deterministic, the rebuilt run is the same run - same
 * rungs, same deadlines - rather than a second ladder starting from zero.
 *
 * The one rule that is not obvious from the types: **a device with no
 * emergency contact never dials anything, including the emergency number.** An
 * app that rang 112 on behalf of somebody who had not named a single person to
 * call would be making an emergency call with no name, no history and nobody
 * expecting it, which is worse than silence and is how a safety product loses
 * the right to make calls at all.
 */
object EscalationLadder {

    /** The reason recorded when there is nobody to call. Quoted in the UI. */
    const val NO_CONTACTS = "No emergency contact stored"

    /**
     * Whether an alert should start a ladder at all.
     *
     * Three conditions, all necessary. The feature must be on; the alert must
     * still be unanswered; and nobody must already have been contacted about
     * it - a trip logged with `wasEmergencyContacted` set has had its call
     * placed by the countdown, and climbing a second ladder over the top of it
     * would dial the same person twice.
     */
    fun shouldStart(event: FallAlertEvent, settings: EscalationSettings): Boolean =
        settings.enabled &&
            event.outcome == TripOutcome.PENDING &&
            !event.wasEmergencyContacted

    /**
     * Builds the rungs for one alert.
     *
     * Deterministic in [startedAt], which is the alert's own timestamp - never
     * "now" - so that planning the same alert twice, in two processes, hours
     * apart, produces the identical ladder.
     *
     * With contacts present the shape is: contact one after `firstDelaySec`,
     * contact two `secondDelaySec` after that, then the emergency number
     * another `secondDelaySec` later if the settings allow it. A phone with
     * only one contact stored still reaches the emergency rung, at the time
     * contact two would have been tried plus that same gap; skipping the gap
     * would make a one-contact install ring 112 sooner than a two-contact one,
     * which is backwards.
     */
    fun plan(
        alertId: String,
        wearerId: String?,
        contacts: List<EmergencyContact>,
        settings: EscalationSettings,
        startedAt: Long
    ): EscalationRun {
        val usable = contacts.filter { it.phone.isNotBlank() }

        if (usable.isEmpty()) {
            // One settled rung, so the run is finished on arrival and the plate
            // can say why nothing happened rather than showing an empty ladder.
            return EscalationRun(
                alertId = alertId,
                wearerId = wearerId,
                startedAt = startedAt,
                steps = listOf(
                    EscalationStep(
                        target = EscalationTarget.Emergency(settings.emergencyNumber),
                        dueAt = startedAt,
                        outcome = StepOutcome.Skipped(NO_CONTACTS)
                    )
                )
            )
        }

        val first = startedAt + settings.firstDelaySec * 1_000L
        val second = first + settings.secondDelaySec * 1_000L
        val steps = mutableListOf<EscalationStep>()

        steps += EscalationStep(
            target = EscalationTarget.Contact(usable[0].name, usable[0].phone),
            dueAt = first
        )
        usable.getOrNull(1)?.let {
            steps += EscalationStep(
                target = EscalationTarget.Contact(it.name, it.phone),
                dueAt = second
            )
        }
        if (settings.thenEmergency) {
            steps += EscalationStep(
                target = EscalationTarget.Emergency(settings.emergencyNumber),
                dueAt = second + settings.secondDelaySec * 1_000L
            )
        }

        return EscalationRun(alertId, wearerId, startedAt, steps)
    }

    /** The index of the first rung that is due and unsettled, or null. */
    fun dueIndex(run: EscalationRun, now: Long): Int? =
        run.steps.indexOfFirst { !it.outcome.isSettled && it.dueAt <= now }
            .takeIf { it >= 0 }

    /** The next rung still waiting, whether or not it is due. Null when finished. */
    fun nextWaiting(run: EscalationRun): EscalationStep? =
        run.steps.firstOrNull { !it.outcome.isSettled }

    /** The index of [nextWaiting], or null. */
    fun nextWaitingIndex(run: EscalationRun): Int? =
        run.steps.indexOfFirst { !it.outcome.isSettled }.takeIf { it >= 0 }

    /** Records what happened at one rung. Out-of-range indices are ignored. */
    fun withOutcome(run: EscalationRun, index: Int, outcome: StepOutcome): EscalationRun {
        if (index !in run.steps.indices) return run
        return run.copy(
            steps = run.steps.mapIndexed { i, step ->
                if (i == index) step.copy(outcome = outcome) else step
            }
        )
    }

    /**
     * Stops the ladder: every rung still waiting becomes [StepOutcome.Cancelled].
     *
     * Rungs already dialled keep their outcome. The plate's whole job is to
     * show what the app did before the person answered, and rewriting a placed
     * call as cancelled would erase the one part of the record somebody may
     * need to explain later.
     */
    fun cancel(run: EscalationRun): EscalationRun = run.copy(
        steps = run.steps.map {
            if (it.outcome.isSettled) it else it.copy(outcome = StepOutcome.Cancelled)
        }
    )
}
