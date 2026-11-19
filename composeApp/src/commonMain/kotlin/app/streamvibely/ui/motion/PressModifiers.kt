package app.streamvibely.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

/**
 * Shrinks the element to [pressedScale] while [interactionSource] reports a press, and springs
 * back on release. Use it on anything tappable that already owns an interaction source (a
 * Material button, a custom clickable). The scale is read in the graphics layer, so a press
 * does not trigger recomposition or layout.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.96f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = StreamvibelyMotion.spatialFast(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * A clickable with the springy press scale and no ripple. Use it for cards, tiles and list rows
 * where the squish is the only press feedback. Pass [role] for accessibility, for example
 * `Role.Button`.
 */
@Composable
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    role: Role? = null,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .pressScale(interactionSource, pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}
