package app.streamvibely.ui.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens in the style of Material 3 Expressive.
 *
 * Spatial springs (movement, size, shape) have a little bounce. Effects springs (color, alpha)
 * never overshoot, because overshooting an alpha or a color looks like a glitch.
 *
 * The helpers are generic so one call fits any animated type:
 * `animateFloatAsState(x, StreamvibelyMotion.spatialDefault())`,
 * `slideInVertically(StreamvibelyMotion.spatialDefault())` (IntOffset), and so on.
 */
object StreamvibelyMotion {

    /** Fast, bouncy spring for small movements and size changes, such as a press. */
    fun <T> spatialFast(): SpringSpec<T> = spring(dampingRatio = 0.6f, stiffness = 1400f)

    /** Default bouncy spring for movement, size and shape morphs. */
    fun <T> spatialDefault(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 700f)

    /** Slow, bouncy spring for large movements such as full-screen elements. */
    fun <T> spatialSlow(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 300f)

    /** Fast spring without bounce for color and alpha changes. */
    fun <T> effectsFast(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)

    /** Default spring without bounce for color and alpha changes. */
    fun <T> effectsDefault(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)

    /** Slow spring without bounce for color and alpha changes. */
    fun <T> effectsSlow(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)

    /** Standard durations in milliseconds, for the few animations that need a tween. */
    object Duration {
        const val Short = 150
        const val Medium = 300
        const val Long = 500
    }
}
