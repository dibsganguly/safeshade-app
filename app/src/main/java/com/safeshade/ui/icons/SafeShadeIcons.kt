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
 * row's accent exactly as a Material icon does. The one consequence worth
 * knowing is that a glyph drawn in brand colours - `GoogleLogo` - arrives
 * monochrome like every other, because the tint covers the whole painter.
 *
 * Members rather than extension properties. Material declares its icons as
 * extensions so that a very large set stays splittable; this one does not need
 * that, and extensions would cost a separate import line per icon at every call
 * site. `by lazy` still means none is built until it is first drawn.
 *
 * **The whole app is on this set.** Earlier revisions of this file carved out
 * exceptions - first utility chrome, then the mode personas and device shapes
 * in `ui/board/ModeVisuals.kt` and a handful of one-off actions - because the
 * set had no equivalent for them. It does now, down to a walking figure, a
 * bicycle, a paw and a pair of brand marks, so the exceptions are retired and
 * no screen draws a Material icon.
 *
 * Every icon is emitted into a 24-unit viewport whatever box its SVG was drawn
 * in, so a call site can size any two of them against each other and get what
 * it asked for.
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
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                scaleX = 0.86f,
                scaleY = 0.86f,
                translationX = 1.3962f,
                translationY = 1.1898f
            )
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
            clearGroup()
        }.build()
    }

    /** `add-new-place.svg` */
    val AddNewPlace: ImageVector by lazy {
        ImageVector.Builder(
            name = "AddNewPlace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C5.08963 4.09916 8.45834 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C22.9082 13.4393 17.599 17.6389 13.6177 21.367Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 11.8333C9 11.8333 9.875 11.8333 10.75 13.5C10.75 13.5 13.5294 9.33333 16 8.5"
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

    /** `alarm-clock.svg` */
    val AlarmClock: ImageVector by lazy {
        ImageVector.Builder(
            name = "AlarmClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20.5 12.5C20.5 17.1944 16.6944 21 12 21C7.30558 21 3.5 17.1944 3.5 12.5C3.5 7.80558 7.30558 4 12 4C16.6944 4 20.5 7.80558 20.5 12.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.88 18.7031L3.5 21.0031"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.14 18.668L20.5 20.998"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5 3L2 6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22 6L19 3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 8V12.5L14 14.5"
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

    /** `appearance.svg` */
    val Appearance: ImageVector by lazy {
        ImageVector.Builder(
            name = "Appearance",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 8C2 8 6.47715 3 12 3C17.5228 3 22 8 22 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M21.544 13.045C21.848 13.4713 22 13.6845 22 14C22 14.3155 21.848 14.5287 21.544 14.955C20.1779 16.8706 16.6892 21 12 21C7.31078 21 3.8221 16.8706 2.45604 14.955C2.15201 14.5287 2 14.3155 2 14C2 13.6845 2.15201 13.4713 2.45604 13.045C3.8221 11.1294 7.31078 7 12 7C16.6892 7 20.1779 11.1294 21.544 13.045Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15 14C15 12.3431 13.6569 11 12 11C10.3431 11 9 12.3431 9 14C9 15.6569 10.3431 17 12 17C13.6569 17 15 15.6569 15 14Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `apple-logo-outline.svg` */
    val AppleLogoOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppleLogoOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 5.75C12 3.75 13.5 1.75 15.5 1.75C15.5 3.75 14 5.75 12 5.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.5 8.09001C11.9851 8.09001 11.5867 7.92646 11.1414 7.74368C10.5776 7.51225 9.93875 7.25 8.89334 7.25C7.02235 7.25 4 8.74945 4 12.7495C4 17.4016 7.10471 22.25 9.10471 22.25C9.77426 22.25 10.3775 21.9871 10.954 21.7359C11.4815 21.5059 11.9868 21.2857 12.5 21.2857C13.0132 21.2857 13.5185 21.5059 14.046 21.7359C14.6225 21.9871 15.2257 22.25 15.8953 22.25C17.2879 22.25 18.9573 19.8992 20 16.9008C18.3793 16.2202 17.338 14.618 17.338 12.75C17.338 11.121 18.2036 10.0398 19.5 9.25C18.5 7.75 17.0134 7.25 15.9447 7.25C14.8993 7.25 14.2604 7.51225 13.6966 7.74368C13.2514 7.92646 13.0149 8.09001 12.5 8.09001Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `apple-logo.svg` */
    val AppleLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "AppleLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                scaleX = 0.86f,
                scaleY = 0.86f,
                translationX = 1.68f,
                translationY = 1.68f
            )
                // Drawn in a 560.035 x 560.035 box; fitted to the shared 24-unit viewport.
                addGroup(
                    scaleX = 0.0428545f,
                    scaleY = 0.0428545f,
                    translationX = 2.22886f
                )
                    addPath(
                        pathData = addPathNodes(
                            "M380.844 297.529c.787 84.752 74.349 112.955 75.164 113.314-.622 1.988-11.754 40.191-38.756 79.652-23.343 34.117-47.568 68.107-85.731 68.811-37.499.691-49.557-22.236-92.429-22.236-42.859 0-56.256 21.533-91.753 22.928-36.837 1.395-64.889-36.891-88.424-70.883-48.093-69.53-84.846-196.475-35.496-282.165 24.516-42.554 68.328-69.501 115.882-70.192 36.173-.69 70.315 24.336 92.429 24.336 22.1 0 63.59-30.096 107.208-25.676 18.26.76 69.517 7.376 102.429 55.552-2.652 1.644-61.159 35.704-60.523 106.559M310.369 89.418C329.926 65.745 343.089 32.79 339.498 0 311.308 1.133 277.22 18.785 257 42.445c-18.121 20.952-33.991 54.487-29.709 86.628 31.421 2.431 63.52-15.967 83.078-39.655"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                clearGroup()
            clearGroup()
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

    /** `audio-wave.svg` */
    val AudioWave: ImageVector by lazy {
        ImageVector.Builder(
            name = "AudioWave",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9 3V21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6 7V17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 6V18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15 9L15 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 7L18 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21 11L21 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 11L3 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `backpack.svg` */
    val Backpack: ImageVector by lazy {
        ImageVector.Builder(
            name = "Backpack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19 14H20.2389C21.3498 14 22.1831 15.0805 21.9652 16.2386L21.7003 17.6466C21.4429 19.015 20.3127 20 19 20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5 14H3.76113C2.65015 14 1.81691 15.0805 2.03479 16.2386L2.29967 17.6466C2.55711 19.015 3.68731 20 5 20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M18.2696 10.5L18.7911 15.1967C19.071 18.379 19.211 19.9701 18.2696 20.985C17.3283 22 15.7125 22 12.481 22H11.519C8.2875 22 6.67174 22 5.73038 20.985C4.78901 19.9701 4.92899 18.379 5.20893 15.1967L5.73038 10.4999"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15 5C15 3.34315 13.6569 2 12 2C10.3431 2 9 3.34315 9 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5.2617 8.86971C5.01152 7.45403 4.88643 6.74619 5.13559 6.20431C5.30195 5.84248 5.57803 5.53512 5.9291 5.32087C6.45489 5 7.21577 5 8.73753 5H15.2625C16.7842 5 17.5451 5 18.0709 5.32087C18.422 5.53512 18.698 5.84248 18.8644 6.20431C19.1136 6.74619 18.9885 7.45403 18.7383 8.86971L18.6872 9.15901C18.5902 9.70796 18.5417 9.98243 18.446 10.2349C18.2806 10.671 18.0104 11.0651 17.6565 11.3863C17.4517 11.5722 17.2062 11.7266 16.7153 12.0353C16.2537 12.3255 16.0229 12.4706 15.779 12.5845C15.3579 12.7812 14.905 12.9105 14.439 12.9672C14.169 13 13.8916 13 13.3369 13H10.6631C10.1084 13 9.831 13 9.56102 12.9672C9.09497 12.9105 8.64214 12.7812 8.22104 12.5845C7.9771 12.4706 7.74632 12.3255 7.28474 12.0353C6.79376 11.7266 6.54827 11.5722 6.34346 11.3863C5.98959 11.0651 5.7194 10.671 5.55404 10.2349C5.45833 9.98243 5.40983 9.70796 5.31282 9.15901L5.2617 8.86971Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 10H12M12.25 10C12.25 10.1381 12.1381 10.25 12 10.25C11.8619 10.25 11.75 10.1381 11.75 10C11.75 9.86193 11.8619 9.75 12 9.75C12.1381 9.75 12.25 9.86193 12.25 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `battery-optimisation-off.svg` */
    val BatteryOptimisationOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryOptimisationOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16 16L11.6667 21.6535C11.1282 22.356 10.1188 21.9188 10.1188 20.9829V14.0301C10.1188 13.4695 9.72302 13.015 9.23474 13.015H5.88582C5.12506 13.015 4.71954 11.9851 5.22212 11.3294L7.7741 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 5.72714L12.4969 2.35038C13.0221 1.63999 14.0067 2.08215 14.0067 3.02843V10.059C14.0067 10.6258 14.3928 11.0854 14.8691 11.0854H18.1359C18.878 11.0854 19.2736 12.1268 18.7833 12.7898L17.8885 14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `battery-optimisation-on.svg` */
    val BatteryOptimisationOn: ImageVector by lazy {
        ImageVector.Builder(
            name = "BatteryOptimisationOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5.22576 11.3294L12.224 2.34651C12.7713 1.64397 13.7972 2.08124 13.7972 3.01707V9.96994C13.7972 10.5305 14.1995 10.985 14.6958 10.985H18.0996C18.8729 10.985 19.2851 12.0149 18.7742 12.6706L11.776 21.6535C11.2287 22.356 10.2028 21.9188 10.2028 20.9829V14.0301C10.2028 13.4695 9.80048 13.015 9.3042 13.015H5.90035C5.12711 13.015 4.71494 11.9851 5.22576 11.3294Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `bicycle-01.svg` */
    val Bicycle01: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bicycle01",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6 20.0026C8.20914 20.0026 10 18.2118 10 16.0026C10 13.7935 8.20914 12.0026 6 12.0026C3.79086 12.0026 2 13.7935 2 16.0026C2 18.2118 3.79086 20.0026 6 20.0026Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 20.0026C20.2091 20.0026 22 18.2118 22 16.0026C22 13.7935 20.2091 12.0026 18 12.0026C15.7909 12.0026 14 13.7935 14 16.0026C14 18.2118 15.7909 20.0026 18 20.0026Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6 16.0026H10.3706C10.7302 16.0026 11.0622 15.8095 11.2399 15.4968L15.5 8.00262"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 13.0026L7 7.00262M7 7.00262H5M7 7.00262H9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.0039 6.21868C19.7999 5.64268 19.4399 4.74268 18.2399 4.32268C17.4599 4.02268 15.5399 3.90268 15.2999 4.08268C14.9527 4.16949 14.9399 4.56268 15.1079 5.10268C15.2444 5.68163 15.4559 6.42824 15.6479 7.14268C16.1399 8.97348 17.2199 12.9387 18.0239 15.9987"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `blood-oxygen.svg` */
    val BloodOxygen: ImageVector by lazy {
        ImageVector.Builder(
            name = "BloodOxygen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9 13H9.80031C10.4304 13 10.7454 13 10.9985 13.1493C11.2517 13.2987 11.3926 13.5677 11.6743 14.1056L13.1905 17L15.2857 11L16.8018 13.8944C17.0836 14.4323 17.2245 14.7013 17.4777 14.8507C17.7308 15 18.0458 15 18.6759 15H20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M20.5 12C19.732 8.4154 16.7237 5.03871 14.5241 2.97222C13.1443 1.67593 11.04 1.67593 9.66019 2.97222C7.11961 5.35907 3.5 9.49387 3.5 13.678C3.5 17.7804 6.75366 22 12.0921 22C15.4851 22 18.0359 20.2955 19.4444 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `bluetooth.svg` */
    val Bluetooth: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bluetooth",
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

    /** `body-temperature.svg` */
    val BodyTemperature: ImageVector by lazy {
        ImageVector.Builder(
            name = "BodyTemperature",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 22C14.7614 22 17 19.7614 17 17C17 15.3644 16.2147 13.9122 15.0005 13V5.00049C15.0005 4.06815 15.0005 3.60198 14.8481 3.23428C14.6451 2.74451 14.256 2.35537 13.7662 2.15239C13.3985 2 12.9323 2 12 2C11.0677 2 10.6015 2 10.2338 2.15239C9.74402 2.35537 9.35488 2.74451 9.1519 3.23428C8.99951 3.60198 8.99951 4.06815 8.99951 5.00049V13C7.78534 13.9122 7 15.3644 7 17C7 19.7614 9.23858 22 12 22Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 15C10.8954 15 10 15.8954 10 17C10 18.1046 10.8954 19 12 19C13.1046 19 14 18.1046 14 17C14 15.8954 13.1046 15 12 15ZM12 15V8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `bolt-flash.svg` */
    val BoltFlash: ImageVector by lazy {
        ImageVector.Builder(
            name = "BoltFlash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.62814 12.6736H8.16918C6.68545 12.6736 5.94358 12.6736 5.62736 12.1844C5.31114 11.6953 5.61244 11.0138 6.21504 9.65083L8.02668 5.55323C8.57457 4.314 8.84852 3.69438 9.37997 3.34719C9.91142 3 10.5859 3 11.935 3H14.0244C15.6632 3 16.4826 3 16.7916 3.53535C17.1007 4.0707 16.6942 4.78588 15.8811 6.21623L14.8092 8.10188C14.405 8.81295 14.2029 9.16849 14.2057 9.45952C14.2094 9.83775 14.4105 10.1862 14.7354 10.377C14.9854 10.5239 15.3927 10.5239 16.2074 10.5239C17.2373 10.5239 17.7523 10.5239 18.0205 10.7022C18.3689 10.9338 18.5513 11.3482 18.4874 11.7632C18.4382 12.0826 18.0918 12.4656 17.399 13.2317L11.8639 19.3523C10.7767 20.5545 10.2331 21.1556 9.86807 20.9654C9.50303 20.7751 9.67833 19.9822 10.0289 18.3962L10.7157 15.2896C10.9826 14.082 11.1161 13.4782 10.7951 13.0759C10.4741 12.6736 9.85877 12.6736 8.62814 12.6736Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
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
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                translationY = 2.5f
            )
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
            clearGroup()
        }.build()
    }

    /** `camera-add.svg` */
    val CameraAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "CameraAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M22.0002 8.99998V15C22.0002 17.8284 22.0002 19.2426 21.1215 20.1213C20.2428 21 18.8286 21 16.0002 21H8.00018C5.17176 21 3.75754 21 2.87886 20.1213C2.00018 19.2426 2.00018 17.8284 2.00018 15V11.0537C2.00018 10.0736 2.00018 9.58356 2.1136 9.18288C2.39734 8.18054 3.18074 7.39714 4.18307 7.11341C4.58376 6.99998 5.07379 6.99998 6.05387 6.99998C6.41985 6.99998 6.60284 6.99998 6.77329 6.97027C7.19563 6.89665 7.58313 6.68926 7.87867 6.37869C7.99794 6.25335 8.29718 5.8045 8.50018 5.49998C8.89656 4.90543 9.09474 4.60815 9.36568 4.40365C9.53113 4.27877 9.71499 4.18038 9.91067 4.11198C10.2311 3.99998 10.5884 3.99998 11.303 3.99998H13.0002"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16.0002 13.5C16.0002 15.7091 14.2093 17.5 12.0002 17.5C9.79104 17.5 8.00018 15.7091 8.00018 13.5C8.00018 11.2908 9.79104 9.49998 12.0002 9.49998C14.2093 9.49998 16.0002 11.2908 16.0002 13.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16.0002 5.49998H21.0002M18.5002 7.99998V2.99998"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cane.svg` */
    val Cane: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cane",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5.20209 14.4862L6.63118 19.7143M8.13036 9.51447L9.55945 14.7425M11.2373 5.19627L12.4877 9.77084M14.8801 2.8386L15.7733 6.10615M13.9519 7.28499L6.14314 20.5429C5.60406 21.4581 4.41013 21.7717 3.47641 21.2433C2.5427 20.7149 2.22279 19.5445 2.76187 18.6293L10.5706 5.37138C12.1878 2.62558 15.7696 1.6848 18.5708 3.27008C21.3719 4.85537 22.3316 8.36641 20.7144 11.1122C20.1753 12.0275 18.9814 12.3411 18.0477 11.8126C17.114 11.2842 16.794 10.1139 17.3331 9.1986C17.8722 8.28333 17.5523 7.11299 16.6186 6.58456C15.6849 6.05613 14.4909 6.36972 13.9519 7.28499Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cap.svg` */
    val Cap: ImageVector by lazy {
        ImageVector.Builder(
            name = "Cap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4.5 14L2.34718 15.7223C2.12774 15.8978 2 16.1636 2 16.4446C2 16.7864 2.18686 17.1029 2.49648 17.2477C4.78892 18.3202 8.19601 19 12 19C15.804 19 19.2111 18.3202 21.5035 17.2477C21.8131 17.1029 22 16.7864 22 16.4446C22 16.1636 21.8723 15.8978 21.6528 15.7223L19.5 14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 15C15.0669 15 17.7898 14.6072 19.5 14L19.093 10.3374C18.811 7.79862 18.6699 6.52923 17.8156 5.76462C16.9614 5 15.6842 5 13.1297 5H10.8703C8.31585 5 7.03864 5 6.18436 5.76462C5.33009 6.52923 5.18904 7.79862 4.90695 10.3374L4.5 14C6.21023 14.6072 8.93312 15 12 15Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 10H13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
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

    /** `chat-lock.svg` */
    val ChatLock: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChatLock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 10.5C2 9.72921 2.01346 8.97679 2.03909 8.2503C2.12282 5.87683 2.16469 4.69009 3.13007 3.71745C4.09545 2.74481 5.3157 2.6926 7.7562 2.58819C9.09517 2.5309 10.5209 2.5 12 2.5C13.4791 2.5 14.9048 2.5309 16.2438 2.58819C18.6843 2.6926 19.9046 2.74481 20.8699 3.71745C21.8353 4.69009 21.8772 5.87683 21.9609 8.2503C21.9865 8.97679 22 9.72921 22 10.5C22 11.2708 21.9865 12.0232 21.9609 12.7497C21.8772 15.1232 21.8353 16.3099 20.8699 17.2826C19.9046 18.2552 18.6843 18.3074 16.2437 18.4118C15.5098 18.4432 14.7498 18.4667 13.9693 18.4815C13.2282 18.4955 12.8576 18.5026 12.532 18.6266C12.2064 18.7506 11.9325 18.9855 11.3845 19.4553L9.20503 21.3242C9.07273 21.4376 8.90419 21.5 8.72991 21.5C8.32679 21.5 8 21.1732 8 20.7701V18.4219C7.91842 18.4186 7.83715 18.4153 7.75619 18.4118C5.31569 18.3074 4.09545 18.2552 3.13007 17.2825C2.16469 16.3099 2.12282 15.1232 2.03909 12.7497C2.01346 12.0232 2 11.2708 2 10.5Z"
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

    /** `close-x.svg` */
    val CloseX: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloseX",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18 6L12 12M12 12L6 18M12 12L18 18M12 12L6 6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cloud-backup.svg` */
    val CloudBackup: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudBackup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.4776 9.00005C17.485 9.00002 17.4925 9 17.5 9C19.9853 9 22 11.0147 22 13.5C22 15.2415 21.0108 16.7519 19.5636 17.5M16.9003 11C17.2119 10.3904 17.4131 9.71494 17.4776 9.00005C17.4924 8.83536 17.5 8.66856 17.5 8.5C17.5 5.46243 15.0376 3 12 3C9.12324 3 6.76233 5.20862 6.52042 8.0227M6.52042 8.0227C3.98398 8.26407 2 10.4003 2 13C2 14.9791 3.14985 16.6896 4.81794 17.5M6.52042 8.0227C6.67826 8.00768 6.83823 8 7 8C7.7111 8 8.38754 8.14845 9 8.41604"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.82918 16.1646C7.64394 16.5351 7.79411 16.9856 8.16459 17.1708C8.53507 17.3561 8.98558 17.2059 9.17082 16.8354L8.5 16.5L7.82918 16.1646ZM9.5623 20.1495C9.28821 19.839 8.81425 19.8094 8.5037 20.0835C8.19315 20.3576 8.1636 20.8316 8.4377 21.1421L9 20.6458L9.5623 20.1495ZM8.93511 13.6125C8.99726 13.203 8.71565 12.8206 8.30612 12.7585C7.8966 12.6963 7.51423 12.978 7.45209 13.3875L8.1936 13.5L8.93511 13.6125ZM11.6125 17.5479C12.0221 17.4857 12.3037 17.1034 12.2415 16.6938C12.1794 16.2843 11.797 16.0027 11.3875 16.0649L11.5 16.8064L11.6125 17.5479ZM16 18H16.75C16.75 15.3766 14.6234 13.25 12 13.25V14V14.75C13.7949 14.75 15.25 16.2051 15.25 18H16ZM12 22V22.75C14.6234 22.75 16.75 20.6234 16.75 18H16H15.25C15.25 19.7949 13.7949 21.25 12 21.25V22ZM12 14V13.25C9.5419 13.25 8.35298 15.117 7.82918 16.1646L8.5 16.5L9.17082 16.8354C9.64702 15.883 10.4581 14.75 12 14.75V14ZM9 20.6458L8.4377 21.1421C9.30697 22.127 10.5812 22.75 12 22.75V22V21.25C11.0294 21.25 10.1589 20.8255 9.5623 20.1495L9 20.6458ZM8.19363 16.8064L8.72396 16.2761C8.8172 16.3693 8.81693 16.4411 8.79653 16.3588C8.78192 16.2999 8.76719 16.2017 8.75841 16.0597C8.74099 15.7779 8.75204 15.4163 8.77839 15.0426C8.80439 14.6738 8.84365 14.3152 8.8767 14.0472C8.89317 13.9136 8.90796 13.8038 8.91855 13.7277C8.92385 13.6898 8.92808 13.6603 8.93095 13.6407C8.93239 13.6308 8.93348 13.6235 8.93418 13.6187C8.93454 13.6164 8.93479 13.6146 8.93495 13.6136C8.93503 13.6131 8.93508 13.6127 8.93511 13.6125C8.93512 13.6125 8.93513 13.6124 8.93513 13.6124C8.93513 13.6124 8.93513 13.6124 8.93513 13.6124C8.93512 13.6125 8.93511 13.6125 8.1936 13.5C7.45209 13.3875 7.45208 13.3876 7.45207 13.3876C7.45206 13.3877 7.45204 13.3878 7.45203 13.3879C7.45201 13.388 7.45197 13.3882 7.45193 13.3885C7.45186 13.389 7.45175 13.3897 7.45162 13.3906C7.45136 13.3923 7.451 13.3947 7.45055 13.3978C7.44963 13.4039 7.44833 13.4127 7.44668 13.424C7.44338 13.4467 7.43868 13.4793 7.43292 13.5207C7.42139 13.6033 7.40555 13.7211 7.38798 13.8636C7.35297 14.1474 7.31055 14.5337 7.2821 14.9371C7.25401 15.3355 7.23784 15.7732 7.26126 16.1522C7.27291 16.3406 7.29561 16.5382 7.34056 16.7196C7.37973 16.8777 7.46115 17.1346 7.66329 17.3367L8.19363 16.8064ZM11.5 16.8064C11.3875 16.0649 11.3875 16.0649 11.3876 16.0648C11.3876 16.0648 11.3876 16.0648 11.3876 16.0648C11.3876 16.0648 11.3875 16.0648 11.3874 16.0649C11.3873 16.0649 11.3869 16.0649 11.3864 16.065C11.3853 16.0652 11.3836 16.0654 11.3813 16.0658C11.3765 16.0665 11.3692 16.0676 11.3593 16.069C11.3397 16.0719 11.3102 16.0761 11.2722 16.0814C11.1962 16.092 11.0863 16.1068 10.9528 16.1233C10.6848 16.1563 10.3262 16.1956 9.95741 16.2216C9.58373 16.248 9.22207 16.259 8.9403 16.2416C8.79827 16.2328 8.70012 16.2181 8.64115 16.2035C8.55886 16.1831 8.63072 16.1828 8.72396 16.2761L8.19363 16.8064L7.66329 17.3367C7.86544 17.5389 8.12231 17.6203 8.28039 17.6594C8.46181 17.7044 8.6594 17.7271 8.84777 17.7387C9.22678 17.7622 9.66449 17.746 10.0629 17.7179C10.4663 17.6894 10.8526 17.647 11.1365 17.612C11.2789 17.5944 11.3967 17.5786 11.4794 17.5671C11.5207 17.5613 11.5534 17.5566 11.576 17.5533C11.5873 17.5516 11.5961 17.5503 11.6022 17.5494C11.6053 17.549 11.6077 17.5486 11.6094 17.5483C11.6103 17.5482 11.611 17.5481 11.6115 17.548C11.6118 17.548 11.612 17.548 11.6122 17.5479C11.6122 17.5479 11.6123 17.5479 11.6124 17.5479C11.6125 17.5479 11.6125 17.5479 11.5 16.8064Z"
                ),
                fill = SolidColor(Color.Black)
            )
        }.build()
    }

    /** `cloud-download.svg` */
    val CloudDownload: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudDownload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.4776 9.01106C17.485 9.01102 17.4925 9.01101 17.5 9.01101C19.9853 9.01101 22 11.0294 22 13.5193C22 15.8398 20.25 17.7508 18 18M17.4776 9.01106C17.4924 8.84606 17.5 8.67896 17.5 8.51009C17.5 5.46695 15.0376 3 12 3C9.12324 3 6.76233 5.21267 6.52042 8.03192M17.4776 9.01106C17.3753 10.1476 16.9286 11.1846 16.2428 12.0165M6.52042 8.03192C3.98398 8.27373 2 10.4139 2 13.0183C2 15.4417 3.71776 17.4632 6 17.9273M6.52042 8.03192C6.67826 8.01687 6.83823 8.00917 7 8.00917C8.12582 8.00917 9.16474 8.38194 10.0005 9.01101"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 21L12 13M12 21C11.2998 21 9.99153 19.0057 9.5 18.5M12 21C12.7002 21 14.0085 19.0057 14.5 18.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `cloud-error-alert.svg` */
    val CloudErrorAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudErrorAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 13V17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.125 20.75H12M12.25 20.75C12.25 20.8881 12.1381 21 12 21C11.8619 21 11.75 20.8881 11.75 20.75C11.75 20.6119 11.8619 20.5 12 20.5C12.1381 20.5 12.25 20.6119 12.25 20.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.4776 9.00005C17.485 9.00002 17.4925 9 17.5 9C19.9853 9 22 11.0147 22 13.5C22 15.9853 19.9853 18 17.5 18H16M17.4776 9.00005C17.4924 8.83536 17.5 8.66856 17.5 8.5C17.5 5.46243 15.0376 3 12 3C9.12324 3 6.76233 5.20862 6.52042 8.0227M17.4776 9.00005C17.3753 10.1345 16.9286 11.1696 16.2428 12M6.52042 8.0227C3.98398 8.26407 2 10.4003 2 13C2 15.7614 4.23858 18 7 18H8M6.52042 8.0227C6.67826 8.00768 6.83823 8 7 8C8.12582 8 9.16474 8.37209 10.0005 9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
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

    /** `cloud-off-unavailable.svg` */
    val CloudOffUnavailable: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudOffUnavailable",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.4776 10.5001C17.485 10.5 17.4925 10.5 17.5 10.5C19.9853 10.5 22 12.5147 22 15C22 15.8582 21.7597 16.6604 21.3428 17.3428M17.4776 10.5001C17.4924 10.3354 17.5 10.1686 17.5 10C17.5 6.96243 15.0376 4.5 12 4.5C10.9945 4.5 10.052 4.76982 9.24101 5.24101M17.4776 10.5001C17.4039 11.3178 17.1512 12.0839 16.759 12.759M6.52042 9.5227C3.98398 9.76407 2 11.9003 2 14.5C2 17.2614 4.23858 19.5 7 19.5H17.5C18.0928 19.5 18.6588 19.3854 19.1771 19.1771M6.52042 9.5227C6.67826 9.50768 6.83823 9.5 7 9.5C8.12582 9.5 9.16474 9.87209 10.0005 10.5M6.52042 9.5227C6.59145 8.69641 6.84518 7.92232 7.24101 7.24101"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `cloud-upload.svg` */
    val CloudUpload: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloudUpload",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.4776 9.01106C17.485 9.01102 17.4925 9.01101 17.5 9.01101C19.9853 9.01101 22 11.0294 22 13.5193C22 15.8398 20.25 17.7508 18 18M17.4776 9.01106C17.4924 8.84606 17.5 8.67896 17.5 8.51009C17.5 5.46695 15.0376 3 12 3C9.12324 3 6.76233 5.21267 6.52042 8.03192M17.4776 9.01106C17.3753 10.1476 16.9286 11.1846 16.2428 12.0165M6.52042 8.03192C3.98398 8.27373 2 10.4139 2 13.0183C2 15.4417 3.71776 17.4632 6 17.9273M6.52042 8.03192C6.67826 8.01687 6.83823 8.00917 7 8.00917C8.12582 8.00917 9.16474 8.38194 10.0005 9.01101"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 13L12 21M12 13C11.2998 13 9.99153 14.9943 9.5 15.5M12 13C12.7002 13 14.0085 14.9943 14.5 15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `collar.svg` */
    val Collar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Collar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.058 8.53645L17.058 7.92286C16.0553 7.30762 15.554 7 15 7C14.446 7 13.9447 7.30762 12.942 7.92286L11.942 8.53645C10.9935 9.11848 10.5192 9.40949 10.2596 9.87838C10 10.3473 10 10.9129 10 12.0442V17.9094C10 19.8377 10 20.8019 10.5858 21.4009C11.1716 22 12.1144 22 14 22H16C17.8856 22 18.8284 22 19.4142 21.4009C20 20.8019 20 19.8377 20 17.9094V12.0442C20 10.9129 20 10.3473 19.7404 9.87838C19.4808 9.40949 19.0065 9.11848 18.058 8.53645Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14 7.10809C13.3612 6.4951 12.9791 6.17285 12.4974 6.05178C11.9374 5.91102 11.3491 6.06888 10.1725 6.3846L8.99908 6.69947C7.88602 6.99814 7.32949 7.14748 6.94287 7.5163C6.55624 7.88513 6.40642 8.40961 6.10679 9.45857L4.55327 14.8971C4.0425 16.6852 3.78712 17.5792 4.22063 18.2836C4.59336 18.8892 6.0835 19.6339 7.5 20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.4947 10C15.336 9.44058 16.0828 8.54291 16.5468 7.42653C17.5048 5.12162 16.8944 2.75724 15.1836 2.14554C13.4727 1.53383 11.3091 2.90644 10.3512 5.21135C10.191 5.59667 10.0747 5.98366 10 6.36383"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `cpu-chip.svg` */
    val CpuChip: ImageVector by lazy {
        ImageVector.Builder(
            name = "CpuChip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4 12C4 8.22876 4 6.34315 5.17157 5.17157C6.34315 4 8.22876 4 12 4C15.7712 4 17.6569 4 18.8284 5.17157C20 6.34315 20 8.22876 20 12C20 15.7712 20 17.6569 18.8284 18.8284C17.6569 20 15.7712 20 12 20C8.22876 20 6.34315 20 5.17157 18.8284C4 17.6569 4 15.7712 4 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.73223 16.2678C8.46447 17 9.64298 17 12 17C12.7898 17 13.4473 17 14 16.9724L16.9724 14C17 13.4473 17 12.7898 17 12C17 9.64298 17 8.46447 16.2678 7.73223C15.5355 7 14.357 7 12 7C9.64298 7 8.46447 7 7.73223 7.73223C7 8.46447 7 9.64298 7 12C7 14.357 7 15.5355 7.73223 16.2678Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 2V4M16 2V4M12 2V4M8 20V22M12 20V22M16 20V22M22 16H20M4 8H2M4 16H2M4 12H2M22 8H20M22 12H20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `credit-card.svg` */
    val CreditCard: ImageVector by lazy {
        ImageVector.Builder(
            name = "CreditCard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12C2 8.46252 2 6.69377 3.0528 5.5129C3.22119 5.32403 3.40678 5.14935 3.60746 4.99087C4.86213 4 6.74142 4 10.5 4H13.5C17.2586 4 19.1379 4 20.3925 4.99087C20.5932 5.14935 20.7788 5.32403 20.9472 5.5129C22 6.69377 22 8.46252 22 12C22 15.5375 22 17.3062 20.9472 18.4871C20.7788 18.676 20.5932 18.8506 20.3925 19.0091C19.1379 20 17.2586 20 13.5 20H10.5C6.74142 20 4.86213 20 3.60746 19.0091C3.40678 18.8506 3.22119 18.676 3.0528 18.4871C2 17.3062 2 15.5375 2 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 16H11.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 10.0f
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5 16L18 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 10.0f
            )
            addPath(
                pathData = addPathNodes(
                    "M2 9H22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
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

    /** `crown.svg` */
    val Crown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Crown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5 20.5H19"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16.8717 17.5H7.1283C6.10017 17.5 5.58611 17.5 5.19623 17.2234C4.80634 16.9468 4.63649 16.4616 4.29679 15.4912L2.05123 9.07668C1.93172 8.72325 2.02503 8.3336 2.29225 8.07016C2.62854 7.73864 3.15545 7.6872 3.55117 7.94727L4.78349 8.75718C6.02739 9.5747 6.64935 9.98345 7.27815 9.83488C7.90696 9.68631 8.28019 9.04241 9.02665 7.75461L11.2412 3.93412C11.3968 3.66567 11.6864 3.5 12 3.5C12.3136 3.5 12.6032 3.66567 12.7588 3.93412L14.9733 7.75461C15.7198 9.04241 16.093 9.68631 16.7218 9.83488C17.3507 9.98345 17.9726 9.5747 19.2165 8.75718L20.4488 7.94727C20.8445 7.6872 21.3715 7.73864 21.7078 8.07016C21.975 8.3336 22.0683 8.72325 21.9488 9.07668L19.7032 15.4912C19.3635 16.4616 19.1937 16.9468 18.8038 17.2234C18.4139 17.5 17.8998 17.5 16.8717 17.5Z"
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

    /** `delete-bin.svg` */
    val DeleteBin: ImageVector by lazy {
        ImageVector.Builder(
            name = "DeleteBin",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.5 5.5L18.8803 15.5251C18.7219 18.0864 18.6428 19.3671 18.0008 20.2879C17.6833 20.7431 17.2747 21.1273 16.8007 21.416C15.8421 22 14.559 22 11.9927 22C9.42312 22 8.1383 22 7.17905 21.4149C6.7048 21.1257 6.296 20.7408 5.97868 20.2848C5.33688 19.3626 5.25945 18.0801 5.10461 15.5152L4.5 5.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M3 5.5H21M16.0557 5.5L15.3731 4.09173C14.9196 3.15626 14.6928 2.68852 14.3017 2.39681C14.215 2.3321 14.1231 2.27454 14.027 2.2247C13.5939 2 13.0741 2 12.0345 2C10.9688 2 10.436 2 9.99568 2.23412C9.8981 2.28601 9.80498 2.3459 9.71729 2.41317C9.32164 2.7167 9.10063 3.20155 8.65861 4.17126L8.05292 5.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9.5 16.5L9.5 10.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5 16.5L14.5 10.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `download.svg` */
    val Download: ImageVector by lazy {
        ImageVector.Builder(
            name = "Download",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.0001 12C16.0001 12 13.0542 16 12.0001 16C10.946 16 8.00012 12 8.00012 12M12.0001 15.5L12.0001 3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.0001 8C19.2093 8 21.0001 9.79086 21.0001 12V14.5C21.0001 16.8346 21.0001 18.0019 20.5278 18.8856C20.1549 19.5833 19.5834 20.1547 18.8857 20.5277C18.0021 21 16.8348 21 14.5001 21H9.50052C7.16551 21 5.99801 21 5.11426 20.5275C4.41677 20.1546 3.84547 19.5834 3.47258 18.8859C3.00012 18.0021 3.00012 16.8346 3.00012 14.4996V11.999C3.00067 9.79114 4.78999 8.00125 6.99785 8H7.00012"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `edit.svg` */
    val Edit: ImageVector by lazy {
        ImageVector.Builder(
            name = "Edit",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.4249 4.60509L17.4149 3.6151C18.2351 2.79497 19.5648 2.79497 20.3849 3.6151C21.205 4.43524 21.205 5.76493 20.3849 6.58507L19.3949 7.57506M16.4249 4.60509L9.76558 11.2644C9.25807 11.772 8.89804 12.4078 8.72397 13.1041L8 16L10.8959 15.276C11.5922 15.102 12.228 14.7419 12.7356 14.2344L19.3949 7.57506M16.4249 4.60509L19.3949 7.57506"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.9999 13.5C18.9999 16.7875 18.9999 18.4312 18.092 19.5376C17.9258 19.7401 17.7401 19.9258 17.5375 20.092C16.4312 21 14.7874 21 11.4999 21H11C7.22876 21 5.34316 21 4.17159 19.8284C3.00003 18.6569 3 16.7712 3 13V12.5C3 9.21252 3 7.56879 3.90794 6.46244C4.07417 6.2599 4.2599 6.07417 4.46244 5.90794C5.56879 5 7.21252 5 10.5 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `elderly.svg` */
    val Elderly: ImageVector by lazy {
        ImageVector.Builder(
            name = "Elderly",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 3.125V3.75M12 5C11.3096 5 10.75 4.44035 10.75 3.75C10.75 3.05964 11.3096 2.5 12 2.5C12.6904 2.5 13.25 3.05964 13.25 3.75C13.25 4.44035 12.6904 5 12 5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 12L14.4693 11.2769C13.8596 11.1027 13.3687 10.6498 13.146 10.0561L12.5238 8.39683C12.2087 7.55663 11.4055 7 10.5082 7C10.174 7 9.83974 7.07145 9.56864 7.26687C8.51644 8.02532 7 9.82311 7 12C7 16 8 17 8 21.5M12 8.5C11.1667 9 9.11244 10.1783 9.5 13C9.78716 15.0907 12.5 18 12.5 21.5M11 8C9.83333 8.66667 8 10 8.5 13.5M7.61244 14H9.61244"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 15V21.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `enter-a-place.svg` */
    val EnterAPlace: ImageVector by lazy {
        ImageVector.Builder(
            name = "EnterAPlace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C5.08963 4.09916 8.45834 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C22.9082 13.4393 17.599 17.6389 13.6177 21.367Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 11H12M12 11H8.5M12 11V14.5M12 11L12 7.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `eye-off-hidden.svg` */
    val EyeOffHidden: ImageVector by lazy {
        ImageVector.Builder(
            name = "EyeOffHidden",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.439 15.439C20.3636 14.5212 21.0775 13.6091 21.544 12.955C21.848 12.5287 22 12.3155 22 12C22 11.6845 21.848 11.4713 21.544 11.045C20.1779 9.12944 16.6892 5 12 5C11.0922 5 10.2294 5.15476 9.41827 5.41827M6.74742 6.74742C4.73118 8.1072 3.24215 9.94266 2.45604 11.045C2.15201 11.4713 2 11.6845 2 12C2 12.3155 2.15201 12.5287 2.45604 12.955C3.8221 14.8706 7.31078 19 12 19C13.9908 19 15.7651 18.2557 17.2526 17.2526"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.85786 10C9.32783 10.53 9 11.2623 9 12.0711C9 13.6887 10.3113 15 11.9289 15C12.7377 15 13.47 14.6722 14 14.1421"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M3 3L21 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `eye-on.svg` */
    val EyeOn: ImageVector by lazy {
        ImageVector.Builder(
            name = "EyeOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.544 11.045C21.848 11.4713 22 11.6845 22 12C22 12.3155 21.848 12.5287 21.544 12.955C20.1779 14.8706 16.6892 19 12 19C7.31078 19 3.8221 14.8706 2.45604 12.955C2.15201 12.5287 2 12.3155 2 12C2 11.6845 2.15201 11.4713 2.45604 11.045C3.8221 9.12944 7.31078 5 12 5C16.6892 5 20.1779 9.12944 21.544 11.045Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15 12C15 10.3431 13.6569 9 12 9C10.3431 9 9 10.3431 9 12C9 13.6569 10.3431 15 12 15C13.6569 15 15 13.6569 15 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
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
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                scaleX = 0.88f,
                scaleY = 0.88f,
                translationX = 1.44f,
                translationY = 1.44f
            )
                // Drawn in a 512 x 512 box; fitted to the shared 24-unit viewport.
                addGroup(
                    scaleX = 0.046875f,
                    scaleY = 0.046875f
                )
                    addPath(
                        pathData = addPathNodes(
                            "M271.6826 5.12c17.3429 0 31.3544 14.0115 31.3544 31.3544v9.6023c0 53.4984 -27.3371 102.4896 -71.0372 130.9045l0.1959 0.294 55.6541 78.6799h85.7346c14.7954 0 28.7089 6.9568 37.6253 18.8126l42.3284 56.4379c10.3861 13.8156 7.5446 33.51 -6.2709 43.8962s-33.51 7.5446 -43.8961 -6.2709l-37.6253 -50.167h-95.4349l90.4378 139.723c9.4063 14.5014 5.2911 33.9019 -9.3083 43.4062s-33.902 5.2911 -43.4063 -9.3083L150.6743 249.2923c-2.8415 9.0144 -4.4092 18.6167 -4.4092 28.4149v72.3111c0 17.3429 -14.0115 31.3544 -31.3544 31.3544s-31.3544 -14.0115 -31.3544 -31.3544v-72.3111c0 -63.7866 38.8011 -121.2043 98.0805 -144.9161 35.4696 -14.2074 58.6914 -48.5013 58.6914 -86.7144v-9.6023c0 -17.3429 14.0115 -31.3544 31.3544 -31.3544ZM99.2335 36.4744c36.2049 0 58.833 39.193 40.7306 70.5474 -8.4018 14.5522 -23.9271 23.5165 -40.7306 23.5158 -36.2049 0 -58.833 -39.193 -40.7305 -70.5474 8.4017 -14.5523 23.927 -23.5166 40.7305 -23.5158Z"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                clearGroup()
            clearGroup()
        }.build()
    }

    /** `favourite-place.svg` */
    val FavouritePlace: ImageVector by lazy {
        ImageVector.Builder(
            name = "FavouritePlace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C5.08963 4.09916 8.45834 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C22.9082 13.4393 17.599 17.6389 13.6177 21.367Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9.3881 7.83138C10.3267 7.28308 11.1459 7.50404 11.638 7.856C11.8398 8.00032 11.9406 8.07248 12 8.07248C12.0594 8.07248 12.1602 8.00032 12.362 7.856C12.8541 7.50404 13.6733 7.28308 14.6119 7.83138C15.8437 8.55098 16.1224 10.925 13.2812 12.9278C12.74 13.3093 12.4694 13.5 12 13.5C11.5306 13.5 11.26 13.3093 10.7188 12.9278C7.8776 10.925 8.15632 8.55098 9.3881 7.83138Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `file-export.svg` */
    val FileExport: ImageVector by lazy {
        ImageVector.Builder(
            name = "FileExport",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20 14V10.6569C20 9.83935 20 9.4306 19.8478 9.06306C19.6955 8.69552 19.4065 8.40649 18.8284 7.82843L14.0919 3.09188C13.593 2.593 13.3436 2.34355 13.0345 2.19575C12.9702 2.165 12.9044 2.13772 12.8372 2.11401C12.5141 2 12.1614 2 11.4558 2C8.21082 2 6.58831 2 5.48933 2.88607C5.26731 3.06508 5.06508 3.26731 4.88607 3.48933C4 4.58831 4 6.21082 4 9.45584V14C4 17.7712 4 19.6569 5.17157 20.8284C6.34315 22 8.22876 22 12 22M13 2.5V3C13 5.82843 13 7.24264 13.8787 8.12132C14.7574 9 16.1716 9 19 9H19.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 22C17.6068 21.4102 20 19.8403 20 19C20 18.1597 17.6068 16.5898 17 16M19 19H12"
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

    /** `fire-extinguisher.svg` */
    val FireExtinguisher: ImageVector by lazy {
        ImageVector.Builder(
            name = "FireExtinguisher",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10 18V10C10 7.79086 11.7909 6 14 6C16.2091 6 18 7.79086 18 10V18C18 19.8856 18 20.8284 17.4142 21.4142C16.8284 22 15.8856 22 14 22C12.1144 22 11.1716 22 10.5858 21.4142C10 20.8284 10 19.8856 10 18Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M10 13H18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 6V4C12 3.25231 12 2.87846 12.1608 2.6C12.2661 2.41758 12.4176 2.26609 12.6 2.16077C12.8785 2 13.2523 2 14 2C14.7477 2 15.1215 2 15.4 2.16077C15.5824 2.26609 15.7339 2.41758 15.8392 2.6C16 2.87846 16 3.25231 16 4V6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 3H11C8.23855 3 5.99997 5.23858 5.99997 8V14C5.99997 15.1046 5.10454 16 3.99997 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M16 3H20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `first-aid.svg` */
    val FirstAid: ImageVector by lazy {
        ImageVector.Builder(
            name = "FirstAid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12.5625C2 9.46891 2 7.92211 3.02513 6.96106C4.05025 6 5.70017 6 9 6H15C18.2998 6 19.9497 6 20.9749 6.96106C22 7.92211 22 9.46891 22 12.5625V14.4375C22 17.5311 22 19.0779 20.9749 20.0389C19.9497 21 18.2998 21 15 21H9C5.70017 21 4.05025 21 3.02513 20.0389C2 19.0779 2 17.5311 2 14.4375V12.5625Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 13.5H15M12 10.5L12 16.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 6C17 3.518 16.482 3 14 3H10C7.518 3 7 3.518 7 6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `flask-test-tube.svg` */
    val FlaskTestTube: ImageVector by lazy {
        ImageVector.Builder(
            name = "FlaskTestTube",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8 2H16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16.2187 13.0044L15.9921 13.6151C15.5219 14.65 14.1115 15.7439 11.7609 14.3182C10.2471 13.4001 8.93663 12.6631 7.9997 13.16L7.21891 13.5412"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5323 2V8.56602C14.5323 9.27829 14.5323 9.63442 14.6304 9.97222C14.7285 10.31 14.9197 10.612 15.3021 11.216L17.2861 14.35C19.4275 17.7326 20.4982 19.4238 19.7751 20.7119C19.0519 22 17.0317 22 12.9914 22H11.0086C6.96825 22 4.94807 22 4.22495 20.7119C3.50182 19.4238 4.57251 17.7326 6.71389 14.35L8.69792 11.216C9.08029 10.612 9.27148 10.31 9.36961 9.97222C9.46773 9.63442 9.46773 9.27829 9.46773 8.56602V2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.125 19H15M15.25 19C15.25 19.1381 15.1381 19.25 15 19.25C14.8619 19.25 14.75 19.1381 14.75 19C14.75 18.8619 14.8619 18.75 15 18.75C15.1381 18.75 15.25 18.8619 15.25 19Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.125 17H10M10.25 17C10.25 17.1381 10.1381 17.25 10 17.25C9.86193 17.25 9.75 17.1381 9.75 17C9.75 16.8619 9.86193 16.75 10 16.75C10.1381 16.75 10.25 16.8619 10.25 17Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `folder-add.svg` */
    val FolderAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "FolderAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13 21H12C7.28595 21 4.92893 21 3.46447 19.5355C2 18.0711 2 15.714 2 11V7.94427C2 6.1278 2 5.21956 2.38032 4.53806C2.65142 4.05227 3.05227 3.65142 3.53806 3.38032C4.21956 3 5.1278 3 6.94427 3C8.10802 3 8.6899 3 9.19926 3.19101C10.3622 3.62712 10.8418 4.68358 11.3666 5.73313L12 7M8 7H16.75C18.8567 7 19.91 7 20.6667 7.50559C20.9943 7.72447 21.2755 8.00572 21.4944 8.33329C21.9796 9.05942 21.9992 10.0588 22 12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M18 13V21M22 17H14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `folder-cloud.svg` */
    val FolderCloud: ImageVector by lazy {
        ImageVector.Builder(
            name = "FolderCloud",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.00001 7.00116H16.75C18.8567 7.00116 19.9101 7.00116 20.6667 7.5069C20.9943 7.72584 21.2756 8.00717 21.4944 8.33484C21.9796 9.06117 21.9992 10.0608 22 12.0026V13.0029M12 7.00116L11.3666 5.73392C10.8418 4.68406 10.3622 3.6273 9.19927 3.19106C8.68991 3 8.10803 3 6.94428 3C5.1278 3 4.21957 3 3.53807 3.38043C3.05227 3.65161 2.65142 4.05257 2.38032 4.53851C2 5.22021 2 6.12871 2 7.94571V11.0023C2 15.7177 2 18.0754 3.46447 19.5403C4.70529 20.7815 6.58688 20.9711 10 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M13 18.6667C13 19.9553 14.0074 21 15.25 21H19.975C21.0934 21 22 20.0598 22 18.9C22 17.7402 21.0833 16.8 19.9649 16.8C20.0897 15.3643 18.9799 14 17.5 14C16.2055 14 15.1431 15.0307 15.0342 16.3439C13.8928 16.4566 13 17.4535 13 18.6667Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `folder-favourite.svg` */
    val FolderFavourite: ImageVector by lazy {
        ImageVector.Builder(
            name = "FolderFavourite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12.0027 21C7.28739 21 4.92973 21 3.46487 19.5355C2 18.0711 2 15.714 2 11V7.94427C2 6.1278 2 5.21956 2.38042 4.53806C2.6516 4.05227 3.05255 3.65142 3.53848 3.38032C4.22017 3 5.12865 3 6.94562 3C8.10968 3 8.69172 3 9.20122 3.19101C10.3645 3.62712 10.8442 4.68358 11.3691 5.73313L12.0027 7M8.00163 7H16.754C18.8613 7 19.9149 7 20.6718 7.50559C20.9995 7.72447 21.2808 8.00572 21.4997 8.33329C21.8937 8.92282 21.9808 9.69244 22 11"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M20.586 13.331C18.7898 12.3795 17.5 13.7821 17.5 13.7821C17.5 13.7821 16.2102 12.3795 14.4139 13.331C12.2383 14.4834 12.0821 18.9964 17.5 21C22.9179 18.9964 22.7616 14.4834 20.586 13.331Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `folder-lock.svg` */
    val FolderLock: ImageVector by lazy {
        ImageVector.Builder(
            name = "FolderLock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.9946 10.5C21.9753 9.19244 21.8883 8.42282 21.4944 7.83329C21.2755 7.50572 20.9943 7.22447 20.6667 7.00559C19.91 6.5 18.8567 6.5 16.75 6.5H8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 6.5L11.3666 5.23313C10.8418 4.18358 10.3622 3.12712 9.19926 2.69101C8.6899 2.5 8.10802 2.5 6.94427 2.5C5.1278 2.5 4.21956 2.5 3.53806 2.88032C3.05227 3.15142 2.65142 3.55227 2.38032 4.03806C2 4.71956 2 5.6278 2 7.44427V10.5C2 15.214 2 17.5711 3.46447 19.0355C4.8215 20.3926 6.94493 20.4921 11 20.4994"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M20.5 17V15.5C20.5 14.3954 19.6046 13.5 18.5 13.5C17.3954 13.5 16.5 14.3954 16.5 15.5V17M20.5 17H16.5M20.5 17C21.3284 17 22 17.6716 22 18.5V19.25C22 19.9489 22 20.2984 21.8858 20.574C21.7336 20.9416 21.4416 21.2336 21.074 21.3858C20.7984 21.5 20.4489 21.5 19.75 21.5H17.25C16.5511 21.5 16.2016 21.5 15.926 21.3858C15.5584 21.2336 15.2664 20.9416 15.1142 20.574C15 20.2984 15 19.9489 15 19.25V18.5C15 17.6716 15.6716 17 16.5 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `folder-share.svg` */
    val FolderShare: ImageVector by lazy {
        ImageVector.Builder(
            name = "FolderShare",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.1574 14.171C18.4037 14.6625 18.9124 15 19.4999 15C20.3288 15 21.0007 14.3284 21.0007 13.5C21.0007 12.6716 20.3288 12 19.4999 12C18.6711 12 17.9992 12.6716 17.9992 13.5C17.9992 13.7412 18.0561 13.9691 18.1574 14.171ZM18.1574 14.171L14.8395 15.829M14.8395 15.829C14.5931 15.3375 14.0844 15 13.4969 15C12.668 15 11.9961 15.6716 11.9961 16.5C11.9961 17.3284 12.668 18 13.4969 18C14.0844 18 14.5931 17.6625 14.8395 17.171M14.8395 15.829C14.9407 16.0309 14.9976 16.2588 14.9976 16.5C14.9976 16.7412 14.9407 16.9691 14.8395 17.171M14.8395 17.171L18.1574 18.829M18.1574 18.829C18.0561 19.0309 17.9992 19.2588 17.9992 19.5C17.9992 20.3284 18.6711 21 19.4999 21C20.3288 21 21.0007 20.3284 21.0007 19.5C21.0007 18.6716 20.3288 18 19.4999 18C18.9124 18 18.4037 18.3375 18.1574 18.829Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M13.0244 21H12.0222C7.29769 21 4.93543 21 3.46772 19.5355C2 18.0711 2 15.714 2 11V7.94427C2 6.1278 2 5.21956 2.38116 4.53806C2.65287 4.05227 3.0546 3.65142 3.54148 3.38032C4.22449 3 5.13474 3 6.95525 3C8.12158 3 8.70475 3 9.21524 3.19101C10.3808 3.62712 10.8614 4.68358 11.3874 5.73313L12.0222 7M8.01332 7H16.7827C18.8941 7 19.9498 7 20.7081 7.50559C21.0364 7.72447 21.3183 8.00572 21.5377 8.33329C21.8193 8.75388 21.9444 9.26614 22 10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `footprint-steps-activity.svg` */
    val FootprintStepsActivity: ImageVector by lazy {
        ImageVector.Builder(
            name = "FootprintStepsActivity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.535 10.2187L9.99444 12.4393C9.746 13.4599 9.62179 13.9701 9.24607 14.2823C9.21466 14.3084 9.18215 14.3333 9.14863 14.3568C8.74779 14.6384 8.20467 14.6384 7.11844 14.6384H6.44056C5.71535 14.6384 5.35274 14.6384 5.05823 14.5126C4.7893 14.3978 4.56057 14.2103 4.39963 13.9727C4.22339 13.7125 4.16475 13.3672 4.04748 12.6765L3.5637 9.8272C3.16319 7.46831 4.68869 5.20279 7.08482 4.59797L7.29168 4.54575C8.7774 4.17073 10.2847 5.07654 10.5916 6.52879C10.8492 7.74826 10.8299 9.00715 10.535 10.2187Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.48863 17.5793H8.32119C8.93314 17.5793 9.42923 18.0581 9.42923 18.6487V19.0641C9.42923 20.4096 8.29905 21.5004 6.90491 21.5004C5.51077 21.5004 4.38059 20.4096 4.38059 19.0641V18.6487C4.38059 18.0581 4.87668 17.5793 5.48863 17.5793Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.463 8.25779L14.0036 10.4784C14.252 11.4989 14.3763 12.0092 14.752 12.3214C14.7834 12.3475 14.8159 12.3724 14.8494 12.3959C15.2502 12.6775 15.7934 12.6775 16.8796 12.6775H17.5575C18.2827 12.6775 18.6453 12.6775 18.9398 12.5517C19.2087 12.4368 19.4375 12.2493 19.5984 12.0118C19.7747 11.7516 19.8333 11.4063 19.9506 10.7156L20.4343 7.86626C20.8348 5.50737 19.3094 3.24185 16.9132 2.63703L16.7064 2.58482C15.2206 2.20979 13.7133 3.1156 13.4065 4.56785C13.1488 5.78732 13.1681 7.04621 13.463 8.25779Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.5094 15.6183H15.6769C15.0649 15.6183 14.5688 16.0971 14.5688 16.6877V17.1032C14.5688 18.4487 15.699 19.5394 17.0931 19.5394C18.4873 19.5394 19.6174 18.4487 19.6174 17.1032V16.6877C19.6174 16.0971 19.1214 15.6183 18.5094 15.6183Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `full-screen.svg` */
    val FullScreen: ImageVector by lazy {
        ImageVector.Builder(
            name = "FullScreen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12.5667 7.93408L15.3088 8.03416C15.7163 8.04903 16.0391 8.38374 16.0391 8.79156V11.4064M10.5391 13.4341L15.5828 8.41565"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 17C2 15.1144 2 14.1716 2.58579 13.5858C3.17157 13 4.11438 13 6 13H7C8.88562 13 9.82843 13 10.4142 13.5858C11 14.1716 11 15.1144 11 17V18C11 19.8856 11 20.8284 10.4142 21.4142C9.82843 22 8.88562 22 7 22H6C4.11438 22 3.17157 22 2.58579 21.4142C2 20.8284 2 19.8856 2 18V17Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 8.5V10.5M14 2H10M22 14V10M13.5 22H15.5M2.05986 5.5C2.21387 4.43442 2.51347 3.67903 3.09625 3.09625C3.67903 2.51347 4.43442 2.21387 5.5 2.05986M18.5 2.05986C19.5656 2.21387 20.321 2.51347 20.9037 3.09625C21.4865 3.67903 21.7861 4.43442 21.9401 5.5M21.9401 18.5C21.7861 19.5656 21.4865 20.321 20.9037 20.9037C20.321 21.4865 19.5656 21.7861 18.5 21.9401"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `google-logo-outline.svg` */
    val GoogleLogoOutline: ImageVector by lazy {
        ImageVector.Builder(
            name = "GoogleLogoOutline",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.1891 14.1152C16.4619 16.6256 12.6694 18.8144 9.19027 16.6256C6.37273 14.853 6.27434 11.5944 7.27665 9.62754C8.27897 7.66072 11.3889 5.41688 15.6856 7.99314L18.3308 5.41688C16.7993 3.71784 11.4349 1.40171 6.97039 4.58582C1.66738 8.368 2.56578 14.1917 4.56819 17.2425C5.80223 19.1226 9.30789 22.1457 15.1844 20.5513C19.5527 19.366 21.2253 14.4945 20.9759 10.6802H12.1771V14.2814H15.1844"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `google-logo.svg` */
    val GoogleLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "GoogleLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Optically matched to its neighbours - see OPTICAL in tools/gen_icons.py.
            addGroup(
                scaleX = 0.86f,
                scaleY = 0.86f,
                translationX = 1.68f,
                translationY = 1.68f
            )
                // Drawn in a 262 x 262 box; fitted to the shared 24-unit viewport.
                addGroup(
                    scaleX = 0.0916031f,
                    scaleY = 0.0916031f,
                    translationX = 0.274809f
                )
                    addPath(
                        pathData = addPathNodes(
                            "M255.878 133.451c0-10.734-.871-18.567-2.756-26.69H130.55v48.448h71.947c-1.45 12.04-9.283 30.172-26.69 42.356l-.244 1.622 38.755 30.023 2.685.268c24.659-22.774 38.875-56.282 38.875-96.027"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                    addPath(
                        pathData = addPathNodes(
                            "M130.55 261.1c35.248 0 64.839-11.605 86.453-31.622l-41.196-31.913c-11.024 7.688-25.82 13.055-45.257 13.055-34.523 0-63.824-22.773-74.269-54.25l-1.531.13-40.298 31.187-.527 1.465C35.393 231.798 79.49 261.1 130.55 261.1"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                    addPath(
                        pathData = addPathNodes(
                            "M56.281 156.37c-2.756-8.123-4.351-16.827-4.351-25.82 0-8.994 1.595-17.697 4.206-25.82l-.073-1.73L15.26 71.312l-1.335.635C5.077 89.644 0 109.517 0 130.55s5.077 40.905 13.925 58.602l42.356-32.782"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                    addPath(
                        pathData = addPathNodes(
                            "M130.55 50.479c24.514 0 41.05 10.589 50.479 19.438l36.844-35.974C195.245 12.91 165.798 0 130.55 0 79.49 0 35.393 29.301 13.925 71.947l42.211 32.783c10.59-31.477 39.891-54.251 74.414-54.251"
                        ),
                        fill = SolidColor(Color.Black)
                    )
                clearGroup()
            clearGroup()
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

    /** `hand-pointing-up.svg` */
    val HandPointingUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "HandPointingUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20.5 9.5V14.1667C20.5 16.34 20.5 17.4267 20.1689 18.2918C19.6627 19.6148 18.6207 20.6601 17.3019 21.1679C16.4395 21.5 15.3562 21.5 13.1896 21.5C12.0534 21.5 11.4853 21.5 10.9566 21.3834C10.1499 21.2056 9.40001 20.8294 8.77419 20.2888C8.36398 19.9344 8.02311 19.4785 7.34137 18.5667L4.33738 14.5487C3.8758 13.9314 3.88907 13.0789 4.36965 12.4763C4.99772 11.6888 6.16877 11.6237 6.8797 12.3369L8.5011 13.9634V10.5V6C8.5011 5.17157 9.17267 4.5 10.0011 4.5C10.8295 4.5 11.5011 5.17157 11.5011 6M11.5011 6V4C11.5011 3.17157 12.1727 2.5 13.0011 2.5C13.8295 2.5 14.5011 3.17157 14.5011 4V6M11.5011 6V10.5M14.5011 6C14.5011 5.17157 15.1727 4.5 16.0011 4.5C16.8295 4.5 17.5011 5.17157 17.5011 6V8M14.5011 6V10.5M20.5011 10.5V8C20.5011 7.17157 19.8295 6.5 19.0011 6.5C18.1727 6.5 17.5011 7.17157 17.5011 8M17.5011 8V10.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `heart-with-pulse.svg` */
    val HeartWithPulse: ImageVector by lazy {
        ImageVector.Builder(
            name = "HeartWithPulse",
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
            addPath(
                pathData = addPathNodes(
                    "M20.001 13.0001H16.0288C15.8168 13.0001 15.7107 13.0001 15.619 12.9639C15.5691 12.9442 15.5229 12.9169 15.4821 12.8831C15.4072 12.8209 15.3598 12.7303 15.2649 12.5491C14.9921 12.0278 14.8557 11.7672 14.6597 11.7045C14.5567 11.6716 14.4453 11.6716 14.3422 11.7045C14.1462 11.7672 14.0098 12.0278 13.737 12.5491L13.1172 13.7335C12.6442 14.6372 12.4078 15.089 12.0706 15.0624C11.7335 15.0357 11.578 14.5529 11.267 13.5872L10.8024 12.1447C10.4668 11.1027 10.299 10.5817 9.95039 10.5639C9.60176 10.5462 9.377 11.0472 8.92748 12.0493L8.76073 12.4211C8.63475 12.7019 8.57176 12.8423 8.44652 12.9212C8.32129 13.0001 8.16139 13.0001 7.84158 13.0001H4.00098"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `helmet.svg` */
    val Helmet: ImageVector by lazy {
        ImageVector.Builder(
            name = "Helmet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.5 8.5C18 5 14.9924 3 11.4046 3C6.21058 3 2 7.24151 2 12.4737C2 15.8912 3.79635 18.886 6.48977 20.5523C7.06928 20.9108 7.54664 21 8.22657 21H14.763C16.1727 21 17.3155 19.8807 17.3155 18.5C17.3155 17.1193 16.1727 16 14.763 16C14.3687 16 13.6311 16.1485 13.3534 15.8267C13.2038 15.6533 13.2359 15.4366 13.3 15.0031C13.7388 12.0363 16.2376 11.5 19.4564 11.5C20.2168 11.5 20.9772 10.655 21.5235 9.86188C21.9052 9.30765 22.096 9.03053 21.952 8.76527C21.808 8.5 21.4444 8.5 20.7171 8.5H19.5ZM19.5 8.5H15.0693"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.0078 18.5L14.9988 18.5"
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

    /** `history.svg` */
    val History: ImageVector by lazy {
        ImageVector.Builder(
            name = "History",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3.49902 14.9656C4.72475 18.4791 8.06749 21 11.999 21C16.9696 21 20.999 16.9706 20.999 12C20.999 7.02944 16.9696 3 11.999 3C8.29827 3 4.8984 5.6756 3.68943 8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.999 7V12L14.999 14"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.49751 8.74363C7.49751 8.74363 3.81388 9.3026 3.25487 8.7436C2.69585 8.1846 3.25488 4.50098 3.25488 4.50098"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `hourglass-timer.svg` */
    val HourglassTimer: ImageVector by lazy {
        ImageVector.Builder(
            name = "HourglassTimer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.2014 2H6.79876C5.341 2 4.06202 2.9847 4.0036 4.40355C3.93009 6.18879 5.18564 7.37422 6.50435 8.4871C8.32861 10.0266 9.24075 10.7964 9.33642 11.7708C9.35139 11.9233 9.35139 12.0767 9.33642 12.2292C9.24075 13.2036 8.32862 13.9734 6.50435 15.5129C5.14932 16.6564 3.9263 17.7195 4.0036 19.5964C4.06202 21.0153 5.341 22 6.79876 22L17.2014 22C18.6591 22 19.9381 21.0153 19.9965 19.5964C20.043 18.4668 19.6244 17.342 18.7352 16.56C18.3298 16.2034 17.9089 15.8615 17.4958 15.5129C15.6715 13.9734 14.7594 13.2036 14.6637 12.2292C14.6487 12.0767 14.6487 11.9233 14.6637 11.7708C14.7594 10.7964 15.6715 10.0266 17.4958 8.4871C18.8366 7.35558 20.0729 6.25809 19.9965 4.40355C19.9381 2.9847 18.6591 2 17.2014 2Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 21.6381C9 21.1962 9 20.9752 9.0876 20.7821C9.10151 20.7514 9.11699 20.7214 9.13399 20.6923C9.24101 20.509 9.42211 20.3796 9.78432 20.1208C10.7905 19.4021 11.2935 19.0427 11.8652 19.0045C11.955 18.9985 12.045 18.9985 12.1348 19.0045C12.7065 19.0427 13.2095 19.4021 14.2157 20.1208C14.5779 20.3796 14.759 20.509 14.866 20.6923C14.883 20.7214 14.8985 20.7514 14.9124 20.7821C15 20.9752 15 21.1962 15 21.6381V22H9V21.6381Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `image-add.svg` */
    val ImageAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "ImageAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3 16L7.46967 11.5303C7.80923 11.1908 8.26978 11 8.75 11C9.23022 11 9.69077 11.1908 10.0303 11.5303L14 15.5M15.5 17L14 15.5M21 16L18.5303 13.5303C18.1908 13.1908 17.7302 13 17.25 13C16.7698 13 16.3092 13.1908 15.9697 13.5303L14 15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 2.5C7.77027 2.5 5.6554 2.5 4.25276 3.69797C4.05358 3.86808 3.86808 4.05358 3.69797 4.25276C2.5 5.6554 2.5 7.77027 2.5 12C2.5 16.2297 2.5 18.3446 3.69797 19.7472C3.86808 19.9464 4.05358 20.1319 4.25276 20.302C5.6554 21.5 7.77027 21.5 12 21.5C16.2297 21.5 18.3446 21.5 19.7472 20.302C19.9464 20.1319 20.1319 19.9464 20.302 19.7472C21.5 18.3446 21.5 16.2297 21.5 12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21.5 6H18M18 6H14.5M18 6V2.5M18 6V9.5"
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

    /** `key-password.svg` */
    val KeyPassword: ImageVector by lazy {
        ImageVector.Builder(
            name = "KeyPassword",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15.5 14.5C18.8137 14.5 21.5 11.8137 21.5 8.5C21.5 5.18629 18.8137 2.5 15.5 2.5C12.1863 2.5 9.5 5.18629 9.5 8.5C9.5 9.38041 9.68962 10.2165 10.0303 10.9697L2.5 18.5V21.5H5.5V19.5H7.5V17.5H9.5L13.0303 13.9697C13.7835 14.3104 14.6196 14.5 15.5 14.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.5 6.5L16.5 7.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `kid.svg` */
    val Kid: ImageVector by lazy {
        ImageVector.Builder(
            name = "Kid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.9504 10.8961C19.5049 14.8926 16.1153 18 12 18C7.88465 18 4.49508 14.8926 4.04963 10.8961C3.87943 10.9632 3.69402 11 3.5 11C2.67157 11 2 10.3284 2 9.5C2 8.67157 2.67157 8 3.5 8C3.75626 8 3.99751 8.06426 4.20851 8.17754C5.03332 4.63736 8.20867 2 12 2C15.7913 2 18.9667 4.63736 19.7915 8.17755C20.0025 8.06426 20.2437 8 20.5 8C21.3284 8 22 8.67157 22 9.5C22 10.3284 21.3284 11 20.5 11C20.306 11 20.1206 10.9632 19.9504 10.8961Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 22C18 20.208 17.2144 18.5994 15.9687 17.5M6 22C6 20.208 6.78563 18.5994 8.03126 17.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 2C13 2 14 2.89543 14 4C14 5.10457 13 6 12 6C11.5 6 10.9246 5.81669 10.5 5.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8.375 10H8.25M15.625 10H15.75M8.5 10C8.5 10.1381 8.38807 10.25 8.25 10.25C8.11193 10.25 8 10.1381 8 10C8 9.86193 8.11193 9.75 8.25 9.75C8.38807 9.75 8.5 9.86193 8.5 10ZM15.5 10C15.5 10.1381 15.6119 10.25 15.75 10.25C15.8881 10.25 16 10.1381 16 10C16 9.86193 15.8881 9.75 15.75 9.75C15.6119 9.75 15.5 9.86193 15.5 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 15C12.5523 15 13 14.5523 13 14H11C11 14.5523 11.4477 15 12 15Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `leave-a-place.svg` */
    val LeaveAPlace: ImageVector by lazy {
        ImageVector.Builder(
            name = "LeaveAPlace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C5.08963 4.09916 8.45834 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C22.9082 13.4393 17.599 17.6389 13.6177 21.367Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 11H8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `light-bulb.svg` */
    val LightBulb: ImageVector by lazy {
        ImageVector.Builder(
            name = "LightBulb",
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
                    "M12 11V16"
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

    /** `location-check.svg` */
    val LocationCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "LocationCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C5.08963 4.09916 8.45834 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C22.9082 13.4393 17.599 17.6389 13.6177 21.367Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 11.8333C9 11.8333 9.875 11.8333 10.75 13.5C10.75 13.5 13.5294 9.33333 16 8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `log-sign-in.svg` */
    val LogSignIn: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogSignIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3.00006 7.63576C4.6208 4.29965 8.04185 2 12 2C17.5229 2 22 6.47715 22 12C22 17.5228 17.5229 22 12 22C8.04185 22 4.6208 19.7004 3.00006 16.3642"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 8C11 8 15 10.946 15 12C15 13.0541 11 16 11 16M14.5 12H2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `log-sign-out.svg` */
    val LogSignOut: ImageVector by lazy {
        ImageVector.Builder(
            name = "LogSignOut",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.5 4.40041C16.752 2.9039 14.4815 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C14.4815 22 16.752 21.0961 18.5 19.5996"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18 8C18 8 22 10.946 22 12C22 13.0541 18 16 18 16M21.5 12H9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `lost-pet.svg` */
    val LostPet: ImageVector by lazy {
        ImageVector.Builder(
            name = "LostPet",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.5 17.6461C16.2676 18.9628 14.8763 20.1884 13.6177 21.367C13.1841 21.773 12.6044 22 12.0011 22C11.3978 22 10.8182 21.773 10.3845 21.367C6.41302 17.626 1.09076 13.4469 3.68627 7.37966C4.02067 6.59797 4.46666 5.63512 5 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M7 3.48631C8.46914 2.53477 10.213 2 12.0011 2C15.5439 2 18.9126 4.09916 20.316 7.37966C21.6603 10.5221 20.8796 13.1643 19.2612 15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `loud-environment.svg` */
    val LoudEnvironment: ImageVector by lazy {
        ImageVector.Builder(
            name = "LoudEnvironment",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 9V15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 9H6C5.06812 9 4.60218 9 4.23463 9.15224C3.74458 9.35523 3.35523 9.74458 3.15224 10.2346C3 10.6022 3 11.0681 3 12C3 12.9319 3 13.3978 3.15224 13.7654C3.35523 14.2554 3.74458 14.6448 4.23463 14.8478C4.60218 15 5.06812 15 6 15H7L15.0796 17.4239C16.0291 17.7087 16.5039 17.8512 16.9257 18.1014L16.9459 18.1135C17.3663 18.3663 17.7167 18.7167 18.4177 19.4177L18.5858 19.5858C18.7051 19.7051 18.7647 19.7647 18.831 19.8123C18.9561 19.9021 19.1003 19.9619 19.2523 19.9868C19.3327 20 19.4171 20 19.5858 20C19.9713 20 20.1641 20 20.3196 19.9475C20.6155 19.8477 20.8477 19.6155 20.9475 19.3196C21 19.1641 21 18.9713 21 18.5858V5.41421C21 5.02866 21 4.83589 20.9475 4.68039C20.8477 4.38452 20.6155 4.15225 20.3196 4.05245C20.1641 4 19.9713 4 19.5858 4C19.4171 4 19.3327 4 19.2523 4.0132C19.1003 4.03815 18.9561 4.09787 18.831 4.18771C18.7647 4.23526 18.7051 4.29491 18.5858 4.41421L18.4177 4.5823C17.7167 5.28326 17.3663 5.63374 16.9459 5.88649L16.9257 5.89856C16.5039 6.14884 16.0291 6.29126 15.0796 6.57611L7 9Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 15.5V18.0458C8 19.1251 8.87491 20 9.95416 20C10.6075 20 11.2177 19.6735 11.5801 19.1298L13 17"
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

    /** `mail-send-invite.svg` */
    val MailSendInvite: ImageVector by lazy {
        ImageVector.Builder(
            name = "MailSendInvite",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21.9999 11C21.9999 10.5086 21.9946 10.0172 21.9841 9.52439C21.9188 6.45886 21.8861 4.92609 20.755 3.79066C19.6238 2.65523 18.0496 2.61568 14.9011 2.53657C12.9606 2.48781 11.0392 2.48781 9.09871 2.53656C5.95022 2.61566 4.37597 2.65521 3.24484 3.79065C2.11371 4.92608 2.08103 6.45885 2.01565 9.52438C1.99463 10.5101 1.99464 11.4899 2.01566 12.4756C2.08103 15.5412 2.11372 17.0739 3.24485 18.2094C4.37597 19.3448 5.95022 19.3843 9.09872 19.4634C10.069 19.4878 11.0344 19.5 11.9999 19.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6.99989 7.5L9.94191 9.23943C11.6571 10.2535 12.3427 10.2535 14.0579 9.23943L16.9999 7.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22.0001 21.5003C21.884 18.9758 21.9869 18.0573 20.3438 16.8793C19.5362 16.3003 17.9115 15.9188 15.7177 16.1248M17.4519 13.5928L15.1552 15.7464C14.9611 15.9406 14.9597 16.2561 15.1519 16.4521L17.4519 18.6401"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `mail-sign-in.svg` */
    val MailSignIn: ImageVector by lazy {
        ImageVector.Builder(
            name = "MailSignIn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 7.5L9.94202 9.23943C11.6572 10.2535 12.3428 10.2535 14.058 9.23943L17 7.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21.996 10C21.9933 9.63328 21.9894 9.77017 21.9842 9.5265C21.9189 6.46005 21.8862 4.92682 20.7551 3.79105C19.6239 2.65528 18.0497 2.61571 14.9012 2.53658C12.9607 2.48781 11.0393 2.48781 9.09882 2.53657C5.95033 2.6157 4.37608 2.65526 3.24495 3.79103C2.11382 4.92681 2.08114 6.46003 2.01576 9.52648C1.99474 10.5125 1.99475 11.4926 2.01577 12.4786C2.08114 15.5451 2.11383 17.0783 3.24496 18.2141C4.37608 19.3498 5.95033 19.3894 9.09883 19.4685C9.7068 19.4838 10.4957 19.4943 11 19.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.586 18.6482C14.9572 19.0167 13.3086 19.7693 14.3127 20.711C14.8032 21.171 15.3495 21.5 16.0364 21.5H19.9556C20.6424 21.5 21.1887 21.171 21.6792 20.711C22.6834 19.7693 21.0347 19.0167 20.4059 18.6482C18.9314 17.7839 17.0605 17.7839 15.586 18.6482Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19.996 14C19.996 15.1046 19.1005 16 17.996 16C16.8914 16 15.996 15.1046 15.996 14C15.996 12.8954 16.8914 12 17.996 12C19.1005 12 19.996 12.8954 19.996 14Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `mail-sync-recovery.svg` */
    val MailSyncRecovery: ImageVector by lazy {
        ImageVector.Builder(
            name = "MailSyncRecovery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 8L9.94202 9.73943C11.6572 10.7535 12.3428 10.7535 14.058 9.73943L17 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22 10.0262C21.9346 6.95987 21.9019 5.42671 20.7698 4.29099C19.6378 3.15527 18.0623 3.11571 14.9114 3.03658C12.9693 2.98781 11.0464 2.98781 9.10442 3.03657C5.95344 3.11569 4.37796 3.15525 3.24593 4.29098C2.11391 5.4267 2.0812 6.95986 2.01578 10.0262C1.99474 11.0121 1.99474 11.9922 2.01578 12.9782C2.0812 16.0445 2.11392 17.5777 3.24594 18.7134C4.37796 19.8491 5.95345 19.8887 9.10443 19.9678C9.74025 19.9837 10.374 19.9945 11.0071 20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21.6632 15.9994C21.1015 14.8169 19.8962 13.9994 18.5 13.9994C16.567 13.9994 15 15.5664 15 17.4994C15 19.4323 16.567 20.9994 18.5 20.9994C20.0853 20.9994 21.5695 19.9454 22 18.5M21.6632 15.9994V13.5M21.6632 15.9994L19.4277 16.1574"
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

    /** `microphone-off.svg` */
    val MicrophoneOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "MicrophoneOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.1572 4.1572C8.94761 2.86349 10.373 2 12 2C14.4853 2 16.5 4.01472 16.5 6.5V11.5C16.5 11.8111 16.4684 12.1149 16.4083 12.4083M7.5 7.5V11.5C7.5 13.9853 9.51472 16 12 16C13.1154 16 14.136 15.5942 14.9222 14.9222"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 19H11.5828C8.07267 19 5.07706 16.4623 4.5 13M12 19H12.4172C14.2325 19 15.9102 18.3213 17.1869 17.1869M12 19V22M19.5 13C19.3878 13.6733 19.1841 14.3116 18.903 14.903"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `microphone.svg` */
    val Microphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Microphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M7 6.5C7 4.01472 9.01472 2 11.5 2C13.9853 2 16 4.01472 16 6.5V11.5C16 13.9853 13.9853 16 11.5 16C9.01472 16 7 13.9853 7 11.5V6.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.5 19H11.0828C7.57267 19 4.57706 16.4623 4 13M11.5 19H11.9172C15.4273 19 18.4229 16.4623 19 13M11.5 19V22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `moon.svg` */
    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Moon",
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

    /** `necklace.svg` */
    val Necklace: ImageVector by lazy {
        ImageVector.Builder(
            name = "Necklace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.542 12.0002L10.9325 8.87621C11.0235 8.14822 11.3545 8.00024 12.042 8.00024C12.7295 8.00024 13.0605 8.14822 13.1515 8.87621L13.542 12.0002"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8.2844 12.6118C9.6196 11.8003 10.785 12.1273 11.485 12.6482C11.7721 12.8618 11.9156 12.9685 12 12.9685C12.0845 12.9685 12.228 12.8618 12.5151 12.6482C13.2151 12.1273 14.3805 11.8003 15.7157 12.6118C17.468 13.6767 17.8645 17.1898 13.8226 20.1538C13.0527 20.7183 12.6678 21.0005 12 21.0005C11.3323 21.0005 10.9474 20.7183 10.1775 20.1538C6.13558 17.1898 6.53208 13.6767 8.2844 12.6118Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2.0059 3.00024C1.90863 4.57792 2.97686 8.0433 8.05347 9.66205M21.9941 3.00024C22.0914 4.57792 21.0231 8.0433 15.9465 9.66205"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `network-nodes.svg` */
    val NetworkNodes: ImageVector by lazy {
        ImageVector.Builder(
            name = "NetworkNodes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11 5L18 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 10L14.5 14.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5 11L5 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 6.44444 a 4.44444 4.44444 0 1 0 8.88888 0 a 4.44444 4.44444 0 1 0 -8.88888 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M3 20 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M14 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M18 5 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `nfc.svg` */
    val Nfc: ImageVector by lazy {
        ImageVector.Builder(
            name = "Nfc",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15.5 3C17.2934 4.79648 18.5 8.15423 18.5 12C18.5 15.8458 17.2934 19.2035 15.5 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.5 5C13.6956 6.39726 14.5 9.00885 14.5 12C14.5 14.9912 13.6956 17.6027 12.5 19"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 7C9.8967 7.99804 10.5 9.86346 10.5 12C10.5 14.1365 9.8967 16.002 9 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.5 9C6.0978 9.59883 6.5 10.7181 6.5 12C6.5 13.2819 6.0978 14.4012 5.5 15"
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

    /** `notification-bell-do-not-disturb.svg` */
    val NotificationBellDoNotDisturb: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationBellDoNotDisturb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
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
                    "M8 3.12589C8.88491 2.48978 9.95074 2.08864 11.1071 2.01285C11.3031 2 11.5353 2 11.9995 2C12.4638 2 12.696 2 12.892 2.01285C15.8965 2.20977 18.2898 4.60304 18.4867 7.60758C18.4995 7.80358 18.4995 8.03572 18.4995 8.5V9.8056C18.4995 10.5353 18.4995 10.9002 18.5356 11.254C18.6477 12.354 19.0187 13.4119 19.6184 14.3409C19.8113 14.6397 20.0392 14.9246 20.4951 15.4944L20.6648 15.7066C20.7508 15.814 20.8286 15.9113 20.8986 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M18 18H4.43654C3.70878 18 3.3449 18 3.13431 17.8951C2.72509 17.6913 2.50621 17.2359 2.60268 16.789C2.65233 16.559 2.87964 16.2749 3.33427 15.7066L3.50401 15.4944C3.95985 14.9246 4.18779 14.6397 4.38067 14.3409C4.98035 13.4119 5.35143 12.354 5.46349 11.254C5.49954 10.9002 5.49954 10.5353 5.49954 9.8056V8.5C5.49954 8.03572 5.49954 7.80358 5.51239 7.60758C5.54913 7.04701 5.66232 6.50772 5.84168 6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `notification-bell.svg` */
    val NotificationBell: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationBell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
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

    /** `padlock-locked.svg` */
    val PadlockLocked: ImageVector by lazy {
        ImageVector.Builder(
            name = "PadlockLocked",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.4964 9V6.5C16.4964 4.01472 14.4817 2 11.9964 2C9.51112 2 7.4964 4.01472 7.4964 6.5V9"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.4958 9H10.4964C8.16158 9 6.99417 9 6.11049 9.47237C5.41275 9.84535 4.84128 10.4169 4.46837 11.1146C3.99608 11.9984 3.99619 13.1658 3.99641 15.5006C3.99662 17.835 3.99673 19.0023 4.46907 19.8858C4.84203 20.5835 5.41347 21.1548 6.11115 21.5277C6.99475 22 8.16197 22 10.4964 22H13.4958C15.8304 22 16.9978 22 17.8814 21.5277C18.5791 21.1548 19.1506 20.5833 19.5235 19.8856C19.9958 19.0019 19.9958 17.8346 19.9958 15.5C19.9958 13.1654 19.9958 11.9981 19.5235 11.1144C19.1506 10.4167 18.5791 9.84525 17.8814 9.47231C16.9978 9 15.8304 9 13.4958 9Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9.9964 15.5 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `padlock-unlocked.svg` */
    val PadlockUnlocked: ImageVector by lazy {
        ImageVector.Builder(
            name = "PadlockUnlocked",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.9961 15.5 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M7.49609 9V6.5C7.49609 4.01472 9.51081 2 11.9961 2C13.9554 2 15.3783 3.25221 15.9961 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.4955 9H10.4961C8.16128 9 6.99386 9 6.11018 9.47237C5.41244 9.84535 4.84098 10.4169 4.46807 11.1146C3.99578 11.9984 3.99589 13.1658 3.9961 15.5006C3.99632 17.835 3.99643 19.0023 4.46877 19.8858C4.84172 20.5835 5.41317 21.1548 6.11085 21.5277C6.99445 22 8.16166 22 10.4961 22H13.4955C15.8301 22 16.9974 22 17.8811 21.5277C18.5788 21.1548 19.1503 20.5833 19.5232 19.8856C19.9955 19.0019 19.9955 17.8346 19.9955 15.5C19.9955 13.1654 19.9955 11.9981 19.5232 11.1144C19.1503 10.4167 18.5788 9.84525 17.8811 9.47231C16.9974 9 15.8301 9 13.4955 9Z"
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

    /** `paw.svg` */
    val Paw: ImageVector by lazy {
        ImageVector.Builder(
            name = "Paw",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6.49219 6 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addGroup(
                scaleX = -1f,
                translationX = 17.4922f,
                translationY = 4f
            )
                addPath(
                    pathData = addPathNodes(
                        "M0 2 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                    ),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter
                )
            clearGroup()
            addPath(
                pathData = addPathNodes(
                    "M1.99219 11 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addGroup(
                scaleX = -1f,
                translationX = 21.9922f,
                translationY = 9f
            )
                addPath(
                    pathData = addPathNodes(
                        "M0 2 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                    ),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.5f,
                    strokeLineCap = StrokeCap.Butt,
                    strokeLineJoin = StrokeJoin.Miter
                )
            clearGroup()
            addPath(
                pathData = addPathNodes(
                    "M7.31094 20C6.65469 20 6.10417 19.7604 5.65938 19.2813C5.21458 18.8021 4.99219 18.2361 4.99219 17.5833C4.99219 16.8611 5.25104 16.2292 5.76875 15.6875C6.28646 15.1458 6.80052 14.6111 7.31094 14.0833C7.73385 13.6528 8.09844 13.184 8.40469 12.6771C8.71094 12.1701 9.07552 11.6944 9.49844 11.25C9.81927 10.8889 10.1911 10.5903 10.6141 10.3542C11.037 10.1181 11.4964 10 11.9922 10C12.488 10 12.9474 10.1111 13.3703 10.3333C13.7932 10.5556 14.1651 10.8472 14.4859 11.2083C14.8943 11.6528 15.2552 12.1319 15.5688 12.6458C15.8823 13.1597 16.2505 13.6389 16.6734 14.0833C17.1839 14.6111 17.6979 15.1458 18.2156 15.6875C18.7333 16.2292 18.9922 16.8611 18.9922 17.5833C18.9922 18.2361 18.7698 18.8021 18.325 19.2813C17.8802 19.7604 17.3297 20 16.6734 20C15.8859 20 15.1057 19.9375 14.3328 19.8125C13.5599 19.6875 12.7797 19.625 11.9922 19.625C11.2047 19.625 10.4245 19.6875 9.65156 19.8125C8.87865 19.9375 8.09844 20 7.31094 20Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `pdf.svg` */
    val Pdf: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pdf",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20 13V10.6569C20 9.83935 20 9.4306 19.8478 9.06306C19.6955 8.69552 19.4065 8.40649 18.8284 7.82843L14.0919 3.09188C13.593 2.593 13.3436 2.34355 13.0345 2.19575C12.9702 2.165 12.9044 2.13772 12.8372 2.11401C12.5141 2 12.1614 2 11.4558 2C8.21082 2 6.58831 2 5.48933 2.88607C5.26731 3.06508 5.06508 3.26731 4.88607 3.48933C4 4.58831 4 6.21082 4 9.45584V13M13 2.5V3C13 5.82843 13 7.24264 13.8787 8.12132C14.7574 9 16.1716 9 19 9H19.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19.75 16H17.25C16.6977 16 16.25 16.4477 16.25 17V19M16.25 19V22M16.25 19H19.25M4.25 22V19.5M4.25 19.5V16H6C6.9665 16 7.75 16.7835 7.75 17.75C7.75 18.7165 6.9665 19.5 6 19.5H4.25ZM10.25 16H11.75C12.8546 16 13.75 16.8954 13.75 18V20C13.75 21.1046 12.8546 22 11.75 22H10.25V16Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `pharmacy.svg` */
    val Pharmacy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pharmacy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20.1932 12.999C21.8501 15.8688 20.8669 19.5383 17.9971 21.1952C15.1273 22.8521 11.4578 21.8688 9.80094 18.999M20.1932 12.999C18.5364 10.1293 14.8669 9.14604 11.9971 10.8029C9.12734 12.4598 8.14409 16.1293 9.80094 18.999M20.1932 12.999L9.80094 18.999"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M10.0428 5.54203L15.1278 2.5374C17 1.43112 19.394 2.08763 20.4749 4.00376C21.3433 5.54315 21.1 7.4272 20 8.6822M10.0428 5.54203L4.95785 8.54667C3.08563 9.65294 2.44415 12.1031 3.52508 14.0192C4.17499 15.1713 5.29956 15.868 6.5 16M10.0428 5.54203L11.5 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `pin-code.svg` */
    val PinCode: ImageVector by lazy {
        ImageVector.Builder(
            name = "PinCode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15 5H11.3137C9.67871 5 8.8612 5 8.12612 5.30448C7.39104 5.60896 6.81297 6.18702 5.65685 7.34315L2.70711 10.2929C2.25435 10.7456 2 11.3597 2 12C2 12.6403 2.25435 13.2544 2.70711 13.7071L5.65685 16.6569C6.81298 17.813 7.39104 18.391 8.12612 18.6955C8.8612 19 9.67871 19 11.3137 19H15C17.8089 19 19.2134 19 20.2223 18.3259C20.659 18.034 21.034 17.659 21.3259 17.2223C22 16.2134 22 14.8089 22 12C22 9.19108 22 7.78661 21.3259 6.77772C21.034 6.34096 20.659 5.96596 20.2223 5.67412C19.2134 5 17.8089 5 15 5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.125 12H9M9.25 12C9.25 12.1381 9.13807 12.25 9 12.25C8.86193 12.25 8.75 12.1381 8.75 12C8.75 11.8619 8.86193 11.75 9 11.75C9.13807 11.75 9.25 11.8619 9.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.125 12H13M13.25 12C13.25 12.1381 13.1381 12.25 13 12.25C12.8619 12.25 12.75 12.1381 12.75 12C12.75 11.8619 12.8619 11.75 13 11.75C13.1381 11.75 13.25 11.8619 13.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.125 12H17M17.25 12C17.25 12.1381 17.1381 12.25 17 12.25C16.8619 12.25 16.75 12.1381 16.75 12C16.75 11.8619 16.8619 11.75 17 11.75C17.1381 11.75 17.25 11.8619 17.25 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `plug-socket.svg` */
    val PlugSocket: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlugSocket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.5 4.5L21.5 2.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.5 21.5L4.5 19.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.1497 15.3212L8.85034 13.0219C8.44485 12.6164 8.2421 12.4137 8.03374 12.2879C7.39802 11.904 6.60198 11.904 5.96626 12.2879C5.7579 12.4137 5.55515 12.6164 5.14966 13.0219C4.88327 13.2883 4.75007 13.4215 4.63794 13.5647C4.30099 13.995 4.088 14.5092 4.02197 15.0518C4 15.2323 4 15.4207 4 15.7974V16.7574C4 17.3342 4 17.6226 4.07661 17.8914C4.1187 18.039 4.17764 18.1814 4.25229 18.3155C4.38817 18.5597 4.59211 18.7637 5 19.1716C5.40789 19.5795 5.61184 19.7834 5.85606 19.9193C5.99022 19.9939 6.13253 20.0529 6.28018 20.095C6.54895 20.1716 6.83737 20.1716 7.41421 20.1716H8.37414C8.75087 20.1716 8.93924 20.1716 9.11979 20.1496C9.66234 20.0836 10.1765 19.8706 10.6069 19.5336C10.7501 19.4215 10.8833 19.2883 11.1497 19.0219C11.5551 18.6164 11.7579 18.4137 11.8837 18.2053C12.2675 17.5696 12.2675 16.7736 11.8837 16.1378C11.7579 15.9295 11.5551 15.7267 11.1497 15.3212Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.0005 5.12552C12.6035 5.5225 12.405 5.72099 12.2818 5.92498C11.9061 6.54735 11.9061 7.32668 12.2818 7.94904C12.405 8.15304 12.6035 8.35152 13.0005 8.7485L15.2515 10.9995C15.6485 11.3965 15.847 11.595 16.051 11.7182C16.6733 12.0939 17.4527 12.0939 18.075 11.7182C18.279 11.595 18.4775 11.3965 18.8745 10.9995C19.1353 10.7387 19.2657 10.6083 19.3755 10.4681C19.7053 10.0469 19.9138 9.54345 19.9785 9.01229C20 8.83553 20 8.65112 20 8.2823V7.34253C20 6.7778 20 6.49543 19.925 6.2323C19.8838 6.08775 19.8261 5.94843 19.753 5.81709C19.62 5.57799 19.4203 5.37833 19.021 4.979C18.6217 4.57968 18.422 4.38002 18.1829 4.24699C18.0516 4.17391 17.9122 4.11621 17.7677 4.075C17.5046 4 17.2222 4 16.6575 4L15.7177 4C15.3489 4 15.1645 4 14.9877 4.02151C14.4566 4.08615 13.9531 4.29467 13.5319 4.62455C13.3917 4.73432 13.2613 4.86472 13.0005 5.12552Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8.5 12.5L10.5 10.5M11.5 15.5L13.5 13.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `plus-add.svg` */
    val PlusAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlusAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11.9922 4.00012V20.0001M19.9922 12.0001H3.99222"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `police-badge.svg` */
    val PoliceBadge: ImageVector by lazy {
        ImageVector.Builder(
            name = "PoliceBadge",
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

    /** `qr-code.svg` */
    val QrCode: ImageVector by lazy {
        ImageVector.Builder(
            name = "QrCode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6.79289 6.79289C6.5 7.08579 6.5 7.55719 6.5 8.5C6.5 9.44281 6.5 9.91421 6.79289 10.2071C7.08579 10.5 7.55719 10.5 8.5 10.5C9.44281 10.5 9.91421 10.5 10.2071 10.2071C10.5 9.91421 10.5 9.44281 10.5 8.5C10.5 7.55719 10.5 7.08579 10.2071 6.79289C9.91421 6.5 9.44281 6.5 8.5 6.5C7.55719 6.5 7.08579 6.5 6.79289 6.79289Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.5 10.5C14.4428 10.5 14.9142 10.5 15.2071 10.2071C15.5 9.91421 15.5 9.44281 15.5 8.5C15.5 7.55719 15.5 7.08579 15.7929 6.79289C16.0858 6.5 16.5572 6.5 17.5 6.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6.5 13.5H8.5C9.44281 13.5 9.91421 13.5 10.2071 13.7929C10.5 14.0858 10.5 14.5572 10.5 15.5V17.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6.875 17.25H6.75M7 17.25C7 17.3881 6.88807 17.5 6.75 17.5C6.61193 17.5 6.5 17.3881 6.5 17.25C6.5 17.1119 6.61193 17 6.75 17C6.88807 17 7 17.1119 7 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.7929 13.7929C13.5 14.0858 13.5 14.5572 13.5 15.5C13.5 16.4428 13.5 16.9142 13.7929 17.2071C14.0858 17.5 14.5572 17.5 15.5 17.5C16.4428 17.5 16.9142 17.5 17.2071 17.2071C17.5 16.9142 17.5 16.4428 17.5 15.5C17.5 14.5572 17.5 14.0858 17.2071 13.7929C16.9142 13.5 16.4428 13.5 15.5 13.5C14.5572 13.5 14.0858 13.5 13.7929 13.7929Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M6.5 21.5C6.03563 21.5 5.80344 21.5 5.60812 21.478C3.98545 21.2952 2.70484 20.0145 2.52201 18.3919C2.5 18.1966 2.5 17.9644 2.5 17.5M17.5 21.5C17.9644 21.5 18.1966 21.5 18.3919 21.478C20.0145 21.2952 21.2952 20.0145 21.478 18.3919C21.5 18.1966 21.5 17.9644 21.5 17.5M6.5 2.5C6.03563 2.5 5.80344 2.5 5.60812 2.52201C3.98545 2.70484 2.70484 3.98545 2.52201 5.60812C2.5 5.80344 2.5 6.03563 2.5 6.5M17.5 2.5C17.9644 2.5 18.1966 2.5 18.3919 2.52201C20.0145 2.70484 21.2952 3.98545 21.478 5.60812C21.5 5.80344 21.5 6.03563 21.5 6.5"
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

    /** `radar-broadcast.svg` */
    val RadarBroadcast: ImageVector by lazy {
        ImageVector.Builder(
            name = "RadarBroadcast",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10 12 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.5 8C6.5 9 6 10.5 6 12C6 13.5 6.5 15 7.5 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M4.5 6C3 7.5 2 9.5 2 12C2 14.5 3 16.5 4.5 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16.5 16C17.5 15 18 13.5 18 12C18 10.5 17.5 9 16.5 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19.5 18C21 16.5 22 14.5 22 12C22 9.5 21 7.5 19.5 6"
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

    /** `receipt-invoice.svg` */
    val ReceiptInvoice: ImageVector by lazy {
        ImageVector.Builder(
            name = "ReceiptInvoice",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8.06805 2.72546L7.89604 2.86189C7.71084 3.00878 7.61823 3.08223 7.52605 3.12852C7.20698 3.28874 6.8259 3.26781 6.52663 3.07364C6.44017 3.01754 6.35631 2.93441 6.1886 2.76813C5.78856 2.37152 5.58853 2.17321 5.43777 2.10043C4.89824 1.83999 4.25045 2.10601 4.0547 2.6684C4 2.82556 4 3.10601 4 3.66691V20.698C4 20.9548 4 21.0832 4.01158 21.158C4.12554 21.8938 4.98624 22.2473 5.59159 21.8069C5.65313 21.7621 5.74474 21.6713 5.92789 21.4897C6.0431 21.3755 6.10079 21.3183 6.15539 21.2735C6.66242 20.8578 7.38352 20.8182 7.93376 21.1759C7.99303 21.2144 8.05667 21.2649 8.18395 21.3658L8.32009 21.4738C8.55044 21.6565 8.66564 21.7479 8.78105 21.8104C9.22912 22.053 9.77088 22.053 10.219 21.8104C10.3344 21.7479 10.4495 21.6565 10.6799 21.4738L10.75 21.4182C11.047 21.1827 11.1955 21.0649 11.3484 20.9918C11.7601 20.7949 12.2399 20.7949 12.6516 20.9918C12.8045 21.0649 12.953 21.1827 13.25 21.4182L13.3201 21.4738C13.5505 21.6565 13.6656 21.7479 13.781 21.8104C14.2291 22.053 14.7709 22.053 15.219 21.8104C15.3344 21.7479 15.4496 21.6565 15.6799 21.4738L15.816 21.3658C15.9433 21.2649 16.007 21.2144 16.0662 21.1759C16.6165 20.8182 17.3376 20.8578 17.8446 21.2735C17.8992 21.3183 17.9569 21.3755 18.0721 21.4897C18.2553 21.6713 18.3469 21.7621 18.4084 21.8069C19.0138 22.2473 19.8745 21.8938 19.9884 21.158C20 21.0832 20 20.9548 20 20.698V3.66691C20 3.10601 20 2.82556 19.9453 2.6684C19.7495 2.10601 19.1018 1.83999 18.5622 2.10043C18.4115 2.17321 18.2114 2.37152 17.8114 2.76813C17.6437 2.93441 17.5598 3.01754 17.4734 3.07364C17.1741 3.26781 16.793 3.28874 16.4739 3.12852C16.3818 3.08223 16.2892 3.00878 16.104 2.86189L15.932 2.72546C15.4614 2.35223 15.2261 2.16562 14.9695 2.08178C14.6646 1.98214 14.3354 1.98214 14.0305 2.08178C13.7739 2.16562 13.5386 2.35224 13.068 2.72546L13 2.77943C12.6428 3.06273 12.4642 3.20438 12.2661 3.2586C12.092 3.30627 11.908 3.30627 11.7339 3.2586C11.5358 3.20438 11.3572 3.06273 11 2.77943L10.932 2.72546C10.4614 2.35223 10.2261 2.16562 9.96953 2.08178C9.66458 1.98214 9.33542 1.98214 9.03047 2.08178C8.7739 2.16562 8.53862 2.35223 8.06805 2.72546Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 12H16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 8H12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M8 16H16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `refresh.svg` */
    val Refresh: ImageVector by lazy {
        ImageVector.Builder(
            name = "Refresh",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20.4879 15C19.2524 18.4956 15.9187 21 12 21C7.02943 21 3 16.9706 3 12C3 7.02943 7.02943 3 12 3C15.7292 3 18.9286 5.26806 20.2941 8.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15 9H18C19.4142 9 20.1213 9 20.5607 8.56066C21 8.12132 21 7.41421 21 6V3"
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

    /** `restart.svg` */
    val Restart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Restart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M3 4.5H21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 11.5H8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 18.5H8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.7578 17.5C13.565 18.706 14.9398 19.5 16.5 19.5C18.9853 19.5 21 17.4853 21 15C21 12.5147 18.9853 10.5 16.5 10.5C14.8075 10.5 13.3332 11.4344 12.5649 12.8154M12 9.5V10.5C12 11.9142 12 12.6213 12.4393 13.0607C12.8787 13.5 13.5858 13.5 15 13.5H16"
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

    /** `rope.svg` */
    val Rope: ImageVector by lazy {
        ImageVector.Builder(
            name = "Rope",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17.3003 15.5116C20.3416 12.9804 22.6484 9.95901 21.8354 6.92985C20.7852 3.01732 15.5349 1.02232 10.1083 2.4739C4.68179 3.92547 1.13402 8.27394 2.18415 12.1865C3.03697 15.3638 5.88849 16.9746 10.3503 16.42M17.3003 15.5116C15.9161 11.5244 9.71766 12.8164 10.002 15.5116C10.2129 17.5105 14.9298 17.5105 17.3003 15.5116ZM17.3003 15.5116C18.1269 18.2959 16.2449 21.4457 12.9572 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `route-navigation.svg` */
    val RouteNavigation: ImageVector by lazy {
        ImageVector.Builder(
            name = "RouteNavigation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4 3V21M20 3V21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.66101 16.8652C10.6709 14.9551 11.1759 14 12 14C12.8241 14 13.3291 14.9551 14.339 16.8652L15.0267 18.166C15.8023 19.6329 16.1901 20.3664 15.9082 20.7191C15.8324 20.8139 15.7325 20.8921 15.6163 20.9476C15.1841 21.1541 14.3908 20.7381 12.8043 19.9062C12.4524 19.7216 12.2764 19.6294 12.084 19.6129C12.0281 19.6081 11.9719 19.6081 11.916 19.6129C11.7236 19.6294 11.5476 19.7216 11.1957 19.9062C9.60915 20.7381 8.81587 21.1541 8.38372 20.9476C8.26754 20.8921 8.16764 20.8139 8.09184 20.7191C7.80989 20.3664 8.19769 19.6329 8.97329 18.166L9.66101 16.8652Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M12 3V5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 9V11"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
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

    /** `share.svg` */
    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M19.0001 13V14C19.0001 17.2998 19.0001 18.9497 17.9749 19.9749C16.9498 21 15.2999 21 12.0001 21H10.0001C6.70023 21 5.05031 21 4.02519 19.9749C3.00006 18.9497 3.00006 17.2998 3.00006 14V12C3.00006 8.70017 3.00006 7.05025 4.02519 6.02513C5.05031 5 6.70023 5 10.0001 5H11.0001"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14 3H18C19.4142 3 20.1213 3 20.5607 3.43934C21 3.87868 21 4.58579 21 6V10M20 4L11 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `shield-off.svg` */
    val ShieldOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShieldOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M4.5 4.5L18 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.7088 3.49534C16.8165 2.55382 14.5009 2 12 2C9.4991 2 7.1835 2.55382 5.29116 3.49534C4.36318 3.95706 3.89919 4.18792 3.4496 4.91378C3 5.63965 3 6.34248 3 7.74814V11.2371C3 16.9205 7.54236 20.0804 10.173 21.4338C10.9067 21.8113 11.2735 22 12 22C12.7265 22 13.0933 21.8113 13.8269 21.4338C16.4576 20.0804 21 16.9205 21 11.2371L21 7.74814C21 6.34249 21 5.63966 20.5504 4.91378C20.1008 4.18791 19.6368 3.95706 18.7088 3.49534Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `shield-with-keyhole.svg` */
    val ShieldWithKeyhole: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShieldWithKeyhole",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.7088 3.49546C16.8165 2.55394 14.5009 2.00012 12 2.00012C9.4991 2.00012 7.1835 2.55394 5.29116 3.49547C4.36318 3.95718 3.89919 4.18804 3.4496 4.91391C3 5.63978 3 6.3426 3 7.74826V11.2372C3 16.9206 7.54236 20.0805 10.173 21.4339C10.9067 21.8114 11.2735 22.0001 12 22.0001C12.7265 22.0001 13.0933 21.8114 13.8269 21.4339C16.4576 20.0805 21 16.9206 21 11.2372L21 7.74827C21 6.34261 21 5.63978 20.5504 4.91391C20.1008 4.18804 19.6368 3.95718 18.7088 3.49546Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 9.00012V10.0001M11 9.50012C11 9.76534 11.1054 10.0197 11.2929 10.2072C11.4804 10.3948 11.7348 10.5001 12 10.5001C12.2652 10.5001 12.5196 10.3948 12.7071 10.2072C12.8946 10.0197 13 9.76534 13 9.50012C13 9.23491 12.8946 8.98055 12.7071 8.79302C12.5196 8.60548 12.2652 8.50012 12 8.50012C11.7348 8.50012 11.4804 8.60548 11.2929 8.79302C11.1054 8.98055 11 9.23491 11 9.50012Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.75 14.0001H11.25L12 10.5001L12.75 14.0001Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `shield-with-padlock.svg` */
    val ShieldWithPadlock: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShieldWithPadlock",
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

    /** `sleeping.svg` */
    val Sleeping: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sleeping",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13 2.04938C12.6711 2.01672 12.3375 2 12 2C6.47715 2 2 6.47715 2 12C2 17.5228 6.47715 22 12 22C17.5228 22 22 17.5228 22 12C22 11.3151 21.9311 10.6462 21.8 10"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 11H8.70711C8.25435 11 7.82014 10.8201 7.5 10.5M14 11H15.2929C15.7456 11 16.1799 10.8201 16.5 10.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 16 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 2H19.9474C20.5675 2 20.8775 2 20.9601 2.20009C21.0427 2.40019 20.8317 2.64023 20.4098 3.1203L17.9846 5.8797C17.5627 6.35977 17.3517 6.59981 17.4343 6.79991C17.5169 7 17.8269 7 18.447 7H21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `smart-home.svg` */
    val SmartHome: ImageVector by lazy {
        ImageVector.Builder(
            name = "SmartHome",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20 8.58505V13.5005C20 17.2717 20 19.1574 18.8284 20.3289C18.0203 21.1371 16.8723 21.3878 15 21.4655M4 8.58505V13.5005C4 17.2717 4 19.1574 5.17157 20.3289C6.23465 21.392 7.88563 21.4905 10.9998 21.4996C11.5521 21.5012 12 21.0528 12 20.5005V17.5005"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22 10.5003L17.6569 6.33582C14.9902 3.77883 13.6569 2.50034 12 2.50034C10.3431 2.50034 9.00981 3.77883 6.34315 6.33582L2 10.5003"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M14.001 9.00034V11.5003M10.001 11.5003V9.00034M8.50553 12.3803C8.46629 11.9054 8.87602 11.5003 9.39552 11.5003H14.6104C15.1299 11.5003 15.5396 11.9054 15.5004 12.3803L15.3931 13.6777C15.316 14.6104 14.9786 15.5093 14.4133 16.2879L14.0628 16.7706C13.7319 17.2264 13.1741 17.5003 12.5768 17.5003H11.4291C10.8318 17.5003 10.2741 17.2264 9.94308 16.7706L9.59262 16.2879C9.02726 15.5093 8.68984 14.6104 8.61276 13.6777L8.50553 12.3803Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `snow.svg` */
    val Snow: ImageVector by lazy {
        ImageVector.Builder(
            name = "Snow",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21 14.25L20.1689 13.591C19.223 12.841 18.75 12.466 18.75 12C18.75 11.534 19.223 11.159 20.1689 10.409L21 9.75M3 9.75L3.83115 10.409C4.77705 11.159 5.25 11.534 5.25 12C5.25 12.466 4.77705 12.841 3.83115 13.591L3 14.25"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5718 21L14.7282 19.9412C14.9062 18.7362 14.9951 18.1337 15.4019 17.8986C15.8087 17.6635 16.3744 17.8876 17.5058 18.3358L18.5 18.7296M9.4282 3L9.27182 4.0588C9.09384 5.26379 9.00486 5.86629 8.59808 6.10139C8.1913 6.3365 7.62558 6.1124 6.49416 5.6642L5.5 5.27038"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5 18.7317L6.07032 18.3375C7.2884 17.8889 7.89747 17.6645 8.33521 17.8994C8.77295 18.1343 8.86844 18.7367 9.05941 19.9414L9.22722 21M19 5.26825L17.9297 5.66249C16.7116 6.11115 16.1025 6.33548 15.6648 6.1006C15.2271 5.86571 15.1316 5.26333 14.9406 4.05859L14.7728 3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19 12.0003H5M15.5 17.9998L8.5 6M15.5 6.00025L8.5 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `source-code.svg` */
    val SourceCode: ImageVector by lazy {
        ImageVector.Builder(
            name = "SourceCode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 8L18.8398 9.85008C19.6133 10.6279 20 11.0168 20 11.5C20 11.9832 19.6133 12.3721 18.8398 13.1499L17 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7 8L5.16019 9.85008C4.38673 10.6279 4 11.0168 4 11.5C4 11.9832 4.38673 12.3721 5.16019 13.1499L7 15"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.5 4L9.5 20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `speedometer.svg` */
    val Speedometer: ImageVector by lazy {
        ImageVector.Builder(
            name = "Speedometer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13.5 13L17 9M14 15C14 16.1046 13.1046 17 12 17C10.8954 17 10 16.1046 10 15C10 13.8954 10.8954 13 12 13C13.1046 13 14 13.8954 14 15Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6 12C6 8.68629 8.68629 6 12 6C13.0929 6 14.1175 6.29218 15 6.80269"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M2.50006 12.0001C2.50006 7.52172 2.50006 5.28255 3.8913 3.8913C5.28255 2.50006 7.52172 2.50006 12.0001 2.50006C16.4784 2.50006 18.7176 2.50006 20.1088 3.8913C21.5001 5.28255 21.5001 7.52172 21.5001 12.0001C21.5001 16.4784 21.5001 18.7176 20.1088 20.1088C18.7176 21.5001 16.4784 21.5001 12.0001 21.5001C7.52172 21.5001 5.28255 21.5001 3.8913 20.1088C2.50006 18.7176 2.50006 16.4784 2.50006 12.0001Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `star-off.svg` */
    val StarOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "StarOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.5038 14.5L21.1477 11.8925C22.616 10.4206 22.1366 8.92853 20.099 8.58575L16.9128 8.05143C16.3734 7.9607 15.7342 7.48687 15.4945 6.99288L13.7366 3.44418C12.7877 1.51861 11.2296 1.51861 10.2707 3.44418L9.5 5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.29561 7.99998C7.22668 8.02267 7.15848 8.04006 7.09201 8.05144L3.90008 8.58576C1.85885 8.92853 1.38856 10.4206 2.84945 11.8925L5.33095 14.3927C5.7512 14.8161 5.98134 15.6327 5.85126 16.2175L5.14083 19.3125C4.58049 21.7522 5.88128 22.7099 8.02257 21.4296L11.0144 19.6452C11.5647 19.3226 12.4553 19.3226 12.9956 19.6452L15.9874 21.4296C18.1387 22.7099 19.4295 21.7623 18.8691 19.3125L18.7974 19"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2 2L22 22"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `sun-moon.svg` */
    val SunMoon: ImageVector by lazy {
        ImageVector.Builder(
            name = "SunMoon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.5 11.5C16.5 9.567 14.933 8 13 8"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13.5 2.5V4.5M19.5018 4.5L18.0028 5.99902M21.5018 10.5H19.502"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17 16.5314C16.116 17.0034 15.1064 17.271 14.0343 17.271C10.552 17.271 7.72899 14.448 7.72899 10.9657C7.72899 9.89358 7.99657 8.88398 8.46857 8C5.33406 8.73462 3 11.548 3 14.9065C3 18.8241 6.17586 22 10.0935 22C13.452 22 16.2654 19.6659 17 16.5314Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `sun.svg` */
    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.9991 12C16.9991 14.7614 14.7605 17 11.9991 17C9.23766 17 6.99908 14.7614 6.99908 12C6.99908 9.23858 9.23766 7 11.9991 7C14.7605 7 16.9991 9.23858 16.9991 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12.1247 3.25H11.9997M12.1242 20.75H11.9992M20.75 12.125V12M3.25 12.125V12M18.2752 5.90098L18.1868 5.81259M5.90051 18.275L5.81212 18.1866M18.0987 18.2756L18.187 18.1872M5.72429 5.9012L5.81267 5.81282M12.2497 3.25C12.2497 3.38807 12.1378 3.5 11.9997 3.5C11.8616 3.5 11.7497 3.38807 11.7497 3.25C11.7497 3.11193 11.8616 3 11.9997 3C12.1378 3 12.2497 3.11193 12.2497 3.25ZM12.2492 20.75C12.2492 20.8881 12.1373 21 11.9992 21C11.8611 21 11.7492 20.8881 11.7492 20.75C11.7492 20.6119 11.8611 20.5 11.9992 20.5C12.1373 20.5 12.2492 20.6119 12.2492 20.75ZM20.75 12.25C20.6119 12.25 20.5 12.1381 20.5 12C20.5 11.8619 20.6119 11.75 20.75 11.75C20.8881 11.75 21 11.8619 21 12C21 12.1381 20.8881 12.25 20.75 12.25ZM3.25 12.25C3.11193 12.25 3 12.1381 3 12C3 11.8619 3.11193 11.75 3.25 11.75C3.38807 11.75 3.5 11.8619 3.5 12C3.5 12.1381 3.38807 12.25 3.25 12.25ZM18.3636 5.98937C18.266 6.087 18.1077 6.087 18.01 5.98937C17.9124 5.89174 17.9124 5.73345 18.01 5.63582C18.1077 5.53819 18.266 5.53819 18.3636 5.63582C18.4612 5.73345 18.4612 5.89174 18.3636 5.98937ZM5.9889 18.3634C5.89127 18.461 5.73297 18.461 5.63534 18.3634C5.53771 18.2658 5.53771 18.1075 5.63534 18.0099C5.73297 17.9122 5.89127 17.9122 5.9889 18.0099C6.08653 18.1075 6.08653 18.2658 5.9889 18.3634ZM18.0103 18.364C17.9126 18.2663 17.9126 18.108 18.0103 18.0104C18.1079 17.9128 18.2662 17.9128 18.3638 18.0104C18.4614 18.108 18.4614 18.2663 18.3638 18.364C18.2662 18.4616 18.1079 18.4616 18.0103 18.364ZM5.6359 5.98959C5.53827 5.89196 5.53827 5.73367 5.6359 5.63604C5.73353 5.53841 5.89182 5.53841 5.98945 5.63604C6.08708 5.73367 6.08708 5.89196 5.98945 5.98959C5.89182 6.08722 5.73353 6.08722 5.6359 5.98959Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `sunset.svg` */
    val Sunset: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sunset",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M9.5 7.5C9.99153 8.0057 11.2998 10 12 10M14.5 7.5C14.0085 8.0057 12.7002 10 12 10M12 10V4"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.3633 10.6357L16.9491 12.05"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M3 17H5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5.63657 10.6356L7.05078 12.0498"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M21 17H19"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M21 20H3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M16 17C16 14.7909 14.2091 13 12 13C9.79086 13 8 14.7909 8 17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
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

    /** `telephone-call.svg` */
    val TelephoneCall: ImageVector by lazy {
        ImageVector.Builder(
            name = "TelephoneCall",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13 3C17.4183 3 21 6.58172 21 11M13.5 6.5C15.7091 6.5 17.5 8.29086 17.5 10.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.15825 5.71223L8.7556 4.80625C8.49232 4.21388 8.36068 3.91768 8.1638 3.69101C7.91707 3.40694 7.59547 3.19794 7.23567 3.08785C6.94858 3 6.62446 3 5.97621 3C5.02791 3 4.55375 3 4.15573 3.18229C3.68687 3.39702 3.26343 3.86328 3.09473 4.3506C2.95151 4.76429 2.99253 5.18943 3.07458 6.0397C3.94791 15.0902 8.90981 20.0521 17.9603 20.9254C18.8106 21.0075 19.2357 21.0485 19.6494 20.9053C20.1367 20.7366 20.603 20.3131 20.8177 19.8443C21 19.4462 21 18.9721 21 18.0238C21 17.3755 21 17.0514 20.9122 16.7643C20.8021 16.4045 20.5931 16.0829 20.309 15.8362C20.0823 15.6393 19.7861 15.5077 19.1937 15.2444L18.2878 14.8417C17.6462 14.5566 17.3255 14.4141 16.9995 14.3831C16.6876 14.3534 16.3731 14.3972 16.0811 14.5109C15.776 14.6297 15.5063 14.8544 14.967 15.3038C14.4301 15.7512 14.1617 15.9749 13.8337 16.0947C13.543 16.2009 13.1586 16.2403 12.8523 16.1951C12.5069 16.1442 12.2423 16.0029 11.7133 15.7201C10.0672 14.8405 9.15953 13.9328 8.27986 12.2867C7.99714 11.7577 7.85578 11.4931 7.80487 11.1477C7.75974 10.8414 7.79908 10.457 7.9053 10.1663C8.02512 9.83828 8.24881 9.56986 8.69619 9.033C9.14562 8.49368 9.37034 8.22402 9.48915 7.91891C9.60285 7.62694 9.64661 7.3124 9.61694 7.00048C9.58594 6.67452 9.44338 6.35376 9.15825 5.71223Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `temperature-hot.svg` */
    val TemperatureHot: ImageVector by lazy {
        ImageVector.Builder(
            name = "TemperatureHot",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M16.5001 22C18.7092 22 20.5001 20.2091 20.5001 18C20.5001 16.9335 20.0827 15.9646 19.4025 15.2475C18.8958 14.7134 18.6424 14.4463 18.5713 14.2679C18.5001 14.0895 18.5001 13.8535 18.5001 13.3815V4C18.5001 2.89543 17.6047 2 16.5001 2C15.3955 2 14.5001 2.89543 14.5001 4V13.3815C14.5001 13.8535 14.5001 14.0895 14.4289 14.2679C14.3578 14.4463 14.1044 14.7134 13.5977 15.2475C12.9174 15.9646 12.5001 16.9335 12.5001 18C12.5001 20.2091 14.291 22 16.5001 22Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10.3133 15.8303C8.67792 15.5416 7.36329 14.104 7.20333 12.2607C7.01373 10.076 8.51806 8.14861 10.5634 7.95588C10.883 7.92576 11.197 7.9398 11.5 7.99327M10.2201 4L10.323 5.18677M6.04201 7.57572L5.18359 6.81058M4.611 12.505L3.5 12.6097M6.86776 17.0868L6.15499 18"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `toggle-off.svg` */
    val ToggleOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "ToggleOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M11 12C11 13.6569 9.65685 15 8 15C6.34315 15 5 13.6569 5 12C5 10.3431 6.34315 9 8 9C9.65685 9 11 10.3431 11 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M16 6H8C4.68629 6 2 8.68629 2 12C2 15.3137 4.68629 18 8 18H16C19.3137 18 22 15.3137 22 12C22 8.68629 19.3137 6 16 6Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `toggle-on.svg` */
    val ToggleOn: ImageVector by lazy {
        ImageVector.Builder(
            name = "ToggleOn",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M13 12C13 13.6569 14.3431 15 16 15C17.6569 15 19 13.6569 19 12C19 10.3431 17.6569 9 16 9C14.3431 9 13 10.3431 13 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8 6H16C19.3137 6 22 8.68629 22 12C22 15.3137 19.3137 18 16 18H8C4.68629 18 2 15.3137 2 12C2 8.68629 4.68629 6 8 6Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
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

    /** `torus-icon.svg` */
    val TorusIcon: ImageVector by lazy {
        ImageVector.Builder(
            name = "TorusIcon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M2 12 a 10 8 0 1 0 20 0 a 10 8 0 1 0 -20 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9 11 a 3 2 0 1 0 6 0 a 3 2 0 1 0 -6 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `two-way-fork.svg` */
    val TwoWayFork: ImageVector by lazy {
        ImageVector.Builder(
            name = "TwoWayFork",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M21 8.5V6.6C21 4.90294 21 4.05442 20.4728 3.52721C19.9456 3 19.0971 3 17.4 3H15.5M20 4L14.5 9.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 8.5V6.6C3 4.90294 3 4.05442 3.52721 3.52721C4.05442 3 4.90294 3 6.6 3H8.5M4 4L9.65686 9.65686C10.813 10.813 11.391 11.391 11.6955 12.1261C12 12.8612 12 13.6787 12 15.3137V21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `umbrella.svg` */
    val Umbrella: ImageVector by lazy {
        ImageVector.Builder(
            name = "Umbrella",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 3.5V2"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 3.5C11.0608 3.5 7.52791 7.29323 6.97182 12.2037M12 3.5C12.9392 3.5 16.4721 7.29322 17.0282 12.2037M12 3.5C16.9367 3.5 21.0545 6.93552 22 11.5C20.6123 12.7 18.1073 12.4691 17.0282 12.2037M12 3.5C7.06333 3.5 2.94545 6.93552 2 11.5C3.38792 12.7 5.89285 12.4691 6.97182 12.2037M6.97182 12.2037C8.4559 13.0288 10.1718 13.5 12 13.5C13.8282 13.5 15.5441 13.0288 17.0282 12.2037"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 13.5V20.5C12 21.3284 11.3284 22 10.5 22C9.67157 22 9 21.3284 9 20.5V20"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `user-add.svg` */
    val UserAdd: ImageVector by lazy {
        ImageVector.Builder(
            name = "UserAdd",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M15 8C15 5.23858 12.7614 3 10 3C7.23858 3 5 5.23858 5 8C5 10.7614 7.23858 13 10 13C12.7614 13 15 10.7614 15 8Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M17.5 21L17.5 14M14 17.5H21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M3 20C3 16.134 6.13401 13 10 13C11.4872 13 12.8662 13.4638 14 14.2547"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `user-circle.svg` */
    val UserCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "UserCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.4984 19.1511C17.3377 17.4018 15.2947 16.2009 12.9313 16.0569L11.9984 16C11.6652 16.0083 11.3547 16.0194 11.0617 16.0325C8.71722 16.1376 6.66598 17.3796 5.5 19.1511"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14.9961 10C14.9961 11.6569 13.6529 13 11.9961 13C10.3392 13 8.99609 11.6569 8.99609 10C8.99609 8.34315 10.3392 7 11.9961 7C13.6529 7 14.9961 8.34315 14.9961 10Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2C17.5228 2 22 6.47715 22 12Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `user-group.svg` */
    val UserGroup: ImageVector by lazy {
        ImageVector.Builder(
            name = "UserGroup",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M18.4995 20.5C18.2663 17.5685 15.8417 15.2477 12.808 15.0521L11.9995 15C11.7107 15.0076 11.4416 15.0178 11.1877 15.0298C8.18075 15.1723 5.7304 17.5974 5.49951 20.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.2495 9.25C15.2495 11.0449 13.7944 12.5 11.9995 12.5C10.2046 12.5 8.74952 11.0449 8.74952 9.25C8.74952 7.45507 10.2046 6 11.9995 6C13.7944 6 15.2495 7.45507 15.2495 9.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M5.50249 8.5C5.17908 7.99485 4.99158 7.39432 4.99158 6.75C4.99158 4.95507 6.44665 3.5 8.24157 3.5C8.68752 3.5 9.1125 3.58982 9.49939 3.75235"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.4963 8.5C18.8197 7.99485 19.0072 7.39432 19.0072 6.75C19.0072 4.95507 17.5521 3.5 15.7572 3.5C15.3113 3.5 14.8863 3.58982 14.4994 3.75235"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M22.0007 17.9996C21.8208 15.7374 19.9995 13.5 17.9995 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M1.99927 17.9996C2.17923 15.7374 4.00049 13.5 6.00049 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `user-remove.svg` */
    val UserRemove: ImageVector by lazy {
        ImageVector.Builder(
            name = "UserRemove",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M14.5 8C14.5 5.23858 12.2614 3 9.5 3C6.73858 3 4.5 5.23858 4.5 8C4.5 10.7614 6.73858 13 9.5 13C12.2614 13 14.5 10.7614 14.5 8Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.5 21L18.5 18M18.5 18L21.5 15M18.5 18L15.5 15M18.5 18L21.5 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M2.5 20C2.5 16.134 5.63401 13 9.5 13C10.775 13 11.9704 13.3409 13 13.9365"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `user.svg` */
    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M17 8.5C17 5.73858 14.7614 3.5 12 3.5C9.23858 3.5 7 5.73858 7 8.5C7 11.2614 9.23858 13.5 12 13.5C14.7614 13.5 17 11.2614 17 8.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M19 20.5C19 16.634 15.866 13.5 12 13.5C8.13401 13.5 5 16.634 5 20.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `uv-index.svg` */
    val UvIndex: ImageVector by lazy {
        ImageVector.Builder(
            name = "UvIndex",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M12 3.00002V4.50002"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M17 13C17 10.2386 14.7614 8 12 8C9.23858 8 7 10.2386 7 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M5.98828 6.98927L4.92762 5.92861"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M22 13L20.5 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M3.5 13L2 13"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M19.0703 5.92873L18.0097 6.98939"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M6.5 16V19C6.5 19.9428 6.5 20.4142 6.79289 20.7071C7.08579 21 7.55719 21 8.5 21C9.44281 21 9.91421 21 10.2071 20.7071C10.5 20.4142 10.5 19.9428 10.5 19V16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M13.5 16L15.5 21L17.5 16"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `version.svg` */
    val Version: ImageVector by lazy {
        ImageVector.Builder(
            name = "Version",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M8 7H16.75C18.8567 7 19.91 7 20.6667 7.50559C20.9943 7.72447 21.2755 8.00572 21.4944 8.33329C22 9.08996 22 10.1433 22 12.25C22 15.7612 22 17.5167 21.1573 18.7779C20.7926 19.3238 20.3238 19.7926 19.7779 20.1573C18.5167 21 16.7612 21 13.25 21H12C7.28595 21 4.92893 21 3.46447 19.5355C2 18.0711 2 15.714 2 11V7.94427C2 6.1278 2 5.21956 2.38032 4.53806C2.65142 4.05227 3.05227 3.65142 3.53806 3.38032C4.21956 3 5.1278 3 6.94427 3C8.10802 3 8.6899 3 9.19926 3.19101C10.3622 3.62712 10.8418 4.68358 11.3666 5.73313L12 7"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M10 14 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M14 14H18M10 14H6"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `walkie-talkie.svg` */
    val WalkieTalkie: ImageVector by lazy {
        ImageVector.Builder(
            name = "WalkieTalkie",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M20 9C19.2048 5.01455 15.5128 2 11.0793 2C6.06549 2 2 5.85521 2 10.61C2 12.8946 2.93819 14.9704 4.46855 16.5108C4.80549 16.85 5.03045 17.3134 4.93966 17.7903C4.78982 18.5701 4.45026 19.2975 3.95305 19.9037C5.26123 20.1449 6.62147 19.9277 7.78801 19.3127C8.20039 19.0954 8.40657 18.9867 8.55207 18.9646C8.65392 18.9492 8.78659 18.9636 9 19.0002"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11 16.2617C11 19.1674 13.4628 21.5234 16.5 21.5234C16.8571 21.5238 17.2132 21.4908 17.564 21.425C17.8165 21.3775 17.9428 21.3538 18.0309 21.3673C18.119 21.3807 18.244 21.4472 18.4938 21.58C19.2004 21.9558 20.0244 22.0885 20.8169 21.9411C20.5157 21.5707 20.31 21.1262 20.2192 20.6496C20.1642 20.3582 20.3005 20.075 20.5046 19.8677C21.4317 18.9263 22 17.6578 22 16.2617C22 13.356 19.5372 11 16.5 11C13.4628 11 11 13.356 11 16.2617Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `walking-person.svg` */
    val WalkingPerson: ImageVector by lazy {
        ImageVector.Builder(
            name = "WalkingPerson",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6 12.5L7.73811 9.89287C7.91034 9.63452 8.14035 9.41983 8.40993 9.26578L10.599 8.01487C11.1619 7.69323 11.8483 7.67417 12.4282 7.9641C13.0851 8.29255 13.4658 8.98636 13.7461 9.66522C14.2069 10.7814 15.3984 12 18 12"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M13 9L11.7772 14.5951M10.5 8.5L9.77457 11.7645C9.6069 12.519 9.88897 13.3025 10.4991 13.777L14 16.5L15.5 21"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M9.5 16L9 17.5L6.5 20.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15 4.5C15 5.32843 14.3284 6 13.5 6C12.6716 6 12 5.32843 12 4.5C12 3.67157 12.6716 3 13.5 3C14.3284 3 15 3.67157 15 4.5Z"
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

    /** `webhook.svg` */
    val Webhook: ImageVector by lazy {
        ImageVector.Builder(
            name = "Webhook",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M5.062 13C3.83229 13.6824 3 14.994 3 16.5C3 18.7091 4.79086 20.5 7 20.5C9.20914 20.5 11 18.7091 11 16.5H17"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 7.5L15.0571 13.0027C15.6323 12.6825 16.2949 12.5 17 12.5C19.2091 12.5 21 14.2909 21 16.5C21 18.7091 19.2091 20.5 17 20.5C16.0541 20.5 15.1848 20.1716 14.5 19.6227"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M12 8.5C12.5523 8.5 13 8.05228 13 7.5C13 6.94772 12.5523 6.5 12 6.5M12 8.5C11.4477 8.5 11 8.05228 11 7.5C11 6.94772 11.4477 6.5 12 6.5M12 8.5V6.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M7 17.5C7.55228 17.5 8 17.0523 8 16.5C8 15.9477 7.55228 15.5 7 15.5M7 17.5C6.44772 17.5 6 17.0523 6 16.5C6 15.9477 6.44772 15.5 7 15.5M7 17.5V15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M17 17.5C17.5523 17.5 18 17.0523 18 16.5C18 15.9477 17.5523 15.5 17 15.5M17 17.5C16.4477 17.5 16 17.0523 16 16.5C16 15.9477 16.4477 15.5 17 15.5M17 17.5V15.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M16 7.5C16 5.29086 14.2091 3.5 12 3.5C9.79086 3.5 8 5.29086 8 7.5C8 9.004 8.83007 10.3141 10.0571 10.9973L7 16.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `welcome-sparkles.svg` */
    val WelcomeSparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "WelcomeSparkles",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6.99069 4.56404C5.14721 3.92359 4.06068 3.76816 3.41442 4.41442C2.49121 5.33764 3.20405 7.15934 4.62973 10.8028L6.99069 16.8363C8.35243 20.3163 9.03329 22.0563 10.1133 21.9986C11.1934 21.9409 11.6886 20.125 12.6791 16.4933C12.974 15.4119 13.1215 14.8712 13.4963 14.4963C13.8712 14.1215 14.4119 13.974 15.4933 13.6791C19.125 12.6886 20.9409 12.1934 20.9986 11.1133C21.0428 10.287 20.0346 9.69434 18 8.85164"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M11.4999 4C12.1 5.62152 13.3784 6.89998 14.9999 7.5C13.3784 8.10002 12.1 9.37848 11.4999 11C10.8999 9.37848 9.62146 8.10002 7.99994 7.5C9.62146 6.89998 10.8999 5.62152 11.4999 4Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M15.7499 2C15.9642 2.57911 16.4208 3.03571 16.9999 3.25C16.4208 3.46429 15.9642 3.92089 15.7499 4.5C15.5356 3.92089 15.0791 3.46429 14.4999 3.25C15.0791 3.03571 15.5356 2.57911 15.7499 2Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }.build()
    }

    /** `widget-grid-tile.svg` */
    val WidgetGridTile: ImageVector by lazy {
        ImageVector.Builder(
            name = "WidgetGridTile",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M10.5 8.75V6.75C10.5 5.10626 10.5 4.28439 10.046 3.73121C9.96291 3.62995 9.87005 3.53709 9.76879 3.45398C9.21561 3 8.39374 3 6.75 3C5.10626 3 4.28439 3 3.73121 3.45398C3.62995 3.53709 3.53709 3.62995 3.45398 3.73121C3 4.28439 3 5.10626 3 6.75V8.75C3 10.3937 3 11.2156 3.45398 11.7688C3.53709 11.8701 3.62995 11.9629 3.73121 12.046C4.28439 12.5 5.10626 12.5 6.75 12.5C8.39374 12.5 9.21561 12.5 9.76879 12.046C9.87005 11.9629 9.96291 11.8701 10.046 11.7688C10.5 11.2156 10.5 10.3937 10.5 8.75Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M7.75 15.5H5.75C5.05222 15.5 4.70333 15.5 4.41943 15.5861C3.78023 15.78 3.28002 16.2802 3.08612 16.9194C3 17.2033 3 17.5522 3 18.25C3 18.9478 3 19.2967 3.08612 19.5806C3.28002 20.2198 3.78023 20.72 4.41943 20.9139C4.70333 21 5.05222 21 5.75 21H7.75C8.44778 21 8.79667 21 9.08057 20.9139C9.71977 20.72 10.22 20.2198 10.4139 19.5806C10.5 19.2967 10.5 18.9478 10.5 18.25C10.5 17.5522 10.5 17.2033 10.4139 16.9194C10.22 16.2802 9.71977 15.78 9.08057 15.5861C8.79667 15.5 8.44778 15.5 7.75 15.5Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M21 17.25V15.25C21 13.6063 21 12.7844 20.546 12.2312C20.4629 12.1299 20.3701 12.0371 20.2688 11.954C19.7156 11.5 18.8937 11.5 17.25 11.5C15.6063 11.5 14.7844 11.5 14.2312 11.954C14.1299 12.0371 14.0371 12.1299 13.954 12.2312C13.5 12.7844 13.5 13.6063 13.5 15.25V17.25C13.5 18.8937 13.5 19.7156 13.954 20.2688C14.0371 20.3701 14.1299 20.4629 14.2312 20.546C14.7844 21 15.6063 21 17.25 21C18.8937 21 19.7156 21 20.2688 20.546C20.3701 20.4629 20.4629 20.3701 20.546 20.2688C21 19.7156 21 18.8937 21 17.25Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M18.25 3H16.25C15.5522 3 15.2033 3 14.9194 3.08612C14.2802 3.28002 13.78 3.78023 13.5861 4.41943C13.5 4.70333 13.5 5.05222 13.5 5.75C13.5 6.44778 13.5 6.79667 13.5861 7.08057C13.78 7.71977 14.2802 8.21998 14.9194 8.41388C15.2033 8.5 15.5522 8.5 16.25 8.5H18.25C18.9478 8.5 19.2967 8.5 19.5806 8.41388C20.2198 8.21998 20.72 7.71977 20.9139 7.08057C21 6.79667 21 6.44778 21 5.75C21 5.05222 21 4.70333 20.9139 4.41943C20.72 3.78023 20.2198 3.28002 19.5806 3.08612C19.2967 3 18.9478 3 18.25 3Z"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
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

    /** `wind.svg` */
    val Wind: ImageVector by lazy {
        ImageVector.Builder(
            name = "Wind",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6 6C6 4.34315 7.34315 3 9 3C10.6569 3 12 4.34315 12 6C12 7.65685 10.6569 9 9 9H3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M9 18C9 19.6569 10.3431 21 12 21C13.6569 21 15 19.6569 15 18C15 16.3431 13.6569 15 12 15H3"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M15 9C15 7.34315 16.3431 6 18 6C19.6569 6 21 7.34315 21 9C21 10.6569 19.6569 12 18 12H6.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Miter
            )
        }.build()
    }

    /** `wrist-watch.svg` */
    val WristWatch: ImageVector by lazy {
        ImageVector.Builder(
            name = "WristWatch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                pathData = addPathNodes(
                    "M6 12 a 6 6 0 1 0 12 0 a 6 6 0 1 0 -12 0"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Miter
            )
            addPath(
                pathData = addPathNodes(
                    "M8 7.5C8 7.5 8.89734 5.92822 9.06196 4.01957C9.13851 3.13198 9.17678 2.68819 9.42636 2.43221C9.67594 2.17623 9.97701 2.14256 10.5792 2.07523C10.9774 2.03069 11.451 2 12 2C12.549 2 13.0226 2.03069 13.4208 2.07523C14.023 2.14256 14.3241 2.17623 14.5736 2.43221C14.8232 2.68819 14.8615 3.13198 14.938 4.01957C15.1027 5.92822 16 7.5 16 7.5"
                ),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            addPath(
                pathData = addPathNodes(
                    "M16 16.5C16 16.5 15.1027 18.0718 14.938 19.9804C14.8615 20.868 14.8232 21.3118 14.5736 21.5678C14.3241 21.8238 14.023 21.8574 13.4208 21.9248C13.0226 21.9693 12.549 22 12 22C11.451 22 10.9774 21.9693 10.5792 21.9248C9.97701 21.8574 9.67594 21.8238 9.42636 21.5678C9.17678 21.3118 9.13851 20.868 9.06196 19.9804C8.89734 18.0718 8 16.5 8 16.5"
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
        "add-new-place" to AddNewPlace,
        "ai-search-02" to AiSearch02,
        "alarm-clock" to AlarmClock,
        "alert-02" to Alert02,
        "all-bookmark" to AllBookmark,
        "appearance" to Appearance,
        "apple-logo-outline" to AppleLogoOutline,
        "apple-logo" to AppleLogo,
        "arrow-down-01" to ArrowDown01,
        "arrow-left-01" to ArrowLeft01,
        "arrow-right-01" to ArrowRight01,
        "arrow-up-01" to ArrowUp01,
        "at-sign" to AtSign,
        "audio-wave" to AudioWave,
        "backpack" to Backpack,
        "badge-alert" to BadgeAlert,
        "ban" to Ban,
        "battery-charging-02" to BatteryCharging02,
        "battery-eco-charging" to BatteryEcoCharging,
        "battery-full" to BatteryFull,
        "battery-low" to BatteryLow,
        "battery-medium-01" to BatteryMedium01,
        "battery-medium-02" to BatteryMedium02,
        "battery-optimisation-off" to BatteryOptimisationOff,
        "battery-optimisation-on" to BatteryOptimisationOn,
        "battery-warning" to BatteryWarning,
        "bicycle-01" to Bicycle01,
        "blood-oxygen" to BloodOxygen,
        "bluetooth" to Bluetooth,
        "body-temperature" to BodyTemperature,
        "bolt-flash" to BoltFlash,
        "bookmark-02" to Bookmark02,
        "bookmark-block-02" to BookmarkBlock02,
        "call-after-a-fall" to CallAfterAFall,
        "camera-add" to CameraAdd,
        "cane" to Cane,
        "cap" to Cap,
        "chat-favourite-01" to ChatFavourite01,
        "chat-lock" to ChatLock,
        "check-in" to CheckIn,
        "circle-dashed" to CircleDashed,
        "circle" to Circle,
        "close-x" to CloseX,
        "cloud-backup" to CloudBackup,
        "cloud-download" to CloudDownload,
        "cloud-error-alert" to CloudErrorAlert,
        "cloud-loading" to CloudLoading,
        "cloud-off-unavailable" to CloudOffUnavailable,
        "cloud-saving-done-01" to CloudSavingDone01,
        "cloud-saving-done-02" to CloudSavingDone02,
        "cloud-upload" to CloudUpload,
        "collar" to Collar,
        "connect-to-the-device" to ConnectToTheDevice,
        "cpu-chip" to CpuChip,
        "credit-card" to CreditCard,
        "cross" to Cross,
        "crown" to Crown,
        "cyber" to Cyber,
        "daily-reminder" to DailyReminder,
        "dashboard-circle-edit" to DashboardCircleEdit,
        "delete-bin" to DeleteBin,
        "device-settings" to DeviceSettings,
        "download" to Download,
        "edit" to Edit,
        "elderly" to Elderly,
        "emergency-contacts" to EmergencyContacts,
        "enter-a-place" to EnterAPlace,
        "eye-off-hidden" to EyeOffHidden,
        "eye-on" to EyeOn,
        "fall-detection" to FallDetection,
        "favourite-place" to FavouritePlace,
        "favourite" to Favourite,
        "file-export" to FileExport,
        "find-the-device" to FindTheDevice,
        "fire-extinguisher" to FireExtinguisher,
        "fire" to Fire,
        "first-aid" to FirstAid,
        "flask-test-tube" to FlaskTestTube,
        "folder-add" to FolderAdd,
        "folder-cloud" to FolderCloud,
        "folder-favourite" to FolderFavourite,
        "folder-lock" to FolderLock,
        "folder-share" to FolderShare,
        "footprint-steps-activity" to FootprintStepsActivity,
        "full-screen" to FullScreen,
        "full-signal" to FullSignal,
        "geometric-shapes-01" to GeometricShapes01,
        "google-logo-outline" to GoogleLogoOutline,
        "google-logo" to GoogleLogo,
        "gps-disconnected" to GpsDisconnected,
        "gps-no-signal-02" to GpsNoSignal02,
        "gps-signal-01" to GpsSignal01,
        "gps" to Gps,
        "hand-pointing-up" to HandPointingUp,
        "hard-drive-download" to HardDriveDownload,
        "hard-drive-upload" to HardDriveUpload,
        "heart-with-pulse" to HeartWithPulse,
        "helmet" to Helmet,
        "help-circle" to HelpCircle,
        "help-square" to HelpSquare,
        "history" to History,
        "hourglass-timer" to HourglassTimer,
        "image-add" to ImageAdd,
        "info" to Info,
        "information-square" to InformationSquare,
        "internet" to Internet,
        "key-password" to KeyPassword,
        "kid" to Kid,
        "leave-a-place" to LeaveAPlace,
        "light-bulb" to LightBulb,
        "lights" to Lights,
        "location-check" to LocationCheck,
        "log-sign-in" to LogSignIn,
        "log-sign-out" to LogSignOut,
        "lost-pet" to LostPet,
        "loud-environment" to LoudEnvironment,
        "low-signal" to LowSignal,
        "mail-send-invite" to MailSendInvite,
        "mail-sign-in" to MailSignIn,
        "mail-sync-recovery" to MailSyncRecovery,
        "medical-id" to MedicalId,
        "medium-signal" to MediumSignal,
        "message-inbox" to MessageInbox,
        "microphone-off" to MicrophoneOff,
        "microphone" to Microphone,
        "moon" to Moon,
        "navbar-board" to NavbarBoard,
        "navbar-circle" to NavbarCircle,
        "navbar-device" to NavbarDevice,
        "navbar-safety" to NavbarSafety,
        "navigation-07" to Navigation07,
        "necklace" to Necklace,
        "network-nodes" to NetworkNodes,
        "new-releases" to NewReleases,
        "nfc" to Nfc,
        "no-internet" to NoInternet,
        "notification-bell-do-not-disturb" to NotificationBellDoNotDisturb,
        "notification-bell" to NotificationBell,
        "ocean" to Ocean,
        "padlock-locked" to PadlockLocked,
        "padlock-unlocked" to PadlockUnlocked,
        "paired-devices" to PairedDevices,
        "parental-control" to ParentalControl,
        "pause" to Pause,
        "paw" to Paw,
        "pdf" to Pdf,
        "pharmacy" to Pharmacy,
        "pin-code" to PinCode,
        "pin-location" to PinLocation,
        "play" to Play,
        "plug-socket" to PlugSocket,
        "plus-add" to PlusAdd,
        "police-badge" to PoliceBadge,
        "police" to Police,
        "pulse" to Pulse,
        "qr-code" to QrCode,
        "quiet-hours" to QuietHours,
        "radar-broadcast" to RadarBroadcast,
        "rainbow" to Rainbow,
        "receipt-invoice" to ReceiptInvoice,
        "refresh" to Refresh,
        "reminders" to Reminders,
        "repeating-check-in" to RepeatingCheckIn,
        "restart" to Restart,
        "ring-the-device" to RingTheDevice,
        "rope" to Rope,
        "route-navigation" to RouteNavigation,
        "safe-zone" to SafeZone,
        "search-01" to Search01,
        "send-message-diagonal" to SendMessageDiagonal,
        "send-message" to SendMessage,
        "share" to Share,
        "shield-off" to ShieldOff,
        "shield-with-keyhole" to ShieldWithKeyhole,
        "shield-with-padlock" to ShieldWithPadlock,
        "silent-sos" to SilentSos,
        "sim-and-sms" to SimAndSms,
        "sleeping" to Sleeping,
        "smart-home" to SmartHome,
        "sms-feedback-alert" to SmsFeedbackAlert,
        "snow" to Snow,
        "source-code" to SourceCode,
        "speedometer" to Speedometer,
        "spirals" to Spirals,
        "square-check-big" to SquareCheckBig,
        "star-off" to StarOff,
        "star" to Star,
        "stop" to Stop,
        "sun-moon" to SunMoon,
        "sun" to Sun,
        "sunset" to Sunset,
        "telemetry" to Telemetry,
        "telephone-call" to TelephoneCall,
        "temperature-hot" to TemperatureHot,
        "text-as-well" to TextAsWell,
        "thumbs-down" to ThumbsDown,
        "thumbs-up" to ThumbsUp,
        "tick-02" to Tick02,
        "toggle-off" to ToggleOff,
        "toggle-on" to ToggleOn,
        "top-right-settings" to TopRightSettings,
        "torch" to Torch,
        "torus-icon" to TorusIcon,
        "two-way-fork" to TwoWayFork,
        "umbrella" to Umbrella,
        "user-add" to UserAdd,
        "user-circle" to UserCircle,
        "user-group" to UserGroup,
        "user-remove" to UserRemove,
        "user" to User,
        "uv-index" to UvIndex,
        "version" to Version,
        "walkie-talkie" to WalkieTalkie,
        "walking-person" to WalkingPerson,
        "wave-triangle" to WaveTriangle,
        "webhook" to Webhook,
        "welcome-sparkles" to WelcomeSparkles,
        "widget-grid-tile" to WidgetGridTile,
        "wifi-01" to Wifi01,
        "wind" to Wind,
        "wrist-watch" to WristWatch,
    )
}
