package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ActionResult
import com.safeshade.data.EmergencyService
import com.safeshade.data.IndiaEmergencyServices
import com.safeshade.dialNumber
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the services directory draws. */
data class ServicesUiState(
    /**
     * Defaults to the built-in Indian list. Parameterised rather than read
     * from the constant inside the composable so a preview, a test, or a
     * future second country does not have to fight a hardcoded list.
     */
    val services: List<EmergencyService> = IndiaEmergencyServices
)

/**
 * The public emergency numbers, held on the phone.
 *
 * No network, no account, no permission. That is the whole point: this list is
 * useful in a basement with no signal to the extent that a phone can dial at
 * all, and it never fails because a server was down.
 *
 * **Every row pre-fills the dialer and stops.** `dialNumber(..., allowDirectCall
 * = false)` is not a limitation to be worked around later — auto-dialling 112
 * on a mis-tap wastes an emergency line and frightens the person holding the
 * phone. The user is looking straight at the screen when they pick a number,
 * so the extra tap costs a second and makes every mistake recoverable.
 */
@Composable
fun ServicesScreen(
    state: ServicesUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current

    // Whether the last tap actually reached a dialer. A phone with no dialer
    // app is rare and not impossible (a tablet), and silently doing nothing on
    // this screen of all screens is not acceptable.
    var failure by remember { mutableStateOf<String?>(null) }

    fun dial(number: String) {
        failure = when (val result = dialNumber(context, number, allowDirectCall = false)) {
            is ActionResult.Failed -> result.reason
            else -> null
        }
    }

    val primary = state.services.firstOrNull { it.number == "112" }
    val rest = state.services.filter { it.number != "112" }

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
            // ScreenHeader, not PanelHeader - see the note in ContactsScreen.
            // A list with its own `spacedBy` must not also take the adapter's.
            ScreenHeader(
                title = "Emergency numbers",
                subtitle = "Stored on this phone. Nothing here calls on its own.",
                onBack = onBack
            )
        }

        if (primary != null) {
            // 112 gets the whole width and the top of the screen because it
            // reaches police, fire and ambulance together — someone who does
            // not know which service they need still gets the right one.
            //
            // The supporting line used to append ". Opens the dialer; you
            // press call." to the blurb — a second clause that made this
            // BoardButton's centred, unwrapped text three lines deep on a
            // narrow phone at a raised font scale. Dropped rather than
            // shortened: the header above already promises "Nothing here
            // calls on its own", so restating it per-button was duplicate
            // information, not just long information.
            item("primary") {
                BoardButton(
                    label = "112 — ${primary.label}",
                    supporting = primary.blurb,
                    icon = SafeShadeIcons.Police,
                    onClick = { dial(primary.number) },
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (failure != null) {
            item("failure") { FailureNote(text = failure.orEmpty()) }
        }

        item("rest-heading") { SectionPlate(title = "If you know which one you need") }

        item("rest") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                rest.forEachIndexed { index, service ->
                    if (index > 0) Hairline()
                    Way(
                        // The number leads the nameplate. Someone who half
                        // remembers "the ambulance one" is scanning for three
                        // digits, and burying them in the state column would
                        // set them in the faintest ink on the row.
                        name = "${service.number} — ${service.label}",
                        // A public line has no circuit state to report, so the
                        // lamp stays unlit and the state word says what the
                        // row does rather than pretending to be a status.
                        state = LampState.OFF,
                        stateLabel = "Dial",
                        detail = service.blurb,
                        onClick = { dial(service.number) }
                    )
                }
            }
        }

        item("footer") {
            Note(
                text = "Tapping any of these opens your dialer with the number already in it. " +
                    "Nothing is dialled until you press the call button, so a mis-tap costs " +
                    "nothing. These are Indian numbers and will not work abroad."
            )
        }
    }
}

// ============================================
// PREVIEWS
// ============================================

@Preview(name = "Services — light", showBackground = true, heightDp = 1000)
@Composable
private fun ServicesLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        ServicesScreen(state = ServicesUiState(), onBack = {})
    }
}

@Preview(
    name = "Services — dark",
    showBackground = true,
    heightDp = 1000,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ServicesDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        ServicesScreen(state = ServicesUiState(), onBack = {})
    }
}
