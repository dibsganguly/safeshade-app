package com.safeshade.cloud.repo

import com.safeshade.cloud.CloudSession
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.cloud.sync.SyncEngine
import com.safeshade.repo.SyncHooks
import com.safeshade.repo.SyncKeys

/**
 * The cloud's implementation of [SyncHooks]: queue it, then ask for a drain.
 *
 * ### Two lines, and both of them matter
 *
 * The `enqueue` is a DataStore write - fast, local, and the thing that makes the
 * write durable across a process death. The `kick` is a request for a drain that
 * returns immediately; the drain itself runs on the application scope. Nothing
 * here awaits a network call, and that is not an optimisation. A fall alert has
 * to be recorded and its SMS sent at exactly the same speed whether or not the
 * phone has a signal, so a hook that could block on a server would be a safety
 * regression wearing the costume of a feature.
 *
 * ### `profiles` is the one table whose record id the repositories cannot know
 *
 * A profile row's primary key is `auth.users.id`, which lives in the session and
 * not in any repository. So `ProfileRepository` queues the placeholder
 * [SyncKeys.PROFILE_SELF] and the substitution happens here, where the session
 * is. Signed out, there is no id to substitute and the write is not queued -
 * which is correct: a profile row for nobody has no primary key and could never
 * be inserted.
 */
class CloudSyncHooks(
    private val outbox: Outbox,
    private val syncEngine: SyncEngine,
    private val session: () -> CloudSession
) : SyncHooks {

    override suspend fun onUpsert(table: String, recordId: String) =
        enqueue(table, recordId, OutboxOp.UPSERT)

    override suspend fun onDelete(table: String, recordId: String) =
        enqueue(table, recordId, OutboxOp.DELETE)

    private suspend fun enqueue(table: String, recordId: String, op: OutboxOp) {
        val id = resolveId(table, recordId) ?: return
        outbox.enqueue(table, id, op)
        syncEngine.kick()
    }

    private fun resolveId(table: String, recordId: String): String? {
        if (recordId.isBlank()) return null
        if (table != CloudTables.PROFILES) return recordId
        return (session() as? CloudSession.SignedIn)?.userId?.takeIf { it.isNotBlank() }
    }
}
