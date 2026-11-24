package com.dosetrack.reminders

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.dosetrack.R
import com.dosetrack.data.model.Medication
import com.dosetrack.ui.MainActivity

object ReminderNotifier {

    private const val CHANNEL_ID = "injection_reminders"

    /** Notification ids are offset per medication, so reminders for different medications don't replace each other. */
    private const val NOTIFICATION_ID_BASE = 2001

    fun show(context: Context, medication: Medication) {
        ensureChannel(context)

        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val notificationId = NOTIFICATION_ID_BASE + medication.id.toInt()

        // Opens the log screen with this medication selected (handled in MainActivity).
        val contentIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_LOG_MEDICATION_ID, medication.id)
        val pendingIntent = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(contentIntent)
            .getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Shown on a secure lock screen instead of the real content, so it doesn't reveal medical use.
        val publicVersion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_public_title))
            .setContentText(context.getString(R.string.reminder_notification_public_body))
            .build()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title, medication.name))
            .setContentText(context.getString(R.string.reminder_notification_body, medication.dosageLabel))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { lockscreenVisibility = Notification.VISIBILITY_PRIVATE }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}
