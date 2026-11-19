package app.streamvibely.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqualizerState
import app.streamvibely.theme.Tokens
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlinx.coroutines.delay

private val ValueRowH = 24.dp
private val SliderH = 220.dp
private val LabelH = 34.dp
private val SliderPad = 18.dp
private val ScaleW = 34.dp
private val ScrollColumnW = 56.dp
private const val MaxFlexBands = 12
private const val CurveSteps = 8
private val DbMarks = intArrayOf(15, 10, 5, 0, -5, -10, -15)

private fun sliderY(db: Float, minDb: Float, maxDb: Float, top: Float, height: Float, pad: Float): Float =
    top + pad + (1f - ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)) * (height - 2 * pad)

private fun gainAt(y: Float, height: Float, pad: Float, minDb: Float, maxDb: Float): Float {
    val fraction = ((y - pad) / (height - 2 * pad)).coerceIn(0f, 1f)
    val gain = snap(maxDb - fraction * (maxDb - minDb), 0.1f).coerceIn(minDb, maxDb)
    return if (abs(gain) < 0.3f) 0f else gain
}

/**
 * One vertical slider per band, spread over the full width (scrolls sideways beyond 12 bands),
 * with a dB scale on the left and the combined response curve drawn behind the sliders.
 */
@Composable
internal fun GraphicBands(
    state: EqualizerState,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onBandChange: (EqBand) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sorted = remember(state.bands) { state.bands.sortedBy { it.frequencyHz } }
    val scrolls = sorted.size > MaxFlexBands
    val scroll = rememberScrollState()
    val total = ValueRowH + SliderH + LabelH
    Row(modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
        ScaleGutter(state.minGainDb, state.maxGainDb, Modifier.width(ScaleW).height(total))
        Box(Modifier.weight(1f).then(if (scrolls) Modifier.horizontalScroll(scroll) else Modifier)) {
            Box(if (scrolls) Modifier.width(ScrollColumnW * sorted.size) else Modifier.fillMaxWidth()) {
                Backdrop(sorted, state, Modifier.matchParentSize())
                Row(Modifier.fillMaxWidth()) {
                    sorted.forEach { band ->
                        key(band.id) {
                            GraphicColumn(band, state, band.id == selectedId, { onSelect(band.id) }, onBandChange, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleGutter(minDb: Float, maxDb: Float, modifier: Modifier) {
    val colors = rememberEqColors()
    val measurer = rememberTextMeasurer()
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = colors.muted)
    Canvas(modifier) {
        val top = ValueRowH.toPx()
        val h = SliderH.toPx()
        val pad = SliderPad.toPx()
        for (mark in DbMarks) {
            if (mark < minDb || mark > maxDb) continue
            val label = measurer.measure((if (mark > 0) "+" else "") + mark, style)
            val y = sliderY(mark.toFloat(), minDb, maxDb, top, h, pad)
            drawText(label, topLeft = Offset(size.width - label.size.width - 4.dp.toPx(), y - label.size.height / 2f))
        }
    }
}

/** Grid lines and the combined response curve, in the same coordinates as the sliders. */
@Composable
private fun Backdrop(bands: List<EqBand>, state: EqualizerState, modifier: Modifier) {
    val colors = rememberEqColors()
    val n = bands.size
    val samples = remember(bands) {
        if (n < 2) FloatArray(0) else FloatArray((n - 1) * CurveSteps + 1) { j ->
            val i = min(j / CurveSteps, n - 2)
            val t = (j - i * CurveSteps) / CurveSteps.toFloat()
            val a = bands[i].frequencyHz
            val b = bands[i + 1].frequencyHz
            responseDb(bands, a * (b / a).pow(t))
        }
    }
    val enabledAlpha = if (state.enabled) 1f else 0.5f
    Canvas(modifier) {
        val top = ValueRowH.toPx()
        val h = SliderH.toPx()
        val pad = SliderPad.toPx()
        val hairline = 1.dp.toPx()
        for (mark in DbMarks) {
            if (mark < state.minGainDb || mark > state.maxGainDb) continue
            val y = sliderY(mark.toFloat(), state.minGainDb, state.maxGainDb, top, h, pad)
            drawLine(if (mark == 0) colors.gridStrong else colors.grid, Offset(0f, y), Offset(size.width, y), hairline)
        }
        if (samples.isEmpty()) return@Canvas
        val zeroY = sliderY(0f, state.minGainDb, state.maxGainDb, top, h, pad)
        val line = Path()
        val fill = Path()
        for (j in samples.indices) {
            val x = (0.5f + j / CurveSteps.toFloat()) / n * size.width
            val y = sliderY(samples[j], state.minGainDb, state.maxGainDb, top, h, pad)
            if (j == 0) {
                line.moveTo(x, y)
                fill.moveTo(x, zeroY)
                fill.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo((0.5f + (samples.size - 1) / CurveSteps.toFloat()) / n * size.width, zeroY)
        fill.close()
        drawPath(fill, colors.accent.copy(alpha = 0.12f * enabledAlpha))
        drawPath(line, colors.accent.copy(alpha = 0.55f * enabledAlpha), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun GraphicColumn(
    band: EqBand,
    state: EqualizerState,
    selected: Boolean,
    onSelect: () -> Unit,
    onBandChange: (EqBand) -> Unit,
    modifier: Modifier,
) {
    val colors = rememberEqColors()
    val shape = eqShape(Tokens.radiusSm)
    var local by remember { mutableStateOf<Float?>(null) }
    var released by remember { mutableStateOf(0) }
    LaunchedEffect(released) {
        if (released > 0) {
            delay(250)
            local = null
        }
    }
    val shown = local ?: band.gainDb
    val valueStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
    Column(
        modifier
            .alpha(if (band.enabled) 1f else 0.45f)
            .padding(horizontal = 1.dp)
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.13f) else Color.Transparent, shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(ValueRowH).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                formatSigned(shown),
                style = valueStyle,
                color = if (selected) colors.accent else colors.muted,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentWidth(unbounded = true),
            )
        }
        BandSlider(
            gain = shown,
            minDb = state.minGainDb,
            maxDb = state.maxGainDb,
            colors = colors,
            onSelect = onSelect,
            onGain = { g ->
                if (g != shown) {
                    local = g
                    onBandChange(band.copy(gainDb = g))
                }
            },
            onRelease = { released++ },
        )
        Box(Modifier.height(LabelH).fillMaxWidth().clickable(onClick = onSelect), contentAlignment = Alignment.Center) {
            Text(
                formatHzShort(band.frequencyHz),
                style = labelStyle,
                color = if (selected) colors.accent else colors.muted,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.wrapContentWidth(unbounded = true),
            )
        }
    }
}

@Composable
private fun BandSlider(
    gain: Float,
    minDb: Float,
    maxDb: Float,
    colors: EqColors,
    onSelect: () -> Unit,
    onGain: (Float) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectRef by rememberUpdatedState(onSelect)
    val gainRef by rememberUpdatedState(onGain)
    val releaseRef by rememberUpdatedState(onRelease)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(SliderH)
            .pointerInput(minDb, maxDb) {
                detectTapGestures(
                    onDoubleTap = {
                        selectRef()
                        gainRef(0f)
                        releaseRef()
                    },
                    onTap = { offset ->
                        selectRef()
                        gainRef(gainAt(offset.y, size.height.toFloat(), SliderPad.toPx(), minDb, maxDb))
                        releaseRef()
                    },
                )
            }
            .pointerInput(minDb, maxDb) {
                detectVerticalDragGestures(
                    onDragStart = { selectRef() },
                    onDragEnd = { releaseRef() },
                    onDragCancel = { releaseRef() },
                ) { change, _ ->
                    change.consume()
                    gainRef(gainAt(change.position.y, size.height.toFloat(), SliderPad.toPx(), minDb, maxDb))
                }
            },
    ) {
        val pad = SliderPad.toPx()
        val cx = size.width / 2f
        val trackW = min(8.dp.toPx(), size.width * 0.6f)
        val round = CornerRadius(trackW / 2f)
        val zeroY = sliderY(0f, minDb, maxDb, 0f, size.height, pad)
        val thumbY = sliderY(gain, minDb, maxDb, 0f, size.height, pad)
        drawRoundRect(colors.track, Offset(cx - trackW / 2f, pad), Size(trackW, size.height - 2 * pad), round)
        val boost = gain >= 0f
        val tint = if (boost) colors.accent else colors.cool
        val topY = min(zeroY, thumbY)
        val fillH = abs(zeroY - thumbY)
        if (fillH > 0.5f) {
            val brush = if (boost) Brush.verticalGradient(listOf(colors.accent, colors.accent.copy(alpha = 0.5f)), topY, zeroY)
            else Brush.verticalGradient(listOf(colors.cool.copy(alpha = 0.5f), colors.cool), zeroY, thumbY)
            drawRoundRect(brush, Offset(cx - trackW / 2f, topY), Size(trackW, fillH), round)
        }
        drawLine(colors.gridStrong, Offset(cx - 10.dp.toPx(), zeroY), Offset(cx + 10.dp.toPx(), zeroY), 1.5.dp.toPx())
        val r = min(14.dp.toPx(), size.width / 2f)
        drawCircle(tint.copy(alpha = 0.22f), r + 4.dp.toPx(), Offset(cx, thumbY))
        drawCircle(tint, r, Offset(cx, thumbY))
        drawCircle(colors.onAccent.copy(alpha = 0.85f), r * 0.28f, Offset(cx, thumbY))
    }
}

