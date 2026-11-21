package com.dosetrack.ui

import com.dosetrack.data.model.SitePoint
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
}
