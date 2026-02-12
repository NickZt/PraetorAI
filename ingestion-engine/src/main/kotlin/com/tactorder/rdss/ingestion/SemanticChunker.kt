package com.tactorder.rdss.ingestion

class SemanticChunker(
    private val chunkSize: Int = 1000,
    private val overlap: Int = 200
) {
    fun chunk(text: String): List<String> {
        // Naive implementation: Character/Word based for now.
        // In production, use token counting (e.g. JTokkit) to match LLM limits.
        // Here we simulate "semantic" by splitting on paragraphs/sentences within limits.
        
        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n\n")
        
        var currentChunk = StringBuilder()
        
        for (para in paragraphs) {
            if (currentChunk.length + para.length > chunkSize) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString().trim())
                    // Apply overlap: keep last N chars? 
                    // Complex with string builder.
                    // For MVP: clear and start new. 
                    // Better: use sliding window of sentences.
                    currentChunk = StringBuilder()
                }
                
                // If paragraph itself is too large, split it hard
                if (para.length > chunkSize) {
                    para.chunked(chunkSize).forEach { subPara ->
                         chunks.add(subPara)
                    }
                } else {
                    currentChunk.append(para).append("\n\n")
                }
            } else {
                currentChunk.append(para).append("\n\n")
            }
        }
        
        if (currentChunk.isNotEmpty()) {
             chunks.add(currentChunk.toString().trim())
        }
        
        return chunks
    }
}
