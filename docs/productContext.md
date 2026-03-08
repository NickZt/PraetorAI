# Product Context

## Purpose

The Research Decision Support System (RDSS) aims to augment researchers' capabilities by intelligently processing source
materials and synthesizing knowledge. This system is designed to provide "Core Knowledge Architecture" enabling users to
transform papers, books, and reports into evolving concepts, and ultimately into actionable decision support.

## Problems Solved

1. **Information Overload**: Researchers struggle to keep track of citations and influences across vast document
   repositories. RDSS automates relationship mapping and tracks concept evolution.
2. **Disconnected Insights**: Concepts found in different sources are often isolated. By linking them in a Neo4j
   knowledge graph, RDSS can find hidden connections and highlight patterns.
3. **Complex Decisions**: Determining "what to research next" requires synthesizing current trends and past insights.
   Agentic orchestration within RDSS assists with these recommendations.

## Expected Operation

Users typically interact with tools like LogSeq or Obsidian for notes. RDSS ingests these changes via the Curator, and
multi-agent coordination (Scout, Historian, Composer, Connector, Advisor) synthesizes and updates a centralized
Knowledge Graph. From this graph, deep insights are extracted via Retrieval-Augmented Generation (RAG).

## Future Roadmap (Planned Features)

1. **Dialogue Import**: Ability to import entire dialogues and workspaces from external AI assistants like ChatGPT and
   Perplexity to merge transient brainstorming sessions into the permanent Knowledge Graph.
2. **Chatbot Interface**: A dedicated Telegram or WhatsApp bot interface allowing users to converse directly with their
   Knowledge Graph on the go, effectively talking to their personal "Advisor" agent.
