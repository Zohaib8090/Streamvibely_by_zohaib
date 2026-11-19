package app.streamvibely.audio.dsp

import app.streamvibely.domain.EqBandType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Highest usable frequency as a fraction of the sample rate; anything above is clamped. */
const val MAX_FREQUENCY_FRACTION = 0.45

/** Gains whose magnitude is below this (in dB) are treated as 0 dB and the band is bypassed. */
const val BYPASS_GAIN_DB = 0.005f

/** Normalised biquad coefficients (a0 == 1): y = b0 x + b1 x1 + b2 x2 - a1 y1 - a2 y2. */
class BiquadCoefficients(
    val b0: Double,
    val b1: Double,
    val b2: Double,
    val a1: Double,
    val a2: Double,
) {
    /** Magnitude of the transfer function at [frequencyHz] in dB. */
    fun magnitudeDb(frequencyHz: Double, sampleRate: Int): Double {
        val w = 2.0 * PI * frequencyHz / sampleRate
        val c1 = cos(w)
        val s1 = sin(w)
        val c2 = cos(2 * w)
        val s2 = sin(2 * w)
        val numRe = b0 + b1 * c1 + b2 * c2
        val numIm = -(b1 * s1 + b2 * s2)
        val denRe = 1.0 + a1 * c1 + a2 * c2
        val denIm = -(a1 * s1 + a2 * s2)
        val num = numRe * numRe + numIm * numIm
        val den = denRe * denRe + denIm * denIm
        return 10.0 * log10(max(num, 1e-300) / max(den, 1e-300))
    }

    companion object {
        val Identity = BiquadCoefficients(1.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/** True when a band with this gain does nothing audible and can be skipped. */
fun isBypassGain(gainDb: Float): Boolean = abs(gainDb) < BYPASS_GAIN_DB

/**
 * RBJ audio EQ cookbook coefficients. Shelves use shelf slope S = 1 and ignore [q].
 * The frequency is clamped to 10 Hz .. 0.45 x [sampleRate]; q to 0.05 .. 50.
 */
fun biquadCoefficients(
    sampleRate: Int,
    frequencyHz: Float,
    gainDb: Float,
    q: Float,
    type: EqBandType,
): BiquadCoefficients {
    if (isBypassGain(gainDb)) return BiquadCoefficients.Identity
    val maxFreq = sampleRate * MAX_FREQUENCY_FRACTION
    val f = min(max(frequencyHz.toDouble(), 10.0), maxFreq)
    val qq = min(max(q.toDouble(), 0.05), 50.0)
    val a = 10.0.pow(gainDb.toDouble() / 40.0)
    val w0 = 2.0 * PI * f / sampleRate
    val cosW = cos(w0)
    val sinW = sin(w0)

    val b0: Double
    val b1: Double
    val b2: Double
    val a0: Double
    val a1: Double
    val a2: Double
    when (type) {
        EqBandType.Peaking -> {
            val alpha = sinW / (2.0 * qq)
            b0 = 1.0 + alpha * a
            b1 = -2.0 * cosW
            b2 = 1.0 - alpha * a
            a0 = 1.0 + alpha / a
            a1 = -2.0 * cosW
            a2 = 1.0 - alpha / a
        }
        EqBandType.LowShelf -> {
            val t = 2.0 * sqrt(a) * (sinW / 2.0 * sqrt(2.0))
            b0 = a * ((a + 1) - (a - 1) * cosW + t)
            b1 = 2.0 * a * ((a - 1) - (a + 1) * cosW)
            b2 = a * ((a + 1) - (a - 1) * cosW - t)
            a0 = (a + 1) + (a - 1) * cosW + t
            a1 = -2.0 * ((a - 1) + (a + 1) * cosW)
            a2 = (a + 1) + (a - 1) * cosW - t
        }
        EqBandType.HighShelf -> {
            val t = 2.0 * sqrt(a) * (sinW / 2.0 * sqrt(2.0))
            b0 = a * ((a + 1) + (a - 1) * cosW + t)
            b1 = -2.0 * a * ((a - 1) + (a + 1) * cosW)
            b2 = a * ((a + 1) + (a - 1) * cosW - t)
            a0 = (a + 1) - (a - 1) * cosW + t
            a1 = 2.0 * ((a - 1) - (a + 1) * cosW)
            a2 = (a + 1) - (a - 1) * cosW - t
        }
    }
    return BiquadCoefficients(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
}

/** Delay line of one filter on one channel (Direct Form II Transposed, Double state). */
class BiquadState {
    var z1 = 0.0
    var z2 = 0.0

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }

    fun process(c: BiquadCoefficients, x: Float): Float {
        val y = c.b0 * x + z1
        z1 = c.b1 * x - c.a1 * y + z2
        z2 = c.b2 * x - c.a2 * y
        return y.toFloat()
    }

    /**
     * Filters [count] interleaved samples of one channel in place: indices [start], [start] + [stride], ...
     * below [end]. Does not allocate.
     */
    fun processInterleaved(c: BiquadCoefficients, buffer: FloatArray, start: Int, stride: Int, end: Int) {
        val b0 = c.b0
        val b1 = c.b1
        val b2 = c.b2
        val a1 = c.a1
        val a2 = c.a2
        var s1 = z1
        var s2 = z2
        var i = start
        while (i < end) {
            val x = buffer[i]
            val y = b0 * x + s1
            s1 = b1 * x - a1 * y + s2
            s2 = b2 * x - a2 * y
            buffer[i] = y.toFloat()
            i += stride
        }
        // Flush tiny values so the state never becomes denormal during silence.
        z1 = if (abs(s1) < 1e-30) 0.0 else s1
        z2 = if (abs(s2) < 1e-30) 0.0 else s2
    }
}
