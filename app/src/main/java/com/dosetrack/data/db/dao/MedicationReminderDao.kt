package com.dosetrack.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dosetrack.data.db.entities.MedicationReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationReminderDao {
    @Upsert
    suspend fun upsert(reminder: MedicationReminderEntity)

    @Query("DELETE FROM medication_reminders WHERE medicationId = :medicationId")
    suspend fun delete(medicationId: Long)

    @Query("SELECT * FROM medication_reminders WHERE medicationId = :medicationId")
    suspend fun get(medicationId: Long): MedicationReminderEntity?

    /** Reminders of medications that haven't been deleted. */
    @Query(
        "SELECT r.* FROM medication_reminders r JOIN medications m ON m.id = r.medicationId " +
            "WHERE m.isDeleted = 0",
    )
    suspend fun getActive(): List<MedicationReminderEntity>

    @Query(
        "SELECT r.* FROM medication_reminders r JOIN medications m ON m.id = r.medicationId " +
            "WHERE m.isDeleted = 0",
    )
    fun observeActive(): Flow<List<MedicationReminderEntity>>

    @Query("SELECT * FROM medication_reminders")
    suspend fun getAll(): List<MedicationReminderEntity>
}
