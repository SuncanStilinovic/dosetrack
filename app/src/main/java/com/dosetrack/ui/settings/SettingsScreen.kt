package com.dosetrack.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosetrack.di.appContainer
import com.dosetrack.export.CsvExporter
import com.dosetrack.export.ShareIntentFactory
import com.dosetrack.util.PermissionUtils
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(container))
    val settings by viewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var hasNotificationPermission by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var hasExactAlarmPermission by remember { mutableStateOf(PermissionUtils.canScheduleExactAlarms(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> hasNotificationPermission = granted }

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Display")
        OutlinedTextField(
            value = settings.siteHistoryCount.toString(),
            onValueChange = { value -> value.toIntOrNull()?.let { viewModel.updateSiteHistoryCount(it) } },
            label = { Text("Previous sites to show") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Show injection guide", modifier = Modifier.weight(1f))
            Switch(
                checked = settings.showInjectionGuide,
                onCheckedChange = viewModel::updateShowInjectionGuide,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        ReminderConfigSection(
            config = settings.reminderConfig,
            onConfigChange = viewModel::updateReminderConfig,
            hasNotificationPermission = hasNotificationPermission,
            hasExactAlarmPermission = hasExactAlarmPermission,
            onRequestNotificationPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onRequestExactAlarmPermission = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Data")
        Button(
            onClick = {
                coroutineScope.launch {
                    val entries = viewModel.getAllEntriesOnce()
                    val csv = CsvExporter.buildCsv(entries)
                    val intent = ShareIntentFactory.createShareIntent(context, csv)
                    context.startActivity(intent)
                }
            },
            modifier = Modifier.padding(top = 8.dp),
        ) {
            Text("Export / Share history")
        }
    }
}
