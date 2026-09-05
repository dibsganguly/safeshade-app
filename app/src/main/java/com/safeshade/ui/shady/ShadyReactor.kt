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
            listOf(::blinkAndBeam, ::littleHop, ::curiousTilt, ::quickWave, ::sparkle, ::ponder)
        } else {
            listOf(::startle, ::spin, ::cheer, ::tumble, ::delight, ::wobble)
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

    // ============================================
    // Bold set — the stage
    // ============================================

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
        anim(620, FastOutSlowInEasing) { t ->
            pose = ShadyPose(rotation = 360f * t, mouth = MouthStyle.OPEN, eyes = EyeStyle.CLOSED)
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
            pose = pose.copy(rotation = wobble * 7f, offsetY = -0.04f * kotlin.math.abs(wobble))
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
