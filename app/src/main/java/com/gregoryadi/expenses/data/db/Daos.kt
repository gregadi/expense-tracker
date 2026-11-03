package com.gregoryadi.expenses.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentMethodDao {
    @Query("SELECT * FROM payment_method WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_method ORDER BY archived, sortOrder, id")
    fun observeAll(): Flow<List<PaymentMethodEntity>>

    @Query("SELECT * FROM payment_method ORDER BY sortOrder, id")
    suspend fun getAll(): List<PaymentMethodEntity>

    @Query("SELECT COUNT(*) FROM payment_method")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM payment_method")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(item: PaymentMethodEntity): Long

    @Insert
    suspend fun insertAll(items: List<PaymentMethodEntity>)

    @Update
    suspend fun update(item: PaymentMethodEntity)

    @Query("UPDATE payment_method SET color = :to WHERE color = :from")
    suspend fun replaceColor(from: Long, to: Long)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category WHERE archived = 0 ORDER BY sortOrder, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY archived, sortOrder, id")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM category ORDER BY sortOrder, id")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT COUNT(*) FROM category WHERE kind = :kind")
    suspend fun countByKind(kind: String): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM category")
    suspend fun nextSortOrder(): Int

    @Insert
    suspend fun insert(item: CategoryEntity): Long

    @Insert
    suspend fun insertAll(items: List<CategoryEntity>)

    @Update
    suspend fun update(item: CategoryEntity)

    @Query("UPDATE category SET color = :to WHERE color = :from")
    suspend fun replaceColor(from: Long, to: Long)
}

/** Every summary query takes a [kind]: "expense" for money out, "income" for money in. */
@Dao
interface ExpenseDao {
    /** REPLACE lets "undo delete" put an entry back under its old id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("SELECT * FROM expense WHERE id = :id")
    suspend fun get(id: Long): ExpenseEntity?

    @Query("SELECT * FROM expense ORDER BY occurredAt")
    suspend fun getAll(): List<ExpenseEntity>

    @Transaction
    @Query("SELECT * FROM expense ORDER BY occurredAt DESC, id DESC")
    fun observeAll(): Flow<List<ExpenseWithDetails>>

    @Transaction
    @Query("SELECT * FROM expense ORDER BY occurredAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ExpenseWithDetails>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM expense
        WHERE kind = :kind AND occurredAt >= :start AND occurredAt < :end
        """
    )
    fun observeTotal(kind: String, start: Long, end: Long): Flow<Long>

    @Query(
        """
        SELECT occurredAt AS ts, amount AS amount FROM expense
        WHERE kind = :kind AND occurredAt >= :start AND occurredAt < :end
        """
    )
    fun observeAmounts(kind: String, start: Long, end: Long): Flow<List<AmountAt>>

    @Query(
        """
        SELECT pm.id AS id, pm.name AS name, pm.color AS color,
               COALESCE(SUM(e.amount), 0) AS total, COUNT(e.id) AS cnt
        FROM expense e
        INNER JOIN payment_method pm ON pm.id = e.paymentMethodId
        WHERE e.kind = :kind AND e.occurredAt >= :start AND e.occurredAt < :end
        GROUP BY pm.id
        ORDER BY total DESC
        """
    )
    fun observeTotalsByMethod(kind: String, start: Long, end: Long): Flow<List<GroupTotal>>

    @Query(
        """
        SELECT c.id AS id, c.name AS name, c.color AS color,
               COALESCE(SUM(e.amount), 0) AS total, COUNT(e.id) AS cnt
        FROM expense e
        INNER JOIN category c ON c.id = e.categoryId
        WHERE e.kind = :kind AND e.occurredAt >= :start AND e.occurredAt < :end
        GROUP BY c.id
        ORDER BY total DESC
        """
    )
    fun observeTotalsByCategory(kind: String, start: Long, end: Long): Flow<List<GroupTotal>>

    /** Used by import to skip entries that are already in the database. */
    @Query(
        """
        SELECT COUNT(*) FROM expense
        WHERE kind = :kind AND occurredAt = :occurredAt AND amount = :amount
          AND paymentMethodId = :methodId AND categoryId = :categoryId AND note = :note
        """
    )
    suspend fun countMatching(
        kind: String,
        occurredAt: Long,
        amount: Long,
        methodId: Long,
        categoryId: Long,
        note: String,
    ): Int
}
