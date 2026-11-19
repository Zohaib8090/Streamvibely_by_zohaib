package app.streamvibely.lyrics

import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.LyricsProvider
import app.streamvibely.domain.Track
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs

/** HTTP client with JSON support. The engine is picked per platform from the classpath. */
fun defaultClient(): HttpClient = HttpClient {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
}

@Serializable
internal data class LrclibResult(
    val trackName: String? = null,
    val artistName: String? = null,
    val duration: Double? = null,
    val instrumental: Boolean = false,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null,
)

class LrclibProvider(
    private val client: HttpClient = defaultClient(),
    private val baseUrl: String = "https://lrclib.net/api",
) : LyricsProvider {
    override val id = "lrclib"
    override val displayName = "LRCLIB"

    override suspend fun fetch(track: Track): Lyrics? = try {
        val seconds = track.durationMs.takeIf { it > 0 }?.div(1000)
        val direct = request("$baseUrl/get", track, withAlbum = true, seconds)
        when {
            direct.status.isSuccess() -> direct.body<LrclibResult>().toLyrics()
            direct.status == HttpStatusCode.NotFound -> search(track, seconds)
            else -> null
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }

    private suspend fun search(track: Track, seconds: Long?): Lyrics? {
        val response = request("$baseUrl/search", track, withAlbum = false, null)
        if (!response.status.isSuccess()) return null
        val usable = response.body<List<LrclibResult>>().filter { it.toLyrics() != null }
        val best = if (seconds == null) usable.firstOrNull()
        else usable.minByOrNull { abs((it.duration ?: Double.MAX_VALUE) - seconds) }
        return best?.toLyrics()
    }

    private suspend fun request(url: String, track: Track, withAlbum: Boolean, seconds: Long?): HttpResponse =
        client.get(url) {
            header(HttpHeaders.UserAgent, "Streamvibely/0.1")
            parameter("track_name", track.title)
            parameter("artist_name", track.artist)
            if (withAlbum) track.album?.takeIf { it.isNotBlank() }?.let { parameter("album_name", it) }
            if (seconds != null) parameter("duration", seconds)
        }

    private fun LrclibResult.toLyrics(): Lyrics? {
        if (instrumental) return null
        val synced = syncedLyrics?.takeIf { it.isNotBlank() }?.let(LrcParser::parse).orEmpty()
        return when {
            synced.isNotEmpty() -> Lyrics.Synced("LRCLIB", synced)
            !plainLyrics.isNullOrBlank() -> Lyrics.Plain("LRCLIB", plainLyrics)
            else -> null
        }
    }
}
