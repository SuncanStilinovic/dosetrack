package com.dosetrack.ui.logentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosetrack.data.model.Medication
import com.dosetrack.di.appContainer
import com.dosetrack.ui.common.BodyDiagram
import com.dosetrack.ui.common.DateTimePickerField
import com.dosetrack.ui.common.ReactionToggleSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryScreen(
    entryId: Long?,
    noteOnly: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer()
    val viewModel: LogEntryViewModel = viewModel(
        factory = LogEntryViewModelFactory(container, entryId, noteOnly),
    )
    val uiState = viewModel.uiState

    val title = when {
        entryId != null -> "Edit Entry"
        uiState.isNoteOnly -> "Add Note"
        else -> "Log Injection"
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(title) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (!uiState.isNoteOnly) {
                BodyDiagram(
                    currentSite = uiState.site,
                    historicalSites = uiState.historicalSites,
                    showGuide = true,
                    onSiteSelected = viewModel::updateSite,
                )
                Text(
                    text = "Tap a square to choose the injection site",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            DateTimePickerField(
                value = uiState.timestamp,
                onValueChange = viewModel::updateTimestamp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (!uiState.isNoteOnly) {
                MedicationDropdown(
                    medications = uiState.medications,
                    selectedId = uiState.medicationId,
                    onSelect = viewModel::updateMedicationId,
                )
                Spacer(modifier = Modifier.height(8.dp))

                ReactionToggleSection(
                    hadReaction = uiState.hadReaction,
                    reactionNote = uiState.reactionNote,
                    onHadReactionChange = viewModel::updateHadReaction,
                    onReactionNoteChange = viewModel::updateReactionNote,
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::updateNotes,
                label = { Text("Notes") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.save(onDone) },
                enabled = uiState.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicationDropdown(
    medications: List<Medication>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = medications.firstOrNull { it.id == selectedId }
        ?.let { "${it.name} (${it.dosageLabel})" }
        ?: "None"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("Medication") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            medications.forEach { medication ->
                DropdownMenuItem(
                    text = { Text("${medication.name} (${medication.dosageLabel})") },
                    onClick = {
                        onSelect(medication.id)
                        expanded = false
                    },
                )
            }
        }
    }
}
