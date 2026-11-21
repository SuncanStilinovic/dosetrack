package com.dosetrack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimaryLight = Color(0xFF1565C0)
private val PrimaryDark = Color(0xFF90CAF9)

private val LightColors = lightColorScheme(
    primary = PrimaryLight,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
)

@Composable
fun DoseTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
