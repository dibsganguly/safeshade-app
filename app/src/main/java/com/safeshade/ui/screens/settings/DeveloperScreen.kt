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
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.BuildConfig
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * One scripted state of the world, as this screen needs to render it.
 *
 * A flat presentation type rather than the `Scenario` enum itself, because that
 * enum lives in the debug source set along with the fake link, its scripted
 * alerts and its fabricated telemetry. This screen is in the main source set,
 * so it cannot see it — and that separation is the point: leak-proofing the
 * fake by source set rather than by a `BuildConfig.DEBUG` branch means the
 * fabricated fall alerts are not compiled into a release APK at all, where one
 * mis-evaluated condition would be an alert the user never had.
 */
data class SimulatorScenario(
    /**
     * The contract with the debug source set: this is `Scenario.name`.
     *
     * [onSelectScenario] hands it straight back, so the caller resolves it with
     * `Scenario.valueOf(key)`. Anything else — an ordinal, a label — breaks
     * silently the first time the enum is reordered or reworded.
     */
    val key: String,
    val label: String,
    val blurb: String
)

/** Everything the developer screen draws. */
data class DeveloperUiState(
    val scenarios: List<SimulatorScenario> = emptyList(),
    val activeScenarioKey: String? = null,
    /** True while the fake link is driving the app instead of the radio. */
    val usingFakeLink: Boolean = false,
    val buildFingerprint: String = ""
)

/**
 * Developer tools.
 *
 * Debug builds only, and guarded here as well as in the navigation graph: a
 * route can be reached by deep link, and a screen that offers to replace the
 * real Bluetooth link with a scripted one should not be reachable in a shipped
 * build by any route at all.
 */
@Composable
fun DeveloperScreen(
    state: DeveloperUiState,
    onSelectScenario: (String) -> Unit,
    onUseRealLink: () -> Unit,
    onOpenKitGallery: () -> Unit,
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
            ScreenHeader(title = "Developer", onBack = onBack)
        }

        if (!BuildConfig.DEBUG) {
            // Belt and braces. If this ever renders in a release build, it says
            // so and offers nothing, rather than half-working.
            item("release") {
                EmptyBay(message = "Developer tools are only available in debug builds.", withShady = false)
            }
            return@LazyColumn
        }

        // ---------- Simulator ----------
        item("sim-heading") { SectionPlate(title = "Simulator") }

        item("sim-intro") {
            Text(
                text = "Every Bluetooth-dependent screen is unreachable on an emulator: " +
                    "with no radio the link never leaves Disconnected, so the connected, " +
                    "tripped, ringing and live-telemetry layouts are exactly the ones " +
                    "nobody ever looks at. These scenarios drive a fake link through a " +
                    "fixed timeline instead, so a screenshot of one is reproducible.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted
            )
        }

        if (state.scenarios.isEmpty()) {
            item("sim-empty") {
                EmptyBay(
                    message = "No scenarios were supplied. The debug source set owns " +
                        "the list; this screen only renders it.",
                    // The debug source set forgot to wire up any scenarios —
                    // squarely the developer's own doing.
                    shadyMood = ShadyMood.DUMBFOUNDED
                )
            }
        } else {
            item("sim-list") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    state.scenarios.forEachIndexed { index, scenario ->
                        if (index > 0) Hairline()
                        val active = state.usingFakeLink &&
                            scenario.key == state.activeScenarioKey
                        Way(
                            name = scenario.label,
                            state = if (active) LampState.LIVE else LampState.OFF,
                            stateLabel = if (active) "Running" else "Off",
                            detail = scenario.blurb,
                            icon = Icons.Outlined.Science,
                            onClick = { onSelectScenario(scenario.key) }
                        )
                    }
                }
            }

            if (state.usingFakeLink) {
                item("sim-stop") {
                    BoardButton(
                        label = "Use the real radio",
                        supporting = "Stops the fake link and reconnects over Bluetooth",
                        onClick = onUseRealLink,
                        weight = ButtonWeight.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ---------- Kit ----------
        item("kit-heading") { SectionPlate(title = "Component kit") }

        item("kit") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Kit gallery",
                    state = LampState.OFF,
                    stateLabel = "Open",
                    detail = "Every component in the board kit on one screen, for " +
                        "checking a change in both themes at a raised font scale " +
                        "before six screens are built on it.",
                    icon = Icons.Outlined.Widgets,
                    onClick = onOpenKitGallery
                )
            }
        }

        item("kit-note") {
            Text(
                text = "The gallery is com.safeshade.ui.board.KitGallery. It is not in " +
                    "the navigation graph — this row is the only way in — and it is the " +
                    "reference to check before inventing a seventh kind of card.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }

        if (state.buildFingerprint.isNotBlank()) {
            item("build") {
                BoardPlate(modifier = Modifier.fillMaxWidth(), recessed = true) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Nameplate("Build", small = true, muted = true)
                        Text(
                            text = state.buildFingerprint,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// Previews
// ============================================================================

private val previewScenarios = listOf(
    SimulatorScenario(
        "DISCONNECTED",
        "Disconnected",
        "No device. The empty state every screen must handle."
    ),
    SimulatorScenario(
        "CONNECTED_IDLE",
        "Connected",
        "Link up and Ready, no telemetry yet. The gap between Connected and Ready is scripted."
    ),
    SimulatorScenario(
        "LIVE_TELEMETRY",
        "Live telemetry",
        "~1 Hz accelerometer walk and a slowly draining battery, so sparklines have real shape."
    ),
    SimulatorScenario(
        "FALL_ALERT",
        "Fall detected",
        "A fall two seconds after Ready, then a second one, to prove alerts are events."
    ),
    SimulatorScenario(
        "RINGING",
        "Ringing",
        "ringDevice() called. Nothing is ever acknowledged, matching the firmware."
    )
)

@Preview(name = "Developer · light", showBackground = true)
@Composable
private fun DeveloperPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeveloperScreen(
                state = DeveloperUiState(
                    scenarios = previewScenarios,
                    activeScenarioKey = "LIVE_TELEMETRY",
                    usingFakeLink = true,
                    buildFingerprint = "2.0.0 (2) · debug"
                ),
                onSelectScenario = {},
                onUseRealLink = {},
                onOpenKitGallery = {}
            )
        }
    }
}

@Preview(name = "Developer · dark", showBackground = true)
@Composable
private fun DeveloperPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            DeveloperScreen(
                state = DeveloperUiState(scenarios = previewScenarios),
                onSelectScenario = {},
                onUseRealLink = {},
                onOpenKitGallery = {}
            )
        }
    }
}
