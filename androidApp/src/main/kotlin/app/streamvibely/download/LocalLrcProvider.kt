package app.streamvibely.download

import android.net.Uri
import app.streamvibely.domain.Lyrics
import app.streamvibely.domain.LyricsProvider
import app.streamvibely.domain.Track
import app.streamvibely.lyrics.LrcParser
import java.io.File

/** Reads the `.lrc` saved next to a downloaded or local audio file, so synced lyrics work offline. */
class LocalLrcProvider : LyricsProvider {
    override val id = "local-lrc"
    override val displayName = "Saved with the song"

    override suspend fun fetch(track: Track): Lyrics? {
        val path = track.uri?.takeIf { it.startsWith("file:") }?.let { Uri.parse(it).path } ?: return null
        val audio = File(path)
        val lrc = File(audio.parentFile, audio.nameWithoutExtension + ".lrc")
        if (!lrc.isFile || lrc.length() > 512 * 1024) return null
        val lines = LrcParser.parse(lrc.readText())
        return if (lines.isEmpty()) null else Lyrics.Synced("Saved with the song", lines)
    }
}
