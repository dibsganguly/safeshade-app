package com.safeshade.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SafeShade colour system — the "Distribution Board" world.
 *
 * The governing rule, and the one most likely to be violated by accident:
 * **colour only ever means state.** Grounds, plates, rules and every piece of
 * type are achromatic. Teal, amber and red appear exclusively in pilot lamps,
 * seals and trip flags. If something is coloured, a reader is entitled to
 * assume it is telling them about a live circuit.
 *
 * Consequence for contributors: there is deliberately no "accent" token for
 * decoration, and no per-mode palette. Adaptive modes are distinguished by
 * icon and nameplate, never by hue — seven competing accents is exactly the
 * thing this system replaces.
 *
 * Hues derive from the brand emblem (docs/Logo/SafeShade Emblem Logo.png):
 * charcoal body, teal signal arcs, amber sun dot.
 */

// ============================================
// BRAND CONSTANTS (from the emblem, theme-independent)
// ============================================
/** The emblem's shield body. Also the light theme's primary ink. */
val BrandCharcoal = Color(0xFF22282E)
/** The emblem's signal arcs. Lamp glass only — too light to carry text on bone. */
val BrandTeal = Color(0xFF6FD3CC)
/** The emblem's sun dot. */
val BrandAmber = Color(0xFFF5A623)

// ============================================
// LAMP GLASS — the lit appearance of a pilot lamp
// ============================================
private val LampLiveGlass = BrandTeal
private val LampAttentionGlass = BrandAmber
private val LampTripGlass = Color(0xFFE5484D)

/**
 * Ink-safe counterparts. A lamp's glass colour is chosen to look right lit;
 * it is not chosen to pass contrast as text. Any text that must carry a state
 * meaning uses these instead — which is why every state has two tokens.
 */
private val LiveInkLight = Color(0xFF0F7A72)
private val LiveInkDark = Color(0xFF7EDCD5)
private val AttentionInkLight = Color(0xFF8A5300)
private val AttentionInkDark = Color(0xFFFFC46B)
private val TripInkLight = Color(0xFFB3151A)
private val TripInkDark = Color(0xFFFF8A8D)

// ============================================
// LIGHT — the panel in a lit room
// ============================================
private val BoneGround = Color(0xFFF2EFE9)
private val BonePlate = Color(0xFFFBF9F5)
private val BoneRecess = Color(0xFFE5E1D8)
private val BrassRuleLight = Color(0xFFB9A88A)
private val HairlineLight = Color(0xFFD3CDC0)
private val InkLight = BrandCharcoal
// Warm, and measured. The previous values were a cool blue-grey on a warm
// ground - wrong hue family - and inkFaint sat at 3.06:1 against the plate,
// under the 4.5:1 body-text floor. On a product whose stated audience includes
// elderly wearers and their (often also elderly) guardians, secondary text
// failing contrast is a functional defect, not a refinement.
// Measured on plate #FBF9F5 / ground #F2EFE9: muted 7.49:1 / 6.87:1,
// faint 5.51:1 / 5.04:1.
private val InkMutedLight = Color(0xFF55514B)
private val InkFaintLight = Color(0xFF69655F)

// ============================================
// DARK — the same panel in a hallway at night
// ============================================
private val NightGround = Color(0xFF14171A)
// Re-anchored to the emblem's own charcoal so the named brand value appears in
// the dark scheme rather than only as light-mode ink. The ground stays two
// steps darker, which is what keeps a plate reading as raised.
private val NightPlate = Color(0xFF22282E)
private val NightRecess = Color(0xFF0E1113)
private val BrassRuleDark = Color(0xFF8A7A5E)
private val HairlineDark = Color(0xFF353C43)
private val InkDark = Color(0xFFECEFF1)
private val InkMutedDark = Color(0xFFB4AEA4)
// 4.63:1 on the plate, 5.60:1 on the ground. The old #6B7480 was 3.42:1.
private val InkFaintDark = Color(0xFF948F86)

/**
 * Semantic tokens, resolved per theme and read via `MaterialTheme.board`.
 * Screens must never reference the private constants above directly — that is
 * how a light-only colour ends up hardcoded into a dark screen.
 */
data class BoardColors(
    /** The panel behind everything. */
    val ground: Color,
    /** A raised plate: a way row, a card, a section body. */
    val plate: Color,
    /** A routed channel — inputs, wells, anything set *into* the panel. */
    val recess: Color,
    /** Structural separator between ways. */
    val hairline: Color,
    /** The engraved-brass rule under a section nameplate. Emphasis, not decoration. */
    val brass: Color,

    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,

    /** Lamp glass — fills only. Never use for text. */
    val lampLive: Color,
    val lampAttention: Color,
    val lampTrip: Color,
    /** An unlit lamp. */
    val lampOff: Color,

    /** Contrast-safe counterparts for text carrying the same state meaning. */
    val inkLive: Color,
    val inkAttention: Color,
    val inkTrip: Color,

    /** True when this is the dark resolution — for the few genuinely asymmetric decisions. */
    val isDark: Boolean
)

val LightBoardColors = BoardColors(
    ground = BoneGround,
    plate = BonePlate,
    recess = BoneRecess,
    hairline = HairlineLight,
    brass = BrassRuleLight,
    ink = InkLight,
    inkMuted = InkMutedLight,
    inkFaint = InkFaintLight,
    lampLive = LampLiveGlass,
    lampAttention = LampAttentionGlass,
    lampTrip = LampTripGlass,
    lampOff = Color(0xFFC8C2B4),
    inkLive = LiveInkLight,
    inkAttention = AttentionInkLight,
    inkTrip = TripInkLight,
    isDark = false
)

val DarkBoardColors = BoardColors(
    ground = NightGround,
    plate = NightPlate,
    recess = NightRecess,
    hairline = HairlineDark,
    brass = BrassRuleDark,
    ink = InkDark,
    inkMuted = InkMutedDark,
    inkFaint = InkFaintDark,
    lampLive = LampLiveGlass,
    lampAttention = LampAttentionGlass,
    lampTrip = LampTripGlass,
    lampOff = Color(0xFF31383E),
    inkLive = LiveInkDark,
    inkAttention = AttentionInkDark,
    inkTrip = TripInkDark,
    isDark = true
)
