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
- A quick message's send arrow crossfading to a teal tick (caught mid-fade).
- The red location-refresh glyph in the Where heading.
- Fall detection's header gap, and its countdown as a `DialControl`.
- Collapsed-section counts, including `Appearance, 3 settings` as spoken.
- The map picker: no coordinate fields, a working radius slider, the custom
  red-on-bone pin, live OSM tiles.
- The board slider's material, cropped and inspected at pixel level.
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
- The location-refresh **nod** animation. It fires on tap; a still cannot show
  it.
- **Raised font scale.** Nothing this pass was checked at 1.3×, and several
  things changed size: the masthead, the header, the slider, the lamp word.
  (`adb shell settings put system font_scale 1.3`, and **put it back to 1.0**.)
- **Dark theme beyond two screens.** The Board and the kit gallery were swept;
  the map picker, the intro and the Circle cards were not.
- Anything reaching the firmware. Nothing was connected at any point.
- The paired-device write path — still the highest-value hardware check,
  carried forward from `handoff4.md` §4.
- The phone SOS has still never been fired end to end.

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
- **Title Case** is the ordinary kind — principal words capitalised, articles
  and short prepositions left alone unless they lead. "Connect to the Device",
  not "Connect To The Device".

---

## 5. Rough edges left deliberately

- **`OptionWay` still lives in `ui/screens/safety/SafetyCommon.kt`** and is
  imported from `device/DeviceSettingsScreen.kt`. Carried over from
  `handoff4.md` §3; it belongs in `ui/board/`.
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

`versionCode = 7`, `versionName = "2.4.0"`. Three commits on `master`, not
pushed. `assembleDebug` green, 28 unit tests pass. Working tree clean apart from
`CLAUDE.md`, `docs/SafeShadev21/SafeShadev21.ino` and untracked `PRODUCT.md` —
none of them ours.

**The user is compiling a further list of UI/UX refinements for the next pass.**
That list comes first when it arrives.
