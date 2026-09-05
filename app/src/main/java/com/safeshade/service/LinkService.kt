package com.safeshade.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import com.safeshade.MainActivity
import com.safeshade.SafeShadeApplication
import com.safeshade.alerts.Channels
import com.safeshade.alerts.NotificationIds
import com.safeshade.device.ConnectionState
import com.safeshade.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The service that makes this product work with the screen off.
 *
 * ### Why it exists at all
 *
 * It does **not** own the link. [AppContainer] does — the `DeviceLink`, the
 * repositories and every collector hang off the application scope and are
 * constructed in `Application.onCreate`. This service adds exactly one thing:
 * a reason for the OS not to kill the process.
 *
 * That is not a theoretical concern. Stock Android puts a backgrounded app
 * into a cached state and reclaims it under memory pressure; MIUI is
 * considerably more aggressive and will do it within minutes of the screen
 * going off even with memory to spare. When the process dies the GATT
 * connection dies with it, and a fall alert then does not arrive late — it
 * never arrives, with no crash, no log and nothing for the user to notice
 * until it matters. A foreground service is the only supported way to say
 * "this process is doing something for the user right now".
 *
 * ### Why the type is `connectedDevice` and not `location`
 *
 * See [JourneyService]. An FGS with the `location` type cannot be started from
 * the background on API 34+, and this one must be startable from the
 * background — a reconnect, a boot, a reply arriving over SMS. Splitting the
 * two types across two services is what keeps that legal.
 *
 * The type passed to `startForeground` must match the `android:foregroundServiceType`
 * in the manifest exactly or the platform throws on API 29+. The required
 * manifest entry is in this task's report.
 */
class LinkService : Service() {

    /**
     * Main-dispatched because everything it does is notification updates.
     * Cancelled in [onDestroy]; it must never outlive the service, unlike the
     * application scope it reads from.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Guards against re-collecting on every redelivered start command. */
    private var observing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Channels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = container()

        /*
         * startForeground FIRST, before anything that can suspend, throw or
         * take time.
         *
         * The platform gives roughly five seconds between `startService` and
         * `startForeground` before it kills the process with a
         * ForegroundServiceDidNotStartInTimeException. Doing flow collection
         * or a DataStore read first is the classic way to lose that race, and
         * it only shows up on a slow cold start — i.e. on the tester's cheapest
         * device, never on the developer's.
         */
        if (!promote(container)) {
            // Could not go foreground (permission revoked mid-flight, or a
            // background-start restriction). Stop rather than sit in the
            // "started but not foreground" state, which the OS kills anyway
            // and which looks identical to working.
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_DISCONNECT -> {
                container?.deviceRepository?.disconnect()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (!observing && container != null) {
            observing = true
            observe(container)
        }

        /*
         * START_STICKY: if the OS kills us for memory it should bring the
         * service back. The redelivered intent is null, which is why every
         * branch above tolerates a null action.
         */
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // ============================================
    // Notification
    // ============================================

    /**
     * Keeps the ongoing notification in step with the link.
     *
     * Reading `.value` off the repository StateFlows is safe *here* — a service
     * lives long enough for the first DataStore/link emission to have landed,
     * and anything it misses arrives on the next emission. That is emphatically
     * not true inside a `BroadcastReceiver`; see the note in
     * `alarms/ReminderReceiver.kt`.
     */
    private fun observe(container: AppContainer) {
        combine(
            container.deviceRepository.connection,
            container.deviceRepository.telemetry,
            container.deviceRepository.deviceName
        ) { state, telemetry, name ->
            Triple(state, telemetry.batteryLevel.takeIf { telemetry.isRealData }, name)
        }
            .distinctUntilChanged()
            .onEach { (state, battery, name) -> notify(build(state, battery, name)) }
            .launchIn(scope)
    }

    /** @return false when the platform refused to let us go foreground. */
    private fun promote(container: AppContainer?): Boolean {
        val state = container?.deviceRepository?.connection?.value ?: ConnectionState.Disconnected
        val telemetry = container?.deviceRepository?.telemetry?.value
        val name = container?.deviceRepository?.deviceName?.value.orEmpty()
        val notification = build(
            state = state,
            battery = telemetry?.batteryLevel?.takeIf { telemetry.isRealData },
            deviceName = name
        )

        return try {
            ServiceCompat.startForeground(
                this,
                NotificationIds.LINK_SERVICE,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                } else {
                    0
                }
            )
            true
        } catch (e: IllegalStateException) {
            /*
             * Caught as IllegalStateException, not as
             * ForegroundServiceStartNotAllowedException: that class only exists
             * on API 31+, and naming it in a catch clause makes the verifier
             * resolve it on older devices. It is a subclass of
             * IllegalStateException, so the base type covers it and is safe
             * everywhere.
             */
            Log.e(TAG, "Refused foreground start", e)
            false
        } catch (e: SecurityException) {
            // API 34+: the declared type's prerequisite permission is missing.
            Log.e(TAG, "Missing permission for connectedDevice FGS type", e)
            false
        }
    }

    private fun build(
        state: ConnectionState,
        battery: Int?,
        deviceName: String
    ): Notification {
        val title = when (state) {
            is ConnectionState.Ready -> "Watching ${deviceName.ifBlank { "your device" }}"
            is ConnectionState.Connected -> "Connecting…"
            is ConnectionState.Connecting -> "Connecting…"
            is ConnectionState.Scanning -> "Looking for your device"
            is ConnectionState.Found -> "Found ${state.name}"
            is ConnectionState.BluetoothUnavailable -> "Bluetooth is off"
            is ConnectionState.ScanFailed -> "Could not scan for the device"
            is ConnectionState.Disconnected -> "Not connected"
        }

        val detail = buildString {
            append(
                when {
                    state.isUsable -> "Link up"
                    state.isBusy -> "Working on it"
                    else -> "SafeShade keeps running so alerts still arrive"
                }
            )
            if (battery != null) append(" · Battery $battery%")
        }

        return NotificationCompat.Builder(this, Channels.LINK)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle(title)
            .setContentText(detail)
            .setContentIntent(openApp(route = null, requestCode = RC_CONTENT))
            // Ongoing + LOW + silent: this notification is a receipt for the
            // process staying alive, not something to read. It must never
            // peek, buzz or badge.
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                serviceAction(ACTION_DISCONNECT, RC_DISCONNECT)
            )
            /*
             * "Locate" opens the app's locate screen rather than calling
             * DeviceLink.ringDevice() directly. Ringing puts the wearable on
             * its full siren + red-LED SOS screen which, per the firmware,
             * clears only when somebody physically presses the button on the
             * device. A one-tap mis-press on a permanently-resident
             * notification would set off a siren on an elderly wearer's cane
             * that nobody nearby can silence. One extra tap is the correct
             * price for that.
             */
            .addAction(
                android.R.drawable.ic_menu_mylocation,
                "Locate",
                openApp(route = ROUTE_DEVICE_LOCATE, requestCode = RC_LOCATE)
            )
            .build()
    }

    private fun notify(notification: Notification) {
        // Degrade, never crash: POST_NOTIFICATIONS can be revoked while the
        // service runs. The service itself survives a revoked permission (the
        // FGS notification is exempt from the runtime permission), but an
        // explicit notify() is not, and it throws.
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this)
                .notify(NotificationIds.LINK_SERVICE, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification update refused", e)
        }
    }

    private fun serviceAction(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, LinkService::class.java).setAction(action)
        return PendingIntent.getForegroundService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun openApp(route: String?, requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (route != null) putExtra(EXTRA_ROUTE, route)
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun container(): AppContainer? =
        (applicationContext as? SafeShadeApplication)?.container

    companion object {
        private const val TAG = "LinkService"

        const val ACTION_DISCONNECT = "com.safeshade.action.LINK_DISCONNECT"
        const val ACTION_STOP = "com.safeshade.action.LINK_STOP"

        /**
         * Optional route for `MainActivity` to navigate to on open.
         *
         * Read by the UI lane if it wants to; ignoring it costs nothing but a
         * tap. Kept here rather than in `Routes` because that file is a
         * declared merge hotspot no lane may edit.
         */
        const val EXTRA_ROUTE = "com.safeshade.extra.ROUTE"

        /** Mirrors `Routes.DEVICE_LOCATE`, duplicated for the reason above. */
        const val ROUTE_DEVICE_LOCATE = "device/locate"

        private const val RC_CONTENT = 6101
        private const val RC_DISCONNECT = 6102
        private const val RC_LOCATE = 6103

        /**
         * Starts the service.
         *
         * `startForegroundService` on API 26+ commits the caller to a
         * `startForeground` within about five seconds — [onStartCommand] does
         * it first thing for exactly that reason.
         *
         * Wrapped because a background start is illegal on API 31+ outside the
         * exemption list. Callers that need to know should check the return.
         */
        fun start(context: Context): Boolean {
            val intent = Intent(context.applicationContext, LinkService::class.java)
            return try {
                context.applicationContext.startForegroundService(intent)
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Background FGS start refused", e)
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "FGS start refused", e)
                false
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context.applicationContext, LinkService::class.java)
            runCatching { context.applicationContext.stopService(intent) }
        }
    }
}
