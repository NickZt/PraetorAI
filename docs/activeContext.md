# Active Context

## Current Focus
Developing the unified application launcher (bootstrap module) to test inter-component communication and ensure all microservices (API Gateway, Ingestion Engine, RAG Service, Core Domain) can start properly within a single JVM.

## Recent Changes
1. Configured LangChain4j embedding and chat models to point to the local LLM Gateway API (`http://localhost:8080/v1`) using `OpenAiEmbeddingModel` and `OpenAiChatModel` in `RagVerticle`.
2. Updated Neo4j GraphRetriever mapping configuration logic to properly map Neo4j entity IDs (`elementId`) instead of deprecated legacy IDs.
3. Bootstrapped a single unified launcher (`MainLauncher`) in the `:bootstrap` module.

## Active Challenges
- **Database Dependency**: The system currently throws a `Connection refused: localhost/127.0.0.1:7687` (Neo4j) on unified launch because the Docker service isn't reachable or up.
- **Docker Issue**: The user's `desktop-linux` Docker context socket is unavailable, and the `default` systemd Docker service is not installed/running.
