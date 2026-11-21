package com.dosetrack.data.repository

import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.db.entities.toEntity
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.model.SitePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class InjectionRepository(private val injectionDao: InjectionDao) {

    fun observeAllWithMedication(): Flow<List<InjectionWithMedication>> =
        injectionDao.observeAllWithMedication().map { list -> list.map { it.toDomain() } }

    suspend fun getById(id: Long): Injection? = injectionDao.getById(id)?.toDomain()

    suspend fun save(injection: Injection): Long {
        return if (injection.id == 0L) {
            injectionDao.insert(injection.toEntity())
        } else {
            injectionDao.update(injection.toEntity())
            injection.id
        }
    }

    suspend fun delete(injection: Injection) = injectionDao.delete(injection.toEntity())

    suspend fun getRecentSites(limit: Int, excludeId: Long = -1): List<SitePoint> =
        injectionDao.getRecentSites(limit, excludeId).map { SitePoint(it.siteX, it.siteY) }
}
