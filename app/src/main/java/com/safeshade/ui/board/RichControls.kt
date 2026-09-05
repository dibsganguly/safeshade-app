package com.safeshade.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import kotlin.math.abs
import kotlin.math.min
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
 * @param onCommit fired once when the drag ends, for callers whose write is
 *   expensive. Anything that goes out over BLE needs this: Android allows one
 *   outstanding GATT operation at a time, so a write per frame spends the whole
 *   gesture draining a queue and drops the last value - the only one that
 *   matters. Null means the value is cheap and [onValueChange] is enough.
 */
@Composable
fun DialControl(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onCommit: (() -> Unit)? = null,
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
            BoardSlider(
                value = current,
                onValueChange = onValueChange,
                onValueChangeFinished = onCommit,
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
                dayStripBackground(colors)

                // The marker is a bar, not a filled track. A time is a point in
                // the day, and filling everything before it would read as a
                // magnitude - "more time" - which is not what is being chosen.
                marker(minutes, tint)
            }

            Spacer(Modifier.height(Spacing.xs))
            DayMarks(colors)

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

/**
 * Two times of day at once, on the same strip, with the span between them lit.
 *
 * Quiet hours is the setting this exists for, and it is the reason the span has
 * to wrap: quiet hours almost always cross midnight, so the band is drawn as
 * the two pieces it really is when the end is earlier than the start, rather
 * than being treated as an invalid range. Two separate hour fields - which is
 * what ships today - make "22:00 to 07:00" something the user has to hold in
 * their head; here it is simply the lit part of the night.
 *
 * Snapped to the hour by default, because the underlying setting is stored as
 * two whole hours (`quietStartHour` / `quietEndHour`), and offering minutes the
 * model cannot hold would be a control that silently rounds what it was told.
 *
 * The handle that moves is whichever is nearer the touch, measured *around* the
 * clock rather than along the strip - so a grab just after midnight picks up a
 * 23:00 handle, which is the one visually closest even though it sits at the
 * far end of the pixels.
 */
@Composable
fun RangeStrip(
    label: String,
    startMinutes: Int,
    endMinutes: Int,
    onChange: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier,
    snapMinutes: Int = 60,
    accent: Color = Color.Unspecified,
    advice: ((Int, Int) -> String)? = null
) {
    val colors = MaterialTheme.board
    val tint = accent.takeOrElse { colors.brass }
    val start = startMinutes.coerceIn(0, MINUTES_IN_DAY - 1)
    val end = endMinutes.coerceIn(0, MINUTES_IN_DAY - 1)

    val latestOnChange by rememberUpdatedState(onChange)
    val width = remember { mutableFloatStateOf(0f) }
    // Which handle the current drag owns. Decided once on the way down and held
    // for the whole gesture: recomputing it per movement lets a fast drag past
    // the other handle hand the gesture over mid-stroke, which feels like the
    // control fighting back.
    val grabbed = remember { mutableIntStateOf(GRAB_START) }

    fun minutesAt(x: Float): Int? {
        val w = width.floatValue
        if (w <= 0f) return null
        val fraction = (x / w).coerceIn(0f, 1f)
        val step = snapMinutes.coerceAtLeast(1)
        val snapped = ((fraction * MINUTES_IN_DAY) / step).roundToInt() * step
        return snapped % MINUTES_IN_DAY
    }

    fun grabNearest(x: Float) {
        val at = minutesAt(x) ?: return
        grabbed.intValue =
            if (aroundTheClock(at, start) <= aroundTheClock(at, end)) GRAB_START else GRAB_END
    }

    fun moveGrabbed(x: Float) {
        val at = minutesAt(x) ?: return
        if (grabbed.intValue == GRAB_START) latestOnChange(at, end) else latestOnChange(start, at)
    }

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            // The span caption sits under the readout, not beside it.
            // "22:00 - 09:00" at readout size is most of a phone wide, so the
            // weighted spacer between them collapsed to nothing and "11 hours"
            // ended up touching the last digit. Seen on a device; a preview
            // with a narrower value does not show it.
            Readout(
                label = label,
                value = formatClock(start) + " - " + formatClock(end),
                large = true
            )
            Text(
                text = spanWords(start, end),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
            Spacer(Modifier.height(Spacing.md))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(STRIP_HEIGHT)
                    .clip(RoundedCornerShape(Radius.plate))
                    .onSizeChanged { width.floatValue = it.width.toFloat() }
                    .pointerInput(Unit) {
                        detectTapGestures { grabNearest(it.x); moveGrabbed(it.x) }
                    }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { grabNearest(it.x) },
                            onHorizontalDrag = { change, _ -> moveGrabbed(change.position.x) }
                        )
                    }
                    .semantics {
                        contentDescription = label
                        stateDescription = formatClock(start) + " to " + formatClock(end)
                    }
            ) {
                dayStripBackground(colors)

                // A fill, not two bare markers, because *this* control genuinely
                // is choosing an extent rather than a point in the day.
                //
                // 0.38, not the 0.22 this started at. The band is drawn on top
                // of the night shading, and brass at low alpha over a warm
                // recess came out so close to the night bands that the chosen
                // span could not be told from the unchosen one - which is the
                // only thing the control has to communicate. Checked against
                // both a lit daytime span and one wrapping midnight.
                val lit = tint.copy(alpha = 0.38f)
                if (start <= end) {
                    band(start, end, lit)
                } else {
                    band(start, MINUTES_IN_DAY, lit)
                    band(0, end, lit)
                }
                marker(start, tint)
                marker(end, tint)
            }

            Spacer(Modifier.height(Spacing.xs))
            DayMarks(colors)

            if (advice != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = advice(start, end),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted
                )
            }
        }
    }
}

/**
 * The strip itself: the recess, the two night bands and the hour ticks.
 *
 * Shared by both strips so the day always looks like the same day.
 */
private fun DrawScope.dayStripBackground(colors: BoardColors) {
    val w = size.width
    val h = size.height

    drawRect(color = colors.recess, size = Size(w, h))

    val nightEnd = w * (NIGHT_END_HOUR / 24f)
    val nightStart = w * (NIGHT_START_HOUR / 24f)
    val night = colors.ink.copy(alpha = if (colors.isDark) 0.10f else 0.06f)
    drawRect(color = night, size = Size(nightEnd, h))
    drawRect(color = night, topLeft = Offset(nightStart, 0f), size = Size(w - nightStart, h))

    // Hour ticks, with the quarter-day marks drawn full height so the strip has
    // landmarks to aim between.
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
}

/** One handle: a full-height bar, kept inside the strip at either extreme. */
private fun DrawScope.marker(minutes: Int, color: Color) {
    val w = size.width
    val barWidth = 3.dp.toPx()
    val x = w * (minutes / MINUTES_IN_DAY.toFloat())
    drawRect(
        color = color,
        topLeft = Offset((x - barWidth / 2f).coerceIn(0f, w - barWidth), 0f),
        size = Size(barWidth, size.height)
    )
}

/** A lit span between two minute-of-day positions. */
private fun DrawScope.band(fromMinutes: Int, toMinutes: Int, color: Color) {
    val w = size.width
    val x0 = w * (fromMinutes / MINUTES_IN_DAY.toFloat())
    val x1 = w * (toMinutes / MINUTES_IN_DAY.toFloat())
    if (x1 <= x0) return
    drawRect(color = color, topLeft = Offset(x0, 0f), size = Size(x1 - x0, size.height))
}

/** The strip's captions. Five marks, because midnight appears at both ends. */
@Composable
private fun DayMarks(colors: BoardColors) {
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
}

private const val GRAB_START = 0
private const val GRAB_END = 1

/**
 * The shorter way round a 24-hour clock face between two times.
 *
 * Plain subtraction would call 23:30 and 00:30 twenty-three hours apart, which
 * would hand a drag near midnight to the wrong handle.
 */
internal fun aroundTheClock(a: Int, b: Int): Int {
    val d = abs(a - b) % MINUTES_IN_DAY
    return min(d, MINUTES_IN_DAY - d)
}

/** How long the lit span lasts, in words, wrapping past midnight. */
internal fun spanWords(start: Int, end: Int): String {
    val span = ((end - start) + MINUTES_IN_DAY) % MINUTES_IN_DAY
    if (span == 0) return "nothing selected"
    val hours = span / 60
    val minutes = span % 60
    return when {
        hours == 0 -> minutes.toString() + " min"
        minutes == 0 && hours == 1 -> "1 hour"
        minutes == 0 -> hours.toString() + " hours"
        else -> hours.toString() + "h " + minutes + "m"
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

/**
 * A slider in this panel's material.
 *
 * Material 3's stock `Slider` was the last piece of another design system
 * visible in the build, and unlike the snackbar it was on screen the whole time
 * somebody was choosing a value: a wide pill thumb with three dots milled into
 * it, a lavender inactive track from a colour role nothing else in this app
 * uses, and tick marks in a fourth tone. Four decisions, none of them this
 * app's, on the control that sets a fall countdown.
 *
 * Drawn as a handle on a channel instead. The track is the recess this panel
 * routes everywhere else, the filled part is ink, and the thumb is a
 * square-shouldered slug at [Radius.tight] - the corner every other small
 * mechanical part in the system takes. Tick marks are gone: the stepping is
 * felt when the handle snaps, and eight dots under a handle is decoration that
 * looks like data.
 *
 * The thumb is deliberately narrow and tall rather than round. A circle reads
 * as a bead sliding on a wire; a slug reads as a control seated in a channel,
 * which is what everything else on this panel is.
 */
// SliderDefaults.Track and the thumb/track slots are still marked experimental.
// Opted into here rather than at the module level, so the annotation stays next
// to the one place that actually depends on the API: if a future Compose
// release changes its shape, this function is what breaks and this comment is
// what the next person reads.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoardSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val colors = MaterialTheme.board
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = colors.ink,
            activeTrackColor = colors.ink,
            inactiveTrackColor = colors.recess,
            // The ticks are drawn in the track colour so they vanish rather
            // than being drawn and then hidden - Slider has no way to switch
            // them off.
            activeTickColor = colors.ink,
            inactiveTickColor = colors.recess,
            disabledThumbColor = colors.inkFaint,
            disabledActiveTrackColor = colors.inkFaint,
            disabledInactiveTrackColor = colors.recess
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(width = SliderThumbW, height = SliderThumbH)
                    .clip(RoundedCornerShape(Radius.tight))
                    .background(colors.ink)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                colors = SliderDefaults.colors(
                    activeTrackColor = colors.ink,
                    inactiveTrackColor = colors.recess,
                    activeTickColor = colors.ink,
                    inactiveTickColor = colors.recess
                ),
                // Both of Material's extra marks off. `drawStopIndicator` is
                // the dot Material 3 parks at the far end of the track, which
                // on this panel reads as a value sitting there rather than as
                // the end of a range; `drawTick` is the row of pips, and the
                // stepping is already felt when the handle snaps.
                drawStopIndicator = null,
                drawTick = { _, _ -> },
                modifier = Modifier.height(SliderTrackH)
            )
        }
    )
}

/** The handle: a slug seated in a channel, not a bead on a wire. */
private val SliderThumbW: Dp = 8.dp
private val SliderThumbH: Dp = 28.dp

/** Thinner than Material's 16dp, which reads as a bar rather than a channel. */
private val SliderTrackH: Dp = 8.dp
