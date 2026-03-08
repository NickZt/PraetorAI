package com.tactorder.rdss.agent

import com.tactorder.rdss.domain.*
import com.tactorder.rdss.repository.*
import dev.langchain4j.model.chat.ChatLanguageModel
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
@Transactional
class AdvisorAgent(
    private val chatModel: ChatLanguageModel,
    private val conceptRepository: ConceptRepository,
    private val experimentRepository: ExperimentRepository,
    private val sourceDocumentRepository: SourceDocumentRepository
) {

    private val logger = KotlinLogging.logger {}

    data class MaturityAssessment(
        val conceptId: String,
        val conceptName: String,
        val currentMaturity: ConceptMaturity,
        val maturityScore: Double,
        val factors: List<MaturityFactor>,
        val recommendation: String
    )

    data class MaturityFactor(
        val factor: String,
        val score: Double,
        val description: String
    )

    data class ResearchRecommendation(
        val type: RecommendationType,
        val title: String,
        val description: String,
        val priority: Priority,
        val estimatedEffort: String,
        val relatedConcepts: List<String>,
        val reasoning: String
    )

    enum class RecommendationType {
        EXPERIMENT, VALIDATION, PUBLICATION, COLLABORATION, FURTHER_RESEARCH
    }

    enum class Priority {
        HIGH, MEDIUM, LOW
    }

    data class ResearchRoadmap(
        val title: String,
        val timeframe: String,
        val phases: List<RoadmapPhase>
    )

    data class RoadmapPhase(
        val name: String,
        val duration: String,
        val objectives: List<String>,
        val deliverables: List<String>
    )

    /**
     * Analyze concept maturity and provide scoring
     */
    fun analyzeConceptMaturity(conceptId: String): MaturityAssessment {
        logger.info { "Analyzing maturity for concept: $conceptId" }

        val concept = conceptRepository.findById(conceptId).orElseThrow()
        val factors = calculateMaturityFactors(concept)
        val maturityScore = calculateOverallMaturityScore(factors)
        val recommendation = generateMaturityRecommendation(concept, maturityScore, factors)

        return MaturityAssessment(
            conceptId = concept.id,
            conceptName = concept.name,
            currentMaturity = concept.maturity,
            maturityScore = maturityScore,
            factors = factors,
            recommendation = recommendation
        )
    }

    private fun calculateMaturityFactors(concept: Concept): List<MaturityFactor> {
        val factors = mutableListOf<MaturityFactor>()

        // Factor 1: Validation by experiments
        val validatingExperiments = experimentRepository.findByValidatedConcept(concept.id)
        val validationScore = calculateValidationScore(validatingExperiments)
        factors.add(
            MaturityFactor(
                factor = "Experimental Validation",
                score = validationScore,
                description = "Based on ${validatingExperiments.size} validating experiments"
            )
        )

        // Factor 2: Concept age and evolution
        val ageScore = calculateAgeScore(concept)
        factors.add(
            MaturityFactor(
                factor = "Temporal Development",
                score = ageScore,
                description = "Based on concept age and version history"
            )
        )

        // Factor 3: Citation and inspiration
        val inspirationScore = calculateInspirationScore(concept)
        factors.add(
            MaturityFactor(
                factor = "Academic Recognition",
                score = inspirationScore,
                description = "Based on citations and academic inspiration"
            )
        )

        // Factor 4: Relationship complexity
        val relationshipScore = calculateRelationshipScore(concept)
        factors.add(
            MaturityFactor(
                factor = "Network Integration",
                score = relationshipScore,
                description = "Based on connections to other concepts"
            )
        )

        return factors
    }

    private fun calculateValidationScore(experiments: List<Experiment>): Double {
        if (experiments.isEmpty()) return 0.0

        val completedExperiments = experiments.count { it.status == ExperimentStatus.COMPLETED }
        val successfulExperiments = experiments.count {
            it.status == ExperimentStatus.COMPLETED &&
                    it.conclusion?.lowercase()?.contains("success") == true
        }

        return when {
            completedExperiments == 0 -> 0.0
            successfulExperiments == completedExperiments -> 1.0
            else -> (successfulExperiments.toDouble() / completedExperiments) * 0.8 + 0.2
        }
    }

    private fun calculateAgeScore(concept: Concept): Double {
        val daysSinceCreation = ChronoUnit.DAYS.between(concept.firstMentioned, LocalDateTime.now())
        val daysSinceUpdate = ChronoUnit.DAYS.between(concept.lastUpdated, LocalDateTime.now())

        val ageScore = minOf(daysSinceCreation / 365.0, 1.0) // Max 1 point for age
        val activityScore = if (daysSinceUpdate < 30) 1.0 else if (daysSinceUpdate < 90) 0.7 else 0.3

        return (ageScore + activityScore) / 2.0
    }

    private fun calculateInspirationScore(concept: Concept): Double {
        val inspirationCount = concept.inspiredBy.size
        return minOf(inspirationCount / 5.0, 1.0) // Max 1 point for 5+ inspirations
    }

    private fun calculateRelationshipScore(concept: Concept): Double {
        val relationshipCount = concept.buildsOn.size + concept.contradicts.size
        return minOf(relationshipCount / 3.0, 1.0) // Max 1 point for 3+ relationships
    }

    private fun calculateOverallMaturityScore(factors: List<MaturityFactor>): Double {
        return factors.map { it.score }.average()
    }

    private fun generateMaturityRecommendation(
        concept: Concept,
        maturityScore: Double,
        factors: List<MaturityFactor>
    ): String {
        return when {
            maturityScore >= 0.8 -> "Concept is mature and ready for publication or deployment"
            maturityScore >= 0.6 -> "Concept is developing well, consider additional validation"
            maturityScore >= 0.4 -> "Concept needs more development and experimental validation"
            else -> "Concept is in early stages, requires significant further research"
        }
    }

    /**
     * Generate research recommendations based on concept analysis
     */
    suspend fun generateRecommendations(conceptId: String, limit: Int = 10): List<ResearchRecommendation> {
        logger.info { "Generating recommendations for concept: $conceptId" }

        val concept = conceptRepository.findById(conceptId).orElseThrow()
        val maturity = analyzeConceptMaturity(conceptId)
        val recommendations = mutableListOf<ResearchRecommendation>()

        // Experiment recommendations
        if (maturity.maturityScore < 0.7) {
            recommendations.add(
                ResearchRecommendation(
                    type = RecommendationType.EXPERIMENT,
                    title = "Validate ${concept.name} through controlled experiments",
                    description = "Design and conduct experiments to test the core hypotheses of ${concept.name}",
                    priority = Priority.HIGH,
                    estimatedEffort = "2-4 weeks",
                    relatedConcepts = listOf(conceptId),
                    reasoning = "Concept needs experimental validation to increase maturity"
                )
            )
        }

        // Publication recommendations
        if (maturity.maturityScore >= 0.7) {
            recommendations.add(
                ResearchRecommendation(
                    type = RecommendationType.PUBLICATION,
                    title = "Publish research on ${concept.name}",
                    description = "Prepare a manuscript describing ${concept.name} and its validation",
                    priority = Priority.MEDIUM,
                    estimatedEffort = "4-8 weeks",
                    relatedConcepts = listOf(conceptId),
                    reasoning = "Concept shows sufficient maturity for academic publication"
                )
            )
        }

        // Collaboration recommendations
        val relatedConcepts = conceptRepository.findRelatedConcepts(conceptId)
        if (relatedConcepts.isNotEmpty()) {
            recommendations.add(
                ResearchRecommendation(
                    type = RecommendationType.COLLABORATION,
                    title = "Collaborate with researchers working on related concepts",
                    description = "Explore synergies with concepts: ${
                        relatedConcepts.take(3).joinToString(", ") { it.name }
                    }",
                    priority = Priority.MEDIUM,
                    estimatedEffort = "1-2 weeks",
                    relatedConcepts = relatedConcepts.map { it.id },
                    reasoning = "Related concepts suggest opportunities for collaboration"
                )
            )
        }

        // Use LLM to generate additional recommendations
        val llmRecommendations = generateLLMRecommendations(concept, maturity)
        recommendations.addAll(llmRecommendations)

        return recommendations.sortedByDescending { it.priority.ordinal }
            .take(limit)
    }

    private suspend fun generateLLMRecommendations(
        concept: Concept,
        maturity: MaturityAssessment
    ): List<ResearchRecommendation> {
        val prompt = buildRecommendationPrompt(concept, maturity)
        val response = chatModel.generate(prompt)

        return parseRecommendationResponse(response.content().text(), concept.id)
    }

    private fun buildRecommendationPrompt(concept: Concept, maturity: MaturityAssessment): String {
        return """
            You are an expert research advisor specializing in computer science and military AI research.
            
            Based on the following concept analysis, provide specific research recommendations:
            
            Concept: ${concept.name}
            Description: ${concept.description}
            Current Maturity: ${concept.maturity}
            Maturity Score: ${maturity.maturityScore}
            
            Maturity Factors:
            ${maturity.factors.joinToString("\n") { "- ${it.factor}: ${it.score} (${it.description})" }}
            
            Tags: ${concept.tags.joinToString(", ")}
            
            Provide 3-5 specific, actionable recommendations for advancing this concept. 
            Consider the military/tactical context and fog computing applications.
            
            Format your response as JSON:
            ```json
            {
              "recommendations": [
                {
                  "type": "EXPERIMENT|VALIDATION|PUBLICATION|COLLABORATION|FURTHER_RESEARCH",
                  "title": "Brief title",
                  "description": "Detailed description",
                  "priority": "HIGH|MEDIUM|LOW",
                  "estimatedEffort": "Time estimate",
                  "reasoning": "Why this recommendation"
                }
              ]
            }
            ```
        """.trimIndent()
    }

    private fun parseRecommendationResponse(response: String, conceptId: String): List<ResearchRecommendation> {
        return try {
            // Extract JSON from response
            val jsonMatch = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(response)
                ?.groupValues?.get(1)
                ?: response.trim()

            // Parse recommendations (simplified - would need proper JSON parsing)
            emptyList<ResearchRecommendation>() // Placeholder
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse recommendation response" }
            emptyList()
        }
    }

    /**
     * Generate a research roadmap
     */
    suspend fun generateResearchRoadmap(conceptId: String): ResearchRoadmap {
        logger.info { "Generating research roadmap for concept: $conceptId" }

        val concept = conceptRepository.findById(conceptId).orElseThrow()
        val maturity = analyzeConceptMaturity(conceptId)
        val recommendations = generateRecommendations(conceptId)

        val prompt = buildRoadmapPrompt(concept, maturity, recommendations)
        val response = chatModel.generate(prompt)

        return parseRoadmapResponse(response.content().text(), concept.name)
    }

    private fun buildRoadmapPrompt(
        concept: Concept,
        maturity: MaturityAssessment,
        recommendations: List<ResearchRecommendation>
    ): String {
        return """
            Create a research roadmap for the following concept:
            
            Concept: ${concept.name}
            Current Maturity: ${maturity.maturityScore}
            
            Key Recommendations:
            ${recommendations.take(5).joinToString("\n") { "- ${it.title}: ${it.description}" }}
            
            Create a 6-12 month roadmap with 3-4 phases. Each phase should have:
            - Clear name and duration
            - Specific objectives
            - Tangible deliverables
            
            Format as JSON:
            ```json
            {
              "title": "Research Roadmap for ${concept.name}",
              "timeframe": "6-12 months",
              "phases": [
                {
                  "name": "Phase Name",
                  "duration": "2-3 months",
                  "objectives": ["Objective 1", "Objective 2"],
                  "deliverables": ["Deliverable 1", "Deliverable 2"]
                }
              ]
            }
            ```
        """.trimIndent()
    }

    private fun parseRoadmapResponse(response: String, conceptName: String): ResearchRoadmap {
        return try {
            // Extract JSON from response
            val jsonMatch = Regex("```json\\s*(\\{.*?\\})\\s*```", RegexOption.DOT_MATCHES_ALL)
                .find(response)
                ?.groupValues?.get(1)
                ?: response.trim()

            // Parse roadmap (simplified - would need proper JSON parsing)
            ResearchRoadmap(
                title = "Research Roadmap for $conceptName",
                timeframe = "6-12 months",
                phases = emptyList()
            ) // Placeholder
        } catch (e: Exception) {
            logger.error(e) { "Failed to parse roadmap response" }
            ResearchRoadmap(
                title = "Research Roadmap for $conceptName",
                timeframe = "6-12 months",
                phases = emptyList()
            )
        }
    }

    /**
     * Identify publication opportunities
     */
    fun identifyPublicationOpportunities(conceptId: String): List<String> {
        logger.info { "Identifying publication opportunities for concept: $conceptId" }

        val concept = conceptRepository.findById(conceptId).orElseThrow()
        val opportunities = mutableListOf<String>()

        // Based on concept tags and research areas
        if (concept.tags.contains("fog-computing")) {
            opportunities.add("IEEE Transactions on Fog Computing")
            opportunities.add("ACM MobiCom (Mobile Computing and Networking)")
        }

        if (concept.tags.contains("military-ai")) {
            opportunities.add("Military Operations Research")
            opportunities.add("IEEE Transactions on Systems, Man, and Cybernetics")
        }

        if (concept.tags.contains("ml-inference")) {
            opportunities.add("NeurIPS (Neural Information Processing Systems)")
            opportunities.add("ICML (International Conference on Machine Learning)")
        }

        return opportunities.distinct()
    }
}
