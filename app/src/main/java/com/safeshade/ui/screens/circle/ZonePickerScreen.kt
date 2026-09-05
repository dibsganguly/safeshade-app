package com.safeshade.ui.screens.circle

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.Stroke
import com.safeshade.ui.theme.board

/** Everything the map picker draws. */
data class ZonePickerUiState(
    /** The chosen point, or null until one is chosen. */
    val lat: Double? = null,
    val lon: Double? = null,
    /** Drawn as a ring on the map so the radius is judged against real streets. */
    val radiusMeters: Float = 200f,
    /**
     * The coordinate fields as typed.
     *
     * Held as strings rather than as doubles because half-typed input
     * ("22.", "-") is not a number and a field that refuses to hold what the
     * user is in the middle of typing is unusable.
     */
    val latField: String = "",
    val lonField: String = "",
    /** A reverse-geocoded name when one is known. Never required. */
    val placeLabel: String? = null,
    val isLocating: Boolean = false,
    /** Why the current-location button did not work. Plain, not apologetic. */
    val locationError: String? = null,
    val canConfirm: Boolean = false
)

/**
 * The safe-zone map picker.
 *
 * This is a **full route**, not a section of the editor, and that is a
 * structural requirement rather than a layout preference. A `WebView` inside a
 * scrolling parent fights the parent for every vertical drag — panning the map
 * scrolls the page instead — and any parent that recycles its children throws
 * the WebView away and reloads the map, losing the user's position. Giving it
 * its own route with a fixed-size slot removes both problems by construction.
 *
 * The coordinate fields below the map are **always visible**. They are not an
 * error state and not a fallback the user has to discover: on a phone with no
 * data connection the map is a blank ground, and the screen still has to work
 * completely. Hiding the only usable control behind a failure the app cannot
 * detect reliably would make an offline device unable to add a safe zone at
 * all.
 */
@Composable
fun ZonePickerScreen(
    state: ZonePickerUiState,
    onLatFieldChange: (String) -> Unit,
    onLonFieldChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onPointPicked: (Double, Double) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            // The theme draws edge to edge, so the window does not resize
            // when the keyboard opens: without this inset the coordinate
            // fields go behind it, and those fields are the whole reason this
            // route still works with no tiles.
            .imePadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Close the map without choosing a place",
                    tint = colors.inkMuted
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Choose the centre",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.ink
                )
                Text(
                    text = "Tap the map, or type the coordinates below",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }

        // The map takes whatever height is left after the panel below has
        // taken what it needs. That ordering is deliberate: at a large font
        // scale the panel grows and the map shrinks, rather than the panel
        // being pushed off the bottom of a screen that cannot scroll.
        MapSurface(
            state = state,
            onPointPicked = onPointPicked,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        BoardPlate(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = Radius.card, topEnd = Radius.card)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                    BoardField(
                        value = state.latField,
                        onValueChange = onLatFieldChange,
                        label = "Latitude",
                        placeholder = "22.5726",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    BoardField(
                        value = state.lonField,
                        onValueChange = onLonFieldChange,
                        label = "Longitude",
                        placeholder = "88.3639",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.placeLabel != null) {
                    Text(
                        text = state.placeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.inkMuted
                    )
                }

                if (state.locationError != null) {
                    Text(
                        text = state.locationError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkAttention
                    )
                }

                BoardButton(
                    label = if (state.isLocating) "Finding you" else "Use my current location",
                    icon = SafeShadeIcons.Gps,
                    onClick = onUseCurrentLocation,
                    enabled = !state.isLocating,
                    weight = ButtonWeight.SECONDARY,
                    modifier = Modifier.fillMaxWidth()
                )

                BoardButton(
                    label = "Use this place",
                    supporting = if (state.canConfirm) null else "Choose a point on the map or type both coordinates",
                    onClick = onConfirm,
                    enabled = state.canConfirm,
                    weight = ButtonWeight.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * The WebView, and every decision about it that is easy to get wrong.
 *
 * `loadUrl` is called in `factory` and nowhere else. Calling it from `update`
 * — the obvious place, because that is where the current state is — reloads
 * the page on every recomposition, which resets the map's centre and zoom
 * every time an unrelated field changes. State reaches the loaded page through
 * `evaluateJavascript` instead.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MapSurface(
    state: ZonePickerUiState,
    onPointPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    // A WebView cannot render in the preview renderer, and an empty grey box
    // is a worse thing to look at than the panel it sits above. Skipping it in
    // inspection mode also makes the preview show what an offline phone shows.
    if (LocalInspectionMode.current) {
        MapPlaceholder(modifier = modifier)
        return
    }

    // Callbacks captured by the factory would otherwise be frozen at the
    // values they had on first composition.
    val currentOnPointPicked by rememberUpdatedState(onPointPicked)

    // Built once. The initial coordinates ride in on the query string because
    // evaluateJavascript before onPageFinished is silently dropped, so the
    // first point has to be part of the load itself. The theme travels the
    // same way: prefers-color-scheme inside a WebView needs API 33+ or the
    // deprecated forceDark, and minSdk here is 26.
    val dark = colors.isDark
    val initialUrl = remember {
        buildString {
            append("file:///android_asset/map/map.html")
            append("?lat=").append(state.lat ?: DEFAULT_LAT)
            append("&lon=").append(state.lon ?: DEFAULT_LON)
            append("&radius=").append(state.radiusMeters.toInt())
            append("&dark=").append(if (dark) "1" else "0")
        }
    }

    AndroidView(
        modifier = modifier,
        // A WebView that outlives its composition keeps a renderer process and
        // its own timers alive; destroy() is the only thing that stops both.
        // onRelease is the hook that runs when the node leaves the tree.
        onRelease = { it.destroy() },
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true // Leaflet
                // Not enabled: file access, content access, geolocation. The
                // page reads one bundled asset and needs none of them, and a
                // picker screen is not a place to widen a WebView's reach.
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest
                    ): Boolean {
                        val url = request.url
                        // The page reports a picked point by navigating to a
                        // custom scheme. That is used instead of
                        // addJavascriptInterface so no Kotlin object is
                        // injected into the page at all.
                        if (url.scheme == "safeshade" && url.host == "pick") {
                            val pickedLat = url.getQueryParameter("lat")?.toDoubleOrNull()
                            val pickedLon = url.getQueryParameter("lon")?.toDoubleOrNull()
                            if (pickedLat != null && pickedLon != null) {
                                currentOnPointPicked(pickedLat, pickedLon)
                            }
                            return true
                        }
                        // Anything else is refused. This WebView shows one
                        // local page; it must not turn into a browser because
                        // a tile host answered with a redirect.
                        return true
                    }
                }

                loadUrl(initialUrl)
            }
        },
        update = { view ->
            val lat = state.lat
            val lon = state.lon
            if (lat != null && lon != null) {
                // Guarded on the function existing: if Leaflet never loaded,
                // the page defines nothing and this is a no-op rather than a
                // console error on every keystroke.
                view.evaluateJavascript(
                    "window.safeshadeSetPoint && window.safeshadeSetPoint($lat, $lon, ${state.radiusMeters});",
                    null
                )
            }
        }
    )
}

/** What the map slot shows where a WebView cannot run. */
@Composable
private fun MapPlaceholder(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    Box(
        modifier = modifier
            .background(colors.recess)
            .border(Stroke.hairline, colors.hairline),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Nameplate("Map", small = true, muted = true)
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = "The map needs a data connection. The coordinates below work either way.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.inkFaint,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = Spacing.xl)
                    .width(260.dp)
            )
        }
    }
}

/** Kolkata. Only ever used when no point has been chosen and none is known. */
private const val DEFAULT_LAT = 22.5726
private const val DEFAULT_LON = 88.3639

@Preview(name = "Zone picker — light", showBackground = true, heightDp = 780)
@Composable
private fun ZonePickerLightPreview() {
    SafeShadeTheme(darkTheme = false) {
        ZonePickerScreen(
            state = ZonePickerUiState(
                lat = 22.5726,
                lon = 88.3639,
                radiusMeters = 250f,
                latField = "22.5726",
                lonField = "88.3639",
                placeLabel = "Near Salt Lake Sector V",
                canConfirm = true
            ),
            onLatFieldChange = {},
            onLonFieldChange = {},
            onUseCurrentLocation = {},
            onPointPicked = { _, _ -> },
            onConfirm = {},
            onCancel = {}
        )
    }
}

@Preview(name = "Zone picker — dark, nothing chosen", showBackground = true, heightDp = 780)
@Composable
private fun ZonePickerDarkPreview() {
    SafeShadeTheme(darkTheme = true) {
        ZonePickerScreen(
            state = ZonePickerUiState(
                locationError = "Location is switched off on this phone. Type the coordinates instead."
            ),
            onLatFieldChange = {},
            onLonFieldChange = {},
            onUseCurrentLocation = {},
            onPointPicked = { _, _ -> },
            onConfirm = {},
            onCancel = {}
        )
    }
}
