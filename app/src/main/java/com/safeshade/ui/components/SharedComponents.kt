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

import androidx.compose.animation.core.animateFloatAsState
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
    Card(
        colors = CardDefaults.cardColors(containerColor = CardColor),
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
            Text(label, fontSize = 11.sp, color = TextGray)
            Text(
                value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
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
            color = TextDark
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
            tint = TextGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            message,
            color = TextGray,
            fontSize = 14.sp
        )
    }
}
