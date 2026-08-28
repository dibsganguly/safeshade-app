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
 * @version 2.0.0
 */

package com.safeshade

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import com.google.android.gms.location.*
import com.safeshade.data.*
import com.safeshade.ui.SafeShadeApp
import com.safeshade.ui.theme.BgColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    // Permission request launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Permissions result handling can be added here if needed
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize managers
        bleManager = BleManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Request required permissions
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        // Set up Compose UI
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = BgColor)) {
                SafeShadeApp(
                    bleManager = bleManager,
                    weather = weatherState,
                    location = locationState,
                    onSyncWeather = ::fetchAndSendWeather
                )
            }
        }
    }

    /**
     * Fetches weather data from API and sends it to the connected device.
     *
     * This function:
     * 1. Gets current GPS location
     * 2. Calls Open-Meteo API for weather data
     * 3. Updates local state
     * 4. Sends data to device via BLE
     */
    @SuppressLint("MissingPermission")
    private fun fetchAndSendWeather() {
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
                        val humidity = 65f  // Simulated - Open-Meteo free tier limitation

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
