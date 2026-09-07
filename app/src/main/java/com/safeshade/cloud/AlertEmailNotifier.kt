package com.safeshade.cloud

import android.util.Log
import com.safeshade.cloud.dto.CloudTables
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Asks the server to email the Circle about an alert that has just been pushed.
 *
 * ### The gap this closes
 *
 * `send-alert-email` has existed since the schema did and **nothing called it**.
 * A fall was recorded, synced, and shown on the trip log, and not one email was
 * ever sent about it. This class is the caller.
 *
 * ### Where it hangs, and why there
 *
 * On `SyncEngine.onPushed`, which runs after a batch of rows has been accepted
 * by the server. Not on the repository write, and not on the alert being
 * *created*: the function takes an alert id and reads the row under the service
 * role, so asking for an email about a row the server has never seen is a
 * guaranteed 404. The push is the moment the row exists somewhere the function
 * can read it.
 *
 * ### Only PENDING, and only alerts
 *
 * `outcome` is the whole filter. An alert that arrives already `DISMISSED` is a
 * guardian tapping "I have got this" on the phone that raised it, or a
 * historical row being backfilled to a new Circle - and neither is a reason to
 * email a family in the middle of the night. `AUTO_RESOLVED` and `CONTACTED`
 * are the same: something already happened. Only `PENDING` means nobody has
 * answered.
 *
 * The backfill is the case that makes this rule load-bearing. `enqueueAll`
 * queues every record this phone holds, including months of old alerts, and
 * every one of them would otherwise be pushed and emailed as if it had just
 * happened.
 *
 * ### The dedupe is on the server, not here
 *
 * `send-alert-email` claims `alerts.notified_at` with a conditional UPDATE
 * before it sends anything, so a second invoke of the same id sends nothing and
 * returns what the first pass achieved. That is deliberately not mirrored here
 * with a local "already asked" set: a phone that is reinstalled, or a second
 * phone in the same Circle pushing the same row, would each have their own
 * empty set, and the only place that can count the emails a guardian receives
 * is the place that sends them.
 *
 * So this is allowed to be enthusiastic. It is not allowed to be silent about
 * failure - see [notify].
 *
 * ### What it does NOT recover from
 *
 * A crash between the push succeeding and this invoke completing loses the
 * email for that alert: the row is on the server with `notified_at` still null
 * and nothing will try again. Closing that needs a server-side sweeper over
 * `alerts_circle_notified_idx`, which is why that index exists. Stated here
 * rather than left to be discovered.
 */
class AlertEmailNotifier(
    private val client: CloudClient,
    private val log: (String) -> Unit = { Log.w(TAG, it) }
) {

    /**
     * The [SyncEngine][com.safeshade.cloud.sync.SyncEngine] hook.
     *
     * Rows for any table other than `alerts` are ignored without a round trip.
     */
    suspend fun onPushed(table: String, rows: List<Pair<String, JsonObject>>) {
        if (table != CloudTables.ALERTS) return
        for ((recordId, row) in rows) {
            if (recordId.isBlank()) continue
            if (!isUnanswered(row)) continue
            notify(recordId)
        }
    }

    /**
     * True only for an alert nobody has answered yet.
     *
     * A missing `outcome` counts as unanswered. The column is nullable and the
     * app's own enum has no null member, so a row that arrives without one is a
     * row from an older writer - and the safe reading of "we do not know
     * whether anybody has seen this fall" is to tell the Circle.
     */
    private fun isUnanswered(row: JsonObject): Boolean {
        if (row["deleted_at"] is JsonPrimitive &&
            (row["deleted_at"] as JsonPrimitive).content.isNotBlank() &&
            (row["deleted_at"] as JsonPrimitive).content != "null"
        ) {
            return false
        }
        val outcome = (row["outcome"] as? JsonPrimitive)?.content?.trim()
        return outcome.isNullOrEmpty() || outcome.equals(PENDING, ignoreCase = true)
    }

    /**
     * One invoke, and its outcome in the log.
     *
     * Nothing is surfaced to the UI from here and nothing should be: this runs
     * inside a background drain that may be happening while the phone is in a
     * pocket, and the place a person finds out who was reached is the alert's
     * own `alert_deliveries` rows, which the server writes from Resend's answer.
     * A toast fired from a drain would be a claim made by the phone.
     *
     * A `Disabled` result is not logged as a failure. No cloud is configured on
     * that build, no email was ever going to be sent, and reporting it as an
     * error would be the app complaining about a feature the user never
     * switched on.
     */
    private suspend fun notify(alertId: String) {
        val body = buildJsonObject { put("alert_id", alertId) }
        when (val result = client.invoke(FUNCTION, body)) {
            is CloudResult.Ok -> log("alert email requested for " + alertId)
            is CloudResult.Failed -> log("alert email for " + alertId + ": " + result.reason)
            CloudResult.Disabled -> Unit
        }
    }

    companion object {
        private const val TAG = "SafeShadeAlertMail"

        /** Mirrors `AlertOutcome.PENDING`'s wire name in `SafetyRows.kt`. */
        internal const val PENDING = "PENDING"

        /** The edge function's slug. */
        internal const val FUNCTION = "send-alert-email"
    }
}
