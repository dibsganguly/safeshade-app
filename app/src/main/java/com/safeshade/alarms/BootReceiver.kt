package com.safeshade.alarms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.safeshade.SafeShadeApplication
import com.safeshade.service.LinkService
import kotlinx.coroutines.launch

/**
 * Re-arms everything a reboot erased.
 *
 * ### This receiver is mandatory, not a nicety
 *
 * `AlarmManager` keeps its alarms in system RAM. A reboot clears them, and so
 * does a force stop and, on some OEM ROMs, an app update or an aggressive
 * "clean up" from the battery manager. **Nothing tells the app that it
 * happened.** There is no callback, no exception, no flag to check on next
 * launch.
 *
 * The result without this receiver is the worst kind of failure this product
 * can have: a medication reminder that worked for three weeks and then quietly
 * stopped. The user has by then stopped watching the clock, because the app
 * was doing it. A reminder that never existed would have left them checking
 * for themselves; one that silently stops takes that away first. The same
 * applies to a journey deadline and a check-in escalation, where the thing that
 * stops is somebody being told that a person did not arrive.
 *
 * ### What it does and does not restart
 *
 * - **Alarms**: always, from DataStore. See [ReminderScheduler.rearmAll].
 * - **[LinkService]**: attempted, because `BOOT_COMPLETED` is on the platform's
 *   exemption list for starting a foreground service from the background — but
 *   guarded, because `BLUETOOTH_CONNECT` may have been revoked while the phone
 *   was off, and refused starts must not crash a boot receiver.
 * - **[com.safeshade.service.JourneyService]**: never. Its type is `location`,
 *   which API 34+ forbids starting from the background outright, and its
 *   contract is that only an in-app tap starts it. A phone that reboots
 *   mid-journey keeps its escalation alarm — which is the part that matters —
 *   and loses only the position sampling.
 *
 * ### Why not `LOCKED_BOOT_COMPLETED`
 *
 * That fires earlier, before the user has unlocked, in direct-boot mode where
 * only device-encrypted storage is readable. This app's DataStore lives in
 * credential-encrypted storage, so a read there would fail or return defaults
 * and we would cheerfully re-arm nothing at all. `BOOT_COMPLETED` arrives after
 * the first unlock, when the data is actually there.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED) return

        val app = context.applicationContext as? SafeShadeApplication ?: return
        val pending = goAsync()

        app.applicationScope.launch {
            try {
                // Reading DataStore is suspending file I/O; without goAsync the
                // process could be reaped before a single alarm was re-armed.
                ReminderScheduler.rearmAll(app)
                restartLink(app)
            } catch (e: Exception) {
                Log.e(TAG, "Boot re-arm failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun restartLink(context: Context) {
        if (!hasBluetoothPermission(context)) {
            Log.i(TAG, "No BLUETOOTH_CONNECT after boot; link service not started")
            return
        }
        // start() swallows the refusal cases itself. A boot receiver that
        // throws takes down a process the user cannot see and will never
        // report.
        LinkService.start(context)
    }

    private fun hasBluetoothPermission(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            // Pre-12 BLUETOOTH / BLUETOOTH_ADMIN are install-time permissions
            // and are always held if declared.
            true
        }

    private companion object {
        const val TAG = "BootReceiver"

        /**
         * `MY_PACKAGE_REPLACED` is here for the same reason as the boot
         * action: an app update stops the package and clears its alarms, and
         * an app that loses its reminders on every Play update is broken in a
         * way nobody will connect to the update.
         */
        val HANDLED = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            // Some OEM ROMs (notably older HTC/Xiaomi quick-boot paths) send
            // this instead of BOOT_COMPLETED. Harmless where it is not used.
            "android.intent.action.QUICKBOOT_POWERON"
        )
    }
}
