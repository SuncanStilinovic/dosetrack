package com.dosetrack.util

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val time24Formatter = DateTimeFormatter.ofPattern("HH:mm")
private val time12Formatter = DateTimeFormatter.ofPattern("h:mm a")

private fun timeFormatter(use24Hour: Boolean) = if (use24Hour) time24Formatter else time12Formatter

/** Localized date followed by the time in the user's chosen 12- or 24-hour clock. */
fun Instant.toDisplayString(use24Hour: Boolean): String {
    val local = atZone(ZoneId.systemDefault())
    return "${dateFormatter.format(local)}, ${timeFormatter(use24Hour).format(local)}"
}

/** Formats minutes after midnight, e.g. 810 → "13:30" or "1:30 PM". */
fun formatTimeOfDay(minutesOfDay: Int, use24Hour: Boolean): String =
    timeFormatter(use24Hour).format(LocalTime.of(minutesOfDay / 60, minutesOfDay % 60))
