package com.dosetrack.export

import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.SitePoint
import com.dosetrack.data.settings.AppSettings
import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import com.dosetrack.export.CsvExporter.SettingKeys
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class BackupMedication(
    /** Id in the file; only used to link injections, never written to the local database. */
    val backupId: Long,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: DosageUnit,
    val isDeleted: Boolean,
    val createdAt: Instant,
)

data class BackupInjection(
    val timestamp: Instant,
    val isNoteOnly: Boolean,
    val site: SitePoint?,
    val backupMedicationId: Long?,
    val hadReaction: Boolean,
    val reactionNote: String?,
    val notes: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)

/** Everything read from a file, normalised to one shape whatever the file's format version. */
data class ParsedBackup(
    val medications: List<BackupMedication>,
    val injections: List<BackupInjection>,
    /** Null when the file carries no settings (format 1). */
    val settings: AppSettings?,
    val invalidRows: Int,
)

class InvalidImportFileException(message: String) : Exception(message)

/** Pure parsing of [CsvExporter] files (format 2 backups and format 1 history exports). */
object CsvImporter {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parse(text: String, zone: ZoneId = ZoneId.systemDefault()): ParsedBackup {
        val records = CsvParser.parse(text)
        if (records.isEmpty()) throw InvalidImportFileException("The file is empty.")

        val first = records.first().map { it.trim() }
        return when {
            first.firstOrNull() == CsvExporter.MAGIC -> {
                val version = first.getOrNull(1)?.toIntOrNull()
                if (version == null || version > CsvExporter.FORMAT_VERSION) {
                    throw InvalidImportFileException(
                        "This backup was made by a newer version of DoseTrack. Update the app and try again.",
                    )
                }
                parseBackup(records.drop(1))
            }
            first.matchesHeader(CsvExporter.LEGACY_HEADER) -> parseLegacy(records.drop(1), zone)
            else -> throw InvalidImportFileException("This doesn't look like a DoseTrack backup file.")
        }
    }

    // region Format 2: sectioned full backup

    private fun parseBackup(records: List<List<String>>): ParsedBackup {
        val sections = splitSections(records)
        val medicationRows = sections[CsvExporter.SECTION_MEDICATIONS]
            ?: throw InvalidImportFileException("The backup is missing its medications section.")
        val injectionRows = sections[CsvExporter.SECTION_INJECTIONS]
            ?: throw InvalidImportFileException("The backup is missing its injections section.")

        var invalid = 0
        val medications = rowsUnder(medicationRows, CsvExporter.MEDICATION_HEADER).mapNotNull { record ->
            parseMedication(record).also { if (it == null) invalid++ }
        }
        val knownIds = medications.map { it.backupId }.toSet()
        val injections = rowsUnder(injectionRows, CsvExporter.INJECTION_HEADER).mapNotNull { record ->
            parseInjection(record, knownIds).also { if (it == null) invalid++ }
        }
        val settings = sections[CsvExporter.SECTION_SETTINGS]
            ?.let { rowsUnder(it, CsvExporter.SETTINGS_HEADER) }
            ?.let(::parseSettings)

        return ParsedBackup(medications, injections, settings, invalid)
    }

    /** Section marker rows are single-cell rows naming the section; every data row has several cells. */
    private fun splitSections(records: List<List<String>>): Map<String, List<List<String>>> {
        val names = setOf(CsvExporter.SECTION_MEDICATIONS, CsvExporter.SECTION_INJECTIONS, CsvExporter.SECTION_SETTINGS)
        val result = mutableMapOf<String, MutableList<List<String>>>()
        var current: MutableList<List<String>>? = null
        records.forEach { record ->
            val marker = record.singleOrNull()?.trim()
            if (marker != null && marker in names) {
                current = mutableListOf<List<String>>().also { result[marker] = it }
            } else {
                current?.add(record)
            }
        }
        return result
    }

    private fun rowsUnder(section: List<List<String>>, header: List<String>): List<List<String>> {
        val actual = section.firstOrNull()?.map { it.trim() }
        if (actual == null || !actual.matchesHeader(header)) {
            throw InvalidImportFileException("The backup file is damaged (unexpected columns).")
        }
        return section.drop(1)
    }

    private fun parseMedication(r: List<String>): BackupMedication? {
        if (r.size != CsvExporter.MEDICATION_HEADER.size) return null
        return BackupMedication(
            backupId = r[0].trim().toLongOrNull() ?: return null,
            name = r[1].trim().ifBlank { return null },
            dosageAmount = r[2].trim().toDoubleOrNull()?.takeIf { it > 0 } ?: return null,
            dosageUnit = enumOrNull<DosageUnit>(r[3]) ?: return null,
            isDeleted = r[4].isYes(),
            createdAt = instantOrNull(r[5]) ?: return null,
        )
    }

    private fun parseInjection(r: List<String>, knownMedicationIds: Set<Long>): BackupInjection? {
        if (r.size != CsvExporter.INJECTION_HEADER.size) return null
        val site = parseSite(r[7], r[8]) ?: return null
        val isNoteOnly = when (r[9].trim()) {
            CsvExporter.TYPE_NOTE -> true
            CsvExporter.TYPE_INJECTION -> false
            else -> return null
        }
        if (!isNoteOnly && site.value == null) return null
        val medicationId = r[10].trim().ifBlank { null }?.let { it.toLongOrNull() ?: return null }
        if (medicationId != null && medicationId !in knownMedicationIds) return null
        return BackupInjection(
            timestamp = instantOrNull(r[11]) ?: return null,
            isNoteOnly = isNoteOnly,
            site = site.value,
            backupMedicationId = medicationId,
            hadReaction = r[5].isYes(),
            reactionNote = r[6].ifBlank { null },
            notes = r[4].ifBlank { null },
            createdAt = instantOrNull(r[12]),
            updatedAt = instantOrNull(r[13]),
        )
    }

    private fun parseSettings(rows: List<List<String>>): AppSettings {
        val values = rows.filter { it.size == 2 }.associate { it[0].trim() to it[1].trim() }
        val defaults = AppSettings()
        val reminderDefaults = defaults.reminderConfig
        return AppSettings(
            siteHistoryCount = values[SettingKeys.SITE_HISTORY_COUNT]?.toIntOrNull()?.takeIf { it >= 0 }
                ?: defaults.siteHistoryCount,
            showInjectionGuide = values[SettingKeys.SHOW_INJECTION_GUIDE]?.toBooleanStrictOrNull()
                ?: defaults.showInjectionGuide,
            reminderConfig = ReminderConfig(
                mode = values[SettingKeys.REMINDER_MODE]?.let { enumOrNull<ReminderMode>(it) } ?: reminderDefaults.mode,
                days = values[SettingKeys.REMINDER_DAYS]
                    ?.split(';')
                    ?.mapNotNull { enumOrNull<DayOfWeek>(it) }
                    ?.toSet()
                    ?: reminderDefaults.days,
                timeOfDayMinutes = values[SettingKeys.REMINDER_TIME_MINUTES]?.toIntOrNull()?.takeIf { it in 0 until 24 * 60 }
                    ?: reminderDefaults.timeOfDayMinutes,
                intervalValue = values[SettingKeys.REMINDER_INTERVAL_VALUE]?.toIntOrNull()?.takeIf { it >= 1 }
                    ?: reminderDefaults.intervalValue,
                intervalUnit = values[SettingKeys.REMINDER_INTERVAL_UNIT]?.let { enumOrNull<IntervalUnit>(it) }
                    ?: reminderDefaults.intervalUnit,
                startAt = values[SettingKeys.REMINDER_START_AT]?.let(::instantOrNull) ?: reminderDefaults.startAt,
            ),
        )
    }

    // endregion

    // region Format 1: history-only export (no medication list, minute precision, no settings)

    private fun parseLegacy(records: List<List<String>>, zone: ZoneId): ParsedBackup {
        val medications = mutableListOf<BackupMedication>()
        val medicationIds = mutableMapOf<Pair<String, String>, Long>()
        var invalid = 0
        val injections = records.mapNotNull { r ->
            parseLegacyRow(r, zone, medications, medicationIds).also { if (it == null) invalid++ }
        }
        return ParsedBackup(medications, injections, settings = null, invalidRows = invalid)
    }

    private fun parseLegacyRow(
        r: List<String>,
        zone: ZoneId,
        medications: MutableList<BackupMedication>,
        medicationIds: MutableMap<Pair<String, String>, Long>,
    ): BackupInjection? {
        if (r.size != CsvExporter.LEGACY_HEADER.size) return null
        val timestamp = try {
            LocalDate.parse(r[0].trim(), dateFormatter)
                .atTime(LocalTime.parse(r[1].trim(), timeFormatter))
                .atZone(zone)
                .toInstant()
        } catch (e: DateTimeParseException) {
            return null
        }
        val site = parseSite(r[7], r[8]) ?: return null

        // Format 1 has no medication list: synthesise one entry per distinct name + dosage.
        val name = r[2].trim()
        val dosageLabel = r[3].trim()
        val medicationId = if (name.isEmpty()) {
            null
        } else {
            val key = name.lowercase() to dosageLabel.lowercase()
            medicationIds[key] ?: run {
                val (amount, unit) = parseDosage(dosageLabel) ?: return null
                val id = medications.size.toLong() + 1
                medications += BackupMedication(id, name, amount, unit, isDeleted = false, createdAt = Instant.now())
                medicationIds[key] = id
                id
            }
        }

        return BackupInjection(
            timestamp = timestamp,
            isNoteOnly = site.value == null,
            site = site.value,
            backupMedicationId = medicationId,
            hadReaction = r[5].isYes(),
            reactionNote = r[6].ifBlank { null },
            notes = r[4].ifBlank { null },
            createdAt = null,
            updatedAt = null,
        )
    }

    /** Parses a dosage label such as "0.5 mL" (see Medication.dosageLabel). */
    fun parseDosage(label: String): Pair<Double, DosageUnit>? {
        val parts = label.trim().split(Regex("\\s+"), limit = 2)
        if (parts.size != 2) return null
        val amount = parts[0].toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val unit = DosageUnit.entries.firstOrNull {
            it.label.equals(parts[1], ignoreCase = true) || it.name.equals(parts[1], ignoreCase = true)
        } ?: return null
        return amount to unit
    }

    // endregion

    /** Wraps a nullable site so "no site" (valid) can be told apart from "unreadable site" (null). */
    private class ParsedSite(val value: SitePoint?)

    private fun parseSite(xText: String, yText: String): ParsedSite? {
        if (xText.isBlank() && yText.isBlank()) return ParsedSite(null)
        val x = xText.trim().toFloatOrNull() ?: return null
        val y = yText.trim().toFloatOrNull() ?: return null
        if (x !in 0f..1f || y !in 0f..1f) return null
        return ParsedSite(SitePoint(x, y))
    }

    private fun List<String>.matchesHeader(header: List<String>) =
        size == header.size && zip(header).all { (a, b) -> a.equals(b, ignoreCase = true) }

    private fun String.isYes() = trim().equals("yes", ignoreCase = true)

    private fun instantOrNull(text: String): Instant? = try {
        Instant.parse(text.trim())
    } catch (e: DateTimeParseException) {
        null
    }

    private inline fun <reified T : Enum<T>> enumOrNull(text: String): T? =
        enumValues<T>().firstOrNull { it.name.equals(text.trim(), ignoreCase = true) }
}

/** Minimal RFC 4180 reader: quoted fields, doubled quotes, embedded commas/newlines, CRLF, UTF-8 BOM. */
object CsvParser {
    fun parse(text: String): List<List<String>> {
        val source = text.removePrefix("﻿")
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < source.length) {
            val c = source[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < source.length && source[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        row.add(field.toString())
                        field.clear()
                    }
                    '\r' -> Unit
                    '\n' -> {
                        row.add(field.toString())
                        field.clear()
                        rows.add(row)
                        row = mutableListOf()
                    }
                    else -> field.append(c)
                }
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows.filterNot { it.size == 1 && it[0].isBlank() }
    }
}
