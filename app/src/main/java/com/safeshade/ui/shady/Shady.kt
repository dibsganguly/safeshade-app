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
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
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
    OFFLINE,

    /** Nothing has been set up here yet. Head tilted, one brow raised. */
    CURIOUS,

    /** A list that could have content, but does not yet. Eyes sweeping. */
    LOOKING,

    /** Caught out by something of its own doing. Wide-eyed, frozen, mouth open. */
    DUMBFOUNDED,

    /** Something just went right — a sync landed, an ack came back. Chest out, grinning. */
    PROUD,

    /** Late, in quiet hours. Heavy lids, slow breath, still awake. */
    SLEEPY,

    /** Cold weather outside. A fine shiver and a wobbly mouth. */
    CHILLY
}

// Character palette. Amber skin from the brand mark; teal reserved for the
// antenna nub, which is the one part of Shady that reports something real.
//
// Depth is cel-shaded, never gradient-shaded. The design system bans gradients
// everywhere and this character is drawn on the same panel, so the light
// comes from the upper left as three flat tones: a rim band along the lit
// edges, the skin, and a core band along the shadowed edges. Read against
// the pilot lamp's flat halo, which is the one precedent for a depth cue.
private val Skin = Color(0xFFF5A623)
private val SkinLight = Color(0xFFFFC65A)
private val SkinCore = Color(0xFFE29414)
private val SkinShade = Color(0xFFD08508)
private val SkinDim = Color(0xFFB08A52)
private val SkinDimLight = Color(0xFFC49E66)
private val SkinDimCore = Color(0xFFA37E48)
private val SkinDimShade = Color(0xFF947036)
private val Feature = Color(0xFF22282E)
private val EyeWhite = Color(0xFFFFFDF7)
private val EyeShadow = Color(0xFFD9D3C6)
private val NubLive = Color(0xFF6FD3CC)
private val NubDim = Color(0xFF6B7480)
private val Blush = Color(0xFFE5484D)

/** How far up-left the rim band and how far down-right the core band reach, in fractions of the size. */
private const val RIM = 0.034f
private const val CORE = 0.030f

@Composable
fun Shady(
    mood: ShadyMood,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    /** A momentary action layered over the mood. See [ShadyPose]. */
    pose: ShadyPose = ShadyPose.Neutral,
    /**
     * Where the character is in its container, in pixels, if the container
     * moves it around (the stage does, along the ground). Only used to
     * derive the antenna's secondary motion, which needs to know about
     * travel the pose itself does not carry.
     */
    worldX: Float = 0f
) {
    val isStatic = LocalInspectionMode.current
    val isDark = MaterialTheme.board.isDark
    val sizePx = with(LocalDensity.current) { size.toPx() }

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
                    ShadyMood.SEARCHING, ShadyMood.LOOKING -> 1900
                    ShadyMood.CONCERNED -> 2600
                    ShadyMood.CHILLY -> 2200
                    ShadyMood.RESTING, ShadyMood.DUMBFOUNDED, ShadyMood.SLEEPY -> 5200
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
        ShadyMood.SEARCHING, ShadyMood.LOOKING -> -4f
        ShadyMood.CONCERNED -> 3f
        ShadyMood.RESTING -> 5f
        // The classic quizzical head-tilt — steeper than any other mood, since
        // it is the entire message.
        ShadyMood.CURIOUS -> -8f
        ShadyMood.PROUD -> -1.5f
        ShadyMood.SLEEPY -> 4f
        else -> 0f
    }
    val moodTilt by animateFloatAsState(tiltTarget, tween(600), label = "shady-tilt")

    // The antenna is springy. It is drawn from a lag the composable derives
    // from its own motion — take-off, landing, a walk — so every beat gets
    // secondary motion without any beat having to ask for it.
    val lag = remember { AntennaLag() }
    val (lagX, lagY) = remember(pose, worldX, phase) {
        lag.step(
            x = pose.offsetX + if (sizePx > 0f) worldX / sizePx else 0f,
            y = pose.offsetY,
            rotation = pose.rotation,
            still = isStatic
        )
    }
    val antennaLagX = pose.antennaLagX + lagX
    val antennaLagY = pose.antennaLagY + lagY

    val breath = sin(phase * 2f * PI).toFloat()
    val bob = when (mood) {
        ShadyMood.OFFLINE -> 0f
        // Barely moving — a body still catching up with what it just saw.
        ShadyMood.DUMBFOUNDED -> 0.6f
        ShadyMood.RESTING, ShadyMood.SLEEPY -> 1.2f
        ShadyMood.CONCERNED, ShadyMood.CHILLY -> 1.6f
        ShadyMood.CURIOUS -> 2.2f
        ShadyMood.SEARCHING, ShadyMood.LOOKING -> 3.5f
        else -> 3f
    }
    // A fine, fast tremor on top of the breath. Only the cold mood has it.
    val shiver = if (mood == ShadyMood.CHILLY && !isStatic) sin(phase * 44f * PI).toFloat() * 1.3f else 0f

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = describe(mood) }
    ) {
        val s = this.size.minDimension
        val pivot = Offset(s * 0.5f, s * 0.88f)

        // The contact shadow sits on the ground, so it is drawn before any of
        // the body's transforms: it does not rotate with a tumble or mirror
        // with a turn, and a hop lifts the body off it rather than taking it
        // along. A flat ellipse, not a blur — nothing on this panel is soft.
        drawContactShadow(s, pose, isDark)

        // Turning narrows the silhouette on the way through edge-on. The
        // mirror alone reads as a cut; the squeeze is what makes it a turn.
        val turnSqueeze = 1f - 0.24f * pose.turn

        translate(left = pose.offsetX * s, top = breath * bob + pose.offsetY * s) {
            rotate(degrees = moodTilt + pose.rotation + shiver, pivot = pivot) {
                // Squash and stretch anchors at the feet, so a hop lifts the
                // body rather than scaling it about its middle.
                scale(scaleX = pose.scaleX * turnSqueeze, scaleY = pose.scaleY, pivot = pivot) {
                    if (pose.facingLeft) {
                        scale(scaleX = -1f, scaleY = 1f, pivot = Offset(s * 0.5f, s * 0.5f)) {
                            drawShady(s, mood, phase, breath, stalkInk, pose, antennaLagX, antennaLagY)
                        }
                    } else {
                        drawShady(s, mood, phase, breath, stalkInk, pose, antennaLagX, antennaLagY)
                    }
                }
            }
        }
    }
}

/**
 * A critically-damped follower for the antenna tip.
 *
 * The tip is pulled *against* the body's motion — back on a walk, down on
 * take-off, up on landing — and eases back to rest when the body stops.
 * Rotation feeds in too, so a spin or a tumble whips the stalk. The gains are
 * small: this is a stalk, not a whip antenna, and it has to stay legible at
 * 56dp.
 */
private class AntennaLag {
    private var lastX = Float.NaN
    private var lastY = 0f
    private var lastRot = 0f
    private var lagX = 0f
    private var lagY = 0f

    fun step(x: Float, y: Float, rotation: Float, still: Boolean): Pair<Float, Float> {
        if (still) return 0f to 0f
        if (lastX.isNaN()) {
            lastX = x; lastY = y; lastRot = rotation
            return 0f to 0f
        }
        val vx = (x - lastX).coerceIn(-0.08f, 0.08f)
        val vy = (y - lastY).coerceIn(-0.08f, 0.08f)
        val vr = (rotation - lastRot).coerceIn(-30f, 30f)
        lastX = x; lastY = y; lastRot = rotation

        val targetX = -vx * 1.6f - vr * 0.0035f
        val targetY = -vy * 1.4f
        lagX += (targetX - lagX) * 0.42f
        lagY += (targetY - lagY) * 0.42f
        // Ease toward rest so a stopped body settles rather than freezes.
        lagX *= 0.86f
        lagY *= 0.86f
        return lagX.coerceIn(-0.12f, 0.12f) to lagY.coerceIn(-0.10f, 0.10f)
    }
}

private fun DrawScope.drawContactShadow(s: Float, pose: ShadyPose, isDark: Boolean) {
    val lift = (-pose.offsetY / 0.25f).coerceIn(0f, 1f)
    val alpha = (if (isDark) 0.30f else 0.14f) * (1f - lift * 0.85f)
    if (alpha <= 0.005f) return
    val w = s * 0.60f * pose.scaleX * (1f - 0.35f * lift) * (1f - 0.20f * pose.turn)
    val h = s * 0.050f * (1f - 0.35f * lift)
    val cx = s * 0.5f + pose.offsetX * s
    val cy = s * 0.968f
    drawOval(
        color = (if (isDark) Color.Black else Feature).copy(alpha = alpha),
        topLeft = Offset(cx - w / 2f, cy - h / 2f),
        size = Size(w, h)
    )
}

private fun describe(mood: ShadyMood) = when (mood) {
    ShadyMood.WATCHING -> "Shady is watching. The device is connected."
    ShadyMood.CALM -> "Shady is calm."
    ShadyMood.SEARCHING -> "Shady is looking for the device."
    ShadyMood.RESTING -> "Shady is resting."
    ShadyMood.CONCERNED -> "Shady is concerned. Something needs attention."
    ShadyMood.OFFLINE -> "Shady is offline. No connection to the device."
    ShadyMood.CURIOUS -> "Shady is curious. Nothing has been set up here yet."
    ShadyMood.LOOKING -> "Shady is looking around for something to show."
    ShadyMood.DUMBFOUNDED -> "Shady is stumped."
    ShadyMood.PROUD -> "Shady is pleased. Something just went right."
    ShadyMood.SLEEPY -> "Shady is sleepy. It is late."
    ShadyMood.CHILLY -> "Shady is cold. It is cold outside."
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

/** The three flat tones a part is shaded with. */
private class Tones(val light: Color, val skin: Color, val core: Color, val shade: Color)

private fun DrawScope.drawShady(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    breath: Float,
    stalkInk: Color,
    pose: ShadyPose,
    antennaLagX: Float,
    antennaLagY: Float
) {
    val dim = mood == ShadyMood.OFFLINE
    val tones = if (dim) Tones(SkinDimLight, SkinDim, SkinDimCore, SkinDimShade)
    else Tones(SkinLight, Skin, SkinCore, SkinShade)

    drawAntenna(s, mood, phase, stalkInk, pose.turn, antennaLagX, antennaLagY)
    if (mood != ShadyMood.OFFLINE) drawLegs(s, mood, phase, tones, pose)
    drawHands(s, mood, phase, tones, pose)
    drawBody(s, tones)
    drawFace(s, mood, phase, breath, pose)
    // A pose that asks for a specific flourish wins; otherwise the mood picks
    // one, so a call site that only has a mood to give still gets the mark
    // that goes with it, rather than nothing.
    val flourish = if (pose.flourish != Flourish.NONE) pose.flourish else defaultFlourish(mood)
    drawFlourish(s, flourish, phase, stalkInk)
}

private fun defaultFlourish(mood: ShadyMood): Flourish = when (mood) {
    ShadyMood.CURIOUS -> Flourish.QUESTION
    ShadyMood.LOOKING -> Flourish.MAGNIFY
    else -> Flourish.NONE
}

private fun roundRectPath(x: Float, y: Float, w: Float, h: Float, r: Float): Path =
    Path().apply { addRoundRect(RoundRect(Rect(x, y, x + w, y + h), CornerRadius(r, r))) }

/**
 * The body, cel-shaded.
 *
 * Four flat layers: the shade plate underneath (weight), the skin, then a
 * *core* band along the bottom and right edges and a *rim* band along the top
 * and left. Each band is the body's own silhouette minus the same silhouette
 * pushed diagonally — so the bands follow the rounded corners exactly and
 * taper to nothing where the light grazes, which a stroked outline never
 * would. A single small sheen near the lit corner finishes it.
 */
private fun DrawScope.drawBody(s: Float, tones: Tones) {
    val x = s * L.BODY_X
    val y = s * L.BODY_Y
    val w = s * L.BODY_W
    val h = s * L.BODY_H
    val r = s * L.CORNER
    val corner = CornerRadius(r, r)

    // A darker plate offset downward gives weight without a drop shadow, which
    // this flat world does not permit anywhere.
    drawRoundRect(color = tones.shade, topLeft = Offset(x, y + s * 0.025f), size = Size(w, h), cornerRadius = corner)
    drawRoundRect(color = tones.skin, topLeft = Offset(x, y), size = Size(w, h), cornerRadius = corner)

    val body = roundRectPath(x, y, w, h, r)
    val core = Path().apply {
        op(body, roundRectPath(x - s * CORE, y - s * CORE, w, h, r), PathOperation.Difference)
    }
    drawPath(core, tones.core)
    val rim = Path().apply {
        op(body, roundRectPath(x + s * RIM, y + s * RIM, w, h, r), PathOperation.Difference)
    }
    drawPath(rim, tones.light)

    // The sheen: one small flat highlight where the corner faces the light.
    drawRoundRect(
        color = tones.light,
        topLeft = Offset(x + s * 0.075f, y + s * 0.060f),
        size = Size(s * 0.075f, s * 0.030f),
        cornerRadius = CornerRadius(s * 0.015f, s * 0.015f)
    )
}

private fun DrawScope.drawAntenna(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    stalkInk: Color,
    turn: Float,
    lagX: Float,
    lagY: Float
) {
    // Edge-on, the antenna sits on the centreline; square-on it is off to the
    // left. Sliding the base with the turn is most of what sells the rotation.
    val slide = turn * (s * 0.5f - s * 0.355f)
    val baseX = s * 0.355f + slide
    val baseY = s * 0.25f
    // The tip trails the base: lag is applied to the tip and, at half
    // strength, to the control point nearest it, so the stalk bends rather
    // than hinges.
    val tipX = s * 0.235f + slide + lagX * s
    val tipY = s * 0.085f + lagY * s

    val stalk = Path().apply {
        moveTo(baseX, baseY)
        cubicTo(
            s * 0.345f + slide, s * 0.17f,
            s * 0.30f + slide + lagX * s * 0.5f, s * 0.115f + lagY * s * 0.5f,
            tipX, tipY
        )
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
    // A specular dot on the glass, up and to the left like everything else.
    drawCircle(EyeWhite.copy(alpha = 0.55f), radius = s * 0.011f, center = Offset(tipX - s * 0.013f, tipY - s * 0.014f))
}

/** A hand or a foot: shade plate, skin, and one flat highlight facing the light. */
private fun DrawScope.drawNub(cx: Float, cy: Float, r: Float, tones: Tones, s: Float) {
    drawCircle(tones.shade, radius = r, center = Offset(cx, cy + s * 0.014f))
    drawCircle(tones.skin, radius = r, center = Offset(cx, cy))
    drawCircle(tones.light, radius = r * 0.34f, center = Offset(cx - r * 0.30f, cy - r * 0.32f))
}

private fun DrawScope.drawHands(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    tones: Tones,
    pose: ShadyPose
) {
    if (!pose.handsUp && (mood == ShadyMood.RESTING || mood == ShadyMood.OFFLINE || mood == ShadyMood.SLEEPY)) return

    val rest = s * 0.62f
    val r = s * 0.055f
    val leftX = s * 0.10f
    val rightX = s * 0.90f

    if (pose.handsUp) {
        // Both up, for a wave or a celebration.
        val lift = rest - s * 0.20f
        val flap = sin(phase * 12f * PI).toFloat() * s * 0.03f
        listOf(leftX to -flap, rightX to flap).forEach { (x, dy) -> drawNub(x, lift + dy, r, tones, s) }
        return
    }

    // Cold: hands tucked in close to the body, no wave.
    if (mood == ShadyMood.CHILLY) {
        drawNub(leftX + s * 0.05f, rest + s * 0.02f, r, tones, s)
        drawNub(rightX - s * 0.05f, rest + s * 0.02f, r, tones, s)
        return
    }

    val waving = mood == ShadyMood.WATCHING || mood == ShadyMood.SEARCHING
    val wave = if (waving) sin(phase * 8f * PI).toFloat() * s * 0.045f else 0f
    // Proud: hands on hips, a touch wider and lower.
    val hips = if (mood == ShadyMood.PROUD) s * 0.03f else 0f
    val rightY = if (waving) rest - s * 0.10f else rest + hips

    drawNub(leftX - hips * 0.5f, rest + hips, r, tones, s)
    drawNub(rightX + hips * 0.5f, rightY + wave, r, tones, s)
}

private fun DrawScope.drawLegs(
    s: Float,
    mood: ShadyMood,
    phase: Float,
    tones: Tones,
    pose: ShadyPose
) {
    val kick = if (mood == ShadyMood.WATCHING) sin(phase * 4f * PI).toFloat() * s * 0.020f else 0f
    val swing = pose.legSwing * s
    val w = s * 0.115f
    val h = s * 0.105f
    val r = CornerRadius(s * 0.055f, s * 0.055f)

    listOf(s * 0.295f to kick + swing, s * 0.590f to -kick - swing).forEach { (x, offset) ->
        val y = s * L.LEG_Y + offset
        drawRoundRect(tones.shade, Offset(x, y + s * 0.012f), Size(w, h), r)
        drawRoundRect(tones.skin, Offset(x, y), Size(w, h), r)
        // The body sits on the legs: a core band where it occludes them.
        drawRoundRect(tones.core, Offset(x, y), Size(w, h * 0.30f), r)
        drawRoundRect(tones.light, Offset(x + w * 0.16f, y + h * 0.40f), Size(w * 0.30f, h * 0.18f), CornerRadius(s * 0.012f, s * 0.012f))
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
        mood == ShadyMood.SEARCHING || mood == ShadyMood.LOOKING ->
            sin(phase * 2f * PI).toFloat() * outer * 0.6f
        mood == ShadyMood.CONCERNED -> -outer * 0.28f
        // Off to one side rather than centred — a straight-ahead stare would
        // read as surprise, not curiosity.
        mood == ShadyMood.CURIOUS -> outer * 0.32f
        // Fixed dead ahead. A dumbfounded stare does not wander.
        mood == ShadyMood.DUMBFOUNDED -> 0f
        else -> breath * outer * 0.12f
    }
    // Turning pulls the eyes toward the centreline with the antenna.
    val turnPull = pose.turn * outer * 0.9f

    val gazeDrop = pose.gazeY * outer

    val blinking = pose.eyes == null &&
        mood != ShadyMood.RESTING &&
        mood != ShadyMood.DUMBFOUNDED &&
        (phase % 0.5f) > 0.475f

    val baseStyle = pose.eyes ?: when {
        mood == ShadyMood.RESTING || blinking -> EyeStyle.CLOSED
        mood == ShadyMood.DUMBFOUNDED -> EyeStyle.WIDE
        mood == ShadyMood.SLEEPY -> EyeStyle.SLEEPY
        mood == ShadyMood.CHILLY -> EyeStyle.SQUINT
        else -> EyeStyle.NORMAL
    }

    listOf(s * L.EYE_L + turnPull, s * L.EYE_R - turnPull).forEachIndexed { index, cx ->
        // A wink is one closed eye and one open one; which is which is fixed so
        // the mirrored character still winks the eye nearer the camera.
        val style = when {
            baseStyle == EyeStyle.WINK && index == 1 -> EyeStyle.CLOSED
            baseStyle == EyeStyle.WINK -> EyeStyle.NORMAL
            else -> baseStyle
        }
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
            EyeStyle.HEART -> drawHeart(cx, eyeY, outer * 1.05f, Blush)
            EyeStyle.DIZZY -> drawSpiralEye(cx, eyeY, outer, s)
            EyeStyle.WINK -> Unit
            EyeStyle.WIDE, EyeStyle.NORMAL -> {
                val scale = if (style == EyeStyle.WIDE) 1.22f else 1f
                val pupilScale = if (style == EyeStyle.WIDE) 0.72f else 1f
                drawOval(
                    color = EyeWhite,
                    topLeft = Offset(cx - outer * scale, eyeY - outer * 1.2f * scale),
                    size = Size(outer * 2f * scale, outer * 2.4f * scale)
                )
                // A flat crescent where the upper lid shades the white. It is
                // what turns a white oval into a wet eye set into a face.
                drawArc(
                    color = EyeShadow,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(cx - outer * scale * 0.86f, eyeY - outer * 1.2f * scale * 0.86f),
                    size = Size(outer * 2f * scale * 0.86f, outer * 2.4f * scale * 0.86f),
                    style = Stroke(width = s * 0.020f)
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
        // The same mismatched pair a raised eyebrow uses elsewhere — one brow
        // higher than the other is what turns "surprised" into "unconvinced".
        mood == ShadyMood.CURIOUS && pose.eyes == null -> drawRaisedBrows(s, eyeY, outer, 1f)
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
    ShadyMood.SEARCHING, ShadyMood.LOOKING -> MouthStyle.OPEN
    ShadyMood.RESTING -> MouthStyle.FLAT
    ShadyMood.CONCERNED -> MouthStyle.FROWN
    ShadyMood.OFFLINE -> MouthStyle.FLAT
    // Lopsided, not sunny — the same shape a raised eyebrow pairs with
    // elsewhere in the reactor's subtle set.
    ShadyMood.CURIOUS -> MouthStyle.SMIRK
    // A tall "oh" — caught with nothing to say.
    ShadyMood.DUMBFOUNDED -> MouthStyle.GASP
    ShadyMood.PROUD -> MouthStyle.GRIN
    ShadyMood.SLEEPY -> MouthStyle.FLAT
    ShadyMood.CHILLY -> MouthStyle.WOBBLE
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
        MouthStyle.TONGUE -> {
            // The big smile, with a tongue poking out of its lower edge.
            val p = Path().apply {
                moveTo(cx - w * 1.25f, y - s * 0.004f)
                quadraticTo(cx, y + s * 0.085f, cx + w * 1.25f, y - s * 0.004f)
                close()
            }
            drawPath(p, Feature)
            drawOval(
                color = Blush,
                topLeft = Offset(cx + w * 0.05f, y + s * 0.020f),
                size = Size(s * 0.050f, s * 0.046f)
            )
        }
        MouthStyle.WHISTLE -> drawCircle(
            Feature, radius = s * 0.020f, center = Offset(cx + s * 0.010f, y + s * 0.014f),
            style = Stroke(width = s * 0.020f)
        )
        MouthStyle.GRIN -> {
            val p = Path().apply {
                moveTo(cx - w * 1.5f, y - s * 0.010f)
                quadraticTo(cx, y + s * 0.100f, cx + w * 1.5f, y - s * 0.010f)
                close()
            }
            drawPath(p, Feature)
            // A band of teeth along the top edge; flat, no individual teeth.
            drawRect(EyeWhite, Offset(cx - w * 1.15f, y - s * 0.006f), Size(w * 2.3f, s * 0.022f))
        }
    }
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + r * 0.95f)
        cubicTo(cx - r * 1.5f, cy - r * 0.10f, cx - r * 0.7f, cy - r * 1.15f, cx, cy - r * 0.35f)
        cubicTo(cx + r * 0.7f, cy - r * 1.15f, cx + r * 1.5f, cy - r * 0.10f, cx, cy + r * 0.95f)
        close()
    }
    drawPath(path, color)
}

/** Small marks above the head. Never text, never a bubble. */
private fun DrawScope.drawFlourish(s: Float, flourish: Flourish, phase: Float, ink: Color) {
    when (flourish) {
        Flourish.NONE -> Unit
        Flourish.BLUSH -> {
            // On the cheeks, below the eyes and outside the mouth's reach.
            listOf(s * 0.285f, s * 0.715f).forEach { cx ->
                drawOval(
                    color = Blush.copy(alpha = 0.55f),
                    topLeft = Offset(cx - s * 0.046f, s * 0.555f),
                    size = Size(s * 0.092f, s * 0.040f)
                )
            }
        }
        Flourish.NOTES -> {
            // Two quaver glyphs drifting up and to the right, each a head, a
            // stem and a flag. Glyphs, not letters.
            listOf(0f to 0f, 0.09f to -0.07f).forEachIndexed { i, (dx, dy) ->
                val rise = ((phase + i * 0.5f) % 1f)
                val hx = s * (0.72f + dx) + sin(rise * 2f * PI).toFloat() * s * 0.012f
                val hy = s * (0.16f + dy) - rise * s * 0.06f
                val a = ink.copy(alpha = 1f - rise * 0.8f)
                drawOval(a, Offset(hx - s * 0.020f, hy - s * 0.014f), Size(s * 0.040f, s * 0.028f))
                drawLine(a, Offset(hx + s * 0.018f, hy), Offset(hx + s * 0.018f, hy - s * 0.070f), s * 0.014f, StrokeCap.Round)
                drawPath(
                    Path().apply {
                        moveTo(hx + s * 0.018f, hy - s * 0.070f)
                        quadraticTo(hx + s * 0.050f, hy - s * 0.060f, hx + s * 0.040f, hy - s * 0.030f)
                    },
                    a, style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
                )
            }
        }
        Flourish.SWEAT -> {
            val fall = phase % 1f
            val cx = s * 0.80f
            val cy = s * 0.30f + fall * s * 0.06f
            val drop = Path().apply {
                moveTo(cx, cy - s * 0.040f)
                quadraticTo(cx + s * 0.030f, cy + s * 0.004f, cx, cy + s * 0.022f)
                quadraticTo(cx - s * 0.030f, cy + s * 0.004f, cx, cy - s * 0.040f)
                close()
            }
            drawPath(drop, NubLive.copy(alpha = 0.9f - fall * 0.5f))
        }
        Flourish.HEART -> {
            val beat = 1f + 0.10f * ((sin(phase * 6f * PI).toFloat() + 1f) / 2f)
            drawHeart(s * 0.76f, s * 0.13f, s * 0.045f * beat, Blush)
        }
        Flourish.LEAF -> {
            // A leaf tumbling down past the head on a slow sway.
            val fall = phase % 1f
            val cx = s * 0.78f + sin(fall * 4f * PI).toFloat() * s * 0.05f
            val cy = s * 0.02f + fall * s * 0.36f
            rotate(degrees = sin(fall * 4f * PI).toFloat() * 40f, pivot = Offset(cx, cy)) {
                val leaf = Path().apply {
                    moveTo(cx - s * 0.040f, cy)
                    quadraticTo(cx, cy - s * 0.036f, cx + s * 0.040f, cy)
                    quadraticTo(cx, cy + s * 0.036f, cx - s * 0.040f, cy)
                    close()
                }
                drawPath(leaf, Color(0xFF7C9A6B).copy(alpha = 1f - fall * 0.6f))
                drawLine(ink.copy(alpha = 0.5f), Offset(cx - s * 0.034f, cy), Offset(cx + s * 0.034f, cy), s * 0.008f, StrokeCap.Round)
            }
        }
        Flourish.STEAM -> {
            // Three wisps rising from between the hands, drawn low so they read
            // as coming off something held rather than as an idea.
            repeat(3) { i ->
                val t = (phase * 1.5f + i * 0.33f) % 1f
                val x = s * (0.40f + 0.10f * i) + sin(t * 3f * PI + i).toFloat() * s * 0.020f
                val y = s * 0.60f - t * s * 0.14f
                drawPath(
                    Path().apply {
                        moveTo(x, y + s * 0.030f)
                        quadraticTo(x + s * 0.016f, y + s * 0.010f, x, y - s * 0.010f)
                        quadraticTo(x - s * 0.016f, y - s * 0.030f, x, y - s * 0.050f)
                    },
                    ink.copy(alpha = 0.45f * (1f - t)),
                    style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
                )
            }
        }
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
        Flourish.QUESTION -> {
            val cx = s * 0.755f
            val hook = Path().apply {
                moveTo(cx - s * 0.032f, s * 0.075f)
                cubicTo(
                    cx - s * 0.032f, s * 0.030f,
                    cx + s * 0.048f, s * 0.030f,
                    cx + s * 0.048f, s * 0.075f
                )
                cubicTo(
                    cx + s * 0.048f, s * 0.108f,
                    cx, s * 0.100f,
                    cx, s * 0.140f
                )
            }
            drawPath(hook, ink, style = Stroke(width = s * 0.020f, cap = StrokeCap.Round))
            drawCircle(ink, radius = s * 0.013f, center = Offset(cx, s * 0.172f))
        }
        Flourish.MAGNIFY -> {
            // Held up over the shoulder, not floating in the corner.
            //
            // Three things were wrong with the first version and each one was
            // only visible at the size this is actually drawn. The lens was
            // 0.044 of the canvas, which at the 64dp `EmptyBay` calls it from
            // is a ring five pixels across - a smudge, not a glass. The handle
            // pointed down and to the *right*, away from Shady and off into
            // empty canvas, so nothing connected the mark to the character
            // holding it. And the whole thing slid sideways, which reads as
            // drifting rather than as scanning.
            //
            // Now: nearly twice the lens, the handle swung to 135 degrees so it
            // points back down towards the head, and the wobble is a small
            // rotation about the lens rather than a translation - the motion a
            // wrist makes. The top-right quadrant is free at every mood (the
            // antenna is on the left, from x 0.235 to 0.355), so this sits in
            // the one part of the frame nothing else uses.
            val cx = s * 0.795f
            val cy = s * 0.145f
            val r = s * 0.082f
            val tilt = sin(phase * 2f * PI).toFloat() * 7f

            rotate(degrees = tilt, pivot = Offset(cx, cy)) {
                val handleAngle = PI.toFloat() * 0.75f
                val hx = cx + cos(handleAngle) * r
                val hy = cy + sin(handleAngle) * r
                // Handle first, so the lens rim is drawn over the join and the
                // two read as one object rather than as a stick touching a ring.
                drawLine(
                    ink,
                    Offset(hx, hy),
                    Offset(hx + cos(handleAngle) * s * 0.075f, hy + sin(handleAngle) * s * 0.075f),
                    strokeWidth = s * 0.026f,
                    cap = StrokeCap.Round
                )
                drawCircle(ink, radius = r, style = Stroke(width = s * 0.026f), center = Offset(cx, cy))
                drawArc(
                    color = ink.copy(alpha = 0.5f),
                    startAngle = -155f,
                    sweepAngle = 60f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.58f, cy - r * 0.58f),
                    size = Size(r * 1.16f, r * 1.16f),
                    style = Stroke(width = s * 0.014f, cap = StrokeCap.Round)
                )
            }
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
