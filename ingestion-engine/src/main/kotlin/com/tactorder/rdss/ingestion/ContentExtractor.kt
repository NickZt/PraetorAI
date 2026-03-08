package com.tactorder.rdss.ingestion

import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import java.io.File
import java.io.FileInputStream

class ContentExtractor {
    private val tika = Tika()

    fun extract(filePath: String): ExtractedDocument {
        val file = File(filePath)
        val metadata = Metadata()

        // Tika parses content to string
        val content = FileInputStream(file).use { stream ->
            tika.parseToString(stream, metadata)
        }

        return ExtractedDocument(
            filePath = filePath,
            fileName = file.name,
            content = content,
            metadata = metadata.names().associateWith { metadata.get(it) }
        )
    }
}

data class ExtractedDocument(
    val filePath: String,
    val fileName: String,
    val content: String,
    val metadata: Map<String, String>
)
