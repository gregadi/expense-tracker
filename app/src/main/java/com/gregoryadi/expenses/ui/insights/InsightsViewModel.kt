package com.gregoryadi.expenses.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.db.GroupTotal
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.domain.Bucket
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Period
import com.gregoryadi.expenses.domain.PeriodType
import com.gregoryadi.expenses.domain.bucketize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneId

data class InsightsState(
    val period: Period,
    /** Spent in the period. */
    val total: Long = 0,
    val previousTotal: Long = 0,
    /** Spending per hour / day / month, for the trend chart. */
    val buckets: List<Bucket> = emptyList(),
    val byMethod: List<GroupTotal> = emptyList(),
    val byCategory: List<GroupTotal> = emptyList(),
    /** Received in the period. */
    val income: Long = 0,
    val incomeBySource: List<GroupTotal> = emptyList(),
    /** True when the period is entirely in the past, so a "vs previous" percentage is fair. */
    val isComplete: Boolean = false,
    /** False when the next period would start in the future. */
    val canGoNext: Boolean = false,
) {
    val saved: Long get() = income - total
}

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(repo: ExpenseRepository) : ViewModel() {
    private val zone = ZoneId.systemDefault()
    private val period = MutableStateFlow(Period(PeriodType.MONTH, LocalDate.now(zone)))

    val state: StateFlow<InsightsState> = period.flatMapLatest { p ->
        val previous = p.previous()
        val start = p.startMillis(zone)
        val end = p.endMillis(zone)
        val spending = combine(
            repo.observeTotal(Kind.EXPENSE, start, end),
            repo.observeTotal(Kind.EXPENSE, previous.startMillis(zone), previous.endMillis(zone)),
            repo.observeAmounts(Kind.EXPENSE, start, end),
            repo.observeTotalsByMethod(Kind.EXPENSE, start, end),
            repo.observeTotalsByCategory(Kind.EXPENSE, start, end),
        ) { total, previousTotal, amounts, byMethod, byCategory ->
            val today = LocalDate.now(zone)
            InsightsState(
                period = p,
                total = total,
                previousTotal = previousTotal,
                buckets = bucketize(p, amounts, zone),
                byMethod = byMethod,
                byCategory = byCategory,
                isComplete = p.isComplete(today),
                canGoNext = p.isComplete(today),
            )
        }
        combine(
            spending,
            repo.observeTotal(Kind.INCOME, start, end),
            repo.observeTotalsByCategory(Kind.INCOME, start, end),
        ) { s, income, bySource -> s.copy(income = income, incomeBySource = bySource) }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        InsightsState(period = period.value),
    )

    fun setType(type: PeriodType) = period.update { it.copy(type = type) }

    fun previous() = period.update { it.previous() }

    fun next() = period.update { it.next() }

    fun goToToday() = period.update { it.copy(anchor = LocalDate.now(zone)) }
}
