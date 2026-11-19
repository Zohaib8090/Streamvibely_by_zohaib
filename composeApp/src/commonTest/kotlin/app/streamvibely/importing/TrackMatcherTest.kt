package app.streamvibely.importing

import app.streamvibely.domain.RemoteItem
import app.streamvibely.domain.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackMatcherTest {
    private val lib = listOf(
        Track("1", "Midnight Drive", "Neon Arcade"),
        Track("2", "Golden Hour", "Amber Sky"),
        Track("3", "Ocean Floor", "Blue Hour"),
    )

    @Test fun matchesPlainTitleAndArtist() {
        assertEquals(listOf("1"), matchTracks(listOf(RemoteItem("Midnight Drive", "Neon Arcade")), lib).map { it.id })
    }

    @Test fun ignoresVideoNoiseAndTopicSuffix() {
        val r = matchTracks(listOf(RemoteItem("Golden Hour (Official Video) [HD]", "Amber Sky - Topic")), lib)
        assertEquals(listOf("2"), r.map { it.id })
    }

    @Test fun artistMustAgree() {
        assertEquals(emptyList(), matchTracks(listOf(RemoteItem("Ocean Floor", "Someone Else")), lib))
    }

    @Test fun keepsPlaylistOrderWithoutDuplicates() {
        val items = listOf(RemoteItem("Ocean Floor", "Blue Hour"), RemoteItem("Midnight Drive", "Neon Arcade"), RemoteItem("Ocean Floor", "Blue Hour"))
        assertEquals(listOf("3", "1"), matchTracks(items, lib).map { it.id })
    }

    @Test fun unmatchedReturnsEmpty() {
        assertEquals(emptyList(), matchTracks(listOf(RemoteItem("Nothing Here", "Nobody")), lib))
    }
}
