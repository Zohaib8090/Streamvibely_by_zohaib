package app.streamvibely.importing

import app.streamvibely.domain.RemoteItem
import app.streamvibely.domain.Track

/** Lowercase, drop accents-free noise: brackets, "official video" style tags, punctuation. */
internal fun normalize(text: String): String {
    var t = text.lowercase()
    t = t.replace(Regex("\\([^)]*\\)|\\[[^]]*]"), " ")
    t = t.replace(Regex("\\b(official|video|audio|lyrics?|lyric video|hd|hq|remastered|music video|mv)\\b"), " ")
    t = t.replace(Regex("[^\\p{L}\\p{N} ]"), " ")
    return t.replace(Regex("\\s+"), " ").trim()
}

/** Channel names for auto-generated YouTube music end with " - Topic". */
internal fun cleanArtist(artist: String): String = artist.removeSuffix(" - Topic").removeSuffix("VEVO").trim()

/**
 * Finds the tracks the user already has (local files, downloads) that match [items], in playlist order,
 * without duplicates. A match needs the same normalized title (or the title inside the video title)
 * and, when both sides know an artist, one artist name containing the other. Nothing is downloaded.
 */
fun matchTracks(items: List<RemoteItem>, library: List<Track>): List<Track> {
    val prepared = library.map { Triple(it, normalize(it.title), normalize(it.artist)) }
    val out = LinkedHashMap<String, Track>()
    for (item in items) {
        val title = normalize(item.title)
        if (title.isEmpty()) continue
        val artist = normalize(cleanArtist(item.artist))
        val hit = prepared.firstOrNull { (_, t, a) ->
            val titleOk = t.isNotEmpty() && (t == title || title.contains(t) && t.length >= 4)
            val artistOk = artist.isEmpty() || a.isEmpty() || a == "unknown" || a.contains(artist) || artist.contains(a) || title.contains(a)
            titleOk && artistOk
        }
        if (hit != null && !out.containsKey(hit.first.id)) out[hit.first.id] = hit.first
    }
    return out.values.toList()
}
