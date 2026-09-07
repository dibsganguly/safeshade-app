package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.EmergencyContact
import com.safeshade.data.PairedDevice
import com.safeshade.data.Wearer
import com.safeshade.data.WearerResult
import com.safeshade.platform.PhoneNumbers
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.screens.profile.FaceEditor
import com.safeshade.ui.screens.safety.PlateField
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlinx.coroutines.launch

/** Everything the editor draws. */
data class WearerEditorUiState(
    /** The person being edited; a fresh `Wearer()` for a new one. */
    val wearer: Wearer = Wearer(),
    val isNew: Boolean = false,
    /** The wearables this phone has met; each can be bound to this person. */
    val pairedDevices: List<PairedDevice> = emptyList(),
    /** Which of them is connected right now, if any. */
    val connectedAddress: String? = null,
    /** How many medical fields are filled, for the row's state word. */
    val medicalFieldsFilled: Int = 0,
    val medicalFieldsTotal: Int = 11,
    /** False for the Companion's own record and for the only wearer. */
    val canRemove: Boolean = true
)

/**
 * One person a Guardian looks after: name and face, which wearable is
 * theirs, their medical ID, and contacts that are theirs alone.
 *
 * Binding a wearable to a person is the one decision on this page with a
 * consequence on the device: the medical card that gets pushed over BLE is
 * the bound person's. So the page never guesses it. A wearable this phone
 * has met is offered as a switch, off until someone throws it.
 *
 * Nothing is written until Save, and Save reports the repository's answer:
 * a refused write (the last person, the Companion's own record) shows its
 * reason under the button rather than pretending.
 */
@Composable
fun WearerEditorScreen(
    state: WearerEditorUiState,
    onSave: suspend (Wearer) -> WearerResult,
    onRemove: suspend () -> WearerResult,
    onOpenMedicalId: () -> Unit,
    onOpenPairing: () -> Unit,
    onDone: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf(state.wearer.name) }
    var avatarId by rememberSaveable { mutableStateOf(state.wearer.avatarId) }
    var addresses by rememberSaveable { mutableStateOf(state.wearer.deviceAddresses) }
    var contacts by rememberSaveable { mutableStateOf(state.wearer.contacts) }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactPhone by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmingRemove by remember { mutableStateOf(false) }

    val self = state.wearer.isSelf
    val title = when {
        state.isNew -> "Add a person"
        self -> "You"
        else -> state.wearer.name.ifBlank { "This person" }
    }

    fun act(block: suspend () -> WearerResult) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            when (val r = block()) {
                is WearerResult.Ok -> onDone()
                is WearerResult.Refused -> error = r.reason
            }
            busy = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = contentPadding.calculateTopPadding() + Spacing.sm, bottom = Spacing.lg)
        ) {
            ScreenHeader(
                title = title,
                subtitle = if (self) "Your name is on the board and on the emergency card."
                else "Their name is on the board and on the emergency card.",
                onBack = onBack,
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            Spacer(Modifier.height(Spacing.lg))

            FaceEditor(
                name = name,
                onNameChange = { name = it },
                avatarId = avatarId,
                onAvatarChange = { avatarId = it },
                nameLabel = if (self) "Your name" else "Their name",
                namePlaceholder = if (self) "Priya" else "Baba"
            )

            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(
                title = if (self) "Your wearable" else "Their wearable",
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            Spacer(Modifier.height(Spacing.md))
            BoardPlate(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter)) {
                if (state.pairedDevices.isEmpty()) {
                    Way(
                        name = "No wearable paired yet",
                        state = LampState.OFF,
                        stateLabel = "Pair",
                        detail = "Pair one from the Device page, then bind it here",
                        icon = SafeShadeIcons.DeviceSettings,
                        onClick = onOpenPairing
                    )
                }
                state.pairedDevices.forEachIndexed { index, device ->
                    if (index > 0) Hairline()
                    val bound = addresses.any { it.equals(device.address, ignoreCase = true) }
                    val connected = device.address.equals(state.connectedAddress, ignoreCase = true)
                    Way(
                        name = device.name.ifBlank { "Wearable" },
                        state = when {
                            bound && connected -> LampState.LIVE
                            bound -> LampState.OFF
                            else -> LampState.OFF
                        },
                        stateLabel = when {
                            bound && connected -> "Worn, live"
                            bound -> "Worn"
                            else -> "Not theirs"
                        },
                        detail = device.address,
                        icon = SafeShadeIcons.DeviceSettings,
                        checked = bound,
                        onCheckedChange = { on ->
                            addresses = if (on) addresses + device.address
                            else addresses.filterNot { it.equals(device.address, ignoreCase = true) }
                        },
                        // The whole row throws the switch; a name is a bigger target than a 52 dp track.
                        onClick = {
                            addresses = if (bound) addresses.filterNot { it.equals(device.address, ignoreCase = true) }
                            else addresses + device.address
                        }
                    )
                }
            }

            if (!state.isNew) {
                Spacer(Modifier.height(Spacing.xl))
                SectionPlate(title = "Medical ID", modifier = Modifier.padding(horizontal = Spacing.gutter))
                Spacer(Modifier.height(Spacing.md))
                BoardPlate(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter)) {
                    Way(
                        name = "Medical ID",
                        state = if (state.medicalFieldsFilled > 0) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.medicalFieldsFilled > 0) "Filled in" else "Empty",
                        detail = "${state.medicalFieldsFilled} of ${state.medicalFieldsTotal} fields",
                        icon = SafeShadeIcons.MedicalId,
                        onClick = onOpenMedicalId
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(
                title = if (self) "Contacts for you alone" else "Contacts for this person alone",
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            Spacer(Modifier.height(Spacing.md))
            BoardPlate(modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter)) {
                Column {
                    contacts.forEachIndexed { index, contact ->
                        if (index > 0) Hairline()
                        Way(
                            name = contact.name.ifBlank { contact.phone },
                            state = LampState.LIVE,
                            stateLabel = "Remove",
                            detail = if (contact.name.isBlank()) null else contact.phone,
                            icon = SafeShadeIcons.User,
                            onClick = { contacts = contacts.filterIndexed { i, _ -> i != index } }
                        )
                    }
                    if (contacts.isNotEmpty()) Hairline()
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md)
                    ) {
                        Text(
                            text = "The Safety page's emergency contacts are called for everyone. These are called too, only for ${if (self) "you" else name.ifBlank { "this person" }}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint
                        )
                        Row {
                            PlateField(
                                label = "Name",
                                value = contactName,
                                onValueChange = { contactName = it },
                                placeholder = "Dr Sen",
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(Spacing.md))
                            PlateField(
                                label = "Phone",
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                placeholder = "98300 00000",
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        val phoneOk = PhoneNumbers.digitsOf(contactPhone).length >= 10
                        BoardButton(
                            label = "Add Contact",
                            icon = SafeShadeIcons.UserAdd,
                            onClick = {
                                contacts = contacts + EmergencyContact(name = contactName.trim(), phone = contactPhone.trim())
                                contactName = ""
                                contactPhone = ""
                            },
                            weight = ButtonWeight.SECONDARY,
                            enabled = phoneOk,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (!state.isNew && state.canRemove) {
                Spacer(Modifier.height(Spacing.xl))
                Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
                    BoardButton(
                        label = "Remove This Person",
                        icon = SafeShadeIcons.UserRemove,
                        supporting = "Their medical ID and contacts go with them",
                        onClick = { confirmingRemove = true },
                        weight = ButtonWeight.DANGER,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm, bottom = contentPadding.calculateBottomPadding() + Spacing.lg)
        ) {
            val shown = error
            if (shown != null) {
                Text(text = shown, style = MaterialTheme.typography.bodyMedium, color = colors.inkTrip)
                Spacer(Modifier.height(Spacing.sm))
            }
            BoardButton(
                label = if (busy) "Saving…" else "Save",
                onClick = {
                    act {
                        onSave(
                            state.wearer.copy(
                                name = name.trim(),
                                avatarId = avatarId,
                                deviceAddresses = addresses,
                                contacts = contacts
                            )
                        )
                    }
                },
                weight = ButtonWeight.COMMIT,
                enabled = name.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (confirmingRemove) {
        AlertDialog(
            onDismissRequest = { confirmingRemove = false },
            containerColor = colors.plate,
            titleContentColor = colors.ink,
            textContentColor = colors.inkMuted,
            title = { Text("Remove ${name.ifBlank { "this person" }}", style = MaterialTheme.typography.titleMedium, color = colors.ink) },
            text = {
                Text(
                    "Their medical ID and their own contacts are removed from this phone. A wearable bound to them stays paired and can be bound to someone else.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted
                )
            },
            confirmButton = {
                BoardButton(label = "Remove", onClick = { confirmingRemove = false; act(onRemove) }, weight = ButtonWeight.DANGER)
            },
            dismissButton = {
                BoardButton(label = "Keep", onClick = { confirmingRemove = false }, weight = ButtonWeight.QUIET)
            }
        )
    }
}

@Preview(name = "Wearer editor", showBackground = true)
@Composable
private fun WearerEditorPreview() {
    SafeShadeTheme {
        WearerEditorScreen(
            state = WearerEditorUiState(
                wearer = Wearer(name = "Baba", deviceAddresses = listOf("AA:BB")),
                pairedDevices = listOf(PairedDevice("AA:BB", "SafeShade S1")),
                medicalFieldsFilled = 6
            ),
            onSave = { WearerResult.Ok(emptyList()) },
            onRemove = { WearerResult.Ok(emptyList()) },
            onOpenMedicalId = {}, onOpenPairing = {}, onDone = {}, onBack = {}
        )
    }
}
