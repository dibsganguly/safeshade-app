package com.safeshade.cloud

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `send-invite`'s answer, read the way the app reads it.
 *
 * The one thing these prove is the thing the function's own header insists on:
 * the invitation and the email are separate facts. A refused delivery is a
 * successful invitation with a failed email, and it must arrive at the screen
 * carrying the provider's own words - with no sending domain configured, that
 * is the normal case for every address except the Resend account owner's.
 */
class InviteResponseTest {

    private fun delivery(raw: String): JsonObject =
        Json.parseToJsonElement(raw) as JsonObject

    @Test
    fun `a sent email leaves the invitation pending, not accepted`() {
        val status = deliveryStatus(delivery("""{"status":"sent","error":null}"""))
        assertEquals(InviteStatus.Pending, status)
    }

    @Test
    fun `a failed delivery carries the provider's own message verbatim`() {
        val body = delivery(
            """{"invite_id":"abc","expires_at":"2026-09-21T00:00:00Z",""" +
                """"delivery":{"status":"failed",""" +
                """"error":"You can only send testing emails to your own email address"}}"""
        )
        val status = deliveryStatus(body["delivery"] as JsonObject)

        assertTrue(status is InviteStatus.Failed)
        assertEquals(
            "You can only send testing emails to your own email address",
            (status as InviteStatus.Failed).reason
        )
    }

    @Test
    fun `a failure with no message still says something a person can read`() {
        val status = deliveryStatus(delivery("""{"status":"failed","error":null}"""))
        assertTrue(status is InviteStatus.Failed)
        assertTrue((status as InviteStatus.Failed).reason.isNotBlank())
    }

    @Test
    fun `unknown is a failure and never reads as on its way`() {
        // "Resend accepted the request and the response could not be read."
        // Calling that Pending would be a claim nobody is in a position to make.
        val status = deliveryStatus(delivery("""{"status":"unknown","error":null}"""))
        assertTrue(status is InviteStatus.Failed)
    }

    @Test
    fun `no delivery block at all is pending`() {
        assertEquals(InviteStatus.Pending, deliveryStatus(null))
    }

    @Test
    fun `roles and tiers survive the round trip through the wire spelling`() {
        assertEquals(CircleRole.OWNER, CircleRole.fromWire("owner"))
        assertEquals(CircleRole.VIEWER, CircleRole.fromWire("VIEWER"))
        // An unknown role is a guardian, not a crash and not an owner.
        assertEquals(CircleRole.GUARDIAN, CircleRole.fromWire("admin"))
        assertEquals(CloudTier.PLUS, CloudTier.fromWire("plus"))
        assertEquals(CloudTier.FREE, CloudTier.fromWire(null))
    }

    @Test
    fun `the developer override is what the gates read`() {
        val state = CloudState(tier = CloudTier.FREE, devTierOverride = CloudTier.PRO)
        assertEquals(CloudTier.PRO, state.effectiveTier)
        assertEquals(CloudTier.FREE, state.tier)
        assertEquals(CloudTier.FREE, state.copy(devTierOverride = null).effectiveTier)
    }
}
