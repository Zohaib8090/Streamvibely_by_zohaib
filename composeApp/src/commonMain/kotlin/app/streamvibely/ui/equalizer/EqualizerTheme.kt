package app.streamvibely.ui.equalizer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled

/** Theme colors captured once so draw lambdas (which cannot read composition locals) can use them. */
@Immutable
internal class EqColors(
    val accent: Color,
    val onAccent: Color,
    /** Cooler contrast color: used for cuts and as the second stop of gradients. */
    val cool: Color,
    val text: Color,
    val muted: Color,
    val card: Color,
    val cardHigh: Color,
    val track: Color,
    val grid: Color,
    val gridStrong: Color,
    val bandPalette: List<Color>,
) {
    fun bandColor(id: Int): Color = bandPalette[((id % bandPalette.size) + bandPalette.size) % bandPalette.size]
}

@Composable
internal fun rememberEqColors(): EqColors {
    val c = AppTheme.colors
    val scheme = MaterialTheme.colorScheme
    return remember(c, scheme.tertiary, scheme.secondary) {
        val cool = scheme.tertiary
        val second = scheme.secondary
        EqColors(
            accent = c.accent,
            onAccent = c.onAccent,
            cool = cool,
            text = c.onCard,
            muted = c.muted,
            card = c.card,
            cardHigh = c.cardHigh,
            track = c.sliderTrack,
            grid = c.outlineVariant.copy(alpha = 0.55f),
            gridStrong = c.outline.copy(alpha = 0.8f),
            bandPalette = listOf(
                c.accent, cool, second,
                lerp(c.accent, cool, 0.5f), lerp(second, c.accent, 0.5f), lerp(cool, second, 0.5f),
            ),
        )
    }
}

/** Themed card surface; clips its content to [radius] (scaled by the theme's corner scale). */
@Composable
internal fun Modifier.eqCard(radius: Int = Tokens.radiusCard, kind: SurfaceKind = SurfaceKind.Card): Modifier {
    val shape = RoundedCornerShape(radius.dp.scaled())
    return appSurface(kind, shape).clip(shape)
}

@Composable
internal fun eqShape(radius: Int): RoundedCornerShape = RoundedCornerShape(radius.dp.scaled())
