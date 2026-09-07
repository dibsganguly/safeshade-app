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
     * Whether an alert pushed from this phone carries the place it happened.
     *
     * True by default, and true is the right default: the guardians in the
     * Circle are the reason a location is recorded at all, and a trip log that
     * says only "a fall, at some point, somewhere" helps nobody.
     *
     * Turning it off strips `lat`, `lon` and `location_label` from every alert
     * pushed **from then on** - see `PayloadResolver.alert`. The alert itself
     * still syncs, so the trip log stays consistent across the Circle; only the
     * place is withheld. It is a client-side rule and it is not retroactive:
     * rows already on the server keep the place they were sent with, including
     * in the community heat map built over them. Copy describing this switch
     * has to say so rather than implying an erasure it does not perform.
     */
    val shareAlertPlaces: Boolean = true,

    /**
     * Which emails this account has asked for, as the **server** last reported
     * them, or null when the profile has not been read yet.
     *
     * Null is not "all off" and it is not "all on": it is "this phone has not
     * been told". A settings page must render dashes rather than switches for
     * it, because a switch drawn in the off position against an unknown value
     * is a lie about a fall alert.
     *
     * The value is written by `set_email_prefs` and read back from it; the
     * sending functions read `profiles.email_prefs` themselves and this copy
     * never decides anything. A preference the client enforces is a preference
     * that stops working the moment anything else calls the function.
     */
    val emailPreferences: EmailPreferences? = null,

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

/**
 * The four email switches, as `profiles.email_prefs` holds them.
 *
 * ### Four, split by what the mail is FOR
 *
 * Not by which function sends it, and not one switch per email. A person
 * deciding what reaches their inbox is deciding between "tell me if she falls"
 * and "tell me who joined"; they are not deciding between `send-alert-email`
 * and `notify-joined`, and a settings page built on function names would make
 * them learn the server's vocabulary to answer a question about their mother.
 *
 * @param alerts a fall or an SOS nobody answered. The one switch whose
 *   consequence is worth spelling out on the page: turning it off means a fall
 *   alert does not reach this address, and the alert email says so to everybody
 *   else in the Circle rather than pretending the person was notified.
 * @param circle somebody joined, an invitation was accepted, membership
 *   changed.
 * @param weeklyReport the Monday summary.
 * @param account account-level notices, such as a deletion request.
 *
 * The Supabase sign-in emails - the six-digit code, address confirmation,
 * reauthentication - are **not** covered by any of these and cannot be turned
 * off here. They are sign-in mechanics rather than notifications: somebody who
 * has switched everything off still has to be able to sign in.
 *
 * Every field defaults to true because that is the schema default, and because
 * the alternative reading of an absent value would silently withhold a fall
 * alert from somebody who never opened the page.
 */
data class EmailPreferences(
    val alerts: Boolean = true,
    val circle: Boolean = true,
    val weeklyReport: Boolean = true,
    val account: Boolean = true
)

/**
 * What "send this week's report now" achieved, per address.
 *
 * Three lists rather than a count, and they are three different facts:
 *
 *  - [sent] Resend returned a 2xx for this address.
 *  - [skipped] this person has the weekly report switched off. Not a failure.
 *  - [failed] the address and the provider's own words. With no sending domain
 *    configured, every address except the Resend account owner's lands here
 *    carrying Resend's explanation, and that has to reach the screen intact.
 *
 * A page that showed only a total would be back to reporting a success that
 * reached nobody, which is the one thing this codebase is written against.
 */
data class WeeklyReportSend(
    val sent: List<String> = emptyList(),
    val skipped: List<String> = emptyList(),
    val failed: Map<String, String> = emptyMap()
)
