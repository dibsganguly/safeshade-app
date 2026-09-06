package com.safeshade.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.MedicalId
import com.safeshade.data.UserRole
import com.safeshade.device.ConnectionState
import com.safeshade.platform.IndianPhoneTransformation
import com.safeshade.ui.board.Avatar
import com.safeshade.ui.board.AvatarCustomiser
import com.safeshade.ui.board.AvatarPresetStrip
import com.safeshade.ui.board.AvatarSpec
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.accentFor
import com.safeshade.ui.screens.safety.PlateField
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/**
 * First run.
 *
 * Six steps, in an order chosen so the app never has to ask a question it
 * could have inferred, and never speaks in the wrong voice:
 *
 *  0. Welcome — Shady walks on and lights the board. One button.
 *  1. Role — who is this for. Everything downstream is worded from the answer.
 *  2. Who wears it — the name that appears on the board from then on, and a
 *     face for them.
 *  3. Permissions — asked in context, with the reason stated before the dialog.
 *  4. Pair — find the device.
 *  5. Medical — the minimum that makes the emergency card worth showing.
 *
 * Every step is the same shape: a scene at the top in which the character
 * does the thing the step is about, a progress rail, a headline, a line of
 * body copy at most, the controls, and the actions pinned at the foot. The
 * scene is the only decoration in the flow; it earns its place because a
 * first run is the one time the app is allowed to charm.
 *
 * Permissions come after the role and the name rather than at launch, because
 * a system dialog fired before a user knows what the app is gets denied, and a
 * denied Bluetooth permission makes the whole product inert.
 */
const val ONBOARDING_STEPS = 5

/**
 * Onboarding progress, as bus ticks rather than a line of text.
 *
 * Six short rules across the width, the ones behind lit in ink. It reads at
 * a glance and it is the same mark that carries state on every way row.
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

/**
 * Chrome shared by every step.
 *
 * The scene sits above the status bar inset so it runs to the top edge; the
 * content scrolls between the rail and the actions; the actions stay put at
 * the foot and rise with the keyboard, so Continue is never behind it.
 */
@Composable
private fun OnboardingStep(
    stepIndex: Int,
    scene: @Composable () -> Unit,
    title: String,
    body: String?,
    onContinue: (() -> Unit)?,
    continueLabel: String = "Continue",
    continueEnabled: Boolean = true,
    continueWeight: ButtonWeight = ButtonWeight.PRIMARY,
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
    ) {
        scene()
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(Spacing.lg))
            StepProgress(step = stepIndex, total = ONBOARDING_STEPS, modifier = Modifier.padding(horizontal = Spacing.gutter))
            Spacer(Modifier.height(Spacing.lg))
            Text(
                title,
                style = MaterialTheme.typography.headlineLarge,
                color = colors.ink,
                modifier = Modifier.padding(horizontal = Spacing.gutter)
            )
            if (body != null) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = Spacing.gutter)
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Column(content = content)
            Spacer(Modifier.height(Spacing.lg))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm, bottom = Spacing.lg)
        ) {
            if (onContinue != null) {
                BoardButton(
                    label = continueLabel,
                    onClick = onContinue,
                    enabled = continueEnabled,
                    weight = continueWeight,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (onSkip != null) {
                Spacer(Modifier.height(Spacing.xs))
                TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                    Text(skipLabel, style = MaterialTheme.typography.bodyMedium, color = colors.inkMuted)
                }
            }
        }
    }
}

// ============================================
// Step 0 — welcome
// ============================================

@Composable
fun WelcomeScreen(onStart: () -> Unit, modifier: Modifier = Modifier) {
    OnboardingStep(
        stepIndex = 0,
        scene = { OnboardingScene(OnboardingSceneKind.WELCOME, height = 220.dp) },
        title = "Welcome to SafeShade",
        body = "A wearable that notices when someone falls, and a phone that does " +
            "something about it. Setting up takes about two minutes.",
        onContinue = onStart,
        continueLabel = "Set Up",
        continueWeight = ButtonWeight.ATTENTION,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                FeatureRow(SafeShadeIcons.FallDetection, "Falls, noticed", "The wearable listens for an impact and calls for help by itself.")
                Hairline()
                FeatureRow(SafeShadeIcons.SafeZone, "Location and safe zones", "Where they are, and a word when they leave somewhere they should be.")
                Hairline()
                FeatureRow(SafeShadeIcons.NavbarCircle, "Your Circle", "Messages, check-ins and a medical card the people who matter can reach.")
            }
        }
    }
}

/** A feature statement: an icon in its accent, a title, a line. Not a circuit, so no bus tick and no lamp. */
@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md)
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accentFor(title),
            modifier = Modifier.size(22.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = colors.inkMuted)
        }
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
    avatarId: String,
    onAvatarChange: (String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    var changes by rememberSaveable { mutableStateOf(0) }
    var customising by rememberSaveable { mutableStateOf(false) }
    val spec = remember(avatarId) { AvatarSpec.decode(avatarId) ?: AvatarSpec.PRESETS.first() }
    val guardian = role == UserRole.GUARDIAN

    OnboardingStep(
        stepIndex = 2,
        scene = { OnboardingScene(OnboardingSceneKind.WEARER, progress = changes.toFloat()) },
        title = if (guardian) "Who wears it?" else "What should we call you?",
        body = if (guardian) "Their name is on the board from here on, and on the card a responder would see."
        else "Your name goes on the emergency card a responder would see.",
        onContinue = onContinue,
        continueEnabled = wearerName.isNotBlank(),
        onSkip = onContinue,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.gutter)
        ) {
            Avatar(avatarId = avatarId, name = wearerName, size = 64.dp)
            Spacer(Modifier.width(Spacing.lg))
            PlateField(
                label = if (guardian) "Their name" else "Your name",
                value = wearerName,
                onValueChange = onWearerNameChange,
                placeholder = if (guardian) "Baba" else "Priya",
                imeAction = ImeAction.Done,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(Spacing.xl))
        Nameplate("A face for them", small = true, muted = true, modifier = Modifier.padding(horizontal = Spacing.gutter))
        Spacer(Modifier.height(Spacing.sm))
        AvatarPresetStrip(
            selectedId = avatarId,
            onSelect = { onAvatarChange(it); changes++ }
        )
        Spacer(Modifier.height(Spacing.xs))
        TextButton(
            onClick = { customising = !customising },
            modifier = Modifier.padding(horizontal = Spacing.sm)
        ) {
            Text(
                if (customising) "Done making one" else "Make your own face",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkAttention
            )
        }
        AnimatedVisibility(visible = customising) {
            Column {
                Spacer(Modifier.height(Spacing.sm))
                AvatarCustomiser(spec = spec, onChange = { onAvatarChange(it.encode()); changes++ })
            }
        }
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
    val granted = listOf(bluetoothGranted, locationGranted, notificationsGranted).count { it }
    OnboardingStep(
        stepIndex = 3,
        scene = { OnboardingScene(OnboardingSceneKind.PERMISSIONS, progress = granted.toFloat()) },
        title = "Three permissions",
        body = "Each one has a job. None is used for advertising.",
        onContinue = onContinue,
        // Always enabled. A button labelled "Continue anyway" that cannot be
        // pressed strands anyone who declines, and permissions can be granted
        // later from the Profile page - onboarding is not the last chance.
        continueEnabled = true,
        continueLabel = if (bluetoothGranted) "Continue" else "Continue Anyway",
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Way(
                    name = "Bluetooth",
                    state = if (bluetoothGranted) LampState.LIVE else LampState.OFF,
                    stateLabel = if (bluetoothGranted) "Granted" else "Needed",
                    detail = "Finds and talks to the wearable. Without it nothing else works."
                )
                Way(
                    name = "Location",
                    state = if (locationGranted) LampState.LIVE else LampState.OFF,
                    stateLabel = if (locationGranted) "Granted" else "Needed",
                    detail = "Weather on the device and safe-zone alerts. Scanning never derives your position."
                )
                Way(
                    name = "Notifications",
                    state = if (notificationsGranted) LampState.LIVE else LampState.ATTENTION,
                    stateLabel = if (notificationsGranted) "Granted" else "Recommended",
                    detail = "Without this you will not hear about a fall while the app is closed."
                )
            }
            Spacer(Modifier.height(Spacing.lg))
            BoardButton(
                label = if (granted == 3) "All Granted" else "Grant Permissions",
                onClick = onRequest,
                enabled = granted < 3,
                weight = ButtonWeight.ATTENTION,
                modifier = Modifier.fillMaxWidth()
            )
        }
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
        scene = { OnboardingScene(OnboardingSceneKind.PAIR, progress = if (paired) 1f else 0f) },
        title = if (paired) "Paired" else "Find the wearable",
        body = if (paired) "Connected to $deviceName. Everything you set from here reaches it."
        else "Switch it on and keep it within a few metres. Pairing takes a few seconds.",
        onContinue = onContinue,
        continueLabel = if (paired) "Continue" else "Do This Later",
        continueWeight = if (paired) ButtonWeight.PRIMARY else ButtonWeight.SECONDARY,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    PilotLamp(
                        state = when {
                            paired -> LampState.LIVE
                            connection.isBusy -> LampState.ATTENTION
                            else -> LampState.OFF
                        }
                    )
                    Spacer(Modifier.width(Spacing.md))
                    Column {
                        Text(
                            text = when (connection) {
                                is ConnectionState.Ready -> "Connected"
                                is ConnectionState.Scanning -> "Searching"
                                is ConnectionState.Connecting -> "Connecting"
                                is ConnectionState.Connected -> "Starting up"
                                is ConnectionState.BluetoothUnavailable -> "Bluetooth is off"
                                is ConnectionState.ScanFailed -> "Search failed"
                                else -> "Not connected"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.ink
                        )
                        Text(
                            text = if (paired) deviceName else "Looking for a SafeShade wearable",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkMuted
                        )
                    }
                }
            }
            if (!paired) {
                Spacer(Modifier.height(Spacing.lg))
                BoardButton(
                    label = if (connection.isBusy) "Searching" else "Search for the Wearable",
                    onClick = onScan,
                    enabled = !connection.isBusy,
                    weight = ButtonWeight.ATTENTION,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
        scene = { OnboardingScene(OnboardingSceneKind.CARD) },
        title = "The emergency card",
        body = "Shown on the wearable's screen, and readable from a QR code by any phone. The rest can wait.",
        onContinue = onFinish,
        continueLabel = "Finish",
        continueWeight = ButtonWeight.COMMIT,
        onSkip = onFinish,
        skipLabel = "Fill this in later",
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.gutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            PlateField(
                label = "Blood type",
                value = medicalId.bloodType,
                onValueChange = onBloodTypeChange,
                placeholder = "O+",
                maxLength = 3
            )
            PlateField(
                label = "Emergency contact - name",
                value = medicalId.contactName,
                onValueChange = onContactNameChange,
                placeholder = "Moumita"
            )
            PlateField(
                label = "Emergency contact - number",
                value = medicalId.emergencyContact,
                onValueChange = onContactNumberChange,
                placeholder = "+91 98765 43210",
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done,
                visualTransformation = IndianPhoneTransformation()
            )
        }
    }
}

// ============================================
// Previews
// ============================================

@Preview(name = "Welcome", showBackground = true)
@Composable
private fun WelcomePreview() = SafeShadeTheme { WelcomeScreen(onStart = {}) }

@Preview(name = "Wearer", showBackground = true)
@Composable
private fun WearerPreview() = SafeShadeTheme {
    WearerScreen(role = UserRole.GUARDIAN, wearerName = "Baba", onWearerNameChange = {}, avatarId = AvatarSpec.PRESETS[2].encode(), onAvatarChange = {}, onContinue = {})
}

@Preview(name = "Permissions", showBackground = true)
@Composable
private fun PermissionsPreview() = SafeShadeTheme {
    PermissionsScreen(bluetoothGranted = true, locationGranted = false, notificationsGranted = false, onRequest = {}, onContinue = {})
}

@Preview(name = "Pair · dark", showBackground = true)
@Composable
private fun PairPreview() = SafeShadeTheme(darkTheme = true) {
    PairScreen(connection = ConnectionState.Scanning, deviceName = "SafeShade S1", onScan = {}, onContinue = {})
}

@Preview(name = "Card", showBackground = true)
@Composable
private fun CardPreview() = SafeShadeTheme {
    MedicalStartScreen(medicalId = MedicalId(), onBloodTypeChange = {}, onContactNameChange = {}, onContactNumberChange = {}, onFinish = {})
}
