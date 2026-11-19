package app.streamvibely.download

import android.content.Context
import android.net.Uri
import app.streamvibely.domain.DownloadRepository
import app.streamvibely.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private enum class AudioFormat(val extension: String) {
    WEBM("webm"),
    MP4("m4a"),
    OGG("ogg"),
    FLAC("flac"),
    MP3("mp3");

    companion object {
        fun detect(file: File): AudioFormat {
            val head = ByteArray(16)
            val read = try {
                file.inputStream().use { it.read(head) }
            } catch (_: Exception) {
                0
            }
            if (read < 4) return MP3

            // WebM / Matroska (EBML): 0x1A, 0x45, 0xDF, 0xA3
            if (head[0] == 0x1A.toByte() && head[1] == 0x45.toByte() &&
                head[2] == 0xDF.toByte() && head[3] == 0xA3.toByte()) {
                return WEBM
            }

            // OGG: "OggS"
            if (head[0] == 'O'.code.toByte() && head[1] == 'g'.code.toByte() &&
                head[2] == 'g'.code.toByte() && head[3] == 'S'.code.toByte()) {
                return OGG
            }

            // FLAC: "fLaC"
            if (head[0] == 'f'.code.toByte() && head[1] == 'L'.code.toByte() &&
                head[2] == 'a'.code.toByte() && head[3] == 'C'.code.toByte()) {
                return FLAC
            }

            // MP4 / M4A: "ftyp" at offset 4
            if (read >= 8 && head[4] == 'f'.code.toByte() && head[5] == 't'.code.toByte() &&
                head[6] == 'y'.code.toByte() && head[7] == 'p'.code.toByte()) {
                return MP4
            }

            return MP3
        }
    }
}

/**
 * Saves downloadable online tracks into the app's own storage (no storage permission needed;
 * removed with the app). An index file remembers the track details. Two downloads run at a time.
 * For MP3 files, ID3 tags are attached. For WebM/Opus and MP4/AAC, the native containers are preserved intact.
 * Each track gets a `.lrc` file when synced lyrics exist, and album art is cached locally.
 */
class AndroidDownloadRepository(
    context: Context,
    /** Looks up lyrics for the track being saved. Failures and timeouts just mean no lyrics. */
    private val lyricsFor: suspend (Track) -> app.streamvibely.domain.Lyrics? = { null },
) : DownloadRepository {
    private val dir = File(context.applicationContext.getExternalFilesDir("Music") ?: context.applicationContext.filesDir, "downloads")
        .apply { mkdirs() }
    private val index = File(dir, "index.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Semaphore(2)
    private val lock = Any()

    private val _downloaded = MutableStateFlow<List<Track>>(emptyList())
    override val downloaded: StateFlow<List<Track>> = _downloaded
    private val _progress = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val progress: StateFlow<Map<String, Float>> = _progress

    init {
        dir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
        val rawList = readIndex()
        val repaired = rawList.mapNotNull { t ->
            val u = t.uri ?: return@mapNotNull null
            val file = File(Uri.parse(u).path ?: "")
            if (!file.exists()) return@mapNotNull null
            val fixed = repairCorruptedDownload(file)
            if (fixed.path != file.path) {
                t.copy(uri = Uri.fromFile(fixed).toString())
            } else {
                t
            }
        }
        _downloaded.value = repaired
        if (repaired != rawList) {
            writeIndex(repaired)
        }
    }

    override fun download(track: Track) {
        val url = track.uri ?: return
        if (!url.startsWith("https://")) return
        if (_downloaded.value.any { it.id == track.id } || _progress.value.containsKey(track.id)) return
        setProgress(track.id, 0f)
        scope.launch {
            try {
                gate.withPermit { fetch(track, url) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Failed downloads return to the "not downloaded" state.
            } finally {
                setProgress(track.id, null)
            }
        }
    }

    override fun remove(trackId: String) {
        synchronized(lock) {
            val track = _downloaded.value.firstOrNull { it.id == trackId } ?: return
            track.uri?.let { File(Uri.parse(it).path ?: "").delete() }
            track.artworkUrl?.takeIf { it.startsWith("file://") }?.let { File(Uri.parse(it).path ?: "").delete() }
            track.uri?.let {
                val f = File(Uri.parse(it).path ?: "")
                f.resolveSibling("${f.nameWithoutExtension}.lrc").delete()
            }
            _downloaded.value = _downloaded.value.filterNot { it.id == trackId }
            writeIndex(_downloaded.value)
        }
    }

    private suspend fun fetch(track: Track, url: String) {
        val base = hash(track.id)
        val part = File(dir, "$base.part")
        part.delete()

        val isYouTube = url.contains("googlevideo.com") || url.contains("youtube.com")
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        val success = if (isYouTube) {
            downloadChunked(track, url, part, userAgent)
        } else {
            downloadDirect(track, url, part, userAgent)
        }

        if (!success || !part.exists() || part.length() == 0L) {
            part.delete()
            return
        }

        val format = AudioFormat.detect(part)
        val audio = File(dir, "$base.${format.extension}")

        val cover = saveCover(track, base)
        val lyrics = try {
            kotlinx.coroutines.withTimeoutOrNull(10_000) { lyricsFor(track) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        val lyricsText = when (lyrics) {
            is app.streamvibely.domain.Lyrics.Synced -> lyrics.lines.joinToString("\n") { it.text }
            is app.streamvibely.domain.Lyrics.Plain -> lyrics.text
            null -> null
        }
        if (lyrics is app.streamvibely.domain.Lyrics.Synced) {
            File(dir, "$base.lrc").writeText(app.streamvibely.tags.LrcWriter.write(lyrics.lines))
        }

        if (format == AudioFormat.MP3) {
            val tags = app.streamvibely.tags.Id3Tags(track.title, track.artist, track.album, lyricsText, cover?.readBytes())
            if (!writeTagged(part, audio, tags)) {
                if (!part.renameTo(audio)) { part.copyTo(audio, overwrite = true); part.delete() }
            }
        } else {
            // WebM, MP4, OGG, FLAC containers must stay untouched to prevent corruption
            if (!part.renameTo(audio)) {
                part.copyTo(audio, overwrite = true)
                part.delete()
            }
        }

        val saved = track.copy(
            uri = Uri.fromFile(audio).toString(),
            artworkUrl = cover?.let { Uri.fromFile(it).toString() },
            isDownloaded = true,
            canDownload = false,
        )
        synchronized(lock) {
            _downloaded.value = _downloaded.value.filterNot { it.id == track.id } + saved
            writeIndex(_downloaded.value)
        }
    }

    private fun downloadChunked(track: Track, url: String, part: File, userAgent: String): Boolean {
        var total = -1L
        try {
            val probe = open(url).apply {
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Range", "bytes=0-0")
            }
            try {
                if (probe.responseCode in 200..299) {
                    val cr = probe.getHeaderField("Content-Range")
                    total = parseContentRangeTotal(cr)
                }
            } finally {
                probe.disconnect()
            }
        } catch (_: Exception) {}

        if (total <= 0) {
            return downloadDirect(track, url, part, userAgent)
        }

        if (total > MAX_BYTES) return false

        val chunkSize = 512 * 1024L
        var start = 0L
        var lastReportedTime = 0L
        return try {
            part.outputStream().use { out ->
                while (start < total) {
                    val end = minOf(start + chunkSize - 1, total - 1)
                    val conn = open(url).apply {
                        setRequestProperty("User-Agent", userAgent)
                        setRequestProperty("Range", "bytes=$start-$end")
                    }
                    try {
                        val code = conn.responseCode
                        if (code != 206 && code != 200) {
                            return false
                        }
                        conn.inputStream.use { it.copyTo(out) }
                        start = end + 1
                        val now = System.currentTimeMillis()
                        if (now - lastReportedTime >= 250 || start >= total) {
                            lastReportedTime = now
                            setProgress(track.id, start.toFloat() / total)
                        }
                    } finally {
                        conn.disconnect()
                    }
                }
            }
            part.length() >= total
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadDirect(track: Track, url: String, part: File, userAgent: String): Boolean {
        return try {
            val conn = open(url).apply {
                setRequestProperty("User-Agent", userAgent)
            }
            try {
                if (conn.responseCode !in 200..299) return false
                val total = conn.contentLengthLong
                if (total > MAX_BYTES) return false
                var done = 0L
                var lastReportedTime = 0L
                conn.inputStream.use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            done += n
                            if (done > MAX_BYTES) return false
                            output.write(buffer, 0, n)
                            val now = System.currentTimeMillis()
                            if (total > 0 && (now - lastReportedTime >= 250 || done == total)) {
                                lastReportedTime = now
                                setProgress(track.id, done.toFloat() / total)
                            }
                        }
                    }
                }
                true
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun parseContentRangeTotal(cr: String?): Long {
        if (cr == null) return -1L
        val idx = cr.lastIndexOf('/')
        return if (idx >= 0) cr.substring(idx + 1).trim().toLongOrNull() ?: -1L else -1L
    }

    /** Writes the tag, then the audio without any tag it already had, into [out]; deletes [part] on success. */
    private fun writeTagged(part: File, out: File, tags: app.streamvibely.tags.Id3Tags): Boolean {
        val tmp = File(out.path + ".tag")
        return try {
            val tag = app.streamvibely.tags.Id3v2.encode(tags)
            part.inputStream().buffered().use { input ->
                input.mark(16)
                val head = ByteArray(10)
                val n = input.read(head)
                input.reset()
                val skip = if (n == 10) app.streamvibely.tags.Id3v2.existingTagLength(head).toLong() else 0L
                if (skip > 0) input.skip(skip)
                tmp.outputStream().buffered().use { o -> o.write(tag); input.copyTo(o) }
            }
            if (tmp.renameTo(out)) { part.delete(); true } else { tmp.delete(); false }
        } catch (e: Exception) {
            tmp.delete()
            false
        }
    }

    /** Strips prepended ID3 tag if prepended to a WebM or MP4 container. */
    private fun repairCorruptedDownload(file: File): File {
        if (!file.exists() || file.length() < 16) return file
        try {
            val head = ByteArray(10)
            file.inputStream().use { it.read(head) }
            if (head[0] == 'I'.code.toByte() && head[1] == 'D'.code.toByte() && head[2] == '3'.code.toByte()) {
                val tagLen = ((head[6].toInt() and 0x7F) shl 21) or
                             ((head[7].toInt() and 0x7F) shl 14) or
                             ((head[8].toInt() and 0x7F) shl 7) or
                             (head[9].toInt() and 0x7F)
                val skip = tagLen + 10L
                val next = ByteArray(8)
                file.inputStream().use {
                    it.skip(skip)
                    it.read(next)
                }
                val isWebM = next[0] == 0x1A.toByte() && next[1] == 0x45.toByte() &&
                             next[2] == 0xDF.toByte() && next[3] == 0xA3.toByte()
                val isMp4 = next.size >= 8 && next[4] == 'f'.code.toByte() && next[5] == 't'.code.toByte() &&
                            next[6] == 'y'.code.toByte() && next[7] == 'p'.code.toByte()
                if (isWebM || isMp4) {
                    val ext = if (isWebM) "webm" else "m4a"
                    val fixed = File(file.parentFile, file.nameWithoutExtension + "." + ext)
                    val tmp = File(file.parentFile, file.nameWithoutExtension + ".tmp")
                    file.inputStream().use { input ->
                        input.skip(skip)
                        tmp.outputStream().use { out -> input.copyTo(out) }
                    }
                    if (tmp.renameTo(fixed)) {
                        file.delete()
                        return fixed
                    } else {
                        tmp.delete()
                    }
                }
            }
        } catch (_: Exception) {}
        return file
    }

    private fun saveCover(track: Track, base: String): File? {
        val url = track.artworkUrl?.takeIf { it.startsWith("https://") } ?: return null
        return try {
            val connection = open(url)
            try {
                if (connection.responseCode !in 200..299) return null
                val bytes = connection.inputStream.use { it.readNBytes(COVER_MAX) }
                File(dir, "$base.jpg").also { it.writeBytes(bytes) }
            } finally {
                connection.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun open(url: String) = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 20_000
        instanceFollowRedirects = true
    }

    private fun setProgress(id: String, value: Float?) {
        _progress.value = if (value == null) _progress.value - id else _progress.value + (id to value)
    }

    private fun hash(id: String) = MessageDigest.getInstance("SHA-1").digest(id.toByteArray())
        .joinToString("") { "%02x".format(it) }.take(24)

    private fun readIndex(): List<Track> = try {
        val arr = JSONArray(index.takeIf { it.exists() }?.readText() ?: "[]")
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Track(
                id = o.getString("id"), title = o.getString("title"), artist = o.getString("artist"),
                album = o.optString("album").ifEmpty { null }, durationMs = o.optLong("durationMs"),
                artworkUrl = o.optString("artworkUrl").ifEmpty { null }, source = o.optString("source", "download"),
                isDownloaded = true, uri = o.getString("uri"),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun writeIndex(tracks: List<Track>) {
        val arr = JSONArray()
        tracks.forEach { t ->
            arr.put(JSONObject().apply {
                put("id", t.id); put("title", t.title); put("artist", t.artist)
                put("album", t.album ?: ""); put("durationMs", t.durationMs)
                put("artworkUrl", t.artworkUrl ?: ""); put("source", t.source); put("uri", t.uri ?: "")
            })
        }
        val tmp = File(dir, "index.json.tmp")
        tmp.writeText(arr.toString())
        tmp.renameTo(index)
    }

    private companion object {
        const val MAX_BYTES = 300L * 1024 * 1024
        const val COVER_MAX = 4 * 1024 * 1024
    }
}
