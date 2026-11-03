package com.gregoryadi.expenses.domain

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/** Rupiah formatting. Amounts are whole rupiah in a Long. */
object Money {
    private val idLocale = Locale("id", "ID")

    /** "Rp 25.000" */
    fun format(amount: Long): String {
        val sign = if (amount < 0) "-" else ""
        return sign + "Rp " + NumberFormat.getIntegerInstance(idLocale).format(abs(amount))
    }

    /** "+Rp 25.000" for money coming in ("Rp 0" stays unsigned). */
    fun incoming(amount: Long): String = if (amount > 0) "+" + format(amount) else format(amount)

    /** Short form for chart labels and tight spaces: "Rp 25 rb", "Rp 1,2 jt", "Rp 3 M". */
    fun compact(amount: Long): String {
        val a = abs(amount).toDouble()
        val sign = if (amount < 0) "-" else ""
        return when {
            a >= 1e9 -> "${sign}Rp ${trimmed(a / 1e9)} M"
            a >= 1e6 -> "${sign}Rp ${trimmed(a / 1e6)} jt"
            a >= 1e3 -> "${sign}Rp ${trimmed(a / 1e3)} rb"
            else -> "${sign}Rp ${a.toLong()}"
        }
    }

    private fun trimmed(value: Double): String {
        val text = String.format(idLocale, "%.1f", value)
        return if (text.endsWith(",0")) text.dropLast(2) else text
    }
}
