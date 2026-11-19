package app.streamvibely.providers

import app.streamvibely.domain.AudioQuality
import app.streamvibely.domain.MusicProvider
import app.streamvibely.domain.StreamCandidate
import app.streamvibely.domain.Track
import app.streamvibely.lyrics.defaultClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.coroutines.cancellation.CancellationException

/** Turns a YouTube video into an audio URL the player can open, or null. */
fun interface StreamResolver {
    suspend fun resolve(track: Track): String?
    suspend fun resolveCandidate(track: Track): StreamCandidate? =
        resolve(track)?.let { StreamCandidate(it, AudioQuality.High) }
    suspend fun resolveCandidate(track: Track, maxBitrateKbps: Int?): StreamCandidate? =
        resolveCandidate(track)
}

@Serializable internal data class YtThumb(val url: String? = null)
@Serializable internal data class YtThumbs(val high: YtThumb? = null, val medium: YtThumb? = null, val default: YtThumb? = null)
@Serializable internal data class YtSnippet(val title: String? = null, val channelTitle: String? = null, val thumbnails: YtThumbs? = null)
@Serializable internal data class YtId(val videoId: String? = null)
@Serializable internal data class YtItem(val id: YtId? = null, val snippet: YtSnippet? = null)
@Serializable internal data class YtSearch(val items: List<YtItem> = emptyList())

/**
 * YouTube Music provider supporting account-free search via YouTube's InnerTube API,
 * with optional fallback to the official YouTube Data API v3 when signed in.
 */
class YouTubeProvider(
    private val accessToken: (suspend () -> String?)? = null,
    private val resolver: StreamResolver? = null,
    /** True: only YouTube's Music category. False: every kind of video. */
    private val musicOnly: Boolean = true,
    private val client: HttpClient = defaultClient(),
) : MusicProvider {
    override val id = if (musicOnly) "youtube-music" else "youtube"
    override val displayName = if (musicOnly) "YouTube Music" else "YouTube"
    override val description = if (musicOnly) "Songs and music videos (YouTube Music)" else "All YouTube videos, including talks and tutorials"
    override val enabledByDefault = musicOnly
    override val requiresAccount = false
    override val canPlay get() = resolver != null

    override suspend fun search(query: String): List<Track> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return emptyList()

        // 1. Primary: Account-free InnerTube Music API search (unlimited, no OAuth token required)
        val innerTubeResults = searchInnerTube(trimmed)
        if (innerTubeResults.isNotEmpty()) {
            return innerTubeResults
        }

        // 2. Fallback: Official YouTube Data API if signed in
        val token = accessToken?.invoke()
        if (token != null) {
            val officialResults = searchOfficialApi(trimmed, token)
            if (officialResults.isNotEmpty()) {
                return officialResults
            }
        }

        return emptyList()
    }

    private suspend fun searchInnerTube(query: String): List<Track> = try {
        val requestBody = buildJsonObject {
            put("context", buildJsonObject {
                put("client", buildJsonObject {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", "1.20240101.01.00")
                    put("hl", "en")
                    put("gl", "US")
                })
            })
            put("query", query)
            // Songs filter
            if (musicOnly) {
                put("params", "EgWKAQIIAWoKEAkQBRAKEAMQBA==")
            }
        }

        val response = client.post("https://music.youtube.com/youtubei/v1/search?prettyPrint=false") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.UserAgent, YOUTUBE_WEB_USER_AGENT)
            header("Origin", "https://music.youtube.com")
            header("Referer", "https://music.youtube.com/")
            setBody(requestBody.toString())
        }

        if (!response.status.isSuccess()) {
            emptyList()
        } else {
            val bodyString = response.body<String>()
            val root = Json.parseToJsonElement(bodyString)
            parseInnerTubeSearchResults(root, id)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        emptyList()
    }

    private suspend fun searchOfficialApi(query: String, token: String): List<Track> = try {
        val response = client.get("https://www.googleapis.com/youtube/v3/search") {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("part", "snippet")
            parameter("type", "video")
            if (musicOnly) parameter("videoCategoryId", "10")
            parameter("maxResults", 20)
            parameter("q", query)
        }
        if (!response.status.isSuccess()) emptyList()
        else response.body<YtSearch>().items.mapNotNull { item ->
            val videoId = item.id?.videoId ?: return@mapNotNull null
            val s = item.snippet ?: return@mapNotNull null
            Track(
                id = "yt:$videoId",
                title = unescape(s.title ?: return@mapNotNull null),
                artist = unescape(s.channelTitle.orEmpty()).removeSuffix(" - Topic").ifBlank { "YouTube" },
                artworkUrl = s.thumbnails?.high?.url ?: s.thumbnails?.medium?.url ?: s.thumbnails?.default?.url,
                source = id,
                uri = null,
            )
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        emptyList()
    }

    override suspend fun streams(track: Track): List<StreamCandidate> = streams(track, null)

    override suspend fun streams(track: Track, maxBitrateKbps: Int?): List<StreamCandidate> {
        val candidate = resolver?.resolveCandidate(track, maxBitrateKbps)
            ?: resolver?.resolveCandidate(track)
            ?: resolver?.resolve(track)?.let { StreamCandidate(it, AudioQuality.High) }
            ?: return emptyList()
        return listOf(candidate)
    }

    companion object {
        private const val YOUTUBE_WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        private fun parseInnerTubeSearchResults(root: JsonElement, providerId: String): List<Track> {
            val renderers = mutableListOf<JsonObject>()
            collectRenderers(root, renderers)

            return renderers.mapNotNull { renderer ->
                val videoId = renderer["playlistItemData"]?.jsonObject?.get("videoId")?.jsonPrimitive?.contentOrNull
                    ?: findFirstString(renderer, "videoId")
                    ?: return@mapNotNull null

                val flexColumns = renderer["flexColumns"]?.jsonArray
                val title = flexColumns?.getOrNull(0)?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject?.get("runs")?.jsonArray
                    ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                    ?.trim()?.takeIf { it.isNotBlank() }
                    ?: renderer["title"]?.jsonObject?.get("runs")?.jsonArray
                        ?.joinToString("") { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull.orEmpty() }
                        ?.trim()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null

                val runsCol2 = flexColumns?.getOrNull(1)?.jsonObject
                    ?.get("musicResponsiveListItemFlexColumnRenderer")?.jsonObject
                    ?.get("text")?.jsonObject?.get("runs")?.jsonArray

                val artist = runsCol2?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: "YouTube Music"

                val album = if ((runsCol2?.size ?: 0) >= 3) {
                    runsCol2?.getOrNull(2)?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                } else null

                val durationText = runsCol2?.lastOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.contentOrNull
                val durationMs = parseDurationMs(durationText)

                val thumbnails = renderer["thumbnail"]?.jsonObject
                    ?.get("musicThumbnailRenderer")?.jsonObject
                    ?.get("thumbnail")?.jsonObject?.get("thumbnails")?.jsonArray
                val rawThumb = thumbnails?.lastOrNull()?.jsonObject?.get("url")?.jsonPrimitive?.contentOrNull
                val artworkUrl = rawThumb?.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")

                Track(
                    id = "yt:$videoId",
                    title = unescape(title),
                    artist = unescape(artist).removeSuffix(" - Topic"),
                    album = album?.let(::unescape),
                    durationMs = durationMs ?: 0L,
                    artworkUrl = artworkUrl,
                    source = providerId,
                    uri = null,
                )
            }.distinctBy { it.id }
        }

        private fun collectRenderers(element: JsonElement, out: MutableList<JsonObject>) {
            when (element) {
                is JsonObject -> {
                    for ((key, value) in element) {
                        if (key == "musicResponsiveListItemRenderer" && value is JsonObject) {
                            out.add(value)
                        } else {
                            collectRenderers(value, out)
                        }
                    }
                }
                is JsonArray -> {
                    for (child in element) {
                        collectRenderers(child, out)
                    }
                }
                else -> Unit
            }
        }

        private fun findFirstString(element: JsonElement, targetKey: String): String? {
            when (element) {
                is JsonObject -> {
                    element[targetKey]?.jsonPrimitive?.contentOrNull?.let { return it }
                    for ((_, value) in element) {
                        findFirstString(value, targetKey)?.let { return it }
                    }
                }
                is JsonArray -> {
                    for (child in element) {
                        findFirstString(child, targetKey)?.let { return it }
                    }
                }
                else -> Unit
            }
            return null
        }

        private fun parseDurationMs(text: String?): Long? {
            if (text.isNullOrBlank()) return null
            val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
            return when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000L
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                else -> null
            }
        }

        private fun unescape(text: String) = text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
