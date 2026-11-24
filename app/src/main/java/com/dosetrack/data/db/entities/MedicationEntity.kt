package com.dosetrack.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.dosetrack.data.model.AdministrationRoute
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.Medication
import java.time.Instant

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: String,
    /** [AdministrationRoute] name; medications from database version 1 are injections. */
    @ColumnInfo(defaultValue = "INJECTION") val route: String = AdministrationRoute.INJECTION.name,
    val isDeleted: Boolean = false,
    val createdAt: Long,
)

fun MedicationEntity.toDomain(): Medication = Medication(
    id = id,
    name = name,
    dosageAmount = dosageAmount,
    dosageUnit = DosageUnit.valueOf(dosageUnit),
    route = AdministrationRoute.entries.firstOrNull { it.name == route } ?: AdministrationRoute.INJECTION,
    isDeleted = isDeleted,
    createdAt = Instant.ofEpochMilli(createdAt),
)

fun Medication.toEntity(): MedicationEntity = MedicationEntity(
    id = id,
    name = name,
    dosageAmount = dosageAmount,
    dosageUnit = dosageUnit.name,
    route = route.name,
    isDeleted = isDeleted,
    createdAt = createdAt.toEpochMilli(),
)
