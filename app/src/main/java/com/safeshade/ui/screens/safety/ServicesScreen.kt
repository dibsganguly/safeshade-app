package com.safeshade.ui.screens.safety

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.safeshade.platform.NearbyService
import com.safeshade.platform.OverpassResult
import com.safeshade.platform.ServiceKind
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

/**
 * What the phone knows about the nearest hospitals, police stations, fire
 * stations and pharmacies.
 *
 * `result` is the Overpass answer or the cached one with its age; null while
 * the first fetch is out. `aroundLabel` says which point the search ran
 * from - the phone's current fix, or the first safe zone when there is no
 * fix - because "nearest" is meaningless without saying nearest to what.
 */
data class NearbyUiState(
    val result: OverpassResult? = null,
    val loading: Boolean = false,
    val aroundLabel: String? = null,
    /** True when there was nothing to search around; the section then says so. */
    val noPoint: Boolean = false
)

/** Everything the services directory draws. */
data class ServicesUiState(
    val nearby: NearbyUiState = NearbyUiState(),
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
    onRefreshNearby: () -> Unit = {},
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
                    // Title Case, like every other prominent button, and
                    // spelled out rather than taking the sentence-case label
                    // the rows below share with it.
                    label = "112 – All Emergencies",
                    supporting = primary.blurb,
                    // The cross, not the police badge. This number reaches
                    // police, fire *and* ambulance - the blurb underneath says
                    // so - and a badge names one of the three. The button on
                    // the Safety screen that leads here already took the cross
                    // for the same reason; these two are the same act and now
                    // look like it.
                    icon = SafeShadeIcons.Cross,
                    onClick = { dial(primary.number) },
                    // Red. This is the one button in the app that puts a
                    // person one tap from the national emergency line, and it
                    // sat in the same charcoal as Save and Back.
                    weight = ButtonWeight.DANGER,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (failure != null) {
            item("failure") { FailureNote(text = failure.orEmpty()) }
        }

        // ---- Nearest, from OpenStreetMap. Below the public numbers on
        // purpose: 112 works in a basement and this section needs a network.
        item("nearby-heading") {
            SectionPlate(
                title = "Nearest to you",
                trailing = {
                    TextButton(onClick = onRefreshNearby, enabled = !state.nearby.loading) {
                        Text(
                            if (state.nearby.loading) "Looking…" else "Refresh",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.inkAttention
                        )
                    }
                }
            )
        }
        item("nearby") {
            NearbyPlate(
                nearby = state.nearby,
                onCall = { dial(it) },
                onDirections = { service ->
                    val uri = Uri.parse("geo:${service.lat},${service.lon}?q=${service.lat},${service.lon}(${Uri.encode(service.name)})")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    failure = try {
                        context.startActivity(intent); null
                    } catch (e: ActivityNotFoundException) {
                        "No map app on this phone can open directions"
                    }
                }
            )
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
                        name = "${service.number} – ${service.label}",
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
    }
}

/**
 * The nearest services, grouped by kind, each a way with its distance as the
 * state word and Call / Directions on tap. Offline shows the cached list with
 * its age in the line above it; a failure with no cache shows the reason and
 * nothing else. The first fetch shows a line, not a spinner: the public
 * numbers above are already usable and the eye should stay on them.
 */
@Composable
private fun NearbyPlate(
    nearby: NearbyUiState,
    onCall: (String) -> Unit,
    onDirections: (NearbyService) -> Unit
) {
    val colors = MaterialTheme.board
    val result = nearby.result
    val list: OverpassResult.Ok? = when (result) {
        is OverpassResult.Ok -> result
        is OverpassResult.Failed -> result.cached
        null -> null
    }
    val statusLine = when {
        nearby.noPoint -> "No location yet. Turn on location or add a safe zone, and the nearest services appear here."
        result == null && nearby.loading -> "Looking around ${nearby.aroundLabel ?: "your location"}…"
        result is OverpassResult.Failed && list == null -> result.reason
        result is OverpassResult.Failed -> "${result.reason}. Showing the list from ${agoLabel(list!!.fetchedAt)}."
        list != null && list.fromCache -> "Around ${nearby.aroundLabel ?: "your location"}, as of ${agoLabel(list.fetchedAt)}. From OpenStreetMap."
        list != null -> "Around ${nearby.aroundLabel ?: "your location"}. From OpenStreetMap."
        else -> null
    }

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        if (statusLine != null) {
            Text(
                text = statusLine,
                style = MaterialTheme.typography.bodySmall,
                color = if (result is OverpassResult.Failed) colors.inkAttention else colors.inkMuted,
                modifier = Modifier.padding(Spacing.lg)
            )
        }
        if (list != null) {
            var first = statusLine == null
            ServiceKind.entries.forEach { kind ->
                val ofKind = list.services.filter { it.kind == kind }.take(3)
                ofKind.forEach { service ->
                    if (!first) Hairline()
                    first = false
                    Way(
                        name = service.name,
                        state = LampState.OFF,
                        stateLabel = distanceLabel(service.distanceM),
                        detail = listOfNotNull(kind.label, service.address, service.phone).joinToString(" · "),
                        icon = when (kind) {
                            ServiceKind.HOSPITAL -> SafeShadeIcons.Cross
                            ServiceKind.PHARMACY -> SafeShadeIcons.Cross
                            ServiceKind.POLICE -> SafeShadeIcons.SafeZone
                            ServiceKind.FIRE -> SafeShadeIcons.Alert02
                        },
                        onClick = { if (service.phone != null) onCall(service.phone) else onDirections(service) }
                    )
                }
            }
            if (list.services.isEmpty()) {
                Text(
                    text = "OpenStreetMap lists nothing of these kinds within 5 km.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(Spacing.lg)
                )
            } else {
                Text(
                    text = "A row with a number dials it; one without opens directions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg, top = Spacing.sm)
                )
            }
        }
    }
}

private fun distanceLabel(m: Int): String =
    if (m < 1000) "$m m" else String.format(java.util.Locale.US, "%.1f km", m / 1000.0)

private fun agoLabel(at: Long, now: Long = System.currentTimeMillis()): String {
    val minutes = ((now - at) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 48 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
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
