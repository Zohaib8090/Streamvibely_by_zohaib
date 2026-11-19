package app.streamvibely

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import app.streamvibely.domain.AccountRepository
import app.streamvibely.domain.EqualizerController
import app.streamvibely.domain.ListeningUsageRepository
import app.streamvibely.domain.LibraryRepository
import app.streamvibely.domain.PlatformActions
import app.streamvibely.domain.PlayerController
import app.streamvibely.domain.PlaylistRepository
import app.streamvibely.domain.ThemeRepository
import app.streamvibely.lyrics.LyricsRepository

/** Everything the shared UI needs from the platform, built once per process by the host. */
class AppServices(
    val player: PlayerController,
    val library: LibraryRepository,
    val playlists: PlaylistRepository,
    val equalizer: EqualizerController,
    val lyrics: LyricsRepository,
    val platform: PlatformActions,
    val account: AccountRepository,
    val usage: ListeningUsageRepository,
    val theme: ThemeRepository,
    /** Online music sources the user can search. Local files are not a provider. */
    val providers: List<app.streamvibely.domain.MusicProvider> = defaultProviders(),
    val downloads: app.streamvibely.domain.DownloadRepository = app.streamvibely.domain.NoDownloads,
    /** Null where account import is not available (web, desktop). */
    val importer: app.streamvibely.domain.PlaylistImporter? = null,
    val providerSettings: app.streamvibely.domain.ProviderSettings = app.streamvibely.domain.InMemoryProviderSettings(),
    val qualitySettings: app.streamvibely.domain.PlaybackQualityRepository = app.streamvibely.domain.InMemoryPlaybackQualityRepository(),
    val spotifySync: app.streamvibely.domain.SpotifySyncRepository? = null,
    val musicPreferences: app.streamvibely.domain.MusicPreferencesRepository = app.streamvibely.domain.InMemoryMusicPreferencesRepository(),
    val recentlyPlayed: app.streamvibely.domain.RecentlyPlayedRepository = app.streamvibely.domain.InMemoryRecentlyPlayedRepository(),
)

/** Jamendo's code stays in the project but is hidden from the UI for now. Set to true to show it again. */
private const val SHOW_JAMENDO = false

fun defaultProviders(jamendoClientId: String = ""): List<app.streamvibely.domain.MusicProvider> =
    listOfNotNull(
        app.streamvibely.providers.AudiusProvider(),
        app.streamvibely.providers.InternetArchiveProvider(),
        if (SHOW_JAMENDO) app.streamvibely.providers.JamendoProvider(jamendoClientId) else null,
    )

/** Lets the host (Android) handle the system Back gesture. Null where there is no such gesture. */
interface BackHandlerHost {
    @Composable
    fun Handle(enabled: Boolean, onBack: () -> Unit)
}

val LocalBackHandler = staticCompositionLocalOf<BackHandlerHost?> { null }
