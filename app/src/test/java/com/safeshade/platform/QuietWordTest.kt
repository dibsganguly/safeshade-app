package com.safeshade.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietWordTest {

    @Test
    fun `matches a whole word case- and punctuation-insensitively`() {
        val matcher = QuietWordMatcher("Pineapple")
        assertTrue(matcher.matches("I left the pineapple on the counter"))
        assertTrue(matcher.matches("PINEAPPLE!"))
        assertTrue(matcher.matches("is the pineapple, still there?"))
    }

    @Test
    fun `does not match a partial word`() {
        val matcher = QuietWordMatcher("pine")
        assertFalse(matcher.matches("I love pineapple"))
    }

    @Test
    fun `matches a multi-word phrase as a unit`() {
        val matcher = QuietWordMatcher("purple elephant")
        assertTrue(matcher.matches("there is a purple elephant in the room"))
        assertFalse(matcher.matches("the elephant is purple today"))
    }

    @Test
    fun `whitespace and punctuation differences do not prevent a match`() {
        val matcher = QuietWordMatcher("  Purple   Elephant  ")
        assertTrue(matcher.matches("purple, elephant!"))
    }

    @Test
    fun `no match when the word is absent`() {
        val matcher = QuietWordMatcher("pineapple")
        assertFalse(matcher.matches("just a normal message"))
    }

    @Test
    fun `empty quiet word never matches`() {
        val matcher = QuietWordMatcher("   ")
        assertFalse(matcher.matches("anything at all"))
    }

    @Test
    fun `redact replaces the quiet word with bullets and leaves the rest`() {
        val matcher = QuietWordMatcher("pineapple")
        assertEquals("I left the ••• on the counter", matcher.redact("I left the pineapple on the counter"))
    }

    @Test
    fun `redact is case-insensitive and replaces every occurrence`() {
        val matcher = QuietWordMatcher("pineapple")
        assertEquals("••• and ••• again", matcher.redact("Pineapple and PINEAPPLE again"))
    }

    @Test
    fun `redact leaves text untouched when the word never appears`() {
        val matcher = QuietWordMatcher("pineapple")
        assertEquals("nothing to see here", matcher.redact("nothing to see here"))
    }

    @Test
    fun `validate rejects words shorter than three letters`() {
        assertEquals(QuietWordValidation.TooShort, QuietWord.validate("hi"))
        assertEquals(QuietWordValidation.TooShort, QuietWord.validate("a"))
        assertEquals(QuietWordValidation.TooShort, QuietWord.validate(""))
    }

    @Test
    fun `validate rejects the common-word list`() {
        // "ok", "hi" and "no" are also too short (under three letters), so
        // TooShort wins for those; only the length-3-and-up common words
        // reach the TooCommon check.
        for (word in listOf("help", "Yes", "Mum", "dad", "please", "STOP")) {
            assertEquals("expected '$word' to be too common", QuietWordValidation.TooCommon, QuietWord.validate(word))
        }
    }

    @Test
    fun `short common words are rejected as too short before too common`() {
        for (word in listOf("ok", "hi", "no")) {
            assertEquals("expected '$word' to be too short", QuietWordValidation.TooShort, QuietWord.validate(word))
        }
    }

    @Test
    fun `validate rejects digits-only input`() {
        assertEquals(QuietWordValidation.ContainsDigitsOnly, QuietWord.validate("12345"))
        assertEquals(QuietWordValidation.ContainsDigitsOnly, QuietWord.validate("007 42"))
    }

    @Test
    fun `validate accepts an ordinary quiet word`() {
        assertEquals(QuietWordValidation.Ok, QuietWord.validate("pineapple"))
        assertEquals(QuietWordValidation.Ok, QuietWord.validate("purple elephant"))
    }
}
