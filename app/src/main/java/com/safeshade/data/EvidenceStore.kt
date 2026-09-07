package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type
import java.util.UUID

/**
 * Evidence clips: the rows, and the settings that decide whether any are made.
 *
 * ### Why its own DataStore file
 *
 * `data/Preferences.kt` declares its delegate as
 * `private val Context.dataStore by preferencesDataStore(name = "safeshade_prefs")`.
 * The delegate is private to that file, and a second delegate for the *same*
 * file name does not share the store - it builds a second `DataStore` over one
 * file and throws `IllegalStateException: There are multiple DataStores active
 * for the same file` on first use, at runtime, inside a coroutine. So this uses
 * its own file, `safeshade_evidence`, exactly as `cloud/sync/Outbox.kt` uses
 * `safeshade_cloud`.
 *
 * It is also the better shape here. A clip row is rewritten every time an
 * upload changes state, and DataStore rewrites its whole file on each `edit`;
 * sharing a file with the medical ID and the zone list would make all of those
 * churn behind a recording.
 *
 * ### Gson, not kotlinx.serialization
 *
 * Same reasoning as the outbox: this blob never leaves the phone, so there is
 * no schema to negotiate, and Gson is the established pattern here. It carries
 * that pattern's one hard rule - **the DTOs below have every field nullable
 * with a default** - because Gson does not call Kotlin constructors. It
 * allocates a zeroed object through `Unsafe` and assigns only the fields
 * present in the JSON, so a Kotlin default never runs and a non-null property
 * can arrive holding null.
 *
 * ### Why the store is an interface
 *
 * [EvidenceStore] has no Android in it, so `EvidenceRepository` - which owns
 * the cap, the file deletions and the upload opt-in gate, i.e. all the rules
 * worth testing - can be constructed in a plain JVM unit test against an
 * in-memory fake. [DataStoreEvidenceStore] is the only implementation that
 * touches disk.
 */

/** Where a clip's bytes have got to. Never a claim the phone has not verified. */
enum class EvidenceUploadState {
    /** Recorded here, and here only. The state every clip starts in. */
    LOCAL_ONLY,

    /** The user opted in to upload and the id has been handed to the outbox. */
    QUEUED,

    /** The server acknowledged the object. Only the sync track may set this. */
    UPLOADED,

    /** An attempt was made and refused. `uploadReason` says what refused it. */
    FAILED
}

/**
 * One recording made as evidence around an alert.
 *
 * [file] is a **file name**, never a path - the directory is
 * `filesDir/evidence` and is joined in exactly one place,
 * `EvidenceRepository`. Storing an absolute path would survive neither a
 * backup-restore nor an app-data move, and would let a row name a file outside
 * the folder this app is allowed to delete from.
 */
data class EvidenceClip(
    val id: String = UUID.randomUUID().toString(),
    val file: String,
    val capturedAt: Long,
    val durationMs: Int,
    val byteSize: Long,
    /** The trip this was recorded around, or null for a clip started by hand. */
    val alertId: String? = null,
    /** Matches `EvidenceRow.kind`. Audio is the only kind this phase records. */
    val kind: String = KIND_AUDIO,
    val upload: EvidenceUploadState = EvidenceUploadState.LOCAL_ONLY,
    /**
     * Why the upload is where it is, when that needs saying.
     *
     * Only ever a real reason from whatever refused - never a placeholder. A
     * row with [EvidenceUploadState.FAILED] and no reason would tell the user
     * that something went wrong and refuse to say what.
     */
    val uploadReason: String? = null
) {
    companion object {
        /** Mirrors the `audio` value of `EvidenceRow.kind`. */
        const val KIND_AUDIO = "audio"
    }
}

/**
 * What the user asked for, in the moment they asked for it.
 *
 * Both triggers default to **off**. Turning a phone's microphone on during an
 * emergency is a decision only the person carrying it can make, and a default
 * that recorded a room the first time a fall was detected would be this app
 * making it for them.
 */
data class EvidenceSettings(
    val recordOnSos: Boolean = false,
    val recordOnFall: Boolean = false,
    val durationSeconds: Int = 30,
    /**
     * Whether a finished clip may leave the phone. Off means the audio stays
     * in `filesDir/evidence` and nothing is queued for the cloud - see
     * `EvidenceRepository`, which will not call
     * [com.safeshade.repo.SyncHooks] at all while this is false.
     */
    val uploadToCloud: Boolean = false
)

/** The clip list and the settings, however they happen to be stored. */
interface EvidenceStore {
    val clips: Flow<List<EvidenceClip>>
    suspend fun setClips(clips: List<EvidenceClip>)
    val settings: Flow<EvidenceSettings>
    suspend fun setSettings(settings: EvidenceSettings)
}

// ============================================
// The on-disk shapes. Every field nullable; see the file KDoc.
// ============================================

private data class EvidenceClipDto(
    val id: String? = null,
    val file: String? = null,
    val capturedAt: Long? = null,
    val durationMs: Int? = null,
    val byteSize: Long? = null,
    val alertId: String? = null,
    val kind: String? = null,
    val upload: String? = null,
    val uploadReason: String? = null
)

private data class EvidenceSettingsDto(
    val recordOnSos: Boolean? = null,
    val recordOnFall: Boolean? = null,
    val durationSeconds: Int? = null,
    val uploadToCloud: Boolean? = null
)

/**
 * The JSON codec, as a pure object.
 *
 * Pure so the round trip can be unit-tested without a `Context`: the
 * interesting cases are a corrupt blob (which must yield an empty list rather
 * than throw inside a DataStore `map`, since a throw there kills every
 * collector of the flow) and a row from an older build missing fields the
 * current one reads.
 */
object EvidenceJson {

    private val gson = Gson()
    private val clipListType: Type = object : TypeToken<List<EvidenceClipDto>>() {}.type

    fun encodeClips(clips: List<EvidenceClip>): String = gson.toJson(
        clips.map {
            EvidenceClipDto(
                id = it.id,
                file = it.file,
                capturedAt = it.capturedAt,
                durationMs = it.durationMs,
                byteSize = it.byteSize,
                alertId = it.alertId,
                kind = it.kind,
                upload = it.upload.name,
                uploadReason = it.uploadReason
            )
        }
    )

    fun decodeClips(json: String?): List<EvidenceClip> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<EvidenceClipDto?>>(json, clipListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
        }.getOrDefault(emptyList())
    }

    fun encodeSettings(settings: EvidenceSettings): String = gson.toJson(
        EvidenceSettingsDto(
            recordOnSos = settings.recordOnSos,
            recordOnFall = settings.recordOnFall,
            durationSeconds = settings.durationSeconds,
            uploadToCloud = settings.uploadToCloud
        )
    )

    fun decodeSettings(json: String?): EvidenceSettings {
        if (json.isNullOrBlank()) return EvidenceSettings()
        val dto = runCatching { gson.fromJson(json, EvidenceSettingsDto::class.java) }
            .getOrNull() ?: return EvidenceSettings()
        val defaults = EvidenceSettings()
        return EvidenceSettings(
            recordOnSos = dto.recordOnSos ?: defaults.recordOnSos,
            recordOnFall = dto.recordOnFall ?: defaults.recordOnFall,
            durationSeconds = (dto.durationSeconds ?: defaults.durationSeconds)
                .coerceIn(MIN_EVIDENCE_SECONDS, MAX_EVIDENCE_SECONDS),
            uploadToCloud = dto.uploadToCloud ?: defaults.uploadToCloud
        )
    }

    /**
     * A row with no id or no file name points at nothing and can never be
     * played, uploaded or deleted, so it is dropped rather than resurrected
     * with an empty string - the same call the outbox makes about an entry
     * naming a table called empty-string.
     */
    private fun EvidenceClipDto.toDomainOrNull(): EvidenceClip? {
        val realId = id?.takeIf { it.isNotBlank() } ?: return null
        val realFile = file?.takeIf { it.isNotBlank() } ?: return null
        return EvidenceClip(
            id = realId,
            file = realFile,
            capturedAt = capturedAt ?: 0L,
            durationMs = durationMs ?: 0,
            byteSize = byteSize ?: 0L,
            alertId = alertId?.takeIf { it.isNotBlank() },
            kind = kind?.takeIf { it.isNotBlank() } ?: EvidenceClip.KIND_AUDIO,
            // An unknown or missing state reads as LOCAL_ONLY: the only claim
            // that is safe to make without having spoken to the server.
            upload = runCatching { EvidenceUploadState.valueOf(upload ?: "") }
                .getOrDefault(EvidenceUploadState.LOCAL_ONLY),
            uploadReason = uploadReason?.takeIf { it.isNotBlank() }
        )
    }
}

/** The shortest recording the app will make. */
const val MIN_EVIDENCE_SECONDS: Int = 10

/** The longest. See [MIN_EVIDENCE_SECONDS]. */
const val MAX_EVIDENCE_SECONDS: Int = 120

/** How many clips are kept before the oldest is deleted, audio and all. */
const val MAX_EVIDENCE_CLIPS: Int = 50

private val Context.evidenceDataStore by preferencesDataStore(name = "safeshade_evidence")

private object EvidenceKeys {
    /** Versioned in the name, so a breaking shape change is a new key. */
    val CLIPS = stringPreferencesKey("evidence_clips_json_v1")
    val SETTINGS = stringPreferencesKey("evidence_settings_json_v1")
}

/** The real store, over `safeshade_evidence`. */
class DataStoreEvidenceStore(context: Context) : EvidenceStore {

    private val appContext = context.applicationContext

    override val clips: Flow<List<EvidenceClip>> =
        appContext.evidenceDataStore.data.map { EvidenceJson.decodeClips(it[EvidenceKeys.CLIPS]) }

    override suspend fun setClips(clips: List<EvidenceClip>) {
        appContext.evidenceDataStore.edit {
            it[EvidenceKeys.CLIPS] = EvidenceJson.encodeClips(clips)
        }
    }

    override val settings: Flow<EvidenceSettings> =
        appContext.evidenceDataStore.data.map { EvidenceJson.decodeSettings(it[EvidenceKeys.SETTINGS]) }

    override suspend fun setSettings(settings: EvidenceSettings) {
        appContext.evidenceDataStore.edit {
            it[EvidenceKeys.SETTINGS] = EvidenceJson.encodeSettings(settings)
        }
    }
}
