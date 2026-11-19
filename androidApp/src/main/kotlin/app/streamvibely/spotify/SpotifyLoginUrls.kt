package app.streamvibely.spotify

/**
 * URLs and patterns used for Spotify web login and session authentication.
 */
object SpotifyLoginUrls {
    val STATUS_URL: Regex = Regex("^https://accounts\\.spotify\\.com/(?:[^/]+/)?status(?:\\?.*)?$")

    val ANONYMOUS_URL: Regex = Regex(
        "^https://accounts\\.spotify\\.com/(?:[^/]+/)?(?:login|signup|sign-up|reset-password|password-reset)(?:[/?#].*)?$",
    )

    const val LOGIN_URL: String = "https://accounts.spotify.com/en/login"

    fun isPostAuth(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (STATUS_URL.matches(url)) return true
        if (!url.startsWith("https://accounts.spotify.com/")) return false
        return !ANONYMOUS_URL.matches(url)
    }

    fun extractSpDc(cookieHeader: String?): String? {
        if (cookieHeader.isNullOrBlank()) return null
        for (pair in cookieHeader.split(";")) {
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2 && parts[0].trim().equals("sp_dc", ignoreCase = true)) {
                val value = parts[1].trim()
                if (value.isNotEmpty()) return value
            }
        }
        return null
    }
}

