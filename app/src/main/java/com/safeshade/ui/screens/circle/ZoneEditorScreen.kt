package com.safeshade.ui.screens.circle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/** Everything the zone editor draws. */
data class ZoneEditorUiState(
    val isNew: Boolean = true,
    val name: String = "",
    val radiusMeters: Float = 200f,
    /** The slider's ends. Below 50 m a geofence fires on GPS noise alone. */
    val minRadiusMeters: Float = 50f,
    val maxRadiusMeters: Float = 1000f,
    val lat: Double? = null,
    val lon: Double? = null,
    /** A readable name for the centre, when one is known. */
    val placeLabel: String? = null,
    val alertOnExit: Boolean = true,
    val alertOnEnter: Boolean = false,
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val isSaving: Boolean = false,
    val errorText: String? = null
) {
    /** A zone with no centre is not a zone. A nameless one cannot be read. */
    val canSave: Boolean
        get() = name.isNotBlank() && lat != null && lon != null && !isSaving
}

/**
 * Adding or changing one safe zone.
 *
 * The map lives on its own route rather than in a panel here, because a
 * WebView inside this scrolling column would swallow every vertical drag meant
 * for the page. What this screen keeps is the *description* of the centre — a
 * place name and a coordinate readout — so a user who never opens the map can
 * still tell which place they are editing.
 */
@Composable
fun ZoneEditorScreen(
    state: ZoneEditorUiState,
    onNameChange: (String) -> Unit,
    onRadiusChange: (Float) -> Unit,
    onAlertOnExitChange: (Boolean) -> Unit,
    onAlertOnEnterChange: (Boolean) -> Unit,
    onPickOnMap: () -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val subject = wearerSubject(state.role, state.wearerName)
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            // The theme draws edge to edge, so the window does not resize
            // when the keyboard opens: without this inset a focused field would sit behind it.
            .imePadding()
    ) {
        ScreenHeader(
            title = if (state.isNew) "New safe zone" else state.name.ifBlank { "Safe zone" },
            subtitle = if (state.isNew) "It starts alerting as soon as it is saved" else null,
            onBack = onBack,
            backDescription = "Go back to the zone list without saving",
            tier = ScreenTier.PUSHED,
            // The `top` is not decoration and is not this screen's choice. Every
            // sub-page outside this bank puts its header inside a LazyColumn
            // whose top contentPadding is `inset + Spacing.sm`; this bank puts
            // the header above the list, so without the same 8dp its content
            // started 8dp higher than every other bank's. That was the whole of
            // "inconsistent header-bottom paddings leading to different sub-page
            // content starting points" - the gap below the header was already
            // uniform; the gap above it was not.
            modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm)
        )

        // The list's own top contentPadding supplies the other half of
        // ScreenHeader's documented 28dp gap, same as every pattern-A screen
        // — see ScreenHeader's KDoc.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = Spacing.lg,
                bottom = Spacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item("name") {
                BoardField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = "Name",
                    placeholder = "Home",
                    supporting = "Short names read best in an alert: Home, Park, Clinic."
                )
            }

            item("place-heading") {
                SectionPlate(title = "Centre")
            }

            item("place") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        if (state.lat != null && state.lon != null) {
                            if (state.placeLabel != null) {
                                Text(
                                    text = state.placeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.ink
                                )
                                Spacer(Modifier.height(Spacing.sm))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                                Readout(label = "Latitude", value = "%.5f".format(state.lat))
                                Readout(label = "Longitude", value = "%.5f".format(state.lon))
                            }
                        } else {
                            Text(
                                text = "No place chosen yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.inkMuted
                            )
                        }
                        Spacer(Modifier.height(Spacing.md))
                        BoardButton(
                            label = if (state.lat == null) "Choose on the map" else "Change the place",
                            icon = Icons.Outlined.Map,
                            onClick = onPickOnMap,
                            weight = ButtonWeight.SECONDARY,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item("radius-heading") {
                SectionPlate(title = "Size")
            }

            item("radius") {
                RadiusControl(
                    radiusMeters = state.radiusMeters,
                    minMeters = state.minRadiusMeters,
                    maxMeters = state.maxRadiusMeters,
                    onRadiusChange = onRadiusChange
                )
            }

            item("alerts-heading") {
                SectionPlate(title = "Alerts")
            }

            item("alerts") {
                BoardPlate(modifier = Modifier.fillMaxWidth()) {
                    Way(
                        name = "Tell me when leaving",
                        state = if (state.alertOnExit) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.alertOnExit) "Armed" else "Off",
                        detail = when (state.role) {
                            UserRole.GUARDIAN -> "A notification when $subject crosses out of this circle"
                            UserRole.COMPANION -> "Your guardian is told when you cross out of this circle"
                        },
                        icon = Icons.Outlined.Logout,
                        checked = state.alertOnExit,
                        onCheckedChange = onAlertOnExitChange
                    )
                    Hairline()
                    Way(
                        name = "Tell me when arriving",
                        state = if (state.alertOnEnter) LampState.LIVE else LampState.OFF,
                        stateLabel = if (state.alertOnEnter) "Armed" else "Off",
                        detail = when (state.role) {
                            UserRole.GUARDIAN -> "A notification when $subject reaches this circle"
                            UserRole.COMPANION -> "Your guardian is told when you reach this circle"
                        },
                        icon = Icons.Outlined.Login,
                        checked = state.alertOnEnter,
                        onCheckedChange = onAlertOnEnterChange
                    )
                }
            }

            if (!state.alertOnExit && !state.alertOnEnter) {
                item("no-alerts") {
                    Text(
                        text = "With both switches off this zone is saved but never says anything. That is fine if you only want it on the map.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                }
            }

            if (state.errorText != null) {
                item("error") {
                    Text(
                        text = state.errorText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkAttention
                    )
                }
            }

            item("save") {
                BoardButton(
                    label = if (state.isSaving) "Saving" else "Save this zone",
                    supporting = if (state.canSave || state.isSaving) null else "A name and a place are needed",
                    onClick = onSave,
                    enabled = state.canSave,
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (onDelete != null) {
                item("delete") {
                    if (confirmingDelete) {
                        Column {
                            Text(
                                text = "Remove ${state.name.ifBlank { "this zone" }}? No more alerts will come from this place.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(Spacing.md))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                BoardButton(
                                    label = "Keep it",
                                    onClick = { confirmingDelete = false },
                                    weight = ButtonWeight.SECONDARY,
                                    modifier = Modifier.weight(1f)
                                )
                                BoardButton(
                                    label = "Remove",
                                    onClick = {
                                        confirmingDelete = false
                                        onDelete()
                                    },
                                    weight = ButtonWeight.DANGER,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    } else {
                        BoardButton(
                            label = "Remove this zone",
                            onClick = { confirmingDelete = true },
                            weight = ButtonWeight.QUIET,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

/**
 * The radius slider and its readout.
 *
 * A stock `Slider` is used rather than a hand-drawn one, and it is on-system
 * here because this theme maps Material's `primary` onto the board's ink
 * rather than onto a brand hue — so it renders as a charcoal track, not as a
 * coloured one. Anything with a genuine hue would break the rule.
 *
 * The value is stepped to 50 m. A geofence radius is not a precise instrument
 * — Android's own accuracy is tens of metres — and a continuous slider invites
 * a user to fiddle for a precision the platform cannot honour.
 */
@Composable
private fun RadiusControl(
    radiusMeters: Float,
    minMeters: Float,
    maxMeters: Float,
    onRadiusChange: (Float) -> Unit
) {
    val colors = MaterialTheme.board
    val steps = (((maxMeters - minMeters) / STEP_METERS).roundToInt() - 1).coerceAtLeast(0)
    val metres = radiusMeters.roundToInt()

    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Readout(label = "Radius", value = "$metres", large = true)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "metres across the circle's edge",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(bottom = Spacing.sm)
                )
            }
            Slider(
                value = radiusMeters.coerceIn(minMeters, maxMeters),
                onValueChange = onRadiusChange,
                valueRange = minMeters..maxMeters,
                steps = steps,
                modifier = Modifier
                    .fillMaxWidth()
                    // The slider's own value is announced as a bare fraction
                    // otherwise, which is meaningless for a distance.
                    .semantics {
                        contentDescription = "Zone radius"
                        stateDescription = "$metres metres"
                    }
            )
            Text(
                text = radiusAdvice(metres),
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkMuted
            )
        }
    }
}

/** 50 m increments. See [RadiusControl] for why it is stepped at all. */
private const val STEP_METERS = 50f

/**
 * What a radius means in practice.
 *
 * Metres are an abstraction to most people; a small radius that fires every
 * time someone walks to the end of the drive is the most common way a safe
 * zone becomes noise, so the trade-off is stated at the point of choosing.
 */
private fun radiusAdvice(metres: Int): String = when {
    metres <= 100 -> "Tight. Good for a single building, but ordinary movement indoors can trip it."
    metres <= 300 -> "A house and the street around it. This is the usual choice."
    metres <= 600 -> "A block or a small park. Fewer alerts, and later ones."
    else -> "A whole neighbourhood. Leaving will be noticed a long way from home."
}

@Preview(name = "Zone editor — new, light", showBackground = true, heightDp = 1100)
@Composable
private fun ZoneEditorNewLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        ZoneEditorScreen(
            state = ZoneEditorUiState(
                isNew = true,
                name = "",
                radiusMeters = 200f,
                role = UserRole.GUARDIAN,
                wearerName = "Baba"
            ),
            onNameChange = {},
            onRadiusChange = {},
            onAlertOnExitChange = {},
            onAlertOnEnterChange = {},
            onPickOnMap = {},
            onSave = {},
            onDelete = null,
            onBack = {}
        )
    }
}

@Preview(name = "Zone editor — existing, dark", showBackground = true, heightDp = 1100)
@Composable
private fun ZoneEditorExistingDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        ZoneEditorScreen(
            state = ZoneEditorUiState(
                isNew = false,
                name = "Home",
                radiusMeters = 250f,
                lat = 22.5726,
                lon = 88.3639,
                placeLabel = "Near Salt Lake Sector V",
                alertOnExit = true,
                alertOnEnter = true,
                role = UserRole.COMPANION
            ),
            onNameChange = {},
            onRadiusChange = {},
            onAlertOnExitChange = {},
            onAlertOnEnterChange = {},
            onPickOnMap = {},
            onSave = {},
            onDelete = {},
            onBack = {}
        )
    }
}
