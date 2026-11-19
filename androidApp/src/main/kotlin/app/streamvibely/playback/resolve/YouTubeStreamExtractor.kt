package app.streamvibely.playback.resolve

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Resolves YouTube audio streams via NewPipeExtractor as a reliable fallback.
 */
class YouTubeStreamExtractor(
    private val http: OkHttpClient = defaultClient(),
) {
    private val downloader = OkHttpNewPipeDownloader(http)

    @Volatile
    private var initialized = false

    private fun initialize() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    NewPipe.init(downloader)
                    initialized = true
                }
            }
        }
    }

    suspend fun resolveAudioStream(
        videoId: String,
        maxBitrateBps: Int? = null,
    ): YouTubeAudioStream = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        initialize()
        val info = try {
            StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$videoId")
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            throw IOException("NewPipe stream extraction failed for $videoId", error)
        }

        val ordered = info.audioStreams.sortedWith(
            compareBy<AudioStream> { isDrcAudioUrl(it.content.orEmpty()) }
                .thenBy { audioFormatPreference(it) }
                .thenByDescending { maxOf(it.averageBitrate, it.bitrate) },
        )
        if (ordered.isEmpty()) {
            throw IOException("YouTube returned no audio streams for $videoId")
        }

        val candidates = ordered.map { stream ->
            stream.toYouTubeAudioStream(videoId, info.duration, now)
        }.filter { stream ->
            maxBitrateBps == null || stream.bitrate <= maxBitrateBps
        }
        if (candidates.isEmpty()) {
            throw IOException("No audio streams within requested bitrate for $videoId")
        }

        for (candidate in candidates.take(MAX_PROBED_STREAMS)) {
            if (probePlayable(candidate)) return@withContext candidate
        }

        // Return highest bitrate candidate if probing fails but stream exists
        candidates.first()
    }

    private fun AudioStream.toYouTubeAudioStream(
        videoId: String,
        durationSeconds: Long,
        now: Long,
    ): YouTubeAudioStream {
        val reportedBitrate = maxOf(averageBitrate, bitrate)
        val normalizedBitrate = if (reportedBitrate in 1..9_999) reportedBitrate * 1_000 else reportedBitrate
        val url = content.orEmpty()
        val expiresAt = url.toHttpUrlOrNull()
            ?.queryParameter("expire")
            ?.toLongOrNull()
            ?.times(1_000L)
            ?: (now + UNKNOWN_EXPIRY_TTL_MS)

        return YouTubeAudioStream(
            videoId = videoId,
            url = url,
            itag = itag.takeIf { it >= 0 },
            mimeType = format?.mimeType,
            codec = codec?.takeIf(String::isNotBlank),
            bitrate = normalizedBitrate,
            sampleRateHz = itagItem?.sampleRate?.takeIf { it > 0 },
            durationMs = itagItem?.approxDurationMs?.takeIf { it > 0 }
                ?: durationSeconds.takeIf { it > 0 }?.times(1_000L),
            contentLength = itagItem?.contentLength?.takeIf { it > 0 },
            isAdaptive = true,
            clientProfile = "NEWPIPE",
            requestHeaders = mapOf(
                "User-Agent" to YOUTUBE_WEB_USER_AGENT,
                "Origin" to YOUTUBE_ORIGIN,
                "Referer" to "$YOUTUBE_ORIGIN/watch?v=$videoId",
            ),
            expiresAtEpochMs = expiresAt,
            userAgent = YOUTUBE_WEB_USER_AGENT,
            isDrc = isDrcAudioUrl(url),
        )
    }

    private fun probePlayable(stream: YouTubeAudioStream): Boolean = try {
        val request = okhttp3.Request.Builder()
            .url(stream.url)
            .header("Range", "bytes=0-1023")
            .header("User-Agent", stream.userAgent)
            .apply {
                stream.requestHeaders.forEach { (k, v) -> header(k, v) }
            }
            .build()
        http.newCall(request).execute().use { response ->
            response.isSuccessful || response.code == 206
        }
    } catch (_: Exception) {
        false
    }

    private fun audioFormatPreference(stream: AudioStream): Int {
        val mime = stream.format?.mimeType.orEmpty().lowercase()
        return when {
            mime.contains("webm") || mime.contains("opus") -> 0
            mime.contains("mp4") || mime.contains("m4a") -> 1
            else -> 2
        }
    }

    private fun isDrcAudioUrl(url: String): Boolean {
        val xtags = url.substringAfter("xtags=", "").substringBefore('&')
        if (xtags.isEmpty()) return false
        return runCatching { java.net.URLDecoder.decode(xtags, "UTF-8") }.getOrDefault(xtags).contains("drc=1")
    }

    private class OkHttpNewPipeDownloader(
        private val http: OkHttpClient,
    ) : Downloader() {
        override fun execute(request: Request): Response {
            val headers = request.headers()
            val requestBuilder = okhttp3.Request.Builder()
                .url(request.url())
                .method(request.httpMethod(), request.dataToSend()?.toRequestBody())

            headers.forEach { (name, values) ->
                values.forEach { value -> requestBuilder.addHeader(name, value) }
            }
            if (headers.keys.none { it.equals("User-Agent", ignoreCase = true) }) {
                requestBuilder.header("User-Agent", YOUTUBE_WEB_USER_AGENT)
            }
            if (headers.keys.none { it.equals("Accept-Language", ignoreCase = true) }) {
                requestBuilder.header("Accept-Language", "en-US,en;q=0.9")
            }
            val origin = if (request.url().contains("music.youtube.com", ignoreCase = true)) {
                "https://music.youtube.com"
            } else {
                YOUTUBE_ORIGIN
            }
            if (headers.keys.none { it.equals("Origin", ignoreCase = true) }) {
                requestBuilder.header("Origin", origin)
            }
            if (headers.keys.none { it.equals("Referer", ignoreCase = true) }) {
                requestBuilder.header("Referer", "$origin/")
            }

            return http.newCall(requestBuilder.build()).execute().use { response ->
                Response(
                    response.code,
                    response.message,
                    response.headers.names().associateWith(response.headers::values),
                    response.body?.string().orEmpty(),
                    response.request.url.toString(),
                )
            }
        }
    }

    companion object {
        const val YOUTUBE_ORIGIN = "https://www.youtube.com"
        const val YOUTUBE_WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        private const val UNKNOWN_EXPIRY_TTL_MS = 5 * 60 * 1000L
        private const val MAX_PROBED_STREAMS = 3

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}

