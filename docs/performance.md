# Performance

Rule: do not call the app lightweight without a number from this file.

## Budget (set by the owner, 2026-11-17)

Linux desktop: **at most 300 MB PSS** (owner, 2026-11-17; replaces the earlier 200 MB idle target). Mobile: lower than desktop, number not set yet. Playback budget not set.

| Metric | Budget | Measured |
|---|---|---|
| Linux PSS | <= 300 MB | 133-223 MB depending on JVM flags with up to 18 covers on screen (see Artwork test); no audio/network yet |
| Playback PSS | TBD | not measured (no audio yet) |
| Idle CPU | TBD | about 2.5-2.9% in the first run (see notes) |
| Cold startup | TBD | not measured |
| Library scan | TBD | not measured |
| Artwork grid scroll | TBD | not measured (no artwork yet) |

## Phase 0 measurement, 2026-11-17

What was measured: the Home screen on the desktop target only (Compose Desktop, JDK 17.0.20,
Kotlin 2.4.21, Compose Multiplatform 1.12.2). Window 1280x800, desktop layout showing, solid
color blocks instead of images, no audio, no network. Single run per variant, 25 seconds after
launch, on the Arch dev machine (15 GiB RAM, about 5.5 GiB free during the test, other apps open).
PSS is read from `/proc/<pid>/smaps_rollup`.

| JVM flags | PSS | RSS | Threads |
|---|---|---|---|
| default (no flags) | 182 MB | 202 MB | 44 |
| `-XX:+UseSerialGC -Xmx256m -Xms32m` | 164 MB | 184 MB | 30 |
| above + `-XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m` | 147 MB | 167 MB | 29 |
| B + `-Xss512k -XX:MaxMetaspaceSize=96m -XX:CICompilerCount=2 -XX:-UsePerfData` | 160 MB | 180 MB | 30 |

Reference point, same machine and moment: the installed Spotify client used about 439 MB PSS
summed over 11 processes (one sample, unknown playback state; only a rough comparison).

### What this tells us

- Trimming flags saves roughly 20 to 35 MB (about 10 to 19 percent). The floor for even this
  trivial screen is about 150 MB. Skia, AWT and the JVM itself make up that floor.
- C1-only compilation (variant C) was the smallest but can make the UI slower. Its speed has
  not been measured, so do not adopt it yet.
- This is the lowest-load case. Real artwork, a large library, the audio engine and network
  will add to it. Treat 150 MB as a minimum, not a typical figure.
- Against the 200 MB idle budget the minimal screen passes, but with only 18 MB of headroom on
  default flags and 53 MB when trimmed. Artwork, a large library and the audio engine must fit
  in that headroom, so the budget is not yet proven for the real app.

## Not measured yet

Startup time, playback RAM, CPU under scrolling, library scan, artwork loading, frame rate.
Idle CPU of 2.5 to 2.9 percent in the first run is higher than expected for a static screen;
cause not investigated (could be the Gradle run wrapper, an animation, or the render loop).

## Gate

Idle budget is set (under 200 MB). Re-measure after each feature that adds memory (artwork,
library, audio). Set a playback budget next. If the real app cannot meet the budget after trimming,
evaluate Flutter (see decisions.md D1). Next measurements: a jlink-trimmed runtime, AppImage or
`.deb` package size, and a screen with real artwork.


## Artwork test, 2026-11-17

Entry point: `composeApp/src/desktopMain/.../ArtworkBench.kt` (benchmark only, not shipped).
Sample set: 100 synthetic 1200px JPEGs (about 240 KB each) and 300px versions (about 15 KB each).
PSS from `/proc/<pid>/smaps_rollup`, 25-second runs, one run per variant.

### Round 1-3: 100-cover scrolling grid (wrong test, kept as a stress result)

This tested far more covers than the app will ever load (owner policy: at most 18, see
decisions.md D5). Only useful as a worst case.

| Variant | PSS |
|---|---|
| Full 1200px decode, default JVM, cache 100 | 2081 MB |
| Full 1200px decode, 256 MB heap | crashed, OutOfMemoryError |
| 1200px decoded at 300px, default JVM | 259-410 MB |
| 1200px decoded at 200px, small heap | 213 MB |
| 1200px decoded at 200px, small heap + C1-only JIT | 177-180 MB |

Lesson that still holds: never decode a full-size cover. Decode at or near display size.

### Round 4: realistic counts, static (no scrolling), 300px covers, LRU cache 24

| Covers on screen | JVM flags | PSS |
|---|---|---|
| 18 (wide max) | defaults | 223 MB |
| 18 | SerialGC, 256 MB heap | 178 MB |
| 18 | SerialGC, 160 MB heap, free-ratio shrink, C1-only, 32 MB code cache | 165 MB |
| 9 (compact max) | same as above | 146 MB |
| 1 (near baseline) | same as above | 133 MB |

### What this shows

- With the realistic limit of 18 covers the desktop app stays **under the 300 MB ceiling in every
  variant, even with default JVM settings (223 MB)**.
- With trimmed JVM settings it is 165 MB, which leaves about 135 MB for audio, the YouTube
  suggestions row, the queue, and the equalizer panel.
- Each additional cover on screen costs roughly 2-4 MB in this setup; the fixed floor is about
  130 MB (JVM, Skia, AWT, Compose).

### Caveats (do not skip)

- These are the cover grid only. Not included: audio engine, networking, the suggestions row,
  queue, equalizer panel. Re-measure as each lands.
- Synthetic images, one run per variant, an idle static screen. Scrolling was only tested in the
  100-cover rounds.
- C1-only JIT and a 160 MB heap cap lower memory but were **not** checked for frame rate or
  stutter, and a 160 MB cap can run out of memory once more features exist. Treat the 256 MB
  heap + SerialGC variant (178 MB) as the safer starting setting.
- Mobile was not measured at all.


## Build sizes, 2026-11-17 (Home screen only; nothing was run except desktop)

| Target | Artifact | Size |
|---|---|---|
| Android | debug APK | 11.6 MB |
| Android | release APK (R8-shrunk, unsigned) | 0.94 MB (not installed or run) |
| Web | production bundle on disk | 13 MB |
| Web | gzipped transfer (skiko.wasm 3.3 MB, app wasm 0.75 MB, JS 0.1 MB) | about 4.2 MB |
| Desktop | app jar (excludes Compose and JVM) | 93 KB |

Android and web runtime memory and startup have not been measured.
