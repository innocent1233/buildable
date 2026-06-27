package com.libraryx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Replaces src/components/ThemeToggle.tsx + the `.dark` CSS class toggling in src/index.css.
 * The boolean `darkTheme` mirrors the original's default of following the OS preference
 * unless the user explicitly flips ThemeToggle, which the calling Composable tracks in its
 * own state and passes down here.
 */
private val DarkColors = darkColorScheme(
    primary = StudyLabColors.NeonGreen,
    secondary = StudyLabColors.NeonCyan,
    tertiary = StudyLabColors.NeonPink,
    background = StudyLabColors.Background,
    surface = StudyLabColors.Surface,
    surfaceVariant = StudyLabColors.SurfaceVariant,
    outline = StudyLabColors.Border,
    onBackground = StudyLabColors.TextPrimary,
    onSurface = StudyLabColors.TextPrimary,
    onPrimary = Color(0xFF03130A),
    error = StudyLabColors.NeonPink
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF12A150),
    secondary = Color(0xFF0E8FA8),
    tertiary = Color(0xFFC0177C),
    background = StudyLabColors.LightBackground,
    surface = StudyLabColors.LightSurface,
    outline = StudyLabColors.LightBorder,
    onBackground = StudyLabColors.LightTextPrimary,
    onSurface = StudyLabColors.LightTextPrimary,
    error = Color(0xFFC0177C)
)

@Composable
fun StudyLabTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = StudyLabTypography,
        content = content
    )
}
