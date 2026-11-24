package com.dosetrack.ui

import com.dosetrack.data.model.AdministrationRoute
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderEndMode
import com.dosetrack.data.model.ReminderMode
import com.dosetrack.data.model.SitePoint
import java.time.DayOfWeek
import java.time.LocalDate
import com.dosetrack.ui.common.InjectionSiteGrids
import com.dosetrack.ui.logentry.LogEntryUiState
import com.dosetrack.ui.medications.MedicationEditUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

class InputValidationTest {

    /** Centre of the first belly cell. */
    private val site = InjectionSiteGrids.grids.first().cells().first().center
    private val offGrid = SitePoint(0.5f, 0.1f)

    @Test
    fun `injection needs a site, a note does not`() {
        assertFalse(LogEntryUiState(isNoteOnly = false, site = null).isValid)
        assertTrue(LogEntryUiState(isNoteOnly = false, site = site).isValid)
        assertTrue(LogEntryUiState(isNoteOnly = true, site = null).isValid)
    }

    @Test
    fun `a dose of a medication that isn't injected needs no site`() {
        val pen = Medication(1, "Pen", 0.5, DosageUnit.ML)
        val tablet = Medication(2, "Tablet", 10.0, DosageUnit.MG, route = AdministrationRoute.ORAL)
        val meds = listOf(pen, tablet)

        val oral = LogEntryUiState(medications = meds, medicationId = 2, site = null)
        assertFalse(oral.needsSite)
        assertTrue(oral.isValid)

        assertFalse(LogEntryUiState(medications = meds, medicationId = 1, site = null).isValid)
        assertFalse(LogEntryUiState(medications = meds, medicationId = null, site = null).isValid)
        assertTrue(LogEntryUiState(medications = meds, medicationId = 1, site = site).isValid)
    }

    @Test
    fun `a new injection must be on a grid cell`() {
        assertFalse(LogEntryUiState(site = offGrid).isValid)
    }

    @Test
    fun `an older entry keeps its off-grid site when edited, but cannot move to another off-grid spot`() {
        assertTrue(LogEntryUiState(site = offGrid, originalSite = offGrid, isEditing = true).isValid)
        assertFalse(LogEntryUiState(site = SitePoint(0.5f, 0.2f), originalSite = offGrid, isEditing = true).isValid)
        assertTrue(LogEntryUiState(site = site, originalSite = offGrid, isEditing = true).isValid)
    }

    @Test
    fun `entry dates must be from 2026 and not in the future`() {
        assertFalse(LogEntryUiState(site = site, timestamp = Instant.parse("2025-12-01T10:00:00Z")).isValid)
        assertFalse(LogEntryUiState(site = site, timestamp = Instant.now().plus(Duration.ofDays(3))).isValid)
        assertTrue(LogEntryUiState(site = site, timestamp = Instant.now()).isValid)
    }

    @Test
    fun `medication dosage accepts only positive finite numbers`() {
        fun state(amount: String) = MedicationEditUiState(name = "Ozempic", dosageAmount = amount)

        assertEquals(0.5, state("0.5").parsedAmount!!, 0.0)
        assertEquals(2.0, state(" 2 ").parsedAmount!!, 0.0)
        listOf("NaN", "Infinity", "0", "-1", "abc", "100001").forEach { bad ->
            assertNull(bad, state(bad).parsedAmount)
            assertFalse(bad, state(bad).isValid)
            assertTrue(bad, state(bad).showAmountError)
        }
    }

    @Test
    fun `empty dosage is invalid but not shown as an error yet`() {
        val state = MedicationEditUiState(name = "Ozempic", dosageAmount = "")
        assertFalse(state.isValid)
        assertFalse(state.showAmountError)
    }

    @Test
    fun `medication needs a name`() {
        assertFalse(MedicationEditUiState(name = "  ", dosageAmount = "1").isValid)
    }

    // region Reminder on the medication screen

    private val today = LocalDate.of(2026, 11, 24)
    private val weekly = ReminderConfig(mode = ReminderMode.DAYS_OF_WEEK, days = setOf(DayOfWeek.MONDAY))

    private fun withReminder(reminder: ReminderConfig, saved: ReminderConfig? = null, doses: Int = 0) =
        MedicationEditUiState(
            name = "Ozempic",
            dosageAmount = "0.5",
            reminder = reminder,
            savedReminder = saved,
            savedDosesLogged = doses,
            today = today,
        )

    @Test
    fun `reminder needs a day, a valid end date and a dose count in range`() {
        assertTrue(withReminder(ReminderConfig()).isValid) // off
        assertTrue(withReminder(weekly).isValid)
        assertFalse(withReminder(weekly.copy(days = emptySet())).isValid)

        val onDate = weekly.copy(endMode = ReminderEndMode.ON_DATE)
        assertFalse(withReminder(onDate).isValid) // no date chosen
        assertTrue(withReminder(onDate.copy(endDate = today)).isValid)
        assertFalse(withReminder(onDate.copy(endDate = today.minusDays(1))).isValid)

        val afterDoses = weekly.copy(endMode = ReminderEndMode.AFTER_DOSES)
        assertFalse(withReminder(afterDoses).isValid) // no count entered
        assertTrue(withReminder(afterDoses.copy(endAfterDoses = 14)).isValid)
        assertFalse(withReminder(afterDoses.copy(endAfterDoses = 0)).isValid)
        assertFalse(withReminder(afterDoses.copy(endAfterDoses = 1001)).isValid)
    }

    @Test
    fun `a saved past end date can be kept while editing other fields`() {
        val ended = weekly.copy(endMode = ReminderEndMode.ON_DATE, endDate = today.minusDays(10))
        assertTrue(withReminder(ended, saved = ended).isValid)
    }

    @Test
    fun `the dose count restarts only when the end rule changes`() {
        val countFrom = Instant.parse("2026-11-01T00:00:00Z")
        val now = Instant.parse("2026-11-24T12:00:00Z")
        val saved = weekly.copy(endMode = ReminderEndMode.AFTER_DOSES, endAfterDoses = 14, countFrom = countFrom)

        val sameRule = withReminder(saved.copy(timeOfDayMinutes = 600), saved = saved, doses = 3)
        assertEquals(3, sameRule.dosesLoggedForDisplay)
        assertEquals(countFrom, sameRule.reminderToSave(now).countFrom)

        val newCount = withReminder(saved.copy(endAfterDoses = 20), saved = saved, doses = 3)
        assertNull(newCount.dosesLoggedForDisplay)
        assertEquals(now, newCount.reminderToSave(now).countFrom)

        assertEquals(now, withReminder(saved).reminderToSave(now).countFrom) // newly turned on
    }

    @Test
    fun `an ended reminder is reported until its end rule changes`() {
        val saved = weekly.copy(endMode = ReminderEndMode.AFTER_DOSES, endAfterDoses = 2)
        assertTrue(withReminder(saved, saved = saved, doses = 2).hasEnded)
        assertFalse(withReminder(saved.copy(endAfterDoses = 5), saved = saved, doses = 2).hasEnded)
    }

    // endregion
}
