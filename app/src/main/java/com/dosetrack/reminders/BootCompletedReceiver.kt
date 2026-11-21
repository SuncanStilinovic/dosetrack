package com.dosetrack.reminders

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosetrack.DoseTrackApplication
import com.dosetrack.data.settings.ReminderMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms the reminder alarm whenever the system may have dropped or invalidated it:
 * reboot, app update, wall-clock/timezone change, or exact-alarm permission being granted.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as DoseTrackApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = container.settingsRepository.settingsFlow.first()
                if (settings.reminderConfig.mode != ReminderMode.NONE) {
                    container.alarmScheduler.scheduleNext(settings.reminderConfig)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
