package app.streamvibely.equalizer

import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import kotlin.math.max
import kotlin.math.roundToInt

/** The default 10-band layout and the built-in presets applied on top of it. */
internal object EqPresets {
    val frequencies = floatArrayOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    const val DEFAULT_Q = 1.41f
    const val FLAT = "Flat"

    private val gains: List<Pair<String, FloatArray>> = listOf(
        FLAT to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Bass boost" to floatArrayOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
        "Treble boost" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f),
        "Vocal" to floatArrayOf(-2f, -2f, -1f, 1f, 3f, 4f, 3f, 2f, 0f, -1f),
        "Rock" to floatArrayOf(4f, 3f, 2f, 1f, -1f, -1f, 0f, 2f, 3f, 4f),
        "Pop" to floatArrayOf(-1f, 2f, 4f, 5f, 3f, 0f, -1f, -1f, -1f, -1f),
        "Jazz" to floatArrayOf(3f, 2f, 1f, 2f, -2f, -2f, 0f, 1f, 2f, 3f),
        "Classical" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, -2f, -2f, -2f, -3f),
        "Electronic" to floatArrayOf(5f, 4f, 1f, 0f, -2f, 2f, 1f, 1f, 4f, 5f),
        "Loudness" to floatArrayOf(6f, 4f, 0f, -1f, -1f, 0f, 0f, 2f, 4f, 5f),
    )

    val names: List<String> = gains.map { it.first }

    fun isBuiltIn(name: String): Boolean = names.any { it.equals(name, ignoreCase = true) }

    fun canonicalName(name: String): String? = names.firstOrNull { it.equals(name, ignoreCase = true) }

    /** Preamp that leaves headroom for the largest boost of a preset (half of it, in 0.5 dB steps). */
    fun preampFor(name: String): Float {
        val g = gains.firstOrNull { it.first == name }?.second ?: return 0f
        val peak = max(0f, g.max())
        return -((peak * 0.5f * 2f).roundToInt() / 2f)
    }

    /** The preset's bands; ids come from [nextId]. */
    fun bands(name: String, nextId: () -> Int): List<EqBand> {
        val g = gains.firstOrNull { it.first == name }?.second ?: gains[0].second
        return List(frequencies.size) { i ->
            EqBand(nextId(), frequencies[i], g[i], DEFAULT_Q, EqBandType.Peaking, true)
        }
    }
}
