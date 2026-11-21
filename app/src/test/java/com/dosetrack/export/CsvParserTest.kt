package com.dosetrack.export

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun `plain rows`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvParser.parse("a,b\n1,2\n"))
    }

    @Test
    fun `last row without trailing newline`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvParser.parse("a,b\n1,2"))
    }

    @Test
    fun `quoted fields keep commas, quotes and newlines`() {
        val csv = "\"x, y\",\"say \"\"hi\"\"\",\"line1\nline2\"\n"
        assertEquals(listOf(listOf("x, y", "say \"hi\"", "line1\nline2")), CsvParser.parse(csv))
    }

    @Test
    fun `CRLF line endings and a UTF-8 BOM are handled`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvParser.parse("﻿a,b\r\n1,2\r\n"))
    }

    @Test
    fun `CR inside quotes is preserved`() {
        assertEquals(listOf(listOf("a\r\nb")), CsvParser.parse("\"a\r\nb\"\n"))
    }

    @Test
    fun `empty fields are kept`() {
        assertEquals(listOf(listOf("", "", "")), CsvParser.parse(",,\n"))
    }

    @Test
    fun `blank lines are skipped`() {
        assertEquals(listOf(listOf("a"), listOf("b", "c")), CsvParser.parse("a\n\n\nb,c\n"))
    }

    @Test
    fun `empty input gives no rows`() {
        assertEquals(emptyList<List<String>>(), CsvParser.parse(""))
    }
}
