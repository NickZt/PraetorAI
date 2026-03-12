package uk.mezon.rdss.utils

import java.awt.FileDialog
import java.awt.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun pickFile(): String? = withContext(Dispatchers.IO) {
    val dialog = FileDialog(null as Frame?, "Select Document to Ingest", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return@withContext null
    val dir = dialog.directory ?: return@withContext null
    "$dir$file"
}
