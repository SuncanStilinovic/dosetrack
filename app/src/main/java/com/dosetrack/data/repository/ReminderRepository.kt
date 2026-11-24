package com.dosetrack.data.repository

import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationReminderDao
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.db.entities.toEntity
import com.dosetrack.data.model.MedicationReminder
import com.dosetrack.data.model.ReminderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Per-medication reminders. A medication without a stored reminder has none. */
class ReminderRepository(
    private val reminderDao: MedicationReminderDao,
    private val injectionDao: InjectionDao,
) {

    /** Reminders of active medications, keyed by medication id. */
    fun observeActive(): Flow<Map<Long, ReminderConfig>> =
        reminderDao.observeActive().map { list -> list.associate { it.medicationId to it.toDomain().config } }

    suspend fun getActive(): List<MedicationReminder> = reminderDao.getActive().map { it.toDomain() }

    suspend fun get(medicationId: Long): ReminderConfig? = reminderDao.get(medicationId)?.toDomain()?.config

    /** Saves the reminder; turning it off removes it. */
    suspend fun save(medicationId: Long, config: ReminderConfig) {
        if (config.isOn) {
            reminderDao.upsert(MedicationReminder(medicationId, config).toEntity())
        } else {
            reminderDao.delete(medicationId)
        }
    }

    suspend fun delete(medicationId: Long) = reminderDao.delete(medicationId)

    /** Doses of the medication logged since the reminder's dose count started. */
    suspend fun dosesLogged(medicationId: Long, config: ReminderConfig): Int =
        injectionDao.countDoses(medicationId, config.countFrom.toEpochMilli())
}
