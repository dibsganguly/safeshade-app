package com.safeshade.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.R
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the about screen draws. */
data class AboutUiState(
    /**
     * Passed in rather than read from `BuildConfig` here, so a preview shows a
     * real version instead of whatever the preview host happens to be built as.
     */
    val versionName: String = "",
    val versionCode: String = "",
    val buildLabel: String = "",
    val firmwareTarget: String = ""
)

/**
 * About.
 *
 * The emblem, the version, the firmware this build talks to, and where alerts
 * travel. It used to carry a "not in this version" roadmap; that ledger now
 * lives in `DeviceCapabilities.awaitingFirmware` and the handoff, because the
 * app reads as it will at launch.
 */
@Composable
fun AboutScreen(
    state: AboutUiState,
    onBack: (() -> Unit)? = null,
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
            ScreenHeader(title = "About", onBack = onBack)
        }

        item("logo") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // The emblem is a full-colour raster, so it is an Image and
                    // not an Icon — an Icon would flatten it to a single tint.
                    // The full lock-up lives in docs/ and is not a resource, so
                    // the wordmark here is set in type rather than drawn.
                    Image(
                        painter = painterResource(R.drawable.splash_emblem),
                        contentDescription = "SafeShade emblem",
                        // Height, not size. Since the drawable was cropped to
                        // its content it is a 0.65-aspect mark, so a square box
                        // reserved 72dp of width for a 47dp-wide image. Same
                        // correction the masthead, the intro and onboarding
                        // carry.
                        modifier = Modifier.height(72.dp)
                    )
                    Spacer(Modifier.height(Spacing.md))
                    Text(
                        text = "SafeShade",
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.ink
                    )
                    Text(
                        text = "A wearable that notices when someone falls, and a " +
                            "phone that does something about it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }
                Hairline()
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
                ) {
                    Readout(label = "Version", value = state.versionName.ifBlank { "--" })
                    if (state.versionCode.isNotBlank()) {
                        Readout(label = "Build", value = state.versionCode)
                    }
                    if (state.buildLabel.isNotBlank()) {
                        Readout(label = "Type", value = state.buildLabel)
                    }
                }
                if (state.firmwareTarget.isNotBlank()) {
                    Hairline()
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Nameplate("Firmware this build talks to", small = true, muted = true)
                        Text(
                            text = state.firmwareTarget,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }

        item("footnote") {
            Text(
                text = "Fall alerts and SOS travel straight between the wearable and this " +
                    "phone and never wait on a server. Signing in adds your Circle, " +
                    "history and the family dashboard on top; the Privacy section of " +
                    "your Profile lists exactly what leaves the phone and when.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

private val previewAbout = AboutUiState(
    versionName = "2.0.0",
    versionCode = "2",
    buildLabel = "debug",
    firmwareTarget = "SafeShade v2.1 (ESP32-C3, no-OTA partitions)"
)

@Preview(name = "About · light", showBackground = true)
@Composable
private fun AboutPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AboutScreen(state = previewAbout)
        }
    }
}

@Preview(name = "About · dark", showBackground = true)
@Composable
private fun AboutPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AboutScreen(state = previewAbout)
        }
    }
}

@Preview(name = "About · large text", showBackground = true, fontScale = 1.3f)
@Composable
private fun AboutPreviewLargeText() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            AboutScreen(state = previewAbout)
        }
    }
}
