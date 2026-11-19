package app.streamvibely.spotify

import android.content.Context
import android.net.Uri
import android.util.Log
import app.streamvibely.domain.PlaylistRepository
import app.streamvibely.domain.SpotifyPlaylist
import app.streamvibely.domain.SpotifySyncRepository
import app.streamvibely.domain.SpotifySyncState
import app.streamvibely.domain.SpotifyTrack
import app.streamvibely.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Android implementation of SpotifySyncRepository using OkHttp, TOTP handshake,
 * and Spotify's native Pathfinder GraphQL API.
 */
class AndroidSpotifySyncRepository(
    context: Context,
) : SpotifySyncRepository {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("spotify_session", Context.MODE_PRIVATE)
    // The session cookie is kept encrypted with an Android Keystore key; a plain copy from older builds is migrated.
    private val session = SecureSession(prefs, "sp_dc", legacyPlainKey = "sp_dc")
    private val tracksFile = File(appContext.filesDir, "spotify_synced_tracks.json")

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val tokenMutex = Mutex()

    private val _isConnected = MutableStateFlow(!session.get().isNullOrBlank())
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _syncState = MutableStateFlow(SpotifySyncState.Idle)
    override val syncState: StateFlow<SpotifySyncState> = _syncState

    private val _statusMessage = MutableStateFlow<String?>(null)
    override val statusMessage: StateFlow<String?> = _statusMessage

    private val _syncedTracks = MutableStateFlow<List<Track>>(loadPersistedTracks())
    override val syncedTracks: StateFlow<List<Track>> = _syncedTracks

    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var tokenExpiresAtMs: Long = 0L

    override suspend fun saveSession(spDcCookie: String): Boolean {
        val clean = spDcCookie.trim()
        if (clean.isBlank()) return false
        session.put(clean)
        _isConnected.value = true
        _statusMessage.value = "Authenticating with Spotify..."
        val token = getAccessToken(forceRefresh = true)
        val valid = !token.isNullOrBlank()
        if (!valid) {
            _statusMessage.value = "Failed to authenticate session with Spotify."
        } else {
            _statusMessage.value = "Connected to Spotify!"
        }
        return valid
    }

    override suspend fun disconnect() {
        session.clear()
        // Also drop the login web view's cookies so no old session is left on the phone.
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        }
        cachedAccessToken = null
        tokenExpiresAtMs = 0L
        _isConnected.value = false
        _statusMessage.value = "Disconnected from Spotify"
    }

    suspend fun getAccessToken(forceRefresh: Boolean = false): String? = tokenMutex.withLock {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceRefresh && cachedAccessToken != null && now < tokenExpiresAtMs - 60_000L) {
                return@withContext cachedAccessToken
            }

            val spDc = session.get() ?: return@withContext null
            val totp = computeTotp(spDc)
            if (totp == null) {
                Log.w(TAG, "Failed to compute TOTP for Spotify handshake")
                return@withContext null
            }

            val tokenUrl = "$SESSION_TOKEN_URL?reason=transport&productType=web-player&totp=${totp.otpClient}&totpServer=${totp.otpServer}&totpVer=${totp.version}"
            try {
                val req = Request.Builder()
                    .url(tokenUrl)
                    .header("User-Agent", USER_AGENT)
                    .header("Cookie", "sp_dc=$spDc")
                    .header("Accept", "application/json")
                    .header("App-platform", "WebPlayer")
                    .header("Spotify-App-Version", SPOTIFY_APP_VERSION)
                    .header("Origin", SPOTIFY_ORIGIN)
                    .header("Referer", "$SPOTIFY_ORIGIN/")
                    .build()

                http.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        Log.w(TAG, "Token exchange failed: HTTP ${resp.code}")
                        return@withContext null
                    }
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val token = json.optString("accessToken").takeIf { it.isNotBlank() }
                    val isAnonymous = json.optBoolean("isAnonymous", false)
                    if (token != null && !isAnonymous && token.length >= 100) {
                        val expiresMs = json.optLong("accessTokenExpirationTimestampMs", now + 3600_000L)
                        cachedAccessToken = token
                        tokenExpiresAtMs = expiresMs
                        Log.i(TAG, "Acquired fresh Spotify access token (len: ${token.length})")
                        return@withContext token
                    } else {
                        Log.w(TAG, "Token response was anonymous or too short: isAnonymous=$isAnonymous, len=${token?.length}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Token request error: ${e.message}")
            }
            null
        }
    }

    private data class TotpResult(val otpClient: String, val otpServer: String, val version: Int)

    private suspend fun computeTotp(spDc: String): TotpResult? = withContext(Dispatchers.IO) {
        val (secretValue, version) = fetchSecret() ?: return@withContext null
        val plainBytes = secretValue.map { it.code }

        val secretKeyString = buildString {
            for ((index, byteVal) in plainBytes.withIndex()) {
                val decoded = byteVal xor ((index % 33) + 9)
                append(decoded.toString())
            }
        }
        val secretKeyBytes = secretKeyString.toByteArray(Charsets.US_ASCII)

        val serverTime = fetchServerTime(spDc) ?: (System.currentTimeMillis() / 1000L)
        val clientTime = System.currentTimeMillis() / 1000L

        val otpClient = generateOtp(secretKeyBytes, clientTime)
        val otpServer = generateOtp(secretKeyBytes, serverTime)
        TotpResult(otpClient, otpServer, version)
    }

    private fun generateOtp(secretKeyBytes: ByteArray, timestampSeconds: Long): String {
        val counter = timestampSeconds / 30L
        val counterBytes = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(counter).array()
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secretKeyBytes, "HmacSHA1"))
        val hmacResult = mac.doFinal(counterBytes)

        val offset = hmacResult[hmacResult.size - 1].toInt() and 0x0F
        val binary = ((hmacResult[offset].toInt() and 0x7F) shl 24) or
            ((hmacResult[offset + 1].toInt() and 0xFF) shl 16) or
            ((hmacResult[offset + 2].toInt() and 0xFF) shl 8) or
            (hmacResult[offset + 3].toInt() and 0xFF)

        return (binary % 1_000_000).toString().padStart(6, '0')
    }

    private fun fetchServerTime(spDc: String): Long? {
        val req = Request.Builder()
            .url(SERVER_TIME_URL)
            .header("User-Agent", USER_AGENT)
            .header("Cookie", "sp_dc=$spDc")
            .header("Accept", "application/json")
            .header("App-platform", "WebPlayer")
            .header("Spotify-App-Version", SPOTIFY_APP_VERSION)
            .header("Origin", SPOTIFY_ORIGIN)
            .header("Referer", "$SPOTIFY_ORIGIN/")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    json.optLong("serverTime").takeIf { it > 0 }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchSecret(): Pair<String, Int>? {
        // Fallback to published secretDict
        val req = Request.Builder()
            .url(SECRETS_DICT_URL)
            .header("User-Agent", USER_AGENT)
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) null
                else {
                    val body = resp.body?.string().orEmpty()
                    val json = JSONObject(body)
                    var maxVer = 0
                    var latestKey: String? = null
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val v = k.toIntOrNull() ?: continue
                        if (v > maxVer) {
                            maxVer = v
                            latestKey = k
                        }
                    }
                    if (latestKey == null) return null
                    val arr = json.optJSONArray(latestKey) ?: return null
                    val chars = CharArray(arr.length()) { arr.getInt(it).toChar() }
                    Pair(String(chars), maxVer)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch secretDict: ${e.message}")
            null
        }
    }

    override suspend fun fetchLikedSongs(): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val results = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 50

        while (true) {
            val variables = JSONObject().apply {
                put("offset", offset)
                put("limit", limit)
            }.toString()

            val extensions = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", HASH_FETCH_LIBRARY_TRACKS)
                })
            }.toString()

            val url = "$PATHFINDER_ENDPOINT?operationName=fetchLibraryTracks&variables=${Uri.encode(variables)}&extensions=${Uri.encode(extensions)}"
            val json = pathfinderGet(url, token) ?: break

            val tracksObj = json.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("library")?.optJSONObject("tracks") ?: break
            val items = tracksObj.optJSONArray("items") ?: break
            if (items.length() == 0) break

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val trackData = item.optJSONObject("track")?.optJSONObject("data")
                    ?: item.optJSONObject("track")
                    ?: continue
                val track = parsePathfinderTrack(trackData)
                if (track != null) results.add(track)
            }

            offset += items.length()
            val total = tracksObj.optInt("totalCount", 0)
            if (offset >= total || items.length() < limit) break
        }
        results
    }

    override suspend fun fetchPlaylists(): List<SpotifyPlaylist> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val results = mutableListOf<SpotifyPlaylist>()

        val variables = JSONObject().apply {
            put("filters", JSONArray().put("Playlists"))
            put("order", JSONObject.NULL)
            put("textFilter", "")
            put("features", JSONArray().put("LIKED_SONGS"))
            put("limit", 50)
            put("offset", 0)
            put("flatten", false)
        }.toString()

        val extensions = JSONObject().apply {
            put("persistedQuery", JSONObject().apply {
                put("version", 1)
                put("sha256Hash", HASH_LIBRARY_V3)
            })
        }.toString()

        val url = "$PATHFINDER_ENDPOINT?operationName=libraryV3&variables=${Uri.encode(variables)}&extensions=${Uri.encode(extensions)}"
        val json = pathfinderGet(url, token) ?: return@withContext emptyList()

        val items = json.optJSONObject("data")?.optJSONObject("me")?.optJSONObject("libraryV3")?.optJSONArray("items") ?: return@withContext emptyList()
        for (i in 0 until items.length()) {
            val item = items.optJSONObject(i) ?: continue
            val dataItem = item.optJSONObject("item")?.optJSONObject("data") ?: continue
            val uri = dataItem.optString("uri")
            val name = dataItem.optString("name")
            if (uri.startsWith("spotify:collection:")) continue // Liked songs handled separately
            val id = uri.removePrefix("spotify:playlist:")
            if (id.isBlank() || name.isBlank()) continue

            val desc = dataItem.optString("description")
            val images = dataItem.optJSONObject("images")?.optJSONArray("items")
            val art = images?.optJSONObject(0)?.optJSONArray("sources")?.optJSONObject(0)?.optString("url")

            results.add(SpotifyPlaylist(id, name, desc, 0, art))
        }
        results
    }

    override suspend fun fetchPlaylistTracks(playlistId: String): List<SpotifyTrack> = withContext(Dispatchers.IO) {
        val token = getAccessToken() ?: return@withContext emptyList()
        val results = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 100

        while (true) {
            val variables = JSONObject().apply {
                put("uri", "spotify:playlist:$playlistId")
                put("offset", offset)
                put("limit", limit)
                put("enableWatchFeedEntrypoint", false)
            }.toString()

            val extensions = JSONObject().apply {
                put("persistedQuery", JSONObject().apply {
                    put("version", 1)
                    put("sha256Hash", HASH_FETCH_PLAYLIST)
                })
            }.toString()

            val url = "$PATHFINDER_ENDPOINT?operationName=fetchPlaylist&variables=${Uri.encode(variables)}&extensions=${Uri.encode(extensions)}"
            val json = pathfinderGet(url, token) ?: break

            val playlistObj = json.optJSONObject("data")?.optJSONObject("playlistV2") ?: break
            val contentObj = playlistObj.optJSONObject("content") ?: break
            val items = contentObj.optJSONArray("items") ?: break
            if (items.length() == 0) break

            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val trackData = item.optJSONObject("itemV2")?.optJSONObject("data") ?: continue
                val track = parsePathfinderTrack(trackData)
                if (track != null) results.add(track)
            }

            offset += items.length()
            val total = contentObj.optInt("totalCount", 0)
            if (offset >= total || items.length() < limit) break
        }
        results
    }

    override suspend fun syncAll(playlistRepository: PlaylistRepository): Result<Int> = withContext(Dispatchers.IO) {
        _syncState.value = SpotifySyncState.Syncing
        _statusMessage.value = "Connecting to Spotify..."

        try {
            val token = getAccessToken(forceRefresh = true)
            if (token.isNullOrBlank()) {
                _syncState.value = SpotifySyncState.Error
                _statusMessage.value = "Failed to authenticate with Spotify. Try logging in again."
                return@withContext Result.failure(Exception("Spotify authentication failed"))
            }

            _statusMessage.value = "Fetching Liked Songs..."
            val liked = fetchLikedSongs()

            val allTracksMap = HashMap<String, Track>()
            _syncedTracks.value.forEach { allTracksMap[it.id] = it }

            var totalImported = 0

            // 1. Sync Liked Songs playlist
            if (liked.isNotEmpty()) {
                val existingPlaylists = playlistRepository.playlists.value
                val existingLiked = existingPlaylists.firstOrNull { it.name.equals("Spotify Liked Songs", ignoreCase = true) }
                val targetId = existingLiked?.id ?: playlistRepository.create("Spotify Liked Songs", "Synced from your Spotify library").id

                val trackIds = mutableListOf<String>()
                for (st in liked) {
                    val track = st.toStreamvibelyTrack()
                    allTracksMap[track.id] = track
                    trackIds.add(track.id)
                }
                playlistRepository.addTracks(targetId, trackIds)
                totalImported += liked.size
            }

            // 2. Sync User Playlists
            _statusMessage.value = "Fetching user playlists..."
            val playlists = fetchPlaylists()
            for ((index, spPlaylist) in playlists.withIndex()) {
                _statusMessage.value = "Syncing playlist ${index + 1}/${playlists.size}: ${spPlaylist.name}..."
                val pTracks = fetchPlaylistTracks(spPlaylist.id)
                if (pTracks.isEmpty()) continue

                val existing = playlistRepository.playlists.value.firstOrNull { it.name.equals(spPlaylist.name, ignoreCase = true) }
                val targetId = existing?.id ?: playlistRepository.create(spPlaylist.name, "Imported from Spotify: ${spPlaylist.description ?: ""}").id

                val trackIds = mutableListOf<String>()
                for (st in pTracks) {
                    val track = st.toStreamvibelyTrack()
                    allTracksMap[track.id] = track
                    trackIds.add(track.id)
                }
                playlistRepository.addTracks(targetId, trackIds)
                totalImported += pTracks.size
            }

            val updatedTracks = allTracksMap.values.toList()
            _syncedTracks.value = updatedTracks
            persistTracks(updatedTracks)

            _syncState.value = SpotifySyncState.Done
            val msg = "Synced ${liked.size} Liked Songs and ${playlists.size} playlists from Spotify."
            _statusMessage.value = msg
            Result.success(totalImported)
        } catch (e: Exception) {
            Log.e(TAG, "Spotify sync failed", e)
            _syncState.value = SpotifySyncState.Error
            _statusMessage.value = "Sync failed: ${e.message}"
            Result.failure(e)
        }
    }

    private fun pathfinderGet(url: String, token: String): JSONObject? {
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()

        return try {
            http.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.w(TAG, "Pathfinder request failed: ${resp.code}")
                    return null
                }
                val body = resp.body?.string().orEmpty()
                JSONObject(body)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Pathfinder network error: ${e.message}")
            null
        }
    }

    private fun parsePathfinderTrack(data: JSONObject): SpotifyTrack? {
        val uri = data.optString("uri")
        val id = uri.removePrefix("spotify:track:").takeIf { it.isNotBlank() }
            ?: data.optString("id").takeIf { it.isNotBlank() }
            ?: return null

        val name = data.optString("name")
        val duration = data.optJSONObject("duration")?.optLong("totalMilliseconds", 0L)
            ?: data.optLong("duration_ms", 0L)

        val artistsArr = data.optJSONObject("artists")?.optJSONArray("items")
        val artistNames = mutableListOf<String>()
        if (artistsArr != null) {
            for (i in 0 until artistsArr.length()) {
                val a = artistsArr.optJSONObject(i)
                val aName = a?.optJSONObject("profile")?.optString("name")
                    ?: a?.optString("name")
                if (!aName.isNullOrBlank()) artistNames.add(aName)
            }
        }
        val artist = if (artistNames.isNotEmpty()) artistNames.joinToString(", ") else "Unknown Artist"

        val albumObj = data.optJSONObject("albumOfTrack") ?: data.optJSONObject("album")
        val albumName = albumObj?.optString("name")
        val images = albumObj?.optJSONObject("coverArt")?.optJSONArray("sources")
            ?: albumObj?.optJSONArray("images")
        val art = images?.optJSONObject(0)?.optString("url")

        return SpotifyTrack(
            id = id,
            title = name,
            artist = artist,
            album = albumName,
            durationMs = duration,
            artworkUrl = art,
        )
    }

    private fun SpotifyTrack.toStreamvibelyTrack(): Track {
        return Track(
            id = "sp:$id",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            artworkUrl = artworkUrl,
            source = "spotify",
            uri = null,
            canDownload = false,
            qualityLabel = "Spotify Web",
        )
    }

    private fun loadPersistedTracks(): List<Track> {
        if (!tracksFile.exists()) return emptyList()
        return try {
            val content = tracksFile.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<Track>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                list.add(
                    Track(
                        id = o.optString("id"),
                        title = o.optString("title"),
                        artist = o.optString("artist"),
                        album = o.optString("album").takeIf { it.isNotBlank() },
                        durationMs = o.optLong("durationMs"),
                        artworkUrl = o.optString("artworkUrl").takeIf { it.isNotBlank() },
                        source = o.optString("source", "spotify"),
                        uri = null,
                        qualityLabel = "Spotify Web",
                    ),
                )
            }
            list
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached Spotify tracks", e)
            emptyList()
        }
    }

    private fun persistTracks(tracks: List<Track>) {
        try {
            val arr = JSONArray()
            for (t in tracks) {
                val o = JSONObject()
                o.put("id", t.id)
                o.put("title", t.title)
                o.put("artist", t.artist)
                o.put("album", t.album.orEmpty())
                o.put("durationMs", t.durationMs)
                o.put("artworkUrl", t.artworkUrl.orEmpty())
                o.put("source", t.source)
                arr.put(o)
            }
            tracksFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to persist Spotify tracks", e)
        }
    }

    companion object {
        private const val TAG = "SpotifySync"
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private const val SESSION_TOKEN_URL = "https://open.spotify.com/api/token"
        private const val SERVER_TIME_URL = "https://open.spotify.com/api/server-time"
        private const val SECRETS_DICT_URL =
            "https://raw.githubusercontent.com/xyloflake/spot-secrets-go/refs/heads/main/secrets/secretDict.json"

        private const val SPOTIFY_ORIGIN = "https://open.spotify.com"
        private const val SPOTIFY_APP_VERSION = "1.2.62.476.g2ad6e7f3"

        private const val PATHFINDER_ENDPOINT = "https://api-partner.spotify.com/pathfinder/v1/query"
        private const val HASH_FETCH_PLAYLIST = "21ea11a6ac75a5f3e01fd365d29c18e0a4a8b7dc09f89bbb5d73910190c6acaf"
        private const val HASH_LIBRARY_V3 = "390c78e5b951029bad359785e69b07b536a509c581cbcd0aded5e5067f187455"
        private const val HASH_FETCH_LIBRARY_TRACKS = "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240"
    }
}
