package app.streamvibely.audio.dsp

import app.streamvibely.domain.EqBand
import kotlin.concurrent.Volatile
import kotlin.math.pow

private const val MAX_STAGES = 32

/**
 * Parametric equalizer: a preamp followed by up to [MAX_STAGES] biquad filters per channel.
 *
 * Threading: [setParams] may be called from any thread. It builds an immutable [Snapshot] and
 * publishes it through a volatile reference; [process] (audio thread) picks it up at the start of
 * the next buffer. Filter state is keyed by band id and carried over, so changing a gain or
 * frequency does not click. [configure] and [process] must be called from the same thread.
 */
class EqChain {
    private class Params(val preampDb: Float, val bands: List<EqBand>)

    private class Snapshot(
        val sampleRate: Int,
        val preampGain: Float,
        val count: Int,
        val ids: IntArray,
        val coefficients: Array<BiquadCoefficients>,
    )

    @Volatile private var sampleRate = 44_100
    @Volatile private var requested = Params(0f, emptyList())
    @Volatile private var snapshot = buildSnapshot(requested, sampleRate)

    // Audio-thread state.
    private var channels = 2
    private var applied: Snapshot? = null
    private var currentGain = 1f
    private var states = Array(MAX_STAGES * channels) { BiquadState() }
    private var spare = Array(MAX_STAGES * channels) { BiquadState() }
    private var slotIds = IntArray(MAX_STAGES)
    private var spareIds = IntArray(MAX_STAGES)
    private val matched = IntArray(MAX_STAGES)
    private val used = BooleanArray(MAX_STAGES)

    /** Call when the stream format changes. Resets all filter state. */
    fun configure(sampleRate: Int, channelCount: Int) {
        require(sampleRate > 0 && channelCount > 0)
        this.sampleRate = sampleRate
        channels = channelCount
        states = Array(MAX_STAGES * channelCount) { BiquadState() }
        spare = Array(MAX_STAGES * channelCount) { BiquadState() }
        slotIds.fill(0)
        applied = null
        snapshot = buildSnapshot(requested, sampleRate)
        currentGain = snapshot.preampGain
    }

    fun setParams(preampDb: Float, bands: List<EqBand>) {
        val p = Params(preampDb, bands.toList())
        requested = p
        snapshot = buildSnapshot(p, sampleRate)
    }

    /** True when processing would leave the signal exactly unchanged. */
    val isIdentity: Boolean
        get() {
            val s = snapshot
            return s.count == 0 && s.preampGain == 1f && currentGain == 1f
        }

    /** Clears the filter memory, for example after a seek. Audio thread only. */
    fun resetState() {
        for (s in states) s.reset()
    }

    /** Filters [frames] interleaved frames of [buffer] in place. Does not allocate. */
    fun process(buffer: FloatArray, frames: Int) {
        if (frames <= 0) return
        var snap = snapshot
        if (snap.sampleRate != sampleRate) {
            // Params were built for an older sample rate (rare race with configure): rebuild.
            snap = buildSnapshot(requested, sampleRate)
            snapshot = snap
        }
        if (snap !== applied) adopt(snap)

        val ch = channels
        val n = frames * ch
        val target = snap.preampGain
        if (currentGain != target) {
            val step = (target - currentGain) / frames
            var g = currentGain
            var i = 0
            for (f in 0 until frames) {
                g += step
                for (c in 0 until ch) {
                    buffer[i] *= g
                    i++
                }
            }
            currentGain = target
        } else if (target != 1f) {
            for (i in 0 until n) buffer[i] *= target
        }

        for (j in 0 until snap.count) {
            val coeffs = snap.coefficients[j]
            for (c in 0 until ch) {
                states[j * ch + c].processInterleaved(coeffs, buffer, c, ch, n)
            }
        }
    }

    /** Makes [snap] current, moving the filter state of bands that keep their id to their new slot. */
    private fun adopt(snap: Snapshot) {
        val ch = channels
        val oldCount = applied?.count ?: 0
        used.fill(false)
        for (j in 0 until snap.count) {
            matched[j] = -1
            val id = snap.ids[j]
            for (i in 0 until oldCount) {
                if (!used[i] && slotIds[i] == id) {
                    used[i] = true
                    matched[j] = i
                    break
                }
            }
        }
        var free = 0
        for (j in 0 until MAX_STAGES) {
            var src = if (j < snap.count) matched[j] else -1
            val fresh = src < 0
            if (fresh) {
                while (used[free]) free++
                used[free] = true
                src = free
            }
            for (c in 0 until ch) {
                val st = states[src * ch + c]
                if (fresh) st.reset()
                spare[j * ch + c] = st
            }
            spareIds[j] = if (j < snap.count) snap.ids[j] else 0
        }
        val s = states; states = spare; spare = s
        val ids = slotIds; slotIds = spareIds; spareIds = ids
        applied = snap
    }

    private companion object {
        fun buildSnapshot(p: Params, sampleRate: Int): Snapshot {
            val ids = IntArray(MAX_STAGES)
            val list = ArrayList<BiquadCoefficients>(MAX_STAGES)
            for (b in p.bands) {
                if (list.size >= MAX_STAGES) break
                if (!b.enabled || isBypassGain(b.gainDb)) continue
                ids[list.size] = b.id
                list.add(biquadCoefficients(sampleRate, b.frequencyHz, b.gainDb, b.q, b.type))
            }
            val gain = if (isBypassGain(p.preampDb)) 1f else 10.0.pow(p.preampDb / 20.0).toFloat()
            return Snapshot(sampleRate, gain, list.size, ids, list.toTypedArray())
        }
    }
}
