package app.streamvibely.domain

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.StateFlow

/** Contracts between the shared UI and each platform's real implementation. */

// ---- Local library -------------------------------------------------------------------------

/** A folder the user chose. [uri] is platform specific (Android: a persisted SAF tree URI). */
data class MusicFolder(val id: String, val displayName: String, val uri: String)

enum class ScanMode {
    /** Every audio file the system's media index knows about. */
    AllDeviceAudio,

    /** Only the folders in [LibrarySettings.folders]. */
    SelectedFolders,
}

data class LibrarySettings(
    val scanMode: ScanMode = ScanMode.AllDeviceAudio,
    val folders: List<MusicFolder> = emptyList(),
    /** Files shorter than this are ignored (ringtones, notification sounds). */
    val minDurationSeconds: Int = 30,
)

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val found: Int) : ScanState
    data class Done(val count: Int) : ScanState
    data object NeedsPermission : ScanState
    data class Error(val message: String) : ScanState
}

interface LibraryRepository {
    val tracks: StateFlow<List<Track>>
    val scanState: StateFlow<ScanState>
    val settings: StateFlow<LibrarySettings>

    /** Re-reads the device according to [settings]. */
    suspend fun rescan()
    suspend fun setScanMode(mode: ScanMode)
    suspend fun addFolder(uri: String, displayName: String)
    suspend fun removeFolder(id: String)
    suspend fun setMinDuration(seconds: Int)
}

/** Saves online tracks for offline playback. Only tracks with [Track.canDownload] are accepted. */
interface DownloadRepository {
    /** Finished downloads as local-file tracks (same id as the online track). */
    val downloaded: StateFlow<List<Track>>
    /** Track id to 0..1 for downloads in progress. */
    val progress: StateFlow<Map<String, Float>>
    fun download(track: Track)
    fun remove(trackId: String)
}

object NoDownloads : DownloadRepository {
    override val downloaded: StateFlow<List<Track>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList())
    override val progress: StateFlow<Map<String, Float>> = kotlinx.coroutines.flow.MutableStateFlow(emptyMap())
    override fun download(track: Track) {}
    override fun remove(trackId: String) {}
}

interface PlaylistRepository {
    val playlists: StateFlow<List<Playlist>>
    suspend fun create(name: String, description: String? = null): Playlist
    suspend fun rename(id: String, name: String)
    suspend fun delete(id: String)
    suspend fun addTracks(id: String, trackIds: List<String>)
    suspend fun removeTrack(id: String, trackId: String)
}

// ---- Equalizer (parametric, any number of bands) ------------------------------------------

enum class EqBandType { Peaking, LowShelf, HighShelf }

/**
 * One filter of the equalizer. Frequencies are in Hz, gain in dB, [q] is the quality factor
 * (higher = narrower; roughly 0.3 very wide to 10 very narrow). Shelves ignore [q].
 */
data class EqBand(
    val id: Int,
    val frequencyHz: Float,
    val gainDb: Float = 0f,
    val q: Float = 1.0f,
    val type: EqBandType = EqBandType.Peaking,
    val enabled: Boolean = true,
)

data class EqualizerState(
    val available: Boolean = false,
    val enabled: Boolean = false,
    /** Overall gain applied before the bands, to avoid clipping when boosting. */
    val preampDb: Float = 0f,
    val bands: List<EqBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val selectedPreset: String? = null,
    val minGainDb: Float = -15f,
    val maxGainDb: Float = 15f,
    val minFrequencyHz: Float = 20f,
    val maxFrequencyHz: Float = 20_000f,
    val minQ: Float = 0.2f,
    val maxQ: Float = 12f,
    val maxBands: Int = 32,
)

interface EqualizerController {
    val state: StateFlow<EqualizerState>

    /**
     * Live spectrum of what is playing: [SPECTRUM_BINS] log-spaced magnitudes from 20 Hz to 20 kHz,
     * each 0..1. All zeros when nothing plays. Updated about 30 times per second while playing, so
     * read it in the draw phase rather than recomposing on every update.
     */
    val spectrum: StateFlow<FloatArray>

    fun setEnabled(enabled: Boolean)
    fun setPreamp(db: Float)
    /** Adds a band and returns its id, or -1 when [EqualizerState.maxBands] is reached. */
    fun addBand(frequencyHz: Float, gainDb: Float = 0f, q: Float = 1.0f, type: EqBandType = EqBandType.Peaking): Int
    fun updateBand(band: EqBand)
    fun removeBand(id: Int)
    fun usePreset(name: String)
    fun savePreset(name: String)
    fun deletePreset(name: String)
    /** Back to a flat 10-band layout with no gain. */
    fun reset()
}

const val SPECTRUM_BINS = 64

// ---- Account and listening usage ---------------------------------------------------------

data class Account(
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val provider: String = "YouTube",
)

interface AccountRepository {
    /** Null when nobody is signed in. */
    val account: StateFlow<Account?>
    /** Starts sign-in. Returns false when sign-in is not available yet. */
    suspend fun signIn(): Boolean
    suspend fun signOut()
}

/** A playlist that lives in an online account, listed so the user can import it. */
data class RemotePlaylist(val id: String, val title: String, val itemCount: Int)

/** One entry of a remote playlist. Metadata only: no audio is ever fetched from the account's service. */
data class RemoteItem(val title: String, val artist: String, val videoId: String = "", val thumbnailUrl: String? = null)

/** Reads playlists from a signed-in account (YouTube Data API, read-only). */
interface PlaylistImporter {
    /** False until the app has been given the keys it needs; the UI then explains what is missing. */
    val configured: Boolean
    suspend fun playlists(): List<RemotePlaylist>
    suspend fun items(playlistId: String): List<RemoteItem>
}

/** Which search sources the user turned on or off. A source with no choice yet uses [MusicProvider.enabledByDefault]. */
interface ProviderSettings {
    val choices: StateFlow<Map<String, Boolean>>
    fun setEnabled(providerId: String, enabled: Boolean)
}

class InMemoryProviderSettings : ProviderSettings {
    private val state = kotlinx.coroutines.flow.MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val choices: StateFlow<Map<String, Boolean>> = state
    override fun setEnabled(providerId: String, enabled: Boolean) { state.value = state.value + (providerId to enabled) }
}

fun MusicProvider.isOn(choices: Map<String, Boolean>): Boolean = choices[id] ?: enabledByDefault

/** A row of Settings > Providers. */
data class ProviderOption(val id: String, val name: String, val description: String, val enabled: Boolean)

/** Time listened on one calendar day. [epochDay] is days since 1970-01-01 (UTC). */
data class DayUsage(val epochDay: Long, val offlineMs: Long = 0, val onlineMs: Long = 0)

data class TrackUsage(val title: String, val artist: String, val listenedMs: Long, val plays: Int)

data class UsageSummary(
    /** The last 30 days, oldest first, including days with no listening. */
    val days: List<DayUsage> = emptyList(),
    val totalOfflineMs: Long = 0,
    val totalOnlineMs: Long = 0,
    val topTracks: List<TrackUsage> = emptyList(),
    val todayEpochDay: Long = 0,
)

interface ListeningUsageRepository {
    val summary: StateFlow<UsageSummary>
    suspend fun clear()
}

// ---- Artwork, platform actions -----------------------------------------------------------

/** Loads a cover at about [sizePx] pixels (never full size). Returns null when there is none. */
interface ArtworkLoader {
    suspend fun load(track: Track, sizePx: Int): ImageBitmap?
}

/** Things only the host platform can do (permission prompts, system pickers). */
interface PlatformActions {
    fun hasAudioPermission(): Boolean
    fun requestAudioPermission(onResult: (Boolean) -> Unit)
    /** Opens the system folder picker. [onPicked] gets a persisted URI and a display name. */
    fun pickFolder(onPicked: (uri: String, displayName: String) -> Unit)
    fun openAppSettings()

    /** True when the system lets this app show notifications (the permission, and the per-app switch on phones like Samsung). */
    fun notificationsEnabled(): Boolean = true

    /** Asks for the notification permission, or opens the app's notification settings when asking is no longer possible. */
    fun requestNotifications() {}

    /** Launches web login to capture Spotify session (sp_dc cookie). Default does nothing. */
    fun openSpotifyLogin(onResult: (String?) -> Unit) { onResult(null) }

    fun getMediaCacheSizeBytes(): Long = 0L
    fun clearMediaCache() {}
}
