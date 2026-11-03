package com.gregoryadi.expenses.domain

import kotlin.math.roundToInt

/** Money in minus money out, and a one-line reading of it. */
object Savings {
    fun saved(income: Long, spent: Long): Long = income - spent

    fun note(income: Long, spent: Long): String? {
        val saved = income - spent
        return when {
            income > 0 && saved >= 0 -> "You kept ${(saved * 100.0 / income).roundToInt()}% of what came in"
            income > 0 -> "You spent ${Money.format(-saved)} more than came in"
            spent > 0 -> "Add your income to see how much you're saving"
            else -> null
        }
    }

    /** Short version for tight spots: "You kept 65%" or "Over by Rp 200 rb". */
    fun shortNote(income: Long, spent: Long): String? {
        val saved = income - spent
        return when {
            income > 0 && saved >= 0 -> "You kept ${(saved * 100.0 / income).roundToInt()}%"
            income > 0 -> "Over by ${Money.compact(-saved)}"
            else -> null
        }
    }
}
