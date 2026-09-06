package com.safeshade.ui.shady

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.board
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
 * Tapping it produces one of twenty-two deliberately larger reactions, and
 * never the same one twice in a row.
 *
 * Now and then something else is on the stage — see [ShadyProps]. A prop
 * arrives, Shady notices it, does something with it over a few beats, and it
 * goes. One at a time, and never while a reaction is playing.
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
    characterSize: Dp = 100.dp,
    /**
     * Whether the app currently believes it is raining where the wearer is.
     * The only piece of app state the stage reads: it decides whether the
     * umbrella is among the props that can turn up. It reports nothing.
     */
    raining: Boolean = false
) {
    val reactor = rememberShadyReactor(ReactionStyle.BOLD)
    val isStatic = LocalInspectionMode.current
    val isDark = MaterialTheme.board.isDark
    val ink = if (isDark) Color(0xFFECEFF1) else Color(0xFF22282E)

    // The director yields the moment a tap lands, so a beat that was halfway
    // across the stage stops there instead of teleporting to its target when
    // the reaction ends.
    val director = remember { ShadyIdleDirector(interrupted = { reactor.isReacting }) }
    director.raining = raining

    // A slow loop for prop idle motion the director is not driving — the
    // cat's tail, the kettle's steam, the plant's sway.
    val transition = rememberInfiniteTransition(label = "stage")
    val phaseAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "stage-phase"
    )
    val phase = if (isStatic) 0.3f else phaseAnim

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

        val density = LocalDensity.current
        val u = with(density) { characterSize.toPx() }
        val travelPx = with(density) { travel.toPx() }
        val stageH = with(density) { height.toPx() }
        // The character's feet sit at 0.96 of its own box, and the box is
        // bottom-aligned, so that is where the ground is.
        val ground = stageH - u * 0.04f
        val worldX = travelPx * director.walkT
        val prop = director.prop
        director.bodyHalf = if (travelPx > 0f) u * 0.40f / travelPx else 0.14f

        // Two prop passes bracket the character: what it stands in front of,
        // and what stands in front of it (the box's front face, the umbrella).
        // Prop `x` is in the character's walkT space, so the same mapping gives
        // both their centres.
        fun propCentre(p: PropState) = p.x * travelPx + u * 0.5f
        // The hand the umbrella hangs from: the leading hand, a little above
        // the rest position, following the body's own offset and squash.
        fun handAt(p: PropState): Offset {
            val dir = if (director.facingLeft) -1f else 1f
            return Offset(
                worldX + u * 0.5f + dir * u * 0.40f + pose.offsetX * u,
                stageH - u * 0.04f - u * 0.34f * pose.scaleY + pose.offsetY * u
            )
        }

        if (prop != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawProp(prop, PropLayer.BEHIND, propCentre(prop), ground, u, phase, ink, isDark)
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = travel * director.walkT)
        ) {
            Shady(
                mood = ShadyMood.CALM,
                size = characterSize,
                pose = pose,
                worldX = worldX
            )
        }

        if (prop != null) {
            Canvas(Modifier.fillMaxSize()) {
                drawProp(
                    prop, PropLayer.FRONT, propCentre(prop), ground, u, phase, ink, isDark,
                    hand = handAt(prop), characterFacingLeft = director.facingLeft
                )
            }
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

    /** The one thing on stage besides the character, if anything. */
    var prop by mutableStateOf<PropState?>(null)
        private set

    /** Set by the stage each composition; decides whether the umbrella exists. */
    var raining: Boolean = false

    /**
     * Half the character's body width in ground units, set by the stage from
     * its real pixel size. Prop positions are in the same units, so this is
     * what "stand next to it" needs to mean anything.
     */
    var bodyHalf: Float = 0.14f

    private var last = -1
    private var turnAmount = 0f
    private var beatsSinceProp = 0
    private var propBeats = 0

    /** Neutral, but still turned the way it was. Used while a tap plays out. */
    fun stand() {
        pose = ShadyPose(facingLeft = facingLeft, turn = turnAmount)
    }

    suspend fun playOne() {
        // A scene with a prop runs its own course: it arrives, it gets a few
        // beats of attention mixed in with the everyday ones, and it leaves.
        val p = prop
        if (p == null) {
            beatsSinceProp++
            if (beatsSinceProp >= 4 && Random.nextFloat() < 0.16f) {
                spawnProp()
                rest()
                return
            }
        } else {
            propBeats++
            if (propBeats > 6 + Random.nextInt(6)) {
                dismissProp()
                rest()
                return
            }
            if (Random.nextFloat() < 0.5f) {
                interactWith(p)
                rest()
                return
            }
        }

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
        rest()
    }

    /**
     * A beat of stillness between actions. Without it the character reads as
     * frantic rather than alive.
     */
    private suspend fun rest() {
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
    // Props — something else on the stage
    // ============================================

    /**
     * Brings a prop on. Most fade in while the character is turned the other
     * way, and it notices them with a double-take when it turns back; the cat
     * walks on by itself, because a cat that fades in is a ghost.
     */
    private suspend fun spawnProp() {
        val kinds = buildList {
            add(PropKind.PLANT); add(PropKind.CAT); add(PropKind.CAT); add(PropKind.PHONE)
            add(PropKind.KETTLE); add(PropKind.BALL); add(PropKind.BOX)
            if (raining) { add(PropKind.UMBRELLA); add(PropKind.UMBRELLA) }
        }
        val kind = kinds[Random.nextInt(kinds.size)]
        propBeats = 0
        beatsSinceProp = 0

        if (kind == PropKind.UMBRELLA) {
            // Handed in from off-stage: a glance up, a hand out, and it is there.
            set(ShadyPose(eyes = EyeStyle.NORMAL, gazeY = -0.9f, mouth = MouthStyle.FLAT))
            wait(500)
            prop = PropState(PropKind.UMBRELLA, walkT, presence = 0f, held = true, a = 1f)
            move(360, FastOutSlowInEasing) { t -> prop = prop?.copy(presence = t); set(ShadyPose(handsUp = false, mouth = MouthStyle.SMILE)) }
            set(ShadyPose(mouth = MouthStyle.SMILE))
            wait(600)
            return
        }

        val farSide = walkT > 0.5f
        val spot = if (farSide) 0.12f + Random.nextFloat() * 0.14f else 0.74f + Random.nextFloat() * 0.14f

        if (kind == PropKind.CAT) {
            // Walks in from the near edge, past nothing, to its spot.
            val edge = if (farSide) -0.15f else 1.15f
            prop = PropState(PropKind.CAT, edge, presence = 1f, facingLeft = edge > spot)
            turn(spot < walkT)
            set(ShadyPose(eyes = EyeStyle.NORMAL, gazeX = 0.9f))
            move(1500, LinearEasing) { t ->
                prop = prop?.copy(x = edge + (spot - edge) * t, a = (t * 6f) % 1f)
            }
            prop = prop?.copy(a = 0f)
            set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN, flourish = Flourish.SPARK))
            wait(700)
            return
        }

        // Turn away, let it appear, turn back and find it there.
        turn(!farSide)
        set(ShadyPose(eyes = EyeStyle.SQUINT))
        wait(300)
        prop = PropState(kind, spot, presence = 0f)
        move(520, LinearEasing) { t -> prop = prop?.copy(presence = t) }
        wait(400)
        turn(farSide)
        move(160, Settle) { t ->
            set(ShadyPose(rotation = (if (farSide) 6f else -6f) * (1f - t), eyes = EyeStyle.WIDE, mouth = MouthStyle.GASP, flourish = Flourish.SPARK))
        }
        wait(600)
    }

    private suspend fun dismissProp() {
        val p = prop ?: return
        when (p.kind) {
            PropKind.CAT -> {
                // Stands, stretches, and walks off the way it came.
                val edge = if (p.x < 0.5f) -0.18f else 1.18f
                prop = p.copy(facingLeft = edge < p.x, b = 0f)
                wait(300)
                move(1600, LinearEasing) { t -> prop = prop?.copy(x = p.x + (edge - p.x) * t, a = (t * 6f) % 1f) }
                set(ShadyPose(eyes = EyeStyle.NORMAL, gazeX = if (edge < walkT) 0.9f else -0.9f, mouth = MouthStyle.FLAT))
                wait(500)
                prop = null
            }
            PropKind.UMBRELLA -> {
                move(360, FastOutSlowInEasing) { t -> prop = prop?.copy(presence = 1f - t) }
                prop = null
                set(ShadyPose(mouth = MouthStyle.SMILE))
                wait(300)
            }
            else -> {
                // Looks away; it is gone when it looks back. The shrug is the joke.
                val away = p.x < walkT
                turn(!away)
                set(ShadyPose(eyes = EyeStyle.SQUINT))
                wait(400)
                move(420, LinearEasing) { t -> prop = prop?.copy(presence = 1f - t, occludes = false) }
                prop = null
                wait(300)
                turn(away)
                set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.FLAT, browRaise = 0.6f))
                wait(700)
                set(ShadyPose(mouth = MouthStyle.SMIRK, eyes = EyeStyle.SQUINT))
                wait(400)
            }
        }
        propBeats = 0
        beatsSinceProp = 0
    }

    private suspend fun interactWith(p: PropState) = when (p.kind) {
        PropKind.PLANT -> if (p.a < 0.9f) waterPlant(p) else admirePlant(p)
        PropKind.CAT -> if (Random.nextBoolean()) petCat(p) else catRubs(p)
        PropKind.PHONE -> phoneBuzzes(p)
        PropKind.KETTLE -> if (p.a < 0.5f) kettleBoils(p) else warmHands(p)
        PropKind.BALL -> if (p.x in 0.05f..0.95f) nudgeBall(p) else examineFloor()
        PropKind.BOX -> if (p.b < 0.5f) peekInBox(p) else climbInBox(p)
        PropKind.UMBRELLA -> underUmbrella()
    }

    private fun approach(p: PropState): Float =
        if (walkT < p.x) p.x - p.halfWidth - bodyHalf - 0.02f else p.x + p.halfWidth + bodyHalf + 0.02f

    private suspend fun faceProp(p: PropState) = turn(p.x < walkT)

    private suspend fun waterPlant(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        // Leans in over it; the drop falls; a new leaf comes up. The pride is
        // the payoff, and it is the only time the grin shows off-reaction.
        val fwd = if (facingLeft) -1f else 1f
        move(360, FastOutSlowInEasing) { t -> set(ShadyPose(rotation = fwd * 10f * t, gazeY = 0.8f * t, mouth = MouthStyle.WHISTLE)) }
        repeat(3) {
            move(420, LinearEasing) { t -> prop = prop?.copy(b = t); set(ShadyPose(rotation = fwd * 10f, gazeY = 0.8f, mouth = MouthStyle.WHISTLE)) }
        }
        prop = prop?.copy(b = 0f)
        move(900, FastOutSlowInEasing) { t -> prop = prop?.copy(a = t); set(ShadyPose(rotation = fwd * 10f * (1f - t), gazeY = 0.8f * (1f - t), eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE)) }
        set(ShadyPose(mouth = MouthStyle.GRIN, eyes = EyeStyle.SQUINT, flourish = Flourish.SPARK))
        wait(900)
    }

    private suspend fun admirePlant(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        set(ShadyPose(eyes = EyeStyle.NORMAL, gazeY = 0.5f, mouth = MouthStyle.SMILE, flourish = Flourish.LEAF))
        wait(1800)
    }

    private suspend fun petCat(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        val fwd = if (facingLeft) -1f else 1f
        move(360, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 1f - 0.10f * t, scaleX = 1f + 0.06f * t, rotation = fwd * 8f * t, gazeY = 0.9f * t, mouth = MouthStyle.SMILE)) }
        // Strokes: the hand bobs; the cat closes its eyes and its tail curls.
        repeat(4) { i ->
            move(320, FastOutSlowInEasing) { t ->
                val stroke = sin(t * PI).toFloat()
                prop = prop?.copy(b = ((i + t) / 4f).coerceIn(0f, 1f))
                set(ShadyPose(scaleY = 0.90f - 0.02f * stroke, scaleX = 1.06f, rotation = fwd * (8f + 3f * stroke), gazeY = 0.9f, mouth = MouthStyle.SMILE, eyes = if (i >= 2) EyeStyle.HEART else EyeStyle.NORMAL, flourish = if (i >= 2) Flourish.HEART else Flourish.NONE))
            }
        }
        move(360, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 0.90f + 0.10f * t, scaleX = 1.06f - 0.06f * t, rotation = fwd * 8f * (1f - t), gazeY = 0.9f * (1f - t), mouth = MouthStyle.SMILE, flourish = Flourish.BLUSH)) }
        wait(500)
        prop = prop?.copy(b = 0.6f)
    }

    private suspend fun catRubs(p: PropState) {
        // The cat comes to Shady, leans on it, and Shady wobbles.
        val to = if (p.x < walkT) walkT - 0.16f else walkT + 0.16f
        prop = p.copy(facingLeft = to < p.x)
        turn(p.x < walkT)
        set(ShadyPose(eyes = EyeStyle.NORMAL, gazeX = 0.9f, gazeY = 0.6f))
        move(1100, LinearEasing) { t -> prop = prop?.copy(x = p.x + (to - p.x) * t, a = (t * 5f) % 1f) }
        prop = prop?.copy(a = 0f)
        val fwd = if (facingLeft) -1f else 1f
        repeat(2) {
            move(500, FastOutSlowInEasing) { t ->
                val lean = sin(t * PI).toFloat()
                prop = prop?.copy(b = 0.8f)
                set(ShadyPose(rotation = -fwd * 7f * lean, offsetX = -fwd * 0.03f * lean, eyes = EyeStyle.SQUINT, mouth = MouthStyle.BIG_SMILE, flourish = Flourish.BLUSH))
            }
        }
        set(ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMILE, flourish = Flourish.BLUSH))
        wait(600)
    }

    private suspend fun phoneBuzzes(p: PropState) {
        // It lights and buzzes; Shady startles, goes over, reads it, nods.
        move(700, LinearEasing) { t -> prop = prop?.copy(a = 1f, b = 1f) }
        move(140, LinearEasing) { t -> set(ShadyPose(offsetY = -0.06f * sin(t * PI).toFloat(), eyes = EyeStyle.WIDE, mouth = MouthStyle.GASP, handsUp = true)) }
        set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN))
        wait(300)
        prop = prop?.copy(b = 0f)
        walkTo(approach(p), eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN)
        faceProp(p)
        val fwd = if (facingLeft) -1f else 1f
        move(360, FastOutSlowInEasing) { t -> set(ShadyPose(rotation = fwd * 12f * t, gazeY = 0.9f * t, eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT)) }
        wait(900)
        repeat(2) {
            move(220, FastOutSlowInEasing) { t -> set(ShadyPose(rotation = fwd * (12f + 5f * sin(t * PI).toFloat()), gazeY = 0.9f, mouth = MouthStyle.SMILE)) }
        }
        move(400, FastOutSlowInEasing) { t -> set(ShadyPose(rotation = fwd * 12f * (1f - t), gazeY = 0.9f * (1f - t), mouth = MouthStyle.SMILE)) }
        move(600, LinearEasing) { t -> prop = prop?.copy(a = 1f - t) }
    }

    private suspend fun kettleBoils(p: PropState) {
        // Steam builds, then the whistle: a jump, then interest.
        move(2200, LinearEasing) { t -> prop = prop?.copy(a = t) }
        prop = prop?.copy(b = 1f)
        move(180, LinearEasing) { t -> set(ShadyPose(offsetY = -0.10f * sin(t * PI).toFloat(), scaleY = 1f + 0.08f * sin(t * PI).toFloat(), eyes = EyeStyle.WIDE, mouth = MouthStyle.GASP, handsUp = true)) }
        set(ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN, flourish = Flourish.SWEAT))
        wait(700)
        prop = prop?.copy(b = 0f)
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE))
        wait(300)
    }

    private suspend fun warmHands(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        set(ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMILE, handsUp = true, flourish = Flourish.STEAM))
        wait(1800)
        set(ShadyPose(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE))
        wait(500)
        move(1500, LinearEasing) { t -> prop = prop?.copy(a = 1f - t) }
    }

    private suspend fun nudgeBall(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        val fwd = if (facingLeft) -1f else 1f
        // A small lean and a push; the ball rolls to the edge and off, and the
        // character watches it go and shrugs. It never comes back.
        move(260, FastOutSlowInEasing) { t -> set(ShadyPose(rotation = fwd * 14f * t, mouth = MouthStyle.SMILE, gazeY = 0.7f)) }
        val edge = if (fwd > 0f) 1.12f else -0.12f
        val start = p.x
        move(1400, FastOutSlowInEasing) { t ->
            prop = prop?.copy(x = start + (edge - start) * t, b = t * 3f)
            set(ShadyPose(rotation = fwd * 14f * (1f - t), gazeX = 0.9f, gazeY = 0.5f * (1f - t), mouth = MouthStyle.OPEN))
        }
        set(ShadyPose(eyes = EyeStyle.NORMAL, gazeX = 0.9f, mouth = MouthStyle.WOBBLE))
        wait(600)
        move(300, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 1f + 0.05f * sin(t * PI).toFloat(), handsUp = t > 0.3f, mouth = MouthStyle.FLAT, eyes = EyeStyle.SQUINT)) }
        wait(500)
        prop = null
        propBeats = 0
    }

    private suspend fun peekInBox(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        val fwd = if (facingLeft) -1f else 1f
        move(400, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 1f + 0.08f * t, rotation = fwd * 10f * t, gazeY = 0.9f * t, eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT)) }
        move(500, FastOutSlowInEasing) { t -> prop = prop?.copy(b = t) }
        wait(900)
        move(400, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 1f + 0.08f * (1f - t), rotation = fwd * 10f * (1f - t), gazeY = 0.9f * (1f - t), mouth = MouthStyle.SMIRK)) }
        wait(300)
    }

    private suspend fun climbInBox(p: PropState) {
        walkTo(approach(p))
        faceProp(p)
        val fwd = if (facingLeft) -1f else 1f
        // Hop in: the box front draws over the character once it is inside.
        move(120, FastOutSlowInEasing) { t -> set(ShadyPose(scaleY = 1f - 0.14f * t, scaleX = 1f + 0.09f * t, eyes = EyeStyle.SQUINT)) }
        val from = walkT
        move(420, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            walkT = from + (p.x - from) * t
            set(ShadyPose(offsetY = -0.30f * lift, scaleY = 1f + 0.08f * lift, eyes = EyeStyle.CLOSED, mouth = MouthStyle.BIG_SMILE))
            if (t > 0.6f) prop = prop?.copy(occludes = true)
        }
        set(ShadyPose(scaleY = 0.80f, eyes = EyeStyle.CLOSED))
        // Rummaging: the box rocks.
        move(1600, LinearEasing) { t -> prop = prop?.copy(a = sin(t * 5f * 2f * PI).toFloat() * 3f) }
        prop = prop?.copy(a = 0f)
        wait(400)
        // Pops out dizzy, lands beside it.
        val out = approach(p)
        move(460, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            walkT = p.x + (out - p.x) * t
            set(ShadyPose(offsetY = -0.34f * lift, rotation = -fwd * 360f * t, eyes = EyeStyle.DIZZY, mouth = MouthStyle.OPEN))
            if (t > 0.35f) prop = prop?.copy(occludes = false)
        }
        move(320, Settle) { t -> set(ShadyPose(scaleY = 0.86f + 0.14f * t, scaleX = 1.10f - 0.10f * t, eyes = EyeStyle.DIZZY, mouth = MouthStyle.WOBBLE, flourish = Flourish.PUFF)) }
        set(ShadyPose(eyes = EyeStyle.DIZZY, mouth = MouthStyle.WOBBLE, flourish = Flourish.SWIRL))
        wait(900)
        set(ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE, flourish = Flourish.SPARK))
        wait(400)
    }

    private suspend fun underUmbrella() {
        // Holds it, looks up, shivers once, and is fine. The umbrella moves
        // with the character on every other beat because it is `held`.
        set(ShadyPose(eyes = EyeStyle.NORMAL, gazeY = -0.8f, mouth = MouthStyle.FLAT))
        wait(900)
        move(700, LinearEasing) { t ->
            val buzz = sin(t * 8f * 2f * PI).toFloat()
            set(ShadyPose(rotation = buzz * 1.8f * (1f - t), eyes = EyeStyle.SCRUNCH, mouth = MouthStyle.WOBBLE))
        }
        set(ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMILE))
        wait(600)
    }

    // ============================================
    // Movement helpers
    // ============================================

    /** Applies the current facing to every pose, so no beat has to remember to. */
    private fun set(next: ShadyPose) {
        pose = next.copy(facingLeft = facingLeft, turn = turnAmount)
    }

    /**
     * Turns to face the other way, as a rotation rather than a cut.
     *
     * The body narrows to edge-on, the mirror flips at the narrowest point
     * where the two facings look the same, and it widens again facing the
     * new way. 160ms total, which is quick enough not to interrupt a beat and
     * slow enough that the antenna visibly swings across.
     */
    private suspend fun turn(left: Boolean) {
        if (left == facingLeft) return
        val held = pose
        move(80, LinearEasing) { t -> turnAmount = t; pose = held.copy(turn = t) }
        facingLeft = left
        move(80, LinearEasing) { t -> turnAmount = 1f - t; pose = held.copy(facingLeft = left, turn = 1f - t) }
        turnAmount = 0f
        pose = held.copy(facingLeft = left, turn = 0f)
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
        // Walk up to a prop, never through it. A target on the far side of
        // one is clamped to its near edge, plus a little standing room.
        val blocked = prop?.takeIf { it.kind != PropKind.UMBRELLA && !it.held }
        val clamped = if (blocked != null && (from < blocked.x) != (target < blocked.x)) {
            if (from < blocked.x) blocked.x - blocked.halfWidth - bodyHalf - 0.02f
            else blocked.x + blocked.halfWidth + bodyHalf + 0.02f
        } else target
        val goal = clamped.coerceIn(0.02f, 0.98f)
        val distance = abs(goal - from)
        if (distance < 0.001f) return
        turn(goal < from)

        val ms = (baseMs + distance * perScreenMs).toInt()
        move(ms, LinearEasing) { t ->
            walkT = from + (goal - from) * t
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
