---
version: 1
slug: "app"
primary_target: "app"
related_targets: []
---

Scope: the whole SafeShade Android app (`app/`), every screen and settings surface.
Visitor mode: operate. Audience: a guardian responsible for someone who wears the device,
and the wearer themselves; India-first, mixed tech comfort, elderly persona real.
Task: know at a glance whether the wearer is covered; act fast when they are not.

## Direction contract

THESIS: Safety is circuits — live, off, tripped. The app is the board showing which.
Refuses the wellness dashboard and the tactical HUD.

OWN-WORLD: Household distribution board. Bone #F2EFE9 / charcoal #22282E, brass hairlines,
engraved nameplates in tracked condensed uppercase. Atomic unit is a Way: nameplate, state,
switch, pilot lamp, seal. Colour only in lamps, seals and trips — teal #6FD3CC live,
amber #F5A623 attention, red trip. No gradients, no glass.

STORY: A guardian knows in one second whether their person is covered. A trip is unmissable
and resettable. A seal shows what the wearer cannot change.

FIRST VIEWPORT: Emblem top-left, settings top-right. Full-bleed mains plate: lamp, protected
person, device, signal, battery. Ways list below, nameplate left, state right. Two gauge tiles.
TEST · RING DEVICE wide and low above the nav bar. Shady beside the plate, arcs pulsing with
the link.

FORM: Distribution Board; candidate 5 of 7 grounded; seed 3db1c433.

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance.

## Raises

- From Ikeda Datamatics (declined): red and amber only ever mean live alarm state; the body of
  every screen is achromatic.
- From Miura deployable sheet (declined): a mode change re-lays the whole board at once.
- From Iridescent cloud edge (declined): colour confined to lamp glass and rule edges.
- From Zoo guide map (competitive, held the audience axis): flat unmodulated colour, no
  gradients anywhere; the switch chip crosses to the leading edge when a way goes live.

## Unresolved

- Per-mode accent colours currently in data/Models.kt must become derived tints of the brand
  triad in ui/theme (Phase 1), not seven competing hues.
- CMD_FIND is built app-side as a low-key locate; firmware follow-up owed.
