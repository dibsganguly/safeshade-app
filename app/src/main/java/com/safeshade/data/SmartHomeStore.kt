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
import java.util.UUID

/**
 * Smart-home hooks: the outbound calls the house makes when something happens
 * to the person wearing the device, and the record of what each call did.
 *
 * ### Why its own DataStore file
 *
 * Same reason as [EvidenceStore]. `data/Preferences.kt` declares its delegate
 * privately over `safeshade_prefs`, and a second delegate for the same file
 * name does not share the store - it builds a second `DataStore` over one file
 * and throws `IllegalStateException: There are multiple DataStores active for
 * the same file` at runtime, inside a coroutine. So this owns
 * `safeshade_smarthome`.
 *
 * It is also the better shape: a firing row is appended every time a hook
 * fires and DataStore rewrites its whole file on each `edit`, so sharing a file
 * with the medical ID and the zone list would make both churn behind a burst of
 * webhook calls.
 *
 * ### Gson, not kotlinx.serialization
 *
 * The established pattern here, and this blob never leaves the phone. It
 * carries that pattern's one hard rule - **every DTO field below is nullable
 * with a default** - because Gson does not call Kotlin constructors. It
 * allocates a zeroed object through `Unsafe` and assigns only the fields
 * present in the JSON, so a Kotlin default never runs and a non-null property
 * can arrive holding null.
 *
 * ### The two secrets in here
 *
 * [SmartHomeHook.secret] is the obvious one, and [SmartHomeHook.toString] is
 * overridden to redact it because a data class `toString` reaches a log line
 * the first time anybody writes `Log.d(TAG, "$hook")`.
 *
 * [SmartHomeHook.endpointUrl] is the non-obvious one. IFTTT's Webhooks URL
 * *contains* the account key (`https://maker.ifttt.com/trigger/x/with/key/…`),
 * so a URL in a log or in an error message is a credential in a log. It is
 * redacted by the same `toString`, and nothing in this feature ever puts a URL
 * into [SmartHomeHook.lastError].
 */

/**
 * The trigger names, matching the `smart_home_hooks.trigger` check constraint
 * in `supabase/migrations/0001_init.sql` exactly. A value not in this set is
 * rejected by the server, so a hook carrying one could never sync.
 */
object SmartHomeTriggers {
    const val FALL = "fall"
    const val SOS = "sos"
    const val ZONE_EXIT = "zone_exit"
    const val ZONE_ENTER = "zone_enter"
    const val LOW_BATTERY = "low_battery"
    const val CHECK_IN_MISSED = "check_in_missed"

    /** Every accepted value, in the order the SQL lists them. */
    val ALL: List<String> = listOf(FALL, SOS, ZONE_EXIT, ZONE_ENTER, LOW_BATTERY, CHECK_IN_MISSED)

    /** What the user is shown for a trigger name. */
    fun label(trigger: String): String = when (trigger) {
        FALL -> "A fall is detected"
        SOS -> "SOS is raised"
        ZONE_EXIT -> "Someone leaves a safe zone"
        ZONE_ENTER -> "Someone arrives in a safe zone"
        LOW_BATTERY -> "The wearable's battery is low"
        CHECK_IN_MISSED -> "A check-in is missed"
        else -> trigger
    }
}

/**
 * The provider names, matching the `smart_home_hooks.provider` check constraint
 * in `supabase/migrations/0001_init.sql` exactly.
 */
object SmartHomeProviders {
    const val WEBHOOK = "webhook"
    const val HOME_ASSISTANT = "home_assistant"
    const val IFTTT = "ifttt"
    const val MATTER = "matter"

    val ALL: List<String> = listOf(WEBHOOK, HOME_ASSISTANT, IFTTT, MATTER)

    fun label(provider: String): String = when (provider) {
        WEBHOOK -> "Webhook"
        HOME_ASSISTANT -> "Home Assistant"
        IFTTT -> "IFTTT"
        MATTER -> "Matter"
        else -> provider
    }
}

/**
 * One outbound call the house should make.
 *
 * @param endpointUrl where the call goes. Treated as a secret - see the file
 *   KDoc; IFTTT puts the account key in the path.
 * @param secret the signing key, or null for an endpoint that does not check
 *   one. When present the poster sends `X-SafeShade-Signature`.
 * @param lastFiredAt when this last actually sent something. Null means it
 *   never has - never a stand-in for "we did not look".
 * @param lastError the reason the last attempt did not land, in plain English,
 *   from whatever refused it. Null when the last attempt was delivered.
 * @param lastStatusCode the HTTP status of the last attempt, or null when the
 *   endpoint was never reached at all.
 */
data class SmartHomeHook(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: String,
    val provider: String,
    val endpointUrl: String,
    val secret: String? = null,
    val enabled: Boolean = true,
    val lastFiredAt: Long? = null,
    val lastError: String? = null,
    val lastStatusCode: Int? = null
) {
    /**
     * Redacts the two credentials. See the file KDoc: the URL is one of them.
     *
     * Overridden rather than left to callers because the leak this prevents is
     * a single interpolation in a log line written months from now.
     */
    override fun toString(): String =
        "SmartHomeHook(id=$id, name=$name, trigger=$trigger, provider=$provider, " +
            "endpointUrl=<redacted>, secret=${if (secret == null) "null" else "<redacted>"}, " +
            "enabled=$enabled, lastFiredAt=$lastFiredAt, lastError=$lastError, " +
            "lastStatusCode=$lastStatusCode)"
}

/**
 * One attempt to fire a hook, kept whatever the outcome was.
 *
 * A failed attempt is the row worth having: a hook that has been quietly
 * failing for a week looks identical to one that has had nothing to report
 * unless the failures are written down.
 */
data class SmartHomeFiring(
    val hookId: String,
    val at: Long,
    val trigger: String,
    /** The HTTP status, or null when nothing was reached. */
    val statusCode: Int? = null,
    /** Plain English, from whatever refused. Null when it was delivered. */
    val error: String? = null
)

/** How many firings are kept. The oldest falls off the end. */
const val MAX_SMART_HOME_FIRINGS: Int = 50

/** The hooks and their firing log, however they happen to be stored. */
interface SmartHomeStore {
    val hooks: Flow<List<SmartHomeHook>>
    suspend fun setHooks(hooks: List<SmartHomeHook>)
    val firings: Flow<List<SmartHomeFiring>>
    suspend fun setFirings(firings: List<SmartHomeFiring>)
}

// ============================================
// The on-disk shapes. Every field nullable; see the file KDoc.
// ============================================

private data class SmartHomeHookDto(
    val id: String? = null,
    val name: String? = null,
    val trigger: String? = null,
    val provider: String? = null,
    val endpointUrl: String? = null,
    val secret: String? = null,
    val enabled: Boolean? = null,
    val lastFiredAt: Long? = null,
    val lastError: String? = null,
    val lastStatusCode: Int? = null
)

private data class SmartHomeFiringDto(
    val hookId: String? = null,
    val at: Long? = null,
    val trigger: String? = null,
    val statusCode: Int? = null,
    val error: String? = null
)

/**
 * The JSON codec, as a pure object.
 *
 * Pure so the round trip can be unit-tested without a `Context`. The
 * interesting cases are a corrupt blob - which must yield an empty list rather
 * than throw inside a DataStore `map`, since a throw there kills every
 * collector of the flow - and a row whose trigger or provider is not one the
 * server would accept, which is dropped rather than resurrected: a hook with a
 * trigger nothing ever fires is a row the user would see, believe, and never
 * get a call from.
 */
object SmartHomeJson {

    private val gson = Gson()
    private val hookListType: Type = object : TypeToken<List<SmartHomeHookDto>>() {}.type
    private val firingListType: Type = object : TypeToken<List<SmartHomeFiringDto>>() {}.type

    fun encodeHooks(hooks: List<SmartHomeHook>): String = gson.toJson(
        hooks.map {
            SmartHomeHookDto(
                id = it.id,
                name = it.name,
                trigger = it.trigger,
                provider = it.provider,
                endpointUrl = it.endpointUrl,
                secret = it.secret,
                enabled = it.enabled,
                lastFiredAt = it.lastFiredAt,
                lastError = it.lastError,
                lastStatusCode = it.lastStatusCode
            )
        }
    )

    fun decodeHooks(json: String?): List<SmartHomeHook> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<SmartHomeHookDto?>>(json, hookListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
        }.getOrDefault(emptyList())
    }

    fun encodeFirings(firings: List<SmartHomeFiring>): String = gson.toJson(
        firings.takeLast(MAX_SMART_HOME_FIRINGS).map {
            SmartHomeFiringDto(
                hookId = it.hookId,
                at = it.at,
                trigger = it.trigger,
                statusCode = it.statusCode,
                error = it.error
            )
        }
    )

    fun decodeFirings(json: String?): List<SmartHomeFiring> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<SmartHomeFiringDto?>>(json, firingListType)
                .orEmpty()
                .filterNotNull()
                .mapNotNull { it.toDomainOrNull() }
                // Capped on read as well as on write: a blob written by a build
                // with a larger cap must not grow this one's list back.
                .takeLast(MAX_SMART_HOME_FIRINGS)
        }.getOrDefault(emptyList())
    }

    /**
     * A hook missing its id, name or endpoint points at nothing and can never
     * fire, and one naming a trigger or provider the server does not accept
     * could never sync. Both are dropped rather than repaired with a guess.
     */
    private fun SmartHomeHookDto.toDomainOrNull(): SmartHomeHook? {
        val realId = id?.takeIf { it.isNotBlank() } ?: return null
        val realName = name?.takeIf { it.isNotBlank() } ?: return null
        val realUrl = endpointUrl?.takeIf { it.isNotBlank() } ?: return null
        val realTrigger = trigger?.takeIf { it in SmartHomeTriggers.ALL } ?: return null
        val realProvider = provider?.takeIf { it in SmartHomeProviders.ALL } ?: return null
        return SmartHomeHook(
            id = realId,
            name = realName,
            trigger = realTrigger,
            provider = realProvider,
            endpointUrl = realUrl,
            secret = secret?.takeIf { it.isNotBlank() },
            enabled = enabled ?: true,
            lastFiredAt = lastFiredAt,
            lastError = lastError?.takeIf { it.isNotBlank() },
            lastStatusCode = lastStatusCode
        )
    }

    /** A firing naming no hook belongs to nothing and can never be shown against one. */
    private fun SmartHomeFiringDto.toDomainOrNull(): SmartHomeFiring? {
        val realHookId = hookId?.takeIf { it.isNotBlank() } ?: return null
        return SmartHomeFiring(
            hookId = realHookId,
            at = at ?: 0L,
            trigger = trigger?.takeIf { it.isNotBlank() } ?: "",
            statusCode = statusCode,
            error = error?.takeIf { it.isNotBlank() }
        )
    }
}

private val Context.smartHomeDataStore by preferencesDataStore(name = "safeshade_smarthome")

private object SmartHomeKeys {
    /** Versioned in the name, so a breaking shape change is a new key. */
    val HOOKS = stringPreferencesKey("smarthome_hooks_json_v1")
    val FIRINGS = stringPreferencesKey("smarthome_firings_json_v1")
}

/** The real store, over `safeshade_smarthome`. */
class DataStoreSmartHomeStore(context: Context) : SmartHomeStore {

    private val appContext = context.applicationContext

    override val hooks: Flow<List<SmartHomeHook>> =
        appContext.smartHomeDataStore.data.map { SmartHomeJson.decodeHooks(it[SmartHomeKeys.HOOKS]) }

    override suspend fun setHooks(hooks: List<SmartHomeHook>) {
        appContext.smartHomeDataStore.edit {
            it[SmartHomeKeys.HOOKS] = SmartHomeJson.encodeHooks(hooks)
        }
    }

    override val firings: Flow<List<SmartHomeFiring>> =
        appContext.smartHomeDataStore.data.map {
            SmartHomeJson.decodeFirings(it[SmartHomeKeys.FIRINGS])
        }

    override suspend fun setFirings(firings: List<SmartHomeFiring>) {
        appContext.smartHomeDataStore.edit {
            it[SmartHomeKeys.FIRINGS] = SmartHomeJson.encodeFirings(firings)
        }
    }
}

/**
 * The store with no disk under it.
 *
 * Lives in `main` rather than in the test source set so the repository - which
 * owns the rules worth testing - can be built without a `Context` in a unit
 * test, and so a preview or a gallery build can hold hooks that go nowhere.
 */
class InMemorySmartHomeStore(
    initialHooks: List<SmartHomeHook> = emptyList(),
    initialFirings: List<SmartHomeFiring> = emptyList()
) : SmartHomeStore {

    private val _hooks = MutableStateFlow(initialHooks)
    private val _firings = MutableStateFlow(initialFirings)

    override val hooks: Flow<List<SmartHomeHook>> = _hooks
    override suspend fun setHooks(hooks: List<SmartHomeHook>) {
        _hooks.value = hooks
    }

    override val firings: Flow<List<SmartHomeFiring>> = _firings
    override suspend fun setFirings(firings: List<SmartHomeFiring>) {
        _firings.value = firings.takeLast(MAX_SMART_HOME_FIRINGS)
    }

    /** What the store holds right now, for assertions. */
    val currentHooks: List<SmartHomeHook> get() = _hooks.value
    val currentFirings: List<SmartHomeFiring> get() = _firings.value
}
