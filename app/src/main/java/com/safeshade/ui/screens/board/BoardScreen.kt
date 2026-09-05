package com.safeshade.ui.screens.board

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.safeshade.R
import com.safeshade.data.UserRole
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Gauge
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.MainsPlate
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.nav.BottomDestination
import com.safeshade.ui.nav.tabForRoute
import com.safeshade.ui.shady.ShadyHost
import com.safeshade.ui.shady.shadyMoodFor
import com.safeshade.ui.shady.ReactionStyle
import com.safeshade.ui.shady.ShadyStage
import com.safeshade.ui.shady.rememberShadyReactor
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * One circuit as the Board screen needs to render it.
 *
 * Deliberately a flat presentation type rather than a domain object: a way on
 * this screen may be backed by a BLE setting, a phone permission, or a derived
 * condition like "the medical card has never been filled in", and the row
 * should not care which.
 */
data class BoardWay(
    val key: String,
    val name: String,
    val state: LampState,
    val stateLabel: String,
    val detail: String? = null,
    val icon: ImageVector? = null,
    val sealed: Boolean = false,
    val route: String? = null
)

/** Everything the Board screen draws. */
data class BoardUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val role: UserRole = UserRole.GUARDIAN,
    val headline: String = "",
    /** Who the device looks after, for the mains plate's instrument bay. */
    val protectedName: String = "",
    val subline: String = "",
    val batteryPercent: Int? = null,
    val signalDbm: Int? = null,
    val ways: List<BoardWay> = emptyList(),
    val temperatureC: Float? = null,
    val weatherCondition: String? = null,
    val uvIndex: Float? = null,
    val lastSyncLabel: String? = null,
    val isSyncing: Boolean = false,
    val isRinging: Boolean = false,
    val hasUnresolvedTrip: Boolean = false,
    val permissionsGranted: Boolean = true
)

/**
 * The board.
 *
 * Answers one question in the first viewport — is the person I am responsible
 * for covered right now — and then lists the circuits that make up that answer.
 * Everything below the mains plate is elaboration; a user who reads only the
 * top of this screen has still got what they came for.
 */
@Composable
fun BoardScreen(
    state: BoardUiState,
    onConnectToggle: () -> Unit,
    onRequestPermissions: () -> Unit,
    onSync: () -> Unit,
    onRing: () -> Unit,
    onOpenWay: (String) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /**
     * Hoisted above the NavHost by SafeShadeApp so scroll position
     * survives a tab switch. Defaulted so the previews still compile
     * without one.
     */
    listState: LazyListState = rememberLazyListState()
) {
    val colors = MaterialTheme.board
    val lamp = state.connection.toLampState()

    // The card's Shady reacts subtly; the one on the stage at the foot of the
    // page reacts boldly. Two reactors, because a beat of personality inside a
    // status card and a full tumble on an empty stage are not the same gesture.
    val cardReactor = rememberShadyReactor(ReactionStyle.SUBTLE)

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            // No bottom padding: the stage below is meant to sit flush against
            // the navigation bar, so Shady stands on its top rule.
            bottom = contentPadding.calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("header") {
            // The settings cog used to live at the end of this row. It has
            // moved to the Device screen's heading, for two reasons: app
            // settings are not a property of the board, and — more concretely —
            // the settings routes belong to the Device tab, so entering them
            // from here lit a tab the user was not in. Its absence is what buys
            // the wordmark the room to be the size a masthead should be.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
            ) {
                // Emblem hard left, wordmark hard right, the width of the
                // screen between them. The previous version put one weighted
                // spacer in front of *both*, which packed them together against
                // the right edge and left the masthead reading as a caption.
                //
                // The emblem is a full-colour raster, so it is an Image rather
                // than an Icon - an Icon would flatten it to a single tint.
                Image(
                    painter = painterResource(R.drawable.splash_emblem),
                    contentDescription = null,
                    // Height, not size. The drawable is the mark itself now
                    // rather than a mark inside a square of transparency, so a
                    // square box would letterbox it back down again.
                    modifier = Modifier.height(52.dp)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "SafeShade",
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.ink
                )
            }
        }

        item("mains") {
            MainsPlate(
                state = lamp,
                headline = state.headline,
                subline = state.subline,
                batteryPercent = state.batteryPercent,
                signalDbm = state.signalDbm,
                protectedName = state.protectedName,
                // Shady rides in the plate's trailing slot rather than floating
                // over it. Overlaying the mascot on the headline was legible
                // with a short status and unreadable with a long one, which is
                // exactly the case that matters most.
                trailing = {
                    ShadyHost(
                        mood = shadyMoodFor(
                            connection = state.connection,
                            batteryPercent = state.batteryPercent,
                            hasUnresolvedTrip = state.hasUnresolvedTrip
                        ),
                        emergencyActive = state.hasUnresolvedTrip,
                        size = 72.dp,
                        pose = cardReactor.pose,
                        modifier = Modifier
                            .clip(RoundedCornerShape(Radius.card))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClickLabel = "Play with Shady"
                            ) { cardReactor.poke() }
                    )
                }
            )
        }

        // One primary action slot, whose contents follow the link.
        //
        // Showing "connect" and a disabled "ring" together wastes the most
        // valuable row on the screen on a control that cannot be pressed. When
        // there is no link, connecting is the only thing worth offering; the
        // moment there is one, ringing takes the same slot — which is also how
        // TEST · RING DEVICE ends up in the first viewport rather than below
        // the fold, in the only state where it does anything.
        item("primary-action") {
            if (lamp == LampState.LIVE) {
                BoardButton(
                    label = if (state.isRinging) "Ringing" else "Test — ring device",
                    supporting = if (state.isRinging) {
                        "Tap the button on the device to stop the siren"
                    } else {
                        "Sounds the siren so you can find it"
                    },
                    icon = SafeShadeIcons.RingTheDevice,
                    onClick = onRing,
                    enabled = !state.isRinging,
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                BoardButton(
                    label = if (state.permissionsGranted) connectLabel(state.connection) else "Grant permissions",
                    supporting = if (state.permissionsGranted) null else "Bluetooth and location are needed to find the device",
                    icon = if (state.permissionsGranted) SafeShadeIcons.ConnectToTheDevice else null,
                    onClick = if (state.permissionsGranted) onConnectToggle else onRequestPermissions,
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item("ways-heading") {
            SectionPlate(title = "Ways")
        }

        item("ways") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.ways.forEachIndexed { index, way ->
                    if (index > 0) Hairline()
                    Way(
                        name = way.name,
                        state = way.state,
                        stateLabel = way.stateLabel,
                        detail = way.detail,
                        icon = way.icon,
                        sealed = way.sealed,
                        onClick = way.route?.let { route -> { onOpenWay(route) } }
                    )
                }
            }
        }

        item("conditions-heading") {
            // The last-synced time sits immediately left of the control that
            // changes it, rather than orphaned somewhere below the gauges. The
            // question "is this reading current?" and the button that answers
            // it are the same thought, and they now occupy the same line.
            val spin by rememberInfiniteTransition(label = "sync-spin")
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "sync-rotation"
                )

            SectionPlate(
                title = "Conditions",
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.lastSyncLabel != null) {
                            Text(
                                text = if (state.isSyncing) "Syncing" else state.lastSyncLabel,
                                style = MaterialTheme.boardType.rowDetail,
                                color = colors.inkFaint
                            )
                        }
                        IconButton(
                            onClick = onSync,
                            enabled = !state.isSyncing && !state.isRinging
                        ) {
                            Icon(
                                Icons.Outlined.Sync,
                                contentDescription = "Sync weather and location",
                                tint = if (state.isSyncing) colors.inkFaint else colors.inkMuted,
                                modifier = Modifier
                                    .size(20.dp)
                                    // Turns only while a sync is actually in
                                    // flight. A spinner that runs regardless
                                    // would be decoration; this one is the only
                                    // evidence the tap did anything, because the
                                    // gauges below may come back identical.
                                    .rotate(if (state.isSyncing) spin else 0f)
                            )
                        }
                    }
                }
            )
        }

        item("gauges") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Gauge(
                    label = "Temperature",
                    value = state.temperatureC?.let { "%.0f".format(it) } ?: "--",
                    unit = "°C",
                    caption = state.weatherCondition,
                    modifier = Modifier.weight(1f)
                )
                Gauge(
                    label = "UV index",
                    value = state.uvIndex?.let { "%.1f".format(it) } ?: "--",
                    caption = state.uvIndex?.let { uvAdvice(it) },
                    state = state.uvIndex?.let { if (it >= 8f) LampState.TRIP else if (it >= 6f) LampState.ATTENTION else null },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // The last-synced time used to be repeated here, orphaned under the
        // gauges, rendering the identical `state.lastSyncLabel` string the
        // Conditions heading now carries beside its sync button. One reading,
        // next to the control that changes it.

        item("shady-stage") {
            // Below everything that matters, on purpose. You reach it only by
            // scrolling past the whole board, which is why it never competes
            // with the status above and why the card's Shady has left the
            // screen by the time this one arrives.
            ShadyStage()
        }
    }
}

private fun connectLabel(connection: ConnectionState): String = when (connection) {
    is ConnectionState.Scanning -> "Searching…"
    is ConnectionState.Connecting -> "Connecting…"
    is ConnectionState.Found -> "Found ${connection.name}"
    is ConnectionState.Connected -> "Starting up…"
    is ConnectionState.BluetoothUnavailable -> "Turn on Bluetooth"
    is ConnectionState.ScanFailed -> "Try again"
    else -> "Connect to the device"
}

/**
 * Link state as a lamp.
 *
 * `Connected` is amber rather than teal on purpose: the GATT link is up but
 * service discovery has not finished, so nothing sent in that window would
 * actually reach the device. Showing it as live would be the same lie the old
 * reconnect logic told itself.
 */
private fun ConnectionState.toLampState(): LampState = when (this) {
    is ConnectionState.Ready -> LampState.LIVE
    is ConnectionState.Connected -> LampState.ATTENTION
    is ConnectionState.Scanning, is ConnectionState.Connecting, is ConnectionState.Found ->
        LampState.ATTENTION
    is ConnectionState.ScanFailed, is ConnectionState.BluetoothUnavailable -> LampState.TRIP
    is ConnectionState.Disconnected -> LampState.OFF
}

private fun uvAdvice(uv: Float): String = when {
    uv >= 11f -> "Extreme — stay inside"
    uv >= 8f -> "Very high — cover up"
    uv >= 6f -> "High — use shade"
    uv >= 3f -> "Moderate"
    else -> "Low"
}
