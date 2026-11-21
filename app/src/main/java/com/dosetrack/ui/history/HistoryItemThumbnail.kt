package com.dosetrack.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dosetrack.data.model.InjectionWithMedication
import com.dosetrack.ui.common.BodyDiagram
import androidx.compose.ui.res.painterResource
import com.dosetrack.R

@Composable
fun HistoryItemThumbnail(
    entry: InjectionWithMedication,
    modifier: Modifier = Modifier,
) {
    if (entry.injection.isNoteOnly) {
        Box(modifier = modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_edit_note), contentDescription = "Note")
        }
    } else {
        BodyDiagram(
            currentSite = entry.injection.site,
            isInteractive = false,
            showSideLabels = false,
            modifier = modifier.width(56.dp),
        )
    }
}
