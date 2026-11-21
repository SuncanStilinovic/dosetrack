package com.dosetrack.di

import android.content.Context
import androidx.room.Room
import com.dosetrack.data.db.DoseTrackDatabase
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.reminders.AlarmScheduler
import com.dosetrack.reminders.AndroidAlarmScheduler

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val database: DoseTrackDatabase by lazy {
        Room.databaseBuilder(context, DoseTrackDatabase::class.java, DoseTrackDatabase.DATABASE_NAME).build()
    }

    override val medicationRepository: MedicationRepository by lazy {
        MedicationRepository(database.medicationDao())
    }

    override val injectionRepository: InjectionRepository by lazy {
        InjectionRepository(database.injectionDao())
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    override val alarmScheduler: AlarmScheduler by lazy {
        AndroidAlarmScheduler(context, settingsRepository)
    }
}
