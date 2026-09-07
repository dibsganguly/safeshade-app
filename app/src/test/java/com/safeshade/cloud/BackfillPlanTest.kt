package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.BackfillPlan
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.PRIMARY_WEARER_ID
import com.safeshade.data.PairedDevice
import com.safeshade.data.QuickMessage
import com.safeshade.data.Wearer
import com.safeshade.repo.SyncKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one-off queueing that happens the first time somebody signs in.
 *
 * The bug it exists for: the hooks only fire on a *write*, so a phone that has
 * been used offline for a month has nothing queued at all, and its owner signs
 * in and reads "Nothing yet" on a full Account page.
 */
class BackfillPlanTest {

    private val circle = "11111111-1111-4111-8111-111111111111"

    private val wearer = Wearer(
        id = PRIMARY_WEARER_ID,
        name = "Amma",
        medicalId = MedicalId(bloodType = "B+"),
        contacts = listOf(EmergencyContact(name = "Ravi", phone = "98765 43210"))
    )

    private val snapshot = SyncSnapshot(
        circleId = circle,
        userId = "22222222-2222-4222-8222-222222222222",
        wearers = listOf(wearer),
        globalContacts = listOf(EmergencyContact(name = "Sister", phone = "91234 56789")),
        pairedDevices = listOf(PairedDevice(address = "AA:BB:CC:DD:EE:FF", name = "S1")),
        alerts = listOf(FallAlertEvent(id = "alert-1", timestamp = 5L)),
        messages = listOf(QuickMessage(id = "msg-1", text = "On my way", timestamp = 5L)),
        zones = listOf(GeofenceZone(id = "zone-1", name = "Home", lat = 1.0, lon = 2.0))
    )

    @Test
    fun `every kind of record this phone holds is queued`() {
        val plan = BackfillPlan.of(snapshot)

        assertTrue(CloudTables.PROFILES to SyncKeys.PROFILE_SELF in plan)
        assertTrue(CloudTables.WEARERS to PRIMARY_WEARER_ID in plan)
        assertTrue(CloudTables.MEDICAL_IDS to SyncKeys.medical(PRIMARY_WEARER_ID) in plan)
        assertTrue(
            CloudTables.EMERGENCY_CONTACTS to SyncKeys.contact(PRIMARY_WEARER_ID, "98765 43210")
                in plan
        )
        assertTrue(
            CloudTables.EMERGENCY_CONTACTS to SyncKeys.contact(null, "91234 56789") in plan
        )
        assertTrue(CloudTables.DEVICES to SyncKeys.device("AA:BB:CC:DD:EE:FF") in plan)
        assertTrue(CloudTables.ZONES to "zone-1" in plan)
        assertTrue(CloudTables.ALERTS to "alert-1" in plan)
        assertTrue(CloudTables.MESSAGES to "msg-1" in plan)
    }

    @Test
    fun `signed out, the profile is not queued because it has no primary key`() {
        val plan = BackfillPlan.of(snapshot.copy(userId = null))
        assertFalse(plan.any { it.first == CloudTables.PROFILES })
        // Everything else still is: the circle is what those rows need.
        assertTrue(CloudTables.WEARERS to PRIMARY_WEARER_ID in plan)
    }

    @Test
    fun `an untouched medical record is not queued as a row of nulls`() {
        val plan = BackfillPlan.of(
            snapshot.copy(wearers = listOf(wearer.copy(medicalId = MedicalId())))
        )
        assertFalse(plan.any { it.first == CloudTables.MEDICAL_IDS })
        assertTrue(CloudTables.WEARERS to PRIMARY_WEARER_ID in plan)
    }

    @Test
    fun `nothing is queued twice`() {
        val plan = BackfillPlan.of(snapshot)
        assertEquals(plan.size, plan.distinct().size)
    }

    // ============================================
    // THE CAP
    // ============================================

    @Test
    fun `history is capped so identity is never evicted from the queue`() {
        // The outbox evicts oldest-first at 500. A year of trips would push the
        // wearer, the medical ID and the emergency contacts straight back out,
        // and the app would sync the history and lose the person it belongs to.
        val huge = snapshot.copy(
            alerts = (1..2000).map { FallAlertEvent(id = "alert-$it", timestamp = it.toLong()) },
            messages = (1..2000).map {
                QuickMessage(id = "msg-$it", text = "hello $it", timestamp = it.toLong())
            }
        )

        val plan = BackfillPlan.of(huge, limit = 500)

        assertTrue(plan.size <= 500)
        assertTrue(CloudTables.PROFILES to SyncKeys.PROFILE_SELF in plan)
        assertTrue(CloudTables.WEARERS to PRIMARY_WEARER_ID in plan)
        assertTrue(CloudTables.MEDICAL_IDS to SyncKeys.medical(PRIMARY_WEARER_ID) in plan)
        assertTrue(CloudTables.ZONES to "zone-1" in plan)
    }

    @Test
    fun `the history that survives the cap is the newest`() {
        val huge = snapshot.copy(
            alerts = (1..2000).map { FallAlertEvent(id = "alert-$it", timestamp = it.toLong()) },
            messages = emptyList()
        )

        val plan = BackfillPlan.of(huge, limit = 500)

        // An alert from this morning matters to somebody; one from March is a
        // record. If something has to be left behind it is the oldest.
        assertTrue(CloudTables.ALERTS to "alert-2000" in plan)
        assertFalse(CloudTables.ALERTS to "alert-1" in plan)
    }

    @Test
    fun `identity alone over the cap leaves history nothing, rather than the reverse`() {
        val manyZones = snapshot.copy(
            zones = (1..600).map {
                GeofenceZone(id = "zone-$it", name = "z$it", lat = 1.0, lon = 2.0)
            }
        )

        val plan = BackfillPlan.of(manyZones, limit = 500)

        assertTrue(CloudTables.WEARERS to PRIMARY_WEARER_ID in plan)
        assertFalse(plan.any { it.first == CloudTables.ALERTS })
    }
}
