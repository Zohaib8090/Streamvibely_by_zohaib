package app.streamvibely.tags

import app.streamvibely.domain.LyricLine

/** What to write into an MP3's ID3 tag. Null or blank fields are left out. [cover] is a JPEG or PNG. */
class Id3Tags(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val lyrics: String? = null,
    val cover: ByteArray? = null,
)

/**
 * Minimal ID3v2.3 writer: title, artist, album, unsynchronised lyrics (USLT) and front cover (APIC).
 * Text is UTF-16 with a byte order mark, which every common player reads. Written independently from the
 * public ID3v2.3 specification (id3.org/id3v2.3.0).
 */
object Id3v2 {
    /** The tag as bytes, ready to put in front of the audio data. Empty when there is nothing to write. */
    fun encode(tags: Id3Tags): ByteArray {
        val frames = ArrayList<ByteArray>()
        tags.title.clean()?.let { frames += frame("TIT2", utf16Text(it)) }
        tags.artist.clean()?.let { frames += frame("TPE1", utf16Text(it)) }
        tags.album.clean()?.let { frames += frame("TALB", utf16Text(it)) }
        tags.lyrics.clean()?.let { frames += frame("USLT", byteArrayOf(1) + "eng".encodeToByteArray() + EMPTY_UTF16 + utf16Text(it).drop(1).toByteArray()) }
        tags.cover?.takeIf { it.isNotEmpty() }?.let { frames += frame("APIC", picture(it)) }
        if (frames.isEmpty()) return ByteArray(0)
        val body = frames.fold(ByteArray(0)) { acc, f -> acc + f }
        return "ID3".encodeToByteArray() + byteArrayOf(3, 0, 0) + synchsafe(body.size) + body
    }

    /** Total length (header included) of an ID3v2 tag at the start of [header] (at least 10 bytes), or 0 if there is none. */
    fun existingTagLength(header: ByteArray): Int {
        if (header.size < 10) return 0
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return 0
        val size = (0 until 4).fold(0) { acc, i -> (acc shl 7) or (header[6 + i].toInt() and 0x7f) }
        val footer = if (header[5].toInt() and 0x10 != 0) 10 else 0
        return 10 + size + footer
    }

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    // Encoding byte 1 = UTF-16 with BOM; the BOM is FF FE (little endian).
    private fun utf16Text(text: String): ByteArray {
        val out = ArrayList<Byte>(text.length * 2 + 3)
        out += 1
        out += 0xFF.toByte(); out += 0xFE.toByte()
        for (c in text) { out += (c.code and 0xFF).toByte(); out += (c.code shr 8).toByte() }
        return out.toByteArray()
    }

    private val EMPTY_UTF16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0, 0)

    private fun picture(data: ByteArray): ByteArray {
        val mime = if (data.size > 3 && data[0] == 0x89.toByte() && data[1] == 'P'.code.toByte()) "image/png" else "image/jpeg"
        return byteArrayOf(0) + mime.encodeToByteArray() + byteArrayOf(0, 3, 0) + data
    }

    private fun frame(id: String, body: ByteArray): ByteArray {
        val size = body.size
        val header = id.encodeToByteArray() + byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte(), 0, 0)
        return header + body
    }

    private fun synchsafe(n: Int) = byteArrayOf(((n shr 21) and 0x7f).toByte(), ((n shr 14) and 0x7f).toByte(), ((n shr 7) and 0x7f).toByte(), (n and 0x7f).toByte())
}

/** Writes synced lyrics as a standard `.lrc` file body: `[mm:ss.xx]text`. */
object LrcWriter {
    fun write(lines: List<LyricLine>): String = lines.joinToString("\n") { line ->
        val total = if (line.startMs < 0) 0 else line.startMs
        val cs = (total % 1000) / 10
        val s = (total / 1000) % 60
        val m = total / 60000
        "[${m.pad(2)}:${s.pad(2)}.${cs.pad(2)}]${line.text}"
    } + "\n"

    private fun Long.pad(n: Int) = toString().padStart(n, '0')
}
