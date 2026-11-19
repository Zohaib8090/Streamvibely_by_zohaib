# Packaging

Priority: Linux x86_64 on Arch. One format at first.

| Format | Notes |
|---|---|
| PKGBUILD (Arch) | Natural first target. Gradle downloads dependencies at build time; plan for offline/clean builds. |
| AppImage | Bundles the JVM runtime; larger, simple distribution. |
| Flatpak | Sandbox and a large runtime; needs portals for tray and shortcuts. |

Document runtime dependencies from actual inspection once a build exists (JRE or jlink
image, GStreamer/libmpv, PipeWire, D-Bus).

Android: signed APK/AAB. Web: static Wasm bundle.
