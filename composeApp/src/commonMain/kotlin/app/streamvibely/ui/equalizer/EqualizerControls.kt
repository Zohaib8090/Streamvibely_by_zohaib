package app.streamvibely.ui.equalizer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import app.streamvibely.domain.EqualizerState
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.Tokens
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons
import kotlin.math.roundToInt

/** Pill-shaped on/off switch drawn from theme colors. */
@Composable
internal fun PillSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = rememberEqColors()
    val t by animateFloatAsState(if (checked) 1f else 0f)
    Box(
        modifier
            .size(width = 52.dp, height = 30.dp)
            .clip(CircleShape)
            .background(lerp(colors.track, colors.accent, t))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = 22.dp * t)
                .size(24.dp)
                .clip(CircleShape)
                .background(lerp(colors.muted, colors.onAccent, t)),
        )
    }
}

/** Round icon-only button; the description doubles as its accessibility label and tooltip text. */
@Composable
internal fun IconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = AppTheme.colors.muted,
) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(icon, description, tint = if (enabled) tint else tint.copy(alpha = 0.35f), size = 22.dp)
    }
}

/** Equal-width segmented selector. */
@Composable
internal fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList(),
) {
    val colors = rememberEqColors()
    val shape = eqShape(Tokens.radiusChipLarge)
    Row(
        modifier.height(40.dp).clip(shape).background(colors.cardHigh, shape).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Row(
                Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(CircleShape)
                    .background(if (on) colors.accent else Color.Transparent, CircleShape)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            ) {
                val tint = if (on) colors.onAccent else colors.muted
                icons.getOrNull(i)?.let { AppIcon(it, null, tint = tint, size = 18.dp) }
                Text(label, style = MaterialTheme.typography.labelMedium, color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Band chip: a colored dot and the frequency. */
@Composable
internal fun BandChip(band: EqBand, selected: Boolean, onClick: () -> Unit) {
    val colors = rememberEqColors()
    val shape = eqShape(Tokens.radiusChipLarge)
    val dot = if (band.enabled) colors.bandColor(band.id) else colors.muted
    Row(
        Modifier
            .heightIn(min = 36.dp)
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.2f) else colors.cardHigh, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(10.dp).background(dot, CircleShape))
        Text(formatHzShort(band.frequencyHz), style = MaterialTheme.typography.labelMedium, color = if (selected) colors.text else colors.muted, maxLines = 1)
    }
}

/** Custom horizontal slider on 0..1. Keeps the in-flight value locally and reports every move. */
@Composable
internal fun EqSlider(
    fraction: Float,
    onFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    /** Where the fill starts: 0f for a plain slider, 0.5f for a centered (gain) slider. */
    origin: Float = 0f,
) {
    val colors = rememberEqColors()
    var local by remember { mutableStateOf<Float?>(null) }
    val onFractionRef by rememberUpdatedState(onFraction)
    val originRef by rememberUpdatedState(origin)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val pad = 14.dp.toPx()
                    fun at(x: Float): Float {
                        val f = ((x - pad) / (size.width - 2 * pad)).coerceIn(0f, 1f)
                        // Detent at the centre of a centered slider.
                        return if (originRef > 0f && kotlin.math.abs(f - originRef) < 0.012f) originRef else f
                    }
                    down.consume()
                    local = at(down.position.x)
                    onFractionRef(local!!)
                    drag(down.id) { change ->
                        change.consume()
                        local = at(change.position.x)
                        onFractionRef(local!!)
                    }
                    local = null
                }
            },
    ) {
        val shown = (local ?: fraction).coerceIn(0f, 1f)
        val pad = 14.dp.toPx()
        val trackH = 8.dp.toPx()
        val left = pad
        val width = size.width - 2 * pad
        val cy = size.height / 2f
        val radius = CornerRadius(trackH / 2f)
        drawRoundRect(colors.track, Offset(left, cy - trackH / 2f), Size(width, trackH), radius)
        val xThumb = left + shown * width
        val xOrigin = left + origin * width
        val a = minOf(xThumb, xOrigin)
        val b = maxOf(xThumb, xOrigin)
        if (b - a > 0.5f) {
            drawRoundRect(
                Brush.horizontalGradient(listOf(colors.accent, lerp(colors.accent, colors.cool, 0.6f)), startX = left, endX = left + width),
                Offset(a, cy - trackH / 2f), Size(b - a, trackH), radius,
            )
        }
        if (origin > 0f) drawLine(colors.gridStrong, Offset(xOrigin, cy - 9.dp.toPx()), Offset(xOrigin, cy + 9.dp.toPx()), 1.5.dp.toPx())
        val thumbR = 11.dp.toPx()
        drawCircle(colors.accent.copy(alpha = 0.22f), thumbR + 6.dp.toPx(), Offset(xThumb, cy))
        drawCircle(colors.accent, thumbR, Offset(xThumb, cy))
        drawCircle(colors.onAccent.copy(alpha = 0.85f), 3.dp.toPx(), Offset(xThumb, cy))
    }
}

/** Label with a typeable value on one line and a full-width custom slider below. */
@Composable
internal fun ParamRow(
    label: String,
    fraction: Float,
    text: String,
    unit: String,
    onFraction: (Float) -> Unit,
    onValue: (Float) -> Unit,
    modifier: Modifier = Modifier,
    origin: Float = 0f,
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = AppTheme.colors.muted, modifier = Modifier.weight(1f))
            NumberField(text, unit, onValue, Modifier.width(104.dp))
        }
        EqSlider(fraction, onFraction, origin = origin)
    }
}

@Composable
private fun NumberField(text: String, unit: String, onValue: (Float) -> Unit, modifier: Modifier = Modifier) {
    val colors = rememberEqColors()
    val shape = eqShape(Tokens.radiusSm)
    var value by remember(text) { mutableStateOf(text) }
    var focused by remember { mutableStateOf(false) }
    fun commit() {
        parseNumber(value)?.let(onValue)
        value = text
    }
    BasicTextField(
        value = value,
        onValueChange = { value = it },
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge.copy(color = colors.text, textAlign = TextAlign.End),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { commit() }),
        modifier = modifier.onFocusChanged {
            if (focused && !it.isFocused) commit()
            focused = it.isFocused
        },
        decorationBox = { inner ->
            Row(
                Modifier
                    .heightIn(min = 36.dp)
                    .clip(shape)
                    .background(colors.cardHigh, shape)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) { inner() }
                if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = colors.muted)
            }
        },
    )
}

/** Frequency, gain and Q sliders with numeric text, type selector, enable switch and remove button. */
@Composable
internal fun BandEditor(
    band: EqBand,
    state: EqualizerState,
    onChange: (EqBand) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberEqColors()
    val types = listOf(EqBandType.Peaking, EqBandType.LowShelf, EqBandType.HighShelf)
    Column(modifier.fillMaxWidth().eqCard(Tokens.radiusPanel).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(12.dp).background(colors.bandColor(band.id), CircleShape))
            Text(formatHz(band.frequencyHz), style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.weight(1f), maxLines = 1)
            PillSwitch(band.enabled, { onChange(band.copy(enabled = it)) })
            IconAction(AppIcons.Delete, "Remove band", onRemove)
        }
        Segmented(
            options = listOf("Peak", "Low shelf", "High shelf"),
            selected = types.indexOf(band.type).coerceAtLeast(0),
            onSelect = { onChange(band.copy(type = types[it])) },
        )
        ParamRow(
            label = "Frequency",
            fraction = logFraction(band.frequencyHz, state.minFrequencyHz, state.maxFrequencyHz),
            text = band.frequencyHz.roundToInt().toString(),
            unit = "Hz",
            onFraction = { f ->
                val hz = logValue(f, state.minFrequencyHz, state.maxFrequencyHz).roundToInt().toFloat()
                if (hz != band.frequencyHz) onChange(band.copy(frequencyHz = hz))
            },
            onValue = { onChange(band.copy(frequencyHz = it.coerceIn(state.minFrequencyHz, state.maxFrequencyHz).roundToInt().toFloat())) },
        )
        ParamRow(
            label = "Gain",
            fraction = ((band.gainDb - state.minGainDb) / (state.maxGainDb - state.minGainDb)).coerceIn(0f, 1f),
            text = formatDecimal(band.gainDb, 1),
            unit = "dB",
            origin = ((0f - state.minGainDb) / (state.maxGainDb - state.minGainDb)).coerceIn(0f, 1f),
            onFraction = { f ->
                val g = snap(state.minGainDb + f * (state.maxGainDb - state.minGainDb), 0.1f)
                if (g != band.gainDb) onChange(band.copy(gainDb = g))
            },
            onValue = { onChange(band.copy(gainDb = snap(it.coerceIn(state.minGainDb, state.maxGainDb), 0.1f))) },
        )
        if (band.type == EqBandType.Peaking) {
            ParamRow(
                label = "Q",
                fraction = logFraction(band.q, state.minQ, state.maxQ),
                text = formatDecimal(band.q, 2),
                unit = "",
                onFraction = { f ->
                    val q = snap(logValue(f, state.minQ, state.maxQ), 0.05f)
                    if (q != band.q) onChange(band.copy(q = q))
                },
                onValue = { onChange(band.copy(q = snap(it.coerceIn(state.minQ, state.maxQ), 0.05f))) },
            )
        }
    }
}

/** Row of band chips, so overlapping nodes can still be selected. */
@Composable
internal fun BandChips(bands: List<EqBand>, selectedId: Int?, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        bands.forEach { band ->
            key(band.id) { BandChip(band, band.id == selectedId) { onSelect(band.id) } }
        }
    }
}
