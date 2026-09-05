package com.safeshade.platform

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Indian phone numbers, stored plainly and shown formatted.
 *
 * The stored value is always bare digits. Formatting is a *display* concern
 * applied by [IndianPhoneTransformation], which matters more here than it
 * usually would: these numbers are handed to `SmsManager` and to `tel:` intents
 * during an emergency, and a stored string carrying spaces or a country prefix
 * that the user typed inconsistently is exactly the kind of thing that works in
 * testing and fails at 3am. It also keeps the value short enough for
 * `DeviceProtocol`'s 20-character contact field.
 *
 * Formatting is deliberately not a general international implementation. This
 * product ships to India, its emergency directory is Indian, and pretending to
 * handle every dialling plan with a hand-rolled formatter would be worse than
 * being honestly narrow: a number that does not look like ten digits is left
 * alone rather than mangled.
 */
object PhoneNumbers {

    private const val LOCAL_LENGTH = 10

    /**
     * Reduces anything a user or a contact picker can produce to bare digits.
     *
     * Drops a `+91`, a bare `91` prefix, and the trunk `0` — all three are
     * common in Indian address books and all three would otherwise produce a
     * twelve- or eleven-digit "number" that no longer matches the same person
     * stored another way.
     */
    fun digitsOf(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.length > LOCAL_LENGTH && digits.startsWith("91")) digits = digits.removePrefix("91")
        if (digits.length > LOCAL_LENGTH && digits.startsWith("0")) digits = digits.trimStart('0')
        return digits.take(LOCAL_LENGTH)
    }

    /** `+91 98765 43210`. Partial input formats as far as it can. */
    fun format(raw: String): String {
        val d = digitsOf(raw)
        if (d.isEmpty()) return ""
        return buildString {
            append("+91 ")
            append(d.take(5))
            if (d.length > 5) {
                append(' ')
                append(d.drop(5))
            }
        }
    }

    /** What actually goes to `SmsManager` or a `tel:` intent. */
    fun dialable(raw: String): String {
        val d = digitsOf(raw)
        return if (d.length == LOCAL_LENGTH) "+91$d" else raw.filter { it.isDigit() || it == '+' }
    }

    fun isComplete(raw: String): Boolean = digitsOf(raw).length == LOCAL_LENGTH
}

/**
 * Renders a bare-digit field as `+91 98765 43210` while typing.
 *
 * The offset mapping is the fiddly half and the reason this is a transformation
 * rather than reformatting in `onValueChange`: the cursor has to land in the
 * right place after the prefix and each space, or editing the middle of a
 * number becomes maddening.
 */
class IndianPhoneTransformation : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text.filter { it.isDigit() }.take(10)
        val formatted = PhoneNumbers.format(digits)

        val mapping = object : OffsetMapping {
            // "+91 " is 4 characters; a space is inserted after the 5th digit.
            override fun originalToTransformed(offset: Int): Int = when {
                digits.isEmpty() -> 0
                offset <= 0 -> 4
                offset <= 5 -> 4 + offset
                else -> 5 + offset.coerceAtMost(10)
            }.coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= 4 -> 0
                offset <= 9 -> offset - 4
                else -> (offset - 5).coerceAtMost(digits.length)
            }.coerceIn(0, digits.length)
        }

        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
