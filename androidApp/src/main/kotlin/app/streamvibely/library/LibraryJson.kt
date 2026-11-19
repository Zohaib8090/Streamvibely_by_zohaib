package app.streamvibely.library

import app.streamvibely.domain.LibrarySettings
import app.streamvibely.domain.MusicFolder
import app.streamvibely.domain.ScanMode
import org.json.JSONArray
import org.json.JSONObject

/** A playlist as stored on disk: only track ids, resolved against the library when read. */
internal data class StoredPlaylist(
    val id: String,
    val name: String,
    val description: String?,
    val trackIds: List<String>,
)

/**
 * JSON codecs for what the library package persists. Uses org.json (part of Android) because the
 * androidApp module does not apply the kotlinx-serialization compiler plugin.
 * Decoding never throws: damaged or missing data yields defaults.
 */
internal object LibraryJson {

    fun encodeSettings(settings: LibrarySettings): String = JSONObject().apply {
        put("scanMode", settings.scanMode.name)
        put("minDurationSeconds", settings.minDurationSeconds)
        put("folders", JSONArray().also { arr ->
            settings.folders.forEach { f ->
                arr.put(JSONObject().put("id", f.id).put("name", f.displayName).put("uri", f.uri))
            }
        })
    }.toString()

    fun decodeSettings(raw: String?): LibrarySettings {
        if (raw.isNullOrBlank()) return LibrarySettings()
        return try {
            val o = JSONObject(raw)
            val defaults = LibrarySettings()
            val mode = ScanMode.entries.firstOrNull { it.name == o.optString("scanMode") } ?: defaults.scanMode
            val folders = ArrayList<MusicFolder>()
            val arr = o.optJSONArray("folders")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val f = arr.optJSONObject(i) ?: continue
                    val uri = f.optString("uri")
                    val id = f.optString("id")
                    if (uri.isEmpty() || id.isEmpty()) continue
                    folders.add(MusicFolder(id, f.optString("name", "Music folder"), uri))
                }
            }
            LibrarySettings(mode, folders, o.optInt("minDurationSeconds", defaults.minDurationSeconds))
        } catch (e: Exception) {
            LibrarySettings()
        }
    }

    fun encodePlaylists(list: List<StoredPlaylist>): String = JSONArray().also { arr ->
        list.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                if (p.description != null) put("description", p.description)
                put("tracks", JSONArray(p.trackIds))
            })
        }
    }.toString()

    fun decodePlaylists(raw: String?): List<StoredPlaylist> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val out = ArrayList<StoredPlaylist>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                if (id.isEmpty()) continue
                val ids = ArrayList<String>()
                val t = o.optJSONArray("tracks")
                if (t != null) for (j in 0 until t.length()) t.optString(j).takeIf { it.isNotEmpty() }?.let(ids::add)
                out.add(
                    StoredPlaylist(
                        id = id,
                        name = o.optString("name", "Playlist"),
                        description = if (o.has("description")) o.optString("description") else null,
                        trackIds = ids,
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }
}
