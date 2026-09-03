/**
 * SafeShade - Universal Safety Companion
 *
 * EmergencyActions.kt
 *
 * Real phone-call and SMS placing logic for confirmed falls / SOS. Nothing
 * like this existed before this pass — SafetySettings.autoCallEmergency was
 * a toggle wired all the way to the firmware but backed by zero actual
 * call-placing code, so it was a false promise in the UI. Both functions
 * here are only ever invoked after explicit user confirmation (see
 * FallAlertDialog's countdown in SafeShadeApp.kt) - never silently.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.safeshade.data.EmergencyContact

/** Places a real call to [contact]. Falls back to ACTION_DIAL (user must tap call) if CALL_PHONE isn't granted. */
fun placeEmergencyCall(context: Context, contact: EmergencyContact) {
    val hasCallPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    val uri = Uri.parse("tel:${contact.phone}")
    val intent = Intent(
        if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL,
        uri
    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    runCatching { context.startActivity(intent) }
        .onFailure { Log.e("EMERGENCY_CALL", "Failed to start call intent", it) }
}

/** Sends a BLE-independent SMS fallback alert to [contact]. Requires SEND_SMS; silently no-ops (logged) if missing. */
fun sendEmergencySms(context: Context, contact: EmergencyContact, message: String) {
    val hasSmsPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.SEND_SMS
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasSmsPermission) {
        Log.e("EMERGENCY_SMS", "SEND_SMS not granted, cannot send fallback SMS")
        return
    }

    runCatching {
        val smsManager = context.getSystemService(SmsManager::class.java)
            ?: @Suppress("DEPRECATION") SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
    }.onFailure { Log.e("EMERGENCY_SMS", "Failed to send SMS - likely no cellular signal", it) }
}
