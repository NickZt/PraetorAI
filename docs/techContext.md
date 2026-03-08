# Technical Context

## Core Technologies

- **Kotlin**: Primary programming language for backend components.
- **Vert.x**: Asynchronous, event-driven framework used to implement microservices.
- **Gradle**: Build system with Kotlin DSL (`build.gradle.kts`).
- **Neo4j**: Graph database configured via the official driver and OGM.
- **MongoDB**: Document database.
- **LangChain4j**: Framework used for Retrieval-Augmented Generation (RAG) pipelines and LLM operations.
- **Docker Compose**: Used to orchestrate local infrastructure dependencies (database services).

## Environment and Constraints

- The `MainLauncher` requires active connections to Neo4j (port 7687) and an LLM Gateway (port 8080) upon execution to
  prevent `Connection refused` errors.
- The unified application uses the Vert.x EventBus to route messages between isolated verticles deployed concurrently.
- Coroutines (`io.vertx.kotlin.coroutines.CoroutineVerticle`) manage async operations in endpoints without blocking the
  Vert.x event loop.

## Key Files

- `conf/config.yaml`: Contains configuration blocks (URI, credentials) loaded at runtime by `ConfigLoader`.
- `bootstrap/build.gradle.kts`: Controls compilation of dependencies and sets the application's `mainClass`.
- `docker-compose.yml`: Defines `neo4j` and `mongodb` service lifecycles.
