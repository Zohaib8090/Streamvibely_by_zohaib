package app.streamvibely.playback.resolve

data class YouTubeAudioStream(
    val videoId: String,
    val url: String,
    val itag: Int? = null,
    val mimeType: String? = null,
    val codec: String? = null,
    val bitrate: Int = 0,
    val sampleRateHz: Int? = null,
    val durationMs: Long? = null,
    val contentLength: Long? = null,
    val isAdaptive: Boolean = false,
    val clientProfile: String = "UNKNOWN",
    val requestHeaders: Map<String, String> = emptyMap(),
    val expiresAtEpochMs: Long? = null,
    val userAgent: String = DEFAULT_USER_AGENT,
    val isDrc: Boolean = false,
) {
    fun isFresh(nowEpochMs: Long = System.currentTimeMillis()): Boolean =
        expiresAtEpochMs == null || (expiresAtEpochMs - nowEpochMs) > EXPIRY_SAFETY_BUFFER_MS

    companion object {
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        private const val EXPIRY_SAFETY_BUFFER_MS = 30_000L
    }
}

