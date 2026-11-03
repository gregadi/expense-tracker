package com.gregoryadi.expenses.domain

import com.gregoryadi.expenses.data.db.AmountAt
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class PeriodType(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
}

/**
 * A calendar day, week, month or year containing [anchor].
 * Weeks start on Monday. All boundaries are computed in Kotlin with java.time, in the phone's time zone;
 * SQL only sums rows between two epoch-millis values.
 */
data class Period(val type: PeriodType, val anchor: LocalDate) {

    val start: LocalDate
        get() = when (type) {
            PeriodType.DAY -> anchor
            PeriodType.WEEK -> anchor.with(TemporalAdjusters.previousOrSame(WEEK_START))
            PeriodType.MONTH -> anchor.withDayOfMonth(1)
            PeriodType.YEAR -> anchor.withDayOfYear(1)
        }

    /** First day after the period. */
    val endExclusive: LocalDate
        get() = when (type) {
            PeriodType.DAY -> start.plusDays(1)
            PeriodType.WEEK -> start.plusWeeks(1)
            PeriodType.MONTH -> start.plusMonths(1)
            PeriodType.YEAR -> start.plusYears(1)
        }

    val dayCount: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()

    fun shift(steps: Long): Period {
        val s = start // shift from the start so Jan 31 + 1 month is February, not "Feb 28"
        val moved = when (type) {
            PeriodType.DAY -> s.plusDays(steps)
            PeriodType.WEEK -> s.plusWeeks(steps)
            PeriodType.MONTH -> s.plusMonths(steps)
            PeriodType.YEAR -> s.plusYears(steps)
        }
        return copy(anchor = moved)
    }

    fun previous(): Period = shift(-1)
    fun next(): Period = shift(1)

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && date.isBefore(endExclusive)

    /** True when the whole period lies before [today], so its totals are final. */
    fun isComplete(today: LocalDate): Boolean = !endExclusive.isAfter(today)

    fun startMillis(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMillis(zone: ZoneId): Long = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

    fun label(locale: Locale = Locale.getDefault()): String = when (type) {
        PeriodType.DAY -> "${shortDow(start, locale)}, ${start.dayOfMonth} ${shortMonth(start.month, locale)} ${start.year}"
        PeriodType.WEEK -> {
            val last = endExclusive.minusDays(1)
            "${start.dayOfMonth} ${shortMonth(start.month, locale)} – ${last.dayOfMonth} ${shortMonth(last.month, locale)} ${last.year}"
        }
        PeriodType.MONTH -> "${start.month.getDisplayName(TextStyle.FULL, locale)} ${start.year}"
        PeriodType.YEAR -> start.year.toString()
    }

    /** "last week", "previous day"... used in "Previous week: Rp ..." captions. */
    val unitName: String get() = type.label.lowercase()

    companion object {
        val WEEK_START: DayOfWeek = DayOfWeek.MONDAY

        private fun shortMonth(month: Month, locale: Locale) = month.getDisplayName(TextStyle.SHORT, locale)
        private fun shortDow(date: LocalDate, locale: Locale) = date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
    }
}

/** One bar of a trend chart. [showLabel] marks the bars that get an axis label. */
data class Bucket(val label: String, val total: Long, val showLabel: Boolean)

/**
 * Splits a period's expenses into bars: hours for a day, days for a week or month, months for a year.
 */
fun bucketize(
    period: Period,
    items: List<AmountAt>,
    zone: ZoneId,
    locale: Locale = Locale.getDefault(),
): List<Bucket> {
    val count = when (period.type) {
        PeriodType.DAY -> 24
        PeriodType.WEEK -> 7
        PeriodType.MONTH -> period.dayCount
        PeriodType.YEAR -> 12
    }
    val totals = LongArray(count)
    items.forEach { item ->
        val time = Instant.ofEpochMilli(item.ts).atZone(zone)
        val index = when (period.type) {
            PeriodType.DAY -> time.hour
            PeriodType.WEEK -> ChronoUnit.DAYS.between(period.start, time.toLocalDate()).toInt()
            PeriodType.MONTH -> time.dayOfMonth - 1
            PeriodType.YEAR -> time.monthValue - 1
        }
        if (index in totals.indices) totals[index] += item.amount
    }
    return List(count) { i ->
        when (period.type) {
            PeriodType.DAY -> Bucket("%02d".format(i), totals[i], showLabel = i % 6 == 0)
            PeriodType.WEEK -> Bucket(
                period.start.plusDays(i.toLong()).dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                totals[i],
                showLabel = true,
            )
            PeriodType.MONTH -> Bucket((i + 1).toString(), totals[i], showLabel = i == 0 || (i + 1) % 5 == 0)
            PeriodType.YEAR -> Bucket(
                Month.of(i + 1).getDisplayName(TextStyle.SHORT, locale),
                totals[i],
                showLabel = true,
            )
        }
    }
}
