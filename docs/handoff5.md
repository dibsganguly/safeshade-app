# SafeShade — Session Handoff #5

**Written at the end of the v2.4.0 pass.** Read this first, then `handoff4.md`
§1–2 for the working style and the environment traps (all still current, none
repeated here), and `handoff3.md` §0 and §3–4 for the firmware-facing detail.

`DESIGN.md` is current as of this pass and was updated in the same commits as
the changes it describes. `CLAUDE.md` still describes an older architecture in
places — trust the code.

---

## 0. What this session was for, and what happened

The user asked for, in order: an icon pass; UI/UX refinements from their own
list plus mine; and then a firmware consistency pass. **The firmware pass did
not start.** Everything below is the app.

Seventeen numbered items came from the user and all seventeen are done. Four
questions were put back to them and answered:

- Circle's pilot-lamp captions: keep the word, make it much quieter, and move
  it somewhere better. It moved to the foot of the card's left-hand block.
- Title Case on prominent buttons only.
- Which duration controls become sliders: my judgement.
- How to handle `material-icons-extended`: my judgement.

---

## 1. The one thing the next session should do first

**`material-icons-extended` can be dropped once eleven or so SVGs exist.** This
is a real APK size win and it is now blocked on exactly one file:
`ui/board/ModeVisuals.kt`, which maps eight `PersonaMode`s and eight
`DeviceIconType`s onto Material glyphs. Sixteen distinguishable icons are
needed and `docs/Icons` covers one of them (`adaptive-mode`, which `AUTO` now
uses).

Reusing one custom glyph across several modes is explicitly the wrong answer —
it destroys the only thing telling the modes apart, and `handoff4.md` §0 warned
against it. So the ask is for **artwork, not code**.

I checked the Hugeicons catalogue through its MCP; every one of these exists by
name and would need exporting into `docs/Icons` as `<name>-stroke-rounded.svg`:

| Concept | Hugeicons name |
|---|---|
| Elderly persona / cane device | `elder` |
| Kids persona | `child` |
| Bike persona / bike device | `bicycle` or `bicycle-01` |
| Pet persona / collar device | (search `pet`, `dog`, `paw`) |
| Helmet persona / hat device | `cricket-helmet` or `baseball-helmet` |
| Wrist persona | (search `hand`) |
| Backpack persona / backpack device | `backpack-01` |
| Umbrella device | `umbrella` |
| Watch device | `watch-01` or `smart-watch-01` |
| Pendant device | (search `diamond`, `gem`) |

The sync control went **back** to `Icons.Outlined.Sync` after the icon pass
put `cloud-loading` there: a cloud with a dashed arc reads as *offline* on the
one screen whose job is saying whether things are connected. A circular-arrow
reload glyph is the single most valuable addition to the drop - search
`arrow-reload-horizontal` or `refresh`.

Nine more would clear the remaining one-off Material usages: `history` (trip
log, ×3), `timer` (overdue journey), `qr-code` (emergency card, ×2), `delete`
(remove a zone), `user-add` (add a contact), `walking` (journey, ×2), `flash`
(battery optimisation), `sun` / `moon` (dark-mode options), `login` / `logout`
(zone entry and exit).

After the SVGs land: run `python tools/gen_icons.py`, point `ModeVisuals.kt` at
the new names, then remove `implementation(libs.androidx.compose.material.icons.extended)`
from `app/build.gradle.kts`. **The only check that matters is a full
`assembleDebug`** — a missed usage fails at compile, not at runtime.

The MCP returns icon *names* only, not path data, so the export has to be done
by hand from the Hugeicons site.

---

## 2. The icon generator, and why it was not working

`tools/gen_icons.py` was checked in last pass with a note saying it had been
"confirmed to run clean against all 100". It had been confirmed to *write a
file*. The file did not compile. Three defects, all found by running it and
reading the output rather than by reading the script:

1. `3-g-signal` pascal-cased to `3GSignalStrokeRounded`, and **a Kotlin
   identifier cannot start with a digit**. The numeric run now moves to the
   end: `Signal3G`.
2. The element scan matched `<path>` and `<circle>` only.
   `internet-stroke-rounded.svg` contains an `<ellipse>`, which was **dropped
   with no error** — that glyph would have rendered as a globe with an equator
   and no meridian. Every paintable element is handled now, and anything the
   converter does not recognise (or any `transform=` it cannot apply) exits
   with the slug rather than producing a partial drawing.
3. Every generated name carried a `-stroke-rounded` suffix that says nothing at
   a call site, because the whole set is that style.

Two things now live in the generator that used to live in nobody's head:

- **`OPTICAL`**, a per-slug scale table. A 24-unit viewBox says nothing about
  how much of itself the artwork fills, and this set is not uniform: most
  glyphs span about 18 units, `check-in` spans 20, so at an identical declared
  20dp it was visibly the largest thing in a column of `Way` rows. The
  correction is emitted as a scaled group in the generated Kotlin, **never as
  an edit to the source SVG**, so redropping the file cannot silently undo it.
  One entry so far.
- **`SafeShadeIcons.All`**, the list the kit gallery renders. It was
  hand-maintained and was already 67 icons out of date.

Verified: all 102 render live in the gallery, in dark theme, including
`internet` with its meridian.

---

## 3. What is verified on the device, and what is not

**Verified live** (Redmi Note 10S, 1.0× font scale, light theme unless stated):

- The amber Connect button, charcoal label, in **both** themes.
- The enlarged Board masthead beside the 52dp emblem.
- The Circle card with the lamp alone in its corner and its word at the foot.
- A quick message **failing**: the arrow stays an arrow and the reason
  appears in the snackbar. This entry read "the send arrow crossfading to a
  teal tick" for most of this pass and that was a false pass - the tick was set
  on the tap and the send behind it had failed. See §3a.
- The red location-refresh glyph in the Where heading.
- Fall detection's header gap, and its countdown as a `DialControl`.
- Collapsed-section counts, including `Appearance, 3 settings` as spoken.
- The map picker: no coordinate fields, a working radius slider, the custom
  red-on-bone pin, live OSM tiles.
- The board slider's material, cropped and inspected at pixel level.
- The board snackbar, which until the send fix had never been triggered on a
  device at all - it is only reachable from a failure or a blocked SOS.
- The `adaptive-mode` glyph, in the Device bank.
- **Dark theme, properly.** The last round of edits was all checked in dark,
  which closes most of the gap listed below: the Board, the Safety bank, the
  Device bank, Reminders, Paired devices, the Circle card and the emergency
  services screen were all swept in it.
- The message thread on the failure path: a quick reply and a typed one both
  report the reason, and the typed draft stays in the box. That last one is a
  deliberate change of behaviour - the box used to empty on a send that had
  failed - so it is here rather than in the list below.
- The empty-state magnifying glass, held over Shady's shoulder.
- All 102 icons in the kit gallery, dark theme.
- The intro lockup, measured programmatically off five device captures against
  the supplied logo's alpha channel. Every dimension within 0.021 of the
  emblem's height, most within 0.006.

**Not verified:**

- **The map pinch.** `adb shell input` cannot produce a two-finger gesture.
  `clipToBounds()` is reasoned from how osmdroid draws during a scale — it
  scales its whole canvas about the pivot rather than re-tiling per frame, and
  an unclipped View paints that wherever it lands — but nobody has watched it.
  **This is the single highest-value thing to confirm by hand.**
- **Anything that only exists while a wearable is connected.** The amber
  "Ring Device" button and the ash "Disconnect" are both reasoned from the code
  path their siblings take and have never been on screen - with nothing in
  range the Board shows Connect and the paired device shows Connect.
- **A quick message succeeding.** The failure path is watched; the success
  path needs a paired wearable or a stored SIM number and a real SMS, and
  neither was available. The tick has been seen correctly withheld and never
  seen correctly shown.
- The location-refresh **nod** animation. It fires on tap; a still cannot show
  it.
- **Raised font scale.** Nothing this pass was checked at 1.3×, and several
  things changed size: the masthead, the header, the slider, the lamp word.
  (`adb shell settings put system font_scale 1.3`, and **put it back to 1.0**.)
  The generator's `OPTICAL` correction is a particular unknown here: it scales
  inside the vector while `Way` draws at a fixed 20dp, so at 1.3× the glyph
  holds still while the text beside it grows and the correction stops meaning
  what it was measured to mean.
- **A second density.** Every intro measurement was taken on one device.
  `lineGap` is measured at runtime and self-corrects, but `EMBLEM_GAP`,
  `WORDMARK_W` and `TAGLINE_GAP` carry corrections derived from this screen.
  Related: the wordmark is fitted by measuring Archivo at a probe size, and
  font resolution is asynchronous - if the probe ever runs before the face
  loads it fits to the fallback's width for one frame. All five captures were
  post-load, so this has not been seen.
- **Dark theme beyond two screens.** The Board and the kit gallery were swept;
  the map picker, the intro and the Circle cards were not.
- Anything reaching the firmware. Nothing was connected at any point.
- The paired-device write path — still the highest-value hardware check,
  carried forward from `handoff4.md` §4.
- The phone SOS has still never been fired end to end.

---

## 3a. The one thing this pass got wrong and then fixed

The quick-message tick added in `4bbe2c3` was set on the tap, not on the
result, so it appeared just as readily for a send that failed - and with
nothing paired and no SIM number stored, which is what a fresh install and this
test phone both are, every send fails. The confirmation was watched, filmed and
written down as verified without anybody asking what it was confirming.

`MessagingRepository` had returned a `SendResult` for this the whole time, with
a `Failed.reason` written to be shown to a person, and its own comment says a
message that reports success and reaches nobody is worse than one that reports
failure. Every layer above it discarded the value. Fixed in `bdcff39`.

Two lessons worth carrying, because both are cheap to repeat:

- **A screenshot of an affordance is not a test of it.** The tick was captured
  mid-fade and the capture proved only that the animation ran.
- **A dead flag reads as live state.** `isSendingQuickMessage` and
  `MessagesUiState.isSending` were both defaulted `false` and never set by
  anything, and both had comments and UI written around them as though they
  worked. Grep for the setter, not the field.

---

## 4. Judgement calls a reader might otherwise re-litigate

- **Colour on a button.** Three hued button weights now exist where DESIGN.md
  previously said danger red was the only one. The rule was weakened
  deliberately and the argument is written into `Controls.kt` and DESIGN.md: a
  screen with four identical charcoal plates makes the one that matters
  findable only by reading. The saturation rule still separates these from the
  twelve decorative accents.
- **Amber means `inkAttention`, not `lampAttention`.** Brand amber on the bone
  panel measures about 1.9:1. Every "make it amber" request in this app
  resolves to the ink token unless the amber is a *fill*, in which case
  charcoal is the only label colour that measures — in both themes, because
  the glasses do not darken for the night panel.
- **The red GPS glyph** was asked for and is the one place in the app where a
  state hue sits on something that is not reporting a state. It is named in
  DESIGN.md so that finding it in the source is not read as a licence.
- **Which durations became sliders.** Fall countdown (15/30/45/60s — a linear
  ramp drawn as a menu) and check-in interval (four small buttons duplicated
  across two screens with two separate constant lists). Fall sensitivity stays
  rows: Low/Medium/High are named behaviours. The Silent SOS staged-call delay
  stays rows: 10s/30s/1m/5m is logarithmic and each option explains what
  staging means.
- **The map's coordinate fields.** Deleted despite a KDoc arguing they must
  never be hidden behind a map failure. That argument was written against the
  old WebView, which could fail to attach its tap handler and leave the map
  inert with no tell. The native map attaches its handler at construction and
  taps work whether or not a tile ever arrives.
- **A way's icon centres on its title's line, not on its row.** Two requests
  to "nudge this icon down" turned out to be one layout fault: the icon was
  anchored to the top of the row, which is right when a detail line puts the
  title at the top and wrong when there is no detail and the text is centred -
  worst on a row whose trailing control is a 48dp toggle, where the glyph and
  the word sat 41px apart. `Way` now anchors the icon to whichever end the
  title is at. Only one of the two icons needed anything done to the artwork.
- **`OPTICAL` carries a `dy` as well as a scale.** Same argument as the scale:
  a nudge is stated in viewBox units in the generator and emitted as a
  translated group, never as an edit to the SVG, so redropping the file cannot
  silently undo it. Two entries have one - `call-after-a-fall`, which really
  did render 13px above its neighbours, and `adaptive-mode`, which is the
  largest artwork in the drop at 21.6 units against a norm of 18.
- **A tick means the transport took it, not that it arrived.** An SMS is
  `Sent` when the send call returns and BLE writes have no application-level
  ack, so nothing in this system can honestly draw a delivery receipt. The
  ceiling is written where the tick is drawn. Closing it needs a firmware
  change, and belongs with the SOS relay gap in §6.
- **The Device bank's mode row takes a fixed glyph.** It drew the selected
  persona's icon, so a row named "Adaptive mode" showed a backpack. The other
  reading of the user's item 5 - keep the row varying and only change what
  `AUTO` maps to - was done as well and turned out to be invisible, because
  `AUTO` is drawn as a hero plate with no icon. If the varying icon is wanted
  back, it is one line in `DeviceScreen.kt`.
- **Title Case** is the ordinary kind — principal words capitalised, articles
  and short prepositions left alone unless they lead. "Connect to the Device",
  not "Connect To The Device".

---

## 5. Rough edges left deliberately

- **`OptionWay` still lives in `ui/screens/safety/SafetyCommon.kt`** and is
  imported from `device/DeviceSettingsScreen.kt`. Carried over from
  `handoff4.md` §3; it belongs in `ui/board/`.
- **The back chevron's touch target overlaps the title's first 8dp.** The slot
  reserves 28dp of layout and the button is a 48dp box offset -12dp, so it
  spans -12 to 36 while the title starts at 28. Harmless today because no
  header title is clickable; it would stop being harmless the moment one is.
- **`ChipRow` has no visible focus ring** (`selectable(indication = null)`).
  Flagged two passes ago, still true.
- The check-in interval's shared constants now live in
  `device/RemindersScreen.kt` as `internal` declarations that
  `DeviceSettingsScreen.kt` reads. That mirrors how `OptionWay` is shared, and
  it is the same smell: two screens sharing through one of themselves rather
  than through the kit.
- `WhyDisclosure` takes no count. It wraps prose rather than a set of options,
  so a number would mean nothing — but it is the one collapsed thing in the app
  without one, and somebody will notice.
- The empty-state magnifying glass now overlaps the top-right corner of Shady's
  body and its handle tip runs behind it. That reads as "held", and it balances
  the antenna on the other side, but it was not the drawn intention.

---

## 6. Firmware — carried forward unchanged

Nothing in this pass touched the firmware and nothing new was learned about it.
Everything in `handoff4.md` §5 still stands verbatim:

- **No wire format exists for relaying a phone-originated SOS over the
  wearable's cellular gateway.** Still the single biggest app↔firmware gap.
- `CMD_FIND` has no real locate path and shares `WEATHER_CHAR_UUID` with
  weather; eight on-device settings have no BLE write path; there is no
  silent-SOS path.
- Torch is the only LED pattern that calls `setHeadlamp(true)`, so choosing any
  other pattern silently switches the wearable's front lamp off. Still an open
  question for the firmware side.
- The Police light swatch runs at 600ms per half against the firmware's 300ms,
  deliberately, for WCAG 2.3.1. If the firmware timing changes, the comment in
  `LightsScreen.kt` needs updating.

`docs/SafeShadev21/SafeShadev21.ino` is **still another session's uncommitted
work** and was not touched. `CLAUDE.md` is likewise modified and not ours.
Coordinate before either is edited.

---

## 7. State at the end of this pass

`versionCode = 7`, `versionName = "2.4.0"`. Eight commits on `master`. `assembleDebug` green, 28 unit tests pass. Working tree clean apart from
`CLAUDE.md`, `docs/SafeShadev21/SafeShadev21.ino` and untracked `PRODUCT.md` —
none of them ours.

**The user is compiling a further list of UI/UX refinements for the next pass.**
That list comes first when it arrives.
