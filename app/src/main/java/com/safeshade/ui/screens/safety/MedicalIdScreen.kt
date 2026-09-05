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
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
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
 * All eleven fields the firmware parses, in the three groups a person actually
 * thinks in rather than the order they cross the wire. Two things about this
 * screen are load-bearing:
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

        Spacer(Modifier.height(Spacing.lg))
        Note(
            text = "Anyone holding the device can bring this up without unlocking anything. " +
                "That is the point — it is meant to be read by a stranger helping $subject. " +
                "Put in what would change how somebody is treated, and leave the rest blank."
        )

        Spacer(Modifier.height(Spacing.lg))
        CompletenessPlate(medicalId = id)

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Identity")
        Spacer(Modifier.height(Spacing.md))

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
            label = "Blood type",
            value = id.bloodType,
            onValueChange = { onChange(id.copy(bloodType = stripDeviceDelimiters(it))) },
            placeholder = "B+",
            helper = "A+, A-, B+, B-, AB+, AB-, O+ or O-. Leave blank if you are not certain.",
            maxLength = MAX_BLOOD_TYPE,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Medical")
        Spacer(Modifier.height(Spacing.sm))

        // The comma warning sits here, above the four free-text fields it
        // applies to, rather than being repeated under each one.
        Note(
            text = "Commas are removed from these boxes as you type. The device splits the " +
                "record on commas, so a single one would shift every later field along and " +
                "produce a wrong card. Separate several items with a space, a full stop or " +
                "the word \"and\"."
        )

        Spacer(Modifier.height(Spacing.md))

        PlateField(
            label = "Allergies",
            value = id.allergies,
            onValueChange = { onChange(id.copy(allergies = stripDeviceDelimiters(it))) },
            placeholder = "Penicillin. Peanuts.",
            helper = "The single most useful line on this card.",
            maxLength = MAX_FREE_TEXT,
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
                detail = "Printed on the card only when this is on.",
                checked = id.organDonor,
                onCheckedChange = { onChange(id.copy(organDonor = it)) }
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Contacts")
        Spacer(Modifier.height(Spacing.sm))
        Note(
            text = "These two are printed on the card and shown on the device. They are separate " +
                "from the emergency contact list that gets called automatically — fill both in."
        )
        Spacer(Modifier.height(Spacing.md))

        PlateField(
            label = "First contact — name",
            value = id.contactName,
            onValueChange = { onChange(id.copy(contactName = stripDeviceDelimiters(it))) },
            placeholder = "Priya (daughter)",
            maxLength = MAX_CONTACT_NAME,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.lg))

        PlateField(
            label = "First contact — number",
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

        Spacer(Modifier.height(Spacing.lg))

        PlateField(
            label = "Second contact — name",
            value = id.secondaryContactName,
            onValueChange = { onChange(id.copy(secondaryContactName = stripDeviceDelimiters(it))) },
            placeholder = "Dr Sen",
            maxLength = MAX_CONTACT_NAME,
            imeAction = ImeAction.Next
        )

        Spacer(Modifier.height(Spacing.lg))

        PlateField(
            label = "Second contact — number",
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

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = if (state.linkLive) "Save and send to the device" else "Save",
            supporting = if (state.linkLive) null else "The device is not connected. This is kept and sent when it is.",
            onClick = onSave,
            enabled = state.isDirty,
            weight = ButtonWeight.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (state.lastSyncedLabel != null) {
            Spacer(Modifier.height(Spacing.sm))
            Note(text = state.lastSyncedLabel)
        }

        Spacer(Modifier.height(Spacing.lg))

        BoardButton(
            label = "Show the emergency card",
            supporting = "A QR code any phone camera can read.",
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
