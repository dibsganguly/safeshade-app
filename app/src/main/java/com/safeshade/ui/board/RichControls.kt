package com.safeshade.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import kotlin.math.roundToInt

/**
 * The pattern the zone-radius control established, extracted so the rest of the
 * app can stop being a wall of plain fields.
 *
 * Its strength was never the slider. It was the **pairing**: a large readout
 * that says the value in its own units, a stepped track that refuses to offer
 * precision the underlying system cannot honour, and a line of plain English
 * that changes as you drag, so the consequence of a choice is visible at the
 * moment of making it. A bare slider with a percentage above it has none of
 * that, and is what most of this app currently ships.
 *
 * Stepping is not decoration. A continuous control invites fiddling for
 * accuracy that does not exist - fall sensitivity has three real settings, a
 * siren about five audible ones - and a value the user cannot reproduce is a
 * value they cannot trust.
 *
 * @param advice called with the current value on every frame of a drag. Keep it
 *   cheap and keep it honest: this is where the trade-off gets stated, so it
 *   should describe what will actually happen rather than praise the choice.
 */
@Composable
fun DialControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    format: (Float) -> String = { it.roundToInt().toString() },
    unit: String? = null,
    advice: ((Float) -> String)? = null,
    spokenValue: ((Float) -> String)? = null
) {
    val colors = MaterialTheme.board
    val span = valueRange.endInclusive - valueRange.start
    // Slider counts the gaps *between* the ends, so a 0..1 range stepped by
    // 0.25 has three interior stops, not four. Off by one here is the
    // difference between the top of the range being reachable and not.
    val steps = ((span / step).roundToInt() - 1).coerceAtLeast(0)
    val current = value.coerceIn(valueRange)

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Readout(label = label, value = format(current), large = true)
                if (unit != null) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }
            }
            Slider(
                value = current,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    // Without this the value is announced as a bare fraction,
                    // which is meaningless for a volume or a duration.
                    .semantics {
                        contentDescription = label
                        stateDescription = spokenValue?.invoke(current)
                            ?: (format(current) + if (unit != null) " " + unit else "")
                    }
            )
            if (advice != null) {
                Text(
                    text = advice(current),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
            }
        }
    }
}

/**
 * A time of day, picked by pointing at it on a strip of the whole day.
 *
 * This exists because the medication reminder had **no way to set its time at
 * all**: `onEditMedicationTime` was a hardcoded empty lambda at two call sites,
 * so the reminder shipped stuck at whatever it defaulted to. The comment beside
 * it said a picker was left "for whoever owns the device screens", which is how
 * a dead control survives a release.
 *
 * A dialog would have closed the bug. A strip closes it better, because the
 * question here is not "what number" but "when in the day", and a day is a
 * thing with a shape: the strip is shaded through night and back, so 7am and
 * 7pm are told apart by where the marker sits rather than by reading am/pm off
 * a label. That is worth real pixels for an audience that includes people
 * setting a medication reminder without their reading glasses.
 *
 * Minutes snap to five. Nobody takes a tablet at 7:23, and a strip one phone
 * wide cannot resolve a single minute anyway - roughly three minutes to the
 * pixel - so offering it would be a lie about the control's precision.
 *
 * @param minutesOfDay 0..1439. Values outside are clamped rather than rejected,
 *   since this is also fed by settings stored before the control existed.
 */
@Composable
fun TimeStrip(
    label: String,
    minutesOfDay: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Color.Unspecified,
    advice: ((Int) -> String)? = null
) {
    val colors = MaterialTheme.board
    val tint = accent.takeOrElse { colors.brass }
    val minutes = minutesOfDay.coerceIn(0, MINUTES_IN_DAY - 1)

    // The gesture handlers are installed once and must not be torn down and
    // rebuilt on every value change, or a drag is cancelled the instant it
    // moves. So `pointerInput` keys on Unit and reads the live callback through
    // rememberUpdatedState rather than capturing a stale one.
    val latestOnChange by rememberUpdatedState(onChange)
    // Written from the layout phase via onSizeChanged, not assigned inside the
    // draw scope. Writing snapshot state while drawing happens to work here
    // only because nothing reads it during composition, and it buys nothing.
    val width = remember { mutableFloatStateOf(0f) }

    fun emit(x: Float) {
        val w = width.floatValue
        if (w <= 0f) return
        val fraction = (x / w).coerceIn(0f, 1f)
        val snapped = ((fraction * MINUTES_IN_DAY) / SNAP_MINUTES).roundToInt() * SNAP_MINUTES
        latestOnChange(snapped.coerceIn(0, MINUTES_IN_DAY - SNAP_MINUTES))
    }

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Readout(label = label, value = formatClock(minutes), large = true)
                Spacer(Modifier.weight(1f))
                Text(
                    text = partOfDay(minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            Spacer(Modifier.height(Spacing.md))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(STRIP_HEIGHT)
                    .clip(RoundedCornerShape(Radius.plate))
                    .onSizeChanged { width.floatValue = it.width.toFloat() }
                    .pointerInput(Unit) { detectTapGestures { emit(it.x) } }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { change, _ -> emit(change.position.x) }
                    }
                    .semantics {
                        contentDescription = label
                        stateDescription = formatClock(minutes)
                    }
            ) {
                val w = size.width
                val h = size.height

                drawRect(color = colors.recess, size = Size(w, h))

                // Night is drawn, not labelled. The two bands are why a glance
                // at the marker tells morning from evening without reading the
                // clock above it.
                val nightEnd = w * (NIGHT_END_HOUR / 24f)
                val nightStart = w * (NIGHT_START_HOUR / 24f)
                val night = colors.ink.copy(alpha = if (colors.isDark) 0.10f else 0.06f)
                drawRect(color = night, size = Size(nightEnd, h))
                drawRect(
                    color = night,
                    topLeft = Offset(nightStart, 0f),
                    size = Size(w - nightStart, h)
                )

                // Hour ticks, with the quarter-day marks drawn full height so
                // the strip has landmarks to aim between.
                for (hour in 1 until 24) {
                    val x = w * (hour / 24f)
                    val major = hour % 6 == 0
                    drawLine(
                        color = if (major) colors.hairline else colors.hairline.copy(alpha = 0.5f),
                        start = Offset(x, if (major) 0f else h * 0.62f),
                        end = Offset(x, h),
                        strokeWidth = 1f
                    )
                }

                // The marker is a bar, not a filled track. A time is a point in
                // the day, and filling everything before it would read as a
                // magnitude - "more time" - which is not what is being chosen.
                val thumbX = w * (minutes / MINUTES_IN_DAY.toFloat())
                val barWidth = 3.dp.toPx()
                drawRect(
                    color = tint,
                    topLeft = Offset((thumbX - barWidth / 2f).coerceIn(0f, w - barWidth), 0f),
                    size = Size(barWidth, h)
                )
            }

            Spacer(Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (mark in DAY_MARKS) {
                    Text(
                        text = mark,
                        style = MaterialTheme.boardType.rowDetail,
                        color = colors.inkFaint
                    )
                }
            }

            if (advice != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = advice(minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
            }
        }
    }
}

private const val MINUTES_IN_DAY = 24 * 60
private const val SNAP_MINUTES = 5
private const val NIGHT_END_HOUR = 6f
private const val NIGHT_START_HOUR = 21f
private val STRIP_HEIGHT = 56.dp
private val DAY_MARKS = listOf("12am", "6am", "noon", "6pm", "12am")

/**
 * 24-hour, zero-padded, and deliberately not localised to am/pm.
 *
 * The wearable's own screen shows 24-hour time, and displaying "7:30 pm" here
 * against "19:30" there for one setting is the sort of small mismatch that
 * makes a user doubt the reminder was saved at all.
 */
fun formatClock(minutesOfDay: Int): String {
    val m = minutesOfDay.coerceIn(0, MINUTES_IN_DAY - 1)
    return "%02d:%02d".format(m / 60, m % 60)
}

/** The words beside the clock, so the strip's shading has a caption. */
private fun partOfDay(minutesOfDay: Int): String = when (minutesOfDay / 60) {
    in 0..4 -> "the middle of the night"
    in 5..7 -> "early morning"
    in 8..11 -> "morning"
    in 12..13 -> "midday"
    in 14..17 -> "afternoon"
    in 18..20 -> "evening"
    else -> "night"
}
