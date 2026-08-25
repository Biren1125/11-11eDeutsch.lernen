package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.ui.components.GermanFlagRibbon
import com.example.deutschlernen.ui.components.RoleBadgePill
import com.example.deutschlernen.ui.theme.*

@Composable
fun HomeScreen(
    repository: DeutschRepository,
    onNavigateLevel: (String) -> Unit,
    onNavigateRandomPractice: () -> Unit,
    onNavigateDifficult: () -> Unit,
    onNavigateProgress: () -> Unit,
    onNavigateFavorites: () -> Unit,
    onNavigateDaily: () -> Unit,
    onNavigateLeaderboard: () -> Unit,
    onNavigateCustomWords: () -> Unit,
    onNavigateProfile: () -> Unit,
    onNavigateAbout: () -> Unit
) {
    val streak by repository.streak.collectAsState()
    val longestStreak by repository.longestStreak.collectAsState()
    val totalXp by repository.totalXp.collectAsState()
    val currentRole by repository.currentRole.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val customWords by repository.customWords.collectAsState()
    val chapterProgress by repository.chapterProgress.collectAsState()

    val level = repository.levelFromXp(totalXp)
    val nextXp = repository.xpForLevel(level + 1)
    val currentLevelBaseXp = repository.xpForLevel(level)
    val xpProgress = if (nextXp > currentLevelBaseXp) {
        ((totalXp - currentLevelBaseXp).toFloat() / (nextXp - currentLevelBaseXp)).coerceIn(0f, 1f)
    } else 1f

    val levels = listOf(
        LevelItem("A1", "Beginner", "🌱", "8 chapters · 925 words", Color(0xFF43A047)),
        LevelItem("A2", "Elementary", "📗", "8 chapters · 590 words", Color(0xFF1E88E5)),
        LevelItem("B1", "Intermediate", "⚔️", "8 chapters · 944 words", Color(0xFF8E24AA)),
        LevelItem("B2", "Upper-Intermediate", "👑", "Coming soon", Color(0xFFD6B05E))
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
    ) {
        // Hero Header
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "11:11e Deutsch Lernen",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Master German vocabulary step-by-step",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(text = "🇩🇪", fontSize = 28.sp)
                        }

                        if (currentRole != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            RoleBadgePill(role = currentRole!!)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Streak and XP strip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔥", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "$streak Day Streak",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Best: $longestStreak days",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Level $level",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "$totalXp / $nextXp XP",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { xpProgress },
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

        // Daily Challenge Hero Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateDaily() }
                    .border(
                        1.dp,
                        Brush.horizontalGradient(listOf(GoldPrimary.copy(alpha = 0.8f), TealAccent.copy(alpha = 0.8f))),
                        RoundedCornerShape(16.dp)
                    )
                    .testTag("daily_challenge_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f))
                    ) {
                        Text(text = "⚡", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Challenge",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "10 seeded words · Earn bonus XP & streak",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Level Selection
        item {
            Text(
                text = "Course Levels",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(levels.size) { index ->
            val lv = levels[index]
            val isB2 = lv.key == "B2"
            val totalChapters = repository.allChaptersFor(lv.key).size
            val completedChapters = (1..totalChapters).count { ch ->
                chapterProgress[repository.chapterProgressKey(lv.key, ch)]?.completed == true
            }
            val progressRatio = if (totalChapters > 0) completedChapters.toFloat() / totalChapters else 0f

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isB2) { onNavigateLevel(lv.key) }
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .testTag("level_${lv.key}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(lv.color.copy(alpha = 0.2f))
                            ) {
                                Text(text = lv.stamp, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lv.key,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "· ${lv.name}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = lv.subtitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!isB2) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Open",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "SOON",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!isB2 && totalChapters > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Progress: $completedChapters / $totalChapters chapters completed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(progressRatio * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = lv.color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Hub Grid
        item {
            Text(
                text = "Practice Hub",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubButton(
                        icon = "🎲",
                        title = "Random (All)",
                        subtitle = "Mix from all levels",
                        onClick = onNavigateRandomPractice,
                        modifier = Modifier.weight(1f).testTag("hub_random")
                    )
                    HubButton(
                        icon = "🎯",
                        title = "Difficult Words",
                        subtitle = "Focus on mistakes",
                        onClick = onNavigateDifficult,
                        modifier = Modifier.weight(1f).testTag("hub_difficult")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubButton(
                        icon = "⭐",
                        title = "Favorites",
                        subtitle = "${favorites.size} saved words",
                        onClick = onNavigateFavorites,
                        modifier = Modifier.weight(1f).testTag("hub_favorites")
                    )
                    HubButton(
                        icon = "📊",
                        title = "Progress",
                        subtitle = "Analytics & Mastery",
                        onClick = onNavigateProgress,
                        modifier = Modifier.weight(1f).testTag("hub_progress")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubButton(
                        icon = "📝",
                        title = "My Vocabulary",
                        subtitle = "${customWords.size} custom words",
                        onClick = onNavigateCustomWords,
                        modifier = Modifier.weight(1f).testTag("hub_custom")
                    )
                    HubButton(
                        icon = "🏆",
                        title = "Leaderboard",
                        subtitle = "Daily top scores",
                        onClick = onNavigateLeaderboard,
                        modifier = Modifier.weight(1f).testTag("hub_leaderboard")
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HubButton(
                        icon = "👤",
                        title = "Profile & Settings",
                        subtitle = "Developer console",
                        onClick = onNavigateProfile,
                        modifier = Modifier.weight(1f).testTag("hub_profile")
                    )
                    HubButton(
                        icon = "✨",
                        title = "About App",
                        subtitle = "Story & Dedication",
                        onClick = onNavigateAbout,
                        modifier = Modifier.weight(1f).testTag("hub_about")
                    )
                }
            }
        }
    }
}

data class LevelItem(
    val key: String,
    val name: String,
    val stamp: String,
    val subtitle: String,
    val color: Color
)

@Composable
fun HubButton(
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
