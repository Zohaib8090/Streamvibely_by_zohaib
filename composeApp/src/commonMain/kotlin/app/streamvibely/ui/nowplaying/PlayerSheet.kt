package app.streamvibely.ui.nowplaying

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.ui.motion.StreamvibelyMotion
import kotlinx.coroutines.launch

/** Fraction of the expanded area, from the top, where a downward drag collapses the sheet. */
private const val CollapseRegionFraction = 0.35f

/** Fling speed (px/s) above which the direction of the fling decides, ignoring the position. */
private const val FlingVelocity = 700f

/** Share of the drag over which the full content fades in and the mini fades out. */
private const val FadeRange = 0.3f

private val MiniLift = 12.dp

/**
 * Expandable player: a mini player that opens into the full-screen player and back, like Spotify.
 *
 * Place it in a `Box` that fills the area above the bottom navigation. Collapsed, only [mini] is
 * drawn, at the bottom edge. Expanded, [full] fills the whole area. In between, [full] slides up
 * from the bottom while it fades in and [mini] fades out.
 *
 * Gesture rules:
 * - A vertical drag that starts anywhere on the mini player moves the sheet with the finger.
 * - When expanded, a vertical drag that starts in the top 35% of the area moves the sheet. Drags
 *   that start lower are left alone, so the seek bar, lyrics scrolling and buttons keep working.
 * - A drag is ignored when a child (a slider, a scrollable list) has already consumed it.
 * - Taps are not intercepted: the drag only starts after touch slop, so the mini's own buttons keep
 *   their clicks. A tap on the mini that no child consumed calls `onExpandedChange(true)`.
 * - On release the fling direction decides when the speed is above about 700 px/s; otherwise the
 *   sheet goes to whichever side of the halfway point it is on. It then springs to rest with
 *   [StreamvibelyMotion.spatialDefault] and `onExpandedChange` is called once with the new state.
 *
 * [expanded] is the source of truth for the resting state: when it changes from outside (for
 * example the Back button), the sheet animates to it. [full] is composed only while the sheet is
 * not fully collapsed, and [mini] only while it is not fully expanded. The progress is read in
 * the draw phase, so dragging does not recompose. The caller handles window insets.
 */
@Composable
fun PlayerSheet(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    mini: @Composable () -> Unit,
    full: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    miniHeight: Dp = 64.dp,
    /** Space kept free below the collapsed mini player, for example a bottom navigation bar that stays tappable. */
    miniBottomInset: Dp = 0.dp,
    /** Called when the collapsed mini player is swiped down far enough: stop playback and hide it. Null disables the gesture. */
    onDismiss: (() -> Unit)? = null,
) {
    // 0f collapsed .. 1f expanded. Bounded so the spring never overshoots past either end.
    val progress = remember { Animatable(if (expanded) 1f else 0f).apply { updateBounds(0f, 1f) } }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    // How far (px) the collapsed mini has been pulled down; used for swipe-down-to-close.
    val dismissY = remember { Animatable(0f) }

    LaunchedEffect(expanded) {
        val target = if (expanded) 1f else 0f
        if (progress.targetValue != target) {
            progress.animateTo(target, StreamvibelyMotion.spatialDefault())
        }
    }

    val showFull by remember { derivedStateOf { progress.value > 0f } }
    val showMini by remember { derivedStateOf { progress.value < 1f } }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val areaPx = constraints.maxHeight.toFloat()
        val miniPx = with(density) { (miniHeight + miniBottomInset).toPx() }
        val rangePx by rememberUpdatedState((areaPx - miniPx).coerceAtLeast(1f))
        val liftPx = with(density) { MiniLift.toPx() }

        // Shared drag state lives in the callbacks below; drag[0] mirrors the finger.
        val drag = remember { floatArrayOf(0f) }
        val onDragStart = {
            drag[0] = progress.value
            scope.launch { progress.stop(); dismissY.stop() }
            Unit
        }
        val onDrag = { dy: Float ->
            val canDismiss = currentOnDismiss != null && progress.value <= 0f && drag[0] <= 0f
            if (canDismiss && (dy > 0f || dismissY.value > 0f)) {
                val v = (dismissY.value + dy).coerceIn(0f, miniPx)
                scope.launch { dismissY.snapTo(v) }
            } else {
                drag[0] = (drag[0] - dy / rangePx).coerceIn(0f, 1f)
                val value = drag[0]
                scope.launch { progress.snapTo(value) }
            }
            Unit
        }
        val onDragEnd: (Float) -> Unit = { velocityY ->
            if (dismissY.value > 0f) {
                val close = dismissY.value > miniPx * 0.4f || velocityY > 900f
                scope.launch {
                    if (close) {
                        dismissY.animateTo(miniPx, StreamvibelyMotion.spatialFast())
                        currentOnDismiss?.invoke()
                        dismissY.snapTo(0f)
                    } else {
                        dismissY.animateTo(0f, StreamvibelyMotion.spatialDefault())
                    }
                }
            } else {
            val target = when {
                velocityY < -FlingVelocity -> true
                velocityY > FlingVelocity -> false
                else -> drag[0] > 0.5f
            }
            scope.launch {
                progress.animateTo(
                    targetValue = if (target) 1f else 0f,
                    animationSpec = StreamvibelyMotion.spatialDefault(),
                    initialVelocity = -velocityY / rangePx,
                )
            }
            currentOnExpandedChange(target)
            }
        }

        if (showFull) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val p = progress.value.coerceIn(0f, 1f)
                        translationY = (1f - p) * (size.height - miniPx)
                        alpha = (p / FadeRange).coerceIn(0f, 1f)
                    }
                    .sheetDrag(CollapseRegionFraction, onDragStart, onDrag, onDragEnd)
                    .background(MaterialTheme.colorScheme.surface),
            ) { full() }
        }

        if (showMini) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = miniBottomInset)
                    .fillMaxWidth()
                    .height(miniHeight)
                    .graphicsLayer {
                        val p = progress.value.coerceIn(0f, 1f)
                        alpha = (1f - p / FadeRange).coerceIn(0f, 1f) * (1f - 0.9f * (dismissY.value / miniPx))
                        translationY = -p * liftPx + dismissY.value
                    }
                    .sheetDrag(1f, onDragStart, onDrag, onDragEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { currentOnExpandedChange(true) },
            ) { mini() }
        }
    }
}

/**
 * Vertical drag for the sheet, limited to the top [regionFraction] of this element. It uses a
 * custom gesture loop rather than `detectVerticalDragGestures` so a press outside the region is
 * never consumed, and a gesture a child already consumed (returns null from the slop check) is
 * dropped. Nothing is consumed before touch slop, so taps reach clickable children.
 */
private fun Modifier.sheetDrag(
    regionFraction: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: (Float) -> Unit,
): Modifier = pointerInput(regionFraction) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (down.position.y > size.height * regionFraction) return@awaitEachGesture
        val tracker = VelocityTracker()
        tracker.addPointerInputChange(down)
        val slopChange = awaitVerticalTouchSlopOrCancellation(down.id) { change, overSlop ->
            onDragStart()
            tracker.addPointerInputChange(change)
            change.consume()
            onDrag(overSlop)
        } ?: return@awaitEachGesture
        verticalDrag(slopChange.id) { change ->
            tracker.addPointerInputChange(change)
            onDrag(change.positionChange().y)
            change.consume()
        }
        onDragEnd(tracker.calculateVelocity().y)
    }
}
