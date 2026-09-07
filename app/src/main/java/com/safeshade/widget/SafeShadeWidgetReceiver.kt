package com.safeshade.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * The manifest-declared entry point (see `AndroidManifest.xml`'s
 * `.widget.SafeShadeWidgetReceiver`). Glance needs this thin `BroadcastReceiver`
 * subclass even though [SafeShadeWidget] itself carries all the real logic —
 * `AppWidgetManager` addresses widgets by receiver component, not by the
 * `GlanceAppWidget` class.
 */
class SafeShadeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SafeShadeWidget()
}
