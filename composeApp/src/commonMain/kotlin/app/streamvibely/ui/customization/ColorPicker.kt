package app.streamvibely.ui.customization

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.Tokens
import kotlin.math.roundToInt

private val hueStops = List(7) { Color.hsv(it * 60f, 1f, 1f) }

/**
 * Hue + shade pad + hex field for one color. Keeps the full HSV locally so hue and saturation
 * survive passing through gray; [onColorChange] gets an opaque ARGB int.
 */
@Composable
internal fun ColorPicker(color: Int, onColorChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var hsv by remember { mutableStateOf(argbToHsv(color)) }
    LaunchedEffect(color) { if (hsv.toArgb() != color) hsv = argbToHsv(color) }
    val emit = rememberUpdatedState(onColorChange)
    val update = { next: Hsv ->
        hsv = next
        val argb = next.toArgb()
        if (argb != color) emit.value(argb)
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ShadePad(hsv, onChange = update)
        HueSlider(hsv.hue, onHueChange = { update(hsv.copy(hue = it)) })
        HexField(color = hsv.toArgb(), onColorChange = { update(argbToHsv(it).let { n -> if (n.saturation == 0f || n.value == 0f) n.copy(hue = hsv.hue) else n }) })
    }
}

/** Hue slider 0..360 with a rainbow track. Reports whole degrees only. */
@Composable
internal fun HueSlider(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val current = rememberUpdatedState(hue)
    val emit = rememberUpdatedState(onHueChange)
    val thumb = AppTheme.colors.onSurface
    Canvas(
        modifier.fillMaxWidth().height(32.dp).pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                fun apply(x: Float) {
                    val deg = (x / size.width * 360f).coerceIn(0f, 360f).roundToInt().toFloat()
                    if (deg != current.value) emit.value(deg)
                }
                apply(down.position.x)
                drag(down.id) { change ->
                    change.consume()
                    apply(change.position.x)
                }
            }
        },
    ) {
        val trackH = 14.dp.toPx()
        val top = (size.height - trackH) / 2f
        drawRoundRect(
            Brush.horizontalGradient(hueStops),
            topLeft = Offset(0f, top),
            size = androidx.compose.ui.geometry.Size(size.width, trackH),
            cornerRadius = CornerRadius(trackH / 2f),
        )
        val cx = (hue.coerceIn(0f, 360f) / 360f * size.width)
        drawCircle(Color.hsv(hue.coerceIn(0f, 360f), 1f, 1f), radius = 12.dp.toPx(), center = Offset(cx, size.height / 2f))
        drawCircle(thumb, radius = 12.dp.toPx(), center = Offset(cx, size.height / 2f), style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
    }
}

/** Two-dimensional pad: saturation left to right, brightness bottom to top, at the current hue. */
@Composable
internal fun ShadePad(hsv: Hsv, onChange: (Hsv) -> Unit, modifier: Modifier = Modifier) {
    val current = rememberUpdatedState(hsv)
    val emit = rememberUpdatedState(onChange)
    Canvas(
        modifier.fillMaxWidth().height(160.dp)
            .clip(RoundedCornerShape(Tokens.radiusSm.dp))
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    fun apply(p: Offset) {
                        val s = (p.x / size.width).coerceIn(0f, 1f).roundTo(0.01f)
                        val v = (1f - p.y / size.height).coerceIn(0f, 1f).roundTo(0.01f)
                        val c = current.value
                        if (s != c.saturation || v != c.value) emit.value(c.copy(saturation = s, value = v))
                    }
                    apply(down.position)
                    drag(down.id) { change ->
                        change.consume()
                        apply(change.position)
                    }
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(hsv.hue.coerceIn(0f, 360f), 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val c = Offset(hsv.saturation * size.width, (1f - hsv.value) * size.height)
        drawCircle(hsv.toArgb().asColor(), radius = 11.dp.toPx(), center = c)
        drawCircle(Color.White, radius = 11.dp.toPx(), center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx()))
    }
}

/** "#RRGGBB" text field. Invalid text shows an error border and keeps the old color. */
@Composable
internal fun HexField(color: Int, onColorChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(argbToHex(color)) }
    LaunchedEffect(color) { if (hexToArgb(text) != (color or 0xFF000000.toInt())) text = argbToHex(color) }
    val valid = hexToArgb(text) != null
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color.asColor()).border(1.dp, AppTheme.colors.outlineVariant, CircleShape))
        Box(
            Modifier.weight(1f)
                .clip(RoundedCornerShape(Tokens.radiusSm.dp))
                .background(AppTheme.colors.cardHigh)
                .border(1.dp, if (valid) AppTheme.colors.outlineVariant else AppTheme.colors.error, RoundedCornerShape(Tokens.radiusSm.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = {
                    val clean = it.take(7)
                    text = clean
                    hexToArgb(clean)?.let(onColorChange)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppTheme.colors.onCard),
                cursorBrush = SolidColor(AppTheme.colors.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (!valid) {
        Text("Use six hex digits, like #8AA4FF", Modifier.padding(start = 4.dp), color = AppTheme.colors.error, style = MaterialTheme.typography.bodySmall)
    }
}
