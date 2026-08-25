package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.ui.components.GermanFlagRibbon
import com.example.deutschlernen.ui.theme.GoldPrimary
import com.example.deutschlernen.ui.theme.GreenSuccess
import com.example.deutschlernen.ui.theme.RedMistake
import com.example.deutschlernen.ui.theme.TealAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    repository: DeutschRepository,
    onBack: () -> Unit
) {
    val totalXp by repository.totalXp.collectAsState()
    val streak by repository.streak.collectAsState()
    val longestStreak by repository.longestStreak.collectAsState()
    val wordStats by repository.wordStats.collectAsState()
    val chapterProgress by repository.chapterProgress.collectAsState()
    val history by repository.history.collectAsState()

    val level = repository.levelFromXp(totalXp)
    val totalPracticed = wordStats.size
    val totalMastered = wordStats.values.count { it.mastered }
    val dueForReview = wordStats.values.count { it.nextReview != null && System.currentTimeMillis() >= it.nextReview!! }
    val totalCorrect = repository.totalCorrect
    val totalWrong = repository.totalWrong
    val overallAccuracy = if (totalCorrect + totalWrong > 0) ((totalCorrect.toFloat() / (totalCorrect + totalWrong)) * 100).toInt() else 100

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Learning Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // Overview Hero
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Column {
                        GermanFlagRibbon()
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Overall Analytics",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProgressStatBox(label = "Level", value = "$level", emoji = "⚡", color = GoldPrimary)
                                ProgressStatBox(label = "XP Points", value = "$totalXp", emoji = "✨", color = TealAccent)
                                ProgressStatBox(label = "Streak", value = "$streak d", emoji = "🔥", color = GoldPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                ProgressStatBox(label = "Words Mastered", value = "$totalMastered", emoji = "👑", color = GreenSuccess)
                                ProgressStatBox(label = "Practiced", value = "$totalPracticed", emoji = "📚", color = TealAccent)
                                ProgressStatBox(label = "Accuracy", value = "$overallAccuracy%", emoji = "🎯", color = if (overallAccuracy >= 80) GreenSuccess else GoldPrimary)
                            }
                        }
                    }
                }
            }

            // Spaced Repetition Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spaced Repetition System",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "⏰ Due for review now", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "$dueForReview words", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "👑 Mastered words", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "$totalMastered words", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "⚔️ Correct vs Wrong answers", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(text = "$totalCorrect / $totalWrong", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Level Progress Breakdown
            item {
                Text(
                    text = "Mastery by Level",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(listOf("A1", "A2", "B1").size) { index ->
                val lv = listOf("A1", "A2", "B1")[index]
                val allChapters = repository.allChaptersFor(lv)
                val completedChapters = allChapters.count { ch ->
                    chapterProgress[repository.chapterProgressKey(lv, ch.chapter)]?.completed == true
                }
                val ratio = if (allChapters.isNotEmpty()) completedChapters.toFloat() / allChapters.size else 0f

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Level $lv",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$completedChapters / ${allChapters.size} Chapters (${(ratio * 100).toInt()}%)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressStatBox(
    label: String,
    value: String,
    emoji: String,
    color: androidx.compose.ui.graphics.Color
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.width(96.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
