# SafeShade — Session Handoff #1

**Written:** 2026-09-04, immediately after a successful demo day.
**Written by:** Claude Sonnet 5, closing out a long session (BLE app + dual ESP32 firmware) that ran out of context.
**Status of this repo right now:** Working tree has substantial **uncommitted** changes (see "Git state" below) — nothing from this session has been committed yet. Do not `git reset`/`checkout`/`clean` anything before reading that section.

Demo day is over and it went well — the panel liked the product. This doc exists so the next
session can pick up cold: what exists, what's fake, what's fragile, what's next. The stated plan
for upcoming sessions (from the user directly):
1. **Next session**: bug fixes (both user-discovered and known-but-unfixed) across firmware + app.
2. **Also next session**: turn demo-stubs into real working features, add missing features from
   the pitch deck.
3. **Also next session**: refine/finalize the *actual hardware components* (user is about to buy
   and interface real parts) to close out **TRL 4** and move into **TRL 5 real-world testing**.
4. **Next-next session** (later): PCB design + 3D-printed enclosure model.

---

## 0. Quick facts / environment

- Repo root: `C:\Dev\SafeShade`, git repo, branch `master`, remote origin exists (not verified this
  session — check before assuming push access).
- Two physical devices, two firmware sketches, two COM ports:
  - **Mainboard** (ESP32-C3, XIAO_ESP32C3 board def) — `docs/SafeShadev21/SafeShadev21.ino` — **COM15**.
  - **Cellular/GNSS gateway** (ESP32-S3 + Quectel EC200U-CN) — `docs/SafeShade_Gateway/SafeShade_Gateway.ino` — **COM16**.
  - Gateway runs a WiFi SoftAP bridge: SSID `SafeShade_GW`, pass `safeshade2026`, HTTP base
    `http://192.168.4.1` (`GATEWAY_BASE_URL` in the mainboard sketch). Mainboard connects to this
    AP and polls the gateway's `/gps`, `/status`, `/messages`, `/reply` HTTP endpoints.
- Android app: single module, Kotlin + Jetpack Compose, no ViewModel layer (state lives in
  `SafeShadeApp.kt` via `remember { mutableStateOf(...) }`, threaded down as params/callbacks).
  `versionName` is currently `"1.1"` in `app/build.gradle.kts`. No `adb` in this dev environment —
  **the user always builds via Gradle here, then installs/tests on their own physical phone.**
- No companion server. Only network dependency is the free Open-Meteo weather API + the
  gateway's own cellular data connection.

### Build/flash commands that actually work in this environment

```bash
# Firmware compile (Bash tool; must prepend PATH each time in a fresh shell)
export PATH="/c/Program Files/Arduino CLI:$PATH"
arduino-cli compile --fqbn "esp32:esp32:XIAO_ESP32C3:PartitionScheme=no_ota" docs/SafeShadev21
arduino-cli upload -p COM15 --fqbn "esp32:esp32:XIAO_ESP32C3:PartitionScheme=no_ota" docs/SafeShadev21

# Gateway firmware — same pattern, COM16, confirm its FQBN before assuming it's identical
# (it's an ESP32-S3 board, not XIAO_ESP32C3 — check docs/SafeShade_Gateway for its actual board def
#  / any board-specific notes in EC200U_Project_Notes_and_Troubleshooting.md before compiling)
```

- **`arduino-cli monitor` does NOT reliably capture serial output in this environment.** The
  pattern that actually works is a raw PowerShell `.NET System.IO.Ports.SerialPort` one-liner:
  open the port at 115200 baud, poll `ReadLine()` in a timed loop, catch `TimeoutException`. Use
  the PowerShell tool for this, not Bash.
- Android:
  ```
  ./gradlew.bat compileDebugKotlin --console=plain   # fast per-file check, use this while iterating
  ./gradlew.bat assembleDebug --console=plain          # full build, run before declaring done
  ```
  Run these via the **PowerShell tool** (it's the Windows Gradle wrapper), not Bash.

---

## 1. Working style notes for the next session (how this user likes to work)

- The user tests everything themselves on real hardware/phone — this environment cannot flash
  the app or watch the OLED. **Always say explicitly which fixes are unverified** (compiled/built
  clean vs. actually confirmed working on-device). Don't claim a fix "works" — say it "compiles
  clean, needs your hands-on confirmation."
- Under time pressure the user explicitly invited maximum parallel sub-agent orchestration
  ("make as many agents as you want, all Sonnet 5 Medium-High Effort, running concurrently... you
  are the manager/CEO"). This is a standing permission the user wants carried forward — **feel
  free to spawn multiple concurrent `general-purpose` agents for the next session's bug-fix +
  feature pass**, especially given how large the surface area is (firmware + BLE protocol + many
  Compose screens).
  - Pattern that worked well: give each agent an **exclusive set of files** to avoid concurrent-edit
    conflicts, with extremely detailed self-contained prompts (exact file:line references, exact
    BLE tag/payload contracts to reuse, exact patterns already established in the codebase to
    copy). Reserve shared "hub" files (`SafeShadeApp.kt`, `MainActivity.kt`, and honestly
    `SafeShadev21.ino` since it's one giant file most firmware bugs live in) for a single
    coordinator pass at the end, since those are where concurrent edits collide.
  - When agents are still mid-flight and a build is run prematurely, transient "unresolved
    reference" errors are expected — don't panic-fix, just wait for the agent and re-run.
- The user writes fast, dense, sometimes-typo'd requests with multiple bugs/features bundled in
  one message ("adadd", "refelcted", "showcasein the demoday"). Parse generously; when a sub-item
  is genuinely ambiguous, make the smallest reasonable interpretation, ship it, and explicitly flag
  the interpretation afterward rather than blocking on a clarifying question — this matches how the
  user operates under demo deadlines. (One such flag is still outstanding — see §5.)
- The user cares about UI/UX being "Apple/Google built it" quality, not "hackathon demo app"
  quality — polish requests should be taken seriously, not treated as nice-to-have filler.
- The user is explicit about **honesty in copy**: don't label real features as fake or fake
  features as real. E.g. the location map snapshot is a fixed-coordinate embed, not live GPS
  tracking yet — but the user explicitly asked to present it plainly/confidently without a
  "demo"/"non-live" disclaimer, i.e. don't undersell it either. Read the room per-feature; when in
  doubt, prefer "this is real and works" framing for genuinely-wired features, and be honest in
  code comments/internally about what's a demo stub vs. real.
- hugeicons MCP tools exist (`mcp__hugeicons__*`) but failed to connect earlier this session —
  worth retrying next session since a real icon pass was requested but fell back to Material Icons
  Extended.

---

## 2. Architecture recap (see root `CLAUDE.md` for the canonical version — keep both in sync)

- **BLE is the core integration point**, `BleManager.kt`. Single custom GATT service
  (`SERVICE_UUID`), dedicated characteristics for weather, alerts, guardian messages, device
  replies, health data, settings — UUIDs must match firmware exactly.
- **GATT operation queue is mandatory**: Android allows only one outstanding
  write/read/descriptor-write/MTU request at a time. Every GATT op must go through
  `enqueue()`/`drainQueue()`. Never call `gatt.writeCharacteristic()` etc. directly — this has
  caused real bugs before.
- **`EXT_CHAR_UUID` tagged-command protocol**: `"TAG:payload"` strings, single unified ack stream
  on `ACK_CHAR_UUID` (`"ACK:TAG"`). `BleManager.sendExtCommand(tag, payload)` on the Android side;
  firmware-side dispatch is `ExtCallbacks::onWrite`, a colon-split `tag`/`payload` into an
  `else if (tag == "...")` chain, each branch ending `tone(PIN_BUZZER, 1700, 40); sendAck(tag);`.
  **Current tag list**: `DEVNAME`, `MODE`, `MED`, `CHECKIN`, `GEOFENCE`, `SMSALLOW`, `QUIET`,
  `NAV`. Any new BLE-driven feature should almost always be a new tag on this same protocol rather
  than a new characteristic.
- **Characteristic directionality matters**: `MESSAGE_CHAR_UUID` is Guardian→Companion (buzzes +
  shows message on-screen). `REPLY_CHAR_UUID` is Companion→Guardian (quick replies). Writing a
  reply to the message characteristic makes firmware treat it as a new incoming message — this was
  a real historical bug, don't reintroduce it.
- Payloads are comma-delimited strings; free-text fields **must** be sanitized
  (`.replace(",", " ")`) before being sent, since the firmware's fixed-token CSV parser doesn't
  escape commas. This bit us once already (medical ID corruption bug, fixed this session) — **any
  new free-text field sent over BLE needs this same sanitization from day one.**
- **7 `PersonaMode` values**: ELDERLY, KIDS, BIKE, PET, HELMET, WRIST, BACKPACK. String-matched
  (not ordinal) between Android and firmware. Each now has an `accentColor: Color` in
  `data/Models.kt`.
- Two-way SMS: gateway polls `AT+CMGL="REC UNREAD"`, queues incoming SMS, serves via `/messages`;
  mainboard's `pollGatewayMessages()` (3s interval) fetches + displays; replies go out via
  gateway's `/reply` endpoint. Android side also has its own system-SMS bridge
  (`SmsReceiver.kt`/`SmsMessageEventBus.kt`, new this session, currently **uncommitted/untracked**)
  that folds real incoming SMS into the same message history, deduped against BLE-arrived
  duplicates within an 8s window.
- **Message Allowlist** (renamed this session from "SMS Allowlist"): Guardian-configurable list of
  phone numbers; empty = allow everything (no regression from pre-allowlist behavior); non-empty =
  only matching senders' messages actually display on-device (compares last-10-digits). Firmware
  tag `SMSALLOW`, Android `Preferences.kt`'s `smsAllowlist` flow + `BleManager.sendSmsAllowlist()`.

---

## 3. What shipped this session (all uncommitted — see §Git state)

### Firmware (`SafeShadev21.ino`)
- Fixed SOS-lock-in dismiss failing: the SOS siren draw case used blocking `delay(100)` calls that
  starved button reads for ~200ms/iteration; rewritten as non-blocking `millis()`-timed phase
  toggle.
- Fixed device needing a power-cycle after "sleep": added `Wire.setTimeout(1000)` after
  `Wire.begin()` — an I2C hiccup on the MPU6050 read could hang `loop()` forever with no timeout.
- **Auto-sleep removed entirely** (most recent change, per explicit user request) — the OLED/device
  must never sleep/shut down on its own. Only Shady's cosmetic "sleepy" mood
  (`lastMotionTime`/`SLEEP_TIMEOUT_MS`-driven) still exists and is unrelated to device power state.
- LED pattern BLE writes now call `updateRGBPattern()` immediately instead of only applying next
  time the RGB menu screen happens to redraw.
- SOS alarm volume (`settings.sosVolume`, 0-100) is now actually wired to a duty-cycle-modulated
  tone, previously a dead variable.
- Elderly mode: Shady now excluded (`shadyAllowed` gained `MODE_ELDERLY`); big clock redrawn
  manually with two digit groups + drawn dots for the colon (font glyph looked like a stray line at
  32px); a later fix also removed a device-name label (`"SafeShade S1"`) that was rendering right
  under the clock and looked like a stray "1".
- Wrist mode watch face changed from a round analog bezel to a rectangular digital-watch case.
- New `QUIET` tag (Kids mode): `"start:end"` hour range, mutes the *non-critical* incoming-message
  chime during the window. **Fall/SOS alerts are never gated by this, by design.**
- New `NAV` tag (Bike/Helmet modes): `"lat:lon:label"`, computes live haversine distance + compass
  bearing from the gateway's GPS fix to a Guardian-set destination, shown on the Location screen.
  Explicitly a "good enough to demo" stand-in for real turn-by-turn, not the real thing yet (see
  §4).
- Message-arrival buzzer chimes (both BLE and gateway-polled paths) gated by
  `inQuietHours()` when `activeMode == MODE_KIDS`.
- Firmware **compiled clean** at 1,495,149 bytes (71% flash) / 43,288 bytes RAM (13%) as of the
  last change (auto-sleep removal + QUIET + NAV) and **was flashed to COM15 and boot-verified**
  before the demo (per the prior turn's completed work — reconfirm this is still the exact binary
  on-device if anything seems off, since no further firmware changes have landed since).

### Gateway firmware (`SafeShade_Gateway.ino`)
- Untouched this entire session beyond the two-way-SMS-messaging work from an earlier session.
  Confirmed working via mainboard's HTTP polls. **No changes pending flash here.**

### Android app
- `BleManager.kt`: medical-ID CSV corruption fixed (comma-sanitize free-text fields before send);
  BLE disconnect-toggle regression fixed (root cause: `disconnect()` calls `gatt.close()`
  synchronously, which frequently prevents Android from ever delivering the async
  `STATE_DISCONNECTED` callback — a `userInitiatedDisconnect` flag could get stuck `true` and
  silently suppress the next connect's auto-retry; now reset defensively in `startScanning()` and
  the `STATE_CONNECTED` branch too, not only in the disconnect callback).
- `MainActivity.kt`: real `syncInProgress` state around `fetchAndSendWeather()`, driving a genuine
  loading spinner + "Syncing..." label on the Home screen's Sync button (not a fake timer).
- `data/Models.kt`: `PersonaMode.accentColor` (7 distinct colors), `FallAlertEvent.location`/`.note`.
- `data/Preferences.kt`: `smsAllowlist` flow + setter.
- `SafeShadeApp.kt` (coordinator-owned hub): wires `syncInProgress`, `smsAllowlist`
  collect/persist/BLE-resync-on-connect, populates real `FallAlertEvent.location`/`.note` from live
  location + sensor data (not fabricated).
- `HomeScreen.kt`: Sync button loading state; old small `ActiveModeChip` replaced with a modern
  `ActiveModeBanner` moved to directly below the Sync button, using each mode's `accentColor`; its
  accent bar was later changed from a left-vertical strip to a top-horizontal strip per explicit
  request.
- `ProfileScreen.kt` / `DeviceScreen.kt`: per-mode chip colors; LED pattern control relocated from
  Device→Profile (plus a cosmetic no-op "Auto" 8th chip, explicitly local-UI-only, no BLE write);
  Appearance card redesigned as a horizontal segmented control; App Info card expanded + relocated
  Profile→Device; **substantially expanded per-mode controls**: Kids gets Quiet Hours (real `QUIET`
  BLE command), Bike/Helmet get Navigation (real `NAV` BLE command, destination lat/lon + label
  input), Pet gets an Owner Details editor (reuses the existing Medical ID sync path, no new wire
  protocol).
- `SafetyScreen.kt`: "Fall Alert History" renamed "Alert History"; `AlertLogItem` redesigned with a
  2-stage timeline plus conditional location/note rows.
- `GuardianScreen.kt`: `DevicePhoneNumberCard` + new allowlist card moved to bottom of the tab;
  "SMS Allowlist" renamed "Message Allowlist" throughout user-facing copy; map snapshot card fixed
  (was calling `it.loadUrl(url)` in Compose's `update` lambda, which re-runs on every recomposition
  and was interrupting its own page load before it ever rendered — removed `update`, added
  `domStorageEnabled` + `WebViewClient`); disclaimer text removed from the map card per explicit
  request.
- Full `./gradlew.bat assembleDebug` **succeeded clean** as of the last change this session.

---

## 4. Known bugs / demo stubs / things that are NOT real yet

Be upfront about these with the user before they get surprised in front of someone technical:

- **Bike/Helmet "Navigation"** is real distance+bearing math from live GPS to a Guardian-entered
  destination — but it is *not* turn-by-turn routing (no road graph, no route line, just
  straight-line distance/compass direction). Was explicitly scoped by the user as "even if not
  perfectly working, enough to showcase in the demo" — fine for now, but a real routing feature (or
  at minimum a route polyline on the map) is a strong pitch-deck-parity candidate for next session.
- **Location map snapshot** is a fixed-coordinate OpenStreetMap embed (Campus 12, KIIT University,
  Patia — Lat `20.35520740274678`, Lon `85.81951928060086`), not live device GPS tracking on the
  map. Presented plainly per user request (no disclaimer), but internally: this needs to become
  live-GPS-driven (gateway already has a real GPS fix via `/gps` — the map card isn't consuming it
  yet). Good candidate for "make the stub real."
- **"Auto" LED pattern chip** in Profile is explicitly a no-op — local UI state only, no BLE write,
  no firmware behavior. If the pitch deck implies auto/adaptive LED behavior, this needs a real
  firmware-side "pick pattern based on mode/context" implementation.
- **Pet mode "Owner Details"** reuses the Medical ID contact fields rather than being genuinely
  pet-specific data (owner name/contact only) — works for the demo, but if pitch deck describes
  richer pet-specific data (e.g. pet name/breed/vet contact), that's a real gap.
- The "Set Check-in, Add button in the SMS allowlist" complaint from the user's last message was
  **never fully resolved** — I interpreted it as wanting visual polish and added an icon to the
  Allowlist screen's "Add" button, left "Set Check-In" untouched (it already used the app's
  standard button pattern). **This was never confirmed correct by the user — re-raise it early
  next session** and get a concrete description of what was actually wrong with either button.
- Elderly clock colon-glyph fix and Wrist watch-face redesign were compiled clean but their visual
  correctness could only be confirmed by the user on real hardware — reconfirm both still look
  right (the user did test and only reported the "extra 1" and round-face complaints as remaining
  issues afterward, so these are likely fine, but worth a quick glance).
- No real automated test suite exists — `ExampleUnitTest`/`ExampleInstrumentedTest` are still
  placeholders. Given the move toward TRL 5 real-world testing, this is worth prioritizing at least
  for the BLE protocol layer and firmware's pure-logic helpers (haversine/bearing/quiet-hours).
- `docs/EC200U_Project_Notes_and_Troubleshooting.md` and `docs/PROJECT_CONTEXT.md` exist from
  earlier sessions — skim both before starting hardware-finalization work; they likely contain
  antenna/wiring caveats not repeated here (see also this assistant's cross-session memory, which
  notes an "unresolved antenna issue" on the EC200U gateway — verify current status, don't assume
  fixed).

---

## 5. Git state — IMPORTANT, read before doing anything destructive

**Nothing from this entire session (or the prior sessions that built the gateway/adaptive-modes/
retro-UI work) has been committed.** `git log` still shows only 3 commits, the most recent being
`303b1e8 Add EC200U cellular/GNSS gateway, 7 adaptive modes, retro OLED UI, and full app sync`,
which predates all of the bug-fix/polish work described in this document.

Current `git status --short`:
```
 M app/build.gradle.kts
 M app/src/main/AndroidManifest.xml
 M app/src/main/java/com/safeshade/BleManager.kt
 M app/src/main/java/com/safeshade/EmergencyActions.kt
 M app/src/main/java/com/safeshade/MainActivity.kt
 M app/src/main/java/com/safeshade/data/Models.kt
 M app/src/main/java/com/safeshade/data/Preferences.kt
 M app/src/main/java/com/safeshade/ui/SafeShadeApp.kt
 M app/src/main/java/com/safeshade/ui/screens/DeviceScreen.kt
 M app/src/main/java/com/safeshade/ui/screens/GuardianScreen.kt
 M app/src/main/java/com/safeshade/ui/screens/HomeScreen.kt
 M app/src/main/java/com/safeshade/ui/screens/ProfileScreen.kt
 M app/src/main/java/com/safeshade/ui/screens/SafetyScreen.kt
 M docs/SafeShade_Gateway/SafeShade_Gateway.ino
 M docs/SafeShadev21/SafeShadev21.ino
?? app/src/main/java/com/safeshade/SmsMessageEventBus.kt
?? app/src/main/java/com/safeshade/SmsReceiver.kt
```
15 modified files, 2 new untracked files, +2096/-431 lines. **First thing next session should do
(after confirming with the user) is propose committing this working set** — it's the entire
demo-day-ready state and leaving it uncommitted risks losing it. Don't commit without asking first
per standard practice, but flag it proactively — this is a lot of uncommitted work sitting in the
working tree.

---

## 6. Suggested first steps for next session

1. Confirm this handoff doc is accurate — ask the user to re-verify auto-sleep-off, BLE
   reconnect, map rendering, and the SOS dismiss fix actually held up live during the demo (they
   said it "went really well" but that's not the same as "every fix confirmed").
2. Propose committing the current working tree (see §5) before making further changes, so there's
   a clean rollback point.
3. Get the user's list of demo-day-discovered bugs (mentioned but not yet enumerated in this
   session) and the pitch-deck feature list to diff against what's real vs. stubbed (§4).
4. Given the TRL 4→5 hardware push, ask what physical components are actually being purchased/
   swapped in (sensors, buzzer, RGB, battery, enclosure-adjacent connectors) — firmware pin
   mappings and power assumptions will likely need updates once real parts replace whatever
   breadboard/dev-kit config was used for the demo.
5. Re-apply the multi-agent parallel-fix pattern from §1 for the bug-fix + stub-to-real pass —
   it worked well and the user has explicitly pre-authorized it.
