package com.safeshade.cloud

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.safeshade.cloud.dto.CircleMemberRow
import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.InviteRow
import com.safeshade.cloud.dto.SubscriptionRow
import com.safeshade.cloud.dto.isoToEpochMillis
import com.safeshade.cloud.sync.CloudKeys
import com.safeshade.cloud.sync.Outbox
import com.safeshade.cloud.sync.SyncEngine
import com.safeshade.cloud.sync.cloudDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * The Circle, as a piece of live state and a handful of things a person can do
 * to it.
 *
 * ### What it is responsible for
 *
 *  * Resolving **which** circle the signed-in account owns, once, and caching
 *    it. Everything circle-scoped is unsendable without this: `circle_id` is
 *    `not null` on every table.
 *  * Turning pulled `circle_members`, `invites` and `subscriptions` rows into
 *    [CloudState], because those three have no repository - see that class.
 *  * Holding the Realtime subscriptions for `alerts` and `messages` while
 *    somebody is signed in, and tearing them down when they are not.
 *  * The four actions: invite, accept an invitation, set the developer tier,
 *    read the heat map.
 *
 * ### What it is not
 *
 * Not a ViewModel and not a cache of anything the app owns. Every field of
 * [CloudState] is a server fact this phone has been told; none of it is
 * persisted, so a cold start shows an empty Circle until the first pull rather
 * than a remembered one that may have changed overnight. The one exception is
 * the circle id itself, which is cached on disk because asking for it again on
 * every foreground would be a round trip that almost always returns the same
 * uuid.
 */
class CircleManager(
    appContext: Context,
    private val client: CloudClient,
    private val outbox: Outbox,
    private val scope: CoroutineScope
) {

    private val context: Context = appContext.applicationContext

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val _state = MutableStateFlow(CloudState())

    /** The Circle as this phone currently understands it. */
    val state: StateFlow<CloudState> = _state.asStateFlow()

    /**
     * Delivery outcomes for invitations sent in this process, by invite id.
     *
     * The `invites` table has no column for whether the email arrived - and
     * should not: a delivery is an event, not a property of the invitation, and
     * `alert_deliveries` exists for the messages where the record matters. So a
     * failure reported by `send-invite` is overlaid here, in memory, on top of
     * the pulled row. It is lost on a process restart, at which point the invite
     * reads as Pending again - which is true: nobody knows whether it arrived.
     */
    private val deliveryOutcomes = mutableMapOf<String, InviteStatus>()

    /** Serialises the read-modify-write of the cached circle id. */
    private val circleLock = Mutex()

    private var realtimeJobs: List<Job> = emptyList()

    /**
     * Where a Realtime row goes. Set by `CloudContainer` to the same
     * `onRowsPulled` the pull uses.
     *
     * A row arriving on a socket and a row arriving in a pull are the same row
     * and must take the same path; two paths would mean two merge rules, and
     * the one that drifts is always the one nobody is looking at.
     */
    var onRealtimeRow: (suspend (String, List<JsonObject>) -> Unit)? = null

    /**
     * Queues everything this phone already holds. Set by `CloudContainer` to
     * `SyncBackfill.enqueueAll`.
     *
     * A lambda rather than a constructor parameter for the same reason
     * [onRealtimeRow] is one: this class is built before the payload source,
     * because the payload source has to be able to ask it for the circle id.
     */
    var onBackfill: (suspend () -> Int?)? = null

    /** The four things a person can do to their Circle. */
    val actions: CircleActions = CircleActions()

    // ============================================
    // LIFECYCLE
    // ============================================

    /**
     * Watches the session and keeps everything downstream of it in step.
     *
     * Signing in resolves the circle, drains whatever accumulated while signed
     * out, and opens the two Realtime subscriptions. Signing out clears the
     * cached circle id, every pull cursor and the whole of [CloudState] - a
     * second account on this phone must not inherit the first one's family, and
     * a cursor from the old circle would make the new one look empty forever.
     */
    fun start(syncEngine: SyncEngine) {
        scope.launch {
            client.session.collectLatest { session ->
                when (session) {
                    is CloudSession.SignedIn -> onSignedIn(session, syncEngine)
                    CloudSession.Guest -> onSignedOut()
                    // Loading and Disabled are not sign-outs. Clearing state for
                    // a session that is still being restored would wipe the
                    // circle on every cold start.
                    else -> Unit
                }
            }
        }
        scope.launch {
            restoreDevTier()
            // Before the first pull lands, so a cold start draws yesterday's
            // Circle rather than an empty page.
            restoreCircleState()
        }
    }

    private suspend fun onSignedIn(session: CloudSession.SignedIn, syncEngine: SyncEngine) {
        // A session with no user id is not a session anything can be attributed
        // to, and every row this would push carries an author.
        if (session.userId.isBlank()) return
        val circle = ensureCircle()
        _state.value = _state.value.copy(circleId = circle)
        if (circle != null) {
            // The records that existed before anybody signed in were never
            // queued, because the hooks only fire on a write. Without this the
            // Account page reads "Nothing yet" against a phone full of data.
            backfillOnce(session.userId, circle, force = false)
            startRealtime(circle)
        }
        // Everything queued while signed out has a circle to go to now.
        syncEngine.kick()
    }

    private suspend fun onSignedOut() {
        stopRealtime()
        circleLock.withLock {
            context.cloudDataStore.edit { it.remove(CloudKeys.CIRCLE_ID) }
            memo = null
        }
        outbox.clearPullCursors()
        deliveryOutcomes.clear()
        clearCircleState()
        _state.value = CloudState(devTierOverride = _state.value.devTierOverride)
    }

    /**
     * The one-off backfill for this `(account, circle)` pair.
     *
     * Idempotent through a marker in the cloud DataStore, and the marker is
     * written **after** the queueing rather than before. Written first, a
     * process killed mid-backfill would leave the pair marked done with half
     * its records never queued and nothing that would ever queue them again;
     * written after, the worst case is that a few already-queued records are
     * queued a second time, which the outbox deduplicates by `(table, id)`
     * anyway.
     *
     * @param force skips the marker. That is "Sync Now": a person who has been
     *   told their data is not there and has pressed the button is entitled to
     *   have it tried again, whatever a flag on disk says.
     */
    private suspend fun backfillOnce(
        userId: String,
        circleId: String,
        force: Boolean
    ): Int? = backfillLock.withLock {
        val marker = "$userId:$circleId"
        val done = context.cloudDataStore.data.map { it[CloudKeys.BACKFILL_DONE] }.first()
            .orEmpty()
        if (!force && marker in done) return@withLock 0

        val queued = onBackfill?.invoke() ?: return@withLock null
        context.cloudDataStore.edit { prefs ->
            prefs[CloudKeys.BACKFILL_DONE] = done + marker
        }
        queued
    }

    private val backfillLock = Mutex()

    // ============================================
    // THE CIRCLE ID
    // ============================================

    /**
     * The signed-in account's circle, from the cache or from the server.
     *
     * `ensure_own_circle()` is idempotent and creates the circle if this account
     * predates the bootstrap migration, so calling it is safe at any time - see
     * `supabase/migrations/0005_bootstrap_circle.sql`. It is still only called
     * when the cache is empty, because "safe" and "free" are different things
     * and this runs on every foreground.
     *
     * Returns null rather than throwing when there is no session or the call
     * fails; a push with no circle stays queued and un-penalised.
     */
    suspend fun ensureCircle(): String? = circleLock.withLock {
        val cached = context.cloudDataStore.data.map { it[CloudKeys.CIRCLE_ID] }.first()
        if (!cached.isNullOrBlank()) return@withLock cached

        when (val result = client.rpc("ensure_own_circle")) {
            is CloudResult.Ok -> {
                val id = (result.value as? JsonPrimitive)?.contentOrNullSafe()
                if (id.isNullOrBlank()) {
                    null
                } else {
                    context.cloudDataStore.edit { it[CloudKeys.CIRCLE_ID] = id }
                    memo = id
                    id
                }
            }

            is CloudResult.Failed -> {
                _state.value = _state.value.copy(lastError = result.reason)
                null
            }

            CloudResult.Disabled -> null
        }
    }

    /**
     * The circle id, as cheaply as possible.
     *
     * Called once per queued record on every drain, so the in-memory memo is
     * not a micro-optimisation: without it a queue of five hundred records
     * would be five hundred DataStore reads, on disk, in the middle of the one
     * operation that has to finish before a fall alert reaches anybody.
     *
     * Falls through to [ensureCircle] the first time, which is the round trip
     * that creates the circle for an account that predates the bootstrap
     * migration.
     */
    suspend fun cachedCircleId(): String? {
        memo?.let { return it }
        val stored = context.cloudDataStore.data.map { it[CloudKeys.CIRCLE_ID] }.first()
            ?.takeIf { it.isNotBlank() }
        val resolved = stored ?: ensureCircle()
        memo = resolved
        return resolved
    }

    @Volatile
    private var memo: String? = null

    // ============================================
    // PULLED ROWS
    // ============================================

    /**
     * The three tables with no repository.
     *
     * Rows are merged into the existing lists by id rather than replacing them,
     * because the pull is cursored: it returns what *changed*, and a phone that
     * treated that as the whole list would show one member every time one
     * member's name was edited.
     */
    suspend fun onSideRows(table: String, rows: List<JsonObject>) {
        applySideRows(table, rows)
        persistCircleState()
    }

    private fun applySideRows(table: String, rows: List<JsonObject>) {
        when (table) {
            CloudTables.CIRCLE_MEMBERS -> applyMembers(decode(rows, CircleMemberRow.serializer()))
            CloudTables.INVITES -> applyInvites(decode(rows, InviteRow.serializer()))
            CloudTables.SUBSCRIPTIONS -> applySubscriptions(
                decode(rows, SubscriptionRow.serializer())
            )

            else -> Unit
        }
    }

    private fun applyMembers(rows: List<CircleMemberRow>) {
        val current = _state.value.members.associateBy { it.userId }.toMutableMap()
        for (row in rows) {
            val userId = row.userId ?: continue
            if (!row.deletedAt.isNullOrBlank()) {
                current.remove(userId)
                continue
            }
            current[userId] = CircleMember(
                userId = userId,
                name = row.displayName?.takeIf { it.isNotBlank() },
                // See CircleMember: one member cannot read another's email.
                email = (client.session.value as? CloudSession.SignedIn)
                    ?.takeIf { it.userId == userId }?.email,
                role = CircleRole.fromWire(row.role)
            )
        }
        _state.value = _state.value.copy(members = current.values.sortedBy { it.role.ordinal })
    }

    private fun applyInvites(rows: List<InviteRow>) {
        val current = _state.value.invites.associateBy { it.id }.toMutableMap()
        for (row in rows) {
            val id = row.id ?: continue
            if (!row.deletedAt.isNullOrBlank()) {
                current.remove(id)
                continue
            }
            current[id] = CircleInvite(
                id = id,
                email = row.email.orEmpty(),
                role = CircleRole.fromWire(row.role),
                status = statusOf(row),
                sentAt = row.createdAt.isoToEpochMillis(fallback = 0L)
            )
        }
        _state.value = _state.value.copy(invites = current.values.sortedByDescending { it.sentAt })
    }

    /**
     * Accepted beats expired beats a delivery failure beats pending.
     *
     * The order is the point. An invitation that was accepted is accepted even
     * if the email this phone sent bounced - somebody got the link another way,
     * and showing "could not send" next to a person who is plainly in the
     * Circle would be the app arguing with the evidence.
     */
    private fun statusOf(row: InviteRow): InviteStatus {
        if (!row.acceptedAt.isNullOrBlank()) return InviteStatus.Accepted
        val expiry = parseServerInstant(row.expiresAt)
        if (expiry != null && expiry.isBefore(Instant.now())) return InviteStatus.Expired
        return deliveryOutcomes[row.id] ?: InviteStatus.Pending
    }

    /**
     * The tier.
     *
     * A row whose `status` says the subscription is over does not grant its
     * tier: the webhook writes `canceled` or `past_due` and leaves `tier` as it
     * was, so reading `tier` alone would keep Pro switched on for somebody whose
     * card was declined in March. `active` and `trialing` count; a blank status
     * counts, because the row exists and nothing has said otherwise.
     */
    private fun applySubscriptions(rows: List<SubscriptionRow>) {
        val live = rows
            .filter { it.deletedAt.isNullOrBlank() }
            .filter { row ->
                val status = row.status?.trim()?.lowercase()
                status.isNullOrBlank() || status in ACTIVE_STATUSES
            }
            .maxByOrNull { it.updatedAt.orEmpty() }
        _state.value = _state.value.copy(tier = CloudTier.fromWire(live?.tier))
    }

    // ============================================
    // REALTIME
    // ============================================

    /**
     * Subscribes to `alerts` and `messages`, and only those two.
     *
     * They are the two tables `0004_realtime_publication.sql` puts in the
     * publication, and a subscription to any other is accepted by the server
     * and then silently delivers nothing forever.
     *
     * Each row is handed to exactly the same `onRowsPulled` the pull uses. The
     * flow does not retry itself - `CloudClient.changes` says so, and a flow
     * with a private retry loop would be a second, invisible schedule fighting
     * the engine's. When it completes, the next `kick()` re-runs the pull, which
     * covers whatever the socket missed.
     */
    private fun startRealtime(circleId: String) {
        stopRealtime()
        realtimeJobs = listOf(CloudTables.ALERTS, CloudTables.MESSAGES).map { table ->
            scope.launch {
                client.changes(table, circleId).collect { row ->
                    onRealtimeRow?.invoke(table, listOf(row))
                }
            }
        }
    }

    private fun stopRealtime() {
        realtimeJobs.forEach { it.cancel() }
        realtimeJobs = emptyList()
    }

    // ============================================
    // DEVELOPER TIER
    // ============================================

    // ============================================
    // THE CACHE
    // ============================================

    /**
     * Writes the members, invitations and tier to disk.
     *
     * Deliberately *not* the circle id, which has its own key, and deliberately
     * nothing else: this is a cache of what the server last said, so that a
     * screen has something to draw on a cold start. It is overwritten by the
     * first pull, and a stale copy can only ever be a few seconds old on any
     * phone that has a network.
     */
    private suspend fun persistCircleState() {
        val current = _state.value
        val stored = StoredCircleState(
            members = current.members.map {
                StoredMember(it.userId, it.name, it.email, it.role.wire)
            },
            invites = current.invites.map {
                StoredInvite(it.id, it.email, it.role.wire, it.sentAt, inviteStatusWire(it.status))
            },
            tier = current.tier.wire
        )
        val encoded = runCatching { json.encodeToString(StoredCircleState.serializer(), stored) }
            .getOrNull() ?: return
        context.cloudDataStore.edit { it[CloudKeys.CIRCLE_STATE] = encoded }
    }

    /**
     * Reads it back, without stepping on anything a pull has already delivered.
     *
     * The restore runs on a launched coroutine and a pull can land first. A
     * blanket overwrite would then replace live data with a cache, which is the
     * one thing a cache must never do, so each list is only filled if it is
     * empty.
     */
    private suspend fun restoreCircleState() {
        val raw = context.cloudDataStore.data.map { it[CloudKeys.CIRCLE_STATE] }.first()
        if (raw.isNullOrBlank()) return
        val stored = runCatching {
            json.decodeFromString(StoredCircleState.serializer(), raw)
        }.getOrNull() ?: return

        val current = _state.value
        _state.value = current.copy(
            members = current.members.ifEmpty {
                stored.members.map {
                    CircleMember(it.userId, it.name, it.email, CircleRole.fromWire(it.role))
                }
            },
            invites = current.invites.ifEmpty {
                stored.invites.map {
                    CircleInvite(
                        id = it.id,
                        email = it.email,
                        role = CircleRole.fromWire(it.role),
                        status = inviteStatusOf(it.status),
                        sentAt = it.sentAt
                    )
                }
            },
            tier = if (current.tier == CloudTier.FREE) {
                CloudTier.fromWire(stored.tier)
            } else {
                current.tier
            }
        )
    }

    private suspend fun clearCircleState() {
        context.cloudDataStore.edit { it.remove(CloudKeys.CIRCLE_STATE) }
    }

    private suspend fun restoreDevTier() {
        val stored = context.cloudDataStore.data.map { it[CloudKeys.DEV_TIER] }.first()
        if (stored.isNullOrBlank()) return
        _state.value = _state.value.copy(devTierOverride = CloudTier.fromWire(stored))
    }

    private fun <T> decode(rows: List<JsonObject>, serializer: KSerializer<T>): List<T> =
        rows.mapNotNull { runCatching { json.decodeFromJsonElement(serializer, it) }.getOrNull() }

    /**
     * The four things a person can do to their Circle.
     *
     * An inner class rather than four methods on [CircleManager] so a screen can
     * be handed the verbs without also being handed the Realtime jobs, the
     * cached circle id and the pull plumbing. Same reason `CloudAuth` exists.
     */
    inner class CircleActions internal constructor() {

        /**
         * Invites somebody by email.
         *
         * Two outcomes are reported and they are not the same thing. A
         * [CloudResult.Failed] means no invitation was created - the caller is
         * not an owner of this circle, or the request never arrived. A
         * [CloudResult.Ok] carrying an [InviteStatus.Failed] means the
         * invitation **exists** and the email did not go: the link can still be
         * copied out of the app, and the reason is Resend's own words.
         *
         * With no sending domain configured every address except the Resend
         * account owner's comes back that second way. That is the honest
         * behaviour and it must reach the screen intact.
         */
        suspend fun invite(email: String, role: CircleRole): CloudResult<CircleInvite> {
            val circle = ensureCircle()
                ?: return CloudResult.Failed(
                    "Sign in before inviting anyone to your Circle.",
                    retryable = false
                )
            val clean = email.trim().lowercase()
            if (!clean.contains("@")) {
                return CloudResult.Failed("That does not look like an email address.", false)
            }

            val body = buildJsonObject {
                put("circle_id", circle)
                put("email", clean)
                put("role", role.wire)
            }

            return when (val result = client.invoke("send-invite", body)) {
                is CloudResult.Ok -> {
                    val id = result.value["invite_id"]?.jsonPrimitive?.contentOrNullSafe()
                        ?: return CloudResult.Failed(
                            "The invitation did not come back with an id, so it may not exist.",
                            retryable = true
                        )
                    val delivery = result.value["delivery"] as? JsonObject
                    val status = deliveryStatus(delivery)
                    if (status is InviteStatus.Failed) deliveryOutcomes[id] = status

                    val invite = CircleInvite(
                        id = id,
                        email = clean,
                        role = role,
                        status = status,
                        sentAt = System.currentTimeMillis()
                    )
                    // Shown immediately rather than waited for: the row exists,
                    // and an owner who has just invited their sister should see
                    // her on the list before the next pull comes round.
                    _state.value = _state.value.copy(
                        invites = listOf(invite) + _state.value.invites.filterNot { it.id == id }
                    )
                    CloudResult.Ok(invite)
                }

                is CloudResult.Failed -> result
                CloudResult.Disabled -> CloudResult.Disabled
            }
        }

        /**
         * Joins a Circle with a token from an invitation link.
         *
         * `accept_invite` is the only way anybody joins: membership is
         * owner-write, the token is server-minted, and the role comes off the
         * invite row rather than from the caller.
         *
         * Every pull cursor is cleared afterwards. The joined circle's rows are
         * older than this phone's cursors, so without the clear it would ask for
         * "anything changed since yesterday" against a circle it has never read
         * and would never see a thing.
         */
        suspend fun acceptInvite(token: String): CloudResult<Unit> {
            val clean = token.trim()
            if (clean.isEmpty()) {
                return CloudResult.Failed("That invitation link is incomplete.", false)
            }
            // The parameter is named p_token in SQL; PostgREST matches by name.
            val args = buildJsonObject { put("p_token", clean) }
            return when (val result = client.rpc("accept_invite", args)) {
                is CloudResult.Ok -> {
                    val joined = (result.value as? JsonPrimitive)?.contentOrNullSafe()
                    if (!joined.isNullOrBlank()) {
                        circleLock.withLock {
                            context.cloudDataStore.edit { it[CloudKeys.CIRCLE_ID] = joined }
                            memo = joined
                        }
                        outbox.clearPullCursors()
                        _state.value = CloudState(
                            circleId = joined,
                            devTierOverride = _state.value.devTierOverride
                        )
                        // A different circle has never seen this phone's
                        // records, so the pair is unmarked and backfills.
                        val uid = (client.session.value as? CloudSession.SignedIn)?.userId
                        if (!uid.isNullOrBlank()) {
                            backfillOnce(uid, joined, force = false)
                        }
                        startRealtime(joined)
                    }
                    CloudResult.Ok(Unit)
                }

                is CloudResult.Failed -> result
                CloudResult.Disabled -> CloudResult.Disabled
            }
        }

        /**
         * Queues every record this phone already holds, whatever the marker
         * says. What "Sync Now" calls.
         *
         * Returns the number of records queued - which is a count of *intents*,
         * not of rows the server has accepted. Nothing here waits on a network,
         * so a caller must not draw a tick from it; the per-record answer is in
         * `Outbox.states`, where it always was.
         */
        suspend fun enqueueAll(): CloudResult<Int> {
            val userId = (client.session.value as? CloudSession.SignedIn)?.userId
            if (userId.isNullOrBlank()) {
                return CloudResult.Failed(
                    "Sign in before syncing what is on this phone.",
                    retryable = false
                )
            }
            val circleId = ensureCircle()
                ?: return CloudResult.Failed(
                    "Your Circle could not be reached, so nothing was queued.",
                    retryable = true
                )
            val queued = backfillOnce(userId, circleId, force = true)
                ?: return CloudResult.Failed(
                    "There is nothing on this phone to sync yet.",
                    retryable = false
                )
            return CloudResult.Ok(queued)
        }

        /** Sets, or with null clears, the hand-chosen tier. See [CloudState.effectiveTier]. */
        suspend fun setDevTierOverride(tier: CloudTier?) {
            context.cloudDataStore.edit { prefs ->
                if (tier == null) prefs.remove(CloudKeys.DEV_TIER) else prefs[CloudKeys.DEV_TIER] =
                    tier.wire
            }
            _state.value = _state.value.copy(devTierOverride = tier)
        }

        /**
         * The community heat map inside a bounding box.
         *
         * `heatmap_in` returns one row per cell **and kind**; a cell with three
         * falls and five SOS calls is two rows. The map draws danger, not
         * taxonomy, so they are summed per cell here. The k >= 5 floor and the
         * 1.1 km rounding are the materialized view's, and are what make this
         * publishable at all.
         */
        suspend fun heatmapIn(
            minLat: Double,
            maxLat: Double,
            minLon: Double,
            maxLon: Double
        ): CloudResult<List<HeatCell>> {
            val args = buildJsonObject {
                put("min_lat", minLat)
                put("max_lat", maxLat)
                put("min_lon", minLon)
                put("max_lon", maxLon)
            }
            return when (val result = client.rpc("heatmap_in", args)) {
                is CloudResult.Ok -> {
                    val array = result.value as? JsonArray
                        ?: return CloudResult.Ok(emptyList())
                    val totals = LinkedHashMap<Pair<Double, Double>, Int>()
                    for (element in array) {
                        val row = runCatching { element.jsonObject }.getOrNull() ?: continue
                        val lat = row["lat_cell"]?.jsonPrimitive?.doubleOrNull ?: continue
                        val lon = row["lon_cell"]?.jsonPrimitive?.doubleOrNull ?: continue
                        val n = row["incidents"]?.jsonPrimitive?.intOrNull ?: 0
                        val key = lat to lon
                        totals[key] = (totals[key] ?: 0) + n
                    }
                    CloudResult.Ok(totals.map { (cell, n) -> HeatCell(cell.first, cell.second, n) })
                }

                is CloudResult.Failed -> result
                CloudResult.Disabled -> CloudResult.Disabled
            }
        }
    }

    private companion object {
        val ACTIVE_STATUSES = setOf("active", "trialing", "trial", "ok")
    }
}

/**
 * `send-invite`'s per-recipient delivery outcome, as the app reports it.
 *
 * The function answers `{"delivery": {"status": "sent"|"failed"|"unknown",
 * "error": ...}}`. `unknown` means Resend accepted the request and the response
 * could not be read - the message may or may not have gone - and it is mapped
 * to [InviteStatus.Failed] with that stated plainly rather than to Pending.
 * Pending would read as "on its way", which is a claim nobody is in a position
 * to make.
 *
 * Internal rather than private so the parsing can be tested; it is the piece of
 * this file most likely to be wrong quietly.
 */
internal fun deliveryStatus(delivery: JsonObject?): InviteStatus {
    val status = delivery?.get("status")?.jsonPrimitive?.contentOrNullSafe()?.lowercase()
    val error = delivery?.get("error")?.jsonPrimitive?.contentOrNullSafe()?.takeIf {
        it.isNotBlank()
    }
    return when (status) {
        "sent" -> InviteStatus.Pending
        "failed" -> InviteStatus.Failed(error ?: "The invitation email was not accepted.")
        "unknown" -> InviteStatus.Failed(
            error ?: "The email provider did not say whether the invitation was sent."
        )
        // No delivery block at all: the invitation exists, and nothing was said
        // about the email. Pending is the only honest reading.
        else -> InviteStatus.Pending
    }
}

/** `JsonPrimitive.content` without the "null" string that a JsonNull yields. */
internal fun JsonPrimitive.contentOrNullSafe(): String? =
    if (this is kotlinx.serialization.json.JsonNull) null else content

// ============================================================================
// The on-disk shape of the Circle cache
//
// kotlinx.serialization, because this is `cloud/` - `data/local/` is Gson and a
// class carrying both is a review-blocker.
//
// A flat mirror of CloudState rather than CloudState itself, and every field a
// primitive with a default. Serialising the domain types directly would make a
// rename of an enum constant a silent parse failure on somebody's phone, and
// would tie the shape a screen reads to the shape a disk holds - the two change
// for entirely different reasons.
//
// A blob that fails to parse is dropped and the page waits for the pull, which
// is what it did before this cache existed.
// ============================================================================

@kotlinx.serialization.Serializable
internal data class StoredCircleState(
    val members: List<StoredMember> = emptyList(),
    val invites: List<StoredInvite> = emptyList(),
    val tier: String = "free"
)

@kotlinx.serialization.Serializable
internal data class StoredMember(
    val userId: String = "",
    val name: String? = null,
    val email: String? = null,
    val role: String = "guardian"
)

@kotlinx.serialization.Serializable
internal data class StoredInvite(
    val id: String = "",
    val email: String = "",
    val role: String = "guardian",
    val sentAt: Long = 0L,
    val status: String = "pending",
    val reason: String? = null
)

/**
 * An invitation status as one word plus, for a failure, its reason.
 *
 * The provider's message is kept: it is the only thing that tells an owner
 * *why* the email did not arrive, and losing it across a restart would leave
 * them with a red row and no explanation.
 */
internal fun inviteStatusWire(status: InviteStatus): String = when (status) {
    InviteStatus.Pending -> "pending"
    InviteStatus.Accepted -> "accepted"
    InviteStatus.Expired -> "expired"
    is InviteStatus.Failed -> "failed:" + status.reason
}

internal fun inviteStatusOf(wire: String): InviteStatus = when {
    wire == "accepted" -> InviteStatus.Accepted
    wire == "expired" -> InviteStatus.Expired
    wire.startsWith("failed:") -> InviteStatus.Failed(
        wire.removePrefix("failed:").ifBlank { "The invitation email was not accepted." }
    )

    else -> InviteStatus.Pending
}
