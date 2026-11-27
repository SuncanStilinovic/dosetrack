package com.dosetrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dosetrack.ui.guide.GuideScreen
import com.dosetrack.ui.history.HistoryScreen
import com.dosetrack.ui.home.HomeScreen
import com.dosetrack.ui.logentry.LogEntryScreen
import com.dosetrack.ui.medications.MedicationEditScreen
import com.dosetrack.ui.medications.MedicationListScreen
import com.dosetrack.ui.settings.SettingsScreen

@Composable
fun DoseTrackNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Destination.Home.route,
        modifier = modifier,
    ) {
        composable(Destination.Home.route) {
            HomeScreen(
                onLogDrug = { navController.navigate(Destination.LogEntry.route(noteOnly = false)) },
                onAddNote = { navController.navigate(Destination.LogEntry.route(noteOnly = true)) },
                onAddMedication = { navController.navigate(Destination.MedicationEdit.route()) },
            )
        }
        composable(Destination.History.route) {
            HistoryScreen(
                onEditEntry = { id -> navController.navigate(Destination.LogEntry.route(entryId = id)) },
            )
        }
        composable(Destination.Medications.route) {
            MedicationListScreen(
                onAddMedication = { navController.navigate(Destination.MedicationEdit.route()) },
                onEditMedication = { id -> navController.navigate(Destination.MedicationEdit.route(id)) },
            )
        }
        composable(Destination.Settings.route) {
            SettingsScreen()
        }
        composable(Destination.Guide.route) {
            GuideScreen(onAddMedication = { navController.navigate(Destination.MedicationEdit.route()) })
        }
        composable(
            route = Destination.LogEntry.route,
            arguments = listOf(
                navArgument(Destination.LogEntry.ARG_ENTRY_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(Destination.LogEntry.ARG_NOTE_ONLY) {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument(Destination.LogEntry.ARG_MEDICATION_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val entryId = backStackEntry.arguments?.getLong(Destination.LogEntry.ARG_ENTRY_ID) ?: -1L
            val noteOnly = backStackEntry.arguments?.getBoolean(Destination.LogEntry.ARG_NOTE_ONLY) ?: false
            val medicationId = backStackEntry.arguments?.getLong(Destination.LogEntry.ARG_MEDICATION_ID) ?: -1L
            LogEntryScreen(
                entryId = entryId.takeIf { it != -1L },
                noteOnly = noteOnly,
                preselectedMedicationId = medicationId.takeIf { it != -1L },
                onDone = { navController.popBackStack() },
            )
        }
        composable(
            route = Destination.MedicationEdit.route,
            arguments = listOf(
                navArgument(Destination.MedicationEdit.ARG_MEDICATION_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getLong(Destination.MedicationEdit.ARG_MEDICATION_ID) ?: -1L
            MedicationEditScreen(
                medicationId = medicationId.takeIf { it != -1L },
                onDone = { navController.popBackStack() },
            )
        }
    }
}
