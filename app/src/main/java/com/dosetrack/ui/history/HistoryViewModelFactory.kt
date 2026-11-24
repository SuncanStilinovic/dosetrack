package com.dosetrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dosetrack.di.AppContainer

class HistoryViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HistoryViewModel(container.injectionRepository, container.reminderCoordinator) as T
}
