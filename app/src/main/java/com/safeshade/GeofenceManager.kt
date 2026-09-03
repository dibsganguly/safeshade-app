/**
 * SafeShade - Universal Safety Companion
 *
 * GeofenceManager.kt
 *
 * Real safe-zone enter/exit alerts (item #11 of the feature roadmap),
 * replacing GuardianScreen's static "COMING SOON" placeholder. Uses
 * Android's Geofencing API (GeofencingClient), independent of the BLE
 * link - alerts fire from phone location, not the wearable.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.safeshade.data.GeofenceZone

class GeofenceManager(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Replaces all currently-registered geofences with [zones]. Requires
     * ACCESS_FINE_LOCATION (and ACCESS_BACKGROUND_LOCATION for exit alerts
     * to fire while backgrounded) already granted - callers must check
     * before calling this.
     */
    @SuppressLint("MissingPermission")
    fun syncZones(zones: List<GeofenceZone>, onResult: (Boolean) -> Unit = {}) {
        geofencingClient.removeGeofences(geofencePendingIntent).addOnCompleteListener {
            if (zones.isEmpty()) {
                onResult(true)
                return@addOnCompleteListener
            }

            val geofences = zones.map { zone ->
                val transitionTypes = when {
                    zone.alertOnEnter && zone.alertOnExit -> Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                    zone.alertOnEnter -> Geofence.GEOFENCE_TRANSITION_ENTER
                    else -> Geofence.GEOFENCE_TRANSITION_EXIT
                }
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(zone.lat, zone.lon, zone.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(transitionTypes)
                    .build()
            }

            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .addGeofences(geofences)
                .build()

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("GEOFENCE", "Registered ${zones.size} zone(s)")
                    onResult(true)
                }
                .addOnFailureListener { e ->
                    Log.e("GEOFENCE", "Failed to register zones", e)
                    onResult(false)
                }
        }
    }

    fun clearZones() {
        geofencingClient.removeGeofences(geofencePendingIntent)
    }
}
