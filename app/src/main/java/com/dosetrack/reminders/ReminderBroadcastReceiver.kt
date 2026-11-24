package com.dosetrack.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dosetrack.DoseTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires for one medication's reminder: shows its notification and arms the next occurrence. */
class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        if (medicationId < 0) return

        val pendingResult = goAsync()
        val container = (context.applicationContext as DoseTrackApplication).container
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Only notify for a medication that still exists and still has a reminder.
                val medication = container.medicationRepository.getById(medicationId)?.takeIf { !it.isDeleted }
                if (medication != null && container.reminderRepository.get(medicationId) != null) {
                    ReminderNotifier.show(context, medication)
                }
                container.reminderCoordinator.reschedule(medicationId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_MEDICATION_ID = "com.dosetrack.extra.MEDICATION_ID"
    }
}
