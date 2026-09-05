---
name: SafeShade
description: A household distribution board for personal safety — every circuit reads live, off, or tripped at a glance.
colors:
  brand-charcoal: "#22282E"
  brand-teal: "#6FD3CC"
  brand-amber: "#F5A623"
  bone-ground: "#F2EFE9"
  bone-plate: "#FBF9F5"
  bone-recess: "#E5E1D8"
  brass-rule-light: "#B9A88A"
  hairline-light: "#D3CDC0"
  ink-light: "#22282E"
  ink-muted-light: "#55514B"
  ink-faint-light: "#69655F"
  night-ground: "#14171A"
  night-plate: "#22282E"
  night-recess: "#0E1113"
  brass-rule-dark: "#8A7A5E"
  hairline-dark: "#353C43"
  ink-dark: "#ECEFF1"
  ink-muted-dark: "#B4AEA4"
  ink-faint-dark: "#948F86"
  lamp-live-glass: "#6FD3CC"
  lamp-attention-glass: "#F5A623"
  lamp-trip-glass: "#E5484D"
  lamp-off-light: "#C8C2B4"
  lamp-off-dark: "#31383E"
  ink-live-light: "#0F7A72"
  ink-live-dark: "#7EDCD5"
  ink-attention-light: "#8A5300"
  ink-attention-dark: "#FFC46B"
  ink-trip-light: "#B3151A"
  ink-trip-dark: "#FF8A8D"
typography:
  display:
    fontFamily: "Archivo Variable (wdth 100)"
    fontSize: "40sp"
    fontWeight: 700
    lineHeight: "46sp"
    letterSpacing: "-0.02em"
  headline:
    fontFamily: "Archivo Variable (wdth 100)"
    fontSize: "19sp"
    fontWeight: 600
    lineHeight: "26sp"
    letterSpacing: "normal"
  title:
    fontFamily: "Archivo Variable (wdth 100)"
    fontSize: "17sp"
    fontWeight: 600
    lineHeight: "24sp"
    letterSpacing: "normal"
  body:
    fontFamily: "Archivo Variable (wdth 100)"
    fontSize: "16sp"
    fontWeight: 400
    lineHeight: "24sp"
    letterSpacing: "normal"
  label:
    fontFamily: "Archivo Variable (wdth 78)"
    fontSize: "13sp"
    fontWeight: 600
    lineHeight: "18sp"
    letterSpacing: "0.12em"
  nameplate:
    fontFamily: "Archivo Variable (wdth 78)"
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
    letterSpacing: "0.12em"
  nameplate-small:
    fontFamily: "Archivo Variable (wdth 78)"
    fontSize: "12sp"
    fontWeight: 600
    lineHeight: "16sp"
    letterSpacing: "0.14em"
  section-plate:
    fontFamily: "Archivo Variable (wdth 78)"
    fontSize: "13sp"
    fontWeight: 700
    lineHeight: "18sp"
    letterSpacing: "0.18em"
  state-label:
    fontFamily: "Archivo Variable (wdth 78)"
    fontSize: "13sp"
    fontWeight: 700
    lineHeight: "16sp"
    letterSpacing: "0.10em"
  readout:
    fontFamily: "Azeret Mono Variable"
    fontSize: "14sp"
    fontWeight: 500
    lineHeight: "20sp"
    letterSpacing: "-0.01em"
  readout-large:
    fontFamily: "Azeret Mono Variable"
    fontSize: "34sp"
    fontWeight: 500
    lineHeight: "38sp"
    letterSpacing: "-0.03em"
  countdown:
    fontFamily: "Azeret Mono Variable"
    fontSize: "64sp"
    fontWeight: 700
    lineHeight: "68sp"
    letterSpacing: "-0.04em"
rounded:
  tight: "2dp"
  plate: "4dp"
  card: "8dp"
  prominent: "14dp"
spacing:
  xxs: "2dp"
  xs: "4dp"
  sm: "8dp"
  md: "12dp"
  lg: "16dp"
  xl: "24dp"
  xxl: "32dp"
  gutter: "20dp"
  touch-target: "48dp"
components:
  board-plate:
    backgroundColor: "{colors.bone-plate}"
    rounded: "{rounded.card}"
  board-plate-recessed:
    backgroundColor: "{colors.bone-recess}"
    rounded: "{rounded.card}"
  way-row:
    backgroundColor: "{colors.bone-plate}"
    textColor: "{colors.ink-light}"
    typography: "{typography.nameplate}"
    padding: "12dp 16dp"
    height: "48dp"
  way-state-live:
    textColor: "{colors.ink-live-light}"
    typography: "{typography.state-label}"
  way-state-attention:
    textColor: "{colors.ink-attention-light}"
    typography: "{typography.state-label}"
  way-state-trip:
    textColor: "{colors.ink-trip-light}"
    typography: "{typography.state-label}"
  way-state-off:
    textColor: "{colors.ink-faint-light}"
    typography: "{typography.state-label}"
  way-switch-on:
    backgroundColor: "{colors.lamp-live-glass}"
    rounded: "{rounded.tight}"
    width: "52dp"
    height: "30dp"
  way-switch-off:
    backgroundColor: "{colors.lamp-off-light}"
    rounded: "{rounded.tight}"
    width: "52dp"
    height: "30dp"
  button-primary:
    backgroundColor: "{colors.ink-light}"
    textColor: "{colors.bone-plate}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-secondary:
    backgroundColor: "{colors.bone-plate}"
    textColor: "{colors.ink-light}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-attention:
    backgroundColor: "{colors.lamp-attention-glass}"
    textColor: "{colors.brand-charcoal}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-commit:
    backgroundColor: "{colors.lamp-live-glass}"
    textColor: "{colors.brand-charcoal}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-quiet:
    backgroundColor: "{colors.bone-ground}"
    textColor: "{colors.ink-light}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-danger:
    backgroundColor: "{colors.lamp-trip-glass}"
    textColor: "{colors.bone-plate}"
    typography: "{typography.nameplate}"
    rounded: "{rounded.plate}"
    padding: "12dp 16dp"
    height: "56dp"
  button-disabled:
    backgroundColor: "{colors.bone-recess}"
    textColor: "{colors.ink-faint-light}"
    rounded: "{rounded.plate}"
  plate-field:
    backgroundColor: "{colors.bone-recess}"
    textColor: "{colors.ink-light}"
    typography: "{typography.body}"
    rounded: "{rounded.plate}"
    padding: "8dp 12dp"
    height: "48dp"
  seal:
    backgroundColor: "{colors.brass-rule-light}"
    textColor: "{colors.ink-muted-light}"
    typography: "{typography.nameplate-small}"
    rounded: "{rounded.tight}"
    padding: "1dp 5dp"
  slider-track:
    backgroundColor: "{colors.bone-recess}"
    rounded: "{rounded.tight}"
    height: "8dp"
  slider-track-active:
    backgroundColor: "{colors.ink-light}"
    rounded: "{rounded.tight}"
    height: "8dp"
  slider-thumb:
    backgroundColor: "{colors.ink-light}"
    rounded: "{rounded.tight}"
    width: "8dp"
    height: "28dp"
  snackbar:
    backgroundColor: "{colors.night-plate}"
    textColor: "{colors.bone-plate}"
    rounded: "{rounded.plate}"
  section-plate:
    textColor: "{colors.ink-muted-light}"
    typography: "{typography.section-plate}"
  nav-item-selected:
    backgroundColor: "{colors.brass-rule-light}"
    textColor: "{colors.ink-light}"
    typography: "{typography.nameplate-small}"
  nav-item-unselected:
    textColor: "{colors.ink-faint-light}"
    typography: "{typography.nameplate-small}"
---

# Design System: SafeShade

## Overview

**Creative North Star: "The Household Distribution Board"**

SafeShade is a consumer panel, not a wellness dashboard and not a tactical HUD. The thesis is that safety is circuits — live, off, tripped — and the app is the board that shows which. Everything a guardian needs to know is expressed the way a real board expresses it: an engraved nameplate on the left, a state on the right, a pilot lamp that either burns or does not, and a brass rule dividing one bank of circuits from the next. The material is bone-coloured sheet metal in a lit room and dark plate in a hallway at night; the corners are milled, not moulded; nothing glows, blurs, or floats.

The density is deliberately generous. Type sits one step larger than a typical phone scale because the guardian of an elderly wearer is often elderly too, and this app gets read in a hurry. Every row — tappable or not — clears a 48dp floor. The board is achromatic almost everywhere, which is what gives the three state hues their force: when something on this screen is coloured, a reader is entitled to assume it is telling them about a live circuit.

Confirmed rejections, all visible in the build: frosted glass and blur (replaced by flat plates with hairline borders), gradients of any kind (the pilot lamp's halo is a flat low-alpha ring, and a local flat interpolation exists specifically so no caller reaches for a gradient brush), 20–28dp pill corners, Material You dynamic colour, per-mode accent hues, and any decorative accent token at all.

**Key Characteristics:**
- Colour means circuit state and nothing else
- Two grounds, one system: bone panel by day, night plate after dark
- Machined corners (2–8dp in practice, 14dp ceiling)
- Separation by hairline and brass rule, never by shadow
- One variable type family at two width axes, plus a mono for readouts
- Mechanical motion: switches throw, lamps warm up
- Honest by construction: sealed, device-only, and unknown are all first-class states

## Colors

A bone-and-charcoal panel carrying two colour families, separated by **saturation, not hue**.

Three *saturated* hues report the condition of a circuit and nothing else. Six *desaturated* accents carry identity — which area of the app a row belongs to, which adaptive mode a card is, what an illustration is made of. At 22% saturation against state's 55–75%, a saturated pixel always means state.

Saturation is the separator rather than a reserved set of hues because it survives a colourblind reader: two greens differing only in hue are one colour to a deuteranope, while a vivid green and a dusty one remain two things.

### Primary

- **Brand Charcoal** (`{colors.brand-charcoal}`): The emblem's shield body, and the light theme's primary ink. It is also what Material's `primary` role resolves to — a filled button in this world is an engraved plate, not a coloured pill, so the Material role deliberately carries no brand hue. That mapping is what stops a stray stock `Button` from breaking the colour rule.

### Secondary — state hues

Each state exists twice: a **lamp-glass** colour, chosen to look right when lit and used for fills only, and a contrast-safe **ink** colour used whenever text carries the same meaning. Glass is theme-invariant — a lit lamp is the same object in both themes — while the inks resolve per theme so they pass on both grounds.

- **Live Teal** (`{colors.lamp-live-glass}` glass; `{colors.ink-live-light}` / `{colors.ink-live-dark}` ink): the emblem's signal arcs. Working and armed. Also the thrown chip of a rocker switch.
- **Attention Amber** (`{colors.lamp-attention-glass}` glass; `{colors.ink-attention-light}` / `{colors.ink-attention-dark}` ink): the emblem's sun dot. On, but degraded, unconfirmed, or nearly out.
- **Trip Red** (`{colors.lamp-trip-glass}` glass; `{colors.ink-trip-light}` / `{colors.ink-trip-dark}` ink): something happened and it needs a person. The only hue a button is ever allowed to carry, and only for actions that place a real call or fire a real alert.
- **Unlit Stone** (`{colors.lamp-off-light}` / `{colors.lamp-off-dark}`): a lamp that is not lit. Used for both OFF and UNKNOWN — neither is a fault.

### Decorative — identity, never status

**Twelve** peers rather than a scale, each solved for rather than picked: at a fixed low saturation, lightness was walked until the value cleared 4.5:1 against **both** its theme's plate and its ground. That is the body-text floor, well past the 3:1 a 20dp icon or a 2dp rule needs.

Measured, not estimated — the current set, re-checked against the source:

| | worst | best | peer spread |
|---|---|---|---|
| light | 4.50:1 | 4.54:1 | 1.01:1 |
| dark | 4.50:1 | 4.60:1 | 1.02:1 |

The spread is the point: twelve different things at one weight, not twelve steps of emphasis. Hues are spaced at least 22° apart so no two read as a retune of each other.

- **accentSage**, **accentSky**, **accentLilac**, **accentClay**, **accentSand**, **accentMoss** — the original six.
- **accentOchre**, **accentFern**, **accentCove**, **accentSlate**, **accentPlum**, **accentRose** — added when six ran out.

Used at full strength for icons, section rules and identity type; at 0.10–0.18 alpha as a wash behind a card, where the normal ink tokens still apply on top. There is deliberately no `onAccent` token — a wash strong enough to need one would be mistaken for a state fill.

**Assignment is by stable hash, not by hand and not ambiently.** `accentFor(key)` derives a row's colour from its own identity. `LocalBoardAccent` is gone: one ambient accent per destination meant a screen was one colour, which is the thing the twelve exist to stop. `String.hashCode` is specified by the language, so a row's colour is arbitrary-looking but never *changes* between launches.

**What keeps this safe is saturation, not hue.** State colours are saturated and reserved; decoration is not. That rule — enforced in how the tokens are built rather than by convention — is what makes "assigned freely" something other than a licence to confuse decoration with status.

### Neutral

- **Bone Ground / Night Ground** (`{colors.bone-ground}` / `{colors.night-ground}`): the panel behind everything.
- **Bone Plate / Night Plate** (`{colors.bone-plate}` / `{colors.night-plate}`): a raised plate — a way row, a card, a section body, the bottom bar.
- **Bone Recess / Night Recess** (`{colors.bone-recess}` / `{colors.night-recess}`): a routed channel. Text inputs, switch tracks, wells — anything set *into* the panel rather than onto it.
- **Hairline** (`{colors.hairline-light}` / `{colors.hairline-dark}`): the structural separator between ways and the border of every plate.
- **Brass Rule** (`{colors.brass-rule-light}` / `{colors.brass-rule-dark}`): emphasis under a section nameplate, the seal's backing tint, and the bottom bar's selection plate. The one ornament the system allows, and it earns its place structurally.
- **Ink / Ink Muted / Ink Faint** (`{colors.ink-light}` / `{colors.ink-muted-light}` / `{colors.ink-faint-light}`, and the dark counterparts): primary copy, supporting copy, and captions or inert state words.

### Named Rules

**The Colour-Means-State Rule.** Teal, amber and red appear in pilot lamps, bus ticks, seals, thrown switch chips, trip banners and state words. Grounds, plates, rules and body type are achromatic. Audit test: point at any saturated pixel and name what it reports; if you cannot, it does not belong.

The rule was once "colour means circuit state and nothing else", and it is now one step weaker on purpose. A hued **action** is permitted where a screen has exactly one thing worth doing and every plate on it looked identical — an amber Connect, a teal Save, a red Emergency numbers. Three exceptions exist, all of them buttons, all of them stating their action in words as well. The saturation rule still separates these from the twelve decorative accents, and nothing else may take a hue on this argument: a hued row, a hued heading or a hued icon that is not reporting something is still wrong.

Two glyphs carry a state hue while reporting nothing: the back chevron (amber) and the Circle screen's location-refresh control (red). Both were asked for, both take an **ink** token rather than a lamp glass, and both are listed here so that finding one in the source is not mistaken for a licence.

**The Two-Form Rule.** Every state token ships as a lamp glass and as an ink. Glass fills, ink speaks. Never set text in a glass colour, and never fill a lamp with an ink colour.

This is what settles every "make it amber" request. Brand amber on the bone panel measures about 1.9:1, which is not a contrast a glyph or a word can be drawn at; `inkAttention` is the same hue family darkened for exactly this and clears 7:1 in both themes. A hued *button* is the mirror case: the glass is the fill and charcoal is the only ink that measures on it, in both themes, because the glasses do not darken for the night panel.

**The No-Dynamic-Colour Rule.** Material You is declined outright. Dynamic colour would repaint the pilot lamps from the user's wallpaper, which destroys the one rule the system is built on. Android's guidance to prefer dynamic colour assumes hue is decorative; here it is load-bearing.

**The Colour-Is-Never-Alone Rule.** Every lamp is paired with a state word, and every lamp carries a spoken state description. Red-versus-teal is exactly the pairing colour vision deficiency costs, and the system survives greyscale because the word is always there.

**The Semantic-Access Rule.** Screens read colours through the theme's semantic accessor, never through the raw constants. That is how a light-only value ends up hardcoded into a dark screen.

## Typography

**Display / Body Font:** Archivo Variable at width axis 100 (bundled TTF)
**Nameplate Font:** Archivo Variable at width axis 78 — the same file, condensed
**Readout Font:** Azeret Mono Variable (bundled TTF)

**Character:** One workhorse grotesk doing two jobs. At normal width Archivo is plain, legible body copy; pulled to width 78 and set uppercase with heavy tracking it becomes the engraved label-plate voice of the board. Because both registers come from one file there is no pairing mismatch to manage. Azeret Mono handles anything a person reads digit by digit — telemetry, coordinates, signal strength, timers — so a changing battery percentage does not jitter the label beside it.

Both families are bundled as `.ttf` rather than pulled through Downloadable Fonts, so screenshot verification is deterministic.

### Hierarchy

- **Display** (700, 40 / 32 / 28sp, tight negative tracking): rare, for full-frame moments.
- **Headline** (600–700, 32 / 22 / 19sp): screen titles and the mains plate headline. The largest step is the Board masthead alone, sized against a 52dp emblem rather than against a paragraph.
- **Title** (600, 20 / 17 / 15sp): plate headings inside a card.
- **Body** (400, 16 / 15 / 13sp): explanatory copy, used heavily and on purpose — every switch on a safety screen changes what happens to a person, and a toggle whose consequence is not spelled out is a toggle nobody dares touch.
- **Nameplate** (600, 15sp, +0.12em, uppercase): the engraved circuit label. Uppercasing happens inside the component, not at the call site.
- **Nameplate Small** (600, 12sp, +0.14em, uppercase): sub-rows, inline labels, seals, nav labels.
- **Section Plate** (700, 13sp, +0.18em, uppercase): a bank heading sitting above a brass rule.
- **State Label** (700, 13sp, +0.10em, uppercase, end-aligned): LIVE / OFF / TRIPPED / SEALED on the right of a way.
- **Readout** (500 mono, 14sp): instrument values. **Readout Large** (500 mono, 34sp): the one big number on a plate or gauge. **Countdown** (700 mono, 64sp): trip-banner digits.

### Named Rules

**The One-Family-Two-Widths Rule.** Nameplates are Archivo condensed; body is Archivo normal. Do not introduce a second display face to get a condensed look, and do not fake condensation with letter-spacing on the normal width.

**The Engraving Rule.** Uppercase plus tracking is reserved for **section plates and state words**, and for nothing else. Caps mean something here precisely because they are rare.

Row titles were uppercase condensed through v2.0 and are now sentence case in the body voice at zero tracking. Caps on every row read as shouting across a whole app, which is a real cost on a product people open when they are worried. The one survivor of the old micro-caps voice is the seal on a guardian-managed way, because a seal is a stamped physical object and reads as one.

**Prominent buttons take Title Case.** A full-width button that is the point of its screen — Connect to the Device, Save and Send to the Device, Pair Another Device, Emergency Numbers — is a named action rather than a sentence, and title case is what separates it from the row titles around it. Title case in the ordinary sense: principal words capitalised, articles and short prepositions left alone unless they lead. Row titles, section headings and small inline buttons stay sentence case.

**An em dash in prose is an en dash.** The long dash was set throughout and reads as a gap in a column of 15sp type. The exception is the standalone dash a gauge shows where it has no reading, which is a glyph standing in for a value and not punctuation at all.

**The Digits-Are-Mono Rule.** Any number a person reads one character at a time is set in Azeret Mono. Prose containing a number is not.

**The One-Step-Larger Rule.** The scale sits one step above a typical phone scale, and every field and row grows with the system font scale rather than clipping — heights are floors, never fixed.

## Layout

A single-column, scrolling panel. Content sits inside a 20dp screen gutter with a 16dp rhythm between plates; inside a plate the padding is 16dp and internal stacks step on 8dp and 12dp. The spacing scale is 4dp-based (2 / 4 / 8 / 12 / 16 / 24 / 32) with a 20dp gutter and a 48dp touch target as named constants, and the observed distribution is heavily weighted to 8, 16 and 12 — the larger steps appear only between banks.

The reference composition, set by the Board screen and followed by every other screen, is: emblem and screen identity top-left with settings top-right; then the full-bleed mains plate answering the only question that matters on opening the app — who is protected, is the link live, how much life is left — as a lamp, a headline, a subline, and an instrument strip below a hairline; then the ways, nameplate left and state right; then gauge tiles; then a wide, low action pair above the nav bar. Everything below the mains plate is elaboration; a user who reads only the top of the screen has still got what they came for.

Pushed screens open with a back-arrow header carrying a title and optional subtitle; the hub screens, which are bottom-bar destinations, carry the same header without the arrow. The instrument strip is rendered only when there is something real to report — dashes for battery and signal while disconnected are noise the lamp has already covered.

The app draws edge-to-edge with transparent system bars, and system-bar icon appearance is set per theme so light icons never land on the bone ground.

### Named Rules

**The 48dp Floor Rule.** Every interactive target clears 48dp — and so do non-interactive rows, because a row read at arm's length needs the same vertical room. A field's recess is drawn inside the text field's own decoration, so the full plate is tappable rather than a hairline-thin target.

**The First-Viewport Rule.** The state of the person being protected is above the fold on every screen that has one. Nothing decorative is allowed to push it down.

## Elevation & Depth

**This system has no shadows.** Not "few" — the built app contains no shadow or elevation call sites at all, and the bottom navigation bar explicitly sets its tonal elevation to zero. A `Elevation` scale exists in the theme file but has no call sites; it is not part of the shipped system and should not be treated as one.

Depth is expressed the way a real panel expresses it, in three registers:

- **Ground → plate**: a tonal step upward plus a 1dp hairline border and a 4–8dp milled corner. A plate sits *on* the panel.
- **Ground → recess**: a tonal step downward plus the same hairline. Inputs, switch tracks and wells are routed *into* the panel.
- **Rules**: a 1dp hairline is a structural separator; a 2dp brass rule under a section plate is emphasis. Stroke weights run hairline 1dp, rule 1.5dp, brass 2dp, heavy 3dp (the bus tick).

### Named Rules

**The Nothing-Floats Rule.** Surfaces separate by tone, hairline and rule. The bottom bar is divided from content by a rule, not a shadow. If a surface needs to feel raised, step its tone and draw its border; do not lift it.

**The No-Emitted-Light Rule.** This panel does not emit light, so nothing on it glows. The pilot lamp's halo is a flat low-alpha ring, not a radial gradient, and buttons respond to a press with a 0.985 scale-down rather than a ripple — a ripple spreads light across a surface that has none.

## Shapes

Corners are machined, not soft. The scale is 2dp (tight — switch tracks, chips, seals, bus ticks), 4dp (plate — buttons and text fields), 8dp (card — plates, gauges, sheets) and a 14dp ceiling. Observed usage never exceeds 8dp outside the Material shape mapping; the 20–28dp pill corners of a consumer wellness app read as the wrong material entirely. The one circular form in the system is the pilot lamp, which is a circle by definition: flat glass disc, a bezel ring always drawn so an unlit lamp still reads as a lamp rather than a smudge, and a flat halo when lit.

Silhouettes are rectangular and wide. Buttons are square-shouldered plates, wide and low, minimum 56dp tall. Switch tracks are 52×30dp rectangles with a 22dp rectangular chip. Nothing in the system is a pill.

## Components

### Buttons

- **Shape:** milled 4dp corners, minimum height 56dp, label uppercase in the nameplate voice with an optional 18dp leading icon and an optional supporting line beneath.
- **Primary:** ink container, plate-coloured label — an engraved plate, not a coloured pill.
- **Secondary:** plate container with a hairline border and ink label. **Quiet:** ground container with a hairline border.
- **Attention:** amber container, charcoal label. The one thing on this screen to do next, where there is exactly one — connecting a wearable, pairing a second.
- **Commit:** teal container, charcoal label. This writes something down. Saving was previously indistinguishable from navigating.
- **Danger:** trip-red container. Actions that place a real call or fire a real alert.
- All three hued weights carry **charcoal** ink, which is not a style choice: the three glasses are light hues that do not darken for the night panel, so charcoal is the only pairing that measures in both themes.
- **Disabled:** recess container, faint ink.
- **Press:** a 0.985 scale-down over 120ms with the ripple indication removed.

### Way (signature component)

The atomic row the whole interface is built from: a bus tick, an optional icon, an engraved nameplate with optional detail line, and on the right either a state word paired with a pilot lamp or a rocker switch. "Is fall detection on", "is the link up", and "has something tripped" are the same shape of question, so they get the same shape of answer and a guardian learns to read the whole app once.

- **Bus tick:** a 3dp × 28dp vertical mark at the leading edge, tinted by state — it reads as the busbar the circuit taps off.
- **Semantics:** the entire row is one node, spoken as "name, state, detail" plus its sealed or device-only qualifier. Announcing nameplate, state and switch separately reads three fragments where a sighted user takes in one line.
- **Sealed:** a brass-tinted SEALED chip beside the nameplate, marking a way the wearer cannot change on the device itself.
- **Device-only:** the same row with no control at all, plus the location of the setting on the wearable. A switch that silently does nothing is worse than no switch.
- **Selection:** a mutually exclusive choice renders as a way that is LIVE / "In use" or OFF / "Not chosen" — never "Off", which would read as the feature being disabled.

### Pilot Lamp (signature component)

Five states and no more — LIVE, ATTENTION, TRIP, OFF, UNKNOWN. A lamp that can say six things says nothing at a glance. Drawn rather than assembled from stock parts, so the whole app has one auditable implementation of the only place saturated colour is allowed. It lights with a filament warm-up rather than a cross-fade, and dies faster than it lights. A TRIP lamp breathes on a 720ms reversing cycle; nothing else on the board moves on its own, which is what makes it the thing that draws the eye across a still screen. Animation is held steady under inspection so captured evidence is deterministic.

### Rocker Switch

Not a Material `Switch`. A 52×30dp recessed track with a hairline border and a 22dp rectangular chip that crosses to the **leading** edge when live, so a bank of switches reads as a row of thrown levers without relying on colour. The chip is lit teal when thrown and unlit stone when not — the same on/off vocabulary as a pilot lamp. The touch target is the full 48dp even though the track is 30dp.

### Plates and Fields

- **Plate:** plate-coloured fill, 1dp hairline border, 8dp corner, 16dp internal padding. Recessed variant swaps the fill for the recess tone.
- **Section plate:** an uppercase tracked heading in muted ink over a 2dp brass rule. This is how a long settings screen reads as labelled banks rather than an undifferentiated list.
- **Text field:** built directly on a basic text field rather than Material's, drawn as a routed channel — recess fill, hairline border, 4dp corner, 48dp minimum height that grows with the font scale. Placeholder in faint ink and hidden from the semantics tree. A character counter appears only once the value reaches three-quarters of its cap; "0 / 40" under an empty field reads as a demand.
- **Gauge:** a labelled plate, not a dial. A small muted nameplate, a large mono value with an optional unit and caption. A dial looks handsome and is slower to read.

### Rich controls

A setting that is genuinely continuous gets **`DialControl`**: a large `Readout` in real units, a track stepped to a value the underlying system can actually honour, and a line of plain English that changes as it moves. The pattern comes from the zone-radius slider, the one control in the app that was already right; its strength was never the slider but the pairing. `onCommit` fires once on release, which anything writing over BLE must use — one outstanding GATT operation at a time means a write per frame drops the last value.

A time of day gets **`TimeStrip`**, and a period gets **`RangeStrip`**: the whole day drawn as a strip, shaded through night, so 7am and 7pm are told apart by where the marker sits rather than by reading am/pm. `RangeStrip` wraps past midnight, because for quiet hours an end earlier than its start is the normal case rather than an invalid range.

The slider itself is drawn in this panel's material and not Material's. Stock `Slider` was the last piece of another design system on screen — a wide pill thumb with three dots milled into it, a lavender inactive track from a colour role nothing else here uses, tick pips in a fourth tone, and a stop-indicator dot parked at the far end that reads as a value sitting there. It is a **square-shouldered slug seated in a channel**: 8 by 28dp at the tight radius, an ink active track, and the same recess every text field is routed into. A round thumb reads as a bead on a wire; this panel has no beads.

**Small fixed sets do not get a slider — but a set that is only small because nobody offered more does.** Fall sensitivity stays `OptionWay` rows: Low, Medium and High are three named behaviours, not three points on a scale, and each states its own consequence. The Silent SOS staged-call delay stays too — 10s, 30s, 1m and 5m are spaced logarithmically and each option explains what staging means.

The fall countdown and the check-in interval have moved the other way, to `DialControl`. Both were genuinely quantities the whole time. The countdown was four rows offering 15/30/45/60 seconds, which is a linear ramp drawn as a menu; the interval was a row of four small buttons **duplicated across two screens with two separate constant lists**. Both now offer every stop the underlying system can honour — the interval eight where it had four — and that is the test: a fixed set that exists because the value is genuinely categorical stays a row, and one that exists because somebody picked four numbers becomes a track.

### Screen header

One treatment on every screen. A back chevron, the title beside it, and the subtitle **below both, at the gutter** — level with the chevron rather than indented under the title, because the chevron belongs to the title it sits beside and the subtitle belongs to the screen. Indented, the two lines read as one block hanging off the arrow, and the subtitle lost a touch target's width on every pushed screen.

The chevron reserves 28dp of layout while keeping a 48dp touch target, so it reads as next to the word rather than a thumb's width from it. An offset alone cannot do this: an offset moves what is drawn and not what is measured, so the title stays put and the trailing slot loses the same width at the other end.

The header-to-content gap is **20dp**, assembled the same way everywhere: 4dp emitted by the header, 16dp of rhythm supplied by whatever holds it. **No call site places its own spacer after a header.** The number was documented as 28dp and measured as 28, 44 and 12 depending on the screen; three screens took a `Column` adapter's spacer inside a list that already carried the rhythm, and a first section carried the gap that separates one section from the next. A screen that needs the 16dp and has no list rhythm to source it from is a screen whose container is missing something.

### Icons

The app draws its own icons. They are generated from the SVGs in `docs/Icons` into a single Kotlin object of `ImageVector`s — values, not `@Composable` calls, because icons travel as plain data on `BoardWay` and are built outside composition.

**Chrome is in scope.** Back chevron, expander chevron, tick, info, help, search, add, map pin, the four navigation tabs. An earlier revision scoped the set to feature icons only, on the honest ground that it then contained no back arrow; the back chevron is the most repeated glyph in the app and was the most visible seam left.

What is still Material is what the set genuinely has no answer for: the eight mode personas and eight device shapes. Sixteen distinguishable glyphs are needed and the set covers one of them. **Do not reuse one glyph for N distinct things** — that would destroy the only thing telling those modes apart, which is worse than the mixture it replaced.

Two mechanical rules live in the generator rather than in anyone's head. A slug whose name starts with a digit has the numeric run moved to the end, because a Kotlin identifier cannot start with one. And an **optical scale table** corrects a glyph whose artwork fills more of its 24-unit viewBox than its neighbours: a declared size is not a perceived size, and one glyph spanning 20 units among peers spanning 18 is visibly the largest thing in a column at an identical `size()`. The correction is a scaled group in the generated Kotlin, never an edit to the source SVG, so redropping the file cannot silently undo it.

### Chips

**`ChipRow`** is for a field whose answer set is open but whose common answers are five or six words nobody should have to type — a relationship, a blood group. It is **not a picker**: it sets a text field that stays fully editable beside it, so an answer the app never thought of is still sayable, and a value matching no chip lights none of them, which is a correct state and not an error. Tapping the lit chip clears it. Square-shouldered and hairlined, never a Material pill, and selection is carried by border weight, fill and ink together rather than by colour alone.

### Navigation

Four primary destinations in a Material `NavigationBar` themed to the board — plate container, zero tonal elevation, a hairline top rule instead of a shadow. Sub-screens keep their parent tab lit, so a user three levels into Safety still knows where they are.

The **items** are not `NavigationBarItem`. That component draws Material's press state layer and exposes no `indication` parameter to turn it off, so the grey flash on every tab press could not be removed while using it. Each tab is a plain `selectable` `Box` instead; the *container* is still `NavigationBar`, which is where the window insets, the 80dp height and `selectableGroup()` come from, so replacing the item costs none of those.

A selected tab is a **filled glyph in its own accent** inside a soft accent disc — Board clay, Circle sky, Safety sage, Device plum. There is no underline rule and no selection plate. This is the one place the system permits colour to carry a non-status meaning, and it is only safe because the filled/outlined weight change carries it too: the bar still reads correctly in greyscale.

A tab item must carry a **fixed height**, never `fillMaxHeight`. `Scaffold` measures its `bottomBar` with the whole screen height as the maximum constraint, so a child that fills it drags the bar to full height and leaves it floating in the middle of a blank screen. This has been introduced twice.

Motion is Material's three patterns applied consistently: fade-through between the four peer destinations (they have no spatial relationship, so nothing slides), shared-axis X from a destination into its sub-screens, and shared-axis Z for overlays that sit above everything. Durations are asymmetric — 90ms out, 90ms hold, 210ms in — so nothing cross-dissolves into mush, and every transition is short enough that none of them delays reaching an emergency control. The pop specs are written to look correct when scrubbed at an arbitrary fraction, because predictive back seeks them as the user drags.

### Transient messages

A snackbar is a plate, not a floating card: night ground in both themes, hairline, 4dp corner, no elevation, its action in amber ink. Material's default was floating, rounded, elevated and in `inverseSurface` — four of this system's stated don'ts at once — and it survived every visual sweep because it is only on screen for two seconds. It keeps the night plate in the light theme deliberately: a bone card on a bone panel with no shadow is invisible, and the tonal step is what says "this arrived over the panel" without casting one.

### Trip Banner

The tripped state takes the whole frame and refuses to be a card among cards: trip-coloured title block, a 64dp mono countdown, and two actions. The **dismissal is the prominent action, not the call** — the countdown is already going to place the call, so what a person needs when they are fine is a fast, unmissable way to say so.

### Shady (the one exception)

The mascot: a rounded companion-device character with a curved antenna, a real face, and six moods mapped from link state by a single pure function so every screen tells the same story about the same situation. Shady is the **single deliberate exception to the colour-means-state rule** — it is a character, not an indicator — and it is kept out of `Way` rows and away from pilot lamps where the two languages could be confused. Its antenna nub is the one part of it that reports something real.

Two rules carried from the firmware: **no speech or thought bubbles** (text emerging from a mascot competes with the copy that actually matters on a safety screen), and **absent during emergencies**. Screens never draw Shady directly; they go through a host that removes it from the composition entirely when an emergency is live, so it cannot animate, cannot be read aloud, and cannot consume frames while a countdown runs. Enforcing the rule at one boundary is why a new emergency surface cannot forget it.

### Named Rules

**The Kit-Is-The-Vocabulary Rule.** No screen invents a card, button, switch or row. A bank of screens may add what the kit genuinely lacks (a pushed-screen header, a text field) as internal helpers in its own package, but nothing state-carrying is redrawn locally. A gallery screen renders the entire kit in both themes at a raised font scale, and is the reference to check before inventing a seventh kind of card.

**The Mode-Is-Icon-And-Plate Rule.** The eight adaptive modes are distinguished by icon and engraved nameplate alone. Per-mode accent hues were removed from the data layer deliberately: seven competing hues across a system whose one rule is that colour means circuit state is exactly the thing this world replaces. Icon plus label is how a real panel labels its ways, and it survives greyscale, colour vision deficiency and dark mode without a second thought.

**The Honesty Rules.** Three conventions are part of the visual system, not decoration around it. A small dotted **stub mark** in a card's corner (with its own spoken description) is the only tell that data is representative rather than live, used only where hardware or infrastructure genuinely does not exist. A **device-only** row draws a readable value and no control where the app has no write path. **UNKNOWN** is a distinct lamp state meaning "we cannot currently tell" — rendered unlit and inert, never as a fault, because no link is not the same thing as a failure.

## Known evidence limits

Two things about this system are documented rather than demonstrated, so the
next person to review it inherits the gap instead of rediscovering it.

- **The connected-state first viewport has never been photographed.** Every
  capture was taken with no wearable present, so the mains plate has only been
  seen with `—` in BATTERY and SIGNAL and with the primary action slot showing
  CONNECT rather than TEST · RING DEVICE. Both halves are built and both are
  reachable; neither has been seen. Capturing them needs a paired device.
- **Motion is unverified.** Stills cannot show the pilot lamp's filament
  warm-up, the trip lamp's breathing, the rocker's overshoot, or Shady's
  antenna pulse. A screen recording is the only evidence that settles those.

Everything else in this document was read off the built system.

## Do's and Don'ts

### Do:

- **Do** use colour only to report circuit state, and always pair it with a word.
- **Do** pick the lamp-glass token for fills and the matching ink token for text; every state has both for a reason.
- **Do** build rows from the `Way` component so a whole screen reads with one learned pattern.
- **Do** separate surfaces with a 1dp hairline and a tonal step, and mark a bank with a 2dp brass rule.
- **Do** keep corners machined — 2dp for chips and tracks, 4dp for buttons and fields, 8dp for plates.
- **Do** set every label uppercase in the condensed nameplate voice, and every readable number in mono.
- **Do** give every row, tappable or not, at least 48dp of height, and let heights grow with the font scale.
- **Do** let a switch throw and a lamp warm up; the overshoot and the asymmetry are what make the panel feel mechanical.
- **Do** mark representative data with a stub mark, and draw a device-only setting as a value with no control.
- **Do** route the mascot through its host so the emergency suppression rule holds by construction.
- **Do** measure a proportion off the artwork before typing it. The intro lockup was wrong three times running because its numbers were chosen rather than read; every one of them is now a fraction of the emblem's height taken from the supplied logo's alpha channel, and the wordmark is fitted to a measured width rather than set at a guessed size.

### Don't:

- **Don't** saturate a decorative accent, or use one on a lamp, a bus tick, a state word, a seal or any trip surface. Saturation is the only thing keeping the two colour families apart.
- **Don't** hand-pick an accent for a row or section. Use `accentFor(key)` so the assignment is stable and the screen does not become one colour. `LocalBoardAccent` no longer exists.
- **Don't** let a logotype scale with the user's font-size preference. The intro wordmark is sized in dp converted to sp, because it is a picture of a name set beside a fixed-size emblem; every other piece of text in the app honours the setting.
- **Don't** size an image with `size()` when its drawable is not square, and don't trust a drawable's canvas to match its artwork. Both the emblem and the tagline plate carried transparent margins that made every layout number around them a lie; both are cropped to their content now.
- **Don't** use a gradient anywhere — not for a lamp halo, not for a plate, not for a background.
- **Don't** use frosted glass, blur, or any translucent card.
- **Don't** cast a shadow or raise elevation to express hierarchy. Nothing on this panel floats.
- **Don't** enable Material You dynamic colour; it would repaint the pilot lamps from the wallpaper.
- **Don't** use pill corners (20–28dp) or pill-shaped controls.
- **Don't** use a ripple; this surface emits no light, so a press is a small scale-down.
- **Don't** set body copy in the condensed width or in uppercase, and don't uppercase a row title. Row titles are sentence case; a prominent button is Title Case.
- **Don't** explain what the label above already says. Two hundred lines of supporting copy were audited against that one question and about sixty failed it. The test is whether the sentence states a consequence, a cost or a limitation a reader could not have assumed — not whether it is true.
- **Don't** position an icon against its row. A row is as tall as its tallest
  part, which may be a 48dp toggle; the thing the icon labels is the title's
  first line, and that is what it centres on. Anchor it to whichever end of the
  row the title is at.
- **Don't** confirm an action before its result is known. A tick, a "Saved" or
  a "Sent" drawn on the tap is not a confirmation, it is an animation, and on
  the paths this app exists for the failure case is the common one - a wearable
  out of range, no SIM number stored. Where a repository returns a result, wait
  for it; where it returns a reason, show the reason. A control that reports
  success and reaches nobody is worse than one that reports nothing.
- **Don't** hedge. "This is a help, not a guarantee" and its relatives were deleted outright: a person setting up fall detection for their parent is not reassured by the app doubting itself, and the honest limitations are already stated where they apply.
- **Don't** set text in a lamp-glass colour, or reach for the raw colour constants instead of the theme's semantic accessor.
- **Don't** draw a control for a setting the app cannot actually write, and don't show representative data without its stub mark.
- **Don't** place Shady inside a `Way` row, beside a pilot lamp, or on any emergency surface — and never give it a speech bubble.
- **Don't** delete an instrument because it has no reading. A panel shows every way it has whether or not current is flowing, so a gauge with no value renders as an em-dash; only invent a reading that isn't there is forbidden.
