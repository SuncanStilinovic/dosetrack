package com.dosetrack.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.model.Medication
import com.dosetrack.data.repository.MedicationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationsViewModel(private val repository: MedicationRepository) : ViewModel() {

    val medications: StateFlow<List<Medication>> = repository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMedication(id: Long) {
        viewModelScope.launch { repository.softDelete(id) }
    }
}
