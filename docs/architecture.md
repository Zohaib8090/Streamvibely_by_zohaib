# Architecture

```
design-system/tokens (one source) ──► generated Kotlin theme

shared/               Kotlin Multiplatform
  core, domain, data, networking, auth, library, playlists,
  search, lyrics, discovery, recommendations, social, sync
composeApp/           Compose Multiplatform UI (commonMain)
  theme, components, screens, adaptive layouts
  androidMain / desktopMain / wasmJsMain  -> thin platform code only
backend/              API, auth, library-sync, social, discovery, lyrics, providers
```

Create modules only when needed. Start with `shared` and `composeApp`, split later.

## Platform interfaces (`expect/actual`)

| Interface | Android | Linux (JVM) | Web |
|---|---|---|---|
| AudioPlayer | Media3/ExoPlayer | GStreamer or libmpv | Web Audio / HTMLAudio |
| MediaSession | MediaSession | MPRIS over D-Bus | Media Session API |
| Notifications | Android | freedesktop notifications | Notification API |
| Tray | — | StatusNotifierItem | — |
| Storage / files | scoped storage | filesystem + SQLite | IndexedDB |
| Output devices | AudioManager | PipeWire/ALSA enumeration | `enumerateDevices` |

## Layout

Adaptive via window size classes:
- Compact (phone): bottom navigation + mini-player.
- Expanded (desktop/web): sidebar, main pane, bottom now-playing bar, optional queue panel.

## Rules

- No platform APIs in `commonMain`.
- The shared core owns state; UI renders it and sends events.
- Page the library from SQLite; never load all tracks into memory.

## Adaptive shell (Build 14)

`App.kt` picks the layout from the window shape, not the device:
- **Compact**: portrait (or under 600 dp wide). Bottom navigation, mini player + swipe-up sheet. Grid columns: 2 (<600 dp), 3 (600 to 839), 4 (840+).
- **Rail**: landscape, 600 to 899 dp wide (phones on their side). 88 dp navigation rail, bottom playback bar. Tapping the track opens the full player over the page.
- **Expanded**: landscape, 900 dp or wider (tablets, desktop). Sidebar, content (3 or 4 columns), right panel (queue / lyrics / equalizer, 300 or 360 dp), bottom playback bar with shuffle, repeat, seek, lyrics and equalizer.
