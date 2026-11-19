package app.streamvibely.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radius as a fraction of the button size: 0.5 is a full circle. */
private const val CircleFraction = 0.5f
private const val SquircleFraction = 0.3f

/**
 * Primary-colored play/pause button for the player. The shape springs between a circle (paused)
 * and a rounded square (playing) while the glyph cross-fades. Use it as the main transport
 * control; it also has the press squish from [bounceClickable]. Shape and glyph are animated in
 * the draw phase, so toggling does not recompose or re-layout.
 */
@Composable
fun MorphingPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
) {
    val corner by animateFloatAsState(
        targetValue = if (isPlaying) SquircleFraction else CircleFraction,
        animationSpec = StreamvibelyMotion.spatialDefault(),
        label = "playButtonCorner",
    )
    val playingProgress by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = StreamvibelyMotion.effectsDefault(),
        label = "playButtonGlyph",
    )
    val container = MaterialTheme.colorScheme.primary
    val glyph = MaterialTheme.colorScheme.onPrimary

    Canvas(
        modifier = modifier
            .size(size)
            .bounceClickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" }
            .graphicsLayer {
                // Spring overshoot is clamped so the corner never exceeds a full circle.
                shape = RoundedCornerShape(CornerSize(this.size.minDimension * corner.coerceIn(0f, CircleFraction)))
                clip = true
            }
            .background(container),
    ) {
        val s = this.size.minDimension
        val glyphSize = s * 0.4f
        val left = (this.size.width - glyphSize) / 2f
        val top = (this.size.height - glyphSize) / 2f
        val fadeOut = (1f - playingProgress).coerceIn(0f, 1f)
        val fadeIn = playingProgress.coerceIn(0f, 1f)

        if (fadeOut > 0f) {
            // Triangle nudged right so its visual center sits in the middle of the button.
            val nudge = glyphSize * 0.08f
            val triangle = Path().apply {
                moveTo(left + nudge, top)
                lineTo(left + nudge + glyphSize * 0.9f, top + glyphSize / 2f)
                lineTo(left + nudge, top + glyphSize)
                close()
            }
            drawPath(triangle, color = glyph, alpha = fadeOut)
        }
        if (fadeIn > 0f) {
            val barWidth = glyphSize * 0.32f
            val radius = CornerRadius(barWidth * 0.2f)
            val bar = Size(barWidth, glyphSize)
            drawRoundRect(glyph, Offset(left + glyphSize * 0.05f, top), bar, radius, alpha = fadeIn)
            drawRoundRect(glyph, Offset(left + glyphSize * 0.95f - barWidth, top), bar, radius, alpha = fadeIn)
        }
    }
}
