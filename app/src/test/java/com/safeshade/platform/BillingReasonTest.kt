package com.safeshade.platform

import com.android.billingclient.api.BillingClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [billingReason] against every `BillingResponseCode` the library defines.
 * This mapping is the actual product surface for checkout on a phone with no
 * Play Console listing yet — `ITEM_UNAVAILABLE` mapping to "This item is not
 * available" is the expected, correct result of a real purchase attempt here,
 * not a placeholder to be replaced later.
 */
class BillingReasonTest {

    @Test
    fun `maps every known response code to a non-blank plain sentence`() {
        val codes = listOf(
            BillingClient.BillingResponseCode.OK,
            BillingClient.BillingResponseCode.USER_CANCELED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
            BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
            BillingClient.BillingResponseCode.DEVELOPER_ERROR,
            BillingClient.BillingResponseCode.ERROR,
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED,
            BillingClient.BillingResponseCode.ITEM_NOT_OWNED,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
            BillingClient.BillingResponseCode.SERVICE_TIMEOUT,
            BillingClient.BillingResponseCode.NETWORK_ERROR,
        )
        for (code in codes) {
            val reason = billingReason(code, null)
            assertTrue("code $code produced a blank reason", reason.isNotBlank())
        }
    }

    @Test
    fun `item unavailable is the expected real result with no Play Console listing`() {
        assertEquals("This item is not available", billingReason(BillingClient.BillingResponseCode.ITEM_UNAVAILABLE, null))
    }

    @Test
    fun `developer error appends the debug message when present`() {
        val reason = billingReason(BillingClient.BillingResponseCode.DEVELOPER_ERROR, "invalid product id")
        assertTrue(reason.contains("invalid product id"))
    }

    @Test
    fun `unknown code falls back to a message that still names the code`() {
        val reason = billingReason(9999, null)
        assertTrue(reason.contains("9999"))
    }

    @Test
    fun `unknown code prefers the debug message when Play supplies one`() {
        val reason = billingReason(9999, "something unusual")
        assertEquals("something unusual", reason)
    }
}
