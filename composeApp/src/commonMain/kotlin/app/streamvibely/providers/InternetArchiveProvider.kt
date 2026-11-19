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
import io.ktor.http.encodeURLPath
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.cancellation.CancellationException

@Serializable
internal data class IaDoc(
    val identifier: String,
    val title: JsonElement? = null,
    val creator: JsonElement? = null,
)

@Serializable
internal data class IaResponse(val docs: List<IaDoc> = emptyList())

@Serializable
internal data class IaSearch(val response: IaResponse = IaResponse())

@Serializable
internal data class IaFile(
    val name: String,
    val format: String? = null,
    val length: String? = null,
    val source: String? = null,
)

@Serializable
internal data class IaMetadata(val files: List<IaFile> = emptyList())

private fun JsonElement?.text(): String? =
    (this as? JsonPrimitive)?.contentOrNull

/**
 * Internet Archive audio (https://archive.org), the Open Source Audio collection: freely shared
 * recordings. The first playable MP3 of each item is used. No account or key is needed.
 */
class InternetArchiveProvider(
    private val client: HttpClient = defaultClient(),
    private val baseUrl: String = "https://archive.org",
) : MusicProvider {
    override val id = "archive"
    override val displayName = "Internet Archive"

    override suspend fun search(query: String): List<Track> = try {
        val response = client.get("$baseUrl/advancedsearch.php") {
            parameter("q", "($query) AND collection:opensource_audio AND mediatype:audio")
            parameter("fl[]", "identifier")
            parameter("fl[]", "title")
            parameter("fl[]", "creator")
            parameter("rows", 12)
            parameter("output", "json")
        }
        if (!response.status.isSuccess()) emptyList()
        else coroutineScope {
            response.body<IaSearch>().response.docs
                .map { doc -> async { resolve(doc) } }
                .awaitAll()
                .filterNotNull()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        emptyList()
    }

    /** Looks up the item's files and builds a track from its first MP3, or null when it has none. */
    private suspend fun resolve(doc: IaDoc): Track? = try {
        val meta = client.get("$baseUrl/metadata/${doc.identifier}")
        if (!meta.status.isSuccess()) null else {
            val file = meta.body<IaMetadata>().files
                .filter { it.format?.contains("MP3", ignoreCase = true) == true }
                .let { mp3 -> mp3.firstOrNull { it.source == "original" } ?: mp3.firstOrNull() }
            file?.let {
                Track(
                    id = "ia:${doc.identifier}",
                    title = doc.title.text() ?: doc.identifier,
                    artist = doc.creator.text() ?: "Internet Archive",
                    durationMs = it.length?.toDoubleOrNull()?.times(1000)?.toLong() ?: 0L,
                    artworkUrl = "$baseUrl/services/img/${doc.identifier}",
                    source = id,
                    uri = "$baseUrl/download/${doc.identifier}/${it.name.encodeURLPath()}",
                    canDownload = true,
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }

    override suspend fun streams(track: Track): List<StreamCandidate> =
        track.uri?.let { listOf(StreamCandidate(it, AudioQuality.Standard, null, "audio/mpeg")) } ?: emptyList()
}
