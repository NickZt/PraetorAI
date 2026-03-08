# System Patterns

## Architecture Landscape

The system is built as a set of decoupled Vert.x microservices:

- **API Gateway**: Exposes HTTP/REST interfaces.
- **Ingestion Engine**: Listens to folders (`FileWatcher`), chunks semantic data (`SemanticChunker`), and extracts
  citations/concepts via LLMs.
- **RAG Service**: Handles Retrieval-Augmented Generation using LangChain4j and Weaviate/Neo4j embeddings.
- **Core Domain**: Contains shared data structures and models.
- **Bootstrap**: A unified entry point designed for development/testing via a single JVM.

## Communication Pattern

- Services emit and consume messages via Vert.x EventBus.
- Modules act independently but share configuration rules established in `conf/config.yaml`.

## Data Storage

- **Knowledge Graph**: Neo4j serves as the backbone for relational representations (Concepts, Sources, Relationships).
- **Document Store**: MongoDB stores unstructured metadata and full document text logs.
- **Embeddings**: Local LLM gateway generates embeddings, which are consumed by LangChain4j vectors for semantic
  searches.

## Component Flow (Ingestion)

1. **Source** -> `FileWatcher`
2. `FileWatcher` -> `ContentExtractor` (Tika/PDF parsing)
3. `ContentExtractor` -> `SemanticChunker` (Splits to ingestible chunks)
4. `SemanticChunker` -> `LLMExtractor` (Prompting for concepts/citations)
5. `LLMExtractor` -> `GraphWriter` (Writes graph to Neo4j)
