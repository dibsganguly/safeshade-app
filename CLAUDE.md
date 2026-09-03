# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

SafeShade is a single-module Android app (Kotlin + Jetpack Compose) that pairs a phone with a
SafeShade ESP32-based wearable device over Bluetooth Low Energy (BLE). It syncs weather/location
data to the device, receives fall-detection alerts, exchanges Guardian↔Companion quick messages,
and manages medical ID / safety settings. There is no companion server — the only network
dependency is the free Open-Meteo weather API.

## Commands

Build, test, and lint from the project root using the Gradle wrapper.

```
./gradlew assembleDebug              # Build debug APK
./gradlew installDebug                # Build and install on connected device/emulator
./gradlew test                        # Run local JVM unit tests (app/src/test)
./gradlew connectedAndroidTest         # Run instrumented tests on a device/emulator (app/src/androidTest)
./gradlew test --tests "com.safeshade.ExampleUnitTest"   # Run a single unit test class
./gradlew lint                        # Android lint
./gradlew clean                       # Clean build outputs
```

On Windows use `gradlew.bat` instead of `./gradlew`.

There are currently only placeholder test classes (`ExampleUnitTest`, `ExampleInstrumentedTest`) —
no meaningful test suite exists yet.

## Architecture

**Single Activity, Compose navigation.** `MainActivity` is the sole entry point. It owns the
`BleManager` instance, runtime permission state, and weather/location state (as `mutableStateOf`),
then passes all of it down into `SafeShadeApp` (`ui/SafeShadeApp.kt`), which hosts a
`NavHost` with 5 routes: `home`, `guardian`, `safety`, `profile`, `device` (bottom nav bar in
`ui/navigation/BottomNavBar.kt`). Screen-level UI state (device settings, medical ID, safety
settings, fall/message history) lives in `SafeShadeApp` via `remember { mutableStateOf(...) }` —
there is no ViewModel layer; state is threaded down through composable parameters and lifted back
up through callbacks.

**BLE is the core integration point (`BleManager.kt`).** It talks to the ESP32 firmware over a
single custom GATT service (`SERVICE_UUID`) with dedicated characteristics for weather, alerts,
guardian messages, device replies, health data, and settings — the UUIDs must match the firmware
exactly. Two things about this class matter for any BLE-related change:

- **GATT operation queue**: Android's `BluetoothGatt` allows only one outstanding
  write/read/descriptor-write/MTU request at a time; issuing a second one before the previous
  callback returns causes it to be silently dropped (no error). Every GATT operation (writes,
  descriptor writes, RSSI reads, MTU requests) must go through `enqueue()`/`drainQueue()` — never
  call `gatt.writeCharacteristic()` etc. directly.
- **Characteristic directionality matters**: `MESSAGE_CHAR_UUID` is Guardian→Companion (firmware
  buzzes the device and shows the message on-screen), while `REPLY_CHAR_UUID` is
  Companion→Guardian (quick replies). Writing a reply to the message characteristic makes the
  firmware treat it as a brand-new incoming message, not a reply — this was a real historical bug
  (see `sendDeviceReply()` vs `sendGuardianMessage()`).
- Connection/state is exposed to Compose via `StateFlow`s (`connectionState`, `fallAlert`,
  `deviceReply`, `rssi`, etc.), collected in `SafeShadeApp` with `collectAsState()` /
  `LaunchedEffect`.
- Payloads are comma-delimited strings (e.g. weather:
  `"rain,condition,uv,humidity,lat,lon,locationName,locality,altitude,hour,minute"`); free-text
  fields are sanitized to strip commas before sending since the firmware parser doesn't escape
  them.

**Permissions gating.** BLE scan/connect actions must never be reachable before
`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT` (API 31+) or `ACCESS_FINE_LOCATION` (pre-31) are granted —
`MainActivity` tracks `permissionsGranted` and passes it plus `onRequestPermissions` down through
`SafeShadeApp` to gate UI actions (e.g. the Home screen connect switch). `BLUETOOTH_SCAN` is
declared with `neverForLocation` since scanning only filters by the app's own service UUID.

**Data models** (`data/Models.kt`) are plain immutable `data class`/`enum class` definitions for
all app state: `WeatherUiState`, `LocationState`, `MedicalId`, `DeviceSettings`, `SafetySettings`,
`FallAlertEvent`, `QuickMessage`, `LiveSensorData`, etc. This is the single place to look when
adding or changing a field that flows between UI, `BleManager`, and (eventually) the device.

**Weather** (`WeatherAPI.kt`) is a minimal Retrofit + Gson client for the Open-Meteo API
(`WeatherService.api`), called from `MainActivity.fetchAndSendWeather()` after getting a fused
location, with the result piped straight into `BleManager.sendWeatherData()`.

**UI structure**: screens live in `ui/screens/` (`HomeScreen`, `GuardianScreen`, `SafetyScreen`,
`ProfileScreen`, `DeviceScreen`), shared composables in `ui/components/` (`SharedComponents.kt`,
`Dialogs.kt`), and theme/colors in `ui/theme/`. Screens are stateless-ish: they receive state and
`BleManager`/callbacks as parameters rather than owning their own BLE logic.

## Notes for future changes

- `minSdk = 26`, `targetSdk = compileSdk = 36` — guard any new API usage against API 26 and branch
  behavior on `Build.VERSION.SDK_INT` where needed (see `BleManager`'s Tiramisu/pre-Tiramisu GATT
  write paths).
- Several files carry a `FIXES (this pass)` doc comment block at the top describing recent bug
  fixes and their root cause (permission crashes, GATT queue races, reply-vs-message mixups,
  dialogs not dismissing on save) — read these before touching the same area, they explain *why*
  the current code is structured the way it is.
