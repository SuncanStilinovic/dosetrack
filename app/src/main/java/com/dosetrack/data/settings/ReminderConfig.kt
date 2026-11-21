package com.dosetrack.data.settings

import java.time.DayOfWeek
import java.time.Instant

data class ReminderConfig(
    val mode: ReminderMode = ReminderMode.NONE,
    val days: Set<DayOfWeek> = emptySet(),
    val timeOfDayMinutes: Int = 8 * 60,
    val intervalValue: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val startAt: Instant = Instant.now(),
    val nextFireAt: Instant? = null,
) {
    companion object {
        /** Accepted "every N <unit>" values; anything else would make no sense or overflow date maths. */
        val INTERVAL_RANGE = 1..365
    }
}
