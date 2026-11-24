package com.dosetrack.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dosetrack.data.model.IntervalUnit
import com.dosetrack.data.model.ReminderConfig
import com.dosetrack.data.model.ReminderEndMode
import com.dosetrack.data.model.ReminderMode
import com.dosetrack.util.formatTimeOfDay
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val allDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

/**
 * Schedule and end of one medication's reminder.
 *
 * @param dosesLogged doses counted towards an "after N doses" end, or null for a new medication.
 * @param hasEnded true when the saved reminder has reached its end and won't fire again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderConfigSection(
    config: ReminderConfig,
    onConfigChange: (ReminderConfig) -> Unit,
    permissions: ReminderPermissions,
    modifier: Modifier = Modifier,
    dosesLogged: Int? = null,
    hasEnded: Boolean = false,
) {
    val hasNotificationPermission = permissions.hasNotificationPermission
    val hasExactAlarmPermission = permissions.hasExactAlarmPermission
    val onRequestNotificationPermission = permissions.requestNotificationPermission
    val onRequestExactAlarmPermission = permissions.requestExactAlarmPermission

    Column(modifier = modifier.fillMaxWidth()) {
        Text("Reminder", style = MaterialTheme.typography.titleMedium)

        ReminderModeRow(
            label = "Off",
            selected = config.mode == ReminderMode.NONE,
            onClick = { onConfigChange(config.copy(mode = ReminderMode.NONE)) },
        )
        ReminderModeRow(
            label = "Specific days of week",
            selected = config.mode == ReminderMode.DAYS_OF_WEEK,
            onClick = { onConfigChange(config.copy(mode = ReminderMode.DAYS_OF_WEEK)) },
        )
        ReminderModeRow(
            label = "Every X hours/days/weeks/months",
            selected = config.mode == ReminderMode.INTERVAL,
            onClick = { onConfigChange(config.copy(mode = ReminderMode.INTERVAL)) },
        )

        if (config.mode != ReminderMode.NONE && (!hasNotificationPermission || !hasExactAlarmPermission)) {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Reminders need extra permissions to fire reliably.")
                    if (!hasNotificationPermission) {
                        Button(onClick = onRequestNotificationPermission, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Allow notifications")
                        }
                    }
                    if (!hasExactAlarmPermission) {
                        Button(onClick = onRequestExactAlarmPermission, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Allow exact alarms")
                        }
                    }
                }
            }
        }

        when (config.mode) {
            ReminderMode.DAYS_OF_WEEK -> DaysOfWeekFields(config, onConfigChange)
            ReminderMode.INTERVAL -> IntervalFields(config, onConfigChange)
            ReminderMode.NONE -> Unit
        }

        if (config.isOn) {
            EndFields(config, onConfigChange, dosesLogged)
            if (hasEnded) {
                Text(
                    text = "This reminder has ended. Change when it ends to turn it back on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndFields(config: ReminderConfig, onConfigChange: (ReminderConfig) -> Unit, dosesLogged: Int?) {
    var showDatePicker by remember { mutableStateOf(false) }
    // Local text so the field can be cleared while typing; the config only gets a number.
    var dosesText by remember(config.endAfterDoses == null) { mutableStateOf(config.endAfterDoses?.toString() ?: "") }

    Text("Ends", modifier = Modifier.padding(top = 16.dp))
    ReminderModeRow(
        label = "Never",
        selected = config.endMode == ReminderEndMode.NEVER,
        onClick = { onConfigChange(config.copy(endMode = ReminderEndMode.NEVER)) },
    )
    ReminderModeRow(
        label = "On a date",
        selected = config.endMode == ReminderEndMode.ON_DATE,
        onClick = { onConfigChange(config.copy(endMode = ReminderEndMode.ON_DATE)) },
    )
    if (config.endMode == ReminderEndMode.ON_DATE) {
        Box(modifier = Modifier.fillMaxWidth().padding(start = 48.dp)) {
            OutlinedTextField(
                value = config.endDate?.format(endDateFormatter) ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Last reminder on") },
                placeholder = { Text("Choose a date") },
                isError = config.endDate == null,
                modifier = Modifier.fillMaxWidth(),
            )
            Surface(
                onClick = { showDatePicker = true },
                modifier = Modifier.matchParentSize(),
                color = Color.Transparent,
            ) {}
        }
    }
    ReminderModeRow(
        label = "After a number of doses",
        selected = config.endMode == ReminderEndMode.AFTER_DOSES,
        onClick = { onConfigChange(config.copy(endMode = ReminderEndMode.AFTER_DOSES)) },
    )
    if (config.endMode == ReminderEndMode.AFTER_DOSES) {
        val outOfRange = config.endAfterDoses == null || config.endAfterDoses !in ReminderConfig.END_DOSES_RANGE
        OutlinedTextField(
            value = dosesText,
            onValueChange = { text ->
                val digits = text.filter(Char::isDigit).take(4)
                dosesText = digits
                onConfigChange(config.copy(endAfterDoses = digits.toIntOrNull()))
            },
            label = { Text("Doses") },
            isError = outOfRange,
            supportingText = {
                Text(
                    when {
                        outOfRange -> "Enter 1 to ${ReminderConfig.END_DOSES_RANGE.last}"
                        dosesLogged != null -> "$dosesLogged of ${config.endAfterDoses} logged so far"
                        else -> "Counts doses of this medication you log from now on"
                    },
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(start = 48.dp),
        )
    }

    if (showDatePicker) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val lastDay = today.plusYears(10)
        val selectable = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !date.isBefore(today) && !date.isAfter(lastDay)
                }

                override fun isSelectableYear(year: Int): Boolean = year in today.year..lastDay.year
            }
        }
        // DatePicker works in UTC-midnight millis of the calendar date.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = (config.endDate ?: today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            yearRange = today.year..lastDay.year,
            selectableDates = selectable,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onConfigChange(config.copy(endDate = date))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

private val endDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
private fun ReminderModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    // The whole row is the touch target, not just the radio circle.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null, modifier = Modifier.minimumInteractiveComponentSize())
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysOfWeekFields(config: ReminderConfig, onConfigChange: (ReminderConfig) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    val hour = config.timeOfDayMinutes / 60
    val minute = config.timeOfDayMinutes % 60
    val use24Hour = LocalUse24HourClock.current

    LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        items(allDays) { day ->
            val selected = day in config.days
            FilterChip(
                selected = selected,
                onClick = {
                    val newDays = if (selected) config.days - day else config.days + day
                    onConfigChange(config.copy(days = newDays))
                },
                label = { Text(day.name.take(3)) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }

    Button(onClick = { showTimePicker = true }, modifier = Modifier.padding(top = 8.dp)) {
        Text("Time: ${formatTimeOfDay(config.timeOfDayMinutes, use24Hour)}")
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = use24Hour)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(onClick = {
                    onConfigChange(config.copy(timeOfDayMinutes = timePickerState.hour * 60 + timePickerState.minute))
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalFields(config: ReminderConfig, onConfigChange: (ReminderConfig) -> Unit) {
    var unitMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = config.intervalValue.toString(),
            onValueChange = { value ->
                value.toIntOrNull()
                    ?.takeIf { it in ReminderConfig.INTERVAL_RANGE }
                    ?.let { onConfigChange(config.copy(intervalValue = it)) }
            },
            label = { Text("Every") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.padding(4.dp))

        ExposedDropdownMenuBox(
            expanded = unitMenuExpanded,
            onExpandedChange = { unitMenuExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = config.intervalUnit.name.lowercase(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(
                expanded = unitMenuExpanded,
                onDismissRequest = { unitMenuExpanded = false },
            ) {
                IntervalUnit.entries.forEach { unit ->
                    DropdownMenuItem(
                        text = { Text(unit.name.lowercase()) },
                        onClick = {
                            onConfigChange(config.copy(intervalUnit = unit))
                            unitMenuExpanded = false
                        },
                    )
                }
            }
        }
    }

    DateTimePickerField(
        value = config.startAt,
        onValueChange = { onConfigChange(config.copy(startAt = it)) },
        maxInstant = Instant.now().plus(Duration.ofDays(3650)),
        modifier = Modifier.padding(top = 8.dp),
    )
}
