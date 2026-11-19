# Brief for build agents (shared rules)

Project: Streamvibely, Kotlin + Compose Multiplatform for Android, web (Wasm) and Linux desktop.
Working directory: /home/zohaibbaig/Documents/songsproject

Read first: CLAUDE.md, docs/design-system.md, docs/decisions.md (D5 thumbnails, D6 design
changes), composeApp/src/commonMain/kotlin/app/streamvibely/domain/Models.kt (shared contracts),
composeApp/src/commonMain/kotlin/app/streamvibely/App.kt and theme/Theme.kt (style to match).

Design reference: Figma frames. The layout dump (node names, positions, sizes, text) is JSON at
/home/zohaibbaig/.claude/projects/-home-zohaibbaig-Documents-songsproject/c7dad065-a54b-44ce-8f46-6a555190804c/tool-results/mcp-276d14b9-5457-481c-be34-0a53c1699abf-get_metadata-1794926488804.txt
(one long line: use `jq -r '.[0].text'` then grep the frame name). A low-resolution overview image is
at /tmp/claude-1000/-home-zohaibbaig--claude-spares-spare-vJHsdA/c7dad065-a54b-44ce-8f46-6a555190804c/scratchpad/figma.png
(view it with the Read tool). Colors, type sizes and radii: design-system/tokens/tokens.json and
the generated `app.streamvibely.theme.Tokens`.

Rules:
1. Write ONLY inside the directories assigned to you. Never edit build files, App.kt, Theme.kt,
   Tokens.kt (generated), domain/, or another agent's folder. If you need a change elsewhere,
   describe it in your report.
2. Do NOT run Gradle or any build. The lead compiles everything; parallel builds exhaust RAM.
   So be careful: check every import and API call. Prefer simple, well-known APIs.
3. Shared code (commonMain) must be pure Kotlin Multiplatform: no `java.*`, no `android.*`.
   Allowed libraries: compose runtime, foundation, ui, material3, animation, kotlinx-coroutines,
   kotlinx-serialization-json, ktor-client (core, content-negotiation, kotlinx-json).
   NOT available: material-icons-extended, image loading libraries (use colored boxes as artwork
   placeholders), navigation libraries.
4. Use `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and `Tokens` / `Tokens.radius*`.
   Do not hard-code colors or font sizes. Inter is not bundled yet; ignore fonts.
5. Screens are stateless: `@Composable fun XScreen(data, callbacks, modifier)` with sensible
   defaults from `SampleData`. They must work at phone width (about 360-412dp) and wide width.
6. Performance: lazy lists with stable `key`, no work in composition, at most 18 thumbnails
   visible (wide), 9 (compact) per decisions D5.
7. Keep files focused and short. No dead code, no TODO placeholders pretending to be features.
8. Final report (under 250 words): files created, public function signatures, assumptions,
   anything you could not verify. Say plainly if something may not compile.

---

# Round 2 additions (local playback milestone)

Goal: a real, testable Android app that plays the user's own music files, with a real library,
settings for which folders to scan, an equalizer, and a polished player UI. No online sources yet.

New shared contracts you must implement or use (read them first):
- composeApp/src/commonMain/kotlin/app/streamvibely/domain/Services.kt: `LibraryRepository`,
  `PlaylistRepository`, `EqualizerController`, `ArtworkLoader`, `PlatformActions`, plus
  `LibrarySettings`, `ScanMode`, `ScanState`, `MusicFolder`, `EqualizerState`.
- domain/Models.kt: `Track` now has `uri` (the content:// or file URI used for playback).
- ui/common/ArtworkImage.kt: `ArtworkImage(track, modifier, sizePx)` and `LocalArtworkLoader`.
- androidApp/.../playback/AudioSession.kt: where the player publishes its audio session id.

Android-only code lives in the `androidApp` module under `app.streamvibely.<area>` (you MAY use
`android.*`, AndroidX, Media3, DataStore, DocumentFile there). Dependencies already declared in
androidApp: media3 exoplayer and session 1.11.1, datastore-preferences, documentfile,
kotlinx-coroutines-android, kotlinx-serialization-json, activity-compose, compose foundation and
material3, glance. Do not edit build files; if you need another dependency, say so in your report.
Target: minSdk 24, compileSdk 37.2, so guard newer APIs with `Build.VERSION.SDK_INT` checks.
Do NOT edit AndroidManifest.xml (the lead merges it); put the exact XML snippets you need in your
report. Do not touch `App.kt` or `MainActivity.kt` (the lead wires them); describe how to construct
your classes in your report.
Memory rule (owner decision D5): never decode a full-size cover; keep at most about 24 bitmaps cached.
Privacy: never log file paths or track metadata at info level; no analytics; no network access.

---

# Round 3 additions (polish, account menu, parametric equalizer)

Owner feedback after testing on a phone: use PROPER ICONS everywhere (no text glyphs or emoji such as
"⚙", "›", "✕", "⏮", "≡", "⋮", "‹"); animate the full-screen player collapsing to the mini player and
back with a swipe; add an account center (avatar button, top left) opening a side menu with Account,
Listening usage and Settings; and make the equalizer world class, like the Poweramp app: any number of
bands, each with frequency, gain and Q, a live spectrum at the bottom while playing.

Icons: `org.jetbrains.compose.material:material-icons-extended` is now available in commonMain.
Use `AppIcon(Icons.Rounded.<Name>, contentDescription, modifier, tint, size)` from
`app.streamvibely.ui.common.AppIcon`. Import icons individually, for example
`import androidx.compose.material.icons.rounded.PlayArrow`. Useful names: Home, Search,
LibraryMusic, Settings, Person, AccountCircle, Menu, ArrowBack, MoreVert, Close, Add, Check,
PlayArrow, Pause, SkipNext, SkipPrevious, Shuffle, Repeat, RepeatOn, Equalizer, Tune, Lyrics, QueueMusic,
Devices, Download, DownloadDone, Folder, FolderOpen, ChevronRight, KeyboardArrowDown, KeyboardArrowUp,
Delete, Edit, Refresh, Info, Security, GraphicEq, BarChart, Timer, Login, Logout, DragHandle,
Favorite, PlaylistAdd, PlaylistPlay, VolumeUp. If an icon name does not exist in the Rounded set, say
so in your report and use the closest one; do not draw characters.

New contracts in domain/Services.kt: the parametric `EqBand`, `EqualizerState`, `EqualizerController`
(with `spectrum: StateFlow<FloatArray>` of `SPECTRUM_BINS` values), plus `Account`,
`AccountRepository`, `UsageSummary`, `ListeningUsageRepository`.

---

# Round 4 additions (theming system)

Icons are now Material Symbols Rounded: use `AppIcon(AppIcons.Name, ...)` (`ui/common/AppIcons.kt`,
generated from SVGs by design-system/generate_icons.py; list in that script; to add an icon add it to
the script's ICONS dict and run `python3 design-system/generate_icons.py fetch` then `gen`).

Theming contracts (read them): `domain/Theme.kt` (`ThemeSpec`, `ThemeMode`, `ThemeStyle`, `ColorSource`,
`PaletteVariant`, `ThemeArea`, `ThemeRepository`), `theme/AppColors.kt` (`AppColors`, `AppTheme.colors`,
`LocalAppColors`), `theme/engine/GeneratedColors.kt`. Rules: screens must never hard-code colors and
must not use the generated `Tokens` color constants any more (`Tokens.radius*` constants stay, but should
be multiplied by the theme's corner scale via `AppTheme.cornerScale` once the runtime provides it).
The default look is NOT green: the original lime green is just one preset ("Lime").
