# Adding your own music source

The app talks to every online source through one small contract, so a new source is one class plus one line.
This guide covers the plug-in point only. **It does not explain how to get audio out of any particular service.**
If a source needs to get around a service's protections (signature unscrambling, bot-check tokens, DRM), the legal
and terms-of-service risk is yours, and that code is not part of this project's default build or docs.
See `docs/providers.md` for why, and `docs/decisions.md` D9.

## 1. The contract (`domain/Models.kt`)

```kotlin
interface MusicProvider {
    val id: String            // short and stable, e.g. "audius"; shown as the track's source label
    val displayName: String   // shown on the Search chip
    suspend fun search(query: String): List<Track>
    suspend fun streams(track: Track): List<StreamCandidate>
}
```

- `search` returns tracks. Set `Track.uri` to an `https://` audio URL the player can open, so it plays directly.
  Never throw for "not found" or network errors: return an empty list.
- `Track.id` must be unique across providers: prefix it (`"audius:abc123"`).
- `Track.artworkUrl` (https) is downloaded by `AndroidArtworkLoader.loadRemote`: small, capped at 4 MB.
- `Track.canDownload = true` only if the source's terms allow saving the file. Leave it false for stream-only sources.
- `streams` can return several qualities (`AudioQuality`); `List<StreamCandidate>.best()` picks one.

## 2. Register it (`AppServices.kt`)

Add it to `defaultProviders()`. It appears as a Search chip and in the "All" search automatically.
For a source that needs the user to configure something (a key, a login), pass it in from the platform host
(`MainActivity`) instead, and leave it out of the list until it is configured.

## 3. Rules for any source

- Query text is sent to the source only when the user picks that chip or "All". Say so in the source's description.
- Use https only. Time out requests (10 to 15 s). Keep response bodies bounded.
- Do not store the user's passwords. Use the platform's sign-in (see `docs/permissions.md`, Google section).
- Check the source's terms before adding it, and record the decision in `docs/decisions.md`.

## 4. Examples to copy

- `providers/AudiusProvider.kt`: JSON search API, direct stream URL, not downloadable.
- `providers/InternetArchiveProvider.kt`: two-step lookup (search, then file list), downloadable.

## 5. YouTube in Search: what the app does and what you plug in

The app ships the **listing and UI**; it ships **no way to play** YouTube audio.

- `providers/YouTubeProvider.kt` searches with the official YouTube Data API (`search.list`, music category) using the
  signed-in user's token (`YouTubeAccount.accessTokenOrNull()`). Each search costs 100 quota units (10,000 per day by default).
  It only works when the user is signed in (Account screen); otherwise the chip says to sign in.
- Results have no audio URL (`uri = null`). When a result is tapped, `App.kt` calls `provider.streams(track)`; if that
  returns a stream URL, the track plays like any other (queue, equalizer, lyrics, notifications).
- Without a resolver, `canPlay` is false: the YouTube chip lists results, is left out of the **All** search, and tapping a
  result shows "Can't play this yet: no stream is set up for YouTube".
- **Your part:** implement `StreamResolver` (`suspend fun resolve(track: Track): String?`) and pass it in `MainActivity`:
  `YouTubeProvider({ youtube.accessTokenOrNull() }, MyResolver())`. The `track.id` is `yt:<videoId>`. Return an https URL
  or null. Whatever the resolver does is outside this project's docs and support, and its legal risk is yours.
- Downloads stay off for these tracks (`canDownload` is false).

## 6. YouTube playlists in the Library (Build 17)

- Once signed in, **Your library** lists the user's YouTube playlists (up to 25) with their songs, covers and channels,
  read through the official Data API (`playlists.list` and `playlistItems.list`, about 1 quota unit per call). Deleted and
  private videos are skipped. They are rebuilt each time the app starts; nothing is stored.
- Tapping a song uses the same path as Search: `provider.streams(track)` (your resolver) when it has no stream URL yet.
  With no resolver the playlist page shows the "can't play yet" message.
- The Account screen's "Import from YouTube" still copies matching songs you already own into a normal local playlist.

## 7. Provider switches and the two YouTube sources (Build 19)

- **Settings > Providers** lists every registered source with an on/off switch (stored in `AndroidProviderSettings`,
  `provider_settings` preferences). Off sources disappear from the Search chips and from "All", and get no queries.
  A source without a saved choice uses `MusicProvider.enabledByDefault`.
- YouTube is registered twice: **YouTube Music** (on by default; official API with the Music category) and **YouTube**
  (off by default; all videos). Either, both or neither can be on. Note: "YouTube Music" here means YouTube's Music
  category through the public Data API, not the separate YouTube Music catalogue.
- With both on, one search costs 200 Data API quota units (100 each).
- Add a switch for your own source by overriding `description` and `enabledByDefault` on your `MusicProvider`.

## 8. Background player

- Playback runs in `PlaybackService` (Media3 `MediaSessionService`, foreground service with notification), so it keeps going
  with the app closed, the screen off, and when the task is swiped away while playing.
- Shuffle and repeat (off, all, one) work from the app, the notification, the lock screen and Bluetooth/car controls.
  The queue shown in the app follows the real shuffled order.

## 9. Owner changes after Build 19 (recorded, not maintained by the assistant)

- `providers/YouTubeProvider.kt` was changed by the owner: search now tries YouTube Music's private web API first
  (no sign-in, no quota) and falls back to the official Data API when signed in. `requiresAccount` is now false.
  Sections 5 and 7 above describe the earlier, official-API-only behaviour and the 100-units-per-search cost; they no
  longer apply to the first path. The assistant does not write, review or debug the private-API part.
- A new test file `YouTubeProviderTest.kt` covers three behaviours (3 tests, passing at the time of writing).

## 10. Queue and next song (Build 22)

The queue now keeps every song of a list, including ones with no stream URL yet. Those carry a placeholder
address (`streamvibely://track?id=...`). Just before ExoPlayer loads such an item (which is also when it
pre-buffers the next song), the service asks the app's `PlayerController.trackResolver` for the real URL. A song that
cannot be resolved is skipped instead of stopping the queue. Shuffle and repeat are ExoPlayer's own, so the app,
notification, lock screen and Bluetooth all agree.

## 11. Jamendo (Build 24)

Jamendo has music shared by independent artists under Creative Commons licenses. It is free, but its API needs a free
client ID: register at https://devportal.jamendo.com, then add `jamendo.clientId=YOUR_ID` to `local.properties`
(or set `JAMENDO_CLIENT_ID`) and rebuild. Jamendo is off by default: switch it on in Settings > Providers. Without an ID it returns nothing. Tracks show their
license (for example "CC BY-NC-SA 3.0"), and the Download button is offered only where the artist allows downloads.
Many Jamendo licenses forbid commercial use ("NC"), so keep that in mind if you ever sell the app.

Jamendo is currently hidden from the app: set `SHOW_JAMENDO = true` in `AppServices.kt` to show it again.
