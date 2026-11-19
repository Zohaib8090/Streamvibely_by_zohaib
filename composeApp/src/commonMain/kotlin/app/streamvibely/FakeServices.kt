package app.streamvibely

import app.streamvibely.domain.Account
import app.streamvibely.domain.AccountRepository
import app.streamvibely.domain.DayUsage
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import app.streamvibely.domain.EqualizerController
import app.streamvibely.domain.EqualizerState
import app.streamvibely.domain.ListeningUsageRepository
import app.streamvibely.domain.SPECTRUM_BINS
import app.streamvibely.domain.TrackUsage
import app.streamvibely.domain.UsageSummary
import app.streamvibely.domain.LibraryRepository
import app.streamvibely.domain.LibrarySettings
import app.streamvibely.domain.MusicFolder
import app.streamvibely.domain.PlatformActions
import app.streamvibely.domain.PlayerController
import app.streamvibely.domain.PlayerUiState
import app.streamvibely.domain.Playlist
import app.streamvibely.domain.PlaylistRepository
import app.streamvibely.domain.SampleData
import app.streamvibely.domain.ScanMode
import app.streamvibely.domain.ScanState
import app.streamvibely.domain.Track
import app.streamvibely.lyrics.LrclibProvider
import app.streamvibely.lyrics.LyricsRepository
import app.streamvibely.theme.InMemoryThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory stand-ins used on web and desktop until their real implementations exist. They make
 * the shared UI fully clickable but play no audio and read no files.
 */
fun fakeServices(): AppServices {
    val library = FakeLibrary()
    return AppServices(
        player = FakePlayer(),
        library = library,
        playlists = FakePlaylists(),
        equalizer = FakeEqualizer(),
        lyrics = LyricsRepository { listOf(LrclibProvider()) },
        platform = FakePlatform,
        account = StubAccountRepository(),
        usage = FakeUsage(),
        theme = InMemoryThemeRepository(),
    )
}

private class FakePlayer : PlayerController {
    private val _state = MutableStateFlow(
        PlayerUiState(current = SampleData.tracks.first(), isPlaying = false, queue = SampleData.tracks),
    )
    override val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    override fun play(track: Track, queue: List<Track>) =
        _state.update { it.copy(current = track, queue = queue.ifEmpty { listOf(track) }, isPlaying = true, positionMs = 0) }

    override fun togglePlayPause() = _state.update { it.copy(isPlaying = !it.isPlaying) }
    override fun stop() = _state.update { it.copy(current = null, isPlaying = false, queue = emptyList(), positionMs = 0) }
    override fun next() = step(1)
    override fun previous() = step(-1)
    override fun seekTo(positionMs: Long) = _state.update { it.copy(positionMs = positionMs) }
    override fun toggleShuffle() = _state.update { it.copy(shuffle = !it.shuffle) }
    override fun toggleRepeat() = _state.update {
        it.copy(repeat = when (it.repeat) { app.streamvibely.domain.RepeatMode.Off -> app.streamvibely.domain.RepeatMode.All; app.streamvibely.domain.RepeatMode.All -> app.streamvibely.domain.RepeatMode.One; else -> app.streamvibely.domain.RepeatMode.Off })
    }

    private fun step(delta: Int) = _state.update { s ->
        val i = s.queue.indexOfFirst { it.id == s.current?.id }
        if (i < 0 || s.queue.isEmpty()) s else s.copy(current = s.queue[(i + delta + s.queue.size) % s.queue.size], positionMs = 0)
    }
}

private class FakeLibrary : LibraryRepository {
    override val tracks: StateFlow<List<Track>> = MutableStateFlow(SampleData.tracks)
    private val _scan = MutableStateFlow<ScanState>(ScanState.Done(SampleData.tracks.size))
    override val scanState: StateFlow<ScanState> = _scan
    private val _settings = MutableStateFlow(LibrarySettings())
    override val settings: StateFlow<LibrarySettings> = _settings

    override suspend fun rescan() { _scan.value = ScanState.Done(SampleData.tracks.size) }
    override suspend fun setScanMode(mode: ScanMode) = _settings.update { it.copy(scanMode = mode) }
    override suspend fun addFolder(uri: String, displayName: String) =
        _settings.update { it.copy(folders = it.folders + MusicFolder(uri, displayName, uri)) }
    override suspend fun removeFolder(id: String) = _settings.update { it.copy(folders = it.folders.filterNot { f -> f.id == id }) }
    override suspend fun setMinDuration(seconds: Int) = _settings.update { it.copy(minDurationSeconds = seconds) }
}

private class FakePlaylists : PlaylistRepository {
    private val _playlists = MutableStateFlow(SampleData.playlists)
    override val playlists: StateFlow<List<Playlist>> = _playlists
    private var counter = 0

    override suspend fun create(name: String, description: String?): Playlist {
        val p = Playlist("new${counter++}", name, description)
        _playlists.update { it + p }
        return p
    }
    override suspend fun rename(id: String, name: String) = _playlists.update { l -> l.map { if (it.id == id) it.copy(name = name) else it } }
    override suspend fun delete(id: String) = _playlists.update { l -> l.filterNot { it.id == id } }
    override suspend fun addTracks(id: String, trackIds: List<String>) = _playlists.update { l ->
        l.map { p ->
            if (p.id != id) p else p.copy(tracks = p.tracks + SampleData.tracks.filter { it.id in trackIds && p.tracks.none { t -> t.id == it.id } })
        }
    }
    override suspend fun removeTrack(id: String, trackId: String) = _playlists.update { l ->
        l.map { if (it.id == id) it.copy(tracks = it.tracks.filterNot { t -> t.id == trackId }) else it }
    }
}

private class FakeEqualizer : EqualizerController {
    private val freqs = listOf(31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
    private var nextId = freqs.size
    private val _state = MutableStateFlow(
        EqualizerState(
            available = true,
            enabled = true,
            bands = freqs.mapIndexed { i, f -> EqBand(i, f, 0f, 1.41f) },
            presets = listOf("Flat", "Bass boost", "Vocal"),
        ),
    )
    override val state: StateFlow<EqualizerState> = _state
    override val spectrum: StateFlow<FloatArray> = MutableStateFlow(FloatArray(SPECTRUM_BINS))

    override fun setEnabled(enabled: Boolean) = _state.update { it.copy(enabled = enabled) }
    override fun setPreamp(db: Float) = _state.update { it.copy(preampDb = db) }
    override fun addBand(frequencyHz: Float, gainDb: Float, q: Float, type: EqBandType): Int {
        if (_state.value.bands.size >= _state.value.maxBands) return -1
        val id = nextId++
        _state.update { it.copy(bands = it.bands + EqBand(id, frequencyHz, gainDb, q, type), selectedPreset = null) }
        return id
    }
    override fun updateBand(band: EqBand) = _state.update { s ->
        s.copy(bands = s.bands.map { if (it.id == band.id) band else it }, selectedPreset = null)
    }
    override fun removeBand(id: Int) = _state.update { s -> s.copy(bands = s.bands.filterNot { it.id == id }) }
    override fun usePreset(name: String) = _state.update { it.copy(selectedPreset = name) }
    override fun savePreset(name: String) = _state.update { if (name in it.presets) it else it.copy(presets = it.presets + name, selectedPreset = name) }
    override fun deletePreset(name: String) = _state.update { it.copy(presets = it.presets - name, selectedPreset = it.selectedPreset.takeIf { p -> p != name }) }
    override fun reset() = _state.update { s ->
        s.copy(preampDb = 0f, bands = freqs.mapIndexed { i, f -> EqBand(i, f, 0f, 1.41f) }, selectedPreset = null)
    }
}

/** Sign-in is not available yet: nobody is ever signed in, and [signIn] reports that. */
class StubAccountRepository : AccountRepository {
    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account
    override suspend fun signIn(): Boolean = false
    override suspend fun signOut() { _account.value = null }
}

private class FakeUsage : ListeningUsageRepository {
    private val today = 20_774L
    private val min = 60_000L
    private val days = (29 downTo 0).map { back ->
        val d = today - back
        DayUsage(d, offlineMs = ((d * 37) % 90 + 20) * min, onlineMs = 0)
    }
    override val summary: StateFlow<UsageSummary> = MutableStateFlow(
        UsageSummary(
            days = days,
            totalOfflineMs = days.sumOf { it.offlineMs },
            topTracks = SampleData.tracks.take(5).mapIndexed { i, t -> TrackUsage(t.title, t.artist, (300 - i * 40) * min, 20 - i * 3) },
            todayEpochDay = today,
        ),
    )
    override suspend fun clear() {}
}

private object FakePlatform : PlatformActions {
    override fun hasAudioPermission() = true
    override fun requestAudioPermission(onResult: (Boolean) -> Unit) = onResult(true)
    override fun pickFolder(onPicked: (uri: String, displayName: String) -> Unit) {}
    override fun openAppSettings() {}
}
