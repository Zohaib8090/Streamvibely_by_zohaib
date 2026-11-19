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
import kotlin.coroutines.cancellation.CancellationException

@Serializable
internal data class AudiusUser(val name: String? = null)

@Serializable
internal data class AudiusArtwork(val `480x480`: String? = null, val `150x150`: String? = null)

@Serializable
internal data class AudiusTrack(
    val id: String,
    val title: String? = null,
    val duration: Int = 0,
    val user: AudiusUser? = null,
    val artwork: AudiusArtwork? = null,
    val is_streamable: Boolean = true,
)

@Serializable
internal data class AudiusSearch(val data: List<AudiusTrack> = emptyList())

/**
 * Audius (https://audius.co): artist-uploaded music with a free public API that allows streaming.
 * No account or key is needed; `app_name` identifies this app. Only tracks the API marks streamable are returned.
 */
class AudiusProvider(
    private val client: HttpClient = defaultClient(),
    private val baseUrl: String = "https://api.audius.co/v1",
) : MusicProvider {
    override val id = "audius"
    override val displayName = "Audius"

    override suspend fun search(query: String): List<Track> = try {
        val response = client.get("$baseUrl/tracks/search") {
            parameter("query", query)
            parameter("limit", 25)
            parameter("app_name", APP_NAME)
        }
        if (!response.status.isSuccess()) emptyList()
        else response.body<AudiusSearch>().data
            .filter { it.is_streamable && !it.title.isNullOrBlank() }
            .map {
                Track(
                    id = "audius:${it.id}",
                    title = it.title!!,
                    artist = it.user?.name ?: "Unknown artist",
                    durationMs = it.duration * 1000L,
                    artworkUrl = it.artwork?.`480x480` ?: it.artwork?.`150x150`,
                    source = id,
                    uri = streamUrl(it.id),
                )
            }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        emptyList()
    }

    override suspend fun streams(track: Track): List<StreamCandidate> =
        track.uri?.let { listOf(StreamCandidate(it, AudioQuality.High, 320, "audio/mpeg")) } ?: emptyList()

    private fun streamUrl(trackId: String) = "$baseUrl/tracks/$trackId/stream?app_name=$APP_NAME"

    private companion object { const val APP_NAME = "Streamvibely" }
}
