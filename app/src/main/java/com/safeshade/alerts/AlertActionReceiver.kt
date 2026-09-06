package com.safeshade.alerts

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.safeshade.SafeShadeApplication
import com.safeshade.alarms.ReminderScheduler
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.SafetySettings
import com.safeshade.data.TripOutcome
import com.safeshade.data.contactsFor
import com.safeshade.data.resolveWearerForDevice
import com.safeshade.emergencyAlertText
import com.safeshade.placeEmergencyCall
import com.safeshade.platform.PhoneNumbers
import com.safeshade.sendEmergencySms
import com.safeshade.service.LastKnownLocation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The two buttons on a fall alert, plus the moment its countdown runs out.
 *
 * ### Why a receiver and not an Activity
 *
 * "I'm OK" has to work from the lock screen, from the shade, without unlocking
 * and without the app being resident. A notification action that starts an
 * Activity makes the user unlock first, which is a poor thing to demand of
 * somebody who has just fallen over — and if the full-screen intent was
 * downgraded (see [AlertNotifier]) there may be no Activity to return to at
 * all.
 *
 * ### Why `goAsync`
 *
 * `onReceive` runs on the main thread and the process may be killed the moment
 * it returns. Resolving a trip is a DataStore write, which is asynchronous, so
 * without [goAsync] the write races the process teardown and the outcome is
 * lost — the user taps "I'm OK", the notification vanishes, and the trip stays
 * PENDING in their history forever. `goAsync` holds the process up (with a
 * ~10 second budget, far more than a preferences write needs) until
 * `finish()` is called.
 *
 * ### Why every branch re-reads DataStore
 *
 * A `PendingIntent` is cancelled only by an *equal* one — same request code,
 * same component, same action; extras are not part of that equality. Any
 * mismatch anywhere means a cancel silently fails and the alarm still fires.
 * So no branch here trusts that it should not have run: each re-reads the trip
 * from disk and does nothing if it has already been resolved. That also makes
 * a double tap harmless.
 *
 * Note that these reads go to `container.preferences`, never to a repository's
 * `StateFlow.value`. Those flows are `stateIn(..., Eagerly, null)`, and
 * `Eagerly` starts the collection but cannot make the first file read
 * instantaneous — in a process woken from cold by this very broadcast, `.value`
 * is still `null`. `preferences.<flow>.first()` suspends until DataStore
 * answers, which is the only correct read in a receiver.
 */
class AlertActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val app = context.applicationContext as? SafeShadeApplication ?: return
        val pending = goAsync()

        app.applicationScope.launch {
            try {
                when (action) {
                    ACTION_IM_OK -> handleImOk(app, eventId)
                    ACTION_CALL_NOW -> handleCallNow(app, eventId)
                    ACTION_ALERT_EXPIRED -> handleExpiry(app, eventId)
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Alert action $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    // ============================================
    // Who this alert is about, and who it reaches
    // ============================================

    /**
     * Everyone this alert should reach.
     *
     * The global emergency contacts, unioned with anything recorded against the
     * wearer this alert is about. The wearer is resolved from the **connected
     * BLE address**, because a receiver woken by a fall is being woken by a
     * particular wearable: in a two-wearable household the person who fell is
     * whoever is wearing the device that reported it, not whoever's page
     * happened to be on screen. Resolution falls back to the selected wearer
     * and then to the first, so a single-wearer install is unaffected.
     *
     * Every read goes to `container.preferences`, never to a repository's
     * `StateFlow.value` - see the class comment. In a process woken cold by
     * this very broadcast those flows are still null.
     */
    private suspend fun contactsForThisAlert(
        app: SafeShadeApplication,
        settings: SafetySettings
    ): List<EmergencyContact> {
        val prefs = app.container.preferences
        val wearer = resolveWearerForDevice(
            wearers = prefs.wearers.first(),
            address = app.container.link.deviceAddress.value,
            selectedWearerId = prefs.selectedWearerId.first()
        )
        return settings.contactsFor(wearer)
    }

    /**
     * The one number to dial from a merged list.
     *
     * `SafetySettings.primaryContact` cannot be used once per-wearer contacts
     * exist: it reads the global list only, so a wearer whose own contact is
     * flagged primary would still have the global list's first entry called.
     */
    private fun primaryOf(contacts: List<EmergencyContact>): EmergencyContact? =
        contacts.firstOrNull { it.isPrimary } ?: contacts.firstOrNull()

    // ============================================
    // Actions
    // ============================================

    private suspend fun handleImOk(app: SafeShadeApplication, eventId: String) {
        // Disarm first. If the process dies between here and the write, the
        // worst case is an unresolved trip in the history — not an ambulance.
        ReminderScheduler.cancelAlertExpiry(app)
        dismissNotification(app)

        if (eventId == AlertNotifier.TEST_ALERT_ID) return
        app.container.safetyRepository.resolveAlert(eventId, TripOutcome.DISMISSED)
    }

    private suspend fun handleCallNow(app: SafeShadeApplication, eventId: String) {
        ReminderScheduler.cancelAlertExpiry(app)
        dismissNotification(app)

        val settings = app.container.preferences.safetySettings.first()
        val contact = primaryOf(contactsForThisAlert(app, settings))

        if (eventId != AlertNotifier.TEST_ALERT_ID) {
            app.container.safetyRepository.resolveAlert(
                eventId = eventId,
                outcome = TripOutcome.CONTACTED,
                contacted = true
            )
        }

        if (contact == null) {
            Log.w(TAG, "Call now pressed with no emergency contact configured")
            return
        }
        if (eventId == AlertNotifier.TEST_ALERT_ID) return

        /*
         * Starting the dialer from a receiver is a background activity start,
         * which API 10+ restricts. A broadcast delivered because the user
         * physically tapped a notification action is on the platform's
         * exemption list for exactly this reason, and the intent carries
         * FLAG_ACTIVITY_NEW_TASK (set inside placeEmergencyCall). If CALL_PHONE
         * is not granted that call degrades to ACTION_DIAL with the number
         * pre-filled, which is the right failure: one tap short, rather than
         * nothing.
         */
        placeEmergencyCall(app, contact)
    }

    /**
     * The countdown reached zero.
     *
     * This — not the chronometer — is what makes an unattended fall call for
     * help. `setChronometerCountDown` draws a number going down; it has no
     * callback and fires nothing. Without this alarm branch the headline
     * behaviour of the product would be a picture of itself.
     */
    private suspend fun handleExpiry(app: SafeShadeApplication, eventId: String) {
        val history = app.container.preferences.fallHistory.first()
        val event = history.firstOrNull { it.id == eventId } ?: return

        // Already answered — the user pressed a button and the cancel raced us,
        // or the app resolved it. Do nothing.
        if (event.outcome != TripOutcome.PENDING) return

        val settings = app.container.preferences.safetySettings.first()
        val contacts = contactsForThisAlert(app, settings)
        val contact = primaryOf(contacts)

        if (!settings.autoCallEmergency || contact == null) {
            // Auto-call off, or nobody to call. Keep the alert on screen —
            // an unanswered fall is more urgent after the countdown, not less.
            AlertNotifier.showExpired(app, event, calledContact = null)
            return
        }

        app.container.safetyRepository.resolveAlert(
            eventId = eventId,
            outcome = TripOutcome.CONTACTED,
            contacted = true
        )

        AlertNotifier.showExpired(app, event, calledContact = contact.name.ifBlank { PhoneNumbers.format(contact.phone) })

        // SMS before the call: dialling hands the foreground to the dialer, and
        // an SMS is the message that survives a call going unanswered.
        if (settings.smsFallbackEnabled) notifyContactsBySms(app, contacts, event)

        placeEmergencyCall(app, contact)
    }

    /**
     * Texts everyone on the merged list.
     *
     * Takes the already-merged contacts rather than the settings blob so that
     * the people texted are exactly the people the dial decision was made from
     * - resolving the wearer twice would let the two disagree if the link
     * dropped between them.
     */
    private suspend fun notifyContactsBySms(
        app: SafeShadeApplication,
        contacts: List<EmergencyContact>,
        event: FallAlertEvent
    ) {
        val wearer = app.container.preferences.profile.first().deviceSettings.wearerName
        val fix = LastKnownLocation.state.value?.takeIf { it.isValid }
        val body = emergencyAlertText(
            wearerName = wearer,
            what = event.kind.label,
            lat = fix?.lat,
            lon = fix?.lon
        )
        contacts.forEach { contact: EmergencyContact ->
            val result = sendEmergencySms(app, contact, body)
            if (!result.succeeded) Log.w(TAG, "SMS to ${contact.name} failed: $result")
        }
    }

    private fun dismissNotification(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NotificationIds.FALL_ALERT)
        }
    }

    companion object {
        private const val TAG = "AlertActionReceiver"

        const val ACTION_IM_OK = "com.safeshade.action.ALERT_IM_OK"
        const val ACTION_CALL_NOW = "com.safeshade.action.ALERT_CALL_NOW"

        /** Fired by `AlarmManager` when a fall countdown expires. */
        const val ACTION_ALERT_EXPIRED = "com.safeshade.action.ALERT_EXPIRED"

        const val EXTRA_EVENT_ID = "com.safeshade.extra.EVENT_ID"

        /**
         * Builds one of the notification-action intents.
         *
         * `FLAG_UPDATE_CURRENT` matters: extras are ignored when the system
         * decides whether two `PendingIntent`s are the same, so without it a
         * second alert would reuse the first alert's *event id* and resolve the
         * wrong trip.
         */
        fun pendingIntent(
            context: Context,
            action: String,
            eventId: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, AlertActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_EVENT_ID, eventId)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
    }
}
