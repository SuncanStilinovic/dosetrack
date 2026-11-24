package com.dosetrack.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dosetrack.ui.common.ProvideClockFormat
import com.dosetrack.ui.navigation.Destination
import com.dosetrack.ui.navigation.DoseTrackNavHost
import com.dosetrack.ui.navigation.topLevelTabs
import com.dosetrack.ui.theme.DoseTrackTheme

class MainActivity : ComponentActivity() {

    /** Medication whose reminder notification opened the app; the log screen is shown for it once. */
    private var pendingLogMedicationId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // On recreation (rotation, process restore) the screen was already opened; don't open it again.
        if (savedInstanceState == null) consumeLogIntent(intent)
        enableEdgeToEdge()
        setContent {
            DoseTrackTheme {
                ProvideClockFormat {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        val navController = rememberNavController()
                        LaunchedEffect(pendingLogMedicationId) {
                            pendingLogMedicationId?.let { id ->
                                navController.navigate(Destination.LogEntry.route(medicationId = id))
                                pendingLogMedicationId = null
                            }
                        }
                        val backStackEntry by navController.currentBackStackEntryAsState()
                        val currentDestination = backStackEntry?.destination

                        Scaffold(
                            bottomBar = {
                                val isTopLevel = topLevelTabs.any { tab ->
                                    currentDestination?.hierarchy?.any { it.route == tab.destination.route } == true
                                }
                                if (isTopLevel) {
                                    NavigationBar {
                                        topLevelTabs.forEach { tab ->
                                            val selected = currentDestination?.hierarchy?.any {
                                                it.route == tab.destination.route
                                            } == true
                                            NavigationBarItem(
                                                selected = selected,
                                                onClick = {
                                                    navController.navigate(tab.destination.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                },
                                                icon = { Icon(painterResource(tab.iconRes), contentDescription = tab.label) },
                                                label = { Text(tab.label) },
                                            )
                                        }
                                    }
                                }
                            },
                        ) { padding ->
                            DoseTrackNavHost(
                                navController = navController,
                                modifier = Modifier.fillMaxSize().padding(padding),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeLogIntent(intent)
    }

    private fun consumeLogIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_LOG_MEDICATION_ID, -1L) ?: -1L
        if (id >= 0) {
            pendingLogMedicationId = id
            intent?.removeExtra(EXTRA_LOG_MEDICATION_ID)
        }
    }

    companion object {
        /** Set by a reminder notification: opens the log screen with this medication selected. */
        const val EXTRA_LOG_MEDICATION_ID = "com.dosetrack.extra.LOG_MEDICATION_ID"
    }
}
