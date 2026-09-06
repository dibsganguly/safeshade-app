package com.safeshade.repo

import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceSettings
import com.safeshade.data.MedicalId
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.UserRole
import com.safeshade.data.local.ProfileSnapshot
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Theme and first-run state. Separate from identity because it loads first. */
data class AppearanceSnapshot(
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    val onboardingSeen: Boolean = false
)

/**
 * Identity: who this phone is, whose device it is paired to, and how that
 * device is configured.
 *
 * Every flow here is `StateFlow<T?>` with a null initial value, and that null
 * is not laziness — it is the only thing that makes `AppState.Loading` mean
 * anything. If this repository seeded itself with `ProfileSnapshot()` the
 * combine downstream would emit a complete-looking defaults snapshot on its
 * very first tick, `Loading` would never be observed, and the first frame would
 * render BACKPACK / GUARDIAN before correcting itself a frame later. That
 * flash is the bug the Loading gate exists to remove, and seeding with defaults
 * simply moves it one layer down where it is harder to see.
 */
class ProfileRepository(
    private val prefs: SafeShadePreferences,
    private val link: DeviceLink,
    private val scope: CoroutineScope
) {

    /** Null until the first DataStore read completes. */
    val profile: StateFlow<ProfileSnapshot?> =
        prefs.profile.stateIn(scope, SharingStarted.Eagerly, null)

    val appearance: StateFlow<AppearanceSnapshot?> = kotlinx.coroutines.flow.combine(
        prefs.darkModePreference,
        prefs.onboardingSeen
    ) { dark, seen -> AppearanceSnapshot(dark, seen) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val pairedDevices: StateFlow<List<PairedDevice>?> =
        prefs.pairedDevices.stateIn(scope, SharingStarted.Eagerly, null)

    val devicePhoneNumber: StateFlow<String?> =
        prefs.devicePhoneNumber.stateIn(scope, SharingStarted.Eagerly, null)

    val smsAllowlist: StateFlow<List<String>?> =
        prefs.smsAllowlist.stateIn(scope, SharingStarted.Eagerly, null)

    // ============================================
    // Writes
    // ============================================

    /**
     * Serialises read-modify-write on the profile blob.
     *
     * `profile_json_v1` is a single aggregate holding the medical ID, the
     * device settings, the role and the active mode. Two setters that each read
     * outside a lock and then write a `copy()` do not merge — the second write
     * overwrites the first wholesale, and what is lost is not one field but an
     * entire subtree. Onboarding calling `setRole` and `setActiveMode` back to
     * back is enough to trigger it, and it would present as a medical ID the
     * user is certain they saved.
     */
    private val profileLock = Mutex()

    private suspend fun mutate(transform: (ProfileSnapshot) -> ProfileSnapshot): ProfileSnapshot =
        profileLock.withLock {
            // The read is inside the lock. Outside it, this is the lost-update
            // race described above.
            val updated = transform(prefs.profile.first())
            prefs.setProfile(updated)
            updated
        }

    /**
     * Saves the Medical ID and pushes it to the wearable.
     *
     * The push is gated on [ConnectionState.Ready], never on `Connected`. This
     * exact write, keyed on "Connected", is one of the three that had been
     * failing silently on every reconnect in the previous app: the GATT link
     * was up but service discovery had not resolved HEALTH_CHAR, so
     * `sendHealthPayload` hit its null guard and returned.
     *
     * The save happens first and unconditionally. A medical ID that reached
     * disk but not the device is recoverable on the next resync; one that
     * reached neither is gone.
     */
    suspend fun setMedicalId(medicalId: MedicalId) {
        mutate { it.copy(medicalId = medicalId) }
        pushHealthIfReady(medicalId)
    }

    private fun pushHealthIfReady(medicalId: MedicalId) {
        if (link.connectionState.value !is ConnectionState.Ready) return
        // The MTU-aware overload lives in DeviceRepository; here the protocol
        // default is correct, because this path is a best-effort immediate
        // push and pushAll() re-sends the budgeted version on the next Ready
        // edge anyway.
        link.writeHealth(DeviceProtocol.health(medicalId))
    }

    suspend fun setDeviceSettings(settings: DeviceSettings) {
        mutate { it.copy(deviceSettings = settings) }
    }

    suspend fun setRole(role: UserRole) {
        mutate { it.copy(role = role) }
    }

    /** The account holder's own name and face. */
    suspend fun setOwner(name: String, avatarId: String) {
        mutate { it.copy(ownerName = name.trim(), ownerAvatarId = avatarId) }
    }

    /**
     * Records the active mode locally.
     *
     * Deliberately does *not* write to the device: `DeviceRepository.setMode`
     * owns that, because it is the only place that can wait for the
     * `ACK:MODE:<name>` round-trip and report whether the switch actually took.
     * Splitting them means the UI can save a preference optimistically while
     * still showing an honest "not confirmed by the device" state.
     */
    suspend fun setActiveMode(mode: PersonaMode) {
        mutate { it.copy(activeMode = mode) }
    }

    suspend fun setDarkMode(pref: DarkModePreference) = prefs.setDarkModePreference(pref)

    suspend fun setOnboardingSeen(seen: Boolean) = prefs.setOnboardingSeen(seen)

    suspend fun setDevicePhoneNumber(number: String) = prefs.setDevicePhoneNumber(number)

    /**
     * Updates the trusted-sender list and pushes it.
     *
     * Third of the three writes that used to fail on every reconnect. Same
     * Ready gate, same reason.
     */
    suspend fun setSmsAllowlist(numbers: List<String>) {
        prefs.setSmsAllowlist(numbers)
        if (link.connectionState.value is ConnectionState.Ready) {
            link.writeExt("SMSALLOW", DeviceProtocol.smsAllowlist(numbers))
        }
    }

    fun upsertPairedDevice(device: PairedDevice) {
        scope.launch { prefs.upsertPairedDevice(device) }
    }

    init {
        // Remember a device the moment the link is actually usable.
        //
        // This is the whole reason the paired-devices feature never worked.
        // `upsertPairedDevice` and its DataStore backing existed and were
        // correct, and nothing in the app had ever called either: a successful
        // connection updated the state flows and wrote nothing down. So the
        // list was permanently empty, the Board row permanently read "0
        // remembered", and `removePairedDevice` - which does work - never had
        // anything to remove.
        //
        // Gated on Ready rather than Connected. `Connected` fires when the GATT
        // socket opens, before services are discovered, and a device that
        // fails discovery is not one worth remembering; `Ready` is the state
        // the rest of this class already uses to decide the link is real.
        //
        // The blank-address guard matters: a link that reports Ready with no
        // address would otherwise write a row keyed on the empty string, which
        // no `removePairedDevice(address)` could ever match - an entry the user
        // could see and not delete.
        link.connectionState
            .filter { it is ConnectionState.Ready }
            .onEach {
                val address = link.deviceAddress.value
                if (address.isBlank()) return@onEach
                upsertPairedDevice(
                    PairedDevice(
                        address = address,
                        name = link.deviceName.value.ifBlank { "SafeShade device" },
                        lastConnected = System.currentTimeMillis()
                    )
                )
            }
            .launchIn(scope)
    }

    fun removePairedDevice(address: String) {
        scope.launch { prefs.removePairedDevice(address) }
    }
}
