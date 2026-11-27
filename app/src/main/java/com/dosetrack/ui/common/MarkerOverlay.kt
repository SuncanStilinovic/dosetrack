package com.dosetrack.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.dosetrack.data.model.SitePoint

/** Matches the outline colour baked into body_diagram_front.xml (the figure is always light). */
private val GridLineColor = Color(0xFF37474F)

/**
 * Draws, over a same-sized parent: filled grid cells (historical, then current), the grid lines,
 * then dot markers for guide points and for any site that falls outside the grids.
 */
@Composable
fun MarkerOverlay(
    currentSite: SitePoint?,
    historicalSites: List<SitePoint>,
    guidePoints: List<SitePoint>,
    showGuide: Boolean,
    modifier: Modifier = Modifier,
    grids: List<SiteGrid> = InjectionSiteGrids.grids,
) {
    val currentColor = MaterialTheme.colorScheme.primary
    // Same blue as the current site (and the History thumbnails), lighter so the new pick stands out.
    val historicalColor = currentColor.copy(alpha = 0.45f)
    val guideColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)

    Canvas(modifier = modifier) {
        fun SitePoint.toOffset() = Offset(x * size.width, y * size.height)
        fun cellFor(site: SitePoint): GridCell? = grids.firstNotNullOfOrNull { it.cellAt(site) }

        // Cell fills first so the grid lines stay crisp on top of them.
        historicalSites.forEach { site -> cellFor(site)?.let { fillCell(it, historicalColor) } }
        currentSite?.let { site -> cellFor(site)?.let { fillCell(it, currentColor) } }

        val lineWidth = (size.minDimension * 0.0045f).coerceAtLeast(1f)
        grids.forEach { grid ->
            grid.cells().forEach { cell ->
                drawRect(
                    color = GridLineColor,
                    topLeft = Offset(cell.left * size.width, cell.top * size.height),
                    size = Size((cell.right - cell.left) * size.width, (cell.bottom - cell.top) * size.height),
                    style = Stroke(width = lineWidth),
                )
            }
        }

        // Sites outside every grid keep the original dot markers.
        historicalSites.filter { cellFor(it) == null }.forEach { site ->
            drawCircle(historicalColor, radius = size.minDimension * 0.02f, center = site.toOffset(), style = Fill)
        }
        if (showGuide) {
            guidePoints.forEach { site ->
                drawCircle(guideColor, radius = size.minDimension * 0.022f, center = site.toOffset(), style = Fill)
            }
        }
        currentSite?.takeIf { cellFor(it) == null }?.let { site ->
            drawCircle(currentColor, radius = size.minDimension * 0.028f, center = site.toOffset(), style = Fill)
            drawCircle(Color.White, radius = size.minDimension * 0.012f, center = site.toOffset(), style = Fill)
        }
    }
}

private fun DrawScope.fillCell(cell: GridCell, color: Color) {
    drawRect(
        color = color,
        topLeft = Offset(cell.left * size.width, cell.top * size.height),
        size = Size((cell.right - cell.left) * size.width, (cell.bottom - cell.top) * size.height),
    )
}
