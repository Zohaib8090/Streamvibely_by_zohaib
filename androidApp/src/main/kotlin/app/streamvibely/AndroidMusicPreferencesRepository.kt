package app.streamvibely

import android.content.Context
import app.streamvibely.domain.MusicPreferences
import app.streamvibely.domain.MusicPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidMusicPreferencesRepository(context: Context) : MusicPreferencesRepository {

    private val prefs = context.getSharedPreferences("music_preferences", Context.MODE_PRIVATE)

    private fun load(): MusicPreferences {
        val raw = prefs.getString("selected_genres", "") ?: ""
        val genres = if (raw.isBlank()) emptySet() else raw.split(",").toSet()
        val complete = prefs.getBoolean("onboarding_complete", false)
        val showSpotify = prefs.getBoolean("show_spotify_on_home", true)
        val showYouTube = prefs.getBoolean("show_youtube_on_home", true)
        return MusicPreferences(
            selectedGenres = genres,
            onboardingComplete = complete,
            showSpotifyOnHome = showSpotify,
            showYouTubeOnHome = showYouTube,
        )
    }

    private val _prefs = MutableStateFlow(load())
    override val preferences: StateFlow<MusicPreferences> = _prefs

    override fun setGenres(genres: Set<String>) {
        prefs.edit().putString("selected_genres", genres.joinToString(",")).apply()
        _prefs.value = _prefs.value.copy(selectedGenres = genres)
    }

    override fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_complete", true).apply()
        _prefs.value = _prefs.value.copy(onboardingComplete = true)
    }

    override fun resetOnboarding() {
        prefs.edit().remove("onboarding_complete").remove("selected_genres").apply()
        _prefs.value = MusicPreferences()
    }

    override fun setShowSpotifyOnHome(show: Boolean) {
        prefs.edit().putBoolean("show_spotify_on_home", show).apply()
        _prefs.value = _prefs.value.copy(showSpotifyOnHome = show)
    }

    override fun setShowYouTubeOnHome(show: Boolean) {
        prefs.edit().putBoolean("show_youtube_on_home", show).apply()
        _prefs.value = _prefs.value.copy(showYouTubeOnHome = show)
    }
}

