package com.dosetrack.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordPolicyTest {

    @Test
    fun `accepts a password meeting every rule`() {
        assertTrue(PasswordPolicy.isAcceptable("Correct-Horse7"))
        assertTrue(PasswordPolicy.isAcceptable("Šifra_Duga2026")) // non-ASCII letters count as letters
        assertTrue(PasswordPolicy.isAcceptable("AAAAAAAAAAA1!")) // lowercase is not required
    }

    @Test
    fun `rejects each missing rule`() {
        assertFalse("11 chars", PasswordPolicy.isAcceptable("Correct-Ho7"))
        assertFalse("no capital", PasswordPolicy.isAcceptable("correct-horse7"))
        assertFalse("no digit", PasswordPolicy.isAcceptable("Correct-Horse"))
        assertFalse("no special", PasswordPolicy.isAcceptable("CorrectHorse7"))
        assertFalse("space is not special", PasswordPolicy.isAcceptable("Correct Horse7"))
        assertFalse("empty", PasswordPolicy.isAcceptable(""))
    }

    @Test
    fun `exactly twelve characters is enough`() {
        assertTrue(PasswordPolicy.isAcceptable("Abcdefghij1!"))
    }

    @Test
    fun `checklist reports each rule separately`() {
        val met = PasswordPolicy.rules.map { it.isMet("abc") }
        assertEquals(listOf(false, false, false, false), met)
        val metAll = PasswordPolicy.rules.map { it.isMet("Correct-Horse7") }
        assertEquals(listOf(true, true, true, true), metAll)
    }

    @Test
    fun `symbols and emoji count as special characters`() {
        listOf("!", "#", "@", "-", "_", "€", "💉").forEach { special ->
            assertTrue(special, PasswordPolicy.isAcceptable("Correcthorse7$special"))
        }
    }
}
