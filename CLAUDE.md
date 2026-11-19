# Streamvibely — Project Instructions

Streamvibely (working name was Reson) is a music app for Android, Linux desktop and web, with a Spotify-style layout
in the Material 3 Expressive design language. One shared codebase, written once.

Status: Phase 0. Desktop, web (Wasm) and Android all build; only desktop has been run. Read `docs/roadmap.md` for the phases
and `docs/decisions.md` for why the stack was chosen.

## Final stack (decided)

- Kotlin Multiplatform for logic, Compose Multiplatform for UI. One `commonMain` UI.
- Android: Compose (native).
- Linux: Compose Desktop on a trimmed JVM. Not Electron, Tauri, or a WebView wrapper.
- Web: Compose for Web (Wasm). Separate marketing site, if any, stays separate.
- Platform-specific code only through `expect/actual`: audio output, tray, MPRIS/media
  keys, notifications, filesystem access.
- Linux audio: GStreamer or libmpv. Do not write a custom decoder or audio engine.
- Fallback if Linux RAM fails its budget: Flutter (whole project). See `docs/decisions.md`.

## Platform scope

Android, Linux x86_64, Web. Do not add Windows or macOS.

## Rules

1. Write UI once in `commonMain`. No per-platform UI forks unless a platform cannot do it.
2. No GTK, Rust, or Kotlin/Native frontends. They were evaluated and dropped (see decisions).
3. Memory ceiling: Linux desktop at most 300 MB; mobile lower. Never decode more thumbnails than
   the visible limits in `docs/decisions.md` D5 (18 wide, 9 compact, 5 in the suggestions row).
   Do not claim "lightweight", "fast" or "small" without a measured number in
   `docs/performance.md`. Measure on the Arch dev machine.
4. Design tokens come from one source (`design-system/tokens`) and are generated into
   platform code. Do not hand-copy colors, radii or type sizes.
5. Do not copy Spotify's logo, brand green, icons, name or exact trade dress. Layout patterns
   (sidebar, cards, persistent player) are fine.
6. Do not claim a build works without running it. Record failures in `gotchas.md`.
7. Do not add frameworks, modules or dependencies silently. Record each choice in
   `docs/decisions.md`.
8. Keep modules minimal. Do not create empty modules for the sake of the suggested structure.

## Files

- `gotchas.md` — known traps and fixes; add to it whenever something costs time.
- `docs/roadmap.md` — phased plan.
- `docs/architecture.md` — modules, boundaries, platform adapters.
- `docs/decisions.md` — decision log with reasons.
- `docs/design-system.md` — tokens, components, Figma mapping.
- `docs/audio.md`, `docs/performance.md`, `docs/packaging.md`.
- `docs/streamvibely-reuse.md` — what to reuse from the existing Expo repo.
- `docs/archive/original-plan.md` — the original planning document, kept verbatim.

## Dev environment

Arch-based Linux, x86_64. Check JDK, Gradle and Kotlin versions before assuming them.
