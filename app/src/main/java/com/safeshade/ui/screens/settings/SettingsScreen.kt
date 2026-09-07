package com.safeshade.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
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
        DarkModePreference.SYSTEM -> SafeShadeIcons.SunMoon
        DarkModePreference.LIGHT -> SafeShadeIcons.Sun
        DarkModePreference.DARK -> SafeShadeIcons.Moon
    }

/** How a dark-mode preference reads on a nameplate. */
internal val DarkModePreference.label: String
    get() = when (this) {
        DarkModePreference.SYSTEM -> "System"
        DarkModePreference.LIGHT -> "Light"
        DarkModePreference.DARK -> "Dark"
    }

// The Settings hub itself is gone: the Profile page (ui/screens/profile) took
// its content, and the `settings` route now renders that. The helpers above
// stay here because the Profile and Role screens both read them.
