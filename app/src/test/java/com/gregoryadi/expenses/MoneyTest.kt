package com.gregoryadi.expenses

import com.gregoryadi.expenses.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @Test
    fun formatsWithDotSeparators() {
        assertEquals("Rp 25.000", Money.format(25_000))
        assertEquals("Rp 1.250.000", Money.format(1_250_000))
        assertEquals("Rp 0", Money.format(0))
    }

    @Test
    fun compactUsesIndonesianUnits() {
        assertEquals("Rp 950", Money.compact(950))
        assertEquals("Rp 25 rb", Money.compact(25_000))
        assertEquals("Rp 1,2 jt", Money.compact(1_200_000))
        assertEquals("Rp 3 M", Money.compact(3_000_000_000))
    }
}
