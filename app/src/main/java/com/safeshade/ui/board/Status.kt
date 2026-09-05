package com.safeshade.ui.board

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.safeshade.ui.shady.ShadyHost
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType

/**
 * The mains plate — the first thing on the Board screen and the answer to the
 * only question that matters on opening the app.
 *
 * Everything else can be scrolled past. This cannot: it says who is protected,
 * whether the link is live, and how much life the device has left, in one
 * glance and in that order of priority.
 */
@Composable
fun MainsPlate(
    state: LampState,
    headline: String,
    /**
     * The line under the headline, or null where there is nothing to say.
     *
     * Nullable because a caller that had nothing to put here was passing an
     * empty string, which is not the same thing: an empty Text still occupies
     * a line box and still takes the 4dp above it, so the card carried a gap
     * where a sentence used to be.
     */
    subline: String?,
    modifier: Modifier = Modifier,
    batteryPercent: Int? = null,
    signalDbm: Int? = null,
    /** Who the device is looking after. The contract's "who is protected". */
    protectedName: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(Spacing.lg)
        ) {
            // No lamp here. The headline already says the state in words, and
            // a 22dp lamp beside a two-word status was the same information
            // twice at the top of the most-read screen.
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.ink
                )
                if (!subline.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = subline,
                        style = MaterialTheme.boardType.rowDetail,
                        color = colors.inkMuted
                    )
                }
            }

            trailing?.invoke()
        }

        // Instrument strip, always present.
        //
        // An earlier version hid this whole row while disconnected, on the
        // reasoning that "--" is noise. On a board it is the opposite: a panel
        // shows every way it has whether or not current is flowing, and a
        // reading that collapses to nothing takes the structure with it — the
        // first thing a new user meets became one sentence and a button. An
        // unlit gauge is information; an absent gauge is an absent instrument.
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
        ) {
            // Three weighted slots rather than two left-packed ones. Two
            // readouts in a full-width bay left a void down the right side,
            // which read as a bay that had not finished loading rather than one
            // whose instruments are simply unlit.
            Readout(
                label = "Protecting",
                value = protectedName?.takeIf { it.isNotBlank() } ?: "Not set",
                state = if (protectedName.isNullOrBlank()) LampState.ATTENTION else null,
                modifier = Modifier.weight(1.2f)
            )
            Readout(
                label = "Battery",
                value = batteryPercent?.let { "$it%" } ?: "—",
                state = batteryPercent?.let {
                    when {
                        it <= 15 -> LampState.TRIP
                        it <= 30 -> LampState.ATTENTION
                        else -> null
                    }
                },
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            )
            Readout(
                label = "Signal",
                value = signalDbm?.let { "$it" } ?: "—",
                state = signalDbm?.let { if (it < -90) LampState.ATTENTION else null },
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The tripped state — a fall, an SOS, a live emergency.
 *
 * Takes the whole frame and refuses to be a card among cards. Two rules govern
 * it, both inherited from the firmware's own behaviour on the wearable:
 *
 *  1. **Shady is absent here.** The mascot is deliberately serious about
 *     emergencies and simply does not appear on this screen. That rule is
 *     enforced at the `ShadyHost` boundary rather than by remembering to leave
 *     it out.
 *  2. **The dismissal is the prominent action, not the call.** The countdown
 *     is already going to place the call; what a person needs from this screen
 *     when they are fine is a fast, unmissable way to say so.
 */
@Composable
fun TripBanner(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    secondsRemaining: Int? = null,
    totalSeconds: Int? = null,
    onDismiss: (() -> Unit)? = null,
    dismissLabel: String = "I am OK",
    onEscalate: (() -> Unit)? = null,
    escalateLabel: String = "Call now"
) {
    val colors = MaterialTheme.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .padding(Spacing.gutter),
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Radius.plate))
                .background(colors.lampTrip)
                .padding(vertical = Spacing.md),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.boardType.sectionPlate,
                color = if (colors.isDark) colors.ground else colors.plate
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (secondsRemaining != null && totalSeconds != null && totalSeconds > 0) {
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = secondsRemaining.toString(),
                style = MaterialTheme.boardType.countdown,
                color = colors.inkTrip,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    // Announced as it changes, so a screen-reader user hears
                    // the countdown rather than discovering it has expired.
                    .semantics { liveRegion = LiveRegionMode.Assertive }
            )
            Spacer(Modifier.height(Spacing.md))
            LinearProgressIndicator(
                progress = { secondsRemaining.toFloat() / totalSeconds.toFloat() },
                color = colors.lampTrip,
                trackColor = colors.recess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Stroke.heavy)
            )
        }

        Spacer(Modifier.height(Spacing.xxl))

        if (onDismiss != null) {
            BoardButton(
                label = dismissLabel,
                onClick = onDismiss,
                weight = ButtonWeight.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (onEscalate != null) {
            Spacer(Modifier.height(Spacing.md))
            BoardButton(
                label = escalateLabel,
                onClick = onEscalate,
                weight = ButtonWeight.DANGER,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * An empty bay on the panel.
 *
 * Named for the blanking plate that covers an unused way — the board is not
 * broken, that circuit simply has not been wired yet. Always offers the action
 * that fills it, because an empty state without a next step is a dead end.
 */
@Composable
fun EmptyBay(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    /**
     * Whether Shady keeps the bay company.
     *
     * On by default, because an empty bay is the one surface in the app that is
     * empty *by definition* and therefore the one place a character costs
     * nothing: there is no information for it to compete with, and "you have
     * not set this up yet" is a friendlier sentence with somebody saying it.
     *
     * Off wherever the emptiness is itself the bad news — an emergency card
     * that was never filled in, a contact list with nobody on it. Those want
     * the plain statement, not a companion.
     */
    withShady: Boolean = true,
    /**
     * Which face Shady wears while it waits, when [withShady] is on.
     *
     * Defaults to the plain [ShadyMood.CALM] so a call site that has not been
     * given a more specific answer keeps behaving exactly as before. Pick the
     * mood that matches what the emptiness actually means: [ShadyMood.LOOKING]
     * for a list that could have content but does not yet,
     * [ShadyMood.CURIOUS] for something nobody has set up yet, and
     * [ShadyMood.DUMBFOUNDED] for a gap that is the viewer's own doing.
     */
    shadyMood: ShadyMood = ShadyMood.CALM
) {
    val colors = MaterialTheme.board
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radius.card))
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline, RoundedCornerShape(Radius.card))
            .padding(Spacing.xl)
    ) {
        if (withShady) {
            ShadyHost(
                mood = shadyMood,
                // An empty bay is never itself an emergency, and `ShadyHost`
                // enforces the suppression rule anyway if that ever changes.
                emergencyActive = false,
                size = 64.dp
            )
            Spacer(Modifier.height(Spacing.md))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.inkMuted,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.md))
            // A text action, not a bordered button. An outlined plate sitting
            // inside this recessed plate is a card inside a card, and the empty
            // state is the one place that reads worst — it is mostly frame
            // already.
            Text(
                text = actionLabel,
                style = MaterialTheme.boardType.nameplate,
                color = colors.ink,
                modifier = Modifier
                    .plateClickable(role = Role.Button, onClick = onAction)
                    .clip(RoundedCornerShape(Radius.tight))
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            )
        }
    }
}
