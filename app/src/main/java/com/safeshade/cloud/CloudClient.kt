package com.safeshade.cloud

import android.content.Intent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.Instant

/**
 * Everything the app is allowed to ask of SafeShade Cloud.
 *
 * ### Why this is an interface, and why it is this small
 *
 * `SupabaseCloudClient` cannot run in a unit test, cannot run in a Compose
 * preview, and cannot run at all on a machine with no `SUPABASE_URL` — which is
 * every machine until someone follows `supabase/README.md`. The kit gallery
 * (`ui/board/KitGallery.kt`) has to render account and sync states in both
 * themes with no network, and the whole app has to keep working offline with no
 * project configured at all. All three need a substitute, and a substitute is
 * only cheap to write if the surface is small.
 *
 * So this deliberately does **not** mirror supabase-kt. There is no query
 * builder, no filter DSL, no realtime channel, no bucket abstraction. There are
 * five verbs against tables and storage plus the auth calls, all of them
 * returning [CloudResult], and any richness a screen needs is expressed as SQL
 * in `supabase/migrations/0001_init.sql` or as an edge function — on the server,
 * where it can be changed without shipping an APK.
 *
 * The same principle already runs the BLE side: `DeviceLink` is the interface,
 * `RealDeviceLink` the implementation, and the debug/release `LinkFactory`
 * twins choose between them. This is that pattern applied to the network.
 *
 * ### Nothing here throws
 *
 * Every method returns [CloudResult]. A caller that forgets to handle failure
 * gets a compile-time nudge rather than a coroutine dying somewhere far from
 * the call site. See [CloudResult] for why that rule was written in blood.
 *
 * ### Timestamps
 *
 * `since` is a `java.time.Instant` — available from API 26, which is this app's
 * `minSdk`, so no desugaring is needed. Row DTOs keep their timestamps as
 * strings; see the note in `dto/CircleRows.kt`.
 */
interface CloudClient {

    /**
     * Who is signed in. Never null, never absent — see [CloudSession] for why
     * the "not yet known" case is a state rather than a null.
     */
    val session: StateFlow<CloudSession>

    // ============================================
    // AUTH
    // ============================================

    /**
     * Emails a six-digit sign-in code.
     *
     * A code rather than a magic link, as the default: the wearer's guardian is
     * often signing in on the phone while reading the email on the same phone,
     * and a link that opens a browser tab and bounces back through a deep link
     * has four more ways to fail than typing six digits does. The magic-link
     * template exists too (`supabase/functions/_shared/email/magic-link.html`)
     * for the desktop case.
     */
    suspend fun requestEmailOtp(email: String): CloudResult<Unit>

    /** Exchanges an emailed code for a session. */
    suspend fun verifyEmailOtp(email: String, code: String): CloudResult<Unit>

    suspend fun signInWithPassword(email: String, password: String): CloudResult<Unit>

    suspend fun signUpWithPassword(email: String, password: String): CloudResult<Unit>

    /**
     * Signs in with an ID token obtained from the platform.
     *
     * @param nonce the raw nonce that was passed to the platform's credential
     *   request. Google returns it hashed inside the token; Supabase compares
     *   the two. Omitting it when one was used fails the exchange, which is the
     *   single most common way this call goes wrong.
     */
    suspend fun signInWithIdToken(
        provider: IdProvider,
        idToken: String,
        nonce: String? = null
    ): CloudResult<Unit>

    suspend fun signOut(): CloudResult<Unit>

    /**
     * Deletes the account and everything owned by it.
     *
     * Not a client-side capability in Supabase — deleting an `auth.users` row
     * needs the service role, which must never ship in an APK. This calls the
     * `delete_account()` SQL function created by the migration, which runs
     * `security definer` and can therefore only ever delete `auth.uid()`: the
     * caller's own account, and nobody else's.
     */
    suspend fun deleteAccount(): CloudResult<Unit>

    /**
     * Hands an incoming deep-link intent to the auth layer.
     *
     * Phase 2's job to call, from `MainActivity.onNewIntent` and `onCreate`.
     * It lives on the interface rather than only on the real implementation so
     * that the Activity can call it through `CloudContainer.client` with no cast
     * and no null check when the fake is in use.
     */
    fun handleDeepLink(intent: Intent)

    // ============================================
    // TABLES
    // ============================================

    /**
     * Inserts or replaces rows in [table].
     *
     * Takes an explicit [serializer] rather than a reified type parameter
     * because the outbox calls this from a `when` over table names, where the
     * concrete row type is not known statically. The rows are encoded to JSON
     * here and sent as one array — one request for the whole batch, which is
     * what makes a drain survive a short window of connectivity.
     */
    suspend fun <T> upsert(
        table: String,
        rows: List<T>,
        serializer: KSerializer<T>
    ): CloudResult<Unit>

    /**
     * Reads rows of [table] for one circle.
     *
     * @param since when non-null, only rows whose `updated_at` is strictly
     *   later. This is the whole of the pull protocol: every table carries
     *   `updated_at` maintained by a trigger and indexed with `circle_id`, so a
     *   phone that has been offline for a week asks one question and gets only
     *   what changed. Deletions come back as rows with `deleted_at` set — see
     *   [com.safeshade.cloud.sync.OutboxOp.DELETE] for why they are not simply
     *   absent.
     *
     * Returns raw [JsonObject]s rather than a typed list for the same reason
     * [upsert] takes a serializer: the caller knows the row type, the sync
     * engine does not.
     */
    suspend fun select(
        table: String,
        circleId: String,
        since: Instant? = null
    ): CloudResult<List<JsonObject>>

    /**
     * Reads rows of [table] belonging to one **user** rather than to a circle.
     *
     * `profiles` and `subscriptions` are the two tables that need it, and they
     * need it for opposite reasons: a profile is an account, and a subscription
     * deliberately follows the person who paid rather than the circle they
     * happen to be in, so that a guardian who leaves one household and joins
     * another keeps the tier they are paying for.
     *
     * Neither table has a `circle_id` column, so [select] against them is not
     * an empty answer - it is a 400 from PostgREST naming a column that does
     * not exist. This is a separate method rather than a nullable `circleId`
     * parameter on [select] for exactly that reason: the two filters are not
     * interchangeable and a caller must not be able to pick the wrong one by
     * passing null.
     *
     * Row-level security still decides what comes back. On both of these tables
     * the policy is `auth.uid() = <the user column>`, so this filter narrows a
     * result that was already narrowed; it does not widen anything.
     *
     * @param userId the column value to filter on: `id` for `profiles`, which
     *   is the account's own primary key, and `user_id` for everything else.
     */
    suspend fun selectOwn(
        table: String,
        userId: String,
        since: Instant? = null
    ): CloudResult<List<JsonObject>>

    /**
     * Calls an edge function and returns its JSON body.
     *
     * A non-2xx response is a [CloudResult.Failed], not an [CloudResult.Ok]
     * carrying an error object — otherwise `send-alert-email` returning
     * "nothing was delivered" would read to a caller as a successful send,
     * which is precisely the class of lie this app has already shipped once.
     */
    suspend fun invoke(function: String, body: JsonObject): CloudResult<JsonObject>

    /**
     * Calls a Postgres function through PostgREST and returns whatever it
     * returned, as JSON.
     *
     * Two callers, and they are the reason this exists rather than each screen
     * reaching for `postgrest.rpc` the way `deleteAccount()` does:
     *
     *  - `accept_invite(token)` — the **only** way anybody joins a Circle.
     *    Membership is owner-write on purpose (circle ids travel in invite links
     *    and in every synced row, so "you need an id nobody publishes" is not
     *    access control), and this `security definer` function gated on a
     *    server-minted token is the one door.
     *  - `heatmap_in(bbox)` — the k≥5 aggregated cells, which are only reachable
     *    through a function because the materialized view itself is not exposed.
     *
     * The return type is [JsonElement], not [JsonObject]: `accept_invite`
     * returns a bare uuid and `heatmap_in` returns an array. Narrowing it to an
     * object here would force both callers to wrap their own SQL in a
     * `json_build_object` to satisfy Kotlin, which is a type system leaking into
     * a schema.
     *
     * A function returning `void` answers with an empty body; that arrives as
     * `JsonNull` rather than as a failure. "It ran and had nothing to say" is a
     * success.
     */
    suspend fun rpc(
        function: String,
        args: JsonObject = JsonObject(emptyMap())
    ): CloudResult<JsonElement>

    // ============================================
    // REALTIME
    // ============================================

    /**
     * Rows arriving in [table] for one circle, live.
     *
     * Emits the **new** row for INSERT and UPDATE and nothing else. A DELETE
     * carries only the old record's primary key, and this app does not hard
     * delete anyway — a removal travels as a row with `deleted_at` set, which
     * arrives here as an ordinary UPDATE.
     *
     * ### Why this returns a bare Flow and not a [CloudResult]
     *
     * Because a subscription is not a call with an outcome. Every other method
     * on this interface answers a question once; this one is a tap that is
     * either running or not. Wrapping each row in `Ok` would suggest the absence
     * of rows was itself an answer, and it is not — a quiet Circle and a dropped
     * socket look identical from here, which is exactly why the *displayed*
     * freshness of the Circle tab must keep coming from
     * [com.safeshade.cloud.sync.SyncState] and the pull, never from this.
     *
     * ### Failure and retry, and who owns them
     *
     * Nothing thrown reaches the collector: a transport failure completes the
     * flow quietly. It does **not** retry itself. `SyncEngine` already owns
     * backoff and the foreground/network triggers, and a flow with its own
     * private retry loop would be a second, invisible, uncancellable schedule
     * fighting the first — the classic way a phone in a lift ends up holding a
     * socket open at 2 Hz until the battery goes. The engine re-subscribes.
     *
     * Applies only to `alerts` and `messages` (handoff7 §6 item 5); those are
     * the two tables `0004_realtime_publication.sql` puts in the
     * `supabase_realtime` publication. A subscription to any other table is
     * accepted by the server and then silently delivers nothing, so do not add
     * one without adding the table to that migration first.
     *
     * @param circleId filtered server-side as `circle_id=eq.<id>`. Row-level
     *   security still applies on top; this filter is about bandwidth, not
     *   about access.
     */
    fun changes(table: String, circleId: String): Flow<JsonObject>

    // ============================================
    // STORAGE
    // ============================================

    /**
     * Uploads to a private bucket.
     *
     * Private is the only kind this method does, and that is on purpose. The
     * things the app uploads are fall-evidence audio and photographs of where
     * somebody collapsed; a public bucket URL is guessable, permanent and
     * un-revokable. Reading one back goes through [signedUrl], which expires.
     */
    suspend fun uploadPrivate(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String
    ): CloudResult<Unit>

    /**
     * A time-limited URL for one private object.
     *
     * @param expiresSec keep it short. This URL is bearer authority over the
     *   object for its whole life, and it will end up in a notification, a
     *   share sheet or a WhatsApp message.
     */
    suspend fun signedUrl(bucket: String, path: String, expiresSec: Long): CloudResult<String>
}
