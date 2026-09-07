package com.safeshade.cloud

import com.safeshade.cloud.dto.CloudTables
import com.safeshade.cloud.dto.DeviceSightingRow
import com.safeshade.cloud.dto.toIsoOrNull
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Rows off the wire, decoded leniently.
 *
 * `ignoreUnknownKeys` because a migration that adds a column must not make
 * every unupdated phone throw on a read it was doing perfectly well yesterday.
 */
private val sightingJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

/**
 * "Somebody's phone saw this wearable, here, just now."
 *
 * ### The one table that is not circle-scoped, and why that is the feature
 *
 * A lost wearable is found because a stranger's SafeShade app walked past it.
 * So `device_sightings` is the only table in the schema whose insert policy is
 * `auth.uid() is not null` rather than `is_circle_member` - any signed-in phone
 * may report a sighting of any address, and the row carries no circle.
 *
 * Reading one back **is** circle-scoped, and through a join the client cannot
 * see: the select policy looks the sighting's `device_id` up in `devices` and
 * checks membership of that device's circle. Two consequences follow, and both
 * are properties of the schema rather than of this class:
 *
 *  1. [lastSeen] can never return a sighting whose `device_id` is null, because
 *     the policy's `exists (select 1 from devices ...)` has nothing to join to.
 *     A sighting of an address nobody has registered is written, counted in the
 *     heat map, and invisible until that device exists - which is correct: an
 *     unregistered address belongs to nobody, so there is no one it could be
 *     shown to.
 *  2. A reporter cannot read back what they just reported unless the device is
 *     in their own circle. That is not a bug either. The passer-by is doing the
 *     owner a favour, not acquiring a right to follow the wearer.
 *
 * ### [reporterId] is left null, always
 *
 * The person who walked past does not need to be identified for the sighting to
 * be useful, and identifying them turns a helpful feature into a tracking
 * network. There is no parameter here to set it, which is deliberate: an
 * optional one would eventually be passed.
 *
 * ### Why the rate limit is in memory
 *
 * A phone walking beside a wearable sees it on every scan window - several
 * times a minute, for as long as the walk lasts. Every one of those is a row,
 * an insert and a battery cost, and none of them says anything the first one
 * did not. So one report per address per [WINDOW_MS], held in a map that dies
 * with the process.
 *
 * In memory rather than on disk on purpose. The cost of forgetting on a restart
 * is one extra row; the cost of a DataStore write per BLE scan result is a file
 * rewritten several times a minute, forever. And a persisted limiter would go
 * on suppressing reports of a wearable somebody is actively searching for,
 * across the app restart they performed precisely because they were searching.
 *
 * @param deviceIdFor the app's own map from a BLE address to the `devices` row
 *   id, when this phone happens to know it - which it does only for its own
 *   circle's wearables. Returns null for a stranger's device, and null is the
 *   normal answer.
 */
class SightingsCloud(
    private val client: CloudClient,
    private val deviceIdFor: (String) -> String? = { null },
    private val now: () -> Long = { System.currentTimeMillis() }
) {

    /** Address, lower-cased, to when it was last reported. See the class KDoc. */
    private val lastReportedAt = mutableMapOf<String, Long>()

    /**
     * Reports one sighting, unless this phone reported the same address inside
     * the last ten minutes.
     *
     * @return [CloudResult.Ok] with true when a row was written, or with false
     *   when the rate limit held it back. False is **not** a failure and must
     *   never be drawn as one - it means the server already knows.
     */
    suspend fun report(
        bleAddress: String,
        rssi: Int,
        lat: Double?,
        lon: Double?,
        accuracyM: Double?,
        seenAt: Long
    ): CloudResult<Boolean> {
        val address = bleAddress.trim()
        if (address.isBlank()) {
            return CloudResult.Failed("There was no device address to report.", retryable = false)
        }
        if (!isDue(address, now())) return CloudResult.Ok(false)

        val row = DeviceSightingRow(
            // `device_sightings.id` is a uuid primary key with no default, so
            // the client mints it. Random rather than derived: two sightings of
            // one address minutes apart are two facts, not one row updated.
            id = UUID.randomUUID().toString(),
            deviceId = deviceIdFor(address),
            bleAddress = address,
            seenAt = seenAt.toIsoOrNull(),
            lat = lat,
            lon = lon,
            accuracyMeters = accuracyM,
            rssi = rssi,
            // Never set. See the class KDoc.
            reporterId = null
        )

        return when (
            val result = client.upsert(
                CloudTables.DEVICE_SIGHTINGS,
                listOf(row),
                DeviceSightingRow.serializer()
            )
        ) {
            is CloudResult.Ok -> {
                // Only after the server took it. Stamping the limiter first
                // would let one failed insert silence the address for ten
                // minutes, which on a wearable somebody is searching for is
                // exactly the wrong ten minutes.
                lastReportedAt[address.lowercase()] = now()
                CloudResult.Ok(true)
            }

            is CloudResult.Failed -> result
            CloudResult.Disabled -> CloudResult.Disabled
        }
    }

    /**
     * The newest sighting of each of [bleAddresses], newest first.
     *
     * At most one row per address: the query asks for them ordered by `seen_at`
     * descending and this keeps the first of each, so a wearable seen forty
     * times yesterday contributes one answer rather than forty.
     *
     * Row-level security means this only ever returns sightings of devices in
     * the caller's own circle - see the class KDoc, and note that a sighting
     * with a null `device_id` can never come back at all.
     */
    suspend fun lastSeen(bleAddresses: List<String>): CloudResult<List<DeviceSightingRow>> {
        val wanted = bleAddresses.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (wanted.isEmpty()) return CloudResult.Ok(emptyList())

        val rows = when (
            val result = client.selectWhereIn(
                table = CloudTables.DEVICE_SIGHTINGS,
                column = "ble_address",
                values = wanted,
                orderBy = "seen_at",
                descending = true
            )
        ) {
            is CloudResult.Ok -> result.value
            is CloudResult.Failed -> return result
            CloudResult.Disabled -> return CloudResult.Disabled
        }

        val decoded = rows.mapNotNull { row ->
            runCatching { sightingJson.decodeFromJsonElement(DeviceSightingRow.serializer(), row) }
                .getOrNull()
        }
        return CloudResult.Ok(newestPerAddress(decoded))
    }

    /** True when [address] has not been reported inside the window. Pure. */
    internal fun isDue(address: String, at: Long): Boolean {
        val previous = lastReportedAt[address.trim().lowercase()] ?: return true
        // A clock that moved backwards - a manual change, a network time sync -
        // makes the difference negative, and refusing to report until it caught
        // up would silence the phone for as long as the jump. So a negative
        // difference is due; zero is not, because zero is this same moment.
        val since = at - previous
        return since < 0L || since >= WINDOW_MS
    }

    companion object {

        /** One report per address per ten minutes. See the class KDoc. */
        const val WINDOW_MS: Long = 10L * 60L * 1000L

        /**
         * One row per address, keeping the newest.
         *
         * Pure and internal so it can be tested without a client. Ordering is
         * asked of the server, but a fake, a cache or a future paged read could
         * hand these over in any order, so the newest is *chosen* here rather
         * than assumed to be first - the assumption would fail silently and
         * report a wearable at last Tuesday's street corner.
         */
        internal fun newestPerAddress(rows: List<DeviceSightingRow>): List<DeviceSightingRow> =
            rows.filter {
                // A soft-deleted sighting is still a row. `selectWhereIn` says
                // its callers filter these, and this is that filter: without it
                // a withdrawn sighting can be reported as the newest one, which
                // is a wearable placed at a corner it has since left.
                !it.bleAddress.isNullOrBlank() && it.deletedAt.isNullOrBlank()
            }
                .groupBy { it.bleAddress!!.lowercase() }
                .values
                .mapNotNull { group -> group.maxByOrNull { it.seenAt.orEmpty() } }
                // ISO-8601 in UTC sorts lexically the way it sorts
                // chronologically, which is why the strings are compared
                // directly rather than parsed.
                .sortedByDescending { it.seenAt.orEmpty() }
    }
}
