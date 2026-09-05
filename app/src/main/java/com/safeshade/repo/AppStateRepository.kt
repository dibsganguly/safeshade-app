package com.safeshade.repo

import com.safeshade.data.CheckInRequest
import com.safeshade.data.DarkModePreference
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.Journey
import com.safeshade.data.LiveSensorData
import com.safeshade.data.LocationState
import com.safeshade.data.MedicalId
import com.safeshade.data.PairedDevice
import com.safeshade.data.PersonaMode
import com.safeshade.data.QuickMessage
import com.safeshade.data.SafetySettings
import com.safeshade.data.TelemetryPoint
import com.safeshade.data.UserRole
import com.safeshade.data.DeviceSettings
import com.safeshade.device.ConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Everything the UI needs, in one flow.
 *
 * ### Why `Loading` is a real state and not a convenience
 *
 * The obvious way to write this is to give every source a sensible default and
 * combine them into a single always-valid snapshot. The previous app did
 * exactly that, and the visible result was a first frame showing BACKPACK mode,
 * the Guardian role and an empty medical ID, corrected one frame later once
 * DataStore actually answered. Users see that as a flicker; a guardian sees it
 * as the app having forgotten who they are.
 *
 * The fix is structural rather than cosmetic. Every persisted source below is a
 * `StateFlow<T?>` seeded with `null`, `null` means "DataStore has not answered
 * yet", and [state] stays [AppState.Loading] until *every* one of them has. The
 * `initialValue` of the `stateIn` is [AppState.Loading] and never a
 * default-constructed [AppState.Ready] — passing a defaults snapshot there
 * reintroduces the same flash one layer down, where it is harder to notice.
 *
 * ### Why the combines are nested
 *
 * `combine` has typed overloads only up to five flows. Beyond that the only
 * option is the `Array<Any?>` vararg, which erases every type and turns the
 * null-checks that implement the Loading gate into unchecked casts. Nesting
 * into intermediate bundles keeps the whole thing type-checked.
 */
class AppStateRepository(
    private val device: DeviceRepository,
    private val profileRepo: ProfileRepository,
    private val safetyRepo: SafetyRepository,
    private val messagingRepo: MessagingRepository,
    private val zoneRepo: ZoneRepository,
    private val journeyRepo: JourneyRepository,
    scope: CoroutineScope
) {

    private val identity: Flow<Identity?> = combine(
        profileRepo.profile,
        profileRepo.appearance,
        profileRepo.pairedDevices,
        profileRepo.smsAllowlist,
        profileRepo.devicePhoneNumber
    ) { profile, appearance, paired, allowlist, phone ->
        // A single null anywhere means this bundle is not ready. Returning null
        // rather than filling in a default is what keeps the gate honest.
        if (profile == null || appearance == null || paired == null ||
            allowlist == null || phone == null
        ) {
            null
        } else {
            Identity(
                role = profile.role,
                medicalId = profile.medicalId,
                deviceSettings = profile.deviceSettings,
                activeMode = profile.activeMode,
                darkMode = appearance.darkMode,
                onboardingSeen = appearance.onboardingSeen,
                pairedDevices = paired,
                smsAllowlist = allowlist,
                devicePhoneNumber = phone
            )
        }
    }

    private val safetyBundle: Flow<SafetyBundle?> = combine(
        safetyRepo.settings,
        safetyRepo.history,
        safetyRepo.checkIns,
        safetyRepo.activeAlert
    ) { settings, history, checkIns, active ->
        if (settings == null || history == null || checkIns == null) null
        else SafetyBundle(settings, history, checkIns, active)
    }

    private val activityBundle: Flow<ActivityBundle?> = combine(
        messagingRepo.messages,
        zoneRepo.zones,
        journeyRepo.journey,
        journeyRepo.loaded,
        zoneRepo.lastTransition
    ) { messages, zones, journey, journeyLoaded, transition ->
        // journey is legitimately null when idle, so its own `loaded` flag is
        // what decides readiness here — not the value.
        if (messages == null || zones == null || !journeyLoaded) null
        else ActivityBundle(messages, zones, journey, transition)
    }

    private val deviceCore: Flow<DeviceCore> = combine(
        device.connection,
        device.telemetry,
        device.rssiSmoothed,
        device.isRinging,
        device.syncStatus
    ) { connection, telemetry, rssi, ringing, sync ->
        DeviceCore(connection, telemetry, rssi, ringing, sync)
    }

    private val deviceLive: Flow<DeviceLive> = combine(
        deviceCore,
        device.history,
        device.lastKnownDeviceLocation
    ) { core, history, location ->
        DeviceLive(core, history, location)
    }

    /**
     * The single source the UI collects.
     *
     * Live device state is not part of the Loading gate on purpose: it is not
     * read from disk, it has a genuine value from the first instant
     * (Disconnected, no telemetry), and blocking the first frame on a BLE scan
     * would mean the app shows a spinner until a device happens to be nearby.
     */
    val state: StateFlow<AppState> = combine(
        identity,
        safetyBundle,
        activityBundle,
        deviceLive
    ) { identity, safety, activity, live ->
        if (identity == null || safety == null || activity == null) {
            AppState.Loading
        } else {
            AppState.Ready(
                role = identity.role,
                medicalId = identity.medicalId,
                deviceSettings = identity.deviceSettings,
                activeMode = identity.activeMode,
                darkMode = identity.darkMode,
                onboardingSeen = identity.onboardingSeen,
                pairedDevices = identity.pairedDevices,
                smsAllowlist = identity.smsAllowlist,
                devicePhoneNumber = identity.devicePhoneNumber,
                safetySettings = safety.settings,
                tripHistory = safety.history,
                checkIns = safety.checkIns,
                activeAlert = safety.activeAlert,
                messages = activity.messages,
                zones = activity.zones,
                journey = activity.journey,
                lastZoneTransition = activity.lastTransition,
                connection = live.core.connection,
                telemetry = live.core.telemetry,
                rssiSmoothed = live.core.rssiSmoothed,
                isRinging = live.core.isRinging,
                syncStatus = live.core.syncStatus,
                telemetryHistory = live.history,
                lastKnownDeviceLocation = live.lastKnownDeviceLocation
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, AppState.Loading)

    // ---- Intermediate bundles. Private; they exist only to stay inside the
    // five-flow limit of the typed `combine` overloads. ----

    private data class Identity(
        val role: UserRole,
        val medicalId: MedicalId,
        val deviceSettings: DeviceSettings,
        val activeMode: PersonaMode,
        val darkMode: DarkModePreference,
        val onboardingSeen: Boolean,
        val pairedDevices: List<PairedDevice>,
        val smsAllowlist: List<String>,
        val devicePhoneNumber: String
    )

    private data class SafetyBundle(
        val settings: SafetySettings,
        val history: List<FallAlertEvent>,
        val checkIns: List<CheckInRequest>,
        val activeAlert: FallAlertEvent?
    )

    private data class ActivityBundle(
        val messages: List<QuickMessage>,
        val zones: List<GeofenceZone>,
        val journey: Journey?,
        val lastTransition: ZoneTransition?
    )

    private data class DeviceCore(
        val connection: ConnectionState,
        val telemetry: LiveSensorData,
        val rssiSmoothed: Int,
        val isRinging: Boolean,
        val syncStatus: SyncStatus
    )

    private data class DeviceLive(
        val core: DeviceCore,
        val history: List<TelemetryPoint>,
        val lastKnownDeviceLocation: LocationState?
    )
}

/**
 * What the UI renders.
 *
 * [Loading] is not "briefly, on a slow phone". It is the correct state until
 * DataStore has answered for every persisted source, and a screen must render
 * a neutral placeholder for it rather than a defaults-shaped guess.
 */
sealed interface AppState {

    data object Loading : AppState

    data class Ready(
        // Identity
        val role: UserRole,
        val medicalId: MedicalId,
        val deviceSettings: DeviceSettings,
        val activeMode: PersonaMode,
        val darkMode: DarkModePreference,
        val onboardingSeen: Boolean,
        val pairedDevices: List<PairedDevice>,
        val smsAllowlist: List<String>,
        val devicePhoneNumber: String,

        // Safety
        val safetySettings: SafetySettings,
        val tripHistory: List<FallAlertEvent>,
        val checkIns: List<CheckInRequest>,
        val activeAlert: FallAlertEvent?,

        // Messaging, zones, journey
        val messages: List<QuickMessage>,
        val zones: List<GeofenceZone>,
        val journey: Journey?,
        val lastZoneTransition: ZoneTransition?,

        // Live link
        val connection: ConnectionState,
        val telemetry: LiveSensorData,
        val rssiSmoothed: Int,
        val isRinging: Boolean,
        val syncStatus: SyncStatus,
        val telemetryHistory: List<TelemetryPoint>,
        val lastKnownDeviceLocation: LocationState?
    ) : AppState {

        /** The only state in which a write to the wearable reaches it. */
        val canWriteToDevice: Boolean get() = connection is ConnectionState.Ready
    }
}
