package app.streamvibely.artwork

import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Keeps at most [MAX_BITMAPS] decoded covers (owner decision D5) and remembers track ids that
 * have no artwork, so lists do not retry on every scroll. android.util.LruCache synchronizes its
 * own access, so this class is safe to call from any thread.
 */
internal class ArtworkCache {
    private val bitmaps = LruCache<String, ImageBitmap>(MAX_BITMAPS)
    private val misses = LruCache<String, Boolean>(MAX_MISSES)

    fun bitmap(key: String): ImageBitmap? = bitmaps.get(key)

    fun putBitmap(key: String, image: ImageBitmap) {
        bitmaps.put(key, image)
    }

    fun isMiss(trackId: String): Boolean = misses.get(trackId) == true

    fun putMiss(trackId: String) {
        misses.put(trackId, true)
    }

    companion object {
        const val MAX_BITMAPS = 24
        const val MAX_MISSES = 512
    }
}
