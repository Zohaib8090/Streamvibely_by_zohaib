package app.streamvibely.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import app.streamvibely.domain.ThemeStyle

/**
 * Every color the UI draws with, resolved from the current [app.streamvibely.domain.ThemeSpec].
 * Screens read these through [AppTheme.colors] and never use hard-coded colors. The standard
 * Material roles are also set on `MaterialTheme.colorScheme` by the theme runtime.
 */
@Immutable
class AppColors(
    val isDark: Boolean,
    val style: ThemeStyle,

    val background: Color,
    val onBackground: Color,
    /** Default panel/page surface. */
    val surface: Color,
    val onSurface: Color,
    /** Secondary text and icons. */
    val muted: Color,

    /** Cards, rows, chips, text fields. */
    val card: Color,
    val cardHigh: Color,
    val onCard: Color,

    val navigation: Color,
    val onNavigation: Color,
    val miniPlayer: Color,
    val onMiniPlayer: Color,
    /** Desktop right-hand panel. */
    val panel: Color,
    val playbackBar: Color,

    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,

    val outline: Color,
    val outlineVariant: Color,
    val sliderTrack: Color,
    val lyricsInactive: Color,
    val error: Color,
    /** Six colors for the category tiles on the Search page. */
    val tiles: List<Color>,
) {
    companion object {
        /** Used before the theme runtime has produced real colors, and in previews. Dark, blue-violet. */
        val Fallback = AppColors(
            isDark = true, style = ThemeStyle.Material,
            background = Color(0xFF0F1115), onBackground = Color(0xFFE6E8F0),
            surface = Color(0xFF151821), onSurface = Color(0xFFE6E8F0), muted = Color(0xFFA3A9BB),
            card = Color(0xFF1B1F2A), cardHigh = Color(0xFF232837), onCard = Color(0xFFE6E8F0),
            navigation = Color(0xFF12151C), onNavigation = Color(0xFFE6E8F0),
            miniPlayer = Color(0xFF2A3150), onMiniPlayer = Color(0xFFE6E8F0),
            panel = Color(0xFF171B24), playbackBar = Color(0xFF1A1E29),
            accent = Color(0xFF8AA4FF), onAccent = Color(0xFF0B1233),
            accentContainer = Color(0xFF2B3A7A), onAccentContainer = Color(0xFFDDE3FF),
            outline = Color(0xFF4A5068), outlineVariant = Color(0xFF2B3040), sliderTrack = Color(0xFF2E3446),
            lyricsInactive = Color(0xFF6F7794), error = Color(0xFFFFB4AB),
            tiles = listOf(Color(0xFF3F5BA9), Color(0xFF7444A0), Color(0xFF326E77), Color(0xFFB25135), Color(0xFF484DA0), Color(0xFF9A6034)),
        )
    }
}

val LocalAppColors = staticCompositionLocalOf { AppColors.Fallback }

/** Entry point for theme values inside composables: `AppTheme.colors.card`. */
object AppTheme {
    val colors: AppColors
        @Composable get() = LocalAppColors.current

    /** Multiplier for every corner radius, from `ThemeSpec.cornerScale`. */
    val cornerScale: Float
        @Composable get() = LocalCornerScale.current
}
