package com.safeshade.ui.screens.device

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.data.LedPattern
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.BusTick
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.rowClickable
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.BoardColors
import com.safeshade.ui.theme.Motion
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Everything the lights screen draws. */
data class LightsUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val pattern: LedPattern = LedPattern.TORCH,
    /** Written to `LED_CHAR` and still waiting for its ack. */
    val inFlightPattern: LedPattern? = null,
    val ack: AckState = AckState.IDLE
)

/**
 * The wearable's LED ring.
 *
 * Every pattern is shown running, on a ring of eight lamps that matches the
 * hardware's own `NUM_LEDS`. An earlier version of this screen deliberately
 * refused to do that, on the grounds that four saturated preview chips would
 * put more colour on one screen than the rest of the app owns, and that colour
 * in this system means "a circuit is live". That objection was real, and it is
 * answered here rather than ignored:
 *
 *  - A swatch is not decoration borrowing a state hue. It is a **picture of the
 *    lamp**, and this is the one screen in the app where saturation is the
 *    subject rather than a signal. Every swatch sits inside a dark lens well
 *    that appears nowhere else, which is what marks it as a depicted light
 *    instead of a lit control.
 *  - Status is still carried entirely by the board's own vocabulary beside it:
 *    the state word, the bus tick, and the border weight on the chosen swatch.
 *    Take every colour out of the swatches and the screen still says which
 *    pattern is running.
 *
 * What it buys is that a pattern gets picked by seeing it. "Cyber" and "Pulse"
 * are not words anyone can rank without trying both on a device in the dark.
 *
 * The swatches are driven from the firmware, not from invention — see
 * [ledColor] and [cycleMillis], which transcribe `updateRGBPattern()` in
 * docs/SafeShadev21/SafeShadev21.ino channel for channel and millisecond for
 * millisecond. Two of the descriptions this screen used to show were wrong
 * against that source, which is exactly the failure a live preview cannot have.
 */
@Composable
fun LightsScreen(
    state: LightsUiState,
    onSelectPattern: (LedPattern) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(
                title = "Lights",
                onBack = onBack
            )
        }

        // No queued-write promise here, unlike mode and settings. An LED
        // pattern is a direct characteristic write with nothing behind it that
        // replays on reconnect, so telling the user it would be sent later
        // would be inventing a mechanism that does not exist.
        item("offline") { OfflineNotice(connection = state.connection) }

        item("preview") {
            PatternPreview(
                // The in-flight pattern when there is one, because that is the
                // choice the user just made and the one they are looking to
                // see. The readout's lamp tint says it is not agreed yet.
                pattern = state.inFlightPattern ?: state.pattern,
                ack = state.ack,
                sending = state.inFlightPattern != null,
                linkUsable = state.connection.isUsable
            )
        }

        item("patterns-heading") { SectionPlate(title = "Pattern") }

        item("patterns") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                LedPattern.entries.forEachIndexed { index, pattern ->
                    if (index > 0) Hairline()
                    PatternWay(
                        pattern = pattern,
                        isSelected = state.pattern == pattern,
                        inFlight = state.inFlightPattern == pattern,
                        linkUsable = state.connection.isUsable,
                        ack = state.ack,
                        onClick = { onSelectPattern(pattern) }
                    )
                }
            }
        }

        // The master on/off is not here because it cannot be here: the firmware
        // has no characteristic for it. Saying that plainly is better than a
        // switch at the top of this screen that does nothing.
        item("master-note") {
            BoardPlate(modifier = Modifier.fillMaxWidth(), recessed = true) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Nameplate("Turning the lights on and off", small = true, muted = true)
                    Text(
                        text = "The master switch for the LED ring is set on the " +
                            "wearable itself, under Settings › Lights. This app can " +
                            "choose the pattern but cannot switch the ring on or off.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkMuted
                    )
                    Text(
                        text = "If a pattern is confirmed here and nothing happens on " +
                            "the device, that switch is the first thing to check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }

    }
}

// ============================================================================
// The chosen pattern, at size
// ============================================================================

/**
 * The large readout for the pattern that is set.
 *
 * Follows the zone-radius control's shape: a value stated big, the thing itself
 * at a size worth looking at, and a line of plain English underneath that
 * changes with it.
 *
 * The lamp tint on the readout comes from [ackLamp], so a pattern that has been
 * written but not acknowledged cannot sit here in confident teal while the
 * device may never have heard the write.
 */
@Composable
private fun PatternPreview(
    pattern: LedPattern,
    ack: AckState,
    sending: Boolean,
    linkUsable: Boolean
) {
    val colors = MaterialTheme.board
    BoardPlate(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(Spacing.lg)
                // One stop, like a way row. Left alone this plate is five
                // separate focus stops, three of which say the pattern's name,
                // because the swatch, the readout's label, its value, the
                // description and the status line are all separately spoken.
                .clearAndSetSemantics {
                    contentDescription = "Pattern, " + pattern.label + ", " +
                        describe(pattern) + ", " + previewNote(ack, sending, linkUsable)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            LedRingSwatch(
                pattern = pattern,
                diameter = HERO_SWATCH,
                selected = false
            )
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Readout(
                    label = "Pattern",
                    value = pattern.label,
                    large = true,
                    state = if (sending) ackLamp(ack, LampState.LIVE) else null
                )
                Spacer(Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.Top) {
                    // The per-pattern glyph, kept from the old row layout. It is
                    // the one identifier on this screen that survives greyscale
                    // completely, so it earns a place beside the description
                    // even though the swatch has taken the leading position.
                    Icon(
                        imageVector = iconFor(pattern),
                        contentDescription = null,
                        tint = colors.inkFaint,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(14.dp)
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = describe(pattern),
                        style = MaterialTheme.boardType.rowDetail,
                        color = colors.inkMuted
                    )
                }
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = previewNote(ack, sending, linkUsable),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }
    }
}

/**
 * The honest one-liner under the preview.
 *
 * "Running on the wearable" is only claimed while the link can actually say so.
 * Offline, all this app knows is what it last sent from this phone.
 */
private fun previewNote(ack: AckState, sending: Boolean, linkUsable: Boolean): String = when {
    sending -> ackWord(ack) ?: "Sending to the device"
    ack == AckState.CONFIRMED -> "Confirmed by the device"
    linkUsable -> "Running on the wearable now"
    else -> "Last set from this phone"
}

// ============================================================================
// One pattern in the picker
// ============================================================================

/**
 * A way row whose leading mark is the pattern running rather than a glyph.
 *
 * Deliberately not `Way`: that composable takes an `ImageVector`, and the whole
 * point here is a live swatch in the place a glyph would occupy. Everything
 * else — the nameplate, the detail line, the uppercase state word, the bus tick
 * on the right edge, the tonal press with no ripple — is the same anatomy, so
 * the row still reads as one of the board's rows.
 *
 * Selection is carried three ways, none of them hue: the swatch's border steps
 * from a hairline to the heavy stroke, the state word changes, and the bus tick
 * lights. That matters more here than anywhere else in the app, because every
 * swatch on this screen is already made of colour.
 */
@Composable
private fun PatternWay(
    pattern: LedPattern,
    isSelected: Boolean,
    inFlight: Boolean,
    linkUsable: Boolean,
    ack: AckState,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.board

    val lamp = when {
        inFlight -> ackLamp(ack, LampState.LIVE)
        // Only claim a pattern is running while the link can actually tell us.
        // Offline, all we know is what was last set from this phone.
        isSelected && linkUsable -> LampState.LIVE
        isSelected -> LampState.UNKNOWN
        else -> LampState.OFF
    }
    val word = when {
        inFlight -> "Sending"
        isSelected && linkUsable -> "Running"
        isSelected -> "Last set"
        else -> "Off"
    }
    val detail = when {
        inFlight -> ackWord(ack) ?: describe(pattern)
        isSelected && ack == AckState.CONFIRMED -> "Confirmed by the device"
        else -> describe(pattern)
    }

    // One semantic node for the whole row, as everywhere else on the board.
    // Announcing swatch, nameplate, detail and state word separately would read
    // as four fragments where a sighted user takes in one line — and the
    // swatch, which is the entire point of the row visually, has nothing to say
    // beyond the name a blind user is already being given.
    val spoken = pattern.label + ", " + word + ", " + detail

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .rowClickable(role = Role.RadioButton, onClick = onClick)
            .padding(start = Spacing.lg, end = Spacing.md, top = Spacing.md, bottom = Spacing.md)
            // Intrinsic height so the bus tick can match whatever the row
            // actually occupies, exactly as `Way` does it.
            .height(IntrinsicSize.Min)
            .clearAndSetSemantics {
                contentDescription = spoken
                // `rowClickable` wraps `clickable`, not `selectable`, so the
                // selected state is not set for us. Without this line a screen
                // reader is handed seven radio buttons and no way to hear which
                // one is on.
                selected = isSelected
            }
    ) {
        LedRingSwatch(
            pattern = pattern,
            diameter = ROW_SWATCH,
            selected = isSelected
        )
        Spacer(Modifier.width(Spacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Nameplate(pattern.label)
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = detail,
                style = MaterialTheme.boardType.rowDetail,
                color = colors.inkFaint
            )
        }

        Spacer(Modifier.width(Spacing.md))
        Text(
            text = word.uppercase(),
            style = MaterialTheme.boardType.stateLabel,
            color = when (lamp) {
                LampState.LIVE -> colors.inkLive
                LampState.ATTENTION -> colors.inkAttention
                LampState.TRIP -> colors.inkTrip
                LampState.OFF, LampState.UNKNOWN -> colors.inkFaint
            }
        )
        Spacer(Modifier.width(Spacing.md))
        BusTick(state = lamp, modifier = Modifier.fillMaxHeight())
    }
}

// ============================================================================
// The swatch
// ============================================================================

/**
 * Eight lamps in a ring, running the pattern.
 *
 * Eight because the wearable has eight (`NUM_LEDS` in the firmware), and
 * because at that count the *shape* of a pattern is legible without colour:
 * Police lights half the ring at a time, Cyber is a three-lamp comet on an
 * otherwise dark ring, Torch is all eight flat out, Ocean is a wave with a
 * visible trough travelling round. Those silhouettes are what keeps the set
 * separable in greyscale, and for a reader who cannot tell violet from blue.
 *
 * The well behind the lamps is dark in both themes — see [lensWell]. An LED
 * ring's substrate is dark, a white Torch is invisible on a bone plate, and
 * that darkness is also what separates a depicted light from a lit control.
 *
 * Motion comes from [rememberInfiniteTransition], which Compose suspends along
 * with the rest of the frame clock when this screen is not showing; a
 * `while (true)` in a `LaunchedEffect` would leave eight of these burning
 * battery behind whatever the user opened next. The phase is read inside the
 * draw lambda rather than during composition, so a frame costs a redraw and not
 * a recomposition.
 */
@Composable
private fun LedRingSwatch(
    pattern: LedPattern,
    diameter: Dp,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val transition = rememberInfiniteTransition(label = "led-" + pattern.name)
    val phase: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = cycleMillis(pattern), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    // Weight, not hue. The selected swatch is ringed in ink at the heavy stroke
    // the board reserves for a bus tick; the rest sit behind a hairline.
    val borderWeight by animateDpAsState(
        targetValue = if (selected) Stroke.heavy else Stroke.hairline,
        animationSpec = tween(Motion.normal),
        label = "swatch-border"
    )

    Canvas(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(lensWell(colors))
            .border(
                width = borderWeight,
                color = if (selected) colors.ink else colors.hairline,
                shape = CircleShape
            )
            .semantics {
                contentDescription = pattern.label + " pattern, shown running"
            }
    ) {
        drawLedRing(pattern = pattern, phase = phase.value)
    }
}

/**
 * The dark backing a swatch sits in.
 *
 * Dark under both themes on purpose — see [LedRingSwatch]. On the light panel
 * that is the ink token, which is the emblem's charcoal; on the dark panel it
 * is the recess, which is already near black. Neither is a state colour, and
 * neither is being asked to carry meaning beyond "this is unlit".
 */
private fun lensWell(colors: BoardColors): Color =
    if (colors.isDark) colors.recess else colors.ink

/** The eight lamps, each drawn as an unlit body with its emission over it. */
private fun DrawScope.drawLedRing(pattern: LedPattern, phase: Float) {
    val ringRadius = size.minDimension * 0.30f
    val lampRadius = size.minDimension * 0.085f
    val body = Color.White.copy(alpha = 0.07f)

    for (index in 0 until LED_COUNT) {
        val angle = (index / LED_COUNT.toFloat()) * TWO_PI - HALF_PI
        val at = Offset(
            x = center.x + cos(angle) * ringRadius,
            y = center.y + sin(angle) * ringRadius
        )

        // The unlit lamp is always drawn, so the ring keeps its shape through
        // the dark half of Police and the trough of Ocean. A ring that loses
        // its geometry every 150ms reads as a rendering fault.
        drawCircle(color = body, radius = lampRadius, center = at)

        val emitted = ledColor(pattern, index, phase)
        // An LED's brightness is its own colour scaled down, so the swatch
        // separates the two back out: the hue at full strength, painted at the
        // alpha the firmware's value implies. Painting the dimmed colour
        // directly would smear the lamp bodies dark rather than dim them.
        val strength = max(emitted.red, max(emitted.green, emitted.blue))
        if (strength <= 0.02f) continue
        val hue = Color(
            red = emitted.red / strength,
            green = emitted.green / strength,
            blue = emitted.blue / strength
        )
        // A cheap bloom: one oversized disc under the core. Real blur needs
        // RenderEffect, which is API 31, and this app runs from 26.
        drawCircle(color = hue, radius = lampRadius * 2.2f, center = at, alpha = strength * 0.18f)
        drawCircle(color = hue, radius = lampRadius, center = at, alpha = strength)
    }
}

/**
 * What lamp [index] is emitting at [phase] through the pattern's own cycle.
 *
 * A transcription of `updateRGBPattern()` in the firmware,
 * docs/SafeShadev21/SafeShadev21.ino, kept literal — the same channel values,
 * the same arithmetic, the same unlit lamps — so the preview cannot drift away
 * from the device it claims to show. If a pattern changes on the wearable, this
 * function is the thing that changes with it.
 *
 * Torch is the one that cannot be transcribed exactly: on the device its level
 * is mapped from the ambient light sensor, 255 in the dark down to 90 in bright
 * sun. The swatch shows it near the top of that range, which is where it sits
 * whenever a torch is worth having.
 */
private fun ledColor(pattern: LedPattern, index: Int, phase: Float): Color = when (pattern) {
    LedPattern.TORCH -> level(TORCH_LEVEL, TORCH_LEVEL, TORCH_LEVEL)

    LedPattern.RAINBOW -> hueWheel((index / LED_COUNT.toFloat()) + phase)

    LedPattern.CYBER -> {
        val head = (phase * LED_COUNT).toInt().coerceIn(0, LED_COUNT - 1)
        when (((index - head) + LED_COUNT) % LED_COUNT) {
            0 -> level(150, 0, 255)
            1 -> level(80, 0, 150)
            2 -> level(30, 0, 80)
            else -> Color.Black
        }
    }

    LedPattern.POLICE ->
        if (phase < 0.5f) {
            if (index < LED_COUNT / 2) level(255, 0, 0) else Color.Black
        } else {
            if (index >= LED_COUNT / 2) level(0, 0, 255) else Color.Black
        }

    LedPattern.FIRE -> {
        val step = (phase * FIRE_STEPS).toInt().coerceIn(0, FIRE_STEPS - 1)
        // The firmware rolls two dice per lamp per step: one picks the bright
        // ember or the banked one, the other picks how yellow it is. Here they
        // are a hash of lamp and step rather than random(), so the flicker is
        // irregular but the loop still closes on itself.
        val whichEmber = noise(index, step)
        val warmth = noise(index, step + FIRE_SALT)
        if (whichEmber > 0.3f) level(255, (50 + warmth * 100).toInt(), 0)
        else level(200, (20 + warmth * 60).toInt(), 0)
    }

    LedPattern.OCEAN -> {
        val wave = sin(phase * TWO_PI + index * 0.8f) * 127f + 128f
        level(0, (wave / 2f).toInt(), wave.toInt())
    }

    LedPattern.PULSE -> {
        val bright = (sin(phase * TWO_PI) * 127f + 128f).toInt()
        level(bright, bright / 2, bright)
    }
}

/**
 * How long one loop of a pattern takes, in milliseconds, from the firmware.
 *
 * Rainbow advances its hue by 30 of 65536 per millisecond, so a whole wheel is
 * 2185ms. Ocean and Pulse are sines of `tick / 200` and `tick / 500`, whose
 * periods are two pi times those. Cyber steps a lamp every 100ms round eight of
 * them, and Police swaps halves every 150ms on the device - though the swatch
 * deliberately runs that one at half speed; see [cycleMillis]. Torch does not move at all; it has
 * a cycle only because the animation is unconditional, and nothing in its
 * colours reads the phase.
 */
private fun cycleMillis(pattern: LedPattern): Int = when (pattern) {
    LedPattern.TORCH -> 2000
    LedPattern.RAINBOW -> 2185
    LedPattern.CYBER -> 800
    // 600, not the firmware's 300. This is the one place the swatch is
    // deliberately not millisecond-faithful to the device.
    //
    // At 300 the ring alternates saturated red and blue 3.3 times a second.
    // WCAG 2.3.1 draws its line at three flashes per second and singles out
    // *red* flashes for a stricter threshold, which is exactly this pattern.
    // The swatch is small enough to sit under the general and red flash
    // thresholds on area alone, so it is arguably compliant either way - but
    // "arguably compliant" is not the standard to hold an app to when its
    // stated audience includes elderly users and it may be left on screen.
    //
    // What a swatch is for is recognising a pattern by its colour and
    // character, and half speed costs neither. The description beside it still
    // states the device's real rate.
    LedPattern.POLICE -> 600
    LedPattern.FIRE -> FIRE_STEPS * 150
    LedPattern.OCEAN -> 1257
    LedPattern.PULSE -> 3142
}

/** A firmware channel triple as a colour. */
private fun level(red: Int, green: Int, blue: Int): Color = Color(
    red = red.coerceIn(0, 255),
    green = green.coerceIn(0, 255),
    blue = blue.coerceIn(0, 255)
)

/**
 * A point on the colour wheel, [fraction] being a whole turn.
 *
 * Hand-rolled rather than pulled from a colour utility so it matches the
 * NeoPixel `ColorHSV` call at full saturation and value, which is what the
 * firmware asks for.
 */
private fun hueWheel(fraction: Float): Color {
    val turned = ((((fraction % 1f) + 1f) % 1f)) * 6f
    val sector = turned.toInt() % 6
    val rise = turned - turned.toInt()
    return when (sector) {
        0 -> Color(1f, rise, 0f)
        1 -> Color(1f - rise, 1f, 0f)
        2 -> Color(0f, 1f, rise)
        3 -> Color(0f, 1f - rise, 1f)
        4 -> Color(rise, 0f, 1f)
        else -> Color(1f, 0f, 1f - rise)
    }
}

/**
 * A repeatable stand-in for the firmware's `random()`, in 0..1.
 *
 * Fire needs values that look unrelated from one lamp and one step to the next,
 * but a loop that runs for as long as this screen is open also has to close on
 * itself. Hashing the coordinates gives both: the same lamp gets the same ember
 * each time the loop comes round, and no two neighbours share one.
 */
private fun noise(index: Int, step: Int): Float {
    var hash = index * 374761393 + step * 668265263
    hash = (hash xor (hash shr 13)) * 1274126177
    return ((hash xor (hash shr 16)) and 0x7FFFFFFF) / 2147483647f
}

private const val LED_COUNT = 8
private const val FIRE_STEPS = 12
private const val FIRE_SALT = 977
private const val TORCH_LEVEL = 220
private const val TWO_PI = (2.0 * PI).toFloat()
private const val HALF_PI = (PI / 2.0).toFloat()
private val ROW_SWATCH = 44.dp
private val HERO_SWATCH = 76.dp

/**
 * What each pattern looks like, in words.
 *
 * The swatch beside these does the showing, so the words are free to carry what
 * a swatch cannot: how fast, and what the ring is doing that a 44dp disc might
 * flatten. They are also checked against the firmware now, and two were not.
 * Cyber was described as "a fast sweep between two cool tones" when it is a
 * single violet comet on an otherwise dark ring; Ocean as "a wash between blue
 * and green" when its hue never leaves blue and it is the brightness that
 * travels.
 *
 * Torch's headlamp note earns its words: it is the only pattern that calls
 * `setHeadlamp(true)`, so choosing any other pattern turns the wearable's front
 * lamp off as a side effect, and nothing else in this app says so.
 */
private fun describe(pattern: LedPattern): String = when (pattern) {
    LedPattern.TORCH -> "All eight steady white, and the only pattern that also " +
        "switches the headlamp on"
    LedPattern.RAINBOW -> "The whole spectrum wrapped round the ring, turning once " +
        "every two seconds"
    LedPattern.CYBER -> "A violet comet with a fading tail, chasing round a dark ring"
    LedPattern.POLICE -> "Half the ring red, then the other half blue, three times a second"
    LedPattern.FIRE -> "Every lamp flickering its own amber, like embers"
    LedPattern.OCEAN -> "A blue swell travelling round the ring, brightening and fading"
    LedPattern.PULSE -> "One pink glow breathing in and out, roughly every three seconds"
}

/**
 * The glyph for each pattern.
 *
 * It no longer leads the row — the live swatch does — but it stays as the
 * colourless identifier beside the preview's description, which is the one mark
 * on this screen that survives being printed in black and white. Police has its
 * own glyph in the custom set; an older comment here claimed it fell back to
 * the shared lights glyph, and had been out of date since that glyph was drawn.
 */
private fun iconFor(pattern: LedPattern) = when (pattern) {
    LedPattern.TORCH -> SafeShadeIcons.Torch
    LedPattern.RAINBOW -> SafeShadeIcons.Rainbow
    LedPattern.CYBER -> SafeShadeIcons.Cyber
    LedPattern.POLICE -> SafeShadeIcons.Police
    LedPattern.FIRE -> SafeShadeIcons.Fire
    LedPattern.OCEAN -> SafeShadeIcons.Ocean
    LedPattern.PULSE -> SafeShadeIcons.Pulse
}

// ============================================================================
// Previews
// ============================================================================

@Composable
private fun LightsPreviewHost(state: LightsUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        LightsScreen(state = state, onSelectPattern = {})
    }
}

@Preview(name = "Lights · light", showBackground = true)
@Composable
private fun LightsPreviewLight() {
    SafeShadeTheme(darkTheme = false) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Ready,
                pattern = LedPattern.PULSE,
                ack = AckState.CONFIRMED
            )
        )
    }
}

@Preview(name = "Lights · dark", showBackground = true)
@Composable
private fun LightsPreviewDark() {
    SafeShadeTheme(darkTheme = true) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Ready,
                pattern = LedPattern.TORCH,
                inFlightPattern = LedPattern.FIRE,
                ack = AckState.PENDING
            )
        )
    }
}

@Preview(name = "Lights · no reply", showBackground = true)
@Composable
private fun LightsPreviewNoReply() {
    SafeShadeTheme(darkTheme = false) {
        LightsPreviewHost(
            LightsUiState(
                connection = ConnectionState.Disconnected,
                pattern = LedPattern.OCEAN,
                inFlightPattern = LedPattern.POLICE,
                ack = AckState.NO_RESPONSE
            )
        )
    }
}
