package com.dosetrack.ui

import com.dosetrack.export.PasswordPolicy
import com.dosetrack.ui.guide.GuideBlock
import com.dosetrack.ui.guide.GuideContent
import com.dosetrack.ui.guide.shouldOpenGuide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideTest {

    @Test
    fun `a brand-new user sees the guide once`() {
        assertTrue(shouldOpenGuide(guideSeen = false, medicationCount = 0, entryCount = 0))
        assertFalse(shouldOpenGuide(guideSeen = true, medicationCount = 0, entryCount = 0))
    }

    @Test
    fun `someone updating with data already in the app is not interrupted`() {
        assertFalse(shouldOpenGuide(guideSeen = false, medicationCount = 2, entryCount = 13))
        assertFalse(shouldOpenGuide(guideSeen = false, medicationCount = 1, entryCount = 0))
        assertFalse(shouldOpenGuide(guideSeen = false, medicationCount = 0, entryCount = 1))
    }

    @Test
    fun `every guide section has a unique title and some content`() {
        val titles = GuideContent.sections.map { it.title }
        assertEquals(titles.distinct(), titles)
        GuideContent.sections.forEach { section ->
            assertTrue(section.title, section.blocks.isNotEmpty())
            section.blocks.forEach { block ->
                val texts = when (block) {
                    is GuideBlock.Paragraph -> listOf(block.text)
                    is GuideBlock.Tip -> listOf(block.text)
                    is GuideBlock.Steps -> block.items
                    is GuideBlock.Bullets -> block.items
                }
                assertTrue(section.title, texts.isNotEmpty() && texts.all { it.isNotBlank() })
            }
        }
    }

    @Test
    fun `the backup section lists the real password rules`() {
        val backups = GuideContent.sections.single { it.title.startsWith("Backups") }
        val bullets = backups.blocks.filterIsInstance<GuideBlock.Bullets>().flatMap { it.items }
        PasswordPolicy.rules.forEach { rule -> assertTrue(rule.description, rule.description in bullets) }
    }
}
