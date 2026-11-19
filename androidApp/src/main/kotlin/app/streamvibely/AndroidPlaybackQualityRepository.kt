package app.streamvibely

import android.content.Context
import app.streamvibely.domain.PlaybackQualityPreference
import app.streamvibely.domain.PlaybackQualityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists user playback quality settings per provider using SharedPreferences.
 */
class AndroidPlaybackQualityRepository(context: Context) : PlaybackQualityRepository {
    private val prefs = context.applicationContext.getSharedPreferences("playback_quality_settings", Context.MODE_PRIVATE)

    private val state = MutableStateFlow<Map<String, PlaybackQualityPreference>>(
        prefs.all.mapNotNull { (k, v) ->
            (v as? String)?.let { k to PlaybackQualityPreference.fromId(it) }
        }.toMap(),
    )

    override val preferences: StateFlow<Map<String, PlaybackQualityPreference>> = state

    override fun getPreference(providerId: String): PlaybackQualityPreference {
        return state.value[providerId] ?: PlaybackQualityPreference.Auto
    }

    override fun setPreference(providerId: String, preference: PlaybackQualityPreference) {
        prefs.edit().putString(providerId, preference.id).apply()
        state.value = state.value + (providerId to preference)
    }
}

