# SafeShade — Session Handoff #3

**Written:** 2026-09-05
**Scope:** The Android app (`app/`) only. Firmware was not touched — `docs/SafeShadev21/SafeShadev21.ino`
is still the previous session's uncommitted work and was deliberately left alone.
**Status:** Full visual and architectural rebuild of the app, shipped as `v2.0.0`.

---

## 0. For the next firmware session — things the app found

This section is the counterpart to handoff2 §0. Everything here was verified by reading
`SafeShadev21.ino` directly and cross-checking against what the app now sends.

### 0.1 `CMD_FIND` needs a real "locate" path — the highest-value fix here

**What the app now does.** The app's intent is settled: find-my-device is a *low-key locate*, not an
emergency. `device/locate` rings the device, shows an RSSI proximity meter, and shows the phone's
last known location.

**What the firmware actually does** (`WeatherCallbacks::onWrite`): drops the device onto the full
`SCREEN_SOS` — blinking inverted "EMERGENCY SOS" banner, siren, red LED ring — sends **no ACK**,
dispatches **no alert** (`sosArmedByHold` stays false, so `beginAlertDispatch()` never runs and
`pAlertChar` never notifies), and clears **only** when somebody physically taps the button on the
device. There is no timeout.

That is a genuine UX mismatch, and the app cannot paper over it: it has no way to know the siren
stopped, so its "ringing" banner has to be dismissed by hand and says so.

**Suggested firmware change:** give `CMD_FIND` its own screen and chime — a `SCREEN_LOCATE` with a
short repeating chirp, a visible "Tap to stop", and an auto-timeout after ~30s — and send an ACK so
the app can confirm the command landed. If instead the intent is a real remote-triggered emergency,
it needs to arm a genuine dispatch; right now it does neither.

### 0.2 `CMD_FIND` and weather share `WEATHER_CHAR_UUID`

`sendCommand()` and `sendWeatherData()` write the same characteristic. A weather payload written
while the device sits on the alarm screen is parsed by the weather parser, and because `CMD_FIND`
sends no ACK there is no way to observe what happens.

The app now guards this: `DeviceRepository` holds `isRinging` and suppresses **all** automatic
weather sync while it is true. That is a workaround, not a fix. `CMD_FIND` would be better as an
`EXT_CHAR` tag like every other command.

### 0.3 Eight on-device settings have no BLE write path at all

Verified against the firmware's `setSettingValue()` and the `ExtCallbacks` dispatcher. These are
reachable from the wearable's own Settings menu and persisted to NVS, but **no characteristic and no
EXT tag sets them**:

`ledsEnabled` · `displayContrast` · `flipDisplay` · `homeTickerEnabled` · `masterVolume` (chime
volume — distinct from `sosVolume`, which *is* wired) · `bootChimeEnabled` · `notifChimeEnabled` ·
and the **Do Not Disturb on/off toggle itself** (only its start/end hours ride on `EXT QUIET`).

Rather than ship eight switches that silently do nothing, the app renders these as read-only rows on
`device/onboard`, each naming the exact menu path to change it on the wearable. If firmware adds EXT
tags for them, moving a row from `DeviceCapabilities.deviceOnly` to `.synced` is a one-line change.

The DND case is the subtle one and worth fixing first: the schedule is settable but the master
toggle is not, which is a strange half-capability from the app's side.

### 0.4 There is no silent SOS path

Grepped the firmware: an SOS always sounds the siren and takes the screen. The app has a silent-SOS
affordance for the women's-safety persona (SMS + location to contacts with no sound and no visible
change), but it cannot ask the device to alert quietly. That control is marked as representative in
the app until firmware gains a silent path.

### 0.5 Confirmed working, no action needed

- **`MODE_AUTO`** — the app now has all 8 modes including `AUTO`, sends `EXT MODE:AUTO`, and defaults
  to it on a fresh install to match the firmware's fresh-boot default. `modeFromName()`'s BACKPACK
  fallback for unrecognised names is mirrored in `PersonaMode.fromWire()` and left deliberately.
- **11-field `HEALTH_CHAR`** — the app now sends all eleven fields in the firmware's exact positional
  order, with commas stripped from all nine free-text fields and `organDonor` as `1`/`0`. The
  on-device extended Medical ID view will now show real data instead of placeholders.
- **Guardian-locked modes** — the app marks ELDERLY/KIDS/PET/HELMET with a visible seal and explains
  that mode switching and the Safety menu are hidden on the wearable in those modes.
- **Message/reply directionality** and **`ALERT_CHAR`'s payload** are unchanged and correct.
- **"Skin"** — removed firmware-side; the app never grew a control for it.

---

## 1. What changed in the app

### 1.1 A real bug that had never worked

`BleManager` sets `connectionState = "Connected"` the instant the GATT link comes up — **before**
`requestMtu()` → `onMtuChanged()` → `discoverServices()` resolves the characteristics. Every send
function null-guards on its characteristic and returns with only a log line.

`SafeShadeApp.kt` re-pushed medical ID, safety settings and the SMS allowlist keyed on
`"Connected"`. All three hit the null guard. **The reconnect re-sync had never once worked** since it
was written.

Fixed by adding a `servicesReady` flag to `BleManager` and a distinct `ConnectionState.Ready` in the
new `DeviceLink` boundary. Every write path now gates on `Ready`, never `Connected`.

A second, related one: `fallAlert` was a `StateFlow<Boolean>`. `MutableStateFlow` conflates equal
values, so a **second fall arriving while the first was still unacknowledged emitted nothing** and
was silently lost. Alerts are now a `SharedFlow` of events.

### 1.2 Architecture

- ViewModel + repository layer (7 repositories on an application-scoped `CoroutineScope`), so alerts,
  geofence forwarding and journey escalation fire with no Activity alive.
- Everything that used to die on process death now persists: medical ID, safety settings, contacts,
  zones, trip history, messages, role, reminders, telemetry history.
- `AppState.Loading` genuinely gates the first frame, fixing the default-value flash.
- `DeviceProtocol` isolates every byte sent to the device, and is unit-tested — the failure mode of
  that format is silence, so it needed tests more than anything else here.

### 1.3 Visual system

Complete replacement. See `DESIGN.md` for the built system.

---

## 2. Known limitations

- **Quiet hours are not persisted** app-side. They live in memory in `DeviceRepository`, so `pushAll`
  skips the `QUIET` stage after a process restart rather than re-asserting. Needs a DataStore key.
- **NFC was dropped from scope.** The deck's "NFC + QR Emergency Access" is a wearable capability;
  the target phone (Redmi Note 10S) has no NFC radio, so an app-side tag writer could be neither used
  nor verified. QR carries the emergency-access promise.
- **API 34+ paths are unverified on hardware.** The test device is Android 13 (SDK 33), so the
  install-time `USE_FULL_SCREEN_INTENT` denial and foreground-service-type enforcement are written
  defensively but exercised only on the emulator.
- **Compose is pinned below the current release.** BOM `2026.06.01` (Compose 1.11.4) is the newest
  train that compiles against `compileSdk 36`; Compose 1.12 / navigation 2.10 / lifecycle 2.11 all
  require compileSdk 37 and AGP 9.2.0. Moving to those is a separate, isolated change.

---

## 3. v2.1.0 — what this pass changed, and what firmware still owes it

Written 2026-09-05. App only; no firmware was touched.

### 3.1 Firmware follow-up needed: relaying a phone SOS over the gateway

The app now has an SOS control in its own bottom bar (hold five seconds). It sends by **phone SMS**
to every emergency contact, and separately writes the same text to `MESSAGE_CHAR` so the wearable
buzzes and shows the alert.

What it deliberately does **not** do is ask the device to relay the alert onward over its own
cellular gateway. There is no wire format for that:

- `DeviceProtocol` exposes `CMD_FIND` as a bare command plus a generic `writeExt(tag, payload)`.
- `handoff2.md` §0.3-4 records `ALERT_CHAR` as device→app notify-only, with nothing for the app to
  send on it.
- `MessagingRepository.sendGuardianMessage`'s own SMS fallback targets `devicePhoneNumber` — the
  device SIM — not the emergency contact.

So a phone-raised SOS currently reaches a contact **only** if the phone itself has signal and the
`SEND_SMS` grant. On a wearer whose phone is dead or out of coverage but whose wearable has a
cellular link, the alert does not go out.

**Closing that needs a new EXT tag and a firmware handler** — something on the order of
`EXT SOSRELAY:<e164>,<text>`, with the gateway sending it and acking so the app can report per
contact honestly rather than optimistically. This is a real gap in the product promise and is worth
a decision rather than being left implicit.

### 3.2 A permission that was declared and never requested

`android.permission.SEND_SMS` was in the manifest from early on and was requested **nowhere**. It is
a dangerous permission at `minSdk 26`, so `sendSmsText` returned `PermissionMissing` on every
install, silently — which meant the SMS fallback in `AlertActionReceiver.notifyContactsBySms` and in
`ReminderReceiver` had never once worked in the field either.

It is now requested at the SOS guard rather than in the cold-start batch, because a restricted
permission asked for before the user has seen anything that sends a text reads as overreach.

Worth knowing when testing: **every SOS test sends real SMS to real people.** Point the emergency
contact at a spare SIM first, and note that a message carrying a maps link is routinely two
segments.

### 3.3 Trip kinds

`TripKind.PHONE_SOS` was added alongside `SOS`, so the trip log stops claiming a phone-raised alert
came from the device's physical button. Enum values are persisted by name (`Dtos.kt`,
`enumOrDefault`), so an older build reading a newer store degrades it to `FALL` rather than shifting
ordinals — still an alert in the log.

### 3.4 The number shown was not the number dialled

Found while verifying the SOS path on the Redmi Note 10S, and the reason that verification was worth
doing: one stored contact produced **three different numbers**.

The contact editor stored whatever was typed — `+91 891736 60065`, spaces and country code included
— even though `PhoneNumbers`' own header states "the stored value is always bare digits". Nothing
enforced it, so every consumer re-derived the digits its own way:

| Path | Result for that contact |
|---|---|
| The list row | `+91 891736 60065` — the raw string |
| The editor field (`IndianPhoneTransformation`) | `+91 91891 73660` — first ten digits, country code **not** stripped |
| `dialable`, i.e. what `SmsManager` and `tel:` actually got | `+918917366006` — country code stripped, then truncated to ten |

The third row is the dangerous one. `digitsOf` ended in `take(LOCAL_LENGTH)`, so an eleven-digit
number was silently trimmed into a ten-digit one that `isComplete` then called valid and `dialable`
dialled with full confidence — **a different, entirely plausible-looking number**. This is not an
edge case: it also fired for the ordinary act of typing a number with its country code, which the
field's own placeholder (`+91 98300 11223`) invites.

Four fixes, all in `PhoneNumbers.kt` and `ContactsScreen.kt`:

1. `digitsOf` no longer truncates. Length capping belongs at the input field, where the user can see
   a character count, not in the function the dialler calls.
2. `IndianPhoneTransformation` uses `PhoneNumbers.digitsOf` instead of its own filter, so the number
   the user proof-reads is the number that will be dialled.
3. The contact field normalises to bare digits on input, which makes the documented contract true
   rather than aspirational.
4. A number longer than ten digits is now an error on the field and blocks the save, instead of
   being accepted and quietly mangled later.

`PhoneNumbersTest` covers all four and fails against the old code on three of its four cases.

**Needs the owner's attention, not a guess:** the emergency contact currently stored on the test
device has *eleven* digits after the country code. It cannot be repaired from here — `...66006` and
`...60065` are both plausible ten-digit numbers and picking one would be inventing an emergency
contact. The app now says so on the contact screen and refuses to save until it is corrected. Every
SMS and call that contact has ever received from this app went to the truncated number.

### 3.5 Two things the SOS still cannot tell you

- **`ActionResult.Sent` means "handed to the radio", not "delivered".** `sendMultipartTextMessage` is
  called with null `sentIntents`/`deliveryIntents`, so a message accepted by a phone with no
  coverage reports success. The banner's "Sent to <name>" inherits that limit. Closing it means
  passing `PendingIntent`s and resolving the outcome asynchronously — worth doing on a channel whose
  whole purpose is that it works when nothing else does.
- **The phone SOS has never been fired end to end.** Firing it sends real SMS; the only emergency
  contact on the test device is the owner's own number, and it is the malformed one above. The
  arming path, both guard snackbars and the permission round trip were verified on device; the
  send, the banner body and the trip-log entry were not.
