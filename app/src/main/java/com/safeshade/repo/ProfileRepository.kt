package com.safeshade.repo

import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceSettings
import com.safeshade.data.MedicalId
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.data.PrefsLimits
import com.safeshade.data.SafeShadePreferences
import com.safeshade.data.UserRole
import com.safeshade.data.Wearer
import com.safeshade.data.WearerResult
import com.safeshade.data.applyRoleFork
import com.safeshade.data.local.ProfileSnapshot
import com.safeshade.data.resolveWearerForDevice
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceLink
import com.safeshade.device.DeviceProtocol
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The people this phone looks after, and which of them is on screen.
 *
 * One bundle rather than two flows because the pair is always read together
 * and because `AppStateRepository`'s identity `combine` is already at the
 * five-flow limit of the typed overloads — a sixth would force the
 * `Array<Any?>` vararg and erase every type in the Loading gate.
 */
data class WearersState(
    val wearers: List<Wearer>,
    /** Null means "the first one", which is the only sensible default. */
    val selectedId: String?
) {
    /**
     * The wearer the UI is on, falling back to the first.
     *
     * Never null in practice — the wearers flow is never empty — but typed
     * nullable rather than asserted, because a `!!` here would crash the whole
     * app on a store that somehow held an empty list, and rendering nobody is
     * a far better failure than that.
     */
    val selected: Wearer?
        get() = wearers.firstOrNull { it.id == selectedId } ?: wearers.firstOrNull()
}

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

    /**
     * Everyone this phone looks after, plus the current selection.
     *
     * Null until DataStore answers, like every other persisted flow here; the
     * list inside it is never empty, because `SafeShadePreferences.wearers`
     * synthesises wearer #1 when the key is absent.
     */
    val wearersState: StateFlow<WearersState?> = kotlinx.coroutines.flow.combine(
        prefs.wearers,
        prefs.selectedWearerId
    ) { wearers, selected -> WearersState(wearers, selected) }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val wearers: StateFlow<List<Wearer>?> =
        prefs.wearers.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * The wearer whose page the UI is on.
     *
     * Deliberately *not* the wearer whose device is connected. Those are
     * different questions and conflating them is how one person's medical card
     * reaches another person's wearable — see [resolveWearerForDevice], which
     * is what the push path uses.
     */
    val selectedWearer: StateFlow<Wearer?> =
        wearersState.map { it?.selected }.stateIn(scope, SharingStarted.Eagerly, null)

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
        pushHealthIfReady()
    }

    /**
     * Sends the connected wearer's medical card, if there is a live link.
     *
     * **The card that goes is the one belonging to the wearer bound to the
     * address on the other end of the link — not the wearer whose page is on
     * screen.** A guardian editing Ma's medical ID while Baba's cane is the
     * connected device is the ordinary case in a household with two wearables,
     * and pushing the card that was just edited would put Ma's blood type and
     * allergies on Baba's screen for a responder to read. Resolution falls back
     * to the selected wearer and then to the first, so a single-wearer install
     * behaves exactly as it always did.
     *
     * The push is gated on [ConnectionState.Ready], never on `Connected`: the
     * GATT socket opens before service discovery resolves HEALTH_CHAR, and a
     * write in that window is dropped with no error to report.
     */
    private suspend fun pushHealthIfReady() {
        if (link.connectionState.value !is ConnectionState.Ready) return
        val wearer = resolveWearerForDevice(
            wearers = prefs.wearers.first(),
            address = link.deviceAddress.value,
            selectedWearerId = prefs.selectedWearerId.first()
        ) ?: return
        // The MTU-aware overload lives in DeviceRepository; here the protocol
        // default is correct, because this path is a best-effort immediate
        // push and pushAll() re-sends the budgeted version on the next Ready
        // edge anyway.
        link.writeHealth(DeviceProtocol.health(wearer.medicalId))
    }

    suspend fun setDeviceSettings(settings: DeviceSettings) {
        mutate { it.copy(deviceSettings = settings) }
    }

    /**
     * Changes the role, and forks the wearer list to match it.
     *
     * The fork belongs here rather than in the screen that offers the choice,
     * because it is a rule about the data and not about a page: a COMPANION is
     * their own wearer, so exactly one entry carries `isSelf` and takes its
     * name from the owner when it has none; a GUARDIAN looks after other
     * people, so none of their wearers is self. A screen implementing that
     * would be one copy of the rule per screen that can change the role, and
     * there are already two - onboarding and the profile.
     *
     * Profile and wearers are written in a single edit. Two would leave a
     * window in which the role says COMPANION and no wearer is self, which is
     * exactly the state every "is this me?" check in the app reads.
     *
     * Switching to COMPANION does **not** delete the other wearers. Somebody
     * who set the app up as a guardian, added three people and then changed
     * their mind about the role would lose three medical IDs; no reading of
     * "exactly one" justifies deleting a person's allergies. They stop being
     * self, and stay on the list.
     */
    suspend fun setRole(role: UserRole) {
        profileLock.withLock {
            val updated = prefs.profile.first().copy(role = role)
            val forked = applyRoleFork(prefs.wearers.first(), role, updated.ownerName)
            prefs.setProfileAndWearers(updated, forked)
        }
    }

    /** The account holder's own name and face. */
    suspend fun setOwner(name: String, avatarId: String) {
        mutate { it.copy(ownerName = name.trim(), ownerAvatarId = avatarId) }
    }

    // ============================================
    // Wearers
    // ============================================

    /**
     * Read-modify-write on the wearer list, under the **profile** lock.
     *
     * One mutex for two keys, and that is not laziness. The mirror means a
     * wearer write also rewrites `profile_json_v1` and a profile write also
     * rewrites `wearers_json_v1`. Two mutexes would let a profile edit and a
     * wearer edit interleave, and the loser's whole subtree would go - the
     * exact lost-update shape that cost the wearer's name on the profile edit
     * page.
     *
     * [transform] returns a [WearerResult] rather than a list, so a refusal
     * never reaches disk and never throws. Every caller is a tap on a control,
     * and the honest answer to "remove the last person" is a sentence.
     */
    private suspend fun mutateWearers(
        transform: (List<Wearer>, ProfileSnapshot) -> WearerResult
    ): WearerResult = profileLock.withLock {
        val profile = prefs.profile.first()
        when (val result = transform(prefs.wearers.first(), profile)) {
            is WearerResult.Ok -> {
                prefs.setWearers(result.wearers)
                result
            }

            is WearerResult.Refused -> result
        }
    }

    /**
     * Adds a person to look after.
     *
     * Refused on a COMPANION phone, because a COMPANION *is* the wearer: the
     * app they are holding is their own, and a second person on it would have
     * no meaning for any screen that says "you". Changing the role first is the
     * route, and the refusal says so rather than making the caller guess.
     */
    suspend fun addWearer(wearer: Wearer): WearerResult = mutateWearers { current, profile ->
        when {
            profile.role == UserRole.COMPANION -> WearerResult.Refused(
                "This phone is set up as the wearer's own. Change that in Profile first."
            )

            current.size >= PrefsLimits.WEARERS -> WearerResult.Refused(
                "This phone can look after " + PrefsLimits.WEARERS + " people at once."
            )

            current.any { it.id == wearer.id } -> WearerResult.Refused(
                "That person is already on the list."
            )

            else -> WearerResult.Ok(current + wearer.copy(isSelf = false))
        }
    }

    /**
     * Edits one wearer in place. An unknown id is refused, never silently added.
     *
     * Pushes the medical card afterwards for the same reason [setMedicalId]
     * does: without it, editing the connected wearer's allergies would reach
     * the wearable only on the next Ready edge, which in practice means the
     * next time the link drops and recovers. [pushHealthIfReady] resolves whose
     * card to send from the connected address, so an edit to somebody else
     * re-sends the right card rather than the edited one.
     */
    suspend fun updateWearer(wearer: Wearer): WearerResult {
        val result = mutateWearers { current, _ ->
            val index = current.indexOfFirst { it.id == wearer.id }
            if (index < 0) {
                WearerResult.Refused("That person is no longer on the list.")
            } else {
                // isSelf is not the caller's to set; only the role fork moves
                // it. A screen that could flip it would be able to make a
                // guardian's phone claim its owner is the person looked after.
                val kept = current[index].isSelf
                WearerResult.Ok(
                    current.toMutableList().also { it[index] = wearer.copy(isSelf = kept) }
                )
            }
        }
        if (result is WearerResult.Ok) pushHealthIfReady()
        return result
    }

    /**
     * Removes a wearer.
     *
     * Two refusals, each protecting a state the app cannot render. An empty
     * list has no primary wearer, so every legacy `wearerName` reader would
     * fall back to a blank; and a COMPANION without themselves on the list is a
     * phone whose owner is not the person it protects.
     */
    suspend fun removeWearer(id: String): WearerResult = mutateWearers { current, profile ->
        val victim = current.firstOrNull { it.id == id }
        when {
            victim == null -> WearerResult.Refused("That person is no longer on the list.")

            current.size <= 1 -> WearerResult.Refused(
                "This is the only person on the list. Add someone else first."
            )

            victim.isSelf && profile.role == UserRole.COMPANION -> WearerResult.Refused(
                "This phone is set up as your own, so you stay on the list."
            )

            else -> WearerResult.Ok(current.filterNot { it.id == id })
        }
    }

    /**
     * Puts a wearer's page on screen.
     *
     * Does not change which device is connected, and must not be mistaken for
     * something that does. See [pushHealthIfReady].
     */
    suspend fun selectWearer(id: String): WearerResult = profileLock.withLock {
        val current = prefs.wearers.first()
        if (current.none { it.id == id }) {
            WearerResult.Refused("That person is no longer on the list.")
        } else {
            prefs.setSelectedWearerId(id)
            WearerResult.Ok(current)
        }
    }

    /**
     * Binds a wearable to a person, by its BLE address.
     *
     * The address is removed from every other wearer in the same write. One
     * device is worn by one person at a time, and an address left on two
     * wearers would make [resolveWearerForDevice] answer by list order - which
     * is to say arbitrarily, and differently after any reordering.
     */
    suspend fun bindDevice(wearerId: String, address: String): WearerResult {
        val result = mutateWearers { current, _ ->
            val clean = address.trim()
            when {
                clean.isBlank() -> WearerResult.Refused("That device has no address to bind.")

                current.none { it.id == wearerId } ->
                    WearerResult.Refused("That person is no longer on the list.")

                else -> WearerResult.Ok(
                    current.map { wearer ->
                        val without = wearer.deviceAddresses
                            .filterNot { it.equals(clean, ignoreCase = true) }
                        if (wearer.id == wearerId) {
                            wearer.copy(deviceAddresses = without + clean)
                        } else {
                            wearer.copy(deviceAddresses = without)
                        }
                    }
                )
            }
        }
        // Binding changes the answer to "whose card belongs on this device",
        // so the card is re-sent immediately. Saying "this is Ma's cane" and
        // leaving Baba's blood type on its screen until the next reconnect is
        // the failure this whole binding exists to prevent.
        if (result is WearerResult.Ok) pushHealthIfReady()
        return result
    }

    /** Unbinds an address from every wearer. Used when a device is forgotten. */
    suspend fun unbindDevice(address: String): WearerResult = mutateWearers { current, _ ->
        val clean = address.trim()
        WearerResult.Ok(
            current.map { wearer ->
                wearer.copy(
                    deviceAddresses = wearer.deviceAddresses
                        .filterNot { it.equals(clean, ignoreCase = true) }
                )
            }
        )
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
