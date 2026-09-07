package com.safeshade.cloud

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `EmailPreferences` on and off the wire.
 *
 * The whole reason this has a test is one field name: `weeklyReport` in Kotlin
 * and `weekly_report` in `profiles.email_prefs`. A mismatch there does not
 * fail — it reads as `true`, which draws the switch in the on position for
 * somebody who turned it off, and nothing anywhere reports it.
 */
class EmailPrefsTest {

    @Test
    fun `every switch survives the round trip`() {
        val prefs = EmailPreferences(
            alerts = true,
            circle = false,
            weeklyReport = false,
            account = true
        )
        assertEquals(prefs, EmailPrefsCodec.decode(EmailPrefsCodec.encode(prefs)))
    }

    /** The one that would be silent if it were wrong. */
    @Test
    fun `weeklyReport is snake_case on the wire`() {
        val encoded = EmailPrefsCodec.encode(EmailPreferences(weeklyReport = false))
        assertTrue("weekly_report" in encoded.keys)
        assertFalse("weeklyReport" in encoded.keys)
        assertEquals(false, (encoded["weekly_report"] as JsonPrimitive).booleanOrNull)
    }

    @Test
    fun `all four keys are always written, so a merge cannot leave a stale one`() {
        val encoded = EmailPrefsCodec.encode(EmailPreferences())
        assertEquals(setOf("alerts", "circle", "weekly_report", "account"), encoded.keys)
    }

    /**
     * A key that is not there reads as ON.
     *
     * `email_prefs` is a jsonb with a default, so an older row or a partial
     * write can leave a key missing. Reading that as `false` would withhold a
     * fall alert from somebody who never made that choice.
     */
    @Test
    fun `a missing key reads as on`() {
        val partial = buildJsonObject { put("alerts", false) }
        val decoded = EmailPrefsCodec.decode(partial)!!
        assertFalse(decoded.alerts)
        assertTrue(decoded.circle)
        assertTrue(decoded.weeklyReport)
        assertTrue(decoded.account)
    }

    /**
     * A truthy string is not a `true`.
     *
     * `set_email_prefs` refuses a non-boolean, so a string here means a row
     * written by something else. Guessing at the truthiness of `"no"` is
     * exactly how a preference comes to mean its opposite, so it is treated the
     * same as absent.
     */
    @Test
    fun `a non-boolean value is treated as absent`() {
        val odd = buildJsonObject {
            put("alerts", "no")
            put("weekly_report", 0)
        }
        val decoded = EmailPrefsCodec.decode(odd)!!
        assertTrue(decoded.alerts)
        assertTrue(decoded.weeklyReport)
    }

    /**
     * Null is "not known", and must not become a default.
     *
     * `CloudState.emailPreferences` being null is what makes the settings page
     * show dashes instead of switches. Collapsing it to all-on here would draw
     * four confident switches over a value nobody has read.
     */
    @Test
    fun `anything that is not an object decodes to null`() {
        assertNull(EmailPrefsCodec.decode(null))
        assertNull(EmailPrefsCodec.decode(JsonNull))
        assertNull(EmailPrefsCodec.decode(JsonPrimitive("all on")))
    }
}
