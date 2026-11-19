# Audio

Each platform has its own audio implementation behind a shared `AudioPlayer` interface.
Do not force Android or browser audio APIs onto Linux.

## Linux

Use an existing native engine. Candidates: GStreamer, libmpv. Both support PipeWire output,
codecs, gapless and ReplayGain. To decide in Phase 0:

- How the JVM calls it (JNA, JNI, or an existing binding)
- Idle and playback RAM
- Gapless and seek behavior
- Output-device selection

Desktop integration: MPRIS over D-Bus (media keys and shell widgets), freedesktop
notifications, StatusNotifierItem tray, XDG portal for global shortcuts.

## Open issues

Content source and DRM decide whether native players can play the catalog at all.
