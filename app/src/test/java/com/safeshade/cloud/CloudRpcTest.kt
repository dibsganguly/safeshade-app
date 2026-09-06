package com.safeshade.cloud

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `CloudClient.rpc` — the pure mapping, and the fake's half of the contract.
 *
 * The real `postgrest.rpc` call cannot be exercised on a JVM, so what is worth
 * protecting is everything around it: the response body's shape, and the fake
 * behaving the way the real one will when Phase 2 wires `accept_invite` and
 * `heatmap_in` to screens.
 */
class CloudRpcTest {

    // ---------------------------------------------------------------
    // parseRpcData
    // ---------------------------------------------------------------

    /**
     * The bug this test exists for.
     *
     * A Postgres function declared `returns void` answers with a zero-length
     * body. `Json.parseToJsonElement("")` throws, so without the blank check
     * every successful call to such a function would be reported as a failure —
     * and, because a parse error looks transient, retried by the outbox until
     * something gave up.
     */
    @Test
    fun `an empty body is a null result, not a failure`() {
        assertEquals(JsonNull, parseRpcData(""))
        assertEquals(JsonNull, parseRpcData("   \n "))
    }

    @Test
    fun `an explicit JSON null is also null`() {
        assertEquals(JsonNull, parseRpcData("null"))
    }

    /** `accept_invite` returns a bare uuid, not an object. */
    @Test
    fun `a bare scalar survives`() {
        val parsed = parseRpcData("\"3f1b2c4d-0000-0000-0000-000000000000\"")
        assertEquals("3f1b2c4d-0000-0000-0000-000000000000", parsed.jsonPrimitive.content)
    }

    /** `heatmap_in` returns an array of cells. */
    @Test
    fun `an array survives as an array`() {
        val parsed = parseRpcData("""[{"lat":12.9,"lon":77.6,"n":7}]""")
        assertTrue(parsed is JsonArray)
        assertEquals(1, (parsed as JsonArray).size)
    }

    @Test
    fun `an object survives as an object`() {
        val parsed = parseRpcData("""{"joined":true}""")
        assertTrue(parsed is JsonObject)
    }

    /**
     * A present-but-unparseable body is a fault, not a quiet null.
     *
     * PostgREST does not produce one; if it ever did, mapping it to "no result"
     * would hide a broken contract behind a value that reads as ordinary.
     */
    @Test
    fun `a non-JSON body throws so the caller reports a failure`() {
        var threw = false
        try {
            parseRpcData("<html>502 Bad Gateway</html>")
        } catch (t: Throwable) {
            threw = true
        }
        assertTrue("a non-JSON body must not be swallowed", threw)
    }

    // ---------------------------------------------------------------
    // FakeCloudClient.rpc
    // ---------------------------------------------------------------

    @Test
    fun `the fake records the call and its arguments`() = runTest {
        val fake = FakeCloudClient()
        val args = buildJsonObject { put("p_token", "abc") }

        fake.rpc("accept_invite", args)

        assertEquals(1, fake.rpcCalls.size)
        assertEquals("accept_invite", fake.rpcCalls[0].first)
        assertEquals(args, fake.rpcCalls[0].second)
    }

    @Test
    fun `an unstubbed function answers null, the way a void function does`() = runTest {
        val fake = FakeCloudClient()
        val result = fake.rpc("accept_invite", JsonObject(emptyMap()))
        assertEquals(JsonNull, (result as CloudResult.Ok).value)
    }

    @Test
    fun `a stubbed result comes back`() = runTest {
        val fake = FakeCloudClient()
        fake.rpcResults["accept_invite"] = JsonPrimitive("a-circle-id")

        val result = fake.rpc("accept_invite", JsonObject(emptyMap()))

        assertEquals("a-circle-id", (result as CloudResult.Ok).value.jsonPrimitive.content)
    }

    /** The Phase 1 rule: no method escapes [FakeCloudClient.guarded]. */
    @Test
    fun `a disabled fake reports Disabled and records nothing`() = runTest {
        val fake = FakeCloudClient(disabled = true)

        val result = fake.rpc("accept_invite", JsonObject(emptyMap()))

        assertEquals(CloudResult.Disabled, result)
        assertTrue(fake.rpcCalls.isEmpty())
    }

    @Test
    fun `failNext turns the next rpc into a retryable failure and is consumed`() = runTest {
        val fake = FakeCloudClient()
        fake.failNext = "The network dropped."

        val failed = fake.rpc("accept_invite", JsonObject(emptyMap()))
        val second = fake.rpc("accept_invite", JsonObject(emptyMap()))

        assertEquals("The network dropped.", (failed as CloudResult.Failed).reason)
        assertTrue(failed.retryable)
        assertTrue(second is CloudResult.Ok)
        assertNull(fake.failNext)
    }
}
