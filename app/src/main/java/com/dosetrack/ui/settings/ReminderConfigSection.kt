package com.dosetrack.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dosetrack.data.settings.IntervalUnit
import com.dosetrack.data.settings.ReminderConfig
import com.dosetrack.data.settings.ReminderMode
import com.dosetrack.ui.common.DateTimePickerField
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant

private val allDays = listOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderConfigSection(
    config: ReminderConfig,
    onConfigChange: (ReminderConfig) -> Unit,
    hasNotificationPermission: Boolean,
    hasExactAlarmPermission: Boolean,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarmPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Reminders")

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
    }
}

@Composable
private fun ReminderModeRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysOfWeekFields(config: ReminderConfig, onConfigChange: (ReminderConfig) -> Unit) {
    var showTimePicker by remember { mutableStateOf(false) }
    val hour = config.timeOfDayMinutes / 60
    val minute = config.timeOfDayMinutes % 60

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
        Text("Time: %02d:%02d".format(hour, minute))
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = false)
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
