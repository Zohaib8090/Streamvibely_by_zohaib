package app.streamvibely.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LrcParserTest {
    @Test
    fun parsesTwoAndThreeDigitFractions() {
        val lines = LrcParser.parse("[00:01.50]a\n[01:02.345]b")
        assertEquals(1500L, lines[0].startMs)
        assertEquals(62_345L, lines[1].startMs)
    }

    @Test
    fun multipleTimestampsOnOneLine() {
        val lines = LrcParser.parse("[00:10.00][00:30.00]chorus\n[00:20.00]verse")
        assertEquals(listOf("chorus", "verse", "chorus"), lines.map { it.text })
        assertEquals(listOf(10_000L, 20_000L, 30_000L), lines.map { it.startMs })
    }

    @Test
    fun appliesOffsetAndClampsAtZero() {
        val lines = LrcParser.parse("[offset:+500]\n[00:00.30]a\n[00:02.00]b")
        assertEquals(listOf(0L, 1500L), lines.map { it.startMs })
        assertEquals(2500L, LrcParser.parse("[offset:-500]\n[00:02.00]b")[0].startMs)
    }

    @Test
    fun ignoresMetadataAndBlankLines() {
        val raw = "[ti:Song]\n[ar:Artist]\n[al:Album]\n[by:me]\n\n[00:01.00]hello\n[00:02.00]\n"
        assertEquals(listOf("hello"), LrcParser.parse(raw).map { it.text })
    }

    @Test
    fun sortsByTime() {
        val lines = LrcParser.parse("[00:05.00]b\n[00:01.00]a")
        assertEquals(listOf("a", "b"), lines.map { it.text })
    }

    @Test
    fun detectsSynced() {
        assertTrue(LrcParser.isSynced("[ti:x]\n[00:01.00]a"))
        assertFalse(LrcParser.isSynced("just plain\nlyrics"))
    }
}
