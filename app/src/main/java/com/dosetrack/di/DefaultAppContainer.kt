package com.dosetrack.di

import android.content.Context
import androidx.room.Room
import com.dosetrack.data.db.DoseTrackDatabase
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.ReminderRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.export.BackupExporter
import com.dosetrack.export.HistoryImporter
import com.dosetrack.reminders.AndroidAlarmScheduler
import com.dosetrack.reminders.ReminderCoordinator

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DoseTrackDatabase by lazy {
        Room.databaseBuilder(context, DoseTrackDatabase::class.java, DoseTrackDatabase.DATABASE_NAME).build()
    }

    override val medicationRepository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }

    override val injectionRepository: InjectionRepository by lazy {
        InjectionRepository(database.injectionDao())
    }

    override val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.medicationReminderDao(), database.injectionDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val reminderCoordinator: ReminderCoordinator by lazy {
        ReminderCoordinator(reminderRepository, AndroidAlarmScheduler(context))
    }

    override val historyImporter: HistoryImporter by lazy {
        HistoryImporter(context, database, settingsRepository, reminderCoordinator)
    }

    override val backupExporter: BackupExporter by lazy {
        BackupExporter(context, medicationRepository, injectionRepository, reminderRepository, settingsRepository)
    }
}
