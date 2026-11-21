package com.dosetrack.ui.navigation

import androidx.annotation.DrawableRes
import com.dosetrack.R

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
    @DrawableRes val iconRes: Int,
)

val topLevelTabs = listOf(
    TopLevelTab(Destination.Home, "Home", R.drawable.ic_home),
    TopLevelTab(Destination.History, "History", R.drawable.ic_history),
    TopLevelTab(Destination.Medications, "Medications", R.drawable.ic_medication),
    TopLevelTab(Destination.Settings, "Settings", R.drawable.ic_settings),
)
