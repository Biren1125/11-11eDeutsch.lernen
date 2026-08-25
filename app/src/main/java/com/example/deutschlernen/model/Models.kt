package com.example.deutschlernen.model

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: String,
    val de: String,
    val en: String,
    val pos: String? = null,
    val article: String? = null,
    val gender: String? = null,
    val plural: String? = null,
    val ex: String? = null,
    val exEn: String? = null,
    val page: Int? = null,
    val level: String = "",
    val chapter: Int = 1,
    val chapterTitle: String = ""
)

@Serializable
data class Chapter(
    val chapter: Int,
    val title: String,
    val sourceBook: String? = null,
    val words: List<Word> = emptyList()
)

@Serializable
data class WordStat(
    val id: String,
    var seen: Int = 0,
    var correct: Int = 0,
    var wrong: Int = 0,
    var streakCorrect: Int = 0,
    var streakWrong: Int = 0,
    var difficulty: Float = 1.0f,
    var lastSeen: Long? = null,
    var nextReview: Long? = null,
    var mastered: Boolean = false
) {
    val accuracy: Int
        get() = if (seen > 0) ((correct.toFloat() / seen) * 100).toInt() else 100

    val tier: String
        get() = when {
            difficulty >= 3.5f -> "high"
            difficulty >= 1.2f -> "med"
            else -> "low"
        }
}

@Serializable
data class ChapterProgress(
    var completedIds: List<String> = emptyList(),
    var currentBatchIds: List<String> = emptyList(),
    var completed: Boolean = false
)

@Serializable
data class GameHistoryItem(
    val mode: String,
    val score: Int,
    val accuracy: Int,
    val date: Long,
    val perfect: Boolean,
    val level: String
)

@Serializable
data class Achievement(
    val id: String,
    val icon: String,
    val name: String,
    val desc: String,
    var unlocked: Boolean = false,
    var unlockedAt: String? = null
)

@Serializable
data class PrivilegedRole(
    val id: String,
    val role: String,
    val title: String,
    val label: String = "",
    val targetPlayerId: String = "",
    val unlimitedHearts: Boolean = true,
    val allUnlocked: Boolean = true,
    val badgeColor: String = "#D6B05E",
    val grantedAt: Long = 0L
)

@Serializable
data class DailyLeaderboardEntry(
    val id: String,
    val name: String,
    val score: Int,
    val accuracy: Int,
    val streak: Int,
    val date: String
)

@Serializable
data class UserSettings(
    var playerName: String = "Learner",
    var playerId: String = "",
    var darkTheme: Boolean = true,
    var accentColorHex: String = "#D6B05E",
    var soundEnabled: Boolean = true,
    var soundVolume: Float = 0.8f,
    var soundStyle: String = "chime", // chime, piano, soft
    var ttsSpeed: Float = 1.0f,
    var ttsPitch: Float = 1.0f,
    var streakReminders: Boolean = false
)

enum class GameMode(val id: String, val title: String, val icon: String, val desc: String) {
    MEMORY("memory", "Memory Match", "🃏", "Flip cards to match German & English"),
    DE2EN("de2en", "German → English", "🇩🇪", "Pick the right English meaning"),
    EN2DE("en2de", "English → German", "🇬🇧", "Pick the right German word"),
    MCQ("mcq", "Multiple Choice", "❓", "Mixed-direction vocabulary quiz"),
    PAIRS("pairs", "Word Pairs", "🔗", "Tap matching pairs in two columns");

    companion object {
        fun fromId(id: String): GameMode = entries.find { it.id == id } ?: MCQ
    }
}

enum class GameDifficulty(val id: String, val label: String, val pairsCount: Int, val mcqCount: Int, val secondsPerWord: Int) {
    EASY("easy", "Easy", 6, 8, 14),
    NORMAL("normal", "Normal", 8, 10, 10),
    HARD("hard", "Hard", 10, 12, 7);

    fun calculateTotalSeconds(wordCount: Int): Int {
        val base = when (this) {
            EASY -> 30
            NORMAL -> 20
            HARD -> 12
        }
        return base + wordCount * secondsPerWord
    }

    companion object {
        fun fromId(id: String): GameDifficulty = entries.find { it.id == id } ?: NORMAL
    }
}

data class ChoiceOption(
    val wordId: String,
    val text: String,
    var isSelected: Boolean = false,
    var isCorrect: Boolean? = null
)

data class MemoryCard(
    val cardId: String,
    val wordId: String,
    val text: String,
    val side: String, // "de" or "en"
    var isFlipped: Boolean = false,
    var isMatched: Boolean = false
)

data class PairItem(
    val wordId: String,
    val text: String,
    val side: String, // "de" or "en"
    var isSelected: Boolean = false,
    var isMatched: Boolean = false,
    var isError: Boolean = false
)
