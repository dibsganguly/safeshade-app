package com.safeshade.ui.shady

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import com.safeshade.ui.theme.board

/**
 * Shady — the SafeShade mascot.
 *
 * A small companion-device character: a rounded body a little wider than it is
 * tall, one curved antenna with a signal nub, and a real face. Hands, legs and
 * eyebrows come and go with the mood rather than being always-on, which is what
 * keeps a simple shape from looking static.
 *
 * This evolves the 8-bit Shady the firmware draws on a 128x64 monochrome OLED
 * rather than porting it. The hardware version is pixel-limited; none of those
 * limits apply here, so this one gets curves, colour and a much wider emotional
 * range while staying recognisably the same character.
 *
 * Two rules carried over from the firmware deliberately:
 *
 *  - **No speech or thought bubbles.** The hardware's bubble takeover is a
 *    different medium's answer to a different problem, and text emerging from a
 *    mascot competes with the copy that actually matters on a safety screen.
 *  - **Absent during emergencies.** Enforced once in [ShadyHost], never per
 *    screen, so a new emergency surface cannot forget the rule.
 *
 * A note on the colour system: everywhere else in this app saturated colour
 * means circuit state. Shady is the single exception — it is a character, not
 * an indicator — so it never sits inside a `Way` row or beside a pilot lamp
 * where the two could be read as the same language.
 */
enum class ShadyMood {
    /** Link up, everything armed. Bright and attentive, waving. */
    WATCHING,

    /** Idle and content. A slow breath. */
    CALM,

    /** Looking for the device. Eyes tracking, antenna pulsing. */
    SEARCHING,

    /** Nothing to do. Eyes closed, breathing slowly. */
    RESTING,

    /** Something is degraded: low battery, weak signal, a stale sync. */
    CONCERNED,

    /** No link at all. Dimmed, antenna dark, no movement. */
    OFFLINE
}

// Character palette. Amber skin from the brand mark; teal reserved for the
// antenna nub, which is the one part of Shady that reports something real.
private val Skin = Color(0xFFF5A623)
private val SkinShade = Color(0xFFD08508)
private val SkinDim = Color(0xFFB08A52)
private val SkinDimShade = Color(0xFF947036)
private val Feature = Color(0xFF22282E)
private val EyeWhite = Color(0xFFFFFDF7)
private val NubLive = Color(0xFF6FD3CC)
private val NubDim = Color(0xFF6B7480)

@Composable
fun Shady(
    mood: ShadyMood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val isStatic = LocalInspectionMode.current

    // The antenna is the one part of Shady drawn OUTSIDE the amber body, so it
    // sits on the app's ground rather than on the character. In charcoal that
    // is invisible against the dark scheme's ground, which left the nub
    // floating detached above the head - and the nub is the one element allowed
    // to carry state colour, so losing its stalk is not cosmetic.
    val stalkInk = if (MaterialTheme.board.isDark) Color(0xFFECEFF1) else Feature

    // One shared clock. Every animated property is a cheap function of this
    // single phase rather than half a dozen infinite transitions competing.
    val transition = rememberInfiniteTransition(label = "shady")
    val phaseAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mood) {
                    ShadyMood.SEARCHING -> 1900
                    ShadyMood.CONCERNED -> 2600
                    ShadyMood.RESTING -> 5200
                    else -> 3400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shady-phase"
    )
    // Held mid-breath in previews and screenshots so captured evidence is
    // identical from run to run.
    val phase = if (isStatic) 0.2f else phaseAnim

    // Posture. A few degrees is plenty; more reads as a toy falling over.
    val tiltTarget = when (mood) {
        ShadyMood.WATCHING -> -2f
        ShadyMood.SEARCHING -> -4f
        ShadyMood.CONCERNED -> 3f
        ShadyMood.RESTING -> 5f
        else -> 0f
    }
    val tilt by animateFloatAsState(tiltTarget, tween(600), label = "shady-tilt")

    val breath = sin(phase * 2f * PI).toFloat()
    val bob = when (mood) {
        ShadyMood.OFFLINE -> 0f
        ShadyMood.RESTING -> 1.2f
        ShadyMood.CONCERNED -> 1.6f
        ShadyMood.SEARCHING -> 3.5f
        else -> 3f
    }

    val description = when (mood) {
        ShadyMood.WATCHING -> "Shady is watching. The device is connected."
        ShadyMood.CALM -> "Shady is calm."
        ShadyMood.SEARCHING -> "Shady is looking for the device."
        ShadyMood.RESTING -> "Shady is resting."
        ShadyMood.CONCERNED -> "Shady is concerned. Something needs attention."
        ShadyMood.OFFLINE -> "Shady is offline. No connection to the device."
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description }
    ) {
        val s = this.size.minDimension
        translate(top = breath * bob) {
            rotate(degrees = tilt, pivot = Offset(s * 0.5f, s * 0.88f)) {
                drawShady(s, mood, phase, breath, stalkInk)
            }
        }
    }
}

/**
 * Layout, in fractions of the square canvas.
 *
 * The body is 0.76 wide by 0.66 tall — a little wider than square, which is
 * what stops it reading as a plain rounded box and gives it a settled,
 * sat-down posture. Antenna and legs occupy the margin that leaves.
 */
private object L {
    const val BODY_W = 0.76f
    const val BODY_H = 0.66f
    const val BODY_X = 0.12f
    const val BODY_Y = 0.21f
    const val CORNER = 0.16f

    const val EYE_Y = 0.45f
    const val EYE_L = 0.355f
    const val EYE_R = 0.645f
    const val EYE_OUTER = 0.082f
    const val PUPIL = 0.042f

    const val MOUTH_Y = 0.64f
    const val LEG_Y = 0.855f
}

private fun DrawScope.drawShady(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    breath: Float,
    stalkInk: Color
) {
    val dim = mood == ShadyMood.OFFLINE
    val skin = if (dim) SkinDim else Skin
    val shade = if (dim) SkinDimShade else SkinShade

    // Draw order matters: antenna and legs sit behind the body so they read as
    // attached rather than pasted on top.
    drawAntenna(s, mood, phase, stalkInk)
    if (mood != ShadyMood.OFFLINE) drawLegs(s, mood, phase, skin, shade)
    drawHands(s, mood, phase, skin, shade)
    drawBody(s, skin, shade)
    drawFace(s, mood, phase, breath)
}

/** The rounded, slightly wide body. */
private fun DrawScope.drawBody(s: Float, skin: Color, shade: Color) {
    val x = s * L.BODY_X
    val y = s * L.BODY_Y
    val w = s * L.BODY_W
    val h = s * L.BODY_H
    val r = CornerRadius(s * L.CORNER, s * L.CORNER)

    // A darker plate offset downward gives the character weight without a drop
    // shadow, which this flat world does not permit anywhere.
    drawRoundRect(color = shade, topLeft = Offset(x, y + s * 0.025f), size = Size(w, h), cornerRadius = r)
    drawRoundRect(color = skin, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = r)
}

/**
 * One curved antenna with a signal nub.
 *
 * The nub is the only part of Shady that reports anything real: teal and
 * pulsing while the link is live or being searched for, dark when it is not.
 */
private fun DrawScope.drawAntenna(s: Float, mood: ShadyMood, phase: Float, stalkInk: Color) {
    val baseX = s * 0.355f
    val baseY = s * 0.25f
    val tipX = s * 0.235f
    val tipY = s * 0.085f

    val stalk = Path().apply {
        moveTo(baseX, baseY)
        // A single curve leaning left, so the silhouette is asymmetric and the
        // character reads as tilted slightly toward the viewer.
        cubicTo(s * 0.345f, s * 0.17f, s * 0.30f, s * 0.115f, tipX, tipY)
    }
    drawPath(stalk, stalkInk, style = Stroke(width = s * 0.030f, cap = StrokeCap.Round))

    val pulse = when (mood) {
        ShadyMood.SEARCHING -> (sin(phase * 6f * PI).toFloat() + 1f) / 2f
        ShadyMood.WATCHING -> 0.75f + 0.25f * ((sin(phase * 2f * PI).toFloat() + 1f) / 2f)
        else -> 1f
    }
    val nub = when (mood) {
        ShadyMood.OFFLINE -> NubDim
        ShadyMood.CONCERNED -> NubDim.copy(alpha = 0.85f)
        ShadyMood.RESTING -> NubLive.copy(alpha = 0.45f)
        else -> NubLive.copy(alpha = pulse.coerceIn(0.4f, 1f))
    }
    drawCircle(stalkInk, radius = s * 0.058f, center = Offset(tipX, tipY))
    drawCircle(nub, radius = s * 0.040f, center = Offset(tipX, tipY))
}

/**
 * Little side hands.
 *
 * The right one lifts and waves when the link comes up or while searching. One
 * moving limb is enough — two reads as flailing.
 */
private fun DrawScope.drawHands(s: Float, mood: ShadyMood, phase: Float, skin: Color, shade: Color) {
    if (mood == ShadyMood.RESTING || mood == ShadyMood.OFFLINE) return

    val y = s * 0.62f
    val r = s * 0.055f
    val leftX = s * 0.10f
    val rightX = s * 0.90f

    val waving = mood == ShadyMood.WATCHING || mood == ShadyMood.SEARCHING
    val wave = if (waving) sin(phase * 8f * PI).toFloat() * s * 0.045f else 0f
    val rightY = if (waving) y - s * 0.10f else y

    drawCircle(shade, radius = r, center = Offset(leftX, y + s * 0.014f))
    drawCircle(skin, radius = r, center = Offset(leftX, y))

    drawCircle(shade, radius = r, center = Offset(rightX, rightY + wave + s * 0.014f))
    drawCircle(skin, radius = r, center = Offset(rightX, rightY + wave))
}

/** Two stubby legs that kick a little when Shady is pleased with itself. */
private fun DrawScope.drawLegs(s: Float, mood: ShadyMood, phase: Float, skin: Color, shade: Color) {
    val kick = if (mood == ShadyMood.WATCHING) sin(phase * 4f * PI).toFloat() * s * 0.020f else 0f
    val w = s * 0.115f
    val h = s * 0.105f
    val r = CornerRadius(s * 0.055f, s * 0.055f)

    listOf(s * 0.295f to kick, s * 0.590f to -kick).forEach { (x, offset) ->
        drawRoundRect(shade, Offset(x, s * L.LEG_Y + offset + s * 0.012f), Size(w, h), r)
        drawRoundRect(skin, Offset(x, s * L.LEG_Y + offset), Size(w, h), r)
    }
}

/**
 * Eyes, mouth, and — only when the mood calls for it — eyebrows.
 *
 * Eyes are a light oval with a solid pupil rather than a bare dot, which is
 * what gives them somewhere to look. Gaze direction does most of the expressive
 * work; the mouth confirms it.
 */
private fun DrawScope.drawFace(s: Float, mood: ShadyMood, phase: Float, breath: Float) {
    val eyeY = s * L.EYE_Y
    val outer = s * L.EYE_OUTER
    val pupil = s * L.PUPIL

    val gaze = when (mood) {
        ShadyMood.SEARCHING -> sin(phase * 2f * PI).toFloat() * outer * 0.6f
        ShadyMood.CONCERNED -> -outer * 0.28f
        else -> breath * outer * 0.12f
    }

    // A blink near the end of each half-cycle. Skipped while resting, where the
    // eyes are already shut.
    val blinking = mood != ShadyMood.RESTING && (phase % 0.5f) > 0.475f

    listOf(s * L.EYE_L, s * L.EYE_R).forEach { cx ->
        if (mood == ShadyMood.RESTING || blinking) {
            // A closed eye is a shallow downward arc, not a flat line: a line
            // reads as switched off, an arc reads as content.
            val lid = Path().apply {
                moveTo(cx - outer * 0.9f, eyeY)
                quadraticTo(cx, eyeY + outer * 0.6f, cx + outer * 0.9f, eyeY)
            }
            drawPath(lid, Feature, style = Stroke(width = s * 0.022f, cap = StrokeCap.Round))
        } else {
            drawOval(
                color = EyeWhite,
                topLeft = Offset(cx - outer, eyeY - outer * 1.2f),
                size = Size(outer * 2f, outer * 2.4f)
            )
            drawCircle(
                color = Feature,
                radius = pupil,
                center = Offset(
                    cx + gaze,
                    eyeY + if (mood == ShadyMood.CONCERNED) outer * 0.25f else 0f
                )
            )
            // A single specular dot. Cheap, and it is most of the difference
            // between "eye" and "hole".
            drawCircle(
                color = EyeWhite,
                radius = pupil * 0.32f,
                center = Offset(cx + gaze + pupil * 0.35f, eyeY - pupil * 0.35f)
            )
        }
    }

    // Eyebrows appear only for concern. They are the strongest signal on the
    // face, so they stay rare — used everywhere they would stop meaning anything.
    if (mood == ShadyMood.CONCERNED) {
        val browY = eyeY - outer * 1.85f
        drawLine(
            Feature,
            Offset(s * L.EYE_L - outer * 0.95f, browY + outer * 0.40f),
            Offset(s * L.EYE_L + outer * 0.65f, browY),
            strokeWidth = s * 0.024f, cap = StrokeCap.Round
        )
        drawLine(
            Feature,
            Offset(s * L.EYE_R + outer * 0.95f, browY + outer * 0.40f),
            Offset(s * L.EYE_R - outer * 0.65f, browY),
            strokeWidth = s * 0.024f, cap = StrokeCap.Round
        )
    }

    drawMouth(s, mood)
}

/** The mouth is always present — a face without one reads as unfinished. */
private fun DrawScope.drawMouth(s: Float, mood: ShadyMood) {
    val cx = s * 0.5f
    val y = s * L.MOUTH_Y
    val w = s * 0.10f
    val stroke = Stroke(width = s * 0.024f, cap = StrokeCap.Round)

    when (mood) {
        ShadyMood.WATCHING, ShadyMood.CALM -> {
            val smile = Path().apply {
                moveTo(cx - w, y)
                quadraticTo(cx, y + s * 0.055f, cx + w, y)
            }
            drawPath(smile, Feature, style = stroke)
        }
        ShadyMood.SEARCHING -> {
            // A small open mouth: mid-thought rather than mid-smile.
            drawCircle(Feature, radius = s * 0.028f, center = Offset(cx, y + s * 0.014f))
        }
        ShadyMood.RESTING -> {
            drawLine(
                Feature,
                Offset(cx - w * 0.5f, y + s * 0.012f),
                Offset(cx + w * 0.5f, y + s * 0.012f),
                strokeWidth = s * 0.022f, cap = StrokeCap.Round
            )
        }
        ShadyMood.CONCERNED -> {
            val frown = Path().apply {
                moveTo(cx - w, y + s * 0.030f)
                quadraticTo(cx, y - s * 0.022f, cx + w, y + s * 0.030f)
            }
            drawPath(frown, Feature, style = stroke)
        }
        ShadyMood.OFFLINE -> {
            drawLine(
                Feature.copy(alpha = 0.5f),
                Offset(cx - w * 0.65f, y + s * 0.012f),
                Offset(cx + w * 0.65f, y + s * 0.012f),
                strokeWidth = s * 0.022f, cap = StrokeCap.Round
            )
        }
    }
}
