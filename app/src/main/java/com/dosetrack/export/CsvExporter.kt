package com.dosetrack.export

import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.MedicationReminder
import com.dosetrack.data.settings.AppSettings
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Writes a full DoseTrack backup as one CSV file with four sections, so it still opens in a
 * spreadsheet but can be restored losslessly on another device by [CsvImporter]:
 *
 * ```
 * DoseTrack backup,4
 * Medications
 * Id,Name,DosageAmount,DosageUnit,Deleted,CreatedAt,Route
 * ...
 * Injections
 * Date,Time,Medication,Dosage,Notes,Reaction,ReactionNote,SiteX,SiteY,Type,MedicationId,Timestamp,CreatedAt,UpdatedAt
 * ...
 * Reminders
 * MedicationId,Mode,Days,Time,IntervalValue,IntervalUnit,StartAt,EndMode,EndDate,EndAfterDoses,CountFrom
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
    /** 3 added the medication Route column, 4 the Reminders section; 2 and 3 are still accepted on import. */
    const val FORMAT_VERSION = 4

    const val SECTION_MEDICATIONS = "Medications"
    const val SECTION_INJECTIONS = "Injections"
    const val SECTION_REMINDERS = "Reminders"
    const val SECTION_SETTINGS = "Settings"

    /** Header of the original history-only export (format 1); still accepted on import. */
    val LEGACY_HEADER = listOf("Date", "Time", "Medication", "Dosage", "Notes", "Reaction", "ReactionNote", "SiteX", "SiteY")
    /** Medications header of format 2, before medications had a route; still accepted on import. */
    val MEDICATION_HEADER_V2 = listOf("Id", "Name", "DosageAmount", "DosageUnit", "Deleted", "CreatedAt")
    val MEDICATION_HEADER = MEDICATION_HEADER_V2 + "Route"
    val INJECTION_HEADER = LEGACY_HEADER + listOf("Type", "MedicationId", "Timestamp", "CreatedAt", "UpdatedAt")
    val REMINDER_HEADER = listOf(
        "MedicationId", "Mode", "Days", "Time", "IntervalValue", "IntervalUnit", "StartAt",
        "EndMode", "EndDate", "EndAfterDoses", "CountFrom",
    )
    val SETTINGS_HEADER = listOf("Key", "Value")

    const val TYPE_INJECTION = "Injection"
    const val TYPE_NOTE = "Note"

    /** Formats 2 and 3 also had app-wide reminder_* keys; those are ignored on import. */
    object SettingKeys {
        const val SITE_HISTORY_COUNT = "site_history_count"
        const val USE_24_HOUR_CLOCK = "use_24_hour_clock"
    }

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun buildBackup(
        medications: List<Medication>,
        entries: List<InjectionWithMedication>,
        settings: AppSettings,
        zone: ZoneId = ZoneId.systemDefault(),
        reminders: List<MedicationReminder> = emptyList(),
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
                    m.route.name,
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
        row(listOf(SECTION_REMINDERS))
        row(REMINDER_HEADER)
        reminders.forEach { r ->
            val c = r.config
            row(
                listOf(
                    r.medicationId.toString(),
                    c.mode.name,
                    c.days.sorted().joinToString(";") { it.name },
                    LocalTime.of(c.timeOfDayMinutes / 60, c.timeOfDayMinutes % 60).format(timeFormatter),
                    c.intervalValue.toString(),
                    c.intervalUnit.name,
                    c.startAt.toString(),
                    c.endMode.name,
                    c.endDate?.toString().orEmpty(),
                    c.endAfterDoses?.toString().orEmpty(),
                    c.countFrom.toString(),
                ),
            )
        }

        appendLine()
        row(listOf(SECTION_SETTINGS))
        row(SETTINGS_HEADER)
        listOf(
            SettingKeys.SITE_HISTORY_COUNT to settings.siteHistoryCount.toString(),
            // Left out while unset, so a restore keeps following the target device's clock.
            settings.use24HourClock?.let { SettingKeys.USE_24_HOUR_CLOCK to it.toString() },
        ).filterNotNull().forEach { (key, value) -> row(listOf(key, value)) }
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
