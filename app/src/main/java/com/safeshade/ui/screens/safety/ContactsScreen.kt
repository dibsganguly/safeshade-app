package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.EmergencyContact
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * A contact being added or edited.
 *
 * [index] is null for a new contact and a position in the list for an existing
 * one, which is the same shape `Routes.contactEdit(index)` already assumes —
 * so this screen serves both an inline editor and a separate edit destination
 * without changing.
 */
data class ContactDraft(
    val index: Int? = null,
    val name: String = "",
    val phone: String = "",
    val isPrimary: Boolean = false
)

/** Everything the contacts screen draws. */
data class ContactsUiState(
    val contacts: List<EmergencyContact> = emptyList(),
    /** Non-null puts the screen in edit mode. */
    val draft: ContactDraft? = null,
    val wearerName: String = "",
    val autoCallEnabled: Boolean = true,
    val smsFallbackEnabled: Boolean = false
)

/**
 * The people an alert actually reaches.
 *
 * The list is ordered, and the order is not cosmetic: the first contact is who
 * the phone calls when a fall countdown runs out. That is stated on the screen
 * rather than left as a convention, because a guardian who assumes the list is
 * alphabetical will put the wrong person first.
 *
 * Editing takes over the whole screen instead of opening a dialog. A dialog on
 * a form with two fields and a destructive action is smaller than the keyboard
 * that covers it, and the elderly persona reads better at full width.
 */
@Composable
fun ContactsScreen(
    state: ContactsUiState,
    onBack: () -> Unit,
    onStartAdd: () -> Unit,
    onStartEdit: (Int) -> Unit,
    onDraftChange: (ContactDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onCancelDraft: () -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val draft = state.draft
    if (draft != null) {
        ContactEditor(
            draft = draft,
            onBack = onCancelDraft,
            onDraftChange = onDraftChange,
            onSave = onSaveDraft,
            onDelete = { draft.index?.let(onDelete) },
            modifier = modifier,
            contentPadding = contentPadding
        )
    } else {
        ContactList(
            state = state,
            onBack = onBack,
            onStartAdd = onStartAdd,
            onStartEdit = onStartEdit,
            modifier = modifier,
            contentPadding = contentPadding
        )
    }
}

@Composable
private fun ContactList(
    state: ContactsUiState,
    onBack: () -> Unit,
    onStartAdd: () -> Unit,
    onStartEdit: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val subject = state.wearerName.ifBlank { "the wearer" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("header") {
            PanelHeader(
                title = "Emergency contacts",
                subtitle = "Who is told when something happens to $subject.",
                onBack = onBack
            )
        }

        if (state.contacts.isEmpty()) {
            item("empty") {
                EmptyBay(
                    message = "No contacts yet. Until there is at least one, a detected fall " +
                        "sounds the siren on the device and stops there.",
                    actionLabel = "Add a contact",
                    onAction = onStartAdd
                )
            }
        } else {
            item("list-heading") { SectionPlate(title = "In order") }

            item("list") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    state.contacts.forEachIndexed { index, contact ->
                        if (index > 0) Hairline()
                        val isFirst = contact.isPrimary ||
                            (state.contacts.none { it.isPrimary } && index == 0)
                        Way(
                            name = contact.name.ifBlank { "Unnamed contact" },
                            // The person who gets called is the live circuit;
                            // the rest are standby. The state word carries it
                            // as well, so the lamp is never the only signal.
                            state = if (isFirst) LampState.LIVE else LampState.OFF,
                            stateLabel = if (isFirst) "Called first" else "Standby",
                            detail = contact.phone,
                            onClick = { onStartEdit(index) }
                        )
                    }
                }
            }

            item("reorder") {
                Note(
                    text = "The first contact is the one the phone calls. Open any other " +
                        "contact to move them to the front."
                )
            }
        }

        item("add") {
            BoardButton(
                label = "Add a contact",
                icon = Icons.Outlined.PersonAdd,
                onClick = onStartAdd,
                weight = if (state.contacts.isEmpty()) ButtonWeight.PRIMARY else ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item("how") {
            Note(
                text = buildString {
                    append("A fall alert ")
                    append(if (state.autoCallEnabled) "calls the first contact" else "does not call anybody, because calling after a fall is switched off")
                    append(", and ")
                    append(if (state.smsFallbackEnabled) "texts everyone on this list with the last known location." else "does not send any texts, because the text fallback is switched off.")
                }
            )
        }
    }
}

/**
 * Add or edit one contact.
 *
 * Validation is deliberately loose — see `isPlausiblePhone`. The only things
 * rejected are an empty name, an empty number, and letters in the number.
 * A number this app refuses to store is a person who does not get called.
 */
@Composable
private fun ContactEditor(
    draft: ContactDraft,
    onBack: () -> Unit,
    onDraftChange: (ContactDraft) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val isNew = draft.index == null

    // Errors appear once a field has been typed in and then emptied, never on
    // a form the user has not touched yet — an add form that opens covered in
    // red reads as an accusation.
    val nameError = if (draft.name.isNotEmpty() && draft.name.isBlank()) "Enter a name" else null
    val phoneError = when {
        draft.phone.isEmpty() -> null
        !isPlausiblePhone(draft.phone) -> "Numbers, spaces, + and - only"
        else -> null
    }
    val canSave = draft.name.isNotBlank() && isPlausiblePhone(draft.phone)

    // A single confirming step for the destructive action, held locally
    // because it is a property of this press and nothing else needs to know.
    var confirmingRemove by remember(draft.index) { mutableStateOf(false) }

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
        PanelHeader(
            title = if (isNew) "Add a contact" else "Edit contact",
            subtitle = "This person is called and texted during an emergency.",
            onBack = onBack
        )

        Spacer(Modifier.height(Spacing.xl))

        PlateField(
            label = "Name",
            value = draft.name,
            onValueChange = { onDraftChange(draft.copy(name = stripDeviceDelimiters(it))) },
            placeholder = "Priya",
            error = nameError,
            maxLength = 24,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.lg))

        PlateField(
            label = "Phone number",
            value = draft.phone,
            onValueChange = { onDraftChange(draft.copy(phone = it)) },
            placeholder = "+91 98300 11223",
            helper = "Include the country code if this phone might be roaming.",
            error = phoneError,
            maxLength = 20,
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done
        )

        Spacer(Modifier.height(Spacing.xl))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Call this person first",
                state = if (draft.isPrimary) LampState.LIVE else LampState.OFF,
                stateLabel = if (draft.isPrimary) "First" else "Not first",
                detail = "Moves them to the top of the list.",
                checked = draft.isPrimary,
                onCheckedChange = { onDraftChange(draft.copy(isPrimary = it)) }
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = if (isNew) "Add contact" else "Save changes",
            onClick = onSave,
            enabled = canSave,
            weight = ButtonWeight.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (!canSave) {
            Spacer(Modifier.height(Spacing.sm))
            Note(text = "A name and a number are both needed before this can be saved.")
        }

        if (!isNew) {
            Spacer(Modifier.height(Spacing.lg))
            BoardButton(
                label = if (confirmingRemove) "Tap again to remove" else "Remove this contact",
                supporting = if (confirmingRemove) "This cannot be undone." else null,
                onClick = {
                    if (confirmingRemove) onDelete() else confirmingRemove = true
                },
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================
// PREVIEWS
// ============================================

private val previewContacts = listOf(
    EmergencyContact("Priya", "+91 98300 11223", isPrimary = true),
    EmergencyContact("Dr Sen", "+91 98300 44556"),
    EmergencyContact("Ashok next door", "+91 98300 77889")
)

@Preview(name = "Contacts — light", showBackground = true, heightDp = 900)
@Composable
private fun ContactsLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        ContactsScreen(
            state = ContactsUiState(
                contacts = previewContacts,
                wearerName = "Baba",
                autoCallEnabled = true,
                smsFallbackEnabled = true
            ),
            onBack = {}, onStartAdd = {}, onStartEdit = {}, onDraftChange = {},
            onSaveDraft = {}, onCancelDraft = {}, onDelete = {}
        )
    }
}

@Preview(
    name = "Contacts — dark, empty",
    showBackground = true,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ContactsEmptyDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        ContactsScreen(
            state = ContactsUiState(autoCallEnabled = true, smsFallbackEnabled = false),
            onBack = {}, onStartAdd = {}, onStartEdit = {}, onDraftChange = {},
            onSaveDraft = {}, onCancelDraft = {}, onDelete = {}
        )
    }
}

@Preview(name = "Contact editor — light", showBackground = true, heightDp = 1000)
@Composable
private fun ContactEditorPreview() {
    SafeShadeTheme(darkTheme = false) {
        ContactsScreen(
            state = ContactsUiState(
                contacts = previewContacts,
                draft = ContactDraft(index = 1, name = "Dr Sen", phone = "+91 98300 44556")
            ),
            onBack = {}, onStartAdd = {}, onStartEdit = {}, onDraftChange = {},
            onSaveDraft = {}, onCancelDraft = {}, onDelete = {}
        )
    }
}
