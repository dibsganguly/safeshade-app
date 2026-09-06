package com.safeshade.cloud.sync

/**
 * What has happened to one record on its way to the server.
 *
 * This is per-record, not per-app, and that is the whole point. A single global
 * "syncing" spinner cannot answer the question a guardian actually asks, which
 * is *did this particular safe zone / this medical ID / this alert reach the
 * cloud?* The states map onto exactly four things a row can be showing:
 *
 *  - **[LocalOnly]** — saved on this phone and nowhere else. The honest default
 *    for every record before it is ever queued, and the state a record stays in
 *    forever when cloud is disabled. It is not an error and must not be drawn
 *    as one.
 *  - **[Syncing]** — queued or in flight.
 *  - **[Synced]** — the server acknowledged it, at [Synced.at].
 *  - **[Failed]** — the outbox gave up after [OutboxPolicy.MAX_ATTEMPTS], with a
 *    user-facing [Failed.reason].
 *
 * There is no `Unknown`. The trap this codebase already fell into once was a
 * flag that claimed an outcome it had not got (`isSendingQuickMessage`, and the
 * quick-message tick that confirmed failed sends); a state that means "we did
 * not look" would be that same trap with a nicer name. If the outbox does not
 * know, the record is [LocalOnly].
 */
sealed interface SyncState {

    /** On this phone only. The default, and not a failure. */
    data object LocalOnly : SyncState

    /** Queued or in flight. */
    data object Syncing : SyncState

    /**
     * The server has it.
     *
     * @param at epoch millis of the acknowledgement, not of the local edit.
     */
    data class Synced(val at: Long) : SyncState

    /**
     * The outbox stopped trying.
     *
     * @param reason user-facing, from [com.safeshade.cloud.CloudResult.Failed].
     */
    data class Failed(val reason: String) : SyncState
}
