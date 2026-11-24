package com.dosetrack.ui.logentry

import com.dosetrack.data.DataRules
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.SitePoint
import com.dosetrack.ui.common.InjectionSiteGrids
import java.time.Instant

data class LogEntryUiState(
    val isNoteOnly: Boolean = false,
    val isEditing: Boolean = false,
    val timestamp: Instant = Instant.now(),
    val site: SitePoint? = null,
    /** Site the entry had when opened for editing; may predate the grids and lie outside them. */
    val originalSite: SitePoint? = null,
    val medicationId: Long? = null,
    val hadReaction: Boolean = false,
    val reactionNote: String = "",
    val notes: String = "",
    val medications: List<Medication> = emptyList(),
    val historicalSites: List<SitePoint> = emptyList(),
) {
    val selectedMedication: Medication?
        get() = medications.firstOrNull { it.id == medicationId }

    /**
     * Whether this entry is placed on the body diagram. Notes and doses of non-injected medications
     * have no site; with no medication (or one no longer listed) it is treated as an injection.
     */
    val needsSite: Boolean
        get() = !isNoteOnly && (selectedMedication?.route?.isInjected ?: true)

    val isValid: Boolean
        get() = (!needsSite || hasValidSite) && DataRules.isValidRecordInstant(timestamp)

    /** A grid cell, or an older entry's existing off-grid site left unchanged. */
    private val hasValidSite: Boolean
        get() = site != null && (InjectionSiteGrids.isOnGrid(site) || site == originalSite)
}
