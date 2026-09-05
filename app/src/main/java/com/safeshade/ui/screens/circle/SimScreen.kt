package com.safeshade.ui.screens.circle

import com.safeshade.platform.IndianPhoneTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the SIM screen draws. */
data class SimUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    /** The number field as typed. */
    val simNumber: String = "",
    /** What is actually stored, so the screen can show unsaved edits honestly. */
    val savedSimNumber: String = "",
    val allowlist: List<String> = emptyList(),
    val newNumber: String = "",
    /**
     * The firmware's own ceiling. Eight numbers is what fits in the allowlist
     * payload; a ninth would be dropped silently on the device, so the app
     * refuses it here where the user can see why.
     */
    val maxEntries: Int = 8,
    /** Each entry is truncated to this length on the wire. */
    val maxNumberLength: Int = 16,
    /** True when the link is up, so a change can actually reach the device. */
    val isDeviceLinked: Boolean = false,
    /** "Sent to the device at 18:02", or why it has not been. */
    val syncLabel: String? = null,
    val isSaving: Boolean = false,
    val errorText: String? = null
) {
    val hasUnsavedNumber: Boolean get() = simNumber.trim() != savedSimNumber.trim()

    val isFull: Boolean get() = allowlist.size >= maxEntries

    val canAddNumber: Boolean
        get() {
            val candidate = newNumber.trim()
            return candidate.isNotBlank() &&
                !isFull &&
                allowlist.none { it.trim() == candidate } &&
                !isSaving
        }
}

/**
 * The wearable's SIM number and who is allowed to message it.
 *
 * Two settings that look like plumbing and are not. The number is what makes
 * the app work at all beyond Bluetooth range — without it, an out-of-range
 * device is simply unreachable — and the allowlist decides whether a stranger
 * who happens to have the number can make the device buzz and display text in
 * front of the person wearing it.
 *
 * The empty allowlist is the trap. It means "accept every sender", which is a
 * reasonable default and an unreasonable surprise, so it is stated as a lit
 * state on the board rather than left as an absence.
 */
@Composable
fun SimScreen(
    state: SimUiState,
    onSimNumberChange: (String) -> Unit,
    onSaveSimNumber: () -> Unit,
    onNewNumberChange: (String) -> Unit,
    onAddNumber: () -> Unit,
    onRemoveNumber: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    // Written out per role rather than interpolating a subject: "not you's own
    // phone" is what a shared sentence produces for the wearer, and a safety
    // app that reads as machine-assembled is a safety app people distrust.
    val whoseNumber = when (state.role) {
        UserRole.GUARDIAN ->
            "This is the number of the SIM inside the wearable, not ${state.wearerName.ifBlank { "the wearer" }}'s own phone number."
        UserRole.COMPANION ->
            "This is the number of the SIM inside the wearable, not the number of this phone."
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            // The theme draws edge to edge, so the window does not resize
            // when the keyboard opens: without this inset a focused field would sit behind it.
            .imePadding()
    ) {
        ScreenHeader(
            title = "SIM and SMS",
            subtitle = "How messages reach the device out of Bluetooth range",
            onBack = onBack,
            backDescription = "Go back to the circle",
            tier = ScreenTier.PUSHED,
            // The `top` is not decoration and is not this screen's choice. Every
            // sub-page outside this bank puts its header inside a LazyColumn
            // whose top contentPadding is `inset + Spacing.sm`; this bank puts
            // the header above the list, so without the same 8dp its content
            // started 8dp higher than every other bank's. That was the whole of
            // "inconsistent header-bottom paddings leading to different sub-page
            // content starting points" - the gap below the header was already
            // uniform; the gap above it was not.
            modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm)
        )

        // The list's own top contentPadding supplies the other half of
        // ScreenHeader's documented 28dp gap, same as every pattern-A screen
        // — see ScreenHeader's KDoc.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.lg,
                bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item("number-heading") {
                SectionPlate(title = "The device's number")
            }

            item("number-state") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "SMS fallback",
                        state = if (state.savedSimNumber.isBlank()) LampState.OFF else LampState.LIVE,
                        stateLabel = if (state.savedSimNumber.isBlank()) "Not set" else "Ready",
                        detail = if (state.savedSimNumber.isBlank()) {
                            "Out of Bluetooth range, nothing can be sent"
                        } else {
                            "Messages fall back to SMS when Bluetooth drops"
                        },
                        icon = SafeShadeIcons.SimAndSms
                    )
                }
            }

            item("number-field") {
                BoardField(
                    value = state.simNumber,
                    onValueChange = onSimNumberChange,
                    label = "SIM number in the device",
                    placeholder = "+91 90000 00000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = IndianPhoneTransformation(),
                    supporting = whoseNumber,
                    enabled = !state.isSaving
                )
            }

            item("number-save") {
                BoardButton(
                    label = if (state.isSaving) "Saving" else "Save the Number",
                    supporting = if (state.hasUnsavedNumber) null else "Nothing has changed",
                    onClick = onSaveSimNumber,
                    enabled = state.hasUnsavedNumber && !state.isSaving,
                    weight = ButtonWeight.COMMIT,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item("allow-heading") {
                SectionPlate(title = "Who may message the device")
            }

            item("allow-state") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "Allowlist",
                        // An empty list is amber rather than off. "Off" would
                        // read as a feature nobody switched on; the truth is
                        // that the device is currently open to every sender,
                        // and that deserves a lit lamp.
                        state = if (state.allowlist.isEmpty()) LampState.ATTENTION else LampState.LIVE,
                        stateLabel = if (state.allowlist.isEmpty()) "Anyone" else "${state.allowlist.size} of ${state.maxEntries}",
                        detail = if (state.allowlist.isEmpty()) {
                            "The device accepts a message from any number"
                        } else {
                            "Messages from any other number are ignored"
                        }
                    )
                }
            }

            if (state.allowlist.isEmpty()) {
                item("allow-empty") {
                    EmptyBay(
                        message = "No numbers yet, so every sender is accepted. Adding one number turns that off and only the listed numbers get through.",
                        // Nothing has been configured yet — the questioning
                        // face, not "still looking".
                        shadyMood = ShadyMood.CURIOUS
                    )
                }
            } else {
                item("allow-list") {
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        state.allowlist.forEachIndexed { index, number ->
                            if (index > 0) Hairline()
                            AllowedNumberRow(
                                number = number,
                                onRemove = { onRemoveNumber(number) },
                                enabled = !state.isSaving
                            )
                        }
                    }
                }
            }

            item("allow-add") {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BoardField(
                        value = state.newNumber,
                        onValueChange = onNewNumberChange,
                        label = "Add a number",
                        placeholder = "+91 90000 00000",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = IndianPhoneTransformation(),
                        supporting = when {
                            state.isFull -> "The list is full at ${state.maxEntries}. Remove one to add another."
                            else -> "Up to ${state.maxEntries} numbers, ${state.maxNumberLength} characters each."
                        },
                        enabled = !state.isFull && !state.isSaving
                    )
                    BoardButton(
                        label = "Add to the list",
                        onClick = onAddNumber,
                        enabled = state.canAddNumber,
                        weight = ButtonWeight.SECONDARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (state.errorText != null) {
                item("error") {
                    Text(
                        text = state.errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkAttention
                    )
                }
            }

            if (state.syncLabel != null) {
                item("sync") {
                    Text(
                        text = state.syncLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }
    }
}

/**
 * One allowed number.
 *
 * Not a `Way`: a way collapses into a single spoken node with no room for a
 * control, and this row's whole purpose is the remove button beside it. The
 * number itself is a `Readout` because it is read digit by digit.
 */
@Composable
private fun AllowedNumberRow(
    number: String,
    onRemove: () -> Unit,
    enabled: Boolean
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.sm)
    ) {
        Readout(value = number, modifier = Modifier.weight(1f))
        IconButton(onClick = onRemove, enabled = enabled) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove $number from the allowlist",
                tint = colors.inkMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Preview(name = "SIM — configured, light", showBackground = true, heightDp = 1100)
@Composable
private fun SimConfiguredLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        SimScreen(
            state = SimUiState(
                role = UserRole.GUARDIAN,
                wearerName = "Baba",
                simNumber = "+91 90000 11111",
                savedSimNumber = "+91 90000 11111",
                allowlist = listOf("+91 90000 22222", "+91 90000 33333"),
                isDeviceLinked = true,
                syncLabel = "Sent to the device at 18:02"
            ),
            onSimNumberChange = {},
            onSaveSimNumber = {},
            onNewNumberChange = {},
            onAddNumber = {},
            onRemoveNumber = {},
            onBack = {}
        )
    }
}

@Preview(name = "SIM — nothing set, dark", showBackground = true, heightDp = 1100)
@Composable
private fun SimEmptyDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        SimScreen(
            state = SimUiState(
                role = UserRole.COMPANION,
                simNumber = "+91 900",
                savedSimNumber = "",
                allowlist = emptyList()
            ),
            onSimNumberChange = {},
            onSaveSimNumber = {},
            onNewNumberChange = {},
            onAddNumber = {},
            onRemoveNumber = {},
            onBack = {}
        )
    }
}
