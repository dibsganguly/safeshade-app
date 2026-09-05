package com.safeshade.ui.screens.circle

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
import java.io.File
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.util.GeoPoint
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.config.Configuration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.ScreenTier
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
 * structural requirement rather than a layout preference. A map inside a
 * scrolling parent fights the parent for every vertical drag — panning the map
 * scrolls the page instead — and any parent that recycles its children throws
 * the map away and rebuilds it, losing the user's position. Giving it its own
 * route with a fixed-size slot removes both problems by construction.
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
        // ScreenHeader, not a hand-rolled row.
        //
        // This screen carried the fourth of the four header patterns the
        // convergence was meant to remove, and it survived the whole rewrite of
        // this file because replacing a WebView with a MapView never touched
        // the top of it. Measured on the device it was visibly the odd one out:
        // its title sat at `titleMedium` in a 51px box indented to x=154, where
        // every other sub-page sits at `headlineMedium` in a 66px box indented
        // to x=187.
        //
        // The control is a back arrow now rather than a close cross. This is a
        // pushed destination on the nav stack like any other, so the arrow is
        // the honest affordance; the spoken description keeps saying what
        // leaving actually costs.
        ScreenHeader(
            title = "Choose the centre",
            subtitle = "Tap the map, or type the coordinates below",
            onBack = onCancel,
            backDescription = "Close the map without choosing a place",
            tier = ScreenTier.PUSHED,
            modifier = Modifier.padding(start = Spacing.gutter, end = Spacing.gutter, top = Spacing.sm)
        )

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
 * The map.
 *
 * Native osmdroid, replacing a `WebView` that loaded Leaflet **from a CDN** and
 * tiled from OpenStreetMap. That arrangement had a failure mode worth
 * describing, because it is why this screen was reported as doing nothing: when
 * the CDN script failed to load — no data, a captive portal, a blocked host —
 * the page's own guard returned early, *before* registering the tap handler. So
 * the screen still opened, still drew its chrome, and simply could not be
 * tapped. Silent, and indistinguishable from a dead button.
 *
 * The native map removes the JavaScript dependency entirely and caches tiles on
 * disk, so a second visit to a place works with no connection at all. Tiles
 * still need the network the first time; nothing can change that, which is why
 * the coordinate fields below stay.
 *
 * The `MapView` is built in `remember`, not in `factory`, so it survives
 * recomposition — `update` then only swaps the overlays rather than rebuilding
 * the map and losing the user's centre and zoom.
 */
@Composable
private fun MapSurface(
    state: ZonePickerUiState,
    onPointPicked: (Double, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board
    val context = LocalContext.current

    // No MapView in the preview renderer, and a grey rectangle is a worse thing
    // to look at than the panel below it. Skipping it in inspection mode also
    // shows what a phone with no tiles yet shows.
    if (LocalInspectionMode.current) {
        MapPlaceholder(modifier = modifier)
        return
    }

    // Callbacks captured by the factory would otherwise be frozen at the values
    // they held on first composition.
    val currentOnPointPicked by rememberUpdatedState(onPointPicked)

    // osmdroid keeps its tile cache and user agent in a process-wide singleton
    // and will refuse to fetch without the agent set. Done once, here, because
    // this is the only screen that draws a map.
    LaunchedEffect(Unit) {
        Configuration.getInstance().apply {
            userAgentValue = context.packageName
            osmdroidBasePath = File(context.cacheDir, "osmdroid")
            osmdroidTileCache = File(osmdroidBasePath, "tiles")
        }
    }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(16.0)
            controller.setCenter(GeoPoint(state.lat ?: DEFAULT_LAT, state.lon ?: DEFAULT_LON))

            // A single tap sets the centre. `MapEventsOverlay` is the supported
            // way in: attaching a raw touch listener would fight the map's own
            // pan and zoom gestures.
            overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                        if (p != null) currentOnPointPicked(p.latitude, p.longitude)
                        return true
                    }

                    override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
            )
        }
    }

    // The map is a View with its own lifecycle. Without these it keeps its tile
    // threads running while the app is in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { view ->
            val lat = state.lat
            val lon = state.lon

            view.overlays.removeAll { it is Marker || it is Polygon }
            if (lat != null && lon != null) {
                val point = GeoPoint(lat, lon)

                // The radius circle, drawn as a polygon because osmdroid has no
                // circle primitive that follows the projection. Filled at low
                // alpha so the map underneath still reads - the point of a map
                // here is to recognise the place, not to admire the circle.
                view.overlays.add(
                    Polygon(view).apply {
                        points = Polygon.pointsAsCircle(point, state.radiusMeters.toDouble())
                        fillPaint.color = colors.accentSky.copy(alpha = 0.18f).toArgb()
                        outlinePaint.color = colors.accentSky.toArgb()
                        outlinePaint.strokeWidth = 4f
                    }
                )
                view.overlays.add(
                    Marker(view).apply {
                        position = point
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = "Zone centre"
                    }
                )
                view.controller.animateTo(point)
            }
            view.invalidate()
        }
    )
}

/** What the map slot shows where a MapView cannot run. */
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
