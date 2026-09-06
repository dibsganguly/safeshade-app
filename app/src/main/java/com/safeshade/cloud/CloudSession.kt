package com.safeshade.cloud

/**
 * Who, if anyone, is signed in — and whether that question is even meaningful
 * on this build.
 *
 * Four states rather than a nullable user, because the UI has to tell three
 * different stories and a `User?` can only tell two:
 *
 *  - **[Disabled]** — this build has no Supabase project configured, so there
 *    is no account system at all. The Circle tab shows local data and no
 *    sign-in affordance. This is not "signed out"; there is nothing to sign in
 *    to. See `CloudContainer`.
 *  - **[Loading]** — the stored session is being read back or refreshed. It is
 *    the first state after process start on a configured build, and it exists
 *    so a signed-in guardian never sees a "Sign in" button flash on cold start
 *    before their session resolves. The same trap `AppState.Loading` exists to
 *    avoid elsewhere in this app: a null that means "not yet" rendered as a
 *    null that means "nothing".
 *  - **[Guest]** — cloud is configured and nobody is signed in.
 *  - **[SignedIn]** — a real account.
 *
 * There is deliberately no `Error` state. A failed refresh lands the user in
 * [Guest]; the failure itself is reported through [CloudResult] at the call
 * site that asked for it, so a transient network blip cannot paint the whole
 * account surface red.
 */
sealed interface CloudSession {

    /** No Supabase project is configured on this build. */
    data object Disabled : CloudSession

    /** Session restore or refresh in flight. Render the signed-in shape, greyed. */
    data object Loading : CloudSession

    /** Cloud is available; no account is signed in. */
    data object Guest : CloudSession

    /**
     * A signed-in account.
     *
     * @param userId the Supabase `auth.users.id`. This is the value every
     *   row-level-security policy compares against, so it is the app's real
     *   identity key — never the email, which a user can change.
     * @param email null when the account was created through a provider that
     *   does not return one (Apple's "hide my email" being the case that will
     *   actually happen). Copy must never assume an email exists.
     */
    data class SignedIn(val userId: String, val email: String?) : CloudSession
}

/** True only when there is a real account to attribute writes to. */
val CloudSession.isSignedIn: Boolean get() = this is CloudSession.SignedIn

/** The signed-in user id, or null in every other state. */
val CloudSession.userIdOrNull: String?
    get() = (this as? CloudSession.SignedIn)?.userId

/** Which identity providers the app can hand an ID token to. */
enum class IdProvider {
    /** Google, via Credential Manager. Needs `GOOGLE_WEB_CLIENT_ID`. */
    GOOGLE,

    /** Apple. Reserved; no iOS/Apple flow is wired on Android yet. */
    APPLE
}
