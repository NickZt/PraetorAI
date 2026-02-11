package com.tactorder.rdss.agent

import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class CitationExtractor {
    
    companion object {
        // Common citation patterns
        private val APA_PATTERN = Pattern.compile(
            "\\(([A-Z][a-z]+(?:, [A-Z][a-z]+)*,\\s*\\d{4}(?:,\\s*p\\.?\\s*\\d+)?|[A-Z][a-z]+\\s*et\\s*al\\.?,\\s*\\d{4})\\)"
        )
        
        private val IEEE_PATTERN = Pattern.compile(
            "\\[(\\d+)\\]|\\[\\d+(?:,\\s*\\d+)*\\]"
        )
        
        private val AUTHOR_YEAR_PATTERN = Pattern.compile(
            "([A-Z][a-z]+(?:,\\s*[A-Z][a-z]+)*)\\s*\\((\\d{4})\\)"
        )
        
        private val DOI_PATTERN = Pattern.compile(
            "(?:doi:|DOI:|https?://doi\\.org/)(10\\.\\d+/[^\s]+)"
        )
        
        private val URL_PATTERN = Pattern.compile(
            "https?://[^\s\\)\\]\\}]+"
        )
    }
    
    data class Citation(
        val text: String,
        val type: CitationType,
        val authors: List<String> = emptyList(),
        val year: Int? = null,
        val doi: String? = null,
        val url: String? = null,
        val context: String = ""
    )
    
    enum class CitationType {
        APA, IEEE, AUTHOR_YEAR, DOI, URL, UNKNOWN
    }
    
    /**
     * Extract citations from text using multiple regex patterns
     */
    fun extractCitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        
        // Extract APA style citations
        citations.addAll(extractAPACitations(text))
        
        // Extract IEEE style citations
        citations.addAll(extractIEEECitations(text))
        
        // Extract Author-Year style citations
        citations.addAll(extractAuthorYearCitations(text))
        
        // Extract DOIs
        citations.addAll(extractDOICitations(text))
        
        // Extract URLs
        citations.addAll(extractURLCitations(text))
        
        return citations.distinctBy { it.text }
    }
    
    private fun extractAPACitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        val matcher = APA_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val citationText = matcher.group()
            val context = extractContext(text, matcher.start(), matcher.end())
            
            // Parse authors and year from APA format
            val cleanText = citationText.trim('(', ')')
            val parts = cleanText.split(",")
            
            val authors = if (parts.isNotEmpty()) {
                parts[0].trim().split(", ").map { it.trim() }
            } else emptyList()
            
            val year = parts.getOrNull(1)?.let { part ->
                "\\d{4}".toRegex().find(part)?.value?.toInt()
            }
            
            citations.add(
                Citation(
                    text = citationText,
                    type = CitationType.APA,
                    authors = authors,
                    year = year,
                    context = context
                )
            )
        }
        
        return citations
    }
    
    private fun extractIEEECitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        val matcher = IEEE_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val citationText = matcher.group()
            val context = extractContext(text, matcher.start(), matcher.end())
            
            citations.add(
                Citation(
                    text = citationText,
                    type = CitationType.IEEE,
                    context = context
                )
            )
        }
        
        return citations
    }
    
    private fun extractAuthorYearCitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        val matcher = AUTHOR_YEAR_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val citationText = matcher.group()
            val context = extractContext(text, matcher.start(), matcher.end())
            
            val authors = matcher.group(1).split(", ").map { it.trim() }
            val year = matcher.group(2).toInt()
            
            citations.add(
                Citation(
                    text = citationText,
                    type = CitationType.AUTHOR_YEAR,
                    authors = authors,
                    year = year,
                    context = context
                )
            )
        }
        
        return citations
    }
    
    private fun extractDOICitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        val matcher = DOI_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val doi = matcher.group(1)
            val citationText = matcher.group()
            val context = extractContext(text, matcher.start(), matcher.end())
            
            citations.add(
                Citation(
                    text = citationText,
                    type = CitationType.DOI,
                    doi = doi,
                    context = context
                )
            )
        }
        
        return citations
    }
    
    private fun extractURLCitations(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()
        val matcher = URL_PATTERN.matcher(text)
        
        while (matcher.find()) {
            val url = matcher.group()
            val context = extractContext(text, matcher.start(), matcher.end())
            
            citations.add(
                Citation(
                    text = url,
                    type = CitationType.URL,
                    url = url,
                    context = context
                )
            )
        }
        
        return citations
    }
    
    /**
     * Extract surrounding context for a citation
     */
    private fun extractContext(text: String, start: Int, end: Int, windowSize: Int = 100): String {
        val contextStart = maxOf(0, start - windowSize)
        val contextEnd = minOf(text.length, end + windowSize)
        return text.substring(contextStart, contextEnd).trim()
    }
    
    /**
     * Normalize citation text for better matching
     */
    fun normalizeCitation(citation: Citation): String {
        return citation.text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
