package com.safeshade.platform

import com.safeshade.data.SmartHomeProviders
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The body a smart-home hook sends, and the signature over it.
 *
 * Pure - no Android, no network, no clock of its own. That is the point: the
 * receiving end of these calls is somebody's house, and the two things that
 * decide whether the house does the right thing are the exact bytes of the
 * body and the exact bytes the signature covers. Both are testable here on the
 * JVM, and both are tested.
 *
 * ### The headers the poster sends
 *
 * - `X-SafeShade-Signature` - lowercase hex HMAC-SHA256, present only when the
 *   hook has a secret.
 * - `X-SafeShade-Timestamp` - the same milliseconds value the signature covers.
 * - `X-SafeShade-Event` - the trigger name, so a receiver can route without
 *   parsing the body.
 * - `Content-Type: application/json`.
 *
 * A receiver verifies by recomputing the HMAC over `"$timestamp.$body"` with
 * the shared secret and comparing. The timestamp is inside the signed string
 * rather than beside it so an old call cannot be replayed with a fresh
 * timestamp header.
 *
 * ### The body keys are a contract
 *
 * Home Assistant users write automation templates against
 * `trigger.json.wearer`, `trigger.json.event` and the rest. Renaming a key
 * here silently breaks an automation in somebody's house, where the failure
 * looks like a light that stopped coming on. Add keys; do not rename them.
 *
 * ### Why the JSON is hand-rolled
 *
 * `org.json` ships with Android but the classes on the unit-test classpath are
 * the stubs, which throw `RuntimeException("Stub!")` on every call. A body
 * built with `JSONObject` could therefore never be asserted on in a JVM test -
 * and an untested escaper is exactly how a wearer named `O"Brien` turns into a
 * body no receiver can parse. So the encoder is thirty lines below, and it is
 * tested.
 */
object WebhookSigner {

    /**
     * Hex HMAC-SHA256 over `"$timestampMs.$body"`, lowercase.
     *
     * @param secret the hook's shared key. Never logged, never echoed.
     */
    fun sign(secret: String, timestampMs: Long, body: String): String {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), HMAC_SHA256))
        val digest = mac.doFinal("$timestampMs.$body".toByteArray(Charsets.UTF_8))
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val v = byte.toInt() and 0xFF
                append(HEX[v ushr 4])
                append(HEX[v and 0x0F])
            }
        }
    }

    /**
     * The JSON body for [event], in the shape [provider] expects.
     *
     * `webhook` and `home_assistant` get the same object: a Home Assistant
     * webhook trigger accepts any JSON and exposes it as `trigger.json`, so
     * there is nothing to translate and a second shape would be a second thing
     * to keep in step.
     *
     * `ifttt` gets IFTTT Webhooks' own three-field shape, because that service
     * reads nothing else - anything not called `value1`, `value2` or `value3`
     * is dropped on the floor by their ingest.
     *
     * `matter` has no body to send; see `SmartHomeApps` for what this build
     * does about Matter. This returns the plain object for it so that a caller
     * that does try has something well-formed rather than an empty string.
     */
    fun bodyFor(event: SmartHomeEvent, provider: String): String =
        if (provider == SmartHomeProviders.IFTTT) iftttBody(event) else bodyFor(event)

    /**
     * The SafeShade body: the shape `webhook` and `home_assistant` receive.
     *
     * Keys, and what a receiver may rely on:
     * - `event` - the trigger name, one of `SmartHomeTriggers.ALL`.
     * - `wearer` - the person's name, or an empty string when the app has none.
     *   Never a placeholder name.
     * - `at` - milliseconds since the epoch, UTC.
     * - `lat` / `lon` - a real fix when one was attached to what happened, and
     *   JSON `null` otherwise. Null means the app did not have a position, not
     *   that the person is at zero.
     * - `detail` - one plain sentence about what happened, or `null`.
     */
    fun bodyFor(event: SmartHomeEvent): String = buildString {
        append('{')
        appendString("event", event.trigger)
        append(',')
        appendString("wearer", event.wearerName)
        append(',')
        append("\"at\":").append(event.at)
        append(",\"lat\":").append(event.lat?.toString() ?: "null")
        append(",\"lon\":").append(event.lon?.toString() ?: "null")
        append(",\"detail\":").append(event.detail?.let { encodeString(it) } ?: "null")
        append('}')
    }

    /**
     * IFTTT Webhooks' three ingredients.
     *
     * `value3` carries the detail when there is one, and otherwise a maps link
     * for the position, because an IFTTT notification with a tappable location
     * is worth more than one with an empty third ingredient. When there is
     * neither, it is an empty string - IFTTT renders a missing ingredient as
     * the literal text "value3", which is worse than nothing.
     */
    fun iftttBody(event: SmartHomeEvent): String = buildString {
        append('{')
        appendString("value1", event.wearerName)
        append(',')
        appendString("value2", event.trigger)
        append(',')
        appendString("value3", event.detail ?: mapsUrl(event) ?: "")
        append('}')
    }

    /**
     * A maps link for the event's position, or null when it has none.
     *
     * Google's `?q=lat,lon` form, which every phone that has a maps app will
     * open and every browser will render.
     */
    fun mapsUrl(event: SmartHomeEvent): String? {
        val la = event.lat ?: return null
        val lo = event.lon ?: return null
        return "https://maps.google.com/?q=$la,$lo"
    }

    /**
     * One JSON string literal, quotes included, with every character JSON
     * requires escaped.
     *
     * The `< 0x20` sweep is the part that matters: a stray newline or tab in a
     * name or a note - and a note is free text typed by a person - produces a
     * body that fails to parse at the far end, which reads to the user as a
     * hook that silently does nothing.
     */
    fun encodeString(value: String): String = buildString(value.length + 2) {
        append('"')
        for (ch in value) {
            when {
                ch == '"' -> append("\\\"")
                ch == '\\' -> append("\\\\")
                ch == '\b' -> append("\\b")
                ch == '\u000C' -> append("\\f")
                ch == '\n' -> append("\\n")
                ch == '\r' -> append("\\r")
                ch == '\t' -> append("\\t")
                ch < ' ' -> append("\\u%04x".format(ch.code))
                else -> append(ch)
            }
        }
        append('"')
    }

    private fun StringBuilder.appendString(key: String, value: String) {
        append(encodeString(key)).append(':').append(encodeString(value))
    }

    private const val HMAC_SHA256 = "HmacSHA256"
    private val HEX = "0123456789abcdef".toCharArray()
}

/**
 * What happened, in the words the house is told it in.
 *
 * @param trigger one of `SmartHomeTriggers.ALL`.
 * @param wearerName the person it happened to, or an empty string when the app
 *   does not know a name. Never a stand-in name.
 * @param at milliseconds since the epoch.
 * @param lat a real fix, or null. Null is never rendered as zero.
 * @param detail one plain sentence, or null.
 */
data class SmartHomeEvent(
    val trigger: String,
    val wearerName: String,
    val at: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val detail: String? = null
)
