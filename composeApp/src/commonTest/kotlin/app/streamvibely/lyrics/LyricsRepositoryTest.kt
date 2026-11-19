package app.streamvibely.lyrics

import app.streamvibely.domain.LyricLine
import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.LyricsProvider
import app.streamvibely.domain.Track
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class Fake(
    override val id: String,
    private val result: () -> Lyrics?,
) : LyricsProvider {
    override val displayName = id
    var calls = 0
    override suspend fun fetch(track: Track): Lyrics? {
        calls++
        return result()
    }
}

class LyricsRepositoryTest {
    private val track = Track("t1", "Title", "Artist")
    private val plain = Lyrics.Plain("a", "words")
    private val synced = Lyrics.Synced("b", listOf(LyricLine(0, "words")))

    @Test
    fun syncedFromLaterProviderBeatsEarlierPlain() = runTest {
        val repo = LyricsRepository { listOf(Fake("a") { plain }, Fake("b") { synced }) }
        assertEquals(synced, repo.lyricsFor(track))
    }

    @Test
    fun plainIsUsedWhenNothingBetterExists() = runTest {
        val repo = LyricsRepository { listOf(Fake("a") { plain }, Fake("b") { null }) }
        assertEquals(plain, repo.lyricsFor(track))
    }

    @Test
    fun throwingProviderIsSkipped() = runTest {
        val repo = LyricsRepository { listOf(Fake("a") { error("boom") }, Fake("b") { synced }) }
        assertEquals(synced, repo.lyricsFor(track))
    }

    @Test
    fun noResultsGivesNull() = runTest {
        assertNull(LyricsRepository { listOf(Fake("a") { null }) }.lyricsFor(track))
    }

    @Test
    fun resultsAreCachedByTrackId() = runTest {
        val provider = Fake("a") { synced }
        val repo = LyricsRepository { listOf(provider) }
        repo.lyricsFor(track)
        repo.lyricsFor(track)
        assertEquals(1, provider.calls)
    }

    @Test
    fun ordersProvidersFromSettings() {
        val a = Fake("a") { null }
        val b = Fake("b") { null }
        val c = Fake("c") { null }
        val ordered = orderProviders(
            listOf(a, b, c),
            listOf(LyricsSourceSetting("a", enabled = false, priority = 0), LyricsSourceSetting("b", true, 1)),
        )
        assertTrue(ordered == listOf(b, c))
    }
}
