/**
 * SafeShade - Universal Safety Companion
 *
 * SmsReceiver.kt
 *
 * Real device-independent messaging, receive side. Fired by the OS for
 * every incoming SMS. This is how a hardware reply (or a stranger texting
 * the SIM) reaches the app's message history even while BLE is
 * disconnected - the same wearable SIM the gateway firmware already
 * receives/forwards SMS through (see SafeShade_Gateway.ino's
 * pollIncomingSms()/handleReply()). Posts unfiltered to
 * SmsMessageEventBus - see its doc comment for why filtering happens in
 * SafeShadeApp instead of here.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        if (body.isBlank()) return

        SmsMessageEventBus.post(sender, body)
    }
}
