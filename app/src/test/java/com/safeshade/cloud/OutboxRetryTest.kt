package com.safeshade.cloud

import com.safeshade.cloud.sync.OutboxEntry
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.OutboxPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutboxRetryTest {

    private fun entry(id: String, attempts: Int = 0, next: Long = 0L, skips: Int = 0) =
        OutboxEntry("alerts", id, OutboxOp.UPSERT, attempts = attempts, lastError = "x", enqueuedAt = 1L, nextAttemptAt = next, skips = skips)

    @Test
    fun retryAll_clears_backoff_and_attempts_but_keeps_the_last_error() {
        val out = OutboxPolicy.retryAll(listOf(entry("a", attempts = 5, next = 9_999L)))
        assertEquals(0, out.single().attempts)
        assertEquals(0L, out.single().nextAttemptAt)
        assertEquals("x", out.single().lastError)
        assertEquals(listOf(out.single()), OutboxPolicy.due(out, now = 10L))
    }

    @Test
    fun retryAll_leaves_abandoned_entries_alone() {
        val abandoned = entry("b", skips = OutboxPolicy.MAX_SKIPS, next = 500L)
        val out = OutboxPolicy.retryAll(listOf(abandoned))
        assertEquals(abandoned, out.single())
    }

    @Test
    fun nextDueAt_is_the_soonest_future_attempt_ignoring_terminal_entries() {
        val list = listOf(
            entry("a", next = 300L),
            entry("b", next = 200L),
            entry("c", attempts = OutboxPolicy.MAX_ATTEMPTS, next = 100L),
            entry("d", next = 50L) // already due: not "next"
        )
        assertEquals(200L, OutboxPolicy.nextDueAt(list, now = 60L))
        assertNull(OutboxPolicy.nextDueAt(listOf(entry("d", next = 50L)), now = 60L))
    }
}
