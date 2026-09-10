package com.safeshade.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.safeshade.cloud.sync.SyncState
import com.safeshade.ui.theme.board

/**
 * Whether one record has reached SafeShade Cloud, as a 6dp lamp.
 *
 * Square, like the pilot lamp it is a miniature of: a 6dp square at the
 * tight radius, so a row's sync mark and the lamp on the plate above it read
 * as the same object at two sizes.
 *
 * Drawn only when there is something to say: a record with no cloud
 * history draws nothing, because "not synced" on a phone that is not
 * signed in is not a state, it is the absence of one. Teal once the row
 * landed, amber while it is queued or in flight, trip red when the outbox
 * gave up with a reason. The colour reports a circuit state, which is the
 * one thing colour is for here, and the spoken description carries the
 * same word so the dot is never colour alone.
 */
@Composable
fun SyncDot(state: SyncState?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.board
    val (fill, word) = when (state) {
        null, SyncState.LocalOnly -> return
        SyncState.Syncing -> colors.lampAttention to "Sending to SafeShade Cloud"
        is SyncState.Synced -> colors.lampLive to "On SafeShade Cloud"
        is SyncState.Failed -> colors.lampTrip to "Not on SafeShade Cloud: ${state.reason}"
    }
    Canvas(
        modifier = modifier
            .size(6.dp)
            .semantics { contentDescription = word }
    ) {
        drawRoundRect(color = fill, cornerRadius = CornerRadius(1.5.dp.toPx()))
    }
}

/**
 * The key a record's state is filed under in `Outbox.states`.
 *
 * That map is keyed by record id alone, not by `table/id`: the screens ask
 * "what happened to *this* trip" holding only the trip's id, and ids are
 * UUIDs, so a collision across tables is not a practical concern. The table
 * is taken anyway so a call site says which queue it means; the first version
 * of this built `"$table/$recordId"` from it, matched nothing, and every dot
 * stayed dark on a phone whose outbox had just sent five alerts.
 */
@Suppress("UNUSED_PARAMETER")
fun syncKey(table: String, recordId: String): String = recordId
