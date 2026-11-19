package app.streamvibely.playback

import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import androidx.media3.session.MediaSessionService
import app.streamvibely.equalizer.EqEngine

/** Owns the ExoPlayer and its MediaSession so playback survives the UI and drives the notification. */
class PlaybackService : MediaSessionService() {

    companion object {
        var streamRefresher: ChunkedRangeDataSource.StreamRefresher? = null
    }

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    private val shuffleCommand = SessionCommand("app.streamvibely.SHUFFLE", Bundle.EMPTY)
    private val repeatCommand = SessionCommand("app.streamvibely.REPEAT", Bundle.EMPTY)

    private val sessionListener = object : Player.Listener {
        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            publishSessionId(audioSessionId)
        }

        // A song that cannot be loaded must not stall the queue: skip to the next one.
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            val p = player ?: return
            if (p.hasNextMediaItem()) {
                p.seekToNextMediaItem()
                p.prepare()
                p.play()
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = refreshButtons()
        override fun onRepeatModeChanged(repeatMode: Int) = refreshButtons()
    }

    /** Lets the notification, lock screen and car/Bluetooth controls toggle shuffle and cycle repeat like the app does. */
    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(shuffleCommand).add(repeatCommand).build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(commands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            val p = player
            if (p != null) when (customCommand.customAction) {
                shuffleCommand.customAction -> p.shuffleModeEnabled = !p.shuffleModeEnabled
                repeatCommand.customAction -> p.repeatMode = when (p.repeatMode) {
                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                    Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    private fun refreshButtons() {
        val p = player ?: return
        val shuffle = CommandButton.Builder(if (p.shuffleModeEnabled) CommandButton.ICON_SHUFFLE_ON else CommandButton.ICON_SHUFFLE_OFF)
            .setDisplayName(if (p.shuffleModeEnabled) "Shuffle on" else "Shuffle off")
            .setSessionCommand(shuffleCommand)
            .build()
        val repeat = CommandButton.Builder(
            when (p.repeatMode) {
                Player.REPEAT_MODE_ALL -> CommandButton.ICON_REPEAT_ALL
                Player.REPEAT_MODE_ONE -> CommandButton.ICON_REPEAT_ONE
                else -> CommandButton.ICON_REPEAT_OFF
            },
        )
            .setDisplayName("Repeat")
            .setSessionCommand(repeatCommand)
            .build()
        session?.setMediaButtonPreferences(ImmutableList.of(shuffle, repeat))
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        // The audio sink runs every buffer through the parametric equalizer.
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean,
            ): AudioSink = DefaultAudioSink.Builder(context)
                .setAudioProcessors(arrayOf<AudioProcessor>(EqEngine.processor))
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
        }

        // Bounded range requests (<= 512 KB) for YouTube / googlevideo media to prevent 403 throttles mid-song.
        val defaultDataSourceFactory = DefaultDataSource.Factory(this)
        val chunkedDataSourceFactory = DataSource.Factory {
            ChunkedRangeDataSource(defaultDataSourceFactory.createDataSource(), refresher = streamRefresher)
        }
        // Local audio stream disk cache — streamed songs are cached in device storage for instant replay
        val cache = MediaCache.get(this)
        val cachedDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(chunkedDataSourceFactory)
            .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Queued songs without a stream yet carry a placeholder address; it is swapped for the real stream
        // right before ExoPlayer loads them, which is also when it pre-buffers the next song.
        val resolvingFactory = androidx.media3.datasource.ResolvingDataSource.Factory(cachedDataSourceFactory) { spec ->
            if (spec.uri.scheme == AndroidPlayerController.LAZY_SCHEME) {
                val id = spec.uri.getQueryParameter("id") ?: throw java.io.IOException("Queued song has no id")
                val url = AndroidPlayerController.resolveBlocking(id)
                    ?: throw java.io.IOException("Could not find a stream for this song")
                spec.withUri(android.net.Uri.parse(url))
            } else spec
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(resolvingFactory)

        val exo = ExoPlayer.Builder(this, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo.addListener(sessionListener)
        player = exo
        session = MediaSession.Builder(this, exo).setCallback(sessionCallback).build()
        refreshButtons()
        publishSessionId(exo.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0 ||
            p.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        AudioSession.publish(null)
        session?.release()
        session = null
        player?.run {
            removeListener(sessionListener)
            release()
        }
        player = null
        super.onDestroy()
    }

    private fun publishSessionId(id: Int) {
        AudioSession.publish(if (id == C.AUDIO_SESSION_ID_UNSET) null else id)
    }
}
