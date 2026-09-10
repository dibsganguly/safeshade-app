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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * Row and tile forms adopted from the v2.0 candidates in v2.8.0, and the two
 * small parts they share: a glyph on a disc and a state word.
 */

/**
 * A glyph on a soft disc of its accent (2.12). The one round form the kit
 * keeps besides faces and the SOS disc: a disc under a glyph reads as a
 * badge, and a badge is round the way a lamp is square.
 */
@Composable
fun IconDisc(icon: ImageVector, accent: Color, modifier: Modifier = Modifier, size: Dp = 36.dp, glyph: Dp = 20.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(glyph))
    }
}

/** The state word on its own, in the state's ink, for a tile or a well that has no row to sit in. */
@Composable
fun StateWord(label: String, state: LampState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Text(
        text = label.uppercase(),
        style = MaterialTheme.boardType.stateLabel,
        color = when (state) {
            LampState.LIVE -> colors.inkLive
            LampState.ATTENTION -> colors.inkAttention
            LampState.TRIP -> colors.inkTrip
            LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
        },
        modifier = modifier
    )
}

/**
 * A numbered row (2.83): the rung number in a 28dp ring at the leading edge
 * where a way wears its glyph, in mono. The ladder, a set of instructions:
 * anything whose order is the point.
 */
@Composable
fun NumberedRow(
    number: Int,
    name: String,
    state: LampState,
    stateLabel: String,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    val spoken = buildString {
        append("$number, $name, $stateLabel")
        if (detail != null) append(", $detail")
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.rowClickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics { contentDescription = spoken }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(if (detail != null) Alignment.Top else Alignment.CenterVertically)
                .size(28.dp)
                .clip(RoundedCornerShape(Radius.plate))
                .border(Stroke.rule, colors.inkMuted, RoundedCornerShape(Radius.plate))
        ) {
            Text(
                "$number",
                style = MaterialTheme.boardType.readout.copy(fontSize = 13.sp, lineHeight = 13.sp),
                color = colors.ink
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Nameplate(name)
            if (detail != null) {
                Spacer(Modifier.height(Spacing.xs))
                Text(detail, style = MaterialTheme.boardType.rowDetail, color = colors.inkFaint)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(Spacing.md))
        }
        StateWord(stateLabel, state)
        Spacer(Modifier.width(Spacing.md))
        BusTick(state, Modifier.fillMaxHeight())
    }
}

/** One face in a [FaceStack]. */
data class Face(val avatarId: String, val name: String)

/**
 * A stack of faces (2.88), overlapping by a third, for who was told, who
 * answered, who is in the zone. Pass it to a `Way`'s `trailing` slot. A trip
 * that says "Meera and Arun were called" as two faces is read faster than the
 * sentence and takes a third of the width. Spoken as the names.
 */
@Composable
fun FaceStack(faces: List<Face>, modifier: Modifier = Modifier, size: Dp = 28.dp, max: Int = 4) {
    val colors = MaterialTheme.board
    val shown = faces.take(max)
    val more = faces.size - shown.size
    val step = size * 0.66f
    val spoken = faces.joinToString(", ") { it.name }
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(size + step * (shown.size - 1).coerceAtLeast(0) + if (more > 0) step else 0.dp).height(size)) {
            shown.forEachIndexed { i, face ->
                Box(Modifier.offset(x = step * i)) {
                    Avatar(avatarId = face.avatarId, name = face.name, size = size)
                }
            }
            if (more > 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = step * shown.size)
                        .size(size)
                        .clip(CircleShape)
                        .background(colors.recess)
                        .border(Stroke.hairline, colors.hairline, CircleShape)
                ) {
                    Text("+$more", style = MaterialTheme.boardType.readout.copy(fontSize = 11.sp, lineHeight = 11.sp), color = colors.inkMuted)
                }
            }
        }
    }
}

/** One tile in a [TileGrid]. */
data class Tile(
    val title: String,
    val icon: ImageVector,
    val state: LampState,
    val stateLabel: String,
    val onClick: () -> Unit,
    /** A stamp in the tile's corner: PLUS, SEALED. */
    val tag: String? = null
)

/**
 * Ways as tiles (2.84): two across, each a small plate with its title, its
 * state word under it, and its own glyph as a watermark in the state's
 * colour bleeding off the bottom-right corner. For a hub's list of places to
 * go, where every row would otherwise have the same shape and a dash. Rows
 * for settings, tiles for destinations.
 *
 * The watermark is the one place the tile carries colour, and it carries
 * the same colour the row's bus tick would have: live teal, attention amber,
 * trip red, and the hairline for off or unknown. A tile that is not doing
 * anything is not decorated.
 */
@Composable
fun TileGrid(tiles: List<Tile>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        tiles.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pair.forEach { tile -> WayTile(tile, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WayTile(tile: Tile, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    val spoken = "${tile.title}, ${tile.stateLabel}" + (tile.tag?.let { ", $it" } ?: "")
    WatermarkPlate(
        icon = tile.icon,
        tint = watermarkTint(tile.state),
        onClick = tile.onClick,
        glyphSize = 88.dp,
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken }
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(Spacing.md).height(IntrinsicSize.Min)) {
                Icon(tile.icon, contentDescription = null, tint = colors.inkMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.height(Spacing.md))
                Nameplate(tile.title)
                Spacer(Modifier.height(Spacing.xxs))
                StateWord(tile.stateLabel, tile.state)
            }
            if (tile.tag != null) {
                CornerTag(tile.tag, modifier = Modifier.align(Alignment.TopEnd))
            }
        }
    }
}
