package com.dosetrack.ui.logentry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.DataRules
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.SitePoint
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.reminders.ReminderCoordinator
import com.dosetrack.ui.common.InjectionSiteGrids
import kotlinx.coroutines.launch
import java.time.Instant

class LogEntryViewModel(
    private val injectionRepository: InjectionRepository,
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val reminderCoordinator: ReminderCoordinator,
    private val entryId: Long?,
    noteOnlyArg: Boolean,
    preselectedMedicationId: Long? = null,
) : ViewModel() {

    var uiState by mutableStateOf(
        LogEntryUiState(
            isNoteOnly = noteOnlyArg,
            isEditing = entryId != null,
            // Only for new entries (e.g. opened from a reminder); an edited entry keeps its own medication.
            medicationId = preselectedMedicationId.takeIf { entryId == null && !noteOnlyArg },
        ),
    )
        private set

    init {
        viewModelScope.launch {
            medicationRepository.observeActive().collect { meds ->
                uiState = uiState.copy(medications = meds)
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                if (!uiState.isNoteOnly) {
                    val sites = injectionRepository.getRecentSites(settings.siteHistoryCount, entryId ?: -1)
                    uiState = uiState.copy(historicalSites = sites)
                }
            }
        }
        if (entryId != null) {
            viewModelScope.launch {
                injectionRepository.getById(entryId)?.let { injection ->
                    uiState = uiState.copy(
                        isNoteOnly = injection.isNoteOnly,
                        timestamp = injection.timestamp,
                        site = injection.site,
                        originalSite = injection.site,
                        medicationId = injection.medicationId,
                        hadReaction = injection.hadReaction,
                        reactionNote = injection.reactionNote ?: "",
                        notes = injection.notes ?: "",
                    )
                }
            }
        }
    }

    fun updateTimestamp(timestamp: Instant) {
        uiState = uiState.copy(timestamp = timestamp)
    }

    fun updateSite(site: SitePoint) {
        val snapped = InjectionSiteGrids.snapToCell(site) ?: return
        uiState = uiState.copy(site = snapped)
    }

    fun updateMedicationId(medicationId: Long?) {
        uiState = uiState.copy(medicationId = medicationId)
    }

    fun updateHadReaction(hadReaction: Boolean) {
        uiState = uiState.copy(hadReaction = hadReaction)
    }

    fun updateReactionNote(note: String) {
        uiState = uiState.copy(reactionNote = note.take(DataRules.MAX_NOTE_LENGTH))
    }

    fun updateNotes(notes: String) {
        uiState = uiState.copy(notes = notes.take(DataRules.MAX_NOTE_LENGTH))
    }

    fun save(onDone: () -> Unit) {
        if (!uiState.isValid) return
        viewModelScope.launch {
            val existing = entryId?.let { injectionRepository.getById(it) }
            val now = Instant.now()
            val injection = Injection(
                id = entryId ?: 0,
                timestamp = uiState.timestamp,
                isNoteOnly = uiState.isNoteOnly,
                site = if (uiState.needsSite) uiState.site else null,
                medicationId = uiState.medicationId,
                hadReaction = uiState.hadReaction,
                reactionNote = uiState.reactionNote.takeIf { uiState.hadReaction && it.isNotBlank() },
                notes = uiState.notes.takeIf { it.isNotBlank() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            injectionRepository.save(injection)
            // A dose may complete an "end after N doses" reminder; an edit may move it between medications.
            reminderCoordinator.rescheduleAll()
            onDone()
        }
    }
}
