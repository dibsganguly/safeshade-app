package com.safeshade.cloud

/**
 * What the app knows about the Circle, as one value the UI can render.
 *
 * ### Why this exists rather than the screens reading tables
 *
 * `circle_members`, `invites` and `subscriptions` are the three synced tables
 * with no repository behind them, because nothing in the offline app has an
 * opinion about who else is in the circle or what anybody is paying. They are
 * pure server facts. Giving them a repository each would mean three DataStore
 * blobs that are only ever overwritten by a pull, which is a cache pretending
 * to be a source of truth.
 *
 * So they live here, in memory, rebuilt by each pull, and the Circle and Plan
 * screens read one flow. On a cold start before the first pull the state is
 * empty - and empty is honest: this phone has not been told yet.
 */
data class CloudState(
    /** The signed-in user's circle, or null when signed out or not resolved. */
    val circleId: String? = null,

    val members: List<CircleMember> = emptyList(),

    val invites: List<CircleInvite> = emptyList(),

    /**
     * The tier the `subscriptions` table says this account has.
     *
     * [CloudTier.FREE] when there is no row, which is the correct reading: the
     * table is written by the billing webhook under the service role and is
     * read-only to every client, so an absent row means nobody has paid, not
     * that the answer is unknown.
     */
    val tier: CloudTier = CloudTier.FREE,

    /** A tier set by hand on this phone. See [effectiveTier]. */
    val devTierOverride: CloudTier? = null,

    /**
     * The last thing that went wrong while talking to the Circle, in plain
     * English, or null.
     *
     * Kept so a screen can say what failed rather than showing an empty list
     * and letting the user guess whether their family has no members or the
     * request did not arrive.
     */
    val lastError: String? = null
) {

    /**
     * What the gates actually read.
     *
     * There is no Play Console listing yet, so a real purchase cannot be made
     * and every paid surface would otherwise be unreachable even to look at.
     * The override is deliberately *not* folded into [tier]: a screen that
     * wants to show what the account is really entitled to still can, and the
     * two never get confused for one another.
     */
    val effectiveTier: CloudTier get() = devTierOverride ?: tier

    /** True when a circle has been resolved and there is somewhere to sync to. */
    val hasCircle: Boolean get() = !circleId.isNullOrBlank()
}

/** What a person may do in a Circle. Mirrors the `role` CHECK in the schema. */
enum class CircleRole(val wire: String) {
    /** Invites and removes people, and changes anything. */
    OWNER("owner"),

    /** Sees alerts and acknowledges them, sends messages, changes settings. */
    GUARDIAN("guardian"),

    /** Sees everything and changes nothing - enforced in SQL, not only in copy. */
    VIEWER("viewer");

    companion object {
        fun fromWire(raw: String?): CircleRole =
            entries.firstOrNull { it.wire.equals(raw?.trim(), ignoreCase = true) } ?: GUARDIAN
    }
}

/**
 * One person in the Circle.
 *
 * @param email null for everybody except the signed-in user, and that is a
 *   property of the schema rather than an omission here: `circle_members`
 *   carries a display name and an avatar but no address, and the `profiles`
 *   policy is `auth.uid() = id`, so one member genuinely cannot read another's
 *   email. Copy must not promise one.
 */
data class CircleMember(
    val userId: String,
    val name: String? = null,
    val email: String? = null,
    val role: CircleRole = CircleRole.GUARDIAN
)

/**
 * An invitation, and what happened to the email carrying it.
 *
 * The two are separate facts and are reported separately, always. The row *is*
 * the invitation - if Resend refuses, the invite still exists and its link can
 * be copied out of the app. Collapsing the two would tell an owner their sister
 * had been invited when the mail bounced, which is the class of lie this
 * codebase has already shipped once.
 */
data class CircleInvite(
    val id: String,
    val email: String,
    val role: CircleRole,
    val status: InviteStatus,
    /** Epoch millis, from the row's `created_at`. */
    val sentAt: Long = 0L
)

/** Where an invitation has got to. */
sealed interface InviteStatus {

    /** Sent, not yet accepted, not yet expired. */
    data object Pending : InviteStatus

    /** Somebody used the link; they are in the Circle. */
    data object Accepted : InviteStatus

    /** Past `expires_at`. Invite again to send a new link. */
    data object Expired : InviteStatus

    /**
     * The invitation exists but the email did not arrive.
     *
     * @param reason the provider's own message, carried verbatim. With no
     *   sending domain configured, `onboarding@resend.dev` delivers only to the
     *   Resend account owner's address and every other recipient comes back
     *   here with Resend's explanation - which is the honest thing to show,
     *   and far more useful than "could not send".
     */
    data class Failed(val reason: String) : InviteStatus
}

/** The three plans. Mirrors the `tier` CHECK on `subscriptions`. */
enum class CloudTier(val wire: String) {
    FREE("free"),
    PLUS("plus"),
    PRO("pro");

    companion object {
        fun fromWire(raw: String?): CloudTier =
            entries.firstOrNull { it.wire.equals(raw?.trim(), ignoreCase = true) } ?: FREE
    }
}

/**
 * One cell of the community heat map.
 *
 * @param lat the cell centre, rounded to two decimals by the materialized view
 *   - about 1.1 km at the equator. Fine enough to show which junction is
 *   dangerous, coarse enough not to be an address.
 * @param count incidents in the cell, summed across kinds. The view has a
 *   `having count(*) >= 5` floor, so no cell can be traced back to one person's
 *   route.
 */
data class HeatCell(
    val lat: Double,
    val lon: Double,
    val count: Int
)
