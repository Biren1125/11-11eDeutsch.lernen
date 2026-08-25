package com.example.deutschlernen.data

import android.content.Context
import android.content.SharedPreferences
import com.example.deutschlernen.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class DeutschRepository(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("deutsch_lernen_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    // In-memory Database loaded from JSON
    private val _vocabDb = mutableMapOf<String, List<Chapter>>()
    val vocabDb: Map<String, List<Chapter>> get() = _vocabDb

    // State flows
    private val _heartsCount = MutableStateFlow(5)
    val heartsCount: StateFlow<Int> = _heartsCount.asStateFlow()

    private val _totalXp = MutableStateFlow(0)
    val totalXp: StateFlow<Int> = _totalXp.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _longestStreak = MutableStateFlow(0)
    val longestStreak: StateFlow<Int> = _longestStreak.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _wordStats = MutableStateFlow<Map<String, WordStat>>(emptyMap())
    val wordStats: StateFlow<Map<String, WordStat>> = _wordStats.asStateFlow()

    private val _chapterProgress = MutableStateFlow<Map<String, ChapterProgress>>(emptyMap())
    val chapterProgress: StateFlow<Map<String, ChapterProgress>> = _chapterProgress.asStateFlow()

    private val _customWords = MutableStateFlow<List<Word>>(emptyList())
    val customWords: StateFlow<List<Word>> = _customWords.asStateFlow()

    private val _userSettings = MutableStateFlow(UserSettings())
    val userSettings: StateFlow<UserSettings> = _userSettings.asStateFlow()

    private val _currentRole = MutableStateFlow<PrivilegedRole?>(null)
    val currentRole: StateFlow<PrivilegedRole?> = _currentRole.asStateFlow()

    private val _achievementsMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _history = MutableStateFlow<List<GameHistoryItem>>(emptyList())
    val history: StateFlow<List<GameHistoryItem>> = _history.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<DailyLeaderboardEntry>>(emptyList())
    val leaderboard: StateFlow<List<DailyLeaderboardEntry>> = _leaderboard.asStateFlow()

    // Game stats
    var gamesPlayed: Int = 0
    var gamesCompleted: Int = 0
    var perfectGames: Int = 0
    var bestScore: Int = 0
    var bestTimeMs: Long? = null
    var wordsMastered: Int = 0
    var totalCorrect: Int = 0
    var totalWrong: Int = 0

    private var lastHeartRefillAt: Long = System.currentTimeMillis()
    private var lastActiveDay: String = ""
    private var dailyChallengeLastDate: String = ""
    private var dailyChallengeStreak: Int = 0

    val MAX_HEARTS = 5
    val HEART_REFILL_MS = 2 * 60 * 1000L // 2 minutes

    private val defaultAchievementTemplates = listOf(
        Achievement("first_game", "🌱", "Beginner", "Complete your first game"),
        Achievement("words_100", "📚", "Vocabulary Novice", "Master 100 words"),
        Achievement("words_500", "🎓", "Vocabulary Master", "Master 500 words"),
        Achievement("words_1000", "👑", "German Prodigy", "Master 1,000 words"),
        Achievement("perfect_1", "⭐", "Sharp Eye", "Finish a game with 100% accuracy"),
        Achievement("perfect_10", "🌟", "Flawless Scholar", "Complete 10 perfect games"),
        Achievement("streak_7", "🔥", "Week-Long Fire", "Maintain a 7-day streak"),
        Achievement("streak_30", "⚡", "German Devotee", "Maintain a 30-day streak")
    )

    init {
        loadVocabularyFromAssets()
        loadState()
        refillHeartsIfNeeded()
        checkStreakLiveness()
        refreshAchievementsList()
    }

    val allWords: List<Word>
        get() {
            val list = mutableListOf<Word>()
            for (lv in listOf("A1", "A2", "B1", "B2")) {
                list.addAll(wordsByLevel(lv))
            }
            list.addAll(_customWords.value)
            return list
        }

    fun wordsByLevel(level: String): List<Word> {
        val list = mutableListOf<Word>()
        for (ch in allChaptersFor(level)) {
            list.addAll(ch.words)
        }
        return list
    }

    fun wordsDaily(): List<Word> = getDailyChallengeWords()

    fun wordsFavorites(): List<Word> {
        val favs = _favorites.value
        return allWords.filter { favs.contains(it.id) }
    }

    fun wordsDifficult(levelKey: String? = null): List<Word> {
        val pool = if (levelKey != null && levelKey != "ALL" && levelKey != "Custom") {
            wordsByLevel(levelKey)
        } else if (levelKey == "Custom") {
            _customWords.value
        } else {
            allWords
        }
        return pool.filter { w ->
            val s = _wordStats.value[w.id]
            s != null && s.seen > 0 && s.tier != "low"
        }.sortedByDescending { _wordStats.value[it.id]?.difficulty ?: 0f }
    }

    fun allChaptersFor(levelKey: String): List<Chapter> = _vocabDb[levelKey] ?: emptyList()

    fun chapterWords(level: String, chapterNum: Int): List<Word> {
        val ch = allChaptersFor(level).find { it.chapter == chapterNum } ?: return emptyList()
        return ch.words
    }

    fun chapterWordsToPractice(level: String, chapterNum: Int, batchSize: Int = 15): List<Word> {
        val pool = chapterWords(level, chapterNum)
        if (pool.isEmpty()) return emptyList()
        val progress = getChapterProgress(level, chapterNum)
        val completedSet = progress.completedIds.toSet()
        val newWords = pool.filter { !completedSet.contains(it.id) }
        val reviewWords = pool.filter { w ->
            if (!completedSet.contains(w.id)) return@filter false
            val s = _wordStats.value[w.id]
            s != null && (s.streakWrong > 0 || (s.nextReview != null && System.currentTimeMillis() >= s.nextReview!!))
        }

        val neededBatchSize = min(batchSize, pool.size)
        val reviewBatch = weightedSample(reviewWords, min(neededBatchSize, reviewWords.size)) { it.id }
        val selectedSet = reviewBatch.map { it.id }.toSet()
        val newFillPool = newWords.filter { !selectedSet.contains(it.id) }
        val newFill = weightedSample(newFillPool, neededBatchSize - reviewBatch.size) { it.id }
        val finalBatch = (reviewBatch + newFill).ifEmpty { pool.take(neededBatchSize) }
        return finalBatch.shuffled()
    }

    fun chapterProgressKey(level: String, chapter: Int): String = "$level-$chapter"

    fun getChapterProgress(level: String, chapter: Int): ChapterProgress {
        val key = chapterProgressKey(level, chapter)
        val map = _chapterProgress.value.toMutableMap()
        if (!map.containsKey(key)) {
            map[key] = ChapterProgress()
            _chapterProgress.value = map
        }
        return map[key]!!
    }

    fun isChapterUnlocked(level: String, chapter: Int): Boolean {
        if (hasUnlimitedHearts() || _currentRole.value?.allUnlocked == true) return true
        if (chapter <= 1) return true
        val prev = getChapterProgress(level, chapter - 1)
        return prev.completed
    }

    fun advanceChapterBatch(level: String, chapter: Int, currentWords: List<Word>) {
        val progress = getChapterProgress(level, chapter)
        val pool = chapterWords(level, chapter)
        val completedSet = progress.completedIds.toMutableSet()
        completedSet.addAll(currentWords.map { it.id })
        progress.completedIds = completedSet.toList()
        progress.currentBatchIds = emptyList()

        if (pool.isNotEmpty() && progress.completedIds.size >= pool.size) {
            progress.completed = true
        }
        val map = _chapterProgress.value.toMutableMap()
        map[chapterProgressKey(level, chapter)] = progress
        _chapterProgress.value = map
        saveState()
    }

    private fun loadVocabularyFromAssets() {
        try {
            val jsonString = context.assets.open("vocab_db.json").bufferedReader().use { it.readText() }
            val rootObj = json.parseToJsonElement(jsonString) as? JsonObject
            if (rootObj != null) {
                for ((levelKey, chaptersElement) in rootObj) {
                    val chaptersList = mutableListOf<Chapter>()
                    val chaptersArray = chaptersElement.jsonArray
                    for (chElem in chaptersArray) {
                        try {
                            val chapter = json.decodeFromJsonElement<Chapter>(chElem)
                            val normalizedWords = chapter.words.map { w ->
                                w.copy(
                                    level = levelKey,
                                    chapter = chapter.chapter,
                                    chapterTitle = chapter.title
                                )
                            }
                            chaptersList.add(chapter.copy(words = normalizedWords))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _vocabDb[levelKey] = chaptersList
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun todayKey(): String = todayString()

    fun todayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun levelFromXp(xp: Int): Int {
        var level = 1
        while (xp >= xpForLevel(level + 1)) {
            level++
        }
        return level
    }

    fun xpForLevel(level: Int): Int {
        if (level <= 1) return 0
        return ((level - 1).toDouble().pow(1.5) * 120).roundToInt()
    }

    fun hasUnlimitedHearts(): Boolean = _currentRole.value?.unlimitedHearts == true

    fun loseHeart(): Int {
        if (hasUnlimitedHearts()) return MAX_HEARTS
        val current = max(0, _heartsCount.value - 1)
        _heartsCount.value = current
        saveState()
        return current
    }

    fun refillHeartsIfNeeded() {
        if (hasUnlimitedHearts()) {
            _heartsCount.value = MAX_HEARTS
            return
        }
        val now = System.currentTimeMillis()
        val elapsed = now - lastHeartRefillAt
        val heartsToAdd = (elapsed / HEART_REFILL_MS).toInt()
        if (heartsToAdd > 0) {
            val updated = min(MAX_HEARTS, _heartsCount.value + heartsToAdd)
            _heartsCount.value = updated
            lastHeartRefillAt = now
            saveState()
        }
    }

    fun addXp(amount: Int) {
        _totalXp.value += amount
        saveState()
    }

    private fun checkStreakLiveness() {
        val today = todayString()
        if (lastActiveDay.isNotEmpty() && lastActiveDay != today) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val lastDate = sdf.parse(lastActiveDay)
                val nowDate = sdf.parse(today)
                if (lastDate != null && nowDate != null) {
                    val diffDays = ((nowDate.time - lastDate.time) / (1000 * 60 * 60 * 24)).toInt()
                    if (diffDays > 1) {
                        _streak.value = 0
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        saveState()
    }

    fun markActiveToday() {
        val today = todayString()
        if (lastActiveDay != today) {
            val newStreak = _streak.value + 1
            _streak.value = newStreak
            _longestStreak.value = max(_longestStreak.value, newStreak)
            lastActiveDay = today
            saveState()
        }
    }

    fun toggleFavorite(wordId: String) {
        val favs = _favorites.value.toMutableSet()
        if (favs.contains(wordId)) {
            favs.remove(wordId)
        } else {
            favs.add(wordId)
        }
        _favorites.value = favs
        saveState()
    }

    fun recordAnswer(wordId: String, isCorrect: Boolean): WordStat {
        val stats = _wordStats.value.toMutableMap()
        val s = stats[wordId] ?: WordStat(id = wordId)
        s.seen += 1
        s.lastSeen = System.currentTimeMillis()
        if (isCorrect) {
            s.correct += 1
            s.streakCorrect += 1
            s.streakWrong = 0
            val reduction = when {
                s.streakCorrect >= 5 -> 0.6f
                s.streakCorrect >= 3 -> 0.4f
                s.streakCorrect >= 1 -> 0.25f
                else -> 0.0f
            }
            s.difficulty = max(0f, s.difficulty - reduction)
            val hours = listOf(1, 4, 12, 24, 72, 168, 336)[min(s.streakCorrect, 6)]
            s.nextReview = System.currentTimeMillis() + hours * 3600 * 1000L
            totalCorrect += 1
        } else {
            s.wrong += 1
            s.streakWrong += 1
            s.streakCorrect = 0
            s.difficulty = min(6.0f, s.difficulty + 1.5f)
            s.nextReview = System.currentTimeMillis() + 30 * 60 * 1000L // 30 mins
            totalWrong += 1
        }

        if (s.seen >= 3 && (s.correct.toFloat() / s.seen) >= 0.8f && s.difficulty < 0.5f) {
            if (!s.mastered) {
                s.mastered = true
                wordsMastered += 1
            }
        }
        stats[wordId] = s
        _wordStats.value = stats
        saveState()
        return s
    }

    fun wordWeight(wordId: String): Float {
        val s = _wordStats.value[wordId] ?: return 3.0f
        if (s.seen == 0) return 3.0f
        var w = 1.0f + s.difficulty * 2.2f
        if (s.nextReview != null && System.currentTimeMillis() >= s.nextReview!!) {
            w *= 1.6f
        }
        return max(0.35f, w)
    }

    fun <T> weightedSample(items: List<T>, count: Int, idFn: (T) -> String): List<T> {
        if (items.isEmpty()) return emptyList()
        val pool = items.map { it to (wordWeight(idFn(it)) + Random.nextFloat() * 0.75f) }.toMutableList()
        pool.sortByDescending { it.second }
        val sliceSize = min(pool.size, max(count * 2, count + 4))
        val top = pool.take(sliceSize).map { it.first }.shuffled()
        return top.take(min(count, top.size))
    }

    fun addCustomWords(rawText: String): Int {
        val seen = mutableSetOf<String>()
        val parsed = mutableListOf<Word>()
        val existing = _customWords.value.map { "${it.de.lowercase()}|${it.en.lowercase()}" }.toSet()

        rawText.lines().forEachIndexed { index, line ->
            val clean = line.trim()
            if (clean.isNotEmpty() && !clean.startsWith("#")) {
                val parts = clean.split(Regex("\\t|;|\\||=|\\s+[–—-]+\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size >= 2) {
                    val de = parts[0]
                    val en = parts.drop(1).joinToString(" — ")
                    val key = "${de.lowercase()}|${en.lowercase()}"
                    if (!seen.contains(key) && !existing.contains(key)) {
                        seen.add(key)
                        parsed.add(
                            Word(
                                id = "custom-${System.currentTimeMillis()}-$index-${Random.nextInt(1000, 9999)}",
                                de = de,
                                en = en,
                                pos = "custom",
                                level = "Custom",
                                chapter = 1,
                                chapterTitle = "My Vocabulary"
                            )
                        )
                    }
                }
            }
        }

        if (parsed.isNotEmpty()) {
            _customWords.value = _customWords.value + parsed
            saveState()
        }
        return parsed.size
    }

    fun removeCustomWord(wordId: String) {
        _customWords.value = _customWords.value.filter { it.id != wordId }
        saveState()
    }

    private fun refreshAchievementsList() {
        val unlockedMap = _achievementsMap.value
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        _achievements.value = defaultAchievementTemplates.map { ach ->
            val timestamp = unlockedMap[ach.id]
            ach.copy(
                unlocked = timestamp != null,
                unlockedAt = timestamp?.let { sdf.format(Date(it)) }
            )
        }
    }

    fun checkAchievements(): List<Achievement> {
        val newlyUnlocked = mutableListOf<Achievement>()
        val currentUnlocked = _achievementsMap.value.toMutableMap()
        val checks = mapOf<String, () -> Boolean>(
            "first_game" to { gamesCompleted >= 1 },
            "words_100" to { wordsMastered >= 100 },
            "words_500" to { wordsMastered >= 500 },
            "words_1000" to { wordsMastered >= 1000 },
            "perfect_1" to { perfectGames >= 1 },
            "perfect_10" to { perfectGames >= 10 },
            "streak_7" to { _longestStreak.value >= 7 },
            "streak_30" to { _longestStreak.value >= 30 }
        )

        for (ach in defaultAchievementTemplates) {
            if (!currentUnlocked.containsKey(ach.id)) {
                val isUnlocked = checks[ach.id]?.invoke() ?: false
                if (isUnlocked) {
                    val now = System.currentTimeMillis()
                    currentUnlocked[ach.id] = now
                    newlyUnlocked.add(ach.copy(unlocked = true, unlockedAt = "Today"))
                }
            }
        }

        if (newlyUnlocked.isNotEmpty()) {
            _achievementsMap.value = currentUnlocked
            refreshAchievementsList()
            saveState()
        }
        return newlyUnlocked
    }

    private fun dailyWordHash(value: String, seed: Long): Int {
        var h = seed.toInt()
        for (ch in value) {
            h = ((h shl 5) - h + ch.code)
        }
        return kotlin.math.abs(h)
    }

    fun getDailyChallengeWords(): List<Word> {
        val seed = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()).toLongOrNull() ?: 20260101L
        val all = allWords
        return all.sortedBy { dailyWordHash(it.id, seed) }.take(10)
    }

    fun recordDailyChallengeResult(score: Int, accuracy: Int, perfect: Boolean): Int {
        val today = todayString()
        var bonus = 0
        if (dailyChallengeLastDate != today) {
            val isConsecutive = try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val last = sdf.parse(dailyChallengeLastDate)
                val now = sdf.parse(today)
                if (last != null && now != null) {
                    (now.time - last.time) / (1000 * 60 * 60 * 24) == 1L
                } else false
            } catch (e: Exception) { false }

            dailyChallengeStreak = if (isConsecutive) dailyChallengeStreak + 1 else 1
            dailyChallengeLastDate = today
            bonus = 50 + min(dailyChallengeStreak, 30) * 10 + (if (perfect) 50 else 0)
        }

        val totalScore = score + bonus
        val playerName = _userSettings.value.playerName
        val entry = DailyLeaderboardEntry(
            id = "$playerName-$today",
            name = playerName,
            score = totalScore,
            accuracy = accuracy,
            streak = dailyChallengeStreak,
            date = today
        )
        val list = _leaderboard.value.toMutableList()
        val index = list.indexOfFirst { it.id == entry.id }
        if (index >= 0) {
            list[index] = entry
        } else {
            list.add(entry)
        }
        list.sortWith(compareByDescending<DailyLeaderboardEntry> { it.score }.thenByDescending { it.accuracy })
        _leaderboard.value = list.take(50)
        saveState()
        return bonus
    }

    fun finishSession(
        mode: GameMode,
        score: Int,
        correct: Int,
        wrong: Int,
        durationMs: Long,
        isDaily: Boolean,
        levelLabel: String
    ): Map<String, Any> {
        gamesPlayed += 1
        gamesCompleted += 1
        bestScore = max(bestScore, score)
        if (bestTimeMs == null || durationMs < bestTimeMs!!) {
            bestTimeMs = durationMs
        }
        val perfect = wrong == 0 && correct > 0
        if (perfect) perfectGames += 1

        val accuracy = if (correct + wrong > 0) ((correct.toFloat() / (correct + wrong)) * 100).toInt() else 100
        val dailyBonus = if (isDaily) recordDailyChallengeResult(score, accuracy, perfect) else 0
        val xpGain = correct * 8 + (if (perfect) 60 else 0) + (score / 20) + dailyBonus
        addXp(xpGain)
        markActiveToday()

        val historyItem = GameHistoryItem(
            mode = mode.id,
            score = score + dailyBonus,
            accuracy = accuracy,
            date = System.currentTimeMillis(),
            perfect = perfect,
            level = levelLabel
        )
        val hist = _history.value.toMutableList()
        hist.add(0, historyItem)
        _history.value = hist.take(50)

        val newlyUnlocked = checkAchievements()
        saveState()

        return mapOf(
            "xpGain" to xpGain,
            "dailyBonus" to dailyBonus,
            "perfect" to perfect,
            "accuracy" to accuracy,
            "newAchievements" to newlyUnlocked
        )
    }

    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun authenticateDeveloper(password: String): Boolean {
        if (password == "11:11e" || password == "admin1111" || password == "deutsch1111" || password == "deutsch") {
            assignRole(
                PrivilegedRole(
                    id = "dev",
                    role = "developer",
                    title = "11:11e Developer",
                    label = "Developer",
                    targetPlayerId = _userSettings.value.playerId,
                    unlimitedHearts = true,
                    allUnlocked = true,
                    badgeColor = "#D6B05E"
                )
            )
            return true
        }
        val hashed = hashPassword(password)
        if (hashed == "d15c7bb74bd56885dfb9e86a51d4faee9203a73c178f87edb2ad8ee5ff0b4df9") {
            assignRole(
                PrivilegedRole(
                    id = "dev",
                    role = "developer",
                    title = "11:11e Developer",
                    label = "Developer",
                    targetPlayerId = _userSettings.value.playerId,
                    unlimitedHearts = true,
                    allUnlocked = true,
                    badgeColor = "#D6B05E"
                )
            )
            return true
        }
        return false
    }

    fun assignRole(role: PrivilegedRole) {
        _currentRole.value = role
        saveState()
    }

    fun updateSettings(settings: UserSettings) {
        _userSettings.value = settings
        saveState()
    }

    fun resetAllProgress() {
        _heartsCount.value = 5
        _totalXp.value = 0
        _streak.value = 0
        _longestStreak.value = 0
        _favorites.value = emptySet()
        _wordStats.value = emptyMap()
        _chapterProgress.value = emptyMap()
        _customWords.value = emptyList()
        _achievementsMap.value = emptyMap()
        _history.value = emptyList()
        _leaderboard.value = emptyList()
        gamesPlayed = 0
        gamesCompleted = 0
        perfectGames = 0
        bestScore = 0
        wordsMastered = 0
        totalCorrect = 0
        totalWrong = 0
        lastActiveDay = ""
        dailyChallengeLastDate = ""
        dailyChallengeStreak = 0
        refreshAchievementsList()
        saveState()
    }

    private fun saveState() {
        prefs.edit().apply {
            putInt("hearts_count", _heartsCount.value)
            putLong("last_heart_refill", lastHeartRefillAt)
            putInt("total_xp", _totalXp.value)
            putInt("streak", _streak.value)
            putInt("longest_streak", _longestStreak.value)
            putString("last_active_day", lastActiveDay)
            putString("favorites", json.encodeToString(_favorites.value))
            putString("word_stats", json.encodeToString(_wordStats.value))
            putString("chapter_progress", json.encodeToString(_chapterProgress.value))
            putString("custom_words", json.encodeToString(_customWords.value))
            putString("user_settings", json.encodeToString(_userSettings.value))
            putString("current_role", json.encodeToString(_currentRole.value))
            putString("achievements", json.encodeToString(_achievementsMap.value))
            putString("history", json.encodeToString(_history.value))
            putString("daily_leaderboard", json.encodeToString(_leaderboard.value))
            putInt("games_played", gamesPlayed)
            putInt("games_completed", gamesCompleted)
            putInt("perfect_games", perfectGames)
            putInt("best_score", bestScore)
            putLong("best_time_ms", bestTimeMs ?: -1L)
            putInt("words_mastered", wordsMastered)
            putInt("total_correct", totalCorrect)
            putInt("total_wrong", totalWrong)
            putString("daily_challenge_date", dailyChallengeLastDate)
            putInt("daily_challenge_streak", dailyChallengeStreak)
            apply()
        }
    }

    private fun loadState() {
        var currentSettings = UserSettings()
        try {
            prefs.getString("user_settings", null)?.let {
                currentSettings = json.decodeFromString(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (currentSettings.playerId.isBlank()) {
            currentSettings.playerId = "P-" + UUID.randomUUID().toString().take(8).uppercase()
        }
        _userSettings.value = currentSettings

        _heartsCount.value = prefs.getInt("hearts_count", 5)
        lastHeartRefillAt = prefs.getLong("last_heart_refill", System.currentTimeMillis())
        _totalXp.value = prefs.getInt("total_xp", 0)
        _streak.value = prefs.getInt("streak", 0)
        _longestStreak.value = prefs.getInt("longest_streak", 0)
        lastActiveDay = prefs.getString("last_active_day", "") ?: ""

        try {
            prefs.getString("favorites", null)?.let {
                _favorites.value = json.decodeFromString(it)
            }
            prefs.getString("word_stats", null)?.let {
                _wordStats.value = json.decodeFromString(it)
            }
            prefs.getString("chapter_progress", null)?.let {
                _chapterProgress.value = json.decodeFromString(it)
            }
            prefs.getString("custom_words", null)?.let {
                _customWords.value = json.decodeFromString(it)
            }
            prefs.getString("current_role", null)?.let {
                _currentRole.value = json.decodeFromString(it)
            }
            prefs.getString("achievements", null)?.let {
                _achievementsMap.value = json.decodeFromString(it)
            }
            prefs.getString("history", null)?.let {
                _history.value = json.decodeFromString(it)
            }
            prefs.getString("daily_leaderboard", null)?.let {
                _leaderboard.value = json.decodeFromString(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        gamesPlayed = prefs.getInt("games_played", 0)
        gamesCompleted = prefs.getInt("games_completed", 0)
        perfectGames = prefs.getInt("perfect_games", 0)
        bestScore = prefs.getInt("best_score", 0)
        val bestTime = prefs.getLong("best_time_ms", -1L)
        bestTimeMs = if (bestTime > 0) bestTime else null
        wordsMastered = prefs.getInt("words_mastered", 0)
        totalCorrect = prefs.getInt("total_correct", 0)
        totalWrong = prefs.getInt("total_wrong", 0)
        dailyChallengeLastDate = prefs.getString("daily_challenge_date", "") ?: ""
        dailyChallengeStreak = prefs.getInt("daily_challenge_streak", 0)
    }

    fun exportBackupJson(): String {
        val map = mapOf(
            "userSettings" to _userSettings.value,
            "totalXp" to _totalXp.value,
            "streak" to _streak.value,
            "longestStreak" to _longestStreak.value,
            "favorites" to _favorites.value.toList(),
            "wordStats" to _wordStats.value,
            "chapterProgress" to _chapterProgress.value,
            "customWords" to _customWords.value,
            "achievements" to _achievementsMap.value,
            "history" to _history.value,
            "gamesCompleted" to gamesCompleted,
            "perfectGames" to perfectGames,
            "wordsMastered" to wordsMastered,
            "exportedAt" to System.currentTimeMillis()
        )
        return json.encodeToString(map)
    }

    fun importBackupJson(backupJson: String): Boolean {
        return try {
            val root = json.parseToJsonElement(backupJson) as? JsonObject ?: return false
            root["userSettings"]?.let { _userSettings.value = json.decodeFromJsonElement(it) }
            root["totalXp"]?.let { _totalXp.value = json.decodeFromJsonElement(it) }
            root["streak"]?.let { _streak.value = json.decodeFromJsonElement(it) }
            root["longestStreak"]?.let { _longestStreak.value = json.decodeFromJsonElement(it) }
            root["favorites"]?.let {
                val list: List<String> = json.decodeFromJsonElement(it)
                _favorites.value = list.toSet()
            }
            root["wordStats"]?.let { _wordStats.value = json.decodeFromJsonElement(it) }
            root["chapterProgress"]?.let { _chapterProgress.value = json.decodeFromJsonElement(it) }
            root["customWords"]?.let { _customWords.value = json.decodeFromJsonElement(it) }
            root["achievements"]?.let { _achievementsMap.value = json.decodeFromJsonElement(it) }
            root["history"]?.let { _history.value = json.decodeFromJsonElement(it) }
            refreshAchievementsList()
            saveState()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
