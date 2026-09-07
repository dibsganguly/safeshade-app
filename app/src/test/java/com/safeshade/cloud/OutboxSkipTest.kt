package com.safeshade.cloud

import com.safeshade.cloud.sync.OutboxEntry
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.OutboxPolicy
import com.safeshade.cloud.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An entry nothing can be sent for must stop claiming to be syncing.
 *
 * The bug: a backfilled record whose row body never resolved sat at
 * [SyncState.Syncing] indefinitely, and the Account page told its owner "12
 * changes are still on this phone only" for minutes about records that were
 * never going to be sent at all. Same family as a tick before a result - the UI
 * stating an outcome nobody had.
 */
class OutboxSkipTest {

    private fun entry(skips: Int = 0, attempts: Int = 0) = OutboxEntry(
        table = "zones",
        recordId = "zone-1",
        op = OutboxOp.UPSERT,
        attempts = attempts,
        skips = skips,
        enqueuedAt = 1L
    )

    @Test
    fun `a skip counts nothing but the skip`() {
        val skipped = OutboxPolicy.onSkipped(entry())

        assertEquals(1, skipped.skips)
        // Nothing was attempted, so nothing is backed off from and no attempt
        // is spent. A skip is not a failure.
        assertEquals(0, skipped.attempts)
        assertEquals(0L, skipped.nextAttemptAt)
        assertEquals(null, skipped.lastError)
    }

    @Test
    fun `one or two skips still read as syncing`() {
        // A drain can land in the same instant as a write. Three consecutive
        // drains is not a race.
        assertEquals(SyncState.Syncing, OutboxPolicy.stateFor(entry(skips = 1)))
        assertEquals(SyncState.Syncing, OutboxPolicy.stateFor(entry(skips = 2)))
        assertFalse(OutboxPolicy.isAbandoned(entry(skips = 2)))
    }

    @Test
    fun `an entry skipped to the limit is abandoned and says so`() {
        val dead = entry(skips = OutboxPolicy.MAX_SKIPS)

        assertTrue(OutboxPolicy.isAbandoned(dead))
        val state = OutboxPolicy.stateFor(dead)
        assertTrue(state is SyncState.Failed)
        assertEquals(OutboxPolicy.ABANDONED_REASON, (state as SyncState.Failed).reason)
        // And it says something a person can read, rather than naming a counter.
        assertFalse(state.reason.contains("skip"))
    }

    @Test
    fun `an abandoned entry stops being due, so it cannot flip back to syncing`() {
        val list = listOf(entry(skips = OutboxPolicy.MAX_SKIPS), entry(skips = 0))
        val due = OutboxPolicy.due(list, now = 10L)

        assertEquals(1, due.size)
        assertEquals(0, due.first().skips)
    }

    @Test
    fun `a real failure still outranks a skip in the reported reason`() {
        val both = entry(skips = OutboxPolicy.MAX_SKIPS, attempts = OutboxPolicy.MAX_ATTEMPTS)
            .copy(lastError = "You are offline.")
        val state = OutboxPolicy.stateFor(both)

        assertEquals("You are offline.", (state as SyncState.Failed).reason)
    }

    @Test
    fun `re-enqueueing a record clears its skips`() {
        // The record was written again, so there is something to send now.
        val fresh = OutboxPolicy.enqueue(
            current = listOf(entry(skips = OutboxPolicy.MAX_SKIPS)),
            entry = entry()
        )

        assertEquals(1, fresh.size)
        assertEquals(0, fresh.first().skips)
        assertEquals(SyncState.Syncing, OutboxPolicy.stateFor(fresh.first()))
    }
}
