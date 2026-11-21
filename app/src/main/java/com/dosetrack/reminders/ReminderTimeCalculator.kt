package com.dosetrack.reminders

import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Pure, unit-testable computation of the next reminder fire time. */
object ReminderTimeCalculator {

    /**
     * Returns null when no reminder should be scheduled, including for configs that are out of range.
     * This runs in broadcast receivers on every boot, so it must never throw or loop unboundedly.
     */
    fun computeNextFireTime(
        config: ReminderConfig,
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Instant? = try {
        when (config.mode) {
            ReminderMode.NONE -> null
            ReminderMode.DAYS_OF_WEEK -> nextDaysOfWeekFireTime(config, now, zone)
            ReminderMode.INTERVAL -> nextIntervalFireTime(config, now, zone)
        }
    } catch (e: DateTimeException) {
        null
    } catch (e: ArithmeticException) {
        null
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

    /**
     * Jumps straight to the right occurrence instead of stepping one interval at a time, so the cost
     * doesn't grow with how long ago the start was. Each occurrence is computed from the start
     * (start + k * interval), which avoids month-end drift (Jan 31 -> Feb 28 -> Mar 28 ...).
     * HOURS are exact elapsed time; DAYS/WEEKS/MONTHS are calendar-based, so they keep the
     * wall-clock time across daylight-saving changes.
     */
    private fun nextIntervalFireTime(config: ReminderConfig, now: Instant, zone: ZoneId): Instant? {
        if (config.intervalValue !in ReminderConfig.INTERVAL_RANGE) return null
        val unit = when (config.intervalUnit) {
            IntervalUnit.HOURS -> ChronoUnit.HOURS
            IntervalUnit.DAYS -> ChronoUnit.DAYS
            IntervalUnit.WEEKS -> ChronoUnit.WEEKS
            IntervalUnit.MONTHS -> ChronoUnit.MONTHS
        }
        val step = config.intervalValue.toLong()
        val start: ZonedDateTime = config.startAt.atZone(zone)
        val nowZoned = now.atZone(zone)
        if (start.isAfter(nowZoned)) return start.toInstant()

        var k = unit.between(start, nowZoned) / step
        var next = start.plus(k * step, unit)
        // At most a couple of iterations: `between` truncates, and calendar units can land short.
        while (!next.isAfter(nowZoned)) {
            k++
            next = start.plus(k * step, unit)
        }
        return next.toInstant()
    }
}
