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

**The one depth carve-out is the character.** Shady and the props on its stage are cel-shaded: a rim band along the lit edges, a core band along the shadowed ones, a flat contact ellipse on the ground, and one small sheen. Three flat tones and a low-alpha ellipse — the same vocabulary as the pilot lamp's halo, and still no gradient and no blur anywhere in the build. It applies to the character and its world only; a plate, a button or a gauge never takes a band or a contact shadow.

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
- **Icon-only (`BoardIconButton`):** the same plate, weights, floor and press with no word on it, the glyph at 28dp centred and the word carried as the spoken description, which is required. For a strip of two or three actions on a plate too narrow for words: the family dashboard's Message, Where and Call, whose labels wrapped mid-word at the phone's own width. Not for a button that stands alone, where the word is the label.

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

The app draws its own icons, and nothing else: every glyph on every screen comes from the 227 SVGs the user drew into `docs/Icons`, generated by `tools/gen_icons.py` into one Kotlin object of `ImageVector`s (values, not `@Composable` calls, because icons travel as plain data on `BoardWay` and are built outside composition). Material's icon library is no longer a dependency. The SVGs are the source of truth and are never edited, renamed or deleted; a redrop regenerates the object.

**Everything is in scope.** Chrome (back and expander chevrons, tick, close, info, help, search, add, pin), the four navigation tabs, the sixteen mode personas and device shapes (elderly, kid, paw, bicycle, helmet, hand, backpack; umbrella, wrist-watch, necklace, cap, cane, collar), the brand marks on the sign-in buttons, and one purpose-named glyph per row where the set has one (walkie-talkie for Talk, radar for the community map, a first-aid case knocked out of the SOS disc, cloud states for the Account page). **Do not reuse one glyph for N distinct things**: the personas and devices exist precisely so that sixteen rows tell apart.

The generator is where the mechanical rules live. Any viewBox is normalised into the shared 24-unit frame; fills, strokes, even-odd and `<g>` transforms are honoured and anything unsupported fails loudly rather than vanishing; a slug that starts with a digit has the numeric run moved to the end. The **optical scale table** corrects a glyph whose artwork fills more of its box than its neighbours (the set's ink spans about 21.5 of 24 units; the three solid imports, the fall figure and the two brand marks spanned the whole box and are scaled to sit level), as a scaled group in the generated Kotlin, never an edit to the SVG. `Icon()` tints the whole painter, so the Google mark is drawn as a solid monochrome G, which is the right voice for this board; a four-colour mark would need an untinted painter and is not wanted.

**Where glyphs go, and where they do not.** A way carries one; a collapsed bank (`ExpandableSection`) carries one before its label so it reads like the rows inside it; an action button carries one when the set has a purpose-named glyph for the act (send the invitation, add a person, sign out, delete, start the journey, I am OK) and none when it does not, rather than a near miss. Section plates and page titles carry none: the word is the label there. The SOS disc alone keeps Material's six-armed asterisk, verbatim, because the set's first-aid case did not read as SOS in the disc and the user asked for exactly the one that was there.

**Faces on contacts.** An emergency contact has a face like a wearer does, made in the same editor (name beside the face, presets, a photo, a made one); the contacts bank is a bank of person rows. The face stays on the phone: the cloud row has no column for it and a pull never clears it.

### Avatar

A person's face, at any size, from one string id: blank draws the initial on the person's accent disc; `a:…` is a procedural face from `AvatarSpec` (face shape, skin, hair including a headscarf, a turban and a cap, hair colour, brows, eyes, mouth, glasses, one accessory, a backdrop from the twelve accents), drawn in the character's flat two-tone voice so it sits beside Shady without a clash; `photo:<file>` is a photo copied app-private. Twenty-four presets ride in a strip with the chosen one ringed in brass; the customiser shows every option as the whole face with only that attribute changed, so a person sees what they are choosing rather than reading a label for it. Apple's Memoji was the reference; this is the same idea in our own hand.

### Profile

The Device page's top-right control is the person holding the phone — their face, not a gear — and it opens the Profile page: an identity plate (a 72dp face, the name, one line on the role), then **People I look after** for a Guardian (each person a row with their face, their name and which wearable they wear), then the phone's own banks (role, appearance, alert reliability) and About. What used to be "settings" are facts about a person and their phone, and a page that opens on a face says so. Editing a person is one page: a name field beside their face, the preset strip, "Use a photo" and "Make your own", and a single COMMIT save; nothing is written until it.

### Person row

A person as a way: bus tick, their face at 44dp, their name, one line, and on the right a state word or a chevron. `Way` carries an icon; a person carries a face, and a face is what a guardian scans a list for. Everything else is the way's grammar, so a bank of people reads like a bank of circuits and is spoken as one node, "name, state, detail". It lives in the kit (`ui/board/PersonRow.kt`) because the Profile page and the People page both draw it and had each grown their own.

### Sync dot

Whether one record has reached SafeShade Cloud, as a 6dp lamp in the row's top-right corner (`ui/board/SyncDot.kt`). Teal once the row landed, amber while it is queued or in flight, trip red when the outbox gave up with a reason; drawn only when there is something to say, so a record with no cloud history, or a phone that is not signed in, draws nothing rather than a grey "not synced". It sits outside the way's own grammar because it reports the row's copy, not the trip, and the spoken description carries the same word as the colour ("On SafeShade Cloud", "Sending to SafeShade Cloud", "Not on SafeShade Cloud: reason"). The trip log and the message thread draw it; the Account page's Sync plate is the same fact for the whole queue, and its Sync Now button greys only while a drain is actually in flight, never merely because something is queued.

### People I look after

The Profile's section is a list: one person row per wearer, then an "Add a person" way. The People page is the same list with a state word per person for the link to their wearable (Live, Off, No wearable) and one commit-weight button to add. A Companion is their own single wearer and sees themselves alone with no add.

**The wearer editor** is one page per person: the shared face editor (name beside the face, preset strip, photo, make your own), then **their wearable** as a bank of rocker switches, one per wearable this phone has met, off until thrown, with the whole row as the target; then their medical ID as a way with its field count; then contacts for this person alone, with the line that the Safety page's contacts are called for everyone and these are called too. One COMMIT save reports the repository's answer, and a refused write (the last person, the Companion's own record) shows its reason under the button. Binding is never guessed: a v2 install's paired wearable is not bound to anyone until a person throws its switch, because the bound person's medical card is what gets pushed to the device.

### The family dashboard

For a Guardian the Circle tab opens on **one plate per person** instead of one person plate: the face at 52dp, the name in headline, the mode on one line, the link lamp top right with its word at the foot of the block; a rule; three mono readouts, Battery, Last seen and Last alert, each a figure or a dash ("7 h ago", "SOS · 21 h", never a sentence, because a mono readout has no room for one and the place itself is on the Where plate below); a rule; and three quiet icon-only actions, Message, Where (the where-navigation glyph, a compass in a ring) and Call, each spoken by name. Battery is only ever the connected wearable's own report on the plate of the person bound to it; every other plate shows a dash. Call is disabled with its reason under it when no SIM number is stored. A Companion still sees the single guardian plate.

### Guardians and invitations

The Circle's Arrangements bank carries a Guardians way whose state word is a fact ("Only you", "3 people") and whose line says what is outstanding. The Guardians page lists the members as ways with their role as the state word, then every invitation as a way whose state word is its real status: Sent (amber, "not yet accepted"), Accepted, Expired, or Not sent (trip red) with the mail provider's own reason as the line. Inviting is an email field, the role as two selection ways (In use / Not chosen), and one COMMIT button that holds "Sending…" until the edge function answers; the row appears from the server's response, never from the tap.

### Plans

Three plates, one per tier, the current one carrying a lit lamp and the line "Your plan". Each plate: name and one line at the left, the lamp and a mono price readout at the right (Play's own price when Play has answered, labelled "Play price"; the list price until then, labelled so), a rule, what the tier includes as plain lines, a rule, and one COMMIT button that hands the product to Google Play. Play's answer is shown verbatim in a "Google Play said" way under the plates; with no Console listing that answer is "This item is not available", and the page shows exactly that. A guest sees "Sign In to Choose" instead, because a plan belongs to an account.

### The heat map

One map with two layers. The household's own places (alerts with a fix, safe zones, the last fix) are small ink rings. The community layer is SafeShade Cloud's 1 km cells with at least five alerts each, drawn as three flat amber wash steps by count, never a ramp, so the layer reads in greyscale and nobody's home is visible in it. The layer is a rocker on a way that says what it is and that it is part of Plus; on the Free plan the row's state word is "Plus" and tapping it opens the plans.

### Emails

On the Account page: one way, "What SafeShade sends you", with a count of what is on, opening a page of rockers per kind of mail (Alerts, Circle changes, Account notices, Weekly report). Each rocker flips only after the server has accepted the change, because the server is what honours it; until the server has answered the row shows a dash. Sign-in codes and password mails are deliberately absent: they are how signing in works, not a notification. "Send This Week's Report Now" reports the server's answer as a sentence naming who received it, who had it off, and who could not be reached.

### Privacy

A page of facts on the Profile, under This phone: what this phone knows and where each thing goes, one way per record, with the state word saying Local, Synced, Held or a count. The one control is the community map: "Share where alerts happen", a rocker that withholds an alert's place while the alert itself still syncs, with the row saying that places already sent stay sent. The page names the region and the delete path rather than reassuring; every row can be checked against the code.

### Talk

Push-to-talk voice notes between the Circle's phones, as a walkie-talkie rather than a voicemail: one control, a plate held to speak and let go to send, up to twenty seconds, its edge brass at rest and trip red while recording with the elapsed time written on it. Each note is a plate: author and time, the note's own **waveform** (`ui/board/Waveform.kt`, one flat bar per sampled amplitude, played bars in brass, the rest in faint ink, no gradient, no cursor), a play control, the duration, and a lamp with a state word the note can stand behind, On this phone, Sending, On SafeShade Cloud, or Not sent with the reason. No tick is drawn before the upload has completed, because until then nobody else can hear it. The microphone permission is asked for at the first hold and a refusal is printed as the reason the control did nothing. The Circle hub's Talk way counts notes not yet heard.

### If nobody answers

The ladder of calls behind an open alert, as one page: a "Keep calling" rocker at the top (off until a person throws it, because a phone that rings people on a timer must be told to), the ladder itself as a bank of numbered ways built by the same planner the runner uses from the real contacts and the real settings, two dials for the timing, and the end of the ladder as a rocker plus the number. Two facts are printed on the page rather than left to be found: the emergency number is never called by itself (the dialer opens with it ready), and a phone with no contact stored dials nothing at all, including that number. The Safety hub's "If nobody answers" way carries the chain as a sentence, "Calls Meera, then Arun, then 112".

**The ladder plate** (`safety/EscalationPlate.kt`) is the same run drawn as a record: one way per rung, state word Dialled, Skipped, Not needed or Waiting, and the detail is the time and what the dial reported, verbatim ("At 04:31 · Dialer opened"). There is no Reached and no tick: nothing on a phone can see that somebody picked up. It sits on the trip's own page under "What the phone did", and on the ladder page under "Right now" while an alert is open.

### Out of reach

The two notices the phone raises about the wearable itself, on one page: a "Right now" plate with the link and the battery in the words the notification would use (Connected; Out of reach since 09:12 · 45 min · you were told; 12% · below 15%), then two dials, minutes out of reach and percent of battery, where zero is drawn as the word Off. Both dials write on release and together, because they share one settings row. The hub's way says what is watched, or the outage in progress, and lights amber while the wearable is off the link.

### Vitals

Heart rate, blood oxygen and temperature as three large mono readouts on one plate with a single provenance line under them ("From Health Connect (Mi Fitness) · 5 min ago"; "From the wearable · just now"). A reading that was not measured is a dash, and a plate with no reading says so in words instead of drawing zeros. The two sources are a bank of two ways: the wearable (Reporting, No sensor reading, or the dash while it is off the link) and Health Connect, whose state word is the exact thing standing between the guardian and a reading, Not installed, Needs an update, Not allowed, Allowed, and whose one button below does the one thing that fixes it. Thresholds are four dials that commit together on release; a breach lights the readout amber, adds an "Outside range" way, and lights the hub's Vitals way. Readings are never alerts and the page says so.

### Evidence

The microphone after a fall or an SOS, on one page: two rockers for the two causes, one dial for how long, one rocker for whether a recording ever leaves the phone (Off means it stays; On while signed out reads Signed out, not On). A recording made from the page is labelled a test and is saved exactly like a real one. The sound meter is an instrument: a large readout in dB with its calibration limit printed under it verbatim, and a Loud environment way that goes amber only on a sustained minute at 85 dB. Recordings are ways whose state word is where the file is (On this phone, Sending, On SafeShade Cloud, Not sent with the reason); tapping one plays it and reveals Delete. Nothing on the page starts the microphone without the system notification showing.

### Product silhouette

The three wearables drawn in the character's three flat tones (rim, skin, shade) with ink outlines: the S1 is the flat puck with its small recessed screen, three light pips and the knob on the right edge; the 5G is the same puck with the aerial nub off centre on top and the SIM tray notch below; the Spark is the round tag on a lanyard ring with the bolt on its button face. The accent is identity only. Used at 84 dp on the pairing page's chooser and at 72 dp beside the firmware readout. Never a render, never a photograph: the job is to let a hand pick the right outline.

### Pair a wearable

Three silhouette plates across the top, the chosen one raised with a live bus tick under it; a bank of four facts for that product (what it carries, what the phone can do with it: the Spark's tag row reads a dash with "This phone has no NFC" on a phone without one); then the search as a plate with the connection's own state word and a lamp, and one button whose label is the next thing to do. A wearable that answers as a different model than the one chosen is reported as such and pairing continues.

### Firmware

The wearable's version as a large readout beside its silhouette, with the sentence saying where the number came from (the wearable over the link, or "acknowledged the question but reported no version"). Published releases are ways whose state word is the relationship to this device (On the wearable, On this phone, Newer, or the size). The update is one way whose state word is the step (a percentage, Verifying, n of m chunks, Asking, Installed, Failed) and whose detail is the real reason. Installed means the wearable reported the new version itself; nothing else counts.

### Lost

Each of this phone's wearables as a way with a Lost rocker and a detail line of two dashes or two times: when this phone last heard it, and when the community last did. A lost device with a community fix gains a second way that opens the map. The sweep is a way (Idle, Listening, Not allowed) and a button; the community is one rocker that says exactly what leaves the phone (the wearable's address, the time, the place) and reads Signed out rather than On when the account is out.

### Smart home

Automations are ways: name, "trigger → destination", and the last thing that happened with its status code; the rocker arms it, the row opens it. The editor is chip rows for trigger and destination, an address field whose error states the reason, an optional secret, a Send a Test button whose supporting line becomes the real reply, and one COMMIT save. The three platform rows report the real outcome of their one action in the past tense and a dash until tapped ("Not installed", "Opened Google Home", "This phone can commission Matter devices").

### Ride log

Three readouts (rides, km, moving) that are dashes until a ride exists, then a bank of rides with the distance as the state word and "moving · top speed · fixes" as the detail; a ride with fewer than three fixes says it could not be measured. The Bike profile's brake light is described in one line as the wearable's own behaviour, because the phone cannot change it.

### Quiet word, positioning, the wearable's own lights, nudges, walk home

Five small additions that follow existing patterns rather than adding kit. The **quiet word** is a way plus a field on the Silent SOS page; the way reads Armed only once a word passes validation, and the field's error states the reason ("Too ordinary. It would trip on an everyday message."). The **positioning readout** on Locate is three compact readouts, SOURCE / ACCURACY / AGE, dashes when there is no fix; the source is the provider's name, never a guess. The **wearable's own lights** are two device-only ways on the Lights page (Brake light, Path light) whose state word names the profile they belong to and whose detail is the firmware's behaviour in numbers; no control, because no BLE path exists. **Weather nudges** are amber ways under the Board's gauges, present only when a rule fires, each line a consequence. **Walk Home** is a secondary button on the Journey setup that fills the same journey in ("Home", twenty minutes); it is not a separate mode.

### Incident report, tag writing, guiding

Three more actions in existing shapes. **Share as PDF** on a trip's page is a secondary button whose supporting line says what the one page holds; the failure, if any, is the share sheet's reason under it, and nothing says "Saved". **Write to a tag** on the Emergency card is a way with a rocker only on a phone that has NFC; its state word is the exact stage (dash, NFC is off, Ready, Hold a tag, Done) and its line the outcome sentence, including the tag's capacity when it is too small. **Guide the wearable** is an icon action on a zone row, present only while the link is up, and the snackbar reports whether the wearable acknowledged the destination.

### First run

Six steps, each the same shape: a scene at the top in which the character does the thing the step is about (walks on and lights the board; stands beside a cane or a phone; peeks at the face being chosen; watches a small board light up as grants land; searches with its antenna until the wearable's own nub answers; holds up the card), a rail of bus ticks for progress, a headline, at most one line of body copy, the controls, and the actions pinned at the foot, rising with the keyboard. The scene runs under the status bar. Feature statements on the welcome step are rows with an icon in its accent and no bus tick or lamp, because they are not circuits.

### Chips

**`ChipRow`** is for a field whose answer set is open but whose common answers are five or six words nobody should have to type — a relationship, a blood group. It is **not a picker**: it sets a text field that stays fully editable beside it, so an answer the app never thought of is still sayable, and a value matching no chip lights none of them, which is a correct state and not an error. Tapping the lit chip clears it. Square-shouldered and hairlined, never a Material pill, and selection is carried by border weight, fill and ink together rather than by colour alone. A focused chip (keyboard, switch access) steps its edge up to ink at the rule weight, because there is no ripple to show focus and a control that cannot be seen to have focus cannot be operated without a finger.

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

The mascot: a rounded companion-device character with a curved antenna, a real face, and twelve moods mapped from link state (and, for three of them, from a fresh success, the quiet hours and the outside temperature) by a single pure function so every screen tells the same story about the same situation. Shady is the **single deliberate exception to the colour-means-state rule** — it is a character, not an indicator — and it is kept out of `Way` rows and away from pilot lamps where the two languages could be confused. Its antenna nub is the one part of it that reports something real.

**Depth.** The character is subtly three-dimensional inside the flat draw code: the light comes from the upper left as three flat tones (rim band, skin, core band, each the body's own silhouette minus the same silhouette pushed diagonally, so the bands follow the corners and taper where the light grazes), a flat contact ellipse on the ground that the body lifts off in a hop, a specular dot on the nub and a lid shadow in the eye whites. A turn is a rotation rather than a mirror cut: the silhouette narrows to edge-on, the antenna slides to the centreline and the eyes pull together, then it widens facing the other way. The antenna is a springy stalk with secondary motion derived from the body's own frame-to-frame movement, so every beat lags and overshoots without asking for it.

**The stage and its props.** At the foot of the Board, Shady shares the ground with one prop at a time — a plant it waters, a cat that rubs against it, a phone that buzzes, a kettle that whistles, a ball it nudges off the edge, a box it climbs into, an umbrella when the app knows it is raining. Props are drawn in the character's register and sized against it; they never report state. The umbrella reads the weather to decide whether to exist, which is a detail, not an indicator. Reactions stay hand-keyframed with an anticipation frame before every launch and an overshoot on every landing.

Two rules carried from the firmware: **no speech or thought bubbles** (text emerging from a mascot competes with the copy that actually matters on a safety screen), and **absent during emergencies**. Screens never draw Shady directly; they go through a host that removes it from the composition entirely when an emergency is live, so it cannot animate, cannot be read aloud, and cannot consume frames while a countdown runs. Enforcing the rule at one boundary is why a new emergency surface cannot forget it.

### Named Rules

**The Kit-Is-The-Vocabulary Rule.** No screen invents a card, button, switch or row. A bank of screens may add what the kit genuinely lacks as internal helpers in its own package (the text field and the option way were such helpers until v2.8.0, when they moved into the kit), but nothing state-carrying is redrawn locally. A gallery screen renders the entire kit in both themes at a raised font scale, and is the reference to check before inventing a seventh kind of card.

**The Look-Then-Choose Rule.** A choice with consequences for a person — which adaptive profile the wearable runs — is made on a page that says everything the app knows about the option, on one commit-weight button at its foot, and never on the list that browses the options. The list shows the whole set at once (the current choice first, at the size it deserves; the rest as scannable rows with a scene thumbnail, a name, one line on who it is for, and the seal where the wearer would lose the device's own menus). The detail page is a bank of facts read off the firmware, in real units — what the wearable trips at in g for the sensitivity it is actually on, which screens its rotary cycles, what its lights do unasked, how often it asks for a position, what a locked profile hides — followed by the deck's priority features linked to their screens. A pager of cards was the earlier answer and it hid the set below the fold and asked for the decision from a swipe.

**The Mode-Is-Icon-And-Plate Rule.** The eight adaptive modes are distinguished by icon and engraved nameplate alone. Per-mode accent hues were removed from the data layer deliberately: seven competing hues across a system whose one rule is that colour means circuit state is exactly the thing this world replaces. Icon plus label is how a real panel labels its ways, and it survives greyscale, colour vision deficiency and dark mode without a second thought.

**The Honesty Rules.** The app draws exactly what launch would draw for the current connection state, and no more. There is no "planned", "coming soon" or "representative" anywhere in the UI; what the firmware does not yet honour is recorded in `DeviceCapabilities.awaitingFirmware` and the handoff, not on a screen. Three conventions carry the honesty instead. **Absent data is absent**: a gauge with no reading shows a dash, never an invented number. **A failed action reports its reason**, and nothing draws a tick, a "Saved" or a "Sent" before its result is known. A **device-only** row draws a readable value and no control where the app has no write path. **UNKNOWN** is a distinct lamp state meaning "we cannot currently tell" — rendered unlit and inert, never as a fault, because no link is not the same thing as a failure.

## v2.0 candidates

The Kit gallery opens on a numbered section of a hundred and one candidate members
(`ui/board/KitCandidates.kt` and `KitCandidates2.kt`, with eleven candidate type families in
`ui/theme/CandidateType.kt` and their `.ttf` files in `res/font`). None is
part of the shipped system; nothing in the frontmatter, the rules above or
any screen refers to them. A candidate is adopted only when the user names its
number, at which point it moves into its family's file, the rule it changes is
rewritten above, and the rest of its group, fonts included, is deleted. The
numbers are stable for as long as a candidate exists.

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
| 2.42 | Space Grotesk titles, Figtree body | the whole scale |
| 2.43 | Outfit throughout | the whole scale |
| 2.44 | Inter throughout | the whole scale |
| 2.45 | JetBrains Mono for readouts | Readout, Gauge, countdown |
| 2.46 | Title to subtitle: three gaps | ScreenHeader |
| 2.47 | Title to detail: three gaps | Way |
| 2.48 | Section plate to bank: three gaps | section rhythm |
| 2.49 | A wider gutter, a tighter plate | Layout |
| 2.50 | A ground per hub, stronger | 2.08 |
| 2.51 | A hub band at the top | the hub head |
| 2.52 | A hub rule under the status bar | the hub head |
| 2.53 | Plates tinted per hub | BoardPlate |
| 2.54 | Section plates in the hub's ink, rows in plain ink | SectionPlate |
| 2.55 | A warm night | dark neutrals |
| 2.56 | A ground with a grain | ground |
| 2.57 | A square lamp | PilotLamp |
| 2.58 | A lamp in a bezel | PilotLamp |
| 2.59 | A lamp cluster | the hub summary |
| 2.60 | A lamp with a count | PilotLamp |
| 2.61 | The state word on glass | the state word on a Way |
| 2.62 | An outlined hued button | the hued button weights |
| 2.63 | A button with its glyph on a disc | BoardButton |
| 2.64 | A split button | BoardButton |
| 2.65 | An action pair | two buttons at a page foot |
| 2.66 | A button that shows its progress | BoardButton while working |
| 2.67 | Hold to confirm | ButtonWeight DANGER |
| 2.68 | A readout with its trend | Readout |
| 2.69 | A readout with a sparkline | Readout |
| 2.70 | One number, very large | Readout large, Gauge |
| 2.71 | An arc gauge | Gauge |
| 2.72 | A battery in cells | the Battery readout |
| 2.73 | A value against its target | Readout, thresholds |
| 2.74 | Readouts with lamps | the instrument strip |
| 2.75 | Shady over the edge of the mains plate | MainsPlate, the stage |
| 2.76 | Shady beside the state | MainsPlate |
| 2.77 | A state band across the top | MainsPlate |
| 2.78 | The mains as a status well with wells | MainsPlate |
| 2.79 | A centred mains | MainsPlate |
| 2.80 | The mains with the place on it | MainsPlate |
| 2.81 | A bank with its header inside | SectionPlate over BoardPlate |
| 2.82 | A bank with alternating rows | Hairline between ways |
| 2.83 | Numbered rows | Way, sequences |
| 2.84 | Ways as tiles | navigating ways |
| 2.85 | A switch row that also says its state | Way with a rocker |
| 2.86 | A row with a meter in it | Way, quantities |
| 2.87 | A row with a leading lamp | Way |
| 2.88 | A row with the faces it concerns | Way, logs |
| 2.89 | A card with a top band | BoardPlate |
| 2.90 | A card with a corner tag | BoardPlate with a qualifier |
| 2.91 | A card with an engraved title plate | BoardPlate with a heading |
| 2.92 | A card with a footer action | BoardPlate with a button |
| 2.93 | A recess inside a plate | BoardPlate recessed |
| 2.94 | A card with a watermark glyph | BoardPlate |
| 2.95 | A strip of cards | a bank of similar things |
| 2.96 | Steps down the page | setup paragraphs |
| 2.97 | An expandable bank that previews its rows | ExpandableSection |
| 2.98 | Tabs inside a page | two lists on one page |
| 2.99 | A section plate that stays | SectionPlate on a long page |
| 2.100 | An empty bay with a glyph disc | EmptyBay |
| 2.101 | A snackbar with a lamp | Transient messages |

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
- **Do** draw a device-only setting as a value with no control, and a gauge with no reading as a dash.
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
- **Don't** draw a control for a setting the app cannot actually write, and don't label anything "planned", "coming soon" or "representative" — the app reads as it will at launch.
- **Don't** place Shady inside a `Way` row, beside a pilot lamp, or on any emergency surface — and never give it a speech bubble.
- **Don't** delete an instrument because it has no reading. A panel shows every way it has whether or not current is flowing, so a gauge with no value renders as an em-dash; only invent a reading that isn't there is forbidden.
