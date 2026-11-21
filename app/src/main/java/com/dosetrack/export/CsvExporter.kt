package com.dosetrack.export

import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.model.Medication
import com.dosetrack.data.settings.AppSettings
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Writes a full DoseTrack backup as one CSV file with three sections, so it still opens in a
 * spreadsheet but can be restored losslessly on another device by [CsvImporter]:
 *
 * ```
 * DoseTrack backup,2
 * Medications
 * Id,Name,DosageAmount,DosageUnit,Deleted,CreatedAt
 * ...
 * Injections
 * Date,Time,Medication,Dosage,Notes,Reaction,ReactionNote,SiteX,SiteY,Type,MedicationId,Timestamp,CreatedAt,UpdatedAt
 * ...
 * Settings
 * Key,Value
 * ...
 * ```
 *
 * The first nine injection columns are the human-readable ones (identical to the older
 * history-only export); the rest carry exact values for restoring. Instants are ISO-8601 UTC.
 */
object CsvExporter {

    const val MAGIC = "DoseTrack backup"
    const val FORMAT_VERSION = 2

    const val SECTION_MEDICATIONS = "Medications"
    const val SECTION_INJECTIONS = "Injections"
    const val SECTION_SETTINGS = "Settings"

    /** Header of the original history-only export (format 1); still accepted on import. */
    val LEGACY_HEADER = listOf("Date", "Time", "Medication", "Dosage", "Notes", "Reaction", "ReactionNote", "SiteX", "SiteY")
    val MEDICATION_HEADER = listOf("Id", "Name", "DosageAmount", "DosageUnit", "Deleted", "CreatedAt")
    val INJECTION_HEADER = LEGACY_HEADER + listOf("Type", "MedicationId", "Timestamp", "CreatedAt", "UpdatedAt")
    val SETTINGS_HEADER = listOf("Key", "Value")

    const val TYPE_INJECTION = "Injection"
    const val TYPE_NOTE = "Note"

    object SettingKeys {
        const val SITE_HISTORY_COUNT = "site_history_count"
        const val SHOW_INJECTION_GUIDE = "show_injection_guide"
        const val REMINDER_MODE = "reminder_mode"
        const val REMINDER_DAYS = "reminder_days"
        const val REMINDER_TIME_MINUTES = "reminder_time_minutes"
        const val REMINDER_INTERVAL_VALUE = "reminder_interval_value"
        const val REMINDER_INTERVAL_UNIT = "reminder_interval_unit"
        const val REMINDER_START_AT = "reminder_start_at"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun buildBackup(
        medications: List<Medication>,
        entries: List<InjectionWithMedication>,
        settings: AppSettings,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = buildString {
        row(listOf(MAGIC, FORMAT_VERSION.toString()))

        appendLine()
        row(listOf(SECTION_MEDICATIONS))
        row(MEDICATION_HEADER)
        medications.forEach { m ->
            row(
                listOf(
                    m.id.toString(),
                    m.name,
                    m.dosageAmount.toString(),
                    m.dosageUnit.name,
                    yesNo(m.isDeleted),
                    m.createdAt.toString(),
                ),
            )
        }

        appendLine()
        row(listOf(SECTION_INJECTIONS))
        row(INJECTION_HEADER)
        entries.forEach { entry ->
            val injection = entry.injection
            val local = injection.timestamp.atZone(zone)
            row(
                listOf(
                    dateFormatter.format(local),
                    timeFormatter.format(local),
                    entry.medication?.name.orEmpty(),
                    entry.medication?.dosageLabel.orEmpty(),
                    injection.notes.orEmpty(),
                    yesNo(injection.hadReaction),
                    injection.reactionNote.orEmpty(),
                    injection.site?.x?.toString().orEmpty(),
                    injection.site?.y?.toString().orEmpty(),
                    if (injection.isNoteOnly) TYPE_NOTE else TYPE_INJECTION,
                    injection.medicationId?.toString().orEmpty(),
                    injection.timestamp.toString(),
                    injection.createdAt.toString(),
                    injection.updatedAt.toString(),
                ),
            )
        }

        appendLine()
        row(listOf(SECTION_SETTINGS))
        row(SETTINGS_HEADER)
        val reminder = settings.reminderConfig
        listOf(
            SettingKeys.SITE_HISTORY_COUNT to settings.siteHistoryCount.toString(),
            SettingKeys.SHOW_INJECTION_GUIDE to settings.showInjectionGuide.toString(),
            SettingKeys.REMINDER_MODE to reminder.mode.name,
            SettingKeys.REMINDER_DAYS to reminder.days.sorted().joinToString(";") { it.name },
            SettingKeys.REMINDER_TIME_MINUTES to reminder.timeOfDayMinutes.toString(),
            SettingKeys.REMINDER_INTERVAL_VALUE to reminder.intervalValue.toString(),
            SettingKeys.REMINDER_INTERVAL_UNIT to reminder.intervalUnit.name,
            SettingKeys.REMINDER_START_AT to reminder.startAt.toString(),
        ).forEach { (key, value) -> row(listOf(key, value)) }
    }

    private fun yesNo(value: Boolean) = if (value) "Yes" else "No"

    private fun StringBuilder.row(values: List<String>) {
        append(values.joinToString(",") { escape(it) }).append('\n')
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
}
