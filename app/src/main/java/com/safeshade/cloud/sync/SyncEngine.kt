package com.safeshade.cloud.sync

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.safeshade.cloud.CloudClient
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.parseServerInstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
     * One drain, if no other is in flight.
     *
     * @return false when another drain held the lock. Not an error.
     */
    suspend fun drainOnce(): Boolean {
        if (!drainLock.tryLock()) return false
        try {
            push()
            pull()
        } catch (t: Throwable) {
            // Cancellation must propagate; everything else is swallowed. A
            // drain that kills the application scope would take the BLE link,
            // the geofence forwarding and the journey escalation with it.
            if (t is kotlinx.coroutines.CancellationException) throw t
        } finally {
            drainLock.unlock()
        }
        return true
    }

    // ============================================
    // PUSH
    // ============================================

    private suspend fun push() {
        val batches = outbox.dueBatches()
        for ((table, entries) in batches) {
            val resolved = entries.mapNotNull { entry ->
                val body = bodyFor(entry) ?: return@mapNotNull null
                entry to body
            }
            if (resolved.isEmpty()) continue

            when (val result = client.upsert(
                table = table,
                rows = resolved.map { it.second },
                serializer = JsonObject.serializer()
            )) {
                is CloudResult.Ok -> resolved.forEach { (entry, _) ->
                    outbox.markSent(entry.table, entry.recordId)
                }

                is CloudResult.Failed -> resolved.forEach { (entry, _) ->
                    outbox.markFailed(entry, result.reason)
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
            val since = parseServerInstant(outbox.lastPulledAt(table))
            when (val result = client.select(table, circleId, since)) {
                is CloudResult.Ok -> {
                    pullSource.onRowsPulled(table, result.value)
                    // The cursor advances only after the rows have been handed
                    // over and accepted. Advancing it first would lose every
                    // row of a batch the app failed to apply, permanently -
                    // they would never be "changed since" again.
                    newestUpdatedAt(result.value)?.let {
                        outbox.setLastPulledAt(table, it)
                    }
                }

                // A failed or disabled pull leaves the cursor where it was, so
                // the same window is asked for again next time.
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
            val since = parseServerInstant(outbox.lastPulledAt(table))
            when (val result = client.selectOwn(table, userId, since)) {
                is CloudResult.Ok -> {
                    pullSource.onRowsPulled(table, result.value)
                    newestUpdatedAt(result.value)?.let {
                        outbox.setLastPulledAt(table, it)
                    }
                }

                else -> Unit
            }
        }
    }

    /** The latest `updated_at` in a batch, as the server wrote it. */
    private fun newestUpdatedAt(rows: List<JsonObject>): String? =
        rows.mapNotNull { row ->
            (row["updated_at"] as? kotlinx.serialization.json.JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
        }.maxOrNull()
}
