package com.safeshade.ui.screens.safety

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Shared parts for the Safety bank of screens.
 *
 * These are deliberately `internal` and deliberately live here rather than in
 * `ui/board/`. The board kit is the app's shared vocabulary and is frozen; a
 * header bar and a text field are neither state-carrying nor reused outside
 * this bank, so promoting them into the kit would grow the thing every other
 * screen has to read before it can start.
 *
 * Nothing in this file introduces a new card, button, switch or row — those
 * come from the kit. What is left here is one genuine gap (a text input), a
 * thin adapter onto the kit's shared header, and small formatting helpers that
 * would otherwise be copy-pasted across nine files and drift.
 */

// ============================================
// HEADER
// ============================================

/**
 * The top of a Safety screen.
 *
 * This used to draw its own row at `headlineSmall`, which is how Safety ended
 * up opening one size smaller than Device and Settings — nobody chose that,
 * it just happened one screen at a time. It is now a thin adapter onto
 * [ScreenHeader] so the whole app converges on one heading treatment, and it
 * survives only so that ten call sites in this bank do not have to change
 * shape to say the same thing.
 *
 * [onBack] is null for the hub, which is a bottom-bar destination and has
 * nowhere to go back to. Every pushed screen passes one — a Safety screen a
 * worried person cannot get out of is its own small emergency. That is exactly
 * why the tier can be inferred from it rather than asked for: in this bank,
 * having nowhere to go back to and being a tab root are the same fact, so a
 * separate tier argument would only be a second chance to get it wrong.
 *
 * This is also where this bank supplies its half of [ScreenHeader]'s
 * documented 20dp header gap. The pushed screens in this bank scroll a plain
 * `Column`, not a list rhythmed with `Arrangement.spacedBy`, so nothing else
 * here would ever contribute the other 16dp — which is exactly how those
 * screens ended up each hand-placing their own `Spacer` and drifting to
 * different values. Putting the `Spacer` here instead means those call sites
 * do not get to choose again.
 *
 * **This adapter is for `Column` screens only.** A screen whose list already
 * carries `spacedBy(Spacing.lg)` must call [ScreenHeader] directly, or it
 * takes the 16dp twice. An earlier revision of this note asserted the opposite
 * — that a list screen "works out to the same gap by the same arithmetic" —
 * and three screens were built on that reading and measured 44dp against
 * everyone else's 28dp for a whole release. Contacts, Emergency numbers and
 * Trip log now call [ScreenHeader] directly, as [SafetyScreen] already did.
 */
@Composable
internal fun PanelHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null
) {
    ScreenHeader(
        title = title,
        modifier = modifier,
        subtitle = subtitle,
        onBack = onBack,
        tier = if (onBack == null) ScreenTier.ROOT else ScreenTier.PUSHED
    )
    Spacer(Modifier.height(Spacing.lg))
}

// ============================================
// EXPLANATORY COPY
// ============================================

/**
 * One short line of orientation.
 *
 * It used to be a paragraph, and there used to be eight of them on a screen.
 * The instinct behind that was right — every switch here changes what happens
 * to a person in an emergency, and a toggle whose consequence is not spelled
 * out is a toggle nobody dares to touch — but three sentences above the
 * controls are read once and scrolled past forever after.
 *
 * So the consequence now rides on the row itself, as a `Way`'s `detail`, where
 * it sits beside the thing it describes; the long form goes behind a
 * `WhyDisclosure`, available to the guardian who is setting this up for
 * somebody else and out of the way of everyone else. What is left for this is
 * a single sentence naming what a section is: keep it under about fifteen
 * words, and if it will not fit, that is the signal it belongs in a
 * disclosure.
 */
@Composable
internal fun Note(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.board.inkMuted,
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * A warning that something did not happen.
 *
 * Rendered in the trip ink because it carries a real state meaning, and always
 * with the words to match — colour is never the only carrier. See
 * `EmergencyActions`, which returns an `ActionResult` precisely so that a
 * failed dial or a missing permission has somewhere visible to land.
 */
@Composable
internal fun FailureNote(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.inkTrip,
        modifier = modifier.fillMaxWidth()
    )
}

// ============================================
// TEXT SANITISING
// ============================================

/**
 * Removes the two characters the firmware's parser cannot survive.
 *
 * The device splits every payload on `,` positionally with no escaping, so one
 * comma in Allergies shifts age into Conditions and everything after it — the
 * kind of failure that produces a wrong medical card rather than an error.
 * Newlines break the same way in the paths that read a line at a time.
 *
 * This runs as the user types so that what is on screen is what the device
 * gets. `DeviceProtocol.clean` does the same job plus whitespace collapsing at
 * send time; that fuller version is not used here because trimming mid-typing
 * would eat the space someone is in the middle of pressing.
 */
internal fun stripDeviceDelimiters(input: String): String =
    input.replace(',', ' ').replace('\n', ' ').replace('\r', ' ')

/** Digits only, for age. Empty maps to zero. */
internal fun parseAge(input: String): Int =
    input.filter { it.isDigit() }.take(3).toIntOrNull()?.coerceIn(0, 130) ?: 0

/**
 * Light phone validation: not empty, and nothing in it that a dialer would
 * choke on.
 *
 * Deliberately permissive. Indian numbers are written with and without +91,
 * with spaces, with hyphens; a strict pattern would reject a number that dials
 * perfectly well, and a rejected emergency contact is worse than an odd-looking
 * one. The only real failures are an empty field and a field with letters in it.
 */
internal fun isPlausiblePhone(input: String): Boolean {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return false
    if (trimmed.count { it.isDigit() } < 3) return false
    return trimmed.all { it.isDigit() || it == '+' || it == ' ' || it == '-' || it == '(' || it == ')' }
}

// ============================================
// TIME
// ============================================

private val timeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy")

/** The wall-clock time of an event, in the phone's own locale format. */
internal fun formatTimeOfDay(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(timeFormatter)

internal fun localDateOf(epochMillis: Long): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()

/**
 * A day heading for the trip log.
 *
 * "Today" and "Yesterday" earn their place: those are the two days a guardian
 * is actually reading about, and a date there makes them do arithmetic to find
 * out whether the thing they are worried about happened this morning.
 */
internal fun formatDayHeading(day: LocalDate, today: LocalDate): String = when (day) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> day.format(dateFormatter)
}

/** A full timestamp, for the one screen that shows a single event in detail. */
internal fun formatFullTimestamp(epochMillis: Long, today: LocalDate): String {
    val day = localDateOf(epochMillis)
    return "${formatDayHeading(day, today)}, ${formatTimeOfDay(epochMillis)}"
}

/** "30 seconds" / "1 minute" / "5 minutes", never "1 minutes". */
internal fun formatDuration(seconds: Int): String = when {
    seconds < 60 -> "$seconds seconds"
    seconds == 60 -> "1 minute"
    seconds % 60 == 0 -> "${seconds / 60} minutes"
    else -> "${seconds / 60} minutes ${seconds % 60} seconds"
}

// ============================================
// READ-ONLY DETAIL
// ============================================

/** A small inline row of label and value, for read-only detail lines. */
@Composable
internal fun DetailLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
    ) {
        // A hard 112dp label column doesn't survive a raised font scale: a
        // label like "Temperature" or "Device said" no longer fits at that
        // width once text is scaled up, and wraps inside the fixed box while
        // the value column (which does get to grow) sits there unused. A
        // minimum instead of a fixed width keeps short labels aligned at the
        // default scale but lets the column grow with the label's own text.
        Box(modifier = Modifier.widthIn(min = 112.dp)) {
            Nameplate(label, small = true, muted = true)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.ink,
            modifier = Modifier.weight(1f)
        )
    }
}
