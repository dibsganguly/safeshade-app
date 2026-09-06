package com.safeshade.cloud

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CloudClient.changes` — the fake's behaviour, which is the part a test can
 * reach.
 *
 * The real subscription needs a websocket and a project, so what is protected
 * here is the contract every caller will be written against: one table, one
 * circle, new rows only, and nothing at all when the cloud is off.
 *
 * ### Why every test launches a collector and then yields
 *
 * The fake is a `MutableSharedFlow` with no replay, deliberately: a Realtime
 * subscription does not hand you rows that arrived before you subscribed, and a
 * fake that replayed them would let a test pass while the real thing quietly
 * missed the row. So the collector has to be attached before the push, and
 * `yield()` is what gets it attached. Getting this wrong makes the test flaky
 * rather than wrong, which is worse.
 */
class CloudChangesTest {

    private fun alertRow(id: String, circleId: String?): JsonObject = buildJsonObject {
        put("id", id)
        if (circleId != null) put("circle_id", circleId)
        put("kind", "FALL")
    }

    @Test
    fun `a pushed row reaches a collector on the same table and circle`() = runTest {
        val fake = FakeCloudClient()
        val seen = mutableListOf<JsonObject>()

        val job = launch { fake.changes("alerts", "circle-1").toList(seen) }
        yield()

        fake.pushChange("alerts", alertRow("a1", "circle-1"))
        yield()
        job.cancel()

        assertEquals(1, seen.size)
        assertEquals("a1", seen[0]["id"].toString().trim('"'))
    }

    @Test
    fun `a row for another circle is not delivered`() = runTest {
        val fake = FakeCloudClient()
        val seen = mutableListOf<JsonObject>()

        val job = launch { fake.changes("alerts", "circle-1").toList(seen) }
        yield()

        fake.pushChange("alerts", alertRow("a2", "circle-2"))
        yield()
        job.cancel()

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `a row on another table is not delivered`() = runTest {
        val fake = FakeCloudClient()
        val seen = mutableListOf<JsonObject>()

        val job = launch { fake.changes("alerts", "circle-1").toList(seen) }
        yield()

        fake.pushChange("messages", alertRow("m1", "circle-1"))
        yield()
        job.cancel()

        assertTrue(seen.isEmpty())
    }

    /**
     * Two subscriptions on two tables do not cross.
     *
     * The real client gives each subscription its own channel with a unique
     * topic for exactly this reason — `Realtime.subscriptions` is keyed by topic
     * and a shared one would let the first collector to finish tear down the
     * second.
     */
    @Test
    fun `two collectors on different tables each see only their own`() = runTest {
        val fake = FakeCloudClient()
        val alerts = mutableListOf<JsonObject>()
        val messages = mutableListOf<JsonObject>()

        val a = launch { fake.changes("alerts", "circle-1").toList(alerts) }
        val m = launch { fake.changes("messages", "circle-1").toList(messages) }
        yield()

        fake.pushChange("alerts", alertRow("a1", "circle-1"))
        fake.pushChange("messages", alertRow("m1", "circle-1"))
        yield()
        a.cancel()
        m.cancel()

        assertEquals(1, alerts.size)
        assertEquals(1, messages.size)
        assertEquals("a1", alerts[0]["id"].toString().trim('"'))
        assertEquals("m1", messages[0]["id"].toString().trim('"'))
    }

    /**
     * A row with no `circle_id` reaches everyone.
     *
     * The same concession `select` makes: the real server would have rejected
     * the insert, and this fake does not pretend to enforce row-level security.
     * Written down as a test so nobody later reads it as a filtering bug.
     */
    @Test
    fun `a row with no circle_id is delivered, matching select`() = runTest {
        val fake = FakeCloudClient()
        val seen = mutableListOf<JsonObject>()

        val job = launch { fake.changes("alerts", "circle-1").toList(seen) }
        yield()

        fake.pushChange("alerts", alertRow("a3", null))
        yield()
        job.cancel()

        assertEquals(1, seen.size)
    }

    /**
     * With no project configured there is no subscription at all.
     *
     * Not an error and not a hang: an empty flow that completes at once, so a
     * screen collecting it signed out simply shows nothing. `CloudResult.Disabled`
     * has no equivalent on a Flow, and inventing one would put a "cloud is off"
     * value into the row stream for every caller to filter out.
     */
    @Test
    fun `a disabled fake emits nothing and completes`() = runTest {
        val fake = FakeCloudClient(disabled = true)

        val seen = fake.changes("alerts", "circle-1").toList()

        assertTrue(seen.isEmpty())
    }
}
