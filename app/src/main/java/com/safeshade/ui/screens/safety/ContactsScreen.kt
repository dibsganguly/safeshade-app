package com.safeshade.ui.screens.safety

import com.safeshade.platform.IndianPhoneTransformation
import com.safeshade.platform.PhoneNumbers
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
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.ChipRow
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.RELATIONSHIP_SUGGESTIONS
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
    val isPrimary: Boolean = false,
    /**
     * How this person is related to the wearer — see [EmergencyContact].
     *
     * Last, and defaulted, on purpose: the draft is built positionally at its
     * one call site, so a field added anywhere else would break it.
     */
    val relationship: String = ""
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
            // ScreenHeader directly, not PanelHeader. This screen scrolls a
            // list already rhythmed with `spacedBy(Spacing.lg)`, and the
            // adapter exists to supply that same 16dp to the bank's plain
            // `Column` screens - so going through it here added the gap twice
            // and started this screen's content 24dp lower than its peers.
            ScreenHeader(
                title = "Emergency contacts",
                subtitle = "Who is told when something happens to $subject.",
                onBack = onBack
            )
        }

        if (state.contacts.isEmpty()) {
            item("empty") {
                EmptyBay(
                    withShady = false,
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
                            // "Daughter · 98300 11223". The relationship is
                            // the whole point of storing it — a responder
                            // holding this phone needs to know who they are
                            // ringing before they ring. A contact without one
                            // shows the number alone rather than a dash: an
                            // empty relationship means nobody said, and a
                            // placeholder there reads as an answer.
                            detail = listOfNotNull(
                                contact.relationship.takeIf { it.isNotBlank() },
                                PhoneNumbers.format(contact.phone)
                            ).joinToString(" · "),
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

            // The empty bay above already carries this action while there are
            // no contacts — a second "add" button beside it would just be the
            // same offer said twice.
            item("add") {
                BoardButton(
                    label = "Add a contact",
                    icon = Icons.Outlined.PersonAdd,
                    onClick = onStartAdd,
                    weight = ButtonWeight.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
    val phoneDigits = PhoneNumbers.digitsOf(draft.phone)
    val phoneError = when {
        draft.phone.isEmpty() -> null
        !isPlausiblePhone(draft.phone) -> "Numbers, spaces, + and - only"
        // Surfaced rather than tolerated. A number with more digits than the
        // dialling plan has is a typo, and an emergency contact carrying one
        // fails at the only moment it is ever used. It was previously accepted
        // and then quietly trimmed to ten digits somewhere downstream, which
        // produced a different, entirely valid-looking number.
        phoneDigits.length > 10 -> "That is ${phoneDigits.length} digits. Indian numbers have 10."
        else -> null
    }
    val canSave = draft.name.isNotBlank() &&
        isPlausiblePhone(draft.phone) &&
        phoneDigits.length <= 10

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
            onBack = onBack
        )

        // No Spacer here: the Name field is the first thing on this screen,
        // and PanelHeader already supplies the whole header-to-content gap.
        // A Spacing.xl on top of that was a third gap stacked on the header's
        // own 20dp, not a separator between two pieces of content.
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

        // Optional, and never validated. "Upstairs neighbour" and "Priya's
        // husband" are real answers, so the chips below are suggestions that
        // fill the box in one tap rather than a list to choose from — a value
        // matching none of them lights none of them, which is correct.
        PlateField(
            label = "Relationship",
            value = draft.relationship,
            onValueChange = { onDraftChange(draft.copy(relationship = stripDeviceDelimiters(it))) },
            placeholder = "Daughter",
            helper = "Optional. Shown beside their name, so whoever picks up this " +
                "phone knows who they are ringing.",
            maxLength = 24,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.sm))

        ChipRow(
            options = RELATIONSHIP_SUGGESTIONS,
            selected = draft.relationship,
            onSelect = { onDraftChange(draft.copy(relationship = it)) }
        )

        Spacer(Modifier.height(Spacing.lg))

        PlateField(
            label = "Phone number",
            value = draft.phone,
            // Normalised on the way in, which is what makes the whole file's
            // stated contract ("the stored value is always bare digits") true:
            // nothing enforced it before, so contacts were stored exactly as
            // typed — country codes, spaces and all — and every consumer
            // re-derived the digits its own slightly different way.
            onValueChange = { onDraftChange(draft.copy(phone = PhoneNumbers.digitsOf(it))) },
            placeholder = "+91 98300 11223",
            helper = "Include the country code if this phone might be roaming.",
            error = phoneError,
            maxLength = 20,
            keyboardType = KeyboardType.Phone,
            visualTransformation = IndianPhoneTransformation(),
            imeAction = ImeAction.Done
        )

        Spacer(Modifier.height(Spacing.xl))

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Call this person first",
                state = if (draft.isPrimary) LampState.LIVE else LampState.OFF,
                stateLabel = if (draft.isPrimary) "First" else "Not first",
                checked = draft.isPrimary,
                onCheckedChange = { onDraftChange(draft.copy(isPrimary = it)) }
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = if (isNew) "Add Contact" else "Save Changes",
            onClick = onSave,
            enabled = canSave,
            weight = ButtonWeight.COMMIT,
            modifier = Modifier.fillMaxWidth()
        )

        if (!canSave) {
            Spacer(Modifier.height(Spacing.sm))
            // Names the actual blocker. "A name and a number are both needed"
            // was the only hint, and it is plainly wrong when both are filled
            // in and it is the number's length that is holding the save —
            // leaving the user to stare at a dead button with no explanation.
            Note(
                text = when {
                    phoneError != null -> "Correct the number before saving."
                    draft.name.isBlank() -> "A name is needed before this can be saved."
                    else -> "A name and a number are both needed before this can be saved."
                }
            )
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

// Two with a relationship, one without — the row builds its detail line
// differently in each case, and a preview where every contact is filled in
// would only ever show one of the two.
private val previewContacts = listOf(
    EmergencyContact("Priya", "+91 98300 11223", isPrimary = true, relationship = "Daughter"),
    EmergencyContact("Dr Sen", "+91 98300 44556", relationship = "Doctor"),
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
                draft = ContactDraft(
                    index = 1,
                    name = "Dr Sen",
                    phone = "+91 98300 44556",
                    relationship = "Doctor"
                )
            ),
            onBack = {}, onStartAdd = {}, onStartEdit = {}, onDraftChange = {},
            onSaveDraft = {}, onCancelDraft = {}, onDelete = {}
        )
    }
}
