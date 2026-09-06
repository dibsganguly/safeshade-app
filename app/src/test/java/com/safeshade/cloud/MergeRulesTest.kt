package com.safeshade.cloud

import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.repo.CloudIds
import com.safeshade.cloud.repo.MergeRules
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two merges where plain last-write-wins would hurt somebody.
 */
class MergeRulesTest {

    private val alertId = "33333333-3333-4333-8333-333333333333"

    /** Derived ids are namespaced by circle, so every call needs one. */
    private val circle = "11111111-1111-4111-8111-111111111111"

    // ============================================
    // ALERTS
    // ============================================

    @Test
    fun `a remote pending never reopens an alert this phone has resolved`() {
        val local = listOf(
            FallAlertEvent(id = alertId, kind = TripKind.FALL, outcome = TripOutcome.DISMISSED)
        )
        val remote = listOf(
            AlertRow(id = CloudIds.cloudId(alertId, circle), kind = "FALL", outcome = "PENDING")
        )

        val merged = MergeRules.alerts(local, remote, circle)

        assertEquals(1, merged.size)
        assertEquals(TripOutcome.DISMISSED, merged.first().outcome)
    }

    @Test
    fun `a real remote outcome does land`() {
        val local = listOf(
            FallAlertEvent(id = alertId, kind = TripKind.FALL, outcome = TripOutcome.PENDING)
        )
        val remote = listOf(
            AlertRow(id = CloudIds.cloudId(alertId, circle), kind = "FALL", outcome = "CONTACTED")
        )

        assertEquals(
            TripOutcome.CONTACTED,
            MergeRules.alerts(local, remote, circle).first().outcome
        )
    }

    @Test
    fun `having contacted somebody is never un-said`() {
        val local = listOf(
            FallAlertEvent(id = alertId, outcome = TripOutcome.CONTACTED, wasEmergencyContacted = true)
        )
        val remote = listOf(
            AlertRow(
                id = CloudIds.cloudId(alertId, circle),
                outcome = "CONTACTED",
                wasEmergencyContacted = false
            )
        )

        assertTrue(MergeRules.alerts(local, remote, circle).first().wasEmergencyContacted)
    }

    @Test
    fun `a pending local edit wins over anything arriving`() {
        val local = listOf(
            FallAlertEvent(id = alertId, note = "typed here", outcome = TripOutcome.DISMISSED)
        )
        val remote = listOf(
            AlertRow(id = CloudIds.cloudId(alertId, circle), note = "from elsewhere", outcome = "CONTACTED")
        )

        val merged = MergeRules.alerts(local, remote, circle, pending = setOf(alertId))

        assertEquals("typed here", merged.first().note)
        assertEquals(TripOutcome.DISMISSED, merged.first().outcome)
    }

    @Test
    fun `an unseen alert arrives, and a tombstoned one leaves`() {
        val newId = "66666666-6666-4666-8666-666666666666"
        val arrived = MergeRules.alerts(
            local = emptyList(),
            remote = listOf(AlertRow(id = newId, kind = "SOS", outcome = "PENDING")),
            circleId = circle
        )
        assertEquals(1, arrived.size)
        assertEquals(TripKind.SOS, arrived.first().kind)

        val gone = MergeRules.alerts(
            local = listOf(FallAlertEvent(id = alertId)),
            remote = listOf(
                AlertRow(id = CloudIds.cloudId(alertId, circle), deletedAt = "2026-09-07T10:00:00Z")
            ),
            circleId = circle
        )
        assertTrue(gone.isEmpty())
    }

    // ============================================
    // CONTACTS
    // ============================================

    @Test
    fun `contacts union by phone number rather than replacing the list`() {
        val local = listOf(EmergencyContact(name = "Sister", phone = "91234 56789"))
        val remote = listOf(
            EmergencyContactRow(id = "a", name = "Ravi", phone = "+91 98765 43210")
        )

        val merged = MergeRules.contacts(local, remote)

        assertEquals(2, merged.size)
        assertTrue(merged.any { it.name == "Sister" })
        assertTrue(merged.any { it.name == "Ravi" })
    }

    @Test
    fun `one number written two ways is one contact`() {
        val local = listOf(EmergencyContact(name = "Amma", phone = "9876543210"))
        val remote = listOf(
            EmergencyContactRow(id = "a", name = "Mother", phone = "+91 98765 43210")
        )

        val merged = MergeRules.contacts(local, remote)

        assertEquals(1, merged.size)
        assertEquals("Mother", merged.first().name)
    }

    @Test
    fun `a tombstoned contact is removed, and only that one`() {
        val local = listOf(
            EmergencyContact(name = "Sister", phone = "91234 56789"),
            EmergencyContact(name = "Ravi", phone = "98765 43210")
        )
        val remote = listOf(
            EmergencyContactRow(
                id = "a",
                phone = "98765 43210",
                deletedAt = "2026-09-07T10:00:00Z"
            )
        )

        val merged = MergeRules.contacts(local, remote)

        assertEquals(1, merged.size)
        assertEquals("Sister", merged.first().name)
    }

    @Test
    fun `two phones cannot both nominate a primary contact`() {
        val local = listOf(EmergencyContact(name = "Sister", phone = "91234 56789", isPrimary = true))
        val remote = listOf(
            EmergencyContactRow(id = "a", name = "Ravi", phone = "98765 43210", priority = 0)
        )

        val merged = MergeRules.contacts(local, remote)

        assertEquals(1, merged.count { it.isPrimary })
    }

    // ============================================
    // ZONES
    // ============================================

    @Test
    fun `a zone keeps its local id so the geofence registration still matches`() {
        val zone = GeofenceZone(id = "zone-1", name = "Home", lat = 1.0, lon = 2.0)
        val remote = listOf(
            ZoneRow(
                id = CloudIds.cloudId("zone-1", circle),
                name = "Home again",
                lat = 1.0,
                lon = 2.0,
                radiusMeters = 300.0
            )
        )

        val merged = MergeRules.zones(listOf(zone), remote, circle)

        assertEquals(1, merged.size)
        assertEquals("zone-1", merged.first().id)
        assertEquals("Home again", merged.first().name)
        assertEquals(300f, merged.first().radiusMeters, 0.01f)
    }

    @Test
    fun `a pulled wearer id is translated back into the local one`() {
        val remote = listOf(
            ZoneRow(
                id = "77777777-7777-4777-8777-777777777777",
                name = "School",
                lat = 1.0,
                lon = 2.0,
                wearerId = CloudIds.cloudId("wearer-primary", circle)
            )
        )

        val merged = MergeRules.zones(
            local = emptyList(),
            remote = remote,
            circleId = circle,
            wearerIds = mapOf(CloudIds.cloudId("wearer-primary", circle) to "wearer-primary")
        )

        assertEquals("wearer-primary", merged.first().wearerId)
    }

    @Test
    fun `a zone with no owner stays owned by nobody`() {
        val merged = MergeRules.zones(
            local = emptyList(),
            remote = listOf(
                ZoneRow(id = "88888888-8888-4888-8888-888888888888", name = "Park", lat = 0.0, lon = 0.0)
            ),
            circleId = circle
        )
        assertNull(merged.first().wearerId)
        assertFalse(merged.isEmpty())
    }
}
