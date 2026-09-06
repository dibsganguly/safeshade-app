package com.safeshade.cloud

import com.safeshade.cloud.repo.CloudIds
import com.safeshade.data.PRIMARY_WEARER_ID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * The two quiet ways a synced id can be wrong.
 */
class CloudIdsTest {

    private val circleA = "11111111-1111-4111-8111-111111111111"
    private val circleB = "22222222-2222-4222-8222-222222222222"

    @Test
    fun `two families do not derive the same primary key for their own wearer`() {
        // "wearer-primary" is the literal id on every fresh install. Without the
        // circle in the hash, the second family to sync would upsert onto the
        // first family's row - a permission error under RLS every time, until
        // the outbox gives up and tells them their wearer did not reach the
        // cloud; or, where the caller is an actor in both circles, a row that
        // silently changes hands.
        assertNotEquals(
            CloudIds.cloudId(PRIMARY_WEARER_ID, circleA),
            CloudIds.cloudId(PRIMARY_WEARER_ID, circleB)
        )
        assertNotEquals(
            CloudIds.medicalId(PRIMARY_WEARER_ID, circleA),
            CloudIds.medicalId(PRIMARY_WEARER_ID, circleB)
        )
        // 112 is 112 in everybody's address book.
        assertNotEquals(
            CloudIds.contactId(null, "112", circleA),
            CloudIds.contactId(null, "112", circleB)
        )
        assertNotEquals(
            CloudIds.deviceId("AA:BB:CC:DD:EE:FF", circleA),
            CloudIds.deviceId("AA:BB:CC:DD:EE:FF", circleB)
        )
    }

    @Test
    fun `two guardians in one circle derive the same id for the same person`() {
        // The desirable half of the same rule: this is the row they share.
        assertEquals(
            CloudIds.cloudId(PRIMARY_WEARER_ID, circleA),
            CloudIds.cloudId(PRIMARY_WEARER_ID, circleA)
        )
    }

    @Test
    fun `a real uuid passes through untouched, in any circle`() {
        val id = "44444444-4444-4444-8444-444444444444"
        assertEquals(id, CloudIds.cloudId(id, circleA))
        assertEquals(id, CloudIds.cloudId(id, circleB))
    }

    @Test
    fun `a derived id is a legal uuid`() {
        val shape = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(CloudIds.cloudId(PRIMARY_WEARER_ID, circleA).matches(shape))
        assertTrue(CloudIds.deviceId("aa:bb", circleA).matches(shape))
    }

    @Test
    fun `a contact is identified by its number however it is written`() {
        assertEquals(
            CloudIds.contactId(null, "9876543210", circleA),
            CloudIds.contactId(null, "+91 98765 43210", circleA)
        )
    }

    // ============================================
    // TIMESTAMPS
    // ============================================

    @Test
    fun `a postgrest timestamp with a numeric offset parses`() {
        // PostgREST renders timestamptz as +00:00, not Z. Instant.parse only
        // accepts that from JDK 12, which Android below API 33 is not - so the
        // pull cursor would be null on every drain, every alert would land at
        // 1970, and no invitation would ever read as expired.
        assertEquals(
            Instant.parse("2026-09-07T10:00:00.123456Z"),
            parseServerInstant("2026-09-07T10:00:00.123456+00:00")
        )
        assertEquals(
            Instant.parse("2026-09-07T04:30:00Z"),
            parseServerInstant("2026-09-07T10:00:00+05:30")
        )
    }

    @Test
    fun `the Z spelling still parses`() {
        assertEquals(
            Instant.parse("2026-09-07T10:00:00Z"),
            parseServerInstant("2026-09-07T10:00:00Z")
        )
    }

    @Test
    fun `nothing unreadable throws`() {
        assertNull(parseServerInstant(null))
        assertNull(parseServerInstant(""))
        assertNull(parseServerInstant("last Tuesday"))
    }
}
