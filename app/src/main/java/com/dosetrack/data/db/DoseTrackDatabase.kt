package com.dosetrack.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.MedicationEntity

@Database(
    entities = [MedicationEntity::class, InjectionEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DoseTrackDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun injectionDao(): InjectionDao

    companion object {
        const val DATABASE_NAME = "dosetrack.db"
    }
}
