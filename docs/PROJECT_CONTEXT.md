# SafeShade — Project Context for Claude Code

This file exists to hand off context from a prior planning/debugging
session (done in claude.ai chat, which cannot run builds or access this
machine directly) to this Claude Code session, which CAN run commands,
build the project, and see real output.

Read this first. Then read `docs/SafeShadev21.ino` (firmware) and
`docs/SafeShade_5G_HackathonFinal1.pdf` (pitch deck) if you need product/
protocol context beyond what's summarized below.

## What SafeShade is

A personal safety wearable (hardware: ESP32-C3, OLED display, IMU,
buzzer, NeoPixel LEDs, SOS button) paired over BLE with this Android app.
Target users: elderly (fall detection), women (panic/SOS), children
(guardian tracking). Currently a university major-project demo, intended
to become a real product later. Hardware team built a TRL-4 breadboard
prototype + custom PCB (v1.3); this repo is the Android companion app.

Two operating personas in the app:
- **Guardian** — the caregiver/family member's phone. Sends messages,
  medical ID, settings TO the wearable.
- **Companion** — the wearable wearer's own phone (or a secondary
  binding), which receives messages and can send quick replies back.

## BLE protocol (defined by the firmware, `docs/SafeShadev21.ino`)

Service UUID: `4fafc201-1fb5-459e-8fcc-c5c9c331914b`

Six characteristics, all comma-delimited plain strings, no escaping:
- `WEATHER_CHAR_UUID` (`beb5483e-...`) — write-only from app.
  Payload: `rain,condition,uv,humidity,lat,lon,locationName,locality,altitude,hour,minute`
  Also accepts the literal string `CMD_FIND` to trigger the device's
  "find my device" SOS screen.
- `ALERT_CHAR_UUID` (`8c5314e3-...`) — notify-only from device.
  Sends `FALL_DETECTED` when the IMU trips the fall-detection threshold.
- `MESSAGE_CHAR_UUID` (`1c95d5e3-...`) — write-only from app (Guardian→
  Companion direction). ANY write here is treated by the firmware as a
  new incoming guardian message: it buzzes, sets a "new message" flag,
  and switches the device's screen to SCREEN_MESSAGE. There is no
  prefix-stripping logic on the firmware side.
- `HEALTH_CHAR_UUID` (`2a4d6e8f-...`) — write-only from app.
  Payload: `bloodType,emergencyContact,contactName,allergies,age`
- `SETTINGS_CHAR_UUID` (`3b5e7f90-...`) — write-only from app.
  Payload: `fallSensitivity(0-2),sosVolume(0-100),autoCallEnabled(0/1)`
- `REPLY_CHAR_UUID` (`4c6f8a01-...`) — notify-capable, BOTH directions in
  practice. The device's own hardware quick-reply UI notifies on this
  characteristic when the wearer picks a canned reply with the physical
  button. The Companion app should WRITE to this same characteristic to
  send a reply back to the Guardian — NOT write to MESSAGE_CHAR_UUID.

## What was already fixed in a prior session (verify these landed correctly)

The following files were manually edited (copy-pasted from a claude.ai
chat session, not applied via git or Claude Code) and `git status` shows
them as modified relative to `origin/master`:

- `app/src/main/java/com/safeshade/BleManager.kt`
- `app/src/main/java/com/safeshade/MainActivity.kt`
- `app/src/main/java/com/safeshade/ui/SafeShadeApp.kt`
- `app/src/main/java/com/safeshade/ui/components/Dialogs.kt`
- `app/src/main/java/com/safeshade/ui/screens/GuardianScreen.kt`
- `app/src/main/java/com/safeshade/ui/screens/HomeScreen.kt`
- `app/src/main/AndroidManifest.xml`

**Your first task: review every one of these diffs (`git diff` against
origin/master) for correctness and internal consistency** — the prior
session could not compile or run the code, only reason about it
statically, so please verify by actually building.

Summary of what each change was meant to do:

1. **BleManager.kt** — added a FIFO GATT operation queue (Android's
   BluetoothGatt silently drops a write/descriptor-write/RSSI-read/MTU-
   request if issued before the previous one's callback fires — there
   was no queueing before), MTU negotiation via `requestMtu(247)` before
   `discoverServices()` (default 20-byte payload was truncating the
   weather packet), a new `sendDeviceReply()` function that writes to
   `REPLY_CHAR_UUID` (previously did not exist at all), and scan
   re-entrancy guards in `startScanning()`/`stopScanning()`.

2. **MainActivity.kt** — permission handling was previously fire-and-
   forget (`requestPermissionLauncher.launch(...)` with an empty result
   callback), so the UI built regardless of whether Bluetooth/location
   permissions were actually granted. On Android 13 this caused a
   `SecurityException` crash the moment BLE scanning was triggered. Now
   tracks `permissionsGranted` state, checks it in `onResume()` too (for
   the case where the user grants via system Settings), and threads it
   through to the UI.

3. **SafeShadeApp.kt** — added `permissionsGranted`/`onRequestPermissions`
   parameters threaded from MainActivity down to HomeScreen. Also fixed
   the `onReply` callback (used by Companion-mode message replies) to
   call `bleManager.sendDeviceReply(reply)` instead of only updating
   local UI state.

4. **HomeScreen.kt** — the connect `Switch` called
   `bleManager.startScanning()` with no permission check at all
   (independent crash path from #2). Now requests permissions instead of
   scanning if they're missing. Also fixed: toggling the switch OFF
   previously did nothing (only the `true` branch was handled) — now
   calls `bleManager.disconnect()`.

5. **GuardianScreen.kt** — `QuickRepliesCard` (Companion mode) was
   calling `bleManager.sendGuardianMessage("REPLY:...")`, which writes to
   `MESSAGE_CHAR_UUID` — per the protocol above, this makes the firmware
   treat it as a brand new incoming message (buzzes, shows
   `SCREEN_MESSAGE`) rather than a reply, and there's no "REPLY:" prefix
   handling in the firmware to unwrap it. Changed all four quick-reply
   buttons to call `bleManager.sendDeviceReply(...)` instead. Also, the
   inline reply chips inside `MessageItem` previously only updated local
   `messageHistory` state and never sent anything over BLE at all — now
   wired through `onReply` to actually transmit.

6. **Dialogs.kt** — `MedicalIdEditorDialog`'s "Save Changes" button and
   `DeviceSettingsDialog`'s "Save" button both called `onSave(...)` but
   never `onDismiss()`, so neither dialog closed after saving. Both now
   call `onDismiss()` immediately after `onSave(...)`.

7. **AndroidManifest.xml** — `BLUETOOTH`/`BLUETOOTH_ADMIN` now scoped to
   `maxSdkVersion="30"` (only needed pre-Android-12). `BLUETOOTH_SCAN` now
   declares `android:usesPermissionFlags="neverForLocation"` since the
   app's scan only filters by its own service UUID and never derives
   physical location from scan results — `ACCESS_FINE_LOCATION` is
   requested separately, purely for the GPS weather-sync feature.

## Known-uncertain items — please verify these directly

1. **`gradlew.bat` line endings.** The original repo's `gradlew.bat` had
   LF-only line endings (should be CRLF on Windows), which caused this
   exact error when run from PowerShell:
   ```
   Error: -classpath requires class path specification
   ```
   A fix was applied by converting the file to CRLF, and a `.gitattributes`
   file was written (`*.bat text eol=crlf`, `gradlew text eol=lf`,
   `*.jar binary`) to prevent recurrence. **Please confirm both actually
   landed in this working copy** — run:
   ```
   git status -uall
   ```
   and check whether `.gitattributes` appears as untracked (new file,
   not yet committed) and whether `gradlew.bat` shows as modified. If
   `.gitattributes` is missing entirely, recreate it. If `gradlew.bat`
   still has LF endings, re-fix it. Verify with a small script or by
   checking the file's line-ending property directly, then confirm
   `.\gradlew --stop` runs without the classpath error.

2. **The project folder was just moved** from
   `C:\Users\KIIT0001\Pictures\SafeShade` to `C:\Dev\SafeShade` (the
   `Pictures` path was suspected of being OneDrive-synced, which caused a
   separate Gradle error: `Unable to delete directory ...
   aar_metadata_check\debug\checkDebugAarMetadata`, i.e. a Windows file
   lock during clean). Delete any stale `app\build` directory that may
   have come along in the move, then do a clean build from scratch and
   confirm the original 10-error build failure is gone.

3. **None of the fixes above have been compiled or run yet.** The prior
   session worked entirely via static code reading — no Gradle, no
   emulator, no device. Please build the project, fix any real compiler
   errors that surface, and — since you can actually interact with a
   connected device or emulator, unlike the prior session — help verify
   BLE behavior against the firmware protocol described above if a
   physical device (Xiaomi Redmi Note 10S, MIUI 14.0.11, Android 13) or
   the ESP32 hardware is available for testing.

## Constraints to keep in mind

- This is a demo/university project for now. Security hardening and
  best-practices review were explicitly deprioritized by the project
  owner — focus on correctness and functionality, not on things like
  encrypting BLE payloads or hardening permission scoping beyond what's
  needed for the app to actually work.
- Target test device: Xiaomi Redmi Note 10S, MIUI 14.0.11, Android 13
  (API 33). `minSdk = 26`, `compileSdk`/`targetSdk` are much newer (36) —
  keep backward-compat branches (`Build.VERSION.SDK_INT` checks) intact
  when editing BLE code, since the API 33+ `writeCharacteristic`/
  `writeDescriptor` overloads differ from the deprecated pre-33 ones.
