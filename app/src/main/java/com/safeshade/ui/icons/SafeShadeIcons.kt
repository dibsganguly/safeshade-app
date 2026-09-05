package com.safeshade.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's own icons.
 *
 * GENERATED from the SVGs in `docs/Icons` - do not hand-edit. Run
 * `python tools/gen_icons.py` from the repo root when the set changes; those
 * SVGs are the source of truth and stay in the repo.
 *
 * **Why Kotlin `ImageVector`s and not vector XML drawables.** The obvious route
 * - drop the SVGs in `res/drawable` and load them with
 * `ImageVector.vectorResource` - cannot work here: that function is
 * `@Composable`, and this app carries icons as plain data on `BoardWay`, built
 * outside composition. Generated `ImageVector`s are ordinary values, so every
 * existing `icon: ImageVector?` parameter accepts one with no change at any
 * call site.
 *
 * Strokes are declared `Color.Black`, which is never seen: `Icon()` applies a
 * tint `ColorFilter` over the whole painter, so a glyph takes `inkMuted` or its
 * row's accent exactly as a Material icon does.
 *
 * Members rather than extension properties. Material declares its icons as
 * extensions so that a very large set stays splittable; this one does not need
 * that, and extensions would cost a separate import line per icon at every call
 * site. `by lazy` still means none is built until it is first drawn.
 *
 * **Chrome is in scope now.** An earlier revision of this file said utility
 * chrome - back arrows, chevrons, close, tick, info - stays on Material,
 * because the set then had no equivalent for any of them. It does now, and the
 * back chevron alone is the most repeated glyph in the app, so the exception is
 * retired. What is still on Material is what this set genuinely has no answer
 * for: the mode personas and device shapes in `ui/board/ModeVisuals.kt`, and a
 * handful of one-off actions. Reusing one glyph across eight distinct modes
 * would be worse than the mixture it replaced.
 *
 * [All] is the whole set in declaration order, which is what the kit gallery
 * renders. It is generated too, so a new SVG appears there without anybody
 * remembering to add it.
 */
object SafeShadeIcons {
    /** `3-g-signal.svg` */
    val Signal3G: ImageVector by lazy {
        ImageVector.Builder(
            name = "Signal3G",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.9999 5.0083C15.2086 1.6639 8.7922 1.6639 4.00012 5.0083M17.1085 7.89972C13.9703 6.03314 10.0303 6.03343 6.89239 7.90057M14.1222 10.886C12.7575 10.3713 11.2427 10.3713 9.87804 10.8862"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.5001 14.5H17.5001C16.0957 14.5 15.3934 14.5 14.889 14.8371C14.6706 14.983 14.4831 15.1705 14.3372 15.3889C14.0001 15.8933 14.0001 16.5955 14.0001 18V18.9286C14.0001 19.4598 14.0001 19.7253 14.0503 19.945C14.2212 20.694 14.8061 21.2789 15.5551 21.4499C15.7748 21.5 16.0404 21.5 16.5716 21.5C16.9699 21.5 17.1691 21.5 17.3339 21.4624C17.8956 21.3342 18.3343 20.8955 18.4625 20.3338C18.5001 20.169 18.5001 19.9698 18.5001 19.5714V19.5C18.5001 19.0341 18.5001 18.8011 18.424 18.6173C18.3225 18.3723 18.1278 18.1776 17.8828 18.0761C17.699 18 17.4661 18 17.0001 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.99988 21.5H8.24988C9.21638 21.5 9.99988 20.7165 9.99988 19.75C9.99988 18.7835 9.21638 18 8.24988 18H7L9.75491 15.7042C9.91021 15.5748 9.99999 15.3831 9.99996 15.181C9.99992 14.8049 9.69501 14.5 9.31891 14.5H5.99988"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `4-g-signal.svg` */
    val Signal4G: ImageVector by lazy {
        ImageVector.Builder(
            name = "Signal4G",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.9999 5.0083C15.2086 1.6639 8.7922 1.6639 4.00012 5.0083M17.1085 7.89972C13.9703 6.03314 10.0303 6.03343 6.89239 7.90057M14.1222 10.886C12.7575 10.3713 11.2427 10.3713 9.87804 10.8862"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.99988 18H8.99988C7.58566 18 6.87856 18 6.43922 17.5607C5.99988 17.1213 5.99988 16.4142 5.99988 15V14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 14.5V21.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.5001 14.5H17.5001C16.0957 14.5 15.3934 14.5 14.889 14.8371C14.6706 14.983 14.4831 15.1705 14.3372 15.3889C14.0001 15.8933 14.0001 16.5955 14.0001 18V18.9286C14.0001 19.4598 14.0001 19.7253 14.0503 19.945C14.2212 20.694 14.8061 21.2789 15.5551 21.4499C15.7748 21.5 16.0404 21.5 16.5716 21.5C16.9699 21.5 17.1691 21.5 17.3339 21.4624C17.8956 21.3342 18.3343 20.8955 18.4625 20.3338C18.5001 20.169 18.5001 19.9698 18.5001 19.5714V19.5C18.5001 19.0341 18.5001 18.8011 18.424 18.6173C18.3225 18.3723 18.1278 18.1776 17.8828 18.0761C17.699 18 17.4661 18 17.0001 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `5-g-signal.svg` */
    val Signal5G: ImageVector by lazy {
        ImageVector.Builder(
            name = "Signal5G",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.9999 5.0083C15.2086 1.6639 8.7922 1.6639 4.00012 5.0083M17.1085 7.89972C13.9703 6.03314 10.0303 6.03343 6.89239 7.90057M14.1222 10.886C12.7575 10.3713 11.2427 10.3713 9.87804 10.8862"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.5001 14.5H17.5001C16.0957 14.5 15.3934 14.5 14.889 14.8371C14.6706 14.983 14.4831 15.1705 14.3372 15.3889C14.0001 15.8933 14.0001 16.5955 14.0001 18V18.9286C14.0001 19.4598 14.0001 19.7253 14.0503 19.945C14.2212 20.694 14.8061 21.2789 15.5551 21.4499C15.7748 21.5 16.0404 21.5 16.5716 21.5C16.9699 21.5 17.1691 21.5 17.3339 21.4624C17.8956 21.3342 18.3343 20.8955 18.4625 20.3338C18.5001 20.169 18.5001 19.9698 18.5001 19.5714V19.5C18.5001 19.0341 18.5001 18.8011 18.424 18.6173C18.3225 18.3723 18.1278 18.1776 17.8828 18.0761C17.699 18 17.4661 18 17.0001 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.99988 21.5H7.99988C9.10445 21.5 9.99988 20.6046 9.99988 19.5C9.99988 18.3954 9.10445 17.5 7.99988 17.5H6.45442C6.20339 17.5 5.99988 17.2965 5.99988 17.0455V16C5.99988 15.5341 5.99988 15.3011 6.076 15.1173C6.17749 14.8723 6.37217 14.6776 6.61719 14.5761C6.80097 14.5 7.03394 14.5 7.49988 14.5H9.99988"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `account-recovery.svg` */
    val AccountRecovery: ImageVector by lazy {
        ImageVector.Builder(
            name = "AccountRecovery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2.5 4V5C2.5 6.41421 2.5 7.12132 2.93934 7.56066C3.37868 8 4.08579 8 5.5 8H6.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.49922 12.0005C2.49922 17.2472 6.75252 21.5005 11.9992 21.5005C17.2459 21.5005 21.4992 17.2472 21.4992 12.0005C21.4992 6.75378 17.2459 2.50049 11.9992 2.50049C8.08133 2.50049 5.04534 4.60026 3.41084 7.26031"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16 17C15.8565 15.1345 14.3644 13.6576 12.4975 13.5332L12 13.5C11.8223 13.5049 11.6567 13.5113 11.5004 13.519C9.65 13.6097 8.14209 15.1529 8 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14 9C14 10.1046 13.1046 11 12 11C10.8954 11 10 10.1046 10 9C10 7.89543 10.8954 7 12 7C13.1046 7 14 7.89543 14 9Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `activity-01.svg` */
    val Activity01: ImageVector by lazy {
        ImageVector.Builder(
            name = "Activity01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4.31802 19.682C3 18.364 3 16.2426 3 12C3 7.75736 3 5.63604 4.31802 4.31802C5.63604 3 7.75736 3 12 3C16.2426 3 18.364 3 19.682 4.31802C21 5.63604 21 7.75736 21 12C21 16.2426 21 18.364 19.682 19.682C18.364 21 16.2426 21 12 21C7.75736 21 5.63604 21 4.31802 19.682Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 14L9.79289 11.2071C10.1834 10.8166 10.8166 10.8166 11.2071 11.2071L12.7929 12.7929C13.1834 13.1834 13.8166 13.1834 14.2071 12.7929L17 10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `adaptive-mode.svg` */
    val AdaptiveMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "AdaptiveMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20.2941 8.49964C20.7487 9.57553 21 10.7582 21 11.9996C21 16.9702 16.9706 20.9996 12 20.9996C10.7586 20.9996 9.57589 20.7483 8.5 20.2937M5.29182 17.9998C3.86662 16.4075 3 14.3048 3 11.9996C3 8.51699 4.97812 5.49635 7.8721 4"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3.79165 15.8775C2.13121 18.56 1.52414 20.7282 2.39946 21.602C3.93971 23.1395 9.48663 20.087 14.7889 14.784C20.0911 9.48095 23.1408 3.93558 21.6005 2.39804C21.3306 2.12854 20.9375 2.00006 20.4442 2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5 2.9375V4.5M14.5 4.5V6.0625M14.5 4.5H13.25M14.5 4.5H15.75M17 4.5L15.9156 4.13852C15.4179 3.97263 15.0274 3.58211 14.8615 3.08443L14.5 2L14.1385 3.08443C13.9726 3.58211 13.5821 3.97263 13.0844 4.13852L12 4.5L13.0844 4.86148C13.5821 5.02737 13.9726 5.41789 14.1385 5.91557L14.5 7L14.8615 5.91557C15.0274 5.41789 15.4179 5.02737 15.9156 4.86148L17 4.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `ai-search-02.svg` */
    val AiSearch02: ImageVector by lazy {
        ImageVector.Builder(
            name = "AiSearch02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.0001 16.5L20 20.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 11.5C18 15.366 14.866 18.5 11 18.5C7.13401 18.5 4 15.366 4 11.5C4 7.63404 7.13401 4.50003 11 4.50003"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 3.50003L15.7579 4.19706C16.0961 5.11105 16.2652 5.56805 16.5986 5.90142C16.932 6.2348 17.389 6.4039 18.303 6.74211L19 7.00003L18.303 7.25795C17.389 7.59616 16.932 7.76527 16.5986 8.09864C16.2652 8.43201 16.0961 8.88901 15.7579 9.803L15.5 10.5L15.2421 9.803C14.9039 8.88901 14.7348 8.43201 14.4014 8.09864C14.068 7.76527 13.611 7.59616 12.697 7.25795L12 7.00003L12.697 6.74211C13.611 6.4039 14.068 6.2348 14.4014 5.90142C14.7348 5.56805 14.9039 5.11105 15.2421 4.19706L15.5 3.50003Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `alert-02.svg` */
    val Alert02: ImageVector by lazy {
        ImageVector.Builder(
            name = "Alert02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.9248 21H10.0752C5.44476 21 3.12955 21 2.27636 19.4939C1.42317 17.9879 2.60736 15.9914 4.97574 11.9985L6.90057 8.75333C9.17559 4.91778 10.3131 3 12 3C13.6869 3 14.8244 4.91777 17.0994 8.75332L19.0243 11.9985C21.3926 15.9914 22.5768 17.9879 21.7236 19.4939C20.8704 21 18.5552 21 13.9248 21Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 9V13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 16.75H12M12.25 16.75C12.25 16.8881 12.1381 17 12 17C11.8619 17 11.75 16.8881 11.75 16.75C11.75 16.6119 11.8619 16.5 12 16.5C12.1381 16.5 12.25 16.6119 12.25 16.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `all-bookmark.svg` */
    val AllBookmark: ImageVector by lazy {
        ImageVector.Builder(
            name = "AllBookmark",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3 17.9808V12.7075C3 9.07416 3 7.25748 4.09835 6.12874C5.1967 5 6.96447 5 10.5 5C14.0355 5 15.8033 5 16.9017 6.12874C18 7.25748 18 9.07416 18 12.7075V17.9808C18 20.2867 18 21.4396 17.2755 21.8523C15.8724 22.6514 13.2405 19.9852 11.9906 19.1824C11.2657 18.7168 10.9033 18.484 10.5 18.484C10.0967 18.484 9.73425 18.7168 9.00938 19.1824C7.7595 19.9852 5.12763 22.6514 3.72454 21.8523C3 21.4396 3 20.2867 3 17.9808Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 2H11C15.714 2 18.0711 2 19.5355 3.46447C21 4.92893 21 7.28595 21 12V18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `arrow-down-01.svg` */
    val ArrowDown01: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowDown01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18 9.00005C18 9.00005 13.5811 15 12 15C10.4188 15 6 9 6 9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `arrow-left-01.svg` */
    val ArrowLeft01: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowLeft01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15 6C15 6 9.00001 10.4189 9 12C8.99999 13.5812 15 18 15 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `arrow-right-01.svg` */
    val ArrowRight01: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowRight01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.00005 6C9.00005 6 15 10.4189 15 12C15 13.5812 9 18 9 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `arrow-up-01.svg` */
    val ArrowUp01: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowUp01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.9998 15C17.9998 15 13.5809 9.00001 11.9998 9C10.4187 8.99999 5.99985 15 5.99985 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `at-sign.svg` */
    val AtSign: ImageVector by lazy {
        ImageVector.Builder(
            name = "AtSign",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 16C14.2091 16 16 14.2091 16 12C16 9.79086 14.2091 8 12 8C9.79086 8 8 9.79086 8 12C8 14.2091 9.79086 16 12 16Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 20.0007C16.3287 21.2561 14.2512 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12V13C22 14.6569 20.6569 16 19 16C17.3431 16 16 14.6569 16 13V8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `badge-alert.svg` */
    val BadgeAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "BadgeAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M14.3942 3.00083L14.1481 2.79115C12.9103 1.73628 11.0897 1.73628 9.85189 2.79115L9.60584 3.00083C8.96518 3.54679 8.16862 3.87674 7.32956 3.9437L7.00731 3.96941C5.38613 4.09878 4.09878 5.38613 3.96941 7.00731L3.9437 7.32956C3.87674 8.16862 3.54679 8.96518 3.00083 9.60584L2.79115 9.85189C1.73628 11.0897 1.73628 12.9103 2.79115 14.1481L3.00083 14.3942C3.54679 15.0348 3.87674 15.8314 3.9437 16.6704L3.96941 16.9927C4.09878 18.6139 5.38613 19.9012 7.00731 20.0306L7.32956 20.0563C8.16862 20.1233 8.96518 20.4532 9.60584 20.9992L9.85188 21.2089C11.0897 22.2637 12.9103 22.2637 14.1481 21.2089L14.3942 20.9992C15.0348 20.4532 15.8314 20.1233 16.6704 20.0563L16.9927 20.0306C18.6139 19.9012 19.9012 18.6139 20.0306 16.9927L20.0563 16.6704C20.1233 15.8314 20.4532 15.0348 20.9992 14.3942L21.2089 14.1481C22.2637 12.9103 22.2637 11.0897 21.2089 9.85188L20.9992 9.60584C20.4532 8.96518 20.1233 8.16862 20.0563 7.32956L20.0306 7.00731C19.9012 5.38613 18.6139 4.09878 16.9927 3.96941L16.6704 3.9437C15.8314 3.87674 15.0348 3.54679 14.3942 3.00083Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 8V12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 15.75H12M12.25 15.75C12.25 15.8881 12.1381 16 12 16C11.8619 16 11.75 15.8881 11.75 15.75C11.75 15.6119 11.8619 15.5 12 15.5C12.1381 15.5 12.25 15.6119 12.25 15.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `ban.svg` */
    val Ban: ImageVector by lazy {
        ImageVector.Builder(
            name = "Ban",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 21C16.9706 21 21 16.9706 21 12C21 7.02944 16.9706 3 12 3C7.02944 3 3 7.02944 3 12C3 16.9706 7.02944 21 12 21Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6 6L18 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `battery-charging-02.svg` */
    val BatteryCharging02: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryCharging02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8 6C5.17157 6 3.75736 6 2.87868 6.87868C2 7.75736 2 9.17157 2 12C2 14.8284 2 16.2426 2.87868 17.1213C3.44798 17.6906 4.24209 17.8911 5.5 17.9616M12 17.9827L13 18C15.8284 18 17.2426 18 18.1213 17.1213C19 16.2426 19 14.8284 19 12C19 9.17157 19 7.75736 18.1213 6.87868C17.414 6.17137 16.3597 6.03342 14.5 6.00652"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M11.5623 6L8.59169 10.4367C8.13166 11.1237 7.90164 11.4673 8.03989 11.7336C8.17814 12 8.58645 12 9.40307 12H10.5969C11.4136 12 11.8219 12 11.9601 12.2664C12.0984 12.5327 11.8683 12.8763 11.4083 13.5633L8.43769 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-eco-charging.svg` */
    val BatteryEcoCharging: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryEcoCharging",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8 19H13C15.8284 19 17.2426 19 18.1213 18.1213C19 17.2426 19 15.8284 19 13C19 11.1366 19 9.887 18.7488 8.99997M5.5 7.0383C4.24209 7.10888 3.44798 7.30933 2.87868 7.87863C2 8.75731 2 10.1715 2 13C2 15.2437 2 16.5975 2.43866 17.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 10.5L20.0272 10.6712C20.7085 10.7847 21.0491 10.8415 21.3076 11.0066C21.5618 11.169 21.7612 11.4044 21.8796 11.6818C22 11.9639 22 12.3093 22 13C22 13.6906 22 14.036 21.8796 14.3181C21.7612 14.5955 21.5618 14.8309 21.3076 14.9933C21.0491 15.1584 20.7085 15.2152 20.0272 15.3287L19 15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5.03319 20C4.54502 14.6 9.00032 13.5 11.0002 11M11.0339 15.8353C13.5787 15.1243 15.2108 13.4224 15.8162 10.8483C16.502 7.93305 15.2725 3.96327 12.1026 4.00026C12.1026 4.00026 12.4253 5.25961 12.143 5.8764C11.1022 8.15057 7.50025 7.99259 6.3325 10.8769C5.63711 12.4908 6.05765 14.2938 7.36616 15.3279C8.18838 15.9778 9.81192 16.1767 11.0339 15.8353Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `battery-full.svg` */
    val BatteryFull: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryFull",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-low.svg` */
    val BatteryLow: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryLow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-medium-01.svg` */
    val BatteryMedium01: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryMedium01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-medium-02.svg` */
    val BatteryMedium02: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryMedium02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12C2 9.17157 2 7.75736 2.87868 6.87868C3.75736 6 5.17157 6 8 6H13C15.8284 6 17.2426 6 18.1213 6.87868C19 7.75736 19 9.17157 19 12C19 14.8284 19 16.2426 18.1213 17.1213C17.2426 18 15.8284 18 13 18H8C5.17157 18 3.75736 18 2.87868 17.1213C2 16.2426 2 14.8284 2 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.5L20.0272 9.6712C20.7085 9.78475 21.0491 9.84152 21.3076 10.0067C21.5618 10.1691 21.7612 10.4044 21.8796 10.6819C22 10.964 22 11.3093 22 12C22 12.6907 22 13.036 21.8796 13.3181C21.7612 13.5956 21.5618 13.8309 21.3076 13.9933C21.0491 14.1585 20.7085 14.2153 20.0272 14.3288L19 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 10V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-warning.svg` */
    val BatteryWarning: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryWarning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 6.00171C4.82497 6.01382 3.64706 6.11027 2.87868 6.87865C2 7.75733 2 9.17154 2 12C2 14.8284 2 16.2426 2.87868 17.1213C3.64706 17.8897 4.82497 17.9861 7 17.9982M14 17.9982C16.175 17.9861 17.3529 17.8897 18.1213 17.1213C19 16.2426 19 14.8284 19 12C19 9.17154 19 7.75733 18.1213 6.87865C17.3529 6.11027 16.175 6.01382 14 6.00171"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19 9.49976L20.0272 9.67096C20.7085 9.7845 21.0491 9.84128 21.3076 10.0064C21.5618 10.1688 21.7612 10.4042 21.8796 10.6816C22 10.9637 22 11.3091 22 11.9998C22 12.6904 22 13.0358 21.8796 13.3179C21.7612 13.5953 21.5618 13.8307 21.3076 13.9931C21.0491 14.1582 20.7085 14.215 20.0272 14.3286L19 14.4998"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 7.99976V11.9998"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.625 15.7498H10.5M10.75 15.7498C10.75 15.8878 10.6381 15.9998 10.5 15.9998C10.3619 15.9998 10.25 15.8878 10.25 15.7498C10.25 15.6117 10.3619 15.4998 10.5 15.4998C10.6381 15.4998 10.75 15.6117 10.75 15.7498Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `bookmark-02.svg` */
    val Bookmark02: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bookmark02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4 17.9808V9.70753C4 6.07416 4 4.25748 5.17157 3.12874C6.34315 2 8.22876 2 12 2C15.7712 2 17.6569 2 18.8284 3.12874C20 4.25748 20 6.07416 20 9.70753V17.9808C20 20.2867 20 21.4396 19.2272 21.8523C17.7305 22.6514 14.9232 19.9852 13.59 19.1824C12.8168 18.7168 12.4302 18.484 12 18.484C11.5698 18.484 11.1832 18.7168 10.41 19.1824C9.0768 19.9852 6.26947 22.6514 4.77285 21.8523C4 21.4396 4 20.2867 4 17.9808Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `bookmark-block-02.svg` */
    val BookmarkBlock02: ImageVector by lazy {
        ImageVector.Builder(
            name = "BookmarkBlock02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19 12.5V17.9808C19 20.2867 19 21.4396 18.2272 21.8523C16.7305 22.6515 13.9232 19.9852 12.59 19.1824C11.8168 18.7168 11.4302 18.484 11 18.484C10.5698 18.484 10.1832 18.7168 9.41 19.1824C8.0768 19.9852 5.26947 22.6515 3.77285 21.8523C3 21.4396 3 20.2867 3 17.9808V9.70753C3 6.07417 3 4.25749 4.17157 3.12875C5.23467 2.10452 6.8857 2.00968 10 2.0009"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.2 3.2L18.8 8.8M20 6C20 3.79086 18.2091 2 16 2C13.7909 2 12 3.79086 12 6C12 8.20914 13.7909 10 16 10C18.2091 10 20 8.20914 20 6Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `call-after-a-fall.svg` */
    val CallAfterAFall: ImageVector by lazy {
        ImageVector.Builder(
            name = "CallAfterAFall",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.1585 5.71223L8.75584 4.80625C8.49256 4.21388 8.36092 3.91768 8.16405 3.69101C7.91732 3.40694 7.59571 3.19794 7.23592 3.08785C6.94883 3 6.6247 3 5.97645 3C5.02815 3 4.554 3 4.15597 3.18229C3.68711 3.39702 3.26368 3.86328 3.09497 4.3506C2.95175 4.76429 2.99278 5.18943 3.07482 6.0397C3.94815 15.0902 8.91006 20.0521 17.9605 20.9254C18.8108 21.0075 19.236 21.0485 19.6496 20.9053C20.137 20.7366 20.6032 20.3131 20.818 19.8443C21.0002 19.4462 21.0002 18.9721 21.0002 18.0238C21.0002 17.3755 21.0002 17.0514 20.9124 16.7643C20.8023 16.4045 20.5933 16.0829 20.3092 15.8362C20.0826 15.6393 19.7864 15.5077 19.194 15.2444L18.288 14.8417C17.6465 14.5566 17.3257 14.4141 16.9998 14.3831C16.6878 14.3534 16.3733 14.3972 16.0813 14.5109C15.7762 14.6297 15.5066 14.8544 14.9672 15.3038C14.4304 15.7512 14.162 15.9749 13.834 16.0947C13.5432 16.2009 13.1588 16.2403 12.8526 16.1951C12.5071 16.1442 12.2426 16.0029 11.7135 15.7201C10.0675 14.8405 9.15977 13.9328 8.28011 12.2867C7.99738 11.7577 7.85602 11.4931 7.80511 11.1477C7.75998 10.8414 7.79932 10.457 7.90554 10.1663C8.02536 9.83828 8.24905 9.56986 8.69643 9.033C9.14586 8.49368 9.37058 8.22402 9.48939 7.91891C9.60309 7.62694 9.64686 7.3124 9.61719 7.00048C9.58618 6.67452 9.44362 6.35376 9.1585 5.71223Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M17.4695 3.02506C17.4764 2.99165 17.5241 2.99165 17.5309 3.02506C17.8854 4.75942 19.2408 6.11481 20.9752 6.4693C21.0086 6.47613 21.0086 6.52387 20.9752 6.5307C19.2408 6.88519 17.8854 8.24058 17.5309 9.97494C17.5241 10.0084 17.4764 10.0084 17.4695 9.97494C17.1151 8.24058 15.7597 6.88519 14.0253 6.5307C13.9919 6.52387 13.9919 6.47613 14.0253 6.4693C15.7597 6.11481 17.1151 4.75942 17.4695 3.02506Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `chat-favourite-01.svg` */
    val ChatFavourite01: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChatFavourite01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.9924 9.99917C21.9974 10.3286 22 10.6621 22 10.9992C22 11.77 21.9865 12.5224 21.9609 13.2489C21.8772 15.6223 21.8353 16.8091 20.8699 17.7817C19.9046 18.7544 18.6843 18.8066 16.2437 18.911C15.5098 18.9424 14.7498 18.9659 13.9693 18.9807C13.2282 18.9947 12.8576 19.0017 12.532 19.1257C12.2064 19.2497 11.9325 19.4846 11.3845 19.9545L9.20503 21.8234C9.07273 21.9368 8.90419 21.9992 8.72991 21.9992C8.32679 21.9992 8 21.6724 8 21.2693V18.9211C7.91842 18.9178 7.83715 18.9144 7.75619 18.911C5.31569 18.8066 4.09545 18.7544 3.13007 17.7817C2.16469 16.8091 2.12282 15.6223 2.03909 13.2489C2.01346 12.5224 2 11.77 2 10.9992C2 10.2284 2.01346 9.47596 2.03909 8.74947C2.12282 6.376 2.16469 5.18926 3.13007 4.21662C4.09545 3.24398 5.3157 3.19177 7.7562 3.08736C8.7908 3.04309 9.8772 3.01458 11 3.00391"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 9C18 9 14 6.52941 14 4.13889C14 2.95761 14.8421 2 16 2C16.6 2 17.2 2.20588 18 3.02941C18.8 2.20588 19.4 2 20 2C21.1579 2 22 2.95761 22 4.13889C22 6.52941 18 9 18 9Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.1257 11H12.0007M8.125 11H8M12.2507 11C12.2507 11.1381 12.1388 11.25 12.0007 11.25C11.8627 11.25 11.7507 11.1381 11.7507 11C11.7507 10.8619 11.8627 10.75 12.0007 10.75C12.1388 10.75 12.2507 10.8619 12.2507 11ZM8.25 11C8.25 11.1381 8.13807 11.25 8 11.25C7.86193 11.25 7.75 11.1381 7.75 11C7.75 10.8619 7.86193 10.75 8 10.75C8.13807 10.75 8.25 10.8619 8.25 11Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `check-in.svg` */
    val CheckIn: ImageVector by lazy {
        ImageVector.Builder(
            name = "CheckIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                scaleX = 0.9f,
                scaleY = 0.9f,
                translationX = 1.2f,
                translationY = 1.2f
            )
            addPath(
                pathData = addPathNodes(
                    "M22 11.9879C22 16.9645 19.1328 20.9988 12 20.9988C10.4991 20.9988 9.20689 20.852 8.0984 20.5797C7.50688 20.4345 6.88252 20.6134 6.39239 20.9754C5.70211 21.4852 4.61547 22 3 22C3.99253 21.3325 4.63281 19.497 3.37161 17.4946C2.37034 15.9718 2 14.0617 2 11.9879C2 7.22912 3.95026 3.33192 11 3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.1248 12H11.9998M8.125 12H8M12.2498 12C12.2498 12.1381 12.1379 12.25 11.9998 12.25C11.8618 12.25 11.7498 12.1381 11.7498 12C11.7498 11.8619 11.8618 11.75 11.9998 11.75C12.1379 11.75 12.2498 11.8619 12.2498 12ZM8.25 12C8.25 12.1381 8.13807 12.25 8 12.25C7.86193 12.25 7.75 12.1381 7.75 12C7.75 11.8619 7.86193 11.75 8 11.75C8.13807 11.75 8.25 11.8619 8.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 6L19 5M22 6C22 8.20914 20.2091 10 18 10C15.7909 10 14 8.20914 14 6C14 3.79086 15.7909 2 18 2C20.2091 2 22 3.79086 22 6Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            clearGroup()
        }.build()
    }

    /** `circle-dashed.svg` */
    val CircleDashed: ImageVector by lazy {
        ImageVector.Builder(
            name = "CircleDashed",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.50293 21.6846C10.302 21.8902 11.1397 21.9996 12.0029 21.9996C12.8662 21.9996 13.7039 21.8902 14.5029 21.6846"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5029 2.31504C13.7039 2.10938 12.8662 2 12.0029 2C11.1397 2 10.302 2.10938 9.50293 2.31504"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.37109 14.6777C2.59251 15.4726 2.91663 16.2527 3.34826 17.0003C3.77988 17.7479 4.29346 18.4187 4.87109 19.0079"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21.6451 9.32261C21.4237 8.52778 21.0996 7.74762 20.668 7.00002C20.2363 6.25243 19.7228 5.58165 19.1451 4.99248"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21.6436 14.6777C21.4221 15.4726 21.098 16.2527 20.6664 17.0003C20.2348 17.7479 19.7212 18.4187 19.1436 19.0079"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.36952 9.32261C2.59093 8.52778 2.91506 7.74762 3.34668 7.00002C3.7783 6.25243 4.29188 5.58165 4.86952 4.99248"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `circle.svg` */
    val Circle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Circle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cloud-loading.svg` */
    val CloudLoading: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudLoading",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.4776 9.00005C17.485 9.00002 17.4925 9 17.5 9C19.9853 9 22 11.0147 22 13.5C22 15.9853 19.9853 18 17.5 18H7C4.23858 18 2 15.7614 2 13C2 10.4003 3.98398 8.26407 6.52042 8.0227M17.4776 9.00005C17.4924 8.83536 17.5 8.66856 17.5 8.5C17.5 5.46243 15.0376 3 12 3C9.12324 3 6.76233 5.20862 6.52042 8.0227M17.4776 9.00005C17.3753 10.1345 16.9286 11.1696 16.2428 12M6.52042 8.0227C6.67826 8.00768 6.83823 8 7 8C8.12582 8 9.16474 8.37209 10.0005 9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6 21H8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M11 21H13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M16 21H18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `cloud-saving-done-01.svg` */
    val CloudSavingDone01: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudSavingDone01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.5 18C19.9853 18 22 15.9853 22 13.5C22 11.0147 19.9853 9 17.5 9C17.4925 9 17.485 9.00002 17.4776 9.00005M17.4776 9.00005C17.4924 8.83536 17.5 8.66856 17.5 8.5C17.5 5.46243 15.0376 3 12 3C9.12324 3 6.76233 5.20862 6.52042 8.0227M17.4776 9.00005C17.4131 9.71494 17.2119 10.3904 16.9003 11M6.52042 8.0227C3.98398 8.26407 2 10.4003 2 13C2 15.419 3.71776 17.4367 6 17.9M6.52042 8.0227C6.67826 8.00768 6.83823 8 7 8C8.12582 8 9.16474 8.37209 10.0005 9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 19C9 19 10 19 11 21C11 21 14.1765 16 17 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cloud-saving-done-02.svg` */
    val CloudSavingDone02: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudSavingDone02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.6563 16.8786C10.2824 16.7003 9.83477 16.8588 9.65642 17.2326C9.47807 17.6065 9.63655 18.0541 10.0104 18.2325L10.6563 16.8786ZM11.4444 18.6667L10.7983 19.0474C10.9389 19.2861 11.1998 19.4279 11.4766 19.416C11.7534 19.4041 12.0012 19.2405 12.1209 18.9906L11.4444 18.6667ZM14.0222 16.5493C14.3869 16.3529 14.5234 15.8981 14.3271 15.5334C14.1307 15.1687 13.6759 15.0322 13.3112 15.2285L14.0222 16.5493ZM16.25 17C16.25 19.3472 14.3472 21.25 12 21.25V22.75C15.1756 22.75 17.75 20.1756 17.75 17H16.25ZM12 21.25C9.65279 21.25 7.75 19.3472 7.75 17H6.25C6.25 20.1756 8.82436 22.75 12 22.75V21.25ZM7.75 17C7.75 14.6528 9.65279 12.75 12 12.75V11.25C8.82436 11.25 6.25 13.8244 6.25 17H7.75ZM12 12.75C14.3472 12.75 16.25 14.6528 16.25 17H17.75C17.75 13.8244 15.1756 11.25 12 11.25V12.75ZM10.3333 17.5556C10.0104 18.2325 10.0102 18.2324 10.0099 18.2322C10.0098 18.2322 10.0096 18.2321 10.0094 18.232C10.0091 18.2319 10.0088 18.2317 10.0085 18.2316C10.008 18.2313 10.0074 18.231 10.0069 18.2308C10.0059 18.2303 10.005 18.2299 10.0043 18.2295C10.0028 18.2288 10.0019 18.2283 10.0015 18.2281C10.0008 18.2278 10.0022 18.2285 10.0056 18.2303C10.0125 18.234 10.0271 18.2421 10.0482 18.255C10.0904 18.2809 10.1574 18.3254 10.2383 18.3909C10.4003 18.5221 10.6137 18.7341 10.7983 19.0474L12.0906 18.2859C11.799 17.791 11.4568 17.4475 11.1823 17.2252C11.045 17.1139 10.9235 17.0322 10.8318 16.976C10.7859 16.9479 10.7472 16.926 10.7174 16.91C10.7026 16.902 10.6899 16.8954 10.6796 16.8902C10.6745 16.8876 10.67 16.8853 10.6661 16.8834C10.6641 16.8824 10.6623 16.8816 10.6607 16.8808C10.6599 16.8804 10.6591 16.88 10.6584 16.8796C10.658 16.8795 10.6576 16.8793 10.6573 16.8791C10.6571 16.879 10.6569 16.8789 10.6568 16.8789C10.6565 16.8788 10.6563 16.8786 10.3333 17.5556ZM11.4444 18.6667C12.1209 18.9906 12.1208 18.9907 12.1208 18.9908C12.1208 18.9909 12.1207 18.9909 12.1207 18.991C12.1207 18.9911 12.1206 18.9911 12.1206 18.9912C12.1206 18.9912 12.1206 18.9911 12.1207 18.9909C12.1209 18.9905 12.1214 18.9896 12.1221 18.9882C12.1234 18.9854 12.1257 18.9807 12.129 18.9741C12.1354 18.9609 12.1456 18.9405 12.1593 18.9136C12.1866 18.8598 12.2278 18.7806 12.2811 18.6827C12.3883 18.4862 12.5425 18.2186 12.7307 17.9338C13.1295 17.3302 13.5986 16.7773 14.0222 16.5493L13.3112 15.2285C12.5331 15.6473 11.8911 16.4834 11.4792 17.1069C11.262 17.4357 11.086 17.7413 10.9642 17.9645C10.9032 18.0765 10.8552 18.1688 10.822 18.234C10.8054 18.2667 10.7925 18.2927 10.7834 18.3111C10.7789 18.3203 10.7753 18.3276 10.7728 18.3329C10.7715 18.3355 10.7704 18.3377 10.7696 18.3393C10.7692 18.3401 10.7689 18.3408 10.7686 18.3414C10.7685 18.3417 10.7684 18.3419 10.7683 18.3421C10.7682 18.3423 10.7682 18.3424 10.7681 18.3424C10.7681 18.3426 10.768 18.3427 11.4444 18.6667Z"
                ),
                fill = SolidColor(Color.Black)
            )
            addPath(
                pathData = addPathNodes(
                    "M17.5 8V7.25L17.4982 7.25L17.5 8ZM6.52042 7.0227L6.59147 7.76933H6.59147L6.52042 7.0227ZM9.54998 8.59962C9.88114 8.84843 10.3513 8.78167 10.6001 8.45051C10.8489 8.11935 10.7822 7.64919 10.451 7.40038L9.54998 8.59962ZM19.555 15.6374C19.2087 15.8648 19.1123 16.3297 19.3397 16.676C19.567 17.0222 20.032 17.1186 20.3783 16.8913L19.555 15.6374ZM3.61477 16.6483C3.94943 16.8924 4.41859 16.819 4.66268 16.4843C4.90676 16.1497 4.83334 15.6805 4.49868 15.4364L3.61477 16.6483ZM16.2325 9.65862C16.0439 10.0274 16.1901 10.4793 16.5589 10.6678C16.9277 10.8563 17.3796 10.7102 17.5681 10.3414L16.2325 9.65862ZM12 2.75C14.6234 2.75 16.75 4.87665 16.75 7.5H18.25C18.25 4.04822 15.4518 1.25 12 1.25V2.75ZM17.5 8.75C19.5711 8.75 21.25 10.4289 21.25 12.5H22.75C22.75 9.60051 20.3995 7.25 17.5 7.25V8.75ZM17.4794 8.75005L17.5018 8.75L17.4982 7.25L17.4757 7.25006L17.4794 8.75005ZM16.75 7.5C16.75 7.64606 16.7434 7.79039 16.7306 7.93274L18.2245 8.06737C18.2414 7.88033 18.25 7.69107 18.25 7.5H16.75ZM2.75 12C2.75 9.79073 4.4363 7.97442 6.59147 7.76933L6.44937 6.27608C3.53166 6.55373 1.25 9.00996 1.25 12H2.75ZM6.59147 7.76933C6.72573 7.75655 6.86199 7.75 7 7.75V6.25C6.81447 6.25 6.63079 6.25881 6.44937 6.27608L6.59147 7.76933ZM7.26767 7.08694C7.47652 4.65738 9.51591 2.75 12 2.75V1.25C8.73056 1.25 6.04814 3.75986 5.77318 6.95847L7.26767 7.08694ZM7 7.75C7.95781 7.75 8.83967 8.06595 9.54998 8.59962L10.451 7.40038C9.48982 6.67823 8.29384 6.25 7 6.25V7.75ZM21.25 12.5C21.25 13.8114 20.5775 14.9661 19.555 15.6374L20.3783 16.8913C21.8052 15.9544 22.75 14.3377 22.75 12.5H21.25ZM4.49868 15.4364C3.43727 14.6623 2.75 13.4114 2.75 12H1.25C1.25 13.9109 2.18287 15.6039 3.61477 16.6483L4.49868 15.4364ZM16.7306 7.93273C16.675 8.55008 16.5013 9.13268 16.2325 9.65862L17.5681 10.3414C17.9225 9.64808 18.1513 8.8798 18.2245 8.06738L16.7306 7.93273Z"
                ),
                fill = SolidColor(Color.Black)
            )
        }.build()
    }

    /** `connect-to-the-device.svg` */
    val ConnectToTheDevice: ImageVector by lazy {
        ImageVector.Builder(
            name = "ConnectToTheDevice",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18 21C18 19.6148 17.8122 18.2734 17.4608 17M3 6C4.38521 6 5.72656 6.18777 7 6.53923"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 21C11 16.5817 7.41828 13 3 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3.75 20.5H3.5M4 20.5C4 20.7761 3.77614 21 3.5 21C3.22386 21 3 20.7761 3 20.5C3 20.2239 3.22386 20 3.5 20C3.77614 20 4 20.2239 4 20.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.6563 7.71197C14.2824 7.53362 13.8348 7.69211 13.6564 8.06596C13.4781 8.43981 13.6366 8.88746 14.0104 9.06581L14.3333 8.38889L14.6563 7.71197ZM15.4444 9.5L14.7983 9.88072C14.9389 10.1195 15.1998 10.2612 15.4766 10.2493C15.7534 10.2374 16.0012 10.0739 16.1209 9.82397L15.4444 9.5ZM18.0222 7.38261C18.3869 7.18627 18.5234 6.73144 18.3271 6.36671C18.1307 6.00199 17.6759 5.86549 17.3112 6.06183L17.6667 6.72222L18.0222 7.38261ZM21 8H20.25C20.25 10.3472 18.3472 12.25 16 12.25V13V13.75C19.1756 13.75 21.75 11.1756 21.75 8H21ZM16 13V12.25C13.6528 12.25 11.75 10.3472 11.75 8H11H10.25C10.25 11.1756 12.8244 13.75 16 13.75V13ZM11 8H11.75C11.75 5.65279 13.6528 3.75 16 3.75V3V2.25C12.8244 2.25 10.25 4.82436 10.25 8H11ZM16 3V3.75C18.3472 3.75 20.25 5.65279 20.25 8H21H21.75C21.75 4.82436 19.1756 2.25 16 2.25V3ZM14.3333 8.38889C14.0104 9.06581 14.0102 9.06569 14.0099 9.06557C14.0098 9.06554 14.0096 9.06542 14.0094 9.06535C14.0091 9.0652 14.0088 9.06505 14.0085 9.06491C14.008 9.06463 14.0074 9.06437 14.0069 9.06412C14.0059 9.06363 14.005 9.0632 14.0043 9.06284C14.0028 9.06211 14.0019 9.06166 14.0015 9.06147C14.0008 9.06111 14.0022 9.06182 14.0056 9.06366C14.0125 9.06735 14.0271 9.07547 14.0482 9.08837C14.0904 9.11425 14.1574 9.15871 14.2383 9.22421C14.4003 9.35539 14.6137 9.56744 14.7983 9.88072L15.4444 9.5L16.0906 9.11928C15.799 8.62434 15.4568 8.28084 15.1823 8.05852C15.045 7.94727 14.9235 7.86549 14.8318 7.80932C14.7859 7.78119 14.7472 7.75934 14.7174 7.74331C14.7026 7.73528 14.6899 7.7287 14.6796 7.7235C14.6745 7.7209 14.67 7.71865 14.6661 7.71673C14.6641 7.71577 14.6623 7.71489 14.6607 7.7141C14.6599 7.7137 14.6591 7.71333 14.6584 7.71297C14.658 7.7128 14.6576 7.71263 14.6573 7.71246C14.6571 7.71237 14.6569 7.71225 14.6568 7.71221C14.6565 7.71209 14.6563 7.71197 14.3333 8.38889ZM15.4444 9.5C16.1209 9.82397 16.1208 9.82408 16.1208 9.82417C16.1208 9.82419 16.1207 9.82428 16.1207 9.82433C16.1207 9.82441 16.1206 9.82447 16.1206 9.82449C16.1206 9.82453 16.1206 9.82445 16.1207 9.82424C16.1209 9.82383 16.1214 9.82292 16.1221 9.82152C16.1234 9.81872 16.1257 9.81399 16.129 9.80742C16.1354 9.79428 16.1456 9.77384 16.1593 9.74697C16.1866 9.69318 16.2278 9.61389 16.2811 9.51602C16.3883 9.31953 16.5425 9.05194 16.7307 8.76709C17.1295 8.16351 17.5986 7.61065 18.0222 7.38261L17.6667 6.72222L17.3112 6.06183C16.5331 6.48067 15.8911 7.3167 15.4792 7.94028C15.262 8.26902 15.086 8.57458 14.9642 8.79786C14.9032 8.90986 14.8552 9.00211 14.822 9.06738C14.8054 9.10004 14.7925 9.12601 14.7834 9.1444C14.7789 9.1536 14.7753 9.16091 14.7728 9.16621C14.7715 9.16887 14.7704 9.17102 14.7696 9.17266C14.7692 9.17348 14.7689 9.17417 14.7686 9.17474C14.7685 9.17502 14.7684 9.17526 14.7683 9.17548C14.7682 9.17559 14.7682 9.17572 14.7681 9.17578C14.7681 9.17591 14.768 9.17603 15.4444 9.5Z"
                ),
                fill = SolidColor(Color.Black)
            )
        }.build()
    }

    /** `cross.svg` */
    val Cross: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cross",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M14.5 7.5V5.5C14.5 5.03534 14.5 4.80302 14.4616 4.60982C14.3038 3.81644 13.6836 3.19624 12.8902 3.03843C12.697 3 12.4647 3 12 3C11.5353 3 11.303 3 11.1098 3.03843C10.3164 3.19624 9.69624 3.81644 9.53843 4.60982C9.5 4.80302 9.5 5.03534 9.5 5.5V7.5C9.5 8.44281 9.5 8.91421 9.20711 9.20711C8.91421 9.5 8.44281 9.5 7.5 9.5H5.5C5.03535 9.5 4.80302 9.5 4.60982 9.53843C3.81644 9.69624 3.19624 10.3164 3.03843 11.1098C3 11.303 3 11.5353 3 12C3 12.4647 3 12.697 3.03843 12.8902C3.19624 13.6836 3.81644 14.3038 4.60982 14.4616C4.80302 14.5 5.03534 14.5 5.5 14.5H7.49998C8.4428 14.5 8.91421 14.5 9.2071 14.7929C9.49999 15.0858 9.49999 15.5572 9.49998 16.5L9.49995 18.5C9.49995 18.9646 9.49995 19.197 9.53838 19.3902C9.69619 20.1836 10.3164 20.8037 11.1097 20.9616C11.303 21 11.5353 21 12 21C12.4647 21 12.697 21 12.8902 20.9616C13.6836 20.8038 14.3038 20.1836 14.4616 19.3902C14.5 19.197 14.5 18.9647 14.5 18.5V16.5C14.5 15.5572 14.5 15.0858 14.7929 14.7929C15.0858 14.5 15.5572 14.5 16.5 14.5H18.5C18.9647 14.5 19.197 14.5 19.3902 14.4616C20.1836 14.3038 20.8038 13.6836 20.9616 12.8902C21 12.697 21 12.4647 21 12C21 11.5353 21 11.303 20.9616 11.1098C20.8038 10.3164 20.1836 9.69624 19.3902 9.53843C19.197 9.5 18.9647 9.5 18.5 9.5H16.5C15.5572 9.5 15.0858 9.5 14.7929 9.20711C14.5 8.91421 14.5 8.44281 14.5 7.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cyber.svg` */
    val Cyber: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cyber",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6.19351 11.3965L12.192 3.31186C12.6611 2.67957 13.5405 3.07311 13.5405 3.91536V10.1729C13.5405 10.6775 13.8853 11.0865 14.3107 11.0865H17.2283C17.891 11.0865 18.2443 12.0134 17.8065 12.6035L11.808 20.6881C11.3389 21.3204 10.4595 20.9269 10.4595 20.0846V13.8271C10.4595 13.3225 10.1147 12.9135 9.68931 12.9135H6.77173C6.10895 12.9135 5.75566 11.9866 6.19351 11.3965Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `daily-reminder.svg` */
    val DailyReminder: ImageVector by lazy {
        ImageVector.Builder(
            name = "DailyReminder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.9 10C20.9734 5.43552 16.9379 2 12.1 2C6.57717 2 2.10002 6.47715 2.10002 12C2.10002 17.5228 6.57717 22 12.1 22C13.1452 22 14.153 21.8396 15.1 21.5422"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.1 8V12L14.1 14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.1 18V14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.225 21.75H20.1M20.35 21.75C20.35 21.8881 20.2381 22 20.1 22C19.962 22 19.85 21.8881 19.85 21.75C19.85 21.6119 19.962 21.5 20.1 21.5C20.2381 21.5 20.35 21.6119 20.35 21.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `dashboard-circle-edit.svg` */
    val DashboardCircleEdit: ImageVector by lazy {
        ImageVector.Builder(
            name = "DashboardCircleEdit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.5 6.75C10.5 4.67893 8.82107 3 6.75 3C4.67893 3 3 4.67893 3 6.75C3 8.82107 4.67893 10.5 6.75 10.5C8.82107 10.5 10.5 8.82107 10.5 6.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M21 17.25C21 15.1789 19.3211 13.5 17.25 13.5C15.1789 13.5 13.5 15.1789 13.5 17.25C13.5 19.3211 15.1789 21 17.25 21C19.3211 21 21 19.3211 21 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 17.25C10.5 15.1789 8.82107 13.5 6.75 13.5C4.67893 13.5 3 15.1789 3 17.25C3 19.3211 4.67893 21 6.75 21C8.82107 21 10.5 19.3211 10.5 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M20.6887 3.93395L20.0661 3.31132C19.651 2.89623 18.978 2.89623 18.5629 3.31131L15.2141 6.66008C14.769 7.10522 14.4656 7.67217 14.3421 8.28947L14 10L15.7105 9.65789C16.3278 9.53443 16.8948 9.23101 17.3399 8.78587L20.6887 5.43711C21.1038 5.02202 21.1038 4.34903 20.6887 3.93395Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `device-settings.svg` */
    val DeviceSettings: ImageVector by lazy {
        ImageVector.Builder(
            name = "DeviceSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 15.5C10 16.3284 9.32843 17 8.5 17C7.67157 17 7 16.3284 7 15.5C7 14.6716 7.67157 14 8.5 14C9.32843 14 10 14.6716 10 15.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M17 8.5C17 7.67157 16.3284 7 15.5 7C14.6716 7 14 7.67157 14 8.5C14 9.32843 14.6716 10 15.5 10C16.3284 10 17 9.32843 17 8.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8.5 14V7"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 10V17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `emergency-contacts.svg` */
    val EmergencyContacts: ImageVector by lazy {
        ImageVector.Builder(
            name = "EmergencyContacts",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.76722 18.8486L12 14L14.2328 18.8486C14.8804 20.2549 15.2042 20.958 14.8612 21.4656C14.8518 21.4795 14.8421 21.4932 14.8321 21.5067C14.4659 22 13.6439 22 12 22C10.3561 22 9.53409 22 9.16795 21.5067C9.15792 21.4932 9.14821 21.4795 9.13882 21.4656C8.79585 20.958 9.11964 20.2549 9.76722 18.8486Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 12 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M4 18.001C2.74418 16.3295 2 14.2516 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12C22 14.2516 21.2558 16.3295 20 18.001"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M7.52779 16C6.57771 14.9385 6 13.5367 6 12C6 8.68629 8.68629 6 12 6C15.3137 6 18 8.68629 18 12C18 13.5367 17.4223 14.9385 16.4722 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `fall-detection.svg` */
    val FallDetection: ImageVector by lazy {
        ImageVector.Builder(
            name = "FallDetection",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 512f,
            viewportHeight = 512f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M271.6826 5.12c17.3429 0 31.3544 14.0115 31.3544 31.3544v9.6023c0 53.4984 -27.3371 102.4896 -71.0372 130.9045l0.1959 0.294 55.6541 78.6799h85.7346c14.7954 0 28.7089 6.9568 37.6253 18.8126l42.3284 56.4379c10.3861 13.8156 7.5446 33.51 -6.2709 43.8962s-33.51 7.5446 -43.8961 -6.2709l-37.6253 -50.167h-95.4349l90.4378 139.723c9.4063 14.5014 5.2911 33.9019 -9.3083 43.4062s-33.902 5.2911 -43.4063 -9.3083L150.6743 249.2923c-2.8415 9.0144 -4.4092 18.6167 -4.4092 28.4149v72.3111c0 17.3429 -14.0115 31.3544 -31.3544 31.3544s-31.3544 -14.0115 -31.3544 -31.3544v-72.3111c0 -63.7866 38.8011 -121.2043 98.0805 -144.9161 35.4696 -14.2074 58.6914 -48.5013 58.6914 -86.7144v-9.6023c0 -17.3429 14.0115 -31.3544 31.3544 -31.3544ZM99.2335 36.4744c36.2049 0 58.833 39.193 40.7306 70.5474 -8.4018 14.5522 -23.9271 23.5165 -40.7306 23.5158 -36.2049 0 -58.833 -39.193 -40.7305 -70.5474 8.4017 -14.5523 23.927 -23.5166 40.7305 -23.5158Z"
                ),
                fill = SolidColor(Color.Black)
            )
        }.build()
    }

    /** `favourite.svg` */
    val Favourite: ImageVector by lazy {
        ImageVector.Builder(
            name = "Favourite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.4107 19.9677C7.58942 17.858 2 13.0348 2 8.69444C2 5.82563 4.10526 3.5 7 3.5C8.5 3.5 10 4 12 6C14 4 15.5 3.5 17 3.5C19.8947 3.5 22 5.82563 22 8.69444C22 13.0348 16.4106 17.858 13.5893 19.9677C12.6399 20.6776 11.3601 20.6776 10.4107 19.9677Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `find-the-device.svg` */
    val FindTheDevice: ImageVector by lazy {
        ImageVector.Builder(
            name = "FindTheDevice",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12.4014 8.29796L15.3213 7.32465C16.2075 7.02924 16.6507 6.88153 16.8846 7.11544C17.1185 7.34935 16.9708 7.79247 16.6753 8.67871L15.702 11.5986C15.1986 13.1088 14.9469 13.8639 14.4054 14.4054C13.8639 14.9469 13.1088 15.1986 11.5986 15.702L8.67871 16.6753C7.79247 16.9708 7.34935 17.1185 7.11544 16.8846C6.88153 16.6507 7.02924 16.2075 7.32465 15.3213L8.29796 12.4014C8.80136 10.8912 9.05306 10.1361 9.59457 9.59457C10.1361 9.05306 10.8912 8.80136 12.4014 8.29796Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 12H12M12.25 12C12.25 12.1381 12.1381 12.25 12 12.25C11.8619 12.25 11.75 12.1381 11.75 12C11.75 11.8619 11.8619 11.75 12 11.75C12.1381 11.75 12.25 11.8619 12.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 12C3 7.75736 3 5.63604 4.31802 4.31802C5.63604 3 7.75736 3 12 3C16.2426 3 18.364 3 19.682 4.31802C21 5.63604 21 7.75736 21 12C21 16.2426 21 18.364 19.682 19.682C18.364 21 16.2426 21 12 21C7.75736 21 5.63604 21 4.31802 19.682C3 18.364 3 16.2426 3 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `fire.svg` */
    val Fire: ImageVector by lazy {
        ImageVector.Builder(
            name = "Fire",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 22C16.1421 22 19.5 18.6421 19.5 14.5C19.5 13.5 19.5 11.5 17.5 9C17.5 9 17.4004 11.8536 15.4262 11.4408C12.2331 10.7732 16.3551 4.50296 10.5 2C10.5 7 4.5 8.5 4.5 14.5C4.5 18.6421 7.85786 22 12 22Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 19.0011C13.933 19.0011 15.5 16.9864 15.5 14.5011C12.3 15.7011 11.1667 12.9379 11 11C9.55426 11.5532 8.5 13.8256 8.5 15C8.5 17.4853 10.067 19.0011 12 19.0011Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `full-signal.svg` */
    val FullSignal: ImageVector by lazy {
        ImageVector.Builder(
            name = "FullSignal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.5 19C18.9659 19 19.1989 19 19.3827 18.9239C19.6277 18.8224 19.8224 18.6277 19.9239 18.3827C20 18.1989 20 17.9659 20 17.5V6.5C20 6.03406 20 5.80109 19.9239 5.61732C19.8224 5.37229 19.6277 5.17761 19.3827 5.07612C19.1989 5 18.9659 5 18.5 5M18.5 19C18.0341 19 17.8011 19 17.6173 18.9239C17.3723 18.8224 17.1776 18.6277 17.0761 18.3827C17 18.1989 17 17.9659 17 17.5V6.5C17 6.03406 17 5.80109 17.0761 5.61732C17.1776 5.37229 17.3723 5.17761 17.6173 5.07612C17.8011 5 18.0341 5 18.5 5M18.5 19V5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 19C12.4659 19 12.6989 19 12.8827 18.9239C13.1277 18.8224 13.3224 18.6277 13.4239 18.3827C13.5 18.1989 13.5 17.9659 13.5 17.5V9.5C13.5 9.03406 13.5 8.80109 13.4239 8.61732C13.3224 8.37229 13.1277 8.17761 12.8827 8.07612C12.6989 8 12.4659 8 12 8M12 19C11.5341 19 11.3011 19 11.1173 18.9239C10.8723 18.8224 10.6776 18.6277 10.5761 18.3827C10.5 18.1989 10.5 17.9659 10.5 17.5V9.5C10.5 9.03406 10.5 8.80109 10.5761 8.61732C10.6776 8.37229 10.8723 8.17761 11.1173 8.07612C11.3011 8 11.5341 8 12 8M12 19V8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.5 19C5.96594 19 6.19891 19 6.38268 18.9239C6.62771 18.8224 6.82239 18.6277 6.92388 18.3827C7 18.1989 7 17.9659 7 17.5V13.5C7 13.0341 7 12.8011 6.92388 12.6173C6.82239 12.3723 6.62771 12.1776 6.38268 12.0761C6.19891 12 5.96594 12 5.5 12M5.5 19C5.03406 19 4.80109 19 4.61732 18.9239C4.37229 18.8224 4.17761 18.6277 4.07612 18.3827C4 18.1989 4 17.9659 4 17.5V13.5C4 13.0341 4 12.8011 4.07612 12.6173C4.17761 12.3723 4.37229 12.1776 4.61732 12.0761C4.80109 12 5.03406 12 5.5 12M5.5 19V12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `geometric-shapes-01.svg` */
    val GeometricShapes01: ImageVector by lazy {
        ImageVector.Builder(
            name = "GeometricShapes01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M14.6171 4.76655C15.6275 3.16588 16.1327 2.36554 16.7947 2.12444C17.2503 1.95852 17.7497 1.95852 18.2053 2.12444C18.8673 2.36554 19.3725 3.16588 20.3829 4.76655C21.5202 6.56824 22.0889 7.46908 21.9887 8.21239C21.92 8.72222 21.6634 9.18799 21.2693 9.51835C20.6947 10 19.6298 10 17.5 10C15.3702 10 14.3053 10 13.7307 9.51835C13.3366 9.18799 13.08 8.72222 13.0113 8.21239C12.9111 7.46908 13.4798 6.56824 14.6171 4.76655Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 6C2 4.33345 2 3.50018 2.42441 2.91603C2.56147 2.72738 2.72738 2.56147 2.91603 2.42441C3.50018 2 4.33345 2 6 2C7.66655 2 8.49982 2 9.08397 2.42441C9.27262 2.56147 9.43853 2.72738 9.57559 2.91603C10 3.50018 10 4.33345 10 6C10 7.66655 10 8.49982 9.57559 9.08397C9.43853 9.27262 9.27262 9.43853 9.08397 9.57559C8.49982 10 7.66655 10 6 10C4.33345 10 3.50018 10 2.91603 9.57559C2.72738 9.43853 2.56147 9.27262 2.42441 9.08397C2 8.49982 2 7.66655 2 6Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.5 18 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.49994 14.5L2.5 21.5M2.50006 14.5L9.5 21.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `gps-disconnected.svg` */
    val GpsDisconnected: ImageVector by lazy {
        ImageVector.Builder(
            name = "GpsDisconnected",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.9999 7.99997L18.9999 4.99997M18.9999 4.99997L15.9999 1.99997M18.9999 4.99997L15.9999 7.99997M18.9999 4.99997L21.9999 1.99997"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8.99988 10.0294C10.3725 8.65685 12.5979 8.65685 13.9704 10.0294C15.343 11.402 15.343 13.6274 13.9704 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M4.85277 19.1471C8.04856 22.3429 12.9126 22.8538 16.6417 20.6797C17.5284 20.1627 17.9717 19.9043 17.9988 19.3782C18.0259 18.8522 17.5276 18.4882 16.5308 17.7603C14.6828 16.4107 12.8635 14.7603 11.0515 12.9484C9.23955 11.1364 7.58915 9.31705 6.23957 7.46904C5.51167 6.47231 5.14772 5.97395 4.62166 6.00105C4.0956 6.02815 3.83713 6.47149 3.32019 7.35818C1.14611 11.0873 1.65697 15.9513 4.85277 19.1471Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `gps-no-signal-02.svg` */
    val GpsNoSignal02: ImageVector by lazy {
        ImageVector.Builder(
            name = "GpsNoSignal02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.03027 9.99984C10.4029 8.62725 12.6283 8.62725 14.0008 9.99984C15.3734 11.3724 15.3734 13.5978 14.0008 14.9704"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M4.85289 19.1471C8.04869 22.3429 12.9127 22.8538 16.6418 20.6797C17.5285 20.1627 17.9719 19.9043 17.999 19.3782C18.0261 18.8522 17.5277 18.4882 16.531 17.7603C14.683 16.4107 12.8636 14.7603 11.0516 12.9484C9.23967 11.1364 7.58927 9.31705 6.23969 7.46904C5.51179 6.47231 5.14784 5.97395 4.62178 6.00105C4.09572 6.02815 3.83725 6.47149 3.32031 7.35818C1.14624 11.0873 1.6571 15.9513 4.85289 19.1471Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.875 3.25H20.75M21 3.25C21 3.38807 20.8881 3.5 20.75 3.5C20.6119 3.5 20.5 3.38807 20.5 3.25C20.5 3.11193 20.6119 3 20.75 3C20.8881 3 21 3.11193 21 3.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.3752 6.75H17.2502M17.5002 6.75C17.5002 6.88807 17.3883 7 17.2502 7C17.1121 7 17.0002 6.88807 17.0002 6.75C17.0002 6.61193 17.1121 6.5 17.2502 6.5C17.3883 6.5 17.5002 6.61193 17.5002 6.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `gps-signal-01.svg` */
    val GpsSignal01: ImageVector by lazy {
        ImageVector.Builder(
            name = "GpsSignal01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9 10.0294C10.3726 8.65685 12.598 8.65685 13.9706 10.0294C15.3431 11.402 15.3431 13.6274 13.9706 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M4.85289 19.1471C8.04869 22.3429 12.9127 22.8538 16.6418 20.6797C17.5285 20.1627 17.9719 19.9043 17.999 19.3782C18.0261 18.8522 17.5277 18.4882 16.531 17.7603C14.683 16.4107 12.8636 14.7603 11.0516 12.9484C9.23967 11.1364 7.58927 9.31705 6.23969 7.46904C5.51179 6.47231 5.14784 5.97395 4.62178 6.00105C4.09572 6.02815 3.83725 6.47149 3.32031 7.35818C1.14624 11.0873 1.6571 15.9513 4.85289 19.1471Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5574 2C17.2371 2.17243 18.7811 2.83962 19.9693 4.02583C21.1611 5.21579 21.8299 6.76334 22 8.4465M18.5017 9C18.3026 8.14257 17.8914 7.36144 17.2598 6.73091C16.6314 6.10356 15.8537 5.69396 15 5.49405"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `gps.svg` */
    val Gps: ImageVector by lazy {
        ImageVector.Builder(
            name = "Gps",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12.5355 6.10913C14.0144 4.63029 16.412 4.63029 17.8909 6.10913C19.3697 7.58796 19.3697 9.98563 17.8909 11.4645L15.7487 13.6066C14.8881 14.4672 14.4578 14.8975 13.937 14.98C13.7688 15.0067 13.5974 15.0067 13.4292 14.98C12.9084 14.8975 12.4781 14.4672 11.6175 13.6066L10.3934 12.3825C9.53278 11.5219 9.10247 11.0916 9.01998 10.5708C8.99334 10.4026 8.99334 10.2312 9.01998 10.063C9.10247 9.54219 9.53278 9.11188 10.3934 8.25126L12.5355 6.10913Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6.8483 14C6.96714 15.5706 8.41803 17.0084 10 17.1305M3.00586 15.2381C2.85202 18.2662 5.7538 21.1419 8.80421 20.9946"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.5293 5.52954L10.9336 3.8673C10.3555 3.2891 10.0664 3 9.70711 3C9.34786 3 9.05876 3.2891 8.48057 3.8673L7.8673 4.48057C7.2891 5.05876 7 5.34786 7 5.70711C7 6.06635 7.2891 6.35545 7.8673 6.93365L9.31826 8.38462M18.4214 11.355L20.1327 13.0664C20.7109 13.6445 21 13.9336 21 14.2929C21 14.6521 20.7109 14.9412 20.1327 15.5194L19.5194 16.1327C18.9412 16.7109 18.6521 17 18.2929 17C17.9336 17 17.6445 16.7109 17.0664 16.1327L15.5111 14.5775"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `hard-drive-download.svg` */
    val HardDriveDownload: ImageVector by lazy {
        ImageVector.Builder(
            name = "HardDriveDownload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 14H7C5.59987 14 4.8998 14 4.36502 14.2725C3.89462 14.5122 3.51217 14.8946 3.27248 15.365C3 15.8998 3 16.5999 3 18C3 19.4001 3 20.1002 3.27248 20.635C3.51217 21.1054 3.89462 21.4878 4.36502 21.7275C4.8998 22 5.59987 22 7 22H17C18.4001 22 19.1002 22 19.635 21.7275C20.1054 21.4878 20.4878 21.1054 20.7275 20.635C21 20.1002 21 19.4001 21 18C21 16.5999 21 15.8998 20.7275 15.365C20.4878 14.8946 20.1054 14.5122 19.635 14.2725C19.1002 14 18.4001 14 17 14Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 18V17.8M7.25 18C7.25 17.8619 7.13807 17.75 7 17.75C6.86193 17.75 6.75 17.8619 6.75 18C6.75 18.1381 6.86193 18.25 7 18.25C7.13807 18.25 7.25 18.1381 7.25 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 18V17.8M11.25 18C11.25 17.8619 11.1381 17.75 11 17.75C10.8619 17.75 10.75 17.8619 10.75 18C10.75 18.1381 10.8619 18.25 11 18.25C11.1381 18.25 11.25 18.1381 11.25 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 6.00003C8 6.00003 10.946 9.99999 12 10C13.0541 10 16 6 16 6M12 9V2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `hard-drive-upload.svg` */
    val HardDriveUpload: ImageVector by lazy {
        ImageVector.Builder(
            name = "HardDriveUpload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 18V17.8M7.25 18C7.25 17.8619 7.13807 17.75 7 17.75C6.86193 17.75 6.75 17.8619 6.75 18C6.75 18.1381 6.86193 18.25 7 18.25C7.13807 18.25 7.25 18.1381 7.25 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 18V17.8M11.25 18C11.25 17.8619 11.1381 17.75 11 17.75C10.8619 17.75 10.75 17.8619 10.75 18C10.75 18.1381 10.8619 18.25 11 18.25C11.1381 18.25 11.25 18.1381 11.25 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 5.99997C8 5.99997 10.946 2.00001 12 2C13.0541 1.99999 16 6 16 6M12 3V10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 14H7C5.59987 14 4.8998 14 4.36502 14.2725C3.89462 14.5122 3.51217 14.8946 3.27248 15.365C3 15.8998 3 16.5999 3 18C3 19.4001 3 20.1002 3.27248 20.635C3.51217 21.1054 3.89462 21.4878 4.36502 21.7275C4.8998 22 5.59987 22 7 22H17C18.4001 22 19.1002 22 19.635 21.7275C20.1054 21.4878 20.4878 21.1054 20.7275 20.635C21 20.1002 21 19.4001 21 18C21 16.5999 21 15.8998 20.7275 15.365C20.4878 14.8946 20.1054 14.5122 19.635 14.2725C19.1002 14 18.4001 14 17 14Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `help-circle.svg` */
    val HelpCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "HelpCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.5 9.5C9.5 8.11929 10.6193 7 12 7C13.3807 7 14.5 8.11929 14.5 9.5C14.5 10.3569 14.0689 11.1131 13.4117 11.5636C12.7283 12.0319 12 12.6716 12 13.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 16.75H12M12.25 16.75C12.25 16.8881 12.1381 17 12 17C11.8619 17 11.75 16.8881 11.75 16.75C11.75 16.6119 11.8619 16.5 12 16.5C12.1381 16.5 12.25 16.6119 12.25 16.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `help-square.svg` */
    val HelpSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "HelpSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9.5 9.5C9.5 8.11929 10.6193 7 12 7C13.3807 7 14.5 8.11929 14.5 9.5C14.5 10.3569 14.0689 11.1131 13.4117 11.5636C12.7283 12.0319 12 12.6716 12 13.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 16.75H12M12.25 16.75C12.25 16.8881 12.1381 17 12 17C11.8619 17 11.75 16.8881 11.75 16.75C11.75 16.6119 11.8619 16.5 12 16.5C12.1381 16.5 12.25 16.6119 12.25 16.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `info.svg` */
    val Info: ImageVector by lazy {
        ImageVector.Builder(
            name = "Info",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 11.9999 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 15.9999L12 11.9999"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.875 8.24994L12 8.24994M11.75 8.24994C11.75 8.11187 11.8619 7.99994 12 7.99994C12.1381 7.99994 12.25 8.11187 12.25 8.24994C12.25 8.38801 12.1381 8.49994 12 8.49994C11.8619 8.49994 11.75 8.38801 11.75 8.24994Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `information-square.svg` */
    val InformationSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "InformationSquare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2.5 12C2.5 7.52166 2.5 5.28249 3.89124 3.89124C5.28249 2.5 7.52166 2.5 12 2.5C16.4783 2.5 18.7175 2.5 20.1088 3.89124C21.5 5.28249 21.5 7.52166 21.5 12C21.5 16.4783 21.5 18.7175 20.1088 20.1088C18.7175 21.5 16.4783 21.5 12 21.5C7.52166 21.5 5.28249 21.5 3.89124 20.1088C2.5 18.7175 2.5 16.4783 2.5 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 16V12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 8.25H12M12.25 8.25C12.25 8.11193 12.1381 8 12 8C11.8619 8 11.75 8.11193 11.75 8.25C11.75 8.38807 11.8619 8.5 12 8.5C12.1381 8.5 12.25 8.38807 12.25 8.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `internet.svg` */
    val Internet: ImageVector by lazy {
        ImageVector.Builder(
            name = "Internet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8 12 a 4 10 0 1 0 8 0 a 4 10 0 1 0 -8 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 12H22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `lights.svg` */
    val Lights: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lights",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.2916 16C18.9525 14.5341 20 12.3894 20 10C20 5.58173 16.4182 2 12 2C7.58173 2 4 5.58173 4 10C4 12.3894 5.04751 14.5341 6.70836 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 7L10.8181 8.35982C10.2013 9.06938 9.89295 9.42416 10.0336 9.71208C10.1743 10 10.656 10 11.6193 10H12.3807C13.344 10 13.8257 10 13.9664 10.2879C14.1071 10.5758 13.7987 10.9306 13.1819 11.6402L12 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8.5 19H15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 22H14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `low-signal.svg` */
    val LowSignal: ImageVector by lazy {
        ImageVector.Builder(
            name = "LowSignal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 17.5V6.5C17 6.03406 17 5.80109 17.0761 5.61732C17.1776 5.37229 17.3723 5.17761 17.6173 5.07612C17.8011 5 18.0341 5 18.5 5C18.9659 5 19.1989 5 19.3827 5.07612C19.6277 5.17761 19.8224 5.37229 19.9239 5.61732C20 5.80109 20 6.03406 20 6.5V17.5C20 17.9659 20 18.1989 19.9239 18.3827C19.8224 18.6277 19.6277 18.8224 19.3827 18.9239C19.1989 19 18.9659 19 18.5 19C18.0341 19 17.8011 19 17.6173 18.9239C17.3723 18.8224 17.1776 18.6277 17.0761 18.3827C17 18.1989 17 17.9659 17 17.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 17.5V9.5C10.5 9.03406 10.5 8.80109 10.5761 8.61732C10.6776 8.37229 10.8723 8.17761 11.1173 8.07612C11.3011 8 11.5341 8 12 8C12.4659 8 12.6989 8 12.8827 8.07612C13.1277 8.17761 13.3224 8.37229 13.4239 8.61732C13.5 8.80109 13.5 9.03406 13.5 9.5V17.5C13.5 17.9659 13.5 18.1989 13.4239 18.3827C13.3224 18.6277 13.1277 18.8224 12.8827 18.9239C12.6989 19 12.4659 19 12 19C11.5341 19 11.3011 19 11.1173 18.9239C10.8723 18.8224 10.6776 18.6277 10.5761 18.3827C10.5 18.1989 10.5 17.9659 10.5 17.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.5 19C5.96594 19 6.19891 19 6.38268 18.9239C6.62771 18.8224 6.82239 18.6277 6.92388 18.3827C7 18.1989 7 17.9659 7 17.5V13.5C7 13.0341 7 12.8011 6.92388 12.6173C6.82239 12.3723 6.62771 12.1776 6.38268 12.0761C6.19891 12 5.96594 12 5.5 12M5.5 19C5.03406 19 4.80109 19 4.61732 18.9239C4.37229 18.8224 4.17761 18.6277 4.07612 18.3827C4 18.1989 4 17.9659 4 17.5V13.5C4 13.0341 4 12.8011 4.07612 12.6173C4.17761 12.3723 4.37229 12.1776 4.61732 12.0761C4.80109 12 5.03406 12 5.5 12M5.5 19V12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `medical-id.svg` */
    val MedicalId: ImageVector by lazy {
        ImageVector.Builder(
            name = "MedicalId",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19 9V7.81818C19 6.12494 19 5.27832 18.7478 4.60214C18.3424 3.5151 17.4849 2.65765 16.3979 2.2522C15.7217 2 14.8751 2 13.1818 2C10.2186 2 8.73706 2 7.55375 2.44135C5.65142 3.15088 4.15088 4.65142 3.44135 6.55375C3 7.73706 3 9.21865 3 12.1818L3 14.7273C3 17.7966 3 19.3313 3.79783 20.3971C4.02643 20.7025 4.29752 20.9736 4.60289 21.2022C5.66867 22 7.20336 22 10.2727 22H11C12.1698 22 14.5 22 14.5 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 14.3333H11.8403C12.5019 14.3333 12.8326 14.3333 13.0985 14.5076C13.3643 14.6818 13.5122 14.9956 13.8081 15.6232L15.4 19L17.6 12L19.1919 15.3768C19.4878 16.0044 19.6357 16.3182 19.9015 16.4924C20.1674 16.6667 20.4981 16.6667 21.1597 16.6667H22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 12C3 10.1591 4.49238 8.66667 6.33333 8.66667H7.44444C7.9611 8.66667 8.21942 8.66667 8.43137 8.60988C9.00652 8.45577 9.45576 8.00652 9.60988 7.43137C9.66667 7.21942 9.66667 6.9611 9.66667 6.44444V5.33333C9.66667 3.49238 11.1591 2 13 2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `medium-signal.svg` */
    val MediumSignal: ImageVector by lazy {
        ImageVector.Builder(
            name = "MediumSignal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 17.5V6.5C17 6.03406 17 5.80109 17.0761 5.61732C17.1776 5.37229 17.3723 5.17761 17.6173 5.07612C17.8011 5 18.0341 5 18.5 5C18.9659 5 19.1989 5 19.3827 5.07612C19.6277 5.17761 19.8224 5.37229 19.9239 5.61732C20 5.80109 20 6.03406 20 6.5V17.5C20 17.9659 20 18.1989 19.9239 18.3827C19.8224 18.6277 19.6277 18.8224 19.3827 18.9239C19.1989 19 18.9659 19 18.5 19C18.0341 19 17.8011 19 17.6173 18.9239C17.3723 18.8224 17.1776 18.6277 17.0761 18.3827C17 18.1989 17 17.9659 17 17.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 19C12.4659 19 12.6989 19 12.8827 18.9239C13.1277 18.8224 13.3224 18.6277 13.4239 18.3827C13.5 18.1989 13.5 17.9659 13.5 17.5V9.5C13.5 9.03406 13.5 8.80109 13.4239 8.61732C13.3224 8.37229 13.1277 8.17761 12.8827 8.07612C12.6989 8 12.4659 8 12 8M12 19C11.5341 19 11.3011 19 11.1173 18.9239C10.8723 18.8224 10.6776 18.6277 10.5761 18.3827C10.5 18.1989 10.5 17.9659 10.5 17.5V9.5C10.5 9.03406 10.5 8.80109 10.5761 8.61732C10.6776 8.37229 10.8723 8.17761 11.1173 8.07612C11.3011 8 11.5341 8 12 8M12 19V8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.5 19C5.96594 19 6.19891 19 6.38268 18.9239C6.62771 18.8224 6.82239 18.6277 6.92388 18.3827C7 18.1989 7 17.9659 7 17.5V13.5C7 13.0341 7 12.8011 6.92388 12.6173C6.82239 12.3723 6.62771 12.1776 6.38268 12.0761C6.19891 12 5.96594 12 5.5 12M5.5 19C5.03406 19 4.80109 19 4.61732 18.9239C4.37229 18.8224 4.17761 18.6277 4.07612 18.3827C4 18.1989 4 17.9659 4 17.5V13.5C4 13.0341 4 12.8011 4.07612 12.6173C4.17761 12.3723 4.37229 12.1776 4.61732 12.0761C4.80109 12 5.03406 12 5.5 12M5.5 19V12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `message-inbox.svg` */
    val MessageInbox: ImageVector by lazy {
        ImageVector.Builder(
            name = "MessageInbox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 8.5L9.94202 10.2394C11.6572 11.2535 12.3428 11.2535 14.058 10.2394L17 8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.01577 13.4756C2.08114 16.5412 2.11383 18.0739 3.24496 19.2094C4.37608 20.3448 5.95033 20.3843 9.09883 20.4634C11.0393 20.5122 12.9607 20.5122 14.9012 20.4634C18.0497 20.3843 19.6239 20.3448 20.7551 19.2094C21.8862 18.0739 21.9189 16.5412 21.9842 13.4756C22.0053 12.4899 22.0053 11.5101 21.9842 10.5244C21.9189 7.45886 21.8862 5.92609 20.7551 4.79066C19.6239 3.65523 18.0497 3.61568 14.9012 3.53657C12.9607 3.48781 11.0393 3.48781 9.09882 3.53656C5.95033 3.61566 4.37608 3.65521 3.24495 4.79065C2.11382 5.92608 2.08114 7.45885 2.01576 10.5244C1.99474 11.5101 1.99475 12.4899 2.01577 13.4756Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `navbar-board.svg` */
    val NavbarBoard: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavbarBoard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21 6.75C21 4.67893 19.3211 3 17.25 3C15.1789 3 13.5 4.67893 13.5 6.75C13.5 8.82107 15.1789 10.5 17.25 10.5C19.3211 10.5 21 8.82107 21 6.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 6.75C10.5 4.67893 8.82107 3 6.75 3C4.67893 3 3 4.67893 3 6.75C3 8.82107 4.67893 10.5 6.75 10.5C8.82107 10.5 10.5 8.82107 10.5 6.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21 17.25C21 15.1789 19.3211 13.5 17.25 13.5C15.1789 13.5 13.5 15.1789 13.5 17.25C13.5 19.3211 15.1789 21 17.25 21C19.3211 21 21 19.3211 21 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 17.25C10.5 15.1789 8.82107 13.5 6.75 13.5C4.67893 13.5 3 15.1789 3 17.25C3 19.3211 4.67893 21 6.75 21C8.82107 21 10.5 19.3211 10.5 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `navbar-circle.svg` */
    val NavbarCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavbarCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15.7078 8.33055C16.6507 9.26482 15.1422 16.8628 13.5942 16.9967C12.2957 17.109 11.891 14.5478 11.6175 13.7361C11.3476 12.9349 11.0472 12.6465 10.2527 12.3836C8.23415 11.7159 7.22489 11.382 7.02507 10.8533C6.49595 9.45337 14.5036 7.13731 15.7078 8.33055Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `navbar-device.svg` */
    val NavbarDevice: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavbarDevice",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11 16.5C11 19.5376 13.4624 22 16.5 22C19.5376 22 22 19.5376 22 16.5C22 13.4624 19.5376 11 16.5 11C13.4624 11 11 13.4624 11 16.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 16H3.54895C2.69349 16 2 15.3065 2 14.4511C2 14.1565 2.08401 13.868 2.24216 13.6195L8.46312 3.84366C8.79751 3.3182 9.37717 3 10 3C10.6228 3 11.2025 3.3182 11.5369 3.84366L14 7.71429"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19.5 2.9375V4.5M19.5 4.5V6.0625M19.5 4.5H18.25M19.5 4.5H20.75M22 4.5L20.9156 4.13852C20.4179 3.97263 20.0274 3.58211 19.8615 3.08443L19.5 2L19.1385 3.08443C18.9726 3.58211 18.5821 3.97263 18.0844 4.13852L17 4.5L18.0844 4.86148C18.5821 5.02737 18.9726 5.41789 19.1385 5.91557L19.5 7L19.8615 5.91557C20.0274 5.41789 20.4179 5.02737 20.9156 4.86148L22 4.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `navbar-safety.svg` */
    val NavbarSafety: ImageVector by lazy {
        ImageVector.Builder(
            name = "NavbarSafety",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.7088 3.49534C16.8165 2.55382 14.5009 2 12 2C9.4991 2 7.1835 2.55382 5.29116 3.49534C4.36318 3.95706 3.89919 4.18792 3.4496 4.91378C3 5.63965 3 6.34248 3 7.74814V11.2371C3 16.9205 7.54236 20.0804 10.173 21.4338C10.9067 21.8113 11.2735 22 12 22C12.7265 22 13.0933 21.8113 13.8269 21.4338C16.4576 20.0804 21 16.9205 21 11.2371L21 7.74814C21 6.34249 21 5.63966 20.5504 4.91378C20.1008 4.18791 19.6368 3.95706 18.7088 3.49534Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15 11C15 12.6568 13.6569 14 12 14C10.3431 14 9 12.6568 9 11C9 9.34314 10.3431 8 12 8C13.6569 8 15 9.34314 15 11Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `navigation-07.svg` */
    val Navigation07: ImageVector by lazy {
        ImageVector.Builder(
            name = "Navigation07",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6.72945 10.4584C9.00174 5.81947 10.1379 3.5 11.9922 3.5C13.8465 3.5 14.9826 5.81946 17.2549 10.4584L18.8023 13.6174C20.5474 17.18 21.4199 18.9613 20.7855 19.8178C20.615 20.0481 20.3902 20.238 20.1288 20.3729C19.1565 20.8743 17.3716 19.8641 13.8018 17.8436C13.01 17.3954 12.6141 17.1713 12.1811 17.1312C12.0555 17.1196 11.9289 17.1196 11.8032 17.1312C11.3703 17.1713 10.9744 17.3954 10.1826 17.8436C6.61278 19.8641 4.82789 20.8743 3.85556 20.3729C3.59415 20.238 3.36938 20.0481 3.19883 19.8178C2.56445 18.9613 3.437 17.18 5.18208 13.6174L6.72945 10.4584Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `new-releases.svg` */
    val NewReleases: ImageVector by lazy {
        ImageVector.Builder(
            name = "NewReleases",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11.5143 2.09327C11.8265 1.96891 12.1735 1.96891 12.4857 2.09327C13.4921 2.49406 13.6887 4.03744 14.8762 4.12336C15.7124 4.18386 16.533 3.48677 17.3721 3.69574C17.7105 3.78003 18.0028 3.99579 18.186 4.29657C18.7472 5.21824 18.0229 6.57292 18.9383 7.33768C19.5743 7.86877 20.6251 7.80004 21.178 8.4511C21.4108 8.72534 21.5252 9.08303 21.4953 9.4437C21.4068 10.5166 20.0389 11.1876 20.3395 12.3439C20.5475 13.1443 21.4253 13.707 21.4953 14.5563C21.5252 14.917 21.4108 15.2747 21.178 15.5489C20.4832 16.3669 18.9808 16.0975 18.5476 17.2062C18.2434 17.9844 18.634 18.9677 18.186 19.7034C18.0028 20.0042 17.7105 20.22 17.3721 20.3043C16.3302 20.5637 15.2727 19.4445 14.2701 20.0758C13.5543 20.5264 13.2978 21.5835 12.4857 21.9067C12.1735 22.0311 11.8265 22.0311 11.5143 21.9067C10.7022 21.5835 10.4457 20.5264 9.72989 20.0758C8.73971 19.4524 7.65213 20.5593 6.62791 20.3043C6.28947 20.22 5.9972 20.0042 5.81405 19.7034C5.25286 18.7818 5.97704 17.427 5.0617 16.6623C4.42582 16.1312 3.37494 16.2 2.82204 15.5489C2.58921 15.2747 2.47484 14.917 2.50465 14.5563C2.57485 13.707 3.4524 13.1443 3.6605 12.3439C3.95808 11.1997 2.59204 10.5009 2.50465 9.4437C2.47484 9.08303 2.58921 8.72534 2.82204 8.4511C3.51676 7.63284 5.01899 7.90253 5.45238 6.79383C5.75662 6.0156 5.36608 5.03227 5.81405 4.29657C5.9972 3.99579 6.28947 3.78003 6.62791 3.69574C7.46705 3.48677 8.28757 4.18387 9.12378 4.12336C10.3113 4.03746 10.5079 2.49406 11.5143 2.09327Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 13.3333C9 13.3333 9.875 13.3333 10.75 15C10.75 15 13.5294 10.8333 16 10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `no-internet.svg` */
    val NoInternet: ImageVector by lazy {
        ImageVector.Builder(
            name = "NoInternet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5 5C3.14864 6.79995 2 9.3082 2 12.0825C2 17.5598 6.47715 22 12 22C14.7255 22 17.1962 20.9187 19 19.165"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 16C14.8828 19.5318 13.6403 22 12 22C9.79086 22 8 17.5228 8 12C8 10.7687 8.08902 9.58934 8.25184 8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 12H12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.16204 2.39775C6.79227 2.5844 6.64381 3.03547 6.83046 3.40525C7.01711 3.77503 7.46818 3.92348 7.83796 3.73683L7.16204 2.39775ZM20.2627 16.1619C20.076 16.5317 20.2244 16.9828 20.5941 17.1695C20.9639 17.3562 21.415 17.2078 21.6017 16.8381L20.2627 16.1619ZM16 12H15.25C15.25 12.4142 15.5858 12.75 16 12.75V12ZM8.30261 4.7262C8.1514 5.11183 8.34143 5.54702 8.72705 5.69824C9.11268 5.84945 9.54787 5.65942 9.69909 5.2738L8.30261 4.7262ZM12 2.75C17.1086 2.75 21.25 6.89137 21.25 12H22.75C22.75 6.06294 17.9371 1.25 12 1.25V2.75ZM7.83796 3.73683C9.08814 3.10579 10.5014 2.75 12 2.75V1.25C10.2613 1.25 8.61705 1.66333 7.16204 2.39775L7.83796 3.73683ZM21.25 12C21.25 13.4986 20.894 14.9118 20.2627 16.1619L21.6017 16.8381C22.3363 15.3831 22.75 13.7389 22.75 12H21.25ZM12 2.75C12.2796 2.75 12.6219 2.88858 13.0121 3.30118C13.4038 3.71536 13.7911 4.35495 14.1321 5.20748C14.8124 6.90837 15.25 9.30957 15.25 12H16.75C16.75 9.16759 16.2921 6.56878 15.5248 4.65039C15.1419 3.69327 14.6673 2.86839 14.1019 2.27052C13.535 1.67106 12.825 1.25 12 1.25V2.75ZM9.69909 5.2738C10.0345 4.41855 10.4545 3.76449 10.8831 3.33637C11.3161 2.90388 11.7049 2.75 12 2.75V1.25C11.1762 1.25 10.4284 1.67044 9.82303 2.27511C9.21329 2.88415 8.69322 3.73009 8.30261 4.7262L9.69909 5.2738ZM16 12.75H22V11.25H16V12.75Z"
                ),
                fill = SolidColor(Color.Black)
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22.0004 22.0004"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `ocean.svg` */
    val Ocean: ImageVector by lazy {
        ImageVector.Builder(
            name = "Ocean",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12.1932C2.68524 13.2443 3.57104 13.2443 4.27299 12.1932C6.52985 8.7408 8.67954 14.6764 10.273 12.2321C12.703 8.56944 14.4508 14.9218 16.273 12.1932C18.6492 8.5582 20.1295 14.5776 22 12.5842"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 6.1932C2.68524 7.24434 3.57104 7.24434 4.27299 6.1932C6.52985 2.7408 8.67954 8.67642 10.273 6.23213C12.703 2.56944 14.4508 8.92184 16.273 6.1932C18.6492 2.5582 20.1295 8.57758 22 6.58418"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 18.1932C2.68524 19.2443 3.57104 19.2443 4.27299 18.1932C6.52985 14.7408 8.67954 20.6764 10.273 18.2321C12.703 14.5694 14.4508 20.9218 16.273 18.1932C18.6492 14.5582 20.1295 20.5776 22 18.5842"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `paired-devices.svg` */
    val PairedDevices: ImageVector by lazy {
        ImageVector.Builder(
            name = "PairedDevices",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12.4742 12L16.2428 9.05534C17.3189 8.21451 17.857 7.79409 17.9716 7.24865C18.0144 7.04517 18.0154 6.83493 17.9748 6.63101C17.8657 6.08438 17.332 5.65832 16.2645 4.8062C14.6552 3.52156 13.8505 2.87924 13.1738 3.01878C12.9267 3.06975 12.6962 3.18351 12.504 3.34942C11.9779 3.80362 11.9779 4.84315 11.9779 6.92221V11.6122M12.4742 12L11.9779 12.3877M12.4742 12L16.2428 14.9446C17.319 15.7855 17.857 16.2059 17.9716 16.7513C18.0144 16.9548 18.0155 17.165 17.9748 17.369C17.8658 17.9156 17.332 18.3417 16.2645 19.1938C14.6552 20.4784 13.8505 21.1208 13.1738 20.9812C12.9266 20.9302 12.6962 20.8165 12.504 20.6506C11.9779 20.1964 11.9779 19.1568 11.9779 17.0778V12.3877M12.4742 12L11.9779 11.6122M11.9779 12.3877L6.00452 17.055M11.9779 12.3877V11.6122M11.9779 11.6122L6.00452 6.94494"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.37952 12H5.25452M18.8795 12H18.7545M5.50452 12C5.50452 12.1381 5.39259 12.25 5.25452 12.25C5.11645 12.25 5.00452 12.1381 5.00452 12C5.00452 11.8619 5.11645 11.75 5.25452 11.75C5.39259 11.75 5.50452 11.8619 5.50452 12ZM19.0045 12C19.0045 12.1381 18.8926 12.25 18.7545 12.25C18.6164 12.25 18.5045 12.1381 18.5045 12C18.5045 11.8619 18.6164 11.75 18.7545 11.75C18.8926 11.75 19.0045 11.8619 19.0045 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `parental-control.svg` */
    val ParentalControl: ImageVector by lazy {
        ImageVector.Builder(
            name = "ParentalControl",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2.5 8.18689C2.60406 6.08717 2.91537 4.77804 3.84664 3.84676C4.77792 2.91549 6.08705 2.60418 8.18677 2.50012M21.5 8.18689C21.3959 6.08717 21.0846 4.77804 20.1534 3.84676C19.2221 2.91549 17.9129 2.60418 15.8132 2.50012M15.8132 21.5001C17.9129 21.396 19.2221 21.0847 20.1534 20.1535C21.0846 19.2222 21.3959 17.913 21.5 15.8133M8.18676 21.5001C6.08705 21.396 4.77792 21.0847 3.84664 20.1535C2.91537 19.2222 2.60406 17.913 2.5 15.8133"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.5 10.5545C9.5 9.7128 9.36781 8.41337 9.87602 7.65569C10.9985 5.98217 13.38 6.20448 14.22 7.83797C14.6323 8.63969 14.4769 9.76055 14.496 10.5545M9.5 10.5545C8.20267 10.5545 7.93843 11.2972 7.74002 11.8797C7.55687 12.535 7.37042 14.0997 7.65602 15.8142C7.86969 16.9064 8.70479 17.3868 9.42297 17.4477C10.1098 17.5059 13.0097 17.4837 13.8492 17.4837C15.1501 17.4837 15.9624 17.1977 16.344 15.887C16.5272 14.8676 16.5773 13.0447 16.272 11.8797C15.8676 10.7146 15.0523 10.5545 14.496 10.5545M9.5 10.5545C10.8736 10.5 13.7107 10.5108 14.496 10.5545"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `pause.svg` */
    val Pause: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4 7C4 5.58579 4 4.87868 4.43934 4.43934C4.87868 4 5.58579 4 7 4C8.41421 4 9.12132 4 9.56066 4.43934C10 4.87868 10 5.58579 10 7V17C10 18.4142 10 19.1213 9.56066 19.5607C9.12132 20 8.41421 20 7 20C5.58579 20 4.87868 20 4.43934 19.5607C4 19.1213 4 18.4142 4 17V7Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M14 7C14 5.58579 14 4.87868 14.4393 4.43934C14.8787 4 15.5858 4 17 4C18.4142 4 19.1213 4 19.5607 4.43934C20 4.87868 20 5.58579 20 7V17C20 18.4142 20 19.1213 19.5607 19.5607C19.1213 20 18.4142 20 17 20C15.5858 20 14.8787 20 14.4393 19.5607C14 19.1213 14 18.4142 14 17V7Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `pin-location.svg` */
    val PinLocation: ImageVector by lazy {
        ImageVector.Builder(
            name = "PinLocation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8 6 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5 16C3.7492 16.6327 3 17.4385 3 18.3158C3 20.3505 7.02944 22 12 22C16.9706 22 21 20.3505 21 18.3158C21 17.4385 20.2508 16.6327 19 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 10L12 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `play.svg` */
    val Play: ImageVector by lazy {
        ImageVector.Builder(
            name = "Play",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.8906 12.846C18.5371 14.189 16.8667 15.138 13.5257 17.0361C10.296 18.8709 8.6812 19.7884 7.37983 19.4196C6.8418 19.2671 6.35159 18.9776 5.95624 18.5787C5 17.6139 5 15.7426 5 12C5 8.2574 5 6.3861 5.95624 5.42132C6.35159 5.02245 6.8418 4.73288 7.37983 4.58042C8.6812 4.21165 10.296 5.12907 13.5257 6.96393C16.8667 8.86197 18.5371 9.811 18.8906 11.154C19.0365 11.7084 19.0365 12.2916 18.8906 12.846Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `police.svg` */
    val Police: ImageVector by lazy {
        ImageVector.Builder(
            name = "Police",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4.26759 4.32782C5.95399 3.02741 8.57337 2 12 2C15.4266 2 18.046 3.02741 19.7324 4.32782C19.9693 4.51048 20.0877 4.60181 20.1849 4.76366C20.2665 4.89952 20.3252 5.10558 20.3275 5.26404C20.3302 5.4528 20.2672 5.62069 20.1413 5.95648C19.8305 6.78539 19.6751 7.19984 19.6122 7.61031C19.533 8.12803 19.5322 8.25474 19.6053 8.77338C19.6632 9.18457 19.9795 10.0598 20.6121 11.8103C20.844 12.452 21 13.1792 21 14C21 17 18.5 19.375 16 20C13.8082 20.548 12.6667 21.3333 12 22C11.3333 21.3333 10.1918 20.548 8 20C5.5 19.375 3 17 3 14C3 13.1792 3.15595 12.452 3.38785 11.8103C4.0205 10.0598 4.33682 9.18457 4.39473 8.77338C4.46777 8.25474 4.46702 8.12803 4.38777 7.61031C4.32494 7.19984 4.16952 6.78539 3.85868 5.95648C3.73276 5.62069 3.6698 5.4528 3.67252 5.26404C3.6748 5.10558 3.73351 4.89952 3.81509 4.76366C3.91227 4.60181 4.03071 4.51048 4.26759 4.32782Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.6911 7.57767L13.395 8.99715C13.491 9.19475 13.7469 9.38428 13.9629 9.42057L15.2388 9.6343C16.0547 9.77141 16.2467 10.3682 15.6587 10.957L14.6668 11.9571C14.4989 12.1265 14.4069 12.4531 14.4589 12.687L14.7428 13.925C14.9668 14.9049 14.4509 15.284 13.591 14.7718L12.3951 14.0581C12.1791 13.929 11.8232 13.929 11.6032 14.0581L10.4073 14.7718C9.5514 15.284 9.03146 14.9009 9.25543 13.925L9.5394 12.687C9.5914 12.4531 9.49941 12.1265 9.33143 11.9571L8.33954 10.957C7.7556 10.3682 7.94358 9.77141 8.75949 9.6343L10.0353 9.42057C10.2473 9.38428 10.5033 9.19475 10.5993 8.99715L11.3032 7.57767C11.6872 6.80744 12.3111 6.80744 12.6911 7.57767Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `pulse.svg` */
    val Pulse: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pulse",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 10.1915 2.4801 8.49505 3.31981 7.03127"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5 12C5 15.866 8.13401 19 12 19C15.866 19 19 15.866 19 12C19 8.13401 15.866 5 12 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 16C14.2091 16 16 14.2091 16 12C16 9.79086 14.2091 8 12 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `quiet-hours.svg` */
    val QuietHours: ImageVector by lazy {
        ImageVector.Builder(
            name = "QuietHours",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.5 14.0784C20.3003 14.7189 18.9301 15.0821 17.4751 15.0821C12.7491 15.0821 8.91792 11.2509 8.91792 6.52485C8.91792 5.06986 9.28105 3.69968 9.92163 2.5C5.66765 3.49698 2.5 7.31513 2.5 11.8731C2.5 17.1899 6.8101 21.5 12.1269 21.5C16.6849 21.5 20.503 18.3324 21.5 14.0784Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `rainbow.svg` */
    val Rainbow: ImageVector by lazy {
        ImageVector.Builder(
            name = "Rainbow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15.5802 8L14.9874 6.85021C13.664 4.2834 13.0023 3 12 3C10.9977 3 10.336 4.2834 9.01261 6.85021L4.59051 15.4272C3.31146 17.908 2.67193 19.1484 3.16823 20.0742C3.66452 21 4.96898 21 7.5779 21H16.4221C19.031 21 20.3355 21 20.8318 20.0742C21.3281 19.1484 20.6885 17.908 19.4095 15.4272L19.0598 14.7489"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 10L22 7M8 10L21.4615 11.5M8 10L22 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 10L2 11"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `reminders.svg` */
    val Reminders: ImageVector by lazy {
        ImageVector.Builder(
            name = "Reminders",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16 2V6M8 2V6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.8985 8C20.7706 6.69989 20.4816 5.82475 19.8284 5.17157C18.6569 4 16.7712 4 13 4H11C7.22876 4 5.34315 4 4.17157 5.17157C3 6.34315 3 8.22876 3 12V14C3 17.7712 3 19.6569 4.17157 20.8284C4.97975 21.6366 6.1277 21.8873 8 21.965"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 10H8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16 22C19.3137 22 22 19.3137 22 16C22 12.6863 19.3137 10 16 10C12.6863 10 10 12.6863 10 16C10 19.3137 12.6863 22 16 22Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16 13V16L18 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `repeating-check-in.svg` */
    val RepeatingCheckIn: ImageVector by lazy {
        ImageVector.Builder(
            name = "RepeatingCheckIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.0117 7.48959L19.5013 6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.51903 19C7.07817 20.8354 9.40308 22 12 22C16.6944 22 20.5 18.1944 20.5 13.5C20.5 8.80558 16.6944 5 12 5C7.30558 5 3.5 8.80558 3.5 13.5C3.5 14.0118 3.54524 14.5131 3.63193 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5 2H9.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 13.5L15.5 10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8.5 18.2213C8.5 18.2213 5.21917 17.7234 4.72128 18.2213C4.22339 18.7192 4.72129 22 4.72129 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `ring-the-device.svg` */
    val RingTheDevice: ImageVector by lazy {
        ImageVector.Builder(
            name = "RingTheDevice",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M22 8C22 5.7 21.2 3.7 20 2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M4 2C2.8 3.7 2 5.7 2 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16 18C16 20.2091 14.2091 22 12 22C9.79086 22 8 20.2091 8 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M4.43654 18H19.5625C20.2903 18 20.6542 18 20.8648 17.8951C21.274 17.6913 21.4929 17.2359 21.3964 16.789C21.3468 16.559 21.1194 16.2749 20.6648 15.7066L20.4951 15.4944C20.0392 14.9246 19.8113 14.6397 19.6184 14.3409C19.0187 13.4119 18.6477 12.354 18.5356 11.254C18.4995 10.9002 18.4995 10.5353 18.4995 9.8056V8.5C18.4995 8.03572 18.4995 7.80358 18.4867 7.60758C18.2898 4.60304 15.8965 2.20977 12.892 2.01285C12.696 2 12.4638 2 11.9995 2C11.5353 2 11.3031 2 11.1071 2.01285C8.10258 2.20977 5.70931 4.60304 5.51239 7.60758C5.49954 7.80358 5.49954 8.03572 5.49954 8.5V9.8056C5.49954 10.5353 5.49954 10.9002 5.46349 11.254C5.35143 12.354 4.98035 13.4119 4.38067 14.3409C4.18779 14.6397 3.95985 14.9246 3.50401 15.4944L3.33427 15.7066C2.87964 16.2749 2.65233 16.559 2.60268 16.789C2.50621 17.2359 2.72509 17.6913 3.13431 17.8951C3.3449 18 3.70878 18 4.43654 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `safe-zone.svg` */
    val SafeZone: ImageVector by lazy {
        ImageVector.Builder(
            name = "SafeZone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.7088 3.49534C16.8165 2.55382 14.5009 2 12 2C9.4991 2 7.1835 2.55382 5.29116 3.49534C4.36318 3.95706 3.89919 4.18792 3.4496 4.91378C3 5.63965 3 6.34248 3 7.74814V11.2371C3 16.9205 7.54236 20.0804 10.173 21.4338C10.9067 21.8113 11.2735 22 12 22C12.7265 22 13.0933 21.8113 13.8269 21.4338C16.4576 20.0804 21 16.9205 21 11.2371L21 7.74814C21 6.34249 21 5.63966 20.5504 4.91378C20.1008 4.18791 19.6368 3.95706 18.7088 3.49534Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 10V8.5C10 7.39543 10.8954 6.5 12 6.5C13.1046 6.5 14 7.39543 14 8.5V10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14 10H10C9.17157 10 8.5 10.6716 8.5 11.5V13C8.5 13.8284 9.17157 14.5 10 14.5H14C14.8284 14.5 15.5 13.8284 15.5 13V11.5C15.5 10.6716 14.8284 10 14 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `search-01.svg` */
    val Search01: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 17L21 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19 11C19 6.58172 15.4183 3 11 3C6.58172 3 3 6.58172 3 11C3 15.4183 6.58172 19 11 19C15.4183 19 19 15.4183 19 11Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `send-message-diagonal.svg` */
    val SendMessageDiagonal: ImageVector by lazy {
        ImageVector.Builder(
            name = "SendMessageDiagonal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.87038 6.13264L14.7327 4.19538C18.033 3.10476 19.6831 2.55945 20.5579 3.43426C21.4327 4.30907 20.8874 5.95922 19.7968 9.25953L17.8595 15.1218C16.6236 18.8619 16.0056 20.7319 14.8796 20.9603C14.6411 21.0087 14.3955 21.0129 14.1549 20.9727C13.019 20.7832 12.3132 18.9359 10.9016 15.2413C10.6328 14.5376 10.4983 14.1858 10.2574 13.9127C10.2018 13.8497 10.1424 13.7903 10.0795 13.7348C9.80638 13.4938 9.45455 13.3594 8.75089 13.0906C5.05627 11.679 3.20896 10.9732 3.01945 9.83727C2.97931 9.59669 2.98353 9.35108 3.03189 9.11259C3.26025 7.98657 5.13029 7.36859 8.87038 6.13264Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.8008 11.1865L15.498 8.48926"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `send-message.svg` */
    val SendMessage: ImageVector by lazy {
        ImageVector.Builder(
            name = "SendMessage",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.325 5.33455L16.1084 8.24495C19.3643 9.88342 20.9922 10.7027 20.9922 12C20.9922 13.2973 19.3643 14.1166 16.1084 15.7551L10.325 18.6655C6.63532 20.5223 4.79046 21.4507 3.7862 20.7851C3.57349 20.6441 3.38825 20.4651 3.23962 20.2569C2.53788 19.2741 3.3843 17.381 5.07715 13.5948C5.39957 12.8736 5.56078 12.5131 5.58462 12.1319C5.59011 12.044 5.59011 11.956 5.58462 11.8681C5.56078 11.4869 5.39957 11.1264 5.07715 10.4052C3.3843 6.61898 2.53788 4.72586 3.23962 3.74307C3.38825 3.53492 3.57349 3.35593 3.7862 3.21495C4.79046 2.54933 6.63532 3.47774 10.325 5.33455Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.49219 12H13.4922"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `silent-sos.svg` */
    val SilentSos: ImageVector by lazy {
        ImageVector.Builder(
            name = "SilentSos",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15.5 18.5C15.5 20.433 13.933 22 12 22C10.067 22 8.5 20.433 8.5 18.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19 11.5V12.7558C19 13.5514 19.3161 14.3145 19.8787 14.8771L20.4819 15.4803C20.8136 15.8121 21 16.262 21 16.7311C21 17.708 20.208 18.5 19.2311 18.5H4.76887C3.79195 18.5 3 17.708 3 16.7311C3 16.262 3.18636 15.8121 3.51809 15.4803L4.12132 14.8771C4.68393 14.3145 5 13.5514 5 12.7558V10C5 6.13401 8.13401 3 12 3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 3.5L19.5 7.5M21 5.5C21 3.567 19.433 2 17.5 2C15.567 2 14 3.567 14 5.5C14 7.433 15.567 9 17.5 9C19.433 9 21 7.433 21 5.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `sim-and-sms.svg` */
    val SimAndSms: ImageVector by lazy {
        ImageVector.Builder(
            name = "SimAndSms",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.9881 7C16.9474 4.98489 16.7678 3.81809 15.9749 3.02513C14.9497 2 13.2998 2 10 2C6.70017 2 5.05025 2 4.02513 3.02513C3 4.05025 3 5.70017 3 9V15C3 18.2998 3 19.9497 4.02513 20.9749C5.05025 22 6.70017 22 10 22C13.2998 22 14.9497 22 15.9749 20.9749C17 19.9497 17 18.2998 17 15L16.9881 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 7H10C9.06812 7 8.60218 7 8.23463 7.15224C7.74458 7.35523 7.35523 7.74458 7.15224 8.23463C7 8.60218 7 9.06812 7 10C7 10.9319 7 11.3978 7.15224 11.7654C7.35523 12.2554 7.74458 12.6448 8.23463 12.8478C8.60218 13 9.06812 13 10 13V15L13 13H18C18.9319 13 19.3978 13 19.7654 12.8478C20.2554 12.6448 20.6448 12.2554 20.8478 11.7654C21 11.3978 21 10.9319 21 10C21 9.06812 21 8.60218 20.8478 8.23463C20.6448 7.74458 20.2554 7.35523 19.7654 7.15224C19.3978 7 18.9319 7 18 7Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.125 19H10M10.25 19C10.25 19.1381 10.1381 19.25 10 19.25C9.86193 19.25 9.75 19.1381 9.75 19C9.75 18.8619 9.86193 18.75 10 18.75C10.1381 18.75 10.25 18.8619 10.25 19Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.125 10H14M14.25 10C14.25 10.1381 14.1381 10.25 14 10.25C13.8619 10.25 13.75 10.1381 13.75 10C13.75 9.86193 13.8619 9.75 14 9.75C14.1381 9.75 14.25 9.86193 14.25 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.625 10.0007H10.5M10.75 10.0007C10.75 10.1388 10.6381 10.2507 10.5 10.2507C10.3619 10.2507 10.25 10.1388 10.25 10.0007C10.25 9.86266 10.3619 9.75073 10.5 9.75073C10.6381 9.75073 10.75 9.86266 10.75 10.0007Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.625 10H17.5M17.75 10C17.75 10.1381 17.6381 10.25 17.5 10.25C17.3619 10.25 17.25 10.1381 17.25 10C17.25 9.86193 17.3619 9.75 17.5 9.75C17.6381 9.75 17.75 9.86193 17.75 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `sms-feedback-alert.svg` */
    val SmsFeedbackAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "SmsFeedbackAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.9881 7C16.9474 4.98489 16.7678 3.81809 15.9749 3.02513C14.9497 2 13.2998 2 10 2C6.70017 2 5.05025 2 4.02513 3.02513C3 4.05025 3 5.70017 3 9V15C3 18.2998 3 19.9497 4.02513 20.9749C5.05025 22 6.70017 22 10 22C13.2998 22 14.9497 22 15.9749 20.9749C17 19.9497 17 18.2998 17 15L16.9881 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 7H10C9.06812 7 8.60218 7 8.23463 7.15224C7.74458 7.35523 7.35523 7.74458 7.15224 8.23463C7 8.60218 7 9.06812 7 10C7 10.9319 7 11.3978 7.15224 11.7654C7.35523 12.2554 7.74458 12.6448 8.23463 12.8478C8.60218 13 9.06812 13 10 13V15L13 13H18C18.9319 13 19.3978 13 19.7654 12.8478C20.2554 12.6448 20.6448 12.2554 20.8478 11.7654C21 11.3978 21 10.9319 21 10C21 9.06812 21 8.60218 20.8478 8.23463C20.6448 7.74458 20.2554 7.35523 19.7654 7.15224C19.3978 7 18.9319 7 18 7Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.125 19H10M10.25 19C10.25 19.1381 10.1381 19.25 10 19.25C9.86193 19.25 9.75 19.1381 9.75 19C9.75 18.8619 9.86193 18.75 10 18.75C10.1381 18.75 10.25 18.8619 10.25 19Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.125 10H14M14.25 10C14.25 10.1381 14.1381 10.25 14 10.25C13.8619 10.25 13.75 10.1381 13.75 10C13.75 9.86193 13.8619 9.75 14 9.75C14.1381 9.75 14.25 9.86193 14.25 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.625 10.0007H10.5M10.75 10.0007C10.75 10.1388 10.6381 10.2507 10.5 10.2507C10.3619 10.2507 10.25 10.1388 10.25 10.0007C10.25 9.86266 10.3619 9.75073 10.5 9.75073C10.6381 9.75073 10.75 9.86266 10.75 10.0007Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.625 10H17.5M17.75 10C17.75 10.1381 17.6381 10.25 17.5 10.25C17.3619 10.25 17.25 10.1381 17.25 10C17.25 9.86193 17.3619 9.75 17.5 9.75C17.6381 9.75 17.75 9.86193 17.75 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `spirals.svg` */
    val Spirals: ImageVector by lazy {
        ImageVector.Builder(
            name = "Spirals",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11.9532 2.00004C17.5019 2.00004 22 6.47719 22 12C22 17.5229 17.5019 22 11.9532 22C-0.631103 22 -1.82658 4.01759 11.4985 5.00004C14.8499 5.24714 18.0289 8.41019 18.0289 12C18.0289 16.5 15.2348 18.5 11.4985 18.5C4.5 18.5 3.19042 8.46695 11.0021 9.00004C12.508 9.1028 14.0162 10.3432 14.0162 12C14.0162 13.9279 13 15 11.1211 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `square-check-big.svg` */
    val SquareCheckBig: ImageVector by lazy {
        ImageVector.Builder(
            name = "SquareCheckBig",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M14.9922 2.5H11.9922C7.51384 2.5 5.27467 2.5 3.88343 3.89124C2.49219 5.28249 2.49219 7.52166 2.49219 12C2.49219 16.4783 2.49219 18.7175 3.88343 20.1088C5.27467 21.5 7.51384 21.5 11.9922 21.5C16.4705 21.5 18.7097 21.5 20.1009 20.1088C21.4922 18.7175 21.4922 16.4783 21.4922 12V10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8.49219 10L11.9922 13.5L20.9924 3.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `star.svg` */
    val Star: ImageVector by lazy {
        ImageVector.Builder(
            name = "Star",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.7276 3.44418L15.4874 6.99288C15.7274 7.48687 16.3673 7.9607 16.9073 8.05143L20.0969 8.58575C22.1367 8.92853 22.6167 10.4206 21.1468 11.8925L18.6671 14.3927C18.2471 14.8161 18.0172 15.6327 18.1471 16.2175L18.8571 19.3125C19.417 21.7623 18.1271 22.71 15.9774 21.4296L12.9877 19.6452C12.4478 19.3226 11.5579 19.3226 11.0079 19.6452L8.01827 21.4296C5.8785 22.71 4.57865 21.7522 5.13859 19.3125L5.84851 16.2175C5.97849 15.6327 5.74852 14.8161 5.32856 14.3927L2.84884 11.8925C1.389 10.4206 1.85895 8.92853 3.89872 8.58575L7.08837 8.05143C7.61831 7.9607 8.25824 7.48687 8.49821 6.99288L10.258 3.44418C11.2179 1.51861 12.7777 1.51861 13.7276 3.44418Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `stop.svg` */
    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4 12C4 8.72077 4 7.08116 4.81382 5.91891C5.1149 5.48891 5.48891 5.1149 5.91891 4.81382C7.08116 4 8.72077 4 12 4C15.2792 4 16.9188 4 18.0811 4.81382C18.5111 5.1149 18.8851 5.48891 19.1862 5.91891C20 7.08116 20 8.72077 20 12C20 15.2792 20 16.9188 19.1862 18.0811C18.8851 18.5111 18.5111 18.8851 18.0811 19.1862C16.9188 20 15.2792 20 12 20C8.72077 20 7.08116 20 5.91891 19.1862C5.48891 18.8851 5.1149 18.5111 4.81382 18.0811C4 16.9188 4 15.2792 4 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `telemetry.svg` */
    val Telemetry: ImageVector by lazy {
        ImageVector.Builder(
            name = "Telemetry",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16 3.38462V2M19.6306 4.36369L20.6081 3.38462M20.6176 8H22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22 12C22 16.714 22 19.0711 20.5355 20.5355C19.0711 22 16.714 22 12 22C7.28595 22 4.92893 22 3.46447 20.5355C2 19.0711 2 16.714 2 12C2 7.28595 2 4.92893 3.46447 3.46447C4.92893 2 7.28595 2 12 2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.8517 8.00684H15.0738C15.4527 8.00684 15.7598 8.32175 15.7598 8.71022V12.0354M2.75977 13.9583C5.03301 14.2241 10.7373 13.5137 14.8914 8.88963"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `text-as-well.svg` */
    val TextAsWell: ImageVector by lazy {
        ImageVector.Builder(
            name = "TextAsWell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.448 11C21.4824 11.3286 21.5 11.6623 21.5 12C21.5 17.2467 17.2467 21.5 12 21.5C10.3719 21.5 8.8394 21.0904 7.5 20.3687C5.63177 19.362 4.37462 20.2979 3.26592 20.4658C3.09774 20.4913 2.93024 20.4302 2.80997 20.31C2.62741 20.1274 2.59266 19.8451 2.6935 19.6074C3.12865 18.5818 3.5282 16.6382 2.98341 15C2.6698 14.057 2.5 13.0483 2.5 12C2.5 6.75329 6.75329 2.5 12 2.5C12.3377 2.5 12.6714 2.51762 13 2.552"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.9693 2.52506C17.9761 2.49165 18.0239 2.49165 18.0307 2.52506C18.3852 4.25942 19.7406 5.61481 21.4749 5.9693C21.5084 5.97613 21.5084 6.02387 21.4749 6.0307C19.7406 6.38519 18.3852 7.74058 18.0307 9.47494C18.0239 9.50835 17.9761 9.50835 17.9693 9.47494C17.6148 7.74058 16.2594 6.38519 14.5251 6.0307C14.4916 6.02387 14.4916 5.97613 14.5251 5.9693C16.2594 5.61481 17.6148 4.25942 17.9693 2.52506Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.1257 12H12.0007M8.125 12H8M12.2507 12C12.2507 12.1381 12.1388 12.25 12.0007 12.25C11.8627 12.25 11.7507 12.1381 11.7507 12C11.7507 11.8619 11.8627 11.75 12.0007 11.75C12.1388 11.75 12.2507 11.8619 12.2507 12ZM8.25 12C8.25 12.1381 8.13807 12.25 8 12.25C7.86193 12.25 7.75 12.1381 7.75 12C7.75 11.8619 7.86193 11.75 8 11.75C8.13807 11.75 8.25 11.8619 8.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `thumbs-down.svg` */
    val ThumbsDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ThumbsDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 11.5C2 12.6046 2.89543 13.5 4 13.5C5.65685 13.5 7 12.1569 7 10.5V6.5C7 4.84315 5.65685 3.5 4 3.5C2.89543 3.5 2 4.39543 2 5.5V11.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.4787 16.1937L15.2124 15.3337C14.9942 14.6289 14.8851 14.2765 14.969 13.9982C15.0369 13.7731 15.1859 13.579 15.389 13.4513C15.64 13.2935 16.0197 13.2935 16.7791 13.2935H17.1831C19.7532 13.2935 21.0382 13.2935 21.6452 12.5327C21.7145 12.4458 21.7762 12.3533 21.8296 12.2563C22.2965 11.4079 21.7657 10.2649 20.704 7.9789C19.7297 5.88111 19.2425 4.83222 18.338 4.21485C18.2505 4.15508 18.1605 4.0987 18.0683 4.04586C17.116 3.5 15.9362 3.5 13.5764 3.5H13.0646C10.2057 3.5 8.77628 3.5 7.88814 4.36053C7 5.22106 7 6.60607 7 9.37607V10.3497C7 11.8054 7 12.5332 7.25834 13.1994C7.51668 13.8656 8.01135 14.4134 9.00069 15.5089L13.0921 20.0394C13.1947 20.1531 13.246 20.2099 13.2913 20.2493C13.7135 20.6167 14.3652 20.5754 14.7344 20.1577C14.774 20.1129 14.8172 20.0501 14.9036 19.9245C15.0388 19.728 15.1064 19.6297 15.1654 19.5323C15.6928 18.6609 15.8524 17.6256 15.6108 16.6429C15.5838 16.5331 15.5488 16.4199 15.4787 16.1937Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `thumbs-up.svg` */
    val ThumbsUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ThumbsUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12.5C2 11.3954 2.89543 10.5 4 10.5C5.65685 10.5 7 11.8431 7 13.5V17.5C7 19.1569 5.65685 20.5 4 20.5C2.89543 20.5 2 19.6046 2 18.5V12.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.4787 7.80626L15.2124 8.66634C14.9942 9.37111 14.8851 9.72349 14.969 10.0018C15.0369 10.2269 15.1859 10.421 15.389 10.5487C15.64 10.7065 16.0197 10.7065 16.7791 10.7065H17.1831C19.7532 10.7065 21.0382 10.7065 21.6452 11.4673C21.7145 11.5542 21.7762 11.6467 21.8296 11.7437C22.2965 12.5921 21.7657 13.7351 20.704 16.0211C19.7297 18.1189 19.2425 19.1678 18.338 19.7852C18.2505 19.8449 18.1605 19.9013 18.0683 19.9541C17.116 20.5 15.9362 20.5 13.5764 20.5H13.0646C10.2057 20.5 8.77628 20.5 7.88814 19.6395C7 18.7789 7 17.3939 7 14.6239V13.6503C7 12.1946 7 11.4668 7.25834 10.8006C7.51668 10.1344 8.01135 9.58664 9.00069 8.49112L13.0921 3.96056C13.1947 3.84694 13.246 3.79012 13.2913 3.75075C13.7135 3.38328 14.3652 3.42464 14.7344 3.84235C14.774 3.8871 14.8172 3.94991 14.9036 4.07554C15.0388 4.27205 15.1064 4.37031 15.1654 4.46765C15.6928 5.33913 15.8524 6.37436 15.6108 7.35715C15.5838 7.46692 15.5488 7.5801 15.4787 7.80626Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `tick-02.svg` */
    val Tick02: ImageVector by lazy {
        ImageVector.Builder(
            name = "Tick02",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5 14L8.5 17.5L19 6.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `top-right-settings.svg` */
    val TopRightSettings: ImageVector by lazy {
        ImageVector.Builder(
            name = "TopRightSettings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.3175 7.14139L20.8239 6.28479C20.4506 5.63696 20.264 5.31305 19.9464 5.18388C19.6288 5.05472 19.2696 5.15664 18.5513 5.36048L17.3311 5.70418C16.8725 5.80994 16.3913 5.74994 15.9726 5.53479L15.6357 5.34042C15.2766 5.11043 15.0004 4.77133 14.8475 4.37274L14.5136 3.37536C14.294 2.71534 14.1842 2.38533 13.9228 2.19657C13.6615 2.00781 13.3143 2.00781 12.6199 2.00781H11.5051C10.8108 2.00781 10.4636 2.00781 10.2022 2.19657C9.94085 2.38533 9.83106 2.71534 9.61149 3.37536L9.27753 4.37274C9.12465 4.77133 8.84845 5.11043 8.48937 5.34042L8.15249 5.53479C7.73374 5.74994 7.25259 5.80994 6.79398 5.70418L5.57375 5.36048C4.85541 5.15664 4.49625 5.05472 4.17867 5.18388C3.86109 5.31305 3.67445 5.63696 3.30115 6.28479L2.80757 7.14139C2.45766 7.74864 2.2827 8.05227 2.31666 8.37549C2.35061 8.69871 2.58483 8.95918 3.05326 9.48012L4.0843 10.6328C4.3363 10.9518 4.51521 11.5078 4.51521 12.0077C4.51521 12.5078 4.33636 13.0636 4.08433 13.3827L3.05326 14.5354C2.58483 15.0564 2.35062 15.3168 2.31666 15.6401C2.2827 15.9633 2.45766 16.2669 2.80757 16.8741L3.30114 17.7307C3.67443 18.3785 3.86109 18.7025 4.17867 18.8316C4.49625 18.9608 4.85542 18.8589 5.57377 18.655L6.79394 18.3113C7.25263 18.2055 7.73387 18.2656 8.15267 18.4808L8.4895 18.6752C8.84851 18.9052 9.12464 19.2442 9.2775 19.6428L9.61149 20.6403C9.83106 21.3003 9.94085 21.6303 10.2022 21.8191C10.4636 22.0078 10.8108 22.0078 11.5051 22.0078H12.6199C13.3143 22.0078 13.6615 22.0078 13.9228 21.8191C14.1842 21.6303 14.294 21.3003 14.5136 20.6403L14.8476 19.6428C15.0004 19.2442 15.2765 18.9052 15.6356 18.6752L15.9724 18.4808C16.3912 18.2656 16.8724 18.2055 17.3311 18.3113L18.5513 18.655C19.2696 18.8589 19.6288 18.9608 19.9464 18.8316C20.264 18.7025 20.4506 18.3785 20.8239 17.7307L21.3175 16.8741C21.6674 16.2669 21.8423 15.9633 21.8084 15.6401C21.7744 15.3168 21.5402 15.0564 21.0718 14.5354L20.0407 13.3827C19.7887 13.0636 19.6098 12.5078 19.6098 12.0077C19.6098 11.5078 19.7888 10.9518 20.0407 10.6328L21.0718 9.48012C21.5402 8.95918 21.7744 8.69871 21.8084 8.37549C21.8423 8.05227 21.6674 7.74864 21.3175 7.14139Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5195 12C15.5195 13.933 13.9525 15.5 12.0195 15.5C10.0865 15.5 8.51953 13.933 8.51953 12C8.51953 10.067 10.0865 8.5 12.0195 8.5C13.9525 8.5 15.5195 10.067 15.5195 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `torch.svg` */
    val Torch: ImageVector by lazy {
        ImageVector.Builder(
            name = "Torch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15 3H9C8.05719 3 7.58579 3 7.29289 3.29289C7 3.58579 7 4.05719 7 5V6C7 7.04416 7.27249 8.07025 7.79054 8.97683L9 11V18C9 18.9319 9 19.3978 9.15224 19.7654C9.35523 20.2554 9.74458 20.6448 10.2346 20.8478C10.6022 21 11.0681 21 12 21C12.9319 21 13.3978 21 13.7654 20.8478C14.2554 20.6448 14.6448 20.2554 14.8478 19.7654C15 19.3978 15 18.9319 15 18V11L16.2095 8.97683C16.7275 8.07025 17 7.04416 17 6V5C17 4.05719 17 3.58579 16.7071 3.29289C16.4142 3 15.9428 3 15 3Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 6H17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 13V15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `wave-triangle.svg` */
    val WaveTriangle: ImageVector by lazy {
        ImageVector.Builder(
            name = "WaveTriangle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3.00012 12H7.34073C7.74075 12 8.10229 12.2384 8.25987 12.6061L10.8436 18.6348C10.9386 18.8563 11.1564 19 11.3975 19C11.7303 19 12.0001 18.7302 12.0001 18.3974V5.60262C12.0001 5.2698 12.2699 5 12.6027 5C12.8438 5 13.0617 5.14367 13.1566 5.36526L15.74 11.3939C15.8976 11.7616 16.2591 12 16.6592 12H20.9998"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `wifi-01.svg` */
    val Wifi01: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wifi01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.25 14.5C10.25 12.5 13.75 12.5 15.75 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.5 11.5C14.7324 8.16667 9.5 8.16667 5.5 11.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 8.5C8.31579 3.16669 15.6842 3.16668 22 8.49989"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.5 18 a 1.5 1.5 0 1 0 3 0 a 1.5 1.5 0 1 0 -3 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** Every icon in the set, for the kit gallery. */
    val All: List<Pair<String, ImageVector>> = listOf(
        "3-g-signal" to Signal3G,
        "4-g-signal" to Signal4G,
        "5-g-signal" to Signal5G,
        "account-recovery" to AccountRecovery,
        "activity-01" to Activity01,
        "adaptive-mode" to AdaptiveMode,
        "ai-search-02" to AiSearch02,
        "alert-02" to Alert02,
        "all-bookmark" to AllBookmark,
        "arrow-down-01" to ArrowDown01,
        "arrow-left-01" to ArrowLeft01,
        "arrow-right-01" to ArrowRight01,
        "arrow-up-01" to ArrowUp01,
        "at-sign" to AtSign,
        "badge-alert" to BadgeAlert,
        "ban" to Ban,
        "battery-charging-02" to BatteryCharging02,
        "battery-eco-charging" to BatteryEcoCharging,
        "battery-full" to BatteryFull,
        "battery-low" to BatteryLow,
        "battery-medium-01" to BatteryMedium01,
        "battery-medium-02" to BatteryMedium02,
        "battery-warning" to BatteryWarning,
        "bookmark-02" to Bookmark02,
        "bookmark-block-02" to BookmarkBlock02,
        "call-after-a-fall" to CallAfterAFall,
        "chat-favourite-01" to ChatFavourite01,
        "check-in" to CheckIn,
        "circle-dashed" to CircleDashed,
        "circle" to Circle,
        "cloud-loading" to CloudLoading,
        "cloud-saving-done-01" to CloudSavingDone01,
        "cloud-saving-done-02" to CloudSavingDone02,
        "connect-to-the-device" to ConnectToTheDevice,
        "cross" to Cross,
        "cyber" to Cyber,
        "daily-reminder" to DailyReminder,
        "dashboard-circle-edit" to DashboardCircleEdit,
        "device-settings" to DeviceSettings,
        "emergency-contacts" to EmergencyContacts,
        "fall-detection" to FallDetection,
        "favourite" to Favourite,
        "find-the-device" to FindTheDevice,
        "fire" to Fire,
        "full-signal" to FullSignal,
        "geometric-shapes-01" to GeometricShapes01,
        "gps-disconnected" to GpsDisconnected,
        "gps-no-signal-02" to GpsNoSignal02,
        "gps-signal-01" to GpsSignal01,
        "gps" to Gps,
        "hard-drive-download" to HardDriveDownload,
        "hard-drive-upload" to HardDriveUpload,
        "help-circle" to HelpCircle,
        "help-square" to HelpSquare,
        "info" to Info,
        "information-square" to InformationSquare,
        "internet" to Internet,
        "lights" to Lights,
        "low-signal" to LowSignal,
        "medical-id" to MedicalId,
        "medium-signal" to MediumSignal,
        "message-inbox" to MessageInbox,
        "navbar-board" to NavbarBoard,
        "navbar-circle" to NavbarCircle,
        "navbar-device" to NavbarDevice,
        "navbar-safety" to NavbarSafety,
        "navigation-07" to Navigation07,
        "new-releases" to NewReleases,
        "no-internet" to NoInternet,
        "ocean" to Ocean,
        "paired-devices" to PairedDevices,
        "parental-control" to ParentalControl,
        "pause" to Pause,
        "pin-location" to PinLocation,
        "play" to Play,
        "police" to Police,
        "pulse" to Pulse,
        "quiet-hours" to QuietHours,
        "rainbow" to Rainbow,
        "reminders" to Reminders,
        "repeating-check-in" to RepeatingCheckIn,
        "ring-the-device" to RingTheDevice,
        "safe-zone" to SafeZone,
        "search-01" to Search01,
        "send-message-diagonal" to SendMessageDiagonal,
        "send-message" to SendMessage,
        "silent-sos" to SilentSos,
        "sim-and-sms" to SimAndSms,
        "sms-feedback-alert" to SmsFeedbackAlert,
        "spirals" to Spirals,
        "square-check-big" to SquareCheckBig,
        "star" to Star,
        "stop" to Stop,
        "telemetry" to Telemetry,
        "text-as-well" to TextAsWell,
        "thumbs-down" to ThumbsDown,
        "thumbs-up" to ThumbsUp,
        "tick-02" to Tick02,
        "top-right-settings" to TopRightSettings,
        "torch" to Torch,
        "wave-triangle" to WaveTriangle,
        "wifi-01" to Wifi01,
    )
}
