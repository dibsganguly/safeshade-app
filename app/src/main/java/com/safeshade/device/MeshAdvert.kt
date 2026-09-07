package com.safeshade.device

/**
 * What one SafeShade says about itself in an advertisement, so another phone
 * can see it without connecting.
 *
 * The wire form is the one named in `DeviceCapabilities.awaitingFirmware`:
 * `<deviceId>,<role>,<batteryPercent>,<lastFixAge>`, comma-delimited like every
 * other payload in [DeviceProtocol] and with the same lack of escaping — which
 * is why [MeshAdvert.deviceId] goes through `DeviceProtocol.clean` on the way
 * out.
 *
 * ### Why the numbers are nullable
 *
 * An advertisement is a broadcast from a device that may know less about itself
 * than the format has room for: a wearable that has never had a GPS fix has no
 * fix age, and one whose battery gauge has not settled has no percentage. Zero
 * would be a lie in both cases — "located just now" and "flat" — and both lies
 * would be acted on. Null means the field was not reported, and a reader draws
 * a dash.
 *
 * Parsing is deliberately tolerant, because a scanner sees whatever is in the
 * air, including frames from a firmware revision this build has never met. A
 * field that is missing or not a number becomes null, an unrecognised role
 * becomes [MeshRole.UNKNOWN], and only a frame with no usable identity at all
 * is rejected outright.
 *
 * Pure Kotlin with no Android imports, so it is unit testable away from a
 * scan callback.
 */
data class MeshAdvert(
    val deviceId: String,
    val role: MeshRole,
    /** 0..100, or null when the device reported no level. */
    val batteryPercent: Int? = null,
    /** Seconds since the device's last position fix, or null when it has never had one. */
    val lastFixAgeSeconds: Int? = null
)

/**
 * What a device is doing in the mesh.
 *
 * [UNKNOWN] is a real member rather than a null: a device whose role this build
 * does not recognise is still there, still nearby, and still worth showing.
 * Dropping it would make a newer wearable invisible to an older phone.
 */
enum class MeshRole(val wireName: String) {
    /** The device is on a person being looked after. */
    WEARER("wearer"),

    /** The device is passing other devices' traffic along. */
    RELAY("relay"),

    /** Present and advertising, role not recognised by this build. */
    UNKNOWN("unknown");

    companion object {
        /** Case-insensitive. Anything unrecognised is [UNKNOWN], never null. */
        fun fromWire(wire: String?): MeshRole {
            val token = wire?.trim()?.lowercase().orEmpty()
            return entries.firstOrNull { it.wireName == token } ?: UNKNOWN
        }
    }
}

object MeshAdvertCodec {

    /** Advertisement payloads are tiny; the id is capped so the frame fits one. */
    private const val DEVICE_ID_MAX = 16

    /** A field the device has no value for. Positionally present, deliberately empty. */
    private const val ABSENT = ""

    /**
     * `<deviceId>,<role>,<batteryPercent>,<lastFixAge>`.
     *
     * A null number is written as an empty field rather than omitted: the
     * parser on the other side is positional, so dropping a field would shift
     * the one after it — the same rule that governs every payload in
     * [DeviceProtocol].
     */
    fun encode(advert: MeshAdvert): String = listOf(
        DeviceProtocol.clean(advert.deviceId, DEVICE_ID_MAX),
        advert.role.wireName,
        advert.batteryPercent?.coerceIn(0, 100)?.toString() ?: ABSENT,
        advert.lastFixAgeSeconds?.coerceAtLeast(0)?.toString() ?: ABSENT
    ).joinToString(",")

    /**
     * Inverse of [encode], as tolerant as the class note describes.
     *
     * Null only for a frame that cannot identify anything: fewer than two
     * fields, or a blank device id. An id is the one field with no sensible
     * fallback — an advertisement from nobody in particular cannot be shown,
     * de-duplicated, or connected to.
     */
    fun parse(payload: String): MeshAdvert? {
        val parts = payload.split(",")
        if (parts.size < 2) return null
        val deviceId = parts[0].trim()
        if (deviceId.isEmpty()) return null

        return MeshAdvert(
            deviceId = deviceId,
            role = MeshRole.fromWire(parts[1]),
            batteryPercent = parts.getOrNull(2)?.trim()?.toIntOrNull()?.takeIf { it in 0..100 },
            lastFixAgeSeconds = parts.getOrNull(3)?.trim()?.toIntOrNull()?.takeIf { it >= 0 }
        )
    }

    /**
     * The same comma text, read out of a BLE manufacturer-data blob as UTF-8.
     *
     * The bytes are expected to be exactly the payload: Android's
     * `ScanRecord.getManufacturerSpecificData(companyId)` already strips the
     * two-byte company identifier before handing the array over, so nothing is
     * skipped here. Passing a raw record including that prefix would put two
     * stray bytes on the front of the device id.
     *
     * Empty bytes, or text that [parse] rejects, yield null — a scanner sees
     * frames from every vendor in the room and must not treat a stranger's
     * advertisement as a SafeShade.
     */
    fun parseManufacturerData(bytes: ByteArray): MeshAdvert? {
        if (bytes.isEmpty()) return null
        return parse(String(bytes, Charsets.UTF_8))
    }
}
