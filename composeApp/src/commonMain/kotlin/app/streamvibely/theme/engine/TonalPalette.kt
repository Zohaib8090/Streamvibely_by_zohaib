package app.streamvibely.theme.engine

import androidx.compose.ui.graphics.Color

/**
 * Colors of one hue and chroma at every tone 0..100 (CIE L*). Results are cached; not thread safe,
 * so create one per generation.
 */
class TonalPalette(hueDeg: Double, chroma: Double) {
    val hue: Double = Oklab.normalizeHue(hueDeg)
    val chroma: Double = if (chroma.isFinite()) chroma.coerceAtLeast(0.0) else 0.0
    private val cache = HashMap<Int, Color>()

    /** The color at [t] (clamped to 0..100). */
    fun tone(t: Int): Color {
        val key = t.coerceIn(0, 100)
        return cache.getOrPut(key) { colorForTone(hue, chroma, key.toDouble()) }
    }
}
