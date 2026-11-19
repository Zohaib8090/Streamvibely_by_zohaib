package app.streamvibely.audio.dsp

import app.streamvibely.domain.SPECTRUM_BINS
import kotlin.concurrent.Volatile
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

private const val FFT_SIZE = 2048
private const val RING_SIZE = 8192
private const val MIN_DB = -80.0
private const val LOW_HZ = 20.0
private const val HIGH_HZ = 20_000.0
private const val ATTACK = 0.8f
private const val RELEASE = 0.12f

/**
 * Live spectrum. [feed] is called from the audio thread (one producer, no locks, no allocation);
 * [computeBins] from one other thread. A full-scale sine reads 0 dB (value 1.0), -80 dB or less
 * reads 0.0.
 */
class SpectrumAnalyzer {
    private val ring = FloatArray(RING_SIZE)
    @Volatile private var written = 0

    private val fft = Fft(FFT_SIZE)
    private val window = DoubleArray(FFT_SIZE) { 0.5 - 0.5 * cos(2.0 * PI * it / FFT_SIZE) }
    private val re = DoubleArray(FFT_SIZE)
    private val im = DoubleArray(FFT_SIZE)
    private val magnitude = DoubleArray(FFT_SIZE / 2 + 1)
    private val smoothed = FloatArray(SPECTRUM_BINS)

    // Display bin -> FFT bin mapping, rebuilt when the sample rate changes.
    @Volatile private var sampleRate = 44_100
    private var mappedRate = 0
    private val loIndex = IntArray(SPECTRUM_BINS)
    private val hiIndex = IntArray(SPECTRUM_BINS)
    private val centerPos = DoubleArray(SPECTRUM_BINS)

    /** Number of samples fed so far (wraps). A changing value means audio is flowing. */
    val writeCount: Int get() = written

    fun configure(sampleRate: Int) {
        this.sampleRate = sampleRate
    }

    /** Audio thread: mixes [frames] interleaved frames down to mono into the ring buffer. */
    fun feed(buffer: FloatArray, frames: Int, channels: Int) {
        var w = written
        var i = 0
        val mask = RING_SIZE - 1
        if (channels == 1) {
            for (f in 0 until frames) {
                ring[w and mask] = buffer[f]
                w++
            }
        } else {
            val inv = 1f / channels
            for (f in 0 until frames) {
                var sum = 0f
                for (c in 0 until channels) sum += buffer[i + c]
                i += channels
                ring[w and mask] = sum * inv
                w++
            }
        }
        written = w
    }

    /** Zeroes the smoothing memory. */
    fun reset() {
        smoothed.fill(0f)
    }

    /** Fills [out] (length [SPECTRUM_BINS]) with 0..1 magnitudes of the most recent audio. */
    fun computeBins(out: FloatArray) {
        val rate = sampleRate
        if (rate != mappedRate) rebuildMapping(rate)

        val end = written
        val mask = RING_SIZE - 1
        for (k in 0 until FFT_SIZE) {
            re[k] = ring[(end - FFT_SIZE + k) and mask] * window[k]
            im[k] = 0.0
        }
        fft.transform(re, im)
        // A full-scale sine through a Hann window peaks at FFT_SIZE / 4.
        val norm = FFT_SIZE / 4.0
        for (k in 0..FFT_SIZE / 2) magnitude[k] = sqrt(re[k] * re[k] + im[k] * im[k]) / norm

        val n = minOf(out.size, SPECTRUM_BINS)
        for (b in 0 until n) {
            val lo = loIndex[b]
            val hi = hiIndex[b]
            val mag = if (hi >= lo) {
                var m = 0.0
                for (k in lo..hi) m = max(m, magnitude[k])
                m
            } else {
                val p = centerPos[b]
                val k = floor(p).toInt().coerceIn(0, FFT_SIZE / 2 - 1)
                val t = p - k
                magnitude[k] * (1 - t) + magnitude[k + 1] * t
            }
            val db = if (mag <= 1e-9) MIN_DB else (20.0 * log10(mag)).coerceIn(MIN_DB, 0.0)
            val target = ((db - MIN_DB) / -MIN_DB).toFloat()
            val prev = smoothed[b]
            val next = prev + (target - prev) * (if (target > prev) ATTACK else RELEASE)
            smoothed[b] = next
            out[b] = next
        }
    }

    private fun rebuildMapping(rate: Int) {
        val binHz = rate.toDouble() / FFT_SIZE
        val ratio = (HIGH_HZ / LOW_HZ).pow(1.0 / SPECTRUM_BINS)
        for (b in 0 until SPECTRUM_BINS) {
            val low = LOW_HZ * ratio.pow(b)
            val high = low * ratio
            loIndex[b] = max(1, ceil(low / binHz).toInt())
            hiIndex[b] = minOf(FFT_SIZE / 2, floor(high / binHz).toInt())
            centerPos[b] = sqrt(low * high) / binHz
        }
        mappedRate = rate
    }

    companion object {
        /** Index of the display bin that contains [frequencyHz], or -1 outside 20 Hz..20 kHz. */
        fun binForFrequency(frequencyHz: Double): Int {
            if (frequencyHz < LOW_HZ || frequencyHz >= HIGH_HZ) return -1
            val pos = SPECTRUM_BINS * log10(frequencyHz / LOW_HZ) / log10(HIGH_HZ / LOW_HZ)
            return pos.toInt().coerceIn(0, SPECTRUM_BINS - 1)
        }
    }
}
