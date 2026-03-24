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
- Edge AI inference engine locally deployed.
- Alibaba MNN + ONNX runtime targeting ARM & Edge hardware.
- Optimized for **4B-class models** to fit within 4-8GB RAM edge devices.

AI Models & Metrics Guarantees:
- **LLM**: `native-Qwen3-VL-4B-Instruct-Eagle3-MNN` (Instruction & Vision).
- **Embedding**: `native-Qwen3-Embedding-4B-MNN` (2560 dimensions).
- **Zero-Shot NER**: `gliner-bi-base-v2.0` (Native MNN implementation).
  - **Rank Support**: Extract names (e.g., "Jane Doe") and ranks (e.g., "Commander") as distinct properties to prevent entity collision.
- **Graph Retrieval**: LangChain4j + Cypher APOC Traversal.
  - **Tunable Density**: Search limits, retrieval depth, and chunking parameters are fully configurable via `config.yaml`.
  - **Temporal Integrity**: Temporal queries successfully deploy Cypher-filters across `ActionNode` boundaries.

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

Praetor AI maintains dual test suites for standard and sanitized verification:

```bash
# Ensure you have the Python dependencies
pip install requests neo4j

# 1. Run Internal Tactical Tests (Requires test_suite/config.yaml)
python3 test_suite/run_e2e_document_test.py

# 2. Run Sanitized "Vegetarian" Tests (Requires test_suite_veg/config.yaml)
python3 test_suite_veg/run_e2e_veg_test.py
```

### 5. Sanitized "Vegetarian" Infrastructure
For open-source safety, use the **Vegetarian Infrastructure**. Isolation is handled via suite-specific configurations located in `test_suite_veg/`. To run in "Veg" mode, simply copy the veg config to `conf/config.yaml` before starting the application. 

Refer to [Development Workflow](docs/development_workflow.md) for detailed isolation protocols.

### 6. Graph Visualization Dashboard

Praetor AI includes a built-in interactive knowledge graph dashboard powered by Cytoscape.js.

- **Access**: `http://localhost:8081/index.html`
- **Visuals**: Documents (Blue), Concepts (Purple), Chunks (Green), Personnel (Pink), Laws (Amber).
- **Interactivity**: Real-time layout adjustment and metadata inspection.

## Documentation

This project inherently maintains a "Memory Bank" for project status, architecture, and current goals. Refer to the
`docs/` directory for detailed information:

- [Project Brief](docs/projectbrief.md)
- [Product Context](docs/productContext.md)
- [Active Context](docs/activeContext.md)
- [System Patterns](docs/systemPatterns.md)
- [Technical Context](docs/techContext.md)
- [Progress Tracking](docs/progress.md)

