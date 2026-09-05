package com.safeshade.ui.screens.safety

import com.safeshade.platform.IndianPhoneTransformation
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.MedicalId
import com.safeshade.ui.board.BLOOD_GROUP_SUGGESTIONS
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.ChipRow
import com.safeshade.ui.board.ExpandableSection
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.WhyDisclosure
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

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
    val subject = state.wearerName.ifBlank { "the wearer" }

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
            title = "Medical ID",
            subtitle = "What a paramedic reads off the device screen.",
            onBack = onBack
        )

        // No Spacer here: CompletenessPlate is the first thing on this screen,
        // and PanelHeader already supplies the whole header-to-content gap.
        // A Spacing.lg on top of that was a third gap stacked on the header's
        // own 20dp, not a separator between two pieces of content.
        CompletenessPlate(medicalId = id)

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "The essentials")
        Spacer(Modifier.height(Spacing.md))

        PlateField(
            label = "Blood type",
            value = id.bloodType,
            onValueChange = { onChange(id.copy(bloodType = stripDeviceDelimiters(it))) },
            placeholder = "B+",
            // The eight groups used to be listed here because there was
            // nothing else to list them. The chips below are that list now,
            // and one tappable, so repeating them in the helper would be the
            // same eight strings said twice.
            maxLength = MAX_BLOOD_TYPE,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.sm))

        // The box stays: a wearer whose record says "O+ (Rh null)" or who
        // knows only "O" can still type it, and neither lights a chip, which
        // is correct rather than an error. What the row removes is spelling a
        // blood group on a phone keyboard, where a slip between B+ and B- is
        // both easy and the worst typo on this card.
        ChipRow(
            options = BLOOD_GROUP_SUGGESTIONS,
            selected = id.bloodType,
            onSelect = { onChange(id.copy(bloodType = it)) }
        )

        Spacer(Modifier.height(Spacing.lg))

        // The comma warning sits above the first free-text field rather than
        // being repeated under each one. The short line is the whole of what
        // somebody in the middle of typing needs; why it matters is worth
        // having and is not worth four lines above the box on every visit.
        Note(text = "Commas are removed from the text boxes as you type.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "Why commas are removed",
            text = "The device splits the record on commas with no escaping, so a single one " +
                "in Allergies would shift age into Conditions and everything after it, and " +
                "produce a wrong card rather than an error. Separate several items with a " +
                "space, a full stop or the word \"and\"."
        )

        Spacer(Modifier.height(Spacing.md))

        PlateField(
            label = "Allergies",
            value = id.allergies,
            onValueChange = { onChange(id.copy(allergies = stripDeviceDelimiters(it))) },
            placeholder = "Penicillin. Peanuts.",
            maxLength = MAX_FREE_TEXT,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.lg))

        Note(text = "The first contact is printed on the card and shown on the device.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "How this differs from the call list",
            text = "The contacts here are printed for a human being to read and ring " +
                "themselves. They are separate from the emergency contact list the phone " +
                "dials automatically after a fall – the same person usually belongs in both, " +
                "and filling one in does not fill in the other."
        )

        Spacer(Modifier.height(Spacing.md))

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

        Spacer(Modifier.height(Spacing.xl))

        // Seven real fields, none of them the reason anybody opens this screen
        // a second time. A card carrying a blood type, an allergy and one
        // number is already worth showing a responder — `isUsable` says so —
        // and the rest is what a guardian fills in once, on the evening they
        // set the device up, and then leaves alone. Twelve stacked text boxes
        // are also the thing that made this screen read as a form to be
        // completed rather than a card to be got right.
        ExpandableSection(label = "More medical details", count = 7) {
            Spacer(Modifier.height(Spacing.sm))

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

            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Organ donor",
                    state = if (id.organDonor) LampState.LIVE else LampState.OFF,
                    stateLabel = if (id.organDonor) "Yes" else "Not stated",
                    checked = id.organDonor,
                    onCheckedChange = { onChange(id.copy(organDonor = it)) }
                )
            }

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

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = if (state.linkLive) "Save and Send to the Device" else "Save",
            supporting = if (state.linkLive) null else "The device is not connected. This is kept and sent when it is.",
            onClick = onSave,
            enabled = state.isDirty,
            weight = ButtonWeight.COMMIT,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.lastSyncedLabel != null) {
            Spacer(Modifier.height(Spacing.sm))
            Note(text = state.lastSyncedLabel)
        }

        Spacer(Modifier.height(Spacing.lg))

        BoardButton(
            label = "Show the emergency card",
            icon = Icons.Outlined.QrCode2,
            onClick = onOpenCard,
            enabled = id.isUsable,
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
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
private fun CompletenessPlate(medicalId: MedicalId) {
    val colors = MaterialTheme.board
    val filled = medicalId.filledFieldCount
    val progress = filled / 11f

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$filled of 11 filled in",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = if (medicalId.isUsable) "Worth showing" else "Not usable yet",
                    style = MaterialTheme.boardType.stateLabel,
                    color = if (medicalId.isUsable) colors.inkLive else colors.inkAttention
                )
            }
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
