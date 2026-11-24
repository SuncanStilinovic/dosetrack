package com.dosetrack.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.dosetrack.data.model.IntervalUnit
import com.dosetrack.data.model.MedicationReminder
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderEndMode
import com.dosetrack.data.model.ReminderMode
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/** One reminder per medication; enum and day values are stored by name. */
@Entity(
    tableName = "medication_reminders",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class MedicationReminderEntity(
    @PrimaryKey val medicationId: Long,
    val mode: String,
    /** Comma-separated [DayOfWeek] names, e.g. "MONDAY,THURSDAY". */
    val days: String,
    val timeOfDayMinutes: Int,
    val intervalValue: Int,
    val intervalUnit: String,
    val startAt: Long,
    val endMode: String,
    /** Epoch day of the last day a reminder may fire. */
    val endDate: Long?,
    val endAfterDoses: Int?,
    val countFrom: Long,
)

private inline fun <reified T : Enum<T>> enumOr(name: String, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback

fun MedicationReminderEntity.toDomain(): MedicationReminder = MedicationReminder(
    medicationId = medicationId,
    config = ReminderConfig(
        mode = enumOr(mode, ReminderMode.NONE),
        days = days.split(',').mapNotNull { name -> DayOfWeek.entries.firstOrNull { it.name == name } }.toSet(),
        timeOfDayMinutes = timeOfDayMinutes,
        intervalValue = intervalValue,
        intervalUnit = enumOr(intervalUnit, IntervalUnit.DAYS),
        startAt = Instant.ofEpochMilli(startAt),
        endMode = enumOr(endMode, ReminderEndMode.NEVER),
        endDate = endDate?.let(LocalDate::ofEpochDay),
        endAfterDoses = endAfterDoses,
        countFrom = Instant.ofEpochMilli(countFrom),
    ),
)

fun MedicationReminder.toEntity(): MedicationReminderEntity = MedicationReminderEntity(
    medicationId = medicationId,
    mode = config.mode.name,
    days = config.days.sorted().joinToString(",") { it.name },
    timeOfDayMinutes = config.timeOfDayMinutes,
    intervalValue = config.intervalValue,
    intervalUnit = config.intervalUnit.name,
    startAt = config.startAt.toEpochMilli(),
    endMode = config.endMode.name,
    endDate = config.endDate?.toEpochDay(),
    endAfterDoses = config.endAfterDoses,
    countFrom = config.countFrom.toEpochMilli(),
)
