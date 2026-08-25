package com.example.deutschlernen.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = InkBlack,
    primaryContainer = GoldMuted,
    onPrimaryContainer = GoldPrimary,
    secondary = TealAccent,
    onSecondary = InkBlack,
    secondaryContainer = Color(0xFF1E3A39),
    onSecondaryContainer = TealAccent,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderLine,
    error = RedMistake,
    onError = InkBlack
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFBF4E4),
    onPrimaryContainer = GoldDark,
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2F3F3),
    onSecondaryContainer = Color(0xFF1F5C5B),
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceCardLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceRaisedLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLineLight,
    error = RedMistake,
    onError = Color.White
)

@Composable
fun DeutschLernenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customAccentHex: String? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val customPrimary = customAccentHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            null
        }
    }

    val colorScheme = if (customPrimary != null) {
        baseScheme.copy(primary = customPrimary)
    } else {
        baseScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
