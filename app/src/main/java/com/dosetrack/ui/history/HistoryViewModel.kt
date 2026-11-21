package com.dosetrack.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.repository.InjectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val injectionRepository: InjectionRepository) : ViewModel() {

    val entries: StateFlow<List<InjectionWithMedication>> = injectionRepository.observeAllWithMedication()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(injection: Injection) {
        viewModelScope.launch { injectionRepository.delete(injection) }
    }
}
