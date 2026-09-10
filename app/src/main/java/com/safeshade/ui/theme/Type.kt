package com.safeshade.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.safeshade.R

/**
 * Type for the Distribution Board world.
 *
 * Three voices, each drawn from one variable file:
 *
 *  - **Plus Jakarta Sans** is the display voice: screen titles, the mains
 *    plate headline, the title of a card. Chosen from the v2.0 candidates
 *    (2.04) for its face, and deliberately *not* used throughout: a geometric
 *    with that much presence in every row reads as a brochure. It appears
 *    where a heading appears and nowhere else.
 *  - **Archivo** carries everything a person reads rather than glances at.
 *    Its `wdth` axis is what makes the system work: at width 100 it is a
 *    plain workhorse grotesk for body copy and row titles, and at width 78 it
 *    becomes the condensed, engraved label-plate voice of section plates and
 *    state words. One family, two registers, no mismatch between them.
 *  - **JetBrains Mono** carries numeric readouts only — telemetry,
 *    coordinates, signal strength, timers. Anything a person reads
 *    digit-by-digit. It replaced Azeret Mono (candidate 2.45): a taller,
 *    rounder mono with a slashed zero that stays legible at 12sp in a well.
 *
 * All three are bundled as .ttf in res/font rather than pulled through
 * Downloadable Fonts: the Play fonts provider is unreliable on emulator
 * images, and a font that sometimes fails to arrive makes screenshot
 * verification non-deterministic.
 *
 * Sizes are deliberately one step larger than a typical phone scale. The
 * elderly wearer's guardian is often also elderly, and this app is read in a
 * hurry.
 */

@OptIn(ExperimentalTextApi::class)
private fun archivo(weight: Int, width: Float) = Font(
    resId = R.font.archivo_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight),
        FontVariation.width(width)
    )
)

@OptIn(ExperimentalTextApi::class)
private fun jetbrains(weight: Int) = Font(
    resId = R.font.jetbrains_mono_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    resId = R.font.plus_jakarta_sans_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/**
 * Body / UI voice — Archivo at normal width.
 *
 * Carries 800 and 900 as well as the text weights: the wordmark in the opening
 * animation is set in this family rather than shipped as an image, so it
 * inherits the theme's ink and scales with the display.
 */
val BoardSans = FontFamily(
    archivo(400, 100f),
    archivo(500, 100f),
    archivo(600, 100f),
    archivo(700, 100f),
    archivo(800, 100f),
    archivo(900, 100f)
)

/**
 * Nameplate voice — Archivo condensed. Uppercase with tracking.
 *
 * Its remit narrowed in v2.1: it now sets **section headings and state words
 * only**. Row titles and button labels moved to [BoardSans] in sentence case,
 * because caps-everywhere read as shouting across a whole app and cost more
 * than the engraved look was worth. Caps still mean something here precisely
 * because they are now rare.
 */
val BoardCondensed = FontFamily(
    archivo(500, 78f),
    archivo(600, 78f),
    archivo(700, 78f)
)

/**
 * Display voice — Plus Jakarta Sans. Headings only.
 *
 * Carried by the Material display, headline and title steps and by nothing in
 * [BoardType]: a nameplate, a row detail, a state word and a readout all keep
 * their own faces, so a heading is the one thing on a screen set in this
 * family and reads as one.
 */
val BoardDisplay = FontFamily(
    jakarta(500),
    jakarta(600),
    jakarta(700),
    jakarta(800)
)

/** Readout voice — digits you read one at a time. */
val BoardMono = FontFamily(
    jetbrains(400),
    jetbrains(500),
    jetbrains(700)
)

/**
 * Styles that have no Material 3 equivalent because they belong to this world
 * rather than to the type scale. Read via `MaterialTheme.boardType`.
 */
data class BoardTypography(
    /**
     * A circuit label — the title of a way, the label on a button.
     *
     * Sentence case in the body voice as of v2.1. It was condensed caps at
     * 0.12em, which is right for one engraved plate and wrong for the two
     * hundred rows an app actually has.
     */
    val nameplate: TextStyle,
    /** A smaller nameplate, for sub-rows, gauge captions and inline field labels. */
    val nameplateSmall: TextStyle,
    /** A section heading sitting above a brass rule. Caps: this is one of the two. */
    val sectionPlate: TextStyle,
    /** The state word on the right of a way: LIVE / OFF / TRIPPED. Caps: the other one. */
    val stateLabel: TextStyle,
    /**
     * The engraved micro-caps of a seal.
     *
     * The last survivor of the old condensed-caps nameplate, kept because a
     * seal is a stamped physical object and reads as one. Do not reach for it
     * as a general small label — [nameplateSmall] is that.
     */
    val sealPlate: TextStyle,
    /** Telemetry and coordinates. */
    val readout: TextStyle,
    /** The one big number on a mains plate. */
    val readoutLarge: TextStyle,
    /**
     * Supporting text under a row's nameplate.
     *
     * Its own style rather than bodySmall because a wrapped detail line needs
     * tighter leading than running prose: at bodySmall's ratio two wrapped
     * lines drift so far apart that the second reads as belonging to the row
     * below it.
     */
    val rowDetail: TextStyle,
    /** Countdown digits on a trip banner. */
    val countdown: TextStyle
)

val BoardType = BoardTypography(
    nameplate = TextStyle(
        fontFamily = BoardSans,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    nameplateSmall = TextStyle(
        fontFamily = BoardSans,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    sealPlate = TextStyle(
        fontFamily = BoardCondensed,
        fontWeight = FontWeight.W600,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.14.em
    ),
    sectionPlate = TextStyle(
        fontFamily = BoardCondensed,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.18.em
    ),
    stateLabel = TextStyle(
        fontFamily = BoardCondensed,
        fontWeight = FontWeight.W700,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.10.em,
        textAlign = TextAlign.End
    ),
    readout = TextStyle(
        fontFamily = BoardMono,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.01).em
    ),
    readoutLarge = TextStyle(
        fontFamily = BoardMono,
        fontWeight = FontWeight.W500,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.03).em
    ),
    rowDetail = TextStyle(
        fontFamily = BoardSans,
        fontWeight = FontWeight.W400,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    countdown = TextStyle(
        fontFamily = BoardMono,
        fontWeight = FontWeight.W700,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.04).em
    )
)

/**
 * The Material 3 scale, set in the board's own voices.
 *
 * Display, headline and title steps take [BoardDisplay]; body and label steps
 * stay in [BoardSans]. The split is the whole point of having a display face:
 * it marks the one line on a screen that names the screen or the card, and it
 * can only do that if nothing else on the screen is set in it. Jakarta's
 * x-height sits a touch lower than Archivo's, so the heading steps each gained
 * a point, and the display steps lost half their negative tracking, which is
 * what stops a geometric face from reading as squeezed beside the emblem.
 */
val BoardMaterialTypography = Typography(
    displayLarge = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.01).em),
    displayMedium = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.01).em),
    displaySmall = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 29.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em),

    // The board masthead and nothing else. Larger than the Material step it
    // occupies, because on the home screen it is a wordmark set beside a 52dp
    // emblem rather than a heading over a paragraph, and at 26sp the emblem
    // was winning. No negative tracking: that is what made the intro wordmark
    // read as squeezed, and this one sits beside the same emblem.
    headlineLarge = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 23.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 27.sp),

    titleLarge = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BoardDisplay, fontWeight = FontWeight.W700, fontSize = 15.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 13.sp, lineHeight = 18.sp),

    // Condensed and heavily tracked while everything was uppercase; both were
    // in service of caps, and both go with them. Tracking at these sizes on
    // lowercase text reads as a rendering fault, not as emphasis.
    labelLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
    labelMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
    labelSmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.01.em)
)
