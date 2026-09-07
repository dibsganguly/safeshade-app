package com.safeshade.platform

import android.os.Build
import com.google.android.gms.common.ConnectionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * The two decisions in the provider layer that are worth testing: what a
 * status code means, and what this phone can honestly say about Matter.
 */
class SmartHomeProvidersTest {

    @Test
    fun `any 2xx is delivered`() {
        assertEquals(WebhookResult.Delivered(200), outcomeOf(200, "ok"))
        assertEquals(WebhookResult.Delivered(204), outcomeOf(204, ""))
        assertEquals(WebhookResult.Delivered(299), outcomeOf(299, ""))
    }

    @Test
    fun `anything else is a rejection carrying what the far end said`() {
        assertEquals(
            WebhookResult.Rejected(404, "unknown webhook id"),
            outcomeOf(404, "  unknown webhook id  ")
        )
        assertEquals(WebhookResult.Rejected(500, ""), outcomeOf(500, ""))
        // A redirect that OkHttp did not follow is not a delivery.
        assertTrue(outcomeOf(302, "") is WebhookResult.Rejected)
    }

    @Test
    fun `a long error body is cut down`() {
        val result = outcomeOf(400, "x".repeat(1000)) as WebhookResult.Rejected
        assertEquals(200, result.bodySnippet.length)
    }

    @Test
    fun `matter needs android 8 point 1 whatever play services says`() {
        val result = mapMatterAvailability(ConnectionResult.SUCCESS, Build.VERSION_CODES.O)
        assertTrue(result is MatterAvailability.Unsupported)
    }

    @Test
    fun `matter is available with play services on a new enough phone`() {
        assertEquals(
            MatterAvailability.Available,
            mapMatterAvailability(ConnectionResult.SUCCESS, Build.VERSION_CODES.TIRAMISU)
        )
    }

    @Test
    fun `each play services problem gets its own reason`() {
        val codes = listOf(
            ConnectionResult.SERVICE_MISSING,
            ConnectionResult.SERVICE_DISABLED,
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED,
            ConnectionResult.SERVICE_INVALID,
            ConnectionResult.SERVICE_UPDATING
        )
        val reasons = codes.map {
            (mapMatterAvailability(it, Build.VERSION_CODES.TIRAMISU)
                as MatterAvailability.NoGooglePlayServices).reason
        }
        assertEquals(codes.size, reasons.toSet().size)
        assertTrue(reasons.none { it.isBlank() })
    }

    @Test
    fun `network reasons are plain english and never the exception's words`() {
        val cases: List<IOException> = listOf(
            UnknownHostException("homeassistant.local"),
            SocketTimeoutException("timeout"),
            ConnectException("ECONNREFUSED"),
            SSLException("handshake"),
            IOException("something")
        )
        val reasons = cases.map { reasonForNetwork(it) }
        assertEquals(cases.size, reasons.toSet().size)
        assertTrue(reasons.none { it.contains("Exception") })
        assertTrue(reasons.none { it.contains("homeassistant.local") })
    }
}
