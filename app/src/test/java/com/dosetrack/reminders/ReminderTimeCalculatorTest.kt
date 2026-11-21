package com.dosetrack.reminders

import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeCalculatorTest {

    private val zone = ZoneId.of("Europe/Zagreb")

    private fun at(local: String): Instant = ZonedDateTime.of(java.time.LocalDateTime.parse(local), zone).toInstant()

    private fun next(config: ReminderConfig, now: String) =
        ReminderTimeCalculator.computeNextFireTime(config, at(now), zone)

    private fun interval(value: Int, unit: IntervalUnit, start: String) = ReminderConfig(
        mode = ReminderMode.INTERVAL,
        intervalValue = value,
        intervalUnit = unit,
        startAt = at(start),
    )

    private fun weekly(vararg days: DayOfWeek, minutes: Int = 8 * 60) = ReminderConfig(
        mode = ReminderMode.DAYS_OF_WEEK,
        days = days.toSet(),
        timeOfDayMinutes = minutes,
    )

    // region Mode NONE / days of week

    @Test
    fun `mode NONE never schedules`() {
        assertNull(next(ReminderConfig(mode = ReminderMode.NONE), "2026-11-21T12:00"))
    }

    @Test
    fun `days of week picks later today when time has not passed`() {
        // 2026-11-21 is a Saturday.
        assertEquals(at("2026-11-21T20:00"), next(weekly(DayOfWeek.SATURDAY, minutes = 20 * 60), "2026-11-21T12:00"))
    }

    @Test
    fun `days of week skips today when time has passed`() {
        assertEquals(at("2026-11-28T08:00"), next(weekly(DayOfWeek.SATURDAY), "2026-11-21T12:00"))
    }

    @Test
    fun `days of week picks nearest of several days`() {
        assertEquals(at("2026-11-23T08:00"), next(weekly(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), "2026-11-21T12:00"))
    }

    @Test
    fun `days of week with no days selected does not schedule`() {
        assertNull(next(weekly(), "2026-11-21T12:00"))
    }

    @Test
    fun `days of week keeps wall-clock time across the DST change`() {
        // DST ends on Sunday 2026-10-25 in Europe; Monday 08:00 must still be 08:00 local.
        assertEquals(at("2026-10-26T08:00"), next(weekly(DayOfWeek.MONDAY), "2026-10-24T12:00"))
    }

    // endregion

    // region Interval

    @Test
    fun `interval start in the future fires at start`() {
        assertEquals(at("2026-12-01T09:00"), next(interval(1, IntervalUnit.DAYS, "2026-12-01T09:00"), "2026-11-21T12:00"))
    }

    @Test
    fun `interval exactly at an occurrence moves to the next one`() {
        assertEquals(at("2026-11-21T13:00"), next(interval(1, IntervalUnit.HOURS, "2026-11-21T10:00"), "2026-11-21T12:00"))
        assertEquals(at("2026-11-22T12:00"), next(interval(1, IntervalUnit.DAYS, "2026-11-20T12:00"), "2026-11-21T12:00"))
    }

    @Test
    fun `hours are exact elapsed time`() {
        assertEquals(at("2026-11-21T14:00"), next(interval(6, IntervalUnit.HOURS, "2026-11-20T08:00"), "2026-11-21T12:30"))
    }

    @Test
    fun `days keep wall-clock time across the DST change`() {
        val result = next(interval(2, IntervalUnit.DAYS, "2026-10-20T08:00"), "2026-10-28T09:00")
        assertEquals(at("2026-10-30T08:00"), result)
    }

    @Test
    fun `weeks step whole weeks`() {
        assertEquals(at("2026-11-24T08:00"), next(interval(2, IntervalUnit.WEEKS, "2026-11-10T08:00"), "2026-11-21T12:00"))
    }

    @Test
    fun `months from the 31st do not drift to the 28th`() {
        assertEquals(at("2026-03-31T08:00"), next(interval(1, IntervalUnit.MONTHS, "2026-01-31T08:00"), "2026-03-01T00:00"))
    }

    @Test
    fun `months land on the last day of shorter months`() {
        assertEquals(at("2026-02-28T08:00"), next(interval(1, IntervalUnit.MONTHS, "2026-01-31T08:00"), "2026-02-01T00:00"))
    }

    @Test(timeout = 2_000)
    fun `a start years in the past is computed without stepping through every hour`() {
        val config = ReminderConfig(
            mode = ReminderMode.INTERVAL,
            intervalValue = 1,
            intervalUnit = IntervalUnit.HOURS,
            startAt = Instant.EPOCH,
        )
        assertEquals(at("2026-11-21T13:00"), next(config, "2026-11-21T12:30"))
    }

    // endregion

    // region Out-of-range configs must not hang or throw (they run in receivers on every boot)

    @Test(timeout = 2_000)
    fun `zero interval does not schedule`() {
        assertNull(next(interval(0, IntervalUnit.HOURS, "2026-11-21T10:00"), "2026-11-21T12:00"))
    }

    @Test(timeout = 2_000)
    fun `negative interval does not schedule`() {
        assertNull(next(interval(-5, IntervalUnit.DAYS, "2026-11-21T10:00"), "2026-11-21T12:00"))
    }

    @Test(timeout = 2_000)
    fun `interval above the allowed range does not schedule`() {
        assertNull(next(interval(366, IntervalUnit.DAYS, "2026-11-21T10:00"), "2026-11-21T12:00"))
        assertNull(next(interval(Int.MAX_VALUE, IntervalUnit.MONTHS, "2026-11-21T10:00"), "2026-11-21T12:00"))
    }

    @Test(timeout = 2_000)
    fun `extreme start instant does not throw`() {
        val config = ReminderConfig(
            mode = ReminderMode.INTERVAL,
            intervalValue = 1,
            intervalUnit = IntervalUnit.HOURS,
            startAt = Instant.MIN,
        )
        assertNull(next(config, "2026-11-21T12:00"))
    }

    // endregion
}
