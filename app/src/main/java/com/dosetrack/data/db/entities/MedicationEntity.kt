package com.dosetrack.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.Medication
import java.time.Instant

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: String,
    val isDeleted: Boolean = false,
    val createdAt: Long,
)

fun MedicationEntity.toDomain(): Medication = Medication(
    id = id,
    name = name,
    dosageAmount = dosageAmount,
    dosageUnit = DosageUnit.valueOf(dosageUnit),
    isDeleted = isDeleted,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Medication.toEntity(): MedicationEntity = MedicationEntity(
    id = id,
    name = name,
    dosageAmount = dosageAmount,
    dosageUnit = dosageUnit.name,
    isDeleted = isDeleted,
    createdAt = createdAt.toEpochMilli(),
)
