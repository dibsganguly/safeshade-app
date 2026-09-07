package com.safeshade.cloud.sync

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * The one DataStore delegate for the file `safeshade_cloud`.
 *
 * ### Why it lives here and not next to its first user
 *
 * `preferencesDataStore` is a property delegate that constructs a `DataStore`
 * the first time it is touched. Declaring a second delegate for the *same file
 * name* anywhere else does not share the store - it builds a second `DataStore`
 * over one file, and the first read throws
 * `IllegalStateException: There are multiple DataStores active for the same
 * file`, at runtime, inside a coroutine, on a phone.
 *
 * [Outbox] declared this delegate privately when it was the only writer. It is
 * no longer: the cached circle id and the developer tier override live in the
 * same file, so the delegate moved here where both can reach it and neither can
 * accidentally declare its own.
 *
 * `safeshade_prefs` (see `data/Preferences.kt`) is deliberately a *different*
 * file. DataStore rewrites the whole file on every `edit`, and the outbox is
 * written on every sync attempt; sharing a file would make every medical-ID
 * read churn on a busy drain.
 */
internal val Context.cloudDataStore by preferencesDataStore(name = "safeshade_cloud")

/**
 * Every key in `safeshade_cloud`.
 *
 * Versioned in the key name, so a breaking change to a stored shape is a new
 * key rather than a silent misparse of the old one. Same convention as
 * `PrefsKeys`.
 */
internal object CloudKeys {

    /** The outbox, as a JSON array of entries. See [Outbox]. */
    val OUTBOX = stringPreferencesKey("outbox_json_v1")

    /** `{"zones":"2026-09-07T10:00:00Z", ...}`. See [Outbox.lastPulledAt]. */
    val PULL_CURSORS = stringPreferencesKey("pull_cursors_json_v1")

    /**
     * The signed-in user's circle id, as last resolved.
     *
     * Cached rather than asked for on every drain: the pull runs on every
     * foreground and every reconnect, and a round trip to `ensure_own_circle`
     * before each one would be a request that almost always returns the same
     * answer. It is cleared on sign-out, so a second account on the same phone
     * cannot inherit the first one's circle.
     */
    val CIRCLE_ID = stringPreferencesKey("circle_id_v1")

    /**
     * A tier chosen by hand on this phone, overriding `subscriptions`.
     *
     * There is no Play Console listing, so a real purchase cannot be made and
     * every paid surface would otherwise be unreachable to look at. Stored
     * rather than held in memory so it survives the process, which is the whole
     * point of it.
     */
    val DEV_TIER = stringPreferencesKey("dev_tier_override_v1")

    /**
     * The `(userId, circleId)` pairs whose one-off backfill has already run.
     *
     * A set rather than a flag, because both halves can change on one phone: a
     * second account can sign in, and accepting an invitation moves the same
     * account into a different circle. Either of those is a fresh server-side
     * world that has never seen this phone's records, and either of them has to
     * backfill again - while a plain foreground, which happens dozens of times a
     * day, must not.
     */
    val BACKFILL_DONE = stringSetPreferencesKey("backfill_done_v1")

    /**
     * The last known Circle: members, invitations and tier, as JSON.
     *
     * A cache of server facts, and named as one. It exists so that a cold start
     * draws the Circle it drew yesterday instead of an empty page while the
     * first pull is in flight - and the pull overwrites it the moment it lands.
     * Nothing is ever *decided* from it that could not be decided again a second
     * later.
     */
    val CIRCLE_STATE = stringPreferencesKey("circle_state_v1")
}
