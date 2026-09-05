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
import com.safeshade.data.DarkModePreference
import com.safeshade.data.DeviceSettings
import com.safeshade.data.EmergencyContact
import com.safeshade.data.GeofenceZone
import com.safeshade.data.LedPattern
import com.safeshade.data.LocationState
import com.safeshade.data.MedicalId
import com.safeshade.data.PersonaMode
import com.safeshade.data.SafetySettings
import com.safeshade.data.TripOutcome
import com.safeshade.data.UserRole
import com.safeshade.data.WeatherUiState
import com.safeshade.device.ConnectionState
import com.safeshade.device.DeviceProtocol
import com.safeshade.di.AppContainer
import com.safeshade.repo.AppState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

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

    /** Re-checked on resume, since the user may have granted from system settings. */
    fun refreshPermissions() {
        _permissionsGranted.value = hasLinkPermissions()
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
                                capturedAt = System.currentTimeMillis()
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
    fun setActiveMode(mode: PersonaMode) = launchIo { container.deviceRepository.setMode(mode) }
    fun setDeviceName(name: String) = launchIo { container.deviceRepository.setDeviceName(name) }
    fun setLedPattern(pattern: LedPattern) = launchIo { container.deviceRepository.setLed(pattern) }

    // ============================================
    // Safety
    // ============================================

    fun setSafetySettings(settings: SafetySettings) =
        launchIo { container.safetyRepository.setSettings(settings) }

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
    // Messaging
    // ============================================

    fun sendGuardianMessage(text: String) =
        launchIo { container.messagingRepository.sendGuardianMessage(text) }

    fun sendCompanionReply(text: String) =
        launchIo { container.messagingRepository.sendCompanionReply(text) }

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
