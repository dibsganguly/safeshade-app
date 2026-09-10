# SafeShade — Session Handoff #9

**Rewritten at the close of the first Phase 4 session (v2.8.0), 2026-09-10.**
Self-contained, like handoff8: a fresh session with no memory should be able
to continue Phase 4 from this file, `DESIGN.md` and the code. Handoff8 is the
record of Phase 3 and of the pass as a whole; where this file and handoff8
differ, this file wins.

## Contents

- [0. What to trust](#0-what-to-trust)
- [1. Where the pass stands](#1-where-the-pass-stands)
- [2. Decisions made this session](#2-decisions-made-this-session)
- [3. What this session shipped](#3-what-this-session-shipped)
- [4. The v2.0 candidates, and how to adopt one](#4-the-v20-candidates-and-how-to-adopt-one)
- [5. Phase 4 — what remains](#5-phase-4--what-remains)
- [6. Verified on the device, and not](#6-verified-on-the-device-and-not)
- [7. Known bugs and debt](#7-known-bugs-and-debt)
- [8. Traps, and the tools that route around them](#8-traps-and-the-tools-that-route-around-them)
- [9. What the user still has to do](#9-what-the-user-still-has-to-do)
- [10. Session close](#10-session-close)

---

## 0. What to trust

| File | Status |
|---|---|
| `DESIGN.md` | **Current.** Updated in the same commits as the kit changes it describes. It gained a "v2.0 candidates" appendix this session; the frontmatter tokens are unchanged. |
| `docs/handoff9.md` | This file. Current. |
| `docs/handoff8.md` | Phase 3 and the pass. Its §2 decisions, §5 cloud, §7 Phase 5 test list, §8 not-verified list, §9 debt and §10 traps still apply unless restated here. |
| `docs/handoff7.md` | Phases 1–2. Its §9 debt and §10 traps still apply. |
| `docs/icons-wanted.md`, `docs/Icons/*.svg`, `CLAUDE.md`, `PRODUCT.md`, `docs/SafeShadev21/SafeShadev21.ino` | **Not ours.** Never commit, stage, revert or edit them. `docs/Icons` is the source of truth for the icon object and is read by `tools/gen_icons.py`; two new SVGs arrived this session and were generated in, but the SVGs themselves stay untracked. |
| `supabase/` | Done and live. Do not touch unless the user asks. |

**Trust the code over any document.**

---

## 1. Where the pass stands

- **Phases 1–3 — done** (v2.5.0, v2.6.0, v2.7.0; handoff8 §1).
- **Phase 4 — in progress** (v2.8.0, `versionCode 11`). This session: the
  version bump, the user's first three UI/UX items, the v2.0 candidate
  section in the Kit gallery, and the mechanical half of the handoff8 §6
  sweep. Commits `a648f99`..HEAD, **not pushed** (the user authorises every
  push). `assembleDebug` green, 645 unit tests pass (unchanged: nothing this
  session touched a tested class).
- **Phase 4 is now blocked on the user**: they asked for the candidate
  gallery *before* any screen changes, and will name the numbers they like.
  Every redesign-flavoured item of §6 waits on those picks (§5).
- **Phase 5 — testing, vulnerability and bug fixing only.** Unchanged.

### Model allocation (a standing instruction)

Fable 5.1 orchestrates and owns all UI/UX; Opus 5 only if something backend is
unavoidable; Sonnet 5 for ordinary implementation; Haiku 4.5 for sweeps. Every
sub-agent gets a hard file scope and never runs a writing git command. This
session ran one Sonnet agent (the §3.4 moves) with that scope; it reported only
errors naming its own files, and the orchestrator compiled after it stopped.

### Ground rules restated by the user on 2026-09-10

Never push without asking. Nothing in the UI says planned, simulated,
representative or coming soon; absent data is a dash, a failed action reports
its reason, no tick before a result. The Supabase anon key lives only in
`local.properties`; no service-role key in the APK; never write the database
password anywhere. Never use eisfoundation.com. Sender stays
`onboarding@resend.dev`. Commit prefix `v2.8.0:` with long why-messages that
say what was not verified. Update `DESIGN.md` in the same commit as any kit or
pattern change.

---

## 2. Decisions made this session

By the orchestrator unless marked; each reversible.

- **The dashboard's three actions are glyphs alone** (user). Message, Where
  and Call on the family dashboard's person plate lost their words, which
  wrapped mid-word at the phone's width; the word is now the spoken
  description on a new kit member `BoardIconButton`, whose description is a
  required parameter. Where wears `where-navigation` (user), Nearby on the
  Device page wears `bluetooth-nearby` (user).
- **Candidates live in one file and touch no screen.** The v2.0 section is
  `ui/board/KitCandidates.kt`; its type families are
  `ui/theme/CandidateType.kt`; nothing outside the gallery references either.
  A candidate is adopted by number only (§4).
- **The candidate fonts ride in the APK until the user chooses.** Six OFL
  variable TTFs, about 1.5 MB. The cost is deliberate and temporary; the
  losers are deleted with their families when a pick lands.
- **A focused chip shows its focus.** `ChipRow` had no focus indication (no
  ripple, no ring); a focused chip now steps its edge to ink at the rule
  weight. Recorded in DESIGN.md's Chips section.
- **`PlateField`, `OptionWay` and the check-in constants are kit**, moved
  from `ui.screens.safety` and `ui.screens.device` into `ui/board/`
  (handoff7 §7 debt). No behaviour or string changed.
- **`Routes.BOARD_LINK` and `ComingSoonPlate` are gone**, and so is the
  stranded `ONBOARDING_RELIABILITY` route constant. The one page that printed
  "Coming in this build" no longer exists.

---

## 3. What this session shipped

Commits on `master`, `v2.8.0:` prefixed, each message listing what was and
was not verified.

### 3.1 `a648f99` — the version bump

`versionCode 11`, `versionName "2.8.0"`. Nothing else.

### 3.2 `088de47` — the user's list, items one to three

- `BoardIconButton` in `ui/board/Controls.kt`: the plate, weights, 56dp
  floor, disabled state and press of `BoardButton` with a 28dp centred glyph
  and a required `contentDescription`. DESIGN.md Buttons section and the
  family dashboard paragraph record it.
- `CircleScreen.kt`'s `WearerPlate` uses three of them: Message
  (`SendMessage`), Where (`WhereNavigation`), Call (`TelephoneCall`, disabled
  with its SIM reason under it as before).
- `DeviceScreen.kt`'s Nearby way wears `BluetoothNearby`.
- `SafeShadeIcons.kt` regenerated from `docs/Icons` (229 glyphs; the diff was
  purely additive: `WhereNavigation`, `BluetoothNearby`, two map entries).

### 3.3 `84ca6a3` — the v2.0 candidates

`ui/board/KitCandidates.kt` (`LazyListScope.kitCandidates()`), wired at the
top of `KitGallery`; the shipped kit follows under "The kit as shipped".
Forty-one candidates, 2.01–2.41, in seven groups; the index is in DESIGN.md's
"v2.0 candidates" appendix and in §4 below. `ui/theme/CandidateType.kt`
declares the six candidate families; `res/font` gained
`bricolage_grotesque_variable.ttf`, `instrument_sans_variable.ttf`,
`fraunces_variable.ttf`, `manrope_variable.ttf`, `plus_jakarta_sans_variable.ttf`,
`geist_mono_variable.ttf`.

### 3.4 `72f61fc` — mechanical §6 items and the focus ring

- `ChipRow` focus ring (`ui/board/Chips.kt`, DESIGN.md).
- `PlateField` → `ui/board/Fields.kt`; `OptionWay` → `ui/board/Way.kt`;
  the check-in interval constants → `ui/board/CheckInConstants.kt`; every
  importer repointed. `SafetyCommon.kt` keeps the rest.
- `Routes.BOARD_LINK`, `ComingSoonPlate`, `Routes.ONBOARDING_RELIABILITY`
  deleted.
- `material-icons-extended`: checked, already absent from
  `gradle/libs.versions.toml` and `app/build.gradle.kts` (handoff8 §6 listed
  it as still to drop; it was dropped with the icon set in Phase 3).

---

## 4. The v2.0 candidates, and how to adopt one

Open the gallery on the phone: **Device tab → the face top-right (Profile) →
scroll → Developer → Kit gallery.** The candidates are the first thing on it.
Each shows its number in a brass tag, its title, "Refines …", the thing
itself (live where it has state), and one line on why.

| # | Candidate | Refines |
|---|---|---|
| 2.01 | Bricolage Grotesque titles, Instrument Sans everything else | the whole scale |
| 2.02 | Fraunces headlines over Archivo | display and headline |
| 2.03 | Manrope throughout | the whole scale |
| 2.04 | Plus Jakarta Sans throughout | the whole scale |
| 2.05 | Instrument Sans throughout, its own condensed | the whole scale |
| 2.06 | Geist Mono for readouts | Readout, Gauge, countdown |
| 2.07 | A readability step: rows at 17 and 14 | nameplate, rowDetail |
| 2.08 | A ground per hub | ground |
| 2.09 | A washed plate with an accent rule | BoardPlate |
| 2.10 | A deeper night | dark neutrals |
| 2.11 | A warmer bone | light neutrals |
| 2.12 | Glyphs on discs | the icon on a Way |
| 2.13 | A way with its glyph on a disc | Way |
| 2.14 | Rows that open look different from rows that report | Way |
| 2.15 | A compact way | Way, dense banks |
| 2.16 | A bank with named groups inside it | the Device page's ways |
| 2.17 | A way whose state is a figure | Way, logs |
| 2.18 | An action as a way | the quiet button under a bank |
| 2.19 | Mains plate with the person on it | MainsPlate |
| 2.20 | A masthead per hub | the hub title |
| 2.21 | Fact wells | readout strips |
| 2.22 | A ledger | paragraphs that list facts |
| 2.23 | A timeline on the bus | trip record, ladder plate |
| 2.24 | A segmented choice | OptionWay, two to four options |
| 2.25 | A footnote under the bank | the explanatory plate |
| 2.26 | A callout | the paragraph that states a consequence |
| 2.27 | What happens, as a chain | the sentence that describes a sequence |
| 2.28 | Help on the row that needs it | WhyDisclosure |
| 2.29 | Qualifier chips | the seal, generalised |
| 2.30 | Glyph buttons with a caption | BoardIconButton |
| 2.31 | A button that carries a figure | BoardButton |
| 2.32 | The bar with words | bottom bar |
| 2.33 | The bar with a rule | bottom bar |
| 2.34 | A foot bar for editors | the commit button on a long editor |
| 2.35 | A gauge with its range | Gauge |
| 2.36 | Signal as bars | the Signal readout |
| 2.37 | A status well | hub-level state |
| 2.38 | Twenty-four bars | history rows |
| 2.39 | The person plate, again | the family dashboard plate |
| 2.40 | A wearer strip | the person switch |
| 2.41 | The wearable's card | the Device page's head plate |

**Adopting one.** When the user names a number: move the composable out of
`KitCandidates.kt` into the file its family lives in (`Way.kt`, `Plates.kt`,
`Controls.kt`, `Status.kt`, a new file for a new family), give it a public
signature with the kit's parameter conventions, rewrite the DESIGN.md rule it
changes in the same commit, apply it to the screens it names, and delete the
rest of its group. For a type pick: move the family into `Type.kt`, retune
`BoardType` and `BoardMaterialTypography`, delete the five losing TTFs and
`CandidateType.kt`. For a colour pick (2.08, 2.10, 2.11): the values are in
the candidate's source and were contrast-checked live on the phone; rewrite
`Color.kt` and the DESIGN.md frontmatter together. The candidate numbers stay
stable until the file is empty; a rejected candidate is deleted, not renumbered.

Some candidates pair: 2.08 with 2.20 (hub grounds and mastheads), 2.12 with
2.13 (discs on ways), 2.21 with 2.19 and 2.39 (wells), 2.30 with 2.39 (the
captioned strip on the person plate), 2.29 with 2.14 (chips on nav rows).

---

## 5. Phase 4 — what remains

**Blocked on the user's picks** (handoff8 §6, redesign-flavoured):

- Pages are too text heavy: fewer paragraphs, more instruments, plates and
  rows. Candidates 2.22, 2.25–2.28 are the offered answers.
- The Device page's eleven ways: 2.16.
- Vitals at 1.3× on a narrow phone, the "Recent" rows' five readouts: 2.21,
  2.38.
- Lost's per-device three-sentence line: 2.21.
- Firmware's five-meaning state word: 2.15 or a legend.
- Smart home editor's two chip rows and three fields: 2.24, 2.34.
- Evidence's Delete discoverable only while playing: 2.17 with a trailing
  action, or a swipe.
- Pair a wearable's three silhouettes at 1.3×: not photographed.
- The Board's nudges plate with no heading: a section plate or 2.26.
- `WhyDisclosure` count (ten call sites): 2.28 replaces the pattern.
- The weekly report caption (handoff7 §7; the exact complaint was not
  restated and the current line reads "Monday morning: the week's alerts,
  messages and coverage, for the whole Circle").
- Dark theme on the map picker: osmdroid's tiles are the daylight Mapnik set
  in both themes; a dark tile source or a tint overlay is a design choice.
- Dark theme on the intro: the intro draws on `colors.ground` and is themed;
  photograph it in dark before deciding anything.
- The Nearby way with no link reads FAR in trip red with "Out of range for
  N s" (noticed this session). Whether an absent link should be a dash with
  an OFF tick is a wording decision; it is not a bug in the leash.

**Not blocked, still to do:**

- Screenshots of every route in both themes and at 1.3× (handoff8 §6 asked
  for it before changing anything; this session photographed the four hubs,
  the Circle plate and the Device page in light at 1.0×, and the gallery in
  all three conditions).
- The Haiku copy sweep: grep the UI for supporting lines that fail the
  "states a consequence" test (DESIGN.md's Don'ts). Useful delegation; the
  result feeds the text-heaviness work above.
- The mandated-string grep before every commit that touches copy: zero hits
  in `ui/` at this session's close (`planned|simulated|representative|coming soon`).

---

## 6. Verified on the device, and not

Test device: Redmi Note 10S, MIUI 14, Android 13 (API 33), signed in with
Google. No wearable was connected at any point.

### Verified this session

- **Family dashboard** (light, 1.0×): three glyph buttons at equal widths on
  both person plates; Call greyed with its SIM reason on the person with no
  wearable; the compass on Where.
- **Device page** (light, 1.0×): the Nearby row with the new glyph.
- **Kit gallery**: every candidate 2.01–2.41 photographed in light at 1.0×,
  dark at 1.0× (through the app's own Appearance setting), and light at 1.3×
  (`settings put system font_scale 1.3`). Three faults were found on the
  phone and fixed before the commit (wells wrapping a phrase, the compact
  way's key folding at 1.3×, the SOS mock drawing a cross).
- **The app's Appearance setting** was returned to Light and the font scale
  to 1.0 at the close.

### Not verified — say this to the user, plainly, every time

- Dark theme and 1.3× on the family dashboard and the Device page after the
  glyph changes.
- TalkBack on `BoardIconButton` (the description is set; not heard).
- That a tap on each glyph button still reaches its destination (the
  callbacks are unchanged; not re-driven).
- The §3.4 moves: Medical ID (`PlateField`) and Fall detection (`OptionWay`)
  were photographed after the move and match; Device settings, Reminders and
  Check-in (the constants) compile and the 645 tests pass but were not
  re-photographed.
- The `ChipRow` focus ring under a keyboard or switch access (built; no
  keyboard on the test phone).
- The candidate fonts on any device other than the Redmi.
- Everything in handoff8 §8 "Not verified".

---

## 7. Known bugs and debt

### Fixed this session

- **The family dashboard's action labels wrapped mid-word** at the phone's
  own width ("Mess / age", "Wher / e"). Glyph-only buttons now.
- **`ComingSoonPlate` on `BOARD_LINK`** (handoff8 §9 carried debt): deleted.
- **`PlateField` placement** (handoff7 §9): moved to the kit.
- **`ChipRow` had no focus indication** (handoff7 §7): a ring.

### Carried debt (unchanged from handoff8 §9 unless listed)

- **Six candidate fonts in the APK** (~1.5 MB) until the user chooses.
- **`KitCandidates.kt` duplicates small pieces of kit** (`StateWord`,
  `IconDisc`, `Well`, `NavWay`) privately so that no candidate leaks into a
  screen. When a candidate is adopted its helpers come with it; when the
  file is emptied the duplicates go.
- **The Nearby way's FAR with no link** (§5).
- Everything else in handoff8 §9 and handoff7 §9.

---

## 8. Traps, and the tools that route around them

Handoff8 §10, handoff7 §10 and handoff6 §3 still apply. Added this session:

- **`drive.sh` needs a Windows-style `SP`.** With `MSYS_NO_PATHCONV=1` set by
  the script, `adb pull` cannot write to `/c/...`; export
  `SP="C:/Users/.../scratchpad/shots"` before sourcing it. `adb shell cat
  /sdcard/ui.xml` outside the sourced script also needs
  `MSYS_NO_PATHCONV=1`, or Git Bash rewrites `/sdcard` to a Program Files
  path.
- **`tapfind` right after `launch` returns NOTFOUND**: the first dump after a
  cold start is taken before the list has laid out. Sleep 3–4 s after
  `launch` and after each tab tap; the Developer row is two long swipes down
  the Profile page (`swipe 540 1700 540 300 800`, sleep 3, then find).
- **A heredoc `python - <<'EOF'` with an empty body hangs the Bash tool** by
  opening the REPL; it took a two-minute timeout to find out. Always give the
  heredoc a body or drop it.
- **The gallery's `LazyColumn` recomposes the live candidates on every
  scroll**; a `swipe … 900` with a 1.5 s sleep produced 31 distinct frames
  and no duplicates, which is the cadence to keep for a full capture.
- **Appearance is set in the app, not with `cmd uimode`** (MIUI ignores it,
  handoff8): Profile → Appearance, the three option ways are spoken
  "Light, On, …" / "Dark, Off, …", so `tapfind "Dark, Off"` throws it and
  `tapfind "Light, Off"` throws it back.
- **The user's mid-turn additions land as tool-result attachments**, not as
  new turns; both this session's extra items (Nearby's icon) arrived that
  way. Read every tool result's trailer.

---

## 9. What the user still has to do

Still open from handoff8 §11: the push (now `a648f99`..HEAD on top of the
four v2.7.0 commits), the second account, the eleven-digit contact, the two
Vault secrets, placing the widget and the tile, a `firmware_releases` row,
Health Connect on the test phone, a phone with NFC. Added:

- **Name the candidates they like**, by number, from the Kit gallery on the
  phone (it is installed as build 2.8.0). Pairs are noted in §4.
- **Say whether the dashboard's glyph-only buttons stay glyph-only** or take
  the captioned form (2.30); the plate is drawn both ways now.
- **The rest of their UI/UX list**, if the three items delivered were not
  the whole of it.

---

## 10. Session close

**What shipped** is §3: the version bump, the three items of the user's list
that arrived, the forty-one candidates, the mechanical half of the sweep, and
one accessibility fix. **What was not verified** is §6. The decisions taken
without the user are §2 and are all reversible.

**Context.** This session was not compacted. It ran the gallery's eighty-odd
screenshots through the model, which is the expensive part; the working
state that matters is in the commits, this file, `DESIGN.md` and the memory
directory. The scratchpad screenshots are throwaway.

**Continue or hand over.** Hand over, and wait. Phase 4 cannot proceed past
this point without the user's picks: every remaining §6 item is a redesign
choice the candidates exist to settle. The next session should open on the
user's list of numbers, read this file's §4 for the adoption procedure and
DESIGN.md's appendix for the index, adopt the picks one commit each (kit
member, DESIGN.md rule, the screens it names), then photograph every route
in both themes and at 1.3× before the copy sweep. If the user has not
chosen, the useful hour is the Haiku copy inventory (§5), which does not
depend on the picks and feeds every one of them.
