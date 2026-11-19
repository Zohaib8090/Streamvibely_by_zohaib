package app.streamvibely.domain

/** Fake data for building and previewing screens before real providers exist. */
object SampleData {
    val tracks = listOf(
        Track("t1", "Weightless", "Marconi Union", "Weightless", 483_000, source = "youtube"),
        Track("t2", "A radiance", "Nils Hoffmann", durationMs = 214_000, source = "spotify"),
        Track("t3", "Bloom", "ODESZA", durationMs = 236_000, source = "youtube"),
        Track("t4", "Night swimmer", "Kano", durationMs = 198_000, source = "local", isDownloaded = true),
        Track("t5", "Innerbloom", "RUFUS DU SOL", durationMs = 580_000, source = "youtube"),
        Track("t6", "Open water", "Tycho", durationMs = 241_000, source = "spotify"),
        Track("t7", "Violet skies", "Kano", durationMs = 205_000, source = "local", isDownloaded = true),
    )
    val playlists = listOf(
        Playlist("p1", "Late night drive", "For the long way home.", tracks),
        Playlist("p2", "Deep focus", "Ambient, quiet, steady.", tracks.take(4)),
        Playlist("p3", "Soft mornings", null, tracks.takeLast(3)),
    )
}
