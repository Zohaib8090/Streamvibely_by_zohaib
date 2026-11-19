package app.streamvibely.domain

import kotlinx.coroutines.flow.StateFlow

data class SpotifyTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long = 0,
    val artworkUrl: String? = null,
)

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val totalTracks: Int = 0,
    val artworkUrl: String? = null,
)

enum class SpotifySyncState {
    Idle,
    Syncing,
    Done,
    Error,
}

interface SpotifySyncRepository {
    /** True if sp_dc session cookie or token is stored and valid. */
    val isConnected: StateFlow<Boolean>
    val syncState: StateFlow<SpotifySyncState>
    val statusMessage: StateFlow<String?>
    val syncedTracks: StateFlow<List<Track>>

    suspend fun saveSession(spDcCookie: String): Boolean
    suspend fun disconnect()
    suspend fun fetchLikedSongs(): List<SpotifyTrack>
    suspend fun fetchPlaylists(): List<SpotifyPlaylist>
    suspend fun fetchPlaylistTracks(playlistId: String): List<SpotifyTrack>

    /** Syncs Liked Songs and all playlists into the user's Streamvibely playlists. */
    suspend fun syncAll(playlistRepository: PlaylistRepository): Result<Int>
}

