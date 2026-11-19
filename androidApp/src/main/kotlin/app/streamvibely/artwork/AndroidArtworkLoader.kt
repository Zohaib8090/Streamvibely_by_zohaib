package app.streamvibely.artwork

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import app.streamvibely.domain.ArtworkLoader
import app.streamvibely.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Local album art for Android tracks. Tries MediaStore thumbnails (API 29+) first, then the
 * embedded picture of the file. Never decodes a full-size cover. Create one instance and share it,
 * because the cache lives in the instance.
 */
class AndroidArtworkLoader(context: Context) : ArtworkLoader {
    private val appContext = context.applicationContext
    private val cache = ArtworkCache()

    override suspend fun load(track: Track, sizePx: Int): ImageBitmap? {
        // Online results may not have a stream URL yet (resolved on tap) but still have a cover link.
        val uri = track.uri
        if (uri == null || uri.startsWith("http")) return loadRemote(track, sizePx)
        track.artworkUrl?.takeIf { it.startsWith("file://") }?.let { return loadCoverFile(track, it, sizePx) }
        if (cache.isMiss(track.id)) return null
        val key = "${track.id}@$sizePx"
        cache.bitmap(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = decode(Uri.parse(uri), sizePx)
                if (bitmap == null) {
                    cache.putMiss(track.id)
                    null
                } else {
                    bitmap.asImageBitmap().also { cache.putBitmap(key, it) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Online tracks: downloads the small cover from [Track.artworkUrl] (https only), sampled down to [sizePx]. */
    private suspend fun loadRemote(track: Track, sizePx: Int): ImageBitmap? {
        val url = track.artworkUrl?.takeIf { it.startsWith("https://") } ?: return null
        if (cache.isMiss(track.id)) return null
        val key = "${track.id}@$sizePx"
        cache.bitmap(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val bytes = try {
                    // Bounded read so a bad server cannot make us hold a huge body.
                    connection.inputStream.use { it.readNBytes(MAX_COVER_BYTES) }
                } finally {
                    connection.disconnect()
                }
                val bitmap = decodeSampled(bytes, sizePx)?.let { fit(it, sizePx) }
                if (bitmap == null) { cache.putMiss(track.id); null }
                else bitmap.asImageBitmap().also { cache.putBitmap(key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    /** Cover saved next to a downloaded track. */
    private suspend fun loadCoverFile(track: Track, fileUri: String, sizePx: Int): ImageBitmap? {
        val key = "${track.id}@$sizePx"
        cache.bitmap(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bytes = java.io.File(Uri.parse(fileUri).path ?: return@withContext null).readBytes()
                decodeSampled(bytes, sizePx)?.let { fit(it, sizePx) }?.asImageBitmap()?.also { cache.putBitmap(key, it) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }

    private companion object { const val MAX_COVER_BYTES = 4 * 1024 * 1024 }

    private fun decode(uri: Uri, sizePx: Int): Bitmap? {
        val raw = thumbnail(uri, sizePx) ?: embedded(uri, sizePx) ?: return null
        return fit(raw, sizePx)
    }

    /** MediaStore audio URIs only. Returns null on older Android or when the provider refuses. */
    private fun thumbnail(uri: Uri, sizePx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            appContext.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
        } catch (e: Exception) {
            null
        }
    }

    /** Works for SAF document URIs too. The retriever is released in every case. */
    private fun embedded(uri: Uri, sizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            val bytes = retriever.embeddedPicture ?: return null
            decodeSampled(bytes, sizePx)
        } catch (e: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    /** Reads the bounds first, then decodes with the largest power-of-two sample that stays at or above [sizePx]. */
    private fun decodeSampled(bytes: ByteArray, sizePx: Int): Bitmap? {
        val target = max(sizePx, 1)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = max(bounds.outWidth, bounds.outHeight)
        if (longest <= 0) return null
        var sample = 1
        while (longest / (sample * 2) >= target) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    /** Scales down only when the longest side is more than twice [sizePx]. */
    private fun fit(bitmap: Bitmap, sizePx: Int): Bitmap {
        val target = max(sizePx, 1)
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= target * 2) return bitmap
        val scale = target.toFloat() / longest
        val width = max(1, (bitmap.width * scale).roundToInt())
        val height = max(1, (bitmap.height * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled !== bitmap) bitmap.recycle()
        return scaled
    }
}
