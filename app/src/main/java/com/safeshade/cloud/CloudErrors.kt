package com.safeshade.cloud

import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.exceptions.RestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Turns anything a cloud call can throw into a sentence a worried person can
 * read, plus the one bit the outbox needs.
 *
 * ### The rule this file exists to enforce
 *
 * **A raw exception message is never shown to a user.** Not because it is ugly
 * — because it is *misleading*. "HTTP 401" reads as a bug in the app; the truth
 * is usually "your session expired, sign in again". A `RestException` from a
 * row-level-security refusal says "new row violates row-level security policy
 * for table \"zones\"", which describes the database's opinion, not the user's
 * situation, and hands an attacker a table name for free.
 *
 * Everything below is therefore written as a sentence, in the app's voice, and
 * every one of them is safe to show on the Circle tab in front of a stranger.
 *
 * ### Why the classification is split in two
 *
 * [failureFor] is pure — three plain values in, a [CloudResult.Failed] out — and
 * it is what `CloudErrorsTest` exercises. It has to be, because supabase-kt's
 * `RestException` and `AuthRestException` both take a Ktor `HttpResponse` in
 * their constructors, and a unit test cannot conjure one of those without
 * standing up an HTTP client. Testing the mapping through the exception types
 * would mean not testing it at all.
 *
 * [toCloudFailure] is then the thin, untestable half: it decides *which* fault
 * a `Throwable` is and hands the decision to [failureFor].
 *
 * ### Walking the cause chain
 *
 * supabase-kt wraps transport failures in its own `HttpRequestException`, which
 * extends `IOException`. A phone in aeroplane mode therefore throws something
 * whose *cause* is `UnknownHostException` and whose own type says only "IO".
 * Matching on the top-level type alone would classify every offline call as a
 * generic IO error and tell the user "something went wrong" when the honest
 * answer — and the actionable one — is "you are offline". [rootFault] walks the
 * chain.
 */

/** The shape of a failure, independent of any library type. */
internal enum class CloudFault {
    /** A response came back with a status. */
    REST,

    /** A response came back from the auth endpoint with an error code. */
    AUTH,

    /** The request went out and nothing came back in time. */
    TIMEOUT,

    /** DNS or connect failed. Almost always: no network. */
    OFFLINE,

    /** Some other I/O failure mid-request. */
    IO,

    /** Anything else, including bugs in this app. */
    UNKNOWN
}

/**
 * The whole mapping, as a pure function.
 *
 * @param fault which kind of failure happened.
 * @param status the HTTP status, for [CloudFault.REST] and [CloudFault.AUTH].
 * @param authCode the Supabase auth error code (the wire string, e.g.
 *   `invalid_credentials`), for [CloudFault.AUTH]. Matched on the wire value
 *   rather than on the `AuthErrorCode` enum constant so that a code the current
 *   supabase-kt has no constant for still maps correctly.
 */
internal fun failureFor(
    fault: CloudFault,
    status: Int? = null,
    authCode: String? = null
): CloudResult.Failed = when (fault) {

    CloudFault.OFFLINE -> CloudResult.Failed(
        "No internet connection. This is saved on your phone and will sync when you are back online.",
        retryable = true
    )

    CloudFault.TIMEOUT -> CloudResult.Failed(
        "SafeShade Cloud did not answer in time. This will be retried.",
        retryable = true
    )

    CloudFault.IO -> CloudResult.Failed(
        "The connection dropped part-way. This will be retried.",
        retryable = true
    )

    CloudFault.AUTH -> authFailure(authCode, status)

    CloudFault.REST -> restFailure(status)

    CloudFault.UNKNOWN -> CloudResult.Failed(
        // Deliberately not retryable. An unrecognised failure repeated on a
        // timer is a battery drain with no diagnosis attached, and five silent
        // retries of a bug look exactly like a network problem to the user.
        "Something went wrong talking to SafeShade Cloud.",
        retryable = false
    )
}

/**
 * Auth error codes, mapped.
 *
 * The five named here are the ones a real user actually hits. Each one gets a
 * sentence that says what happened *and* what to do, because an auth failure is
 * the one failure the user can always fix themselves.
 */
private fun authFailure(authCode: String?, status: Int?): CloudResult.Failed = when (authCode) {

    "invalid_credentials" -> CloudResult.Failed(
        "That email and password do not match an account.",
        retryable = false
    )

    "otp_expired" -> CloudResult.Failed(
        "That code has expired. Ask for a new one.",
        retryable = false
    )

    "over_email_send_rate_limit" -> CloudResult.Failed(
        "Too many emails requested. Wait a minute and try again.",
        // Retryable in the outbox sense: the identical request will succeed
        // once the window rolls. The backoff is what makes this safe.
        retryable = true
    )

    "email_not_confirmed" -> CloudResult.Failed(
        "Check your email and confirm the address before signing in.",
        retryable = false
    )

    "user_already_exists", "email_exists" -> CloudResult.Failed(
        "An account already exists for that email. Sign in instead.",
        retryable = false
    )

    "weak_password" -> CloudResult.Failed(
        "That password is too easy to guess. Use at least eight characters.",
        retryable = false
    )

    "validation_failed" -> CloudResult.Failed(
        "Check the email address and try again.",
        retryable = false
    )

    // An auth code this build has never heard of. Fall back to the status,
    // which is at least honest about whether retrying could help.
    else -> restFailure(status)
}

/** HTTP statuses, mapped. */
private fun restFailure(status: Int?): CloudResult.Failed = when {

    status == null -> CloudResult.Failed(
        "SafeShade Cloud could not be reached.",
        retryable = true
    )

    status == 401 -> CloudResult.Failed(
        "Your session has expired. Sign in again.",
        retryable = false
    )

    status == 403 -> CloudResult.Failed(
        // Never "row-level security policy". This is what a 403 means to the
        // person holding the phone.
        "You do not have permission to do that in this Circle.",
        retryable = false
    )

    status == 404 -> CloudResult.Failed(
        "That is no longer on SafeShade Cloud.",
        retryable = false
    )

    status == 409 -> CloudResult.Failed(
        "Someone else changed this first. Open it again to see the current version.",
        retryable = false
    )

    status == 413 -> CloudResult.Failed(
        "That file is too large to upload.",
        retryable = false
    )

    status == 429 -> CloudResult.Failed(
        "SafeShade Cloud is busy. This will be retried shortly.",
        retryable = true
    )

    status in 500..599 -> CloudResult.Failed(
        "SafeShade Cloud is having trouble. This will be retried.",
        retryable = true
    )

    status in 400..499 -> CloudResult.Failed(
        "SafeShade Cloud rejected that request.",
        retryable = false
    )

    else -> CloudResult.Failed(
        "Something went wrong talking to SafeShade Cloud.",
        retryable = false
    )
}

/**
 * Classifies a throwable by walking its cause chain.
 *
 * Order matters: the auth check comes before the REST check because
 * `AuthRestException` **is a** `RestException`, and a `when` that tested the
 * supertype first would swallow every auth code in the file above.
 */
internal fun Throwable.rootFault(): Triple<CloudFault, Int?, String?> {
    var t: Throwable? = this
    // Guard against a cyclic cause chain, which a badly-behaved library can
    // produce and which would hang this loop forever.
    var hops = 0
    while (t != null && hops < 16) {
        when (t) {
            is AuthRestException ->
                return Triple(CloudFault.AUTH, t.statusCode, t.errorCode?.value)

            is RestException ->
                return Triple(CloudFault.REST, t.statusCode, null)

            is UnknownHostException ->
                return Triple(CloudFault.OFFLINE, null, null)

            is HttpRequestTimeoutException, is SocketTimeoutException ->
                return Triple(CloudFault.TIMEOUT, null, null)
        }
        t = t.cause
        hops++
    }

    // No specific cause found. An IOException anywhere in the chain still means
    // a transport problem, which is worth retrying; anything else is not.
    var u: Throwable? = this
    var h = 0
    while (u != null && h < 16) {
        if (u is IOException) return Triple(CloudFault.IO, null, null)
        u = u.cause
        h++
    }
    return Triple(CloudFault.UNKNOWN, null, null)
}

/**
 * The single entry point every call site uses:
 * `runCatching { ... }.fold(::ok) { it.toCloudFailure() }`.
 */
fun Throwable.toCloudFailure(): CloudResult.Failed {
    val (fault, status, code) = rootFault()
    return failureFor(fault, status, code)
}
