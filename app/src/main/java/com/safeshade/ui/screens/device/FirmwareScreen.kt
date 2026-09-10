package com.safeshade.ui.screens.device

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.DeviceModel
import com.safeshade.device.OtaProtocol.OtaStep
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.LedgerLine
import com.safeshade.ui.board.LedgerRow
import com.safeshade.ui.board.ProductSilhouette
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** One published release, as the row prints it. */
data class FirmwareReleaseRow(
    val id: String,
    val version: String,
    val sizeLabel: String,
    val publishedLabel: String?,
    val notes: String?,
    val mandatory: Boolean,
    /** Whether the verified image is already on this phone. */
    val downloaded: Boolean
)

/** Everything the firmware page draws. */
data class FirmwareUiState(
    val model: DeviceModel = DeviceModel.S1,
    val deviceName: String = "",
    val connected: Boolean = false,
    /** What the wearable reported on the link, or null when it never has. */
    val installedVersion: String? = null,
    /** The last version query's outcome when it did not produce a version. */
    val versionNote: String? = null,
    val lastCheckedLabel: String? = null,
    val checking: Boolean = false,
    val checkError: String? = null,
    val releases: List<FirmwareReleaseRow> = emptyList(),
    /** The release the newest row would install, or null when nothing newer is published. */
    val newestId: String? = null,
    val step: OtaStep = OtaStep.Idle,
    val signedIn: Boolean = false
)

/**
 * Firmware.
 *
 * What the wearable runs, what is published for its model, and the update as
 * a sequence of steps that each report what actually happened. The install
 * row's state word is never a tick on the send: it is what the device
 * answered when asked its version afterwards, and a device that acknowledges
 * every chunk and then reports no version ends in Failed with that sentence.
 */
@Composable
fun FirmwareScreen(
    state: FirmwareUiState,
    onCheck: () -> Unit,
    onAskVersion: () -> Unit,
    onDownload: (releaseId: String) -> Unit,
    onInstall: (releaseId: String) -> Unit,
    onCancel: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val busy = state.step is OtaStep.Downloading || state.step is OtaStep.Verifying ||
        state.step is OtaStep.Sending || state.step is OtaStep.AwaitingDeviceVersion

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
        ScreenHeader(title = "Firmware", subtitle = "What the wearable runs", onBack = onBack)

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(Spacing.lg),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                ProductSilhouette(model = state.model, size = 72.dp)
                Spacer(Modifier.padding(Spacing.sm))
                // The version facts read off the wearable, as a ledger
                // rather than one big readout with a paragraph under it.
                Column(Modifier.weight(1f)) {
                    LedgerLine(LedgerRow("Model", state.model.label))
                    LedgerLine(
                        LedgerRow(
                            key = "Installed version",
                            value = state.installedVersion ?: "—",
                            mono = true,
                            state = if (state.installedVersion != null) LampState.LIVE else null
                        )
                    )
                }
            }
            Hairline()
            Way(
                name = "Ask the wearable",
                state = if (state.connected) LampState.LIVE else LampState.UNKNOWN,
                stateLabel = if (state.connected) "Ready" else "—",
                detail = if (state.connected) "Sends the version query and shows the reply, whatever it is." else "Needs the link.",
                icon = SafeShadeIcons.CpuChip,
                onClick = if (state.connected && !busy) onAskVersion else null
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        Footnote(
            when {
                state.installedVersion != null -> "Reported by the wearable over the link."
                state.versionNote != null -> state.versionNote
                state.connected -> "Not asked yet."
                else -> "Off the link. The version is read from the wearable itself."
            }
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Published for the ${state.model.label.removePrefix("SafeShade ")}")
        Spacer(Modifier.height(Spacing.sm))
        if (state.releases.isEmpty()) {
            Note(
                when {
                    state.checking -> "Asking SafeShade Cloud…"
                    state.checkError != null -> state.checkError
                    state.lastCheckedLabel != null -> "Nothing published. Checked ${state.lastCheckedLabel}."
                    else -> "Not checked yet."
                }
            )
        } else {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.releases.forEachIndexed { i, r ->
                    if (i > 0) Hairline()
                    val installed = r.version == state.installedVersion
                    Way(
                        name = "Version ${r.version}",
                        state = when {
                            installed -> LampState.LIVE
                            r.id == state.newestId -> LampState.ATTENTION
                            else -> LampState.OFF
                        },
                        stateLabel = when {
                            installed -> "On the wearable"
                            r.downloaded -> "On this phone"
                            r.id == state.newestId -> "Newer"
                            else -> r.sizeLabel
                        },
                        detail = listOfNotNull(
                            r.publishedLabel?.let { "Published $it" },
                            r.sizeLabel.takeIf { !installed && r.id != state.newestId || r.downloaded },
                            r.notes,
                            if (r.mandatory) "Marked required by SafeShade." else null
                        ).joinToString(" · ").ifBlank { null },
                        icon = SafeShadeIcons.HardDriveDownload,
                        onClick = if (!busy && !installed) ({ if (r.downloaded) onInstall(r.id) else onDownload(r.id) }) else null
                    )
                }
            }
            if (state.checkError != null) {
                Spacer(Modifier.height(Spacing.sm))
                Note(state.checkError)
            }
        }
        Spacer(Modifier.height(Spacing.md))
        BoardButton(
            label = if (state.checking) "Checking…" else "Check for Updates",
            supporting = if (state.signedIn) null else "Works signed out. Releases are public.",
            icon = SafeShadeIcons.CloudDownload,
            onClick = onCheck,
            enabled = !state.checking && !busy,
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Update")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            val (lamp, word, line) = stepWay(state.step)
            Way(name = "This update", state = lamp, stateLabel = word, detail = line, icon = SafeShadeIcons.Download)
        }
        if (busy) {
            Spacer(Modifier.height(Spacing.md))
            BoardButton(label = "Cancel", onClick = onCancel, weight = ButtonWeight.SECONDARY, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(Spacing.sm))
        Note("The image is checked against its published SHA-256 before a byte is sent, and it counts as installed only when the wearable reports the new version itself.")
    }
}

private fun stepWay(step: OtaStep): Triple<LampState, String, String> = when (step) {
    OtaStep.Idle -> Triple(LampState.OFF, "None", "Pick a published version above.")
    is OtaStep.Downloading -> Triple(LampState.ATTENTION, "${(step.progress * 100).toInt()}%", "Downloading from SafeShade Cloud.")
    OtaStep.Verifying -> Triple(LampState.ATTENTION, "Verifying", "Comparing the file's SHA-256 with the published one.")
    is OtaStep.VerifyFailed -> Triple(LampState.TRIP, "Rejected", step.reason)
    is OtaStep.Sending -> Triple(LampState.ATTENTION, "${step.sent} of ${step.total}", "Sending chunks over the link, one at a time, each acknowledged.")
    OtaStep.AwaitingDeviceVersion -> Triple(LampState.ATTENTION, "Asking", "Every chunk was acknowledged. Asking the wearable which version it now runs.")
    is OtaStep.Installed -> Triple(LampState.LIVE, "Installed", "The wearable reports ${step.version}.")
    is OtaStep.Failed -> Triple(LampState.TRIP, "Failed", step.reason)
}

@Composable
private fun Note(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.board.inkMuted, modifier = Modifier.fillMaxWidth())
}

@Preview(name = "Firmware", showBackground = true, heightDp = 1300)
@Composable
private fun FirmwarePreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            FirmwareScreen(
                state = FirmwareUiState(
                    connected = true,
                    versionNote = "The wearable acknowledged the question but reported no version.",
                    releases = listOf(FirmwareReleaseRow("a", "2.1.0", "1.2 MB", "3 Sep", "Bike braking light timing.", false, false)),
                    newestId = "a",
                    step = OtaStep.Failed("The device acknowledged the update but reported no version")
                ),
                onCheck = {}, onAskVersion = {}, onDownload = {}, onInstall = {}, onCancel = {}, onBack = {}
            )
        }
    }
}
