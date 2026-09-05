package com.safeshade.alarms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safeshade.MainActivity
import com.safeshade.SafeShadeApplication
import com.safeshade.alerts.AlertNotifier
import com.safeshade.alerts.Channels
import com.safeshade.alerts.NotificationIds
import com.safeshade.data.JourneyState
import com.safeshade.data.ReminderKind
import com.safeshade.data.SafetySettings
import com.safeshade.emergencyAlertText
import com.safeshade.sendEmergencySms
import com.safeshade.service.LastKnownLocation
import com.safeshade.service.LinkService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Everything [ReminderScheduler] armed, arriving.
 *
 * ### Two rules hold for every branch in here
 *
 * **1. Re-read from DataStore; never trust that you should have fired.** An
 * `AlarmManager` cancel matches a `PendingIntent` by request code, component
 * and action — extras are not compared — so any mismatch anywhere means the
 * cancel quietly failed and the alarm still arrives. Every branch below
 * re-reads the state it acts on and returns if the thing is no longer due.
 *
 * **2. Read `container.preferences`, never a repository's `StateFlow.value`.**
 * The repositories expose `stateIn(scope, SharingStarted.Eagerly, null)`.
 * `Eagerly` starts the collection when `AppContainer` is built, but the first
 * DataStore read is file I/O and it has not completed by the time `onReceive`
 * runs in a process this very broadcast just woke. `.value` there is `null` —
 * meaning an escalation would find zero emergency contacts and send nothing,
 * with no error anywhere. `preferences.<flow>.first()` suspends until DataStore
 * answers and is the only correct read in a receiver.
 *
 * ### What this receiver deliberately does *not* do
 *
 * It does not record trips for journeys or check-ins.
 * `JourneyRepository.checkOverdue()` runs on a 30-second ticker and already
 * flips the journey to `ESCALATED` and records a `JOURNEY_OVERDUE` trip;
 * `SafetyRepository.escalateOverdueCheckIns()` does the same on a 60-second
 * ticker for check-ins. Both live on the application scope, so simply touching
 * `container` here is enough to have them running. Recording a second trip from
 * this receiver would put two entries in the guardian's history for one event
 * and leave them pairing them up by hand. What the alarm buys is the thing the
 * tickers cannot do for themselves: waking the process, and getting the SMS out
 * immediately rather than up to thirty seconds later.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as? SafeShadeApplication ?: return
        val checkInId = intent.getStringExtra(EXTRA_CHECK_IN_ID)
        val pending = goAsync()

        Channels.ensureCreated(app)

        // goAsync: onReceive returns immediately and the process can be reaped
        // the moment it does. Every branch here needs at least one suspending
        // DataStore read, which would otherwise race the teardown and lose.
        app.applicationScope.launch {
            try {
                when (action) {
                    ACTION_MEDICATION -> medication(app)
                    ACTION_CHECK_IN_PROMPT -> checkInPrompt(app)
                    ACTION_CHECK_IN_ANSWER -> checkInAnswer(app)
                    ACTION_CHECK_IN_ESCALATE -> checkInEscalate(app, checkInId)
                    ACTION_JOURNEY_OVERDUE -> journeyOverdue(app)
                    ACTION_FAKE_CALL -> fakeCall(app)
                    ACTION_FAKE_CALL_DISMISS -> dismiss(app, NotificationIds.REMINDER_BASE + 3)
                    else -> Unit
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reminder action $action failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    // ============================================
    // Medication
    // ============================================

    private suspend fun medication(app: SafeShadeApplication) {
        val reminder = app.container.preferences.reminders.first()
            .firstOrNull { it.kind == ReminderKind.MEDICATION }
        if (reminder == null || !reminder.enabled) return

        notify(
            app,
            NotificationIds.REMINDER_BASE,
            NotificationCompat.Builder(app, Channels.REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(reminder.label.ifBlank { "Time for your medication" })
                .setContentText("Tap when you have taken it")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openApp(app, ROUTE_REMINDERS, RC_MED_CONTENT))
                .build()
        )

        // setExactAndAllowWhileIdle is one-shot; there is no repeating variant
        // that is also exact. Re-arming here is what makes it daily, and it is
        // why a missed re-arm (a crash between fire and re-arm) is repaired by
        // BootReceiver on the next restart.
        ReminderScheduler.scheduleMedication(app, reminder)
    }

    // ============================================
    // Recurring check-in prompt
    // ============================================

    private suspend fun checkInPrompt(app: SafeShadeApplication) {
        val reminder = app.container.preferences.reminders.first()
            .firstOrNull { it.kind == ReminderKind.CHECK_IN }
        if (reminder == null || !reminder.enabled || reminder.intervalMinutes <= 0) return

        notify(
            app,
            NotificationIds.CHECK_IN,
            NotificationCompat.Builder(app, Channels.REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(reminder.label.ifBlank { "Are you OK?" })
                .setContentText("Confirm you are fine, or your guardian will be told")
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openApp(app, ROUTE_CHECKIN, RC_CHECKIN_CONTENT))
                .addAction(
                    android.R.drawable.ic_menu_send,
                    "I'm OK",
                    broadcast(app, ACTION_CHECK_IN_ANSWER, RC_CHECKIN_ANSWER)
                )
                .build()
        )

        ReminderScheduler.scheduleCheckInPrompt(app, reminder)
    }

    /** "I'm OK" on a check-in prompt: answers every request still open. */
    private suspend fun checkInAnswer(app: SafeShadeApplication) {
        dismiss(app, NotificationIds.CHECK_IN)
        val open = app.container.preferences.checkIns.first().filter { it.isOpen }
        open.forEach { app.container.safetyRepository.answerCheckIn(it.id) }
        ReminderScheduler.cancelCheckInDeadline(app)
    }

    // ============================================
    // Check-in escalation
    // ============================================

    private suspend fun checkInEscalate(app: SafeShadeApplication, requestId: String?) {
        val requests = app.container.preferences.checkIns.first()
        val request = requests.firstOrNull { it.id == requestId }

        // Answered in the meantime, or the alarm outlived its request. Nothing
        // to do — and specifically, no SMS.
        if (request == null || !request.isOpen) {
            rearmNextCheckIn(app, requests)
            return
        }
        if (System.currentTimeMillis() < request.deadlineAt) {
            // Fired early (an inexact window can be generous in both
            // directions). Put it back rather than escalating a live check-in.
            ReminderScheduler.scheduleCheckInDeadline(app, request.id, request.deadlineAt)
            return
        }

        notify(
            app,
            NotificationIds.CHECK_IN,
            NotificationCompat.Builder(app, Channels.WATCH)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Check-in missed")
                .setContentText("No response within the check-in window")
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openApp(app, ROUTE_CHECKIN, RC_CHECKIN_CONTENT))
                .build()
        )

        // The trip itself is recorded by SafetyRepository's ticker; see the
        // class note. This only gets the message out.
        alertContacts(app, "Check-in missed")

        rearmNextCheckIn(app, requests.filterNot { it.id == request.id })
    }

    private fun rearmNextCheckIn(
        app: SafeShadeApplication,
        requests: List<com.safeshade.data.CheckInRequest>
    ) {
        val next = requests.filter { it.isOpen }.minByOrNull { it.deadlineAt }
        if (next != null) {
            ReminderScheduler.scheduleCheckInDeadline(app, next.id, next.deadlineAt)
        } else {
            ReminderScheduler.cancelCheckInDeadline(app)
        }
    }

    // ============================================
    // Journey overdue
    // ============================================

    private suspend fun journeyOverdue(app: SafeShadeApplication) {
        val journey = app.container.preferences.journey.first() ?: return

        // Arrived, cancelled, or the alarm survived a journey that ended.
        if (journey.state != JourneyState.ACTIVE && journey.state != JourneyState.ESCALATED) return

        val deadline = journey.etaAt + journey.graceSeconds * 1_000L
        if (journey.state == JourneyState.ACTIVE && System.currentTimeMillis() < deadline) {
            ReminderScheduler.scheduleJourneyDeadline(app, journey)
            return
        }

        notify(
            app,
            NotificationIds.REMINDER_BASE + 1,
            NotificationCompat.Builder(app, Channels.WATCH)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("Journey overdue")
                .setContentText(
                    journey.label.ifBlank { "The walk you started has not been ended" }
                )
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openApp(app, ROUTE_JOURNEY, RC_JOURNEY_CONTENT))
                .build()
        )

        // Journey state and the trip record belong to JourneyRepository's
        // ticker, which is now running because this process is awake.
        alertContacts(app, journey.label.ifBlank { "Journey overdue" })
    }

    // ============================================
    // Fake call
    // ============================================

    /**
     * The staged "someone is calling me, I have to go" escape.
     *
     * Uses `CATEGORY_CALL` and a full-screen intent so it looks like a call on
     * the lock screen. On API 34+ with the full-screen-intent permission denied
     * this degrades to a heads-up banner exactly as a fall alert does — which
     * is fine here, because a banner that says a name is calling still does the
     * social job.
     */
    private suspend fun fakeCall(app: SafeShadeApplication) {
        val reminder = app.container.preferences.reminders.first()
            .firstOrNull { it.kind == ReminderKind.FAKE_CALL }
        val caller = reminder?.label?.takeIf { it.isNotBlank() } ?: "Mum"
        val id = NotificationIds.REMINDER_BASE + 3

        notify(
            app,
            id,
            NotificationCompat.Builder(app, Channels.FALL_ALERT)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(caller)
                .setContentText("Incoming call")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setFullScreenIntent(openApp(app, ROUTE_FAKE_CALL, RC_FAKE_CALL_FSI), true)
                .setContentIntent(openApp(app, ROUTE_FAKE_CALL, RC_FAKE_CALL_FSI))
                .addAction(
                    android.R.drawable.sym_call_missed,
                    "Decline",
                    broadcast(app, ACTION_FAKE_CALL_DISMISS, RC_FAKE_CALL_DISMISS)
                )
                .build()
        )
    }

    // ============================================
    // Shared
    // ============================================

    /**
     * SMS to every emergency contact with the last position this phone knows.
     *
     * [LastKnownLocation] is process-lifetime, so in a cold-woken process it is
     * empty and the message goes out without coordinates rather than with stale
     * ones. That is the honest trade: a three-hour-old fix presented as a
     * current location sends somebody to the wrong place, which is worse than
     * sending them no place at all.
     */
    private suspend fun alertContacts(app: SafeShadeApplication, what: String) {
        val settings: SafetySettings = app.container.preferences.safetySettings.first()
        if (settings.emergencyContacts.isEmpty()) {
            Log.w(TAG, "Escalation with no emergency contacts configured")
            return
        }

        val wearer = app.container.preferences.profile.first().deviceSettings.wearerName
        val fix = LastKnownLocation.state.value?.takeIf { it.isValid }
        val body = emergencyAlertText(
            wearerName = wearer,
            what = what,
            lat = fix?.lat,
            lon = fix?.lon
        )

        settings.emergencyContacts.forEach { contact ->
            val result = sendEmergencySms(app, contact, body)
            if (!result.succeeded) Log.w(TAG, "Escalation SMS to ${contact.name} failed: $result")
        }
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        // Degrade, never crash. AlertNotifier.post already does the
        // enabled-check plus the SecurityException catch; reusing it keeps one
        // definition of "what happens when notifications are off".
        AlertNotifier.post(context, id, notification)
    }

    private fun dismiss(context: Context, id: Int) {
        runCatching { NotificationManagerCompat.from(context).cancel(id) }
    }

    private fun openApp(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(LinkService.EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun broadcast(context: Context, action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val TAG = "ReminderReceiver"

        const val ACTION_MEDICATION = "com.safeshade.action.REMIND_MEDICATION"
        const val ACTION_CHECK_IN_PROMPT = "com.safeshade.action.REMIND_CHECK_IN"
        const val ACTION_CHECK_IN_ANSWER = "com.safeshade.action.CHECK_IN_ANSWER"
        const val ACTION_CHECK_IN_ESCALATE = "com.safeshade.action.CHECK_IN_ESCALATE"
        const val ACTION_JOURNEY_OVERDUE = "com.safeshade.action.JOURNEY_OVERDUE"
        const val ACTION_FAKE_CALL = "com.safeshade.action.FAKE_CALL"
        const val ACTION_FAKE_CALL_DISMISS = "com.safeshade.action.FAKE_CALL_DISMISS"

        const val EXTRA_CHECK_IN_ID = "com.safeshade.extra.CHECK_IN_ID"

        /** Mirrors of `Routes`; that file is a merge hotspot no lane may edit. */
        private const val ROUTE_REMINDERS = "device/reminders"
        private const val ROUTE_CHECKIN = "circle/checkin"
        private const val ROUTE_JOURNEY = "circle/journey"
        private const val ROUTE_FAKE_CALL = "circle/sim"

        private const val RC_MED_CONTENT = 7101
        private const val RC_CHECKIN_CONTENT = 7102
        private const val RC_CHECKIN_ANSWER = 7103
        private const val RC_JOURNEY_CONTENT = 7104
        private const val RC_FAKE_CALL_FSI = 7105
        private const val RC_FAKE_CALL_DISMISS = 7106
    }
}
