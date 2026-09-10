package com.safeshade.ui.screens.safety

import com.safeshade.platform.IndianPhoneTransformation
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.MedicalId
import com.safeshade.ui.board.BLOOD_GROUP_SUGGESTIONS
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.Callout
import com.safeshade.ui.board.ChipRow
import com.safeshade.ui.board.EditorFootBar
import com.safeshade.ui.board.EditorScaffold
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.FooterAction
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PlateField
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.TaggedPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/** Everything the medical ID editor draws. */
data class MedicalIdUiState(
    val medicalId: MedicalId = MedicalId(),
    val wearerName: String = "",
    val linkLive: Boolean = false,
    /** True when there are edits that have not been saved yet. */
    val isDirty: Boolean = false,
    /** e.g. "Sent to the device at 4:12 pm". Null before the first send. */
    val lastSyncedLabel: String? = null
)

/**
 * Per-field caps, mirroring `DeviceProtocol.health`.
 *
 * Not decoration: the device parses this payload positionally against a
 * negotiated MTU and the transport truncates silently when it overflows. A
 * field that accepts 200 characters into a 40-character slot is a field that
 * throws away someone's work at send time without telling them.
 */
private const val MAX_BLOOD_TYPE = 8
private const val MAX_CONTACT_NAME = 24
private const val MAX_CONTACT_PHONE = 20
private const val MAX_FREE_TEXT = 40

/**
 * The Medical ID.
 *
 * All eleven fields the firmware parses, but not at equal weight and no longer
 * in the order they cross the wire. Four stay in the open — blood type,
 * allergies, and the first contact's name and number — because those four are
 * what `isUsable` tests, which is to say they are the ones that make the
 * difference between a card worth showing a paramedic and one that is not. The
 * remaining seven are filled in once during setup and sit behind a counted
 * disclosure; twelve stacked text boxes were what made this read as a form to
 * be completed rather than a card to be got right.
 *
 * Two things about this screen are load-bearing:
 *
 *  1. **Commas are removed as you type.** The device splits every payload on
 *     commas with no escaping, so one comma in Allergies pushes age into
 *     Conditions and corrupts every field after it. Stripping on entry means
 *     what is on screen is exactly what a responder will read; stripping
 *     silently at send time would not.
 *  2. **Blank is a legitimate answer, and completeness is not a score.** The
 *     card omits empty fields entirely rather than printing "Allergies: —",
 *     because an empty label invites a reader to think the answer is "none"
 *     when it means "nobody filled this in".
 */
@Composable
fun MedicalIdScreen(
    state: MedicalIdUiState,
    onBack: () -> Unit,
    onChange: (MedicalId) -> Unit,
    onSave: () -> Unit,
    onOpenCard: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val id = state.medicalId

    // The editor's foot is pinned (candidate 2.34): a person seven fields
    // into the card can always see the way out, and the pair says what is
    // unsaved and where the record stands without a sentence under a button.
    EditorScaffold(
        modifier = modifier.fillMaxSize().background(colors.ground),
        bottomPadding = contentPadding.calculateBottomPadding(),
        foot = {
            EditorFootBar(
                primaryLabel = if (state.linkLive) "Save and Send" else "Save",
                onPrimary = onSave,
                secondaryLabel = "Discard",
                // Leaving the editor drops the unsaved edits; that is what
                // Discard means here, and it is red because it is a loss.
                onSecondary = onBack,
                changedLine = if (state.isDirty) "Unsaved changes" else "Nothing to save",
                statusLine = when {
                    state.isDirty && state.linkLive -> "Sends to the wearable on save"
                    state.isDirty -> "Kept until the wearable connects"
                    else -> state.lastSyncedLabel ?: "Not yet on the wearable"
                },
                statusState = when {
                    state.isDirty -> LampState.ATTENTION
                    state.lastSyncedLabel != null -> LampState.LIVE
                    else -> null
                },
                primaryEnabled = state.isDirty,
                secondaryEnabled = state.isDirty
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
            PanelHeader(
                title = "Medical ID",
                subtitle = "What a paramedic reads off the device screen.",
                onBack = onBack
            )

            CompletenessPlate(medicalId = id, onOpenCard = onOpenCard)

            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "The essentials")
            Spacer(Modifier.height(Spacing.md))

            PlateField(
                label = "Blood type",
                value = id.bloodType,
                onValueChange = { onChange(id.copy(bloodType = stripDeviceDelimiters(it))) },
                placeholder = "B+",
                maxLength = MAX_BLOOD_TYPE,
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(Spacing.sm))

            // The box stays: a wearer whose record says "O+ (Rh null)" or who
            // knows only "O" can still type it, and neither lights a chip,
            // which is correct rather than an error. What the row removes is
            // spelling a blood group on a phone keyboard, where a slip between
            // B+ and B- is both easy and the worst typo on this card.
            ChipRow(
                options = BLOOD_GROUP_SUGGESTIONS,
                selected = id.bloodType,
                onSelect = { onChange(id.copy(bloodType = it)) }
            )

            Spacer(Modifier.height(Spacing.lg))

            PlateField(
                label = "Allergies",
                value = id.allergies,
                onValueChange = { onChange(id.copy(allergies = stripDeviceDelimiters(it))) },
                placeholder = "Penicillin. Peanuts.",
                maxLength = MAX_FREE_TEXT,
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(Spacing.sm))

            // The one thing on this page a person must not miss, so it is the
            // page's one callout and it sits under the first free-text box,
            // which is where the comma gets typed.
            Callout(
                lead = "Commas are removed as you type.",
                sentence = "The wearable splits the record on commas, so one in Allergies would shift every field after it. Separate items with a full stop or the word \"and\"."
            )

            Spacer(Modifier.height(Spacing.lg))

            PlateField(
                label = "First contact – name",
                value = id.contactName,
                onValueChange = { onChange(id.copy(contactName = stripDeviceDelimiters(it))) },
                placeholder = "Priya (daughter)",
                maxLength = MAX_CONTACT_NAME,
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(Spacing.lg))

            PlateField(
                label = "First contact – number",
                value = id.emergencyContact,
                onValueChange = { onChange(id.copy(emergencyContact = stripDeviceDelimiters(it))) },
                placeholder = "+91 98300 11223",
                error = if (id.emergencyContact.isNotEmpty() && !isPlausiblePhone(id.emergencyContact)) {
                    "Numbers, spaces, + and - only"
                } else {
                    null
                },
                maxLength = MAX_CONTACT_PHONE,
                keyboardType = KeyboardType.Phone,
                visualTransformation = IndianPhoneTransformation(),
                imeAction = ImeAction.Next
            )

            Spacer(Modifier.height(Spacing.sm))
            Footnote("Printed on the card for a person to ring themselves. The list the phone dials after a fall is separate, under Contacts.")

            Spacer(Modifier.height(Spacing.xl))

            // Seven real fields, none of them the reason anybody opens this
            // screen a second time. Closed, the bank previews which of the
            // seven are filled in, so it can stay closed.
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                ExpandableSection(
                    label = "More medical details",
                    icon = SafeShadeIcons.MedicalId,
                    count = 7,
                    preview = listOf(
                        (if (id.age > 0) "Age ${id.age}" else "Age") to (if (id.age > 0) LampState.LIVE else LampState.OFF),
                        "Conditions" to (if (id.conditions.isNotBlank()) LampState.LIVE else LampState.OFF),
                        "Medications" to (if (id.medications.isNotBlank()) LampState.LIVE else LampState.OFF),
                        "Notes" to (if (id.notes.isNotBlank()) LampState.LIVE else LampState.OFF),
                        "Donor" to (if (id.organDonor) LampState.LIVE else LampState.OFF),
                        "Second contact" to (if (id.secondaryContact.isNotBlank()) LampState.LIVE else LampState.OFF)
                    )
                ) {
                    Hairline()
                    Column(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.lg)) {
                        PlateField(
                            label = "Age",
                            value = if (id.age > 0) id.age.toString() else "",
                            onValueChange = { onChange(id.copy(age = parseAge(it))) },
                            placeholder = "74",
                            helper = "Responders use this to work out doses.",
                            maxLength = 3,
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )

                        Spacer(Modifier.height(Spacing.lg))

                        PlateField(
                            label = "Conditions",
                            value = id.conditions,
                            onValueChange = { onChange(id.copy(conditions = stripDeviceDelimiters(it))) },
                            placeholder = "Type 2 diabetes. Pacemaker.",
                            maxLength = MAX_FREE_TEXT,
                            imeAction = ImeAction.Next
                        )

                        Spacer(Modifier.height(Spacing.lg))

                        PlateField(
                            label = "Medications",
                            value = id.medications,
                            onValueChange = { onChange(id.copy(medications = stripDeviceDelimiters(it))) },
                            placeholder = "Metformin 500mg twice daily",
                            maxLength = MAX_FREE_TEXT,
                            imeAction = ImeAction.Next
                        )

                        Spacer(Modifier.height(Spacing.lg))

                        PlateField(
                            label = "Anything else",
                            value = id.notes,
                            onValueChange = { onChange(id.copy(notes = stripDeviceDelimiters(it))) },
                            placeholder = "Hard of hearing on the left side",
                            helper = "One short line. This is the first thing dropped if the record is too long to send.",
                            maxLength = MAX_FREE_TEXT,
                            singleLine = false,
                            imeAction = ImeAction.Next
                        )

                        Spacer(Modifier.height(Spacing.lg))

                        PlateField(
                            label = "Second contact – name",
                            value = id.secondaryContactName,
                            onValueChange = { onChange(id.copy(secondaryContactName = stripDeviceDelimiters(it))) },
                            placeholder = "Dr Sen",
                            maxLength = MAX_CONTACT_NAME,
                            imeAction = ImeAction.Next
                        )

                        Spacer(Modifier.height(Spacing.lg))

                        PlateField(
                            label = "Second contact – number",
                            value = id.secondaryContact,
                            onValueChange = { onChange(id.copy(secondaryContact = stripDeviceDelimiters(it))) },
                            placeholder = "+91 98300 44556",
                            error = if (id.secondaryContact.isNotEmpty() && !isPlausiblePhone(id.secondaryContact)) {
                                "Numbers, spaces, + and - only"
                            } else {
                                null
                            },
                            maxLength = MAX_CONTACT_PHONE,
                            keyboardType = KeyboardType.Phone,
                            visualTransformation = IndianPhoneTransformation(),
                            imeAction = ImeAction.Done
                        )
                    }
                    Hairline()
                    Way(
                        name = "Organ donor",
                        state = if (id.organDonor) LampState.LIVE else LampState.OFF,
                        stateLabel = if (id.organDonor) "Yes" else "Not stated",
                        checked = id.organDonor,
                        onCheckedChange = { onChange(id.copy(organDonor = it)) }
                    )
                }
            }
        }
    }
}

/**
 * How much of the card is filled in.
 *
 * Counted, never scored. `filledFieldCount` treats "not an organ donor" as an
 * unfilled field, so eleven of eleven is not reachable for everyone and the
 * copy never suggests it should be — the number is here to show a card that is
 * nearly empty, not to ask for a full house.
 */
@Composable
private fun CompletenessPlate(medicalId: MedicalId, onOpenCard: () -> Unit) {
    val colors = MaterialTheme.board
    val filled = medicalId.filledFieldCount
    val progress = filled / 11f

    // The verdict is a stamp in the corner (2.90) in its state ink, and the
    // card itself is the plate's foot (2.92): the thing this plate is about
    // is one row away rather than a button at the end of the page.
    TaggedPlate(
        tag = if (medicalId.isUsable) "Worth showing" else "Not usable yet",
        tagColor = if (medicalId.isUsable) colors.inkLive else colors.inkAttention
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Text(
                text = "$filled of 11 filled in",
                style = MaterialTheme.typography.titleMedium,
                color = colors.ink
            )
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { progress },
                // Achromatic on purpose. This is a quantity, not a circuit
                // state, and colour on this board only ever means state.
                color = colors.ink,
                trackColor = colors.recess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Stroke.heavy)
                    .semantics { contentDescription = "$filled of 11 details filled in" }
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "Blood type, a contact number or an allergy is enough for the card to be " +
                    "worth carrying. Leave anything blank that does not apply.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint
            )
        }
        FooterAction(
            label = "Show the emergency card",
            icon = SafeShadeIcons.QrCode,
            enabled = medicalId.isUsable,
            onClick = onOpenCard
        )
    }
}

// ============================================
// PREVIEWS
// ============================================

private val previewMedicalId = MedicalId(
    bloodType = "B+",
    emergencyContact = "+91 98300 11223",
    contactName = "Priya (daughter)",
    allergies = "Penicillin. Peanuts.",
    age = 74,
    conditions = "Type 2 diabetes. Pacemaker.",
    medications = "Metformin 500mg twice daily",
    secondaryContactName = "Dr Sen",
    secondaryContact = "+91 98300 44556",
    organDonor = true,
    notes = "Hard of hearing on the left"
)

@Preview(name = "Medical ID — light", showBackground = true, heightDp = 2200)
@Composable
private fun MedicalIdLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        MedicalIdScreen(
            state = MedicalIdUiState(
                medicalId = previewMedicalId,
                wearerName = "Baba",
                linkLive = true,
                isDirty = true,
                lastSyncedLabel = "Sent to the device at 4:12 pm"
            ),
            onBack = {}, onChange = {}, onSave = {}, onOpenCard = {}
        )
    }
}

@Preview(
    name = "Medical ID — dark, empty",
    showBackground = true,
    heightDp = 2200,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun MedicalIdDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        MedicalIdScreen(
            state = MedicalIdUiState(),
            onBack = {}, onChange = {}, onSave = {}, onOpenCard = {}
        )
    }
}

/**
 * The large-text check.
 *
 * This is the screen in the bank with the most stacked labels and helper
 * lines, so it is the one where a raised font scale breaks first. Kept as a
 * preview rather than a note in a review checklist.
 *
 * Its reach shrank when the seven rarely-edited fields moved behind "More
 * medical details": a preview cannot open a disclosure, so what renders here
 * is the essentials, the two short notes and the collapsed headers — which is
 * also what a real reader sees on arrival, and therefore still the layout most
 * worth checking. The seven collapsed fields are the same `PlateField` at the
 * same width and have no font-scale behaviour of their own; if that ever stops
 * being true, open the section by hand rather than trusting this.
 */
@Preview(
    name = "Medical ID — 1.3x text",
    showBackground = true,
    heightDp = 2600,
    fontScale = 1.3f
)
@Composable
private fun MedicalIdLargeTextPreview() {
    SafeShadeTheme(darkTheme = false) {
        MedicalIdScreen(
            state = MedicalIdUiState(medicalId = previewMedicalId, wearerName = "Baba", isDirty = true),
            onBack = {}, onChange = {}, onSave = {}, onOpenCard = {}
        )
    }
}
