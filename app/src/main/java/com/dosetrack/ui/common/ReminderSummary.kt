package com.dosetrack.ui.common

import com.dosetrack.data.model.IntervalUnit
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderEndMode
import com.dosetrack.data.model.ReminderMode
import com.dosetrack.util.formatTimeOfDay
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val endDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/**
 * One line describing a reminder, e.g. "Mon, Thu at 08:30 · 3 of 14 doses" or "Every 2 weeks · until
 * Dec 31, 2026". Returns null when the reminder is off.
 */
fun reminderSummary(
    config: ReminderConfig,
    use24Hour: Boolean,
    dosesLogged: Int,
    ended: Boolean,
    locale: Locale = Locale.getDefault(),
): String? {
    if (ended) return "Reminder ended"
    val schedule = when (config.mode) {
        ReminderMode.NONE -> return null
        ReminderMode.DAYS_OF_WEEK -> {
            val time = formatTimeOfDay(config.timeOfDayMinutes, use24Hour)
            val days = if (config.days.size == DayOfWeek.entries.size) {
                "Every day"
            } else {
                config.days.sorted().joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
            }
            "$days at $time"
        }
        ReminderMode.INTERVAL -> {
            val n = config.intervalValue
            val unit = when (config.intervalUnit) {
                IntervalUnit.HOURS -> if (n == 1) "hour" else "hours"
                IntervalUnit.DAYS -> if (n == 1) "day" else "days"
                IntervalUnit.WEEKS -> if (n == 1) "week" else "weeks"
                IntervalUnit.MONTHS -> if (n == 1) "month" else "months"
            }
            if (n == 1) "Every $unit" else "Every $n $unit"
        }
    }
    val end = when (config.endMode) {
        ReminderEndMode.NEVER -> null
        ReminderEndMode.ON_DATE -> config.endDate?.let { "until ${it.format(endDateFormatter)}" }
        ReminderEndMode.AFTER_DOSES -> config.endAfterDoses?.let { "$dosesLogged of $it doses" }
    }
    return listOfNotNull(schedule, end).joinToString(" · ")
}
