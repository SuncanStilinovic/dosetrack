package com.dosetrack.ui.common

import com.dosetrack.data.model.SitePoint

/**
 * A rectangular grid of injection cells drawn over the body diagram.
 * Bounds are normalised (0f..1f) to the 300x600 viewport of body_diagram_front.xml.
 */
data class SiteGrid(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val columns: Int,
    val rows: Int,
) {
    val cellWidth: Float get() = (right - left) / columns
    val cellHeight: Float get() = (bottom - top) / rows
    val center: SitePoint get() = SitePoint((left + right) / 2f, (top + bottom) / 2f)

    fun cells(): List<GridCell> = (0 until rows).flatMap { row ->
        (0 until columns).map { col ->
            val l = left + col * cellWidth
            val t = top + row * cellHeight
            GridCell(l, t, l + cellWidth, t + cellHeight)
        }
    }

    fun cellAt(point: SitePoint): GridCell? {
        if (point.x < left || point.x > right || point.y < top || point.y > bottom) return null
        val col = ((point.x - left) / cellWidth).toInt().coerceIn(0, columns - 1)
        val row = ((point.y - top) / cellHeight).toInt().coerceIn(0, rows - 1)
        val l = left + col * cellWidth
        val t = top + row * cellHeight
        return GridCell(l, t, l + cellWidth, t + cellHeight)
    }
}

data class GridCell(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val center: SitePoint get() = SitePoint((left + right) / 2f, (top + bottom) / 2f)
}

/** Fixed, bundled injection-site grids; not user data. */
object InjectionSiteGrids {

    private const val VIEWPORT_W = 300f
    private const val VIEWPORT_H = 600f

    /** Builds a grid from viewport units for the image-left side and its mirror on the image-right side. */
    private fun mirroredPair(left: Float, top: Float, right: Float, bottom: Float, columns: Int, rows: Int) = listOf(
        SiteGrid(left / VIEWPORT_W, top / VIEWPORT_H, right / VIEWPORT_W, bottom / VIEWPORT_H, columns, rows),
        SiteGrid(
            (VIEWPORT_W - right) / VIEWPORT_W, top / VIEWPORT_H,
            (VIEWPORT_W - left) / VIEWPORT_W, bottom / VIEWPORT_H,
            columns, rows,
        ),
    )

    val grids: List<SiteGrid> =
        mirroredPair(118f, 230f, 144f, 262f, columns = 2, rows = 2) + // abdomen, beside the navel
            mirroredPair(82f, 164f, 95f, 200f, columns = 1, rows = 3) + // upper arms
            mirroredPair(106f, 314f, 134f, 356f, columns = 2, rows = 3) // front thighs

    /** The grid cell containing [point], if any. */
    fun cellAt(point: SitePoint): GridCell? = grids.firstNotNullOfOrNull { it.cellAt(point) }

    /** Injections can only be placed in a grid cell: a tap inside one snaps to its centre, anywhere else gives null. */
    fun snapToCell(point: SitePoint): SitePoint? = cellAt(point)?.center

    fun isOnGrid(point: SitePoint): Boolean = cellAt(point) != null
}
