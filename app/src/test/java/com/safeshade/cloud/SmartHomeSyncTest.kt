package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.SmartHomeHookRow
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.MergeRules
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.data.SmartHomeHook
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Smart-home hooks across the Circle.
 *
 * A hook is a promise that the hall light comes on when someone falls, and the
 * two ways it breaks in sync are opposite. Push the row without its endpoint
 * and the second guardian's phone shows a hook that looks configured and fires
 * nothing. Pull the row over the top of a local edit and the user's change is
 * silently undone by the copy the server happened to be holding.
 */
class SmartHomeSyncTest {

    private val circle = "11111111-1111-4111-8111-111111111111"
    private val hookId = "66666666-6666-4666-8666-666666666666"

    private val hook = SmartHomeHook(
        id = hookId,
        name = "Hall light",
        trigger = "fall",
        provider = "home_assistant",
        endpointUrl = "http://192.168.1.20:8123/api/webhook/abc",
        secret = "shh",
        enabled = true,
        lastFiredAt = 1_700_000_000_000L,
        lastError = null,
        lastStatusCode = 200
    )

    private fun snapshot(vararg hooks: SmartHomeHook) = SyncSnapshot(
        circleId = circle,
        userId = "22222222-2222-4222-8222-222222222222",
        smartHomeHooks = hooks.toList()
    )

    private fun resolve(s: SyncSnapshot, id: String = hookId, op: OutboxOp = OutboxOp.UPSERT) =
        PayloadResolver.resolve(CloudTables.SMART_HOME_HOOKS, id, op, s)

    private fun JsonObject.str(key: String): String? =
        this[key]?.takeIf { it != JsonNull }?.jsonPrimitive?.content

    // ============================================
    // PUSH
    // ============================================

    /**
     * The endpoint and the secret both travel. `SmartHomeStore` treats the URL
     * as a credential and redacts it from `toString()`, and that is right for a
     * log - but the row is RLS-protected and readable only by the circle that
     * wrote it, and a hook that reached the second phone without them would
     * look configured and do nothing at all.
     */
    @Test
    fun `a hook carries everything the other phone needs to fire it`() {
        val row = resolve(snapshot(hook))
        assertNotNull(row)
        requireNotNull(row)

        assertEquals(circle, row.str("circle_id"))
        assertEquals(CloudIds.cloudId(hookId, circle), row.str("id"))
        assertEquals("Hall light", row.str("name"))
        assertEquals("fall", row.str("trigger"))
        assertEquals("home_assistant", row.str("provider"))
        assertEquals("http://192.168.1.20:8123/api/webhook/abc", row.str("endpoint_url"))
        assertEquals("shh", row.str("secret"))
        assertEquals("true", row.str("enabled"))
        assertEquals("2023-11-14T22:13:20Z", row.str("last_fired_at"))
    }

    @Test
    fun `a hook this phone no longer holds resolves to nothing`() {
        assertNull(resolve(snapshot()))
    }

    @Test
    fun `removing a hook produces a tombstone rather than a skip`() {
        val row = resolve(snapshot(), op = OutboxOp.DELETE)
        assertNotNull(row)
        assertEquals(CloudIds.cloudId(hookId, circle), requireNotNull(row).str("id"))
    }

    @Test
    fun `every hook row, live or tombstone, carries the same keys`() {
        val live = requireNotNull(resolve(snapshot(hook)))
        val tombstone = requireNotNull(resolve(snapshot(), op = OutboxOp.DELETE))
        assertEquals(live.keys, tombstone.keys)
        assertTrue("created_at" !in live.keys)
        assertTrue("updated_at" !in live.keys)
    }

    /**
     * The backfill marker is `userId:circleId`, so accepting an invite runs it
     * again - and every hook already sent to the first circle has left the
     * outbox. Without this the hook would never exist in the new circle.
     */
    @Test
    fun `a hook is queued by the backfill, so it survives joining a new circle`() {
        val plan = com.safeshade.cloud.repo.BackfillPlan.of(snapshot(hook))
        assertTrue(CloudTables.SMART_HOME_HOOKS to hookId in plan)
    }

    // ============================================
    // PULL
    // ============================================

    private fun row(
        id: String = CloudIds.cloudId(hookId, circle),
        endpointUrl: String? = "https://example.test/hook",
        deletedAt: String? = null
    ) = SmartHomeHookRow(
        id = id,
        circleId = circle,
        name = "Porch light",
        trigger = "sos",
        provider = "webhook",
        endpointUrl = endpointUrl,
        secret = "other",
        enabled = false,
        lastFiredAt = "2026-09-06T09:00:00Z",
        lastError = "The address did not answer.",
        deletedAt = deletedAt
    )

    @Test
    fun `a hook set up on another phone arrives whole`() {
        val merged = MergeRules.smartHomeHooks(emptyList(), listOf(row()), circle)
        val incoming = merged.single()

        assertEquals("Porch light", incoming.name)
        assertEquals("sos", incoming.trigger)
        assertEquals("https://example.test/hook", incoming.endpointUrl)
        assertEquals("other", incoming.secret)
        assertEquals(false, incoming.enabled)
        assertEquals("The address did not answer.", incoming.lastError)
    }

    /**
     * A local edit that has not been pushed is by definition newer than
     * anything the server could be holding. Without this the user's change is
     * undone by their own sync.
     */
    @Test
    fun `a queued local edit wins over an arriving row`() {
        val merged = MergeRules.smartHomeHooks(
            listOf(hook),
            listOf(row()),
            circle,
            pending = setOf(hookId)
        )
        assertEquals("Hall light", merged.single().name)
        assertEquals(hook.endpointUrl, merged.single().endpointUrl)
    }

    /**
     * `lastStatusCode` has no column, so an arriving row would blank it. It is
     * this phone's note about its own last attempt, and a row that flickered
     * between "403" and nothing depending on sync timing tells a user debugging
     * a silent hook precisely nothing.
     */
    @Test
    fun `the local status code survives a pull that cannot carry it`() {
        val merged = MergeRules.smartHomeHooks(listOf(hook), listOf(row()), circle)
        assertEquals(200, merged.single().lastStatusCode)
        // And the row it replaced kept its local id, not the server's uuid.
        assertEquals(hookId, merged.single().id)
    }

    @Test
    fun `a hook with no endpoint is not added, because it could never fire`() {
        val merged = MergeRules.smartHomeHooks(emptyList(), listOf(row(endpointUrl = null)), circle)
        assertTrue(merged.isEmpty())
    }

    @Test
    fun `a tombstone removes the hook`() {
        val merged = MergeRules.smartHomeHooks(
            listOf(hook),
            listOf(row(deletedAt = "2026-09-07T10:00:00Z")),
            circle
        )
        assertTrue(merged.isEmpty())
    }
}
