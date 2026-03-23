package com.tactorder.rdss.rag

import io.vertx.core.json.JsonObject

class ContextBuilder {

    fun buildContext(graphData: List<JsonObject>): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Context from Knowledge Graph:\n\n")

        // Simple formatter: Group by type
        // Real implementation would be more narrative

        val nodes = graphData.filter { it.containsKey("labels") }
        val edges = graphData.filter { it.containsKey("type") }

        // Map ID to Name/Title for better readability in edges
        val idMap = nodes.associate {
            it.getLong("id") to (it.getJsonObject("props").getString("title") ?: it.getJsonObject("props")
                .getString("name") ?: "Unknown")
        }

        stringBuilder.append("Entities:\n")
        nodes.forEach { node ->
            val labels = node.getJsonArray("labels").joinToString(", ")
            val props = node.getJsonObject("props")
            val name = props.getString("title") ?: props.getString("name") ?: props.getString("text")?.take(20) ?: "Unknown"
            val desc = props.getString("description") ?: props.getString("content") ?: props.getString("text") ?: "..."

            stringBuilder.append("- [$labels] $name: ${desc.take(1000)}${if (desc.length > 1000) "..." else ""}\n")
        }

        stringBuilder.append("\nRelationships:\n")
        edges.forEach { edge ->
            val startName = idMap[edge.getLong("start")] ?: "ID:${edge.getLong("start")}"
            val endName = idMap[edge.getLong("end")] ?: "ID:${edge.getLong("end")}"
            val type = edge.getString("type")
            val props = edge.getJsonObject("props")
            val info = if (props.isEmpty) "" else " ($props)"

            stringBuilder.append("- $startName --[$type]-- $endName $info\n")
        }

        return stringBuilder.toString()
    }
}
