package com.dosetrack.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onLogDrug: () -> Unit,
    onAddNote: () -> Unit,
    onAddMedication: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text("DoseTrack")
        Button(onClick = onLogDrug) {
            Text("Log Drug")
        }
        OutlinedButton(onClick = onAddNote) {
            Text("Add Note")
        }
        OutlinedButton(onClick = onAddMedication) {
            Text("Add New Medication")
        }
    }
}
