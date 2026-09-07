package com.safeshade.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.board

/**
 * A voice note's picture: one bar per sampled amplitude, played bars in
 * brass, the rest in faint ink.
 *
 * Flat bars with rounded ends, nothing else: no gradient, no glow, no
 * mirrored reflection. The bars are the note's own shape, captured while it
 * was recorded, so two notes never look alike and a note can be told apart
 * before it is played. [progress] is the played fraction, 0..1, and the
 * boundary is drawn on the bar, not as a separate cursor.
 *
 * Purely decorative to a screen reader: the row that holds it says the
 * duration and the state in words.
 */
@Composable
fun Waveform(
    bars: List<Float>,
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp
) {
    val colors = MaterialTheme.board
    val played = colors.brass
    val rest = colors.inkFaint
    val shown = bars.ifEmpty { List(24) { 0.15f } }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val n = shown.size
        val gap = 2.dp.toPx()
        val stroke = ((size.width - gap * (n - 1)) / n).coerceAtLeast(1f)
        val mid = size.height / 2f
        val minHalf = 1.5.dp.toPx()
        val cut = progress.coerceIn(0f, 1f) * n
        shown.forEachIndexed { i, amp ->
            val half = (amp.coerceIn(0f, 1f) * mid).coerceAtLeast(minHalf)
            val x = i * (stroke + gap) + stroke / 2f
            drawLine(
                color = if (i < cut) played else rest,
                start = Offset(x, mid - half),
                end = Offset(x, mid + half),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}
