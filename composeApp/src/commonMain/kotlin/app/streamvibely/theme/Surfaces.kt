package app.streamvibely.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.engine.ColorEngine

/** What a surface is, so each style can decide how much decoration it gets. */
enum class SurfaceKind { Card, CardHigh, Navigation, MiniPlayer, Panel, Chip, Field }

/**
 * Draws a themed surface behind the content. Does not clip: put `.clip(shape)` AFTER this
 * (then `.clickable`) so the Clay shadow, which sits outside the bounds, is not cut off.
 */
@Composable
fun Modifier.appSurface(kind: SurfaceKind, shape: Shape): Modifier {
    val c = AppTheme.colors
    val fill = when (kind) {
        SurfaceKind.Card, SurfaceKind.Field -> c.card
        SurfaceKind.CardHigh, SurfaceKind.Chip -> c.cardHigh
        SurfaceKind.Navigation -> c.navigation
        SurfaceKind.MiniPlayer -> c.miniPlayer
        SurfaceKind.Panel -> c.panel
    }
    val bordered = kind != SurfaceKind.Navigation
    return when (c.style) {
        ThemeStyle.Material -> background(fill, shape)
        ThemeStyle.Oled -> background(fill, shape).let { if (bordered) it.border(1.dp, c.outlineVariant, shape) else it }
        ThemeStyle.Clay -> {
            val depth = LocalThemeSpec.current.clayDepth.coerceIn(0f, 1f)
            val raised = kind == SurfaceKind.Card || kind == SurfaceKind.CardHigh ||
                kind == SurfaceKind.MiniPlayer || kind == SurfaceKind.Panel
            if (!raised) background(fill, shape) else {
                val shade = ColorEngine.blend(fill.copy(alpha = 1f), Color.Black, 0.5f)
                dropShadow(
                    shape,
                    Shadow(radius = 12.dp, color = Color.White, spread = 0.dp, offset = DpOffset((-5).dp, (-5).dp), alpha = (0.14f * depth).coerceIn(0f, 1f)),
                ).dropShadow(
                    shape,
                    Shadow(radius = 16.dp, color = shade, spread = 0.dp, offset = DpOffset(7.dp, 8.dp), alpha = (0.85f * depth).coerceIn(0f, 1f)),
                ).background(fill, shape).innerShadow(
                    shape,
                    Shadow(radius = 5.dp, color = Color.White, spread = 0.dp, offset = DpOffset(2.dp, 2.dp), alpha = (0.5f * depth).coerceIn(0f, 1f)),
                )
            }
        }
        ThemeStyle.Glass -> background(fill, shape).background(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent)), shape,
        ).let {
            if (!bordered) it else it.border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.08f)),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
                shape,
            )
        }
    }
}

/** A pill-shaped chip: filled with the accent color when [selected], a themed chip surface otherwise. */
@Composable
fun Modifier.chipSurface(selected: Boolean): Modifier {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(Tokens.radiusChipLarge.dp.scaled())
    return if (selected) background(AppTheme.colors.accent, shape).clip(shape)
    else appSurface(SurfaceKind.Chip, shape).clip(shape)
}
