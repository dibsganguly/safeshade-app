package com.safeshade.ui.screens.device

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.data.PersonaMode
import com.safeshade.ui.shady.EyeStyle
import com.safeshade.ui.shady.Flourish
import com.safeshade.ui.shady.MouthStyle
import com.safeshade.ui.shady.Shady
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.shady.ShadyPose
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.accents
import com.safeshade.ui.theme.board
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

/**
 * A looping scene per adaptive mode: Shady doing the thing the mode is for.
 *
 * Mode names alone do very little work. "Helmet" and "Backpack" are both just
 * words on a card, and a person choosing a profile for someone else is really
 * asking "which of these is the one where the device is on a building site".
 * A cane and a careful gait answers that before the blurb is read, which is the
 * whole reason these exist.
 *
 * Why Compose Canvas and not a GIF or a Lottie file: three requirements rule
 * those out together. The props have to recolour with the theme, and a baked
 * asset carries a light-mode palette into a dark screen. They have to sit on
 * the same character the rest of the app uses, so they must compose with the
 * live [Shady] and its pose system rather than duplicate a drawing of it. And
 * they have to render in a preview, which is where every one of these was
 * actually designed. Vector drawing costs a few paths a frame; the alternatives
 * cost a parallel art pipeline and a second, drifting Shady.
 *
 * Every prop colour comes from `MaterialTheme.board` — the card's decorative
 * accent for the prop itself, ink and plate tokens for straps, buckles and
 * highlights. Nothing here hardcodes a hex value, so a scene that reads in the
 * lit room reads in the hallway at night.
 *
 * A note on what the scenes deliberately do *not* do: none of them uses
 * [ShadyPose.handsUp], and none asks for a mood other than [ShadyMood.CALM].
 * Both restrictions exist for the same reason — see [withPose].
 */

/**
 * The frame a scene holds on when it is not running.
 *
 * A seventh of the way in rather than zero: most of these cycles start at the
 * bottom of a step or a hop, and a still frame at the bottom of a hop is a
 * character standing still. Picked so all eight land somewhere legible, which
 * is checkable in the previews at the foot of this file.
 */
private const val HELD_FRAME = 0.15f

/**
 * One mode's scene, sized square.
 *
 * @param accent the card's decorative identity colour. Props take it directly;
 *   it is never used to say anything about state.
 * @param playing false freezes the scene at [HELD_FRAME]. Previews and
 *   screenshots freeze themselves via `LocalInspectionMode`; this parameter is
 *   for a caller that knows the scene is out of sight. It is deliberately not
 *   wired to pager position — a page that thaws as it becomes current snaps out
 *   of a frozen pose in the middle of the swipe, exactly where the eye is.
 */
@Composable
fun ModeScene(
    mode: PersonaMode,
    accent: Color,
    modifier: Modifier = Modifier,
    size: Dp = 116.dp,
    playing: Boolean = true
) {
    val t = sceneProgress(mode.scenePeriodMs, playing)
    val colors = MaterialTheme.board
    val paint = ScenePaint(
        prop = accent,
        // Blended toward ink rather than darkened by a fixed amount, so the
        // shade stays a shade in both themes instead of vanishing in one.
        propShade = lerp(accent, colors.ink, if (colors.isDark) 0.34f else 0.24f),
        strap = colors.inkMuted,
        light = colors.plate
    )
    val pose = scenePose(mode, t)

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension
            drawGround(s, paint)
            drawWorld(mode, t, s, paint)
            withPose(pose, s) { drawBehind(mode, t, s, paint) }
        }
        Shady(mood = ShadyMood.CALM, size = size, pose = pose)
        Canvas(Modifier.size(size)) {
            val s = this.size.minDimension
            withPose(pose, s) { drawInFront(mode, t, s, paint) }
        }
    }
}

/**
 * The loop, as a 0..1 phase.
 *
 * Driven by [withFrameMillis] rather than an infinite transition because these
 * cycles are read modulo their period — a hop is the positive half of a sine, a
 * hint fades in and out four times per revolution — and a raw frame clock is
 * the honest way to express that. It also means one float drives every layer of
 * a scene, so a prop cannot drift out of step with the character holding it.
 */
@Composable
private fun sceneProgress(periodMs: Int, playing: Boolean): Float {
    val still = LocalInspectionMode.current || !playing
    var t by remember { mutableFloatStateOf(HELD_FRAME) }
    LaunchedEffect(still, periodMs) {
        if (still) {
            t = HELD_FRAME
            return@LaunchedEffect
        }
        var start = -1L
        while (true) {
            withFrameMillis { now ->
                if (start < 0L) start = now
                t = ((now - start) % periodMs) / periodMs.toFloat()
            }
        }
    }
    return t
}

/** How long one revolution of each scene takes. Pace is characterisation. */
private val PersonaMode.scenePeriodMs: Int
    get() = when (this) {
        PersonaMode.AUTO -> 7200
        PersonaMode.ELDERLY -> 4200
        PersonaMode.KIDS -> 1500
        PersonaMode.BIKE -> 1100
        PersonaMode.PET -> 900
        PersonaMode.HELMET -> 3600
        PersonaMode.WRIST -> 2600
        PersonaMode.BACKPACK -> 2200
    }

/** Prop palette, resolved once per scene from the theme and the card's accent. */
private data class ScenePaint(
    val prop: Color,
    val propShade: Color,
    val strap: Color,
    val light: Color
)

/** Scales a token's alpha, for the AUTO scene's ghosted hints. */
private fun Color.at(multiplier: Float): Color =
    if (multiplier >= 1f) this else copy(alpha = alpha * multiplier)

// ============================================================================
// Posing
// ============================================================================

/**
 * Mirrors the pose transform [Shady] applies inside its own canvas, so a prop
 * drawn in a neighbouring layer leans, hops and settles with the character
 * rather than beside it.
 *
 * Two constraints on the scenes fall out of this being a mirror rather than a
 * shared transform, and both are load-bearing:
 *
 *  - Mood stays [ShadyMood.CALM]. Every other mood adds a tilt of its own
 *    inside Shady, which this cannot see, and the props would lean while the
 *    character stood straight.
 *  - No [ShadyPose.handsUp], and no waving mood. Both animate the hands off
 *    Shady's own internal phase, which is not this scene's clock. A cane held
 *    by a hand on a different clock is a cane sliding through a fist.
 *
 * The one thing not mirrored is Shady's idle breath, worth up to three pixels
 * of vertical bob. Reproducing it would mean reproducing an internal easing;
 * three pixels of give between a character and its hat is beneath notice, and
 * arguably reads as the prop being a separate object.
 */
private fun DrawScope.withPose(pose: ShadyPose, s: Float, block: DrawScope.() -> Unit) {
    val pivot = Offset(s * 0.5f, s * 0.88f)
    translate(left = pose.offsetX * s, top = pose.offsetY * s) {
        rotate(degrees = pose.rotation, pivot = pivot) {
            scale(scaleX = pose.scaleX, scaleY = pose.scaleY, pivot = pivot) {
                block()
            }
        }
    }
}

/** What the character is doing at phase [t]. */
private fun scenePose(mode: PersonaMode, t: Float): ShadyPose {
    val turn = (t * 2f * PI).toFloat()
    return when (mode) {
        // Deliberate, and slightly stooped. The lean is constant and the sway
        // is small: this is a gait being managed, not performed.
        PersonaMode.ELDERLY -> {
            val step = sin(turn)
            ShadyPose(
                offsetY = -0.012f * abs(step),
                rotation = 2.4f + step * 1.2f,
                mouth = MouthStyle.SMILE
            )
        }
        // A skip, not a walk — the take-off squashes and the flight stretches,
        // which is most of the difference between a child moving and an adult
        // moving.
        PersonaMode.KIDS -> {
            val hop = max(0f, sin(turn))
            ShadyPose(
                offsetY = -0.15f * hop,
                rotation = 3f * hop,
                scaleX = 1f - 0.05f * hop,
                scaleY = 1f + 0.07f * hop,
                mouth = MouthStyle.BIG_SMILE,
                flourish = if (hop > 0.9f) Flourish.SPARK else Flourish.NONE
            )
        }
        // Head down over the bars. The lean is the whole read, so it is large
        // and constant; the pedal bob is what stops it looking like a photo.
        PersonaMode.BIKE -> ShadyPose(
            offsetY = -0.010f * abs(sin(turn * 2f)),
            rotation = 11f + sin(turn * 2f) * 1.6f,
            mouth = MouthStyle.SMILE
        )
        PersonaMode.PET -> ShadyPose(
            offsetY = -0.010f * abs(sin(turn)),
            rotation = sin(turn) * 2f,
            mouth = MouthStyle.BIG_SMILE
        )
        // Stood still, and once a cycle a nod: the worker check-in this profile
        // is named for. Stillness is the characterisation here.
        PersonaMode.HELMET -> {
            val nod = if (t < 0.22f) sin(t / 0.22f * PI).toFloat() else 0f
            ShadyPose(
                offsetY = 0.010f * nod,
                rotation = 4.5f * nod,
                eyes = if (nod > 0.55f) EyeStyle.CLOSED else null,
                mouth = MouthStyle.FLAT
            )
        }
        // Everyday and unhurried. The squint late in the cycle is a glance down
        // at the band, which is as close to checking a watch as a character
        // with no separately posable arms can get.
        PersonaMode.WRIST -> ShadyPose(
            offsetY = -0.010f * abs(sin(turn * 2f)),
            rotation = sin(turn) * 1.6f,
            eyes = if (t > 0.56f && t < 0.76f) EyeStyle.SQUINT else null,
            mouth = MouthStyle.SMILE
        )
        PersonaMode.BACKPACK -> ShadyPose(
            offsetY = -0.018f * abs(sin(turn)),
            rotation = 2f + sin(turn) * 2f,
            mouth = MouthStyle.SMILE
        )
        // Weighing it up. The tilt turns twice per revolution while the hints
        // change four times, so the character is never quite in step with the
        // thing it is considering — which is roughly what deciding looks like.
        PersonaMode.AUTO -> ShadyPose(
            rotation = sin(turn * 2f) * -3f,
            eyes = EyeStyle.SQUINT,
            mouth = MouthStyle.FLAT,
            flourish = Flourish.THINK
        )
    }
}

// ============================================================================
// Layers
// ============================================================================

/** Untransformed scenery: things the character moves through, not with. */
private fun DrawScope.drawWorld(mode: PersonaMode, t: Float, s: Float, paint: ScenePaint) {
    if (mode == PersonaMode.BIKE) drawSpeedLines(s, t, paint)
}

private fun DrawScope.drawBehind(mode: PersonaMode, t: Float, s: Float, paint: ScenePaint) {
    when (mode) {
        PersonaMode.KIDS -> drawSchoolPack(s, paint)
        PersonaMode.PET -> drawTail(s, t, paint)
        PersonaMode.BACKPACK -> drawCommuterBag(s, paint)
        PersonaMode.AUTO -> {
            val hint = autoHint(t)
            if (hint.slot == 1) drawSchoolPack(s, paint, hint.fade)
        }
        else -> Unit
    }
}

private fun DrawScope.drawInFront(mode: PersonaMode, t: Float, s: Float, paint: ScenePaint) {
    when (mode) {
        PersonaMode.ELDERLY -> drawCane(s, t, paint)
        PersonaMode.KIDS -> drawPackStrap(s, paint)
        PersonaMode.BIKE -> drawCycleHelmet(s, paint)
        PersonaMode.PET -> drawCollar(s, paint)
        PersonaMode.HELMET -> drawHardHat(s, paint)
        PersonaMode.WRIST -> drawWristband(s, t, paint)
        PersonaMode.BACKPACK -> drawBagStrap(s, paint)
        PersonaMode.AUTO -> {
            val hint = autoHint(t)
            when (hint.slot) {
                0 -> drawCane(s, t, paint, hint.fade)
                2 -> drawHardHat(s, paint, hint.fade)
                3 -> drawCollar(s, paint, hint.fade)
                else -> Unit
            }
        }
    }
}

/**
 * Which of the other modes AUTO is currently entertaining, and how solid it is.
 *
 * Four hints rather than seven: the cycle has to be short enough that a person
 * sees it come round before they swipe on, and one prop from each family — a
 * cane, a school pack, a hard hat, a collar — already says "it works this out
 * for itself" as clearly as all seven would.
 */
private data class AutoHint(val slot: Int, val fade: Float)

private fun autoHint(t: Float): AutoHint {
    val slots = 4
    val index = (t * slots).toInt().coerceIn(0, slots - 1)
    val local = t * slots - index
    // A sine rather than a triangle: a hint that arrives and leaves gently
    // reads as being considered, where a linear ramp reads as a slideshow.
    return AutoHint(index, sin(local * PI).toFloat())
}

// ============================================================================
// Props
// ============================================================================
//
// Everything below is in fractions of the square's side, matching the layout
// Shady itself is built from. The numbers that matter most are the ones that
// keep a prop off the face: the eyes' top edge is at 0.352 and the mouth is at
// 0.64, so anything worn on the head stops by 0.33 and anything at the neck
// starts at 0.72. A prop that drifts into that band turns a character into a
// blindfolded one, and it is not obvious from the code that it has happened.

/** A soft contact shadow, so the character is standing on something. */
private fun DrawScope.drawGround(s: Float, paint: ScenePaint) {
    drawOval(
        color = paint.prop.at(0.20f),
        topLeft = Offset(s * 0.21f, s * 0.945f),
        size = Size(s * 0.58f, s * 0.045f)
    )
}

/** ELDERLY: a crook-handled walking stick, planted and lifted with the step. */
private fun DrawScope.drawCane(s: Float, t: Float, paint: ScenePaint, alpha: Float = 1f) {
    val swing = sin(t * 2f * PI).toFloat()
    // The stick leaves the ground on the half-step it is carried through and is
    // planted for the half-step it takes weight.
    val lift = max(0f, -swing) * 0.030f
    val topX = s * (0.905f + swing * 0.012f)
    val topY = s * 0.585f
    val footX = s * (0.950f + swing * 0.030f)
    val footY = s * (0.965f - lift)

    drawLine(
        color = paint.prop.at(alpha),
        start = Offset(topX, topY),
        end = Offset(footX, footY),
        strokeWidth = s * 0.030f,
        cap = StrokeCap.Round
    )
    val crook = Path().apply {
        moveTo(topX, topY)
        quadraticTo(topX + s * 0.010f, topY - s * 0.085f, topX - s * 0.070f, topY - s * 0.055f)
    }
    drawPath(
        path = crook,
        color = paint.propShade.at(alpha),
        style = Stroke(width = s * 0.030f, cap = StrokeCap.Round)
    )
    drawCircle(
        color = paint.strap.at(alpha),
        radius = s * 0.021f,
        center = Offset(footX, footY)
    )
}

/** KIDS: a school pack, worn high and slightly too big. */
private fun DrawScope.drawSchoolPack(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    val x = s * 0.005f
    val y = s * 0.300f
    val w = s * 0.265f
    val h = s * 0.430f
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(x, y + s * 0.020f),
        size = Size(w, h),
        cornerRadius = corner(s, 0.075f)
    )
    drawRoundRect(
        color = paint.prop.at(alpha),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = corner(s, 0.075f)
    )
    // The flap, and the grab loop every school bag is carried by exactly once
    // before it gets worn properly.
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(x, y),
        size = Size(w, h * 0.36f),
        cornerRadius = corner(s, 0.060f)
    )
    drawPath(
        path = Path().apply {
            moveTo(x + w * 0.30f, y + s * 0.005f)
            quadraticTo(x + w * 0.50f, y - s * 0.075f, x + w * 0.70f, y + s * 0.005f)
        },
        color = paint.strap.at(alpha),
        style = Stroke(width = s * 0.020f, cap = StrokeCap.Round)
    )
    drawRoundRect(
        color = paint.light.at(alpha * 0.9f),
        topLeft = Offset(x + w * 0.28f, y + h * 0.58f),
        size = Size(w * 0.44f, h * 0.26f),
        cornerRadius = corner(s, 0.020f)
    )
}

/** KIDS: the near shoulder strap, kept well clear of the eye. */
private fun DrawScope.drawPackStrap(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    drawPath(
        path = Path().apply {
            moveTo(s * 0.175f, s * 0.355f)
            quadraticTo(s * 0.200f, s * 0.550f, s * 0.255f, s * 0.740f)
        },
        color = paint.strap.at(alpha),
        style = Stroke(width = s * 0.026f, cap = StrokeCap.Round)
    )
}

/** BIKE: a vented road helmet with a stubby peak. */
private fun DrawScope.drawCycleHelmet(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    val brim = s * 0.325f
    val dome = Path().apply {
        moveTo(s * 0.130f, brim)
        cubicTo(s * 0.130f, s * 0.120f, s * 0.870f, s * 0.120f, s * 0.870f, brim)
        close()
    }
    drawPath(dome, paint.prop.at(alpha))
    // The peak points the way the character is leaning, which is the cheapest
    // possible cue that this scene has a direction of travel.
    drawPath(
        path = Path().apply {
            moveTo(s * 0.780f, brim - s * 0.038f)
            lineTo(s * 0.985f, brim - s * 0.006f)
            lineTo(s * 0.780f, brim)
            close()
        },
        color = paint.propShade.at(alpha)
    )
    listOf(0.300f, 0.470f, 0.640f).forEach { cx ->
        drawRoundRect(
            color = paint.light.at(alpha * 0.85f),
            topLeft = Offset(s * cx, s * 0.185f),
            size = Size(s * 0.070f, s * 0.075f),
            cornerRadius = corner(s, 0.035f)
        )
    }
    drawLine(
        color = paint.propShade.at(alpha),
        start = Offset(s * 0.135f, brim),
        end = Offset(s * 0.865f, brim),
        strokeWidth = s * 0.022f,
        cap = StrokeCap.Round
    )
}

/**
 * BIKE: three dashes of moving air.
 *
 * Drawn in the world layer rather than behind the character, so the body passes
 * in front of them. Air the rider moves through, not air stuck to the rider.
 */
private fun DrawScope.drawSpeedLines(s: Float, t: Float, paint: ScenePaint) {
    listOf(0.330f, 0.500f, 0.670f).forEachIndexed { index, y ->
        val p = (t * 2f + index * 0.31f) % 1f
        val x = s * (-0.10f + p * 0.42f)
        drawLine(
            color = paint.prop.at(sin(p * PI).toFloat() * 0.75f),
            start = Offset(x, s * y),
            end = Offset(x + s * 0.170f, s * y),
            strokeWidth = s * 0.022f,
            cap = StrokeCap.Round
        )
    }
}

/** PET: a tail, wagging faster than the body it is attached to. */
private fun DrawScope.drawTail(s: Float, t: Float, paint: ScenePaint, alpha: Float = 1f) {
    val wag = sin(t * 2f * PI).toFloat()
    val tipX = s * (0.060f + wag * 0.050f)
    val tipY = s * (0.470f - abs(wag) * 0.035f)
    drawPath(
        path = Path().apply {
            moveTo(s * 0.190f, s * 0.760f)
            quadraticTo(s * 0.010f, s * 0.700f, tipX, tipY)
        },
        color = paint.prop.at(alpha),
        style = Stroke(width = s * 0.048f, cap = StrokeCap.Round)
    )
    drawCircle(paint.propShade.at(alpha), radius = s * 0.036f, center = Offset(tipX, tipY))
}

/** PET: a collar and a name tag, sitting below the mouth. */
private fun DrawScope.drawCollar(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    drawRoundRect(
        color = paint.prop.at(alpha),
        topLeft = Offset(s * 0.115f, s * 0.735f),
        size = Size(s * 0.770f, s * 0.062f),
        cornerRadius = corner(s, 0.020f)
    )
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(s * 0.115f, s * 0.735f),
        size = Size(s * 0.770f, s * 0.018f),
        cornerRadius = corner(s, 0.010f)
    )
    drawCircle(paint.strap.at(alpha), radius = s * 0.017f, center = Offset(s * 0.500f, s * 0.808f))
    drawRoundRect(
        color = paint.light.at(alpha),
        topLeft = Offset(s * 0.470f, s * 0.818f),
        size = Size(s * 0.060f, s * 0.052f),
        cornerRadius = corner(s, 0.015f)
    )
    drawCircle(paint.prop.at(alpha), radius = s * 0.011f, center = Offset(s * 0.500f, s * 0.844f))
}

/** HELMET: a brimmed hard hat with a centre ridge. */
private fun DrawScope.drawHardHat(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    val dome = Path().apply {
        moveTo(s * 0.235f, s * 0.290f)
        cubicTo(s * 0.235f, s * 0.100f, s * 0.765f, s * 0.100f, s * 0.765f, s * 0.290f)
        close()
    }
    drawPath(dome, paint.prop.at(alpha))
    drawRoundRect(
        color = paint.light.at(alpha * 0.55f),
        topLeft = Offset(s * 0.472f, s * 0.135f),
        size = Size(s * 0.056f, s * 0.155f),
        cornerRadius = corner(s, 0.028f)
    )
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(s * 0.095f, s * 0.272f),
        size = Size(s * 0.810f, s * 0.053f),
        cornerRadius = corner(s, 0.026f)
    )
}

/** WRIST: a band on the right wrist, and the activity tally pulsing out of it. */
private fun DrawScope.drawWristband(s: Float, t: Float, paint: ScenePaint, alpha: Float = 1f) {
    val cx = s * 0.900f
    val cy = s * 0.575f
    // One pulse per half-revolution. It is a tally, not a heartbeat, so it
    // expands and fades once rather than beating twice.
    val p = (t * 2f) % 1f
    drawCircle(
        color = paint.prop.at(alpha * (1f - p) * 0.55f),
        radius = s * (0.055f + p * 0.100f),
        center = Offset(cx, cy),
        style = Stroke(width = s * 0.014f)
    )
    drawRoundRect(
        color = paint.prop.at(alpha),
        topLeft = Offset(cx - s * 0.068f, cy - s * 0.033f),
        size = Size(s * 0.136f, s * 0.066f),
        cornerRadius = corner(s, 0.022f)
    )
    drawRoundRect(
        color = paint.light.at(alpha),
        topLeft = Offset(cx - s * 0.040f, cy - s * 0.026f),
        size = Size(s * 0.080f, s * 0.052f),
        cornerRadius = corner(s, 0.014f)
    )
    drawLine(
        color = paint.strap.at(alpha),
        start = Offset(cx - s * 0.022f, cy),
        end = Offset(cx + s * 0.022f, cy),
        strokeWidth = s * 0.014f,
        cap = StrokeCap.Round
    )
}

/** BACKPACK: a commuter bag — lower than the school pack, flapped, single strap. */
private fun DrawScope.drawCommuterBag(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    val x = 0f
    val y = s * 0.455f
    val w = s * 0.245f
    val h = s * 0.305f
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(x, y + s * 0.018f),
        size = Size(w, h),
        cornerRadius = corner(s, 0.045f)
    )
    drawRoundRect(
        color = paint.prop.at(alpha),
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = corner(s, 0.045f)
    )
    drawRoundRect(
        color = paint.propShade.at(alpha),
        topLeft = Offset(x, y),
        size = Size(w, h * 0.44f),
        cornerRadius = corner(s, 0.040f)
    )
    drawRoundRect(
        color = paint.strap.at(alpha),
        topLeft = Offset(x + w * 0.36f, y + h * 0.34f),
        size = Size(w * 0.28f, h * 0.16f),
        cornerRadius = corner(s, 0.010f)
    )
}

/** BACKPACK: the single strap across the chest that makes it a shoulder bag. */
private fun DrawScope.drawBagStrap(s: Float, paint: ScenePaint, alpha: Float = 1f) {
    drawPath(
        path = Path().apply {
            moveTo(s * 0.155f, s * 0.355f)
            quadraticTo(s * 0.220f, s * 0.560f, s * 0.340f, s * 0.775f)
        },
        color = paint.strap.at(alpha),
        style = Stroke(width = s * 0.028f, cap = StrokeCap.Round)
    )
}

private fun corner(s: Float, fraction: Float) = CornerRadius(s * fraction, s * fraction)

// ============================================================================
// Previews
// ============================================================================
//
// Every scene was designed in these. They hold at HELD_FRAME through
// LocalInspectionMode, so a change that breaks a prop's alignment shows up here
// as a cane through a fist rather than only on a device.

@Composable
private fun SceneSheet(darkTheme: Boolean) {
    SafeShadeTheme(darkTheme = darkTheme) {
        val colors = MaterialTheme.board
        val palette = colors.accents
        Column(
            modifier = Modifier
                .background(colors.ground)
                .padding(Spacing.md)
        ) {
            PersonaMode.entries.chunked(4).forEach { row ->
                Row {
                    row.forEach { mode ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(Spacing.xs)
                        ) {
                            ModeScene(
                                mode = mode,
                                accent = palette[PersonaMode.entries.indexOf(mode) % palette.size],
                                size = 92.dp
                            )
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "Scenes · light", showBackground = true, widthDp = 440)
@Composable
private fun ModeScenesPreviewLight() = SceneSheet(darkTheme = false)

@Preview(name = "Scenes · dark", showBackground = true, widthDp = 440)
@Composable
private fun ModeScenesPreviewDark() = SceneSheet(darkTheme = true)
