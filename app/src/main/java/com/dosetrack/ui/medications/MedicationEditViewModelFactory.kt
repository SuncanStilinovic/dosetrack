package com.dosetrack.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dosetrack.di.AppContainer

class MedicationEditViewModelFactory(
    private val container: AppContainer,
    private val medicationId: Long?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MedicationEditViewModel(container.medicationRepository, medicationId) as T
}
