# Decision log

## D1 — One UI codebase: Compose Multiplatform (2026-11-17)

**Decision:** Kotlin Multiplatform + Compose Multiplatform for Android, Linux desktop, web.

**Why:** The goal is to write the UI once. Native GTK, Rust frontends and Kotlin/Native
frontends each add a second UI codebase. Compose Multiplatform is the only option that gives
one UI codebase, one language, and first-party Material 3 Expressive support.

**Replaces:** the "strict native Linux" override in `archive/original-plan.md`
(no JVM, GTK4, Kotlin/Native linuxX64, Rust fallback). That plan was never implemented.

**Rejected:**
- GTK4 + Kotlin/Native cinterop: custom M3E widgets need GtkWidget subclassing through
  cinterop, no maintained bindings, no coroutine/GLib loop bridge, second UI codebase.
- Rust GTK4 frontend + Kotlin/Native core over C ABI: two runtimes, state serialization,
  still a second UI codebase.
- Qt Quick: good RAM, weak web target, manual M3E.

**Risk:** JVM RAM on Linux (unmeasured). Gate in Phase 0.

**Fallback:** Flutter for the whole project (native Linux binary, no JVM, stable web, but
Dart and weaker M3E support).

## D2 — Linux audio via GStreamer or libmpv (2026-11-17)

Writing a PipeWire/ALSA engine means writing decoding, resampling, gapless, ReplayGain and
buffering. Use an existing native engine behind an `expect/actual` interface. Pick between
GStreamer and libmpv in Phase 0 by testing JVM access (JNA/JNI) and RAM.

## D3 — Platform scope (2026-11-17)

Android, Linux x86_64, Web only. No Windows or macOS.

## D4 — Fresh start; the old Expo repo gets replaced (2026-11-17)

**Decision:** The old repo (github.com/Zohaib8090/Streamvibely_by_zohaib) is an old experiment
(Expo / TypeScript, plus some Kotlin and Nix, with a design the owner considers poor). Only the
name Streamvibely carries over. The new Compose Multiplatform codebase will replace the old
contents of that repo. No old code is ported unless it is clearly useful (see
`streamvibely-reuse.md`; mainly the player-state idea).

**Not done yet:** nothing in the old repo has been touched. Before replacing it:
- Keep the old code reachable: tag or branch it (for example `legacy-expo`) so history is not lost.
- Rotate the committed keystore and Firebase keys. Deleting files in a new commit does not
  remove them from git history.
- Only replace after the new project builds.

**UI reference:** the Figma file is readable through the Figma connector, so no browser agent or
screenshots are needed.

## D5 — Memory budget and thumbnail policy (2026-11-17)

**Budget (owner):** Linux desktop at most **300 MB** (hard ceiling, "not more than 300").
Mobile should use less than desktop; a number is not set yet. This replaces the earlier
"under 200 MB idle" target.

**Thumbnail policy (owner):** the app never loads a whole library of covers. Only what is on
screen is decoded:
- Wide (max) UI: up to **18** thumbnails loaded at once.
- Compact (min) UI: up to **9**.
- Suggestions row (pulled from YouTube): **5** at a time, with images cached on disk.
- Bottom/mini area: only the most recently played song.
- Side player (desktop): the cover of the track playing.
- Playlist header: one cover.

**Consequence:** performance tests must use these counts, not a 100-cover grid. The first
100-cover benchmark was the wrong test (see performance.md).

## D6 — Changes to the Figma mockup (owner, 2026-11-17)

Desktop:
1. The right-hand panel no longer shows the "Your sound" mini equalizer. It shows the **queue**
   (next up) under the now-playing cover.
2. The **equalizer button** goes in the bottom playback bar. The navigation (sidebar) lists
   Home, Search, Library and an Equalizer entry.
3. The **equalizer itself mounts in the right side panel**, not in the bottom bar or the main
   content area.

Mobile: almost final. Two screens need optimization: **Search** and **Now playing** (what
exactly to change is not specified yet).

Needs confirmation: item 2 was described as "Inside area will have home, search, library and
the equalizer button". I read "inside area" as the side navigation. If it means something else,
fix this entry.

## D7 — Streamvibely will be MIT licensed (owner, 2026-11-17) — SUPERSEDED by D9

**Decision:** the project license is MIT.

**Consequences:**
- GPL-3.0 code (including LastWave-native) cannot be copied or ported into the project. Mixing it
  in would make the whole app GPL-3.0, not MIT. "Open source" does not mean "usable in an MIT
  app"; the license must be compatible.
- Every dependency must be under a permissive license (MIT, Apache-2.0, BSD, ISC). Check the license
  of each new library before adding it. Reject GPL, LGPL-with-static-linking issues, AGPL.
- LastWave stays a read-only reference for ideas and feature scope. Only short parts (file names,
  the quality-tier idea, the provider-module concept) were read; none of its code is reproduced.
  Anything similar is written independently from public documentation.
- To use LastWave's code anyway, either relicense Streamvibely to GPL-3.0, or get written
  permission from its copyright holders.

**Not yet done:** no `LICENSE` file exists in this project. Add one with the copyright holder's name
when the owner confirms it.

## Open decisions

- Content source and DRM (affects whether native players can play the catalog).
- Backend language and hosting.
- GStreamer vs libmpv.
- Web delivery: Compose Wasm only, or add a lightweight separate site.


## D8: YouTube sign-in uses Google Play services' authorization API (2026-11-18)

- **Decision:** `com.google.android.gms:play-services-auth` 21.4.0 (Android only). `AuthorizationClient` returns an access
  token for `youtube.readonly`; Google identifies the app by package name + signing SHA-1 (the Android OAuth client).
- **Why:** the first attempt (OAuth code flow in the browser with a custom URI scheme) was rejected by Google with
  "Error 400: invalid_request": Android clients no longer offer the custom-scheme option. The Play services flow is the
  supported way for Android clients, needs no client secret, redirect or stored refresh token.
- **Cost:** needs Google Play services on the device (Waydroid has it); adds a closed-source library to the Android build only.
  Web and Linux would need a different flow (not built). The access token lives about an hour and is held in memory.


## D9 — Streamvibely is licensed GPL-3.0-or-later (owner, 2026-11-18)

**Decision:** the project license is GPL-3.0 (see `LICENSE`). It replaces D7 (MIT); nothing was published under MIT.

**Consequences:**
- GPL-3.0 code such as LastWave-native may be reused in the project as far as the *licence* goes, if copyright
  notices are kept and the source stays available. Dependencies must be GPL-compatible (Apache-2.0, MIT, BSD are fine;
  Google Play services is a system component, not bundled source).
- **Licence is not the only limit.** The assistant still does not write or copy code whose purpose is to get around
  a service's protections: YouTube signature (cipher) unscrambling, the BotGuard/PoToken generator, or add-on modules
  that pull from unlicensed "lossless" servers. That is a terms-of-service and copyright question, not a licence one.
  Generic parts (download tagging, quality tiers, TTML lyrics parsing, module structure) can be reused.
- Rule 5 in `CLAUDE.md` (no Spotify trade dress) is unchanged.

## D10 — YouTube Stream Extraction & Googlevideo Chunking Pipeline (2026-11-19)

**Decision:** Implemented stream extraction and playback streaming pipeline for YouTube Music tracks on Android.

**Components:**
1. **ChunkedRangeDataSource (`app.streamvibely.playback.ChunkedRangeDataSource`):**
   - Enforces bounded range requests ($\le 512\text{ KB}$) on `googlevideo.com` and `youtube.com` media streams to prevent server-side bandwidth throttling and mid-song HTTP 403 disconnects.
   - Parses `Content-Range` headers for total resource length.
   - Provides `StreamRefresher` interface for transparent mid-song re-resolution on HTTP 403 / 410.
   - Integrated into `PlaybackService`'s `DefaultMediaSourceFactory`.
2. **InnerTubeX Reflection & Faraday Player Configs (`app.streamvibely.playback.resolve.InnerTubeXStreamExtractor`):**
   - Calls `innertubex` via dynamic reflection (`java.lang.reflect.Proxy`) to bypass Kotlin metadata version differences.
   - Configures `PlayerConfigRepository` proxy targeting the Faraday player configs registry (`https://raw.githubusercontent.com/MetrolistGroup/faraday/master/registry/player_configs.json`) with Android `SharedPreferences` caching.
   - Enables bounded range capabilities in `ContentHints` (`withStreamCapabilities(true, false, true)`).
   - Handles client profile exclusion and cooldown.
3. **NewPipe Fallback (`app.streamvibely.playback.resolve.YouTubeStreamExtractor`):**
   - Uses `NewPipeExtractor` with custom `OkHttpNewPipeDownloader` as a fallback extractor when InnerTubeX cannot resolve.
   - Filters DRC audio, sorts by format preference (Opus webm first, then AAC/M4A), and probes candidate reachability.
4. **Account-Free InnerTube Search (`app.streamvibely.providers.YouTubeProvider`):**
   - Primary search uses YouTube Music's InnerTube API (`https://music.youtube.com/youtubei/v1/search`), returning songs, artists, artwork, and duration without requiring a Google account or consuming Data API v3 quota.
   - `requiresAccount` set to `false`. Falls back to official YouTube Data API v3 if signed in and InnerTube search misses.
5. **Resolution Coordinator (`app.streamvibely.playback.resolve.YouTubeStreamResolver`):**
   - Implements `app.streamvibely.providers.StreamResolver`, caching streams in memory and handling seamless URL replacement for `ChunkedRangeDataSource`.
   - Wired in `MainActivity.kt` to `YouTubeProvider` and `PlaybackService.streamRefresher`.

## D10: NewPipeExtractor and JitPack added by the owner (2026-11-19)

- `com.github.TeamNewPipe:NewPipeExtractor` v0.26.5 and `maven("https://jitpack.io")` (in `settings.gradle.kts`), used by
  the owner's playback code. NewPipeExtractor is GPL-3.0, which fits this project's licence (D9).
- JitPack builds libraries from source on request. The version is pinned to a tag; keep it pinned and re-check it when updating.
- Recorded here to satisfy CLAUDE.md rule 7. The assistant did not choose or review this dependency.


## D11: Spotify Web Login and Library Sync (2026-11-19)

**Context:** Users wanted to log in to Spotify and sync their Liked Songs and user playlists directly into Streamvibely.

**Decision:**
- Added `SpotifyLoginActivity` containing an Android `WebView` directed to `https://accounts.spotify.com/en/login`.
- Automatically captures the authentication session `sp_dc` cookie from `CookieManager` upon sign-in completion (matching post-auth status URLs), with a fallback dialog to paste `sp_dc` manually.
- `AndroidSpotifySyncRepository` exchanges `sp_dc` for personal access tokens via Spotify Web Player token endpoints (`https://open.spotify.com/get_access_token` and `https://open.spotify.com/api/token`).
- Fetches Liked Songs and all user playlists via Spotify Web API, creating playlists in Streamvibely's `PlaylistRepository`.
- Spotify synced tracks are persisted and fed into `AndroidPlaylistRepository`. When played, songs with `source = "spotify"` are dynamically resolved and streamed through YouTube Music stream extraction without DRM restrictions.

## D12: Dynamic Quality Display & Per-Provider Playback Quality Controls (2026-11-19)

**Context:** The Now Playing screen hardcoded `"On this device"` / `"Local file"` for all tracks, and users had no control over streaming audio quality.

**Decision:**
- Replaced hardcoded labels in `App.kt` and `NowPlayingScreen` with dynamic track source and quality information derived at playback time (e.g. `"Opus 256 kbps"`, `"AAC 128 kbps"`, `"Local file"`).
- Defined `PlaybackQualityPreference` (`Auto`, `High`, `Standard`, `Data Saver`) and `PlaybackQualityRepository`.
- Persisted quality settings on Android via `AndroidPlaybackQualityRepository` (`SharedPreferences`).
- Added "Streaming Quality" selector per provider in `SettingsScreen`.
- Hooked `maxBitrateKbps` into `YouTubeProvider.streams(track, maxBitrateKbps)` and `YouTubeStreamResolver.resolveCandidate(track, maxBitrateKbps)`.


## D13: Home Screen Provider & Playback Quality Preferences Bar (2026-11-19)

**Context:** Users wanted quick visibility and control over provider preferences (YouTube Music, YouTube, Spotify) directly on the Home screen without navigating deep into Settings.

**Decision:**
- Added `ProviderPreferencesRow` ([ProviderPreferencesRow.kt](file:///home/zohaibbaig/Documents/songsproject/composeApp/src/commonMain/kotlin/app/streamvibely/ui/home/ProviderPreferencesRow.kt)) to `HomeContent` directly beneath the top app header.
- Displays interactive pills for each enabled streaming provider showing its active quality setting (`Auto`, `High`, `Standard`, `Data Saver`). Tapping cycles the quality preference immediately with persisted settings.
- Displays a dedicated Spotify status pill indicating connection state (`Connected` / `Syncing...` / `Connect`). Tapping triggers an instant library sync or launches web login if not yet connected.

## D14: Real "Back in rotation", Spotify Recommendations on Home, Stream Audio Cache, & Sync Settings (2026-11-19)

**Context:**
- Synced Spotify tracks were previously isolated from the general `tracks` state, leaving the Home screen showing only local device files.
- "Back in rotation" was hardcoded to take the first scanned local files instead of songs the user actually played.
- Streamed audio from online sources was not cached to disk.
- Users needed settings controls to toggle provider display on Home, re-run the sync wizard, and manage stream cache.

**Decision:**
- **Unified Track Pool:** Merged `syncedTracks` from `SpotifySyncRepository` directly into `tracks` in `App.kt` so all synced songs appear across Home, Search, and Library.
- **Dedicated "From Your Spotify" Carousel:** Added a recommendations carousel on the Home screen displaying synced Spotify tracks with artwork, title, and artist.
- **Real "Back in Rotation":** Created `RecentlyPlayedRepository` and `AndroidRecentlyPlayedRepository` (`recently_played_tracks.json`). Whenever any track is played, it is moved to the top of the history. Home's "Back in rotation" displays these real recently played songs rather than locking to local files.
- **App Storage Stream Cache:** Integrated Media3's `CacheDataSource.Factory` with `SimpleCache` (512MB LRU evictor in `context.cacheDir/audio_stream_cache`) into `PlaybackService.kt`. All streamed songs are automatically cached on disk for fast, offline replay.
- **Settings Controls:** Added "Show Spotify on Home" toggle, "Sync & Preferences Wizard" launcher, and "Streamed Audio Cache" management with a "Clear audio cache" action in `SettingsScreen`.

## D15: Multi-Provider Stream Resolution for Spotify Tracks & Continuous Background Account Sync (2026-11-19)

**Context:**
- Previously, Spotify-synced songs only resolved audio streams by searching YouTube Music or YouTube, ignoring other configured/enabled sources (Audius, Internet Archive, etc.).
- Account and Spotify library sync only triggered when actively opening the "Your library" tab once, so continuous listening did not keep the library up to date.

**Decision:**
- **Multi-Source Resolution:** Updated `resolveStream` in `App.kt` to iterate across all active, enabled playable providers (YouTube Music, Audius, YouTube, Internet Archive, and custom providers). The first provider returning an audio stream is used, and the Now Playing screen indicates the exact provider (`"Spotify (via Audius)"`, `"Spotify (via YouTube Music)"`).
- **Continuous Background Sync:** Removed tab isolation from `LaunchedEffect(spotifyConnected)` and `LaunchedEffect(account)`. Streamvibely now continuously syncs Spotify and YouTube libraries in the background every 5–10 minutes while the app is active and users listen to songs, seamlessly keeping playlists and liked songs fresh.

## D16: Instant Playback via Parallel Provider Racing, Seamless Quality Upgrades, Working Downloads, & Reorganized Settings (2026-11-19)

**Context:**
- Sequential search across providers caused noticeable playback start latency.
- Users wanted instant response from whichever source replies first, with optional seamless upgrading to higher quality without restarting track timing.
- Playlist tracks had no pre-fetching, and offline downloads were previously blocked for non-Archive tracks.
- Settings had Spotify sync and providers scattered on the main screen, and lacked a dedicated Lyrics section.

**Decision:**
- **Parallel Provider Racing:** Rewrote `resolveStream` to query candidate providers simultaneously in parallel using coroutines and channels. The fastest provider response starts playback immediately (sub-second start).
- **Background Quality Upgrade Banner:** Parallel resolution continues for 3.5s in the background. If a higher bitrate stream is found (e.g., Opus 256 kbps vs 128 kbps), Now Playing displays an upgrade prompt (`"Higher quality available: ... [Upgrade]"`). Upgrading seamlessly switches streams while preserving exact playback position via `player.seekTo(currentPos)`.
- **Working Offline Downloads for All Sources:** Removed `canDownload` restrictions in `AndroidDownloadRepository` and wired stream resolution prior to download. Added "Download all" on playlists and "Download song" in track row menus and Now Playing.
- **Playlist Pre-caching:** Pre-resolves top tracks in the background when opening any playlist so tapping begins playback without latency.
- **Settings Reorganization:**
  - Moved Spotify Sync management exclusively to `AccountScreen` with a clean "Accounts & Sync" navigation row in Settings.
  - Grouped providers and their individual quality dropdowns under "Music Providers & Quality".
  - Added a dedicated "Lyrics" section displaying LRCLIB time-synced status and local `.lrc` sidecar details.

## D17: Playlist Batch Download Queueing, Progress Throttling, & UI Performance Fixes (2026-11-19)

**Context:**
- Tapping "Download all" on large playlists (e.g. Spotify Liked Songs with 400+ songs) caused the app to freeze, spike RAM/CPU, and drop frames.
- Root cause: `downloadPlaylist` launched hundreds of unbounded coroutines simultaneously, with each track triggering 4 parallel multi-provider search requests (~1,800 concurrent HTTP requests).
- Furthermore, `AndroidDownloadRepository` emitted progress state updates on every 32 KB chunk, bombarding Compose with thousands of recompositions per second.
- `MediaCache.getMediaCacheSizeBytes()` was executing recursive disk scans (`walkTopDown()`) directly inside Compose rendering frames on Settings.

**Decision:**
- **Sequential Managed Batch Queue:** Replaced unbounded playlist download coroutines with a single sequential queue job. It processes one track at a time, resolves its stream, downloads it, and waits for completion before proceeding to the next track.
- **Throttled Download Progress:** In `AndroidDownloadRepository`, throttled download progress emissions to at most once every 250ms per track (or upon finish/cancel), eliminating excessive Compose recompositions.
- **Off-Main-Thread Cache Size Computation:** Shifted `getMediaCacheSizeBytes()` to an asynchronous `Dispatchers.IO` block inside `LaunchedEffect(overlay == Overlay.Settings)`.
- **Pre-computed Home Track Lists:** Memoized `remainingTracks` with a `HashSet` lookup (`remember(tracks, rotationTracks)`) instead of doing $O(N \times M)$ filtering inside `LazyColumn` scroll passes.

## D18: True Offline Playback, Audio Container Preservation, & Chunked Range Downloads (2026-11-19)

**Context:**
- When internet was disconnected, downloaded songs failed to play from the Downloads area or playlists.
- Root cause 1: `AndroidDownloadRepository` hardcoded `.mp3` and prepended ID3v2 binary tags to all downloaded streams. However, YouTube / YouTube Music streams are WebM (Opus) or MP4 (AAC). Prepending an ID3v2 tag corrupted the container headers, causing ExoPlayer's extractors to fail (`UnrecognizedInputFormatException`).
- Root cause 2: Token-free YouTube (googlevideo) streams throttle or return HTTP 403 on open-ended requests >512 KB when downloaded without range chunking or proper User-Agent.
- Root cause 3: In `App.kt`, `playTrack` and `services.player.trackResolver` did not check if a track had a local downloaded copy before attempting online stream resolution, failing when offline.

**Decision:**
- **Container Sniffing & Preservation:** Implemented `AudioFormat.detect(part)` in `AndroidDownloadRepository`. WebM, MP4/M4A, OGG, and FLAC containers are preserved untouched with their native extensions (`.webm`, `.m4a`, etc.). ID3 tagging is only applied to true MP3 streams.
- **Chunked Range Downloads for YouTube:** Googlevideo streams are downloaded in 512 KB range chunks (`Range: bytes=start-end`) with the browser User-Agent, ensuring complete, unthrottled downloads.
- **Self-Healing Corrupted Downloads:** Added `repairCorruptedDownload` on init to strip invalid prepended ID3 headers from WebM/MP4 files downloaded prior to this fix.
- **Offline Playback Routing:** Updated `playTrack` and `trackResolver` in `App.kt` to prefer local downloaded files (`file:///...`) across all screens (Playlists, Home, Queue), enabling completely offline playback.

## D19: Spotify-Style Playlist Song Sorter & Multi-Provider Fallback Clarification (2026-11-19)

**Context:**
- Users needed the ability to sort songs inside playlists with options matching Spotify (Custom order, Title, Artist, Album, Recently added, Release date, Duration).
- Clarification was also needed regarding Spotify imported tracks that cannot be found on YouTube.

**Decision:**
- **Playlist Sort Bottom Sheet (`PlaylistSortSheet`):** Built a dark theme bottom sheet matching the exact Spotify layout with green checkmark selection (`#1ED760`), drag handle, and options: `Custom order`, `Title`, `Artist`, `Album`, `Recently added`, `Release date`, and `Duration`.
- **Dual Triggering:** Added a Sort icon button (`AppIcons.Sort`) in the playlist header and an interactive filter chip above the track list displaying the active sort mode.
- **Dynamic Queue Sorting:** Sorted tracks dynamically feed both the UI and the playback queue so tapping any track or "Play all" immediately honours the active sort order.






