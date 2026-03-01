# Project Brief: Research Decision Support System (RDSS)

## Core Objective
To build a Research Decision Support System (RDSS) that acts as an intelligent agent orchestra for researchers. It transforms raw sources (papers, books, reports) into a connected relationship graph and concept evolution timeline, ultimately providing actionable decision support on what to research next.

## Vision "Core Knowledge Architecture"
- **Ingestion**: Source Documents -> Your Concepts
- **Processing**: Citation Network <-> Concept Evolution
- **Output**: Relationship Graph -> Decision Support

## Key Components
1. **Agent Orchestrator**: Manages specialized agents (Curator, Connector, Historian, Scout, Composer, Advisor).
2. **Knowledge Graph**: Neo4j-based graph containing concepts, sources, citations, and relationships.
3. **MNN Service & LLM Gateway**: Handles the LLM interactions (embedding, chat) via an OpenAI-compatible API.
4. **Vector & Semantic Layer**: Weaviate integration (planned/existing) for semantic search.
5. **EventBus**: Vert.x EventBus for intra/inter-component communication in the unified application.

## Current State
The project consists of several microservices (API Gateway, Ingestion Engine, RAG Service, Core Domain, Bootstrap/Main Launcher) designed to run within a unified Vert.x application for ease of deployment.
