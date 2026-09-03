/**
 * SafeShade - Universal Safety Companion
 *
 * GeofenceBroadcastReceiver.kt
 *
 * Fired by the OS when a registered safe zone (see GeofenceManager) is
 * entered or exited. Posts a local notification - there's no cloud/relay
 * backend, so this only works while this phone (the Guardian's, since
 * that's who defines safe zones) is nearby to receive it.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

private const val CHANNEL_ID = "safeshade_geofence"
private const val NOTIFICATION_ID_BASE = 2000

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transitionLabel = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> "entered"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "left"
            else -> return
        }

        val zoneIds = event.triggeringGeofences?.map { it.requestId } ?: return

        // Real device sync - see GeofenceEventBus's doc comment for why an
        // event bus (this receiver has no reference to the running app's
        // BleManager instance).
        val isInside = event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
        zoneIds.forEach { zoneId -> GeofenceEventBus.post(zoneId, isInside) }

        ensureChannel(context)

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        zoneIds.forEachIndexed { index, zoneId ->
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Safe zone alert")
                .setContentText("SafeShade wearer $transitionLabel a safe zone")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            notificationManager?.notify(NOTIFICATION_ID_BASE + zoneId.hashCode() + index, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Geofence alerts", NotificationManager.IMPORTANCE_HIGH)
        )
    }
}
