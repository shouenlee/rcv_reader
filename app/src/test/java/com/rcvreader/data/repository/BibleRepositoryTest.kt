package com.rcvreader.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class BibleRepositoryTest {

    @Test
    fun `sanitizeQuery strips tokens shorter than 2 chars`() {
        assertEquals(listOf("grace", "truth"), sanitizeQuery("a grace b truth"))
    }

    @Test
    fun `sanitizeQuery strips FTS5 special characters`() {
        assertEquals(listOf("grace", "truth"), sanitizeQuery("grace* (truth)"))
    }

    @Test
    fun `sanitizeQuery strips double quotes`() {
        assertEquals(listOf("grace"), sanitizeQuery("\"grace\""))
    }

    @Test
    fun `sanitizeQuery returns empty list for blank input`() {
        assertEquals(emptyList<String>(), sanitizeQuery("   "))
    }

    @Test
    fun `sanitizeQuery returns empty list when all tokens are too short`() {
        assertEquals(emptyList<String>(), sanitizeQuery("a b"))
    }

    @Test
    fun `sanitizeQuery handles single valid word`() {
        assertEquals(listOf("grace"), sanitizeQuery("grace"))
    }

    @Test
    fun `sanitizeQuery lowercases nothing - preserves case`() {
        assertEquals(listOf("Grace"), sanitizeQuery("Grace"))
    }

    @Test
    fun `sanitizeQuery strips colon plus caret and braces`() {
        assertEquals(listOf("textgrace", "test"), sanitizeQuery("text:grace ^{test}"))
    }
}
