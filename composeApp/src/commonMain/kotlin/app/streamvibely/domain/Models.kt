package app.streamvibely.domain

/** Core data shapes shared by every screen and provider. Keep this file small and stable. */

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val artworkUrl: String? = null,
    /** Which provider the track came from, e.g. "local", "youtube". */
    val source: String = "local",
    val isDownloaded: Boolean = false,
    /** Where the audio can be read from: a content:// or file URI for local tracks. */
    val uri: String? = null,
    /** True only when the source allows saving the file (open licenses). Streaming-only sources stay false. */
    val canDownload: Boolean = false,
    /** Label describing the stream or file quality (e.g. "Opus 256 kbps", "MP3 320 kbps", "Local file"). */
    val qualityLabel: String? = null,
    /** Short license name for openly licensed music, e.g. "CC BY-NC-SA 3.0". */
    val license: String? = null,
)

data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val tracks: List<Track> = emptyList(),
    val artworkUrl: String? = null,
)

enum class DownloadState { None, Queued, Downloading, Done, Failed }

/** One line of time-synced lyrics. [startMs] is when the line should highlight. */
data class LyricLine(val startMs: Long, val text: String)

/** Lyrics come in two kinds: time-synced (LRC-like) and plain text with no timing. */
sealed interface Lyrics {
    val source: String

    data class Synced(override val source: String, val lines: List<LyricLine>) : Lyrics
    data class Plain(override val source: String, val text: String) : Lyrics
}

/** A place lyrics can be fetched from. Many can be registered; they are tried in priority order. */
interface LyricsProvider {
    val id: String
    val displayName: String
    /** Returns null when this provider has nothing for the track. Must not throw for "not found". */
    suspend fun fetch(track: Track): Lyrics?
}

/** Quality ladder used to pick the best stream automatically. Higher rank wins. */
enum class AudioQuality(val rank: Int) { DataSaver(0), Standard(1), High(2), Lossless(3), HiRes(4) }

data class StreamCandidate(
    val url: String,
    val quality: AudioQuality,
    val bitrateKbps: Int? = null,
    val mimeType: String? = null,
)

/** Picks the highest quality candidate; ties broken by bitrate. */
fun List<StreamCandidate>.best(): StreamCandidate? =
    maxByOrNull { it.quality.rank * 1_000_000 + (it.bitrateKbps ?: 0) }

/**
 * A music source (YouTube, local files, other providers). Implementations live in provider
 * modules. This is only the contract; no provider is implemented yet.
 */
interface MusicProvider {
    val id: String
    val displayName: String
    suspend fun search(query: String): List<Track>
    suspend fun streams(track: Track): List<StreamCandidate>
    suspend fun streams(track: Track, maxBitrateKbps: Int?): List<StreamCandidate> = streams(track)

    /** True when searching needs the user to be signed in to an account first. */
    val requiresAccount: Boolean get() = false

    /**
     * False when this source can list tracks but has no way to play them yet. Such a source still gets its own
     * Search chip, but is left out of the "All" search and tapping a result explains what is missing.
     */
    val canPlay: Boolean get() = true

    /** One line shown under the name in Settings > Providers. */
    val description: String get() = ""

    /** Whether the source is on until the user chooses otherwise. */
    val enabledByDefault: Boolean get() = true
}

/** Off plays the queue once, All loops the queue, One loops the current song. */
enum class RepeatMode { Off, All, One }

/** What the player UI needs to draw. A real player implements [PlayerController] later. */
data class PlayerUiState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val shuffle: Boolean = false,
    val repeat: RepeatMode = RepeatMode.Off,
)

interface PlayerController {
    val state: kotlinx.coroutines.flow.StateFlow<PlayerUiState>
    fun play(track: Track, queue: List<Track> = emptyList())
    fun togglePlayPause()
    fun next()
    fun previous()
    fun seekTo(positionMs: Long)
    fun toggleShuffle()
    fun toggleRepeat()

    /** Inserts [track] right after the current one. Default does nothing. */
    fun playNext(track: Track) {}

    /**
     * Turns a queued track that has no stream URL yet into one, when its turn comes. The player calls
     * it ahead of time for the next song so playback moves on by itself. Default does nothing.
     */
    var trackResolver: (suspend (Track) -> String?)?
        get() = null
        set(value) {}

    /** Stops playback and clears the queue so the mini player goes away. Default does nothing. */
    fun stop() {}
}
