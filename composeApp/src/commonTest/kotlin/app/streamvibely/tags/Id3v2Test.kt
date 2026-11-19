package app.streamvibely.tags

import app.streamvibely.domain.LyricLine
import app.streamvibely.lyrics.LrcParser
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Id3v2Test {
    private fun ByteArray.indexOf(s: String): Int {
        val p = s.encodeToByteArray()
        outer@ for (i in 0..size - p.size) { for (j in p.indices) if (this[i + j] != p[j]) continue@outer; return i }
        return -1
    }

    @Test fun headerSizeMatchesBody() {
        val tag = Id3v2.encode(Id3Tags(title = "Midnight Drive", artist = "Neon Arcade", album = "Night"))
        assertEquals("ID3", tag.copyOfRange(0, 3).decodeToString())
        assertEquals(3, tag[3].toInt())
        assertEquals(tag.size, Id3v2.existingTagLength(tag))
        assertTrue(tag.indexOf("TIT2") > 0 && tag.indexOf("TPE1") > 0 && tag.indexOf("TALB") > 0)
    }

    @Test fun titleIsUtf16WithBom() {
        val tag = Id3v2.encode(Id3Tags(title = "Hi"))
        val at = tag.indexOf("TIT2") + 10
        assertContentEquals(byteArrayOf(1, 0xFF.toByte(), 0xFE.toByte(), 'H'.code.toByte(), 0, 'i'.code.toByte(), 0), tag.copyOfRange(at, at + 7))
    }

    @Test fun coverAndLyricsFramesPresent() {
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3)
        val tag = Id3v2.encode(Id3Tags(title = "T", lyrics = "la la", cover = jpeg))
        assertTrue(tag.indexOf("USLT") > 0 && tag.indexOf("APIC") > 0 && tag.indexOf("image/jpeg") > 0)
        assertContentEquals(jpeg, tag.copyOfRange(tag.size - jpeg.size, tag.size))
    }

    @Test fun emptyTagsWriteNothing() {
        assertEquals(0, Id3v2.encode(Id3Tags(title = " ", artist = null)).size)
    }

    @Test fun noExistingTagIsZero() {
        assertEquals(0, Id3v2.existingTagLength(ByteArray(20)))
    }

    @Test fun lrcRoundTrip() {
        val lines = listOf(LyricLine(0, "first"), LyricLine(12_340, "second"), LyricLine(75_000, "third"))
        val text = LrcWriter.write(lines)
        assertTrue(text.startsWith("[00:00.00]first"))
        assertEquals(lines, LrcParser.parse(text))
    }
}
