package app.streamvibely.audio.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** In-place iterative radix-2 complex FFT of a fixed power-of-two [size]. */
class Fft(val size: Int) {
    private val cosTable = DoubleArray(size / 2)
    private val sinTable = DoubleArray(size / 2)
    private val reversed = IntArray(size)

    init {
        require(size >= 2 && (size and (size - 1)) == 0) { "size must be a power of two" }
        for (i in 0 until size / 2) {
            val a = -2.0 * PI * i / size
            cosTable[i] = cos(a)
            sinTable[i] = sin(a)
        }
        var bits = 0
        while ((1 shl bits) < size) bits++
        for (i in 0 until size) {
            var r = 0
            var v = i
            for (b in 0 until bits) {
                r = (r shl 1) or (v and 1)
                v = v shr 1
            }
            reversed[i] = r
        }
    }

    /** Forward transform of ([re], [im]), both of length [size]. */
    fun transform(re: DoubleArray, im: DoubleArray) {
        for (i in 0 until size) {
            val j = reversed[i]
            if (j > i) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }
        var half = 1
        while (half < size) {
            val step = size / (half * 2)
            var start = 0
            while (start < size) {
                var k = 0
                for (j in start until start + half) {
                    val wr = cosTable[k]
                    val wi = sinTable[k]
                    val l = j + half
                    val xr = re[l] * wr - im[l] * wi
                    val xi = re[l] * wi + im[l] * wr
                    re[l] = re[j] - xr
                    im[l] = im[j] - xi
                    re[j] += xr
                    im[j] += xi
                    k += step
                }
                start += half * 2
            }
            half *= 2
        }
    }
}
