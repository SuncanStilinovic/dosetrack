package com.dosetrack.export

import com.dosetrack.data.db.dao.InjectionDao
import com.dosetrack.data.db.dao.MedicationDao
import com.dosetrack.data.db.dao.MedicationReminderDao
import com.dosetrack.data.db.entities.InjectionEntity
import com.dosetrack.data.db.entities.MedicationEntity
import com.dosetrack.data.db.entities.toDomain
import com.dosetrack.data.db.entities.toEntity
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.MedicationReminder
import java.time.Instant
import java.time.temporal.ChronoUnit

data class ImportResult(
    val imported: Int,
    val duplicates: Int,
    val invalid: Int,
    val medicationsCreated: Int,
    val settingsRestored: Boolean,
    val remindersRestored: Int = 0,
)

/**
 * Writes a parsed backup's medications and history into the database. Free of Android APIs, so it
 * can be unit-tested with in-memory DAOs; the caller runs it inside a database transaction.
 *
 * Medications are matched to existing ones by name + dosage (ignoring case); history rows already
 * present (same minute, site, notes and medication) are skipped, so restoring twice is harmless.
 */
class BackupRestorer(
    private val medicationDao: MedicationDao,
    private val injectionDao: InjectionDao,
    /** Null skips restoring reminders (tests that only cover history). */
    private val reminderDao: MedicationReminderDao? = null,
    private val clock: () -> Instant = Instant::now,
) {

    suspend fun restore(parsed: ParsedBackup): ImportResult {
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
                        route = m.route.name,
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
                    val restored = match.copy(isDeleted = false)
                    medicationDao.update(restored)
                    existing[existing.indexOf(match)] = restored
                    match.id
                }
                else -> match.id
            }
        }

        var imported = 0
        var duplicates = 0
        val now = clock().toEpochMilli()

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

        // A backup's reminder replaces the one on the matching local medication, like settings do.
        var remindersRestored = 0
        if (reminderDao != null) {
            for (reminder in parsed.reminders) {
                val medicationId = localIdByBackupId[reminder.backupMedicationId] ?: continue
                reminderDao.upsert(MedicationReminder(medicationId, reminder.config).toEntity())
                remindersRestored++
            }
        }

        return ImportResult(imported, duplicates, parsed.invalidRows, created, settingsRestored = false, remindersRestored)
    }

    private fun BackupMedication.dosageLabel() =
        Medication(name = name, dosageAmount = dosageAmount, dosageUnit = dosageUnit).dosageLabel

    private fun MedicationEntity.dosageLabel() = toDomain().dosageLabel

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}
