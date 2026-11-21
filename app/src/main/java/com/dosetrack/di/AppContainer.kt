package com.dosetrack.di

import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.export.BackupExporter
import com.dosetrack.export.HistoryImporter
import com.dosetrack.reminders.AlarmScheduler

interface AppContainer {
    val medicationRepository: MedicationRepository
    val injectionRepository: InjectionRepository
    val settingsRepository: SettingsRepository
    val alarmScheduler: AlarmScheduler
    val historyImporter: HistoryImporter
    val backupExporter: BackupExporter
}
