package com.safeshade

import androidx.compose.ui.text.AnnotatedString
import com.safeshade.platform.IndianPhoneTransformation
import com.safeshade.platform.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The number a user reads must be the number the phone dials.
 *
 * These are not abstract formatting tests. `dialable` feeds `SmsManager` and
 * `tel:` intents during an emergency, and `IndianPhoneTransformation` is what
 * the user proof-reads the number against. If the two disagree, the app shows a
 * correct number and quietly texts a different one — which is how a real stored
 * contact on the test device behaved.
 */
class PhoneNumbersTest {

    private fun shown(raw: String): String =
        IndianPhoneTransformation().filter(AnnotatedString(raw)).text.text

    @Test
    fun `a plain ten-digit number formats and dials consistently`() {
        assertEquals("+91 98300 11223", PhoneNumbers.format("9830011223"))
        assertEquals("+919830011223", PhoneNumbers.dialable("9830011223"))
        assertEquals("+91 98300 11223", shown("9830011223"))
    }

    @Test
    fun `a number typed with its country code is not counted twice`() {
        // The field accepts "+91 98300 11223" verbatim today, so this is the
        // shape a real contact actually has in storage.
        assertEquals("9830011223", PhoneNumbers.digitsOf("+91 98300 11223"))
        assertEquals("+919830011223", PhoneNumbers.dialable("+91 98300 11223"))
        assertEquals("+91 98300 11223", shown("+91 98300 11223"))
    }

    @Test
    fun `an over-long number is never silently truncated into a valid one`() {
        // The device's stored contact: eleven digits after the country code.
        // Truncation would turn this into +918917366006 — a different number
        // that looks entirely valid, which is the worst possible failure.
        val stored = "+91 891736 60065"
        assertEquals(false, PhoneNumbers.isComplete(stored))
        assertEquals("+9189173660065", PhoneNumbers.dialable(stored))
    }

    @Test
    fun `what is shown always carries the digits that will be dialled`() {
        for (raw in listOf("9830011223", "+91 98300 11223", "09830011223", "+91 891736 60065")) {
            val shownDigits = shown(raw).filter { it.isDigit() }.removePrefix("91")
            val dialledDigits = PhoneNumbers.dialable(raw).filter { it.isDigit() }.removePrefix("91")
            assertEquals("mismatch for '$raw'", dialledDigits, shownDigits)
        }
    }
}
