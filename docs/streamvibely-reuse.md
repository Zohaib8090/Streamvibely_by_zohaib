# What we can reuse from the existing Streamvibely repo

Repo: https://github.com/Zohaib8090/Streamvibely_by_zohaib (MIT license, public).
Inspected 2026-11-17 by reading the file tree and key files; nothing was built or run.

## What it is

An Expo / React Native (TypeScript) Android app, with react-native-web for web. Expo Router,
Firebase auth, Google sign-in, notifications, react-native-webview and expo-video for
playback. About 122 KB of TypeScript. It was formerly named "spoton".

## Reuse as ideas and logic (port to Kotlin), not as code

TypeScript/React Native code cannot run inside Compose Multiplatform. What carries over:

| From repo | Use in new project |
|---|---|
| `contexts/PlayerContext.tsx` state model: current track, playlist, original playlist (for un-shuffle), history capped at 50, shuffle, loop, player state hidden/mini/full | Port to a shared Kotlin `PlayerState` / queue engine in `shared/core`. The structure is sound. Fix the known holes when porting (see below). |
| Screen list: auth, home, search, search results, library, downloads, history, profile, settings, what's new, create playlist | Confirms the feature scope; map to the Figma screens |
| Stream fallback across several Piped API instances | Idea only; see risk below |
| Dark palette primary `#c2ffc2` (mint green) | Matches the Figma lime-green accent; use as a starting point for tokens |
| Firebase Auth + Google sign-in | A backend/auth decision to make; Firebase has Kotlin Multiplatform-friendly options, but decide in Phase 4 |
| Notifications hook, haptics | Reimplement per platform via `expect/actual` |

## Problems found in the existing code (do not carry over)

- **Secrets and build files are committed:** `key.b64`, `my-release-key.keystore`,
  `android/app/debug.keystore`, `google-services.json` (twice). Treat the release keystore
  and anything in `key.b64` as compromised. Rotate them and remove them from git history.
  Also `node_modules/` is committed.
- **`app/api/stream/[id].ts`** relies on public Piped instances to get YouTube audio URLs.
  These instances are unreliable and go offline, and extracting YouTube streams can violate
  YouTube's terms of service. Do not build the new app's core playback on it. Decide the
  content source first (see `decisions.md`, open decisions).
- `PlayerContext.toggleShuffle` assumes `currentTrack` is non-null; `handleStreamUrlFetched`
  checks `!trackToPlay` after already defaulting to an object, so the guard never fires.
- Types are loose (`createContext(null)`, untyped props).

## Decision implied

The existing app is Expo / React Native. For Android + web it already works from one
codebase. It has no Linux desktop target, which is why the new project uses Compose
Multiplatform. Keep the old repo as a reference and as the live Android app until the new
one replaces it.

## Open questions

- Is the new project a replacement for the Expo app, or a parallel rewrite?
- Keep Firebase for auth and sync?
- Content source: user's own files, Spotify/YouTube connectors, or a licensed catalog?
