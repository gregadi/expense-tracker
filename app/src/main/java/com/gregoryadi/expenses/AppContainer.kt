package com.gregoryadi.expenses

import android.content.Context
import androidx.room.Room
import com.gregoryadi.expenses.data.db.AppDatabase
import com.gregoryadi.expenses.data.port.DataPorter
import com.gregoryadi.expenses.data.prefs.SettingsStore
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.data.repository.LookupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    val expenses = ExpenseRepository(database.expenseDao())
    val lookups = LookupRepository(database.paymentMethodDao(), database.categoryDao())
    val porter = DataPorter(appContext, database)

    fun seedIfEmpty() {
        scope.launch { lookups.seedIfEmpty() }
    }
}
