package com.gregoryadi.expenses.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.gregoryadi.expenses.ExpensesApplication
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Period
import com.gregoryadi.expenses.domain.PeriodType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** Per-widget settings, stored by Glance for each widget on the home screen. */
object WidgetKeys {
    /** Background opacity, 0 (fully see-through) to 100 (solid). */
    val OPACITY = intPreferencesKey("opacity")
    const val DEFAULT_OPACITY = 85

    /** Changed by [WidgetRefresher] so the widget reloads its numbers. */
    val VERSION = longPreferencesKey("version")
}

/** Everything one widget shows, read once per refresh. */
data class WidgetData(
    val today: Long,
    val todayCount: Int,
    val monthSpent: Long,
    val monthIncome: Long,
    val monthName: String,
    val monthShort: String,
    /** Up to three methods for the quick-add buttons, most used first. */
    val quickMethods: List<PaymentMethodEntity>,
) {
    val monthSaved: Long get() = monthIncome - monthSpent

    companion object {
        private const val QUICK_DAYS = 90L

        suspend fun load(context: Context): WidgetData {
            val container = (context.applicationContext as ExpensesApplication).container
            val expenseDao = container.database.expenseDao()
            val methodDao = container.database.paymentMethodDao()
            val zone = ZoneId.systemDefault()
            val date = LocalDate.now(zone)
            val day = Period(PeriodType.DAY, date)
            val month = Period(PeriodType.MONTH, date)

            // Most used in the last 90 days, topped up with your first active methods if you've used fewer than 3.
            val since = date.minusDays(QUICK_DAYS).atStartOfDay(zone).toInstant().toEpochMilli()
            val used = methodDao.mostUsed(since, 3)
            val quick = (used + methodDao.getActive().filter { m -> used.none { it.id == m.id } }).take(3)

            return WidgetData(
                today = expenseDao.sum(Kind.EXPENSE, day.startMillis(zone), day.endMillis(zone)),
                todayCount = expenseDao.countIn(Kind.EXPENSE, day.startMillis(zone), day.endMillis(zone)),
                monthSpent = expenseDao.sum(Kind.EXPENSE, month.startMillis(zone), month.endMillis(zone)),
                monthIncome = expenseDao.sum(Kind.INCOME, month.startMillis(zone), month.endMillis(zone)),
                monthName = date.month.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                monthShort = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                quickMethods = quick,
            )
        }
    }
}
