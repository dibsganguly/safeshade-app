package com.safeshade.cloud

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Circle cache that lets a cold start draw something.
 *
 * The bug: `circle_members` was pulled with an `updated_at` cursor, so on a
 * fresh process against an unchanged Circle the server correctly returned
 * nothing and the Guardians page sat on "Reading the Circle..." forever. The
 * pull is now whole-table for these lists; this cache is the other half, so the
 * page has yesterday's answer to draw while the first pull is in flight.
 */
class CircleCacheTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `members, invitations and tier survive a round trip`() {
        val stored = StoredCircleState(
            members = listOf(
                StoredMember("u1", "Dibs", "owner@example.com", "owner"),
                StoredMember("u2", "Ravi", null, "guardian")
            ),
            invites = listOf(StoredInvite("i1", "sis@example.com", "viewer", 42L, "pending")),
            tier = "plus"
        )

        val back = json.decodeFromString(
            StoredCircleState.serializer(),
            json.encodeToString(StoredCircleState.serializer(), stored)
        )

        assertEquals(stored, back)
        assertEquals(CloudTier.PLUS, CloudTier.fromWire(back.tier))
        assertEquals(CircleRole.OWNER, CircleRole.fromWire(back.members.first().role))
        // Nobody but the account holder has an email to store; see CircleMember.
        assertEquals(null, back.members[1].email)
    }

    @Test
    fun `a failed invitation keeps the provider's own words across a restart`() {
        // Otherwise an owner comes back to a red row with no explanation of why
        // their sister never got the email.
        val reason = "You can only send testing emails to your own email address"
        val wire = inviteStatusWire(InviteStatus.Failed(reason))
        val back = inviteStatusOf(wire)

        assertTrue(back is InviteStatus.Failed)
        assertEquals(reason, (back as InviteStatus.Failed).reason)
    }

    @Test
    fun `the other three statuses round trip too`() {
        assertEquals(InviteStatus.Pending, inviteStatusOf(inviteStatusWire(InviteStatus.Pending)))
        assertEquals(InviteStatus.Accepted, inviteStatusOf(inviteStatusWire(InviteStatus.Accepted)))
        assertEquals(InviteStatus.Expired, inviteStatusOf(inviteStatusWire(InviteStatus.Expired)))
    }

    @Test
    fun `an unreadable status is pending rather than a crash inside a restore`() {
        assertEquals(InviteStatus.Pending, inviteStatusOf(""))
        assertEquals(InviteStatus.Pending, inviteStatusOf("something-else"))
        // A failure with no message still says something a person can read.
        assertTrue((inviteStatusOf("failed:") as InviteStatus.Failed).reason.isNotBlank())
    }

    @Test
    fun `a blob written by an older build still parses`() {
        // Every field has a default, so a key added later is not a parse failure
        // on a phone that has not updated.
        val back = json.decodeFromString(StoredCircleState.serializer(), """{"tier":"pro"}""")

        assertEquals("pro", back.tier)
        assertTrue(back.members.isEmpty())
        assertTrue(back.invites.isEmpty())
    }
}
