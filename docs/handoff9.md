# SafeShade — Session Handoff #9

**Rewritten at the close of the second Phase 4 session (v2.8.0), 2026-09-10.**
Self-contained, like handoff8: a fresh session with no memory should be able
to continue from this file, `DESIGN.md` and the code. Handoff8 is the record
of Phase 3 and of the pass as a whole; where this file and handoff8 differ,
this file wins. The first Phase 4 session's handoff (the candidate gallery)
is superseded by this one; its adoption procedure is now history, recorded in
`DESIGN.md`'s appendix.

## Contents

- [0. What to trust](#0-what-to-trust)
- [1. Where the pass stands](#1-where-the-pass-stands)
- [2. Decisions made this session](#2-decisions-made-this-session)
- [3. What this session shipped](#3-what-this-session-shipped)
- [4. The kit after v2.8.0](#4-the-kit-after-v280)
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
| `DESIGN.md` | **Current.** Rewritten in the same commits as the kit: three type voices, the deeper night, the square lamp, compound actions, the card forms, the explainers, the channels, and an appendix that maps every adopted candidate to the kit member it became and lists the fifty-nine not chosen. |
| `docs/phase4-adoption-brief.md` | **Current.** The brief the four screen sub-agents worked from: old pattern → new member, wording rules, hard rules, report format. Reuse it for any further screen work. |
| `docs/handoff9.md` | This file. Current. |
| `docs/handoff8.md` | Phase 3 and the pass. Its §2 decisions, §5 cloud, §7 Phase 5 test list, §9 debt and §10 traps still apply unless restated here. |
| `docs/handoff7.md` | Phases 1–2. Its §9 debt and §10 traps still apply. |
| `docs/icons-wanted.md`, `docs/Icons/*.svg`, `CLAUDE.md`, `PRODUCT.md`, `docs/SafeShadev21/SafeShadev21.ino` | **Not ours.** Never commit, stage, revert or edit them. `docs/Icons` is the source of truth for the icon object and is read by `tools/gen_icons.py`; lost-search and ladder-stair arrived this session and were generated in (231 glyphs), the SVGs themselves stay untracked. |
| `supabase/` | Done and live. Do not touch unless the user asks. |

**Trust the code over any document.**

---

## 1. Where the pass stands

- **Phases 1–3 — done** (v2.5.0, v2.6.0, v2.7.0; handoff8 §1).
- **Phase 4 — the redesign is applied.** v2.8.0, `versionCode 11`. Fourteen
  v2.8.0 commits, `a648f99`..HEAD, **not pushed** (the user authorises every
  push). `assembleDebug` green, all unit tests pass.
  - Session one (`a648f99`..`8f7e7a9`): version bump, glyph-only dashboard
    actions, a hundred and one numbered candidates in the Kit gallery.
  - Session two, this one (`4496a25`..HEAD): the user chose thirty-three
    candidates by number with refinements; they became the kit (§4); every
    screen was rewritten onto it (§3); the whole app was photographed on the
    Redmi; the faults found were fixed.
- **What remains of Phase 4** is in §5: a short list of verification gaps and
  polish, not a redesign. The user may of course have more picks or changes.
- **Phase 5 — testing, vulnerability and bug fixing only.** Unchanged.

### Model allocation (a standing instruction)

Fable 5.1 orchestrates and owns all UI/UX; Opus 5 only if something backend is
unavoidable; Sonnet 5 for ordinary implementation; Haiku 4.5 for sweeps. Every
sub-agent gets a hard file scope and never runs a writing git command. This
session ran four Sonnet agents in parallel, one per screen group, each with a
file list and `docs/phase4-adoption-brief.md`; they compiled at the end (the
Gradle lock serialised them, and each saw at most one transient error from
another's in-progress file). The orchestrator did the kit, the two exemplar
screens, the header, the review, the photography and the fixes.

### Ground rules restated by the user on 2026-09-10

Never push without asking. Nothing in the UI says planned, simulated,
representative or coming soon; absent data is a dash, a failed action reports
its reason, no tick before a result. The Supabase anon key lives only in
`local.properties`; no service-role key in the APK; never write the database
password anywhere. Never use eisfoundation.com. Sender stays
`onboarding@resend.dev`. Commit prefix `v2.8.0:` with long why-messages that
say what was not verified. Update `DESIGN.md` in the same commit as any kit or
pattern change. The user trusts the orchestrator's taste and allowed "larger
reorganisations, rethinking, wording changes, larger UX changes" across every
page.

---

## 2. Decisions made this session

By the orchestrator unless marked; each reversible.

- **Jakarta is headings only** (user: "likes the face but NOT throughout").
  `BoardDisplay` carries the Material display, headline and title steps;
  nothing in `BoardType` uses it. Heading steps each gained a point because
  Jakarta's x-height sits lower; the display steps lost half their negative
  tracking. The intro wordmark stays Archivo (measured off the emblem).
- **JetBrains Mono replaces Azeret** for every readout. Wider; readout
  strips were checked at 1.3x on the hubs and hold.
- **The night went deeper** (2.10): ground `#0B0D0F`, plate `#181C20`, recess
  `#050607`, hairline `#2A3036`, lampOff `#2C3238`. Theme.kt's Material dark
  scheme, the snackbar plate and the widget's night plate follow. The light
  theme is untouched (user).
- **The lamp is square** (2.57), and so is the sync dot. Faces, glyph discs
  and the SOS disc stay round: a badge is round the way a lamp is square.
- **Save is amber; Discard is red ink on a quiet cell** (user: "Save in
  amber/orange instead of cyan; Discard can be red"). `ActionPair`'s primary
  defaults to the attention weight. Teal `COMMIT` remains for a standalone
  commit that is not the end of an editor (Send the Invitation). A filled red
  cell is a call button and Discard is not one, so the word is red, not the
  cell. DESIGN.md's colour rule was rewritten to say so.
- **The divider between joined cells is near-white** in both themes
  (`#FBF9F5`), 2dp, full height (user).
- **Hold-to-confirm traces anticlockwise from the top-left** (user): a
  rounded-rect path measured backwards from its start, so the trace runs
  down the left edge first and fires when it rejoins. A real hold (1.1 s,
  `Motion.holdToConfirm`), release winds it back, TalkBack fires it on a
  plain click. Only for delete-a-person, delete-a-recording, forget-a-device,
  clear-the-log, delete-account. Never for Save; calls stay DANGER buttons.
- **A bank header's action is a filled ink square, not a heavier stroke**
  (user asked for a heavier icon stroke; the vectors bake their stroke in,
  so prominence comes from the fill and a 22dp glyph).
- **The corner tag is flush** to the plate's top and right edges (user); the
  tag lives outside the padded column.
- **Watermarks carry a state hue or an accent, never both on one bank**, and
  OFF/UNKNOWN take the hairline. Alpha raised from 0.10 to 0.18 (0.22 at
  night) because at 0.10 they were nearly invisible on the phone (user).
- **One tinted plate per hub** (2.53, user: "don't overuse"): the mains plate
  on Board and Device, the person plate on Circle (single-wearer view; the
  guardian's per-person plates stay untinted), the Protection plate on
  Safety. `Hub` is an enum in the theme and `hubAccent(Hub)` is the one place
  an accent is tied to a name rather than hashed.
- **Help lives on the row** (2.28): `Way(help = …)`, a 16dp glyph after the
  title on a 28dp target that takes only the title line's height, the text
  in a recess under the row, exposed to TalkBack as a custom action.
  `WhyDisclosure` has no callers left in `ui/screens`; it still exists in the
  kit.
- **The editor foot is one scaffold.** `EditorScaffold` measures the foot,
  pads the content by it, and sits above the keyboard. It takes the screen's
  bottom content padding and *not* the navigation-bar inset, because the
  host's bar consumes that and adding it again floated the foot a thumb above
  the bar.
- **Non-adoptions the agents reported and the orchestrator accepted:**
  `HourBars` is used nowhere (no screen holds 24 hourly buckets); `FaceStack`
  is not on trips (trips carry a name, not an avatar id); people are not a
  `CardStrip` (faces, not glyphs); the SIM allowlist is not a `Ledger` (rows
  need a remove action); message threads are not a `Timeline`; the
  seven-pattern light picker stays its own rows; the running-mode card on
  the mode picker keeps its animated scene rather than a watermark; Invite
  keeps its standalone teal Send.
- **The unchosen candidates stay** in the gallery at its foot under their
  original numbers, so a later pick can be made the same way. The gallery now
  opens on the shipped kit with every adopted member live
  (`ui/board/KitGalleryAdopted.kt`).
- **The user's four glyph requests** landed: Lost wears lost-search, the
  Safety ladder (tile, chain stop, master row) wears ladder-stair, the
  Emergency card wears qr-code; and the Profile identity plate lost its
  watermark.

---

## 3. What this session shipped

### 3.1 `4496a25` — the kit

Type.kt (`BoardDisplay`, `BoardMono` = JetBrains Mono), Color.kt (deeper
night, `Hub`, `hubAccent`), Theme.kt, PilotLamp.kt and SyncDot.kt (square),
and six new kit files: `Cards.kt` (TitledPlate, BankHeader, TaggedPlate,
CornerTag, WatermarkPlate, watermarkTint, FooterAction, CardStrip, StripCard),
`Actions.kt` (ActionPair, SplitButton, HoldToConfirm, EditorFootBar,
EditorScaffold, buttonColors), `Explainers.kt` (Callout, Footnote,
QualifierChip, Chain, Steps, Ledger/LedgerLine, Timeline), `Tabs.kt`
(PageTabs, SegmentedChoice), `Rows.kt` (IconDisc, StateWord, NumberedRow,
FaceStack, TileGrid/WayTile), `Instruments.kt` (HourBars). Way gained `help`
and `trailing`; ExpandableSection `preview`; BoardPlate `hub`; BoardButton
`figure` and `glyphOnDisc`; BoardIconButton `caption`; MainsPlate `hub`.
Eight candidate fonts and `CandidateType.kt` deleted; the adopted candidates
and the type candidates removed from `KitCandidates*.kt`. DESIGN.md rewritten.

### 3.2 `a6e07ad` — the exemplars

Medical ID on the pinned foot with a stamped completeness plate, one callout,
a footnote, and preview chips on the closed bank; the Device hub with a tinted
mains plate, Nearby as a row with help, ten destinations as tiles.

### 3.3 `23b4d9a` — every screen

Four Sonnet agents, one per group, against `docs/phase4-adoption-brief.md`.
The commit message lists what changed per hub. Also in that commit: the
screen header's back chevron is a plate press rather than Material's
`IconButton` (its ripple was the last grey flash), watermark alpha, the help
glyph's height, and `SafeShadeIcons` with lost-search.

### 3.4 `d96b072` — the sweep's findings

`SegmentedChoice` never breaks a word (small nameplate first, then no lamp);
Silent SOS's four delays are "10 s / 30 s / 1 min / 5 min"; a `BankHeader`
over a strip or a set of plates takes a plate of its own (`rule = false`);
the ladder-stair and qr-code glyphs; the Profile watermark removed.

### 3.5 The closing commit — after the review

The zones bank is rows at any count again: `StripCard` has no action slot,
so the four-or-more strip had lost guide and delete (a callback the screen
used before must still be used the same way). Coloured watermarks glowed on
the night plate; a lamp-glass or accent tint now draws at 0.09 in dark while
the hairline keeps 0.22 (the user: "WAY too bright" in dark, fine in
light). Two stale comments (the lamp is no longer a circle; the shipped kit
sits above the leftovers, not below).

---

## 4. The kit after v2.8.0

Read `DESIGN.md` **Components** for the rules; this is the map.

| Need | Member | File |
|---|---|---|
| A row | `Way` (+ `help`, `trailing`, `sealed`, `deviceOnly`) | Way.kt |
| A ranked row | `NumberedRow` | Rows.kt |
| Faces on a row | `Way(trailing = { FaceStack(...) })` | Rows.kt |
| A hub's destinations | `TileGrid(listOf(Tile(...)))` | Rows.kt |
| A bank with a name inside | `TitledPlate` | Cards.kt |
| A bank with a count and an add action | `BankHeader` (first child of a `BoardPlate`; `rule = false` when it has a plate of its own) | Cards.kt |
| A stamp on a plate | `TaggedPlate` / `CornerTag` | Cards.kt |
| A plate about one thing | `WatermarkPlate(icon, tint)` with `accentFor` or `watermarkTint(state)` | Cards.kt |
| A plate's one action | `FooterAction` (last child) | Cards.kt |
| Four or more peers that only open | `CardStrip { items { StripCard(...) } }` | Cards.kt |
| The hub's head plate | `BoardPlate(hub = Hub.X)` / `MainsPlate(hub = …)` | Plates.kt, Status.kt |
| Save + Discard | `EditorScaffold(bottomPadding = …, foot = { EditorFootBar(...) }) { footPadding -> … }` | Actions.kt |
| Two actions, one default | `ActionPair` | Actions.kt |
| An action with a variant | `SplitButton` | Actions.kt |
| The irreversible action | `HoldToConfirm("Hold to …")` | Actions.kt |
| A commit that counts | `BoardButton(figure = "3 changes")` | Controls.kt |
| A hub's prominent action | `BoardButton(icon, glyphOnDisc = true)` | Controls.kt |
| A glyph strip with words | `BoardIconButton(caption = …)` | Controls.kt |
| Two to four behaviours | `SegmentedChoice(consequences = …, sealed = …)` | Tabs.kt |
| Two lists on one page | `PageTabs` | Tabs.kt |
| The one thing not to miss | `Callout` (one per page) | Explainers.kt |
| One fact after the rows | `Footnote` | Explainers.kt |
| A qualifier | `QualifierChip` | Explainers.kt |
| What happens, in a glance | `Chain` | Explainers.kt |
| A procedure | `Steps` | Explainers.kt |
| Facts | `Ledger` / `LedgerLine` | Explainers.kt |
| A record with times | `Timeline` | Explainers.kt |
| A closed bank that says what is inside | `ExpandableSection(preview = …)` | Expandable.kt |
| A day of hourly readings | `HourBars` (unused until a screen has 24 buckets) | Instruments.kt |

Everything else (PilotLamp, WaySwitch, Seal, SectionPlate, Hairline, BusTick,
Readout, Gauge, DialControl, TimeStrip, RangeStrip, PlateField, ChipRow,
PersonRow, Avatar, EmptyBay, TripBanner, ProductSilhouette, Waveform) is as
handoff8 left it, save that the lamp is square.

---

## 5. Phase 4 — what remains

Small, and none of it blocks Phase 5.

1. **Dark at 1.3x** was not photographed (only dark at 1.0x and light at
   1.3x). One scripted pass with `sweep3.sh`'s shape.
2. **Nearby services** was not reachable by the scripted sweep (its tile's
   spoken name did not match "nearby services"); open it by hand and check
   the two footnotes.
3. **The Companion role's screens** were not photographed (the phone is in
   the Guardian role). Role → Companion, then the four hubs.
4. **The connected-state first viewport** has still never been photographed
   (handoff8 §8). Needs a paired wearable in range.
5. **Motion**: the hold-to-confirm trace, the lamp warm-up, the trip
   breathing. A screen recording (`adb shell screenrecord`), not a still.
6. **The gallery's leftover candidates** could be culled if the user says
   they will not pick more; until then they cost nothing but file length.
7. **`WhyDisclosure`** has no callers; delete it from `Expandable.kt` and its
   DESIGN.md mention when convenient.
8. **Possible polish seen in the sweep, not acted on:** Evidence's recording
   rows show their length as the state word ("10 S" uppercased); the Trip
   log's Recent/All tabs appear only once there is history older than a week,
   so they have not been seen on the phone; the Lost page's `Mark Found`
   cell is always disabled while the device is not lost, which is right but
   reads as a dead button.

---

## 6. Verified on the device, and not

### Verified this session (Redmi Note 10S, build 2.8.0, off the link, Guardian role)

- The Kit gallery's shipped section, light, 1.0x: every adopted member.
- The four hubs and every subpage reachable from them, light, 1.0x:
  Board; Circle (safe zones, walk with me, check in, guardians, talk, where
  alerts happen, SIM and SMS, messages, people, smart home); Safety (fall
  detection, emergency contacts, if nobody answers, emergency card, evidence,
  silent SOS, trip log, vitals, watch); Device (adaptive mode, device
  settings, lights, find the device, telemetry, reminders, paired devices,
  firmware, lost, ride log); Profile (root, privacy, about, appearance).
- The four hubs, Medical ID and Fall detection in the **dark** theme, 1.0x.
- The four hubs, Medical ID and Fall detection in the light theme at
  **1.3x**, including the channel fix.
- The pinned editor foot flush on the bar with the keyboard down.
- The rewritten back chevron (plate press, not `IconButton`) tapped once on
  Lights: it went back to the Device hub.
- The watermark alpha in dark on Device, Safety and Profile after the drop.
- The check-in deadline dial: the repository takes any whole minute
  (`requestCheckIn(withinMinutes)` computes a deadline), so a dial over the
  old four stops loses nothing.
- `assembleDebug`; the unit tests (all pass).

### Not verified — say this to the user, plainly, every time

- Dark at 1.3x; the Companion role; Nearby services; any connected state;
  the keyboard-up editor (the scripted field tap did not land); the dirty
  state of the foot bar ("Unsaved changes" and amber Save enabled); the hold
  trace in motion; the trip log's tabs; the Reminders' interval dial at
  1.3x; the zones rows with four or more zones (the phone has one).

---

## 7. Known bugs and debt

### Fixed this session

- The screen header's back chevron rippled (Material `IconButton`).
- The Nearby row's title-to-detail gap widened by the help glyph's target.
- The Silent SOS delay channel broke words mid-word; "Medium" at 1.3x too.
- The zones strip and paired devices headers floated without a plate.
- Eleven candidate fonts in the APK (handoff9 first edition, carried debt):
  eight deleted with Azeret; Jakarta and JetBrains Mono are the kit.
- `PlateField`/`OptionWay` in screens; `ComingSoonPlate`; `BOARD_LINK`.

### Carried debt

- **`WhyDisclosure`** is dead code in `Expandable.kt` (§5.7).
- **`StripCard` has no action slot**, so the zones bank is rows at any count
  and `CardStrip` has no caller that needs a second action. Give the card a
  trailing action (or a long-press) before putting zones back on a strip.
- **`HourBars`** is a kit member with no caller (§4).
- **`KitCandidates*.kt`** still carry private copies of `Well`, `RangeGauge`,
  `StatusWell`, `DiscWay`, `NavWay`, `MockBar`, `GroupTitle`, `hubAccent`
  (as `HubMock`) for the unchosen candidates.
- **The Nearby way's word with no link** is "—" (the dash) and its
  explanation is on the row's help; the user's earlier "FAR" concern is
  moot unless they say otherwise.
- **Trips carry no avatar id**, which is why `FaceStack` is unused on them;
  threading one through `TripDetailUiState` is a data change for a later
  phase.
- Everything else in handoff8 §9 and handoff7 §9.

---

## 8. Traps, and the tools that route around them

Handoff8 §10, handoff7 §10 and handoff6 §3 still apply. Added or confirmed
this session:

- **`drive.sh` needs a Windows-style `SP`** and `MSYS_NO_PATHCONV=1` (first
  edition). Sleep 3–4 s after `launch` and 2.5 s after a tab tap.
- **A bash function defined inside a `&&` chain is not visible to the
  rest of that chain** in this tool; the first dark pass ran nothing
  ("hubs: command not found"). Put any multi-step phone script in a file in
  the scratchpad and run `bash file.sh`. Three exist there now: `sweep.sh`
  (a hub and its subpages by spoken name), `sweep2.sh`/`sweep3.sh` (dark and
  1.3x). They die with the scratchpad; copy the shape, not the path.
- **`uiautomator dump` only sees what is on screen.** Opening the Appearance
  section put its rows below the fold, so "Dark, Off" was NOTFOUND until a
  swipe after the tap. The working sequence: Profile, one swipe up, tap
  "appearance", one more swipe, tap "dark" (bare; the row's spoken name is
  "Dark, Off, Always the night panel…" and a bare prefix matches).
- **`tapdesc.py`** (scratchpad) taps the first node whose content-desc or
  text starts with a prefix, case-insensitively; tiles are spoken "Title,
  state word", so the tile's title is the prefix. "messages" matched the
  Messages *section plate* on Circle, not the tile; use "open messages".
- **A python heredoc with an empty body hangs the Bash tool** (first
  edition). Confirmed; never do it.
- **Four agents compiling at once** serialise on the Gradle lock and each
  may see one transient `e:` from another's half-written file. Tell them to
  retry once; do not let them "fix" a file outside their scope.
- **`adb shell settings put system font_scale 1.3`** applies without a
  restart; put it back to `1.0` afterwards or the next session photographs
  the wrong scale.
- **The Write tool refuses a file it has not read this session**, even one
  this session created before a compaction. Read five lines first.
- **The user's mid-turn additions arrive as tool-result trailers.** Four
  arrived this session (icons, watermark alpha, Nearby gap, Profile
  watermark). Read every trailer.

---

## 9. What the user still has to do

Still open from handoff8 §11: the push (now `a648f99`..HEAD, fourteen
v2.8.0 commits on top of the four v2.7.0 ones), the second account, the
eleven-digit contact, the two Vault secrets, placing the widget and the tile,
a `firmware_releases` row, Health Connect on the test phone, a phone with
NFC. Added:

- **Look at the phone.** Build 2.8.0 with everything in §3 is installed. The
  things most worth a human eye: the four hub roots, Medical ID's foot,
  Fall detection's channel, the dark theme.
- **Say whether more candidates are wanted** from the fifty-nine left at the
  foot of the gallery, or whether the gallery can drop them.
- **Any wording that reads wrong.** Every explanatory paragraph in the app
  was cut, or became a callout, a footnote or help on a row; the agents'
  reports list the sentences a user would notice, and this is the sweep
  most likely to have changed a meaning.

---

## 10. Session close

**What shipped** is §3: the kit (§4), the two exemplars, every screen, and
the sweep's fixes. **What was not verified** is §6. The decisions taken
without the user are §2 and are all reversible.

**Context.** This session was compacted once, at the start of the adoption
work; the summary carried the user's full list and refinements, and nothing
was lost that the commits do not hold. It then ran roughly a hundred and
fifty phone screenshots through the model as contact sheets (four to a sheet
at a third scale, which is the cheap way to review a route sweep). The
working state that matters is in the commits, this file, `DESIGN.md`,
`docs/phase4-adoption-brief.md` and the memory directory. The scratchpad
scripts and screenshots are throwaway.

**Continue or hand over.** Hand over. Phase 4's redesign is applied and
photographed; what remains (§5) is an hour of verification gaps and small
polish that a fresh session can do from this file, and the user's own look
at the phone is the input that decides whether anything else changes. The
next session should open by asking whether the user wants the §5 gaps
closed, more candidates adopted, or Phase 5 to begin; if Phase 5, handoff8
§7 is the test list and it starts with a version bump to 2.9.0.
