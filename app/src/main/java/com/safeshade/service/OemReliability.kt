package com.safeshade.service

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.safeshade.alarms.ReminderScheduler
import com.safeshade.alerts.AlertNotifier
import com.safeshade.alerts.Channels

/**
 * The permission doctor's back end.
 *
 * ### The problem this exists for
 *
 * Everything else in this layer is correct Android. It is still not enough on
 * the phones this product is actually for. A ₹10,000 Xiaomi, Realme, Oppo or
 * Vivo handset ships with an OEM battery manager that sits above the platform's
 * own rules and will stop a foreground service, drop a BLE link and swallow an
 * alarm regardless of what the app declared. The user has to go and turn those
 * off by hand, and there is no API to do it for them and no API to ask whether
 * it has been done.
 *
 * So this file cannot verify; it can only take the user to the right screen.
 * That is worth doing well — the alternative is a support instruction that
 * begins "open Settings, then Battery, then…" and ends with nobody doing it.
 *
 * ### Every OEM intent is wrapped, without exception
 *
 * MIUI's autostart activity is
 * `com.miui.securitycenter/.permission.autostart.AutoStartManagementActivity`.
 * That component name has moved between MIUI versions, is absent on non-Xiaomi
 * builds, and is absent again on some Xiaomi builds sold outside China. An
 * explicit `Intent` naming a component that does not resolve throws
 * `ActivityNotFoundException` — a crash, in the middle of the screen whose
 * whole purpose is reassuring the user that the app is reliable.
 *
 * Every launch therefore goes through [open], which tries the specific intent,
 * then the generic one, then `ACTION_APPLICATION_DETAILS_SETTINGS`, which
 * exists on every Android device ever shipped. Worst case the user lands one
 * level up from where they were aimed, which is recoverable. A crash is not.
 */
object OemReliability {

    private const val TAG = "OemReliability"

    // ============================================
    // Status, for the checklist UI
    // ============================================

    /** One row of the reliability checklist. */
    data class Check(
        val id: Id,
        val title: String,
        /** Null when the state genuinely cannot be read — see [Id.AUTOSTART]. */
        val satisfied: Boolean?,
        val explanation: String,
        /** What tapping the row should launch. Null when there is nothing to open. */
        val intent: Intent?
    )

    enum class Id { NOTIFICATIONS, FULL_SCREEN_INTENT, EXACT_ALARMS, BATTERY, AUTOSTART }

    /**
     * The whole checklist, in the order it should be shown: the things that
     * silence alerts first, the OEM-specific ones last.
     */
    fun checks(context: Context): List<Check> = listOf(
        Check(
            id = Id.NOTIFICATIONS,
            title = "Alerts can be shown",
            satisfied = Channels.enabled(context) && AlertNotifier.fallChannelEnabled(context),
            explanation = "Without this a fall alert is recorded but never appears.",
            intent = notificationSettingsIntent(context)
        ),
        Check(
            id = Id.FULL_SCREEN_INTENT,
            title = "Alerts can take over the screen",
            satisfied = AlertNotifier.canUseFullScreenIntent(context),
            explanation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                "Without this a fall alert still arrives, with its countdown and " +
                    "buttons, but as a banner rather than filling the locked screen."
            } else {
                "Granted automatically on this version of Android."
            },
            intent = AlertNotifier.fullScreenIntentSettings(context)
        ),
        Check(
            id = Id.EXACT_ALARMS,
            title = "Reminders fire on time",
            satisfied = ReminderScheduler.canScheduleExact(context),
            explanation = if (ReminderScheduler.canScheduleExact(context)) {
                "Medication and check-in reminders fire at the minute."
            } else {
                "Reminders will fire within about 15 minutes of the time you set."
            },
            intent = ReminderScheduler.exactAlarmSettingsIntent(context)
        ),
        Check(
            id = Id.BATTERY,
            title = "Battery saver will not stop SafeShade",
            satisfied = isIgnoringBatteryOptimizations(context),
            explanation = "Without this the link to your device is dropped a few " +
                "minutes after the screen goes off.",
            intent = batteryOptimizationIntent(context)
        )
    ) + oemChecks(context)

    /** Only shown where they mean something — an empty list on a Pixel. */
    private fun oemChecks(context: Context): List<Check> {
        if (!looksLikeMiui()) return emptyList()
        return listOf(
            Check(
                id = Id.AUTOSTART,
                title = "Autostart is on",
                // Deliberately null, not false. MIUI exposes no way to read
                // this, and showing a red cross the user cannot ever clear is
                // worse than saying plainly that we cannot tell.
                satisfied = null,
                explanation = "MIUI stops apps from restarting on their own. " +
                    "SafeShade cannot check this setting – please confirm it is on.",
                intent = autostartIntent()
            )
        )
    }

    // ============================================
    // Individual capabilities
    // ============================================

    /**
     * Whether the OS has exempted this app from Doze's app-standby buckets.
     *
     * Reading this needs no permission. *Requesting* the change does — see
     * [batteryOptimizationIntent].
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val power = context.getSystemService(PowerManager::class.java) ?: return false
        return runCatching {
            power.isIgnoringBatteryOptimizations(context.packageName)
        }.getOrDefault(false)
    }

    /**
     * The battery-optimisation exemption.
     *
     * The direct request, `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, shows
     * a one-tap system dialog but requires the app to hold
     * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in its manifest. If that
     * permission is absent the call fails, so [open] falls back to
     * `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, which needs nothing and
     * shows the full list for the user to find SafeShade in — two more taps,
     * always available.
     */
    fun batteryOptimizationIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )

    /** The app's notification settings, or its channel list on API 26+. */
    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)

    /** The fall-alert channel specifically, so the user lands on the switch itself. */
    fun fallChannelSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, Channels.FALL_ALERT)

    /**
     * MIUI's autostart manager.
     *
     * The component name below is the one that has been stable across most
     * MIUI 12–14 builds. It is still a guess about somebody else's private
     * activity, which is why this must only ever be launched through [open].
     */
    fun autostartIntent(): Intent = Intent().setComponent(
        ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
    )

    /**
     * MIUI's per-app battery saver ("No restrictions"), a *different* screen
     * from autostart and the one that actually kills background BLE.
     */
    fun miuiBatterySaverIntent(context: Context): Intent = Intent(
        "miui.intent.action.APP_PERM_EDITOR"
    ).setClassName(
        "com.miui.securitycenter",
        "com.miui.permcenter.permissions.PermissionsEditorActivity"
    ).putExtra("extra_pkgname", context.packageName)

    /** The last resort. Present on every Android device that has ever shipped. */
    fun appDetailsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )

    // ============================================
    // Launching
    // ============================================

    /**
     * Starts [intent], falling back until something opens.
     *
     * @return the intent that actually launched, or null if even the app
     *   details page failed — which would mean a device with no Settings app,
     *   and is reported rather than swallowed so the UI can say "open Settings
     *   yourself" instead of appearing to do nothing.
     */
    fun open(context: Context, intent: Intent?): Intent? {
        val candidates = listOfNotNull(
            intent,
            // The generic battery list needs no permission, unlike the direct
            // request above; try it before giving up on that particular row.
            if (intent?.action == Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } else {
                null
            },
            appDetailsIntent(context)
        )

        for (candidate in candidates) {
            val launchable = Intent(candidate).apply {
                // Required whenever the caller might not be an Activity — a
                // notification action, a service, a receiver.
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(launchable)
                return candidate
            } catch (e: ActivityNotFoundException) {
                // The expected failure for every OEM component name.
                Log.i(TAG, "No activity for ${candidate.action ?: candidate.component}", e)
            } catch (e: SecurityException) {
                // MIUI in particular guards some of these against outside
                // callers even when the component resolves.
                Log.i(TAG, "Refused ${candidate.action ?: candidate.component}", e)
            } catch (e: Exception) {
                Log.w(TAG, "Unexpected failure opening a settings screen", e)
            }
        }
        return null
    }

    /** Whether [intent] would open, so a row can be hidden rather than failing. */
    fun resolves(context: Context, intent: Intent?): Boolean {
        if (intent == null) return false
        return runCatching {
            context.packageManager.resolveActivity(intent, 0) != null
        }.getOrDefault(false)
    }

    // ============================================
    // OEM detection
    // ============================================

    /**
     * Best-effort MIUI/HyperOS detection.
     *
     * Deliberately only used to decide whether to *offer* a shortcut. Nothing
     * here branches behaviour on the manufacturer — that is how apps end up
     * broken on the one device nobody tested.
     */
    fun looksLikeMiui(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return manufacturer.contains("xiaomi") ||
            manufacturer.contains("redmi") ||
            manufacturer.contains("poco")
    }

    /** True on the OEMs known to need a manual autostart grant. */
    fun needsAutostartGrant(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return looksLikeMiui() ||
            manufacturer.contains("oppo") ||
            manufacturer.contains("vivo") ||
            manufacturer.contains("realme") ||
            manufacturer.contains("oneplus") ||
            manufacturer.contains("huawei") ||
            manufacturer.contains("honor")
    }
}
