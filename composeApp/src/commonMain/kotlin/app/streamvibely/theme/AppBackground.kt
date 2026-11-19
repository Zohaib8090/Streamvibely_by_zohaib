package app.streamvibely.theme

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.ThemeStyle
import app.streamvibely.theme.engine.ColorEngine

/**
 * The page background for the current surface style. Pages are drawn transparent on top of it.
 * [backdrop] (for example the cover of the playing song) is only used by Glass.
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    backdrop: ImageBitmap? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val c = AppTheme.colors
    val spec = LocalThemeSpec.current
    val base = when (c.style) {
        ThemeStyle.Material -> Modifier.background(c.background)
        ThemeStyle.Oled -> Modifier.background(Color.Black)
        ThemeStyle.Clay -> Modifier.background(
            Brush.verticalGradient(listOf(ColorEngine.blend(c.background, c.onBackground, 0.05f), c.background)),
        )
        ThemeStyle.Glass -> Modifier.background(c.background)
    }
    Box(modifier.fillMaxSize().then(base)) {
        if (c.style == ThemeStyle.Glass) GlassBackdrop(c, spec.glassBlurDp, backdrop)
        content()
    }
}

@Composable
private fun BoxScope.GlassBackdrop(c: AppColors, blurDp: Float, backdrop: ImageBitmap?) {
    val second = LocalGeneratedColors.current?.tertiary ?: ColorEngine.blend(c.accent, c.onBackground, 0.4f)
    val third = LocalGeneratedColors.current?.secondary ?: c.accentContainer
    // blur() does nothing before Android 12, so the gradients always stay underneath.
    val layer = if (blurDp > 0f) Modifier.fillMaxSize().blur(blurDp.dp) else Modifier.fillMaxSize()
    Box(
        layer.drawBehind {
            val w = size.width
            val h = size.height
            val r = maxOf(w, h)
            fun glow(color: Color, cx: Float, cy: Float, radius: Float, alpha: Float) = drawRect(
                Brush.radialGradient(
                    listOf(color.copy(alpha = alpha), Color.Transparent),
                    center = Offset(w * cx, h * cy),
                    radius = radius,
                ),
            )
            glow(c.accent, 0.15f, 0.12f, r * 0.8f, 0.55f)
            glow(second, 0.9f, 0.35f, r * 0.7f, 0.45f)
            glow(third, 0.3f, 0.95f, r * 0.8f, 0.4f)
            glow(c.accentContainer, 0.8f, 0.9f, r * 0.6f, 0.35f)
        },
    ) {
        if (backdrop != null) {
            Image(backdrop, null, Modifier.fillMaxSize().blur(48.dp), contentScale = ContentScale.Crop, alpha = 0.9f)
        }
    }
    // Keeps text readable over the colorful backdrop.
    Box(Modifier.fillMaxSize().background(c.background.copy(alpha = 0.25f)))
}
