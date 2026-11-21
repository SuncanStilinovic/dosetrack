package com.dosetrack.export

import android.content.Context
import android.net.Uri
import com.dosetrack.data.repository.InjectionRepository
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Writes a full backup (medications, history, settings): the [CsvExporter] text, encrypted with the
 * user's password by [BackupCrypto]. The caller should clear the password array afterwards.
 */
class BackupExporter(
    private val context: Context,
    private val medicationRepository: MedicationRepository,
    private val injectionRepository: InjectionRepository,
    private val settingsRepository: SettingsRepository,
) {

    /** Suggested file name for the system "save" dialog. */
    fun suggestedFileName(): String =
        "dosetrack_backup_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))}." +
            BackupCrypto.FILE_EXTENSION

    /** Writes the backup to a document the user picked (e.g. in Downloads). */
    suspend fun writeBackup(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        val data = buildEncrypted(password)
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Couldn't open the selected location.")
        stream.use { it.write(data) }
    }

    /** Stages the backup in the app cache so it can be handed to a share target. */
    suspend fun createBackupFile(password: CharArray): File = withContext(Dispatchers.IO) {
        val data = buildEncrypted(password)
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        // Earlier backups were only staged here for sharing; don't let them pile up in the cache.
        dir.listFiles()?.forEach { it.delete() }
        File(dir, suggestedFileName()).apply { writeBytes(data) }
    }

    private suspend fun buildEncrypted(password: CharArray): ByteArray {
        val csv = CsvExporter.buildBackup(
            medications = medicationRepository.observeAll().first(),
            entries = injectionRepository.observeAllWithMedication().first(),
            settings = settingsRepository.settingsFlow.first(),
        )
        // Key derivation is deliberately slow (~1 s); keep it off the IO pool.
        return withContext(Dispatchers.Default) { BackupCrypto.encrypt(csv.toByteArray(Charsets.UTF_8), password) }
    }

    private companion object {
        /** Must match the cache-path in res/xml/file_paths.xml. */
        const val EXPORT_DIR = "exports"
    }
}
