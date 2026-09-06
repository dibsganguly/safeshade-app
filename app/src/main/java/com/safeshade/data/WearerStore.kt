package com.safeshade.data

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.safeshade.data.local.ProfileDto
import com.safeshade.data.local.ProfileSnapshot
import com.safeshade.data.local.SafetySettingsDto
import com.safeshade.data.local.WearerDto
import com.safeshade.data.local.toDto
import java.lang.reflect.Type

/**
 * The preference surgery behind the multi-wearer model.
 *
 * Everything here operates on a [Preferences] snapshot or on the
 * [MutablePreferences] inside an `edit` block, and nothing here touches a
 * `Context`, a `DataStore` or a coroutine. That is what makes the migration
 * and the two-way mirror testable: `mutablePreferencesOf()` is a plain
 * in-memory map, so the exact code the app runs on a real upgrade also runs in
 * a JVM unit test with no Robolectric and no device.
 *
 * ### The mirror, and why it exists
 *
 * Five fields overlap between a [Wearer] and the legacy profile blob: name,
 * face, device icon, adaptive mode and medical ID. About twenty screens still
 * read those through `DeviceSettings.wearerName` and friends, and rewriting
 * all of them in the same change as the data model would mean a single commit
 * that is impossible to review and impossible to bisect. So both keys are
 * written, both directions, inside a single `edit` — [writeWearers] pushes
 * wearer #1 into the profile, [mirrorProfileIntoPrimaryWearer] pushes the
 * profile back into wearer #1 — and neither can go stale. Two separate `edit`
 * calls would be two file rewrites with a window between them in which the
 * two copies disagree.
 */

private val wearerListType: Type = object : TypeToken<List<WearerDto>>() {}.type

private fun <T> decodeJsonList(json: String?, type: Type, gson: Gson): List<T> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        gson.fromJson<List<T?>>(json, type).orEmpty().filterNotNull()
    }.getOrDefault(emptyList())
}

private fun <T> decodeJsonOne(json: String?, clazz: Class<T>, gson: Gson): T? {
    if (json.isNullOrBlank()) return null
    return runCatching { gson.fromJson(json, clazz) }.getOrNull()
}

private fun profileIn(prefs: Preferences, gson: Gson): ProfileSnapshot =
    (decodeJsonOne(prefs[PrefsKeys.PROFILE], ProfileDto::class.java, gson) ?: ProfileDto())
        .toDomain()

private fun safetyIn(prefs: Preferences, gson: Gson): SafetySettings =
    (decodeJsonOne(prefs[PrefsKeys.SAFETY], SafetySettingsDto::class.java, gson)
        ?: SafetySettingsDto()).toDomain()

private fun storedWearers(prefs: Preferences, gson: Gson): List<Wearer> =
    decodeJsonList<WearerDto>(prefs[PrefsKeys.WEARERS], wearerListType, gson).map { it.toDomain() }

/**
 * Builds wearer #1 out of what a v2 install already knew.
 *
 * Name, face, icon and mode come from the profile and its `DeviceSettings`;
 * the medical ID is the one the guardian filled in; the contacts are copied
 * from the global list so that a wearer record read on its own — by the Circle
 * page, or as a server row — is not mysteriously contactless. Copying is safe
 * because `contactsFor` unions on the normalised number, so somebody present
 * in both lists is still one person and is still texted once.
 *
 * The id is [PRIMARY_WEARER_ID], never a fresh UUID. See that constant.
 */
internal fun synthesiseWearerFromLegacy(
    profile: ProfileSnapshot,
    safety: SafetySettings
): Wearer = Wearer(
    id = PRIMARY_WEARER_ID,
    name = profile.deviceSettings.wearerName,
    avatarId = profile.deviceSettings.wearerAvatarId,
    medicalId = profile.medicalId,
    activeMode = profile.activeMode,
    iconType = profile.deviceSettings.iconType,
    // No device addresses, deliberately. A v2 install remembered pairings in
    // `paired_devices_json`, but "this phone has connected to that address" is
    // not "this person wears that device": a guardian who tested a wearable
    // before handing it to somebody else would find it bound to the wrong
    // person forever, and the binding drives which medical card is pushed over
    // the link. Binding is an explicit act.
    deviceAddresses = emptyList(),
    contacts = safety.emergencyContacts,
    isSelf = profile.role == UserRole.COMPANION
)

/**
 * The wearer list as the app should see it: what is stored, or the synthesised
 * primary when nothing is stored yet. **Never empty.**
 *
 * Synthesising here rather than only in the migration is what makes the
 * emptiness impossible to observe. A screen that renders between process start
 * and the migration's `edit` landing still sees exactly one person, carrying
 * the same id they will have afterwards, instead of a blank Circle that fills
 * itself in a frame later.
 */
internal fun decodeWearers(prefs: Preferences, gson: Gson): List<Wearer> {
    val stored = storedWearers(prefs, gson)
    if (stored.isNotEmpty()) return stored
    return listOf(synthesiseWearerFromLegacy(profileIn(prefs, gson), safetyIn(prefs, gson)))
}

/** Writes the profile blob and its v1 mode mirror. No wearer side effects. */
internal fun writeProfile(prefs: MutablePreferences, gson: Gson, snapshot: ProfileSnapshot) {
    prefs[PrefsKeys.PROFILE] = gson.toJson(snapshot.toDto())
    // Mirrored into the v1 key so a downgraded build still opens in the right
    // mode. Cheap, and the alternative is an install that appears to have
    // reset itself.
    prefs[PrefsKeys.ACTIVE_MODE] = snapshot.activeMode.wireName
}

/**
 * Writes the wearer list and pushes wearer #1's shared fields into the profile.
 *
 * What is *not* mirrored is anything a wearer record does not own — the role,
 * the device id and name, the owner's own name — because a mirror that writes
 * fields it does not own is exactly how one edit quietly reverts another.
 */
internal fun writeWearers(prefs: MutablePreferences, gson: Gson, wearers: List<Wearer>) {
    val capped = wearers.take(PrefsLimits.WEARERS)
    prefs[PrefsKeys.WEARERS] = gson.toJson(capped.map { it.toDto() })

    val primary = capped.firstOrNull() ?: return
    val profile = profileIn(prefs, gson)
    writeProfile(
        prefs, gson,
        profile.copy(
            medicalId = primary.medicalId,
            activeMode = primary.activeMode,
            deviceSettings = profile.deviceSettings.copy(
                wearerName = primary.name,
                wearerAvatarId = primary.avatarId,
                iconType = primary.iconType
            )
        )
    )

    // A selection pointing at somebody no longer on the list resolves to the
    // first wearer anyway, but leaving the stale id on disk means a later
    // re-add of that same id silently re-selects a person the user moved on
    // from — a surprise with no visible cause.
    val selected = prefs[PrefsKeys.SELECTED_WEARER]
    if (selected != null && capped.none { it.id == selected }) {
        prefs.remove(PrefsKeys.SELECTED_WEARER)
    }
}

/**
 * The other direction: a profile write updates wearer #1.
 *
 * Only when the key already exists. On an install that has not been migrated
 * there is nothing to update, and writing a wearer here would freeze one out
 * of a profile that is still mid-edit; [decodeWearers] covers that case
 * correctly and for free.
 */
internal fun mirrorProfileIntoPrimaryWearer(
    prefs: MutablePreferences,
    gson: Gson,
    snapshot: ProfileSnapshot
) {
    val stored = storedWearers(prefs, gson)
    if (stored.isEmpty()) return
    val updated = stored.toMutableList()
    updated[0] = updated[0].copy(
        name = snapshot.deviceSettings.wearerName,
        avatarId = snapshot.deviceSettings.wearerAvatarId,
        iconType = snapshot.deviceSettings.iconType,
        medicalId = snapshot.medicalId,
        activeMode = snapshot.activeMode
    )
    prefs[PrefsKeys.WEARERS] = gson.toJson(updated.map { it.toDto() })
}

/**
 * v1 to v2: seed the profile blob from the v1 `active_persona_mode` string.
 *
 * Unchanged behaviour, lifted out of `migrateIfNeeded` so the version ladder
 * reads as a list of steps. The stored mode name is untrusted — it may name a
 * mode a later build removed — so it goes through [PersonaMode.fromWire],
 * which mirrors the firmware's own fallback to BACKPACK rather than throwing
 * out of `valueOf` inside a DataStore edit.
 */
internal fun migrateV1ToV2(prefs: MutablePreferences, gson: Gson) {
    if (prefs[PrefsKeys.PROFILE] != null) return
    val legacyMode = PersonaMode.fromWire(prefs[PrefsKeys.ACTIVE_MODE].orEmpty())
    prefs[PrefsKeys.PROFILE] = gson.toJson(ProfileSnapshot(activeMode = legacyMode).toDto())
}

/**
 * v2 to v3: give the one wearer this install already had a record of their own.
 *
 * Writes exactly one [Wearer], built by [synthesiseWearerFromLegacy], under
 * the stable [PRIMARY_WEARER_ID]. It selects nobody — a list of one needs no
 * selection — and it deletes nothing: `profile_json_v1` keeps its
 * `wearerName`, and [writeWearers] keeps the two agreeing from here on.
 *
 * Idempotent on the presence of the key, so a partly-applied migration (the
 * process killed between the `edit` landing and the version write) re-runs
 * without duplicating the person.
 */
internal fun migrateV2ToV3(prefs: MutablePreferences, gson: Gson) {
    if (prefs[PrefsKeys.WEARERS] != null) return
    val wearer = synthesiseWearerFromLegacy(profileIn(prefs, gson), safetyIn(prefs, gson))
    prefs[PrefsKeys.WEARERS] = gson.toJson(listOf(wearer.toDto()))
}
