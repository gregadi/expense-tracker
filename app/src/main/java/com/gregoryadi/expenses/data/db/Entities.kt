package com.gregoryadi.expenses.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** How the money was paid: cash, a bank card, an e-wallet, QRIS... */
@Entity(tableName = "payment_method")
data class PaymentMethodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** One of [com.gregoryadi.expenses.domain.MethodType.key]. */
    val type: String,
    /** ARGB colour packed in a Long, e.g. 0xFF2B6E5F. Reused in every chart. */
    val color: Long,
    val sortOrder: Int = 0,
    /** Archived items are hidden from pickers but keep their old expenses. */
    val archived: Boolean = false,
)

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    /** "expense" or "income": which kind of entry this category is for. */
    @ColumnInfo(defaultValue = "expense") val kind: String = "expense",
)

/**
 * One money movement: an expense or, since v2, an income. Amounts are whole rupiah stored as Long,
 * so totals never pick up rounding errors.
 */
@Entity(
    tableName = "expense",
    foreignKeys = [
        ForeignKey(
            entity = PaymentMethodEntity::class,
            parentColumns = ["id"],
            childColumns = ["paymentMethodId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("occurredAt"), Index("paymentMethodId"), Index("categoryId")],
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Long,
    /** Epoch millis of when the money was spent. */
    val occurredAt: Long,
    val paymentMethodId: Long,
    val categoryId: Long,
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    /** "expense" (money out) or "income" (money in). The table keeps its old name. */
    @ColumnInfo(defaultValue = "expense") val kind: String = "expense",
)

/** An expense together with its payment method and category, loaded in one query. */
data class ExpenseWithDetails(
    @Embedded val expense: ExpenseEntity,
    @Relation(parentColumn = "paymentMethodId", entityColumn = "id")
    val method: PaymentMethodEntity,
    @Relation(parentColumn = "categoryId", entityColumn = "id")
    val category: CategoryEntity,
)

/** One row of a "spending per payment method / category" summary. */
data class GroupTotal(
    val id: Long,
    val name: String,
    val color: Long,
    val total: Long,
    val cnt: Int,
)

/** A single amount with its timestamp, used to build trend charts. */
data class AmountAt(
    @ColumnInfo(name = "ts") val ts: Long,
    @ColumnInfo(name = "amount") val amount: Long,
)
