package com.dosetrack.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReactionToggleSection(
    hadReaction: Boolean,
    reactionNote: String,
    onHadReactionChange: (Boolean) -> Unit,
    onReactionNoteChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Had a reaction?", modifier = Modifier.weight(1f))
        Switch(checked = hadReaction, onCheckedChange = onHadReactionChange)
    }
    if (hadReaction) {
        OutlinedTextField(
            value = reactionNote,
            onValueChange = onReactionNoteChange,
            label = { Text("Reaction note") },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
}
