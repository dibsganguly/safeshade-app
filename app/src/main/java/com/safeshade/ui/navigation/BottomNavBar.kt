/**
 * SafeShade - Universal Safety Companion
 *
 * BottomNavBar.kt
 *
 * Bottom navigation bar component with floating pill design.
 * Handles navigation between main app screens.
 *
 * @author SafeShade Team
 * @version 2.0.0
 */

package com.safeshade.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.safeshade.ui.theme.AccentPurple
import com.safeshade.ui.theme.IconGray

/**
 * Navigation item data class.
 *
 * @property route Navigation route
 * @property icon Icon to display
 * @property label Accessibility label
 */
private data class NavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * List of navigation items in display order.
 * Order: Home -> Guardian -> Safety -> Profile -> Device
 */
private val navItems = listOf(
    NavItem("home", Icons.Rounded.Home, "Home"),
    NavItem("guardian", Icons.Rounded.Favorite, "Guardian"),
    NavItem("safety", Icons.Rounded.Shield, "Safety"),
    NavItem("profile", Icons.Rounded.Person, "Profile"),
    NavItem("device", Icons.Rounded.Smartphone, "Device")
)

/**
 * SafeShade bottom navigation bar with floating pill design.
 *
 * Features:
 * - Floating card design with elevation
 * - Circular icon buttons with selection highlight
 * - Smooth navigation with state preservation
 *
 * @param navController Navigation controller for routing
 * @param parentalControlsEnabled Whether parental controls are active (unused, reserved for future)
 */
@Composable
fun SafeShadeBottomBar(
    navController: NavController,
    parentalControlsEnabled: Boolean = false
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp, start = 32.dp, end = 32.dp)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    NavBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to start destination to avoid building up back stack
                                popUpTo("home") { saveState = true }
                                // Avoid multiple copies of same destination
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Single navigation bar item.
 *
 * @param item Navigation item data
 * @param isSelected Whether this item is currently selected
 * @param onClick Click handler
 */
@Composable
private fun NavBarItem(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) AccentPurple else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (isSelected) Color.White else IconGray,
            modifier = Modifier.size(22.dp)
        )
    }
}
