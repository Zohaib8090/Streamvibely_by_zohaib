package app.streamvibely.audio.dsp

import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import app.streamvibely.domain.SPECTRUM_BINS
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DspTest {
    private val rate = 48_000

    private fun sine(freq: Double, frames: Int, amp: Double = 0.5): FloatArray =
        FloatArray(frames) { (amp * sin(2.0 * PI * freq * it / rate)).toFloat() }

    private fun rms(x: FloatArray, from: Int): Double {
        var s = 0.0
        for (i in from until x.size) s += x[i] * x[i]
        return sqrt(s / (x.size - from))
    }

    private fun measuredGainDb(chain: EqChain, freq: Double): Double {
        val input = sine(freq, rate)
        val buf = input.copyOf()
        chain.process(buf, buf.size)
        return 20.0 * log10(rms(buf, rate / 2) / rms(input, rate / 2))
    }

    private fun mono(chain: EqChain, bands: List<EqBand>, preamp: Float = 0f): EqChain {
        chain.configure(rate, 1)
        chain.setParams(preamp, bands)
        return chain
    }

    @Test
    fun peakingFilterHasRequestedGainAtCentre() {
        for ((f, g, q) in listOf(Triple(1000f, 9f, 1.41f), Triple(250f, -12f, 4f), Triple(8000f, 6f, 0.5f))) {
            val c = biquadCoefficients(rate, f, g, q, EqBandType.Peaking)
            assertEquals(g.toDouble(), c.magnitudeDb(f.toDouble(), rate), 0.05)
            val chain = mono(EqChain(), listOf(EqBand(1, f, g, q)))
            assertEquals(g.toDouble(), measuredGainDb(chain, f.toDouble()), 0.2)
        }
    }

    @Test
    fun zeroGainAndDisabledLeaveSignalUnchanged() {
        val input = sine(1000.0, 4800)
        val chain = mono(EqChain(), listOf(EqBand(1, 1000f, 0f), EqBand(2, 500f, 10f, enabled = false)))
        val buf = input.copyOf()
        chain.process(buf, buf.size)
        for (i in input.indices) assertEquals(input[i], buf[i])
        assertTrue(chain.isIdentity)
    }

    @Test
    fun shelvesBoostTheRightSide() {
        val low = biquadCoefficients(rate, 200f, 10f, 1f, EqBandType.LowShelf)
        assertTrue(low.magnitudeDb(30.0, rate) > 9.0)
        assertTrue(abs(low.magnitudeDb(10_000.0, rate)) < 0.5)
        val high = biquadCoefficients(rate, 4000f, 10f, 1f, EqBandType.HighShelf)
        assertTrue(high.magnitudeDb(18_000.0, rate) > 9.0)
        assertTrue(abs(high.magnitudeDb(100.0, rate)) < 0.5)
    }

    @Test
    fun preampScalesSignal() {
        val chain = mono(EqChain(), emptyList(), preamp = -6f)
        val first = FloatArray(rate) { 0.5f }
        chain.process(first, first.size) // ramps to the target
        val second = FloatArray(1000) { 0.5f }
        chain.process(second, second.size)
        assertEquals(0.5 * 0.5012, second[500].toDouble(), 0.001)
    }

    @Test
    fun stereoChannelsAreFilteredIndependently() {
        val chain = EqChain()
        chain.configure(rate, 2)
        chain.setParams(0f, listOf(EqBand(1, 1000f, 12f, 1f)))
        val left = sine(1000.0, rate)
        val buf = FloatArray(rate * 2)
        for (i in 0 until rate) buf[2 * i] = left[i]
        chain.process(buf, rate)
        var rightPeak = 0f
        for (i in 0 until rate) rightPeak = maxOf(rightPeak, abs(buf[2 * i + 1]))
        assertEquals(0f, rightPeak)
        assertTrue(abs(buf[2 * (rate - 100)]) > 0f)
    }

    @Test
    fun stateIsKeptWhenOnlyGainChanges() {
        val chain = mono(EqChain(), listOf(EqBand(1, 1000f, 6f, 1f)))
        val a = sine(1000.0, 2400)
        chain.process(a.copyOf(), a.size)
        chain.setParams(0f, listOf(EqBand(1, 1000f, 7f, 1f)))
        val b = sine(1000.0, 2400)
        val out = b.copyOf()
        chain.process(out, out.size)
        // No click: the first output sample stays within the input amplitude plus boost.
        assertTrue(abs(out[0]) < 1f)
        for (v in out) assertTrue(abs(v) < 2f)
    }

    @Test
    fun stableForExtremeSettings() {
        val bands = ArrayList<EqBand>()
        var id = 0
        for (f in listOf(20f, 20_000f)) for (q in listOf(0.2f, 12f)) for (g in listOf(-15f, 15f)) {
            for (t in EqBandType.entries) bands.add(EqBand(id++, f, g, q, t))
        }
        val chain = EqChain()
        chain.configure(44_100, 2)
        chain.setParams(-6f, bands)
        val buf = FloatArray(44_100 * 2)
        for (round in 0 until 5) {
            for (i in 0 until 44_100) {
                val v = (0.3 * sin(2.0 * PI * 997.0 * i / 44_100)).toFloat()
                buf[2 * i] = v
                buf[2 * i + 1] = -v
            }
            chain.process(buf, 44_100)
            for (v in buf) assertTrue(!v.isNaN() && !v.isInfinite(), "non-finite sample")
        }
    }

    @Test
    fun fftOfSinePeaksAtItsBin() {
        val n = 2048
        val fft = Fft(n)
        val bin = 64
        val re = DoubleArray(n) { sin(2.0 * PI * bin * it / n) }
        val im = DoubleArray(n)
        fft.transform(re, im)
        var best = 0
        var bestMag = 0.0
        for (k in 0 until n / 2) {
            val m = re[k] * re[k] + im[k] * im[k]
            if (m > bestMag) { bestMag = m; best = k }
        }
        assertEquals(bin, best)
        assertEquals(n / 2.0, sqrt(bestMag), 1e-6)
    }

    @Test
    fun analyzerPeaksInBinOfOneKilohertz() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(44_100)
        val input = FloatArray(4096) { (0.5 * sin(2.0 * PI * 1000.0 * it / 44_100)).toFloat() }
        analyzer.feed(input, input.size, 1)
        val out = FloatArray(SPECTRUM_BINS)
        repeat(10) { analyzer.computeBins(out) }
        var best = 0
        for (i in out.indices) if (out[i] > out[best]) best = i
        assertEquals(SpectrumAnalyzer.binForFrequency(1000.0), best)
        // 0.5 amplitude is -6 dBFS, about 0.925 on the 0..1 scale.
        assertEquals(0.925f, out[best], 0.06f)
    }

    @Test
    fun analyzerIsSilentWithoutAudioAndReleasesSlowly() {
        val analyzer = SpectrumAnalyzer()
        analyzer.configure(48_000)
        val out = FloatArray(SPECTRUM_BINS)
        analyzer.computeBins(out)
        assertTrue(out.all { it == 0f })
        val loud = FloatArray(4096) { (0.8 * sin(2.0 * PI * 440.0 * it / 48_000)).toFloat() }
        analyzer.feed(loud, loud.size, 1)
        analyzer.computeBins(out)
        val attacked = out.max()
        analyzer.feed(FloatArray(4096), 4096, 1)
        analyzer.computeBins(out)
        assertTrue(out.max() > 0f && out.max() < attacked, "release should decay, not drop to zero at once")
    }
}
