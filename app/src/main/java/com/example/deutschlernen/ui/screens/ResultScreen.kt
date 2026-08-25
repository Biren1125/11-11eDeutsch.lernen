package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.audio.AudioAndTtsManager
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.model.Achievement
import com.example.deutschlernen.model.Word
import com.example.deutschlernen.ui.components.GermanFlagRibbon
import com.example.deutschlernen.ui.components.WordCard
import com.example.deutschlernen.ui.theme.*

@Composable
fun ResultScreen(
    score: Int,
    correctCount: Int,
    wrongCount: Int,
    durationMs: Long,
    mistakes: List<Word>,
    correctWords: List<Word>,
    isDaily: Boolean,
    levelLabel: String,
    repository: DeutschRepository,
    audioManager: AudioAndTtsManager,
    onPlayAgain: () -> Unit,
    onPracticeMistakes: (List<Word>) -> Unit,
    onGoHome: () -> Unit
) {
    val total = correctCount + wrongCount
    val accuracy = if (total > 0) ((correctCount.toFloat() / total) * 100).toInt() else 100
    val perfect = wrongCount == 0 && correctCount > 0
    val settings by repository.userSettings.collectAsState()
    val favorites by repository.favorites.collectAsState()

    var sessionResults by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        if (perfect) {
            audioManager.playVictory(settings.soundEnabled, settings.soundVolume)
        }
        val result = repository.finishSession(
            mode = com.example.deutschlernen.model.GameMode.MCQ,
            score = score,
            correct = correctCount,
            wrong = wrongCount,
            durationMs = durationMs,
            isDaily = isDaily,
            levelLabel = levelLabel
        )
        sessionResults = result
    }

    val xpGained = sessionResults?.get("xpGain") as? Int ?: (correctCount * 8 + (if (perfect) 60 else 0) + score / 20)
    val dailyBonus = sessionResults?.get("dailyBonus") as? Int ?: 0
    val newAchievements = sessionResults?.get("newAchievements") as? List<Achievement> ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Summary Header Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            ) {
                Column {
                    GermanFlagRibbon()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = if (perfect) "🏆" else if (accuracy >= 80) "🌟" else if (accuracy >= 50) "👍" else "🌱",
                            fontSize = 44.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (perfect) "Perfect Game!" else if (accuracy >= 80) "Great Practice!" else "Round Complete",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = levelLabel.ifBlank { "German Vocabulary Game" },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Key Stats Grid
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(vertical = 14.dp)
                        ) {
                            StatSummaryItem(label = "Score", value = "$score")
                            StatSummaryItem(label = "Accuracy", value = "$accuracy%", color = if (accuracy >= 80) GreenSuccess else GoldPrimary)
                            StatSummaryItem(label = "XP Gained", value = "+$xpGained", color = GoldPrimary)
                            StatSummaryItem(label = "Time", value = "${durationMs / 1000}s")
                        }

                        if (dailyBonus > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔥 Daily Challenge Bonus: +$dailyBonus XP!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Newly Unlocked Achievements
        if (newAchievements.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🎉 Achievement Unlocked!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GoldPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        newAchievements.forEach { ach ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Text(text = ach.icon, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = ach.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = ach.desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (mistakes.isNotEmpty()) {
                    Button(
                        onClick = { onPracticeMistakes(mistakes) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RedMistake),
                        modifier = Modifier.fillMaxWidth().testTag("practice_mistakes_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🎯 Practice ${mistakes.size} Mistakes", fontWeight = FontWeight.Bold, color = InkBlack)
                    }
                }

                Button(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().testTag("play_again_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Play Next Round", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                OutlinedButton(
                    onClick = onGoHome,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("go_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Return to Home")
                }
            }
        }

        // Mistakes Review List
        if (mistakes.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text(
                        text = "Words to Review (${mistakes.size})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            items(mistakes.size) { index ->
                val word = mistakes[index]
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

@Composable
fun StatSummaryItem(
    label: String,
    value: String,
    color: Color = Color.Unspecified
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (color != Color.Unspecified) color else MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
