# Phase 4 adoption brief (v2.8.0)

The brief every screen sub-agent works from. Written so it survives a context
reset: it says what the kit now offers, what each old pattern becomes, what is
forbidden, and how to report. Read it whole before touching a file.

## 1. What you are doing

The kit (`app/src/main/java/com/safeshade/ui/board/`) gained thirty-three
members in v2.8.0, chosen by the user from a hundred and one candidates. Your
job is to apply them to a fixed set of screens so every page reads as the same
system as the two exemplars, and to use the licence the user gave for
"larger reorganisations, rethinking, wording changes, larger UX changes"
where a page is better for it. The bar is "award-winning worthy". A page that
merely swaps components without getting shorter, clearer or calmer is not done.

Read first, in this order:

1. `DESIGN.md` sections **Colors → Named Rules**, **Typography**, **Components**
   (Buttons through Instruments), **Do's and Don'ts**, and the appendix
   **v2.0 candidates → Adopted in v2.8.0**.
2. The kit files: `Cards.kt`, `Actions.kt`, `Explainers.kt`, `Tabs.kt`,
   `Rows.kt`, `Instruments.kt`, plus the changed signatures in `Way.kt`
   (`help`, `trailing`), `Expandable.kt` (`preview`), `Plates.kt`
   (`BoardPlate(hub = …)`), `Controls.kt` (`figure`, `glyphOnDisc`, `caption`),
   `Status.kt` (`MainsPlate(hub = …)`).
3. The two exemplars: `ui/screens/safety/MedicalIdScreen.kt` (an editor) and
   `ui/screens/device/DeviceScreen.kt` (a hub).
4. `ui/board/KitGalleryAdopted.kt`, which shows every new member called with
   real arguments.

## 2. The mapping: old pattern → new member

| You find | It becomes |
|---|---|
| A paragraph of body text above a bank explaining the page | Cut, or one `Callout` if it states a consequence a person must not miss (one per page), or a `Footnote` under the bank if it is one fact read after the rows |
| `WhyDisclosure` beside a row or a field | `Way(help = …)` on the row it explains. If it explains a field, a `Footnote` under the field. `WhyDisclosure` should not survive in your files |
| `Note(text)` (SafetyCommon) under a control | A `Footnote`, or nothing if the label already says it |
| Two to four `OptionWay`s choosing one *behaviour* | `SegmentedChoice(options, selected, onSelect, consequences = …, sealed = …)` with one consequence line per option |
| Two lists stacked on one page (recent/all, mine/others, this week/older) | `PageTabs` over one bank |
| A `SectionPlate` over a plate that has a trailing icon button, or a bank with a count | `BankHeader(title, count, actionIcon, actionDescription, onAction)` as the plate's first child; drop the `SectionPlate` |
| A `SectionPlate` over a single plate that is one named thing (a recording list, a wearable's facts) | `TitledPlate(title) { … }` |
| A hub's list of rows that all navigate somewhere | `TileGrid(tiles)`; rows that *report* or carry a switch stay `Way`s in a plate above the grid |
| A sequence: the ladder's rungs, onboarding steps, setup instructions | `NumberedRow`s in a plate for a ranked list; `Steps` for a procedure with done/current/next; `Timeline` for a record with times |
| A trip's record, a sync history, a message thread's events | `Timeline(stops)` |
| A block of "key: value" facts in prose or in Ways with no state | `Ledger(rows)` with `mono = true` on quantities and `state` where a value reports one |
| A sentence describing what happens after a fall/SOS | `Chain(stops)` at the top of the page |
| A qualifier sentence under a row ("needs a SIM", "device-only", "Plus only", "sign in first") | `QualifierChip` beside the title or `TaggedPlate`/`CornerTag` on a plate; the sentence goes |
| A plate whose one action is a `BoardButton` inside it | `FooterAction(label, icon, onClick)` as the plate's last child |
| A card about one identity (a zone, a journey, a person, a wearable) | `WatermarkPlate(icon, tint)` — tint = `colors.accentFor(name)` for a thing, `watermarkTint(state)` for a circuit. At most one per bank |
| A bank of four or more similar peers (zones, wearables, people) where the page is long | `CardStrip { items(...) { StripCard(...) } }` |
| The ring/who-answered/who-was-told sentence on a trip or message row | `Way(trailing = { FaceStack(faces) })` |
| Two `BoardButton`s at the foot of a page (commit + cancel/secondary) | `ActionPair` |
| A `BoardButton` with a variant beside it or a "…more" | `SplitButton` |
| A `BoardButton(weight = DANGER)` that deletes, removes, forgets or signs out | `HoldToConfirm(label = "Hold to …")`. A call button stays a DANGER `BoardButton`: calls are urgent, deletions are not |
| An editor (fields that end in Save) | `EditorScaffold(bottomPadding = contentPadding.calculateBottomPadding(), foot = { EditorFootBar(...) }) { footPadding -> scrolling content padded by footPadding }`; the Save button at the end of the content goes |
| A commit button that commits a countable thing | `BoardButton(figure = "3 changes")` |
| The one or two prominent actions on a hub root | `BoardButton(icon, glyphOnDisc = true)` |
| A strip of icon-only buttons whose glyphs are not universal | `BoardIconButton(caption = …)` |
| The head plate of a hub root (Board, Circle, Safety) | `MainsPlate(hub = Hub.X)` or `BoardPlate(hub = Hub.X)`; the only tinted plate on that hub |
| A day of readings shown as a list (Vitals history, ride log, telemetry) | `HourBars` |
| `ExpandableSection` whose rows have state words | add `preview = listOf(word to state, …)` |
| `PilotLamp` anywhere | leave it; it is square now by itself |

## 3. Wording

- Every sentence that survives must state a consequence, a cost or a
  limitation the reader could not have assumed. "Turn this on to enable X"
  fails; "Off, the wearable stays silent through a fall" passes.
- Row titles sentence case. Prominent buttons Title Case. State words are
  short: a word or a figure, never a sentence.
- Nothing says planned, simulated, representative, coming soon, demo, mock.
  Absent data is a dash. No tick or "Saved" before a result is known.
- No em dashes in prose; use an en dash or a full stop.

## 4. Hard rules

- **Scope.** Edit only the files listed in your task. Read anything. Never
  edit `ui/board/*`, `ui/theme/*`, `ui/nav/*`, `DESIGN.md`, `CLAUDE.md`,
  `PRODUCT.md`, anything under `docs/`, `supabase/`, or `docs/SafeShadev21/`.
  If the kit lacks something you need, do the nearest thing with what exists
  and say so in your report; do not add a helper that draws a card, button,
  switch or row.
- **No git commands that write** (no add, commit, stash, checkout, reset,
  restore). `git diff` and `git status` are fine.
- **Behaviour is unchanged.** Every callback, route, repository call and
  state field a screen used before must still be used the same way. You are
  changing what is drawn and what is said, not what happens.
- **Data honesty.** Do not invent a value to fill a new component. A
  `Ledger` row with no value shows "—". A `Timeline` only shows events the
  state actually carries. A `HourBars` needs 24 real hourly buckets; if the
  data is not bucketed by hour, do not use it.
- **One tinted plate per hub, one watermark per bank, one callout per page,
  one hold per page.**
- **Previews.** Keep every `@Preview` compiling; update preview state to
  exercise the new members (a dirty editor, a filled preview list).
- **Compile.** Run `./gradlew.bat compileDebugKotlin -q` at most three
  times, at the end. Another agent may hold the Gradle lock; wait, do not
  kill it. Fix every `e:` in your own files. If an error is in a file
  outside your scope, report it and stop.
- **Do not run adb, do not install, do not run tests.** The orchestrator
  photographs and tests.

## 5. Report format

Return, in this order, and nothing else:

1. Files changed, one line each: what the page was, what it is now, and
   which members it uses.
2. Wording you changed that a user would notice (old → new), briefly.
3. Anything you could not do and why (a missing kit member, data not shaped
   for the component, a behaviour you did not dare touch).
4. The compile result: clean, or the exact `e:` lines you could not fix.
