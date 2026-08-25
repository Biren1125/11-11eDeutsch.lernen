package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
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
fun SearchScreen(
    repository: DeutschRepository,
    audioManager: AudioAndTtsManager,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val favorites by repository.favorites.collectAsState()
    val settings by repository.userSettings.collectAsState()

    val searchResults: List<Word> = remember(query) {
        if (query.isBlank()) {
            emptyList<Word>()
        } else {
            val q = query.trim().lowercase()
            repository.allWords.filter {
                it.de.lowercase().contains(q) || it.en.lowercase().contains(q) || it.plural?.lowercase()?.contains(q) == true
            }.take(50)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search German or English...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotBlank()) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                            .testTag("search_input_field")
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
            if (query.isBlank()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔍", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Search Dictionary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Search 2,400+ German vocabulary words across all levels.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else if (searchResults.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp)
                    ) {
                        Text(
                            text = "No words matched \"$query\"",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                item {
                    Text(
                        text = "Found ${searchResults.size} results",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(searchResults.size) { index ->
                    val word = searchResults[index]
                    WordCard(
                        word = word,
                        isFavorite = favorites.contains(word.id),
                        onPronounce = { audioManager.speakGerman(word.de, settings.ttsSpeed, settings.ttsPitch) },
                        onToggleFavorite = { repository.toggleFavorite(word.id) }
                    )
                }
            }
        }
    }
}
