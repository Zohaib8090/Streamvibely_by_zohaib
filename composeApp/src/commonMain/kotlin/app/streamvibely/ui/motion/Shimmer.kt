package app.streamvibely.ui.motion

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp

private const val ShimmerPeriodMillis = 1400
private const val HighlightStrength = 0.1f

/**
 * Loading placeholder: fills the element with the theme's surfaceContainerHigh color and sweeps
 * a soft highlight across it. Put it on an empty `Box` sized like the content it stands in for
 * (artwork square, text line). The sweep is read in the draw phase only, so it costs no
 * recomposition or layout per frame.
 */
@Composable
fun Modifier.shimmer(shape: Shape = MaterialTheme.shapes.small): Modifier {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = lerp(base, MaterialTheme.colorScheme.onSurface, HighlightStrength)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(ShimmerPeriodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerProgress",
    )
    return this
        .clip(shape)
        .drawBehind {
            drawRect(base)
            val startX = -size.width + progress * 2f * size.width
            drawRect(
                Brush.horizontalGradient(
                    colors = listOf(base, highlight, base),
                    startX = startX,
                    endX = startX + size.width,
                ),
            )
        }
}
