package com.safeshade.cloud.sync

/** What the outbox is asking the server to do with a record. */
enum class OutboxOp {
    /** Insert or replace the row. Carries a payload, resolved at drain time. */
    UPSERT,

    /**
     * Soft-delete the row.
     *
     * Soft, not hard: every table carries `deleted_at`, because a hard delete
     * is invisible to the other phones in a circle. A guardian's phone that has
     * been offline for a week pulls rows changed since it last looked; a row
     * that simply stopped existing produces no change to pull, so the deleted
     * safe zone reappears on their map forever. A tombstone with an
     * `updated_at` is a change like any other.
     */
    DELETE
}

/**
 * One pending write.
 *
 * Note what is *not* here: the payload. The outbox stores an intent to sync a
 * record, identified by table and id, and resolves the body from the
 * repositories at drain time through [PayloadSource]. Storing the body would
 * mean a record edited three times offline sends three stale versions in a row
 * and the server briefly holds each one; resolving late means it sends the
 * current truth, once.
 *
 * @param table the server table name, e.g. `zones`. Snake case, matching SQL.
 * @param recordId the client-minted UUID that is the row's primary key. Client
 *   minted so that the id exists before the row does and an offline edit can
 *   reference it.
 * @param op see [OutboxOp].
 * @param attempts how many drains have failed for this entry. Zero until the
 *   first failure.
 * @param lastError the user-facing reason from the most recent failure, or null.
 * @param enqueuedAt epoch millis of the *first* enqueue for this key. Preserved
 *   across re-enqueues on purpose — see [OutboxPolicy.enqueue].
 * @param nextAttemptAt epoch millis before which this entry must not be tried
 *   again. Zero means "as soon as possible".
 */
data class OutboxEntry(
    val table: String,
    val recordId: String,
    val op: OutboxOp,
    val attempts: Int = 0,
    val lastError: String? = null,
    val enqueuedAt: Long = 0L,
    val nextAttemptAt: Long = 0L
) {
    /** Identity for dedupe. One pending write per record, never two. */
    val key: String get() = "$table/$recordId"
}

/**
 * The outbox's decisions, with no Android and no I/O anywhere in sight.
 *
 * Split out from [Outbox] for one reason: this is the part that can be wrong in
 * a way nobody notices for weeks — a backoff that grows too slowly and flattens
 * a battery, a cap that evicts newest-first and throws away the edit the user
 * just made, a dedupe that lets two writes race for one row. All of that is
 * pure arithmetic over immutable lists, so it is unit-testable on the JVM
 * without Robolectric, and `OutboxStateMachineTest` tests exactly these
 * functions. [Outbox] is then a thin, boring DataStore shell around it.
 */
object OutboxPolicy {

    /**
     * How many pending writes are kept.
     *
     * DataStore rewrites its whole file on every `edit` (see `PrefsLimits`), so
     * an uncapped outbox makes every unrelated preference write slower forever.
     * 500 records is far more than a phone accumulates between two successful
     * drains; reaching it means sync has been broken for a long time.
     */
    const val MAX_ENTRIES = 500

    /**
     * Failures before an entry is abandoned and reported as
     * [SyncState.Failed].
     *
     * Five, not infinite. An entry the server keeps rejecting is not going to
     * start being accepted, and a queue that retries forever is a queue that
     * never tells the user anything is wrong — which is the failure mode this
     * whole file exists to avoid.
     */
    const val MAX_ATTEMPTS = 5

    /** Backoff ceiling. Fifteen minutes. */
    const val MAX_BACKOFF_SECONDS = 15 * 60L

    /**
     * Seconds to wait before the next attempt, given [attempts] failures so far.
     *
     * The schedule is `2^(4n)` seconds, capped:
     *
     * | attempts | seconds |
     * |---|---|
     * | 0 | 1 |
     * | 1 | 16 |
     * | 2 | 256 |
     * | 3 | 900 (4096, capped) |
     * | 4 | 900 |
     *
     * Steeper than a doubling on purpose. The common failure here is *no
     * network at all*, and a doubling schedule spends its first two minutes
     * making eight pointless radio wakeups; this one makes three. The cap keeps
     * a long outage from pushing the next attempt hours out, so a phone that
     * finds Wi-Fi is never more than fifteen minutes from trying — and in
     * practice not even that, because [SyncEngine] also drains on the
     * connectivity callback and on app foreground.
     */
    fun backoffSeconds(attempts: Int): Long {
        if (attempts <= 0) return 1L
        // 2^(4n) overflows Long at n = 16, and n never exceeds MAX_ATTEMPTS,
        // but the shift is still guarded so a future change cannot make this
        // silently negative.
        val shift = 4 * attempts
        if (shift >= 62) return MAX_BACKOFF_SECONDS
        val seconds = 1L shl shift
        return if (seconds > MAX_BACKOFF_SECONDS) MAX_BACKOFF_SECONDS else seconds
    }

    /** [backoffSeconds] in millis, for arithmetic against `currentTimeMillis`. */
    fun backoffMillis(attempts: Int): Long = backoffSeconds(attempts) * 1000L

    /**
     * Adds or replaces one entry.
     *
     * **Dedupe is by `(table, recordId)`,** so a record edited five times while
     * offline occupies one slot and sends once. The replacement keeps the
     * original [OutboxEntry.enqueuedAt]: a record that has been waiting since
     * this morning should not go to the back of the queue because the user
     * touched it again, or a frequently-edited row could starve indefinitely
     * behind its own edits.
     *
     * It *does* reset [OutboxEntry.attempts] and clear [OutboxEntry.lastError].
     * The old failures belonged to the old content. A row that was rejected for
     * a bad value and has since been corrected deserves a full five tries.
     *
     * Eviction, when over [MAX_ENTRIES], drops the **oldest** entries. The
     * newest edit is the one the user is looking at.
     */
    fun enqueue(current: List<OutboxEntry>, entry: OutboxEntry): List<OutboxEntry> {
        val existing = current.firstOrNull { it.key == entry.key }
        val merged = entry.copy(
            attempts = 0,
            lastError = null,
            nextAttemptAt = 0L,
            enqueuedAt = existing?.enqueuedAt ?: entry.enqueuedAt
        )
        val without = current.filterNot { it.key == entry.key }
        val appended = without + merged
        return if (appended.size <= MAX_ENTRIES) appended else {
            appended.sortedBy { it.enqueuedAt }.drop(appended.size - MAX_ENTRIES)
        }
    }

    /** Removes an entry by key, after a successful send. */
    fun remove(current: List<OutboxEntry>, table: String, recordId: String): List<OutboxEntry> =
        current.filterNot { it.table == table && it.recordId == recordId }

    /**
     * Records a failure against an entry.
     *
     * Returns the updated entry with [OutboxEntry.attempts] incremented and
     * [OutboxEntry.nextAttemptAt] pushed out by [backoffMillis]. The caller
     * decides whether it is now terminal — see [isTerminal] — because dropping
     * a terminal entry silently is exactly the behaviour that would let a write
     * disappear without anybody being told.
     */
    fun onFailure(entry: OutboxEntry, reason: String, now: Long): OutboxEntry {
        val attempts = entry.attempts + 1
        return entry.copy(
            attempts = attempts,
            lastError = reason,
            nextAttemptAt = now + backoffMillis(attempts)
        )
    }

    /** True when [entry] has exhausted [MAX_ATTEMPTS] and must be reported. */
    fun isTerminal(entry: OutboxEntry): Boolean = entry.attempts >= MAX_ATTEMPTS

    /**
     * The entries eligible to be sent at [now], oldest first.
     *
     * Terminal entries are excluded: they have already been reported as
     * [SyncState.Failed] and re-sending them would flip a row's state back to
     * "syncing" with no prospect of it changing.
     */
    fun due(current: List<OutboxEntry>, now: Long): List<OutboxEntry> =
        current.filter { !isTerminal(it) && it.nextAttemptAt <= now }
            .sortedBy { it.enqueuedAt }

    /**
     * Groups due entries by table, preserving each table's oldest-first order.
     *
     * Batching matters more than it looks: PostgREST accepts an array of rows
     * in one request, so twenty edited zones are one round trip rather than
     * twenty. On a phone that has just found signal after a walk, that is the
     * difference between a drain finishing and a drain being killed with the
     * process.
     */
    fun batches(current: List<OutboxEntry>, now: Long): Map<String, List<OutboxEntry>> =
        due(current, now).groupBy { it.table }

    /** The [SyncState] an entry currently implies, for the side map. */
    fun stateFor(entry: OutboxEntry): SyncState =
        if (isTerminal(entry)) {
            SyncState.Failed(entry.lastError ?: "This did not reach SafeShade Cloud.")
        } else {
            SyncState.Syncing
        }
}
