package com.safeshade.alarms

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.safeshade.SafeShadeApplication
import com.safeshade.alerts.AlertActionReceiver
import com.safeshade.data.Journey
import com.safeshade.data.JourneyState
import com.safeshade.data.Reminder
import com.safeshade.data.ReminderKind
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * Everything this app asks the system clock to remember for it.
 *
 * ### The exact-alarm fallback, and why it is honest rather than hidden
 *
 * From API 31 `SCHEDULE_EXACT_ALARM` is a special app access. It is granted by
 * default on a fresh install but the user can revoke it, and some OEM ROMs
 * revoke it on their own when an app is "optimised". `USE_EXACT_ALARM`, the
 * variant that cannot be revoked, is deliberately **not** requested: its Play
 * policy carve-out is for alarm-clock and calendar apps and a safety-wearable
 * companion is neither, so requesting it risks the listing.
 *
 * So there are two behaviours, and the difference is visible to the user:
 *
 * ```
 * exact granted   -> setExactAndAllowWhileIdle : fires at the minute, wakes Doze
 * exact revoked   -> setWindow(..., 15 min)    : fires within a 15-minute window
 * ```
 *
 * The fallback is `setWindow` rather than `setAndAllowWhileIdle` because a
 * 15-minute window is a promise the platform actually keeps, whereas
 * `setAndAllowWhileIdle` in deep Doze can slip by an hour or more. A
 * medication reminder that arrives "sometime this afternoon" is worse than one
 * the user was told would be approximate. [canScheduleExact] and
 * [exactAlarmSettingsIntent] exist so the reliability screen can say which of
 * the two is in force and offer the grant, instead of the app quietly drifting.
 *
 * ### Request codes are fixed constants
 *
 * Not `Reminder.requestCode` (`id.hashCode()`) plus an offset. `DeviceRepository`
 * already enforces exactly one reminder per [ReminderKind], so a per-kind
 * constant is both sufficient and collision-free — whereas an arbitrary hash
 * plus an offset is still arbitrary, and a collision would make one reminder
 * silently cancel another.
 *
 * Cancellation depends on this: `AlarmManager.cancel` matches a `PendingIntent`
 * by request code, component and action only — **extras are not compared**.
 * Every schedule below therefore also passes `FLAG_UPDATE_CURRENT`, or a
 * re-schedule would reuse the previous call's extras.
 */
object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    /** How wide the inexact fallback window is. Quoted verbatim in the UI. */
    const val INEXACT_WINDOW_MS = 15 * 60_000L

    // Request codes. One per schedule, fixed forever; see the class note.
    private const val RC_MEDICATION = 7001
    private const val RC_CHECK_IN_PROMPT = 7002
    private const val RC_FAKE_CALL = 7003
    private const val RC_JOURNEY_DEADLINE = 7004
    private const val RC_CHECK_IN_DEADLINE = 7005
    private const val RC_ALERT_EXPIRY = 7006

    // ============================================
    // Capability
    // ============================================

    /**
     * Whether reminders will fire at the minute.
     *
     * Below API 31 exact alarms need no permission, so this is true. Callers
     * should surface a false plainly — "reminders may be up to 15 minutes late"
     * — rather than pretending.
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(AlarmManager::class.java) ?: return false
        return runCatching { am.canScheduleExactAlarms() }.getOrDefault(false)
    }

    /**
     * The settings screen that grants exact alarms, or null below API 31 where
     * there is nothing to grant.
     *
     * Launch it through [com.safeshade.service.OemReliability.open] so an OEM
     * that does not resolve it falls back to the app's details page rather than
     * throwing.
     */
    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.fromParts("package", context.packageName, null)
        )
    }

    // ============================================
    // Medication
    // ============================================

    /** Arms the next occurrence of a daily medication reminder. */
    fun scheduleMedication(context: Context, reminder: Reminder) {
        if (!reminder.enabled) {
            cancelMedication(context)
            return
        }
        schedule(
            context = context,
            at = nextDailyOccurrence(reminder.hour, reminder.minute),
            pendingIntent = receiver(context, ReminderReceiver.ACTION_MEDICATION, RC_MEDICATION)
        )
    }

    fun cancelMedication(context: Context) =
        cancel(context, ReminderReceiver.ACTION_MEDICATION, RC_MEDICATION)

    // ============================================
    // Recurring check-in prompt (the Helmet / lone-worker one)
    // ============================================

    /** Arms the next interval prompt. Re-armed by the receiver after each fire. */
    fun scheduleCheckInPrompt(context: Context, reminder: Reminder) {
        if (!reminder.enabled || reminder.intervalMinutes <= 0) {
            cancelCheckInPrompt(context)
            return
        }
        schedule(
            context = context,
            at = System.currentTimeMillis() + reminder.intervalMinutes * 60_000L,
            pendingIntent = receiver(
                context, ReminderReceiver.ACTION_CHECK_IN_PROMPT, RC_CHECK_IN_PROMPT
            )
        )
    }

    fun cancelCheckInPrompt(context: Context) =
        cancel(context, ReminderReceiver.ACTION_CHECK_IN_PROMPT, RC_CHECK_IN_PROMPT)

    // ============================================
    // Journey ETA deadline
    // ============================================

    /**
     * Arms the overdue alarm for a journey.
     *
     * [JourneyRepository][com.safeshade.repo.JourneyRepository] already runs a
     * 30-second ticker that escalates and records the trip. This alarm is not a
     * second escalation and must not duplicate one — its job is to **wake the
     * process** so that ticker is running at all, and to get the SMS out. The
     * division of labour is spelled out again in [ReminderReceiver].
     */
    fun scheduleJourneyDeadline(context: Context, journey: Journey) {
        if (journey.state != JourneyState.ACTIVE) {
            cancelJourneyDeadline(context)
            return
        }
        schedule(
            context = context,
            at = journey.etaAt + journey.graceSeconds * 1_000L,
            pendingIntent = receiver(
                context, ReminderReceiver.ACTION_JOURNEY_OVERDUE, RC_JOURNEY_DEADLINE
            )
        )
    }

    fun cancelJourneyDeadline(context: Context) =
        cancel(context, ReminderReceiver.ACTION_JOURNEY_OVERDUE, RC_JOURNEY_DEADLINE)

    // ============================================
    // Check-in escalation deadline
    // ============================================

    /**
     * Arms the escalation for the *earliest still-open* check-in request.
     *
     * One alarm rather than one per request: a guardian asking "are you OK?"
     * is a single outstanding question in practice, and a single alarm keeps
     * cancellation trivially correct. If several are open the earliest is the
     * one that matters, and the receiver re-arms for the next after it fires.
     */
    fun scheduleCheckInDeadline(context: Context, requestId: String, at: Long) {
        schedule(
            context = context,
            at = at,
            pendingIntent = receiver(
                context,
                ReminderReceiver.ACTION_CHECK_IN_ESCALATE,
                RC_CHECK_IN_DEADLINE
            ) { it.putExtra(ReminderReceiver.EXTRA_CHECK_IN_ID, requestId) }
        )
    }

    fun cancelCheckInDeadline(context: Context) =
        cancel(context, ReminderReceiver.ACTION_CHECK_IN_ESCALATE, RC_CHECK_IN_DEADLINE)

    // ============================================
    // Staged fake call
    // ============================================

    /**
     * Stages a fake incoming call [delayMillis] from now.
     *
     * Deliberately an alarm rather than a coroutine `delay`: the entire point
     * is that the user puts the phone away and walks off, and a coroutine dies
     * with the process the moment they do.
     */
    fun scheduleFakeCall(context: Context, delayMillis: Long) {
        schedule(
            context = context,
            at = System.currentTimeMillis() + delayMillis.coerceAtLeast(0L),
            pendingIntent = receiver(context, ReminderReceiver.ACTION_FAKE_CALL, RC_FAKE_CALL)
        )
    }

    fun cancelFakeCall(context: Context) =
        cancel(context, ReminderReceiver.ACTION_FAKE_CALL, RC_FAKE_CALL)

    // ============================================
    // Fall countdown expiry
    // ============================================

    /**
     * The alarm that actually acts when a fall countdown reaches zero.
     *
     * See [com.safeshade.alerts.AlertNotifier] — the chronometer on the
     * notification is a rendering, not a timer. This is the timer.
     */
    fun scheduleAlertExpiry(context: Context, eventId: String, at: Long) {
        schedule(
            context = context,
            at = at,
            pendingIntent = alertExpiry(context) {
                it.putExtra(AlertActionReceiver.EXTRA_EVENT_ID, eventId)
            }
        )
    }

    fun cancelAlertExpiry(context: Context) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { am.cancel(alertExpiry(context)) }
    }

    // ============================================
    // Boot / cold-start re-arm
    // ============================================

    /**
     * Rebuilds every schedule from DataStore.
     *
     * Alarms are held in RAM by the system and are erased by a reboot, a force
     * stop and an app update. Nothing tells the app this happened. A user whose
     * medication reminder simply stopped after a restart is worse off than one
     * who never had it, because they stopped watching the clock — which is why
     * [BootReceiver] is mandatory and why this function reads from disk rather
     * than from any in-memory state.
     *
     * Reads go through `preferences` and `.first()`, never a repository
     * `StateFlow.value`: in a just-booted process those flows have not yet
     * received their first DataStore emission and every one of them is `null`.
     */
    suspend fun rearmAll(context: Context) {
        val prefs = (context.applicationContext as? SafeShadeApplication)
            ?.container?.preferences ?: return

        val reminders = prefs.reminders.first()
        reminders.firstOrNull { it.kind == ReminderKind.MEDICATION }
            ?.let { scheduleMedication(context, it) }
            ?: cancelMedication(context)

        reminders.firstOrNull { it.kind == ReminderKind.CHECK_IN }
            ?.let { scheduleCheckInPrompt(context, it) }
            ?: cancelCheckInPrompt(context)

        /*
         * A staged fake call is not re-armed. It is a "get me out of this
         * conversation in two minutes" gesture; a phone that reboots in those
         * two minutes has ended the conversation more thoroughly than the
         * feature would have, and ringing an hour later would only confuse.
         */
        cancelFakeCall(context)

        val journey = prefs.journey.first()
        if (journey != null && journey.state == JourneyState.ACTIVE) {
            scheduleJourneyDeadline(context, journey)
        } else {
            cancelJourneyDeadline(context)
        }

        val nextCheckIn = prefs.checkIns.first()
            .filter { it.isOpen }
            .minByOrNull { it.deadlineAt }
        if (nextCheckIn != null) {
            scheduleCheckInDeadline(context, nextCheckIn.id, nextCheckIn.deadlineAt)
        } else {
            cancelCheckInDeadline(context)
        }

        /*
         * A fall countdown is never re-armed across a reboot either. The trip
         * is already on disk with a PENDING outcome and the app surfaces it on
         * next launch; auto-dialling an emergency contact minutes or hours
         * after a reboot, for a countdown nobody has been able to see or
         * cancel, is not a defensible thing to do.
         */
        cancelAlertExpiry(context)
    }

    // ============================================
    // Plumbing
    // ============================================

    /**
     * The one place an alarm is actually set.
     *
     * A time already in the past is pushed a few seconds out rather than
     * dropped: that is the boot case, where a deadline elapsed while the phone
     * was off and the escalation still needs to happen.
     */
    private fun schedule(context: Context, at: Long, pendingIntent: PendingIntent) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val fireAt = at.coerceAtLeast(System.currentTimeMillis() + 5_000L)

        try {
            if (canScheduleExact(context)) {
                // RTC_WAKEUP, not ELAPSED_REALTIME: these are wall-clock
                // promises ("9am", "the ETA you typed"), and they must survive
                // the user changing the time zone on a train.
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, pendingIntent)
            } else {
                am.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    fireAt,
                    INEXACT_WINDOW_MS,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            /*
             * canScheduleExactAlarms() can be true and the permission gone by
             * the time setExactAndAllowWhileIdle runs — the user revoking it
             * from settings is instant and there is no lock to take. Falling
             * back rather than crashing means the reminder is late, not absent.
             */
            Log.w(TAG, "Exact alarm refused, falling back to a window", e)
            runCatching {
                am.setWindow(AlarmManager.RTC_WAKEUP, fireAt, INEXACT_WINDOW_MS, pendingIntent)
            }
        }
    }

    private fun cancel(context: Context, action: String, requestCode: Int) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        runCatching { am.cancel(receiver(context, action, requestCode)) }
    }

    private fun receiver(
        context: Context,
        action: String,
        requestCode: Int,
        extras: (Intent) -> Unit = {}
    ): PendingIntent {
        val intent = Intent(context.applicationContext, ReminderReceiver::class.java)
            .setAction(action)
            .also(extras)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun alertExpiry(context: Context, extras: (Intent) -> Unit = {}): PendingIntent {
        val intent = Intent(context.applicationContext, AlertActionReceiver::class.java)
            .setAction(AlertActionReceiver.ACTION_ALERT_EXPIRED)
            .also(extras)
        return PendingIntent.getBroadcast(
            context.applicationContext,
            RC_ALERT_EXPIRY,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    /**
     * The next time today or tomorrow that reads [hour]:[minute].
     *
     * Computed against a fresh `Calendar` rather than by adding an offset, so
     * it stays correct across a DST change — adding 24 hours would drift the
     * reminder by an hour twice a year, in the direction nobody expects.
     */
    internal fun nextDailyOccurrence(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= now) calendar.add(Calendar.DAY_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}
