package com.safeshade.repo

import com.safeshade.SmsMessageEventBus
import com.safeshade.data.MessageChannel
import com.safeshade.data.QuickMessage
import com.safeshade.data.SafeShadePreferences
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** How an outbound message actually left the phone. */
sealed interface SendResult {
    data class Sent(val channel: MessageChannel) : SendResult

    /** Neither BLE nor SMS was available. [reason] is user-facing. */
    data class Failed(val reason: String) : SendResult
}

/**
 * Guardian to Companion messages, and the replies coming back.
 *
 * Two things in here are not obvious and both have bitten this codebase:
 *
 *  1. **[DeviceLink.writeGuardianMessage] and [DeviceLink.writeCompanionReply]
 *     are not interchangeable.** They have identical signatures, so the
 *     compiler cannot tell them apart, but they target different
 *     characteristics with different firmware handlers. MESSAGE_CHAR makes the
 *     wearable buzz and display the text as a *new incoming message*;
 *     REPLY_CHAR notifies the guardian. Routing a reply through the message
 *     characteristic — which this app has already shipped once — makes the
 *     wearable buzz at its own wearer with their own reply.
 *
 *  2. **The firmware relays every reply over both channels.** The gateway
 *     sketch notifies REPLY_CHAR over BLE *and* sends the same text as an SMS,
 *     so a phone that is both connected and in cellular range receives each
 *     reply twice, a second or two apart. Without [DEDUPE_WINDOW_MS] the
 *     message list shows every reply twice, which reads as the wearer having
 *     pressed the button twice.
 */
class MessagingRepository(
    private val prefs: SafeShadePreferences,
    private val link: DeviceLink,
    private val scope: CoroutineScope,
    /** Injected rather than calling `SmsManager` here, so this stays testable. */
    private val sendSms: (phone: String, body: String) -> Unit
) {

    /** Null until the first DataStore read completes. See [ProfileRepository]. */
    val messages: StateFlow<List<QuickMessage>?> =
        prefs.messages.stateIn(scope, SharingStarted.Eagerly, null)

    private val listLock = Mutex()

    init {
        // BLE replies.
        link.replies
            .onEach { text -> ingestReply(text, MessageChannel.BLE) }
            .launchIn(scope)

        // SMS replies. SmsMessageEventBus posts *every* incoming text
        // unfiltered — deliberately, since the receiver is a bare
        // BroadcastReceiver with no access to app state — so the sender check
        // has to happen here. Skipping it would let any stranger's text become
        // a reply from the wearer.
        SmsMessageEventBus.events
            .onEach { (sender, body) ->
                val expected = prefs.devicePhoneNumber.first()
                if (expected.isNotBlank() && matchesNumber(sender, expected)) {
                    ingestReply(body, MessageChannel.SMS)
                }
            }
            .launchIn(scope)
    }

    /**
     * Compares phone numbers by their last nine digits.
     *
     * The same SIM arrives as "+919876543210", "919876543210" or "9876543210"
     * depending on the carrier and on how the guardian typed it into settings.
     * An exact string comparison drops real replies on the floor; nine digits
     * is long enough that a false match is not a practical concern.
     */
    private fun matchesNumber(a: String, b: String): Boolean {
        val da = a.filter { it.isDigit() }.takeLast(9)
        val db = b.filter { it.isDigit() }.takeLast(9)
        return da.isNotEmpty() && da == db
    }

    /**
     * Adds an inbound reply unless the same text already arrived on the other
     * channel inside [DEDUPE_WINDOW_MS].
     *
     * The window is eight seconds because that is comfortably longer than the
     * gateway's BLE-then-SMS gap (typically one to three seconds, plus carrier
     * delivery) and comfortably shorter than a human sending the same canned
     * reply twice on purpose. Deduping on text alone with no window would
     * silently swallow a genuine repeated "I'm OK".
     */
    private suspend fun ingestReply(text: String, channel: MessageChannel) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        listLock.withLock {
            val current = prefs.messages.first()
            val now = System.currentTimeMillis()

            val duplicate = current.any { existing ->
                !existing.fromGuardian &&
                    existing.text.equals(clean, ignoreCase = true) &&
                    existing.channel != channel &&
                    now - existing.timestamp <= DEDUPE_WINDOW_MS
            }
            if (duplicate) return@withLock

            val message = QuickMessage(
                text = clean,
                fromGuardian = false,
                timestamp = now,
                channel = channel
            )
            // Mark the most recent unanswered guardian message as replied, so
            // the thread reads as a conversation rather than two lists.
            val updated = current.toMutableList()
            val pendingIndex = updated.indexOfLast { it.fromGuardian && !it.replied }
            if (pendingIndex >= 0) {
                updated[pendingIndex] =
                    updated[pendingIndex].copy(replied = true, replyText = clean)
            }
            prefs.setMessages(updated + message)
        }
    }

    // ============================================
    // Outbound
    // ============================================

    /**
     * Guardian to Companion. Writes MESSAGE_CHAR over BLE, or falls back to SMS.
     *
     * Never route a reply through here — see point 1 in the class doc.
     */
    suspend fun sendGuardianMessage(text: String): SendResult =
        send(text, fromGuardian = true) { link.writeGuardianMessage(it) }

    /**
     * Companion to Guardian. Writes REPLY_CHAR over BLE, or falls back to SMS.
     *
     * Never route a fresh message through here — see point 1 in the class doc.
     */
    suspend fun sendCompanionReply(text: String): SendResult =
        send(text, fromGuardian = false) { link.writeCompanionReply(it) }

    private suspend fun send(
        text: String,
        fromGuardian: Boolean,
        overBle: (String) -> Unit
    ): SendResult {
        // Cleaned even for the SMS path: the wearable parses the text the same
        // way whichever transport carried it, and a comma shifts every later
        // field of whatever payload the firmware folds it into.
        val clean = DeviceProtocol.clean(text, MAX_MESSAGE_CHARS)
        if (clean.isEmpty()) return SendResult.Failed("Nothing to send")

        // Ready, not Connected: MESSAGE_CHAR and REPLY_CHAR are null until
        // service discovery resolves them, and both write paths return
        // silently on a null characteristic. A message that reports success
        // and reaches nobody is worse than one that reports failure.
        val bleUsable = link.connectionState.value is ConnectionState.Ready

        val result = if (bleUsable) {
            overBle(clean)
            SendResult.Sent(MessageChannel.BLE)
        } else {
            val number = prefs.devicePhoneNumber.first()
            if (number.isBlank()) {
                SendResult.Failed("Not connected, and no device SIM number is set")
            } else {
                sendSms(number, clean)
                SendResult.Sent(MessageChannel.SMS)
            }
        }

        if (result is SendResult.Sent) {
            listLock.withLock {
                val current = prefs.messages.first()
                prefs.setMessages(
                    current + QuickMessage(
                        text = clean,
                        fromGuardian = fromGuardian,
                        channel = result.channel
                    )
                )
            }
        }
        return result
    }

    suspend fun clearMessages() {
        listLock.withLock { prefs.setMessages(emptyList()) }
    }

    private companion object {
        const val DEDUPE_WINDOW_MS = 8_000L

        /** The wearable's message screen cannot show more than this. */
        const val MAX_MESSAGE_CHARS = 120
    }
}
