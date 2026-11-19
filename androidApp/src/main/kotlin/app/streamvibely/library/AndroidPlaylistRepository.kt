package app.streamvibely.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.streamvibely.domain.LibraryRepository
import app.streamvibely.domain.Playlist
import app.streamvibely.domain.PlaylistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.UUID

private val Context.playlistDataStore by preferencesDataStore(name = "playlists")
private val PLAYLISTS_KEY = stringPreferencesKey("playlists")

/**
 * User playlists. Only track ids are stored; [playlists] resolves them against the library, so a
 * track that disappears from the device is hidden but stays in storage and returns if it comes back.
 * A track appears at most once per playlist.
 */
class AndroidPlaylistRepository(
    context: Context,
    library: LibraryRepository,
    /** More tracks playlists may reference, such as downloaded online tracks. */
    extraTracks: kotlinx.coroutines.flow.StateFlow<List<app.streamvibely.domain.Track>> = kotlinx.coroutines.flow.MutableStateFlow(emptyList()),
) : PlaylistRepository {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stored = MutableStateFlow<List<StoredPlaylist>>(emptyList())
    private val loaded = CompletableDeferred<Unit>()
    private val mutex = Mutex()

    override val playlists: StateFlow<List<Playlist>> = combine(stored, library.tracks, extraTracks) { lists, tracks, extra ->
        val byId = (extra + tracks).associateBy { it.id }
        lists.map { p ->
            Playlist(p.id, p.name, p.description, p.trackIds.mapNotNull { byId[it] })
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    init {
        scope.launch {
            val raw = try {
                appContext.playlistDataStore.data.first()[PLAYLISTS_KEY]
            } catch (e: IOException) {
                null
            }
            stored.value = LibraryJson.decodePlaylists(raw)
            loaded.complete(Unit)
        }
    }

    override suspend fun create(name: String, description: String?): Playlist {
        val playlist = StoredPlaylist(
            id = "pl_" + UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Untitled playlist" },
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            trackIds = emptyList(),
        )
        mutate { it + playlist }
        return Playlist(playlist.id, playlist.name, playlist.description)
    }

    override suspend fun rename(id: String, name: String) {
        val clean = name.trim().ifEmpty { return }
        mutate { list -> list.map { if (it.id == id) it.copy(name = clean) else it } }
    }

    override suspend fun delete(id: String) {
        mutate { list -> list.filterNot { it.id == id } }
    }

    override suspend fun addTracks(id: String, trackIds: List<String>) {
        mutate { list ->
            list.map { p ->
                if (p.id != id) p else p.copy(trackIds = (p.trackIds + trackIds).distinct())
            }
        }
    }

    override suspend fun removeTrack(id: String, trackId: String) {
        mutate { list ->
            list.map { p -> if (p.id == id) p.copy(trackIds = p.trackIds.filterNot { it == trackId }) else p }
        }
    }

    private suspend fun mutate(transform: (List<StoredPlaylist>) -> List<StoredPlaylist>) {
        loaded.await()
        mutex.withLock {
            val new = transform(stored.value)
            if (new == stored.value) return
            stored.value = new
            try {
                appContext.playlistDataStore.edit { it[PLAYLISTS_KEY] = LibraryJson.encodePlaylists(new) }
            } catch (e: IOException) {
                // in-memory state stays; the next change writes everything again
            }
        }
    }
}
