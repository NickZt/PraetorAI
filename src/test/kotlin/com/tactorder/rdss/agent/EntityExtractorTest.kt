package com.tactorder.rdss.agent

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.data.message.AiMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class EntityExtractorTest {
    
    private lateinit var mockChatModel: ChatLanguageModel
    private lateinit var entityExtractor: EntityExtractor
    
    @BeforeEach
    fun setUp() {
        mockChatModel = mockk()
        entityExtractor = EntityExtractor(mockChatModel)
    }
    
    @Test
    fun `should extract concepts from research text`() {
        val text = """
            Fog computing represents a distributed computing paradigm that brings computation and data storage closer to the location where it is needed. 
            This approach reduces latency and bandwidth usage compared to traditional cloud computing architectures.
            Machine learning models deployed on fog nodes can provide real-time inference capabilities for edge devices.
        """.trimIndent()
        
        val mockResponse = Response<AiMessage>(
            AiMessage("""
                ```json
                {
                  "concepts": [
                    {
                      "name": "Fog Computing",
                      "description": "Distributed computing paradigm bringing computation closer to data sources",
                      "confidence": 0.9,
                      "context": "Fog computing represents a distributed computing paradigm"
                    },
                    {
                      "name": "Edge Inference",
                      "description": "Machine learning inference on edge devices",
                      "confidence": 0.8,
                      "context": "real-time inference capabilities for edge devices"
                    }
                  ],
                  "methods": [
                    {
                      "name": "Distributed Computing",
                      "description": "Computing across multiple nodes in a network",
                      "confidence": 0.85,
                      "context": "distributed computing paradigm"
                    }
                  ],
                  "datasets": [],
                  "researchAreas": [
                    {
                      "name": "Edge Computing",
                      "description": "Computing at the edge of networks",
                      "confidence": 0.8,
                      "context": "closer to the location where it is needed"
                    }
                  ]
                }
                ```
            """)
        )
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        val entities = entityExtractor.extractEntities(text)
        
        assertEquals(2, entities.concepts.size)
        assertEquals("Fog Computing", entities.concepts[0].name)
        assertEquals(0.9, entities.concepts[0].confidence, 0.01)
        
        assertEquals(1, entities.methods.size)
        assertEquals("Distributed Computing", entities.methods[0].name)
        
        assertEquals(1, entities.researchAreas.size)
        assertEquals("Edge Computing", entities.researchAreas[0].name)
    }
    
    @Test
    fun `should extract methods from technical text`() {
        val text = """
            We employed quantization techniques to reduce the model size by 75% while maintaining accuracy.
            Knowledge distillation was used to transfer knowledge from a large teacher model to a smaller student model.
            Pruning removed unnecessary connections from the neural network.
        """.trimIndent()
        
        val mockResponse = Response<AiMessage>(
            AiMessage("""
                ```json
                {
                  "concepts": [],
                  "methods": [
                    {
                      "name": "Quantization",
                      "description": "Technique to reduce model size and computational requirements",
                      "confidence": 0.95,
                      "context": "employed quantization techniques to reduce the model size"
                    },
                    {
                      "name": "Knowledge Distillation",
                      "description": "Method to transfer knowledge between models",
                      "confidence": 0.9,
                      "context": "Knowledge distillation was used to transfer knowledge"
                    },
                    {
                      "name": "Pruning",
                      "description": "Removing unnecessary neural network connections",
                      "confidence": 0.85,
                      "context": "Pruning removed unnecessary connections"
                    }
                  ],
                  "datasets": [],
                  "researchAreas": []
                }
                ```
            """)
        )
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        val methods = entityExtractor.extractMethods(text)
        
        assertEquals(3, methods.size)
        assertTrue(methods.any { it.name == "Quantization" })
        assertTrue(methods.any { it.name == "Knowledge Distillation" })
        assertTrue(methods.any { it.name == "Pruning" })
        
        methods.forEach { method ->
            assertTrue(method.confidence >= 0.7)
        }
    }
    
    @Test
    fun `should extract research areas from academic text`() {
        val text = """
            This research contributes to the field of military AI by developing new approaches for tactical decision support.
            The work spans both fog computing and edge computing domains, with applications in distributed systems.
            Machine learning techniques are applied to solve real-time processing challenges in battlefield environments.
        """.trimIndent()
        
        val mockResponse = Response<AiMessage>(
            AiMessage("""
                ```json
                {
                  "concepts": [],
                  "methods": [],
                  "datasets": [],
                  "researchAreas": [
                    {
                      "name": "Military AI",
                      "description": "AI applications for military and defense systems",
                      "confidence": 0.9,
                      "context": "field of military AI by developing new approaches"
                    },
                    {
                      "name": "Fog Computing",
                      "description": "Distributed computing paradigm for network edge",
                      "confidence": 0.85,
                      "context": "spans both fog computing and edge computing domains"
                    },
                    {
                      "name": "Edge Computing",
                      "description": "Computing at the edge of networks",
                      "confidence": 0.8,
                      "context": "fog computing and edge computing domains"
                    }
                  ]
                }
                ```
            """)
        )
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        val researchAreas = entityExtractor.extractResearchAreas(text)
        
        assertEquals(3, researchAreas.size)
        assertTrue(researchAreas.any { it.name == "Military AI" })
        assertTrue(researchAreas.any { it.name == "Fog Computing" })
        assertTrue(researchAreas.any { it.name == "Edge Computing" })
        
        researchAreas.forEach { area ->
            assertTrue(area.confidence >= 0.6)
        }
    }
    
    @Test
    fun `should validate and filter entities`() {
        val entities = EntityExtractor.ExtractedEntities(
            concepts = listOf(
                EntityExtractor.ExtractedConcept("Valid Concept", "Description", 0.8, "context"),
                EntityExtractor.ExtractedConcept("", "Description", 0.9, "context"), // Empty name
                EntityExtractor.ExtractedConcept("Example", "Description", 0.6, "context"), // Low confidence
                EntityExtractor.ExtractedConcept("etc", "Description", 0.8, "context") // Filter word
            ),
            methods = listOf(
                EntityExtractor.ExtractedMethod("Valid Method", "Description", 0.7, "context"),
                EntityExtractor.ExtractedMethod("", "Description", 0.8, "context")
            ),
            datasets = emptyList(),
            researchAreas = emptyList()
        )
        
        val validated = entityExtractor.validateEntities(entities)
        
        assertEquals(1, validated.concepts.size)
        assertEquals("Valid Concept", validated.concepts[0].name)
        
        assertEquals(1, validated.methods.size)
        assertEquals("Valid Method", validated.methods[0].name)
    }
    
    @Test
    fun `should handle malformed JSON response gracefully`() {
        val text = "Some research text about machine learning and edge computing."
        
        val mockResponse = Response<AiMessage>(AiMessage("Invalid JSON response"))
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        val entities = entityExtractor.extractEntities(text)
        
        assertTrue(entities.concepts.isEmpty())
        assertTrue(entities.methods.isEmpty())
        assertTrue(entities.datasets.isEmpty())
        assertTrue(entities.researchAreas.isEmpty())
    }
    
    @Test
    fun `should handle empty text`() {
        val mockResponse = Response<AiMessage>(
            AiMessage("""
                ```json
                {
                  "concepts": [],
                  "methods": [],
                  "datasets": [],
                  "researchAreas": []
                }
                ```
            """)
        )
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        val entities = entityExtractor.extractEntities("")
        
        assertTrue(entities.concepts.isEmpty())
        assertTrue(entities.methods.isEmpty())
        assertTrue(entities.datasets.isEmpty())
        assertTrue(entities.researchAreas.isEmpty())
    }
    
    @Test
    fun `should include document title in extraction prompt`() {
        val text = "Research content about fog computing."
        val title = "Fog Computing for Military Applications"
        
        val mockResponse = Response<AiMessage>(
            AiMessage("""
                ```json
                {
                  "concepts": [],
                  "methods": [],
                  "datasets": [],
                  "researchAreas": []
                }
                ```
            """)
        )
        
        every { mockChatModel.generate(any()) } returns mockResponse
        
        entityExtractor.extractEntities(text, title)
        
        // Verify that the mock was called with a prompt containing the title
        verify { mockChatModel.generate(match { it.contains("Document Title: $title") }) }
    }
}
