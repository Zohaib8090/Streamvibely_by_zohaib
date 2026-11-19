package app.streamvibely.library

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import app.streamvibely.domain.MusicFolder
import app.streamvibely.domain.Track
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.security.MessageDigest

private const val PROGRESS_EVERY = 10

private val AUDIO_EXTENSIONS = setOf(
    "mp3", "m4a", "aac", "flac", "wav", "ogg", "oga", "opus", "wma", "amr", "aif", "aiff", "mka",
)
private val PLAYLIST_EXTENSIONS = setOf("m3u", "m3u8", "pls", "cue")
private val PLAYLIST_MIMES = setOf("audio/x-mpegurl", "audio/mpegurl", "audio/x-scpls", "audio/scpls")

private class ChildEntry(val docId: String, val name: String, val mime: String?, val lastModified: Long)

/** Stable short hash of a string, as lowercase hex. */
internal fun stableHash(value: String): String {
    val bytes = MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8))
    return bytes.take(8).joinToString("") { "%02x".format(it) }
}

/** Walks user-picked SAF folders and turns the audio files in them into tracks. */
internal class SafScanner(private val context: Context, private val cache: MetadataCache) {

    suspend fun scan(folders: List<MusicFolder>, minDurationMs: Long, onProgress: (Int) -> Unit): List<Track> {
        val found = LinkedHashMap<String, Track>()
        var seen = 0
        for (folder in folders) {
            val treeUri = try { Uri.parse(folder.uri) } catch (e: Exception) { continue }
            val rootId = try { DocumentsContract.getTreeDocumentId(treeUri) } catch (e: Exception) { continue }
            val pending = ArrayDeque<String>()
            val visited = HashSet<String>()
            pending.addLast(rootId)
            while (pending.isNotEmpty()) {
                currentCoroutineContext().ensureActive()
                val dirId = pending.removeLast()
                if (!visited.add(dirId)) continue
                for (child in listChildren(treeUri, dirId)) {
                    currentCoroutineContext().ensureActive()
                    if (child.mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.addLast(child.docId)
                    } else if (isAudio(child)) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, child.docId)
                        val track = trackFor(docUri, child)
                        // duration 0 means "unknown": keep those rather than hide playable files
                        if (track.durationMs == 0L || track.durationMs >= minDurationMs) found.putIfAbsent(track.id, track)
                        seen++
                        if (seen % PROGRESS_EVERY == 0) onProgress(found.size)
                    }
                }
            }
        }
        return found.values.toList()
    }

    /** One query per directory; a failing directory (permission lost, provider gone) is skipped. */
    private fun listChildren(treeUri: Uri, dirId: String): List<ChildEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, dirId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )
        val out = ArrayList<ChildEntry>()
        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    out.add(ChildEntry(id, c.getString(1) ?: "", c.getString(2), if (c.isNull(3)) 0L else c.getLong(3)))
                }
            }
        } catch (e: Exception) {
            // skip this directory
        }
        return out
    }

    private fun isAudio(e: ChildEntry): Boolean {
        val ext = e.name.substringAfterLast('.', "").lowercase()
        val mime = e.mime?.lowercase()
        if (ext in PLAYLIST_EXTENSIONS || mime in PLAYLIST_MIMES) return false
        return (mime != null && mime.startsWith("audio/")) || ext in AUDIO_EXTENSIONS
    }

    private fun trackFor(docUri: Uri, entry: ChildEntry): Track {
        val key = docUri.toString()
        val meta = cache.get(key, entry.lastModified)
            ?: extract(docUri, entry.lastModified).also { cache.put(key, it) }
        val fileTitle = entry.name.substringBeforeLast('.').trim().ifEmpty { UNKNOWN_TITLE }
        return Track(
            id = "saf:" + stableHash(key),
            title = cleanMeta(meta.title) ?: fileTitle,
            artist = cleanMeta(meta.artist) ?: UNKNOWN_ARTIST,
            album = cleanMeta(meta.album),
            durationMs = meta.durationMs,
            source = "local",
            isDownloaded = true,
            uri = key,
        )
    }

    private fun extract(uri: Uri, lastModified: Long): CachedMeta {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            return CachedMeta(
                lastModified = lastModified,
                title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE),
                artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST),
                album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM),
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()?.coerceAtLeast(0) ?: 0L,
            )
        } catch (e: Exception) {
            return CachedMeta(lastModified, null, null, null, 0L) // odd file: use the file name
        } finally {
            try { retriever.release() } catch (e: Exception) { /* ignore */ }
        }
    }

    companion object {
        fun cacheFile(context: Context): File = File(context.filesDir, "library_metadata_cache.json")
    }
}
