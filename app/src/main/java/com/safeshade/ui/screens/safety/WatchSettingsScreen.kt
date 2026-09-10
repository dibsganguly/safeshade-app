package com.safeshade.ui.screens.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.safeshade.ui.board.BoardPlate
import com.safeshade.ui.board.DialControl
import com.safeshade.ui.board.Footnote
import com.safeshade.ui.board.Hairline
import com.safeshade.ui.board.LampState
import com.safeshade.ui.board.ScreenHeader
import com.safeshade.ui.board.SectionPlate
import com.safeshade.ui.board.Way
import com.safeshade.ui.icons.SafeShadeIcons
import com.safeshade.ui.theme.SafeShadeTheme
import com.safeshade.ui.theme.Spacing
import com.safeshade.ui.theme.board
import kotlin.math.roundToInt

/** Everything the out-of-reach page draws. */
data class WatchUiState(
    /** Zero is off. */
    val offlineAlertMinutes: Int = 120,
    /** Zero is off. */
    val lowBatteryPercent: Int = 15,
    val wearerName: String = "",
    val connected: Boolean = false,
    /** When the wearable was last on the link, while it is off it. */
    val offlineSince: Long? = null,
    /** Whether the out-of-reach notice has gone out for this outage. */
    val offlineAlerted: Boolean = false,
    /** Whether the low-battery notice has gone out for this discharge. */
    val lowBatteryAlerted: Boolean = false,
    /** The last real battery reading, or null when none has arrived. */
    val batteryPercent: Int? = null
)

/**
 * Out of reach: the two notices the phone raises about the wearable itself.
 *
 * The top plate is the wearable as the watch sees it this minute, in the
 * same words the notification would use, so a guardian can check the rule
 * against the thing it watches. The two dials below set the thresholds;
 * both write on release, together, because they live on one settings row
 * and two separate writes would lose one of them.
 *
 * Zero means off on both dials and is drawn as the word, not as a number a
 * person has to know the meaning of.
 */
@Composable
fun WatchSettingsScreen(
    state: WatchUiState,
    onChange: (offlineAlertMinutes: Int, lowBatteryPercent: Int) -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val colors = MaterialTheme.board
    val whose = if (state.wearerName.isBlank()) "The wearable" else "${state.wearerName}'s wearable"

    var offlineDraft by remember(state.offlineAlertMinutes) { mutableFloatStateOf(state.offlineAlertMinutes.toFloat()) }
    var batteryDraft by remember(state.lowBatteryPercent) { mutableFloatStateOf(state.lowBatteryPercent.toFloat()) }

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
            title = "Out of reach",
            subtitle = "When the phone tells you about the wearable itself",
            onBack = onBack
        )

        SectionPlate(title = "Right now")
        Spacer(Modifier.height(Spacing.sm))
        BoardPlate(modifier = Modifier.fillMaxWidth()) {
            val (linkLamp, linkWord, linkLine) = linkWay(state, whose)
            Way(
                name = "Link",
                state = linkLamp,
                stateLabel = linkWord,
                detail = linkLine,
                icon = SafeShadeIcons.Bluetooth
            )
            Hairline()
            val (battLamp, battWord, battLine) = batteryWay(state)
            Way(
                name = "Battery",
                state = battLamp,
                stateLabel = battWord,
                detail = battLine,
                icon = SafeShadeIcons.BatteryMedium01
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionPlate(title = "Tell me when")
        Spacer(Modifier.height(Spacing.sm))
        DialControl(
            label = "Out of reach for",
            value = offlineDraft,
            valueRange = 0f..240f,
            step = 30f,
            onValueChange = { offlineDraft = it },
            onCommit = { onChange(offlineDraft.roundToInt(), batteryDraft.roundToInt()) },
            format = { if (it < 1f) "Off" else minutesShort(it.roundToInt()) },
            advice = { offlineAdvice(it.roundToInt()) },
            spokenValue = { if (it < 1f) "Off" else "${it.roundToInt()} minutes" }
        )
        Spacer(Modifier.height(Spacing.md))
        DialControl(
            label = "Battery below",
            value = batteryDraft,
            valueRange = 0f..50f,
            step = 5f,
            onValueChange = { batteryDraft = it },
            onCommit = { onChange(offlineDraft.roundToInt(), batteryDraft.roundToInt()) },
            unit = "percent",
            format = { if (it < 1f) "Off" else "${it.roundToInt()}%" },
            advice = { if (it < 1f) "No battery notice." else "One notice when the wearable reports ${it.roundToInt()}% or less; again only after it has charged back up." },
            spokenValue = { if (it < 1f) "Off" else "${it.roundToInt()} percent" }
        )
        Spacer(Modifier.height(Spacing.sm))
        Footnote(text = "Each notice comes once: once per time out of reach, once per discharge. The phone checks every minute while it is running.")
    }
}

private fun linkWay(s: WatchUiState, whose: String): Triple<LampState, String, String> = when {
    s.connected -> Triple(LampState.LIVE, "Connected", "$whose is on the link now")
    s.offlineSince == null -> Triple(LampState.UNKNOWN, "—", "$whose has not connected to this phone yet")
    else -> {
        val mins = ((System.currentTimeMillis() - s.offlineSince) / 60_000L).toInt().coerceAtLeast(0)
        val told = when {
            s.offlineAlerted -> " · you were told"
            s.offlineAlertMinutes == 0 -> " · the notice is off"
            mins < s.offlineAlertMinutes -> " · notice at ${minutesShort(s.offlineAlertMinutes)}"
            else -> ""
        }
        Triple(LampState.ATTENTION, "Out of reach", "Since ${clock(s.offlineSince)} · ${minutesShort(mins)}$told")
    }
}

private fun batteryWay(s: WatchUiState): Triple<LampState, String, String> {
    val level = s.batteryPercent
    return when {
        level == null -> Triple(LampState.UNKNOWN, "—", "No reading yet. The wearable reports it while connected.")
        s.lowBatteryPercent > 0 && level <= s.lowBatteryPercent ->
            Triple(LampState.ATTENTION, "$level%", if (s.lowBatteryAlerted) "Below ${s.lowBatteryPercent}% · you were told" else "Below ${s.lowBatteryPercent}%")
        else -> Triple(LampState.LIVE, "$level%", if (s.connected) "Read just now" else "Last reading before the link dropped")
    }
}

private fun offlineAdvice(minutes: Int): String = when {
    minutes == 0 -> "No out-of-reach notice."
    minutes <= 30 -> "Short. A wearable left on its charger in the next room will trip this."
    minutes <= 120 -> "One notice after ${minutesShort(minutes)} without a connection."
    else -> "Long. A wearable left on a bus is reported ${minutesShort(minutes)} later."
}

internal fun minutesShort(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Preview(name = "Out of reach", showBackground = true, heightDp = 1200)
@Composable
private fun WatchPreview() {
    SafeShadeTheme {
        Box(Modifier.background(MaterialTheme.board.ground)) {
            WatchSettingsScreen(
                state = WatchUiState(
                    wearerName = "Baba",
                    connected = false,
                    offlineSince = System.currentTimeMillis() - 45 * 60_000L,
                    batteryPercent = 12
                ),
                onChange = { _, _ -> }, onBack = {}
            )
        }
    }
}
