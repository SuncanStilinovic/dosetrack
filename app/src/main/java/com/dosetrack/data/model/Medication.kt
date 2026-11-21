package com.dosetrack.data.model

import java.time.Instant

data class Medication(
    val id: Long = 0,
    val name: String,
    val dosageAmount: Double,
    val dosageUnit: DosageUnit,
    val isDeleted: Boolean = false,
    val createdAt: Instant = Instant.now(),
) {
    val dosageLabel: String
        get() {
            val amountText = if (dosageAmount == dosageAmount.toLong().toDouble()) {
                dosageAmount.toLong().toString()
            } else {
                dosageAmount.toString()
            }
            return "$amountText ${dosageUnit.label}"
        }
}
