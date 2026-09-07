package com.safeshade.cloud.repo

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.sync.OutboxPolicy
import com.safeshade.repo.SyncHooks
import com.safeshade.repo.SyncKeys

/**
 * What has to be queued the first time an account signs in.
 *
 * ### The gap this closes
 *
 * The hooks queue a record when it is *written*. Everything written before
 * anybody signed in was therefore never queued, and nothing else ever looks at
 * it again - so a person who has been using SafeShade offline for a month signs
 * in, waits, and finds their Account page saying "Nothing yet" and their
 * server tables empty. The app is not broken and nothing failed; there was
 * simply never an event to queue. The fix is to invent the events once.
 *
 * ### Bounded by construction, so nothing important is evicted
 *
 * `OutboxPolicy.enqueue` caps the queue at [OutboxPolicy.MAX_ENTRIES] and
 * evicts the **oldest** entries when it is exceeded. A backfill that queued a
 * thousand trip-log rows would therefore push the wearer, the medical ID and
 * the emergency contacts straight back out of the queue - the app would sync a
 * year of history and lose the person it belongs to.
 *
 * So the plan is capped here rather than left to the eviction rule:
 *
 *  1. **Identity first, always.** The profile, the wearers, their medical
 *     records, the emergency contacts, the paired devices and the safe zones.
 *     All of these are bounded by `PrefsLimits`, all of them are what the
 *     Circle and Account pages actually draw, and together they are a few dozen
 *     rows.
 *  2. **Then history, newest first, filling whatever budget is left.** An alert
 *     from this morning matters to somebody; one from March is a record. If
 *     something has to be left behind it should be the oldest thing, and this
 *     way it is chosen deliberately instead of by an eviction sort.
 *
 * ### It is a list of ids, not of rows
 *
 * Same discipline as the outbox itself: the plan says *what needs syncing*, and
 * the body is resolved at drain time by [PayloadResolver]. A record edited
 * between the backfill and the drain goes up as it stands, once.
 */
internal object BackfillPlan {

    /**
     * How many of the queue's slots history may take.
     *
     * The remainder is the identity records, which are always queued in full.
     * If identity alone somehow exceeds the cap, history gets nothing - which
     * is the right way round.
     */
    fun of(
        snapshot: SyncSnapshot,
        limit: Int = OutboxPolicy.MAX_ENTRIES
    ): List<Pair<String, String>> {
        val identity = mutableListOf<Pair<String, String>>()

        if (!snapshot.userId.isNullOrBlank()) {
            // The repositories queue the placeholder and `CloudSyncHooks`
            // substitutes the account id; the plan speaks the same language.
            identity += CloudTables.PROFILES to SyncKeys.PROFILE_SELF
        }

        for (wearer in snapshot.wearers) {
            identity += CloudTables.WEARERS to wearer.id
            // An untouched medical record is a row of nulls that helps nobody
            // and costs a queue slot. It is queued the moment somebody fills
            // one field in, by the hook on `setMedicalId`.
            if (wearer.medicalId.filledFieldCount > 0) {
                identity += CloudTables.MEDICAL_IDS to SyncKeys.medical(wearer.id)
            }
            for (contact in wearer.contacts) {
                identity += CloudTables.EMERGENCY_CONTACTS to
                    SyncKeys.contact(wearer.id, contact.phone)
            }
        }

        for (contact in snapshot.globalContacts) {
            identity += CloudTables.EMERGENCY_CONTACTS to
                SyncKeys.contact(wearerId = null, phone = contact.phone)
        }

        for (device in snapshot.pairedDevices) {
            identity += CloudTables.DEVICES to SyncKeys.device(device.address)
        }

        for (zone in snapshot.zones) {
            identity += CloudTables.ZONES to zone.id
        }

        val budget = (limit - identity.size).coerceAtLeast(0)
        val history = mutableListOf<Pair<String, String>>()

        // Newest first while choosing, so the ones that get dropped are the
        // oldest; then reversed, so the newest are enqueued last and survive
        // any eviction that a concurrent write might still cause.
        val alerts = snapshot.alerts.sortedByDescending { it.timestamp }
        val messages = snapshot.messages.sortedByDescending { it.timestamp }
        val perTable = budget / 2

        history += alerts.take(perTable).map { CloudTables.ALERTS to it.id }
        history += messages.take(budget - history.size).map { CloudTables.MESSAGES to it.id }

        return identity.distinct() + history.reversed().distinct()
    }
}

/**
 * Runs a [BackfillPlan] through the ordinary write path.
 *
 * Through [SyncHooks] rather than straight into the `Outbox` on purpose: the
 * hooks are what translate the `profiles` placeholder into the account id, and
 * they are what kick the engine. A backfill that reached past them would be a
 * second, subtly different definition of "queue this record", and the one that
 * drifts is always the one nobody is looking at.
 */
class SyncBackfill(
    private val payloads: RepositoryPayloadSource,
    private val hooks: SyncHooks
) {

    /**
     * Queues everything this phone already holds.
     *
     * @return how many records were queued, or null when there was nothing to
     *   queue against - no circle resolved yet, so no `circle_id` to stamp. The
     *   distinction matters to the caller: null must not be recorded as "the
     *   backfill has been done".
     */
    suspend fun enqueueAll(): Int? {
        val snapshot = payloads.snapshot() ?: return null
        val plan = BackfillPlan.of(snapshot)
        for ((table, recordId) in plan) {
            hooks.onUpsert(table, recordId)
        }
        return plan.size
    }
}
