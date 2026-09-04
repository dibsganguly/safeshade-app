/**
 * SafeShade - Universal Safety Companion
 *
 * Preferences.kt
 *
 * DataStore-backed local persistence. Nothing existed before this — dark
 * mode, the paired-device list, and the onboarding-seen flag all need to
 * survive process death, so this is the one place that persists them.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "safeshade_prefs")

enum class DarkModePreference { SYSTEM, LIGHT, DARK }

/**
 * A single BLE-paired device remembered locally. Distinct from the
 * currently-connected BleManager session — this is what powers "My
 * Devices" showing more than one entry (previously it always showed
 * exactly one, hardcoded).
 */
data class PairedDevice(
    val address: String,
    val name: String,
    val iconOrdinal: Int = DeviceIconType.UMBRELLA.ordinal,
    val lastConnectedAt: Long = System.currentTimeMillis()
)

private object PrefsKeys {
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    val PAIRED_DEVICES = stringPreferencesKey("paired_devices_json")
    val ACTIVE_MODE = stringPreferencesKey("active_persona_mode")
    val DEVICE_PHONE_NUMBER = stringPreferencesKey("device_phone_number")
    val SMS_ALLOWLIST = stringPreferencesKey("sms_allowlist")
}

class SafeShadePreferences(private val context: Context) {

    private val gson = Gson()

    val darkModePreference: Flow<DarkModePreference> = context.dataStore.data.map { prefs ->
        when (prefs[PrefsKeys.DARK_MODE]) {
            "light" -> DarkModePreference.LIGHT
            "dark" -> DarkModePreference.DARK
            else -> DarkModePreference.SYSTEM
        }
    }

    suspend fun setDarkModePreference(pref: DarkModePreference) {
        context.dataStore.edit { it[PrefsKeys.DARK_MODE] = pref.name.lowercase() }
    }

    val onboardingSeen: Flow<Boolean> =
        context.dataStore.data.map { it[PrefsKeys.ONBOARDING_SEEN] ?: false }

    suspend fun setOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { it[PrefsKeys.ONBOARDING_SEEN] = seen }
    }

    val pairedDevices: Flow<List<PairedDevice>> = context.dataStore.data.map { prefs ->
        val json = prefs[PrefsKeys.PAIRED_DEVICES] ?: return@map emptyList()
        runCatching {
            val type = object : TypeToken<List<PairedDevice>>() {}.type
            gson.fromJson<List<PairedDevice>>(json, type) ?: emptyList()
        }.getOrDefault(emptyList())
    }

    suspend fun upsertPairedDevice(device: PairedDevice) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val type = object : TypeToken<List<PairedDevice>>() {}.type
                gson.fromJson<List<PairedDevice>>(prefs[PrefsKeys.PAIRED_DEVICES], type) ?: emptyList()
            }.getOrDefault(emptyList())
            val updated = current.filterNot { it.address == device.address } + device
            prefs[PrefsKeys.PAIRED_DEVICES] = gson.toJson(updated)
        }
    }

    suspend fun removePairedDevice(address: String) {
        context.dataStore.edit { prefs ->
            val current = runCatching {
                val type = object : TypeToken<List<PairedDevice>>() {}.type
                gson.fromJson<List<PairedDevice>>(prefs[PrefsKeys.PAIRED_DEVICES], type) ?: emptyList()
            }.getOrDefault(emptyList())
            prefs[PrefsKeys.PAIRED_DEVICES] = gson.toJson(current.filterNot { it.address == address })
        }
    }

    val activeModeName: Flow<String> =
        context.dataStore.data.map { it[PrefsKeys.ACTIVE_MODE] ?: PersonaMode.BACKPACK.name }

    suspend fun setActiveMode(mode: PersonaMode) {
        context.dataStore.edit { it[PrefsKeys.ACTIVE_MODE] = mode.name }
    }

    /**
     * The wearable's own SIM number (the EC200U gateway's number), set by
     * the Guardian so the app can send/receive messages over SMS - the
     * device-independent fallback used when BLE isn't connected. See
     * SafeShadeApp's onSendGuardianMessage / SmsReceiver wiring.
     */
    val devicePhoneNumber: Flow<String> =
        context.dataStore.data.map { it[PrefsKeys.DEVICE_PHONE_NUMBER] ?: "" }

    suspend fun setDevicePhoneNumber(number: String) {
        context.dataStore.edit { it[PrefsKeys.DEVICE_PHONE_NUMBER] = number }
    }

    /**
     * Trusted SMS senders. When empty, every sender's SMS is shown on the
     * device (no filtering) - see AllowlistCard in GuardianScreen.kt for the
     * add/remove UI, and SafeShadeApp's wiring to
     * bleManager.sendSmsAllowlist() for how this reaches the device.
     */
    val smsAllowlist: Flow<List<String>> = context.dataStore.data.map { prefs ->
        val raw = prefs[PrefsKeys.SMS_ALLOWLIST] ?: ""
        if (raw.isBlank()) emptyList() else raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

    suspend fun setSmsAllowlist(numbers: List<String>) {
        context.dataStore.edit { it[PrefsKeys.SMS_ALLOWLIST] = numbers.joinToString(",") }
    }
}
