package com.dosetrack.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.dao.MedicationReminderDao
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.MedicationEntity
import com.dosetrack.data.db.entities.MedicationReminderEntity

@Database(
    entities = [MedicationEntity::class, InjectionEntity::class, MedicationReminderEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        // 1 → 2: adds medications.route; existing medications become injections.
        AutoMigration(from = 1, to = 2),
        // 2 → 3: adds the medication_reminders table (one reminder per medication).
        AutoMigration(from = 2, to = 3),
    ],
)
abstract class DoseTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun injectionDao(): InjectionDao
    abstract fun medicationReminderDao(): MedicationReminderDao

    companion object {
        const val DATABASE_NAME = "dosetrack.db"
    }
}
