package com.dosetrack.ui.logentry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.SitePoint
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.SettingsRepository
import kotlinx.coroutines.launch
import java.time.Instant

class LogEntryViewModel(
    private val injectionRepository: InjectionRepository,
    private val medicationRepository: MedicationRepository,
    private val settingsRepository: SettingsRepository,
    private val entryId: Long?,
    noteOnlyArg: Boolean,
) : ViewModel() {

    var uiState by mutableStateOf(LogEntryUiState(isNoteOnly = noteOnlyArg, isEditing = entryId != null))
        private set

    init {
        viewModelScope.launch {
            medicationRepository.observeActive().collect { meds ->
                uiState = uiState.copy(medications = meds)
            }
        }
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                uiState = uiState.copy(showGuide = settings.showInjectionGuide)
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
        uiState = uiState.copy(site = site)
    }

    fun updateMedicationId(medicationId: Long?) {
        uiState = uiState.copy(medicationId = medicationId)
    }

    fun updateHadReaction(hadReaction: Boolean) {
        uiState = uiState.copy(hadReaction = hadReaction)
    }

    fun updateReactionNote(note: String) {
        uiState = uiState.copy(reactionNote = note)
    }

    fun updateNotes(notes: String) {
        uiState = uiState.copy(notes = notes)
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
                site = if (uiState.isNoteOnly) null else uiState.site,
                medicationId = uiState.medicationId,
                hadReaction = uiState.hadReaction,
                reactionNote = uiState.reactionNote.takeIf { uiState.hadReaction && it.isNotBlank() },
                notes = uiState.notes.takeIf { it.isNotBlank() },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            injectionRepository.save(injection)
            onDone()
        }
    }
}
