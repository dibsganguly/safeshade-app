package com.safeshade.cloud

import com.safeshade.cloud.dto.AlertDeliveryRow
import com.safeshade.cloud.dto.AlertRow
import com.safeshade.cloud.dto.CircleMemberRow
import com.safeshade.cloud.dto.CircleRow
import com.safeshade.cloud.dto.DeviceRow
import com.safeshade.cloud.dto.DeviceSightingRow
import com.safeshade.cloud.dto.EmergencyContactRow
import com.safeshade.cloud.dto.EvidenceRow
import com.safeshade.cloud.dto.FirmwareReleaseRow
import com.safeshade.cloud.dto.InviteRow
import com.safeshade.cloud.dto.MedicalIdRow
import com.safeshade.cloud.dto.MessageRow
import com.safeshade.cloud.dto.ProfileRow
import com.safeshade.cloud.dto.SmartHomeHookRow
import com.safeshade.cloud.dto.SubscriptionRow
import com.safeshade.cloud.dto.VitalsSampleRow
import com.safeshade.cloud.dto.WearerRow
import com.safeshade.cloud.dto.ZoneEventRow
import com.safeshade.cloud.dto.ZoneRow
import com.safeshade.cloud.dto.toDomain
import com.safeshade.cloud.dto.toRow
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The DTO contract, checked.
 *
 * ### The `{}` test, and why it is the important one
 *
 * Every row class must decode from an empty JSON object. That is not a
 * hypothetical: PostgREST returns only the columns a `select` asked for, and a
 * server migration can add or remove a column under a phone that has not
 * updated in months. If any field on any of these classes is ever made non-null
 * or loses its default, the decode stops being total and starts throwing
 * `MissingFieldException` — inside a coroutine, on somebody's phone, on a
 * screen unrelated to the change.
 *
 * The failure mode is what makes this worth a test: it does not break the build,
 * it does not break the emulator (which is always talking to a schema that
 * matches), and it breaks precisely on the phones that have not updated. So it
 * is asserted mechanically over every class, and adding a new row DTO means
 * adding one line here.
 *
 * A round-trip check also runs on each class: decode `{}`, re-encode, decode
 * again. That catches the other half — a field whose serializer cannot handle
 * the value its own default produces.
 */
class CloudDtoTest {

    /**
     * The same configuration `SupabaseCloudClient` builds its client with. A
     * test that used stock `Json` would be testing a decoder the app never runs.
     */
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val all: List<Pair<String, KSerializer<*>>> = listOf(
        "profiles" to ProfileRow.serializer(),
        "circles" to CircleRow.serializer(),
        "circle_members" to CircleMemberRow.serializer(),
        "wearers" to WearerRow.serializer(),
        "devices" to DeviceRow.serializer(),
        "medical_ids" to MedicalIdRow.serializer(),
        "emergency_contacts" to EmergencyContactRow.serializer(),
        "alerts" to AlertRow.serializer(),
        "alert_deliveries" to AlertDeliveryRow.serializer(),
        "messages" to MessageRow.serializer(),
        "vitals_samples" to VitalsSampleRow.serializer(),
        "evidence" to EvidenceRow.serializer(),
        "zones" to ZoneRow.serializer(),
        "zone_events" to ZoneEventRow.serializer(),
        "subscriptions" to SubscriptionRow.serializer(),
        "invites" to InviteRow.serializer(),
        "firmware_releases" to FirmwareReleaseRow.serializer(),
        "smart_home_hooks" to SmartHomeHookRow.serializer(),
        "device_sightings" to DeviceSightingRow.serializer()
    )

    @Test
    fun `every row dto decodes from an empty object`() {
        all.forEach { (table, serializer) ->
            val decoded = runCatching { json.decodeFromString(serializer, "{}") }
            assertTrue(
                "$table did not decode from {}: ${decoded.exceptionOrNull()}",
                decoded.isSuccess
            )
            assertNotNull(table, decoded.getOrNull())
        }
    }

    @Test
    fun `every row dto survives a round trip`() {
        all.forEach { (table, serializer) ->
            @Suppress("UNCHECKED_CAST")
            val s = serializer as KSerializer<Any?>
            val once = json.decodeFromString(s, "{}")
            val encoded = json.encodeToString(s, once)
            val twice = runCatching { json.decodeFromString(s, encoded) }
            assertTrue(
                "$table failed to round trip via $encoded: ${twice.exceptionOrNull()}",
                twice.isSuccess
            )
        }
    }

    @Test
    fun `a row with unknown columns still decodes`() {
        // The forward-compatibility half: a column added by a migration must
        // not break a phone that has never heard of it.
        val withFuture = """{"id":"z1","circle_id":"c1","some_future_column":42}"""
        val row = json.decodeFromString(ZoneRow.serializer(), withFuture)
        assertEquals("z1", row.id)
        assertEquals("c1", row.circleId)
    }

    @Test
    fun `null fields are omitted on encode, not sent as null`() {
        // This is what stops a phone that knows half a row from erasing the
        // other half on upsert. If explicitNulls ever gets flipped back on,
        // this fails.
        val encoded = json.encodeToString(ZoneRow.serializer(), ZoneRow(id = "z1"))
        val parsed = json.parseToJsonElement(encoded) as JsonObject
        assertEquals(setOf("id"), parsed.keys)
    }

    // ============================================
    // Mappers
    // ============================================

    @Test
    fun `a fall alert round trips through its row`() {
        val original = FallAlertEvent(
            id = "a1",
            timestamp = 1_757_000_000_000L,
            kind = TripKind.PHONE_SOS,
            outcome = TripOutcome.CONTACTED,
            wasEmergencyContacted = true,
            location = "Kharagpur, WB",
            note = "impact 3.1g"
        )
        val back = original.toRow(circleId = "c1").toDomain()
        assertEquals(original, back)
    }

    @Test
    fun `an unknown trip kind degrades instead of throwing`() {
        // The reason enums cross the wire by name and are decoded defensively:
        // a newer build can write a kind this one has never heard of, and a
        // trip log that crashes is worse than one that says FALL.
        val row = AlertRow(id = "a1", kind = "TELEPORT_INCIDENT", outcome = "???")
        val domain = row.toDomain()
        assertEquals(TripKind.FALL, domain.kind)
        assertEquals(TripOutcome.PENDING, domain.outcome)
    }

    @Test
    fun `an unparseable timestamp does not become now`() {
        // Dating a two-week-old fall to this morning would silently reorder the
        // trip log around it. Zero is visibly wrong, which is the point.
        val domain = AlertRow(id = "a1", occurredAt = "not a date").toDomain()
        assertEquals(0L, domain.timestamp)
    }

    @Test
    fun `a medical id round trips through its row`() {
        val original = MedicalId(
            bloodType = "O+",
            emergencyContact = "+919876543210",
            contactName = "Rina",
            allergies = "Penicillin",
            age = 71,
            conditions = "Type 2 diabetes",
            medications = "Metformin",
            secondaryContactName = "Arun",
            secondaryContact = "+919812345678",
            organDonor = true,
            notes = "Hard of hearing on the left"
        )
        val back = original.toRow(circleId = "c1", rowId = "m1").toDomain()
        assertEquals(original, back)
    }

    @Test
    fun `a medical id row with no columns yields blanks, not nulls`() {
        val domain = MedicalIdRow().toDomain()
        assertEquals("", domain.bloodType)
        assertEquals(0, domain.age)
        assertEquals(false, domain.organDonor)
        // filledFieldCount must not throw on a wholly empty card.
        assertEquals(0, domain.filledFieldCount)
    }

    @Test
    fun `a zone round trips through its row`() {
        val original = GeofenceZone(
            id = "z1",
            name = "Home",
            lat = 22.3149,
            lon = 87.3105,
            radiusMeters = 150f,
            alertOnExit = true,
            alertOnEnter = true
        )
        assertEquals(original, original.toRow(circleId = "c1").toDomain())
    }

    @Test
    fun `a zone with no radius gets the default, not zero`() {
        // A zero-radius zone matches nothing and silently stops alerting, which
        // is the worst possible failure for a safe zone.
        assertEquals(200f, ZoneRow(id = "z1", name = "Home").toDomain().radiusMeters, 0.001f)
    }
}
