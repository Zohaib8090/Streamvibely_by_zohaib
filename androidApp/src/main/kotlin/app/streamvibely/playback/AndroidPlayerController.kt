package app.streamvibely.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import app.streamvibely.domain.PlayerController
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.Track
import app.streamvibely.widgets.WidgetPlayerState
import app.streamvibely.widgets.updateNowPlayingWidget
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * [PlayerController] backed by a Media3 [MediaController] connected to [PlaybackService].
 * All player access happens on the main thread. Queue order is the original order passed to [play];
 * the shuffled order is not exposed (MediaController does not publish it).
 */
class AndroidPlayerController(context: Context) : PlayerController {
    companion object {
        private const val C_INDEX_UNSET = -1
        /** Placeholder scheme for queued tracks whose stream is looked up just before they play. */
        const val LAZY_SCHEME = "streamvibely"
        private const val URL_TTL_MS = 90 * 60 * 1000L

        // Process-wide so the service keeps working after the activity (and this controller) is recreated.
        private val registry = java.util.concurrent.ConcurrentHashMap<String, Track>()
        private val urlCache = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
        @Volatile private var resolver: (suspend (Track) -> String?)? = null

        /** Called by the playback service on its loader thread when a lazy item is about to load. */
        fun resolveBlocking(trackId: String): String? {
            urlCache[trackId]?.let { (url, at) -> if (System.currentTimeMillis() - at < URL_TTL_MS) return url }
            val track = registry[trackId] ?: return null
            val r = resolver ?: return null
            val url = kotlinx.coroutines.runBlocking {
                kotlinx.coroutines.withTimeoutOrNull(25_000L) { runCatching { r(track) }.getOrNull() }
            } ?: return null
            urlCache[trackId] = url to System.currentTimeMillis()
            return url
        }
    }


    private val appContext = context.applicationContext
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val tracks get() = registry
    private val pending = ArrayDeque<(MediaController) -> Unit>()
    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private var ticker: Job? = null
    private var released = false
    private var lastWidget: Pair<String?, Boolean>? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(PlayerUiState())
    override val state: StateFlow<PlayerUiState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            syncState(player)
        }
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val f = MediaController.Builder(appContext, token).buildAsync()
        future = f
        f.addListener({
            if (released) {
                MediaController.releaseFuture(f)
                return@addListener
            }
            val c = try {
                f.get()
            } catch (e: Exception) {
                pending.clear()
                return@addListener
            }
            controller = c
            c.addListener(listener)
            syncState(c)
            while (pending.isNotEmpty()) pending.removeFirst()(c)
        }, { command -> mainHandler.post(command) })
    }

    private fun withController(block: (MediaController) -> Unit) {
        mainScope.launch {
            val c = controller
            if (c != null) block(c) else if (!released) pending.addLast(block)
        }
    }

    override var trackResolver: (suspend (Track) -> String?)?
        get() = resolver
        set(value) { resolver = value }

    override fun play(track: Track, queue: List<Track>) {
        // Every queued song stays in the queue. Ones without a stream yet are looked up as they come up next.
        val playable = queue.ifEmpty { listOf(track) }
        val startIndex = playable.indexOfFirst { it.id == track.id }
        if (startIndex < 0) return
        withController { c ->
            playable.forEach { tracks[it.id] = it }
            track.uri?.let { urlCache[track.id] = it to System.currentTimeMillis() }
            _state.value = _state.value.copy(queue = playable)
            c.setMediaItems(playable.map(::toMediaItem), startIndex, 0L)
            c.prepare()
            c.play()
        }
    }

    override fun togglePlayPause() = withController { c ->
        if (c.isPlaying || c.playWhenReady) c.pause() else c.play()
    }

    override fun next() = withController { it.seekToNext() }

    override fun previous() = withController { it.seekToPrevious() }

    override fun seekTo(positionMs: Long) = withController { c ->
        c.seekTo(positionMs.coerceAtLeast(0L))
        _state.value = _state.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    override fun playNext(track: Track) {
        withController { c ->
            tracks[track.id] = track
            val at = if (c.mediaItemCount == 0) 0 else c.currentMediaItemIndex + 1
            c.addMediaItem(at, toMediaItem(track))
            val q = _state.value.queue.toMutableList()
            q.removeAll { it.id == track.id }
            q.add(at.coerceAtMost(q.size), track)
            _state.value = _state.value.copy(queue = q)
            if (c.mediaItemCount == 1) { c.prepare(); c.play() }
        }
    }

    override fun stop() = withController { c ->
        c.stop()
        c.clearMediaItems()
        _state.value = _state.value.copy(current = null, isPlaying = false, positionMs = 0L, queue = emptyList())
        updateTicker(c, false)
        pushWidget(null, false)
    }

    override fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    override fun toggleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        if (released) return
        released = true
        ticker?.cancel()
        controller?.removeListener(listener)
        future?.let { MediaController.releaseFuture(it) }
        controller = null
        pending.clear()
        mainScope.cancel()
        widgetScope.cancel()
    }

    private fun toMediaItem(track: Track): MediaItem =
        MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(
                track.uri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
                    ?: Uri.Builder().scheme(LAZY_SCHEME).authority("track").appendQueryParameter("id", track.id).build(),
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    // Online and downloaded covers show in the notification; local files have no cover link.
                    .apply { track.artworkUrl?.takeIf { it.startsWith("https://") || it.startsWith("file://") }?.let { setArtworkUri(Uri.parse(it)) } }
                    .build()
            )
            .build()

    private fun syncState(player: Player) {
        // After the activity is recreated this controller starts empty while the service keeps playing,
        // so rebuild the track from the item's own metadata.
        val current = player.currentMediaItem?.let { item ->
            tracks[item.mediaId] ?: Track(
                id = item.mediaId,
                title = item.mediaMetadata.title?.toString().orEmpty(),
                artist = item.mediaMetadata.artist?.toString().orEmpty(),
                album = item.mediaMetadata.albumTitle?.toString(),
                durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                uri = item.localConfiguration?.uri?.toString(),
                isDownloaded = true,
            ).also { tracks[item.mediaId] = it }
        }
        val playing = player.isPlaying
        // Some online sources do not know the length; the player does once the file is open.
        val known = current?.let { if (it.durationMs <= 0 && player.duration > 0) it.copy(durationMs = player.duration) else it }
        _state.value = _state.value.copy(
            current = known,
            isPlaying = playing,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeat = when (player.repeatMode) {
                Player.REPEAT_MODE_ALL -> app.streamvibely.domain.RepeatMode.All
                Player.REPEAT_MODE_ONE -> app.streamvibely.domain.RepeatMode.One
                else -> app.streamvibely.domain.RepeatMode.Off
            },
            queue = playOrder(player).ifEmpty { _state.value.queue },
        )
        updateTicker(player, playing)
        pushWidget(current, playing)
    }

    /** The queue in the order it will play: the shuffled order when shuffle is on, so "next" matches what you hear. */
    private fun playOrder(player: Player): List<Track> {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) return emptyList()
        val out = ArrayList<Track>()
        var index = timeline.getFirstWindowIndex(player.shuffleModeEnabled)
        val window = androidx.media3.common.Timeline.Window()
        while (index != C_INDEX_UNSET && out.size < timeline.windowCount) {
            val id = timeline.getWindow(index, window).mediaItem.mediaId
            tracks[id]?.let { out += it }
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, player.shuffleModeEnabled)
        }
        return out
    }

    private fun updateTicker(player: Player, playing: Boolean) {
        if (!playing) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return
        ticker = mainScope.launch {
            while (isActive) {
                delay(500)
                _state.value = _state.value.copy(positionMs = player.currentPosition.coerceAtLeast(0L))
            }
        }
    }

    private fun pushWidget(current: Track?, playing: Boolean) {
        val key = current?.id to playing
        if (key == lastWidget) return
        lastWidget = key
        val snapshot = WidgetPlayerState(current?.title.orEmpty(), current?.artist.orEmpty(), playing)
        widgetScope.launch {
            try {
                updateNowPlayingWidget(appContext, snapshot)
            } catch (_: Exception) {
                // Widget updates are best effort.
            }
        }
    }
}
