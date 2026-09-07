package com.safeshade.platform

import com.safeshade.data.SmartHomeProviders
import com.safeshade.data.SmartHomeTriggers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * The exact bytes a house receives, and the exact bytes the signature covers.
 * Both are a contract with software this app does not own.
 */
class WebhookSignerTest {

    private val event = SmartHomeEvent(
        trigger = SmartHomeTriggers.FALL,
        wearerName = "Asha",
        at = 1_700_000_000_000L,
        lat = 12.9716,
        lon = 77.5946,
        detail = "Fall detected"
    )

    @Test
    fun `the body carries the documented keys`() {
        assertEquals(
            """{"event":"fall","wearer":"Asha","at":1700000000000,""" +
                """"lat":12.9716,"lon":77.5946,"detail":"Fall detected"}""",
            WebhookSigner.bodyFor(event)
        )
    }

    @Test
    fun `a missing position is JSON null and never zero`() {
        val body = WebhookSigner.bodyFor(event.copy(lat = null, lon = null, detail = null))
        assertEquals(
            """{"event":"fall","wearer":"Asha","at":1700000000000,""" +
                """"lat":null,"lon":null,"detail":null}""",
            body
        )
        assertTrue("a null position was rendered as a coordinate", !body.contains("0.0"))
    }

    @Test
    fun `quotes backslashes and control characters are escaped`() {
        val body = WebhookSigner.bodyFor(
            event.copy(wearerName = "O\"Brien\\", detail = "line\none\ttab\u0001")
        )
        assertEquals(
            "{\"event\":\"fall\",\"wearer\":\"O\\\"Brien\\\\\",\"at\":1700000000000," +
                "\"lat\":12.9716,\"lon\":77.5946,\"detail\":\"line\\none\\ttab\\u0001\"}",
            body
        )
    }

    @Test
    fun `IFTTT gets its own three ingredients`() {
        assertEquals(
            """{"value1":"Asha","value2":"fall","value3":"Fall detected"}""",
            WebhookSigner.bodyFor(event, SmartHomeProviders.IFTTT)
        )
    }

    @Test
    fun `IFTTT falls back to a maps link when there is no detail`() {
        assertEquals(
            """{"value1":"Asha","value2":"fall",""" +
                """"value3":"https://maps.google.com/?q=12.9716,77.5946"}""",
            WebhookSigner.bodyFor(event.copy(detail = null), SmartHomeProviders.IFTTT)
        )
    }

    @Test
    fun `IFTTT third ingredient is empty rather than absent when nothing is known`() {
        assertEquals(
            """{"value1":"Asha","value2":"fall","value3":""}""",
            WebhookSigner.bodyFor(
                event.copy(detail = null, lat = null, lon = null),
                SmartHomeProviders.IFTTT
            )
        )
    }

    @Test
    fun `home assistant gets the same body as a plain webhook`() {
        assertEquals(
            WebhookSigner.bodyFor(event, SmartHomeProviders.WEBHOOK),
            WebhookSigner.bodyFor(event, SmartHomeProviders.HOME_ASSISTANT)
        )
    }

    @Test
    fun `the signature is lowercase hex HMAC-SHA256 over timestamp dot body`() {
        val body = """{"a":1}"""
        val signed = WebhookSigner.sign("key", 1_700_000_000_000L, body)

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec("key".toByteArray(), "HmacSHA256"))
        val expected = mac.doFinal("1700000000000.$body".toByteArray())
            .joinToString("") { "%02x".format(it) }

        assertEquals(expected, signed)
        assertEquals(64, signed.length)
        assertEquals(signed.lowercase(), signed)
    }

    @Test
    fun `the signature changes when the timestamp does, so a call cannot be replayed`() {
        val body = WebhookSigner.bodyFor(event)
        val a = WebhookSigner.sign("key", 1L, body)
        val b = WebhookSigner.sign("key", 2L, body)
        assertTrue(a != b)
    }
}
