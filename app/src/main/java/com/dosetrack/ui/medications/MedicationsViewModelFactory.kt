package com.dosetrack.ui.medications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dosetrack.di.AppContainer

class MedicationsViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MedicationsViewModel(container.medicationRepository) as T
}
