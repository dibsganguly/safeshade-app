# SafeShade — Session Handoff #4

**Written at the end of the v2.2.0 pass, tagged v2.3.0.** Read this first, then
`handoff3.md` §0 and §3–4 for the firmware-facing detail it does not repeat.

The app is a single-module Android app (Kotlin + Compose) paired with an ESP32
wearable over BLE. `CLAUDE.md` describes an older architecture in places — the
app now has a ViewModel layer (`ui/vm/SafeShadeViewModel.kt`) and a repository
layer, not the `mutableStateOf`-in-`SafeShadeApp` design that file describes.
Trust the code.

---

## 0. What the next session is for

In the user's own words, in order:

1. **More UI/UX enhancements, refinements and bug fixes on the Android app.**
2. **Another icon pass.** `docs/Icons` has grown from 35 SVGs to **100**. The
   user's words: *"Most icons now look great, now that you have swapped with my
   custom icons, I don't want the material icons to clash with my custom icon
   set so I have actually added a lot more commonly used and potentially-useful
   icons in docs/Icons."* The goal is to stop Material and custom glyphs
   sitting side by side.
3. **Then** a consistency pass on the **hardware/firmware** to bring it level
   with the app.

### The icon pass, concretely

The registry `ui/icons/SafeShadeIcons.kt` currently holds **35** of the 100.
Regenerate it with **`tools/gen_icons.py`** (checked in for exactly this
reason — it was a throwaway in a temp directory that would not have
survived the session). Run it from the repo root: it reads every SVG in
`docs/Icons` and overwrites `ui/icons/SafeShadeIcons.kt`. It handles per-path
stroke-width overrides, filled paths, `<circle>` to arc conversion and non-24
viewBoxes. Confirmed to run clean against all 100. The SVGs stay the source of
truth; the generated Kotlin is checked in so the build needs no codegen step.

The new 65 change two decisions made in v2.2.0 that were correct then and are
wrong now:

- **`navbar-safety` now exists.** The Safety tab kept Material's `Shield`
  purely because there was no custom equivalent. Swap it; the bottom bar is the
  most-seen mixed-glyph surface in the app.
- **The "chrome stays Material" rule can now be retired.** v2.2.0 explicitly
  scoped the swap to *feature* icons because the set had no back arrow,
  chevron, close, add, search, info or tick. It now has
  `arrow-left-01` / `arrow-right-01` / `arrow-up-01` / `arrow-down-01`,
  `search-01`, `info` / `information-square`, `help-circle` / `help-square`,
  `tick-02`, `square-check-big`, `ban`, `alert-02`, `badge-alert`,
  `play` / `pause` / `stop`, `at-sign`, `star`, `favourite`,
  `thumbs-up` / `thumbs-down`, `bookmark-02`, `all-bookmark`.
  **The back arrow matters most** — it is on every pushed screen, it is the
  single most repeated glyph in the app, and it is currently Material's.
  It lives in `ui/board/ScreenHeader.kt` (both `ScreenHeader` and
  `BackOnlyHeader`), so it is one edit for the whole app.
- There are now **battery** (`full` / `medium-01` / `medium-02` / `low` /
  `warning` / `charging` / `eco-charging`), **signal** (`full-signal` /
  `medium-signal` / `low-signal`, `3-g` / `4-g` / `5-g`), **GPS**
  (`gps-signal-01` / `gps-disconnected` / `gps-no-signal-02`), **network**
  (`wifi-01`, `internet`, `no-internet`) and **cloud/sync**
  (`cloud-loading`, `cloud-saving-done-01/02`, `hard-drive-upload/download`)
  families. These map onto real state the app already tracks — the Board's
  battery and signal readouts, the sync indicator, the offline notice — so this
  is the pass where a *state-varying* icon becomes possible rather than one
  static glyph per concept.

**Do not** reuse one glyph for N distinct things (the `ModePickerScreen`
personas were left on Material for exactly this reason — eight modes, one
`adaptive-mode` glyph). Check whether the 100 now cover them.

`$CLAUDE_JOB_DIR/tmp/icon-map.md` from the v2.2.0 pass documents the previous
mapping and its rules; it is a useful template but its "leave on Material"
column is now largely obsolete.

---

## 1. Working style the user has asked for and confirmed

These are settled. Do not re-litigate them.

- **Commit messages carry the `vX.Y.Z:` prefix** and stay in sync with
  `versionName` in `app/build.gradle.kts`. Messages in this repo are long and
  explain *why*, including what was wrong before and what was not verified.
  Match that register — it is the house style and the user reads them.
- **Parallel subagents are pre-authorised.** "Spawn subagents freely." They
  have repeatedly caught real errors in my own instructions; when one flags a
  mismatch, check the source rather than overruling it. Give each agent a
  **hard file scope** and tell it explicitly not to run any writing `git`
  command — commits are the coordinator's job.
- **Ask before pushing.** The user grants it explicitly each time. (It was
  granted for the v2.2.0/v2.3.0 push; assume nothing beyond that.)
- **Do not commit or revert `docs/SafeShadev21/SafeShadev21.ino`.** It is
  another session's in-flight firmware work and must stay unstaged. `CLAUDE.md`
  is likewise modified and not ours.
- **Flag firmware gaps explicitly in the final report.** "Don't silently guess
  and move on."
- **Verify on the device, not in a preview.** Nearly every genuine fault this
  pass — the wrapped readout, the invisible range band, the tagline's
  transparent margins, the collided caption — was invisible in code and in
  Compose previews. `adb` is on PATH and a Redmi Note 10S is usually connected.
- The user likes being told what was *not* verified. Say it plainly.

### Design constraints that keep coming back

- Shady must not read as childish. **No speech or thought bubbles.** Absent
  from every safety-critical surface, and off entirely where the emptiness is
  itself the bad news (`Status.kt:260-262` names the emergency card and an
  empty contact list).
- **Do not regress:** the GATT operation queue (one outstanding op — everything
  goes through `enqueue()`/`drainQueue()`), `MESSAGE_CHAR_UUID` is
  Guardian→Companion while `REPLY_CHAR_UUID` is Companion→Guardian, permission
  gating before any scan/connect, `minSdk 26` / `targetSdk 36`.
- Brand assets are in `docs/Logo/`.
- `DESIGN.md` at the repo root is the design system and is current as of
  v2.2.0. Update it in the same pass as any change it describes — it went stale
  once this pass (it still named `LocalBoardAccent` after that symbol was
  deleted) and a design doc naming a dead symbol is worse than one saying
  nothing.

---

## 2. Traps in this repo and this environment

Every one of these has cost real time.

- **Kotlin block comments NEST.** A `/` immediately followed by `*` inside a
  KDoc — writing a directory glob, for instance — opens a comment that never
  closes and fails the build with "Unclosed comment". This has broken the build
  three times. Warn every subagent explicitly.
- **`git add` validates all pathspecs before staging any.** One bad path stages
  *nothing*, and if the next line is `git commit` with no `&&`, it commits an
  empty or partial change with a message describing work it does not contain.
  This produced `d1ea704`, a commit whose message claimed the paired-devices
  fix and the osmdroid map and contained neither — leaving three commits where
  HEAD did not compile while the working tree was perfectly fine. **Always
  check `git diff --cached --stat` before committing, and prefer `&&`.**
- **`uiautomator dump` reports app node bounds as though the IME were not on
  screen.** With the keyboard open, a tap at those coordinates lands on the
  keyboard. On the contact editor this silently typed digits into the phone
  field and made a *correct* refusal look like a broken save; it cost about an
  hour. Send `keyevent 4` to dismiss the IME before tapping anything below it —
  `keyevent 111` (ESC) is **not** enough.
- **MSYS path mangling:** `adb shell` paths like `/sdcard/x.png` get rewritten
  to `C:/Program Files/Git/sdcard/x.png`. Prefix commands with
  `export MSYS_NO_PATHCONV=1`.
- **Never use a regex that spans a KDoc block.** A greedy
  `re.search(r"\n/\*\*.*?\*/\n...", re.S)` matched from the *earliest* `/**` in
  the file and deleted 202 lines of `Color.kt` — then did the same to
  `BoardScreen.kt`. Use exact-string replacement with an occurrence-count
  assertion. The pattern used all pass:
  ```python
  assert s.count(old) == 1, (old[:60], s.count(old))
  ```
- **Files are mixed LF/CRLF** in the working tree (`core.autocrlf` is on).
  Read as bytes, detect `\r\n`, normalise to `\n` for matching, restore on
  write. Otherwise exact-match replacements fail confusingly.
- **The Bash tool's parser chokes on large heredocs.** Writing a ~200-line
  Python script via `cat > f <<'EOF'` failed twice with "unexpected EOF while
  looking for matching `'`". Use the `Write` tool for the script, then run it.
- **`Scaffold` measures its `bottomBar` with the full screen height as the max
  constraint.** A `fillMaxHeight()` child drags the whole bar to full height
  and it ends up floating over a blank screen. This has been introduced twice;
  both `BoardBottomBar` tabs and `SosSlot` now carry fixed heights with
  comments saying so.
- **`Modifier.padding` has no overload mixing `horizontal` with `top`.** Use
  `start`/`end`/`top`.
- A drawable whose canvas is larger than its artwork makes every layout number
  around it a lie. Both `splash_emblem` and `brand_tagline` were like this.
  Check `Image.open(p).convert("RGBA").split()[3].getbbox()` before trusting a
  size.

---

## 3. Where the code stands

`versionCode = 6`, `versionName = "2.3.0"`. 28 unit tests pass
(`DayStripTest` 8, `DeviceProtocolTest` 15, `PhoneNumbersTest` 4,
`ExampleUnitTest` 1). `assembleDebug` is green.

### The kit (`ui/board/`) — read before inventing anything

`Controls.kt` (BoardButton, Readout, Gauge, StubMark), `Plates.kt` (BoardPlate,
SectionPlate, Hairline), `Way.kt`, `PilotLamp.kt`, `Status.kt` (MainsPlate,
EmptyBay), `ScreenHeader.kt`, `Expandable.kt`, and new this pass:

- **`Chips.kt`** — `ChipRow`, plus `RELATIONSHIP_SUGGESTIONS` and
  `BLOOD_GROUP_SUGGESTIONS`. Not a picker: it sets a text field that stays
  editable beside it. Tapping the lit chip clears it.
- **`RichControls.kt`** — `DialControl` (the zone-radius pattern generalised:
  big readout, stepped track, advice line that changes as it moves; `onCommit`
  fires on release and **anything writing over BLE must use it**),
  `TimeStrip` (a time of day on a strip of the whole day, shaded through
  night), `RangeStrip` (two handles, wraps past midnight), and public
  `formatClock`.

`KitGallery.kt` renders the whole kit live in both themes and is reachable from
the developer screen. It has entries for all of the above. **Check it before
adding an eighth kind of card.**

### Rules the kit enforces

- **Small fixed sets do not get a slider.** Three named behaviours on a track
  read as a continuum with two invisible stops. Fall sensitivity, fall
  countdown and check-in interval stay `OptionWay` rows.
- Selection is never carried by colour alone — border weight, fill and ink
  together.
- Twelve decorative accents, assigned by `accentFor(key)` (a stable hash), not
  ambiently. `LocalBoardAccent` was deleted. Contrast re-measured this pass:
  worst 4.50:1 in both themes, peer spread 1.01–1.02:1.
- The **saturation** rule is what keeps decoration from reading as status —
  state colours are saturated and reserved. One deliberate exception exists and
  is argued in the file: the light-pattern swatches in `LightsScreen.kt`.

### Known rough edges / deliberate debt

- **`OptionWay` lives in `ui/screens/safety/SafetyCommon.kt`** and is imported
  from `device/DeviceSettingsScreen.kt`. It is `internal`, so this is legal,
  but it belongs in `ui/board/` with the rest of the row kit. Moving it touches
  several safety files; worth doing in a quiet moment.
- **`ChipRow` has no visible focus ring.** It uses
  `selectable(indication = null)`, so tabbing or an IME "Next" into the row
  lands focus invisibly. Flagged by a subagent; not yet fixed.
- Five of the six "cramped card" fixes are **code-verified only** — blind-tap
  navigation on this MIUI device was too unreliable for a full 1.3× visual
  sweep. `ValueRow`, the mode-card title row, the emergency supporting text on
  two screens, `ScheduleRow`'s successor and `BlockedFeatureCard` all deserve a
  look at `font_scale 1.3`. (`adb shell settings put system font_scale 1.3`,
  and **put it back to 1.0** — an agent left it at 1.3 mid-session and every
  screenshot taken meanwhile was at the wrong scale.)
- `material-icons-extended` is still a dependency. After the icon pass, check
  whether it can go — that is a real APK size win.

---

## 4. What was verified on the device, and what was not

**Verified live** (Redmi Note 10S, light theme, 1.0× unless stated):
the medication strip sets 19:00 and survives leaving the screen; quiet hours
drags both handles and wraps past midnight (22:00–09:00, "11 hours"); a contact
saved with a relationship survives a **cold start** (the Gson round-trip, which
is the check that matters); the map opens, renders OSM tiles and moves its
centre on a tap; the intro shows exactly one emblem and the tagline plate
matches the lockup within 2px; the launcher icon under a circular mask
(half-diagonal 120.7px vs 144px safe radius); all four sub-page header patterns
land at identical bounds; the navbar's accent-filled tabs share a centreline
with the SOS; empty states show one CTA and the right Shady; light swatches
animate per pattern; the Device header icon sits on the headline's centreline.

**Not verified:**

- **The paired-device write path.** All four bugs are fixed in code, but
  confirming a device actually gets remembered needs the physical wearable.
  This is the highest-value thing to check next time hardware is to hand.
- **Anything reaching the firmware.** Nothing was connected during this pass,
  so no setting was observed arriving at the device.
- **The phone SOS has still never been fired end to end.** See `handoff3.md`
  §3.5 — `ActionResult.Sent` means "handed to the radio", not "delivered".
- Five of six cramped-card fixes at raised font scale (above).
- Dark theme was not swept this pass.

---

## 5. Firmware — what the app now knows that the firmware side should

Carried forward and still outstanding:

- **No wire format exists for relaying a phone-originated SOS over the
  wearable's cellular gateway.** Needs a new EXT tag plus a firmware handler.
  `handoff3.md` §3.1. This is the single biggest app↔firmware gap.
- `handoff3.md` §0.1–0.4 still stand: `CMD_FIND` has no real locate path,
  `CMD_FIND` and weather share `WEATHER_CHAR_UUID`, eight on-device settings
  have no BLE write path at all, and there is no silent-SOS path.

Found by reading `updateRGBPattern()` this pass (app corrected, firmware side
worth a decision):

- **Torch is the only LED pattern that calls `setHeadlamp(true)`.** Choosing
  any other pattern silently switches the wearable's front lamp off. Nothing in
  the app said so; Torch's description now does. Is that coupling intended?
- Two app descriptions were factually wrong against the firmware and are fixed:
  Cyber is a single violet comet with a fading tail on a dark ring (not "a
  sweep between two cool tones"); Ocean's hue never leaves blue — the
  brightness travels.
- The light swatches transcribe that function channel-for-channel and
  millisecond-for-millisecond, **except Police**, which the app runs at 600ms
  per half rather than the firmware's 300ms. At the real rate it alternates
  saturated red and blue 3.3×/second; WCAG 2.3.1 draws its line at three, with
  a stricter threshold for red specifically. If the firmware timing changes,
  the swatch comment in `LightsScreen.kt` needs updating.

**When the hardware consistency pass begins:** `arduino-cli` is installed
locally, the mainboard must build as `XIAO_ESP32C3` with the `no_ota`
partition scheme, and `docs/SafeShadev21/SafeShadev21.ino` is currently being
edited by another session — coordinate before touching it.

---

## 6. The five real defects fixed in v2.2.0

Recorded because each was invisible until specifically hunted, and the class of
mistake will recur.

1. **The medication reminder could never be set.** `onEditMedicationTime` was
   a hardcoded `{}` at both call sites, with a comment deferring the picker to
   "whoever owns the device screens". Two screens each drew a "Change" button
   that did nothing.
2. **`d1ea704` contained none of what its message claimed** (see the `git add`
   trap above). Repaired by `234b73f`.
3. **The map was never missing** — a WebView whose Leaflet CDN fetch, on
   failure, returned before attaching the tap handler. Now native osmdroid, no
   key, disk tile cache, works offline on a second visit.
4. **Text ran off the right edge** of `BlockedFeatureCard` at *every* font
   scale — neither side of the row carried a weight.
5. **Item 9 (header consistency) was marked done and measurably was not.** The
   map picker still hand-rolled a fourth header pattern, and six Circle
   sub-pages started 8dp higher than every other bank. Both found by comparing
   `uiautomator` dumps that had already been captured. **Compare measurements
   before declaring a layout item done.**

Two assumptions exploration overturned, so nobody spends the time again: the
intro wordmark was **already** the right font (Archivo W800; the fault was
`letterSpacing = -0.03em`), and the proportion problems were in the drawables'
transparent margins rather than in any layout constant.
