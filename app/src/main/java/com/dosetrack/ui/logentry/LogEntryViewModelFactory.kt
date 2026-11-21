package com.dosetrack.ui.logentry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dosetrack.di.AppContainer

class LogEntryViewModelFactory(
    private val container: AppContainer,
    private val entryId: Long?,
    private val noteOnly: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LogEntryViewModel(
            container.injectionRepository,
            container.medicationRepository,
            container.settingsRepository,
            entryId,
            noteOnly,
        ) as T
}
