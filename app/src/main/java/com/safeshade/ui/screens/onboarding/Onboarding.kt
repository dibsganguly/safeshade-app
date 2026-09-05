package com.safeshade.ui.screens.onboarding

import com.safeshade.platform.IndianPhoneTransformation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.R
import com.safeshade.data.MedicalId
import com.safeshade.data.UserRole
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Way
import com.safeshade.ui.shady.Shady
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.board

/**
 * First run.
 *
 * Five steps, in an order chosen so the app never has to ask a question it
 * could have inferred, and never speaks in the wrong voice:
 *
 *  1. Role — who is this for. Everything downstream is worded from the answer.
 *  2. Who wears it — the name that appears on the board from then on.
 *  3. Permissions — asked in context, with the reason stated before the dialog.
 *  4. Pair — find the device.
 *  5. Medical — the minimum that makes the emergency card worth showing.
 *
 * Permissions come after the role and the name rather than at launch, because
 * a system dialog fired before a user knows what the app is gets denied, and a
 * denied Bluetooth permission makes the whole product inert.
 */


/**
 * Onboarding progress, as a bar rather than a line of text.
 *
 * This replaces a "STEP 2 OF 5" label sitting above each heading. The
 * information is genuinely useful — people want to know how long this will take
 * — but rendered as a small tracked label above a title it read as a
 * decorative kicker, which is the one piece of chrome this system does not
 * allow itself. A segmented bar says the same thing, faster, without competing
 * with the question being asked.
 */
@Composable
fun StepProgress(
    step: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Step $step of $total" },
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(Stroke.heavy)
                    .clip(RoundedCornerShape(Radius.tight))
                    .background(if (index < step) colors.ink else colors.hairline)
            )
        }
    }
}

/** Chrome shared by every step: step counter, title, body, and the action row. */
@Composable
private fun OnboardingStep(
    stepIndex: Int,
    totalSteps: Int,
    title: String,
    body: String?,
    onContinue: (() -> Unit)?,
    continueLabel: String = "Continue",
    continueEnabled: Boolean = true,
    onSkip: (() -> Unit)? = null,
    skipLabel: String = "Skip for now",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(Spacing.gutter)
    ) {
        Spacer(Modifier.height(Spacing.xl))
        StepProgress(step = stepIndex, total = totalSteps)
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.displaySmall, color = colors.ink)
        if (body != null) {
            Spacer(Modifier.height(Spacing.sm))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
        }
        Spacer(Modifier.height(Spacing.xl))

        Column(content = content)

        Spacer(Modifier.height(Spacing.xl))
        if (onContinue != null) {
            BoardButton(
                label = continueLabel,
                onClick = onContinue,
                enabled = continueEnabled,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (onSkip != null) {
            Spacer(Modifier.height(Spacing.sm))
            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(skipLabel, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
            }
        }
        Spacer(Modifier.height(Spacing.xl))
    }
}

// ============================================
// Step 0 — welcome
// ============================================

@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .padding(Spacing.gutter),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.splash_emblem),
            contentDescription = null,
            // Height, not size. The drawable is the mark itself now rather
            // than a mark floating inside a square of transparency, so a square
            // box would reserve 96dp of width for a 63dp-wide image and centre
            // the emblem inside its own padding. The same correction the Board
            // masthead and the intro already carry.
            modifier = Modifier.height(96.dp)
        )
        Spacer(Modifier.height(Spacing.lg))
        Text("SafeShade", style = MaterialTheme.typography.displayLarge, color = colors.ink)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Your everything safety companion",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.inkMuted
        )

        Spacer(Modifier.height(Spacing.xxl))
        // Shady introduces itself here and nowhere else in onboarding. A mascot
        // on every step would compete with the questions being asked.
        Shady(mood = ShadyMood.WATCHING, size = 120.dp)
        Spacer(Modifier.height(Spacing.xxl))

        Text(
            "This app pairs with your SafeShade device over Bluetooth. Nothing is sent to a server – alerts travel straight between the device and this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted
        )
        Spacer(Modifier.height(Spacing.xl))
        BoardButton("Set up", onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

// ============================================
// Step 2 — who wears it
// ============================================

@Composable
fun WearerScreen(
    role: UserRole,
    wearerName: String,
    onWearerNameChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStep(
        stepIndex = 2,
        totalSteps = 5,
        title = if (role == UserRole.GUARDIAN) "Who wears it?" else "What should we call you?",
        body = if (role == UserRole.GUARDIAN) {
            null
        } else {
            "Used on the emergency card a responder would see."
        },
        onContinue = onContinue,
        continueEnabled = wearerName.isNotBlank(),
        onSkip = onContinue,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = wearerName,
            onValueChange = onWearerNameChange,
            label = { Text(if (role == UserRole.GUARDIAN) "Their name" else "Your name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================
// Step 3 — permissions
// ============================================

@Composable
fun PermissionsScreen(
    bluetoothGranted: Boolean,
    locationGranted: Boolean,
    notificationsGranted: Boolean,
    onRequest: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStep(
        stepIndex = 3,
        totalSteps = 5,
        title = "Three permissions",
        body = "Each one is here for a specific job. Nothing is used for advertising and nothing leaves your phone.",
        onContinue = onContinue,
        // Always enabled. A button labelled "Continue anyway" that cannot be
        // pressed strands anyone who declines, and permissions can be granted
        // later from Settings - onboarding is not the last chance.
        continueEnabled = true,
        continueLabel = if (bluetoothGranted) "Continue" else "Continue anyway",
        modifier = modifier
    ) {
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Bluetooth",
                state = if (bluetoothGranted) LampState.LIVE else LampState.OFF,
                stateLabel = if (bluetoothGranted) "Granted" else "Needed",
                detail = "Finds and talks to the device. Without it nothing else works."
            )
            Way(
                name = "Location",
                state = if (locationGranted) LampState.LIVE else LampState.OFF,
                stateLabel = if (locationGranted) "Granted" else "Needed",
                detail = "Weather sync and safe-zone alerts. Bluetooth scanning here never derives your position."
            )
            Way(
                name = "Notifications",
                state = if (notificationsGranted) LampState.LIVE else LampState.ATTENTION,
                stateLabel = if (notificationsGranted) "Granted" else "Recommended",
                detail = "Without this you will not be told about a fall while the app is closed."
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        BoardButton(
            label = "Grant permissions",
            onClick = onRequest,
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================
// Step 4 — pair
// ============================================

@Composable
fun PairScreen(
    connection: ConnectionState,
    deviceName: String,
    onScan: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val paired = connection is ConnectionState.Ready

    OnboardingStep(
        stepIndex = 4,
        totalSteps = 5,
        title = if (paired) "Paired" else "Find the device",
        body = if (paired) {
            "Connected to $deviceName."
        } else {
            "Switch the device on and keep it nearby. Pairing takes a few seconds."
        },
        onContinue = onContinue,
        continueLabel = if (paired) "Continue" else "Do this later",
        modifier = modifier
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Shady(
                mood = if (paired) ShadyMood.WATCHING else ShadyMood.SEARCHING,
                size = 72.dp
            )
            Spacer(Modifier.width(Spacing.lg))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PilotLamp(state = if (paired) LampState.LIVE else LampState.OFF)
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = when (connection) {
                            is ConnectionState.Ready -> "Connected"
                            is ConnectionState.Scanning -> "Searching…"
                            is ConnectionState.Connecting -> "Connecting…"
                            is ConnectionState.Connected -> "Starting up…"
                            is ConnectionState.BluetoothUnavailable -> "Bluetooth is off"
                            is ConnectionState.ScanFailed -> "Search failed"
                            else -> "Not connected"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                }
            }
        }
        Spacer(Modifier.height(Spacing.lg))
        if (!paired) {
            BoardButton(
                label = if (connection.isBusy) "Searching…" else "Search for the device",
                onClick = onScan,
                enabled = !connection.isBusy,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ============================================
// Step 5 — the minimum medical card
// ============================================

@Composable
fun MedicalStartScreen(
    medicalId: MedicalId,
    onBloodTypeChange: (String) -> Unit,
    onContactNameChange: (String) -> Unit,
    onContactNumberChange: (String) -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStep(
        stepIndex = 5,
        totalSteps = 5,
        title = "The emergency card",
        body = "Shown on the device's screen, and readable from a QR code by any phone. You can fill in the rest later.",
        onContinue = onFinish,
        continueLabel = "Finish",
        onSkip = onFinish,
        skipLabel = "Fill this in later",
        modifier = modifier
    ) {
        OutlinedTextField(
            value = medicalId.bloodType,
            onValueChange = onBloodTypeChange,
            label = { Text("Blood type") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = medicalId.contactName,
            onValueChange = onContactNameChange,
            label = { Text("Emergency contact name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = medicalId.emergencyContact,
            onValueChange = onContactNumberChange,
            label = { Text("Emergency contact number") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            visualTransformation = IndianPhoneTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============================================
// Previews
// ============================================

@Preview(name = "Welcome", showBackground = true)
@Composable
private fun WelcomePreview() = SafeShadeTheme { WelcomeScreen(onStart = {}) }

@Preview(name = "Role fork", showBackground = true)
@Composable
private fun RolePreview() = SafeShadeTheme {
    RoleForkScreen(selected = UserRole.GUARDIAN, onSelect = {}, onContinue = {})
}

@Preview(name = "Role fork — dark", showBackground = true)
@Composable
private fun RoleDarkPreview() = SafeShadeTheme(darkTheme = true) {
    RoleForkScreen(selected = null, onSelect = {}, onContinue = {})
}

@Preview(name = "Permissions", showBackground = true)
@Composable
private fun PermissionsPreview() = SafeShadeTheme {
    PermissionsScreen(
        bluetoothGranted = true,
        locationGranted = false,
        notificationsGranted = false,
        onRequest = {},
        onContinue = {}
    )
}

@Preview(name = "Pair", showBackground = true)
@Composable
private fun PairPreview() = SafeShadeTheme {
    PairScreen(
        connection = ConnectionState.Scanning,
        deviceName = "SafeShade S1",
        onScan = {},
        onContinue = {}
    )
}
