package com.safeshade.ui.shady

/**
 * A momentary override on top of a mood.
 *
 * Mood answers "how is Shady, given the state of the link". Pose answers "what
 * is Shady doing right now" — mid-hop, mid-spin, startled by a tap. Keeping the
 * two separate is what lets the same character be simultaneously *concerned*
 * about a low battery and *mid-stretch*, instead of needing a mood per
 * combination.
 *
 * Every field is a neutral default, so `ShadyPose()` renders exactly the mood.
 *
 * Offsets and translations are expressed as fractions of the drawn size rather
 * than in dp, so an action looks identical at 56dp in a card and at 104dp on
 * the stage.
 */
data class ShadyPose(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    /** Squash and stretch. A hop compresses on take-off and extends in the air. */
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val eyes: EyeStyle? = null,
    val mouth: MouthStyle? = null,
    /** Both hands raised, for a wave or a celebration. */
    val handsUp: Boolean = false,
    /** Mirrors the character so it can walk the other way. */
    val facingLeft: Boolean = false,
    /** Small marks above the head: sleep, surprise, delight. */
    val flourish: Flourish = Flourish.NONE
) {
    companion object {
        val Neutral = ShadyPose()
    }
}

enum class EyeStyle {
    NORMAL,

    /** Content, shut. Also used for the blink. */
    CLOSED,

    /** Startled. Bigger whites, smaller pupils. */
    WIDE,

    /** Delight. */
    STAR,

    /** Dazed after a spin or a tumble. */
    DIZZY,

    /** Amused, or looking into the sun. */
    SQUINT
}

enum class MouthStyle { SMILE, BIG_SMILE, OPEN, FLAT, FROWN, WOBBLE }

enum class Flourish {
    NONE,

    /** Two small z's. Asleep. */
    SLEEP,

    /** A pair of ticks. Surprise, or a sudden idea. */
    SPARK,

    /** Three dots. Thinking. */
    THINK,

    /** A small ring, drawn while dizzy. */
    SWIRL
}
