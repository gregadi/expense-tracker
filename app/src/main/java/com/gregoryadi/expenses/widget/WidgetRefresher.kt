package com.gregoryadi.expenses.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Tells every Expenses widget on the home screen to reload its numbers. */
class WidgetRefresher(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    fun refresh() {
        scope.launch { refreshAll(context) }
    }

    companion object {
        /**
         * Bumps each widget's version (so its content reloads even if it is already showing) and updates it.
         * Never throws: a widget problem must not break saving an expense.
         */
        suspend fun refreshAll(context: Context) {
            try {
                val widget = ExpenseWidget()
                GlanceAppWidgetManager(context).getGlanceIds(ExpenseWidget::class.java).forEach { id ->
                    updateAppWidgetState(context, id) { prefs ->
                        prefs[WidgetKeys.VERSION] = System.currentTimeMillis()
                    }
                    widget.update(context, id)
                }
            } catch (e: Exception) {
                Log.w("ExpenseWidget", "Widget refresh failed", e)
            }
        }
    }
}
