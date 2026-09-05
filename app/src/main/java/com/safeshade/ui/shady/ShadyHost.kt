package com.safeshade.ui.shady

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.device.ConnectionState
import com.safeshade.ui.theme.Motion

/**
 * The single place Shady is allowed to appear, and the single place the
 * suppression rule is enforced.
 *
 * The firmware keeps Shady off the SOS and Fall screens deliberately: an
 * emergency is not the moment for a mascot, and a character loitering beside a
 * fall countdown undermines the seriousness of the thing it is standing next
 * to. That rule is worth keeping, but it is exactly the kind of rule that
 * decays when it is re-implemented per screen — one new emergency surface and
 * somebody forgets.
 *
 * So screens never call [Shady] directly. They call this, pass whether an
 * emergency is live, and the rule holds everywhere by construction.
 */
@Composable
fun ShadyHost(
    mood: ShadyMood,
    emergencyActive: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    // Not merely hidden: when an emergency is live Shady is removed from the
    // composition entirely, so it cannot animate, cannot be read out by a
    // screen reader, and cannot consume frames while the countdown runs.
    AnimatedVisibility(
        visible = !emergencyActive,
        enter = fadeIn(tween(Motion.normal)) + scaleIn(initialScale = 0.92f, animationSpec = tween(Motion.normal)),
        exit = fadeOut(tween(Motion.fast)) + scaleOut(targetScale = 0.92f, animationSpec = tween(Motion.fast)),
        modifier = modifier
    ) {
        Shady(mood = mood, size = size)
    }
}

/**
 * Maps link state onto a mood, so every screen tells the same story about the
 * same situation.
 *
 * Kept as a pure function rather than scattered `when` blocks: Shady looking
 * calm on one screen and concerned on another about the identical connection
 * state is the sort of inconsistency nobody reports as a bug and everybody
 * feels as sloppiness.
 */
fun shadyMoodFor(
    connection: ConnectionState,
    batteryPercent: Int? = null,
    hasUnresolvedTrip: Boolean = false
): ShadyMood = when {
    hasUnresolvedTrip -> ShadyMood.CONCERNED
    connection is ConnectionState.Scanning || connection is ConnectionState.Connecting ->
        ShadyMood.SEARCHING
    connection is ConnectionState.Ready && batteryPercent != null && batteryPercent <= 20 ->
        ShadyMood.CONCERNED
    connection is ConnectionState.Ready -> ShadyMood.WATCHING
    connection is ConnectionState.Connected -> ShadyMood.SEARCHING
    connection is ConnectionState.Disconnected -> ShadyMood.OFFLINE
    connection is ConnectionState.BluetoothUnavailable -> ShadyMood.OFFLINE
    connection is ConnectionState.ScanFailed -> ShadyMood.CONCERNED
    else -> ShadyMood.CALM
}
