package com.safeshade

import com.safeshade.data.FallSensitivity
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.device.DeviceProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the wire format.
 *
 * This is the one part of the BLE path whose failure mode is complete silence.
 * The firmware parser splits on commas positionally with no escaping, and the
 * ATT layer truncates an over-long write rather than rejecting it — so a
 * malformed Medical ID does not throw, does not log, and does not fail an ack.
 * It just puts the wearer's age in the Conditions field on a screen a first
 * responder reads.
 *
 * `DeviceProtocol` is pure Kotlin with no Android imports precisely so that
 * these can be plain JVM tests with no instrumentation, no Robolectric and no
 * coroutines runtime.
 */
class DeviceProtocolTest {

    // ============================================
    // Field count
    // ============================================

    @Test
    fun `health always emits exactly 11 fields`() {
        val payload = DeviceProtocol.health(FULL_MEDICAL_ID)
        assertEquals(11, payload.split(",").size)
    }

    @Test
    fun `health emits 11 fields even when every input is empty`() {
        val payload = DeviceProtocol.health(MedicalId())
        assertEquals(11, payload.split(",").size)
    }

    // ============================================
    // Delimiter safety
    // ============================================

    @Test
    fun `no field contains a comma even when every input does`() {
        // The realistic version of this: a guardian types "Penicillin, Peanuts"
        // into Allergies, which shifts age into Conditions and every later
        // field by one, on a card a paramedic reads.
        val hostile = MedicalId(
            bloodType = "A,B",
            emergencyContact = "+91,98765,43210",
            contactName = "Ganguly, D",
            allergies = "Penicillin, Peanuts, Latex",
            age = 71,
            conditions = "Diabetes, Hypertension",
            medications = "Metformin, Ramipril",
            secondaryContactName = "Roy, S",
            secondaryContact = "+91,12345,67890",
            organDonor = true,
            notes = "Deaf, left ear"
        )

        val payload = DeviceProtocol.health(hostile)

        // Exactly ten commas means exactly eleven fields, which means not one
        // of the eleven inputs contributed a delimiter of its own.
        assertEquals(11, payload.split(",").size)
        assertEquals(10, payload.count { it == ',' })
    }

    @Test
    fun `clean strips newlines as well as commas`() {
        // The firmware reads a line at a time in some paths, so an embedded
        // newline truncates just as destructively as a comma shifts.
        val cleaned = DeviceProtocol.clean("one,\ntwo\r\nthree", 100)
        assertTrue(cleaned, !cleaned.contains(','))
        assertTrue(cleaned, !cleaned.contains('\n'))
        assertTrue(cleaned, !cleaned.contains('\r'))
    }

    // ============================================
    // organDonor
    // ============================================

    @Test
    fun `organDonor serialises as 1 when true`() {
        val payload = DeviceProtocol.health(FULL_MEDICAL_ID.copy(organDonor = true))
        assertEquals("1", payload.split(",")[ORGAN_DONOR_INDEX])
    }

    @Test
    fun `organDonor serialises as 0 when false`() {
        val payload = DeviceProtocol.health(FULL_MEDICAL_ID.copy(organDonor = false))
        assertEquals("0", payload.split(",")[ORGAN_DONOR_INDEX])
    }

    // ============================================
    // MTU budget
    // ============================================

    @Test
    fun `health never exceeds the mtu minus ATT overhead`() {
        val oversized = MedicalId(
            bloodType = "AB-negative",
            emergencyContact = "+919876543210",
            contactName = "A very long emergency contact name",
            allergies = "A".repeat(120),
            age = 71,
            conditions = "C".repeat(120),
            medications = "M".repeat(120),
            secondaryContactName = "Another long secondary contact name",
            secondaryContact = "+911234567890",
            organDonor = true,
            notes = "N".repeat(120)
        )

        for (mtu in listOf(23, 40, 64, 100, 185, 247, 512)) {
            val payload = DeviceProtocol.health(oversized, mtu)
            val budget = (mtu - ATT_OVERHEAD).coerceIn(20, 512)
            assertTrue(
                "mtu=$mtu produced ${payload.length} bytes, budget was $budget",
                payload.length <= budget
            )
        }
    }

    @Test
    fun `degradation shortens notes before allergies`() {
        // Sized so that halving exactly one field is enough. If the order were
        // reversed, allergies would be the field that lost half its content and
        // notes would be untouched.
        val medicalId = MedicalId(
            bloodType = "AB",
            emergencyContact = "1",
            contactName = "C",
            allergies = "A".repeat(40),
            age = 30,
            organDonor = false,
            notes = "N".repeat(40)
        )

        val fields = DeviceProtocol.health(medicalId, mtu = 80).split(",")

        assertEquals("allergies must be untouched", 40, fields[ALLERGIES_INDEX].length)
        assertEquals("notes must be halved", 20, fields[NOTES_INDEX].length)
    }

    @Test
    fun `responder-critical fields survive the hardest truncation`() {
        // At the BLE minimum MTU the payload is hard-truncated, which can cost
        // trailing fields. What must not happen is losing the blood type or the
        // emergency contact, because the degradation order exists to protect
        // exactly those.
        val medicalId = FULL_MEDICAL_ID.copy(bloodType = "O+", emergencyContact = "112")
        val payload = DeviceProtocol.health(medicalId, mtu = 23)

        assertTrue(payload, payload.startsWith("O+,112,"))
    }

    // ============================================
    // SETTINGS
    // ============================================

    @Test
    fun `settings maps fall sensitivity to the firmware ordinal`() {
        // The firmware clamps this field to 0..2 in LOW, MEDIUM, HIGH order.
        // Reordering the enum would silently change what the device does, which
        // is the only reason this test exists.
        val base = SafetySettings()
        assertEquals(
            "0",
            DeviceProtocol.settings(base.copy(fallSensitivity = FallSensitivity.LOW)).split(",")[0]
        )
        assertEquals(
            "1",
            DeviceProtocol.settings(base.copy(fallSensitivity = FallSensitivity.MEDIUM)).split(",")[0]
        )
        assertEquals(
            "2",
            DeviceProtocol.settings(base.copy(fallSensitivity = FallSensitivity.HIGH)).split(",")[0]
        )
    }

    @Test
    fun `settings emits five fields with booleans as 1 and 0`() {
        val payload = DeviceProtocol.settings(
            SafetySettings(
                fallSensitivity = FallSensitivity.HIGH,
                sosVolumeLevel = 0.5f,
                autoCallEmergency = true,
                parentalControlsEnabled = false,
                smsFallbackEnabled = true
            )
        )
        assertEquals(listOf("2", "50", "1", "0", "1"), payload.split(","))
    }

    // ============================================
    // PersonaMode wire names
    // ============================================

    @Test
    fun `fromWire resolves AUTO`() {
        // AUTO is the firmware's fresh-boot default and had no app
        // representation at all until now, so a factory-fresh device sat in a
        // mode the app could neither display nor select.
        assertEquals(PersonaMode.AUTO, PersonaMode.fromWire("AUTO"))
    }

    @Test
    fun `fromWire falls back to BACKPACK for an unknown name`() {
        // Matches the firmware's own modeFromName(), which deliberately falls
        // back to BACKPACK rather than AUTO so that garbage from an old app
        // build never lands the device on the adaptive placeholder.
        assertEquals(PersonaMode.BACKPACK, PersonaMode.fromWire("garbage"))
        assertEquals(PersonaMode.BACKPACK, PersonaMode.fromWire(""))
    }

    @Test
    fun `fromWire is case and whitespace insensitive`() {
        assertEquals(PersonaMode.ELDERLY, PersonaMode.fromWire(" elderly "))
    }

    @Test
    fun `every mode round-trips through its wire name`() {
        // The wire name is the contract with modeFromName(); a typo in one
        // entry would be a mode the device silently refuses to enter.
        PersonaMode.entries.forEach { mode ->
            assertEquals(mode, PersonaMode.fromWire(DeviceProtocol.mode(mode)))
        }
    }

    private companion object {
        const val ATT_OVERHEAD = 3

        // Positional indices into the 11-field HEALTH payload.
        const val ALLERGIES_INDEX = 3
        const val ORGAN_DONOR_INDEX = 9
        const val NOTES_INDEX = 10

        val FULL_MEDICAL_ID = MedicalId(
            bloodType = "O+",
            emergencyContact = "+919876543210",
            contactName = "Dibyendu",
            allergies = "Penicillin",
            age = 71,
            conditions = "Hypertension",
            medications = "Ramipril",
            secondaryContactName = "Sohini",
            secondaryContact = "+911234567890",
            organDonor = true,
            notes = "Hard of hearing"
        )
    }
}
