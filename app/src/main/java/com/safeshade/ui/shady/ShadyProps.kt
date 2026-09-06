package com.safeshade.ui.shady

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

/**
 * The things Shady shares the ground with.
 *
 * A prop is something on the stage that is not Shady: a plant to water, a cat
 * to be rubbed against by, a phone that buzzes, a kettle that whistles, a ball
 * to nudge off the edge, a box to climb into, an umbrella for when the app
 * knows it is raining. One is on stage at a time, for a scene of a few beats,
 * and then it goes — the stage is a strip of ground, not a room, and a room
 * full of furniture would stop reading as a character having a moment.
 *
 * Props are drawn in the same voice as the character: flat tones, the light
 * from the upper left, a contact shadow on the ground, no gradients and no
 * outlines. They never report state. The umbrella *reads* the weather state
 * to decide whether to exist, which is the one concession — a prop that
 * appears when it is raining is a detail, a prop that tells you it is raining
 * would be an indicator, and indicators are the pilot lamp's job.
 */
enum class PropKind { PLANT, CAT, PHONE, KETTLE, BALL, BOX, UMBRELLA }

/**
 * A prop's whole state, as an immutable snapshot the director replaces.
 *
 * [x] is along the ground in the same 0..1 space as the character's `walkT`,
 * so "walk to the prop" is one subtraction. [presence] fades a prop in and
 * out. [a] and [b] are per-kind animation scalars — the cat's stride, the
 * kettle's steam, how far the ball has rolled, how much the plant has grown —
 * named vaguely on purpose so a beat can drive them without a field per kind.
 */
data class PropState(
    val kind: PropKind,
    val x: Float,
    val presence: Float = 0f,
    val a: Float = 0f,
    val b: Float = 0f,
    val facingLeft: Boolean = false,
    /** True while the character is inside the box, so its front face draws over him. */
    val occludes: Boolean = false,
    /** True while the character is holding it (the umbrella). */
    val held: Boolean = false
) {
    /** Half the ground the prop occupies, in walkT units, so the character walks up to it and not through it. */
    val halfWidth: Float
        get() = when (kind) {
            PropKind.PLANT -> 0.09f
            PropKind.CAT -> 0.13f
            PropKind.PHONE -> 0.07f
            PropKind.KETTLE -> 0.11f
            PropKind.BALL -> 0.05f
            PropKind.BOX -> 0.12f
            PropKind.UMBRELLA -> 0f
        }
}

/** Which pass of the stage is drawing: what sits behind the character, or in front of it. */
enum class PropLayer { BEHIND, FRONT }

// A prop palette in the character's register: flat, lit from the upper left,
// desaturated enough that none of it could be mistaken for a lamp.
private object P {
    val Pot = Color(0xFFB9705A)
    val PotCore = Color(0xFF96594A)
    val PotLight = Color(0xFFCF8A73)
    val Leaf = Color(0xFF7C9A6B)
    val LeafCore = Color(0xFF5F7D50)
    val LeafLight = Color(0xFF9AB68A)
    val Cat = Color(0xFF8A8F96)
    val CatCore = Color(0xFF6A6F76)
    val CatLight = Color(0xFFA9AEB4)
    val CatInner = Color(0xFFE8B4B8)
    val Phone = Color(0xFF22282E)
    val PhoneLight = Color(0xFF3A4249)
    val Screen = Color(0xFF6FD3CC)
    val ScreenOff = Color(0xFF14171A)
    val Pad = Color(0xFF5E636A)
    val Steel = Color(0xFFB8BCC2)
    val SteelCore = Color(0xFF8E939A)
    val SteelLight = Color(0xFFD9DCE0)
    val Ball = Color(0xFF6F8FB0)
    val BallCore = Color(0xFF546F8C)
    val BallLight = Color(0xFF93AEC9)
    val Card = Color(0xFFC9A57A)
    val CardCore = Color(0xFFA8875E)
    val CardLight = Color(0xFFDBBB94)
    val Canopy = Color(0xFF6F8FB0)
    val CanopyCore = Color(0xFF546F8C)
    val CanopyLight = Color(0xFF93AEC9)
    val Amber = Color(0xFFF5A623)
}

/**
 * Draws one prop.
 *
 * @param cx the prop's centre along the ground, in pixels.
 * @param ground the y of the ground line, in pixels.
 * @param u the character's size in pixels — every prop is sized against it,
 *   so a prop looks the same next to a 100dp Shady as next to a 120dp one.
 * @param phase a slow 0..1 loop for idle motion the director is not driving.
 */
fun DrawScope.drawProp(
    prop: PropState,
    layer: PropLayer,
    cx: Float,
    ground: Float,
    u: Float,
    phase: Float,
    ink: Color,
    isDark: Boolean,
    /** Where the character's hand is, in pixels, for a held prop. */
    hand: Offset = Offset.Zero,
    characterFacingLeft: Boolean = false
) {
    if (prop.presence <= 0.01f) return
    when (prop.kind) {
        PropKind.PLANT -> if (layer == PropLayer.BEHIND) drawPlant(prop, cx, ground, u, phase, isDark)
        PropKind.CAT -> if (layer == PropLayer.BEHIND) drawCat(prop, cx, ground, u, phase, ink, isDark)
        PropKind.PHONE -> if (layer == PropLayer.BEHIND) drawPhone(prop, cx, ground, u, phase, isDark)
        PropKind.KETTLE -> if (layer == PropLayer.BEHIND) drawKettle(prop, cx, ground, u, phase, ink, isDark)
        PropKind.BALL -> if (layer == PropLayer.BEHIND) drawBall(prop, cx, ground, u, isDark)
        PropKind.BOX -> drawBox(prop, layer, cx, ground, u, isDark)
        PropKind.UMBRELLA -> if (layer == PropLayer.FRONT) drawUmbrella(prop, cx, ground, u, phase, ink, hand, characterFacingLeft)
    }
}

private fun DrawScope.contact(cx: Float, ground: Float, w: Float, h: Float, alpha: Float, isDark: Boolean) {
    drawOval(
        color = (if (isDark) Color.Black else Color(0xFF22282E)).copy(alpha = alpha),
        topLeft = Offset(cx - w / 2f, ground - h / 2f),
        size = Size(w, h)
    )
}

private fun DrawScope.shadedRoundRect(x: Float, y: Float, w: Float, h: Float, r: Float, base: Color, core: Color, light: Color, alpha: Float) {
    val cr = CornerRadius(r, r)
    drawRoundRect(base.copy(alpha = alpha), Offset(x, y), Size(w, h), cr)
    // Core band: right and bottom edges.
    drawRoundRect(core.copy(alpha = alpha), Offset(x + w - r * 0.9f, y + r * 0.6f), Size(r * 0.9f, h - r * 0.6f), CornerRadius(r * 0.5f, r * 0.5f))
    drawRoundRect(core.copy(alpha = alpha), Offset(x + r * 0.6f, y + h - r * 0.9f), Size(w - r * 0.6f, r * 0.9f), CornerRadius(r * 0.5f, r * 0.5f))
    // Rim: a short highlight along the top-left.
    drawRoundRect(light.copy(alpha = alpha), Offset(x + r * 0.5f, y + r * 0.35f), Size(w * 0.35f, r * 0.55f), CornerRadius(r * 0.3f, r * 0.3f))
}

// ============================================================================
// Nature — a potted plant. `a` is growth: 0 a bud, 1 a new leaf unfurled.
// `b` is a drop of water arriving (0..1) from the watering beat.
// ============================================================================

private fun DrawScope.drawPlant(p: PropState, cx: Float, ground: Float, u: Float, phase: Float, isDark: Boolean) {
    val al = p.presence
    val potW = u * 0.34f
    val potH = u * 0.25f
    contact(cx, ground, potW * 1.1f, u * 0.045f, 0.14f * al, isDark)

    // Pot: a tapered trapezoid with a lip.
    val pot = Path().apply {
        moveTo(cx - potW / 2f, ground - potH)
        lineTo(cx + potW / 2f, ground - potH)
        lineTo(cx + potW * 0.40f, ground)
        lineTo(cx - potW * 0.40f, ground)
        close()
    }
    drawPath(pot, P.Pot.copy(alpha = al))
    val core = Path().apply {
        moveTo(cx + potW * 0.22f, ground - potH)
        lineTo(cx + potW / 2f, ground - potH)
        lineTo(cx + potW * 0.40f, ground)
        lineTo(cx + potW * 0.16f, ground)
        close()
    }
    drawPath(core, P.PotCore.copy(alpha = al))
    drawRoundRect(P.PotLight.copy(alpha = al), Offset(cx - potW * 0.55f, ground - potH - u * 0.028f), Size(potW * 1.10f, u * 0.036f), CornerRadius(u * 0.008f, u * 0.008f))
    drawRect(P.PotCore.copy(alpha = al), Offset(cx + potW * 0.30f, ground - potH - u * 0.028f), Size(potW * 0.25f, u * 0.036f))

    // Stem and leaves. The whole plant sways very slightly.
    val sway = sin(phase * 2f * PI).toFloat() * 2f
    val top = ground - potH - u * 0.02f
    rotate(degrees = sway, pivot = Offset(cx, top)) {
        drawLine(P.LeafCore.copy(alpha = al), Offset(cx, top), Offset(cx, top - u * 0.30f), u * 0.024f, StrokeCap.Round)
        leaf(cx, top - u * 0.10f, u * 0.13f, -35f, al)
        leaf(cx, top - u * 0.18f, u * 0.13f, 35f, al, mirror = true)
        leaf(cx, top - u * 0.05f, u * 0.10f, 40f, al, mirror = true)
        // The new leaf, unfurling with `a`.
        if (p.a > 0.02f) {
            val size = u * 0.14f * p.a
            leaf(cx, top - u * 0.29f, size, -12f + 40f * (1f - p.a), al)
        }
    }
    // A drop of water falling in from above during the watering beat.
    if (p.b > 0.01f && p.b < 0.99f) {
        val dy = top - u * 0.44f + p.b * u * 0.22f
        drawCircle(P.Screen.copy(alpha = al * (1f - p.b * 0.5f)), u * 0.016f, Offset(cx - u * 0.02f, dy))
    }
}

private fun DrawScope.leaf(x: Float, y: Float, len: Float, angle: Float, alpha: Float, mirror: Boolean = false) {
    val dir = if (mirror) 1f else -1f
    rotate(degrees = angle, pivot = Offset(x, y)) {
        val path = Path().apply {
            moveTo(x, y)
            quadraticTo(x + dir * len * 0.6f, y - len * 0.55f, x + dir * len, y - len * 0.15f)
            quadraticTo(x + dir * len * 0.55f, y + len * 0.10f, x, y)
            close()
        }
        drawPath(path, P.Leaf.copy(alpha = alpha))
        val vein = Path().apply {
            moveTo(x, y)
            quadraticTo(x + dir * len * 0.5f, y - len * 0.22f, x + dir * len * 0.92f, y - len * 0.14f)
        }
        drawPath(vein, P.LeafCore.copy(alpha = alpha), style = Stroke(width = len * 0.05f, cap = StrokeCap.Round))
        drawPath(
            Path().apply {
                moveTo(x + dir * len * 0.12f, y - len * 0.10f)
                quadraticTo(x + dir * len * 0.40f, y - len * 0.42f, x + dir * len * 0.62f, y - len * 0.30f)
            },
            P.LeafLight.copy(alpha = alpha), style = Stroke(width = len * 0.07f, cap = StrokeCap.Round)
        )
    }
}

// ============================================================================
// Pet — a cat. `a` is the stride phase while walking (0 when sitting), `b` is
// how pleased it is (0..1): ears relax, eyes close, tail curls up.
// ============================================================================

private fun DrawScope.drawCat(p: PropState, cx: Float, ground: Float, u: Float, phase: Float, ink: Color, isDark: Boolean) {
    val al = p.presence
    val dir = if (p.facingLeft) -1f else 1f
    val bodyW = u * 0.54f
    val bodyH = u * 0.27f
    val walking = p.a > 0f
    val stride = if (walking) sin(p.a * 2f * PI).toFloat() else 0f
    val bob = if (walking) abs(stride) * u * 0.012f else 0f
    contact(cx, ground, bodyW * 1.15f, u * 0.045f, 0.14f * al, isDark)

    val bodyY = ground - bodyH - u * 0.05f - bob
    // Tail: curls up when content, sweeps when walking, flicks idly otherwise.
    val flick = sin(phase * 4f * PI).toFloat()
    val tail = Path().apply {
        val root = Offset(cx - dir * bodyW * 0.48f, bodyY + bodyH * 0.45f)
        moveTo(root.x, root.y)
        val curl = p.b
        cubicTo(
            root.x - dir * u * 0.10f, root.y + u * 0.02f - curl * u * 0.10f,
            root.x - dir * u * 0.16f, root.y - u * 0.10f - curl * u * 0.12f + flick * u * 0.02f,
            root.x - dir * u * (0.08f + 0.06f * curl), root.y - u * (0.20f + 0.10f * curl) + flick * u * 0.02f
        )
    }
    drawPath(tail, P.Cat.copy(alpha = al), style = Stroke(width = u * 0.040f, cap = StrokeCap.Round))

    // Legs, two visible pairs offset with the stride.
    listOf(-0.30f, -0.12f, 0.10f, 0.28f).forEachIndexed { i, fx ->
        val swing = if (walking) stride * (if (i % 2 == 0) 1f else -1f) * u * 0.03f else 0f
        drawRoundRect(P.CatCore.copy(alpha = al), Offset(cx + dir * bodyW * fx - u * 0.025f + swing, bodyY + bodyH * 0.75f), Size(u * 0.05f, ground - bodyY - bodyH * 0.75f), CornerRadius(u * 0.02f, u * 0.02f))
    }

    // Body.
    shadedRoundRect(cx - bodyW / 2f, bodyY, bodyW, bodyH, bodyH * 0.45f, P.Cat, P.CatCore, P.CatLight, al)

    // Head, forward of the body.
    val headR = u * 0.145f
    val hx = cx + dir * bodyW * 0.50f
    val hy = bodyY + bodyH * 0.10f
    drawCircle(P.Cat.copy(alpha = al), headR, Offset(hx, hy))
    drawCircle(P.CatCore.copy(alpha = al), headR * 0.92f, Offset(hx + headR * 0.10f, hy + headR * 0.10f), style = Stroke(width = headR * 0.16f))
    drawCircle(P.Cat.copy(alpha = al), headR * 0.80f, Offset(hx, hy))
    drawCircle(P.CatLight.copy(alpha = al), headR * 0.28f, Offset(hx - headR * 0.35f, hy - headR * 0.40f))
    // Ears: upright, relaxing outward as it is pleased.
    listOf(-1f, 1f).forEach { side ->
        val relax = p.b * 12f
        rotate(degrees = side * relax * dir, pivot = Offset(hx + side * headR * 0.55f, hy - headR * 0.55f)) {
            val ear = Path().apply {
                moveTo(hx + side * headR * 0.20f, hy - headR * 0.70f)
                lineTo(hx + side * headR * 0.62f, hy - headR * 1.30f)
                lineTo(hx + side * headR * 0.88f, hy - headR * 0.45f)
                close()
            }
            drawPath(ear, P.Cat.copy(alpha = al))
            val inner = Path().apply {
                moveTo(hx + side * headR * 0.38f, hy - headR * 0.72f)
                lineTo(hx + side * headR * 0.62f, hy - headR * 1.10f)
                lineTo(hx + side * headR * 0.76f, hy - headR * 0.55f)
                close()
            }
            drawPath(inner, P.CatInner.copy(alpha = al * 0.9f))
        }
    }
    // Eyes: slits when content, else small pupils. Nose and whiskers.
    val eyeY = hy - headR * 0.05f
    listOf(-0.36f, 0.36f).forEach { fx ->
        val ex = hx + dir * headR * (fx + 0.25f)
        if (p.b > 0.5f) {
            drawPath(
                Path().apply {
                    moveTo(ex - headR * 0.16f, eyeY)
                    quadraticTo(ex, eyeY + headR * 0.16f, ex + headR * 0.16f, eyeY)
                },
                ink.copy(alpha = al), style = Stroke(width = headR * 0.09f, cap = StrokeCap.Round)
            )
        } else {
            drawOval(ink.copy(alpha = al), Offset(ex - headR * 0.07f, eyeY - headR * 0.13f), Size(headR * 0.14f, headR * 0.26f))
        }
    }
    drawCircle(P.CatInner.copy(alpha = al), headR * 0.08f, Offset(hx + dir * headR * 0.62f, hy + headR * 0.22f))
    listOf(-0.10f, 0.08f).forEach { dy ->
        drawLine(ink.copy(alpha = al * 0.55f), Offset(hx + dir * headR * 0.70f, hy + headR * (0.28f + dy)), Offset(hx + dir * headR * 1.25f, hy + headR * (0.20f + dy * 2f)), headR * 0.04f, StrokeCap.Round)
    }
}

// ============================================================================
// Tech — a phone lying on a charging pad. `a` is how lit the screen is,
// `b` is a buzz (a small sideways jitter while > 0).
// ============================================================================

private fun DrawScope.drawPhone(p: PropState, cx: Float, ground: Float, u: Float, phase: Float, isDark: Boolean) {
    val al = p.presence
    val padW = u * 0.34f
    val padH = u * 0.035f
    contact(cx, ground, padW * 1.15f, u * 0.045f, 0.14f * al, isDark)
    drawRoundRect(P.Pad.copy(alpha = al), Offset(cx - padW / 2f, ground - padH), Size(padW, padH), CornerRadius(u * 0.012f, u * 0.012f))

    val buzz = if (p.b > 0f) sin(phase * 60f * PI).toFloat() * u * 0.006f * p.b else 0f
    val w = u * 0.28f
    val h = u * 0.055f
    val x = cx - w / 2f + buzz
    val y = ground - padH - h
    shadedRoundRect(x, y, w, h, u * 0.012f, P.Phone, Color(0xFF14171A), P.PhoneLight, al)
    // The screen: a lit rectangle, no content. Nothing on it could be read.
    val screen = if (p.a > 0.02f) P.Screen.copy(alpha = al * (0.35f + 0.65f * p.a)) else P.ScreenOff.copy(alpha = al)
    drawRoundRect(screen, Offset(x + w * 0.05f, y + h * 0.20f), Size(w * 0.90f, h * 0.55f), CornerRadius(u * 0.006f, u * 0.006f))
    // A charging tick beside the pad while lit.
    if (p.a > 0.5f) drawCircle(P.Screen.copy(alpha = al * 0.8f), u * 0.010f, Offset(cx + padW * 0.46f, ground - padH / 2f))
}

// ============================================================================
// Appliance — a kettle. `a` is steam (0..1), `b` is the whistle (rocks the lid).
// ============================================================================

private fun DrawScope.drawKettle(p: PropState, cx: Float, ground: Float, u: Float, phase: Float, ink: Color, isDark: Boolean) {
    val al = p.presence
    val w = u * 0.44f
    val h = u * 0.36f
    contact(cx, ground, w * 1.1f, u * 0.045f, 0.14f * al, isDark)

    val bodyY = ground - h
    // Body: a rounded trapezoid.
    val body = Path().apply {
        moveTo(cx - w * 0.38f, bodyY + h * 0.18f)
        quadraticTo(cx - w * 0.38f, bodyY, cx - w * 0.20f, bodyY)
        lineTo(cx + w * 0.20f, bodyY)
        quadraticTo(cx + w * 0.38f, bodyY, cx + w * 0.38f, bodyY + h * 0.18f)
        lineTo(cx + w * 0.50f, ground - h * 0.06f)
        quadraticTo(cx + w * 0.50f, ground, cx + w * 0.42f, ground)
        lineTo(cx - w * 0.42f, ground)
        quadraticTo(cx - w * 0.50f, ground, cx - w * 0.50f, ground - h * 0.06f)
        close()
    }
    drawPath(body, P.Steel.copy(alpha = al))
    // Core band down the right, rim up the left.
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.22f, bodyY + h * 0.10f)
            lineTo(cx + w * 0.38f, bodyY + h * 0.18f)
            lineTo(cx + w * 0.50f, ground - h * 0.06f)
            lineTo(cx + w * 0.42f, ground)
            lineTo(cx + w * 0.22f, ground)
            close()
        },
        P.SteelCore.copy(alpha = al)
    )
    drawRoundRect(P.SteelLight.copy(alpha = al), Offset(cx - w * 0.30f, bodyY + h * 0.14f), Size(w * 0.07f, h * 0.55f), CornerRadius(w * 0.03f, w * 0.03f))
    // Spout, on the left; handle on the right.
    drawPath(
        Path().apply {
            moveTo(cx - w * 0.36f, bodyY + h * 0.30f)
            lineTo(cx - w * 0.62f, bodyY + h * 0.04f)
            lineTo(cx - w * 0.52f, bodyY + h * 0.02f)
            lineTo(cx - w * 0.30f, bodyY + h * 0.44f)
            close()
        },
        P.SteelCore.copy(alpha = al)
    )
    drawPath(
        Path().apply {
            moveTo(cx + w * 0.30f, bodyY + h * 0.24f)
            cubicTo(cx + w * 0.72f, bodyY + h * 0.10f, cx + w * 0.72f, bodyY + h * 0.70f, cx + w * 0.42f, bodyY + h * 0.72f)
        },
        ink.copy(alpha = al), style = Stroke(width = u * 0.024f, cap = StrokeCap.Round)
    )
    // Lid, rocking with the whistle.
    val rock = if (p.b > 0f) sin(phase * 50f * PI).toFloat() * 4f * p.b else 0f
    rotate(degrees = rock, pivot = Offset(cx, bodyY)) {
        drawRoundRect(P.SteelLight.copy(alpha = al), Offset(cx - w * 0.16f, bodyY - u * 0.020f), Size(w * 0.32f, u * 0.026f), CornerRadius(u * 0.008f, u * 0.008f))
        drawCircle(ink.copy(alpha = al), u * 0.012f, Offset(cx, bodyY - u * 0.028f))
    }
    // Steam from the spout.
    if (p.a > 0.02f) {
        repeat(3) { i ->
            val t = (phase * 1.6f + i * 0.33f) % 1f
            val sx = cx - w * 0.60f + sin(t * 3f * PI + i).toFloat() * u * 0.02f
            val sy = bodyY - u * 0.02f - t * u * 0.16f * p.a
            drawPath(
                Path().apply {
                    moveTo(sx, sy + u * 0.03f)
                    quadraticTo(sx + u * 0.016f, sy + u * 0.010f, sx, sy - u * 0.010f)
                    quadraticTo(sx - u * 0.016f, sy - u * 0.030f, sx, sy - u * 0.050f)
                },
                ink.copy(alpha = al * 0.45f * (1f - t) * p.a),
                style = Stroke(width = u * 0.014f, cap = StrokeCap.Round)
            )
        }
    }
}

// ============================================================================
// Materials — a ball. `a` is how far it has rolled (in walkT units, applied by
// the director to `x`), `b` its rotation in turns.
// ============================================================================

private fun DrawScope.drawBall(p: PropState, cx: Float, ground: Float, u: Float, isDark: Boolean) {
    val al = p.presence
    val r = u * 0.13f
    contact(cx, ground, r * 2.0f, u * 0.04f, 0.14f * al, isDark)
    val cy = ground - r - u * 0.01f
    drawCircle(P.Ball.copy(alpha = al), r, Offset(cx, cy))
    drawCircle(P.BallCore.copy(alpha = al), r * 0.92f, Offset(cx + r * 0.12f, cy + r * 0.12f), style = Stroke(width = r * 0.18f))
    drawCircle(P.Ball.copy(alpha = al), r * 0.78f, Offset(cx, cy))
    drawCircle(P.BallLight.copy(alpha = al), r * 0.26f, Offset(cx - r * 0.36f, cy - r * 0.40f))
    // One band across it so the roll is visible.
    rotate(degrees = p.b * 360f, pivot = Offset(cx, cy)) {
        drawArc(P.BallCore.copy(alpha = al * 0.8f), 20f, 140f, false, Offset(cx - r * 0.78f, cy - r * 0.78f), Size(r * 1.56f, r * 1.56f), style = Stroke(width = r * 0.14f))
    }
}

// ============================================================================
// Materials — a cardboard box. `a` is a wobble (rotation degrees), `b` is how
// open the flap is. The front face draws over the character when `occludes`.
// ============================================================================

private fun DrawScope.drawBox(p: PropState, layer: PropLayer, cx: Float, ground: Float, u: Float, isDark: Boolean) {
    val al = p.presence
    val w = u * 0.56f
    val h = u * 0.40f
    val x = cx - w / 2f
    val y = ground - h
    rotate(degrees = p.a, pivot = Offset(cx, ground)) {
        if (layer == PropLayer.BEHIND) {
            contact(cx, ground, w * 1.05f, u * 0.045f, 0.14f * al, isDark)
            // Back wall and floor of the box (visible through the open top).
            drawRect(P.CardCore.copy(alpha = al), Offset(x, y - u * 0.02f), Size(w, h * 0.30f))
            // Back flap, standing up.
            drawRect(P.Card.copy(alpha = al), Offset(x + w * 0.08f, y - u * 0.12f), Size(w * 0.84f, u * 0.13f))
        } else {
            // Front face and the front flap, folded down and forward.
            shadedRoundRect(x, y, w, h, u * 0.012f, P.Card, P.CardCore, P.CardLight, al)
            drawLine(P.CardCore.copy(alpha = al), Offset(cx, y + u * 0.02f), Offset(cx, ground - u * 0.02f), u * 0.010f)
            val flapH = u * 0.10f * (0.4f + 0.6f * p.b)
            drawRect(P.CardLight.copy(alpha = al), Offset(x + w * 0.04f, y), Size(w * 0.92f, flapH))
            drawRect(P.CardCore.copy(alpha = al * 0.6f), Offset(x + w * 0.04f, y + flapH - u * 0.012f), Size(w * 0.92f, u * 0.012f))
        }
    }
}

// ============================================================================
// Weather — an umbrella, held over the character. Drawn in front, anchored
// to the hand. `a` is rain (0..1) for a few drops around the canopy.
// ============================================================================

private fun DrawScope.drawUmbrella(p: PropState, cx: Float, ground: Float, u: Float, phase: Float, ink: Color, hand: Offset, facingLeft: Boolean) {
    val al = p.presence
    val dir = if (facingLeft) -1f else 1f
    val hx = hand.x
    val hy = hand.y
    val top = hy - u * 0.62f
    val canopyW = u * 0.92f
    val tilt = dir * -6f + sin(phase * 2f * PI).toFloat() * 1.5f
    rotate(degrees = tilt, pivot = Offset(hx, hy)) {
        // Shaft and hook.
        drawLine(ink.copy(alpha = al), Offset(hx, hy), Offset(hx, top + u * 0.02f), u * 0.018f, StrokeCap.Round)
        drawPath(
            Path().apply {
                moveTo(hx, hy)
                quadraticTo(hx, hy + u * 0.06f, hx + dir * u * 0.04f, hy + u * 0.05f)
            },
            ink.copy(alpha = al), style = Stroke(width = u * 0.018f, cap = StrokeCap.Round)
        )
        // Canopy: a dome with four scallops along the hem.
        val canopy = Path().apply {
            moveTo(hx - canopyW / 2f, top + u * 0.16f)
            quadraticTo(hx, top - u * 0.14f, hx + canopyW / 2f, top + u * 0.16f)
            var sx = hx + canopyW / 2f
            repeat(4) {
                val nx = sx - canopyW / 4f
                quadraticTo((sx + nx) / 2f, top + u * 0.21f, nx, top + u * 0.16f)
                sx = nx
            }
            close()
        }
        drawPath(canopy, P.Canopy.copy(alpha = al))
        // Core on the right half, rim on the left.
        drawPath(
            Path().apply {
                moveTo(hx + canopyW * 0.12f, top - u * 0.055f)
                quadraticTo(hx + canopyW * 0.42f, top - u * 0.02f, hx + canopyW / 2f, top + u * 0.16f)
                lineTo(hx + canopyW * 0.25f, top + u * 0.16f)
                close()
            },
            P.CanopyCore.copy(alpha = al)
        )
        drawPath(
            Path().apply {
                moveTo(hx - canopyW * 0.40f, top + u * 0.10f)
                quadraticTo(hx - canopyW * 0.22f, top - u * 0.05f, hx - canopyW * 0.05f, top - u * 0.08f)
            },
            P.CanopyLight.copy(alpha = al), style = Stroke(width = u * 0.024f, cap = StrokeCap.Round)
        )
        // Ribs and the tip.
        listOf(-0.25f, 0f, 0.25f).forEach { fx ->
            drawLine(P.CanopyCore.copy(alpha = al * 0.7f), Offset(hx, top - u * 0.02f), Offset(hx + canopyW * fx, top + u * 0.16f), u * 0.008f)
        }
        drawCircle(ink.copy(alpha = al), u * 0.014f, Offset(hx, top - u * 0.15f))
    }
    // A few drops sliding off the hem, only while it rains.
    if (p.a > 0.02f) {
        repeat(4) { i ->
            val t = (phase * 2f + i * 0.25f) % 1f
            val dx = hx + canopyW * (-0.42f + 0.28f * i)
            val dy = top + u * 0.18f + t * u * 0.30f
            drawLine(P.Screen.copy(alpha = al * p.a * (1f - t)), Offset(dx, dy), Offset(dx, dy + u * 0.03f), u * 0.010f, StrokeCap.Round)
        }
    }
}
