package com.dosetrack.reminders

import com.dosetrack.data.settings.ReminderConfig

interface AlarmScheduler {
    /** Computes and arms the next fire time for [config], persisting it so it survives reboot. */
    suspend fun scheduleNext(config: ReminderConfig)

    /** Cancels any pending alarm. */
    fun cancel()
}
