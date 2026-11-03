package com.gregoryadi.expenses.data.repository

import com.gregoryadi.expenses.data.db.CategoryDao
import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.PaymentMethodDao
import com.gregoryadi.expenses.data.db.PaymentMethodEntity
import com.gregoryadi.expenses.domain.DefaultData
import com.gregoryadi.expenses.domain.Kind
import com.gregoryadi.expenses.domain.Palette
import kotlinx.coroutines.flow.Flow

/** Payment methods and categories: the "lookup" tables expenses point to. */
class LookupRepository(
    private val methodDao: PaymentMethodDao,
    private val categoryDao: CategoryDao,
) {
    fun observeActiveMethods(): Flow<List<PaymentMethodEntity>> = methodDao.observeActive()
    fun observeAllMethods(): Flow<List<PaymentMethodEntity>> = methodDao.observeAll()
    fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActive()
    fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun saveMethod(item: PaymentMethodEntity) {
        val clean = item.copy(name = item.name.trim())
        if (clean.id == 0L) {
            methodDao.insert(clean.copy(sortOrder = methodDao.nextSortOrder()))
        } else {
            methodDao.update(clean)
        }
    }

    suspend fun saveCategory(item: CategoryEntity) {
        val clean = item.copy(name = item.name.trim())
        if (clean.id == 0L) {
            categoryDao.insert(clean.copy(sortOrder = categoryDao.nextSortOrder()))
        } else {
            categoryDao.update(clean)
        }
    }

    /**
     * Fills in a sensible starting set of methods and categories. Runs on every launch but only adds
     * what is missing, so after the v2 upgrade it adds the income categories once.
     */
    suspend fun seedIfEmpty() {
        if (methodDao.count() == 0) methodDao.insertAll(DefaultData.methods())
        if (categoryDao.countByKind(Kind.EXPENSE) == 0) categoryDao.insertAll(DefaultData.categories())
        if (categoryDao.countByKind(Kind.INCOME) == 0) categoryDao.insertAll(DefaultData.incomeCategories())
        restyleLegacyColors()
    }

    /** Swaps the first theme's muted colours for the Ink palette; a no-op once done. */
    private suspend fun restyleLegacyColors() {
        Palette.legacy.forEachIndexed { i, old ->
            val new = Palette.colors[i]
            methodDao.replaceColor(old, new)
            categoryDao.replaceColor(old, new)
        }
    }
}
