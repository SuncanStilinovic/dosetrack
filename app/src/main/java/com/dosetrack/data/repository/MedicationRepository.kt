package com.dosetrack.data.repository

import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.db.entities.toEntity
import com.dosetrack.data.model.Medication
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MedicationRepository(private val medicationDao: MedicationDao) {

    fun observeActive(): Flow<List<Medication>> =
        medicationDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAll(): Flow<List<Medication>> =
        medicationDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Medication? = medicationDao.getById(id)?.toDomain()

    suspend fun save(medication: Medication): Long {
        return if (medication.id == 0L) {
            medicationDao.insert(medication.toEntity())
        } else {
            medicationDao.update(medication.toEntity())
            medication.id
        }
    }

    suspend fun softDelete(id: Long) = medicationDao.softDelete(id)
}
