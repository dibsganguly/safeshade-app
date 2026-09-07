package com.safeshade.cloud

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `weekly-report`'s response, as the page reads it.
 *
 * Four server statuses collapse into three lists, and which one each lands in
 * is the whole point: a person who switched the report off did not fail to
 * receive it, and an address whose outcome is genuinely unknown did not succeed.
 */
class WeeklyReportSendTest {

    private fun body(vararg rows: Triple<String, String, String?>) = buildJsonObject {
        putJsonArray("deliveries") {
            for ((email, status, error) in rows) {
                add(buildJsonObject {
                    put("email", email)
                    put("status", status)
                    if (error != null) put("error", error)
                })
            }
        }
    }

    @Test
    fun `sent, skipped and failed land in three different lists`() {
        val send = weeklyReportSend(
            body(
                Triple("a@example.com", "sent", null),
                Triple("b@example.com", "skipped", null),
                Triple("c@example.com", "failed", "You can only send testing emails to your own address.")
            )
        )
        assertEquals(listOf("a@example.com"), send.sent)
        assertEquals(listOf("b@example.com"), send.skipped)
        assertEquals(
            mapOf("c@example.com" to "You can only send testing emails to your own address."),
            send.failed
        )
    }

    /**
     * Resend's own words survive.
     *
     * With no sending domain, every address except the account owner's comes
     * back with the provider's explanation, and that sentence is far more
     * useful on a screen than "could not send".
     */
    @Test
    fun `a failure carries the provider's message verbatim`() {
        val send = weeklyReportSend(
            body(Triple("x@example.com", "failed", "Domain is not verified."))
        )
        assertEquals("Domain is not verified.", send.failed["x@example.com"])
    }

    /**
     * `unknown` is a failure on the page, and says so.
     *
     * The request left the function and nothing came back. A screen has two
     * columns, and the honest place for "we do not know" is beside the failures
     * rather than beside the successes.
     */
    @Test
    fun `an unknown outcome is not counted as sent`() {
        val send = weeklyReportSend(body(Triple("y@example.com", "unknown", null)))
        assertTrue(send.sent.isEmpty())
        assertTrue(send.failed.containsKey("y@example.com"))
        assertTrue(send.failed["y@example.com"]!!.contains("did not get an answer"))
    }

    /** A circle where nobody has an address is three empty lists, not a crash. */
    @Test
    fun `an empty deliveries array is three empty lists`() {
        val send = weeklyReportSend(buildJsonObject { putJsonArray("deliveries") { } })
        assertEquals(WeeklyReportSend(), send)
    }

    @Test
    fun `a response with no deliveries key does not throw`() {
        val send = weeklyReportSend(buildJsonObject { put("circle_id", "c1") })
        assertEquals(WeeklyReportSend(), send)
    }

    /** A row with no address cannot be shown against anybody, so it is dropped. */
    @Test
    fun `a row with no email is ignored`() {
        val send = weeklyReportSend(buildJsonObject {
            putJsonArray("deliveries") {
                add(buildJsonObject { put("status", "sent") })
            }
        })
        assertEquals(WeeklyReportSend(), send)
    }
}
