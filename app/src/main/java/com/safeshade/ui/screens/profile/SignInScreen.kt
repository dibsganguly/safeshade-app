package com.safeshade.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.cloud.CloudResult
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.screens.safety.PlateField
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlinx.coroutines.launch

/**
 * The four ways into an account, and what each one does when it fails.
 *
 * Every action is a suspend call returning the client's own answer, so the
 * screen can hold the button in its "working" state until there is something
 * true to say. `Failed` on a sign-in is the ordinary case, not the exception:
 * a typo in the address, a code typed from the wrong email, no network in a
 * lift. The reason is shown in words, under the control that failed.
 */
data class SignInActions(
    val requestCode: suspend (email: String) -> CloudResult<Unit>,
    val verifyCode: suspend (email: String, code: String) -> CloudResult<Unit>,
    val signInWithPassword: suspend (email: String, password: String) -> CloudResult<Unit>,
    val signUpWithPassword: suspend (email: String, password: String) -> CloudResult<Unit>,
    /** Null when this build has no Google Web client ID; the button is then not drawn at all. */
    val signInWithGoogle: (suspend (activityContext: android.content.Context) -> CloudResult<Unit>)?,
    /** The reason the Apple button reports when tapped. */
    val appleReason: String = APPLE_NOT_SET_UP
)

/**
 * Sign in.
 *
 * A six-digit code is the front door: the guardian is usually reading the
 * email on the same phone, and typing six digits has fewer ways to fail than
 * a link that leaves the app. A password is one expander down for people
 * who prefer one. Google is a single button when the build carries a client
 * ID. Apple is drawn — it is on the launch screen — and reports, in words,
 * why it cannot sign anyone in on this phone.
 *
 * The screen never says "Sent" or "Signed in" on the tap. The button reads
 * "Sending…" until the client answers, then either the next step appears or
 * the reason does.
 */
@Composable
fun SignInScreen(
    actions: SignInActions,
    signedIn: Boolean,
    onSignedIn: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var codeSentTo by rememberSaveable { mutableStateOf<String?>(null) }
    var usePassword by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf<Busy?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var appleError by remember { mutableStateOf<String?>(null) }

    // The session flow is the only thing that may say "signed in". When it
    // flips, this screen leaves; it never pops itself on its own guess.
    LaunchedEffect(signedIn) { if (signedIn) onSignedIn() }

    val emailOk = email.contains('@') && email.substringAfter('@').contains('.')

    fun run(kind: Busy, block: suspend () -> CloudResult<Unit>, onOk: () -> Unit = {}) {
        if (busy != null) return
        busy = kind
        error = null
        scope.launch {
            when (val r = block()) {
                is CloudResult.Ok -> onOk()
                is CloudResult.Failed -> if (r.reason != CANCELLED) error = r.reason
                CloudResult.Disabled -> error = "This build has no SafeShade Cloud project"
            }
            busy = null
        }
    }

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
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ScreenHeader(
            title = "Sign in",
            subtitle = "Your Circle on every phone, and a copy of what matters",
            onBack = onBack
        )

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                PlateField(
                    label = "Email",
                    value = email,
                    onValueChange = { email = it.trim(); if (codeSentTo != null && it.trim() != codeSentTo) codeSentTo = null },
                    placeholder = "you@example.com",
                    maxLength = 120,
                    enabled = busy == null,
                    keyboardType = KeyboardType.Email,
                    imeAction = if (usePassword) ImeAction.Next else ImeAction.Done
                )

                if (!usePassword) {
                    val sentTo = codeSentTo
                    if (sentTo == null) {
                        BoardButton(
                            label = if (busy == Busy.CODE) "Sending…" else "Email Me a Code",
                            onClick = { run(Busy.CODE, { actions.requestCode(email) }) { codeSentTo = email; code = "" } },
                            weight = ButtonWeight.COMMIT,
                            enabled = emailOk && busy == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "A six-digit code is on its way to $sentTo. It is good for an hour.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkMuted
                        )
                        PlateField(
                            label = "Code",
                            value = code,
                            onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            placeholder = "000000",
                            maxLength = 6,
                            enabled = busy == null,
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        )
                        BoardButton(
                            label = if (busy == Busy.VERIFY) "Checking…" else "Sign In",
                            onClick = { run(Busy.VERIFY, { actions.verifyCode(sentTo, code) }) },
                            weight = ButtonWeight.COMMIT,
                            enabled = code.length == 6 && busy == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        BoardButton(
                            label = if (busy == Busy.CODE) "Sending…" else "Send Another Code",
                            onClick = { run(Busy.CODE, { actions.requestCode(sentTo) }) { code = "" } },
                            weight = ButtonWeight.QUIET,
                            enabled = busy == null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    PlateField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "At least eight characters",
                        maxLength = 72,
                        enabled = busy == null,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    BoardButton(
                        label = if (busy == Busy.PASSWORD) "Signing In…" else "Sign In",
                        onClick = { run(Busy.PASSWORD, { actions.signInWithPassword(email, password) }) },
                        weight = ButtonWeight.COMMIT,
                        enabled = emailOk && password.length >= 8 && busy == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BoardButton(
                        label = if (busy == Busy.SIGNUP) "Creating…" else "Create an Account",
                        onClick = { run(Busy.SIGNUP, { actions.signUpWithPassword(email, password) }) },
                        weight = ButtonWeight.SECONDARY,
                        enabled = emailOk && password.length >= 8 && busy == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                val shownError = error
                if (shownError != null) {
                    Text(
                        text = shownError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkTrip
                    )
                }

                Hairline()
                BoardButton(
                    label = if (usePassword) "Use an Emailed Code Instead" else "Use a Password Instead",
                    onClick = { usePassword = !usePassword; error = null },
                    weight = ButtonWeight.QUIET,
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Hairline(modifier = Modifier.weight(1f))
            Spacer(Modifier.width(Spacing.md))
            Nameplate("Or", small = true, muted = true)
            Spacer(Modifier.width(Spacing.md))
            Hairline(modifier = Modifier.weight(1f))
        }

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                val google = actions.signInWithGoogle
                if (google != null) {
                    BoardButton(
                        label = if (busy == Busy.GOOGLE) "Waiting for Google…" else "Continue with Google",
                        onClick = { run(Busy.GOOGLE, { google(context) }) },
                        weight = ButtonWeight.SECONDARY,
                        icon = SafeShadeIcons.GoogleLogo,
                        enabled = busy == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BoardButton(
                    label = "Continue with Apple",
                    onClick = { error = null; appleError = actions.appleReason },
                    weight = ButtonWeight.SECONDARY,
                    icon = SafeShadeIcons.AppleLogo,
                    enabled = busy == null,
                    modifier = Modifier.fillMaxWidth()
                )
                val shownApple = appleError
                if (shownApple != null) {
                    Text(
                        text = shownApple,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkTrip
                    )
                }
            }
        }

        Text(
            text = "Everything keeps working on this phone without an account. Signing in adds a copy on " +
                "SafeShade Cloud and lets the rest of the Circle see the same board.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.inkFaint
        )
        Spacer(Modifier.height(Spacing.sm))
    }
}

/** Which control is waiting on the client; at most one at a time. */
private enum class Busy { CODE, VERIFY, PASSWORD, SIGNUP, GOOGLE }

/** Matches [com.safeshade.ui.vm.CloudViewModel.CANCELLED]; the screen shows nothing for it. */
private const val CANCELLED = "cancelled"

/** The reason the Apple button gives. It is a fact about the account, not about a roadmap. */
const val APPLE_NOT_SET_UP =
    "Sign in with Apple needs SafeShade's Apple Services ID, and this build does not carry one. " +
        "Use an emailed code, a password, or Google."

@Preview(name = "Sign in", showBackground = true)
@Composable
private fun SignInPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            SignInScreen(
                actions = SignInActions(
                    requestCode = { CloudResult.Ok(Unit) },
                    verifyCode = { _, _ -> CloudResult.Ok(Unit) },
                    signInWithPassword = { _, _ -> CloudResult.Ok(Unit) },
                    signUpWithPassword = { _, _ -> CloudResult.Ok(Unit) },
                    signInWithGoogle = { CloudResult.Ok(Unit) }
                ),
                signedIn = false,
                onSignedIn = {},
                onBack = {}
            )
        }
    }
}
