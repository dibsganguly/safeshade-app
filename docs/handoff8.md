# SafeShade — Session Handoff #8

**Rewritten at the end of Phase 3 (v2.7.0) of the v2.5→v2.7 pass, 2026-09-07.** Like
handoff7 it is **self-contained**: a fresh session with no memory should be
able to start Phase 4 from this file, `DESIGN.md` and the code. Handoff7 is
the record of Phases 1–2 and their traps; where this file and handoff7
differ, this file wins.

## Contents

- [0. What to trust](#0-what-to-trust)
- [1. The pass, and where it stands](#1-the-pass-and-where-it-stands)
- [2. Decisions made this pass](#2-decisions-made-this-pass)
- [3. What Phase 3 shipped](#3-what-phase-3-shipped)
- [4. Architecture, as it is now](#4-architecture-as-it-is-now)
- [5. The cloud, as it is now](#5-the-cloud-as-it-is-now)
- [6. Phase 4 — the brief](#6-phase-4--the-brief)
- [7. Phase 5 — the test list](#7-phase-5--the-test-list)
- [8. Verified on the device, and not](#8-verified-on-the-device-and-not)
- [9. Known bugs, debt and firmware owes](#9-known-bugs-debt-and-firmware-owes)
- [10. Traps, and the tools that route around them](#10-traps-and-the-tools-that-route-around-them)
- [11. What the user still has to do](#11-what-the-user-still-has-to-do)
- [12. Phase 3 close](#12-phase-3-close)

---

## 0. What to trust

| File | Status |
|---|---|
| `DESIGN.md` | **Current.** Updated in the same commits as the kit and pattern changes it describes. Read it before inventing any visual decision. |
| `docs/handoff8.md` | This file. Current. |
| `docs/handoff7.md` | Phases 1–2: the cloud foundation, sign-in, sync, Circle, Talk, ladder, mail. Its §9 debt and §10 traps still apply unless restated here. |
| `docs/handoff6.md` | History, the pitch-deck-versus-app matrix, the firmware ledger. |
| `docs/icons-wanted.md`, `docs/Icons/*.svg`, `CLAUDE.md`, `PRODUCT.md`, `docs/SafeShadev21/SafeShadev21.ino` | **Not ours.** Modified or untracked by another session. Never commit, stage, revert or edit them. `CLAUDE.md`'s BLE notes are correct and load-bearing; its `./gradlew test --tests` form is wrong (§10). |
| `supabase/README.md` | Deploying functions, secrets, Auth SMTP, templates, the Vault secrets for the weekly report. |

**Trust the code over any document.**

---

## 1. The pass, and where it stands

The brief for the pass: **finish the app** so that when the hardware lands the
only thing left is plugging it in.

- **Phase 1 — done** (v2.5.0, `versionCode 8`).
- **Phase 2 — done** (v2.6.0, `versionCode 9`). 27 commits, `4131176`..`b6ac200`.
- **Phase 3 — done in scope, see §12** (v2.7.0, `versionCode 10`). Six
  commits, `0767563`..the review fix after `cbc16a3`, **not pushed** (the user authorises every
  push). `assembleDebug` green, **645 unit tests** pass (265 at the close of
  Phase 2). 128 files changed, 80 new.
- **Phase 4 — next.** UI/UX enhancements, refinements, the all-screen sweep.
  The user's own UI/UX list lands here and jumps the queue. §6.
- **Phase 5 — testing, vulnerability and bug fixing only.** Everything in §8's
  not-verified list and §7. Nothing from it is re-verified in Phase 4.

### Model allocation (a standing instruction)

Fable 5.1 orchestrates and does all UI/UX, all Shady, the hardest tasks. Opus 5
for backend, schema, sync, protocol, data model, migrations; Sonnet 5 for
ordinary implementation; Haiku 4.5 for greps, inventories, sweeps. Every
sub-agent gets a hard file scope and "no writing git command". This phase ran
four agents in wave one and seven in wave two, all concurrently; none died,
but see §10 for what concurrency cost.

### File ownership

Sub-agents own `data/`, `repo/`, `di/` (by wiring instruction only), `cloud/`,
`service/`, `platform/`, `widget/`, tests, gradle, manifest. The orchestrator
owns `ui/nav/`, `ui/screens/`, `ui/board/`, `ui/shady/`, `ui/theme/`,
`ui/vm/`, `di/AppContainer.kt`, `MainActivity.kt`, `DESIGN.md`, handoffs.
`Routes.kt` and `MainNavGraph.kt` are merge hotspots; the orchestrator adds
every route itself before any screen work is spawned.

---

## 2. Decisions made this pass

Handoff7 §2 stands in full. Added in Phase 3, **by the orchestrator unless
marked**, each reversible:

- **Nothing in the UI says planned, simulated, representative or coming
  soon** (user, standing). Every new page follows the outcome rule: absent
  data is a dash, a failed action reports its reason, no tick before a result.
- **Health Connect is the phone-side vitals source** and `READ_SKIN_TEMPERATURE`
  is deliberately not requested: the page's one temperature is core body
  temperature against a fever threshold, and wrist skin temperature would
  either never trip it or mislead.
- **Vitals are readings, not alerts.** A threshold breach lights the readout,
  the row and the hub's way; nobody is called. The page says so.
- **Evidence never leaves the phone unless the rocker says so**, and a clip
  recorded while opted out stays `LOCAL_ONLY` forever (the cloud layer never
  demotes or promotes it). The FileProvider exposes `files/reports` only;
  evidence and voice files are not shareable through it.
- **The microphone service starts only from a surface the person is looking
  at**: the SOS tap, the alert banner (keyed on the alert id), the Evidence
  page's test button. Never from a receiver or an alarm (Android 14 refuses a
  background microphone start).
- **A bare `ACK` is never a success.** `EXT VER` answered `ACK:VER` with no
  version is "acknowledged, no version"; an OTA install counts as installed
  only when the wearable reports the release's version. On the shipped
  firmware the install path can only ever end in that Failed sentence.
- **The quiet word maps to the `sos` smart-home trigger** (Opus, flagged for
  the user). A duress word exists so the room does not know; a hook that turns
  on lights could reveal it. The user chooses which hooks fire on `sos`.
- **Webhooks accept `http://` only for a numeric LAN address** (10/8,
  172.16/12, 192.168/16, 127.x, ::1, localhost, 169.254). `homeassistant.local`
  is rejected because a hostname cannot be checked for being local without a
  network call inside a validator. Whether `.local` joins the list is a
  product call.
- **Community sighting reports are off until the person switches them on**,
  and the rocker says exactly what leaves the phone: the wearable's address,
  the time, the place.
- **Pairing by product narrows what the page says, not what the radio does.**
  Every SafeShade advertises one service UUID; a wearable answering as another
  model is reported, not accepted silently.
- **The Spark's anti-removal row draws no control.** No firmware for the
  Spark exists; the row describes the product and says the phone changes
  nothing. Every Spark fact on the pairing page is a product description.
- **Brake light and path light are device-only rows**, because the firmware
  has no BLE path for either (Haiku's read, §9).
- **The ladder default stays off** (handoff7 §2). The user has not yet been
  asked.

---

## 3. What Phase 3 shipped

Commits on `master`, `v2.7.0:` prefixed. Each commit message lists what was
and was not verified; §8 collects them.

### 3.1 Sub-phase A — `eaebdde` — Vitals, Evidence, wire formats, helpers

- **Vitals** (`ui/screens/safety/VitalsScreen.kt`, `SAFETY_VITALS`). Three
  readouts and a provenance line; dashes where nothing was measured. Sources:
  the wearable's telemetry fields 7–9 (`DeviceProtocol.parseTelemetry`, with
  plausibility clamps hr 25–250, SpO₂ 50–100, skin 20–45 °C; out of range →
  null, never a reading) and Health Connect (`platform/HealthConnectVitals.kt`,
  `connect-client 1.1.0`, whose row reads Not installed / Needs an update /
  Not allowed / Allowed with the one fixing action). Four threshold dials
  commit together. `repo/VitalsRepository.kt` over `safeshade_vitals`
  (ring of 500, `source` "device" | "phone"); device vitals sampled once a
  minute from `AppContainer`.
- **Evidence** (`ui/screens/safety/EvidenceScreen.kt`, `SAFETY_EVIDENCE`).
  `service/EvidenceService.kt` (type `microphone`), `platform/EvidenceRecorder.kt`
  (AAC 16 kHz mono, `setMaxDuration`, the MAX_DURATION stop trap handled),
  `platform/SoundLevel.kt` (AudioRecord RMS → uncalibrated SPL, the
  calibration sentence as a constant printed verbatim; `LoudEnvironment`
  goes Loud only on a sustained minute at 85 dB), `repo/EvidenceRepository.kt`
  over `safeshade_evidence` (cap 50, hooks fire only under the opt-in). The
  trip page gained "What the microphone heard".
- **Wire formats** (`device/`): telemetry 7–9, `EXT VER` and `VersionReply`,
  `OtaProtocol` (`<seq>/<total>,<crc32>,<base64>` chunks, SHA-256 of the
  image, `OtaStep`), `MeshAdvertCodec`, `DeviceModel { S1, FIVE_G, SPARK }`
  with `fromAdvertisedName` (substring; default S1).
- **Pure helpers** (`platform/`): `IncidentPdf`, `WeatherNudges`,
  `QuietWordMatcher`, `VirtualLeash`, `PositioningReadout`.

### 3.2 Sub-phase B — `b359a16` — Smart home, lost, firmware, pairing, recommendations

- **Smart home** (`ui/screens/circle/SmartHomeScreen.kt`, `CIRCLE_SMART_HOME`,
  editor `CIRCLE_SMART_HOOK_EDIT?hookId=`). `repo/SmartHomeRepository.kt` over
  `safeshade_smarthome`; `service/SmartHomeRunner.kt` fires on the active
  alert (fall, SOS, missed check-in, quiet word → sos), zone transitions, the
  watch's low-battery edge; one firing per hook and trigger per minute.
  `platform/WebhookSigner.kt` (HMAC-SHA256 over `timestamp.body`, headers
  `X-SafeShade-Signature/-Timestamp/-Event`; IFTTT `value1..3`; Home Assistant
  the plain JSON). `platform/SmartHomeProviders.kt`: Google Home and Alexa open
  by launch intent (manifest `<queries>`), Matter reports Play Services
  availability only (no Home module dependency). Synced to `smart_home_hooks`;
  in the backfill identity block.
- **Lost** (`ui/screens/device/LostModeScreen.kt`, `DEVICE_LOST`). `BleManager`
  emits every scan result for our service as a `BleSighting` and offers a
  20 s low-power sweep that never connects and never touches
  `_connectionState`; `repo/SightingsRepository.kt` over `safeshade_sightings`
  dedupes per address per five minutes, keeps own addresses (including the
  connected one) out of the community ring, holds lost marks;
  `cloud/SightingsCloud.kt` reports once per address per ten minutes and
  fetches the newest sighting per address.
- **Firmware** (`ui/screens/device/FirmwareScreen.kt`, `DEVICE_FIRMWARE`).
  `repo/FirmwareRepository.kt` over `safeshade_firmware` through a small
  `FirmwareSource` (adapted in `AppContainer` from `cloud/FirmwareCloud.kt`);
  `platform/FirmwareDownloader.kt` (OkHttp, progress, size check).
  `downloadAndVerify` returns `OtaStep.Verifying` **meaning verified** (there
  is no Verified member); `install` awaits an ack per chunk, then `EXT VER`.
- **Pairing by product** (`ui/screens/device/PairScreen.kt`, `DEVICE_PAIR`)
  with the kit member `ui/board/ProductSilhouette.kt`. Paired devices' "Pair
  Another Device" opens it.
- **Recommendations**: the leash (`DeviceRepository.leash`, the Device page's
  Nearby way); positioning readout on Find the device (`LocationState` gained
  `provider`, `accuracyM`, `fixAt`, `fromDevice`); brake light and path light
  as device-only rows on Lights; the ride log (`ui/screens/device/RidesScreen.kt`,
  `DEVICE_RIDES`, `repo/RideLogRepository.kt` over `safeshade_rides`, fed by
  `RideLogSink` from `JourneyService`); the quiet word on Silent SOS
  (`SafetySettings.quietWord`, matched in `MessagingRepository.ingestReply`
  with a ten-minute cool-down, raising `TripKind.QUIET_WORD`); weather and
  air-quality nudges under the Board's gauges (`AirQualityService`,
  Open-Meteo); Walk Home on the Journey setup; the Quick Settings SOS tile
  (`platform/SosTileService.kt`), three static shortcuts, the Glance widget
  (`widget/`, `glance-appwidget 1.1.1`), all of which only open the app
  (`MainActivity` routes `com.safeshade.action.SOS|CHECK_IN|LOCATE` through
  `SafeShadeViewModel.launchAction`); `platform/NfcTagWriter.kt`.
- **Cloud**: resolver branches for `vitals_samples` and `evidence` (upload to
  the private bucket first; the clip's state set from the result), the four
  new no-default `CloudContainer.Repositories` fields.

### 3.3 Sub-phase C — `87acf95` — PDF, NFC write, guide

- **Share as PDF** on a trip's page through a `FileProvider`
  (`${applicationId}.files`, `res/xml/file_paths.xml`, `files/reports` only).
- **Write to a tag** on the Emergency card: a rocker only on a phone with NFC;
  `MainActivity` holds reader mode while armed and resumed; the row prints the
  writer's real outcome.
- **Guide the wearable** on a zone row while the link is up, through the
  revived `DeviceRepository.setNavTarget`.

### 3.4 Review fixes after the handoff was first written

- **The microphone opens only for the kinds the rockers name.** The effect in
  `SafeShadeApp` had started a recording for any non-phone alert under the
  "A fall is detected" rocker: a zone exit, a missed check-in, a quiet word.
  It now maps `FALL` to `recordOnFall`, `SOS` and `PHONE_SOS` to
  `recordOnSos`, nothing else, skips `PHONE_SOS` there because `firePhoneSos`
  starts it itself, and skips an alert that already has a clip (a cold start
  with a persisted alert hours old no longer records again). The SOS rocker's
  line now names the wearable's button as well as the phone.
- **The widget rewrites only when its words change.** `AppContainer` mapped
  every `AppState` emission (about one a second with telemetry) into a
  `WidgetFeed.publish`; a `distinctUntilChanged` on the snapshot sits before
  the publish now. Not verifiable without a wearable.
- **The mandated-string grep was run** (it had been asserted): zero hits in
  `ui/`. One near miss recorded in §9: `ComingSoonPlate` on `Routes.BOARD_LINK`
  prints "Coming in this build"; nothing navigates there.

---

## 4. Architecture, as it is now

Handoff7 §4 stands. Added:

- **DataStore files, one per feature**, never a second delegate on
  `safeshade_prefs`: `safeshade_vitals`, `safeshade_evidence`,
  `safeshade_sightings`, `safeshade_smarthome`, `safeshade_firmware`,
  `safeshade_rides`. Each has a pure Gson `…Json` codec, an interface, a
  DataStore and an in-memory implementation.
- **`AppContainer` order**: … `journeyRepository`, `rideLogRepository`,
  `voiceNoteRepository`, `escalationRunner`, `wearableWatch`,
  `healthConnectVitals`, `vitalsRepository`, `evidenceStore`/`evidenceSettings`
  (a hot `StateFlow` so the opt-in check is synchronous)/`evidenceRepository`,
  `sightingsRepository`, `smartHomeRepository`, `httpClient`, `smartHomeRunner`,
  `appStateRepository`, `cloud`, then **after cloud**: `firmwareRepository`,
  `sightingsCloud`. `init` starts the runners, points `RideLogSink` and
  `EvidenceService.onRecorded` at their repositories, samples device vitals,
  and feeds `SosTileState` and `WidgetFeed` from `appStateRepository.state`.
- **Routes added**: `SAFETY_VITALS`, `SAFETY_EVIDENCE`, `DEVICE_FIRMWARE`,
  `DEVICE_LOST`, `DEVICE_PAIR`, `DEVICE_RIDES`, `CIRCLE_SMART_HOME`,
  `CIRCLE_SMART_HOOK_EDIT?hookId=`. Bottom bar and motion untouched.
- **`LiveSensorData`** gained `heartRateBpm`, `spo2Percent`, `skinTempC`,
  `vitalsSource`, `hasVitals`. **`LocationState`** gained `provider`,
  `accuracyM`, `fixAt`, `fromDevice`. **`SafetySettings`** gained `quietWord`.
  **`TripKind`** gained `QUIET_WORD`. **`DeviceModel`** is new.
- **`DeviceLink`** gained `sightings`, `startSightingScan(ms)`,
  `stopSightingScan()`; three implementations (`RealDeviceLink`,
  `FakeDeviceLink`, `SwitchableDeviceLink` under `app/src/debug`).
- **Manifest**: `FOREGROUND_SERVICE_MICROPHONE`, `EvidenceService` (type
  `microphone`), Health Connect read permissions and its two activity-aliases,
  `<queries>` for Health Connect, Google Home and Alexa, `NFC` permission with
  `uses-feature required=false`, `SosTileService`, `SafeShadeWidgetReceiver`,
  shortcuts meta-data, the `FileProvider`.
- **Dependencies added**: `androidx.health.connect:connect-client 1.1.0`
  (Kotlin 2.0.21 metadata), `androidx.glance:glance-appwidget 1.1.1`
  (Kotlin 1.8.22 metadata). Both checked against the 2.2.10 compiler's
  metadata ceiling before adding; the toml comments say so.
- **Process-wide sinks** (the `LastKnownLocation` pattern): `RideLogSink`,
  `EvidenceService.onRecorded`/`state`, `SosTileState`, `WidgetFeed`.

---

## 5. The cloud, as it is now

Handoff7 §5 stands. Added in Phase 3:

- **`CloudClient` gained** `selectWhereIn(table, column, values, orderBy,
  descending, limit)` (because `firmware_releases` and `device_sightings`
  have no `circle_id`, so `select()` 400s on them) and `publicUrl(bucket,
  path)`; both implemented on the real and fake clients.
- **Resolver branches**: `vitals_samples` (push only), `evidence` (uploads
  `filesDir/evidence/<clip>.m4a` to the private `evidence` bucket at
  `<circle>/<clip>.m4a`, then the row; `EvidenceRepository.setUploadState`
  follows the result; a missing file is Failed with the reason, never Skip),
  `smart_home_hooks` (push and pull; `lastStatusCode` has no server column and
  is kept from the local copy). Vitals and evidence are not pulled: a second
  guardian's phone does not see them yet.
- **`SightingsCloud`** and **`FirmwareCloud`** as described in §3.2.
- **Backfill**: hooks are in the identity block; vitals and evidence are
  deliberately not (500 telemetry rows would evict identity records; evidence
  is opt-in).
- **No migrations this phase.** The 0001 schema already had every table.

---

## 6. Phase 4 — the brief

**UI/UX only.** The all-screen polish sweep as a redesign pass, plus the
user's own list, which jumps the queue when it arrives.

From handoff7 §7, unchanged: pages are too text heavy; screenshots of all
routes in both themes and at 1.3×; fewer paragraphs, more instruments, plates
and rows; dark theme on the map picker and the intro; `ChipRow` focus ring;
back-chevron/title overlap; `WhyDisclosure` count; `OptionWay` and the
check-in constants into `ui/board/`; `PlateField` from `ui.screens.safety`
into `ui/board/`; dead `ui/navigation/` and the stranded
`ONBOARDING_RELIABILITY` route; `material-icons-extended` dropped; the weekly
report caption.

Added by this phase's own pages, none photographed in dark or at 1.3×:

- **Vitals**: the three large readouts at 1.3× on a narrow phone; the
  "Recent" rows carry five readouts across and will wrap.
- **Evidence**: the recordings list's Delete appears only while playing,
  which is discoverable only by the note under it; consider a swipe or a
  trailing action.
- **Smart home editor**: two chip rows and three fields is a long page; the
  Send a Test supporting line doubles as the result line.
- **Pair a wearable**: the three silhouettes at 84 dp in a third of the width
  each; check the 5G's aerial at 1.3×.
- **Lost**: the per-device detail line is three sentences with two dashes
  when nothing has been heard; it could be two readouts.
- **Firmware**: the release row's state word switches between five meanings;
  a legend or a second line may be needed once a release exists.
- **Device page**: four new ways (Nearby, Firmware, Lost, Ride log) make the
  Ways bank eleven rows; consider grouping.
- **Board**: the nudges plate sits between the gauges and the Shady stage
  with no heading.
- The five small additions (quiet word, positioning readout, own lights,
  nudges, Walk Home) were placed inside existing pages by pattern and not
  re-photographed after the surrounding page changed.

---

## 7. Phase 5 — the test list

Everything in §8 "Not verified", plus:

- **RLS with a second account** and accept-invite (handoff7 §11).
- **The 401 burst / `ensureFreshSession`** (handoff7 §9).
- **The sync engine in airplane mode** going to Failed.
- **The evidence FK ordering**: on a first drain an `evidence` row can go
  before its `alerts` row and take a 23503; it should retry with backoff and
  land. Watch the gateway log for it.
- **The webhook signature timestamp is `event.at`**, not send time; a receiver
  enforcing freshness will refuse a firing replayed at cold start. Decide
  whether to swap to send time.
- **Low-battery cold start**: `WatchState` starts false in memory, so a
  restart while the battery is still low produces one more `low_battery`
  firing (a repeated true statement).
- **The mandated-string check**: grep the UI for "planned", "simulated",
  "representative", "coming soon" before every release. Run at the Phase 3
  close: zero hits.

---

## 8. Verified on the device, and not

Test device: Redmi Note 10S, MIUI 14, Android 13 (API 33), 1.0× font scale,
light theme, signed in with Google. **No wearable was connected at any point in
Phases 1–3.** No Health Connect, no NFC, no Google Home or Alexa app on the
phone. Everything below was driven over adb and read back from uiautomator
dumps and screenshots.

### Verified in Phase 3

- **Safety hub**: the Vitals way (dash, "No reading yet") and the Evidence
  way (Off).
- **Vitals page**: renders; Health Connect reads Not installed with the Play
  Store button under it; the threshold dials draw.
- **Evidence page**: a ten-second test recording with the system microphone
  indicator, "Recording 2 s / Stop Now" while running, a 43 KB m4a on disk,
  two clips listed "On this phone", tapping one reads Playing and reveals
  Delete; the sound meter to Listening reading 53 dB with the calibration
  sentence.
- **Pair a wearable**: all three silhouettes drawn; the Spark facts (Tag: dash,
  "This phone has no NFC").
- **Lights**: "On its own" reads Brake light: Bike profile, Path light: Armed
  (the phone is on the Elderly profile).
- **Lost**: a sweep to Listening and back to Idle with "Last swept just now";
  the reporting rocker On with "0 heard today".
- **Ride log** empty state; **Find the device**'s SOURCE / ACCURACY / AGE row;
  **Silent SOS**'s Quiet word section; the **Device page**'s Nearby, Firmware,
  Lost and Ride log ways.
- **Firmware**: renders; Check for Updates answered "Nothing published.
  Checked just now." **from the live table**.
- **Smart home**: Google Home and Alexa read Not installed, Matter reads
  Ready after their taps; the editor; a test post to `https://httpbin.org/post`
  answered "Delivered. The address answered 200."; the hook saved and listed
  Armed with the firing under Recent.
- **Share as PDF**: a 197 KB file under `files/reports`, the share sheet
  naming it, and the PDF pulled raw (`adb exec-out`) renders the full report
  text (wearer, type, time, outcome, location, medical ID, contacts,
  timeline, footer).
- **Emergency card**: "Write to a tag" reads a dash with "This phone has no
  NFC, so it cannot write a tag."

### Not verified — say this to the user, plainly, every time

- **Any vitals reading**: no wearable sends fields 7–9; Health Connect is not
  installed, so Allow, Read Now, the history rows, NEEDS_UPDATE and a flag
  have never been drawn live. The Play Store button's launch.
- **The fall-triggered and SOS-triggered recording starts** (no alert was
  raised; the same service was driven by the test button). The notification's
  Stop action. Durations other than 10 s. Delete. The loud verdict itself.
  The API 34+ foreground-start refusal. Evidence and vitals **reaching the
  cloud** (resolver branches exist; no drain was observed with an entry).
- **Any automation firing on a real trigger**; Home Assistant and IFTTT
  bodies against real receivers; the http-on-LAN rule against a real Home
  Assistant.
- **A sighting of another SafeShade** (none in range): the community report,
  the `lastSeen` query, the map action and the dedupe are unit-tested only.
- **A firmware release** (the table is empty): download, verify, chunking and
  install have never run against a device. `EXT VER` has never been sent.
- **The leash's Near/Drifting/Far words; the guide action** (no link).
- **A ride** (no journey run). **The quiet word raising an alert** from a real
  message. **Weather nudges** (the Board was not synced this pass).
- **The tile, the shortcuts and the widget** on the phone (built and declared,
  not placed or tapped); the widget's dark palette. **NFC writing**, reader
  mode, the Disabled and Ready states.
- **Walk Home.** **The PDF for a fall with a ladder run and coordinates.**
- **Dark theme, 1.3× text and TalkBack** on every page added this phase.
- Everything carried from handoff7 §8.

---

## 9. Known bugs, debt and firmware owes

### Fixed in Phase 3 (recorded so nobody re-diagnoses them)

- **`setNavTarget` was a live orphan**; a zone row now sends it.
- **A test firing before Save belonged to a stranger**: the editor generated a
  new hook id on every `toHook()`; one id is now held for the whole edit.
- **`tools/adb/drive.sh` could not run `tapfind.py`** under
  `MSYS_NO_PATHCONV` (python cannot open `/c/...`); `TOOLS` now resolves to a
  Windows path (`pwd -W`).
- **Evidence recorded for the wrong alert kinds** and again on a cold start;
  **the widget republished once a second** while telemetry flowed. Both fixed
  in the review commit, §3.4.

### Carried debt (new this phase)

- **`Routes.BOARD_LINK` still mounts `ComingSoonPlate`** ("Coming in this
  build") in `MainNavGraph`. Nothing navigates to it (the board's own row goes
  to `DEVICE_PAIRED` deliberately). Phase 4 should delete the route and the
  plate rather than leave a dead page that breaks the wording rule in spirit.

- **`downloadAndVerify` returns `OtaStep.Verifying` to mean verified**;
  `OtaStep` has no Verified member. Add one when `device/` is next opened.
- **`DeviceModel.fromAdvertisedName` is substring-based**: "SafeShade 15G"
  would read as FIVE_G. Fine for three names; word-boundary it if naming
  diversifies.
- **Duplicate vitals-source vocabulary**: `VitalsSource` (enum, `Models.kt`)
  and `VitalsSample.source` (string, `VitalsStore.kt`) both exist; the enum is
  used on the link, the string on the store and the server. Map one onto the
  other.
- **`EvidenceRecorder.MIN/MAX_DURATION_SECONDS` and
  `MIN/MAX_EVIDENCE_SECONDS`** in `EvidenceStore.kt` are the same values
  twice.
- **`SoundMath.approxSplOf` is `dbfs + 90`**, so full scale reads 90; the 120
  ceiling is a clamp only reached above full scale.
- **Matter-provider hooks never POST**; they record a firing whose reason says
  so. **`homeassistant.local`** is rejected by the URL validator (§2).
- **Vitals and evidence are push-only.** A second guardian's phone sees
  neither.
- **`ProductSilhouette`** draws the Spark from the deck; no Spark hardware or
  firmware exists to check it against.
- **The Lost page's reporting rocker is `rememberSaveable`**, not persisted:
  it resets on a cold start. Persist it in `SightingsStore` when the page is
  next touched.
- **`SightingsCloud.lastSeen` is fetched only while the Lost page is open and
  signed in**, once per change of the lost set.
- **`Sighting` from the sweep carries no manufacturer data today** (the
  firmware advertises none); `MeshAdvertCodec.parseManufacturerData` is ready
  for when it does.
- Everything in handoff7 §9 carried debt still stands (`PayloadResolver`
  `author_id`, `FallAlertEvent` coordinates, the 401 hook, the eleven-digit
  contact, `PlateField` placement, the screen-off timeout on the test phone).

### Firmware owes the app

Handoff7 §9's list stands. Haiku's read of the firmware this phase added, with
line numbers in the commit `b359a16` message's sources:

- **Brake light**: `deltaMotion > 15000` then a 1500 ms timer; Bike profile
  strobes red at 80 ms, other profiles solid red. **No BLE path.**
- **Path light**: Elderly profile and ambient < 1000 lux → warm amber on all
  16 LEDs and the headlamp. **No BLE path.**
- **No Spark or 5G string anywhere**; the advertised name is
  `deviceDisplayName`, default "SafeShade S1", settable by `EXT DEVNAME`.
- **No removal, strap, wear or tamper detection** of any kind.
- The eight EXT tags are DEVNAME, MODE, MED, CHECKIN, GEOFENCE, SMSALLOW,
  QUIET, NAV; seven LED patterns 0–6. `VER`, `OTA`, `SOSRELAY`, `ERS`, the
  VOICE chunk path, telemetry 7–9 and a MESH advertisement remain designed
  and unit-tested on the app side only (`DeviceCapabilities.awaitingFirmware`).

---

## 10. Traps, and the tools that route around them

Handoff7 §10 and handoff6 §3 still apply. Added this pass:

- **`./gradlew test --tests "…"` fails** on this AGP: `test` is an aggregate
  lifecycle task and rejects `--tests`. Use
  `gradlew.bat testDebugUnitTest --tests "com.safeshade.*"`. `CLAUDE.md` still
  shows the wrong form and is not ours to edit.
- **`adb shell run-as … cat file > local` corrupts binaries on Windows**
  (`\n` → `\r\n`; a 197 651-byte PDF arrived as 198 582 and would not
  inflate). Use `adb exec-out run-as com.safeshade cat …`.
- **`tapfind` reads a stale dump right after a fling.** A fast swipe followed
  immediately by `tapfind` returns NOTFOUND for a row that is plainly on
  screen. Swipe with a longer duration (800 ms), sleep 2–3 s, then find.
- **A `Way`'s rocker is at the row's right edge, not its centre.** `tapfind`
  returns the row centre; tapping x ≈ 890 (of 1080) at that y throws the
  switch, and x = 985 misses it.
- **The `Bash` tool's heredoc parser fails on large Python with quotes**
  (handoff7): every multi-file wiring pass this phase was written with `Write`
  to the scratchpad and run with `python <path>`. Two scripts still failed
  mid-way on a stale anchor; because each writes its files only after all its
  substitutions pass, a failed script leaves nothing half-applied — but a
  script that spans several files writes the earlier ones before a later
  anchor fails. Keep one file per script, or write all at the end.
- **Seven agents on one tree**: every agent's `assembleDebug` saw other
  agents' half-edits and reported the tree red for reasons outside its scope.
  The rule that saved it: an agent only reports errors naming its own files,
  and the orchestrator compiles once everyone has stopped. Two agents both
  needed the manifest; giving it to one and having the other report its
  needs avoided a conflict. `Routes.kt`/`MainNavGraph.kt` were never given
  to an agent.
- **An agent that "pauses to wait for its background build" has stopped.**
  Send it a message telling it to run the build in the foreground and report.
- **`private val AppState.Ready.wearerName`** is an extension in
  `MainNavGraph.kt`, not a property; from `AppContainer` use
  `selectedWearer?.name ?: deviceSettings.wearerName`.
- **Health Connect's `<queries>` entry is load-bearing**: without it the SDK
  cannot tell "not installed" from "needs update".
- **The Play Store is not in `<queries>`**, so a resolve check on the Health
  Connect install intent returns null on exactly the phone that needs it;
  the launcher catches `ActivityNotFoundException` instead.
- **A gauge's `stub` flag, a "planned" lamp, a "representative" caption** —
  none may return. The rule is in `DESIGN.md`.

---

## 11. What the user still has to do

Handoff7 §11 stands (push; the second account; paste the auth templates'
contents; leaked-password protection; the two Vault secrets; the ladder
default; the eleven-digit contact; their UI/UX list). Added:

- **Push.** Ten v2.5.0, 27 v2.6.0 and four v2.7.0 commits are local. Ask
  before pushing.
- **Place the widget and the tile** on the test phone (long-press home;
  Quick Settings edit) so Phase 5 can photograph them.
- **Publish one `firmware_releases` row** (any model, any small `.bin` in the
  public `firmware` bucket with its SHA-256) so the download and verify path
  can be driven. Install can only ever end in "acknowledged, no version" on
  the shipped firmware, which is the honest outcome to photograph.
- **Install Health Connect** on the test phone, or bring a phone that has it
  with a watch app writing to it, for the Allow / Read Now path.
- **A phone with NFC** and a blank NTAG for the tag write.
- **Confirm the quiet word → `sos` mapping** for smart-home hooks (§2).
- **Decide whether `.local` hostnames are allowed over http** for Home
  Assistant (§2).
- **Their UI/UX list**, which jumps the Phase 4 queue.

---

## 12. Phase 3 close

**What shipped** is §3, item by item against handoff7 §7's brief; every item
of that brief landed except that vitals and evidence do not yet pull to a
second phone (§5). **What was not verified** is §8. The decisions taken without
the user are listed in §2 and are all reversible.

**Context.** This session was not compacted, but it ran long: four commits,
eleven sub-agents, and every wiring pass through scratchpad scripts. The
working state that matters is in the commits, this file, `DESIGN.md` and the
memory directory; nothing is held only in the conversation. The scratchpad
scripts (`wire_*.py`) are throwaway and not in the repo.

**Continue or hand over.** Hand over. Phase 4 is a different shape of work
(a redesign pass over every route, in both themes and at 1.3×, with the user's
own list first) and wants a fresh session that reads this file, then
`DESIGN.md` in full, then photographs every route before changing anything.
The sub-agent allocation in §1 holds, but Phase 4 is orchestrator-heavy: the
screens are the orchestrator's, and Haiku sweeps (greps for copy that fails
the "states a consequence" test) are the useful delegation. Before Phase 4
opens, the cheapest high-value hour is still handoff7 §11's second-account
check.
