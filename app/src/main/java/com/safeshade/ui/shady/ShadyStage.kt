package com.safeshade.ui.shady

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Shady's own world, at the foot of the Board.
 *
 * The character stands on the bottom navigation bar's top rule and treats it as
 * ground: it wanders along it, stops to look around, hops, stretches, sits down
 * and swings its legs, dozes off, chases something off the edge and comes back
 * without it. None of it reports state and none of it is interactive chrome —
 * it is there because an app you open several times a day to check on someone
 * you are worried about is allowed to have one place that is simply pleasant.
 *
 * It earns its place by being *below the fold*: you only meet it having scrolled
 * past everything that matters, so it never competes with the board itself. The
 * status Shady in the card at the top has scrolled away by then, which is why
 * two instances never share the screen.
 *
 * Tapping it produces one of fourteen deliberately larger reactions, and never
 * the same one twice in a row.
 */
@Composable
fun ShadyStage(
    modifier: Modifier = Modifier,
    /**
     * How tall the stage is.
     *
     * Grows with [characterSize] and is not independent of it: the character is
     * bottom-aligned in a box of exactly this height, so a taller character in
     * an unchanged stage has its hops and stretches clipped off at the top.
     */
    height: Dp = 132.dp,
    characterSize: Dp = 100.dp
) {
    val reactor = rememberShadyReactor(ReactionStyle.BOLD)
    val isStatic = LocalInspectionMode.current

    // The director yields the moment a tap lands, so a beat that was halfway
    // across the stage stops there instead of teleporting to its target when
    // the reaction ends.
    val director = remember { ShadyIdleDirector(interrupted = { reactor.isReacting }) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .semantics {
                contentDescription = "Shady, the SafeShade mascot. Tap to play."
                onClick(label = "Play with Shady") { reactor.poke(); true }
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { reactor.poke() })
            }
    ) {
        val travel = maxWidth - characterSize

        // The idle loop. Held still in previews and screenshots so captured
        // evidence is deterministic.
        if (!isStatic) {
            LaunchedEffect(Unit) {
                while (true) {
                    // Stand aside while a tap reaction is playing; resume after.
                    if (reactor.isReacting) {
                        director.stand()
                        frameWait(120)
                        continue
                    }
                    director.playOne()
                }
            }
        }

        val pose = if (reactor.isReacting) {
            reactor.pose.copy(facingLeft = director.facingLeft)
        } else {
            director.pose
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = travel * director.walkT)
        ) {
            Shady(
                mood = ShadyMood.CALM,
                size = characterSize,
                pose = pose
            )
        }
    }
}

/**
 * Everything Shady does when nobody has touched it.
 *
 * Kept as a class with one function per beat rather than a `when` inside the
 * composable: sixteen inline branches is the point at which a loop stops being
 * readable, and naming each beat is most of what makes the set easy to add to.
 *
 * The three pieces of state — where it is along the ground, which way it is
 * turned, and what it is doing this instant — are Compose state, so the
 * composable recomposes off them exactly as it did when they were `remember`ed
 * locals.
 */
private class ShadyIdleDirector(private val interrupted: () -> Boolean) {
    var pose by mutableStateOf(ShadyPose.Neutral)
        private set

    /** Position along the ground, 0f at the left edge, 1f at the right. */
    var walkT by mutableFloatStateOf(0.5f)
        private set

    var facingLeft by mutableStateOf(false)
        private set

    private var last = -1

    /** Neutral, but still turned the way it was. Used while a tap plays out. */
    fun stand() {
        pose = ShadyPose(facingLeft = facingLeft)
    }

    suspend fun playOne() {
        val action = pick()
        last = action
        when (action) {
            0 -> amble()
            1 -> lookAround()
            2 -> hop()
            3 -> stretchAwake()
            4 -> sitAndSwing()
            5 -> doze()
            6 -> think()
            7 -> peekFromEdge()
            8 -> chase()
            9 -> tripAndRecover()
            10 -> doubleTake()
            11 -> sneeze()
            12 -> examineFloor()
            13 -> waveAtYou()
            14 -> shiver()
            else -> noticesYou()
        }

        // A beat of stillness between actions. Without it the character reads
        // as frantic rather than alive.
        stand()
        wait(500 + Random.nextInt(1400))
    }

    /**
     * Weighted pick that never repeats the previous action.
     *
     * Walking is the connective tissue, so it comes up most. The beats at the
     * bottom of the bag — dozing, peeking in from off-stage, and above all
     * being caught watching you — are rare enough that meeting one feels like
     * having been there at the right moment rather than like a rotation.
     */
    private fun pick(): Int {
        val bag = intArrayOf(
            0, 0, 0, 0, 0, 0,
            1, 1, 1, 1,
            2, 2, 3, 6, 6,
            8, 8, 10, 10, 12, 12, 13, 13,
            4, 9, 11, 14,
            5, 7, 15
        )
        var choice = bag[Random.nextInt(bag.size)]
        var guard = 0
        while (choice == last && guard++ < 8) choice = bag[Random.nextInt(bag.size)]
        return choice
    }

    // ============================================
    // The everyday beats
    // ============================================

    private suspend fun amble() {
        val target = (walkT + (Random.nextFloat() - 0.5f) * 0.6f).coerceIn(0.02f, 0.98f)
        walkTo(target)
    }

    private suspend fun lookAround() {
        set(ShadyPose(eyes = EyeStyle.SQUINT))
        wait(700)
        turn(!facingLeft)
        set(ShadyPose(eyes = EyeStyle.SQUINT))
        wait(700)
    }

    private suspend fun hop() {
        move(260, FastOutSlowInEasing) { t ->
            val lift = sin(t * PI).toFloat()
            set(
                ShadyPose(
                    offsetY = -0.18f * lift,
                    scaleY = 1f + 0.09f * lift,
                    scaleX = 1f - 0.06f * lift,
                    mouth = MouthStyle.BIG_SMILE
                )
            )
        }
    }

    private suspend fun stretchAwake() {
        // Starts heavy-lidded and finishes awake. A stretch that begins from a
        // neutral face is just a shape change.
        move(560, FastOutSlowInEasing) { t ->
            val e = sin(t * PI).toFloat()
            set(
                ShadyPose(
                    scaleY = 1f + 0.13f * e,
                    scaleX = 1f - 0.07f * e,
                    eyes = if (e > 0.5f) EyeStyle.CLOSED else EyeStyle.SLEEPY,
                    mouth = if (e > 0.4f) MouthStyle.GASP else MouthStyle.FLAT,
                    handsUp = e > 0.4f
                )
            )
        }
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE))
        wait(320)
    }

    private suspend fun sitAndSwing() {
        move(300, FastOutSlowInEasing) { t ->
            set(ShadyPose(scaleY = 1f - 0.12f * t, scaleX = 1f + 0.07f * t, mouth = MouthStyle.FLAT))
        }
        // Sitting still is a pause; sitting with its legs going is a character
        // waiting for something. The swing has to stay well under the 0.09 of
        // leg that clears the bottom of the body, or the rising leg vanishes
        // behind it and the pair reads as blinking rather than swinging.
        move(2600, LinearEasing) { t ->
            val swing = sin(t * 3.5f * 2f * PI).toFloat()
            set(
                ShadyPose(
                    scaleY = 0.88f,
                    scaleX = 1.07f,
                    rotation = swing * 1.6f,
                    legSwing = swing * 0.030f,
                    eyes = EyeStyle.SQUINT,
                    mouth = MouthStyle.SMILE
                )
            )
        }
        move(320, FastOutSlowInEasing) { t ->
            set(ShadyPose(scaleY = 1f - 0.12f * (1f - t), scaleX = 1f + 0.07f * (1f - t)))
        }
    }

    private suspend fun doze() {
        set(
            ShadyPose(
                scaleY = 0.94f,
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.FLAT,
                flourish = Flourish.SLEEP
            )
        )
        wait(2600)
    }

    private suspend fun think() {
        set(ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.FLAT, flourish = Flourish.THINK))
        wait(1600)
    }

    // ============================================
    // The beats that use the whole stage
    // ============================================

    private suspend fun peekFromEdge() {
        // Leaves the stage completely before coming back, so the return reads
        // as a return rather than as a lean.
        val edge = if (walkT > 0.5f) 0.98f else 0.02f
        val outward = if (edge > 0.5f) 1f else -1f
        walkTo(edge)
        move(240, FastOutSlowInEasing) { t -> set(ShadyPose(offsetX = outward * 0.85f * t)) }
        wait(800)

        // Turns to face back into the stage, and only the leading edge of it
        // comes into view. The gaze is aimed forward, which the mirror turns
        // into "toward the middle of the screen" whichever side it is on.
        turn(outward > 0f)
        move(420, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    offsetX = outward * (0.85f - 0.52f * t),
                    eyes = EyeStyle.WIDE,
                    gazeX = 0.9f,
                    mouth = MouthStyle.FLAT
                )
            )
        }
        wait(950)
        move(380, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    offsetX = outward * 0.33f * (1f - t),
                    eyes = EyeStyle.NORMAL,
                    mouth = MouthStyle.SMILE
                )
            )
        }
    }

    private suspend fun chase() {
        // Something goes past. It follows, loses whatever it was, thinks about
        // it, and walks back — never catching anything is the joke.
        val start = walkT
        val away = if (walkT > 0.5f) 0.02f else 0.98f
        turn(away < walkT)
        set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN))
        wait(220)

        walkTo(away, baseMs = 380, perScreenMs = 1000, strideMs = 105f, eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN)
        set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN, flourish = Flourish.PUFF))
        wait(420)
        set(ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.FLAT, flourish = Flourish.THINK))
        wait(1200)
        walkTo(start)
    }

    private suspend fun tripAndRecover() {
        // The stumble needs a stride to interrupt, so it walks into it.
        val ahead = (walkT + if (facingLeft) -0.30f else 0.30f).coerceIn(0.04f, 0.96f)
        walkTo(ahead)

        // Rotation is screen space, so falling forward is clockwise going right
        // and anticlockwise going left.
        val forward = if (facingLeft) -1f else 1f
        move(170, LinearEasing) { t ->
            set(
                ShadyPose(
                    offsetY = 0.02f * t,
                    rotation = forward * 30f * t,
                    eyes = EyeStyle.WIDE,
                    mouth = MouthStyle.OPEN,
                    handsUp = true
                )
            )
        }
        move(160, LinearEasing) { t ->
            set(
                ShadyPose(
                    rotation = forward * (30f - 12f * t),
                    eyes = EyeStyle.WIDE,
                    mouth = MouthStyle.OPEN,
                    handsUp = true,
                    flourish = Flourish.PUFF
                )
            )
        }
        wait(200)
        move(460, Settle) { t ->
            set(
                ShadyPose(
                    rotation = forward * 18f * (1f - t),
                    eyes = EyeStyle.SQUINT,
                    mouth = MouthStyle.WOBBLE
                )
            )
        }
        wait(240)
    }

    private suspend fun doubleTake() {
        val was = facingLeft
        turn(!was)
        set(ShadyPose(mouth = MouthStyle.FLAT))
        wait(560)
        turn(was)
        val forward = if (was) -1f else 1f
        move(160, Settle) { t ->
            set(
                ShadyPose(
                    rotation = forward * -6f * (1f - t),
                    eyes = EyeStyle.WIDE,
                    mouth = MouthStyle.GASP,
                    flourish = Flourish.SPARK
                )
            )
        }
        wait(600)
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE))
        wait(300)
    }

    private suspend fun sneeze() {
        move(400, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    rotation = -10f * t,
                    scaleY = 1f + 0.05f * t,
                    eyes = EyeStyle.SCRUNCH,
                    mouth = MouthStyle.GASP,
                    browRaise = 0.6f * t
                )
            )
        }
        move(90, LinearEasing) { t ->
            set(
                ShadyPose(
                    rotation = -10f + 26f * t,
                    scaleY = 1f - 0.09f * t,
                    scaleX = 1f + 0.07f * t,
                    eyes = EyeStyle.SCRUNCH,
                    mouth = MouthStyle.OPEN,
                    flourish = Flourish.PUFF
                )
            )
        }
        wait(160)
        move(460, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    rotation = 16f * (1f - t),
                    scaleY = 1f - 0.09f * (1f - t),
                    scaleX = 1f + 0.07f * (1f - t),
                    eyes = EyeStyle.SLEEPY,
                    mouth = MouthStyle.FLAT
                )
            )
        }
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE))
        wait(300)
    }

    private suspend fun examineFloor() {
        // Tips forward from the feet rather than crouching straight down: the
        // lean is what makes it read as looking at something.
        val forward = if (facingLeft) -1f else 1f
        move(420, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    rotation = forward * 14f * t,
                    scaleY = 1f - 0.07f * t,
                    scaleX = 1f + 0.04f * t,
                    eyes = EyeStyle.NORMAL,
                    mouth = MouthStyle.FLAT,
                    gazeY = 0.9f * t
                )
            )
        }
        set(
            ShadyPose(
                rotation = forward * 14f,
                scaleY = 0.93f,
                scaleX = 1.04f,
                eyes = EyeStyle.NORMAL,
                mouth = MouthStyle.FLAT,
                gazeY = 0.9f,
                flourish = Flourish.THINK
            )
        )
        wait(1300)
        move(400, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    rotation = forward * 14f * (1f - t),
                    scaleY = 1f - 0.07f * (1f - t),
                    scaleX = 1f + 0.04f * (1f - t),
                    eyes = EyeStyle.SQUINT,
                    mouth = MouthStyle.SMIRK,
                    gazeY = 0.9f * (1f - t)
                )
            )
        }
        wait(240)
    }

    private suspend fun waveAtYou() {
        // One of two beats that face out of the screen. The character is
        // otherwise entirely absorbed in its own strip of ground, which is what
        // makes being addressed by it land.
        turn(false)
        move(220, FastOutSlowInEasing) { t ->
            set(ShadyPose(scaleY = 1f - 0.05f * t, scaleX = 1f + 0.03f * t, mouth = MouthStyle.SMILE))
        }
        set(ShadyPose(handsUp = true, eyes = EyeStyle.SQUINT, mouth = MouthStyle.BIG_SMILE))
        wait(1500)
        set(ShadyPose(mouth = MouthStyle.SMILE))
        wait(260)
    }

    private suspend fun shiver() {
        move(900, LinearEasing) { t ->
            val buzz = sin(t * 8f * 2f * PI).toFloat()
            val fade = 1f - t * 0.6f
            set(
                ShadyPose(
                    offsetX = buzz * 0.010f * fade,
                    rotation = buzz * 2.4f * fade,
                    scaleY = 0.97f,
                    scaleX = 1.02f,
                    eyes = EyeStyle.SCRUNCH,
                    mouth = MouthStyle.WOBBLE
                )
            )
        }
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT))
        wait(300)
    }

    private suspend fun noticesYou() {
        // The rarest beat in the bag, and the one that has to stay rare: it
        // only works while it is still possible that you imagined it.
        val was = facingLeft
        wait(400)
        turn(false)
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT))
        wait(280)
        set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.FLAT, browRaise = 0.5f))
        wait(1100)

        // Looks away as though it had not been staring.
        move(340, FastOutSlowInEasing) { t ->
            set(
                ShadyPose(
                    rotation = -3f * t,
                    eyes = EyeStyle.NORMAL,
                    mouth = MouthStyle.SMIRK,
                    gazeX = -1.1f * t
                )
            )
        }
        wait(760)
        turn(was)
        set(ShadyPose(mouth = MouthStyle.SMILE))
        wait(320)
    }

    // ============================================
    // Movement helpers
    // ============================================

    /** Applies the current facing to every pose, so no beat has to remember to. */
    private fun set(next: ShadyPose) {
        pose = next.copy(facingLeft = facingLeft)
    }

    private fun turn(left: Boolean) {
        facingLeft = left
    }

    private suspend fun walkTo(
        target: Float,
        baseMs: Int = 900,
        perScreenMs: Int = 2600,
        strideMs: Float = 190f,
        eyes: EyeStyle? = null,
        mouth: MouthStyle? = MouthStyle.SMILE
    ) {
        val from = walkT
        val distance = abs(target - from)
        if (distance < 0.001f) return
        turn(target < from)

        val ms = (baseMs + distance * perScreenMs).toInt()
        move(ms, LinearEasing) { t ->
            walkT = from + (target - from) * t
            // A walk cycle: a small bounce, and a lean into the direction of
            // travel. Stride length is fixed in time, so a longer walk is more
            // steps rather than slower ones.
            val step = sin(t * ms / strideMs * PI).toFloat()
            set(
                ShadyPose(
                    offsetY = -0.035f * abs(step),
                    rotation = step * 3.5f,
                    eyes = eyes,
                    mouth = mouth
                )
            )
        }
        stand()
    }

    private suspend fun move(durationMs: Int, easing: Easing, onFrame: (Float) -> Unit) =
        animate(durationMs, easing, interrupted, onFrame)

    private suspend fun wait(ms: Int) = move(ms, LinearEasing) { }
}

/** Catches slightly past upright and settles — a body, not a fade. */
private val Settle = CubicBezierEasing(0.2f, 1.5f, 0.4f, 1f)

private suspend fun animate(
    durationMs: Int,
    easing: Easing,
    cancelIf: () -> Boolean = { false },
    onFrame: (Float) -> Unit
) {
    var start = -1L
    var t = 0f
    while (t < 1f) {
        if (cancelIf()) return
        withFrameMillis { now ->
            if (start < 0) start = now
            t = ((now - start).toFloat() / durationMs).coerceIn(0f, 1f)
            onFrame(easing.transform(t))
        }
    }
}

private suspend fun frameWait(ms: Int) = animate(ms, LinearEasing) { }
