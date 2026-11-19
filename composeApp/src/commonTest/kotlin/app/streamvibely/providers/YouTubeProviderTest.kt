package app.streamvibely.providers

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YouTubeProviderTest {

    @Test
    fun doesNotRequireAccount() {
        val provider = YouTubeProvider(accessToken = null, resolver = null)
        assertFalse(provider.requiresAccount)
        assertFalse(provider.canPlay)
    }

    @Test
    fun canPlayWhenResolverIsProvided() {
        val provider = YouTubeProvider(accessToken = null, resolver = { null })
        assertTrue(provider.canPlay)
    }

    @Test
    fun searchReturnsEmptyOnBlankQuery() = runTest {
        val provider = YouTubeProvider(accessToken = null, resolver = null)
        val results = provider.search("   ")
        assertTrue(results.isEmpty())
    }
}

