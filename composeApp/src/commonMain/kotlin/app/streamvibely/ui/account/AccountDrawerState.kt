package app.streamvibely.ui.account

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.streamvibely.ui.motion.StreamvibelyMotion

/**
 * Position of the account drawer: 0f closed, 1f fully open. A finger pulling past open
 * overshoots a little above 1f (rubber band) and springs back.
 */
@Stable
class AccountDrawerState internal constructor() {
    private val anim = Animatable(0f)

    /** Read this in draw phases (graphicsLayer, drawBehind), never in composition. */
    val progress: Float get() = anim.value.coerceAtLeast(0f)

    /** True while the drawer is open or still animating shut. Safe to read in composition. */
    val isOpen: Boolean by derivedStateOf { anim.value > 0.001f }

    suspend fun open() {
        anim.animateTo(1f, StreamvibelyMotion.spatialDefault())
    }

    suspend fun close() {
        anim.animateTo(0f, StreamvibelyMotion.spatialDefault())
    }

    internal suspend fun snapTo(value: Float) {
        anim.snapTo(value)
    }

    /** Springs to [target] (0f or 1f) carrying the fling [velocity] in progress units per second. */
    internal suspend fun settle(target: Float, velocity: Float) {
        anim.animateTo(target, StreamvibelyMotion.spatialDefault(), initialVelocity = velocity)
    }
}

@Composable
fun rememberAccountDrawerState(): AccountDrawerState = remember { AccountDrawerState() }
