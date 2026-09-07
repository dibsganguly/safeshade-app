package com.safeshade.cloud.sync

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.safeshade.cloud.CloudClient
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.parseServerInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Where a row's current contents come from, at the moment it is about to be
 * sent.
 *
 * The outbox stores *that* a record needs syncing, never *what* it contains
 * (see [OutboxEntry]). This is the other half of that decision: at drain time
 * the engine asks the repositories for the record as it stands right now, so a
 * zone edited four times while offline sends its final shape once, rather than
 * four stale versions in sequence.
 *
 * Phase 2 implements this over `ZoneRepository`, `SafetyRepository`,
 * `ProfileRepository` and the rest. Phase 1 installs [NoPayloadSource].
 */
interface PayloadSource {
    /**
     * The row body for `(table, recordId)`, or null.
     *
     * **Null means "cannot resolve this right now", and it is not a failure.**
     * The engine skips the entry without counting an attempt against it. That
     * distinction is load-bearing: with the no-op source installed in Phase 1,
     * counting attempts would march every queued record to
     * [OutboxPolicy.MAX_ATTEMPTS] and paint the UI with "this did not reach
     * SafeShade Cloud" failures that never happened.
     */
    suspend fun payloadFor(table: String, recordId: String, op: OutboxOp): JsonElement?

    /**
     * Whether this source is in a position to resolve anything at all.
     *
     * False means "not signed in, or no circle yet" - a state of the *app*, not
     * of any record. The whole push is skipped, nothing is penalised, and the
     * queue waits. True means a null from [payloadFor] is a statement about
     * that one record, and it is counted (see [OutboxPolicy.MAX_SKIPS]).
     *
     * Without this distinction the two are indistinguishable, and either every
     * queued record on an offline-only phone marches to Failed, or a record
     * that can never be sent reads as Syncing forever. Both have happened.
     */
    suspend fun isReady(): Boolean = true
}

/**
 * The no-op source: resolves nothing, so nothing is ever sent.
 *
 * Still installed on a build with no cloud configured, and still the reason a
 * null must never count as an attempt.
 */
object NoPayloadSource : PayloadSource {
    override suspend fun payloadFor(table: String, recordId: String, op: OutboxOp): JsonElement? =
        null

    /** Never ready, so nothing queued against it is ever penalised. */
    override suspend fun isReady(): Boolean = false
}

/**
 * The receiving end of a pull.
 *
 * Deliberately a stub in Phase 1. Writing rows *back* into the repositories
 * needs a merge rule for every table — last-write-wins is fine for a zone's
 * radius and completely wrong for an alert's outcome, where "dismissed by the
 * guardian" must not be overwritten by a stale "pending" from another phone —
 * and inventing those rules before there is a second phone to test them against
 * would be guessing.
 */
interface PullSource {
    /** Which circle-scoped tables to pull, in order. Empty disables the pull. */
    val tables: List<String>

    /**
     * Tables scoped to the signed-in user rather than to a circle.
     *
     * `profiles` and `subscriptions` have no `circle_id` column at all - a
     * profile belongs to an account and a subscription follows the person who
     * paid, across every circle they are in - so filtering them by circle is a
     * 400 from PostgREST, not an empty result. They go through
     * [CloudClient.selectOwn] instead.
     */
    val userTables: List<String> get() = emptyList()

    /**
     * Tables read in full on every pull, with no `updated_at` cursor.
     *
     * The cursor is the right protocol for anything that grows - a trip log, a
     * message thread - and exactly the wrong one for a small list the app has
     * to be able to *render*. `circle_members` has three rows and changes
     * monthly, so a cursored pull on a fresh process returns nothing at all and
     * the Guardians page reads "Reading the Circle..." forever, correctly
     * reporting that it has been told nothing while the server holds the
     * answer.
     *
     * A table listed here is fetched whole and advances no cursor.
     */
    val fullPullTables: Set<String> get() = emptySet()

    /** The circle to pull for, or null when the user is not in one yet. */
    suspend fun circleId(): String?

    /** The signed-in user id, or null. Only needed for [userTables]. */
    suspend fun userId(): String? = null

    /** Called with each table's changed rows. */
    suspend fun onRowsPulled(table: String, rows: List<JsonObject>)
}

/** The no-op pull source: pulls nothing. */
object NoPullSource : PullSource {
    override val tables: List<String> = emptyList()
    override suspend fun circleId(): String? = null
    override suspend fun onRowsPulled(table: String, rows: List<JsonObject>) = Unit
}

/**
 * Drains the [Outbox] to the server, and pulls what changed back.
 *
 * ### Single-flight
 *
 * A drain runs under a [Mutex] and a second request while one is running is
 * **dropped, not queued**. Three triggers fire this — an explicit `kick()`,
 * the app coming to the foreground, and the connectivity callback — and on a
 * phone that reconnects while being unlocked, all three land inside about two
 * hundred milliseconds. Queueing them would send every row three times; a
 * `tryLock` sends it once, because a drain that starts after them will pick up
 * anything they would have.
 *
 * ### Three triggers, and why each one is needed
 *
 *  - **`kick()`** — a write just happened and the user is watching. Without it
 *    a save appears to do nothing for up to fifteen minutes.
 *  - **`ON_START` on the process** — the app was reopened. This is the one that
 *    catches the case of a phone that was offline all night and is now in the
 *    kitchen on Wi-Fi with the app in hand.
 *  - **Connectivity `onAvailable`** — the radio just associated, and the app may
 *    be in the background with a fall alert queued. This is the trigger that
 *    actually matters for safety, and it is also the one allowed to be absent;
 *    see [Connectivity].
 *
 * ### It never throws
 *
 * Every server call already returns [CloudResult]. Anything else that goes
 * wrong inside a drain is caught, because this runs on the application scope
 * with no supervisor above it that could report a failure to a user.
 */
class SyncEngine(
    private val client: CloudClient,
    private val outbox: Outbox,
    private val connectivity: Connectivity,
    private val scope: CoroutineScope,
    private val payloadSource: PayloadSource,
    private val pullSource: PullSource,
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    private val drainLock = Mutex()

    /** Set when a drain was asked for while one was already running. */
    private val requestedDuringDrain = java.util.concurrent.atomic.AtomicBoolean(false)

    private val _draining = MutableStateFlow(false)

    /**
     * True only while a drain is actually running.
     *
     * This, and not "is anything queued", is what a page should grey a Sync
     * button on. The first version greyed it on the queue, and an entry sitting
     * in backoff - queued, not moving, due in ten minutes - disabled the one
     * control that would have sent it. The person tapped, nothing happened,
     * nothing said why.
     */
    val draining: StateFlow<Boolean> get() = _draining

    /**
     * The timer for the next backed-off entry. One at a time; re-armed after
     * every drain from whatever the queue then says.
     */
    private var retryJob: Job? = null

    /**
     * True when the connectivity trigger is actually armed.
     *
     * Exposed so that a settings screen can say "syncs when you reconnect" only
     * when that is true. Claiming a behaviour the app does not have is the
     * habit this codebase is trying to break.
     */
    var reconnectTriggerArmed: Boolean = false
        private set

    /** Wires the automatic triggers. Call once, from `CloudContainer`. */
    fun start() {
        reconnectTriggerArmed = connectivity.start { kick() }

        // Lifecycle observers must be added on the main thread, and
        // ProcessLifecycleOwner asserts it.
        scope.launch(Dispatchers.Main) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        kick()
                    }
                }
            )
        }
    }

    /** Asks for a drain. Returns immediately; the work runs on [scope]. */
    fun kick() {
        scope.launch { drainOnce() }
    }

    /**
     * One drain, if no other is in flight - and then another if anything was
     * asked for while it ran.
     *
     * ### Why dropping a concurrent request was not enough
     *
     * The original rule was "a second request while one is running is dropped,
     * not queued", on the reasoning that a drain starting after them picks up
     * whatever they would have. That holds only if a later drain is actually
     * started, and on a cold start nothing starts one:
     *
     * The process foregrounds, `ON_START` fires a drain, and that drain spends
     * several seconds on eight pull requests. During those seconds the session
     * resolves, the backfill queues twelve records and asks for a drain twelve
     * times - and all twelve are dropped, because the first one still holds the
     * lock. It then finishes, having pushed nothing, because the queue was empty
     * when it looked. Twelve records sit at [SyncState.Syncing] until the next
     * foreground or reconnect, which on a phone left on the Account page is
     * never. That is exactly what a user saw: pulls in the server log, not one
     * upsert, and "12 changes are still on this phone only" for minutes.
     *
     * So a dropped request now sets a flag and the holder loops. Conflated, not
     * queued: ten requests during one drain produce one extra pass, not ten.
     *
     * Bounded at [MAX_CONFLATED_PASSES] so that a write loop somewhere else
     * cannot hold this coroutine forever; anything still outstanding after that
     * waits for the next trigger, which is the old behaviour and is safe.
     *
     * @return false when another drain held the lock. Not an error - the
     *   holder will take the work.
     */
    suspend fun drainOnce(): Boolean {
        if (!drainLock.tryLock()) {
            requestedDuringDrain.set(true)
            return false
        }
        try {
            _draining.value = true
            var pass = 0
            do {
                requestedDuringDrain.set(false)
                push()
                pull()
                pass++
            } while (requestedDuringDrain.get() && pass < MAX_CONFLATED_PASSES)
        } catch (t: Throwable) {
            // Cancellation must propagate; everything else is swallowed. A
            // drain that kills the application scope would take the BLE link,
            // the geofence forwarding and the journey escalation with it.
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "drain failed: " + t.javaClass.simpleName + ": " + t.message)
        } finally {
            _draining.value = false
            drainLock.unlock()
        }
        armRetry()
        return true
    }

    /**
     * Arms a drain for the moment the soonest backed-off entry comes due.
     *
     * Without this, an entry in backoff waits for the next *trigger* - a
     * foreground, a reconnect - and on a phone left open on the Account page
     * that is never. One medical record sat "still on this phone" for an
     * afternoon this way, due since eleven minutes past the hour, with every
     * automatic drain having run in the minute before it became due.
     */
    private fun armRetry() {
        retryJob?.cancel()
        val dueAt = outbox.nextDueAt() ?: return
        val wait = (dueAt - now()).coerceAtLeast(0L)
        retryJob = scope.launch {
            delay(wait)
            drainOnce()
        }
    }

    // ============================================
    // PUSH
    // ============================================

    private suspend fun push() {
        val batches = outbox.dueBatches()
        if (batches.isEmpty()) return

        // Not signed in, or no circle resolved yet. Nothing is penalised and
        // nothing is skipped: the queue simply waits for somewhere to go.
        if (!payloadSource.isReady()) {
            Log.w(TAG, "push held: no circle yet, " + batches.values.sumOf { it.size } + " queued")
            return
        }

        for ((table, entries) in batches) {
            val resolved = entries.mapNotNull { entry ->
                val body = bodyFor(entry)
                if (body == null) {
                    // Diagnosable from logcat. There was nothing in the log at
                    // all the first time this went wrong, which is why finding
                    // it took a server-side API trace.
                    Log.w(TAG, "skip " + entry.table + "/" + entry.recordId + " (no row body)")
                    outbox.markSkipped(entry)
                    return@mapNotNull null
                }
                entry to body
            }
            if (resolved.isEmpty()) continue
            Log.w(TAG, "push " + table + " x" + resolved.size)

            when (val result = client.upsert(
                table = table,
                rows = resolved.map { it.second },
                serializer = JsonObject.serializer()
            )) {
                is CloudResult.Ok -> resolved.forEach { (entry, _) ->
                    outbox.markSent(entry.table, entry.recordId)
                }

                is CloudResult.Failed -> {
                    Log.w(TAG, "push " + table + " failed: " + result.reason)
                    resolved.forEach { (entry, _) -> outbox.markFailed(entry, result.reason) }
                }

                // Cloud is off. The entries stay queued, untouched and
                // un-penalised, so that configuring a project later syncs
                // everything that accumulated rather than finding it terminal.
                CloudResult.Disabled -> Unit
            }
        }
    }

    /**
     * The JSON body for one entry, or null to skip it.
     *
     * A [OutboxOp.DELETE] is sent as an upsert carrying `deleted_at`, because
     * the schema soft-deletes — see [OutboxOp.DELETE] for why a row that simply
     * stops existing is invisible to every other phone in the circle.
     */
    private suspend fun bodyFor(entry: OutboxEntry): JsonObject? {
        val element = payloadSource.payloadFor(entry.table, entry.recordId, entry.op) ?: return null
        val row = element as? JsonObject ?: return null
        if (entry.op == OutboxOp.UPSERT) return row
        return buildJsonObject {
            row.forEach { (k, v) -> put(k, v) }
            put("deleted_at", Instant.ofEpochMilli(now()).toString())
        }
    }

    // ============================================
    // PULL
    // ============================================

    private suspend fun pull() {
        pullUserTables()
        if (pullSource.tables.isEmpty()) return
        val circleId = pullSource.circleId() ?: return

        for (table in pullSource.tables) {
            val whole = table in pullSource.fullPullTables
            val since = if (whole) null else parseServerInstant(outbox.lastPulledAt(table))
            when (val result = client.select(table, circleId, since)) {
                is CloudResult.Ok -> {
                    pullSource.onRowsPulled(table, result.value)
                    // The cursor advances only after the rows have been handed
                    // over and accepted. Advancing it first would lose every
                    // row of a batch the app failed to apply, permanently -
                    // they would never be "changed since" again.
                    //
                    // A whole-table pull advances no cursor at all: it is whole
                    // precisely because it has to be readable from nothing on
                    // every fresh process.
                    if (!whole) newestUpdatedAt(result.value)?.let {
                        outbox.setLastPulledAt(table, it)
                    }
                }

                is CloudResult.Failed -> Log.w(TAG, "pull " + table + " failed: " + result.reason)

                // A disabled pull leaves the cursor where it was, so the same
                // window is asked for again next time.
                else -> Unit
            }
        }
    }

    /**
     * The tables keyed by account rather than by circle.
     *
     * Same cursor discipline as the circle pass, and the same rule about the
     * cursor advancing only after the rows have been accepted.
     */
    private suspend fun pullUserTables() {
        if (pullSource.userTables.isEmpty()) return
        val userId = pullSource.userId() ?: return

        for (table in pullSource.userTables) {
            val since = if (table in pullSource.fullPullTables) {
                null
            } else {
                parseServerInstant(outbox.lastPulledAt(table))
            }
            when (val result = client.selectOwn(table, userId, since)) {
                is CloudResult.Ok -> {
                    pullSource.onRowsPulled(table, result.value)
                    if (table !in pullSource.fullPullTables) {
                        newestUpdatedAt(result.value)?.let { outbox.setLastPulledAt(table, it) }
                    }
                }

                is CloudResult.Failed -> Log.w(TAG, "pull " + table + " failed: " + result.reason)

                else -> Unit
            }
        }
    }

    private companion object {
        /**
         * One logcat tag for the whole of sync.
         *
         * `Log.w` rather than `Log.d`: debug is stripped from a release build
         * and filtered out of most logcat views by default, and the entire
         * reason this exists is that a push that never happened left no trace
         * anywhere and had to be found from a server-side API trace instead.
         * None of these lines carries a name, a number, a location or a medical
         * field - only table names and record ids.
         */
        const val TAG = "SafeShadeSync"

        /** See [drainOnce]. */
        const val MAX_CONFLATED_PASSES = 3
    }

    /** The latest `updated_at` in a batch, as the server wrote it. */
    private fun newestUpdatedAt(rows: List<JsonObject>): String? =
        rows.mapNotNull { row ->
            (row["updated_at"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        }.maxOrNull()
}
