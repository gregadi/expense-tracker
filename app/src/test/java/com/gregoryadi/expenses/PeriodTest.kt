package com.gregoryadi.expenses

import com.gregoryadi.expenses.data.db.AmountAt
import com.gregoryadi.expenses.domain.Period
import com.gregoryadi.expenses.domain.PeriodType
import com.gregoryadi.expenses.domain.bucketize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class PeriodTest {

    @Test
    fun weekStartsOnMonday() {
        // 1 Nov 2026 is a Sunday, so its week began on Monday 26 Oct.
        val week = Period(PeriodType.WEEK, LocalDate.of(2026, 11, 1))
        assertEquals(LocalDate.of(2026, 10, 26), week.start)
        assertEquals(LocalDate.of(2026, 11, 2), week.endExclusive)
    }

    @Test
    fun monthHandlesLeapDay() {
        val month = Period(PeriodType.MONTH, LocalDate.of(2028, 2, 10))
        assertEquals(LocalDate.of(2028, 2, 1), month.start)
        assertEquals(LocalDate.of(2028, 3, 1), month.endExclusive)
        assertEquals(29, month.dayCount)
    }

    @Test
    fun shiftingFromTheThirtyFirstLandsInTheNextMonth() {
        val january = Period(PeriodType.MONTH, LocalDate.of(2026, 1, 31))
        assertEquals(LocalDate.of(2026, 2, 1), january.next().start)
        assertEquals(LocalDate.of(2025, 12, 1), january.previous().start)
    }

    @Test
    fun yearCoversTwelveMonths() {
        val year = Period(PeriodType.YEAR, LocalDate.of(2026, 6, 15))
        assertEquals(LocalDate.of(2026, 1, 1), year.start)
        assertEquals(LocalDate.of(2027, 1, 1), year.endExclusive)
    }

    @Test
    fun completeOnlyOnceItIsOver() {
        val month = Period(PeriodType.MONTH, LocalDate.of(2026, 10, 15))
        assertTrue(month.isComplete(LocalDate.of(2026, 11, 1)))
        assertFalse(month.isComplete(LocalDate.of(2026, 10, 31)))
    }

    @Test
    fun bucketsAddUpPerDayOfMonth() {
        val period = Period(PeriodType.MONTH, LocalDate.of(2026, 11, 15))
        fun at(day: Int, hour: Int) =
            LocalDate.of(2026, 11, day).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

        val buckets = bucketize(
            period,
            listOf(
                AmountAt(at(2, 9), 10_000),
                AmountAt(at(2, 20), 5_000),
                AmountAt(at(30, 23), 7_000),
            ),
            ZoneOffset.UTC,
            Locale.ENGLISH,
        )

        assertEquals(30, buckets.size)
        assertEquals(15_000L, buckets[1].total)
        assertEquals(7_000L, buckets[29].total)
        assertEquals(22_000L, buckets.sumOf { it.total })
    }

    @Test
    fun yearBucketsAreMonths() {
        val period = Period(PeriodType.YEAR, LocalDate.of(2026, 3, 1))
        val march = LocalDate.of(2026, 3, 5).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val buckets = bucketize(period, listOf(AmountAt(march, 1_000)), ZoneOffset.UTC, Locale.ENGLISH)
        assertEquals(12, buckets.size)
        assertEquals(1_000L, buckets[2].total)
        assertEquals("Mar", buckets[2].label)
    }
}
