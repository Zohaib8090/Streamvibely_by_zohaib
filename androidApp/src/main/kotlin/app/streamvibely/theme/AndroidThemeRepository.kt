package app.streamvibely.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.streamvibely.domain.ColorSource
import app.streamvibely.domain.PaletteVariant
import app.streamvibely.domain.ThemeArea
import app.streamvibely.domain.ThemeMode
import app.streamvibely.domain.ThemeRepository
import app.streamvibely.domain.ThemeSpec
import app.streamvibely.domain.ThemeStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

private val Context.themeStore by preferencesDataStore(name = "theme_spec")
private val SPEC_KEY = stringPreferencesKey("spec")

/**
 * Persists the whole [ThemeSpec] as one JSON string. The saved spec is read once on creation
 * (a tiny file) so the first frame already uses the user's theme instead of flashing the default.
 */
class AndroidThemeRepository(context: Context, private val scope: CoroutineScope) : ThemeRepository {
    private val store = context.applicationContext.themeStore
    private val _spec = MutableStateFlow(
        runBlocking { store.data.first()[SPEC_KEY] }?.let(::decode) ?: ThemeSpec(),
    )
    override val spec: StateFlow<ThemeSpec> = _spec.asStateFlow()

    override suspend fun update(transform: (ThemeSpec) -> ThemeSpec) {
        val next = transform(_spec.value)
        _spec.value = next
        scope.launch { store.edit { it[SPEC_KEY] = encode(next) } }
    }

    override suspend fun reset() = update { ThemeSpec() }
}

private fun encode(s: ThemeSpec): String = JSONObject().apply {
    put("mode", s.mode.name)
    put("style", s.style.name)
    put("source", s.source.name)
    put("seedColor", s.seedColor)
    put("variant", s.variant.name)
    put("contrast", s.contrast.toDouble())
    put("cornerScale", s.cornerScale.toDouble())
    put("clayDepth", s.clayDepth.toDouble())
    put("glassBlurDp", s.glassBlurDp.toDouble())
    put("glassOpacity", s.glassOpacity.toDouble())
    put("areaOverrides", JSONObject().also { o -> s.areaOverrides.forEach { (k, v) -> o.put(k.name, v) } })
    s.presetId?.let { put("presetId", it) }
}.toString()

private inline fun <reified E : Enum<E>> JSONObject.enumOr(key: String, default: E): E =
    optString(key).let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default

/** A damaged or outdated file yields null, so the default theme is used. */
private fun decode(json: String): ThemeSpec? = runCatching {
    val o = JSONObject(json)
    val d = ThemeSpec()
    val overrides = HashMap<ThemeArea, Int>()
    o.optJSONObject("areaOverrides")?.let { ao ->
        ao.keys().forEach { k ->
            ThemeArea.entries.firstOrNull { it.name == k }?.let { overrides[it] = ao.getInt(k) }
        }
    }
    ThemeSpec(
        mode = o.enumOr("mode", d.mode),
        style = o.enumOr("style", d.style),
        source = o.enumOr("source", d.source),
        seedColor = o.optInt("seedColor", d.seedColor),
        variant = o.enumOr("variant", d.variant),
        contrast = o.optDouble("contrast", d.contrast.toDouble()).toFloat(),
        cornerScale = o.optDouble("cornerScale", d.cornerScale.toDouble()).toFloat(),
        clayDepth = o.optDouble("clayDepth", d.clayDepth.toDouble()).toFloat(),
        glassBlurDp = o.optDouble("glassBlurDp", d.glassBlurDp.toDouble()).toFloat(),
        glassOpacity = o.optDouble("glassOpacity", d.glassOpacity.toDouble()).toFloat(),
        areaOverrides = overrides,
        presetId = if (o.has("presetId")) o.getString("presetId") else null,
    )
}.getOrNull()
