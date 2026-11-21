package com.dosetrack.data.model

import java.time.Instant

data class Injection(
    val id: Long = 0,
    val timestamp: Instant,
    val isNoteOnly: Boolean,
    val site: SitePoint?,
    val medicationId: Long?,
    val hadReaction: Boolean,
    val reactionNote: String?,
    val notes: String?,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

data class InjectionWithMedication(
    val injection: Injection,
    val medication: Medication?,
)
