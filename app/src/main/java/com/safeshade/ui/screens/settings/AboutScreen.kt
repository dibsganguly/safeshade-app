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

/**
 * A feature that is genuinely not possible on this hardware or this
 * infrastructure.
 *
 * Not a backlog item and not a teaser. Each one names what would have to change
 * before it could exist, so that a reader can tell the difference between "we
 * have not got round to it" and "the sensor is not on the board".
 */
data class BlockedFeature(
    val title: String,
    val description: String,
    /** The specific thing standing in the way. No hedging, no dates. */
    val blocker: String
)

/** Everything the about screen draws. */
data class AboutUiState(
    /**
     * Passed in rather than read from `BuildConfig` here, so a preview shows a
     * real version instead of whatever the preview host happens to be built as.
     */
    val versionName: String = "",
    val versionCode: String = "",
    val buildLabel: String = "",
    val firmwareTarget: String = "",
    val roadmap: List<BlockedFeature> = DefaultRoadmap
)

/**
 * About.
 *
 * Half of this screen is a list of things the product cannot do. That is the
 * unusual half and the deliberate one: a demo audience and a first user both
 * arrive with expectations set by every other wearable on the market, and the
 * fastest way to lose their trust is to let them discover on their own that
 * there is no heart-rate sensor. Saying it first, with the reason, costs
 * nothing and buys the rest of the app the benefit of the doubt.
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

        item("roadmap-heading") { SectionPlate(title = "Not in this version") }

        item("roadmap-intro") {
            Text(
                text = "These are asked about often enough to be worth answering. " +
                    "None of them are close, and each one says why.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }

        state.roadmap.forEach { feature ->
            item("roadmap-${feature.title}") {
                BlockedFeatureCard(feature = feature)
            }
        }

        item("footnote") {
            Text(
                text = "SafeShade keeps everything on your phone and the device. There " +
                    "is no account and no server; the only thing that leaves the phone " +
                    "is a weather lookup for your location.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }
}

/**
 * One blocked feature.
 *
 * No badge, no "coming soon", and no [com.safeshade.ui.board.StubMark] — that
 * marker means "this data is representative rather than live", which is a
 * different claim entirely and would be the wrong one here. The whole treatment
 * is a plate, a name, a sentence, and the blocker stated flatly under a rule.
 */
@Composable
private fun BlockedFeatureCard(
    feature: BlockedFeature,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Nameplate(feature.title)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = feature.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink
            )
        }
        Hairline()
        // Neither side of this row had a weight, and `feature.blocker` is a
        // two-to-three sentence explanation — with no constraint on its
        // width, Compose gave it a single unbroken line and let it run off
        // the right edge of the screen instead of wrapping. The label is
        // short and fixed ("Blocked by"), so it keeps its natural width
        // unweighted; the explanation gets the weight so it wraps into the
        // space that's left.
        Row(modifier = Modifier.padding(Spacing.lg)) {
            Nameplate("Blocked by", small = true, muted = true)
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = feature.blocker,
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The roadmap, as facts.
 *
 * Kept as data rather than as markup so the same list can be rendered in a
 * press kit, a README or a demo script without being retyped and quietly
 * softened on the way.
 */
val DefaultRoadmap: List<BlockedFeature> = listOf(
    BlockedFeature(
        title = "Heart rate, blood oxygen and body temperature",
        description = "Continuous vitals alongside fall detection, so a guardian can " +
            "tell a faint from a trip.",
        blocker = "There is no PPG sensor on this board. The temperature reading the " +
            "device already sends is the ambient sensor, not a body reading. Adding " +
            "vitals means a hardware revision, not a software update."
    ),
    BlockedFeature(
        title = "Matter smart home",
        description = "The wearable announcing a fall to lights, speakers and hubs " +
            "already in the house.",
        blocker = "Matter needs a certified device with an allocated vendor ID and a " +
            "commissioning flow. SafeShade has none of the three, and certification " +
            "is not open to a one-off build."
    ),
    BlockedFeature(
        title = "Cloud tier – heatmap, family dashboard, mesh relay, over-the-air updates",
        description = "A shared view for a whole family, alerts relayed between nearby " +
            "SafeShade devices, and firmware updates pushed without a cable.",
        blocker = "There is no SafeShade server, and every one of these needs one, plus " +
            "accounts, storage and the running cost behind them. The app is built to " +
            "work with no backend at all, which is why it currently does."
    ),
    BlockedFeature(
        title = "SafeShade Spark",
        description = "A second, smaller device – a clip or a pendant – paired alongside " +
            "the main one.",
        blocker = "Spark is hardware that does not exist yet. The app can already hold " +
            "more than one paired device, which is as far as this half can go on its own."
    )
)

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
