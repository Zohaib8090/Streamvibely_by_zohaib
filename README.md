# Streamvibely

A music app for Android, Linux desktop and web. Spotify-style layout, Material 3 Expressive
design language, one shared Kotlin codebase.

Status: Phase 0. Compose Multiplatform project. Desktop (Linux), web (Wasm) and Android all
build. Only the desktop target has been run; Android and web have not been run yet.

Run on Linux (needs JDK 17+):

```bash
./gradlew :composeApp:run                         # Linux desktop
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # web dev server
./gradlew :composeApp:wasmJsBrowserDistribution     # web production bundle
./gradlew :androidApp:assembleDebug                 # Android debug APK (needs Android SDK, API 37.2)
```

Regenerate theme tokens after editing `design-system/tokens/tokens.json`:

```bash
python3 design-system/generate_tokens.py
```

Start here:
- `CLAUDE.md` — rules and the final stack
- `docs/roadmap.md` — phases
- `docs/decisions.md` — why this stack
- `gotchas.md` — known traps
