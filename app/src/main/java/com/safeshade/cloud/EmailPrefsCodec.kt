package com.safeshade.cloud

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * `EmailPreferences` on and off the wire.
 *
 * ### Why this is its own file rather than two lines in `CircleActions`
 *
 * Because the wire names and the Kotlin names differ by exactly one field -
 * `weekly_report` against `weeklyReport` - and that is the whole class of bug
 * this is written against. A mismatched key does not fail: `set_email_prefs`
 * rejects an unknown key loudly, but a *read* of a key that is not there
 * silently produces `true`, which is a switch that shows as on and a person who
 * thinks they turned the weekly report off. So the mapping lives in one place
 * with a test over the round trip.
 *
 * ### An absent key reads as true, deliberately
 *
 * `profiles.email_prefs` is a jsonb with a default rather than four columns, so
 * an older row, a partial write or a key added in a later release can all leave
 * a key missing. Reading that as `false` would withhold a fall alert from
 * somebody who never made that choice. Reading it as `true` at worst sends an
 * email somebody can then switch off.
 *
 * Anything that is not a JSON boolean is treated the same way as absent. The
 * server validates the shape on write (`set_email_prefs` refuses a non-boolean),
 * so a string here means a row that predates that function or was written by
 * something else, and guessing at the truthiness of `"no"` is exactly how a
 * preference ends up meaning its opposite.
 */
internal object EmailPrefsCodec {

    const val ALERTS = "alerts"
    const val CIRCLE = "circle"
    const val WEEKLY_REPORT = "weekly_report"
    const val ACCOUNT = "account"

    /** The four switches as `set_email_prefs` expects them. */
    fun encode(prefs: EmailPreferences): JsonObject = buildJsonObject {
        put(ALERTS, prefs.alerts)
        put(CIRCLE, prefs.circle)
        put(WEEKLY_REPORT, prefs.weeklyReport)
        put(ACCOUNT, prefs.account)
    }

    /**
     * Reads whatever the server sent back.
     *
     * Returns null for anything that is not a JSON object - a null column, a
     * `void` rpc answer, a failure that came back as a bare string. Null means
     * "not known", which `CloudState.emailPreferences` renders as dashes; it
     * does not mean all-on, and it must not be collapsed into a default here.
     */
    fun decode(element: JsonElement?): EmailPreferences? {
        val obj = element as? JsonObject ?: return null
        return EmailPreferences(
            alerts = obj.flag(ALERTS),
            circle = obj.flag(CIRCLE),
            weeklyReport = obj.flag(WEEKLY_REPORT),
            account = obj.flag(ACCOUNT)
        )
    }

    private fun JsonObject.flag(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.booleanOrNull ?: true
}
