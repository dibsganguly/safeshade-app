package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The explainers adopted from the v2.0 candidates in v2.8.0.
 *
 * Every one of them exists to retire a paragraph. The test a screen applies
 * before reaching for prose is: is this one consequence a person must not
 * miss (a [Callout]), one fact read after the rows (a [Footnote]), a sequence
 * (a [Chain], [Steps] or a [Timeline]), a set of facts (a [Ledger]), or a
 * qualifier on a row (a [QualifierChip])? If it is none of those, it is a
 * sentence the label above already said, and it is cut.
 */

/**
 * A callout (2.26): a lead of three or four words, one sentence under it
 * with real air between, and an accent rule down the leading edge. For the
 * one thing on a page a person must not miss: that the emergency number is
 * never dialled by itself, that a recording stays on the phone. A page
 * carries one of these at most.
 */
@Composable
fun Callout(
    lead: String,
    sentence: String,
    modifier: Modifier = Modifier,
    /** Left null, derived from the lead. A state ink is legal here when the callout reports one. */
    accent: Color? = null
) {
    val colors = MaterialTheme.board
    val rule = accent ?: colors.accentFor(lead)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.plate))
            .background(colors.plate)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.plate))
            .height(IntrinsicSize.Min)
    ) {
        Box(Modifier.width(Stroke.heavy).fillMaxHeight().background(rule))
        Column(Modifier.padding(start = Spacing.md, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md)) {
            Text(lead, style = MaterialTheme.typography.titleSmall, color = colors.ink)
            Spacer(Modifier.height(Spacing.sm))
            Text(sentence, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
    }
}

/**
 * A footnote (2.25): the one sentence a bank needs, under the plate rather
 * than in a paragraph above it. Read after the rows, which is when a person
 * wants it, and set at the row's own text inset so it lines up with the
 * titles above it. The glyph sits on a small recess tile so it reads as a
 * mark in the margin, not as a stray icon.
 */
@Composable
fun Footnote(text: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xs),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp)
                .clip(RoundedCornerShape(Radius.tight))
                .background(colors.recess)
                .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.tight))
        ) {
            Icon(SafeShadeIcons.Info, contentDescription = null, tint = colors.inkFaint, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = text,
            style = MaterialTheme.boardType.rowDetail.copy(lineHeight = 17.sp),
            color = colors.inkFaint,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * A qualifier chip (2.29): the seal, generalised. NEEDS SIM, DEVICE-ONLY,
 * PLUS, SIGNED OUT, in the same micro-caps on the same brass tint. Each
 * replaces a sentence under a row with a stamp beside its title, and each is
 * a fact the row cannot change.
 */
@Composable
fun QualifierChip(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = MaterialTheme.board
    Text(
        text = text.uppercase(),
        style = MaterialTheme.boardType.sealPlate,
        color = color ?: colors.inkMuted,
        modifier = modifier
            .clip(RoundedCornerShape(Radius.tight))
            .background(colors.brass.copy(alpha = if (colors.isDark) 0.28f else 0.20f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    )
}

/** One stop in a [Chain]. */
data class ChainStop(val icon: ImageVector, val word: String, val sub: String)

/**
 * What happens, as a chain (2.27): three or four stops across one line,
 * each a glyph on a disc with a word and a sub-line under it, an arrow
 * between. "A fall is detected, thirty seconds pass, Meera is called, then
 * the ladder" is read in one glance. For the top of Safety and of the ladder
 * page, in place of the paragraph.
 */
@Composable
fun Chain(stops: List<ChainStop>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    val spoken = stops.joinToString(", then ") { "${it.word} ${it.sub}" }
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg)
                .semantics { contentDescription = spoken }
        ) {
            stops.forEachIndexed { i, stop ->
                if (i > 0) {
                    Icon(
                        SafeShadeIcons.ArrowRight01,
                        contentDescription = null,
                        tint = colors.inkFaint,
                        modifier = Modifier.padding(top = 11.dp).size(14.dp)
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconDisc(stop.icon, colors.accentFor(stop.word))
                    Spacer(Modifier.height(Spacing.xs))
                    Text(stop.word, style = MaterialTheme.boardType.nameplateSmall, color = colors.ink, textAlign = TextAlign.Center)
                    Text(stop.sub, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.W400), color = colors.inkFaint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** One step in [Steps]. [done] fills the ring; [current] marks the one to do now. */
data class Step(val lead: String, val line: String, val done: Boolean = false, val current: Boolean = false)

/**
 * Steps down the page (2.96): numbered rings on a bus, a bold lead and one
 * line each. A done step's ring fills in the live glass with a tick; the
 * current step's ring and lead take the step's own accent so the eye lands
 * on the one thing to do; steps still to come sit in faint ink. A
 * paragraph hides that a page is a procedure; this admits it.
 */
@Composable
fun Steps(steps: List<Step>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            steps.forEachIndexed { i, step ->
                val accent = colors.accentFor(step.lead)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp).fillMaxHeight()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(Radius.plate))
                                .background(
                                    when {
                                        step.done -> colors.lampLive
                                        step.current -> accent.copy(alpha = 0.16f)
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    Stroke.rule,
                                    when {
                                        step.done -> colors.lampLive
                                        step.current -> accent
                                        else -> colors.hairline
                                    },
                                    RoundedCornerShape(Radius.plate)
                                )
                        ) {
                            if (step.done) {
                                Icon(SafeShadeIcons.Tick02, contentDescription = "Done", tint = com.safeshade.ui.theme.BrandCharcoal, modifier = Modifier.size(14.dp))
                            } else {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.boardType.readout.copy(fontSize = 13.sp, lineHeight = 13.sp),
                                    color = if (step.current) accent else colors.inkFaint
                                )
                            }
                        }
                        if (i < steps.lastIndex) {
                            Box(Modifier.width(2.dp).weight(1f).background(if (step.done) colors.lampLive.copy(alpha = 0.5f) else colors.hairline))
                        }
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f).padding(bottom = if (i < steps.lastIndex) Spacing.lg else 0.dp)) {
                        Text(
                            step.lead,
                            style = MaterialTheme.boardType.nameplate,
                            color = when {
                                step.done -> colors.inkMuted
                                step.current -> colors.ink
                                else -> colors.inkMuted
                            }
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(step.line, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
                    }
                }
            }
        }
    }
}

/** One line of a [Ledger]. [mono] for a value that is a quantity. */
data class LedgerRow(val key: String, val value: String, val mono: Boolean = false, val state: LampState? = null)

/**
 * A ledger (2.22): key on the left, value on the right, a dotted leader
 * between, one line each. The medical ID's fields, a wearable's facts, the
 * smart-home hook's last firing: each is a ledger and none is prose. An
 * absent value is a dash, in faint ink.
 */
@Composable
fun Ledger(rows: List<LedgerRow>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
            rows.forEach { row -> LedgerLine(row) }
        }
    }
}

/** One ledger line, for a caller that already has a plate. */
@Composable
fun LedgerLine(row: LedgerRow, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    val absent = row.value.isBlank() || row.value == "—"
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(row.key, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        Spacer(Modifier.width(Spacing.sm))
        Box(Modifier.weight(1f).height(1.dp)) { DottedLeader() }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = if (absent) "—" else row.value,
            style = if (row.mono) MaterialTheme.boardType.readout else MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600),
            color = when {
                absent -> colors.inkFaint
                row.state == LampState.LIVE -> colors.inkLive
                row.state == LampState.ATTENTION -> colors.inkAttention
                row.state == LampState.TRIP -> colors.inkTrip
                else -> colors.ink
            },
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DottedLeader() {
    val colors = MaterialTheme.board
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(80) {
            Box(Modifier.size(1.5.dp).clip(RoundedCornerShape(Radius.tight)).background(colors.hairline))
        }
    }
}

/** One stop on a [Timeline]. */
data class TimelineStop(val time: String, val what: String, val state: LampState)

/**
 * A timeline on the bus (2.23): one vertical bus down the left, a lamp at
 * each stop, the time in mono and what happened beside it. A trip's record
 * and a ladder's rungs are both sequences, and a sequence reads as a line
 * with stops, not as a bank of ways that happen to be in order.
 */
@Composable
fun Timeline(stops: List<TimelineStop>, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            stops.forEachIndexed { i, stop ->
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(20.dp).fillMaxHeight()) {
                        if (i > 0) Box(Modifier.width(2.dp).height(8.dp).background(colors.hairline))
                        PilotLamp(stop.state, size = 12.dp)
                        if (i < stops.lastIndex) Box(Modifier.width(2.dp).weight(1f).background(colors.hairline))
                    }
                    Spacer(Modifier.width(Spacing.md))
                    Column(Modifier.weight(1f).padding(top = if (i > 0) 8.dp else 0.dp, bottom = if (i < stops.lastIndex) Spacing.md else 0.dp)) {
                        Text(stop.time, style = MaterialTheme.boardType.readout, color = colors.inkMuted)
                        Text(stop.what, style = MaterialTheme.typography.bodyMedium, color = colors.ink)
                    }
                }
            }
        }
    }
}
