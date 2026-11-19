# Roadmap

Adapted from the original planning document (`archive/original-plan.md`) to the final
decision in `decisions.md` D1: Compose Multiplatform for all three platforms.
Items marked (dropped) were in the original plan and are no longer in scope.

## Platform scope

Android, Linux x86_64, Web. Not Windows, not macOS.

## Phase 0 — Foundation and feasibility (prove before building)

1. Inspect the machine: JDK, Gradle, Kotlin, Android SDK, GStreamer/libmpv, PipeWire.
2. Read the Figma mockup (needs the Figma connector authorized). Extract tokens and screens
   into `design-system.md`.
3. Create a minimal Compose Multiplatform project (Android, desktop, Wasm web).
4. Build one real screen from Figma in `commonMain`: track list, mini-player, seek bar.
5. Run it on all three targets. Record build failures in `gotchas.md`.
6. Measure Linux idle/playback RAM (PSS), CPU, startup. Fill `performance.md`.
7. Test Linux audio: GStreamer vs libmpv from the JVM, with MPRIS.
8. Decide: pass the RAM gate and continue, or evaluate Flutter.
9. Document the results.

(dropped) Kotlin/Native GTK4 cinterop prototype, Rust gtk4-rs fallback.

## Phase 1 — Foundations

- Finalize the shared KMP core.
- Finalize the Android architecture and the Linux desktop adapters.
- Establish Material 3 Expressive design tokens from Figma; generate the theme.
- Shared component library (navigation, cards, track row, mini-player, sliders, dialogs).
- Android UI shell, desktop shell, experimental web target.

(dropped) GTK4 application shell.

## Phase 2 — Core screens

- Adaptive navigation (bottom bar on phone, sidebar on desktop and web).
- Home, Search, Library, Now Playing, Queue.
- Playback controls wired to platform audio.

## Phase 3 — Platform integration

- Linux: MPRIS and media keys, notifications, tray (StatusNotifierItem), output devices.
- Android: MediaSession, notifications, widgets, system equalizer.
- Web: Media Session API, browser storage.

## Phase 4 — Backend and features

Authentication, library sync, playlists, lyrics, discovery, recommendations, social,
providers. Decide content source and DRM before this phase.

## Phase 5 — Release

Linux: PKGBUILD first, then AppImage/Flatpak if needed. Android: signed build. Web: static
bundle. Final performance measurements.

## Shared modules (kept)

Core, domain, data, networking, authentication, library, playlists, search, lyrics,
discovery, recommendations, social, synchronization. Create them only when needed.

## Suggested structure (adapted)

```
reson/
  shared/            KMP logic
  composeApp/        Compose Multiplatform UI (android/desktop/wasm source sets)
  design-system/     tokens, generated theme
  backend/           api, auth, library-sync, social, discovery, lyrics, providers
  docs/
```

(dropped) separate `linuxApp/gtk-ui`, `webApp/browser-ui`, `androidApp/compose-ui` UI trees.
Platform code lives in `expect/actual` source sets.

## Development rules (kept from the original)

- Do not add Windows or macOS.
- Do not silently introduce other frameworks; log them in `decisions.md`.
- Verify every technical assumption with a working prototype.
- No claim of success without an executed build.
- No unsupported performance claims; real measurements only.
- No Electron, Tauri, or WebView desktop wrapper.
- No large fake implementation.

## First-response checklist (from the original, still required in Phase 0)

1. KMP feasibility on the three targets
2. Build requirements
3. Material 3 Expressive implementation strategy
4. Audio architecture
5. Environment limitations
6. Packaging strategy
7. Runtime dependencies from actual inspection
8. A working proof-of-concept plan
