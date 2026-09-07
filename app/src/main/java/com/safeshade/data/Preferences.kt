/**
 * SafeShade - Universal Safety Companion
 *
 * Preferences.kt
 *
 * DataStore-backed local persistence. Everything that must survive process
 * death lives behind this class and nowhere else.
 *
 * FIXES (this pass):
 *
 *  1. `PairedDevice` was declared *twice* in `com.safeshade.data` — once in
 *     `Models.kt` and once here — which is a redeclaration error, not a
 *     shadowing warning. The domain type in `Models.kt` wins; the copy that
 *     lived in this file is gone. Its `iconOrdinal` field survives only in
 *     `PairedDeviceDto` for read compatibility (see that type's comment).
 *
 *  2. Everything below `SMS_ALLOWLIST` used to live in `remember {}` blocks
 *     inside `SafeShadeApp`, so the medical ID, the safety settings, the fall
 *     history, the message log, the safe zones and the active journey were all
 *     lost on process death. A fall-detection product that forgets its own
 *     trip history when Android reclaims the process is not one you can
 *     hand to a guardian.
 *
 * Design rules that apply to every accessor here:
 *
 *  - **Every decode is wrapped in `runCatching { }.getOrDefault(default)`.**
 *    A `JsonSyntaxException` thrown inside a `map` on `dataStore.data` does not
 *    fail one read; it cancels the flow for every collector in the process and
 *    the app renders empty forever with no crash to report.
 *  - **Lists are capped on write, never on read.** Capping on read leaves the
 *    oversized blob on disk to be re-parsed on every single emission.
 *  - **Legacy keys are never deleted.** They still feed the migration path, and
 *    a user who downgrades should find their pairing list intact.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safeshade.data.local.CheckInRequestDto
import com.safeshade.data.local.FallAlertEventDto
import com.safeshade.data.local.GeofenceZoneDto
import com.safeshade.data.local.JourneyDto
import com.safeshade.data.local.LocationStateDto
import com.safeshade.data.local.PairedDeviceDto
import com.safeshade.data.local.ProfileDto
import com.safeshade.data.local.ProfileSnapshot
import com.safeshade.data.local.QuickMessageDto
import com.safeshade.data.local.ReminderDto
import com.safeshade.data.local.SafetySettingsDto
import com.safeshade.data.local.TelemetryPointDto
import com.safeshade.data.local.VoiceNoteDto
import com.safeshade.data.local.WearerDto
import com.safeshade.data.local.toDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

private val Context.dataStore by preferencesDataStore(name = "safeshade_prefs")

enum class DarkModePreference { SYSTEM, LIGHT, DARK }

/**
 * What the phone remembers about the wearable between sessions, for the
 * out-of-reach and low-battery notices.
 *
 * @param lastConnectedAt null when this phone has never had a wearable on the
 *   link. That is not "offline since the epoch": with no connection ever made
 *   there is no outage to report, and the watch stays quiet.
 * @param lastAddress the BLE address of that wearable, kept because the link
 *   reports a blank address once disconnected and the notice has to name a
 *   person.
 */
data class WearableWatchMarks(
    val lastConnectedAt: Long? = null,
    val lastAddress: String = "",
    val offlineAlerted: Boolean = false,
    val lowBatteryAlerted: Boolean = false
)

/**
 * Internal rather than private so the top-level migration steps below — and
 * the unit tests that drive them with `mutablePreferencesOf()` — can name the
 * same keys the class writes. A migration tested against a second, hand-copied
 * list of key names is a migration that passes while the app breaks.
 */
internal object PrefsKeys {
    // --- v1 keys. Format frozen; still read and still written. ---
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    val PAIRED_DEVICES = stringPreferencesKey("paired_devices_json")
    val ACTIVE_MODE = stringPreferencesKey("active_persona_mode")
    val DEVICE_PHONE_NUMBER = stringPreferencesKey("device_phone_number")
    val SMS_ALLOWLIST = stringPreferencesKey("sms_allowlist")

    // --- v2 keys. All JSON, all versioned in the key name itself so a
    // breaking shape change is a new key rather than a silent misparse. ---
    val PROFILE = stringPreferencesKey("profile_json_v1")
    val SAFETY = stringPreferencesKey("safety_json_v1")
    val ZONES = stringPreferencesKey("zones_json_v1")
    val FALL_HISTORY = stringPreferencesKey("fall_history_json_v1")
    val MESSAGES = stringPreferencesKey("messages_json_v1")
    val REMINDERS = stringPreferencesKey("reminders_json_v1")
    val JOURNEY = stringPreferencesKey("journey_json_v1")
    val TELEMETRY_HISTORY = stringPreferencesKey("telemetry_history_json_v1")
    val LAST_DEVICE_LOCATION = stringPreferencesKey("last_device_location_json_v1")
    val CHECKINS = stringPreferencesKey("checkins_json_v1")
    val SCHEMA_VERSION = intPreferencesKey("prefs_schema_version")

    // --- v3 keys. The multi-wearer model. ---
    val WEARERS = stringPreferencesKey("wearers_json_v1")
    val SELECTED_WEARER = stringPreferencesKey("selected_wearer_id")

    // --- Additive keys. No schema bump: every one of them reads as a
    // documented default when absent, so there is nothing for a migration to
    // do and a version bump would only make an upgrade rewrite the file. ---
    val VOICE_NOTES = stringPreferencesKey("voice_notes_json_v1")

    /**
     * When the phone last had a bound wearable on the other end of the link,
     * and which address that was.
     *
     * On disk rather than in memory because "out of reach for two hours" is a
     * claim about wall-clock time that has to survive the process being
     * reclaimed, which on a phone in a pocket is the normal case. The address
     * is stored alongside because the link reports a blank one once it is
     * disconnected, and the notice has to name the person, not "the wearable".
     */
    val LAST_WEARABLE_CONNECTED_AT = longPreferencesKey("last_wearable_connected_at")
    val LAST_WEARABLE_ADDRESS = stringPreferencesKey("last_wearable_address")

    /**
     * Whether the guardian has already been told about the current outage.
     *
     * Persisted for one reason: without it, every cold start during a long
     * outage would re-raise the same notice, and a phone that restarts a few
     * times overnight would deliver the same sentence four times.
     */
    val OFFLINE_ALERTED = booleanPreferencesKey("wearable_offline_alerted")

    /** Whether the low-battery notice has fired for the current discharge. */
    val LOW_BATTERY_ALERTED = booleanPreferencesKey("wearable_low_battery_alerted")
}

/**
 * Caps, in one place so the storage budget is visible at a glance.
 *
 * DataStore rewrites the entire preferences file on every `edit`, so an
 * uncapped list is not merely large — it makes every unrelated write slower,
 * forever. These numbers are chosen so the whole file stays well inside a few
 * hundred kilobytes even when every list is full.
 */
object PrefsLimits {
    const val ZONES = 20
    const val FALL_HISTORY = 100
    const val MESSAGES = 200
    const val REMINDERS = 32
    const val TELEMETRY_HISTORY = 48
    const val CHECKINS = 20

    /**
     * How many people one phone may look after.
     *
     * Not a licensing limit — it is the point past which a `wearers_json_v1`
     * blob starts to dominate a file that DataStore rewrites in full on every
     * unrelated write. A guardian with more than a dozen wearers is running an
     * institution, and that is a different product.
     */
    const val WEARERS = 12

    /**
     * How many voice notes are kept.
     *
     * The same number as [MESSAGES], because a voice note *is* a message on the
     * Talk thread and a thread whose text and audio scrolled off at different
     * depths would read as gaps in the conversation. Twenty seconds of AAC at
     * 24 kbit/s is roughly 60 kB, so 200 notes is a bounded ~12 MB of audio
     * under `filesDir` — and the rows the cap drops take their files with them.
     */
    const val VOICE_NOTES = 200

    /** Bumped whenever [SafeShadePreferences.migrateIfNeeded] gains a step. */
    const val CURRENT_SCHEMA_VERSION = 3
}

class SafeShadePreferences(private val context: Context) {

    private val gson = Gson()

    // ============================================
    // JSON plumbing
    // ============================================

    /**
     * Decodes a stored JSON list, or returns empty.
     *
     * Takes an erased [Type] rather than a reified generic because Gson needs
     * the `TypeToken` anyway, and a reified inline function that captures a
     * `TypeToken` per call site inlines a new anonymous class into every
     * accessor for no benefit.
     */
    private fun <T> decodeList(json: String?, type: Type): List<T> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            gson.fromJson<List<T?>>(json, type).orEmpty().filterNotNull()
        }.getOrDefault(emptyList())
    }

    private fun <T> decodeOne(json: String?, clazz: Class<T>): T? {
        if (json.isNullOrBlank()) return null
        return runCatching { gson.fromJson(json, clazz) }.getOrNull()
    }

    private val zoneListType: Type = object : TypeToken<List<GeofenceZoneDto>>() {}.type
    private val fallListType: Type = object : TypeToken<List<FallAlertEventDto>>() {}.type
    private val messageListType: Type = object : TypeToken<List<QuickMessageDto>>() {}.type
    private val reminderListType: Type = object : TypeToken<List<ReminderDto>>() {}.type
    private val telemetryListType: Type = object : TypeToken<List<TelemetryPointDto>>() {}.type
    private val checkInListType: Type = object : TypeToken<List<CheckInRequestDto>>() {}.type
    private val pairedListType: Type = object : TypeToken<List<PairedDeviceDto>>() {}.type
    private val voiceNoteListType: Type = object : TypeToken<List<VoiceNoteDto>>() {}.type

    /**
     * Every reader goes through here so the `dataStore.data` reference exists
     * once. The lambda is intentionally *not* suspending: a decode is pure CPU
     * work and nothing in a reader should be able to do I/O of its own.
     */
    private fun <T> read(block: (Preferences) -> T): Flow<T> =
        context.dataStore.data.map { block(it) }

    // ============================================
    // v1 KEYS - behaviour unchanged
    // ============================================

    val darkModePreference: Flow<DarkModePreference> = read { prefs ->
        when (prefs[PrefsKeys.DARK_MODE]) {
            "light" -> DarkModePreference.LIGHT
            "dark" -> DarkModePreference.DARK
            else -> DarkModePreference.SYSTEM
        }
    }

    suspend fun setDarkModePreference(pref: DarkModePreference) {
        context.dataStore.edit { it[PrefsKeys.DARK_MODE] = pref.name.lowercase() }
    }

    val onboardingSeen: Flow<Boolean> = read { it[PrefsKeys.ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ONBOARDING_SEEN] = seen }
    }

    val pairedDevices: Flow<List<PairedDevice>> = read { prefs ->
        decodeList<PairedDeviceDto>(prefs[PrefsKeys.PAIRED_DEVICES], pairedListType)
            .map { it.toDomain() }
    }

    suspend fun upsertPairedDevice(device: PairedDevice) {
        context.dataStore.edit { prefs ->
            val current = decodeList<PairedDeviceDto>(prefs[PrefsKeys.PAIRED_DEVICES], pairedListType)
                .map { it.toDomain() }
            val updated = current.filterNot { it.address == device.address } + device
            prefs[PrefsKeys.PAIRED_DEVICES] = gson.toJson(updated.map { it.toDto() })
        }
    }

    suspend fun removePairedDevice(address: String) {
        context.dataStore.edit { prefs ->
            val current = decodeList<PairedDeviceDto>(prefs[PrefsKeys.PAIRED_DEVICES], pairedListType)
                .map { it.toDomain() }
            prefs[PrefsKeys.PAIRED_DEVICES] =
                gson.toJson(current.filterNot { it.address == address }.map { it.toDto() })
        }
    }

    /**
     * The v1 active-mode key, kept as the raw stored string.
     *
     * Superseded by [profile] but never removed: it is the source the schema-2
     * migration reads from, and a downgrade to a v1 build must still find it.
     * Callers wanting a [PersonaMode] should read [profile] instead — this is
     * an untrusted string that may name a mode no build has any more.
     */
    val activeModeName: Flow<String> =
        read { it[PrefsKeys.ACTIVE_MODE] ?: PersonaMode.BACKPACK.wireName }

    suspend fun setActiveMode(mode: PersonaMode) {
        context.dataStore.edit { it[PrefsKeys.ACTIVE_MODE] = mode.wireName }
    }

    /**
     * The wearable's own SIM number (the EC200U gateway's number), set by the
     * Guardian so the app can send and receive messages over SMS — the
     * device-independent fallback used when BLE is not connected.
     */
    val devicePhoneNumber: Flow<String> = read { it[PrefsKeys.DEVICE_PHONE_NUMBER] ?: "" }

    suspend fun setDevicePhoneNumber(number: String) {
        context.dataStore.edit { it[PrefsKeys.DEVICE_PHONE_NUMBER] = number }
    }

    /**
     * Trusted SMS senders. When empty, every sender's SMS is shown on the
     * device (no filtering). Stored comma-joined, which is also the exact
     * `EXT SMSALLOW` wire format.
     */
    val smsAllowlist: Flow<List<String>> = read { prefs ->
        val raw = prefs[PrefsKeys.SMS_ALLOWLIST] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun setSmsAllowlist(numbers: List<String>) {
        context.dataStore.edit { it[PrefsKeys.SMS_ALLOWLIST] = numbers.joinToString(",") }
    }

    // ============================================
    // v2 KEYS
    // ============================================

    /** Identity: medical ID, device settings, role, active mode. */
    val profile: Flow<ProfileSnapshot> = read { prefs ->
        (decodeOne(prefs[PrefsKeys.PROFILE], ProfileDto::class.java) ?: ProfileDto()).toDomain()
    }

    suspend fun setProfile(snapshot: ProfileSnapshot) {
        context.dataStore.edit { prefs ->
            writeProfile(prefs, gson, snapshot)
            mirrorProfileIntoPrimaryWearer(prefs, gson, snapshot)
        }
    }

    val safetySettings: Flow<SafetySettings> = read { prefs ->
        (decodeOne(prefs[PrefsKeys.SAFETY], SafetySettingsDto::class.java) ?: SafetySettingsDto())
            .toDomain()
    }

    suspend fun setSafetySettings(settings: SafetySettings) {
        context.dataStore.edit { it[PrefsKeys.SAFETY] = gson.toJson(settings.toDto()) }
    }

    /** Safe zones, in the user's own order. Capped at [PrefsLimits.ZONES]. */
    val zones: Flow<List<GeofenceZone>> = read { prefs ->
        decodeList<GeofenceZoneDto>(prefs[PrefsKeys.ZONES], zoneListType).map { it.toDomain() }
    }

    suspend fun setZones(zones: List<GeofenceZone>) {
        context.dataStore.edit {
            // take(), not takeLast(): this list is user-ordered, so the entries
            // that fall off the end must be the ones the user added last.
            it[PrefsKeys.ZONES] = gson.toJson(zones.take(PrefsLimits.ZONES).map { z -> z.toDto() })
        }
    }

    /**
     * Trip history — falls, SOS presses, missed check-ins, zone exits.
     *
     * Chronological ascending, so the cap drops the *oldest* entries. Every
     * history list in this file uses that convention; the UI reverses for
     * display rather than the storage layer second-guessing it.
     */
    val fallHistory: Flow<List<FallAlertEvent>> = read { prefs ->
        decodeList<FallAlertEventDto>(prefs[PrefsKeys.FALL_HISTORY], fallListType)
            .map { it.toDomain() }
    }

    suspend fun setFallHistory(events: List<FallAlertEvent>) {
        context.dataStore.edit {
            it[PrefsKeys.FALL_HISTORY] =
                gson.toJson(events.takeLast(PrefsLimits.FALL_HISTORY).map { e -> e.toDto() })
        }
    }

    val messages: Flow<List<QuickMessage>> = read { prefs ->
        decodeList<QuickMessageDto>(prefs[PrefsKeys.MESSAGES], messageListType).map { it.toDomain() }
    }

    suspend fun setMessages(messages: List<QuickMessage>) {
        context.dataStore.edit {
            it[PrefsKeys.MESSAGES] =
                gson.toJson(messages.takeLast(PrefsLimits.MESSAGES).map { m -> m.toDto() })
        }
    }

    val reminders: Flow<List<Reminder>> = read { prefs ->
        decodeList<ReminderDto>(prefs[PrefsKeys.REMINDERS], reminderListType).map { it.toDomain() }
    }

    suspend fun setReminders(reminders: List<Reminder>) {
        context.dataStore.edit {
            it[PrefsKeys.REMINDERS] =
                gson.toJson(reminders.take(PrefsLimits.REMINDERS).map { r -> r.toDto() })
        }
    }

    /**
     * The single in-flight journey, or null.
     *
     * Nullable rather than a list because "walk with me" is deliberately
     * one-at-a-time: two concurrent journeys would mean two competing
     * escalation deadlines with no way for a guardian to tell which alert
     * belongs to which walk.
     */
    val journey: Flow<Journey?> = read { prefs ->
        decodeOne(prefs[PrefsKeys.JOURNEY], JourneyDto::class.java)?.toDomain()
    }

    suspend fun setJourney(journey: Journey?) {
        context.dataStore.edit { prefs ->
            if (journey == null) prefs.remove(PrefsKeys.JOURNEY)
            else prefs[PrefsKeys.JOURNEY] = gson.toJson(journey.toDto())
        }
    }

    /**
     * The persisted, downsampled battery/link trail.
     *
     * Deliberately small: the live in-memory ring in `DeviceRepository` holds
     * far more, and this exists only so the sparklines are not blank for the
     * first minute after a cold start.
     */
    val telemetryHistory: Flow<List<TelemetryPoint>> = read { prefs ->
        decodeList<TelemetryPointDto>(prefs[PrefsKeys.TELEMETRY_HISTORY], telemetryListType)
            .map { it.toDomain() }
    }

    suspend fun setTelemetryHistory(points: List<TelemetryPoint>) {
        context.dataStore.edit {
            it[PrefsKeys.TELEMETRY_HISTORY] =
                gson.toJson(points.takeLast(PrefsLimits.TELEMETRY_HISTORY).map { p -> p.toDto() })
        }
    }

    /** Where the wearable was last known to be. Null until one fix exists. */
    val lastDeviceLocation: Flow<LocationState?> = read { prefs ->
        decodeOne(prefs[PrefsKeys.LAST_DEVICE_LOCATION], LocationStateDto::class.java)
            ?.toDomain()
            ?.takeIf { it.isValid }
    }

    suspend fun setLastDeviceLocation(location: LocationState?) {
        context.dataStore.edit { prefs ->
            if (location == null || !location.isValid) prefs.remove(PrefsKeys.LAST_DEVICE_LOCATION)
            else prefs[PrefsKeys.LAST_DEVICE_LOCATION] = gson.toJson(location.toDto())
        }
    }

    val checkIns: Flow<List<CheckInRequest>> = read { prefs ->
        decodeList<CheckInRequestDto>(prefs[PrefsKeys.CHECKINS], checkInListType)
            .map { it.toDomain() }
    }

    suspend fun setCheckIns(requests: List<CheckInRequest>) {
        context.dataStore.edit {
            it[PrefsKeys.CHECKINS] =
                gson.toJson(requests.takeLast(PrefsLimits.CHECKINS).map { r -> r.toDto() })
        }
    }

    // ============================================
    // VOICE NOTES
    // ============================================

    /**
     * The Talk thread's audio, oldest first.
     *
     * Same ordering convention as every other history here, so the cap on
     * write drops the oldest. The *files* those dropped rows point at are the
     * repository's problem, not this class's: nothing at this layer holds a
     * `Context` from which `filesDir` could be reached, and a cap that silently
     * orphaned an `.m4a` per note would leak megabytes with no row to find them
     * by. See `VoiceNoteRepository.add`.
     */
    val voiceNotes: Flow<List<VoiceNote>> = read { prefs ->
        decodeList<VoiceNoteDto>(prefs[PrefsKeys.VOICE_NOTES], voiceNoteListType)
            .map { it.toDomain() }
    }

    suspend fun setVoiceNotes(notes: List<VoiceNote>) {
        context.dataStore.edit {
            it[PrefsKeys.VOICE_NOTES] =
                gson.toJson(notes.takeLast(PrefsLimits.VOICE_NOTES).map { n -> n.toDto() })
        }
    }

    // ============================================
    // WEARABLE WATCH - the out-of-reach and low-battery notices
    // ============================================

    /**
     * The last time a bound wearable was on the link, its address, and whether
     * each notice has already fired for the current outage or discharge.
     *
     * One flow rather than four because every reader wants all of them and a
     * `combine` of four DataStore reads would let a reader see a fresh
     * timestamp beside a stale "already told them" flag - which is precisely
     * the state in which the notice fires twice.
     */
    val wearableWatchMarks: Flow<WearableWatchMarks> = read { prefs ->
        WearableWatchMarks(
            lastConnectedAt = prefs[PrefsKeys.LAST_WEARABLE_CONNECTED_AT],
            lastAddress = prefs[PrefsKeys.LAST_WEARABLE_ADDRESS].orEmpty(),
            offlineAlerted = prefs[PrefsKeys.OFFLINE_ALERTED] ?: false,
            lowBatteryAlerted = prefs[PrefsKeys.LOW_BATTERY_ALERTED] ?: false
        )
    }

    /** Writes all four marks in one edit. See [wearableWatchMarks]. */
    suspend fun setWearableWatchMarks(marks: WearableWatchMarks) {
        context.dataStore.edit { prefs ->
            val at = marks.lastConnectedAt
            if (at == null) prefs.remove(PrefsKeys.LAST_WEARABLE_CONNECTED_AT)
            else prefs[PrefsKeys.LAST_WEARABLE_CONNECTED_AT] = at
            prefs[PrefsKeys.LAST_WEARABLE_ADDRESS] = marks.lastAddress
            prefs[PrefsKeys.OFFLINE_ALERTED] = marks.offlineAlerted
            prefs[PrefsKeys.LOW_BATTERY_ALERTED] = marks.lowBatteryAlerted
        }
    }

    // ============================================
    // v3 KEYS - the people this phone looks after
    // ============================================

    /**
     * Everyone this phone looks after. **Never empty.**
     *
     * When `wearers_json_v1` is absent — every install that predates this
     * schema, and every fresh one until something writes a wearer — this
     * synthesises wearer #1 from the profile and the safety settings held in
     * the *same* [Preferences] snapshot. Doing it here rather than only in the
     * migration is what makes the emptiness impossible to observe: a screen
     * that renders between process start and the migration's `edit` landing
     * still sees exactly one person, with the same id it will have afterwards,
     * rather than a blank Circle that fills in a frame later.
     *
     * The synthesis reads one snapshot rather than combining three flows, so
     * there is no window in which the profile has been read and the safety
     * settings have not.
     */
    val wearers: Flow<List<Wearer>> = read { prefs -> decodeWearers(prefs, gson) }

    /**
     * Which wearer the UI is currently showing, or null for "the first one".
     *
     * Null rather than an eagerly-written id: writing a selection nobody made
     * would mean a guardian's first launch had silently picked a favourite,
     * and the fallback in [resolveWearerForDevice] is the first entry anyway.
     */
    val selectedWearerId: Flow<String?> = read { prefs ->
        prefs[PrefsKeys.SELECTED_WEARER]?.takeIf { it.isNotBlank() }
    }

    /**
     * Replaces the wearer list, mirroring wearer #1 back into the legacy
     * profile fields in the same edit.
     *
     * The mirror is the reason roughly twenty screens that read
     * `DeviceSettings.wearerName` keep working untouched. Both directions are
     * implemented ([setProfile] mirrors the other way) so neither key can go
     * stale, and both happen inside one `edit` block on the one
     * [MutablePreferences] the platform hands us — two `edit` calls would be
     * two file rewrites with a window between them in which the two keys
     * disagree.
     */
    suspend fun setWearers(wearers: List<Wearer>) {
        context.dataStore.edit { prefs -> writeWearers(prefs, gson, wearers) }
    }

    /**
     * Writes the profile and the wearer list together, in one edit.
     *
     * Needed by any change that touches both — the role fork above all, which
     * moves `role` in the profile and `isSelf` in the list and would otherwise
     * be two edits with a visibly inconsistent state between them. The wearer
     * list wins on the fields the two share, because it is the newer and more
     * specific record.
     */
    suspend fun setProfileAndWearers(snapshot: ProfileSnapshot, wearers: List<Wearer>) {
        context.dataStore.edit { prefs ->
            writeProfile(prefs, gson, snapshot)
            writeWearers(prefs, gson, wearers)
        }
    }

    suspend fun setSelectedWearerId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(PrefsKeys.SELECTED_WEARER)
            else prefs[PrefsKeys.SELECTED_WEARER] = id
        }
    }

    val schemaVersion: Flow<Int> = read { it[PrefsKeys.SCHEMA_VERSION] ?: 0 }

    // ============================================
    // MIGRATION
    // ============================================

    /**
     * Brings a v1 install up to the current schema. Idempotent.
     *
     * Called exactly once, from `AppContainer` on the application scope — **not**
     * from inside a Flow operator. Writing to DataStore from within a `map` on
     * `dataStore.data` re-triggers that very read, and the resulting write ->
     * emit -> write cycle spins until something crashes.
     *
     * Two steps today. v1 to v2 seeds [ProfileDto] from the v1
     * `active_persona_mode` string; v2 to v3 writes the first [Wearer]. Both
     * are pure functions over the [MutablePreferences] handed to the `edit`
     * block, so a unit test drives them with `mutablePreferencesOf()` instead
     * of an Android DataStore.
     *
     * No legacy key is deleted. The migration is a copy, not a move, so a
     * user who downgrades finds their v1 install exactly as they left it -
     * and the mirror in `writeWearers` keeps the copies agreeing afterwards.
     */
    suspend fun migrateIfNeeded() {
        val current = schemaVersion.first()
        if (current >= PrefsLimits.CURRENT_SCHEMA_VERSION) return

        context.dataStore.edit { prefs ->
            // Step-wise, and every step is skipped by version rather than by
            // "does the key look empty". The previous shape set the version
            // unconditionally at the end of a single block, which is correct
            // for exactly one step and silently skips the second the moment a
            // second one exists.
            if (current < 2) migrateV1ToV2(prefs, gson)
            if (current < 3) migrateV2ToV3(prefs, gson)
            prefs[PrefsKeys.SCHEMA_VERSION] = PrefsLimits.CURRENT_SCHEMA_VERSION
        }
    }
}
