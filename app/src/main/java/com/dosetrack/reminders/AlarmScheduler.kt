package com.dosetrack.reminders

import com.dosetrack.data.settings.ReminderConfig

interface AlarmScheduler {
    /** Computes and arms the next fire time for [config]. After a reboot it is recomputed from the saved config. */
    suspend fun scheduleNext(config: ReminderConfig)

    /** Cancels any pending alarm. */
    fun cancel()
}
