package app.streamvibely.playback.resolve

import android.content.Context
import android.net.Uri
import android.util.Log
import app.streamvibely.domain.Track
import app.streamvibely.playback.ChunkedRangeDataSource
import app.streamvibely.providers.StreamResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level YouTube stream resolver coordinating InnerTubeX (with Faraday player configs),
 * NewPipe fallback, in-memory stream caching, and seamless URL replacement on HTTP 403/410.
 */
class YouTubeStreamResolver(
    private val context: Context,
) : StreamResolver, ChunkedRangeDataSource.StreamRefresher {

    private val innerTubeX = InnerTubeXStreamExtractor(context)
    private val newPipe = YouTubeStreamExtractor()

    private val cache = ConcurrentHashMap<String, YouTubeAudioStream>()
    private val activeResolves = ConcurrentHashMap<String, Mutex>()

    /** Maps currently playing/cached stream URLs to their videoId for seamless refresh. */
    private val urlToVideoId = ConcurrentHashMap<String, String>()

    override suspend fun resolve(track: Track): String? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(track.id) ?: return@withContext null
        resolveStream(videoId)?.url
    }

    override suspend fun resolveCandidate(track: Track): app.streamvibely.domain.StreamCandidate? =
        resolveCandidate(track, null)

    override suspend fun resolveCandidate(
        track: Track,
        maxBitrateKbps: Int?,
    ): app.streamvibely.domain.StreamCandidate? = withContext(Dispatchers.IO) {
        val videoId = extractVideoId(track.id) ?: return@withContext null
        val maxBps = maxBitrateKbps?.let { it * 1000 }
        val stream = resolveStream(videoId, maxBitrateBps = maxBps) ?: return@withContext null
        val bitrateKbps = if (stream.bitrate > 0) stream.bitrate / 1000 else null
        val quality = when {
            stream.bitrate >= 200_000 -> app.streamvibely.domain.AudioQuality.High
            stream.bitrate >= 100_000 -> app.streamvibely.domain.AudioQuality.Standard
            else -> app.streamvibely.domain.AudioQuality.DataSaver
        }
        app.streamvibely.domain.StreamCandidate(
            url = stream.url,
            quality = quality,
            bitrateKbps = bitrateKbps,
            mimeType = stream.mimeType,
        )
    }

    suspend fun resolveStream(
        videoId: String,
        maxBitrateBps: Int? = null,
        forceRefresh: Boolean = false,
    ): YouTubeAudioStream? = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = cache[videoId]
            if (cached != null && cached.isFresh()) {
                return@withContext cached
            }
        }

        val mutex = activeResolves.computeIfAbsent(videoId) { Mutex() }
        mutex.withLock {
            if (!forceRefresh) {
                val cached = cache[videoId]
                if (cached != null && cached.isFresh()) {
                    return@withContext cached
                }
            }

            Log.d(TAG, "Resolving audio stream for $videoId")
            // 1. Try InnerTubeX (reflection + Faraday player configs)
            var stream: YouTubeAudioStream? = try {
                innerTubeX.resolve(videoId = videoId, maxBitrateBps = maxBitrateBps)
            } catch (e: Exception) {
                Log.w(TAG, "InnerTubeX attempt failed for $videoId", e)
                null
            }

            // 2. Fall back to NewPipe extractor if InnerTubeX was null
            if (stream == null) {
                Log.d(TAG, "Falling back to NewPipeExtractor for $videoId")
                stream = try {
                    newPipe.resolveAudioStream(videoId = videoId, maxBitrateBps = maxBitrateBps)
                } catch (e: Exception) {
                    Log.w(TAG, "NewPipe fallback failed for $videoId", e)
                    null
                }
            }

            if (stream != null) {
                cache[videoId] = stream
                urlToVideoId[stream.url] = videoId
                Log.i(TAG, "Successfully resolved $videoId via ${stream.clientProfile} (${stream.bitrate / 1000} kbps)")
            }
            stream
        }
    }

    /**
     * Seamless mid-song URL swap invoked by [ChunkedRangeDataSource] when a chunk returns HTTP 403 or 410.
     */
    override fun refresh(failedUri: Uri): ChunkedRangeDataSource.Replacement? {
        val failedUrl = failedUri.toString()
        val videoId = urlToVideoId[failedUrl] ?: return null
        Log.i(TAG, "Refreshing refused YouTube URL for $videoId (HTTP 403/410)")

        val previous = cache[videoId]
        innerTubeX.reportPlaybackFailure(videoId, previous?.clientProfile)

        val fresh = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            runCatching {
                resolveStream(videoId = videoId, forceRefresh = true)
            }.getOrNull()
        } ?: return null

        urlToVideoId[fresh.url] = videoId
        val headers = fresh.requestHeaders + ("User-Agent" to fresh.userAgent)
        return ChunkedRangeDataSource.Replacement(Uri.parse(fresh.url), headers)
    }

    private fun extractVideoId(rawId: String): String? {
        val trimmed = rawId.trim()
        val withoutPrefix = when {
            trimmed.startsWith("yt:") -> trimmed.removePrefix("yt:")
            trimmed.startsWith("youtube:") -> trimmed.removePrefix("youtube:")
            else -> trimmed
        }
        return withoutPrefix.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "YouTubeStreamResolver"
    }
}

