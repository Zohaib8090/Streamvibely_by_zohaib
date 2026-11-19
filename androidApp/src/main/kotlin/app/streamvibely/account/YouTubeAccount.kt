package app.streamvibely.account

import android.content.Context
import app.streamvibely.domain.Account
import app.streamvibely.domain.AccountRepository
import app.streamvibely.domain.PlaylistImporter
import app.streamvibely.domain.RemoteItem
import app.streamvibely.domain.RemotePlaylist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

/**
 * YouTube account link, read-only. Sign-in goes through Google Play services ([GoogleAuthorizer]); this class
 * only keeps the access token in memory (about an hour) and asks again, silently, when it expires. Nothing
 * secret is stored: just the display name, email and photo link so the Account screen can show who is signed in.
 * Sign-out forgets them; the user can also remove the app's access at myaccount.google.com/permissions.
 */
class YouTubeAccount(
    context: Context,
    /** Returns an access token or null. The Boolean says whether UI may be shown. */
    private val authorize: suspend (interactive: Boolean) -> String?,
) : AccountRepository, PlaylistImporter {
    private val prefs = context.applicationContext.getSharedPreferences("youtube_account", Context.MODE_PRIVATE)
    private val _account = MutableStateFlow<Account?>(null)
    override val account: StateFlow<Account?> = _account

    override val configured = true

    private var accessToken: String? = null
    private var accessExpiresAt = 0L

    init {
        if (prefs.getBoolean(KEY_SIGNED_IN, false)) {
            _account.value = Account(prefs.getString(KEY_NAME, null) ?: "YouTube account", prefs.getString(KEY_EMAIL, null), prefs.getString(KEY_PHOTO, null))
        }
    }

    override suspend fun signIn(): Boolean {
        val token = authorize(true) ?: return false
        keep(token)
        return withContext(Dispatchers.IO) {
            try {
                val me = get("https://openidconnect.googleapis.com/v1/userinfo", token)
                val account = Account(me.optString("name", "YouTube account"), me.optString("email").ifEmpty { null }, me.optString("picture").ifEmpty { null })
                prefs.edit().putBoolean(KEY_SIGNED_IN, true).putString(KEY_NAME, account.displayName)
                    .putString(KEY_EMAIL, account.email).putString(KEY_PHOTO, account.avatarUrl).apply()
                _account.value = account
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    /** An access token for the signed-in account, or null when nobody is signed in. */
    suspend fun accessTokenOrNull(): String? = if (_account.value == null) null else runCatching { token() }.getOrNull()

    override suspend fun signOut() {
        prefs.edit().clear().apply()
        accessToken = null
        _account.value = null
    }

    override suspend fun playlists(): List<RemotePlaylist> = withContext(Dispatchers.IO) {
        val out = mutableListOf<RemotePlaylist>()
        var page: String? = null
        do {
            val url = "https://www.googleapis.com/youtube/v3/playlists?part=snippet,contentDetails&mine=true&maxResults=50" +
                (page?.let { "&pageToken=$it" } ?: "")
            val json = get(url, token())
            val items = json.optJSONArray("items")
            for (i in 0 until (items?.length() ?: 0)) {
                val p = items!!.getJSONObject(i)
                out += RemotePlaylist(p.getString("id"), p.getJSONObject("snippet").getString("title"), p.getJSONObject("contentDetails").optInt("itemCount"))
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null && out.size < 200)
        out
    }

    override suspend fun items(playlistId: String): List<RemoteItem> = withContext(Dispatchers.IO) {
        val out = mutableListOf<RemoteItem>()
        var page: String? = null
        do {
            val url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=${URLEncoder.encode(playlistId, "UTF-8")}" +
                (page?.let { "&pageToken=$it" } ?: "")
            val json = get(url, token())
            val items = json.optJSONArray("items")
            for (i in 0 until (items?.length() ?: 0)) {
                val s = items!!.getJSONObject(i).getJSONObject("snippet")
                val title = s.optString("title")
                if (title == "Deleted video" || title == "Private video") continue
                val thumbs = s.optJSONObject("thumbnails")
                out += RemoteItem(
                    title = title,
                    artist = s.optString("videoOwnerChannelTitle"),
                    videoId = s.optJSONObject("resourceId")?.optString("videoId").orEmpty(),
                    thumbnailUrl = (thumbs?.optJSONObject("high") ?: thumbs?.optJSONObject("medium") ?: thumbs?.optJSONObject("default"))
                        ?.optString("url")?.ifEmpty { null },
                )
            }
            page = json.optString("nextPageToken").ifEmpty { null }
        } while (page != null && out.size < 500)
        out
    }

    // ---- tokens and HTTP -------------------------------------------------------------------

    private fun keep(token: String) {
        accessToken = token
        accessExpiresAt = System.currentTimeMillis() + 50 * 60_000L
    }

    private suspend fun token(): String {
        accessToken?.takeIf { System.currentTimeMillis() < accessExpiresAt }?.let { return it }
        val fresh = authorize(false) ?: error("Sign in again")
        keep(fresh)
        return fresh
    }

    private fun get(url: String, bearer: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000; c.readTimeout = 15_000
            c.setRequestProperty("Authorization", "Bearer $bearer")
            check(c.responseCode in 200..299) { "HTTP ${c.responseCode}" }
            return JSONObject(c.inputStream.use { it.readBytes().decodeToString() })
        } finally { c.disconnect() }
    }

    private companion object {
        const val KEY_SIGNED_IN = "signedIn"; const val KEY_NAME = "name"; const val KEY_EMAIL = "email"; const val KEY_PHOTO = "photo"
    }
}
