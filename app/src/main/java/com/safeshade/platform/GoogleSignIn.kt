package com.safeshade.platform

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Google sign-in via Credential Manager, for `SupabaseCloudClient.signInWithIdToken`.
 *
 * This is Credential Manager, not the old `GoogleSignInClient` — the latter is
 * deprecated and the former is what Play Services actually surfaces on a
 * current phone. The one subtlety worth writing down is the nonce, because
 * getting it backwards produces something that *looks* like it works (a token
 * comes back, sign-in appears to succeed in testing) and only fails the
 * security property it exists for:
 *
 * Supabase's `signInWithIdToken` is told the **raw** nonce and hashes it
 * itself to compare against the hash Google embedded in the ID token. So the
 * raw value must travel from here to the caller, and only its SHA-256 **hex
 * digest** goes into [GetGoogleIdOption] — never the reverse. Passing the raw
 * nonce to Google (so its hash never matches what Supabase computes) or the
 * hash to Supabase (so it never matches the raw value Supabase re-hashes) both
 * fail the same way: a token Supabase silently rejects as a replay risk,
 * which is a much worse debugging session than the two lines this file spends
 * getting it right.
 */
class GoogleSignInHelper(context: Context) {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Requests a Google ID token via the Credential Manager bottom sheet.
     *
     * [activityContext] must be an `Activity` context (or one that resolves to
     * one) — Credential Manager needs it to host the account picker UI; the
     * application context passed to the constructor cannot do that.
     */
    suspend fun requestIdToken(webClientId: String, activityContext: Context): GoogleSignInResult {
        val rawNonce = randomNonce()
        val hashedNonce = sha256Hex(rawNonce)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setNonce(hashedNonce)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val response = credentialManager.getCredential(activityContext, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val idTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Ok(idTokenCredential.idToken, rawNonce)
            } else {
                GoogleSignInResult.Failed("Google did not return a sign-in token")
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Failed("No Google account on this phone, or Google Play services is unavailable")
        } catch (e: GetCredentialProviderConfigurationException) {
            GoogleSignInResult.Failed("Google Play services is missing or out of date on this phone")
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failed(e.message ?: "Google sign-in failed")
        } catch (e: Exception) {
            GoogleSignInResult.Failed(e.message ?: "Google sign-in failed")
        }
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

/** Result of [GoogleSignInHelper.requestIdToken]. Nothing throws to the caller. */
sealed interface GoogleSignInResult {
    data class Ok(val idToken: String, val rawNonce: String) : GoogleSignInResult
    data class Failed(val reason: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

/** SHA-256 of [s] as lowercase hex — the form [GetGoogleIdOption.Builder.setNonce] wants. */
internal fun sha256Hex(s: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
