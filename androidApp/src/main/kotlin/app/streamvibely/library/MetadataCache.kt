package app.streamvibely.library

import org.json.JSONObject
import java.io.File

/** Metadata read from one audio document. Empty strings are stored as null. */
internal data class CachedMeta(
    val lastModified: Long,
    val title: String?,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
)

/**
 * Remembers extracted tags keyed by document URI so a rescan only reads new or changed files.
 * One instance serves one scan: [save] keeps only the entries touched during that scan, which
 * drops files that were deleted. Stored privately in the app's files directory.
 */
internal class MetadataCache(private val file: File) {
    private val old = HashMap<String, CachedMeta>()
    private val used = HashMap<String, CachedMeta>()

    fun load() {
        try {
            if (!file.exists()) return
            val entries = JSONObject(file.readText()).optJSONObject("e") ?: return
            for (key in entries.keys()) {
                val e = entries.optJSONObject(key) ?: continue
                old[key] = CachedMeta(
                    lastModified = e.optLong("m"),
                    title = e.optString("t").ifEmpty { null },
                    artist = e.optString("a").ifEmpty { null },
                    album = e.optString("b").ifEmpty { null },
                    durationMs = e.optLong("d"),
                )
            }
        } catch (e: Exception) {
            old.clear() // damaged cache: start over
        }
    }

    fun get(key: String, lastModified: Long): CachedMeta? {
        val hit = old[key]?.takeIf { it.lastModified == lastModified } ?: return null
        used[key] = hit
        return hit
    }

    fun put(key: String, meta: CachedMeta) {
        used[key] = meta
    }

    fun save() {
        try {
            val entries = JSONObject()
            for ((key, m) in used) {
                entries.put(key, JSONObject().apply {
                    put("m", m.lastModified)
                    put("t", m.title ?: "")
                    put("a", m.artist ?: "")
                    put("b", m.album ?: "")
                    put("d", m.durationMs)
                })
            }
            val text = JSONObject().put("v", 1).put("e", entries).toString()
            synchronized(LOCK) {
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(text)
                if (!tmp.renameTo(file)) {
                    file.delete()
                    tmp.renameTo(file)
                }
            }
        } catch (e: Exception) {
            // The cache is only an optimisation; losing it just makes the next scan slower.
        }
    }

    private companion object {
        val LOCK = Any()
    }
}
