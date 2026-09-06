package com.safeshade.cloud

import com.safeshade.cloud.sync.OutboxEntry
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.OutboxPolicy
import com.safeshade.cloud.sync.SyncState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The outbox's decisions, tested without Android.
 *
 * This is the reason `OutboxPolicy` is a separate object from `Outbox`. The
 * queue's failure modes are all *quiet* ones — a backoff that grows too slowly
 * and flattens a battery overnight, a cap that evicts the edit the user just
 * made instead of the oldest one, a dedupe that lets two writes race for one
 * row, an entry that retries forever and so never tells anybody it is broken.
 * None of them throw, none of them show up in a crash report, and all of them
 * are pure arithmetic over immutable lists. So they get pinned here, with the
 * exact numbers written out, rather than described in a comment and drifted
 * away from.
 */
class OutboxStateMachineTest {

    private fun entry(
        table: String = "zones",
        id: String = "z1",
        op: OutboxOp = OutboxOp.UPSERT,
        attempts: Int = 0,
        enqueuedAt: Long = 1_000L,
        nextAttemptAt: Long = 0L,
        lastError: String? = null
    ) = OutboxEntry(table, id, op, attempts, lastError, enqueuedAt, nextAttemptAt)

    // ============================================
    // Backoff
    // ============================================

    @Test
    fun `the backoff schedule is exactly 1, 16, 256, 900, 900 seconds`() {
        // 2^(4n), capped at fifteen minutes. Steeper than a doubling because
        // the common failure is no network at all, and a doubling schedule
        // spends its first two minutes making eight pointless radio wakeups.
        assertEquals(1L, OutboxPolicy.backoffSeconds(0))
        assertEquals(16L, OutboxPolicy.backoffSeconds(1))
        assertEquals(256L, OutboxPolicy.backoffSeconds(2))
        assertEquals(900L, OutboxPolicy.backoffSeconds(3))
        assertEquals(900L, OutboxPolicy.backoffSeconds(4))
    }

    @Test
    fun `the backoff never exceeds fifteen minutes and never goes negative`() {
        // The shift overflows Long at n = 16. n cannot reach that today, but a
        // silently negative delay would make every entry permanently due.
        (0..40).forEach { n ->
            val s = OutboxPolicy.backoffSeconds(n)
            assertTrue("negative at $n: $s", s > 0)
            assertTrue("over cap at $n: $s", s <= OutboxPolicy.MAX_BACKOFF_SECONDS)
        }
        // A negative attempt count is nonsense, but it must not produce a
        // negative delay - that would make the entry permanently due.
        assertEquals(1L, OutboxPolicy.backoffSeconds(-1))
    }

    @Test
    fun `a failure pushes the next attempt out by the backoff`() {
        val failed = OutboxPolicy.onFailure(entry(), reason = "No internet.", now = 10_000L)
        assertEquals(1, failed.attempts)
        assertEquals("No internet.", failed.lastError)
        assertEquals(10_000L + 16_000L, failed.nextAttemptAt)
    }

    // ============================================
    // Terminality
    // ============================================

    @Test
    fun `an entry goes terminal on the fifth failure and not before`() {
        var e = entry()
        repeat(4) { i ->
            e = OutboxPolicy.onFailure(e, "nope", now = 0L)
            assertFalse("terminal too early at attempt ${i + 1}", OutboxPolicy.isTerminal(e))
        }
        e = OutboxPolicy.onFailure(e, "still nope", now = 0L)
        assertEquals(OutboxPolicy.MAX_ATTEMPTS, e.attempts)
        assertTrue(OutboxPolicy.isTerminal(e))
    }

    @Test
    fun `a terminal entry reports the last real reason to the user`() {
        var e = entry()
        repeat(5) { e = OutboxPolicy.onFailure(e, "SafeShade Cloud rejected that request.", 0L) }
        val state = OutboxPolicy.stateFor(e)
        assertTrue(state is SyncState.Failed)
        assertEquals(
            "SafeShade Cloud rejected that request.",
            (state as SyncState.Failed).reason
        )
    }

    @Test
    fun `a terminal entry with no recorded reason still says something`() {
        // Never an empty snackbar. A silent failure is the exact thing this
        // whole mechanism exists to prevent.
        val e = entry(attempts = OutboxPolicy.MAX_ATTEMPTS)
        val state = OutboxPolicy.stateFor(e) as SyncState.Failed
        assertTrue(state.reason.isNotBlank())
    }

    @Test
    fun `a non-terminal entry reads as syncing, not failed`() {
        assertEquals(SyncState.Syncing, OutboxPolicy.stateFor(entry(attempts = 3)))
    }

    // ============================================
    // Dedupe
    // ============================================

    @Test
    fun `enqueueing the same record twice leaves one entry`() {
        val list = OutboxPolicy.enqueue(
            OutboxPolicy.enqueue(emptyList(), entry(enqueuedAt = 100L)),
            entry(enqueuedAt = 500L)
        )
        assertEquals(1, list.size)
    }

    @Test
    fun `a re-enqueue keeps its place in the queue`() {
        // A record edited repeatedly must not starve behind its own edits: if
        // enqueuedAt were refreshed, a frequently-touched row would sit at the
        // back forever while everything else drained past it.
        val first = OutboxPolicy.enqueue(emptyList(), entry(enqueuedAt = 100L))
        val again = OutboxPolicy.enqueue(first, entry(enqueuedAt = 900L))
        assertEquals(100L, again.single().enqueuedAt)
    }

    @Test
    fun `a re-enqueue resets attempts and clears the old error`() {
        // The old failures belonged to the old content. A row rejected for a
        // bad value and since corrected deserves a full five tries.
        val failed = listOf(entry(attempts = 4, lastError = "rejected", nextAttemptAt = 9_000L))
        val again = OutboxPolicy.enqueue(failed, entry()).single()
        assertEquals(0, again.attempts)
        assertNull(again.lastError)
        assertEquals(0L, again.nextAttemptAt)
        assertFalse(OutboxPolicy.isTerminal(again))
    }

    @Test
    fun `a delete replaces a pending upsert for the same record`() {
        val list = OutboxPolicy.enqueue(
            OutboxPolicy.enqueue(emptyList(), entry(op = OutboxOp.UPSERT)),
            entry(op = OutboxOp.DELETE)
        )
        assertEquals(OutboxOp.DELETE, list.single().op)
    }

    @Test
    fun `two different records in the same table both survive`() {
        val list = OutboxPolicy.enqueue(
            OutboxPolicy.enqueue(emptyList(), entry(id = "z1")),
            entry(id = "z2")
        )
        assertEquals(2, list.size)
    }

    @Test
    fun `the same id in two tables is two entries`() {
        // Keys are table-scoped. Folding these together would silently drop one
        // of the two writes.
        val list = OutboxPolicy.enqueue(
            OutboxPolicy.enqueue(emptyList(), entry(table = "zones", id = "x")),
            entry(table = "alerts", id = "x")
        )
        assertEquals(2, list.size)
    }

    // ============================================
    // Cap
    // ============================================

    @Test
    fun `the queue caps at 500 and evicts the oldest`() {
        var list = emptyList<OutboxEntry>()
        repeat(OutboxPolicy.MAX_ENTRIES + 20) { i ->
            list = OutboxPolicy.enqueue(list, entry(id = "z$i", enqueuedAt = i.toLong()))
        }
        assertEquals(OutboxPolicy.MAX_ENTRIES, list.size)
        // The newest edit is the one the user is looking at, so the oldest go.
        assertFalse(list.any { it.recordId == "z0" })
        assertTrue(list.any { it.recordId == "z519" })
        assertEquals(20L, list.minOf { it.enqueuedAt })
    }

    @Test
    fun `staying under the cap changes nothing`() {
        var list = emptyList<OutboxEntry>()
        repeat(OutboxPolicy.MAX_ENTRIES) { i ->
            list = OutboxPolicy.enqueue(list, entry(id = "z$i", enqueuedAt = i.toLong()))
        }
        assertEquals(OutboxPolicy.MAX_ENTRIES, list.size)
        assertTrue(list.any { it.recordId == "z0" })
    }

    // ============================================
    // Due and batching
    // ============================================

    @Test
    fun `an entry in backoff is not due yet`() {
        val list = listOf(entry(id = "soon", nextAttemptAt = 5_000L))
        assertTrue(OutboxPolicy.due(list, now = 4_999L).isEmpty())
        assertEquals(1, OutboxPolicy.due(list, now = 5_000L).size)
    }

    @Test
    fun `a terminal entry is never due again`() {
        // It has already been reported as Failed. Re-sending it would flip the
        // row's state back to "syncing" with no prospect of it changing.
        val list = listOf(entry(attempts = OutboxPolicy.MAX_ATTEMPTS))
        assertTrue(OutboxPolicy.due(list, now = Long.MAX_VALUE).isEmpty())
    }

    @Test
    fun `due entries come back oldest first`() {
        val list = listOf(
            entry(id = "c", enqueuedAt = 300L),
            entry(id = "a", enqueuedAt = 100L),
            entry(id = "b", enqueuedAt = 200L)
        )
        assertEquals(listOf("a", "b", "c"), OutboxPolicy.due(list, 1_000L).map { it.recordId })
    }

    @Test
    fun `batching groups by table so each table is one request`() {
        val list = listOf(
            entry(table = "zones", id = "z1", enqueuedAt = 1L),
            entry(table = "alerts", id = "a1", enqueuedAt = 2L),
            entry(table = "zones", id = "z2", enqueuedAt = 3L)
        )
        val batches = OutboxPolicy.batches(list, now = 1_000L)
        assertEquals(setOf("zones", "alerts"), batches.keys)
        assertEquals(listOf("z1", "z2"), batches.getValue("zones").map { it.recordId })
    }

    @Test
    fun `removing an entry removes only that one`() {
        val list = listOf(entry(id = "z1"), entry(id = "z2"))
        val after = OutboxPolicy.remove(list, "zones", "z1")
        assertEquals(listOf("z2"), after.map { it.recordId })
    }
}
