package app.streamvibely.usage

import android.content.Context
import android.os.SystemClock
import app.streamvibely.domain.DayUsage
import app.streamvibely.domain.ListeningUsageRepository
import app.streamvibely.domain.PlayerController
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.Track
import app.streamvibely.domain.TrackUsage
import app.streamvibely.domain.UsageSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val FILE_NAME = "listening_usage.json"
private const val MS_PER_DAY = 86_400_000L
private const val LOCAL_SOURCE = "local"
private const val TICK_MS = 1_000L

/** The most one tick may credit. Gaps (process suspended, slow emissions) cannot inflate totals. */
private const val MAX_SLICE_MS = 2_000L
private const val SAVE_INTERVAL_MS = 15_000L
private const val PLAY_THRESHOLD_MS = 30_000L
private const val TOP_TRACKS = 10

private fun todayEpochDay(): Long = System.currentTimeMillis() / MS_PER_DAY

private fun isCounting(s: PlayerUiState): Boolean = s.isPlaying && s.current != null

private fun buildSummary(data: UsageData, today: Long): UsageSummary {
    val days = (today - WINDOW_DAYS + 1..today).map { day ->
        val bucket = data.days[day]
        DayUsage(day, bucket?.offlineMs ?: 0L, bucket?.onlineMs ?: 0L)
    }
    val top = data.tracks.values
        .sortedByDescending { it.listenedMs }
        .take(TOP_TRACKS)
        .map { TrackUsage(it.title, it.artist, it.listenedMs, it.plays) }
    return UsageSummary(
        days = days,
        totalOfflineMs = days.sumOf { it.offlineMs },
        totalOnlineMs = days.sumOf { it.onlineMs },
        topTracks = top,
        todayEpochDay = today,
    )
}

/**
 * Measures listening time from [player] state and keeps it on this device only.
 *
 * Time is wall clock (SystemClock.elapsedRealtime), so seeking does not change it. A track counts
 * a play once it has been current for [PLAY_THRESHOLD_MS] of playing time; a track change starts a
 * new run. All state changes run on one serial context, so no locks are needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AndroidListeningUsageRepository(
    context: Context,
    private val player: PlayerController,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : ListeningUsageRepository {

    private val serial = Dispatchers.Default.limitedParallelism(1)
    private val store = UsageStore(File(context.filesDir, FILE_NAME))
    private var rollDay = todayEpochDay()
    private val data: UsageData = store.load(rollDay)

    private val _summary = MutableStateFlow(buildSummary(data, rollDay))
    override val summary: StateFlow<UsageSummary> = _summary.asStateFlow()

    private var current = PlayerUiState()
    private var lastMarkMs = SystemClock.elapsedRealtime()
    private var lastSaveMs = lastMarkMs
    private var dirty = false
    private var changed = false
    private var ticker: Job? = null

    private var runTrackId: String? = null
    private var runMs = 0L
    private var runCounted = false

    init {
        scope.launch(serial) {
            player.state.collect { onPlayerState(it) }
        }
    }

    override suspend fun clear() {
        withContext(serial) {
            advance()
            data.days.clear()
            data.tracks.clear()
            runMs = 0L
            runCounted = false
            dirty = false
            store.delete()
            changed = true
            publish()
        }
    }

    private fun onPlayerState(next: PlayerUiState) {
        advance()
        val wasCounting = isCounting(current)
        val nextTrackId = next.current?.id
        if (nextTrackId != runTrackId) {
            runTrackId = nextTrackId
            runMs = 0L
            runCounted = false
        }
        current = next
        if (isCounting(next)) startTicker() else stopTicker()
        if (wasCounting && !isCounting(next) && dirty) save()
        publish()
    }

    /** Credits the wall-clock time since the last mark to the track that was playing. */
    private fun advance() {
        val now = SystemClock.elapsedRealtime()
        val slice = (now - lastMarkMs).coerceIn(0L, MAX_SLICE_MS)
        lastMarkMs = now
        checkRollover(todayEpochDay())
        val track = current.current ?: return
        if (current.isPlaying && slice > 0L) credit(track, slice)
    }

    private fun checkRollover(today: Long) {
        if (today == rollDay) return
        rollDay = today
        if (data.dropDaysBefore(today - WINDOW_DAYS + 1)) dirty = true
        changed = true
    }

    private fun credit(track: Track, ms: Long) {
        val day = data.days.getOrPut(rollDay) { DayBucket() }
        if (track.source == LOCAL_SOURCE) day.offlineMs += ms else day.onlineMs += ms

        val total = data.tracks.getOrPut(track.id) { TrackBucket(track.title, track.artist) }
        total.title = track.title
        total.artist = track.artist
        total.listenedMs += ms
        data.trimTracks(track.id)

        runMs += ms
        if (!runCounted && runMs >= PLAY_THRESHOLD_MS) {
            runCounted = true
            total.plays++
        }
        dirty = true
        changed = true
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch(serial) {
            while (true) {
                delay(TICK_MS)
                onTick()
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private fun onTick() {
        advance()
        if (dirty && isCounting(current) && SystemClock.elapsedRealtime() - lastSaveMs >= SAVE_INTERVAL_MS) {
            save()
        }
        publish()
    }

    /** Writes the file. On failure the data stays dirty and the next attempt retries. Logs nothing. */
    private fun save() {
        try {
            store.save(data)
            dirty = false
            lastSaveMs = SystemClock.elapsedRealtime()
        } catch (e: IOException) {
            // Keep dirty; retried on the next save point.
        }
    }

    private fun publish() {
        if (!changed) return
        changed = false
        _summary.value = buildSummary(data, rollDay)
    }
}
