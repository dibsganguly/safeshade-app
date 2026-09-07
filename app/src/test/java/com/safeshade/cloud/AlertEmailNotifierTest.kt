package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The alert email's trigger.
 *
 * `send-alert-email` shipped with the first migration and nothing ever called
 * it: a fall was recorded, synced, drawn on the trip log, and no guardian was
 * emailed. These tests are about the two ways the fix could be worse than the
 * gap — never firing, and firing for rows that are not news.
 */
class AlertEmailNotifierTest {

    private fun alert(
        id: String = "a1",
        outcome: String? = "PENDING",
        deletedAt: String? = null
    ): Pair<String, JsonObject> = id to buildJsonObject {
        put("id", id)
        put("circle_id", "c1")
        if (outcome != null) put("outcome", outcome)
        if (deletedAt != null) put("deleted_at", deletedAt)
    }

    private fun notifier(client: FakeCloudClient) = AlertEmailNotifier(client) { }

    @Test
    fun `an unanswered alert asks the server to email the circle`() = runTest {
        val client = FakeCloudClient()
        notifier(client).onPushed(CloudTables.ALERTS, listOf(alert(id = "fall-1")))

        assertEquals(1, client.invocations.size)
        val (function, body) = client.invocations.single()
        assertEquals(AlertEmailNotifier.FUNCTION, function)
        assertEquals("fall-1", body["alert_id"]!!.jsonPrimitive.content)
    }

    /**
     * The body carries the alert id and nothing else.
     *
     * That is the function's security model, not a detail: if the caller could
     * name recipients it would be an open relay wearing SafeShade's branding.
     */
    @Test
    fun `the request names the alert and nothing else`() = runTest {
        val client = FakeCloudClient()
        notifier(client).onPushed(CloudTables.ALERTS, listOf(alert()))
        assertEquals(setOf("alert_id"), client.invocations.single().second.keys)
    }

    /**
     * An alert that arrives already dealt with is not news.
     *
     * This is the rule that makes the backfill survivable: `enqueueAll` pushes
     * every alert this phone has ever held, and without the filter a new sign-in
     * would email the family about months of falls as if they had just happened.
     */
    @Test
    fun `an alert somebody has already answered is not emailed`() = runTest {
        val client = FakeCloudClient()
        val n = notifier(client)
        n.onPushed(CloudTables.ALERTS, listOf(alert(id = "d", outcome = "DISMISSED")))
        n.onPushed(CloudTables.ALERTS, listOf(alert(id = "c", outcome = "CONTACTED")))
        n.onPushed(CloudTables.ALERTS, listOf(alert(id = "r", outcome = "AUTO_RESOLVED")))
        assertTrue(client.invocations.isEmpty())
    }

    /** A tombstone is a deletion travelling, not an alert being raised. */
    @Test
    fun `a soft-deleted alert is not emailed`() = runTest {
        val client = FakeCloudClient()
        notifier(client).onPushed(
            CloudTables.ALERTS,
            listOf(alert(id = "gone", deletedAt = "2026-09-01T00:00:00Z"))
        )
        assertTrue(client.invocations.isEmpty())
    }

    /**
     * A row with no outcome at all is emailed.
     *
     * The column is nullable and the app's enum has no null member, so a row
     * without one came from an older writer. "We do not know whether anybody
     * has seen this fall" resolves towards telling the Circle.
     */
    @Test
    fun `an alert with no outcome is treated as unanswered`() = runTest {
        val client = FakeCloudClient()
        notifier(client).onPushed(CloudTables.ALERTS, listOf(alert(outcome = null)))
        assertEquals(1, client.invocations.size)
    }

    @Test
    fun `no other table triggers an email`() = runTest {
        val client = FakeCloudClient()
        val n = notifier(client)
        n.onPushed(CloudTables.MESSAGES, listOf(alert()))
        n.onPushed(CloudTables.ZONE_EVENTS, listOf(alert()))
        n.onPushed(CloudTables.PROFILES, listOf(alert()))
        assertTrue(client.invocations.isEmpty())
    }

    /** A batch of three pushed alerts is three requests, not one. */
    @Test
    fun `every unanswered alert in a batch is asked for separately`() = runTest {
        val client = FakeCloudClient()
        notifier(client).onPushed(
            CloudTables.ALERTS,
            listOf(alert(id = "a"), alert(id = "b", outcome = "DISMISSED"), alert(id = "c"))
        )
        assertEquals(
            listOf("a", "c"),
            client.invocations.map { it.second["alert_id"]!!.jsonPrimitive.content }
        )
    }

    /**
     * On a build with no project this does nothing and says nothing.
     *
     * No cloud is configured, no email was ever going to be sent, and drawing a
     * failure at somebody who never switched cloud sync on would be the app
     * complaining about a feature it does not have.
     */
    @Test
    fun `a build with no cloud is silent`() = runTest {
        val client = FakeCloudClient(disabled = true)
        notifier(client).onPushed(CloudTables.ALERTS, listOf(alert()))
        assertTrue(client.invocations.isEmpty())
    }
}
