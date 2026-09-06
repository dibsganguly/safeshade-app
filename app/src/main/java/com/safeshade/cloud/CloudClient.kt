package com.safeshade.cloud

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.KSerializer
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
     * Calls an edge function and returns its JSON body.
     *
     * A non-2xx response is a [CloudResult.Failed], not an [CloudResult.Ok]
     * carrying an error object — otherwise `send-alert-email` returning
     * "nothing was delivered" would read to a caller as a successful send,
     * which is precisely the class of lie this app has already shipped once.
     */
    suspend fun invoke(function: String, body: JsonObject): CloudResult<JsonObject>

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
