package com.safeshade.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.BuildConfig
import com.safeshade.data.DarkModePreference
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.nav.Routes
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

// ============================================================================
// Shared across com.safeshade.ui.screens.settings
// ============================================================================

/** How a role reads on a nameplate. */
internal val UserRole.label: String
    get() = when (this) {
        UserRole.GUARDIAN -> "Guardian"
        UserRole.COMPANION -> "Companion"
    }

/** One sentence on who the app is talking to in this role. */
internal val UserRole.blurb: String
    get() = when (this) {
        UserRole.GUARDIAN -> "You look after someone who wears the device."
        UserRole.COMPANION -> "You wear the device yourself."
    }

/**
 * What each dark-mode option actually does.
 *
 * Carried over verbatim from the retired Appearance screen — the wording was
 * the good part of that page and there was no reason to lose it along with the
 * route.
 */
internal val DarkModePreference.blurb: String
    get() = when (this) {
        DarkModePreference.SYSTEM -> "Follows the phone, including its night schedule"
        DarkModePreference.LIGHT -> "Always the bone panel, whatever the phone is set to"
        DarkModePreference.DARK -> "Always the night panel – easier to read in a dark hallway"
    }

internal val DarkModePreference.icon: ImageVector
    get() = when (this) {
        DarkModePreference.SYSTEM -> Icons.Outlined.Brightness4
        DarkModePreference.LIGHT -> Icons.Outlined.LightMode
        DarkModePreference.DARK -> Icons.Outlined.DarkMode
    }

/** How a dark-mode preference reads on a nameplate. */
internal val DarkModePreference.label: String
    get() = when (this) {
        DarkModePreference.SYSTEM -> "System"
        DarkModePreference.LIGHT -> "Light"
        DarkModePreference.DARK -> "Dark"
    }

// ============================================================================
// The screen
// ============================================================================

/** Everything the settings hub draws. */
data class SettingsUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM,
    /**
     * How many reliability checks are currently failing.
     *
     * Surfaced on the hub rather than buried, because every one of them is a
     * way for a fall alert to silently not arrive, and nobody goes looking in
     * settings for a problem they have not been told about.
     */
    val reliabilityIssueCount: Int = 0,
    val versionName: String = "",
    /**
     * Defaults to the build type but is overridable, so a preview can show the
     * developer row without the preview itself depending on how it was built.
     */
    val showDeveloperOptions: Boolean = BuildConfig.DEBUG
)

/**
 * Settings.
 *
 * Reached from the Board's top bar rather than the bottom bar, and kept
 * deliberately short: everything that is part of *using* the product lives on
 * one of the four main destinations, and what is left here is how the app
 * presents itself and whether the phone will let it do its job.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenWay: (String) -> Unit,
    onSelectDarkMode: (DarkModePreference) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(title = "Settings", onBack = onBack)
        }

        item("app-heading") { SectionPlate(title = "This app") }

        item("app") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                // Appearance used to be a whole pushed screen holding three
                // radio rows and a paragraph. A page you open, tap once, and
                // leave forever is not worth a route, a back stack entry and a
                // transition — it is worth a drawer. The three options are the
                // same three; they are simply here now.
                ExpandableSection(
                    label = "Appearance",
                    // The count is the point of a collapsed section: without it
                    // "Appearance" is a word with a chevron beside it and no
                    // hint that there is anything behind it, let alone how
                    // much. Every other collapsed bank in the app states its
                    // number; this one was the exception for no reason.
                    count = DarkModePreference.entries.size
                ) {
                    DarkModePreference.entries.forEach { option ->
                        Hairline()
                        Way(
                            name = option.label,
                            state = if (option == state.darkMode) LampState.LIVE else LampState.OFF,
                            stateLabel = if (option == state.darkMode) "On" else "Off",
                            detail = option.blurb,
                            icon = option.icon,
                            onClick = { onSelectDarkMode(option) }
                        )
                    }
                }
                Hairline()
                Way(
                    name = "Your role",
                    state = LampState.LIVE,
                    stateLabel = state.role.label,
                    icon = Icons.Outlined.Person,
                    onClick = { onOpenWay(Routes.SETTINGS_ROLE) }
                )
            }
        }

        item("reliability-heading") { SectionPlate(title = "Will it reach you") }

        item("reliability") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Alert reliability",
                    // Amber, not red. A missing permission is not an emergency;
                    // it is a thing that will quietly cost you one later.
                    state = if (state.reliabilityIssueCount > 0) {
                        LampState.ATTENTION
                    } else {
                        LampState.LIVE
                    },
                    stateLabel = if (state.reliabilityIssueCount > 0) {
                        "${state.reliabilityIssueCount} to fix"
                    } else {
                        "All set"
                    },
                    detail = if (state.reliabilityIssueCount > 0) {
                        "Some alerts may not reach this phone"
                    } else {
                        "Permissions and battery settings are in order"
                    },
                    icon = SafeShadeIcons.Alert02,
                    onClick = { onOpenWay(Routes.SETTINGS_RELIABILITY) }
                )
            }
        }

        item("about-heading") { SectionPlate(title = "About") }

        item("about") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "About SafeShade",
                    state = LampState.OFF,
                    stateLabel = state.versionName.ifBlank { "Version" },
                    icon = SafeShadeIcons.Info,
                    onClick = { onOpenWay(Routes.SETTINGS_ABOUT) }
                )
                if (state.showDeveloperOptions) {
                    Hairline()
                    Way(
                        name = "Developer",
                        state = LampState.OFF,
                        stateLabel = "Debug",
                        icon = Icons.Outlined.Code,
                        onClick = { onOpenWay(Routes.SETTINGS_DEVELOPER) }
                    )
                }
            }
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Settings · light", showBackground = true)
@Composable
private fun SettingsPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            SettingsScreen(
                state = SettingsUiState(
                    role = UserRole.GUARDIAN,
                    darkMode = DarkModePreference.SYSTEM,
                    reliabilityIssueCount = 2,
                    versionName = "2.0.0",
                    showDeveloperOptions = true
                ),
                onOpenWay = {},
                onSelectDarkMode = {}
            )
        }
    }
}

@Preview(name = "Settings · dark", showBackground = true)
@Composable
private fun SettingsPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            SettingsScreen(
                state = SettingsUiState(
                    role = UserRole.COMPANION,
                    darkMode = DarkModePreference.DARK,
                    reliabilityIssueCount = 0,
                    versionName = "2.0.0",
                    showDeveloperOptions = false
                ),
                onOpenWay = {},
                onSelectDarkMode = {}
            )
        }
    }
}
