package app.streamvibely.library

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import app.streamvibely.domain.Track
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal const val UNKNOWN_ARTIST = "Unknown artist"
internal const val UNKNOWN_TITLE = "Unknown title"
private const val MEDIASTORE_UNKNOWN = "<unknown>"
private const val PROGRESS_EVERY = 100

/** Cleans a metadata string: null for blank or MediaStore's "<unknown>" marker. */
internal fun cleanMeta(value: String?): String? =
    value?.trim()?.takeIf { it.isNotEmpty() && !it.equals(MEDIASTORE_UNKNOWN, ignoreCase = true) }

/** Reads every music file the system media index knows about. */
internal object MediaStoreScanner {

    suspend fun scan(context: Context, minDurationMs: Long, onProgress: (Int) -> Unit): List<Track> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf(minDurationMs.toString())
        val out = ArrayList<Track>()
        val cursor = context.contentResolver.query(collection, projection, selection, args, null) ?: return out
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val title = cleanMeta(c.getString(titleCol))
                    ?: cleanMeta(c.getString(nameCol))?.substringBeforeLast('.')?.takeIf { it.isNotEmpty() }
                    ?: UNKNOWN_TITLE
                out.add(
                    Track(
                        id = "local:$id",
                        title = title,
                        artist = cleanMeta(c.getString(artistCol)) ?: UNKNOWN_ARTIST,
                        album = cleanMeta(c.getString(albumCol)),
                        durationMs = c.getLong(durCol).coerceAtLeast(0),
                        source = "local",
                        isDownloaded = true,
                        uri = ContentUris.withAppendedId(collection, id).toString(),
                    )
                )
                if (out.size % PROGRESS_EVERY == 0) {
                    currentCoroutineContext().ensureActive()
                    onProgress(out.size)
                }
            }
        }
        return out
    }
}
