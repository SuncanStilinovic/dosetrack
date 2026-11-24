package com.dosetrack.reminders

import com.dosetrack.data.repository.ReminderRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one place that turns saved reminders into armed alarms. Call it after anything that can
 * change when a reminder should next fire: a reminder or medication edit, a logged or deleted dose
 * (doses count towards "end after N doses"), an import, an alarm firing, or a reboot.
 */
class ReminderCoordinator(
    private val reminderRepository: ReminderRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    private val mutex = Mutex()

    suspend fun rescheduleAll() = mutex.withLock {
        // Reminders used to be a single app-wide alarm; make sure it can't fire any more.
        alarmScheduler.cancelLegacy()
        reminderRepository.getActive().forEach { schedule(it.medicationId) }
    }

    suspend fun reschedule(medicationId: Long) = mutex.withLock { schedule(medicationId) }

    fun cancel(medicationId: Long) = alarmScheduler.cancel(medicationId)

    private suspend fun schedule(medicationId: Long) {
        val config = reminderRepository.get(medicationId)
        if (config == null) {
            alarmScheduler.cancel(medicationId)
            return
        }
        val doses = reminderRepository.dosesLogged(medicationId, config)
        alarmScheduler.schedule(medicationId, ReminderTimeCalculator.computeNextFireTime(config, dosesLogged = doses))
    }
}
