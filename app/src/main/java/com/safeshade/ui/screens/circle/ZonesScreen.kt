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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.UserRole
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/**
 * Where the device is relative to one zone.
 *
 * [UNKNOWN] is a first-class value, not an error. Before the first geofence
 * callback arrives — and any time location permission is missing — the honest
 * answer is that the app does not know, and saying "outside" instead would be
 * a guess dressed up as a fact.
 */
enum class ZonePresence { INSIDE, OUTSIDE, UNKNOWN }

/** One safe zone as the list needs to render it. */
data class ZoneRow(
    val id: String,
    val name: String,
    /** "200 m · told when leaving" — radius and which alerts are armed. */
    val detail: String,
    val presence: ZonePresence = ZonePresence.UNKNOWN,
    /** When the presence was last established. Null while never established. */
    val lastChangeLabel: String? = null
)

/** Everything the zone list draws. */
data class ZonesUiState(
    val role: UserRole = UserRole.GUARDIAN,
    val wearerName: String = "",
    val zones: List<ZoneRow> = emptyList(),
    /** False until the zone list has actually been read from disk. */
    val isLoaded: Boolean = true,
    /**
     * Geofences only fire in the background with the always-allow grant.
     * Without it the list still works and still saves; it simply cannot alert,
     * and the screen has to say so rather than appear to be working.
     */
    val backgroundLocationGranted: Boolean = true
)

/**
 * The safe zones.
 *
 * Every zone is a `Way`, which is the point: "is the device inside the zone"
 * is the same shape of question as "is fall detection armed", so it gets the
 * same shape of answer and a guardian reads the whole app the same way.
 */
@Composable
fun ZonesScreen(
    state: ZonesUiState,
    onAddZone: () -> Unit,
    onEditZone: (String) -> Unit,
    onDeleteZone: (String) -> Unit,
    onRequestBackgroundLocation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val subject = wearerSubject(state.role, state.wearerName)

    // Which row is asking "are you sure". This is not app state — nothing
    // outside this screen has any use for it and it must not survive
    // navigating away — but it does need to survive a rotation mid-question,
    // which is exactly what rememberSaveable is for.
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
    ) {
        ScreenHeader(
            title = "Safe zones",
            subtitle = when (state.role) {
                UserRole.GUARDIAN -> "Places where $subject is expected to be"
                UserRole.COMPANION -> "Places you are expected to be"
            },
            onBack = onBack,
            backDescription = "Go back to the circle",
            tier = ScreenTier.PUSHED,
            modifier = Modifier.padding(horizontal = Spacing.gutter)
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
            if (!state.backgroundLocationGranted) {
                item("permission") {
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(Spacing.lg)) {
                            Text(
                                text = "Safe zones are saved, but they cannot alert anyone yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.ink
                            )
                            Spacer(Modifier.height(Spacing.xs))
                            Text(
                                text = "Android only reports crossing a zone while the app is closed if location is set to Allow all the time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.inkMuted
                            )
                            Spacer(Modifier.height(Spacing.md))
                            BoardButton(
                                label = "Allow all the time",
                                onClick = onRequestBackgroundLocation,
                                weight = ButtonWeight.SECONDARY,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            item("heading") {
                SectionPlate(title = "Zones")
            }

            if (state.zones.isEmpty()) {
                item("empty") {
                    EmptyBay(
                        message = if (state.isLoaded) {
                            "No safe zones yet. Add one and you will be told when the device leaves it."
                        } else {
                            "Reading saved zones."
                        },
                        actionLabel = if (state.isLoaded) "Add a zone" else null,
                        onAction = if (state.isLoaded) onAddZone else null,
                        // Still reading the list back reads as "looking";
                        // confirmed empty reads as "nothing set up yet".
                        shadyMood = if (state.isLoaded) ShadyMood.CURIOUS else ShadyMood.LOOKING
                    )
                }
            } else {
                item("zones") {
                    BoardPlate(modifier = Modifier.fillMaxWidth()) {
                        state.zones.forEachIndexed { index, zone ->
                            if (index > 0) Hairline()
                            ZoneListRow(
                                zone = zone,
                                confirmingDelete = pendingDeleteId == zone.id,
                                onEdit = { onEditZone(zone.id) },
                                onAskDelete = { pendingDeleteId = zone.id },
                                onCancelDelete = { pendingDeleteId = null },
                                onConfirmDelete = {
                                    pendingDeleteId = null
                                    onDeleteZone(zone.id)
                                }
                            )
                        }
                    }
                }

                // The empty bay above already carries this action while the
                // list has nothing in it — a second "add" button beside it
                // would just be the same offer said twice.
                item("add") {
                    BoardButton(
                        label = "Add a safe zone",
                        icon = Icons.Outlined.Add,
                        onClick = onAddZone,
                        weight = ButtonWeight.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item("explainer") {
                Text(
                    text = when (state.role) {
                        UserRole.GUARDIAN ->
                            "A safe zone is a circle on a map. When the device crosses its edge you get a notification, and the crossing is recorded on the board."
                        UserRole.COMPANION ->
                            "A safe zone is a circle on a map. When you cross its edge your guardian gets a notification, and the crossing is recorded on the board."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

/**
 * A zone row and its delete control.
 *
 * The delete button is a sibling of the `Way` rather than something inside it:
 * `Way` collapses its whole subtree into one spoken utterance, so a control
 * nested in it would be unreachable by a screen reader. Placed alongside, it
 * keeps its own node and its own name.
 *
 * Deleting a zone is not confirmed by a dialog. A dialog would take the whole
 * screen away from a list the user is halfway through reading; an inline
 * question keeps the row it is about on screen and in place.
 */
@Composable
private fun ZoneListRow(
    zone: ZoneRow,
    confirmingDelete: Boolean,
    onEdit: () -> Unit,
    onAskDelete: () -> Unit,
    onCancelDelete: () -> Unit,
    onConfirmDelete: () -> Unit
) {
    val colors = MaterialTheme.board

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Way(
                name = zone.name,
                state = zone.presence.toLampState(),
                stateLabel = zone.presence.label,
                detail = listOfNotNull(zone.detail, zone.lastChangeLabel).joinToString(" · "),
                icon = SafeShadeIcons.SafeZone,
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onAskDelete,
                modifier = Modifier.padding(end = Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove the ${zone.name} zone",
                    tint = colors.inkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (confirmingDelete) {
            Hairline()
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "Remove ${zone.name}? No more alerts will come from this place.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.ink
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    BoardButton(
                        label = "Keep it",
                        onClick = onCancelDelete,
                        weight = ButtonWeight.SECONDARY,
                        modifier = Modifier.weight(1f)
                    )
                    BoardButton(
                        label = "Remove",
                        onClick = onConfirmDelete,
                        weight = ButtonWeight.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Presence as a lamp.
 *
 * Outside is amber, not red. A red lamp says something has tripped and needs a
 * person, and "at the shops rather than at home" is not that — the actual
 * breach is what gets logged as a trip. A lamp that cries trip for an ordinary
 * afternoon is a lamp a guardian learns to ignore.
 */
private fun ZonePresence.toLampState(): LampState = when (this) {
    ZonePresence.INSIDE -> LampState.LIVE
    ZonePresence.OUTSIDE -> LampState.ATTENTION
    ZonePresence.UNKNOWN -> LampState.UNKNOWN
}

private val ZonePresence.label: String
    get() = when (this) {
        ZonePresence.INSIDE -> "Inside"
        ZonePresence.OUTSIDE -> "Outside"
        ZonePresence.UNKNOWN -> "Not known"
    }

private val sampleZones = listOf(
    ZoneRow(
        id = "1",
        name = "Home",
        detail = "200 m · told when leaving",
        presence = ZonePresence.INSIDE,
        lastChangeLabel = "since 16:40"
    ),
    ZoneRow(
        id = "2",
        name = "Park",
        detail = "150 m · told when leaving and arriving",
        presence = ZonePresence.OUTSIDE
    ),
    ZoneRow(
        id = "3",
        name = "Clinic",
        detail = "300 m · told when arriving",
        presence = ZonePresence.UNKNOWN
    )
)

@Preview(name = "Zones — light", showBackground = true, heightDp = 900)
@Composable
private fun ZonesLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        ZonesScreen(
            state = ZonesUiState(
                role = UserRole.GUARDIAN,
                wearerName = "Baba",
                zones = sampleZones
            ),
            onAddZone = {},
            onEditZone = {},
            onDeleteZone = {},
            onRequestBackgroundLocation = {},
            onBack = {}
        )
    }
}

@Preview(name = "Zones — dark, empty and unpermitted", showBackground = true, heightDp = 900)
@Composable
private fun ZonesDarkEmptyPreview() {
    SafeShadeTheme(darkTheme = true) {
        ZonesScreen(
            state = ZonesUiState(
                role = UserRole.COMPANION,
                zones = emptyList(),
                backgroundLocationGranted = false
            ),
            onAddZone = {},
            onEditZone = {},
            onDeleteZone = {},
            onRequestBackgroundLocation = {},
            onBack = {}
        )
    }
}
