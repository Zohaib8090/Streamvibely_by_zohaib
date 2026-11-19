package app.streamvibely.playback

import android.content.Context
import app.streamvibely.domain.RecentlyPlayedRepository
import app.streamvibely.domain.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AndroidRecentlyPlayedRepository(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) : RecentlyPlayedRepository {

    private val file = File(context.filesDir, "recently_played_tracks.json")
    private val _recentTracks = MutableStateFlow<List<Track>>(loadPersisted())
    override val recentTracks: StateFlow<List<Track>> = _recentTracks.asStateFlow()

    override fun recordPlay(track: Track) {
        val current = _recentTracks.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)
        val updated = current.take(30)
        _recentTracks.value = updated
        scope.launch { persist(updated) }
    }

    override fun clear() {
        _recentTracks.value = emptyList()
        scope.launch { runCatching { file.delete() } }
    }

    private fun loadPersisted(): List<Track> {
        if (!file.exists()) return emptyList()
        return try {
            val content = file.readText()
            val array = JSONArray(content)
            val list = mutableListOf<Track>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id")
                val title = obj.optString("title")
                val artist = obj.optString("artist")
                if (id.isBlank() || title.isBlank()) continue
                list.add(
                    Track(
                        id = id,
                        title = title,
                        artist = artist,
                        album = obj.optString("album").takeIf { it.isNotBlank() },
                        durationMs = obj.optLong("durationMs", 0L),
                        artworkUrl = obj.optString("artworkUrl").takeIf { it.isNotBlank() },
                        source = obj.optString("source", "local"),
                        isDownloaded = obj.optBoolean("isDownloaded", false),
                        uri = obj.optString("uri").takeIf { it.isNotBlank() },
                        canDownload = obj.optBoolean("canDownload", false),
                        qualityLabel = obj.optString("qualityLabel").takeIf { it.isNotBlank() },
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(tracks: List<Track>) {
        try {
            val array = JSONArray()
            for (t in tracks) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("album", t.album ?: "")
                    put("durationMs", t.durationMs)
                    put("artworkUrl", t.artworkUrl ?: "")
                    put("source", t.source)
                    put("isDownloaded", t.isDownloaded)
                    put("uri", t.uri ?: "")
                    put("canDownload", t.canDownload)
                    put("qualityLabel", t.qualityLabel ?: "")
                }
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (e: Exception) {
            // best-effort persistence
        }
    }
}

