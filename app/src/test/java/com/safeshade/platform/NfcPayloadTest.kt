package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `NfcPayload.textFor` — the one decision in the NFC card write path that is
 * plain Kotlin. `NdefRecord`/`NdefMessage` are Android classes with no
 * local-JVM fake, so the round trip through them is not tested here; see
 * `platform/NfcTagWriter.kt`'s class doc.
 */
class NfcPayloadTest {

    @Test
    fun `text under the cap is returned unchanged`() {
        val summary = "Type 1 diabetic. Allergic to penicillin."
        assertEquals(summary, NfcPayload.textFor(summary))
    }

    @Test
    fun `text over the cap is truncated to it`() {
        val summary = "A".repeat(300)
        val result = NfcPayload.textFor(summary)
        assertEquals(NfcPayload.MAX_SUMMARY_LENGTH, result.length)
        assertEquals("A".repeat(NfcPayload.MAX_SUMMARY_LENGTH), result)
    }

    @Test
    fun `text exactly at the cap is unchanged`() {
        val summary = "B".repeat(NfcPayload.MAX_SUMMARY_LENGTH)
        assertEquals(summary, NfcPayload.textFor(summary))
    }

    @Test
    fun `leading and trailing whitespace is trimmed before capping`() {
        val summary = "   Penicillin allergy   "
        assertEquals("Penicillin allergy", NfcPayload.textFor(summary))
    }

    @Test
    fun `a custom max length is honoured`() {
        val summary = "0123456789"
        assertEquals("01234", NfcPayload.textFor(summary, maxLength = 5))
    }

    @Test
    fun `an empty summary stays empty`() {
        assertEquals("", NfcPayload.textFor(""))
    }

    @Test
    fun `the result never exceeds the cap regardless of input length`() {
        val summary = "x".repeat(10_000)
        assertTrue(NfcPayload.textFor(summary).length <= NfcPayload.MAX_SUMMARY_LENGTH)
    }
}
