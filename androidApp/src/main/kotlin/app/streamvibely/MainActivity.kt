package app.streamvibely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import app.streamvibely.domain.PlayerController
import app.streamvibely.domain.Track
import app.streamvibely.equalizer.AndroidEqualizerController
import app.streamvibely.library.AndroidLibraryRepository
import app.streamvibely.library.AndroidPlatformActions
import app.streamvibely.library.AndroidPlaylistRepository
import app.streamvibely.lyrics.LrclibProvider
import app.streamvibely.lyrics.LyricsRepository
import app.streamvibely.permissions.AppPermission
import app.streamvibely.permissions.PermissionRationaleDialog
import app.streamvibely.permissions.isGranted
import app.streamvibely.permissions.rememberPermissionRequester
import app.streamvibely.playback.AndroidPlayerController
import app.streamvibely.usage.AndroidListeningUsageRepository
import app.streamvibely.artwork.AndroidArtworkLoader
import app.streamvibely.theme.AndroidThemeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import app.streamvibely.ui.common.LocalArtworkLoader

class MainActivity : ComponentActivity() {
    private lateinit var player: AndroidPlayerController
    private lateinit var equalizer: AndroidEqualizerController
    private val askNotifications = mutableStateOf(false)
    private var askedNotificationsOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContext = applicationContext

        // The platform actions register activity-result launchers, so they must be built here.
        val platform = AndroidPlatformActions(this)
        val googleAuth = app.streamvibely.account.GoogleAuthorizer(this)
        val youtube = app.streamvibely.account.YouTubeAccount(appContext) { interactive -> googleAuth.accessToken(interactive) }
        val library = AndroidLibraryRepository(appContext)
        val lyricsRepository = LyricsRepository { listOf(app.streamvibely.download.LocalLrcProvider(), LrclibProvider()) }
        val downloads = app.streamvibely.download.AndroidDownloadRepository(appContext) { lyricsRepository.lyricsFor(it) }
        val spotifySync = app.streamvibely.spotify.AndroidSpotifySyncRepository(appContext)
        val qualitySettings = AndroidPlaybackQualityRepository(appContext)
        val combinedExtraTracks = combine(
            downloads.downloaded,
            spotifySync.syncedTracks,
        ) { dl, sp -> dl + sp }.stateIn(
            CoroutineScope(SupervisorJob() + Dispatchers.Default),
            SharingStarted.Eagerly,
            downloads.downloaded.value + spotifySync.syncedTracks.value,
        )
        val playlists = AndroidPlaylistRepository(appContext, library, combinedExtraTracks)
        player = AndroidPlayerController(appContext)
        equalizer = AndroidEqualizerController(appContext)
        val artwork = AndroidArtworkLoader(appContext)
        val youtubeStreamResolver = app.streamvibely.playback.resolve.YouTubeStreamResolver(appContext)
        app.streamvibely.playback.PlaybackService.streamRefresher = youtubeStreamResolver

        val services = AppServices(
            player = AskNotificationsOnFirstPlay(player) {
                if (!askedNotificationsOnce && !isGranted(appContext, AppPermission.Notifications)) {
                    askedNotificationsOnce = true
                    askNotifications.value = true
                }
            },
            library = library,
            playlists = playlists,
            equalizer = equalizer,
            lyrics = lyricsRepository,
            platform = platform,
            account = youtube,
            importer = youtube,
            usage = AndroidListeningUsageRepository(appContext, player),
            theme = AndroidThemeRepository(appContext, CoroutineScope(SupervisorJob() + Dispatchers.IO)),
            downloads = downloads,
            // Two YouTube sources: Music category only, and all videos. Both list through the official API.
            providers = app.streamvibely.defaultProviders(BuildConfig.JAMENDO_CLIENT_ID) +
                app.streamvibely.providers.YouTubeProvider({ youtube.accessTokenOrNull() }, youtubeStreamResolver, musicOnly = true) +
                app.streamvibely.providers.YouTubeProvider({ youtube.accessTokenOrNull() }, youtubeStreamResolver, musicOnly = false),
            providerSettings = AndroidProviderSettings(appContext),
            qualitySettings = qualitySettings,
            spotifySync = spotifySync,
            musicPreferences = AndroidMusicPreferencesRepository(appContext),
            recentlyPlayed = app.streamvibely.playback.AndroidRecentlyPlayedRepository(appContext),
        )

        setContent {
            val requestNotifications = rememberPermissionRequester(AppPermission.Notifications) {}
            CompositionLocalProvider(
                LocalArtworkLoader provides artwork,
                LocalBackHandler provides AndroidBackHost,
            ) {
                // targetSdk 35 draws behind system bars; keep content clear of them.
                Box(Modifier.safeDrawingPadding()) {
                    App(services)
                    if (askNotifications.value) {
                        PermissionRationaleDialog(
                            permission = AppPermission.Notifications,
                            onContinue = { askNotifications.value = false; requestNotifications() },
                            onDismiss = { askNotifications.value = false },
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            player.release()
            equalizer.release()
        }
        super.onDestroy()
    }
}

/** Asks for the notification permission (with an explanation) the first time playback starts. */
private class AskNotificationsOnFirstPlay(
    private val delegate: PlayerController,
    private val onFirstPlay: () -> Unit,
) : PlayerController by delegate {
    override fun play(track: Track, queue: List<Track>) {
        onFirstPlay()
        delegate.play(track, queue)
    }

    override fun playNext(track: Track) = delegate.playNext(track)

    override fun stop() = delegate.stop()
}

private object AndroidBackHost : BackHandlerHost {
    @Composable
    override fun Handle(enabled: Boolean, onBack: () -> Unit) = BackHandler(enabled = enabled, onBack = onBack)
}
