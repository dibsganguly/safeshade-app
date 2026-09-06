package com.safeshade.ui.screens.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.PilotLamp
import com.safeshade.ui.shady.EyeStyle
import com.safeshade.ui.shady.Flourish
import com.safeshade.ui.shady.MouthStyle
import com.safeshade.ui.shady.PropKind
import com.safeshade.ui.shady.PropLayer
import com.safeshade.ui.shady.PropState
import com.safeshade.ui.shady.Shady
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.shady.ShadyPose
import com.safeshade.ui.shady.drawProp
import com.safeshade.ui.theme.board
import kotlin.math.PI
import kotlin.math.sin

/**
 * One scene per onboarding step.
 *
 * Each is a strip of ground, in the same voice as the stage at the foot of
 * the Board: Shady on a rule, doing the thing the step is about. Welcome
 * walks on and lights a lamp; the fork shows the two people it could be for;
 * the wearer step reacts to the avatar being chosen; permissions light a
 * small board as each grant lands; pairing is the antenna searching until
 * the device answers with its own nub; the card is Shady holding it up.
 *
 * None of it is chrome and none of it reports state that the step's own
 * controls do not already say. It exists because a first run is the one time
 * the app is allowed to charm, and a form with a paragraph above it does not.
 */
@Composable
fun OnboardingScene(
    step: OnboardingSceneKind,
    modifier: Modifier = Modifier,
    height: Dp = 168.dp,
    /** Step-specific input, 0..1 or a small count; see each scene. */
    progress: Float = 0f,
    focusLeft: Boolean? = null
) {
    val colors = MaterialTheme.board
    val isStatic = LocalInspectionMode.current
    val ink = if (colors.isDark) Color(0xFFECEFF1) else Color(0xFF22282E)

    val transition = rememberInfiniteTransition(label = "onboarding-scene")
    val phaseAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase"
    )
    val phase = if (isStatic) 0.3f else phaseAnim

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.recess)
            .statusBarsPadding()
            .height(height)
    ) {
        when (step) {
            OnboardingSceneKind.WELCOME -> WelcomeScene(phase, ink, isStatic)
            OnboardingSceneKind.FORK -> ForkScene(phase, ink, focusLeft)
            OnboardingSceneKind.WEARER -> WearerScene(phase, progress, isStatic)
            OnboardingSceneKind.PERMISSIONS -> PermissionsScene(phase, progress, ink)
            OnboardingSceneKind.PAIR -> PairScene(phase, progress, ink, isStatic)
            OnboardingSceneKind.CARD -> CardScene(phase, ink)
        }
        // The ground rule, on every scene, so the strip reads as one place.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.hairline)
        )
    }
}

enum class OnboardingSceneKind { WELCOME, FORK, WEARER, PERMISSIONS, PAIR, CARD }

private const val U = 104f

// ============================================================================
// Welcome: Shady walks in from the left, stops beside a small board, and the
// board's lamp warms up. The app, in one motion.
// ============================================================================

@Composable
private fun WelcomeScene(phase: Float, ink: Color, isStatic: Boolean) {
    val colors = MaterialTheme.board
    var walk by remember { mutableFloatStateOf(if (isStatic) 1f else 0f) }
    var lit by remember { mutableStateOf(isStatic) }
    if (!isStatic) {
        LaunchedEffect(Unit) {
            animateTo(1400) { t -> walk = t }
            lit = true
        }
    }
    val u = with(LocalDensity.current) { U.dp.toPx() }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthDp = maxWidth.value
        val plateWDp = U * 0.62f
        val plateLeftDp = widthDp - plateWDp - U * 0.20f
        // The little board plate on the right, with one lamp drawn in the
        // canvas so it sits exactly on its row.
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val ground = size.height - 1f
            val plateW = u * 0.62f
            val plateH = u * 0.50f
            val x = w - plateW - u * 0.20f
            val y = ground - plateH - u * 0.02f
            drawOval((if (colors.isDark) Color.Black else ink).copy(alpha = 0.10f), Offset(x + plateW * 0.05f, ground - u * 0.03f), Size(plateW * 0.9f, u * 0.05f))
            drawRoundRect(colors.plate, Offset(x, y), Size(plateW, plateH), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(colors.hairline, Offset(x, y), Size(plateW, plateH), CornerRadius(u * 0.06f, u * 0.06f), style = Stroke(1.5f))
            // Three engraved rows: a bus tick and a nameplate line each, and a
            // pilot lamp on the first that warms up when the character arrives.
            repeat(3) { i ->
                val ry = y + u * 0.09f + i * u * 0.13f
                val on = i == 0 && lit
                drawRoundRect(if (on) colors.lampLive else colors.lampOff, Offset(x + u * 0.06f, ry), Size(u * 0.025f, u * 0.08f), CornerRadius(u * 0.01f, u * 0.01f))
                drawRoundRect(colors.hairline, Offset(x + u * 0.12f, ry + u * 0.025f), Size(plateW * 0.42f, u * 0.03f), CornerRadius(u * 0.015f, u * 0.015f))
                val lampC = Offset(x + plateW - u * 0.10f, ry + u * 0.04f)
                if (on) drawCircle(colors.lampLive.copy(alpha = 0.25f), u * 0.065f, lampC)
                drawCircle(colors.hairline, u * 0.042f, lampC)
                drawCircle(if (on) colors.lampLive else colors.lampOff, u * 0.034f, lampC)
            }
        }
        val step = sin(walk * 9f * PI).toFloat()
        val walking = walk < 1f
        // Walks in from off the left edge and stops just short of the plate.
        val startDp = -U * 0.9f
        val endDp = plateLeftDp - U * 0.92f
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (startDp + (endDp - startDp) * walk).dp)
        ) {
            Shady(
                mood = if (lit) ShadyMood.WATCHING else ShadyMood.CALM,
                size = U.dp,
                pose = if (walking) ShadyPose(offsetY = -0.035f * kotlin.math.abs(step), rotation = step * 3.5f, mouth = MouthStyle.SMILE)
                else ShadyPose(mouth = MouthStyle.BIG_SMILE, eyes = EyeStyle.NORMAL, gazeX = 0.8f)
            )
        }
    }
}

// ============================================================================
// Fork: two people the device could be for. The focused side comes forward;
// the other waits. Left: an elderly parent with a cane. Right: the user
// themselves, phone in hand.
// ============================================================================

@Composable
private fun ForkScene(phase: Float, ink: Color, focusLeft: Boolean?) {
    val colors = MaterialTheme.board
    val leftScale by animateFloatAsState(if (focusLeft == false) 0.86f else 1f, tween(280), label = "l")
    val rightScale by animateFloatAsState(if (focusLeft == true) 0.86f else 1f, tween(280), label = "r")
    val leftAlpha by animateFloatAsState(if (focusLeft == false) 0.55f else 1f, tween(280), label = "la")
    val rightAlpha by animateFloatAsState(if (focusLeft == true) 0.55f else 1f, tween(280), label = "ra")
    val u = with(LocalDensity.current) { U.dp.toPx() }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        Canvas(Modifier.fillMaxSize()) {
            val ground = size.height - 1f
            // A cane leaning beside the left figure, a phone on the ground by the right.
            val lx = size.width * 0.27f + u * 0.46f
            drawLine(Color(0xFF7B5535).copy(alpha = leftAlpha), Offset(lx, ground - u * 0.02f), Offset(lx + u * 0.06f, ground - u * 0.62f), u * 0.035f, StrokeCap.Round)
            drawPath(
                Path().apply {
                    moveTo(lx + u * 0.06f, ground - u * 0.62f)
                    quadraticTo(lx + u * 0.08f, ground - u * 0.74f, lx - u * 0.04f, ground - u * 0.70f)
                },
                Color(0xFF7B5535).copy(alpha = leftAlpha), style = Stroke(u * 0.035f, cap = StrokeCap.Round)
            )
            drawProp(
                PropState(PropKind.PHONE, 0f, presence = rightAlpha, a = if (focusLeft == false) 1f else 0f),
                PropLayer.BEHIND, size.width * 0.73f + u * 0.62f, ground, u, phase, ink, colors.isDark
            )
        }
        Box(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (w * 0.27f - U * 0.5f).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Shady(
                mood = ShadyMood.CALM, size = U.dp,
                pose = ShadyPose(scaleX = leftScale, scaleY = leftScale, eyes = EyeStyle.SQUINT, mouth = MouthStyle.SMILE, facingLeft = true, gazeX = 0.5f)
            )
        }
        Box(
            modifier = Modifier.align(Alignment.BottomStart).offset(x = (w * 0.73f - U * 0.5f).dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Shady(
                mood = ShadyMood.CALM, size = U.dp,
                pose = ShadyPose(scaleX = rightScale, scaleY = rightScale, mouth = MouthStyle.SMILE, gazeX = 0.5f, gazeY = 0.4f, flourish = if (focusLeft == false) Flourish.SPARK else Flourish.NONE)
            )
        }
    }
}

// ============================================================================
// Wearer: Shady peeks at the avatar as it changes. `progress` is bumped by the
// caller on every change; the character does a small double-take on each.
// ============================================================================

@Composable
private fun WearerScene(phase: Float, progress: Float, isStatic: Boolean) {
    var kick by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        if (isStatic || progress == 0f) return@LaunchedEffect
        animateTo(420) { t -> kick = sin(t * PI).toFloat() }
        kick = 0f
    }
    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-8).dp)) {
            Shady(
                mood = ShadyMood.CURIOUS, size = U.dp,
                pose = ShadyPose(
                    rotation = -6f * kick,
                    scaleY = 1f + 0.06f * kick,
                    eyes = if (kick > 0.3f) EyeStyle.WIDE else EyeStyle.NORMAL,
                    mouth = if (kick > 0.3f) MouthStyle.GASP else MouthStyle.SMIRK,
                    gazeX = 0.9f, gazeY = -0.6f,
                    flourish = if (kick > 0.3f) Flourish.SPARK else Flourish.NONE
                )
            )
        }
    }
}

// ============================================================================
// Permissions: a small board with three lamps that light as grants land.
// `progress` is the count granted, 0..3.
// ============================================================================

@Composable
private fun PermissionsScene(phase: Float, progress: Float, ink: Color) {
    val colors = MaterialTheme.board
    val u = with(LocalDensity.current) { U.dp.toPx() }
    val granted = progress.toInt()
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        Canvas(Modifier.fillMaxSize()) {
            val ground = size.height - 1f
            val plateW = u * 0.70f
            val plateH = u * 0.62f
            val x = size.width * 0.60f
            val y = ground - plateH - u * 0.02f
            drawRoundRect(colors.plate, Offset(x, y), Size(plateW, plateH), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(colors.hairline, Offset(x, y), Size(plateW, plateH), CornerRadius(u * 0.06f, u * 0.06f), style = Stroke(1.5f))
            repeat(3) { i ->
                val ry = y + u * 0.10f + i * u * 0.16f
                val on = i < granted
                drawRoundRect(if (on) colors.lampLive else colors.lampOff, Offset(x + u * 0.06f, ry), Size(u * 0.025f, u * 0.09f), CornerRadius(u * 0.01f, u * 0.01f))
                drawRoundRect(colors.hairline, Offset(x + u * 0.13f, ry + u * 0.03f), Size(plateW * 0.40f, u * 0.03f), CornerRadius(u * 0.015f, u * 0.015f))
                drawCircle(if (on) colors.lampLive else colors.lampOff, u * 0.035f, Offset(x + plateW - u * 0.10f, ry + u * 0.045f))
                if (on) drawCircle(colors.lampLive.copy(alpha = 0.25f), u * 0.06f, Offset(x + plateW - u * 0.10f, ry + u * 0.045f))
            }
            drawOval((if (colors.isDark) Color.Black else ink).copy(alpha = 0.10f), Offset(x + plateW * 0.05f, ground - u * 0.03f), Size(plateW * 0.9f, u * 0.05f))
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).offset(x = (w * 0.60f - U - 8f).dp)) {
            Shady(
                mood = if (granted >= 3) ShadyMood.PROUD else ShadyMood.CALM, size = U.dp,
                pose = ShadyPose(
                    gazeX = 0.9f, gazeY = 0.2f,
                    mouth = if (granted >= 3) MouthStyle.GRIN else if (granted > 0) MouthStyle.SMILE else MouthStyle.FLAT,
                    handsUp = granted >= 3
                )
            )
        }
    }
}

// ============================================================================
// Pair: the antenna searches; when the device answers, a second nub lights on
// a small device outline to the right. `progress` is 0 searching, 1 found.
// ============================================================================

@Composable
private fun PairScene(phase: Float, progress: Float, ink: Color, isStatic: Boolean) {
    val colors = MaterialTheme.board
    val u = with(LocalDensity.current) { U.dp.toPx() }
    val found = progress >= 1f
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth.value
        Canvas(Modifier.fillMaxSize()) {
            val ground = size.height - 1f
            // The wearable: a small rounded device with a screen and a nub.
            val dw = u * 0.42f
            val dh = u * 0.30f
            val x = size.width * 0.66f
            val y = ground - dh - u * 0.04f
            drawRoundRect(Color(0xFF22282E), Offset(x, y), Size(dw, dh), CornerRadius(u * 0.06f, u * 0.06f))
            drawRoundRect(Color(0xFF3A4249), Offset(x + u * 0.02f, y + u * 0.02f), Size(dw * 0.4f, u * 0.03f), CornerRadius(u * 0.01f, u * 0.01f))
            drawRoundRect(if (found) Color(0xFF6FD3CC).copy(alpha = 0.9f) else Color(0xFF14171A), Offset(x + dw * 0.12f, y + dh * 0.28f), Size(dw * 0.76f, dh * 0.5f), CornerRadius(u * 0.015f, u * 0.015f))
            // Its own antenna nub.
            drawLine(ink, Offset(x + dw * 0.85f, y), Offset(x + dw * 0.95f, y - u * 0.16f), u * 0.025f, StrokeCap.Round)
            drawCircle(ink, u * 0.05f, Offset(x + dw * 0.95f, y - u * 0.17f))
            drawCircle(if (found) Color(0xFF6FD3CC) else Color(0xFF6B7480), u * 0.034f, Offset(x + dw * 0.95f, y - u * 0.17f))
            drawOval((if (colors.isDark) Color.Black else ink).copy(alpha = 0.12f), Offset(x + dw * 0.05f, ground - u * 0.03f), Size(dw * 0.9f, u * 0.05f))
            // Searching rings between them, or a single settled line when found.
            if (!found) {
                repeat(3) { i ->
                    val t = ((phase * 1.4f + i * 0.33f) % 1f)
                    val cx = size.width * 0.40f + t * (size.width * 0.24f)
                    val r = u * (0.04f + 0.10f * t)
                    drawCircle(Color(0xFF6FD3CC).copy(alpha = (1f - t) * 0.5f), r, Offset(cx, ground - u * 0.62f), style = Stroke(u * 0.02f))
                }
            } else {
                drawLine(Color(0xFF6FD3CC).copy(alpha = 0.7f), Offset(size.width * 0.40f, ground - u * 0.62f), Offset(x + dw * 0.95f, y - u * 0.17f), u * 0.018f, StrokeCap.Round)
            }
        }
        Box(modifier = Modifier.align(Alignment.BottomStart).offset(x = (w * 0.38f - U * 0.76f).dp)) {
            Shady(
                mood = if (found) ShadyMood.WATCHING else ShadyMood.SEARCHING, size = U.dp,
                pose = if (found) ShadyPose(mouth = MouthStyle.BIG_SMILE, gazeX = 0.8f, flourish = Flourish.SPARK) else ShadyPose(gazeX = 0.9f)
            )
        }
    }
}

// ============================================================================
// Card: Shady holds up the emergency card — a small plate with three lines
// and a QR corner — to the camera.
// ============================================================================

@Composable
private fun CardScene(phase: Float, ink: Color) {
    val colors = MaterialTheme.board
    val u = with(LocalDensity.current) { U.dp.toPx() }
    Box(Modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.BottomCenter).offset(x = (-22).dp)) {
            Shady(mood = ShadyMood.CALM, size = U.dp, pose = ShadyPose(handsUp = true, mouth = MouthStyle.SMILE, gazeX = 0.9f, gazeY = -0.3f))
        }
        Canvas(Modifier.fillMaxSize()) {
            val ground = size.height - 1f
            val cw = u * 0.52f
            val ch = u * 0.34f
            val x = size.width * 0.5f + u * 0.28f
            val y = ground - u * 0.74f + sin(phase * 2f * PI).toFloat() * u * 0.012f
            drawRoundRect((if (colors.isDark) Color.Black else ink).copy(alpha = 0.10f), Offset(x + u * 0.02f, y + u * 0.03f), Size(cw, ch), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(colors.plate, Offset(x, y), Size(cw, ch), CornerRadius(u * 0.04f, u * 0.04f))
            drawRoundRect(colors.hairline, Offset(x, y), Size(cw, ch), CornerRadius(u * 0.04f, u * 0.04f), style = Stroke(1.5f))
            drawRoundRect(Color(0xFFE5484D), Offset(x, y), Size(cw, u * 0.05f), CornerRadius(u * 0.04f, u * 0.04f))
            repeat(3) { i ->
                drawRoundRect(colors.hairline, Offset(x + u * 0.05f, y + u * 0.10f + i * u * 0.07f), Size(cw * (0.55f - i * 0.1f), u * 0.03f), CornerRadius(u * 0.015f, u * 0.015f))
            }
            // QR corner: a 4x4 of squares.
            val q = u * 0.032f
            repeat(4) { r -> repeat(4) { c ->
                if ((r + c) % 2 == 0 || r == 0 || c == 0) drawRect(ink, Offset(x + cw - u * 0.18f + c * q, y + u * 0.10f + r * q), Size(q * 0.85f, q * 0.85f))
            } }
        }
    }
}

private suspend fun animateTo(durationMs: Int, onFrame: (Float) -> Unit) {
    var start = -1L
    var t = 0f
    while (t < 1f) {
        withFrameMillis { now ->
            if (start < 0) start = now
            t = ((now - start).toFloat() / durationMs).coerceIn(0f, 1f)
            onFrame(t)
        }
    }
}
