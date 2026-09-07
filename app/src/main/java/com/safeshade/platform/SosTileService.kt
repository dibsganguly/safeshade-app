package com.safeshade.platform

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.safeshade.MainActivity
import com.safeshade.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether an alert is currently live, for the Quick Settings tile.
 *
 * Process-wide rather than owned by the tile, the same shape as
 * `service.JourneyService.LastKnownLocation`: a `TileService` is only
 * instantiated by the system while a tile is bound (added to the shade, or
 * being drawn), so it cannot itself be where "is an alert open right now" is
 * decided - that answer lives in `AppStateRepository.activeAlert`, which the
 * orchestrator (the app's `Application`/container wiring, outside this file's
 * scope) is expected to mirror in here on every change.
 *
 * Defaults to `false` (INACTIVE) so a tile added before the app has ever
 * updated this - the moment right after install - reads as "nothing wrong"
 * rather than a guess.
 */
object SosTileState {
    private val _alertActive = MutableStateFlow(false)
    val alertActive: StateFlow<Boolean> = _alertActive.asStateFlow()

    /** Called by the orchestrator whenever `activeAlert` changes. */
    fun update(active: Boolean) {
        _alertActive.value = active
    }
}

/**
 * The "SafeShade SOS" Quick Settings tile.
 *
 * ### It never sends anything
 *
 * A phone SOS needs contacts and the SEND_SMS grant (`canFireSos` /
 * `sosBlocker` in `SafeShadeViewModel`), and it is a five-second hold on the
 * bottom bar's disc for a reason: an SOS that could fire from a single stray
 * tap in the notification shade — glass in a pocket, a child's thumb — is a
 * false-alarm generator, not a safety feature. So [onClick] only ever opens
 * [MainActivity] with `ACTION_SOS`; MainActivity must route that to the
 * Safety tab, where the real hold-to-fire disc (`BoardBottomBar`'s `SosSlot`,
 * which is drawn on every tab) is what the user then has to hold.
 *
 * ### Tile state
 *
 * `Tile.STATE_ACTIVE` while [SosTileState.alertActive] is true, so a glance at
 * the shade shows whether help has already been raised before the tile is
 * even touched. `onStartListening`/`onStopListening` bracket a collector
 * because `TileService` is not itself a `LifecycleOwner` — nothing stops this
 * collecting forever if it were started once and never cancelled.
 */
class SosTileService : TileService() {

    private var listenJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        applyState(SosTileState.alertActive.value)
        listenJob = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).launch {
            SosTileState.alertActive.collect { active -> applyState(active) }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        listenJob?.cancel()
        listenJob = null
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_SOS
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        // API 34 requires the PendingIntent overload; the plain-Intent one is
        // deprecated there but is what every API 24-33 device still needs.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val pendingIntent = PendingIntent.getActivity(
                this,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            startActivityAndCollapse(pendingIntent)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun applyState(active: Boolean) {
        val tile = qsTile ?: return
        tile.label = "SafeShade SOS"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_sos_asterisk)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    companion object {
        private const val REQUEST_CODE = 4201

        /**
         * MainActivity must handle this: navigate to `Routes.SAFETY` (or simply
         * bring the app forward — the hold-to-fire SOS disc is drawn on every
         * tab's bottom bar) and never fire an SOS on its own from this action.
         */
        const val ACTION_SOS = "com.safeshade.action.SOS"
    }
}
