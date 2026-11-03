package com.gregoryadi.expenses.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ExpenseEntity::class, PaymentMethodEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun paymentMethodDao(): PaymentMethodDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        /**
         * v2 adds income: a "kind" column on expenses and categories.
         * Everything already recorded becomes an expense, so no data is lost.
         * Income categories are added by the repository's seedIfEmpty() on the next launch.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expense ADD COLUMN kind TEXT NOT NULL DEFAULT 'expense'")
                db.execSQL("ALTER TABLE category ADD COLUMN kind TEXT NOT NULL DEFAULT 'expense'")
            }
        }
    }
}
