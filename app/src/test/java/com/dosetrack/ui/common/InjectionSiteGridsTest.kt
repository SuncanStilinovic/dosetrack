package com.dosetrack.ui.common

import com.dosetrack.data.model.SitePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InjectionSiteGridsTest {

    private val eps = 1e-5f

    @Test
    fun `grids come in mirrored pairs`() {
        val grids = InjectionSiteGrids.grids
        assertEquals(6, grids.size)
        grids.chunked(2).forEach { (left, right) ->
            assertEquals(1f - left.right, right.left, eps)
            assertEquals(1f - left.left, right.right, eps)
            assertEquals(left.top, right.top, eps)
            assertEquals(left.columns, right.columns)
            assertEquals(left.rows, right.rows)
        }
    }

    @Test
    fun `grids lie inside the diagram and do not overlap`() {
        val grids = InjectionSiteGrids.grids
        grids.forEach { g ->
            assertTrue(g.left in 0f..1f && g.right in 0f..1f && g.top in 0f..1f && g.bottom in 0f..1f)
            assertTrue(g.left < g.right && g.top < g.bottom)
        }
        for (i in grids.indices) for (j in i + 1 until grids.size) {
            val a = grids[i]
            val b = grids[j]
            val overlap = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom
            assertTrue("grids $i and $j overlap", !overlap)
        }
    }

    @Test
    fun `every cell centre snaps to itself`() {
        InjectionSiteGrids.grids.flatMap { it.cells() }.forEach { cell ->
            val snapped = InjectionSiteGrids.snap(cell.center)
            assertEquals(cell.center.x, snapped.x, eps)
            assertEquals(cell.center.y, snapped.y, eps)
        }
    }

    @Test
    fun `a tap anywhere inside a cell snaps to that cell's centre`() {
        val cell = InjectionSiteGrids.grids.first().cells().first()
        val nearCorner = SitePoint(cell.left + 0.001f, cell.top + 0.001f)
        assertEquals(cell.center, InjectionSiteGrids.snap(nearCorner))
    }

    @Test
    fun `taps outside every grid are kept as they are`() {
        val head = SitePoint(0.5f, 0.1f)
        assertNull(InjectionSiteGrids.cellAt(head))
        assertEquals(head, InjectionSiteGrids.snap(head))
    }

    @Test
    fun `cell count matches the design (2x2 belly, 1x3 arms, 2x3 thighs)`() {
        assertEquals(listOf(4, 4, 3, 3, 6, 6), InjectionSiteGrids.grids.map { it.cells().size })
    }

    @Test
    fun `guide points sit on grid centres`() {
        InjectionGuidePoints.points.forEach { assertNotNull(InjectionSiteGrids.cellAt(it)) }
    }
}
