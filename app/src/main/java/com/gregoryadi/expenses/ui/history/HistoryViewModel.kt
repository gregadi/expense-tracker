package com.gregoryadi.expenses.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.data.repository.LookupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryFilter(
    val query: String = "",
    /** null = everything, otherwise only [com.gregoryadi.expenses.domain.Kind.EXPENSE] or INCOME. */
    val kind: String? = null,
    val methodId: Long? = null,
    val categoryId: Long? = null,
)

class HistoryViewModel(
    private val repo: ExpenseRepository,
    lookups: LookupRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter())
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    /** The expense shown in the detail pane on wide screens. */
    private val selectedId = MutableStateFlow<Long?>(null)

    val items: StateFlow<List<ExpenseWithDetails>> =
        combine(repo.observeAll(), _filter) { all, filter ->
            val query = filter.query.trim().lowercase()
            all.filter { item ->
                (filter.kind == null || item.expense.kind == filter.kind) &&
                    (filter.methodId == null || item.expense.paymentMethodId == filter.methodId) &&
                    (filter.categoryId == null || item.expense.categoryId == filter.categoryId) &&
                    (query.isEmpty() ||
                        item.expense.note.lowercase().contains(query) ||
                        item.category.name.lowercase().contains(query) ||
                        item.method.name.lowercase().contains(query) ||
                        item.expense.amount.toString().contains(query))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selected: StateFlow<ExpenseWithDetails?> =
        combine(items, selectedId) { list, id -> list.firstOrNull { it.expense.id == id } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val methods: StateFlow<List<PaymentMethodEntity>> =
        lookups.observeActiveMethods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        lookups.observeActiveCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(query: String) = _filter.update { it.copy(query = query) }

    /** Switching between expenses and income clears a category filter of the other kind. */
    fun setKind(kind: String?) = _filter.update { it.copy(kind = kind, categoryId = null) }

    fun toggleMethod(id: Long) = _filter.update { it.copy(methodId = if (it.methodId == id) null else id) }

    fun toggleCategory(id: Long) = _filter.update { it.copy(categoryId = if (it.categoryId == id) null else id) }

    fun select(id: Long?) {
        selectedId.value = id
    }

    fun delete(item: ExpenseWithDetails) {
        viewModelScope.launch { repo.delete(item.expense) }
    }

    fun restore(item: ExpenseWithDetails) {
        viewModelScope.launch { repo.restore(item.expense) }
    }
}
