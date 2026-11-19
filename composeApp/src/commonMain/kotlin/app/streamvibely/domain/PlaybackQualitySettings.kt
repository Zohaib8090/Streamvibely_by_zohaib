package app.streamvibely.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Playback quality preferences allowing users to limit or optimize bandwidth.
 */
enum class PlaybackQualityPreference(
    val id: String,
    val displayName: String,
    val description: String,
    val maxBitrateKbps: Int?,
) {
    DataSaver("data_saver", "Data Saver", "~64 kbps (Opus/AAC)", 70),
    Standard("standard", "Standard", "~128-160 kbps", 160),
    High("high", "High", "Up to 256-320 kbps", 320),
    Auto("auto", "Auto", "Best available quality", null);

    companion object {
        fun fromId(id: String?): PlaybackQualityPreference =
            entries.firstOrNull { it.id == id } ?: Auto
    }
}

/**
 * Settings repository managing playback quality per provider.
 */
interface PlaybackQualityRepository {
    val preferences: StateFlow<Map<String, PlaybackQualityPreference>>
    fun getPreference(providerId: String): PlaybackQualityPreference
    fun setPreference(providerId: String, preference: PlaybackQualityPreference)
}

class InMemoryPlaybackQualityRepository(
    initial: Map<String, PlaybackQualityPreference> = emptyMap()
) : PlaybackQualityRepository {
    private val _prefs = MutableStateFlow(initial)
    override val preferences: StateFlow<Map<String, PlaybackQualityPreference>> = _prefs

    override fun getPreference(providerId: String): PlaybackQualityPreference {
        return _prefs.value[providerId] ?: PlaybackQualityPreference.Auto
    }

    override fun setPreference(providerId: String, preference: PlaybackQualityPreference) {
        _prefs.value = _prefs.value + (providerId to preference)
    }
}

