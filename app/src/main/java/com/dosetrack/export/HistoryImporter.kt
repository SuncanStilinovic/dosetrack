package com.dosetrack.export

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.dosetrack.data.db.DoseTrackDatabase
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.MedicationEntity
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.model.Medication
import com.dosetrack.data.repository.SettingsRepository
import com.dosetrack.reminders.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.temporal.ChronoUnit

data class ImportResult(
    val imported: Int,
    val duplicates: Int,
    val invalid: Int,
    val medicationsCreated: Int,
    val settingsRestored: Boolean,
)

/**
 * Restores a backup: a password-protected file from [BackupExporter] (see [BackupCrypto]), or an
 * unencrypted CSV from earlier app versions.
 *
 * Medications and history go in one transaction, so a failure leaves the database untouched.
 * Medications are matched to existing ones by name + dosage; history rows already present
 * (same minute, site, notes and medication) are skipped, so restoring the same file twice is
 * harmless. Settings, when the file has them, replace the current ones and the reminder is re-armed.
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
        val result = database.withTransaction { insert(parsed) }

        parsed.settings?.let { settings ->
            settingsRepository.updateSiteHistoryCount(settings.siteHistoryCount)
            settingsRepository.updateShowInjectionGuide(settings.showInjectionGuide)
            val reminder = settings.reminderConfig.copy(nextFireAt = null)
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

    private suspend fun insert(parsed: ParsedBackup): ImportResult {
        val medicationDao = database.medicationDao()
        val injectionDao = database.injectionDao()

        // Active medications first so a match prefers them over soft-deleted ones.
        val existing = medicationDao.getAll().sortedBy { it.isDeleted }.toMutableList()
        val localIdByBackupId = mutableMapOf<Long, Long>()
        var created = 0

        for (m in parsed.medications) {
            val label = m.dosageLabel()
            val match = existing.firstOrNull {
                it.name.equals(m.name, ignoreCase = true) && it.dosageLabel().equals(label, ignoreCase = true)
            }
            localIdByBackupId[m.backupId] = when {
                match == null -> {
                    val entity = MedicationEntity(
                        name = m.name,
                        dosageAmount = m.dosageAmount,
                        dosageUnit = m.dosageUnit.name,
                        isDeleted = m.isDeleted,
                        createdAt = m.createdAt.toEpochMilli(),
                    )
                    val id = medicationDao.insert(entity)
                    existing.add(entity.copy(id = id))
                    created++
                    id
                }
                // Active in the backup but deleted here: bring it back so it shows in the picker.
                match.isDeleted && !m.isDeleted -> {
                    medicationDao.update(match.copy(isDeleted = false))
                    match.id
                }
                else -> match.id
            }
        }

        var imported = 0
        var duplicates = 0
        val now = Instant.now().toEpochMilli()

        for (row in parsed.injections) {
            val medicationId = row.backupMedicationId?.let { localIdByBackupId.getValue(it) }

            // Minute window: older exports only kept HH:mm, so compare at that precision.
            val minuteStart = row.timestamp.truncatedTo(ChronoUnit.MINUTES).toEpochMilli()
            val isDuplicate = injectionDao.getInRange(minuteStart, minuteStart + MINUTE_MILLIS).any {
                it.siteX == row.site?.x &&
                    it.siteY == row.site?.y &&
                    it.notes == row.notes &&
                    it.medicationId == medicationId
            }
            if (isDuplicate) {
                duplicates++
                continue
            }

            injectionDao.insert(
                InjectionEntity(
                    timestamp = row.timestamp.toEpochMilli(),
                    isNoteOnly = row.isNoteOnly,
                    siteX = row.site?.x,
                    siteY = row.site?.y,
                    medicationId = medicationId,
                    hadReaction = row.hadReaction,
                    reactionNote = row.reactionNote.takeIf { row.hadReaction },
                    notes = row.notes,
                    createdAt = row.createdAt?.toEpochMilli() ?: now,
                    updatedAt = row.updatedAt?.toEpochMilli() ?: now,
                ),
            )
            imported++
        }

        return ImportResult(imported, duplicates, parsed.invalidRows, created, settingsRestored = false)
    }

    private fun BackupMedication.dosageLabel() =
        Medication(name = name, dosageAmount = dosageAmount, dosageUnit = dosageUnit).dosageLabel

    private fun MedicationEntity.dosageLabel() = toDomain().dosageLabel

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
        const val MINUTE_MILLIS = 60_000L
        const val MAX_FILE_BYTES = 10 * 1024 * 1024
    }
}
