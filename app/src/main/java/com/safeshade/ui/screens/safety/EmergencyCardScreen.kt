package com.safeshade.ui.screens.safety

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ActionResult
import com.safeshade.data.MedicalId
import com.safeshade.platform.QrCodec
import com.safeshade.shareText
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Everything the emergency card draws. */
data class EmergencyCardUiState(
    val medicalId: MedicalId = MedicalId(),
    val wearerName: String = ""
)

/**
 * The card you hold up to a stranger.
 *
 * The whole design follows from one situation: someone is on the ground, the
 * person helping them is not you, and the only phone available is theirs. So
 * the payload is plain text rather than a link — every phone camera shows the
 * decoded text of a plain-text QR inline, with no app, no account, no network
 * and no tap on a URL from a stranger's screen.
 *
 * Three consequences that look like small details and are not:
 *
 *  - **The code is drawn black on white regardless of the theme.** Scanners
 *    need that contrast, and a bone or charcoal ground behind a QR is how a
 *    code becomes unreadable at an angle in bad light. This is the one place
 *    in the app where a colour is not a theme token.
 *  - **Brightness is forced to maximum while this screen is up**, and put back
 *    on the way out. A screen dimmed by the battery saver is a code that does
 *    not scan.
 *  - **The same information is printed underneath in words.** A camera that
 *    will not focus, a cracked screen, or a responder who does not think to
 *    scan should not cost anybody their blood type.
 */
@Composable
fun EmergencyCardScreen(
    state: EmergencyCardUiState,
    onBack: () -> Unit,
    onOpenMedicalId: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    val inInspection = LocalInspectionMode.current

    // A failed share is worth showing. `shareText` returns a result precisely
    // so it does not have to fail into a log line nobody reads; this is a
    // property of the last tap, so it lives here rather than in the UiState.
    var shareFailure by remember { mutableStateOf<String?>(null) }

    ForceMaxBrightness(enabled = !inInspection)

    val cardText = remember(state.medicalId, state.wearerName) {
        QrCodec.medicalCardText(state.medicalId, state.wearerName)
    }

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
            title = "Emergency card",
            subtitle = "Any phone camera can read this. No app, no internet.",
            onBack = onBack
        )

        Spacer(Modifier.height(Spacing.xl))

        if (!state.medicalId.isUsable) {
            // Nothing worth encoding, so nothing is encoded. A QR that decodes
            // to a header and no details is worse than no card at all — it
            // reads as "we checked, there is nothing wrong with this person".
            EmptyBay(
                message = "There is nothing on the card yet. Add a blood type, an allergy or a " +
                    "contact number and it becomes something a responder can use.",
                actionLabel = "Fill in the medical ID",
                onAction = onOpenMedicalId
            )
            return@Column
        }

        QrPlate(text = cardText, inInspection = inInspection)

        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Hold this up to a phone camera. The details appear as plain text.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.xl))

        // The same content in words, for when the camera will not cooperate.
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = cardText,
                style = MaterialTheme.boardType.readout,
                color = colors.ink,
                modifier = Modifier.padding(Spacing.lg)
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        BoardButton(
            label = "Share this card",
            supporting = "Send the text to a family member so a second person has it.",
            icon = Icons.Outlined.Share,
            onClick = {
                shareFailure = when (val result = shareText(context, cardText, "Emergency card")) {
                    is ActionResult.Failed -> result.reason
                    else -> null
                }
            },
            weight = ButtonWeight.SECONDARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (shareFailure != null) {
            Spacer(Modifier.height(Spacing.sm))
            FailureNote(text = shareFailure.orEmpty())
        }

        Spacer(Modifier.height(Spacing.lg))

        BoardButton(
            label = "Edit what is on it",
            onClick = onOpenMedicalId,
            weight = ButtonWeight.QUIET,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(Spacing.lg))

        Note(
            text = "The screen is at full brightness while this card is open so that it scans " +
                "cleanly, and goes back to normal when you leave."
        )
    }
}

/**
 * The code itself.
 *
 * Encoding runs on `Dispatchers.Default` through `produceState`, never in
 * composition: a dense symbol at 640 pixels is tens of milliseconds of work,
 * which is a dropped frame every time this screen recomposes. In a preview it
 * is skipped entirely — `Bitmap.createBitmap` does not exist in layoutlib, and
 * a preview that throws is a preview nobody looks at.
 */
@Composable
private fun QrPlate(text: String, inInspection: Boolean) {
    val colors = MaterialTheme.board

    val code by produceState<ImageBitmap?>(initialValue = null, text, inInspection) {
        value = if (inInspection) null else withContext(Dispatchers.Default) {
            QrCodec.encode(text)
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(Radius.card))
            // Hardcoded white, deliberately and only here. A QR needs a quiet
            // zone and maximum contrast to survive an angled, low-light scan;
            // theming this surface would make the card fail exactly when it
            // matters. The border keeps it from floating on the bone ground.
            .background(Color.White)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
            .padding(Spacing.xl)
    ) {
        val current = code
        if (current != null) {
            Image(
                bitmap = current,
                contentDescription = "QR code containing the emergency medical details, " +
                    "printed in words below this code",
                // No smoothing: interpolating between modules is how a scaled
                // QR turns into a grey blur a scanner cannot resolve.
                filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = if (inInspection) "QR CODE" else "Preparing the code",
                style = MaterialTheme.boardType.nameplate,
                // Ink on the white substrate, not a theme token — this text
                // sits on the same forced-white plate as the code.
                color = Color(0xFF5A6068),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Holds the screen at full brightness while this composable is on screen.
 *
 * Set through the window's own layout params rather than by asking the user to
 * change a system setting, and restored on the way out so the app does not
 * quietly drain a battery it may need later. A wearer's phone at 3% is a
 * different emergency.
 */
@Composable
private fun ForceMaxBrightness(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = if (enabled) context.findActivity()?.window else null

        // Read before write, and into a Float rather than holding the params
        // object: `window.attributes` hands back the live LayoutParams, so a
        // saved reference would be mutated by the very line below it and the
        // restore would put back full brightness.
        val previousBrightness: Float? = window?.attributes?.screenBrightness

        if (window != null) {
            window.attributes = window.attributes.apply {
                screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            }
        }

        // Restoration happens when the card leaves the composition, which is
        // the normal way out of this screen. Backgrounding the app while it is
        // still open leaves the override in place until the user comes back
        // and navigates away — acceptable for a screen nobody parks on, but
        // worth knowing before this pattern is copied somewhere stickier.
        onDispose {
            if (window != null && previousBrightness != null) {
                window.attributes = window.attributes.apply {
                    screenBrightness = previousBrightness
                }
            }
        }
    }
}

/**
 * The Activity behind a Compose context.
 *
 * `LocalContext` is not guaranteed to be the Activity — it is a ContextWrapper
 * in a dialog, in a preview, and inside some host views — so this unwraps
 * rather than casting and crashing.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// ============================================
// PREVIEWS
// ============================================

private val previewCardId = MedicalId(
    bloodType = "B+",
    emergencyContact = "+91 98300 11223",
    contactName = "Priya",
    allergies = "Penicillin. Peanuts.",
    age = 74,
    conditions = "Type 2 diabetes",
    medications = "Metformin 500mg",
    organDonor = true
)

@Preview(name = "Emergency card — light", showBackground = true, heightDp = 1200)
@Composable
private fun EmergencyCardLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        EmergencyCardScreen(
            state = EmergencyCardUiState(medicalId = previewCardId, wearerName = "Amit Ganguly"),
            onBack = {}, onOpenMedicalId = {}
        )
    }
}

@Preview(
    name = "Emergency card — dark",
    showBackground = true,
    heightDp = 1200,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun EmergencyCardDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        EmergencyCardScreen(
            state = EmergencyCardUiState(medicalId = previewCardId, wearerName = "Amit Ganguly"),
            onBack = {}, onOpenMedicalId = {}
        )
    }
}

@Preview(name = "Emergency card — nothing filled in", showBackground = true, heightDp = 600)
@Composable
private fun EmergencyCardEmptyPreview() {
    SafeShadeTheme(darkTheme = false) {
        EmergencyCardScreen(
            state = EmergencyCardUiState(),
            onBack = {}, onOpenMedicalId = {}
        )
    }
}
