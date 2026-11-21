package com.dosetrack.export

import com.dosetrack.data.model.InjectionWithMedication
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object CsvExporter {

    private val header = listOf("Date", "Time", "Medication", "Dosage", "Notes", "Reaction", "ReactionNote", "SiteX", "SiteY")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    fun buildCsv(entries: List<InjectionWithMedication>): String {
        val builder = StringBuilder()
        builder.append(header.joinToString(",") { escape(it) }).append('\n')
        entries.forEach { entry ->
            val injection = entry.injection
            val row = listOf(
                dateFormatter.format(injection.timestamp),
                timeFormatter.format(injection.timestamp),
                entry.medication?.name ?: "",
                entry.medication?.dosageLabel ?: "",
                injection.notes ?: "",
                if (injection.hadReaction) "Yes" else "No",
                injection.reactionNote ?: "",
                injection.site?.x?.toString() ?: "",
                injection.site?.y?.toString() ?: "",
            )
            builder.append(row.joinToString(",") { escape(it) }).append('\n')
        }
        return builder.toString()
    }

    private fun escape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
