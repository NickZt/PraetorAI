# Progress

## Completed Features
- **Architecture Base**: Created subprojects for API Gateway, Core Domain, Ingestion Engine, and RAG Service.
- **LangChain4j Integration**: Switched out standard Ollama configurations in favor of standard `OpenAiChatModel` pointing to a local MNN Gateway.
- **Graph Retreival Modifications**: Fixed Neo4j legacy entity ID bindings (`node.id()`) to use current `elementId()` functions in `GraphRetriever.kt`.
- **Unified Bootstrapper**: `MainLauncher.kt` created and tested to start all Verticles synchronously.

## Current Working Item
- Verifying the Launch via `./gradlew :bootstrap:run`.

## Blockers and Issues
- Launch attempt resulted in a connection crash for `Neo4j` (`java.net.ConnectException: Connection refused`).
- A blocked deployment was attempted due to Docker Daemon being inaccessible locally (`docker context ls` points to unavailable instances). Needs the user to start their engine manually.
