package com.safeshade.repo

import com.safeshade.platform.PhoneNumbers

/**
 * What a repository tells the cloud when it writes something.
 *
 * ### Why the repositories own this call and nothing above them does
 *
 * A record reaches the outbox when it is *written*, not when a screen happens
 * to remember to say so. Every one of these repositories is reachable from more
 * than one place - the view model, an alarm receiver, an SMS broadcast, the BLE
 * callback that records a fall while the app is in the background - and a hook
 * fired by the caller would be missing from whichever of those paths was added
 * last. The write is the single choke point, so the write is where the hook
 * goes.
 *
 * ### Why it is an interface with a no-op default
 *
 * `repo/` must not depend on `cloud/`. These classes are constructed in unit
 * tests and in the kit gallery with no Supabase project, no network and no
 * outbox; a hard reference to `CloudContainer` would drag Android DataStore,
 * Ktor and a websocket into a test that wanted to check a merge rule. So the
 * dependency points the other way: `cloud/` implements this, `repo/` only knows
 * the two verbs, and [None] is what runs when nobody wired anything up.
 *
 * ### Nothing here throws, and nothing here blocks on a network
 *
 * An implementation writes to the on-disk queue and asks the sync engine for a
 * drain; the drain itself is launched, never awaited. A fall alert must be
 * recorded and its SMS sent at the same speed whether or not the phone has a
 * signal, so a hook that could wait on a server would be a safety regression,
 * not a feature.
 */
interface SyncHooks {

    /**
     * A row was created or changed.
     *
     * @param table the server table name, snake case, e.g. `zones`.
     * @param recordId the **local** id of the record. Translating it to the
     *   uuid the server uses is the cloud layer's job - see
     *   `cloud/repo/CloudIds.kt` - because the per-record sync state the UI
     *   draws is keyed by the id the UI is holding.
     */
    suspend fun onUpsert(table: String, recordId: String)

    /** A row was removed. Travels to the server as a soft delete. */
    suspend fun onDelete(table: String, recordId: String)

    /** The default. Does nothing, and is not an error. */
    object None : SyncHooks {
        override suspend fun onUpsert(table: String, recordId: String) = Unit
        override suspend fun onDelete(table: String, recordId: String) = Unit
    }
}

/**
 * A [SyncHooks] whose real implementation arrives after the repositories do.
 *
 * `AppContainer` builds the repositories first and `CloudContainer` last, and
 * that order is deliberate - handoff7 §2: "`CloudContainer` is the last field of
 * `AppContainer`, after `appStateRepository`, and that position is the
 * enforcement" of cloud being strictly additive. Reordering the container to
 * satisfy a constructor parameter would quietly delete that enforcement, so the
 * parameter is satisfied by this instead and bound a few lines later.
 *
 * Calls made before [bind] are dropped rather than buffered. The only writes
 * that can happen in that window are the ones `AppContainer`'s own
 * initialisation performs, and a queue that replayed them would be queueing
 * rows for a circle nobody has signed into yet.
 */
class LateBoundSyncHooks : SyncHooks {

    @Volatile
    private var delegate: SyncHooks = SyncHooks.None

    fun bind(hooks: SyncHooks) {
        delegate = hooks
    }

    override suspend fun onUpsert(table: String, recordId: String) =
        delegate.onUpsert(table, recordId)

    override suspend fun onDelete(table: String, recordId: String) =
        delegate.onDelete(table, recordId)
}

/**
 * The record ids the repositories hand to [SyncHooks], for the records that
 * have no id of their own.
 *
 * `EmergencyContact`, `MedicalId` and `PairedDevice` were modelled before there
 * was a server to send them to, and none of the three carries an id. Rather
 * than change three DataStore shapes and write a migration for a table nobody
 * has written to yet, the id is a readable composite built here and parsed on
 * the cloud side (`cloud/repo/CloudIds.kt`), which derives the row's uuid from
 * it.
 *
 * Readable rather than hashed on purpose: `Outbox.states` is keyed by record id
 * and the UI looks a record up by the id it is holding, so a `medical_ids`
 * entry and a `wearers` entry for the same person have to stay two distinct
 * keys instead of colliding on one.
 */
object SyncKeys {

    /** Stands in for the signed-in account, whose id the repositories do not know. */
    const val PROFILE_SELF = "self"

    /** The owner segment for the circle-wide contact list, which has no wearer. */
    const val GLOBAL = "global"

    fun medical(wearerId: String): String = "medical:$wearerId"

    /** @param wearerId null for `SafetySettings.emergencyContacts`, the SOS list. */
    fun contact(wearerId: String?, phone: String): String =
        "contact:" + (wearerId ?: GLOBAL) + ":" + PhoneNumbers.digitsOf(phone)

    fun device(address: String): String = "device:" + address.trim().lowercase()
}
