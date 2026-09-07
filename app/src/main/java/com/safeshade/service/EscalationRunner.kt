package com.safeshade.service

import android.content.Context
import com.safeshade.ActionResult
import com.safeshade.alarms.ReminderScheduler
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.SafetySettings
import com.safeshade.data.TripOutcome
import com.safeshade.data.contactsFor
import com.safeshade.data.resolveWearerForDevice
import com.safeshade.device.DeviceLink
import com.safeshade.dialNumber
import com.safeshade.repo.SafetyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Drives [EscalationLadder] against the real clock, the real contacts and the
 * real dialer.
 *
 * ### How a ladder is scheduled
 *
 * One alarm at a time, through [ReminderScheduler] - the same `AlarmManager`
 * plumbing the journey and check-in deadlines use, with its exact-alarm
 * fallback and its `RTC_WAKEUP` semantics. Not a coroutine `delay`: the entire
 * premise of the feature is a phone nobody is looking at, and a delay dies with
 * the process the moment Android reclaims it.
 *
 * The alarm is armed for the next waiting rung's `dueAt`, and the receiver
 * re-arms for the one after it. One at a time rather than three at once because
 * a cancel then has exactly one `PendingIntent` to match, and a cancel that
 * silently fails to match is how an app dials an emergency number for an alert
 * somebody answered ten minutes ago.
 *
 * ### What cancels it
 *
 * Anything that settles the trip. "I'm OK" resolves it to `DISMISSED`, "Call
 * now" to `CONTACTED`, and both go through `SafetyRepository.resolveAlert`,
 * whose write this class observes. On top of that, every fire re-reads the
 * trip from DataStore before acting and stops if it is no longer `PENDING`, so
 * a cancel that failed to land still cannot produce a call.
 *
 * ### Why the trip is left PENDING
 *
 * A dialled rung does not resolve the trip. The gate that lets rung two fire is
 * the trip still being unanswered, so a ladder that recorded `CONTACTED` on its
 * own first dial would be a ladder that could never climb. The run is the
 * record of what was dialled; the outcome stays the human's to give.
 */
class EscalationRunner(
    private val appContext: Context,
    private val prefs: SafeShadePreferences,
    private val safety: SafetyRepository,
    private val link: DeviceLink,
    private val scope: CoroutineScope,
    /**
     * Injected so a test can watch what would have been dialled. Returns
     * whatever the dial reported, which is recorded on the step verbatim.
     */
    private val dial: (number: String, allowDirectCall: Boolean) -> ActionResult =
        { number, allowDirect -> dialNumber(appContext, number, allowDirect) }
) {

    private val _run = MutableStateFlow<EscalationRun?>(null)

    /**
     * The ladder as it stands, for the plate that shows who was called.
     *
     * In memory only. A run is rebuilt from the alert's own timestamp when an
     * alarm wakes a cold process (see [onStepDue]), so nothing is lost by not
     * persisting it - and persisting it would add a second source of truth for
     * a thing already derivable from the trip.
     */
    val run: StateFlow<EscalationRun?> = _run.asStateFlow()

    /**
     * Starts watching for alerts to escalate.
     *
     * Keyed on the alert's identity, like `AlertBridge`: the repository updates
     * the event in place as its outcome changes, and re-planning on every one
     * of those would restart the ladder each time.
     */
    fun start() {
        safety.activeAlert
            .distinctUntilChanged { old, new -> old?.id == new?.id }
            .onEach { alert -> if (alert == null) stop() else arm(alert) }
            .launchIn(scope)
    }

    /**
     * Plans a ladder for [event] and arms its first rung.
     *
     * Public so the alert path can arm one directly rather than waiting for the
     * flow to turn around, and idempotent on the alert id so calling it twice
     * for the same alert cannot produce two ladders.
     */
    suspend fun arm(event: FallAlertEvent) {
        val settings = prefs.safetySettings.first()
        if (!EscalationLadder.shouldStart(event, settings.escalation)) {
            stop()
            return
        }
        if (_run.value?.alertId == event.id) return

        val planned = planFor(event, settings)
        _run.value = planned
        armNext(planned)
    }

    /** Cancels the ladder and its alarm. Safe to call when nothing is running. */
    fun stop() {
        val current = _run.value
        if (current != null) _run.value = EscalationLadder.cancel(current)
        ReminderScheduler.cancelEscalationStep(appContext)
    }

    /**
     * One rung came due.
     *
     * Called from the receiver, in a process this alarm may itself have woken -
     * which is why nothing here reads a repository's `StateFlow.value` and why
     * the run is rebuilt when it is missing. The rebuild is exact: the plan is
     * a pure function of the alert's timestamp and the stored settings.
     *
     * @param stepIndex which rung the alarm was armed for. Re-checked against
     *   the freshly planned ladder rather than trusted: a contact removed
     *   between arming and firing shortens the ladder, and an index that no
     *   longer exists must end the run rather than crash inside `goAsync`.
     */
    suspend fun onStepDue(alertId: String, stepIndex: Int) {
        val event = prefs.fallHistory.first().firstOrNull { it.id == alertId }
        if (event == null) {
            stop()
            return
        }

        // Answered while the alarm was in flight, or the cancel did not match.
        // Either way there is nothing to escalate.
        if (event.outcome != TripOutcome.PENDING) {
            stop()
            return
        }

        val settings = prefs.safetySettings.first()
        if (!settings.escalation.enabled) {
            stop()
            return
        }

        val current = _run.value?.takeIf { it.alertId == alertId } ?: planFor(event, settings)

        if (stepIndex !in current.steps.indices) {
            // The ladder got shorter. Nothing left to climb; keep whatever was
            // already dialled and stop.
            _run.value = EscalationLadder.cancel(current)
            ReminderScheduler.cancelEscalationStep(appContext)
            return
        }

        val step = current.steps[stepIndex]
        if (step.outcome.isSettled) {
            _run.value = current
            armNext(current)
            return
        }

        // A contact may be placed directly when "Call after a fall" allows it.
        // The emergency number is never placed directly by this class: it
        // opens the dialer with the number ready and stops there. A phone
        // making an unattended call to 112 on a timer, with nobody holding it,
        // is the one outcome here that cannot be taken back, and the dialer
        // is one press away for anybody who is.
        val direct = step.target is EscalationTarget.Contact && settings.autoCallEmergency
        val result = dial(step.target.dialTo, direct)
        val outcome = StepOutcome.Dialled(
            at = System.currentTimeMillis(),
            result = describe(result)
        )

        val updated = EscalationLadder.withOutcome(current, stepIndex, outcome)
        _run.value = updated
        armNext(updated)
    }

    /**
     * Whether the countdown's own auto-dial should stand down.
     *
     * The fall countdown and the ladder are two mechanisms that both dial the
     * primary contact, and with the shipped defaults - a thirty-second
     * countdown and a thirty-second first rung - they come due in the same
     * instant. Left alone they race: whichever wins dials, and if the countdown
     * wins it also resolves the trip to `CONTACTED`, which cancels the ladder
     * before rungs two and three exist.
     *
     * So when the ladder is on, it owns every automatic dial and the countdown
     * keeps only the parts the ladder does not do: the expired notification and
     * the SMS fallback. `AlertActionReceiver.handleExpiry` asks this.
     */
    suspend fun ownsAutoDial(): Boolean = prefs.safetySettings.first().escalation.enabled

    // ============================================
    // Internals
    // ============================================

    private suspend fun planFor(event: FallAlertEvent, settings: SafetySettings): EscalationRun {
        // The wearer of the device that raised the alert, not the one on
        // screen - handoff7 section 6 item 2. A fall belongs to whoever was
        // wearing the wearable that reported it.
        val wearer = resolveWearerForDevice(
            wearers = prefs.wearers.first(),
            address = link.deviceAddress.value,
            selectedWearerId = prefs.selectedWearerId.first()
        )
        return EscalationLadder.plan(
            alertId = event.id,
            wearerId = event.wearerId ?: wearer?.id,
            contacts = settings.contactsFor(wearer),
            settings = settings.escalation,
            startedAt = event.timestamp
        )
    }

    private fun armNext(run: EscalationRun) {
        val index = EscalationLadder.nextWaitingIndex(run)
        if (index == null) {
            ReminderScheduler.cancelEscalationStep(appContext)
            return
        }
        ReminderScheduler.scheduleEscalationStep(
            context = appContext,
            alertId = run.alertId,
            stepIndex = index,
            at = run.steps[index].dueAt
        )
    }

    /** The dial result, in the words the plate shows. */
    // "Asked", not "opened": Started means startActivity did not throw. From
    // an alarm receiver with the app in the background, API 33 may refuse the
    // start silently, and nothing here can see that. The plate says what the
    // phone did, which is ask.
    private fun describe(result: ActionResult): String = when (result) {
        is ActionResult.Started -> "Asked the dialer to open"
        is ActionResult.Sent -> "Sent"
        is ActionResult.PermissionMissing -> "Permission not granted: ${result.permission}"
        is ActionResult.Failed -> result.reason
    }
}
