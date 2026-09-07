package com.safeshade.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietWordGuardTest {

    @Test
    fun `a blank word never raises`() {
        assertFalse(QuietWordGuard.shouldRaise("", "pineapple is on the counter", null, 1_000L))
        assertFalse(QuietWordGuard.shouldRaise("   ", "pineapple", null, 1_000L))
    }

    @Test
    fun `a first match with no prior raise raises`() {
        assertTrue(QuietWordGuard.shouldRaise("pineapple", "is the pineapple still there", null, 1_000L))
    }

    @Test
    fun `text without the word does not raise`() {
        assertFalse(QuietWordGuard.shouldRaise("pineapple", "everything is fine", null, 1_000L))
    }

    @Test
    fun `a second match inside the cooldown does not raise`() {
        val lastRaisedAt = 1_000L
        val now = lastRaisedAt + QuietWordGuard.COOLDOWN_MS - 1
        assertFalse(QuietWordGuard.shouldRaise("pineapple", "pineapple again", lastRaisedAt, now))
    }

    @Test
    fun `a match exactly at the cooldown boundary raises`() {
        val lastRaisedAt = 1_000L
        val now = lastRaisedAt + QuietWordGuard.COOLDOWN_MS
        assertTrue(QuietWordGuard.shouldRaise("pineapple", "pineapple again", lastRaisedAt, now))
    }

    @Test
    fun `a match well after the cooldown raises again`() {
        val lastRaisedAt = 1_000L
        val now = lastRaisedAt + QuietWordGuard.COOLDOWN_MS + 60_000L
        assertTrue(QuietWordGuard.shouldRaise("pineapple", "pineapple", lastRaisedAt, now))
    }

    @Test
    fun `the cooldown is ten minutes`() {
        assertEqualsTenMinutes(QuietWordGuard.COOLDOWN_MS)
    }

    private fun assertEqualsTenMinutes(ms: Long) {
        org.junit.Assert.assertEquals(10 * 60_000L, ms)
    }
}
