package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.deutschlernen.ui.components.WordCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DifficultWordsScreen(
    repository: DeutschRepository,
    audioManager: AudioAndTtsManager,
    onBack: () -> Unit,
    onPracticeDifficult: (List<Word>) -> Unit
) {
    var selectedLevelFilter by remember { mutableStateOf<String?>("ALL") }
    val wordStats by repository.wordStats.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val settings by repository.userSettings.collectAsState()

    val levelKey = if (selectedLevelFilter == "ALL") null else selectedLevelFilter
    val difficultWords = remember(selectedLevelFilter, wordStats) {
        repository.wordsDifficult(levelKey)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Difficult Words", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            text = "${difficultWords.size} words need review",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (difficultWords.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { onPracticeDifficult(difficultWords) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("practice_difficult_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Practice Difficult Words (${difficultWords.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // Level Filter Chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("ALL", "A1", "A2", "B1", "Custom").forEach { lv ->
                        val isSelected = selectedLevelFilter == lv
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLevelFilter = lv },
                            label = { Text(lv, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            if (difficultWords.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🎉", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No difficult words!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Play practice rounds; mistakes will appear here for review.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(difficultWords.size) { index ->
                    val word = difficultWords[index]
                    val stat = wordStats[word.id]
                    WordCard(
                        word = word,
                        isFavorite = favorites.contains(word.id),
                        accuracy = stat?.accuracy,
                        tier = stat?.tier,
                        onPronounce = { audioManager.speakGerman(word.de, settings.ttsSpeed, settings.ttsPitch) },
                        onToggleFavorite = { repository.toggleFavorite(word.id) }
                    )
                }
            }
        }
    }
}
