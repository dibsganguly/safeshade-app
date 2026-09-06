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
    val flourish: Flourish = Flourish.NONE,
    /**
     * Where the pupils are aimed, in fractions of the eye radius. Non-zero
     * takes the gaze away from the mood, which otherwise drifts it with the
     * breath — so a deliberate look at the floor is not fighting an idle sweep.
     *
     * Body space, not screen space: [facingLeft] mirrors the character, so a
     * gaze aimed forward stays aimed forward whichever way it has turned, and
     * a caller that means "toward the right of the screen" has to negate it.
     * [offsetX] is the opposite — it is applied outside the mirror, so it
     * always moves the character to the right of the screen.
     *
     * Only styles that draw a pupil can honour it — [EyeStyle.NORMAL],
     * [EyeStyle.WIDE] and [EyeStyle.SLEEPY]. Pairing a gaze with a closed or
     * squinting eye silently does nothing.
     */
    val gazeX: Float = 0f,
    val gazeY: Float = 0f,
    /**
     * Eyebrows, in fractions of the eye radius. Positive raises them (curious,
     * unconvinced), negative furrows them. Brows are the loudest thing on the
     * face, so they stay at zero unless an action really means them.
     */
    val browRaise: Float = 0f,
    /**
     * Legs swinging in opposition, in fractions of the drawn size. Only reads
     * when the body is otherwise still — it is the whole point of sitting on
     * the edge of something.
     */
    val legSwing: Float = 0f,
    /**
     * How far through a turn the body is, 0 = square on, 1 = edge on.
     *
     * A mirror flip on its own reads as a cut. Passing through 1 on the way
     * from one facing to the other narrows the silhouette, slides the antenna
     * to the centreline and pulls the eyes together, so the character reads
     * as *rotating* toward the camera and away again. Set only by the stage
     * director; a reaction never turns.
     */
    val turn: Float = 0f,
    /**
     * Where the antenna tip trails, in fractions of the drawn size. Secondary
     * motion: the stalk is springy, so it lags a hop upward on take-off and
     * overshoots downward on landing, and it sweeps back against the
     * direction of travel on a walk. Normally computed from the pose's own
     * frame-to-frame motion inside [Shady]; a beat may set it deliberately.
     */
    val antennaLagX: Float = 0f,
    val antennaLagY: Float = 0f
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
    SQUINT,

    /** Heavy lids. Waking up, or about to stop being awake. */
    SLEEPY,

    /** Screwed shut against something — a sneeze, a shiver, a hard landing. */
    SCRUNCH,

    /** One eye shut, the other open. Knowing, not sleepy. */
    WINK,

    /** Two small hearts. Smitten — reserved for the cat. */
    HEART
}

enum class MouthStyle {
    SMILE,
    BIG_SMILE,
    OPEN,
    FLAT,
    FROWN,
    WOBBLE,

    /** A tall oval. Caught mid-breath: a gasp, a yawn, a hiccup. */
    GASP,

    /** Lopsided. The one expression that reads as knowing rather than sunny. */
    SMIRK,

    /** A grin with the tongue out. Effort, or mischief. */
    TONGUE,

    /** A small pursed circle. Whistling, or blowing on something hot. */
    WHISTLE,

    /** A wide toothy grin — the proud one. */
    GRIN
}

enum class Flourish {
    NONE,

    /** Two small z's. Asleep. */
    SLEEP,

    /** A pair of ticks. Surprise, or a sudden idea. */
    SPARK,

    /** Three dots. Thinking. */
    THINK,

    /** A small ring, drawn while dizzy. */
    SWIRL,

    /** Scuff arcs kicked up at the feet. A landing, a skid, a stumble. */
    PUFF,

    /** A curled question mark. Nothing wrong, just nothing here yet. */
    QUESTION,

    /** A magnifying glass, held up while looking something over. */
    MAGNIFY,

    /** Two flat discs on the cheeks. Drawn on the face, not above it. */
    BLUSH,

    /** Two note glyphs drifting up. Humming — glyphs, never text. */
    NOTES,

    /** A single drop beside the head. Effort, or a near miss. */
    SWEAT,

    /** One small heart above the head. */
    HEART,

    /** A leaf, drifting down past the head. */
    LEAF,

    /** Wisps rising — steam or heat, drawn at the hands. */
    STEAM
}
