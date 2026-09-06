package com.safeshade.platform

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Nearest hospital, police station, fire station and pharmacy, from
 * OpenStreetMap.
 *
 * The deck's "nearest emergency services" is real data, not a curated list —
 * `IndiaEmergencyServices` (the offline, permission-free primary) already
 * covers the "no network, no location" case, and this covers "actually the
 * closest one right now". Overpass is the only source that is free, needs no
 * API key, and has no rate-limit contract worth building against — which is
 * exactly why this client is defensive about it:
 *
 *  - **A real `User-Agent`.** Overpass's public instance blocks anonymous or
 *    generic-looking clients; the identifying string is not decoration, it is
 *    what keeps this endpoint usable at all.
 *  - **No polling, no retry loop.** One request per call. Overpass's public
 *    instance is a shared, rate-limited resource run by volunteers; hammering
 *    it after a timeout would be the kind of thing that gets an app's
 *    `User-Agent` blocked for everyone who has it installed.
 *  - **A cache with a visible age.** A failed fetch — no signal, Overpass
 *    down, a timeout — still has something to show if this location (or one
 *    near it) was fetched before, and the caller can say "as of 3 h ago"
 *    instead of pretending the list is live.
 *
 * Parsing is a free top-level function ([parseOverpass]) rather than a method,
 * specifically so a fixture JSON payload can be run through it in a JVM unit
 * test without a network, an [OkHttpClient] or a cache directory.
 */
class OverpassClient(
    private val okHttp: OkHttpClient,
    private val cacheDir: File,
) {
    private val gson = Gson()

    private val cacheFolder: File by lazy {
        File(cacheDir, "overpass").apply { mkdirs() }
    }

    /**
     * Fetches the nearest hospitals, police stations, fire stations and
     * pharmacies within [radiusM] of ([lat], [lon]).
     *
     * A cache hit within [CACHE_FRESH_MS] of this location's grid cell is
     * returned without a network call at all — not as an optimisation, but so
     * that opening the services screen twice in the same afternoon does not
     * cost Overpass a second request.
     */
    suspend fun nearby(lat: Double, lon: Double, radiusM: Int = RADIUS_DEFAULT_M): OverpassResult =
        withContext(Dispatchers.IO) {
            val cellFile = cacheFileFor(lat, lon)
            val cached = readCache(cellFile)

            if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CACHE_FRESH_MS) {
                return@withContext OverpassResult.Ok(cached.services, cached.fetchedAt, fromCache = true)
            }

            try {
                val json = fetch(lat, lon, radiusM)
                val services = parseOverpass(json, lat, lon)
                val fetchedAt = System.currentTimeMillis()
                writeCache(cellFile, CacheEntry(fetchedAt, services))
                OverpassResult.Ok(services, fetchedAt, fromCache = false)
            } catch (e: Exception) {
                val cachedOk = cached?.let { OverpassResult.Ok(it.services, it.fetchedAt, fromCache = true) }
                OverpassResult.Failed(reasonFor(e), cachedOk)
            }
        }

    private fun fetch(lat: Double, lon: Double, radiusM: Int): String {
        val body = buildQuery(lat, lon, radiusM).toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url(OVERPASS_URL)
            .header("User-Agent", USER_AGENT)
            .post(body)
            .build()
        val client = okHttp.newBuilder()
            .connectTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_S, TimeUnit.SECONDS)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Overpass returned HTTP ${response.code}")
            return response.body?.string() ?: throw IOException("Overpass returned an empty response")
        }
    }

    private fun readCache(file: File): CacheEntry? {
        if (!file.exists()) return null
        return try {
            gson.fromJson(file.readText(), CacheEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(file: File, entry: CacheEntry) {
        try {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(gson.toJson(entry))
            tmp.renameTo(file)
        } catch (e: Exception) {
            // Caching is a convenience, not a contract — a failed write just
            // means the next call fetches fresh instead of from cache.
        }
    }

    /**
     * The cell key is a 1 km-ish grid rounded to two decimal degrees. That is
     * not exact away from the equator (a degree of longitude shrinks toward
     * the poles), but it only has to be good enough to group "roughly the same
     * spot" fetches together — a coarser or finer cache key here would not
     * change correctness, only how often a fetch is reused.
     */
    private fun cacheFileFor(lat: Double, lon: Double): File {
        val latCell = (lat * 100.0).roundToLong()
        val lonCell = (lon * 100.0).roundToLong()
        return File(cacheFolder, "cell_${latCell}_$lonCell.json")
    }

    private fun reasonFor(e: Exception): String = when (e) {
        is UnknownHostException -> "No internet connection"
        is SocketTimeoutException -> "OpenStreetMap did not respond in time"
        is JsonSyntaxException -> "OpenStreetMap returned data this app could not read"
        is IOException -> e.message ?: "Could not reach OpenStreetMap"
        else -> e.message ?: "Could not fetch nearby services"
    }

    private data class CacheEntry(val fetchedAt: Long, val services: List<NearbyService>)

    companion object {
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
        private const val USER_AGENT = "SafeShade/2.6.0 (Android; https://github.com/safeshade)"
        private const val TIMEOUT_S = 20L
        private const val RADIUS_DEFAULT_M = 5000
        private const val CACHE_FRESH_MS = 6L * 60 * 60 * 1000
    }
}

/** A single emergency service returned by Overpass, sorted by distance. */
data class NearbyService(
    val kind: ServiceKind,
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceM: Int,
    val phone: String?,
    val address: String?,
)

enum class ServiceKind(val label: String) {
    HOSPITAL("Hospital"),
    POLICE("Police"),
    FIRE("Fire station"),
    PHARMACY("Pharmacy"),
}

/** Result of [OverpassClient.nearby]. Nothing throws to the caller. */
sealed interface OverpassResult {
    data class Ok(val services: List<NearbyService>, val fetchedAt: Long, val fromCache: Boolean) : OverpassResult
    data class Failed(val reason: String, val cached: Ok?) : OverpassResult
}

/**
 * `[out:json][timeout:20];(...);out center;` for the four amenity kinds this
 * app cares about, as nodes, ways and relations — a hospital drawn as a
 * building outline is a way or relation with no lat/lon of its own, only a
 * `center`, which `out center;` supplies.
 */
private fun buildQuery(lat: Double, lon: Double, radiusM: Int): String {
    val amenities = listOf("hospital", "police", "fire_station", "pharmacy")
    val clauses = amenities.joinToString("\n") { amenity ->
        listOf("node", "way", "relation").joinToString("\n") { element ->
            "  $element[\"amenity\"=\"$amenity\"](around:$radiusM,$lat,$lon);"
        }
    }
    return "[out:json][timeout:20];\n(\n$clauses\n);\nout center;"
}

/**
 * Turns raw Overpass JSON into [NearbyService]s, sorted nearest-first.
 *
 * Free-standing so a fixture payload can exercise it directly in a JVM unit
 * test — this is the only part of [OverpassClient] with real logic worth
 * testing; the rest is I/O and caching plumbing.
 */
internal fun parseOverpass(json: String, lat: Double, lon: Double): List<NearbyService> {
    val gson = Gson()
    val response = gson.fromJson(json, OverpassResponse::class.java) ?: return emptyList()
    val elements = response.elements ?: return emptyList()

    return elements.mapNotNull { element ->
        val tags = element.tags ?: return@mapNotNull null
        val kind = kindFor(tags["amenity"]) ?: return@mapNotNull null
        val elLat = element.lat ?: element.center?.lat ?: return@mapNotNull null
        val elLon = element.lon ?: element.center?.lon ?: return@mapNotNull null
        val name = tags["name"]?.takeIf { it.isNotBlank() } ?: kind.label
        val phone = tags["phone"] ?: tags["contact:phone"]
        val address = addressFrom(tags)
        val distance = haversineMeters(lat, lon, elLat, elLon).roundToInt()
        NearbyService(kind, name, elLat, elLon, distance, phone, address)
    }.sortedBy { it.distanceM }
}

private fun kindFor(amenity: String?): ServiceKind? = when (amenity) {
    "hospital" -> ServiceKind.HOSPITAL
    "police" -> ServiceKind.POLICE
    "fire_station" -> ServiceKind.FIRE
    "pharmacy" -> ServiceKind.PHARMACY
    else -> null
}

private fun addressFrom(tags: Map<String, String>): String? {
    val parts = listOfNotNull(
        listOfNotNull(tags["addr:housenumber"], tags["addr:street"]).joinToString(" ").takeIf { it.isNotBlank() },
        tags["addr:city"],
        tags["addr:postcode"],
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
}

private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadiusM = 6_371_000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * asin(sqrt(a))
    return earthRadiusM * c
}

private data class OverpassResponse(val elements: List<OverpassElement>?)

private data class OverpassElement(
    val type: String?,
    val id: Long?,
    val lat: Double?,
    val lon: Double?,
    val center: OverpassCenter?,
    val tags: Map<String, String>?,
)

private data class OverpassCenter(val lat: Double?, val lon: Double?)
