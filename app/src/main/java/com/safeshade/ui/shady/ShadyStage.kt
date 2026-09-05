package com.safeshade.ui.shady

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
 * ground: it wanders along it, stops to look around, hops, stretches, sits down,
 * dozes off, and occasionally just thinks about something. None of it reports
 * state and none of it is interactive chrome — it is there because an app you
 * open several times a day to check on someone you are worried about is allowed
 * to have one place that is simply pleasant.
 *
 * It earns its place by being *below the fold*: you only meet it having scrolled
 * past everything that matters, so it never competes with the board itself. The
 * status Shady in the card at the top has scrolled away by then, which is why
 * two instances never share the screen.
 *
 * Tapping it produces one of six deliberately larger reactions, and never the
 * same one twice in a row.
 */
@Composable
fun ShadyStage(
    modifier: Modifier = Modifier,
    height: Dp = 104.dp,
    characterSize: Dp = 76.dp
) {
    val reactor = rememberShadyReactor(ReactionStyle.BOLD)
    val isStatic = LocalInspectionMode.current

    var idlePose by remember { mutableStateOf(ShadyPose.Neutral) }
    var facingLeft by remember { mutableStateOf(false) }
    // Position along the ground, 0f at the left edge, 1f at the right.
    var walkT by remember { mutableFloatStateOf(0.5f) }
    var lastAction by remember { mutableStateOf(-1) }

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
                        idlePose = ShadyPose.Neutral
                        frameWait(120)
                        continue
                    }

                    val action = pickAction(lastAction)
                    lastAction = action

                    when (action) {
                        0 -> { // amble a short distance
                            val from = walkT
                            val target = (from + (Random.nextFloat() - 0.5f) * 0.6f).coerceIn(0.02f, 0.98f)
                            facingLeft = target < from
                            val ms = (900 + abs(target - from) * 2600).toInt()
                            animate(ms, LinearEasing) { t ->
                                walkT = from + (target - from) * t
                                // A walk cycle: a small bounce, and a lean into
                                // the direction of travel.
                                val step = sin(t * ms / 190f * PI).toFloat()
                                idlePose = ShadyPose(
                                    offsetY = -0.035f * abs(step),
                                    rotation = step * 3.5f,
                                    facingLeft = facingLeft,
                                    mouth = MouthStyle.SMILE
                                )
                            }
                            idlePose = ShadyPose(facingLeft = facingLeft)
                        }
                        1 -> { // stop and look around
                            idlePose = ShadyPose(facingLeft = facingLeft, eyes = EyeStyle.SQUINT)
                            frameWait(700)
                            facingLeft = !facingLeft
                            idlePose = ShadyPose(facingLeft = facingLeft, eyes = EyeStyle.SQUINT)
                            frameWait(700)
                        }
                        2 -> { // a hop, for no reason
                            animate(260, FastOutSlowInEasing) { t ->
                                val lift = sin(t * PI).toFloat()
                                idlePose = ShadyPose(
                                    offsetY = -0.18f * lift,
                                    scaleY = 1f + 0.09f * lift,
                                    scaleX = 1f - 0.06f * lift,
                                    facingLeft = facingLeft,
                                    mouth = MouthStyle.BIG_SMILE
                                )
                            }
                        }
                        3 -> { // a stretch
                            animate(520, FastOutSlowInEasing) { t ->
                                val e = sin(t * PI).toFloat()
                                idlePose = ShadyPose(
                                    scaleY = 1f + 0.13f * e,
                                    scaleX = 1f - 0.07f * e,
                                    handsUp = e > 0.4f,
                                    eyes = EyeStyle.CLOSED,
                                    facingLeft = facingLeft
                                )
                            }
                        }
                        4 -> { // sit down for a moment
                            animate(300, FastOutSlowInEasing) { t ->
                                idlePose = ShadyPose(
                                    scaleY = 1f - 0.12f * t,
                                    scaleX = 1f + 0.07f * t,
                                    facingLeft = facingLeft,
                                    mouth = MouthStyle.FLAT
                                )
                            }
                            frameWait(1400)
                            animate(320, FastOutSlowInEasing) { t ->
                                idlePose = ShadyPose(
                                    scaleY = 1f - 0.12f * (1f - t),
                                    scaleX = 1f + 0.07f * (1f - t),
                                    facingLeft = facingLeft
                                )
                            }
                        }
                        5 -> { // doze off
                            idlePose = ShadyPose(
                                scaleY = 0.94f,
                                eyes = EyeStyle.CLOSED,
                                mouth = MouthStyle.FLAT,
                                flourish = Flourish.SLEEP,
                                facingLeft = facingLeft
                            )
                            frameWait(2600)
                        }
                        else -> { // think about something
                            idlePose = ShadyPose(
                                eyes = EyeStyle.SQUINT,
                                mouth = MouthStyle.FLAT,
                                flourish = Flourish.THINK,
                                facingLeft = facingLeft
                            )
                            frameWait(1600)
                        }
                    }

                    // A beat of stillness between actions. Without it the
                    // character reads as frantic rather than alive.
                    idlePose = ShadyPose(facingLeft = facingLeft)
                    frameWait(500 + Random.nextInt(1400))
                }
            }
        }

        val pose = if (reactor.isReacting) {
            reactor.pose.copy(facingLeft = facingLeft)
        } else {
            idlePose
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = travel * walkT)
        ) {
            Shady(
                mood = ShadyMood.CALM,
                size = characterSize,
                pose = pose
            )
        }
    }
}

/** Weighted pick that never repeats the previous action. */
private fun pickAction(previous: Int): Int {
    // Walking is the connective tissue, so it comes up most; dozing is rare
    // enough to feel like a small event when it happens.
    val bag = intArrayOf(0, 0, 0, 0, 1, 1, 2, 2, 3, 4, 5, 6)
    var pick = bag[Random.nextInt(bag.size)]
    var guard = 0
    while (pick == previous && guard++ < 8) pick = bag[Random.nextInt(bag.size)]
    return pick
}

private suspend fun animate(
    durationMs: Int,
    easing: androidx.compose.animation.core.Easing,
    onFrame: (Float) -> Unit
) {
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

private suspend fun frameWait(ms: Int) = animate(ms, LinearEasing) { }
