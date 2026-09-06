package com.safeshade.cloud

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * What this suite is actually protecting.
 *
 * Not "does the mapping compile" — the compiler covers that. Two things that
 * genuinely break silently:
 *
 *  1. **A raw exception message reaching a user.** Someone adds a branch and
 *     reaches for `t.message` because it is right there and it is 11pm. The
 *     result is "new row violates row-level security policy for table
 *     \"zones\"" rendered on a guardian's Circle tab. [reasonsAreHuman] fails
 *     on any reason containing library or stack-trace shrapnel.
 *  2. **A wrong `retryable`.** It is one boolean, it is invisible in the UI,
 *     and getting it wrong either burns a battery re-sending a row the server
 *     will reject forever, or abandons a write the moment the lift doors close.
 *     Every branch is asserted individually.
 *
 * ### Why this tests [failureFor] and not the exception types
 *
 * `RestException` and `AuthRestException` both take a Ktor `HttpResponse` in
 * their constructors, and a JVM unit test cannot produce one without standing
 * up an HTTP client. Testing "the mapping" through them would mean testing a
 * mock of them, which tests nothing. So `CloudErrors.kt` is split: the whole
 * decision table is the pure [failureFor], exercised here with plain values,
 * and only the "which kind of throwable is this" step goes through the types —
 * which is checked below with JDK exceptions that a test *can* construct.
 */
class CloudErrorsTest {

    /** Anything that would betray a library, a class name or a stack frame. */
    private val leaks = listOf(
        "Exception", "exception", "\tat ", "java.", "kotlin.", "io.ktor",
        "io.github", "supabase", "postgrest", "HTTP", "null", "row-level"
    )

    private fun assertHuman(f: CloudResult.Failed) {
        assertTrue("reason is blank", f.reason.isNotBlank())
        leaks.forEach { needle ->
            assertFalse(
                "reason leaks '$needle': ${f.reason}",
                f.reason.contains(needle)
            )
        }
        // A sentence, not a fragment. Every string in CloudErrors ends in a
        // full stop, and the UI puts them straight into a snackbar.
        assertTrue("reason is not a sentence: ${f.reason}", f.reason.trimEnd().endsWith("."))
    }

    // ============================================
    // Transport faults
    // ============================================

    @Test
    fun `offline is retryable and says so plainly`() {
        val f = failureFor(CloudFault.OFFLINE)
        assertHuman(f)
        assertTrue(f.retryable)
        assertTrue(f.reason.contains("No internet"))
    }

    @Test
    fun `timeout is retryable`() {
        val f = failureFor(CloudFault.TIMEOUT)
        assertHuman(f)
        assertTrue(f.retryable)
    }

    @Test
    fun `io is retryable`() {
        val f = failureFor(CloudFault.IO)
        assertHuman(f)
        assertTrue(f.retryable)
    }

    @Test
    fun `unknown is not retryable`() {
        // Deliberate: an unrecognised failure retried five times is a battery
        // drain with no diagnosis attached.
        val f = failureFor(CloudFault.UNKNOWN)
        assertHuman(f)
        assertFalse(f.retryable)
    }

    // ============================================
    // Auth codes
    // ============================================

    @Test
    fun `every mapped auth code is human and correctly retryable`() {
        val expectations = mapOf(
            "invalid_credentials" to false,
            "otp_expired" to false,
            "email_not_confirmed" to false,
            "user_already_exists" to false,
            "email_exists" to false,
            "weak_password" to false,
            "validation_failed" to false,
            // The one retryable auth code: the identical request succeeds once
            // the rate-limit window rolls, and the outbox backoff is what makes
            // waiting safe.
            "over_email_send_rate_limit" to true
        )
        expectations.forEach { (code, retryable) ->
            val f = failureFor(CloudFault.AUTH, status = 400, authCode = code)
            assertHuman(f)
            assertEquals("retryable wrong for $code", retryable, f.retryable)
        }
    }

    @Test
    fun `an unknown auth code falls back to the status`() {
        // A code this build has never heard of must not produce an empty or
        // generic-but-wrong answer; it should say what the status says.
        val f = failureFor(CloudFault.AUTH, status = 429, authCode = "some_future_code")
        assertHuman(f)
        assertTrue("a 429 is always worth retrying", f.retryable)
    }

    @Test
    fun `invalid credentials does not blame the network`() {
        val f = failureFor(CloudFault.AUTH, status = 400, authCode = "invalid_credentials")
        assertFalse(f.retryable)
        assertFalse(f.reason.contains("connection"))
        assertFalse(f.reason.contains("retried"))
    }

    // ============================================
    // HTTP statuses
    // ============================================

    @Test
    fun `statuses map to the right retryability`() {
        val expectations = mapOf(
            401 to false,
            403 to false,
            404 to false,
            409 to false,
            413 to false,
            429 to true,
            500 to true,
            502 to true,
            503 to true,
            400 to false,
            418 to false
        )
        expectations.forEach { (status, retryable) ->
            val f = failureFor(CloudFault.REST, status = status)
            assertHuman(f)
            assertEquals("retryable wrong for $status", retryable, f.retryable)
        }
    }

    @Test
    fun `a 403 never names a table or a policy`() {
        // The literal message Postgres produces for an RLS refusal names the
        // table. Handing that to a user leaks schema and explains nothing.
        val f = failureFor(CloudFault.REST, status = 403)
        assertFalse(f.reason.contains("policy"))
        assertFalse(f.reason.contains("zones"))
        assertTrue(f.reason.contains("permission"))
    }

    @Test
    fun `a missing status is treated as unreachable and retried`() {
        val f = failureFor(CloudFault.REST, status = null)
        assertHuman(f)
        assertTrue(f.retryable)
    }

    // ============================================
    // Throwable classification
    // ============================================

    @Test
    fun `an offline cause is found through a wrapper`() {
        // supabase-kt wraps transport failures in its own IOException subclass,
        // so a phone in aeroplane mode throws something whose *cause* is
        // UnknownHostException. Matching only the top-level type would report
        // "the connection dropped part-way" instead of "no internet".
        val wrapped = IOException("request failed", UnknownHostException("api.supabase.co"))
        val f = wrapped.toCloudFailure()
        assertHuman(f)
        assertTrue(f.retryable)
        assertTrue(f.reason.contains("No internet"))
    }

    @Test
    fun `a socket timeout is a timeout, not a generic io error`() {
        val f = IOException("boom", SocketTimeoutException("read timed out")).toCloudFailure()
        assertHuman(f)
        assertTrue(f.retryable)
        assertTrue(f.reason.contains("in time"))
    }

    @Test
    fun `a bare IOException is retryable`() {
        val f = IOException("stream closed").toCloudFailure()
        assertHuman(f)
        assertTrue(f.retryable)
    }

    @Test
    fun `a programming error is not retryable and leaks nothing`() {
        val f = IllegalStateException("someRepository was null at line 42").toCloudFailure()
        assertHuman(f)
        assertFalse(f.retryable)
        assertFalse(f.reason.contains("42"))
        assertFalse(f.reason.contains("Repository"))
    }

    @Test
    fun `a cyclic cause chain terminates`() {
        // A badly-behaved library can produce a self-referential cause. Without
        // the hop guard in rootFault this hangs the test runner rather than
        // failing it, which is the worst kind of bug to diagnose.
        val a = RuntimeException("a")
        val b = RuntimeException("b", a)
        a.initCause(b)
        val f = a.toCloudFailure()
        assertHuman(f)
    }
}
