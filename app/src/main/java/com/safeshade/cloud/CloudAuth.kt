package com.safeshade.cloud

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * The account surface, as the UI sees it.
 *
 * ### Why this exists when it adds no behaviour
 *
 * It is a facade over [CloudClient], and its whole job is to be the thing a
 * sign-in screen depends on so that the sign-in screen does not depend on
 * `upsert`, `select`, `uploadPrivate` and `signedUrl` as well. A screen that
 * takes a `CloudClient` can, by construction, write to any table; a screen that
 * takes a `CloudAuth` cannot. That is worth one thin class.
 *
 * It is also what keeps [CloudClient] small enough to fake. Every method added
 * to the client interface has to be written three times — real, fake, and in
 * whatever test double comes next — so pushing anything that is really *policy*
 * up here keeps that cost down.
 *
 * ### It is not a ViewModel
 *
 * No state of its own beyond what the client already publishes, no scope, no
 * side effects. The app has exactly one ViewModel (`ui/vm/SafeShadeViewModel.kt`)
 * and this is not a second one.
 */
class CloudAuth(private val client: CloudClient) {

    /** Who is signed in. See [CloudSession] for the four states. */
    val session: StateFlow<CloudSession> get() = client.session

    /** True when there is no project configured on this build. */
    val isDisabled: Boolean get() = session.value is CloudSession.Disabled

    /**
     * Emails a six-digit code.
     *
     * The email is trimmed and lower-cased here rather than at the call site,
     * because a guardian typing on a phone keyboard produces a leading space
     * often enough that "no account matches" would otherwise be the app's fault.
     * Supabase treats addresses case-insensitively, but the *app* compares them
     * in places, and two spellings of one address is a bug waiting to be filed.
     */
    suspend fun requestEmailOtp(email: String): CloudResult<Unit> =
        client.requestEmailOtp(email.normalizeEmail())

    suspend fun verifyEmailOtp(email: String, code: String): CloudResult<Unit> =
        client.verifyEmailOtp(email.normalizeEmail(), code.filter { it.isDigit() })

    suspend fun signInWithPassword(email: String, password: String): CloudResult<Unit> =
        client.signInWithPassword(email.normalizeEmail(), password)

    suspend fun signUpWithPassword(email: String, password: String): CloudResult<Unit> =
        client.signUpWithPassword(email.normalizeEmail(), password)

    suspend fun signInWithGoogle(idToken: String, nonce: String? = null): CloudResult<Unit> =
        client.signInWithIdToken(IdProvider.GOOGLE, idToken, nonce)

    suspend fun signInWithApple(idToken: String, nonce: String? = null): CloudResult<Unit> =
        client.signInWithIdToken(IdProvider.APPLE, idToken, nonce)

    suspend fun signOut(): CloudResult<Unit> = client.signOut()

    /**
     * Deletes the account.
     *
     * Nothing in this class asks for confirmation and nothing here should: a
     * destructive action's confirmation belongs where the user can see what
     * they are about to lose, not buried in a helper.
     */
    suspend fun deleteAccount(): CloudResult<Unit> = client.deleteAccount()

    /** For `MainActivity` to call in Phase 2. See [CloudClient.handleDeepLink]. */
    fun handleDeepLink(intent: Intent) = client.handleDeepLink(intent)
}

private fun String.normalizeEmail(): String = trim().lowercase()
