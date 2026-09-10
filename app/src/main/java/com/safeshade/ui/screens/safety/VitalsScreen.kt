package com.safeshade.ui.screens.safety

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardButton
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.ButtonWeight
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.Readout
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/** Where the phone can get a reading from, as the page reports it. */
enum class HealthConnectState {
    /** Health Connect is not on this phone. The row opens the Play Store. */
    NOT_INSTALLED,
    /** Installed, but too old for the client the app was built with. */
    NEEDS_UPDATE,
    /** Installed; the wearer has not yet allowed SafeShade to read. */
    NO_PERMISSION,
    /** Installed and allowed. */
    READY,
    /** This phone cannot run Health Connect at all. */
    UNSUPPORTED
}

/** One stored reading, already reduced to what the row prints. */
data class VitalsHistoryRow(
    val at: Long,
    val heartRateBpm: Int?,
    val spo2Percent: Int?,
    val tempC: Float?,
    /** "Wearable" or "Phone". */
    val sourceLabel: String
)

/** Everything the vitals page draws. Nulls are dashes. */
data class VitalsUiState(
    val wearerName: String = "",
    val heartRateBpm: Int? = null,
    val spo2Percent: Int? = null,
    val tempC: Float? = null,
    /** When the newest of the three above was measured. */
    val measuredAt: Long? = null,
    /** "Wearable", "Phone", or null when there is no reading at all. */
    val sourceLabel: String? = null,
    /** True while the wearable is on the link and its telemetry is arriving. */
    val deviceLive: Boolean = false,
    /** True once the wearable has sent a vitals field this session. */
    val deviceReportsVitals: Boolean = false,
    val healthConnect: HealthConnectState = HealthConnectState.NOT_INSTALLED,
    /** The app Health Connect says wrote the newest reading, e.g. "Mi Fitness". */
    val healthConnectOrigin: String? = null,
    /** True while a Health Connect read is in flight. */
    val reading: Boolean = false,
    /** The last read's failure, in plain English, or null. */
    val readError: String? = null,
    val hrLow: Int = 40,
    val hrHigh: Int = 130,
    val spo2Low: Int = 90,
    val tempHigh: Float = 38.0f,
    /** Threshold breaches on the newest reading, already worded. */
    val flags: List<String> = emptyList(),
    val history: List<VitalsHistoryRow> = emptyList()
)

/**
 * Vitals: heart rate, blood oxygen and temperature.
 *
 * The top plate is three readouts and one line saying where they came from
 * and when. A dash is a dash: the page never draws a figure that was not
 * measured, and the source line is the proof of provenance a guardian can
 * check. Two sources feed it. The wearable's telemetry carries vitals fields
 * when a sensor is fitted; Health Connect carries whatever another app on
 * the phone has measured, and the row for it reports the exact state that
 * stands between the guardian and a reading, with the one action that fixes
 * it.
 *
 * The thresholds are dials because they are quantities, and they commit
 * together on release, on one row of the store.
 */
@Composable
fun VitalsScreen(
    state: VitalsUiState,
    onReadNow: () -> Unit,
    onInstallHealthConnect: () -> Unit,
    onAllowHealthConnect: () -> Unit,
    onThresholds: (hrLow: Int, hrHigh: Int, spo2Low: Int, tempHigh: Float) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val whose = if (state.wearerName.isBlank()) "the wearer" else state.wearerName

    var hrLow by remember(state.hrLow) { mutableFloatStateOf(state.hrLow.toFloat()) }
    var hrHigh by remember(state.hrHigh) { mutableFloatStateOf(state.hrHigh.toFloat()) }
    var spo2Low by remember(state.spo2Low) { mutableFloatStateOf(state.spo2Low.toFloat()) }
    var tempHigh by remember(state.tempHigh) { mutableFloatStateOf(state.tempHigh) }
    val commit = { onThresholds(hrLow.roundToInt(), hrHigh.roundToInt(), spo2Low.roundToInt(), (tempHigh * 10f).roundToInt() / 10f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ground)
            .verticalScroll(rememberScrollState())
            .padding(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl
            )
    ) {
        ScreenHeader(
            title = "Vitals",
            subtitle = "Heart, oxygen and temperature",
            onBack = onBack
        )

        SectionPlate(title = "Right now")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(Spacing.lg),
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Readout(
                    label = "HEART",
                    value = state.heartRateBpm?.toString() ?: "—",
                    large = true,
                    state = lampFor(state.flags.any { it.startsWith("Heart") }, state.heartRateBpm),
                    modifier = Modifier.weight(1f)
                )
                Readout(
                    label = "OXYGEN",
                    value = state.spo2Percent?.let { "$it%" } ?: "—",
                    large = true,
                    state = lampFor(state.flags.any { it.startsWith("Oxygen") }, state.spo2Percent),
                    modifier = Modifier.weight(1f)
                )
                Readout(
                    label = "TEMP",
                    value = state.tempC?.let { "%.1f°".format(it) } ?: "—",
                    large = true,
                    state = lampFor(state.flags.any { it.startsWith("Temperature") }, state.tempC),
                    modifier = Modifier.weight(1f)
                )
            }
            Hairline()
            Text(
                text = sourceLine(state),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)
            )
        }

        if (state.flags.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.flags.forEachIndexed { i, flag ->
                    if (i > 0) Hairline()
                    Way(
                        name = flag,
                        state = LampState.ATTENTION,
                        stateLabel = "Outside range",
                        icon = SafeShadeIcons.HeartWithPulse
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Where readings come from")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            Way(
                name = "Wearable",
                state = when {
                    state.deviceReportsVitals && state.deviceLive -> LampState.LIVE
                    state.deviceLive -> LampState.OFF
                    else -> LampState.UNKNOWN
                },
                stateLabel = when {
                    state.deviceReportsVitals && state.deviceLive -> "Reporting"
                    state.deviceLive -> "No sensor reading"
                    else -> "—"
                },
                detail = when {
                    state.deviceReportsVitals && state.deviceLive -> "Vitals arrive with the wearable's telemetry, about once a second."
                    state.deviceLive -> "Connected. Its telemetry carries no heart, oxygen or temperature field."
                    else -> "Off the link. Readings from the wearable arrive only while it is connected."
                },
                icon = SafeShadeIcons.Pulse
            )
            Hairline()
            val (hcLamp, hcWord, hcLine) = healthConnectWay(state, whose)
            Way(
                name = "Health Connect",
                state = hcLamp,
                stateLabel = hcWord,
                detail = hcLine,
                icon = SafeShadeIcons.HeartWithPulse,
                onClick = when (state.healthConnect) {
                    HealthConnectState.NOT_INSTALLED, HealthConnectState.NEEDS_UPDATE -> onInstallHealthConnect
                    HealthConnectState.NO_PERMISSION -> onAllowHealthConnect
                    else -> null
                }
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        when (state.healthConnect) {
            HealthConnectState.READY -> BoardButton(
                label = if (state.reading) "Reading…" else "Read Now",
                supporting = "The newest heart rate, oxygen and temperature any app has written.",
                icon = SafeShadeIcons.HeartWithPulse,
                onClick = onReadNow,
                enabled = !state.reading,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
            HealthConnectState.NOT_INSTALLED, HealthConnectState.NEEDS_UPDATE -> BoardButton(
                label = if (state.healthConnect == HealthConnectState.NEEDS_UPDATE) "Update Health Connect" else "Get Health Connect",
                supporting = "Opens the Play Store. Free, from Google.",
                icon = SafeShadeIcons.Download,
                onClick = onInstallHealthConnect,
                weight = ButtonWeight.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
            HealthConnectState.NO_PERMISSION -> BoardButton(
                label = "Allow Reading",
                supporting = "Health Connect asks which of the three SafeShade may read.",
                icon = SafeShadeIcons.HeartWithPulse,
                onClick = onAllowHealthConnect,
                weight = ButtonWeight.ATTENTION,
                modifier = Modifier.fillMaxWidth()
            )
            HealthConnectState.UNSUPPORTED -> Unit
        }
        if (state.readError != null) {
            Spacer(Modifier.height(Spacing.sm))
            FailureNote(text = state.readError)
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Tell me when")
        Spacer(Modifier.height(Spacing.sm))
        DialControl(
            label = "Heart rate below",
            value = hrLow,
            valueRange = 30f..70f,
            step = 5f,
            onValueChange = { hrLow = it },
            onCommit = commit,
            unit = "bpm",
            advice = { "Resting adults sit near 60 to 100. Below ${it.roundToInt()} at rest is worth a call." }
        )
        Spacer(Modifier.height(Spacing.md))
        DialControl(
            label = "Heart rate above",
            value = hrHigh,
            valueRange = 100f..180f,
            step = 5f,
            onValueChange = { hrHigh = it },
            onCommit = commit,
            unit = "bpm",
            advice = { "Climbing stairs reaches this. Sitting still and reaching ${it.roundToInt()} does not." }
        )
        Spacer(Modifier.height(Spacing.md))
        DialControl(
            label = "Oxygen below",
            value = spo2Low,
            valueRange = 80f..95f,
            step = 1f,
            onValueChange = { spo2Low = it },
            onCommit = commit,
            unit = "percent",
            format = { "${it.roundToInt()}%" },
            advice = { if (it >= 94f) "Healthy lungs read 95 to 100. This trips on an ordinary dip." else "Below ${it.roundToInt()}% for more than a moment needs attention." }
        )
        Spacer(Modifier.height(Spacing.md))
        DialControl(
            label = "Temperature above",
            value = tempHigh,
            valueRange = 37f..40f,
            step = 0.5f,
            onValueChange = { tempHigh = it },
            onCommit = commit,
            unit = "°C",
            format = { "%.1f".format(it) },
            advice = { if (it < 38f) "Skin runs cooler than core; a warm room reaches this." else "A fever. %.1f° is where most clinicians start to act.".format(it) }
        )
        Spacer(Modifier.height(Spacing.sm))
        Footnote(text = "A reading outside a range lights the row above and the Safety page. Nobody is called: these are readings, not alerts.")

        if (state.history.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xl))
            SectionPlate(title = "Recent")
            Spacer(Modifier.height(Spacing.sm))
            BoardPlate(modifier = Modifier.fillMaxWidth()) {
                state.history.take(8).forEachIndexed { i, row ->
                    if (i > 0) Hairline()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Readout(value = clock(row.at), compact = true, modifier = Modifier.weight(1.2f))
                        Readout(value = row.heartRateBpm?.toString() ?: "—", label = "bpm", compact = true, modifier = Modifier.weight(1f))
                        Readout(value = row.spo2Percent?.let { "$it%" } ?: "—", label = "SpO₂", compact = true, modifier = Modifier.weight(1f))
                        Readout(value = row.tempC?.let { "%.1f°".format(it) } ?: "—", label = "temp", compact = true, modifier = Modifier.weight(1f))
                        Text(
                            text = row.sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.inkFaint,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private fun lampFor(flagged: Boolean, value: Number?): LampState? = when {
    value == null -> null
    flagged -> LampState.ATTENTION
    else -> LampState.LIVE
}

private fun sourceLine(s: VitalsUiState): String {
    if (s.sourceLabel == null || s.measuredAt == null) {
        return "No reading yet. Nothing here is drawn until something has been measured."
    }
    val from = if (s.sourceLabel == "Phone" && s.healthConnectOrigin != null) {
        "Health Connect (${s.healthConnectOrigin})"
    } else if (s.sourceLabel == "Phone") "Health Connect" else "the wearable"
    return "From $from · ${agoText(s.measuredAt)}"
}

private fun healthConnectWay(s: VitalsUiState, whose: String): Triple<LampState, String, String> = when (s.healthConnect) {
    HealthConnectState.NOT_INSTALLED -> Triple(LampState.OFF, "Not installed", "Google's health store for this phone. Watches and fitness apps write to it; SafeShade reads from it.")
    HealthConnectState.NEEDS_UPDATE -> Triple(LampState.ATTENTION, "Needs an update", "The installed Health Connect is older than the one SafeShade was built for.")
    HealthConnectState.NO_PERMISSION -> Triple(LampState.ATTENTION, "Not allowed", "SafeShade has not been allowed to read heart rate, oxygen or temperature.")
    HealthConnectState.READY -> Triple(
        LampState.LIVE, "Allowed",
        if (s.healthConnectOrigin != null) "Newest reading written by ${s.healthConnectOrigin}." else "Allowed. No app has written a reading for $whose yet."
    )
    HealthConnectState.UNSUPPORTED -> Triple(LampState.UNKNOWN, "—", "This phone cannot run Health Connect.")
}

internal fun agoText(at: Long, now: Long = System.currentTimeMillis()): String {
    val s = ((now - at) / 1000L).coerceAtLeast(0L)
    return when {
        s < 5 -> "just now"
        s < 60 -> "$s s ago"
        s < 3600 -> "${s / 60} min ago"
        s < 86_400 -> "${s / 3600} h ago"
        else -> "${s / 86_400} d ago"
    }
}

@Preview(name = "Vitals", showBackground = true, heightDp = 1500)
@Composable
private fun VitalsPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            VitalsScreen(
                state = VitalsUiState(
                    wearerName = "Baba",
                    heartRateBpm = 134,
                    spo2Percent = 97,
                    tempC = null,
                    measuredAt = System.currentTimeMillis() - 90_000L,
                    sourceLabel = "Phone",
                    healthConnect = HealthConnectState.READY,
                    healthConnectOrigin = "Mi Fitness",
                    flags = listOf("Heart rate 134, above 130"),
                    history = listOf(
                        VitalsHistoryRow(System.currentTimeMillis() - 90_000L, 134, 97, null, "Phone"),
                        VitalsHistoryRow(System.currentTimeMillis() - 3_600_000L, 71, 98, 36.4f, "Phone")
                    )
                ),
                onReadNow = {}, onInstallHealthConnect = {}, onAllowHealthConnect = {},
                onThresholds = { _, _, _, _ -> }, onBack = {}
            )
        }
    }
}
