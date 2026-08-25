package com.example.deutschlernen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.deutschlernen.audio.AudioAndTtsManager
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.model.*
import com.example.deutschlernen.ui.screens.*
import com.example.deutschlernen.ui.theme.DeutschLernenTheme
import com.example.deutschlernen.ui.theme.GoldPrimary

sealed class Screen {
    data object Home : Screen()
    data class ChapterList(val level: String) : Screen()
    data class Study(val title: String, val words: List<Word>, val level: String, val chapter: Int) : Screen()
    data class ModeSelect(
        val title: String,
        val pool: List<Word>,
        val levelLabel: String,
        val isDaily: Boolean = false,
        val chapterSource: Pair<String, Int>? = null
    ) : Screen()
    data class Game(
        val mode: GameMode,
        val difficulty: GameDifficulty,
        val pool: List<Word>,
        val levelLabel: String,
        val isDaily: Boolean = false,
        val chapterSource: Pair<String, Int>? = null
    ) : Screen()
    data class Result(
        val score: Int,
        val correctCount: Int,
        val wrongCount: Int,
        val durationMs: Long,
        val mistakes: List<Word>,
        val correctWords: List<Word>,
        val isDaily: Boolean,
        val levelLabel: String,
        val mode: GameMode,
        val difficulty: GameDifficulty,
        val pool: List<Word>,
        val chapterSource: Pair<String, Int>? = null
    ) : Screen()
    data object DifficultWords : Screen()
    data object Favorites : Screen()
    data object Progress : Screen()
    data object MyVocabulary : Screen()
    data object DailyChallenge : Screen()
    data object Leaderboard : Screen()
    data object Profile : Screen()
    data object Search : Screen()
    data object About : Screen()
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: DeutschRepository
    private lateinit var audioManager: AudioAndTtsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = (application as DeutschApplication).repository
        audioManager = AudioAndTtsManager(this)

        setContent {
            val settings by repository.userSettings.collectAsState()
            val currentRole by repository.currentRole.collectAsState()

            var backStack by remember { mutableStateOf(listOf<Screen>(Screen.Home)) }
            val currentScreen = backStack.lastOrNull() ?: Screen.Home

            fun navigateTo(screen: Screen) {
                backStack = backStack + screen
            }

            fun popBack() {
                if (backStack.size > 1) {
                    backStack = backStack.dropLast(1)
                }
            }

            fun navigateRoot(screen: Screen) {
                backStack = listOf(screen)
            }

            DeutschLernenTheme(
                darkTheme = settings.darkTheme,
                customAccentHex = currentRole?.badgeColor
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        bottomBar = {
                            val isTopLevelScreen = currentScreen is Screen.Home ||
                                    currentScreen is Screen.DifficultWords ||
                                    currentScreen is Screen.Favorites ||
                                    currentScreen is Screen.Progress ||
                                    currentScreen is Screen.Search

                            if (isTopLevelScreen) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Home,
                                        onClick = { navigateRoot(Screen.Home) },
                                        icon = { Icon(if (currentScreen is Screen.Home) Icons.Filled.Home else Icons.Outlined.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        modifier = Modifier.testTag("nav_home")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Search,
                                        onClick = { navigateTo(Screen.Search) },
                                        icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                        label = { Text("Search") },
                                        modifier = Modifier.testTag("nav_search")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Favorites,
                                        onClick = { navigateTo(Screen.Favorites) },
                                        icon = { Icon(if (currentScreen is Screen.Favorites) Icons.Filled.Star else Icons.Outlined.StarBorder, contentDescription = "Favorites") },
                                        label = { Text("Favorites") },
                                        modifier = Modifier.testTag("nav_favorites")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Progress,
                                        onClick = { navigateTo(Screen.Progress) },
                                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Progress") },
                                        label = { Text("Progress") },
                                        modifier = Modifier.testTag("nav_progress")
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen is Screen.Profile,
                                        onClick = { navigateTo(Screen.Profile) },
                                        icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                                        label = { Text("Profile") },
                                        modifier = Modifier.testTag("nav_profile")
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (val scr = currentScreen) {
                                is Screen.Home -> {
                                    HomeScreen(
                                        repository = repository,
                                        onNavigateLevel = { lv -> navigateTo(Screen.ChapterList(lv)) },
                                        onNavigateRandomPractice = {
                                            val words = repository.allWords
                                            navigateTo(Screen.ModeSelect(title = "Random All Levels", pool = words, levelLabel = "All Levels"))
                                        },
                                        onNavigateDifficult = { navigateTo(Screen.DifficultWords) },
                                        onNavigateProgress = { navigateTo(Screen.Progress) },
                                        onNavigateFavorites = { navigateTo(Screen.Favorites) },
                                        onNavigateDaily = { navigateTo(Screen.DailyChallenge) },
                                        onNavigateLeaderboard = { navigateTo(Screen.Leaderboard) },
                                        onNavigateCustomWords = { navigateTo(Screen.MyVocabulary) },
                                        onNavigateProfile = { navigateTo(Screen.Profile) },
                                        onNavigateAbout = { navigateTo(Screen.About) }
                                    )
                                }

                                is Screen.ChapterList -> {
                                    ChapterListScreen(
                                        level = scr.level,
                                        repository = repository,
                                        onBack = ::popBack,
                                        onStudyChapter = { ch ->
                                            val chapterData = repository.chapterWords(scr.level, ch)
                                            val title = "Level ${scr.level} · Ch.$ch"
                                            navigateTo(Screen.Study(title = title, words = chapterData, level = scr.level, chapter = ch))
                                        },
                                        onPlayChapterBatch = { ch ->
                                            val batch = repository.chapterWordsToPractice(scr.level, ch, 15)
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Level ${scr.level} · Chapter $ch",
                                                    pool = batch,
                                                    levelLabel = "Level ${scr.level} Ch.$ch",
                                                    chapterSource = Pair(scr.level, ch)
                                                )
                                            )
                                        },
                                        onPlayRandomLevel = {
                                            val words = repository.wordsByLevel(scr.level)
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Level ${scr.level} (All Words)",
                                                    pool = words,
                                                    levelLabel = "Level ${scr.level}"
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.Study -> {
                                    StudyScreen(
                                        title = scr.title,
                                        words = scr.words,
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack,
                                        onStartGame = {
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = scr.title,
                                                    pool = scr.words,
                                                    levelLabel = "Level ${scr.level} Ch.${scr.chapter}",
                                                    chapterSource = Pair(scr.level, scr.chapter)
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.ModeSelect -> {
                                    ModeSelectScreen(
                                        title = scr.title,
                                        wordCount = scr.pool.size,
                                        onBack = ::popBack,
                                        onSelectModeAndDifficulty = { mode, diff ->
                                            navigateTo(
                                                Screen.Game(
                                                    mode = mode,
                                                    difficulty = diff,
                                                    pool = scr.pool,
                                                    levelLabel = scr.levelLabel,
                                                    isDaily = scr.isDaily,
                                                    chapterSource = scr.chapterSource
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.Game -> {
                                    GameScreen(
                                        mode = scr.mode,
                                        difficulty = scr.difficulty,
                                        pool = scr.pool,
                                        levelLabel = scr.levelLabel,
                                        isDaily = scr.isDaily,
                                        chapterSource = scr.chapterSource,
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack,
                                        onFinishGame = { score, correct, wrong, dur, mistakes, correctWords, isDaily, lvl ->
                                            navigateTo(
                                                Screen.Result(
                                                    score = score,
                                                    correctCount = correct,
                                                    wrongCount = wrong,
                                                    durationMs = dur,
                                                    mistakes = mistakes,
                                                    correctWords = correctWords,
                                                    isDaily = isDaily,
                                                    levelLabel = lvl,
                                                    mode = scr.mode,
                                                    difficulty = scr.difficulty,
                                                    pool = scr.pool,
                                                    chapterSource = scr.chapterSource
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.Result -> {
                                    ResultScreen(
                                        score = scr.score,
                                        correctCount = scr.correctCount,
                                        wrongCount = scr.wrongCount,
                                        durationMs = scr.durationMs,
                                        mistakes = scr.mistakes,
                                        correctWords = scr.correctWords,
                                        isDaily = scr.isDaily,
                                        levelLabel = scr.levelLabel,
                                        repository = repository,
                                        audioManager = audioManager,
                                        onPlayAgain = {
                                            val nextBatch = if (scr.chapterSource != null) {
                                                repository.chapterWordsToPractice(scr.chapterSource.first, scr.chapterSource.second, 15)
                                            } else scr.pool
                                            navigateTo(
                                                Screen.Game(
                                                    mode = scr.mode,
                                                    difficulty = scr.difficulty,
                                                    pool = nextBatch,
                                                    levelLabel = scr.levelLabel,
                                                    isDaily = scr.isDaily,
                                                    chapterSource = scr.chapterSource
                                                )
                                            )
                                        },
                                        onPracticeMistakes = { mistakeWords ->
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Practice Mistakes",
                                                    pool = mistakeWords,
                                                    levelLabel = "Mistakes Review"
                                                )
                                            )
                                        },
                                        onGoHome = {
                                            navigateRoot(Screen.Home)
                                        }
                                    )
                                }

                                is Screen.DifficultWords -> {
                                    DifficultWordsScreen(
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack,
                                        onPracticeDifficult = { diffWords ->
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Difficult Words",
                                                    pool = diffWords,
                                                    levelLabel = "Difficult Words"
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.Favorites -> {
                                    FavoritesScreen(
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack,
                                        onPracticeFavorites = { favWords ->
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Favorite Words",
                                                    pool = favWords,
                                                    levelLabel = "Favorites"
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.Progress -> {
                                    ProgressScreen(
                                        repository = repository,
                                        onBack = ::popBack
                                    )
                                }

                                is Screen.MyVocabulary -> {
                                    MyVocabularyScreen(
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack,
                                        onPracticeCustom = { customWords ->
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "My Vocabulary",
                                                    pool = customWords,
                                                    levelLabel = "Custom Words"
                                                )
                                            )
                                        }
                                    )
                                }

                                is Screen.DailyChallenge -> {
                                    DailyChallengeScreen(
                                        repository = repository,
                                        onBack = ::popBack,
                                        onStartDailyGame = { words ->
                                            navigateTo(
                                                Screen.ModeSelect(
                                                    title = "Daily Challenge",
                                                    pool = words,
                                                    levelLabel = "Daily Challenge",
                                                    isDaily = true
                                                )
                                            )
                                        },
                                        onOpenLeaderboard = {
                                            navigateTo(Screen.Leaderboard)
                                        }
                                    )
                                }

                                is Screen.Leaderboard -> {
                                    LeaderboardScreen(
                                        repository = repository,
                                        onBack = ::popBack
                                    )
                                }

                                is Screen.Profile -> {
                                    ProfileScreen(
                                        repository = repository,
                                        onBack = ::popBack
                                    )
                                }

                                is Screen.Search -> {
                                    SearchScreen(
                                        repository = repository,
                                        audioManager = audioManager,
                                        onBack = ::popBack
                                    )
                                }

                                is Screen.About -> {
                                    AboutScreen(
                                        onBack = ::popBack
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager.shutdown()
    }
}
