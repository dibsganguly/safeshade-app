package com.safeshade.repo

import com.safeshade.data.FallAlertEvent
import com.safeshade.data.Journey
import com.safeshade.data.JourneyState
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.TripKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The "walk with me" state machine.
 *
 * ```
 *            start()                arrive()
 *   Idle ─────────────► Active ────────────────► Arrived ──► Idle
 *    ▲                    │                                    ▲
 *    │                    │ eta + grace elapses                │
 *    │                    ▼                                    │
 *    └──── clear() ──── Escalated ─────────────────────────────┘
 * ```
 *
 * `Idle` is represented by a null [journey] rather than by a fourth enum value,
 * because "there is no journey" and "there is a journey that is over" are
 * genuinely different things to the UI: the second one still has a label and a
 * duration worth showing until the user dismisses it.
 *
 * The ticker runs on the **application** scope. A journey escalates precisely
 * when the user is not looking at their phone — that is the entire premise of
 * the feature — so a `viewModelScope` here would mean the timer only runs while
 * someone is watching it, which is the one case where it is not needed.
 */
class JourneyRepository(
    private val prefs: SafeShadePreferences,
    private val safety: SafetyRepository,
    private val scope: CoroutineScope
) {

    /** Null means Idle. Null also means "not loaded yet" on the very first tick. */
    val journey: StateFlow<Journey?> =
        prefs.journey.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Whether the first DataStore read has happened.
     *
     * [journey] cannot carry this itself: null is a legitimate value for it,
     * so the loaded/unloaded distinction needs its own flow or the Loading gate
     * downstream cannot tell "no journey" from "not read yet" — and would show
     * a Start button for a frame while a journey was actually running.
     */
    val loaded: StateFlow<Boolean> =
        prefs.journey.map { true }.stateIn(scope, SharingStarted.Eagerly, false)

    private val lock = Mutex()

    init {
        scope.launch {
            while (isActive) {
                delay(TICK_MS)
                checkOverdue()
            }
        }
    }

    /**
     * Begins a journey. Replaces any existing one.
     *
     * Deliberately one at a time: two concurrent journeys mean two competing
     * escalation deadlines, and a guardian receiving an overdue alert would
     * have no way to tell which walk it belongs to.
     */
    suspend fun start(
        label: String,
        etaAt: Long,
        graceSeconds: Int = 60,
        destinationLat: Double? = null,
        destinationLon: Double? = null
    ): Journey {
        val journey = Journey(
            label = label,
            etaAt = etaAt,
            graceSeconds = graceSeconds.coerceAtLeast(0),
            destinationLat = destinationLat,
            destinationLon = destinationLon,
            state = JourneyState.ACTIVE
        )
        lock.withLock { prefs.setJourney(journey) }
        return journey
    }

    /** The wearer arrived safely. */
    suspend fun arrive() = transition(JourneyState.ARRIVED)

    /** The user called it off. No trip is logged; nothing went wrong. */
    suspend fun cancel() = transition(JourneyState.CANCELLED)

    /** Dismisses a finished journey and returns to Idle. */
    suspend fun clear() {
        lock.withLock { prefs.setJourney(null) }
    }

    private suspend fun transition(state: JourneyState) {
        lock.withLock {
            val current = prefs.journey.first() ?: return@withLock
            if (current.state != JourneyState.ACTIVE) return@withLock
            prefs.setJourney(current.copy(state = state))
        }
    }

    /**
     * Escalates a journey whose ETA plus grace period has passed.
     *
     * The grace period is honoured rather than escalating on the ETA itself,
     * because an ETA is an estimate a user typed while distracted and a
     * five-minute overrun is normal. Escalating on the dot would train
     * guardians to ignore the alert, which costs more than the delay does.
     */
    private suspend fun checkOverdue() {
        val escalated = lock.withLock {
            val current = prefs.journey.first() ?: return@withLock null
            if (current.state != JourneyState.ACTIVE) return@withLock null
            val deadline = current.etaAt + current.graceSeconds * 1_000L
            if (System.currentTimeMillis() < deadline) return@withLock null
            val updated = current.copy(state = JourneyState.ESCALATED)
            prefs.setJourney(updated)
            updated
        } ?: return

        // Logged outside the journey lock: SafetyRepository takes its own, and
        // keeping the two acquisitions in a fixed order in one place is the
        // cheapest way to be sure nothing inverts it elsewhere.
        safety.record(
            FallAlertEvent(
                kind = TripKind.JOURNEY_OVERDUE,
                note = escalated.label.ifBlank { "Journey overdue" }
            )
        )
    }

    private companion object {
        const val TICK_MS = 30_000L
    }
}
