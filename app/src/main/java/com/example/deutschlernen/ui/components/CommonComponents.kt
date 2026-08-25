package com.example.deutschlernen.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.model.PrivilegedRole
import com.example.deutschlernen.model.Word
import com.example.deutschlernen.ui.theme.*

@Composable
fun GermanFlagRibbon(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GermanBlack))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GermanRed))
        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(GermanGold))
    }
}

@Composable
fun HeartMeter(
    hearts: Int,
    maxHearts: Int = 5,
    hasUnlimitedHearts: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .testTag("hearts_pill")
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            if (hasUnlimitedHearts) {
                Text(
                    text = "❤️",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "∞",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                for (i in 1..maxHearts) {
                    val isFilled = i <= hearts
                    Text(
                        text = if (isFilled) "❤️" else "🖤",
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun XpBadge(
    xp: Int,
    level: Int,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .testTag("xp_badge")
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "⚡",
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Lv.$level",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "$xp XP",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun RoleBadgePill(role: PrivilegedRole, modifier: Modifier = Modifier) {
    val badgeColor = when (role.role) {
        "developer" -> GoldPrimary
        "coDeveloper" -> Color(0xFFFF80AB)
        "elite" -> TealAccent
        else -> Color(0xFF64B5F6)
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = badgeColor.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = role.title.ifBlank { role.label },
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = badgeColor
            )
        }
    }
}

@Composable
fun WordArticleChip(article: String?) {
    if (article.isNullOrBlank()) return
    val (chipBg, chipFg) = when (article.lowercase().trim()) {
        "der" -> Color(0xFF1E88E5).copy(alpha = 0.2f) to Color(0xFF64B5F6)
        "die" -> Color(0xFFE91E63).copy(alpha = 0.2f) to Color(0xFFF48FB1)
        "das" -> Color(0xFF43A047).copy(alpha = 0.2f) to Color(0xFF81C784)
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = chipBg,
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Text(
            text = article,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = chipFg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun WordCard(
    word: Word,
    isFavorite: Boolean,
    onPronounce: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    accuracy: Int? = null,
    tier: String? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    WordArticleChip(word.article)
                    Text(
                        text = word.de,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPronounce,
                        modifier = Modifier.size(36.dp).testTag("pronounce_${word.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Pronounce",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp).testTag("fav_${word.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = word.en,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!word.plural.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Plural: ${word.plural}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (!word.ex.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "“${word.ex}”",
                            fontSize = 13.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!word.exEn.isNullOrBlank()) {
                            Text(
                                text = word.exEn,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (accuracy != null || tier != null || word.pos != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (word.pos != null) {
                        Text(
                            text = word.pos.replace("-", " "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (accuracy != null) {
                        val tierColor = when (tier) {
                            "high" -> RedMistake
                            "med" -> GoldPrimary
                            else -> GreenSuccess
                        }
                        Text(
                            text = "Accuracy: $accuracy%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tierColor
                        )
                    }
                }
            }
        }
    }
}
