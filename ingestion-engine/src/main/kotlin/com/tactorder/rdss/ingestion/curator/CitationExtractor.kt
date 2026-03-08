package com.tactorder.rdss.ingestion.curator

/**
 * Extracts citations from text using regex patterns.
 * Currently supports:
 * - Numeric style: [1], [1, 2]
 * - Author-Year style: (Smith, 2023), (Doe et al., 2024)
 */
class CitationExtractor {

    private val numericPattern = Regex("\\[\\d+(?:,\\s*\\d+)*\\]")

    // Matches (Author, Year) or (Author et al., Year)
    // Detailed breakdown:
    // \\( : Start with (
    // [A-Z][a-z]+ : Author name (Capitalized)
    // (?: et al\\.)? : Optional " et al."
    // ,?\\s+ : Optional comma and required whitespace
    // \\d{4}[a-z]? : Year (4 digits) optionally followed by a letter (e.g., 2023a)
    // \\) : End with )
    private val authorYearPattern = Regex("\\([A-Z][a-z]+(?: et al\\.)?,?\\s+\\d{4}[a-z]?\\)")

    fun extract(text: String): List<Citation> {
        val citations = mutableListOf<Citation>()

        // Extract numeric citations
        numericPattern.findAll(text).forEach { matchResult ->
            citations.add(
                Citation(
                    rawText = matchResult.value,
                    type = CitationType.NUMERIC,
                    metadata = parseNumericMetadata(matchResult.value)
                )
            )
        }

        // Extract author-year citations
        authorYearPattern.findAll(text).forEach { matchResult ->
            citations.add(
                Citation(
                    rawText = matchResult.value,
                    type = CitationType.AUTHOR_YEAR,
                    metadata = parseAuthorYearMetadata(matchResult.value)
                )
            )
        }

        return citations
    }

    private fun parseNumericMetadata(raw: String): Map<String, String> {
        // Remove brackets and split by comma
        val numbers = raw.trim('[', ']').split(",").map { it.trim() }
        return mapOf("indices" to numbers.joinToString(","))
    }

    private fun parseAuthorYearMetadata(raw: String): Map<String, String> {
        // Remove parentheses
        val content = raw.trim('(', ')')
        // Split by last space to separate author part from year
        // This is a naive split, might need refinement for complex cases
        val lastSpaceIndex = content.lastIndexOf(' ')
        if (lastSpaceIndex == -1) return emptyMap()

        val authorPart = content.substring(0, lastSpaceIndex).trim().removeSuffix(",")
        val yearPart = content.substring(lastSpaceIndex + 1).trim()

        return mapOf(
            "author" to authorPart,
            "year" to yearPart
        )
    }
}

data class Citation(
    val rawText: String,
    val type: CitationType,
    val metadata: Map<String, String>
)

enum class CitationType {
    NUMERIC,
    AUTHOR_YEAR
}
