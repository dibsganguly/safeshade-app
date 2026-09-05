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
 * Two voices, both drawn from one variable file each:
 *
 *  - **Archivo** carries everything. Its `wdth` axis is what makes the system
 *    work: at width 100 it is a plain workhorse grotesk for body copy, and at
 *    width 78 it becomes the condensed, engraved label-plate voice used for
 *    nameplates. One family, two registers, no visual mismatch between them.
 *  - **Azeret Mono** carries numeric readouts only — telemetry, coordinates,
 *    signal strength, timers. Anything a person reads digit-by-digit.
 *
 * Both are bundled as .ttf in res/font rather than pulled through Downloadable
 * Fonts: the Play fonts provider is unreliable on emulator images, and a font
 * that sometimes fails to arrive makes screenshot verification non-deterministic.
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
private fun azeret(weight: Int) = Font(
    resId = R.font.azeret_mono_variable,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

/** Body / UI voice — Archivo at normal width. */
val BoardSans = FontFamily(
    archivo(400, 100f),
    archivo(500, 100f),
    archivo(600, 100f),
    archivo(700, 100f)
)

/** Nameplate voice — Archivo condensed. Intended for uppercase with tracking. */
val BoardCondensed = FontFamily(
    archivo(500, 78f),
    archivo(600, 78f),
    archivo(700, 78f)
)

/** Readout voice — digits you read one at a time. */
val BoardMono = FontFamily(
    azeret(400),
    azeret(500),
    azeret(700)
)

/**
 * Styles that have no Material 3 equivalent because they belong to this world
 * rather than to the type scale. Read via `MaterialTheme.boardType`.
 */
data class BoardTypography(
    /** An engraved circuit label. Caller supplies uppercase text. */
    val nameplate: TextStyle,
    /** A smaller nameplate, for sub-rows and inline labels. */
    val nameplateSmall: TextStyle,
    /** A section heading sitting above a brass rule. */
    val sectionPlate: TextStyle,
    /** The state word on the right of a way: LIVE / OFF / TRIPPED / SEALED. */
    val stateLabel: TextStyle,
    /** Telemetry and coordinates. */
    val readout: TextStyle,
    /** The one big number on a mains plate. */
    val readoutLarge: TextStyle,
    /** Countdown digits on a trip banner. */
    val countdown: TextStyle
)

val BoardType = BoardTypography(
    nameplate = TextStyle(
        fontFamily = BoardCondensed,
        fontWeight = FontWeight.W600,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.12.em
    ),
    nameplateSmall = TextStyle(
        fontFamily = BoardCondensed,
        fontWeight = FontWeight.W600,
        fontSize = 12.sp,
        lineHeight = 16.sp,
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
    countdown = TextStyle(
        fontFamily = BoardMono,
        fontWeight = FontWeight.W700,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-0.04).em
    )
)

/** The Material 3 scale, set in the board's own voice. */
val BoardMaterialTypography = Typography(
    displayLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W700, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W700, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em),
    displaySmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W700, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em),

    headlineLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W700, fontSize = 26.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 19.sp, lineHeight = 26.sp),

    titleLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 20.sp),

    bodyLarge = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = BoardSans, fontWeight = FontWeight.W400, fontSize = 13.sp, lineHeight = 18.sp),

    labelLarge = TextStyle(fontFamily = BoardCondensed, fontWeight = FontWeight.W600, fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.08.em),
    labelMedium = TextStyle(fontFamily = BoardCondensed, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.12.em),
    labelSmall = TextStyle(fontFamily = BoardCondensed, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.14.em)
)
