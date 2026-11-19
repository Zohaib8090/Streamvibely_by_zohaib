package app.streamvibely.theme.engine

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw output of the color engine for one seed color: the Material 3 style role set. The theme
 * runtime turns this (plus user overrides and the chosen surface style) into
 * `MaterialTheme.colorScheme` and `AppColors`.
 */
@Immutable
data class GeneratedColors(
    val primary: Color, val onPrimary: Color, val primaryContainer: Color, val onPrimaryContainer: Color,
    val secondary: Color, val onSecondary: Color, val secondaryContainer: Color, val onSecondaryContainer: Color,
    val tertiary: Color, val onTertiary: Color, val tertiaryContainer: Color, val onTertiaryContainer: Color,
    val error: Color, val onError: Color, val errorContainer: Color, val onErrorContainer: Color,
    val background: Color, val onBackground: Color,
    val surface: Color, val onSurface: Color, val surfaceVariant: Color, val onSurfaceVariant: Color,
    val surfaceDim: Color, val surfaceBright: Color,
    val surfaceContainerLowest: Color, val surfaceContainerLow: Color, val surfaceContainer: Color,
    val surfaceContainerHigh: Color, val surfaceContainerHighest: Color,
    val outline: Color, val outlineVariant: Color,
    val inverseSurface: Color, val inverseOnSurface: Color, val inversePrimary: Color,
    val scrim: Color,
)
