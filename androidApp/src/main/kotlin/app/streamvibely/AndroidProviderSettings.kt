package app.streamvibely

import android.content.Context
import app.streamvibely.domain.ProviderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Remembers which search sources are on or off, in the app's private preferences. */
class AndroidProviderSettings(context: Context) : ProviderSettings {
    private val prefs = context.applicationContext.getSharedPreferences("provider_settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow<Map<String, Boolean>>(
        prefs.all.mapNotNull { (k, v) -> (v as? Boolean)?.let { k to it } }.toMap(),
    )
    override val choices: StateFlow<Map<String, Boolean>> = state

    override fun setEnabled(providerId: String, enabled: Boolean) {
        prefs.edit().putBoolean(providerId, enabled).apply()
        state.value = state.value + (providerId to enabled)
    }
}
