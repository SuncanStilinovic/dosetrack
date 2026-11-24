package com.dosetrack.export

import com.dosetrack.data.DataRules
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.model.Medication
import com.dosetrack.data.model.SitePoint
import com.dosetrack.data.settings.AppSettings
import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** Export -> import of the CSV backup format, plus the older history-only format and hostile input. */
class BackupFormatTest {

    private val zone = ZoneId.of("Europe/Zagreb")
    private val now = Instant.parse("2026-11-21T12:00:00Z")
    private val t0 = Instant.parse("2026-11-20T09:54:37.123Z")

    private fun parse(csv: String) = CsvImporter.parse(csv, zone, now)

    // region Round trip

    @Test
    fun `full backup round-trips every field`() {
        val pen = Medication(7, "Ozempic, pen \"A\"", 0.5, DosageUnit.ML, false, t0)
        val unused = Medication(9, "Unused", 10.0, DosageUnit.MG, false, t0)
        val deleted = Medication(11, "Old", 2.0, DosageUnit.IU, true, t0)
        val injection = Injection(
            id = 1, timestamp = t0, isNoteOnly = false, site = SitePoint(0.4366667f, 0.383f), medicationId = 7,
            hadReaction = true, reactionNote = "red,\nitchy", notes = "line1\r\nline2",
            createdAt = t0, updatedAt = t0.plusSeconds(5),
        )
        val note = Injection(
            id = 2, timestamp = t0.minusSeconds(86_400), isNoteOnly = true, site = null, medicationId = null,
            hadReaction = false, reactionNote = null, notes = "Settings", createdAt = t0, updatedAt = t0,
        )
        val settings = AppSettings(
            siteHistoryCount = 7,
            reminderConfig = ReminderConfig(
                mode = ReminderMode.DAYS_OF_WEEK,
                days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY),
                timeOfDayMinutes = 495,
                intervalValue = 2,
                intervalUnit = IntervalUnit.WEEKS,
                startAt = t0,
            ),
            use24HourClock = true,
        )

        val csv = CsvExporter.buildBackup(
            medications = listOf(pen, unused, deleted),
            entries = listOf(InjectionWithMedication(injection, pen), InjectionWithMedication(note, null)),
            settings = settings,
            zone = zone,
        )
        val parsed = parse(csv)

        assertEquals(0, parsed.invalidRows)
        assertEquals(
            listOf(
                BackupMedication(7, pen.name, 0.5, DosageUnit.ML, false, t0),
                BackupMedication(9, "Unused", 10.0, DosageUnit.MG, false, t0),
                BackupMedication(11, "Old", 2.0, DosageUnit.IU, true, t0),
            ),
            parsed.medications,
        )
        assertEquals(
            listOf(
                BackupInjection(t0, false, injection.site, 7, true, "red,\nitchy", "line1\r\nline2", t0, t0.plusSeconds(5)),
                BackupInjection(note.timestamp, true, null, null, false, null, "Settings", t0, t0),
            ),
            parsed.injections,
        )
        assertEquals(settings, parsed.settings)
    }

    @Test
    fun `human-readable columns use local date and time`() {
        val med = Medication(1, "M", 1.0, DosageUnit.MG, false, t0)
        val inj = Injection(1, t0, false, SitePoint(0.5f, 0.5f), 1, false, null, null, t0, t0)
        val csv = CsvExporter.buildBackup(listOf(med), listOf(InjectionWithMedication(inj, med)), AppSettings(), zone)
        // 09:54 UTC is 10:54 in Zagreb (CET, UTC+1) on 2026-11-20.
        assertTrue(csv.lines().any { it.startsWith("2026-11-20,10:54,M,1 mg,") })
    }

    // endregion

    // region Older history-only export (format 1)

    @Test
    fun `legacy export imports with synthesised medications`() {
        val csv = """
            Date,Time,Medication,Dosage,Notes,Reaction,ReactionNote,SiteX,SiteY
            2026-11-20,10:54,Ozempic,0.5 mL,,Yes,red,0.44,0.41
            2026-11-19,08:00,,,note only,No,,,
            2026-11-18,08:00,ozempic,0.5 ML,,No,,0.56,0.41
        """.trimIndent()
        val parsed = parse(csv)

        assertEquals(0, parsed.invalidRows)
        assertEquals(1, parsed.medications.size) // name/dosage matched case-insensitively
        assertEquals(Instant.parse("2026-11-20T09:54:00Z"), parsed.injections[0].timestamp)
        assertTrue(parsed.injections[1].isNoteOnly)
        assertNull(parsed.injections[1].backupMedicationId)
        assertEquals(parsed.injections[0].backupMedicationId, parsed.injections[2].backupMedicationId)
        assertNull(parsed.settings)
    }

    @Test
    fun `legacy row with unreadable dosage is skipped`() {
        val csv = """
            Date,Time,Medication,Dosage,Notes,Reaction,ReactionNote,SiteX,SiteY
            2026-11-20,10:54,Ozempic,a lot,,No,,0.44,0.41
        """.trimIndent()
        val parsed = parse(csv)
        assertEquals(1, parsed.invalidRows)
        assertTrue(parsed.injections.isEmpty())
    }

    // endregion

    // region Rejected files

    @Test
    fun `unrelated file is rejected`() {
        assertThrows(InvalidImportFileException::class.java) { parse("name,email\nx,y\n") }
    }

    @Test
    fun `empty file is rejected`() {
        assertThrows(InvalidImportFileException::class.java) { parse("") }
    }

    @Test
    fun `backup from a newer format version is rejected`() {
        assertThrows(InvalidImportFileException::class.java) { parse("DoseTrack backup,99\n") }
    }

    @Test
    fun `backup missing a section is rejected`() {
        assertThrows(InvalidImportFileException::class.java) {
            parse("DoseTrack backup,2\nMedications\nId,Name,DosageAmount,DosageUnit,Deleted,CreatedAt\n")
        }
    }

    @Test
    fun `backup with changed columns is rejected`() {
        val csv = """
            DoseTrack backup,2
            Medications
            Id,Name
            Injections
            ${CsvExporter.INJECTION_HEADER.joinToString(",")}
        """.trimIndent()
        assertThrows(InvalidImportFileException::class.java) { parse(csv) }
    }

    // endregion

    // region Per-row validation (DataRules)

    private fun backup(medicationRows: String, injectionRows: String, settingsRows: String = "") = """
DoseTrack backup,2
Medications
${CsvExporter.MEDICATION_HEADER.joinToString(",")}
$medicationRows
Injections
${CsvExporter.INJECTION_HEADER.joinToString(",")}
$injectionRows
Settings
Key,Value
$settingsRows
""".trim()

    private val goodMed = "1,Good,0.5,ML,No,2026-02-01T00:00:00Z"

    private fun injectionRow(
        timestamp: String = "2026-03-01T09:00:00Z",
        notes: String = "",
        siteX: String = "0.5",
        siteY: String = "0.5",
        type: String = "Injection",
        medicationId: String = "1",
    ) = "2026-03-01,10:00,Good,0.5 mL,$notes,No,,$siteX,$siteY,$type,$medicationId,$timestamp,,"

    @Test
    fun `valid rows are accepted`() {
        val parsed = parse(backup(goodMed, injectionRow()))
        assertEquals(0, parsed.invalidRows)
        assertEquals(1, parsed.injections.size)
    }

    @Test
    fun `medications with impossible dosages or bad dates are skipped`() {
        val rows = listOf(
            "2,NaN,NaN,MG,No,2026-02-01T00:00:00Z",
            "3,Inf,Infinity,MG,No,2026-02-01T00:00:00Z",
            "4,Zero,0,MG,No,2026-02-01T00:00:00Z",
            "5,Neg,-1,MG,No,2026-02-01T00:00:00Z",
            "6,Unit,1,LITRES,No,2026-02-01T00:00:00Z",
            "7,Old,1,MG,No,2025-06-01T00:00:00Z",
            "8,${"n".repeat(DataRules.MAX_NAME_LENGTH + 1)},1,MG,No,2026-02-01T00:00:00Z",
        ).joinToString("\n")
        val parsed = parse(backup("$goodMed\n$rows", ""))
        assertEquals(listOf("Good"), parsed.medications.map { it.name })
        assertEquals(7, parsed.invalidRows)
    }

    @Test
    fun `injections before 2026 or in the future are skipped`() {
        val rows = listOf(
            injectionRow(timestamp = "2025-03-01T09:00:00Z"),
            injectionRow(timestamp = "2027-03-01T09:00:00Z"),
            injectionRow(timestamp = "-1000000000-01-01T00:00:00Z"),
            injectionRow(timestamp = "+1000000000-12-31T23:59:59Z"),
        ).joinToString("\n")
        val parsed = parse(backup(goodMed, rows))
        assertTrue(parsed.injections.isEmpty())
        assertEquals(4, parsed.invalidRows)
    }

    @Test
    fun `a few hours of clock difference between phones is tolerated`() {
        val parsed = parse(backup(goodMed, injectionRow(timestamp = "2026-11-21T18:00:00Z")))
        assertEquals(1, parsed.injections.size)
    }

    @Test
    fun `bad sites, types, references and over-long notes are skipped`() {
        val rows = listOf(
            injectionRow(siteX = "NaN"),
            injectionRow(siteX = "1.5"),
            injectionRow(siteX = "0.5", siteY = ""),
            injectionRow(type = "Shot"),
            injectionRow(siteX = "", siteY = "", type = "Injection"), // an injection needs a site
            injectionRow(medicationId = "42"), // not in the medications section
            injectionRow(notes = "x".repeat(DataRules.MAX_NOTE_LENGTH + 1)),
        ).joinToString("\n")
        val parsed = parse(backup(goodMed, rows))
        assertTrue(parsed.injections.isEmpty())
        assertEquals(7, parsed.invalidRows)
    }

    @Test
    fun `note-only rows need no site`() {
        val parsed = parse(backup(goodMed, injectionRow(siteX = "", siteY = "", type = "Note", medicationId = "")))
        assertEquals(1, parsed.injections.size)
        assertTrue(parsed.injections.single().isNoteOnly)
    }

    @Test
    fun `out-of-range settings fall back to defaults`() {
        val settings = """
            site_history_count,2000000000
            reminder_interval_value,0
            reminder_time_minutes,9999
            reminder_mode,BOGUS
            reminder_start_at,-1000000000-01-01T00:00:00Z
        """.trimIndent()
        val parsed = parse(backup(goodMed, "", settings))
        val defaults = AppSettings()
        val restored = parsed.settings!!
        assertEquals(defaults.siteHistoryCount, restored.siteHistoryCount)
        assertEquals(defaults.reminderConfig.intervalValue, restored.reminderConfig.intervalValue)
        assertEquals(defaults.reminderConfig.timeOfDayMinutes, restored.reminderConfig.timeOfDayMinutes)
        assertEquals(defaults.reminderConfig.mode, restored.reminderConfig.mode)
        assertTrue(DataRules.isValidReminderStart(restored.reminderConfig.startAt))
    }

    @Test
    fun `clock format is left unset when the backup has none`() {
        val parsed = parse(backup(goodMed, "", "site_history_count,3"))
        assertEquals(null, parsed.settings!!.use24HourClock)
    }

    @Test
    fun `unset clock format is not exported`() {
        val csv = CsvExporter.buildBackup(emptyList(), emptyList(), AppSettings(), zone)
        assertTrue(CsvExporter.SettingKeys.USE_24_HOUR_CLOCK !in csv)
    }

    @Test
    fun `reminder start may be in the future`() {
        val parsed = parse(backup(goodMed, "", "reminder_start_at,2027-06-01T08:00:00Z"))
        assertEquals(Instant.parse("2027-06-01T08:00:00Z"), parsed.settings!!.reminderConfig.startAt)
    }

    // endregion

    @Test
    fun `parseDosage reads labels in any case`() {
        assertEquals(0.5 to DosageUnit.ML, CsvImporter.parseDosage("0.5 mL"))
        assertEquals(10.0 to DosageUnit.UNITS, CsvImporter.parseDosage("10  UNITS"))
        assertNull(CsvImporter.parseDosage("0.5"))
        assertNull(CsvImporter.parseDosage("half mL"))
    }
}
