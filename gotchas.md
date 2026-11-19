# Gotchas

Known traps for Streamvibely. Add an entry whenever something costs time. Format:
**Problem** / **Cause** / **Fix** / **Date**.

## Planning-stage gotchas (not yet verified by a build)

These come from analysis, not from running code. Verify each one when you reach it, then
update the entry with the real result.

- **Compose Multiplatform has no Kotlin/Native `linuxX64` UI target.** Linux desktop runs on
  the JVM. Do not plan a JVM-free Compose UI on Linux.
- **Linux RAM is the main risk.** A JVM app costs more RAM than a native one. The size is
  unmeasured. Gate: measure idle and playback RSS/PSS on the Arch machine in Phase 0.
- **No `Dispatchers.Main` assumptions on desktop.** Use the Compose Desktop dispatcher; do
  not call UI code from audio or network threads.
- **Compose for Web (Wasm) is canvas-based.** Expect a large first download, limited SEO and
  weaker accessibility. Keep any marketing site separate.
- **DRM.** Widevine-style DRM works only in browsers. Decide the content source before
  building the player, or the Linux and Android players may not be able to play it.
- **Linux desktop integration is not free.** System tray needs StatusNotifierItem over D-Bus.
  "Media keys" means implementing MPRIS over D-Bus. Global shortcuts go through the XDG portal.
- **Packaging.** A PKGBUILD that runs Gradle downloads dependencies at build time, which is
  awkward for clean or offline AUR builds. Flatpak needs a large runtime. Plan early.
- **Figma to code is manual.** No tool converts the mockup into good Compose code. Build
  components from tokens.
- **Spotify trade dress.** Use their layout patterns only; keep Streamvibely's own brand.
- **Check M3 Expressive API status.** Compose Material 3 expressive APIs may be marked
  experimental or alpha. Verify the version and opt-in annotations before relying on them.
- **Existing repo has committed secrets.** See `docs/streamvibely-reuse.md`: rotate the keystore and remove secrets from history before anything else touches that repo.
- **Do not base playback on Piped/YouTube extraction.** Unreliable and likely against YouTube terms; decide the content source first.

- **The Figma mockup is a flat drawing.** No variables, styles, effects or auto-layout; 14 type sizes and 11+ radii. Normalize before building the theme. See `design-system/tokens/tokens.json`.
- **Two color pairs fail contrast:** `#a5afa7` on the mini-player (4.46:1) and lyrics `#8ea57b` on `#33452b` (3.86:1).

## Verified gotchas

Verified on 2026-11-17 by building and running the desktop target.

- **Toolchain that works:** JDK 17.0.20, Gradle 9.8.1 (wrapper), Kotlin 2.4.21, Compose
  Multiplatform 1.12.2. `./gradlew :composeApp:desktopJar` builds in about 1.5 minutes cold.
  `./gradlew :composeApp:run` starts the app.
- **Material 3 version differs from the Compose version.** The classpath resolves
  `org.jetbrains.compose.material3:material3-desktop:1.9.0` while the Compose plugin is 1.12.2.
  Check whether the Material 3 Expressive APIs exist in that version before using them.
- **Idle memory floor is about 150 MB PSS** on desktop even for a trivial screen. See
  `docs/performance.md`.
- **Inter is not bundled yet.** The theme uses the platform default font, so text differs from
  Figma. Bundle Inter before judging visual fidelity.
- **Do not use `pkill -f` with a pattern that appears in your own command line.** It kills the
  shell running it.
- **A full-screen screenshot captures everything on the screen**, not only the app window.
  Crop or use a window capture, and delete the file afterwards.
- **Never decode a full-size cover.** A 1200px JPEG decoded to a bitmap costs about 5.7 MB, and
  100 of them used over 2 GB. Decode near display size; with a capped heap, full-size decoding
  crashes with OutOfMemoryError.
- **Test with the real limits.** Benchmark with the thumbnail counts in `docs/decisions.md` D5
  (18 wide, 9 compact, 5 in the suggestions row), not a large library.
- **Android build needs compileSdk 37.2 (fixed 2026-11-17).** Compose Multiplatform 1.12.2 pulls
  androidx libraries that require compiling against API 37. API 37.2 was installed with
  `sdkmanager --channel=3 "platforms;android-37.2"` (canary channel at the time) and the modules use
  `compileSdk { version = release(37) { minorApiLevel = 2 } }`. This does not raise the app's
  requirements: `targetSdk` stays 35 and `minSdk` stays 24.
- **AGP 9 does not allow `com.android.application` and Kotlin Multiplatform in one module.**
  The shared code is a KMP library (`com.android.kotlin.multiplatform.library`) in `composeApp`
  and the Android app is a separate `androidApp` module.
- **Web bundle is large.** Production Wasm build: `skiko.wasm` 8.25 MiB (3.3 MB gzip), app wasm
  2.2 MiB (0.75 MB gzip), JS 0.5 MiB (0.1 MB gzip); about 4.2 MB over the wire if gzip/brotli is
  served. First load will be slow on mobile data.
- **A dependency exposed by `implementation` in `composeApp` is not visible in `androidApp`.**
  `androidApp` declares its own `compose.foundation` dependency.
- **The web build needs WebGL.** The built-in browser pane has none, so the app shows a blank page
  with a `getParameter` error there. It runs in a normal Chrome with WebGL.
- **A new web dependency means a lockfile refresh.** After adding libraries run
  `./gradlew kotlinWasmUpgradeYarnLock`, or the web build fails with "Lock file was changed".
- **Agent-written code compiled on the first try except one missing import.** Still always compile
  and run it before trusting it.
- **Browser click tests inside an iframe were unreliable** in the phone-width test frame (taps on
  the bottom nav did not switch tabs there, but the same navigation worked in the wide layout).
  The phone-layout bottom nav is therefore not confirmed; check it on a real phone.
- **Kotlin interface default methods and delegation:** `PlayerController.playNext` has a default no-op;
  the notification-permission wrapper uses `by delegate`. If `playNext` ever does nothing on Android,
  check that the wrapper forwards it.
- **Queue order with shuffle is the original order.** Media3's `MediaController` does not expose the
  shuffled order, so the Queue screen shows the unshuffled list while shuffle is on.
- **`AndroidPlatformActions` must be created in `onCreate`,** before the activity starts, because it
  registers activity-result launchers.
