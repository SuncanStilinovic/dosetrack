package com.dosetrack.reminders

import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import java.time.Duration
import java.time.Instant
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

/** Pure, unit-testable computation of the next reminder fire time. */
object ReminderTimeCalculator {

    fun computeNextFireTime(
        config: ReminderConfig,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant? = when (config.mode) {
        ReminderMode.NONE -> null
        ReminderMode.DAYS_OF_WEEK -> nextDaysOfWeekFireTime(config, now, zone)
        ReminderMode.INTERVAL -> nextIntervalFireTime(config, now, zone)
    }

    private fun nextDaysOfWeekFireTime(config: ReminderConfig, now: Instant, zone: ZoneId): Instant? {
        if (config.days.isEmpty()) return null
        val nowZoned = now.atZone(zone)
        val hour = config.timeOfDayMinutes / 60
        val minute = config.timeOfDayMinutes % 60

        for (offset in 0..7) {
            val candidateDate = nowZoned.toLocalDate().plusDays(offset.toLong())
            val candidate = candidateDate.atTime(hour, minute).atZone(zone)
            if (candidate.dayOfWeek in config.days && candidate.isAfter(nowZoned)) {
                return candidate.toInstant()
            }
        }
        return null
    }

    private fun nextIntervalFireTime(config: ReminderConfig, now: Instant, zone: ZoneId): Instant {
        var next: ZonedDateTime = config.startAt.atZone(zone)
        val nowZoned = now.atZone(zone)
        while (!next.isAfter(nowZoned)) {
            next = when (config.intervalUnit) {
                IntervalUnit.HOURS -> next.plus(Duration.ofHours(config.intervalValue.toLong()))
                IntervalUnit.DAYS -> next.plus(Period.ofDays(config.intervalValue))
                IntervalUnit.WEEKS -> next.plus(Period.ofWeeks(config.intervalValue))
                IntervalUnit.MONTHS -> next.plus(Period.ofMonths(config.intervalValue))
            }
        }
        return next.toInstant()
    }
}
