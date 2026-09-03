/**
 * SafeShade - Universal Safety Companion
 *
 * OnboardingScreen.kt
 *
 * First-run welcome/explainer flow (item #16) - nothing like this existed
 * before; the app dropped straight into the Home screen on first launch
 * with zero context on what SafeShade/Guardian/Companion mean.
 *
 * @author SafeShade Team
 * @version 1.0.0
 */

package com.safeshade.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeshade.ui.components.BouncyButton
import com.safeshade.ui.theme.*

private data class OnboardingPage(
    val icon: ImageVector,
    val accent: Color,
    val title: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        Icons.Rounded.Shield, AccentPurple,
        "Welcome to SafeShade",
        "Your everything safety companion - pairs with your SafeShade wearable over Bluetooth to watch for falls, share your medical ID, and keep you connected to the people who look out for you."
    ),
    OnboardingPage(
        Icons.Rounded.People, AccentBlue,
        "Guardian & Companion",
        "Guardian is the caregiver's phone - sends messages, medical info, and settings to the wearable. Companion is the wearer's own phone, which receives messages and can reply. Switch between them any time on the Guardian tab."
    ),
    OnboardingPage(
        Icons.Rounded.Warning, AccentRed,
        "Fall detection, honestly",
        "When a fall is detected you'll get an on-screen alert with a countdown before any emergency call is placed - nothing dials automatically without you seeing it first. Turn on auto-call and SMS fallback in Safety settings."
    ),
    OnboardingPage(
        Icons.Rounded.Tune, AccentGreen,
        "Pick your mode",
        "Choose an Adaptive Mode on your Profile that matches how you're wearing SafeShade - Elderly, Kids, Bike, Pet, and more each tune fall sensitivity and simplify the UI automatically."
    )
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pageIndex by remember { mutableStateOf(0) }
    val colors = MaterialTheme.safeShadeColors
    val page = pages[pageIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(page.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(page.icon, contentDescription = null, tint = page.accent, modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            page.title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            page.body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            pages.indices.forEach { i ->
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .width(if (i == pageIndex) 22.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i == pageIndex) page.accent else colors.onSurfaceFaint.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        BouncyButton(
            onClick = {
                if (pageIndex < pages.lastIndex) pageIndex++ else onFinish()
            },
            color = page.accent,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(
                if (pageIndex < pages.lastIndex) "Next" else "Get Started",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        TextButton(onClick = onFinish) {
            Text("Skip", color = colors.onSurfaceMuted)
        }
    }
}
