package com.gregoryadi.expenses.ui.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.data.repository.LookupRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ManageViewModel(private val lookups: LookupRepository) : ViewModel() {

    val methods: StateFlow<List<PaymentMethodEntity>> =
        lookups.observeAllMethods().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> =
        lookups.observeAllCategories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveMethod(item: PaymentMethodEntity) {
        if (item.name.isBlank()) return
        viewModelScope.launch { lookups.saveMethod(item) }
    }

    fun saveCategory(item: CategoryEntity) {
        if (item.name.isBlank()) return
        viewModelScope.launch { lookups.saveCategory(item) }
    }
}
