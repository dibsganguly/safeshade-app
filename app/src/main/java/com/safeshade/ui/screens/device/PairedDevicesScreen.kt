package com.safeshade.ui.screens.device

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.PairedDevice
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the paired devices screen draws. */
data class PairedDevicesUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val devices: List<PairedDevice> = emptyList(),
    /** MAC of the device the link is currently up to, if any. */
    val connectedAddress: String? = null,
    val isScanning: Boolean = false,
    /**
     * False until BLUETOOTH_SCAN / BLUETOOTH_CONNECT (or, below API 31,
     * ACCESS_FINE_LOCATION) are granted. Pairing must be unreachable until
     * then — the scan call itself throws without them.
     */
    val permissionsGranted: Boolean = true,
    /** Address awaiting a forget confirmation, hoisted so the caller can clear it. */
    val confirmingForget: String? = null,
    /** Preformatted "last connected" text, keyed by address. */
    val lastConnectedLabels: Map<String, String> = emptyMap()
)

/**
 * Paired devices.
 *
 * A short list by design. Most households have one wearable; two is a
 * grandparent and a child. The screen is therefore laid out for reading rather
 * than for scanning a long list — each device gets its own plate with its
 * actions attached, instead of a dense row with a hidden overflow menu.
 */
@Composable
fun PairedDevicesScreen(
    state: PairedDevicesUiState,
    onPairNew: () -> Unit,
    onRequestPermissions: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onForgetRequested: (String) -> Unit,
    onConfirmForget: (String) -> Unit,
    onCancelForget: () -> Unit,
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
            ScreenHeader(title = "Paired devices", onBack = onBack)
        }

        item("heading") { SectionPlate(title = "Saved") }

        if (state.devices.isEmpty()) {
            item("empty") {
                EmptyBay(
                    message = "No SafeShade device saved yet.",
                    actionLabel = if (state.permissionsGranted) "Pair a device" else "Grant permissions",
                    onAction = if (state.permissionsGranted) onPairNew else onRequestPermissions,
                    // Nobody has paired anything yet — this app's own doing,
                    // not a bad-news state.
                    shadyMood = ShadyMood.DUMBFOUNDED
                )
            }
        } else {
            items(state.devices, key = { it.address }) { device ->
                DevicePlate(
                    device = device,
                    isConnected = device.address == state.connectedAddress,
                    lastConnectedLabel = state.lastConnectedLabels[device.address],
                    onConnect = { onConnect(device.address) },
                    onDisconnect = onDisconnect,
                    onForget = { onForgetRequested(device.address) }
                )
            }

            item("pair") {
                BoardButton(
                    label = if (state.isScanning) "Searching" else "Pair Another Device",
                    supporting = if (state.permissionsGranted) {
                        "Hold the button on the wearable until it shows the pairing screen"
                    } else {
                        "Bluetooth permission is needed before the app can search"
                    },
                    icon = SafeShadeIcons.ConnectToTheDevice,
                    onClick = if (state.permissionsGranted) onPairNew else onRequestPermissions,
                    enabled = !state.isScanning,
                    weight = ButtonWeight.ATTENTION,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item("note") {
            Text(
                text = "Only one device is connected at a time. Forgetting a device " +
                    "removes it from this phone; it does not reset the wearable.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
    }

    val forgetting = state.confirmingForget
    if (forgetting != null) {
        val device = state.devices.firstOrNull { it.address == forgetting }
        ForgetDialog(
            name = device?.name ?: forgetting,
            onConfirm = { onConfirmForget(forgetting) },
            onCancel = onCancelForget
        )
    }
}

/**
 * One saved device with its two actions attached.
 *
 * The actions sit under the row rather than behind an overflow menu because
 * "forget" is destructive and hiding a destructive action behind a menu is how
 * people tap it by accident on the way to something else.
 */
@Composable
private fun DevicePlate(
    device: PairedDevice,
    isConnected: Boolean,
    lastConnectedLabel: String?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Way(
            name = device.name,
            state = if (isConnected) LampState.LIVE else LampState.OFF,
            stateLabel = if (isConnected) "Connected" else "Saved",
            detail = lastConnectedLabel?.let { "Last connected $it · ${device.address}" }
                ?: device.address,
            icon = SafeShadeIcons.PairedDevices
        )
        Hairline()
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(Spacing.lg)
        ) {
            BoardButton(
                label = if (isConnected) "Disconnect" else "Connect",
                onClick = if (isConnected) onDisconnect else onConnect,
                weight = if (isConnected) ButtonWeight.SECONDARY else ButtonWeight.PRIMARY,
                modifier = Modifier.weight(1f)
            )
            BoardButton(
                label = "Forget",
                onClick = onForget,
                // QUIET rather than DANGER: DANGER is reserved for things that
                // happen in the world — a siren, a call. Forgetting a pairing is
                // recoverable in under a minute.
                weight = ButtonWeight.QUIET,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ForgetDialog(
    name: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = MaterialTheme.board
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = colors.plate,
        titleContentColor = colors.ink,
        textContentColor = colors.inkMuted,
        title = {
            Text(
                text = "Forget $name",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = "This phone will stop connecting to it. Fall alerts from " +
                        "this device will no longer reach you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
                Text(
                    text = "Settings already on the wearable stay as they are. You can " +
                        "pair it again at any time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        },
        confirmButton = {
            BoardButton(label = "Forget", onClick = onConfirm, weight = ButtonWeight.PRIMARY)
        },
        dismissButton = {
            BoardButton(label = "Keep", onClick = onCancel, weight = ButtonWeight.QUIET)
        }
    )
}

// ============================================================================
// Previews
// ============================================================================

private val previewPaired = PairedDevicesUiState(
    connection = ConnectionState.Ready,
    devices = listOf(
        PairedDevice(address = "C4:DE:E2:11:07:A9", name = "Baba's cane"),
        PairedDevice(address = "C4:DE:E2:11:0B:31", name = "Mishti's backpack")
    ),
    connectedAddress = "C4:DE:E2:11:07:A9",
    lastConnectedLabels = mapOf(
        "C4:DE:E2:11:07:A9" to "now",
        "C4:DE:E2:11:0B:31" to "3 days ago"
    )
)

@Composable
private fun PairedPreviewHost(state: PairedDevicesUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        PairedDevicesScreen(
            state = state,
            onPairNew = {},
            onRequestPermissions = {},
            onConnect = {},
            onDisconnect = {},
            onForgetRequested = {},
            onConfirmForget = {},
            onCancelForget = {}
        )
    }
}

@Preview(name = "Paired · light", showBackground = true)
@Composable
private fun PairedPreviewLight() {
    SafeShadeTheme(darkTheme = false) { PairedPreviewHost(previewPaired) }
}

@Preview(name = "Paired · dark", showBackground = true)
@Composable
private fun PairedPreviewDark() {
    SafeShadeTheme(darkTheme = true) { PairedPreviewHost(previewPaired) }
}

@Preview(name = "Paired · none saved", showBackground = true)
@Composable
private fun PairedPreviewEmpty() {
    SafeShadeTheme(darkTheme = false) {
        PairedPreviewHost(
            PairedDevicesUiState(
                connection = ConnectionState.Disconnected,
                permissionsGranted = false
            )
        )
    }
}

@Preview(name = "Paired · forgetting", showBackground = true)
@Composable
private fun PairedPreviewForgetting() {
    SafeShadeTheme(darkTheme = false) {
        PairedPreviewHost(previewPaired.copy(confirmingForget = "C4:DE:E2:11:0B:31"))
    }
}
