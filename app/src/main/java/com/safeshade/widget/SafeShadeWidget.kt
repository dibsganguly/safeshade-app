package com.safeshade.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.safeshade.MainActivity
import com.safeshade.platform.SosTileService

/**
 * Everything the widget draws, handed over by the orchestrator (outside this
 * file's scope) from `AppState.Ready` on every change.
 *
 * A plain snapshot rather than the live `StateFlow`s themselves: Glance's
 * `provideGlance` runs in its own process/worker on its own schedule, so the
 * only correct hand-off is "here is a value, now go persist and redraw" —
 * see [WidgetFeed.publish] — not a `Flow` this composable could collect,
 * which is not how Glance recomposition works.
 */
data class WidgetSnapshot(
    val wearerName: String?,
    val linkWord: String,
    val batteryPercent: Int?,
    val tripLine: String?,
    val alertOpen: Boolean
)

private val KeyWearer = stringPreferencesKey("wearer_name")
private val KeyLinkWord = stringPreferencesKey("link_word")
private val KeyBattery = intPreferencesKey("battery_percent")
private val KeyTripLine = stringPreferencesKey("trip_line")
private val KeyAlertOpen = booleanPreferencesKey("alert_open")

/** No reading — never rendered as 0, which reads as "the battery is dead." */
private const val NO_BATTERY_READING = -1

/**
 * Publishes a [WidgetSnapshot] into every placed instance of [SafeShadeWidget]
 * and asks Glance to redraw them.
 *
 * The orchestrator calls this whenever `AppState.Ready` changes any of the
 * fields the widget shows. A no-op (returns without writing) when the widget
 * has never been placed, so nothing here forces the app to touch Glance on
 * every state change for a user who has not added the widget.
 */
object WidgetFeed {
    suspend fun publish(context: Context, snapshot: WidgetSnapshot) {
        val ids = GlanceAppWidgetManager(context).getGlanceIds(SafeShadeWidget::class.java)
        if (ids.isEmpty()) return
        ids.forEach { id ->
            updateAppWidgetState(context, id) { prefs ->
                prefs[KeyWearer] = snapshot.wearerName.orEmpty()
                prefs[KeyLinkWord] = snapshot.linkWord
                prefs[KeyBattery] = snapshot.batteryPercent ?: NO_BATTERY_READING
                prefs[KeyTripLine] = snapshot.tripLine.orEmpty()
                prefs[KeyAlertOpen] = snapshot.alertOpen
            }
        }
        SafeShadeWidget().updateAll(context)
    }
}

/**
 * A 2x2-to-4x2 status tile: wearer, link state, battery, the open trip (or
 * "Nothing open"), and an SOS button.
 *
 * The SOS button never fires anything — same rule as [SosTileService]: it
 * opens MainActivity with `ACTION_SOS`, and MainActivity routes that to the
 * hold-to-fire disc. A tap on a home-screen widget is even easier to land by
 * accident than a tap in the notification shade, so this control has to be
 * *more* conservative about firing directly, not less.
 */
class SafeShadeWidget : GlanceAppWidget() {

    // The two cell shapes this widget is declared for in
    // res/xml/safeshade_widget_info.xml: a square 2x2 and a wide 4x2. Each
    // size gets its own composition rather than one layout doing double duty.
    override val sizeMode = SizeMode.Responsive(
        setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

// The board's own tokens (DESIGN.md front-matter), as literal Color values -
// this widget has no route into the app's Compose theme. Bone/night plate and
// ink swap by Glance's day/night ColorProvider; teal and trip-red are each a
// single fixed colour (teal only ever appears in day mode's palette here, and
// trip-red is deliberately the same in both - an open alert should not read
// differently by time of day), so both still go through the two-arg
// constructor rather than pulling in the separate single-colour overload.
private val BonePlate = ColorProvider(day = Color(0xFFFBF9F5), night = Color(0xFF22282E))
private val InkColor = ColorProvider(day = Color(0xFF22282E), night = Color(0xFFECEFF1))
private val TealAccent = ColorProvider(day = Color(0xFF6FD3CC), night = Color(0xFF6FD3CC))
private val TripRed = ColorProvider(day = Color(0xFFE5484D), night = Color(0xFFE5484D))
private val OnTripRed = ColorProvider(day = Color(0xFFFBF9F5), night = Color(0xFFFBF9F5))

@Composable
private fun WidgetContent() {
    val prefs = currentState<Preferences>()
    val wearer = prefs[KeyWearer]?.takeIf { it.isNotBlank() } ?: "—"
    val linkWord = prefs[KeyLinkWord]?.takeIf { it.isNotBlank() } ?: "—"
    val batteryPercent = prefs[KeyBattery]?.takeIf { it >= 0 }
    val batteryText = batteryPercent?.let { "$it%" } ?: "—"
    val tripLine = prefs[KeyTripLine]?.takeIf { it.isNotBlank() } ?: "Nothing open"
    val alertOpen = prefs[KeyAlertOpen] ?: false

    val plate = if (alertOpen) TripRed else BonePlate
    val ink = if (alertOpen) OnTripRed else InkColor
    val linkColor = if (alertOpen) OnTripRed else if (linkWord == "Connected") TealAccent else ink

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(plate)
            .cornerRadius(8.dp)
            .padding(12.dp)
    ) {
        Text(
            text = wearer,
            style = TextStyle(color = ink, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        Text(text = linkWord, style = TextStyle(color = linkColor))
        Spacer(modifier = GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(text = "Battery $batteryText", style = TextStyle(color = ink))
        }
        Text(text = tripLine, style = TextStyle(color = ink))
        Spacer(modifier = GlanceModifier.height(4.dp))
        SosButton(alertOpen)
    }
}

@Composable
private fun SosButton(alertOpen: Boolean) {
    val context = LocalContext.current
    val intent = Intent(context, MainActivity::class.java).apply {
        action = SosTileService.ACTION_SOS
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(if (alertOpen) OnTripRed else TripRed)
            .cornerRadius(8.dp)
            .padding(vertical = 6.dp, horizontal = 8.dp)
            .clickable(actionStartActivity(intent)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SOS",
            style = TextStyle(
                color = if (alertOpen) TripRed else OnTripRed,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
