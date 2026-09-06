package com.safeshade.cloud.sync

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Type

/**
 * The pending-writes queue, on disk.
 *
 * ### Why a queue at all
 *
 * Because the phone this app runs on is, by design, in the worst places for a
 * network: a stairwell, a car park, a village with one bar of signal. Every
 * cloud write in SafeShade is therefore fire-and-forget-and-remember. The UI
 * saves locally and returns immediately; this queue is what makes that honest
 * rather than a lie, because it is also what eventually tells the user when a
 * write never landed (see [SyncState.Failed]).
 *
 * ### Why its own DataStore file
 *
 * `data/Preferences.kt` declares its delegate as
 * `private val Context.dataStore by preferencesDataStore(name = "safeshade_prefs")`.
 * The delegate is private to that file, and declaring a second one anywhere
 * else for the *same file name* does not share the store — it creates a second
 * `DataStore` over one file, which throws
 * `IllegalStateException: There are multiple DataStores active for the same
 * file` on first use, at runtime, in a coroutine. So this uses its own file,
 * `safeshade_cloud`. That is a deviation from the plan for this phase and it is
 * the only one available without editing `Preferences.kt`.
 *
 * It is also, on reflection, the better shape: the outbox is written on every
 * sync attempt, and DataStore rewrites its whole file on each `edit`. Sharing a
 * file with the medical ID, the zone list and the trip log would make every one
 * of those reads churn on a busy drain.
 *
 * ### Gson, not kotlinx.serialization
 *
 * This blob never leaves the phone, so there is no schema negotiation to do,
 * and Gson is already a dependency with an established pattern in this
 * codebase. [OutboxEntryDto] therefore follows that pattern's one hard rule:
 * **every field nullable with a default**, because Gson does not call Kotlin
 * constructors — it allocates a zeroed object through `Unsafe` and assigns only
 * the fields present in the JSON, so a Kotlin default never runs and a
 * non-null property can arrive holding null. `data/local/Dtos.kt` explains this
 * at length.
 *
 * ### What is *not* here
 *
 * All the decisions. Dedupe, backoff, eviction and terminality live in
 * [OutboxPolicy], which has no Android in it and is unit-tested. This class is
 * the shell: read, apply a pure function, write.
 */

/** The on-disk shape of [OutboxEntry]. Every field nullable. See the file KDoc. */
private data class OutboxEntryDto(
    val table: String? = null,
    val recordId: String? = null,
    val op: String? = null,
    val attempts: Int? = null,
    val lastError: String? = null,
    val enqueuedAt: Long? = null,
    val nextAttemptAt: Long? = null
)

private fun OutboxEntry.toDto() = OutboxEntryDto(
    table = table,
    recordId = recordId,
    op = op.name,
    attempts = attempts,
    lastError = lastError,
    enqueuedAt = enqueuedAt,
    nextAttemptAt = nextAttemptAt
)

/**
 * Null table or record id means the entry is unusable, so it is dropped rather
 * than resurrected as `""` — an entry pointing at a table called empty-string
 * would fail forever and occupy a slot.
 */
private fun OutboxEntryDto.toDomainOrNull(): OutboxEntry? {
    val t = table?.takeIf { it.isNotBlank() } ?: return null
    val r = recordId?.takeIf { it.isNotBlank() } ?: return null
    val operation = runCatching { OutboxOp.valueOf(op ?: "") }.getOrDefault(OutboxOp.UPSERT)
    return OutboxEntry(
        table = t,
        recordId = r,
        op = operation,
        attempts = attempts ?: 0,
        lastError = lastError,
        enqueuedAt = enqueuedAt ?: 0L,
        nextAttemptAt = nextAttemptAt ?: 0L
    )
}

class Outbox(
    context: Context,
    /** Overridable so tests and the fake can control time. */
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val listType: Type = object : TypeToken<List<OutboxEntryDto>>() {}.type

    /**
     * Serialises read-modify-write.
     *
     * DataStore's `edit` is already atomic per call, but the outbox's
     * operations are *read the list, apply a policy function, write the list*,
     * and two of those interleaving loses one of the two edits. This is the
     * same reason `MessagingRepository` holds a `listLock`.
     */
    private val lock = Mutex()

    /** The queue, as stored. */
    val entries: StateFlow<List<OutboxEntry>> get() = _entries
    private val _entries = MutableStateFlow<List<OutboxEntry>>(emptyList())

    /**
     * Per-record sync state, keyed by `recordId`.
     *
     * **In memory only, and deliberately.** A [SyncState.Synced] persisted
     * across a process restart would be a claim about the server made by a
     * phone that has not spoken to it since booting — and this app has already
     * shipped one flag that claimed an outcome it had not got. On a cold start
     * every record is [SyncState.LocalOnly] until the outbox says otherwise,
     * which is both true and cheap.
     *
     * Keyed by record id alone rather than by `table/id` because the UI asks
     * "what happened to *this* zone", holding the zone's id and nothing else.
     * Ids are UUIDs, so a collision across tables is not a practical concern.
     */
    val states: StateFlow<Map<String, SyncState>> get() = _states
    private val _states = MutableStateFlow<Map<String, SyncState>>(emptyMap())

    /** Reads the queue off disk. Call once, from [com.safeshade.cloud.CloudContainer]. */
    suspend fun load() {
        val stored = readEntries()
        _entries.value = stored
        _states.value = stored.associate { it.recordId to OutboxPolicy.stateFor(it) }
    }

    private suspend fun readEntries(): List<OutboxEntry> {
        val json = appContext.cloudDataStore.data.map { it[CloudKeys.OUTBOX] }.first()
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<OutboxEntryDto?>>(json, listType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
            // A corrupt blob yields an empty queue rather than an exception.
            // Losing the queue is bad; killing every collector of this flow -
            // which is what a throw inside a DataStore `map` does - is worse.
        }.getOrDefault(emptyList())
    }

    private suspend fun writeEntries(list: List<OutboxEntry>) {
        appContext.cloudDataStore.edit { prefs ->
            prefs[CloudKeys.OUTBOX] = gson.toJson(list.map { it.toDto() })
        }
        _entries.value = list
    }

    /**
     * Queues a write for one record.
     *
     * Idempotent per record: enqueueing the same `(table, recordId)` twice
     * leaves one entry. See [OutboxPolicy.enqueue] for what is kept and what is
     * reset.
     */
    suspend fun enqueue(table: String, recordId: String, op: OutboxOp) {
        lock.withLock {
            val current = _entries.value.ifEmpty { readEntries() }
            val updated = OutboxPolicy.enqueue(
                current,
                OutboxEntry(table = table, recordId = recordId, op = op, enqueuedAt = now())
            )
            writeEntries(updated)
            setState(recordId, SyncState.Syncing)
        }
    }

    /** The server took it. Removes the entry and marks the record synced. */
    suspend fun markSent(table: String, recordId: String) {
        lock.withLock {
            writeEntries(OutboxPolicy.remove(_entries.value, table, recordId))
            setState(recordId, SyncState.Synced(now()))
        }
    }

    /**
     * The attempt failed.
     *
     * The entry stays queued with a longer backoff until it goes terminal, at
     * which point it stays in the list *and* is reported as
     * [SyncState.Failed] — it is not silently dropped. A write that vanished
     * without anybody being told is the exact failure this whole file is here
     * to prevent, and leaving the row visible is what lets a future "retry
     * everything" affordance mean something.
     */
    suspend fun markFailed(entry: OutboxEntry, reason: String) {
        lock.withLock {
            val updated = OutboxPolicy.onFailure(entry, reason, now())
            val list = _entries.value.map { if (it.key == updated.key) updated else it }
            writeEntries(list)
            setState(entry.recordId, OutboxPolicy.stateFor(updated))
        }
    }

    /** Drops an entry outright. For a record the user deleted before it synced. */
    suspend fun forget(table: String, recordId: String) {
        lock.withLock {
            writeEntries(OutboxPolicy.remove(_entries.value, table, recordId))
            _states.value = _states.value - recordId
        }
    }

    /** Everything currently due, batched by table. See [OutboxPolicy.batches]. */
    fun dueBatches(): Map<String, List<OutboxEntry>> =
        OutboxPolicy.batches(_entries.value, now())

    private fun setState(recordId: String, state: SyncState) {
        _states.value = _states.value + (recordId to state)
    }

    // ============================================
    // PULL CURSORS
    // ============================================

    /**
     * When this phone last successfully pulled [table], as an ISO-8601 string.
     *
     * Per table, not global. A single global cursor advanced by a partly
     * successful pull would silently skip every row of the tables that failed,
     * forever — the rows would never be "changed since" again. Per-table
     * cursors make a partial pull merely incomplete.
     *
     * Stored as the server's own string rather than a locally computed time,
     * because the phone's clock and the database's clock disagree, and a cursor
     * a few seconds ahead of the server loses rows.
     */
    suspend fun lastPulledAt(table: String): String? = readCursors()[table]

    suspend fun setLastPulledAt(table: String, isoTimestamp: String) {
        lock.withLock {
            val updated = readCursors() + (table to isoTimestamp)
            appContext.cloudDataStore.edit { prefs ->
                prefs[CloudKeys.PULL_CURSORS] = gson.toJson(updated)
            }
        }
    }

    /**
     * Forgets every pull cursor, so the next pull asks for the whole table.
     *
     * Called when the circle changes - on sign-out, and after accepting an
     * invite into somebody else's circle. Without it, a phone that joins a
     * circle whose rows are older than its own cursor would ask for "changed
     * since yesterday" against a circle it has never read, and would never see
     * anything written before it joined.
     */
    suspend fun clearPullCursors() {
        lock.withLock {
            appContext.cloudDataStore.edit { prefs -> prefs.remove(CloudKeys.PULL_CURSORS) }
        }
    }

    private suspend fun readCursors(): Map<String, String> {
        val json = appContext.cloudDataStore.data.map { it[CloudKeys.PULL_CURSORS] }.first()
        if (json.isNullOrBlank()) return emptyMap()
        val type: Type = object : TypeToken<Map<String, String>>() {}.type
        return runCatching {
            gson.fromJson<Map<String, String>>(json, type).orEmpty()
        }.getOrDefault(emptyMap())
    }
}
