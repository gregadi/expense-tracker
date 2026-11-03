package com.gregoryadi.expenses.data.repository

import com.gregoryadi.expenses.data.db.AmountAt
import com.gregoryadi.expenses.data.db.ExpenseDao
import com.gregoryadi.expenses.data.db.ExpenseEntity
import com.gregoryadi.expenses.data.db.ExpenseWithDetails
import com.gregoryadi.expenses.data.db.GroupTotal
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val dao: ExpenseDao) {
    fun observeAll(): Flow<List<ExpenseWithDetails>> = dao.observeAll()
    fun observeRecent(limit: Int): Flow<List<ExpenseWithDetails>> = dao.observeRecent(limit)
    fun observeTotal(kind: String, start: Long, end: Long): Flow<Long> = dao.observeTotal(kind, start, end)
    fun observeAmounts(kind: String, start: Long, end: Long): Flow<List<AmountAt>> = dao.observeAmounts(kind, start, end)
    fun observeTotalsByMethod(kind: String, start: Long, end: Long): Flow<List<GroupTotal>> =
        dao.observeTotalsByMethod(kind, start, end)
    fun observeTotalsByCategory(kind: String, start: Long, end: Long): Flow<List<GroupTotal>> =
        dao.observeTotalsByCategory(kind, start, end)

    suspend fun get(id: Long): ExpenseEntity? = dao.get(id)

    /** Inserts a new expense (id == 0) or updates an existing one. */
    suspend fun save(expense: ExpenseEntity): Long =
        if (expense.id == 0L) {
            dao.insert(expense)
        } else {
            dao.update(expense)
            expense.id
        }

    suspend fun delete(expense: ExpenseEntity) = dao.delete(expense)

    /** Puts a deleted expense back, keeping its old id. */
    suspend fun restore(expense: ExpenseEntity) {
        dao.insert(expense)
    }
}
