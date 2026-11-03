package com.gregoryadi.expenses.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date and time text shared by the screens. */
object Formats {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun time(millis: Long): String =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(Instant.ofEpochMilli(millis).atZone(zone))

    fun dateTime(millis: Long): String =
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy · HH:mm", Locale.getDefault())
            .format(Instant.ofEpochMilli(millis).atZone(zone))

    fun localDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** "Today", "Yesterday", or "Mon, 26 Oct". */
    fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> DateTimeFormatter.ofPattern(
            if (date.year == today.year) "EEE, d MMM" else "EEE, d MMM yyyy",
            Locale.getDefault(),
        ).format(date)
    }

    /** Label on the add-expense date chip. */
    fun chipLabel(millis: Long, today: LocalDate): String {
        val date = localDate(millis)
        val prefix = when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(date)
        }
        return "$prefix, ${time(millis)}"
    }
}
