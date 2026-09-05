package com.safeshade.device

import com.safeshade.data.GeofenceZone
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings

/**
 * Every byte the app sends to the wearable is built here, and nowhere else.
 *
 * Two properties of the firmware parser make this worth isolating:
 *
 *  1. **There is no escaping.** Payloads are split on `,` positionally. A
 *     single comma typed into a free-text field shifts every later field by
 *     one, so a guardian writing "Penicillin, Peanuts" into Allergies would
 *     silently push their age into the Conditions slot. Every free-text field
 *     therefore goes through [clean].
 *  2. **Over-long writes are truncated by the ATT layer, not rejected.** The
 *     11-field Medical ID payload can exceed the negotiated MTU with realistic
 *     content, and the loss is silent. [health] budgets against the MTU and
 *     degrades in a fixed, documented order instead.
 *
 * Being pure Kotlin with no Android imports, this file is directly unit
 * testable — which is the only way to be confident about a format whose
 * failure mode is silence.
 */
object DeviceProtocol {

    /** ATT overhead: 3 bytes of opcode + handle on every write. */
    private const val ATT_OVERHEAD = 3

    /**
     * Strips the delimiters the firmware parser cannot survive, collapses
     * whitespace, and caps length.
     *
     * Newlines matter as much as commas here: the firmware reads a line at a
     * time in some paths, so an embedded newline truncates just as badly.
     */
    fun clean(value: String, maxLength: Int): String =
        value.replace(',', ' ')
            .replace('\n', ' ')
            .replace('\r', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(maxLength)

    // ============================================
    // HEALTH_CHAR - 11 fields
    // ============================================

    /**
     * The Medical ID payload, in the firmware's exact positional order:
     *
     * `bloodType,emergencyContact,contactName,allergies,age,conditions,
     *  medications,secondaryContactName,secondaryContact,organDonor,notes`
     *
     * `organDonor` serialises as `1`/`0`; the firmware accepts `1` or `true`
     * for yes and treats anything else as no.
     *
     * When the payload will not fit the negotiated MTU it is degraded in a
     * fixed order — notes, then medications, then conditions, then allergies —
     * chosen so that the fields a first responder needs first (blood type,
     * contacts, allergies) are the last to be shortened. Field *count* is never
     * reduced, because the parser is positional.
     */
    fun health(medicalId: MedicalId, mtu: Int = 247): String {
        val budget = (mtu - ATT_OVERHEAD).coerceIn(20, 512)

        val fields = mutableListOf(
            clean(medicalId.bloodType, 8),
            clean(medicalId.emergencyContact, 20),
            clean(medicalId.contactName, 24),
            clean(medicalId.allergies, 40),
            medicalId.age.coerceIn(0, 130).toString(),
            clean(medicalId.conditions, 40),
            clean(medicalId.medications, 40),
            clean(medicalId.secondaryContactName, 24),
            clean(medicalId.secondaryContact, 20),
            if (medicalId.organDonor) "1" else "0",
            clean(medicalId.notes, 40)
        )

        // Degrade in priority order until it fits. Indices: notes, medications,
        // conditions, allergies.
        for (index in listOf(10, 6, 5, 3)) {
            if (fields.joinToString(",").length <= budget) break
            fields[index] = fields[index].take(fields[index].length / 2)
        }

        // Last resort: hard truncate. Still 11 fields as long as the tail
        // survives, and the leading responder-critical fields are intact.
        return fields.joinToString(",").take(budget)
    }

    // ============================================
    // SETTINGS_CHAR - 5 fields
    // ============================================

    /**
     * `fallSensitivity,sosVolume,autoCall,parentalControls,smsFallback`
     *
     * Sensitivity rides as the enum ordinal because the firmware clamps to
     * 0..2 in that same order — LOW, MEDIUM, HIGH. Reordering [com.safeshade.data.FallSensitivity]
     * would silently change what the device does, which is why that enum
     * carries a do-not-reorder note.
     */
    fun settings(settings: SafetySettings): String = listOf(
        settings.fallSensitivity.ordinal,
        (settings.sosVolumeLevel * 100).toInt().coerceIn(0, 100),
        if (settings.autoCallEmergency) 1 else 0,
        if (settings.parentalControlsEnabled) 1 else 0,
        if (settings.smsFallbackEnabled) 1 else 0
    ).joinToString(",")

    // ============================================
    // WEATHER_CHAR - 11 fields
    // ============================================

    fun weather(
        rainChance: Int,
        condition: String,
        uvIndex: Float,
        humidity: Float,
        lat: Double,
        lon: Double,
        locationName: String,
        locality: String,
        altitude: Int,
        hour: Int,
        minute: Int
    ): String = listOf(
        rainChance.coerceIn(0, 100).toString(),
        clean(condition, 20),
        "%.1f".format(uvIndex),
        "%.0f".format(humidity),
        "%.5f".format(lat),
        "%.5f".format(lon),
        clean(locationName, 24),
        clean(locality, 24),
        altitude.toString(),
        hour.coerceIn(0, 23).toString(),
        minute.coerceIn(0, 59).toString()
    ).joinToString(",")

    // ============================================
    // EXT_CHAR tagged commands
    // ============================================

    /** Firmware truncates the device name to 24 characters. */
    fun deviceName(name: String): String = clean(name, 24)

    fun mode(mode: PersonaMode): String = mode.wireName

    /** `"HH:MM"`, or empty to clear the reminder. */
    fun medicationTime(hour: Int?, minute: Int?): String =
        if (hour == null || minute == null) "" else "%02d:%02d".format(hour, minute)

    /** Seconds. Zero disables the check-in. */
    fun checkInInterval(seconds: Int): String = seconds.coerceIn(0, 86_400).toString()

    /** `"<startHour>:<endHour>"`, or empty to disable Do Not Disturb hours. */
    fun quietHours(startHour: Int?, endHour: Int?): String =
        if (startHour == null || endHour == null) "" else "$startHour:$endHour"

    /**
     * `"<zoneName>:<IN|OUT>"`.
     *
     * The firmware splits on the *last* colon, so a zone name containing a
     * colon still parses. The name is cleaned anyway to keep it short enough
     * for the on-device display.
     */
    fun geofence(zone: GeofenceZone, isInside: Boolean): String =
        "${clean(zone.name, 20)}:${if (isInside) "IN" else "OUT"}"

    /** `"<lat>:<lon>:<label>"`, or empty to stop navigation. */
    fun navigation(lat: Double?, lon: Double?, label: String): String =
        if (lat == null || lon == null) "" else "%.5f:%.5f:%s".format(lat, lon, clean(label, 20))

    /** Comma-joined, maximum 8 entries. Empty means allow every sender. */
    fun smsAllowlist(numbers: List<String>): String =
        numbers.take(8).joinToString(",") { clean(it, 16) }

    // ============================================
    // Literal commands
    // ============================================

    /**
     * Written to WEATHER_CHAR as a bare literal, not as an EXT tag.
     *
     * The firmware special-cases this string before its weather parser runs
     * and returns early, so it is never acknowledged. See [DeviceLink.ringDevice]
     * for what it actually does on the wearable, which is not what the name
     * suggests.
     */
    const val CMD_FIND = "CMD_FIND"
}
