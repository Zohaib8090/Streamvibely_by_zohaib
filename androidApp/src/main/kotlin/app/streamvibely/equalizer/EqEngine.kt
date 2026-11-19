package app.streamvibely.equalizer

import app.streamvibely.domain.EqBand
import app.streamvibely.domain.SPECTRUM_BINS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val TICK_MS = 33L
private const val SILENT_TICKS = 15 // about half a second without audio

/**
 * Process-wide equalizer engine. The playback service plugs [processor] into the audio sink; the
 * controller pushes parameters with [update] and the UI reads [spectrum]. The spectrum is only
 * computed (on [Dispatchers.Default], never on the audio thread) while someone collects it and
 * audio is flowing.
 */
object EqEngine {
    val processor = EqAudioProcessor()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val zeros = FloatArray(SPECTRUM_BINS)
    private val _spectrum = MutableStateFlow(zeros)
    val spectrum: StateFlow<FloatArray> = _spectrum.asStateFlow()

    @Volatile
    var enabled: Boolean = true
        private set

    @Volatile
    var preampDb: Float = 0f
        private set

    @Volatile
    var bands: List<EqBand> = emptyList()
        private set

    /** Applies new parameters. A disabled equalizer behaves as flat with no preamp. */
    fun update(enabled: Boolean, preampDb: Float, bands: List<EqBand>) {
        this.enabled = enabled
        this.preampDb = preampDb
        this.bands = bands
        if (enabled) processor.chain.setParams(preampDb, bands) else processor.chain.setParams(0f, emptyList())
    }

    init {
        scope.launch {
            _spectrum.subscriptionCount.map { it > 0 }.distinctUntilChanged().collectLatest { watched ->
                if (watched) tickLoop() else publish(zeros)
            }
        }
    }

    private suspend fun tickLoop() {
        val analyzer = processor.analyzer
        var lastCount = analyzer.writeCount
        var idleTicks = 0
        var silent = true
        analyzer.reset()
        while (true) {
            delay(TICK_MS)
            val count = analyzer.writeCount
            if (count != lastCount) {
                lastCount = count
                idleTicks = 0
                silent = false
                val bins = FloatArray(SPECTRUM_BINS)
                analyzer.computeBins(bins)
                publish(bins)
            } else if (!silent && ++idleTicks >= SILENT_TICKS) {
                silent = true
                analyzer.reset()
                publish(zeros)
            }
        }
    }

    private fun publish(bins: FloatArray) {
        _spectrum.value = bins
    }
}
