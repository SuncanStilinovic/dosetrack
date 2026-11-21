package com.dosetrack.ui.logentry

import com.dosetrack.data.DataRules
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.SitePoint
import java.time.Instant

data class LogEntryUiState(
    val isNoteOnly: Boolean = false,
    val isEditing: Boolean = false,
    val timestamp: Instant = Instant.now(),
    val site: SitePoint? = null,
    val medicationId: Long? = null,
    val hadReaction: Boolean = false,
    val reactionNote: String = "",
    val notes: String = "",
    val medications: List<Medication> = emptyList(),
    val historicalSites: List<SitePoint> = emptyList(),
    val showGuide: Boolean = false,
) {
    val isValid: Boolean
        get() = (isNoteOnly || site != null) && DataRules.isValidRecordInstant(timestamp)
}
