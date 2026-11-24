package com.dosetrack.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import java.time.Instant

class AndroidAlarmScheduler(
    private val context: Context,
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(medicationId: Long, at: Instant?) {
        if (at == null) {
            cancel(medicationId)
            return
        }
        val pendingIntent = buildPendingIntent(medicationId)
        val triggerAtMillis = at.toEpochMilli()

        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    override fun cancel(medicationId: Long) {
        alarmManager.cancel(buildPendingIntent(medicationId))
    }

    override fun cancelLegacy() {
        // Must match the old PendingIntent exactly: same receiver, no data, request code 1001.
        val legacy = PendingIntent.getBroadcast(
            context,
            LEGACY_REQUEST_CODE,
            Intent(context, ReminderBroadcastReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(legacy)
        legacy.cancel()
    }

    /** The data URI makes each medication's PendingIntent distinct, so alarms don't replace each other. */
    private fun buildPendingIntent(medicationId: Long): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
            .setData("dosetrack://reminder/$medicationId".toUri())
            .putExtra(ReminderBroadcastReceiver.EXTRA_MEDICATION_ID, medicationId)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val REQUEST_CODE = 1002
        const val LEGACY_REQUEST_CODE = 1001
    }
}
