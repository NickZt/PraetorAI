package com.tactorder.rdss.agent

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class CitationExtractorTest {

    private lateinit var citationExtractor: CitationExtractor

    @BeforeEach
    fun setUp() {
        citationExtractor = CitationExtractor()
    }

    @Test
    fun `should extract APA style citations`() {
        val text =
            "Recent studies (Smith, 2023) have shown that machine learning models can be optimized for edge deployment. " +
                    "This is supported by Johnson et al. (2022, p. 45) who demonstrated significant improvements."

        val citations = citationExtractor.extractCitations(text)

        assertEquals(2, citations.size)

        val smithCitation = citations.find { it.text.contains("Smith, 2023") }
        assertNotNull(smithCitation)
        assertEquals(CitationExtractor.CitationType.APA, smithCitation?.type)
        assertEquals(listOf("Smith"), smithCitation?.authors)
        assertEquals(2023, smithCitation?.year)

        val johnsonCitation = citations.find { it.text.contains("Johnson et al.") }
        assertNotNull(johnsonCitation)
        assertEquals(CitationExtractor.CitationType.APA, johnsonCitation?.type)
        assertEquals(listOf("Johnson", "et al."), johnsonCitation?.authors)
        assertEquals(2022, johnsonCitation?.year)
    }

    @Test
    fun `should extract IEEE style citations`() {
        val text =
            "Several approaches have been proposed [1], [2, 3]. The most comprehensive study [4] suggests that " +
                    "distributed computing offers significant advantages [5, 6, 7]."

        val citations = citationExtractor.extractCitations(text)

        assertTrue(citations.isNotEmpty())

        val ieeeCitations = citations.filter { it.type == CitationExtractor.CitationType.IEEE }
        assertTrue(ieeeCitations.isNotEmpty())

        assertTrue(ieeeCitations.any { it.text == "[1]" })
        assertTrue(ieeeCitations.any { it.text == "[2, 3]" })
        assertTrue(ieeeCitations.any { it.text == "[5, 6, 7]" })
    }

    @Test
    fun `should extract author-year citations`() {
        val text = "Brown (2021) first proposed this approach. Later, Miller and Davis (2022) extended it to " +
                "include real-time processing capabilities."

        val citations = citationExtractor.extractCitations(text)

        assertEquals(2, citations.size)

        val brownCitation = citations.find { it.text.contains("Brown (2021)") }
        assertNotNull(brownCitation)
        assertEquals(CitationExtractor.CitationType.AUTHOR_YEAR, brownCitation?.type)
        assertEquals(listOf("Brown"), brownCitation?.authors)
        assertEquals(2021, brownCitation?.year)

        val millerDavisCitation = citations.find { it.text.contains("Miller and Davis (2022)") }
        assertNotNull(millerDavisCitation)
        assertEquals(CitationExtractor.CitationType.AUTHOR_YEAR, millerDavisCitation?.type)
        assertEquals(listOf("Miller", "Davis"), millerDavisCitation?.authors)
        assertEquals(2022, millerDavisCitation?.year)
    }

    @Test
    fun `should extract DOI citations`() {
        val text = "The complete dataset and code are available at doi:10.1109/ACCESS.2023.1234567. " +
                "Additional information can be found at https://doi.org/10.1145/3583131.3590462."

        val citations = citationExtractor.extractCitations(text)

        assertEquals(2, citations.size)

        val doiCitations = citations.filter { it.type == CitationExtractor.CitationType.DOI }
        assertEquals(2, doiCitations.size)

        assertTrue(doiCitations.any { it.doi == "10.1109/ACCESS.2023.1234567" })
        assertTrue(doiCitations.any { it.doi == "10.1145/3583131.3590462" })
    }

    @Test
    fun `should extract URL citations`() {
        val text = "The project website is available at https://example.com/project. " +
                "Additional resources can be found at https://github.com/user/repo."

        val citations = citationExtractor.extractCitations(text)

        assertEquals(2, citations.size)

        val urlCitations = citations.filter { it.type == CitationExtractor.CitationType.URL }
        assertEquals(2, urlCitations.size)

        assertTrue(urlCitations.any { it.url == "https://example.com/project" })
        assertTrue(urlCitations.any { it.url == "https://github.com/user/repo" })
    }

    @Test
    fun `should extract context for citations`() {
        val text = "Recent advances in edge computing (Smith, 2023) have enabled new applications in " +
                "military AI systems. These developments are particularly important for real-time " +
                "decision making scenarios."

        val citations = citationExtractor.extractCitations(text)

        val smithCitation = citations.find { it.text.contains("Smith, 2023") }
        assertNotNull(smithCitation)
        assertTrue(smithCitation?.context?.contains("edge computing") == true)
        assertTrue(smithCitation?.context?.contains("military AI") == true)
    }

    @Test
    fun `should normalize citation text`() {
        val citation = CitationExtractor.Citation(
            text = "(Smith, J., & Johnson, K., 2023)",
            type = CitationExtractor.CitationType.APA,
            authors = listOf("Smith, J.", "Johnson, K."),
            year = 2023
        )

        val normalized = citationExtractor.normalizeCitation(citation)

        assertEquals("smith j johnson k 2023", normalized)
    }

    @Test
    fun `should handle mixed citation formats`() {
        val text = "As discussed by Smith (2023) and later confirmed by Johnson et al. (2022, p. 45), " +
                "the approach shows promise [1]. The complete study is available at doi:10.1109/2023.1234567."

        val citations = citationExtractor.extractCitations(text)

        assertEquals(4, citations.size)

        val types = citations.map { it.type }.toSet()
        assertTrue(types.contains(CitationExtractor.CitationType.AUTHOR_YEAR))
        assertTrue(types.contains(CitationExtractor.CitationType.APA))
        assertTrue(types.contains(CitationExtractor.CitationType.IEEE))
        assertTrue(types.contains(CitationExtractor.CitationType.DOI))
    }

    @Test
    fun `should handle empty text`() {
        val citations = citationExtractor.extractCitations("")

        assertTrue(citations.isEmpty())
    }

    @Test
    fun `should handle text without citations`() {
        val text = "This is a simple paragraph without any academic citations or references."

        val citations = citationExtractor.extractCitations(text)

        assertTrue(citations.isEmpty())
    }
}
