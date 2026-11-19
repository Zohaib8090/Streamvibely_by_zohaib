# Music and lyrics providers

Status: contracts exist (`domain/Models.kt`: `MusicProvider`, `LyricsProvider`, `AudioQuality`,
`best()`); **two open providers are implemented (Audius, Internet Archive)**; more are listed below. One lyrics provider is being built (LRCLIB).

## Idea

Several sources can supply a track. For playback the app asks every enabled provider for stream
candidates, ranks them by `AudioQuality` and bitrate, and plays the best one (`List<StreamCandidate>.best()`).
Lyrics work the same way with a priority chain: the first provider that returns lyrics wins,
preferring time-synced lyrics over plain text.

## Reference app: LastWave-native (studied 2026-11-17)

https://github.com/Clash-Projects/LastWave-native, cloned read-only for study. Not copied.

What it is: a large Android-only app (about 770 Kotlin files, Media3, Compose) for YouTube Music.

Relevant parts found:
- **Lyrics** (`data/lyrics/`): about 15 sources, including LRCLIB, Genius, Musixmatch, Apple Music,
  Spotify, Kugou, BetterLyrics, SimpMusic, plus a TTML parser, sanitizer, translation and
  romanization.
- **Streams** (`data/music/`): YouTube Music's private InnerTube API, cipher handling and "PoToken"
  code to get audio URLs.
- **Quality** (`playback/StreamingQualityPolicy.kt`): a ladder Data saver, YouTube, MP3 320,
  CD lossless, Hi-Res 96, Max Hi-Res, Dolby Atmos; a selected tier is a ceiling, never a minimum.
- **Downloads** (`data/download/`): downloading with embedded tags and `.lrc` lyrics, remuxing, a
  storage layer, notifications, restore scan.
- **Provider modules** (`data/plugin/`): installable ".lwp" packages holding encrypted config.

## Why we are not copying it

(Written under D7 (MIT). D9 (2026-11-18) switched the project to GPL-3.0, which removes reason 1 below; reasons 2 and 3 still apply.)


1. **License.** LastWave is GPL-3.0. Being open source does not make it usable in an MIT app:
   copying its code would require the whole Streamvibely app to be released under GPL-3.0 too.
   The services it connects to (YouTube, Apple Music, Spotify, Musixmatch, lossless providers) are
   not open source either, and using them is a separate legal and terms-of-service question. Reading it for ideas and writing our own
   code is fine. Exception: if the owner of this project is also the copyright holder of
   LastWave, they may reuse it. That is unconfirmed.
2. **Terms of service and copyright.** The stream code works around YouTube's protections, and
   the "lossless" and Apple/Spotify/Musixmatch sources use private APIs or paid services without
   a license. Extracting and downloading copyrighted music this way can breach those services'
   terms and copyright law. We will not build circumvention code (cipher, PoToken, DRM bypass) or
   integrations with unlicensed sources without the owner explicitly deciding and accepting the risk.
3. **Android-only.** It is built on Android classes, so it cannot be reused in shared code anyway.

## How LastWave handles sources (read from its repo, 2026-11-17)

- **Disclaimer only:** its README says it is "open-source, non-commercial ... for educational and
  research purposes", is not affiliated with Google, YouTube, Spotify or Apple, and "does not host,
  store ... or distribute copyrighted media". A disclaimer is a statement, not a legal defense.
- **YouTube:** private InnerTube API, signature (cipher) handling, and a BotGuard token generator
  (`data/music/potoken/`) that imitates YouTube's bot checks to get playable audio URLs.
- **Lossless:** installable encrypted "addon" modules (`data/lossless/`, `data/addon/`) that point the
  app at third-party servers; the app itself ships none. The sources are not open source and are
  not shown to be licensed.
- **Lyrics / metadata:** many private or paid APIs (Apple Music, Spotify, Musixmatch) alongside open
  ones (LRCLIB).

## Decision for Streamvibely (assistant's position, owner may disagree)

Adopt the **structure** that keeps the app source-neutral: a provider-module system where providers
are separate, optional, and user-enabled, plus a clear disclaimer. Build only providers that are
licensed or openly allowed. Do **not** write BotGuard/PoToken or cipher bypass code, or integrations
with unlicensed lossless servers. If the owner wants those, they carry the legal risk and must
write or supply them; they are not part of the default build.

## Candidate lawful providers (to verify each one's terms before use)

- Local files and the app's own downloads.
- YouTube playlist import through the official YouTube Data API (metadata only).
- Open or licensed catalogs with public APIs: Jamendo (Creative Commons music), Audius,
  Internet Archive, Free Music Archive. Several allow streaming and downloads. Check license per track.
- Lyrics: LRCLIB (open). Others only after reading their terms.

## Lawful starting set

- **Lyrics:** LRCLIB (free, open, has synced and plain lyrics). Others added one at a time after a
  check of their terms.
- **Music:** local files on the device, files the app itself downloaded, and the user's own
  YouTube library imported through the official YouTube Data API (metadata and playlists). Playing
  YouTube audio is a separate question: embedding YouTube's official player is permitted; extracting
  raw audio streams is the risky part.
- **Downloads:** local files and sources that allow downloading.

## Open decisions

See `docs/tasks.md`, "Needs a decision from the owner".


## Implemented providers (Build 8)

- **Audius** (`providers/AudiusProvider.kt`): public API `api.audius.co/v1`, no key, `app_name=Streamvibely`.
  Only tracks the API marks streamable are shown. Stream URL redirects to a creator node.
- **Internet Archive** (`providers/InternetArchiveProvider.kt`): Open Source Audio collection via
  `advancedsearch.php` + `metadata/<id>`; plays the first MP3 of each item. Search takes about 2 requests per result (12 results).
- Privacy: a query is sent to a provider only after the user selects that provider's chip in Search.
  "This device" never leaves the phone.
- Covers for online tracks are downloaded on Android only (`AndroidArtworkLoader.loadRemote`, https only,
  4 MB cap, sampled to the requested size). Web and Linux show the placeholder for now.
- Not done: Jamendo (needs a free client id from the owner), downloads, per-track license display, queue across providers.
- Checked on Waydroid: both search, covers show, Audius and Internet Archive tracks play and pause.

## Downloads (Build 9)

- Contract: `DownloadRepository` in `domain/Services.kt`; Android: `download/AndroidDownloadRepository.kt`.
- Saved to the app's own folder (`Android/data/app.streamvibely/files/Music/downloads`), no storage permission,
  removed with the app. An `index.json` keeps track details; the cover is saved as a file next to the audio.
- Only tracks with `Track.canDownload` can be saved. **Internet Archive: yes. Audius: no** (its public API only
  streams; most tracks are "All rights reserved"). Add other providers only after reading their terms.
- Downloaded tracks appear in All songs, a "Downloads" playlist, and can be added to playlists. Remove via the track menu.
- Limits: 2 at a time, 300 MB per file, https only. A download stops if the app is killed (partial files are deleted next start).
  No notification or resume yet. Web and Linux use `NoDownloads`.

## Download tagging and saved lyrics (Build 15)

- Each download gets an ID3v2.3 tag (title, artist, album, cover, plain lyrics) written by `tags/Id3v2.kt` (shared code,
  independent implementation from the public spec, 6 tests). Any tag already in the file is replaced.
- Synced lyrics found at download time (LRCLIB) are saved as `<file>.lrc` next to the audio (`LrcWriter`).
  `download/LocalLrcProvider.kt` reads that file first, so lyrics work offline. Removing a download deletes the `.lrc` too.
- The title and artist of the track are sent to LRCLIB once, when the download starts.
- Checked on Waydroid: tags and cover read back with ffprobe; a `.lrc` beside a download shows synced lyrics ("via Saved with the song").
  Not seen yet: a download where LRCLIB actually returned lyrics (the test track had none).
- Adding your own source: `docs/custom-providers.md`.
