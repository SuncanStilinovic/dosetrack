package com.dosetrack.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.MedicationEntity

@Database(
    entities = [MedicationEntity::class, InjectionEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        // 1 → 2: adds medications.route; existing medications become injections.
        AutoMigration(from = 1, to = 2),
    ],
)
abstract class DoseTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun injectionDao(): InjectionDao

    companion object {
        const val DATABASE_NAME = "dosetrack.db"
    }
}
