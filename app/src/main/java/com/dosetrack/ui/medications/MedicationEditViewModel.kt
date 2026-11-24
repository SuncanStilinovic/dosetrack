package com.dosetrack.ui.medications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dosetrack.data.DataRules
import com.dosetrack.data.model.AdministrationRoute
import com.dosetrack.data.model.DosageUnit
import com.dosetrack.data.model.Medication
import com.dosetrack.data.repository.MedicationRepository
import kotlinx.coroutines.launch

data class MedicationEditUiState(
    val name: String = "",
    val dosageAmount: String = "",
    val dosageUnit: DosageUnit = DosageUnit.MG,
    val route: AdministrationRoute = AdministrationRoute.INJECTION,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && parsedAmount != null

    /** Only positive, finite amounts: "NaN", "Infinity", 0 or negatives couldn't be restored from a backup. */
    val parsedAmount: Double?
        get() = dosageAmount.trim().toDoubleOrNull()?.takeIf(DataRules::isValidDosageAmount)

    val showAmountError: Boolean
        get() = dosageAmount.isNotBlank() && parsedAmount == null
}

class MedicationEditViewModel(
    private val repository: MedicationRepository,
    private val medicationId: Long?,
) : ViewModel() {

    var uiState by mutableStateOf(MedicationEditUiState())
        private set

    init {
        medicationId?.let { id ->
            viewModelScope.launch {
                repository.getById(id)?.let { medication ->
                    uiState = uiState.copy(
                        name = medication.name,
                        dosageAmount = formatAmount(medication.dosageAmount),
                        dosageUnit = medication.dosageUnit,
                        route = medication.route,
                    )
                }
            }
        }
    }

    fun updateName(name: String) {
        uiState = uiState.copy(name = name.take(DataRules.MAX_NAME_LENGTH))
    }

    fun updateDosageAmount(amount: String) {
        uiState = uiState.copy(dosageAmount = amount)
    }

    fun updateDosageUnit(unit: DosageUnit) {
        uiState = uiState.copy(dosageUnit = unit)
    }

    fun updateRoute(route: AdministrationRoute) {
        uiState = uiState.copy(route = route)
    }

    fun save(onDone: () -> Unit) {
        val amount = uiState.parsedAmount ?: return
        if (uiState.name.isBlank()) return
        viewModelScope.launch {
            repository.save(
                Medication(
                    id = medicationId ?: 0,
                    name = uiState.name.trim(),
                    dosageAmount = amount,
                    dosageUnit = uiState.dosageUnit,
                    route = uiState.route,
                ),
            )
            onDone()
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
}
