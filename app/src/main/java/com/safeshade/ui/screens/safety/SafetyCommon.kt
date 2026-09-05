package com.safeshade.ui.screens.safety

import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
import com.safeshade.ui.board.Way
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
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
// SELECTION
// ============================================

/**
 * One choice in a mutually exclusive set, as a way.
 *
 * A selected option reads as a lit circuit rather than as a radio dot, which
 * is the same vocabulary the role fork already uses. It costs nothing to learn
 * and it survives greyscale, because the lamp is always paired with a word.
 */
@Composable
internal fun OptionWay(
    name: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    selectedLabel: String = "In use",
    // "Not chosen", never "Off". `Way` wipes the subtree semantics and rebuilds
    // the spoken line as "NAME, STATELABEL", so this string is the whole of
    // what a screen-reader user hears about an unselected option — and "30
    // seconds, off" says the countdown is disabled when it only means this
    // length is not the one in use.
    unselectedLabel: String = "Not chosen",
    sealed: Boolean = false
) {
    Way(
        name = name,
        state = if (selected) LampState.LIVE else LampState.OFF,
        stateLabel = if (selected) selectedLabel else unselectedLabel,
        detail = detail,
        sealed = sealed,
        onClick = onSelect,
        modifier = modifier
    )
}

// ============================================
// INPUT
// ============================================

/**
 * A text input, drawn as a routed channel in the panel.
 *
 * Built on `BasicTextField` rather than Material's `TextField` for one
 * structural reason: the stock field owns a container colour, an animated
 * indicator line and a floating label, all of which are Material's visual
 * language rather than this one. Restyling it into a flat recess means
 * overriding roughly twenty colour slots and still fighting the label
 * animation. Composing the recess directly is less code and cannot drift.
 *
 * [maxLength] is not cosmetic. The firmware parses the Medical ID payload
 * positionally against a negotiated MTU, and `DeviceProtocol.health` caps each
 * field on the way out — a field that lets someone type 200 characters into a
 * 40-character slot is a field that silently discards their work at send time.
 * The caps here mirror that function.
 */
@Composable
internal fun PlateField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    helper: String? = null,
    error: String? = null,
    maxLength: Int = 40,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    /** Formats the displayed text without changing what is stored. */
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    val colors = MaterialTheme.board
    val shape = RoundedCornerShape(Radius.plate)

    Column(modifier = modifier.fillMaxWidth()) {
        Nameplate(label, small = true, muted = true)
        Spacer(Modifier.height(Spacing.xs))

        // The recess is drawn inside `decorationBox` rather than around the
        // field, which is what makes the whole 48dp plate part of the text
        // field's own touch target. Wrapping the field in a Box instead would
        // leave the padding dead to a tap — a hairline-thin target for the
        // person this app is most often read by.
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it.take(maxLength)) },
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.ink),
            cursorBrush = SolidColor(colors.ink),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            visualTransformation = visualTransformation,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        // Grows with the font scale rather than clipping:
                        // heightIn sets a floor, not a fixed height.
                        .heightIn(min = Spacing.touchTarget)
                        .clip(shape)
                        .background(if (enabled) colors.recess else colors.ground)
                        .border(Stroke.hairline, colors.hairline, shape)
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                ) {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.inkFaint,
                            // Hidden from the semantics tree: a screen reader
                            // announcing the placeholder as well as the field
                            // reads the same box twice.
                            modifier = Modifier.clearAndSetSemantics {}
                        )
                    }
                    innerTextField()
                }
            }
        )

        // The counter only appears once it is nearly relevant. Showing "0 / 40"
        // under an empty field is noise, and it reads as a demand.
        val nearCap = value.length >= (maxLength * 3) / 4
        if (error != null || helper != null || nearCap) {
            Spacer(Modifier.height(Spacing.xs))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = error ?: helper.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) colors.inkTrip else colors.inkFaint,
                    modifier = Modifier.weight(1f)
                )
                if (nearCap) {
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "${value.length} / $maxLength",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }
    }
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
        Box(modifier = Modifier.width(112.dp)) {
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
