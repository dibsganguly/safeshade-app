package com.safeshade.service

import com.safeshade.data.EmergencyContact
import com.safeshade.data.EscalationSettings
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.TripOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EscalationLadderTest {

    private fun settings(
        enabled: Boolean = true,
        firstDelaySec: Int = 30,
        secondDelaySec: Int = 60,
        thenEmergency: Boolean = true,
        emergencyNumber: String = "112"
    ) = EscalationSettings(
        enabled = enabled,
        firstDelaySec = firstDelaySec,
        secondDelaySec = secondDelaySec,
        thenEmergency = thenEmergency,
        emergencyNumber = emergencyNumber
    )

    private fun contact(name: String, phone: String) = EmergencyContact(name = name, phone = phone)

    private fun event(
        outcome: TripOutcome = TripOutcome.PENDING,
        wasEmergencyContacted: Boolean = false
    ) = FallAlertEvent(outcome = outcome, wasEmergencyContacted = wasEmergencyContacted)

    // --- shouldStart ---

    @Test
    fun shouldStart_false_when_disabled() {
        assertFalse(EscalationLadder.shouldStart(event(), settings(enabled = false)))
    }

    @Test
    fun shouldStart_false_when_not_pending() {
        assertFalse(EscalationLadder.shouldStart(event(outcome = TripOutcome.DISMISSED), settings()))
    }

    @Test
    fun shouldStart_false_when_already_contacted() {
        assertFalse(EscalationLadder.shouldStart(event(wasEmergencyContacted = true), settings()))
    }

    @Test
    fun shouldStart_true_when_all_conditions_met() {
        assertTrue(EscalationLadder.shouldStart(event(), settings()))
    }

    // --- plan: no usable contacts ---

    @Test
    fun plan_with_no_usable_contacts_yields_one_skipped_emergency_rung() {
        val run = EscalationLadder.plan(
            alertId = "a1",
            wearerId = "w1",
            contacts = emptyList(),
            settings = settings(),
            startedAt = 1_000L
        )

        assertEquals(1, run.steps.size)
        val step = run.steps.single()
        assertTrue(step.target is EscalationTarget.Emergency)
        assertEquals(1_000L, step.dueAt)
        assertEquals(StepOutcome.Skipped(EscalationLadder.NO_CONTACTS), step.outcome)
        assertTrue(run.isFinished)
    }

    @Test
    fun plan_ignores_contacts_with_blank_phone() {
        val run = EscalationLadder.plan(
            alertId = "a1",
            wearerId = null,
            contacts = listOf(contact("Blank", "")),
            settings = settings(),
            startedAt = 1_000L
        )

        assertEquals(1, run.steps.size)
        assertEquals(StepOutcome.Skipped(EscalationLadder.NO_CONTACTS), run.steps.single().outcome)
    }

    // --- plan: contacts present ---

    @Test
    fun plan_with_two_contacts_and_then_emergency_gives_three_rungs() {
        val startedAt = 10_000L
        val s = settings(firstDelaySec = 30, secondDelaySec = 60, thenEmergency = true)
        val run = EscalationLadder.plan(
            alertId = "a1",
            wearerId = "w1",
            contacts = listOf(contact("Meera", "1"), contact("Arun", "2")),
            settings = s,
            startedAt = startedAt
        )

        val first = startedAt + 30_000L
        val second = first + 60_000L
        val third = second + 60_000L

        assertEquals(3, run.steps.size)
        assertEquals(first, run.steps[0].dueAt)
        assertEquals(second, run.steps[1].dueAt)
        assertEquals(third, run.steps[2].dueAt)
        assertTrue(run.steps[0].target is EscalationTarget.Contact)
        assertTrue(run.steps[1].target is EscalationTarget.Contact)
        assertTrue(run.steps[2].target is EscalationTarget.Emergency)
    }

    @Test
    fun plan_with_one_contact_still_puts_emergency_at_first_plus_twice_second() {
        val startedAt = 10_000L
        val s = settings(firstDelaySec = 30, secondDelaySec = 60, thenEmergency = true)
        val run = EscalationLadder.plan(
            alertId = "a1",
            wearerId = "w1",
            contacts = listOf(contact("Meera", "1")),
            settings = s,
            startedAt = startedAt
        )

        val first = startedAt + 30_000L
        val emergencyDue = first + 60_000L + 60_000L // not "first + second", the full second gap again

        assertEquals(2, run.steps.size)
        assertEquals(first, run.steps[0].dueAt)
        assertTrue(run.steps[0].target is EscalationTarget.Contact)
        assertEquals(emergencyDue, run.steps[1].dueAt)
        assertTrue(run.steps[1].target is EscalationTarget.Emergency)
    }

    @Test
    fun plan_with_then_emergency_false_has_no_emergency_rung() {
        val run = EscalationLadder.plan(
            alertId = "a1",
            wearerId = "w1",
            contacts = listOf(contact("Meera", "1"), contact("Arun", "2")),
            settings = settings(thenEmergency = false),
            startedAt = 10_000L
        )

        assertEquals(2, run.steps.size)
        assertTrue(run.steps.none { it.target is EscalationTarget.Emergency })
    }

    @Test
    fun plan_is_deterministic() {
        val contacts = listOf(contact("Meera", "1"), contact("Arun", "2"))
        val s = settings()
        val run1 = EscalationLadder.plan("a1", "w1", contacts, s, 10_000L)
        val run2 = EscalationLadder.plan("a1", "w1", contacts, s, 10_000L)

        assertEquals(run1, run2)
    }

    // --- dueIndex ---

    @Test
    fun dueIndex_null_before_first_due() {
        val run = EscalationLadder.plan("a1", "w1", listOf(contact("M", "1")), settings(), 1_000L)
        assertNull(EscalationLadder.dueIndex(run, now = run.steps[0].dueAt - 1))
    }

    @Test
    fun dueIndex_zero_at_exactly_due_at() {
        val run = EscalationLadder.plan("a1", "w1", listOf(contact("M", "1")), settings(), 1_000L)
        assertEquals(0, EscalationLadder.dueIndex(run, now = run.steps[0].dueAt))
    }

    @Test
    fun dueIndex_skips_settled_rungs() {
        val run0 = EscalationLadder.plan(
            "a1", "w1",
            listOf(contact("M", "1"), contact("A", "2")),
            settings(),
            1_000L
        )
        val run = EscalationLadder.withOutcome(run0, 0, StepOutcome.Dialled(at = 1L, result = "started"))

        // now is at/after both rungs' dueAt, but rung 0 is already settled.
        assertEquals(1, EscalationLadder.dueIndex(run, now = run.steps[1].dueAt))
    }

    // --- withOutcome ---

    @Test
    fun withOutcome_ignores_out_of_range_index() {
        val run = EscalationLadder.plan("a1", "w1", listOf(contact("M", "1")), settings(), 1_000L)

        val unchangedNegative = EscalationLadder.withOutcome(run, -1, StepOutcome.Cancelled)
        val unchangedTooLarge = EscalationLadder.withOutcome(run, run.steps.size, StepOutcome.Cancelled)

        assertEquals(run, unchangedNegative)
        assertEquals(run, unchangedTooLarge)
    }

    // --- cancel / dialled ---

    @Test
    fun cancel_turns_only_waiting_rungs_into_cancelled_and_keeps_dialled() {
        val run0 = EscalationLadder.plan(
            "a1", "w1",
            listOf(contact("M", "1"), contact("A", "2")),
            settings(thenEmergency = true),
            1_000L
        )
        val dialled = StepOutcome.Dialled(at = 5L, result = "started")
        val run = EscalationLadder.withOutcome(run0, 0, dialled)

        val cancelled = EscalationLadder.cancel(run)

        assertEquals(dialled, cancelled.steps[0].outcome)
        assertEquals(StepOutcome.Cancelled, cancelled.steps[1].outcome)
        assertEquals(StepOutcome.Cancelled, cancelled.steps[2].outcome)
    }

    @Test
    fun dialled_lists_only_dialled_steps_in_order() {
        val run0 = EscalationLadder.plan(
            "a1", "w1",
            listOf(contact("M", "1"), contact("A", "2")),
            settings(thenEmergency = true),
            1_000L
        )
        val d0 = StepOutcome.Dialled(at = 5L, result = "started-1")
        val d2 = StepOutcome.Dialled(at = 9L, result = "started-2")
        val run = EscalationLadder.withOutcome(
            EscalationLadder.withOutcome(run0, 0, d0),
            2,
            d2
        )

        assertEquals(listOf(d0, d2), run.dialled.map { it.outcome })
    }
}
