package com.safeshade.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * SafeShade colour system — the "Distribution Board" world.
 *
 * There are two colour families here, and the line between them is drawn by
 * **saturation, not hue**:
 *
 *  - **State** is saturated. Teal, amber and red appear exclusively in pilot
 *    lamps, bus ticks, seals, state words and trip surfaces. A saturated pixel
 *    always means a reader is being told about a live circuit.
 *  - **Decoration** is always desaturated — the six `accent*` tokens below sit
 *    at 22% saturation against state's 55–75%. They carry identity, not status:
 *    which section a row belongs to, which adaptive mode a card is, what an
 *    illustration is made of.
 *
 * Saturation is the separator rather than a reserved set of hues because it
 * survives a colourblind reader. Two greens that differ only in hue are one
 * colour to a deuteranope; a vivid green and a dusty one are still two things.
 *
 * The earlier version of this file forbade decorative colour outright. That
 * held the system together but made it read as cold and corporate across a
 * whole app, which is a real cost on a product people open when they are
 * worried. The saturation rule buys the warmth back without spending the
 * meaning of a lit lamp.
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
// DECORATIVE ACCENTS — identity, never status
// ============================================
/**
 * Twelve peers, not a scale.
 *
 * Each was solved for rather than picked: at a fixed 22% saturation, lightness
 * was walked until the value cleared 4.5:1 against *both* its theme's plate and
 * its ground. That is the text floor, comfortably past the 3:1 a 20dp icon or a
 * 2dp rule needs, so any of these can tint a glyph, draw a rule, or carry a
 * label without a second look. Measured, all twelve, on plate / ground:
 * light 4.91-4.96 / 4.50-4.54, dark 4.50-4.55 / 5.44-5.50.
 *
 * Because they are solved to one floor they land within 1.02:1 of each other,
 * which is the point: twelve different things at one weight, not twelve steps
 * of emphasis. Hue alone separates them - 16, 40, 65, 93, 126, 168, 190, 212,
 * 238, 266, 300 and 332 degrees, no two closer than 22.
 *
 * **Why twelve, and why they are handed out arbitrarily.** Six was too few to
 * give a screenful of rows any variety, and tying a colour to a category meant
 * most screens wore a single hue. They are assigned by a stable hash of a
 * thing's own name instead - arbitrary-looking, but fixed. A row that is plum
 * today is plum tomorrow, because a palette that reshuffles between launches
 * would be worse than no colour at all.
 *
 * **Saturation is what keeps that safe, not hue.** State colours are saturated;
 * these never are. A saturated pixel means status anywhere in this app, so
 * decoration can be scattered freely without ever being read as a lamp. Note
 * the deliberate absence of a decorative colour *named* teal, amber or red -
 * the state hues keep those names to themselves.
 *
 * Used at full strength for icons, rules and identity type; used at low alpha
 * (0.10–0.18) as a wash behind a card, where the normal ink tokens still apply
 * on top. There is deliberately no `onAccent` token — a wash that needed one
 * would be strong enough to be mistaken for a state fill.
 */
private val SageLight = Color(0xFF4C7750)
private val SageDark = Color(0xFF639C68)
private val SkyLight = Color(0xFF586F89)
private val SkyDark = Color(0xFF7A90AA)
private val LilacLight = Color(0xFF7A6299)
private val LilacDark = Color(0xFF9986B2)
private val ClayLight = Color(0xFF896558)
private val ClayDark = Color(0xFFA98679)
private val SandLight = Color(0xFF7A6B4E)
private val SandDark = Color(0xFF9E8C67)
private val MossLight = Color(0xFF5D744A)
private val MossDark = Color(0xFF7A9760)
private val OchreLight = Color(0xFF6D7048)
private val OchreDark = Color(0xFF8D925D)
private val FernLight = Color(0xFF4B756D)
private val FernDark = Color(0xFF61988D)
private val CoveLight = Color(0xFF4E737B)
private val CoveDark = Color(0xFF67959E)
private val SlateLight = Color(0xFF67689E)
private val SlateDark = Color(0xFF898AB3)
private val PlumLight = Color(0xFF8F5B8F)
private val PlumDark = Color(0xFFAD7EAD)
private val RoseLight = Color(0xFF925D76)
private val RoseDark = Color(0xFFAE8196)

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

    /**
     * Decorative identity. Named for what they label, not for their hue, so a
     * later retune cannot leave the codebase calling a blue token "green".
     *
     * Never legal on a lamp, a bus tick, a state word, a seal or a trip
     * surface — those are state, and state is saturated.
     */
    val accentSage: Color,
    val accentSky: Color,
    val accentLilac: Color,
    val accentClay: Color,
    val accentSand: Color,
    val accentMoss: Color,
    val accentOchre: Color,
    val accentFern: Color,
    val accentCove: Color,
    val accentSlate: Color,
    val accentPlum: Color,
    val accentRose: Color,

    /** True when this is the dark resolution — for the few genuinely asymmetric decisions. */
    val isDark: Boolean
)

/**
 * The decorative family as an ordered list, for anything that assigns one per item.
 *
 * Ordered by hue, so two adjacent entries are always visibly different: code
 * that walks this list never lands two neighbouring shades of one colour side
 * by side.
 */
val BoardColors.accents: List<Color>
    get() = listOf(
        accentClay, accentSand, accentOchre, accentMoss, accentSage, accentFern,
        accentCove, accentSky, accentSlate, accentLilac, accentPlum, accentRose
    )

/**
 * The accent a thing wears, derived from its own name.
 *
 * Arbitrary by design - variety was wanted, not a colour code - but *stable*,
 * which is the half that matters. Hashing the key means a row keeps its colour
 * across launches, across a reordering of the list it sits in, and across a
 * later insertion above it. Assigning by list position would have repainted
 * half a screen the first time a row was added to the top of it.
 *
 * `String.hashCode` is specified by the language rather than left to the
 * runtime, so this is reproducible and not merely consistent so far.
 */
fun BoardColors.accentFor(key: String): Color {
    val family = accents
    return family[((key.hashCode() % family.size) + family.size) % family.size]
}

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
    accentSage = SageLight,
    accentSky = SkyLight,
    accentLilac = LilacLight,
    accentClay = ClayLight,
    accentSand = SandLight,
    accentMoss = MossLight,
    accentOchre = OchreLight,
    accentFern = FernLight,
    accentCove = CoveLight,
    accentSlate = SlateLight,
    accentPlum = PlumLight,
    accentRose = RoseLight,
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
    accentSage = SageDark,
    accentSky = SkyDark,
    accentLilac = LilacDark,
    accentClay = ClayDark,
    accentSand = SandDark,
    accentMoss = MossDark,
    accentOchre = OchreDark,
    accentFern = FernDark,
    accentCove = CoveDark,
    accentSlate = SlateDark,
    accentPlum = PlumDark,
    accentRose = RoseDark,
    isDark = true
)
