package app.streamvibely.providers

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JamendoProviderTest {
    @Test
    fun formatsCreativeCommonsLicense() {
        assertEquals("CC BY-NC-SA 3.0", ccLabel("https://creativecommons.org/licenses/by-nc-sa/3.0/"))
        assertEquals(null, ccLabel(null))
    }

    @Test
    fun isOffByDefaultAndEmptyWithoutClientId() = runTest {
        assertFalse(JamendoProvider("some-id").enabledByDefault)
        val p = JamendoProvider("")
        assertFalse(p.enabledByDefault)
        assertTrue(p.search("rock").isEmpty())
    }
}
