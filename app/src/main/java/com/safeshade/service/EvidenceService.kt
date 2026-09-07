package com.safeshade.service

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.safeshade.MainActivity
import com.safeshade.alerts.Channels
import com.safeshade.alerts.NotificationIds
import com.safeshade.platform.EvidenceRecorder
import com.safeshade.platform.EvidenceRecordingResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Records the wearer's surroundings for a fixed time after an alert.
 *
 * ### Why this is a third service and not a flag on [LinkService]
 *
 * The same argument [JourneyService] makes about `location`, one step
 * stronger. On API 34+ a foreground service whose type includes `microphone`
 * **cannot be started while the app is in the background** - the platform
 * throws `ForegroundServiceStartNotAllowedException`, and none of the
 * `BOOT_COMPLETED` or high-priority-message exemptions extend to that type.
 * [LinkService] must be startable from the background, so merging the types
 * into one service would make every background restart of the link illegal and
 * the wearable would silently stop being watched.
 *
 * So the rule for this service is absolute and narrower than
 * [JourneyService]'s:
 *
 * **Start it only from an in-app surface the user is looking at** - the alert
 * full-screen activity, or the SOS screen. Never from
 * [com.safeshade.alerts.AlertActionReceiver], never from
 * [com.safeshade.alarms.ReminderReceiver] or [com.safeshade.alarms.BootReceiver],
 * never from a geofence callback, never from an alarm. A receiver has no
 * foreground standing to lend, [start] would return false, and the recording a
 * fall was supposed to make would simply not exist.
 *
 * ### The second 34+ rule
 *
 * `startForeground` with `FOREGROUND_SERVICE_TYPE_MICROPHONE` throws a
 * `SecurityException` outright if `RECORD_AUDIO` is not held **at that
 * instant** - not at install, not when the screen was drawn, at that instant.
 * A grant revoked from the notification shade while the app sat in the
 * background is enough. So the permission is checked before promoting and
 * again before recording, and a missing grant stops the service with a logged
 * reason. That is the whole difference between "the recording did not happen"
 * and "the app closed itself during a fall".
 *
 * ### Where the result goes
 *
 * To [onRecorded], a process-wide sink set once from the `Application`. Same
 * shape as [LastKnownLocation]: this service cannot reach `AppContainer`'s
 * repositories without binding a graph into a class the platform constructs,
 * and the orchestrator that owns those repositories is the right place to
 * decide what a finished clip means.
 */
class EvidenceService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var recorder: EvidenceRecorder? = null
    private var alertId: String? = null
    private var totalMs = 0
    private var running = false

    /**
     * Whether this service instance has already reported an outcome.
     *
     * The guard is not belt-and-braces. [EvidenceRecorder.result] is collected
     * on `Main.immediate`, so the collector resumes *inline* inside the
     * assignment that publishes the result - which means by the time
     * `recorder.stop()` returns to its caller, delivery has already happened.
     * Every caller of `stop()` therefore looks like the first one, and without
     * this flag a Stop tap or an [onDestroy] during a recording would hand the
     * orchestrator the same clip twice, and it would store two rows for one
     * recording.
     */
    private val delivered = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Channels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            // An early stop keeps what was captured; see EvidenceRecorder.stop.
            // Only stop() is called here: the recorder publishes the outcome to
            // its own result flow, the collector armed below picks it up, and
            // that collector is the single place delivery happens. Calling
            // finishWith() here as well would deliver twice - the collector
            // runs inline on Main.immediate, so it has already finished by the
            // time stop() returns - and the orchestrator would store the clip
            // twice.
            val rec = recorder
            if (rec == null) {
                stopSelf()
            } else {
                rec.stop()
            }
            return START_NOT_STICKY
        }

        if (running) {
            // A second alert while a recording runs does not restart it. The
            // microphone is already open on the incident that matters, and
            // tearing that down to start again would lose the seconds nearest
            // to it.
            Log.i(TAG, "A recording is already running; ignoring the new start")
            return START_NOT_STICKY
        }

        if (!hasMicPermission()) {
            Log.w(TAG, "No RECORD_AUDIO grant; evidence recording cannot start")
            // A refusal is reported, not swallowed into Idle. A screen that
            // asked for a recording has to be able to say why there isn't one.
            deliver(EvidenceRecordingResult.Failed("Microphone permission has not been granted"))
            stopSelf()
            return START_NOT_STICKY
        }

        val seconds = (intent?.getIntExtra(EXTRA_DURATION_SECONDS, DEFAULT_SECONDS) ?: DEFAULT_SECONDS)
            .coerceIn(EvidenceRecorder.MIN_DURATION_SECONDS, EvidenceRecorder.MAX_DURATION_SECONDS)
        alertId = intent?.getStringExtra(EXTRA_ALERT_ID)?.takeIf { it.isNotBlank() }
        totalMs = seconds * 1000

        /*
         * startForeground FIRST, before touching MediaRecorder. The platform
         * allows roughly five seconds between startForegroundService and
         * startForeground before killing the process with a
         * ForegroundServiceDidNotStartInTimeException, and opening the
         * microphone is exactly the kind of work that loses that race on a
         * cheap phone under load. Same rule as LinkService.
         */
        if (!promote()) {
            deliver(
                EvidenceRecordingResult.Failed(
                    "The system would not let the recording start in the background"
                )
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val rec = EvidenceRecorder(this)
        val file = rec.start(seconds)
        if (file == null) {
            // start() has already put the reason into its own result flow.
            val outcome = rec.result.value
                ?: EvidenceRecordingResult.Failed("The recording could not be started")
            rec.release()
            finishWith(outcome)
            return START_NOT_STICKY
        }

        recorder = rec
        running = true
        publish(EvidenceServiceState.Recording(0, totalMs, alertId))

        rec.elapsedMs
            .onEach { elapsed ->
                if (running) {
                    publish(EvidenceServiceState.Recording(elapsed, totalMs, alertId))
                    refreshNotification(elapsed)
                }
            }
            .launchIn(scope)

        // The cap fires inside MediaRecorder with nobody watching, so the
        // service learns that a recording ended the same way any other
        // observer does - from the recorder's own result.
        rec.result
            .onEach { outcome -> if (outcome != null && running) finishWith(outcome) }
            .launchIn(scope)

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // A service torn down mid-recording must still release the microphone
        // and report what it got. Dropping the clip silently here would be the
        // one failure this feature cannot afford.
        recorder?.let { rec ->
            if (running) {
                running = false
                deliver(rec.stop())
            }
            rec.release()
        }
        recorder = null
        scope.cancel()
        super.onDestroy()
    }

    private fun finishWith(outcome: EvidenceRecordingResult?) {
        if (!running && outcome == null) {
            stopSelf()
            return
        }
        running = false
        val rec = recorder
        recorder = null
        deliver(outcome ?: EvidenceRecordingResult.Failed("The recording ended without a result"))
        rec?.release()
        stopSelf()
    }

    private fun deliver(outcome: EvidenceRecordingResult) {
        if (!delivered.compareAndSet(false, true)) return
        publish(EvidenceServiceState.Finished(outcome))
        val sink = onRecorded
        if (sink == null) {
            Log.w(TAG, "No onRecorded sink is set; the clip was recorded but nothing stored it")
            return
        }
        runCatching { sink(outcome, alertId) }
            .onFailure { Log.e(TAG, "The onRecorded sink threw", it) }
    }

    // ============================================
    // Notification
    // ============================================

    private fun promote(): Boolean = try {
        ServiceCompat.startForeground(
            this,
            NotificationIds.EVIDENCE_SERVICE,
            build(0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            } else {
                0
            }
        )
        true
    } catch (e: IllegalStateException) {
        // The base type deliberately, as in LinkService and JourneyService:
        // ForegroundServiceStartNotAllowedException only exists on 31+, and
        // catching it by name would need an API guard around a catch clause.
        Log.e(TAG, "Refused foreground start for the microphone service", e)
        false
    } catch (e: SecurityException) {
        // The 34+ rule in the class doc: RECORD_AUDIO was revoked between the
        // check above and this call.
        Log.e(TAG, "Microphone permission is missing for the microphone FGS type", e)
        false
    }

    private fun refreshNotification(elapsedMs: Int) {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return
        try {
            NotificationManagerCompat.from(this)
                .notify(NotificationIds.EVIDENCE_SERVICE, build(elapsedMs))
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification update refused", e)
        }
    }

    private fun build(elapsedMs: Int): Notification {
        val remaining = ((totalMs - elapsedMs).coerceAtLeast(0) + 999) / 1000

        val open = PendingIntent.getActivity(
            this,
            RC_CONTENT,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, Channels.EVIDENCE)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Recording for evidence")
            .setContentText(
                if (remaining > 0) {
                    "The microphone is on for another $remaining s"
                } else {
                    "Finishing the recording"
                }
            )
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                PendingIntent.getService(
                    this,
                    RC_STOP,
                    Intent(this, EvidenceService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun publish(next: EvidenceServiceState) {
        _state.value = next
    }

    companion object {
        private const val TAG = "EvidenceService"

        const val ACTION_STOP = "com.safeshade.action.EVIDENCE_STOP"
        const val EXTRA_ALERT_ID = "com.safeshade.extra.EVIDENCE_ALERT_ID"
        const val EXTRA_DURATION_SECONDS = "com.safeshade.extra.EVIDENCE_SECONDS"

        private const val DEFAULT_SECONDS = 30
        private const val RC_CONTENT = 6301
        private const val RC_STOP = 6302

        private val _state = MutableStateFlow<EvidenceServiceState>(EvidenceServiceState.Idle)

        /** What the recording is doing, for the UI to draw. */
        val state: StateFlow<EvidenceServiceState> = _state.asStateFlow()

        /**
         * Where a finished recording goes.
         *
         * Set once, from `SafeShadeApplication.onCreate`, to something that
         * hands the result to `EvidenceRepository`. A process-wide sink rather
         * than a bound service because the platform constructs this class and
         * there is no constructor to inject a repository into - the same
         * compromise [LastKnownLocation] makes, and acceptable for the same
         * reason: one writer, set before any reader can run.
         *
         * Null means a clip was recorded and nothing stored it; the service
         * logs that rather than pretending it was saved.
         */
        @Volatile
        @JvmStatic
        var onRecorded: ((EvidenceRecordingResult, String?) -> Unit)? = null

        /**
         * Starts a recording.
         *
         * **Only ever call this from a foreground surface the user is looking
         * at** - the alert full-screen activity or the SOS screen. See the
         * class doc: from a receiver or an alarm this returns false on API
         * 34+, and on every version it is the wrong thing, because it opens
         * the microphone.
         *
         * @return false when the microphone permission is missing or the
         *   platform refuses the start. The reason is logged; the caller must
         *   say something honest rather than show a recording that is not
         *   happening.
         */
        fun start(context: Context, alertId: String?, durationSeconds: Int): Boolean {
            val app = context.applicationContext

            if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "Evidence recording not started: RECORD_AUDIO has not been granted")
                return false
            }

            val intent = Intent(app, EvidenceService::class.java).apply {
                putExtra(EXTRA_ALERT_ID, alertId)
                putExtra(
                    EXTRA_DURATION_SECONDS,
                    durationSeconds.coerceIn(
                        EvidenceRecorder.MIN_DURATION_SECONDS,
                        EvidenceRecorder.MAX_DURATION_SECONDS
                    )
                )
            }

            return try {
                app.startForegroundService(intent)
                true
            } catch (e: IllegalStateException) {
                val background = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e is ForegroundServiceStartNotAllowedException
                if (background) {
                    Log.w(
                        TAG,
                        "Background start of a microphone FGS refused - this service may only " +
                            "be started from a screen the user is looking at",
                        e
                    )
                } else {
                    Log.w(TAG, "Evidence service start refused", e)
                }
                false
            } catch (e: SecurityException) {
                Log.w(TAG, "Microphone FGS start refused", e)
                false
            }
        }

        fun stop(context: Context) {
            runCatching {
                context.applicationContext.startService(
                    Intent(context.applicationContext, EvidenceService::class.java)
                        .setAction(ACTION_STOP)
                )
            }
        }
    }
}

/** What [EvidenceService] is doing, for the UI to draw. */
sealed interface EvidenceServiceState {

    data object Idle : EvidenceServiceState

    data class Recording(val elapsedMs: Int, val totalMs: Int, val alertId: String?) :
        EvidenceServiceState

    /** The last recording's outcome, success or failure. Never cleared to Idle silently. */
    data class Finished(val result: EvidenceRecordingResult) : EvidenceServiceState
}
