package com.safeshade.data

/**
 * What happened when a phone SOS fired.
 *
 * Reported per contact rather than as a single boolean, because "sent" is not
 * one fact. `SmsManager` can succeed for one number and fail for another on the
 * same send, and a person who has just raised an alarm is entitled to know
 * *who* was actually reached rather than being told it worked.
 */
sealed interface SosOutcome {

    /** Fired with nobody to send to. Should be unreachable — the control is disarmed. */
    data object NoContact : SosOutcome

    data class Sent(
        val reached: List<String>,
        val failed: List<String>,
        /** The wearable was told too, so it buzzes and shows the alert. */
        val onDevice: Boolean,
        /** Whether coordinates went with the message, or only the words. */
        val hasLocation: Boolean
    ) : SosOutcome {
        val anyReached: Boolean get() = reached.isNotEmpty()
    }
}

/**
 * Why the SOS cannot be armed.
 *
 * Checked when the finger goes down, never at the end of the hold. Spending
 * five seconds to be told there was nobody to call is the wrong failure, and
 * each of these has a fix the UI can offer as a single action.
 */
enum class SosBlocker {
    NO_CONTACT,
    NO_SMS_PERMISSION,
    ALERT_ALREADY_LIVE,

    /** Persisted state has not loaded. Momentary, and not worth explaining. */
    NOT_READY
}
