package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Chain
import com.safeshade.ui.board.ChainStop
import com.safeshade.ui.board.ChipRow
import com.safeshade.ui.board.EditorFootBar
import com.safeshade.ui.board.EditorScaffold
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.HoldToConfirm
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Ledger
import com.safeshade.ui.board.LedgerRow
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.board.PlateField
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** One automation as the list prints it. */
data class SmartHookRow(
    val id: String,
    val name: String,
    val triggerLabel: String,
    val providerLabel: String,
    val enabled: Boolean,
    /** "Fired 2 min ago · 200", "Failed 09:12: connection refused", or null when never fired. */
    val lastLine: String?,
    val lastFailed: Boolean
)

/** A platform's row: the real outcome of the last Connect tap, or nothing yet. */
data class PlatformRow(
    val name: String,
    val state: LampState,
    val word: String,
    val line: String
)

/** Everything the smart-home page draws. */
data class SmartHomeUiState(
    val wearerName: String = "",
    val hooks: List<SmartHookRow> = emptyList(),
    val googleHome: PlatformRow = PlatformRow("Google Home", LampState.OFF, "—", "Opens the Google Home app if it is on this phone."),
    val alexa: PlatformRow = PlatformRow("Amazon Alexa", LampState.OFF, "—", "Opens the Alexa app if it is on this phone."),
    val matter: PlatformRow = PlatformRow("Matter", LampState.OFF, "—", "Whether this phone can commission a Matter device."),
    /** The last firing lines across all hooks, newest first. */
    val recent: List<String> = emptyList()
)

/**
 * Smart home.
 *
 * Automations are rows: a name, what fires it, where it goes, and the last
 * thing that actually happened when it fired, status code included. The
 * three platform rows below report the real outcome of their one action, in
 * the past tense, and nothing until it has been tapped. A webhook is the
 * thing the phone can genuinely do today, so it comes first and is the thing
 * the Add button makes.
 */
@Composable
fun SmartHomeScreen(
    state: SmartHomeUiState,
    onAdd: () -> Unit,
    onOpen: (id: String) -> Unit,
    onToggle: (id: String, enabled: Boolean) -> Unit,
    onGoogleHome: () -> Unit,
    onAlexa: () -> Unit,
    onMatter: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
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
        ScreenHeader(title = "Smart home", subtitle = "What the house does when something happens", onBack = onBack)

        // What an automation does, as a chain (2.27) rather than a paragraph:
        // one of these four things happens, SafeShade posts a signed
        // request, and the automation does the rest.
        Chain(
            listOf(
                ChainStop(SafeShadeIcons.SmartHome, "A fall, SOS,", "zone change or low battery"),
                ChainStop(SafeShadeIcons.SendMessage, "SafeShade posts", "a signed request"),
                ChainStop(SafeShadeIcons.CpuChip, "Your automation", "does the rest")
            )
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Automations")
        Spacer(Modifier.height(Spacing.sm))
        if (state.hooks.isEmpty()) {
            Footnote("None yet. Add one below and point a routine at it.")
        } else {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.hooks.forEachIndexed { i, h ->
                    if (i > 0) Hairline()
                    Way(
                        name = h.name,
                        state = when {
                            !h.enabled -> LampState.OFF
                            h.lastFailed -> LampState.TRIP
                            else -> LampState.LIVE
                        },
                        stateLabel = when {
                            !h.enabled -> "Off"
                            h.lastFailed -> "Failed"
                            h.lastLine != null -> "Fired"
                            else -> "Armed"
                        },
                        detail = listOfNotNull("${h.triggerLabel} → ${h.providerLabel}", h.lastLine).joinToString(" · "),
                        icon = SafeShadeIcons.SmartHome,
                        checked = h.enabled,
                        onCheckedChange = { onToggle(h.id, it) },
                        onClick = { onOpen(h.id) }
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.md))
        BoardButton(
            label = "Add an Automation",
            icon = SafeShadeIcons.SmartHome,
            onClick = onAdd,
            weight = if (state.hooks.isEmpty()) ButtonWeight.ATTENTION else ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Platforms")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(name = state.googleHome.name, state = state.googleHome.state, stateLabel = state.googleHome.word, detail = state.googleHome.line, icon = SafeShadeIcons.SmartHome, onClick = onGoogleHome)
            Hairline()
            Way(name = state.alexa.name, state = state.alexa.state, stateLabel = state.alexa.word, detail = state.alexa.line, icon = SafeShadeIcons.SmartHome, onClick = onAlexa)
            Hairline()
            Way(name = state.matter.name, state = state.matter.state, stateLabel = state.matter.word, detail = state.matter.line, icon = SafeShadeIcons.CpuChip, onClick = onMatter)
        }
        Spacer(Modifier.height(Spacing.sm))
        Footnote("Google Home and Alexa take a webhook through their own routines: make an automation here, then point a routine at it from their app.")

        if (state.recent.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "Recent")
            Spacer(Modifier.height(Spacing.sm))
            // The hooks' last firing facts, as a ledger (2.22) rather than a
            // list of loose sentences.
            Ledger(state.recent.take(8).mapIndexed { i, line -> LedgerRow(key = "${i + 1}", value = line) })
        }
    }
}

/** Everything the automation editor draws and edits. */
data class SmartHookEditorUiState(
    val id: String? = null,
    val name: String = "",
    val trigger: String = "fall",
    val provider: String = "webhook",
    val endpointUrl: String = "",
    val secret: String = "",
    val urlError: String? = null,
    val saving: Boolean = false,
    val saveError: String? = null,
    /** The last test post's outcome sentence, or null. */
    val testLine: String? = null,
    val testing: Boolean = false
)

val SmartHookTriggerLabels: Map<String, String> = linkedMapOf(
    "fall" to "A fall",
    "sos" to "An SOS",
    "zone_exit" to "Leaving a zone",
    "zone_enter" to "Entering a zone",
    "low_battery" to "Low battery",
    "check_in_missed" to "Missed check-in"
)

val SmartHookProviderLabels: Map<String, String> = linkedMapOf(
    "webhook" to "Webhook",
    "home_assistant" to "Home Assistant",
    "ifttt" to "IFTTT"
)

/**
 * One automation.
 *
 * Trigger and destination are chip rows because each set is small, fixed and
 * categorical. The address is checked as typed, with the reason; the secret
 * is optional and never shown once saved. "Send a Test" posts the real
 * request and prints the real answer. One COMMIT button saves.
 */
@Composable
fun SmartHookEditorScreen(
    state: SmartHookEditorUiState,
    onName: (String) -> Unit,
    onTrigger: (String) -> Unit,
    onProvider: (String) -> Unit,
    onUrl: (String) -> Unit,
    onSecret: (String) -> Unit,
    onTest: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val canSave = !state.saving && state.name.isNotBlank() && state.endpointUrl.isNotBlank() && state.urlError == null

    // The editor's foot is pinned (2.34): Save is amber, Discard leaves
    // without writing anything.
    EditorScaffold(
        modifier = modifier.fillMaxSize().background(colors.ground),
        bottomPadding = contentPadding.calculateBottomPadding(),
        foot = {
            EditorFootBar(
                primaryLabel = if (state.saving) "Saving" else "Save",
                onPrimary = onSave,
                secondaryLabel = "Discard",
                onSecondary = onBack ?: {},
                statusLine = state.saveError,
                statusState = if (state.saveError != null) LampState.TRIP else null,
                primaryEnabled = canSave,
                secondaryEnabled = !state.saving && onBack != null
            )
        }
    ) { footPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = footPadding.calculateBottomPadding() + Spacing.lg
            )
    ) {
        ScreenHeader(title = if (state.id == null) "New automation" else "Automation", onBack = onBack)

        PlateField(label = "Name", value = state.name, onValueChange = onName, placeholder = "Hall lights on", maxLength = 40)

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "When")
        Spacer(Modifier.height(Spacing.sm))
        ChipRow(
            options = SmartHookTriggerLabels.values.toList(),
            selected = SmartHookTriggerLabels[state.trigger] ?: "A fall",
            onSelect = { label -> SmartHookTriggerLabels.entries.firstOrNull { it.value == label }?.let { onTrigger(it.key) } }
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Where")
        Spacer(Modifier.height(Spacing.sm))
        ChipRow(
            options = SmartHookProviderLabels.values.toList(),
            selected = SmartHookProviderLabels[state.provider] ?: "Webhook",
            onSelect = { label -> SmartHookProviderLabels.entries.firstOrNull { it.value == label }?.let { onProvider(it.key) } }
        )
        Spacer(Modifier.height(Spacing.md))
        PlateField(
            label = "Address",
            value = state.endpointUrl,
            onValueChange = onUrl,
            placeholder = if (state.provider == "ifttt") "https://maker.ifttt.com/trigger/…/with/key/…" else "https://…",
            helper = when (state.provider) {
                "home_assistant" -> "Your Home Assistant's webhook URL. http:// is accepted only for an address on your own network."
                "ifttt" -> "The Webhooks service URL from IFTTT. value1 is the wearer, value2 the trigger."
                else -> "SafeShade posts JSON here with a signature header when the trigger happens."
            },
            error = state.urlError,
            maxLength = 300,
            keyboardType = KeyboardType.Uri
        )
        Spacer(Modifier.height(Spacing.md))
        PlateField(
            label = "Secret",
            value = state.secret,
            onValueChange = onSecret,
            placeholder = "Optional",
            helper = "Signs each post (HMAC-SHA256 over the timestamp and body) so the receiver can check it came from SafeShade.",
            maxLength = 120
        )

        Spacer(Modifier.height(Spacing.xl))
        BoardButton(
            label = if (state.testing) "Sending…" else "Send a Test",
            supporting = state.testLine ?: "Posts a test event to the address and shows the reply.",
            onClick = onTest,
            enabled = !state.testing && state.endpointUrl.isNotBlank() && state.urlError == null,
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
        if (onDelete != null) {
            Spacer(Modifier.height(Spacing.xl))
            // Deleting an automation is irreversible, so it sits behind a
            // hold rather than a plain button (2.67).
            HoldToConfirm(label = "Hold to Delete", onConfirm = onDelete, icon = SafeShadeIcons.DeleteBin, modifier = Modifier.fillMaxWidth())
        }
    }
    }
}


@Preview(name = "Smart home", showBackground = true, heightDp = 1200)
@Composable
private fun SmartHomePreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            SmartHomeScreen(
                state = SmartHomeUiState(
                    hooks = listOf(SmartHookRow("1", "Hall lights on", "A fall", "Home Assistant", true, "Fired 09:12 · 200", false)),
                    googleHome = PlatformRow("Google Home", LampState.OFF, "Not installed", "The Google Home app is not on this phone."),
                    recent = listOf(
                        "Hall lights on · fired 09:12 · 200",
                        "Hall lights on · fired yesterday 21:40 · 200"
                    )
                ),
                onAdd = {}, onOpen = {}, onToggle = { _, _ -> }, onGoogleHome = {}, onAlexa = {}, onMatter = {}, onBack = {}
            )
        }
    }
}
