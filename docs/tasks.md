# Task list

Order of work (owner, 2026-11-17): **Android and web first.** Linux UI code is written now in the
shared code, but the Linux app is not built after each change; build it only on request.

## Done

- Project builds for Android, web (Wasm) and Linux desktop from one codebase.
- Theme and tokens from Figma (`design-system/tokens/tokens.json`, generated `Tokens.kt`).
- Shared contracts: models, lyrics and music provider interfaces, player state, sample data.
- Screens written and wired with fake data and simple navigation (`App.kt`): Home, Search,
  Your library, Playlist, Now playing, Queue. Phone layout (bottom nav and mini-player) and
  wide layout (sidebar, right panel, bottom bar).
- Seen rendering in Chrome (web build): Home (phone and wide), Now playing (phone), Search,
  Library and Playlist (wide).
- Motion toolkit (springs, press scale, morphing play button, transitions, shimmer): written and
  compiles, **not yet used by any screen**.
- Lyrics engine: LRC parser, provider chain preferring synced lyrics, LRCLIB provider, lyrics view.
  12 unit tests pass. **Lyrics view is not reachable from the UI yet.**
- Android: widgets (now playing, recently played) and permission helpers written; manifest lists
  the permissions and widget receivers. The APK builds. **Not run on a device.**

## Local playback milestone (2026-11-17, built, not yet run on a phone)

Owner direction: local files first; online sources later.

- Android playback: Media3 player in a foreground service (lock-screen and notification controls),
  queue, shuffle, repeat, audio focus. Asks for the notification permission, with an explanation, the
  first time you press play.
- Local library: scans either all audio on the device or only the folders you pick (system folder
  picker, remembered between launches); needs the audio-read permission for "all". Settings screen has
  the scan-mode choice, folder list, rescan and permission status.
- Playlists you create are saved on the device; "All songs" is a built-in list.
- Equalizer: real Android equalizer (band count depends on the phone) with presets; full screen on
  phone, side panel on wide screens.
- Real covers from the files, decoded near display size with a 24-image cache.
- Lyrics: opens inside Now playing; fetched from LRCLIB only while the lyrics view is open.
- Home, Search (150 ms debounce), Library, Playlist, Now playing, Queue, Settings all wired to real data.
- Web and desktop still run on fake in-memory services (no audio).

## Build 3 (2026-11-17, built; tested only by unit tests, not on a device)

- Real icons in all screens, tab bar and mini player (Material Icons Rounded for now).
- Expandable player: drag up/down with the finger, tap the mini to open; swipe the collapsed mini
  player DOWN to stop playback and hide it.
- Account button (top left of Home) opens a side menu you can drag out from the left; it holds
  Account, Listening usage and Settings. Sign-in is a stub ("not available yet").
- Listening usage: time per day, offline vs online, most played, kept on the device only.
- New equalizer: any number of bands (frequency, gain, Q, type), preamp, presets, live spectrum.
  22 unit tests pass (equalizer math, FFT, LRC parser, lyrics chain).
- Icon options for review: `docs/icon-options.html` (Material Symbols, Lucide, Phosphor, Tabler).

## Build 4 (2026-11-18, tested on Android 13 in Waydroid and on the web build)

Verified on a running Android device: library scan and real album covers, playback (media session
PLAYING), phone layout and tab icons, swipe up to open the player, swipe down to collapse it, swipe down on the
mini player to stop playback, side menu dragged out from the left edge, Customization page, and album-art
colors (green cover -> green app, pink cover -> pink app).
Verified on the web build: Material / Clay / Glass / OLED styles switch live, color picker, presets.
- Icons are Material Symbols Rounded (generated from SVGs: design-system/generate_icons.py).
- Theme system: OKLCH color engine (5 palette styles, contrast, OLED), per-area overrides, album-art
  colors, Material / Clay / Glass / Classic OLED surfaces. Default theme is blue, not green.
- Found and fixed: the live spectrum stayed flat because nothing subscribed to it.
- No-cover placeholder is a music note.

## Backlog

- Wire the motion toolkit into screens; add a lyrics entry point in Now playing.
- Verify on a real phone: phone-layout bottom-nav taps (not reproducible in the browser test frame).
- Library "Downloaded" label shows for any playlist with one downloaded track; show a count instead.
- Glyph icons (such as pause bars) depend on platform fonts; replace with drawn icons.

- Real player (Android: Media3; web: audio element; Linux later).
- Downloads and offline library, "On this device" scan, downloaded-by-app list.
- YouTube sign-in via the official API, playlist import and sync, sync notifications.
- Music providers beyond YouTube, automatic best-quality selection (see `docs/providers.md`).
- More lyrics sources (the chain exists; add providers one at a time).
- Equalizer panel, queue in the desktop right panel, equalizer button (see decisions D6).
- Inter font bundling, Search and Now playing mobile optimization pass.
- Android release signing, web hosting.
- Linux desktop: run and measure again once the screens exist.

## Needs a decision from the owner

- Which icon family to use (see `docs/icon-options.html`), and whether "icon" also means the app launcher icon.

- Is LastWave-native yours? It is GPL-3.0, which limits copying its code (`docs/providers.md`).
- Which music sources are allowed (YouTube stream extraction and downloads have legal and
  terms-of-service risk; see `docs/providers.md`).
- Which lyrics sources are acceptable beyond the open ones.
