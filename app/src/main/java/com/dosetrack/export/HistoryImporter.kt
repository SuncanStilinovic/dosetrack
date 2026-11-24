package com.dosetrack.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dosetrack.data.db.DoseTrackDatabase
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.reminders.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Restores a backup: a password-protected file from [BackupExporter] (see [BackupCrypto]), or an
 * unencrypted CSV from earlier app versions.
 *
 * Medications and history are written by [BackupRestorer] in one transaction, so a failure leaves
 * the database untouched. Settings, when the file has them, replace the current ones and the
 * reminder is re-armed.
 */
class HistoryImporter(
    private val context: Context,
    private val database: DoseTrackDatabase,
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
) {

    /**
     * @param password needed for encrypted backups; the caller should clear it afterwards.
     * @throws PasswordRequiredException if the file is encrypted and [password] is null.
     * @throws WrongPasswordException if [password] doesn't open the file.
     */
    suspend fun import(uri: Uri, password: CharArray?): ImportResult = withContext(Dispatchers.IO) {
        val bytes = readBytes(uri)
        val plaintext = if (BackupCrypto.isEncrypted(bytes)) {
            BackupCrypto.decrypt(bytes, password ?: throw PasswordRequiredException())
        } else {
            bytes
        }
        val parsed = CsvImporter.parse(plaintext.toString(Charsets.UTF_8))
        val result = database.withTransaction {
            BackupRestorer(database.medicationDao(), database.injectionDao()).restore(parsed)
        }

        parsed.settings?.let { settings ->
            settingsRepository.updateSiteHistoryCount(settings.siteHistoryCount)
            settingsRepository.updateShowInjectionGuide(settings.showInjectionGuide)
            settings.use24HourClock?.let { settingsRepository.updateUse24HourClock(it) }
            val reminder = settings.reminderConfig
            settingsRepository.updateReminderConfig(reminder)
            alarmScheduler.scheduleNext(reminder)
        }
        result.copy(settingsRestored = parsed.settings != null)
    }

    private fun readBytes(uri: Uri): ByteArray {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Couldn't open the selected file.")
        val bytes = stream.use { it.readAtMost(MAX_FILE_BYTES + 1) }
        if (bytes.size > MAX_FILE_BYTES) {
            throw InvalidImportFileException("The file is too large to be a DoseTrack backup.")
        }
        return bytes
    }

    private fun InputStream.readAtMost(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (out.size() < limit) {
            val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
            if (read < 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
    }
}
