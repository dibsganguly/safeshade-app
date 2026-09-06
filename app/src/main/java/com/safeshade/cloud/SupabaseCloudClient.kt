package com.safeshade.cloud

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Apple
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * The real client. Everything above it talks to [CloudClient], never to this.
 *
 * ### The shape of every method
 *
 * ```
 * runCatching { ...one supabase call... }
 *     .fold({ CloudResult.Ok(it) }, { it.toCloudFailure() })
 * ```
 *
 * There is no other shape in this file, and that is the point. supabase-kt
 * throws on every failure — a rejected row, an expired session, a phone in a
 * tunnel — and a single missed `catch` anywhere here would put an exception
 * into the application scope, where nothing is watching and nothing can tell
 * the user. [runOrFail] is the one helper that enforces it.
 *
 * ### The session flow
 *
 * Derived from `auth.sessionStatus` rather than kept alongside it. supabase-kt
 * refreshes tokens on its own timer and can drop a session at any moment; a
 * second copy of "who is signed in" would go stale exactly when it matters, on
 * the refresh failure that signs somebody out mid-emergency. Mapping the one
 * source is the only way to be sure the app and the library agree.
 *
 * `SessionStatus.RefreshFailure` maps to [CloudSession.Guest], not to an error
 * state: the user is, factually, not authenticated any more. Why that happened
 * belongs to whichever call next tries something and gets a 401 back with a
 * sentence attached.
 *
 * ### Deep links
 *
 * `handleDeeplinks` is **not** called here. It needs an `Intent`, which means
 * an Activity, and `MainActivity` is out of scope for this phase. [handleDeepLink]
 * exposes it for Phase 2 to call from `onCreate` and `onNewIntent`. Until it is
 * wired, the OTP and password paths work and the OAuth-redirect path does not
 * complete — which is why the OTP path is the one the UI should offer first.
 */
class SupabaseCloudClient(
    supabaseUrl: String,
    supabaseKey: String,
    scope: CoroutineScope
) : CloudClient {

    /**
     * The JSON contract with the server, in one place.
     *
     *  - `ignoreUnknownKeys` — a migration that adds a column must not break
     *    decode on every phone that has not updated. Without this, one ALTER
     *    TABLE bricks the installed base.
     *  - `explicitNulls = false` — a null field is *omitted* from an outgoing
     *    body rather than sent as JSON `null`. On an upsert that is the
     *    difference between "I have nothing to say about this column" and
     *    "erase this column", and a phone that only knows half a row must not
     *    wipe the other half on its first sync.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val client: SupabaseClient = createSupabaseClient(supabaseUrl, supabaseKey) {
        defaultSerializer = KotlinXSerializer(json)

        install(Auth) {
            // Must match the BROWSABLE intent-filter on MainActivity exactly:
            // safeshade://login-callback. A mismatch does not error - the
            // browser simply never comes back, which reads as a hung sign-in.
            scheme = DEEP_LINK_SCHEME
            host = DEEP_LINK_HOST

            // PKCE, not implicit. The token never appears in a URL fragment
            // that another app registered for the same scheme could read.
            flowType = FlowType.PKCE

            alwaysAutoRefresh = true
            autoLoadFromStorage = true
        }
        install(Postgrest)
        install(Storage)
        install(Realtime)
        install(Functions)
    }

    override val session: StateFlow<CloudSession> = client.auth.sessionStatus
        .map { status ->
            when (status) {
                is SessionStatus.Authenticated -> CloudSession.SignedIn(
                    userId = status.session.user?.id.orEmpty(),
                    email = status.session.user?.email
                )

                is SessionStatus.Initializing -> CloudSession.Loading

                // A refresh failure is not a third state. The user is not
                // authenticated; see the class KDoc.
                else -> CloudSession.Guest
            }
        }
        .stateIn(scope, SharingStarted.Eagerly, CloudSession.Loading)

    /**
     * The only error boundary in this file.
     *
     * `runCatching` catches `Throwable`, which includes `CancellationException`
     * — and swallowing that turns a cancelled scope into a coroutine that keeps
     * running and reports a bogus failure. So it is rethrown explicitly.
     */
    private inline fun <T> runOrFail(block: () -> T): CloudResult<T> = try {
        CloudResult.Ok(block())
    } catch (c: kotlinx.coroutines.CancellationException) {
        throw c
    } catch (t: Throwable) {
        t.toCloudFailure()
    }

    // ============================================
    // AUTH
    // ============================================

    override suspend fun requestEmailOtp(email: String): CloudResult<Unit> = runOrFail {
        client.auth.signInWith(OTP) {
            this.email = email
            // True: an unknown email creates the account. The product has one
            // sign-in flow, not a separate sign-up, because a guardian being
            // handed a phone in a hospital corridor should not have to work out
            // which of two buttons they are.
            createUser = true
        }
    }

    override suspend fun verifyEmailOtp(email: String, code: String): CloudResult<Unit> = runOrFail {
        client.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = code
        )
    }

    override suspend fun signInWithPassword(
        email: String,
        password: String
    ): CloudResult<Unit> = runOrFail {
        client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun signUpWithPassword(
        email: String,
        password: String
    ): CloudResult<Unit> = runOrFail {
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        Unit
    }

    override suspend fun signInWithIdToken(
        provider: IdProvider,
        idToken: String,
        nonce: String?
    ): CloudResult<Unit> = runOrFail {
        client.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = when (provider) {
                IdProvider.GOOGLE -> Google
                IdProvider.APPLE -> Apple
            }
            // Only set when there was one. Passing null and passing nothing are
            // the same to the library, but passing a nonce that was not used to
            // mint the token fails the exchange with a message about the token
            // being invalid, which sends people looking in the wrong place.
            if (nonce != null) this.nonce = nonce
        }
    }

    override suspend fun signOut(): CloudResult<Unit> = runOrFail {
        client.auth.signOut()
    }

    /**
     * Calls the `delete_account()` SQL function.
     *
     * Deleting an `auth.users` row is a service-role operation and the service
     * role must never be in an APK. The migration therefore defines
     * `delete_account()` as `security definer`, and it deletes `auth.uid()` and
     * only `auth.uid()` — so the worst a stolen session can do is delete its own
     * account, which it could do by asking anyway.
     */
    override suspend fun deleteAccount(): CloudResult<Unit> = runOrFail {
        client.postgrest.rpc("delete_account")
        Unit
    }

    override fun handleDeepLink(intent: Intent) {
        client.handleDeeplinks(intent)
    }

    // ============================================
    // TABLES
    // ============================================

    override suspend fun <T> upsert(
        table: String,
        rows: List<T>,
        serializer: KSerializer<T>
    ): CloudResult<Unit> = runOrFail {
        if (rows.isEmpty()) return@runOrFail
        // Encoded to a JsonArray and sent through the JsonArray overload rather
        // than the reified one, because the caller (the outbox) knows the row
        // type only as a serializer. One request for the whole batch.
        val array = JsonArray(rows.map { json.encodeToJsonElement(serializer, it) })
        client.from(table).upsert(array)
        Unit
    }

    override suspend fun select(
        table: String,
        circleId: String,
        since: Instant?
    ): CloudResult<List<JsonObject>> = runOrFail {
        client.from(table).select(Columns.ALL) {
            filter {
                eq("circle_id", circleId)
                // Strictly greater than, so the row that set the cursor is not
                // fetched again on every single pull forever.
                if (since != null) gt("updated_at", since.toString())
            }
        }.decodeList<JsonObject>()
    }

    override suspend fun invoke(function: String, body: JsonObject): CloudResult<JsonObject> =
        runOrFail {
            val response = client.functions.invoke(function = function, body = body)
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) {
                // A function that answers "nothing was delivered" with a 4xx
                // must not reach the caller as a success carrying an error
                // object. Throwing here routes it through toCloudFailure with
                // the real status.
                throw io.github.jan.supabase.exceptions.RestException(
                    "edge_function_failed",
                    "status ${response.status.value}",
                    response
                )
            }
            runCatching { json.parseToJsonElement(text) as JsonObject }
                .getOrDefault(JsonObject(emptyMap()))
        }

    override suspend fun rpc(
        function: String,
        args: JsonObject
    ): CloudResult<JsonElement> = runOrFail {
        // The (name, JsonObject) overload, not the builder one: the arguments
        // are already a JsonObject by the time they reach here, and the builder
        // overload would need them re-encoded through a serializer for nothing.
        parseRpcData(client.postgrest.rpc(function, args).data)
    }

    // ============================================
    // REALTIME
    // ============================================

    /**
     * See [CloudClient.changes] for the contract. Three decisions live here.
     *
     * **The flow is built and collected before `subscribe()`.** `postgresChangeFlow`
     * registers a binding on the channel, and the bindings are sent to the
     * server inside the join message. Subscribing first produces a channel that
     * reports SUBSCRIBED and then delivers nothing, forever, with no error
     * anywhere — the single most expensive way to get this wrong.
     *
     * **The topic carries a UUID.** `Realtime.subscriptions` is keyed by topic,
     * so two collectors of `changes("alerts", c)` sharing one topic would share
     * one channel, and whichever finished first would tear the other one down.
     * The Circle tab and a background pull can both be live at once, so they get
     * a channel each.
     *
     * **Teardown is `NonCancellable`.** `removeChannel` suspends, and the usual
     * reason this flow ends is that its scope was cancelled — in which case a
     * plain suspend call in `finally` would itself be cancelled immediately and
     * leave the channel joined on the server.
     */
    override fun changes(table: String, circleId: String): Flow<JsonObject> = channelFlow {
        val channel = client.channel("safeshade:$table:$circleId:${UUID.randomUUID()}")

        // PostgresAction (the interface) as the reified type means event "*":
        // every change on the table, filtered below to the two this app has a
        // use for.
        val actions = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            this.table = table
            filter("circle_id", FilterOperator.EQ, circleId)
        }

        val pump = launch {
            actions.collect { action ->
                val row = when (action) {
                    is PostgresAction.Insert -> action.record
                    is PostgresAction.Update -> action.record
                    // Delete carries only the old key, and this app never hard
                    // deletes; Select only occurs under selectAsFlow, which is
                    // not used here.
                    else -> null
                }
                if (row != null) send(row)
            }
        }

        try {
            // Inside the try, not before it. `client.channel(...)` has already
            // registered this topic in `Realtime.subscriptions`, so a subscribe
            // that throws — no connection, auth refused — would otherwise skip
            // the finally and leave the channel joined with nothing collecting
            // it, which is the exact leak the teardown below exists to prevent.
            channel.subscribe(blockUntilSubscribed = true)
            awaitCancellation()
        } finally {
            pump.cancel()
            withContext(NonCancellable) {
                runCatching { client.realtime.removeChannel(channel) }
            }
        }
    }.catch {
        // Nothing thrown reaches the collector; the flow simply ends. See the
        // KDoc on CloudClient.changes for why re-subscription is SyncEngine's
        // job and not this flow's.
    }

    // ============================================
    // STORAGE
    // ============================================

    override suspend fun uploadPrivate(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String
    ): CloudResult<Unit> = runOrFail {
        client.storage[bucket].upload(path, bytes) {
            // Overwrite rather than fail. The one caller is evidence capture
            // retrying an upload whose response was lost, and a duplicate-key
            // error there would strand the recording on the phone.
            upsert = true
            this.contentType = runCatching { ContentType.parse(contentType) }
                .getOrDefault(ContentType.Application.OctetStream)
        }
        Unit
    }

    override suspend fun signedUrl(
        bucket: String,
        path: String,
        expiresSec: Long
    ): CloudResult<String> = runOrFail {
        client.storage[bucket].createSignedUrl(path, expiresSec.seconds)
    }

    companion object {
        /** Must match the manifest's BROWSABLE intent-filter on MainActivity. */
        const val DEEP_LINK_SCHEME = "safeshade"
        const val DEEP_LINK_HOST = "login-callback"
    }
}
