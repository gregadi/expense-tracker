package com.gregoryadi.expenses.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ExpenseWidget()

    /**
     * The periodic update (every 30 minutes, see expense_widget_info.xml). Reloads the numbers rather than
     * only redrawing, so "today" rolls over after midnight. Replaces the default handling, which would
     * redraw a widget that is already showing without re-reading the database.
     */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                WidgetRefresher.refreshAll(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
