package com.safeshade.ui.screens.device

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.data.LiveSensorData
import com.safeshade.device.ConnectionState
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.EmptyBay
import com.safeshade.ui.board.Gauge
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Nameplate
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.shady.ShadyMood
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board

/** Everything the telemetry screen draws. */
data class TelemetryUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val sensors: LiveSensorData = LiveSensorData(),
    /** Battery percentages, oldest first. */
    val batteryHistory: List<Int> = emptyList(),
    /** Smoothed RSSI in dBm, oldest first. */
    val linkHistory: List<Int> = emptyList(),
    /** Preformatted by the caller, e.g. "312 samples over 5 minutes". */
    val historySummary: String? = null
)

/**
 * What the wearable is actually reporting.
 *
 * This screen exists to be checkable. A guardian who is not sure whether the
 * link is really live can come here and watch the accelerometer move, which is
 * a better answer than any status word.
 *
 * That only works if it never fabricates. `LiveSensorData` defaults every field
 * to zero, and a screen that renders those defaults shows a perfectly plausible
 * stationary device with a flat battery and no light — indistinguishable from a
 * real reading. So nothing is drawn at all until `isRealData` is true.
 */
@Composable
fun TelemetryScreen(
    state: TelemetryUiState,
    onReadSignal: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val hasData = state.sensors.isRealData

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = contentPadding.calculateTopPadding() + Spacing.sm,
            bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        item("title") {
            ScreenHeader(title = "Telemetry", onBack = onBack)
        }

        if (!hasData) {
            item("empty") {
                EmptyBay(
                    message = if (state.connection.isUsable) {
                        "No data yet. The wearable sends a reading about once a " +
                            "second once it has settled – this fills in shortly."
                    } else {
                        "No data yet. Telemetry arrives only while the device is " +
                            "connected."
                    },
                    // Readings that could arrive any second — a magnifying
                    // glass, not a plain statement.
                    shadyMood = ShadyMood.LOOKING
                )
            }
            return@LazyColumn
        }

        // ---------- Motion ----------
        item("motion-heading") { SectionPlate(title = "Motion") }

        item("motion") {
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    // Three identical readouts, deliberately. Colouring the
                    // axes would make hue mean "which axis", and hue in this
                    // system means circuit state and nothing else.
                    Readout(
                        label = "X",
                        value = "%+.2f".format(state.sensors.accelX),
                        modifier = Modifier.weight(1f)
                    )
                    Readout(
                        label = "Y",
                        value = "%+.2f".format(state.sensors.accelY),
                        modifier = Modifier.weight(1f)
                    )
                    Readout(
                        label = "Z",
                        value = "%+.2f".format(state.sensors.accelZ),
                        modifier = Modifier.weight(1f)
                    )
                }
                Hairline()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(Spacing.lg)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Nameplate("Magnitude", small = true, muted = true)
                        Text(
                            text = magnitudeNote(state.sensors.magnitudeG),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.inkFaint
                        )
                    }
                    Readout(
                        value = "%.2f g".format(state.sensors.magnitudeG),
                        large = true
                    )
                }
            }
        }

        // ---------- Environment ----------
        item("env-heading") { SectionPlate(title = "Environment") }

        item("env") {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Gauge(
                    label = "Temperature",
                    value = "%.1f".format(state.sensors.temperature),
                    unit = "°C",
                    modifier = Modifier.weight(1f)
                )
                Gauge(
                    label = "Light",
                    value = state.sensors.lightLevel.toString(),
                    caption = lightNote(state.sensors.lightLevel),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item("battery-gauge") {
            Gauge(
                label = "Battery",
                value = state.sensors.batteryLevel.toString(),
                unit = "%",
                state = when {
                    state.sensors.batteryLevel <= 15 -> LampState.TRIP
                    state.sensors.batteryLevel <= 30 -> LampState.ATTENTION
                    else -> null
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // ---------- History ----------
        item("history-heading") { SectionPlate(title = "History") }

        item("battery-spark") {
            SparkPlate(
                label = "Battery",
                points = state.batteryHistory.map { it.toFloat() },
                latest = "${state.sensors.batteryLevel}%",
                unitSpoken = "percent"
            )
        }

        item("link-spark") {
            SparkPlate(
                label = "Link quality",
                points = state.linkHistory.map { it.toFloat() },
                latest = state.linkHistory.lastOrNull()?.let { "$it dBm" } ?: "--",
                unitSpoken = "decibel milliwatts"
            )
        }

        if (state.historySummary != null) {
            item("history-note") {
                Text(
                    text = state.historySummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            }
        }

        item("read-signal") {
            BoardButton(
                label = "Take a signal reading",
                supporting = "Asks the radio for a fresh RSSI sample",
                onClick = onReadSignal,
                enabled = state.connection.isUsable,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A labelled sparkline.
 *
 * Drawn with a Compose `Path` rather than a charting library, which would be a
 * large dependency for two lines that carry no axes, no legend and no
 * interaction. The line is ink, not a lamp colour — a trend is not a circuit
 * state, and colouring it teal would suggest it is one.
 */
@Composable
private fun SparkPlate(
    label: String,
    points: List<Float>,
    latest: String,
    unitSpoken: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.board

    BoardPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Nameplate(label, small = true, muted = true, modifier = Modifier.weight(1f))
                Readout(value = latest)
            }
            Spacer(Modifier.height(Spacing.md))

            if (points.size < 2) {
                // One point is not a line, and Path with a single point draws
                // nothing at all — a silently blank plate rather than an honest
                // one. Say so instead.
                Text(
                    text = "Not enough samples to draw a line yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.inkFaint
                )
            } else {
                val min = points.minOrNull() ?: 0f
                val max = points.maxOrNull() ?: 0f
                val spokenRange = "$label from ${min.toInt()} to ${max.toInt()} $unitSpoken, " +
                    "${points.size} samples"

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clearAndSetSemantics { contentDescription = spokenRange }
                ) {
                    // A flat series has zero range. Dividing by it yields NaN
                    // for every point and the Path silently renders nothing, so
                    // a perfectly healthy steady battery would look like a bug.
                    // Draw it down the middle instead.
                    val range = (max - min).takeIf { it > 0.0001f }
                    val stepX = size.width / (points.size - 1)
                    val path = Path()

                    points.forEachIndexed { index, value ->
                        val normalised = if (range == null) 0.5f else (value - min) / range
                        val x = stepX * index
                        // Inset by the stroke width so the extremes are not
                        // clipped in half at the top and bottom edges.
                        val y = size.height - (normalised * (size.height - 4f)) - 2f
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = colors.ink,
                        style = DrawStroke(width = 2f, cap = StrokeCap.Round)
                    )
                }

                Spacer(Modifier.height(Spacing.xs))
                Row {
                    Text(
                        text = "${min.toInt()} low",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = "${max.toInt()} high",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.inkFaint
                    )
                }
            }
        }
    }
}

/** Plain-language reading of an accelerometer magnitude. */
private fun magnitudeNote(g: Float): String = when {
    g < 0.4f -> "In free fall or reading nothing"
    g < 1.3f -> "At rest, or moving gently"
    g < 2.5f -> "Being carried or walked with"
    else -> "A sharp movement"
}

/** Plain-language reading of the light sensor. */
private fun lightNote(level: Int): String = when {
    level < 40 -> "Dark – a pocket or a bag"
    level < 250 -> "Indoors"
    level < 700 -> "Bright indoors"
    else -> "Outdoors in daylight"
}

// ============================================================================
// Previews
// ============================================================================

private val previewTelemetry = TelemetryUiState(
    connection = ConnectionState.Ready,
    sensors = LiveSensorData(
        accelX = 0.04f,
        accelY = -0.11f,
        accelZ = 1.01f,
        temperature = 31.4f,
        lightLevel = 180,
        batteryLevel = 68,
        isRealData = true
    ),
    batteryHistory = listOf(74, 73, 73, 72, 71, 71, 70, 70, 69, 69, 68, 68),
    linkHistory = listOf(-58, -61, -60, -66, -72, -69, -64, -63, -61, -60, -62, -63),
    historySummary = "312 samples over the last five minutes"
)

@Composable
private fun TelemetryPreviewHost(state: TelemetryUiState) {
    Box(Modifier.background(MaterialTheme.board.ground)) {
        TelemetryScreen(state = state, onReadSignal = {})
    }
}

@Preview(name = "Telemetry · light", showBackground = true)
@Composable
private fun TelemetryPreviewLight() {
    SafeShadeTheme(darkTheme = false) { TelemetryPreviewHost(previewTelemetry) }
}

@Preview(name = "Telemetry · dark", showBackground = true)
@Composable
private fun TelemetryPreviewDark() {
    SafeShadeTheme(darkTheme = true) { TelemetryPreviewHost(previewTelemetry) }
}

@Preview(name = "Telemetry · no data", showBackground = true)
@Composable
private fun TelemetryPreviewNoData() {
    SafeShadeTheme(darkTheme = false) {
        TelemetryPreviewHost(TelemetryUiState(connection = ConnectionState.Ready))
    }
}

@Preview(name = "Telemetry · flat series", showBackground = true)
@Composable
private fun TelemetryPreviewFlat() {
    SafeShadeTheme(darkTheme = false) {
        TelemetryPreviewHost(
            previewTelemetry.copy(
                batteryHistory = List(12) { 88 },
                linkHistory = listOf(-70)
            )
        )
    }
}
