/**
 * SafeShade - Universal Safety Companion
 *
 * SharedComponents.kt
 *
 * Reusable UI components used across multiple screens.
 * Includes buttons, cards, and other common elements.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshade.ui.theme.*

/**
 * Bouncy button with scale animation on press.
 *
 * Used for primary actions throughout the app with
 * a satisfying press feedback animation.
 *
 * @param onClick Click handler
 * @param enabled Whether button is enabled
 * @param color Background color
 * @param modifier Modifier for the button
 * @param content Button content
 */
@Composable
fun BouncyButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        label = "scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .shadow(if (enabled) 6.dp else 0.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) color else Color.LightGray)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Information card displaying an icon, label, and value.
 *
 * Used on the home screen for weather stats and other metrics.
 *
 * @param modifier Modifier for the card
 * @param icon Icon to display
 * @param iconColor Color of the icon
 * @param label Label text
 * @param value Value text
 */
@Composable
fun InfoCard(
    modifier: Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    val colors = MaterialTheme.safeShadeColors
    Card(
        // Was the deprecated flat CardColor (permanently light-scheme) -
        // stayed a white card in dark mode, same bug class fixed in
        // BottomNavBar.kt.
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontSize = 11.sp, color = colors.onSurfaceMuted)
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

/**
 * Quick message button for Guardian/Companion messaging.
 *
 * Compact button used in message grids with consistent styling.
 *
 * @param text Button text
 * @param color Button background color
 * @param enabled Whether button is enabled
 * @param modifier Modifier for the button
 * @param onClick Click handler
 */
@Composable
fun QuickMessageButton(
    text: String,
    color: Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(45.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Section header with optional trailing content.
 *
 * Used to introduce sections within cards or screens.
 *
 * @param title Section title
 * @param modifier Modifier for the header
 * @param trailingContent Optional trailing content (e.g., badge, button)
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.safeShadeColors.onSurface
        )
        trailingContent?.invoke()
    }
}

/**
 * Badge component for status indicators.
 *
 * @param text Badge text
 * @param color Badge color
 */
@Composable
fun StatusBadge(
    text: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

private enum class AckState { IDLE, WAITING, APPLIED, TIMED_OUT }

/**
 * Small "applied on device" confirmation badge - the on-device
 * acknowledgement counterpart to a settings/health/LED/mode write. Waits
 * on [bleManager].awaitAck([tag]) whenever [trigger] changes (pass the
 * value just written, e.g. the LedPattern or a settings snapshot), then
 * shows a real confirmed/timed-out result instead of the optimistic-only
 * pattern this replaces (see BleManager.sendLedPattern's doc comment).
 *
 * Deliberately the one place a small retro/monospace touch shows up in
 * this app (per the intentional choice to keep the rest of the UI in its
 * existing glass/Material 3 language, with only targeted accents in
 * device-facing spots) - it reads like a tiny device console line.
 */
@Composable
fun AckBadge(
    bleManager: com.safeshade.BleManager,
    tag: String,
    trigger: Any?,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(AckState.IDLE) }

    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        state = AckState.WAITING
        val ok = bleManager.awaitAck(tag)
        state = if (ok) AckState.APPLIED else AckState.TIMED_OUT
        if (ok) {
            kotlinx.coroutines.delay(1600)
            state = AckState.IDLE
        }
    }

    val (label, color) = when (state) {
        AckState.IDLE -> return
        AckState.WAITING -> ">> SYNCING" to MaterialTheme.safeShadeColors.accentPrimary
        AckState.APPLIED -> "SYNCED" to MaterialTheme.safeShadeColors.accentSuccess
        AckState.TIMED_OUT -> "NO ACK" to MaterialTheme.safeShadeColors.accentWarning
    }

    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state == AckState.WAITING) {
            CircularProgressIndicator(
                modifier = Modifier.size(9.dp),
                strokeWidth = 1.5.dp,
                color = color
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(
            label,
            fontSize = 9.sp,
            color = color,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

/**
 * Coming Soon badge for unreleased features.
 */
@Composable
fun ComingSoonBadge() {
    Box(
        modifier = Modifier
            .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            "COMING SOON",
            fontSize = 10.sp,
            color = AccentOrange,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Empty state placeholder with icon and message.
 *
 * @param icon Icon to display
 * @param message Message to display
 * @param modifier Modifier for the component
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.safeShadeColors.onSurfaceMuted,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            message,
            color = MaterialTheme.safeShadeColors.onSurfaceMuted,
            fontSize = 14.sp
        )
    }
}

/**
 * A single tappable settings row: leading icon, title (+ optional subtitle),
 * optional trailing content. Replaces the ad hoc row layouts each screen was
 * hand-rolling for its settings/list items.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: Color = AccentPurple,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val colors = MaterialTheme.safeShadeColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = colors.onSurfaceMuted)
            }
        }
        trailingContent?.invoke()
    }
}

/**
 * Small pill showing BLE connection state with a live-pulsing dot when
 * connected, replacing the several inline "connected"/"disconnected" chips
 * each screen previously hand-rolled independently.
 */
@Composable
fun ConnectionStatusChip(
    connected: Boolean,
    modifier: Modifier = Modifier,
    connectedLabel: String = "Connected",
    disconnectedLabel: String = "Disconnected"
) {
    val color = if (connected) AccentGreen else MaterialTheme.safeShadeColors.onSurfaceFaint
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (connected) {
            LiveDot(color = color)
        } else {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            if (connected) connectedLabel else disconnectedLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/** Small pulsing dot used to mark genuinely live/streaming data. */
@Composable
fun LiveDot(color: Color = AccentGreen, size: androidx.compose.ui.unit.Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "liveDot")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liveDotAlpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = alpha))
    )
}

/**
 * A single chat-style bubble for Guardian<->Companion message history,
 * replacing GuardianScreen's previous raw unstyled AssistChip usage.
 */
@Composable
fun MessageBubble(
    text: String,
    fromGuardian: Boolean,
    timestampLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.safeShadeColors
    val bubbleColor = if (fromGuardian) AccentPurple.copy(alpha = 0.12f) else AccentTeal.copy(alpha = 0.12f)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (fromGuardian) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(text, fontSize = 13.sp, color = colors.onSurface)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(timestampLabel, fontSize = 9.sp, color = colors.onSurfaceMuted)
    }
}
