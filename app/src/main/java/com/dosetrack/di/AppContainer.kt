package com.dosetrack.di

import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.ReminderRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.export.BackupExporter
import com.dosetrack.export.HistoryImporter
import com.dosetrack.reminders.ReminderCoordinator

interface AppContainer {
    val medicationRepository: MedicationRepository
    val injectionRepository: InjectionRepository
    val reminderRepository: ReminderRepository
    val settingsRepository: SettingsRepository
    val reminderCoordinator: ReminderCoordinator
    val historyImporter: HistoryImporter
    val backupExporter: BackupExporter
}
