package com.safeshade

import android.app.Application
import com.safeshade.alerts.AlertBridge
import com.safeshade.alerts.Channels
import com.safeshade.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Owns the one thing in this app that must outlive every screen: the
 * application scope, and the object graph hanging off it.
 *
 * ### Why the scope lives here
 *
 * Fall alerts, geofence forwarding, check-in escalation and journey overdue all
 * fire when there is no ViewModel and no Activity — a phone in a pocket with
 * the screen off is the normal case for every one of them. A `viewModelScope`
 * would cancel each of those collectors at exactly the moment they are needed,
 * and it would do so silently: no crash, no log, just alerts that never arrive.
 *
 * ### Why `SupervisorJob`
 *
 * The children are independent. If the telemetry ring's persistence coroutine
 * throws, that must not cancel the collector listening for falls. A plain `Job`
 * propagates the failure to every sibling; a `SupervisorJob` contains it.
 *
 * ### Why `Dispatchers.Default`
 *
 * Nothing in the repository layer touches a View. DataStore does its own I/O
 * dispatch internally, and the rest is JSON decoding and flow plumbing, which
 * is CPU work that has no business on the main thread. Anything that genuinely
 * needs the main thread is a Compose collector, which switches for itself.
 *
 * ### Registration
 *
 * This class only runs if `AndroidManifest.xml` names it:
 *
 * ```xml
 * <application android:name=".SafeShadeApplication" ... >
 * ```
 *
 * Without that attribute Android instantiates the base `Application`, this
 * class is never constructed, and the entire graph below silently does not
 * exist.
 */
class SafeShadeApplication : Application() {

    /**
     * Never cancelled. Its lifetime is the process's lifetime, and the OS
     * reclaims it by killing the process — there is no later moment at which
     * cancelling would be more correct than letting it die with everything
     * else.
     */
    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, applicationScope)

        // Channels must exist before anything tries to post to them, and
        // creating them is idempotent, so the earliest possible moment is the
        // right one.
        Channels.ensureCreated(this)

        // The wire between a detected fall and a notification the user can
        // actually see. Started here, on the application scope, because the
        // case it exists for is a locked phone with no Activity alive.
        AlertBridge.start(this, container.safetyRepository, applicationScope)
    }
}
