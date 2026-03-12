package uk.mezon.rdss.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uk.mezon.rdss.api.PraetorApiClient
import uk.mezon.rdss.utils.pickFile

data class ChatMessage(val text: String, val isUser: Boolean, val timestamp: String? = null)

@Composable
fun ChatScreen() {
    val apiClient = remember { PraetorApiClient() }
    val coroutineScope = rememberCoroutineScope()

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    var ingestPath by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var stats by remember { mutableStateOf(uk.mezon.rdss.api.StatsResponse()) }

    LaunchedEffect(Unit) {
        // Initial fetch
        stats = apiClient.fetchStats()
        // Simple polling every 10 seconds for metrics
        while (true) {
            delay(10000)
            stats = apiClient.fetchStats()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Praetor AI: Temporal Intelligence",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Metrics Widget
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Documents", style = MaterialTheme.typography.labelSmall)
                        Text(text = "${stats.documents}", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Concepts", style = MaterialTheme.typography.labelSmall)
                        Text(text = "${stats.concepts}", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Sections", style = MaterialTheme.typography.labelSmall)
                        Text(text = "${stats.sections}", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Relations", style = MaterialTheme.typography.labelSmall)
                        Text(text = "${stats.relations}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Ingestion Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ingestPath,
                onValueChange = { ingestPath = it },
                label = { Text("Ingest Entity Path (.pdf/.md)") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val path = pickFile()
                        if (path != null) {
                            ingestPath = path
                        }
                    }
                }
            ) {
                Text("Browse")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (ingestPath.isNotBlank()) {
                        coroutineScope.launch {
                            val response = apiClient.ingestDocument(ingestPath)
                            messages = messages + ChatMessage(
                                text = "System Event: ${response.status ?: response.error}",
                                isUser = false,
                                timestamp = "INGESTION"
                            )
                            Napier.d { "System Event: ${response.status ?: response.error}" }
                            ingestPath = ""
                            stats = apiClient.fetchStats() // Refresh exact stats after ingest click
                        }
                    }
                }
            ) {
                Text("Submit Source")
            }
        }

        HorizontalDivider()

        // Chat History
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
        }

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = dateText,
                onValueChange = { dateText = it },
                label = { Text("Temporal Status (YYYY-MM-DD)") },
                modifier = Modifier.weight(0.3f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Query Knowledge Graph...") },
                modifier = Modifier.weight(0.7f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = ChatMessage(
                            text = inputText,
                            isUser = true,
                            timestamp = dateText.takeIf { it.isNotBlank() })
                        messages = messages + userMsg
                        val queryToRun = inputText
                        val dateToRun = dateText
                        inputText = ""
                        isLoading = true

                        coroutineScope.launch {
                            val response = apiClient.queryRagAgent(queryToRun, dateToRun)
                            isLoading = false
                            messages = messages + ChatMessage(
                                text = response.answer ?: response.error ?: "Operation Failed",
                                isUser = false
                            )
                            Napier.d { "apiClient.queryRagAgent: ${response.answer ?: response.error ?: "Operation Failed"}" }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                Text("Transmit")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor =
        if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor =
        if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).widthIn(max = 700.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.timestamp != null) {
                    Text(
                        text = "Timecode Filter: ${message.timestamp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
