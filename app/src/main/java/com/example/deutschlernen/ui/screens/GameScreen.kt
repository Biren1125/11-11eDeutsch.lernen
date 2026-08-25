package com.example.deutschlernen.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.audio.AudioAndTtsManager
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.model.*
import com.example.deutschlernen.ui.components.GermanFlagRibbon
import com.example.deutschlernen.ui.components.HeartMeter
import com.example.deutschlernen.ui.components.WordArticleChip
import com.example.deutschlernen.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    mode: GameMode,
    difficulty: GameDifficulty,
    pool: List<Word>,
    levelLabel: String,
    isDaily: Boolean = false,
    chapterSource: Pair<String, Int>? = null, // level, chapter
    repository: DeutschRepository,
    audioManager: AudioAndTtsManager,
    onBack: () -> Unit,
    onFinishGame: (
        score: Int,
        correct: Int,
        wrong: Int,
        durationMs: Long,
        mistakes: List<Word>,
        correctWords: List<Word>,
        isDaily: Boolean,
        levelLabel: String
    ) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val hearts by repository.heartsCount.collectAsState()
    val settings by repository.userSettings.collectAsState()
    val hasUnlimitedHearts = repository.hasUnlimitedHearts()

    // Game round word selection
    val roundWords = remember {
        val count = if (mode == GameMode.MEMORY) {
            min(10, pool.size)
        } else if (chapterSource != null) {
            pool.size
        } else {
            difficulty.mcqCount
        }
        if (pool.size <= count) pool.shuffled() else repository.weightedSample(pool, count) { it.id }
    }

    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var maxCombo by remember { mutableIntStateOf(0) }
    var correctCount by remember { mutableIntStateOf(0) }
    var wrongCount by remember { mutableIntStateOf(0) }
    val mistakeWords = remember { mutableStateListOf<Word>() }
    val correctWordsList = remember { mutableStateListOf<Word>() }

    val startTime = remember { System.currentTimeMillis() }
    val totalSeconds = remember { difficulty.calculateTotalSeconds(roundWords.size) }
    var remainingSeconds by remember { mutableIntStateOf(totalSeconds) }
    var isGameOver by remember { mutableStateOf(false) }

    // Countdown timer loop
    LaunchedEffect(Unit) {
        while (remainingSeconds > 0 && !isGameOver) {
            delay(1000L)
            remainingSeconds--
        }
        if (remainingSeconds <= 0 && !isGameOver) {
            isGameOver = true
            val durationMs = System.currentTimeMillis() - startTime
            onFinishGame(score, correctCount, wrongCount, durationMs, mistakeWords, correctWordsList, isDaily, levelLabel)
        }
    }

    fun handleCorrect(word: Word) {
        combo++
        maxCombo = max(maxCombo, combo)
        correctCount++
        val comboBonus = min(combo, 10) * 10
        score += (100 + comboBonus)
        if (!correctWordsList.contains(word)) correctWordsList.add(word)
        repository.recordAnswer(word.id, true)
        audioManager.playCorrect(settings.soundEnabled, settings.soundVolume, settings.soundStyle)
    }

    fun handleWrong(word: Word) {
        combo = 0
        wrongCount++
        if (!mistakeWords.any { it.id == word.id }) mistakeWords.add(word)
        repository.recordAnswer(word.id, false)
        audioManager.playWrong(settings.soundEnabled, settings.soundVolume)
        val remaining = repository.loseHeart()
        if (remaining <= 0 && !hasUnlimitedHearts) {
            isGameOver = true
            val durationMs = System.currentTimeMillis() - startTime
            onFinishGame(score, correctCount, wrongCount, durationMs, mistakeWords, correctWordsList, isDaily, levelLabel)
        }
    }

    fun completeGame() {
        if (isGameOver) return
        isGameOver = true
        if (chapterSource != null) {
            repository.advanceChapterBatch(chapterSource.first, chapterSource.second, roundWords)
        }
        val durationMs = System.currentTimeMillis() - startTime
        onFinishGame(score, correctCount, wrongCount, durationMs, mistakeWords, correctWordsList, isDaily, levelLabel)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HeartMeter(
                            hearts = hearts,
                            hasUnlimitedHearts = hasUnlimitedHearts
                        )

                        // Timer Badge
                        val timerColor = if (remainingSeconds <= 10) RedMistake else MaterialTheme.colorScheme.primary
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = timerColor.copy(alpha = 0.15f),
                            modifier = Modifier.testTag("game_timer")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(text = "⏱️", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${remainingSeconds}s",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = timerColor
                                )
                            }
                        }

                        // Score & Combo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (combo > 1) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoldPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "${combo}x 🔥",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = GoldPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "$score",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("game_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quit Game")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (mode) {
                GameMode.MEMORY -> {
                    MemoryMatchGameContent(
                        words = roundWords,
                        audioManager = audioManager,
                        soundEnabled = settings.soundEnabled,
                        soundVolume = settings.soundVolume,
                        ttsSpeed = settings.ttsSpeed,
                        ttsPitch = settings.ttsPitch,
                        onCorrect = ::handleCorrect,
                        onWrong = ::handleWrong,
                        onCompleted = ::completeGame
                    )
                }
                GameMode.PAIRS -> {
                    WordPairsGameContent(
                        words = roundWords,
                        audioManager = audioManager,
                        soundEnabled = settings.soundEnabled,
                        soundVolume = settings.soundVolume,
                        ttsSpeed = settings.ttsSpeed,
                        ttsPitch = settings.ttsPitch,
                        onCorrect = ::handleCorrect,
                        onWrong = ::handleWrong,
                        onCompleted = ::completeGame
                    )
                }
                GameMode.DE2EN, GameMode.EN2DE, GameMode.MCQ -> {
                    McqGameContent(
                        mode = mode,
                        words = roundWords,
                        fullPool = pool,
                        audioManager = audioManager,
                        soundEnabled = settings.soundEnabled,
                        soundVolume = settings.soundVolume,
                        ttsSpeed = settings.ttsSpeed,
                        ttsPitch = settings.ttsPitch,
                        onCorrect = ::handleCorrect,
                        onWrong = ::handleWrong,
                        onCompleted = ::completeGame
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MCQ / DE2EN / EN2DE Content
// -------------------------------------------------------------
@Composable
fun McqGameContent(
    mode: GameMode,
    words: List<Word>,
    fullPool: List<Word>,
    audioManager: AudioAndTtsManager,
    soundEnabled: Boolean,
    soundVolume: Float,
    ttsSpeed: Float,
    ttsPitch: Float,
    onCorrect: (Word) -> Unit,
    onWrong: (Word) -> Unit,
    onCompleted: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }
    var isAnswerLocked by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    if (currentIndex >= words.size) {
        LaunchedEffect(Unit) { onCompleted() }
        return
    }

    val currentWord = words[currentIndex]

    // Determine direction
    val isDeToEn = remember(currentIndex, mode) {
        when (mode) {
            GameMode.DE2EN -> true
            GameMode.EN2DE -> false
            else -> kotlin.random.Random.nextBoolean()
        }
    }

    val promptText = if (isDeToEn) currentWord.de else currentWord.en
    val answerFieldIsDe = !isDeToEn

    // Generate 4 choices
    val choices = remember(currentIndex) {
        val distractors = fullPool
            .filter { it.id != currentWord.id && (if (isDeToEn) it.en.isNotBlank() else it.de.isNotBlank()) }
            .shuffled()
            .take(3)
        (listOf(currentWord) + distractors).shuffled().map { w ->
            ChoiceOption(
                wordId = w.id,
                text = if (isDeToEn) w.en else w.de
            )
        }
    }

    // Auto-pronounce if German is prompt
    LaunchedEffect(currentIndex) {
        selectedOptionId = null
        isAnswerLocked = false
        if (isDeToEn) {
            audioManager.speakGerman(currentWord.de, ttsSpeed, ttsPitch)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Question Progress Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Question ${currentIndex + 1} / ${words.size}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (isDeToEn) "🇩🇪 → 🇬🇧" else "🇬🇧 → 🇩🇪",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { (currentIndex + 1).toFloat() / words.size },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Question Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                if (isDeToEn) {
                    WordArticleChip(currentWord.article)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = promptText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isDeToEn) {
                    Spacer(modifier = Modifier.height(12.dp))
                    FilledIconButton(
                        onClick = { audioManager.speakGerman(currentWord.de, ttsSpeed, ttsPitch) },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.size(42.dp).testTag("mcq_audio_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Hear German", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Choice Options List
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            choices.forEachIndexed { index, option ->
                val isSelected = selectedOptionId == option.wordId
                val isCorrectChoice = option.wordId == currentWord.id

                val cardBg by animateColorAsState(
                    targetValue = when {
                        !isAnswerLocked -> MaterialTheme.colorScheme.surface
                        isCorrectChoice -> GreenSuccess.copy(alpha = 0.25f)
                        isSelected && !isCorrectChoice -> RedMistake.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                    },
                    label = "choice_bg"
                )

                val borderStroke = when {
                    !isAnswerLocked && isSelected -> BorderLine
                    isAnswerLocked && isCorrectChoice -> GreenSuccess
                    isAnswerLocked && isSelected && !isCorrectChoice -> RedMistake
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isAnswerLocked) {
                            selectedOptionId = option.wordId
                            isAnswerLocked = true

                            val correct = option.wordId == currentWord.id
                            if (correct) {
                                onCorrect(currentWord)
                            } else {
                                onWrong(currentWord)
                            }

                            if (answerFieldIsDe) {
                                audioManager.speakGerman(option.text, ttsSpeed, ttsPitch)
                            }

                            coroutineScope.launch {
                                delay(850L)
                                if (currentIndex < words.size - 1) {
                                    currentIndex++
                                } else {
                                    onCompleted()
                                }
                            }
                        }
                        .border(1.5.dp, borderStroke, RoundedCornerShape(14.dp))
                        .testTag("choice_$index")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = listOf("A", "B", "C", "D").getOrElse(index) { "?" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Text(
                            text = option.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )

                        if (isAnswerLocked) {
                            if (isCorrectChoice) {
                                Text(text = "✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                            } else if (isSelected) {
                                Text(text = "✗", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedMistake)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Memory Match Content (Flip Card Grid)
// -------------------------------------------------------------
@Composable
fun MemoryMatchGameContent(
    words: List<Word>,
    audioManager: AudioAndTtsManager,
    soundEnabled: Boolean,
    soundVolume: Float,
    ttsSpeed: Float,
    ttsPitch: Float,
    onCorrect: (Word) -> Unit,
    onWrong: (Word) -> Unit,
    onCompleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var sectionIndex by remember { mutableIntStateOf(0) } // 0 or 1 for 5 words each

    val activeFiveWords = remember(sectionIndex) {
        val start = sectionIndex * 5
        words.drop(start).take(5)
    }

    val cards = remember(sectionIndex) {
        val list = mutableListOf<MemoryCard>()
        activeFiveWords.forEach { w ->
            list.add(MemoryCard(cardId = "${w.id}_de", wordId = w.id, text = w.de, side = "de"))
            list.add(MemoryCard(cardId = "${w.id}_en", wordId = w.id, text = w.en, side = "en"))
        }
        list.shuffled().toMutableStateList()
    }

    val flippedIndices = remember { mutableStateListOf<Int>() }
    var isInputLocked by remember { mutableStateOf(false) }

    fun checkSectionCompletion() {
        if (cards.all { it.isMatched }) {
            audioManager.playMatch(soundEnabled, soundVolume)
            coroutineScope.launch {
                delay(600L)
                if (sectionIndex == 0 && words.size > 5) {
                    sectionIndex = 1
                    flippedIndices.clear()
                    isInputLocked = false
                } else {
                    onCompleted()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Memory Match · Section ${sectionIndex + 1} of ${if (words.size > 5) 2 else 1}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${cards.count { it.isMatched } / 2} / 5 Pairs",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(cards.size) { index ->
                val card = cards[index]
                val rotation by animateFloatAsState(
                    targetValue = if (card.isFlipped || card.isMatched) 180f else 0f,
                    animationSpec = tween(300),
                    label = "mem_card_$index"
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            card.isMatched -> GreenSuccess.copy(alpha = 0.2f)
                            card.isFlipped -> MaterialTheme.colorScheme.surface
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable(enabled = !isInputLocked && !card.isFlipped && !card.isMatched) {
                            audioManager.playCardFlip(soundEnabled, soundVolume)
                            card.isFlipped = true
                            if (card.side == "de") {
                                audioManager.speakGerman(card.text, ttsSpeed, ttsPitch)
                            }
                            flippedIndices.add(index)

                            if (flippedIndices.size == 2) {
                                isInputLocked = true
                                val firstIdx = flippedIndices[0]
                                val secondIdx = flippedIndices[1]
                                val card1 = cards[firstIdx]
                                val card2 = cards[secondIdx]

                                if (card1.wordId == card2.wordId && card1.side != card2.side) {
                                    // Match found
                                    card1.isMatched = true
                                    card2.isMatched = true
                                    val matchedWord = activeFiveWords.find { it.id == card1.wordId }
                                    if (matchedWord != null) onCorrect(matchedWord)
                                    flippedIndices.clear()
                                    isInputLocked = false
                                    checkSectionCompletion()
                                } else {
                                    // Mismatch
                                    val wrongWord = activeFiveWords.find { it.id == card1.wordId } ?: activeFiveWords.first()
                                    onWrong(wrongWord)
                                    coroutineScope.launch {
                                        delay(850L)
                                        card1.isFlipped = false
                                        card2.isFlipped = false
                                        flippedIndices.clear()
                                        isInputLocked = false
                                    }
                                }
                            }
                        }
                        .border(
                            1.dp,
                            if (card.isMatched) GreenSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            RoundedCornerShape(14.dp)
                        )
                        .testTag("memory_card_$index")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        if (rotation > 90f) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.graphicsLayer { rotationY = 180f }
                            ) {
                                Text(
                                    text = if (card.side == "de") "🇩🇪" else "🇬🇧",
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = card.text,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = if (card.isMatched) GreenSuccess else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else {
                            Text(
                                text = "11:11e",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// Word Pairs Content (2 Columns)
// -------------------------------------------------------------
@Composable
fun WordPairsGameContent(
    words: List<Word>,
    audioManager: AudioAndTtsManager,
    soundEnabled: Boolean,
    soundVolume: Float,
    ttsSpeed: Float,
    ttsPitch: Float,
    onCorrect: (Word) -> Unit,
    onWrong: (Word) -> Unit,
    onCompleted: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val leftItems = remember { words.map { PairItem(wordId = it.id, text = it.de, side = "de") }.shuffled().toMutableStateList() }
    val rightItems = remember { words.map { PairItem(wordId = it.id, text = it.en, side = "en") }.shuffled().toMutableStateList() }

    var selectedLeftIndex by remember { mutableStateOf<Int?>(null) }
    var selectedRightIndex by remember { mutableStateOf<Int?>(null) }
    var isMatchingLocked by remember { mutableStateOf(false) }

    fun checkMatches() {
        val lIdx = selectedLeftIndex ?: return
        val rIdx = selectedRightIndex ?: return
        val left = leftItems[lIdx]
        val right = rightItems[rIdx]

        isMatchingLocked = true

        if (left.wordId == right.wordId) {
            left.isMatched = true
            right.isMatched = true
            left.isSelected = false
            right.isSelected = false
            val word = words.find { it.id == left.wordId }
            if (word != null) onCorrect(word)

            selectedLeftIndex = null
            selectedRightIndex = null
            isMatchingLocked = false

            if (leftItems.all { it.isMatched }) {
                audioManager.playMatch(soundEnabled, soundVolume)
                coroutineScope.launch {
                    delay(500L)
                    onCompleted()
                }
            }
        } else {
            left.isError = true
            right.isError = true
            val word = words.find { it.id == left.wordId } ?: words.first()
            onWrong(word)

            coroutineScope.launch {
                delay(700L)
                left.isSelected = false
                right.isSelected = false
                left.isError = false
                right.isError = false
                selectedLeftIndex = null
                selectedRightIndex = null
                isMatchingLocked = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Word Pairs · Match columns",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${leftItems.count { it.isMatched }} / ${words.size} Matched",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left column (German)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(leftItems.size) { index ->
                    val item = leftItems[index]
                    val isSelected = selectedLeftIndex == index

                    val cardBg = when {
                        item.isMatched -> GreenSuccess.copy(alpha = 0.15f)
                        item.isError -> RedMistake.copy(alpha = 0.25f)
                        isSelected -> GoldPrimary.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatchingLocked && !item.isMatched) {
                                if (selectedLeftIndex == index) {
                                    selectedLeftIndex = null
                                    item.isSelected = false
                                } else {
                                    selectedLeftIndex?.let { leftItems[it].isSelected = false }
                                    selectedLeftIndex = index
                                    item.isSelected = true
                                    audioManager.speakGerman(item.text, ttsSpeed, ttsPitch)
                                    if (selectedRightIndex != null) checkMatches()
                                }
                            }
                            .border(
                                1.dp,
                                when {
                                    item.isMatched -> GreenSuccess
                                    item.isError -> RedMistake
                                    isSelected -> GoldPrimary
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("pair_left_$index")
                    ) {
                        Text(
                            text = item.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isMatched) GreenSuccess else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Right column (English)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(rightItems.size) { index ->
                    val item = rightItems[index]
                    val isSelected = selectedRightIndex == index

                    val cardBg = when {
                        item.isMatched -> GreenSuccess.copy(alpha = 0.15f)
                        item.isError -> RedMistake.copy(alpha = 0.25f)
                        isSelected -> TealAccent.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isMatchingLocked && !item.isMatched) {
                                if (selectedRightIndex == index) {
                                    selectedRightIndex = null
                                    item.isSelected = false
                                } else {
                                    selectedRightIndex?.let { rightItems[it].isSelected = false }
                                    selectedRightIndex = index
                                    item.isSelected = true
                                    if (selectedLeftIndex != null) checkMatches()
                                }
                            }
                            .border(
                                1.dp,
                                when {
                                    item.isMatched -> GreenSuccess
                                    item.isError -> RedMistake
                                    isSelected -> TealAccent
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .testTag("pair_right_$index")
                    ) {
                        Text(
                            text = item.text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isMatched) GreenSuccess else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}
