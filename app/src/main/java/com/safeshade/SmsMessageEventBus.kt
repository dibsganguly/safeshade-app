/**
 * SafeShade - Universal Safety Companion
 *
 * SmsMessageEventBus.kt
 *
 * SmsReceiver is a standalone Android component instantiated fresh by the
 * OS - it has no reference to the running app's state (in particular, the
 * Guardian-configured device phone number needed to tell a real device
 * reply apart from an unrelated text). This is the bridge: the receiver
 * posts every incoming SMS's (sender, body) here unfiltered, and
 * SafeShadeApp (which holds the live devicePhoneNumber) filters by sender
 * and merges matches into messageHistory - the SMS-side mirror of
 * GeofenceEventBus.
 */

package com.safeshade

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SmsMessageEventBus {
    /** (originatingAddress, messageBody). */
    private val _events = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    fun post(sender: String, body: String) {
        _events.tryEmit(sender to body)
    }
}
