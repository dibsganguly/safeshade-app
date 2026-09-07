package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

/**
 * The firmware releases the cloud last offered, and what each device answered.
 *
 * ### Why its own DataStore file
 *
 * `data/Preferences.kt` owns `safeshade_prefs`, `cloud/sync/CloudDataStore.kt`
 * owns `safeshade_cloud`, `data/EvidenceStore.kt` owns `safeshade_evidence`
 * and `data/VitalsStore.kt` owns `safeshade_vitals`. A second delegate over a
 * file that already has one does not share the store — it builds a second
 * `DataStore` over one file and the first read throws
 * `IllegalStateException: There are multiple DataStores active for the same
 * file`, at runtime, inside a coroutine. So this declares its own file,
 * `safeshade_firmware`, and the delegate lives here where its only writer is.
 *
 * ### Gson, not kotlinx.serialization
 *
 * `data/` is Gson and `cloud/dto/` is `@Serializable`; a class carrying both
 * annotations is the thing this codebase has decided not to have.
 * [FirmwareRelease] therefore mirrors `cloud.dto.FirmwareReleaseRow` as a plain
 * domain type, and [FirmwareReleaseDto] is its on-disk shape with **every field
 * nullable and defaulted** — Gson does not call Kotlin constructors, so a
 * default argument never runs and a non-null property can arrive holding null.
 *
 * ### What the cache is for, and what it is not
 *
 * The list is a *cache of what the cloud said*, kept so the update screen has
 * something true to show when the network is down. It is never evidence that a
 * release is installed. The only record of an installed version is
 * [FirmwareCache.installedVersionByAddress], and the one thing allowed to write
 * that is a real `ACK:VER:<semver>` from the wearable — see
 * `OtaProtocol.VersionReply` for why a bare `ACK:VER` is not one.
 */

/**
 * One published firmware image, as this phone knows it.
 *
 * Mirrors `cloud.dto.FirmwareReleaseRow` field for field, with [publishedAt]
 * reduced to epoch millis because nothing on the phone needs the server's
 * timestamp text.
 *
 * [versionCode] is the monotonic ordering the server maintains, so "newer" is a
 * comparison rather than a string parse; [FirmwareRepository.compareVersions]
 * exists only for the case where two releases share a code.
 */
data class FirmwareRelease(
    val id: String,
    /** `s1` | `5g` | `spark`, matching `DeviceModel.wireName`. */
    val model: String,
    val version: String,
    val versionCode: Int,
    /** The object's path in cloud storage; a download URL is minted from it. */
    val storagePath: String,
    /** Lower- or upper-case hex; comparison is case-insensitive. */
    val sha256: String,
    val byteSize: Long,
    val releaseNotes: String? = null,
    /** True for a fix nobody should be able to defer. */
    val mandatory: Boolean = false,
    val publishedAt: Long? = null
)

/**
 * Everything this store holds, in one blob.
 *
 * One value rather than three keys because DataStore rewrites its whole file on
 * every `edit` anyway, and a single read-modify-write is easier to reason about
 * than three that can interleave.
 */
data class FirmwareCache(
    /** Keyed by `DeviceModel.wireName`. Absent means never fetched, not empty. */
    val releasesByModel: Map<String, List<FirmwareRelease>> = emptyMap(),
    /** When the cloud was last asked, whatever the answer was. Null if never. */
    val lastCheckedAt: Long? = null,
    /**
     * Keyed by BLE address: the version *that device* reported over the link.
     *
     * Per address rather than one global value because a phone may pair with
     * more than one wearable, and attributing one device's version to another
     * would be a claim nothing verified.
     */
    val installedVersionByAddress: Map<String, String> = emptyMap()
)

/** The firmware cache, however it happens to be stored. */
interface FirmwareStore {
    val cache: Flow<FirmwareCache>
    suspend fun setCache(cache: FirmwareCache)
}

// ============================================
// The on-disk shape. Every field nullable; see the file KDoc.
// ============================================

private data class FirmwareReleaseDto(
    val id: String? = null,
    val model: String? = null,
    val version: String? = null,
    val versionCode: Int? = null,
    val storagePath: String? = null,
    val sha256: String? = null,
    val byteSize: Long? = null,
    val releaseNotes: String? = null,
    val mandatory: Boolean? = null,
    val publishedAt: Long? = null
)

private data class FirmwareCacheDto(
    val releasesByModel: Map<String, List<FirmwareReleaseDto?>?>? = null,
    val lastCheckedAt: Long? = null,
    val installedVersionByAddress: Map<String, String?>? = null
)

/**
 * The JSON codec, as a pure object.
 *
 * Pure so the round trip can be unit-tested without a `Context`. The
 * interesting cases are a corrupt blob — which must yield the empty cache
 * rather than throw inside a DataStore `map`, since a throw there kills every
 * collector of the flow — and a row from an older build missing fields this one
 * reads.
 */
object FirmwareJson {

    private val gson = Gson()
    private val cacheType: Type = object : TypeToken<FirmwareCacheDto>() {}.type

    fun encode(cache: FirmwareCache): String = gson.toJson(
        FirmwareCacheDto(
            releasesByModel = cache.releasesByModel.mapValues { (_, releases) ->
                releases.map { it.toDto() }
            },
            lastCheckedAt = cache.lastCheckedAt,
            installedVersionByAddress = cache.installedVersionByAddress
        )
    )

    fun decode(json: String?): FirmwareCache {
        if (json.isNullOrBlank()) return FirmwareCache()
        val dto = runCatching { gson.fromJson<FirmwareCacheDto>(json, cacheType) }
            .getOrNull() ?: return FirmwareCache()
        return FirmwareCache(
            releasesByModel = dto.releasesByModel.orEmpty()
                .mapValues { (_, releases) -> releases.orEmpty().filterNotNull().mapNotNull { it.toDomainOrNull() } }
                .filterValues { it.isNotEmpty() },
            lastCheckedAt = dto.lastCheckedAt,
            installedVersionByAddress = dto.installedVersionByAddress.orEmpty()
                .mapNotNull { (address, version) ->
                    val realVersion = version?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    if (address.isBlank()) null else address to realVersion
                }
                .toMap()
        )
    }

    private fun FirmwareRelease.toDto() = FirmwareReleaseDto(
        id = id,
        model = model,
        version = version,
        versionCode = versionCode,
        storagePath = storagePath,
        sha256 = sha256,
        byteSize = byteSize,
        releaseNotes = releaseNotes,
        mandatory = mandatory,
        publishedAt = publishedAt
    )

    /**
     * A release missing an id, a version, a storage path or a digest cannot be
     * offered to anyone: there would be nothing to download, or nothing to
     * check the download against. It is dropped rather than resurrected with
     * empty strings, which is the same call `EvidenceJson` makes about a clip
     * row naming no file.
     */
    private fun FirmwareReleaseDto.toDomainOrNull(): FirmwareRelease? {
        val realId = id?.takeIf { it.isNotBlank() } ?: return null
        val realModel = model?.takeIf { it.isNotBlank() } ?: return null
        val realVersion = version?.takeIf { it.isNotBlank() } ?: return null
        val realPath = storagePath?.takeIf { it.isNotBlank() } ?: return null
        val realSha = sha256?.takeIf { it.isNotBlank() } ?: return null
        return FirmwareRelease(
            id = realId,
            model = realModel,
            version = realVersion,
            versionCode = versionCode ?: 0,
            storagePath = realPath,
            sha256 = realSha,
            byteSize = byteSize ?: 0L,
            releaseNotes = releaseNotes?.takeIf { it.isNotBlank() },
            mandatory = mandatory ?: false,
            publishedAt = publishedAt
        )
    }
}

private val Context.firmwareDataStore by preferencesDataStore(name = "safeshade_firmware")

private object FirmwareKeys {
    /** Versioned in the name, so a breaking shape change is a new key. */
    val CACHE = stringPreferencesKey("firmware_cache_json_v1")
}

/** The real store, over `safeshade_firmware`. */
class DataStoreFirmwareStore(context: Context) : FirmwareStore {

    private val appContext = context.applicationContext

    override val cache: Flow<FirmwareCache> =
        appContext.firmwareDataStore.data.map { FirmwareJson.decode(it[FirmwareKeys.CACHE]) }

    override suspend fun setCache(cache: FirmwareCache) {
        appContext.firmwareDataStore.edit {
            it[FirmwareKeys.CACHE] = FirmwareJson.encode(cache)
        }
    }
}

/** In-memory, for unit tests and for previews. No Android, no disk. */
class InMemoryFirmwareStore(initial: FirmwareCache = FirmwareCache()) : FirmwareStore {

    private val state = MutableStateFlow(initial)

    override val cache: Flow<FirmwareCache> = state

    override suspend fun setCache(cache: FirmwareCache) {
        state.value = cache
    }

    /** The current value, for assertions that do not want to collect a flow. */
    val current: FirmwareCache get() = state.value
}
