package com.safeshade.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.google.gson.Gson
import com.safeshade.data.local.ProfileSnapshot
import com.safeshade.data.local.WearerDto
import com.safeshade.data.local.toDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The multi-wearer model, tested where it is a pure function.
 *
 * Two of these questions are the reason the model exists at all, and neither
 * is observable at runtime without two wearables and two people:
 *
 *  - **Whose device is this?** The connected wearer is not the selected
 *    wearer. Getting it wrong pushes one person's medical card - blood type,
 *    allergies, the lot - to another person's wearable, where a responder
 *    reads it in an emergency and believes it.
 *  - **Who does an SOS reach?** Per-wearer contacts supplement the global
 *    list; a union that dropped an entry would silently shorten the list of
 *    people an alarm reaches, and nothing on screen would look different.
 *
 * The migration is here for the third reason: it runs exactly once per
 * install, on an upgrade, on a user's real data, and a bug in it is
 * unobservable in development and unrecoverable in the field.
 */
class WearerModelTest {

    private val gson = Gson()

    private fun wearer(
        id: String,
        name: String = id,
        addresses: List<String> = emptyList(),
        contacts: List<EmergencyContact> = emptyList(),
        isSelf: Boolean = false
    ) = Wearer(
        id = id,
        name = name,
        deviceAddresses = addresses,
        contacts = contacts,
        isSelf = isSelf
    )

    // ============================================
    // resolveWearerForDevice
    // ============================================

    @Test
    fun `the bound wearer wins over the selected one`() {
        val baba = wearer("baba", addresses = listOf("AA:BB:CC:DD:EE:01"))
        val ma = wearer("ma", addresses = listOf("AA:BB:CC:DD:EE:02"))

        val resolved = resolveWearerForDevice(
            wearers = listOf(baba, ma),
            address = "AA:BB:CC:DD:EE:02",
            selectedWearerId = "baba"
        )

        assertEquals("ma", resolved?.id)
    }

    @Test
    fun `address matching ignores case`() {
        val baba = wearer("baba", addresses = listOf("aa:bb:cc:dd:ee:01"))
        val resolved = resolveWearerForDevice(listOf(baba, wearer("ma")), "AA:BB:CC:DD:EE:01")
        assertEquals("baba", resolved?.id)
    }

    @Test
    fun `an unbound address falls back to the selected wearer`() {
        val resolved = resolveWearerForDevice(
            wearers = listOf(wearer("baba"), wearer("ma")),
            address = "AA:BB:CC:DD:EE:09",
            selectedWearerId = "ma"
        )
        assertEquals("ma", resolved?.id)
    }

    @Test
    fun `no address and no selection falls back to the first wearer`() {
        val resolved = resolveWearerForDevice(listOf(wearer("baba"), wearer("ma")), "")
        assertEquals("baba", resolved?.id)
    }

    @Test
    fun `a selection naming somebody who has left falls back to the first`() {
        val resolved = resolveWearerForDevice(
            wearers = listOf(wearer("baba"), wearer("ma")),
            address = null,
            selectedWearerId = "somebody-removed"
        )
        assertEquals("baba", resolved?.id)
    }

    @Test
    fun `an empty list resolves to nobody rather than inventing a person`() {
        assertNull(resolveWearerForDevice(emptyList(), "AA:BB:CC:DD:EE:01", "baba"))
    }

    // ============================================
    // contactsFor
    // ============================================

    private fun contact(name: String, phone: String, primary: Boolean = false) =
        EmergencyContact(name = name, phone = phone, isPrimary = primary)

    @Test
    fun `a wearer with no contacts of their own returns the global list unchanged`() {
        val settings = SafetySettings(
            emergencyContacts = listOf(contact("Didi", "9830011223"))
        )
        assertEquals(settings.emergencyContacts, settings.contactsFor(wearer("baba")))
        assertEquals(settings.emergencyContacts, settings.contactsFor(null))
    }

    @Test
    fun `per-wearer contacts are appended after the global ones`() {
        val settings = SafetySettings(emergencyContacts = listOf(contact("Didi", "9830011223")))
        val baba = wearer("baba", contacts = listOf(contact("Neighbour", "9000000001")))

        val merged = settings.contactsFor(baba)

        assertEquals(listOf("Didi", "Neighbour"), merged.map { it.name })
    }

    @Test
    fun `the same number written two ways is one person`() {
        val settings = SafetySettings(emergencyContacts = listOf(contact("Didi", "9830011223")))
        // The same phone, stored the way a contact picker hands it over.
        val baba = wearer("baba", contacts = listOf(contact("Didi (mobile)", "+91 98300 11223")))

        val merged = settings.contactsFor(baba)

        assertEquals(1, merged.size)
        assertEquals("Didi", merged.single().name)
    }

    @Test
    fun `a blank number never merges with another blank number`() {
        val settings = SafetySettings(emergencyContacts = emptyList())
        val baba = wearer(
            "baba",
            contacts = listOf(contact("No number", ""), contact("Also none", ""))
        )
        // Neither is dialable, so neither is added: a blank key must not become
        // the entry that swallows every other blank one.
        assertTrue(settings.contactsFor(baba).isEmpty())
    }

    // ============================================
    // applyRoleFork
    // ============================================

    @Test
    fun `becoming a companion makes the first wearer self and names them`() {
        val forked = applyRoleFork(
            wearers = listOf(wearer("w1", name = ""), wearer("w2", name = "Ma")),
            role = UserRole.COMPANION,
            ownerName = "  Riya  "
        )

        assertTrue(forked[0].isSelf)
        assertEquals("Riya", forked[0].name)
        assertFalse(forked[1].isSelf)
    }

    @Test
    fun `becoming a companion keeps every wearer that already existed`() {
        val forked = applyRoleFork(
            wearers = listOf(wearer("w1"), wearer("w2"), wearer("w3")),
            role = UserRole.COMPANION,
            ownerName = "Riya"
        )
        // Deleting the extras would delete their medical IDs. Nobody's
        // allergies are worth a tidy invariant.
        assertEquals(listOf("w1", "w2", "w3"), forked.map { it.id })
    }

    @Test
    fun `a guardian owns no wearer marked self`() {
        val forked = applyRoleFork(
            wearers = listOf(wearer("w1", isSelf = true), wearer("w2")),
            role = UserRole.GUARDIAN,
            ownerName = "Riya"
        )
        assertTrue(forked.none { it.isSelf })
    }

    @Test
    fun `an existing name survives the companion fork`() {
        val forked = applyRoleFork(
            listOf(wearer("w1", name = "Baba")), UserRole.COMPANION, ownerName = "Riya"
        )
        assertEquals("Baba", forked.single().name)
    }

    // ============================================
    // The wearer DTO
    // ============================================

    @Test
    fun `a wearer survives a JSON round trip intact`() {
        val original = Wearer(
            id = "baba",
            name = "Baba",
            avatarId = "avatar-07",
            medicalId = MedicalId(bloodType = "B+", allergies = "Penicillin"),
            activeMode = PersonaMode.ELDERLY,
            iconType = DeviceIconType.CANE,
            deviceAddresses = listOf("AA:BB:CC:DD:EE:01"),
            contacts = listOf(contact("Didi", "9830011223", primary = true)),
            isSelf = false
        )

        val decoded = gson.fromJson(gson.toJson(original.toDto()), WearerDto::class.java).toDomain()

        assertEquals(original, decoded)
    }

    @Test
    fun `a wearer carries no secret that belongs to the phone or the hardware`() {
        // The PIN guards this phone, the allowlist and the SIM number are facts
        // about hardware, and a wearer record is the thing most likely to be
        // handed to somebody else. Enforced structurally: there is no field.
        val fields = WearerDto::class.java.declaredFields.map { it.name }
        assertFalse(fields.any { it.contains("pin", ignoreCase = true) })
        assertFalse(fields.any { it.contains("allowlist", ignoreCase = true) })
        assertFalse(fields.any { it.contains("phoneNumber", ignoreCase = true) })
    }

    @Test
    fun `a stored wearer with no id gets one rather than becoming unaddressable`() {
        val decoded = gson.fromJson("""{"name":"Baba"}""", WearerDto::class.java).toDomain()
        assertTrue(decoded.id.isNotBlank())
        assertEquals("Baba", decoded.name)
    }

    @Test
    fun `a stored mode no build knows falls back the way the firmware does`() {
        val decoded = gson
            .fromJson("""{"id":"baba","activeMode":"TELEPORT"}""", WearerDto::class.java)
            .toDomain()
        assertEquals(PersonaMode.BACKPACK, decoded.activeMode)
    }

    // ============================================
    // The v2 to v3 migration
    // ============================================

    @Test
    fun `the migration turns a v2 install into exactly one wearer`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.PROFILE] = gson.toJson(
            ProfileSnapshot(
                medicalId = MedicalId(bloodType = "O-"),
                deviceSettings = DeviceSettings(
                    wearerName = "Baba",
                    wearerAvatarId = "avatar-03",
                    iconType = DeviceIconType.CANE
                ),
                role = UserRole.GUARDIAN,
                activeMode = PersonaMode.ELDERLY
            ).toDto()
        )
        prefs[PrefsKeys.SAFETY] = gson.toJson(
            SafetySettings(emergencyContacts = listOf(contact("Didi", "9830011223"))).toDto()
        )

        migrateV2ToV3(prefs, gson)

        val wearers = decodeWearers(prefs, gson)
        assertEquals(1, wearers.size)
        val only = wearers.single()
        assertEquals(PRIMARY_WEARER_ID, only.id)
        assertEquals("Baba", only.name)
        assertEquals("avatar-03", only.avatarId)
        assertEquals("O-", only.medicalId.bloodType)
        assertEquals(PersonaMode.ELDERLY, only.activeMode)
        assertEquals(DeviceIconType.CANE, only.iconType)
        assertEquals(listOf("Didi"), only.contacts.map { it.name })
        assertFalse(only.isSelf)
        // A pairing is not a binding. See synthesiseWearerFromLegacy.
        assertTrue(only.deviceAddresses.isEmpty())
    }

    @Test
    fun `the migration marks a companion install as self`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.PROFILE] = gson.toJson(
            ProfileSnapshot(role = UserRole.COMPANION).toDto()
        )

        migrateV2ToV3(prefs, gson)

        assertTrue(decodeWearers(prefs, gson).single().isSelf)
    }

    @Test
    fun `the migration keeps every legacy key it read`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.PROFILE] = gson.toJson(
            ProfileSnapshot(deviceSettings = DeviceSettings(wearerName = "Baba")).toDto()
        )

        migrateV2ToV3(prefs, gson)

        // A copy, not a move: a downgraded build must still find its profile.
        assertNotNull(prefs[PrefsKeys.PROFILE])
        assertTrue(prefs[PrefsKeys.PROFILE]!!.contains("Baba"))
    }

    @Test
    fun `running the migration twice does not duplicate the person`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.PROFILE] = gson.toJson(
            ProfileSnapshot(deviceSettings = DeviceSettings(wearerName = "Baba")).toDto()
        )

        migrateV2ToV3(prefs, gson)
        val first = prefs[PrefsKeys.WEARERS]
        migrateV2ToV3(prefs, gson)

        assertEquals(first, prefs[PrefsKeys.WEARERS])
        assertEquals(1, decodeWearers(prefs, gson).size)
    }

    @Test
    fun `a v1 install migrates through both steps`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.ACTIVE_MODE] = "ELDERLY"

        migrateV1ToV2(prefs, gson)
        migrateV2ToV3(prefs, gson)

        assertEquals(PersonaMode.ELDERLY, decodeWearers(prefs, gson).single().activeMode)
    }

    // ============================================
    // The synthesis and the mirror
    // ============================================

    @Test
    fun `an unmigrated store still reads as exactly one wearer with a stable id`() {
        val prefs = mutablePreferencesOf()
        prefs[PrefsKeys.PROFILE] = gson.toJson(
            ProfileSnapshot(deviceSettings = DeviceSettings(wearerName = "Baba")).toDto()
        )

        val first = decodeWearers(prefs, gson)
        val second = decodeWearers(prefs, gson)

        assertEquals(1, first.size)
        // A fresh UUID per emission would make the wearer unselectable and
        // unbindable, and would not match what the migration later writes.
        assertEquals(PRIMARY_WEARER_ID, first.single().id)
        assertEquals(first, second)
    }

    @Test
    fun `an empty store still reads as one wearer`() {
        assertEquals(1, decodeWearers(mutablePreferencesOf(), gson).size)
    }

    @Test
    fun `writing wearer one mirrors the name into the legacy profile field`() {
        val prefs = mutablePreferencesOf()
        migrateV2ToV3(prefs, gson)

        writeWearers(
            prefs, gson,
            listOf(
                decodeWearers(prefs, gson).single().copy(
                    name = "Baba",
                    avatarId = "avatar-11",
                    iconType = DeviceIconType.CANE,
                    activeMode = PersonaMode.ELDERLY,
                    medicalId = MedicalId(bloodType = "A+")
                )
            )
        )

        // The roughly twenty screens still reading DeviceSettings.wearerName
        // keep working because of this line, and only because of it.
        val profile = gson
            .fromJson(prefs[PrefsKeys.PROFILE], com.safeshade.data.local.ProfileDto::class.java)
            .toDomain()
        assertEquals("Baba", profile.deviceSettings.wearerName)
        assertEquals("avatar-11", profile.deviceSettings.wearerAvatarId)
        assertEquals(DeviceIconType.CANE, profile.deviceSettings.iconType)
        assertEquals(PersonaMode.ELDERLY, profile.activeMode)
        assertEquals("A+", profile.medicalId.bloodType)
        assertEquals(PersonaMode.ELDERLY.wireName, prefs[PrefsKeys.ACTIVE_MODE])
    }

    @Test
    fun `writing the profile mirrors back into wearer one`() {
        val prefs = mutablePreferencesOf()
        migrateV2ToV3(prefs, gson)

        mirrorProfileIntoPrimaryWearer(
            prefs, gson,
            ProfileSnapshot(
                medicalId = MedicalId(bloodType = "AB-"),
                deviceSettings = DeviceSettings(wearerName = "Ma", wearerAvatarId = "avatar-02")
            )
        )

        val only = decodeWearers(prefs, gson).single()
        assertEquals("Ma", only.name)
        assertEquals("avatar-02", only.avatarId)
        assertEquals("AB-", only.medicalId.bloodType)
    }

    @Test
    fun `a second wearer is untouched by a profile write`() {
        val prefs = mutablePreferencesOf()
        writeWearers(prefs, gson, listOf(wearer("w1", name = "Baba"), wearer("w2", name = "Ma")))

        mirrorProfileIntoPrimaryWearer(
            prefs, gson,
            ProfileSnapshot(deviceSettings = DeviceSettings(wearerName = "Baba Sen"))
        )

        val wearers = decodeWearers(prefs, gson)
        assertEquals("Baba Sen", wearers[0].name)
        assertEquals("Ma", wearers[1].name)
    }

    @Test
    fun `removing the selected wearer clears the stale selection`() {
        val prefs = mutablePreferencesOf()
        writeWearers(prefs, gson, listOf(wearer("w1"), wearer("w2")))
        prefs[PrefsKeys.SELECTED_WEARER] = "w2"

        writeWearers(prefs, gson, listOf(wearer("w1")))

        assertNull(prefs[PrefsKeys.SELECTED_WEARER])
    }

    @Test
    fun `the wearer list is capped on write`() {
        val prefs = mutablePreferencesOf()
        writeWearers(prefs, gson, (1..40).map { wearer("w$it") })
        assertEquals(PrefsLimits.WEARERS, decodeWearers(prefs, gson).size)
    }
}
