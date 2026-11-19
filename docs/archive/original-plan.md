# ARCHIVED — original planning document (superseded by docs/decisions.md D1)

Kept verbatim for reference. The "strict native Linux" decision below was replaced by
Compose Multiplatform for all targets. See `docs/roadmap.md` for the adapted plan.

---

# RESON — ARCHITECTURE OVERRIDE
# STRICT NATIVE LINUX DESKTOP DECISION

This document overrides all previous desktop architecture
recommendations in the Reson project specification.

## 1. FINAL PLATFORM SCOPE

Reson will initially support exactly THREE platforms:

1. Android.
2. Linux Desktop.
3. Web.

Remove Windows and macOS from: project targets, build configurations, distribution plans,
platform-specific modules, development roadmaps, testing requirements.

Do not introduce Windows or macOS support unless explicitly requested later.

## 2. STRICT NATIVE LINUX REQUIREMENT

The Linux desktop application MUST be a native Linux application.

PROHIBITED: Electron; Tauri; Chromium-based wrappers; WebView-based desktop shells;
Compose Desktop/JVM as the final desktop UI; unnecessary browser runtime dependencies.

The Linux application must use native Linux GUI and audio integrations.

Preferred GUI toolkit: GTK4; native GTK widgets; Material 3 Expressive-inspired visual
styling; optional libadwaita integration where appropriate.

IMPORTANT: Libadwaita follows GNOME design conventions, which are not identical to
Material 3 Expressive. Do not replace Reson's design language with default GNOME visual
styling. Material 3 Expressive remains the product's design direction. Use native GTK4
functionality while maintaining Reson-specific Material 3 Expressive color roles,
typography, shapes, controls and interaction patterns.

## 3. KOTLIN MULTIPLATFORM ON LINUX

Preserve Kotlin Multiplatform as the shared application logic foundation.

Preferred Linux implementation to investigate:

Kotlin Multiplatform -> linuxX64 target -> Kotlin/Native compiler -> Native GTK4
integration -> Native Linux executable

Use Kotlin/Native linuxX64 rather than Kotlin/JVM for the Linux desktop application.
Kotlin/Native should produce native Linux binaries without requiring a JVM runtime.
Investigate Kotlin/Native C interoperability (cinterop) to integrate GTK4 and relevant
Linux system libraries. The goal is to preserve shared Kotlin business logic and, where
practical, use Kotlin/Native for the Linux application. However, validate the complexity
of GTK4 integration before committing to the implementation.

## 4. GTK4 INTEGRATION PROOF OF CONCEPT

Before implementing the full Linux desktop UI, create a minimal native Linux application that:

1. Initializes GTK4.
2. Creates a native desktop window.
3. Displays a native GTK navigation layout.
4. Displays a basic music-library list.
5. Displays an example mini-player.
6. Handles basic user interaction.
7. Demonstrates Material 3 Expressive-inspired styling.
8. Compiles into a native Linux executable.
9. Runs without a JVM or Chromium runtime.

Investigate GTK4 integration using Kotlin/Native cinterop. Evaluate: GTK4 C bindings;
GObject object ownership; native callbacks; event loops; GTK application lifecycle; signal
handling; memory management; threading; Gradle integration; packaging. Document any
interoperability limitations.

## 5. FALLBACK NATIVE ARCHITECTURE

If direct Kotlin/Native GTK integration is too difficult or unmaintainable, investigate:

Native GTK4 frontend: Rust, gtk4-rs, libadwaita where useful.
Shared application logic: Kotlin Multiplatform, Kotlin/Native linuxX64 shared library, C ABI
integration between frontend and shared core.

Rust GTK4 Frontend -> C ABI / Native Interface -> Kotlin/Native Shared Library -> Shared
Reson Business Logic

This fallback is allowed because the GUI remains genuinely native. Do not use a WebView or
Electron as a fallback.

Before selecting the fallback, explain: why direct Kotlin/Native GTK integration is
unsuitable; the additional complexity of the Rust/Kotlin boundary; how application state
will be transferred; how native resources will be managed; how the shared business logic
will remain reusable. Do not silently change the language architecture.

## 6. SHARED KMP MODULES

Retain common Kotlin modules for: Core, Domain, Data, Networking, Authentication, Music
library, Playlists, Search, Lyrics, Discovery, Recommendations, Social, Synchronization.

The Linux target must have native platform adapters. Do not put GTK widgets or Linux-specific
system APIs in commonMain. Use platform interfaces and platform-specific implementations.

## 7. LINUX NATIVE AUDIO

Investigate the Linux audio stack. Preferred integrations: PipeWire; ALSA; native
output-device enumeration; native audio playback; media keys; desktop notifications; system
tray; local filesystem access.

The Linux audio engine must be independent of the Android audio implementation and browser
audio implementation. Implement audio output interfaces appropriate to Linux. Do not force
Android AudioTrack or browser Web Audio APIs into the Linux backend.

## 8. MATERIAL 3 EXPRESSIVE ON LINUX

Material 3 Expressive remains mandatory for the product's visual design direction.
Implement its design language using the native GUI toolkit.

Requirements: semantic color roles; typography system; expressive shapes; navigation
components; buttons; cards; sliders; dialogs; menus; motion and transitions; light and dark
themes.

The GTK implementation should visually correspond to the Android and web design system.
Share design tokens where practical. Do not introduce a browser rendering engine just to
reuse the Android or web interface.

## 9. LINUX PERFORMANCE REQUIREMENTS

Resource efficiency is a core requirement. Measure: idle RAM; playback RAM; CPU; startup
time; UI responsiveness; audio latency; library scanning performance; artwork loading;
background resource usage.

The target is a lightweight native desktop application. Do not make unsupported claims that
the application is lightweight. Provide actual build and runtime measurements.

## 10. LINUX DISTRIBUTION

Initial priority: Linux x86_64. The user's development environment is Arch-based Linux.
Evaluate: native Arch package / PKGBUILD; AppImage; Flatpak. Do not require every packaging
format in the first milestone. The application must use the system's appropriate native
libraries and avoid embedding unnecessary browser runtimes. Document runtime dependencies.

## 11. UPDATED PROJECT STRUCTURE

```
reson/
+-- shared/ (core, domain, data, networking, authentication, library, playlists, lyrics,
|            discovery, recommendations, social, synchronization)
+-- androidApp/ (compose-ui, native-player, system-equalizer, media-session, widgets)
+-- linuxApp/ (gtk-ui, native-window, navigation, native-player, pipewire, alsa, media-keys,
|              notifications, filesystem, packaging)
+-- webApp/ (browser-ui, browser-player, browser-audio, browser-storage)
+-- design-system/ (tokens, colors, typography, components, motion)
+-- backend/ (api, authentication, library-sync, social, discovery, lyrics, providers)
```

Adjust the project structure to match actual Gradle and platform requirements. Do not
create unnecessary modules.

## 12. UPDATED DEVELOPMENT PRIORITIES

PHASE 0:
1. Inspect existing repository and Replit workspace.
2. Check Kotlin and Gradle availability.
3. Check Kotlin/Native linuxX64 support.
4. Check GTK4 development dependencies.
5. Create a minimal Kotlin/Native GTK4 integration prototype.
6. Measure native executable startup and resource usage.
7. Validate the GTK event loop and native callbacks.
8. Evaluate the Rust GTK4 fallback only if needed.
9. Document the results.

PHASE 1: Finalize shared KMP core; finalize Android architecture; finalize Linux native
architecture; create GTK4 application shell; establish Material 3 Expressive design tokens;
create the initial Android UI; create the experimental web target.

PHASE 2: Implement native Linux navigation; Home; Search; Library; Now Playing; Queue;
native playback controls. Continue with the existing Reson implementation roadmap after
these foundation tasks.

## 13. IMPORTANT DEVELOPMENT RULES

1. No Electron. 2. No Tauri. 3. No WebView desktop wrapper. 4. No Compose Desktop/JVM final
Linux UI. 5. Native GTK4 desktop frontend. 6. Kotlin Multiplatform shared logic.
7. Kotlin/Native linuxX64 preferred for Linux compilation. 8. Material 3 Expressive visual
design. 9. Native Linux audio integrations. 10. Native Linux application packaging.
11. Android remains Kotlin/Jetpack Compose. 12. Web remains a separate browser target.
13. Do not add Windows or macOS targets. 14. Do not silently introduce other frameworks.
15. Verify every technical assumption through a working prototype.

## 14. FIRST RESPONSE REQUIRED FROM REPLIT

Before implementing the full application, provide: 1. KMP linuxX64 feasibility. 2. GTK4 +
Kotlin/Native interoperability assessment. 3. Native Linux build requirements. 4. GTK
event-loop and callback integration strategy. 5. Material 3 Expressive implementation
strategy for GTK. 6. Native audio architecture. 7. Replit environment limitations.
8. Proposed native packaging strategy. 9. Estimated runtime dependencies based on actual
inspection. 10. A working Phase 0 proof-of-concept plan.

Start with the proof of concept. Do not generate a large fake implementation. Do not claim
success without an executed build.

END OF ARCHITECTURE OVERRIDE.

Note: this archive condenses line breaks and bullet lists of the original for readability;
wording and requirements are preserved.
