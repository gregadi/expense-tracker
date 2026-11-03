package com.gregoryadi.expenses.data.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tiny key-value settings. Included in Android Auto Backup together with the database. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _dynamicColor = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC, false))
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC, enabled).apply()
        _dynamicColor.value = enabled
    }

    /** The payment method used last, preselected when adding the next expense. */
    var lastMethodId: Long
        get() = prefs.getLong(KEY_LAST_METHOD, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_METHOD, value).apply()
        }

    var lastCategoryId: Long
        get() = prefs.getLong(KEY_LAST_CATEGORY, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CATEGORY, value).apply()
        }

    /** Where income usually lands (e.g. the bank account), remembered separately from spending. */
    var lastIncomeMethodId: Long
        get() = prefs.getLong(KEY_LAST_INCOME_METHOD, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_INCOME_METHOD, value).apply()
        }

    var lastIncomeCategoryId: Long
        get() = prefs.getLong(KEY_LAST_INCOME_CATEGORY, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_INCOME_CATEGORY, value).apply()
        }

    private companion object {
        const val KEY_DYNAMIC = "dynamic_color"
        const val KEY_LAST_METHOD = "last_method"
        const val KEY_LAST_CATEGORY = "last_category"
        const val KEY_LAST_INCOME_METHOD = "last_income_method"
        const val KEY_LAST_INCOME_CATEGORY = "last_income_category"
    }
}
