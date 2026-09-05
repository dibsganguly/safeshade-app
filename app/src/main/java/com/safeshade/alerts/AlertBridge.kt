package com.safeshade.alerts

import android.content.Context
import com.safeshade.data.SafetySettings
import com.safeshade.repo.SafetyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Connects a detected fall to the notification a person actually sees.
 *
 * This is a small file and it is the single most important wire in the app.
 * Everything else about fall handling — the firmware detecting it, the alert
 * crossing the link, the repository recording it — was already working, and a
 * guardian whose phone was in a pocket still saw nothing, because the alert
 * only ever rendered as an in-app composable. That is the failure this closes.
 *
 * It lives on the application scope rather than in a ViewModel or a Composable
 * for the obvious reason: the case this exists for is a phone that is locked,
 * face down, with the app not running. There is no ViewModel then.
 *
 * The bridge only *presents*. Deciding what an expired countdown does — placing
 * the call, sending the SMS, recording the outcome — belongs to
 * `AlertActionReceiver`'s expiry path, which is driven by an alarm and
 * therefore survives the process being killed mid-countdown. Duplicating that
 * decision here would mean two things racing to resolve the same trip.
 */
object AlertBridge {

    fun start(context: Context, safety: SafetyRepository, scope: CoroutineScope) {
        val app = context.applicationContext

        safety.activeAlert
            // Keyed on identity rather than the object: the repository updates
            // the event in place as its outcome changes, and re-posting the
            // notification on every one of those would restart the countdown.
            .distinctUntilChanged { old, new -> old?.id == new?.id }
            .onEach { alert ->
                if (alert == null) {
                    AlertNotifier.cancel(app)
                } else {
                    // Read the current value rather than suspending for one.
                    // Settings are a `StateFlow<SafetySettings?>` seeded null
                    // until DataStore answers, and a fall alert must never wait
                    // on disk — if it has not loaded yet the documented default
                    // countdown is the right thing to use.
                    val countdown = safety.settings.value?.fallCountdownSeconds
                        ?: SafetySettings().fallCountdownSeconds
                    AlertNotifier.show(app, alert, countdown)
                }
            }
            .launchIn(scope)
    }
}
