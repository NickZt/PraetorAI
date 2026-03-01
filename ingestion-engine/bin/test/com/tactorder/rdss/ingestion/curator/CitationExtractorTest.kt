package com.tactorder.rdss.ingestion.curator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CitationExtractorTest {

    private val extractor = CitationExtractor()

    @Test
    fun `should extract single numeric citation`() {
        val text = "This is a referenced statement [1]."
        val citations = extractor.extract(text)

        assertEquals(1, citations.size)
        val citation = citations[0]
        assertEquals("[1]", citation.rawText)
        assertEquals(CitationType.NUMERIC, citation.type)
        assertEquals("1", citation.metadata["indices"])
    }

    @Test
    fun `should extract multiple numeric citations`() {
        val text = "Several studies show this [1, 2, 3]."
        val citations = extractor.extract(text)

        assertEquals(1, citations.size)
        val citation = citations[0]
        assertEquals("[1, 2, 3]", citation.rawText)
        assertEquals(CitationType.NUMERIC, citation.type)
        assertEquals("1,2,3", citation.metadata["indices"])
    }

    @Test
    fun `should extract author-year citation`() {
        val text = "As shown by (Smith, 2023), this is true."
        val citations = extractor.extract(text)

        assertEquals(1, citations.size)
        val citation = citations[0]
        assertEquals("(Smith, 2023)", citation.rawText)
        assertEquals(CitationType.AUTHOR_YEAR, citation.type)
        assertEquals("Smith", citation.metadata["author"])
        assertEquals("2023", citation.metadata["year"])
    }

    @Test
    fun `should extract author-year citation with et al`() {
        val text = "Recent work (Doe et al., 2024) confirms this."
        val citations = extractor.extract(text)

        assertEquals(1, citations.size)
        val citation = citations[0]
        assertEquals("(Doe et al., 2024)", citation.rawText)
        assertEquals(CitationType.AUTHOR_YEAR, citation.type)
        assertEquals("Doe et al.", citation.metadata["author"])
        assertEquals("2024", citation.metadata["year"])
    }

    @Test
    fun `should handle mixed citations`() {
        val text = "Some prefer numeric [1], while others use (Jones, 2022)."
        val citations = extractor.extract(text)

        assertEquals(2, citations.size)
        
        val numeric = citations.find { it.type == CitationType.NUMERIC }
        val authorYear = citations.find { it.type == CitationType.AUTHOR_YEAR }

        assertTrue(numeric != null)
        assertTrue(authorYear != null)
        
        assertEquals("[1]", numeric?.rawText)
        assertEquals("(Jones, 2022)", authorYear?.rawText)
    }
    
    @Test
    fun `should return empty list for no citations`() {
        val text = "This is a statement with no citations."
        val citations = extractor.extract(text)
        assertEquals(0, citations.size)
    }
}
