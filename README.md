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





