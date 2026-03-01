┌─────────────────────────────────────────────────────────────┐
│               CORE KNOWLEDGE ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  📄 Source Documents         →    🧠 Your Concepts         │
│  (Papers, books, reports)         (Evolving ideas)         │
│                                                             │
│         ↓                              ↓                    │
│                                                             │
│  🔗 Citation Network          ↔    💡 Concept Evolution    │
│  (Who influenced what)             (How ideas develop)     │
│                                                             │
│         ↓                              ↓                    │
│                                                             │
│  📊 Relationship Graph        →    🎯 Decision Support     │
│  (Connections & patterns)          (What to research next) │
│                                                             │
└─────────────────────────────────────────────────────────────┘


You (Obsidian/LogSeq)
        ↓ write/edit notes
    ┌───────────────┐
    │  Vault Sync   │──→ monitors changes
    └───────────────┘
            ↓
    ┌─────────────────────────────┐
    │    Agent Orchestrator       │
    ├─────────────────────────────┤
    │ • Curator (ingest)          │──→ Neo4j + Weaviate
    │ • Connector (relationships) │──→ finds hidden links
    │ • Historian (evolution)     │──→ tracks changes
    │ • Scout (trends)            │──→ monitors field
    │ • Composer (synthesis)      │──→ generates text
    │ • Advisor (decisions)       │──→ recommends next steps
    └─────────────────────────────┘
            ↓
    ┌─────────────────────────────┐
    │   Knowledge Graph (Neo4j)   │
    │   • Concepts & evolution    │
    │   • Sources & citations     │
    │   • Relationships & context │
    └─────────────────────────────┘
            ↓
    ┌─────────────────────────────┐
    │  Insights & Recommendations │
    │  • What to research next    │
    │  • Hidden connections       │
    │  • Emerging trends          │
    │  • Writing assistance       │
    └─────────────────────────────┘

Knowledge Graph:
  - Neo4j 5.x для relationship mapping
  - Специфічні node types для research domain

Vector & Semantic Layer:
  - Weaviate для embeddings
  - BGE-M3 (multilingual) для українська/English
  
Document Processing:
  - Apache Tika + PDF parsing
  - Zotero integration для citation extraction
  
AI Models:
  - Local: Llama 3.3 70B (reasoning) або DeepSeek R1
  - Specialized: Gemini 2.0 Flash Thinking для research tasks
  - Fallback: Claude 3.5 Sonnet для complex synthesis



## How to Run

The RDSS uses a unified Vert.x bootstrap launcher for development.

### 1. Prerequisites
- **Java 17+**
- **Docker** (Required for Neo4j and MongoDB)
- **Local LLM Engine** (Currently expects an OpenAI-compatible endpoint at `http://localhost:8080/v1` such as MNN Gateway or Ollama proxy).

### 2. Start Infrastructure
Start the required databases using Docker Compose:
```bash
docker compose up -d
```

### 3. Launch the Application
Run the unified launcher via Gradle. This starts the Ingestion Engine, RAG Service, and API Gateway simultaneously:
```bash
./gradlew :bootstrap:run
```

## Documentation

This project inherently maintains a "Memory Bank" for project status, architecture, and current goals. Refer to the `docs/` directory for detailed information:
- [Project Brief](docs/projectbrief.md)
- [Product Context](docs/productContext.md)
- [Active Context](docs/activeContext.md)
- [System Patterns](docs/systemPatterns.md)
- [Technical Context](docs/techContext.md)
- [Progress Tracking](docs/progress.md)

