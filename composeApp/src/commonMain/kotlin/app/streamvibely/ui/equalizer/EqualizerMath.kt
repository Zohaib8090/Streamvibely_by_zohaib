package app.streamvibely.ui.equalizer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

internal const val AxisMinHz = 20f
internal const val AxisMaxHz = 20_000f
internal val AxisLabelsHz = floatArrayOf(50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)

internal fun frequencyToFraction(hz: Float): Float =
    (ln(hz.coerceAtLeast(AxisMinHz) / AxisMinHz) / ln(AxisMaxHz / AxisMinHz)).coerceIn(0f, 1f)

internal fun fractionToFrequency(fraction: Float): Float =
    AxisMinHz * (AxisMaxHz / AxisMinHz).pow(fraction.coerceIn(0f, 1f))

/** Position of [value] inside [min]..[max] on a logarithmic scale, 0..1. */
internal fun logFraction(value: Float, min: Float, max: Float): Float =
    (ln(value.coerceIn(min, max) / min) / ln(max / min)).coerceIn(0f, 1f)

internal fun logValue(fraction: Float, min: Float, max: Float): Float =
    min * (max / min).pow(fraction.coerceIn(0f, 1f))

internal fun snap(value: Float, step: Float): Float = (value / step).roundToInt() * step

/**
 * Magnitude in dB of the whole band list at [frequencyHz], using the RBJ audio EQ cookbook biquads
 * (peaking, low shelf, high shelf) at [sampleRate]. Disabled bands are skipped.
 */
internal fun responseDb(bands: List<EqBand>, frequencyHz: Float, sampleRate: Int = 48000): Float {
    val w = 2.0 * PI * min(frequencyHz.toDouble(), sampleRate * 0.499) / sampleRate
    var total = 0.0
    for (band in bands) {
        if (band.enabled && band.gainDb != 0f) total += bandResponseDb(band, w, sampleRate)
    }
    return total.toFloat()
}

private fun bandResponseDb(band: EqBand, w: Double, sampleRate: Int): Double {
    val w0 = 2.0 * PI * min(band.frequencyHz.toDouble(), sampleRate * 0.499) / sampleRate
    val a = 10.0.pow(band.gainDb / 40.0)
    val cosW0 = cos(w0)
    val sinW0 = sin(w0)
    val sqrtA = sqrt(a)
    // c = b0, b1, b2, a0, a1, a2
    val c: DoubleArray = when (band.type) {
        EqBandType.Peaking -> {
            val alpha = sinW0 / (2.0 * band.q.coerceAtLeast(0.05f))
            doubleArrayOf(
                1 + alpha * a, -2 * cosW0, 1 - alpha * a,
                1 + alpha / a, -2 * cosW0, 1 - alpha / a,
            )
        }
        EqBandType.LowShelf -> {
            val alpha = sinW0 / 2.0 * sqrt(2.0)
            doubleArrayOf(
                a * ((a + 1) - (a - 1) * cosW0 + 2 * sqrtA * alpha),
                2 * a * ((a - 1) - (a + 1) * cosW0),
                a * ((a + 1) - (a - 1) * cosW0 - 2 * sqrtA * alpha),
                (a + 1) + (a - 1) * cosW0 + 2 * sqrtA * alpha,
                -2 * ((a - 1) + (a + 1) * cosW0),
                (a + 1) + (a - 1) * cosW0 - 2 * sqrtA * alpha,
            )
        }
        EqBandType.HighShelf -> {
            val alpha = sinW0 / 2.0 * sqrt(2.0)
            doubleArrayOf(
                a * ((a + 1) + (a - 1) * cosW0 + 2 * sqrtA * alpha),
                -2 * a * ((a - 1) + (a + 1) * cosW0),
                a * ((a + 1) + (a - 1) * cosW0 - 2 * sqrtA * alpha),
                (a + 1) - (a - 1) * cosW0 + 2 * sqrtA * alpha,
                2 * ((a - 1) - (a + 1) * cosW0),
                (a + 1) - (a - 1) * cosW0 - 2 * sqrtA * alpha,
            )
        }
    }
    val cw = cos(w)
    val sw = sin(w)
    val c2 = cos(2 * w)
    val s2 = sin(2 * w)
    val nr = c[0] + c[1] * cw + c[2] * c2
    val ni = -(c[1] * sw + c[2] * s2)
    val dr = c[3] + c[4] * cw + c[5] * c2
    val di = -(c[4] * sw + c[5] * s2)
    return 10.0 * log10((nr * nr + ni * ni + 1e-12) / (dr * dr + di * di + 1e-12))
}

/** Geometric middle of the widest empty stretch of the spectrum, for a newly added band. */
internal fun suggestFrequency(bands: List<EqBand>): Float {
    val points = (bands.map { it.frequencyHz } + AxisMinHz + AxisMaxHz).sorted()
    var best = 1000f
    var widest = -1f
    for (i in 0 until points.size - 1) {
        val gap = ln(points[i + 1] / points[i])
        if (gap > widest) {
            widest = gap
            best = sqrt(points[i] * points[i + 1])
        }
    }
    return best.roundToInt().toFloat()
}

internal fun formatDecimal(value: Float, decimals: Int): String {
    var p = 1
    repeat(decimals) { p *= 10 }
    val scaled = (abs(value) * p).roundToInt()
    val sign = if (value < 0f && scaled != 0) "-" else ""
    if (decimals == 0) return sign + scaled
    return sign + (scaled / p) + "." + (scaled % p).toString().padStart(decimals, '0')
}

internal fun formatSigned(db: Float): String = (if (db >= 0.05f) "+" else "") + formatDecimal(db, 1)

internal fun formatHzShort(hz: Float): String =
    if (hz >= 1000f) formatDecimal(hz / 1000f, 1).removeSuffix(".0") + "k" else hz.roundToInt().toString()

internal fun formatHz(hz: Float): String =
    if (hz >= 1000f) formatDecimal(hz / 1000f, 2).removeSuffix("0").removeSuffix(".0") + " kHz"
    else hz.roundToInt().toString() + " Hz"

/** Accepts "440", "1.2k", "1,5". Returns null when the text is not a number. */
internal fun parseNumber(text: String): Float? {
    val t = text.trim().replace(',', '.')
    if (t.isEmpty()) return null
    val kilo = t.endsWith("k") || t.endsWith("K")
    val n = (if (kilo) t.dropLast(1) else t).toFloatOrNull() ?: return null
    return if (kilo) n * 1000f else n
}

/** Which band is selected, and the hook that selects a band the lead has just created. */
@Stable
internal class BandSelection {
    var selectedId by mutableStateOf<Int?>(null)
    var knownIds: Set<Int>? = null

    fun expectNew(bands: List<EqBand>) {
        knownIds = bands.mapTo(HashSet()) { it.id }
    }

    fun resolve(bands: List<EqBand>): EqBand? = bands.firstOrNull { it.id == selectedId } ?: bands.firstOrNull()
}

@Composable
internal fun rememberBandSelection(bands: List<EqBand>): BandSelection {
    val selection = remember { BandSelection() }
    LaunchedEffect(bands) {
        val known = selection.knownIds
        if (known != null) {
            val fresh = bands.firstOrNull { it.id !in known }
            if (fresh != null) {
                selection.selectedId = fresh.id
                selection.knownIds = null
            }
        }
    }
    return selection
}

/** Width in octaves of a peaking filter with quality factor [q]. */
internal fun bandwidthOctaves(q: Float): Float {
    val x = 1.0 / (2.0 * q.coerceAtLeast(0.05f))
    return (2.0 * ln(x + sqrt(x * x + 1.0)) / ln(2.0)).toFloat()
}
