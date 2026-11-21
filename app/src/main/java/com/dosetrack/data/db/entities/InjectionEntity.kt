package com.dosetrack.data.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.dosetrack.data.model.Injection
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.data.model.SitePoint
import java.time.Instant

@Entity(
    tableName = "injections",
    foreignKeys = [
        ForeignKey(
            entity = MedicationEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("medicationId")],
)
data class InjectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val isNoteOnly: Boolean,
    val siteX: Float?,
    val siteY: Float?,
    val medicationId: Long?,
    val hadReaction: Boolean,
    val reactionNote: String?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class InjectionWithMedicationEntity(
    @Embedded val injection: InjectionEntity,
    @Relation(parentColumn = "medicationId", entityColumn = "id")
    val medication: MedicationEntity?,
)

/** Projection used for the site-rotation overlay; only the columns it needs. */
data class SiteCoordinates(
    val siteX: Float,
    val siteY: Float,
)

fun InjectionEntity.toDomain(): Injection = Injection(
    id = id,
    timestamp = Instant.ofEpochMilli(timestamp),
    isNoteOnly = isNoteOnly,
    site = if (siteX != null && siteY != null) SitePoint(siteX, siteY) else null,
    medicationId = medicationId,
    hadReaction = hadReaction,
    reactionNote = reactionNote,
    notes = notes,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Injection.toEntity(): InjectionEntity = InjectionEntity(
    id = id,
    timestamp = timestamp.toEpochMilli(),
    isNoteOnly = isNoteOnly,
    siteX = site?.x,
    siteY = site?.y,
    medicationId = medicationId,
    hadReaction = hadReaction,
    reactionNote = reactionNote,
    notes = notes,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)

fun InjectionWithMedicationEntity.toDomain(): InjectionWithMedication = InjectionWithMedication(
    injection = injection.toDomain(),
    medication = medication?.toDomain(),
)
