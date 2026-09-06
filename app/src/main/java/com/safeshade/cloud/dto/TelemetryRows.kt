package com.safeshade.cloud.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The wire shape of the telemetry, fleet and integration tables.
 *
 * The nullable-with-default rule and the string-typed timestamps are explained
 * in `CircleRows.kt`.
 */

/**
 * One reading from the health sensors.
 *
 * Nothing writes this yet, and that is stated deliberately rather than left to
 * be discovered. The deck marks heart rate, SpO2 and temperature as *Planned*;
 * `HEALTH_CHAR` on the wearable carries the Medical ID, not vitals, and the
 * device's telemetry today is accelerometer, battery and RSSI. The table exists
 * so the schema does not have to be migrated the week the sensor lands.
 *
 * **Do not read this as "vitals ship".** The handoff's standing rule is that a
 * complete, correct, plausible API that nothing calls is a trap — the same one
 * that made `DeviceRepository.setNavTarget` look like a shipped feature for two
 * releases. Any UI built on this must carry `StubMark` until a real sample has
 * been seen.
 *
 * Every measurement is nullable independently because one sample carries
 * whichever sensors were readable at that moment; a wrist that lost contact
 * gives a temperature and no SpO2, and zero is a real reading, not "absent".
 */
@Serializable
data class VitalsSampleRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("wearer_id") val wearerId: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("measured_at") val measuredAt: String? = null,
    @SerialName("heart_rate_bpm") val heartRateBpm: Int? = null,
    @SerialName("spo2_percent") val spo2Percent: Int? = null,
    @SerialName("body_temp_c") val bodyTempC: Double? = null,
    /** Ambient sound level, for the loud-environment warning. */
    @SerialName("ambient_db") val ambientDb: Double? = null,
    @SerialName("battery_percent") val batteryPercent: Int? = null,
    /** How the sample was obtained, so a stubbed sample can never be mistaken
     *  for a measured one. `device` | `phone` | `simulated`. */
    @SerialName("source") val source: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A published firmware image.
 *
 * The one **publicly readable** table in the schema, and intentionally so: a
 * device checking whether it is out of date should not need a session, and the
 * firmware bucket is public for the same reason. Nothing here is secret — the
 * images are signed, or they are not trustworthy whether or not the list is
 * private.
 *
 * Not circle-scoped: a release is a property of the product line, not of one
 * family.
 */
@Serializable
data class FirmwareReleaseRow(
    @SerialName("id") val id: String? = null,
    /** `s1` | `5g` | `spark`. */
    @SerialName("model") val model: String? = null,
    @SerialName("version") val version: String? = null,
    /** Monotonic, so "newer" is a comparison and not a string parse. */
    @SerialName("version_code") val versionCode: Int? = null,
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("sha256") val sha256: String? = null,
    @SerialName("byte_size") val byteSize: Long? = null,
    @SerialName("release_notes") val releaseNotes: String? = null,
    /** True for a fix nobody should be able to defer. */
    @SerialName("mandatory") val mandatory: Boolean? = null,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * A smart-home action to fire on a SafeShade event.
 *
 * Modelled as an outbound webhook plus a trigger rather than as a per-vendor
 * integration, because the deck's promise is "smart home integration, Matter",
 * and every one of those platforms — Home Assistant, SmartThings, IFTTT, a
 * Matter bridge — can be reached by a POST. A vendor-shaped table would need a
 * migration per vendor.
 *
 * [secret] is a shared secret used to sign the outbound call, and it is stored
 * here only because the row is protected by row-level security and readable
 * solely by the circle that created it. It must never be logged.
 */
@Serializable
data class SmartHomeHookRow(
    @SerialName("id") val id: String? = null,
    @SerialName("circle_id") val circleId: String? = null,
    @SerialName("name") val name: String? = null,
    /** `fall` | `sos` | `zone_exit` | `zone_enter` | `low_battery` | `check_in_missed`. */
    @SerialName("trigger") val trigger: String? = null,
    /** `webhook` | `home_assistant` | `ifttt` | `matter`. */
    @SerialName("provider") val provider: String? = null,
    @SerialName("endpoint_url") val endpointUrl: String? = null,
    @SerialName("secret") val secret: String? = null,
    @SerialName("enabled") val enabled: Boolean? = null,
    @SerialName("last_fired_at") val lastFiredAt: String? = null,
    @SerialName("last_error") val lastError: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

/**
 * Somebody's phone saw a device over BLE, somewhere.
 *
 * This is the community mesh relay and the safety heat map, in one table. Two
 * things about it are load-bearing:
 *
 *  1. **A sighting is written by a phone that is not in the device's circle.**
 *     That is the entire feature: a lost wearable is found because a stranger's
 *     SafeShade app walked past it. So the insert policy on this table is the
 *     only one in the schema that is not `is_circle_member`, and the row
 *     carries no circle. Reading a sighting back *is* circle-scoped, through a
 *     lookup on the device.
 *  2. **[reporterId] is nullable and should usually stay null.** The person who
 *     walked past does not need to be identified for the sighting to be useful,
 *     and identifying them turns a helpful feature into a tracking network.
 *
 * The heat map is a materialized view over this table with a `having count(*)
 * >= 5` floor, so no cell can ever be traced back to one person's route.
 */
@Serializable
data class DeviceSightingRow(
    @SerialName("id") val id: String? = null,
    @SerialName("device_id") val deviceId: String? = null,
    /** The BLE address seen, when the device is not yet known to the cloud. */
    @SerialName("ble_address") val bleAddress: String? = null,
    @SerialName("seen_at") val seenAt: String? = null,
    @SerialName("lat") val lat: Double? = null,
    @SerialName("lon") val lon: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Double? = null,
    @SerialName("rssi") val rssi: Int? = null,
    /** Null by default, and best left that way. See the class KDoc. */
    @SerialName("reporter_id") val reporterId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
