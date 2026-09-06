package com.safeshade.ui.shady

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** How loud a reaction should be. */
enum class ReactionStyle {
    /** For the status card: a beat of personality, gone in under a second. */
    SUBTLE,

    /** For the stage at the bottom of the Board, where Shady has room. */
    BOLD
}

/**
 * Drives Shady's momentary reactions to being touched.
 *
 * The rule the whole thing exists to serve: **never the same reaction twice in
 * a row.** A mascot that does one trick is furniture; the reason to tap it a
 * second time is finding out what else it does. So the reactor tracks what it
 * played last and always picks something else.
 *
 * Reactions are keyframed by hand rather than expressed as a spring, because
 * squash-and-stretch has to be *timed* — the compression before a hop and the
 * stretch at the apex are the whole effect, and an interpolator that merely
 * gets from A to B smoothly loses them.
 *
 * The two sets are sized differently on purpose. The subtle set stays inside a
 * status card that is barely taller than the character, so nothing in it
 * travels more than a fraction of its own height. The bold set has the stage's
 * clearance to play with, but not much more than that — see [ShadyStage].
 */
class ShadyReactor(
    private val scope: CoroutineScope,
    private val style: ReactionStyle
) {
    var pose by mutableStateOf(ShadyPose.Neutral)
        private set

    /** True while a reaction is playing, so idle behaviour can stand aside. */
    var isReacting by mutableStateOf(false)
        private set

    private var job: Job? = null
    private var lastIndex = -1

    private val reactions: List<suspend () -> Unit> =
        if (style == ReactionStyle.SUBTLE) {
            listOf(
                ::blinkAndBeam, ::littleHop, ::curiousTilt, ::quickWave,
                ::sparkle, ::ponder, ::raisedBrow, ::nod,
                ::peer, ::shiver, ::doubleTake, ::yawn,
                ::wink, ::blush, ::tiptoe, ::headBob, ::leanIn, ::stretchTall
            )
        } else {
            listOf(
                ::startle, ::spin, ::cheer, ::tumble,
                ::delight, ::wobble, ::backflip, ::sneeze,
                ::hiccup, ::trip, ::shakeOff, ::nodOff,
                ::bow, ::sidestep,
                ::moonwalk, ::cartwheel, ::juggle, ::dance,
                ::faint, ::leapSpin, ::slide, ::pounce
            )
        }

    fun poke() {
        job?.cancel()
        val next = pickDifferent()
        lastIndex = next
        job = scope.launch {
            isReacting = true
            try {
                reactions[next]()
            } finally {
                pose = ShadyPose.Neutral
                isReacting = false
            }
        }
    }

    private fun pickDifferent(): Int {
        if (reactions.size == 1) return 0
        var candidate = Random.nextInt(reactions.size)
        while (candidate == lastIndex) candidate = Random.nextInt(reactions.size)
        return candidate
    }

    // ============================================
    // Subtle set — the status card
    // ============================================

    private suspend fun blinkAndBeam() {
        pose = ShadyPose(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)
        wait(140)
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.BIG_SMILE)
        wait(520)
    }

    private suspend fun littleHop() {
        // Compress, launch, hang, land, absorb. The two squash frames either
        // side of the arc are what sell the weight.
        anim(90, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f - 0.10f * t, scaleX = 1f + 0.07f * t, mouth = MouthStyle.SMILE)
        }
        anim(240, Overshoot) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                offsetY = -0.14f * lift,
                scaleY = 1f + 0.08f * lift,
                scaleX = 1f - 0.05f * lift,
                mouth = MouthStyle.BIG_SMILE
            )
        }
        anim(130, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f - 0.07f * (1f - t), scaleX = 1f + 0.05f * (1f - t))
        }
    }

    private suspend fun curiousTilt() {
        anim(220, FastOutSlowInEasing) { t -> pose = ShadyPose(rotation = -11f * t, eyes = EyeStyle.SQUINT) }
        wait(300)
        anim(260, FastOutSlowInEasing) { t -> pose = ShadyPose(rotation = -11f * (1f - t), eyes = EyeStyle.SQUINT) }
    }

    private suspend fun quickWave() {
        pose = ShadyPose(handsUp = true, mouth = MouthStyle.BIG_SMILE)
        wait(700)
    }

    private suspend fun sparkle() {
        pose = ShadyPose(eyes = EyeStyle.STAR, mouth = MouthStyle.BIG_SMILE, flourish = Flourish.SPARK)
        wait(620)
    }

    private suspend fun ponder() {
        pose = ShadyPose(eyes = EyeStyle.SQUINT, mouth = MouthStyle.FLAT, flourish = Flourish.THINK)
        wait(760)
    }

    private suspend fun raisedBrow() {
        // Nothing moves but the face. Everything else in the subtle set travels
        // a little; this one earns its place by refusing to.
        anim(200, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                eyes = EyeStyle.NORMAL,
                mouth = MouthStyle.SMIRK,
                gazeX = 0.38f * t,
                browRaise = 1.1f * t
            )
        }
        wait(520)
        anim(240, FastOutSlowInEasing) { t ->
            pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMIRK, browRaise = 1.1f * (1f - t))
        }
    }

    private suspend fun nod() {
        repeat(2) {
            anim(180, FastOutSlowInEasing) { t ->
                val dip = sin(t * PI).toFloat()
                pose = ShadyPose(
                    offsetY = 0.045f * dip,
                    scaleY = 1f - 0.05f * dip,
                    scaleX = 1f + 0.035f * dip,
                    eyes = EyeStyle.CLOSED,
                    mouth = MouthStyle.SMILE
                )
            }
        }
        pose = ShadyPose(mouth = MouthStyle.SMILE)
        wait(220)
    }

    private suspend fun peer() {
        // Leaning *out* of a flat drawing can only be done by growing, so the
        // whole character scales rather than translating.
        anim(240, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                offsetY = 0.02f * t,
                scaleX = 1f + 0.07f * t,
                scaleY = 1f + 0.07f * t,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.FLAT,
                browRaise = 0.55f * t
            )
        }
        wait(360)
        anim(280, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                offsetY = 0.02f * (1f - t),
                scaleX = 1f + 0.07f * (1f - t),
                scaleY = 1f + 0.07f * (1f - t),
                eyes = EyeStyle.NORMAL,
                mouth = MouthStyle.SMILE
            )
        }
    }

    private suspend fun shiver() {
        // High frequency and small amplitude, decaying. Slow it down at all and
        // it stops being a shiver and becomes the dizzy wobble.
        anim(640, LinearEasing) { t ->
            val buzz = sin(t * 9f * 2f * PI).toFloat()
            val fade = 1f - t * 0.55f
            pose = ShadyPose(
                offsetX = buzz * 0.012f * fade,
                rotation = buzz * 2.6f * fade,
                eyes = EyeStyle.SCRUNCH,
                mouth = MouthStyle.WOBBLE
            )
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT)
        wait(200)
    }

    private suspend fun doubleTake() {
        // The look away has to land before the snap back, or there is nothing
        // for the second look to be a correction of.
        anim(180, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = -4f * t, eyes = EyeStyle.NORMAL, gazeX = -1.1f * t)
        }
        wait(260)
        anim(120, Overshoot) { t ->
            pose = ShadyPose(
                rotation = -4f + 6f * t,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.GASP,
                gazeX = -1.1f * (1f - t),
                flourish = Flourish.SPARK
            )
        }
        wait(300)
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE)
        wait(180)
    }

    private suspend fun yawn() {
        anim(420, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                scaleX = 1f - 0.04f * t,
                scaleY = 1f + 0.07f * t,
                eyes = EyeStyle.SLEEPY,
                mouth = MouthStyle.GASP,
                handsUp = t > 0.55f
            )
        }
        wait(320)
        anim(320, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                scaleX = 1f - 0.04f * (1f - t),
                scaleY = 1f + 0.07f * (1f - t),
                eyes = EyeStyle.SLEEPY,
                mouth = MouthStyle.FLAT
            )
        }
        pose = ShadyPose(eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)
        wait(200)
    }

    private suspend fun wink() {
        anim(160, FastOutSlowInEasing) { t -> pose = ShadyPose(rotation = -4f * t, eyes = EyeStyle.WINK, mouth = MouthStyle.SMIRK) }
        wait(520)
        anim(200, FastOutSlowInEasing) { t -> pose = ShadyPose(rotation = -4f * (1f - t), mouth = MouthStyle.SMILE) }
    }

    private suspend fun blush() {
        // Looks away and down, cheeks up. The gaze is the whole thing; the
        // blush only confirms it.
        anim(260, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = 5f * t, gazeX = -0.9f * t, gazeY = 0.6f * t, mouth = MouthStyle.SMILE, flourish = Flourish.BLUSH)
        }
        wait(700)
        anim(260, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = 5f * (1f - t), gazeX = -0.9f * (1f - t), gazeY = 0.6f * (1f - t), mouth = MouthStyle.SMILE, flourish = Flourish.BLUSH)
        }
    }

    private suspend fun tiptoe() {
        // Up on the toes to see over something, then down. A stretch in Y
        // with the legs tucked, not a hop: the feet never leave.
        anim(240, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f + 0.10f * t, scaleX = 1f - 0.05f * t, gazeY = -0.7f * t, eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT)
        }
        wait(500)
        anim(220, Overshoot) { t ->
            pose = ShadyPose(scaleY = 1f + 0.10f * (1f - t), scaleX = 1f - 0.05f * (1f - t), gazeY = -0.7f * (1f - t), mouth = MouthStyle.SMILE)
        }
    }

    private suspend fun headBob() {
        // Humming along to something. Three bobs, notes drifting.
        repeat(3) { i ->
            anim(260, FastOutSlowInEasing) { t ->
                val bob = sin(t * PI).toFloat()
                pose = ShadyPose(
                    rotation = (if (i % 2 == 0) -5f else 5f) * bob,
                    scaleY = 1f - 0.03f * bob,
                    eyes = EyeStyle.CLOSED,
                    mouth = MouthStyle.SMILE,
                    flourish = Flourish.NOTES
                )
            }
        }
    }

    private suspend fun leanIn() {
        // Leans toward the viewer as if to see them better, brows up.
        anim(260, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleX = 1f + 0.06f * t, scaleY = 1f + 0.03f * t, offsetY = 0.01f * t, eyes = EyeStyle.WIDE, browRaise = 0.5f * t, mouth = MouthStyle.FLAT)
        }
        wait(560)
        anim(260, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleX = 1f + 0.06f * (1f - t), scaleY = 1f + 0.03f * (1f - t), offsetY = 0.01f * (1f - t), mouth = MouthStyle.SMILE)
        }
    }

    private suspend fun stretchTall() {
        anim(420, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f + 0.14f * t, scaleX = 1f - 0.08f * t, handsUp = t > 0.5f, eyes = EyeStyle.CLOSED, mouth = MouthStyle.GASP)
        }
        wait(300)
        anim(360, Overshoot) { t ->
            pose = ShadyPose(scaleY = 1f + 0.14f * (1f - t), scaleX = 1f - 0.08f * (1f - t), eyes = EyeStyle.SLEEPY, mouth = MouthStyle.SMILE)
        }
    }

    // ============================================
    // Bold set — the stage
    // ============================================
    //
    // Airborne beats are capped at roughly 0.30 of the character's height, and
    // never stack a large vertical stretch on top of a large lift: the stage
    // leaves only about a quarter of the character's height in clearance above
    // it, and anything more overdraws the content the stage sits under.

    private suspend fun startle() {
        pose = ShadyPose(eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN, flourish = Flourish.SPARK)
        anim(120, FastOutSlowInEasing) { t ->
            pose = pose.copy(scaleY = 1f - 0.14f * t, scaleX = 1f + 0.10f * t)
        }
        anim(340, Overshoot) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                offsetY = -0.30f * lift,
                scaleY = 1f + 0.12f * lift,
                scaleX = 1f - 0.08f * lift,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.OPEN,
                flourish = Flourish.SPARK
            )
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE)
        wait(220)
    }

    private suspend fun spin() {
        // Anticipation: a crouch and a small wind-up the other way. A spin
        // that starts from stillness reads as a glitch, not an action.
        anim(110, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f - 0.08f * t, scaleX = 1f + 0.05f * t, rotation = -14f * t, eyes = EyeStyle.SQUINT)
        }
        anim(620, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = -14f + 374f * t, mouth = MouthStyle.OPEN, eyes = EyeStyle.CLOSED)
        }
        pose = ShadyPose(eyes = EyeStyle.DIZZY, mouth = MouthStyle.WOBBLE, flourish = Flourish.SWIRL)
        wait(760)
    }

    private suspend fun cheer() {
        repeat(2) {
            anim(230, Overshoot) { t ->
                val lift = sin(t * PI).toFloat()
                pose = ShadyPose(
                    offsetY = -0.20f * lift,
                    handsUp = true,
                    eyes = EyeStyle.STAR,
                    mouth = MouthStyle.BIG_SMILE
                )
            }
        }
        wait(220)
    }

    private suspend fun tumble() {
        anim(480, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                rotation = -200f * t,
                offsetY = -0.22f * lift,
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.OPEN
            )
        }
        // Lands flat on its side, then springs back upright.
        pose = ShadyPose(rotation = -200f, eyes = EyeStyle.DIZZY, mouth = MouthStyle.WOBBLE)
        wait(420)
        anim(360, Overshoot) { t ->
            pose = ShadyPose(
                rotation = -200f * (1f - t),
                eyes = EyeStyle.DIZZY,
                mouth = MouthStyle.WOBBLE,
                flourish = Flourish.SWIRL
            )
        }
        wait(200)
    }

    private suspend fun delight() {
        pose = ShadyPose(eyes = EyeStyle.STAR, mouth = MouthStyle.BIG_SMILE, flourish = Flourish.SPARK)
        anim(700, LinearEasing) { t ->
            val wobble = sin(t * 3f * 2f * PI).toFloat()
            pose = pose.copy(rotation = wobble * 7f, offsetY = -0.04f * abs(wobble))
        }
    }

    private suspend fun wobble() {
        anim(900, LinearEasing) { t ->
            val sway = sin(t * 2.5f * 2f * PI).toFloat()
            pose = ShadyPose(
                rotation = sway * 13f,
                offsetX = sway * 0.03f,
                eyes = EyeStyle.DIZZY,
                mouth = MouthStyle.WOBBLE,
                flourish = Flourish.SWIRL
            )
        }
    }

    private suspend fun backflip() {
        anim(140, FastOutSlowInEasing) { t ->
            pose = ShadyPose(scaleY = 1f - 0.16f * t, scaleX = 1f + 0.11f * t, eyes = EyeStyle.SQUINT)
        }
        anim(560, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                rotation = -360f * t,
                offsetY = -0.26f * lift,
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.OPEN
            )
        }
        // The landing is sold by the scuff at the feet rather than by a
        // triumphant pose, which would be the childish reading of the same beat.
        anim(320, Overshoot) { t ->
            pose = ShadyPose(
                scaleY = 0.86f + 0.14f * t,
                scaleX = 1.10f - 0.10f * t,
                eyes = EyeStyle.SQUINT,
                mouth = MouthStyle.BIG_SMILE,
                flourish = Flourish.PUFF
            )
        }
        wait(200)
    }

    private suspend fun sneeze() {
        anim(360, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                offsetY = -0.03f * t,
                rotation = -12f * t,
                scaleY = 1f + 0.05f * t,
                eyes = EyeStyle.SCRUNCH,
                mouth = MouthStyle.GASP,
                browRaise = 0.6f * t
            )
        }
        // The release is a handful of frames. Any longer and the reflex reads
        // as a deliberate bow instead.
        anim(90, LinearEasing) { t ->
            pose = ShadyPose(
                offsetY = 0.03f * t,
                rotation = -12f + 30f * t,
                scaleY = 1f - 0.10f * t,
                scaleX = 1f + 0.08f * t,
                eyes = EyeStyle.SCRUNCH,
                mouth = MouthStyle.OPEN,
                flourish = Flourish.PUFF
            )
        }
        wait(150)
        anim(420, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                rotation = 18f * (1f - t),
                scaleY = 1f - 0.10f * (1f - t),
                scaleX = 1f + 0.08f * (1f - t),
                eyes = EyeStyle.SLEEPY,
                mouth = MouthStyle.FLAT
            )
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE)
        wait(220)
    }

    private suspend fun hiccup() {
        // Unevenly spaced: three jolts on a metronome would read as a machine.
        val gaps = intArrayOf(260, 190, 70)
        repeat(3) { i ->
            anim(110, LinearEasing) { t ->
                val jolt = sin(t * PI).toFloat()
                pose = ShadyPose(
                    offsetY = -0.09f * jolt,
                    scaleY = 1f + 0.07f * jolt,
                    scaleX = 1f - 0.05f * jolt,
                    eyes = EyeStyle.WIDE,
                    mouth = MouthStyle.GASP
                )
            }
            pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT)
            wait(gaps[i])
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.FLAT, browRaise = 0.7f)
        wait(340)
    }

    private suspend fun trip() {
        anim(200, LinearEasing) { t ->
            pose = ShadyPose(
                offsetX = 0.05f * t,
                offsetY = 0.02f * t,
                rotation = 34f * t,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.OPEN,
                handsUp = true
            )
        }
        // Caught, never fallen — it stops short of the angle at which it would
        // have to land, so the recovery is the point rather than the stumble.
        anim(140, LinearEasing) { t ->
            pose = ShadyPose(
                offsetX = 0.05f,
                rotation = 34f - 12f * t,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.OPEN,
                handsUp = true,
                flourish = Flourish.PUFF
            )
        }
        wait(180)
        anim(420, Overshoot) { t ->
            pose = ShadyPose(
                offsetX = 0.05f * (1f - t),
                rotation = 22f * (1f - t),
                eyes = EyeStyle.SQUINT,
                mouth = MouthStyle.WOBBLE
            )
        }
        wait(220)
    }

    private suspend fun shakeOff() {
        anim(700, LinearEasing) { t ->
            val buzz = sin(t * 7f * 2f * PI).toFloat()
            val fade = 1f - t
            pose = ShadyPose(
                offsetX = buzz * 0.020f * fade,
                rotation = buzz * 11f * fade,
                scaleX = 1f + 0.05f * fade,
                scaleY = 1f - 0.04f * fade,
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.FLAT
            )
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMILE, flourish = Flourish.PUFF)
        wait(340)
    }

    private suspend fun nodOff() {
        anim(900, LinearEasing) { t ->
            pose = ShadyPose(
                offsetY = 0.03f * t,
                rotation = 16f * t,
                scaleY = 1f - 0.05f * t,
                eyes = if (t > 0.45f) EyeStyle.CLOSED else EyeStyle.SLEEPY,
                mouth = MouthStyle.FLAT,
                flourish = if (t > 0.6f) Flourish.SLEEP else Flourish.NONE
            )
        }
        // The whole beat is the contrast between the drift and the catch, so
        // the recovery runs about five times faster than the drift did.
        anim(170, Overshoot) { t ->
            pose = ShadyPose(
                rotation = 16f * (1f - t) - 5f * t,
                scaleY = 0.95f + 0.05f * t,
                eyes = EyeStyle.WIDE,
                mouth = MouthStyle.GASP,
                flourish = Flourish.SPARK
            )
        }
        wait(240)
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.SMIRK, browRaise = 0.5f)
        wait(320)
    }

    private suspend fun bow() {
        anim(380, FastOutSlowInEasing) { t ->
            pose = ShadyPose(offsetY = 0.03f * t, rotation = 26f * t, eyes = EyeStyle.CLOSED, mouth = MouthStyle.SMILE)
        }
        wait(400)
        anim(420, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                offsetY = 0.03f * (1f - t),
                rotation = 26f * (1f - t),
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.SMILE
            )
        }
        pose = ShadyPose(eyes = EyeStyle.NORMAL, mouth = MouthStyle.BIG_SMILE)
        wait(240)
    }

    private suspend fun sidestep() {
        // It looks back at where it came from at the far end. Without that the
        // shuffle is just a translation; the glance is what motivates it.
        anim(280, FastOutSlowInEasing) { t ->
            val step = sin(t * 2f * PI).toFloat()
            pose = ShadyPose(
                offsetX = 0.16f * t,
                offsetY = -0.04f * abs(step),
                rotation = 4f * step,
                mouth = MouthStyle.SMILE
            )
        }
        pose = ShadyPose(
            offsetX = 0.16f,
            eyes = EyeStyle.SQUINT,
            mouth = MouthStyle.SMIRK,
            gazeX = -1.0f
        )
        wait(420)
        anim(300, FastOutSlowInEasing) { t ->
            val step = sin(t * 2f * PI).toFloat()
            pose = ShadyPose(
                offsetX = 0.16f * (1f - t),
                offsetY = -0.04f * abs(step),
                rotation = -4f * step,
                mouth = MouthStyle.SMILE
            )
        }
        wait(180)
    }

    private suspend fun moonwalk() {
        // Slides backward while the legs walk forward. Sold entirely by the
        // lean: the body tips away from the direction of travel.
        anim(140, FastOutSlowInEasing) { t -> pose = ShadyPose(rotation = 8f * t, eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMIRK) }
        anim(900, LinearEasing) { t ->
            val step = sin(t * 6f * PI).toFloat()
            pose = ShadyPose(
                offsetX = -0.22f * t,
                rotation = 8f + step * 2f,
                legSwing = step * 0.025f,
                offsetY = -0.01f * abs(step),
                eyes = EyeStyle.SQUINT,
                mouth = MouthStyle.SMIRK
            )
        }
        anim(320, FastOutSlowInEasing) { t -> pose = ShadyPose(offsetX = -0.22f * (1f - t), rotation = 8f * (1f - t), mouth = MouthStyle.SMILE) }
    }

    private suspend fun cartwheel() {
        anim(120, FastOutSlowInEasing) { t -> pose = ShadyPose(scaleY = 1f - 0.12f * t, scaleX = 1f + 0.08f * t, rotation = -10f * t, eyes = EyeStyle.SQUINT) }
        anim(640, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                offsetX = 0.30f * t,
                offsetY = -0.16f * lift,
                rotation = -10f + 370f * t,
                handsUp = true,
                eyes = EyeStyle.CLOSED,
                mouth = MouthStyle.BIG_SMILE
            )
        }
        anim(300, Overshoot) { t -> pose = ShadyPose(offsetX = 0.30f, scaleY = 0.88f + 0.12f * t, scaleX = 1.08f - 0.08f * t, eyes = EyeStyle.STAR, mouth = MouthStyle.GRIN, flourish = Flourish.PUFF) }
        wait(300)
        anim(360, FastOutSlowInEasing) { t -> pose = ShadyPose(offsetX = 0.30f * (1f - t), mouth = MouthStyle.SMILE) }
    }

    private suspend fun juggle() {
        // Hands up, eyes tracking three imagined balls: gaze arcs up and over
        // in time with a small bounce. Nothing is drawn in the air — the eyes
        // are what say there is something there.
        repeat(4) { i ->
            anim(300, LinearEasing) { t ->
                val arc = sin(t * PI).toFloat()
                pose = ShadyPose(
                    handsUp = true,
                    gazeX = (if (i % 2 == 0) -0.8f else 0.8f) * (1f - t) + (if (i % 2 == 0) 0.8f else -0.8f) * t,
                    gazeY = -0.9f * arc,
                    scaleY = 1f - 0.02f * arc,
                    eyes = EyeStyle.NORMAL,
                    mouth = MouthStyle.TONGUE
                )
            }
        }
        // Drops one.
        anim(200, LinearEasing) { t -> pose = ShadyPose(gazeY = 0.9f * t, eyes = EyeStyle.WIDE, mouth = MouthStyle.GASP, flourish = Flourish.SWEAT) }
        wait(500)
    }

    private suspend fun dance() {
        // Side to side with a hip, hands up on the offbeats, notes rising.
        repeat(4) { i ->
            anim(260, FastOutSlowInEasing) { t ->
                val sway = sin(t * PI).toFloat()
                val dir = if (i % 2 == 0) 1f else -1f
                pose = ShadyPose(
                    offsetX = dir * 0.05f * sway,
                    rotation = dir * 7f * sway,
                    scaleY = 1f - 0.05f * sway,
                    scaleX = 1f + 0.03f * sway,
                    handsUp = i % 2 == 1,
                    eyes = EyeStyle.CLOSED,
                    mouth = MouthStyle.BIG_SMILE,
                    flourish = Flourish.NOTES
                )
            }
        }
        anim(200, FastOutSlowInEasing) { t -> pose = ShadyPose(mouth = MouthStyle.SMILE, flourish = Flourish.NOTES) }
    }

    private suspend fun faint() {
        // Overcome. Sways, hand to the head (both up, one is the gesture),
        // topples backward slowly, lies there, then sits up dizzy.
        anim(500, LinearEasing) { t ->
            pose = ShadyPose(rotation = sin(t * 3f * PI).toFloat() * 5f, eyes = EyeStyle.SLEEPY, mouth = MouthStyle.WOBBLE, handsUp = t > 0.4f)
        }
        anim(520, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = -80f * t, offsetY = 0.02f * t, eyes = EyeStyle.CLOSED, mouth = MouthStyle.FLAT)
        }
        pose = ShadyPose(rotation = -80f, offsetY = 0.02f, eyes = EyeStyle.CLOSED, mouth = MouthStyle.FLAT, flourish = Flourish.PUFF)
        wait(700)
        pose = ShadyPose(rotation = -80f, offsetY = 0.02f, eyes = EyeStyle.DIZZY, mouth = MouthStyle.OPEN, flourish = Flourish.SWIRL)
        wait(600)
        anim(560, Overshoot) { t -> pose = ShadyPose(rotation = -80f * (1f - t), offsetY = 0.02f * (1f - t), eyes = EyeStyle.DIZZY, mouth = MouthStyle.WOBBLE) }
        wait(300)
    }

    private suspend fun leapSpin() {
        anim(130, FastOutSlowInEasing) { t -> pose = ShadyPose(scaleY = 1f - 0.16f * t, scaleX = 1f + 0.11f * t, eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMILE) }
        anim(620, LinearEasing) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(
                offsetY = -0.30f * lift,
                scaleY = 1f + 0.10f * lift,
                scaleX = 1f - 0.06f * lift,
                rotation = 360f * t,
                handsUp = true,
                eyes = EyeStyle.STAR,
                mouth = MouthStyle.BIG_SMILE
            )
        }
        anim(300, Overshoot) { t -> pose = ShadyPose(scaleY = 0.86f + 0.14f * t, scaleX = 1.10f - 0.10f * t, eyes = EyeStyle.STAR, mouth = MouthStyle.GRIN, flourish = Flourish.PUFF) }
        wait(260)
    }

    private suspend fun slide() {
        // A run-up in place and a knee slide: low, fast, and a long scuff.
        anim(240, LinearEasing) { t ->
            val step = sin(t * 4f * PI).toFloat()
            pose = ShadyPose(offsetY = -0.03f * abs(step), rotation = step * 4f, eyes = EyeStyle.SQUINT, mouth = MouthStyle.TONGUE)
        }
        anim(560, FastOutSlowInEasing) { t ->
            pose = ShadyPose(
                offsetX = 0.34f * t,
                scaleY = 1f - 0.22f * (1f - (1f - t) * (1f - t)) ,
                scaleX = 1f + 0.10f,
                rotation = 12f,
                handsUp = true,
                eyes = EyeStyle.SQUINT,
                mouth = MouthStyle.GRIN,
                flourish = Flourish.PUFF
            )
        }
        wait(400)
        anim(360, Overshoot) { t -> pose = ShadyPose(offsetX = 0.34f * (1f - t), scaleY = 0.78f + 0.22f * t, scaleX = 1.10f - 0.10f * t, rotation = 12f * (1f - t), mouth = MouthStyle.SMILE) }
    }

    private suspend fun pounce() {
        // Sees something, crouches with the wiggle, springs at it, and it
        // was not there. The wiggle is the cat's.
        anim(200, FastOutSlowInEasing) { t -> pose = ShadyPose(eyes = EyeStyle.WIDE, gazeX = 0.9f, gazeY = 0.6f, mouth = MouthStyle.FLAT, scaleY = 1f - 0.10f * t, scaleX = 1f + 0.06f * t) }
        anim(520, LinearEasing) { t ->
            val wiggle = sin(t * 6f * PI).toFloat()
            pose = ShadyPose(eyes = EyeStyle.WIDE, gazeX = 0.9f, gazeY = 0.6f, scaleY = 0.90f, scaleX = 1.06f, rotation = wiggle * 2.5f, offsetX = wiggle * 0.006f, mouth = MouthStyle.FLAT)
        }
        anim(380, FastOutSlowInEasing) { t ->
            val lift = sin(t * PI).toFloat()
            pose = ShadyPose(offsetX = 0.26f * t, offsetY = -0.16f * lift, scaleY = 1f + 0.10f * lift, scaleX = 1f - 0.06f * lift, rotation = 12f * lift, handsUp = true, eyes = EyeStyle.WIDE, mouth = MouthStyle.OPEN)
        }
        anim(220, Overshoot) { t -> pose = ShadyPose(offsetX = 0.26f, scaleY = 0.90f + 0.10f * t, scaleX = 1.06f - 0.06f * t, eyes = EyeStyle.NORMAL, gazeY = 0.8f, mouth = MouthStyle.FLAT, flourish = Flourish.PUFF) }
        wait(400)
        pose = ShadyPose(offsetX = 0.26f, eyes = EyeStyle.NORMAL, mouth = MouthStyle.WOBBLE, browRaise = 0.5f)
        wait(500)
        anim(360, FastOutSlowInEasing) { t -> pose = ShadyPose(offsetX = 0.26f * (1f - t), mouth = MouthStyle.SMILE) }
    }

    // ============================================
    // Timing helpers
    // ============================================

    /**
     * Frame-driven rather than `delay`-driven.
     *
     * Tying every keyframe to the choreographer means a reaction runs at the
     * display's real cadence and pauses with the composition, instead of
     * drifting against it on a slow frame.
     */
    private suspend fun anim(durationMs: Int, easing: Easing, onFrame: (Float) -> Unit) {
        var start = -1L
        var t = 0f
        while (t < 1f) {
            withFrameMillis { now ->
                if (start < 0) start = now
                t = ((now - start).toFloat() / durationMs).coerceIn(0f, 1f)
                onFrame(easing.transform(t))
            }
        }
    }

    private suspend fun wait(ms: Int) = anim(ms, LinearEasing) { }
}

/** Lands past the target and settles — a body with weight, not a fade. */
private val Overshoot = CubicBezierEasing(0.2f, 1.5f, 0.4f, 1f)

@Composable
fun rememberShadyReactor(style: ReactionStyle): ShadyReactor {
    val scope = rememberCoroutineScope()
    return remember(style) { ShadyReactor(scope, style) }
}
