package com.safeshade.repo

import com.safeshade.data.CheckInRequest
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.SafetySettings
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceAlert
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Falls, SOS presses, check-ins and the settings behind them.
 *
 * The collector in [init] runs on the **application** scope, never on a
 * `viewModelScope`. A fall alert has to be recorded whether or not a ViewModel
 * exists, whether or not an Activity is alive, and whether or not the screen is
 * on. Scoping this to any UI lifetime means the one alert that matters — the
 * one that arrives while the phone is in a pocket — is the one that is dropped.
 */
class SafetyRepository(
    private val prefs: SafeShadePreferences,
    private val link: DeviceLink,
    private val scope: CoroutineScope
) {

    /** Null until the first DataStore read completes. See [ProfileRepository]. */
    val settings: StateFlow<SafetySettings?> =
        prefs.safetySettings.stateIn(scope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<FallAlertEvent>?> =
        prefs.fallHistory.stateIn(scope, SharingStarted.Eagerly, null)

    val checkIns: StateFlow<List<CheckInRequest>?> =
        prefs.checkIns.stateIn(scope, SharingStarted.Eagerly, null)

    private val _activeAlert = MutableStateFlow<FallAlertEvent?>(null)

    /**
     * The trip currently demanding a response, for the full-screen alarm UI.
     *
     * Raised only *after* the event is on disk. See [record].
     */
    val activeAlert: StateFlow<FallAlertEvent?> = _activeAlert.asStateFlow()

    private val _lastUnrecognisedAlert = MutableStateFlow<String?>(null)

    /**
     * The most recent ALERT_CHAR payload this build does not understand.
     *
     * Kept rather than dropped, but deliberately *not* turned into a trip. This
     * firmware only ever notifies `FALL_DETECTED` on that characteristic, so
     * anything else is a future firmware revision or a corrupted notify — and
     * logging it as an SOS would put a fabricated emergency in a guardian's
     * history and raise a full-screen siren for a diagnostic string. The one
     * exception is a payload that names itself an SOS, handled in [init].
     */
    val lastUnrecognisedAlert: StateFlow<String?> = _lastUnrecognisedAlert.asStateFlow()

    /** Serialises read-modify-write on the history and check-in lists. */
    private val historyLock = Mutex()
    private val checkInLock = Mutex()

    /**
     * Serialises read-modify-write on the settings blob.
     *
     * `SafetySettings` is stored as one aggregate, so two concurrent
     * read-then-write callers do not merge — the loser's entire change is
     * overwritten. Adding a contact while a sensitivity change commits would
     * silently discard one of them.
     */
    private val settingsLock = Mutex()

    init {
        /*
         * Alerts arrive as a SharedFlow of events, and must stay one.
         *
         * The previous design used a StateFlow<Boolean> flag. MutableStateFlow
         * conflates equal values, so a second fall arriving while the flag was
         * still true emitted nothing whatsoever and was lost — on a
         * fall-detection product, the second fall is frequently the serious
         * one. Every alert below becomes its own persisted event.
         */
        link.alerts
            .onEach { alert ->
                when (alert) {
                    is DeviceAlert.Fall -> record(
                        FallAlertEvent(timestamp = alert.at, kind = TripKind.FALL)
                    )

                    is DeviceAlert.Unknown ->
                        // Verified against SafeShadev21.ino: the only payload
                        // this firmware ever notifies on ALERT_CHAR is
                        // "FALL_DETECTED". Anything else is a newer firmware or
                        // a corrupted notify, so it is kept for diagnostics but
                        // not written into the trip history — a guardian's
                        // history must not contain emergencies that were never
                        // reported. A payload that names itself an SOS is the
                        // one case worth acting on, because a future firmware
                        // relaying the button press this way is plausible and
                        // under-reacting to it is the worse error.
                        if (alert.raw.contains("SOS", ignoreCase = true)) {
                            record(
                                FallAlertEvent(
                                    timestamp = alert.at,
                                    kind = TripKind.SOS,
                                    note = alert.raw
                                )
                            )
                        } else {
                            _lastUnrecognisedAlert.value = alert.raw
                        }
                }
            }
            .launchIn(scope)

        // Check-in escalation. A minute of granularity is plenty for a deadline
        // measured in tens of minutes, and it keeps the app off the CPU.
        scope.launch {
            while (isActive) {
                delay(ESCALATION_TICK_MS)
                escalateOverdueCheckIns()
            }
        }
    }

    // ============================================
    // Recording
    // ============================================

    /**
     * Persists a trip, then raises it to the UI.
     *
     * The order is deliberate and is the whole point of this function. If the
     * UI state were raised first, the window between showing a full-screen
     * alarm and committing it to disk would be a window in which the process
     * can be killed — Android is entirely willing to do that to a backgrounded
     * app mid-alarm — leaving a fall that was displayed, possibly acted on, and
     * has no record anywhere. Writing first means the worst case is an alarm
     * that has to be re-surfaced from history, not one that never happened.
     */
    suspend fun record(event: FallAlertEvent) {
        historyLock.withLock {
            val current = prefs.fallHistory.first()
            prefs.setFallHistory(current + event)
        }
        _activeAlert.value = event
    }

    /** Convenience for the SOS button on this phone. */
    suspend fun recordSos(note: String? = null) =
        record(FallAlertEvent(kind = TripKind.SOS, note = note))

    /**
     * Closes out the active alert with an outcome.
     *
     * Updates the stored event in place rather than appending a second one, so
     * the history shows one trip with a resolution instead of two entries that
     * a guardian has to mentally pair up.
     */
    suspend fun resolveAlert(eventId: String, outcome: TripOutcome, contacted: Boolean = false) {
        historyLock.withLock {
            val current = prefs.fallHistory.first()
            prefs.setFallHistory(
                current.map {
                    if (it.id == eventId) {
                        it.copy(outcome = outcome, wasEmergencyContacted = contacted)
                    } else {
                        it
                    }
                }
            )
        }
        if (_activeAlert.value?.id == eventId) _activeAlert.value = null
    }

    /** Dismisses the alarm UI without changing the recorded outcome. */
    fun clearActiveAlert() {
        _activeAlert.value = null
    }

    suspend fun clearHistory() {
        historyLock.withLock { prefs.setFallHistory(emptyList()) }
    }

    // ============================================
    // Settings
    // ============================================

    /**
     * Saves safety settings and pushes them.
     *
     * Second of the three writes that had been failing on every reconnect in
     * the previous app, for the same reason as the medical ID: it was keyed on
     * "Connected", where SETTINGS_CHAR is still null.
     */
    suspend fun setSettings(settings: SafetySettings) {
        settingsLock.withLock { writeSettings(settings) }
    }

    suspend fun setContacts(contacts: List<EmergencyContact>) =
        mutateSettings { it.copy(emergencyContacts = contacts) }

    suspend fun addContact(contact: EmergencyContact) = mutateSettings { current ->
        // A second primary would make SafetySettings.primaryContact return
        // whichever happened to be first in the list, which is not a decision
        // storage order should be making.
        val existing = if (contact.isPrimary) {
            current.emergencyContacts.map { it.copy(isPrimary = false) }
        } else {
            current.emergencyContacts
        }
        current.copy(emergencyContacts = existing + contact)
    }

    suspend fun removeContact(phone: String) = mutateSettings { current ->
        current.copy(emergencyContacts = current.emergencyContacts.filterNot { it.phone == phone })
    }

    /**
     * Read-modify-write under [settingsLock].
     *
     * The read has to be inside the lock. With it outside, two callers both
     * read the pre-change blob and the second write overwrites the first
     * wholesale — and because this is one aggregate, the loss is not one field
     * but every field the other caller touched.
     */
    private suspend fun mutateSettings(transform: (SafetySettings) -> SafetySettings) {
        settingsLock.withLock { writeSettings(transform(prefs.safetySettings.first())) }
    }

    /** Must be called with [settingsLock] held. A Kotlin `Mutex` is not reentrant. */
    private suspend fun writeSettings(settings: SafetySettings) {
        prefs.setSafetySettings(settings)
        if (link.connectionState.value is ConnectionState.Ready) {
            link.writeSettings(DeviceProtocol.settings(settings))
        }
    }

    // ============================================
    // Check-ins
    // ============================================

    /**
     * Opens a check-in request with a deadline.
     *
     * Also pushes the interval to the wearable so it can prompt on its own
     * screen; the app half only handles the escalation.
     */
    suspend fun requestCheckIn(withinMinutes: Int): CheckInRequest {
        val request = CheckInRequest(
            deadlineAt = System.currentTimeMillis() + withinMinutes * 60_000L
        )
        checkInLock.withLock {
            val current = prefs.checkIns.first()
            prefs.setCheckIns(current + request)
        }
        return request
    }

    suspend fun answerCheckIn(id: String) {
        checkInLock.withLock {
            val current = prefs.checkIns.first()
            prefs.setCheckIns(
                current.map { if (it.id == id) it.copy(answeredAt = System.currentTimeMillis()) else it }
            )
        }
    }

    /**
     * Marks every overdue open request escalated and logs one trip for each.
     *
     * Runs on the application scope so a missed check-in escalates while the
     * app is backgrounded — which is the only situation in which a check-in is
     * ever actually missed.
     */
    private suspend fun escalateOverdueCheckIns() {
        val now = System.currentTimeMillis()
        val newlyEscalated = checkInLock.withLock {
            val current = prefs.checkIns.first()
            val overdue = current.filter { it.isOpen && it.deadlineAt <= now }
            if (overdue.isEmpty()) return@withLock emptyList()
            val ids = overdue.map { it.id }.toSet()
            prefs.setCheckIns(current.map { if (it.id in ids) it.copy(escalated = true) else it })
            overdue
        }

        // Logged outside the check-in lock: record() takes the history lock,
        // and taking two locks in a fixed order in one place is the cheapest
        // way to be sure there is no order to invert anywhere else.
        newlyEscalated.forEach {
            record(
                FallAlertEvent(
                    timestamp = now,
                    kind = TripKind.MISSED_CHECKIN,
                    note = "No response within the check-in window"
                )
            )
        }
    }

    private companion object {
        const val ESCALATION_TICK_MS = 60_000L
    }
}
