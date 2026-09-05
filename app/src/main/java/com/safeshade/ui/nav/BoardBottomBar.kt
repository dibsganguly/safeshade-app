package com.safeshade.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.safeshade.data.UserRole
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The four primary destinations.
 *
 * A Material `NavigationBar` rather than something hand-rolled — this is
 * exactly the component Android users expect at the bottom of a phone app, and
 * replacing it with a custom pill is the most common way an app announces that
 * it was designed for iOS. It is themed to the board (flat plate, hairline top
 * rule, engraved labels, a square selection plate instead of a rounded pill)
 * without departing from the component's behaviour.
 */
@Composable
fun BoardBottomBar(
    navController: NavController,
    role: UserRole,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column(modifier = modifier.fillMaxWidth()) {
        // The bar is separated by a rule, not a shadow. Nothing on this panel
        // floats above anything else.
        Box(
            Modifier
                .fillMaxWidth()
                .height(Stroke.hairline)
                .background(colors.hairline)
        )
        NavigationBar(
            containerColor = colors.plate,
            tonalElevation = 0.dp
        ) {
            BottomDestination.entries.forEach { destination ->
                // Sub-screens keep their parent tab lit, so a user three levels
                // into Safety still knows where they are.
                val selected = currentRoute?.startsWith(destination.route) == true

                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(destination.route) {
                                // Anchored to the graph's real start destination
                                // rather than a hardcoded route: the previous
                                // version pinned this to "home", which breaks the
                                // moment onboarding is the start destination.
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = destination.labelFor(role).uppercase(),
                            style = MaterialTheme.boardType.nameplateSmall
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.ink,
                        selectedTextColor = colors.ink,
                        unselectedIconColor = colors.inkFaint,
                        unselectedTextColor = colors.inkFaint,
                        indicatorColor = colors.brass.copy(alpha = if (colors.isDark) 0.30f else 0.22f)
                    )
                )
            }
        }
    }
}

private fun BottomDestination.labelFor(role: UserRole): String =
    when (role) {
        UserRole.GUARDIAN -> guardianLabel
        UserRole.COMPANION -> companionLabel
    }

private val BottomDestination.icon: ImageVector
    get() = when (this) {
        BottomDestination.BOARD -> Icons.Outlined.Dashboard
        BottomDestination.CIRCLE -> Icons.Outlined.Groups
        BottomDestination.SAFETY -> Icons.Outlined.Shield
        BottomDestination.DEVICE -> Icons.Outlined.Watch
    }
