package com.dosetrack.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DataRulesTest {

    private val now = Instant.parse("2026-11-21T12:00:00Z")

    @Test
    fun `nothing before 2026`() {
        assertFalse(DataRules.isValidRecordInstant(Instant.parse("2025-06-01T00:00:00Z"), now))
        assertFalse(DataRules.isValidRecordInstant(Instant.parse("1970-01-01T00:00:00Z"), now))
    }

    @Test
    fun `the first minutes of 2026 count in every time zone`() {
        // 00:00 on 1 January 2026 in the earliest (UTC+14) and latest (UTC-12) zones.
        listOf(14, 1, 0, -12).forEach { hours ->
            val midnight = LocalDateTime.of(2026, 1, 1, 0, 0).toInstant(ZoneOffset.ofHours(hours))
            assertTrue("UTC$hours", DataRules.isValidRecordInstant(midnight, now))
        }
    }

    @Test
    fun `records cannot be in the future beyond clock skew`() {
        assertTrue(DataRules.isValidRecordInstant(now, now))
        assertTrue(DataRules.isValidRecordInstant(now.plus(Duration.ofHours(23)), now))
        assertFalse(DataRules.isValidRecordInstant(now.plus(Duration.ofDays(2)), now))
    }

    @Test
    fun `reminder start may be up to ten years ahead`() {
        assertTrue(DataRules.isValidReminderStart(now.plus(Duration.ofDays(365)), now))
        assertTrue(DataRules.isValidReminderStart(now.plus(Duration.ofDays(3650)), now))
        assertFalse(DataRules.isValidReminderStart(now.plus(Duration.ofDays(3660)), now))
        assertFalse(DataRules.isValidReminderStart(Instant.parse("2025-06-01T00:00:00Z"), now))
    }

    @Test
    fun `dosage must be positive, finite and bounded`() {
        assertTrue(DataRules.isValidDosageAmount(0.25))
        assertTrue(DataRules.isValidDosageAmount(DataRules.MAX_DOSAGE_AMOUNT))
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, DataRules.MAX_DOSAGE_AMOUNT + 1).forEach {
            assertFalse("$it", DataRules.isValidDosageAmount(it))
        }
    }
}
