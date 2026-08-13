package com.example.util

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Formats a double value as Indian Rupee currency (e.g. ₹5,000.00 or ₹5,000).
 */
fun formatInrCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 2
    format.minimumFractionDigits = 0
    return format.format(amount)
}

/**
 * Calculates due date for a given dayOfMonth in a specific YearMonth, handling short months safely.
 * e.g., Day 31 in April (30 days) resolves to April 30th.
 */
fun getDueDateForYearMonth(dayOfMonth: Int, yearMonth: YearMonth): LocalDate {
    val validDay = dayOfMonth.coerceAtMost(yearMonth.lengthOfMonth())
    return yearMonth.atDay(validDay)
}

/**
 * Gets the current due date for the active month based on today's date and the reminder's target dayOfMonth.
 */
fun getCurrentDueDate(dayOfMonth: Int, referenceDate: LocalDate = LocalDate.now()): LocalDate {
    val currentYearMonth = YearMonth.from(referenceDate)
    return getDueDateForYearMonth(dayOfMonth, currentYearMonth)
}

/**
 * Calculates the number of days from referenceDate until target LocalDate.
 * Negative value indicates overdue.
 */
fun daysUntilDue(dueDate: LocalDate, referenceDate: LocalDate = LocalDate.now()): Long {
    return ChronoUnit.DAYS.between(referenceDate, dueDate)
}
