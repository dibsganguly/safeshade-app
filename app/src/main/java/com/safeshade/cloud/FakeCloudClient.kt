package com.safeshade.cloud

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

/**
 * A [CloudClient] that keeps everything in a map.
 *
 * ### Why this lives in `main` and not in `test`
 *
 * Because three of its four jobs are production jobs:
 *
 *  1. **It is the client when no project is configured.** `BuildConfig`
 *     `SUPABASE_URL` is blank on every clone of this repo until somebody works
 *     through `supabase/README.md`, and the app must still build, run, and
 *     behave correctly offline. `CloudContainer` installs
 *     `FakeCloudClient(disabled = true)` in that case, so every cloud call
 *     returns [CloudResult.Disabled] — which the UI renders as *nothing*, not
 *     as an error. See [CloudResult.Disabled].
 *  2. **It is what `KitGallery.kt` draws against.** The gallery renders the
 *     whole kit live in both themes and is the first place a new state gets
 *     checked; a sync badge or an account row that cannot be shown there will
 *     not get looked at.
 *  3. **It makes failure reachable by hand.** [failNext] forces the very next
 *     call to fail with a chosen reason, which is the only practical way to
 *     look at the failure copy on a real device. The one genuine defect of the
 *     previous pass was a confirmation shown for a send that had failed, found
 *     only because somebody finally made the failure happen.
 *
 * Its fourth job is being a unit-test double, which it is also good at.
 *
 * ### Deterministic
 *
 * No random delays, no simulated flakiness. A fake that fails one call in ten
 * makes every test that uses it flaky, and a test suite people learn to re-run
 * is a test suite that stops being read. Ids are minted with `randomUUID`
 * because a row needs a unique key, but nothing about the *behaviour* varies.
 */
class FakeCloudClient(
    /** When true, every method returns [CloudResult.Disabled] and does nothing. */
    val disabled: Boolean = false,
    initialSession: CloudSession = if (disabled) CloudSession.Disabled else CloudSession.Guest
) : CloudClient {

    private val _session = MutableStateFlow(initialSession)
    override val session: StateFlow<CloudSession> = _session.asStateFlow()

    /**
     * Set to force exactly one failure.
     *
     * Consumed by the next call, whichever it is, and cleared. The failure it
     * produces is retryable, matching the common real case (a network blip)
     * rather than the rare one.
     */
    var failNext: String? = null

    /** Table name to row id to row body. Readable by tests. */
    val tables: MutableMap<String, MutableMap<String, JsonObject>> = mutableMapOf()

    /** Bucket to path to bytes. */
    val storage: MutableMap<String, MutableMap<String, ByteArray>> = mutableMapOf()

    /** Every edge-function call made, in order, for assertions. */
    val invocations: MutableList<Pair<String, JsonObject>> = mutableListOf()

    /** The last OTP "emailed", so a test can verify it without a mailbox. */
    var lastOtpCode: String? = null
        private set

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    /**
     * The one place [disabled] and [failNext] are checked.
     *
     * Every method funnels through here so a new method cannot forget the
     * contract - which is exactly how a fake drifts from the thing it stands in
     * for and starts passing tests the real client would fail.
     */
    private inline fun <T> guarded(block: () -> T): CloudResult<T> {
        if (disabled) return CloudResult.Disabled
        failNext?.let { reason ->
            failNext = null
            return CloudResult.Failed(reason, retryable = true)
        }
        return CloudResult.Ok(block())
    }

    // ============================================
    // AUTH
    // ============================================

    override suspend fun requestEmailOtp(email: String): CloudResult<Unit> = guarded {
        // Fixed, not random: a test asserting on the code should not have to
        // read it back, and a person poking at the UI should not have to guess.
        lastOtpCode = "123456"
    }

    override suspend fun verifyEmailOtp(email: String, code: String): CloudResult<Unit> {
        if (disabled) return CloudResult.Disabled
        failNext?.let { reason ->
            failNext = null
            return CloudResult.Failed(reason, retryable = true)
        }
        if (code != (lastOtpCode ?: "123456")) {
            // Deliberately the same sentence CloudErrors gives for a real
            // expired or wrong code, so the UI is exercised against the copy it
            // will actually show.
            return CloudResult.Failed("That code has expired. Ask for a new one.", retryable = false)
        }
        signIn(email)
        return CloudResult.Ok(Unit)
    }

    override suspend fun signInWithPassword(email: String, password: String): CloudResult<Unit> {
        if (disabled) return CloudResult.Disabled
        failNext?.let { reason ->
            failNext = null
            return CloudResult.Failed(reason, retryable = true)
        }
        if (password.length < 8) {
            return CloudResult.Failed(
                "That email and password do not match an account.",
                retryable = false
            )
        }
        signIn(email)
        return CloudResult.Ok(Unit)
    }

    override suspend fun signUpWithPassword(email: String, password: String): CloudResult<Unit> =
        signInWithPassword(email, password)

    override suspend fun signInWithIdToken(
        provider: IdProvider,
        idToken: String,
        nonce: String?
    ): CloudResult<Unit> = guarded {
        signIn("$provider.user@example.com".lowercase())
    }

    override suspend fun signOut(): CloudResult<Unit> = guarded {
        _session.value = CloudSession.Guest
    }

    override suspend fun deleteAccount(): CloudResult<Unit> = guarded {
        tables.clear()
        storage.clear()
        _session.value = CloudSession.Guest
    }

    override fun handleDeepLink(intent: Intent) = Unit

    private fun signIn(email: String) {
        _session.value = CloudSession.SignedIn(
            userId = UUID.nameUUIDFromBytes(email.toByteArray()).toString(),
            email = email
        )
    }

    // ============================================
    // TABLES
    // ============================================

    override suspend fun <T> upsert(
        table: String,
        rows: List<T>,
        serializer: KSerializer<T>
    ): CloudResult<Unit> = guarded {
        val bucket = tables.getOrPut(table) { mutableMapOf() }
        rows.forEach { row ->
            val encoded = json.encodeToJsonElement(serializer, row) as? JsonObject ?: return@forEach
            // Stamp `updated_at` the way the server's trigger does, so a pull
            // in a test sees the same field the real one would.
            val stamped = buildJsonObject {
                encoded.forEach { (k, v) -> put(k, v) }
                put("updated_at", Instant.now().toString())
            }
            val id = (encoded["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                ?: UUID.randomUUID().toString()
            bucket[id] = stamped
        }
    }

    override suspend fun select(
        table: String,
        circleId: String,
        since: Instant?
    ): CloudResult<List<JsonObject>> = guarded {
        tables[table].orEmpty().values.filter { row ->
            val rowCircle = (row["circle_id"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            // A row with no circle_id is visible to everyone here; the real
            // server would have rejected the insert, and the fake does not
            // pretend to enforce row-level security.
            (rowCircle == null || rowCircle == circleId) && newerThan(row, since)
        }.toList()
    }

    private fun newerThan(row: JsonObject, since: Instant?): Boolean {
        if (since == null) return true
        val raw = (row["updated_at"] as? kotlinx.serialization.json.JsonPrimitive)?.content
            ?: return true
        val at = runCatching { Instant.parse(raw) }.getOrNull() ?: return true
        return at.isAfter(since)
    }

    override suspend fun invoke(function: String, body: JsonObject): CloudResult<JsonObject> =
        guarded {
            invocations += function to body
            buildJsonObject { put("ok", true) }
        }

    // ============================================
    // STORAGE
    // ============================================

    override suspend fun uploadPrivate(
        bucket: String,
        path: String,
        bytes: ByteArray,
        contentType: String
    ): CloudResult<Unit> = guarded {
        storage.getOrPut(bucket) { mutableMapOf() }[path] = bytes
    }

    override suspend fun signedUrl(
        bucket: String,
        path: String,
        expiresSec: Long
    ): CloudResult<String> = guarded {
        // Not a reachable URL, and shaped so that nobody mistakes it for one.
        "https://fake.local/$bucket/$path?expires=$expiresSec"
    }
}
