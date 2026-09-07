package com.safeshade.alerts

import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Notification channels.
 *
 * Separated by urgency rather than by feature, because channels are the only
 * control a user actually has: someone who wants to mute reminders must be able
 * to do that without also muting fall alerts. Bundling them into one channel
 * makes "stop bothering me about pills" and "never tell me my father fell" the
 * same switch.
 */
object Channels {

    private const val GROUP_SAFETY = "group_safety"
    private const val GROUP_ROUTINE = "group_routine"

    /** A fall or SOS. Maximum urgency, bypasses Do Not Disturb. */
    const val FALL_ALERT = "fall_alert"

    /** A safe-zone crossing, a missed check-in, an overdue journey. */
    const val WATCH = "watch"

    /** Medication and check-in reminders. */
    const val REMINDERS = "reminders"

    /** The ongoing "SafeShade is watching" notification for the link service. */
    const val LINK = "link"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        manager.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_SAFETY, "Safety alerts")
        )
        manager.createNotificationChannelGroup(
            NotificationChannelGroup(GROUP_ROUTINE, "Routine")
        )

        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        manager.createNotificationChannel(
            NotificationChannel(
                FALL_ALERT,
                "Fall and SOS alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                group = GROUP_SAFETY
                description = "The wearable detected a fall, or the SOS button was held."
                // A fall at 3am is exactly the case this product exists for, so
                // this is one of the rare legitimate uses of DND bypass.
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 400)
                setSound(
                    android.media.RingtoneManager.getDefaultUri(
                        android.media.RingtoneManager.TYPE_ALARM
                    ),
                    alarmAudio
                )
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(WATCH, "Safe zones and check-ins", NotificationManager.IMPORTANCE_HIGH).apply {
                group = GROUP_SAFETY
                description = "Left a safe zone, missed a check-in, or a journey ran overdue."
                enableVibration(true)
            }
        )

        manager.createNotificationChannel(
            NotificationChannel(REMINDERS, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                group = GROUP_ROUTINE
                description = "Medication reminders and worker check-in prompts."
            }
        )

        manager.createNotificationChannel(
            // LOW so the persistent link notification never makes a sound or
            // peeks. It exists to keep the process alive, not to be read.
            NotificationChannel(LINK, "Connection", NotificationManager.IMPORTANCE_LOW).apply {
                group = GROUP_ROUTINE
                description = "The ongoing notice shown while SafeShade is watching the device."
                setShowBadge(false)
            }
        )
    }

    /** Whether the user has left notifications on at all. */
    fun enabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()
}

/** Stable notification ids, so an update replaces rather than stacks. */
object NotificationIds {
    const val FALL_ALERT = 1001
    const val LINK_SERVICE = 1002
    const val JOURNEY_SERVICE = 1003
    const val ZONE = 1010
    const val CHECK_IN = 1011

    /** The wearable has been out of reach for longer than the guardian allowed. */
    const val WEARABLE_OFFLINE = 1012

    /** The wearable's battery is at or below the guardian's threshold. */
    const val WEARABLE_LOW_BATTERY = 1013
    const val REMINDER_BASE = 2000
}
