package app.streamvibely.providers

import app.streamvibely.domain.AudioQuality
import app.streamvibely.domain.MusicProvider
import app.streamvibely.domain.StreamCandidate
import app.streamvibely.domain.Track
import app.streamvibely.lyrics.defaultClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

@Serializable
internal data class JamendoTrack(
    val id: String,
    val name: String = "",
    val duration: Int = 0,
    val artist_name: String = "",
    val album_name: String? = null,
    val image: String? = null,
    val audio: String? = null,
    val audiodownload: String? = null,
    val audiodownload_allowed: Boolean = false,
    val license_ccurl: String? = null,
)

@Serializable
internal data class JamendoResponse(val results: List<JamendoTrack> = emptyList())

/** "https://creativecommons.org/licenses/by-nc-sa/3.0/" becomes "CC BY-NC-SA 3.0". */
internal fun ccLabel(url: String?): String? {
    val m = Regex("licenses/([a-z-]+)/([0-9.]+)").find(url ?: return null) ?: return null
    return "CC " + m.groupValues[1].uppercase() + " " + m.groupValues[2]
}

/**
 * Jamendo (https://www.jamendo.com): independent artists who share their music under Creative Commons
 * licenses. Free to use, but the API needs a free client ID from https://devportal.jamendo.com.
 * Without one this source returns nothing. Downloads are offered only where the artist allows them.
 */
class JamendoProvider(
    private val clientId: String,
    private val client: HttpClient = defaultClient(),
    private val baseUrl: String = "https://api.jamendo.com/v3.0",
) : MusicProvider {
    override val id = "jamendo"
    override val displayName = "Jamendo"
    override val description =
        if (clientId.isBlank()) "Free Creative Commons music. Needs a free client ID (see docs/custom-providers.md)."
        else "Free music from independent artists, shared under Creative Commons licenses."
    // Off until the user switches it on in Settings > Providers.
    override val enabledByDefault = false

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun search(query: String): List<Track> {
        if (clientId.isBlank() || query.isBlank()) return emptyList()
        return try {
            val response = client.get("$baseUrl/tracks/") {
                parameter("client_id", clientId)
                parameter("format", "json")
                parameter("limit", 20)
                parameter("search", query)
                parameter("audioformat", "mp32")
                parameter("audiodlformat", "mp32")
                parameter("imagesize", 300)
            }
            if (!response.status.isSuccess()) emptyList()
            else json.decodeFromString(JamendoResponse.serializer(), response.body<String>()).results.mapNotNull { t ->
                val url = t.audio?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Track(
                    id = "jamendo:${t.id}",
                    title = t.name,
                    artist = t.artist_name,
                    album = t.album_name?.takeIf { it.isNotBlank() },
                    durationMs = t.duration * 1000L,
                    artworkUrl = t.image?.takeIf { it.isNotBlank() },
                    source = id,
                    uri = url,
                    canDownload = t.audiodownload_allowed,
                    qualityLabel = "MP3 VBR",
                    license = ccLabel(t.license_ccurl),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun streams(track: Track): List<StreamCandidate> =
        track.uri?.let { listOf(StreamCandidate(it, AudioQuality.High, null, "audio/mpeg")) } ?: emptyList()
}
