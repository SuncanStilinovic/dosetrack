package com.dosetrack.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.ReminderRepository
import com.dosetrack.reminders.ReminderCoordinator
import com.dosetrack.reminders.ReminderTimeCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MedicationListItem(
    val medication: Medication,
    val reminder: ReminderConfig?,
    val dosesLogged: Int,
    val reminderEnded: Boolean,
)

class MedicationsViewModel(
    private val repository: MedicationRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderCoordinator: ReminderCoordinator,
    injectionRepository: InjectionRepository,
) : ViewModel() {

    /** Re-emits when a dose is logged too, since doses count towards "end after N doses". */
    val medications: StateFlow<List<MedicationListItem>> = combine(
        repository.observeActive(),
        reminderRepository.observeActive(),
        injectionRepository.observeAllWithMedication(),
    ) { medications, reminders, entries ->
        medications.map { medication ->
            val reminder = reminders[medication.id]
            val doses = reminder?.let { config ->
                entries.count {
                    it.injection.medicationId == medication.id &&
                        !it.injection.isNoteOnly &&
                        !it.injection.timestamp.isBefore(config.countFrom)
                }
            } ?: 0
            MedicationListItem(
                medication = medication,
                reminder = reminder,
                dosesLogged = doses,
                reminderEnded = reminder != null && ReminderTimeCalculator.hasEnded(reminder, doses),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMedication(id: Long) {
        viewModelScope.launch {
            repository.softDelete(id)
            reminderRepository.delete(id)
            reminderCoordinator.cancel(id)
        }
    }
}
