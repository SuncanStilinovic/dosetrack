package com.dosetrack.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate

/**
 * Validation limits shared by the input screens and the backup importer, so anything the app lets
 * you enter can always be restored, and nothing a backup contains can exceed what the app allows.
 */
object DataRules {

    /** DoseTrack was first released in 2026, so no record can be older than that. */
    val EARLIEST_DATE: LocalDate = LocalDate.of(2026, 1, 1)

    /**
     * Start of 1 January 2026 in the easternmost time zone (UTC+14), i.e. the earliest instant that is
     * 2026 *somewhere*. Zone-independent, so a backup made in one zone imports in any other.
     */
    val EARLIEST_INSTANT: Instant = Instant.parse("2025-12-31T10:00:00Z")

    /** Imported records may be slightly "in the future" when the two phones' clocks differ. */
    val CLOCK_SKEW_TOLERANCE: Duration = Duration.ofDays(1)

    /** A reminder schedule may start up to this far ahead (matches the reminder date picker). */
    val REMINDER_START_MAX_AHEAD: Duration = Duration.ofDays(3650)

    const val MAX_NAME_LENGTH = 200
    const val MAX_NOTE_LENGTH = 10_000
    const val MAX_DOSAGE_AMOUNT = 100_000.0

    /** For injections, notes and medication records: 2026 up to now. */
    fun isValidRecordInstant(instant: Instant, now: Instant = Instant.now()): Boolean =
        !instant.isBefore(EARLIEST_INSTANT) && !instant.isAfter(now.plus(CLOCK_SKEW_TOLERANCE))

    fun isValidReminderStart(instant: Instant, now: Instant = Instant.now()): Boolean =
        !instant.isBefore(EARLIEST_INSTANT) &&
            !instant.isAfter(now.plus(REMINDER_START_MAX_AHEAD).plus(CLOCK_SKEW_TOLERANCE))

    fun isValidDosageAmount(amount: Double): Boolean =
        amount.isFinite() && amount > 0.0 && amount <= MAX_DOSAGE_AMOUNT
}
