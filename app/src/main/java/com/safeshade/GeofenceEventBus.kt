/**
 * SafeShade - Universal Safety Companion
 *
 * GeofenceEventBus.kt
 *
 * GeofenceBroadcastReceiver is a standalone Android component instantiated
 * fresh by the OS - it has no reference to the running app's BleManager
 * instance. This is the bridge: the receiver posts real enter/exit events
 * here, and SafeShadeApp (which does hold the live BleManager) collects
 * them and forwards to the device via EXT_CHAR "GEOFENCE:<zone>:<IN|OUT>",
 * turning GuardianScreen's previous "COMING SOON" geofencing card into an
 * actually-working on-device feature (see SCREEN_SAFE_ZONE in the
 * firmware).
 */

package com.safeshade

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object GeofenceEventBus {
    /** zoneId (GeofenceZone.id) to isInside. */
    private val _events = MutableSharedFlow<Pair<String, Boolean>>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun post(zoneId: String, isInside: Boolean) {
        _events.tryEmit(zoneId to isInside)
    }
}
