package com.dosetrack.ui.common

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dosetrack.util.PermissionUtils

/** The permissions reminders need, and how to ask for each. Refreshed whenever the screen resumes. */
class ReminderPermissions(
    val hasNotificationPermission: Boolean,
    val hasExactAlarmPermission: Boolean,
    val requestNotificationPermission: () -> Unit,
    val requestExactAlarmPermission: () -> Unit,
)

@Composable
fun rememberReminderPermissions(): ReminderPermissions {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(PermissionUtils.canScheduleExactAlarms(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasNotificationPermission = granted }

    // The exact-alarm permission is granted on a system settings page, so re-check when we come back.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
                hasExactAlarmPermission = PermissionUtils.canScheduleExactAlarms(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return ReminderPermissions(
        hasNotificationPermission = hasNotificationPermission,
        hasExactAlarmPermission = hasExactAlarmPermission,
        requestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        requestExactAlarmPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()),
                )
            }
        },
    )
}
