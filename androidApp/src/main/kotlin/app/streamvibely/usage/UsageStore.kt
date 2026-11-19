package app.streamvibely.usage

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Days kept, including today. */
internal const val WINDOW_DAYS = 30

/** Per-track totals kept; the least listened are dropped beyond this. */
internal const val MAX_TRACKS = 200

internal class DayBucket(var offlineMs: Long = 0L, var onlineMs: Long = 0L)

internal class TrackBucket(
    var title: String,
    var artist: String,
    var listenedMs: Long = 0L,
    var plays: Int = 0,
)

/** All usage state. Only touched on the repository's serial context. */
internal class UsageData {
    /** Keyed by UTC epoch day. */
    val days = HashMap<Long, DayBucket>()

    /** Keyed by track id. */
    val tracks = HashMap<String, TrackBucket>()

    /**
     * Drops the least listened tracks until [MAX_TRACKS] remain. [protect] (the track playing now)
     * is never dropped, so a new track can grow past the current minimum.
     */
    fun trimTracks(protect: String?) {
        while (tracks.size > MAX_TRACKS) {
            val victim = tracks.entries
                .filter { it.key != protect }
                .minByOrNull { it.value.listenedMs } ?: return
            tracks.remove(victim.key)
        }
    }

    /** Returns true when any day was removed. */
    fun dropDaysBefore(firstKept: Long): Boolean = days.keys.removeAll { it < firstKept }
}

/**
 * Reads and writes the usage file. Decoding never throws: a missing or damaged file yields empty
 * data. Writes go to a temp file that then replaces the real one, so a crash never leaves half a file.
 */
internal class UsageStore(private val file: File) {

    private val tmpFile = File(file.parentFile, file.name + ".tmp")

    fun load(today: Long): UsageData {
        val data = UsageData()
        if (!file.isFile) return data
        val firstKept = today - WINDOW_DAYS + 1
        return try {
            val root = JSONObject(file.readText(Charsets.UTF_8))
            root.optJSONArray("days").objects().forEach { o ->
                val day = o.optLong("d", Long.MIN_VALUE)
                if (day in firstKept..today) {
                    data.days[day] = DayBucket(o.optLong("off"), o.optLong("on"))
                }
            }
            root.optJSONArray("tracks").objects().forEach { o ->
                val id = o.optString("id")
                if (id.isNotEmpty()) {
                    data.tracks[id] = TrackBucket(
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        listenedMs = o.optLong("ms"),
                        plays = o.optInt("plays"),
                    )
                }
            }
            data.trimTracks(null)
            data
        } catch (e: JSONException) {
            UsageData()
        } catch (e: IOException) {
            UsageData()
        }
    }

    /** Writes [data] atomically. Throws [IOException] when the disk write fails. */
    fun save(data: UsageData) {
        val days = JSONArray()
        for ((day, bucket) in data.days) {
            days.put(JSONObject().put("d", day).put("off", bucket.offlineMs).put("on", bucket.onlineMs))
        }
        val tracks = JSONArray()
        for ((id, t) in data.tracks) {
            tracks.put(
                JSONObject()
                    .put("id", id)
                    .put("title", t.title)
                    .put("artist", t.artist)
                    .put("ms", t.listenedMs)
                    .put("plays", t.plays)
            )
        }
        val root = JSONObject().put("version", 1).put("days", days).put("tracks", tracks)
        val bytes = root.toString().toByteArray(Charsets.UTF_8)

        FileOutputStream(tmpFile).use { out ->
            out.write(bytes)
            out.flush()
            out.fd.sync()
        }
        if (!tmpFile.renameTo(file)) {
            file.delete()
            if (!tmpFile.renameTo(file)) throw IOException("could not replace listening usage file")
        }
    }

    fun delete() {
        file.delete()
        tmpFile.delete()
    }

    private fun JSONArray?.objects(): List<JSONObject> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { optJSONObject(it) }
    }
}
