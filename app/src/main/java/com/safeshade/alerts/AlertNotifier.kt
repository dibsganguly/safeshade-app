package com.safeshade.alerts

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safeshade.MainActivity
import com.safeshade.alarms.ReminderScheduler
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.TripKind

/**
 * The one notification this app exists to deliver.
 *
 * ### The full-screen-intent downgrade, and why the notification is the contract
 *
 * A fall alert wants to take over the screen: phone in a pocket, screen off,
 * nobody looking. `setFullScreenIntent` is the API for that, and on API 33 and
 * below `USE_FULL_SCREEN_INTENT` is a normal install-time permission that just
 * works.
 *
 * On API 34+ it is granted at install time **only** to apps whose declared
 * purpose is calling or alarms. Everyone else starts denied and has to be sent
 * to `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT`. The failure mode is what makes
 * this dangerous: when the permission is absent the platform does not throw, it
 * does not return an error, and it does not call anything back. It silently
 * downgrades the notification to an ordinary heads-up banner and carries on.
 * Code written as "launch the alarm Activity via the FSI, the Activity owns the
 * countdown and the buttons" therefore *appears* to work in review and, on a
 * locked API 34 phone with the permission denied, shows a banner with no
 * countdown and no way to respond — while the code that would have called the
 * ambulance never runs.
 *
 * So the rule here is: **the notification is the mechanism, the full-screen
 * Activity is an enhancement.** Everything the user needs is on the
 * notification itself —
 *
 *  - the live countdown, via [NotificationCompat.Builder.setUsesChronometer] +
 *    `setChronometerCountDown` + a `when` in the future, which the system
 *    renders and ticks with no process of ours running;
 *  - both actions, "I'm OK" and "Call now", as [AlertActionReceiver]
 *    broadcasts;
 *  - and, separately from the display, a one-shot alarm that fires when the
 *    countdown expires — because a chronometer is a picture of a countdown, not
 *    a countdown. See [ReminderScheduler.scheduleAlertExpiry].
 *
 * If the FSI is granted, the user also gets the takeover. If it is not, nothing
 * is lost but the takeover.
 */
object AlertNotifier {

    private const val TAG = "AlertNotifier"

    /** Extra carried into `MainActivity` so it can open the alarm UI. */
    const val EXTRA_ALERT_ID = "com.safeshade.extra.ALERT_ID"

    /** A synthetic id used by [showTestAlert] so a drill is never mistaken for a fall. */
    const val TEST_ALERT_ID = "test-alert"

    private const val RC_FULL_SCREEN = 6301
    private const val RC_CONTENT = 6302
    private const val RC_OK = 6303
    private const val RC_CALL = 6304

    // ============================================
    // Showing
    // ============================================

    /**
     * Raises the alert.
     *
     * @param countdownSeconds from `SafetySettings.fallCountdownSeconds`. Zero
     *   or less shows the alert with no countdown, which is the right rendering
     *   when auto-call is off — a counter that reaches zero and does nothing
     *   teaches the user that the countdown is decorative.
     * @param scheduleExpiry when true, arms the alarm that acts on the expiry.
     *   [showTestAlert] passes false: a drill must never place a call.
     */
    fun show(
        context: Context,
        event: FallAlertEvent,
        countdownSeconds: Int,
        scheduleExpiry: Boolean = true
    ) {
        Channels.ensureCreated(context)

        val app = context.applicationContext
        val notification = build(app, event, countdownSeconds)

        if (!post(app, NotificationIds.FALL_ALERT, notification)) {
            /*
             * POST_NOTIFICATIONS denied. Everything below still has to happen —
             * the countdown expiry is what places the call, and it does not
             * depend on anything being on screen. A user who muted
             * notifications has muted the display, not the ambulance.
             */
            Log.w(TAG, "Alert notification could not be posted; expiry still armed")
        }

        if (scheduleExpiry && countdownSeconds > 0) {
            ReminderScheduler.scheduleAlertExpiry(
                context = app,
                eventId = event.id,
                at = System.currentTimeMillis() + countdownSeconds * 1_000L
            )
        }
    }

    /**
     * The drill used by the reliability screen.
     *
     * Looks and sounds exactly like the real thing — that is the point, since
     * what is being tested is whether this phone's OEM lets the real thing
     * through — but arms no expiry and so can never dial. Its actions resolve
     * nothing, because [TEST_ALERT_ID] matches no stored event.
     */
    fun showTestAlert(context: Context) {
        show(
            context = context,
            event = FallAlertEvent(
                id = TEST_ALERT_ID,
                kind = TripKind.FALL,
                note = "Test alert — no call will be placed"
            ),
            countdownSeconds = 20,
            scheduleExpiry = false
        )
    }

    /** Clears the alert and disarms its expiry. Safe to call when nothing is showing. */
    fun cancel(context: Context) {
        val app = context.applicationContext
        runCatching {
            NotificationManagerCompat.from(app).cancel(NotificationIds.FALL_ALERT)
        }
        ReminderScheduler.cancelAlertExpiry(app)
    }

    /**
     * Re-posts the alert once the countdown has run out.
     *
     * Two genuinely different notifications, because the two situations demand
     * opposite affordances:
     *
     *  - **[calledContact] is null** — auto-call is off, or there is nobody to
     *    call. Nothing has happened yet, so the alert stays exactly as urgent
     *    as it was: ongoing, undismissable, both actions still live. A fall
     *    nobody answered is more urgent after thirty seconds, not less.
     *  - **[calledContact] is set** — the call has already been placed and the
     *    trip is already recorded as `CONTACTED`. Keeping the original
     *    notification here would be actively wrong: it is `setOngoing(true)`,
     *    so the user could not swipe it away while trying to talk to the person
     *    now on the phone, and its "Call now" action would re-dial and
     *    overwrite an outcome that is already correct. So this becomes a plain,
     *    dismissable receipt with no actions.
     */
    fun showExpired(context: Context, event: FallAlertEvent, calledContact: String?) {
        val app = context.applicationContext

        if (calledContact == null) {
            val builder = baseBuilder(app, event)
                .setContentText("No response yet. Tap to respond.")
                .setShowWhen(true)
                .setWhen(event.timestamp)
            post(app, NotificationIds.FALL_ALERT, builder.build())
            return
        }

        val receipt = NotificationCompat.Builder(app, Channels.FALL_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Emergency contact called")
            .setContentText("No response to the alert — calling $calledContact")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(false)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(event.timestamp)
            .setContentIntent(openApp(app, event.id, RC_CONTENT))
            .build()
        post(app, NotificationIds.FALL_ALERT, receipt)
    }

    // ============================================
    // Building
    // ============================================

    private fun build(
        context: Context,
        event: FallAlertEvent,
        countdownSeconds: Int
    ): Notification {
        val builder = baseBuilder(context, event)

        if (countdownSeconds > 0) {
            /*
             * All three of these are required together. `setWhen` supplies the
             * target instant, `setUsesChronometer` turns the "when" field into
             * a ticking clock instead of a timestamp, `setChronometerCountDown`
             * makes it count towards that instant rather than away from it, and
             * `setShowWhen(true)` is what actually puts the field on screen —
             * omit that last one and the countdown is computed and never drawn.
             *
             * The system ticks this itself. No process of ours needs to be
             * alive for the user to watch the seconds go down, which is the
             * entire reason to use it rather than re-posting once a second.
             */
            builder
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + countdownSeconds * 1_000L)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setContentText("Calling your emergency contact unless you respond")
        } else {
            builder
                .setShowWhen(true)
                .setWhen(event.timestamp)
                .setContentText("Tap to respond")
        }

        return builder.build()
    }

    private fun baseBuilder(context: Context, event: FallAlertEvent): NotificationCompat.Builder {
        val title = when (event.kind) {
            TripKind.FALL -> "Fall detected"
            TripKind.SOS -> "SOS pressed"
            TripKind.MISSED_CHECKIN -> "Check-in missed"
            TripKind.ZONE_EXIT -> "Left a safe zone"
            TripKind.JOURNEY_OVERDUE -> "Journey overdue"
        }

        return NotificationCompat.Builder(context, Channels.FALL_ALERT)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            // CATEGORY_ALARM is what earns the DND bypass the channel asks for
            // and what tells the system this is not social noise.
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // Public so it is readable on the lock screen. A fall alert that
            // hides its content behind "1 notification" is useless in exactly
            // the situation it was built for.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Ongoing: a swipe must not be able to dismiss an unanswered
            // emergency. The only ways out are the two actions.
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(openApp(context, event.id, RC_CONTENT))
            /*
             * `true` for the highPriority flag. On a locked device with the
             * permission granted this launches the Activity immediately; with
             * the permission denied it is downgraded to a heads-up banner in
             * silence — which is survivable precisely because the actions and
             * the countdown are on this notification and not inside that
             * Activity.
             */
            .setFullScreenIntent(openApp(context, event.id, RC_FULL_SCREEN), true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "I'm OK",
                AlertActionReceiver.pendingIntent(
                    context, AlertActionReceiver.ACTION_IM_OK, event.id, RC_OK
                )
            )
            .addAction(
                android.R.drawable.sym_action_call,
                "Call now",
                AlertActionReceiver.pendingIntent(
                    context, AlertActionReceiver.ACTION_CALL_NOW, event.id, RC_CALL
                )
            )
    }

    private fun openApp(context: Context, eventId: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ALERT_ID, eventId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /** @return whether the notification actually reached the shade. */
    internal fun post(context: Context, id: Int, notification: Notification): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        return try {
            manager.notify(id, notification)
            true
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS revoked between the check and the call.
            Log.w(TAG, "notify() refused", e)
            false
        }
    }

    // ============================================
    // Capability reporting, for the reliability screen
    // ============================================

    /**
     * Whether a full-screen intent will actually take over the screen.
     *
     * Below API 34 the permission is install-time and always granted, so this
     * is true. From API 34 it is a special app access the user must grant, and
     * because a denial is silent this is the only way the UI can tell the user
     * the truth about what a fall alert will do on their phone.
     */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        return runCatching { manager.canUseFullScreenIntent() }.getOrDefault(false)
    }

    /**
     * The settings screen where the user can grant it, or null below API 34
     * where there is nothing to grant.
     */
    fun fullScreenIntentSettings(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return null
        return Intent(
            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
            android.net.Uri.fromParts("package", context.packageName, null)
        )
    }

    /** Whether the fall channel itself has been muted or blocked by the user. */
    fun fallChannelEnabled(context: Context): Boolean {
        if (!Channels.enabled(context)) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(Channels.FALL_ALERT) ?: return true
        return channel.importance != NotificationManager.IMPORTANCE_NONE
    }
}
