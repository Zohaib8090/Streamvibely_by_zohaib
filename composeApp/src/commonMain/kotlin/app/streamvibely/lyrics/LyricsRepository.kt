package app.streamvibely.lyrics

import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.LyricsProvider
import app.streamvibely.domain.Track
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException

/** User choice for one lyrics source. Lower [priority] is tried first. */
data class LyricsSourceSetting(val providerId: String, val enabled: Boolean = true, val priority: Int = 0)

/**
 * Providers that are enabled, ordered by priority. Providers without a setting are enabled and
 * go after configured ones, keeping their registration order.
 */
fun orderProviders(all: List<LyricsProvider>, settings: List<LyricsSourceSetting>): List<LyricsProvider> {
    val byId = settings.associateBy { it.providerId }
    val (configured, unconfigured) = all.partition { it.id in byId }
    return configured.filter { byId.getValue(it.id).enabled }
        .sortedBy { byId.getValue(it.id).priority } + unconfigured
}

class LyricsRepository(private val providers: () -> List<LyricsProvider>) {
    private val cache = HashMap<String, Lyrics>()
    private val lock = Mutex()

    /** First synced result wins; otherwise the first plain result; otherwise null. Only hits are cached. */
    suspend fun lyricsFor(track: Track): Lyrics? {
        lock.withLock { cache[track.id] }?.let { return it }
        var plain: Lyrics? = null
        for (provider in providers()) {
            val found = try {
                provider.fetch(track)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Throwable) {
                null
            } ?: continue
            if (found is Lyrics.Synced) return remember(track, found)
            if (plain == null) plain = found
        }
        return plain?.let { remember(track, it) }
    }

    private suspend fun remember(track: Track, lyrics: Lyrics): Lyrics {
        lock.withLock { cache[track.id] = lyrics }
        return lyrics
    }
}
