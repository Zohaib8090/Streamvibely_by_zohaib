package app.streamvibely.library

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.streamvibely.domain.LibraryRepository
import app.streamvibely.domain.LibrarySettings
import app.streamvibely.domain.MusicFolder
import app.streamvibely.domain.ScanMode
import app.streamvibely.domain.ScanState
import app.streamvibely.domain.Track
import app.streamvibely.permissions.AppPermission
import app.streamvibely.permissions.isGranted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

private val Context.libraryDataStore by preferencesDataStore(name = "library")
private val SETTINGS_KEY = stringPreferencesKey("library_settings")
private const val REFRESH_DEBOUNCE_MS = 2_000L

/**
 * The on-device music library. Create one per process (for example in the Application or the
 * first activity) and share it: it owns a scan job, a MediaStore observer and a DataStore file.
 */
class AndroidLibraryRepository(context: Context) : LibraryRepository {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    private val _settings = MutableStateFlow(LibrarySettings())
    override val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    override val settings: StateFlow<LibrarySettings> = _settings.asStateFlow()

    private val settingsLoaded = CompletableDeferred<Unit>()
    private val settingsMutex = Mutex()
    private val scanLock = Any()
    private var scanJob: Job? = null
    private var refreshJob: Job? = null

    private val observer = object : android.database.ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleRefresh()
    }

    init {
        scope.launch {
            val raw = try {
                appContext.libraryDataStore.data.first()[SETTINGS_KEY]
            } catch (e: IOException) {
                null
            }
            _settings.value = LibraryJson.decodeSettings(raw)
            settingsLoaded.complete(Unit)
        }
        try {
            appContext.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        } catch (e: Exception) {
            // no live refresh; manual rescan still works
        }
        startScan()
    }

    /** Stops observing and cancels work. Only needed if the repository is not process-lived. */
    fun close() {
        try { appContext.contentResolver.unregisterContentObserver(observer) } catch (e: Exception) { /* ignore */ }
        scope.coroutineContext[Job]?.cancel()
    }

    override suspend fun rescan() {
        startScan().join()
    }

    override suspend fun setScanMode(mode: ScanMode) {
        if (updateSettings { it.copy(scanMode = mode) }) startScan()
    }

    override suspend fun addFolder(uri: String, displayName: String) {
        val changed = updateSettings { s ->
            if (s.folders.any { it.uri == uri }) s
            else s.copy(folders = s.folders + MusicFolder("f_" + stableHash(uri), displayName, uri))
        }
        if (changed) startScan()
    }

    override suspend fun removeFolder(id: String) {
        val removed = _settings.value.folders.firstOrNull { it.id == id }
        val changed = updateSettings { s -> s.copy(folders = s.folders.filterNot { it.id == id }) }
        if (removed != null) {
            try {
                appContext.contentResolver.releasePersistableUriPermission(
                    android.net.Uri.parse(removed.uri), android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (e: Exception) {
                // already released or never held
            }
        }
        if (changed) startScan()
    }

    override suspend fun setMinDuration(seconds: Int) {
        if (updateSettings { it.copy(minDurationSeconds = seconds.coerceIn(0, 3_600)) }) startScan()
    }

    /** Applies [transform], persists the result, and returns true when something changed. */
    private suspend fun updateSettings(transform: (LibrarySettings) -> LibrarySettings): Boolean {
        settingsLoaded.await()
        return settingsMutex.withLock {
            val old = _settings.value
            val new = transform(old)
            if (new == old) return@withLock false
            _settings.value = new
            try {
                appContext.libraryDataStore.edit { it[SETTINGS_KEY] = LibraryJson.encodeSettings(new) }
            } catch (e: IOException) {
                // keep the in-memory value; it will be retried on the next change
            }
            true
        }
    }

    /** Cancels any running scan and starts a new one. */
    private fun startScan(): Job = synchronized(scanLock) {
        scanJob?.cancel()
        scope.launch {
            settingsLoaded.await()
            runScan(_settings.value)
        }.also { scanJob = it }
    }

    private fun scheduleRefresh() {
        synchronized(scanLock) {
            refreshJob?.cancel()
            refreshJob = scope.launch {
                delay(REFRESH_DEBOUNCE_MS)
                settingsLoaded.await()
                if (_settings.value.scanMode == ScanMode.AllDeviceAudio &&
                    isGranted(appContext, AppPermission.AudioLibrary)
                ) startScan()
            }
        }
    }

    private suspend fun runScan(s: LibrarySettings) {
        val minMs = s.minDurationSeconds * 1_000L
        try {
            val result: List<Track> = when (s.scanMode) {
                ScanMode.AllDeviceAudio -> {
                    if (!isGranted(appContext, AppPermission.AudioLibrary)) {
                        _scanState.value = ScanState.NeedsPermission
                        return
                    }
                    _scanState.value = ScanState.Scanning(0)
                    MediaStoreScanner.scan(appContext, minMs) { _scanState.value = ScanState.Scanning(it) }
                }
                ScanMode.SelectedFolders -> {
                    _scanState.value = ScanState.Scanning(0)
                    val cache = MetadataCache(SafScanner.cacheFile(appContext)).also { it.load() }
                    val tracks = SafScanner(appContext, cache)
                        .scan(s.folders, minMs) { _scanState.value = ScanState.Scanning(it) }
                    currentCoroutineContext().ensureActive() // a cancelled scan must not prune the cache
                    cache.save()
                    tracks
                }
            }
            val sorted = withContext(Dispatchers.Default) {
                result.sortedWith { a, b -> a.title.compareTo(b.title, ignoreCase = true) }
            }
            currentCoroutineContext().ensureActive()
            _tracks.value = sorted
            _scanState.value = ScanState.Done(sorted.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            _scanState.value = if (s.scanMode == ScanMode.AllDeviceAudio) ScanState.NeedsPermission
            else ScanState.Error("Access to a music folder was lost. Pick the folder again.")
        } catch (e: Exception) {
            _scanState.value = ScanState.Error("Could not read the music library.")
        }
    }
}
