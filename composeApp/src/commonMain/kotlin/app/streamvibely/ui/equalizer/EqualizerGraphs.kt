package app.streamvibely.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import app.streamvibely.domain.EqualizerState
import app.streamvibely.domain.SPECTRUM_BINS
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/** Horizontal and vertical padding inside every plot, shared so curve and spectrum line up. */
internal val PlotInset = 16.dp
private val HitRadius = 28.dp
private const val CurvePoints = 160

/** Maps frequency and gain to pixels (and back) for one plot. */
private class PlotGeometry(
    val width: Float,
    val height: Float,
    val inset: Float,
    val minDb: Float,
    val maxDb: Float,
) {
    private val plotWidth get() = width - 2 * inset
    private val plotHeight get() = height - 2 * inset
    fun x(hz: Float) = inset + frequencyToFraction(hz) * plotWidth
    fun y(db: Float) = inset + (1f - ((db - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)) * plotHeight
    fun hzAt(x: Float) = fractionToFrequency(((x - inset) / plotWidth).coerceIn(0f, 1f))
    fun dbAt(y: Float) = maxDb - ((y - inset) / plotHeight).coerceIn(0f, 1f) * (maxDb - minDb)
}

/** A clock that only invalidates the draw phase of whoever reads it there. */
@Composable
internal fun rememberFrameTick(): State<Long> {
    val tick = remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) androidx.compose.runtime.withFrameNanos { tick.value = it }
    }
    return tick
}

/** Response curve with one draggable node per band. Double tap on empty space adds a band there. */
@Composable
internal fun ParametricGraph(
    state: EqualizerState,
    selectedId: Int?,
    onSelect: (Int) -> Unit,
    onBandChange: (EqBand) -> Unit,
    onAddAt: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragging by remember { mutableStateOf<EqBand?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    val bands = remember(state.bands, dragging) {
        val d = dragging
        if (d == null) state.bands else state.bands.map { if (it.id == d.id) d else it }
    }
    LaunchedEffect(state.bands) { if (!isDragging) dragging = null }
    val curve = remember(bands) {
        FloatArray(CurvePoints) { i -> responseDb(bands, fractionToFrequency(i / (CurvePoints - 1f))) }
    }

    val stateRef by rememberUpdatedState(state)
    val bandsRef by rememberUpdatedState(bands)
    val selectRef by rememberUpdatedState(onSelect)
    val changeRef by rememberUpdatedState(onBandChange)
    val addRef by rememberUpdatedState(onAddAt)

    val measurer = rememberTextMeasurer()
    val colors = rememberEqColors()
    val axisStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = colors.muted)
    val popupStyle = MaterialTheme.typography.labelMedium.copy(color = colors.text)

    Canvas(
        modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val s = stateRef
                    val geo = PlotGeometry(size.width.toFloat(), size.height.toFloat(), PlotInset.toPx(), s.minGainDb, s.maxGainDb)
                    val radius = HitRadius.toPx()
                    val hit = bandsRef
                        .map { it to Offset(geo.x(it.frequencyHz), geo.y(it.gainDb)) }
                        .filter { (_, p) -> (p - down.position).getDistance() <= radius }
                        .minByOrNull { (_, p) -> (p - down.position).getDistance() }
                        ?: return@awaitEachGesture
                    selectRef(hit.first.id)
                    val grab = hit.second - down.position
                    var last = hit.first
                    dragging = last
                    isDragging = true
                    drag(down.id) { change ->
                        change.consume()
                        val p = change.position + grab
                        val hz = geo.hzAt(p.x).roundToInt().toFloat().coerceIn(s.minFrequencyHz, s.maxFrequencyHz)
                        val db = (geo.dbAt(p.y) * 10f).roundToInt() / 10f
                        val gain = db.coerceIn(s.minGainDb, s.maxGainDb)
                        if (hz != last.frequencyHz || gain != last.gainDb) {
                            last = last.copy(frequencyHz = hz, gainDb = gain)
                            dragging = last
                            changeRef(last)
                        }
                    }
                    isDragging = false
                    if (stateRef.bands.any { it == last }) dragging = null
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { offset ->
                    val s = stateRef
                    val geo = PlotGeometry(size.width.toFloat(), size.height.toFloat(), PlotInset.toPx(), s.minGainDb, s.maxGainDb)
                    addRef(geo.hzAt(offset.x).roundToInt().toFloat())
                })
            },
    ) {
        val geo = PlotGeometry(size.width, size.height, PlotInset.toPx(), state.minGainDb, state.maxGainDb)
        val hairline = 1.dp.toPx()
        val enabledAlpha = if (state.enabled) 1f else 0.5f

        for (f in AxisLabelsHz) {
            val x = geo.x(f)
            drawLine(colors.grid, Offset(x, 0f), Offset(x, size.height), hairline)
        }
        val step = if (size.height < 200.dp.toPx()) 10f else 5f
        var db = ceil(state.minGainDb / step) * step
        while (db <= state.maxGainDb) {
            val y = geo.y(db)
            drawLine(if (db == 0f) colors.gridStrong else colors.grid, Offset(0f, y), Offset(size.width, y), hairline)
            val label = measurer.measure((if (db > 0f) "+" else "") + db.roundToInt(), axisStyle)
            drawText(label, topLeft = Offset(4.dp.toPx(), y - label.size.height - 1.dp.toPx()))
            db += step
        }

        // Bandwidth ribbon of the selected band.
        val sel = bands.firstOrNull { it.id == selectedId }
        if (sel != null) {
            val f = sel.frequencyHz
            val (lo, hi) = when (sel.type) {
                EqBandType.Peaking -> {
                    val half = bandwidthOctaves(sel.q) / 2f
                    f * 2f.pow(-half) to f * 2f.pow(half)
                }
                EqBandType.LowShelf -> AxisMinHz to f
                EqBandType.HighShelf -> f to AxisMaxHz
            }
            val x1 = geo.x(lo)
            val x2 = geo.x(hi)
            val tint = colors.bandColor(sel.id)
            drawRect(tint.copy(alpha = 0.13f * enabledAlpha), Offset(x1, 0f), Size(max(x2 - x1, 2.dp.toPx()), size.height))
            drawLine(tint.copy(alpha = 0.45f * enabledAlpha), Offset(x1, 0f), Offset(x1, size.height), hairline)
            drawLine(tint.copy(alpha = 0.45f * enabledAlpha), Offset(x2, 0f), Offset(x2, size.height), hairline)
        }

        val zeroY = geo.y(0f)
        val line = Path()
        val fill = Path().apply { moveTo(geo.x(AxisMinHz), zeroY) }
        for (i in 0 until CurvePoints) {
            val x = geo.inset + i / (CurvePoints - 1f) * (size.width - 2 * geo.inset)
            val y = geo.y(curve[i])
            if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
            fill.lineTo(x, y)
        }
        fill.lineTo(geo.x(AxisMaxHz), zeroY)
        fill.close()
        drawPath(fill, colors.accent.copy(alpha = 0.18f * enabledAlpha))
        drawPath(line, colors.accent.copy(alpha = enabledAlpha), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        for (band in bands.sortedBy { it.id == selectedId }) {
            val center = Offset(geo.x(band.frequencyHz), geo.y(band.gainDb))
            val selected = band.id == selectedId
            val tint = if (band.enabled) colors.bandColor(band.id) else colors.muted
            if (selected) {
                drawCircle(tint.copy(alpha = 0.28f * enabledAlpha), 22.dp.toPx(), center)
                drawCircle(tint.copy(alpha = enabledAlpha), 12.dp.toPx(), center)
                drawCircle(colors.onAccent.copy(alpha = 0.9f), 4.dp.toPx(), center)
            } else {
                drawCircle(colors.card, 11.dp.toPx(), center)
                drawCircle(tint.copy(alpha = enabledAlpha), 9.dp.toPx(), center)
            }
        }

        val active = dragging
        if (isDragging && active != null) {
            val label = measurer.measure(formatHz(active.frequencyHz) + "  /  " + formatSigned(active.gainDb) + " dB", popupStyle)
            val padX = 8.dp.toPx()
            val padY = 5.dp.toPx()
            val w = label.size.width + 2 * padX
            val h = label.size.height + 2 * padY
            val cx = geo.x(active.frequencyHz)
            val cy = geo.y(active.gainDb)
            val left = (cx - w / 2f).coerceIn(2.dp.toPx(), max(2.dp.toPx(), size.width - w - 2.dp.toPx()))
            val top = if (cy - 30.dp.toPx() - h < 0f) cy + 26.dp.toPx() else cy - 30.dp.toPx() - h
            drawRoundRect(colors.cardHigh, Offset(left, top), Size(w, h), CornerRadius(h / 2f))
            drawRoundRect(colors.accent.copy(alpha = 0.6f), Offset(left, top), Size(w, h), CornerRadius(h / 2f), style = Stroke(hairline))
            drawText(label, topLeft = Offset(left + padX, top + padY))
        }
    }
}

/** Per-frame animation state of the spectrum bars, mutated only while drawing. */
private class SpectrumAnim {
    val level = FloatArray(SPECTRUM_BINS)
    val peak = FloatArray(SPECTRUM_BINS)
    val hold = FloatArray(SPECTRUM_BINS)
    var lastNanos = 0L
    var idle = 1f
}

/** Live spectrum as rounded bars with falling peak caps. [spectrum] is read only while drawing. */
@Composable
internal fun SpectrumGraph(spectrum: () -> FloatArray, modifier: Modifier = Modifier) {
    val tick = rememberFrameTick()
    val anim = remember { SpectrumAnim() }
    val colors = rememberEqColors()
    val measurer = rememberTextMeasurer()
    val caption = MaterialTheme.typography.labelMedium.copy(color = colors.muted)

    Canvas(modifier.clipToBounds()) {
        val now = tick.value
        val dt = if (anim.lastNanos == 0L) 0.016f else ((now - anim.lastNanos) / 1e9f).coerceIn(0f, 0.1f)
        anim.lastNanos = now
        val seconds = now / 1e9f

        val data = spectrum()
        var loudest = 0f
        for (v in data) loudest = max(loudest, v)
        val silent = loudest < 0.02f
        anim.idle += ((if (silent) 1f else 0f) - anim.idle) * min(1f, dt * 4f)
        val idle = anim.idle

        val n = SPECTRUM_BINS
        val inset = PlotInset.toPx()
        val plotWidth = size.width - 2 * inset
        val top = 4.dp.toPx()
        val bottom = size.height - 2.dp.toPx()
        val span = bottom - top
        val slot = plotWidth / n
        val barW = max(1.5.dp.toPx(), slot * 0.66f)
        val corner = CornerRadius(barW / 2f)
        val capH = 2.5.dp.toPx()

        val tail = lerp(colors.accent, colors.cool, 0.8f)
        val barAlpha = 1f - 0.6f * idle
        val brush = Brush.verticalGradient(listOf(colors.accent.copy(alpha = barAlpha), tail.copy(alpha = barAlpha)), top, bottom)
        val capColor = lerp(colors.accent, colors.text, 0.5f)

        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, colors.accent.copy(alpha = 0.16f * (1f - 0.6f * idle))), bottom - span * 0.55f, bottom),
            Offset(0f, bottom - span * 0.55f), Size(size.width, span * 0.55f),
        )
        drawLine(colors.grid, Offset(inset, bottom), Offset(size.width - inset, bottom), 1.dp.toPx())

        clipRect(bottom = bottom) {
            for (i in 0 until n) {
                val target = if (i < data.size) data[i].coerceIn(0f, 1f) else 0f
                val shimmer = (0.07f + 0.05f * (sin(seconds * 1.6f + i * 0.33f) * 0.5f + 0.5f)) * (0.55f + 0.45f * sin(i * 0.19f + seconds * 0.4f))
                val v = max(target, shimmer * idle)
                val l = anim.level[i]
                anim.level[i] = if (v > l) l + (v - l) * min(1f, dt * 30f) else max(v, l - dt * 1.8f)
                if (anim.level[i] >= anim.peak[i]) {
                    anim.peak[i] = anim.level[i]
                    anim.hold[i] = 0.35f
                } else if (anim.hold[i] > 0f) {
                    anim.hold[i] -= dt
                } else {
                    anim.peak[i] = max(anim.level[i], anim.peak[i] - dt * 0.5f)
                }
                val x = inset + i * slot + (slot - barW) / 2f
                val h = max(anim.level[i] * span, barW)
                drawRoundRect(brush, Offset(x, bottom - h), Size(barW, h + barW), corner)
                if (idle < 0.9f) {
                    val capY = bottom - max(anim.peak[i] * span, barW) - capH - 1.5.dp.toPx()
                    drawRoundRect(capColor.copy(alpha = 0.9f * (1f - idle)), Offset(x, capY), Size(barW, capH), CornerRadius(capH / 2f))
                }
            }
        }

        if (idle > 0.02f) {
            val label = measurer.measure("Play something to see it move", caption)
            drawText(
                label,
                color = colors.muted.copy(alpha = 0.9f * idle),
                topLeft = Offset((size.width - label.size.width) / 2f, top + (span * 0.3f - label.size.height) / 2f + 2.dp.toPx()),
            )
        }
    }
}

/** 50, 100, ... 20k under a plot, using the same mapping and inset as the plots. */
@Composable
internal fun FrequencyLabels(modifier: Modifier = Modifier) {
    val measurer = rememberTextMeasurer()
    val colors = rememberEqColors()
    val style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = colors.muted)
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        val inset = PlotInset.toPx()
        for (f in AxisLabelsHz) {
            val layout = measurer.measure(formatHzShort(f), style)
            val x = (inset + frequencyToFraction(f) * (size.width - 2 * inset) - layout.size.width / 2f)
                .coerceIn(0f, max(0f, size.width - layout.size.width))
            drawText(layout, topLeft = Offset(x, 0f))
        }
    }
}
