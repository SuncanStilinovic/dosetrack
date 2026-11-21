package com.dosetrack.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.di.appContainer
import com.dosetrack.ui.common.ConfirmDeleteDialog
import com.dosetrack.util.toDisplayString

@Composable
fun HistoryScreen(
    onEditEntry: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalContext.current.appContainer()
    val viewModel: HistoryViewModel = viewModel(factory = HistoryViewModelFactory(container))
    val entries by viewModel.entries.collectAsState()
    var pendingDelete by remember { mutableStateOf<InjectionWithMedication?>(null) }

    Scaffold(modifier = modifier) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("No entries yet.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    end = 16.dp,
                    bottom = padding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                items(entries, key = { it.injection.id }) { entry ->
                    HistoryRow(
                        entry = entry,
                        onClick = { onEditEntry(entry.injection.id) },
                        onDelete = { pendingDelete = entry },
                    )
                }
            }
        }
    }

    pendingDelete?.let { entry ->
        ConfirmDeleteDialog(
            title = "Delete entry?",
            message = "This can't be undone.",
            onConfirm = {
                viewModel.delete(entry.injection)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun HistoryRow(
    entry: InjectionWithMedication,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryItemThumbnail(entry = entry)

            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(entry.injection.timestamp.toDisplayString())
                entry.medication?.let { medication ->
                    Text("${medication.name} (${medication.dosageLabel})")
                }
                if (entry.injection.hadReaction) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = "Had a reaction",
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(entry.injection.reactionNote ?: "Reaction")
                    }
                }
                entry.injection.notes?.let { notes ->
                    Text(notes, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}
