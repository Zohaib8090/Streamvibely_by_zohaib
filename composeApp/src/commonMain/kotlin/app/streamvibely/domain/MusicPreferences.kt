package app.streamvibely.domain

import kotlinx.coroutines.flow.StateFlow

/** Genre card shown during onboarding. */
data class GenreCard(
    val id: String,
    val label: String,
    /** Emoji used as the icon inside the card. */
    val emoji: String,
    /** Background gradient start colour as ARGB long. */
    val colorArgb: Long,
)

/** Persistent music preference selections made during onboarding. */
data class MusicPreferences(
    val selectedGenres: Set<String> = emptySet(),
    val onboardingComplete: Boolean = false,
    val showSpotifyOnHome: Boolean = true,
    val showYouTubeOnHome: Boolean = true,
)

interface MusicPreferencesRepository {
    val preferences: StateFlow<MusicPreferences>
    fun setGenres(genres: Set<String>)
    fun completeOnboarding()
    fun resetOnboarding() // for debug / "re-run wizard"
    fun setShowSpotifyOnHome(show: Boolean)
    fun setShowYouTubeOnHome(show: Boolean)
}

/** In-memory stub — used on desktop / preview. */
class InMemoryMusicPreferencesRepository : MusicPreferencesRepository {
    private val _prefs = kotlinx.coroutines.flow.MutableStateFlow(MusicPreferences())
    override val preferences: StateFlow<MusicPreferences> = _prefs
    override fun setGenres(genres: Set<String>) { _prefs.value = _prefs.value.copy(selectedGenres = genres) }
    override fun completeOnboarding() { _prefs.value = _prefs.value.copy(onboardingComplete = true) }
    override fun resetOnboarding() { _prefs.value = MusicPreferences() }
    override fun setShowSpotifyOnHome(show: Boolean) { _prefs.value = _prefs.value.copy(showSpotifyOnHome = show) }
    override fun setShowYouTubeOnHome(show: Boolean) { _prefs.value = _prefs.value.copy(showYouTubeOnHome = show) }
}

/** Canonical genre list shown in the onboarding wizard. */
val AllGenres: List<GenreCard> = listOf(
    GenreCard("pop",        "Pop",          "🎤", 0xFFE91E8C),
    GenreCard("hiphop",     "Hip-Hop",      "🎧", 0xFFFF6B35),
    GenreCard("rnb",        "R&B",          "🎵", 0xFF9C27B0),
    GenreCard("electronic", "Electronic",   "🎛️", 0xFF00BCD4),
    GenreCard("rock",       "Rock",         "🎸", 0xFFB71C1C),
    GenreCard("bollywood",  "Bollywood",    "🎬", 0xFFFF9800),
    GenreCard("desi",       "Desi",         "🥁", 0xFF4CAF50),
    GenreCard("punjabi",    "Punjabi",      "🪘", 0xFFFFEB3B),
    GenreCard("lofi",       "Lo-fi",        "☕", 0xFF607D8B),
    GenreCard("phonk",      "Phonk",        "🌀", 0xFF212121),
    GenreCard("jazz",       "Jazz",         "🎷", 0xFF795548),
    GenreCard("classical",  "Classical",    "🎻", 0xFF5C6BC0),
    GenreCard("indie",      "Indie",        "🌿", 0xFF8BC34A),
    GenreCard("metal",      "Metal",        "🤘", 0xFF37474F),
    GenreCard("kpop",       "K-Pop",        "✨", 0xFFF06292),
    GenreCard("latin",      "Latin",        "💃", 0xFFFF5722),
    GenreCard("gaming",     "Gaming OST",   "🎮", 0xFF1565C0),
    GenreCard("country",    "Country",      "🤠", 0xFF8D6E63),
)

