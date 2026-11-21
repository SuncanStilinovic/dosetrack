package com.dosetrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destination(val route: String) {
    data object Home : Destination("home")
    data object History : Destination("history")
    data object Medications : Destination("medications")
    data object Settings : Destination("settings")

    data object LogEntry : Destination("log_entry?entryId={entryId}&noteOnly={noteOnly}") {
        const val ARG_ENTRY_ID = "entryId"
        const val ARG_NOTE_ONLY = "noteOnly"

        fun route(entryId: Long? = null, noteOnly: Boolean = false): String =
            "log_entry?entryId=${entryId ?: -1}&noteOnly=$noteOnly"
    }

    data object MedicationEdit : Destination("medication_edit?medicationId={medicationId}") {
        const val ARG_MEDICATION_ID = "medicationId"

        fun route(medicationId: Long? = null): String =
            "medication_edit?medicationId=${medicationId ?: -1}"
    }
}

data class TopLevelTab(
    val destination: Destination,
    val label: String,
    val icon: ImageVector,
)

val topLevelTabs = listOf(
    TopLevelTab(Destination.Home, "Home", Icons.Filled.Home),
    TopLevelTab(Destination.History, "History", Icons.Filled.History),
    TopLevelTab(Destination.Medications, "Medications", Icons.Filled.Medication),
    TopLevelTab(Destination.Settings, "Settings", Icons.Filled.Settings),
)
