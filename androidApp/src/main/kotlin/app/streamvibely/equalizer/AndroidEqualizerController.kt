package app.streamvibely.equalizer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.streamvibely.domain.EqBand
import app.streamvibely.domain.EqBandType
import app.streamvibely.domain.EqualizerController
import app.streamvibely.domain.EqualizerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.parametricEqStore by preferencesDataStore(name = "parametric_equalizer")
private val KeyEnabled = booleanPreferencesKey("enabled")
private val KeyPreamp = floatPreferencesKey("preamp_db")
private val KeyBands = stringPreferencesKey("bands_json")
private val KeyPresets = stringPreferencesKey("user_presets_json")
private val KeySelected = stringPreferencesKey("selected_preset")

private const val SAVE_DELAY_MS = 300L

private class UserPreset(val name: String, val preampDb: Float, val bands: List<EqBand>)

/**
 * Parametric equalizer. Every state change is pushed to [EqEngine] (which filters the audio inside
 * the Media3 pipeline) and saved to DataStore. Call from one thread (the main thread by default).
 */
class AndroidEqualizerController(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
) : EqualizerController {

    private val store = context.applicationContext.parametricEqStore
    private val lock = Any()
    private var nextId = 1 // advanced by defaultState(), which hands out ids 1..10
    private var userPresets: List<UserPreset> = emptyList()
    private var loaded = false
    private var saveJob: Job? = null

    private val _state = MutableStateFlow(defaultState())
    override val state: StateFlow<EqualizerState> = _state
    override val spectrum: StateFlow<FloatArray> = EqEngine.spectrum

    private val pushJob: Job
    private val loadJob: Job

    init {
        pushJob = scope.launch {
            _state.collect { s ->
                EqEngine.update(s.enabled, s.preampDb, s.bands)
                if (loaded) scheduleSave()
            }
        }
        loadJob = scope.launch {
            val saved = runCatching { store.data.first() }.getOrNull()
            if (saved != null) restore(
                enabled = saved[KeyEnabled],
                preamp = saved[KeyPreamp],
                bandsJson = saved[KeyBands],
                presetsJson = saved[KeyPresets],
                selected = saved[KeySelected],
            )
            loaded = true
        }
    }

    /** Stops the collectors. The controller cannot be reused afterwards. */
    fun release() {
        pushJob.cancel()
        loadJob.cancel()
        if (saveJob?.isActive == true) {
            saveJob?.cancel()
            val snapshot = _state.value
            val presets = userPresets
            CoroutineScope(Dispatchers.IO).launch { write(snapshot, presets) }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _state.update { it.copy(enabled = enabled) }
    }

    override fun setPreamp(db: Float) {
        _state.update { it.copy(preampDb = db.coerceIn(-24f, 12f), selectedPreset = null) }
    }

    override fun addBand(frequencyHz: Float, gainDb: Float, q: Float, type: EqBandType): Int {
        synchronized(lock) {
            val s = _state.value
            if (s.bands.size >= s.maxBands) return -1
            val id = nextId++
            val band = clamp(EqBand(id, frequencyHz, gainDb, q, type, true), s)
            _state.value = s.copy(bands = s.bands + band, selectedPreset = null)
            return id
        }
    }

    override fun updateBand(band: EqBand) {
        synchronized(lock) {
            val s = _state.value
            if (s.bands.none { it.id == band.id }) return
            val fixed = clamp(band, s)
            _state.value = s.copy(bands = s.bands.map { if (it.id == band.id) fixed else it }, selectedPreset = null)
        }
    }

    override fun removeBand(id: Int) {
        _state.update { s -> s.copy(bands = s.bands.filterNot { it.id == id }, selectedPreset = null) }
    }

    override fun usePreset(name: String) {
        synchronized(lock) {
            val builtIn = EqPresets.canonicalName(name)
            if (builtIn != null) {
                val bands = EqPresets.bands(builtIn) { nextId++ }
                _state.update { it.copy(bands = bands, preampDb = EqPresets.preampFor(builtIn), selectedPreset = builtIn) }
                return
            }
            val user = userPresets.firstOrNull { it.name == name } ?: return
            val bands = user.bands.map { it.copy(id = nextId++) }
            _state.update { it.copy(bands = bands, preampDb = user.preampDb, selectedPreset = user.name) }
        }
    }

    override fun savePreset(name: String) {
        val clean = name.trim().take(40)
        if (clean.isEmpty() || EqPresets.isBuiltIn(clean)) return
        synchronized(lock) {
            val s = _state.value
            val preset = UserPreset(clean, s.preampDb, s.bands)
            userPresets = userPresets.filterNot { it.name == clean } + preset
            _state.value = s.copy(presets = presetNames(), selectedPreset = clean)
        }
    }

    override fun deletePreset(name: String) {
        synchronized(lock) {
            if (userPresets.none { it.name == name }) return
            userPresets = userPresets.filterNot { it.name == name }
            _state.update {
                it.copy(presets = presetNames(), selectedPreset = if (it.selectedPreset == name) null else it.selectedPreset)
            }
        }
    }

    override fun reset() {
        synchronized(lock) {
            val bands = EqPresets.bands(EqPresets.FLAT) { nextId++ }
            _state.update { it.copy(bands = bands, preampDb = 0f, selectedPreset = EqPresets.FLAT) }
        }
    }

    // ---- internals ------------------------------------------------------------------------

    private fun presetNames(): List<String> = EqPresets.names + userPresets.map { it.name }

    private fun defaultState(): EqualizerState = EqualizerState(
        available = true,
        enabled = true,
        preampDb = 0f,
        bands = EqPresets.bands(EqPresets.FLAT) { nextId++ },
        presets = EqPresets.names,
        selectedPreset = EqPresets.FLAT,
    )

    private fun clamp(b: EqBand, s: EqualizerState) = b.copy(
        frequencyHz = b.frequencyHz.coerceIn(s.minFrequencyHz, s.maxFrequencyHz),
        gainDb = b.gainDb.coerceIn(s.minGainDb, s.maxGainDb),
        q = b.q.coerceIn(s.minQ, s.maxQ),
    )

    private fun restore(enabled: Boolean?, preamp: Float?, bandsJson: String?, presetsJson: String?, selected: String?) {
        synchronized(lock) {
            val bands = bandsJson?.let { parseBands(it) }
            userPresets = presetsJson?.let { parsePresets(it) }.orEmpty()
            var maxId = nextId - 1
            bands?.forEach { maxId = maxOf(maxId, it.id) }
            nextId = maxOf(nextId, maxId + 1)
            _state.update { cur ->
                cur.copy(
                    enabled = enabled ?: cur.enabled,
                    preampDb = preamp?.coerceIn(-24f, 12f) ?: cur.preampDb,
                    bands = bands?.map { clamp(it, cur) } ?: cur.bands,
                    presets = presetNames(),
                    selectedPreset = if (bands != null) selected else cur.selectedPreset,
                )
            }
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            write(_state.value, userPresets)
        }
    }

    private suspend fun write(s: EqualizerState, presets: List<UserPreset>) {
        val bands = bandsToJson(s.bands).toString()
        val presetsJson = JSONArray().also { arr ->
            for (p in presets) {
                arr.put(JSONObject().put("name", p.name).put("preamp", p.preampDb.toDouble()).put("bands", bandsToJson(p.bands)))
            }
        }.toString()
        runCatching {
            store.edit { prefs ->
                val sel = s.selectedPreset
                if (sel != null) prefs[KeySelected] = sel else prefs.remove(KeySelected)
                prefs[KeyEnabled] = s.enabled
                prefs[KeyPreamp] = s.preampDb
                prefs[KeyBands] = bands
                prefs[KeyPresets] = presetsJson
            }
        }
    }

    private fun bandsToJson(bands: List<EqBand>): JSONArray {
        val arr = JSONArray()
        for (b in bands) {
            arr.put(
                JSONObject()
                    .put("id", b.id)
                    .put("f", b.frequencyHz.toDouble())
                    .put("g", b.gainDb.toDouble())
                    .put("q", b.q.toDouble())
                    .put("t", b.type.name)
                    .put("on", b.enabled),
            )
        }
        return arr
    }

    private fun parseBands(json: String): List<EqBand>? = runCatching {
        val arr = JSONArray(json)
        val seen = HashSet<Int>()
        val out = ArrayList<EqBand>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val id = o.getInt("id")
            if (!seen.add(id) || out.size >= 32) continue
            out.add(
                EqBand(
                    id = id,
                    frequencyHz = o.getDouble("f").toFloat(),
                    gainDb = o.optDouble("g", 0.0).toFloat(),
                    q = o.optDouble("q", 1.0).toFloat(),
                    type = runCatching { EqBandType.valueOf(o.optString("t", "Peaking")) }.getOrDefault(EqBandType.Peaking),
                    enabled = o.optBoolean("on", true),
                ),
            )
        }
        out
    }.getOrNull()

    private fun parsePresets(json: String): List<UserPreset>? = runCatching {
        val arr = JSONArray(json)
        val out = ArrayList<UserPreset>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val bands = parseBands(o.getJSONArray("bands").toString()) ?: continue
            out.add(UserPreset(o.getString("name"), o.optDouble("preamp", 0.0).toFloat(), bands))
        }
        out
    }.getOrNull()
}
