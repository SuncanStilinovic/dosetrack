package com.dosetrack.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.data.settings.AppSettings
import com.dosetrack.export.BackupExporter
import com.dosetrack.export.HistoryImporter
import com.dosetrack.export.ImportResult
import com.dosetrack.export.InvalidImportFileException
import com.dosetrack.export.PasswordRequiredException
import com.dosetrack.export.WrongPasswordException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

sealed interface ImportUiState {
    data object Idle : ImportUiState
    data object Running : ImportUiState

    /** The chosen file is encrypted; ask for its password (again, if [wrongPassword]). */
    data class NeedsPassword(val uri: Uri, val wrongPassword: Boolean) : ImportUiState
    data class Done(val result: ImportResult) : ImportUiState
    data class Failed(val message: String) : ImportUiState
}

sealed interface ExportUiState {
    data object Idle : ExportUiState
    data object Running : ExportUiState
    data object Saved : ExportUiState
    data class ReadyToShare(val file: File) : ExportUiState
    data class Failed(val message: String) : ExportUiState
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val historyImporter: HistoryImporter,
    private val backupExporter: BackupExporter,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun updateSiteHistoryCount(count: Int) {
        viewModelScope.launch { settingsRepository.updateSiteHistoryCount(count) }
    }

    fun updateUse24HourClock(use24Hour: Boolean) {
        viewModelScope.launch { settingsRepository.updateUse24HourClock(use24Hour) }
    }

    // region Export

    private val _exportState = MutableStateFlow<ExportUiState>(ExportUiState.Idle)
    val exportState: StateFlow<ExportUiState> = _exportState.asStateFlow()

    /**
     * Held only between the password dialog and the system "save" dialog returning. Kept in memory
     * (never in saved state); if the process dies meanwhile the user is simply asked to retry.
     */
    private var pendingSavePassword: CharArray? = null

    fun suggestedBackupFileName(): String = backupExporter.suggestedFileName()

    fun setPendingSavePassword(password: CharArray) {
        pendingSavePassword?.fill('\u0000')
        pendingSavePassword = password
    }

    /** Called with the location from the system "save" dialog, or null if the user cancelled it. */
    fun saveBackup(uri: Uri?) {
        val password = pendingSavePassword
        pendingSavePassword = null
        if (uri == null) {
            password?.fill('\u0000')
            return
        }
        if (password == null) {
            _exportState.value = ExportUiState.Failed("Please export the backup again.")
            return
        }
        runExport(password) {
            backupExporter.writeBackup(uri, password)
            ExportUiState.Saved
        }
    }

    fun shareBackup(password: CharArray) {
        runExport(password) { ExportUiState.ReadyToShare(backupExporter.createBackupFile(password)) }
    }

    fun onExportHandled() {
        _exportState.value = ExportUiState.Idle
    }

    private fun runExport(password: CharArray, block: suspend () -> ExportUiState) {
        _exportState.value = ExportUiState.Running
        viewModelScope.launch {
            _exportState.value = try {
                block()
            } catch (e: IOException) {
                ExportUiState.Failed("Couldn't save the backup.")
            } catch (e: SecurityException) {
                ExportUiState.Failed("Couldn't save the backup.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ExportUiState.Failed("Couldn't create the backup.")
            } finally {
                password.fill('\u0000')
            }
        }
    }

    // endregion

    // region Import

    private val _importState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    fun importBackup(uri: Uri, password: CharArray? = null) {
        if (_importState.value is ImportUiState.Running) return
        _importState.value = ImportUiState.Running
        viewModelScope.launch {
            _importState.value = try {
                ImportUiState.Done(historyImporter.import(uri, password))
            } catch (e: PasswordRequiredException) {
                ImportUiState.NeedsPassword(uri, wrongPassword = false)
            } catch (e: WrongPasswordException) {
                ImportUiState.NeedsPassword(uri, wrongPassword = true)
            } catch (e: InvalidImportFileException) {
                ImportUiState.Failed(e.message ?: "The file couldn't be imported.")
            } catch (e: IOException) {
                ImportUiState.Failed("The file couldn't be read.")
            } catch (e: SecurityException) {
                ImportUiState.Failed("DoseTrack wasn't given access to that file.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Anything unforeseen in a damaged or hostile file must fail the import, not crash the app.
                // The database transaction has already rolled back at this point.
                ImportUiState.Failed("The file couldn't be imported. It may be damaged.")
            } finally {
                password?.fill('\u0000')
            }
        }
    }

    fun dismissImportResult() {
        _importState.value = ImportUiState.Idle
    }

    // endregion

    override fun onCleared() {
        pendingSavePassword?.fill('\u0000')
    }
}
