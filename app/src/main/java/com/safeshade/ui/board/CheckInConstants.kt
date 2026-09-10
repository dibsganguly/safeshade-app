package com.safeshade.ui.board

/**
 * The check-in interval control's range, step, default and formatting.
 *
 * Shared with `DeviceSettingsScreen` — both screens write the same
 * `EXT CHECKIN` field, and a user who changes this in one place and finds a
 * different control offering different options in the other is exactly the
 * confusion a second, drifted copy of this would cause. This is the one home
 * for it; `DeviceSettingsScreen` and `RemindersScreen` both import these
 * rather than keeping their own copy.
 *
 * This used to be a row of four buttons — 30m / 1h / 2h / 4h — which is why
 * the range now reaching every half hour in between (1h30, 2h30, 3h, 3h30)
 * is a deliberate change, not scope creep: a slider that only ever lands on
 * four of its eight possible ticks looks broken, so the step is the true
 * minute resolution the device honours rather than the old row's arbitrary
 * subset of it.
 */
val CHECK_IN_INTERVAL_RANGE = 30f..240f
const val CHECK_IN_INTERVAL_STEP = 30f

/** What a newly-enabled check-in defaults to, before anyone has chosen. */
const val DEFAULT_CHECK_IN_INTERVAL_MINUTES = 60
