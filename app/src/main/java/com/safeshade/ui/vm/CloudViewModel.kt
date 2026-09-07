package com.safeshade.ui.vm

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.safeshade.BuildConfig
import com.safeshade.SafeShadeApplication
import com.safeshade.cloud.CircleInvite
import com.safeshade.cloud.CircleRole
import com.safeshade.cloud.CloudResult
import com.safeshade.cloud.CloudSession
import com.safeshade.cloud.CloudState
import com.safeshade.cloud.CloudTier
import com.safeshade.cloud.HeatCell
import com.safeshade.cloud.sync.SyncState
import com.safeshade.di.AppContainer
import com.safeshade.platform.GoogleSignInHelper
import com.safeshade.platform.GoogleSignInResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Everything the account and sign-in screens read and do.
 *
 * Kept apart from [SafeShadeViewModel] on purpose. That class is the phone's
 * relationship with the wearable and the person; this one is the phone's
 * relationship with SafeShade Cloud, and the two must be able to fail
 * independently. A build with no cloud project (blank `SUPABASE_URL`) still
 * constructs this, and every call on it returns [CloudResult.Disabled], which
 * the screens render as "saved on this phone only" rather than as an error.
 *
 * ### Nothing here claims an outcome before it has one
 *
 * Every action is a `suspend fun` returning the [CloudResult] the client gave
 * back, so the screen that called it is the one that decides what to say and
 * says it only once the answer exists. A "Code sent" line drawn on the tap
 * would be an animation, not a confirmation, and the failure case here
 * (a typo in the address, no network, Resend refusing an address) is the
 * common one.
 */
class CloudViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    private val cloud get() = container.cloud

    /** Who is signed in, or why nobody can be. */
    val session: StateFlow<CloudSession> get() = cloud.auth.session

    /** True when this build was pointed at a project at all. */
    val isConfigured: Boolean get() = cloud.isConfigured

    /** True when the Google button may be offered at all. */
    val googleAvailable: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

    /**
     * What the account page says about sync.
     *
     * Derived from the outbox rather than from an engine flag, because the
     * outbox is the thing that is actually on disk: a count of pending rows
     * and the newest error are facts, "syncing" is a moment.
     */
    val syncSummary: StateFlow<SyncSummary> =
        combine(cloud.outbox.entries, cloud.outbox.states, cloud.syncEngine.draining) { entries, states, draining ->
            val lastSynced = states.values
                .filterIsInstance<SyncState.Synced>()
                .maxOfOrNull { it.at }
            val failed = states.values.filterIsInstance<SyncState.Failed>().firstOrNull()
            SyncSummary(
                pending = entries.size,
                lastSyncedAt = lastSynced,
                // In flight, not merely queued. See SyncEngine.draining.
                syncing = draining,
                lastError = failed?.reason ?: entries.firstNotNullOfOrNull { it.lastError }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncSummary())

    /** Per-record cloud state by outbox key, for the dots on trip and message rows. */
    val outboxStates: StateFlow<Map<String, SyncState>> get() = cloud.outbox.states

    /** The Circle and the tier, as the sync layer knows them. */
    val cloudState: StateFlow<CloudState> get() = cloud.cloudState

    suspend fun invite(email: String, role: CircleRole = CircleRole.GUARDIAN): CloudResult<CircleInvite> =
        cloud.circleActions.invite(email, role)

    suspend fun acceptInvite(token: String): CloudResult<Unit> =
        cloud.circleActions.acceptInvite(token)

    suspend fun setShareAlertPlaces(on: Boolean) = cloud.circleActions.setShareAlertPlaces(on)

    suspend fun setDevTierOverride(tier: CloudTier?) =
        cloud.circleActions.setDevTierOverride(tier)

    suspend fun heatmapIn(minLat: Double, maxLat: Double, minLon: Double, maxLon: Double): CloudResult<List<HeatCell>> =
        cloud.circleActions.heatmapIn(minLat, maxLat, minLon, maxLon)

    suspend fun requestEmailCode(email: String): CloudResult<Unit> =
        cloud.auth.requestEmailOtp(email)

    suspend fun verifyEmailCode(email: String, code: String): CloudResult<Unit> =
        cloud.auth.verifyEmailOtp(email, code)

    suspend fun signInWithPassword(email: String, password: String): CloudResult<Unit> =
        cloud.auth.signInWithPassword(email, password)

    suspend fun signUpWithPassword(email: String, password: String): CloudResult<Unit> =
        cloud.auth.signUpWithPassword(email, password)

    /**
     * Google, end to end: Credential Manager asks for an ID token whose
     * audience is the Web client ID, and Supabase checks that audience and
     * the nonce. The helper hashes the nonce for Google and hands back the raw
     * one, which is what Supabase wants; passing the hashed one here is the
     * classic "invalid token" failure that sends people chasing client IDs.
     */
    suspend fun signInWithGoogle(activityContext: Context): CloudResult<Unit> {
        if (!googleAvailable) {
            return CloudResult.Failed("Google sign-in is not set up in this build", retryable = false)
        }
        return when (val got = GoogleSignInHelper(getApplication())
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID, activityContext)) {
            is GoogleSignInResult.Ok -> cloud.auth.signInWithGoogle(got.idToken, got.rawNonce)
            is GoogleSignInResult.Failed -> CloudResult.Failed(got.reason, retryable = true)
            GoogleSignInResult.Cancelled -> CloudResult.Failed(CANCELLED, retryable = true)
        }
    }

    suspend fun signOut(): CloudResult<Unit> = cloud.auth.signOut()

    suspend fun deleteAccount(): CloudResult<Unit> = cloud.auth.deleteAccount()

    /**
     * Sync Now queues everything again and drains. The forced backfill is
     * for the person who has been told their data is not there; per-record
     * truth still comes from the outbox states, never from this call.
     */
    suspend fun syncNow(): CloudResult<Int> {
        // The tap clears every backoff first: a person asking is not the
        // unattended retry the backoff was written for.
        cloud.outbox.retryNow()
        val r = cloud.circleActions.enqueueAll()
        cloud.syncEngine.kick()
        return r
    }

    companion object {
        /** The reason a cancelled Google picker reports; screens show nothing for it. */
        const val CANCELLED = "cancelled"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as SafeShadeApplication
                CloudViewModel(app, app.container)
            }
        }
    }
}

/** The account page's sync plate, as facts. */
data class SyncSummary(
    val pending: Int = 0,
    val lastSyncedAt: Long? = null,
    val syncing: Boolean = false,
    val lastError: String? = null
)
