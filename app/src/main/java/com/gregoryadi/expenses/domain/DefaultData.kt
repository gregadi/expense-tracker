package com.gregoryadi.expenses.domain

import com.gregoryadi.expenses.data.db.CategoryEntity
import com.gregoryadi.expenses.data.db.PaymentMethodEntity

/** Whether an entry is money going out or money coming in. Stored as text in the database. */
object Kind {
    const val EXPENSE = "expense"
    const val INCOME = "income"
}

enum class MethodType(val key: String, val label: String) {
    CASH("cash", "Cash"),
    DEBIT("debit", "Debit card"),
    CREDIT("credit", "Credit card"),
    EWALLET("ewallet", "E-wallet"),
    QRIS("qris", "QRIS"),
    TRANSFER("transfer", "Bank transfer"),
    OTHER("other", "Other");

    companion object {
        fun fromKey(key: String): MethodType = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

/** Bright colours that read on the dark Ink ground, for payment methods and categories. */
object Palette {
    val colors: List<Long> = listOf(
        0xFFD4F45A, 0xFF7FB8FF, 0xFFFFB86B, 0xFFC4A7FF, 0xFF6FD6C1,
        0xFFFF8FA3, 0xFFA3B1BC, 0xFFB7D96B, 0xFF8FA0FF, 0xFFF2D16B,
    )

    /** The muted colours of the first light theme; updated to [colors] index by index on launch. */
    val legacy: List<Long> = listOf(
        0xFF2B6E5F, 0xFF4F7CAC, 0xFFC97B63, 0xFFD4A24C, 0xFF7A6BA8,
        0xFF5E9E8F, 0xFFB5586F, 0xFF6B7F8C, 0xFF8AA05A, 0xFF3F5F8A,
    )
}

/** What a fresh install starts with. Everything can be renamed or archived in Manage. */
object DefaultData {
    fun methods(): List<PaymentMethodEntity> = listOf(
        PaymentMethodEntity(name = "Cash", type = MethodType.CASH.key, color = Palette.colors[0], sortOrder = 0),
        PaymentMethodEntity(name = "BCA Debit", type = MethodType.DEBIT.key, color = Palette.colors[1], sortOrder = 1),
        PaymentMethodEntity(name = "QRIS", type = MethodType.QRIS.key, color = Palette.colors[2], sortOrder = 2),
        PaymentMethodEntity(name = "GoPay", type = MethodType.EWALLET.key, color = Palette.colors[3], sortOrder = 3),
        PaymentMethodEntity(name = "OVO", type = MethodType.EWALLET.key, color = Palette.colors[4], sortOrder = 4),
        PaymentMethodEntity(name = "DANA", type = MethodType.EWALLET.key, color = Palette.colors[5], sortOrder = 5),
        PaymentMethodEntity(name = "ShopeePay", type = MethodType.EWALLET.key, color = Palette.colors[6], sortOrder = 6),
        PaymentMethodEntity(name = "Credit card", type = MethodType.CREDIT.key, color = Palette.colors[7], sortOrder = 7),
    )

    fun categories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Food & drink", color = Palette.colors[2], sortOrder = 0),
        CategoryEntity(name = "Transport", color = Palette.colors[1], sortOrder = 1),
        CategoryEntity(name = "Groceries", color = Palette.colors[8], sortOrder = 2),
        CategoryEntity(name = "Bills", color = Palette.colors[3], sortOrder = 3),
        CategoryEntity(name = "Shopping", color = Palette.colors[4], sortOrder = 4),
        CategoryEntity(name = "Health", color = Palette.colors[0], sortOrder = 5),
        CategoryEntity(name = "Entertainment", color = Palette.colors[6], sortOrder = 6),
        CategoryEntity(name = "Other", color = Palette.colors[7], sortOrder = 7),
    )

    fun incomeCategories(): List<CategoryEntity> = listOf(
        CategoryEntity(name = "Salary", color = Palette.colors[0], sortOrder = 0, kind = Kind.INCOME),
        CategoryEntity(name = "Freelance", color = Palette.colors[1], sortOrder = 1, kind = Kind.INCOME),
        CategoryEntity(name = "Business", color = Palette.colors[3], sortOrder = 2, kind = Kind.INCOME),
        CategoryEntity(name = "Gift", color = Palette.colors[6], sortOrder = 3, kind = Kind.INCOME),
        CategoryEntity(name = "Interest", color = Palette.colors[8], sortOrder = 4, kind = Kind.INCOME),
        CategoryEntity(name = "Other income", color = Palette.colors[7], sortOrder = 5, kind = Kind.INCOME),
    )
}
