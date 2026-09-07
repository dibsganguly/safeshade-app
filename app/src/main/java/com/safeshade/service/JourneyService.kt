package com.safeshade.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.safeshade.MainActivity
import com.safeshade.SafeShadeApplication
import com.safeshade.alerts.Channels
import com.safeshade.alerts.NotificationIds
import com.safeshade.data.JourneyState
import com.safeshade.data.LocationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Location sampling for the duration of a "walk with me" journey.
 *
 * ### Why this is a second service rather than a flag on [LinkService]
 *
 * This is the single most important structural decision in this layer, and it
 * is not tidiness — merging the two would break the link.
 *
 * On API 34+ a foreground service whose type includes `location` **cannot be
 * started while the app is in the background**: the platform throws
 * `ForegroundServiceStartNotAllowedException`, and the `BOOT_COMPLETED` /
 * high-priority-message exemptions do not extend to that type. [LinkService]
 * must be startable from the background — after a reboot, on a reconnect, when
 * an SMS wakes the process. If the two shared one service, that one service
 * would carry the `location` type, and every background restart of the link
 * would be refused. The link would then only ever run while somebody had the
 * app open, which is precisely the case where it is not needed.
 *
 * So: [LinkService] is `connectedDevice` and may start from anywhere;
 * [JourneyService] is `location` and is started **only** from an in-app tap on
 * a screen the user is looking at. Nothing — not [com.safeshade.alarms.BootReceiver],
 * not a geofence, not an alarm — may start this one.
 *
 * ### What it actually does
 *
 * Samples position roughly once a minute and publishes it to
 * [LastKnownLocation], so that a journey that runs overdue can put a real
 * coordinate into the SMS that goes to the emergency contacts. Sampling stops
 * the moment the journey leaves [JourneyState.ACTIVE].
 */
class JourneyService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var fused: FusedLocationProviderClient? = null
    private var observing = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val fix = result.lastLocation ?: return
            LastKnownLocation.update(fix)
            RideLogSink.onFix(
                lat = fix.latitude,
                lon = fix.longitude,
                speedMps = if (fix.hasSpeed()) fix.speed else null,
                accuracyM = if (fix.hasAccuracy()) fix.accuracy else null,
                at = fix.time
            )
            refreshNotification()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Channels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        /*
         * On API 34+ startForeground with FOREGROUND_SERVICE_TYPE_LOCATION
         * throws a SecurityException outright if ACCESS_FINE_LOCATION is not
         * held at that instant. Checking first turns a crash into a no-op the
         * UI can explain, which is the whole difference between "the journey
         * feature is unavailable" and "the app closed itself".
         */
        if (!hasLocationPermission()) {
            Log.w(TAG, "No location permission; journey tracking cannot start")
            stopSelf()
            return START_NOT_STICKY
        }

        if (!promote()) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!observing) {
            observing = true
            startSampling()
            observeJourney()
        }

        /*
         * START_NOT_STICKY, unlike LinkService. If the OS killed us the
         * journey may well be over, and silently resurrecting a location
         * sampler the user never re-authorised is worse than stopping. The
         * escalation deadline is an AlarmManager alarm and survives
         * independently of this service — that is the part that must not be
         * lost, and it is not lost.
         */
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        runCatching { fused?.removeLocationUpdates(callback) }
        scope.cancel()
        super.onDestroy()
    }

    // ============================================
    // Sampling
    // ============================================

    @SuppressLint("MissingPermission") // checked in onStartCommand
    private fun startSampling() {
        val client = LocationServices.getFusedLocationProviderClient(this)
        fused = client

        // Seed from the platform's cached fix so the first minute of a journey
        // is not a blank. It costs nothing and may be seconds old.
        runCatching {
            client.lastLocation.addOnSuccessListener { it?.let(LastKnownLocation::update) }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, SAMPLE_MS)
            // Accept an earlier fix if another app happens to ask for one —
            // free accuracy, no extra battery.
            .setMinUpdateIntervalMillis(SAMPLE_MS / 2)
            // Doze will batch these anyway; asking for tighter delivery than
            // the product needs just costs the wearer battery on a walk.
            .setMaxUpdateDelayMillis(SAMPLE_MS * 2)
            .build()

        runCatching {
            client.requestLocationUpdates(request, callback, mainLooper)
        }.onFailure { Log.e(TAG, "Could not request location updates", it) }
    }

    /**
     * Stops as soon as the journey is no longer running, and brackets the
     * ride log around the same lifetime through [RideLogSink]: every journey
     * is a ride (there is no per-mode gate here — see `RideLogRepository`),
     * it begins the moment the journey is first seen ACTIVE and ends the
     * moment it leaves ACTIVE, so a ride's duration always matches the
     * journey it was walked for.
     */
    private fun observeJourney() {
        val container = (applicationContext as? SafeShadeApplication)?.container ?: return
        var rideActive = false
        container.journeyRepository.journey
            .onEach { journey ->
                if (journey != null && journey.state == JourneyState.ACTIVE) {
                    if (!rideActive) {
                        rideActive = true
                        RideLogSink.begin(journey.id)
                    }
                    refreshNotification()
                } else {
                    if (rideActive) {
                        rideActive = false
                        RideLogSink.end()
                    }
                    if (container.journeyRepository.loaded.value) {
                        // `loaded` guards the cold-start case: `journey` is
                        // null both for "no journey" and for "DataStore has
                        // not answered yet", and stopping on the second would
                        // end the service a frame after it started.
                        stopSelf()
                    }
                }
            }
            .launchIn(scope)
    }

    // ============================================
    // Notification
    // ============================================

    private fun promote(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NotificationIds.JOURNEY_SERVICE,
            build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                0
            }
        )
        true
    } catch (e: IllegalStateException) {
        // Base type deliberately; see the same catch in LinkService.
        Log.e(TAG, "Refused foreground start", e)
        false
    } catch (e: SecurityException) {
        Log.e(TAG, "Missing permission for location FGS type", e)
        false
    }

    private fun refreshNotification() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this)
                .notify(NotificationIds.JOURNEY_SERVICE, build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification update refused", e)
        }
    }

    private fun build(): Notification {
        val journey = (applicationContext as? SafeShadeApplication)
            ?.container?.journeyRepository?.journey?.value
        val label = journey?.label?.takeIf { it.isNotBlank() } ?: "Journey"
        val fix = LastKnownLocation.state.value

        val open = PendingIntent.getActivity(
            this,
            RC_CONTENT,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(LinkService.EXTRA_ROUTE, ROUTE_JOURNEY)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Channels.LINK)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Walking with you – $label")
            .setContentText(
                if (fix?.isValid == true) {
                    "Location shared with your contacts if you run late"
                } else {
                    "Waiting for a location fix"
                }
            )
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop sharing",
                PendingIntent.getService(
                    this,
                    RC_STOP,
                    Intent(this, JourneyService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        private const val TAG = "JourneyService"

        const val ACTION_STOP = "com.safeshade.action.JOURNEY_STOP"

        /** Mirrors `Routes.CIRCLE_JOURNEY`; see the note in [LinkService]. */
        const val ROUTE_JOURNEY = "circle/journey"

        private const val SAMPLE_MS = 60_000L
        private const val RC_CONTENT = 6201
        private const val RC_STOP = 6202

        /**
         * Starts journey tracking.
         *
         * **Only ever call this from a foreground, user-initiated tap.** On
         * API 34+ a background call is refused by the platform, and on every
         * version it is the wrong thing: this service turns on the GPS.
         */
        fun start(context: Context): Boolean {
            val intent = Intent(context.applicationContext, JourneyService::class.java)
            return try {
                context.applicationContext.startForegroundService(intent)
                true
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Background start of a location FGS refused — this is expected", e)
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "Location FGS start refused", e)
                false
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, JourneyService::class.java)
                )
            }
        }
    }
}

/**
 * The most recent position this phone reported for itself.
 *
 * Process-lifetime only, and deliberately so. It exists to put a coordinate
 * into an escalation SMS, and a fix from before the process last died is not a
 * coordinate worth sending someone to. A receiver waking in a fresh process
 * finds this empty and falls back to
 * `FusedLocationProviderClient.getLastLocation()`, which is the platform's own
 * cache and the honest answer to "where were they last seen".
 *
 * Kept out of DataStore for the same reason: persisting it would make a
 * three-hour-old fix indistinguishable from a fresh one at exactly the moment
 * somebody is deciding where to drive.
 */
object LastKnownLocation {

    private val _state = MutableStateFlow<LocationState?>(null)
    val state: StateFlow<LocationState?> = _state.asStateFlow()

    fun update(location: Location) {
        _state.value = LocationState(
            lat = location.latitude,
            lon = location.longitude,
            altitude = location.altitude.toInt(),
            isValid = true,
            capturedAt = System.currentTimeMillis(),
            provider = location.provider,
            accuracyM = if (location.hasAccuracy()) location.accuracy else null,
            fixAt = location.time,
            fromDevice = false
        )
    }

    fun clear() {
        _state.value = null
    }
}

/**
 * Where a journey's begin/end and location fixes go so the ride log can be
 * built from them.
 *
 * Mirrors [LastKnownLocation]: a process-wide, plumbing-free target so
 * [JourneyService] — started by the platform, outside the DI graph — can hand
 * ride events on to `RideLogRepository` without holding a reference to
 * `AppContainer` itself. `AppContainer` is the one place that calls [attach],
 * once, pointing this at the real repository
 * (`RideLogSink.attach(rideLogRepository)`); before that call every event is
 * silently dropped, which is correct — one arriving before the container
 * exists has nowhere real to be logged.
 */
object RideLogSink {

    private var repository: com.safeshade.repo.RideLogRepository? = null

    /** Called once, from `AppContainer`, to point this at the real repository. */
    fun attach(repository: com.safeshade.repo.RideLogRepository) {
        this.repository = repository
    }

    fun begin(journeyId: String?) {
        repository?.begin(journeyId)
    }

    fun onFix(lat: Double, lon: Double, speedMps: Float?, accuracyM: Float?, at: Long) {
        repository?.addFix(lat, lon, speedMps, accuracyM, at)
    }

    suspend fun end() {
        repository?.end()
    }
}
