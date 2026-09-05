package com.safeshade.ui.board

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * A bank of ways that starts closed.
 *
 * Several screens in this app carry fifteen controls where three are the ones
 * anybody touches. Deleting the other twelve is not an option — they are real
 * settings somebody depends on — but presenting all fifteen at equal weight
 * makes the three that matter as hard to find as the twelve that do not, and
 * makes the screen read as a manual.
 *
 * So: the common controls stay in the open, and the rest live behind one of
 * these. Nothing is removed and nothing is hidden — it is one tap away and its
 * label says what is inside.
 *
 * Open state is [rememberSaveable], so a section a user opened survives
 * rotation and a trip through the background. It deliberately does *not*
 * persist across app launches: the closed state is the considered default, and
 * a screen that gradually unfolds itself over weeks of use ends up back where
 * it started.
 *
 * @param count shown beside the label. Worth the small cost: "12 more" tells
 *   someone whether it is worth opening in a way "More settings" cannot.
 */
@Composable
fun ExpandableSection(
    label: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    initiallyOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    var open by rememberSaveable(label) { mutableStateOf(initiallyOpen) }
    ExpandableSection(
        label = label,
        open = open,
        onOpenChange = { open = it },
        modifier = modifier,
        count = count,
        content = content
    )
}

/**
 * The hoisted form, for the few callers that need to open a section from
 * elsewhere — a deep link that lands on a specific setting, say.
 */
@Composable
fun ExpandableSection(
    label: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.board
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(Motion.normal),
        label = "expand-chevron"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .rowClickable(role = Role.Button, onClick = { onOpenChange(!open) })
                .defaultMinSize(minHeight = Spacing.touchTarget)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                // One node, one announcement. Expanded state rides as a state
                // description rather than as a second focusable chevron.
                .clearAndSetSemantics {
                    contentDescription = if (count != null) "$label, $count settings" else label
                    stateDescription = if (open) "Expanded" else "Collapsed"
                }
        ) {
            Nameplate(label, modifier = Modifier.weight(1f), muted = true)
            if (count != null && !open) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkFaint
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(20.dp).rotate(chevron)
            )
        }

        AnimatedVisibility(
            visible = open,
            enter = expandVertically(tween(Motion.normal)) + fadeIn(tween(Motion.normal)),
            exit = shrinkVertically(tween(Motion.fast)) + fadeOut(tween(Motion.fast))
        ) {
            Column { content() }
        }
    }
}

/**
 * The long explanation, available rather than unavoidable.
 *
 * Ten screens in this app opened with two or three paragraphs telling you what
 * the screen was for. The information is genuinely worth having — fall
 * sensitivity and silent SOS both do surprising things, and a guardian setting
 * them for someone else deserves to understand what they are choosing. But
 * prose above the controls is read once and skipped forever after, while
 * costing every future visit a screenful of scrolling.
 *
 * So each screen keeps one short orienting line in the open, and the rest goes
 * behind this. Set into the recess, because an explanation is reference
 * material set into the panel rather than another control mounted on it.
 */
@Composable
fun WhyDisclosure(
    text: String,
    modifier: Modifier = Modifier,
    label: String = "Why this matters"
) {
    val colors = MaterialTheme.board
    var open by rememberSaveable(label, text.take(24)) { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (open) 180f else 0f,
        animationSpec = tween(Motion.normal),
        label = "why-chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(tween(Motion.normal))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .rowClickable(role = Role.Button, onClick = { open = !open })
                .defaultMinSize(minHeight = Spacing.touchTarget)
                .clearAndSetSemantics {
                    contentDescription = label
                    stateDescription = if (open) "Expanded" else "Collapsed"
                }
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = colors.inkMuted
            )
            Spacer(Modifier.width(Spacing.xs))
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(18.dp).rotate(chevron)
            )
        }
        if (open) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
        }
    }
}

