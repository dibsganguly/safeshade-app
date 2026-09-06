package com.safeshade.cloud.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log

/**
 * Tells the sync engine the moment a usable network appears.
 *
 * ### Why a callback rather than a poll
 *
 * The alternative is a periodic drain, and a periodic drain is wrong in both
 * directions at once: too slow to get a fall alert into the cloud while it
 * still matters, and too frequent to run all day on a phone that is in a lift.
 * `registerDefaultNetworkCallback` fires within a second of the radio
 * associating, costs nothing while there is no network, and is the difference
 * between "your guardian was emailed as you left the car park" and "…twenty
 * minutes later".
 *
 * ### It is allowed to not work
 *
 * `registerDefaultNetworkCallback` requires `ACCESS_NETWORK_STATE`, and **this
 * build does not have it.** Checked, not assumed: the merged manifest at
 * `app/build/intermediates/merged_manifests/debug/processDebugManifest/AndroidManifest.xml`
 * lists nineteen permissions and that is not one of them — no dependency
 * contributes it either.
 *
 * So today [start] returns false, [SyncEngine.reconnectTriggerArmed] stays
 * false, and sync happens on app foreground and on every explicit `kick()`
 * only. Sync is slower; nothing breaks. Adding
 * `<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />`
 * to `AndroidManifest.xml` is the whole fix — it is a normal permission, granted
 * at install with no runtime prompt — and it was out of scope for the pass that
 * wrote this file.
 *
 * The registration stays wrapped even after that, because the alternative —
 * letting a `SecurityException` escape — would take down the process on a build
 * where a manifest line was removed by somebody tidying up.
 *
 * **`reconnectTriggerArmed` must be honoured by any UI that describes this.**
 * Telling a user "SafeShade syncs when you reconnect" while this returns false
 * is exactly the class of claim this codebase has already shipped once and had
 * to fix.
 */
class Connectivity(context: Context) {

    private val appContext = context.applicationContext
    private var callback: ConnectivityManager.NetworkCallback? = null

    /**
     * Calls [onAvailable] each time a default network becomes available.
     *
     * @return true when the callback actually registered. False means the
     *   permission is missing or the service is unavailable, and the caller
     *   should not claim the app syncs on reconnect.
     */
    fun start(onAvailable: () -> Unit): Boolean {
        if (callback != null) return true
        val manager = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                onAvailable()
            }
        }
        return runCatching {
            manager.registerDefaultNetworkCallback(cb)
            callback = cb
            true
        }.getOrElse { t ->
            // SecurityException when ACCESS_NETWORK_STATE is not granted;
            // anything else is a device quirk. Either way this is a downgrade,
            // not a fault, so it is logged and not thrown.
            Log.w(TAG, "No connectivity trigger: ${t.javaClass.simpleName}")
            false
        }
    }

    fun stop() {
        val cb = callback ?: return
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        runCatching { manager?.unregisterNetworkCallback(cb) }
        callback = null
    }

    private companion object {
        const val TAG = "SafeShadeConnectivity"
    }
}
