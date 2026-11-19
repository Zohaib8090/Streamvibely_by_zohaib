package app.streamvibely

import app.streamvibely.ui.common.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.streamvibely.domain.Account
import app.streamvibely.domain.EqualizerState
import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.ProviderOption
import app.streamvibely.domain.isOn
import app.streamvibely.domain.best
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.Playlist
import app.streamvibely.domain.ScanState
import app.streamvibely.domain.Track
import app.streamvibely.domain.ThemeSpec
import app.streamvibely.theme.AppBackground
import app.streamvibely.theme.AppTheme
import app.streamvibely.theme.StreamvibelyTheme
import app.streamvibely.theme.rememberArtSeed
import app.streamvibely.ui.customization.CustomizationScreen
import androidx.compose.foundation.layout.safeDrawingPadding
import app.streamvibely.theme.Tokens
import app.streamvibely.theme.SurfaceKind
import app.streamvibely.theme.appSurface
import app.streamvibely.theme.scaled
import app.streamvibely.ui.account.AccountButton
import app.streamvibely.ui.account.AccountDrawerHost
import app.streamvibely.ui.account.AccountScreen
import app.streamvibely.ui.account.rememberAccountDrawerState
import app.streamvibely.ui.common.AppIcon
import app.streamvibely.ui.common.ArtworkImage
import app.streamvibely.ui.equalizer.EqualizerPanel
import app.streamvibely.ui.equalizer.EqualizerScreen
import app.streamvibely.ui.library.LibraryScreen
import app.streamvibely.ui.library.LibraryTab
import app.streamvibely.ui.onboarding.OnboardingScreen
import app.streamvibely.ui.nowplaying.NowPlayingScreen
import app.streamvibely.ui.nowplaying.PlayerSheet
import app.streamvibely.ui.nowplaying.QueueScreen
import app.streamvibely.ui.playlist.AddToPlaylistDialog
import app.streamvibely.ui.playlist.CreatePlaylistDialog
import app.streamvibely.ui.playlist.PlaylistScreen
import app.streamvibely.ui.search.SearchScreen
import app.streamvibely.ui.settings.SettingsScreen
import app.streamvibely.ui.usage.ListeningUsageScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class Tab(val label: String, val icon: ImageVector, val iconSelected: ImageVector) {
    Home("Home", AppIcons.Home, AppIcons.HomeFilled),
    Search("Search", AppIcons.Search, AppIcons.SearchFilled),
    Library("Your library", AppIcons.LibraryMusic, AppIcons.LibraryMusicFilled),
}

/** Pages that cover the tab content. Simple state-based navigation; a real router comes later. */
private sealed interface Overlay {
    data class PlaylistOpen(val id: String) : Overlay
    data object Equalizer : Overlay
    data object Settings : Overlay
    data object Account : Overlay
    data object Usage : Overlay
    data object Customization : Overlay
    data object Onboarding : Overlay
}

private data class QualityUpgradeOffer(
    val trackId: String,
    val newUrl: String,
    val label: String,
    val provider: String,
)

private enum class SheetPage { NowPlaying, Queue }

private enum class ShellLayout { Compact, Rail, Expanded }

private const val ALL_SONGS_ID = "all-songs"
private const val DOWNLOADS_ID = "downloads"
private val NavHeight = 64.dp
private val MiniHeight = 64.dp

private const val THIS_DEVICE = "This device"
private const val ALL_SOURCES = "All"

@Composable
fun App(services: AppServices = remember { fakeServices() }) {
    val spec by services.theme.spec.collectAsState()
    val player by services.player.state.collectAsState()
    val artSeed = rememberArtSeed(player.current)
    StreamvibelyTheme(spec, artSeed) {
        val backdrop = app.streamvibely.theme.rememberArtBackdrop(player.current, spec.style == app.streamvibely.domain.ThemeStyle.Glass)
        AppBackground(Modifier.fillMaxSize(), backdrop = backdrop) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { AppShell(services, spec, artSeed) }
        }
    }
}

@Composable
private fun AppShell(services: AppServices, spec: ThemeSpec, artSeed: Int?) {
    val scope = rememberCoroutineScope()
    val player by services.player.state.collectAsState()
    val libraryTracks by services.library.tracks.collectAsState()
    val downloaded by services.downloads.downloaded.collectAsState()
    val downloadProgress by services.downloads.progress.collectAsState()
    val syncedTracks by (services.spotifySync?.syncedTracks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val recentTracks by services.recentlyPlayed.recentTracks.collectAsState()
    // Downloaded online tracks and synced Spotify tracks behave like library tracks (playlists, All songs, search, playback).
    val tracks = remember(libraryTracks, downloaded, syncedTracks) {
        val seen = HashSet<String>()
        val list = ArrayList<Track>(libraryTracks.size + downloaded.size + syncedTracks.size)
        for (t in libraryTracks) { if (seen.add(t.id)) list.add(t) }
        for (t in downloaded) { if (seen.add(t.id)) list.add(t) }
        for (t in syncedTracks) { if (seen.add(t.id)) list.add(t) }
        list
    }
    val scanState by services.library.scanState.collectAsState()
    val librarySettings by services.library.settings.collectAsState()
    val savedPlaylists by services.playlists.playlists.collectAsState()
    val eq by services.equalizer.state.collectAsState()
    val account by services.account.account.collectAsState()
    val usage by services.usage.summary.collectAsState()
    val drawerState = rememberAccountDrawerState()
    val qualityPreferences by services.qualitySettings.preferences.collectAsState()
    val spotifyConnected by (services.spotifySync?.isConnected ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val spotifySyncState by (services.spotifySync?.syncState ?: kotlinx.coroutines.flow.MutableStateFlow(app.streamvibely.domain.SpotifySyncState.Idle)).collectAsState()
    val spotifyStatusMessage by (services.spotifySync?.statusMessage ?: kotlinx.coroutines.flow.MutableStateFlow(null)).collectAsState()
    val musicPrefs by services.musicPreferences.preferences.collectAsState()

    var tab by remember { mutableStateOf(Tab.Home) }
    // Show onboarding overlay on very first launch (before onboarding_complete is set).
    var overlay by remember { mutableStateOf<Overlay?>(if (!musicPrefs.onboardingComplete) Overlay.Onboarding else null) }
    var libraryTab by remember { mutableStateOf(LibraryTab.Playlists) }
    var eqPanelOpen by remember { mutableStateOf(false) }
    var playerExpanded by remember { mutableStateOf(false) }
    var sheetPage by remember { mutableStateOf(SheetPage.NowPlaying) }
    var query by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var showLyrics by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var addTarget by remember { mutableStateOf<Track?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var createFor by remember { mutableStateOf<Track?>(null) }
    var permissionTick by remember { mutableStateOf(0) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    // Quality/source label for the now playing screen, populated at play time.
    var nowPlayingQuality by remember { mutableStateOf<String?>(null) }
    var nowPlayingSource by remember { mutableStateOf<String?>(null) }
    var upgradeOffer by remember { mutableStateOf<QualityUpgradeOffer?>(null) }

    LaunchedEffect(query) { delay(150); debouncedQuery = query }

    // Lyrics are only requested while the lyrics view is open, so no track names leave the device otherwise.
    val currentTrack = player.current
    LaunchedEffect(currentTrack?.id, showLyrics) {
        if (showLyrics && currentTrack != null) {
            lyrics = null
            lyrics = services.lyrics.lyricsFor(currentTrack)
        }
    }

    val results = remember(debouncedQuery, tracks) {
        if (debouncedQuery.isBlank()) emptyList()
        else tracks.filter { it.title.contains(debouncedQuery, true) || it.artist.contains(debouncedQuery, true) }.take(60)
    }
    val providerChoices by services.providerSettings.choices.collectAsState()
    val activeProviders = remember(services.providers, providerChoices) { services.providers.filter { it.isOn(providerChoices) } }
    var pickedSource by remember { mutableStateOf(ALL_SOURCES) }
    // A source that was switched off in Settings can no longer be the selected chip.
    val searchSource = if (pickedSource == ALL_SOURCES || pickedSource == THIS_DEVICE || activeProviders.any { it.displayName == pickedSource }) pickedSource else ALL_SOURCES
    var onlineResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var onlineLoading by remember { mutableStateOf(false) }
    var searchNotice by remember { mutableStateOf<String?>(null) }
    // Whether the notification player can show. Re-read while Settings is open so it updates after the user comes back from system settings.
    var notificationsOn by remember { mutableStateOf(true) }
    var mediaCacheSize by remember { mutableStateOf(0L) }
    LaunchedEffect(overlay == Overlay.Settings) {
        if (overlay == Overlay.Settings) {
            mediaCacheSize = withContext(Dispatchers.Default) { services.platform.getMediaCacheSizeBytes() }
        }
        while (overlay == Overlay.Settings) {
            notificationsOn = services.platform.notificationsEnabled()
            delay(1000)
        }
    }
    LaunchedEffect(overlay) { searchNotice = null }
    // The user's own YouTube playlists, read through the official API once signed in (about 1 quota unit per call).
    var youtubePlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    LaunchedEffect(account) {
        val importer = services.importer
        if (account == null || importer == null) {
            youtubePlaylists = emptyList()
            return@LaunchedEffect
        }
        while (true) {
            val lists = runCatching { importer.playlists() }.getOrNull().orEmpty().take(25)
            if (lists.isNotEmpty()) {
                val resolved = lists.mapNotNull { remote ->
                    val items = runCatching { importer.items(remote.id) }.getOrNull() ?: return@mapNotNull null
                    val trks = items.filter { it.videoId.isNotEmpty() }.map {
                        Track(
                            id = "yt:${it.videoId}",
                            title = it.title,
                            artist = it.artist.removeSuffix(" - Topic").ifBlank { "YouTube" },
                            artworkUrl = it.thumbnailUrl,
                            source = "youtube",
                            uri = null,
                        )
                    }
                    Playlist("yt-pl:${remote.id}", remote.title, "From your YouTube account", trks, trks.firstOrNull()?.artworkUrl)
                }
                youtubePlaylists = resolved
            }
            delay(10 * 60 * 1000L)
        }
    }

    // Continuously keep Spotify library in sync in background as you listen to songs
    LaunchedEffect(spotifyConnected) {
        val sync = services.spotifySync
        if (!spotifyConnected || sync == null) return@LaunchedEffect
        while (true) {
            if (sync.syncState.value != app.streamvibely.domain.SpotifySyncState.Syncing) {
                runCatching { sync.syncAll(services.playlists) }
            }
            delay(5 * 60 * 1000L)
        }
    }
    // Finds a playable stream for a track that has none yet (Spotify-synced, YouTube...). Used for the tapped
    // song and, through the player's resolver hook, for every song that comes up next in the queue.
    val labels = remember { HashMap<String, String>() }
    val resolvedProviderNames = remember { HashMap<String, String>() }

    fun formatCandidateQuality(candidate: app.streamvibely.domain.StreamCandidate): String {
        val codec = when {
            candidate.mimeType?.contains("webm", ignoreCase = true) == true -> "Opus"
            candidate.mimeType?.contains("mp4", ignoreCase = true) == true -> "AAC"
            candidate.mimeType?.contains("mp3", ignoreCase = true) == true -> "MP3"
            candidate.mimeType?.contains("flac", ignoreCase = true) == true -> "FLAC"
            else -> "Audio"
        }
        return candidate.bitrateKbps?.let { "$codec $it kbps" } ?: "Streaming"
    }

    suspend fun resolveStream(t: Track): app.streamvibely.domain.StreamCandidate? {
        val pref = services.qualitySettings.getPreference(t.source)
        val provider = services.providers.firstOrNull { it.id == t.source }
        val candidateWithSource: Pair<app.streamvibely.domain.StreamCandidate, String>? = if (t.source == "spotify" || (provider == null && t.uri == null)) {
            // Track has no direct audio (e.g. Spotify): search across ALL available enabled sources concurrently!
            val query = "${t.title} ${t.artist}".trim()
            val candidateProviders = activeProviders.filter { it.canPlay }
            if (candidateProviders.isEmpty()) null
            else kotlinx.coroutines.coroutineScope {
                val channel = Channel<Pair<app.streamvibely.domain.StreamCandidate, String>>(capacity = candidateProviders.size)
                candidateProviders.forEach { p ->
                    launch {
                        runCatching {
                            val match = p.search(query).firstOrNull() ?: return@runCatching
                            val pPref = services.qualitySettings.getPreference(match.source)
                            val cand = p.streams(match, pPref.maxBitrateKbps).best() ?: return@runCatching
                            channel.send(Pair(cand, p.displayName))
                        }
                    }
                }
                // Fastest provider response wins immediately!
                val firstMatch = runCatching {
                    withTimeoutOrNull(4500L) { channel.receive() }
                }.getOrNull()

                if (firstMatch != null) {
                    val initialKbps = firstMatch.first.bitrateKbps ?: 128
                    scope.launch {
                        var bestKbps = initialKbps
                        val started = kotlin.time.TimeSource.Monotonic.markNow()
                        fun msLeft() = 3500L - started.elapsedNow().inWholeMilliseconds
                        while (msLeft() > 0) {
                            val next = runCatching {
                                withTimeoutOrNull(msLeft().coerceAtLeast(100L)) {
                                    channel.receive()
                                }
                            }.getOrNull() ?: break
                            val nextKbps = next.first.bitrateKbps ?: 128
                            if (nextKbps > bestKbps && next.first.url.isNotBlank()) {
                                bestKbps = nextKbps
                                val nextLabel = formatCandidateQuality(next.first)
                                upgradeOffer = QualityUpgradeOffer(
                                    trackId = t.id,
                                    newUrl = next.first.url,
                                    label = "$nextLabel (via ${next.second})",
                                    provider = next.second,
                                )
                            }
                        }
                    }
                }
                firstMatch
            }
        } else {
            provider?.let { runCatching { it.streams(t, pref.maxBitrateKbps).best() }.getOrNull() }?.let {
                Pair(it, provider.displayName)
            }
        }

        val candidate = candidateWithSource?.first
        val providerName = candidateWithSource?.second ?: provider?.displayName ?: t.source
        if (candidate != null) {
            resolvedProviderNames[t.id] = providerName
            labels[t.id] = formatCandidateQuality(candidate)
        }
        return candidate
    }
    LaunchedEffect(Unit) {
        services.player.trackResolver = { t ->
            downloaded.firstOrNull { it.id == t.id }?.uri ?: resolveStream(t)?.url
        }
    }
    // Keeps the "source / quality" line right when the player moves to the next song by itself.
    LaunchedEffect(player.current?.id) {
        val cur = player.current ?: return@LaunchedEffect
        nowPlayingSource = when {
            cur.isDownloaded || cur.source == "download" || downloaded.any { it.id == cur.id } -> "Downloaded (Offline)"
            cur.source == "local" -> "On this device"
            cur.source == "spotify" -> resolvedProviderNames[cur.id]?.let { "Spotify (via $it)" } ?: "Spotify"
            else -> services.providers.firstOrNull { it.id == cur.source }?.displayName ?: cur.source
        }
        nowPlayingQuality = when {
            cur.isDownloaded || cur.source == "download" || downloaded.any { it.id == cur.id } -> "Offline storage"
            else -> labels[cur.id] ?: cur.qualityLabel ?: if (cur.source == "local") "Local file" else "Streaming"
        }
    }
    // One tap-to-play path for every list: tracks without a stream URL are resolved when tapped (and ahead of time when queued).
    val playTrack: (Track, List<Track>) -> Unit = { t, queue ->
        services.recentlyPlayed.recordPlay(t)
        // If this track was downloaded locally, prefer the local file directly so offline playback works
        val downloadedTrack = downloaded.firstOrNull { it.id == t.id }
        val target = downloadedTrack ?: t
        if (target.uri != null) {
            val localQueue = queue.map { q -> downloaded.firstOrNull { it.id == q.id } ?: q }
            services.player.play(target, localQueue)
        } else scope.launch {
            val candidate = resolveStream(t)
            val url = candidate?.url
            if (url == null) {
                val provider = services.providers.firstOrNull { it.id == t.source }
                searchNotice = if (t.source == "spotify") {
                    "Could not find an audio stream for \"${t.title}\"."
                } else {
                    "Can't play this yet: no stream is set up for ${provider?.displayName ?: "this source"} (see docs/custom-providers.md)."
                }
            } else {
                searchNotice = null
                val ready = t.copy(uri = url, qualityLabel = labels[t.id])
                services.player.play(ready, queue.map { if (it.id == t.id) ready else it })
            }
        }
    }
    val downloadTrack: (Track) -> Unit = { t ->
        if (!t.isDownloaded && !downloadProgress.containsKey(t.id)) {
            scope.launch {
                val ready = if (t.uri != null && t.uri.startsWith("https://")) {
                    t
                } else {
                    val resolved = resolveStream(t)
                    resolved?.url?.let { t.copy(uri = it, canDownload = true) }
                }
                if (ready != null && ready.uri != null) {
                    services.downloads.download(ready.copy(canDownload = true))
                }
            }
        }
    }
    var playlistDownloadJob by remember { mutableStateOf<Job?>(null) }
    val downloadPlaylist: (Playlist) -> Unit = { p ->
        playlistDownloadJob?.cancel()
        playlistDownloadJob = scope.launch(Dispatchers.Default) {
            val todo = p.tracks.filter { it.source != "local" && !it.isDownloaded }
            for (t in todo) {
                if (!isActive) break
                if (t.isDownloaded || services.downloads.downloaded.value.any { it.id == t.id } || services.downloads.progress.value.containsKey(t.id)) continue
                val ready = if (t.uri != null && t.uri.startsWith("https://")) {
                    t
                } else {
                    val resolved = resolveStream(t)
                    resolved?.url?.let { t.copy(uri = it, canDownload = true) }
                }
                if (!isActive) break
                if (ready != null && ready.uri != null) {
                    services.downloads.download(ready.copy(canDownload = true))
                    while (isActive && services.downloads.progress.value.containsKey(t.id)) {
                        delay(400)
                    }
                }
                delay(200)
            }
        }
    }
    LaunchedEffect(searchSource, debouncedQuery) { searchNotice = null }
    // Only the chosen provider ever sees the query, and only after the user picks it.
    LaunchedEffect(debouncedQuery, searchSource) {
        val chosen = when (searchSource) {
            ALL_SOURCES -> activeProviders.filter { it.canPlay }
            THIS_DEVICE -> emptyList()
            else -> activeProviders.filter { it.displayName == searchSource }
        }
        if (chosen.isEmpty() || debouncedQuery.isBlank()) {
            onlineResults = emptyList(); onlineLoading = false
        } else {
            onlineLoading = true
            val query = debouncedQuery.trim()
            // Ask every chosen source at once; one slow or failing source must not hold back the others.
            onlineResults = kotlinx.coroutines.coroutineScope {
                chosen.map { p -> async { p.search(query) } }.awaitAll()
            }.let { lists ->
                // Interleave so the first rows mix sources instead of showing one source's 25 results first.
                val out = ArrayList<Track>()
                var i = 0
                while (lists.any { i < it.size }) { lists.forEach { l -> if (i < l.size) out += l[i] }; i++ }
                out
            }
            onlineLoading = false
        }
    }
    val shownResults = run {
        val local = downloaded.associateBy { it.id }
        val online = onlineResults.map { local[it.id] ?: it }
        when (searchSource) {
            THIS_DEVICE -> results
            ALL_SOURCES -> results + online.filter { o -> results.none { it.id == o.id } }
            else -> online
        }
    }
    val allPlaylists = remember(tracks, savedPlaylists, youtubePlaylists) {
        listOfNotNull(
            Playlist(ALL_SONGS_ID, "All songs", "Everything in your library", tracks),
            if (downloaded.isEmpty()) null else Playlist(DOWNLOADS_ID, "Downloads", "Saved for offline listening", downloaded),
        ) + savedPlaylists + youtubePlaylists
    }
    val hasAudioPermission = remember(permissionTick, scanState) { services.platform.hasAudioPermission() }
    val spectrumFlow = services.equalizer.spectrum
    val spectrum = remember(spectrumFlow) { { spectrumFlow.value } }
    // The engine only computes the spectrum while someone is subscribed, so subscribe (without
    // recomposing: the drawing code reads `.value`) only while an equalizer view is on screen.
    if (overlay == Overlay.Equalizer || eqPanelOpen) {
        LaunchedEffect(spectrumFlow) { spectrumFlow.collect { } }
    }

    // Pages opened from Settings (Customization, Equalizer) go back to Settings, not out of it.
    var returnTo by remember { mutableStateOf<Overlay?>(null) }
    val closeOverlay: () -> Unit = { overlay = returnTo; returnTo = null }

    // Back: drawer first, then the player, then pages, then the side equalizer.
    // Onboarding cannot be dismissed with back — user must complete or skip.
    LocalBackHandler.current?.Handle(
        enabled = drawerState.isOpen || playerExpanded ||
                (overlay != null && overlay != Overlay.Onboarding) || eqPanelOpen,
    ) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            playerExpanded -> if (sheetPage == SheetPage.Queue) sheetPage = SheetPage.NowPlaying else { showLyrics = false; playerExpanded = false }
            overlay != null -> closeOverlay()
            else -> eqPanelOpen = false
        }
    }

    val content: @Composable (columns: Int) -> Unit = { columns ->
        val ov = overlay
        when {
            ov is Overlay.PlaylistOpen -> {
                val playlist = allPlaylists.firstOrNull { it.id == ov.id }
                if (playlist == null) overlay = null else PlaylistScreen(
                    playlist = playlist,
                    currentTrackId = player.current?.id,
                    onBack = { overlay = null },
                    onPlayAll = { sorted -> sorted.firstOrNull()?.let { playTrack(it, sorted) } },
                    onShuffle = { sorted ->
                        if (!player.shuffle) services.player.toggleShuffle()
                        sorted.randomOrNull()?.let { playTrack(it, sorted) }
                    },
                    onTrackClick = { track, sorted -> playTrack(track, sorted) },
                    notice = searchNotice,
                    onAddToPlaylist = { addTarget = it },
                    onPlayNext = { services.player.playNext(it) },
                    onRemoveDownload = { services.downloads.remove(it.id) },
                    downloadProgress = downloadProgress,
                    repeat = player.repeat,
                    shuffleOn = player.shuffle,
                    onRepeat = { services.player.toggleRepeat() },
                    onDownloadAll = { downloadPlaylist(playlist) },
                    onDownloadTrack = { downloadTrack(it) },
                )
            }
            ov == Overlay.Equalizer -> EqualizerScreen(
                state = eq,
                spectrum = spectrum,
                onBack = closeOverlay,
                onEnabledChange = { services.equalizer.setEnabled(it) },
                onPreampChange = { services.equalizer.setPreamp(it) },
                onAddBand = { f -> services.equalizer.addBand(f) },
                onBandChange = { services.equalizer.updateBand(it) },
                onRemoveBand = { services.equalizer.removeBand(it) },
                onPreset = { services.equalizer.usePreset(it) },
                onSavePreset = { services.equalizer.savePreset(it) },
                onDeletePreset = { services.equalizer.deletePreset(it) },
                onReset = { services.equalizer.reset() },
            )
            ov == Overlay.Settings -> SettingsScreen(
                library = librarySettings,
                scanState = scanState,
                trackCount = tracks.size,
                hasAudioPermission = hasAudioPermission,
                onBack = { overlay = null },
                onScanModeChange = { m -> scope.launch { services.library.setScanMode(m) } },
                onAddFolder = {
                    services.platform.pickFolder { uri, name -> scope.launch { services.library.addFolder(uri, name) } }
                },
                onRemoveFolder = { f -> scope.launch { services.library.removeFolder(f.id) } },
                onRescan = { scope.launch { services.library.rescan() } },
                onRequestPermission = {
                    services.platform.requestAudioPermission { permissionTick++; scope.launch { services.library.rescan() } }
                },
                onOpenAppSettings = { services.platform.openAppSettings() },
                onOpenEqualizer = { returnTo = Overlay.Settings; overlay = Overlay.Equalizer },
                onOpenCustomization = { returnTo = Overlay.Settings; overlay = Overlay.Customization },
                providers = services.providers.map { ProviderOption(it.id, it.displayName, it.description, it.isOn(providerChoices)) },
                onProviderToggle = { id, on -> services.providerSettings.setEnabled(id, on) },
                notificationPlayer = ProviderOption(
                    "notifications", "Notification player",
                    if (notificationsOn) "Shows the song and controls in the notification shade and on the lock screen"
                    else "Off: the song and controls won't show in the notification shade or lock screen. Turn on to fix it.",
                    notificationsOn,
                ),
                onEnableNotifications = { services.platform.requestNotifications() },
                qualityPreferences = qualityPreferences,
                onQualityChange = { id, pref -> services.qualitySettings.setPreference(id, pref) },
                spotifyConnected = spotifyConnected,
                spotifySyncState = spotifySyncState,
                spotifyStatusMessage = spotifyStatusMessage,
                onConnectSpotify = {
                    services.platform.openSpotifyLogin { spDc ->
                        if (!spDc.isNullOrBlank()) {
                            scope.launch {
                                services.spotifySync?.saveSession(spDc)
                            }
                        }
                    }
                },
                onSyncSpotify = {
                    scope.launch {
                        services.spotifySync?.syncAll(services.playlists)
                    }
                },
                onDisconnectSpotify = {
                    scope.launch {
                        services.spotifySync?.disconnect()
                    }
                },
                showSpotifyOnHome = musicPrefs.showSpotifyOnHome,
                onToggleShowSpotifyOnHome = { services.musicPreferences.setShowSpotifyOnHome(it) },
                onOpenOnboarding = { overlay = Overlay.Onboarding },
                mediaCacheSize = mediaCacheSize,
                onClearMediaCache = {
                    services.platform.clearMediaCache()
                    mediaCacheSize = 0L
                },
                onOpenAccount = { returnTo = Overlay.Settings; overlay = Overlay.Account },
            )
            ov == Overlay.Account -> AccountScreen(
                account = account,
                message = accountMessage,
                onBack = { overlay = null },
                onSignIn = {
                    scope.launch {
                        accountMessage = if (services.account.signIn()) null
                        else if (services.importer?.configured == false) "YouTube sign-in is not available in this build."
                        else "Sign-in did not finish. Check that Google Play services is up to date and try again."
                    }
                },
                onSignOut = { scope.launch { services.account.signOut() } },
                importer = services.importer,
                onImportPlaylist = { remote, report ->
                    scope.launch {
                        val items = runCatching { services.importer!!.items(remote.id) }.getOrNull()
                        if (items == null) report("Could not read \"${remote.title}\". Check your connection and try again.") else {
                            val matched = app.streamvibely.importing.matchTracks(items, tracks)
                            if (matched.isEmpty()) report("None of the ${items.size} songs in \"${remote.title}\" are on this device yet.")
                            else {
                                val created = services.playlists.create(remote.title, "Imported from YouTube")
                                services.playlists.addTracks(created.id, matched.map { it.id })
                                report("Imported ${matched.size} of ${items.size} songs into \"${remote.title}\". The rest are not on this device.")
                            }
                        }
                    }
                },
                spotifyConnected = spotifyConnected,
                spotifySyncState = spotifySyncState,
                spotifyStatusMessage = spotifyStatusMessage,
                onConnectSpotify = {
                    services.platform.openSpotifyLogin { spDc ->
                        if (!spDc.isNullOrBlank()) {
                            scope.launch {
                                services.spotifySync?.saveSession(spDc)
                            }
                        }
                    }
                },
                onSyncSpotify = {
                    scope.launch {
                        services.spotifySync?.syncAll(services.playlists)
                    }
                },
                onDisconnectSpotify = {
                    scope.launch {
                        services.spotifySync?.disconnect()
                    }
                },
            )
            ov == Overlay.Customization -> CustomizationScreen(
                spec = spec,
                onSpecChange = { t -> scope.launch { services.theme.update(t) } },
                onBack = closeOverlay,
                artSeed = artSeed,
            )
            ov == Overlay.Usage -> ListeningUsageScreen(
                summary = usage,
                onBack = { overlay = null },
                onClear = { scope.launch { services.usage.clear() } },
            )
            ov == Overlay.Onboarding -> OnboardingScreen(
                 spotifyConnected = spotifyConnected,
                 ytConnected = account != null,
                 onConnectSpotify = {
                     services.platform.openSpotifyLogin { spDc ->
                         if (!spDc.isNullOrBlank()) {
                             scope.launch { services.spotifySync?.saveSession(spDc) }
                         }
                     }
                 },
                 onConnectYouTube = {
                     scope.launch { services.account.signIn() }
                 },
                 onSyncSpotify = {
                     scope.launch { services.spotifySync?.syncAll(services.playlists) }
                 },
                 onComplete = {
                     services.musicPreferences.completeOnboarding()
                     overlay = null
                 },
             )
            tab == Tab.Home -> HomeContent(
                columns = columns,
                tracks = tracks,
                playlists = savedPlaylists,
                recentTracks = recentTracks,
                syncedSpotifyTracks = syncedTracks,
                showSpotifyOnHome = musicPrefs.showSpotifyOnHome,
                scanState = scanState,
                account = account,
                drawerProgress = { drawerState.progress },
                onAccountClick = { scope.launch { drawerState.open() } },
                onPlay = { playTrack(it, tracks) },
                onOpenPlaylist = { overlay = Overlay.PlaylistOpen(it.id) },
                onOpenSettings = { overlay = Overlay.Settings },
            )
            tab == Tab.Search -> SearchScreen(
                query = query,
                onQueryChange = { query = it },
                selectedSource = searchSource,
                sources = listOf(ALL_SOURCES, THIS_DEVICE) + activeProviders.map { it.displayName },
                onSourceSelected = { pickedSource = it },
                results = shownResults,
                loading = onlineLoading,
                notice = searchNotice ?: services.providers.firstOrNull { it.displayName == searchSource }?.let { p ->
                    if (p.requiresAccount && account == null) "Sign in with YouTube in Account to search ${p.displayName}." else null
                },
                downloadProgress = downloadProgress,
                onDownload = { downloadTrack(it) },
                onTrackClick = { t -> playTrack(t, shownResults) },
                onCategoryClick = { query = it },
            )
            else -> LibraryScreen(
                playlists = allPlaylists,
                selectedTab = libraryTab,
                onTabSelected = { libraryTab = it },
                onPlaylistClick = { overlay = Overlay.PlaylistOpen(it.id) },
                onCreatePlaylist = { createFor = null; showCreate = true },
            )
        }
    }

    AccountDrawerHost(
        state = drawerState,
        account = account,
        onOpenAccount = { accountMessage = null; overlay = Overlay.Account },
        onOpenUsage = { overlay = Overlay.Usage },
        onOpenSettings = { overlay = Overlay.Settings },
        gesturesEnabled = overlay == null && !playerExpanded,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val onTab: (Tab) -> Unit = { tab = it; overlay = null; returnTo = null }
            // Layout follows the window's shape, not the device type: portrait always gets the phone-style
            // bottom navigation (more columns when wide), landscape gets a rail, and wide landscape gets three panes.
            val windowWidth = maxWidth
            val windowHeight = maxHeight
            val layout = when {
                maxWidth <= maxHeight || maxWidth < 600.dp -> ShellLayout.Compact
                maxWidth >= 900.dp -> ShellLayout.Expanded
                else -> ShellLayout.Rail
            }
            val sheetContent: @Composable () -> Unit = {
                when (sheetPage) {
                    SheetPage.NowPlaying -> NowPlayingScreen(
                        state = player,
                        onBack = { showLyrics = false; playerExpanded = false },
                        onPlayPause = { services.player.togglePlayPause() },
                        onNext = { services.player.next() },
                        onPrevious = { services.player.previous() },
                        onSeek = { services.player.seekTo(it) },
                        onShuffle = { services.player.toggleShuffle() },
                        onRepeat = { services.player.toggleRepeat() },
                        onOpenQueue = { sheetPage = SheetPage.Queue },
                        onOpenEqualizer = { showLyrics = false; playerExpanded = false; overlay = Overlay.Equalizer },
                        source = nowPlayingSource ?: player.current?.let {
                            if (it.source == "local") "On this device"
                            else services.providers.firstOrNull { p -> p.id == it.source }?.displayName ?: it.source
                        } ?: "Unknown",
                        quality = nowPlayingQuality ?: player.current?.qualityLabel
                            ?: if (player.current?.source == "local") "Local file" else "Unknown",
                        lyrics = lyrics,
                        showLyrics = showLyrics,
                        onToggleLyrics = { showLyrics = !showLyrics },
                        upgradeOfferText = upgradeOffer?.takeIf { it.trackId == player.current?.id }?.label?.let { "Higher quality available: $it" },
                        onUpgradeQuality = {
                            val offer = upgradeOffer
                            val cur = player.current
                            if (offer != null && cur != null && cur.id == offer.trackId) {
                                val pos = player.positionMs
                                val ready = cur.copy(uri = offer.newUrl, qualityLabel = offer.label)
                                services.player.play(ready, player.queue.map { if (it.id == ready.id) ready else it })
                                services.player.seekTo(pos)
                                labels[offer.trackId] = offer.label
                                nowPlayingQuality = offer.label
                                nowPlayingSource = "Spotify (via ${offer.provider})"
                            }
                            upgradeOffer = null
                        },
                        onDismissUpgrade = { upgradeOffer = null },
                        onDownload = { player.current?.let { downloadTrack(it) } },
                        isDownloaded = player.current?.let { c -> c.isDownloaded || downloaded.any { it.id == c.id } } ?: false,
                    )
                    SheetPage.Queue -> QueueScreen(
                        state = player,
                        onTrackClick = { services.player.play(it, player.queue) },
                        onBack = { sheetPage = SheetPage.NowPlaying },
                    )
                }
            }
            when (layout) {
                ShellLayout.Expanded, ShellLayout.Rail -> {
                    val expanded = layout == ShellLayout.Expanded
                    Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            Row(Modifier.weight(1f)) {
                                if (expanded) {
                                    Sidebar(
                                        selected = tab,
                                        account = account,
                                        drawerProgress = { drawerState.progress },
                                        onAccountClick = { scope.launch { drawerState.open() } },
                                        onTab = onTab,
                                        onEqualizer = { eqPanelOpen = !eqPanelOpen },
                                        onSettings = { overlay = Overlay.Settings },
                                        modifier = Modifier.width(220.dp).fillMaxHeight(),
                                    )
                                } else {
                                    NavRail(
                                        selected = tab,
                                        account = account,
                                        drawerProgress = { drawerState.progress },
                                        onAccountClick = { scope.launch { drawerState.open() } },
                                        onTab = onTab,
                                        onEqualizer = { overlay = Overlay.Equalizer },
                                        onSettings = { overlay = Overlay.Settings },
                                        modifier = Modifier.width(88.dp).fillMaxHeight(),
                                    )
                                }
                                Box(Modifier.weight(1f)) { content(if (windowWidth >= 1300.dp) 4 else 3) }
                                if (expanded) {
                                    RightPanel(
                                        player = player,
                                        eqOpen = eqPanelOpen,
                                        eq = eq,
                                        spectrum = spectrum,
                                        lyrics = lyrics,
                                        showLyrics = showLyrics,
                                        onSeek = { services.player.seekTo(it) },
                                        onEnabledChange = { services.equalizer.setEnabled(it) },
                                        onPreampChange = { services.equalizer.setPreamp(it) },
                                        onAddBand = { f -> services.equalizer.addBand(f) },
                                        onBandChange = { services.equalizer.updateBand(it) },
                                        onRemoveBand = { services.equalizer.removeBand(it) },
                                        onPreset = { services.equalizer.usePreset(it) },
                                        onSavePreset = { services.equalizer.savePreset(it) },
                                        onDeletePreset = { services.equalizer.deletePreset(it) },
                                        onReset = { services.equalizer.reset() },
                                        onCloseEq = { eqPanelOpen = false },
                                        modifier = Modifier.width(if (windowWidth >= 1300.dp) 360.dp else 300.dp).fillMaxHeight().padding(8.dp),
                                    )
                                }
                            }
                            PlaybackBar(
                                state = player,
                                onToggle = { services.player.togglePlayPause() },
                                onNext = { services.player.next() },
                                onPrevious = { services.player.previous() },
                                onShuffle = { services.player.toggleShuffle() },
                                onRepeat = { services.player.toggleRepeat() },
                                onSeek = { services.player.seekTo(it) },
                                onEqualizer = { if (expanded) eqPanelOpen = !eqPanelOpen else overlay = Overlay.Equalizer },
                                onLyrics = if (expanded) ({ showLyrics = !showLyrics; eqPanelOpen = false }) else null,
                                lyricsOn = showLyrics,
                                onOpenPlayer = if (expanded) null else ({ playerExpanded = true }),
                                dense = windowHeight < 520.dp,
                            )
                        }
                        // Without a right panel, the full player opens over the page, like the phone sheet.
                        if (!expanded && playerExpanded && player.current != null) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { sheetContent() }
                        }
                    }
                }
                ShellLayout.Compact -> Box(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize()) {
                        // Leave room for the floating mini player so it never covers page content.
                        Box(Modifier.weight(1f).padding(bottom = if (player.current != null) MiniHeight else 0.dp)) {
                            content(if (windowWidth >= 840.dp) 4 else if (windowWidth >= 600.dp) 3 else 2)
                        }
                        BottomNav(tab, onTab)
                    }
                    if (player.current != null) {
                        PlayerSheet(
                            expanded = playerExpanded,
                            onExpandedChange = {
                                playerExpanded = it
                                if (!it) { sheetPage = SheetPage.NowPlaying; showLyrics = false }
                            },
                            miniHeight = MiniHeight,
                            miniBottomInset = NavHeight,
                            onDismiss = { services.player.stop() },
                            mini = {
                                MiniPlayer(
                                    state = player,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    onToggle = { services.player.togglePlayPause() },
                                )
                            },
                            full = sheetContent,
                        )
                    }
                }
            }
        }
    }

    addTarget?.let { target ->
        AddToPlaylistDialog(
            playlists = savedPlaylists,
            onPick = { p ->
                scope.launch { services.playlists.addTracks(p.id, listOf(target.id)) }
                addTarget = null
            },
            onCreateNew = { createFor = target; addTarget = null; showCreate = true },
            onDismiss = { addTarget = null },
        )
    }
    if (showCreate) {
        CreatePlaylistDialog(
            onCreate = { name ->
                val pending = createFor
                scope.launch {
                    val p = services.playlists.create(name)
                    if (pending != null) services.playlists.addTracks(p.id, listOf(pending.id))
                }
                showCreate = false
                createFor = null
            },
            onDismiss = { showCreate = false; createFor = null },
        )
    }
}

@Composable
private fun HomeContent(
    columns: Int,
    tracks: List<Track>,
    playlists: List<Playlist>,
    recentTracks: List<Track>,
    syncedSpotifyTracks: List<Track>,
    showSpotifyOnHome: Boolean,
    scanState: ScanState,
    account: Account?,
    drawerProgress: () -> Float,
    onAccountClick: () -> Unit,
    onPlay: (Track) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
    onOpenSettings: () -> Unit,
) {
    // "Back in rotation" uses tracks the user actually played!
    // If no tracks have been played yet, fall back to top synced Spotify tracks or local tracks.
    val rotationTracks = when {
        recentTracks.isNotEmpty() -> recentTracks
        syncedSpotifyTracks.isNotEmpty() -> syncedSpotifyTracks
        else -> tracks
    }
    val rotationTitle = when {
        recentTracks.isNotEmpty() -> "Back in rotation"
        syncedSpotifyTracks.isNotEmpty() -> "Picked for you"
        else -> "Quick picks"
    }
    val remainingTracks = remember(tracks, rotationTracks) {
        val rotationIds = rotationTracks.mapTo(HashSet()) { it.id }
        tracks.filter { it.id !in rotationIds }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "header") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                AccountButton(account = account, onClick = onAccountClick, pull = drawerProgress)
                Text("Streamvibely", style = MaterialTheme.typography.headlineMedium)
            }
        }
        if (tracks.isEmpty() && playlists.isEmpty()) {
            item(key = "empty") { EmptyLibrary(scanState, onOpenSettings) }
            return@LazyColumn
        }

        // 1. Back in rotation (Recent plays or top picks)
        if (rotationTracks.isNotEmpty()) {
            item(key = "t1") { Text(rotationTitle, style = MaterialTheme.typography.headlineLarge) }
            items(rotationTracks.take(6).chunked(columns), key = { row -> "q" + row.first().id }) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { QuickPick(it, Modifier.weight(1f)) { onPlay(it) } }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }

        // 2. Recommendations from Spotify (when synced and toggle is on)
        if (showSpotifyOnHome && syncedSpotifyTracks.isNotEmpty()) {
            item(key = "t_spotify") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("From Your Spotify", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        "${syncedSpotifyTracks.size} songs",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "spotify_tracks") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(syncedSpotifyTracks.take(20), key = { "sp_${it.id}" }) { t ->
                        TrackCard(t) { onPlay(t) }
                    }
                }
            }
        }

        // 3. Playlists (including Spotify Liked Songs, Spotify playlists, and custom playlists)
        if (playlists.isNotEmpty()) {
            item(key = "t2") { Text("Your playlists", style = MaterialTheme.typography.headlineLarge) }
            item(key = "playlists") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(playlists.take(12), key = { it.id }) { p -> PlaylistCard(p) { onOpenPlaylist(p) } }
                }
            }
        }

        // 4. More tracks
        if (remainingTracks.isNotEmpty()) {
            item(key = "t3") { Text("More tracks", style = MaterialTheme.typography.headlineLarge) }
            item(key = "recent") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(remainingTracks.take(15), key = { it.id }) { t -> TrackCard(t) { onPlay(t) } }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary(scanState: ScanState, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusCard.dp.scaled())).clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (scanState is ScanState.Scanning) "Looking for your music..." else "Add your music",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            when (scanState) {
                is ScanState.NeedsPermission -> "Allow access to your audio files, or choose folders, in Settings."
                is ScanState.Scanning -> "Found ${scanState.found} songs so far."
                else -> "No songs found yet. Open Settings to allow access or pick the folders to scan."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            Modifier.clip(RoundedCornerShape(Tokens.radiusChipLarge.dp.scaled())).background(AppTheme.colors.accent)
                .clickable(onClick = onOpenSettings).padding(horizontal = 18.dp, vertical = 10.dp),
        ) { Text("Open Settings", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
private fun QuickPick(t: Track, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.appSurface(SurfaceKind.Card, RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clip(RoundedCornerShape(Tokens.radiusSm.dp.scaled())).clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(t, Modifier.size(52.dp), sizePx = 150)
        Column(Modifier.padding(horizontal = 10.dp).weight(1f)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}

@Composable
private fun PlaylistCard(p: Playlist, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        ArtworkImage(p.tracks.firstOrNull(), Modifier.size(150.dp).clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())), sizePx = 300)
        Text(p.name, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text("${p.tracks.size} songs", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TrackCard(t: Track, onClick: () -> Unit) {
    Column(Modifier.width(150.dp).clickable(onClick = onClick)) {
        ArtworkImage(t, Modifier.size(150.dp).clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())), sizePx = 300)
        Text(t.title, Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

/** The tap-to-expand and swipe-up behaviour comes from [PlayerSheet]; only the play button is clickable here. */
@Composable
private fun MiniPlayer(state: PlayerUiState, modifier: Modifier, onToggle: () -> Unit) {
    val t = state.current ?: return
    CompositionLocalProvider(LocalContentColor provides AppTheme.colors.onMiniPlayer) {
    Row(
        modifier.fillMaxWidth()
            .appSurface(SurfaceKind.MiniPlayer, RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled()))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(t, Modifier.size(40.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())), sizePx = 150)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(t.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(t.artist, color = AppTheme.colors.onMiniPlayer.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
        PlayButton(state.isPlaying, 40.dp, onToggle)
    }
    }
}

@Composable
private fun PlayButton(playing: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier.size(size).clip(CircleShape).background(AppTheme.colors.accent).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(
            icon = if (playing) AppIcons.Pause else AppIcons.PlayArrow,
            contentDescription = if (playing) "Pause" else "Play",
            tint = AppTheme.colors.onAccent,
            size = size * 0.6f,
        )
    }
}

@Composable
private fun BottomNav(selected: Tab, onTab: (Tab) -> Unit) {
    Row(Modifier.fillMaxWidth().height(NavHeight).appSurface(SurfaceKind.Navigation, RectangleShape), horizontalArrangement = Arrangement.SpaceEvenly) {
        Tab.entries.forEach { t ->
            val tint = if (t == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable { onTab(t) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                AppIcon(if (t == selected) t.iconSelected else t.icon, contentDescription = null, tint = tint, size = 26.dp)
                Text(t.label, color = tint, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun Sidebar(
    selected: Tab,
    account: Account?,
    drawerProgress: () -> Float,
    onAccountClick: () -> Unit,
    onTab: (Tab) -> Unit,
    onEqualizer: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.appSurface(SurfaceKind.Navigation, RectangleShape).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccountButton(account = account, onClick = onAccountClick, pull = drawerProgress)
            Text("streamvibely", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(8.dp))
        Tab.entries.forEach { t ->
            SidebarItem(if (t == selected) t.iconSelected else t.icon, t.label, t == selected) { onTab(t) }
        }
        SidebarItem(AppIcons.Equalizer, "Equalizer", false, onEqualizer)
        SidebarItem(AppIcons.Settings, "Settings", false, onSettings)
    }
}

@Composable
private fun SidebarItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(Modifier.clickable(onClick = onClick).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppIcon(icon, contentDescription = null, tint = tint, size = 22.dp)
        Text(label, color = tint, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun RightPanel(
    player: PlayerUiState,
    eqOpen: Boolean,
    eq: EqualizerState,
    spectrum: () -> FloatArray,
    lyrics: Lyrics?,
    showLyrics: Boolean,
    onSeek: (Long) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPreampChange: (Float) -> Unit,
    onAddBand: (Float) -> Unit,
    onBandChange: (app.streamvibely.domain.EqBand) -> Unit,
    onRemoveBand: (Int) -> Unit,
    onPreset: (String) -> Unit,
    onSavePreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    onReset: () -> Unit,
    onCloseEq: () -> Unit,
    modifier: Modifier,
) {
    Column(modifier.appSurface(SurfaceKind.Panel, RoundedCornerShape(Tokens.radiusPanel.dp.scaled())).clip(RoundedCornerShape(Tokens.radiusPanel.dp.scaled())).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (eqOpen) {
            EqualizerPanel(
                state = eq,
                spectrum = spectrum,
                onClose = onCloseEq,
                onEnabledChange = onEnabledChange,
                onPreampChange = onPreampChange,
                onAddBand = onAddBand,
                onBandChange = onBandChange,
                onRemoveBand = onRemoveBand,
                onPreset = onPreset,
                onSavePreset = onSavePreset,
                onDeletePreset = onDeletePreset,
                onReset = onReset,
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }
        val t = player.current
        if (showLyrics && t != null) {
            Text(t.title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(t.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            app.streamvibely.lyrics.LyricsView(lyrics, player.positionMs, onSeek, Modifier.weight(1f).padding(top = 8.dp))
            return@Column
        }
        Text("Now playing", style = MaterialTheme.typography.titleLarge)
        ArtworkImage(t, Modifier.fillMaxWidth().aspectRatio(1f, matchHeightConstraintsFirst = false).heightIn(max = 320.dp).clip(RoundedCornerShape(Tokens.radiusCard.dp.scaled())), sizePx = 600)
        Text(t?.title ?: "Nothing playing", style = MaterialTheme.typography.headlineSmall, maxLines = 1)
        Text(t?.artist ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        val upNext = remember(player.queue, t?.id) {
            val i = player.queue.indexOfFirst { it.id == t?.id }
            if (i < 0) player.queue.take(4) else player.queue.drop(i + 1).take(4)
        }
        if (upNext.isNotEmpty()) {
            Text("Next in queue", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
            upNext.forEach { n ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ArtworkImage(n, Modifier.size(36.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())), sizePx = 150)
                    Column(Modifier.padding(start = 10.dp)) {
                        Text(n.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                        Text(n.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackBar(
    state: PlayerUiState,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onSeek: (Long) -> Unit,
    onEqualizer: () -> Unit,
    onLyrics: (() -> Unit)?,
    lyricsOn: Boolean,
    onOpenPlayer: (() -> Unit)?,
    dense: Boolean = false,
) {
    val t = state.current
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier.fillMaxWidth().appSurface(SurfaceKind.Navigation, RectangleShape).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left: what is playing. Tapping it opens the full player when there is no side panel.
        Row(
            Modifier.weight(1f).then(if (onOpenPlayer != null && t != null) Modifier.clickable(onClick = onOpenPlayer) else Modifier),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (t != null) ArtworkImage(t, Modifier.size(48.dp).clip(RoundedCornerShape(Tokens.radiusArtwork.dp.scaled())), sizePx = 150)
            Column(Modifier.padding(start = if (t != null) 12.dp else 0.dp)) {
                Text(t?.title ?: "Nothing playing", style = MaterialTheme.typography.titleSmall, maxLines = 1)
                if (t != null) Text(t.artist, color = muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        // Centre: transport and timeline.
        Column(Modifier.weight(1.6f).widthIn(max = 560.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.Shuffle, "Shuffle", onShuffle, active = state.shuffle, size = if (dense) 36.dp else 44.dp)
                app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.SkipPrevious, "Previous", onPrevious, size = if (dense) 36.dp else 44.dp)
                PlayButton(state.isPlaying, if (dense) 36.dp else 44.dp, onToggle)
                app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.SkipNext, "Next", onNext, size = if (dense) 36.dp else 44.dp)
                app.streamvibely.ui.nowplaying.AppIconButton(if (state.repeat == app.streamvibely.domain.RepeatMode.One) AppIcons.RepeatOne else AppIcons.Repeat, "Repeat", onRepeat, active = state.repeat != app.streamvibely.domain.RepeatMode.Off, size = if (dense) 36.dp else 44.dp)
            }
            val duration = t?.durationMs ?: 0L
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(app.streamvibely.ui.nowplaying.formatTime(state.positionMs), color = muted, style = MaterialTheme.typography.labelSmall)
                app.streamvibely.ui.nowplaying.SeekBar(state.positionMs, duration, onSeek, Modifier.weight(1f).padding(horizontal = 8.dp))
                Text(app.streamvibely.ui.nowplaying.formatTime(duration), color = muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        // Right: extra tools.
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (onLyrics != null) {
                app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.Lyrics, "Lyrics", onLyrics, active = lyricsOn)
            }
            app.streamvibely.ui.nowplaying.AppIconButton(AppIcons.Equalizer, "Equalizer", onEqualizer)
        }
    }
}

/** Narrow icon-and-label navigation for landscape windows too small for the full sidebar. */
@Composable
private fun NavRail(
    selected: Tab,
    account: Account?,
    drawerProgress: () -> Float,
    onAccountClick: () -> Unit,
    onTab: (Tab) -> Unit,
    onEqualizer: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.appSurface(SurfaceKind.Navigation, RectangleShape).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AccountButton(account = account, onClick = onAccountClick, pull = drawerProgress)
        Tab.entries.forEach { t -> RailItem(if (t == selected) t.iconSelected else t.icon, t.label, t == selected) { onTab(t) } }
        RailItem(AppIcons.Equalizer, "Equalizer", false, onEqualizer)
        RailItem(AppIcons.Settings, "Settings", false, onSettings)
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(icon, contentDescription = null, tint = tint, size = 24.dp)
        Text(label, color = tint, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}
