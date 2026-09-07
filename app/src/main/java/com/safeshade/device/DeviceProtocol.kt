package com.safeshade.device

import com.safeshade.data.GeofenceZone
import com.safeshade.data.LiveSensorData
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.data.VitalsSource

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
 *
 * The inbound direction lives here too, for the same reason: [parseTelemetry]
 * is the one place that reads a TELEMETRY notification, so the rule that a
 * field the app cannot believe becomes *no reading* is written once and
 * tested, rather than re-improvised inside a GATT callback.
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
    // EXT ERS - nearest emergency services cache
    // ============================================

    /**
     * One cached emergency-services entry: `kind` is `'H'` hospital, `'P'`
     * police, `'F'` fire, `'M'` pharmacy. `phone` is nullable — not every
     * result the app fetches carries a number.
     */
    data class ErsEntry(val kind: Char, val name: String, val phone: String?, val distanceM: Int)

    /** Deterministic on-device ordering: hospital, police, fire, pharmacy. */
    private val ERS_KIND_ORDER = listOf('H', 'P', 'F', 'M')

    private const val ERS_MAX_ENTRIES = 4

    /**
     * `EXT ERS` write budget.
     *
     * `BleManager` (`DESIRED_MTU = 247`) requests 247 and [health] budgets
     * against `mtu - ATT_OVERHEAD` up to 512 once that is granted. Android is
     * free to grant less, and stacks that refuse renegotiation commonly settle
     * around 185 — the value this file's own tests use for the "generous" MTU
     * case. This payload is fixed at a flat 180 bytes rather than taking an
     * `mtu` parameter like [health] does, so the four cached entries always
     * fit even on a link that never got past that fallback ceiling.
     */
    private const val ERS_BUDGET_BYTES = 180

    /** Strips the `:` and `;` this format uses as delimiters, on top of [clean]. */
    private fun ersClean(value: String, maxLength: Int): String =
        clean(value, maxLength).replace(':', ' ').replace(';', ' ').replace(Regex("\\s+"), " ").trim()

    /**
     * `EXT ERS` payload: up to 4 entries, one per [ErsEntry.kind], joined by
     * `;`; each entry is `kind:name:phoneDigits:distanceMetres` joined by `:`.
     *
     * Colons and semicolons are used instead of commas (unlike [health] or
     * [weather]) following the same convention as [geofence] and [navigation]:
     * a structured, non-comma format that survives the firmware's positional
     * comma split at the outer `EXT` layer untouched. `phone` is digits-only;
     * a missing phone renders as an empty field, still positionally present.
     * Entries are sorted into [ERS_KIND_ORDER] regardless of input order, and
     * duplicate kinds collapse to the first occurrence, so the device always
     * sees at most one row per kind in a fixed order. Names are truncated
     * (and, at 4 entries, truncated harder) until the whole payload fits
     * [ERS_BUDGET_BYTES]; a payload that still would not fit is hard-cut as a
     * last resort, exactly as [health] does.
     */
    fun encodeErsPayload(entries: List<ErsEntry>): String {
        val ordered = entries
            .distinctBy { it.kind }
            .sortedBy { ERS_KIND_ORDER.indexOf(it.kind).let { i -> if (i < 0) ERS_KIND_ORDER.size else i } }
            .take(ERS_MAX_ENTRIES)

        fun render(nameLen: Int): String = ordered.joinToString(";") { entry ->
            val phoneDigits = entry.phone.orEmpty().filter { it.isDigit() }
            listOf(
                entry.kind.toString(),
                ersClean(entry.name, nameLen),
                phoneDigits,
                entry.distanceM.coerceIn(0, 999_999).toString()
            ).joinToString(":")
        }

        var nameLen = 32
        var payload = render(nameLen)
        while (payload.toByteArray(Charsets.UTF_8).size > ERS_BUDGET_BYTES && nameLen > 0) {
            nameLen = (nameLen - 4).coerceAtLeast(0)
            payload = render(nameLen)
        }
        return payload.take(ERS_BUDGET_BYTES)
    }

    /**
     * Inverse of [encodeErsPayload], for the round-trip test — the firmware
     * never sends this back, but the format has to decode what it encodes.
     * An entry with too few `:`-separated fields, or a non-numeric distance,
     * is skipped rather than failing the whole parse.
     */
    fun parseErsPayload(payload: String): List<ErsEntry> {
        if (payload.isBlank()) return emptyList()
        return payload.split(";").mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size < 4) return@mapNotNull null
            val kind = parts[0].firstOrNull() ?: return@mapNotNull null
            val distanceM = parts[3].toIntOrNull() ?: return@mapNotNull null
            ErsEntry(kind, parts[1], parts[2].ifEmpty { null }, distanceM)
        }
    }

    // ============================================
    // VOICE chunk path - 8 kHz ADPCM push-to-talk
    // ============================================

    /** One binary-characteristic chunk: header tag, then payload bytes. */
    data class VoiceChunk(val seq: Int, val total: Int, val bytes: ByteArray)

    /** `0x56` = ASCII `'V'`, distinguishing this characteristic's frames from any other binary write. */
    private const val VOICE_TAG: Byte = 0x56

    /** tag(1) + seq(1) + total(1) + length(1). */
    private const val VOICE_HEADER_BYTES = 4

    /**
     * `VOICE` binary frame: `[0x56][seq][total][length][...payload]`.
     *
     * `seq`, `total` and `length` are each a single unsigned byte (0..255),
     * matching a fixed 4-byte header on a binary characteristic rather than
     * the comma-delimited text format the rest of this file uses — there is
     * no free text here to sanitise, only raw ADPCM samples. The ADPCM
     * encoding of [VoiceChunk.bytes] itself is out of scope; this only
     * frames and reassembles whatever bytes it is given.
     */
    fun encodeVoiceChunk(chunk: VoiceChunk): ByteArray {
        require(chunk.seq in 0..255) { "seq must fit in a byte: ${chunk.seq}" }
        require(chunk.total in 1..255) { "total must fit in a byte: ${chunk.total}" }
        require(chunk.bytes.size in 0..255) { "chunk payload must fit in a byte length: ${chunk.bytes.size}" }
        val header = byteArrayOf(VOICE_TAG, chunk.seq.toByte(), chunk.total.toByte(), chunk.bytes.size.toByte())
        return header + chunk.bytes
    }

    /** Inverse of [encodeVoiceChunk]. Returns null on a bad tag or a length mismatch, never throws. */
    fun decodeVoiceChunk(raw: ByteArray): VoiceChunk? {
        if (raw.size < VOICE_HEADER_BYTES || raw[0] != VOICE_TAG) return null
        val seq = raw[1].toInt() and 0xFF
        val total = raw[2].toInt() and 0xFF
        val length = raw[3].toInt() and 0xFF
        val payload = raw.copyOfRange(VOICE_HEADER_BYTES, raw.size)
        if (payload.size != length) return null
        return VoiceChunk(seq, total, payload)
    }

    /**
     * Splits raw ADPCM bytes into [VoiceChunk]s sized to fit one write at
     * `mtu`: payload per chunk is `mtu - ATT_OVERHEAD - VOICE_HEADER_BYTES`,
     * mirroring how [health] budgets against `mtu - ATT_OVERHEAD` for a text
     * write. `total` is capped at 255 to keep it representable in the header's
     * single byte, so a clip long enough to need more chunks than that at the
     * given MTU is truncated to the first 255 — acceptable for a
     * push-to-talk clip, not for a file transfer.
     */
    fun chunkVoice(adpcm: ByteArray, mtu: Int): List<VoiceChunk> {
        if (adpcm.isEmpty()) return emptyList()
        val maxPayload = (mtu - ATT_OVERHEAD - VOICE_HEADER_BYTES).coerceAtLeast(1)
        val total = ((adpcm.size + maxPayload - 1) / maxPayload).coerceIn(1, 255)
        return (0 until total).map { seq ->
            val start = seq * maxPayload
            val end = minOf(start + maxPayload, adpcm.size)
            VoiceChunk(seq, total, adpcm.copyOfRange(start, end))
        }
    }

    /**
     * Reassembles chunks in `seq` order. Null whenever the set is incomplete
     * or inconsistent — a missing `seq`, a duplicate, or chunks disagreeing on
     * `total` — since a partial voice clip is worse than none: it must never
     * play back as if it were the whole message.
     */
    fun reassembleVoice(chunks: List<VoiceChunk>): ByteArray? {
        if (chunks.isEmpty()) return null
        val total = chunks[0].total
        if (chunks.any { it.total != total } || chunks.size != total) return null
        val bySeq = chunks.associateBy { it.seq }
        var result = ByteArray(0)
        for (seq in 0 until total) {
            val chunk = bySeq[seq] ?: return null
            result += chunk.bytes
        }
        return result
    }

    // ============================================
    // TELEMETRY_CHAR notify - inbound
    // ============================================

    /**
     * Field count of the payload the shipped firmware sends:
     * `accelXg,accelYg,accelZg,tempC,lightRaw,batteryPct`.
     */
    private const val TELEMETRY_BASE_FIELDS = 6

    /** What a field carries when the device has the slot but no reading. */
    private const val NO_READING = "-"

    /** Plausible human heart rate, in bpm. Outside this the reading is dropped, not clamped. */
    private val HR_RANGE = 25..250

    /** Plausible blood-oxygen saturation, as a whole percentage. */
    private val SPO2_RANGE = 50..100

    /** Plausible skin temperature in Celsius. Wider and lower than core temperature: skin runs cool. */
    private val SKIN_TEMP_RANGE = 20f..45f

    /**
     * Parses a TELEMETRY notification into [LiveSensorData], or null when the
     * payload is too short to be one.
     *
     * ### The six-field payload must keep parsing exactly as it always did
     *
     * The firmware on every device in the field sends six fields and no more.
     * Fields 7-9 — `hr,spo2,skinTempC` — are the vitals slots this side is
     * ready for, so a six-field payload leaves all three null and
     * [LiveSensorData.vitalsSource] null, which is what a screen renders as a
     * dash. Fields beyond the ninth are ignored rather than treated as an
     * error, so a firmware that appends something new later cannot break the
     * fields that already work.
     *
     * ### A number the app cannot believe is not a reading
     *
     * An empty field or the literal `-` means the device has no reading. So
     * does a value outside human range — heart rate 25..250 bpm, SpO2 50..100
     * %, skin temperature 20..45 °C — and so does a value that is not a number
     * of the expected kind (`72.5` in the integer heart-rate slot yields null).
     * Nothing is clamped *into* range: a sensor reporting 4 bpm is a broken
     * sensor rather than a dying wearer, and on this product a vital shown
     * confidently but wrongly is worse than a dash.
     *
     * [LiveSensorData.vitalsSource] is [VitalsSource.DEVICE] only when at least
     * one vital survives all of that. A payload ending `-,-,-` and one ending
     * in three implausible numbers are both indistinguishable from no vitals,
     * and both leave the source null.
     */
    fun parseTelemetry(value: String): LiveSensorData? {
        val parts = value.split(",")
        if (parts.size < TELEMETRY_BASE_FIELDS) return null

        fun field(index: Int): String? =
            parts.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() && it != NO_READING }

        val heartRate = field(6)?.toIntOrNull()?.takeIf { it in HR_RANGE }
        val spo2 = field(7)?.toIntOrNull()?.takeIf { it in SPO2_RANGE }
        val skinTemp = field(8)?.toFloatOrNull()?.takeIf { it in SKIN_TEMP_RANGE }

        return LiveSensorData(
            accelX = parts[0].toFloatOrNull() ?: 0f,
            accelY = parts[1].toFloatOrNull() ?: 0f,
            accelZ = parts[2].toFloatOrNull() ?: 0f,
            temperature = parts[3].toFloatOrNull() ?: 0f,
            lightLevel = parts[4].toIntOrNull() ?: 0,
            batteryLevel = parts[5].toIntOrNull() ?: 0,
            isRealData = true,
            heartRateBpm = heartRate,
            spo2Percent = spo2,
            skinTempC = skinTemp,
            vitalsSource =
                if (heartRate != null || spo2 != null || skinTemp != null) VitalsSource.DEVICE else null
        )
    }

    /**
     * Builds a TELEMETRY payload.
     *
     * Only the wearable ever sends one of these, so this exists for the
     * round-trip test rather than for dispatch — a format nothing on this side
     * writes still has to decode what it encodes. A null vital is written as
     * [NO_READING] rather than omitted, because the parser is positional and a
     * missing field would shift the ones after it.
     */
    fun encodeTelemetry(
        accelX: Float,
        accelY: Float,
        accelZ: Float,
        temperature: Float,
        lightLevel: Int,
        batteryLevel: Int,
        heartRateBpm: Int? = null,
        spo2Percent: Int? = null,
        skinTempC: Float? = null
    ): String = listOf(
        "%.2f".format(accelX),
        "%.2f".format(accelY),
        "%.2f".format(accelZ),
        "%.1f".format(temperature),
        lightLevel.toString(),
        batteryLevel.toString(),
        heartRateBpm?.toString() ?: NO_READING,
        spo2Percent?.toString() ?: NO_READING,
        skinTempC?.let { "%.1f".format(it) } ?: NO_READING
    ).joinToString(",")

    // ============================================
    // EXT VER - the firmware version over the link
    // ============================================

    /**
     * `EXT VER` carries no payload: the tag alone is the question.
     *
     * `BleManager.sendExtCommand` writes the bare tag when the payload is
     * empty, so this returns `""` deliberately rather than a placeholder that
     * would ride on the wire as `VER:`.
     */
    fun versionQuery(): String = ""

    /**
     * Reads the semver out of a `VER:<semver>` acknowledgement tag, or null.
     *
     * The tag arrives here already stripped of its wire prefix: `BleManager`
     * does `value.removePrefix("ACK:")`, which removes only those four leading
     * characters and so leaves `"VER:1.2.3"` intact — the colon payload
     * survives. `BleManager.awaitAck` matches `it == tag || it.startsWith("$tag:")`,
     * so `awaitAck("VER")` does fire on a version reply; it returns a Boolean
     * only, so the version itself has to be read off the `acks` flow.
     *
     * A reply is accepted only when it looks like a version — a leading digit
     * and at least one dot. That guard exists because of the firmware fact this
     * whole path is shaped around: the device acknowledges *every* EXT tag,
     * including ones it does not know, so something like `VER:OK` is a
     * plausible thing to receive from a build that has no version to give, and
     * turning it into a version would let a non-answer masquerade as an answer.
     * A bare `VER` with no colon is exactly that non-answer — see
     * [OtaProtocol.VersionReply].
     */
    fun parseVersionAck(ackTag: String): String? {
        val trimmed = ackTag.trim()
        if (!trimmed.startsWith("VER:")) return null
        val semver = trimmed.removePrefix("VER:").trim()
        if (semver.isEmpty()) return null
        if (!semver.first().isDigit() || !semver.contains('.')) return null
        return semver
    }

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
