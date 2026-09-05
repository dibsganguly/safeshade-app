package com.safeshade.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness4
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.DarkModePreference
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the appearance screen draws. */
data class AppearanceUiState(
    val selected: DarkModePreference = DarkModePreference.SYSTEM
)

/**
 * Light or dark.
 *
 * Three options and nothing else. There is no accent picker and no dynamic
 * colour, and that is a product decision rather than an omission: this app uses
 * colour for exactly one thing, which is telling you whether a circuit is live.
 * A palette drawn from the user's wallpaper would repaint the pilot lamps and
 * take that meaning away.
 */
@Composable
fun AppearanceScreen(
    state: AppearanceUiState,
    onSelect: (DarkModePreference) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

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
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.displaySmall,
                color = colors.ink
            )
        }

        item("heading") { SectionPlate(title = "Theme") }

        item("options") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                DarkModePreference.entries.forEachIndexed { index, option ->
                    if (index > 0) Hairline()
                    val selected = state.selected == option
                    Way(
                        name = option.label,
                        state = if (selected) LampState.LIVE else LampState.OFF,
                        stateLabel = if (selected) "In use" else "Off",
                        detail = describe(option),
                        icon = iconFor(option),
                        onClick = { onSelect(option) }
                    )
                }
            }
        }

        item("note") {
            BoardPlate(modifier = Modifier.fillMaxWidth(), recessed = true) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Nameplate("Why there is no colour option", small = true, muted = true)
                    Text(
                        text = "Teal, amber and red are reserved for saying what a " +
                            "circuit is doing. Letting the theme change them would " +
                            "make a lit lamp mean nothing in particular, so the " +
                            "palette stays fixed in both themes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
            }
        }
    }
}

private fun describe(option: DarkModePreference): String = when (option) {
    DarkModePreference.SYSTEM -> "Follows the phone, including its night schedule"
    DarkModePreference.LIGHT -> "Always the bone panel, whatever the phone is set to"
    DarkModePreference.DARK -> "Always the night panel — easier to read in a dark hallway"
}

private fun iconFor(option: DarkModePreference): ImageVector = when (option) {
    DarkModePreference.SYSTEM -> Icons.Outlined.Brightness4
    DarkModePreference.LIGHT -> Icons.Outlined.LightMode
    DarkModePreference.DARK -> Icons.Outlined.DarkMode
}

// ============================================================================
// Previews
// ============================================================================

@Preview(name = "Appearance · light", showBackground = true)
@Composable
private fun AppearancePreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AppearanceScreen(
                state = AppearanceUiState(DarkModePreference.SYSTEM),
                onSelect = {}
            )
        }
    }
}

@Preview(name = "Appearance · dark", showBackground = true)
@Composable
private fun AppearancePreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AppearanceScreen(
                state = AppearanceUiState(DarkModePreference.DARK),
                onSelect = {}
            )
        }
    }
}
