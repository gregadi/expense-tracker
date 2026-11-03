package com.gregoryadi.expenses.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.ExpenseEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.data.prefs.SettingsStore
import com.gregoryadi.expenses.data.repository.ExpenseRepository
import com.gregoryadi.expenses.data.repository.LookupRepository
import com.gregoryadi.expenses.domain.Kind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The draft in the add/edit sheet. It lives in a ViewModel, so folding, unfolding or rotating keeps it. */
data class AddState(
    val visible: Boolean = false,
    val editingId: Long? = null,
    /** [Kind.EXPENSE] for money out, [Kind.INCOME] for money in. */
    val kind: String = Kind.EXPENSE,
    /** Digits typed on the keypad; the amount in whole rupiah. */
    val digits: String = "",
    val methodId: Long? = null,
    val categoryId: Long? = null,
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = 0L,
) {
    val amount: Long get() = digits.toLongOrNull() ?: 0L
    val isIncome: Boolean get() = kind == Kind.INCOME
    val canSave: Boolean get() = amount > 0L && methodId != null && categoryId != null
}

class AddExpenseViewModel(
    private val repo: ExpenseRepository,
    lookups: LookupRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    private val zone = ZoneId.systemDefault()

    private val _state = MutableStateFlow(AddState())
    val state: StateFlow<AddState> = _state.asStateFlow()

    // Eagerly: the lists are tiny, and startNew() needs them even before the sheet is on screen.
    /** All methods, archived last. The sheet hides archived ones unless one is already selected. */
    val methods: StateFlow<List<PaymentMethodEntity>> =
        lookups.observeAllMethods().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** All categories of both kinds; the sheet shows the ones matching the current kind. */
    val categories: StateFlow<List<CategoryEntity>> =
        lookups.observeAllCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun startNew() {
        _state.value = AddState(
            visible = true,
            kind = Kind.EXPENSE,
            methodId = defaultMethod(Kind.EXPENSE),
            categoryId = defaultCategory(Kind.EXPENSE),
            occurredAt = System.currentTimeMillis(),
        )
    }

    fun startEdit(id: Long) {
        viewModelScope.launch {
            val e = repo.get(id) ?: return@launch
            _state.value = AddState(
                visible = true,
                editingId = e.id,
                kind = e.kind,
                digits = e.amount.toString(),
                methodId = e.paymentMethodId,
                categoryId = e.categoryId,
                note = e.note,
                occurredAt = e.occurredAt,
                createdAt = e.createdAt,
            )
        }
    }

    fun dismiss() = _state.update { it.copy(visible = false) }

    /** Switches between expense and income, picking a matching category (and, for new entries, method). */
    fun setKind(kind: String) = _state.update { s ->
        if (s.kind == kind) {
            s
        } else {
            s.copy(
                kind = kind,
                categoryId = defaultCategory(kind),
                methodId = if (s.editingId == null) defaultMethod(kind) ?: s.methodId else s.methodId,
            )
        }
    }

    fun key(keys: String) = _state.update { s ->
        val next = (s.digits + keys).trimStart('0')
        if (next.length > MAX_DIGITS) s else s.copy(digits = next)
    }

    fun backspace() = _state.update { it.copy(digits = it.digits.dropLast(1)) }

    fun setMethod(id: Long) = _state.update { it.copy(methodId = id) }

    fun setCategory(id: Long) = _state.update { it.copy(categoryId = id) }

    fun setNote(note: String) = _state.update { it.copy(note = note.take(120)) }

    /** Changes the date but keeps the time of day. */
    fun setDate(date: LocalDate) = _state.update { s ->
        val time = Instant.ofEpochMilli(s.occurredAt).atZone(zone).toLocalTime()
        s.copy(occurredAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli())
    }

    fun save(onSaved: () -> Unit) {
        val s = _state.value
        val methodId = s.methodId
        val categoryId = s.categoryId
        if (!s.canSave || methodId == null || categoryId == null) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            repo.save(
                ExpenseEntity(
                    id = s.editingId ?: 0L,
                    amount = s.amount,
                    occurredAt = s.occurredAt,
                    paymentMethodId = methodId,
                    categoryId = categoryId,
                    note = s.note.trim(),
                    createdAt = if (s.editingId != null) s.createdAt else now,
                    updatedAt = now,
                    kind = s.kind,
                )
            )
            if (s.isIncome) {
                settings.lastIncomeMethodId = methodId
                settings.lastIncomeCategoryId = categoryId
            } else {
                settings.lastMethodId = methodId
                settings.lastCategoryId = categoryId
            }
            _state.value = AddState()
            onSaved()
        }
    }

    /** Deletes the entry being edited and hands it back so the UI can offer "Undo". */
    fun deleteEditing(onDeleted: (ExpenseEntity) -> Unit) {
        val id = _state.value.editingId ?: return
        viewModelScope.launch {
            val e = repo.get(id) ?: return@launch
            repo.delete(e)
            _state.value = AddState()
            onDeleted(e)
        }
    }

    fun restore(expense: ExpenseEntity) {
        viewModelScope.launch { repo.restore(expense) }
    }

    private fun defaultMethod(kind: String): Long? {
        val active = methods.value.filter { !it.archived }
        val last = if (kind == Kind.INCOME) settings.lastIncomeMethodId else settings.lastMethodId
        return active.firstOrNull { it.id == last }?.id ?: active.firstOrNull()?.id
    }

    private fun defaultCategory(kind: String): Long? {
        val active = categories.value.filter { !it.archived && it.kind == kind }
        val last = if (kind == Kind.INCOME) settings.lastIncomeCategoryId else settings.lastCategoryId
        return active.firstOrNull { it.id == last }?.id ?: active.firstOrNull()?.id
    }

    private companion object {
        const val MAX_DIGITS = 12
    }
}
