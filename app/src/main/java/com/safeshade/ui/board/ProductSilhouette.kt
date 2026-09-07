package com.safeshade.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.data.DeviceModel
import com.safeshade.ui.theme.board

/**
 * The three products, drawn.
 *
 * A product picture on a pairing page has one job: let a person who has the
 * thing in their hand point at the right one. So each silhouette is the
 * outline a hand knows, not a render: the S1 is the flat puck with its small
 * screen and the knob on the right edge; the 5G is the same puck with the
 * antenna nub and the SIM tray notch; the Spark is the small round tag on a
 * lanyard ring. Three flat tones in the manner of the character (rim, skin,
 * shade), no gradient, ink outlines, and the screen drawn as a recess so it
 * reads as glass in both themes. The accent is identity only.
 */
@Composable
fun ProductSilhouette(
    model: DeviceModel,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    accent: Color = Color.Unspecified
) {
    val colors = MaterialTheme.board
    val body = if (accent == Color.Unspecified) colors.brass else accent
    val shade = lerp(body, colors.ink, if (colors.isDark) 0.36f else 0.26f)
    val rim = lerp(body, colors.plate, 0.35f)
    Canvas(
        modifier = modifier
            .semantics { contentDescription = model.label }
            .size(size)
    ) {
        when (model) {
            DeviceModel.S1 -> drawPuck(rim, body, shade, colors.ink, colors.recess, colors.inkFaint, antenna = false)
            DeviceModel.FIVE_G -> drawPuck(rim, body, shade, colors.ink, colors.recess, colors.inkFaint, antenna = true)
            DeviceModel.SPARK -> drawTag(rim, body, shade, colors.ink, colors.recess)
        }
    }
}

private fun DrawScope.drawPuck(
    rim: Color, body: Color, shade: Color, ink: Color, glass: Color, glassLine: Color, antenna: Boolean
) {
    val s = size.minDimension
    val w = s * 0.78f
    val h = s * 0.52f
    val left = (s - w) / 2f
    val top = (s - h) / 2f + if (antenna) s * 0.04f else 0f
    val r = CornerRadius(s * 0.09f)
    val outline = Stroke(width = s * 0.022f)

    if (antenna) {
        // The nub sits on the top edge, off centre, the way the aerial does.
        val nub = RoundRect(Rect(left + w * 0.66f, top - s * 0.11f, left + w * 0.78f, top + s * 0.02f), CornerRadius(s * 0.03f))
        drawPath(Path().apply { addRoundRect(nub) }, shade)
        drawPath(Path().apply { addRoundRect(nub) }, ink, style = outline)
    }

    val bodyRect = RoundRect(Rect(left, top, left + w, top + h), r)
    drawPath(Path().apply { addRoundRect(bodyRect) }, body)
    // Shade band along the lower-right edge, rim along the upper-left; the
    // same three-tone carve the character uses, done with clipped strips.
    clipPath(Path().apply { addRoundRect(bodyRect) }) {
        drawRect(shade, topLeft = Offset(left, top + h * 0.72f), size = Size(w, h * 0.28f))
        drawRect(rim, topLeft = Offset(left, top), size = Size(w, h * 0.10f))
    }
    drawPath(Path().apply { addRoundRect(bodyRect) }, ink, style = outline)

    // The screen: a recess on the left two-thirds.
    val screen = RoundRect(Rect(left + w * 0.10f, top + h * 0.20f, left + w * 0.62f, top + h * 0.80f), CornerRadius(s * 0.02f))
    drawPath(Path().apply { addRoundRect(screen) }, glass)
    drawPath(Path().apply { addRoundRect(screen) }, ink, style = Stroke(width = s * 0.014f))
    // Two lines of text on it, as the device draws them.
    drawLine(glassLine, Offset(left + w * 0.16f, top + h * 0.40f), Offset(left + w * 0.50f, top + h * 0.40f), strokeWidth = s * 0.02f)
    drawLine(glassLine, Offset(left + w * 0.16f, top + h * 0.58f), Offset(left + w * 0.40f, top + h * 0.58f), strokeWidth = s * 0.02f)

    // The knob on the right edge.
    val knobCx = left + w * 0.80f
    val knobCy = top + h * 0.50f
    drawCircle(shade, radius = s * 0.085f, center = Offset(knobCx, knobCy))
    drawCircle(ink, radius = s * 0.085f, center = Offset(knobCx, knobCy), style = Stroke(width = s * 0.018f))
    drawLine(ink, Offset(knobCx, knobCy - s * 0.05f), Offset(knobCx, knobCy + s * 0.05f), strokeWidth = s * 0.014f)

    if (antenna) {
        // The SIM tray notch on the bottom edge.
        drawRect(ink, topLeft = Offset(left + w * 0.12f, top + h - s * 0.012f), size = Size(w * 0.18f, s * 0.012f))
    }

    // The lights: three pips under the screen.
    for (i in 0..2) {
        drawCircle(rim, radius = s * 0.018f, center = Offset(left + w * (0.18f + i * 0.12f), top + h * 0.90f))
    }
}

private fun DrawScope.drawTag(rim: Color, body: Color, shade: Color, ink: Color, glass: Color) {
    val s = size.minDimension
    val cx = s * 0.5f
    val cy = s * 0.56f
    val radius = s * 0.30f
    val outline = Stroke(width = s * 0.022f)

    // The lanyard ring, behind.
    val ringC = Offset(cx, cy - radius - s * 0.06f)
    drawCircle(shade, radius = s * 0.075f, center = ringC, style = Stroke(width = s * 0.04f))
    drawCircle(ink, radius = s * 0.075f, center = ringC, style = Stroke(width = s * 0.014f))

    drawCircle(body, radius = radius, center = Offset(cx, cy))
    clipPath(Path().apply { addOval(Rect(Offset(cx - radius, cy - radius), Size(radius * 2, radius * 2))) }) {
        drawRect(shade, topLeft = Offset(cx - radius, cy + radius * 0.45f), size = Size(radius * 2, radius))
        drawRect(rim, topLeft = Offset(cx - radius, cy - radius), size = Size(radius * 2, radius * 0.22f))
    }
    drawCircle(ink, radius = radius, center = Offset(cx, cy), style = outline)

    // One button face in the middle, recessed, with the spark mark on it.
    drawCircle(glass, radius = radius * 0.52f, center = Offset(cx, cy))
    drawCircle(ink, radius = radius * 0.52f, center = Offset(cx, cy), style = Stroke(width = s * 0.014f))
    val bolt = Path().apply {
        moveTo(cx + radius * 0.06f, cy - radius * 0.30f)
        lineTo(cx - radius * 0.14f, cy + radius * 0.04f)
        lineTo(cx + radius * 0.02f, cy + radius * 0.04f)
        lineTo(cx - radius * 0.06f, cy + radius * 0.30f)
        lineTo(cx + radius * 0.14f, cy - radius * 0.04f)
        lineTo(cx - radius * 0.02f, cy - radius * 0.04f)
        close()
    }
    drawPath(bolt, ink)
}
