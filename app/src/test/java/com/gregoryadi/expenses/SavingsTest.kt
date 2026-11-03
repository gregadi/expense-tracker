package com.gregoryadi.expenses

import com.gregoryadi.expenses.domain.Money
import com.gregoryadi.expenses.domain.Savings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavingsTest {
    @Test
    fun savedIsIncomeMinusSpending() {
        assertEquals(1_500_000L, Savings.saved(income = 8_000_000, spent = 6_500_000))
        assertEquals(-200_000L, Savings.saved(income = 1_000_000, spent = 1_200_000))
    }

    @Test
    fun noteDescribesTheMonth() {
        assertEquals("You kept 25% of what came in", Savings.note(income = 8_000_000, spent = 6_000_000))
        assertEquals("You spent Rp 200.000 more than came in", Savings.note(income = 1_000_000, spent = 1_200_000))
        assertEquals("Add your income to see how much you're saving", Savings.note(income = 0, spent = 50_000))
        assertNull(Savings.note(income = 0, spent = 0))
    }

    @Test
    fun incomingAmountsGetAPlus() {
        assertEquals("+Rp 5.000.000", Money.incoming(5_000_000))
        assertEquals("Rp 0", Money.incoming(0))
    }
}
