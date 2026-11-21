package com.dosetrack.ui.medications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosetrack.data.model.Medication
import com.dosetrack.di.appContainer
import com.dosetrack.ui.common.ConfirmDeleteDialog
import androidx.compose.ui.res.painterResource
import com.dosetrack.R

@Composable
fun MedicationListScreen(
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer()
    val viewModel: MedicationsViewModel = viewModel(factory = MedicationsViewModelFactory(container))
    val medications by viewModel.medications.collectAsState()
    var pendingDeleteId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedication) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Add medication")
            }
        },
    ) { padding ->
        if (medications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No medications yet. Tap + to add one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 80.dp,
                ),
            ) {
                items(medications, key = { it.id }) { medication ->
                    MedicationRow(
                        medication = medication,
                        onClick = { onEditMedication(medication.id) },
                        onDelete = { pendingDeleteId = medication.id },
                    )
                }
            }
        }
    }

    pendingDeleteId?.let { id ->
        ConfirmDeleteDialog(
            title = "Delete medication?",
            message = "Past injections that used it will keep showing its name.",
            onConfirm = {
                viewModel.deleteMedication(id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}

@Composable
private fun MedicationRow(
    medication: Medication,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(medication.name)
                Text(medication.dosageLabel)
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete ${medication.name}")
            }
        }
    }
}
