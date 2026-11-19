package app.streamvibely.ui.nowplaying

import app.streamvibely.theme.AppTheme
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.Tokens

/** Formats milliseconds as "m:ss". Negative values are treated as zero. */
fun formatTime(ms: Long): String {
    val totalSeconds = (if (ms < 0) 0 else ms) / 1000
    val seconds = totalSeconds % 60
    return "${totalSeconds / 60}:${if (seconds < 10) "0" else ""}$seconds"
}

/**
 * Tappable and draggable timeline. While dragging, the knob follows the finger and [onSeek]
 * is called once on release; a tap seeks immediately.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentDuration by rememberUpdatedState(durationMs)
    // Negative means "not dragging". Read only in the draw phase, so dragging redraws without recomposing.
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val trackColor = AppTheme.colors.sliderTrack
    val playedColor = AppTheme.colors.accent

    Canvas(
        modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    currentOnSeek((f * currentDuration).toLong())
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> dragFraction = (offset.x / size.width).coerceIn(0f, 1f) },
                    onHorizontalDrag = { change, delta ->
                        change.consume()
                        dragFraction = (dragFraction + delta / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        if (dragFraction >= 0f) currentOnSeek((dragFraction * currentDuration).toLong())
                        dragFraction = -1f
                    },
                    onDragCancel = { dragFraction = -1f },
                )
            },
    ) {
        val fraction = when {
            dragFraction >= 0f -> dragFraction
            durationMs > 0 -> (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
            else -> 0f
        }
        val trackHeight = 5.dp.toPx()
        val radius = CornerRadius(trackHeight / 2)
        val top = (size.height - trackHeight) / 2
        val playedWidth = size.width * fraction
        drawRoundRect(trackColor, Offset(0f, top), Size(size.width, trackHeight), radius)
        drawRoundRect(playedColor, Offset(0f, top), Size(playedWidth, trackHeight), radius)
        drawCircle(playedColor, radius = 6.5.dp.toPx(), center = Offset(playedWidth, size.height / 2))
    }
}
