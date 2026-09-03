/**
 * SafeShade - Universal Safety Companion
 *
 * MainActivity.kt
 *
 * Entry point for the SafeShade Android application.
 * Handles permission requests, initializes BLE manager and location services,
 * and sets up the Compose UI.
 *
 * @author SafeShade Team
 * @version 2.2.0
 *
 * FEATURES (this pass):
 *  - Wired the app onto the real SafeShadeTheme (M3 + dark mode) instead of
 *    a hardcoded MaterialTheme(lightColorScheme(...)) - dark mode is now a
 *    real, persisted (DataStore-backed) user preference.
 *  - fetchAndSendWeather() now reads Open-Meteo's real relative_humidity_2m
 *    field instead of a hardcoded `65f // Simulated` value.
 *  - Gates the first run behind OnboardingScreen (data/Preferences.kt
 *    onboardingSeen flag).
 *
 * FIXES (earlier pass):
 *  - Permissions were previously requested via requestPermissionLauncher
 *    but the result was never checked (the callback body was empty), and
 *    the Compose UI (including the Home screen's connect Switch, which can
 *    call BleManager.startScanning() immediately) was built regardless of
 *    whether the user actually granted anything. On Android 13, calling
 *    BluetoothLeScanner.startScan() without a granted BLUETOOTH_SCAN
 *    permission throws a SecurityException and crashes the app - this was
 *    almost certainly the main crash source on-device. Permission state is
 *    now tracked and surfaced to the UI so BLE actions are only reachable
 *    once permissions are actually granted.
 *  - fetchAndSendWeather() called fusedLocationClient.lastLocation without
 *    checking ACCESS_FINE_LOCATION was granted; guarded now.
 */

package com.safeshade

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.*
import com.safeshade.data.*
import com.safeshade.ui.SafeShadeApp
import com.safeshade.ui.theme.SafeShadeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Main Activity - Application entry point
 *
 * Responsibilities:
 * - Initialize BLE manager for device communication
 * - Set up location services for GPS data
 * - Request necessary permissions (Bluetooth, Location)
 * - Fetch and sync weather data to device
 */
class MainActivity : ComponentActivity() {

    // BLE manager for device communication
    private lateinit var bleManager: BleManager

    // Location client for GPS functionality
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Weather state - observed by UI
    private var weatherState by mutableStateOf(WeatherUiState())

    // Location state - observed by UI
    private var locationState by mutableStateOf(LocationState())

    // Whether all Bluetooth + location permissions required for this app
    // are currently granted. The UI uses this to decide whether BLE
    // actions (scanning/connecting) are safe to trigger.
    private var permissionsGranted by mutableStateOf(false)

    private lateinit var preferences: SafeShadePreferences
    private var darkModePreference by mutableStateOf(DarkModePreference.SYSTEM)
    // null while the DataStore read is in flight, so we never flash the
    // onboarding flow for a returning user before we actually know.
    private var onboardingSeen by mutableStateOf<Boolean?>(null)

    /** The exact set of runtime permissions this app needs. */
    private val requiredPermissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            // Pre-Android 12: BLUETOOTH/BLUETOOTH_ADMIN are install-time
            // permissions, only fine location needs a runtime request.
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    private fun hasAllPermissions(): Boolean =
        requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    // Permission request launcher - now actually checks the result and
    // updates permissionsGranted so the UI reacts correctly instead of
    // assuming everything was granted.
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            permissionsGranted = results.values.all { it }
        }

    // Sensitive, opt-in-triggered permissions (auto-call, SMS fallback,
    // geofence background alerts, notifications) - requested contextually
    // from SafetyScreen/GuardianScreen when the user turns the relevant
    // feature on, never bundled into the upfront BLE permission request.
    private val requestSensitivePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* result observed via hasAllPermissions()-style checks at call sites */ }

    fun requestSensitivePermissions(permissions: Array<String>) {
        requestSensitivePermissionLauncher.launch(permissions)
    }

    val geofenceManager: GeofenceManager by lazy { GeofenceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        bleManager = BleManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        preferences = SafeShadePreferences(this)

        permissionsGranted = hasAllPermissions()

        // Request required permissions (only asks for ones not already granted)
        if (!permissionsGranted) {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        launchPreferenceCollectors()

        // Set up Compose UI
        setContent {
            val darkModePref = darkModePreference
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (darkModePref) {
                DarkModePreference.LIGHT -> false
                DarkModePreference.DARK -> true
                DarkModePreference.SYSTEM -> systemDark
            }
            SafeShadeTheme(darkTheme = useDarkTheme) {
                SafeShadeApp(
                    bleManager = bleManager,
                    weather = weatherState,
                    location = locationState,
                    permissionsGranted = permissionsGranted,
                    onRequestPermissions = { requestPermissionLauncher.launch(requiredPermissions) },
                    onSyncWeather = ::fetchAndSendWeather,
                    preferences = preferences,
                    darkModePreference = darkModePref,
                    onDarkModeChange = { pref ->
                        darkModePreference = pref
                        lifecycleScope.launch { preferences.setDarkModePreference(pref) }
                    },
                    onboardingSeen = onboardingSeen,
                    onOnboardingComplete = {
                        onboardingSeen = true
                        lifecycleScope.launch { preferences.setOnboardingSeen(true) }
                    },
                    geofenceManager = geofenceManager,
                    onRequestSensitivePermissions = ::requestSensitivePermissions
                )
            }
        }
    }

    private fun launchPreferenceCollectors() {
        lifecycleScope.launch {
            preferences.darkModePreference.collectLatest { darkModePreference = it }
        }
        lifecycleScope.launch {
            preferences.onboardingSeen.collectLatest { onboardingSeen = it }
        }
    }

    override fun onResume() {
        super.onResume()
        // Catch the case where the user granted permissions from system
        // Settings (after a prior denial) and came back to the app.
        val nowGranted = hasAllPermissions()
        if (nowGranted != permissionsGranted) {
            permissionsGranted = nowGranted
        }
    }

    /**
     * Fetches weather data from API and sends it to the connected device.
     *
     * This function:-
     * 1. Gets current GPS location
     * 2. Calls Open-Meteo API for weather data
     * 3. Updates local state
     * 4. Sends data to device via BLE
     */
    @SuppressLint("MissingPermission")
    private fun fetchAndSendWeather() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Don't touch fusedLocationClient without the permission -
            // this previously could throw a SecurityException if the sync
            // button was somehow reachable before permissions were granted.
            requestPermissionLauncher.launch(requiredPermissions)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                // Update location state
                locationState = LocationState(
                    lat = location.latitude,
                    lon = location.longitude,
                    locationName = "Current Location",
                    locality = "",
                    altitude = location.altitude.toInt(),
                    isValid = true
                )

                // Fetch weather data on IO thread
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = WeatherService.api.getWeather(
                            location.latitude,
                            location.longitude
                        )
                        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                        val rainProb = response.hourly.precipitation_probability.getOrElse(hour) { 0 }
                        val uv = response.hourly.uv_index.getOrElse(hour) { 0f }
                        val temp = response.current.temperature_2m
                        val humidity = response.hourly.relative_humidity_2m.getOrElse(hour) { 0 }.toFloat()

                        val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val calendar = Calendar.getInstance()

                        // Update weather state
                        weatherState = WeatherUiState(
                            rainChance = rainProb,
                            uvIndex = uv,
                            humidity = humidity,
                            temp = temp,
                            isLoaded = true,
                            lastSyncTime = timeNow
                        )

                        val condition = if (rainProb > 30) "RAIN EXPECTED" else "CLEAR SKIES"

                        // Send to device via BLE
                        bleManager.sendWeatherData(
                            rainChance = rainProb,
                            condition = condition,
                            uvIndex = uv,
                            humidity = humidity,
                            lat = location.latitude,
                            lon = location.longitude,
                            locationName = "Current Location",
                            locality = "",
                            altitude = location.altitude.toInt(),
                            hour = calendar.get(Calendar.HOUR_OF_DAY),
                            minute = calendar.get(Calendar.MINUTE)
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
