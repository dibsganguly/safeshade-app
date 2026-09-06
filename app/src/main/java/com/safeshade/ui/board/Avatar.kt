package com.safeshade.ui.board

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke as StrokeTokens
import com.safeshade.ui.theme.accents
import com.safeshade.ui.theme.board
import com.safeshade.ui.theme.boardType
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A face for a person in the app: the account holder, a wearer, a member of
 * the Circle.
 *
 * Three kinds of avatar share one string id, so a profile row stores a single
 * value and every screen draws it the same way:
 *
 *  - `""` — none yet. Draws the person's initial on a disc in their accent.
 *  - `"a:…"` — a procedural face, built from [AvatarSpec]. Ours, drawn in the
 *    character's flat two-tone voice, so it sits beside Shady without a clash
 *    of rendering styles. Apple's Memoji is what the user asked for by name
 *    and is proprietary; this is the same idea drawn in our own hand.
 *  - `"photo:<file>"` — a photo the person chose, stored app-private under
 *    `files/avatars/`. The picker copies it there so the id never points at a
 *    content URI whose permission expires.
 *
 * Faces are deliberately simple: a head, hair, brows, eyes, a mouth, optional
 * glasses and one accessory, on an accent disc. The hair set includes a
 * headscarf and a turban because the people this product is for wear them.
 */
data class AvatarSpec(
    /** 0 round, 1 oval, 2 square, 3 heart. */
    val face: Int = 0,
    /** Index into [SKIN]. */
    val skin: Int = 2,
    /** 0 buzz, 1 short, 2 side part, 3 curly, 4 bun, 5 long, 6 headscarf, 7 turban, 8 cap. */
    val hair: Int = 1,
    /** Index into [HAIR]. */
    val hairColor: Int = 0,
    /** 0 straight, 1 arched, 2 thick. */
    val brows: Int = 0,
    /** 0 dots, 1 almond, 2 lashes, 3 happy (closed arcs). */
    val eyes: Int = 0,
    /** 0 smile, 1 grin, 2 neutral, 3 open smile. */
    val mouth: Int = 0,
    /** 0 none, 1 round, 2 square. */
    val glasses: Int = 0,
    /** 0 none, 1 earrings, 2 bindi, 3 beard, 4 moustache. */
    val accessory: Int = 0,
    /** Index into the theme's twelve decorative accents, for the disc. */
    val disc: Int = 0
) {
    fun encode(): String =
        "a:f$face-s$skin-h$hair-c$hairColor-b$brows-e$eyes-m$mouth-g$glasses-x$accessory-d$disc"

    companion object {
        val SKIN = listOf(
            Color(0xFFF6D7B8), Color(0xFFE8B98F), Color(0xFFD29B6B),
            Color(0xFFB5794C), Color(0xFF8D5A34), Color(0xFF5E3A22)
        )
        val HAIR = listOf(
            Color(0xFF22282E), Color(0xFF4A3323), Color(0xFF7B5535),
            Color(0xFFA9603A), Color(0xFF8A8F96), Color(0xFFE6E2DA)
        )
        const val FACES = 4
        const val HAIRS = 9
        const val BROWS = 3
        const val EYES = 4
        const val MOUTHS = 4
        const val GLASSES = 3
        const val ACCESSORIES = 5

        fun decode(id: String): AvatarSpec? {
            if (!id.startsWith("a:")) return null
            val parts = id.removePrefix("a:").split('-')
            fun v(prefix: Char, max: Int, default: Int): Int =
                parts.firstOrNull { it.startsWith(prefix) }?.drop(1)?.toIntOrNull()
                    ?.coerceIn(0, max - 1) ?: default
            return AvatarSpec(
                face = v('f', FACES, 0),
                skin = v('s', SKIN.size, 2),
                hair = v('h', HAIRS, 1),
                hairColor = v('c', HAIR.size, 0),
                brows = v('b', BROWS, 0),
                eyes = v('e', EYES, 0),
                mouth = v('m', MOUTHS, 0),
                glasses = v('g', GLASSES, 0),
                accessory = v('x', ACCESSORIES, 0),
                disc = v('d', 12, 0)
            )
        }

        /**
         * Twenty-four faces chosen to cover the range at a glance — skin,
         * hair, age markers, headwear — so most people find one they will
         * accept without opening the customiser.
         */
        val PRESETS: List<AvatarSpec> = listOf(
            AvatarSpec(face = 0, skin = 2, hair = 1, hairColor = 0, eyes = 1, mouth = 0, disc = 0),
            AvatarSpec(face = 1, skin = 1, hair = 5, hairColor = 1, eyes = 2, mouth = 0, accessory = 1, disc = 1),
            AvatarSpec(face = 2, skin = 3, hair = 0, hairColor = 0, brows = 2, eyes = 0, mouth = 2, accessory = 3, disc = 2),
            AvatarSpec(face = 3, skin = 0, hair = 3, hairColor = 3, eyes = 3, mouth = 1, disc = 3),
            AvatarSpec(face = 0, skin = 4, hair = 6, hairColor = 0, eyes = 1, mouth = 0, disc = 4),
            AvatarSpec(face = 1, skin = 2, hair = 7, hairColor = 0, brows = 2, eyes = 1, mouth = 0, accessory = 3, disc = 5),
            AvatarSpec(face = 0, skin = 1, hair = 4, hairColor = 4, eyes = 3, mouth = 0, glasses = 1, disc = 6),
            AvatarSpec(face = 2, skin = 5, hair = 1, hairColor = 0, eyes = 0, mouth = 1, glasses = 2, disc = 7),
            AvatarSpec(face = 1, skin = 3, hair = 5, hairColor = 0, eyes = 2, mouth = 0, accessory = 2, disc = 8),
            AvatarSpec(face = 0, skin = 2, hair = 2, hairColor = 1, brows = 1, eyes = 1, mouth = 3, disc = 9),
            AvatarSpec(face = 3, skin = 1, hair = 3, hairColor = 0, eyes = 0, mouth = 0, accessory = 1, disc = 10),
            AvatarSpec(face = 2, skin = 4, hair = 8, hairColor = 0, eyes = 1, mouth = 1, disc = 11),
            AvatarSpec(face = 0, skin = 0, hair = 1, hairColor = 5, brows = 0, eyes = 3, mouth = 0, glasses = 1, disc = 1),
            AvatarSpec(face = 1, skin = 2, hair = 4, hairColor = 4, eyes = 1, mouth = 2, glasses = 2, accessory = 2, disc = 3),
            AvatarSpec(face = 0, skin = 5, hair = 6, hairColor = 0, eyes = 2, mouth = 0, disc = 5),
            AvatarSpec(face = 2, skin = 3, hair = 0, hairColor = 0, brows = 2, eyes = 1, mouth = 0, accessory = 4, disc = 7),
            AvatarSpec(face = 3, skin = 2, hair = 5, hairColor = 3, eyes = 2, mouth = 1, disc = 9),
            AvatarSpec(face = 1, skin = 4, hair = 3, hairColor = 0, eyes = 0, mouth = 3, disc = 11),
            AvatarSpec(face = 0, skin = 1, hair = 2, hairColor = 2, eyes = 1, mouth = 0, glasses = 2, disc = 0),
            AvatarSpec(face = 2, skin = 2, hair = 7, hairColor = 0, eyes = 1, mouth = 2, accessory = 3, glasses = 1, disc = 2),
            AvatarSpec(face = 1, skin = 0, hair = 4, hairColor = 1, eyes = 3, mouth = 0, accessory = 1, disc = 4),
            AvatarSpec(face = 0, skin = 3, hair = 1, hairColor = 4, brows = 2, eyes = 0, mouth = 0, accessory = 4, disc = 6),
            AvatarSpec(face = 3, skin = 5, hair = 5, hairColor = 0, eyes = 1, mouth = 1, accessory = 2, disc = 8),
            AvatarSpec(face = 1, skin = 1, hair = 8, hairColor = 2, eyes = 3, mouth = 3, disc = 10)
        )
    }
}

/** Where chosen photos live. The picker writes here; [Avatar] reads from here. */
fun avatarPhotoFile(context: android.content.Context, id: String): File? {
    if (!id.startsWith("photo:")) return null
    val name = id.removePrefix("photo:").takeIf { it.isNotBlank() && !it.contains('/') } ?: return null
    return File(File(context.filesDir, "avatars"), name)
}

/**
 * A person's avatar at any size.
 *
 * @param name used for the initial when there is no avatar, and for the
 *   spoken description always.
 */
@Composable
fun Avatar(
    avatarId: String,
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    /** A hairline ring in the plate's tone, so a photo does not bleed into the ground. */
    ringed: Boolean = true
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    val spec = remember(avatarId) { AvatarSpec.decode(avatarId) }
    val photo = remember(avatarId) {
        avatarPhotoFile(context, avatarId)?.takeIf { it.exists() }?.let { f ->
            runCatching { BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    val described = if (name.isBlank()) "Avatar" else "$name's avatar"

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(if (ringed) Modifier.border(StrokeTokens.hairline, colors.hairline, CircleShape) else Modifier)
            .clearAndSetSemantics { contentDescription = described },
        contentAlignment = Alignment.Center
    ) {
        when {
            photo != null -> Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size)
            )
            spec != null -> Canvas(Modifier.size(size)) { drawAvatar(spec, colors.accents, colors.plate) }
            else -> {
                val accent = colors.accents[((name.hashCode() % 12) + 12) % 12]
                Box(
                    modifier = Modifier.size(size).background(accent.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = if (size >= 56.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        color = colors.ink
                    )
                }
            }
        }
    }
}

// ============================================================================
// The drawing
// ============================================================================

private const val INK_ALPHA = 1f
private val Ink = Color(0xFF22282E)
private val EyeWhite = Color(0xFFFFFDF7)
private val Lip = Color(0xFFB85C5C)
private val Bindi = Color(0xFFB3151A)
private val Gold = Color(0xFFD9A441)

/** Darkens a flat tone by a fixed step, for the core band. */
private fun Color.core(): Color = Color(red * 0.80f, green * 0.78f, blue * 0.76f, alpha)
private fun Color.light(): Color = Color(
    (red + (1f - red) * 0.22f), (green + (1f - green) * 0.22f), (blue + (1f - blue) * 0.22f), alpha
)

fun DrawScope.drawAvatar(spec: AvatarSpec, accents: List<Color>, plate: Color) {
    val s = size.minDimension
    val disc = accents[spec.disc.coerceIn(0, accents.size - 1)]
    drawCircle(disc.copy(alpha = 0.28f), radius = s / 2f, center = Offset(s / 2f, s / 2f))

    val skin = AvatarSpec.SKIN[spec.skin]
    val hair = AvatarSpec.HAIR[spec.hairColor]

    // Everything below the disc's equator is clipped to the disc, so the
    // shoulders and long hair end at the rim rather than at a hard line.
    val discPath = Path().apply { addOval(Rect(0f, 0f, s, s)) }
    clipPath(discPath) {
        // Shoulders.
        drawRoundRect(
            color = plateShirt(disc),
            topLeft = Offset(s * 0.18f, s * 0.80f),
            size = Size(s * 0.64f, s * 0.40f),
            cornerRadius = CornerRadius(s * 0.16f, s * 0.16f)
        )
        drawRoundRect(
            color = plateShirt(disc).core(),
            topLeft = Offset(s * 0.62f, s * 0.84f),
            size = Size(s * 0.20f, s * 0.36f),
            cornerRadius = CornerRadius(s * 0.10f, s * 0.10f)
        )
        // Neck.
        drawRoundRect(skin.core(), Offset(s * 0.42f, s * 0.68f), Size(s * 0.16f, s * 0.18f), CornerRadius(s * 0.04f, s * 0.04f))

        // Hair behind the head (long, curly, bun back).
        drawHairBack(spec, s, hair)

        // Head.
        val head = headPath(spec.face, s)
        drawPath(head, skin)
        // Core band along the right and jaw; rim along the upper left.
        clipPath(head) {
            val shifted = headPath(spec.face, s, dx = -s * 0.035f, dy = -s * 0.03f)
            val band = Path().apply { op(head, shifted, PathOperation.Difference) }
            drawPath(band, skin.core())
            val rimShift = headPath(spec.face, s, dx = s * 0.04f, dy = s * 0.04f)
            val rim = Path().apply { op(head, rimShift, PathOperation.Difference) }
            drawPath(rim, skin.light())
            // Ears.
        }
        listOf(s * 0.245f, s * 0.755f).forEach { ex ->
            drawCircle(skin, s * 0.045f, Offset(ex, s * 0.50f))
            drawCircle(skin.core(), s * 0.02f, Offset(ex, s * 0.50f))
        }

        drawFace(spec, s, skin)
        drawHairFront(spec, s, hair, skin)
        drawAccessory(spec, s, hair)
        drawGlasses(spec, s)
    }
}

/** A shirt in a deeper cut of the disc's own accent, so the pair reads as one identity. */
private fun plateShirt(disc: Color): Color = Color(disc.red * 0.82f, disc.green * 0.82f, disc.blue * 0.86f)

private fun headPath(face: Int, s: Float, dx: Float = 0f, dy: Float = 0f): Path {
    val cx = s * 0.5f + dx
    val cy = s * 0.47f + dy
    return Path().apply {
        when (face) {
            // Round.
            0 -> addOval(Rect(cx - s * 0.235f, cy - s * 0.25f, cx + s * 0.235f, cy + s * 0.26f))
            // Oval.
            1 -> addOval(Rect(cx - s * 0.215f, cy - s * 0.27f, cx + s * 0.215f, cy + s * 0.28f))
            // Square.
            2 -> addRoundRect(RoundRect(Rect(cx - s * 0.235f, cy - s * 0.25f, cx + s * 0.235f, cy + s * 0.26f), CornerRadius(s * 0.13f, s * 0.13f)))
            // Heart: wide at the temples, narrowing to the chin.
            else -> {
                moveTo(cx, cy + s * 0.27f)
                cubicTo(cx - s * 0.16f, cy + s * 0.24f, cx - s * 0.25f, cy + s * 0.05f, cx - s * 0.24f, cy - s * 0.10f)
                cubicTo(cx - s * 0.23f, cy - s * 0.27f, cx - s * 0.10f, cy - s * 0.26f, cx, cy - s * 0.26f)
                cubicTo(cx + s * 0.10f, cy - s * 0.26f, cx + s * 0.23f, cy - s * 0.27f, cx + s * 0.24f, cy - s * 0.10f)
                cubicTo(cx + s * 0.25f, cy + s * 0.05f, cx + s * 0.16f, cy + s * 0.24f, cx, cy + s * 0.27f)
                close()
            }
        }
    }
}

private fun DrawScope.drawFace(spec: AvatarSpec, s: Float, skin: Color) {
    val cx = s * 0.5f
    val eyeY = s * 0.47f
    val eyeDx = s * 0.085f
    val ink = Ink.copy(alpha = INK_ALPHA)

    // Brows.
    val browY = eyeY - s * 0.075f
    val browW = s * 0.07f
    val browStroke = if (spec.brows == 2) s * 0.030f else s * 0.018f
    listOf(cx - eyeDx, cx + eyeDx).forEachIndexed { i, ex ->
        val dir = if (i == 0) -1f else 1f
        when (spec.brows) {
            1 -> drawPath(
                Path().apply {
                    moveTo(ex - browW, browY + s * 0.012f)
                    quadraticTo(ex, browY - s * 0.022f, ex + browW, browY + s * 0.012f)
                }, ink, style = Stroke(browStroke, cap = StrokeCap.Round)
            )
            else -> drawLine(ink, Offset(ex - browW, browY + dir * s * 0.004f), Offset(ex + browW, browY - dir * s * 0.004f), browStroke, StrokeCap.Round)
        }
    }

    // Eyes.
    listOf(cx - eyeDx, cx + eyeDx).forEach { ex ->
        when (spec.eyes) {
            0 -> drawCircle(ink, s * 0.024f, Offset(ex, eyeY))
            1 -> {
                drawOval(EyeWhite, Offset(ex - s * 0.045f, eyeY - s * 0.028f), Size(s * 0.09f, s * 0.056f))
                drawCircle(ink, s * 0.022f, Offset(ex, eyeY + s * 0.002f))
                drawCircle(EyeWhite, s * 0.007f, Offset(ex + s * 0.008f, eyeY - s * 0.008f))
            }
            2 -> {
                drawOval(EyeWhite, Offset(ex - s * 0.045f, eyeY - s * 0.028f), Size(s * 0.09f, s * 0.056f))
                drawCircle(ink, s * 0.022f, Offset(ex, eyeY + s * 0.002f))
                drawCircle(EyeWhite, s * 0.007f, Offset(ex + s * 0.008f, eyeY - s * 0.008f))
                // Lashes: a lid line and two ticks.
                drawPath(
                    Path().apply {
                        moveTo(ex - s * 0.045f, eyeY - s * 0.02f)
                        quadraticTo(ex, eyeY - s * 0.05f, ex + s * 0.045f, eyeY - s * 0.02f)
                    }, ink, style = Stroke(s * 0.014f, cap = StrokeCap.Round)
                )
                drawLine(ink, Offset(ex + s * 0.040f, eyeY - s * 0.026f), Offset(ex + s * 0.055f, eyeY - s * 0.040f), s * 0.012f, StrokeCap.Round)
            }
            else -> drawPath(
                Path().apply {
                    moveTo(ex - s * 0.04f, eyeY + s * 0.006f)
                    quadraticTo(ex, eyeY - s * 0.034f, ex + s * 0.04f, eyeY + s * 0.006f)
                }, ink, style = Stroke(s * 0.020f, cap = StrokeCap.Round)
            )
        }
    }

    // Nose: a small shadow-side hook.
    drawPath(
        Path().apply {
            moveTo(cx + s * 0.004f, eyeY + s * 0.03f)
            quadraticTo(cx + s * 0.03f, eyeY + s * 0.095f, cx - s * 0.008f, eyeY + s * 0.098f)
        }, skin.core(), style = Stroke(s * 0.016f, cap = StrokeCap.Round)
    )

    // Mouth.
    val my = s * 0.62f
    val mw = s * 0.06f
    when (spec.mouth) {
        0 -> drawPath(
            Path().apply { moveTo(cx - mw, my); quadraticTo(cx, my + s * 0.045f, cx + mw, my) },
            ink, style = Stroke(s * 0.018f, cap = StrokeCap.Round)
        )
        1 -> {
            val p = Path().apply { moveTo(cx - mw * 1.3f, my - s * 0.004f); quadraticTo(cx, my + s * 0.07f, cx + mw * 1.3f, my - s * 0.004f); close() }
            drawPath(p, ink)
            drawRect(EyeWhite, Offset(cx - mw * 1.0f, my), Size(mw * 2f, s * 0.016f))
        }
        2 -> drawLine(ink, Offset(cx - mw * 0.7f, my + s * 0.01f), Offset(cx + mw * 0.7f, my + s * 0.01f), s * 0.016f, StrokeCap.Round)
        else -> {
            val p = Path().apply { moveTo(cx - mw * 1.1f, my - s * 0.002f); quadraticTo(cx, my + s * 0.075f, cx + mw * 1.1f, my - s * 0.002f); close() }
            drawPath(p, ink)
            drawOval(Lip, Offset(cx - mw * 0.55f, my + s * 0.028f), Size(mw * 1.1f, s * 0.03f))
        }
    }
}

private fun DrawScope.drawHairBack(spec: AvatarSpec, s: Float, hair: Color) {
    val cx = s * 0.5f
    when (spec.hair) {
        // Long: a mantle behind the head down to the shoulders.
        5 -> drawRoundRect(hair, Offset(cx - s * 0.27f, s * 0.24f), Size(s * 0.54f, s * 0.62f), CornerRadius(s * 0.22f, s * 0.22f))
        // Curly: a wider cloud behind.
        3 -> repeat(9) { i ->
            val a = (PI * (0.95f + i * 0.13f)).toFloat()
            drawCircle(hair, s * 0.085f, Offset(cx + cos(a) * s * 0.25f, s * 0.44f + sin(a) * s * 0.25f))
        }
        // Bun: the knot on top.
        4 -> drawCircle(hair, s * 0.085f, Offset(cx, s * 0.175f))
        else -> Unit
    }
}

private fun DrawScope.drawHairFront(spec: AvatarSpec, s: Float, hair: Color, skin: Color) {
    val cx = s * 0.5f
    val top = s * 0.22f
    val hairLight = hair.light()
    when (spec.hair) {
        0 -> {
            // Buzz: a faint cap in a half-tone of the hair colour.
            val cap = Path().apply {
                moveTo(cx - s * 0.235f, s * 0.42f)
                quadraticTo(cx - s * 0.23f, top - s * 0.02f, cx, top - s * 0.02f)
                quadraticTo(cx + s * 0.23f, top - s * 0.02f, cx + s * 0.235f, s * 0.42f)
                quadraticTo(cx, s * 0.36f, cx - s * 0.235f, s * 0.42f)
                close()
            }
            drawPath(cap, hair.copy(alpha = 0.45f))
        }
        1 -> {
            // Short: a clean cap with a fringe line.
            val cap = Path().apply {
                moveTo(cx - s * 0.24f, s * 0.40f)
                quadraticTo(cx - s * 0.235f, top - s * 0.03f, cx, top - s * 0.03f)
                quadraticTo(cx + s * 0.235f, top - s * 0.03f, cx + s * 0.24f, s * 0.40f)
                lineTo(cx + s * 0.20f, s * 0.40f)
                quadraticTo(cx + s * 0.12f, s * 0.30f, cx - s * 0.06f, s * 0.33f)
                quadraticTo(cx - s * 0.18f, s * 0.35f, cx - s * 0.20f, s * 0.42f)
                close()
            }
            drawPath(cap, hair)
            drawPath(Path().apply { moveTo(cx - s * 0.16f, s * 0.245f); quadraticTo(cx - s * 0.02f, s * 0.20f, cx + s * 0.10f, s * 0.235f) }, hairLight, style = Stroke(s * 0.022f, cap = StrokeCap.Round))
        }
        2 -> {
            // Side part: swept to the right.
            val cap = Path().apply {
                moveTo(cx - s * 0.24f, s * 0.44f)
                quadraticTo(cx - s * 0.24f, top - s * 0.03f, cx - s * 0.02f, top - s * 0.035f)
                quadraticTo(cx + s * 0.24f, top - s * 0.04f, cx + s * 0.245f, s * 0.36f)
                quadraticTo(cx + s * 0.20f, s * 0.30f, cx + s * 0.06f, s * 0.31f)
                quadraticTo(cx - s * 0.12f, s * 0.33f, cx - s * 0.19f, s * 0.30f)
                quadraticTo(cx - s * 0.22f, s * 0.34f, cx - s * 0.24f, s * 0.44f)
                close()
            }
            drawPath(cap, hair)
            drawPath(Path().apply { moveTo(cx - s * 0.12f, s * 0.235f); quadraticTo(cx + s * 0.04f, s * 0.20f, cx + s * 0.16f, s * 0.24f) }, hairLight, style = Stroke(s * 0.022f, cap = StrokeCap.Round))
        }
        3 -> {
            // Curly: bumps over the crown.
            repeat(6) { i ->
                val a = (PI * (1.05f + i * 0.18f)).toFloat()
                drawCircle(hair, s * 0.078f, Offset(cx + cos(a) * s * 0.20f, s * 0.42f + sin(a) * s * 0.21f))
            }
            drawCircle(hairLight, s * 0.03f, Offset(cx - s * 0.10f, s * 0.235f))
        }
        4 -> {
            // Bun: a smooth cap pulled back.
            val cap = Path().apply {
                moveTo(cx - s * 0.235f, s * 0.42f)
                quadraticTo(cx - s * 0.235f, top - s * 0.02f, cx, top - s * 0.02f)
                quadraticTo(cx + s * 0.235f, top - s * 0.02f, cx + s * 0.235f, s * 0.42f)
                quadraticTo(cx, s * 0.31f, cx - s * 0.235f, s * 0.42f)
                close()
            }
            drawPath(cap, hair)
            drawPath(Path().apply { moveTo(cx - s * 0.06f, s * 0.24f); quadraticTo(cx, s * 0.215f, cx + s * 0.08f, s * 0.245f) }, hairLight, style = Stroke(s * 0.02f, cap = StrokeCap.Round))
        }
        5 -> {
            // Long: the front of the mantle, parted in the middle.
            val cap = Path().apply {
                moveTo(cx - s * 0.25f, s * 0.60f)
                lineTo(cx - s * 0.25f, s * 0.36f)
                quadraticTo(cx - s * 0.24f, top - s * 0.03f, cx, top - s * 0.03f)
                quadraticTo(cx + s * 0.24f, top - s * 0.03f, cx + s * 0.25f, s * 0.36f)
                lineTo(cx + s * 0.25f, s * 0.60f)
                lineTo(cx + s * 0.19f, s * 0.60f)
                quadraticTo(cx + s * 0.22f, s * 0.36f, cx + s * 0.02f, s * 0.30f)
                lineTo(cx - s * 0.02f, s * 0.30f)
                quadraticTo(cx - s * 0.22f, s * 0.36f, cx - s * 0.19f, s * 0.60f)
                close()
            }
            drawPath(cap, hair)
            drawPath(Path().apply { moveTo(cx - s * 0.17f, s * 0.27f); quadraticTo(cx - s * 0.10f, s * 0.215f, cx - s * 0.03f, s * 0.235f) }, hairLight, style = Stroke(s * 0.02f, cap = StrokeCap.Round))
        }
        6 -> {
            // Headscarf: a soft hood framing the face, in the disc's family.
            val scarf = Color(0xFF8C6E9E)
            val hood = Path().apply {
                moveTo(cx - s * 0.30f, s * 0.86f)
                lineTo(cx - s * 0.29f, s * 0.40f)
                quadraticTo(cx - s * 0.28f, top - s * 0.06f, cx, top - s * 0.06f)
                quadraticTo(cx + s * 0.28f, top - s * 0.06f, cx + s * 0.29f, s * 0.40f)
                lineTo(cx + s * 0.30f, s * 0.86f)
                lineTo(cx + s * 0.21f, s * 0.86f)
                quadraticTo(cx + s * 0.24f, s * 0.38f, cx, s * 0.27f)
                quadraticTo(cx - s * 0.24f, s * 0.38f, cx - s * 0.21f, s * 0.86f)
                close()
            }
            drawPath(hood, scarf)
            drawPath(Path().apply { moveTo(cx - s * 0.20f, s * 0.30f); quadraticTo(cx, s * 0.20f, cx + s * 0.20f, s * 0.30f) }, scarf.light(), style = Stroke(s * 0.024f, cap = StrokeCap.Round))
        }
        7 -> {
            // Turban: layered folds above the brow.
            val cloth = Color(0xFFB9705A)
            val base = Path().apply {
                moveTo(cx - s * 0.26f, s * 0.36f)
                quadraticTo(cx - s * 0.26f, top - s * 0.09f, cx, top - s * 0.09f)
                quadraticTo(cx + s * 0.26f, top - s * 0.09f, cx + s * 0.26f, s * 0.36f)
                quadraticTo(cx, s * 0.27f, cx - s * 0.26f, s * 0.36f)
                close()
            }
            drawPath(base, cloth)
            drawPath(Path().apply { moveTo(cx - s * 0.22f, s * 0.33f); quadraticTo(cx - s * 0.02f, s * 0.15f, cx + s * 0.24f, s * 0.30f) }, cloth.core(), style = Stroke(s * 0.022f, cap = StrokeCap.Round))
            drawPath(Path().apply { moveTo(cx - s * 0.25f, s * 0.29f); quadraticTo(cx - s * 0.06f, s * 0.20f, cx + s * 0.16f, s * 0.19f) }, cloth.light(), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
            // A little hair at the temples under the wrap.
            drawRoundRect(hair, Offset(cx - s * 0.245f, s * 0.36f), Size(s * 0.06f, s * 0.05f), CornerRadius(s * 0.02f, s * 0.02f))
            drawRoundRect(hair, Offset(cx + s * 0.185f, s * 0.36f), Size(s * 0.06f, s * 0.05f), CornerRadius(s * 0.02f, s * 0.02f))
        }
        else -> {
            // Cap: a crown and a peak to the right, over short hair.
            val capColor = Color(0xFF546F8C)
            drawRoundRect(hair, Offset(cx - s * 0.235f, s * 0.30f), Size(s * 0.47f, s * 0.10f), CornerRadius(s * 0.05f, s * 0.05f))
            val crown = Path().apply {
                moveTo(cx - s * 0.25f, s * 0.35f)
                quadraticTo(cx - s * 0.24f, top - s * 0.05f, cx, top - s * 0.05f)
                quadraticTo(cx + s * 0.24f, top - s * 0.05f, cx + s * 0.25f, s * 0.35f)
                close()
            }
            drawPath(crown, capColor)
            drawRoundRect(capColor.core(), Offset(cx + s * 0.02f, s * 0.32f), Size(s * 0.34f, s * 0.055f), CornerRadius(s * 0.025f, s * 0.025f))
            drawPath(Path().apply { moveTo(cx - s * 0.16f, s * 0.25f); quadraticTo(cx - s * 0.04f, s * 0.19f, cx + s * 0.08f, s * 0.215f) }, capColor.light(), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
        }
    }
}

private fun DrawScope.drawGlasses(spec: AvatarSpec, s: Float) {
    if (spec.glasses == 0) return
    val cx = s * 0.5f
    val eyeY = s * 0.47f
    val eyeDx = s * 0.085f
    val ink = Ink
    val stroke = Stroke(s * 0.014f, cap = StrokeCap.Round)
    listOf(cx - eyeDx, cx + eyeDx).forEach { ex ->
        if (spec.glasses == 1) {
            drawCircle(ink, s * 0.062f, Offset(ex, eyeY), style = stroke)
        } else {
            drawRoundRect(ink, Offset(ex - s * 0.062f, eyeY - s * 0.05f), Size(s * 0.124f, s * 0.10f), CornerRadius(s * 0.02f, s * 0.02f), style = stroke)
        }
    }
    drawLine(ink, Offset(cx - s * 0.023f, eyeY - s * 0.006f), Offset(cx + s * 0.023f, eyeY - s * 0.006f), s * 0.014f, StrokeCap.Round)
    drawLine(ink, Offset(cx - eyeDx - s * 0.062f, eyeY - s * 0.006f), Offset(cx - s * 0.245f, eyeY - s * 0.02f), s * 0.012f, StrokeCap.Round)
    drawLine(ink, Offset(cx + eyeDx + s * 0.062f, eyeY - s * 0.006f), Offset(cx + s * 0.245f, eyeY - s * 0.02f), s * 0.012f, StrokeCap.Round)
}

private fun DrawScope.drawAccessory(spec: AvatarSpec, s: Float, hair: Color) {
    val cx = s * 0.5f
    when (spec.accessory) {
        1 -> listOf(s * 0.245f, s * 0.755f).forEach { ex ->
            drawCircle(Gold, s * 0.018f, Offset(ex, s * 0.555f))
        }
        2 -> drawCircle(Bindi, s * 0.016f, Offset(cx, s * 0.375f))
        3 -> {
            // Beard: along the jaw, under the mouth.
            val beard = Path().apply {
                moveTo(cx - s * 0.22f, s * 0.52f)
                quadraticTo(cx - s * 0.20f, s * 0.76f, cx, s * 0.76f)
                quadraticTo(cx + s * 0.20f, s * 0.76f, cx + s * 0.22f, s * 0.52f)
                quadraticTo(cx + s * 0.16f, s * 0.60f, cx, s * 0.585f)
                quadraticTo(cx - s * 0.16f, s * 0.60f, cx - s * 0.22f, s * 0.52f)
                close()
            }
            drawPath(beard, hair)
            // The mouth shows through: a small skin gap where the lips are.
            drawOval(AvatarSpec.SKIN[spec.skin], Offset(cx - s * 0.07f, s * 0.60f), Size(s * 0.14f, s * 0.05f))
            drawPath(Path().apply { moveTo(cx - s * 0.06f, s * 0.625f); quadraticTo(cx, s * 0.66f, cx + s * 0.06f, s * 0.625f) }, Ink, style = Stroke(s * 0.016f, cap = StrokeCap.Round))
        }
        4 -> drawPath(
            Path().apply {
                moveTo(cx - s * 0.085f, s * 0.60f)
                quadraticTo(cx - s * 0.04f, s * 0.565f, cx, s * 0.585f)
                quadraticTo(cx + s * 0.04f, s * 0.565f, cx + s * 0.085f, s * 0.60f)
                quadraticTo(cx + s * 0.04f, s * 0.595f, cx, s * 0.605f)
                quadraticTo(cx - s * 0.04f, s * 0.595f, cx - s * 0.085f, s * 0.60f)
                close()
            }, hair
        )
        else -> Unit
    }
}

// ============================================================================
// Picking one
// ============================================================================

/**
 * The preset strip: twenty-four faces in a row, the chosen one ringed in
 * brass, and a customise affordance at the end. Selection is carried by ring
 * weight and a state word, never by colour alone.
 */
@Composable
fun AvatarPresetStrip(
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = MaterialTheme.board
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.gutter)
    ) {
        items(AvatarSpec.PRESETS.size) { index ->
            val spec = AvatarSpec.PRESETS[index]
            val id = spec.encode()
            val chosen = id == selectedId
            Box(
                modifier = Modifier
                    .size(size + 8.dp)
                    .clip(CircleShape)
                    .border(
                        if (chosen) StrokeTokens.brass else StrokeTokens.hairline,
                        if (chosen) colors.brass else colors.hairline,
                        CircleShape
                    )
                    .plateClickable(role = Role.RadioButton, onClick = { onSelect(id) })
                    .clearAndSetSemantics {
                        selected = chosen
                        contentDescription = "Face ${index + 1} of ${AvatarSpec.PRESETS.size}" + if (chosen) ", chosen" else ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Avatar(avatarId = id, name = "", size = size, ringed = false)
            }
        }
        if (trailing != null) item { Box(Modifier.size(size + 8.dp), contentAlignment = Alignment.Center) { trailing() } }
    }
}

/**
 * The customiser: one row of swatches per attribute, each a small live
 * rendering of the whole face with only that attribute changed, so a person
 * sees what they are choosing rather than reading a label for it.
 */
@Composable
fun AvatarCustomiser(
    spec: AvatarSpec,
    onChange: (AvatarSpec) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        AttributeRow("Face", AvatarSpec.FACES, spec.face, { spec.copy(face = it) }) { onChange(spec.copy(face = it)) }
        SwatchRow("Skin", AvatarSpec.SKIN, spec.skin) { onChange(spec.copy(skin = it)) }
        AttributeRow("Hair", AvatarSpec.HAIRS, spec.hair, { spec.copy(hair = it) }) { onChange(spec.copy(hair = it)) }
        SwatchRow("Hair colour", AvatarSpec.HAIR, spec.hairColor) { onChange(spec.copy(hairColor = it)) }
        AttributeRow("Eyes", AvatarSpec.EYES, spec.eyes, { spec.copy(eyes = it) }) { onChange(spec.copy(eyes = it)) }
        AttributeRow("Brows", AvatarSpec.BROWS, spec.brows, { spec.copy(brows = it) }) { onChange(spec.copy(brows = it)) }
        AttributeRow("Mouth", AvatarSpec.MOUTHS, spec.mouth, { spec.copy(mouth = it) }) { onChange(spec.copy(mouth = it)) }
        AttributeRow("Glasses", AvatarSpec.GLASSES, spec.glasses, { spec.copy(glasses = it) }) { onChange(spec.copy(glasses = it)) }
        AttributeRow("Extras", AvatarSpec.ACCESSORIES, spec.accessory, { spec.copy(accessory = it) }) { onChange(spec.copy(accessory = it)) }
        val accents = MaterialTheme.board.accents
        SwatchRow("Backdrop", accents.map { it.copy(alpha = 0.5f) }, spec.disc) { onChange(spec.copy(disc = it)) }
    }
}

/**
 * One attribute's options, each drawn as the whole face with only that
 * attribute changed. A person sees what they are choosing rather than reading
 * a number for it.
 */
@Composable
private fun AttributeRow(
    label: String,
    count: Int,
    selected: Int,
    variant: (Int) -> AvatarSpec,
    onSelect: (Int) -> Unit
) {
    val colors = MaterialTheme.board
    Column {
        Nameplate(label, small = true, muted = true, modifier = Modifier.padding(horizontal = Spacing.gutter))
        Spacer(Modifier.height(Spacing.xs))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = Spacing.gutter)
        ) {
            items(count) { index ->
                val chosen = index == selected
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(
                            if (chosen) StrokeTokens.brass else StrokeTokens.hairline,
                            if (chosen) colors.brass else colors.hairline,
                            CircleShape
                        )
                        .plateClickable(role = Role.RadioButton, onClick = { onSelect(index) })
                        .clearAndSetSemantics {
                            this.selected = chosen
                            contentDescription = "$label option ${index + 1}" + if (chosen) ", chosen" else ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Avatar(avatarId = variant(index).encode(), name = "", size = 50.dp, ringed = false)
                }
            }
        }
    }
}

@Composable
private fun SwatchRow(label: String, swatches: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.board
    Column {
        Nameplate(label, small = true, muted = true, modifier = Modifier.padding(horizontal = Spacing.gutter))
        Spacer(Modifier.height(Spacing.xs))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(horizontal = Spacing.gutter)
        ) {
            swatches.forEachIndexed { index, swatch ->
                val chosen = index == selected
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatch)
                        .border(
                            if (chosen) StrokeTokens.brass else StrokeTokens.hairline,
                            if (chosen) colors.ink else colors.hairline,
                            CircleShape
                        )
                        .plateClickable(role = Role.RadioButton, onClick = { onSelect(index) })
                        .clearAndSetSemantics {
                            this.selected = chosen
                            contentDescription = "$label option ${index + 1}"
                        }
                )
            }
        }
    }
}
