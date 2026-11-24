package com.dosetrack.ui.common

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.dosetrack.di.appContainer
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Whether times are shown and picked on a 24-hour clock (true) or a 12-hour clock (false). */
val LocalUse24HourClock = compositionLocalOf { false }

/** Provides [LocalUse24HourClock] from settings, following the device's clock until the user picks one. */
@Composable
fun ProvideClockFormat(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val choiceFlow = remember(context) {
        context.appContainer().settingsRepository.settingsFlow
            .map { it.use24HourClock }
            .distinctUntilChanged()
    }
    val choice by choiceFlow.collectAsState(initial = null)
    val use24Hour = choice ?: DateFormat.is24HourFormat(context)
    CompositionLocalProvider(LocalUse24HourClock provides use24Hour, content = content)
}
