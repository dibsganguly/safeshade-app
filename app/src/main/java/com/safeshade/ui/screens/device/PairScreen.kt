package com.safeshade.ui.screens.device

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.DeviceModel
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.ProductSilhouette
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Step
import com.safeshade.ui.board.Steps
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the pairing page draws. */
data class PairUiState(
    val chosen: DeviceModel = DeviceModel.S1,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val permissionsGranted: Boolean = true,
    /** The advertised name of the wearable found or connected, when there is one. */
    val foundName: String? = null,
    /** The model the found name resolves to, when it disagrees with [chosen]. */
    val foundModel: DeviceModel? = null,
    /** Which features of the chosen model this build can drive; the rest are listed by name. */
    val nfcAvailable: Boolean = false
)

/**
 * Pairing by product.
 *
 * Three silhouettes across the top, one ringed; picking one changes the
 * facts below it and the name the search looks for. The search itself is the
 * app's ordinary connect: every SafeShade advertises the same service, so the
 * product choice narrows what the page says, not what the radio does, and a
 * wearable that turns out to be a different model is reported as such rather
 * than silently accepted. The facts under each product are the ones the
 * choice changes: what the device carries, what the phone can do with it.
 */
@Composable
fun PairScreen(
    state: PairUiState,
    onChoose: (DeviceModel) -> Unit,
    onSearch: () -> Unit,
    onStop: () -> Unit,
    onRequestPermissions: () -> Unit,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val busy = state.connection.isBusy
    val ready = state.connection.isUsable

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            )
    ) {
        ScreenHeader(
            title = "Pair a wearable",
            subtitle = "Which SafeShade is in your hand?",
            onBack = onBack
        )

        Spacer(Modifier.height(Spacing.lg))
        // Pairing is a procedure (2.96): which product, then the search, then
        // the link. Done and current follow the connection itself, not a
        // guess about how far a person has got.
        Steps(pairingSteps(state.connection))
        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            DeviceModel.entries.forEach { model ->
                val selected = model == state.chosen
                BoardPlate(
                    modifier = Modifier
                        .weight(1f)
                        .selectable(selected = selected, role = Role.RadioButton, onClick = { onChoose(model) }),
                    recessed = !selected
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.md),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProductSilhouette(model = model, size = 84.dp, accent = colors.accentFor(model.name))
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = model.label.removePrefix("SafeShade "),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) colors.ink else colors.inkMuted
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        BusTick(state = if (selected) LampState.LIVE else LampState.OFF)
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = state.chosen.label)
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            productFacts(state.chosen, state.nfcAvailable).forEachIndexed { i, (name, word, line, lamp) ->
                if (i > 0) Hairline()
                Way(name = name, state = lamp, stateLabel = word, detail = line)
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Search")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            val (lamp, word, line) = searchWay(state)
            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(word, style = MaterialTheme.typography.titleMedium, color = colors.ink)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
                Spacer(Modifier.padding(Spacing.sm))
                PilotLamp(state = lamp, size = 18.dp)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        when {
            !state.permissionsGranted -> BoardButton(
                label = "Allow Bluetooth",
                supporting = "Searching needs Bluetooth and, on older Android, location.",
                icon = SafeShadeIcons.Bluetooth,
                onClick = onRequestPermissions,
                weight = ButtonWeight.ATTENTION,
                modifier = Modifier.fillMaxWidth()
            )
            ready -> BoardButton(
                label = "Done",
                icon = SafeShadeIcons.Bluetooth,
                onClick = onDone,
                weight = ButtonWeight.COMMIT,
                modifier = Modifier.fillMaxWidth()
            )
            busy -> BoardButton(
                label = "Stop Searching",
                onClick = onStop,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
            else -> BoardButton(
                label = "Search for the ${state.chosen.label.removePrefix("SafeShade ")}",
                supporting = "Hold the wearable near the phone and switch it on.",
                icon = SafeShadeIcons.Bluetooth,
                onClick = onSearch,
                weight = ButtonWeight.ATTENTION,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The three-step procedure this page walks, done and current read off the link itself. */
private fun pairingSteps(connection: ConnectionState): List<Step> {
    val linked = connection.isUsable
    val searching = !linked
    return listOf(
        Step(lead = "Choose the product", line = "Pick which SafeShade this is.", done = true),
        Step(lead = "Search and connect", line = "Hold it near the phone and switch it on.", done = linked, current = searching),
        Step(lead = "Ready to use", line = "The wearable is on the link.", done = false, current = linked)
    )
}

private data class Fact(val name: String, val word: String, val line: String, val lamp: LampState)

private fun productFacts(model: DeviceModel, nfc: Boolean): List<Fact> = when (model) {
    DeviceModel.S1 -> listOf(
        Fact("Fall detection", "On board", "Accelerometer and gyroscope; the phone sets the sensitivity.", LampState.LIVE),
        Fact("Screen and lights", "On board", "A small screen, three lights and a knob. The phone sets the light pattern.", LampState.LIVE),
        Fact("Calls and texts", "Through the phone", "The S1 has no SIM. Alerts reach people through this phone.", LampState.OFF),
        Fact("Sound", "Chime only", "No microphone, no speaker for voice.", LampState.OFF)
    )
    DeviceModel.FIVE_G -> listOf(
        Fact("Fall detection", "On board", "Accelerometer and gyroscope; the phone sets the sensitivity.", LampState.LIVE),
        Fact("Calls and texts", "Own SIM", "Sends its own SMS and takes a call without this phone nearby.", LampState.LIVE),
        Fact("Position", "Own GPS", "Reports where it is over the mobile network when the phone is out of reach.", LampState.LIVE),
        Fact("Sound", "Microphone and speaker", "Push-to-talk and a call to the wearable.", LampState.LIVE)
    )
    DeviceModel.SPARK -> listOf(
        Fact("Fall detection", "On board", "Accelerometer only; sensitivity set by the phone.", LampState.LIVE),
        Fact("Distress recording", "On board", "Holds the button and the phone's microphone records, if you arm it under Evidence.", LampState.LIVE),
        Fact("Tag", if (nfc) "Writable" else "—",
            if (nfc) "This phone can write the emergency card to the Spark's tag." else "This phone has no NFC, so it cannot write the Spark's tag.",
            if (nfc) LampState.LIVE else LampState.UNKNOWN),
        Fact("Anti-removal", "—", "The Spark alarms when its strap opens. Nothing on the phone changes this.", LampState.UNKNOWN)
    )
}

private fun searchWay(s: PairUiState): Triple<LampState, String, String> = when (val c = s.connection) {
    ConnectionState.Disconnected -> Triple(LampState.OFF, "Not searching", "Nothing found yet.")
    ConnectionState.Scanning -> Triple(LampState.ATTENTION, "Searching", "Listening for a SafeShade advertising nearby.")
    is ConnectionState.Found -> Triple(LampState.ATTENTION, "Found ${c.name}", foundLine(s))
    ConnectionState.Connecting -> Triple(LampState.ATTENTION, "Connecting", "Opening the link${s.foundName?.let { " to $it" } ?: ""}.")
    ConnectionState.Connected -> Triple(LampState.ATTENTION, "Connected", "Reading what the wearable offers.")
    ConnectionState.Ready -> Triple(LampState.LIVE, "Paired", foundLine(s))
    ConnectionState.BluetoothUnavailable -> Triple(LampState.TRIP, "Bluetooth is off", "Turn Bluetooth on and search again.")
    is ConnectionState.ScanFailed -> Triple(LampState.TRIP, "Search failed", "Android refused the scan (code ${c.code}). Wait half a minute and try again.")
}

private fun foundLine(s: PairUiState): String {
    val found = s.foundModel
    return when {
        found != null && found != s.chosen -> "${s.foundName ?: "This wearable"} is a ${found.label}, not the ${s.chosen.label} you chose. Pairing continues; the page now describes what was found."
        s.foundName != null -> "${s.foundName} answers as a ${s.chosen.label}."
        else -> "A ${s.chosen.label} answered."
    }
}

@Preview(name = "Pair", showBackground = true, heightDp = 1300)
@Composable
private fun PairPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            PairScreen(
                state = PairUiState(chosen = DeviceModel.SPARK),
                onChoose = {}, onSearch = {}, onStop = {}, onRequestPermissions = {}, onDone = {}, onBack = {}
            )
        }
    }
}
