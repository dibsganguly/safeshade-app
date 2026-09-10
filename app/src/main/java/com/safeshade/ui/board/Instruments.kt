package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * Twenty-four bars (2.38): a day as twenty-four flat bars in one plate, each
 * an hour's reading, the threshold as a hairline across, breaches in the
 * attention glass. A list of readings is a ledger; a day of readings is a
 * shape, and a shape is read in a glance.
 *
 * The plate's head is the instrument's own: a small muted nameplate, the
 * latest value large in mono with its unit, and one line of state ink at the
 * right saying what the shape means ("2 above 110"). Hours with no reading
 * draw a short stub in the hairline rather than nothing, so a gap in the
 * day is visible as a gap and not as a zero. The latest hour with a reading
 * is drawn in ink so the eye finds "now".
 *
 * @param values twenty-four fractions of the plate's full height, midnight
 *   first, null where there is no reading.
 * @param threshold the same fraction the [values] use, or null for no line.
 */
@Composable
fun HourBars(
    label: String,
    value: String,
    values: List<Float?>,
    modifier: Modifier = Modifier,
    unit: String? = null,
    threshold: Float? = null,
    note: String? = null,
    noteState: LampState? = null,
    /** Spoken summary of the shape, since a screen reader cannot read bars. */
    description: String? = null
) {
    val colors = MaterialTheme.board
    val latest = values.indexOfLast { it != null }
    val plotHeight = 64.dp
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(Spacing.lg)
                .semantics { if (description != null) contentDescription = description }
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Nameplate(label, small = true, muted = true)
                    Spacer(Modifier.height(Spacing.xs))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(value, style = MaterialTheme.boardType.readoutLarge, color = colors.ink)
                        if (unit != null) {
                            Spacer(Modifier.width(Spacing.xs))
                            Text(unit, style = MaterialTheme.typography.bodySmall, color = colors.inkFaint, modifier = Modifier.padding(bottom = 5.dp))
                        }
                    }
                }
                if (note != null) {
                    Text(
                        note,
                        style = MaterialTheme.boardType.rowDetail,
                        color = when (noteState) {
                            LampState.LIVE -> colors.inkLive
                            LampState.ATTENTION -> colors.inkAttention
                            LampState.TRIP -> colors.inkTrip
                            else -> colors.inkFaint
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(plotHeight)
                    .clip(RoundedCornerShape(Radius.plate))
                    .background(colors.recess)
                    .padding(horizontal = Spacing.xs)
            ) {
                Row(
                    Modifier.fillMaxWidth().fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    values.take(24).forEachIndexed { i, v ->
                        val over = v != null && threshold != null && v > threshold
                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(v?.coerceIn(0.03f, 1f) ?: 0.03f)
                                .clip(RoundedCornerShape(topStart = Radius.tight, topEnd = Radius.tight))
                                .background(
                                    when {
                                        v == null -> colors.hairline
                                        over -> colors.lampAttention
                                        i == latest -> colors.ink
                                        else -> colors.inkFaint.copy(alpha = 0.7f)
                                    }
                                )
                        )
                    }
                }
                if (threshold != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = plotHeight * (1f - threshold.coerceIn(0f, 1f)))
                            .height(Stroke.hairline)
                            .background(colors.inkAttention.copy(alpha = 0.6f))
                    )
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("00", "06", "12", "18", "24").forEach {
                    Text(it, style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp, lineHeight = 13.sp), color = colors.inkFaint)
                }
            }
        }
    }
}
