package com.dosetrack.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.dosetrack.util.toDisplayString
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    value: Instant,
    onValueChange: (Instant) -> Unit,
    modifier: Modifier = Modifier,
    maxInstant: Instant = Instant.now(),
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    val zone = ZoneId.systemDefault()

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value.toDisplayString(),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date & time") },
            trailingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Surface(
            onClick = { showDatePicker = true },
            modifier = Modifier.matchParentSize(),
            color = Color.Transparent,
        ) {}
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val existing = value.atZone(zone)
        val timePickerState = rememberTimePickerState(
            initialHour = existing.hour,
            initialMinute = existing.minute,
            is24Hour = false,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = pendingDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    } ?: existing.toLocalDate()
                    val combined = selectedDate
                        .atTime(timePickerState.hour, timePickerState.minute)
                        .atZone(zone)
                        .toInstant()
                    onValueChange(if (combined.isAfter(maxInstant)) maxInstant else combined)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}
