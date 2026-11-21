package com.dosetrack.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosetrack.DoseTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ReminderNotifier.show(context)

        val pendingResult = goAsync()
        val container = (context.applicationContext as DoseTrackApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = container.settingsRepository.settingsFlow.first()
                container.alarmScheduler.scheduleNext(settings.reminderConfig)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
