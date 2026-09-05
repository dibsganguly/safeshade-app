# SafeShade — Session Handoff #2

**Written:** 2026-09-04, updated across 6 further rounds through 2026-09-06 (see the dated section
headers below — §1 is the original pass; §4-§9 are follow-up rounds in the same overall arc).
**Scope so far:** Mainboard firmware only (`docs/SafeShadev21/SafeShadev21.ino`). Gateway firmware
(COM16, `docs/SafeShade_Gateway/`) and the Android app (`app/`) have **not been touched** across any
of these rounds — this doc is entirely about the mainboard.
**Status as of the last round (§9):** Compiles clean, flashed to COM15, boots cleanly (WiFi/gateway
polling observed, no panics/watchdog resets across every round's boot check). **Real hands-on testing
by the user is still outstanding** for most of what's listed below — "flashed and boot-checked" is not
the same as "verified working," especially for the SOS/Fall lock-in flows, the rotary encoder feel, and
the alert-retry/network-down states from §9.
**Not committed** — confirmed via `git status` at the time of writing: `SafeShadev21.ino` is still just
modified in the working tree (plus an unrelated `CLAUDE.md` modification and a few untracked files that
aren't this doc's concern), nothing from any of these 7 rounds has been committed yet.

The plan the *first* round (§1) executed from is preserved at
`C:\Users\KIIT0001\.claude\plans\for-this-pass-firmware-hashed-clover.md` (not in the repo, and not
representative of §4 onward, which were direct iterative fixes with no separate plan file) if a full
rationale/design trace for that first pass is ever needed.

---

## 0. For the next session — Android app pass

This is the one section written for that specific pass, not firmware history. Everything else below is
chronological firmware detail, most of it irrelevant to app work.

**Nothing here has been tested against the actual app code** — every item below is inferred from the
firmware side of the wire protocol only. Verify against `app/src/main/java/com/safeshade/BleManager.kt`
and `data/Models.kt` before assuming any of it is already handled.

### Protocol changes the app should account for
1. **`MODE_AUTO`** (§1) — an 8th persona, now the **default mode on a fresh device** (was Backpack).
   Firmware sends `ACK:MODE:AUTO` for it. Check: does the app's mode picker list/handle the string
   `"AUTO"`, or will it show as unrecognized/blank? `modeFromName()`'s fallback for any *other*
   unrecognized string is still `MODE_BACKPACK`, not `MODE_AUTO` — that fallback is deliberate and
   shouldn't change, but `"AUTO"` itself needs a real app-side entry, not just fallback handling.
2. **`HEALTH_CHAR_UUID`'s comma-payload grew from 5 fields to 11** (§6): `bloodType,emergencyContact,
   contactName,allergies,age` unchanged (fields 1-5), then six new optional trailing fields -
   `conditions,medications,secondaryContactName,secondaryContact,organDonor,notes` (organDonor as
   `"1"`/`"true"` or `"0"`/anything else). Fully backward-compatible (a 5-field payload still parses
   fine, trailing fields just keep their firmware-side defaults) - but the on-device Medical ID
   extended view (§6) has UI for all 11 and currently shows placeholder text for the 6 new ones since
   nothing sends them. This is the single most user-visible app-side gap: a guardian filling out a
   fuller Medical ID in the app currently has no fields to fill in the new data into.
3. **No protocol changes for messages/replies** - `Message` gained internal-only `kind`/`phone` fields
   (§7) for the on-device Messages UI (badges showing "SMS \<number\>" vs "ALERT" vs "You"), but these
   are derived firmware-side from *which channel* a message arrived on (`MessageCharacteristic` BLE
   push vs. the gateway's SMS relay) - nothing new for the app to send. `MESSAGE_CHAR_UUID`/
   `REPLY_CHAR_UUID` directionality and payload format are unchanged.
4. **`ALERT_CHAR_UUID`'s notify payload is unchanged** (`"FALL_DETECTED"` string) - the SOS/Fall
   lock-in/retry rework (§8, §9) changed *when* and *how reliably* this fires, never its format.

### Settings now available on-device that may overlap the app's own settings UI
Added across §4-§6, all persisted via NVS and independently reachable via BLE too: fall sensitivity,
auto-call, parental controls, SMS fallback, Do Not Disturb + quiet hours, SMS allowlist clear,
medication reminder time (Elderly), check-in interval (Helmet), display contrast, flip screen, boot
chime, notification chime, LEDs enabled, Home-screen ticker toggle. Worth a pass to make sure the app's
own settings screens read/write the same underlying fields rather than drifting out of sync with what's
changeable on-device now. Note: **"Skin" was added then removed again** (§6 then §9) - if the app ever
grew a control for it, it should come back out; the setting no longer exists at all.

### Guardian-locked modes (Elderly/Kids/Pet/Helmet) - app UX consideration
`isModeGuardianLocked()` (§4) hides Mode-switching and the entire Safety settings category from the
on-device menu for these four modes - deliberately, so the wearer can't quietly weaken their own safety
config. Those settings remain fully controllable via the app/BLE, as before. If the app's UI doesn't
already make clear that these are guardian-managed-only for those modes, that'd be a good app-side UX
note to add, though not a functional requirement.

### One open product question, not resolved this round
The BLE audit (§8) flagged `CMD_FIND` (`WeatherCallbacks::onWrite`, tag `"CMD_FIND"`) as ambiguous: it
puts the device on the full "EMERGENCY SOS" screen with siren, but never actually arms a real alert
dispatch (no SMS goes out). If the app's "find my device" feature (if that's what sends this) is meant
to be a lower-key "ring the device" ping, showing the full alarming SOS screen for it is a UX mismatch
worth reconsidering on the firmware side. If it's meant to be a real remote-triggered emergency alert,
firmware needs a follow-up fix to actually dispatch one. Whoever picks up the app pass should check
what the app's intent actually is here and flag it back for a firmware fix if needed.

---

## 1. What shipped, and its verification status

Everything below **compiles clean**. None of it has been confirmed on real hardware yet — that's the
immediate next step for whoever picks this up.

### Stability / bug fixes (the 5 reported symptoms)
- **"Device goes unresponsive after a while"**: added a hardware watchdog (`esp_task_wdt`, 8s timeout,
  fed as `loop()`'s very first statement) as a backstop; removed the blocking `delay(500)` BLE-
  reconnect stall; made every chime that used to run `tone()+delay()` inside a **BLE callback task**
  non-blocking (`ServerCallbacks::onConnect/onDisconnect`, `applyMode()`, `MessageCallbacks`, etc. —
  see the new chime sequencer in "10. SOUND FUNCTIONS"); moved the Pet-mode "virtual leash" gateway
  HTTP alert out of `onDisconnect` (BLE task) into a flag serviced from `loop()`; added I2C bus
  recovery (`recoverI2CBus()`) after 5 consecutive sensor-read failures, on top of the existing
  `Wire.setTimeout(1000)`. **Flagged, not fixed** (needs a real hardware check): whether the OLED's
  separate `u8g2` HW-I2C driver shares the same wedge-recovery benefit as the `Wire`-based MPU6050
  path — deliberately wedge the bus and confirm the display keeps working. The 8s watchdog timeout
  is a starting point, not validated against worst-case legitimate stalls.
- **Button reliability**: added real software debounce (25ms stability window) as defense-in-depth.
  **This will NOT fix a wiring issue** — confirmed the pin (`PIN_BTN`=GPIO3) is correctly
  `INPUT_PULLUP`, not floating, not a strapping pin. If presses still register inconsistently after
  flashing this, that confirms a breadboard/GND wiring problem, not firmware — check that connection
  with a multimeter. Separately: `PIN_LDR` (ambient light sensor) = **GPIO2, which IS an ESP32-C3
  strapping pin** — if its voltage-divider's resting state reads as logic-low at boot, that's a
  plausible source of intermittent boot glitches that could get misattributed to "the button." Worth
  checking, and worth moving off GPIO2 on a future board revision.
- **"Auto sleep still has issues"**: literal device/OLED sleep was already fully removed (confirmed
  dead code, unchanged this pass). The actual mechanism was Shady's cosmetic `SHADY_SLEEPY` mood going
  nearly motionless/permanently-closed-eyed after ~135s of stillness, reading as "the device sleeping."
  Replaced with a rotating idle cycle (`shadyIdleMoodForTick()`): stretch beat → ambient-idle beat →
  resting, on a ~2.5 min rotation, so a resting device stays visibly alive. Also fixed a real latent
  bug found along the way: Wrist mode's "Sleep: N min" stat was gated on the confirmed-dead
  `SCREEN_SLEEP` state and could never move off 0 — now keyed off the same stillness signal.
- **NeoPixel "doesn't follow the set pattern / goes dark"**: root-caused as fully structural (not
  flaky) — the pattern-render function only ever ran from the on-device Light Mode menu screen or
  once per BLE write; every other loop iteration on every other screen, an unconditional block
  overwrote the strip and fell through to `strip.clear()` in bright conditions. Fixed: the selected
  pattern is now the real persistent baseline rendered every loop iteration on every screen, with the
  existing pet-lost/braking/Kids-shimmer/dark-taillight overlays unchanged and still layered on top
  only while actively triggered. New `ledsEnabled` toggle (Settings > Lights) added without touching
  the existing numeric BLE wire protocol for LED pattern selection.
- **Buzzer "more lively"**: built a non-blocking chime sequencer (`ChimeStep`/`playChime()`/
  `serviceChime()`) and redesigned every chime into a short multi-note sequence instead of a flat
  tone — boot, click, screen-change, mode-change, reply-select/sent, BLE connect/disconnect
  (distinct from a Pet-mode "lost" chirp), incoming message, health/settings/reply/ack ticks, med
  reminder vs. helmet check-in (now audibly distinct), plus new Settings-menu navigate/enter/back
  chimes. `SCREEN_SOS`/`SCREEN_FALL`'s sirens were already correctly non-blocking and are untouched.

### New settings menu + navigation overhaul
- **Gesture vocabulary, system-wide**: **double-click** now opens a real hierarchical Settings menu
  from any screen (or backs out one level / cancels an in-progress edit if already inside it);
  **triple-click** is now a global "jump to Home" from anywhere (Settings at any depth, RGB menu, Mode
  Select, reply mode); **long-press-from-Home for SOS is completely untouched**. Mode Select and the
  RGB Light Mode menu keep their own existing internal single-click/hold interaction unchanged — they're
  just reached through Settings now instead of their old direct double/triple-click entry gestures.
- **Settings menu** (`SCREEN_SETTINGS_MENU`, table-driven via `MenuItem` arrays, one generic renderer):
  **Mode** (→ Mode Select), **Lights** (pattern picker, LEDs on/off — hidden for Helmet like before),
  **Display** (Contrast: Dim/Normal/Bright presets, Flip Screen, **Skin**: Clean/Retro — a chrome-only
  border+corner-tick treatment, no new fonts/bitmaps), **Sound** (chime volume, SOS volume, boot chime
  toggle), **Notifications** (message-chime toggle, a new mode-agnostic **Do Not Disturb** generalized
  from the old Kids-only quiet hours, SMS allowlist count + clear), **Safety** (fall sensitivity,
  auto-call/parental-controls/SMS-fallback — all newly on-device, previously BLE-only), **Reminders**
  (medication time — Elderly only; check-in interval — Helmet only, also newly on-device), **Device
  Info** (name/battery/firmware version, read-only).
- **Persistence (new)**: every setting above now survives a power-cycle via NVS (`Preferences`),
  debounced (~2s after the last change) and written from a single place in `loop()` so BLE-driven and
  on-device-menu-driven changes can't race each other. Deliberately **not** persisted: location/
  weather/gateway state, message history, and "today" tallies (ride distance, sleep/active minutes,
  impact count) — there's no RTC/day-boundary concept yet, so persisting those would just mean they
  never really reset. A fresh flash behaves identically to before this pass (all defaults preserved).

### 8th "Auto" mode
- Added as the new **default** mode on first boot — reuses the existing generic/default Home UI
  (same as Backpack), per the scoped-down request. `modeFromName()`'s fallback for unrecognized
  names stays `MODE_BACKPACK` (not `MODE_AUTO`), so an old app build sending garbage never lands on
  the placeholder mode. A comment marks where real automatic mode-detection logic belongs later — no
  stub function, out of scope this pass. **Flagged dependency**: the app doesn't know the string
  "AUTO" yet (Android pass is separate) — firmware will send `ACK:MODE:AUTO` to it; should degrade
  gracefully but hasn't been confirmed against the actual app code this session.

### Per-mode UI differentiation
Small, budget-conscious additions per persona: Elderly gets larger health-screen text; Kids gets a
4th rotating Home ticker message; Bike's Ride Stats gets a derived avg-speed stat + a lean-angle
warning; Pet's Activity screen shows "Last seen Nm ago"; Helmet's Impact Log shows the check-in
interval; Wrist got its own explicit (currently baseline-identical) `getModeProfile()` case plus a
real motion-integrated activity tally on the Vitals screen. Backpack/Auto deliberately left as the
shared generic baseline. Also fixed a real pre-existing display bug: several screens hardcoded a
stale pagination-dots total against a fixed 5-screen assumption; now derived live from
`buildModeScreenCycle()`.

### Shady
New optional, occasional additions (never on SOS/Fall screens, kept visually serious there): a tiny
waving hand after BLE connect, small kicking legs during the existing "takeover" bounce animation, a
tiny mode-flavored accessory doodle (helmet brim, backpack strap, collar, wristband, bow, cyclist-cap
brim, pillbox+cross) on the Home screen and Mode Select preview, plus a "curious head-tilt" bias
triggered on new-message arrival. All reuse existing primitive draws — no new fonts/bitmaps.

---

## 2. Deferred / explicitly not done this pass

- **Heap-hygiene `String`→`char[]` cleanup** (per-frame `String` churn in several draw functions, and
  the gateway JSON-parsing helpers) — this was the lowest-priority, "do last if time permits" item in
  the plan. Deliberately **not done**: the refactor would ripple into functions with no hardware path
  to verify in this environment (the gateway-polling code specifically needs a live COM16 board), and
  the benefit (reduced fragmentation risk) is plausible-but-unconfirmed research, not a proven bug —
  not worth the regression risk on a safety device without being able to test it. Worth a dedicated,
  hardware-verified pass later if long-uptime instability is still observed after this pass's other
  fixes land.
- Pagination dots were **not** added to the 6 per-mode priority screens (Ride Stats/Activity/Vitals/
  Impact Log/Safe Zone/Medication) — they didn't have them before either; not a regression, just
  left as a minor "nice to have" given time.
- Settings-menu value editing uses immediate-apply-per-click (matching the existing RGB pattern
  picker's convention) rather than snapshot/revert-on-cancel — double-click while editing exits the
  edit state without reverting to the pre-edit value. Documented as a deliberate scope decision in
  code, not an oversight.

## 3. Suggested next steps

1. Flash COM15 and go through the verification checklist in the plan file's "Verification" section:
   extended-runtime freeze check, button consistency (a wiring-issue confirmation either way), LED
   pattern persistence across screens, Shady's idle liveliness, double-click Settings / triple-click
   Home from various depths, settings surviving a power-cycle, each mode's UI reading as distinct,
   Auto being the default on first boot.
2. Specifically stress-test the two flagged hardware-uncertain items: deliberately wedge the I2C bus
   and watch whether the OLED survives; watch Serial for any unexpected watchdog reboots and adjust
   the 8s timeout if legitimate stalls trip it.
3. Once hardware-confirmed, this is a good commit point (nothing has been committed yet this session).
4. Android app pass next (per the original plan) — needs to learn about `MODE_AUTO` at minimum, plus
   whatever UI hooks make sense for the new on-device settings now duplicating some app-side controls.

## 4. Session update — follow-up bug-fix pass (2026-09-05)

The section 1 pass above got flashed and used; the user came back with 6 more items. All are firmware
(`SafeShadev21.ino`), compiled clean and flashed to COM15 (hash-verified), with a ~15s boot check
showing clean WiFi/gateway polling and no panics/watchdog resets. **Still needs the user's own hands-on
retest** — none of this has been button-tested on real hardware yet.

- **Guardian-locked settings**: added `isModeGuardianLocked()` (Elderly/Kids/Pet/Helmet). In those
  modes, Mode-switching, the whole Safety submenu, and "Clear Allowlist" are now hidden from the
  on-device Settings menu entirely (still reachable via the app/BLE) — `isMenuItemVisible()`.
- **NeoPixel pattern not surviving reboot — root-caused**: cycling the pattern via the on-device RGB
  Light Mode screen's single-click never set `settingsDirty`, so only a BLE-driven change was ever
  persisted. Fixed. Also found and fixed a related latent bug while in there: `SCREEN_RGB_MENU` was
  missing from the hold-time if/else chain, so holding the button to "confirm" a pattern (per that
  screen's own on-screen hint) fell through into arming SOS instead — holding now just exits cleanly.
- **Status bar overhaul**: BRIGHT/DARK text → sun/moon icon next to Bluetooth; temperature dropped
  from the bar entirely (still on the Weather screen); gateway signal bars now show a "no network"
  glyph instead of vanishing when stale; battery icon redrawn smaller/rounder; the bottom-line
  scanning-pixel "loading bar" (`drawAnimatedPulse()`) removed entirely; footer ticker is now center-
  aligned and rotates in the button-gesture hints too; new Settings > Display > "Home Tips" toggle to
  turn the ticker off completely (persisted, default on).
- **Settings menu**: the "TAP/HOLD/2x:..." hint line removed from every Settings screen per request.
- **Shady**: fixed a real rendering bug where the Backpack/Auto shoulder-strap doodle and the Pet
  collar doodle both drew a line almost exactly through the left/both eyes ("weird line behind the
  eye") — repositioned both clear of the face. Takeover now disables itself while Shady is in the
  deep-rest phase of the idle cycle (and ends early if that phase starts mid-takeover), resumes with a
  one-time "just woke up" groggy-blink expression, and any button click on Home now dismisses an
  active takeover immediately instead of it running out its full ~3.5s. Idle (non-takeover) mood
  gained two new personality quirks (sideways glance, content twinkle) on top of the existing four.
  Takeover itself now has a second "silent" act (~1/3 of the time): a wordless little dance instead of
  the speech-bubble bounce. **Scope note**: the user asked for takeover to get "a lot of work" —
  this pass made it meaningfully better (variety, non-disruptive, no longer fights the rest cycle) but
  is not a full redesign; more acts/personality there would be a reasonable next increment.
- **Rotary encoder integrated** (follow-up within the same session, after the user wired one up):
  `SW` reuses `PIN_BTN`/D1 (the old tact button, now unplugged), `CLK`→D7, `DT`→D8 (D9 avoided, it's
  a boot-strapping pin). Full navigation redesign, not just wiring support:
  - **Rotate** now owns every "move/cycle" gesture that single-click used to (`handleEncoderRotation()`,
    called from a new CLK-falling-edge poll in `loop()`): next/prev screen on content screens, menu
    selection/value-adjust in Settings, pattern in the RGB menu, mode preview in Mode Select, reply
    selection in reply mode.
  - **Single press (SW)** now means confirm/select - it took over everything that used to require an
    800ms hold (enter reply mode, send a reply, confirm a mode, activate/confirm in Settings, confirm
    an RGB pattern). Double-click (open/back in Settings) and triple-click (jump Home) are unchanged.
  - **Long-press is now reserved solely for arming SOS** - the exact same condition/timing as before,
    untouched, since that's the one gesture called out as safety-critical/must-not-change.
  - Settings menu now shows **5 rows instead of 4**, using the space the removed hint line freed up.
  - **Not yet button/rotation-tested on real hardware** - compiled clean and reflashed, boots fine, but
    the actual feel of rotation direction (a specific module might read backwards - swap CLK/DT wires
    if so) and the new single-press-to-confirm timing need the user's hands-on check.
- **Shady takeover — full redesign**: replaced the single bounce-to-center animation with 5 distinct
  `ShadyAct` vignettes, randomly chosen each time (never repeats), each its own little "world" rather
  than a mood swap on the same movement: `ACT_SPEECH` (the original bubble+bounce), `ACT_DANCE` (silent,
  kicking legs + wave + a floating music note, no bubble), `ACT_PEEK` (shy corner cameo that slides out
  from behind a small "wall" and ducks back), `ACT_CHASE` (chases a bouncing prop across the screen),
  `ACT_TWIN` (a small phase-offset "buddy" Shady dancing alongside). All still fully replace the Home
  screen for their duration (a deliberate simplification - true non-overlapping "peek while clock stays
  visible" would need a bigger structural change) and are dismissed instantly by any click, same as
  before.

## 5. Session update 2 — status bar, encoder decoder fix, SOS redesign

- **Status bar**: removed the day/night sun/moon icon and all ambient-light indication entirely (was
  briefly added last update, now gone per feedback). `icon_sun` (kept - "looks like a satellite") now
  sits evenly spaced between the network icon and the battery icon, shown only while the gateway link
  is fresh, signifying "GPS is working." `icon_moon` deleted outright (nothing else used it).
- **Rotary encoder decoder — replaced, real bug fix**: the previous version triggered off a single CLK
  falling edge and sampled DT at that instant - exactly the failure mode the user hit ("clockwise
  sort-of works, counter-clockwise is riddled with bugs, sometimes forward sometimes backward"), since
  one edge+sample can't tell real rotation from contact bounce. Replaced with a proper quadrature
  state machine (`ENC_TRANSITION[]`, `encAccum`, resolved only when the encoder returns to its rest
  state) that's antisymmetric by construction, so any bounce that dips into an adjacent state and
  springs back always sums to exactly zero - it can't fire a spurious step in either direction, and
  it no longer depends on catching one precise instant regardless of what else `loop()` is doing.
  **`ENC_DIRECTION` (a `+1`/`-1` constant right above the table) may need flipping** if the mapped
  direction still doesn't feel right on the user's specific module - impossible to predict from
  firmware alone, noted in-code.
- **Ticker "1x" → drawn circle**: the literal "1x:next" text was already stale (rotation, not
  single-click, cycles screens now) - replaced with an actual drawn circle glyph + "Next  2x:Settings".
- **SOS — fully redesigned as one continuous hold gesture**: was previously a two-stage model (a short
  mode-dependent hold to *enter* the SOS screen, then an independent 5s countdown that kept running
  even if the button was released - cancelling required a separate tap or a fresh 2s hold). Removed
  `ModeProfile.sosHoldMs` entirely; SOS is now armed by any hold past `SOS_ARM_THRESHOLD_MS` (400ms,
  same boundary already used to classify clicks vs. holds), with the on-screen 5s (`SOS_HOLD_MS`)
  countdown measured from the actual press-down. **Releasing the button at any point before the
  countdown completes cancels it automatically** - no extra gesture; holding continuously through the
  full 5s fires the real alert exactly once. Gated with a new `sosArmedByHold` flag so the separate
  BLE `CMD_FIND` "locate my device" trigger (unrelated to any physical hold) isn't affected by the
  auto-cancel-on-release logic.
- **SOS screen — visually redesigned, Shady removed**: the old version alternated every 300ms between
  two entirely different layouts (different fonts/positions) plus a small Shady crammed in the corner
  and a stray left-aligned status line while everything else was centered - that inconsistency was the
  actual "looks terrible" problem. New version is one fixed, centered layout (blinking inverted banner
  for urgency instead of swapping layouts, a big centered countdown number, "Release to cancel"
  copy matching the new hold model, centered post-alert SMS-status text) with no Shady at all.
- **Not yet hardware-tested this update** - compiled clean, reflashed, boots fine (WiFi/gateway polling
  observed with no crashes), but the actual encoder feel, SOS hold timing, and the new status-bar
  icon spacing all need the user's hands-on check.

## 6. Session update 3 — encoder rewrite #2, Fall/Medical ID/Mode/Light redesigns, cycle reorder

- **Rotary encoder decoder replaced again - user reported it got WORSE, not better**: the update-2
  quadrature state machine only resolved accumulated steps once the pins returned to raw value 3
  (both HIGH), assuming that's this encoder's physical rest state - an assumption that didn't hold,
  so it barely resolved anything or resolved at arbitrary moments. Root-caused and fixed with two
  real changes: (1) resolution now fires purely on accumulated magnitude reaching one full detent
  (`ENC_STEPS_PER_DETENT` = 4), never assuming which raw pin-state is "rest" - still bounce-proof via
  the same antisymmetric table property; (2) CLK/DT are now read via `attachInterrupt(..., CHANGE)`
  on both pins (`onEncoderChange()`), not polled once per `loop()` iteration - a busy loop() (I2C/
  BLE/OLED work) could previously miss fast transitions outright. Single-core XIAO ESP32C3, so the
  shared `volatile int encAccum` needs no locking. **Still genuinely unverified on real hardware** -
  this is the third attempt at this specific piece; please test thoroughly and report back exactly
  what's still wrong if anything, ideally which direction/speed of turn misbehaves.
- **Fall Detected screen - full redesign, Shady removed**: same "one consistent centered layout, no
  Shady" fix as the SOS screen last update - the old version had Shady in a corner, a stray left-
  aligned SMS-status readout, and a blinking triangle tall enough to visually overlap the "FALL" text.
- **Medical ID - "ICE:" label removed, extended scrollable view added**: the compact view is now a
  4-line summary (Blood/Age, Allergy, Contact, Phone-no-longer-labeled-"ICE") plus a "Tap: more info"
  line; a press toggles into a new extended view (`drawHealthExtendedView()`) - a scrollable list,
  rotation-driven, same windowed-list visual language as the Settings menu - covering 11 fields total
  (added Medications, Conditions, a second contact, Organ Donor, Notes to `HealthData`, all optional/
  backward-compatible additions to `HEALTH_CHAR_UUID`'s comma-parser). The Android app doesn't send
  these new fields yet - a separate future pass - so they'll show their placeholder defaults until it
  does.
- **Mode Select + Light Mode menu - visual pass**: Light Mode dropped the stale "<"/">" tap-arrows and
  "TAP: change 3x: exit" hint (neither gesture applies anymore); Mode Select's name now sits in the
  same rounded box style as Light Mode's pattern name for visual consistency between the two, and its
  stale "TAP:cycle HOLD:confirm" hint is fixed. Also fixed the same class of stale hint on the Quick
  Reply screen ("TAP:Next HOLD:Send" -> "Press: send") and the Messages screen ("Hold to reply" ->
  "Press: reply") - all left over from before the rotary-encoder gesture redesign, missed at the time.
- **Screen cycle reorder**: Backpack/Auto/Kids's cycle is now Home > Medical ID > Messages > Location
  > Weather (Weather was first, now last; Message/Location also swapped to match "messages then
  location"). Bike/Pet/Helmet/Wrist untouched (no Weather in their cycles, out of scope for this ask).
- **Shady takeover no longer "follows" navigation**: a real gap in last update's "any click dismisses
  it" fix - that only covered button clicks (2x/3x/hold), not rotation, which is now how the wearer
  actually leaves Home most of the time. Rotating away mid-takeover now cancels it immediately too, so
  it can't resume when rotating back within its remaining ~2-4s window.
- **Takeover animations moved down** ~6-10px per act (there was unused space at the bottom of that
  screen) - all bounds-checked against the 64px display height so nothing (including kicking legs)
  clips off-screen.
- **Home screen clock nudged down** a few px, but only in modes where Shady actually shares that row
  (`activeMode != MODE_HELMET`) - Helmet's clock position is unchanged since there's no Shady there to
  balance against.

## 7. Session update 4 — Fall alert bug fix, Messages redesign, Medical ID trim, remaining polish

- **Real bug found and fixed: Fall detection fired its SMS/BLE alert unconditionally, immediately,
  with zero confirmation window** - `triggerGatewayAlert()` (and the BLE `FALL_DETECTED` notify) used
  to run the instant an impact was detected, before the wearer could do anything about it; the "Press
  to dismiss" text only ever closed the screen *after* the alert had already gone out. This is almost
  certainly what the user actually saw when they thought a dismissed SOS/Fall "still sent" - the pure
  SOS-hold code path was re-verified line by line and is structurally sound (release cancels, only a
  full continuous 5s hold fires); holding the button for 5 seconds involves enough arm/wrist motion to
  plausibly also trip fall detection concurrently, which had no confirmation window at all until now.
- **Fall now gets the same accidental-trigger lock-in as SOS** (`FALL_LOCKIN_MS` = 5000ms): the alert
  is deferred until the lock-in elapses with the Fall screen still showing. Dismissal is a **double-
  click** for every mode except Helmet, which keeps its existing deliberate two-step single-press
  "confirm OK" flow (a real fall can easily cause an accidental single tap on impact - double-click,
  or Helmet's 2-step, both require more deliberate intent than a passive single press).
- **Messages screen - full redesign**: real chat-style two-line "cards" (a kind/sender badge line -
  "SMS <number>", "ALERT", or "You" - then the message text), left-aligned for incoming, right-aligned
  for sent replies. `Message` gained `kind` (`MSG_KIND_ALERT`/`MSG_KIND_SMS`/`MSG_KIND_REPLY`) and
  `phone` fields; the SMS-relay path (`pollGatewayMessages()`) now stores the real phone number as
  structured data instead of jamming a "[last-4-digits]" tag into the text. Rotation scrolls the full
  message history (previously only the 3 newest were ever visible, with no way to see older ones);
  scroll position resets to newest whenever a message arrives or is sent.
- **Medical ID compact view trimmed to 3 lines** (Blood+Age, Contact, Phone - Allergy dropped, still in
  the extended view) with the original comfortable 12px row spacing restored, so "Tap: more info" has
  real room again instead of being crammed under a 4-line layout.
- **Encoder direction flipped** (`ENC_DIRECTION` now -1) - was inverted from what felt natural.
- **Weather screen**: "SYNCING..." placeholder no longer prints as if it were real condition text at
  the bottom - a small 3-dot buffering animation in the header (right-aligned) shows instead while
  waiting on the first real payload; the bottom condition line is blank until real data arrives.
- **Location screen**: "(0 sats)" no longer shows - the satellite count only prints when it's actually
  >0 (a real value from the gateway), otherwise just "Onboard GPS" alone.
- **Item 8 - comprehensive bug-fix pass**: 6 parallel read-only audit agents dispatched across
  sensor/physics/fall/SOS, BLE protocol/callbacks, button/encoder/navigation state, on-device UI
  rendering/alignment, Shady/animations, and settings/persistence/per-mode logic. Findings will be
  consolidated and fixed in a follow-up update to this doc once all agents report back.
- **Not yet hardware-tested this update** - compiled clean, reflashed, boots fine, but Fall's new
  lock-in/double-click flow, the Messages redesign, the encoder direction flip, and the Weather/GPS
  text changes all need the user's hands-on check.

## 8. Session update 5 — 6-agent audit findings fixed, card-style UI pass, Fall redesign

6 parallel read-only audit agents (sensor/physics/Fall/SOS, BLE protocol, button/encoder navigation,
on-device UI rendering, Shady/animations, settings/persistence) each did a deep pass over the whole
file. All findings were consolidated and fixed directly (not left as a report) - see below. This is
the fourth pass at the encoder specifically (§6 and §7's fixes weren't the whole story) and the third
real bug found in the Fall/SOS interaction, so **please test especially thoroughly this time**.

### Safety-critical fixes (Fall/SOS)
- **`SCREEN_FALL` was missing from the SOS-arm hold-exclusion list** (only `SCREEN_SOS`/Mode Select/
  Settings/RGB menu were excluded) - a reflexive button grab during a real fall's 5s lock-in (very
  plausible right after an impact) could silently hijack the screen to SOS and, on release before a
  full SOS hold, abandon the fall alert entirely with nothing ever sent. This is almost certainly the
  root cause of the "SOS still sent when dismissed" report from last update.
- **Four other call sites unconditionally overwrote `currentScreen`** (BLE `CMD_FIND`, an incoming
  Guardian BLE message, a relayed SMS, the medication-reminder tick) with no guard against interrupting
  an in-progress Fall lock-in or SOS hold - all four now check `currentScreen != SCREEN_SOS &&
  currentScreen != SCREEN_FALL` before jumping (the underlying event - message received, reminder due -
  still gets recorded/buzzed either way, only the screen jump is suppressed).
- **Helmet's concussion-confirm timeout fail-safe had no `currentScreen == SCREEN_FALL` guard** - could
  force-switch away from an unrelated, active SOS screen 10s later.
- **Helmet's two confirm taps also incremented the global click counter** (they act on press-down with
  an early return, but their release still counted as a normal click) - two quick "I'm OK" taps could
  land as clickCount==2 and bounce straight into the Settings menu right after confirming a head impact.
  Fixed with the same `btnWasHeld` suppression idiom already used for genuine holds.
- **A pending Fall alert could be silently discarded via triple-click** ("jump to Home" had no
  `SCREEN_FALL` exclusion, unlike SOS which is protected by its own earlier branch).
- **Fall alert now fully matches SOS's "one message, one send" model**: the Helmet confirm-pending
  flags now clear the instant the alert actually fires, so the wearer is never left staring at "Confirm
  OK? Press again" while the real SMS already silently went out behind it (Helmet's 10s confirm timeout
  is longer than the 5s lock-in).
- **BLE-driven mode switch (`EXT "MODE:"`) never reset `currentScreen`** - unlike the on-device Mode
  Select path, which already force-navigates Home - leaving the wearer on a mode-specific screen the
  new mode's own cycle doesn't contain, silently deadening the rotate gesture there. Now matches.
- **Wrist's tallies (`sleepSecondsToday`/`wristActiveSecondsToday`) were never reset on mode switch**,
  unlike every sibling tally - fixed.
- **Medication reminder fired exactly once, ever** - the re-fire guard latched permanently (no RTC, the
  clock wraps to the same minute-of-day every "day," and the guard was never cleared afterward). Fixed
  to clear once the clock moves off the due minute.
- **`fallSensitivity`'s BLE write path had no bounds clamp**, unlike the on-device menu editor - fixed.
- **Helmet's check-in interval wasn't guardian-locked** despite escalating to a real SMS alert on a
  missed check-in - exactly the kind of setting the guardian lock exists to protect. Now hidden on-
  device for Helmet, guardian-managed only, same as the other Safety-category settings.

### Encoder (fourth pass)
- **`encAccum`'s drain loop in `loop()` had a non-atomic read-modify-write race with the ISR** -
  `encAccum -= ENC_STEPS_PER_DETENT` is two separate operations; if the ISR fired in between, its own
  contribution got silently clobbered, most likely during a fast spin (exactly when the ISR fires
  most). Fixed with an atomic snapshot-and-clear (`noInterrupts()`/`interrupts()` around just the two-
  instruction critical section, not the rotation-handling calls themselves).

### BLE
- **`ReplyCallbacks::onWrite` ran a blocking HTTP call (`triggerGatewayReply()`, up to ~2.5s) directly
  on the BLE stack's own callback task** - the exact anti-pattern the codebase otherwise guards against
  everywhere else (e.g. Pet's "virtual leash" alert). Deferred to `loop()` via a pending-flag, matching
  the existing convention.
- `applyMode()`'s hand-built ack notify now reuses `sendAck()` instead of duplicating it.

### UI/UX
- **`healthExtendedView` could leak true across an accidental SOS detour** from the Health screen -
  fixed at both SOS exit paths.
- **Unbounded strings that could overflow the screen edge**: Medical ID's contact name/phone, GPS
  locality, the Settings > Device Info name, and the Home ticker's device-name slot (the last one had
  the exact same unsigned-coordinate-wrap hazard already fixed once for Shady's speech bubble) - all
  now length-capped; `deviceDisplayName` is also capped at the BLE write site itself (24 chars).
- **Medical ID header redesigned**: dropped the hline + EKG-sweep animation (read as clutter), heart
  icon nudged for vertical centering with the heading, "Tap: more info" now centered to the card (not
  the full screen) with a footer divider line above it - all via new shared `drawCardHeader()`/
  `drawCardFooterLine()`/`drawCenteredTextInCard()` helpers.
- **Every other non-Settings info screen now uses the same card style** (outer frame, vertically-
  centered icon+heading, no header divider): Weather, Location, Messages, and all six per-mode
  priority screens (Ride Stats, Activity, Vitals, Impact Log, Safe Zone, Medication) - the latter six
  previously had no header icon, no outer frame, no pagination dots, and no screen-wipe transition
  despite being full members of the same screen-cycle as everything else. **Home is deliberately
  unchanged** - its own clock+Shady layout stays exactly as-is, per explicit instruction.
- **Messages icon redesigned** - a filled rounded speech-bubble-with-tail instead of the old flat
  envelope outline, matching the solid-fill weight of the other status icons.
- **Fall screen - complete redesign**, now sharing the SOS screen's exact visual language (the same
  blinking inverted banner, big centered content, single-line bottom hint) instead of its own distinct,
  less-polished layout.
- **Settings menu back-navigation now restores the previous scroll position** instead of always
  resetting to the first item - a per-depth memory array saved just before descending into a category.
- Two remaining stale "Tap"/gesture hints fixed (SOS's post-alert "Press to dismiss"; "Tap: more info"
  on Medical ID was deliberately kept as-is, per direct instruction).
- A handful of stale/misleading comments fixed (Impact Log's check-in-interval note, an
  activeSecondsToday reset claim, a Helmet-brim geometry note, `sendAck()`'s coverage claim).
- **Two Shady geometry bugs** (same bug class as the previously-fixed backpack-strap/collar issues):
  the Wrist wristband and the Elderly pillbox both crossed through an eye in the Mode Select preview
  (r=9) - neither reachable from Home, which is why they weren't caught before. Both repositioned clear
  of the face.
- `displayContrast`'s default (200) didn't match its own "Normal preset" comment/intent -
  `contrastPresetIndexFromRaw()` actually rounded it to "Bright". Fixed to 150, the literal Normal value.
- `SETTING_MED_MINUTE` now shows "--" when unset, matching its sibling `SETTING_MED_HOUR` (used to
  print a raw "-1").

### Explicitly deferred (noted, not fixed, given the scope already covered this pass)
- Settings menu label/value column spacing for long labels (cosmetic, low severity).
- `drawHealthExtendedView()`'s selection box is 120px vs Settings menu's 112px (no current visible
  collision, just an unexplained inconsistency).
- Incoming message text truncation (20 chars, no width-check) vs. every other cap in the file (16-18
  chars, several with an explicit width check) - low severity, same truncation *style* just a longer cap.
- `drawActivityScreen()`'s "Last seen Nm ago" is unbounded until `millis()` wraps (~71,582 minutes) -
  extreme edge case.
- `BLEDevice::init()` hardcodes the advertised BLE name; never updated when `deviceDisplayName` changes
  - cosmetic, the app matches by service UUID not name.
- **Not yet hardware-tested** - compiled clean, reflashed, boots fine (WiFi/gateway polling observed,
  no crashes/watchdog reboots), but this is the single largest batch of changes in this file's history
  and needs a genuinely thorough hands-on pass, not a quick glance.

## 9. Session update 6 — remaining low-severity findings, Skin removed, alert retry + honest network state

- **All previously-deferred low-severity audit findings fixed**: Settings menu label/value columns now
  measure the value first and truncate the label to whatever room is actually left (was two independent
  measurements with no shared-space check); `drawHealthExtendedView()`'s selection box width aligned to
  Settings menu's (was 120px vs 112px, unexplained); Messages' incoming/outgoing text now truncates by
  measured width instead of a flat 20-char cap (could still overflow for a wide 20-char string);
  Activity's "Last seen" switches to hours past 60 minutes (was unbounded until `millis()` wraps at
  ~49.7 days); `BLEDevice::init()` now uses `deviceDisplayName` instead of a hardcoded literal, so a
  renamed device advertises correctly from the next boot.
- **"Skin" removed from Settings > Display** per product feedback. `DisplaySkin`/`SKIN_RETRO`/
  `drawScreenChrome()` deliberately left in place as inert infrastructure - `displaySkin` is now
  permanently `SKIN_CLEAN`, and `drawScreenChrome()` already no-ops for that value, so the 20+ existing
  call sites needed no changes.
- **SOS/Fall alert dispatch - fast retry loop instead of one fire-and-forget attempt**: the mainboard
  only ever tried `triggerGatewayAlert()` once, exactly at the moment the lock-in/hold completed - if
  WiFi wasn't connected at that precise instant, the alert was silently abandoned until some unrelated
  later event (stillness escalation, a missed check-in) happened to retry it, which could be many
  seconds later or never. `beginAlertDispatch()`/`serviceAlertRetry()` now retry every 800ms, up to 8
  times, the instant the lock-in/hold completes - this is almost certainly the real explanation for
  "SOS SMS takes longer than a regular message" (regular replies were never the issue; the alert path's
  own retry-lessness was).
- **Honest on-screen state while that's happening**: SOS/Fall's "sent" phase now shows a real
  "Connecting... / Sending your alert" animation (3-dot buffering, same visual language as the Weather
  screen's) while a dispatch is in flight, and an explicit "Alert Not Sent / No network connection"
  error if all retries are exhausted - both with the same dismiss hint as before - instead of always
  assuming success and showing "Help is on the way" the instant the countdown ended.
- **Messages screen polish**: "No messages" empty state and the "Press: reply" footer both now center
  to the 120px card (were centered to the full 128px screen, drifting right of the card); the footer
  also gained a divider line above it, matching Medical ID's footer treatment; the up/down scroll-arrow
  indicators were overlapping the vertical screen-cycle pagination dots (same x-column, 118-126) -
  moved to their own column (110-116) and shrunk.
- **Not yet hardware-tested** - compiled clean, reflashed, boots fine. The alert retry/connecting-state
  behavior specifically can't be verified without deliberately testing SOS/Fall with WiFi down, then
  back up - please check both the "network available" and "network unavailable" paths.
