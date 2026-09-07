package com.safeshade.ui.screens.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.EmergencyContact
import com.safeshade.data.EscalationSettings
import com.safeshade.service.EscalationLadder
import com.safeshade.service.EscalationRun
import com.safeshade.service.EscalationTarget
import com.safeshade.service.StepOutcome
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.board.WhyDisclosure
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/** Everything the ladder page draws. */
data class EscalationUiState(
    val settings: EscalationSettings = EscalationSettings(),
    /** The contacts the ladder would climb, in order. */
    val contacts: List<EmergencyContact> = emptyList(),
    val wearerName: String = "",
    /** Whether "Call after a fall" lets a contact be rung without a press. */
    val directCallsAllowed: Boolean = false,
    /** The ladder climbing right now, when an alert is open. */
    val run: EscalationRun? = null
)

/**
 * If nobody answers: the ladder of calls behind an unanswered alert.
 *
 * The page is a preview of the machine before it runs and a record of it
 * while it does. The preview is built by the same `EscalationLadder.plan`
 * the runner uses, from the real contacts and the settings as they stand,
 * so what the page shows is what would happen and not a sentence about it.
 * Every control writes straight through; there is no save button, in line
 * with the fall settings page next door.
 *
 * Two facts are stated on the page rather than left to be discovered: the
 * emergency number is never rung by itself (the dialer opens, a person
 * presses), and a phone with no contacts stored dials nothing at all,
 * including that number.
 */
@Composable
fun EscalationScreen(
    state: EscalationUiState,
    onChange: (EscalationSettings) -> Unit,
    onOpenContacts: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val settings = state.settings
    val subject = state.wearerName.ifBlank { "the wearer" }
    val usable = state.contacts.filter { it.phone.isNotBlank() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            )
    ) {
        ScreenHeader(
            title = "If nobody answers",
            subtitle = "Who is called, in what order, when an alert about $subject stays open",
            onBack = onBack
        )

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Keep calling",
                state = if (settings.enabled) LampState.LIVE else LampState.OFF,
                stateLabel = if (settings.enabled) "On" else "Off",
                detail = if (settings.enabled) {
                    "Each person in turn, until the trip is closed"
                } else {
                    "Only the one call after a fall's countdown"
                },
                icon = SafeShadeIcons.CallAfterAFall,
                checked = settings.enabled,
                onCheckedChange = { onChange(settings.copy(enabled = it)) }
            )
        }

        val run = state.run
        if (run != null) {
            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "Right now")
            Spacer(Modifier.height(Spacing.sm))
            EscalationPlate(run = run)
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "The ladder")
        Spacer(Modifier.height(Spacing.sm))
        if (usable.isEmpty()) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Nobody to call",
                    state = LampState.TRIP,
                    stateLabel = "Empty",
                    detail = "${EscalationLadder.NO_CONTACTS}. With no contact the phone dials nothing, not even ${settings.emergencyNumber}.",
                    icon = SafeShadeIcons.EmergencyContacts
                )
                Hairline()
                Way(
                    name = "Add a contact",
                    state = LampState.OFF,
                    stateLabel = "Go",
                    icon = SafeShadeIcons.UserAdd,
                    onClick = onOpenContacts
                )
            }
        } else {
            // The preview is the real plan from the alert's own instant, so
            // each rung's dueAt is its offset from the alert.
            val preview = remember(usable, settings) {
                EscalationLadder.plan("preview", null, usable, settings, startedAt = 0L)
            }
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                preview.steps.forEachIndexed { index, step ->
                    if (index > 0) Hairline()
                    val gap = if (index == 0) step.dueAt else step.dueAt - preview.steps[index - 1].dueAt
                    val when_ = if (index == 0) {
                        "${formatDuration((gap / 1000L).toInt())} after the alert"
                    } else {
                        "${formatDuration((gap / 1000L).toInt())} after that"
                    }
                    val how = when (step.target) {
                        is EscalationTarget.Contact ->
                            if (state.directCallsAllowed) "called without a press" else "the dialer opens"
                        is EscalationTarget.Emergency -> "the dialer opens, never called by itself"
                    }
                    Way(
                        name = rungName(step.target),
                        state = if (settings.enabled) LampState.LIVE else LampState.OFF,
                        stateLabel = "${index + 1}",
                        detail = if (step.outcome is StepOutcome.Skipped) {
                            (step.outcome as StepOutcome.Skipped).reason
                        } else {
                            "$when_ · $how"
                        },
                        icon = SafeShadeIcons.CallAfterAFall
                    )
                }
            }
            if (usable.size == 1) {
                Spacer(Modifier.height(Spacing.sm))
                Note(text = "One contact stored. A second would be tried before the emergency number.")
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Timing")
        Spacer(Modifier.height(Spacing.sm))
        var firstDraft by remember(settings.firstDelaySec) { mutableFloatStateOf(settings.firstDelaySec.toFloat()) }
        DialControl(
            label = "First call",
            value = firstDraft,
            valueRange = 15f..180f,
            step = 15f,
            onValueChange = { firstDraft = it },
            onCommit = { onChange(settings.copy(firstDelaySec = firstDraft.roundToInt())) },
            unit = "seconds after the alert",
            format = { formatDuration(it.roundToInt()) },
            advice = { firstAdvice(it.roundToInt()) }
        )
        Spacer(Modifier.height(Spacing.md))
        var secondDraft by remember(settings.secondDelaySec) { mutableFloatStateOf(settings.secondDelaySec.toFloat()) }
        DialControl(
            label = "Each next call",
            value = secondDraft,
            valueRange = 30f..300f,
            step = 30f,
            onValueChange = { secondDraft = it },
            onCommit = { onChange(settings.copy(secondDelaySec = secondDraft.roundToInt())) },
            unit = "after the one before",
            format = { formatDuration(it.roundToInt()) }
        )
        Spacer(Modifier.height(Spacing.sm))
        Note(text = "The whole ladder should finish while a fall is still an emergency. Somebody who did not answer in thirty seconds is not more likely to answer in five minutes.")

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "The end of the ladder")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "End at the emergency number",
                state = if (settings.thenEmergency) LampState.LIVE else LampState.OFF,
                stateLabel = if (settings.thenEmergency) "On" else "Off",
                detail = if (settings.thenEmergency) {
                    "After the contacts, the dialer opens with ${settings.emergencyNumber} ready"
                } else {
                    "The ladder stops after the last contact"
                },
                icon = SafeShadeIcons.TelephoneCall,
                checked = settings.thenEmergency,
                onCheckedChange = { onChange(settings.copy(thenEmergency = it)) }
            )
            Hairline()
            Column(modifier = Modifier.padding(Spacing.lg)) {
                PlateField(
                    label = "Emergency number",
                    value = settings.emergencyNumber,
                    onValueChange = { v -> onChange(settings.copy(emergencyNumber = v.filter { it.isDigit() }.take(6))) },
                    placeholder = "112",
                    helper = "112 reaches every service in India. Change it only for another country.",
                    error = if (settings.emergencyNumber.isBlank()) "A number is needed to end the ladder" else null,
                    maxLength = 6,
                    enabled = settings.thenEmergency,
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Note(text = "The emergency number is never called by itself. The dialer opens with it ready and a person presses call.")
        Spacer(Modifier.height(Spacing.xs))
        WhyDisclosure(
            label = "Why the phone will not call ${settings.emergencyNumber} on its own",
            text = "An unattended call to the emergency services, made by a timer, with nobody " +
                "holding the phone and nothing to say, is the one thing here that cannot be taken " +
                "back. Contacts can be rung directly because a person chose them for exactly this. " +
                "The emergency number gets the dialer, one press away for whoever is there."
        )
    }
}

private fun firstAdvice(seconds: Int): String = when {
    seconds <= 30 -> "Close behind the fall countdown. The first person hears within a minute of the fall."
    seconds <= 90 -> "A little room for $seconds seconds of the person answering on their own."
    else -> "Long. The first call comes ${formatDuration(seconds)} after the alert."
}

@Preview(name = "If nobody answers", showBackground = true, heightDp = 1600)
@Composable
private fun EscalationPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            EscalationScreen(
                state = EscalationUiState(
                    settings = EscalationSettings(enabled = true),
                    contacts = listOf(EmergencyContact(name = "Meera", phone = "9876543210")),
                    wearerName = "Baba",
                    directCallsAllowed = true
                ),
                onChange = {}, onOpenContacts = {}, onBack = {}
            )
        }
    }
}
