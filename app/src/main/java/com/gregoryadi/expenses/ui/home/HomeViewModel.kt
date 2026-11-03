package com.gregoryadi.expenses.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.data.db.GroupTotal
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Period
import com.gregoryadi.expenses.domain.PeriodType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

data class HomeState(
    /** Spent today. */
    val today: Long = 0,
    val monthSpent: Long = 0,
    val monthIncome: Long = 0,
    /** Latest entries of both kinds. */
    val recent: List<ExpenseWithDetails> = emptyList(),
    /** Spending per payment method this month. */
    val methodsThisMonth: List<GroupTotal> = emptyList(),
) {
    val monthSaved: Long get() = monthIncome - monthSpent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(repo: ExpenseRepository) : ViewModel() {
    private val zone = ZoneId.systemDefault()

    /** Emits today's date, and again after midnight, so "Today" never goes stale. */
    private val currentDate = flow {
        while (true) {
            emit(LocalDate.now(zone))
            delay(60_000)
        }
    }.distinctUntilChanged()

    val state: StateFlow<HomeState> = currentDate.flatMapLatest { date ->
        val day = Period(PeriodType.DAY, date)
        val month = Period(PeriodType.MONTH, date)
        val monthStart = month.startMillis(zone)
        val monthEnd = month.endMillis(zone)
        combine(
            repo.observeTotal(Kind.EXPENSE, day.startMillis(zone), day.endMillis(zone)),
            repo.observeTotal(Kind.EXPENSE, monthStart, monthEnd),
            repo.observeTotal(Kind.INCOME, monthStart, monthEnd),
            repo.observeRecent(5),
            repo.observeTotalsByMethod(Kind.EXPENSE, monthStart, monthEnd),
        ) { today, spent, income, recent, methods ->
            HomeState(today, spent, income, recent, methods)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
