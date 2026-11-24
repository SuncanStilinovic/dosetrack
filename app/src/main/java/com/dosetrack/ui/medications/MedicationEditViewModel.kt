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
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderEndMode
import com.dosetrack.data.model.ReminderMode
import com.dosetrack.data.repository.MedicationRepository
import com.dosetrack.data.repository.ReminderRepository
import com.dosetrack.reminders.ReminderCoordinator
import com.dosetrack.reminders.ReminderTimeCalculator
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class MedicationEditUiState(
    val name: String = "",
    val dosageAmount: String = "",
    val dosageUnit: DosageUnit = DosageUnit.MG,
    val route: AdministrationRoute = AdministrationRoute.INJECTION,
    val reminder: ReminderConfig = ReminderConfig(),
    /** The reminder as stored when the screen opened; null for a new medication or one without a reminder. */
    val savedReminder: ReminderConfig? = null,
    /** Doses counted towards the saved reminder's end. */
    val savedDosesLogged: Int = 0,
    val today: LocalDate = LocalDate.now(),
) {
    val isValid: Boolean
        get() = name.isNotBlank() && parsedAmount != null && isReminderValid

    /** Only positive, finite amounts: "NaN", "Infinity", 0 or negatives couldn't be restored from a backup. */
    val parsedAmount: Double?
        get() = dosageAmount.trim().toDoubleOrNull()?.takeIf(DataRules::isValidDosageAmount)

    val showAmountError: Boolean
        get() = dosageAmount.isNotBlank() && parsedAmount == null

    val isReminderValid: Boolean
        get() = !reminder.isOn || (
            reminder.hasValidEnd &&
                (reminder.mode != ReminderMode.DAYS_OF_WEEK || reminder.days.isNotEmpty()) &&
                // A past end date can't be picked, but an existing one may be kept while editing other fields.
                (
                    reminder.endMode != ReminderEndMode.ON_DATE ||
                        reminder.endDate == savedReminder?.endDate ||
                        reminder.endDate?.isBefore(today) == false
                    )
            )

    /** The dose count only carries on while the end rule is unchanged; otherwise it restarts on save. */
    private val keepsDoseCount: Boolean
        get() = savedReminder != null && savedReminder.isOn && savedReminder.endRule == reminder.endRule

    val dosesLoggedForDisplay: Int?
        get() = if (keepsDoseCount) savedDosesLogged else null

    val hasEnded: Boolean
        get() = keepsDoseCount && savedReminder != null &&
            ReminderTimeCalculator.hasEnded(savedReminder, savedDosesLogged)

    /** The reminder to store, with the dose count restarted if the end rule changed or it was just turned on. */
    fun reminderToSave(now: Instant): ReminderConfig =
        if (reminder.isOn && !keepsDoseCount) reminder.copy(countFrom = now) else reminder
}

class MedicationEditViewModel(
    private val repository: MedicationRepository,
    private val reminderRepository: ReminderRepository,
    private val reminderCoordinator: ReminderCoordinator,
    private val medicationId: Long?,
) : ViewModel() {

    var uiState by mutableStateOf(MedicationEditUiState())
        private set

    /** Kept so saving an edit doesn't reset fields the screen doesn't show, such as createdAt. */
    private var loaded: Medication? = null

    init {
        medicationId?.let { id ->
            viewModelScope.launch {
                repository.getById(id)?.let { medication ->
                    loaded = medication
                    val reminder = reminderRepository.get(id)
                    uiState = uiState.copy(
                        name = medication.name,
                        dosageAmount = formatAmount(medication.dosageAmount),
                        dosageUnit = medication.dosageUnit,
                        route = medication.route,
                        reminder = reminder ?: uiState.reminder,
                        savedReminder = reminder,
                        savedDosesLogged = reminder?.let { reminderRepository.dosesLogged(id, it) } ?: 0,
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

    fun updateReminder(config: ReminderConfig) {
        uiState = uiState.copy(reminder = config)
    }

    fun save(onDone: () -> Unit) {
        val amount = uiState.parsedAmount ?: return
        if (!uiState.isValid) return
        viewModelScope.launch {
            val base = loaded ?: Medication(name = "", dosageAmount = amount, dosageUnit = uiState.dosageUnit)
            val id = repository.save(
                base.copy(
                    id = medicationId ?: 0,
                    name = uiState.name.trim(),
                    dosageAmount = amount,
                    dosageUnit = uiState.dosageUnit,
                    route = uiState.route,
                ),
            )
            reminderRepository.save(id, uiState.reminderToSave(Instant.now()))
            reminderCoordinator.reschedule(id)
            onDone()
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) amount.toLong().toString() else amount.toString()
}
