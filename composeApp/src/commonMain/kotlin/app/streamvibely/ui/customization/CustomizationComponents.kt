package app.streamvibely.ui.customization

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.AppIcons
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Hue 0..360, saturation 0..1, value 0..1. */
internal data class Hsv(val hue: Float, val saturation: Float, val value: Float)

internal fun Hsv.toArgb(): Int = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f)).toArgb()

internal fun argbToHsv(argb: Int): Hsv {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val mx = max(r, max(g, b))
    val mn = min(r, min(g, b))
    val d = mx - mn
    val h = when {
        d == 0f -> 0f
        mx == r -> 60f * (((g - b) / d) % 6f)
        mx == g -> 60f * ((b - r) / d + 2f)
        else -> 60f * ((r - g) / d + 4f)
    }
    return Hsv(if (h < 0f) h + 360f else h, if (mx == 0f) 0f else d / mx, mx)
}

/** "#RRGGBB" for an ARGB int (alpha ignored). */
internal fun argbToHex(argb: Int): String {
    val rgb = argb and 0xFFFFFF
    return "#" + rgb.toString(16).uppercase().padStart(6, '0')
}

private val hexPattern = Regex("^#?[0-9a-fA-F]{6}$")

/** Parses "#RRGGBB" or "RRGGBB" into an opaque ARGB int, or null when invalid. */
internal fun hexToArgb(text: String): Int? {
    val t = text.trim()
    if (!hexPattern.matches(t)) return null
    return (0xFF000000L or t.removePrefix("#").toLong(16)).toInt()
}

/** Black or white, whichever reads better on [argb]. */
internal fun readableOn(argb: Int): Color {
    val r = ((argb shr 16) and 0xFF) * 0.299f
    val g = ((argb shr 8) and 0xFF) * 0.587f
    val b = (argb and 0xFF) * 0.114f
    return if (r + g + b > 150f) Color.Black else Color.White
}

internal fun Float.roundTo(step: Float): Float = (this / step).roundToInt() * step

internal fun Int.asColor(): Color = Color(this)

@Composable
internal fun CustomizationSectionHeader(title: String, icon: ImageVector, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier.fillMaxWidth().padding(start = 4.dp, top = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, null, Modifier.padding(end = 10.dp), tint = AppTheme.colors.accent, size = 22.dp)
            Text(title, color = AppTheme.colors.onSurface, style = MaterialTheme.typography.titleLarge)
        }
        if (subtitle != null) {
            Text(subtitle, color = AppTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A rounded surface that groups related controls. */
@Composable
internal fun CustomizationPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusCard.dp))
            .background(AppTheme.colors.card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

/** Color circle; [selected] draws a ring and a check mark. */
@Composable
internal fun ColorSwatch(
    color: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    selected: Boolean = false,
    label: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val ring = if (selected) AppTheme.colors.accent else AppTheme.colors.outlineVariant
    var m = modifier.size(size).clip(CircleShape).background(color)
        .border(if (selected) 3.dp else 1.dp, ring, CircleShape)
    if (label != null) m = m.semantics { contentDescription = label }
    if (onClick != null) m = m.clickable(role = Role.Button, onClick = onClick)
    Box(m, contentAlignment = Alignment.Center) {
        if (selected) {
            AppIcon(AppIcons.Check, null, tint = readableOn(color.toArgb()), size = size * 0.5f)
        }
    }
}

data class SegmentOption<T>(val value: T, val label: String, val icon: ImageVector? = null)

/** Equal-width segmented control. The selected segment is filled with the accent container. */
@Composable
internal fun <T> SegmentedControl(
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(Tokens.radiusPill.dp))
            .background(AppTheme.colors.card)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option.value == selected
            val content = if (isSelected) AppTheme.colors.onAccentContainer else AppTheme.colors.muted
            Row(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(Tokens.radiusPill.dp))
                    .background(if (isSelected) AppTheme.colors.accentContainer else Color.Transparent)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option.value) })
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (option.icon != null) {
                    AppIcon(option.icon, null, Modifier.padding(end = 6.dp), tint = content, size = 18.dp)
                }
                Text(option.label, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Selectable text chip. */
@Composable
internal fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(Tokens.radiusChip.dp))
            .background(if (selected) AppTheme.colors.accentContainer else AppTheme.colors.cardHigh)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) AppTheme.colors.onAccentContainer else AppTheme.colors.onCard,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/**
 * Labelled slider with a live value. Drag state is local, so only this row recomposes while
 * dragging; [onCommit] runs only when the value, rounded to [step], actually changes.
 */
@Composable
internal fun SliderRow(
    icon: ImageVector,
    title: String,
    value: Float,
    onCommit: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier,
    step: Float = 0.01f,
) {
    var local by remember(value) { mutableFloatStateOf(value) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(icon, null, Modifier.padding(end = 10.dp), tint = AppTheme.colors.muted, size = 20.dp)
            Text(title, Modifier.weight(1f), color = AppTheme.colors.onCard, style = MaterialTheme.typography.titleSmall)
            Text(valueLabel(local), color = AppTheme.colors.accent, style = MaterialTheme.typography.labelLarge)
        }
        Slider(
            value = local,
            onValueChange = { raw ->
                val snapped = raw.roundTo(step).coerceIn(range.start, range.endInclusive)
                val changed = abs(snapped - value) >= step / 2f
                local = raw
                if (changed) onCommit(snapped)
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AppTheme.colors.accent,
                activeTrackColor = AppTheme.colors.accent,
                inactiveTrackColor = AppTheme.colors.sliderTrack,
            ),
        )
    }
}
