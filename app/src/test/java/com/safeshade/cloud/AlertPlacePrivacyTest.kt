package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.repo.PayloadResolver
import com.safeshade.cloud.repo.SyncSnapshot
import com.safeshade.cloud.sync.OutboxOp
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * The community heat-map opt-out, as the push actually applies it.
 *
 * The switch withholds the **place**, never the alert. A guardian in the Circle
 * has to keep seeing that a fall happened, when, and how it ended - a trip log
 * that silently loses entries would be a far worse privacy feature than one
 * that says "somewhere".
 *
 * Two limits are asserted here as well as the behaviour, because both are easy
 * to forget and expensive to rediscover:
 *
 *  * The key set does not change. `PayloadResolver` sends every row of a table
 *    with identical keys or PostgREST rejects the whole batch, so the place is
 *    stripped by being set to null and never by being dropped.
 *  * It is forward-looking. Nothing here reaches back for rows already pushed;
 *    those keep the place they were sent with, in the row and in the heat map
 *    built over it.
 */
class AlertPlacePrivacyTest {

    private val circle = "11111111-1111-4111-8111-111111111111"

    private val event = FallAlertEvent(
        id = "33333333-3333-4333-8333-333333333333",
        timestamp = 1_700_000_000_000L,
        kind = TripKind.FALL,
        outcome = TripOutcome.CONTACTED,
        wasEmergencyContacted = true,
        location = "Near Salt Lake Sector V",
        note = "Battery 78%"
    )

    private fun row(share: Boolean): JsonObject = requireNotNull(
        PayloadResolver.resolve(
            CloudTables.ALERTS,
            event.id,
            OutboxOp.UPSERT,
            SyncSnapshot(
                circleId = circle,
                userId = "22222222-2222-4222-8222-222222222222",
                alerts = listOf(event),
                shareAlertPlaces = share
            )
        )
    )

    @Test
    fun `sharing on, the place travels with the alert`() {
        val sent = row(share = true)
        assertEquals("Near Salt Lake Sector V", sent["location_label"]?.jsonPrimitive?.content)
    }

    @Test
    fun `sharing off, the three place columns go as null`() {
        val sent = row(share = false)
        assertEquals(JsonNull, sent["location_label"])
        assertEquals(JsonNull, sent["lat"])
        assertEquals(JsonNull, sent["lon"])
    }

    @Test
    fun `sharing off withholds the place and nothing else about the alert`() {
        val sent = row(share = false)
        assertEquals("FALL", sent["kind"]?.jsonPrimitive?.content)
        assertEquals("CONTACTED", sent["outcome"]?.jsonPrimitive?.content)
        assertEquals(true, sent["was_emergency_contacted"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals("Battery 78%", sent["note"]?.jsonPrimitive?.content)
        assertEquals(circle, sent["circle_id"]?.jsonPrimitive?.content)
        assertNotNull(sent["occurred_at"]?.jsonPrimitive?.content)
    }

    /**
     * The batch rule. Two alerts in one push, one from before the switch was
     * turned off and one from after, still have to have the same key set - and
     * they do, because a withheld place is a null and not a missing column.
     */
    @Test
    fun `withholding the place does not change the row's key set`() {
        assertEquals(row(share = true).keys, row(share = false).keys)
    }

    /**
     * The switch defaults to on, and it has to: the key is written only when
     * somebody changes it, and reading an absent key as "off" would quietly
     * stop sharing the place with the person's own guardians.
     */
    @Test
    fun `the default is to share, because the Circle is who the place is for`() {
        assertEquals(true, SyncSnapshot(circleId = circle).shareAlertPlaces)
        assertEquals(true, CloudState().shareAlertPlaces)
    }
}
