package com.safeshade.ui.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * The top of a screen. One treatment, everywhere.
 *
 * Before this existed, four peer screens opened four different ways —
 * `headlineSmall` in Safety, `titleMedium` in Circle, `displaySmall` in Device
 * and Settings — and the difference was visible the moment you moved between
 * tabs. None of the four was wrong on its own; having four was.
 *
 * The convergence goes **upward**. [ScreenTier.PUSHED] lands at
 * `headlineMedium`, above both incumbents rather than at the smaller of them:
 * these headings are the first thing read on a screen somebody may be opening
 * in a hurry, and this app's stated audience includes elderly guardians. Tab
 * roots keep a step above that again, so depth is still legible at a glance.
 *
 * The generous top margin is deliberate and is why [topMargin] is not a
 * caller's decision. A heading crowding the status bar was the specific
 * complaint; leaving it to call sites is how it comes back on one screen in
 * six months.
 *
 * @param onBack null means no back control — correct for a tab root, which has
 *   nowhere to go. Every pushed screen passes one. It is nullable rather than
 *   defaulted-to-empty so a call site that forgets shows a visibly absent
 *   arrow instead of a drawn dead one.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    tier: ScreenTier = ScreenTier.PUSHED,
    /** Spoken name for the back control. "Back" alone is vague out of context. */
    backDescription: String = "Back",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board

    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(if (tier == ScreenTier.ROOT) Spacing.lg else Spacing.sm))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Spacing.touchTarget)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    // Pulled back into the gutter so the arrow's optical edge
                    // lines up with the text below it rather than its 48dp
                    // touch box, which would leave the title indented.
                    modifier = Modifier.offset(x = (-12).dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = backDescription,
                        tint = colors.inkMuted
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = when (tier) {
                        ScreenTier.ROOT -> MaterialTheme.typography.displaySmall
                        ScreenTier.PUSHED -> MaterialTheme.typography.headlineMedium
                    },
                    color = colors.ink,
                    modifier = Modifier.semantics { heading() }
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.sm))
                trailing()
            }
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

/** How deep a screen sits. Decides the heading's weight, nothing else. */
enum class ScreenTier {
    /** A bottom-bar destination. */
    ROOT,

    /** Anything pushed on top of one. */
    PUSHED
}

/**
 * A header with the back control but no separate title row, for screens whose
 * first content item already names them (a large illustrated hero, a live map).
 */
@Composable
fun BackOnlyHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    backDescription: String = "Back",
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = backDescription,
                tint = colors.inkMuted
            )
        }
        Spacer(Modifier.weight(1f))
        trailing?.invoke()
    }
}
