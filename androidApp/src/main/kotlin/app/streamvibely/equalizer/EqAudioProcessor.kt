package app.streamvibely.equalizer

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import app.streamvibely.audio.dsp.EqChain
import app.streamvibely.audio.dsp.SpectrumAnalyzer
import java.nio.ByteBuffer

/**
 * Media3 audio processor that runs the parametric [EqChain] on 16-bit or float PCM and feeds the
 * [SpectrumAnalyzer]. Output uses the same encoding as the input. Other encodings are not handled
 * (the processor is bypassed for them).
 */
@OptIn(UnstableApi::class)
class EqAudioProcessor : BaseAudioProcessor() {
    val chain = EqChain()
    val analyzer = SpectrumAnalyzer()

    private var floatInput = false
    private var channels = 2
    private var work = FloatArray(0)

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) return AudioFormat.NOT_SET
        if (inputAudioFormat.channelCount < 1 || inputAudioFormat.sampleRate < 1) return AudioFormat.NOT_SET
        floatInput = enc == C.ENCODING_PCM_FLOAT
        channels = inputAudioFormat.channelCount
        chain.configure(inputAudioFormat.sampleRate, channels)
        analyzer.configure(inputAudioFormat.sampleRate)
        return inputAudioFormat
    }

    override fun onFlush() {
        chain.resetState()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerSample = if (floatInput) 4 else 2
        val frames = inputBuffer.remaining() / (bytesPerSample * channels)
        if (frames == 0) {
            inputBuffer.position(inputBuffer.limit())
            return
        }
        val samples = frames * channels
        if (work.size < samples) work = FloatArray(samples)
        val start = inputBuffer.position()

        if (floatInput) {
            for (i in 0 until samples) work[i] = inputBuffer.getFloat(start + i * 4)
        } else {
            for (i in 0 until samples) work[i] = inputBuffer.getShort(start + i * 2) * (1f / 32768f)
        }
        inputBuffer.position(start + samples * bytesPerSample)

        val identity = chain.isIdentity
        chain.process(work, frames)
        analyzer.feed(work, frames, channels)

        val out = replaceOutputBuffer(samples * bytesPerSample)
        if (identity && !floatInput) {
            // Nothing to apply: pass the original 16-bit samples through bit-exact.
            for (i in 0 until samples) out.putShort(inputBuffer.getShort(start + i * 2))
        } else if (floatInput) {
            for (i in 0 until samples) out.putFloat(work[i])
        } else {
            for (i in 0 until samples) {
                val v = (work[i] * 32768f).coerceIn(-32768f, 32767f)
                out.putShort(v.toInt().toShort())
            }
        }
        out.flip()
    }
}
