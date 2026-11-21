package com.dosetrack.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.dosetrack.export.BackupCrypto
import com.dosetrack.export.ImportResult
import com.dosetrack.export.ShareIntentFactory
import com.dosetrack.util.PermissionUtils

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(container))
    val settings by viewModel.settings.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val exportState by viewModel.exportState.collectAsState()

    /** Which export action the password dialog is collecting a password for; null = dialog hidden. */
    var exportPasswordFor by remember { mutableStateOf<ExportTarget?>(null) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(BackupCrypto.MIME_TYPE),
    ) { uri -> viewModel.saveBackup(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { viewModel.importBackup(it) } }

    LaunchedEffect(exportState) {
        when (val state = exportState) {
            ExportUiState.Saved -> Toast.makeText(context, "Backup saved.", Toast.LENGTH_LONG).show()
            is ExportUiState.ReadyToShare ->
                context.startActivity(ShareIntentFactory.createShareIntent(context, state.file))
            is ExportUiState.Failed -> Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
            else -> return@LaunchedEffect
        }
        viewModel.onExportHandled()
    }

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
        val isExporting = exportState is ExportUiState.Running
        Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { exportPasswordFor = ExportTarget.SAVE }, enabled = !isExporting) {
                Text(if (isExporting) "Encrypting…" else "Export backup")
            }
            OutlinedButton(
                onClick = { exportPasswordFor = ExportTarget.SHARE },
                enabled = !isExporting,
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text("Share")
            }
        }
        Text(
            text = "Saves all medications, history and settings to one password-protected file " +
                "you can import on another device.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )

        val isImporting = importState is ImportUiState.Running
        Button(
            onClick = { importLauncher.launch(IMPORT_MIME_TYPES) },
            enabled = !isImporting,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(if (isImporting) "Importing…" else "Import backup")
        }
        Text(
            text = "Restores a file created with Export backup. Entries already in the app are skipped; " +
                "settings are replaced by the ones in the backup.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }

    when (val state = importState) {
        is ImportUiState.Done -> AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            confirmButton = { TextButton(onClick = viewModel::dismissImportResult) { Text("OK") } },
            title = { Text("Import complete") },
            text = { Text(state.result.summary()) },
        )
        is ImportUiState.NeedsPassword -> BackupPasswordDialog(
            isNewPassword = false,
            errorMessage = if (state.wrongPassword) "Wrong password" else null,
            onConfirm = { password -> viewModel.importBackup(state.uri, password) },
            onDismiss = viewModel::dismissImportResult,
        )
        is ImportUiState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismissImportResult,
            confirmButton = { TextButton(onClick = viewModel::dismissImportResult) { Text("OK") } },
            title = { Text("Import failed") },
            text = { Text(state.message) },
        )
        else -> Unit
    }

    exportPasswordFor?.let { target ->
        BackupPasswordDialog(
            isNewPassword = true,
            onConfirm = { password ->
                exportPasswordFor = null
                when (target) {
                    ExportTarget.SAVE -> {
                        viewModel.setPendingSavePassword(password)
                        saveBackupLauncher.launch(viewModel.suggestedBackupFileName())
                    }
                    ExportTarget.SHARE -> viewModel.shareBackup(password)
                }
            },
            onDismiss = { exportPasswordFor = null },
        )
    }
}

private enum class ExportTarget { SAVE, SHARE }

/** Broad on purpose: .dtbackup is reported as octet-stream, and providers label .csv files inconsistently. */
private val IMPORT_MIME_TYPES = arrayOf(
    BackupCrypto.MIME_TYPE,
    // Unencrypted CSV backups from earlier app versions.
    "text/csv",
    "text/comma-separated-values",
    "application/csv",
    "text/plain",
)

private fun ImportResult.summary(): String = buildString {
    append("Imported $imported ${if (imported == 1) "entry" else "entries"}.")
    if (settingsRestored) append("\nSettings restored.")
    if (medicationsCreated > 0) append("\nAdded $medicationsCreated new ${if (medicationsCreated == 1) "medication" else "medications"}.")
    if (duplicates > 0) append("\nSkipped $duplicates already in the app.")
    if (invalid > 0) append("\nSkipped $invalid unreadable ${if (invalid == 1) "row" else "rows"}.")
}
