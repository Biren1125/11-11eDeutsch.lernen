package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.audio.AudioAndTtsManager
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.model.Word
import com.example.deutschlernen.ui.theme.RedMistake

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyVocabularyScreen(
    repository: DeutschRepository,
    audioManager: AudioAndTtsManager,
    onBack: () -> Unit,
    onPracticeCustom: (List<Word>) -> Unit
) {
    val customWords by repository.customWords.collectAsState()
    val settings by repository.userSettings.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBulkImportDialog by remember { mutableStateOf(false) }
    var singleDe by remember { mutableStateOf("") }
    var singleEn by remember { mutableStateOf("") }
    var bulkText by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "My Custom Vocabulary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${customWords.size} words added",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBulkImportDialog = true }, modifier = Modifier.testTag("bulk_import_btn")) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Bulk Import")
                    }
                    IconButton(onClick = { showAddDialog = true }, modifier = Modifier.testTag("add_word_btn")) {
                        Icon(Icons.Default.Add, contentDescription = "Add Word")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (customWords.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { onPracticeCustom(customWords) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("practice_custom_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice My Vocabulary (${customWords.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            if (customWords.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📝", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No custom words yet!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Add personal words or paste vocabulary lists (e.g. German — English).",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { showAddDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Word")
                                }
                                OutlinedButton(
                                    onClick = { showBulkImportDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import List")
                                }
                            }
                        }
                    }
                }
            } else {
                items(customWords.size) { index ->
                    val word = customWords[index]
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = word.de,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = word.en,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { audioManager.speakGerman(word.de, settings.ttsSpeed, settings.ttsPitch) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "Pronounce", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = { repository.removeCustomWord(word.id) },
                                    modifier = Modifier.size(36.dp).testTag("delete_custom_${word.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RedMistake)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Single Word Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Word") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = singleDe,
                        onValueChange = { singleDe = it },
                        label = { Text("German Word (e.g. der Tisch)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_de_word")
                    )
                    OutlinedTextField(
                        value = singleEn,
                        onValueChange = { singleEn = it },
                        label = { Text("English Translation (e.g. table)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_en_word")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (singleDe.isNotBlank() && singleEn.isNotBlank()) {
                            repository.addCustomWords("$singleDe — $singleEn")
                            singleDe = ""
                            singleEn = ""
                            showAddDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_custom_word_btn")
                ) {
                    Text("Save Word")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Bulk Import Dialog
    if (showBulkImportDialog) {
        AlertDialog(
            onDismissRequest = { showBulkImportDialog = false },
            title = { Text("Bulk Vocabulary Import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your word pairs (one per line). Formats supported: German — English, German; English, TSV, or CSV.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        placeholder = { Text("der Apfel — apple\ndas Haus — house\nlernen; to learn") },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("input_bulk_text")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bulkText.isNotBlank()) {
                            val count = repository.addCustomWords(bulkText)
                            bulkText = ""
                            showBulkImportDialog = false
                        }
                    },
                    modifier = Modifier.testTag("import_words_btn")
                ) {
                    Text("Import Words")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkImportDialog = false }) { Text("Cancel") }
            }
        )
    }
}
