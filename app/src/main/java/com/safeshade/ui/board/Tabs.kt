package com.safeshade.ui.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * Two channel controls adopted from the v2.0 candidates in v2.8.0. Both are
 * a recess with two to four cells and one cell raised to the plate tone. They
 * differ in what the raised cell means: a [PageTabs] cell chooses what a bank
 * *shows*, a [SegmentedChoice] cell chooses what a setting *is*, and the
 * second carries a lamp so a chosen behaviour reads as a lit circuit.
 */

/**
 * Tabs inside a page (2.98): two or three words in a channel at the top of a
 * bank, the chosen one raised, switching what the bank shows. Recent and All,
 * Mine and Community, This week and Older. A page that stacks both lists is
 * twice as long as a page that shows one.
 */
@Composable
fun PageTabs(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.plate))
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        tabs.forEachIndexed { i, word ->
            val on = i == selected
            val fill by animateColorAsState(if (on) colors.plate else Color.Transparent, tween(Motion.fast), label = "tab-fill")
            val edge by animateColorAsState(if (on) colors.hairline else Color.Transparent, tween(Motion.fast), label = "tab-edge")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(Radius.tight))
                    .background(fill)
                    .border(Stroke.hairline, edge, RoundedCornerShape(Radius.tight))
                    .selectable(selected = on, role = Role.Tab, onClick = { onSelect(i) })
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            ) {
                Text(
                    text = word,
                    style = if (on) MaterialTheme.boardType.nameplateSmall.copy(fontWeight = FontWeight.W700) else MaterialTheme.boardType.nameplateSmall,
                    color = if (on) colors.ink else colors.inkMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * A segmented choice (2.24): two to four named behaviours in one channel, the
 * chosen one raised to plate with an ink edge and a lit lamp at its leading
 * edge, and the chosen option's consequence as the one line under the
 * control. Fall sensitivity's three rows with three sentences become one
 * control and one sentence.
 *
 * For a small set of *behaviours*: Low, Medium, High; Off, Vibrate, Ring. A
 * quantity that only looks like a small set is a `DialControl`.
 *
 * @param consequences one line per option, shown for the chosen one. Pass
 *   an empty list where the options explain themselves.
 * @param sealed marks a setting the wearer cannot change on the device.
 */
@Composable
fun SegmentedChoice(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    consequences: List<String> = emptyList(),
    enabled: Boolean = true,
    sealed: Boolean = false
) {
    val colors = MaterialTheme.board
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.plate))
                .background(colors.recess)
                .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
                .padding(3.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            options.forEachIndexed { i, name ->
                val on = i == selected
                val fill by animateColorAsState(if (on) colors.plate else Color.Transparent, tween(Motion.fast), label = "choice-fill")
                val edge by animateColorAsState(if (on) colors.ink else Color.Transparent, tween(Motion.fast), label = "choice-edge")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Spacing.touchTarget)
                        .clip(RoundedCornerShape(Radius.tight))
                        .background(fill)
                        .border(Stroke.hairline, edge, RoundedCornerShape(Radius.tight))
                        .selectable(selected = on, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(i) })
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                ) {
                    PilotLamp(
                        state = if (on) LampState.LIVE else LampState.OFF,
                        size = 10.dp,
                        description = if (on) "In use" else "Not chosen"
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = name,
                        style = if (on) MaterialTheme.boardType.nameplate.copy(fontWeight = FontWeight.W700) else MaterialTheme.boardType.nameplate,
                        color = when {
                            !enabled -> colors.inkFaint
                            on -> colors.ink
                            else -> colors.inkMuted
                        },
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        val line = consequences.getOrNull(selected)
        if (line != null || sealed) {
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(horizontal = Spacing.xs)) {
                if (sealed) {
                    Seal(modifier = Modifier.padding(top = 1.dp))
                    Spacer(Modifier.width(Spacing.sm))
                }
                if (line != null) {
                    Text(line, style = MaterialTheme.boardType.rowDetail, color = colors.inkMuted, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
