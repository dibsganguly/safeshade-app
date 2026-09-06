package com.safeshade.cloud

/**
 * The result of every cloud call in this app. There is no other return shape.
 *
 * ### Why a result type rather than an exception
 *
 * This codebase already learned this lesson once, expensively. The quick-message
 * tick used to be set on tap and cleared 1400ms later regardless of what
 * happened, so with nothing paired and no SIM number stored — a fresh install —
 * every send failed and every send drew a confirmation. `MessagingRepository`
 * had been returning a user-facing `SendResult` the whole time and every layer
 * above it threw the value away. `DESIGN.md` now carries the rule that came out
 * of it: **don't confirm an action before its result is known.**
 *
 * A login, a row upsert, an email send and an evidence upload are all things
 * that fail quietly and often. Modelling failure as a value rather than a thrown
 * exception means a caller cannot accidentally succeed by forgetting a `catch`:
 * the compiler makes them look at [Failed] before they can reach [Ok.value].
 * This mirrors `SendResult` deliberately — one house style, learned once.
 *
 * ### [Failed.reason] is shown to a person, verbatim
 *
 * It is written for the wearer's guardian, not for a log. A raw exception
 * message ("HTTP 401 Unauthorized", "Failed to connect to
 * xyz.supabase.co/104.18.0.1:443") tells them nothing they can act on and leaks
 * infrastructure into the UI. Every mapping in `CloudErrors.kt` produces plain
 * English that names the situation and, where there is one, the next move.
 *
 * ### [Failed.retryable]
 *
 * Drives the outbox, not the copy. A retryable failure is one where the same
 * request, unchanged, could succeed later: no network, a timeout, a 5xx, a rate
 * limit. A wrong password or a rejected row is not retryable, and re-sending it
 * on a timer only burns battery and rate limit.
 */
sealed interface CloudResult<out T> {

    /** The call happened and returned [value]. */
    data class Ok<T>(val value: T) : CloudResult<T>

    /**
     * The call was attempted and did not succeed.
     *
     * @param reason human-readable, shown to the user as written. Never a
     *   stack trace, never an exception class name, never a raw HTTP status.
     * @param retryable true when re-sending the identical request later could
     *   plausibly succeed. See the class KDoc.
     */
    data class Failed(val reason: String, val retryable: Boolean) : CloudResult<Nothing>

    /**
     * No cloud is configured on this build, so the call was never attempted.
     *
     * Deliberately distinct from [Failed]. "SafeShade Cloud is not set up on
     * this phone" is not an error and must not be reported as one: the app is
     * fully functional offline, and drawing a red failure banner at a user who
     * never asked for cloud sync would be a lie about the state of their
     * device. Callers typically render this as nothing at all.
     */
    data object Disabled : CloudResult<Nothing>
}

/** The value on success, or null for both failure and disabled. */
fun <T> CloudResult<T>.valueOrNull(): T? = (this as? CloudResult.Ok)?.value

/** True only for [CloudResult.Ok]. */
val CloudResult<*>.isOk: Boolean get() = this is CloudResult.Ok

/**
 * The user-facing reason, or null when there is nothing to say.
 *
 * [CloudResult.Disabled] returns null on purpose — see its KDoc.
 */
fun CloudResult<*>.reasonOrNull(): String? = (this as? CloudResult.Failed)?.reason
