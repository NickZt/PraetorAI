# Praetor AI (formerly RDSS)
[![License: CC BY-NC-SA 4.0](https://img.shields.io/badge/License-CC%20BY--NC--SA%204.0-lightgrey.svg)](https://creativecommons.org/licenses/by-nc-sa/4.0/)
**Praetor AI** is a sovereign Edge AI platform designed for constrained environments and highly sensitive data operations. It transforms unstructured sensor data and documents into a deterministic, **Temporal Knowledge Graph** deployed entirely on edge hardware.

┌─────────────────────────────────────────────────────────────┐
│ CORE KNOWLEDGE ARCHITECTURE                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ 📄 Source Documents → 🧠 Your Concepts                      │
│  (Papers, books, reports)         (Evolving ideas)          │
│                                                             │
│ ↓                                 ↓                         │
│                                                             │
│ 🔗 Citation Network ↔ 💡 Concept Evolution                  │
│  (Who influenced what)             (How ideas develop)      │
│                                                             │
│ ↓                                 ↓                         │
│                                                             │
│ 📊 Temporal Graph   → 🎯 Decision Support                    │
│  (Connections & patterns)          (What to research next)  │
│                                                             │
└─────────────────────────────────────────────────────────────┘

You (Obsidian/LogSeq)
↓ write/edit notes
┌───────────────┐
│ Vault Sync │──→ monitors changes
└───────────────┘
↓
┌─────────────────────────────┐
│ Agent Orchestrator │
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
│ Knowledge Graph (Neo4j)   │
│ • Concepts & evolution │
│ • Sources & citations │
│ • Relationships & context │
└─────────────────────────────┘
↓
┌─────────────────────────────┐
│ Insights & Recommendations │
│ • What to research next │
│ • Hidden connections │
│ • Emerging trends │
│ • Writing assistance │
└─────────────────────────────┘

Knowledge Graph:
- Neo4j 5.x + APOC (Temporal GraphRAG)
- strict temporal versioning (ActionNode based `StartDate`/`EndDate`)

Integration & Core Stack:
- Pure Kotlin / Eclipse Vert.X Multi-Reactor
- Zero-Copy Pipeline via DirectByteBuffer (No Python in transport layer)
- Earliest Deadline First (EDF) scheduler for CRITICAL priority event routing

Inference Engine (MNNLLama):
- Edge AI inference engine locally deployed
- Alibaba MNN + ONNX runtime targeting ARM & Edge hardware
- Single Active Model constraint for 2GB RAM edge devices

AI Models & Metrics Guarantees:
- LLM: `Qwen2.5-7B` / `native-Qwen3-Embedding`
- Zero-Shot NER: `gliner-bi-base-v2.0` (bi-encoder architecture for $\mathcal{O}(1)$ edge speed)
  - **Experiment 2 Guarantee:** Edge NER Scalability processing a pool of field contracts extracting up to 1,000 predefined classes on CPU retains $<6\%$ speed degradation vs 10 classes, while maintaining a Micro-F1 $>60\%$.
- Graph Retrieval: LangChain4j Agent Temporal Querying.
  - **Experiment 3 Guarantee:** Temporal queries successfully deploy Cypher-filters across `ActionNode` boundaries to return historical anachronism-free states.

## How to Run

Praetor AI uses a unified Vert.x bootstrap launcher for development.

### 1. Prerequisites

- **Java 17+**
- **Docker** (Required for Neo4j and MongoDB)
- **Local LLM Engine** (Currently expects an OpenAI-compatible endpoint at `http://localhost:8080/v1` such as MNN
  Gateway or Ollama proxy).

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

### 4. Run End-to-End Tests

After starting the application, you can run the automated E2E testing suite:

```bash
# Ensure you have the Python dependencies
/home/nickzt/Projects/TactOrder/RDSS/.venv/bin/python -m pip install requests neo4j

# Run the test suite
/home/nickzt/Projects/TactOrder/RDSS/.venv/bin/python scripts/run_e2e_document_test.py
```

## Documentation

This project inherently maintains a "Memory Bank" for project status, architecture, and current goals. Refer to the
`docs/` directory for detailed information:

- [Project Brief](docs/projectbrief.md)
- [Product Context](docs/productContext.md)
- [Active Context](docs/activeContext.md)
- [System Patterns](docs/systemPatterns.md)
- [Technical Context](docs/techContext.md)
- [Progress Tracking](docs/progress.md)

