package com.safeshade.ui.vm

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.gms.location.LocationServices
import com.safeshade.SafeShadeApplication
import com.safeshade.WeatherService
import com.safeshade.AirQualityService
import com.safeshade.AirQualityMapper
import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceSettings
import com.safeshade.data.EmergencyContact
import com.safeshade.data.FallAlertEvent
import com.safeshade.data.GeofenceZone
import com.safeshade.data.LedPattern
import com.safeshade.data.LocationState
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.data.SosBlocker
import com.safeshade.data.SosOutcome
import com.safeshade.data.TripKind
import com.safeshade.data.TripOutcome
import com.safeshade.data.UserRole
import com.safeshade.data.WeatherUiState
import com.safeshade.data.Wearer
import com.safeshade.data.WearerResult
import com.safeshade.data.contactsFor
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceProtocol
import com.safeshade.di.AppContainer
import com.safeshade.emergencyAlertText
import com.safeshade.repo.AppState
import com.safeshade.repo.SendResult
import com.safeshade.sendEmergencySms
import com.safeshade.service.LastKnownLocation
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The single view model for the app.
 *
 * One rather than a dozen, because every screen here is a view onto the same
 * device and the same person — splitting that into per-screen view models would
 * mean each one re-deriving the same connection state and re-plumbing the same
 * repositories. Screens still receive plain UI-state objects and callbacks, so
 * they stay testable and previewable without this class.
 *
 * It deliberately owns no state of its own beyond the transient bits that only
 * matter while the UI is alive (weather, a location fix, an in-flight sync).
 * Everything durable lives in the repositories, on an application-scoped
 * coroutine scope, so a fall alert still fires with no view model and no
 * Activity in existence.
 */
class SafeShadeViewModel(
    application: Application,
    private val container: AppContainer
) : AndroidViewModel(application) {

    val appState: StateFlow<AppState> = container.appStateRepository.state

    // ============================================
    // Transient UI-only state
    // ============================================

    private val _weather = MutableStateFlow(WeatherUiState())
    val weather = _weather.asStateFlow()

    private val _location = MutableStateFlow(LocationState())
    val location = _location.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _permissionsGranted = MutableStateFlow(hasLinkPermissions())
    val permissionsGranted = _permissionsGranted.asStateFlow()

    /**
     * Whether this app may send a text.
     *
     * Separate from [permissionsGranted] rather than folded into it, and that
     * separation is load-bearing. `hasLinkPermissions` reads only the Bluetooth
     * and location grants, and `MutableStateFlow` conflates equal values — so
     * granting SEND_SMS from the system dialog changed none of those three keys
     * and emitted nothing. Anything keyed on [permissionsGranted] alone stayed
     * stale, which meant the SOS could never arm on a fresh install: the only
     * path to arming it is the very grant that failed to notify.
     */
    private val _smsGranted = MutableStateFlow(hasSmsPermission())
    val smsGranted = _smsGranted.asStateFlow()

    /** Re-checked on resume, since the user may have granted from system settings. */
    fun refreshPermissions() {
        _permissionsGranted.value = hasLinkPermissions()
        _smsGranted.value = hasSmsPermission()
    }

    private fun hasLinkPermissions(): Boolean {
        val ctx = getApplication<Application>()
        fun granted(p: String) =
            ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            granted(Manifest.permission.BLUETOOTH_SCAN) &&
                granted(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            granted(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // ============================================
    // Link
    // ============================================

    fun connect() {
        if (!hasLinkPermissions()) return
        container.deviceRepository.startScan()
    }

    /**
     * The address of the device currently on the other end of the link.
     *
     * Blank when there is none. `AppState` records only *that* there is a link,
     * not which device it is to, which is why the paired-devices list marked
     * every saved device "Saved" even while connected to one of them.
     */
    val connectedAddress: StateFlow<String> = container.deviceRepository.deviceAddress

    /**
     * Connects, preferring a specific saved device.
     *
     * The scan is narrowed to that one address, so tapping Connect on a saved
     * device joins *that* device rather than whichever one answers first. Both
     * rows in a two-device household used to do the identical thing.
     */
    fun connectTo(address: String) {
        if (!hasLinkPermissions()) return
        container.deviceRepository.startScan(preferredAddress = address)
    }

    fun disconnect() = container.deviceRepository.disconnect()

    /**
     * Rings the wearable so it can be found.
     *
     * Loud and not stoppable from here: the firmware puts the device on its full
     * SOS screen with the siren running and clears only on a physical button
     * press. The repository suppresses weather sync for the duration, because
     * `CMD_FIND` and weather share one characteristic.
     */
    fun ringDevice() = container.deviceRepository.ringDevice()

    fun acknowledgeRingStopped() = container.deviceRepository.acknowledgeRingStopped()

    // ============================================
    // Weather + location
    // ============================================

    /**
     * Fetches a location fix and the forecast for it, then pushes both to the
     * wearable in one payload.
     *
     * Open-Meteo is the app's only network dependency, and nothing safety
     * critical depends on it — a failure here leaves the previous reading in
     * place and the link untouched.
     */
    fun syncWeather() {
        if (_isSyncing.value) return
        val ctx = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val fix = lastKnownLocation() ?: return@launch
                _location.value = fix
                container.deviceRepository.recordDeviceLocation(fix)

                val response = runCatching {
                    WeatherService.api.getWeather(fix.lat, fix.lon)
                }.getOrNull() ?: return@launch

                val hourIndex = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    .coerceIn(0, response.hourly.uv_index.lastIndex.coerceAtLeast(0))

                val state = WeatherUiState(
                    rainChance = response.hourly.precipitation_probability.getOrElse(hourIndex) { 0 },
                    condition = conditionFor(response.current.weather_code),
                    uvIndex = response.hourly.uv_index.getOrElse(hourIndex) { 0f },
                    humidity = response.hourly.relative_humidity_2m.getOrElse(hourIndex) { 0 }.toFloat(),
                    temp = response.current.temperature_2m,
                    isLoaded = true,
                    lastSyncTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                )
                _weather.value = state

                // Air quality rides alongside; a failure leaves the last reading.
                runCatching { AirQualityService.api.getAirQuality(fix.lat, fix.lon) }
                    .getOrNull()?.let { _airQuality.value = AirQualityMapper.map(it) }

                val now = Calendar.getInstance()
                container.deviceRepository.syncWeather(
                    DeviceProtocol.weather(
                        rainChance = state.rainChance,
                        condition = state.condition,
                        uvIndex = state.uvIndex,
                        humidity = state.humidity,
                        lat = fix.lat,
                        lon = fix.lon,
                        locationName = fix.locationName,
                        locality = fix.locality,
                        altitude = fix.altitude,
                        hour = now.get(Calendar.HOUR_OF_DAY),
                        minute = now.get(Calendar.MINUTE)
                    )
                )
            } finally {
                _isSyncing.value = false
            }
        }
    }

    @Suppress("MissingPermission")
    private suspend fun lastKnownLocation(): LocationState? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(getApplication())
        runCatching {
            client.lastLocation
                .addOnSuccessListener { loc ->
                    cont.resume(
                        loc?.let {
                            LocationState(
                                lat = it.latitude,
                                lon = it.longitude,
                                altitude = it.altitude.toInt(),
                                isValid = true,
                                capturedAt = System.currentTimeMillis(),
                                provider = it.provider,
                                accuracyM = if (it.hasAccuracy()) it.accuracy else null,
                                fixAt = it.time
                            )
                        }
                    )
                }
                .addOnFailureListener { cont.resume(null) }
        }.onFailure { cont.resume(null) }
    }

    /** WMO weather codes, collapsed to words the device's small screen can show. */
    private fun conditionFor(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        in 95..99 -> "Thunderstorm"
        else -> "Unsettled"
    }

    // ============================================
    // Profile
    // ============================================

    fun setRole(role: UserRole) = launchIo { container.profileRepository.setRole(role) }
    fun setMedicalId(id: MedicalId) = launchIo { container.profileRepository.setMedicalId(id) }
    fun setDarkMode(pref: DarkModePreference) = launchIo { container.profileRepository.setDarkMode(pref) }
    fun setOnboardingSeen(seen: Boolean) = launchIo { container.profileRepository.setOnboardingSeen(seen) }
    /**
     * Stores the profile, then sends it if the link is usable.
     *
     * The store comes first, and unconditionally: `pushAll` re-sends the
     * *stored* mode on every reconnect, so the store is what makes "chosen
     * now, sent when the device connects" a promise the app keeps. Until this
     * wrote it, nothing did — the mode went to the radio and nowhere else, and
     * a choice made with no device in range vanished.
     *
     * Returns whether the wearable acknowledged. `false` on a dead link means
     * "stored, not sent", which the caller can tell from the connection state;
     * `false` on a live link means the ack window elapsed.
     */
    fun setActiveMode(mode: PersonaMode): Deferred<Boolean> = viewModelScope.async {
        container.profileRepository.setActiveMode(mode)
        val usable = appState.value.readyOrNull?.connection?.isUsable == true
        if (!usable) return@async false
        container.deviceRepository.setMode(mode)
    }
    fun setDeviceName(name: String) = launchIo { container.deviceRepository.setDeviceName(name) }
    fun setLedPattern(pattern: LedPattern) = launchIo { container.deviceRepository.setLed(pattern) }

    // ============================================
    // Safety
    // ============================================

    fun setSafetySettings(settings: SafetySettings) =
        launchIo { container.safetyRepository.setSettings(settings) }

    /** The ladder climbing an open alert right now, or null. */
    val escalationRun: StateFlow<com.safeshade.service.EscalationRun?> = container.escalationRunner.run

    /** What the wearable watch believes this minute. */
    val watchState: StateFlow<com.safeshade.service.WatchState> = container.wearableWatch.state

    // ============================================
    // Rides, leash, air, launcher actions
    // ============================================

    val rides: StateFlow<List<com.safeshade.data.Ride>?> = container.rideLogRepository.rides
    fun clearRides() = launchIo { container.rideLogRepository.clear() }

    val leash: StateFlow<com.safeshade.platform.LeashState> = container.deviceRepository.leash

    private val _airQuality = MutableStateFlow<com.safeshade.platform.AirQualityReading?>(null)
    /** The newest Open-Meteo air-quality reading for the last fix, or null. */
    val airQuality = _airQuality.asStateFlow()

    /**
     * An action a launcher shortcut or the Quick Settings tile asked for:
     * "com.safeshade.action.SOS", CHECK_IN or LOCATE. The app graph consumes
     * it once and clears it. The tile never fires anything itself.
     */
    private val _launchAction = MutableStateFlow<String?>(null)
    val launchAction = _launchAction.asStateFlow()
    fun onLaunchAction(action: String?) { if (action != null && action.startsWith("com.safeshade.action.")) _launchAction.value = action }
    fun consumeLaunchAction() { _launchAction.value = null }

    // ============================================
    // Sightings and lost mode
    // ============================================

    val sightings: StateFlow<List<com.safeshade.data.Sighting>?> = container.sightingsRepository.recent
    val lostDevices: StateFlow<List<com.safeshade.data.LostMode>?> = container.sightingsRepository.lost
    val ownLastSeen: StateFlow<Map<String, Long>> = container.sightingsRepository.ownLastSeen
    fun markLost(address: String, label: String) = launchIo { container.sightingsRepository.markLost(address, label) }
    fun markFound(address: String) = launchIo { container.sightingsRepository.markFound(address) }
    /** A twenty-second low-power listen for any SafeShade nearby. Needs the scan permission; the link logs a refusal. */
    fun sweepForSightings() = container.sightingsRepository.startSweep()

    // ============================================
    // Smart home
    // ============================================

    val smartHooks: StateFlow<List<com.safeshade.data.SmartHomeHook>?> = container.smartHomeRepository.hooks
    val smartFirings: StateFlow<List<com.safeshade.data.SmartHomeFiring>?> = container.smartHomeRepository.firings
    fun validateHookUrl(url: String): String? = container.smartHomeRepository.validateUrl(url)
    suspend fun saveHook(hook: com.safeshade.data.SmartHomeHook, isNew: Boolean) {
        if (isNew) container.smartHomeRepository.add(hook) else container.smartHomeRepository.update(hook)
    }
    fun removeHook(id: String) = launchIo { container.smartHomeRepository.removeById(id) }
    fun setHookEnabled(hook: com.safeshade.data.SmartHomeHook, enabled: Boolean) =
        launchIo { container.smartHomeRepository.update(hook.copy(enabled = enabled)) }
    /** Posts a real test event to the hook and returns what the endpoint answered. */
    suspend fun testHook(hook: com.safeshade.data.SmartHomeHook): com.safeshade.platform.WebhookResult {
        val ready = appState.value.readyOrNull
        val event = com.safeshade.platform.SmartHomeEvent(
            trigger = hook.trigger,
            wearerName = ready?.selectedWearer?.name?.takeIf { it.isNotBlank() } ?: ready?.deviceSettings?.wearerName.orEmpty(),
            at = System.currentTimeMillis(),
            detail = "Test from the SafeShade app"
        )
        val result = com.safeshade.platform.SmartHomeApps.post(hook, event, container.httpClient)
        val (code, error) = when (result) {
            is com.safeshade.platform.WebhookResult.Delivered -> result.statusCode to null
            is com.safeshade.platform.WebhookResult.Rejected -> result.statusCode to "Rejected: ${result.bodySnippet}"
            is com.safeshade.platform.WebhookResult.Unreachable -> null to result.reason
        }
        container.smartHomeRepository.recordFiring(hook.id, code, error)
        return result
    }

    // ============================================
    // Firmware
    // ============================================

    val otaStep: StateFlow<com.safeshade.device.OtaProtocol.OtaStep> = container.firmwareRepository.state
    val firmwareReleases: StateFlow<List<com.safeshade.data.FirmwareRelease>?> = container.firmwareRepository.releases
    val installedFirmware: StateFlow<String?> = container.firmwareRepository.installedVersion
    suspend fun checkFirmware(model: com.safeshade.data.DeviceModel) = container.firmwareRepository.check(model)
    suspend fun queryFirmwareVersion() = container.firmwareRepository.queryVersion()
    suspend fun downloadFirmware(release: com.safeshade.data.FirmwareRelease) = container.firmwareRepository.downloadAndVerify(release)
    suspend fun installFirmware(release: com.safeshade.data.FirmwareRelease) =
        container.firmwareRepository.install(release, mtu = 185)

    /** Where the community last heard each of the given addresses, newest first per address. */
    suspend fun communityLastSeen(addresses: List<String>): Map<String, com.safeshade.cloud.dto.DeviceSightingRow> {
        if (addresses.isEmpty()) return emptyMap()
        return when (val r = container.sightingsCloud.lastSeen(addresses)) {
            is com.safeshade.cloud.CloudResult.Ok -> r.value
                .filter { it.bleAddress != null }
                .groupBy { it.bleAddress!!.uppercase() }
                .mapValues { (_, rows) -> rows.maxByOrNull { it.seenAt.orEmpty() }!! }
            else -> emptyMap()
        }
    }

    /** Reports every unreported sighting to the community, once per address per ten minutes (the cloud rate-limits too). */
    fun reportSightings() = launchIo {
        val pending = container.sightingsRepository.unreported()
        val done = mutableListOf<Pair<String, Long>>()
        for (s in pending) {
            val r = container.sightingsCloud.report(s.address, s.rssi, s.lat, s.lon, s.accuracyM, s.at)
            if (r is com.safeshade.cloud.CloudResult.Ok) done += s.address to s.at
        }
        if (done.isNotEmpty()) container.sightingsRepository.markReported(done)
    }

    // ============================================
    // Navigation target and NFC
    // ============================================

    /** Sends a place to the wearable's navigation screen; true when the device acknowledged. */
    fun guideTo(lat: Double, lon: Double, label: String): Deferred<Boolean> =
        viewModelScope.async { container.deviceRepository.setNavTarget(lat, lon, label) }

    /** Whether the Activity should hold NFC reader mode open for a tag write. */
    private val _nfcArmed = MutableStateFlow(false)
    val nfcArmed = _nfcArmed.asStateFlow()
    fun armNfcWrite(armed: Boolean) { _nfcArmed.value = armed; if (armed) _nfcResult.value = null }

    /** The last tag write's outcome sentence, or null. */
    private val _nfcResult = MutableStateFlow<String?>(null)
    val nfcResult = _nfcResult.asStateFlow()

    /** Called by the Activity from reader mode. Writes the emergency card and disarms. */
    fun onNfcTag(tag: android.nfc.Tag) {
        val ready = appState.value.readyOrNull ?: return
        val id = ready.medicalId
        val summary = listOfNotNull(
            id.bloodType.takeIf { it.isNotBlank() }?.let { "Blood $it" },
            id.allergies.takeIf { it.isNotBlank() }?.let { "Allergies: $it" },
            id.conditions.takeIf { it.isNotBlank() }?.let { "Conditions: $it" },
            id.medications.takeIf { it.isNotBlank() }?.let { "Medication: $it" }
        ).joinToString(". ")
        val name = ready.selectedWearer?.name?.takeIf { it.isNotBlank() } ?: ready.deviceSettings.wearerName
        val message = com.safeshade.platform.NfcTagWriter.payloadFor(name, summary, id.emergencyContact.takeIf { it.isNotBlank() })
        _nfcResult.value = when (val r = com.safeshade.platform.NfcTagWriter.write(tag, message)) {
            is com.safeshade.platform.NfcWriteResult.Written -> "Written: ${r.bytes} bytes on the tag."
            is com.safeshade.platform.NfcWriteResult.TooSmall -> "The tag holds ${r.capacity} bytes; the card needs ${r.needed}."
            com.safeshade.platform.NfcWriteResult.ReadOnly -> "That tag is read-only."
            is com.safeshade.platform.NfcWriteResult.Failed -> r.reason
        }
        _nfcArmed.value = false
    }

    // ============================================
    // Vitals
    // ============================================

    val vitalsSamples: StateFlow<List<com.safeshade.data.VitalsSample>?> = container.vitalsRepository.samples
    val vitalsThresholds: StateFlow<com.safeshade.data.VitalsThresholds> = container.vitalsRepository.thresholds
    val vitalsFlags: StateFlow<List<com.safeshade.data.VitalsFlag>> =
        container.vitalsRepository.latestFlags.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setVitalsThresholds(t: com.safeshade.data.VitalsThresholds) =
        launchIo { container.vitalsRepository.setThresholds(t) }

    /** Health Connect as the phone sees it right now. Re-evaluated on every call because installing it changes the answer. */
    fun healthConnectAvailability(): com.safeshade.platform.Availability = container.healthConnectVitals.availability()

    fun healthConnectContract() = container.healthConnectVitals.permissionsContract()
    val healthConnectPermissions: Set<String> get() = container.healthConnectVitals.requiredPermissions
    suspend fun healthConnectGranted(): Boolean =
        container.healthConnectVitals.grantedPermissions().containsAll(container.healthConnectVitals.requiredPermissions)

    /**
     * Reads the newest vitals from Health Connect and stores them. Returns the
     * failure reason, or null when the read worked (a read that found nothing
     * new is a success with nothing to say).
     */
    suspend fun readHealthConnect(wearerId: String?): String? {
        val since = System.currentTimeMillis() - 24 * 3_600_000L
        return when (val r = container.healthConnectVitals.latest(since)) {
            is com.safeshade.platform.VitalsResult.Readings -> {
                container.vitalsRepository.recordFromPhone(r, wearerId); null
            }
            is com.safeshade.platform.VitalsResult.Failed -> r.reason
            com.safeshade.platform.VitalsResult.NotAvailable -> "Health Connect is not available on this phone."
            com.safeshade.platform.VitalsResult.NoPermission -> "SafeShade has not been allowed to read from Health Connect."
        }
    }

    // ============================================
    // Evidence
    // ============================================

    val evidenceClips: StateFlow<List<com.safeshade.data.EvidenceClip>?> = container.evidenceRepository.clips
    val evidenceSettings: StateFlow<com.safeshade.data.EvidenceSettings> = container.evidenceSettings
    val evidenceService: StateFlow<com.safeshade.service.EvidenceServiceState> = com.safeshade.service.EvidenceService.state

    fun setEvidenceSettings(settings: com.safeshade.data.EvidenceSettings) =
        launchIo { container.evidenceRepository.setSettings(settings) }

    fun deleteEvidence(id: String) = launchIo { container.evidenceRepository.delete(id) }
    fun hasEvidenceFor(alertId: String): Boolean = container.evidenceRepository.forAlert(alertId).isNotEmpty()

    /**
     * Opens the microphone for [seconds], tied to [alertId]. Only ever called
     * from a surface the person is looking at (the alert banner, the SOS
     * outcome, the Evidence page), because on Android 14 a microphone service
     * cannot start from the background. Returns false with the reason logged
     * when the permission is missing or the start was refused.
     */
    fun startEvidence(alertId: String?, seconds: Int = container.evidenceSettings.value.durationSeconds): Boolean =
        com.safeshade.service.EvidenceService.start(getApplication(), alertId, seconds)

    fun stopEvidence() = com.safeshade.service.EvidenceService.stop(getApplication())

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Records a finished voice note. See VoiceNoteRepository.add. */
    fun addVoiceNote(
        fileName: String,
        durationMs: Int,
        waveform: List<Float>,
        wearerId: String?,
        fromGuardian: Boolean,
        authorName: String
    ) = launchIo {
        container.voiceNoteRepository.add(fileName, durationMs, waveform, wearerId, fromGuardian, authorName)
    }

    fun markVoiceNoteListened(id: String) = launchIo { container.voiceNoteRepository.markListened(id) }

    fun setEscalation(settings: com.safeshade.data.EscalationSettings) =
        launchIo { container.safetyRepository.setEscalation(settings) }

    fun setWatchThresholds(offlineAlertMinutes: Int, lowBatteryPercent: Int) =
        launchIo { container.safetyRepository.setWatchThresholds(offlineAlertMinutes, lowBatteryPercent) }

    fun addContact(contact: EmergencyContact) =
        launchIo { container.safetyRepository.addContact(contact) }

    fun removeContact(phone: String) =
        launchIo { container.safetyRepository.removeContact(phone) }

    /**
     * Closes out the trip currently on screen.
     *
     * Takes the outcome explicitly rather than inferring it, because "the user
     * pressed I'm OK" and "the countdown expired and we called someone" are
     * genuinely different entries in a history a guardian may read months later.
     */
    fun resolveActiveAlert(outcome: TripOutcome, contacted: Boolean = false) = launchIo {
        val id = appState.value.readyOrNull?.activeAlert?.id ?: return@launchIo
        container.safetyRepository.resolveAlert(id, outcome, contacted)
        container.safetyRepository.clearActiveAlert()
    }

    // ============================================
    // SOS raised from this phone
    // ============================================

    private val _sosOutcome = MutableStateFlow<SosOutcome?>(null)

    /**
     * What the last phone SOS actually did.
     *
     * Durable state rather than a one-shot event, and that is forced by the
     * surface that shows it: recording the trip raises the full-frame
     * `TripBanner` *above* the Scaffold, so it is already covering anything a
     * transient snackbar could have used. The banner has to be able to read
     * this whenever it recomposes, not once when it happens.
     */
    val sosOutcome = _sosOutcome.asStateFlow()

    fun clearSosOutcome() { _sosOutcome.value = null }

    /**
     * Everyone a phone SOS would reach right now.
     *
     * The global emergency contacts plus anything extra recorded against the
     * wearer whose page is on screen, unioned on the normalised number so a
     * contact saved in both places is texted once. The selected wearer rather
     * than the connected one, because a phone SOS is raised *about the person
     * the guardian is looking at* - the wearable may be in a drawer, or may be
     * a second person's.
     *
     * One function feeding all three of the synchronous SOS checks, so the
     * control can never arm against one list and send against another.
     */
    private fun sosContacts(ready: AppState.Ready) =
        ready.safetySettings.contactsFor(ready.selectedWearer)

    /** True when the SOS control can be armed. Checked at press, never at completion. */
    fun canFireSos(): Boolean {
        val ready = appState.value.readyOrNull ?: return false
        return sosContacts(ready).isNotEmpty() &&
            hasSmsPermission() &&
            ready.activeAlert == null
    }

    /** Why it cannot, so the caller can offer the fix rather than just refusing. */
    fun sosBlocker(): SosBlocker? {
        val ready = appState.value.readyOrNull ?: return SosBlocker.NOT_READY
        return when {
            ready.activeAlert != null -> SosBlocker.ALERT_ALREADY_LIVE
            sosContacts(ready).isEmpty() -> SosBlocker.NO_CONTACT
            !hasSmsPermission() -> SosBlocker.NO_SMS_PERMISSION
            else -> null
        }
    }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Sends the alert.
     *
     * Order matters and is not arbitrary. The trip is recorded *before* anything
     * is sent, for the reason `SafetyRepository.record` spells out: a process
     * killed mid-send should leave an alert that has to be re-surfaced from
     * history, never one that fired and was never written down.
     *
     * Two channels, honestly ranked:
     *  - **SMS to every emergency contact** is the one that actually reaches a
     *    person, and is the reason the SEND_SMS grant is checked before the
     *    hold is even armed.
     *  - **The wearable** gets the same text, which makes it buzz and display
     *    the alert. That matters when the wearer is nearby and does not know
     *    help has been called. It does *not* relay onward to the contact —
     *    there is no wire tag for that (see docs/handoff3.md), and inventing
     *    one here would be an app feature depending on firmware that does not
     *    exist.
     */
    fun firePhoneSos() = launchIo {
        val ready = appState.value.readyOrNull ?: return@launchIo
        val contacts = sosContacts(ready)
        if (contacts.isEmpty()) {
            _sosOutcome.value = SosOutcome.NoContact
            return@launchIo
        }

        // Recorded now, queued for the cloud later. `sync = false` is the whole
        // of it: enqueueing is a DataStore write, and nothing between the tap
        // and the outcome the person is staring at may be a write this phone
        // could have deferred. handoff7 section 6 item 5.
        val alert = FallAlertEvent(kind = TripKind.PHONE_SOS, note = "Raised from the phone")
        container.safetyRepository.record(alert, sync = false)

        // The microphone, if the person armed it for an SOS. The tap that
        // fired this came from a screen in front of them, which is the one
        // condition the service needs; a refused start is logged and nothing
        // else here waits on it.
        if (container.evidenceSettings.value.recordOnSos) startEvidence(alert.id)

        val fix = LastKnownLocation.state.value?.takeIf { it.isValid }
            ?: _location.value.takeIf { it.isValid }
        val body = emergencyAlertText(
            // The selected wearer's name, falling back to the mirrored legacy
            // field so a store written before wearers existed still names
            // somebody rather than saying "the wearer".
            wearerName = ready.selectedWearer?.name?.takeIf { it.isNotBlank() }
                ?: ready.deviceSettings.wearerName,
            what = "SOS raised from the phone",
            lat = fix?.lat,
            lon = fix?.lon
        )

        val ctx = getApplication<Application>()
        val results = contacts.map { it to sendEmergencySms(ctx, it, body) }
        val onDevice = ready.connection.isUsable &&
            runCatching { container.messagingRepository.sendGuardianMessage(body) }.isSuccess

        _sosOutcome.value = SosOutcome.Sent(
            reached = results.filter { it.second.succeeded }.map { it.first.name },
            failed = results.filterNot { it.second.succeeded }.map { it.first.name },
            onDevice = onDevice,
            hasLocation = fix != null
        )

        // After the outcome, never before.
        container.safetyRepository.syncAlert(alert.id)
    }

    // ============================================
    // Messaging
    // ============================================

    /**
     * Sends a message as [role], and hands back what actually happened.
     *
     * The two `launchIo` calls this replaces threw away a [SendResult] whose
     * `Failed.reason` is written to be read by a person, so a send that
     * reached nobody was indistinguishable at the call site from one that
     * arrived - and the UI above them had just started drawing a tick for it.
     * `MessagingRepository` itself says a message that reports success and
     * reaches nobody is worse than one that reports failure; every caller was
     * making it do exactly that.
     *
     * The coroutine belongs to the view model rather than to the caller, and
     * that is the point of returning a [Deferred] instead of taking a
     * `suspend` shape. The repository writes the message into history *after*
     * the transport has taken it, so a screen that owned the job and was then
     * navigated away from would send the message and lose the record of it.
     * Awaiting this can be cancelled freely; the send underneath it cannot.
     */
    fun sendMessage(role: UserRole, text: String): Deferred<SendResult> =
        viewModelScope.async {
            when (role) {
                UserRole.GUARDIAN -> container.messagingRepository.sendGuardianMessage(text)
                UserRole.COMPANION -> container.messagingRepository.sendCompanionReply(text)
            }
        }

    // ============================================
    // Zones and journeys
    // ============================================

    fun addZone(zone: GeofenceZone) = launchIo { container.zoneRepository.addZone(zone) }
    fun updateZone(zone: GeofenceZone) = launchIo { container.zoneRepository.updateZone(zone) }
    fun removeZone(id: String) = launchIo { container.zoneRepository.removeZone(id) }

    // ============================================
    // Device identity and on-device settings
    // ============================================

    /**
     * Persists who wears the device.
     *
     * Nearly every string in the Guardian-role app reads this field — "Baba is
     * covered", "Baba left a safe zone" — so leaving it in composition state
     * meant the app forgot the wearer's name on every cold start and quietly
     * fell back to "the wearer".
     */
    fun setDeviceSettings(settings: DeviceSettings) =
        launchIo { container.profileRepository.setDeviceSettings(settings) }

    fun setWearerName(name: String) = launchIo {
        val current = appState.value.readyOrNull?.deviceSettings ?: return@launchIo
        container.profileRepository.setDeviceSettings(current.copy(wearerName = name.trim()))
    }

    fun setWearerAvatar(avatarId: String) = launchIo {
        val current = appState.value.readyOrNull?.deviceSettings ?: return@launchIo
        container.profileRepository.setDeviceSettings(current.copy(wearerAvatarId = avatarId))
    }

    /**
     * Name and face together, in one write.
     *
     * Two consecutive `launchIo` setters each read `appState.value` before
     * either has landed, and the second overwrites the first with its stale
     * copy — the wearer's name was lost that way on the Profile edit page.
     */
    fun setWearer(name: String, avatarId: String) = launchIo {
        val current = appState.value.readyOrNull?.deviceSettings ?: return@launchIo
        container.profileRepository.setDeviceSettings(
            current.copy(wearerName = name.trim(), wearerAvatarId = avatarId)
        )
    }

    fun setOwner(name: String, avatarId: String) = launchIo {
        container.profileRepository.setOwner(name, avatarId)
    }

    // ============================================
    // The people this phone looks after
    // ============================================

    /*
     * Every one of these returns a Deferred<WearerResult> rather than firing
     * and forgetting. A refusal - the last person on the list, a companion
     * phone trying to add a second wearer - carries the sentence the UI has to
     * show, and a caller that could not see it would draw a tick for a change
     * that never happened. That is the outcome rule this app is built around.
     *
     * The coroutine belongs to the view model, not to the caller, so a screen
     * navigated away from mid-write still completes the write; awaiting the
     * Deferred can be cancelled freely.
     */

    fun addWearer(wearer: Wearer): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.addWearer(wearer) }

    fun updateWearer(wearer: Wearer): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.updateWearer(wearer) }

    fun removeWearer(id: String): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.removeWearer(id) }

    fun selectWearer(id: String): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.selectWearer(id) }

    /**
     * Binds a wearable to a person by its BLE address.
     *
     * The address, never `DeviceSettings.id`: that id is a UUID this app minted
     * for itself and says nothing about the hardware on the other end of the
     * link. Defaults to the address currently connected, which is what a
     * "this device is Baba's" control on a live link means.
     */
    fun bindDeviceToWearer(
        wearerId: String,
        address: String = connectedAddress.value
    ): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.bindDevice(wearerId, address) }

    fun unbindDeviceFromWearers(address: String): Deferred<WearerResult> =
        viewModelScope.async { container.profileRepository.unbindDevice(address) }

    fun setQuietHours(startHour: Int?, endHour: Int?) =
        launchIo { container.deviceRepository.setQuietHours(startHour, endHour) }

    fun setMedicationTime(hour: Int?, minute: Int?) =
        launchIo { container.deviceRepository.setMedicationTime(hour, minute) }

    fun setCheckInInterval(seconds: Int) =
        launchIo { container.deviceRepository.setCheckInInterval(seconds) }

    /**
     * Nothing to do, deliberately.
     *
     * [com.safeshade.repo.DeviceRepository] already polls RSSI once a second
     * while the link is Ready, so an explicit "read signal" would add a queued
     * GATT operation for a value that is at most a second old. Kept as a
     * documented no-op rather than deleted, because the telemetry screen offers
     * the affordance and a reader deserves to know why it does nothing.
     */
    fun readRssi() = Unit

    fun removePairedDevice(address: String) =
        container.profileRepository.removePairedDevice(address)

    // ============================================
    // SIM number and message allowlist
    // ============================================

    fun setDevicePhoneNumber(number: String) =
        launchIo { container.profileRepository.setDevicePhoneNumber(number) }

    fun setSmsAllowlist(numbers: List<String>) =
        launchIo { container.profileRepository.setSmsAllowlist(numbers) }

    // ============================================
    // Check-ins
    // ============================================

    fun requestCheckIn(withinMinutes: Int) =
        launchIo { container.safetyRepository.requestCheckIn(withinMinutes) }

    fun answerCheckIn(id: String) =
        launchIo { container.safetyRepository.answerCheckIn(id) }

    fun answerOpenCheckIn() = launchIo {
        val open = appState.value.readyOrNull?.checkIns?.firstOrNull { it.isOpen } ?: return@launchIo
        container.safetyRepository.answerCheckIn(open.id)
    }

    // ============================================
    // Journeys
    // ============================================

    fun startJourney(label: String, minutes: Int) = launchIo {
        container.journeyRepository.start(
            label = label,
            etaAt = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        )
    }

    fun arriveJourney() = launchIo { container.journeyRepository.arrive() }
    fun cancelJourney() = launchIo { container.journeyRepository.cancel() }

    // ============================================
    // Trips
    // ============================================

    /**
     * Closes out any trip by id, not only the live one.
     *
     * Without this a historical entry left `PENDING` — an alert the app
     * recorded while the phone was off, say — could never be answered, and the
     * board's "needs an answer" lamp would stay lit forever.
     */
    fun resolveTrip(eventId: String, outcome: TripOutcome, contacted: Boolean = false) =
        launchIo { container.safetyRepository.resolveAlert(eventId, outcome, contacted) }

    fun clearTripHistory() = launchIo { container.safetyRepository.clearHistory() }

    private fun launchIo(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    companion object {
        /**
         * Built from the Application rather than injected, because there is no
         * DI framework here on purpose: adding a Gradle plugin is the one class
         * of change this toolchain punishes, and a container held by the
         * Application is enough for a single-module app.
         */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as SafeShadeApplication
                SafeShadeViewModel(app, app.container)
            }
        }
    }
}

/** Convenience so screens can read a `Ready` snapshot without repeating the cast. */
val AppState.readyOrNull: AppState.Ready?
    get() = this as? AppState.Ready
