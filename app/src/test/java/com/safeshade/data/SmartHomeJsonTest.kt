package com.safeshade.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The codec's job is to survive a blob it did not write: a corrupt one, an
 * older one, and one carrying a trigger or provider the server would reject.
 */
class SmartHomeJsonTest {

    private val hook = SmartHomeHook(
        id = "h1",
        name = "Hall light",
        trigger = SmartHomeTriggers.FALL,
        provider = SmartHomeProviders.HOME_ASSISTANT,
        endpointUrl = "https://example.invalid/api/webhook/abc",
        secret = "s3cret",
        enabled = true,
        lastFiredAt = 1_700_000_000_000L,
        lastError = null,
        lastStatusCode = 200
    )

    @Test
    fun `hooks round trip`() {
        val decoded = SmartHomeJson.decodeHooks(SmartHomeJson.encodeHooks(listOf(hook)))
        assertEquals(listOf(hook), decoded)
    }

    @Test
    fun `corrupt blob decodes to empty rather than throwing`() {
        assertEquals(emptyList<SmartHomeHook>(), SmartHomeJson.decodeHooks("{not json"))
        assertEquals(emptyList<SmartHomeFiring>(), SmartHomeJson.decodeFirings("{not json"))
        assertEquals(emptyList<SmartHomeHook>(), SmartHomeJson.decodeHooks(null))
    }

    @Test
    fun `a hook naming a trigger the server would reject is dropped`() {
        val json = """[{"id":"h1","name":"n","trigger":"doorbell","provider":"webhook",
            |"endpointUrl":"https://x.invalid"}]""".trimMargin()
        assertEquals(emptyList<SmartHomeHook>(), SmartHomeJson.decodeHooks(json))
    }

    @Test
    fun `a hook naming a provider the server would reject is dropped`() {
        val json = """[{"id":"h1","name":"n","trigger":"fall","provider":"smartthings",
            |"endpointUrl":"https://x.invalid"}]""".trimMargin()
        assertEquals(emptyList<SmartHomeHook>(), SmartHomeJson.decodeHooks(json))
    }

    @Test
    fun `a hook with no endpoint is dropped rather than repaired`() {
        val json = """[{"id":"h1","name":"n","trigger":"fall","provider":"webhook"}]"""
        assertEquals(emptyList<SmartHomeHook>(), SmartHomeJson.decodeHooks(json))
    }

    @Test
    fun `a row from an older build missing the newer fields still decodes`() {
        val json = """[{"id":"h1","name":"n","trigger":"sos","provider":"ifttt",
            |"endpointUrl":"https://x.invalid"}]""".trimMargin()
        val decoded = SmartHomeJson.decodeHooks(json)
        assertEquals(1, decoded.size)
        assertTrue(decoded[0].enabled)
        assertNull(decoded[0].lastFiredAt)
        assertNull(decoded[0].lastStatusCode)
        assertNull(decoded[0].secret)
    }

    @Test
    fun `firings are capped on both encode and decode`() {
        val many = (1..80).map { SmartHomeFiring("h1", it.toLong(), SmartHomeTriggers.FALL) }
        val decoded = SmartHomeJson.decodeFirings(SmartHomeJson.encodeFirings(many))
        assertEquals(MAX_SMART_HOME_FIRINGS, decoded.size)
        // The newest survive, which is the half anyone reads.
        assertEquals(80L, decoded.last().at)
    }

    @Test
    fun `toString redacts the secret and the endpoint`() {
        val text = hook.toString()
        assertTrue(text.contains("<redacted>"))
        assertTrue("the secret leaked", !text.contains("s3cret"))
        assertTrue("the endpoint leaked", !text.contains("example.invalid"))
    }

    @Test
    fun `the constant sets match the SQL check constraints`() {
        assertEquals(
            listOf("fall", "sos", "zone_exit", "zone_enter", "low_battery", "check_in_missed"),
            SmartHomeTriggers.ALL
        )
        assertEquals(
            listOf("webhook", "home_assistant", "ifttt", "matter"),
            SmartHomeProviders.ALL
        )
    }
}
