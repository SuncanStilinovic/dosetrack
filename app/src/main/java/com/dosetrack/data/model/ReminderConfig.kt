package com.dosetrack.data.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** A medication's reminder schedule, and when it stops. */
data class ReminderConfig(
    val mode: ReminderMode = ReminderMode.NONE,
    val days: Set<DayOfWeek> = emptySet(),
    val timeOfDayMinutes: Int = 8 * 60,
    val intervalValue: Int = 1,
    val intervalUnit: IntervalUnit = IntervalUnit.DAYS,
    val startAt: Instant = Instant.now(),
    val endMode: ReminderEndMode = ReminderEndMode.NEVER,
    /** Last day (inclusive, local date) a reminder may fire; used with [ReminderEndMode.ON_DATE]. */
    val endDate: LocalDate? = null,
    /** Doses to log before the reminder stops; used with [ReminderEndMode.AFTER_DOSES]. */
    val endAfterDoses: Int? = null,
    /** Doses logged from this moment on count towards [endAfterDoses]. */
    val countFrom: Instant = Instant.now(),
) {
    val isOn: Boolean
        get() = mode != ReminderMode.NONE

    /** True when the end rule is complete and usable (a date, or a count in range). */
    val hasValidEnd: Boolean
        get() = when (endMode) {
            ReminderEndMode.NEVER -> true
            ReminderEndMode.ON_DATE -> endDate != null
            ReminderEndMode.AFTER_DOSES -> endAfterDoses != null && endAfterDoses in END_DOSES_RANGE
        }

    /** The end rule as one comparable value, to detect when the user changed it. */
    val endRule: Triple<ReminderEndMode, LocalDate?, Int?>
        get() = Triple(endMode, endDate, endAfterDoses)

    companion object {
        /** Accepted "every N <unit>" values; anything else would make no sense or overflow date maths. */
        val INTERVAL_RANGE = 1..365

        /** Accepted "end after N doses" values. */
        val END_DOSES_RANGE = 1..1000
    }
}

enum class ReminderEndMode {
    NEVER,
    ON_DATE,
    AFTER_DOSES,
}

/** A reminder attached to one medication. */
data class MedicationReminder(
    val medicationId: Long,
    val config: ReminderConfig,
)
