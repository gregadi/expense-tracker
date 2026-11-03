package com.gregoryadi.expenses

import android.content.Context
import androidx.room.Room
import com.gregoryadi.expenses.data.db.AppDatabase
import com.gregoryadi.expenses.data.port.DataPorter
import com.gregoryadi.expenses.data.prefs.SettingsStore
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.data.repository.LookupRepository
import com.gregoryadi.expenses.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Hand-wired dependencies. At this size a manual container is simpler than Hilt;
 * the layering (screens -> ViewModels -> repositories -> DAOs) is the same either way.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase =
        Room.databaseBuilder(appContext, AppDatabase::class.java, "expenses.db")
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    val settings = SettingsStore(appContext)
    val widgets = WidgetRefresher(appContext, scope)
    val expenses = ExpenseRepository(database.expenseDao(), onChange = widgets::refresh)
    val lookups = LookupRepository(database.paymentMethodDao(), database.categoryDao(), onChange = widgets::refresh)
    val porter = DataPorter(appContext, database, onChange = widgets::refresh)

    /**
     * "Open the Add sheet" requests, e.g. from a widget tap. Conflated: only the latest matters,
     * and each is handled once.
     */
    val addRequests = Channel<AddRequest>(Channel.CONFLATED)

    fun seedIfEmpty() {
        scope.launch { lookups.seedIfEmpty() }
    }
}

/** Open the Add sheet, optionally with a payment method already picked. */
data class AddRequest(val methodId: Long?)
