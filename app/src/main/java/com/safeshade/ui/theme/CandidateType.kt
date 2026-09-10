package com.safeshade.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.safeshade.R

/**
 * Type families that exist only so the Kit gallery's v2.0 section can show
 * them, side by side with Archivo and Azeret, on a real phone.
 *
 * None of these is part of the shipped system. The Material scale and
 * [BoardType] still read Archivo and Azeret alone; nothing outside the gallery
 * may reference a family from this file until DESIGN.md promotes it, at which
 * point it moves into `Type.kt` and the rest are deleted along with their
 * `.ttf` files (eleven files, about 2.5 MB together, which is a real cost on the
 * APK and the reason this is a candidate set rather than a permanent one).
 *
 * All six are Open Font Licence faces from the google/fonts repository,
 * fetched as their variable `.ttf` so one file carries every weight.
 */
@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, weight: Int, vararg extra: FontVariation.Setting) = Font(
    resId = resId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight), *extra)
)

private val Weights = listOf(400, 500, 600, 700, 800)

/** Bricolage Grotesque: a grotesk with character in the caps and an optical size axis. Display and titles. */
val CandidateBricolage: FontFamily = FontFamily(
    Weights.map { variable(R.font.bricolage_grotesque_variable, it, FontVariation.Setting("opsz", 36f), FontVariation.width(100f)) }
)

/** Instrument Sans: a warm, plain UI grotesk with a width axis, so it can also do the condensed nameplate voice. */
val CandidateInstrument: FontFamily = FontFamily(
    Weights.map { variable(R.font.instrument_sans_variable, it, FontVariation.width(100f)) }
)

/** Instrument Sans pulled to its condensed end, for section plates and state words. */
val CandidateInstrumentCondensed: FontFamily = FontFamily(
    listOf(500, 600, 700).map { variable(R.font.instrument_sans_variable, it, FontVariation.width(75f)) }
)

/** Fraunces: a soft serif with an optical size axis. Headlines and the mains plate only; never body. */
val CandidateFraunces: FontFamily = FontFamily(
    Weights.map {
        variable(
            R.font.fraunces_variable, it,
            FontVariation.Setting("opsz", 48f),
            FontVariation.Setting("SOFT", 50f),
            FontVariation.Setting("WONK", 0f)
        )
    }
)

/** Manrope: rounded terminals, open apertures. A friendlier whole-app voice. */
val CandidateManrope: FontFamily = FontFamily(Weights.map { variable(R.font.manrope_variable, it) })

/** Plus Jakarta Sans: geometric and contemporary. A whole-app voice with more presence than Manrope. */
val CandidateJakarta: FontFamily = FontFamily(Weights.map { variable(R.font.plus_jakarta_sans_variable, it) })

/** Geist Mono: a mono with tabular figures and a clearer zero than Azeret at readout sizes. */
val CandidateGeistMono: FontFamily = FontFamily(listOf(400, 500, 700).map { variable(R.font.geist_mono_variable, it) })

/** Space Grotesk: a grotesk with a mechanical edge to its round letters. Titles. */
val CandidateSpaceGrotesk: FontFamily = FontFamily(Weights.map { variable(R.font.space_grotesk_variable, it) })

/** Figtree: a friendly geometric with a large x-height. Body. */
val CandidateFigtree: FontFamily = FontFamily(Weights.map { variable(R.font.figtree_variable, it) })

/** Outfit: geometric, light in colour, with a distinctive lowercase a. */
val CandidateOutfit: FontFamily = FontFamily(Weights.map { variable(R.font.outfit_variable, it) })

/** Inter: the plain UI face, with an optical size axis. The safe choice. */
val CandidateInter: FontFamily = FontFamily(Weights.map { variable(R.font.inter_variable, it, FontVariation.Setting("opsz", 16f)) })

/** JetBrains Mono: taller, rounder mono with a slashed zero. */
val CandidateJetBrainsMono: FontFamily = FontFamily(listOf(400, 500, 700).map { variable(R.font.jetbrains_mono_variable, it) })
