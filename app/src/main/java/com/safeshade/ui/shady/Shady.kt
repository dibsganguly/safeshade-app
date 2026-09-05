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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.board
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
 * limits apply here.
 *
 * Two rules carried over from the firmware deliberately:
 *
 *  - **No speech or thought bubbles.** Text emerging from a mascot competes
 *    with the copy that actually matters on a safety screen.
 *  - **Absent during emergencies.** Enforced once in [ShadyHost], never per
 *    screen, so a new emergency surface cannot forget the rule.
 *
 * A note on the colour system: everywhere else in this app saturated colour
 * means circuit state. Shady is the single exception — it is a character, not
 * an indicator — so it never sits inside a `Way` row or beside a pilot lamp.
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
    size: Dp = 96.dp,
    /** A momentary action layered over the mood. See [ShadyPose]. */
    pose: ShadyPose = ShadyPose.Neutral
) {
    val isStatic = LocalInspectionMode.current

    // The antenna is the one part drawn OUTSIDE the amber body, so it sits on
    // the app's ground. In charcoal it vanishes against the dark scheme and
    // leaves the nub floating detached.
    val stalkInk = if (MaterialTheme.board.isDark) Color(0xFFECEFF1) else Feature

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
    val phase = if (isStatic) 0.2f else phaseAnim

    val tiltTarget = when (mood) {
        ShadyMood.WATCHING -> -2f
        ShadyMood.SEARCHING -> -4f
        ShadyMood.CONCERNED -> 3f
        ShadyMood.RESTING -> 5f
        else -> 0f
    }
    val moodTilt by animateFloatAsState(tiltTarget, tween(600), label = "shady-tilt")

    val breath = sin(phase * 2f * PI).toFloat()
    val bob = when (mood) {
        ShadyMood.OFFLINE -> 0f
        ShadyMood.RESTING -> 1.2f
        ShadyMood.CONCERNED -> 1.6f
        ShadyMood.SEARCHING -> 3.5f
        else -> 3f
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = describe(mood) }
    ) {
        val s = this.size.minDimension
        val pivot = Offset(s * 0.5f, s * 0.88f)

        translate(left = pose.offsetX * s, top = breath * bob + pose.offsetY * s) {
            rotate(degrees = moodTilt + pose.rotation, pivot = pivot) {
                // Squash and stretch anchors at the feet, so a hop lifts the
                // body rather than scaling it about its middle.
                scale(scaleX = pose.scaleX, scaleY = pose.scaleY, pivot = pivot) {
                    if (pose.facingLeft) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(s * 0.5f, s * 0.5f)) {
                            drawShady(s, mood, phase, breath, stalkInk, pose)
                        }
                    } else {
                        drawShady(s, mood, phase, breath, stalkInk, pose)
                    }
                }
            }
        }
    }
}

private fun describe(mood: ShadyMood) = when (mood) {
    ShadyMood.WATCHING -> "Shady is watching. The device is connected."
    ShadyMood.CALM -> "Shady is calm."
    ShadyMood.SEARCHING -> "Shady is looking for the device."
    ShadyMood.RESTING -> "Shady is resting."
    ShadyMood.CONCERNED -> "Shady is concerned. Something needs attention."
    ShadyMood.OFFLINE -> "Shady is offline. No connection to the device."
}

/**
 * Layout, in fractions of the square canvas.
 *
 * The body is 0.76 wide by 0.66 tall — a little wider than square, which stops
 * it reading as a plain rounded box and gives it a settled, sat-down posture.
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
    stalkInk: Color,
    pose: ShadyPose
) {
    val dim = mood == ShadyMood.OFFLINE
    val skin = if (dim) SkinDim else Skin
    val shade = if (dim) SkinDimShade else SkinShade

    drawAntenna(s, mood, phase, stalkInk)
    if (mood != ShadyMood.OFFLINE) drawLegs(s, mood, phase, skin, shade, pose)
    drawHands(s, mood, phase, skin, shade, pose)
    drawBody(s, skin, shade)
    drawFace(s, mood, phase, breath, pose)
    drawFlourish(s, pose.flourish, phase, stalkInk)
}

private fun DrawScope.drawBody(s: Float, skin: Color, shade: Color) {
    val x = s * L.BODY_X
    val y = s * L.BODY_Y
    val w = s * L.BODY_W
    val h = s * L.BODY_H
    val r = CornerRadius(s * L.CORNER, s * L.CORNER)

    // A darker plate offset downward gives weight without a drop shadow, which
    // this flat world does not permit anywhere.
    drawRoundRect(color = shade, topLeft = Offset(x, y + s * 0.025f), size = Size(w, h), cornerRadius = r)
    drawRoundRect(color = skin, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = r)
}

private fun DrawScope.drawAntenna(s: Float, mood: ShadyMood, phase: Float, stalkInk: Color) {
    val baseX = s * 0.355f
    val baseY = s * 0.25f
    val tipX = s * 0.235f
    val tipY = s * 0.085f

    val stalk = Path().apply {
        moveTo(baseX, baseY)
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

private fun DrawScope.drawHands(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    skin: Color,
    shade: Color,
    pose: ShadyPose
) {
    if (!pose.handsUp && (mood == ShadyMood.RESTING || mood == ShadyMood.OFFLINE)) return

    val rest = s * 0.62f
    val r = s * 0.055f
    val leftX = s * 0.10f
    val rightX = s * 0.90f

    if (pose.handsUp) {
        // Both up, for a wave or a celebration.
        val lift = rest - s * 0.20f
        val flap = sin(phase * 12f * PI).toFloat() * s * 0.03f
        listOf(leftX to -flap, rightX to flap).forEach { (x, dy) ->
            drawCircle(shade, radius = r, center = Offset(x, lift + dy + s * 0.014f))
            drawCircle(skin, radius = r, center = Offset(x, lift + dy))
        }
        return
    }

    val waving = mood == ShadyMood.WATCHING || mood == ShadyMood.SEARCHING
    val wave = if (waving) sin(phase * 8f * PI).toFloat() * s * 0.045f else 0f
    val rightY = if (waving) rest - s * 0.10f else rest

    drawCircle(shade, radius = r, center = Offset(leftX, rest + s * 0.014f))
    drawCircle(skin, radius = r, center = Offset(leftX, rest))
    drawCircle(shade, radius = r, center = Offset(rightX, rightY + wave + s * 0.014f))
    drawCircle(skin, radius = r, center = Offset(rightX, rightY + wave))
}

private fun DrawScope.drawLegs(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    skin: Color,
    shade: Color,
    pose: ShadyPose
) {
    val kick = if (mood == ShadyMood.WATCHING) sin(phase * 4f * PI).toFloat() * s * 0.020f else 0f
    val swing = pose.legSwing * s
    val w = s * 0.115f
    val h = s * 0.105f
    val r = CornerRadius(s * 0.055f, s * 0.055f)

    listOf(s * 0.295f to kick + swing, s * 0.590f to -kick - swing).forEach { (x, offset) ->
        drawRoundRect(shade, Offset(x, s * L.LEG_Y + offset + s * 0.012f), Size(w, h), r)
        drawRoundRect(skin, Offset(x, s * L.LEG_Y + offset), Size(w, h), r)
    }
}

private fun DrawScope.drawFace(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    breath: Float,
    pose: ShadyPose
) {
    val eyeY = s * L.EYE_Y
    val outer = s * L.EYE_OUTER
    val pupil = s * L.PUPIL

    val gaze = when {
        pose.gazeX != 0f -> pose.gazeX * outer
        pose.eyes != null -> 0f
        mood == ShadyMood.SEARCHING -> sin(phase * 2f * PI).toFloat() * outer * 0.6f
        mood == ShadyMood.CONCERNED -> -outer * 0.28f
        else -> breath * outer * 0.12f
    }

    val gazeDrop = pose.gazeY * outer

    val blinking = pose.eyes == null &&
        mood != ShadyMood.RESTING &&
        (phase % 0.5f) > 0.475f

    val style = pose.eyes ?: when {
        mood == ShadyMood.RESTING || blinking -> EyeStyle.CLOSED
        else -> EyeStyle.NORMAL
    }

    listOf(s * L.EYE_L, s * L.EYE_R).forEachIndexed { index, cx ->
        when (style) {
            EyeStyle.CLOSED -> {
                // A shallow downward arc, not a flat line: a line reads as
                // switched off, an arc reads as content.
                val lid = Path().apply {
                    moveTo(cx - outer * 0.9f, eyeY)
                    quadraticTo(cx, eyeY + outer * 0.6f, cx + outer * 0.9f, eyeY)
                }
                drawPath(lid, Feature, style = Stroke(width = s * 0.022f, cap = StrokeCap.Round))
            }
            EyeStyle.SQUINT -> {
                val lid = Path().apply {
                    moveTo(cx - outer * 0.9f, eyeY + outer * 0.25f)
                    quadraticTo(cx, eyeY - outer * 0.45f, cx + outer * 0.9f, eyeY + outer * 0.25f)
                }
                drawPath(lid, Feature, style = Stroke(width = s * 0.022f, cap = StrokeCap.Round))
            }
            EyeStyle.SLEEPY -> {
                // Half-shut is a short eye under a heavy lid line rather than
                // a full eye with its top masked off: masking would need the
                // body colour down here, which changes on the dim palette.
                drawOval(
                    color = EyeWhite,
                    topLeft = Offset(cx - outer * 0.95f, eyeY - outer * 0.12f),
                    size = Size(outer * 1.9f, outer * 1.05f)
                )
                drawCircle(
                    Feature,
                    radius = pupil * 0.85f,
                    center = Offset(cx + gaze, eyeY + gazeDrop + outer * 0.34f)
                )
                val lid = Path().apply {
                    moveTo(cx - outer * 1.0f, eyeY - outer * 0.08f)
                    quadraticTo(cx, eyeY - outer * 0.62f, cx + outer * 1.0f, eyeY - outer * 0.08f)
                }
                drawPath(lid, Feature, style = Stroke(width = s * 0.026f, cap = StrokeCap.Round))
            }
            EyeStyle.SCRUNCH -> {
                // A caret, not an arc: CLOSED is restful, this is effortful.
                val squeeze = Path().apply {
                    moveTo(cx - outer * 0.95f, eyeY + outer * 0.45f)
                    lineTo(cx, eyeY - outer * 0.42f)
                    lineTo(cx + outer * 0.95f, eyeY + outer * 0.45f)
                }
                drawPath(squeeze, Feature, style = Stroke(width = s * 0.026f, cap = StrokeCap.Round))
            }
            EyeStyle.STAR -> drawStarEye(cx, eyeY, outer)
            EyeStyle.DIZZY -> drawSpiralEye(cx, eyeY, outer, s)
            EyeStyle.WIDE, EyeStyle.NORMAL -> {
                val scale = if (style == EyeStyle.WIDE) 1.22f else 1f
                val pupilScale = if (style == EyeStyle.WIDE) 0.72f else 1f
                drawOval(
                    color = EyeWhite,
                    topLeft = Offset(cx - outer * scale, eyeY - outer * 1.2f * scale),
                    size = Size(outer * 2f * scale, outer * 2.4f * scale)
                )
                val py = eyeY + gazeDrop +
                    if (mood == ShadyMood.CONCERNED && pose.eyes == null) outer * 0.25f else 0f
                drawCircle(Feature, radius = pupil * pupilScale, center = Offset(cx + gaze, py))
                drawCircle(
                    EyeWhite,
                    radius = pupil * 0.32f,
                    center = Offset(cx + gaze + pupil * 0.35f, py - pupil * 0.35f)
                )
            }
        }
        if (index == 0) Unit
    }

    // Eyebrows are the strongest signal on the face, so they stay rare — worn
    // everywhere, they would stop meaning anything. A pose that asks for brows
    // replaces the mood's rather than stacking a second pair on top of them.
    when {
        pose.browRaise != 0f -> drawRaisedBrows(s, eyeY, outer, pose.browRaise)
        mood == ShadyMood.CONCERNED && pose.eyes == null -> drawWorriedBrows(s, eyeY, outer)
    }

    drawMouth(s, pose.mouth ?: defaultMouth(mood))
}

private fun DrawScope.drawWorriedBrows(s: Float, eyeY: Float, outer: Float) {
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

private fun DrawScope.drawRaisedBrows(s: Float, eyeY: Float, outer: Float, raise: Float) {
    // The two brows lift by different amounts. A matched pair reads as plain
    // surprise; the mismatch is what makes it read as unconvinced.
    listOf(s * L.EYE_L to 1.35f, s * L.EYE_R to 0.55f).forEach { (cx, share) ->
        val y = eyeY - outer * (1.55f + raise * share)
        drawPath(
            Path().apply {
                moveTo(cx - outer * 0.85f, y + outer * 0.22f)
                quadraticTo(cx, y - outer * 0.30f, cx + outer * 0.85f, y + outer * 0.22f)
            },
            Feature, style = Stroke(width = s * 0.024f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawStarEye(cx: Float, cy: Float, outer: Float) {
    val path = Path()
    repeat(10) { i ->
        val r = if (i % 2 == 0) outer * 1.05f else outer * 0.42f
        val a = (-PI / 2 + i * PI / 5).toFloat()
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, Feature)
}

private fun DrawScope.drawSpiralEye(cx: Float, cy: Float, outer: Float, s: Float) {
    val path = Path()
    val turns = 2.2f
    val steps = 40
    repeat(steps + 1) { i ->
        val t = i / steps.toFloat()
        val a = (t * turns * 2f * PI).toFloat()
        val r = outer * 1.0f * t
        val x = cx + cos(a) * r
        val y = cy + sin(a) * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, Feature, style = Stroke(width = s * 0.016f, cap = StrokeCap.Round))
}

private fun defaultMouth(mood: ShadyMood) = when (mood) {
    ShadyMood.WATCHING, ShadyMood.CALM -> MouthStyle.SMILE
    ShadyMood.SEARCHING -> MouthStyle.OPEN
    ShadyMood.RESTING -> MouthStyle.FLAT
    ShadyMood.CONCERNED -> MouthStyle.FROWN
    ShadyMood.OFFLINE -> MouthStyle.FLAT
}

/** The mouth is always present — a face without one reads as unfinished. */
private fun DrawScope.drawMouth(s: Float, style: MouthStyle) {
    val cx = s * 0.5f
    val y = s * L.MOUTH_Y
    val w = s * 0.10f
    val stroke = Stroke(width = s * 0.024f, cap = StrokeCap.Round)

    when (style) {
        MouthStyle.SMILE -> drawPath(
            Path().apply {
                moveTo(cx - w, y)
                quadraticTo(cx, y + s * 0.055f, cx + w, y)
            }, Feature, style = stroke
        )
        MouthStyle.BIG_SMILE -> {
            val p = Path().apply {
                moveTo(cx - w * 1.35f, y - s * 0.006f)
                quadraticTo(cx, y + s * 0.095f, cx + w * 1.35f, y - s * 0.006f)
                close()
            }
            drawPath(p, Feature)
        }
        MouthStyle.OPEN -> drawCircle(Feature, radius = s * 0.028f, center = Offset(cx, y + s * 0.014f))
        MouthStyle.FLAT -> drawLine(
            Feature,
            Offset(cx - w * 0.5f, y + s * 0.012f),
            Offset(cx + w * 0.5f, y + s * 0.012f),
            strokeWidth = s * 0.022f, cap = StrokeCap.Round
        )
        MouthStyle.FROWN -> drawPath(
            Path().apply {
                moveTo(cx - w, y + s * 0.030f)
                quadraticTo(cx, y - s * 0.022f, cx + w, y + s * 0.030f)
            }, Feature, style = stroke
        )
        MouthStyle.GASP -> drawOval(
            color = Feature,
            topLeft = Offset(cx - s * 0.026f, y - s * 0.006f),
            size = Size(s * 0.052f, s * 0.074f)
        )
        MouthStyle.SMIRK -> drawPath(
            Path().apply {
                moveTo(cx - w * 0.95f, y + s * 0.006f)
                quadraticTo(cx + w * 0.10f, y + s * 0.050f, cx + w * 1.05f, y - s * 0.016f)
            }, Feature, style = stroke
        )
        MouthStyle.WOBBLE -> drawPath(
            Path().apply {
                moveTo(cx - w, y + s * 0.012f)
                quadraticTo(cx - w * 0.33f, y - s * 0.020f, cx, y + s * 0.012f)
                quadraticTo(cx + w * 0.33f, y + s * 0.044f, cx + w, y + s * 0.012f)
            }, Feature, style = stroke
        )
    }
}

/** Small marks above the head. Never text, never a bubble. */
private fun DrawScope.drawFlourish(s: Float, flourish: Flourish, phase: Float, ink: Color) {
    when (flourish) {
        Flourish.NONE -> Unit
        Flourish.SLEEP -> {
            val drift = sin(phase * 2f * PI).toFloat() * s * 0.012f
            listOf(0.70f to 0.16f, 0.79f to 0.08f).forEachIndexed { i, (fx, fy) ->
                val size = if (i == 0) s * 0.055f else s * 0.038f
                drawZ(Offset(s * fx, s * fy + drift), size, ink, s)
            }
        }
        Flourish.SPARK -> {
            listOf(0.72f to 0.14f, 0.84f to 0.22f, 0.66f to 0.06f).forEach { (fx, fy) ->
                val c = Offset(s * fx, s * fy)
                val r = s * 0.030f
                drawLine(ink, c.copy(y = c.y - r), c.copy(y = c.y + r), s * 0.016f, StrokeCap.Round)
                drawLine(ink, c.copy(x = c.x - r), c.copy(x = c.x + r), s * 0.016f, StrokeCap.Round)
            }
        }
        Flourish.THINK -> repeat(3) { i ->
            drawCircle(
                ink.copy(alpha = 0.35f + 0.22f * i),
                radius = s * (0.014f + 0.004f * i),
                center = Offset(s * (0.70f + 0.055f * i), s * (0.15f - 0.030f * i))
            )
        }
        Flourish.PUFF -> {
            // The one flourish drawn at the feet rather than above the head:
            // kicked-up dust floating over the antenna would read as an idea
            // rather than as a landing.
            val groundY = s * 0.94f
            listOf(-1f, 1f).forEach { dir ->
                repeat(2) { i ->
                    val r = s * (0.052f + 0.028f * i)
                    val cxp = s * 0.5f + dir * s * (0.20f + 0.11f * i)
                    drawPath(
                        Path().apply {
                            moveTo(cxp - r * 0.8f, groundY)
                            quadraticTo(cxp, groundY - r, cxp + r * 0.8f, groundY)
                        },
                        ink.copy(alpha = 0.50f - 0.18f * i),
                        style = Stroke(width = s * 0.016f, cap = StrokeCap.Round)
                    )
                }
            }
        }
        Flourish.SWIRL -> {
            val path = Path()
            repeat(28) { i ->
                val t = i / 27f
                val a = (t * 1.8f * 2f * PI + phase * 2f * PI).toFloat()
                val r = s * 0.055f * t
                val x = s * 0.76f + cos(a) * r
                val y = s * 0.12f + sin(a) * r
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, ink, style = Stroke(width = s * 0.014f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawZ(at: Offset, size: Float, ink: Color, s: Float) {
    val w = size
    val h = size
    val sw = s * 0.016f
    drawLine(ink, at, at.copy(x = at.x + w), sw, StrokeCap.Round)
    drawLine(ink, at.copy(x = at.x + w), at.copy(x = at.x, y = at.y + h), sw, StrokeCap.Round)
    drawLine(ink, at.copy(x = at.x, y = at.y + h), at.copy(x = at.x + w, y = at.y + h), sw, StrokeCap.Round)
}
