package com.dosetrack.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.data.settings.AppSettings
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.reminders.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val injectionRepository: InjectionRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSiteHistoryCount(count: Int) {
        viewModelScope.launch { settingsRepository.updateSiteHistoryCount(count) }
    }

    fun updateShowInjectionGuide(show: Boolean) {
        viewModelScope.launch { settingsRepository.updateShowInjectionGuide(show) }
    }

    fun updateReminderConfig(config: ReminderConfig) {
        viewModelScope.launch {
            settingsRepository.updateReminderConfig(config)
            alarmScheduler.scheduleNext(config)
        }
    }

    suspend fun getAllEntriesOnce(): List<InjectionWithMedication> =
        injectionRepository.observeAllWithMedication().first()
}
