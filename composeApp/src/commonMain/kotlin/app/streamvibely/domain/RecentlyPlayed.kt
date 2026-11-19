package app.streamvibely.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface RecentlyPlayedRepository {
    val recentTracks: StateFlow<List<Track>>
    fun recordPlay(track: Track)
    fun clear()
}

class InMemoryRecentlyPlayedRepository : RecentlyPlayedRepository {
    private val _recentTracks = MutableStateFlow<List<Track>>(emptyList())
    override val recentTracks: StateFlow<List<Track>> = _recentTracks

    override fun recordPlay(track: Track) {
        val current = _recentTracks.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)
        _recentTracks.value = current.take(30)
    }

    override fun clear() {
        _recentTracks.value = emptyList()
    }
}

