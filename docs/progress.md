# Progress

## Completed Features

- **Architecture Base**: Created subprojects for API Gateway, Core Domain, Ingestion Engine, and RAG Service.
- **LangChain4j Integration**: Switched out standard Ollama configurations in favor of standard `OpenAiChatModel`
  pointing to a local MNN Gateway, using a mock `sk-local` API key to prevent proxying.
- **Graph Retreival Modifications**: Fixed Neo4j legacy entity ID bindings (`node.id()`) to use current `elementId()`
  functions in `GraphRetriever.kt`.
- **Unified Bootstrapper**: `MainLauncher.kt` created and tested to start all Verticles synchronously.
- **Ingestion Pipeline Validation**: Validated parsing functionality (Tika extraction) and discovered PDFBox versioning
  incompatibilities which were patched out.
- **Documentation**: Instantiated Memory Bank and 'How to Run' architecture in `README.md`.

## Current Working Item

- Standing up the local LLM proxy (MNN / Ollama) to finalize end-to-end ingestion tests.

## Blockers and Issues

- The local text extraction pipeline works, but the AI chunking/extraction step `java.net.ConnectException` fails
  because port 8080 (LLM) is currently offline.

## Next Steps (Roadmap)

- Develop an import script/system for ChatGPT and Perplexity dialogues.
- Create a conversational Chatbot Interface (Telegram or WhatsApp) for mobile Knowledge Graph interactions.
