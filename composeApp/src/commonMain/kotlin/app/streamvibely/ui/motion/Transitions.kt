package app.streamvibely.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Fade-through for switching between unrelated screens or tabs: the old content fades out, then
 * the new content fades in with a slight scale-up. Use as
 * `AnimatedContent(target, transitionSpec = fadeThroughTransition())`.
 */
fun <S> fadeThroughTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    val outDuration = StreamvibelyMotion.Duration.Short
    val inDuration = StreamvibelyMotion.Duration.Medium - outDuration
    (fadeIn(tween(inDuration, delayMillis = outDuration)) +
        scaleIn(StreamvibelyMotion.spatialDefault(), initialScale = 0.92f))
        .togetherWith(fadeOut(tween(outDuration)))
}

/**
 * New content slides up from the bottom edge with a spring while the old content fades out. Use
 * it for screens that open on top of the current one, such as the now playing screen or a
 * detail page.
 */
fun <S> slideUpTransition(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    (slideInVertically(StreamvibelyMotion.spatialDefault()) { it / 6 } +
        fadeIn(StreamvibelyMotion.effectsDefault()))
        .togetherWith(fadeOut(tween(StreamvibelyMotion.Duration.Short)))
}

private const val StaggerStepMillis = 40L
private const val StaggerMaxSteps = 8

/**
 * Fades and lifts an item into place when it first appears, delayed by [index] so a list
 * cascades in. The delay is capped at 8 steps (320 ms), so items far down a long list animate
 * almost immediately. Use it on list rows and grid cards. In a lazy list, an item that scrolls
 * out and back in plays the entrance again, so apply it only to the first screenful, for
 * example `if (index < 12)`.
 */
@Composable
fun Modifier.staggeredEntrance(index: Int): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceIn(0, StaggerMaxSteps) * StaggerStepMillis)
        progress.animateTo(1f, StreamvibelyMotion.spatialSlow())
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p.coerceIn(0f, 1f)
        translationY = (1f - p) * 24.dp.toPx()
    }
}
