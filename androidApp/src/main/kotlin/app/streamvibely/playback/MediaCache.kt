package app.streamvibely.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object MediaCache {
    private const val MAX_CACHE_BYTES = 512 * 1024 * 1024L // 512 MB
    private const val CACHE_DIR_NAME = "audio_stream_cache"

    @Volatile
    private var instance: SimpleCache? = null

    fun get(context: Context): SimpleCache {
        val existing = instance
        if (existing != null) return existing
        return synchronized(this) {
            instance ?: run {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES)
                val dbProvider = StandaloneDatabaseProvider(context.applicationContext)
                SimpleCache(cacheDir, evictor, dbProvider).also { instance = it }
            }
        }
    }

    fun getCacheSize(context: Context): Long {
        val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) return 0L
        return runCatching {
            cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }.getOrDefault(0L)
    }

    fun clear(context: Context) {
        synchronized(this) {
            runCatching { instance?.release() }
            instance = null
            val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
            cacheDir.deleteRecursively()
        }
    }
}

