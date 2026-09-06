package com.safeshade.ui.screens.circle

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.Radius
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import java.io.File

/** One cell or point of heat: where, and how much. */
data class HeatPoint(
    val lat: Double,
    val lon: Double,
    /** Alerts counted here. Community cells are never below five by construction. */
    val count: Int,
    /** True for the household's own points, drawn as marks rather than washes. */
    val own: Boolean = false
)

/** Everything the heat map draws. */
data class HeatmapUiState(
    val centerLat: Double? = null,
    val centerLon: Double? = null,
    /** Community cells (1.1 km, k ≥ 5) from SafeShade Cloud. Empty when off, gated, or not fetched. */
    val cells: List<HeatPoint> = emptyList(),
    /** The household's own places: alerts with a fix, zones, the last fix. */
    val own: List<HeatPoint> = emptyList(),
    val communityOn: Boolean = false,
    /** True when the plan does not include the community layer. */
    val gated: Boolean = false,
    val tierLabel: String = "Free",
    /** The last fetch's outcome, in words, or null. */
    val status: String? = null,
    val loading: Boolean = false
)

/**
 * Where alerts happen.
 *
 * Two layers on one map. The household's own places are drawn as small
 * rings — their alerts, their zones, the last fix — because those are facts
 * this phone holds and a guardian may want to see them together. The
 * community layer is SafeShade Cloud's `heatmap_in`: 1.1 km cells with at
 * least five alerts each, drawn as flat amber washes stepped by count, so no
 * single household is ever visible in it. It is a paid layer and it is off
 * until the person turns it on, and the switch says both.
 *
 * Flat washes, not a gradient: the panel has no gradients, and a stepped
 * wash reads the same in greyscale as a smooth one does not.
 */
@Composable
fun HeatmapScreen(
    state: HeatmapUiState,
    onToggleCommunity: (Boolean) -> Unit,
    onOpenPlan: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.lg
            ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        ScreenHeader(
            title = "Where alerts happen",
            subtitle = "Your household's places, and the community's",
            onBack = onBack
        )

        HeatMapSurface(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .clip(RoundedCornerShape(Radius.card))
        )

        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Your household",
                state = if (state.own.isEmpty()) LampState.OFF else LampState.LIVE,
                stateLabel = if (state.own.isEmpty()) "Nothing yet" else "${state.own.size} places",
                detail = if (state.own.isEmpty()) "Alerts, zones and fixes appear here as they happen"
                else "Alerts with a fix, safe zones, and the last fix",
                icon = SafeShadeIcons.SafeZone
            )
            Hairline()
            Way(
                name = "Community",
                state = when {
                    state.gated -> LampState.OFF
                    state.communityOn && state.cells.isNotEmpty() -> LampState.LIVE
                    state.communityOn -> LampState.ATTENTION
                    else -> LampState.OFF
                },
                stateLabel = when {
                    state.gated -> "Plus"
                    state.loading -> "Loading"
                    state.communityOn -> "${state.cells.size} cells"
                    else -> "Off"
                },
                detail = when {
                    state.gated -> "Nearby alert density from every SafeShade household, 1 km cells, never fewer than five. Part of Plus."
                    else -> "1 km cells, never fewer than five alerts each, so nobody's home is visible in it."
                },
                icon = SafeShadeIcons.NavbarCircle,
                checked = if (state.gated) null else state.communityOn,
                onCheckedChange = if (state.gated) null else onToggleCommunity,
                // Gated: the row opens the plans. Otherwise the whole row
                // throws the switch; the track alone is a small target.
                onClick = if (state.gated) onOpenPlan else ({ onToggleCommunity(!state.communityOn) })
            )
            val status = state.status
            if (status != null) {
                Hairline()
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkAttention,
                    modifier = Modifier.padding(Spacing.lg)
                )
            }
        }

        if (state.gated) {
            BoardButton(
                label = "See the Plans",
                supporting = "You are on ${state.tierLabel}",
                onClick = onOpenPlan,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeatMapSurface(state: HeatmapUiState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    val context = LocalContext.current
    if (LocalInspectionMode.current) {
        androidx.compose.foundation.layout.Box(modifier = modifier.background(colors.recess))
        return
    }
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
            controller.setZoom(13.0)
            controller.setCenter(GeoPoint(state.centerLat ?: 20.35, state.centerLon ?: 85.82))
        }
    }
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
    val wash = colors.lampAttention.toArgb()
    val ownInk = colors.ink.toArgb()
    AndroidView(
        factory = { mapView },
        modifier = modifier.clipToBounds(),
        update = { map ->
            map.overlays.clear()
            // Community first, so the household's marks draw on top of it.
            val maxCount = state.cells.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
            state.cells.forEach { cell ->
                // Three flat steps, not a ramp: 0.14, 0.24, 0.36 alpha.
                val step = when {
                    cell.count >= maxCount * 2 / 3 -> 0.36f
                    cell.count >= maxCount / 3 -> 0.24f
                    else -> 0.14f
                }
                map.overlays.add(
                    Polygon(map).apply {
                        points = squareAround(cell.lat, cell.lon, 550.0)
                        fillPaint.color = withAlpha(wash, step)
                        outlinePaint.color = withAlpha(wash, 0.5f)
                        outlinePaint.strokeWidth = 2f
                    }
                )
            }
            state.own.forEach { p ->
                map.overlays.add(
                    Polygon(map).apply {
                        points = Polygon.pointsAsCircle(GeoPoint(p.lat, p.lon), 60.0)
                        fillPaint.color = withAlpha(ownInk, 0.10f)
                        outlinePaint.color = ownInk
                        outlinePaint.strokeWidth = 4f
                    }
                )
            }
            map.invalidate()
        }
    )
}

/** A square of side `halfSideM * 2` metres around a point, as the cell boundary. */
private fun squareAround(lat: Double, lon: Double, halfSideM: Double): List<GeoPoint> {
    val dLat = halfSideM / 111_320.0
    val dLon = halfSideM / (111_320.0 * Math.cos(Math.toRadians(lat)).coerceAtLeast(0.01))
    return listOf(
        GeoPoint(lat + dLat, lon - dLon),
        GeoPoint(lat + dLat, lon + dLon),
        GeoPoint(lat - dLat, lon + dLon),
        GeoPoint(lat - dLat, lon - dLon)
    )
}

private fun withAlpha(argb: Int, alpha: Float): Int =
    AndroidColor.argb((alpha * 255).toInt(), AndroidColor.red(argb), AndroidColor.green(argb), AndroidColor.blue(argb))

@Preview(name = "Heat map", showBackground = true)
@Composable
private fun HeatmapPreview() {
    SafeShadeTheme {
        HeatmapScreen(
            state = HeatmapUiState(own = listOf(HeatPoint(20.35, 85.82, 1, own = true)), gated = true),
            onToggleCommunity = {}, onOpenPlan = {}, onBack = {}
        )
    }
}
