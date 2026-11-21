package com.dosetrack.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import com.dosetrack.data.model.SitePoint

/** Draws historical -> guide -> current markers, in that order, over a same-sized parent. */
@Composable
fun MarkerOverlay(
    currentSite: SitePoint?,
    historicalSites: List<SitePoint>,
    guidePoints: List<SitePoint>,
    showGuide: Boolean,
    modifier: Modifier = Modifier,
) {
    val currentColor = MaterialTheme.colorScheme.primary
    val historicalColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
    val guideColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        historicalSites.forEach { site ->
            drawCircle(
                color = historicalColor,
                radius = size.minDimension * 0.02f,
                center = androidx.compose.ui.geometry.Offset(site.x * size.width, site.y * size.height),
                style = Fill,
            )
        }
        if (showGuide) {
            guidePoints.forEach { site ->
                drawCircle(
                    color = guideColor,
                    radius = size.minDimension * 0.022f,
                    center = androidx.compose.ui.geometry.Offset(site.x * size.width, site.y * size.height),
                    style = Fill,
                )
            }
        }
        currentSite?.let { site ->
            drawCircle(
                color = currentColor,
                radius = size.minDimension * 0.028f,
                center = androidx.compose.ui.geometry.Offset(site.x * size.width, site.y * size.height),
                style = Fill,
            )
            drawCircle(
                color = Color.White,
                radius = size.minDimension * 0.012f,
                center = androidx.compose.ui.geometry.Offset(site.x * size.width, site.y * size.height),
                style = Fill,
            )
        }
    }
}
