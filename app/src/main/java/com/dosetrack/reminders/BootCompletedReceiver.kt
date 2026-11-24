package com.dosetrack.reminders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosetrack.DoseTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms every medication's reminder alarm whenever the system may have dropped or invalidated it:
 * reboot, app update, wall-clock/timezone change, or exact-alarm permission being granted.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESCHEDULE_ACTIONS) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as DoseTrackApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // After an update from 1.3.x or earlier, this also discards the old app-wide reminder.
                container.settingsRepository.removeLegacyReminder()
                container.reminderCoordinator.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        // The exact-alarm action is API 31+. It's a compile-time String constant, so listing it is safe on
        // API 30: Android 11 never sends that broadcast, and the other actions still reschedule as usual.
        @SuppressLint("InlinedApi")
        val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
