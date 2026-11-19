package app.streamvibely.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.streamvibely.domain.ColorSource
import app.streamvibely.domain.ThemeArea
import app.streamvibely.domain.ThemeMode
import app.streamvibely.domain.ThemeSpec
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.engine.ColorEngine
import app.streamvibely.theme.engine.GeneratedColors

val LocalThemeSpec = staticCompositionLocalOf { ThemeSpec() }
val LocalCornerScale = staticCompositionLocalOf { 1f }
internal val LocalGeneratedColors = staticCompositionLocalOf<GeneratedColors?> { null }

/** Multiplies a corner radius by the theme's corner scale. Use as `Tokens.radiusCard.dp.scaled()`. */
@Composable
fun Dp.scaled(): Dp = this * AppTheme.cornerScale

// Inter is not bundled yet (see gotchas.md); the platform default font is used for now.
private fun style(size: Int, weight: FontWeight) = TextStyle(fontSize = size.sp, fontWeight = weight)

private val StreamvibelyTypography = Typography(
    headlineLarge = style(25, FontWeight.Bold),
    headlineMedium = style(24, FontWeight.Bold),
    headlineSmall = style(19, FontWeight.Bold),
    titleLarge = style(16, FontWeight.Bold),
    titleMedium = style(14, FontWeight.SemiBold),
    titleSmall = style(13, FontWeight.SemiBold),
    bodyMedium = style(13, FontWeight.Normal),
    bodySmall = style(11, FontWeight.Normal),
    labelMedium = style(12, FontWeight.Medium),
    labelSmall = style(11, FontWeight.Medium),
)

private fun shapesFor(scale: Float) = Shapes(
    extraSmall = RoundedCornerShape((Tokens.radiusArtwork * scale).dp),
    small = RoundedCornerShape((Tokens.radiusSm * scale).dp),
    medium = RoundedCornerShape((Tokens.radiusCard * scale).dp),
    large = RoundedCornerShape((Tokens.radiusTile * scale).dp),
    extraLarge = RoundedCornerShape((Tokens.radiusPanel * scale).dp),
)

/** Colors for one frame of the theme: the engine output plus the resolved [AppColors]. */
private class Resolved(val generated: GeneratedColors, val colors: AppColors)

@Composable
fun StreamvibelyTheme(
    spec: ThemeSpec = ThemeSpec(),
    artSeed: Int? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (spec.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val seed = if (spec.source == ColorSource.AlbumArt && artSeed != null) artSeed else spec.seedColor
    val target = remember(
        dark, seed, spec.variant, spec.contrast, spec.style, spec.clayDepth, spec.glassOpacity, spec.areaOverrides,
    ) { resolve(spec, dark, seed) }

    // One progress value lerps every role from the colors on screen to the new target.
    val progress = remember { Animatable(1f) }
    var from by remember { mutableStateOf(target.colors) }
    var to by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        if (target === to) return@LaunchedEffect
        from = if (progress.value >= 1f) to.colors else lerpColors(from, to.colors, progress.value)
        to = target
        progress.snapTo(0f)
        progress.animateTo(1f, tween(350))
    }
    val p = progress.value
    val colors = if (p >= 1f) to.colors else lerpColors(from, to.colors, p)
    val scheme = schemeFor(to.generated, colors)

    CompositionLocalProvider(
        LocalAppColors provides colors,
        LocalThemeSpec provides spec,
        LocalCornerScale provides spec.cornerScale,
        LocalGeneratedColors provides to.generated,
        LocalContentColor provides colors.onBackground,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = StreamvibelyTypography,
            shapes = remember(spec.cornerScale) { shapesFor(spec.cornerScale) },
            content = content,
        )
    }
}

private fun resolve(spec: ThemeSpec, dark: Boolean, seed: Int): Resolved {
    val g = ColorEngine.generate(seed, dark, spec.variant, spec.contrast, oled = spec.style == ThemeStyle.Oled && dark)
    val black = Color.Black
    val white = Color.White
    fun b(x: Color, y: Color, t: Float) = ColorEngine.blend(x, y, t)

    var background = g.background
    var surface = g.surface
    var card = g.surfaceContainer
    var cardHigh = g.surfaceContainerHigh
    var navigation = g.surfaceContainerLow
    var panel = g.surfaceContainerLow
    var playbackBar = g.surfaceContainerHigh
    var miniPlayer = g.secondaryContainer
    when (spec.style) {
        ThemeStyle.Material -> Unit
        ThemeStyle.Oled -> if (dark) {
            background = black
            surface = b(black, g.surface, 0.25f)
            card = b(black, g.surfaceContainer, 0.4f)
            cardHigh = b(black, g.surfaceContainerHigh, 0.5f)
            navigation = black
            panel = b(black, g.surfaceContainerLow, 0.3f)
            playbackBar = b(black, g.surfaceContainerHigh, 0.3f)
            miniPlayer = b(black, g.secondaryContainer, 0.55f)
        }
        ThemeStyle.Clay -> {
            val lift = if (dark) 0.06f else 0.45f
            card = b(card, white, lift)
            cardHigh = b(cardHigh, white, lift)
            navigation = b(navigation, white, lift)
            panel = b(panel, white, lift)
            playbackBar = b(playbackBar, white, lift)
            miniPlayer = b(miniPlayer, white, lift * 0.6f)
        }
        ThemeStyle.Glass -> Unit
    }

    var onBackground = g.onBackground
    var onSurface = g.onSurface
    var muted = g.onSurfaceVariant
    var onCard = g.onSurface
    var onNavigation = g.onSurface
    var accent = g.primary
    var onAccent = g.onPrimary
    var accentContainer = g.primaryContainer
    var onAccentContainer = g.onPrimaryContainer
    var onMiniPlayer = ColorEngine.onColorFor(miniPlayer)

    val ov = spec.areaOverrides
    ov[ThemeArea.Background]?.let {
        val c = Color(it)
        background = c
        surface = c
        onBackground = ColorEngine.onColorFor(c)
        onSurface = onBackground
        muted = b(onBackground, c, 0.35f)
    }
    ov[ThemeArea.Surfaces]?.let {
        val c = Color(it)
        card = c
        cardHigh = if (dark) b(c, white, 0.08f) else b(c, black, 0.06f)
        onCard = ColorEngine.onColorFor(c)
    }
    ov[ThemeArea.Navigation]?.let {
        val c = Color(it)
        navigation = c
        playbackBar = c
        onNavigation = ColorEngine.onColorFor(c)
    }
    ov[ThemeArea.MiniPlayer]?.let {
        val c = Color(it)
        miniPlayer = c
        onMiniPlayer = ColorEngine.onColorFor(c)
    }
    ov[ThemeArea.SidePanel]?.let { panel = Color(it) }
    ov[ThemeArea.Accent]?.let {
        val c = Color(it)
        accent = c
        onAccent = ColorEngine.onColorFor(c)
        accentContainer = if (dark) b(c, black, 0.6f) else b(c, white, 0.7f)
        onAccentContainer = ColorEngine.onColorFor(accentContainer)
    }
    if (ov[ThemeArea.MiniPlayer] == null) onMiniPlayer = ColorEngine.onColorFor(miniPlayer)

    if (spec.style == ThemeStyle.Glass) {
        val o = spec.glassOpacity.coerceIn(0f, 1f)
        card = card.copy(alpha = o)
        cardHigh = cardHigh.copy(alpha = (o + 0.1f).coerceAtMost(1f))
        navigation = navigation.copy(alpha = o)
        panel = panel.copy(alpha = o)
        playbackBar = playbackBar.copy(alpha = o)
        miniPlayer = miniPlayer.copy(alpha = o)
    }

    val tiles = listOf(
        g.primaryContainer, g.secondaryContainer, g.tertiaryContainer,
        b(g.primary, g.tertiary, 0.5f), b(g.secondary, g.tertiary, 0.5f), b(g.primary, g.secondary, 0.5f),
    ).map { b(it, background, 0.15f) }

    val colors = AppColors(
        isDark = dark, style = spec.style,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onSurface, muted = muted,
        card = card, cardHigh = cardHigh, onCard = onCard,
        navigation = navigation, onNavigation = onNavigation,
        miniPlayer = miniPlayer, onMiniPlayer = onMiniPlayer,
        panel = panel, playbackBar = playbackBar,
        accent = accent, onAccent = onAccent, accentContainer = accentContainer, onAccentContainer = onAccentContainer,
        outline = g.outline, outlineVariant = g.outlineVariant,
        sliderTrack = b(g.surfaceVariant, background, 0.2f),
        lyricsInactive = b(muted, background, 0.35f),
        error = g.error,
        tiles = tiles,
    )
    return Resolved(g, colors)
}

private fun lerpColors(a: AppColors, c: AppColors, t: Float): AppColors {
    fun l(x: Color, y: Color) = ColorEngine.blend(x, y, t)
    return AppColors(
        isDark = c.isDark, style = c.style,
        background = l(a.background, c.background), onBackground = l(a.onBackground, c.onBackground),
        surface = l(a.surface, c.surface), onSurface = l(a.onSurface, c.onSurface), muted = l(a.muted, c.muted),
        card = l(a.card, c.card), cardHigh = l(a.cardHigh, c.cardHigh), onCard = l(a.onCard, c.onCard),
        navigation = l(a.navigation, c.navigation), onNavigation = l(a.onNavigation, c.onNavigation),
        miniPlayer = l(a.miniPlayer, c.miniPlayer), onMiniPlayer = l(a.onMiniPlayer, c.onMiniPlayer),
        panel = l(a.panel, c.panel), playbackBar = l(a.playbackBar, c.playbackBar),
        accent = l(a.accent, c.accent), onAccent = l(a.onAccent, c.onAccent),
        accentContainer = l(a.accentContainer, c.accentContainer),
        onAccentContainer = l(a.onAccentContainer, c.onAccentContainer),
        outline = l(a.outline, c.outline), outlineVariant = l(a.outlineVariant, c.outlineVariant),
        sliderTrack = l(a.sliderTrack, c.sliderTrack), lyricsInactive = l(a.lyricsInactive, c.lyricsInactive),
        error = l(a.error, c.error),
        tiles = c.tiles.mapIndexed { i, tile -> l(a.tiles.getOrElse(i) { tile }, tile) },
    )
}

private fun schemeFor(g: GeneratedColors, c: AppColors): ColorScheme {
    fun opaque(x: Color) = x.copy(alpha = 1f)
    return if (c.isDark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer,
            onPrimaryContainer = c.onAccentContainer, inversePrimary = g.inversePrimary,
            secondary = g.secondary, onSecondary = g.onSecondary, secondaryContainer = g.secondaryContainer,
            onSecondaryContainer = g.onSecondaryContainer,
            tertiary = g.tertiary, onTertiary = g.onTertiary, tertiaryContainer = g.tertiaryContainer,
            onTertiaryContainer = g.onTertiaryContainer,
            background = c.background, onBackground = c.onBackground,
            surface = opaque(c.surface), onSurface = c.onSurface, surfaceVariant = g.surfaceVariant,
            onSurfaceVariant = c.muted, surfaceTint = c.accent,
            inverseSurface = g.inverseSurface, inverseOnSurface = g.inverseOnSurface,
            error = c.error, onError = g.onError, errorContainer = g.errorContainer, onErrorContainer = g.onErrorContainer,
            outline = c.outline, outlineVariant = c.outlineVariant, scrim = g.scrim,
            surfaceBright = g.surfaceBright, surfaceDim = g.surfaceDim,
            surfaceContainerLowest = opaque(c.navigation), surfaceContainerLow = opaque(c.panel),
            surfaceContainer = opaque(c.card), surfaceContainerHigh = opaque(c.cardHigh),
            surfaceContainerHighest = g.surfaceContainerHighest,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentContainer,
            onPrimaryContainer = c.onAccentContainer, inversePrimary = g.inversePrimary,
            secondary = g.secondary, onSecondary = g.onSecondary, secondaryContainer = g.secondaryContainer,
            onSecondaryContainer = g.onSecondaryContainer,
            tertiary = g.tertiary, onTertiary = g.onTertiary, tertiaryContainer = g.tertiaryContainer,
            onTertiaryContainer = g.onTertiaryContainer,
            background = c.background, onBackground = c.onBackground,
            surface = opaque(c.surface), onSurface = c.onSurface, surfaceVariant = g.surfaceVariant,
            onSurfaceVariant = c.muted, surfaceTint = c.accent,
            inverseSurface = g.inverseSurface, inverseOnSurface = g.inverseOnSurface,
            error = c.error, onError = g.onError, errorContainer = g.errorContainer, onErrorContainer = g.onErrorContainer,
            outline = c.outline, outlineVariant = c.outlineVariant, scrim = g.scrim,
            surfaceBright = g.surfaceBright, surfaceDim = g.surfaceDim,
            surfaceContainerLowest = opaque(c.navigation), surfaceContainerLow = opaque(c.panel),
            surfaceContainer = opaque(c.card), surfaceContainerHigh = opaque(c.cardHigh),
            surfaceContainerHighest = g.surfaceContainerHighest,
        )
    }
}
