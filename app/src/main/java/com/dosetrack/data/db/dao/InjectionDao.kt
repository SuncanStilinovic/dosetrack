package com.dosetrack.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.InjectionWithMedicationEntity
import com.dosetrack.data.db.entities.SiteCoordinates
import kotlinx.coroutines.flow.Flow

@Dao
interface InjectionDao {
    @Insert
    suspend fun insert(injection: InjectionEntity): Long

    @Update
    suspend fun update(injection: InjectionEntity)

    @Delete
    suspend fun delete(injection: InjectionEntity)

    @Query("SELECT * FROM injections WHERE id = :id")
    suspend fun getById(id: Long): InjectionEntity?

    /** Doses (not notes) of one medication logged at or after [fromMillis]; counts towards a reminder's end. */
    @Query(
        "SELECT COUNT(*) FROM injections " +
            "WHERE medicationId = :medicationId AND isNoteOnly = 0 AND timestamp >= :fromMillis",
    )
    suspend fun countDoses(medicationId: Long, fromMillis: Long): Int

    @Query("SELECT * FROM injections WHERE timestamp >= :fromMillis AND timestamp < :toMillis")
    suspend fun getInRange(fromMillis: Long, toMillis: Long): List<InjectionEntity>

    @Transaction
    @Query("SELECT * FROM injections ORDER BY timestamp DESC")
    fun observeAllWithMedication(): Flow<List<InjectionWithMedicationEntity>>

    @Query(
        "SELECT siteX, siteY FROM injections " +
            "WHERE isNoteOnly = 0 AND siteX IS NOT NULL AND siteY IS NOT NULL AND id != :excludeId " +
            "ORDER BY timestamp DESC LIMIT :limit",
    )
    suspend fun getRecentSites(limit: Int, excludeId: Long = -1): List<SiteCoordinates>
}
