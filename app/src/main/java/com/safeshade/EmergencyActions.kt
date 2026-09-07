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
import com.safeshade.platform.PhoneNumbers

/**
 * Real phone-call and SMS actions.
 *
 * Everything here fires only after explicit user confirmation or an expired
 * countdown the user could see and cancel — nothing dials silently.
 *
 * Every function returns whether it actually did the thing. The previous
 * version returned `Unit` and logged failures, which meant a missing SEND_SMS
 * permission produced a UI that looked like it had sent an emergency alert and
 * a logcat line nobody was reading. On a safety product a silent failure is the
 * worst possible outcome, so callers now get a result they are expected to
 * surface.
 */

/** Why an action could not be completed, for the caller to show. */
sealed interface ActionResult {
    data object Sent : ActionResult
    data object Started : ActionResult
    data class PermissionMissing(val permission: String) : ActionResult
    data class Failed(val reason: String) : ActionResult

    val succeeded: Boolean get() = this is Sent || this is Started
}

/**
 * Calls [contact].
 *
 * Falls back to ACTION_DIAL when CALL_PHONE is not granted, which pre-fills the
 * dialer and leaves the final tap to the user. That is a degradation worth
 * having rather than a failure — in an emergency a pre-filled dialer is still
 * most of the way there.
 */
fun placeEmergencyCall(context: Context, contact: EmergencyContact): ActionResult =
    dialNumber(context, contact.phone, allowDirectCall = true)

/**
 * Opens or places a call to a bare number.
 *
 * @param allowDirectCall when false, always uses ACTION_DIAL. The emergency
 *   services directory passes false deliberately: auto-dialling 112 on a
 *   mis-tap is worse than the extra tap, and the user is looking right at the
 *   screen when they choose one.
 */
fun dialNumber(context: Context, number: String, allowDirectCall: Boolean = false): ActionResult {
    val canCallDirectly = allowDirectCall && ContextCompat.checkSelfPermission(
        context, Manifest.permission.CALL_PHONE
    ) == PackageManager.PERMISSION_GRANTED

    // Normalised at the point of use, not at the point of storage. A number
    // typed as "+91 98765 43210" is correct to display and wrong to hand to a
    // tel: URI, where the spaces percent-encode and some dialers refuse it.
    // Short service numbers (112, 108) contain no spaces and pass through.
    val dialable = PhoneNumbers.dialable(number)
    val intent = Intent(
        if (canCallDirectly) Intent.ACTION_CALL else Intent.ACTION_DIAL,
        Uri.parse("tel:${Uri.encode(dialable)}")
    ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }

    return runCatching {
        context.startActivity(intent)
        ActionResult.Started
    }.getOrElse {
        Log.e("EMERGENCY_CALL", "Failed to start call intent", it)
        ActionResult.Failed("No app on this phone can place calls")
    }
}

/** Sends a link-independent SMS alert to [contact]. */
fun sendEmergencySms(context: Context, contact: EmergencyContact, message: String): ActionResult =
    sendSmsText(context, contact.phone, message, logTag = "EMERGENCY_SMS")

/**
 * Sends a plain SMS.
 *
 * Shared by the emergency fallback and by Guardian↔device messaging when the
 * BLE link is down — the wearable's gateway relays it back out over cellular,
 * so a message still arrives with no Bluetooth at all.
 */
fun sendSmsText(
    context: Context,
    phoneNumber: String,
    message: String,
    logTag: String = "GUARDIAN_SMS"
): ActionResult {
    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.e(logTag, "SEND_SMS not granted, cannot send SMS")
        return ActionResult.PermissionMissing(Manifest.permission.SEND_SMS)
    }

    return runCatching {
        val smsManager = context.getSystemService(SmsManager::class.java)
            ?: @Suppress("DEPRECATION") SmsManager.getDefault()
        // Split rather than truncate: an emergency message carrying a location
        // link routinely exceeds one 160-character segment, and losing the tail
        // would lose the coordinates.
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(PhoneNumbers.dialable(phoneNumber), null, parts, null, null)
        ActionResult.Sent
    }.getOrElse {
        Log.e(logTag, "Failed to send SMS - likely no cellular signal", it)
        ActionResult.Failed("Could not send. Check signal.")
    }
}

/**
 * Opens a location in whatever maps app is installed.
 *
 * Uses the `geo:` scheme with a `q` label so the pin is named rather than being
 * a bare coordinate, and does not assume Google Maps is present.
 */
fun openInMaps(context: Context, lat: Double, lon: Double, label: String): ActionResult {
    val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    return runCatching {
        context.startActivity(intent)
        ActionResult.Started
    }.getOrElse {
        Log.e("MAPS", "No maps app available", it)
        ActionResult.Failed("No maps app installed")
    }
}

/** A plain-text share sheet — used to export a trip log or a medical card. */
/**
 * Offers a PDF from the app's private `files/reports` directory through the
 * share sheet, via the FileProvider declared in the manifest. Like
 * [shareText], a started chooser proves only that the sheet opened.
 */
fun sharePdf(context: Context, file: java.io.File, chooserTitle: String): ActionResult {
    val uri = runCatching {
        androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }.getOrElse { return ActionResult.Failed("The report could not be handed to another app.") }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching {
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        )
        ActionResult.Started
    }.getOrElse { ActionResult.Failed("No app on this phone can take a PDF.") }
}

fun shareText(context: Context, text: String, chooserTitle: String): ActionResult {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return runCatching {
        context.startActivity(
            Intent.createChooser(intent, chooserTitle).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        ActionResult.Started
    }.getOrElse {
        Log.e("SHARE", "Failed to open share sheet", it)
        ActionResult.Failed("Nothing on this phone can share text")
    }
}

/**
 * The text of an emergency alert.
 *
 * Kept here rather than inline at the call sites so the wording of the single
 * most important message this app ever sends is defined once and can be read
 * in one place. Location is appended as a tappable maps link because a
 * recipient reading this on a feature phone still gets usable coordinates.
 */
fun emergencyAlertText(
    wearerName: String,
    what: String,
    lat: Double?,
    lon: Double?
): String = buildString {
    append("SafeShade alert: ")
    append(what)
    if (wearerName.isNotBlank()) {
        append(" – ")
        append(wearerName)
    }
    append(".")
    if (lat != null && lon != null) {
        append(" Last known location: https://maps.google.com/?q=")
        append("%.5f,%.5f".format(lat, lon))
    }
    append(" This is an automated message.")
}
